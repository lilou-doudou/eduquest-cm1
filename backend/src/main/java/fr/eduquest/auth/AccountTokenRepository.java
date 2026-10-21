package fr.eduquest.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {
    Optional<AccountToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE AccountToken t SET t.usedAt = :now WHERE t.parentId = :parentId AND t.type = :type AND t.usedAt IS NULL")
    int invalidateAll(UUID parentId, AccountToken.Type type, Instant now);
}
