package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.review.RatingSummary;
import com.shut.review.ReviewService;
import com.shut.review.ReviewView;
import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/** The reviews widget asks for a page of reviews, newest first: {@code "i"} for the first, {@code "p:2"} for the second. */
@Component
@TelegramBotEnabled
class ReviewsWidgetAnswerer implements WidgetAnswerer {

    private final ReviewService reviews;

    ReviewsWidgetAnswerer(ReviewService reviews) {
        this.reviews = reviews;
    }

    @Override
    public WidgetKind kind() {
        return WidgetKind.REVIEWS;
    }

    @Override
    public String answer(ProviderDetails provider, String request) {
        return state(provider, PriceWidgetAnswerer.requestedPage(request));
    }

    /** The state for a page counted from 1; a page past the end becomes the last one. */
    String state(ProviderDetails provider, int page) {
        long userId = provider.userId();
        int index = Math.max(1, page) - 1;
        Page<ReviewView> found = reviews.page(userId, index, ReviewsWidgetState.PAGE_SIZE);
        if (found.isEmpty() && index > 0 && found.getTotalPages() > 0) {
            found = reviews.page(userId, found.getTotalPages() - 1, ReviewsWidgetState.PAGE_SIZE);
        }
        RatingSummary rating = reviews.summary(userId);
        return ReviewsWidgetState.page(WidgetNames.of(provider), rating, found.getContent(),
                found.getTotalPages(), found.getNumber() + 1);
    }
}
