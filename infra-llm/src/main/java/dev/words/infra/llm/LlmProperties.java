package dev.words.infra.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * LLM settings, bound from {@code words.llm.*}.
 *
 * @param providers providers in the order they are tried: the first one is the main one,
 *                  the next ones are used when it fails
 */
@ConfigurationProperties("words.llm")
public record LlmProperties(
        @DefaultValue({"openai", "anthropic"}) List<String> providers,
        @DefaultValue ProviderProperties openai,
        @DefaultValue ProviderProperties anthropic) {
}
