package com.scse.curriculum.syllabus.entity;

import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.course.entity.Course;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "version_number")
    private Integer versionNumber;

    @Column(name = "version_label")
    private String versionLabel;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "course_designation", columnDefinition = "TEXT")
private String courseDesignation;

@Column(name = "course_types", columnDefinition = "TEXT")
private String courseTypes;

@Column(name = "semester")
private String semester;

@Column(name = "language")
private String language;

@Column(name = "relation", columnDefinition = "TEXT")
private String relation;

@Column(name = "teaching_methods", columnDefinition = "TEXT")
private String teachingMethods;

@Column(name = "workload_total")
private String workloadTotal;

@Column(name = "workload_contact")
private String workloadContact;

@Column(name = "workload_private")
private String workloadPrivate;

@Column(name = "prerequisites", columnDefinition = "TEXT")
private String prerequisites;

@Column(name = "objectives", columnDefinition = "TEXT")
private String objectives;

@Column(name = "exam_forms", columnDefinition = "TEXT")
private String examForms;

@Column(name = "exam_requirements", columnDefinition = "TEXT")
private String examRequirements;

@Column(name = "rubrics", columnDefinition = "TEXT")
private String rubrics;

@Column(name = "major")
private String major;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SyllabusStatus status;

    @Column(name = "is_current")
    private Boolean isCurrent;

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

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(
            mappedBy = "syllabus",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Clo> clos = new ArrayList<>();

    @OneToMany(
            mappedBy = "syllabus",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Topic> topics = new ArrayList<>();

    @OneToMany(
            mappedBy = "syllabus",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<AssessmentComponent> assessments = new ArrayList<>();

    @OneToMany(
            mappedBy = "syllabus",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<SyllabusBook> references = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}