package fr.eduquest.learning;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Normalisation déterministe des réponses (§7.2). Aucun flottant binaire : les nombres sont
 * comparés en {@link BigDecimal}.
 */
public final class AnswerNormalizer {
    private static final Pattern PLAIN_DECIMAL = Pattern.compile("-?\\d{1,15}(\\.\\d{1,6})?");
    /** Séparateur de milliers « à la française » : 1 784 ou 12 500 (espace, espace insécable ou fine). */
    private static final Pattern GROUPED_DECIMAL = Pattern.compile("-?\\d{1,3}( \\d{3})+(\\.\\d{1,6})?");

    private AnswerNormalizer() {}

    /** @return la valeur décimale, ou vide si le format est invalide */
    public static Optional<BigDecimal> parseNumber(String raw) {
        if (raw == null) return Optional.empty();
        String s = raw.replace(' ', ' ').replace(' ', ' ').strip().replace(',', '.');
        if (s.isEmpty() || s.length() > 30) return Optional.empty();
        if (PLAIN_DECIMAL.matcher(s).matches()) return Optional.of(new BigDecimal(s));
        if (GROUPED_DECIMAL.matcher(s).matches()) return Optional.of(new BigDecimal(s.replace(" ", "")));
        return Optional.empty();
    }

    /** Forme canonique d'un nombre (sans zéros superflus, sans exposant). */
    public static String canonicalNumber(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) stripped = stripped.setScale(0);
        return stripped.toPlainString();
    }

    /**
     * Texte anglais : Unicode NFC, apostrophes typographiques ramenées à ', espaces périphériques supprimés,
     * espaces internes regroupés, casse ignorée, ponctuation finale (. ! ?) ignorée.
     */
    public static String normalizeText(String raw) {
        if (raw == null) return "";
        String s = Normalizer.normalize(raw, Normalizer.Form.NFC)
                .replace('’', '\'').replace('‘', '\'').replace('ʼ', '\'')
                .replace(' ', ' ')
                .strip()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        s = s.replaceAll("[.!?]+$", "").strip();
        return s;
    }
}
