package com.shut.telegram.model;

@TelegramModel
public record CopyMessageRequest(String chatId, long fromChatId, long messageId) {
}
