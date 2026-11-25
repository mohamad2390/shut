package com.shut.auth;

import com.shut.common.text.PersianDigits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;

/** One-time codes for signing in with a phone number. */
@Service
public class OtpService {

    static final Duration LIFETIME = Duration.ofMinutes(2);
    static final Duration MIN_GAP = Duration.ofSeconds(60);
    static final int MAX_PER_HOUR = 5;
    static final int MAX_WRONG_ATTEMPTS = 5;

    private final OtpCodeRepository codes;
    private final SmsSender sms;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpCodeRepository codes, SmsSender sms, Clock clock) {
        this.codes = codes;
        this.sms = sms;
        this.clock = clock;
    }

    /**
     * Sends a new code to the phone.
     *
     * @param phone normalized, see {@link PhoneNumbers}
     * @throws TooManyOtpRequestsException if one was sent less than a minute ago or five were sent in the last hour
     */
    @Transactional
    public void request(String phone) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        codes.findFirstByPhoneOrderByIdDesc(phone).ifPresent(last -> {
            if (last.getCreatedAt().isAfter(now.minus(MIN_GAP))) {
                throw new TooManyOtpRequestsException("Wait a minute before asking for another code");
            }
        });
        if (codes.countByPhoneAndCreatedAtAfter(phone, now.minusHours(1)) >= MAX_PER_HOUR) {
            throw new TooManyOtpRequestsException("Too many codes were requested; try again later");
        }
        String code = "%06d".formatted(random.nextInt(1_000_000));
        codes.save(new OtpCode(phone, hash(phone, code), now.plus(LIFETIME), now));
        sms.send(phone, code);
    }

    /**
     * Checks the code and uses it up. A wrong try is counted even though the call fails, so five wrong tries burn
     * the code.
     *
     * @throws InvalidOtpException if the code is wrong, expired, used or burnt
     */
    @Transactional(noRollbackFor = InvalidOtpException.class)
    public void verify(String phone, String code) {
        OtpCode latest = codes.findFirstByPhoneAndUsedFalseOrderByIdDesc(phone).orElseThrow(InvalidOtpException::new);
        if (latest.getExpiresAt().isBefore(OffsetDateTime.now(clock)) || latest.getAttempts() >= MAX_WRONG_ATTEMPTS) {
            throw new InvalidOtpException();
        }
        latest.recordAttempt();
        String given = code == null ? "" : PersianDigits.toAscii(code).trim();
        if (!Hashing.same(latest.getCodeHash(), hash(phone, given))) {
            throw new InvalidOtpException();
        }
        latest.markUsed();
    }

    private static String hash(String phone, String code) {
        return Hashing.sha256(phone + ":" + code);
    }
}
