package com.shut.telegram.browse;

import com.shut.browse.ProviderDetails;
import com.shut.browse.ProviderSearchService;
import com.shut.review.Review;
import com.shut.review.ReviewService;
import com.shut.review.SelfReviewException;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Handles every button for rating a member and writing about them. */
@Component
@TelegramBotEnabled
class ReviewCallbackHandler extends CallbackHandler {

    static final String PROVIDER_KEY = "providerId";

    private final BotMessenger messenger;
    private final ProviderSearchService providers;
    private final ReviewService reviews;
    private final TelegramAccountResolver accounts;
    private final ConversationService conversations;
    private final ReviewPages pages;
    private final BrowseSession session;

    ReviewCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                          ProviderSearchService providers, ReviewService reviews,
                          TelegramAccountResolver accounts, ConversationService conversations, ReviewPages pages,
                          BrowseSession session) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.providers = providers;
        this.reviews = reviews;
        this.accounts = accounts;
        this.conversations = conversations;
        this.pages = pages;
        this.session = session;
    }

    @Override
    protected String prefix() {
        return ReviewCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        Optional<Long> providerId = parseLong(context.arg(1));
        if (providerId.isEmpty()) {
            return;
        }
        long id = providerId.get();
        switch (context.arg(0)) {
            case "l" -> showList(context, id, parseLong(context.arg(2)).orElse(0L).intValue());
            case "rate" -> withProvider(context, id, details ->
                    ReviewScreens.chooseStars(BrowseScreens.name(details), id));
            case "s" -> saveStars(context, id);
            case "c" -> askComment(context, id);
            case "d" -> withProvider(context, id, details ->
                    ReviewScreens.confirmDelete(BrowseScreens.name(details), id));
            case "dok" -> delete(context, id);
            default -> { }
        }
    }

    private void showList(CallbackContext context, long providerId, int page) {
        session.resume(context.user().id());
        show(context, pages.list(context.user(), providerId, Math.max(0, page)));
    }

    private void saveStars(CallbackContext context, long providerId) {
        int stars = parseLong(context.arg(2)).orElse(0L).intValue();
        Optional<ProviderDetails> details = providers.find(providerId);
        if (details.isEmpty()) {
            show(context, BrowseScreens.providerGone());
            return;
        }
        try {
            reviews.rate(providerId, viewerId(context), stars);
        } catch (SelfReviewException | IllegalArgumentException e) {
            showList(context, providerId, 0);
            return;
        }
        show(context, ReviewScreens.rated(BrowseScreens.name(details.get()), providerId, stars));
    }

    /** Only someone who has already given stars can write a comment. */
    private void askComment(CallbackContext context, long providerId) {
        Optional<ProviderDetails> details = providers.find(providerId);
        Optional<Review> own = reviews.findOwn(providerId, viewerId(context));
        if (details.isEmpty() || own.isEmpty()) {
            showList(context, providerId, 0);
            return;
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_REVIEW_COMMENT,
                session.withSearch(context.user().id(), Map.of(PROVIDER_KEY, Long.toString(providerId))));
        show(context, ReviewScreens.askComment(BrowseScreens.name(details.get()), providerId,
                own.get().getComment()));
    }

    private void delete(CallbackContext context, long providerId) {
        reviews.deleteOwn(providerId, viewerId(context));
        showList(context, providerId, 0);
    }

    private void withProvider(CallbackContext context, long providerId, Function<ProviderDetails, Screen> screen) {
        show(context, providers.find(providerId).map(screen).orElseGet(BrowseScreens::providerGone));
    }

    private Long viewerId(CallbackContext context) {
        return accounts.resolve(context.user()).getId();
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }

    private static Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
