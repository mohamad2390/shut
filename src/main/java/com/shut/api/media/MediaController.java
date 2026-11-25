package com.shut.api.media;

import com.shut.common.web.ApiPaths;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Pictures and videos for the web app. The bytes come through our server so the Telegram bot token is never exposed.
 * A file never changes under its id, so browsers may keep it for a day.
 */
@RestController
public class MediaController {

    private static final Duration MAX_AGE = Duration.ofDays(1);

    private final MediaService media;

    public MediaController(MediaService media) {
        this.media = media;
    }

    @GetMapping(ApiPaths.Venues.BASE + ApiPaths.Venues.PHOTO)
    public ResponseEntity<byte[]> venuePhoto(@PathVariable Long id, @PathVariable Long photoId) {
        return respond(media.venuePhoto(id, photoId));
    }

    @GetMapping(ApiPaths.Providers.BASE + ApiPaths.Providers.SHOWCASE_MEDIA)
    public ResponseEntity<byte[]> showcaseMedia(@PathVariable Long id, @PathVariable Long itemId) {
        return respond(media.showcaseItem(id, itemId));
    }

    static ResponseEntity<byte[]> respond(MediaService.Media found) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(found.file().contentType()))
                .cacheControl(CacheControl.maxAge(MAX_AGE).cachePublic())
                .eTag("\"" + found.tag() + "\"")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(found.file().content());
    }
}
