package fr.eduquest.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

/** Métadonnées du contrat OpenAPI (docs/api/openapi.yaml), exposé uniquement en profil dev. */
@Configuration
public class OpenApiConfig {
    static {
        // Les réponses libres (JSON) sont documentées comme objets génériques.
        SpringDocUtils.getConfig().replaceWithClass(JsonNode.class, Object.class).replaceWithClass(ObjectNode.class, Map.class);
    }

    @Bean
    OpenAPI eduquestOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduQuest CM1 API")
                        .version("v1")
                        .description("""
                                API REST du MVP EduQuest CM1. Authentification par cookie de session opaque (EQSESSION, \
                                HttpOnly, SameSite=Lax). Toute requête modifiante exige l'en-tête X-CSRF-TOKEN obtenu via \
                                GET /api/v1/auth/csrf. Erreurs au format application/problem+json avec `code` et `traceId`. \
                                Rôles : PARENT (espace adulte) et CHILD (session enfant limitée)."""))
                .servers(List.of(new Server().url("/").description("Même origine que le frontend")));
    }
}
