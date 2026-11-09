package com.shut.common.text;

import java.util.Locale;

/** Converts between ASCII digits and Persian digits. */
public final class PersianDigits {

    private static final char PERSIAN_ZERO = '۰';
    private static final char ARABIC_INDIC_ZERO = '٠';

    private PersianDigits() {
    }

    /** Replaces every ASCII digit in the text with its Persian form. */
    public static String toPersian(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            result.append(c >= '0' && c <= '9' ? (char) (PERSIAN_ZERO + (c - '0')) : c);
        }
        return result.toString();
    }

    public static String toPersian(long number) {
        return toPersian(Long.toString(number));
    }

    /** Formats a number with Persian digits and the Persian thousands separator, e.g. ۱٬۰۰۰٬۰۰۰. */
    public static String toPersianGrouped(long number) {
        return toPersian(String.format(Locale.ROOT, "%,d", number)).replace(',', '\u066C');
    }

    /** Replaces Persian and Arabic-Indic digits with ASCII digits so the text can be parsed. */
    public static String toAscii(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c >= PERSIAN_ZERO && c <= PERSIAN_ZERO + 9) {
                result.append((char) ('0' + (c - PERSIAN_ZERO)));
            } else if (c >= ARABIC_INDIC_ZERO && c <= ARABIC_INDIC_ZERO + 9) {
                result.append((char) ('0' + (c - ARABIC_INDIC_ZERO)));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
