package com.shut.common.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class JalaliDateTest {

    @Test
    void convertsKnownDates() {
        assertThat(JalaliDate.of(LocalDate.of(2026, 3, 21))).isEqualTo(new JalaliDate(1405, 1, 1));
        assertThat(JalaliDate.of(LocalDate.of(2026, 11, 10))).isEqualTo(new JalaliDate(1405, 8, 19));
        assertThat(JalaliDate.of(LocalDate.of(2025, 3, 21))).isEqualTo(new JalaliDate(1404, 1, 1));
        assertThat(JalaliDate.of(LocalDate.of(2024, 3, 20))).isEqualTo(new JalaliDate(1403, 1, 1));
    }

    @Test
    void knowsLeapYears() {
        assertThat(JalaliDate.isLeapYear(1403)).isTrue();
        assertThat(JalaliDate.isLeapYear(1404)).isFalse();
        assertThat(JalaliDate.isLeapYear(1408)).isTrue();
        assertThat(new JalaliDate(1403, 12, 30).toLocalDate()).isEqualTo(LocalDate.of(2025, 3, 20));
    }

    @Test
    void roundTripsEveryDayOfSeveralYears() {
        for (LocalDate date = LocalDate.of(2020, 1, 1); date.isBefore(LocalDate.of(2032, 1, 1)); date = date.plusDays(1)) {
            assertThat(JalaliDate.of(date).toLocalDate()).isEqualTo(date);
        }
    }

    @Test
    void monthArithmeticCrossesYearBoundaries() {
        assertThat(new JalaliMonth(1405, 12).plusMonths(1)).isEqualTo(new JalaliMonth(1406, 1));
        assertThat(new JalaliMonth(1405, 1).plusMonths(-1)).isEqualTo(new JalaliMonth(1404, 12));
        assertThat(new JalaliMonth(1405, 8).name()).isEqualTo("آبان");
        assertThat(new JalaliMonth(1405, 7).length()).isEqualTo(30);
        assertThat(new JalaliMonth(1405, 12).atDay(1)).isEqualTo(LocalDate.of(2027, 2, 20));
    }
}
