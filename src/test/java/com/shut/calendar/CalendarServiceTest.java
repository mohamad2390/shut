package com.shut.calendar;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalendarServiceTest {

    private static final Long USER = 1L;
    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private final CalendarDayRepository repository = mock(CalendarDayRepository.class);
    private final Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(TEHRAN).toInstant(), TEHRAN);
    private final CalendarService service = new CalendarService(repository, clock);

    @Test
    void anUnsetDayBecomesFree() {
        LocalDate day = TODAY.plusDays(3);
        when(repository.findByUserIdAndDay(USER, day)).thenReturn(Optional.empty());

        assertThat(service.cycle(USER, day)).contains(CalendarStatus.FREE);
        verify(repository).save(any(CalendarDay.class));
    }

    @Test
    void aFreeDayBecomesBusyAndABusyDayIsCleared() {
        LocalDate day = TODAY.plusDays(3);
        CalendarDay marked = new CalendarDay(USER, day, CalendarStatus.FREE);
        when(repository.findByUserIdAndDay(USER, day)).thenReturn(Optional.of(marked));

        assertThat(service.cycle(USER, day)).contains(CalendarStatus.BUSY);
        assertThat(service.cycle(USER, day)).isEmpty();
        verify(repository).delete(marked);
    }

    @Test
    void todayCanBeMarkedButPastAndFarFutureDaysCannot() {
        when(repository.findByUserIdAndDay(USER, TODAY)).thenReturn(Optional.empty());
        assertThat(service.cycle(USER, TODAY)).contains(CalendarStatus.FREE);

        assertThatThrownBy(() -> service.cycle(USER, TODAY.minusDays(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.cycle(USER, TODAY.plusDays(CalendarService.MAX_DAYS_AHEAD + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void usesTehranAsTheCurrentDay() {
        Clock lateUtc = Clock.fixed(Instant.parse("2026-11-09T22:00:00Z"), TEHRAN);

        assertThat(new CalendarService(repository, lateUtc).today()).isEqualTo(LocalDate.of(2026, 11, 10));
    }
}
