package com.shut.telegram.widget;

import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.stereotype.Component;

/** Shows a member's availability calendar in the calendar widget. */
@Component
@TelegramBotEnabled
public class CalendarWidgetPresenter {

    private final WidgetDelivery delivery;
    private final WidgetHtml html;
    private final CalendarWidgetContent content;

    public CalendarWidgetPresenter(WidgetDelivery delivery, WidgetHtml html, CalendarWidgetContent content) {
        this.delivery = delivery;
        this.html = html;
        this.content = content;
    }

    /**
     * Sends the widget opened on the current month, replacing the one this chat got earlier.
     * Returns false if it could not be sent, and the caller should show the calendar as text instead.
     */
    public boolean show(long chatId, long providerId, String providerName) {
        if (html.calendar() == null) {
            return false;
        }
        return delivery.replace(chatId, providerId, WidgetKind.CALENDAR, html.calendar(),
                content.month(providerId, providerName, content.currentMonth()));
    }
}
