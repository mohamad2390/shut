package com.shut.telegram.client;

/** Raised when a Bot API call fails. The message never contains the bot token. */
public class TelegramApiException extends RuntimeException {

    public TelegramApiException(String message) {
        super(message);
    }
}
