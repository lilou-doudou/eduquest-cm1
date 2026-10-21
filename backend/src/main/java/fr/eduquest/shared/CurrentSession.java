package fr.eduquest.shared;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Accès au contexte de session vérifié par Spring Security. */
public final class CurrentSession {
    private CurrentSession() {}

    public static SessionPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SessionPrincipal p) return p;
        throw ApiException.unauthorized("SESSION_REQUIRED", "Session absente ou expirée.");
    }

    public static UUID parentId() {
        SessionPrincipal p = principal();
        if (p.role() != Role.PARENT) throw ApiException.forbidden("PARENT_REQUIRED", "Espace réservé à l'adulte.");
        return p.parentId();
    }

    public static UUID childId() {
        SessionPrincipal p = principal();
        if (p.role() != Role.CHILD) throw ApiException.forbidden("CHILD_REQUIRED", "Espace réservé au mode enfant.");
        return p.childId();
    }
}
