package com.shut.browse;

import java.util.Locale;

/** Turns what a user typed into a case-insensitive "contains" pattern for SQL LIKE (escape character: backslash). */
final class SearchPattern {

    private static final int MAX_LENGTH = 60;

    private SearchPattern() {
    }

    /** A blank query matches everything. */
    static String of(String query) {
        String normalized = normalize(query);
        return "%" + escape(normalized) + "%";
    }

    static String normalize(String query) {
        if (query == null) {
            return "";
        }
        String trimmed = query.strip().replaceAll("\\s+", " ");
        // Telegram users often type Arabic forms of ی and ک; profiles are stored with the Persian ones.
        trimmed = trimmed.replace('ي', 'ی').replace('ك', 'ک');
        if (trimmed.length() > MAX_LENGTH) {
            trimmed = trimmed.substring(0, MAX_LENGTH);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
