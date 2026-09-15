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

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = Join-Path $root "backup_phase1_3d_che011_alias_$timestamp"
New-Item -ItemType Directory -Path $backupDir | Out-Null
Copy-Item $target (Join-Path $backupDir "SyllabusImportServiceImpl.java")

Copy-Item $source $target -Force

Write-Host ""
Write-Host "PHASE 1.3D CHE011 ALIAS APPLIED SUCCESSFULLY" -ForegroundColor Green
Write-Host "Branch: $branch" -ForegroundColor Cyan
Write-Host "Backup: $backupDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Change:" -ForegroundColor Cyan
Write-Host "- Verified official alias CHE011IU -> canonical CH011IU is handled directly in course-code equivalence."
Write-Host "- This applies consistently to bulk resolution, confirm validation, and source-snapshot validation."
Write-Host "- Existing PE008IU/PE008WE alternate-code support remains unchanged."
Write-Host "- No database file or data was changed."
Write-Host ""
Write-Host "NEXT: .\mvnw.cmd clean compile" -ForegroundColor Yellow
Write-Host "Then STOP the currently running backend and start it again with .\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
