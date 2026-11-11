package com.shut.project;

public class BidRefusedException extends RuntimeException {

    private final BidRefusal reason;

    public BidRefusedException(BidRefusal reason) {
        super("Bid refused: " + reason);
        this.reason = reason;
    }

    public BidRefusal reason() {
        return reason;
    }
}
