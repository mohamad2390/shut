package com.shut.telegram.calendar;

import com.shut.calendar.CalendarStatus;
import com.shut.common.calendar.JalaliMonth;
import com.shut.common.text.PersianDigits;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** The month view: a grid of days that the user taps to mark free or busy. */
final class CalendarScreens {

    /** Telegram lays keyboard rows out left to right, so each row is reversed to put Saturday on the right. */
    private static final boolean RIGHT_TO_LEFT = true;
    private static final int DAYS_IN_WEEK = 7;
    private static final String[] WEEKDAYS = {"ش", "ی", "د", "س", "چ", "پ", "ج"};
    private static final String FREE_MARK = "🟢";
    private static final String BUSY_MARK = "🔴";
    private static final String BLANK = "·";

    private CalendarScreens() {
    }

    /**
     * @param today           days before this are shown but cannot be changed
     * @param lastMarkableDay days after this are shown but cannot be changed
     */
    static Screen month(JalaliMonth month, Map<LocalDate, CalendarStatus> statuses, LocalDate today,
                        LocalDate lastMarkableDay, boolean hasPrevious, boolean hasNext) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        keyboard.grid(arrangeRow(weekdayHeader()), DAYS_IN_WEEK);
        keyboard.grid(dayGrid(month, statuses, today, lastMarkableDay), DAYS_IN_WEEK);

        List<InlineKeyboardButton> navigation = new ArrayList<>();
        if (hasPrevious) {
            navigation.add(InlineKeyboardButton.callback("▶️ ماه قبل", CalendarCallbacks.month(month.plusMonths(-1))));
        }
        if (hasNext) {
            navigation.add(InlineKeyboardButton.callback("ماه بعد ◀️", CalendarCallbacks.month(month.plusMonths(1))));
        }
        keyboard.grid(arrangeRow(navigation), Math.max(1, navigation.size()));
        keyboard.row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME));

        return new Screen(text(month, statuses), keyboard.build());
    }

    private static String text(JalaliMonth month, Map<LocalDate, CalendarStatus> statuses) {
        long free = statuses.values().stream().filter(status -> status == CalendarStatus.FREE).count();
        long busy = statuses.values().stream().filter(status -> status == CalendarStatus.BUSY).count();
        return "📅 " + month.name() + " " + PersianDigits.toPersian(month.year())
                + "\n\n" + FREE_MARK + " آزاد: " + PersianDigits.toPersian(free) + " روز"
                + "\n" + BUSY_MARK + " پر: " + PersianDigits.toPersian(busy) + " روز"
                + "\n\nروی هر روز بزن تا وضعیتش عوض بشه: آزاد، پر، و دوباره نامشخص.";
    }

    private static List<InlineKeyboardButton> weekdayHeader() {
        List<InlineKeyboardButton> header = new ArrayList<>();
        for (String name : WEEKDAYS) {
            header.add(InlineKeyboardButton.callback(name, CalendarCallbacks.NOOP));
        }
        return header;
    }

    /** All the day cells of the month, a week at a time, starting on Saturday. */
    private static List<InlineKeyboardButton> dayGrid(JalaliMonth month, Map<LocalDate, CalendarStatus> statuses,
                                                      LocalDate today, LocalDate lastMarkableDay) {
        List<InlineKeyboardButton> cells = new ArrayList<>();
        for (int blank = 0; blank < weekdayIndex(month.atDay(1)); blank++) {
            cells.add(InlineKeyboardButton.callback(BLANK, CalendarCallbacks.NOOP));
        }
        for (int dayOfMonth = 1; dayOfMonth <= month.length(); dayOfMonth++) {
            LocalDate date = month.atDay(dayOfMonth);
            boolean markable = !date.isBefore(today) && !date.isAfter(lastMarkableDay);
            cells.add(InlineKeyboardButton.callback(
                    label(dayOfMonth, statuses.get(date)),
                    markable ? CalendarCallbacks.day(date) : CalendarCallbacks.NOOP));
        }
        while (cells.size() % DAYS_IN_WEEK != 0) {
            cells.add(InlineKeyboardButton.callback(BLANK, CalendarCallbacks.NOOP));
        }

        List<InlineKeyboardButton> arranged = new ArrayList<>();
        for (int start = 0; start < cells.size(); start += DAYS_IN_WEEK) {
            arranged.addAll(arrangeRow(cells.subList(start, start + DAYS_IN_WEEK)));
        }
        return arranged;
    }

    private static String label(int dayOfMonth, CalendarStatus status) {
        String number = PersianDigits.toPersian(dayOfMonth);
        if (status == CalendarStatus.FREE) {
            return FREE_MARK + number;
        }
        return status == CalendarStatus.BUSY ? BUSY_MARK + number : number;
    }

    /** Saturday is 0, Sunday 1, and so on up to Friday, which is 6. */
    private static int weekdayIndex(LocalDate date) {
        return (date.getDayOfWeek().getValue() + 1) % DAYS_IN_WEEK;
    }

    private static List<InlineKeyboardButton> arrangeRow(List<InlineKeyboardButton> row) {
        List<InlineKeyboardButton> arranged = new ArrayList<>(row);
        if (RIGHT_TO_LEFT) {
            Collections.reverse(arranged);
        }
        return arranged;
    }
}
