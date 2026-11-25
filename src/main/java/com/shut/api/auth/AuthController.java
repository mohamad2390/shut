package com.shut.api.auth;

import com.shut.common.web.ApiPaths;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sign in with a phone number and a one-time code; no Telegram membership is needed. */
@RestController
public class AuthController {

    private final AuthApiService auth;

    public AuthController(AuthApiService auth) {
        this.auth = auth;
    }

    /** Sends a code. 204 whether or not the number already has an account. */
    @PostMapping(ApiPaths.Auth.BASE + ApiPaths.Auth.OTP_REQUEST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestCode(@RequestBody PhoneRequest request) {
        auth.requestCode(request.phone());
    }

    @PostMapping(ApiPaths.Auth.BASE + ApiPaths.Auth.OTP_VERIFY)
    public LoginResponse verify(@RequestBody VerifyRequest request) {
        return auth.login(request.phone(), request.code());
    }

    @PostMapping(ApiPaths.Auth.BASE + ApiPaths.Auth.LOGOUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.logout(authorization);
    }

    @GetMapping(ApiPaths.Me.BASE)
    public MeResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return auth.me(authorization);
    }
}
