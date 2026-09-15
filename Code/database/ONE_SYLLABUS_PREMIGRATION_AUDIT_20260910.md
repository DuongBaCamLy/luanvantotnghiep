Pre-mutation audit: one logical syllabus per course/program/cohort/semester

Live database inspected before changes: curriculum_iu. All labels are canonical. Four old submit/archive chains share identical business identity:

| Course ID | Program | Cohort | Semester | Obsolete row | Survivor |
|---|---|---|---|---|---|
| 27 | CS-2021 | CS2026 | Semester 8 | 3047 v1.0 ARCHIVED | 3051 v2.0 SUBMITTED |
| 36 | CS-2021 | CS2026 | Semester 7 | 3046 v1.0 ARCHIVED | 3053 v2.0 SUBMITTED |
| 53 (IT116IU) | CS-2021 | CS2021 | Semester 2 | 3054 v2.0 ARCHIVED | 3055 v3.0 APPROVED |
| 116 | CS-2021 | CS2026 | NULL (ELECTIVE) | 3038 v1.0 ARCHIVED | 3052 v2.0 SUBMITTED |

The elective's linked CourseProgram 297 also has null semesterSuggest and term ELECTIVE; preserve this unscheduled identity. Do not invent a semester. Existing CourseProgram links already point to survivors: 286->3051, 285->3053, 196->3055, 297->3052. ClassSection 11 points to 3055. CourseProgram 246->3013 is the separate CS2026 IT116IU Draft and must not be merged.

Approval requests: 26->3051 PENDING, 27->3052 PENDING, 28->3053 PENDING, 29/30->3055 APPROVED. Existing approval_request has no revision snapshot, so mutable same-ID revisions would mislabel history unless a revision number is added/backfilled before transitions.

Source snapshots are original-document fragments (binary content + field map), not complete structured revision history. None references the eight affected rows. Existing audit records must remain untouched. Add a separate immutable structured workflow/revision snapshot table. Preserve old row IDs in snapshots so historical audit references remain traceable. Capture owned detail rows and relationship data before deletion; preserve shared Book/SourceDocument rows and all assignment records.

Current unique index (course_id, version_number) is incompatible with independent cohort revision counters and must be replaced after duplicate consolidation. New logical identity uniqueness must also cover the existing unscheduled elective without changing its semester.

Planned mutation is guarded, transactional and rollback-by-default. Schema DDL is a separate auditable phase because MySQL DDL implicitly commits. Main table survivor content/status/version/timestamps must be identical after migration. Snapshot/archive historical children before deleting obsolete rows, and verify final unique identities and exact IT116IU states before commit.
