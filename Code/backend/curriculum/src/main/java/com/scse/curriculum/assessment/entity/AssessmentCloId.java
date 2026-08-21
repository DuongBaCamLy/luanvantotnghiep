package com.scse.curriculum.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AssessmentCloId implements Serializable {

    @Column(name = "assessment_component_id")
    private Integer assessmentComponentId;

    @Column(name = "clo_id")
    private Integer cloId;
}