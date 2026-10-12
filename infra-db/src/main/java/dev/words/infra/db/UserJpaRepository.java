package dev.words.infra.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByTelegramId(long telegramId);
}
