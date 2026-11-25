package com.shut.market;

/** An offer that cannot be made. */
public class MarketException extends RuntimeException {

    public enum Reason { LISTING_NOT_AVAILABLE, OWN_LISTING, ALREADY_OFFERED, INVALID_AMOUNT }

    private final Reason reason;

    public MarketException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
