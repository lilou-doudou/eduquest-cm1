package fr.eduquest.it;

import fr.eduquest.support.ApiClient;
import fr.eduquest.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Recette 2 : isolation entre familles et absence d'élévation depuis le mode enfant. */
class IsolationIntegrationTest extends IntegrationTest {

    @Test
    void otherFamilyGets404OnEveryRouteCarryingAnIdentifier() {
        Family a = newFamily("famille-a");
        String parentB = uniqueEmail("famille-b");
        createVerifiedParent(parentB);
        ApiClient b = loginParent(parentB);
        String childA = a.childId().toString();

        assertThat(b.get("/children/" + childA + "/progress").status()).isEqualTo(404);
        assertThat(b.patch("/children/" + childA, Map.of("nickname", "Pirate")).status()).isEqualTo(404);
        assertThat(b.delete("/children/" + childA, Map.of("confirmation", "SUPPRIMER")).status()).isEqualTo(404);
        assertThat(b.post("/auth/child-session", Map.of("childId", childA)).status()).isEqualTo(404);
        assertThat(b.get("/children").body().size()).isZero();

        // Partie de l'enfant A inaccessible à l'enfant B.
        String attemptA = a.client().post("/missions/" + missionId("MATHS-1-1") + "/attempts", Map.of()).body().path("attemptId").asString();
        String childB = createChild(b, "Bibi");
        enterChildMode(b, childB);
        assertThat(b.get("/attempts/" + attemptA).status()).isEqualTo(404);
        assertThat(b.post("/attempts/" + attemptA + "/complete", Map.of()).status()).isEqualTo(404);
        assertThat(b.post("/attempts/" + attemptA + "/abandon", Map.of()).status()).isEqualTo(404);
        String exercise = a.client().get("/attempts/" + attemptA).body().path("currentQuestion").path("id").asString();
        assertThat(b.put("/attempts/" + attemptA + "/answers/" + exercise, Map.of("answer", Map.of("number", "4205"))).status()).isEqualTo(404);
        assertThat(b.post("/attempts/" + attemptA + "/answers/" + exercise + "/hint", Map.of()).status()).isEqualTo(404);
    }

    @Test
    void childSessionCannotReachParentRoutesEvenBypassingTheFrontend() {
        Family a = newFamily("enfant-403");
        ApiClient c = a.client();
        assertThat(c.get("/children").status()).isEqualTo(403);
        assertThat(c.post("/children", Map.of("nickname", "Intrus", "avatarCode", "fox", "grade", "CM1")).status()).isEqualTo(403);
        assertThat(c.get("/children/" + a.childId() + "/progress").status()).isEqualTo(403);
        assertThat(c.get("/privacy/export").status()).isEqualTo(403);
        assertThat(c.delete("/account", Map.of("confirmation", "SUPPRIMER")).status()).isEqualTo(403);
        assertThat(c.post("/auth/child-session", Map.of("childId", a.childId().toString())).status()).isEqualTo(403);
        assertThat(c.post("/auth/reauthenticate", Map.of("password", PASSWORD)).status()).isEqualTo(403);

        ApiClient.Response me = c.get("/auth/me");
        assertThat(me.body().path("role").asString()).isEqualTo("CHILD");
        assertThat(me.body().path("parent").isNull()).as("aucune donnée parent en mode enfant").isTrue();
        assertThat(me.raw().body()).doesNotContain(a.email());
    }

    @Test
    void parentRoutesRefuseLearningRoutes() {
        String email = uniqueEmail("parent-403");
        createVerifiedParent(email);
        ApiClient p = loginParent(email);
        assertThat(p.get("/worlds").status()).isEqualTo(403);
        assertThat(p.post("/missions/" + missionId("MATHS-1-1") + "/attempts", Map.of()).status()).isEqualTo(403);
    }

    @Test
    void returnToParentRequiresOwnerCredentials() {
        Family a = newFamily("retour-parent");
        String otherEmail = uniqueEmail("autre-parent");
        createVerifiedParent(otherEmail);
        ApiClient c = a.client();
        assertThat(c.post("/auth/parent-session", Map.of("email", a.email(), "password", "mauvais mot de passe")).status()).isEqualTo(401);
        assertThat(c.post("/auth/parent-session", Map.of("email", otherEmail, "password", PASSWORD)).status())
                .as("un autre adulte ne peut pas prendre la main").isEqualTo(401);
        assertThat(c.post("/auth/parent-session", Map.of("email", a.email(), "password", PASSWORD)).status()).isEqualTo(200);
        assertThat(c.get("/children").status()).isEqualTo(200);
    }

    @Test
    void maximumFiveChildrenPerFamily() {
        String email = uniqueEmail("cinq");
        createVerifiedParent(email);
        ApiClient p = loginParent(email);
        for (int i = 0; i < 5; i++) createChild(p, "Enfant" + i);
        ApiClient.Response sixth = p.post("/children", Map.of("nickname", "Sixième", "avatarCode", "owl", "grade", "CM1"));
        assertThat(sixth.status()).isEqualTo(409);
        assertThat(sixth.code()).isEqualTo("CHILD_LIMIT_REACHED");
    }

    @Test
    void deletingAChildRequiresRecentReauthentication() {
        String email = uniqueEmail("suppr-enfant");
        createVerifiedParent(email);
        ApiClient p = loginParent(email);
        String child = createChild(p, "Temporaire");
        clock.advance(java.time.Duration.ofMinutes(6));
        try {
            ApiClient.Response r = p.delete("/children/" + child, Map.of("confirmation", "SUPPRIMER"));
            assertThat(r.code()).isEqualTo("REAUTHENTICATION_REQUIRED");
            assertThat(p.post("/auth/reauthenticate", Map.of("password", PASSWORD)).status()).isEqualTo(204);
            assertThat(p.delete("/children/" + child, Map.of("confirmation", "SUPPRIMER")).status()).isEqualTo(204);
            assertThat(p.get("/children").body().size()).isZero();
        } finally {
            clock.advance(java.time.Duration.ofMinutes(-6));
        }
    }
}
