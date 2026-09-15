package com.scse.curriculum.syllabus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.syllabus.dto.SyllabusDiffResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;

/**
 * Contract test for the canonical syllabus comparison surface.
 *
 * If a canonical comparison field/section is intentionally added or removed,
 * this test must be updated in the same change.
 *
 * Template-specific parser layout must never change this contract.
 */
@ExtendWith(MockitoExtension.class)
class SyllabusComparisonCoverageContractTest {

    @Mock
    private CloRepository cloRepository;

    @Mock
    private CloPloMappingRepository cloPloMappingRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicCloRepository topicCloRepository;

    @Mock
    private AssessmentComponentRepository assessmentComponentRepository;

    @Mock
    private AssessmentCloRepository assessmentCloRepository;

    @Mock
    private SyllabusBookRepository syllabusBookRepository;

    @InjectMocks
    private SyllabusDiffService service;

    @Test
    void canonicalComparisonCoversAllThirtyScalarFieldsAndEightStructuralSections() {

        Course oldCourse = Course.builder()
                .id(10)
                .courseCode("IT999IU")
                .name("Old Course Name")
                .nameVn("Tên môn cũ")
                .creditTheory(3)
                .creditLab(1)
                .build();

        Course newCourse = Course.builder()
                .id(10)
                .courseCode("IT999IU")
                .name("New Course Name")
                .nameVn("Tên môn mới")
                .creditTheory(4)
                .creditLab(2)
                .build();

        Syllabus oldSyllabus = Syllabus.builder()
                .id(100)
                .course(oldCourse)
                .courseCodeSnapshot("IT999IU-OLD")
                .courseNameSnapshot("Old Course Name")
                .versionNumber(1)
                .versionLabel("v1.0")
                .academicYear("CS2025")
                .program("CS-2021")
                .courseDesignation("Old designation")
                .courseTypes("Old type")
                .semester("Semester 4")
                .language("English")
                .relation("Old relation")
                .teachingMethods("Lecture")
                .workloadTotal("120")
                .workloadContact("45")
                .workloadPrivate("75")
                .prerequisites("Old prerequisite")
                .objectives("Old objective")
                .examForms("Old exam form")
                .examRequirements("Old exam requirement")
                .rubrics("Old rubric")
                .major("Old major")
                .changeSummary("Old change summary")
                .notes("""
                        {
                          "internalNotes": "Old internal note",
                          "personResponsible": "Old lecturer",
                          "dateRevised": "2025-01-01",
                          "creditPoints": "4",
                          "lectureCredits": "3",
                          "laboratoryCredits": "1",
                          "workloadStudentResponsibility": "Old responsibility",
                          "assessmentPassNote": "Old assessment pass note",
                          "contentNote": "Old content note"
                        }
                        """)
                .build();

        Syllabus newSyllabus = Syllabus.builder()
                .id(200)
                .course(newCourse)
                .courseCodeSnapshot("IT999IU-NEW")
                .courseNameSnapshot("New Course Name")
                .versionNumber(1)
                .versionLabel("v1.0")
                .academicYear("CS2026")
                .program("CS-2025")
                .courseDesignation("New designation")
                .courseTypes("New type")
                .semester("Semester 5")
                .language("Vietnamese")
                .relation("New relation")
                .teachingMethods("Project based learning")
                .workloadTotal("150")
                .workloadContact("60")
                .workloadPrivate("90")
                .prerequisites("New prerequisite")
                .objectives("New objective")
                .examForms("New exam form")
                .examRequirements("New exam requirement")
                .rubrics("New rubric")
                .major("New major")
                .changeSummary("New change summary")
                .notes("""
                        {
                          "internalNotes": "New internal note",
                          "personResponsible": "New lecturer",
                          "dateRevised": "2026-01-01",
                          "creditPoints": "6",
                          "lectureCredits": "4",
                          "laboratoryCredits": "2",
                          "workloadStudentResponsibility": "New responsibility",
                          "assessmentPassNote": "New assessment pass note",
                          "contentNote": "New content note"
                        }
                        """)
                .build();

        stubEmptyRelations(100);
        stubEmptyRelations(200);

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        /*
         * ============================================================
         * 30 CANONICAL SCALAR FIELD KEYS
         * ============================================================
         */

        assertThat(result.getGeneralInfoDiff())
                .containsOnlyKeys(
                        "courseCode",
                        "courseName",
                        "courseNameVn",
                        "academicYear",
                        "program",
                        "courseDesignation",
                        "courseTypes",
                        "semester",
                        "personResponsible",
                        "language",
                        "relation",
                        "teachingMethods",
                        "major");

        assertThat(result.getWorkloadCreditDiff())
                .containsOnlyKeys(
                        "workloadTotal",
                        "workloadContact",
                        "workloadPrivate",
                        "workloadStudentResponsibility",
                        "creditPoints",
                        "lectureCredits",
                        "laboratoryCredits");

        assertThat(result.getRequirementsDiff())
                .containsOnlyKeys(
                        "prerequisites",
                        "objectives");

        assertThat(result.getContentDiff())
                .containsOnlyKeys(
                        "contentNote");

        assertThat(result.getAssessmentInfoDiff())
                .containsOnlyKeys(
                        "assessmentPassNote");

        assertThat(result.getExaminationDiff())
                .containsOnlyKeys(
                        "examForms",
                        "examRequirements",
                        "rubrics");

        assertThat(result.getRevisionInfoDiff())
                .containsOnlyKeys(
                        "dateRevised",
                        "internalNotes",
                        "changeSummary");

        int scalarFieldCount =
                result.getGeneralInfoDiff().size()
                        + result.getWorkloadCreditDiff().size()
                        + result.getRequirementsDiff().size()
                        + result.getContentDiff().size()
                        + result.getAssessmentInfoDiff().size()
                        + result.getExaminationDiff().size()
                        + result.getRevisionInfoDiff().size();

        assertThat(scalarFieldCount)
                .as("Canonical scalar comparison coverage")
                .isEqualTo(30);

        /*
         * ============================================================
         * 8 CANONICAL REPEATING / RELATION SECTIONS
         * ============================================================
         */

        assertThat(result.getCloDiff()).isNotNull();
        assertThat(result.getCloPloDiff()).isNotNull();
        assertThat(result.getTopicDiff()).isNotNull();
        assertThat(result.getTopicCloDiff()).isNotNull();
        assertThat(result.getPlannedActivityDiff()).isNotNull();
        assertThat(result.getAssessmentDiff()).isNotNull();
        assertThat(result.getAssessmentCloDiff()).isNotNull();
        assertThat(result.getReadingsDiff()).isNotNull();

        assertEmpty(result.getCloDiff());
        assertEmpty(result.getCloPloDiff());
        assertEmpty(result.getTopicDiff());
        assertEmpty(result.getTopicCloDiff());
        assertEmpty(result.getPlannedActivityDiff());
        assertEmpty(result.getAssessmentDiff());
        assertEmpty(result.getAssessmentCloDiff());
        assertEmpty(result.getReadingsDiff());

        assertThat(result.isHasChanges()).isTrue();
    }

    private void stubEmptyRelations(Integer syllabusId) {

        when(cloRepository.findBySyllabusId(syllabusId))
                .thenReturn(List.of());

        when(cloPloMappingRepository.findByClo_Syllabus_Id(syllabusId))
                .thenReturn(List.of());

        when(topicRepository.findBySyllabusId(syllabusId))
                .thenReturn(List.of());

        when(topicCloRepository.findByTopic_Syllabus_Id(syllabusId))
                .thenReturn(List.of());

        when(assessmentComponentRepository.findBySyllabusId(syllabusId))
                .thenReturn(List.of());

        when(assessmentCloRepository
                .findByAssessmentComponent_Syllabus_Id(syllabusId))
                .thenReturn(List.of());

        when(syllabusBookRepository.findBySyllabus_Id(syllabusId))
                .thenReturn(List.of());
    }

    private void assertEmpty(
            SyllabusDiffResponse.ListDiff<?> diff) {

        assertThat(diff.getAdded()).isEmpty();
        assertThat(diff.getRemoved()).isEmpty();
        assertThat(diff.getModified()).isEmpty();
    }
}