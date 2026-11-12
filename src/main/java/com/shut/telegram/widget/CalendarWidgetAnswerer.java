package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.common.calendar.JalaliMonth;
import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** The calendar widget asks for a month: {@code "i"} for the current one, {@code "m:1405:9"} for Azar 1405. */
@Component
@TelegramBotEnabled
class CalendarWidgetAnswerer implements WidgetAnswerer {

    private static final String MONTH_PREFIX = "m:";

    private final CalendarWidgetContent content;

    CalendarWidgetAnswerer(CalendarWidgetContent content) {
        this.content = content;
    }

    @Override
    public WidgetKind kind() {
        return WidgetKind.CALENDAR;
    }

    @Override
    public String answer(ProviderDetails provider, String request) {
        return content.month(provider.userId(), WidgetNames.of(provider),
                requestedMonth(request).orElseGet(content::currentMonth));
    }

    static Optional<JalaliMonth> requestedMonth(String request) {
        if (request == null || !request.startsWith(MONTH_PREFIX)) {
            return Optional.empty();
        }
        String[] parts = request.substring(MONTH_PREFIX.length()).split(":");
        try {
            return parts.length == 2
                    ? Optional.of(new JalaliMonth(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])))
                    : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
