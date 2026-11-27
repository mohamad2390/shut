package com.shut.telegram.rental;

/** Callback data for what anyone sees: the rental section, shops, items and the item search. */
public final class RentalCallbacks {

    public static final String PREFIX = "rnt";
    public static final String HOME = PREFIX + ":home";
    /** The member's own shop: its management screen, or the form to register one. */
    public static final String OWNER_HOME = RentalOwnerCallbacks.MINE;
    static final String SEARCH = PREFIX + ":search";

    private RentalCallbacks() {
    }

    static String list(int page) {
        return PREFIX + ":list:" + page;
    }

    static String shop(long id) {
        return PREFIX + ":s:" + id;
    }

    static String items(long shopId) {
        return PREFIX + ":its:" + shopId;
    }

    static String item(long id) {
        return PREFIX + ":i:" + id;
    }

    static String photos(long itemId) {
        return PREFIX + ":ph:" + itemId;
    }
}
