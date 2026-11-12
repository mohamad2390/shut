package com.shut.telegram.model;

/**
 * Sends an HTML widget.
 *
 * @param html         the widget page: at most 32768 bytes, and at most 4096 bytes after zlib compression
 * @param caption      text shown below the widget
 * @param allowQueries lets the widget send requests to the bot (they arrive as {@code widget_query} updates)
 * @param chatData     the widget's first state, at most 1024 bytes; the widget reads it as {@code TelegramWidget.chatData}
 */
@TelegramModel
public record SendWidgetRequest(
        long chatId,
        String html,
        String caption,
        boolean allowQueries,
        String chatData,
        InlineKeyboardMarkup replyMarkup
) {
}
