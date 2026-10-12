package dev.words.infra.db;

import dev.words.domain.CefrLevel;
import dev.words.domain.Example;
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
@Table(name = "example")
class ExampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lexeme_id", nullable = false)
    private Long lexemeId;

    @Column(name = "text", nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private CefrLevel level;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "example_translation", joinColumns = @JoinColumn(name = "example_id"))
    @MapKeyColumn(name = "native_lang")
    @Column(name = "translation", nullable = false)
    private Map<String, String> translations = new HashMap<>();

    protected ExampleEntity() {
    }

    static ExampleEntity fromDomain(Example example) {
        var entity = new ExampleEntity();
        entity.id = example.id();
        entity.lexemeId = example.lexemeId();
        entity.text = example.text();
        entity.level = example.level();
        entity.translations = LanguageConverter.toCodes(example.translations());
        return entity;
    }

    Example toDomain() {
        return new Example(id, lexemeId, text, level, LanguageConverter.fromCodes(translations));
    }
}
