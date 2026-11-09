package com.shut.telegram.model;

@TelegramModel
public record DeleteMessageRequest(String chatId, long messageId) {
}
