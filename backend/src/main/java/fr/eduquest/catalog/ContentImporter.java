package fr.eduquest.catalog;

import fr.eduquest.learning.AnswerNormalizer;
import fr.eduquest.shared.Hashing;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Import transactionnel et idempotent des contenus versionnés (dossier content/). Tout est validé
 * avant la moindre écriture. Une mission dont le contenu change reçoit une nouvelle version publiée ;
 * l'ancienne est retirée sans être modifiée (les parties commencées y restent attachées).
 */
@Service
public class ContentImporter {
    public static final Set<String> SUBJECTS = Set.of("MATHS", "ENGLISH", "GEOMETRY", "GEOGRAPHY", "SCIENCES", "PHYSICS");
    private static final Set<String> RULE_TYPES = Set.of("FIRST_STAR", "FIRST_THREE_STARS", "WORLD_COMPLETED", "LEVEL_COMPLETED", "RANK_REACHED");
    private static final Set<String> SLOTS = Set.of("HEAD", "BACK", "COMPANION");
    /** Un dossier de missions par monde jouable. */
    public static final List<String> MISSION_DIRS = List.of("maths", "english", "sciences", "physique");
    /** Monde jouable : 3 niveaux × 2 missions × 5 exercices. */
    static final int MISSIONS_PER_WORLD = 6;
    static final int EXERCISES_PER_MISSION = 5;

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final Clock clock;

