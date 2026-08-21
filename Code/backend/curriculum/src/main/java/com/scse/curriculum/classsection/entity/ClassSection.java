package com.scse.curriculum.classsection.entity;

import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.syllabus.entity.Syllabus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * FR-03.1: phân công phải tồn tại trước khi giảng viên tạo đề cương.
     * Vì vậy syllabus được phép null và sẽ được gắn tự động sau khi Faculty
     * tạo Draft đúng môn/học kỳ/năm học.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = true)
    private Syllabus syllabus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @Column(nullable = false)
    private Integer semester;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(name = "group_number", nullable = false)
    private Integer groupNumber;

    @Column(name = "lab_group")
    private Integer labGroup;

    @Column(name = "max_students")
    private Integer maxStudents;

    private String room;
    private String schedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false)
    private SectionType sectionType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
