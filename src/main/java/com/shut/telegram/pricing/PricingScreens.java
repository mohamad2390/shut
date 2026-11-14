package com.shut.telegram.pricing;

import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingCatalog;
import com.shut.pricing.PricingService;
import com.shut.pricing.PricingUnit;
import com.shut.pricing.ServiceType;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.InlineKeyboardMarkup;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
                    "💰 قیمت‌ها\n\nهنوز خدمتی ثبت نکردی. خدمتت رو از لیست انتخاب کن و قیمتش رو بنویس.",
                    new KeyboardBuilder()
                            .row(InlineKeyboardButton.callback("➕ افزودن خدمت", PricingCallbacks.ADD))
                            .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                            .build());
        }

        StringBuilder text = new StringBuilder("💰 قیمت‌ها\n");
        items.forEach(item -> text.append("\n• ").append(PriceText.line(item)));
        text.append("\n\nقیمت‌ها به تومان هستن.");

        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (items.size() < PricingService.MAX_ITEMS) {
            keyboard.row(InlineKeyboardButton.callback("➕ افزودن خدمت", PricingCallbacks.ADD));
        }
        keyboard.row(InlineKeyboardButton.callback("✏️ ویرایش یک خدمت", PricingCallbacks.EDIT))
                .row(InlineKeyboardButton.callback("🗑 حذف یک خدمت", PricingCallbacks.MANAGE))
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

    /** One button per service; pressing a button opens it for editing. */
    static Screen editMenu(List<PriceItem> items) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        items.forEach(item -> keyboard.row(
                InlineKeyboardButton.callback(item.getIcon() + " " + item.getTitle(), PricingCallbacks.item(item.getId()))));
        keyboard.row(InlineKeyboardButton.callback("↩️ برگشت", PricingCallbacks.CANCEL));
        return new Screen("کدوم خدمت رو ویرایش کنیم؟", keyboard.build());
    }

    static Screen item(PriceItem item) {
        return new Screen(
                "✏️ " + PriceText.line(item),
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("💰 تغییر قیمت", PricingCallbacks.editAmount(item.getId())),
                                InlineKeyboardButton.callback("📏 تغییر واحد", PricingCallbacks.editUnit(item.getId())))
                        .row(InlineKeyboardButton.callback("↩️ برگشت", PricingCallbacks.CANCEL))
                        .build());
    }

    static Screen askNewAmount(PriceItem item) {
        return new Screen(
                "💰 " + PriceText.line(item) + "\n\nقیمت تازه رو به تومان و فقط با عدد بفرست. مثلاً ۵۰۰۰۰۰",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("انصراف", PricingCallbacks.item(item.getId())))
                        .build());
    }

    static Screen unitMenu(PriceItem item, Map<PricingUnit, String> unitIcons) {
        List<InlineKeyboardButton> units = Arrays.stream(PricingUnit.values())
                .map(unit -> InlineKeyboardButton.callback(unitLabel(unit, unitIcons), PricingCallbacks.setUnit(item.getId(), unit)))
                .toList();
        return new Screen(
                "📏 " + item.getIcon() + " " + item.getTitle() + "\n\nقیمت رو برای چی حساب می‌کنی؟",
                new KeyboardBuilder()
                        .grid(units, 2)
                        .row(InlineKeyboardButton.callback("انصراف", PricingCallbacks.item(item.getId())))
                        .build());
    }

    /** The services a member can pick from, two to a row, each with its icon. */
    static Screen askService(List<ServiceType> services) {
        List<InlineKeyboardButton> buttons = services.stream()
                .map(service -> InlineKeyboardButton.callback(service.getIcon() + " " + service.getName(),
                        PricingCallbacks.service(service.getId())))
                .toList();
        return new Screen(
                "➕ خدمت جدید\n\nخدمتت رو از لیست انتخاب کن.",
                new KeyboardBuilder()
                        .grid(buttons, 2)
                        .row(InlineKeyboardButton.callback("انصراف", PricingCallbacks.CANCEL))
                        .build());
    }

    /** Asked only for services that let the member name their own price; the name is optional. */
    static Screen askName(ServiceType service) {
        return new Screen(
                "➕ " + service.getIcon() + " " + service.getName()
                        + "\n\nاگه می‌خوای اسم دلخواه برای این خدمت بذاری، همین‌جا بفرست. مثلاً «عکاسی از غذا». اگه نمی‌خوای، «رد کردن» رو بزن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("رد کردن", PricingCallbacks.SKIP_NAME))
                        .row(InlineKeyboardButton.callback("انصراف", PricingCallbacks.CANCEL))
                        .build());
    }

    /** @param name what the price is called: the member's own name, or the name of the service */
    static Screen askUnit(ServiceType service, String name, Map<PricingUnit, String> unitIcons) {
        List<InlineKeyboardButton> units = Arrays.stream(PricingUnit.values())
                .map(unit -> InlineKeyboardButton.callback(unitLabel(unit, unitIcons), PricingCallbacks.unit(unit)))
                .toList();
        return new Screen(
                "➕ " + service.getIcon() + " " + name + "\n\nقیمت رو برای چی حساب می‌کنی؟",
                new KeyboardBuilder()
                        .grid(units, 2)
                        .row(InlineKeyboardButton.callback("انصراف", PricingCallbacks.CANCEL))
                        .build());
    }

    static Screen askAmount(ServiceType service, String name, PricingUnit unit) {
        return new Screen(
                "➕ " + service.getIcon() + " " + name + " (" + unit.phrase() + ")"
                        + "\n\nقیمت رو به تومان و فقط با عدد بفرست. مثلاً ۵۰۰۰۰۰",
                CANCEL_ONLY);
    }

    /** The button text of a unit: its icon from the database, then how it reads, e.g. "⏱ هر ساعت". */
    static String unitLabel(PricingUnit unit, Map<PricingUnit, String> unitIcons) {
        return unitIcons.getOrDefault(unit, PricingCatalog.DEFAULT_UNIT_ICON) + " " + unit.phrase();
    }
}
