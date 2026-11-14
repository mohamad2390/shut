package com.shut.telegram.venue;

import com.shut.common.calendar.JalaliMonth;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;

/** Reads the parts of callback data and conversation data back into numbers, dates and months. */
final class VenueArgs {

    private VenueArgs() {
    }

    static Optional<Long> id(String raw) {
        try {
            return Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static Optional<LocalDate> date(String raw) {
        try {
            return Optional.of(LocalDate.parse(raw));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    /** The month for the two parts, or empty if they are missing or not a month. */
    static Optional<JalaliMonth> month(String year, String month) {
        try {
            return Optional.of(new JalaliMonth(Integer.parseInt(year), Integer.parseInt(month)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
