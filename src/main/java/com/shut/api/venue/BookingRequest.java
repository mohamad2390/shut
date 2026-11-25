package com.shut.api.venue;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A request for a venue: a day ({@code 2026-12-05}) and, for hours rather than the whole day, both a start and an end
 * time ({@code 14:30}).
 */
public record BookingRequest(LocalDate date, LocalTime startTime, LocalTime endTime, String note) {
}
