package com.shut.browse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchPatternTest {

    @Test
    void blankQueryMatchesEverything() {
        assertEquals("%%", SearchPattern.of("   "));
        assertEquals("%%", SearchPattern.of(null));
    }

    @Test
    void wrapsLowerCasedQueryInWildcards() {
        assertEquals("%portrait%", SearchPattern.of("  Portrait "));
    }

    @Test
    void collapsesInnerWhitespace() {
        assertEquals("%wedding photo%", SearchPattern.of("wedding    photo"));
    }

    @Test
    void turnsArabicLettersIntoPersianOnes() {
        assertEquals("%کی%", SearchPattern.of("كي"));
    }

    @Test
    void escapesLikeWildcards() {
        assertEquals("%100\\%\\_a\\\\b%", SearchPattern.of("100%_a\\b"));
    }

    @Test
    void cutsVeryLongQueries() {
        assertEquals(60 + 2, SearchPattern.of("x".repeat(200)).length());
    }
}
