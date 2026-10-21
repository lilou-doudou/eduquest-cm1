package fr.eduquest.learning;

import fr.eduquest.catalog.CatalogRepository;
import fr.eduquest.catalog.CatalogRepository.ExerciseRow;
import fr.eduquest.catalog.CatalogRepository.MissionRow;
import fr.eduquest.catalog.UnlockRules;
import fr.eduquest.learning.LearningDtos.AttemptView;
import fr.eduquest.learning.LearningDtos.Correction;
import fr.eduquest.learning.LearningDtos.QuestionView;
import fr.eduquest.learning.LearningDtos.Summary;
import fr.eduquest.rewards.RewardService;
import fr.eduquest.shared.ApiException;
import fr.eduquest.shared.Locks;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Moteur de mission. Le serveur est la seule autorité pour l'ordre des questions, la correction,
 * les déblocages et les gains. Ordre des verrous : enfant → partie → progression.
 */
@Service
public class AttemptService {
    public record StartResult(AttemptView view, boolean created) {}

    private final AttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final MissionProgressRepository progress;
    private final CatalogRepository catalog;
    private final RewardService rewards;
    private final Locks locks;
    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final Clock clock;

    public AttemptService(AttemptRepository attempts, AttemptAnswerRepository answers, MissionProgressRepository progress,
                          CatalogRepository catalog, RewardService rewards, Locks locks, JdbcClient jdbc,
                          JsonMapper json, Clock clock) {
        this.attempts = attempts;
        this.answers = answers;
        this.progress = progress;
        this.catalog = catalog;
        this.rewards = rewards;
        this.locks = locks;
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ démarrage / reprise

    @Transactional
    public StartResult start(UUID childId, UUID missionId) {
        lockChild(childId);
        var world = catalog.worldOfMission(missionId).orElseThrow(ApiException::notFound);
        if (!world.playable()) {
            throw ApiException.conflict("WORLD_NOT_AVAILABLE", "Ce monde sera bientôt disponible.");
        }
        MissionRow mission = catalog.publishedMission(missionId).orElseThrow(ApiException::notFound);
        Optional<Attempt> existing = attempts.findByChildIdAndMissionIdAndStatus(childId, missionId, Attempt.Status.IN_PROGRESS);
        if (existing.isPresent()) {
            return new StartResult(view(existing.get()), false);
        }
        if (missionState(childId, mission) == UnlockRules.State.LOCKED) {
            throw ApiException.conflict("MISSION_LOCKED", "Cette mission n'est pas encore débloquée.");
        }
        Attempt attempt = attempts.saveAndFlush(new Attempt(childId, missionId, mission.versionId(), clock.instant()));
        return new StartResult(view(attempt), true);
    }

    @Transactional(readOnly = true)
    public AttemptView get(UUID childId, UUID attemptId) {
        return view(owned(childId, attemptId));
    }

    // ------------------------------------------------------------------ indice

    @Transactional
    public LearningDtos.HintResponse hint(UUID childId, UUID attemptId, UUID exerciseId) {
        lockChild(childId);
        Attempt attempt = lockedOwned(childId, attemptId);
        ExerciseRow exercise = exerciseOf(attempt, exerciseId);
        AttemptAnswer row = answers.findByAttemptIdAndExerciseId(attemptId, exerciseId).orElse(null);
        if (row != null && row.isHintUsed()) {
            return new LearningDtos.HintResponse(exerciseId, exercise.hint());
        }
        requireInProgress(attempt);
        if (row != null && row.isAnswered()) {
            throw ApiException.conflict("ALREADY_ANSWERED", "Cette question a déjà été corrigée.");
        }
        requireCurrent(attempt, exerciseId);
        if (row == null) row = new AttemptAnswer(attemptId, exerciseId);
        row.useHint(clock.instant());
        answers.save(row);
        return new LearningDtos.HintResponse(exerciseId, exercise.hint());
    }

    // ------------------------------------------------------------------ réponse

    @Transactional
    public Correction answer(UUID childId, UUID attemptId, UUID exerciseId, JsonNode submitted) {
        lockChild(childId);
        Attempt attempt = lockedOwned(childId, attemptId);
        ExerciseRow exercise = exerciseOf(attempt, exerciseId);
        JsonNode answerKey = json.readTree(exercise.answerKey());
        JsonNode payload = json.readTree(exercise.publicPayload());
        // Validation avant tout : une réponse vide ou mal formée n'est jamais enregistrée.
        AnswerEvaluator.Evaluation evaluation = AnswerEvaluator.evaluate(json, exercise.type(), answerKey, payload, submitted);

        AttemptAnswer row = answers.findByAttemptIdAndExerciseId(attemptId, exerciseId).orElse(null);
        if (row != null && row.isAnswered()) {
            // Rejeu idempotent (réponse réseau perdue) : même réponse → même correction ; autre réponse → 409.
            if (json.readTree(row.getSubmittedAnswer()).equals(evaluation.canonical())) {
                return correction(attempt, exercise, row);
            }
            throw ApiException.conflict("ALREADY_ANSWERED", "Cette question a déjà été corrigée.");
        }
        requireInProgress(attempt);
        requireCurrent(attempt, exerciseId);
        if (row == null) row = new AttemptAnswer(attemptId, exerciseId);
        row.answer(json.writeValueAsString(evaluation.canonical()), evaluation.correct(), clock.instant());
        answers.saveAndFlush(row);
        return correction(attempt, exercise, row);
    }

    private Correction correction(Attempt attempt, ExerciseRow exercise, AttemptAnswer row) {
        int answered = answeredCount(attempt.getId());
        return new Correction(exercise.id(), Boolean.TRUE.equals(row.getCorrect()), row.isHintUsed(),
                AnswerEvaluator.expectedAnswer(json, exercise.type(), json.readTree(exercise.answerKey())),
                exercise.explanation(), answered,
                attempt.getStatus() == Attempt.Status.IN_PROGRESS && answered == ScoringRules.QUESTIONS_PER_MISSION);
    }

    // ------------------------------------------------------------------ clôture

    @Transactional
    public Summary complete(UUID childId, UUID attemptId) {
        lockChild(childId);
        Attempt attempt = lockedOwned(childId, attemptId);
        // État relu après acquisition des verrous : une clôture concurrente renvoie le bilan mémorisé.
        if (attempt.getStatus() == Attempt.Status.COMPLETED) {
            return json.readValue(attempt.getCompletionSummary(), Summary.class);
        }
        requireInProgress(attempt);
        List<ExerciseRow> exercises = catalog.exercises(attempt.getMissionVersionId());
        Map<UUID, AttemptAnswer> byExercise = answers.findByAttemptId(attemptId).stream()
                .filter(AttemptAnswer::isAnswered)
                .collect(Collectors.toMap(AttemptAnswer::getExerciseId, Function.identity()));
        if (byExercise.size() < exercises.size()) {
            throw ApiException.conflict("ATTEMPT_INCOMPLETE", "Réponds aux cinq questions avant de terminer la mission.");
        }
        int correct = (int) byExercise.values().stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
        int hints = (int) byExercise.values().stream().filter(AttemptAnswer::isHintUsed).count();
        int stars = ScoringRules.stars(correct);
        Instant now = clock.instant();

        UUID worldId = catalog.worldOfMission(attempt.getMissionId()).orElseThrow().id();
        List<UUID> worldMissions = catalog.publishedMissions(worldId).stream().map(MissionRow::id).toList();
        Map<UUID, Integer> starsBefore = bestStars(childId);
        List<UnlockRules.State> before = UnlockRules.states(worldMissions, starsBefore);

        MissionProgress mp = progress.findByChildIdAndMissionId(childId, attempt.getMissionId())
                .orElseGet(() -> new MissionProgress(childId, attempt.getMissionId()));
        int xpDelta = mp.recordCompletion(correct, stars, now);
        progress.saveAndFlush(mp);
        if (xpDelta > 0) {
            jdbc.sql("INSERT INTO xp_event (id, child_id, attempt_id, mission_id, amount, created_at) VALUES (:id, :c, :a, :m, :x, :t)")
                    .param("id", UUID.randomUUID()).param("c", childId).param("a", attemptId)
                    .param("m", attempt.getMissionId()).param("x", xpDelta)
                    .param("t", OffsetDateTime.ofInstant(now, java.time.ZoneOffset.UTC)).update();
        }
        int xpTotal = xpTotal(childId);

        Map<UUID, Integer> starsAfter = new HashMap<>(starsBefore);
        starsAfter.put(attempt.getMissionId(), mp.getBestStars());
        List<UnlockRules.State> after = UnlockRules.states(worldMissions, starsAfter);
        List<UUID> unlocked = new ArrayList<>();
        UUID next = null;
        for (int i = 0; i < worldMissions.size(); i++) {
            if (before.get(i) == UnlockRules.State.LOCKED && after.get(i) != UnlockRules.State.LOCKED) unlocked.add(worldMissions.get(i));
            if (next == null && worldMissions.get(i).equals(attempt.getMissionId()) && i + 1 < worldMissions.size()
                    && after.get(i + 1) != UnlockRules.State.LOCKED) {
                next = worldMissions.get(i + 1);
            }
        }

        List<String> newRewards = rewards.grantEarned(childId, attemptId);

        Map<String, int[]> perSkill = new LinkedHashMap<>();
        Map<String, String> labels = skillLabels();
        for (ExerciseRow e : exercises) {
            int[] counts = perSkill.computeIfAbsent(e.skillCode(), k -> new int[2]);
            counts[1]++;
            if (Boolean.TRUE.equals(byExercise.get(e.id()).getCorrect())) counts[0]++;
        }
        List<LearningDtos.SkillResult> skills = perSkill.entrySet().stream()
                .map(en -> new LearningDtos.SkillResult(en.getKey(), labels.get(en.getKey()), en.getValue()[0], en.getValue()[1]))
                .toList();

        Summary summary = new Summary(attemptId, attempt.getMissionId(), correct, exercises.size(), stars, hints,
                xpDelta, xpTotal, ScoringRules.adventurerLevel(xpTotal), newRewards, unlocked, skills, next);
        attempt.complete(correct, stars, json.writeValueAsString(summary), now);
        attempts.saveAndFlush(attempt);
        return summary;
    }

    @Transactional
    public void abandon(UUID childId, UUID attemptId) {
        lockChild(childId);
        Attempt attempt = lockedOwned(childId, attemptId);
        if (attempt.getStatus() == Attempt.Status.ABANDONED) return;
        if (attempt.getStatus() == Attempt.Status.COMPLETED) {
            throw ApiException.conflict("ATTEMPT_CLOSED", "Cette mission est déjà terminée.");
        }
        attempt.abandon();
        attempts.saveAndFlush(attempt);
    }

    // ------------------------------------------------------------------ outils

    private void lockChild(UUID childId) {
        if (!locks.lockChild(childId)) throw ApiException.unauthorized("SESSION_REQUIRED", "Profil introuvable.");
    }

    private Attempt owned(UUID childId, UUID attemptId) {
        return attempts.findByIdAndChildId(attemptId, childId).orElseThrow(ApiException::notFound);
    }

    private Attempt lockedOwned(UUID childId, UUID attemptId) {
        locks.lockAttempt(attemptId);
        return owned(childId, attemptId);
    }

    private ExerciseRow exerciseOf(Attempt attempt, UUID exerciseId) {
        return catalog.exercises(attempt.getMissionVersionId()).stream()
                .filter(e -> e.id().equals(exerciseId)).findFirst()
                .orElseThrow(ApiException::notFound);
    }

    private static void requireInProgress(Attempt attempt) {
        if (attempt.getStatus() != Attempt.Status.IN_PROGRESS) {
            throw ApiException.conflict("ATTEMPT_CLOSED", "Cette mission n'est plus en cours.");
        }
    }

    private void requireCurrent(Attempt attempt, UUID exerciseId) {
        ExerciseRow current = currentExercise(attempt).orElse(null);
        if (current == null || !current.id().equals(exerciseId)) {
            throw ApiException.conflict("NOT_CURRENT_QUESTION", "Seule la question en cours peut recevoir une réponse.");
        }
    }

    private Optional<ExerciseRow> currentExercise(Attempt attempt) {
        Map<UUID, AttemptAnswer> rows = answers.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(AttemptAnswer::getExerciseId, Function.identity()));
        return catalog.exercises(attempt.getMissionVersionId()).stream()
                .filter(e -> rows.get(e.id()) == null || !rows.get(e.id()).isAnswered())
                .findFirst();
    }

