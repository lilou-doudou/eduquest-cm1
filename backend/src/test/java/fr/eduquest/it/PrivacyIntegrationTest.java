package fr.eduquest.it;

import fr.eduquest.privacy.RetentionJob;
import fr.eduquest.support.ApiClient;
import fr.eduquest.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Recette 10 : export complet sans secret, suppression atomique, purges avec horloge contrôlée. */
class PrivacyIntegrationTest extends IntegrationTest {
    @Autowired
    RetentionJob retention;

    @Test
    void exportContainsFamilyDataButNoSecret() {
        Family f = newFamily("export");
        playMission(f.client(), "MATHS-1-1", 4);
        ApiClient parent = loginParent(f.email());
        ApiClient.Response r = parent.get("/privacy/export");
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.raw().headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        JsonNode body = r.body();
        assertThat(body.path("formatVersion").asInt()).isEqualTo(1);
        assertThat(body.path("account").path("email").asString()).isEqualTo(f.email());
        JsonNode child = body.path("children").get(0);
        assertThat(child.path("attempts").get(0).path("answers").size()).isEqualTo(5);
        assertThat(child.path("missionProgress").get(0).path("xp_awarded").asInt()).isEqualTo(60);
        assertThat(child.path("rewards").toString()).contains("FIRST_STEP");
        String raw = r.raw().body();
        assertThat(raw).doesNotContain("password").doesNotContain("$argon2").doesNotContain("token_hash").doesNotContain("answer_key");
    }

    @Test
    void exportAndDeletionRequireRecentReauthentication() {
        String email = uniqueEmail("reauth");
        createVerifiedParent(email);
        ApiClient p = loginParent(email);
        clock.advance(Duration.ofMinutes(6));
        try {
            assertThat(p.get("/privacy/export").code()).isEqualTo("REAUTHENTICATION_REQUIRED");
            assertThat(p.delete("/account", Map.of("confirmation", "SUPPRIMER")).code()).isEqualTo("REAUTHENTICATION_REQUIRED");
        } finally {
            clock.advance(Duration.ofMinutes(-6));
        }
    }

    @Test
    void accountDeletionRemovesEverythingOfThatFamilyOnly() {
        Family a = newFamily("suppression-a");
        Family b = newFamily("suppression-b");
        playMission(a.client(), "MATHS-1-1", 5);
        playMission(b.client(), "MATHS-1-1", 5);
        ApiClient childSessionA = a.client();
        ApiClient parentA = loginParent(a.email());
        assertThat(parentA.delete("/account", Map.of("confirmation", "non")).code()).isEqualTo("CONFIRMATION_REQUIRED");
        assertThat(parentA.delete("/account", Map.of("confirmation", "SUPPRIMER")).status()).isEqualTo(204);

        UUID child = a.childId();
        for (String table : new String[]{"child_profile WHERE id", "attempt WHERE child_id", "mission_progress WHERE child_id",
                "xp_event WHERE child_id", "child_reward WHERE child_id"}) {
            assertThat(jdbc.sql("SELECT count(*) FROM " + table + " = :c").param("c", child).query(Long.class).single()).as(table).isZero();
        }
        assertThat(jdbc.sql("SELECT count(*) FROM parent_account WHERE id = :p").param("p", a.parentId()).query(Long.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM account_token WHERE parent_id = :p").param("p", a.parentId()).query(Long.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM spring_session WHERE principal_name = :p").param("p", a.parentId().toString()).query(Long.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM audit_event WHERE actor_pseudonymous_ref = :p").param("p", a.parentId().toString()).query(Long.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM deletion_record WHERE subject_id = :p").param("p", a.parentId()).query(Long.class).single()).isEqualTo(1);
        assertThat(childSessionA.get("/worlds").status()).as("session enfant révoquée").isEqualTo(401);
        assertThat(parentA.post("/auth/login", Map.of("email", a.email(), "password", PASSWORD)).status()).isEqualTo(401);

        assertThat(b.client().get("/me/progress").body().path("xpTotal").asInt()).as("autre famille intacte").isEqualTo(70);
    }

    @Test
    void inactiveFamiliesAreNotifiedThenDeleted() {
        String email = uniqueEmail("inactif");
        UUID parent = createVerifiedParent(email);
        int before = mail.sent.size();
        clock.advance(Duration.ofDays(366));
        try {
            retention.run();
            assertThat(mail.sent.subList(before, mail.sent.size())).anyMatch(m -> java.util.List.of(m.getTo()).contains(email));
            assertThat(jdbc.sql("SELECT count(*) FROM parent_account WHERE id = :p").param("p", parent).query(Long.class).single()).isEqualTo(1);
            clock.advance(Duration.ofDays(31));
            retention.run();
            assertThat(jdbc.sql("SELECT count(*) FROM parent_account WHERE id = :p").param("p", parent).query(Long.class).single()).isZero();
        } finally {
            clock.advance(Duration.ofDays(-397));
        }
    }

    @Test
    void reconnectionCancelsInactivityDeletion() {
        String email = uniqueEmail("revenu");
        UUID parent = createVerifiedParent(email);
        clock.advance(Duration.ofDays(366));
        try {
            retention.run();
            loginParent(email);
            clock.advance(Duration.ofDays(31));
            retention.run();
            assertThat(jdbc.sql("SELECT count(*) FROM parent_account WHERE id = :p").param("p", parent).query(Long.class).single()).isEqualTo(1);
        } finally {
            clock.advance(Duration.ofDays(-397));
        }
    }

    @Test
    void neverVerifiedAccountsArePurgedAfterSevenDays() {
        ApiClient c = client();
        String email = uniqueEmail("jamais-verifie");
        c.post("/auth/register", Map.of("email", email, "password", PASSWORD, "noticeVersion", "2026-10-adulte-v1"));
        clock.advance(Duration.ofDays(8));
        try {
            retention.run();
            assertThat(jdbc.sql("SELECT count(*) FROM parent_account WHERE email_normalized = :e").param("e", email).query(Long.class).single()).isZero();
        } finally {
            clock.advance(Duration.ofDays(-8));
        }
    }
}
