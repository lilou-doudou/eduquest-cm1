package fr.eduquest.learning;

import fr.eduquest.shared.CurrentSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Routes d'apprentissage : l'enfant est toujours celui de la session, jamais un identifiant fourni. */
@RestController
@RequestMapping("/api/v1")
public class LearningController {
    private final AttemptService attempts;
    private final ProgressService progress;

    public LearningController(AttemptService attempts, ProgressService progress) {
        this.attempts = attempts;
        this.progress = progress;
    }

    @PostMapping("/missions/{missionId}/attempts")
    public ResponseEntity<LearningDtos.AttemptView> start(@PathVariable UUID missionId,
                                                          @RequestBody(required = false) Map<String, Object> body) {
        requireEmpty(body);
        AttemptService.StartResult result = attempts.start(CurrentSession.childId(), missionId);
        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/v1/attempts/" + result.view().attemptId())).body(result.view());
        }
        return ResponseEntity.ok(result.view());
    }

    @GetMapping("/attempts/{attemptId}")
    public LearningDtos.AttemptView get(@PathVariable UUID attemptId) {
        return attempts.get(CurrentSession.childId(), attemptId);
    }

    @PostMapping("/attempts/{attemptId}/answers/{exerciseId}/hint")
    public LearningDtos.HintResponse hint(@PathVariable UUID attemptId, @PathVariable UUID exerciseId,
                                          @RequestBody(required = false) Map<String, Object> body) {
        requireEmpty(body);
        return attempts.hint(CurrentSession.childId(), attemptId, exerciseId);
    }

    @PutMapping("/attempts/{attemptId}/answers/{exerciseId}")
    public LearningDtos.Correction answer(@PathVariable UUID attemptId, @PathVariable UUID exerciseId,
                                          @RequestBody JsonNode body) {
        // Seule la clé « answer » est acceptée : score, xp, isCorrect… envoyés par le client sont refusés.
        if (body == null || !body.isObject() || !Set.copyOf(body.propertyNames()).equals(Set.of("answer"))) {
            throw fr.eduquest.shared.ApiException.badRequest("MALFORMED_REQUEST", "Seul le champ « answer » est attendu.");
        }
        return attempts.answer(CurrentSession.childId(), attemptId, exerciseId, body.get("answer"));
    }

    @PostMapping("/attempts/{attemptId}/complete")
    public LearningDtos.Summary complete(@PathVariable UUID attemptId, @RequestBody(required = false) Map<String, Object> body) {
        requireEmpty(body);
        return attempts.complete(CurrentSession.childId(), attemptId);
    }

    @PostMapping("/attempts/{attemptId}/abandon")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void abandon(@PathVariable UUID attemptId, @RequestBody(required = false) Map<String, Object> body) {
        requireEmpty(body);
        attempts.abandon(CurrentSession.childId(), attemptId);
    }

    @GetMapping("/me/progress")
    public ProgressDtos.ChildProgressResponse myProgress() {
        return progress.forChild(CurrentSession.childId());
    }

    @GetMapping("/worlds")
    public java.util.List<ProgressDtos.WorldSummary> worlds() {
        return progress.worlds(CurrentSession.childId());
    }

    @GetMapping("/worlds/{slug}")
    public ProgressDtos.WorldDetail world(@PathVariable String slug) {
        return progress.world(CurrentSession.childId(), slug);
    }

    private static void requireEmpty(Map<String, Object> body) {
        if (body != null && !body.isEmpty()) {
            throw fr.eduquest.shared.ApiException.badRequest("MALFORMED_REQUEST", "Aucun champ n'est attendu.");
        }
    }
}
