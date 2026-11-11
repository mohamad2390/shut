package com.shut.telegram.calendar;

import com.shut.calendar.CalendarStatus;
import com.shut.common.calendar.JalaliMonth;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarScreensTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);          // 19 Aban 1405
    private static final JalaliMonth ABAN = new JalaliMonth(1405, 8);            // starts on a Friday

    private Screen aban(Map<LocalDate, CalendarStatus> statuses) {
        return CalendarScreens.month(ABAN, statuses, TODAY, TODAY.plusDays(366), true, true);
    }

    private static List<String> texts(List<InlineKeyboardButton> row) {
        return row.stream().map(InlineKeyboardButton::text).toList();
    }

    @Test
    void labelsDaysWithTheJalaliDayOfMonthNotTheGregorianOne() {
        List<String> labels = aban(Map.of()).markup().inlineKeyboard().stream()
                .skip(1).limit(6).flatMap(List::stream)
                .map(InlineKeyboardButton::text)
                .filter(text -> !text.equals("·"))
                .toList();

        assertThat(labels).hasSize(30);
        assertThat(labels).contains("۱", "۱۵", "۳۰");
    }

    @Test
    void putsSaturdayOnTheRightOfTheWeekdayHeader() {
        List<InlineKeyboardButton> header = aban(Map.of()).markup().inlineKeyboard().get(0);

        assertThat(texts(header)).containsExactly("ج", "پ", "چ", "س", "د", "ی", "ش");
    }

    @Test
    void startsTheMonthOnTheRightWeekday() {
        // 1 Aban 1405 is a Friday, the last day of a Saturday-first week, so it sits alone at the left of the first row.
        List<InlineKeyboardButton> firstWeek = aban(Map.of()).markup().inlineKeyboard().get(1);

        assertThat(texts(firstWeek)).containsExactly("۱", "·", "·", "·", "·", "·", "·");
    }

    @Test
    void marksDaysAndOnlyLetsUpcomingOnesBeChanged() {
        Screen screen = aban(Map.of(
                LocalDate.of(2026, 11, 12), CalendarStatus.FREE,
                LocalDate.of(2026, 11, 13), CalendarStatus.BUSY));
        List<InlineKeyboardButton> buttons = screen.markup().inlineKeyboard().stream().flatMap(List::stream).toList();

        InlineKeyboardButton free = buttons.stream().filter(b -> b.text().equals("🟢۲۱")).findFirst().orElseThrow();
        InlineKeyboardButton busy = buttons.stream().filter(b -> b.text().equals("🔴۲۲")).findFirst().orElseThrow();
        InlineKeyboardButton past = buttons.stream().filter(b -> b.text().equals("۱۸")).findFirst().orElseThrow();
        InlineKeyboardButton today = buttons.stream().filter(b -> b.text().equals("۱۹")).findFirst().orElseThrow();

        assertThat(free.callbackData()).isEqualTo("cal:d:2026-11-12");
        assertThat(busy.callbackData()).isEqualTo("cal:d:2026-11-13");
        assertThat(past.callbackData()).isEqualTo(CalendarCallbacks.NOOP);
        assertThat(today.callbackData()).isEqualTo("cal:d:2026-11-10");
    }

    @Test
    void summarisesTheMonthInTheText() {
        Screen screen = aban(Map.of(
                LocalDate.of(2026, 11, 12), CalendarStatus.FREE,
                LocalDate.of(2026, 11, 13), CalendarStatus.BUSY,
                LocalDate.of(2026, 11, 14), CalendarStatus.BUSY));

        assertThat(screen.text()).contains("آبان ۱۴۰۵").contains("آزاد: ۱ روز").contains("پر: ۲ روز");
    }

    @Test
    void offersNavigationOnlyInTheDirectionsThatExist() {
        Screen first = CalendarScreens.month(ABAN, Map.of(), TODAY, TODAY.plusDays(366), false, true);
        List<InlineKeyboardButton> buttons = first.markup().inlineKeyboard().stream().flatMap(List::stream).toList();

        assertThat(buttons).noneMatch(b -> b.text().contains("ماه قبل"));
        assertThat(buttons).anyMatch(b -> "cal:m:1405:9".equals(b.callbackData()));
    }
}
