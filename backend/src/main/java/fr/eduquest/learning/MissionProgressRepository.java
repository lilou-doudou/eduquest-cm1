package fr.eduquest.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MissionProgressRepository extends JpaRepository<MissionProgress, UUID> {
    List<MissionProgress> findByChildId(UUID childId);

    Optional<MissionProgress> findByChildIdAndMissionId(UUID childId, UUID missionId);
}
