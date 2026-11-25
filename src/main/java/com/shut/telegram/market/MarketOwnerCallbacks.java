package com.shut.telegram.market;

/** Callback data for the seller's side: their listings, photos, status and the offers on them. */
final class MarketOwnerCallbacks {

    static final String PREFIX = "mown";
    static final String MINE = PREFIX + ":mine";
    static final String NEW = PREFIX + ":new";

    private MarketOwnerCallbacks() {
    }

    static String manage(long id) {
        return PREFIX + ":m:" + id;
    }

    static String edit(long id, ListingField field) {
        return PREFIX + ":e:" + id + ":" + field.name();
    }

    /** The condition picked while creating a listing (the title and price wait in the conversation). */
    static String newCondition(String conditionKey) {
        return PREFIX + ":nc:" + conditionKey;
    }

    static String pickCondition(long id) {
        return PREFIX + ":cond:" + id;
    }

    static String setCondition(long id, String conditionKey) {
        return PREFIX + ":sc:" + id + ":" + conditionKey;
    }

    static String pickCategory(long id) {
        return PREFIX + ":cat:" + id;
    }

    /** {@code categoryId} 0 clears the category. */
    static String setCategory(long id, long categoryId) {
        return PREFIX + ":sk:" + id + ":" + categoryId;
    }

    static String toggleNegotiable(long id) {
        return PREFIX + ":ng:" + id;
    }

    static String setStatus(long id, String statusKey) {
        return PREFIX + ":ss:" + id + ":" + statusKey;
    }

    static String addPhotos(long id) {
        return PREFIX + ":ph:" + id;
    }

    static String clearPhotos(long id) {
        return PREFIX + ":phx:" + id;
    }

    static String offers(long id) {
        return PREFIX + ":of:" + id;
    }

    static String accept(long offerId) {
        return PREFIX + ":oa:" + offerId;
    }

    static String decline(long offerId) {
        return PREFIX + ":od:" + offerId;
    }

    static String delete(long id) {
        return PREFIX + ":del:" + id;
    }

    static String confirmDelete(long id) {
        return PREFIX + ":delok:" + id;
    }
}
