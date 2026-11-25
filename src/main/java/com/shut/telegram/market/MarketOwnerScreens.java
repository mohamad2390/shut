package com.shut.telegram.market;

import com.shut.common.text.PersianDigits;
import com.shut.equipment.EquipmentType;
import com.shut.market.Listing;
import com.shut.market.ListingCondition;
import com.shut.market.ListingStatus;
import com.shut.market.MarketService;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;

import java.util.List;
import java.util.Map;

/** The seller's pages: their listings, how to manage one, photos, status and the offers waiting for an answer. */
final class MarketOwnerScreens {

    /** An offer with the buyer's name for the seller to read. */
    record OfferLine(com.shut.market.Offer offer, String buyer) {
    }

    private MarketOwnerScreens() {
    }

    static Screen mine(List<Listing> listings, Map<Long, Long> pending) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text = listings.isEmpty()
                ? "هنوز آگهی‌ای نذاشتی. تجهیزاتی که نمی‌خوای رو اینجا بفروش."
                : "📦 آگهی‌های من\n\nروی هر آگهی بزن تا مدیریتش کنی.";
        listings.forEach(listing -> {
            long waiting = pending.getOrDefault(listing.getId(), 0L);
            String badge = listing.getStatus() == ListingStatus.ACTIVE ? "" : " (" + listing.getStatus().label() + ")";
            keyboard.row(InlineKeyboardButton.callback(MarketTexts.listLine(listing) + badge
                    + (waiting > 0 ? " 💬" + PersianDigits.toPersian(waiting) : ""), MarketOwnerCallbacks.manage(listing.getId())));
        });
        keyboard.row(InlineKeyboardButton.callback("➕ آگهی جدید", MarketOwnerCallbacks.NEW));
        keyboard.row(InlineKeyboardButton.callback("↩️ بازار تجهیزات", MarketCallbacks.HOME));
        return new Screen(text, keyboard.build());
    }

    static Screen limitReached() {
        return new Screen("به سقف " + PersianDigits.toPersian(MarketService.MAX_ACTIVE_LISTINGS_PER_SELLER)
                + " آگهی فعال رسیدی. یکی رو «فروخته شد» یا «بسته» کن و دوباره امتحان کن.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("📦 آگهی‌های من", MarketOwnerCallbacks.MINE))
                .build());
    }

    static Screen gone() {
        return new Screen("این آگهی پیدا نشد.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("📦 آگهی‌های من", MarketOwnerCallbacks.MINE))
                .build());
    }

    static Screen askTitle() {
        return new Screen("➕ آگهی جدید\n\nعنوان آگهی رو بفرست؛ مثلاً «سونی A7 III بدنه».\n\nبرای انصراف /cancel رو بزن.",
                new KeyboardBuilder().row(InlineKeyboardButton.callback("انصراف", MarketOwnerCallbacks.MINE)).build());
    }

    static Screen askPrice(String title) {
        return new Screen("«" + title + "»\n\nقیمتش رو به تومان بفرست؛ مثلاً ۵۰٬۰۰۰٬۰۰۰.\n\nبرای انصراف /cancel رو بزن.",
                new KeyboardBuilder().row(InlineKeyboardButton.callback("انصراف", MarketOwnerCallbacks.MINE)).build());
    }

    /** Picks the condition while creating ({@code listingId} null) or changing a listing. */
    static Screen pickCondition(Long listingId, String text) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        for (ListingCondition condition : ListingCondition.values()) {
            keyboard.row(InlineKeyboardButton.callback(condition.label(), listingId == null
                    ? MarketOwnerCallbacks.newCondition(condition.name())
                    : MarketOwnerCallbacks.setCondition(listingId, condition.name())));
        }
        keyboard.row(InlineKeyboardButton.callback("انصراف", listingId == null
                ? MarketOwnerCallbacks.MINE : MarketOwnerCallbacks.manage(listingId)));
        return new Screen(text, keyboard.build());
    }

    static Screen pickCategory(Listing listing, List<EquipmentType> types) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        keyboard.grid(types.stream().map(type -> InlineKeyboardButton.callback(type.getIcon() + " " + type.getName(),
                MarketOwnerCallbacks.setCategory(listing.getId(), type.getId()))).toList(), 2);
        keyboard.row(InlineKeyboardButton.callback("بدون دسته", MarketOwnerCallbacks.setCategory(listing.getId(), 0)));
        keyboard.row(InlineKeyboardButton.callback("↩️ مدیریت آگهی", MarketOwnerCallbacks.manage(listing.getId())));
        return new Screen("دسته‌ی این تجهیزات چیه؟", keyboard.build());
    }

    static Screen manage(Listing listing, String category, long photoCount, long pendingOffers) {
        long id = listing.getId();
        KeyboardBuilder keyboard = new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("✏️ عنوان", MarketOwnerCallbacks.edit(id, ListingField.TITLE)),
                        InlineKeyboardButton.callback("💰 قیمت", MarketOwnerCallbacks.edit(id, ListingField.PRICE)))
                .row(InlineKeyboardButton.callback("🏷 برند", MarketOwnerCallbacks.edit(id, ListingField.BRAND)),
                        InlineKeyboardButton.callback("🏷 مدل", MarketOwnerCallbacks.edit(id, ListingField.MODEL)))
                .row(InlineKeyboardButton.callback("📍 شهر", MarketOwnerCallbacks.edit(id, ListingField.CITY)),
                        InlineKeyboardButton.callback("📝 توضیحات", MarketOwnerCallbacks.edit(id, ListingField.DESCRIPTION)))
                .row(InlineKeyboardButton.callback("📂 دسته", MarketOwnerCallbacks.pickCategory(id)),
                        InlineKeyboardButton.callback("🔖 وضعیت جنس", MarketOwnerCallbacks.pickCondition(id)))
                .row(InlineKeyboardButton.callback(listing.isNegotiable() ? "🤝 قابل مذاکره: بله" : "🤝 قابل مذاکره: نه",
                        MarketOwnerCallbacks.toggleNegotiable(id)))
                .row(InlineKeyboardButton.callback("🖼 افزودن عکس", MarketOwnerCallbacks.addPhotos(id)));
        if (photoCount > 0) {
            keyboard.row(InlineKeyboardButton.callback("🗑 حذف همه‌ی عکس‌ها", MarketOwnerCallbacks.clearPhotos(id)));
        }
        keyboard.row(InlineKeyboardButton.callback("💬 پیشنهادها" + (pendingOffers > 0
                ? " (" + PersianDigits.toPersian(pendingOffers) + ")" : ""), MarketOwnerCallbacks.offers(id)));
        if (listing.getStatus() == ListingStatus.ACTIVE) {
            keyboard.row(InlineKeyboardButton.callback("✅ فروخته شد", MarketOwnerCallbacks.setStatus(id, ListingStatus.SOLD.name())),
                    InlineKeyboardButton.callback("⛔ بستن آگهی", MarketOwnerCallbacks.setStatus(id, ListingStatus.CLOSED.name())));
        } else {
            keyboard.row(InlineKeyboardButton.callback("♻️ دوباره توی بازار بذار",
                    MarketOwnerCallbacks.setStatus(id, ListingStatus.ACTIVE.name())));
        }
        keyboard.row(InlineKeyboardButton.callback("🗑 حذف آگهی", MarketOwnerCallbacks.delete(id)));
        keyboard.row(InlineKeyboardButton.callback("↩️ آگهی‌های من", MarketOwnerCallbacks.MINE));
        String state = listing.isHidden() ? "⛔ این آگهی توسط ادمین از بازار مخفی شده.\n\n"
                : listing.getStatus() == ListingStatus.ACTIVE ? "" : "وضعیت: " + listing.getStatus().label() + "\n\n";
        return new Screen(state + MarketTexts.details(listing, category, photoCount, null), keyboard.build());
    }

    static Screen confirmDelete(Listing listing) {
        return new Screen("آگهی «" + listing.getTitle() + "» با عکس‌ها و پیشنهادهاش برای همیشه پاک بشه؟",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("🗑 بله، حذف کن", MarketOwnerCallbacks.confirmDelete(listing.getId())))
                        .row(InlineKeyboardButton.callback("انصراف", MarketOwnerCallbacks.manage(listing.getId())))
                        .build());
    }

    static Screen addPhotos(Listing listing, long count) {
        return new Screen("🖼 عکس‌های «" + listing.getTitle() + "» رو بفرست (تا " + PersianDigits.toPersian(MarketService.MAX_PHOTOS)
                + " عکس؛ الان " + PersianDigits.toPersian(count) + " عکس داری). وقتی تموم شد «تمام» رو بزن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("✅ تمام", MarketOwnerCallbacks.manage(listing.getId())))
                        .build());
    }

    static Screen photoSaved(Listing listing, long count) {
        return new Screen("✅ عکس ذخیره شد (" + PersianDigits.toPersian(count) + " از "
                + PersianDigits.toPersian(MarketService.MAX_PHOTOS) + "). عکس بعدی رو بفرست یا «تمام» رو بزن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("✅ تمام", MarketOwnerCallbacks.manage(listing.getId())))
                        .build());
    }

    static Screen photoRejected(Listing listing, String reason) {
        return new Screen(reason, new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("✅ تمام", MarketOwnerCallbacks.manage(listing.getId())))
                .build());
    }

    static Screen offers(Listing listing, List<OfferLine> lines) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text;
        if (lines.isEmpty()) {
            text = "پیشنهادی منتظر جواب نیست.";
        } else {
            StringBuilder out = new StringBuilder("💬 پیشنهادهای «" + listing.getTitle() + "»\n");
            lines.forEach(line -> {
                out.append("\n").append(offerText(line)).append("\n");
                keyboard.row(InlineKeyboardButton.callback("✅ قبول", MarketOwnerCallbacks.accept(line.offer().getId())),
                        InlineKeyboardButton.callback("❌ رد", MarketOwnerCallbacks.decline(line.offer().getId())));
            });
            out.append("\nقبول کردن پیشنهاد آگهی رو نمی‌بنده؛ وقتی فروختی «فروخته شد» رو بزن.");
            text = out.toString();
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ مدیریت آگهی", MarketOwnerCallbacks.manage(listing.getId())));
        return new Screen(text, keyboard.build());
    }

    /** One offer for the seller to read: who, how much, and the message. */
    static String offerText(OfferLine line) {
        String text = "👤 " + line.buyer() + "\n💰 " + MarketTexts.offerAmount(line.offer());
        return line.offer().getMessage() == null ? text : text + "\n📝 " + line.offer().getMessage();
    }
}
