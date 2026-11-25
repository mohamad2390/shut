package com.shut.api.me;

import com.shut.calendar.CalendarStatus;

/** The new status of a day: {@code FREE}, {@code BUSY}, or null to clear it. */
public record CalendarDayRequest(CalendarStatus status) {
}
