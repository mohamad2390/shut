package com.shut.api.venue;

import com.shut.api.BadRequestException;
import com.shut.telegram.venue.VenueNotifier;
import com.shut.venue.BookingStatus;
import com.shut.venue.Venue;
import com.shut.venue.VenueBooking;
import com.shut.venue.VenueBookingService;
import com.shut.venue.VenueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/** Booking requests from the web, under the same rules as the bot; the owner is told in Telegram when they use it. */
@Service
public class VenueBookingApiService {

    static final int MAX_NOTE_LENGTH = 500;
    private static final Logger log = LoggerFactory.getLogger(VenueBookingApiService.class);

    private final VenueBookingService bookings;
    private final VenueService venues;
    private final ObjectProvider<VenueNotifier> notifier;

    public VenueBookingApiService(VenueBookingService bookings, VenueService venues,
                                  ObjectProvider<VenueNotifier> notifier) {
        this.bookings = bookings;
        this.venues = venues;
        this.notifier = notifier;
    }

    /**
     * @throws com.shut.venue.VenueBookingException if the venue is not available, is the member's own, the day is not
     *                                              free, the hours are wrong, or the same request is waiting already
     * @throws BadRequestException                  if the day is missing or the note is too long
     */
    public BookingResponse request(Long requesterId, Long venueId, BookingRequest request) {
        if (request.date() == null) {
            throw new BadRequestException("A date is required");
        }
        String note = request.note() == null ? "" : request.note().strip();
        if (note.length() > MAX_NOTE_LENGTH) {
            throw new BadRequestException("The note can be at most " + MAX_NOTE_LENGTH + " characters");
        }
        VenueBooking booking = bookings.request(venueId, requesterId, request.date(), request.startTime(),
                request.endTime(), note.isEmpty() ? null : note);
        Venue venue = venues.find(venueId).orElseThrow();
        tellOwner(venue, booking);
        return response(booking, venue.getName());
    }

    /** The member's latest requests, newest first. */
    public List<BookingResponse> mine(Long requesterId) {
        return bookings.latestOf(requesterId).stream()
                .map(booking -> response(booking,
                        venues.find(booking.getVenueId()).map(Venue::getName).orElse("")))
                .toList();
    }

    private void tellOwner(Venue venue, VenueBooking booking) {
        VenueNotifier telegram = notifier.getIfAvailable();
        if (telegram == null) {
            return;
        }
        try {
            telegram.requestArrived(venue, booking);
        } catch (RuntimeException e) {
            // The request is saved; the owner will still see it in their list.
            log.warn("Could not tell the owner of venue {} about request {}: {}", venue.getId(), booking.getId(),
                    e.getMessage());
        }
    }

    static BookingResponse response(VenueBooking booking, String venueName) {
        BookingStatus status = booking.getStatus();
        return new BookingResponse(booking.getId(), booking.getVenueId(), venueName, booking.getDay(),
                booking.getStartTime(), booking.getEndTime(), booking.isFullDay(), booking.getNote(),
                new LabelResponse(status.name(), status.label()));
    }
}
