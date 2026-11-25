package com.shut.api.media;

import com.shut.browse.ProviderNotFoundException;
import com.shut.browse.ProviderSearchService;
import com.shut.showcase.ShowcaseItem;
import com.shut.showcase.ShowcaseService;
import com.shut.venue.VenueNotFoundException;
import com.shut.venue.VenuePhoto;
import com.shut.venue.VenueService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Serves pictures and videos to the web app under the same visibility rules as the bot: a venue photo only when the
 * venue is listed, a showcase item only when its owner has a public page.
 */
@Service
public class MediaService {

    private final VenueService venues;
    private final ShowcaseService showcase;
    private final ProviderSearchService providers;
    private final ObjectProvider<MediaSource> source;

    public MediaService(VenueService venues, ShowcaseService showcase, ProviderSearchService providers,
                        ObjectProvider<MediaSource> source) {
        this.venues = venues;
        this.showcase = showcase;
        this.providers = providers;
        this.source = source;
    }

    /** A picture plus the id Telegram gives it, which never changes and so works as a cache tag. */
    public record Media(MediaFile file, String tag) {
    }

    /** @throws VenueNotFoundException if the venue is not visible or the photo is not one of its photos */
    public Media venuePhoto(Long venueId, Long photoId) {
        venues.findListed(venueId).orElseThrow(() -> new VenueNotFoundException(venueId));
        VenuePhoto photo = venues.photos(venueId).stream()
                .filter(candidate -> candidate.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new MediaNotFoundException("Photo not found: " + photoId));
        return new Media(fetch(photo.getFileId()), photo.getFileUniqueId());
    }

    /** @throws ProviderNotFoundException if the member has no public page */
    public Media showcaseItem(Long userId, Long itemId) {
        providers.find(userId).orElseThrow(() -> new ProviderNotFoundException(userId));
        ShowcaseItem item = showcase.find(itemId, userId)
                .orElseThrow(() -> new MediaNotFoundException("Showcase item not found: " + itemId));
        return new Media(fetch(item.getFileId()), item.getFileUniqueId());
    }

    private MediaFile fetch(String fileId) {
        MediaSource media = source.getIfAvailable();
        if (media == null) {
            throw new MediaUnavailableException("Media storage is not connected");
        }
        return media.fetch(fileId);
    }
}
