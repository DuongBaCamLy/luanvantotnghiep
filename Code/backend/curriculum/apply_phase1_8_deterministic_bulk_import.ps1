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
    -not $current.Contains("IT155") -or
    -not $current.Contains("courseCodesEquivalent")) {
    Write-Host "ERROR: Current importer is not the expected Phase 1.7 baseline. Nothing changed." -ForegroundColor Red
    return
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = Join-Path $root "backup_phase1_8_deterministic_bulk_$timestamp"
New-Item -ItemType Directory -Path $backupDir | Out-Null
Copy-Item $target (Join-Path $backupDir "SyllabusImportServiceImpl.java")

Copy-Item $source $target -Force

Write-Host ""
Write-Host "PHASE 1.8 DETERMINISTIC BULK IMPORT APPLIED" -ForegroundColor Green
Write-Host "Branch: $branch" -ForegroundColor Cyan
Write-Host "Backup: $backupDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Fixes:" -ForegroundColor Cyan
Write-Host "- Non-instructor imports resolve Course from the selected Program/Cohort itself."
Write-Host "- Imported code and final persisted Course can no longer diverge."
Write-Host "- Duplicate detailed syllabus sections with the same canonical code are deduplicated."
Write-Host "- Cleaner duplicate wins; ties keep the first source occurrence."
Write-Host "- Instructor/ClassSection authorization behavior is unchanged."
Write-Host "- No database data was changed by this apply script."
Write-Host ""
Write-Host "NEXT: .\mvnw.cmd clean compile" -ForegroundColor Yellow
Write-Host "After BUILD SUCCESS restart Spring Boot before importing again." -ForegroundColor Yellow
