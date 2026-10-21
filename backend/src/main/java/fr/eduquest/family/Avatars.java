package fr.eduquest.family;

import java.util.List;

/** Catalogue local d'avatars (illustrations SVG originales du projet, voir docs/assets-licenses.md). */
public final class Avatars {
    public static final List<String> CODES = List.of("fox", "owl", "turtle", "cat", "panda", "rabbit");

    private Avatars() {}

    public static boolean isValid(String code) {
        return code != null && CODES.contains(code);
    }
}
