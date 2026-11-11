package com.shut.telegram.calendar;

import com.shut.calendar.CalendarStatus;
import com.shut.common.calendar.JalaliMonth;
import com.shut.common.text.PersianDigits;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.model.InlineKeyboardButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/** Lays a Jalali month out as keyboard rows: weekday names, then weeks starting on Saturday. */
public final class CalendarGrid {

    /** Telegram lays keyboard rows out left to right, so each row is reversed to put Saturday on the right. */
    private static final boolean RIGHT_TO_LEFT = true;
    private static final int DAYS_IN_WEEK = 7;
    private static final String[] WEEKDAYS = {"ش", "ی", "د", "س", "چ", "پ", "ج"};
    private static final String BLANK = "·";

    public static final String FREE_MARK = "🟢";
    public static final String BUSY_MARK = "🔴";

    private CalendarGrid() {
    }

    /**
     * Adds the weekday header and one row per week.
     *
     * @param noopData callback data for buttons that do nothing: weekday names and empty cells
     * @param dayButton makes the button for a day, given its Jalali day of the month and its date
     */
    public static void append(KeyboardBuilder keyboard, JalaliMonth month, String noopData,
                              BiFunction<Integer, LocalDate, InlineKeyboardButton> dayButton) {
        List<InlineKeyboardButton> header = new ArrayList<>();
        for (String name : WEEKDAYS) {
            header.add(InlineKeyboardButton.callback(name, noopData));
        }
        keyboard.grid(arrangeRow(header), DAYS_IN_WEEK);

        List<InlineKeyboardButton> cells = new ArrayList<>();
        for (int blank = 0; blank < weekdayIndex(month.atDay(1)); blank++) {
            cells.add(InlineKeyboardButton.callback(BLANK, noopData));
        }
        for (int dayOfMonth = 1; dayOfMonth <= month.length(); dayOfMonth++) {
            cells.add(dayButton.apply(dayOfMonth, month.atDay(dayOfMonth)));
        }
        while (cells.size() % DAYS_IN_WEEK != 0) {
            cells.add(InlineKeyboardButton.callback(BLANK, noopData));
        }

        List<InlineKeyboardButton> arranged = new ArrayList<>();
        for (int start = 0; start < cells.size(); start += DAYS_IN_WEEK) {
            arranged.addAll(arrangeRow(cells.subList(start, start + DAYS_IN_WEEK)));
        }
        keyboard.grid(arranged, DAYS_IN_WEEK);
    }

    /** The day number in Persian digits, with a coloured mark in front when the day is free or busy. */
    public static String label(int dayOfMonth, CalendarStatus status) {
        String number = PersianDigits.toPersian(dayOfMonth);
        if (status == CalendarStatus.FREE) {
            return FREE_MARK + number;
        }
        return status == CalendarStatus.BUSY ? BUSY_MARK + number : number;
    }

    /** Puts a row in reading order for right-to-left text. Use it for rows built outside the grid too. */
    public static List<InlineKeyboardButton> arrangeRow(List<InlineKeyboardButton> row) {
        List<InlineKeyboardButton> arranged = new ArrayList<>(row);
        if (RIGHT_TO_LEFT) {
            Collections.reverse(arranged);
        }
        return arranged;
    }

    /** Saturday is 0, Sunday 1, and so on up to Friday, which is 6. */
    private static int weekdayIndex(LocalDate date) {
        return (date.getDayOfWeek().getValue() + 1) % DAYS_IN_WEEK;
    }
}
