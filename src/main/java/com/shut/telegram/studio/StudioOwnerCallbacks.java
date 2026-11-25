package com.shut.telegram.studio;

/** Callback data for the studio owner's side. A member has one studio, so none of these carry its id. */
final class StudioOwnerCallbacks {

    static final String PREFIX = "sown";
    static final String MINE = PREFIX + ":mine";
    static final String NEW = PREFIX + ":new";
    static final String TOGGLE = PREFIX + ":tg";
    static final String OFFERINGS = PREFIX + ":of";
    static final String ADD_OFFERING = PREFIX + ":oa";
    static final String MEDIA = PREFIX + ":md";
    static final String ADD_MEDIA = PREFIX + ":ma";
    static final String CLEAR_MEDIA = PREFIX + ":mc";
    static final String PEOPLE = PREFIX + ":pp";
    static final String INVITE = PREFIX + ":iv";
    static final String DELETE = PREFIX + ":del";
    static final String CONFIRM_DELETE = PREFIX + ":delok";

    private StudioOwnerCallbacks() {
    }

    static String edit(StudioField field) {
        return PREFIX + ":e:" + field.name();
    }

    static String removeOffering(long offeringId) {
        return PREFIX + ":orm:" + offeringId;
    }

    static String removePerson(long userId) {
        return PREFIX + ":prm:" + userId;
    }
}
