package de.adrianrostin.altsetter.config;

import de.adrianrostin.altsetter.entity.Language;
import de.adrianrostin.altsetter.exception.ProblemType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Arrays;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beschreibung der API für Swagger UI unter {@code /api/docs}, inklusive aller Fehlerantworten. Nur im
 * Profil {@code local} erreichbar; in Produktion ist springdoc abgeschaltet.
 */
@Configuration
public class OpenApiConfig {

    private static final String PROBLEM = "Problem";

    @Bean
    OpenAPI altsetterOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Altsetter API")
                        .version("v1")
                        .description("Muskeln, Equipment und Übungen für Altsetter. Nur lesend, ohne Anmeldung. "
                                + "Alle Texte gibt es auf Deutsch und Englisch, gewählt mit Accept-Language."))
                .servers(List.of(new Server().url("/").description("Gleiche Domain wie die App")));
    }

    /** Jede Operation der v1 versteht Accept-Language und nennt die gelieferte Sprache. */
    @Bean
    OpenApiCustomizer languageHeaders() {
        List<String> codes =
                Arrays.stream(Language.values()).map(Language::getCode).toList();
        return openApi -> openApi.getPaths().forEach((path, item) -> {
            if (!path.startsWith("/api/v1/")) {
                return;
            }
            item.readOperations().forEach(operation -> {
                operation.addParametersItem(new HeaderParameter()
                        .name("Accept-Language")
                        .description("Sprache der Texte; ohne passende Angabe Deutsch")
                        .schema(new StringSchema()._enum(codes)._default(Language.DEFAULT.getCode())));
                operation
                        .getResponses()
                        .values()
                        .forEach(response -> response.addHeaderObject(
                                "Content-Language",
                                new Header()
                                        .description("Gelieferte Sprache")
                                        .schema(new StringSchema()._enum(codes))));
            });
        });
    }

    /** Jede Operation kann mit diesen Problem Details antworten. */
    @Bean
    OpenApiCustomizer problemResponses() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas(PROBLEM, problemSchema());
            addProblemResponses(openApi);
        };
    }

    private static void addProblemResponses(OpenAPI openApi) {
        openApi.getPaths()
                .values()
                .forEach(path -> path.readOperations().forEach(operation -> {
                    var responses = operation.getResponses();
                    responses.addApiResponse("400", problem("Ungültiger Parameter"));
                    responses.addApiResponse("429", problem("Zu viele Anfragen; siehe Retry-After"));
                    responses.addApiResponse("503", problem("Datenbank gerade ausgelastet; siehe Retry-After"));
                    boolean hasId = operation.getParameters() != null
                            && operation.getParameters().stream().anyMatch(p -> "id".equals(p.getName()));
                    if (hasId) {
                        responses.addApiResponse("404", problem("Nicht gefunden"));
                    }
                }));
    }

    private static ApiResponse problem(String description) {
        Schema<?> ref = new Schema<>().$ref("#/components/schemas/" + PROBLEM);
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/problem+json", new MediaType().schema(ref)));
    }

    @SuppressWarnings("rawtypes")
    private static Schema problemSchema() {
        List<String> types = Arrays.stream(ProblemType.values())
                .map(type -> type.uri().toString())
                .toList();
        return new ObjectSchema()
                .description("Problem Details nach RFC 9457")
                .addProperty("type", new StringSchema()._enum(types).description("Art des Fehlers, stabil"))
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema().description("Meldung in der gewählten Sprache"))
                .addProperty("instance", new StringSchema())
                .addProperty("requestId", new StringSchema().description("Steht auch im Header X-Request-Id"))
                .addProperty(
                        "errors",
                        new ArraySchema()
                                .items(new ObjectSchema()
                                        .addProperty("parameter", new StringSchema())
                                        .addProperty("value", new Schema<>()))
                                .description("Nur bei invalid-parameter"));
    }
}
