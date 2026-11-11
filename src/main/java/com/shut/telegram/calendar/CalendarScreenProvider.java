package com.shut.telegram.calendar;

import com.shut.calendar.CalendarService;
import com.shut.common.calendar.JalaliMonth;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.TelegramUser;
import org.springframework.stereotype.Component;

/** Builds the calendar page for a Telegram user. Months outside the markable range are clamped. */
@Component
@TelegramBotEnabled
public class CalendarScreenProvider {

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
        JalaliMonth month = calendar.clamp(requested);
        Long userId = accounts.resolve(from).getId();
        var statuses = calendar.statuses(userId, month.atDay(1), month.atDay(month.length()));
        return CalendarScreens.month(month, statuses, calendar.today(), calendar.lastMarkableDay(),
                calendar.hasPreviousMonth(month), calendar.hasNextMonth(month));
    }
}
