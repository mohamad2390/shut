package com.shut.telegram.calendar;

import com.shut.common.calendar.JalaliMonth;

import java.time.LocalDate;

/** Callback data for the calendar buttons. Format: {@code cal:<action>[:<argument>...]}. */
final class CalendarCallbacks {

    static final String PREFIX = "cal";
    /** For buttons that only take up space, such as weekday names and days that cannot be changed. */
    static final String NOOP = PREFIX + ":noop";

    private CalendarCallbacks() {
    }

    static String month(JalaliMonth month) {
        return PREFIX + ":m:" + month.year() + ":" + month.month();
    }

    static String day(LocalDate day) {
        return PREFIX + ":d:" + day;
    }
}
