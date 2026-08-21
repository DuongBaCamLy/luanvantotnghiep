package com.scse.curriculum.courseprogram.entity;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.coursetype.entity.CourseType;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.syllabus.entity.Syllabus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_program")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseProgram {
@Enumerated(EnumType.STRING)
@Column(name = "term_code", length = 20)
private CurriculumTerm termCode;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id")
    private Cohort cohort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_type_id")
    private CourseType courseType;

    @ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "syllabus_id")
private Syllabus syllabus;

    @Column(name = "semester_suggest")
    private Integer semesterSuggest;

    @Column(name = "year_suggest")
    private Integer yearSuggest;

    @Column(name = "is_required")
    private Boolean required;
}