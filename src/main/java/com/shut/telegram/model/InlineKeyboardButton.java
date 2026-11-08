package com.shut.telegram.model;

@TelegramModel
public record InlineKeyboardButton(String text, String url, String callbackData) {

    public static InlineKeyboardButton link(String text, String url) {
        return new InlineKeyboardButton(text, url, null);
    }

    public static InlineKeyboardButton callback(String text, String callbackData) {
        return new InlineKeyboardButton(text, null, callbackData);
    }
}
