package com.shut.telegram.pricing;

import com.shut.pricing.PricingUnit;

/** Callback data for the price menu buttons. Format: {@code pricing:<action>[:<argument>]}. */
final class PricingCallbacks {

    static final String PREFIX = "pricing";
    static final String ADD = PREFIX + ":add";
    static final String MANAGE = PREFIX + ":manage";
    static final String CANCEL = PREFIX + ":cancel";
    static final String EDIT = PREFIX + ":edit";
    /** Skips the optional name of a price. */
    static final String SKIP_NAME = PREFIX + ":skip";

    private PricingCallbacks() {
    }

    /** The member picked a service from the list. */
    static String service(long serviceId) {
        return PREFIX + ":svc:" + serviceId;
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
