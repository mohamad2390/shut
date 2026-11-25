package com.shut.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/** A member's request to use a venue on one day. */
@Entity
@Table(name = "venue_bookings")
public class VenueBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venue_id", nullable = false, updatable = false)
    private Long venueId;

    @Column(name = "requester_id", nullable = false, updatable = false)
    private Long requesterId;

    @Column(name = "day_date", nullable = false, updatable = false)
    private LocalDate day;

    /** When the booking starts, or null together with {@code endTime} for the whole day. */
    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected VenueBooking() {
    }

    public VenueBooking(Long venueId, Long requesterId, LocalDate day, LocalTime startTime, LocalTime endTime,
                        String note) {
        this.venueId = venueId;
        this.requesterId = requesterId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.note = note;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getVenueId() { return venueId; }
    public Long getRequesterId() { return requesterId; }
    public LocalDate getDay() { return day; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getNote() { return note; }

    public boolean isFullDay() {
        return startTime == null || endTime == null;
    }

    public boolean sameSlotAs(LocalDate otherDay, LocalTime otherStart, LocalTime otherEnd) {
        return day.equals(otherDay) && java.util.Objects.equals(startTime, otherStart)
                && java.util.Objects.equals(endTime, otherEnd);
    }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
}
