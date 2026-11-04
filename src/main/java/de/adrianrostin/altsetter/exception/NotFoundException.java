package de.adrianrostin.altsetter.exception;

import lombok.Getter;

/** Eine angefragte Ressource gibt es nicht. Die Meldung für Clients steht in {@code messages*.properties}. */
@Getter
public class NotFoundException extends RuntimeException {

    /** Was gesucht wurde; bestimmt den Text der Meldung ({@code detail.not-found.<key>}). */
    @Getter
    public enum Resource {
        EXERCISE("exercise");

        private final String key;

        Resource(String key) {
            this.key = key;
        }
    }

    private final Resource resource;
    private final String id;

    public NotFoundException(Resource resource, String id) {
        super("%s '%s' not found".formatted(resource.getKey(), id));
        this.resource = resource;
        this.id = id;
    }
}
