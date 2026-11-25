package com.shut.venue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Venues, their owners and their photos. Every change checks that the venue belongs to the member making it. */
@Service
public class VenueService {

    public static final int MAX_VENUES_PER_OWNER = 5;
    public static final int MAX_PHOTOS = 10;
    public static final int MAX_PRICES = 8;

    /** What happened when a photo was added. */
    public enum PhotoResult { ADDED, DUPLICATE, LIMIT_REACHED, NOT_FOUND }

    private final VenueRepository venues;
    private final VenuePhotoRepository photos;
    private final VenuePriceRepository prices;

    public VenueService(VenueRepository venues, VenuePhotoRepository photos, VenuePriceRepository prices) {
        this.venues = venues;
        this.photos = photos;
        this.prices = prices;
    }

    /** Starts a venue with only a name; the owner fills in the rest. Empty if they already have the maximum. */
    @Transactional
    public Optional<Venue> create(Long ownerId, String name) {
        if (venues.countByOwnerId(ownerId) >= MAX_VENUES_PER_OWNER) {
            return Optional.empty();
        }
        return Optional.of(venues.save(new Venue(ownerId, name)));
    }

    @Transactional(readOnly = true)
    public Optional<Venue> find(Long venueId) {
        return venues.findById(venueId);
    }

    /** The venue if other members can see it: active, with a city and at least one price. */
    @Transactional(readOnly = true)
    public Optional<Venue> findListed(Long venueId) {
        return venues.findById(venueId).filter(venue -> venue.isListable() && prices.existsByVenueId(venue.getId()));
    }

    @Transactional(readOnly = true)
    public Optional<Venue> findOwned(Long venueId, Long ownerId) {
        return venues.findByIdAndOwnerId(venueId, ownerId);
    }

    @Transactional(readOnly = true)
    public List<Venue> ownedBy(Long ownerId) {
        return venues.findByOwnerIdOrderByIdAsc(ownerId);
    }

    /** The venues other members can see, newest first. {@code page} starts at 0. */
    @Transactional(readOnly = true)
    public Page<Venue> listed(int page, int size) {
        return venues.listed(PageRequest.of(page, size));
    }

    /** Changes the venue if it is the owner's. Empty if there is no such venue of theirs. */
    @Transactional
    public Optional<Venue> update(Long venueId, Long ownerId, Consumer<Venue> change) {
        return venues.findByIdAndOwnerId(venueId, ownerId).map(venue -> {
            change.accept(venue);
            return venue;
        });
    }

    /** Switches the venue on or off, whoever owns it; for administrators. Empty if there is no such venue. */
    @Transactional
    public Optional<Venue> setActive(Long venueId, boolean active) {
        return venues.findById(venueId).map(venue -> {
            venue.setActive(active);
            return venue;
        });
    }

    /** Deletes the venue with its photos, calendar and bookings. Returns its photos so their backups can be removed. */
    @Transactional
    public Optional<List<VenuePhoto>> delete(Long venueId, Long ownerId) {
        return venues.findByIdAndOwnerId(venueId, ownerId).map(venue -> {
            List<VenuePhoto> removed = photos.findByVenueIdOrderByIdAsc(venueId);
            venues.delete(venue);
            return removed;
        });
    }

    @Transactional(readOnly = true)
    public List<VenuePrice> prices(Long venueId) {
        return prices.findByVenueIdOrderByIdAsc(venueId);
    }

    /** The prices of several venues at once, each list in the order the owner added them. */
    @Transactional(readOnly = true)
    public Map<Long, List<VenuePrice>> pricesOf(Collection<Long> venueIds) {
        if (venueIds.isEmpty()) {
            return Map.of();
        }
        return prices.findByVenueIdInOrderByIdAsc(venueIds).stream()
                .collect(Collectors.groupingBy(VenuePrice::getVenueId));
    }

    /** Adds a price to the owner's venue. Empty if it is not theirs or it already has the maximum. */
    @Transactional
    public Optional<VenuePrice> addPrice(Long venueId, Long ownerId, VenuePriceUnit unit, String title, long amount) {
        if (venues.findByIdAndOwnerId(venueId, ownerId).isEmpty() || prices.countByVenueId(venueId) >= MAX_PRICES) {
            return Optional.empty();
        }
        return Optional.of(prices.save(new VenuePrice(venueId, title, unit, amount)));
    }

    /** Removes a price of the owner's venue and returns the venue's id, or empty if there was no such price. */
    @Transactional
    public Optional<Long> removePrice(Long priceId, Long ownerId) {
        return prices.findById(priceId)
                .filter(price -> venues.findByIdAndOwnerId(price.getVenueId(), ownerId).isPresent())
                .map(price -> {
                    prices.delete(price);
                    return price.getVenueId();
                });
    }

    @Transactional(readOnly = true)
    public List<VenuePhoto> photos(Long venueId) {
        return photos.findByVenueIdOrderByIdAsc(venueId);
    }

    @Transactional(readOnly = true)
    public long photoCount(Long venueId) {
        return photos.countByVenueId(venueId);
    }

    @Transactional(readOnly = true)
    public PhotoResult checkCanAddPhoto(Long venueId, Long ownerId, String fileUniqueId) {
        if (venues.findByIdAndOwnerId(venueId, ownerId).isEmpty()) {
            return PhotoResult.NOT_FOUND;
        }
        if (photos.existsByVenueIdAndFileUniqueId(venueId, fileUniqueId)) {
            return PhotoResult.DUPLICATE;
        }
        return photos.countByVenueId(venueId) >= MAX_PHOTOS ? PhotoResult.LIMIT_REACHED : PhotoResult.ADDED;
    }

    @Transactional
    public PhotoResult addPhoto(Long venueId, Long ownerId, String fileId, String fileUniqueId, Long backupMessageId) {
        PhotoResult check = checkCanAddPhoto(venueId, ownerId, fileUniqueId);
        if (check == PhotoResult.ADDED) {
            photos.save(new VenuePhoto(venueId, fileId, fileUniqueId, backupMessageId));
        }
        return check;
    }

    /** Removes every photo of the owner's venue and returns them so their backups can be removed. */
    @Transactional
    public List<VenuePhoto> clearPhotos(Long venueId, Long ownerId) {
        if (venues.findByIdAndOwnerId(venueId, ownerId).isEmpty()) {
            return List.of();
        }
        List<VenuePhoto> removed = photos.findByVenueIdOrderByIdAsc(venueId);
        photos.deleteAll(removed);
        return removed;
    }
}
