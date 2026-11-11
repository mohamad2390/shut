package com.shut.telegram.project;

import com.shut.common.text.PersianDigits;
import com.shut.project.Bid;
import com.shut.project.BidService;
import com.shut.project.BidStatus;
import com.shut.project.Project;
import com.shut.project.ProjectStatus;
import com.shut.review.RatingSummary;
import com.shut.telegram.browse.BrowseCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.messaging.TelegramLinks;
import com.shut.telegram.model.InlineKeyboardButton;
import org.springframework.data.domain.Page;

import java.util.Locale;
import java.util.Map;

/** Everything the user sees while writing, reading and choosing offers. */
final class BidScreens {

    private BidScreens() {
    }

    // ---- the provider writes an offer ---------------------------------------------------------------------

    /** @param existing the provider's earlier offer, or null */
    static Screen askAmount(Project project, Bid existing) {
        StringBuilder text = new StringBuilder("💼 پیشنهاد برای «" + project.getTitle() + "»\n\n");
        text.append("بودجه‌ی صاحب پروژه: ").append(ProjectText.budget(project)).append("\n");
        if (existing != null) {
            text.append("پیشنهاد فعلی تو: ").append(ProjectText.money(existing.getAmount())).append("\n");
        }
        text.append("\nقیمتت رو به تومان و فقط با عدد بفرست. مثلاً ۴۵۰۰۰۰۰");
        return new Screen(text.toString(), new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("انصراف", BidCallbacks.ABANDON))
                .build());
    }

    static Screen askNote(long amount) {
        return new Screen("قیمت: " + ProjectText.money(amount)
                + "\n\nاگه می‌خوای یه توضیح کوتاه برای صاحب پروژه بنویس: زمان تحویل، چی شامل قیمته، نمونه‌کار مرتبط "
                + "(حداکثر " + PersianDigits.toPersian(BidService.MAX_NOTE_LENGTH) + " حرف).",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("بدون توضیح", BidCallbacks.NO_NOTE))
                        .row(InlineKeyboardButton.callback("انصراف", BidCallbacks.ABANDON))
                        .build());
    }

    static Screen withdrawConfirm(Project project) {
        return new Screen("پیشنهادت برای «" + project.getTitle() + "» پس گرفته بشه؟",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("بله، پس بگیر", BidCallbacks.confirmWithdraw(project.getId())),
                                InlineKeyboardButton.callback("نه", ProjectCallbacks.view(project.getId())))
                        .build());
    }

    // ---- the owner reads and chooses ----------------------------------------------------------------------

    /**
     * The owner's list of offers, cheapest first.
     *
     * @param names   provider names by user id
     * @param ratings provider ratings by user id
     */
    static Screen ownerList(Project project, Page<Bid> bids, Map<Long, String> names, Map<Long, RatingSummary> ratings) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        bids.forEach(bid -> keyboard.row(InlineKeyboardButton.callback(
                bidLabel(bid, names.getOrDefault(bid.getBidderId(), "یک عضو"), ratings.get(bid.getBidderId())),
                BidCallbacks.view(bid.getId()))));
        ProjectScreens.addPaging(keyboard, bids, page -> BidCallbacks.list(project.getId(), page));
        keyboard.row(InlineKeyboardButton.callback("↩️ برگشت به پروژه", ProjectCallbacks.view(project.getId())));

        String text = "📥 پیشنهادهای «" + project.getTitle() + "»\n\n"
                + (bids.isEmpty() ? "هنوز پیشنهادی نرسیده." : PersianDigits.toPersian(bids.getTotalElements())
                + " پیشنهاد، از ارزون‌ترین. برای دیدن جزئیات و انتخاب، روی هر کدوم بزن.");
        return new Screen(text, keyboard.build());
    }

    static Screen ownerDetail(Project project, Bid bid, String bidderName, RatingSummary rating, String bidderUsername) {
        StringBuilder text = new StringBuilder("💼 " + bidderName + "\n");
        text.append("پروژه: ").append(project.getTitle()).append("\n\n");
        text.append("قیمت: ").append(ProjectText.money(bid.getAmount())).append("\n");
        text.append("وضعیت: ").append(ProjectText.bidStatus(bid.getStatus())).append("\n");
        text.append("امتیاز: ").append(ratingText(rating)).append("\n");
        if (bid.getNote() != null) {
            text.append("\n").append(bid.getNote());
        }

        KeyboardBuilder keyboard = new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("👤 دیدن پروفایل و نمونه‌کارها",
                        BrowseCallbacks.provider(bid.getBidderId())));
        if (project.getStatus() == ProjectStatus.OPEN && bid.getStatus() == BidStatus.PENDING) {
            keyboard.row(InlineKeyboardButton.callback("✅ انتخاب این پیشنهاد", BidCallbacks.pick(bid.getId())));
        }
        if (bid.getStatus() == BidStatus.ACCEPTED) {
            TelegramLinks.messageButton(bidderUsername).ifPresent(keyboard::row);
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ پیشنهادها", BidCallbacks.list(project.getId(), 0)));
        return new Screen(text.toString(), keyboard.build());
    }

    static Screen pickConfirm(Project project, Bid bid, String bidderName) {
        return new Screen("پیشنهاد " + bidderName + " با قیمت " + ProjectText.money(bid.getAmount())
                + " برای «" + project.getTitle() + "» انتخاب بشه؟\n\nبعد از انتخاب، پروژه واگذار می‌شه، "
                + "به بقیه‌ی پیشنهاددهنده‌ها خبر داده می‌شه و دیگه پیشنهاد جدید قبول نمی‌شه.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("بله، انتخاب کن", BidCallbacks.confirmPick(bid.getId())),
                                InlineKeyboardButton.callback("نه", BidCallbacks.view(bid.getId())))
                        .build());
    }

    static Screen awarded(Project project, Bid bid, String bidderName, String bidderUsername) {
        StringBuilder text = new StringBuilder("✅ پروژه‌ی «" + project.getTitle() + "» به " + bidderName + " واگذار شد.\n")
                .append("قیمت توافقی: ").append(ProjectText.money(bid.getAmount())).append("\n\n");
        text.append(bidderUsername == null
                ? "این عضو نام کاربری تلگرام نداره؛ از طریق پروفایلش یا کانال باهاش هماهنگ کن."
                : "برای هماهنگی، دکمه‌ی پیام رو بزن.");
        KeyboardBuilder keyboard = new KeyboardBuilder();
        TelegramLinks.messageButton(bidderUsername).ifPresent(keyboard::row);
        keyboard.row(InlineKeyboardButton.callback("↩️ پروژه‌های من", ProjectCallbacks.mine(0)));
        return new Screen(text.toString(), keyboard.build());
    }

    // ---- the provider's own offers -----------------------------------------------------------------------

    static Screen myBids(Page<Bid> bids, Map<Long, Project> projects) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        bids.forEach(bid -> {
            Project project = projects.get(bid.getProjectId());
            if (project != null) {
                String label = statusEmoji(bid.getStatus()) + " " + project.getTitle() + " — " + ProjectText.money(bid.getAmount());
                keyboard.row(InlineKeyboardButton.callback(
                        label.length() > 55 ? label.substring(0, 54) + "…" : label,
                        ProjectCallbacks.view(project.getId())));
            }
        });
        ProjectScreens.addPaging(keyboard, bids, BidCallbacks::mine);
        keyboard.row(InlineKeyboardButton.callback("↩️ برگشت", ProjectCallbacks.HOME));
        String text = bids.isEmpty()
                ? "🧾 پیشنهادهای من\n\nهنوز روی پروژه‌ای پیشنهاد ندادی."
                : "🧾 پیشنهادهای من\n\n⏳ در انتظار   ✅ انتخاب شد   ❌ انتخاب نشد";
        return new Screen(text, keyboard.build());
    }

    // ---- shared -------------------------------------------------------------------------------------------

    private static String bidLabel(Bid bid, String name, RatingSummary rating) {
        String label = ProjectText.money(bid.getAmount()) + " — " + name
                + (rating == null || rating.isEmpty() ? "" : " ⭐" + averageText(rating));
        return label.length() > 55 ? label.substring(0, 54) + "…" : label;
    }

    private static String ratingText(RatingSummary rating) {
        return rating == null || rating.isEmpty()
                ? "هنوز امتیازی نداره"
                : "⭐ " + averageText(rating) + " (" + PersianDigits.toPersian(rating.count()) + " نظر)";
    }

    private static String averageText(RatingSummary rating) {
        return PersianDigits.toPersian(String.format(Locale.ROOT, "%.1f", rating.average()).replace('.', '٫'));
    }

    private static String statusEmoji(BidStatus status) {
        return switch (status) {
            case PENDING -> "⏳";
            case ACCEPTED -> "✅";
            case REJECTED -> "❌";
        };
    }
}
