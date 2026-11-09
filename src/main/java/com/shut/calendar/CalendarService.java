package com.shut.calendar;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A provider's availability calendar: which upcoming days are free and which are busy. */
@Service
public class CalendarService {

    /** How far ahead a day can be marked. */
    public static final int MAX_DAYS_AHEAD = 366;

    private final CalendarDayRepository repository;
    private final Clock clock;

    public CalendarService(CalendarDayRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** The last day that can still be marked. */
    public LocalDate lastMarkableDay() {
        return today().plusDays(MAX_DAYS_AHEAD);
    }

    /** The marked days between the two dates, inclusive. Days that are not set are absent. */
    @Transactional(readOnly = true)
    public Map<LocalDate, CalendarStatus> statuses(Long userId, LocalDate from, LocalDate to) {
        Map<LocalDate, CalendarStatus> statuses = new HashMap<>();
        repository.findByUserIdAndDayBetween(userId, from, to)
                .forEach(day -> statuses.put(day.getDay(), day.getStatus()));
        return statuses;
    }

    /**
     * Moves the day to its next status: not set, then free, then busy, then back to not set.
     * Returns the new status, or empty if the day is now not set.
     *
     * @throws IllegalArgumentException if the day is in the past or too far ahead
     */
    @Transactional
    public Optional<CalendarStatus> cycle(Long userId, LocalDate day) {
        if (day.isBefore(today()) || day.isAfter(lastMarkableDay())) {
            throw new IllegalArgumentException("Day is outside the range that can be marked: " + day);
        }

        Optional<CalendarDay> existing = repository.findByUserIdAndDay(userId, day);
        if (existing.isEmpty()) {
            repository.save(new CalendarDay(userId, day, CalendarStatus.FREE));
            return Optional.of(CalendarStatus.FREE);
        }

        CalendarDay marked = existing.get();
        if (marked.getStatus() == CalendarStatus.FREE) {
            marked.setStatus(CalendarStatus.BUSY);
            return Optional.of(CalendarStatus.BUSY);
        }
        repository.delete(marked);
        return Optional.empty();
    }
}
