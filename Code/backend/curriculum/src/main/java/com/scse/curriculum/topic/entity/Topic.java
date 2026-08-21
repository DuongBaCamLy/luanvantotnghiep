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
    private Integer teachingHours;

    @Column(name = "lab_hours")
    private Integer labHours;

    @Column(name = "self_study_hours")
    private Integer selfStudyHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_type")
    private TopicType topicType;

    @Column(name = "teaching_method",
            columnDefinition = "TEXT")
    private String teachingMethod;

    @Column(name = "learning_activity",
            columnDefinition = "TEXT")
    private String learningActivity;

    @Column(columnDefinition = "TEXT")
    private String notes;
}