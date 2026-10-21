package fr.eduquest.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static fr.eduquest.catalog.UnlockRules.State.AVAILABLE;
import static fr.eduquest.catalog.UnlockRules.State.COMPLETED;
import static fr.eduquest.catalog.UnlockRules.State.LOCKED;
import static org.assertj.core.api.Assertions.assertThat;

class UnlockRulesTest {
    private final List<UUID> missions = IntStream.range(0, 6).mapToObj(i -> UUID.randomUUID()).toList();

    @Test
    void onlyFirstMissionIsOpenAtStart() {
        assertThat(UnlockRules.states(missions, Map.of())).containsExactly(AVAILABLE, LOCKED, LOCKED, LOCKED, LOCKED, LOCKED);
    }

    @Test
    void zeroStarDoesNotUnlockNext() {
        assertThat(UnlockRules.states(missions, Map.of(missions.get(0), 0))).startsWith(AVAILABLE, LOCKED);
    }

    @Test
    void levelOpensWhenBothPreviousMissionsHaveAStar() {
        var stars = Map.of(missions.get(0), 1, missions.get(1), 2);
        assertThat(UnlockRules.states(missions, stars)).containsExactly(COMPLETED, COMPLETED, AVAILABLE, LOCKED, LOCKED, LOCKED);
    }

    @Test
    void worldCompletedWhenAllSixHaveAStar() {
        var stars = new java.util.HashMap<UUID, Integer>();
        missions.forEach(m -> stars.put(m, 1));
        assertThat(UnlockRules.worldCompleted(missions, stars)).isTrue();
        stars.put(missions.get(5), 0);
        assertThat(UnlockRules.worldCompleted(missions, stars)).isFalse();
    }
}
