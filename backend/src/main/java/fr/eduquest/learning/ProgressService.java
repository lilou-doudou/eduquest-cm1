package fr.eduquest.learning;

import fr.eduquest.catalog.CatalogRepository;
import fr.eduquest.catalog.CatalogRepository.MissionRow;
import fr.eduquest.catalog.CatalogRepository.WorldRow;
import fr.eduquest.catalog.UnlockRules;
import fr.eduquest.family.ChildProfile;
import fr.eduquest.family.ChildProfileRepository;
import fr.eduquest.learning.ProgressDtos.*;
import fr.eduquest.shared.ApiException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProgressService {
    static final int MIN_EVALUATED_FOR_STATUS = 5;
    static final int RECENT_ATTEMPTS_WINDOW = 5;

    private final CatalogRepository catalog;
    private final MissionProgressRepository progress;
    private final ChildProfileRepository children;
    private final JdbcClient jdbc;

    public ProgressService(CatalogRepository catalog, MissionProgressRepository progress,
                           ChildProfileRepository children, JdbcClient jdbc) {
        this.catalog = catalog;
        this.progress = progress;
        this.children = children;
        this.jdbc = jdbc;
    }

    private Map<UUID, MissionProgress> progressMap(UUID childId) {
        Map<UUID, MissionProgress> map = new HashMap<>();
        progress.findByChildId(childId).forEach(p -> map.put(p.getMissionId(), p));
        return map;
    }

    private static Map<UUID, Integer> stars(Map<UUID, MissionProgress> pm) {
        Map<UUID, Integer> map = new HashMap<>();
        pm.forEach((k, v) -> map.put(k, v.getBestStars()));
        return map;
    }

    private WorldSummary summary(WorldRow w, List<MissionRow> missions, Map<UUID, MissionProgress> pm) {
        int completed = 0;
        int stars = 0;
        for (MissionRow m : missions) {
            MissionProgress p = pm.get(m.id());
            if (p != null) {
                stars += p.getBestStars();
                if (p.getBestStars() >= 1) completed++;
            }
        }
        return new WorldSummary(w.slug(), w.title(), w.subjectCode(), w.displayOrder(), w.availability(), w.assetKey(),
                w.description(), missions.size(), completed, stars, missions.size() * 3,
                !missions.isEmpty() && completed == missions.size());
    }

    @Transactional(readOnly = true)
    public List<WorldSummary> worlds(UUID childId) {
        Map<UUID, MissionProgress> pm = progressMap(childId);
        List<MissionRow> all = catalog.publishedMissions();
        return catalog.worlds().stream()
                .map(w -> summary(w, w.playable() ? all.stream().filter(m -> m.worldId().equals(w.id())).toList() : List.of(), pm))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorldDetail world(UUID childId, String slug) {
        WorldRow w = catalog.worldBySlug(slug).orElseThrow(ApiException::notFound);
        Map<UUID, MissionProgress> pm = progressMap(childId);
        List<MissionRow> missions = w.playable() ? catalog.publishedMissions(w.id()) : List.of();
        List<UnlockRules.State> states = UnlockRules.states(missions.stream().map(MissionRow::id).toList(), stars(pm));
        Map<UUID, UUID> inProgress = new HashMap<>();
        jdbc.sql("SELECT mission_id, id FROM attempt WHERE child_id = :c AND status = 'IN_PROGRESS'").param("c", childId)
                .query((rs, i) -> inProgress.put(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class))).list();

        Map<Integer, List<MissionSummary>> byLevel = new java.util.TreeMap<>();
        Map<Integer, String> levelTitles = new HashMap<>();
        for (int i = 0; i < missions.size(); i++) {
            MissionRow m = missions.get(i);
            MissionProgress p = pm.get(m.id());
            levelTitles.put(m.levelOrdinal(), m.levelTitle());
            byLevel.computeIfAbsent(m.levelOrdinal(), k -> new ArrayList<>()).add(new MissionSummary(m.id(), m.code(), m.ordinal(),
                    m.title(), states.get(i).name(), p == null ? 0 : p.getBestStars(), p == null ? 0 : p.getBestCorrectCount(),
                    p == null ? 0 : p.getCompletedAttempts(), inProgress.get(m.id())));
        }
        List<LevelSummary> levels = new ArrayList<>();
        if (w.playable()) {
            byLevel.forEach((ordinal, list) -> levels.add(new LevelSummary(ordinal, levelTitles.get(ordinal),
                    list.stream().anyMatch(ms -> !"LOCKED".equals(ms.state())), list)));
        } else {
            jdbc.sql("SELECT ordinal, title FROM level WHERE world_id = :w ORDER BY ordinal").param("w", w.id())
                    .query((rs, i) -> levels.add(new LevelSummary(rs.getInt(1), rs.getString(2), false, List.of()))).list();
        }
        return new WorldDetail(summary(w, missions, pm), levels);
    }

    private int xpTotal(UUID childId) {
        return jdbc.sql("SELECT COALESCE(SUM(xp_awarded), 0) FROM mission_progress WHERE child_id = :c")
                .param("c", childId).query(Integer.class).single();
    }

    @Transactional(readOnly = true)
    public ChildProgressResponse forChild(UUID childId) {
        ChildProfile child = children.findById(childId).orElseThrow(ApiException::notFound);
        int xp = xpTotal(childId);
        int level = ScoringRules.adventurerLevel(xp);
        List<WorldSummary> worlds = worlds(childId);
        List<BadgeView> badges = jdbc.sql("""
                SELECT r.code, r.title, r.asset_key FROM child_reward cr JOIN reward_definition r ON r.id = cr.reward_id
                WHERE cr.child_id = :c AND r.type = 'BADGE' ORDER BY cr.granted_at""")
                .param("c", childId).query(BadgeView.class).list();
        return new ChildProgressResponse(child.getId(), child.getNickname(), child.getAvatarCode(), xp, level,
                xp - (level - 1) * 100, 100, worlds.stream().mapToInt(WorldSummary::stars).sum(), badges, worlds);
    }

    @Transactional(readOnly = true)
    public ParentProgressResponse forParent(ChildProfile child, int page, int size) {
        UUID childId = child.getId();
        int xp = xpTotal(childId);
        List<WorldSummary> worlds = worlds(childId);
        List<SkillIndicator> skills = skillIndicators(childId);

        long total = jdbc.sql("SELECT count(*) FROM attempt WHERE child_id = :c").param("c", childId).query(Long.class).single();
        List<AttemptHistoryItem> items = jdbc.sql("""
                SELECT a.id, mv.title AS mission_title, w.title AS world_title, a.status, a.started_at, a.completed_at,
                       a.correct_count, a.stars,
                       (SELECT count(*) FROM attempt_answer aa WHERE aa.attempt_id = a.id AND aa.hint_used) AS hints
                FROM attempt a
                JOIN mission_version mv ON mv.id = a.mission_version_id
                JOIN mission m ON m.id = a.mission_id JOIN level l ON l.id = m.level_id JOIN world w ON w.id = l.world_id
                WHERE a.child_id = :c ORDER BY a.started_at DESC, a.id LIMIT :limit OFFSET :offset""")
                .param("c", childId).param("limit", size).param("offset", page * size)
                .query((rs, i) -> new AttemptHistoryItem(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                        rs.getString(4), toInstant(rs.getTimestamp(5)), toInstant(rs.getTimestamp(6)),
                        (Integer) rs.getObject(7), (Integer) rs.getObject(8), rs.getInt(9)))
                .list();

        List<RevisionSuggestion> suggestions = new ArrayList<>();
        for (SkillIndicator s : skills) {
            if ("TO_REVIEW".equals(s.status())) {
                suggestions.add(new RevisionSuggestion("SKILL", s.label(),
                        s.successRate() + " % de réussite sur " + s.evaluatedCount() + " réponses récentes ("
                                + s.attemptCount() + " parties). Rejouer les missions concernées peut aider."));
            }
        }
        Map<UUID, MissionProgress> pm = progressMap(childId);
        for (MissionRow m : catalog.publishedMissions()) {
            MissionProgress p = pm.get(m.id());
            if (p != null && p.getCompletedAttempts() > 0 && p.getBestStars() == 0) {
                suggestions.add(new RevisionSuggestion("MISSION", m.title(),
                        "Mission terminée sans étoile (meilleur score " + p.getBestCorrectCount() + "/5) : elle bloque la suite du monde."));
            }
        }
        return new ParentProgressResponse(childId, child.getNickname(), child.getAvatarCode(), xp,
                ScoringRules.adventurerLevel(xp), worlds.stream().mapToInt(WorldSummary::stars).sum(), worlds, skills,
                new HistoryPage(items, page, size, total), suggestions);
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    /**
     * Taux de réussite sur les réponses évaluées des cinq dernières parties terminées contenant la compétence.
     * Les parties abandonnées ne comptent pas.
     */
    public List<SkillIndicator> skillIndicators(UUID childId) {
        Map<String, int[]> stats = new HashMap<>();
        jdbc.sql("""
                WITH answered AS (
                    SELECT a.id AS attempt_id, a.completed_at, e.skill_id, aa.is_correct, aa.hint_used
                    FROM attempt a
                    JOIN attempt_answer aa ON aa.attempt_id = a.id AND aa.answered_at IS NOT NULL
                    JOIN exercise e ON e.id = aa.exercise_id
                    WHERE a.child_id = :c AND a.status = 'COMPLETED'
                ), ranked AS (
                    SELECT answered.*, dense_rank() OVER (PARTITION BY skill_id ORDER BY completed_at DESC, attempt_id) AS rk
                    FROM answered
                )
                SELECT s.code, count(*) AS total,
                       count(*) FILTER (WHERE is_correct) AS correct,
                       count(DISTINCT attempt_id) AS attempts,
                       count(*) FILTER (WHERE hint_used) AS hints,
                       count(*) FILTER (WHERE hint_used AND is_correct) AS hinted_correct
                FROM ranked JOIN skill s ON s.id = ranked.skill_id
                WHERE rk <= :window GROUP BY s.code""")
                .param("c", childId).param("window", RECENT_ATTEMPTS_WINDOW)
                .query((rs, i) -> stats.put(rs.getString(1),
                        new int[]{rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6)}))
                .list();
        List<SkillIndicator> result = new ArrayList<>();
        jdbc.sql("SELECT code, label, subject_code FROM skill ORDER BY subject_code DESC, code").query((rs, i) -> {
            String code = rs.getString(1);
            int[] s = stats.getOrDefault(code, new int[5]);
            Integer rate = s[0] == 0 ? null : (int) Math.floor(s[1] * 100.0 / s[0]);
            result.add(new SkillIndicator(code, rs.getString(2), rs.getString(3), status(s[0], s[1]), rate, s[0], s[2], s[3], s[4]));
            return null;
        }).list();
        return result;
    }

    /** Statuts §7.4 : En découverte (moins de 5 réponses), puis seuils 60 % et 80 %. */
    static String status(int evaluated, int correct) {
        if (evaluated < MIN_EVALUATED_FOR_STATUS) return "DISCOVERY";
        int scaled = correct * 100;
        if (scaled < 60 * evaluated) return "TO_REVIEW";
        if (scaled < 80 * evaluated) return "IN_PROGRESS";
        return "CONSISTENT";
    }
}
