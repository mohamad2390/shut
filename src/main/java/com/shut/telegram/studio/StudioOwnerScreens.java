package com.shut.telegram.studio;

import com.shut.pricing.ServiceType;
import com.shut.studio.Studio;
import com.shut.studio.StudioMemberService;
import com.shut.studio.StudioOffering;
import com.shut.studio.StudioService;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.shut.common.text.PersianDigits.toPersian;

/** The studio owner's screens: registering, managing the studio, its services, showcase and people. */
final class StudioOwnerScreens {

    private StudioOwnerScreens() {
    }

    static Screen intro() {
        return new Screen("🏢 استودیوی من\n\nهنوز استودیویی ثبت نکردی. با ثبت استودیو، اسم، مدیر، تلفن و آدرست، "
                + "خدمات و تعرفه‌ها، نمونه‌کارها و افرادت برای همه نمایش داده می‌شه. ثبت‌نام رایگانه.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("➕ ثبت استودیو", StudioOwnerCallbacks.NEW))
                        .row(InlineKeyboardButton.callback("↩️ دسته‌ها", com.shut.telegram.business.BusinessCallbacks.HOME))
                        .build());
    }

    static Screen gone() {
        return new Screen("استودیویی پیدا نشد.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("🏢 استودیوی من", StudioOwnerCallbacks.MINE)).build());
    }

    /** Asks one detail; during registration the question says how far along the member is. */
    static Screen ask(StudioField field, boolean registration, String name) {
        String title = registration
                ? "🏢 ثبت استودیو (" + toPersian(index(field) + 1) + " از " + toPersian(StudioField.REGISTRATION.length) + ")"
                : "✏️ ویرایش " + field.label() + (name == null ? "" : " " + name);
        return new Screen(title + "\n\n" + field.prompt() + "\n\nبرای انصراف /cancel رو بزن.", cancel());
    }

    /** The studio's management screen. */
    static Screen manage(Studio studio, boolean listed, long offerings, long media, long people, long pending) {
        String status = listed ? "✅ استودیو برای همه نمایش داده می‌شه."
                : "⚠️ هنوز نمایش داده نمی‌شه: " + whatIsMissing(studio, offerings);
        InlineKeyboardMarkup keyboard = new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("✏️ اسم", StudioOwnerCallbacks.edit(StudioField.NAME)),
                        InlineKeyboardButton.callback("👤 مدیر", StudioOwnerCallbacks.edit(StudioField.MANAGER)))
                .row(InlineKeyboardButton.callback("📞 تلفن", StudioOwnerCallbacks.edit(StudioField.PHONE)),
                        InlineKeyboardButton.callback("📍 شهر", StudioOwnerCallbacks.edit(StudioField.CITY)))
                .row(InlineKeyboardButton.callback("🏠 آدرس", StudioOwnerCallbacks.edit(StudioField.ADDRESS)),
                        InlineKeyboardButton.callback("📝 توضیحات", StudioOwnerCallbacks.edit(StudioField.DESCRIPTION)))
                .row(InlineKeyboardButton.callback("🎬 خدمات (" + toPersian(offerings) + ")", StudioOwnerCallbacks.OFFERINGS),
                        InlineKeyboardButton.callback("🖼 نمونه‌کار (" + toPersian(media) + ")", StudioOwnerCallbacks.MEDIA))
                .row(InlineKeyboardButton.callback("👥 افراد (" + toPersian(people)
                        + (pending > 0 ? " + " + toPersian(pending) + " منتظر" : "") + ")", StudioOwnerCallbacks.PEOPLE))
                .row(InlineKeyboardButton.callback(studio.isActive() ? "⏸ پنهان کردن موقت" : "▶️ نمایش دوباره",
                        StudioOwnerCallbacks.TOGGLE))
                .row(listed ? InlineKeyboardButton.callback("👁 صفحهٔ عمومی", StudioCallbacks.studio(studio.getId()))
                        : InlineKeyboardButton.callback("🔄 بررسی دوباره", StudioOwnerCallbacks.MINE),
                        InlineKeyboardButton.callback("🗑 حذف استودیو", StudioOwnerCallbacks.DELETE))
                .row(InlineKeyboardButton.callback("↩️ استودیوها", StudioCallbacks.HOME))
                .build();
        return new Screen(StudioTexts.details(studio, offerings, media, people) + "\n\n" + status, keyboard);
    }

