IT116IU cleanup and canonical workflow verification - 2026-09-10

A. Files changed in this task

The repository already contained extensive uncommitted work. This task edited these production files under `Code/backend/curriculum/src/main/java/com/scse/curriculum/`:

- `syllabus/service/SyllabusServiceImpl.java:1582`: require linked curriculum semesterSuggest 1-8; remove fallback to stale Draft semester.
- `syllabus/service/SyllabusAccessService.java:660`: generate Semester N.
- `deadline/service/DeadlineEscalationDeliveryService.java:154`, `DeadlineReminderDeliveryService.java:129`, `SyllabusDeadlineService.java:601`: syllabus-facing deadline labels use Semester N. No SMTP configuration changes.
- `report/controller/ReportController.java:102,117,212`: export filenames no longer generate _HK.
- `syllabus/service/SyllabusSubmissionValidationService.java:247`: checklist guidance advertises canonical Semester format.

Tests under `Code/backend/curriculum/src/test/java/com/scse/curriculum/`:

- `syllabus/service/SyllabusServiceImplCloneTest.java:471`: stale 2026-2027/HK1 Draft linked to CS2026/semesterSuggest 2 submits as CS2026/Semester 2/CS-2021; old Draft archived; both links moved; canonical pending uniqueness query verified; missing objectives do not prevent submission; no validation-service interactions.
- Same file, `:773`: new regression proves missing curriculum semester cannot fall back to stale metadata or create a version/approval.
- `syllabus/service/SyllabusAccessServiceTest.java:159`: canonical semester expectation updated.

SQL, this report, and the `it116_cleanup_audit_20260910/` evidence directory are also new. No migrations, UI redesigns, Course API/domain removals, SMTP setup, or AI comparison implementation changes were made.

B. SQL scripts and backup

- [Primary cleanup](cleanup_it116_invalid_workflow_versions_20260910.sql): default execution rehearses and rolls back; `SET @cleanup_commit=1` enables commit. Validates identities, links, the 15 incoming FK edges including descendants, transactional engines, enabled FK checks, absence of cleanup-table triggers and cross-syllabus child links. Locks course/version/assignment ranges. Errors rollback and SIGNAL. All final invariants are checked before commit. Run without mysql `--force`. Intentionally aborts on already-cleaned data.
- [Timestamp correction](cleanup_it116_preserve_timestamp_20260910.sql): MySQL ON UPDATE initially advanced 3013.updated_at. Restored its exact original timestamp from the fresh backup. The first correction safely aborted on a timezone mismatch; explicit UTC, matching mysqldump, succeeded. Primary script now explicitly preserves updated_at.
- [Read-only verification](verify_it116_cleanup_20260910.sql).
- [Commit evidence](it116_cleanup_audit_20260910/it116-cleanup-commit.log) and [exact deleted row keys](it116_cleanup_audit_20260910/it116-row-diff.json).

Fresh affected-table backup: `tmp/it116-affected-tables-before-20260910.sql`. PowerShell wrote UTF-16; decode/convert before use. No restore was performed. Attempting a full backup exhausted disk space because of original uploaded-file data; the incomplete file was removed. The affected-table backup succeeded. The source snapshot table had zero target rows and its checksum remained unchanged.

C. Exact database changes

| Table | Deleted rows |
|---|---:|
| syllabus: 3048, 3049, 3050 | 3 |
| approval_request: 23, 24, 25 | 3 |
| syllabus_import_history | 1 |
| syllabus_book: links only | 3 |
| assessment_clo | 36 |
| assessment_component | 12 |
| topic_clo | 81 |
| topic | 45 |
| clo_plo_mapping | 9 |
| clo | 9 |
| student_score | 0 |
| syllabus_source_snapshot | 0 |

Total: 202 deleted rows, including 3 invalid syllabuses.

Final updates: course_program 196.syllabus_id from 3050 to NULL; class_section 11.syllabus_id from 3050 to NULL; syllabus 3013.version_label from Version 1 to v1.0. The timestamp correction restores the original value, with no final before/after difference.

