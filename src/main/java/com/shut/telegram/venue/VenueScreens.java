package com.shut.telegram.venue;

import com.shut.common.text.PersianDigits;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.messaging.TelegramLinks;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.venue.BookingStatus;
import com.shut.venue.Venue;
import com.shut.venue.VenueBooking;
import com.shut.venue.VenuePrice;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** The pages anyone sees: the venues section, the list, a venue's page and the member's own requests. */
final class VenueScreens {

    private VenueScreens() {
    }

    static Screen home() {
        return new Screen("📍 مکان‌های عکاسی\n\nعمارت، باغ و استودیوهایی که برای عکاسی و فیلمبرداری اجاره داده می‌شن. "
                + "اگه خودت مکانی داری، اینجا ثبتش کن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("🔎 دیدن مکان‌ها", VenueCallbacks.list(0)))
                        .row(InlineKeyboardButton.callback("🏡 مکان‌های من", VenueOwnerCallbacks.MINE))
                        .row(InlineKeyboardButton.callback("📨 درخواست‌های رزرو من", VenueCallbacks.MY_BOOKINGS))
                        .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                        .build());
    }

    static Screen list(Page<Venue> venues, Map<Long, List<VenuePrice>> prices) {
        if (venues.isEmpty()) {
            return new Screen("هنوز مکانی ثبت نشده. اولین نفر باش!", new KeyboardBuilder()
                    .row(InlineKeyboardButton.callback("🏡 ثبت مکان من", VenueOwnerCallbacks.MINE))
                    .row(InlineKeyboardButton.callback("↩️ مکان‌های عکاسی", VenueCallbacks.HOME))
                    .build());
        }
        KeyboardBuilder keyboard = new KeyboardBuilder();
        venues.forEach(venue -> keyboard.row(InlineKeyboardButton.callback(
                VenueTexts.listLine(venue) + " — " + VenueTexts.priceSummary(prices.get(venue.getId())),
                VenueCallbacks.venue(venue.getId()))));
        if (venues.hasPrevious()) {
            keyboard.row(InlineKeyboardButton.callback("▶️ قبلی", VenueCallbacks.list(venues.getNumber() - 1)));
        }
        if (venues.hasNext()) {
            keyboard.row(InlineKeyboardButton.callback("بعدی ◀️", VenueCallbacks.list(venues.getNumber() + 1)));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ مکان‌های عکاسی", VenueCallbacks.HOME));
        return new Screen("📍 " + PersianDigits.toPersian(venues.getTotalElements())
                + " مکان برای عکاسی\n\nروی هر مکان بزن تا جزئیاتش رو ببینی.", keyboard.build());
    }

    /** A venue's page. {@code ownerUsername} is null when the owner has no Telegram username. */
    static Screen venue(Venue venue, long photoCount, List<VenuePrice> prices, String ownerUsername) {
        long id = venue.getId();
        KeyboardBuilder keyboard = new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("🖼 عکس‌ها", VenueCallbacks.photos(id)),
                        InlineKeyboardButton.callback("📅 تقویم و رزرو", VenueCallbacks.calendar(id)));
        TelegramLinks.messageButton(ownerUsername).ifPresent(keyboard::row);
        keyboard.row(InlineKeyboardButton.callback("↩️ لیست مکان‌ها", VenueCallbacks.list(0)));
        return new Screen(VenueTexts.details(venue, photoCount, prices), keyboard.build());
    }

    static Screen gone() {
        return new Screen("این مکان دیگه در دسترس نیست.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("↩️ لیست مکان‌ها", VenueCallbacks.list(0)))
                .build());
    }

    /** Asks which hours of the day are wanted. */
    static Screen askTime(Venue venue, LocalDate day) {
        return new Screen("📨 رزرو " + venue.getName() + " برای " + VenueTexts.date(day)
                + "\n\nساعت مورد نظرت رو بفرست؛ مثلاً «۹ تا ۱۲» یا «۱۴:۳۰ تا ۱۸». اگه کل روز رو می‌خوای، دکمه‌ی «تمام روز» رو بزن."
                + "\n\nبرای انصراف /cancel رو بزن.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("🕘 تمام روز", VenueCallbacks.fullDay(venue.getId(), day)))
                .row(InlineKeyboardButton.callback("انصراف", VenueCallbacks.venue(venue.getId())))
                .build());
    }

    static Screen askNote(Venue venue, String hours, LocalDate day) {
        return new Screen("📨 درخواست رزرو " + venue.getName() + " برای " + VenueTexts.date(day) + "، " + hours
                + "\n\nتوضیح کوتاه بفرست: نوع کار و تعداد نفرات. این پیام همراه درخواستت برای صاحب مکان می‌ره."
                + "\n\nبرای انصراف /cancel رو بزن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("انصراف", VenueCallbacks.venue(venue.getId())))
                        .build());
    }

    static Screen requestSent(Venue venue) {
        return new Screen("✅ درخواستت برای " + venue.getName()
                + " ارسال شد. وقتی صاحب مکان جواب بده، همین‌جا خبرت می‌کنم.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("📨 درخواست‌های من", VenueCallbacks.MY_BOOKINGS))
                        .row(InlineKeyboardButton.callback("↩️ مکان‌های عکاسی", VenueCallbacks.HOME))
                        .build());
    }

    /** The member's latest requests; {@code venues} holds the venues they were for, by id. */
    static Screen myBookings(List<VenueBooking> bookings, Map<Long, Venue> venues) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text;
        if (bookings.isEmpty()) {
            text = "هنوز درخواست رزروی ندادی. از «دیدن مکان‌ها» یک مکان انتخاب کن و روز آزادش رو بزن.";
        } else {
            StringBuilder lines = new StringBuilder("📨 آخرین درخواست‌های تو\n");
            bookings.forEach(booking -> {
                Venue venue = venues.get(booking.getVenueId());
                lines.append("\n").append(venue == null ? "مکان حذف‌شده" : venue.getName())
                        .append(" — ").append(VenueTexts.date(booking.getDay())).append("، ").append(VenueTexts.hours(booking))
                        .append("\n   ").append(booking.getStatus().label());
                if (venue != null && booking.getStatus() == BookingStatus.ACCEPTED) {
                    keyboard.row(InlineKeyboardButton.callback("✅ " + venue.getName(), VenueCallbacks.venue(venue.getId())));
                }
            });
            text = lines.toString();
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ مکان‌های عکاسی", VenueCallbacks.HOME));
        return new Screen(text, keyboard.build());
    }
}
