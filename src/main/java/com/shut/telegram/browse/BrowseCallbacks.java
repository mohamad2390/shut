package com.shut.telegram.browse;

import com.shut.common.calendar.JalaliMonth;
import com.shut.showcase.ShowcaseCategory;

/**
 * Callback data for finding other members and viewing their pages.
 * Format: {@code browse:<action>[:<argument>...]}.
 */
public final class BrowseCallbacks {

    public static final String PREFIX = "browse";
    /** Opens the search prompt; used by the main menu. */
    public static final String START = PREFIX + ":start";
    static final String ALL = PREFIX + ":all";
    static final String BACK_TO_RESULTS = PREFIX + ":back";
    /** For buttons that only take up space, such as weekday names and empty cells. */
    static final String NOOP = PREFIX + ":noop";

    private BrowseCallbacks() {
    }

    static String results(int page) {
        return PREFIX + ":r:" + page;
    }

    /** Opens a member's page. */
    public static String provider(long userId) {
        return PREFIX + ":p:" + userId;
    }

    static String showcase(long userId) {
        return PREFIX + ":sc:" + userId;
    }

    static String showcaseItems(long userId, ShowcaseCategory category, int page) {
        return PREFIX + ":sv:" + userId + ":" + category.name() + ":" + page;
    }

    /** The member's calendar, opened on the current month. */
    static String currentCalendar(long userId) {
        return PREFIX + ":cal:" + userId;
    }

    static String calendar(long userId, JalaliMonth month) {
        return PREFIX + ":cal:" + userId + ":" + month.year() + ":" + month.month();
    }

    static String prices(long userId) {
        return PREFIX + ":pr:" + userId;
    }
}
