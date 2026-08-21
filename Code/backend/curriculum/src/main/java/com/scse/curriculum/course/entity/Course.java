package com.scse.curriculum.course.entity;

import com.scse.curriculum.department.entity.Department;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "course_code",
            nullable = false,
            unique = true)
    private String courseCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_vn",
            nullable = false)
    private String nameVn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "credit_theory")
    private Integer creditTheory;

    @Column(name = "credit_lab")
    private Integer creditLab;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_level")
    private CourseLevel courseLevel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}