package fr.eduquest.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Progression par enfant et par mission stable (indépendante des versions de contenu). */
@Entity
@Table(name = "mission_progress")
public class MissionProgress {
    @Id
    private UUID id;
    @Column(name = "child_id", nullable = false)
    private UUID childId;
    @Column(name = "mission_id", nullable = false)
    private UUID missionId;
    @Column(name = "best_correct_count", nullable = false)
    private int bestCorrectCount;
    @Column(name = "best_stars", nullable = false)
    private int bestStars;
    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;
    @Column(name = "completed_attempts", nullable = false)
    private int completedAttempts;
    @Column(name = "last_completed_at")
    private Instant lastCompletedAt;

    protected MissionProgress() {}

    public MissionProgress(UUID childId, UUID missionId) {
        this.id = UUID.randomUUID();
        this.childId = childId;
        this.missionId = missionId;
    }

    public UUID getMissionId() { return missionId; }
    public int getBestCorrectCount() { return bestCorrectCount; }
    public int getBestStars() { return bestStars; }
    public int getXpAwarded() { return xpAwarded; }
    public int getCompletedAttempts() { return completedAttempts; }

    /**
     * Enregistre une partie terminée. Les meilleurs résultats ne diminuent jamais.
     * @return XP nouvellement attribués (différence positive uniquement)
     */
    public int recordCompletion(int correctCount, int stars, Instant now) {
        bestCorrectCount = Math.max(bestCorrectCount, correctCount);
        bestStars = Math.max(bestStars, stars);
        completedAttempts++;
        lastCompletedAt = now;
        int delta = ScoringRules.xpDelta(xpAwarded, bestCorrectCount, bestStars);
        xpAwarded += delta;
        return delta;
    }
}
