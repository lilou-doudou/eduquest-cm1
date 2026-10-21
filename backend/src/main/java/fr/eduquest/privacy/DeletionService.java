package fr.eduquest.privacy;

import fr.eduquest.shared.SessionManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Suppressions effectives dans la base active. Un registre minimal (identifiant + date) permet de
 * réappliquer les suppressions après restauration d'une sauvegarde ; il est purgé après la rotation
 * maximale des sauvegardes (voir RetentionJob).
 */
@Service
public class DeletionService {
    private final JdbcClient jdbc;
    private final SessionManager sessions;
    private final Clock clock;

    public DeletionService(JdbcClient jdbc, SessionManager sessions, Clock clock) {
        this.jdbc = jdbc;
        this.sessions = sessions;
        this.clock = clock;
    }

    private void register(String scope, UUID subject) {
        jdbc.sql("INSERT INTO deletion_record (id, scope, subject_id, deleted_at) VALUES (:id, :s, :sub, :at)")
                .param("id", UUID.randomUUID()).param("s", scope).param("sub", subject)
                .param("at", OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)).update();
    }

    /** Supprime un profil enfant et, par cascade, ses parties, réponses, progression, XP et récompenses. */
    @Transactional
    public void deleteChild(UUID childId) {
        jdbc.sql("SELECT id FROM child_profile WHERE id = :c FOR UPDATE").param("c", childId).query(UUID.class).optional();
        jdbc.sql("DELETE FROM child_profile WHERE id = :c").param("c", childId).update();
        register("CHILD", childId);
    }

    /** Supprime atomiquement tout le compte familial, révoque ses sessions et anonymise les journaux conservés. */
    @Transactional
    public void deleteFamily(UUID parentId) {
        jdbc.sql("SELECT id FROM parent_account WHERE id = :p FOR UPDATE").param("p", parentId).query(UUID.class).optional();
        sessions.revokeFamilySessions(parentId, null);
        jdbc.sql("UPDATE audit_event SET actor_pseudonymous_ref = NULL WHERE actor_pseudonymous_ref = :p")
                .param("p", parentId.toString()).update();
        jdbc.sql("DELETE FROM parent_account WHERE id = :p").param("p", parentId).update();
        register("FAMILY", parentId);
    }
}
