$ErrorActionPreference = "Stop"

$container = "curriculum_mysql"
$db = "curriculum_iu"
$dbUser = "root"
$dbPassword = "root"

$root = Get-Location
$sqlFile = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "cleanup_it2026_phase18_current_test.sql"
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
SELECT COUNT(*)
FROM syllabus
WHERE UPPER(REPLACE(TRIM(academic_year),' ',''))='IT2026'
  AND status='DRAFT';
"@

if ($LASTEXITCODE -ne 0 -or [int]($count.Trim()) -ne 65) {
    Write-Host "ERROR: expected exactly 65 current IT2026 DRAFT rows. Nothing changed." -ForegroundColor Red
    Write-Host "Observed: $count"
    return
}

$approvals = docker exec $container mysql -N -B "-u$dbUser" "-p$dbPassword" -D $db -e @"
SELECT COUNT(*)
FROM approval_request ar
JOIN syllabus s ON s.id=ar.syllabus_id
WHERE UPPER(REPLACE(TRIM(s.academic_year),' ',''))='IT2026'
  AND s.status='DRAFT';
"@

if ($LASTEXITCODE -ne 0 -or [int]($approvals.Trim()) -ne 0) {
    Write-Host "ERROR: IT2026 test rows have approval records or could not be verified. Nothing changed." -ForegroundColor Red
    return
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupName = "it2026_before_phase18_reimport_$timestamp.sql"
$containerBackup = "/tmp/$backupName"
$localBackup = Join-Path $backupDir $backupName

Write-Host ""
Write-Host "Creating targeted backup..." -ForegroundColor Cyan
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
Write-Host "Removing ONLY the current 65 IT2026 DRAFT test rows..." -ForegroundColor Cyan
Get-Content -Raw $sqlFile |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" --show-warnings $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: cleanup failed. Check MySQL error above." -ForegroundColor Red
    Write-Host "Backup remains at: $localBackup"
    return
}

Write-Host ""
Write-Host "PHASE 1.8 IT2026 TEST CLEANUP COMPLETED" -ForegroundColor Green
Write-Host "CS2021 and CS2026 were not selected by this cleanup." -ForegroundColor Cyan
Write-Host "Backup: $localBackup" -ForegroundColor Cyan
