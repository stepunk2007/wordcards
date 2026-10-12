package dev.words.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Configuration
@EnableConfigurationProperties(TelegramProperties.class)
class TelegramBotConfig {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotConfig.class);

    /**
     * Starts long polling after the context is ready and stops it on shutdown.
     * Without a token the application runs without the bot, e.g. in tests or for the REST API only.
     */
    @Bean
    SmartLifecycle telegramBotLifecycle(TelegramProperties properties, UpdateHandler handler) {
        return new SmartLifecycle() {

            private TelegramBotsLongPollingApplication application;
            private WordsBot bot;

            @Override
            public void start() {
                if (!properties.canStart()) {
                    log.warn("Telegram bot is not started: words.telegram.token (TELEGRAM_BOT_TOKEN) is not set "
                            + "or words.telegram.enabled is false");
                    return;
                }
                bot = new WordsBot(new OkHttpTelegramClient(properties.token()), handler);
                application = new TelegramBotsLongPollingApplication();
                try {
                    application.registerBot(properties.token(), bot);
                    log.info("Telegram bot @{} is polling for updates", properties.username());
                } catch (TelegramApiException e) {
                    throw new IllegalStateException("Could not start the Telegram bot", e);
                }
            }

            @Override
            public void stop() {
                try {
                    if (application != null) {
                        application.close();
                    }
                } catch (Exception e) {
                    log.warn("Error while stopping the Telegram bot", e);
                } finally {
                    if (bot != null) {
                        bot.close();
                    }
                    application = null;
                    bot = null;
                }
            }

            @Override
            public boolean isRunning() {
                return application != null;
            }
        };
    }
}
