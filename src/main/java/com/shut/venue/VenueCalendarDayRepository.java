package com.shut.venue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VenueCalendarDayRepository extends JpaRepository<VenueCalendarDay, Long> {

    Optional<VenueCalendarDay> findByVenueIdAndDay(Long venueId, LocalDate day);

    List<VenueCalendarDay> findByVenueIdAndDayBetween(Long venueId, LocalDate from, LocalDate to);
}
