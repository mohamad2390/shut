package com.shut.telegram.model;

/**
 * A tap inside a widget the bot sent.
 *
 * @param data the text the widget passed to {@code TelegramWidget.request}
 */
@TelegramModel
public record WidgetQuery(
        String id,
        TelegramUser from,
        Chat chat,
        Long messageId,
        String chatInstance,
        String widgetId,
        String data
) {
}
