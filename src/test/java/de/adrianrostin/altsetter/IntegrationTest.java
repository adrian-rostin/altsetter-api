package de.adrianrostin.altsetter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Startet die ganze Anwendung gegen ein echtes Postgres in Docker, in das Flyway die echte Migration
 * mit allen Daten einspielt. Die Tests prüfen also gegen genau den Katalog, der auch im Cluster liegt.
 * Alle Tests teilen sich den Kontext und damit einen Container.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        properties = {
            "spring.flyway.enabled=true",
            "altsetter.rate-limit.enabled=false",
            "springdoc.api-docs.enabled=true"
        })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {}
