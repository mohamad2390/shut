package com.shut.telegram.browse;

/** Callback data for ratings and comments about a member. Format: {@code review:<action>:<memberId>[:<argument>]}. */
final class ReviewCallbacks {

    static final String PREFIX = "review";

    private ReviewCallbacks() {
    }

    /** The member's ratings and comments, newest first. */
    static String list(long providerId, int page) {
        return PREFIX + ":l:" + providerId + ":" + page;
    }

    /** Opens the star chooser. */
    static String rate(long providerId) {
        return PREFIX + ":rate:" + providerId;
    }

    static String stars(long providerId, int stars) {
        return PREFIX + ":s:" + providerId + ":" + stars;
    }

    static String comment(long providerId) {
        return PREFIX + ":c:" + providerId;
    }

    static String delete(long providerId) {
        return PREFIX + ":d:" + providerId;
    }

    static String confirmDelete(long providerId) {
        return PREFIX + ":dok:" + providerId;
    }
}
