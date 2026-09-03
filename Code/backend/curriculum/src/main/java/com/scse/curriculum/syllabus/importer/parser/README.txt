MULTI-FORMAT PDF IMPORT - PHASE 1 (SAFE)

Prerequisite:
- Baseline project compiles successfully.
- SyllabusPdfParser.java and PdfProgramDocumentParser.java exist again.

This patch:
1. ADDS SyllabusSectionBoundaryDetector.java.
2. MODIFIES only the PDF batch boundary-detection method in SyllabusPdfParser.java.
3. MODIFIES PdfProgramDocumentParser.java only to report the real PDF page count.
4. KEEPS all existing SyllabusPdfParser field extraction methods unchanged.
5. DOES NOT modify:
   - ProgramDocumentParser.java
   - DocxProgramDocumentParser.java
   - SyllabusDocxParser.java
   - SyllabusImportServiceImpl.java
   - SyllabusServiceImpl.java
   - delete logic
   - confirm/save logic
   - database schema
   - Course/Program/Cohort/CourseProgram/SourceDocument lifecycle

Supported boundary layouts after Phase 1:
- Course Name: ...
  Course Code: ...

- 1. Course Name: ...
  Course Code: ...

- 2) Course Name: ...
  Course Code: ...

There is also a Vietnamese boundary fallback.

SAFE APPLY:
Place this extracted folder's files in the backend curriculum root and run:
  powershell -ExecutionPolicy Bypass -File ".\apply_multiformat_phase1.ps1"

Then:
  .\mvnw.cmd clean compile

Only after BUILD SUCCESS:
  .\mvnw.cmd spring-boot:run

Regression tests:
A. First import the old CS2021 PDF that worked before.
B. Then import the CS2026 PDF.
Expected for CS2026:
- Page count is the real PDF page count (517 for the uploaded document).
- Detected is no longer 0.
- Boundary detector should recognize numbered Course Name / Course Code pages.

If anything fails, restore the two modified files from the backup folder printed by the script and delete only the newly added SyllabusSectionBoundaryDetector.java.
