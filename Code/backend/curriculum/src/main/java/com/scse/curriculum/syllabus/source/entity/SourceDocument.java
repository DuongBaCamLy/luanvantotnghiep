package com.scse.curriculum.syllabus.source.entity;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "source_document")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SourceDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="original_filename", nullable=false, length=255) private String originalFilename;
    @Column(name="content_type", nullable=false, length=150) private String contentType;
    @Column(name="file_size", nullable=false) private Long fileSize;
    @Column(name="sha256", nullable=false, length=64) private String sha256;
    @Column(name="uploaded_at", nullable=false) private LocalDateTime uploadedAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="uploaded_by", nullable=false) private UserAccount uploadedBy;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="program_id", nullable=false) private Program program;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="cohort_id", nullable=false) private Cohort cohort;
    @Lob @Column(name="content", nullable=false, columnDefinition="LONGBLOB") private byte[] content;
}
