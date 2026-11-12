package com.shut.telegram.widget;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.model.SendWidgetRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sends a widget to a chat and remembers what it is about, so that taps inside it can be answered.
 * Widgets are sent alone, with no caption, so that they look like part of an app and not like a chat message.
 */
@Component
@TelegramBotEnabled
public class WidgetDelivery {

    private static final Logger log = LoggerFactory.getLogger(WidgetDelivery.class);

    private final TelegramApiClient telegram;
    private final WidgetMessageRepository widgets;

    public WidgetDelivery(TelegramApiClient telegram, WidgetMessageRepository widgets) {
        this.telegram = telegram;
        this.widgets = widgets;
    }

    /**
     * Sends the widget as a new message and deletes the widget of the same kind about the same member that this chat
     * got earlier, so asking again moves the widget to the bottom of the chat instead of piling up copies.
     * Returns false if the widget could not be sent, in which case the earlier widget stays.
     *
     * @param chatData the widget's first state, at most 1024 bytes
     */
    public boolean replace(long chatId, long providerId, WidgetKind kind, String html, String chatData) {
        try {
            List<WidgetMessage> earlier = widgets.findByChatIdAndProviderIdAndKind(chatId, providerId, kind);
            long messageId = telegram.sendWidget(new SendWidgetRequest(chatId, html, null, true, chatData, null));
            widgets.save(new WidgetMessage(chatId, messageId, providerId, kind));
            earlier.forEach(widget -> remove(chatId, widget));
            return true;
        } catch (TelegramApiException e) {
            log.warn("Could not send the {} widget to chat {}: {}", kind, chatId, e.getMessage());
            return false;
        }
    }

    /** Deletes an earlier widget message. The member may have deleted it already, which is fine. */
    private void remove(long chatId, WidgetMessage widget) {
        try {
            telegram.deleteMessage(Long.toString(chatId), widget.getMessageId());
        } catch (TelegramApiException e) {
            log.debug("Earlier widget {} in chat {} was already gone: {}", widget.getMessageId(), chatId, e.getMessage());
        }
        widgets.delete(widget);
    }
}
