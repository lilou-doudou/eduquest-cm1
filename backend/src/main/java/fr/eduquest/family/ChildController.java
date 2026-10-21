package fr.eduquest.family;

import fr.eduquest.auth.AuthService;
import fr.eduquest.learning.ProgressService;
import fr.eduquest.learning.ProgressDtos;
import fr.eduquest.shared.ApiException;
import fr.eduquest.shared.CurrentSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/children")
public class ChildController {
    private final ChildService children;
    private final AuthService auth;
    private final ProgressService progress;

    public ChildController(ChildService children, AuthService auth, ProgressService progress) {
        this.children = children;
        this.auth = auth;
        this.progress = progress;
    }

    @GetMapping
    public List<FamilyDtos.ChildResponse> list() {
        return children.list(CurrentSession.parentId()).stream().map(FamilyDtos.ChildResponse::of).toList();
    }

    @PostMapping
    public ResponseEntity<FamilyDtos.ChildResponse> create(@Valid @RequestBody FamilyDtos.CreateChildRequest body) {
        ChildProfile child = children.create(CurrentSession.parentId(), body.nickname(), body.avatarCode());
        return ResponseEntity.created(URI.create("/api/v1/children/" + child.getId())).body(FamilyDtos.ChildResponse.of(child));
    }

    @PatchMapping("/{id}")
    public FamilyDtos.ChildResponse update(@PathVariable UUID id, @Valid @RequestBody FamilyDtos.UpdateChildRequest body) {
        return FamilyDtos.ChildResponse.of(children.update(CurrentSession.parentId(), id, body.nickname(), body.avatarCode()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @Valid @RequestBody FamilyDtos.DeleteChildRequest body) {
        UUID parentId = CurrentSession.parentId();
        children.getOwned(parentId, id);
        auth.requireRecentAuthentication(CurrentSession.principal());
        if (!"SUPPRIMER".equals(body.confirmation())) {
            throw ApiException.badRequest("CONFIRMATION_REQUIRED", "Écris SUPPRIMER pour confirmer.");
        }
        children.delete(parentId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/progress")
    public ProgressDtos.ParentProgressResponse progress(@PathVariable UUID id,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        ChildProfile child = children.getOwned(CurrentSession.parentId(), id);
        return progress.forParent(child, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }
}
