package com.shut.telegram.model;

/** One incoming event from Telegram: either a message or a button press. */
@TelegramModel
public record Update(long updateId, Message message, CallbackQuery callbackQuery) {
}
