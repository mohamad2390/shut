package com.shut.telegram.menu;

import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;

/** The main menu: one button per section of the bot. */
public final class MainMenuScreen {

    private MainMenuScreen() {
    }

    public static Screen build(String intro) {
        return new Screen(
                intro + "\n\nیکی از بخش‌ها رو انتخاب کن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("🖼 ویترین", MenuCallbacks.SHOWCASE),
                                InlineKeyboardButton.callback("📅 تقویم", MenuCallbacks.CALENDAR))
                        .row(InlineKeyboardButton.callback("💰 قیمت‌ها", MenuCallbacks.PRICING),
                                InlineKeyboardButton.callback("👤 پروفایل", MenuCallbacks.PROFILE))
                        .build());
    }

    /** A screen for a section that is not built yet, with a way back to the menu. */
    public static Screen comingSoon(String title) {
        return new Screen(
                title + "\n\nاین بخش هنوز آماده نیست و به‌زودی اضافه می‌شه.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                        .build());
    }
}
