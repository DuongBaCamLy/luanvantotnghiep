package com.scse.curriculum.syllabus.importer.entity;


import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.user.entity.UserAccount;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;



@Entity
@Table(name = "syllabus_import_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusImportHistory {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id",
            nullable = false)
    private Syllabus syllabus;



    @Column(
        name = "original_file_name",
        nullable = false
    )
    private String originalFileName;



    @Column(
        name = "original_file_type",
        nullable = false
    )
    private String originalFileType;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by")
    private UserAccount importedBy;



    @Enumerated(EnumType.STRING)
    @Column(
        name = "import_status",
        nullable = false
    )
    private ImportStatus importStatus;



    @Column(
        name = "error_message",
        columnDefinition = "TEXT"
    )
    private String errorMessage;



    @Column(name = "created_at")
    private LocalDateTime createdAt;



    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    public void prePersist(){

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

    }


    @PreUpdate
    public void preUpdate(){

        updatedAt = LocalDateTime.now();

    }

}