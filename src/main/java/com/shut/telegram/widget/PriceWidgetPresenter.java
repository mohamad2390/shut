package com.shut.telegram.widget;

import com.shut.pricing.PriceItem;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.model.SendWidgetRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** Shows a member's prices in the price widget. */
@Component
@TelegramBotEnabled
public class PriceWidgetPresenter {

    private static final Logger log = LoggerFactory.getLogger(PriceWidgetPresenter.class);

    private final TelegramApiClient telegram;
    private final WidgetHtml html;
    private final PriceWidgetRepository widgets;

    public PriceWidgetPresenter(TelegramApiClient telegram, WidgetHtml html, PriceWidgetRepository widgets) {
        this.telegram = telegram;
        this.html = html;
        this.widgets = widgets;
    }

    /**
     * Sends the widget with the member's prices as a new message, and remembers whose prices it shows so that taps
     * inside it can be answered. A widget with the same member's prices that was sent to this chat earlier is
     * deleted, so asking again moves the widget to the bottom of the chat instead of piling up copies.
     * Returns false if the widget could not be sent, in which case the earlier widget stays and the caller should
     * show the prices as text instead.
     */
    public boolean show(long chatId, long providerId, String providerName, List<PriceItem> items) {
        if (html.prices() == null) {
            return false;
        }
        String heading = heading(providerName);
        try {
            List<PriceWidget> earlier = widgets.findByChatIdAndProviderId(chatId, providerId);
            long messageId = telegram.sendWidget(new SendWidgetRequest(chatId, html.prices(), "💰 " + heading, true,
                    PriceWidgetState.page(heading, items, 1), null));
            widgets.save(new PriceWidget(chatId, messageId, providerId));
            earlier.forEach(widget -> remove(chatId, widget));
            return true;
        } catch (TelegramApiException e) {
            log.warn("Could not send the price widget to chat {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    /** Deletes an earlier widget message. The member may have deleted it already, which is fine. */
    private void remove(long chatId, PriceWidget widget) {
        try {
            telegram.deleteMessage(Long.toString(chatId), widget.getMessageId());
        } catch (TelegramApiException e) {
            log.debug("Earlier price widget {} in chat {} was already gone: {}", widget.getMessageId(), chatId, e.getMessage());
        }
        widgets.delete(widget);
    }

    static String heading(String providerName) {
        return "قیمت‌های " + providerName;
    }
}
