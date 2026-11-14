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

    static final String MENU_RESOURCE = "telegram/widgets/provider-menu-widget.html";
    static final String PROFILE_RESOURCE = "telegram/widgets/profile-widget.html";
    static final String SHOWCASE_RESOURCE = "telegram/widgets/showcase-widget.html";
    static final String REVIEWS_RESOURCE = "telegram/widgets/reviews-widget.html";
    static final String MEMBERS_RESOURCE = "telegram/widgets/members-widget.html";

    private static final Logger log = LoggerFactory.getLogger(WidgetHtml.class);

    private final String prices;
    private final String calendar;
    private final String members;
    private final String menu;
    private final String profile;
    private final String showcase;
    private final String reviews;

    public WidgetHtml() {
        this.prices = load(PRICES_RESOURCE);
        this.calendar = load(CALENDAR_RESOURCE);
        this.members = load(MEMBERS_RESOURCE);
        this.menu = load(MENU_RESOURCE);
        this.profile = load(PROFILE_RESOURCE);
        this.showcase = load(SHOWCASE_RESOURCE);
        this.reviews = load(REVIEWS_RESOURCE);
    }

    /** The price widget page, or null if it is missing or too big for Telegram. */
    public String prices() {
        return prices;
    }

    /** The calendar widget page, or null if it is missing or too big for Telegram. */
    public String calendar() {
        return calendar;
    }

    /** The ranked members widget page, or null if it is missing or too big for Telegram. */
    public String members() {
        return members;
    }

    /** The page of the widget with a member's four options, or null if it is missing or too big for Telegram. */
    public String menu() {
        return menu;
    }

    /** The member profile widget page, or null if it is missing or too big for Telegram. */
    public String profile() {
        return profile;
    }

    /** The member showcase widget page, or null if it is missing or too big for Telegram. */
    public String showcase() {
        return showcase;
    }

    /** The member reviews widget page, or null if it is missing or too big for Telegram. */
    public String reviews() {
        return reviews;
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
