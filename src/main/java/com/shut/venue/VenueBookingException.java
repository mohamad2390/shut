package com.shut.venue;

/** A booking request that cannot be made, with the reason. */
public class VenueBookingException extends RuntimeException {

    /** Why the request was refused. */
    public enum Reason { VENUE_NOT_AVAILABLE, OWN_VENUE, DAY_NOT_FREE, ALREADY_REQUESTED }

    private final Reason reason;

    public VenueBookingException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
