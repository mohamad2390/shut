package com.shut.telegram.model;

import java.util.List;

@TelegramModel
public record InlineKeyboardMarkup(List<List<InlineKeyboardButton>> inlineKeyboard) {

    public static InlineKeyboardMarkup singleButton(InlineKeyboardButton button) {
        return new InlineKeyboardMarkup(List.of(List.of(button)));
    }
}
