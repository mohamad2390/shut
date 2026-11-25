package com.shut.api.admin;

import com.shut.common.web.ApiPaths;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.Admin.BASE)
public class AdminCatalogController {

    private static final String AUTH = "Authorization";

    private final AdminAccess access;
    private final AdminCatalogApiService catalog;

    public AdminCatalogController(AdminAccess access, AdminCatalogApiService catalog) {
        this.access = access;
        this.catalog = catalog;
    }

    @GetMapping(ApiPaths.Admin.SERVICES)
    public List<AdminServiceItem> services(@RequestHeader(value = AUTH, required = false) String authorization) {
        access.userId(authorization);
        return catalog.services();
    }

    @PostMapping(ApiPaths.Admin.SERVICES)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminServiceItem create(@RequestHeader(value = AUTH, required = false) String authorization,
                                   @RequestBody ServiceRequest request) {
        access.userId(authorization);
        return catalog.create(request);
    }

    @PutMapping(ApiPaths.Admin.SERVICE)
    public AdminServiceItem update(@RequestHeader(value = AUTH, required = false) String authorization,
                                   @PathVariable Long id, @RequestBody ServiceRequest request) {
        access.userId(authorization);
        return catalog.update(id, request);
    }

    @GetMapping(ApiPaths.Admin.UNITS)
    public List<AdminUnitItem> units(@RequestHeader(value = AUTH, required = false) String authorization) {
        access.userId(authorization);
        return catalog.units();
    }

    @PutMapping(ApiPaths.Admin.UNIT)
    public AdminUnitItem setUnitIcon(@RequestHeader(value = AUTH, required = false) String authorization,
                                     @PathVariable String key, @RequestBody UnitIconRequest request) {
        access.userId(authorization);
        return catalog.setUnitIcon(key, request);
    }
}
