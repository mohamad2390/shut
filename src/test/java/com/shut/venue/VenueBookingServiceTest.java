package com.shut.venue;

import com.shut.calendar.CalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueBookingServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);
    private static final LocalDate DAY = TODAY.plusDays(5);

    @Mock VenueBookingRepository bookings;
    @Mock VenueRepository venues;
    @Mock VenuePriceRepository prices;
    @Mock VenueCalendarService calendar;
    @Mock CalendarService dates;

    VenueBookingService service;

    @BeforeEach
    void setUp() {
        service = new VenueBookingService(bookings, venues, prices, calendar, dates);
    }

    private static Venue listedVenue(long ownerId) {
        Venue venue = new Venue(ownerId, "باغ");
        venue.setCity("کرج");
        return venue;
    }

    private void expectRefusal(VenueBookingException.Reason reason) {
        assertThatThrownBy(() -> service.request(1L, 2L, DAY, null))
                .isInstanceOfSatisfying(VenueBookingException.class, e -> assertThat(e.reason()).isEqualTo(reason));
        verify(bookings, never()).save(any());
    }

    @Test
    void aFreeDayCanBeRequested() {
        when(venues.findById(1L)).thenReturn(Optional.of(listedVenue(9L)));
        when(prices.existsByVenueId(any())).thenReturn(true);
        when(dates.today()).thenReturn(TODAY);
        when(calendar.isFree(1L, DAY)).thenReturn(true);
        when(bookings.existsByVenueIdAndRequesterIdAndDayAndStatus(1L, 2L, DAY, BookingStatus.PENDING)).thenReturn(false);
        when(bookings.save(any(VenueBooking.class))).thenAnswer(call -> call.getArgument(0));

        VenueBooking booking = service.request(1L, 2L, DAY, "عروسی");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getDay()).isEqualTo(DAY);
        assertThat(booking.getNote()).isEqualTo("عروسی");
    }

    @Test
    void anUnlistedVenueCannotBeRequested() {
        when(venues.findById(1L)).thenReturn(Optional.of(new Venue(9L, "ناقص")));

        expectRefusal(VenueBookingException.Reason.VENUE_NOT_AVAILABLE);
    }

    @Test
    void aVenueWithoutAPriceCannotBeRequested() {
        when(venues.findById(1L)).thenReturn(Optional.of(listedVenue(9L)));
        when(prices.existsByVenueId(any())).thenReturn(false);

        expectRefusal(VenueBookingException.Reason.VENUE_NOT_AVAILABLE);
    }

    @Test
    void ownersCannotBookTheirOwnVenue() {
        when(venues.findById(1L)).thenReturn(Optional.of(listedVenue(2L)));
        when(prices.existsByVenueId(any())).thenReturn(true);

        expectRefusal(VenueBookingException.Reason.OWN_VENUE);
    }

    @Test
    void aDayThatIsNotFreeIsRefused() {
        when(venues.findById(1L)).thenReturn(Optional.of(listedVenue(9L)));
        when(prices.existsByVenueId(any())).thenReturn(true);
        when(dates.today()).thenReturn(TODAY);
        when(calendar.isFree(1L, DAY)).thenReturn(false);

        expectRefusal(VenueBookingException.Reason.DAY_NOT_FREE);
    }

    @Test
    void askingTwiceForTheSameDayIsRefused() {
        when(venues.findById(1L)).thenReturn(Optional.of(listedVenue(9L)));
        when(prices.existsByVenueId(any())).thenReturn(true);
        when(dates.today()).thenReturn(TODAY);
        when(calendar.isFree(1L, DAY)).thenReturn(true);
        when(bookings.existsByVenueIdAndRequesterIdAndDayAndStatus(1L, 2L, DAY, BookingStatus.PENDING)).thenReturn(true);

        expectRefusal(VenueBookingException.Reason.ALREADY_REQUESTED);
    }

    @Test
    void acceptingMarksTheDayTaken() {
        VenueBooking booking = new VenueBooking(1L, 2L, DAY, null);
        when(bookings.findById(5L)).thenReturn(Optional.of(booking));
        when(venues.findByIdAndOwnerId(1L, 9L)).thenReturn(Optional.of(listedVenue(9L)));

        Optional<VenueBooking> answered = service.respond(5L, 9L, true);

        assertThat(answered).isPresent();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
        verify(calendar).markBusy(1L, DAY);
    }

    @Test
    void decliningLeavesTheCalendarAlone() {
        VenueBooking booking = new VenueBooking(1L, 2L, DAY, null);
        when(bookings.findById(5L)).thenReturn(Optional.of(booking));
        when(venues.findByIdAndOwnerId(1L, 9L)).thenReturn(Optional.of(listedVenue(9L)));

        service.respond(5L, 9L, false);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.DECLINED);
        verify(calendar, never()).markBusy(any(), any());
    }

    @Test
    void onlyTheOwnerCanAnswer() {
        VenueBooking booking = new VenueBooking(1L, 2L, DAY, null);
        when(bookings.findById(5L)).thenReturn(Optional.of(booking));
        when(venues.findByIdAndOwnerId(1L, 3L)).thenReturn(Optional.empty());

        assertThat(service.respond(5L, 3L, true)).isEmpty();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void aRequestAnsweredBeforeCannotBeAnsweredAgain() {
        VenueBooking booking = new VenueBooking(1L, 2L, DAY, null);
        booking.setStatus(BookingStatus.DECLINED);
        when(bookings.findById(5L)).thenReturn(Optional.of(booking));

        assertThat(service.respond(5L, 9L, true)).isEmpty();
    }
}
