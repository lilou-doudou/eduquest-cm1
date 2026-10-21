package fr.eduquest.family;

import fr.eduquest.privacy.DeletionService;
import fr.eduquest.privacy.PrivacyService;
import fr.eduquest.shared.ApiException;
import fr.eduquest.shared.Locks;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class ChildService {
    public static final int MAX_CHILDREN = 5;

    private final ChildProfileRepository children;
    private final Locks locks;
    private final DeletionService deletion;
    private final PrivacyService privacy;
    private final Clock clock;

    public ChildService(ChildProfileRepository children, Locks locks, DeletionService deletion, PrivacyService privacy, Clock clock) {
        this.children = children;
        this.locks = locks;
        this.deletion = deletion;
        this.privacy = privacy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ChildProfile> list(UUID parentId) {
        return children.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    /** Propriété vérifiée côté serveur : un enfant d'une autre famille est indiscernable d'un enfant inexistant (404). */
    @Transactional(readOnly = true)
    public ChildProfile getOwned(UUID parentId, UUID childId) {
        return children.findByIdAndParentId(childId, parentId).orElseThrow(ApiException::notFound);
    }

    @Transactional
    public ChildProfile create(UUID parentId, String nickname, String avatarCode) {
        String cleaned = cleanNickname(nickname);
        requireAvatar(avatarCode);
        // Verrou du parent : le maximum de cinq profils est garanti même en cas de requêtes concurrentes.
        locks.lockParent(parentId);
        if (children.countByParentId(parentId) >= MAX_CHILDREN) {
            throw ApiException.conflict("CHILD_LIMIT_REACHED", "Un compte familial peut contenir au plus cinq profils.");
        }
        ChildProfile child = children.saveAndFlush(new ChildProfile(parentId, cleaned, avatarCode, clock.instant()));
        privacy.recordChildInformation(parentId, child.getId());
        return child;
    }

    @Transactional
    public ChildProfile update(UUID parentId, UUID childId, String nickname, String avatarCode) {
        ChildProfile child = getOwned(parentId, childId);
        if (nickname != null) child.rename(cleanNickname(nickname), clock.instant());
        if (avatarCode != null) {
            requireAvatar(avatarCode);
            child.changeAvatar(avatarCode, clock.instant());
        }
        return child;
    }

    @Transactional
    public void delete(UUID parentId, UUID childId) {
        getOwned(parentId, childId);
        deletion.deleteChild(childId);
    }

    static String cleanNickname(String raw) {
        String s = raw == null ? "" : raw.strip().replaceAll("\\s+", " ");
        if (s.length() < 2 || s.length() > 30) {
            throw ApiException.badRequest("INVALID_NICKNAME", "Le pseudonyme doit contenir entre 2 et 30 caractères.");
        }
        if (s.contains("@") || s.matches(".*\\d{6,}.*")) {
            throw ApiException.badRequest("INVALID_NICKNAME", "Choisis un pseudonyme sans adresse ni numéro.");
        }
        return s;
    }

    private static void requireAvatar(String code) {
        if (!Avatars.isValid(code)) throw ApiException.badRequest("INVALID_AVATAR", "Avatar inconnu.");
    }
}
