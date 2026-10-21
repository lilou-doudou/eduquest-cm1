package fr.eduquest.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Création et révocation des sessions. Chaque changement de privilège invalide l'ancienne session
 * et en crée une nouvelle (nouvel identifiant, nouveau jeton CSRF).
 */
@Component
public class SessionManager {
    private final SecurityContextRepository contextRepository;
    // Absent lorsque l'application tourne sans serveur web (outil d'import CLI).
    private final ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessions;
    private final Duration parentIdle;
    private final Duration childIdle;

    public SessionManager(SecurityContextRepository contextRepository,
                          ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessions,
                          @Value("${eduquest.session.parent-idle:PT30M}") Duration parentIdle,
                          @Value("${eduquest.session.child-idle:PT60M}") Duration childIdle) {
        this.contextRepository = contextRepository;
        this.sessions = sessions;
        this.parentIdle = parentIdle;
        this.childIdle = childIdle;
    }

    public void establish(SessionPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval((int) (principal.role() == Role.PARENT ? parentIdle : childIdle).toSeconds());
        store(principal, request, response);
    }

    /** Met à jour le contexte sans changer de privilège (ex. horodatage de réauthentification). */
    public void store(SessionPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        var auth = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
    }

    /** Révoque toutes les sessions d'une famille, sauf éventuellement la session courante. */
    public void revokeFamilySessions(UUID parentId, String keepSessionId) {
        FindByIndexNameSessionRepository<? extends Session> repository = sessions.getIfAvailable();
        if (repository == null) return;
        repository.findByPrincipalName(parentId.toString()).keySet().stream()
                .filter(id -> !id.equals(keepSessionId))
                .forEach(repository::deleteById);
    }
}
