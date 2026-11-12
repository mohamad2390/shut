package com.shut.telegram.widget;

import com.shut.telegram.config.TelegramBotEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** The pages of the widgets. They are checked against Telegram's limits once at startup, not on every send. */
@Component
@TelegramBotEnabled
public class WidgetHtml {

    static final String PRICES_RESOURCE = "telegram/widgets/services-widget.html";
    static final String CALENDAR_RESOURCE = "telegram/widgets/calendar-widget.html";

    private static final Logger log = LoggerFactory.getLogger(WidgetHtml.class);

    private final String prices;
    private final String calendar;

    public WidgetHtml() {
        this.prices = load(PRICES_RESOURCE);
        this.calendar = load(CALENDAR_RESOURCE);
    }

    /** The price widget page, or null if it is missing or too big for Telegram. */
    public String prices() {
        return prices;
    }

    /** The calendar widget page, or null if it is missing or too big for Telegram. */
    public String calendar() {
        return calendar;
    }

    private static String load(String resource) {
        try {
            String html = new ClassPathResource(resource).getContentAsString(StandardCharsets.UTF_8);
            String problem = WidgetLimits.problemWith(html);
            if (problem != null) {
                log.error("Widget {} will not be used: {}", resource, problem);
                return null;
            }
            return html;
        } catch (IOException e) {
            log.error("Widget {} could not be read: {}", resource, e.getMessage());
            return null;
        }
    }
}
