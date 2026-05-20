package com.siide.linkup.core.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Single {@link Clock} bean used by services that need time. Centralising it makes the
 * codebase deterministic in tests (override with {@code Clock.fixed(...)}).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
