package com.shut.telegram.business;

import com.shut.business.BusinessKind;
import com.shut.business.BusinessProfile;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.studio.StudioCallbacks;
import com.shut.telegram.venue.VenueCallbacks;

import java.util.Set;

/** The screens of the business section. */
final class BusinessScreens {

    private BusinessScreens() {
    }

    /** What a member held in each category decides the check marks and where each button leads. */
    record Held(boolean freelancer, boolean venue, boolean studio, Set<BusinessKind> cards) {
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
                .row(InlineKeyboardButton.callback(mark(held.cards().contains(BusinessKind.RENTAL)) + "🎒 اجاره‌دهندهٔ تجهیزات",
                        BusinessCallbacks.open(BusinessKind.RENTAL)))
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

    static Screen card(BusinessKind kind, BusinessProfile profile) {
        StringBuilder text = new StringBuilder(kind.icon() + " " + kind.title() + "\n\n" + profile.getName());
        if (profile.getCity() != null) {
            text.append("\n📍 ").append(profile.getCity());
        }
        if (profile.getDescription() != null) {
            text.append("\n\n").append(profile.getDescription());
        }
        return new Screen(text.toString(), new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("✏️ ویرایش", BusinessCallbacks.edit(kind)))
                .row(InlineKeyboardButton.callback("↩️ دسته‌ها", BusinessCallbacks.HOME))
                .build());
    }

    static Screen askName(BusinessKind kind) {
        return new Screen(kind.icon() + " ثبت‌نام: " + kind.title() + "\n\nاسم " + nameOf(kind)
                + " رو بفرست.\n\nبرای انصراف /cancel رو بزن.", cancel());
    }

    static Screen askCity() {
        return new Screen("شهرت کجاست؟ اگه نمی‌خوای بگی «-» بفرست.", cancel());
    }

    static Screen askDescription() {
        return new Screen("چند خط دربارهٔ تجهیزاتی که اجاره می‌دی بنویس و آدرس مغازه رو هم بگو. "
                + "اگه نمی‌خوای «-» بفرست.", cancel());
    }

    static Screen done(BusinessKind kind, BusinessProfile profile) {
        Screen card = card(kind, profile);
        return new Screen("✅ ثبت شد.\n\n" + card.text(), card.markup());
    }

    private static String nameOf(BusinessKind kind) {
        return "مجموعهٔ اجارهٔ تجهیزاتت";
    }

    private static String mark(boolean held) {
        return held ? "✅ " : "";
    }

    private static com.shut.telegram.model.InlineKeyboardMarkup cancel() {
        return new KeyboardBuilder().row(InlineKeyboardButton.callback("انصراف", BusinessCallbacks.HOME)).build();
    }
}
