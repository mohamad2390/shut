package com.shut.telegram.menu;

import com.shut.telegram.business.BusinessCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;

/** The first screen of /start: enter as a visitor, or as a business. */
public final class StartScreen {

    private StartScreen() {
    }

    public static Screen build(String welcome) {
        return new Screen(welcome + "\n\nچطور وارد بشی؟\n\n"
                + "🔍 بازدیدکننده: جستجوی متخصص، دیدن مکان‌های عکاسی و بازار تجهیزات.\n"
                + "💼 کسب‌وکار: فریلنسر، استودیودار، عمارت‌دار یا اجاره‌دهندهٔ تجهیزات؛ می‌تونی توی چند دسته فعال باشی "
                + "و هر دسته رو هر وقت خواستی ثبت‌نام کنی.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("🔍 ورود به‌عنوان بازدیدکننده", MenuCallbacks.HOME))
                        .row(InlineKeyboardButton.callback("💼 ورود به‌عنوان کسب‌وکار", BusinessCallbacks.HOME))
                        .build());
    }
}
