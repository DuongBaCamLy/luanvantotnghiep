package com.scse.curriculum.cohort.entity;

import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    /*
     * Archive metadata belongs to the Cohort itself.
     *
     * The Cohort remains the same database record when archived/restored.
     * No curriculum copy or duplicate identity is created.
     */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by")
    private UserAccount archivedBy;

    /** The canonical cohort code used by every API and screen. */
    public static String canonicalName(String programCode, Integer entryYear) {
        if (entryYear == null || entryYear < 2000 || entryYear > 2100) {
            throw new IllegalArgumentException(
                    "Cohort entry year must be between 2000 and 2100");
        }

        String prefix = programCode == null
                ? ""
                : programCode.trim().toUpperCase();

        prefix = prefix
                .replaceFirst("[-_]?\\d{4}$", "")
                .replaceAll("[^A-Z0-9]", "");

        if (prefix.isBlank()) {
            throw new IllegalArgumentException(
                    "Program code is required to generate the cohort code");
        }

        return prefix + entryYear;
    }

    @PrePersist
    @PreUpdate
    private void normalizeName() {
        name = canonicalName(
                program == null ? null : program.getCode(),
                entryYear);
    }
}