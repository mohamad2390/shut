package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.pricing.PricingService;
import com.shut.review.ReviewService;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.BotMessenger;
import org.springframework.stereotype.Component;

/**
 * The widget with a member's five options. A tap ({@code "o:profile"}, {@code "o:calendar"}, {@code "o:prices"},
 * {@code "o:showcase"}, {@code "o:reviews"}) sends the widget of that option as a new message in the chat.
 */
@Component
@TelegramBotEnabled
class ProviderMenuWidgetAnswerer implements WidgetAnswerer {

    static final String FAILED = "الان نمی‌تونم این ویجت رو نشون بدم. چند لحظه‌ی دیگه دوباره امتحان کن.";

    private final ReviewService reviews;
    private final PricingService pricing;
    private final ProfileWidgetPresenter profiles;
    private final CalendarWidgetPresenter calendars;
    private final PriceWidgetPresenter prices;
    private final ShowcaseWidgetPresenter showcases;
    private final ReviewsWidgetPresenter reviewWidgets;
    private final BotMessenger messenger;

    ProviderMenuWidgetAnswerer(ReviewService reviews, PricingService pricing, ProfileWidgetPresenter profiles,
                               CalendarWidgetPresenter calendars, PriceWidgetPresenter prices,
                               ShowcaseWidgetPresenter showcases, ReviewsWidgetPresenter reviewWidgets,
                               BotMessenger messenger) {
        this.reviews = reviews;
        this.pricing = pricing;
        this.profiles = profiles;
        this.calendars = calendars;
        this.prices = prices;
        this.showcases = showcases;
        this.reviewWidgets = reviewWidgets;
        this.messenger = messenger;
    }

    @Override
    public WidgetKind kind() {
        return WidgetKind.PROVIDER_MENU;
    }

    @Override
    public String answer(ProviderDetails provider, String request) {
        return ProviderMenuWidgetState.of(WidgetNames.of(provider), reviews.summary(provider.userId()));
    }

    @Override
    public boolean open(long chatId, ProviderDetails provider, String request) {
        if (request == null) {
            return false;
        }
        Boolean sent = switch (request) {
            case "o:profile" -> profiles.show(chatId, provider);
            case "o:calendar" -> calendars.show(chatId, provider.userId(), WidgetNames.of(provider));
            case "o:prices" -> prices.show(chatId, provider.userId(), WidgetNames.of(provider),
                    pricing.list(provider.userId()));
            case "o:showcase" -> showcases.show(chatId, provider);
            case "o:reviews" -> reviewWidgets.show(chatId, provider);
            default -> null;
        };
        if (sent == null) {
            return false;
        }
        if (!sent) {
            messenger.sendText(chatId, FAILED);
        }
        return true;
    }
}
