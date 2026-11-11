package com.shut.telegram.project;

/** Callback data for offers on projects. Format: {@code bid:<action>[:<argument>...]}. */
final class BidCallbacks {

    static final String PREFIX = "bid";
    /** Sends the offer without a note. */
    static final String NO_NOTE = PREFIX + ":nonote";
    /** Leaves the offer being written. */
    static final String ABANDON = PREFIX + ":abandon";

    private BidCallbacks() {
    }

    static String place(long projectId) {
        return PREFIX + ":new:" + projectId;
    }

    static String withdraw(long projectId) {
        return PREFIX + ":wd:" + projectId;
    }

    static String confirmWithdraw(long projectId) {
        return PREFIX + ":wdok:" + projectId;
    }

    /** The project owner's list of offers, cheapest first. */
    static String list(long projectId, int page) {
        return PREFIX + ":list:" + projectId + ":" + page;
    }

    static String view(long bidId) {
        return PREFIX + ":view:" + bidId;
    }

    static String pick(long bidId) {
        return PREFIX + ":pick:" + bidId;
    }

    static String confirmPick(long bidId) {
        return PREFIX + ":pickok:" + bidId;
    }

    static String mine(int page) {
        return PREFIX + ":mine:" + page;
    }
}
