package fr.eduquest.it;

import fr.eduquest.support.ApiClient;
import fr.eduquest.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Recettes 3, 4, 6, 7, 8 et 12 : mission persistée, gains, reprise, contournements, mondes futurs, rejeu. */
class MissionFlowIntegrationTest extends IntegrationTest {

    @Test
    void firstMissionThreeOutOfFiveGivesOneStarFiftyXpAndUnlocksNext() {
        Family f = newFamily("recette3");
        ApiClient c = f.client();
        JsonNode world = c.get("/worlds/iles-des-nombres").body();
        assertThat(world.path("levels").get(0).path("missions").get(1).path("state").asString()).isEqualTo("LOCKED");

        JsonNode summary = playMission(c, "MATHS-1-1", 3);
        assertThat(summary.path("correctCount").asInt()).isEqualTo(3);
        assertThat(summary.path("stars").asInt()).isEqualTo(1);
        assertThat(summary.path("xpDelta").asInt()).isEqualTo(50);
        assertThat(summary.path("xpTotal").asInt()).isEqualTo(50);
        assertThat(summary.path("newRewards").toString()).contains("FIRST_STEP");
        assertThat(summary.path("unlockedMissionIds").get(0).asString()).isEqualTo(missionId("MATHS-1-2"));

        JsonNode after = c.get("/worlds/iles-des-nombres").body();
        assertThat(after.path("levels").get(0).path("missions").get(1).path("state").asString()).isEqualTo("AVAILABLE");
        assertThat(c.get("/me/progress").body().path("xpTotal").asInt()).isEqualTo(50);
    }

    @Test
    void replayingGivesOnlyTheImprovementAndNoDuplicateBadge() {
        Family f = newFamily("recette4");
        ApiClient c = f.client();
        assertThat(playMission(c, "ENGLISH-1-1", 3).path("xpDelta").asInt()).isEqualTo(50);
        JsonNode same = playMission(c, "ENGLISH-1-1", 3);
        assertThat(same.path("xpDelta").asInt()).isZero();
        assertThat(same.path("newRewards").size()).isZero();
        JsonNode perfect = playMission(c, "ENGLISH-1-1", 5);
        assertThat(perfect.path("xpDelta").asInt()).isEqualTo(20);
        assertThat(perfect.path("stars").asInt()).isEqualTo(3);
        assertThat(perfect.path("newRewards").toString()).contains("PERFECT_MISSION").doesNotContain("FIRST_STEP");
        assertThat(playMission(c, "ENGLISH-1-1", 5).path("xpDelta").asInt()).isZero();

        Long firstStep = jdbc.sql("""
                SELECT count(*) FROM child_reward cr JOIN reward_definition r ON r.id = cr.reward_id
                WHERE cr.child_id = :c AND r.code = 'FIRST_STEP'""").param("c", f.childId()).query(Long.class).single();
        assertThat(firstStep).isEqualTo(1);
        Integer xpEvents = jdbc.sql("SELECT COALESCE(SUM(amount),0) FROM xp_event WHERE child_id = :c").param("c", f.childId()).query(Integer.class).single();
        Integer xpProgress = jdbc.sql("SELECT COALESCE(SUM(xp_awarded),0) FROM mission_progress WHERE child_id = :c").param("c", f.childId()).query(Integer.class).single();
        assertThat(xpEvents).as("invariant journal XP = progression").isEqualTo(xpProgress).isEqualTo(70);
    }

    @Test
    void firstTwoOutOfFiveGivesTwentyXpButDoesNotUnlock() {
        Family f = newFamily("deuxsurcinq");
        JsonNode s = playMission(f.client(), "MATHS-1-1", 2);
        assertThat(s.path("xpDelta").asInt()).isEqualTo(20);
        assertThat(s.path("stars").asInt()).isZero();
        assertThat(s.path("unlockedMissionIds").size()).isZero();
        assertThat(f.client().post("/missions/" + missionId("MATHS-1-2") + "/attempts", Map.of()).code()).isEqualTo("MISSION_LOCKED");
    }

    @Test
    void interruptedMissionResumesAtThirdQuestionAfterReconnection() {
        Family f = newFamily("recette6");
        ApiClient c = f.client();
        String attemptId = c.post("/missions/" + missionId("ENGLISH-1-1") + "/attempts", Map.of()).body().path("attemptId").asString();
        String first = null;
        for (int i = 0; i < 2; i++) {
            String ex = c.get("/attempts/" + attemptId).body().path("currentQuestion").path("id").asString();
            if (first == null) first = ex;
            c.put("/attempts/" + attemptId + "/answers/" + ex, Map.of("answer", answerFor(ex, true)));
        }
        // Fermeture : nouvelle connexion adulte puis sélection de l'enfant.
        ApiClient again = loginParent(f.email());
        enterChildMode(again, f.childId().toString());
        ApiClient.Response resumed = again.post("/missions/" + missionId("ENGLISH-1-1") + "/attempts", Map.of());
        assertThat(resumed.status()).as("partie existante").isEqualTo(200);
        assertThat(resumed.body().path("attemptId").asString()).isEqualTo(attemptId);
        assertThat(resumed.body().path("answeredCount").asInt()).isEqualTo(2);
        assertThat(resumed.body().path("currentQuestion").path("position").asInt()).isEqualTo(3);
        ApiClient.Response modify = again.put("/attempts/" + attemptId + "/answers/" + first, Map.of("answer", answerFor(first, false)));
        assertThat(modify.status()).as("réponse passée non modifiable").isEqualTo(409);
    }

