package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.showcase.ShowcaseService;
import org.springframework.stereotype.Component;

/** Shows the categories of a member's showcase in a widget. */
@Component
@TelegramBotEnabled
public class ShowcaseWidgetPresenter {

    private final WidgetDelivery delivery;
    private final WidgetHtml html;
    private final ShowcaseService showcase;

    ShowcaseWidgetPresenter(WidgetDelivery delivery, WidgetHtml html, ShowcaseService showcase) {
        this.delivery = delivery;
        this.html = html;
        this.showcase = showcase;
    }

    /** Sends the widget, replacing the one this chat got earlier. False means it could not be sent. */
    public boolean show(long chatId, ProviderDetails details) {
        if (html.showcase() == null) {
            return false;
        }
        return delivery.replace(chatId, details.userId(), WidgetKind.SHOWCASE, html.showcase(),
                ShowcaseWidgetState.of(WidgetNames.of(details), showcase.countsByCategory(details.userId())));
    }
}
