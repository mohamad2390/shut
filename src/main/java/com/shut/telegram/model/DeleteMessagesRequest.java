package com.shut.telegram.model;

import java.util.List;

/** Deletes up to 100 messages of a chat at once; messages that cannot be deleted are skipped. */
@TelegramModel
public record DeleteMessagesRequest(String chatId, List<Long> messageIds) {
}
