package fr.eduquest.shared;

import org.slf4j.MDC;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Journal d'audit minimal : type d'événement, référence pseudonyme (UUID du compte), résultat.
 * Jamais de mot de passe, réponse, e-mail ou jeton.
 */
@Service
public class AuditService {
    private final JdbcClient jdbc;
    private final Clock clock;

    public AuditService(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String eventType, UUID actor, String outcome) {
        jdbc.sql("""
                INSERT INTO audit_event (id, event_type, actor_pseudonymous_ref, outcome, created_at, correlation_id)
                VALUES (:id, :type, :actor, :outcome, :at, :corr)""")
                .param("id", UUID.randomUUID())
                .param("type", eventType)
                .param("actor", actor == null ? null : actor.toString())
                .param("outcome", outcome)
                .param("at", OffsetDateTime.now(clock))
                .param("corr", MDC.get(TraceIdFilter.MDC_KEY))
                .update();
    }
}
