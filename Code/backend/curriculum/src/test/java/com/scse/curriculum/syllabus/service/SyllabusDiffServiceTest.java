package com.scse.curriculum.syllabus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentCloId;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.entity.AssessmentType;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.book.entity.BookType;
import com.scse.curriculum.clo.entity.BloomLevel;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.entity.CompetencyLevel;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.plo.entity.Plo;
import com.scse.curriculum.syllabus.dto.SyllabusDiffResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.syllabusbook.entity.SyllabusBookId;
import com.scse.curriculum.syllabusbook.entity.UsageType;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.entity.TopicType;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.entity.TeachingLevel;
import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.entity.TopicCloId;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;

@ExtendWith(MockitoExtension.class)
class SyllabusDiffServiceTest {

    @Mock private CloRepository cloRepository;
    @Mock private CloPloMappingRepository cloPloMappingRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private TopicCloRepository topicCloRepository;
    @Mock private AssessmentComponentRepository assessmentComponentRepository;
    @Mock private AssessmentCloRepository assessmentCloRepository;
    @Mock private SyllabusBookRepository syllabusBookRepository;

    @InjectMocks
    private SyllabusDiffService service;

    private Syllabus oldSyllabus;
    private Syllabus newSyllabus;

    @BeforeEach
    void setUp() {
        Course course = Course.builder()
                .id(10)
                .courseCode("IT064IU")
                .name("Introduction to Computing")
                .build();

        oldSyllabus = Syllabus.builder()
                .id(100)
                .course(course)
                .versionLabel("v1.0")
                .academicYear("CS2021")
                .semester("1")
                .language("English")
                .objectives("Old objective")
                .notes("Old note")
                .build();

        newSyllabus = Syllabus.builder()
                .id(101)
                .course(course)
                .versionLabel("v2.0")
                .academicYear("CS2022")
                .semester("2")
                .language("English")
                .objectives("New objective")
                .notes("New note")
                .build();
    }

