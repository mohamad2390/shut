package com.shut.market;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Listings of gear: the same rules for the bot and the web. */
@Service
public class MarketService {

    public static final int MAX_ACTIVE_LISTINGS_PER_SELLER = 20;
    public static final int MAX_PHOTOS = 8;

    /** What happened when a photo was added. */
    public enum PhotoResult { ADDED, DUPLICATE, LIMIT_REACHED, NOT_FOUND }

    private final ListingRepository listings;
    private final ListingPhotoRepository photos;

    public MarketService(ListingRepository listings, ListingPhotoRepository photos) {
        this.listings = listings;
        this.photos = photos;
    }

    /** Empty if the seller already has the maximum number of active listings. */
    @Transactional
    public Optional<Listing> create(Long sellerId, String title, ListingCondition condition, long price,
                                    Consumer<Listing> more) {
        if (listings.countBySellerIdAndStatus(sellerId, ListingStatus.ACTIVE) >= MAX_ACTIVE_LISTINGS_PER_SELLER) {
            return Optional.empty();
        }
        Listing listing = new Listing(sellerId, title, condition, price);
        more.accept(listing);
        return Optional.of(listings.save(listing));
    }

    @Transactional(readOnly = true)
    public Optional<Listing> find(Long id) {
        return listings.findById(id);
    }

    /** The listing if others can see it. */
    @Transactional(readOnly = true)
    public Optional<Listing> findPublic(Long id) {
        return listings.findById(id).filter(Listing::isPublic);
    }

    @Transactional(readOnly = true)
    public Optional<Listing> findOwned(Long id, Long sellerId) {
        return listings.findByIdAndSellerId(id, sellerId);
    }

    @Transactional(readOnly = true)
    public List<Listing> ownedBy(Long sellerId) {
        return listings.findBySellerIdOrderByIdDesc(sellerId);
    }

    /** @param text matches title, brand and model; empty matches all */
    @Transactional(readOnly = true)
    public Page<Listing> search(String text, String city, Long categoryId, int page, int size) {
        Pageable paging = PageRequest.of(page, size);
        return listings.searchPublic(pattern(text), pattern(city),
                categoryId == null ? ListingRepository.ANY_CATEGORY : categoryId, paging);
    }

    /** Changes the listing if it is the seller's. Empty if there is no such listing of theirs. */
    @Transactional
    public Optional<Listing> update(Long id, Long sellerId, Consumer<Listing> change) {
        return listings.findByIdAndSellerId(id, sellerId).map(listing -> {
            change.accept(listing);
            return listing;
        });
    }

    /**
     * Puts the listing back to active, or marks it sold or closed. Empty if there is no such listing of the seller's
     * or reopening would go over the limit of active listings.
     */
    @Transactional
    public Optional<Listing> setStatus(Long id, Long sellerId, ListingStatus status) {
        Optional<Listing> found = listings.findByIdAndSellerId(id, sellerId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Listing listing = found.get();
        boolean reopening = status == ListingStatus.ACTIVE && listing.getStatus() != ListingStatus.ACTIVE;
        if (reopening && listings.countBySellerIdAndStatus(sellerId, ListingStatus.ACTIVE) >= MAX_ACTIVE_LISTINGS_PER_SELLER) {
            return Optional.empty();
        }
        listing.setStatus(status);
        return Optional.of(listing);
    }

    /** Hides or shows the listing, whoever sells it; for administrators. */
    @Transactional
    public Optional<Listing> setHidden(Long id, boolean hidden) {
        return listings.findById(id).map(listing -> {
            listing.setHidden(hidden);
            return listing;
        });
    }

    @Transactional(readOnly = true)
    public Page<Listing> all(Pageable paging) {
        return listings.findAll(paging);
    }

    @Transactional(readOnly = true)
    public List<ListingPhoto> photos(Long listingId) {
        return photos.findByListingIdOrderByIdAsc(listingId);
    }

    @Transactional(readOnly = true)
    public long photoCount(Long listingId) {
        return photos.countByListingId(listingId);
    }

    @Transactional(readOnly = true)
    public PhotoResult checkCanAddPhoto(Long listingId, Long sellerId, String fileUniqueId) {
        if (listings.findByIdAndSellerId(listingId, sellerId).isEmpty()) {
            return PhotoResult.NOT_FOUND;
        }
        if (photos.existsByListingIdAndFileUniqueId(listingId, fileUniqueId)) {
            return PhotoResult.DUPLICATE;
        }
        return photos.countByListingId(listingId) >= MAX_PHOTOS ? PhotoResult.LIMIT_REACHED : PhotoResult.ADDED;
    }

    @Transactional
    public PhotoResult addPhoto(Long listingId, Long sellerId, String fileId, String fileUniqueId, Long backupMessageId) {
        PhotoResult check = checkCanAddPhoto(listingId, sellerId, fileUniqueId);
        if (check == PhotoResult.ADDED) {
            photos.save(new ListingPhoto(listingId, fileId, fileUniqueId, backupMessageId));
        }
        return check;
    }

    /** Removes every photo of the seller's listing and returns them so their backups can be removed. */
    @Transactional
    public List<ListingPhoto> clearPhotos(Long listingId, Long sellerId) {
        if (listings.findByIdAndSellerId(listingId, sellerId).isEmpty()) {
            return List.of();
        }
        List<ListingPhoto> removed = photos.findByListingIdOrderByIdAsc(listingId);
        photos.deleteAll(removed);
        return removed;
    }

    /** Deletes the listing with its photos and offers. Returns its photos so their backups can be removed. */
    @Transactional
    public Optional<List<ListingPhoto>> delete(Long listingId, Long sellerId) {
        return listings.findByIdAndSellerId(listingId, sellerId).map(listing -> {
            List<ListingPhoto> removed = photos.findByListingIdOrderByIdAsc(listingId);
            listings.delete(listing);
            return removed;
        });
    }

    private static String pattern(String raw) {
        String text = raw == null ? "" : raw.strip().toLowerCase().replaceAll("[%_\\\\]", "");
        return "%" + text + "%";
    }
}