    @Test
    void bypassAttemptsAreRefused() {
        Family f = newFamily("recette7");
        ApiClient c = f.client();
        JsonNode start = c.post("/missions/" + missionId("MATHS-1-1") + "/attempts", Map.of()).body();
        String attemptId = start.path("attemptId").asString();
        String current = start.path("currentQuestion").path("id").asString();
        assertThat(start.toString()).doesNotContain("answerKey").doesNotContain("explanation").doesNotContain("4205");

        List<String> all = jdbc.sql("""
                SELECT e.id::text FROM exercise e JOIN attempt a ON a.mission_version_id = e.mission_version_id
                WHERE a.id = :a ORDER BY e.ordinal""").param("a", java.util.UUID.fromString(attemptId)).query(String.class).list();
        ApiClient.Response skip = c.put("/attempts/" + attemptId + "/answers/" + all.get(2), Map.of("answer", answerFor(all.get(2), true)));
        assertThat(skip.code()).isEqualTo("NOT_CURRENT_QUESTION");

        String foreign = jdbc.sql("SELECT e.id::text FROM exercise e JOIN mission_version v ON v.id = e.mission_version_id JOIN mission m ON m.id = v.mission_id WHERE m.code = 'ENGLISH-1-1' LIMIT 1")
                .query(String.class).single();
        assertThat(c.put("/attempts/" + attemptId + "/answers/" + foreign, Map.of("answer", Map.of("optionId", "a"))).status())
                .as("exercice d'une autre mission").isEqualTo(404);

        ApiClient.Response withScore = c.put("/attempts/" + attemptId + "/answers/" + current,
                Map.of("answer", Map.of("number", "4205"), "score", 5, "isCorrect", true));
        assertThat(withScore.status()).isEqualTo(400);
        ApiClient.Response injected = c.put("/attempts/" + attemptId + "/answers/" + current, Map.of("answer", Map.of("number", "4205", "xp", 70)));
        assertThat(injected.status()).isEqualTo(422);

        assertThat(c.post("/attempts/" + attemptId + "/complete", Map.of()).code()).isEqualTo("ATTEMPT_INCOMPLETE");
        assertThat(c.post("/attempts/" + attemptId + "/complete", Map.of("correctCount", 5)).status()).isEqualTo(400);
        assertThat(c.post("/missions/" + missionId("MATHS-3-2") + "/attempts", Map.of()).code()).isEqualTo("MISSION_LOCKED");

        // Une réponse vide ne consomme pas la question.
        assertThat(c.put("/attempts/" + attemptId + "/answers/" + current, Map.of("answer", Map.of("number", " "))).status()).isEqualTo(400);
        assertThat(c.get("/attempts/" + attemptId).body().path("currentQuestion").path("id").asString()).isEqualTo(current);
    }

    @Test
    void comingSoonWorldsAreVisibleButNotPlayable() {
        Family f = newFamily("recette8");
        ApiClient c = f.client();
        JsonNode worlds = c.get("/worlds").body();
        assertThat(worlds.size()).isEqualTo(6);
        long playable = 0;
        for (JsonNode w : worlds) if ("PLAYABLE".equals(w.path("availability").asString())) playable++;
        assertThat(playable).isEqualTo(2);
        for (String slug : List.of("vallee-des-formes", "archipel-des-explorateurs", "foret-du-vivant", "cite-des-inventeurs")) {
            JsonNode detail = c.get("/worlds/" + slug).body();
            assertThat(detail.path("world").path("availability").asString()).isEqualTo("COMING_SOON");
            assertThat(detail.path("levels").size()).isZero();
        }
        assertThat(c.post("/missions/" + java.util.UUID.randomUUID() + "/attempts", Map.of()).status()).isEqualTo(404);
    }

