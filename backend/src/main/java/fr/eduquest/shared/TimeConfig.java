package fr.eduquest.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class TimeConfig {
    /** Horloge injectable : les tests de purge la remplacent par une horloge contrôlée. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
