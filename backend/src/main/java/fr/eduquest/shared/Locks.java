package fr.eduquest.shared;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Verrous pessimistes explicites. Ordre global imposé pour toutes les mutations :
 * parent → enfant → partie → progression. Ne jamais les prendre dans l'ordre inverse.
 */
@Component
public class Locks {
    private final JdbcClient jdbc;

    public Locks(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean lockParent(UUID parentId) {
        return jdbc.sql("SELECT id FROM parent_account WHERE id = :id FOR UPDATE")
                .param("id", parentId).query(UUID.class).optional().isPresent();
    }

    public boolean lockChild(UUID childId) {
        return jdbc.sql("SELECT id FROM child_profile WHERE id = :id FOR UPDATE")
                .param("id", childId).query(UUID.class).optional().isPresent();
    }

    public boolean lockAttempt(UUID attemptId) {
        return jdbc.sql("SELECT id FROM attempt WHERE id = :id FOR UPDATE")
                .param("id", attemptId).query(UUID.class).optional().isPresent();
    }
}
