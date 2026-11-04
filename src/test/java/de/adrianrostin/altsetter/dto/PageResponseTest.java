package de.adrianrostin.altsetter.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PageResponseTest {

    private final List<Integer> all = IntStream.rangeClosed(1, 5).boxed().toList();

    @Test
    void cutsTheRequestedPage() {
        assertThat(PageResponse.of(all, 1, 2)).isEqualTo(new PageResponse<>(List.of(3, 4), 1, 2, 5, 3));
    }

    @Test
    void theLastPageMayBeShorter() {
        assertThat(PageResponse.of(all, 2, 2).items()).containsExactly(5);
    }

    @Test
    void pagesBehindTheEndAreEmpty() {
        assertThat(PageResponse.of(all, 10_000, 100)).isEqualTo(new PageResponse<>(List.of(), 10_000, 100, 5, 1));
    }
}
