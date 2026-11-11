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
import java.util.List;
import java.util.Map;

/** The month view: a grid of days that the user taps to mark free or busy. */
final class CalendarScreens {

    private CalendarScreens() {
    }

    /**
     * @param today           days before this are shown but cannot be changed
     * @param lastMarkableDay days after this are shown but cannot be changed
     */
    static Screen month(JalaliMonth month, Map<LocalDate, CalendarStatus> statuses, LocalDate today,
                        LocalDate lastMarkableDay, boolean hasPrevious, boolean hasNext) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        CalendarGrid.append(keyboard, month, CalendarCallbacks.NOOP, (dayOfMonth, date) -> {
            boolean markable = !date.isBefore(today) && !date.isAfter(lastMarkableDay);
            return InlineKeyboardButton.callback(
                    CalendarGrid.label(dayOfMonth, statuses.get(date)),
                    markable ? CalendarCallbacks.day(date) : CalendarCallbacks.NOOP);
        });

        List<InlineKeyboardButton> navigation = new ArrayList<>();
        if (hasPrevious) {
            navigation.add(InlineKeyboardButton.callback("▶️ ماه قبل", CalendarCallbacks.month(month.plusMonths(-1))));
        }
        if (hasNext) {
            navigation.add(InlineKeyboardButton.callback("ماه بعد ◀️", CalendarCallbacks.month(month.plusMonths(1))));
        }
        keyboard.grid(CalendarGrid.arrangeRow(navigation), Math.max(1, navigation.size()));
        keyboard.row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME));

        return new Screen(text(month, statuses), keyboard.build());
    }

    private static String text(JalaliMonth month, Map<LocalDate, CalendarStatus> statuses) {
        long free = statuses.values().stream().filter(status -> status == CalendarStatus.FREE).count();
        long busy = statuses.values().stream().filter(status -> status == CalendarStatus.BUSY).count();
        return "📅 " + month.name() + " " + PersianDigits.toPersian(month.year())
                + "\n\n" + CalendarGrid.FREE_MARK + " آزاد: " + PersianDigits.toPersian(free) + " روز"
                + "\n" + CalendarGrid.BUSY_MARK + " پر: " + PersianDigits.toPersian(busy) + " روز"
                + "\n\nروی هر روز بزن تا وضعیتش عوض بشه: آزاد، پر، و دوباره نامشخص.";
    }
}
