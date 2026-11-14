package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.stereotype.Component;

/** Shows the ratings and comments about a member in a widget. */
@Component
@TelegramBotEnabled
class ReviewsWidgetPresenter {

    private final WidgetDelivery delivery;
    private final WidgetHtml html;
    private final ReviewsWidgetAnswerer answerer;

    ReviewsWidgetPresenter(WidgetDelivery delivery, WidgetHtml html, ReviewsWidgetAnswerer answerer) {
        this.delivery = delivery;
        this.html = html;
        this.answerer = answerer;
    }

    /** Sends the widget on its first page, replacing the one this chat got earlier. False means it could not be sent. */
    boolean show(long chatId, ProviderDetails details) {
        if (html.reviews() == null) {
            return false;
        }
        return delivery.replace(chatId, details.userId(), WidgetKind.REVIEWS, html.reviews(), answerer.state(details, 1));
    }
}
