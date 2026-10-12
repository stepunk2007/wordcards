package dev.words.infra.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ExampleJpaRepository extends JpaRepository<ExampleEntity, Long> {

    List<ExampleEntity> findByLexemeIdOrderByIdAsc(Long lexemeId);
}
