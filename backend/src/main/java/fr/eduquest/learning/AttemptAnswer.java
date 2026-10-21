package fr.eduquest.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

/** Une ligne par exercice et par partie ; elle peut exister pour un indice avant la réponse. */
@Entity
@Table(name = "attempt_answer")
public class AttemptAnswer {
    @Id
    private UUID id;
    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;
    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;
    @Column(name = "submitted_answer", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String submittedAnswer;
    @Column(name = "is_correct")
    private Boolean correct;
    @Column(name = "hint_used", nullable = false)
    private boolean hintUsed;
    @Column(name = "hint_requested_at")
    private Instant hintRequestedAt;
    @Column(name = "answered_at")
    private Instant answeredAt;

    protected AttemptAnswer() {}

    public AttemptAnswer(UUID attemptId, UUID exerciseId) {
        this.id = UUID.randomUUID();
        this.attemptId = attemptId;
        this.exerciseId = exerciseId;
    }

    public UUID getExerciseId() { return exerciseId; }
    public String getSubmittedAnswer() { return submittedAnswer; }
    public Boolean getCorrect() { return correct; }
    public boolean isHintUsed() { return hintUsed; }
    public Instant getAnsweredAt() { return answeredAt; }
    public boolean isAnswered() { return answeredAt != null; }

    public void useHint(Instant now) {
        if (!hintUsed) {
            hintUsed = true;
            hintRequestedAt = now;
        }
    }

    public void answer(String canonicalJson, boolean isCorrect, Instant now) {
        this.submittedAnswer = canonicalJson;
        this.correct = isCorrect;
        this.answeredAt = now;
    }
}
