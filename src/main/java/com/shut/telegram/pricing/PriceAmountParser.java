package com.shut.telegram.pricing;

import com.shut.common.text.PersianDigits;

import java.util.regex.Pattern;

/** Reads a price typed by a user, accepting Persian digits and thousands separators. */
final class PriceAmountParser {

    static final long MAX_AMOUNT = 10_000_000_000L;

    private static final Pattern SEPARATORS = Pattern.compile("[,\\u066C\\u060C\\s]");
    private static final Pattern DIGITS = Pattern.compile("\\d{1,11}");

    private PriceAmountParser() {
    }

    static long parse(String raw) {
        String value = SEPARATORS.matcher(PersianDigits.toAscii(raw)).replaceAll("");
        if (DIGITS.matcher(value).matches()) {
            long amount = Long.parseLong(value);
            if (amount > 0 && amount <= MAX_AMOUNT) {
                return amount;
            }
        }
        throw new InvalidPriceInputException(
                "قیمت معتبر نیست. فقط عدد بفرست، به تومان و بیشتر از صفر. مثلاً ۵۰۰۰۰۰");
    }
}
