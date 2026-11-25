package com.shut.telegram.studio;

import com.shut.common.text.PersianDigits;
import com.shut.studio.Studio;
import com.shut.studio.StudioOffering;

import java.util.List;

/** Texts about studios and their services. */
final class StudioTexts {

    private StudioTexts() {
    }

    static String money(long amount) {
        return PersianDigits.toPersianGrouped(amount) + " تومان";
    }

    /** One line for a list: name and city. */
    static String listLine(Studio studio) {
        return studio.getName() + (studio.getCity() == null ? "" : " — " + studio.getCity());
    }

    /** The studio's page; the counts are of services, showcase items and people. */
    static String details(Studio studio, long offerings, long media, long people) {
        StringBuilder text = new StringBuilder("🏢 ").append(studio.getName()).append("\n");
        if (studio.getManagerName() != null) {
            text.append("\n👤 مدیر: ").append(studio.getManagerName());
        }
        if (studio.getPhone() != null) {
            text.append("\n📞 ").append(PersianDigits.toPersian(studio.getPhone()));
        }
        if (studio.getCity() != null || studio.getAddress() != null) {
            text.append("\n📍 ").append(join(studio.getCity(), studio.getAddress()));
        }
        if (studio.getDescription() != null) {
            text.append("\n\n").append(studio.getDescription());
        }
        text.append("\n\n🎬 ").append(PersianDigits.toPersian(offerings)).append(" خدمت • 🖼 ")
                .append(PersianDigits.toPersian(media)).append(" نمونه‌کار • 👥 ")
                .append(PersianDigits.toPersian(people)).append(" نفر");
        return text.toString();
    }

    /** A service: title, what it includes and its price. */
    static String offering(StudioOffering offering) {
        StringBuilder text = new StringBuilder("🎬 ").append(offering.getTitle());
        if (offering.getIncludes() != null) {
            text.append("\n   شامل: ").append(offering.getIncludes());
        }
        text.append("\n   💰 ").append(offering.getPrice() == null ? "قیمت: با تماس" : money(offering.getPrice()));
        return text.toString();
    }

    static String offerings(List<StudioOffering> offerings) {
        StringBuilder text = new StringBuilder();
        offerings.forEach(offering -> text.append(text.length() == 0 ? "" : "\n\n").append(offering(offering)));
        return text.toString();
    }

    private static String join(String city, String address) {
        if (city == null) {
            return address;
        }
        return address == null ? city : city + "، " + address;
    }
}
