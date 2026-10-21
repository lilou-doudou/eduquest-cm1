package fr.eduquest.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {
    Optional<Attempt> findByIdAndChildId(UUID id, UUID childId);

    Optional<Attempt> findByChildIdAndMissionIdAndStatus(UUID childId, UUID missionId, Attempt.Status status);
}
