package com.shut.telegram.model;

/** One size of an uploaded photo. Telegram sends several sizes of every photo. */
@TelegramModel
public record PhotoSize(String fileId, String fileUniqueId, int width, int height) {
}
