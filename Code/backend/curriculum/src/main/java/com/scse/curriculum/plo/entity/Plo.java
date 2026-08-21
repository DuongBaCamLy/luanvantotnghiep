package com.scse.curriculum.plo.entity;

import com.scse.curriculum.program.entity.Program;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "plo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_vn",
            columnDefinition = "TEXT")
    private String descriptionVn;

    private String category;

    @Column(name = "version_number")
    private Integer versionNumber;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}