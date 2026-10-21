package fr.eduquest.it;

import fr.eduquest.support.ApiClient;
import fr.eduquest.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Recette 1 (compte), sessions, CSRF, récupération de mot de passe, limitation de débit. */
class AuthAndSecurityIntegrationTest extends IntegrationTest {

    @Test
    void registrationRequiresVerifiedEmailThenAllowsChildCreation() {
        ApiClient c = client();
        String email = uniqueEmail("inscription");
        String notice = c.get("/privacy/notice").body().path("noticeVersion").asString();

        ApiClient.Response reg = c.post("/auth/register", Map.of("email", email, "password", PASSWORD, "noticeVersion", notice));
        assertThat(reg.status()).isEqualTo(202);
        // Réponse identique pour une adresse déjà utilisée (pas d'énumération de comptes).
        assertThat(c.post("/auth/register", Map.of("email", email, "password", PASSWORD, "noticeVersion", notice)).status()).isEqualTo(202);

        ApiClient.Response unverified = c.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        assertThat(unverified.status()).isEqualTo(403);
        assertThat(unverified.code()).isEqualTo("EMAIL_NOT_VERIFIED");

        String token = mail.lastToken(email, "verifier-email").orElseThrow();
        assertThat(c.post("/auth/verify-email", Map.of("token", token)).status()).isEqualTo(204);
        assertThat(c.post("/auth/verify-email", Map.of("token", token)).code()).as("usage unique").isEqualTo("INVALID_TOKEN");

        assertThat(c.post("/auth/login", Map.of("email", email.toUpperCase(), "password", PASSWORD)).status()).isEqualTo(200);
        ApiClient.Response me = c.get("/auth/me");
        assertThat(me.body().path("role").asString()).isEqualTo("PARENT");
        assertThat(me.body().path("parent").path("emailVerified").asBoolean()).isTrue();
        createChild(c, "Lou");

        // Le jeton brut n'est jamais stocké.
        Long raw = jdbc.sql("SELECT count(*) FROM account_token WHERE token_hash = :t").param("t", token).query(Long.class).single();
        assertThat(raw).isZero();
    }

    @Test
    void passwordPolicyAndNoticeVersionAreEnforced() {
        ApiClient c = client();
        ApiClient.Response shortPwd = c.post("/auth/register", Map.of("email", uniqueEmail("pwd"), "password", "court", "noticeVersion", "2026-10-adulte-v1"));
        assertThat(shortPwd.status()).isEqualTo(400);
        assertThat(shortPwd.body().path("fieldErrors").get(0).path("field").asString()).isEqualTo("password");
        ApiClient.Response oldNotice = c.post("/auth/register", Map.of("email", uniqueEmail("pwd"), "password", PASSWORD, "noticeVersion", "ancienne"));
        assertThat(oldNotice.code()).isEqualTo("NOTICE_VERSION_MISMATCH");
    }

    @Test
    void mutationsWithoutCsrfAreRefused() {
        ApiClient c = client();
        c.refreshCsrf();
        ApiClient.Response r = c.postWithoutCsrf("/auth/login", Map.of("email", "x@y.z", "password", "whatever-whatever"));
        assertThat(r.status()).isEqualTo(403);
        assertThat(r.code()).isEqualTo("CSRF_INVALID");
    }

    @Test
    void sessionCookieIsHttpOnlyAndRenewedOnLogin() {
        String email = uniqueEmail("cookie");
        createVerifiedParent(email);
        ApiClient c = client();
        c.refreshCsrf();
        String before = sessionCookie(c);
        ApiClient.Response login = c.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        String setCookie = login.raw().headers().allValues("Set-Cookie").stream().filter(h -> h.startsWith("EQSESSION=")).findFirst().orElseThrow();
        assertThat(setCookie).contains("HttpOnly").contains("SameSite=Lax");
        assertThat(sessionCookie(c)).as("identifiant renouvelé").isNotEqualTo(before);
    }

