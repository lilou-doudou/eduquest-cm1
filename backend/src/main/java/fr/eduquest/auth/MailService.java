package fr.eduquest.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** E-mails strictement transactionnels. Ni l'adresse ni le jeton ne sont journalisés. */
@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender sender;
    private final String from;
    private final String baseUrl;

    public MailService(JavaMailSender sender,
                       @Value("${eduquest.mail.from}") String from,
                       @Value("${eduquest.base-url}") String baseUrl) {
        this.sender = sender;
        this.from = from;
        this.baseUrl = baseUrl;
    }

    public void sendVerification(String to, String token) {
        send(to, "EduQuest CM1 — confirme ton adresse e-mail",
                "Bonjour,\n\nPour activer le compte familial EduQuest CM1, ouvre ce lien (valable 24 heures) :\n"
                        + baseUrl + "/verifier-email?token=" + token
                        + "\n\nSi tu n'es pas à l'origine de cette demande, ignore ce message.\n");
    }

    public void sendPasswordReset(String to, String token) {
        send(to, "EduQuest CM1 — réinitialisation du mot de passe",
                "Bonjour,\n\nPour choisir un nouveau mot de passe, ouvre ce lien (valable 30 minutes) :\n"
                        + baseUrl + "/reinitialiser-mot-de-passe?token=" + token
                        + "\n\nSi tu n'es pas à l'origine de cette demande, ignore ce message : ton mot de passe reste inchangé.\n");
    }

    public void sendInactivityNotice(String to) {
        send(to, "EduQuest CM1 — compte inactif",
                "Bonjour,\n\nLe compte familial EduQuest CM1 associé à cette adresse n'a pas été utilisé depuis 12 mois.\n"
                        + "Sans connexion d'ici 30 jours, le compte et toutes les données des enfants seront supprimés.\n"
                        + "Pour le conserver, il suffit de te connecter : " + baseUrl + "/connexion\n");
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            sender.send(message);
        } catch (MailException e) {
            log.warn("Échec d'envoi d'un e-mail transactionnel ({})", e.getClass().getSimpleName());
        }
    }
}
