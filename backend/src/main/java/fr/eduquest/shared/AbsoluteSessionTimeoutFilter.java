package fr.eduquest.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/** Durée absolue de session (12 h) : au-delà, la session est invalidée quelle que soit l'activité. */
public class AbsoluteSessionTimeoutFilter extends OncePerRequestFilter {
    private final Clock clock;
    private final Duration absolute;

    public AbsoluteSessionTimeoutFilter(Clock clock, Duration absolute) {
        this.clock = clock;
        this.absolute = absolute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SessionPrincipal p
                && p.sessionStartedAt().plus(absolute).isBefore(clock.instant())) {
            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }
}
