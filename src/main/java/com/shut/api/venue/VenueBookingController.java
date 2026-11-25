package com.shut.api.venue;

import com.shut.api.me.MemberAccess;
import com.shut.common.web.ApiPaths;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Asking for a venue, and the member's own requests. Both need a sign-in. */
@RestController
public class VenueBookingController {

    private final MemberAccess access;
    private final VenueBookingApiService bookings;

    public VenueBookingController(MemberAccess access, VenueBookingApiService bookings) {
        this.access = access;
        this.bookings = bookings;
    }

    @PostMapping(ApiPaths.Venues.BASE + ApiPaths.Venues.BOOKINGS)
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse request(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable Long id, @RequestBody BookingRequest request) {
        return bookings.request(access.userId(authorization), id, request);
    }

    @GetMapping(ApiPaths.Me.BASE + ApiPaths.Me.BOOKINGS)
    public List<BookingResponse> mine(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return bookings.mine(access.userId(authorization));
    }
}
