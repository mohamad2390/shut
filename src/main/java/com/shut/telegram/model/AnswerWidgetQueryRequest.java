package com.shut.telegram.model;

/** @param data what the widget receives as the result of its request: at most 1024 bytes */
@TelegramModel
public record AnswerWidgetQueryRequest(String widgetQueryId, String data) {
}
