package de.adrianrostin.altsetter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** REST-API für Altsetter: Muskeln, Equipment und Übungen, ausschließlich lesend. */
@SpringBootApplication
public class AltsetterApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AltsetterApiApplication.class, args);
    }
}
