package com.scse.curriculum.syllabus.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.scse.curriculum.CurriculumApplication;
import com.scse.curriculum.auditlog.listener.AuditLogAsyncProcessor;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = CurriculumApplication.class)
@ActiveProfiles("test")
@Transactional
class SyllabusPersistenceRegressionTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SyllabusRepository syllabusRepository;

    @MockBean
    private AuditLogAsyncProcessor auditLogAsyncProcessor;

    @Test
    void savingSameNewSyllabusTwiceKeepsSingleDatabaseRow() {

        assertThat(
                jdbcTemplate.queryForObject(
                        "SELECT DATABASE()",
                        String.class))
                .isEqualTo("curriculum_iu_test");

        Course course = Course.builder()
                .courseCode("SYLLABUS_PERSISTENCE_TEST")
                .name("Syllabus persistence regression test")
                .nameVn("Syllabus persistence regression test")
                .creditTheory(3)
                .creditLab(0)
                .isActive(true)
                .build();

        entityManager.persist(course);
        entityManager.flush();

        Syllabus syllabus = Syllabus.builder()
                .course(course)
                .program("CS-2021")
                .academicYear("CS2099")
                .semester("Semester 8")
                .versionNumber(1)
                .versionLabel("v1.0")
                .status(SyllabusStatus.DRAFT)
                .isCurrent(false)
                .build();

        /*
         * Regression guard:
         *
         * A brand-new Syllabus with a nullable @Version must begin with
         * lockVersion == null so Spring Data JPA recognizes it as new and
         * calls persist() instead of merge().
         */
        assertThat(syllabus.getId()).isNull();
        assertThat(syllabus.getLockVersion()).isNull();

        Syllabus firstSave =
                syllabusRepository.saveAndFlush(syllabus);

        Integer persistedId = firstSave.getId();

        assertThat(persistedId).isNotNull();
        assertThat(firstSave).isSameAs(syllabus);
        assertThat(syllabus.getId()).isEqualTo(persistedId);
        assertThat(syllabus.getLockVersion()).isNotNull();

        /*
         * Import currently flushes the same Syllabus again after importing
         * child data and linking CourseProgram. This second save must UPDATE
         * the already-managed row, never INSERT another logical syllabus.
         */
        Syllabus secondSave =
                syllabusRepository.saveAndFlush(syllabus);

        assertThat(secondSave).isSameAs(syllabus);
        assertThat(secondSave.getId()).isEqualTo(persistedId);

        Long logicalRowCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM syllabus
                        WHERE course_id = ?
                          AND program = ?
                          AND academic_year = ?
                          AND semester = ?
                        """,
                        Long.class,
                        course.getId(),
                        "CS-2021",
                        "CS2099",
                        "Semester 8");

        assertThat(logicalRowCount).isEqualTo(1L);
    }
}