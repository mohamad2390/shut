package com.shut.telegram.showcase;

import com.shut.showcase.ShowcaseMediaType;
import com.shut.telegram.model.Document;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.PhotoSize;
import com.shut.telegram.model.Video;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReceivedMediaTest {

    private static Message message(List<PhotoSize> photo, Video video, Document document) {
        return new Message(1, null, null, null, null, photo, video, document, null);
    }

    @Test
    void picksTheLargestPhotoSize() {
        Message message = message(List.of(
                new PhotoSize("small", "u-small", 90, 60),
                new PhotoSize("large", "u-large", 1280, 853),
                new PhotoSize("medium", "u-medium", 320, 213)), null, null);

        ReceivedMedia media = ReceivedMedia.from(message).orElseThrow();

        assertThat(media.type()).isEqualTo(ShowcaseMediaType.PHOTO);
        assertThat(media.fileId()).isEqualTo("large");
        assertThat(media.fileUniqueId()).isEqualTo("u-large");
    }

    @Test
    void readsVideos() {
        ReceivedMedia media = ReceivedMedia.from(message(null, new Video("v1", "uv1"), null)).orElseThrow();

        assertThat(media.type()).isEqualTo(ShowcaseMediaType.VIDEO);
    }

    @Test
    void acceptsImageAndVideoFilesButNotOtherFiles() {
        assertThat(ReceivedMedia.from(message(null, null, new Document("d1", "ud1", "image/jpeg")))).isPresent();
        assertThat(ReceivedMedia.from(message(null, null, new Document("d2", "ud2", "video/mp4")))).isPresent();
        assertThat(ReceivedMedia.from(message(null, null, new Document("d3", "ud3", "application/pdf")))).isEmpty();
    }

    @Test
    void ignoresPlainTextMessages() {
        assertThat(ReceivedMedia.from(message(null, null, null))).isEmpty();
    }
}
