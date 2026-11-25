package com.shut.api.me;

import com.shut.api.BadRequestException;
import com.shut.api.venue.CalendarDayResponse;
import com.shut.calendar.CalendarService;
import com.shut.calendar.CalendarStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** The specialist's own availability calendar, from the same service the bot uses. */
@Service
public class MyCalendarApiService {

    /** The longest stretch one request can ask for. */
    public static final int MAX_DAYS = 93;
    private static final int DEFAULT_DAYS = 31;

    private final CalendarService calendar;

    public MyCalendarApiService(CalendarService calendar) {
        this.calendar = calendar;
    }

    /**
     * The days the member marked between {@code from} and {@code to}, inclusive, in date order. Without them it is
     * today and the 30 days after.
     *
     * @throws BadRequestException if {@code to} is before {@code from} or the range is longer than 93 days
     */
    public List<CalendarDayResponse> days(Long userId, LocalDate from, LocalDate to) {
        LocalDate start = from == null ? calendar.today() : from;
        LocalDate end = to == null ? start.plusDays(DEFAULT_DAYS - 1) : to;
        if (end.isBefore(start)) {
            throw new BadRequestException("`to` must not be before `from`");
        }
        if (start.plusDays(MAX_DAYS).isBefore(end)) {
            throw new BadRequestException("The range can be at most " + MAX_DAYS + " days");
        }
        return calendar.statuses(userId, start, end).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new CalendarDayResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * @param status null clears the day
     * @throws BadRequestException if the day is in the past or more than a year ahead
     */
    public void mark(Long userId, LocalDate day, CalendarStatus status) {
        try {
            calendar.setStatus(userId, day, status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("That day cannot be marked: it is in the past or too far ahead");
        }
    }
}
