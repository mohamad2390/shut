package com.shut.telegram.project;

import com.shut.common.text.PersianDigits;
import com.shut.project.Bid;
import com.shut.project.BidStatus;
import com.shut.project.Project;
import com.shut.project.ProjectService;
import com.shut.project.ProjectStatus;
import com.shut.showcase.ShowcaseCategory;
import com.shut.telegram.calendar.CalendarGrid;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.messaging.TelegramLinks;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.InlineKeyboardMarkup;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/** Everything the user sees about projects: the section's home, creating a project, lists and a project's page. */
final class ProjectScreens {

    private static final InlineKeyboardMarkup ABANDON_ONLY = new KeyboardBuilder()
            .row(InlineKeyboardButton.callback("انصراف", ProjectCallbacks.ABANDON))
            .build();

    private ProjectScreens() {
    }

    static Screen home() {
        return new Screen(
                "📋 پروژه‌ها\n\nپروژه‌ات رو تعریف کن تا متخصص‌ها براش پیشنهاد قیمت بدن، یا پروژه‌های بقیه رو ببین و پیشنهاد بده.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("➕ پروژه‌ی جدید", ProjectCallbacks.NEW))
                        .row(InlineKeyboardButton.callback("🔎 پروژه‌های باز", ProjectCallbacks.open(ProjectCallbacks.ALL, 0)))
                        .row(InlineKeyboardButton.callback("📁 پروژه‌های من", ProjectCallbacks.mine(0)),
                                InlineKeyboardButton.callback("🧾 پیشنهادهای من", BidCallbacks.mine(0)))
                        .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                        .build());
    }

    // ---- creating a project -------------------------------------------------------------------------------

    static Screen askTitle() {
        return new Screen("➕ پروژه‌ی جدید\n\nیه عنوان کوتاه برای پروژه بفرست. مثلاً «عکاسی عروسی در شیراز» یا «تدوین کلیپ تبلیغاتی».",
                ABANDON_ONLY);
    }

    static Screen askCategory(String title) {
        List<InlineKeyboardButton> buttons = Arrays.stream(ShowcaseCategory.values())
                .map(category -> InlineKeyboardButton.callback(category.label(), ProjectCallbacks.category(category)))
                .toList();
        return new Screen("➕ " + title + "\n\nپروژه از چه نوعیه؟", new KeyboardBuilder()
                .grid(buttons, 2)
                .row(InlineKeyboardButton.callback("انصراف", ProjectCallbacks.ABANDON))
                .build());
    }

    static Screen askDescription() {
        return new Screen("توضیح پروژه رو بنویس: چی لازم داری، چه زمانی، چند ساعت یا چند عکس، و هر چیز دیگه‌ای که برای قیمت دادن لازمه "
                + "(حداکثر " + PersianDigits.toPersian(ProjectService.MAX_DESCRIPTION_LENGTH) + " حرف).", ABANDON_ONLY);
    }

    static Screen askCity() {
        return new Screen("پروژه توی کدوم شهره؟ اسم شهر رو بفرست. اگه کار از راه دور انجام می‌شه یا مهم نیست، دکمه‌ی پایین رو بزن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("فرقی نمی‌کنه", ProjectCallbacks.SKIP_CITY))
                        .row(InlineKeyboardButton.callback("انصراف", ProjectCallbacks.ABANDON))
                        .build());
    }

    static Screen askBudget() {
        return new Screen("بودجه‌ی تقریبی‌ات چقدره؟ به تومان و فقط با عدد بفرست. مثلاً ۵۰۰۰۰۰۰. "
                + "اگه نمی‌خوای بگی، دکمه‌ی پایین رو بزن؛ متخصص‌ها بدون سقف قیمت پیشنهاد می‌دن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("نمی‌گم", ProjectCallbacks.SKIP_BUDGET))
                        .row(InlineKeyboardButton.callback("انصراف", ProjectCallbacks.ABANDON))
                        .build());
    }

