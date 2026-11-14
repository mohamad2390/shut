package com.shut.browse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Turns what a user typed into words to look for. A member matches when every word is part of something in their
 * profile, so "تصویر برداری" finds "تصویربرداری هوایی" and "ترکیه وان" finds a member in "وان, ترکیه".
 * Spaces, punctuation and half-spaces (ZWNJ) do not count, on either side of the comparison.
 */
final class SearchPattern {

    static final char ZWNJ = '‌';
    private static final int MAX_WORDS = 5;
    private static final int MAX_WORD_LENGTH = 30;

    private SearchPattern() {
    }

    /** SQL LIKE patterns, one per word (escape character: backslash). A blank query gives none, and matches everyone. */
    static List<String> words(String query) {
        List<String> patterns = new ArrayList<>();
        for (String word : split(query)) {
            patterns.add("%" + escape(word) + "%");
        }
        return patterns;
    }

    /** The words of the query, lower case, in Persian letters, without half-spaces. */
    static List<String> split(String query) {
        if (query == null) {
            return List.of();
        }
        // Telegram users often type Arabic forms of ی and ک; profiles are compared with the Persian ones.
        String cleaned = query.replace('ي', 'ی').replace('ك', 'ک').replace(ZWNJ, ' ').toLowerCase(Locale.ROOT)
                // Commas and other punctuation only separate words: "وان, ترکیه" is the words وان and ترکیه.
                .replaceAll("[^\\p{L}\\p{N}]+", " ");
        return Arrays.stream(cleaned.strip().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .map(word -> word.length() > MAX_WORD_LENGTH ? word.substring(0, MAX_WORD_LENGTH) : word)
                .limit(MAX_WORDS)
                .toList();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
