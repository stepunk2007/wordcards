package dev.words.application.word;

import dev.words.application.port.Pronunciation;
import dev.words.domain.IpaSource;
import dev.words.domain.Language;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PronunciationResolverTest {

    private final Map<String, String> dictionary = Map.of(
            "borrow", "ˈbɒɹəʊ",
            "went", "wɛnt");

    private final PronunciationResolver resolver = new PronunciationResolver((language, text) ->
            Optional.ofNullable(dictionary.get(text)).map(ipa -> new Pronunciation(ipa, IpaSource.WIKTIONARY)));

    @Test
    void dictionaryWinsOverLlm() {
        assertThat(resolver.resolve(Language.ENGLISH, "borrow", "borrowed", "ˈbɒrəʊ"))
                .contains(new Pronunciation("ˈbɒɹəʊ", IpaSource.WIKTIONARY));
    }

    @Test
    void fallsBackToTypedFormWhenLemmaIsMissing() {
        assertThat(resolver.resolve(Language.ENGLISH, "go", "went", "ɡəʊ"))
                .contains(new Pronunciation("wɛnt", IpaSource.WIKTIONARY));
    }

    @Test
    void usesLlmIpaWhenDictionaryHasNothing() {
        assertThat(resolver.resolve(Language.ENGLISH, "serendipity", "serendipity", "/ˌsɛɹ.ənˈdɪp.ɪ.ti/"))
                .contains(new Pronunciation("ˌsɛɹ.ənˈdɪp.ɪ.ti", IpaSource.LLM));
    }

    @Test
    void emptyWhenNobodyKnows() {
        assertThat(resolver.resolve(Language.ENGLISH, "zzz", "zzz", null)).isEmpty();
        assertThat(resolver.resolve(Language.ENGLISH, "zzz", "zzz", " / / ")).isEmpty();
    }
}