    private int answeredCount(UUID attemptId) {
        return (int) answers.findByAttemptId(attemptId).stream().filter(AttemptAnswer::isAnswered).count();
    }

    Map<UUID, Integer> bestStars(UUID childId) {
        Map<UUID, Integer> map = new HashMap<>();
        progress.findByChildId(childId).forEach(p -> map.put(p.getMissionId(), p.getBestStars()));
        return map;
    }

    private int xpTotal(UUID childId) {
        return jdbc.sql("SELECT COALESCE(SUM(xp_awarded), 0) FROM mission_progress WHERE child_id = :c")
                .param("c", childId).query(Integer.class).single();
    }

    private Map<String, String> skillLabels() {
        Map<String, String> map = new HashMap<>();
        jdbc.sql("SELECT code, label FROM skill").query((rs, i) -> map.put(rs.getString(1), rs.getString(2))).list();
        return map;
    }

    private UnlockRules.State missionState(UUID childId, MissionRow mission) {
        List<UUID> ids = catalog.publishedMissions(mission.worldId()).stream().map(MissionRow::id).toList();
        List<UnlockRules.State> states = UnlockRules.states(ids, bestStars(childId));
        return states.get(ids.indexOf(mission.id()));
    }

    private AttemptView view(Attempt attempt) {
        MissionRow mission = catalog.publishedMission(attempt.getMissionId()).orElseThrow();
        String title = catalog.missionVersionTitle(attempt.getMissionVersionId());
        int answered = answeredCount(attempt.getId());
        int total = ScoringRules.QUESTIONS_PER_MISSION;
        if (attempt.getStatus() == Attempt.Status.COMPLETED) {
            Summary summary = json.readValue(attempt.getCompletionSummary(), Summary.class);
            return new AttemptView(attempt.getId(), attempt.getMissionId(), title, mission.worldSlug(), "COMPLETED",
                    answered, total, false, null, summary);
        }
        QuestionView question = null;
        if (attempt.getStatus() == Attempt.Status.IN_PROGRESS) {
            question = currentExercise(attempt).map(e -> publicQuestion(attempt, e)).orElse(null);
        }
        return new AttemptView(attempt.getId(), attempt.getMissionId(), title, mission.worldSlug(), attempt.getStatus().name(),
                answered, total, attempt.getStatus() == Attempt.Status.IN_PROGRESS && answered == total, question, null);
    }

    private QuestionView publicQuestion(Attempt attempt, ExerciseRow e) {
        JsonNode payload = json.readTree(e.publicPayload());
        List<LearningDtos.Option> options = null;
        if (payload.has("options")) {
            options = new ArrayList<>();
            for (JsonNode o : payload.get("options")) {
                options.add(new LearningDtos.Option(o.get("id").asString(), o.get("label").asString()));
            }
        }
        String unit = payload.has("unit") ? payload.get("unit").asString() : null;
        boolean hintUsed = answers.findByAttemptIdAndExerciseId(attempt.getId(), e.id()).map(AttemptAnswer::isHintUsed).orElse(false);
        return new QuestionView(e.id(), e.ordinal(), ScoringRules.QUESTIONS_PER_MISSION, e.type(), e.instruction(), e.prompt(),
                options, unit, hintUsed, hintUsed ? e.hint() : null);
    }
}
