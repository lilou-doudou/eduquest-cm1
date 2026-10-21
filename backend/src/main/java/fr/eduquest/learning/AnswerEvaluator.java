package fr.eduquest.learning;

import fr.eduquest.shared.ApiException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/** Correction serveur des trois types d'exercice. */
public final class AnswerEvaluator {
    public record Evaluation(boolean correct, ObjectNode canonical) {}

    private AnswerEvaluator() {}

    /**
     * Valide puis corrige une réponse. Une réponse vide (400) ou d'un format incompatible (422)
     * n'est pas évaluée et ne consomme pas la question.
     */
    public static Evaluation evaluate(JsonMapper json, String type, JsonNode answerKey, JsonNode publicPayload, JsonNode answer) {
        if (answer == null || answer.isNull() || !answer.isObject()) {
            throw ApiException.badRequest("ANSWER_REQUIRED", "Réponse manquante.");
        }
        String expectedField = switch (type) {
            case "MCQ" -> "optionId";
            case "NUMBER" -> "number";
            case "TEXT" -> "text";
            default -> throw new IllegalStateException("Type inconnu " + type);
        };
        Set<String> fields = new HashSet<>(answer.propertyNames());
        if (!fields.equals(Set.of(expectedField))) {
            throw ApiException.unprocessable("ANSWER_TYPE_MISMATCH", "Le format de la réponse ne correspond pas à la question.");
        }
        JsonNode value = answer.get(expectedField);
        if (!value.isString()) {
            throw ApiException.unprocessable("ANSWER_TYPE_MISMATCH", "Le format de la réponse ne correspond pas à la question.");
        }
        String raw = value.asString();
        if (raw.isBlank()) throw ApiException.badRequest("EMPTY_ANSWER", "Écris ou choisis une réponse avant de valider.");
        if (raw.length() > 100) throw ApiException.unprocessable("ANSWER_TOO_LONG", "Réponse trop longue.");

        ObjectNode canonical = json.createObjectNode();
        switch (type) {
            case "MCQ" -> {
                boolean known = false;
                for (JsonNode option : publicPayload.path("options")) {
                    if (option.path("id").asString().equals(raw)) known = true;
                }
                if (!known) throw ApiException.unprocessable("UNKNOWN_OPTION", "Cette proposition n'existe pas.");
                canonical.put("optionId", raw);
                return new Evaluation(raw.equals(answerKey.path("optionId").asString()), canonical);
            }
            case "NUMBER" -> {
                BigDecimal submitted = AnswerNormalizer.parseNumber(raw)
                        .orElseThrow(() -> ApiException.unprocessable("INVALID_NUMBER", "Écris un nombre, par exemple 377 ou 12,5."));
                BigDecimal expected = new BigDecimal(answerKey.path("number").asString());
                canonical.put("number", AnswerNormalizer.canonicalNumber(submitted));
                return new Evaluation(submitted.compareTo(expected) == 0, canonical);
            }
            default -> {
                String normalized = AnswerNormalizer.normalizeText(raw);
                if (normalized.isEmpty()) throw ApiException.badRequest("EMPTY_ANSWER", "Écris une réponse avant de valider.");
                canonical.put("text", normalized);
                boolean correct = false;
                for (JsonNode accepted : answerKey.path("accepted")) {
                    if (accepted.asString().equals(normalized)) correct = true;
                }
                return new Evaluation(correct, canonical);
            }
        }
    }

    /** Bonne réponse affichable, renvoyée uniquement après la soumission. */
    public static ObjectNode expectedAnswer(JsonMapper json, String type, JsonNode answerKey) {
        ObjectNode node = json.createObjectNode();
        switch (type) {
            case "MCQ" -> node.put("optionId", answerKey.path("optionId").asString());
            case "NUMBER" -> node.put("number", answerKey.path("number").asString());
            default -> node.put("text", answerKey.path("accepted").path(0).asString());
        }
        return node;
    }
}
