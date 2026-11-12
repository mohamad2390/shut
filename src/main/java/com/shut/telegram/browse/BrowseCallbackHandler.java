package com.shut.telegram.browse;

import com.shut.browse.ProviderDetails;
import com.shut.browse.ProviderSearchService;
import com.shut.calendar.CalendarService;
import com.shut.common.calendar.JalaliMonth;
import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingService;
import com.shut.review.ReviewService;
import com.shut.showcase.ShowcaseCategory;
import com.shut.showcase.ShowcaseItem;
import com.shut.showcase.ShowcaseService;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.widget.PriceWidgetPresenter;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.SendMediaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Handles every button used to find other members and read their pages. Everything here is read-only. */
@Component
@TelegramBotEnabled
class BrowseCallbackHandler extends CallbackHandler {

    private static final int SHOWCASE_PAGE_SIZE = 5;
    private static final Logger log = LoggerFactory.getLogger(BrowseCallbackHandler.class);

    private final TelegramApiClient telegram;
    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final BrowseSession session;
    private final ProviderSearchService providers;
    private final ShowcaseService showcase;
    private final PricingService pricing;
    private final CalendarService calendar;
    private final ReviewService reviews;
    private final PriceWidgetPresenter priceWidgets;

    BrowseCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                          ConversationService conversations, BrowseSession session, ProviderSearchService providers,
                          ShowcaseService showcase, PricingService pricing, CalendarService calendar,
                          ReviewService reviews, PriceWidgetPresenter priceWidgets) {
        super(telegram, membershipGate);
        this.telegram = telegram;
        this.messenger = messenger;
        this.conversations = conversations;
        this.session = session;
        this.providers = providers;
        this.showcase = showcase;
        this.pricing = pricing;
        this.calendar = calendar;
        this.reviews = reviews;
        this.priceWidgets = priceWidgets;
    }

    @Override
    protected String prefix() {
        return BrowseCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "start" -> startSearch(context);
            case "all" -> show(context, session.results(context.user(), "", 0));
            case "r" -> show(context, session.lastResults(context.user(), parseInt(context.arg(1)).orElse(0)));
            case "back" -> show(context, session.lastResults(context.user(), null));
            case "p" -> withProvider(context, details ->
                    BrowseScreens.provider(details, reviews.summary(details.userId())));
            case "sc" -> withProvider(context, details ->
                    BrowseScreens.showcaseHome(details, showcase.countsByCategory(details.userId())));
            case "cal" -> withProvider(context, details -> calendarScreen(details, context));
            case "pr" -> showPrices(context);
            case "sv" -> viewShowcaseItems(context);
            default -> { }
        }
    }

    private void startSearch(CallbackContext context) {
        conversations.begin(context.user().id(), ConversationStep.AWAITING_SEARCH_QUERY, Map.of());
        show(context, BrowseScreens.searchPrompt());
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }

    /** Shows the screen built for the member in the callback, or a "not available" page if they are gone. */
    private void withProvider(CallbackContext context, Function<ProviderDetails, Screen> screen) {
        Screen result = parseLong(context.arg(1))
                .flatMap(providers::find)
                .map(screen)
                .orElseGet(BrowseScreens::providerGone);
        show(context, result);
    }

    /** The prices go in the price widget; if it cannot be sent they are shown as text. */
    private void showPrices(CallbackContext context) {
        Optional<ProviderDetails> details = parseLong(context.arg(1)).flatMap(providers::find);
        if (details.isEmpty()) {
            show(context, BrowseScreens.providerGone());
            return;
        }
        List<PriceItem> items = pricing.list(details.get().userId());
        boolean sentAsWidget = !items.isEmpty()
                && priceWidgets.show(context.chatId(), details.get().userId(), BrowseScreens.name(details.get()), items);
        if (!sentAsWidget) {
            show(context, BrowseScreens.prices(details.get(), items));
        }
    }

    private Screen calendarScreen(ProviderDetails details, CallbackContext context) {
        JalaliMonth requested = parseMonth(context).orElseGet(() -> JalaliMonth.of(calendar.today()));
        JalaliMonth month = calendar.clamp(requested);
        var statuses = calendar.statuses(details.userId(), month.atDay(1), month.atDay(month.length()));
        return BrowseScreens.calendar(details, month, statuses,
                calendar.hasPreviousMonth(month), calendar.hasNextMonth(month));
    }

    private void viewShowcaseItems(CallbackContext context) {
        Optional<Long> userId = parseLong(context.arg(1));
        Optional<ShowcaseCategory> category = ShowcaseCategory.fromKey(context.arg(2));
        if (userId.isEmpty() || category.isEmpty() || userId.flatMap(providers::find).isEmpty()) {
            show(context, BrowseScreens.providerGone());
            return;
        }
        int page = parseInt(context.arg(3)).orElse(0);
        Page<ShowcaseItem> items = showcase.page(userId.get(), category.get(), page, SHOWCASE_PAGE_SIZE);
        if (items.isEmpty()) {
            withProvider(context, details ->
                    BrowseScreens.showcaseHome(details, showcase.countsByCategory(details.userId())));
            return;
        }
        items.forEach(item -> sendItem(context.chatId(), item));
        messenger.send(context.chatId(), BrowseScreens.showcasePage(
                userId.get(), category.get(), page, SHOWCASE_PAGE_SIZE, items.getTotalElements()));
    }

    /** One broken file must not stop the rest of the page from being sent. */
    private void sendItem(long chatId, ShowcaseItem item) {
        try {
            telegram.sendMedia(switch (item.getMediaType()) {
                case PHOTO -> SendMediaRequest.photo(chatId, item.getFileId(), item.getCaption(), null);
                case VIDEO -> SendMediaRequest.video(chatId, item.getFileId(), item.getCaption(), null);
                case DOCUMENT -> SendMediaRequest.document(chatId, item.getFileId(), item.getCaption(), null);
            });
        } catch (TelegramApiException e) {
            log.warn("Could not send showcase item {}: {}", item.getId(), e.getMessage());
        }
    }

    private static Optional<JalaliMonth> parseMonth(CallbackContext context) {
        try {
            return Optional.of(new JalaliMonth(Integer.parseInt(context.arg(2)), Integer.parseInt(context.arg(3))));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Math.max(0, Integer.parseInt(value)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
