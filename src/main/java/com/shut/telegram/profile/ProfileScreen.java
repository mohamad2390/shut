package com.shut.telegram.profile;

import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.InlineKeyboardMarkup;
import com.shut.telegram.model.TelegramUser;

import java.util.Arrays;
import java.util.List;

/** The profile page: the user's details as text and one edit button per field. */
final class ProfileScreen {

    private static final String EMPTY_PROFILE =
            "هنوز پروفایل حرفه‌ای نساخته‌ای. هر بخش رو که خواستی با دکمه‌های زیر پر کن، "
                    + "تا بقیه‌ی اعضای کانال با کارت آشنا بشن.";

    private ProfileScreen() {
    }

    /** {@code profile} is null when the user has not saved anything yet. */
    static Screen build(ProfessionalProfileResponse profile, TelegramUser from) {
        return new Screen(profile == null ? emptyText(from) : ProfileCard.text(profile, fallbackName(from)), keyboard());
    }

    private static String emptyText(TelegramUser from) {
        return "👤 " + fallbackName(from) + "\n\n" + EMPTY_PROFILE;
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
