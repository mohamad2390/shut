package com.shut.common.time;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** The application's notion of "today" is Tehran's, since that is where its users live. */
@Configuration
class ClockConfig {

    static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.system(TEHRAN);
    }
}
