$ErrorActionPreference = "Stop"

$container = "curriculum_mysql"
$db = "curriculum_iu"
$dbUser = "root"
$dbPassword = "root"

$root = Get-Location
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptRoot "V20260903_06__reconcile_ds2026_course_catalog.sql"
$verifyFile = Join-Path $scriptRoot "verify_phase2_0_ds2026.sql"

$databaseRoot = Resolve-Path (Join-Path $root "..\..\database")
$backupDir = Join-Path $databaseRoot "backups"

if (-not (Test-Path $sqlFile) -or -not (Test-Path $verifyFile)) {
    Write-Host "ERROR: Phase 2.0 SQL files not found. Nothing changed." -ForegroundColor Red
    return
}

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne "true") {
    Write-Host "ERROR: Docker container '$container' is not running. Nothing changed." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "PHASE 2.0 DS2026 - READ-ONLY PREFLIGHT" -ForegroundColor Cyan

docker exec $container mysql -t "-u$dbUser" "-p$dbPassword" -D $db -e @"
SELECT id, code, name
FROM program
WHERE id = (SELECT program_id FROM cohort WHERE UPPER(REPLACE(TRIM(name),' ',''))='DS2026' LIMIT 1);

SELECT id, name, program_id
FROM cohort
WHERE UPPER(REPLACE(TRIM(name),' ',''))='DS2026';

SELECT c.id, c.course_code, c.name, c.credit_theory, c.credit_lab
FROM course c
WHERE UPPER(REPLACE(REPLACE(TRIM(c.course_code),'-',''),' ','')) IN (
  'PE021IU','PE021','IT178IU','IT178','IT176IU','IT176',
  'IT172IU','IT172','IT173IU','IT173','IT169IU','IT169',
  'IT076IU','IT076','IT170IU','IT170','IT093IU','IT093',
  'IT164IU','IT164','IT153IU','IT153'
)
ORDER BY c.course_code;
"@

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: preflight failed. Nothing changed." -ForegroundColor Red
    return
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupName = "course_and_course_program_before_phase2_0_ds2026_$timestamp.sql"
$containerBackup = "/tmp/$backupName"
$localBackup = Join-Path $backupDir $backupName

Write-Host ""
Write-Host "Creating targeted Course + CourseProgram backup..." -ForegroundColor Cyan

docker exec $container sh -c "mysqldump -u$dbUser -p$dbPassword --single-transaction $db course course_program > $containerBackup"
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
Write-Host "Applying DS2026 official curriculum reconciliation..." -ForegroundColor Cyan

Get-Content -Raw $sqlFile |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" --show-warnings $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Phase 2.0 reconciliation failed. Check MySQL output above." -ForegroundColor Red
    Write-Host "Backup remains at: $localBackup"
    return
}

Write-Host ""
Write-Host "Verifying..." -ForegroundColor Cyan

Get-Content -Raw $verifyFile |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" -t $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: verification failed." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "PHASE 2.0 DS2026 RECONCILIATION COMPLETED" -ForegroundColor Green
Write-Host "Expected: reconciled_ds2026_mapping_count = 11" -ForegroundColor Cyan
Write-Host ""
Write-Host "No existing Course/CourseProgram/Syllabus row was updated or deleted." -ForegroundColor Cyan
Write-Host "Backup: $localBackup" -ForegroundColor Cyan
