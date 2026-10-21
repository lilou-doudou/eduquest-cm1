package fr.eduquest.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {
    List<AttemptAnswer> findByAttemptId(UUID attemptId);

    Optional<AttemptAnswer> findByAttemptIdAndExerciseId(UUID attemptId, UUID exerciseId);
}
