package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.equipment.EquipmentService;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.review.ReviewService;
import org.springframework.stereotype.Component;

/** Shows a member's profile in a widget. */
@Component
@TelegramBotEnabled
class ProfileWidgetPresenter {

    private final WidgetDelivery delivery;
    private final WidgetHtml html;
    private final ReviewService reviews;
    private final EquipmentService equipment;

    ProfileWidgetPresenter(WidgetDelivery delivery, WidgetHtml html, ReviewService reviews,
                           EquipmentService equipment) {
        this.delivery = delivery;
        this.html = html;
        this.reviews = reviews;
        this.equipment = equipment;
    }

    /** Sends the widget, replacing the one this chat got earlier. False means it could not be sent. */
    boolean show(long chatId, ProviderDetails details) {
        if (html.profile() == null) {
            return false;
        }
        return delivery.replace(chatId, details.userId(), WidgetKind.PROFILE, html.profile(),
                ProfileWidgetState.of(details, reviews.summary(details.userId()),
                        equipment.lines(details.userId())));
    }
}
