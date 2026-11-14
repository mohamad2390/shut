package com.shut.venue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenueBookingRepository extends JpaRepository<VenueBooking, Long> {

    List<VenueBooking> findByVenueIdAndStatusOrderByDayAscIdAsc(Long venueId, BookingStatus status);

    List<VenueBooking> findTop10ByRequesterIdOrderByIdDesc(Long requesterId);

    boolean existsByVenueIdAndRequesterIdAndDayAndStatus(Long venueId, Long requesterId, java.time.LocalDate day,
                                                         BookingStatus status);

    long countByVenueIdAndStatus(Long venueId, BookingStatus status);
}
