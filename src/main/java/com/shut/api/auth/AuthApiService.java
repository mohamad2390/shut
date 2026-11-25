package com.shut.api.auth;

import com.shut.auth.OtpService;
import com.shut.auth.PhoneNumbers;
import com.shut.auth.SessionService;
import com.shut.auth.WebAccountService;
import com.shut.user.service.UserService;
import org.springframework.stereotype.Service;

/** Signing in to the web app: a code by SMS, then a token. */
@Service
public class AuthApiService {

    private final OtpService otp;
    private final WebAccountService accounts;
    private final SessionService sessions;
    private final UserService users;

    public AuthApiService(OtpService otp, WebAccountService accounts, SessionService sessions, UserService users) {
        this.otp = otp;
        this.accounts = accounts;
        this.sessions = sessions;
        this.users = users;
    }

    public void requestCode(String phone) {
        otp.request(PhoneNumbers.normalize(phone));
    }

    /** Checks the code, finds or creates the account of that phone and signs it in. */
    public LoginResponse login(String phone, String code) {
        String normalized = PhoneNumbers.normalize(phone);
        otp.verify(normalized, code);
        var user = accounts.accountFor(normalized);
        SessionService.Issued issued = sessions.create(user.getId());
        return new LoginResponse(issued.token(), issued.expiresAt(), MeResponse.of(user));
    }

    public MeResponse me(String authorization) {
        return MeResponse.of(users.findById(sessions.requireUserId(authorization)));
    }

    public void logout(String authorization) {
        sessions.revoke(authorization);
    }
}
