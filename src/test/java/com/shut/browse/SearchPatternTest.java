package com.shut.browse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchPatternTest {

    @Test
    void blankQueryHasNoWordsAndMatchesEverything() {
        assertEquals(List.of(), SearchPattern.words("   "));
        assertEquals(List.of(), SearchPattern.words(null));
    }

    @Test
    void everyWordBecomesItsOwnLowerCasedPattern() {
        assertEquals(List.of("%wedding%", "%photo%"), SearchPattern.words("  Wedding    PHOTO "));
    }

    @Test
    void halfSpacesSplitWordsSoSpacingDoesNotMatter() {
        assertEquals(List.of("%تصویر%", "%برداری%"), SearchPattern.words("تصویر‌برداری"));
        assertEquals(List.of("%تصویر%", "%برداری%"), SearchPattern.words("تصویر برداری"));
    }

    @Test
    void punctuationOnlySeparatesWords() {
        assertEquals(List.of("%وان%", "%ترکیه%"), SearchPattern.words("وان, ترکیه"));
    }

    @Test
    void turnsArabicLettersIntoPersianOnes() {
        assertEquals(List.of("%کی%"), SearchPattern.words("كي"));
    }

    @Test
    void escapesLikeWildcards() {
        assertEquals(List.of("%100\\%\\_a\\\\b%"), SearchPattern.words("100%_a\\b"));
    }

    @Test
    void cutsLongWordsAndLongQueries() {
        assertEquals(List.of("%" + "x".repeat(30) + "%"), SearchPattern.words("x".repeat(200)));
        assertEquals(5, SearchPattern.words("a b c d e f g h").size());
    }
}
