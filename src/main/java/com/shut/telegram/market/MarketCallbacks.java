package com.shut.telegram.market;

/** Callback data for browsing the gear market and making offers. Format: {@code mkt:<action>[:<argument>...]}. */
public final class MarketCallbacks {

    public static final String PREFIX = "mkt";
    /** The market section: browse, my listings, my offers. */
    public static final String HOME = PREFIX + ":home";
    static final String MY_OFFERS = PREFIX + ":mo";

    private MarketCallbacks() {
    }

    static String list(int page) {
        return PREFIX + ":list:" + page;
    }

    static String listing(long listingId) {
        return PREFIX + ":l:" + listingId;
    }

    static String photos(long listingId) {
        return PREFIX + ":ph:" + listingId;
    }

    /** Starts an offer: asks for the amount. */
    static String offer(long listingId) {
        return PREFIX + ":of:" + listingId;
    }

    /** "I take it at the asking price": goes on to the note. */
    static String atAskingPrice(long listingId) {
        return PREFIX + ":ask:" + listingId;
    }

    /** Sends the offer without a note. */
    static String skipNote(long listingId) {
        return PREFIX + ":skip:" + listingId;
    }
}
