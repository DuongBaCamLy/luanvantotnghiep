package com.scse.curriculum;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.service.SyllabusService;

@SpringBootTest(classes = CurriculumApplication.class)
@ActiveProfiles("test")
public class CheckDBTest {

    @Autowired
    private SyllabusService syllabusService;

    @Test
    public void testSyllabusCreationException() {
        System.out.println("=== STARTING SPRING BOOT SYLLABUS CREATION DIAGNOSTIC TEST ===");

        CreateSyllabusRequest request = new CreateSyllabusRequest();

        request.setCourseId(1);
        request.setCreatedBy(1);
        request.setVersionLabel("v1.0");
        request.setAcademicYear("K24");
        request.setChangeSummary("Test syllabus creation");
        request.setNotes("Diagnostic test");

        List<CreateSyllabusRequest.CloDTO> clos = new ArrayList<>();
        clos.add(new CreateSyllabusRequest.CloDTO(
                "CLO1",
                "Understand basics",
                "Hiểu cơ bản",
                "KNOWLEDGE",
                "APPLY",
                1
        ));
        request.setClos(clos);

        List<CreateSyllabusRequest.TopicDTO> topics = new ArrayList<>();
       topics.add(new CreateSyllabusRequest.TopicDTO(
        1,
        1,
        "Introduction",
        "Giới thiệu",
        3,
        0,
        6,
        "LECTURE",
        "Slide",
        "Listen",
        "Midterm Exam",
        "Textbook / Slide",
        "None"
));
        request.setTopics(topics);

        List<CreateSyllabusRequest.AssessmentDTO> assessments = new ArrayList<>();
        assessments.add(new CreateSyllabusRequest.AssessmentDTO(
                "Midterm Exam",
                "Thi giữa kỳ",
                "MIDTERM_EXAM",
                30f,
                0f,
                100f,
                1
        ));
        request.setAssessments(assessments);

        try {
            System.out.println("Invoking syllabusService.create()...");
            syllabusService.create(request);
            System.out.println("Syllabus created successfully without errors!");
        } catch (Exception e) {
            System.out.println("!!! CAPTURED EXCEPTION DURING SYLLABUS CREATION !!!");
            e.printStackTrace(System.out);

            Throwable cause = e.getCause();
            if (cause != null) {
                System.out.println("Root Cause Exception:");
                cause.printStackTrace(System.out);
            }
        }
    }
}