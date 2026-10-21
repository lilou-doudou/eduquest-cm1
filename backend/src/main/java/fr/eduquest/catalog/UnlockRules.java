package fr.eduquest.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Règles de déblocage (§8.1) : la première mission d'un monde est ouverte ; chaque mission suivante
 * s'ouvre quand la précédente a au moins une étoile. Les missions étant ordonnées niveau par niveau,
 * un niveau s'ouvre donc quand ses deux missions précédentes ont chacune au moins une étoile.
 */
public final class UnlockRules {
    public enum State { LOCKED, AVAILABLE, COMPLETED }

    private UnlockRules() {}

    /** @param orderedMissions missions d'un seul monde, triées par niveau puis par ordre */
    public static List<State> states(List<UUID> orderedMissions, Map<UUID, Integer> bestStars) {
        List<State> result = new ArrayList<>(orderedMissions.size());
        boolean previousStarred = true;
        for (UUID missionId : orderedMissions) {
            int stars = bestStars.getOrDefault(missionId, 0);
            if (!previousStarred) {
                result.add(State.LOCKED);
            } else {
                result.add(stars >= 1 ? State.COMPLETED : State.AVAILABLE);
            }
            previousStarred = previousStarred && stars >= 1;
        }
        return result;
    }

    public static boolean worldCompleted(List<UUID> orderedMissions, Map<UUID, Integer> bestStars) {
        return !orderedMissions.isEmpty() && orderedMissions.stream().allMatch(id -> bestStars.getOrDefault(id, 0) >= 1);
    }
}
