package com.scse.curriculum.studentscore.entity;

import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.enrollment.entity.Enrollment;
import com.scse.curriculum.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_component_id")
    private AssessmentComponent assessmentComponent;

    @Column(name = "raw_score")
    private Float rawScore;

    @Column(name = "final_score")
    private Float finalScore;

    @Column(name = "is_absent")
    private Boolean isAbsent;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private UserAccount recordedBy;
}