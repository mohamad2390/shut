package com.shut.venue;

/** There is no venue with this id that other members can see. */
public class VenueNotFoundException extends RuntimeException {

    public VenueNotFoundException(Long venueId) {
        super("Venue not found: " + venueId);
    }
}
