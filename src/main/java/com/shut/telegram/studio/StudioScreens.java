package com.shut.telegram.studio;

import com.shut.common.text.PersianDigits;
import com.shut.studio.Studio;
import com.shut.studio.StudioOffering;
import com.shut.telegram.browse.BrowseCallbacks;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.messaging.TelegramLinks;
import com.shut.telegram.model.InlineKeyboardButton;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/** The pages anyone sees: the studios section, the list, a studio's page, its services and its people. */
final class StudioScreens {

    private StudioScreens() {
    }

    static Screen home() {
        return new Screen("🏢 استودیوها\n\nاستودیوهایی که مجموعه‌ای از خدمات عکاسی و فیلمبرداری ارائه می‌دن. "
                + "ما فقط شما رو به هم وصل می‌کنیم؛ برای اطمینان، تماس بگیر یا به آدرس استودیو مراجعه کن. "
                + "اگه استودیو داری، اینجا ثبتش کن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("🔎 دیدن استودیوها", StudioCallbacks.list(0)))
                        .row(InlineKeyboardButton.callback("🏢 استودیوی من", StudioCallbacks.OWNER_HOME))
                        .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                        .build());
    }

    static Screen list(Page<Studio> studios) {
        if (studios.isEmpty()) {
            return new Screen("هنوز استودیویی ثبت نشده. اولین نفر باش!", new KeyboardBuilder()
                    .row(InlineKeyboardButton.callback("🏢 ثبت استودیوی من", StudioCallbacks.OWNER_HOME))
                    .row(InlineKeyboardButton.callback("↩️ استودیوها", StudioCallbacks.HOME))
                    .build());
        }
        KeyboardBuilder keyboard = new KeyboardBuilder();
        studios.forEach(studio -> keyboard.row(InlineKeyboardButton.callback(StudioTexts.listLine(studio),
                StudioCallbacks.studio(studio.getId()))));
        if (studios.hasPrevious()) {
            keyboard.row(InlineKeyboardButton.callback("▶️ قبلی", StudioCallbacks.list(studios.getNumber() - 1)));
        }
        if (studios.hasNext()) {
            keyboard.row(InlineKeyboardButton.callback("بعدی ◀️", StudioCallbacks.list(studios.getNumber() + 1)));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ استودیوها", StudioCallbacks.HOME));
        return new Screen("🏢 " + PersianDigits.toPersian(studios.getTotalElements())
                + " استودیو\n\nروی هر استودیو بزن تا خدمات و نمونه‌کارهاش رو ببینی.", keyboard.build());
    }

    /** A studio's page. {@code ownerUsername} is null when the owner has no Telegram username. */
    static Screen studio(Studio studio, long offerings, long media, long people, String ownerUsername,
                         boolean viewerIsMember) {
        long id = studio.getId();
        KeyboardBuilder keyboard = new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("🎬 خدمات و تعرفه", StudioCallbacks.offerings(id)));
        if (media > 0) {
            keyboard.row(InlineKeyboardButton.callback("🖼 نمونه‌کارها", StudioCallbacks.media(id)));
        }
        if (people > 0) {
            keyboard.row(InlineKeyboardButton.callback("👥 افراد استودیو", StudioCallbacks.people(id)));
        }
        TelegramLinks.messageButton(ownerUsername).ifPresent(keyboard::row);
        if (viewerIsMember) {
            keyboard.row(InlineKeyboardButton.callback("🚪 خروج از این استودیو", StudioCallbacks.leave(id)));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ لیست استودیوها", StudioCallbacks.list(0)));
        return new Screen(StudioTexts.details(studio, offerings, media, people), keyboard.build());
    }

    static Screen gone() {
        return new Screen("این استودیو دیگه در دسترس نیست.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("↩️ لیست استودیوها", StudioCallbacks.list(0)))
                .build());
    }

    static Screen offerings(Studio studio, List<StudioOffering> offerings, Map<Long, String> labels) {
        return new Screen("🎬 خدمات " + studio.getName() + "\n\n" + StudioTexts.offerings(offerings, labels)
                + "\n\nبرای سفارش یا قیمت دقیق با استودیو تماس بگیر.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("↩️ صفحهٔ استودیو", StudioCallbacks.studio(studio.getId())))
                .build());
    }

    /** The people of the studio; each opens that specialist's page. */
    static Screen people(Studio studio, List<PersonLine> people) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        people.forEach(person -> keyboard.row(InlineKeyboardButton.callback(person.name(),
                BrowseCallbacks.provider(person.userId()))));
        keyboard.row(InlineKeyboardButton.callback("↩️ صفحهٔ استودیو", StudioCallbacks.studio(studio.getId())));
        return new Screen("👥 افراد " + studio.getName() + "\n\nروی هر نفر بزن تا پروفایلش رو ببینی.", keyboard.build());
    }
}
