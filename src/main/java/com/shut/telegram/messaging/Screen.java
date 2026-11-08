package com.shut.telegram.messaging;

import com.shut.telegram.model.InlineKeyboardMarkup;

/** What the user sees: a text and, optionally, a keyboard under it. */
public record Screen(String text, InlineKeyboardMarkup markup) {

    /** Telegram rejects messages longer than this. */
    private static final int MAX_TEXT_LENGTH = 4096;

    public Screen {
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH - 1) + "…";
        }
    }

    public static Screen of(String text) {
        return new Screen(text, null);
    }
}
