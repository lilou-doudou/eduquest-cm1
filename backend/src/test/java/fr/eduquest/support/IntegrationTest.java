package fr.eduquest.support;

import fr.eduquest.catalog.ContentImporter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration sur un vrai PostgreSQL (Testcontainers), avec Flyway, Spring Session JDBC,
 * CSRF et Tomcat embarqué. Le conteneur est partagé par toutes les classes de test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.servlet.session.cookie.secure=false",
        "eduquest.retention.cron=-",
        "eduquest.rate-limit.ip-factor=1000",
        "eduquest.mail.from=test@eduquest.local",
        "eduquest.base-url=http://localhost:5173"
})
@Import(IntegrationTest.TestBeans.class)
public abstract class IntegrationTest {
    public static final String PASSWORD = "mot de passe très long";
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final AtomicInteger SEQ = new AtomicInteger();
    private static volatile boolean imported;

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    public static class TestBeans {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-10-21T08:00:00Z"));
        }

        @Bean
        @Primary
        CapturingMailSender capturingMailSender() {
            return new CapturingMailSender();
        }
    }

    @LocalServerPort
    protected int port;
    @Autowired
    protected JdbcClient jdbc;
    @Autowired
    protected ContentImporter importer;
    @Autowired
    protected PasswordEncoder encoder;
    @Autowired
    protected CapturingMailSender mail;
    @Autowired
    protected MutableClock clock;
    protected final JsonMapper json = JsonMapper.builder().build();

    public static final Path CONTENT = Path.of("../content");

    @BeforeEach
    void ensureContent() {
        if (!imported) {
            synchronized (IntegrationTest.class) {
                if (!imported) {
                    importer.importFrom(CONTENT);
                    imported = true;
                }
            }
        }
    }

    protected ApiClient client() {
        return new ApiClient(port);
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 6) + "@exemple.test";
    }

    /** Compte parent vérifié créé directement en base (évite de dépendre du flux e-mail dans chaque test). */
    protected UUID createVerifiedParent(String email) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbc.sql("""
                INSERT INTO parent_account (id, email_normalized, password_hash, email_verified_at, status, created_at, last_activity_at)
                VALUES (:id, :e, :h, :now, 'ACTIVE', :now, :now)""")
                .param("id", id).param("e", email).param("h", encoder.encode(PASSWORD)).param("now", now).update();
        return id;
    }

    protected ApiClient loginParent(String email) {
        ApiClient c = client();
        ApiClient.Response r = c.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        assertThat(r.status()).as("login %s", r.body()).isEqualTo(200);
        return c;
    }

    protected String createChild(ApiClient parent, String nickname) {
        ApiClient.Response r = parent.post("/children", Map.of("nickname", nickname, "avatarCode", "fox", "grade", "CM1"));
        assertThat(r.status()).as("création enfant %s", r.body()).isEqualTo(201);
        return r.body().path("id").asString();
    }

    protected void enterChildMode(ApiClient c, String childId) {
        ApiClient.Response r = c.post("/auth/child-session", Map.of("childId", childId));
        assertThat(r.status()).as("mode enfant %s", r.body()).isEqualTo(200);
    }

    /** Famille complète : parent vérifié, un enfant, session enfant ouverte. */
    protected Family newFamily(String prefix) {
        String email = uniqueEmail(prefix);
        UUID parentId = createVerifiedParent(email);
        ApiClient c = loginParent(email);
        String childId = createChild(c, "Explo" + SEQ.incrementAndGet());
        enterChildMode(c, childId);
        return new Family(email, parentId, UUID.fromString(childId), c);
    }

    public record Family(String email, UUID parentId, UUID childId, ApiClient client) {}

    protected String missionId(String code) {
        return jdbc.sql("SELECT id FROM mission WHERE code = :c").param("c", code).query(UUID.class).single().toString();
    }

    protected JsonNode answerKey(String exerciseId) {
        return json.readTree(jdbc.sql("SELECT answer_key::text FROM exercise WHERE id = :id")
                .param("id", UUID.fromString(exerciseId)).query(String.class).single());
    }

    protected String typeOf(String exerciseId) {
        return jdbc.sql("SELECT type FROM exercise WHERE id = :id").param("id", UUID.fromString(exerciseId)).query(String.class).single();
    }

    /** Réponse correcte (ou volontairement fausse) au format attendu par l'API. */
    protected Map<String, Object> answerFor(String exerciseId, boolean correct) {
        JsonNode key = answerKey(exerciseId);
        return switch (typeOf(exerciseId)) {
            case "MCQ" -> {
                String expected = key.path("optionId").asString();
                String wrong = expected.equals("a") ? "b" : "a";
                yield Map.of("optionId", correct ? expected : wrong);
            }
            case "NUMBER" -> Map.of("number", correct ? key.path("number").asString() : "999999");
            default -> Map.of("text", correct ? key.path("accepted").path(0).asString() : "zzz");
        };
    }

    /** Joue une mission complète avec exactement {@code correctCount} bonnes réponses ; renvoie le bilan. */
    protected JsonNode playMission(ApiClient c, String missionCode, int correctCount) {
        ApiClient.Response start = c.post("/missions/" + missionId(missionCode) + "/attempts", Map.of());
        assertThat(start.status()).as("démarrage %s", start.body()).isIn(200, 201);
        String attemptId = start.body().path("attemptId").asString();
        for (int i = 0; i < 5; i++) {
            ApiClient.Response view = c.get("/attempts/" + attemptId);
            String exerciseId = view.body().path("currentQuestion").path("id").asString();
            ApiClient.Response r = c.put("/attempts/" + attemptId + "/answers/" + exerciseId,
                    Map.of("answer", answerFor(exerciseId, i < correctCount)));
            assertThat(r.status()).as("réponse %s", r.body()).isEqualTo(200);
        }
        ApiClient.Response done = c.post("/attempts/" + attemptId + "/complete", Map.of());
        assertThat(done.status()).as("clôture %s", done.body()).isEqualTo(200);
        return done.body();
    }
}