Before/after dump comparison verifies every retained row in affected tables is identical except those three fields. In particular, 3013 content and timestamps and the complete course_program 246 row are unchanged. Shared-table checksums match for Course, Cohort, Program, Book, SourceDocument and SyllabusSourceSnapshot. No original filesystem files were touched. The comparison script and before/after checksums are in the audit directory.

D. Final live state

```text
IT116IU:
3013 | 1 | v1.0 | CS2026 | Semester 2 | DRAFT

course_program:
196 | CS2021 | NULL
246 | CS2026 | 3013

class_section:
11 | syllabus_id NULL
```

All nine direct syllabus FK tables have zero references to 3048/3049/3050: approval_request, assessment_component, class_section, clo, course_program, syllabus_book, syllabus_import_history, syllabus_source_snapshot, topic. No invalid syllabus remains. [Query output](it116_cleanup_audit_20260910/it116-final-state.txt).

E. Canonical submit review

Current submit already derives academicYear and program from linked CourseProgram cohort.name/program.code. Pending uniqueness uses canonical values at SyllabusServiceImpl.java:1615-1620. The submitted builder uses them at :1648, :1663 and :1690. ClassSection and CourseProgram links both move to the new version; old Draft becomes ARCHIVED. The edit at :1582 removes the remaining stale-semester fallback. Submit never calls validateOrThrow; completeness remains an explicit checklist endpoint.

getCreateContext() at :591 already generates Semester N from semesterSuggest and needed no further change. Current creation code was inspected and already derives linked cohort/program/semester context.

F. Remaining production HK references

- `courseprogram/entity/CurriculumTerm.java`: HK1-HK8 enum and parser normalization are legitimate internal term/database concepts.
- `syllabus/importer/service/SyllabusImportServiceImpl.java:534,873`: constructs CurriculumTerm enum values; legitimate internal use, not syllabus-facing strings.
- `report/service/CurriculumChangeReportExporter.java:83,128`: Vietnamese old/new curriculum semester column headings; not syllabus business metadata.
- `syllabus/service/SyllabusSubmissionValidationService.java:47`: optional checklist regex accepts legacy HK input; does not generate or persist HK. Guidance now uses Semester.
- `syllabus/service/SyllabusServiceImpl.java:1591`: explanatory comment about legacy metadata; no generated value.
- `.bak` files are not production Java and were not modified.

G. Tests and verification

- Initial full `.\mvnw.cmd test`: 248 tests, 0 failures, 0 errors, 1 skipped; BUILD SUCCESS.
- Final full `.\mvnw.cmd test`: 249 tests, 0 failures, 0 errors, 1 skipped; BUILD SUCCESS. [Full summary](it116_cleanup_audit_20260910/full-test-summary.txt).
- Targeted SyllabusServiceImplCloneTest, SyllabusServiceImplCreateTest, SyllabusAccessServiceTest, SyllabusServiceImplDeleteTest: 31 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS and final command exit 0. [Targeted summary](it116_cleanup_audit_20260910/targeted-test-summary.txt).
- Tests use curriculum_iu_test, not production curriculum_iu.
- One intermediate test invocation was interrupted by the failed backup's disk-space exhaustion; reran successfully after removal. PowerShell native-stderr handling reported exit 1 despite Maven BUILD SUCCESS; the targeted run confirmed exit 0 with cmd redirection.
- Cleanup rollback rehearsal, transactional postconditions, independent final FK queries, shared-table checksums and affected-table row comparison all passed.

H. Golden workflow readiness and limits

Database cleanup invariants and automated tests pass. A live browser/API golden workflow was not executed: submitting 3013 would change the explicitly required final DRAFT state. Existing automated tests cover submit/reject/revision/resubmit/approval as well as canonical metadata.

The only live IT116IU class section is 11: active, cohort CS2021, academic_year 2026-2027, semester 1, now unlinked. Its other metadata was preserved as requested. There is no CS2026/Semester 2 teaching assignment for an instructor-based golden workflow. Configure the correct assignment deliberately before testing assignment relinking. No backend process was restarted; reload/restart the backend with the changed code before a live test. Neither setup step was silently performed during cleanup.
