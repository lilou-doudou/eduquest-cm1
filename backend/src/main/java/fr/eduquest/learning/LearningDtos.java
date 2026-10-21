package fr.eduquest.learning;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

public final class LearningDtos {
    private LearningDtos() {}

    public record AnswerRequest(JsonNode answer) {}

    /** Question publique : aucune bonne réponse ni explication. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuestionView(UUID id, int position, int total, String type, String instruction, String prompt,
                               List<Option> options, String unit, boolean hintUsed, String hint) {}

    public record Option(String id, String label) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AttemptView(UUID attemptId, UUID missionId, String missionTitle, String worldSlug, String status,
                              int answeredCount, int total, boolean readyToComplete, QuestionView currentQuestion,
                              Summary summary) {}

    public record HintResponse(UUID exerciseId, String hint) {}

    public record Correction(UUID exerciseId, boolean correct, boolean hintUsed, JsonNode expectedAnswer,
                             String explanation, int answeredCount, boolean readyToComplete) {}

    public record SkillResult(String code, String label, int correct, int total) {}

    public record Summary(UUID attemptId, UUID missionId, int correctCount, int questionCount, int stars, int hintsUsed,
                          int xpDelta, int xpTotal, int adventurerLevel, List<String> newRewards,
                          List<UUID> unlockedMissionIds, List<SkillResult> skills, UUID nextMissionId) {}
}
