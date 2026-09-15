$ErrorActionPreference = "Stop"

$container = "curriculum_mysql"
$db = "curriculum_iu"
$dbUser = "root"
$dbPassword = "root"

$root = Get-Location
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptRoot "cleanup_ds2026_phase21_current_draft.sql"
$databaseRoot = Resolve-Path (Join-Path $root "..\..\database")
$backupDir = Join-Path $databaseRoot "backups"

if (-not (Test-Path $sqlFile)) {
    Write-Host "ERROR: cleanup SQL not found. Nothing changed." -ForegroundColor Red
    return
}

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne "true") {
    Write-Host "ERROR: curriculum_mysql is not running. Nothing changed." -ForegroundColor Red
    return
}

$count = docker exec $container mysql -N -B "-u$dbUser" "-p$dbPassword" -D $db -e @"
SELECT COUNT(DISTINCT s.id)
FROM syllabus s
JOIN course_program cp ON cp.syllabus_id=s.id
WHERE cp.cohort_id=14 AND s.status='DRAFT';
"@

if ($LASTEXITCODE -ne 0 -or [int]($count.Trim()) -ne 45) {
    Write-Host "ERROR: expected exactly 45 linked DS2026 DRAFT rows. Nothing changed." -ForegroundColor Red
    Write-Host "Observed: $count"
    return
}

$approvalCount = docker exec $container mysql -N -B "-u$dbUser" "-p$dbPassword" -D $db -e @"
SELECT COUNT(*)
FROM approval_request ar
JOIN syllabus s ON s.id=ar.syllabus_id
JOIN course_program cp ON cp.syllabus_id=s.id
WHERE cp.cohort_id=14 AND s.status='DRAFT';
"@

if ($LASTEXITCODE -ne 0 -or [int]($approvalCount.Trim()) -ne 0) {
    Write-Host "ERROR: DS2026 test rows have approval workflow data or could not be verified. Nothing changed." -ForegroundColor Red
    return
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupName = "ds2026_before_phase21_clean_reimport_$timestamp.sql"
$containerBackup = "/tmp/$backupName"
$localBackup = Join-Path $backupDir $backupName

Write-Host ""
Write-Host "Creating targeted DS2026 backup..." -ForegroundColor Cyan

docker exec $container sh -c "mysqldump -u$dbUser -p$dbPassword --single-transaction $db syllabus course_program class_section syllabus_import_history syllabus_source_snapshot assessment_component clo syllabus_book topic > $containerBackup"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: backup failed. Cleanup NOT executed." -ForegroundColor Red
    return
}

docker cp "${container}:${containerBackup}" "$localBackup"
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $localBackup) -or (Get-Item $localBackup).Length -le 0) {
    Write-Host "ERROR: backup copy failed. Cleanup NOT executed." -ForegroundColor Red
    return
}

Write-Host "Backup OK: $localBackup" -ForegroundColor Green

Write-Host ""
Write-Host "Removing ONLY the verified 45 DS2026 DRAFT test syllabuses..." -ForegroundColor Cyan

Get-Content -Raw $sqlFile |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" --show-warnings $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: cleanup failed. Check MySQL output above." -ForegroundColor Red
    Write-Host "Backup remains at: $localBackup"
    return
}

Write-Host ""
Write-Host "PHASE 2.1 DS2026 DRAFT CLEANUP COMPLETED" -ForegroundColor Green
Write-Host "CS2021 / CS2026 / IT2026 were not selected by this cleanup." -ForegroundColor Cyan
Write-Host "Backup: $localBackup" -ForegroundColor Cyan
