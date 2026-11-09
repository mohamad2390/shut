package com.shut.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** A day a provider marked as free or busy. */
@Entity
@Table(name = "calendar_days")
public class CalendarDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "day_date", nullable = false, updatable = false)
    private LocalDate day;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CalendarStatus status;

    protected CalendarDay() {
    }

    public CalendarDay(Long userId, LocalDate day, CalendarStatus status) {
        this.userId = userId;
        this.day = day;
        this.status = status;
    }

    public LocalDate getDay() { return day; }
    public CalendarStatus getStatus() { return status; }
    public void setStatus(CalendarStatus status) { this.status = status; }
}
