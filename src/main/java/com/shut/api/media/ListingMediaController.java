package com.shut.api.media;

import com.shut.common.web.ApiPaths;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ListingMediaController {

    private final ListingMediaService media;

    public ListingMediaController(ListingMediaService media) {
        this.media = media;
    }

    @GetMapping(ApiPaths.Market.BASE + ApiPaths.Market.LISTING_PHOTO)
    public ResponseEntity<byte[]> photo(@PathVariable Long id, @PathVariable Long photoId) {
        return MediaController.respond(media.photo(id, photoId));
    }
}
