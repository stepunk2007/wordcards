package dev.words.infra.pronunciation;

import dev.words.application.port.Pronunciation;
import dev.words.application.port.PronunciationPort;
import dev.words.domain.IpaSource;
import dev.words.domain.Language;
import dev.words.domain.WordNormalizer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Looks words up in the IPA dictionary imported by {@link KaikkiImporter}.
 */
@Component
class JdbcPronunciationAdapter implements PronunciationPort {

    private final JdbcTemplate jdbc;

    JdbcPronunciationAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Pronunciation> findIpa(Language language, String text) {
        String normalized;
        try {
            normalized = WordNormalizer.normalize(text);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return jdbc.query(
                "select ipa, source from pronunciation where lang = ? and normalized_text = ?",
                (rs, row) -> new Pronunciation(rs.getString("ipa"), IpaSource.valueOf(rs.getString("source"))),
                language.code(), normalized
        ).stream().findFirst();
    }
}
