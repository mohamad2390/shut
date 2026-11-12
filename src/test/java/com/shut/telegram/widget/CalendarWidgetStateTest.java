package com.shut.telegram.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.calendar.CalendarStatus;
import com.shut.common.calendar.JalaliMonth;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarWidgetStateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12); // 21 Aban 1405
    private static final JalaliMonth ABAN = new JalaliMonth(1405, 8);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode state(JalaliMonth month, Map<LocalDate, CalendarStatus> statuses,
                                  boolean hasPrevious, boolean hasNext) throws Exception {
        return JSON.readTree(CalendarWidgetState.month("تقویم علی", month, statuses, TODAY, hasPrevious, hasNext));
    }

    @Test
    void theWeekStartsOnSaturdayAndEndsOnFriday() {
        assertThat(CalendarWidgetState.weekOffset(LocalDate.of(2026, 11, 14))).isZero();   // Saturday
        assertThat(CalendarWidgetState.weekOffset(LocalDate.of(2026, 11, 15))).isEqualTo(1); // Sunday
        assertThat(CalendarWidgetState.weekOffset(LocalDate.of(2026, 11, 16))).isEqualTo(2); // Monday
        assertThat(CalendarWidgetState.weekOffset(LocalDate.of(2026, 11, 13))).isEqualTo(6); // Friday
    }

    @Test
    void describesTheMonthByJalaliDays() throws Exception {
        JsonNode state = state(ABAN, Map.of(), false, true);

        assertThat(state.get("h").asText()).isEqualTo("تقویم علی");
        assertThat(state.get("l").asText()).isEqualTo("آبان ۱۴۰۵");
        assertThat(state.get("y").asInt()).isEqualTo(1405);
        assertThat(state.get("o").asInt()).isEqualTo(8);
        assertThat(state.get("n").asInt()).isEqualTo(30);
        assertThat(state.get("w").asInt()).isEqualTo(6); // 1 Aban 1405 is a Friday
        assertThat(state.get("a").asInt()).isZero();
        assertThat(state.get("z").asInt()).isEqualTo(1);
    }

    @Test
    void writesOneCharacterPerDayWithItsStatusOnTheRightDay() throws Exception {
        Map<LocalDate, CalendarStatus> statuses = new HashMap<>();
        statuses.put(ABAN.atDay(21), CalendarStatus.FREE);
        statuses.put(ABAN.atDay(23), CalendarStatus.BUSY);

        String days = state(ABAN, statuses, false, true).get("s").asText();

        assertThat(days).hasSize(30);
        assertThat(days.charAt(20)).isEqualTo('1');
        assertThat(days.charAt(22)).isEqualTo('2');
        assertThat(days.replace("0", "")).hasSize(2);
    }

    @Test
    void marksTodayOnlyInTheMonthItIsIn() throws Exception {
        assertThat(state(ABAN, Map.of(), false, true).get("t").asInt()).isEqualTo(21);
        assertThat(state(ABAN.plusMonths(1), Map.of(), true, true).get("t").asInt()).isZero();
    }

    @Test
    void esfandHasTwentyNineDaysInACommonYear() throws Exception {
        JsonNode state = state(new JalaliMonth(1405, 12), Map.of(), true, false);

        assertThat(state.get("n").asInt()).isEqualTo(29);
        assertThat(state.get("s").asText()).hasSize(29);
    }

    @Test
    void aLongNameIsShortenedAndTheStateStaysFarBelowTheLimit() {
        String longName = "ن".repeat(200);

        String json = CalendarWidgetState.month("تقویم " + longName, ABAN, Map.of(), TODAY, true, true);

        assertThat(json.getBytes(StandardCharsets.UTF_8).length).isLessThan(300);
    }
}
