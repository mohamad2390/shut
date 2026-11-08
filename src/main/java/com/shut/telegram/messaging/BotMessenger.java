package com.shut.telegram.messaging;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.model.EditMessageTextRequest;
import com.shut.telegram.model.SendMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** The one place that turns a {@link Screen} into a Telegram message. */
@Component
@TelegramBotEnabled
public class BotMessenger {

    private static final Logger log = LoggerFactory.getLogger(BotMessenger.class);
    private static final String NOT_MODIFIED = "message is not modified";

    private final TelegramApiClient telegram;

    public BotMessenger(TelegramApiClient telegram) {
        this.telegram = telegram;
    }

    public void send(long chatId, Screen screen) {
        telegram.sendMessage(SendMessageRequest.text(chatId, screen.text()).withReplyMarkup(screen.markup()));
    }

    public void sendText(long chatId, String text) {
        send(chatId, Screen.of(text));
    }

    /** Shows the screen by editing the message the user pressed a button on; falls back to a new message. */
    public void show(long chatId, long messageId, Screen screen) {
        try {
            telegram.editMessageText(new EditMessageTextRequest(chatId, messageId, screen.text(), screen.markup()));
        } catch (TelegramApiException e) {
            if (e.getMessage() != null && e.getMessage().contains(NOT_MODIFIED)) {
                return;
            }
            log.debug("Could not edit message {}, sending a new one: {}", messageId, e.getMessage());
            send(chatId, screen);
        }
    }
}
