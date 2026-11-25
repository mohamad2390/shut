package com.shut.api.media;

import com.shut.market.ListingNotFoundException;
import com.shut.market.ListingPhoto;
import com.shut.market.MarketService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Photos of listings, fetched through the same media source as the other pictures. Only for listings others can see. */
@Service
public class ListingMediaService {

    private final MarketService market;
    private final ObjectProvider<MediaSource> source;

    public ListingMediaService(MarketService market, ObjectProvider<MediaSource> source) {
        this.market = market;
        this.source = source;
    }

    /** @throws ListingNotFoundException if the listing is not visible */
    public MediaService.Media photo(Long listingId, Long photoId) {
        market.findPublic(listingId).orElseThrow(() -> new ListingNotFoundException(listingId));
        ListingPhoto photo = market.photos(listingId).stream()
                .filter(candidate -> candidate.getId().equals(photoId)).findFirst()
                .orElseThrow(() -> new MediaNotFoundException("Photo not found: " + photoId));
        MediaSource media = source.getIfAvailable();
        if (media == null) {
            throw new MediaUnavailableException("Media storage is not connected");
        }
        return new MediaService.Media(media.fetch(photo.getFileId()), photo.getFileUniqueId());
    }
}
