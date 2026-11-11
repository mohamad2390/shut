package com.shut.telegram.browse;

import com.shut.review.RatingSummary;
import com.shut.review.Review;
import com.shut.review.ReviewView;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewScreensTest {

    private static final long PROVIDER = 7;

    @Test
    void summaryLineShowsAverageWithOneDecimalAndCount() {
        assertThat(ReviewScreens.summaryLine(new RatingSummary(4.5, 12))).isEqualTo("⭐ ۴٫۵ (۱۲ نظر)");
    }

    @Test
    void summaryLineSaysSoWhenThereAreNoRatings() {
        assertThat(ReviewScreens.summaryLine(RatingSummary.NONE)).isEqualTo("هنوز امتیازی نداره");
    }

    @Test
    void listShowsReviewsAndAnInviteToRateForSomeoneWhoHasNotRated() {
        Screen screen = ReviewScreens.list("علی", PROVIDER, new RatingSummary(4, 1), page(1, 0),
                null, false);

        assertThat(screen.text()).contains("علی", "⭐⭐⭐⭐", "نگار", "سریع تحویل داد");
        assertThat(callbacks(screen)).contains(ReviewCallbacks.rate(PROVIDER))
                .doesNotContain(ReviewCallbacks.delete(PROVIDER));
    }

    @Test
    void listOffersEditAndDeleteToSomeoneWhoHasRated() {
        Review own = new Review(PROVIDER, 2L, 3);
        Screen screen = ReviewScreens.list("علی", PROVIDER, new RatingSummary(4, 1), page(1, 0), own, false);

        assertThat(screen.text()).contains("امتیاز تو: ⭐⭐⭐");
        assertThat(callbacks(screen)).contains(ReviewCallbacks.rate(PROVIDER), ReviewCallbacks.comment(PROVIDER),
                ReviewCallbacks.delete(PROVIDER));
    }

    @Test
    void aMemberLookingAtTheirOwnPageCannotRate() {
        Screen screen = ReviewScreens.list("من", PROVIDER, new RatingSummary(4, 1), page(1, 0), null, true);

        assertThat(callbacks(screen)).doesNotContain(ReviewCallbacks.rate(PROVIDER));
    }

    @Test
    void listHasNextButtonOnlyWhenThereAreMorePages() {
        Screen first = ReviewScreens.list("علی", PROVIDER, new RatingSummary(4, 6), page(6, 0), null, false);
        Screen last = ReviewScreens.list("علی", PROVIDER, new RatingSummary(4, 6), page(6, 1), null, false);

        assertThat(callbacks(first)).contains(ReviewCallbacks.list(PROVIDER, 1));
        assertThat(callbacks(last)).doesNotContain(ReviewCallbacks.list(PROVIDER, 2));
        assertThat(callbacks(last)).contains(ReviewCallbacks.list(PROVIDER, 0));
    }

    @Test
    void starChooserHasFiveButtons() {
        Screen screen = ReviewScreens.chooseStars("علی", PROVIDER);

        assertThat(callbacks(screen)).contains(ReviewCallbacks.stars(PROVIDER, 1), ReviewCallbacks.stars(PROVIDER, 5));
        assertThat(callbacks(screen).stream().filter(data -> data.startsWith("review:s:"))).hasSize(5);
    }

    @Test
    void callbackDataFitsTelegramsLimit() {
        long largeId = Long.MAX_VALUE;
        for (String data : List.of(ReviewCallbacks.list(largeId, 99999), ReviewCallbacks.rate(largeId),
                ReviewCallbacks.stars(largeId, 5), ReviewCallbacks.comment(largeId),
                ReviewCallbacks.delete(largeId), ReviewCallbacks.confirmDelete(largeId))) {
            assertThat(data.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);
        }
    }

    private static PageImpl<ReviewView> page(int total, int pageNumber) {
        List<ReviewView> all = java.util.stream.IntStream.range(0, total)
                .mapToObj(i -> new ReviewView("نگار", 4, "سریع تحویل داد", OffsetDateTime.now()))
                .toList();
        int from = Math.min(all.size(), pageNumber * 5);
        int to = Math.min(all.size(), from + 5);
        return new PageImpl<>(all.subList(from, to), PageRequest.of(pageNumber, 5), total);
    }

    private static List<String> callbacks(Screen screen) {
        return screen.markup().inlineKeyboard().stream()
                .flatMap(List::stream)
                .map(InlineKeyboardButton::callbackData)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
