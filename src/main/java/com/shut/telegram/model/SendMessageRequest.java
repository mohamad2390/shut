package com.shut.telegram.model;

@TelegramModel
public record SendMessageRequest(long chatId, String text, String parseMode, InlineKeyboardMarkup replyMarkup) {

    public static SendMessageRequest text(long chatId, String text) {
        return new SendMessageRequest(chatId, text, null, null);
    }

    public SendMessageRequest withReplyMarkup(InlineKeyboardMarkup markup) {
        return new SendMessageRequest(chatId, text, parseMode, markup);
    }
}
