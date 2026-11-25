package com.shut.api.admin;

import com.shut.auth.ForbiddenException;
import com.shut.auth.InvalidPhoneException;
import com.shut.auth.PhoneNumbers;
import com.shut.auth.SessionService;
import com.shut.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Who may use the admin API: the signed-in accounts whose phone number is in {@code app.admin.phones} (comma
 * separated, any usual way of writing a number). Nobody is an administrator while the list is empty.
 */
@Component
public class AdminAccess {

    private final SessionService sessions;
    private final UserService users;
    private final Set<String> phones;

    public AdminAccess(SessionService sessions, UserService users, @Value("${app.admin.phones:}") String phones) {
        this.sessions = sessions;
        this.users = users;
        this.phones = Arrays.stream(phones.split("[,;\\s]+"))
                .filter(entry -> !entry.isBlank())
                .map(AdminAccess::normalized)
                .filter(entry -> entry != null)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @return the id of the administrator
     * @throws com.shut.auth.UnauthorizedException if nobody is signed in
     * @throws ForbiddenException                  if the account is not an administrator
     */
    public Long userId(String authorization) {
        Long userId = sessions.requireUserId(authorization);
        String phone = users.findById(userId).getPhoneNumber();
        if (phone == null || !phones.contains(phone)) {
            throw new ForbiddenException("Administrators only");
        }
        return userId;
    }

    private static String normalized(String raw) {
        try {
            return PhoneNumbers.normalize(raw);
        } catch (InvalidPhoneException e) {
            return null;
        }
    }
}
