package com.shut.api.venue;

import java.time.LocalDate;
import java.time.LocalTime;

/** A member's own booking request. {@code startTime} and {@code endTime} are null for the whole day. */
public record BookingResponse(Long id, Long venueId, String venueName, LocalDate date, LocalTime startTime,
                              LocalTime endTime, boolean fullDay, String note, LabelResponse status) {
}