    @Test
    void compareIncludesReadingListAndAllMappings() {
        Clo oldClo = Clo.builder()
                .id(1)
                .syllabus(oldSyllabus)
                .code("CLO1")
                .description("Explain computing concepts")
                .descriptionVn("Giải thích khái niệm máy tính")
                .competencyLevel(CompetencyLevel.KNOWLEDGE)
                .bloomLevel(BloomLevel.UNDERSTAND)
                .orderIndex(1)
                .build();
        Clo newClo = Clo.builder()
                .id(11)
                .syllabus(newSyllabus)
                .code("CLO1")
                .description("Analyze computing concepts")
                .descriptionVn("Phân tích khái niệm máy tính")
                .competencyLevel(CompetencyLevel.KNOWLEDGE)
                .bloomLevel(BloomLevel.ANALYZE)
                .orderIndex(1)
                .build();

        Plo oldPlo = Plo.builder().id(7).code("PLO2").description("Problem solving").build();
        Plo newPlo = Plo.builder().id(7).code("PLO2").description("Problem solving").build();

        CloPloMapping oldCloPlo = CloPloMapping.builder()
                .id(20)
                .clo(oldClo)
                .plo(oldPlo)
                .level(ContributionLevel.I)
                .contributionWeight(0.25f)
                .notes("Introduced")
                .build();
        CloPloMapping newCloPlo = CloPloMapping.builder()
                .id(21)
                .clo(newClo)
                .plo(newPlo)
                .level(ContributionLevel.D)
                .contributionWeight(0.75f)
                .notes("Developed")
                .build();

        Topic oldTopic = Topic.builder()
                .id(2)
                .syllabus(oldSyllabus)
                .weekNumber(1)
                .orderInWeek(1)
                .name("Computer basics")
                .nameVn("Cơ bản máy tính")
                .teachingHours(3)
                .labHours(1)
                .selfStudyHours(4)
                .topicType(TopicType.LECTURE)
                .teachingMethod("Lecture")
                .learningActivity("Discussion")
                .build();
        Topic newTopic = Topic.builder()
                .id(12)
                .syllabus(newSyllabus)
                .weekNumber(1)
                .orderInWeek(1)
                .name("Computer foundations")
                .nameVn("Nền tảng máy tính")
                .teachingHours(2)
                .labHours(2)
                .selfStudyHours(4)
                .topicType(TopicType.LECTURE)
                .teachingMethod("Flipped classroom")
                .learningActivity("Discussion")
                .build();

        TopicClo oldTopicClo = TopicClo.builder()
                .id(new TopicCloId(2, 1))
                .topic(oldTopic)
                .clo(oldClo)
                .teachingLevel(TeachingLevel.I)
                .build();
        TopicClo newTopicClo = TopicClo.builder()
                .id(new TopicCloId(12, 11))
                .topic(newTopic)
                .clo(newClo)
                .teachingLevel(TeachingLevel.D)
                .build();

        AssessmentComponent oldAssessment = AssessmentComponent.builder()
                .id(3)
                .syllabus(oldSyllabus)
                .name("Final exam")
                .nameVn("Thi cuối kỳ")
                .assessmentType(AssessmentType.FINAL_EXAM)
                .weightPercent(50f)
                .minScore(0f)
                .maxScore(100f)
                .orderIndex(1)
                .build();
        AssessmentComponent newAssessment = AssessmentComponent.builder()
                .id(13)
                .syllabus(newSyllabus)
                .name("Final exam")
                .nameVn("Thi cuối kỳ")
                .assessmentType(AssessmentType.FINAL_EXAM)
                .weightPercent(60f)
                .minScore(0f)
                .maxScore(100f)
                .orderIndex(1)
                .build();

        AssessmentClo oldAssessmentClo = AssessmentClo.builder()
                .id(new AssessmentCloId(3, 1))
                .assessmentComponent(oldAssessment)
                .clo(oldClo)
                .contributionPercent(40f)
                .build();
        AssessmentClo newAssessmentClo = AssessmentClo.builder()
                .id(new AssessmentCloId(13, 11))
                .assessmentComponent(newAssessment)
                .clo(newClo)
                .contributionPercent(60f)
                .build();

        Book oldBook = Book.builder()
                .id(8)
                .title("Computer Science: An Overview")
                .author("J. Glenn Brookshear")
                .publisher("Pearson")
                .year(2019)
                .edition("13")
                .isbn("9780134875460")
                .bookType(BookType.TEXTBOOK)
                .build();
        Book newBook = Book.builder()
                .id(8)
                .title("Computer Science: An Overview")
                .author("J. Glenn Brookshear, Dennis Brylow")
                .publisher("Pearson")
                .year(2019)
                .edition("13")
                .isbn("9780134875460")
                .bookType(BookType.TEXTBOOK)
                .build();

        SyllabusBook oldReading = SyllabusBook.builder()
                .id(new SyllabusBookId(100, 8))
                .syllabus(oldSyllabus)
                .book(oldBook)
                .usageType(UsageType.REQUIRED)
                .orderIndex(1)
                .build();
        SyllabusBook newReading = SyllabusBook.builder()
                .id(new SyllabusBookId(101, 8))
                .syllabus(newSyllabus)
                .book(newBook)
                .usageType(UsageType.RECOMMENDED)
                .orderIndex(2)
                .build();

        when(cloRepository.findBySyllabusId(100)).thenReturn(List.of(oldClo));
        when(cloRepository.findBySyllabusId(101)).thenReturn(List.of(newClo));
        when(cloPloMappingRepository.findByClo_Syllabus_Id(100))
                .thenReturn(List.of(oldCloPlo));
        when(cloPloMappingRepository.findByClo_Syllabus_Id(101))
                .thenReturn(List.of(newCloPlo));
        when(topicRepository.findBySyllabusId(100)).thenReturn(List.of(oldTopic));
        when(topicRepository.findBySyllabusId(101)).thenReturn(List.of(newTopic));
        when(topicCloRepository.findByTopic_Syllabus_Id(100))
                .thenReturn(List.of(oldTopicClo));
        when(topicCloRepository.findByTopic_Syllabus_Id(101))
                .thenReturn(List.of(newTopicClo));
        when(assessmentComponentRepository.findBySyllabusId(100))
                .thenReturn(List.of(oldAssessment));
        when(assessmentComponentRepository.findBySyllabusId(101))
                .thenReturn(List.of(newAssessment));
        when(assessmentCloRepository.findByAssessmentComponent_Syllabus_Id(100))
                .thenReturn(List.of(oldAssessmentClo));
        when(assessmentCloRepository.findByAssessmentComponent_Syllabus_Id(101))
                .thenReturn(List.of(newAssessmentClo));
        when(syllabusBookRepository.findBySyllabus_Id(100))
                .thenReturn(List.of(oldReading));
        when(syllabusBookRepository.findBySyllabus_Id(101))
                .thenReturn(List.of(newReading));

        SyllabusDiffResponse result = service.compare(oldSyllabus, newSyllabus);

        assertThat(result.isHasChanges()).isTrue();
        assertThat(result.getGeneralInfoDiff())
                .containsKeys("academicYear", "semester", "objectives", "notes");

        assertThat(result.getCloDiff().getModified()).hasSize(1);
        assertThat(result.getCloDiff().getModified().get(0).getChanges())
                .containsKeys("description", "descriptionVn", "bloomLevel");

        assertThat(result.getCloPloDiff().getModified()).hasSize(1);
        assertThat(result.getCloPloDiff().getModified().get(0).getChanges())
                .containsKeys("level", "contributionWeight", "notes");

        assertThat(result.getTopicDiff().getModified()).hasSize(1);
        assertThat(result.getTopicCloDiff().getModified()).hasSize(1);
        assertThat(result.getTopicCloDiff().getModified().get(0).getChanges())
                .containsKey("teachingLevel");

        assertThat(result.getAssessmentDiff().getModified()).hasSize(1);
        assertThat(result.getAssessmentCloDiff().getModified()).hasSize(1);
        assertThat(result.getAssessmentCloDiff().getModified().get(0).getChanges())
                .containsKey("contributionPercent");

        assertThat(result.getReadingListDiff().getModified()).hasSize(1);
        assertThat(result.getReadingListDiff().getModified().get(0).getChanges())
                .containsKeys("author", "usageType", "orderIndex");
    }

