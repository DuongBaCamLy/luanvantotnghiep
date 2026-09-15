Syllabus version format audit - 2026-09-10

All requested completion checks pass: compiled production generators use canonical whole-revision labels, live noncanonical count is zero, backend tests pass, and frontend build/lint pass. No minor-revision domain was found or added.

A. Files changed

The repository already had extensive uncommitted work. [Exact task file list](version_label_audit_20260910/changed-files.txt) distinguishes this task's 42 source, test, setup and report files; the evidence files in `version_label_audit_20260910/` are also new.

Backend changes cover the shared formatter, entity read/write boundaries, create/submit/revision/clone, import confirmation, catalog/list DTOs and mappers, dashboard fallback, PDF and semester-report exporters. The older backend-root `SyllabusImportServiceImpl.java` copy was also updated: local apply scripts copy it into production source, so leaving its generator unchanged would reintroduce the bug.

Frontend changes cover shared formatting and numeric comparison, API/form payloads, import preview, create/edit/detail/history/list, cohort comparison labels, assignment and approval pages, dean/department dashboards, heatmap fallback, clone-template descriptions, and editor fields. Version fields are read-only; layout and workflow are unchanged. The list's version-history filter now includes its existing `historyMode` input in memo dependencies, resolving the lint warning.

Setup changes: 38 first-version seed rows previously used the year-based example `v2022.1`; their authoritative version_number is 1 and the workflow has no minor semantics, so the seed identifiers now use `v1.0`. Original source-year descriptions remain intact. The schema DDL comment now illustrates `v1.0, v2.0`. Neither setup SQL file was executed against live data.

B. Central canonical formatter

- Backend: `Code/backend/curriculum/src/main/java/com/scse/curriculum/syllabus/entity/SyllabusVersion.java`.
- `format(Integer)` requires a positive numeric revision and produces `vN.0`.
- `display(Integer, String)` uses the authoritative number, with a narrow legacy fallback when the number is absent. It never mutates historical entities or timestamps.
- `requireCanonical(Integer, String)` rejects malformed, minor or conflicting labels.
- `Syllabus.getVersionLabel()` is the common read boundary used by API, approval, dashboard, diff, curriculum map, export and notification mappers. JPA insert/update callbacks validate the actual stored field, so a canonical getter cannot hide an invalid write. Missing labels are initialized from the authoritative number.
- Update requests cannot rename a workflow version; create/import derive their own number and label rather than trusting client/source text.
- Frontend: `Code/frontend/src/lib/syllabusVersion.ts`; prefers a consistent canonical backend label, otherwise formats the numeric revision or recognized legacy text. Missing/invalid identity displays a dash rather than inventing a version. `compareSyllabusVersions` orders numerically and is used by detail/history.

Catalog/list APIs now expose explicit `versionNumber` and `versionLabel` while preserving their existing canonical `version` compatibility alias. Other existing syllabus responses retain their numeric and canonical-label fields.

C. Previously noncanonical production generation

| Location | Previous behavior | Final behavior |
|---|---|---|
| Shared import confirmation, plus backend-root copy used by apply scripts | `Version ` + nextVersion | Central `SyllabusVersion.format(nextVersion)` |
| Syllabus update and editable form fields | Arbitrary caller-supplied version label | Read-only UI and matching-label validation |
| InstructorAssignmentsPage | `Version N` | Shared frontend formatter |
| SyllabusListPage delete confirmation | `Version N` fallback | Shared frontend formatter |
| CohortSyllabusDiffPage | `vN`, or incorrectly `Version <syllabus ID>` | Numeric/label formatter; never use record ID as version |
| List, create/template descriptions, approval page/dialog, detail/editor, department/dean dashboards and heatmap | Local `vN` fallbacks | Canonical `vN.0` fallback |
| ClassSectionManagementPage and general-info editor | `v` followed by integer | Canonical formatter |
| DashboardServiceImpl search fallback and SyllabusPdfRenderer | `vN` | Backend formatter |
| SyllabusSemesterReportExporter | Bare version_number fallback | Backend formatter |
| Seed data and old schema examples | Year-shaped version identifiers unrelated to numeric workflow revision | Whole-revision examples |

Existing canonical create/submit/rejected-revision/clone generators were consolidated into the same backend helper. Frontend preview/form generators were consolidated as well.

Reviewed displays that already consume backend labels include SyllabusTable, ReportManagementPage, Admin/Faculty dashboards, CloneSyllabusDialog, CourseDetailsModal, curriculum timeline/map and structural/semantic comparison components. They receive canonical values through the common backend read boundary. Comparison algorithms and previous-cohort selection were not changed. Existing repository/report/dashboard orderings use numeric versionNumber, not lexical labels; report-page composite sorting already specifies numeric comparison.

Generated PDF header/body/footer and filenames use canonical version data; Excel/PDF semester reports use the common formatter. Original Word/source-document downloads preserve their source content and filenames; they do not generate a new system version identifier. Original source text and historical notification/audit payloads are not rewritten.

D. Database normalization

[Guarded one-time SQL](normalize_syllabus_version_labels_20260910.sql) reviewed all 60 live rows and normalized exactly 55 labels: 54 `Version 1` values and one `Version 2` value (3054). [Exact IDs and before/after labels](version_label_audit_20260910/normalized-rows.json).

Safety controls:

