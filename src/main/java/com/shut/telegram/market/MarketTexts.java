package com.shut.telegram.market;

import com.shut.common.text.PersianDigits;
import com.shut.market.Listing;
import com.shut.market.Offer;

/** Listing details as text, for the pages buyers see and for the seller's management page. */
final class MarketTexts {

    private MarketTexts() {
    }

    /** "۵۰٬۰۰۰٬۰۰۰ تومان". */
    static String money(long amount) {
        return PersianDigits.toPersianGrouped(amount) + " تومان";
    }

    /** What the offer says about the price: its amount, or "با قیمت آگهی". */
    static String offerAmount(Offer offer) {
        return offer.getAmount() == null ? "با قیمت آگهی" : money(offer.getAmount());
    }

    /** One line for a list: title and price. */
    static String listLine(Listing listing) {
        return listing.getTitle() + " — " + money(listing.getPrice());
    }

    /** The page of a listing. {@code category} and {@code sellerName} may be null. */
    static String details(Listing listing, String category, long photoCount, String sellerName) {
        StringBuilder text = new StringBuilder("🛒 ").append(listing.getTitle()).append("\n\n");
        text.append("💰 ").append(money(listing.getPrice()));
        if (listing.isNegotiable()) {
            text.append(" (قابل مذاکره)");
        }
        text.append("\n🔖 ").append(listing.getCondition().label());
        if (category != null) {
            text.append("\n📂 ").append(category);
        }
        String gear = join(listing.getBrand(), listing.getModel());
        if (!gear.isEmpty()) {
            text.append("\n🏷 ").append(gear);
        }
        if (listing.getCity() != null) {
            text.append("\n📍 ").append(listing.getCity());
        }
        if (sellerName != null) {
            text.append("\n👤 فروشنده: ").append(sellerName);
        }
        text.append("\n🖼 ").append(photoCount > 0 ? PersianDigits.toPersian(photoCount) + " عکس" : "بدون عکس");
        if (listing.getDescription() != null) {
            text.append("\n\n").append(listing.getDescription());
        }
        return text.toString();
    }

    private static String join(String brand, String model) {
        if (brand == null) {
            return model == null ? "" : model;
        }
        return model == null ? brand : brand + " " + model;
    }
}
