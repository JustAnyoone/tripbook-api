package com.tt.tripbook.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// "Today" comes from a Clock bean instead of LocalDate.now(), so tests can freeze the date
// and the date rules never depend on the day the tests happen to run.
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
