package com.shut.telegram.venue;

import com.shut.common.calendar.JalaliMonth;

import java.time.LocalDate;

/** Callback data for browsing venues and asking for a booking. Format: {@code venue:<action>[:<argument>...]}. */
public final class VenueCallbacks {

    public static final String PREFIX = "venue";
    /** The venues section: browse, my venues, my booking requests. */
    public static final String HOME = PREFIX + ":home";
    /** The owner's own venues, for the business menu. */
    public static final String OWNER_HOME = VenueOwnerCallbacks.MINE;
    /** Starts a new venue (asks for its name). */
    public static final String OWNER_NEW = VenueOwnerCallbacks.NEW;
    static final String MY_BOOKINGS = PREFIX + ":mybook";
    static final String NOOP = PREFIX + ":noop";

    private VenueCallbacks() {
    }

    static String list(int page) {
        return PREFIX + ":list:" + page;
    }

    static String venue(long venueId) {
        return PREFIX + ":v:" + venueId;
    }

    static String photos(long venueId) {
        return PREFIX + ":ph:" + venueId;
    }

    /** The current month of the venue's calendar. */
    static String calendar(long venueId) {
        return PREFIX + ":cal:" + venueId;
    }

    static String calendar(long venueId, JalaliMonth month) {
        return PREFIX + ":cal:" + venueId + ":" + month.year() + ":" + month.month();
    }

    /** Asks for the whole day instead of typing hours. */
    static String fullDay(long venueId, LocalDate day) {
        return PREFIX + ":bt:" + venueId + ":" + day;
    }

    static String book(long venueId, LocalDate day) {
        return PREFIX + ":book:" + venueId + ":" + day;
    }
}
