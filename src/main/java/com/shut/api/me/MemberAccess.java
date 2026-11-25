package com.shut.api.me;

import com.shut.auth.ForbiddenException;
import com.shut.auth.SessionService;
import com.shut.user.service.UserService;
import org.springframework.stereotype.Component;

/** Who is making a request, from its {@code Authorization} header. */
@Component
public class MemberAccess {

    private final SessionService sessions;
    private final UserService users;

    public MemberAccess(SessionService sessions, UserService users) {
        this.sessions = sessions;
        this.users = users;
    }

    /** @throws com.shut.auth.UnauthorizedException if nobody is signed in */
    public Long userId(String authorization) {
        return sessions.requireUserId(authorization);
    }

    /**
     * Like {@link #userId} but the member must have a specialist profile, as the bot also requires for the calendar
     * and the price menu.
     *
     * @throws ForbiddenException if they do not
     */
    public Long providerId(String authorization) {
        Long userId = userId(authorization);
        if (!users.findById(userId).isProvider()) {
            throw new ForbiddenException("Create your specialist profile first");
        }
        return userId;
    }
}
