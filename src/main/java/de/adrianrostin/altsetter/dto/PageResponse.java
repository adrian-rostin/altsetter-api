package de.adrianrostin.altsetter.dto;

import java.util.List;

/**
 * Stabile JSON-Form einer Ergebnisseite. Spring Datas {@code Page} wird bewusst nicht direkt
 * serialisiert, weil sich deren Aufbau zwischen Versionen ändern kann.
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    /** Seite {@code page} (ab 0) mit höchstens {@code size} Einträgen aus der vollständigen Liste. */
    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int from = (int) Math.min((long) page * size, all.size());
        int to = Math.min(from + size, all.size());
        int totalPages = (all.size() + size - 1) / size;
        return new PageResponse<>(List.copyOf(all.subList(from, to)), page, size, all.size(), totalPages);
    }
}
