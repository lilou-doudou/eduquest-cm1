package fr.eduquest.shared;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionGuardTest {
    private static void run(String password, String baseUrl, boolean devSeed) {
        new ProductionGuard(password, baseUrl, devSeed).run(new DefaultApplicationArguments());
    }

    @Test
    void refuseLesSecretsDeDemonstrationEtLaConfigurationNonSecurisee() {
        assertThatThrownBy(() -> run("", "https://eduquest.example", false)).hasMessageContaining("POSTGRES_PASSWORD");
        assertThatThrownBy(() -> run("eduquest-dev-only", "https://eduquest.example", false)).hasMessageContaining("POSTGRES_PASSWORD");
        assertThatThrownBy(() -> run("un-vrai-secret-solide", "http://eduquest.example", false)).hasMessageContaining("HTTPS");
        assertThatThrownBy(() -> run("un-vrai-secret-solide", "https://eduquest.example", true)).hasMessageContaining("démonstration");
    }

    @Test
    void accepteUneConfigurationDeProduction() {
        assertThatCode(() -> run("un-vrai-secret-solide", "https://eduquest.example", false)).doesNotThrowAnyException();
    }
}
