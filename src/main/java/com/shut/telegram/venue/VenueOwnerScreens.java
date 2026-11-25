package com.shut.telegram.venue;

import com.shut.common.text.PersianDigits;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.venue.Venue;
import com.shut.venue.VenuePrice;
import com.shut.venue.VenuePriceUnit;
import com.shut.venue.VenueService;
import com.shut.venue.VenueType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** The owner's pages: their venues, one venue's management page, and the pickers and prompts that go with it. */
final class VenueOwnerScreens {

    private VenueOwnerScreens() {
    }

    static Screen mine(List<Venue> venues, Map<Long, Long> pendingRequests) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        venues.forEach(venue -> {
            long pending = pendingRequests.getOrDefault(venue.getId(), 0L);
            keyboard.row(InlineKeyboardButton.callback(VenueTexts.listLine(venue)
                    + (pending > 0 ? " ⏳" + PersianDigits.toPersian(pending) : ""), VenueOwnerCallbacks.manage(venue.getId())));
        });
        if (venues.size() < VenueService.MAX_VENUES_PER_OWNER) {
            keyboard.row(InlineKeyboardButton.callback("➕ مکان جدید", VenueOwnerCallbacks.NEW));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ مکان‌های عکاسی", VenueCallbacks.HOME));
        String text = venues.isEmpty()
                ? "🏡 هنوز مکانی ثبت نکردی.\n\nعمارت، باغ یا استودیوی خودت رو با عکس، قیمت و تقویم ثبت کن تا بقیه بتونن رزروش کنن."
                : "🏡 مکان‌های من\n\nروی هر مکان بزن تا مدیریتش کنی. ⏳ یعنی درخواست رزرو منتظر جواب داری.";
        return new Screen(text, keyboard.build());
    }

    static Screen askName() {
        return new Screen("اسم مکانت رو بفرست (مثلاً «باغ ویلا لواسان»).\n\nبرای انصراف /cancel رو بزن.", null);
    }

    static Screen limitReached() {
        return new Screen("حداکثر " + PersianDigits.toPersian(VenueService.MAX_VENUES_PER_OWNER)
                + " مکان می‌تونی ثبت کنی. اول یکی رو حذف کن.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("↩️ مکان‌های من", VenueOwnerCallbacks.MINE))
                .build());
    }

    static Screen gone() {
        return new Screen("این مکان پیدا نشد.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("↩️ مکان‌های من", VenueOwnerCallbacks.MINE))
                .build());
    }

