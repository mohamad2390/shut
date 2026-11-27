package com.shut.telegram.rental;

import com.shut.equipment.EquipmentModel;
import com.shut.rental.RentalItem;
import com.shut.rental.RentalService;
import com.shut.rental.RentalShop;
import com.shut.telegram.business.BusinessCallbacks;
import com.shut.telegram.equipment.EquipmentCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

import static com.shut.common.text.PersianDigits.toPersian;

/** The rental owner's screens: registering, managing the shop, its items and their photos. */
final class RentalOwnerScreens {

    private RentalOwnerScreens() {
    }

    static Screen intro() {
        return new Screen("🎒 مغازهٔ اجارهٔ من\n\nهنوز مغازه‌ای ثبت نکردی. با ثبت مغازه، اسم، تلفن و آدرست و تجهیزاتی که "
                + "اجاره می‌دی با قیمت روزانه برای همه نمایش داده می‌شه. ثبت‌نام رایگانه.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("➕ ثبت مغازه", RentalOwnerCallbacks.NEW))
                        .row(InlineKeyboardButton.callback("↩️ دسته‌ها", BusinessCallbacks.HOME))
                        .build());
    }

    static Screen gone() {
        return new Screen("مغازه یا تجهیزات پیدا نشد.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("🎒 مغازهٔ من", RentalOwnerCallbacks.MINE)).build());
    }

    /** Asks one detail; during registration the question says how far along the member is. */
    static Screen ask(RentalField field, boolean registration) {
        String title = registration
                ? "🎒 ثبت مغازه (" + toPersian(index(field) + 1) + " از " + toPersian(RentalField.REGISTRATION.length) + ")"
                : "✏️ ویرایش " + field.label();
        return new Screen(title + "\n\n" + field.prompt() + "\n\nبرای انصراف /cancel رو بزن.", cancelTo(RentalOwnerCallbacks.MINE));
    }

    static Screen manage(RentalShop shop, boolean listed, long items, long available) {
        String status = listed ? "✅ مغازه برای همه نمایش داده می‌شه."
                : "⚠️ هنوز نمایش داده نمی‌شه: " + whatIsMissing(shop, available);
        InlineKeyboardMarkup keyboard = new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("✏️ اسم", RentalOwnerCallbacks.edit(RentalField.NAME)),
                        InlineKeyboardButton.callback("📞 تلفن", RentalOwnerCallbacks.edit(RentalField.PHONE)))
                .row(InlineKeyboardButton.callback("📍 شهر", RentalOwnerCallbacks.edit(RentalField.CITY)),
                        InlineKeyboardButton.callback("🏠 آدرس", RentalOwnerCallbacks.edit(RentalField.ADDRESS)))
                .row(InlineKeyboardButton.callback("📝 توضیحات", RentalOwnerCallbacks.edit(RentalField.DESCRIPTION)),
                        InlineKeyboardButton.callback("🎥 تجهیزات (" + toPersian(items) + ")", RentalOwnerCallbacks.ITEMS))
                .row(InlineKeyboardButton.callback(shop.isActive() ? "⏸ پنهان کردن موقت" : "▶️ نمایش دوباره",
                        RentalOwnerCallbacks.TOGGLE))
                .row(listed ? InlineKeyboardButton.callback("👁 صفحهٔ عمومی", RentalCallbacks.shop(shop.getId()))
                        : InlineKeyboardButton.callback("🔄 بررسی دوباره", RentalOwnerCallbacks.MINE),
                        InlineKeyboardButton.callback("🗑 حذف مغازه", RentalOwnerCallbacks.DELETE))
                .row(InlineKeyboardButton.callback("↩️ اجارهٔ تجهیزات", RentalCallbacks.HOME))
                .build();
        return new Screen(RentalTexts.shop(shop, available) + "\n\n" + status, keyboard);
    }

    private static String whatIsMissing(RentalShop shop, long available) {
        List<String> missing = new ArrayList<>();
        if (!shop.isActive()) {
            missing.add("پنهان شده");
        }
        if (!shop.hasContactDetails()) {
            missing.add("تلفن، شهر و آدرس کامل نیست");
        }
        if (available == 0) {
            missing.add("حداقل یک مورد قابل اجاره اضافه کن");
        }
        return String.join("؛ ", missing) + ".";
    }

