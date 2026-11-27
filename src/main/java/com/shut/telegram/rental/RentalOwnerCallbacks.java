package com.shut.telegram.rental;

/** Callback data for the rental shop owner's side. A member has one shop, so only items carry an id. */
final class RentalOwnerCallbacks {

    static final String PREFIX = "rown";
    static final String MINE = PREFIX + ":mine";
    static final String NEW = PREFIX + ":new";
    static final String TOGGLE = PREFIX + ":tg";
    static final String ITEMS = PREFIX + ":its";
    static final String DELETE = PREFIX + ":del";
    static final String CONFIRM_DELETE = PREFIX + ":delok";

    private RentalOwnerCallbacks() {
    }

    static String edit(RentalField field) {
        return PREFIX + ":e:" + field.name();
    }

    static String item(long id) {
        return PREFIX + ":i:" + id;
    }

    static String toggleItem(long id) {
        return PREFIX + ":it:" + id;
    }

    static String editPrice(long id) {
        return PREFIX + ":ip:" + id;
    }

    static String addPhoto(long id) {
        return PREFIX + ":ph:" + id;
    }

    static String clearPhotos(long id) {
        return PREFIX + ":phx:" + id;
    }

    static String removeItem(long id) {
        return PREFIX + ":irm:" + id;
    }
}