    static Screen manage(Venue venue, long photoCount, long pendingRequests, List<VenuePrice> prices) {
        long id = venue.getId();
        KeyboardBuilder keyboard = new KeyboardBuilder();
        keyboard.grid(Arrays.stream(VenueField.values())
                .map(field -> InlineKeyboardButton.callback("✏️ " + field.label(), VenueOwnerCallbacks.edit(id, field)))
                .toList(), 2);
        keyboard.row(InlineKeyboardButton.callback("🏷 نوع مکان", VenueOwnerCallbacks.pickType(id)),
                        InlineKeyboardButton.callback("💰 قیمت‌ها", VenueOwnerCallbacks.prices(id)))
                .row(InlineKeyboardButton.callback("🖼 افزودن عکس", VenueOwnerCallbacks.addPhotos(id)),
                        InlineKeyboardButton.callback("🗑 حذف عکس‌ها", VenueOwnerCallbacks.clearPhotos(id)))
                .row(InlineKeyboardButton.callback("📅 تقویم", VenueOwnerCallbacks.calendar(id)),
                        InlineKeyboardButton.callback("📨 درخواست‌ها"
                                + (pendingRequests > 0 ? " (" + PersianDigits.toPersian(pendingRequests) + ")" : ""),
                                VenueOwnerCallbacks.requests(id)))
                .row(InlineKeyboardButton.callback(venue.isActive() ? "⏸ غیرفعال کردن" : "▶️ فعال کردن",
                                VenueOwnerCallbacks.toggle(id)),
                        InlineKeyboardButton.callback("🗑 حذف مکان", VenueOwnerCallbacks.delete(id)));
        if (venue.isListable() && !prices.isEmpty()) {
            keyboard.row(InlineKeyboardButton.callback("👀 نمایش برای بقیه", VenueCallbacks.venue(id)));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ مکان‌های من", VenueOwnerCallbacks.MINE));
        return new Screen(VenueTexts.details(venue, photoCount, prices) + "\n\n" + status(venue, prices.size()), keyboard.build());
    }

    private static String status(Venue venue, int priceCount) {
        String missing = VenueTexts.missing(venue, priceCount);
        if (!missing.isEmpty()) {
            return "⚠️ این مکان هنوز برای بقیه نمایش داده نمی‌شه. اول این‌ها رو کامل کن: " + missing;
        }
        return venue.isActive()
                ? "✅ این مکان توی فهرست دیده می‌شه. روزهای آزادش رو توی تقویم مشخص کن تا بقیه بتونن درخواست بدن."
                : "⏸ این مکان غیرفعاله و توی فهرست دیده نمی‌شه.";
    }

    static Screen pickType(Venue venue) {
        KeyboardBuilder keyboard = new KeyboardBuilder().grid(Arrays.stream(VenueType.values())
                .map(type -> InlineKeyboardButton.callback(type.icon() + " " + type.label(),
                        VenueOwnerCallbacks.setType(venue.getId(), type.name())))
                .toList(), 2);
        keyboard.row(InlineKeyboardButton.callback("↩️ برگشت", VenueOwnerCallbacks.manage(venue.getId())));
        return new Screen("نوع مکانت چیه؟", keyboard.build());
    }

    /** The venue's prices, each with a button to remove it, and one to add another. */
    static Screen prices(Venue venue, List<VenuePrice> prices) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text;
        if (prices.isEmpty()) {
            text = "💰 هنوز قیمتی ثبت نکردی.\n\nمی‌تونی چند قیمت بذاری؛ مثلاً ساعتی ۳ میلیون، روزی ۱۰ میلیون و «خصوصی» ۵۰ میلیون.";
        } else {
            StringBuilder lines = new StringBuilder("💰 قیمت‌های " + venue.getName() + "\n");
            prices.forEach(price -> {
                lines.append("\n• ").append(VenueTexts.priceLine(price));
                keyboard.row(InlineKeyboardButton.callback("✖ " + VenueTexts.priceLine(price),
                        VenueOwnerCallbacks.removePrice(price.getId())));
            });
            text = lines.append("\n\nبرای حذف هر قیمت، دکمه‌ی ✖ کنارش رو بزن.").toString();
        }
        if (prices.size() < VenueService.MAX_PRICES) {
            keyboard.row(InlineKeyboardButton.callback("➕ افزودن قیمت", VenueOwnerCallbacks.addPrice(venue.getId())));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ مدیریت مکان", VenueOwnerCallbacks.manage(venue.getId())));
        return new Screen(text, keyboard.build());
    }

    static Screen pickPriceUnit(Venue venue) {
        KeyboardBuilder keyboard = new KeyboardBuilder().grid(Arrays.stream(VenuePriceUnit.values())
                .map(unit -> InlineKeyboardButton.callback(unit.label(),
                        VenueOwnerCallbacks.priceUnit(venue.getId(), unit.name())))
                .toList(), 2);
        keyboard.row(InlineKeyboardButton.callback("↩️ برگشت", VenueOwnerCallbacks.prices(venue.getId())));
        return new Screen("این قیمت برای چه مدتیه؟\n\n«یک‌جا» برای قیمت ثابت یک بار استفاده است؛ مثلاً «خصوصی» یا «پکیج عروسی».",
                keyboard.build());
    }

    static Screen askPriceAmount(Venue venue, VenuePriceUnit unit) {
        return new Screen("قیمت " + unit.label() + " رو به تومان و فقط با عدد بفرست (مثلاً ۳۰۰۰۰۰۰).\n\nبرای انصراف /cancel رو بزن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("انصراف", VenueOwnerCallbacks.prices(venue.getId())))
                        .build());
    }

    static Screen askPriceTitle() {
        return new Screen("اگه این قیمت اسم خاصی داره بنویس (مثلاً «خصوصی» یا «عروسی»). برای بدون اسم «-» بفرست.\n\nبرای انصراف /cancel رو بزن.", null);
    }

    static Screen addPhotos(Venue venue, long photoCount) {
        return new Screen("🖼 عکس‌های " + venue.getName() + " رو بفرست (تا "
                + PersianDigits.toPersian(VenueService.MAX_PHOTOS) + " عکس، الان "
                + PersianDigits.toPersian(photoCount) + " تا داری). هر وقت تموم شد «تمام» رو بزن.",
                photosDone(venue));
    }

    static Screen photoSaved(Venue venue, long photoCount) {
        return new Screen("✅ ذخیره شد (" + PersianDigits.toPersian(photoCount) + " از "
                + PersianDigits.toPersian(VenueService.MAX_PHOTOS) + "). عکس بعدی رو بفرست یا «تمام» رو بزن.",
                photosDone(venue));
    }

    static Screen photoRejected(Venue venue, String reason) {
        return new Screen(reason, photosDone(venue));
    }

    private static com.shut.telegram.model.InlineKeyboardMarkup photosDone(Venue venue) {
        return new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("✅ تمام", VenueOwnerCallbacks.manage(venue.getId())))
                .build();
    }

    static Screen confirmDelete(Venue venue) {
        return new Screen("«" + venue.getName() + "» با عکس‌ها، تقویم و درخواست‌هاش برای همیشه حذف می‌شه. مطمئنی؟",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("🗑 بله، حذف کن", VenueOwnerCallbacks.confirmDelete(venue.getId())),
                                InlineKeyboardButton.callback("انصراف", VenueOwnerCallbacks.manage(venue.getId())))
                        .build());
    }

    static Screen requests(Venue venue, List<RequestLine> requests) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text;
        if (requests.isEmpty()) {
            text = "📨 درخواستی منتظر جواب نیست.";
        } else {
            StringBuilder lines = new StringBuilder("📨 درخواست‌های " + venue.getName() + "\n");
            requests.forEach(line -> {
                lines.append("\n").append(requestText(line));
                long id = line.booking().getId();
                keyboard.row(InlineKeyboardButton.callback("✅ قبول " + VenueTexts.date(line.booking().getDay()),
                                VenueOwnerCallbacks.accept(id)),
                        InlineKeyboardButton.callback("❌ رد", VenueOwnerCallbacks.decline(id)));
            });
            text = lines.toString();
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ برگشت", VenueOwnerCallbacks.manage(venue.getId())));
        return new Screen(text, keyboard.build());
    }

    /** A request as the owner reads it, in the list and in the message sent when it arrives. */
    static String requestText(RequestLine line) {
        String note = line.booking().getNote() == null ? "" : "\n   " + line.booking().getNote();
        return "• " + line.requester() + " — " + VenueTexts.date(line.booking().getDay()) + "، "
                + VenueTexts.hours(line.booking()) + note;
    }
}
