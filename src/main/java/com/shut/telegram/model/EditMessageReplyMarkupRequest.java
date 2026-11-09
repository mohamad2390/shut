package com.shut.telegram.model;

@TelegramModel
public record EditMessageReplyMarkupRequest(long chatId, long messageId, InlineKeyboardMarkup replyMarkup) {
}
