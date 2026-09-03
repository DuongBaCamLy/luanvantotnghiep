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

    /** The canonical cohort code used by every API and screen. */
    public static String canonicalName(String programCode, Integer entryYear) {
        if (entryYear == null || entryYear < 2000 || entryYear > 2100) {
            throw new IllegalArgumentException("Cohort entry year must be between 2000 and 2100");
        }
        String prefix = programCode == null ? "" : programCode.trim().toUpperCase();
        prefix = prefix.replaceFirst("[-_]?\\d{4}$", "").replaceAll("[^A-Z0-9]", "");
        if (prefix.isBlank()) {
            throw new IllegalArgumentException("Program code is required to generate the cohort code");
        }
        return prefix + entryYear;
    }

    @PrePersist
    @PreUpdate
    private void normalizeName() {
        name = canonicalName(program == null ? null : program.getCode(), entryYear);
    }
}
