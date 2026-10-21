package fr.eduquest.shared;

import org.springframework.security.core.AuthenticatedPrincipal;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Contexte serveur d'une session. Le nom (indexé par Spring Session) est l'identifiant du compte
 * parent, ce qui permet de révoquer toutes les sessions d'une famille.
 *
 * @param authenticatedAt dernière preuve du mot de passe parent (réauthentification récente)
 * @param sessionStartedAt début de la session, pour la durée absolue de 12 h
 */
public record SessionPrincipal(Role role, UUID parentId, UUID childId, Instant authenticatedAt, Instant sessionStartedAt)
        implements AuthenticatedPrincipal, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return parentId.toString();
    }

    public SessionPrincipal withAuthenticatedAt(Instant at) {
        return new SessionPrincipal(role, parentId, childId, at, sessionStartedAt);
    }
}