    static Screen askDeadline() {
        List<InlineKeyboardButton> buttons = ProjectService.DEADLINE_DAYS.stream()
                .map(days -> InlineKeyboardButton.callback(
                        PersianDigits.toPersian(days) + " روز", ProjectCallbacks.deadline(days)))
                .toList();
        return new Screen("متخصص‌ها تا چند روز می‌تونن پیشنهاد بدن؟ بعد از این مهلت پیشنهاد جدید قبول نمی‌شه، "
                + "ولی خودت هنوز می‌تونی بین پیشنهادها انتخاب کنی.", new KeyboardBuilder()
                .grid(buttons, 2)
                .row(InlineKeyboardButton.callback("انصراف", ProjectCallbacks.ABANDON))
                .build());
    }

    // ---- lists --------------------------------------------------------------------------------------------

    /** Projects that take offers, with category filters. {@code filter} is a category name or "ALL". */
    static Screen openList(String filter, Page<Project> projects) {
        List<InlineKeyboardButton> filters = new ArrayList<>();
        filters.add(InlineKeyboardButton.callback(
                (ProjectCallbacks.ALL.equals(filter) ? "• " : "") + "همه", ProjectCallbacks.open(ProjectCallbacks.ALL, 0)));
        for (ShowcaseCategory category : ShowcaseCategory.values()) {
            filters.add(InlineKeyboardButton.callback(
                    (category.name().equals(filter) ? "• " : "") + category.label(),
                    ProjectCallbacks.open(category.name(), 0)));
        }

        KeyboardBuilder keyboard = new KeyboardBuilder().grid(filters, 3);
        projects.forEach(project -> keyboard.row(InlineKeyboardButton.callback(
                ProjectText.listLabel(project, ""), ProjectCallbacks.view(project.getId()))));
        addPaging(keyboard, projects, page -> ProjectCallbacks.open(filter, page));
        keyboard.row(InlineKeyboardButton.callback("↩️ برگشت", ProjectCallbacks.HOME));

        String text = projects.isEmpty()
                ? "🔎 پروژه‌های باز\n\nالان پروژه‌ای که پیشنهاد قبول کنه توی این دسته نیست."
                : "🔎 پروژه‌های باز\n\n" + PersianDigits.toPersian(projects.getTotalElements())
                + " پروژه. اونایی که مهلتشون زودتر تموم می‌شه اول هستن.";
        return new Screen(text, keyboard.build());
    }

