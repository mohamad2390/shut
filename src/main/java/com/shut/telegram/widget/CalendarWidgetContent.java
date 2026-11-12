package com.shut.telegram.widget;

import com.shut.calendar.CalendarService;
import com.shut.common.calendar.JalaliMonth;
import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.stereotype.Component;

/** Builds the state of the calendar widget for one member and month. Months that cannot be viewed are clamped. */
@Component
@TelegramBotEnabled
public class CalendarWidgetContent {

    private final CalendarService calendar;

    public CalendarWidgetContent(CalendarService calendar) {
        this.calendar = calendar;
    }

    public JalaliMonth currentMonth() {
        return JalaliMonth.of(calendar.today());
    }

    public String month(long userId, String providerName, JalaliMonth requested) {
        JalaliMonth month = calendar.clamp(requested);
        var statuses = calendar.statuses(userId, month.atDay(1), month.atDay(month.length()));
        return CalendarWidgetState.month(heading(providerName), month, statuses, calendar.today(),
                calendar.hasPreviousMonth(month), calendar.hasNextMonth(month));
    }

    static String heading(String providerName) {
        return "تقویم " + providerName;
    }
}
