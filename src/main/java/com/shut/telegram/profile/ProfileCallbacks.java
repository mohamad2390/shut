package com.shut.telegram.profile;

/** Callback data for the profile screen buttons. */
final class ProfileCallbacks {

    static final String PREFIX = "profile";
    static final String CANCEL = PREFIX + ":cancel";

    private ProfileCallbacks() {
    }

    static String edit(ProfileField field) {
        return PREFIX + ":edit:" + field.name();
    }
}
