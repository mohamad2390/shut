package com.shut.pricing;

/** The user already has the maximum number of services on their price menu. */
public class PriceLimitReachedException extends RuntimeException {

    public PriceLimitReachedException() {
        super("Price menu is full");
    }
}
