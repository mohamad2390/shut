package com.shut.telegram.studio;

/** Callback data for what anyone sees: the studios section, a studio's pages, and answering an invitation. */
public final class StudioCallbacks {

    public static final String PREFIX = "std";
    public static final String HOME = PREFIX + ":home";
    /** The member's own studio: its management screen, or the form to register one. */
    public static final String OWNER_HOME = StudioOwnerCallbacks.MINE;

    private StudioCallbacks() {
    }

    static String list(int page) {
        return PREFIX + ":list:" + page;
    }

    static String studio(long id) {
        return PREFIX + ":s:" + id;
    }

    static String offerings(long id) {
        return PREFIX + ":of:" + id;
    }

    static String media(long id) {
        return PREFIX + ":md:" + id;
    }

    static String people(long id) {
        return PREFIX + ":pp:" + id;
    }

    static String leave(long id) {
        return PREFIX + ":lv:" + id;
    }

    static String accept(long memberId) {
        return PREFIX + ":ia:" + memberId;
    }

    static String decline(long memberId) {
        return PREFIX + ":id:" + memberId;
    }
}
