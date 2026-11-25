package com.shut.api.admin;

import com.shut.api.BadRequestException;
import com.shut.api.PageResponse;
import com.shut.api.Paging;
import com.shut.api.venue.LabelResponse;
import com.shut.user.entity.User;
import com.shut.user.service.UserService;
import com.shut.venue.Venue;
import com.shut.venue.VenueBookingService;
import com.shut.venue.VenueNotFoundException;
import com.shut.venue.VenueRepository;
import com.shut.venue.VenueService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Every venue, listed or not, and switching one off (or on again) whoever owns it. */
@Service
public class AdminVenueApiService {

    private final VenueRepository repository;
    private final VenueService venues;
    private final VenueBookingService bookings;
    private final UserService users;

    public AdminVenueApiService(VenueRepository repository, VenueService venues, VenueBookingService bookings,
                                UserService users) {
        this.repository = repository;
        this.venues = venues;
        this.bookings = bookings;
        this.users = users;
    }

    public PageResponse<AdminVenueItem> list(int page, int size) {
        PageRequest paging = PageRequest.of(Paging.page(page), Paging.size(size), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(repository.findAll(paging), this::item);
    }

    /** @throws VenueNotFoundException if there is no such venue */
    public AdminVenueItem setActive(Long venueId, Boolean active) {
        if (active == null) {
            throw new BadRequestException("`active` must be true or false");
        }
        Venue venue = venues.setActive(venueId, active).orElseThrow(() -> new VenueNotFoundException(venueId));
        return item(venue);
    }

    private AdminVenueItem item(Venue venue) {
        int prices = venues.prices(venue.getId()).size();
        User owner = users.findById(venue.getOwnerId());
        String ownerName = ((owner.getFirstName() == null ? "" : owner.getFirstName()) + " "
                + (owner.getLastName() == null ? "" : owner.getLastName())).trim();
        return new AdminVenueItem(venue.getId(), venue.getName(),
                venue.getType() == null ? null : new LabelResponse(venue.getType().name(), venue.getType().label()),
                venue.getCity(), owner.getId(), ownerName.isEmpty() ? owner.getPhoneNumber() : ownerName,
                venue.isActive(), venue.isListable() && prices > 0, prices, venues.photoCount(venue.getId()),
                bookings.pendingCount(venue.getId()));
    }
}
