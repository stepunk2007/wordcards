package dev.words.infra.db;

import dev.words.domain.Language;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores {@link Language} as its ISO 639-1 code.
 */
@Converter(autoApply = true)
class LanguageConverter implements AttributeConverter<Language, String> {

    @Override
    public String convertToDatabaseColumn(Language language) {
        return language == null ? null : language.code();
    }

    @Override
    public Language convertToEntityAttribute(String code) {
        return code == null ? null : Language.fromCode(code);
    }

    static Map<String, String> toCodes(Map<Language, String> byLanguage) {
        var result = new HashMap<String, String>();
        byLanguage.forEach((language, value) -> result.put(language.code(), value));
        return result;
    }

    static Map<Language, String> fromCodes(Map<String, String> byCode) {
        var result = new EnumMap<Language, String>(Language.class);
        byCode.forEach((code, value) -> result.put(Language.fromCode(code), value));
        return result;
    }
}
