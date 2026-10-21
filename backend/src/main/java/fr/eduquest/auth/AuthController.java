package fr.eduquest.auth;

import fr.eduquest.family.ChildProfile;
import fr.eduquest.shared.CurrentSession;
import fr.eduquest.shared.SessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final SessionManager sessions;

    public AuthController(AuthService auth, SessionManager sessions) {
        this.auth = auth;
        this.sessions = sessions;
    }

    @GetMapping("/csrf")
    public AuthDtos.CsrfResponse csrf(CsrfToken token) {
        return new AuthDtos.CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody AuthDtos.RegisterRequest body, HttpServletRequest request) {
        auth.register(body.email(), body.password(), body.noticeVersion(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verify(@Valid @RequestBody AuthDtos.TokenRequest body) {
        auth.verifyEmail(body.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public AuthDtos.RoleResponse login(@Valid @RequestBody AuthDtos.LoginRequest body,
                                       HttpServletRequest request, HttpServletResponse response) {
        auth.login(body.email(), body.password(), request, response);
        return new AuthDtos.RoleResponse("PARENT");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgot(@Valid @RequestBody AuthDtos.ForgotPasswordRequest body, HttpServletRequest request) {
        auth.forgotPassword(body.email(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@Valid @RequestBody AuthDtos.ResetPasswordRequest body, HttpServletRequest request) {
        auth.resetPassword(body.token(), body.newPassword(), request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthDtos.MeResponse me() {
        return auth.me(CurrentSession.principal());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        sessions.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reauthenticate")
    public ResponseEntity<Void> reauthenticate(@Valid @RequestBody AuthDtos.ReauthenticateRequest body,
                                               HttpServletRequest request, HttpServletResponse response) {
        auth.reauthenticate(CurrentSession.principal(), body.password(), request, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/child-session")
    public AuthDtos.MeResponse childSession(@Valid @RequestBody AuthDtos.ChildSessionRequest body,
                                            HttpServletRequest request, HttpServletResponse response) {
        ChildProfile child = auth.enterChildMode(CurrentSession.principal(), body.childId(), request, response);
        return new AuthDtos.MeResponse("CHILD", null,
                new AuthDtos.ChildInfo(child.getId(), child.getNickname(), child.getAvatarCode()));
    }

    @PostMapping("/parent-session")
    public AuthDtos.RoleResponse parentSession(@Valid @RequestBody AuthDtos.LoginRequest body,
                                               HttpServletRequest request, HttpServletResponse response) {
        auth.returnToParent(CurrentSession.principal(), body.email(), body.password(), request, response);
        return new AuthDtos.RoleResponse("PARENT");
    }
}
