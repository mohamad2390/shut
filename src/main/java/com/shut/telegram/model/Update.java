package com.shut.telegram.model;

/** One incoming event from Telegram: a message, a button press or a tap inside a widget. */
@TelegramModel
public record Update(long updateId, Message message, CallbackQuery callbackQuery, WidgetQuery widgetQuery) {
}
