package com.shut.telegram.rental;

import com.shut.common.text.PersianDigits;
import com.shut.rental.RentalItem;
import com.shut.rental.RentalShop;

/** Texts about rental shops and their items. */
final class RentalTexts {

    private RentalTexts() {
    }

    /** "۵۰۰٬۰۰۰ تومان در روز". */
    static String daily(long amount) {
        return PersianDigits.toPersianGrouped(amount) + " تومان در روز";
    }

    static String listLine(RentalShop shop) {
        return shop.getName() + (shop.getCity() == null ? "" : " — " + shop.getCity());
    }

    static String itemLine(RentalItem item) {
        return item.getModel().getType().getIcon() + " " + item.getTitle() + " — " + daily(item.getDailyPrice());
    }

    /** The shop's page; {@code items} is how many items customers can ask about. */
    static String shop(RentalShop shop, long items) {
        StringBuilder text = new StringBuilder("🎒 ").append(shop.getName()).append("\n");
        if (shop.getPhone() != null) {
            text.append("\n📞 ").append(PersianDigits.toPersian(shop.getPhone()));
        }
        if (shop.getCity() != null || shop.getAddress() != null) {
            text.append("\n📍 ").append(shop.getCity() == null ? shop.getAddress()
                    : shop.getAddress() == null ? shop.getCity() : shop.getCity() + "، " + shop.getAddress());
        }
        if (shop.getDescription() != null) {
            text.append("\n\n").append(shop.getDescription());
        }
        text.append("\n\n🎥 ").append(PersianDigits.toPersian(items)).append(" مورد برای اجاره");
        return text.toString();
    }

    /** An item's page for a customer: what it is, the price, and where to go to rent it. */
    static String item(RentalItem item, RentalShop shop) {
        StringBuilder text = new StringBuilder("🎥 ").append(item.getTitle())
                .append(" (").append(item.getModel().getType().getName()).append(")\n");
        if (item.getDescription() != null) {
            text.append("\n").append(item.getDescription()).append("\n");
        }
        text.append("\n💰 ").append(daily(item.getDailyPrice()))
                .append("\n\n🏪 ").append(shop.getName());
        if (shop.getPhone() != null) {
            text.append("\n📞 ").append(PersianDigits.toPersian(shop.getPhone()));
        }
        if (shop.getAddress() != null) {
            text.append("\n📍 ").append(shop.getCity() == null ? shop.getAddress() : shop.getCity() + "، " + shop.getAddress());
        }
        text.append("\n\nبرای اجاره، تماس بگیر یا به آدرس مغازه مراجعه کن.");
        return text.toString();
    }
}
