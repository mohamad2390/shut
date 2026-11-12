package com.shut.telegram.widget;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WidgetLimitsTest {

    @Test
    void thePriceWidgetIsSmallEnoughForTelegram() throws Exception {
        String html = new ClassPathResource(WidgetHtml.PRICES_RESOURCE).getContentAsString(StandardCharsets.UTF_8);

        assertThat(WidgetLimits.problemWith(html)).isNull();
    }

    @Test
    void aPageWithANulCharacterIsRefused() {
        assertThat(WidgetLimits.problemWith("<p>a\u0000b</p>")).contains("NUL");
    }

    @Test
    void aPageOverTheSizeLimitIsRefused() {
        assertThat(WidgetLimits.problemWith("a".repeat(WidgetLimits.MAX_BYTES + 1))).contains("limit");
    }

    @Test
    void aPageThatDoesNotCompressWellIsRefused() {
        java.util.Random random = new java.util.Random(1);
        StringBuilder noise = new StringBuilder();
        for (int i = 0; i < 12_000; i++) {
            noise.append((char) ('a' + random.nextInt(26)));
        }

        assertThat(WidgetLimits.problemWith(noise.toString())).contains("compressed");
    }
}
