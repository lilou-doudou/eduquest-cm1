package fr.eduquest.family;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChildProfileRepository extends JpaRepository<ChildProfile, UUID> {
    List<ChildProfile> findByParentIdOrderByCreatedAtAsc(UUID parentId);

    Optional<ChildProfile> findByIdAndParentId(UUID id, UUID parentId);

    long countByParentId(UUID parentId);
}
