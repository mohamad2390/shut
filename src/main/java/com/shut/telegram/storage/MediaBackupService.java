package com.shut.telegram.storage;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.config.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.OptionalLong;

/**
 * Keeps a copy of uploaded media in a private channel, so portfolios survive if the bot's
 * file ids ever stop working. A failed backup never blocks the user.
 */
@Service
@TelegramBotEnabled
public class MediaBackupService {

    private static final Logger log = LoggerFactory.getLogger(MediaBackupService.class);

    private final TelegramApiClient telegram;
    private final String channelId;

    public MediaBackupService(TelegramApiClient telegram, TelegramProperties properties) {
        this.telegram = telegram;
        this.channelId = properties.storageChannelId();
    }

    public boolean isEnabled() {
        return channelId != null && !channelId.isBlank();
    }

    /** Copies the user's message into the backup channel; returns the id of the copy, or empty on failure. */
    public OptionalLong backup(long fromChatId, long messageId) {
        if (!isEnabled()) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(telegram.copyMessage(channelId, fromChatId, messageId));
        } catch (TelegramApiException e) {
            log.warn("Could not back up message {}: {}", messageId, e.getMessage());
            return OptionalLong.empty();
        }
    }

    /** Removes a backup copy. Does nothing when there is none. */
    public void remove(Long backupMessageId) {
        if (!isEnabled() || backupMessageId == null) {
            return;
        }
        try {
            telegram.deleteMessage(channelId, backupMessageId);
        } catch (TelegramApiException e) {
            log.warn("Could not delete backup message {}: {}", backupMessageId, e.getMessage());
        }
    }
}
