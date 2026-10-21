package fr.eduquest.shared;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class SecurityConfig {

    /**
     * Argon2id. Paramètres : sel 16 o, hachage 32 o, parallélisme 1, mémoire 19 Mio (19456 Kio),
     * 2 itérations — valeurs minimales recommandées par l'OWASP pour Argon2id, à recalibrer sur le
     * matériel de production (cible ≈ 100–300 ms par hachage). Voir docs/security-privacy.md.
     */
    @Bean
    PasswordEncoder passwordEncoder(@Value("${eduquest.security.argon2.memory-kib:19456}") int memoryKib,
                                    @Value("${eduquest.security.argon2.iterations:2}") int iterations) {
        return new Argon2PasswordEncoder(16, 32, 1, memoryKib, iterations);
    }

    /** Aucun utilisateur Spring Security par défaut : l'authentification passe uniquement par AuthService. */
    @Bean
    UserDetailsService noDefaultUsers() {
        return username -> {
            throw new UsernameNotFoundException("désactivé");
        };
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain api(HttpSecurity http, SecurityContextRepository contextRepository,
                            CsrfTokenRepository csrfRepository, JsonMapper json, Clock clock,
                            @Value("${eduquest.session.absolute:PT12H}") Duration absolute,
                            @Value("${eduquest.security.hsts:false}") boolean hsts) throws Exception {
        http
            .securityContext(sc -> sc.securityContextRepository(contextRepository))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable())
            .requestCache(rc -> rc.disable())
            .anonymous(a -> {})
            .headers(h -> {
                h.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                                + "script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"));
                h.referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
                h.contentTypeOptions(c -> {});
                h.frameOptions(f -> f.deny());
                if (!hsts) h.httpStrictTransportSecurity(s -> s.disable());
            })
            .addFilterAfter(new AbsoluteSessionTimeoutFilter(clock, absolute), BasicAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf", "/api/v1/privacy/notice").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/verify-email",
                        "/api/v1/auth/login", "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password").permitAll()
                .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout").authenticated()
                .requestMatchers("/api/v1/auth/child-session", "/api/v1/auth/reauthenticate").hasRole("PARENT")
                .requestMatchers("/api/v1/auth/parent-session").hasRole("CHILD")
                .requestMatchers("/api/v1/children/**", "/api/v1/privacy/**", "/api/v1/account").hasRole("PARENT")
                .requestMatchers("/api/v1/worlds/**", "/api/v1/me/**", "/api/v1/missions/**", "/api/v1/attempts/**").hasRole("CHILD")
                .anyRequest().denyAll())
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((req, res, ex) ->
                        write(res, json, HttpStatus.UNAUTHORIZED, "SESSION_REQUIRED", "Session absente ou expirée.", req.getRequestURI()))
                .accessDeniedHandler((req, res, ex) -> {
                    if (ex instanceof CsrfException) {
                        write(res, json, HttpStatus.FORBIDDEN, "CSRF_INVALID", "Jeton de sécurité invalide ou absent.", req.getRequestURI());
                    } else {
                        write(res, json, HttpStatus.FORBIDDEN, "FORBIDDEN", "Accès interdit.", req.getRequestURI());
                    }
                }));
        return http.build();
    }

    private static void write(HttpServletResponse res, JsonMapper json, HttpStatus status, String code, String detail, String uri)
            throws IOException {
        ProblemDetail pd = ProblemHandler.problem(status, code, detail, uri);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", pd.getType().toString());
        body.put("title", pd.getTitle());
        body.put("status", pd.getStatus());
        body.put("detail", pd.getDetail());
        body.put("instance", uri);
        if (pd.getProperties() != null) body.putAll(pd.getProperties());
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(json.writeValueAsString(body));
    }
}
