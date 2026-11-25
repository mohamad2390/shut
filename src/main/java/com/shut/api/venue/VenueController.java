package com.shut.api.venue;

import com.shut.api.PageResponse;
import com.shut.api.Paging;
import com.shut.common.web.ApiPaths;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Public, read-only view of the photography venues. */
@RestController
@RequestMapping(ApiPaths.Venues.BASE)
public class VenueController {

    private final VenueApiService venues;

    public VenueController(VenueApiService venues) {
        this.venues = venues;
    }

    /** Newest first. Pages count from 0. */
    @GetMapping
    public PageResponse<VenueListItem> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "0") int size) {
        return venues.list(Paging.page(page), Paging.size(size));
    }

    @GetMapping(ApiPaths.Venues.BY_ID)
    public VenueDetailsResponse details(@PathVariable Long id) {
        return venues.details(id);
    }

    /** The marked days in a range of Gregorian dates ({@code yyyy-MM-dd}), at most 93 days; default: the next 31 days. */
    @GetMapping(ApiPaths.Venues.CALENDAR)
    public List<CalendarDayResponse> calendar(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return venues.calendar(id, from, to);
    }
}
