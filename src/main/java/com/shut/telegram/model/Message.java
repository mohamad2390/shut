package com.shut.telegram.model;

import java.util.List;

/** A chat message. Media fields are null when the message carries no such media. */
@TelegramModel
public record Message(
        long messageId,
        TelegramUser from,
        Chat chat,
        String text,
        String caption,
        List<PhotoSize> photo,
        Video video,
        Document document,
        String mediaGroupId
) {
}
