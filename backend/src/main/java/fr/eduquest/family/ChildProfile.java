package fr.eduquest.family;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "child_profile")
public class ChildProfile {
    @Id
    private UUID id;
    @Column(name = "parent_id", nullable = false)
    private UUID parentId;
    @Column(nullable = false)
    private String nickname;
    @Column(name = "avatar_code", nullable = false)
    private String avatarCode;
    @Column(nullable = false)
    private String grade;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChildProfile() {}

    public ChildProfile(UUID parentId, String nickname, String avatarCode, Instant now) {
        this.id = UUID.randomUUID();
        this.parentId = parentId;
        this.nickname = nickname;
        this.avatarCode = avatarCode;
        this.grade = "CM1";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getParentId() { return parentId; }
    public String getNickname() { return nickname; }
    public String getAvatarCode() { return avatarCode; }
    public String getGrade() { return grade; }
    public Instant getCreatedAt() { return createdAt; }

    public void rename(String nickname, Instant now) {
        this.nickname = nickname;
        this.updatedAt = now;
    }

    public void changeAvatar(String avatarCode, Instant now) {
        this.avatarCode = avatarCode;
        this.updatedAt = now;
    }
}
