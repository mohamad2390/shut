package com.shut.telegram.model;

/** A press on an inline keyboard button. {@code message} is the message the keyboard is attached to. */
@TelegramModel
public record CallbackQuery(String id, TelegramUser from, Message message, String data) {
}
