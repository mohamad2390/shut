package com.shut.api.me;

import com.shut.common.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The member's own showcase, read-only: adding photos and videos is only possible in the Telegram bot. */
@RestController
@RequestMapping(ApiPaths.Me.BASE)
public class MyShowcaseController {

    private final MemberAccess access;
    private final MyShowcaseApiService showcase;

    public MyShowcaseController(MemberAccess access, MyShowcaseApiService showcase) {
        this.access = access;
        this.showcase = showcase;
    }

    @GetMapping(ApiPaths.Me.SHOWCASE)
    public MyShowcaseResponse get(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return showcase.get(access.userId(authorization));
    }
}
