package fr.eduquest.privacy;

import fr.eduquest.auth.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Purges proposées (§12.2, à valider) : inactivité familiale 12 mois → notification, suppression 30 jours
 * plus tard ; comptes jamais vérifiés après 7 jours ; journaux de sécurité 90 jours ; jetons expirés ;
 * registre des suppressions après 35 jours (rotation des sauvegardes 30 jours + marge).
 */
@Component
public class RetentionJob {
    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final JdbcClient jdbc;
    private final DeletionService deletion;
    private final MailService mail;
    private final TransactionTemplate tx;
    private final Clock clock;

    public RetentionJob(JdbcClient jdbc, DeletionService deletion, MailService mail, TransactionTemplate tx, Clock clock) {
        this.jdbc = jdbc;
        this.deletion = deletion;
        this.mail = mail;
        this.tx = tx;
        this.clock = clock;
    }

    public record Report(int notified, int inactiveDeleted, int unverifiedDeleted, int auditPurged, int tokensPurged) {}

    @Scheduled(cron = "${eduquest.retention.cron:0 30 3 * * *}", zone = "UTC")
    public void scheduled() {
        Report r = run();
        log.info("Purge de rétention : {}", r);
    }

    public Report run() {
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        record Candidate(UUID id, String email) {}
        List<Candidate> toNotify = jdbc.sql("""
                SELECT id, email_normalized FROM parent_account
                WHERE last_activity_at < :limit AND inactivity_notified_at IS NULL AND status <> 'DELETING'""")
                .param("limit", now.minusMonths(12)).query(Candidate.class).list();
        for (Candidate c : toNotify) {
            mail.sendInactivityNotice(c.email());
            jdbc.sql("UPDATE parent_account SET inactivity_notified_at = :now WHERE id = :id")
                    .param("now", now).param("id", c.id()).update();
        }

        List<UUID> inactive = jdbc.sql("""
                SELECT id FROM parent_account
                WHERE inactivity_notified_at IS NOT NULL AND inactivity_notified_at < :limit
                  AND last_activity_at < inactivity_notified_at""")
                .param("limit", now.minusDays(30)).query(UUID.class).list();
        inactive.forEach(id -> tx.executeWithoutResult(s -> deletion.deleteFamily(id)));

        List<UUID> unverified = jdbc.sql("""
                SELECT id FROM parent_account WHERE email_verified_at IS NULL AND created_at < :limit""")
                .param("limit", now.minusDays(7)).query(UUID.class).list();
        unverified.forEach(id -> tx.executeWithoutResult(s -> deletion.deleteFamily(id)));

        int audit = jdbc.sql("DELETE FROM audit_event WHERE created_at < :limit").param("limit", now.minusDays(90)).update();
        int tokens = jdbc.sql("DELETE FROM account_token WHERE expires_at < :limit").param("limit", now.minusDays(1)).update();
        jdbc.sql("DELETE FROM rate_limit_bucket WHERE window_start < :limit").param("limit", now.minusDays(1)).update();
        jdbc.sql("DELETE FROM deletion_record WHERE deleted_at < :limit").param("limit", now.minusDays(35)).update();
        return new Report(toNotify.size(), inactive.size(), unverified.size(), audit, tokens);
    }
}
