$ErrorActionPreference = "Stop"

$container = "curriculum_mysql"
$db = "curriculum_iu"
$dbUser = "root"
$dbPassword = "root"

$root = Get-Location
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptRoot "V20260903_07__reconcile_ds2026_it177.sql"
$databaseRoot = Resolve-Path (Join-Path $root "..\..\database")
$backupDir = Join-Path $databaseRoot "backups"

if (-not (Test-Path $sqlFile)) {
    Write-Host "ERROR: Phase 2.1 IT177 SQL file not found. Nothing changed." -ForegroundColor Red
    return
}

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne "true") {
    Write-Host "ERROR: curriculum_mysql is not running. Nothing changed." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "Read-only IT177 / DS2026 preflight..." -ForegroundColor Cyan

docker exec $container mysql -t "-u$dbUser" "-p$dbPassword" -D $db -e @"
SELECT c.id AS course_id, c.course_code, c.name, c.credit_theory, c.credit_lab
FROM course c
WHERE UPPER(REPLACE(REPLACE(TRIM(c.course_code),'-',''),' ','')) IN ('IT177IU','IT177');

SELECT cp.id AS course_program_id, cp.program_id, cp.cohort_id,
       ct.code AS course_type, cp.semester_suggest, cp.year_suggest,
       cp.term_code, cp.is_required, cp.syllabus_id
FROM course_program cp
JOIN course c ON c.id=cp.course_id
LEFT JOIN course_type ct ON ct.id=cp.course_type_id
WHERE UPPER(REPLACE(REPLACE(TRIM(c.course_code),'-',''),' ','')) IN ('IT177IU','IT177')
ORDER BY cp.program_id, cp.cohort_id;
"@

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: preflight failed. Nothing changed." -ForegroundColor Red
    return
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupName = "course_program_before_phase2_1_ds2026_it177_$timestamp.sql"
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
Write-Host "Reconciling ONLY IT177IU for DS2026..." -ForegroundColor Cyan

Get-Content -Raw $sqlFile |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" --show-warnings $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Phase 2.1 IT177 reconciliation failed." -ForegroundColor Red
    Write-Host "Backup remains at: $localBackup"
    return
}

Write-Host ""
Write-Host "PHASE 2.1 DS2026 IT177 RECONCILIATION COMPLETED" -ForegroundColor Green
Write-Host "Expected: IT177IU | DS2026 | COMPULSORY | Semester 7 | Year 4 | HK7 | required=1" -ForegroundColor Cyan
Write-Host "Backup: $localBackup" -ForegroundColor Cyan
