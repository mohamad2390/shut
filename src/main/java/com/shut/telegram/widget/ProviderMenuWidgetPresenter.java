package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.review.ReviewService;
import org.springframework.stereotype.Component;

/** Shows a member's four options (profile, calendar, prices, showcase) in a widget. */
@Component
@TelegramBotEnabled
public class ProviderMenuWidgetPresenter {

    private final WidgetDelivery delivery;
    private final WidgetHtml html;
    private final ReviewService reviews;

    public ProviderMenuWidgetPresenter(WidgetDelivery delivery, WidgetHtml html, ReviewService reviews) {
        this.delivery = delivery;
        this.html = html;
        this.reviews = reviews;
    }

    /** Sends the widget, replacing the one this chat got earlier. False means it could not be sent. */
    public boolean show(long chatId, ProviderDetails details) {
        if (html.menu() == null) {
            return false;
        }
        return delivery.replace(chatId, details.userId(), WidgetKind.PROVIDER_MENU, html.menu(),
                ProviderMenuWidgetState.of(WidgetNames.of(details), reviews.summary(details.userId())));
    }
}
