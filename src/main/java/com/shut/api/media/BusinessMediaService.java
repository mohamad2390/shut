package com.shut.api.media;

import com.shut.rental.RentalItemPhoto;
import com.shut.rental.RentalService;
import com.shut.studio.StudioMedia;
import com.shut.studio.StudioService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Serves studio samples and rental photos to the web app under the bot's visibility rules: a studio file only when the
 * studio is listed, a rental photo only while its item is available in a listed shop.
 */
@Service
public class BusinessMediaService {

    private final StudioService studios;
    private final RentalService rental;
    private final ObjectProvider<MediaSource> source;

    public BusinessMediaService(StudioService studios, RentalService rental, ObjectProvider<MediaSource> source) {
        this.studios = studios;
        this.rental = rental;
        this.source = source;
    }

    /** @throws MediaNotFoundException if the studio is not visible or the file is not one of its samples */
    public MediaService.Media studioMedia(Long studioId, Long mediaId) {
        studios.findListed(studioId).orElseThrow(() -> new MediaNotFoundException("Studio not found: " + studioId));
        StudioMedia found = studios.media(studioId).stream()
                .filter(candidate -> candidate.getId().equals(mediaId)).findFirst()
                .orElseThrow(() -> new MediaNotFoundException("File not found: " + mediaId));
        return new MediaService.Media(fetch(found.getFileId()), found.getFileUniqueId());
    }

    /** @throws MediaNotFoundException if the item is not visible or the photo is not one of its photos */
    public MediaService.Media rentalPhoto(Long itemId, Long photoId) {
        rental.publicItem(itemId).orElseThrow(() -> new MediaNotFoundException("Item not found: " + itemId));
        RentalItemPhoto found = rental.photos(itemId).stream()
                .filter(candidate -> candidate.getId().equals(photoId)).findFirst()
                .orElseThrow(() -> new MediaNotFoundException("Photo not found: " + photoId));
        return new MediaService.Media(fetch(found.getFileId()), found.getFileUniqueId());
    }

    private MediaFile fetch(String fileId) {
        MediaSource media = source.getIfAvailable();
        if (media == null) {
            throw new MediaUnavailableException("Media storage is not connected");
        }
        return media.fetch(fileId);
    }
}
