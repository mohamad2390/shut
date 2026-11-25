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
public class AdminListingController {

    private static final String AUTH = "Authorization";

    private final AdminAccess access;
    private final AdminListingApiService listings;

    public AdminListingController(AdminAccess access, AdminListingApiService listings) {
        this.access = access;
        this.listings = listings;
    }

    @GetMapping(ApiPaths.Admin.LISTINGS)
    public PageResponse<AdminListingItem> list(@RequestHeader(value = AUTH, required = false) String authorization,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "0") int size) {
        access.userId(authorization);
        return listings.list(page, size);
    }

    @PutMapping(ApiPaths.Admin.LISTING_HIDDEN)
    public AdminListingItem setHidden(@RequestHeader(value = AUTH, required = false) String authorization,
                                      @PathVariable Long id, @RequestBody HiddenRequest request) {
        access.userId(authorization);
        return listings.setHidden(id, request.hidden());
    }
}
