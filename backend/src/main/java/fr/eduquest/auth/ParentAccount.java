package fr.eduquest.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parent_account")
public class ParentAccount {
    public enum Status { PENDING, ACTIVE, DELETING }

    @Id
    private UUID id;
    @Column(name = "email_normalized", nullable = false)
    private String emailNormalized;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;
    @Column(name = "inactivity_notified_at")
    private Instant inactivityNotifiedAt;

    protected ParentAccount() {}

    public ParentAccount(String emailNormalized, String passwordHash, Instant now) {
        this.id = UUID.randomUUID();
        this.emailNormalized = emailNormalized;
        this.passwordHash = passwordHash;
        this.status = Status.PENDING;
        this.createdAt = now;
        this.lastActivityAt = now;
    }

    public UUID getId() { return id; }
    public String getEmailNormalized() { return emailNormalized; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }

    public void markVerified(Instant now) {
        this.emailVerifiedAt = now;
        this.status = Status.ACTIVE;
    }

    public void changePassword(String hash) { this.passwordHash = hash; }

    public void recordLogin(Instant now) {
        this.lastLoginAt = now;
        recordActivity(now);
    }

    public void recordActivity(Instant now) {
        this.lastActivityAt = now;
        this.inactivityNotifiedAt = null;
    }
}
