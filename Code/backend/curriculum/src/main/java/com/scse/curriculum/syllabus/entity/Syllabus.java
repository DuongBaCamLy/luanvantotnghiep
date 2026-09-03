package com.scse.curriculum.syllabus.entity;

import org.hibernate.annotations.BatchSize;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.syllabus.importer.entity.SyllabusImportHistory;
import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.user.entity.UserAccount;


import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



@Entity
@Table(name = "syllabus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Syllabus {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;



    /*
     * ==========================
     * Course Reference
     * ==========================
     */


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;





    /*
     * ==========================
     * Version Management
     * ==========================
     */


    @Column(name = "version_number")
    private Integer versionNumber;



    @Column(name = "version_label")
    private String versionLabel;



    @Column(name = "academic_year")
    private String academicYear;






    /*
     * ==========================
     * Snapshot Metadata
     *
     * Keep historical information
     * independent from Course
     * ==========================
     */


    @Column(name = "course_code_snapshot")
    private String courseCodeSnapshot;



    @Column(name = "course_name_snapshot")
    private String courseNameSnapshot;



    @Column(name = "program")
    private String program;






    /*
     * ==========================
     * Import Information
     * ==========================
     */


    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    @Builder.Default
    private SyllabusSourceType sourceType =
            SyllabusSourceType.MANUAL;




    @Column(name = "original_file_name")
    private String originalFileName;




    @Column(name = "original_file_type")
    private String originalFileType;





    @Enumerated(EnumType.STRING)
    @Column(name = "import_status")
    @Builder.Default
    private SyllabusImportStatus importStatus =
            SyllabusImportStatus.NONE;




    @Column(name = "final_approval_date")
    private LocalDateTime finalApprovalDate;







    /*
     * ==========================
     * General Information
     * ==========================
     */


    @Column(name = "course_designation",
            columnDefinition = "TEXT")
    private String courseDesignation;



    @Column(name = "course_types",
            columnDefinition = "TEXT")
    private String courseTypes;



    @Column(name = "semester")
    private String semester;



    @Column(name = "language")
    private String language;



    @Column(name = "relation",
            columnDefinition = "TEXT")
    private String relation;



    @Column(name = "teaching_methods",
            columnDefinition = "TEXT")
    private String teachingMethods;



    @Column(name = "workload_total")
    private String workloadTotal;



    @Column(name = "workload_contact")
    private String workloadContact;



    @Column(name = "workload_private")
    private String workloadPrivate;



    @Column(name = "prerequisites",
            columnDefinition = "TEXT")
    private String prerequisites;



    @Column(name = "objectives",
            columnDefinition = "TEXT")
    private String objectives;



    @Column(name = "exam_forms",
            columnDefinition = "TEXT")
    private String examForms;



    @Column(name = "exam_requirements",
            columnDefinition = "TEXT")
    private String examRequirements;



    @Column(name = "rubrics",
            columnDefinition = "TEXT")
    private String rubrics;



    @Column(name = "major")
    private String major;







    /*
     * ==========================
     * Workflow Status
     * ==========================
     */


    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private SyllabusStatus status =
            SyllabusStatus.DRAFT;




    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private Boolean isCurrent = Boolean.FALSE;





    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;




    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private UserAccount approvedBy;




    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;




    @Column(name = "approved_at")
    private LocalDateTime approvedAt;




    @Column(name = "change_summary",
            columnDefinition = "TEXT")
    private String changeSummary;




    @Column(name = "notes",
            columnDefinition = "TEXT")
    private String notes;







    /*
     * ==========================
     * CLO
     * ==========================
     */


    @OneToMany(
            mappedBy = "syllabus",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Clo> clos =
            new ArrayList<>();








    /*
     * ==========================
     * Teaching Content / Topics
     * ==========================
     */

@BatchSize(size = 50)
@OneToMany(
        mappedBy = "syllabus",
        cascade = CascadeType.ALL,
        orphanRemoval = true
)
@Builder.Default
private List<Topic> topics =
        new ArrayList<>();








    /*
     * ==========================
     * Assessment Plan
     * ==========================
     */


 @BatchSize(size = 50)
@OneToMany(
        mappedBy = "syllabus",
        cascade = CascadeType.ALL,
        orphanRemoval = true
)
@Builder.Default
private List<AssessmentComponent> assessments =
        new ArrayList<>();







    /*
     * ==========================
     * Reading List
     * ==========================
     */


@BatchSize(size = 50)
@OneToMany(
        mappedBy = "syllabus",
        cascade = CascadeType.ALL,
        orphanRemoval = true
)
@Builder.Default
private List<SyllabusBook> references =
        new ArrayList<>();








    /*
     * ==========================
     * Import History
     *
     * Phase 2
     *
     * Track:
     * - PDF/DOCX file
     * - importer
     * - status
     * - error
     * ==========================
     */


    @OneToMany(
            mappedBy = "syllabus",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<SyllabusImportHistory> importHistory =
            new ArrayList<>();








    /*
     * ==========================
     * Audit
     * ==========================
     */


    @Column(name = "created_at")
    private LocalDateTime createdAt;



    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    private void initializeAuditTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        if (isCurrent == null) {
            isCurrent = Boolean.FALSE;
        }
        if (status == null) {
            status = SyllabusStatus.DRAFT;
        }
        if (sourceType == null) {
            sourceType = SyllabusSourceType.MANUAL;
        }
        if (importStatus == null) {
            importStatus = SyllabusImportStatus.NONE;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    private void refreshUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }



}
