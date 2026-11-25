package com.shut.api.me.venue;

import com.shut.api.me.MemberAccess;
import com.shut.api.venue.CalendarDayResponse;
import com.shut.api.me.CalendarDayRequest;
import com.shut.common.web.ApiPaths;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** The signed-in member's own venues. Photos and deleting a venue are only possible in the Telegram bot. */
@RestController
@RequestMapping(ApiPaths.Me.BASE)
public class MyVenueController {

    private static final String AUTH = "Authorization";

    private final MemberAccess access;
    private final MyVenueApiService venues;

    public MyVenueController(MemberAccess access, MyVenueApiService venues) {
        this.access = access;
        this.venues = venues;
    }

    @GetMapping(ApiPaths.Me.VENUES)
    public List<MyVenueSummary> list(@RequestHeader(value = AUTH, required = false) String authorization) {
        return venues.list(access.userId(authorization));
    }

    @PostMapping(ApiPaths.Me.VENUES)
    @ResponseStatus(HttpStatus.CREATED)
    public MyVenueDetails create(@RequestHeader(value = AUTH, required = false) String authorization,
                                 @RequestBody CreateVenueRequest request) {
        return venues.create(access.userId(authorization), request);
    }

    @GetMapping(ApiPaths.Me.VENUE)
    public MyVenueDetails details(@RequestHeader(value = AUTH, required = false) String authorization,
                                  @PathVariable Long id) {
        return venues.details(access.userId(authorization), id);
    }

    @PutMapping(ApiPaths.Me.VENUE)
    public MyVenueDetails update(@RequestHeader(value = AUTH, required = false) String authorization,
                                 @PathVariable Long id, @RequestBody UpdateVenueRequest request) {
        return venues.update(access.userId(authorization), id, request);
    }

    @PostMapping(ApiPaths.Me.VENUE_PRICES)
    @ResponseStatus(HttpStatus.CREATED)
    public MyVenueDetails addPrice(@RequestHeader(value = AUTH, required = false) String authorization,
                                   @PathVariable Long id, @RequestBody VenuePriceRequest request) {
        return venues.addPrice(access.userId(authorization), id, request);
    }

    @DeleteMapping(ApiPaths.Me.VENUE_PRICE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePrice(@RequestHeader(value = AUTH, required = false) String authorization,
                            @PathVariable Long id, @PathVariable Long priceId) {
        venues.removePrice(access.userId(authorization), id, priceId);
    }

    @GetMapping(ApiPaths.Me.VENUE_CALENDAR)
    public List<CalendarDayResponse> calendar(
            @RequestHeader(value = AUTH, required = false) String authorization, @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return venues.calendar(access.userId(authorization), id, from, to);
    }

    @PutMapping(ApiPaths.Me.VENUE_CALENDAR_DAY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mark(@RequestHeader(value = AUTH, required = false) String authorization, @PathVariable Long id,
                     @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                     @RequestBody CalendarDayRequest request) {
        venues.mark(access.userId(authorization), id, date, request.status());
    }

    @GetMapping(ApiPaths.Me.VENUE_REQUESTS)
    public List<OwnerRequestResponse> requests(@RequestHeader(value = AUTH, required = false) String authorization,
                                               @PathVariable Long id) {
        return venues.requests(access.userId(authorization), id);
    }

    @PutMapping(ApiPaths.Me.VENUE_REQUEST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void answer(@RequestHeader(value = AUTH, required = false) String authorization, @PathVariable Long id,
                       @PathVariable Long bookingId, @RequestBody AnswerRequest request) {
        venues.answer(access.userId(authorization), id, bookingId, request.accept());
    }
}
