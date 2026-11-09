package com.shut.telegram.showcase;

import com.shut.showcase.ShowcaseMediaType;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.PhotoSize;

import java.util.Comparator;
import java.util.Optional;

/** The photo, video or file in a message, reduced to what the portfolio needs. */
record ReceivedMedia(ShowcaseMediaType type, String fileId, String fileUniqueId) {

    /** Empty when the message has no photo, video, or image/video file. */
    static Optional<ReceivedMedia> from(Message message) {
        if (message.photo() != null && !message.photo().isEmpty()) {
            PhotoSize largest = message.photo().stream()
                    .max(Comparator.comparingLong(size -> (long) size.width() * size.height()))
                    .orElseThrow();
            return Optional.of(new ReceivedMedia(ShowcaseMediaType.PHOTO, largest.fileId(), largest.fileUniqueId()));
        }
        if (message.video() != null) {
            return Optional.of(new ReceivedMedia(
                    ShowcaseMediaType.VIDEO, message.video().fileId(), message.video().fileUniqueId()));
        }
        if (message.document() != null && isImageOrVideo(message.document().mimeType())) {
            return Optional.of(new ReceivedMedia(
                    ShowcaseMediaType.DOCUMENT, message.document().fileId(), message.document().fileUniqueId()));
        }
        return Optional.empty();
    }

    private static boolean isImageOrVideo(String mimeType) {
        return mimeType != null && (mimeType.startsWith("image/") || mimeType.startsWith("video/"));
    }
}
