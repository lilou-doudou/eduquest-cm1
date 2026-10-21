package fr.eduquest.family;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class FamilyDtos {
    private FamilyDtos() {}

    public record CreateChildRequest(
            @NotBlank @Size(min = 2, max = 30) String nickname,
            @NotBlank @Size(max = 40) String avatarCode,
            @NotBlank @Pattern(regexp = "CM1") String grade) {}

    public record UpdateChildRequest(@Size(min = 2, max = 30) String nickname, @Size(max = 40) String avatarCode) {}

    public record DeleteChildRequest(@NotBlank String confirmation) {}

    public record ChildResponse(UUID id, String nickname, String avatarCode, String grade, Instant createdAt) {
        static ChildResponse of(ChildProfile c) {
            return new ChildResponse(c.getId(), c.getNickname(), c.getAvatarCode(), c.getGrade(), c.getCreatedAt());
        }
    }
}
