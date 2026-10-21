package fr.eduquest.learning;

import fr.eduquest.shared.ApiException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerEvaluatorTest {
    private final JsonMapper json = JsonMapper.builder().build();

    private JsonNode n(String s) { return json.readTree(s); }

    private static final String OPTIONS = "{\"options\":[{\"id\":\"a\",\"label\":\"blue\"},{\"id\":\"b\",\"label\":\"red\"},{\"id\":\"c\",\"label\":\"green\"}]}";

    @Test
    void mcqComparesStableIdentifiers() {
        var ok = AnswerEvaluator.evaluate(json, "MCQ", n("{\"optionId\":\"a\"}"), n(OPTIONS), n("{\"optionId\":\"a\"}"));
        var ko = AnswerEvaluator.evaluate(json, "MCQ", n("{\"optionId\":\"a\"}"), n(OPTIONS), n("{\"optionId\":\"b\"}"));
        assertThat(ok.correct()).isTrue();
        assertThat(ko.correct()).isFalse();
    }

    @Test
    void mcqUnknownOptionIsUnprocessable() {
        assertThatThrownBy(() -> AnswerEvaluator.evaluate(json, "MCQ", n("{\"optionId\":\"a\"}"), n(OPTIONS), n("{\"optionId\":\"z\"}")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("UNKNOWN_OPTION");
    }

    @Test
    void numberAcceptsCommaAndSpaces() {
        var r = AnswerEvaluator.evaluate(json, "NUMBER", n("{\"number\":\"1784\"}"), n("{}"), n("{\"number\":\" 1 784 \"}"));
        assertThat(r.correct()).isTrue();
        assertThat(r.canonical().path("number").asString()).isEqualTo("1784");
    }

    @Test
    void textUsesClosedListOfVariants() {
        JsonNode key = n("{\"accepted\":[\"thank you\",\"thanks\"]}");
        assertThat(AnswerEvaluator.evaluate(json, "TEXT", key, n("{}"), n("{\"text\":\"Thanks!\"}")).correct()).isTrue();
        assertThat(AnswerEvaluator.evaluate(json, "TEXT", key, n("{}"), n("{\"text\":\"thank\"}")).correct()).isFalse();
    }

    @Test
    void emptyAnswerIsNotEvaluated() {
        assertThatThrownBy(() -> AnswerEvaluator.evaluate(json, "TEXT", n("{\"accepted\":[\"cat\"]}"), n("{}"), n("{\"text\":\"   \"}")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("EMPTY_ANSWER");
    }

    @Test
    void extraOrMismatchedKeysAreRejected() {
        assertThatThrownBy(() -> AnswerEvaluator.evaluate(json, "NUMBER", n("{\"number\":\"5\"}"), n("{}"), n("{\"number\":\"5\",\"isCorrect\":true}")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ANSWER_TYPE_MISMATCH");
        assertThatThrownBy(() -> AnswerEvaluator.evaluate(json, "NUMBER", n("{\"number\":\"5\"}"), n("{}"), n("{\"optionId\":\"a\"}")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ANSWER_TYPE_MISMATCH");
        assertThatThrownBy(() -> AnswerEvaluator.evaluate(json, "NUMBER", n("{\"number\":\"5\"}"), n("{}"), n("{\"number\":5}")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ANSWER_TYPE_MISMATCH");
    }

    @Test
    void invalidNumberFormatIsUnprocessable() {
        assertThatThrownBy(() -> AnswerEvaluator.evaluate(json, "NUMBER", n("{\"number\":\"5\"}"), n("{}"), n("{\"number\":\"cinq\"}")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("INVALID_NUMBER");
    }
}
