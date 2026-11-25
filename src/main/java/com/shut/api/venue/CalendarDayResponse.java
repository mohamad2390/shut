package com.shut.api.venue;

import com.shut.calendar.CalendarStatus;

import java.time.LocalDate;

/** A day the owner marked. Days not listed have not been set. Dates are Gregorian ({@code yyyy-MM-dd}). */
public record CalendarDayResponse(LocalDate date, CalendarStatus status) {
}
