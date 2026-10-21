package fr.eduquest.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Jeton à usage unique. Seul le hachage SHA-256 est stocké, jamais le jeton brut. */
@Entity
@Table(name = "account_token")
public class AccountToken {
    public enum Type { VERIFY_EMAIL, RESET_PASSWORD }

    @Id
    private UUID id;
    @Column(name = "parent_id", nullable = false)
    private UUID parentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountToken() {}

    public AccountToken(UUID parentId, Type type, String tokenHash, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.parentId = parentId;
        this.type = type;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public UUID getParentId() { return parentId; }
    public Type getType() { return type; }

    public boolean isUsable(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) { this.usedAt = now; }
}
