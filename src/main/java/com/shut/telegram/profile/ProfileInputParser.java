package com.shut.telegram.profile;

import com.shut.common.text.PersianDigits;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Turns what a user typed into a clean value for one profile field, or explains what is wrong. */
final class ProfileInputParser {

    /** Sending this alone clears the field. */
    private static final String CLEAR = "-";
    private static final int MAX_YEARS = 80;
    private static final Pattern LIST_SEPARATOR = Pattern.compile("[,،\\n]");
    private static final Pattern INSTAGRAM_PREFIX =
            Pattern.compile("^(https?://)?(www\\.)?instagram\\.com/", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSTAGRAM_NAME = Pattern.compile("^[A-Za-z0-9._]{1,30}$");

    private ProfileInputParser() {
    }

    static String text(String raw, int maxLength) {
        String value = raw.trim();
        if (CLEAR.equals(value)) {
            return "";
        }
        if (value.length() > maxLength) {
            throw new InvalidProfileInputException(
                    "متن خیلی بلنده. حداکثر " + PersianDigits.toPersian(maxLength) + " حرف مجازه و متن تو "
                            + PersianDigits.toPersian(value.length()) + " حرف بود.");
        }
        return value;
    }

    static Integer years(String raw) {
        String value = PersianDigits.toAscii(raw.trim());
        if (CLEAR.equals(value)) {
            return 0;
        }
        try {
            int years = Integer.parseInt(value);
            if (years >= 0 && years <= MAX_YEARS) {
                return years;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the message below
        }
        throw new InvalidProfileInputException(
                "فقط یه عدد بین ۰ تا " + PersianDigits.toPersian(MAX_YEARS) + " بفرست.");
    }

    static Set<String> list(String raw, int maxItems, int maxItemLength) {
        if (CLEAR.equals(raw.trim())) {
            return Set.of();
        }
        Set<String> items = new LinkedHashSet<>();
        for (String part : LIST_SEPARATOR.split(raw)) {
            String item = part.trim();
            if (item.isEmpty()) {
                continue;
            }
            if (item.length() > maxItemLength) {
                throw new InvalidProfileInputException(
                        "«" + item + "» خیلی بلنده. هر مورد حداکثر "
                                + PersianDigits.toPersian(maxItemLength) + " حرف می‌تونه باشه.");
            }
            items.add(item);
        }
        if (items.size() > maxItems) {
            throw new InvalidProfileInputException(
                    "حداکثر " + PersianDigits.toPersian(maxItems) + " مورد می‌تونی بنویسی.");
        }
        return items;
    }

    static String website(String raw) {
        String value = text(raw, 500);
        if (value.isEmpty()) {
            return value;
        }
        String withScheme = value.matches("(?i)^https?://.*") ? value : "https://" + value;
        try {
            URI uri = new URI(withScheme);
            if (uri.getHost() != null && uri.getHost().contains(".")) {
                return uri.toString();
            }
        } catch (URISyntaxException ignored) {
            // falls through to the message below
        }
        throw new InvalidProfileInputException("آدرس وب‌سایت معتبر نیست. مثلاً example.com رو بفرست.");
    }

    static String instagram(String raw) {
        String value = text(raw, 100);
        if (value.isEmpty()) {
            return value;
        }
        String name = INSTAGRAM_PREFIX.matcher(value).replaceFirst("")
                .replaceFirst("^@", "")
                .replaceFirst("/+$", "");
        if (!INSTAGRAM_NAME.matcher(name).matches()) {
            throw new InvalidProfileInputException(
                    "نام کاربری اینستاگرام معتبر نیست. فقط حرف انگلیسی، عدد، نقطه و زیرخط مجازه.");
        }
        return name;
    }
}
