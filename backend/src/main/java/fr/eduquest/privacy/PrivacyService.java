package fr.eduquest.privacy;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Traçabilité de l'information délivrée et export familial. Les bases légales enregistrées sont les
 * propositions documentées dans docs/security-privacy.md, à valider avant ouverture publique.
 */
@Service
public class PrivacyService {
    public static final int EXPORT_FORMAT_VERSION = 1;

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final Clock clock;

    public PrivacyService(JdbcClient jdbc, JsonMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    @Transactional
    public void recordAccountNotice(UUID parentId, String noticeVersion) {
        insert(parentId, null, noticeVersion, "ACCOUNT", "CONTRACT", "NOTICE_READ_AT_SIGNUP", null);
    }

    @Transactional
    public void recordChildInformation(UUID parentId, UUID childId) {
        insert(parentId, childId, PrivacyNotice.VERSION, "LEARNING_TRACKING", "CONTRACT",
                "CHILD_PROFILE_CREATED_WITH_INFORMATION", PrivacyNotice.CHILD_VERSION);
    }

    private void insert(UUID parentId, UUID childId, String notice, String purpose, String basis, String action, String childVersion) {
        jdbc.sql("""
                INSERT INTO privacy_acknowledgement (id, parent_id, child_id, notice_version, purpose_code, legal_basis,
                    parent_action, child_information_version, recorded_at)
                VALUES (:id, :p, :c, :n, :purpose, :basis, :action, :cv, :at)""")
                .param("id", UUID.randomUUID()).param("p", parentId).param("c", childId).param("n", notice)
                .param("purpose", purpose).param("basis", basis).param("action", action).param("cv", childVersion)
                .param("at", now()).update();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> acknowledgements(UUID parentId) {
        return jdbc.sql("""
                SELECT a.notice_version, a.purpose_code, a.legal_basis, a.parent_action, a.child_information_version,
                       a.recorded_at, a.withdrawn_at, c.nickname AS child_nickname
                FROM privacy_acknowledgement a LEFT JOIN child_profile c ON c.id = a.child_id
                WHERE a.parent_id = :p ORDER BY a.recorded_at""")
                .param("p", parentId).query().listOfRows();
    }

    /** Export JSON versionné de toutes les données familiales, sans hachage ni secret. */
    @Transactional(readOnly = true)
    public ObjectNode export(UUID parentId) {
        ObjectNode root = json.createObjectNode();
        root.put("format", "eduquest-family-export");
        root.put("formatVersion", EXPORT_FORMAT_VERSION);
        root.put("exportedAt", now().toString());
        root.set("account", json.valueToTree(jdbc.sql("""
                SELECT id, email_normalized AS email, status, email_verified_at, created_at, last_login_at
                FROM parent_account WHERE id = :p""").param("p", parentId).query().singleRow()));
        root.set("privacyAcknowledgements", json.valueToTree(acknowledgements(parentId)));

        ArrayNode children = root.putArray("children");
        List<Map<String, Object>> profiles = jdbc.sql("""
                SELECT id, nickname, avatar_code, grade, created_at, updated_at FROM child_profile
                WHERE parent_id = :p ORDER BY created_at""").param("p", parentId).query().listOfRows();
        for (Map<String, Object> profile : profiles) {
            UUID childId = (UUID) profile.get("id");
            ObjectNode child = json.valueToTree(profile);
            child.set("missionProgress", json.valueToTree(jdbc.sql("""
                    SELECT m.code AS mission_code, p.best_correct_count, p.best_stars, p.xp_awarded, p.completed_attempts, p.last_completed_at
                    FROM mission_progress p JOIN mission m ON m.id = p.mission_id WHERE p.child_id = :c ORDER BY m.code""")
                    .param("c", childId).query().listOfRows()));
            child.set("xpEvents", json.valueToTree(jdbc.sql("""
                    SELECT x.attempt_id, m.code AS mission_code, x.amount, x.created_at
                    FROM xp_event x JOIN mission m ON m.id = x.mission_id WHERE x.child_id = :c ORDER BY x.created_at""")
                    .param("c", childId).query().listOfRows()));
            child.set("rewards", json.valueToTree(jdbc.sql("""
                    SELECT r.code, r.title, cr.granted_at FROM child_reward cr JOIN reward_definition r ON r.id = cr.reward_id
                    WHERE cr.child_id = :c ORDER BY cr.granted_at""").param("c", childId).query().listOfRows()));
            child.set("equipment", json.valueToTree(jdbc.sql("""
                    SELECT e.slot, r.code FROM child_equipment e JOIN reward_definition r ON r.id = e.reward_id
                    WHERE e.child_id = :c""").param("c", childId).query().listOfRows()));
            ArrayNode attempts = child.putArray("attempts");
            for (Map<String, Object> a : jdbc.sql("""
                    SELECT a.id, m.code AS mission_code, mv.version_number AS content_version, a.status, a.started_at,
                           a.completed_at, a.correct_count, a.stars
                    FROM attempt a JOIN mission m ON m.id = a.mission_id JOIN mission_version mv ON mv.id = a.mission_version_id
                    WHERE a.child_id = :c ORDER BY a.started_at""").param("c", childId).query().listOfRows()) {
                ObjectNode attempt = json.valueToTree(a);
                ArrayNode answers = attempt.putArray("answers");
                for (Map<String, Object> ans : jdbc.sql("""
                        SELECT e.ordinal AS position, e.prompt, aa.submitted_answer::text AS submitted_answer, aa.is_correct,
                               aa.hint_used, aa.hint_requested_at, aa.answered_at
                        FROM attempt_answer aa JOIN exercise e ON e.id = aa.exercise_id
                        WHERE aa.attempt_id = :a ORDER BY e.ordinal""").param("a", a.get("id")).query().listOfRows()) {
                    ObjectNode node = json.valueToTree(ans);
                    Object submitted = ans.get("submitted_answer");
                    if (submitted != null) node.set("submitted_answer", json.readTree(submitted.toString()));
                    answers.add(node);
                }
                attempts.add(attempt);
            }
            children.add(child);
        }
        return root;
    }
}
