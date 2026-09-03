package com.scse.curriculum.clo.entity;

import com.scse.curriculum.syllabus.entity.Syllabus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id")
    private Syllabus syllabus;

    @Column(nullable = false)
    private String code;

    @Column(columnDefinition = "TEXT",
            nullable = false)
    private String description;

    @Column(name = "description_vn",
            columnDefinition = "TEXT")
    private String descriptionVn;

    @Enumerated(EnumType.STRING)
    @Column(name = "competency_level")
    private CompetencyLevel competencyLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "bloom_level")
    private BloomLevel bloomLevel;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 1;

    @PrePersist
    private void initializeOrderIndex() {
        if (orderIndex == null) orderIndex = 1;
    }
}
