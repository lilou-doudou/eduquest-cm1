package fr.eduquest.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Refuse de démarrer en production avec des secrets absents ou connus comme valeurs de démonstration. */
@Component
@Profile("prod")
public class ProductionGuard implements ApplicationRunner {
    private static final Set<String> KNOWN_DEMO_VALUES = Set.of("", "eduquest", "eduquest-dev-only", "changeme", "password");

    private final String dbPassword;
    private final String baseUrl;
    private final boolean devSeed;

    public ProductionGuard(@Value("${spring.datasource.password:}") String dbPassword,
                           @Value("${eduquest.base-url}") String baseUrl,
                           @Value("${eduquest.dev-seed:false}") boolean devSeed) {
        this.dbPassword = dbPassword;
        this.baseUrl = baseUrl;
        this.devSeed = devSeed;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (KNOWN_DEMO_VALUES.contains(dbPassword)) {
            throw new IllegalStateException("POSTGRES_PASSWORD manquant ou valeur de démonstration en production.");
        }
        if (!baseUrl.startsWith("https://")) {
            throw new IllegalStateException("APP_BASE_URL doit être en HTTPS en production.");
        }
        if (devSeed) {
            throw new IllegalStateException("Les données de démonstration sont interdites en production.");
        }
    }
}
