package com.shut.telegram.studio;

import com.shut.auth.InvalidPhoneException;
import com.shut.auth.PhoneNumbers;
import com.shut.common.text.PersianDigits;
import com.shut.studio.Studio;

import java.util.Optional;

/** The details of a studio the owner can type, with how each is checked. The description can be cleared with "-". */
enum StudioField {
    NAME("اسم استودیو", "اسم استودیو رو بفرست.", 120, true),
    MANAGER("اسم مدیر", "اسم مدیر استودیو رو بفرست.", 100, true),
    PHONE("تلفن", "شماره‌ای که مشتری‌ها باهاش تماس می‌گیرن رو بفرست؛ مثلاً ۰۹۱۲۱۲۳۴۵۶۷.", 0, true),
    CITY("شهر", "استودیو توی کدوم شهره؟", 100, true),
    ADDRESS("آدرس", "آدرس دقیق استودیو رو بفرست؛ مشتری‌ها برای اطمینان به همین آدرس مراجعه می‌کنن.", 300, true),
    DESCRIPTION("توضیحات", "چند خط دربارهٔ استودیو بنویس: سابقه، سبک کار، امکانات. برای پاک کردن «-» بفرست.", 2000, false);

    /** The details asked one after another when a studio is registered. */
    static final StudioField[] REGISTRATION = {NAME, MANAGER, PHONE, CITY, ADDRESS};

    private final String label;
    private final String prompt;
    private final int maxLength;
    private final boolean required;

    StudioField(String label, String prompt, int maxLength, boolean required) {
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

    static Optional<StudioField> fromKey(String key) {
        try {
            return Optional.of(valueOf(key));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    /** The field asked after this one during registration, or empty after the last. */
    Optional<StudioField> nextInRegistration() {
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
     * @throws InvalidStudioInputException if the text is not valid
     */
    String value(String raw) {
        String text = raw.strip().replaceAll("[ \\t]+", " ");
        if (this == PHONE) {
            try {
                return PhoneNumbers.normalize(text);
            } catch (InvalidPhoneException e) {
                throw new InvalidStudioInputException("شمارهٔ موبایل معتبر نیست؛ مثلاً ۰۹۱۲۱۲۳۴۵۶۷.");
            }
        }
        if (!required && (text.isEmpty() || text.equals("-"))) {
            return null;
        }
        if (text.isEmpty() || text.length() > maxLength) {
            throw new InvalidStudioInputException(label + " باید بین ۱ تا " + PersianDigits.toPersian(maxLength) + " حرف باشه.");
        }
        return text;
    }

    void applyTo(Studio studio, String raw) {
        String value = value(raw);
        switch (this) {
            case NAME -> studio.setName(value);
            case MANAGER -> studio.setManagerName(value);
            case PHONE -> studio.setPhone(value);
            case CITY -> studio.setCity(value);
            case ADDRESS -> studio.setAddress(value);
            case DESCRIPTION -> studio.setDescription(value);
        }
    }
}
