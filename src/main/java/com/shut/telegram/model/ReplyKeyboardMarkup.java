package com.shut.telegram.model;

import java.util.List;

@TelegramModel
public record ReplyKeyboardMarkup(List<List<KeyboardButton>> keyboard, boolean resizeKeyboard, boolean oneTimeKeyboard) {
}
