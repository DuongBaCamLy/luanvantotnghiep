package com.scse.curriculum.assessment.entity;

import com.scse.curriculum.clo.entity.Clo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assessment_clo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentClo {

    @EmbeddedId
    private AssessmentCloId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("assessmentComponentId")
    @JoinColumn(name = "assessment_component_id")
    private AssessmentComponent assessmentComponent;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cloId")
    @JoinColumn(name = "clo_id")
    private Clo clo;

    @Column(name = "contribution_percent")
    private Float contributionPercent;
}