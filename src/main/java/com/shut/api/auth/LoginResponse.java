package com.shut.api.auth;

import java.time.OffsetDateTime;

/** Send {@code token} as {@code Authorization: Bearer <token>}. It is shown only once. */
public record LoginResponse(String token, OffsetDateTime expiresAt, MeResponse user) {
}
