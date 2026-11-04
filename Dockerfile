# syntax=docker/dockerfile:1

# --- Build: Jar bauen und in Schichten zerlegen ---------------------------------
# Basis-Images auf einen Digest festgelegt, damit jeder Build dasselbe Image nutzt; Dependabot hält sie aktuell.
FROM eclipse-temurin:25-jdk-alpine@sha256:425f0b733cb23843bd78dbe5552513976d0cf12b9722752c48a5161cf5cd2f32 AS build
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
FROM eclipse-temurin:25-jre-alpine@sha256:56f273059de6b13a6b49fe0ded691ed2d0399bbee76156a8f2d385f7524a3d43 AS runtime
WORKDIR /app

RUN addgroup -S -g 1000 app && adduser -S -u 1000 -G app app

# Selten geänderte Schichten zuerst: Abhängigkeiten bleiben zwischen Releases im Cache.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER 1000
EXPOSE 8080 8081
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
