package com.shut.telegram.widget;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.calendar.CalendarStatus;
import com.shut.common.calendar.JalaliMonth;
import com.shut.common.text.PersianDigits;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The state the calendar widget draws, as the JSON it expects:
 * <pre>{"h": heading, "l": "آبان ۱۴۰۵", "y": 1405, "o": 8, "w": 3, "n": 30, "s": "0120…", "t": 12, "a": 1, "z": 1}</pre>
 * where {@code y}/{@code o} are the year and month, {@code w} how many cells the first day is from the start of the
 * week (the week starts on Saturday), {@code n} the number of days, {@code s} one character per day
 * ({@code 0} not set, {@code 1} free, {@code 2} busy), {@code t} today's day of the month (0 if today is not in this
 * month; earlier days are in the past), and {@code a}/{@code z} whether there is a month before/after.
 *
 * <p>A month is at most about 150 bytes, far below the 1024 bytes Telegram allows.
 */
public final class CalendarWidgetState {

    private static final int MAX_HEADING_LENGTH = 36;
    private static final ObjectMapper JSON = new ObjectMapper();

    private CalendarWidgetState() {
    }

    public static String month(String heading, JalaliMonth month, Map<LocalDate, CalendarStatus> statuses,
                               LocalDate today, boolean hasPrevious, boolean hasNext) {
        StringBuilder days = new StringBuilder();
        for (int day = 1; day <= month.length(); day++) {
            days.append(code(statuses.get(month.atDay(day))));
        }
        LocalDate first = month.atDay(1);
        LocalDate last = month.atDay(month.length());
        boolean todayInMonth = !today.isBefore(first) && !today.isAfter(last);

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("h", shorten(heading));
        state.put("l", month.name() + " " + PersianDigits.toPersian(month.year()));
        state.put("y", month.year());
        state.put("o", month.month());
        state.put("w", weekOffset(first));
        state.put("n", month.length());
        state.put("s", days.toString());
        state.put("t", todayInMonth ? today.toEpochDay() - first.toEpochDay() + 1 : 0);
        state.put("a", hasPrevious ? 1 : 0);
        state.put("z", hasNext ? 1 : 0);
        return write(state);
    }

    /** How many cells after the start of the week (Saturday) the date falls: Saturday is 0, Friday is 6. */
    static int weekOffset(LocalDate date) {
        return (date.getDayOfWeek().getValue() + 1) % 7;
    }

    private static char code(CalendarStatus status) {
        if (status == null) {
            return '0';
        }
        return status == CalendarStatus.FREE ? '1' : '2';
    }

    private static String shorten(String text) {
        return text.length() > MAX_HEADING_LENGTH ? text.substring(0, MAX_HEADING_LENGTH - 1) + "…" : text;
    }

    private static String write(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write widget state", e);
        }
    }
}
