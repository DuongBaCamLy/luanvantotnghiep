package com.scse.curriculum.topic.entity;

import com.scse.curriculum.syllabus.entity.Syllabus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "topic")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id")
    private Syllabus syllabus;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "order_in_week")
    private Integer orderInWeek;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_vn")
    private String nameVn;

    @Column(name = "teaching_hours")
    @Builder.Default
    private Integer teachingHours = 3;

    @Column(name = "lab_hours")
    @Builder.Default
    private Integer labHours = 0;

    @Column(name = "self_study_hours")
    @Builder.Default
    private Integer selfStudyHours = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_type")
    @Builder.Default
    private TopicType topicType = TopicType.LECTURE;

    @Column(name = "teaching_method",
            columnDefinition = "TEXT")
    private String teachingMethod;

    @Column(name = "learning_activity",
            columnDefinition = "TEXT")
    private String learningActivity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assessments",
        columnDefinition = "TEXT")
private String assessments;


@Column(name = "resources",
        columnDefinition = "TEXT")
private String resources;

@PrePersist
private void initializeRequiredValues() {
    if (weekNumber == null) weekNumber = 1;
    if (orderInWeek == null) orderInWeek = 1;
    if (teachingHours == null) teachingHours = 3;
    if (labHours == null) labHours = 0;
    if (selfStudyHours == null) selfStudyHours = 0;
    if (topicType == null) topicType = TopicType.LECTURE;
}
}
