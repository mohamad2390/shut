package com.shut.telegram.handler;

import com.shut.telegram.model.TelegramUser;

import java.util.List;

/** What a callback handler needs to know about a button press. */
public record CallbackContext(TelegramUser user, long chatId, long messageId, List<String> args) {

    /** The {@code index}-th part of the callback data after the handler's prefix, or "" if missing. */
    public String arg(int index) {
        return index < args.size() ? args.get(index) : "";
    }
}
