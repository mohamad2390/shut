package com.shut.telegram.model;

@TelegramModel
public record Document(String fileId, String fileUniqueId, String mimeType) {
}
