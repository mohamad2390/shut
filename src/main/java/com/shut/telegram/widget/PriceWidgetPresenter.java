package com.shut.telegram.widget;

import com.shut.pricing.PriceItem;
import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Shows a member's prices in the price widget. */
@Component
@TelegramBotEnabled
public class PriceWidgetPresenter {

    private final WidgetDelivery delivery;
    private final WidgetHtml html;

    public PriceWidgetPresenter(WidgetDelivery delivery, WidgetHtml html) {
        this.delivery = delivery;
        this.html = html;
    }

    /**
     * Sends the widget with the member's prices, replacing the one this chat got earlier.
     * Returns false if it could not be sent, and the caller should show the prices as text instead.
     */
    public boolean show(long chatId, long providerId, String providerName, List<PriceItem> items) {
        if (html.prices() == null) {
            return false;
        }
        return delivery.replace(chatId, providerId, WidgetKind.PRICES, html.prices(),
                PriceWidgetState.page(heading(providerName), items, 1));
    }

    static String heading(String providerName) {
        return "قیمت‌های " + providerName;
    }
}
