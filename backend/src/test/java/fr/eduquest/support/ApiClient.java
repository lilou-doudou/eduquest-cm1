package fr.eduquest.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Client HTTP réel (Tomcat embarqué) avec cookies et jeton CSRF, comme un navigateur. */
public class ApiClient {
    public record Response(int status, JsonNode body, HttpResponse<String> raw) {
        public String code() { return body == null ? null : body.path("code").asString(); }
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final String base;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient http;
    private String csrfHeader;
    private String csrfToken;

    public ApiClient(int port) {
        this.base = "http://localhost:" + port + "/api/v1";
        this.http = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
    }

    public CookieManager cookies() { return cookies; }

    public void refreshCsrf() {
        Response r = send("GET", "/auth/csrf", null, false);
        csrfHeader = r.body().path("headerName").asString();
        csrfToken = r.body().path("token").asString();
    }

    public Response get(String path) { return send("GET", path, null, false); }
    public Response post(String path, Object body) { return send("POST", path, body, true); }
    public Response put(String path, Object body) { return send("PUT", path, body, true); }
    public Response patch(String path, Object body) { return send("PATCH", path, body, true); }
    public Response delete(String path, Object body) { return send("DELETE", path, body, true); }

    /** Mutation sans en-tête CSRF (pour vérifier le refus). */
    public Response postWithoutCsrf(String path, Object body) {
        return exchange("POST", path, body, false);
    }

    private Response send(String method, String path, Object body, boolean mutation) {
        if (mutation && csrfToken == null) refreshCsrf();
        Response r = exchange(method, path, body, mutation);
        if (mutation && r.status() == 403 && "CSRF_INVALID".equals(r.code())) {
            refreshCsrf();
            r = exchange(method, path, body, true);
        }
        return r;
    }

    private Response exchange(String method, String path, Object body, boolean withCsrf) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).header("Accept", "application/json");
            if (withCsrf && csrfToken != null) b.header(csrfHeader, csrfToken);
            String payload = body == null ? null : (body instanceof String s ? s : JSON.writeValueAsString(body));
            if (payload != null) {
                b.header("Content-Type", "application/json");
                b.method(method, HttpRequest.BodyPublishers.ofString(payload));
            } else {
                b.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode node = res.body() == null || res.body().isBlank() ? null : JSON.readTree(res.body());
            return new Response(res.statusCode(), node, res);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
