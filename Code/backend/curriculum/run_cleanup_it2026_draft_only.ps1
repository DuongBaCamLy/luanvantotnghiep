$ErrorActionPreference = "Stop"

$container = "curriculum_mysql"
$db = "curriculum_iu"
$dbUser = "root"
$dbPassword = "root"

$root = Get-Location
$sqlFile = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "cleanup_it2026_draft_only.sql"
$databaseRoot = Resolve-Path (Join-Path $root "..\..\database")
$backupDir = Join-Path $databaseRoot "backups"

if (-not (Test-Path $sqlFile)) {
    Write-Host "ERROR: cleanup SQL file not found. Nothing changed." -ForegroundColor Red
    return
}

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne "true") {
    Write-Host "ERROR: Docker container '$container' is not running. Nothing changed." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "Preflight: verifying exact IT2026 test state..." -ForegroundColor Cyan

$draftCount = docker exec $container mysql -N -B "-u$dbUser" "-p$dbPassword" -D $db -e @"
SELECT COUNT(DISTINCT s.id)
FROM syllabus s
JOIN course_program cp ON cp.syllabus_id=s.id
WHERE cp.cohort_id=13 AND s.status='DRAFT';
"@

if ($LASTEXITCODE -ne 0 -or [int]($draftCount.Trim()) -ne 49) {
    Write-Host "ERROR: expected exactly 49 IT2026 DRAFT syllabuses. Nothing changed." -ForegroundColor Red
    Write-Host "Observed: $draftCount"
    return
}

$sharedCount = docker exec $container mysql -N -B "-u$dbUser" "-p$dbPassword" -D $db -e @"
SELECT COUNT(*)
FROM (
  SELECT cp_all.syllabus_id
  FROM course_program cp_it
  JOIN course_program cp_all ON cp_all.syllabus_id=cp_it.syllabus_id
  JOIN syllabus s ON s.id=cp_it.syllabus_id
  WHERE cp_it.cohort_id=13 AND s.status='DRAFT'
  GROUP BY cp_all.syllabus_id
  HAVING COUNT(DISTINCT cp_all.cohort_id)>1
) x;
"@

if ($LASTEXITCODE -ne 0 -or [int]($sharedCount.Trim()) -ne 0) {
    Write-Host "ERROR: an IT2026 test syllabus is shared with another cohort. Nothing changed." -ForegroundColor Red
    return
}

$approvalCount = docker exec $container mysql -N -B "-u$dbUser" "-p$dbPassword" -D $db -e @"
SELECT COUNT(*)
FROM approval_request ar
JOIN syllabus s ON s.id=ar.syllabus_id
JOIN course_program cp ON cp.syllabus_id=s.id
WHERE cp.cohort_id=13 AND s.status='DRAFT';
"@

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: could not inspect approval_request. Nothing changed." -ForegroundColor Red
    return
}

if ([int]($approvalCount.Trim()) -ne 0) {
    Write-Host "ERROR: IT2026 DRAFT data contains approval requests. Cleanup aborted." -ForegroundColor Red
    return
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupName = "it2026_draft_cleanup_before_$timestamp.sql"
$containerBackup = "/tmp/$backupName"
$localBackup = Join-Path $backupDir $backupName

Write-Host ""
Write-Host "Creating targeted backup before cleanup..." -ForegroundColor Cyan

# Back up every table that can be changed/deleted by this scoped cleanup,
# including tables reached by ON DELETE CASCADE.
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
Write-Host "Cleaning ONLY the 49 IT2026 DRAFT test syllabuses..." -ForegroundColor Cyan
Get-Content -Raw $sqlFile |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" --show-warnings $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: cleanup failed. Check the MySQL error above." -ForegroundColor Red
    Write-Host "Backup remains at: $localBackup"
    return
}

Write-Host ""
Write-Host "IT2026 DRAFT-ONLY CLEANUP COMPLETED" -ForegroundColor Green
Write-Host "- CS2021 untouched" -ForegroundColor Cyan
Write-Host "- CS2026 untouched" -ForegroundColor Cyan
Write-Host "- IT2026 CourseProgram curriculum mappings preserved" -ForegroundColor Cyan
Write-Host "- Only the verified 49 IT2026 DRAFT test syllabuses were removed" -ForegroundColor Cyan
Write-Host "Backup: $localBackup" -ForegroundColor Cyan