    @Test
    void compareReturnsNoChangesForEquivalentVersions() {
        when(cloRepository.findBySyllabusId(100)).thenReturn(List.of());
        when(cloRepository.findBySyllabusId(101)).thenReturn(List.of());
        when(cloPloMappingRepository.findByClo_Syllabus_Id(100)).thenReturn(List.of());
        when(cloPloMappingRepository.findByClo_Syllabus_Id(101)).thenReturn(List.of());
        when(topicRepository.findBySyllabusId(100)).thenReturn(List.of());
        when(topicRepository.findBySyllabusId(101)).thenReturn(List.of());
        when(topicCloRepository.findByTopic_Syllabus_Id(100)).thenReturn(List.of());
        when(topicCloRepository.findByTopic_Syllabus_Id(101)).thenReturn(List.of());
        when(assessmentComponentRepository.findBySyllabusId(100)).thenReturn(List.of());
        when(assessmentComponentRepository.findBySyllabusId(101)).thenReturn(List.of());
        when(assessmentCloRepository.findByAssessmentComponent_Syllabus_Id(100))
                .thenReturn(List.of());
        when(assessmentCloRepository.findByAssessmentComponent_Syllabus_Id(101))
                .thenReturn(List.of());
        when(syllabusBookRepository.findBySyllabus_Id(100)).thenReturn(List.of());
        when(syllabusBookRepository.findBySyllabus_Id(101)).thenReturn(List.of());

        oldSyllabus.setAcademicYear("CS2022");
        oldSyllabus.setSemester("2");
        oldSyllabus.setObjectives("New objective");
        oldSyllabus.setNotes("New note");

        SyllabusDiffResponse result = service.compare(oldSyllabus, newSyllabus);

        assertThat(result.isHasChanges()).isFalse();
        assertThat(result.getGeneralInfoDiff()).isEmpty();
        assertThat(result.getReadingListDiff().getModified()).isEmpty();
    }
}
