package com.scse.curriculum.cloplomapping.entity;

import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.plo.entity.Plo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "clo_plo_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "clo_id",
                                "plo_id"
                        })
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloPloMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clo_id")
    private Clo clo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plo_id")
    private Plo plo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContributionLevel level;

        @Builder.Default
        @Column(name = "contribution_weight", nullable = false)
        private Float contributionWeight = 1.0f;

    @Column(columnDefinition = "TEXT")
    private String notes;

        @PrePersist
        @PreUpdate
        private void applyDefaultContributionWeight() {
                if (contributionWeight == null) {
                        contributionWeight = 1.0f;
                }
        }
}