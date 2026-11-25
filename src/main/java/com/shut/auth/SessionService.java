package com.shut.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;

/** Sign-ins for the web app: an opaque token in the {@code Authorization: Bearer} header. */
@Service
public class SessionService {

    static final Duration LIFETIME = Duration.ofDays(30);
    private static final String BEARER = "Bearer ";

    /** A new sign-in. {@code token} is shown once; only its hash is stored. */
    public record Issued(String token, OffsetDateTime expiresAt) {
    }

    private final WebSessionRepository sessions;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public SessionService(WebSessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional
    public Issued create(Long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime expiresAt = now.plus(LIFETIME);
        sessions.save(new WebSession(userId, Hashing.sha256(token), now, expiresAt));
        return new Issued(token, expiresAt);
    }

    /**
     * The signed-in user of a request.
     *
     * @param authorization the {@code Authorization} header, or null
     * @throws UnauthorizedException if it is missing, unknown or expired
     */
    @Transactional(readOnly = true)
    public Long requireUserId(String authorization) {
        return find(authorization).getUserId();
    }

    /** Ends every sign-in of the account, for instance when it is blocked. */
    @Transactional
    public void revokeAll(Long userId) {
        sessions.deleteAllByUserId(userId);
    }

    /** Ends this sign-in. Signing out twice is fine. */
    @Transactional
    public void revoke(String authorization) {
        sessions.delete(find(authorization));
    }

    private WebSession find(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            throw new UnauthorizedException("Sign in first");
        }
        String token = authorization.substring(BEARER.length()).trim();
        WebSession session = sessions.findByTokenHash(Hashing.sha256(token))
                .orElseThrow(() -> new UnauthorizedException("Sign in first"));
        if (session.getExpiresAt().isBefore(OffsetDateTime.now(clock))) {
            throw new UnauthorizedException("The sign-in has expired");
        }
        return session;
    }
}
