package com.shut.telegram.widget;

import com.shut.calendar.CalendarService;
import com.shut.calendar.CalendarDayRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalendarWidgetPresenterTest {

    private static final long CHAT = 500L;
    private static final long PROVIDER = 7L;
    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");

    private final WidgetDelivery delivery = mock(WidgetDelivery.class);
    private final Clock clock = Clock.fixed(java.time.LocalDate.of(2026, 11, 12).atTime(12, 0).atZone(TEHRAN).toInstant(), TEHRAN);
    private final CalendarWidgetContent content =
            new CalendarWidgetContent(new CalendarService(mock(CalendarDayRepository.class), clock));
    private final CalendarWidgetPresenter presenter = new CalendarWidgetPresenter(delivery, new WidgetHtml(), content);

    @Test
    void sendsTheCalendarWidgetOpenedOnTheCurrentMonth() {
        when(delivery.replace(anyLong(), anyLong(), any(), any(), any())).thenReturn(true);

        assertThat(presenter.show(CHAT, PROVIDER, "علی")).isTrue();

        ArgumentCaptor<String> state = ArgumentCaptor.forClass(String.class);
        verify(delivery).replace(eq(CHAT), eq(PROVIDER), eq(WidgetKind.CALENDAR), any(), state.capture());
        assertThat(state.getValue()).contains("تقویم علی", "آبان ۱۴۰۵");
    }

    @Test
    void reportsFailureSoTheCalendarCanBeShownAsText() {
        when(delivery.replace(anyLong(), anyLong(), any(), any(), any())).thenReturn(false);

        assertThat(presenter.show(CHAT, PROVIDER, "علی")).isFalse();
    }
}
