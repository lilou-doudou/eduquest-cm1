package fr.eduquest.auth;

import fr.eduquest.family.ChildProfile;
import fr.eduquest.family.ChildService;
import fr.eduquest.privacy.PrivacyNotice;
import fr.eduquest.privacy.PrivacyService;
import fr.eduquest.shared.ApiException;
import fr.eduquest.shared.AuditService;
import fr.eduquest.shared.Hashing;
import fr.eduquest.shared.RateLimiter;
import fr.eduquest.shared.Role;
import fr.eduquest.shared.SessionManager;
import fr.eduquest.shared.SessionPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    static final Duration VERIFY_TTL = Duration.ofHours(24);
    static final Duration RESET_TTL = Duration.ofMinutes(30);
    public static final Duration RECENT_AUTH = Duration.ofMinutes(5);

    private final ParentAccountRepository accounts;
    private final AccountTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final SessionManager sessions;
    private final RateLimiter rateLimiter;
    private final AuditService audit;
    private final ChildService children;
    private final PrivacyService privacy;
    private final Clock clock;
    private final boolean requireVerifiedEmail;
    private final String dummyHash;

    public AuthService(ParentAccountRepository accounts, AccountTokenRepository tokens, PasswordEncoder encoder,
                       MailService mail, SessionManager sessions, RateLimiter rateLimiter, AuditService audit,
                       ChildService children, PrivacyService privacy, Clock clock,
                       @Value("${eduquest.auth.require-verified-email:true}") boolean requireVerifiedEmail) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mail = mail;
        this.sessions = sessions;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
        this.children = children;
        this.privacy = privacy;
        this.clock = clock;
        this.requireVerifiedEmail = requireVerifiedEmail;
        // Hachage factice : une adresse inconnue coûte le même temps qu'un mauvais mot de passe.
        this.dummyHash = encoder.encode("eduquest-timing-equalizer");
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void register(String email, String password, String noticeVersion, String ip) {
        rateLimiter.consume("register:ip:" + ip, 10, Duration.ofHours(1));
        if (!PrivacyNotice.VERSION.equals(noticeVersion)) {
            throw ApiException.badRequest("NOTICE_VERSION_MISMATCH", "Merci de lire la version actuelle de la notice d'information.");
        }
        String normalized = normalizeEmail(email);
        Optional<ParentAccount> existing = accounts.findByEmailNormalized(normalized);
        if (existing.isPresent()) {
            // Réponse identique pour ne pas révéler l'existence du compte ; renvoi du lien si non vérifié.
            ParentAccount account = existing.get();
            if (account.getEmailVerifiedAt() == null) {
                rateLimiter.consume("verify-mail:" + normalized, 3, Duration.ofHours(1));
                issueVerification(account);
            }
            return;
        }
        Instant now = clock.instant();
        ParentAccount account = accounts.save(new ParentAccount(normalized, encoder.encode(password), now));
        privacy.recordAccountNotice(account.getId(), noticeVersion);
        issueVerification(account);
        audit.record("ACCOUNT_REGISTERED", account.getId(), "SUCCESS");
    }

    private void issueVerification(ParentAccount account) {
        Instant now = clock.instant();
        tokens.invalidateAll(account.getId(), AccountToken.Type.VERIFY_EMAIL, now);
        String raw = Hashing.randomToken();
        tokens.save(new AccountToken(account.getId(), AccountToken.Type.VERIFY_EMAIL, Hashing.sha256Hex(raw), now, now.plus(VERIFY_TTL)));
        mail.sendVerification(account.getEmailNormalized(), raw);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        Instant now = clock.instant();
        AccountToken token = tokens.findByTokenHash(Hashing.sha256Hex(rawToken))
                .filter(t -> t.getType() == AccountToken.Type.VERIFY_EMAIL && t.isUsable(now))
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN", "Ce lien n'est plus valide. Demande un nouveau lien."));
        token.markUsed(now);
        ParentAccount account = accounts.findById(token.getParentId()).orElseThrow(ApiException::notFound);
        account.markVerified(now);
        audit.record("EMAIL_VERIFIED", account.getId(), "SUCCESS");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void login(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        String normalized = normalizeEmail(email);
        String ip = request.getRemoteAddr();
        rateLimiter.consume("login:ip:" + ip, 30, Duration.ofMinutes(15));
        rateLimiter.consume("login:account:" + normalized, 10, Duration.ofMinutes(15));
        ParentAccount account = checkCredentials(normalized, password);
        if (requireVerifiedEmail && account.getEmailVerifiedAt() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Confirme d'abord ton adresse e-mail grâce au lien reçu.");
        }
        Instant now = clock.instant();
        account.recordLogin(now);
        sessions.establish(new SessionPrincipal(Role.PARENT, account.getId(), null, now, now), request, response);
        audit.record("LOGIN", account.getId(), "SUCCESS");
    }

    private ParentAccount checkCredentials(String normalizedEmail, String password) {
        Optional<ParentAccount> found = accounts.findByEmailNormalized(normalizedEmail)
                .filter(a -> a.getStatus() != ParentAccount.Status.DELETING);
        if (found.isEmpty()) {
            encoder.matches(password, dummyHash);
            audit.record("LOGIN", null, "FAILURE");
            throw invalidCredentials();
        }
        ParentAccount account = found.get();
        if (!encoder.matches(password, account.getPasswordHash())) {
            audit.record("LOGIN", account.getId(), "FAILURE");
            throw invalidCredentials();
        }
        if (encoder.upgradeEncoding(account.getPasswordHash())) {
            account.changePassword(encoder.encode(password));
        }
        return account;
    }

    private static ApiException invalidCredentials() {
        return ApiException.unauthorized("INVALID_CREDENTIALS", "Adresse e-mail ou mot de passe incorrect.");
    }

    @Transactional
    public void forgotPassword(String email, String ip) {
        String normalized = normalizeEmail(email);
        rateLimiter.consume("forgot:ip:" + ip, 10, Duration.ofHours(1));
        rateLimiter.consume("forgot:account:" + normalized, 3, Duration.ofHours(1));
        accounts.findByEmailNormalized(normalized)
                .filter(a -> a.getStatus() != ParentAccount.Status.DELETING)
                .ifPresent(account -> {
                    Instant now = clock.instant();
                    tokens.invalidateAll(account.getId(), AccountToken.Type.RESET_PASSWORD, now);
                    String raw = Hashing.randomToken();
                    tokens.save(new AccountToken(account.getId(), AccountToken.Type.RESET_PASSWORD, Hashing.sha256Hex(raw), now, now.plus(RESET_TTL)));
                    mail.sendPasswordReset(account.getEmailNormalized(), raw);
                });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword, String ip) {
        rateLimiter.consume("reset:ip:" + ip, 20, Duration.ofHours(1));
        Instant now = clock.instant();
        AccountToken token = tokens.findByTokenHash(Hashing.sha256Hex(rawToken))
                .filter(t -> t.getType() == AccountToken.Type.RESET_PASSWORD && t.isUsable(now))
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN", "Ce lien n'est plus valide. Demande un nouveau lien."));
        token.markUsed(now);
        ParentAccount account = accounts.findById(token.getParentId()).orElseThrow(ApiException::notFound);
        account.changePassword(encoder.encode(newPassword));
        // Cliquer sur le lien reçu prouve aussi la possession de l'adresse.
        if (account.getEmailVerifiedAt() == null) account.markVerified(now);
        tokens.invalidateAll(account.getId(), AccountToken.Type.RESET_PASSWORD, now);
        sessions.revokeFamilySessions(account.getId(), null);
        audit.record("PASSWORD_RESET", account.getId(), "SUCCESS");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void reauthenticate(SessionPrincipal current, String password, HttpServletRequest request, HttpServletResponse response) {
        rateLimiter.consume("reauth:account:" + current.parentId(), 10, Duration.ofMinutes(15));
        ParentAccount account = accounts.findById(current.parentId()).orElseThrow(ApiException::notFound);
        if (!encoder.matches(password, account.getPasswordHash())) {
            audit.record("REAUTHENTICATE", account.getId(), "FAILURE");
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Mot de passe incorrect.");
        }
        sessions.store(current.withAuthenticatedAt(clock.instant()), request, response);
    }

    /** Passage en mode enfant : la session adulte est remplacée par une session limitée. */
    @Transactional
    public ChildProfile enterChildMode(SessionPrincipal current, UUID childId, HttpServletRequest request, HttpServletResponse response) {
        ChildProfile child = children.getOwned(current.parentId(), childId);
        Instant now = clock.instant();
        sessions.establish(new SessionPrincipal(Role.CHILD, current.parentId(), child.getId(), null, now), request, response);
        audit.record("CHILD_MODE", current.parentId(), "SUCCESS");
        return child;
    }

    /** Retour à l'espace adulte depuis le mode enfant : e-mail et mot de passe du parent propriétaire exigés. */
    @Transactional(noRollbackFor = ApiException.class)
    public void returnToParent(SessionPrincipal current, String email, String password,
                               HttpServletRequest request, HttpServletResponse response) {
        rateLimiter.consume("parent-session:ip:" + request.getRemoteAddr(), 10, Duration.ofMinutes(15));
        rateLimiter.consume("parent-session:account:" + current.parentId(), 5, Duration.ofMinutes(15));
        ParentAccount account = accounts.findById(current.parentId()).orElseThrow(ApiException::notFound);
        boolean emailMatches = account.getEmailNormalized().equals(normalizeEmail(email));
        boolean passwordMatches = encoder.matches(password, account.getPasswordHash());
        if (!emailMatches || !passwordMatches) {
            audit.record("PARENT_MODE", account.getId(), "FAILURE");
            throw invalidCredentials();
        }
        Instant now = clock.instant();
        account.recordLogin(now);
        sessions.establish(new SessionPrincipal(Role.PARENT, account.getId(), null, now, now), request, response);
        audit.record("PARENT_MODE", account.getId(), "SUCCESS");
    }

    @Transactional(readOnly = true)
    public AuthDtos.MeResponse me(SessionPrincipal p) {
        if (p.role() == Role.CHILD) {
            ChildProfile child = children.getOwned(p.parentId(), p.childId());
            return new AuthDtos.MeResponse("CHILD", null,
                    new AuthDtos.ChildInfo(child.getId(), child.getNickname(), child.getAvatarCode()));
        }
        ParentAccount account = accounts.findById(p.parentId()).orElseThrow(ApiException::notFound);
        return new AuthDtos.MeResponse("PARENT",
                new AuthDtos.ParentInfo(account.getId(), account.getEmailNormalized(), account.getEmailVerifiedAt() != null), null);
    }

    public void requireRecentAuthentication(SessionPrincipal p) {
        if (p.authenticatedAt() == null || p.authenticatedAt().plus(RECENT_AUTH).isBefore(clock.instant())) {
            throw ApiException.forbidden("REAUTHENTICATION_REQUIRED", "Pour cette action, confirme ton mot de passe.");
        }
    }
}
