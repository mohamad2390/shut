package com.shut.telegram.model;

/** Where Telegram keeps a file: {@code filePath} is what the download URL needs. */
@TelegramModel
public record TelegramFile(String fileId, String fileUniqueId, Long fileSize, String filePath) {
}
