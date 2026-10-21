package fr.eduquest.privacy;

import fr.eduquest.auth.AuthService;
import fr.eduquest.shared.ApiException;
import fr.eduquest.shared.AuditService;
import fr.eduquest.shared.CurrentSession;
import fr.eduquest.shared.SessionManager;
import fr.eduquest.shared.SessionPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PrivacyController {
    private final PrivacyService privacy;
    private final DeletionService deletion;
    private final AuthService auth;
    private final SessionManager sessions;
    private final AuditService audit;

    public PrivacyController(PrivacyService privacy, DeletionService deletion, AuthService auth,
                             SessionManager sessions, AuditService audit) {
        this.privacy = privacy;
        this.deletion = deletion;
        this.auth = auth;
        this.sessions = sessions;
        this.audit = audit;
    }

    public record DeleteAccountRequest(@NotBlank String confirmation) {}

    @GetMapping("/privacy/notice")
    public Map<String, String> notice() {
        return Map.of("noticeVersion", PrivacyNotice.VERSION, "childInformationVersion", PrivacyNotice.CHILD_VERSION);
    }

    @GetMapping("/privacy/acknowledgements")
    public List<Map<String, Object>> acknowledgements() {
        return privacy.acknowledgements(CurrentSession.parentId());
    }

    @GetMapping("/privacy/export")
    public ResponseEntity<ObjectNode> export() {
        SessionPrincipal p = CurrentSession.principal();
        auth.requireRecentAuthentication(p);
        ObjectNode body = privacy.export(p.parentId());
        audit.record("DATA_EXPORT", p.parentId(), "SUCCESS");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"eduquest-export-" + LocalDate.now() + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest body, HttpServletRequest request) {
        SessionPrincipal p = CurrentSession.principal();
        auth.requireRecentAuthentication(p);
        if (!"SUPPRIMER".equals(body.confirmation())) {
            throw ApiException.badRequest("CONFIRMATION_REQUIRED", "Écris SUPPRIMER pour confirmer.");
        }
        deletion.deleteFamily(p.parentId());
        sessions.logout(request);
        audit.record("ACCOUNT_DELETED", null, "SUCCESS");
        return ResponseEntity.noContent().build();
    }
}
