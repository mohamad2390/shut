package com.shut.api.me.venue;

import com.shut.api.BadRequestException;
import com.shut.api.ConflictException;
import com.shut.api.NotFoundException;
import com.shut.api.venue.CalendarDayResponse;
import com.shut.api.venue.LabelResponse;
import com.shut.calendar.CalendarService;
import com.shut.calendar.CalendarStatus;
import com.shut.common.text.PersianDigits;
import com.shut.pricing.PricingService;
import com.shut.telegram.messaging.BotLinks;
import com.shut.telegram.venue.VenueNotifier;
import com.shut.user.entity.User;
import com.shut.user.service.UserService;
import com.shut.venue.Venue;
import com.shut.venue.VenueBooking;
import com.shut.venue.VenueBookingService;
import com.shut.venue.VenueCalendarService;
import com.shut.venue.VenueNotFoundException;
import com.shut.venue.VenuePrice;
import com.shut.venue.VenuePriceUnit;
import com.shut.venue.VenueService;
import com.shut.venue.VenueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A venue owner's own venues for the web app: details, prices, calendar and the requests waiting for an answer, under
 * the same rules as the bot. Photos can only be added in the bot, and so can deleting a venue.
 */
@Service
public class MyVenueApiService {

    /** The longest stretch of calendar one request can ask for. */
    public static final int MAX_CALENDAR_DAYS = 93;
    private static final int DEFAULT_CALENDAR_DAYS = 31;
    private static final Pattern PHONE_PATTERN = Pattern.compile("[+0-9][0-9 \\-]{3,28}");
    private static final Logger log = LoggerFactory.getLogger(MyVenueApiService.class);

    private final VenueService venues;
    private final VenueCalendarService venueCalendar;
    private final VenueBookingService bookings;
    private final CalendarService dates;
    private final UserService users;
    private final ObjectProvider<VenueNotifier> notifier;
    private final ObjectProvider<BotLinks> botLinks;

    public MyVenueApiService(VenueService venues, VenueCalendarService venueCalendar, VenueBookingService bookings,
                             CalendarService dates, UserService users, ObjectProvider<VenueNotifier> notifier,
                             ObjectProvider<BotLinks> botLinks) {
        this.venues = venues;
        this.venueCalendar = venueCalendar;
        this.bookings = bookings;
        this.dates = dates;
        this.users = users;
        this.notifier = notifier;
        this.botLinks = botLinks;
    }

    public List<MyVenueSummary> list(Long ownerId) {
        return venues.ownedBy(ownerId).stream().map(venue -> {
            List<VenuePrice> prices = venues.prices(venue.getId());
            return new MyVenueSummary(venue.getId(), venue.getName(), type(venue), venue.getCity(), venue.isActive(),
                    isListed(venue, prices), bookings.pendingCount(venue.getId()), prices.size(),
                    venues.photoCount(venue.getId()));
        }).toList();
    }

    /** @throws ConflictException if the member already has the maximum number of venues */
    public MyVenueDetails create(Long ownerId, CreateVenueRequest request) {
        String name = required(request.name(), 100, "The name");
        Venue venue = venues.create(ownerId, name)
                .orElseThrow(() -> new ConflictException("VENUE_LIMIT_REACHED",
                        "You can have at most " + VenueService.MAX_VENUES_PER_OWNER + " venues"));
        return details(venue);
    }

    /** @throws VenueNotFoundException if there is no such venue of the member's */
    public MyVenueDetails details(Long ownerId, Long venueId) {
        return details(owned(ownerId, venueId));
    }

