package dev.words;

import dev.words.application.user.RegisterUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterUserIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    RegisterUser registerUser;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void repeatedRegistrationKeepsSingleUser() {
        var first = registerUser.register(5001);
        var second = registerUser.register(5001);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(countUsers(5001)).isOne();
    }

    @Test
    void concurrentFirstMessagesCreateSingleUser() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = IntStream.range(0, 8)
                    .<Callable<Long>>mapToObj(i -> () -> registerUser.register(5002).id())
                    .toList();

            var ids = executor.invokeAll(tasks).stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }).distinct().toList();

            assertThat(ids).hasSize(1);
        }
        assertThat(countUsers(5002)).isOne();
    }

    private int countUsers(long telegramId) {
        return jdbc.queryForObject("select count(*) from users where telegram_id = ?", Integer.class, telegramId);
    }
}
