package com.shut.api.admin;

import com.shut.api.PageResponse;
import com.shut.common.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.Admin.BASE)
public class AdminVenueController {

    private static final String AUTH = "Authorization";

    private final AdminAccess access;
    private final AdminVenueApiService venues;

    public AdminVenueController(AdminAccess access, AdminVenueApiService venues) {
        this.access = access;
        this.venues = venues;
    }

    @GetMapping(ApiPaths.Admin.VENUES)
    public PageResponse<AdminVenueItem> list(@RequestHeader(value = AUTH, required = false) String authorization,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "0") int size) {
        access.userId(authorization);
        return venues.list(page, size);
    }

    @PutMapping(ApiPaths.Admin.VENUE_ACTIVE)
    public AdminVenueItem setActive(@RequestHeader(value = AUTH, required = false) String authorization,
                                    @PathVariable Long id, @RequestBody ActiveRequest request) {
        access.userId(authorization);
        return venues.setActive(id, request.active());
    }
}
