package com.shut.telegram.browse;

import com.shut.browse.ProviderDetails;
import com.shut.browse.ProviderSummary;
import com.shut.calendar.CalendarStatus;
import com.shut.common.calendar.JalaliMonth;
import com.shut.common.text.PersianDigits;
import com.shut.pricing.PriceItem;
import com.shut.review.RatingSummary;
import com.shut.showcase.ShowcaseCategory;
import com.shut.telegram.calendar.CalendarGrid;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.messaging.TelegramLinks;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.pricing.PriceText;
import com.shut.telegram.profile.ProfileCard;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Everything the user sees while looking for other members and reading their pages. Nothing here can change data. */
final class BrowseScreens {

    private static final String NO_NAME = "بدون نام";

    private BrowseScreens() {
    }

    static Screen searchPrompt() {
        return new Screen(
                "🔎 جستجوی اعضا\n\nاسم، شهر یا مهارتِ کسی که دنبالشی رو بفرست. مثلاً «تدوین» یا «شیراز» یا «پرتره».",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("👥 همه‌ی اعضا", BrowseCallbacks.ALL))
                        .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                        .build());
    }

    static Screen results(String query, Page<ProviderSummary> providers) {
        String what = query.isBlank() ? "همه‌ی اعضا" : "«" + query + "»";
        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (providers.isEmpty()) {
            keyboard.row(InlineKeyboardButton.callback("🔎 جستجوی تازه", BrowseCallbacks.START))
                    .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME));
            return new Screen("🔎 " + what + "\n\nکسی پیدا نشد. یه کلمه‌ی کوتاه‌تر یا یه شهر رو امتحان کن.", keyboard.build());
        }

        providers.forEach(provider -> keyboard.row(
                InlineKeyboardButton.callback(summaryLabel(provider), BrowseCallbacks.provider(provider.userId()))));

        List<InlineKeyboardButton> navigation = new ArrayList<>();
        if (providers.hasPrevious()) {
            navigation.add(InlineKeyboardButton.callback("▶️ قبلی", BrowseCallbacks.results(providers.getNumber() - 1)));
        }
        if (providers.hasNext()) {
            navigation.add(InlineKeyboardButton.callback("بعدی ◀️", BrowseCallbacks.results(providers.getNumber() + 1)));
        }
        if (!navigation.isEmpty()) {
            keyboard.grid(CalendarGrid.arrangeRow(navigation), navigation.size());
        }
        keyboard.row(InlineKeyboardButton.callback("🔎 جستجوی تازه", BrowseCallbacks.START))
                .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME));

        return new Screen("🔎 " + what + "\n\n" + PersianDigits.toPersian(providers.getTotalElements())
                + " نفر. برای دیدن صفحه‌ی هر نفر، اسمش رو بزن.", keyboard.build());
    }

    /** The member's profile with buttons for their showcase, calendar, prices and a way to message them. */
    static Screen provider(ProviderDetails details, RatingSummary rating) {
        long userId = details.userId();
        KeyboardBuilder keyboard = new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("🖼 ویترین", BrowseCallbacks.showcase(userId)),
                        InlineKeyboardButton.callback("📅 تقویم", BrowseCallbacks.currentCalendar(userId)))
                .row(InlineKeyboardButton.callback("💰 قیمت‌ها", BrowseCallbacks.prices(userId)),
                        InlineKeyboardButton.callback("⭐ نظرها", ReviewCallbacks.list(userId, 0)));
        String username = details.telegramUsername();
        TelegramLinks.messageButton(username).ifPresent(keyboard::row);
        keyboard.row(InlineKeyboardButton.callback("↩️ نتایج", BrowseCallbacks.BACK_TO_RESULTS));

        String text = ProfileCard.text(details.profile(), NO_NAME) + "\n\nامتیاز: " + ReviewScreens.summaryLine(rating);
        if (username == null) {
            text += "\n\nاین عضو نام کاربری تلگرام نداره، برای همین دکمه‌ی پیام نمایش داده نمی‌شه.";
        }
        return new Screen(text, keyboard.build());
    }

    static Screen showcaseHome(ProviderDetails details, Map<ShowcaseCategory, Long> counts) {
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (total == 0) {
            keyboard.row(backToProvider(details.userId()));
            return new Screen("🖼 ویترین " + name(details) + "\n\nهنوز نمونه‌کاری اضافه نکرده.", keyboard.build());
        }
        List<InlineKeyboardButton> buttons = Arrays.stream(ShowcaseCategory.values())
                .filter(category -> counts.getOrDefault(category, 0L) > 0)
                .map(category -> InlineKeyboardButton.callback(
                        category.label() + " (" + PersianDigits.toPersian(counts.get(category)) + ")",
                        BrowseCallbacks.showcaseItems(details.userId(), category, 0)))
                .toList();
        keyboard.grid(buttons, 2).row(backToProvider(details.userId()));
        return new Screen("🖼 ویترین " + name(details) + "\n\nیه دسته رو انتخاب کن.", keyboard.build());
    }

    /** The "1 to 5 of 12" message under a batch of showcase items. */
    static Screen showcasePage(long userId, ShowcaseCategory category, int page, int pageSize, long total) {
        long first = (long) page * pageSize + 1;
        long last = Math.min(total, (long) (page + 1) * pageSize);
        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (last < total) {
            keyboard.row(InlineKeyboardButton.callback("▶️ بعدی",
                    BrowseCallbacks.showcaseItems(userId, category, page + 1)));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ دسته‌ها", BrowseCallbacks.showcase(userId)));
        return new Screen(category.label() + ": " + PersianDigits.toPersian(first) + " تا "
                + PersianDigits.toPersian(last) + " از " + PersianDigits.toPersian(total), keyboard.build());
    }

    static Screen calendar(ProviderDetails details, JalaliMonth month, Map<LocalDate, CalendarStatus> statuses,
                           boolean hasPrevious, boolean hasNext) {
        long userId = details.userId();
        KeyboardBuilder keyboard = new KeyboardBuilder();
        CalendarGrid.append(keyboard, month, BrowseCallbacks.NOOP, (dayOfMonth, date) -> InlineKeyboardButton.callback(
                CalendarGrid.label(dayOfMonth, statuses.get(date)), BrowseCallbacks.NOOP));

        List<InlineKeyboardButton> navigation = new ArrayList<>();
        if (hasPrevious) {
            navigation.add(InlineKeyboardButton.callback("▶️ ماه قبل",
                    BrowseCallbacks.calendar(userId, month.plusMonths(-1))));
        }
        if (hasNext) {
            navigation.add(InlineKeyboardButton.callback("ماه بعد ◀️",
                    BrowseCallbacks.calendar(userId, month.plusMonths(1))));
        }
        if (!navigation.isEmpty()) {
            keyboard.grid(CalendarGrid.arrangeRow(navigation), navigation.size());
        }
        keyboard.row(backToProvider(userId));

        return new Screen("📅 تقویم " + name(details) + "\n" + month.name() + " " + PersianDigits.toPersian(month.year())
                + "\n\n" + CalendarGrid.FREE_MARK + " آزاد   " + CalendarGrid.BUSY_MARK + " پر"
                + "\nروزهای بدون رنگ یعنی هنوز وضعیتشون مشخص نشده.", keyboard.build());
    }

    static Screen prices(ProviderDetails details, List<PriceItem> items) {
        KeyboardBuilder keyboard = new KeyboardBuilder().row(backToProvider(details.userId()));
        if (items.isEmpty()) {
            return new Screen("💰 قیمت‌های " + name(details) + "\n\nهنوز قیمتی ثبت نکرده.", keyboard.build());
        }
        StringBuilder text = new StringBuilder("💰 قیمت‌های " + name(details) + "\n");
        items.forEach(item -> text.append("\n• ").append(PriceText.describe(item)));
        text.append("\n\nقیمت‌ها به تومان هستن.");
        return new Screen(text.toString(), keyboard.build());
    }

    static Screen providerGone() {
        return new Screen("این صفحه دیگه در دسترس نیست.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("🔎 جستجوی تازه", BrowseCallbacks.START))
                .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                .build());
    }

    private static InlineKeyboardButton backToProvider(long userId) {
        return InlineKeyboardButton.callback("↩️ برگشت به پروفایل", BrowseCallbacks.provider(userId));
    }

    static String name(ProviderDetails details) {
        String name = details.profile().displayName();
        return name == null ? NO_NAME : name;
    }

    private static String summaryLabel(ProviderSummary provider) {
        StringBuilder label = new StringBuilder(provider.displayName());
        if (provider.headline() != null) {
            label.append(" — ").append(provider.headline());
        } else if (provider.city() != null) {
            label.append(" — ").append(provider.city());
        }
        String text = label.toString();
        return text.length() > 50 ? text.substring(0, 49) + "…" : text;
    }
}
