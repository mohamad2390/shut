package com.shut.venue;

import com.shut.calendar.CalendarStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** A day the owner marked a venue as free or taken. */
@Entity
@Table(name = "venue_calendar_days")
public class VenueCalendarDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venue_id", nullable = false, updatable = false)
    private Long venueId;

    @Column(name = "day_date", nullable = false, updatable = false)
    private LocalDate day;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CalendarStatus status;

    protected VenueCalendarDay() {
    }

    public VenueCalendarDay(Long venueId, LocalDate day, CalendarStatus status) {
        this.venueId = venueId;
        this.day = day;
        this.status = status;
    }

    public LocalDate getDay() { return day; }
    public CalendarStatus getStatus() { return status; }
    public void setStatus(CalendarStatus status) { this.status = status; }
}
