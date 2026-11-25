package com.shut.venue;

import com.shut.calendar.CalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime TWELVE = LocalTime.of(12, 0);

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

    private static VenueBooking booking(LocalTime start, LocalTime end) {
        return new VenueBooking(1L, 2L, DAY, start, end, null);
    }

    private void openDay() {
        when(venues.findById(1L)).thenReturn(Optional.of(listedVenue(9L)));
        when(prices.existsByVenueId(any())).thenReturn(true);
        when(dates.today()).thenReturn(TODAY);
        when(calendar.isFree(1L, DAY)).thenReturn(true);
    }

    private void expectRefusal(VenueBookingException.Reason reason, LocalTime start, LocalTime end) {
        assertThatThrownBy(() -> service.request(1L, 2L, DAY, start, end, null))
                .isInstanceOfSatisfying(VenueBookingException.class, e -> assertThat(e.reason()).isEqualTo(reason));
        verify(bookings, never()).save(any());
    }

    @Test
    void someHoursOfAFreeDayCanBeRequested() {
        openDay();
        when(bookings.save(any(VenueBooking.class))).thenAnswer(call -> call.getArgument(0));

        VenueBooking made = service.request(1L, 2L, DAY, NINE, TWELVE, "عروسی");

        assertThat(made.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(made.getStartTime()).isEqualTo(NINE);
        assertThat(made.getEndTime()).isEqualTo(TWELVE);
        assertThat(made.getNote()).isEqualTo("عروسی");
    }

    @Test
    void theWholeDayCanBeRequestedWithoutHours() {
        openDay();
        when(bookings.save(any(VenueBooking.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.request(1L, 2L, DAY, null, null, null).isFullDay()).isTrue();
    }

    @Test
    void severalMembersCanAskForTheSameHours() {
        openDay();
        when(bookings.save(any(VenueBooking.class))).thenAnswer(call -> call.getArgument(0));

        VenueBooking first = service.request(1L, 2L, DAY, NINE, TWELVE, null);
        VenueBooking second = service.request(1L, 3L, DAY, NINE, TWELVE, null);

        assertThat(first.getRequesterId()).isEqualTo(2L);
        assertThat(second.getRequesterId()).isEqualTo(3L);
        assertThat(second.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void anEndBeforeTheStartIsRefused() {
        when(venues.findById(1L)).thenReturn(Optional.of(listedVenue(9L)));
        when(prices.existsByVenueId(any())).thenReturn(true);

        expectRefusal(VenueBookingException.Reason.INVALID_TIME, TWELVE, NINE);
    }

    @Test
    void anUnlistedVenueCannotBeRequested() {
        when(venues.findById(1L)).thenReturn(Optional.of(new Venue(9L, "ناقص")));

        expectRefusal(VenueBookingException.Reason.VENUE_NOT_AVAILABLE, null, null);
    }

    @Test
    void aVenueWithoutAPriceCannotBeRequested() {
        when(venues.findById(1L)).thenReturn(Optional.of(listedVenue(9L)));
        when(prices.existsByVenueId(any())).thenReturn(false);

        expectRefusal(VenueBookingException.Reason.VENUE_NOT_AVAILABLE, null, null);
    }

    @Test
    void ownersCannotBookTheirOwnVenue() {
        when(venues.findById(1L)).thenReturn(Optional.of(listedVenue(2L)));
        when(prices.existsByVenueId(any())).thenReturn(true);

        expectRefusal(VenueBookingException.Reason.OWN_VENUE, null, null);
    }

    @Test
    void aDayThatIsNotFreeIsRefused() {
        when(venues.findById(1L)).thenReturn(Optional.of(listedVenue(9L)));
        when(prices.existsByVenueId(any())).thenReturn(true);
        when(dates.today()).thenReturn(TODAY);
        when(calendar.isFree(1L, DAY)).thenReturn(false);

        expectRefusal(VenueBookingException.Reason.DAY_NOT_FREE, NINE, TWELVE);
    }

    @Test
    void askingTwiceForTheSameHoursIsRefusedButOtherHoursAreFine() {
        openDay();
        when(bookings.findByVenueIdAndRequesterIdAndDayAndStatus(1L, 2L, DAY, BookingStatus.PENDING))
                .thenReturn(List.of(booking(NINE, TWELVE)));
        when(bookings.save(any(VenueBooking.class))).thenAnswer(call -> call.getArgument(0));

        assertThatThrownBy(() -> service.request(1L, 2L, DAY, NINE, TWELVE, null))
                .isInstanceOfSatisfying(VenueBookingException.class,
                        e -> assertThat(e.reason()).isEqualTo(VenueBookingException.Reason.ALREADY_REQUESTED));
        assertThat(service.request(1L, 2L, DAY, LocalTime.of(14, 0), LocalTime.of(16, 0), null)).isNotNull();
    }

    @Test
    void acceptingNeverChangesTheCalendarOrBlocksOverlappingBookings() {
        VenueBooking whole = booking(null, null);
        when(bookings.findById(5L)).thenReturn(Optional.of(whole));
        when(venues.findByIdAndOwnerId(1L, 9L)).thenReturn(Optional.of(listedVenue(9L)));

        assertThat(service.respond(5L, 9L, true)).isPresent();

        assertThat(whole.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
        verify(calendar, never()).cycle(any(), any(), any());
    }

    @Test
    void decliningIsRecorded() {
        VenueBooking part = booking(NINE, TWELVE);
        when(bookings.findById(5L)).thenReturn(Optional.of(part));
        when(venues.findByIdAndOwnerId(1L, 9L)).thenReturn(Optional.of(listedVenue(9L)));

        service.respond(5L, 9L, false);

        assertThat(part.getStatus()).isEqualTo(BookingStatus.DECLINED);
    }

    @Test
    void onlyTheOwnerCanAnswer() {
        VenueBooking whole = booking(null, null);
        when(bookings.findById(5L)).thenReturn(Optional.of(whole));
        when(venues.findByIdAndOwnerId(1L, 3L)).thenReturn(Optional.empty());

        assertThat(service.respond(5L, 3L, true)).isEmpty();
        assertThat(whole.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void aRequestAnsweredBeforeCannotBeAnsweredAgain() {
        VenueBooking whole = booking(null, null);
        whole.setStatus(BookingStatus.DECLINED);
        when(bookings.findById(5L)).thenReturn(Optional.of(whole));

        assertThat(service.respond(5L, 9L, true)).isEmpty();
    }
}
