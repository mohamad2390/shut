package com.shut.telegram.model;

/** A button of the keyboard under the text box. With {@code requestContact}, pressing it shares the user's number. */
@TelegramModel
public record KeyboardButton(String text, boolean requestContact) {
}
