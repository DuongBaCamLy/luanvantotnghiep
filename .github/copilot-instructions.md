# Copilot Instructions — Curriculum Management System

## 1. Project Overview

This is a graduation thesis project for a Curriculum Management System.

The project manages:

* Academic programs
* Courses
* Curricula
* Syllabi
* Topics
* Instructors
* PLO/CLO mappings
* Approvals
* Program and syllabus differences
* Syllabus import and validation

The project contains both backend and frontend applications.

---

## 2. Technology Stack

### Backend

The backend is located at:

`Code/backend/curriculum`

Use the existing backend architecture and conventions.

Before modifying backend code:

* Inspect the related controller.
* Inspect the service layer.
* Inspect repositories/entities/DTOs involved.
* Inspect existing tests.
* Reuse existing services and utilities where possible.
* Do not introduce a new architecture when an existing implementation already solves the problem.

### Frontend

The frontend is located at:

`Code/frontend`

The frontend uses:

* React
* TypeScript
* Vite

Important areas include:

`Code/frontend/src/pages/admin`

`Code/frontend/src/pages/syllabus`

`Code/frontend/src/pages/program`

`Code/frontend/src/types`

`Code/frontend/src/store`

`Code/frontend/src/routes`

Follow the existing component, API, state-management, routing, and TypeScript conventions.

---

## 3. Critical Development Rule

Do NOT make broad or unrelated changes.

When fixing a bug:

1. Identify the exact flow involved.
2. Find the smallest set of files responsible for the problem.
3. Modify only those files unless another change is demonstrably required.
4. Preserve existing functionality.
5. Do not rewrite working modules unnecessarily.
6. Do not remove existing code simply because it appears unused without proving it is safe to remove.

Never modify unrelated Admin, Program, Course, Instructor, Approval, or Authentication functionality when working on Syllabus unless required by the actual dependency chain.

---

## 4. Syllabus Module

The Syllabus module is a critical part of the project.

Relevant frontend pages include:

* `SyllabusCreatePage.tsx`
* `SyllabusDetailPage.tsx`
* `SyllabusEditPage.tsx`
* `SyllabusEditorPage.tsx`
* `SyllabusListPage.tsx`
* `SyllabusDiffPage.tsx`
* `CohortSyllabusDiffPage.tsx`
* `InstructorAssignmentsPage.tsx`

Relevant frontend types include:

* `syllabus.ts`
* `syllabusDiff.ts`
* `syllabusImport.ts`
* `topic.ts`
* `course.ts`
* `instructor.ts`

There are also syllabus import templates under:

`Code/templates`

---

## 5. Critical Syllabus Import Requirement

A major requirement is:

Admin should be able to create a syllabus for a new course and import a syllabus file so that the system automatically extracts and populates the syllabus data.

The user should NOT be forced to manually enter all syllabus fields when creating a new syllabus.

The intended flow is:

Admin
→ Add Syllabus
→ Create/select syllabus context
→ Import file
→ Parse file
→ Extract syllabus data
→ Validate imported data
→ Preview changes
→ Confirm
→ Save to database
→ Open/edit resulting syllabus

Supported import formats should follow the existing implementation and requirements. Do not invent unsupported formats.

---

## 6. Syllabus Import Investigation Rule

When investigating an import bug, trace the complete flow before modifying anything:

Frontend UI
→ API request
→ Backend controller
→ Service
→ Parser/import logic
→ Validation
→ DTO/entity mapping
→ Database persistence
→ Frontend response
→ Editor/preview

Identify the actual failure point before proposing a fix.

Do not assume that the frontend is the problem just because the import button or dialog does not behave correctly.

---

## 7. Import Data Rules

Imported data must respect:

* Existing database relationships
* Existing syllabus/course structure
* Existing validation rules
* Existing DTOs
* Existing naming conventions
* Existing business rules

Do not silently discard imported fields.

Do not overwrite existing syllabus data without following the existing update/diff/confirmation mechanism.

For a new syllabus, imported data should be used to populate the new syllabus where the existing business rules allow it.

---

## 8. Validation

Validation is important.

Before saving imported data:

* Validate required fields.
* Validate references to existing entities.
* Detect invalid or missing data.
* Preserve existing validation behavior.
* Return meaningful errors to the frontend.

Do not weaken validation merely to make the import succeed.

If validation fails, explain exactly which field or record caused the failure.

---

## 9. Testing Requirements

Before declaring a bug fixed:

1. Inspect existing tests.
2. Add or update focused tests when appropriate.
3. Run the smallest relevant test suite first.
4. If the focused tests pass, run broader tests when necessary.
5. Do not claim that a fix is successful without test evidence.

For syllabus import changes, prioritize:

* Parser tests
* Validation tests
* Service tests
* Controller/API tests
* Frontend import-flow tests where available

Existing test files and scripts must be reused when applicable.

---

## 10. Database Safety

Do not modify database schema casually.

Before changing:

* entities
* tables
* columns
* relationships
* constraints

inspect the existing schema and related SQL/migration files.

If a database change is required, clearly explain:

* why it is required
* which tables are affected
* whether existing data remains compatible
* whether migration is required

Never drop or recreate production-like data merely to make a test pass.

---

## 11. API Safety

Before changing an API:

* Find existing callers.
* Check frontend usage.
* Check tests.
* Preserve backward compatibility where possible.

Do not change endpoint names, request structures, or response structures unnecessarily.

If an API contract must change, update all known consumers and tests.

---

## 12. TypeScript Rules

Prefer existing TypeScript types.

Do not use `any` merely to bypass a type error.

Do not disable TypeScript checks to make code compile.

If a new type is required, place it according to the existing project structure.

Keep API request/response types consistent with the backend.

---

## 13. React Rules

Follow existing React patterns.

Before creating a new component or hook:

* Search for an existing reusable implementation.
* Reuse existing components when possible.

Do not introduce a new state-management library.

Do not rewrite an entire page for a small UI bug.

Keep loading, error, empty, success, and validation states consistent with existing pages.

---

## 14. Error Handling

Do not hide errors.

Bad pattern:

```text
catch error → ignore error → continue
```

Errors should be:

* logged appropriately
* returned to the appropriate layer
* displayed meaningfully to the user when relevant

Avoid exposing sensitive backend information to the frontend.

---

## 15. Security

Never hard-code:

* passwords
* API keys
* access tokens
* private keys
* database credentials
* secrets

Never commit `.env` files containing secrets.

Do not weaken authentication or authorization to solve a functional issue.

Respect existing role-based access control.

---

## 16. Git Safety

Do not execute destructive Git commands automatically.

Never run without explicit user approval:

* `git reset --hard`
* `git clean -fd`
* `git push --force`
* history rewriting
* mass file deletion

Do not discard the user's existing work.

Before suggesting a destructive Git operation, explain its effect and request confirmation.

---

## 17. Working Method

For every non-trivial task, follow this sequence:

### Step 1 — Understand

Inspect the relevant code and trace dependencies.

### Step 2 — Explain

Explain:

* current behavior
* root cause
* affected files
* proposed fix

### Step 3 — Plan

Provide a minimal implementation plan.

### Step 4 — Modify

Only modify files required for the task.

### Step 5 — Test

Run relevant tests and checks.

### Step 6 — Report

Report:

* files changed
* behavior changed
* tests executed
* test results
* remaining risks

Do not claim success without evidence.

---

## 18. Important Instruction for Copilot

When the user asks to "fix" something, do not immediately rewrite code.

First inspect the existing implementation and determine the root cause.

If multiple possible causes exist, investigate them before making changes.

Prefer the smallest safe fix that preserves the current architecture.

The goal is to improve the existing system, not replace it.

---

## 19. Current Priority

The current development priority is the Syllabus creation/import workflow.

The immediate goal is to make this workflow work correctly:

Create new syllabus
→ Import syllabus file
→ Automatically extract data
→ Validate data
→ Preview
→ Save
→ Display/edit the imported syllabus

The user should not have to manually fill every syllabus field when an appropriate import file is available.

Any change related to this workflow must preserve existing syllabus, course, curriculum, program, authentication, and authorization functionality.