    /**
     * Saves the fields that are sent (an empty text clears a field; a missing one is left alone).
     *
     * @throws BadRequestException if a value is not valid; then nothing is changed
     */
    public MyVenueDetails update(Long ownerId, Long venueId, UpdateVenueRequest request) {
        owned(ownerId, venueId);
        String name = request.name() == null ? null : required(request.name(), 100, "The name");
        VenueType type = request.type() == null || request.type().isBlank() ? null : VenueType.fromKey(request.type())
                .orElseThrow(() -> new BadRequestException("Unknown venue type"));
        String city = optional(request.city(), 100, "The city");
        String address = optional(request.address(), 300, "The address");
        String description = optional(request.description(), 1000, "The description");
        String amenities = optional(request.amenities(), 500, "The amenities");
        String phone = request.phone() == null ? null : phone(request.phone());
        Venue venue = venues.update(venueId, ownerId, found -> {
            if (name != null) found.setName(name);
            if (request.type() != null) found.setType(type);
            if (request.city() != null) found.setCity(city);
            if (request.address() != null) found.setAddress(address);
            if (request.description() != null) found.setDescription(description);
            if (request.amenities() != null) found.setAmenities(amenities);
            if (request.phone() != null) found.setPhone(phone);
            if (request.active() != null) found.setActive(request.active());
        }).orElseThrow(() -> new VenueNotFoundException(venueId));
        return details(venue);
    }

    /**
     * @throws BadRequestException if the unit, amount or name is not valid
     * @throws ConflictException   if the venue already has the maximum number of prices
     */
    public MyVenueDetails addPrice(Long ownerId, Long venueId, VenuePriceRequest request) {
        Venue venue = owned(ownerId, venueId);
        VenuePriceUnit unit = request.unit() == null ? null : VenuePriceUnit.fromKey(request.unit()).orElse(null);
        if (unit == null) {
            throw new BadRequestException("Unknown price unit");
        }
        Long amount = request.amount();
        if (amount == null || amount <= 0 || amount > PricingService.MAX_AMOUNT) {
            throw new BadRequestException("The amount must be between 1 and " + PricingService.MAX_AMOUNT + " tomans");
        }
        String title = optional(request.title(), 100, "The name");
        venues.addPrice(venueId, ownerId, unit, title, amount)
                .orElseThrow(() -> new ConflictException("VENUE_PRICE_LIMIT_REACHED",
                        "A venue can have at most " + VenueService.MAX_PRICES + " prices"));
        return details(venue);
    }

    /** @throws NotFoundException if the price is not one of this venue's */
    public void removePrice(Long ownerId, Long venueId, Long priceId) {
        owned(ownerId, venueId);
        boolean ofThisVenue = venues.prices(venueId).stream().anyMatch(price -> price.getId().equals(priceId));
        if (!ofThisVenue) {
            throw new NotFoundException("Price not found: " + priceId);
        }
        venues.removePrice(priceId, ownerId);
    }

