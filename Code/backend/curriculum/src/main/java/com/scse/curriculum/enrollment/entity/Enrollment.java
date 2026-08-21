package com.scse.curriculum.enrollment.entity;

import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "enrollment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id")
    private ClassSection classSection;

    @Column(name = "enrolled_at")
    private LocalDate enrolledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EnrollmentStatus status;
}