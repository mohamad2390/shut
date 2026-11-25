package com.shut.venue;

import com.shut.calendar.CalendarService;
import com.shut.calendar.CalendarStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A venue's calendar: the days it is free and the days it is taken. The date rules are the specialists' calendar's. */
@Service
public class VenueCalendarService {

    private final VenueCalendarDayRepository days;
    private final VenueRepository venues;
    private final CalendarService dates;

    public VenueCalendarService(VenueCalendarDayRepository days, VenueRepository venues, CalendarService dates) {
        this.days = days;
        this.venues = venues;
        this.dates = dates;
    }

    /** The marked days between the two dates, inclusive. Days that are not set are absent. */
    @Transactional(readOnly = true)
    public Map<LocalDate, CalendarStatus> statuses(Long venueId, LocalDate from, LocalDate to) {
        Map<LocalDate, CalendarStatus> statuses = new HashMap<>();
        days.findByVenueIdAndDayBetween(venueId, from, to).forEach(day -> statuses.put(day.getDay(), day.getStatus()));
        return statuses;
    }

    @Transactional(readOnly = true)
    public boolean isFree(Long venueId, LocalDate day) {
        return days.findByVenueIdAndDay(venueId, day).map(marked -> marked.getStatus() == CalendarStatus.FREE).orElse(false);
    }

    /**
     * Moves the owner's day to its next status: not set, free, taken, not set. Returns the new status, or empty if
     * the day is now not set.
     *
     * @throws IllegalArgumentException if the venue is not the owner's or the day cannot be marked
     */
    @Transactional
    public Optional<CalendarStatus> cycle(Long venueId, Long ownerId, LocalDate day) {
        if (venues.findByIdAndOwnerId(venueId, ownerId).isEmpty()) {
            throw new IllegalArgumentException("Not the owner's venue: " + venueId);
        }
        checkMarkable(day);
        Optional<VenueCalendarDay> existing = days.findByVenueIdAndDay(venueId, day);
        if (existing.isEmpty()) {
            days.save(new VenueCalendarDay(venueId, day, CalendarStatus.FREE));
            return Optional.of(CalendarStatus.FREE);
        }
        VenueCalendarDay marked = existing.get();
        if (marked.getStatus() == CalendarStatus.FREE) {
            marked.setStatus(CalendarStatus.BUSY);
            return Optional.of(CalendarStatus.BUSY);
        }
        days.delete(marked);
        return Optional.empty();
    }

    /**
     * Sets the owner's day to the given status, or clears it when {@code status} is null.
     *
     * @throws IllegalArgumentException if the venue is not the owner's or the day cannot be marked
     */
    @Transactional
    public void setStatus(Long venueId, Long ownerId, LocalDate day, CalendarStatus status) {
        if (venues.findByIdAndOwnerId(venueId, ownerId).isEmpty()) {
            throw new IllegalArgumentException("Not the owner's venue: " + venueId);
        }
        checkMarkable(day);
        Optional<VenueCalendarDay> existing = days.findByVenueIdAndDay(venueId, day);
        if (status == null) {
            existing.ifPresent(days::delete);
        } else if (existing.isPresent()) {
            existing.get().setStatus(status);
        } else {
            days.save(new VenueCalendarDay(venueId, day, status));
        }
    }

    private void checkMarkable(LocalDate day) {
        if (day.isBefore(dates.today()) || day.isAfter(dates.lastMarkableDay())) {
            throw new IllegalArgumentException("Day is outside the range that can be marked: " + day);
        }
    }
}