    /**
     * The days the owner marked, in date order. Without {@code from} and {@code to} it is today and 30 days on.
     *
     * @throws BadRequestException if {@code to} is before {@code from} or the range is over 93 days
     */
    public List<CalendarDayResponse> calendar(Long ownerId, Long venueId, LocalDate from, LocalDate to) {
        owned(ownerId, venueId);
        LocalDate start = from == null ? dates.today() : from;
        LocalDate end = to == null ? start.plusDays(DEFAULT_CALENDAR_DAYS - 1) : to;
        if (end.isBefore(start)) {
            throw new BadRequestException("`to` must not be before `from`");
        }
        if (start.plusDays(MAX_CALENDAR_DAYS).isBefore(end)) {
            throw new BadRequestException("The range can be at most " + MAX_CALENDAR_DAYS + " days");
        }
        return venueCalendar.statuses(venueId, start, end).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new CalendarDayResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * @param status null clears the day
     * @throws BadRequestException if the day is in the past or more than a year ahead
     */
    public void mark(Long ownerId, Long venueId, LocalDate day, CalendarStatus status) {
        owned(ownerId, venueId);
        try {
            venueCalendar.setStatus(venueId, ownerId, day, status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("That day cannot be marked: it is in the past or too far ahead");
        }
    }

    /** The requests of the venue that wait for an answer, earliest day first. */
    public List<OwnerRequestResponse> requests(Long ownerId, Long venueId) {
        owned(ownerId, venueId);
        return bookings.pending(venueId, ownerId).stream().map(this::requestResponse).toList();
    }

    /**
     * Accepts or declines a waiting request and tells the member in Telegram if they use it.
     *
     * @throws NotFoundException if the request is not for this venue
     * @throws ConflictException if it was answered already
     */
    public void answer(Long ownerId, Long venueId, Long bookingId, Boolean accept) {
        if (accept == null) {
            throw new BadRequestException("`accept` must be true or false");
        }
        Venue venue = owned(ownerId, venueId);
        bookings.find(bookingId).filter(booking -> booking.getVenueId().equals(venueId))
                .orElseThrow(() -> new NotFoundException("Request not found: " + bookingId));
        VenueBooking answered = bookings.respond(bookingId, ownerId, accept)
                .orElseThrow(() -> new ConflictException("REQUEST_ALREADY_ANSWERED", "This request was answered already"));
        VenueNotifier telegram = notifier.getIfAvailable();
        if (telegram != null) {
            try {
                telegram.answered(venue, answered);
            } catch (RuntimeException e) {
                log.warn("Could not tell the requester of {} about the answer: {}", bookingId, e.getMessage());
            }
        }
    }

    private Venue owned(Long ownerId, Long venueId) {
        return venues.findOwned(venueId, ownerId).orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    private MyVenueDetails details(Venue venue) {
        List<VenuePrice> prices = venues.prices(venue.getId());
        List<String> missing = new ArrayList<>();
        if (venue.getCity() == null) {
            missing.add("CITY");
        }
        if (prices.isEmpty()) {
            missing.add("PRICE");
        }
        BotLinks links = botLinks.getIfAvailable();
        return new MyVenueDetails(venue.getId(), venue.getName(), type(venue), venue.getCity(), venue.getAddress(),
                venue.getDescription(), venue.getAmenities(), venue.getPhone(), venue.isActive(),
                isListed(venue, prices), missing,
                prices.stream().map(price -> new MyVenueDetails.Price(price.getId(), price.getTitle(),
                        new LabelResponse(price.getUnit().name(), price.getUnit().label()), price.getAmount())).toList(),
                venues.photoCount(venue.getId()), links == null ? null : links.botUrl().orElse(null));
    }

    private OwnerRequestResponse requestResponse(VenueBooking booking) {
        User user = users.findById(booking.getRequesterId());
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return new OwnerRequestResponse(booking.getId(), booking.getDay(), booking.getStartTime(), booking.getEndTime(),
                booking.isFullDay(), booking.getNote(),
                new RequesterResponse(name.isEmpty() ? null : name, user.getTelegramUsername(), user.getPhoneNumber()));
    }

    private static boolean isListed(Venue venue, List<VenuePrice> prices) {
        return venue.isListable() && !prices.isEmpty();
    }

    private static LabelResponse type(Venue venue) {
        return venue.getType() == null ? null : new LabelResponse(venue.getType().name(), venue.getType().label());
    }

    private static String clean(String raw) {
        return raw.strip().replaceAll("\\s+", " ");
    }

    private static String required(String raw, int max, String what) {
        String value = raw == null ? "" : clean(raw);
        if (value.isEmpty()) {
            throw new BadRequestException(what + " is required");
        }
        if (value.length() > max) {
            throw new BadRequestException(what + " can be at most " + max + " characters");
        }
        return value;
    }

    /** Null stays null (not sent); an empty text becomes null (clear). */
    private static String optional(String raw, int max, String what) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = clean(raw);
        if (value.length() > max) {
            throw new BadRequestException(what + " can be at most " + max + " characters");
        }
        return value;
    }

    private static String phone(String raw) {
        if (raw.isBlank()) {
            return null;
        }
        String value = PersianDigits.toAscii(raw.strip());
        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new BadRequestException("The phone number is not valid");
        }
        return value;
    }
}
