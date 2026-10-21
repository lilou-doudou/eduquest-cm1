package fr.eduquest.catalog;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lecture du catalogue publié (requêtes SQL explicites, pas d'entité exposée). */
@Repository
public class CatalogRepository {
    private final JdbcClient jdbc;

    public CatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record WorldRow(UUID id, String slug, String title, String subjectCode, int displayOrder,
                           String availability, String assetKey, String description) {
        public boolean playable() { return "PLAYABLE".equals(availability); }
    }

    public record MissionRow(UUID id, String code, UUID worldId, String worldSlug, int levelOrdinal, String levelTitle,
                             int ordinal, UUID versionId, String title) {}

    public record ExerciseRow(UUID id, int ordinal, UUID skillId, String skillCode, String type, int difficulty,
                              String instruction, String prompt, String publicPayload, String answerKey,
                              String hint, String explanation) {}

    private static final String MISSION_SELECT = """
            SELECT m.id, m.code, w.id AS world_id, w.slug AS world_slug, l.ordinal AS level_ordinal, l.title AS level_title,
                   m.ordinal, mv.id AS version_id, mv.title
            FROM mission m
            JOIN level l ON l.id = m.level_id
            JOIN world w ON w.id = l.world_id
            JOIN mission_version mv ON mv.mission_id = m.id AND mv.status = 'PUBLISHED'
            """;

    public List<WorldRow> worlds() {
        return jdbc.sql("SELECT id, slug, title, subject_code, display_order, availability, asset_key, description FROM world ORDER BY display_order")
                .query(WorldRow.class).list();
    }

    public Optional<WorldRow> worldBySlug(String slug) {
        return jdbc.sql("SELECT id, slug, title, subject_code, display_order, availability, asset_key, description FROM world WHERE slug = :slug")
                .param("slug", slug).query(WorldRow.class).optional();
    }

    public Optional<WorldRow> worldOfMission(UUID missionId) {
        return jdbc.sql("""
                SELECT w.id, w.slug, w.title, w.subject_code, w.display_order, w.availability, w.asset_key, w.description
                FROM world w JOIN level l ON l.world_id = w.id JOIN mission m ON m.level_id = l.id WHERE m.id = :id""")
                .param("id", missionId).query(WorldRow.class).optional();
    }

    /** Missions jouables (ayant une version publiée), dans l'ordre du parcours. */
    public List<MissionRow> publishedMissions() {
        return jdbc.sql(MISSION_SELECT + " ORDER BY w.display_order, l.ordinal, m.ordinal")
                .query(MissionRow.class).list();
    }

    public List<MissionRow> publishedMissions(UUID worldId) {
        return jdbc.sql(MISSION_SELECT + " WHERE w.id = :world ORDER BY l.ordinal, m.ordinal")
                .param("world", worldId).query(MissionRow.class).list();
    }

    public Optional<MissionRow> publishedMission(UUID missionId) {
        return jdbc.sql(MISSION_SELECT + " WHERE m.id = :id").param("id", missionId).query(MissionRow.class).optional();
    }

    public List<ExerciseRow> exercises(UUID versionId) {
        return jdbc.sql("""
                SELECT e.id, e.ordinal, e.skill_id, s.code AS skill_code, e.type, e.difficulty, e.instruction, e.prompt,
                       e.public_payload::text AS public_payload, e.answer_key::text AS answer_key, e.hint, e.explanation
                FROM exercise e JOIN skill s ON s.id = e.skill_id
                WHERE e.mission_version_id = :v ORDER BY e.ordinal""")
                .param("v", versionId).query(ExerciseRow.class).list();
    }

    public String missionVersionTitle(UUID versionId) {
        return jdbc.sql("SELECT title FROM mission_version WHERE id = :id").param("id", versionId).query(String.class).single();
    }
}
