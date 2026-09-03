package com.scse.curriculum.assessment.entity;

import com.scse.curriculum.syllabus.entity.Syllabus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assessment_component")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id")
    private Syllabus syllabus;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_vn")
    private String nameVn;

    @Column(name = "assessment_type", nullable = false, length = 100)
    private String assessmentType;

    @Column(name = "weight_percent")
    @Builder.Default
    private Float weightPercent = 0.0f;

    @Column(name = "min_score")
    @Builder.Default
    private Float minScore = 0.0f;

    @Column(name = "max_score")
    @Builder.Default
    private Float maxScore = 100.0f;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 1;

    @PrePersist
    private void initializeRequiredValues() {
        if (assessmentType == null || assessmentType.isBlank()) assessmentType = "Assessment";
        if (weightPercent == null) weightPercent = 0.0f;
        if (minScore == null) minScore = 0.0f;
        if (maxScore == null) maxScore = 100.0f;
        if (orderIndex == null) orderIndex = 1;
    }
}
