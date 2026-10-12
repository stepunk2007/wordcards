package dev.words.infra.db;

import dev.words.application.port.ExampleRepository;
import dev.words.domain.Example;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
class JpaExampleRepository implements ExampleRepository {

    private final ExampleJpaRepository jpa;

    JpaExampleRepository(ExampleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public List<Example> saveAll(List<Example> examples) {
        var entities = examples.stream().map(ExampleEntity::fromDomain).toList();
        return jpa.saveAllAndFlush(entities).stream().map(ExampleEntity::toDomain).toList();
    }

    @Override
    public List<Example> findByLexemeId(long lexemeId) {
        return jpa.findByLexemeIdOrderByIdAsc(lexemeId).stream().map(ExampleEntity::toDomain).toList();
    }
}
