package com.shut.telegram.venue;

import com.shut.common.calendar.JalaliDate;
import com.shut.common.calendar.JalaliMonth;
import com.shut.common.text.PersianDigits;
import com.shut.venue.Venue;
import com.shut.venue.VenuePrice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Venue details as text, for the pages visitors see and for the owner's management page. */
final class VenueTexts {

    private VenueTexts() {
    }

    /** "۱۴ آبان ۱۴۰۵". */
    static String date(LocalDate day) {
        JalaliDate jalali = JalaliDate.of(day);
        return PersianDigits.toPersian(jalali.day()) + " " + new JalaliMonth(jalali.year(), jalali.month()).name()
                + " " + PersianDigits.toPersian(jalali.year());
    }

    /** One price as a line: "خصوصی: ۵۰٬۰۰۰٬۰۰۰ تومان، یک‌جا". */
    static String priceLine(VenuePrice price) {
        String amount = PersianDigits.toPersianGrouped(price.getAmount()) + " تومان، " + price.getUnit().phrase();
        return price.getTitle() == null ? amount : price.getTitle() + ": " + amount;
    }

    /** The first price, with "و …" when there are more; for the list buttons. */
    static String priceSummary(List<VenuePrice> prices) {
        if (prices == null || prices.isEmpty()) {
            return "قیمت ثبت نشده";
        }
        return priceLine(prices.get(0)) + (prices.size() > 1 ? " و …" : "");
    }

    /** One line for a list: icon, name, city, price. */
    static String listLine(Venue venue) {
        StringBuilder line = new StringBuilder(icon(venue)).append(" ").append(venue.getName());
        if (venue.getCity() != null) {
            line.append(" — ").append(venue.getCity());
        }
        return line.toString();
    }

    static String details(Venue venue, long photoCount, List<VenuePrice> prices) {
        List<String> lines = new ArrayList<>();
        lines.add(icon(venue) + " " + venue.getName());
        if (venue.getType() != null) {
            lines.add("نوع: " + venue.getType().label());
        }
        if (venue.getCity() != null) {
            lines.add("📍 شهر: " + venue.getCity());
        }
        if (venue.getAddress() != null) {
            lines.add("آدرس: " + venue.getAddress());
        }
        if (prices.isEmpty()) {
            lines.add("💰 قیمت ثبت نشده");
        } else {
            lines.add("💰 قیمت‌ها:");
            prices.forEach(price -> lines.add("   • " + priceLine(price)));
        }
        if (venue.getAmenities() != null) {
            lines.add("امکانات: " + venue.getAmenities());
        }
        if (venue.getPhone() != null) {
            lines.add("📞 " + venue.getPhone());
        }
        lines.add("🖼 " + PersianDigits.toPersian(photoCount) + " عکس");
        if (venue.getDescription() != null) {
            lines.add("");
            lines.add(venue.getDescription());
        }
        return String.join("\n", lines);
    }

    /** What the owner still has to fill in before the venue shows up for others, or empty if it is complete. */
    static String missing(Venue venue, int priceCount) {
        List<String> missing = new ArrayList<>();
        if (venue.getCity() == null) {
            missing.add("شهر");
        }
        if (priceCount == 0) {
            missing.add("قیمت");
        }
        return String.join("، ", missing);
    }

    private static String icon(Venue venue) {
        return venue.getType() == null ? "📍" : venue.getType().icon();
    }
}
