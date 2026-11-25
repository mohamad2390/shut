package com.shut.telegram.model;

import java.util.List;

/** A message that also shows or removes the keyboard under the text box (needed to ask for a phone number). */
@TelegramModel
public record SendReplyKeyboardRequest(long chatId, String text, Object replyMarkup) {

    /** Shows one button that shares the user's own phone number. */
    public static SendReplyKeyboardRequest askForContact(long chatId, String text, String buttonText) {
        return new SendReplyKeyboardRequest(chatId, text, new ReplyKeyboardMarkup(
                List.of(List.of(new KeyboardButton(buttonText, true))), true, true));
    }

    public static SendReplyKeyboardRequest removeKeyboard(long chatId, String text) {
        return new SendReplyKeyboardRequest(chatId, text, new ReplyKeyboardRemove(true));
    }
}
