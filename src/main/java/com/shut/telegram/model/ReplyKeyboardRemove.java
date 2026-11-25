package com.shut.telegram.model;

/** Takes the keyboard under the text box away. */
@TelegramModel
public record ReplyKeyboardRemove(boolean removeKeyboard) {
}
