package com.shut.common.calendar;

import java.time.LocalDate;

/**
 * A date in the Jalali (Persian) calendar.
 *
 * <p>Leap years follow the 33-year rule, which matches the official Iranian calendar for the
 * years 1244 to 1472 (about 1865 to 2093). Conversion counts days from a known anchor date.
 */
public record JalaliDate(int year, int month, int day) {

    private static final int ANCHOR_YEAR = 1405;
    /** 1 Farvardin 1405. */
    private static final LocalDate ANCHOR = LocalDate.of(2026, 3, 21);
    private static final int[] LEAP_REMAINDERS = {1, 5, 9, 13, 17, 22, 26, 30};

    public static JalaliDate of(LocalDate date) {
        long days = date.toEpochDay() - ANCHOR.toEpochDay();
        int year = ANCHOR_YEAR;
        while (days >= yearLength(year)) {
            days -= yearLength(year);
            year++;
        }
        while (days < 0) {
            year--;
            days += yearLength(year);
        }

        int month = 1;
        while (days >= monthLength(year, month)) {
            days -= monthLength(year, month);
            month++;
        }
        return new JalaliDate(year, month, (int) days + 1);
    }

    public LocalDate toLocalDate() {
        long days = 0;
        for (int y = ANCHOR_YEAR; y < year; y++) {
            days += yearLength(y);
        }
        for (int y = year; y < ANCHOR_YEAR; y++) {
            days -= yearLength(y);
        }
        for (int m = 1; m < month; m++) {
            days += monthLength(year, m);
        }
        return ANCHOR.plusDays(days + day - 1);
    }

    public static boolean isLeapYear(int year) {
        int remainder = Math.floorMod(year, 33);
        for (int leap : LEAP_REMAINDERS) {
            if (remainder == leap) {
                return true;
            }
        }
        return false;
    }

    public static int yearLength(int year) {
        return isLeapYear(year) ? 366 : 365;
    }

    /** The first six months have 31 days, the next five 30, and Esfand 29 (30 in a leap year). */
    public static int monthLength(int year, int month) {
        if (month <= 6) {
            return 31;
        }
        if (month <= 11) {
            return 30;
        }
        return isLeapYear(year) ? 30 : 29;
    }
}
