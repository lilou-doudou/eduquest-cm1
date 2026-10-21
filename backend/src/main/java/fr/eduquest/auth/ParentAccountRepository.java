package fr.eduquest.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ParentAccountRepository extends JpaRepository<ParentAccount, UUID> {
    Optional<ParentAccount> findByEmailNormalized(String emailNormalized);
}
