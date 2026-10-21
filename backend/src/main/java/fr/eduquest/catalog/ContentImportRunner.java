package fr.eduquest.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Outil CLI : {@code --eduquest.content.import=true --eduquest.content.path=../content}.
 * Utilisé par {@code make content-import} (application lancée sans serveur web, puis arrêtée).
 */
@Component
@ConditionalOnProperty(name = "eduquest.content.import", havingValue = "true")
public class ContentImportRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ContentImportRunner.class);
    private final ContentImporter importer;
    private final ConfigurableApplicationContext context;
    private final Path path;

    public ContentImportRunner(ContentImporter importer, ConfigurableApplicationContext context,
                               @Value("${eduquest.content.path:../content}") String path) {
        this.importer = importer;
        this.context = context;
        this.path = Path.of(path);
    }

    @Override
    public void run(ApplicationArguments args) {
        int code = 0;
        try {
            ContentImporter.Report report = importer.importFrom(path);
            log.info("Import des contenus terminé : {}", report);
        } catch (ContentImporter.ContentValidationException e) {
            log.error(e.getMessage());
            code = 1;
        }
        int exitCode = code;
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }
}