    public ContentImporter(JdbcClient jdbc, JsonMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    public static class ContentValidationException extends RuntimeException {
        private final List<String> errors;

        public ContentValidationException(List<String> errors) {
            super("Contenu invalide :\n - " + String.join("\n - ", errors));
            this.errors = errors;
        }

        public List<String> errors() { return errors; }
    }

    public record Report(int worlds, int skills, int missions, int exercises, int newVersions, int unchangedMissions, int rewards) {}

    record LoadedContent(JsonNode worlds, List<JsonNode> missions, JsonNode rewards) {}

    // ------------------------------------------------------------------ chargement + validation

    public LoadedContent load(Path root) {
        List<String> errors = new ArrayList<>();
        JsonNode worlds = read(root.resolve("worlds/worlds.json"), errors);
        JsonNode rewards = read(root.resolve("rewards/rewards.json"), errors);
        JsonNode manifest = read(root.resolve("assets-manifest.json"), errors);
        List<JsonNode> missions = new ArrayList<>();
        for (String dir : MISSION_DIRS) {
            try (Stream<Path> files = Files.list(root.resolve(dir))) {
                files.filter(f -> f.toString().endsWith(".json")).sorted()
                        .forEach(f -> Optional.ofNullable(read(f, errors)).ifPresent(missions::add));
            } catch (IOException e) {
                errors.add("Dossier illisible : " + dir);
            }
        }
        if (errors.isEmpty()) validate(worlds, missions, rewards, manifest, errors);
        if (!errors.isEmpty()) throw new ContentValidationException(errors);
        return new LoadedContent(worlds, missions, rewards);
    }

    private JsonNode read(Path file, List<String> errors) {
        try {
            return json.readTree(Files.readString(file));
        } catch (Exception e) {
            errors.add("Fichier illisible ou JSON invalide : " + file.getFileName());
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && v.isString() ? v.asString() : null;
    }

    private static void requireText(JsonNode node, String field, int max, String where, List<String> errors) {
        String v = text(node, field);
        if (v == null || v.isBlank()) {
            errors.add(where + " : champ « " + field + " » manquant");
        } else if (v.length() > max) {
            errors.add(where + " : « " + field + " » dépasse " + max + " caractères");
        } else if (v.contains("<") || v.contains(">")) {
            errors.add(where + " : « " + field + " » ne doit pas contenir de HTML");
        }
    }

    void validate(JsonNode worlds, List<JsonNode> missions, JsonNode rewards, JsonNode manifest, List<String> errors) {
        Set<String> assets = new HashSet<>();
        manifest.path("assets").forEach(a -> assets.add(a.path("key").asString()));

        Map<String, JsonNode> worldBySlug = new HashMap<>();
        Set<String> subjects = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        for (JsonNode w : worlds.path("worlds")) {
            String slug = text(w, "slug");
            String where = "monde " + slug;
            if (slug == null || !slug.matches("[a-z0-9-]{3,40}")) errors.add("Monde avec slug invalide");
            else if (worldBySlug.put(slug, w) != null) errors.add(where + " : slug en double");
            requireText(w, "title", 80, where, errors);
            requireText(w, "description", 300, where, errors);
            if (!SUBJECTS.contains(text(w, "subjectCode")) || !subjects.add(text(w, "subjectCode"))) errors.add(where + " : matière invalide ou en double");
            if (!orders.add(w.path("displayOrder").asInt(-1)) || w.path("displayOrder").asInt(-1) < 1) errors.add(where + " : ordre invalide ou en double");
            if (!assets.contains(text(w, "assetKey"))) errors.add(where + " : asset inconnu " + text(w, "assetKey"));
            String availability = text(w, "availability");
            if (!"PLAYABLE".equals(availability) && !"COMING_SOON".equals(availability)) errors.add(where + " : disponibilité invalide");
            if ("PLAYABLE".equals(availability)) {
                List<Integer> ordinals = new ArrayList<>();
                w.path("levels").forEach(l -> {
                    ordinals.add(l.path("ordinal").asInt());
                    requireText(l, "title", 80, where + " niveau", errors);
                });
                if (!ordinals.equals(List.of(1, 2, 3))) errors.add(where + " : un monde jouable doit avoir les niveaux 1, 2 et 3");
            }
        }
        if (worldBySlug.size() != 6) errors.add("Il faut exactement 6 mondes (trouvés : " + worldBySlug.size() + ")");

        Map<String, String> skillSubject = new HashMap<>();
        for (JsonNode s : worlds.path("skills")) {
            String code = text(s, "code");
            if (code == null || !code.matches("[A-Z0-9_]{3,40}")) { errors.add("Compétence avec code invalide"); continue; }
            if (skillSubject.put(code, text(s, "subjectCode")) != null) errors.add("Compétence en double : " + code);
            if (!SUBJECTS.contains(text(s, "subjectCode"))) errors.add("Compétence " + code + " : matière invalide");
            requireText(s, "label", 80, "compétence " + code, errors);
            requireText(s, "description", 300, "compétence " + code, errors);
        }

        Set<String> codes = new HashSet<>();
        Map<String, Set<String>> slotsPerWorld = new HashMap<>();
        int exerciseCount = 0;
        for (JsonNode m : missions) {
            String code = text(m, "code");
            String where = "mission " + code;
            if (code == null || !code.matches("[A-Z0-9-]{3,40}")) { errors.add("Mission avec code invalide"); continue; }
            if (!codes.add(code)) errors.add(where + " : code en double");
            requireText(m, "title", 80, where, errors);
            JsonNode world = worldBySlug.get(text(m, "world"));
            if (world == null || !"PLAYABLE".equals(text(world, "availability"))) { errors.add(where + " : monde inconnu ou non jouable"); continue; }
            int level = m.path("level").asInt();
            int ordinal = m.path("ordinal").asInt();
            if (level < 1 || level > 3 || ordinal < 1 || ordinal > 2) errors.add(where + " : niveau (1–3) ou ordre (1–2) invalide");
            if (!slotsPerWorld.computeIfAbsent(text(m, "world"), k -> new HashSet<>()).add(level + ":" + ordinal)) {
                errors.add(where + " : emplacement niveau/ordre déjà occupé");
            }
            JsonNode exercises = m.path("exercises");
            if (!exercises.isArray() || exercises.size() != EXERCISES_PER_MISSION) { errors.add(where + " : exactement " + EXERCISES_PER_MISSION + " exercices attendus"); continue; }
            int i = 0;
            for (JsonNode e : exercises) {
                i++;
                exerciseCount++;
                validateExercise(e, where + " exercice " + i, text(world, "subjectCode"), skillSubject, errors);
            }
        }
        int playable = 0;
        for (JsonNode w : worldBySlug.values()) {
            if ("PLAYABLE".equals(text(w, "availability"))) {
                playable++;
                Set<String> slots = slotsPerWorld.getOrDefault(text(w, "slug"), Set.of());
                if (slots.size() != MISSIONS_PER_WORLD) {
                    errors.add("monde " + text(w, "slug") + " : " + MISSIONS_PER_WORLD + " missions attendues (trouvées : " + slots.size() + ")");
                }
            }
        }
        int expectedMissions = playable * MISSIONS_PER_WORLD;
        if (missions.size() != expectedMissions) errors.add(expectedMissions + " missions attendues (trouvées : " + missions.size() + ")");
        if (exerciseCount != expectedMissions * EXERCISES_PER_MISSION) {
            errors.add(expectedMissions * EXERCISES_PER_MISSION + " exercices attendus (trouvés : " + exerciseCount + ")");
        }

        Set<String> rewardCodes = new HashSet<>();
        Set<String> ruleCodes = new HashSet<>();
        for (JsonNode r : rewards.path("rewards")) {
            String code = text(r, "code");
            String where = "récompense " + code;
            if (code == null || !code.matches("[A-Z0-9_]{3,40}")) { errors.add("Récompense avec code invalide"); continue; }
            if (!rewardCodes.add(code)) errors.add(where + " : code en double");
            if (!ruleCodes.add(String.valueOf(text(r, "ruleCode")))) errors.add(where + " : règle en double");
            requireText(r, "title", 80, where, errors);
            requireText(r, "description", 200, where, errors);
            if (!assets.contains(text(r, "assetKey"))) errors.add(where + " : asset inconnu");
            String type = text(r, "type");
            String slot = text(r, "slot");
            if ("COSMETIC".equals(type) ? !SLOTS.contains(slot) : !"BADGE".equals(type) || slot != null) {
                errors.add(where + " : type/emplacement incohérent");
            }
            String rule = text(r, "ruleType");
            JsonNode params = r.path("ruleParameters");
            if (!RULE_TYPES.contains(rule)) errors.add(where + " : type de règle non autorisé");
            if (("WORLD_COMPLETED".equals(rule) || "LEVEL_COMPLETED".equals(rule))
                    && (worldBySlug.get(params.path("world").asString()) == null
                    || !"PLAYABLE".equals(text(worldBySlug.get(params.path("world").asString()), "availability")))) {
                errors.add(where + " : monde de la règle inconnu");
            }
            if ("LEVEL_COMPLETED".equals(rule) && (params.path("level").asInt() < 1 || params.path("level").asInt() > 3)) errors.add(where + " : niveau invalide");
            if ("RANK_REACHED".equals(rule) && (params.path("rank").asInt() < 2 || params.path("rank").asInt() > 9)) errors.add(where + " : rang invalide (2–9)");
        }
    }

    private void validateExercise(JsonNode e, String where, String subject, Map<String, String> skillSubject, List<String> errors) {
        String skill = text(e, "skill");
        if (!subject.equals(skillSubject.get(skill))) errors.add(where + " : compétence inconnue ou d'une autre matière");
        int difficulty = e.path("difficulty").asInt();
        if (difficulty < 1 || difficulty > 3) errors.add(where + " : difficulté 1–3 attendue");
        requireText(e, "instruction", 200, where, errors);
        requireText(e, "prompt", 400, where, errors);
        requireText(e, "hint", 300, where, errors);
        requireText(e, "explanation", 400, where, errors);
        JsonNode answer = e.path("answer");
        Set<String> answerKeys = new HashSet<>(answer.propertyNames());
        switch (String.valueOf(text(e, "type"))) {
            case "MCQ" -> {
                JsonNode options = e.path("options");
                Set<String> ids = new HashSet<>();
                if (!options.isArray() || options.size() < 3) errors.add(where + " : au moins 3 options attendues");
                options.forEach(o -> {
                    String id = text(o, "id");
                    if (id == null || !id.matches("[a-z0-9]{1,10}") || !ids.add(id)) errors.add(where + " : identifiant d'option invalide ou en double");
                    requireText(o, "label", 100, where + " option", errors);
                });
                if (!answerKeys.equals(Set.of("optionId")) || !ids.contains(answer.path("optionId").asString())) {
                    errors.add(where + " : la réponse doit désigner exactement une option existante");
                }
            }
            case "NUMBER" -> {
                if (!answerKeys.equals(Set.of("number")) || AnswerNormalizer.parseNumber(answer.path("number").asString()).isEmpty()) {
                    errors.add(where + " : réponse numérique invalide");
                }
                if (e.has("unit")) requireText(e, "unit", 20, where, errors);
            }
            case "TEXT" -> {
                JsonNode accepted = answer.path("accepted");
                if (!answerKeys.equals(Set.of("accepted")) || !accepted.isArray() || accepted.isEmpty()) {
                    errors.add(where + " : liste « accepted » attendue");
                } else {
                    Set<String> seen = new HashSet<>();
                    accepted.forEach(a -> {
                        String n = AnswerNormalizer.normalizeText(a.asString());
                        if (n.isEmpty() || !seen.add(n)) errors.add(where + " : variante vide ou en double après normalisation");
                    });
                }
            }
            default -> errors.add(where + " : type inconnu (MCQ, NUMBER ou TEXT)");
        }
    }

    // ------------------------------------------------------------------ écriture

    @Transactional
    public Report importFrom(Path root) {
        LoadedContent content = load(root);
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        Map<String, UUID> worldIds = new HashMap<>();
        Map<String, UUID> levelIds = new HashMap<>();
        for (JsonNode w : content.worlds().path("worlds")) {
            String slug = text(w, "slug");
            UUID id = jdbc.sql("""
                    INSERT INTO world (id, slug, title, subject_code, display_order, availability, asset_key, description)
                    VALUES (:id, :slug, :title, :subject, :ord, :avail, :asset, :desc)
                    ON CONFLICT (slug) DO UPDATE SET title = EXCLUDED.title, availability = EXCLUDED.availability,
                        asset_key = EXCLUDED.asset_key, description = EXCLUDED.description
                    RETURNING id""")
                    .param("id", UUID.randomUUID()).param("slug", slug).param("title", text(w, "title"))
                    .param("subject", text(w, "subjectCode")).param("ord", w.path("displayOrder").asInt())
                    .param("avail", text(w, "availability")).param("asset", text(w, "assetKey"))
                    .param("desc", text(w, "description"))
                    .query(UUID.class).single();
            worldIds.put(slug, id);
            for (JsonNode l : w.path("levels")) {
                UUID levelId = jdbc.sql("""
                        INSERT INTO level (id, world_id, ordinal, title) VALUES (:id, :w, :o, :t)
                        ON CONFLICT (world_id, ordinal) DO UPDATE SET title = EXCLUDED.title RETURNING id""")
                        .param("id", UUID.randomUUID()).param("w", id).param("o", l.path("ordinal").asInt())
                        .param("t", text(l, "title")).query(UUID.class).single();
                levelIds.put(slug + ":" + l.path("ordinal").asInt(), levelId);
            }
        }

        Map<String, UUID> skillIds = new HashMap<>();
        for (JsonNode s : content.worlds().path("skills")) {
            UUID id = jdbc.sql("""
                    INSERT INTO skill (id, code, subject_code, label, description) VALUES (:id, :c, :s, :l, :d)
                    ON CONFLICT (code) DO UPDATE SET label = EXCLUDED.label, description = EXCLUDED.description
                    RETURNING id""")
                    .param("id", UUID.randomUUID()).param("c", text(s, "code")).param("s", text(s, "subjectCode"))
                    .param("l", text(s, "label")).param("d", text(s, "description")).query(UUID.class).single();
            skillIds.put(text(s, "code"), id);
        }

        int newVersions = 0;
        int unchanged = 0;
        int exercises = 0;
        for (JsonNode m : content.missions()) {
            String code = text(m, "code");
            UUID levelId = levelIds.get(text(m, "world") + ":" + m.path("level").asInt());
            int ordinal = m.path("ordinal").asInt();
            record Existing(UUID id, UUID levelId, int ordinal) {}
            Optional<Existing> existing = jdbc.sql("SELECT id, level_id, ordinal FROM mission WHERE code = :c")
                    .param("c", code).query(Existing.class).optional();
            UUID missionId;
            if (existing.isPresent()) {
                if (!existing.get().levelId().equals(levelId) || existing.get().ordinal() != ordinal) {
                    throw new ContentValidationException(List.of("mission " + code
                            + " : l'identité d'une mission (monde, niveau, ordre) est stable ; créer une nouvelle mission"));
                }
                missionId = existing.get().id();
            } else {
                missionId = UUID.randomUUID();
                jdbc.sql("INSERT INTO mission (id, level_id, ordinal, code) VALUES (:id, :l, :o, :c)")
                        .param("id", missionId).param("l", levelId).param("o", ordinal).param("c", code).update();
            }
            ObjectNode hashed = json.createObjectNode();
            hashed.put("title", text(m, "title"));
            hashed.set("exercises", m.path("exercises"));
            String hash = Hashing.sha256Hex(json.writeValueAsString(hashed));
            Optional<String> currentHash = jdbc.sql("SELECT content_hash FROM mission_version WHERE mission_id = :m AND status = 'PUBLISHED'")
                    .param("m", missionId).query(String.class).optional();
            if (currentHash.isPresent() && currentHash.get().equals(hash)) {
                unchanged++;
                continue;
            }
            int nextVersion = jdbc.sql("SELECT COALESCE(MAX(version_number), 0) + 1 FROM mission_version WHERE mission_id = :m")
                    .param("m", missionId).query(Integer.class).single();
            jdbc.sql("UPDATE mission_version SET status = 'RETIRED' WHERE mission_id = :m AND status = 'PUBLISHED'")
                    .param("m", missionId).update();
            UUID versionId = UUID.randomUUID();
            jdbc.sql("""
                    INSERT INTO mission_version (id, mission_id, version_number, title, status, content_hash, published_at)
                    VALUES (:id, :m, :v, :t, 'PUBLISHED', :h, :at)""")
                    .param("id", versionId).param("m", missionId).param("v", nextVersion).param("t", text(m, "title"))
                    .param("h", hash).param("at", now).update();
            int position = 0;
            for (JsonNode e : m.path("exercises")) {
                position++;
                exercises++;
                insertExercise(versionId, position, e, skillIds);
            }
            newVersions++;
        }

        int rewards = 0;
        for (JsonNode r : content.rewards().path("rewards")) {
            rewards++;
            jdbc.sql("""
                    INSERT INTO reward_definition (id, code, type, title, description, asset_key, slot, rule_type, rule_code, rule_parameters)
                    VALUES (:id, :code, :type, :title, :desc, :asset, :slot, :rt, :rc, CAST(:params AS jsonb))
                    ON CONFLICT (code) DO UPDATE SET title = EXCLUDED.title, description = EXCLUDED.description,
                        asset_key = EXCLUDED.asset_key, slot = EXCLUDED.slot, rule_type = EXCLUDED.rule_type,
                        rule_code = EXCLUDED.rule_code, rule_parameters = EXCLUDED.rule_parameters""")
                    .param("id", UUID.randomUUID()).param("code", text(r, "code")).param("type", text(r, "type"))
                    .param("title", text(r, "title")).param("desc", text(r, "description")).param("asset", text(r, "assetKey"))
                    .param("slot", text(r, "slot")).param("rt", text(r, "ruleType")).param("rc", text(r, "ruleCode"))
                    .param("params", json.writeValueAsString(r.path("ruleParameters"))).update();
        }
        return new Report(worldIds.size(), skillIds.size(), content.missions().size(), exercises, newVersions, unchanged, rewards);
    }

    private void insertExercise(UUID versionId, int position, JsonNode e, Map<String, UUID> skillIds) {
        String type = text(e, "type");
        ObjectNode publicPayload = json.createObjectNode();
        ObjectNode answerKey = json.createObjectNode();
        switch (type) {
            case "MCQ" -> {
                ArrayNode options = publicPayload.putArray("options");
                e.path("options").forEach(o -> options.addObject().put("id", text(o, "id")).put("label", text(o, "label")));
                answerKey.put("optionId", e.path("answer").path("optionId").asString());
            }
            case "NUMBER" -> {
                if (e.has("unit")) publicPayload.put("unit", text(e, "unit"));
                answerKey.put("number", AnswerNormalizer.canonicalNumber(
                        AnswerNormalizer.parseNumber(e.path("answer").path("number").asString()).orElseThrow()));
            }
            default -> {
                ArrayNode accepted = answerKey.putArray("accepted");
                e.path("answer").path("accepted").forEach(a -> accepted.add(AnswerNormalizer.normalizeText(a.asString())));
            }
        }
        jdbc.sql("""
                INSERT INTO exercise (id, mission_version_id, ordinal, skill_id, type, difficulty, instruction, prompt,
                    public_payload, answer_key, hint, explanation)
                VALUES (:id, :v, :o, :s, :type, :d, :i, :p, CAST(:pub AS jsonb), CAST(:key AS jsonb), :h, :x)""")
                .param("id", UUID.randomUUID()).param("v", versionId).param("o", position)
                .param("s", skillIds.get(text(e, "skill"))).param("type", type).param("d", e.path("difficulty").asInt())
                .param("i", text(e, "instruction")).param("p", text(e, "prompt"))
                .param("pub", json.writeValueAsString(publicPayload)).param("key", json.writeValueAsString(answerKey))
                .param("h", text(e, "hint")).param("x", text(e, "explanation")).update();
    }
}
