# syntax=docker/dockerfile:1

# --- Build: Jar bauen und in Schichten zerlegen ---------------------------------
# Basis-Images auf einen Digest festgelegt, damit jeder Build dasselbe Image nutzt; Dependabot hält sie aktuell.
FROM eclipse-temurin:25-jdk-alpine@sha256:c7c3d0bdf7f2bbc5a05a6712ed334d198360105634ea9adf76fca94f381b1e3f AS build
WORKDIR /workspace

# Abhängigkeiten zuerst, damit dieser Schritt bei reinen Code-Änderungen aus dem Cache kommt.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
# Lint und Tests laufen in der Pipeline, nicht noch einmal im Image-Build.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp package -DskipTests -Dcheckstyle.skip -Djacoco.skip \
 && java -Djarmode=tools -jar target/altsetter-api-*.jar extract --layers --launcher --destination extracted

# --- Runtime: nur JRE und die Anwendung, ohne Root-Rechte ------------------------
FROM eclipse-temurin:25-jre-alpine@sha256:e1898fd6a11fc965a994d8ace186fd3585aeb636cf8f05f47a723fcfee0b0918 AS runtime
WORKDIR /app

# Sicherheitsupdates des Basis-Images sofort einspielen, nicht erst beim nächsten Image-Release.
RUN apk upgrade --no-cache \
 && addgroup -S -g 1000 app && adduser -S -u 1000 -G app app

# Selten geänderte Schichten zuerst: Abhängigkeiten bleiben zwischen Releases im Cache.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER 1000
EXPOSE 8080 8081
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
