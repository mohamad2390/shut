package com.shut.telegram.browse;

import com.shut.browse.ProviderDetails;
import com.shut.browse.ProviderSearchService;
import com.shut.review.ReviewService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.TelegramUser;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Builds the ratings page of a member as one user sees it. */
@Component
@TelegramBotEnabled
class ReviewPages {

    static final int PAGE_SIZE = 5;

    private final ProviderSearchService providers;
    private final ReviewService reviews;
    private final TelegramAccountResolver accounts;

    ReviewPages(ProviderSearchService providers, ReviewService reviews, TelegramAccountResolver accounts) {
        this.providers = providers;
        this.reviews = reviews;
        this.accounts = accounts;
    }

    /** The member's ratings and comments, or the "not available" page if the member is gone. */
    Screen list(TelegramUser viewer, long providerId, int page) {
        Optional<ProviderDetails> details = providers.find(providerId);
        if (details.isEmpty()) {
            return BrowseScreens.providerGone();
        }
        Long viewerId = accounts.resolve(viewer).getId();
        return ReviewScreens.list(
                BrowseScreens.name(details.get()), providerId, reviews.summary(providerId),
                reviews.page(providerId, page, PAGE_SIZE),
                reviews.findOwn(providerId, viewerId).orElse(null),
                viewerId.equals(providerId));
    }
}
