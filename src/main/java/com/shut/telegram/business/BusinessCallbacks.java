package com.shut.telegram.business;

import com.shut.business.BusinessKind;

/** Callback data for the business section: choosing a category and filling in a studio or rental card. */
public final class BusinessCallbacks {

    public static final String PREFIX = "biz";
    public static final String HOME = PREFIX + ":home";
    static final String VENUE = PREFIX + ":venue";

    private BusinessCallbacks() {
    }

    /** Opens the card of the kind, or its form if the member has none yet. */
    static String open(BusinessKind kind) {
        return PREFIX + ":k:" + kind.key();
    }

    static String edit(BusinessKind kind) {
        return PREFIX + ":edit:" + kind.key();
    }
}