    @Test
    void lostNetworkResponsesCanBeReplayedWithoutExtraGain() {
        Family f = newFamily("recette12");
        ApiClient c = f.client();
        String attemptId = c.post("/missions/" + missionId("MATHS-1-1") + "/attempts", Map.of()).body().path("attemptId").asString();
        for (int i = 0; i < 5; i++) {
            String ex = c.get("/attempts/" + attemptId).body().path("currentQuestion").path("id").asString();
            Map<String, Object> body = Map.of("answer", answerFor(ex, true));
            ApiClient.Response first = c.put("/attempts/" + attemptId + "/answers/" + ex, body);
            ApiClient.Response replay = c.put("/attempts/" + attemptId + "/answers/" + ex, body);
            assertThat(replay.status()).isEqualTo(200);
            assertThat(replay.body().path("correct")).isEqualTo(first.body().path("correct"));
            assertThat(replay.body().path("answeredCount").asInt()).isEqualTo(i + 1);
        }
        JsonNode s1 = c.post("/attempts/" + attemptId + "/complete", Map.of()).body();
        JsonNode s2 = c.post("/attempts/" + attemptId + "/complete", Map.of()).body();
        assertThat(s2).isEqualTo(s1);
        assertThat(s1.path("xpDelta").asInt()).isEqualTo(70);
        assertThat(c.get("/me/progress").body().path("xpTotal").asInt()).isEqualTo(70);
        assertThat(c.post("/attempts/" + attemptId + "/abandon", Map.of()).code()).isEqualTo("ATTEMPT_CLOSED");
    }

    @Test
    void hintIsRecordedIdempotentlyAndCountedSeparately() {
        Family f = newFamily("indice");
        ApiClient c = f.client();
        JsonNode start = c.post("/missions/" + missionId("MATHS-1-1") + "/attempts", Map.of()).body();
        String attemptId = start.path("attemptId").asString();
        String ex = start.path("currentQuestion").path("id").asString();
        ApiClient.Response h1 = c.post("/attempts/" + attemptId + "/answers/" + ex + "/hint", Map.of());
        ApiClient.Response h2 = c.post("/attempts/" + attemptId + "/answers/" + ex + "/hint", Map.of());
        assertThat(h1.status()).isEqualTo(200);
        assertThat(h2.body()).isEqualTo(h1.body());
        assertThat(c.get("/attempts/" + attemptId).body().path("currentQuestion").path("hintUsed").asBoolean()).isTrue();
        ApiClient.Response corr = c.put("/attempts/" + attemptId + "/answers/" + ex, Map.of("answer", answerFor(ex, true)));
        assertThat(corr.body().path("correct").asBoolean()).isTrue();
        assertThat(corr.body().path("hintUsed").asBoolean()).isTrue();
    }

    @Test
    void abandonedAttemptGivesNothingAndAllowsANewOne() {
        Family f = newFamily("abandon");
        ApiClient c = f.client();
        String mission = missionId("MATHS-1-1");
        String a1 = c.post("/missions/" + mission + "/attempts", Map.of()).body().path("attemptId").asString();
        assertThat(c.post("/attempts/" + a1 + "/abandon", Map.of()).status()).isEqualTo(204);
        assertThat(c.post("/attempts/" + a1 + "/abandon", Map.of()).status()).as("répétable").isEqualTo(204);
        ApiClient.Response again = c.post("/missions/" + mission + "/attempts", Map.of());
        assertThat(again.status()).isEqualTo(201);
        assertThat(again.body().path("attemptId").asString()).isNotEqualTo(a1);
        assertThat(c.get("/me/progress").body().path("xpTotal").asInt()).isZero();
    }

    @Test
    void finishingAWorldGrantsWorldBadgeAndLevelCosmetics() {
        Family f = newFamily("monde");
        ApiClient c = f.client();
        for (String code : List.of("MATHS-1-1", "MATHS-1-2", "MATHS-2-1", "MATHS-2-2", "MATHS-3-1", "MATHS-3-2")) {
            playMission(c, code, 5);
        }
        JsonNode rewards = c.get("/me/rewards").body();
        List<String> owned = new java.util.ArrayList<>();
        rewards.path("rewards").forEach(r -> { if (r.path("owned").asBoolean()) owned.add(r.path("code").asString()); });
        assertThat(owned).contains("NUMBERS_EXPLORER", "CAPTAIN_CAP", "STAR_CAPE", "OWL_FRIEND", "FIRST_STEP", "PERFECT_MISSION",
                "STRAW_HAT", "TURTLE_FRIEND", "LEAF_CAPE", "FLOWER_CROWN");
        assertThat(owned).doesNotContain("WORDS_FRIEND");
        JsonNode progress = c.get("/me/progress").body();
        assertThat(progress.path("xpTotal").asInt()).isEqualTo(420);
        assertThat(progress.path("adventurerLevel").asInt()).isEqualTo(5);

        assertThat(c.put("/me/equipment/HEAD", Map.of("rewardCode", "CAPTAIN_CAP")).body().path("equipment").path("HEAD").asString()).isEqualTo("CAPTAIN_CAP");
        assertThat(c.put("/me/equipment/HEAD", Map.of("rewardCode", "SPACE_HELMET")).status()).as("non possédé").isEqualTo(422);
        assertThat(c.put("/me/equipment/BACK", Map.of("rewardCode", "CAPTAIN_CAP")).status()).as("mauvais emplacement").isEqualTo(422);
    }
}
