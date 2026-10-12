package dev.words.infra.db;

import dev.words.domain.IpaSource;
import dev.words.domain.Language;
import dev.words.domain.Lexeme;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "lexeme")
class LexemeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lang", nullable = false)
    private Language language;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "normalized_text", nullable = false)
    private String normalizedText;

    @Column(name = "part_of_speech")
    private String partOfSpeech;

    @Column(name = "ipa")
    private String ipa;

    @Enumerated(EnumType.STRING)
    @Column(name = "ipa_source")
    private IpaSource ipaSource;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lexeme_meaning", joinColumns = @JoinColumn(name = "lexeme_id"))
    @MapKeyColumn(name = "native_lang")
    @Column(name = "meaning", nullable = false)
    private Map<String, String> meanings = new HashMap<>();

    protected LexemeEntity() {
    }

    static LexemeEntity fromDomain(Lexeme lexeme) {
        var entity = new LexemeEntity();
        entity.id = lexeme.id();
        entity.language = lexeme.language();
        entity.text = lexeme.text();
        entity.normalizedText = lexeme.normalizedText();
        entity.partOfSpeech = lexeme.partOfSpeech();
        entity.ipa = lexeme.ipa();
        entity.ipaSource = lexeme.ipaSource();
        entity.meanings = LanguageConverter.toCodes(lexeme.meanings());
        return entity;
    }

    Lexeme toDomain() {
        return new Lexeme(id, language, text, normalizedText, partOfSpeech, ipa, ipaSource,
                LanguageConverter.fromCodes(meanings));
    }
}
