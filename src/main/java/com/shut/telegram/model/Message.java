package com.shut.telegram.model;

@TelegramModel
public record Message(long messageId, TelegramUser from, Chat chat, String text) {
}
