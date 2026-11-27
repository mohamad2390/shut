package com.shut.telegram.business;

import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.rental.RentalCallbacks;
import com.shut.telegram.studio.StudioCallbacks;
import com.shut.telegram.venue.VenueCallbacks;

/** The screens of the business section. */
final class BusinessScreens {

    private BusinessScreens() {
    }

    /** Which categories a member already works in; they get a check mark. */
    record Held(boolean freelancer, boolean venue, boolean studio, boolean rental) {
    }

    static Screen home(Held held) {
        String text = "💼 ورود به‌عنوان کسب‌وکار\n\n"
                + "توی کدوم دسته می‌خوای فعالیت کنی؟ می‌تونی توی چند دسته فعال باشی، و هر دسته رو هر وقت خواستی "
                + "جداگانه ثبت‌نام کنی؛ لازم نیست همین الان همه رو بزنی.";
        return new Screen(text, new KeyboardBuilder()
                .row(InlineKeyboardButton.callback(mark(held.freelancer()) + "🎥 فریلنسر (عکاس، فیلمبردار، تدوینگر…)",
                        held.freelancer() ? MenuCallbacks.ME : MenuCallbacks.REGISTER))
                .row(InlineKeyboardButton.callback(mark(held.studio()) + "🏢 استودیودار", StudioCallbacks.OWNER_HOME))
                .row(InlineKeyboardButton.callback(mark(held.venue()) + "🏰 عمارت‌دار (مکان عکاسی)", BusinessCallbacks.VENUE))
                .row(InlineKeyboardButton.callback(mark(held.rental()) + "🎒 اجاره‌دهندهٔ تجهیزات", RentalCallbacks.OWNER_HOME))
                .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                .build());
    }

    static Screen venue(boolean hasVenues) {
        String text = "🏰 عمارت‌دار\n\nاز اینجا مکان‌هات رو ثبت و مدیریت می‌کنی: عکس، قیمت‌ها، تقویم و درخواست‌های رزرو.";
        return new Screen(text, new KeyboardBuilder()
                .row(hasVenues
                        ? InlineKeyboardButton.callback("📍 مکان‌های من", VenueCallbacks.OWNER_HOME)
                        : InlineKeyboardButton.callback("➕ ثبت اولین مکان", VenueCallbacks.OWNER_NEW))
                .row(InlineKeyboardButton.callback("↩️ دسته‌ها", BusinessCallbacks.HOME))
                .build());
    }

    private static String mark(boolean held) {
        return held ? "✅ " : "";
    }
}
