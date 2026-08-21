package com.scse.curriculum.cohort.entity;

import com.scse.curriculum.program.entity.Program;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cohort")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cohort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @Column(name = "entry_year",
            nullable = false,
            columnDefinition = "YEAR")
    private Integer entryYear;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "is_active")
    private Boolean isActive;
}