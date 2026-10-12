package dev.words.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Telegram bot settings, bound from {@code words.telegram.*}.
 *
 * @param enabled  start polling Telegram on application startup; tests switch it off
 * @param token    bot token from @BotFather; the bot does not start without it
 * @param username bot username without @, used to recognise commands like /start@my_bot in groups
 */
@ConfigurationProperties("words.telegram")
public record TelegramProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("") String token,
        @DefaultValue("") String username) {

    public boolean canStart() {
        return enabled && token != null && !token.isBlank();
    }
}
