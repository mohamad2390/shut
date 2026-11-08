package com.shut.telegram.profile;

import com.shut.profile.dto.ProfessionalProfileRequest;

import java.util.Optional;
import java.util.function.BiConsumer;

/** The profile fields a user can edit from the bot: how each is named, asked for and read. */
enum ProfileField {

    DISPLAY_NAME("نام نمایشی", "نام نمایشی‌ات رو بفرست.",
            (draft, raw) -> draft.displayName = ProfileInputParser.text(raw, 150)),
    HEADLINE("عنوان حرفه‌ای", "عنوان حرفه‌ای‌ات رو بفرست. مثلاً «عکاس پرتره و مد».",
            (draft, raw) -> draft.headline = ProfileInputParser.text(raw, 180)),
    BIO("درباره‌ی من", "چند خط درباره‌ی خودت و کارت بنویس.",
            (draft, raw) -> draft.bio = ProfileInputParser.text(raw, 1000)),
    YEARS("سابقه", "چند سال سابقه‌ی کاری داری؟ فقط عدد بفرست.",
            (draft, raw) -> draft.yearsExperience = ProfileInputParser.years(raw)),
    BASE_CITY("شهر", "شهر محل کارت رو بفرست.",
            (draft, raw) -> draft.baseCity = ProfileInputParser.text(raw, 100)),
    BASE_COUNTRY("کشور", "کشور محل کارت رو بفرست.",
            (draft, raw) -> draft.baseCountry = ProfileInputParser.text(raw, 100)),
    SKILLS("مهارت‌ها", "مهارت‌هات رو بفرست و با ویرگول از هم جدا کن.\nمثلاً: عکاسی پرتره، تدوین، ادیت عکس",
            (draft, raw) -> draft.skills = ProfileInputParser.list(raw, 15, 50)),
    SERVICE_CITIES("شهرهای فعالیت", "شهرهایی که برای کار بهشون می‌ری رو بفرست و با ویرگول از هم جدا کن.",
            (draft, raw) -> draft.serviceCities = ProfileInputParser.list(raw, 15, 50)),
    COLLABORATORS("همکاری‌ها", "اسم استودیوها و افرادی که باهاشون کار کردی رو بفرست و با ویرگول از هم جدا کن.",
            (draft, raw) -> draft.collaborators = ProfileInputParser.list(raw, 15, 60)),
    EQUIPMENT("تجهیزات", "تجهیزاتت رو بنویس.",
            (draft, raw) -> draft.equipmentDescription = ProfileInputParser.text(raw, 500)),
    WEBSITE("وب‌سایت", "آدرس وب‌سایتت رو بفرست.",
            (draft, raw) -> draft.websiteUrl = ProfileInputParser.website(raw)),
    INSTAGRAM("اینستاگرام", "نام کاربری اینستاگرامت رو بفرست.",
            (draft, raw) -> draft.instagramUsername = ProfileInputParser.instagram(raw));

    private static final String CLEAR_HINT = "\n\nبرای پاک کردن این بخش «-» بفرست.";

    private final String label;
    private final String question;
    private final BiConsumer<ProfileDraft, String> reader;

    ProfileField(String label, String question, BiConsumer<ProfileDraft, String> reader) {
        this.label = label;
        this.question = question;
        this.reader = reader;
    }

    String label() {
        return label;
    }

    /** The message that asks the user for this field's value. */
    String prompt() {
        return question + CLEAR_HINT;
    }

    /** Reads the user's text into a request that changes only this field. */
    ProfessionalProfileRequest toRequest(String raw) {
        ProfileDraft draft = new ProfileDraft();
        reader.accept(draft, raw);
        return draft.toRequest();
    }

    static Optional<ProfileField> fromKey(String key) {
        try {
            return Optional.of(valueOf(key));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