    private static String whatIsMissing(Studio studio, long offerings) {
        List<String> missing = new ArrayList<>();
        if (!studio.isActive()) {
            missing.add("پنهان شده");
        }
        if (!studio.hasContactDetails()) {
            missing.add("مدیر، تلفن، شهر و آدرس کامل نیست");
        }
        if (offerings == 0) {
            missing.add("حداقل یک خدمت اضافه کن");
        }
        return String.join("؛ ", missing) + ".";
    }

    static Screen confirmDelete(Studio studio) {
        return new Screen("استودیوی «" + studio.getName() + "» با خدمات، نمونه‌کارها و لیست افرادش حذف بشه؟ "
                + "این کار برگشت نداره.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("🗑 بله، حذف کن", StudioOwnerCallbacks.CONFIRM_DELETE),
                        InlineKeyboardButton.callback("انصراف", StudioOwnerCallbacks.MINE))
                .build());
    }

    static Screen offerings(List<StudioOffering> offerings, Map<Long, String> labels) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text;
        if (offerings.isEmpty()) {
            text = "🎬 خدمات\n\nهنوز خدمتی اضافه نکردی. هر خدمت یک بستهٔ کامل‌ه؛ مثلاً «پوشش کامل مراسم عروسی» "
                    + "که از بین خدمات شات (عکاسی، فیلمبرداری، فرمالیته، آلبوم…) انتخاب می‌کنی چی شاملش می‌شه.";
        } else {
            text = "🎬 خدمات\n\n" + StudioTexts.offerings(offerings, labels);
            offerings.forEach(offering -> keyboard.row(InlineKeyboardButton.callback("❌ " + offering.getTitle(),
                    StudioOwnerCallbacks.removeOffering(offering.getId()))));
        }
        if (offerings.size() < StudioService.MAX_OFFERINGS) {
            keyboard.row(InlineKeyboardButton.callback("➕ افزودن خدمت", StudioOwnerCallbacks.ADD_OFFERING));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ استودیوی من", StudioOwnerCallbacks.MINE));
        return new Screen(text, keyboard.build());
    }

    static Screen askOfferingTitle() {
        return new Screen("🎬 خدمت تازه (۱ از ۳)\n\nاسم خدمت رو بفرست؛ مثلاً «پوشش کامل مراسم عروسی».\n\n"
                + "برای انصراف /cancel رو بزن.", cancelTo(StudioOwnerCallbacks.OFFERINGS));
    }

    /** The catalog services to tick for a package; a tick marks the ones already chosen. */
    static Screen pickServices(String title, List<ServiceType> catalog, ServiceSelection chosen, boolean warn) {
        List<InlineKeyboardButton> toggles = catalog.stream()
                .map(service -> InlineKeyboardButton.callback((chosen.contains(service.getId()) ? "✅ " : "")
                        + service.getIcon() + " " + service.getName(), StudioOwnerCallbacks.service(service.getId())))
                .toList();
        String text = "🎬 «" + title + "» (۲ از ۳)\n\nچه خدماتی شامل این بسته می‌شه؟ هر چندتا که لازمه بزن، بعد «ثبت انتخاب».";
        if (warn) {
            text += "\n\n⚠️ حداقل یک خدمت انتخاب کن.";
        }
        return new Screen(text, new KeyboardBuilder()
                .grid(toggles, 2)
                .row(InlineKeyboardButton.callback("✔️ ثبت انتخاب (" + toPersian(chosen.size()) + ")",
                        StudioOwnerCallbacks.SERVICES_DONE))
                .row(InlineKeyboardButton.callback("انصراف", StudioOwnerCallbacks.OFFERINGS))
                .build());
    }

    static Screen askOfferingPrice() {
        return new Screen("🎬 خدمت تازه (۳ از ۳)\n\nقیمت رو به تومان بفرست؛ مثلاً ۵۰٬۰۰۰٬۰۰۰. "
                + "اگه قیمت رو با تماس می‌گی «-» بفرست.", cancelTo(StudioOwnerCallbacks.OFFERINGS));
    }

    static Screen media(long count) {
        return new Screen("🖼 نمونه‌کارها\n\n" + (count == 0 ? "هنوز چیزی اضافه نکردی."
                : toPersian(count) + " مورد داری.") + " حداکثر " + toPersian(StudioService.MAX_MEDIA)
                + " عکس یا ویدیو می‌تونی بذاری.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("➕ افزودن عکس و ویدیو", StudioOwnerCallbacks.ADD_MEDIA))
                .row(InlineKeyboardButton.callback("🗑 حذف همه", StudioOwnerCallbacks.CLEAR_MEDIA))
                .row(InlineKeyboardButton.callback("↩️ استودیوی من", StudioOwnerCallbacks.MINE))
                .build());
    }

    static Screen addMedia(long count) {
        return new Screen("🖼 عکس‌ها و ویدیوهای نمونه‌کارت رو بفرست. الان " + toPersian(count)
                + " مورد داری. وقتی تموم شد «تمام» رو بزن.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("✅ تمام", StudioOwnerCallbacks.MEDIA)).build());
    }

    static Screen mediaSaved(long count) {
        return new Screen("✅ ذخیره شد. الان " + toPersian(count) + " مورد داری. بازم بفرست یا «تمام» رو بزن.",
                new KeyboardBuilder().row(InlineKeyboardButton.callback("✅ تمام", StudioOwnerCallbacks.MEDIA)).build());
    }

    static Screen mediaRejected(String reason) {
        return new Screen(reason, new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("✅ تمام", StudioOwnerCallbacks.MEDIA)).build());
    }

    /** The studio's people and the invitations not yet answered, each with a button to take them off. */
    static Screen people(List<PersonLine> people, List<PersonLine> pending) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        StringBuilder text = new StringBuilder("👥 افراد استودیو\n");
        if (people.isEmpty() && pending.isEmpty()) {
            text.append("\nهنوز کسی رو اضافه نکردی. متخصصی که از قبل توی شات ثبت‌نام کرده رو با شمارهٔ موبایل یا "
                    + "یوزرنیم تلگرامش پیدا کن و دعوتش کن؛ بعد از قبول کردنش به لیست اضافه می‌شه.");
        }
        people.forEach(person -> {
            text.append("\n✅ ").append(person.name());
            keyboard.row(InlineKeyboardButton.callback("❌ " + person.name(), StudioOwnerCallbacks.removePerson(person.userId())));
        });
        pending.forEach(person -> {
            text.append("\n⏳ ").append(person.name()).append(" (منتظر جواب)");
            keyboard.row(InlineKeyboardButton.callback("❌ لغو دعوت " + person.name(),
                    StudioOwnerCallbacks.removePerson(person.userId())));
        });
        if (people.size() + pending.size() < StudioMemberService.MAX_PEOPLE) {
            keyboard.row(InlineKeyboardButton.callback("➕ دعوت متخصص", StudioOwnerCallbacks.INVITE));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ استودیوی من", StudioOwnerCallbacks.MINE));
        return new Screen(text.toString(), keyboard.build());
    }

    static Screen askInvitee() {
        return new Screen("شمارهٔ موبایل یا یوزرنیم تلگرام متخصص رو بفرست؛ مثلاً ۰۹۱۲۱۲۳۴۵۶۷ یا @username.\n"
                + "فقط متخصصی که از قبل توی شات ثبت‌نام کرده پیدا می‌شه.\n\nبرای انصراف /cancel رو بزن.",
                cancelTo(StudioOwnerCallbacks.PEOPLE));
    }

    private static int index(StudioField field) {
        for (int i = 0; i < StudioField.REGISTRATION.length; i++) {
            if (StudioField.REGISTRATION[i] == field) {
                return i;
            }
        }
        return 0;
    }

    private static InlineKeyboardMarkup cancel() {
        return cancelTo(StudioOwnerCallbacks.MINE);
    }

    private static InlineKeyboardMarkup cancelTo(String callback) {
        return new KeyboardBuilder().row(InlineKeyboardButton.callback("انصراف", callback)).build();
    }
}
