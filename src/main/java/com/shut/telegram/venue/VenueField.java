package com.shut.telegram.venue;

import com.shut.common.text.PersianDigits;
import com.shut.venue.Venue;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/** The venue details the owner types as text. The type and the price unit are picked from buttons instead. */
enum VenueField {
    NAME("نام", "نام مکان رو بفرست.", false,
            (venue, raw) -> venue.setName(text(raw, 100))),
    CITY("شهر", "شهر مکان رو بفرست.", true,
            (venue, raw) -> venue.setCity(optionalText(raw, 100))),
    ADDRESS("آدرس", "آدرس کامل مکان رو بفرست.", true,
            (venue, raw) -> venue.setAddress(optionalText(raw, 300))),
    DESCRIPTION("توضیحات", "توضیحات مکان رو بفرست: فضا، متراژ، سبک، مناسب چه عکس‌هایی.", true,
            (venue, raw) -> venue.setDescription(optionalText(raw, 1000))),
    AMENITIES("امکانات", "امکانات رو بنویس (مثلاً پارکینگ، سرویس بهداشتی، برق، اتاق گریم).", true,
            (venue, raw) -> venue.setAmenities(optionalText(raw, 500))),
    PHONE("تلفن", "شماره‌ی تماس رو بفرست.", true,
            (venue, raw) -> venue.setPhone(phone(raw)));

    private static final String CLEAR_HINT = "\n\nبرای پاک کردن این بخش «-» بفرست.\nبرای انصراف /cancel رو بزن.";
    private static final String CANCEL_HINT = "\n\nبرای انصراف /cancel رو بزن.";
    private static final Pattern PHONE_PATTERN = Pattern.compile("[+0-9][0-9 \\-]{3,28}");

    private final String label;
    private final String question;
    private final boolean clearable;
    private final BiConsumer<Venue, String> apply;

    VenueField(String label, String question, boolean clearable, BiConsumer<Venue, String> apply) {
        this.label = label;
        this.question = question;
        this.clearable = clearable;
        this.apply = apply;
    }

    String label() {
        return label;
    }

    String prompt() {
        return question + (clearable ? CLEAR_HINT : CANCEL_HINT);
    }

    /** @throws InvalidVenueInputException if the text cannot be used for this field */
    void applyTo(Venue venue, String raw) {
        apply.accept(venue, raw);
    }

    static Optional<VenueField> fromKey(String key) {
        return Arrays.stream(values()).filter(field -> field.name().equals(key)).findFirst();
    }

    private static String text(String raw, int maxLength) {
        String value = raw.strip().replaceAll("\\s+", " ");
        if (value.isEmpty()) {
            throw new InvalidVenueInputException("این بخش نمی‌تونه خالی باشه.");
        }
        if (value.length() > maxLength) {
            throw new InvalidVenueInputException("خیلی بلنده. حداکثر " + PersianDigits.toPersian(maxLength) + " حرف بفرست.");
        }
        return value;
    }

    /** Like {@link #text} but "-" clears the value. */
    private static String optionalText(String raw, int maxLength) {
        return raw.strip().equals("-") ? null : text(raw, maxLength);
    }

    private static String phone(String raw) {
        if (raw.strip().equals("-")) {
            return null;
        }
        String value = PersianDigits.toAscii(raw.strip());
        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new InvalidVenueInputException("شماره معتبر نیست. فقط عدد بفرست، مثلاً ۰۹۱۲۱۲۳۴۵۶۷.");
        }
        return value;
    }
}
