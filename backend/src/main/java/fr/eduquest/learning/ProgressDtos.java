package fr.eduquest.learning;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProgressDtos {
    private ProgressDtos() {}

    public record WorldSummary(String slug, String title, String subjectCode, int displayOrder, String availability,
                               String assetKey, String description, int missionCount, int completedMissions,
                               int stars, int maxStars, boolean completed) {}

    public record MissionSummary(UUID id, String code, int ordinal, String title, String state, int bestStars,
                                 int bestCorrectCount, int completedAttempts, UUID inProgressAttemptId) {}

    public record LevelSummary(int ordinal, String title, boolean unlocked, List<MissionSummary> missions) {}

    public record WorldDetail(WorldSummary world, List<LevelSummary> levels) {}

    public record BadgeView(String code, String title, String assetKey) {}

    public record ChildProgressResponse(UUID childId, String nickname, String avatarCode, int xpTotal, int adventurerLevel,
                                        int xpIntoLevel, int xpForNextLevel, int totalStars, List<BadgeView> badges,
                                        List<WorldSummary> worlds) {}

    /** Indicateur d'entraînement (§7.4), jamais une attestation de maîtrise. */
    public record SkillIndicator(String code, String label, String subjectCode, String status, Integer successRate,
                                 int evaluatedCount, int attemptCount, int hintsUsed, int correctWithHint) {}

    public record AttemptHistoryItem(UUID attemptId, String missionTitle, String worldTitle, String status, Instant startedAt,
                                     Instant completedAt, Integer correctCount, Integer stars, int hintsUsed) {}

    public record HistoryPage(List<AttemptHistoryItem> items, int page, int size, long totalElements) {}

    public record RevisionSuggestion(String kind, String label, String detail) {}

    public record ParentProgressResponse(UUID childId, String nickname, String avatarCode, int xpTotal, int adventurerLevel,
                                         int totalStars, List<WorldSummary> worlds, List<SkillIndicator> skills,
                                         HistoryPage history, List<RevisionSuggestion> suggestions) {}
}
