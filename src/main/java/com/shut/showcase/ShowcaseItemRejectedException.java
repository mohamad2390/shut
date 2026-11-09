package com.shut.showcase;

/** The item cannot be added to the portfolio. {@link #reason()} says why. */
public class ShowcaseItemRejectedException extends RuntimeException {

    public enum Reason { DUPLICATE, LIMIT_REACHED }

    private final Reason reason;

    public ShowcaseItemRejectedException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
