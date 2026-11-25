package com.shut.telegram.menu;

import com.shut.telegram.browse.BrowseCallbacks;
import com.shut.telegram.market.MarketCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.project.ProjectCallbacks;
import com.shut.telegram.venue.VenueCallbacks;

/** The main menu: my info, member search and tender projects. */
public final class MainMenuScreen {

    private MainMenuScreen() {
    }

    public static Screen build(String intro) {
        return new Screen(
                intro + "\n\nیکی از بخش‌ها رو انتخاب کن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("👤 اطلاعات من", MenuCallbacks.ME))
                        .row(InlineKeyboardButton.callback("🔎 جستجوی متخصص", BrowseCallbacks.START))
                        .row(InlineKeyboardButton.callback("📋 پروژه‌های مناقصه‌ای", ProjectCallbacks.HOME))
                        .row(InlineKeyboardButton.callback("📍 مکان‌های عکاسی", VenueCallbacks.HOME))
                        .row(InlineKeyboardButton.callback("🛒 بازار تجهیزات", MarketCallbacks.HOME))
                        .build());
    }
}
