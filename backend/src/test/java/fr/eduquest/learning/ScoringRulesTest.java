package fr.eduquest.learning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoringRulesTest {

    @ParameterizedTest
    @CsvSource({"0,0", "1,0", "2,0", "3,1", "4,2", "5,3"})
    void starGrid(int correct, int stars) {
        assertThat(ScoringRules.stars(correct)).isEqualTo(stars);
    }

    @Test
    void rejectsImpossibleScores() {
        assertThatThrownBy(() -> ScoringRules.stars(6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void specExamplesForXp() {
        MissionProgress p = new MissionProgress(UUID.randomUUID(), UUID.randomUUID());
        Instant now = Instant.now();
        assertThat(p.recordCompletion(3, 1, now)).as("première réussite 3/5").isEqualTo(50);
        assertThat(p.recordCompletion(3, 1, now)).as("nouveau 3/5").isZero();
        assertThat(p.recordCompletion(2, 0, now)).as("moins bien").isZero();
        assertThat(p.recordCompletion(5, 3, now)).as("amélioration à 5/5").isEqualTo(20);
        assertThat(p.getXpAwarded()).isEqualTo(70);
        assertThat(p.getBestStars()).isEqualTo(3);
        assertThat(p.recordCompletion(5, 3, now)).isZero();
    }

    @Test
    void firstTwoOutOfFiveGivesTwentyXpWithoutStar() {
        MissionProgress p = new MissionProgress(UUID.randomUUID(), UUID.randomUUID());
        assertThat(p.recordCompletion(2, 0, Instant.now())).isEqualTo(20);
        assertThat(p.getBestStars()).isZero();
        assertThat(p.recordCompletion(3, 1, Instant.now())).as("10 de plus + 20 de bonus étoile").isEqualTo(30);
    }

    @Test
    void maxima() {
        assertThat(ScoringRules.xpTarget(5, 3)).isEqualTo(ScoringRules.MAX_XP_PER_MISSION);
        assertThat(12 * ScoringRules.MAX_XP_PER_MISSION).isEqualTo(840);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "99,1", "100,2", "199,2", "840,9"})
    void adventurerLevel(int xp, int level) {
        assertThat(ScoringRules.adventurerLevel(xp)).isEqualTo(level);
    }
}
