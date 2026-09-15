$ErrorActionPreference = "Stop"

$root = Get-Location
$branch = (git branch --show-current).Trim()

if ([string]::IsNullOrWhiteSpace($branch)) {
    Write-Host "ERROR: Cannot determine current Git branch. Nothing changed." -ForegroundColor Red
    return
}

if ($branch -eq "main" -or $branch -eq "teacher-review-phase1") {
    Write-Host "ERROR: Protected branch '$branch'. Nothing changed." -ForegroundColor Red
    return
}

$target = Join-Path $root "src\main\java\com\scse\curriculum\syllabus\importer\service\SyllabusImportServiceImpl.java"
$source = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "SyllabusImportServiceImpl.java"

if (-not (Test-Path $target)) {
    Write-Host "ERROR: SyllabusImportServiceImpl.java not found. Nothing changed." -ForegroundColor Red
    return
}

$current = Get-Content -Raw $target

if (-not $current.Contains("ProgramCurriculumScopeExtractor") -or
    -not $current.Contains("deduplicateParsedSyllabiByCourseCode") -or
    -not $current.Contains("resolveImportedCourseInSelectedCurriculum")) {
    Write-Host "ERROR: Current importer is not the expected Phase 1.8 baseline. Nothing changed." -ForegroundColor Red
    return
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = Join-Path $root "backup_phase2_1_official_scope_autoreconcile_$timestamp"
New-Item -ItemType Directory -Path $backupDir | Out-Null
Copy-Item $target (Join-Path $backupDir "SyllabusImportServiceImpl.java")

Copy-Item $source $target -Force

Write-Host ""
Write-Host "PHASE 2.1 OFFICIAL-SCOPE AUTO-RECONCILIATION APPLIED" -ForegroundColor Green
Write-Host "Branch: $branch" -ForegroundColor Cyan
Write-Host "Backup: $backupDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Permanent behavior:" -ForegroundColor Cyan
Write-Host "- Runs only when the PDF official curriculum table was recognized."
Write-Host "- If a syllabus course already exists uniquely in Course Catalog but the exact Program/Cohort mapping is missing, CourseProgram is inserted automatically."
Write-Host "- Existing Course/CourseProgram rows are never updated or deleted."
Write-Host "- New unknown Course codes are NOT auto-created from syllabus text."
Write-Host "- Ambiguous Catalog matches are NOT guessed."
Write-Host "- Elective mappings remain unplaced; compulsory mappings use one unambiguous syllabus semester when available."
Write-Host "- Files are copied as-is; no UTF-8 BOM rewrite is performed."
Write-Host ""
Write-Host "NEXT: .\mvnw.cmd clean compile" -ForegroundColor Yellow
Write-Host "After BUILD SUCCESS restart Spring Boot before importing again." -ForegroundColor Yellow
