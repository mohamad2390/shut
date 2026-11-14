package com.shut.telegram.profile;

/** Callback data for the profile screen buttons. */
final class ProfileCallbacks {

    static final String PREFIX = "profile";
    static final String CANCEL = PREFIX + ":cancel";

    private ProfileCallbacks() {
    }

    /** Ends the skills page and goes back to the profile. */
    static final String DONE = PREFIX + ":done";

    /** Adds the service to the skills, or takes it out if it is already one. */
    static String skill(long serviceId) {
        return PREFIX + ":skill:" + serviceId;
    }

    static String edit(ProfileField field) {
        return PREFIX + ":edit:" + field.name();
    }
}
