package com.shut.telegram.calendar;

import com.shut.calendar.CalendarService;
import com.shut.common.calendar.JalaliMonth;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;

/** Handles the calendar buttons: moving between months and marking days. */
@Component
@TelegramBotEnabled
class CalendarCallbackHandler extends CallbackHandler {

    private final TelegramAccountResolver accounts;
    private final CalendarService calendar;
    private final BotMessenger messenger;
    private final CalendarScreenProvider screens;

    CalendarCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate,
                            TelegramAccountResolver accounts, CalendarService calendar,
                            BotMessenger messenger, CalendarScreenProvider screens) {
        super(telegram, membershipGate);
        this.accounts = accounts;
        this.calendar = calendar;
        this.messenger = messenger;
        this.screens = screens;
    }

    @Override
    protected String prefix() {
        return CalendarCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "m" -> parseMonth(context).ifPresent(month -> showMonth(context, month));
            case "d" -> parseDay(context).ifPresent(day -> markDay(context, day));
            default -> { }
        }
    }

    private void markDay(CallbackContext context, LocalDate day) {
        try {
            calendar.cycle(accounts.resolve(context.user()).getId(), day);
        } catch (IllegalArgumentException ignored) {
            // the day is outside the range that can be marked; just redraw the month
        }
        showMonth(context, JalaliMonth.of(day));
    }

    private void showMonth(CallbackContext context, JalaliMonth month) {
        messenger.show(context.chatId(), context.messageId(), screens.month(context.user(), month));
    }

    private static Optional<JalaliMonth> parseMonth(CallbackContext context) {
        try {
            return Optional.of(new JalaliMonth(Integer.parseInt(context.arg(1)), Integer.parseInt(context.arg(2))));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDate> parseDay(CallbackContext context) {
        try {
            return Optional.of(LocalDate.parse(context.arg(1)));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }
}
