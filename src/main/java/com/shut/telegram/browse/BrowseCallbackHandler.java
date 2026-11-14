package com.shut.telegram.browse;

import com.shut.browse.ProviderDetails;
import com.shut.browse.ProviderSearchService;
import com.shut.calendar.CalendarService;
import com.shut.equipment.EquipmentService;
import com.shut.common.calendar.JalaliMonth;
import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingService;
import com.shut.review.ReviewService;
import com.shut.showcase.ShowcaseCategory;
import com.shut.showcase.ShowcaseService;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.widget.CalendarWidgetPresenter;
import com.shut.telegram.widget.MemberListWidgetPresenter;
import com.shut.telegram.widget.ShowcaseWidgetPresenter;
import com.shut.telegram.widget.PriceWidgetPresenter;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Handles every button used to find other members and read their pages. Everything here is read-only. */
@Component
@TelegramBotEnabled
class BrowseCallbackHandler extends CallbackHandler {

    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final BrowseSession session;
    private final ProviderSearchService providers;
    private final ShowcaseService showcase;
    private final PricingService pricing;
    private final CalendarService calendar;
    private final ReviewService reviews;
    private final PriceWidgetPresenter priceWidgets;
    private final CalendarWidgetPresenter calendarWidgets;
    private final MemberListWidgetPresenter memberWidgets;
    private final ShowcaseSender showcaseSender;
    private final ShowcaseWidgetPresenter showcaseWidgets;
    private final EquipmentService equipment;

    BrowseCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                          ConversationService conversations, BrowseSession session, ProviderSearchService providers,
                          ShowcaseService showcase, PricingService pricing, CalendarService calendar,
                          ReviewService reviews, PriceWidgetPresenter priceWidgets,
                          CalendarWidgetPresenter calendarWidgets, MemberListWidgetPresenter memberWidgets,
                          ShowcaseSender showcaseSender, ShowcaseWidgetPresenter showcaseWidgets,
                          EquipmentService equipment) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.equipment = equipment;
        this.conversations = conversations;
        this.session = session;
        this.providers = providers;
        this.showcase = showcase;
        this.pricing = pricing;
        this.calendar = calendar;
        this.reviews = reviews;
        this.priceWidgets = priceWidgets;
        this.calendarWidgets = calendarWidgets;
        this.memberWidgets = memberWidgets;
        this.showcaseSender = showcaseSender;
        this.showcaseWidgets = showcaseWidgets;
    }

    @Override
    protected String prefix() {
        return BrowseCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "start" -> startSearch(context);
            case "all" -> showEveryone(context);
            case "r" -> show(context, session.lastResults(context.user(), parseInt(context.arg(1)).orElse(0)));
            case "back" -> show(context, session.lastResults(context.user(), null));
            case "p" -> withProvider(context, details ->
                    BrowseScreens.provider(details, reviews.summary(details.userId()),
                            equipment.lines(details.userId())));
            case "sc" -> showShowcase(context);
            case "cal" -> showCalendar(context);
            case "pr" -> showPrices(context);
            case "sv" -> viewShowcaseItems(context);
            default -> { }
        }
    }

    private void startSearch(CallbackContext context) {
        conversations.begin(context.user().id(), ConversationStep.AWAITING_SEARCH_QUERY, Map.of());
        show(context, BrowseScreens.searchPrompt());
    }

    /** Everyone goes in the members widget, best rated first; the text list is the fallback if it cannot be sent. */
    private void showEveryone(CallbackContext context) {
        if (!memberWidgets.show(context.chatId())) {
            show(context, session.results(context.user(), "", 0));
        }
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

    /**
     * The calendar goes in the calendar widget, which moves between months by itself. The text calendar is the
     * fallback when the widget cannot be sent, and the buttons of the text calendar ask for a month explicitly.
     */
    private void showCalendar(CallbackContext context) {
        Optional<ProviderDetails> details = parseLong(context.arg(1)).flatMap(providers::find);
        if (details.isEmpty()) {
            show(context, BrowseScreens.providerGone());
            return;
        }
        boolean asksForMonth = parseMonth(context).isPresent();
        if (asksForMonth || !calendarWidgets.show(context.chatId(), details.get().userId(), BrowseScreens.name(details.get()))) {
            show(context, calendarScreen(details.get(), context));
        }
    }

    private Screen calendarScreen(ProviderDetails details, CallbackContext context) {
        JalaliMonth requested = parseMonth(context).orElseGet(() -> JalaliMonth.of(calendar.today()));
        JalaliMonth month = calendar.clamp(requested);
        var statuses = calendar.statuses(details.userId(), month.atDay(1), month.atDay(month.length()));
        return BrowseScreens.calendar(details, month, statuses,
                calendar.hasPreviousMonth(month), calendar.hasNextMonth(month));
    }

    /** The categories go in the showcase widget; the text list is the fallback if it cannot be sent. */
    private void showShowcase(CallbackContext context) {
        Optional<ProviderDetails> details = parseLong(context.arg(1)).flatMap(providers::find);
        if (details.isEmpty()) {
            show(context, BrowseScreens.providerGone());
            return;
        }
        if (!showcaseWidgets.show(context.chatId(), details.get())) {
            show(context, BrowseScreens.showcaseHome(details.get(), showcase.countsByCategory(details.get().userId())));
        }
    }

    private void viewShowcaseItems(CallbackContext context) {
        Optional<Long> userId = parseLong(context.arg(1));
        Optional<ShowcaseCategory> category = ShowcaseCategory.fromKey(context.arg(2));
        if (userId.isEmpty() || category.isEmpty() || userId.flatMap(providers::find).isEmpty()) {
            show(context, BrowseScreens.providerGone());
            return;
        }
        int page = parseInt(context.arg(3)).orElse(0);
        if (!showcaseSender.sendPage(context.chatId(), userId.get(), category.get(), page)) {
            withProvider(context, details ->
                    BrowseScreens.showcaseHome(details, showcase.countsByCategory(details.userId())));
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