    static Screen mineList(Page<Project> projects, Predicate<Project> takingBids) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        projects.forEach(project -> keyboard.row(InlineKeyboardButton.callback(
                ProjectText.listLabel(project, statusEmoji(project, takingBids.test(project)) + " "),
                ProjectCallbacks.view(project.getId()))));
        addPaging(keyboard, projects, ProjectCallbacks::mine);
        keyboard.row(InlineKeyboardButton.callback("➕ پروژه‌ی جدید", ProjectCallbacks.NEW))
                .row(InlineKeyboardButton.callback("↩️ برگشت", ProjectCallbacks.HOME));
        String text = projects.isEmpty()
                ? "📁 پروژه‌های من\n\nهنوز پروژه‌ای تعریف نکردی."
                : "📁 پروژه‌های من\n\n🟢 دریافت پیشنهاد   ⏳ منتظر انتخاب تو   ✅ واگذار شده   🚫 لغو شده";
        return new Screen(text, keyboard.build());
    }

    // ---- one project --------------------------------------------------------------------------------------

    /**
     * The project as its owner sees it.
     *
     * @param winnerName     name of the chosen provider, or null if none was chosen
     * @param winnerUsername their Telegram username, if they have one
     */
    static Screen ownerView(Project project, boolean takingBids, String deadline, long bidCount,
                            String winnerName, Bid winner, String winnerUsername) {
        StringBuilder text = new StringBuilder(ProjectText.details(project, "خودت", deadline, takingBids));
        text.append("\n\nپیشنهادها: ").append(PersianDigits.toPersian(bidCount));
        if (winnerName != null && winner != null) {
            text.append("\nبرنده: ").append(winnerName).append(" با ").append(ProjectText.money(winner.getAmount()));
        }

        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (bidCount > 0) {
            keyboard.row(InlineKeyboardButton.callback("📥 دیدن پیشنهادها (" + PersianDigits.toPersian(bidCount) + ")",
                    BidCallbacks.list(project.getId(), 0)));
        }
        if (project.getStatus() == ProjectStatus.AWARDED) {
            TelegramLinks.messageButton(winnerUsername).ifPresent(keyboard::row);
        }
        if (project.getStatus() == ProjectStatus.OPEN) {
            keyboard.row(InlineKeyboardButton.callback("🚫 لغو پروژه", ProjectCallbacks.cancel(project.getId())));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ پروژه‌های من", ProjectCallbacks.mine(0)));
        return new Screen(text.toString(), keyboard.build());
    }

    /**
     * The project as another member sees it. Offers are sealed, so they only see how many there are and their own.
     *
     * @param own          this member's offer, or null
     * @param ownerUsername the owner's Telegram username; shown as a button once this member's offer was chosen
     */
    static Screen bidderView(Project project, String ownerName, boolean takingBids, String deadline,
                             long bidCount, Bid own, String ownerUsername) {
        StringBuilder text = new StringBuilder(ProjectText.details(project, ownerName, deadline, takingBids));
        text.append("\n\nتعداد پیشنهادها: ").append(PersianDigits.toPersian(bidCount))
                .append(" (مبلغ پیشنهادها فقط برای صاحب پروژه دیده می‌شه)");
        if (own != null) {
            text.append("\n\nپیشنهاد تو: ").append(ProjectText.money(own.getAmount()))
                    .append(" — ").append(ProjectText.bidStatus(own.getStatus()));
            if (own.getNote() != null) {
                text.append("\n").append(own.getNote());
            }
        }

        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (takingBids && own == null) {
            keyboard.row(InlineKeyboardButton.callback("💼 ثبت پیشنهاد", BidCallbacks.place(project.getId())));
        } else if (takingBids) {
            keyboard.row(InlineKeyboardButton.callback("✏️ ویرایش پیشنهاد", BidCallbacks.place(project.getId())),
                    InlineKeyboardButton.callback("🗑 پس گرفتن", BidCallbacks.withdraw(project.getId())));
        }
        if (own != null && own.getStatus() == BidStatus.ACCEPTED) {
            TelegramLinks.messageButton(ownerUsername).ifPresent(keyboard::row);
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ پروژه‌های باز", ProjectCallbacks.open(ProjectCallbacks.ALL, 0)));
        return new Screen(text.toString(), keyboard.build());
    }

    static Screen cancelConfirm(Project project) {
        return new Screen("پروژه‌ی «" + project.getTitle() + "» لغو بشه؟ به همه‌ی پیشنهاددهنده‌ها خبر داده نمی‌شه، ولی دیگه کسی نمی‌تونه ببینه‌اش.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("بله، لغو کن", ProjectCallbacks.confirmCancel(project.getId())),
                                InlineKeyboardButton.callback("نه", ProjectCallbacks.view(project.getId())))
                        .build());
    }

    static Screen notAvailable() {
        return new Screen("این پروژه دیگه در دسترس نیست.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("🔎 پروژه‌های باز", ProjectCallbacks.open(ProjectCallbacks.ALL, 0)))
                .row(InlineKeyboardButton.callback("↩️ برگشت", ProjectCallbacks.HOME))
                .build());
    }

    // ---- shared -------------------------------------------------------------------------------------------

    static String statusEmoji(Project project, boolean takingBids) {
        return switch (project.getStatus()) {
            case AWARDED -> "✅";
            case CANCELLED -> "🚫";
            case OPEN -> takingBids ? "🟢" : "⏳";
        };
    }

    /** Adds a "previous / next" row when the list has more than one page. */
    static void addPaging(KeyboardBuilder keyboard, Page<?> page, IntFunction<String> callback) {
        List<InlineKeyboardButton> navigation = new ArrayList<>();
        if (page.hasPrevious()) {
            navigation.add(InlineKeyboardButton.callback("▶️ قبلی", callback.apply(page.getNumber() - 1)));
        }
        if (page.hasNext()) {
            navigation.add(InlineKeyboardButton.callback("بعدی ◀️", callback.apply(page.getNumber() + 1)));
        }
        if (!navigation.isEmpty()) {
            keyboard.grid(CalendarGrid.arrangeRow(navigation), navigation.size());
        }
    }
}
