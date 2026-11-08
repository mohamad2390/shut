package com.shut.telegram.profile;

import com.shut.common.text.PersianDigits;
import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.InlineKeyboardMarkup;
import com.shut.telegram.model.TelegramUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** The profile page: the user's details as text and one edit button per field. */
final class ProfileScreen {

    private static final String EMPTY_PROFILE =
            "هنوز پروفایل حرفه‌ای نساخته‌ای. هر بخش رو که خواستی با دکمه‌های زیر پر کن، "
                    + "تا بقیه‌ی اعضای کانال با کارت آشنا بشن.";

    private ProfileScreen() {
    }

    /** {@code profile} is null when the user has not saved anything yet. */
    static Screen build(ProfessionalProfileResponse profile, TelegramUser from) {
        return new Screen(profile == null ? emptyText(from) : text(profile, from), keyboard());
    }

    private static String emptyText(TelegramUser from) {
        return "👤 " + fallbackName(from) + "\n\n" + EMPTY_PROFILE;
    }

    private static String text(ProfessionalProfileResponse profile, TelegramUser from) {
        List<String> lines = new ArrayList<>();
        lines.add("👤 " + (profile.displayName() != null ? profile.displayName() : fallbackName(from)));
        if (profile.headline() != null) {
            lines.add(profile.headline());
        }
        lines.add("");

        if (profile.yearsExperience() != null && profile.yearsExperience() > 0) {
            lines.add("سابقه: " + PersianDigits.toPersian(profile.yearsExperience()) + " سال");
        }
        addJoined(lines, "شهر", Arrays.asList(profile.baseCity(), profile.baseCountry()));
        addJoined(lines, "مهارت‌ها", profile.skills());
        addJoined(lines, "شهرهای فعالیت", profile.serviceCities());
        addJoined(lines, "همکاری‌ها", profile.collaborators());
        if (profile.equipmentDescription() != null) {
            lines.add("تجهیزات: " + profile.equipmentDescription());
        }
        if (profile.websiteUrl() != null) {
            lines.add("وب‌سایت: " + profile.websiteUrl());
        }
        if (profile.instagramUsername() != null) {
            lines.add("اینستاگرام: @" + profile.instagramUsername());
        }
        if (profile.bio() != null) {
            lines.add("");
            lines.add("درباره:");
            lines.add(profile.bio());
        }
        return String.join("\n", lines);
    }

    /** Adds "label: a، b، c" when at least one value is present. */
    private static void addJoined(List<String> lines, String label, Collection<String> values) {
        String joined = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("، "));
        if (!joined.isEmpty()) {
            lines.add(label + ": " + joined);
        }
    }

    private static String fallbackName(TelegramUser from) {
        String name = (nullToEmpty(from.firstName()) + " " + nullToEmpty(from.lastName())).trim();
        return name.isEmpty() ? "بدون نام" : name;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static InlineKeyboardMarkup keyboard() {
        List<InlineKeyboardButton> editButtons = Arrays.stream(ProfileField.values())
                .map(field -> InlineKeyboardButton.callback("✏️ " + field.label(), ProfileCallbacks.edit(field)))
                .toList();
        return new KeyboardBuilder()
                .grid(editButtons, 2)
                .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                .build();
    }
}
