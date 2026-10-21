package fr.eduquest.learning;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SkillStatusTest {
    @ParameterizedTest
    @CsvSource({"4,4,DISCOVERY", "5,2,TO_REVIEW", "5,3,IN_PROGRESS", "10,7,IN_PROGRESS", "5,4,CONSISTENT", "10,8,CONSISTENT", "10,5,TO_REVIEW"})
    void thresholds(int evaluated, int correct, String status) {
        assertThat(ProgressService.status(evaluated, correct)).isEqualTo(status);
    }
}
