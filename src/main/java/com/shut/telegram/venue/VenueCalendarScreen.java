package com.shut.telegram.venue;

import com.shut.calendar.CalendarStatus;
import com.shut.common.calendar.JalaliMonth;
import com.shut.common.text.PersianDigits;
import com.shut.telegram.calendar.CalendarGrid;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** The month grid of a venue, used by the owner to mark days and by visitors to pick a day to ask for. */
final class VenueCalendarScreen {

    private VenueCalendarScreen() {
    }

    /**
     * @param dayCallback  callback data for a day button, or null when that day cannot be tapped
     * @param monthCallback callback data for the button that opens another month
     */
    static Screen month(String venueName, JalaliMonth month, Map<LocalDate, CalendarStatus> statuses, String help,
                        Function<LocalDate, String> dayCallback, Function<JalaliMonth, String> monthCallback,
                        boolean hasPrevious, boolean hasNext, InlineKeyboardButton back) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        CalendarGrid.append(keyboard, month, VenueCallbacks.NOOP, (dayOfMonth, date) -> {
            String callback = dayCallback.apply(date);
            return InlineKeyboardButton.callback(CalendarGrid.label(dayOfMonth, statuses.get(date)),
                    callback == null ? VenueCallbacks.NOOP : callback);
        });
        List<InlineKeyboardButton> navigation = new ArrayList<>();
        if (hasPrevious) {
            navigation.add(InlineKeyboardButton.callback("▶️ ماه قبل", monthCallback.apply(month.plusMonths(-1))));
        }
        if (hasNext) {
            navigation.add(InlineKeyboardButton.callback("ماه بعد ◀️", monthCallback.apply(month.plusMonths(1))));
        }
        keyboard.grid(CalendarGrid.arrangeRow(navigation), Math.max(1, navigation.size()));
        keyboard.row(back);

        long free = statuses.values().stream().filter(status -> status == CalendarStatus.FREE).count();
        long busy = statuses.values().stream().filter(status -> status == CalendarStatus.BUSY).count();
        String text = "📅 " + venueName + "\n" + month.name() + " " + PersianDigits.toPersian(month.year())
                + "\n\n" + CalendarGrid.FREE_MARK + " آزاد: " + PersianDigits.toPersian(free) + " روز"
                + "\n" + CalendarGrid.BUSY_MARK + " پر: " + PersianDigits.toPersian(busy) + " روز"
                + "\n\n" + help;
        return new Screen(text, keyboard.build());
    }
}
