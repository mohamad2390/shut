package com.shut.telegram.widget;

import com.shut.common.calendar.JalaliMonth;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarWidgetAnswererTest {

    @Test
    void readsTheMonthTheWidgetAsksFor() {
        assertThat(CalendarWidgetAnswerer.requestedMonth("m:1405:9")).contains(new JalaliMonth(1405, 9));
    }

    @Test
    void theFirstRequestAndNonsenseMeanTheCurrentMonth() {
        assertThat(CalendarWidgetAnswerer.requestedMonth("i")).isEmpty();
        assertThat(CalendarWidgetAnswerer.requestedMonth(null)).isEmpty();
        assertThat(CalendarWidgetAnswerer.requestedMonth("m:1405")).isEmpty();
        assertThat(CalendarWidgetAnswerer.requestedMonth("m:abc:9")).isEmpty();
        assertThat(CalendarWidgetAnswerer.requestedMonth("m:1405:13")).isEmpty();
    }
}
