package com.shut.api.venue;

import com.shut.api.BadRequestException;
import com.shut.api.PageResponse;
import com.shut.calendar.CalendarService;
import com.shut.user.service.UserService;
import com.shut.venue.Venue;
import com.shut.venue.VenueCalendarService;
import com.shut.venue.VenueNotFoundException;
import com.shut.venue.VenuePrice;
import com.shut.venue.VenueService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The venues for the web app. It calls the same services the bot uses, so a venue is listed, priced and booked on the
 * same rules in both: only active venues with a city and a price are visible.
 */
@Service
public class VenueApiService {

    /** The longest stretch of calendar one request can ask for. */
    public static final int MAX_CALENDAR_DAYS = 93;
    private static final int DEFAULT_CALENDAR_DAYS = 31;

    private final VenueService venues;
    private final VenueCalendarService calendar;
    private final CalendarService dates;
    private final UserService users;

    public VenueApiService(VenueService venues, VenueCalendarService calendar, CalendarService dates,
                           UserService users) {
        this.venues = venues;
        this.calendar = calendar;
        this.dates = dates;
        this.users = users;
    }

    /** The visible venues, newest first. */
    public PageResponse<VenueListItem> list(int page, int size) {
        var found = venues.listed(page, size);
        Map<Long, List<VenuePrice>> prices = venues.pricesOf(found.stream().map(Venue::getId).toList());
        return PageResponse.of(found, venue -> new VenueListItem(venue.getId(), venue.getName(), type(venue),
                venue.getCity(), priceResponses(prices.getOrDefault(venue.getId(), List.of())),
                venues.photoCount(venue.getId())));
    }

    /** @throws VenueNotFoundException if the venue does not exist or is not visible to other members */
    public VenueDetailsResponse details(Long venueId) {
        Venue venue = visible(venueId);
        return new VenueDetailsResponse(venue.getId(), venue.getName(), type(venue), venue.getCity(),
                venue.getAddress(), venue.getDescription(), venue.getAmenities(), venue.getPhone(),
                users.findById(venue.getOwnerId()).getTelegramUsername(),
                priceResponses(venues.prices(venueId)),
                venues.photos(venueId).stream().map(photo -> new VenuePhotoResponse(photo.getId())).toList());
    }

    /**
     * The days the owner marked between {@code from} and {@code to}, inclusive, in date order. Without them it is
     * today and the 30 days after.
     *
     * @throws VenueNotFoundException if the venue is not visible
     * @throws BadRequestException    if {@code to} is before {@code from} or the range is longer than 93 days
     */
    public List<CalendarDayResponse> calendar(Long venueId, LocalDate from, LocalDate to) {
        visible(venueId);
        LocalDate start = from == null ? dates.today() : from;
        LocalDate end = to == null ? start.plusDays(DEFAULT_CALENDAR_DAYS - 1) : to;
        if (end.isBefore(start)) {
            throw new BadRequestException("`to` must not be before `from`");
        }
        if (start.plusDays(MAX_CALENDAR_DAYS).isBefore(end)) {
            throw new BadRequestException("The range can be at most " + MAX_CALENDAR_DAYS + " days");
        }
        return calendar.statuses(venueId, start, end).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new CalendarDayResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Venue visible(Long venueId) {
        return venues.findListed(venueId).orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    private static LabelResponse type(Venue venue) {
        return venue.getType() == null ? null : new LabelResponse(venue.getType().name(), venue.getType().label());
    }

    private static List<VenuePriceResponse> priceResponses(List<VenuePrice> prices) {
        return prices.stream()
                .map(price -> new VenuePriceResponse(price.getTitle(),
                        new LabelResponse(price.getUnit().name(), price.getUnit().label()), price.getAmount()))
                .toList();
    }
}
