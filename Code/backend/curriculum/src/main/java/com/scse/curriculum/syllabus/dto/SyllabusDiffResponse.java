package com.scse.curriculum.syllabus.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyllabusDiffResponse {

    private Integer oldSyllabusId;
    private Integer newSyllabusId;
    private String oldVersionLabel;
    private String newVersionLabel;
    private boolean hasChanges;

    private Map<String, FieldDiff> generalInfoDiff;
    private ListDiff<CloDiff> cloDiff;
    private ListDiff<CloPloMappingDiff> cloPloDiff;
    private ListDiff<TopicDiff> topicDiff;
    private ListDiff<TopicCloMappingDiff> topicCloDiff;
    private ListDiff<AssessmentDiff> assessmentDiff;
    private ListDiff<AssessmentCloMappingDiff> assessmentCloDiff;
    private ListDiff<readingsDiff> readingsDiff;

    @Data
    @Builder
    public static class FieldDiff {
        private String oldValue;
        private String newValue;
    }

    @Data
    @Builder
    public static class ListDiff<T> {
        private List<T> added;
        private List<T> removed;
        private List<T> modified;
    }

    @Data
    @Builder
    public static class CloDiff {
        private String code;
        private String description;
        private String descriptionVn;
        private String competencyLevel;
        private String bloomLevel;
        private Integer orderIndex;
        private List<String> plos;
        private Map<String, FieldDiff> changes;
    }

    @Data
    @Builder
    public static class CloPloMappingDiff {
        private String cloCode;
        private String ploCode;
        private String level;
        private Float contributionWeight;
        private String notes;
        private Map<String, FieldDiff> changes;
    }

    @Data
    @Builder
    public static class TopicDiff {
        private String name;
        private String nameVn;
        private Integer weekNumber;
        private Integer orderInWeek;
        private Map<String, FieldDiff> changes;
    }

    @Data
    @Builder
    public static class TopicCloMappingDiff {
        private String topicName;
        private Integer weekNumber;
        private Integer orderInWeek;
        private String cloCode;
        private String teachingLevel;
        private Map<String, FieldDiff> changes;
    }

    @Data
    @Builder
    public static class AssessmentDiff {
        private String name;
        private String nameVn;
        private Double weightPercent;
        private Integer orderIndex;
        private Map<String, FieldDiff> changes;
    }

    @Data
    @Builder
    public static class AssessmentCloMappingDiff {
        private String assessmentName;
        private Integer orderIndex;
        private String cloCode;
        private Float contributionPercent;
        private Map<String, FieldDiff> changes;
    }

    @Data
    @Builder
    public static class readingsDiff {
        private Integer bookId;
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
        private Map<String, FieldDiff> changes;
    }
}
