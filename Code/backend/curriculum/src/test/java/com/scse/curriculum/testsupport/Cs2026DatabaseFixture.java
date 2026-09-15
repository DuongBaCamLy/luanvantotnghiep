package com.scse.curriculum.testsupport;

import com.scse.curriculum.syllabus.source.entity.SourceDocument;
import com.scse.curriculum.syllabus.source.repository.SourceDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Fixed PDF and two curriculum examples; no dependency on records in the demo DB. */
public abstract class Cs2026DatabaseFixture {
    @MockBean
    protected SourceDocumentRepository sourceDocumentRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void loadIsolatedFixture() throws Exception {
        // Seed on a separate connection, outside the audit tests' read-only transaction.
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getCatalog()).isEqualTo("curriculum_iu_test");
            new ResourceDatabasePopulator(new ClassPathResource("fixtures/cs2026.sql")).populate(connection);
            if (!connection.getAutoCommit()) connection.commit();
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT COUNT(*) FROM course_program WHERE program_id=1 AND cohort_id=12")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(2);
            }
        }
        try (var stream = new ClassPathResource("syllabus-import/cs2026-program.pdf").getInputStream()) {
            SourceDocument source = SourceDocument.builder()
                    .id(48L).contentType("application/pdf").content(stream.readAllBytes()).build();
            when(sourceDocumentRepository.findById(48L)).thenReturn(Optional.of(source));
        }
    }
}
