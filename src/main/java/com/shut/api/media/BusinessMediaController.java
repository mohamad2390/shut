package com.shut.api.media;

import com.shut.common.web.ApiPaths;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Pictures and videos of studios and rental items, through our server so the bot token stays private. */
@RestController
public class BusinessMediaController {

    private final BusinessMediaService media;

    public BusinessMediaController(BusinessMediaService media) {
        this.media = media;
    }

    @GetMapping(ApiPaths.Studios.BASE + ApiPaths.Studios.MEDIA)
    public ResponseEntity<byte[]> studioMedia(@PathVariable Long id, @PathVariable Long mediaId) {
        return MediaController.respond(media.studioMedia(id, mediaId));
    }

    @GetMapping(ApiPaths.Rental.BASE + ApiPaths.Rental.ITEM_PHOTO)
    public ResponseEntity<byte[]> rentalPhoto(@PathVariable Long id, @PathVariable Long photoId) {
        return MediaController.respond(media.rentalPhoto(id, photoId));
    }
}
