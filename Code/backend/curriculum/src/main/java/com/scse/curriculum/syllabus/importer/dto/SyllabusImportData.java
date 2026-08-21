package com.scse.curriculum.syllabus.importer.dto;

import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SyllabusImportData {
    private String sourceCourseCode;
    private String sourceCourseName;
    private String courseDesignation;
    private String courseTypes;
    private String semester;
    private String language;
    private String relation;
    private String teachingMethods;
    private String workloadTotal;
    private String workloadContact;
    private String workloadPrivate;
    private String prerequisites;
    private String objectives;
    private String examForms;
    private String examRequirements;
    private String rubrics;
    private String major;
    @Builder.Default private List<CreateSyllabusRequest.CloDTO> clos = new ArrayList<>();
    @Builder.Default private List<CreateSyllabusRequest.TopicDTO> topics = new ArrayList<>();
    @Builder.Default private List<CreateSyllabusRequest.AssessmentDTO> assessments = new ArrayList<>();
    @Builder.Default private List<ReadingItem> readingList = new ArrayList<>();
    @Builder.Default private List<CloPloMappingItem> cloPloMappings = new ArrayList<>();
    @Builder.Default private List<TopicCloMappingItem> topicCloMappings = new ArrayList<>();
    @Builder.Default private List<AssessmentCloMappingItem> assessmentCloMappings = new ArrayList<>();

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CloPloMappingItem {
        private String cloCode;
        private String ploCode;
        private String level;
        private Float contributionWeight;
        private String notes;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopicCloMappingItem {
        private Integer weekNumber;
        private String topicName;
        private String cloCode;
        private String teachingLevel;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AssessmentCloMappingItem {
        private String assessmentName;
        private String cloCode;
        private Float contributionPercent;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReadingItem {
        private String title;
        private String author;
        private String publisher;
        private Integer year;
        private String edition;
        private String isbn;
        private String url;
        private String bookType;
        private String usageType;
        private Integer orderIndex;
    }
}
