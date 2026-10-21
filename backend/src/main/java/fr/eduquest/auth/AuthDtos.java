package fr.eduquest.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotNull @Size(min = 12, max = 128, message = "Le mot de passe doit contenir entre 12 et 128 caractères.") String password,
            @NotBlank @Size(max = 32) String noticeVersion) {}

    public record LoginRequest(@NotBlank @Size(max = 254) String email, @NotNull @Size(max = 128) String password) {}

    public record TokenRequest(@NotBlank @Size(max = 100) String token) {}

    public record ForgotPasswordRequest(@NotBlank @Size(max = 254) String email) {}

    public record ResetPasswordRequest(
            @NotBlank @Size(max = 100) String token,
            @NotNull @Size(min = 12, max = 128, message = "Le mot de passe doit contenir entre 12 et 128 caractères.") String newPassword) {}

    public record ReauthenticateRequest(@NotNull @Size(max = 128) String password) {}

    public record ChildSessionRequest(@NotNull UUID childId) {}

    public record CsrfResponse(String headerName, String token) {}

    public record RoleResponse(String role) {}

    /** Contexte de la session : en mode enfant, aucune donnée du parent n'est renvoyée. */
    public record MeResponse(String role, ParentInfo parent, ChildInfo child) {}

    public record ParentInfo(UUID id, String email, boolean emailVerified) {}

    public record ChildInfo(UUID id, String nickname, String avatarCode) {}
}
