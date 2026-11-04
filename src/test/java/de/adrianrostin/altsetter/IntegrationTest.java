package de.adrianrostin.altsetter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Startet die ganze Anwendung gegen ein echtes Postgres in Docker. Hibernate legt die Tabellen dort
 * aus den Entities an, {@link TestCatalog} füllt sie mit einem kleinen Testkatalog. Die echten Daten
 * liegen nur in der Datenbank im Cluster. Alle Tests teilen sich den Kontext und damit einen Container.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        properties = {
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
            "altsetter.rate-limit.enabled=false",
            "springdoc.api-docs.enabled=true"
        })
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestCatalog.class})
public @interface IntegrationTest {}
