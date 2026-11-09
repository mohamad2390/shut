package com.shut.telegram.calendar;

import com.shut.calendar.CalendarService;
import com.shut.common.calendar.JalaliMonth;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.TelegramUser;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Builds the calendar page for a Telegram user. Months outside the markable range are clamped. */
@Component
@TelegramBotEnabled
public class CalendarScreenProvider {

    /** The current month and the eleven after it can be viewed. */
    private static final int MONTHS_AHEAD = 11;

    private final TelegramAccountResolver accounts;
    private final CalendarService calendar;

    CalendarScreenProvider(TelegramAccountResolver accounts, CalendarService calendar) {
        this.accounts = accounts;
        this.calendar = calendar;
    }

    /** The current month. */
    public Screen current(TelegramUser from) {
        return month(from, JalaliMonth.of(calendar.today()));
    }

    Screen month(TelegramUser from, JalaliMonth requested) {
        LocalDate today = calendar.today();
        JalaliMonth first = JalaliMonth.of(today);
        JalaliMonth last = first.plusMonths(MONTHS_AHEAD);
        JalaliMonth month = requested.compareTo(first) < 0 ? first : requested.compareTo(last) > 0 ? last : requested;

        Long userId = accounts.resolve(from).getId();
        var statuses = calendar.statuses(userId, month.atDay(1), month.atDay(month.length()));
        return CalendarScreens.month(month, statuses, today, calendar.lastMarkableDay(),
                month.compareTo(first) > 0, month.compareTo(last) < 0);
    }
}
