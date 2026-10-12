package dev.words.infra.db;

import dev.words.application.port.UserRepository;
import dev.words.domain.User;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpa;

    JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public User save(User user) {
        return jpa.saveAndFlush(UserEntity.fromDomain(user)).toDomain();
    }

    @Override
    public Optional<User> findById(long id) {
        return jpa.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByTelegramId(long telegramId) {
        return jpa.findByTelegramId(telegramId).map(UserEntity::toDomain);
    }
}
