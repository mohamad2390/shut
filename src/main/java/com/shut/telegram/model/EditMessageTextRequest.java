package com.shut.telegram.model;

@TelegramModel
public record EditMessageTextRequest(long chatId, long messageId, String text, InlineKeyboardMarkup replyMarkup) {
}
