package com.shut.telegram.rental;

import com.shut.common.text.PersianDigits;
import com.shut.rental.RentalItem;
import com.shut.rental.RentalShop;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.messaging.TelegramLinks;
import com.shut.telegram.model.InlineKeyboardButton;
import org.springframework.data.domain.Page;

import java.util.List;

/** The pages anyone sees: the rental section, shops, their items, an item, and search results. */
final class RentalScreens {

    private RentalScreens() {
    }

    static Screen home() {
        return new Screen("🎒 اجارهٔ تجهیزات\n\nمغازه‌هایی که دوربین، لنز، نور و بقیهٔ تجهیزات رو اجاره می‌دن. "
                + "ما فقط شما رو به هم وصل می‌کنیم؛ برای اجاره، تماس بگیر یا به آدرس مغازه مراجعه کن. "
                + "اگه تجهیزات اجاره‌ای داری، اینجا ثبتشون کن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("🔎 جستجوی تجهیزات", RentalCallbacks.SEARCH))
                        .row(InlineKeyboardButton.callback("🏪 دیدن مغازه‌ها", RentalCallbacks.list(0)))
                        .row(InlineKeyboardButton.callback("🎒 مغازهٔ من", RentalCallbacks.OWNER_HOME))
                        .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                        .build());
    }

    static Screen askSearch() {
        return new Screen("اسم تجهیزاتی که می‌خوای اجاره کنی رو بفرست؛ مثلاً «سونی» یا «لنز ۸۵».\n\nبرای انصراف /cancel رو بزن.",
                new KeyboardBuilder().row(InlineKeyboardButton.callback("انصراف", RentalCallbacks.HOME)).build());
    }

    /** The first results of a search; each opens the item. */
    static Screen results(String query, Page<RentalItem> items) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text;
        if (items.isEmpty()) {
            text = "چیزی با «" + query + "» پیدا نشد. اسم دیگه‌ای امتحان کن یا مغازه‌ها رو ببین.";
        } else {
            text = "🔎 نتیجهٔ «" + query + "»: " + PersianDigits.toPersian(items.getTotalElements()) + " مورد"
                    + (items.hasNext() ? " (اولین " + PersianDigits.toPersian(items.getNumberOfElements()) + " تا)" : "");
            items.forEach(item -> keyboard.row(InlineKeyboardButton.callback(RentalTexts.itemLine(item),
                    RentalCallbacks.item(item.getId()))));
        }
        keyboard.row(InlineKeyboardButton.callback("🔎 جستجوی دیگه", RentalCallbacks.SEARCH));
        keyboard.row(InlineKeyboardButton.callback("↩️ اجارهٔ تجهیزات", RentalCallbacks.HOME));
        return new Screen(text, keyboard.build());
    }

    static Screen list(Page<RentalShop> shops) {
        if (shops.isEmpty()) {
            return new Screen("هنوز مغازه‌ای ثبت نشده. اولین نفر باش!", new KeyboardBuilder()
                    .row(InlineKeyboardButton.callback("🎒 ثبت مغازهٔ من", RentalCallbacks.OWNER_HOME))
                    .row(InlineKeyboardButton.callback("↩️ اجارهٔ تجهیزات", RentalCallbacks.HOME))
                    .build());
        }
        KeyboardBuilder keyboard = new KeyboardBuilder();
        shops.forEach(shop -> keyboard.row(InlineKeyboardButton.callback(RentalTexts.listLine(shop),
                RentalCallbacks.shop(shop.getId()))));
        if (shops.hasPrevious()) {
            keyboard.row(InlineKeyboardButton.callback("▶️ قبلی", RentalCallbacks.list(shops.getNumber() - 1)));
        }
        if (shops.hasNext()) {
            keyboard.row(InlineKeyboardButton.callback("بعدی ◀️", RentalCallbacks.list(shops.getNumber() + 1)));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ اجارهٔ تجهیزات", RentalCallbacks.HOME));
        return new Screen("🏪 " + PersianDigits.toPersian(shops.getTotalElements())
                + " مغازه\n\nروی هر مغازه بزن تا تجهیزاتش رو ببینی.", keyboard.build());
    }

    /** A shop's page. {@code ownerUsername} is null when the owner has no Telegram username. */
    static Screen shop(RentalShop shop, long items, String ownerUsername) {
        KeyboardBuilder keyboard = new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("🎥 تجهیزات برای اجاره", RentalCallbacks.items(shop.getId())));
        TelegramLinks.messageButton(ownerUsername).ifPresent(keyboard::row);
        keyboard.row(InlineKeyboardButton.callback("↩️ لیست مغازه‌ها", RentalCallbacks.list(0)));
        return new Screen(RentalTexts.shop(shop, items), keyboard.build());
    }

    static Screen items(RentalShop shop, List<RentalItem> items) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        items.forEach(item -> keyboard.row(InlineKeyboardButton.callback(RentalTexts.itemLine(item),
                RentalCallbacks.item(item.getId()))));
        keyboard.row(InlineKeyboardButton.callback("↩️ صفحهٔ مغازه", RentalCallbacks.shop(shop.getId())));
        return new Screen("🎥 تجهیزات " + shop.getName() + "\n\nروی هر مورد بزن تا جزئیاتش رو ببینی.", keyboard.build());
    }

    static Screen item(RentalItem item, RentalShop shop, long photoCount, String ownerUsername) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (photoCount > 0) {
            keyboard.row(InlineKeyboardButton.callback("🖼 عکس‌ها", RentalCallbacks.photos(item.getId())));
        }
        TelegramLinks.messageButton(ownerUsername).ifPresent(keyboard::row);
        keyboard.row(InlineKeyboardButton.callback("🏪 صفحهٔ مغازه", RentalCallbacks.shop(shop.getId())),
                InlineKeyboardButton.callback("↩️ تجهیزات", RentalCallbacks.items(shop.getId())));
        return new Screen(RentalTexts.item(item, shop), keyboard.build());
    }

    static Screen gone() {
        return new Screen("این مورد دیگه در دسترس نیست.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("↩️ اجارهٔ تجهیزات", RentalCallbacks.HOME))
                .build());
    }
}
