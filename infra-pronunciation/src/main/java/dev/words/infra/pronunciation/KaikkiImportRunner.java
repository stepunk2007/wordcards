package dev.words.infra.pronunciation;

import dev.words.domain.Language;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Runs the IPA import on startup when {@code words.pronunciation.import.kaikki} is set:
 * <pre>./gradlew :app:importIpa</pre>
 */
@Component
@ConditionalOnProperty("words.pronunciation.import.kaikki")
class KaikkiImportRunner implements ApplicationRunner {

    private final KaikkiImporter importer;
    private final Environment environment;

    KaikkiImportRunner(KaikkiImporter importer, Environment environment) {
        this.importer = importer;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String location = environment.getRequiredProperty("words.pronunciation.import.kaikki");
        var language = Language.fromCode(environment.getProperty("words.pronunciation.import.lang", "en"));
        importer.importDump(location, language);
    }
}
