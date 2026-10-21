package fr.eduquest.it;

import fr.eduquest.support.ApiClient;
import fr.eduquest.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/** Recette 5 : clôtures et réponses simultanées. */
class ConcurrencyIntegrationTest extends IntegrationTest {

    private <T> List<T> concurrently(int n, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                futures.add(pool.submit(() -> {
                    go.await();
                    return task.call();
                }));
            }
            go.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> f : futures) results.add(f.get());
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void simultaneousCompletionsGiveOneGainAndOneRewardEach() throws Exception {
        Family f = newFamily("concurrence");
        ApiClient c = f.client();
        String attemptId = c.post("/missions/" + missionId("MATHS-1-1") + "/attempts", Map.of()).body().path("attemptId").asString();
        for (int i = 0; i < 5; i++) {
            String ex = c.get("/attempts/" + attemptId).body().path("currentQuestion").path("id").asString();
            c.put("/attempts/" + attemptId + "/answers/" + ex, Map.of("answer", answerFor(ex, true)));
        }
        List<ApiClient.Response> results = concurrently(6, () -> c.post("/attempts/" + attemptId + "/complete", Map.of()));
        for (ApiClient.Response r : results) {
            assertThat(r.status()).isEqualTo(200);
            assertThat(r.body()).isEqualTo(results.get(0).body());
        }
        assertThat(jdbc.sql("SELECT count(*) FROM xp_event WHERE child_id = :c").param("c", f.childId()).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT xp_awarded FROM mission_progress WHERE child_id = :c").param("c", f.childId()).query(Integer.class).single()).isEqualTo(70);
        assertThat(jdbc.sql("SELECT completed_attempts FROM mission_progress WHERE child_id = :c").param("c", f.childId()).query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("""
                SELECT count(*) FROM child_reward cr JOIN reward_definition r ON r.id = cr.reward_id
                WHERE cr.child_id = :c AND r.code IN ('FIRST_STEP', 'PERFECT_MISSION')""").param("c", f.childId()).query(Long.class).single()).isEqualTo(2);
    }

    @Test
    void simultaneousStartsCreateASingleAttempt() throws Exception {
        Family f = newFamily("demarrage");
        ApiClient c = f.client();
        c.refreshCsrf();
        String mission = missionId("ENGLISH-1-1");
        List<ApiClient.Response> results = concurrently(5, () -> c.post("/missions/" + mission + "/attempts", Map.of()));
        assertThat(results).extracting(r -> r.body().path("attemptId").asString()).containsOnly(results.get(0).body().path("attemptId").asString());
        assertThat(results).filteredOn(r -> r.status() == 201).hasSize(1);
    }

    @Test
    void simultaneousDifferentAnswersToTheSameQuestionKeepOnlyOne() throws Exception {
        Family f = newFamily("reponses");
        ApiClient c = f.client();
        String attemptId = c.post("/missions/" + missionId("MATHS-1-1") + "/attempts", Map.of()).body().path("attemptId").asString();
        String ex = c.get("/attempts/" + attemptId).body().path("currentQuestion").path("id").asString();
        c.refreshCsrf();
        List<ApiClient.Response> results = concurrently(4, () -> {
            boolean good = Thread.currentThread().getId() % 2 == 0;
            return c.put("/attempts/" + attemptId + "/answers/" + ex, Map.of("answer", answerFor(ex, good)));
        });
        assertThat(results).allMatch(r -> r.status() == 200 || r.status() == 409);
        assertThat(jdbc.sql("SELECT count(*) FROM attempt_answer WHERE attempt_id = :a AND answered_at IS NOT NULL")
                .param("a", java.util.UUID.fromString(attemptId)).query(Long.class).single()).isEqualTo(1);
    }
}
