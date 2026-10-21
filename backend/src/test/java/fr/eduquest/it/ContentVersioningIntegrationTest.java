package fr.eduquest.it;

import fr.eduquest.catalog.ContentImporter;
import fr.eduquest.support.ApiClient;
import fr.eduquest.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Recettes 11 et B02 : import idempotent, versions immuables, validation du contenu. */
class ContentVersioningIntegrationTest extends IntegrationTest {

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path p : files.toList()) {
                Path target = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(target);
                else Files.copy(p, target);
            }
        }
    }

    @Test
    void reimportCreatesNoDuplicate() {
        long versions = jdbc.sql("SELECT count(*) FROM mission_version").query(Long.class).single();
        ContentImporter.Report report = importer.importFrom(CONTENT);
        assertThat(report.newVersions()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM mission_version").query(Long.class).single()).isEqualTo(versions);
        assertThat(jdbc.sql("SELECT count(*) FROM world").query(Long.class).single()).isEqualTo(6);
        assertThat(jdbc.sql("SELECT count(*) FROM mission").query(Long.class).single()).isEqualTo(36);
        assertThat(jdbc.sql("SELECT count(*) FROM exercise e JOIN mission_version v ON v.id = e.mission_version_id WHERE v.status = 'PUBLISHED'")
                .query(Long.class).single()).isEqualTo(180);
    }

    @Test
    void newVersionDoesNotAlterAttemptInProgressNorXpCeiling(@TempDir Path tmp) throws IOException {
        Family f = newFamily("version");
        ApiClient c = f.client();
        String mission = missionId("ENGLISH-1-2");
        // Débloquer la mission 2 du monde anglais.
        playMission(c, "ENGLISH-1-1", 5);
        assertThat(playMission(c, "ENGLISH-1-2", 5).path("xpDelta").asInt()).isEqualTo(70);
        String attemptId = c.post("/missions/" + mission + "/attempts", Map.of()).body().path("attemptId").asString();
        String firstExercise = c.get("/attempts/" + attemptId).body().path("currentQuestion").path("id").asString();
        String oldPrompt = c.get("/attempts/" + attemptId).body().path("currentQuestion").path("prompt").asString();

        Path content = tmp.resolve("content");
        copy(CONTENT, content);
        Path file = content.resolve("english/english-1-2.json");
        Files.writeString(file, Files.readString(file).replace("Comment dit-on « bleu » en anglais ?", "Comment dit-on « bleu » en anglais ? (v2)"));
        try {
            ContentImporter.Report report = importer.importFrom(content);
            assertThat(report.newVersions()).isEqualTo(1);

            ApiClient.Response view = c.get("/attempts/" + attemptId);
            assertThat(view.body().path("currentQuestion").path("id").asString()).isEqualTo(firstExercise);
            assertThat(view.body().path("currentQuestion").path("prompt").asString()).isEqualTo(oldPrompt);
            assertThat(c.put("/attempts/" + attemptId + "/answers/" + firstExercise, Map.of("answer", answerFor(firstExercise, true))).status()).isEqualTo(200);

            // Nouvelle partie sur la nouvelle version : le plafond XP reste celui de la mission stable.
            c.post("/attempts/" + attemptId + "/abandon", Map.of());
            JsonNodeAssert.assertXp(playMission(c, "ENGLISH-1-2", 5).path("xpDelta").asInt(), 0);
            assertThat(c.get("/attempts/" + attemptId).body().path("status").asString()).isEqualTo("ABANDONED");
            assertThat(jdbc.sql("SELECT count(*) FROM mission_version WHERE mission_id = :m AND status = 'RETIRED'")
                    .param("m", java.util.UUID.fromString(mission)).query(Long.class).single()).isGreaterThanOrEqualTo(1);
        } finally {
            importer.importFrom(CONTENT);
        }
    }

    private static final class JsonNodeAssert {
        static void assertXp(int actual, int expected) {
            assertThat(actual).as("pas de nouveau gain après republication").isEqualTo(expected);
        }
    }

    @Test
    void invalidContentIsRejectedBeforeAnyWrite(@TempDir Path tmp) throws IOException {
        Path content = tmp.resolve("content");
        copy(CONTENT, content);
        Path file = content.resolve("maths/maths-1-1.json");
        Files.writeString(file, Files.readString(file)
                .replace("\"answer\": { \"optionId\": \"b\" }", "\"answer\": { \"optionId\": \"z\" }")
                .replace("\"instruction\": \"Calcule.\"", "\"instruction\": \"<b>Calcule</b>\""));
        long before = jdbc.sql("SELECT count(*) FROM mission_version").query(Long.class).single();
        assertThatThrownBy(() -> importer.importFrom(content))
                .isInstanceOf(ContentImporter.ContentValidationException.class)
                .satisfies(e -> {
                    var errors = ((ContentImporter.ContentValidationException) e).errors();
                    assertThat(errors).anyMatch(m -> m.contains("option existante"));
                    assertThat(errors).anyMatch(m -> m.contains("HTML"));
                });
        assertThat(jdbc.sql("SELECT count(*) FROM mission_version").query(Long.class).single()).isEqualTo(before);
    }

    @Test
    void catalogNeverExposesAnswerKeys() {
        Family f = newFamily("catalogue");
        ApiClient c = f.client();
        String worlds = c.get("/worlds").raw().body() + c.get("/worlds/iles-des-nombres").raw().body() + c.get("/worlds/royaume-des-mots").raw().body();
        assertThat(worlds).doesNotContain("answer").doesNotContain("explanation").doesNotContain("accepted");
    }
}
