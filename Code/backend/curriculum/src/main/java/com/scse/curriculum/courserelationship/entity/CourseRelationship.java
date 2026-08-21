package com.scse.curriculum.courserelationship.entity;

import com.scse.curriculum.course.entity.Course;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_relationship")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_course_id")
    private Course relatedCourse;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type")
    private RelationType relationType;
}