    private static String sessionCookie(ApiClient c) {
        return c.cookies().getCookieStore().getCookies().stream().filter(k -> k.getName().equals("EQSESSION"))
                .map(java.net.HttpCookie::getValue).findFirst().orElse(null);
    }

    @Test
    void logoutRevokesServerSession() {
        String email = uniqueEmail("logout");
        createVerifiedParent(email);
        ApiClient c = loginParent(email);
        assertThat(c.post("/auth/logout", Map.of()).status()).isEqualTo(204);
        assertThat(c.get("/children").status()).isEqualTo(401);
    }

    @Test
    void passwordResetRevokesAllSessions() {
        String email = uniqueEmail("reset");
        createVerifiedParent(email);
        ApiClient other = loginParent(email);
        ApiClient c = client();
        assertThat(c.post("/auth/forgot-password", Map.of("email", email)).status()).isEqualTo(202);
        assertThat(c.post("/auth/forgot-password", Map.of("email", uniqueEmail("inconnu"))).status()).as("réponse neutre").isEqualTo(202);
        String token = mail.lastToken(email, "reinitialiser-mot-de-passe").orElseThrow();
        assertThat(c.post("/auth/reset-password", Map.of("token", token, "newPassword", "un tout nouveau mot de passe")).status()).isEqualTo(204);
        assertThat(other.get("/children").status()).as("ancienne session révoquée").isEqualTo(401);
        assertThat(c.post("/auth/login", Map.of("email", email, "password", PASSWORD)).status()).isEqualTo(401);
        assertThat(c.post("/auth/login", Map.of("email", email, "password", "un tout nouveau mot de passe")).status()).isEqualTo(200);
    }

    @Test
    void resetTokenExpiresAfterThirtyMinutes() {
        String email = uniqueEmail("expire");
        createVerifiedParent(email);
        ApiClient c = client();
        c.post("/auth/forgot-password", Map.of("email", email));
        String token = mail.lastToken(email, "reinitialiser-mot-de-passe").orElseThrow();
        clock.advance(java.time.Duration.ofMinutes(31));
        try {
            assertThat(c.post("/auth/reset-password", Map.of("token", token, "newPassword", "un tout nouveau mot de passe")).code())
                    .isEqualTo("INVALID_TOKEN");
        } finally {
            clock.advance(java.time.Duration.ofMinutes(-31));
        }
    }

    @Test
    void repeatedFailedLoginsAreRateLimitedWithRetryAfter() {
        String email = uniqueEmail("ratelimit");
        createVerifiedParent(email);
        ApiClient c = client();
        ApiClient.Response last = null;
        for (int i = 0; i < 11; i++) {
            last = c.post("/auth/login", Map.of("email", email, "password", "mauvais mot de passe"));
        }
        assertThat(last.status()).isEqualTo(429);
        assertThat(last.raw().headers().firstValue("Retry-After")).isPresent();
    }

    @Test
    void absoluteSessionLifetimeIsTwelveHours() {
        String email = uniqueEmail("absolute");
        createVerifiedParent(email);
        ApiClient c = loginParent(email);
        // Activité régulière toutes les 25 minutes : l'inactivité ne coupe pas, la limite absolue si.
        for (int i = 0; i < 29; i++) {
            clock.advance(java.time.Duration.ofMinutes(25));
        }
        try {
            assertThat(c.get("/children").status()).isEqualTo(401);
        } finally {
            clock.advance(java.time.Duration.ofMinutes(-25 * 29));
        }
    }

    @Test
    void errorsAreProblemJsonWithoutStackTrace() {
        ApiClient c = client();
        ApiClient.Response r = c.get("/children");
        assertThat(r.status()).isEqualTo(401);
        assertThat(r.raw().headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
        assertThat(r.body().has("traceId")).isTrue();
        assertThat(r.raw().body()).doesNotContain("Exception").doesNotContain("at fr.");
    }
}
