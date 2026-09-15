$ErrorActionPreference = "Stop"

$container = "curriculum_mysql"
$db = "curriculum_iu"
$dbUser = "root"
$dbPassword = "root"

$root = Get-Location
$sqlFile = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "V20260903_05__reconcile_it2026_ma033.sql"
$databaseRoot = Resolve-Path (Join-Path $root "..\..\database")
$backupDir = Join-Path $databaseRoot "backups"

if (-not (Test-Path $sqlFile)) {
    Write-Host "ERROR: Phase 1.9 SQL file not found. Nothing changed." -ForegroundColor Red
    return
}

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne "true") {
    Write-Host "ERROR: Docker container '$container' is not running. Nothing changed." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "Read-only MA033 preflight..." -ForegroundColor Cyan

docker exec $container mysql -t "-u$dbUser" "-p$dbPassword" -D $db -e @"
SELECT c.id AS course_id, c.course_code, c.name
FROM course c
WHERE UPPER(REPLACE(REPLACE(TRIM(c.course_code),'-',''),' ','')) IN ('MA033IU','MA033');

SELECT cp.id AS course_program_id, cp.program_id, cp.cohort_id,
       ct.code AS course_type, cp.semester_suggest, cp.year_suggest,
       cp.term_code, cp.is_required, cp.syllabus_id
FROM course_program cp
JOIN course c ON c.id=cp.course_id
LEFT JOIN course_type ct ON ct.id=cp.course_type_id
WHERE UPPER(REPLACE(REPLACE(TRIM(c.course_code),'-',''),' ','')) IN ('MA033IU','MA033')
ORDER BY cp.program_id, cp.cohort_id;
"@

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: preflight failed. Nothing changed." -ForegroundColor Red
    return
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupName = "course_program_before_phase1_9_ma033_$timestamp.sql"
$containerBackup = "/tmp/$backupName"
$localBackup = Join-Path $backupDir $backupName

Write-Host ""
Write-Host "Creating targeted course_program backup..." -ForegroundColor Cyan

docker exec $container sh -c "mysqldump -u$dbUser -p$dbPassword --single-transaction $db course_program > $containerBackup"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: backup failed. Reconciliation NOT executed." -ForegroundColor Red
    return
}

docker cp "${container}:${containerBackup}" "$localBackup"
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $localBackup) -or (Get-Item $localBackup).Length -le 0) {
    Write-Host "ERROR: backup copy failed. Reconciliation NOT executed." -ForegroundColor Red
    return
}

Write-Host "Backup OK: $localBackup" -ForegroundColor Green

Write-Host ""
Write-Host "Reconciling ONLY IT2026 / MA033IU..." -ForegroundColor Cyan

Get-Content -Raw $sqlFile |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" --show-warnings $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Phase 1.9 reconciliation failed. Check MySQL output above." -ForegroundColor Red
    Write-Host "Backup remains at: $localBackup"
    return
}

Write-Host ""
Write-Host "PHASE 1.9 IT2026 MA033 RECONCILIATION COMPLETED" -ForegroundColor Green
Write-Host "Expected final mapping:" -ForegroundColor Cyan
Write-Host "MA033IU | IT2026 | COMPULSORY | Semester 2 | Year 1 | HK2 | required=1"
Write-Host ""
Write-Host "No parser/source-code change is required for this issue." -ForegroundColor Cyan
Write-Host "Backup: $localBackup" -ForegroundColor Cyan
