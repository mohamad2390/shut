package com.shut.api.me;

import com.shut.api.venue.CalendarDayResponse;
import com.shut.common.web.ApiPaths;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** The signed-in specialist's own calendar. Dates are Gregorian ISO dates. */
@RestController
@RequestMapping(ApiPaths.Me.BASE)
public class MyCalendarController {

    private final MemberAccess access;
    private final MyCalendarApiService calendar;

    public MyCalendarController(MemberAccess access, MyCalendarApiService calendar) {
        this.access = access;
        this.calendar = calendar;
    }

    @GetMapping(ApiPaths.Me.CALENDAR)
    public List<CalendarDayResponse> days(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return calendar.days(access.providerId(authorization), from, to);
    }

    @PutMapping(ApiPaths.Me.CALENDAR_DAY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mark(@RequestHeader(value = "Authorization", required = false) String authorization,
                     @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                     @RequestBody CalendarDayRequest request) {
        calendar.mark(access.providerId(authorization), date, request.status());
    }
}
