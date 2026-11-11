package com.shut.telegram.browse;

import com.shut.common.text.PersianDigits;
import com.shut.review.RatingSummary;
import com.shut.review.Review;
import com.shut.review.ReviewService;
import com.shut.review.ReviewView;
import com.shut.telegram.calendar.CalendarGrid;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/** Everything the user sees while reading or writing ratings and comments about a member. */
final class ReviewScreens {

    private static final String STAR = "⭐";

    private ReviewScreens() {
    }

    /** "⭐ ۴٫۵ (۱۲ نظر)", or "هنوز امتیازی نداره". Used on the member's page. */
    static String summaryLine(RatingSummary summary) {
        if (summary.isEmpty()) {
            return "هنوز امتیازی نداره";
        }
        return STAR + " " + average(summary) + " (" + PersianDigits.toPersian(summary.count()) + " نظر)";
    }

    /**
     * @param own  what the user gave this member, or null
     * @param self the user is looking at their own page, so they cannot rate
     */
    static Screen list(String providerName, long providerId, RatingSummary summary, Page<ReviewView> reviews,
                       Review own, boolean self) {
        StringBuilder text = new StringBuilder(STAR + " نظرهای " + providerName + "\n\n");
        text.append(summary.isEmpty() ? "هنوز کسی امتیاز نداده." : "میانگین: " + summaryLine(summary));
        if (own != null) {
            text.append("\nامتیاز تو: ").append(stars(own.getRating()));
        }
        text.append("\n");
        reviews.forEach(review -> {
            text.append("\n").append(stars(review.rating())).append("  ").append(review.reviewerName());
            if (review.comment() != null) {
                text.append("\n").append(review.comment());
            }
            text.append("\n");
        });

        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (!self) {
            if (own == null) {
                keyboard.row(InlineKeyboardButton.callback("⭐ امتیاز دادن", ReviewCallbacks.rate(providerId)));
            } else {
                keyboard.row(InlineKeyboardButton.callback("⭐ تغییر امتیاز", ReviewCallbacks.rate(providerId)),
                        InlineKeyboardButton.callback("✍️ نظر من", ReviewCallbacks.comment(providerId)));
                keyboard.row(InlineKeyboardButton.callback("🗑 حذف نظر من", ReviewCallbacks.delete(providerId)));
            }
        }
        List<InlineKeyboardButton> navigation = new ArrayList<>();
        if (reviews.hasPrevious()) {
            navigation.add(InlineKeyboardButton.callback("▶️ قبلی", ReviewCallbacks.list(providerId, reviews.getNumber() - 1)));
        }
        if (reviews.hasNext()) {
            navigation.add(InlineKeyboardButton.callback("بعدی ◀️", ReviewCallbacks.list(providerId, reviews.getNumber() + 1)));
        }
        if (!navigation.isEmpty()) {
            keyboard.grid(CalendarGrid.arrangeRow(navigation), navigation.size());
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ برگشت به پروفایل", BrowseCallbacks.provider(providerId)));
        return new Screen(text.toString().stripTrailing(), keyboard.build());
    }

    static Screen chooseStars(String providerName, long providerId) {
        List<InlineKeyboardButton> buttons = IntStream
                .rangeClosed(ReviewService.MIN_RATING, ReviewService.MAX_RATING)
                .mapToObj(count -> InlineKeyboardButton.callback(
                        PersianDigits.toPersian(count) + " " + STAR, ReviewCallbacks.stars(providerId, count)))
                .toList();
        return new Screen(
                STAR + " امتیاز به " + providerName + "\n\nاز ۱ تا ۵ چند می‌دی؟",
                new KeyboardBuilder()
                        .grid(CalendarGrid.arrangeRow(buttons), buttons.size())
                        .row(InlineKeyboardButton.callback("انصراف", ReviewCallbacks.list(providerId, 0)))
                        .build());
    }

    static Screen rated(String providerName, long providerId, int rating) {
        return new Screen(
                "✅ امتیازت به " + providerName + " ثبت شد: " + stars(rating)
                        + "\n\nاگه می‌خوای، یه نظر هم بنویس تا بقیه بدونن تجربه‌ات چطور بوده.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("✍️ نوشتن نظر", ReviewCallbacks.comment(providerId)))
                        .row(InlineKeyboardButton.callback("تمام", ReviewCallbacks.list(providerId, 0)))
                        .build());
    }

    static Screen askComment(String providerName, long providerId, String existing) {
        StringBuilder text = new StringBuilder("✍️ نظرت درباره‌ی " + providerName + " رو بفرست (حداکثر "
                + PersianDigits.toPersian(ReviewService.MAX_COMMENT_LENGTH) + " حرف).");
        if (existing != null) {
            text.append("\n\nنظر فعلی:\n").append(existing)
                    .append("\n\nبرای پاک کردنش فقط - بفرست.");
        }
        return new Screen(text.toString(), new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("انصراف", ReviewCallbacks.list(providerId, 0)))
                .build());
    }

    static Screen confirmDelete(String providerName, long providerId) {
        return new Screen(
                "امتیاز و نظرت درباره‌ی " + providerName + " حذف بشه؟",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("بله، حذف کن", ReviewCallbacks.confirmDelete(providerId)),
                                InlineKeyboardButton.callback("نه", ReviewCallbacks.list(providerId, 0)))
                        .build());
    }

    static String stars(int count) {
        return STAR.repeat(count);
    }

    private static String average(RatingSummary summary) {
        String text = String.format(Locale.ROOT, "%.1f", summary.average()).replace('.', '٫');
        return PersianDigits.toPersian(text);
    }
}
