package com.shut.telegram.venue;

import com.shut.venue.VenueBooking;

/** A booking request with the name of the member who made it. */
record RequestLine(VenueBooking booking, String requester) {
}
