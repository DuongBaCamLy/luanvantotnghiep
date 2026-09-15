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

$serviceTarget = Join-Path $root "src\main\java\com\scse\curriculum\syllabus\importer\service\SyllabusImportServiceImpl.java"
$extractorTarget = Join-Path $root "src\main\java\com\scse\curriculum\syllabus\importer\service\ProgramCurriculumScopeExtractor.java"

$databaseRoot = Resolve-Path (Join-Path $root "..\..\database")
$migrationTarget = Join-Path $databaseRoot "migration\V20260903_04__reconcile_it2026_course_program.sql"
$verifyTarget = Join-Path $databaseRoot "verify_phase1_7_it2026.sql"

if (-not (Test-Path $serviceTarget)) {
    Write-Host "ERROR: SyllabusImportServiceImpl.java not found. Nothing changed." -ForegroundColor Red
    return
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = Join-Path $root "backup_phase1_7_it2026_scope_$timestamp"
New-Item -ItemType Directory -Path $backupDir | Out-Null

Copy-Item $serviceTarget (Join-Path $backupDir "SyllabusImportServiceImpl.java")
if (Test-Path $extractorTarget) {
    Copy-Item $extractorTarget (Join-Path $backupDir "ProgramCurriculumScopeExtractor.java")
}
if (Test-Path $migrationTarget) {
    Copy-Item $migrationTarget (Join-Path $backupDir "V20260903_04__reconcile_it2026_course_program.sql")
}

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Copy-Item (Join-Path $scriptRoot "SyllabusImportServiceImpl.java") $serviceTarget -Force
Copy-Item (Join-Path $scriptRoot "ProgramCurriculumScopeExtractor.java") $extractorTarget -Force
Copy-Item (Join-Path $scriptRoot "V20260903_04__reconcile_it2026_course_program.sql") $migrationTarget -Force
Copy-Item (Join-Path $scriptRoot "verify_phase1_7_it2026.sql") $verifyTarget -Force

Write-Host ""
Write-Host "PHASE 1.7 IT2026 CURRICULUM-SCOPE FIX APPLIED" -ForegroundColor Green
Write-Host "Branch: $branch" -ForegroundColor Cyan
Write-Host "Backup: $backupDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Permanent importer behaviour:" -ForegroundColor Cyan
Write-Host "- Reads official course codes from curriculum course-list tables."
Write-Host "- Detailed syllabus appendices outside that official scope are not imported."
Write-Host "- Fail-open if a supported course-list table cannot be identified."
Write-Host "- CHE011IU -> CH011IU remains supported."
Write-Host "- IT155IU -> IT163IU is recognized as a verified legacy alias."
Write-Host "- If canonical IT163IU is already present in the same document, duplicate IT155IU is excluded."
Write-Host ""
Write-Host "IT2026 data reconciliation:" -ForegroundColor Cyan
Write-Host "- Adds 16 missing official CourseProgram mappings only."
Write-Host "- Does NOT add IT165IU, IT155IU, or IT173IU to IT2026 curriculum."
Write-Host "- Does NOT create/update/delete Course rows."
Write-Host ""
Write-Host "NEXT 1: .\mvnw.cmd clean compile" -ForegroundColor Yellow
Write-Host "NEXT 2: after BUILD SUCCESS run .\run_phase1_7_it2026_docker.ps1" -ForegroundColor Yellow
