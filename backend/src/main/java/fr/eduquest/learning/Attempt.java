package fr.eduquest.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attempt")
public class Attempt {
    public enum Status { IN_PROGRESS, COMPLETED, ABANDONED }

    @Id
    private UUID id;
    @Column(name = "child_id", nullable = false)
    private UUID childId;
    @Column(name = "mission_id", nullable = false)
    private UUID missionId;
    @Column(name = "mission_version_id", nullable = false)
    private UUID missionVersionId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "correct_count")
    private Integer correctCount;
    private Integer stars;
    @Column(name = "completion_summary", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String completionSummary;
    @Version
    private long version;

    protected Attempt() {}

    public Attempt(UUID childId, UUID missionId, UUID missionVersionId, Instant now) {
        this.id = UUID.randomUUID();
        this.childId = childId;
        this.missionId = missionId;
        this.missionVersionId = missionVersionId;
        this.status = Status.IN_PROGRESS;
        this.startedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getChildId() { return childId; }
    public UUID getMissionId() { return missionId; }
    public UUID getMissionVersionId() { return missionVersionId; }
    public Status getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Integer getCorrectCount() { return correctCount; }
    public Integer getStars() { return stars; }
    public String getCompletionSummary() { return completionSummary; }

    public void complete(int correctCount, int stars, String summaryJson, Instant now) {
        this.status = Status.COMPLETED;
        this.correctCount = correctCount;
        this.stars = stars;
        this.completionSummary = summaryJson;
        this.completedAt = now;
    }

    public void abandon() {
        this.status = Status.ABANDONED;
    }
}
