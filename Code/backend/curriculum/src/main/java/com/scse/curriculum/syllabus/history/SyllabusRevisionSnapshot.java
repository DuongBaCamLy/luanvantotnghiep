package com.scse.curriculum.syllabus.history;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="syllabus_revision_snapshot", uniqueConstraints=@UniqueConstraint(columnNames={"syllabus_id","version_number","event_type"}))
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class SyllabusRevisionSnapshot {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="syllabus_id", nullable=false, updatable=false) private Integer syllabusId;
    @Column(name="original_syllabus_id", nullable=false, updatable=false) private Integer originalSyllabusId;
    @Column(name="version_number", nullable=false, updatable=false) private Integer versionNumber;
    @Column(name="version_label", nullable=false, updatable=false) private String versionLabel;
    @Column(name="event_type", nullable=false, updatable=false) private String eventType;
    @Column(name="captured_at", nullable=false, updatable=false) private LocalDateTime capturedAt;
    @Column(name="actor", updatable=false) private String actor;
    @Column(name="content", columnDefinition="LONGTEXT", nullable=false, updatable=false) private String content;
}
