package fr.eduquest.support;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Boîte e-mail de test : capture les messages au lieu de les envoyer. */
public class CapturingMailSender implements JavaMailSender {
    public final List<SimpleMailMessage> sent = new CopyOnWriteArrayList<>();

    @Override public MimeMessage createMimeMessage() { return new MimeMessage((Session) null); }
    @Override public MimeMessage createMimeMessage(InputStream contentStream) { throw new UnsupportedOperationException(); }
    @Override public void send(MimeMessage... mimeMessages) { throw new UnsupportedOperationException(); }
    @Override public void send(SimpleMailMessage... simpleMessages) { sent.addAll(List.of(simpleMessages)); }

    /** Dernier jeton envoyé à cette adresse pour un lien donné (« verifier-email » ou « reinitialiser-mot-de-passe »). */
    public Optional<String> lastToken(String to, String linkPath) {
        Pattern p = Pattern.compile(linkPath + "\\?token=([A-Za-z0-9_-]+)");
        for (int i = sent.size() - 1; i >= 0; i--) {
            SimpleMailMessage m = sent.get(i);
            if (m.getTo() != null && List.of(m.getTo()).contains(to)) {
                Matcher matcher = p.matcher(m.getText());
                if (matcher.find()) return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }
}
