package fr.eduquest.catalog;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Valide le corpus réel sans base de données : 4 mondes jouables × 6 missions × 5 exercices. */
class ContentCorpusTest {
    private final ContentImporter importer = new ContentImporter(null, JsonMapper.builder().build(), Clock.systemUTC());

    @Test
    void realCorpusPassesImportValidation() {
        ContentImporter.LoadedContent content = importer.load(Path.of("../content"));
        Map<String, Integer> missionsPerWorld = new HashMap<>();
        int exercises = 0;
        for (JsonNode m : content.missions()) {
            missionsPerWorld.merge(m.path("world").asString(), 1, Integer::sum);
            exercises += m.path("exercises").size();
        }
        assertThat(missionsPerWorld).containsOnly(
                Map.entry("iles-des-nombres", 6), Map.entry("royaume-des-mots", 6),
                Map.entry("foret-du-vivant", 6), Map.entry("cite-des-inventeurs", 6));
        assertThat(exercises).isEqualTo(120);
    }
}
