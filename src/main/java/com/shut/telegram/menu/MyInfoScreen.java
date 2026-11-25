package com.shut.telegram.menu;

import com.shut.telegram.messaging.BotLinks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;

/** "My info": the specialist tools for a specialist, an invitation to register for anyone else. */
public final class MyInfoScreen {

    static final String SPECIALIST_TEXT = "👤 اطلاعات من\n\nیکی از بخش‌ها رو انتخاب کن.";
    static final String CUSTOMER_TEXT = "👤 اطلاعات من\n\nهنوز به‌عنوان متخصص ثبت‌نام نکردی. "
            + "با ثبت‌نام، پروفایل، ویترین، تقویم و تعرفه‌هات دیده می‌شه و می‌تونی روی پروژه‌ها پیشنهاد بدی.";

    private MyInfoScreen() {
    }

    public static Screen forSpecialist() {
        return new Screen(SPECIALIST_TEXT, new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("👤 پروفایل", MenuCallbacks.PROFILE),
                        InlineKeyboardButton.callback("📅 تقویم", MenuCallbacks.CALENDAR))
                .row(InlineKeyboardButton.callback("🖼 ویترین", MenuCallbacks.SHOWCASE),
                        InlineKeyboardButton.callback("💰 تعرفه‌ها", MenuCallbacks.PRICING))
                .row(InlineKeyboardButton.callback("🔗 لینک پروفایل من", MenuCallbacks.LINK),
                        InlineKeyboardButton.callback("📱 شماره‌ی موبایل", MenuCallbacks.PHONE))
                .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                .build());
    }

    /** The member's own profile link with a button to share it, or a note when the link cannot be made. */
    static Screen link(java.util.Optional<String> link) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text;
        if (link.isPresent()) {
            text = "🔗 لینک پروفایل تو\n\n" + link.get()
                    + "\n\nهر عضو کانال که این لینک رو باز کنه، مستقیم پروفایل، ویترین، تقویم و تعرفه‌هات رو می‌بینه.";
            keyboard.row(BotLinks.shareButton(link.get(), "پروفایل حرفه‌ای من"));
        } else {
            text = "الان نمی‌تونم لینک رو بسازم. کمی بعد دوباره امتحان کن.";
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ اطلاعات من", MenuCallbacks.ME));
        return new Screen(text, keyboard.build());
    }

    public static Screen forCustomer() {
        return new Screen(CUSTOMER_TEXT, new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("📝 ثبت‌نام به‌عنوان متخصص", MenuCallbacks.REGISTER))
                .row(InlineKeyboardButton.callback("📱 شماره‌ی موبایل", MenuCallbacks.PHONE))
                .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                .build());
    }
}
