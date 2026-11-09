package com.shut.telegram.pricing;

import com.shut.common.text.PersianDigits;
import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingService;
import com.shut.pricing.PricingUnit;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.InlineKeyboardMarkup;

import java.util.Arrays;
import java.util.List;

/** Everything the user sees in the price menu section. */
final class PricingScreens {

    private static final InlineKeyboardMarkup CANCEL_ONLY = new KeyboardBuilder()
            .row(InlineKeyboardButton.callback("انصراف", PricingCallbacks.CANCEL))
            .build();

    private PricingScreens() {
    }

    /** The user's price menu with buttons to add or remove services. */
    static Screen list(List<PriceItem> items) {
        if (items.isEmpty()) {
            return new Screen(
                    "💰 قیمت‌ها\n\nهنوز خدمتی ثبت نکردی. مثلاً «عکاسی» با قیمت هر ساعت، یا «ادیت عکس» با قیمت هر عکس.",
                    new KeyboardBuilder()
                            .row(InlineKeyboardButton.callback("➕ افزودن خدمت", PricingCallbacks.ADD))
                            .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                            .build());
        }

        StringBuilder text = new StringBuilder("💰 قیمت‌ها\n");
        items.forEach(item -> text.append("\n• ").append(describe(item)));
        text.append("\n\nقیمت‌ها به تومان هستن.");

        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (items.size() < PricingService.MAX_ITEMS) {
            keyboard.row(InlineKeyboardButton.callback("➕ افزودن خدمت", PricingCallbacks.ADD));
        }
        keyboard.row(InlineKeyboardButton.callback("🗑 حذف یک خدمت", PricingCallbacks.MANAGE))
                .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME));
        return new Screen(text.toString(), keyboard.build());
    }

    /** One button per service; pressing a button deletes that service. */
    static Screen deleteMenu(List<PriceItem> items) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        items.forEach(item -> keyboard.row(
                InlineKeyboardButton.callback("🗑 " + item.getTitle(), PricingCallbacks.delete(item.getId()))));
        keyboard.row(InlineKeyboardButton.callback("↩️ برگشت", PricingCallbacks.CANCEL));
        return new Screen("کدوم خدمت حذف بشه؟", keyboard.build());
    }

    static Screen askTitle() {
        return new Screen(
                "➕ خدمت جدید\n\nاسم خدمت رو بفرست. مثلاً «عکاسی پرتره» یا «ادیت عکس».",
                CANCEL_ONLY);
    }

    static Screen askUnit(String title) {
        List<InlineKeyboardButton> units = Arrays.stream(PricingUnit.values())
                .map(unit -> InlineKeyboardButton.callback(unit.phrase(), PricingCallbacks.unit(unit)))
                .toList();
        return new Screen(
                "➕ " + title + "\n\nقیمت رو برای چی حساب می‌کنی؟",
                new KeyboardBuilder()
                        .grid(units, 2)
                        .row(InlineKeyboardButton.callback("انصراف", PricingCallbacks.CANCEL))
                        .build());
    }

    static Screen askAmount(String title, PricingUnit unit) {
        return new Screen(
                "➕ " + title + " (" + unit.phrase() + ")\n\nقیمت رو به تومان و فقط با عدد بفرست. مثلاً ۵۰۰۰۰۰",
                CANCEL_ONLY);
    }

    static String describe(PriceItem item) {
        return item.getTitle() + ": " + PersianDigits.toPersianGrouped(item.getAmount())
                + " تومان، " + item.getUnit().phrase();
    }
}
