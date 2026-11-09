package com.shut.common.calendar;

import java.time.LocalDate;

/** A month of the Jalali calendar, such as Mehr 1405. */
public record JalaliMonth(int year, int month) implements Comparable<JalaliMonth> {

    private static final String[] NAMES = {
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    };

    public JalaliMonth {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be 1..12 but was " + month);
        }
    }

    public static JalaliMonth of(LocalDate date) {
        JalaliDate jalali = JalaliDate.of(date);
        return new JalaliMonth(jalali.year(), jalali.month());
    }

    public JalaliMonth plusMonths(int months) {
        int index = year * 12 + (month - 1) + months;
        return new JalaliMonth(Math.floorDiv(index, 12), Math.floorMod(index, 12) + 1);
    }

    public int length() {
        return JalaliDate.monthLength(year, month);
    }

    /** The Gregorian date of the given day of this month, counting from 1. */
    public LocalDate atDay(int day) {
        return new JalaliDate(year, month, day).toLocalDate();
    }

    public String name() {
        return NAMES[month - 1];
    }

    @Override
    public int compareTo(JalaliMonth other) {
        return Integer.compare(year * 12 + month, other.year * 12 + other.month);
    }
}
