package com.shut.telegram.pricing;

import com.shut.pricing.PricingUnit;

/** Callback data for the price menu buttons. Format: {@code pricing:<action>[:<argument>]}. */
final class PricingCallbacks {

    static final String PREFIX = "pricing";
    static final String ADD = PREFIX + ":add";
    static final String MANAGE = PREFIX + ":manage";
    static final String CANCEL = PREFIX + ":cancel";
    static final String EDIT = PREFIX + ":edit";

    private PricingCallbacks() {
    }

    static String unit(PricingUnit unit) {
        return PREFIX + ":unit:" + unit.name();
    }

    /** Opens one service with its edit buttons. */
    static String item(long itemId) {
        return PREFIX + ":item:" + itemId;
    }

    static String editAmount(long itemId) {
        return PREFIX + ":amt:" + itemId;
    }

    static String editUnit(long itemId) {
        return PREFIX + ":units:" + itemId;
    }

    static String setUnit(long itemId, PricingUnit unit) {
        return PREFIX + ":su:" + itemId + ":" + unit.name();
    }

    static String delete(long itemId) {
        return PREFIX + ":del:" + itemId;
    }
}
