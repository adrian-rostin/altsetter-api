package de.adrianrostin.altsetter.dto;

import java.util.List;

/**
 * Vollständige Liste in derselben Hülle wie {@link PageResponse}: alle Listen der API haben ihre
 * Einträge unter {@code items}. So kann eine Antwort später Metadaten bekommen, ohne Clients zu brechen.
 */
public record ListResponse<T>(List<T> items) {

    public static <T> ListResponse<T> of(List<T> items) {
        return new ListResponse<>(List.copyOf(items));
    }
}
