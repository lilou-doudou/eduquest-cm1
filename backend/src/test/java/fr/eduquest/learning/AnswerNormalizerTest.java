package fr.eduquest.learning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerNormalizerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "377|377", " 377 |377", "12,5|12.5", "12.50|12.5", "4 205|4205", "1 000 000|1000000", "-3|-3", "007|7", "2,0|2", "100|100"
    })
    void parsesDecimalsWithCommaOrPoint(String raw, String canonical) {
        BigDecimal value = AnswerNormalizer.parseNumber(raw).orElseThrow();
        assertThat(AnswerNormalizer.canonicalNumber(value)).isEqualTo(canonical);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "abc", "12a", "1,2,3", "12 34", "1e3", "0x10", "--1", "1.", ",5"})
    void rejectsInvalidNumbers(String raw) {
        assertThat(AnswerNormalizer.parseNumber(raw)).isEmpty();
    }

    @Test
    void numberComparisonIsExactWithoutBinaryFloats() {
        BigDecimal a = AnswerNormalizer.parseNumber("0,3").orElseThrow();
        BigDecimal b = AnswerNormalizer.parseNumber("0.1").orElseThrow().add(new BigDecimal("0.2"));
        assertThat(a.compareTo(b)).isZero();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "  Thank   You |thank you", "CAT|cat", "I’m fine|i'm fine", "red.|red", "Hello!|hello", "sit  down|sit down"
    })
    void normalizesEnglishText(String raw, String expected) {
        assertThat(AnswerNormalizer.normalizeText(raw)).isEqualTo(expected);
    }

    @Test
    void doesNotAcceptSpellingMistakesAutomatically() {
        assertThat(AnswerNormalizer.normalizeText("gren")).isNotEqualTo(AnswerNormalizer.normalizeText("green"));
    }
}
