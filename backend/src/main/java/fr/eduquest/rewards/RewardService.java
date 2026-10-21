package fr.eduquest.rewards;

import fr.eduquest.catalog.CatalogRepository;
import fr.eduquest.catalog.CatalogRepository.MissionRow;
import fr.eduquest.learning.ScoringRules;
import fr.eduquest.shared.ApiException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Récompenses déterministes. Les règles sont interprétées par ce code (types autorisés), jamais par script.
 * La contrainte unique (child_id, reward_id) garantit une attribution unique même en cas de concurrence.
 */
@Service
public class RewardService {
    private final JdbcClient jdbc;
    private final CatalogRepository catalog;
    private final JsonMapper json;
    private final Clock clock;

    public RewardService(JdbcClient jdbc, CatalogRepository catalog, JsonMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.catalog = catalog;
        this.json = json;
        this.clock = clock;
    }

    record Definition(UUID id, String code, String type, String title, String description, String assetKey, String slot,
                      String ruleType, String ruleParameters) {}

    public record RewardView(String code, String type, String title, String description, String assetKey, String slot,
                             boolean owned, OffsetDateTime grantedAt) {}

    public record RewardsResponse(List<RewardView> rewards, Map<String, String> equipment) {}

    private List<Definition> definitions() {
        return jdbc.sql("""
                SELECT id, code, type, title, description, asset_key, slot, rule_type, rule_parameters::text AS rule_parameters
                FROM reward_definition ORDER BY type, code""").query(Definition.class).list();
    }

    /** Évalue toutes les règles sur l'état courant et attribue les récompenses manquantes. Appelé sous verrou enfant. */
    @Transactional
    public List<String> grantEarned(UUID childId, UUID sourceAttemptId) {
        Map<UUID, Integer> bestStars = new HashMap<>();
        jdbc.sql("SELECT mission_id, best_stars FROM mission_progress WHERE child_id = :c").param("c", childId)
                .query((rs, i) -> bestStars.put(rs.getObject(1, UUID.class), rs.getInt(2))).list();
        int xpTotal = jdbc.sql("SELECT COALESCE(SUM(xp_awarded), 0) FROM mission_progress WHERE child_id = :c")
                .param("c", childId).query(Integer.class).single();
        List<MissionRow> missions = catalog.publishedMissions();

        List<String> granted = new ArrayList<>();
        for (Definition def : definitions()) {
            if (!earned(def, bestStars, xpTotal, missions)) continue;
            int inserted = jdbc.sql("""
                    INSERT INTO child_reward (id, child_id, reward_id, granted_at, source_attempt_id)
                    VALUES (:id, :c, :r, :t, :a) ON CONFLICT (child_id, reward_id) DO NOTHING""")
                    .param("id", UUID.randomUUID()).param("c", childId).param("r", def.id())
                    .param("t", OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)).param("a", sourceAttemptId)
                    .update();
            if (inserted == 1) granted.add(def.code());
        }
        return granted;
    }

    private boolean earned(Definition def, Map<UUID, Integer> bestStars, int xpTotal, List<MissionRow> missions) {
        JsonNode params = json.readTree(def.ruleParameters());
        return switch (def.ruleType()) {
            case "FIRST_STAR" -> bestStars.values().stream().anyMatch(s -> s >= 1);
            case "FIRST_THREE_STARS" -> bestStars.values().stream().anyMatch(s -> s >= 3);
            case "WORLD_COMPLETED" -> allStarred(missions.stream()
                    .filter(m -> m.worldSlug().equals(params.path("world").asString())).toList(), bestStars);
            case "LEVEL_COMPLETED" -> allStarred(missions.stream()
                    .filter(m -> m.worldSlug().equals(params.path("world").asString())
                            && m.levelOrdinal() == params.path("level").asInt()).toList(), bestStars);
            case "RANK_REACHED" -> ScoringRules.adventurerLevel(xpTotal) >= params.path("rank").asInt();
            default -> false;
        };
    }

    private static boolean allStarred(List<MissionRow> missions, Map<UUID, Integer> bestStars) {
        return !missions.isEmpty() && missions.stream().allMatch(m -> bestStars.getOrDefault(m.id(), 0) >= 1);
    }

    @Transactional(readOnly = true)
    public RewardsResponse forChild(UUID childId) {
        Map<UUID, OffsetDateTime> owned = new HashMap<>();
        jdbc.sql("SELECT reward_id, granted_at FROM child_reward WHERE child_id = :c").param("c", childId)
                .query((rs, i) -> owned.put(rs.getObject(1, UUID.class), rs.getObject(2, OffsetDateTime.class))).list();
        List<RewardView> views = definitions().stream()
                .map(d -> new RewardView(d.code(), d.type(), d.title(), d.description(), d.assetKey(), d.slot(),
                        owned.containsKey(d.id()), owned.get(d.id())))
                .toList();
        Map<String, String> equipment = new HashMap<>();
        jdbc.sql("""
                SELECT e.slot, r.code FROM child_equipment e JOIN reward_definition r ON r.id = e.reward_id
                WHERE e.child_id = :c""").param("c", childId)
                .query((rs, i) -> equipment.put(rs.getString(1), rs.getString(2))).list();
        return new RewardsResponse(views, equipment);
    }

    @Transactional
    public RewardsResponse equip(UUID childId, String slot, String rewardCode) {
        if (!List.of("HEAD", "BACK", "COMPANION").contains(slot)) throw ApiException.notFound();
        jdbc.sql("SELECT id FROM child_profile WHERE id = :c FOR UPDATE").param("c", childId).query(UUID.class).optional();
        if (rewardCode == null || rewardCode.isBlank()) {
            jdbc.sql("DELETE FROM child_equipment WHERE child_id = :c AND slot = :s").param("c", childId).param("s", slot).update();
            return forChild(childId);
        }
        Optional<UUID> rewardId = jdbc.sql("""
                SELECT r.id FROM reward_definition r JOIN child_reward cr ON cr.reward_id = r.id
                WHERE r.code = :code AND cr.child_id = :c AND r.slot = :s""")
                .param("code", rewardCode).param("c", childId).param("s", slot).query(UUID.class).optional();
        if (rewardId.isEmpty()) {
            throw ApiException.unprocessable("REWARD_NOT_OWNED", "Cet objet n'est pas encore gagné ou ne va pas à cet emplacement.");
        }
        jdbc.sql("""
                INSERT INTO child_equipment (child_id, slot, reward_id) VALUES (:c, :s, :r)
                ON CONFLICT (child_id, slot) DO UPDATE SET reward_id = EXCLUDED.reward_id""")
                .param("c", childId).param("s", slot).param("r", rewardId.get()).update();
        return forChild(childId);
    }
}
