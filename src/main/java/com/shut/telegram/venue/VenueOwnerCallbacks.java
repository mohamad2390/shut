package com.shut.telegram.venue;

import com.shut.common.calendar.JalaliMonth;

import java.time.LocalDate;

/** Callback data for the owner's side: managing venues, their calendar and booking requests. */
final class VenueOwnerCallbacks {

    static final String PREFIX = "vown";
    static final String MINE = PREFIX + ":mine";
    static final String NEW = PREFIX + ":new";

    private VenueOwnerCallbacks() {
    }

    static String manage(long venueId) {
        return PREFIX + ":m:" + venueId;
    }

    static String edit(long venueId, VenueField field) {
        return PREFIX + ":e:" + venueId + ":" + field.name();
    }

    static String pickType(long venueId) {
        return PREFIX + ":type:" + venueId;
    }

    static String setType(long venueId, String typeKey) {
        return PREFIX + ":st:" + venueId + ":" + typeKey;
    }

    static String prices(long venueId) {
        return PREFIX + ":pr:" + venueId;
    }

    static String addPrice(long venueId) {
        return PREFIX + ":pa:" + venueId;
    }

    static String priceUnit(long venueId, String unitKey) {
        return PREFIX + ":pu:" + venueId + ":" + unitKey;
    }

    static String removePrice(long priceId) {
        return PREFIX + ":px:" + priceId;
    }

    static String addPhotos(long venueId) {
        return PREFIX + ":ph:" + venueId;
    }

    static String clearPhotos(long venueId) {
        return PREFIX + ":phx:" + venueId;
    }

    static String calendar(long venueId) {
        return PREFIX + ":oc:" + venueId;
    }

    static String calendar(long venueId, JalaliMonth month) {
        return PREFIX + ":oc:" + venueId + ":" + month.year() + ":" + month.month();
    }

    static String day(long venueId, LocalDate day) {
        return PREFIX + ":od:" + venueId + ":" + day;
    }

    static String requests(long venueId) {
        return PREFIX + ":rq:" + venueId;
    }

    static String accept(long bookingId) {
        return PREFIX + ":ba:" + bookingId;
    }

    static String decline(long bookingId) {
        return PREFIX + ":bd:" + bookingId;
    }

    static String toggle(long venueId) {
        return PREFIX + ":tg:" + venueId;
    }

    static String delete(long venueId) {
        return PREFIX + ":del:" + venueId;
    }

    static String confirmDelete(long venueId) {
        return PREFIX + ":delok:" + venueId;
    }
}
