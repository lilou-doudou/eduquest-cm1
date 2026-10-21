package fr.eduquest.family;

import fr.eduquest.auth.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Famille fictive pour le développement local uniquement (profil « dev » ET eduquest.dev-seed=true).
 * Ne s'exécute jamais en production (ProductionGuard refuse aussi eduquest.dev-seed).
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "eduquest.dev-seed", havingValue = "true")
public class DevSeedRunner implements ApplicationRunner {
    public static final String DEMO_EMAIL = "parent.demo@eduquest.local";
    public static final String DEMO_PASSWORD = "demo-eduquest-local";
    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    private final JdbcClient jdbc;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public DevSeedRunner(JdbcClient jdbc, PasswordEncoder encoder, Clock clock) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean exists = jdbc.sql("SELECT count(*) FROM parent_account WHERE email_normalized = :e")
                .param("e", AuthService.normalizeEmail(DEMO_EMAIL)).query(Long.class).single() > 0;
        if (exists) return;
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        UUID parentId = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO parent_account (id, email_normalized, password_hash, email_verified_at, status, created_at, last_activity_at)
                VALUES (:id, :e, :h, :now, 'ACTIVE', :now, :now)""")
                .param("id", parentId).param("e", DEMO_EMAIL).param("h", encoder.encode(DEMO_PASSWORD)).param("now", now).update();
        String[][] kids = {{"Nova", "fox"}, {"Tiago", "owl"}};
        for (String[] kid : kids) {
            jdbc.sql("""
                    INSERT INTO child_profile (id, parent_id, nickname, avatar_code, grade, created_at, updated_at)
                    VALUES (:id, :p, :n, :a, 'CM1', :now, :now)""")
                    .param("id", UUID.randomUUID()).param("p", parentId).param("n", kid[0]).param("a", kid[1]).param("now", now).update();
        }
        log.info("Famille de démonstration créée (développement local uniquement) : {}", DEMO_EMAIL);
    }
}
