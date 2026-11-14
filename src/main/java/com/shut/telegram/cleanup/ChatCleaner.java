package com.shut.telegram.cleanup;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.widget.WidgetMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deletes everything in a chat that the bot knows about: its own messages, widgets and media, and what the user
 * sent. Telegram only allows deleting messages from the last 48 hours, so older ones stay.
 */
@Component
@TelegramBotEnabled
public class ChatCleaner {

    /** What Telegram accepts in one deleteMessages call. */
    static final int BATCH_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(ChatCleaner.class);

    private final TelegramApiClient telegram;
    private final ChatMessageLog messages;
    private final WidgetMessageRepository widgets;

    public ChatCleaner(TelegramApiClient telegram, ChatMessageLog messages, WidgetMessageRepository widgets) {
        this.telegram = telegram;
        this.messages = messages;
        this.widgets = widgets;
    }

    public void clear(long chatId) {
        List<Long> ids = messages.messageIds(chatId);
        for (int from = 0; from < ids.size(); from += BATCH_SIZE) {
            List<Long> batch = ids.subList(from, Math.min(ids.size(), from + BATCH_SIZE));
            try {
                telegram.deleteMessages(Long.toString(chatId), batch);
            } catch (TelegramApiException e) {
                log.warn("Could not delete {} messages of chat {}: {}", batch.size(), chatId, e.getMessage());
            }
        }
        messages.forget(chatId);
        widgets.deleteByChatId(chatId);
    }
}