- Exact reviewed ID/number/label/status manifest; abort on drift.
- Serializable transaction and locked syllabus rows.
- InnoDB and no unexpected syllabus triggers.
- Recognized whole-revision forms only; unknown or nonzero-minor values abort before update.
- Only version_label is updated; explicit `updated_at=updated_at` prevents MySQL automatic timestamp advancement.
- Every other column is compared with its transaction snapshot before commit.
- Default execution is a rollback rehearsal; commit requires `@version_normalization_commit=1`.
- SQL errors rollback and SIGNAL. Temporary-table reopen and collation issues encountered during rehearsal were corrected; both failed attempts rolled back without data changes. The final rehearsal and commit passed.

Backup: `tmp/version-syllabus-before.sql`; final export: `tmp/version-syllabus-after.sql`. These contain only the syllabus table, not original uploaded-file blobs. PowerShell output is UTF-16. No restore, row deletion, status transition, or content update was performed.

Independent exact dump comparison confirms the same 60 rows remain, exactly 55 labels differ, and all other fields including statuses, content and timestamps are identical. [Reproducible comparison script](version_label_audit_20260910/verify_label_only_changes.py).

E. Before/after examples

| ID | Number | Before | After | Status preserved |
|---|---:|---|---|---|
| 3000 | 1 | Version 1 | v1.0 | DRAFT |
| 3047 | 1 | Version 1 | v1.0 | ARCHIVED |
| 3013 | 1 | v1.0 | v1.0 | DRAFT |
| 3054 | 2 | Version 2 | v2.0 | ARCHIVED |
| 3055 | 3 | v3.0 | v3.0 | APPROVED |

F. Remaining "Version" occurrences

[Remaining literal inventory](version_label_audit_20260910/remaining-version-literals.json) records file/line/text for the requested stale-literal searches in source and setup scripts.

- `SyllabusContentGuard`: natural sentence `Version vN.0 is not editable...`.
- `SyllabusSubmissionValidationService`: natural checklist label, `Version label is required.`
- Frontend headings and controls such as Version, Version Label, Version Number, Version Comparison, previous-version text and history availability messages name a concept, not an identifier.
- `i18n/en.ts` app.version is the application's release label, not a syllabus version. PLO version labels and deadline revisions belong to separate domains and were not changed.
- Formatter parsing recognizes legacy `Version N` text without emitting it. Regression tests deliberately contain malformed/legacy strings to prevent regressions.
- `.versionLabel(...)` builders either call the formatter or receive the canonical entity getter. PDF/email record accessors and frontend helper calls only consume canonical labels.
- The only local numeric label construction in active frontend source is inside the shared helper; the only backend construction is inside SyllabusVersion.
- Historical SQL dumps, `.bak` files, source-document text, earlier cleanup safety manifests and previous reports preserve historical evidence. They are not runtime generators and were not rewritten. The active seed and apply-script source copy were fixed.

G. Backend verification

Final `.\mvnw.cmd test`: **271 tests, 0 failures, 0 errors, 1 skipped; BUILD SUCCESS; exit 0**. [Summary](version_label_audit_20260910/backend-test-summary.txt).

Targeted run: **45 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. This ran before adding the numeric v9/v10 curriculum-map regression; that additional test passes in the final full suite. [Targeted summary](version_label_audit_20260910/targeted-test-summary.txt).

Coverage includes initial create v1.0; submit v2.0; rejection preserving v2.0 and revision Draft v3.0; resubmit/approve v4.0; canonical clone; DOCX/XLSX/PDF confirmation creating v1.0 while preserving source content; legacy read normalization; insert/update rejection of malformed/minor/conflicting labels; client rename rejection; and numeric v10 selection over v9 within the same cohort. Existing completeness-nonblocking and canonical cohort/semester tests still pass. Tests run against curriculum_iu_test, not live curriculum_iu.

The first full test run exposed one obsolete curriculum-map assertion expecting v2. It was updated to v2.0; no test failures remain.

H. Frontend verification

- `npm.cmd run build`: **PASS, exit 0**. Existing large-bundle warning remains; no build errors. [Summary](version_label_audit_20260910/frontend-build-summary.txt).
- `npm.cmd run lint`: **PASS, exit 0, no warnings/errors**. [Output](version_label_audit_20260910/frontend-lint.txt).
- `npm.cmd run test:version`: **3 tests passed, 0 failed**, covering canonical/legacy/missing labels and numeric v9/v10 sorting using the production helper. [Output](version_label_audit_20260910/frontend-tests.txt).

I. Live database verification

[All 60 live rows, ordered by course_id/version_number/id](version_label_audit_20260910/live-final.txt).

```text
noncanonical_versions = 0
labels_not_matching_numeric_version = 0
```

The first query checks the requested v<major>.<minor> regex; the second is stricter and checks exact equality to v<version_number>.0. Approved/history records are preserved.

J. Remaining risks and database constraint decision

No version-format failure remains in the source, tests or verified live data. An already-running backend must be restarted/redeployed to load the new formatter and persistence guards; this task did not restart application processes or execute a live workflow that changes statuses. Original historical files and payloads can still contain their original text by design.

No schema constraint migration was applied. Application-level insert/update validation is enforced, while direct SQL and an older running application bypass it. After deploying the new application and reviewing external writers, a separate DB CHECK can require a positive version_number, non-null version_label, and binary equality to CONCAT('v',version_number,'.0'). All current rows satisfy that proposed condition. Keeping this separate avoids silently breaking older writers during a format-only data repair; it is the remaining operational hardening option, not an unverified data migration.
