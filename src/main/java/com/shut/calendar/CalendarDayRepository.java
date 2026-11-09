package com.shut.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarDayRepository extends JpaRepository<CalendarDay, Long> {

    Optional<CalendarDay> findByUserIdAndDay(Long userId, LocalDate day);

    List<CalendarDay> findByUserIdAndDayBetween(Long userId, LocalDate from, LocalDate to);
}
