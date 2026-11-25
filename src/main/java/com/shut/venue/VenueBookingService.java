package com.shut.venue;

import com.shut.calendar.CalendarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Booking requests for venues: a member asks for a free day, the owner accepts or declines. */
@Service
public class VenueBookingService {

    private final VenueBookingRepository bookings;
    private final VenueRepository venues;
    private final VenuePriceRepository prices;
    private final VenueCalendarService calendar;
    private final CalendarService dates;

    public VenueBookingService(VenueBookingRepository bookings, VenueRepository venues,
                               VenuePriceRepository prices, VenueCalendarService calendar, CalendarService dates) {
        this.bookings = bookings;
        this.venues = venues;
        this.prices = prices;
        this.calendar = calendar;
        this.dates = dates;
    }

    /**
     * Asks for the venue on a day the owner marked free, for the whole day (no times) or for hours of it.
     *
     * Other bookings of the same hours never get in the way: a venue can host several shoots at once.
     *
     * @throws VenueBookingException if the venue is not listed, is the member's own, the day is not free, the hours
     *                               are not valid, or the member already asked for exactly this and is waiting for an
     *                               answer
     */
    @Transactional
    public VenueBooking request(Long venueId, Long requesterId, LocalDate day, LocalTime start, LocalTime end,
                                String note) {
        Venue venue = venues.findById(venueId)
                .filter(found -> found.isListable() && prices.existsByVenueId(found.getId()))
                .orElseThrow(() -> new VenueBookingException(VenueBookingException.Reason.VENUE_NOT_AVAILABLE));
        if (venue.getOwnerId().equals(requesterId)) {
            throw new VenueBookingException(VenueBookingException.Reason.OWN_VENUE);
        }
        if ((start == null) != (end == null) || (start != null && !start.isBefore(end))) {
            throw new VenueBookingException(VenueBookingException.Reason.INVALID_TIME);
        }
        if (day.isBefore(dates.today()) || !calendar.isFree(venueId, day)) {
            throw new VenueBookingException(VenueBookingException.Reason.DAY_NOT_FREE);
        }
        boolean duplicate = bookings.findByVenueIdAndRequesterIdAndDayAndStatus(venueId, requesterId, day,
                BookingStatus.PENDING).stream().anyMatch(earlier -> earlier.sameSlotAs(day, start, end));
        if (duplicate) {
            throw new VenueBookingException(VenueBookingException.Reason.ALREADY_REQUESTED);
        }
        return bookings.save(new VenueBooking(venueId, requesterId, day, start, end, note));
    }

    /**
     * The owner's answer to a request that is still waiting. Accepting does not change the calendar: the owner decides
     * when a day is full. Empty if the request does not exist, is not for one of the owner's venues, or was answered
     * already.
     */
    @Transactional
    public Optional<VenueBooking> respond(Long bookingId, Long ownerId, boolean accept) {
        return bookings.findById(bookingId)
                .filter(booking -> booking.getStatus() == BookingStatus.PENDING)
                .filter(booking -> venues.findByIdAndOwnerId(booking.getVenueId(), ownerId).isPresent())
                .map(booking -> {
                    booking.setStatus(accept ? BookingStatus.ACCEPTED : BookingStatus.DECLINED);
                    return booking;
                });
    }

    @Transactional(readOnly = true)
    public Optional<VenueBooking> find(Long bookingId) {
        return bookings.findById(bookingId);
    }

    /** The requests of one of the owner's venues that are waiting for an answer, earliest day first. */
    @Transactional(readOnly = true)
    public List<VenueBooking> pending(Long venueId, Long ownerId) {
        if (venues.findByIdAndOwnerId(venueId, ownerId).isEmpty()) {
            return List.of();
        }
        return bookings.findByVenueIdAndStatusOrderByDayAscIdAsc(venueId, BookingStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long pendingCount(Long venueId) {
        return bookings.countByVenueIdAndStatus(venueId, BookingStatus.PENDING);
    }

    /** The member's latest requests, newest first. */
    @Transactional(readOnly = true)
    public List<VenueBooking> latestOf(Long requesterId) {
        return bookings.findTop10ByRequesterIdOrderByIdDesc(requesterId);
    }
}
