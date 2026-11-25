package com.shut.api.admin;

import com.shut.api.PageResponse;
import com.shut.common.web.ApiPaths;
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

@RestController
@RequestMapping(ApiPaths.Admin.BASE)
public class AdminUserController {

    private static final String AUTH = "Authorization";

    private final AdminAccess access;
    private final AdminUserApiService users;

    public AdminUserController(AdminAccess access, AdminUserApiService users) {
        this.access = access;
        this.users = users;
    }

    /** 204 for an administrator, 403 for anybody else: lets the web app decide whether to show the panel. */
    @GetMapping(ApiPaths.Admin.ME)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void me(@RequestHeader(value = AUTH, required = false) String authorization) {
        access.userId(authorization);
    }

    @GetMapping(ApiPaths.Admin.USERS)
    public PageResponse<AdminUserItem> list(@RequestHeader(value = AUTH, required = false) String authorization,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "0") int size) {
        access.userId(authorization);
        return users.list(q, page, size);
    }

    @PutMapping(ApiPaths.Admin.USER_ACTIVE)
    public AdminUserItem setActive(@RequestHeader(value = AUTH, required = false) String authorization,
                                   @PathVariable Long id, @RequestBody ActiveRequest request) {
        return users.setActive(access.userId(authorization), id, request.active());
    }
}
