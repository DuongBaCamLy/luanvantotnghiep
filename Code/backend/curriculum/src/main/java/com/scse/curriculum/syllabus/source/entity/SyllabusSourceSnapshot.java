package com.scse.curriculum.syllabus.source.entity;

import com.scse.curriculum.syllabus.entity.Syllabus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="syllabus_source_snapshot")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SyllabusSourceSnapshot {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="source_document_id", nullable=false) private SourceDocument sourceDocument;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="syllabus_id", unique=true) private Syllabus syllabus;
    @Column(name="course_code", length=50) private String courseCode;
    @Column(name="start_boundary", nullable=false) private Integer startBoundary;
    @Column(name="end_boundary", nullable=false) private Integer endBoundary;
    @Column(name="sha256", length=64) private String sha256;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @Lob @Column(name="field_map", columnDefinition="LONGTEXT") private String fieldMap;
    @Lob @Column(name="content", columnDefinition="LONGBLOB") private byte[] content;
}
