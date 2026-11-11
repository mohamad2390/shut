package com.shut.telegram.messaging;

import com.shut.telegram.model.InlineKeyboardButton;

import java.util.Optional;
import java.util.regex.Pattern;

/** Links to Telegram users. Telegram only opens a chat from a link when the person has a username. */
public final class TelegramLinks {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{4,32}");

    private TelegramLinks() {
    }

    /** A "message on Telegram" button, or empty if the username is missing or not a valid Telegram username. */
    public static Optional<InlineKeyboardButton> messageButton(String username) {
        if (username == null || !USERNAME.matcher(username).matches()) {
            return Optional.empty();
        }
        return Optional.of(InlineKeyboardButton.link("💬 پیام در تلگرام", "https://t.me/" + username));
    }
}