    static Screen confirmDelete(RentalShop shop) {
        return new Screen("مغازهٔ «" + shop.getName() + "» با همهٔ تجهیزات و عکس‌هاش حذف بشه؟ این کار برگشت نداره.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("🗑 بله، حذف کن", RentalOwnerCallbacks.CONFIRM_DELETE),
                                InlineKeyboardButton.callback("انصراف", RentalOwnerCallbacks.MINE))
                        .build());
    }

    static Screen items(List<RentalItem> items) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text = items.isEmpty()
                ? "🎥 تجهیزات\n\nهنوز چیزی اضافه نکردی. تجهیزت رو از فهرست (نوع، برند، مدل) انتخاب کن "
                        + "و قیمت اجارهٔ روزانه‌ش رو بگذار؛ اگه توی فهرست نبود، خودت اضافه‌ش کن."
                : "🎥 تجهیزات\n\nروی هر مورد بزن تا مدیریتش کنی. ⏸ یعنی فعلاً برای اجاره نیست.";
        items.forEach(item -> keyboard.row(InlineKeyboardButton.callback(
                (item.isAvailable() ? "" : "⏸ ") + RentalTexts.itemLine(item), RentalOwnerCallbacks.item(item.getId()))));
        if (items.size() < RentalService.MAX_ITEMS) {
            keyboard.row(InlineKeyboardButton.callback("➕ افزودن تجهیزات", EquipmentCallbacks.choose(RentalItemChoice.PURPOSE)));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ مغازهٔ من", RentalOwnerCallbacks.MINE));
        return new Screen(text, keyboard.build());
    }

    static Screen item(RentalItem item, long photoCount) {
        String text = "🎥 " + item.getTitle() + " (" + item.getModel().getType().getName() + ")" + (item.getDescription() == null ? "" : "\n\n" + item.getDescription())
                + "\n\n💰 " + RentalTexts.daily(item.getDailyPrice()) + "\n🖼 " + toPersian(photoCount) + " عکس\n"
                + (item.isAvailable() ? "✅ برای اجاره موجوده" : "⏸ فعلاً برای اجاره نیست");
        return new Screen(text, new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("💰 تغییر قیمت", RentalOwnerCallbacks.editPrice(item.getId())),
                        InlineKeyboardButton.callback(item.isAvailable() ? "⏸ ناموجود" : "▶️ موجود شد",
                                RentalOwnerCallbacks.toggleItem(item.getId())))
                .row(InlineKeyboardButton.callback("➕ عکس", RentalOwnerCallbacks.addPhoto(item.getId())),
                        InlineKeyboardButton.callback("🗑 حذف عکس‌ها", RentalOwnerCallbacks.clearPhotos(item.getId())))
                .row(InlineKeyboardButton.callback("❌ حذف این مورد", RentalOwnerCallbacks.removeItem(item.getId())))
                .row(InlineKeyboardButton.callback("↩️ تجهیزات", RentalOwnerCallbacks.ITEMS))
                .build());
    }

    static Screen askItemPrice(EquipmentModel model) {
        return new Screen("🎥 " + model.getType().getIcon() + " " + model.displayName() + " (۱ از ۲)\n\n"
                + "قیمت اجارهٔ یک روز رو به تومان بفرست؛ مثلاً ۵۰۰٬۰۰۰.\n\nبرای انصراف /cancel رو بزن.",
                cancelTo(RentalOwnerCallbacks.ITEMS));
    }

    static Screen askItemDescription() {
        return new Screen("🎥 تجهیزات تازه (۲ از ۲)\n\nتوضیح کوتاه بفرست: لوازم همراه، شرایط، بیعانه. اگه نمی‌خوای «-» بفرست.",
                cancelTo(RentalOwnerCallbacks.ITEMS));
    }

    static Screen askNewPrice(RentalItem item) {
        return new Screen("قیمت تازهٔ اجارهٔ روزانهٔ «" + item.getTitle() + "» رو به تومان بفرست.\n\nبرای انصراف /cancel رو بزن.",
                cancelTo(RentalOwnerCallbacks.item(item.getId())));
    }

    static Screen addPhotos(RentalItem item, long count) {
        return new Screen("🖼 عکس «" + item.getTitle() + "» رو بفرست. الان " + toPersian(count) + " از "
                + toPersian(RentalService.MAX_PHOTOS_PER_ITEM) + " عکس داره. وقتی تموم شد «تمام» رو بزن.",
                done(item));
    }

    static Screen photoSaved(RentalItem item, long count) {
        return new Screen("✅ ذخیره شد (" + toPersian(count) + " از " + toPersian(RentalService.MAX_PHOTOS_PER_ITEM)
                + "). بازم بفرست یا «تمام» رو بزن.", done(item));
    }

    static Screen photoRejected(RentalItem item, String reason) {
        return new Screen(reason, done(item));
    }

    private static int index(RentalField field) {
        for (int i = 0; i < RentalField.REGISTRATION.length; i++) {
            if (RentalField.REGISTRATION[i] == field) {
                return i;
            }
        }
        return 0;
    }

    private static InlineKeyboardMarkup done(RentalItem item) {
        return new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("✅ تمام", RentalOwnerCallbacks.item(item.getId()))).build();
    }

    private static InlineKeyboardMarkup cancelTo(String callback) {
        return new KeyboardBuilder().row(InlineKeyboardButton.callback("انصراف", callback)).build();
    }
}
