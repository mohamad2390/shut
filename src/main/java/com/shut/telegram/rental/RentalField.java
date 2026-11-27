package com.shut.telegram.rental;

import com.shut.auth.InvalidPhoneException;
import com.shut.auth.PhoneNumbers;
import com.shut.common.text.PersianDigits;
import com.shut.rental.RentalShop;

import java.util.Optional;

/** The details of a rental shop the owner can type, with how each is checked. The description can be cleared with "-". */
enum RentalField {
    NAME("اسم مغازه", "اسم مغازه یا مجموعهٔ اجاره رو بفرست.", 120, true),
    PHONE("تلفن", "شماره‌ای که مشتری‌ها باهاش تماس می‌گیرن رو بفرست؛ مثلاً ۰۹۱۲۱۲۳۴۵۶۷.", 0, true),
    CITY("شهر", "مغازه توی کدوم شهره؟", 100, true),
    ADDRESS("آدرس", "آدرس دقیق مغازه رو بفرست؛ مشتری‌ها برای گرفتن و تحویل تجهیزات به همین آدرس مراجعه می‌کنن.", 300, true),
    DESCRIPTION("توضیحات", "چند خط دربارهٔ مغازه بنویس: ساعت کار، شرایط اجاره، بیعانه… برای پاک کردن «-» بفرست.", 2000, false);

    /** The details asked one after another when a shop is registered. */
    static final RentalField[] REGISTRATION = {NAME, PHONE, CITY, ADDRESS};

    private final String label;
    private final String prompt;
    private final int maxLength;
    private final boolean required;

    RentalField(String label, String prompt, int maxLength, boolean required) {
        this.label = label;
        this.prompt = prompt;
        this.maxLength = maxLength;
        this.required = required;
    }

    String label() {
        return label;
    }

    String prompt() {
        return prompt;
    }

    static Optional<RentalField> fromKey(String key) {
        try {
            return Optional.of(valueOf(key));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    /** The field asked after this one during registration, or empty after the last. */
    Optional<RentalField> nextInRegistration() {
        for (int i = 0; i < REGISTRATION.length - 1; i++) {
            if (REGISTRATION[i] == this) {
                return Optional.of(REGISTRATION[i + 1]);
            }
        }
        return Optional.empty();
    }

    /**
     * The text to store: trimmed, phone numbers normalised, and null when an optional field is cleared.
     *
     * @throws InvalidRentalInputException if the text is not valid
     */
    String value(String raw) {
        String text = raw.strip().replaceAll("[ \\t]+", " ");
        if (this == PHONE) {
            try {
                return PhoneNumbers.normalize(text);
            } catch (InvalidPhoneException e) {
                throw new InvalidRentalInputException("شمارهٔ موبایل معتبر نیست؛ مثلاً ۰۹۱۲۱۲۳۴۵۶۷.");
            }
        }
        if (!required && (text.isEmpty() || text.equals("-"))) {
            return null;
        }
        if (text.isEmpty() || text.length() > maxLength) {
            throw new InvalidRentalInputException(label + " باید بین ۱ تا " + PersianDigits.toPersian(maxLength) + " حرف باشه.");
        }
        return text;
    }

    void applyTo(RentalShop shop, String raw) {
        String value = value(raw);
        switch (this) {
            case NAME -> shop.setName(value);
            case PHONE -> shop.setPhone(value);
            case CITY -> shop.setCity(value);
            case ADDRESS -> shop.setAddress(value);
            case DESCRIPTION -> shop.setDescription(value);
        }
    }
}
