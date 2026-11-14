package com.shut.telegram.cleanup;

import com.shut.telegram.config.TelegramBotEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Remembers which messages are in which chat. Remembering must never get in the way of sending or receiving a message,
 * so a failure here is only logged.
 */
@Component
@TelegramBotEnabled
public class ChatMessageLog {

    /** Telegram lets a bot delete only messages from the last 48 hours. */
    static final Duration DELETABLE_FOR = Duration.ofHours(48);

    private static final Logger log = LoggerFactory.getLogger(ChatMessageLog.class);

    private final ChatMessageRepository messages;

    public ChatMessageLog(ChatMessageRepository messages) {
        this.messages = messages;
    }

    public void record(long chatId, long messageId) {
        try {
            messages.save(new ChatMessage(chatId, messageId));
        } catch (RuntimeException e) {
            log.warn("Could not remember message {} of chat {}: {}", messageId, chatId, e.getMessage());
        }
    }

    /** The messages remembered for a chat, newest first, without repeats. */
    List<Long> messageIds(long chatId) {
        return messages.messageIds(chatId).stream().distinct().toList();
    }

    void forget(long chatId) {
        messages.deleteAllOf(chatId);
    }

    /** Messages older than 48 hours cannot be deleted any more, so there is no use in remembering them. */
    @Scheduled(fixedDelayString = "PT6H", initialDelayString = "PT1M")
    void forgetOld() {
        try {
            int removed = messages.deleteOlderThan(OffsetDateTime.now().minus(DELETABLE_FOR));
            log.debug("Forgot {} old messages", removed);
        } catch (RuntimeException e) {
            log.warn("Could not forget old messages: {}", e.getMessage());
        }
    }
}
