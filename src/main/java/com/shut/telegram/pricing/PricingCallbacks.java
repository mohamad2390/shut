package com.shut.telegram.pricing;

import com.shut.pricing.PricingUnit;

/** Callback data for the price menu buttons. Format: {@code pricing:<action>[:<argument>]}. */
final class PricingCallbacks {

    static final String PREFIX = "pricing";
    static final String ADD = PREFIX + ":add";
    static final String MANAGE = PREFIX + ":manage";
    static final String CANCEL = PREFIX + ":cancel";

    private PricingCallbacks() {
    }

    static String unit(PricingUnit unit) {
        return PREFIX + ":unit:" + unit.name();
    }

    static String delete(long itemId) {
        return PREFIX + ":del:" + itemId;
    }
}
