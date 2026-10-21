package fr.eduquest.learning;

/** Gamification déterministe (§8). */
public final class ScoringRules {
    public static final int QUESTIONS_PER_MISSION = 5;
    public static final int MAX_XP_PER_MISSION = 70;

    private ScoringRules() {}

    /** 0–2 bonnes réponses = 0 étoile ; 3 = 1 ; 4 = 2 ; 5 = 3. */
    public static int stars(int correctCount) {
        if (correctCount < 0 || correctCount > QUESTIONS_PER_MISSION) throw new IllegalArgumentException("correctCount");
        return Math.max(0, correctCount - 2);
    }

    /** Valeur maximale acquise pour une mission : 10 × meilleur score + 20 si au moins une étoile obtenue. */
    public static int xpTarget(int bestCorrectCount, int bestStars) {
        return 10 * bestCorrectCount + (bestStars >= 1 ? 20 : 0);
    }

    /** Seule la différence positive avec ce qui a déjà été attribué est versée. */
    public static int xpDelta(int alreadyAwarded, int bestCorrectCount, int bestStars) {
        return Math.max(0, xpTarget(bestCorrectCount, bestStars) - alreadyAwarded);
    }

    /** Rang d'aventurier cosmétique : 1 + floor(XP / 100). */
    public static int adventurerLevel(int xpTotal) {
        return 1 + Math.floorDiv(Math.max(0, xpTotal), 100);
    }
}
