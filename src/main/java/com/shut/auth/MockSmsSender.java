package com.shut.auth;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * A stand-in for the SMS company, used until one is bought: no SMS is sent, the code is printed in the server log.
 * To replace it, write a {@link SmsSender} for the provider, give it
 * {@code @ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "<provider>")} and set
 * {@code app.sms.provider} to that name; this one then switches itself off. Never run it in production: anyone who
 * can read the log can sign in as anyone.
 */
@Component
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "mock", matchIfMissing = true)
class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @PostConstruct
    void warn() {
        log.warn("Mock SMS is on: sign-in codes are printed here, not sent. Set app.sms.provider for production.");
    }

    @Override
    public void send(String phone, String code) {
        log.warn("MOCK SMS to {}: your SHUT code is {}", phone, code);
    }
}
