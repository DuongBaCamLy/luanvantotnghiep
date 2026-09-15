$ErrorActionPreference = "Stop"

$container = "curriculum_mysql"
$db = "curriculum_iu"
$dbUser = "root"
$dbPassword = "root"

$backendRoot = Get-Location
$databaseRoot = Resolve-Path (Join-Path $backendRoot "..\..\database")
$migrationSql = Join-Path $databaseRoot "migration\V20260903_04__reconcile_it2026_course_program.sql"
$verifySql = Join-Path $databaseRoot "verify_phase1_7_it2026.sql"
$backupDir = Join-Path $databaseRoot "backups"

if (-not (Test-Path $migrationSql) -or -not (Test-Path $verifySql)) {
    Write-Host "ERROR: Phase 1.7 SQL files are missing. Nothing changed." -ForegroundColor Red
    return
}

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne "true") {
    Write-Host "ERROR: Docker container '$container' is not running. Nothing changed." -ForegroundColor Red
    return
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupName = "course_program_before_phase1_7_it2026_$timestamp.sql"
$containerBackup = "/tmp/$backupName"
$localBackup = Join-Path $backupDir $backupName

Write-Host ""
Write-Host "Creating targeted course_program backup..." -ForegroundColor Cyan
docker exec $container sh -c "mysqldump -u$dbUser -p$dbPassword --single-transaction $db course_program > $containerBackup"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: backup failed. Migration NOT executed." -ForegroundColor Red
    return
}

docker cp "${container}:${containerBackup}" "$localBackup"
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $localBackup) -or (Get-Item $localBackup).Length -le 0) {
    Write-Host "ERROR: backup copy failed. Migration NOT executed." -ForegroundColor Red
    return
}
Write-Host "Backup OK: $localBackup" -ForegroundColor Green

Write-Host ""
Write-Host "Applying IT2026 official CourseProgram reconciliation..." -ForegroundColor Cyan
Get-Content -Raw $migrationSql |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" --show-warnings $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Phase 1.7 migration failed. Check the error above." -ForegroundColor Red
    Write-Host "Backup remains at: $localBackup"
    return
}

Write-Host ""
Write-Host "Verifying..." -ForegroundColor Cyan
Get-Content -Raw $verifySql |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" -t $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: verification failed." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "PHASE 1.7 IT2026 DATABASE RECONCILIATION COMPLETED" -ForegroundColor Green
Write-Host "Expected reconciled_official_mapping_count = 16" -ForegroundColor Cyan
Write-Host "IT165/IT155/IT173 are intentionally not added to IT2026 curriculum." -ForegroundColor Cyan
Write-Host "Backup: $localBackup" -ForegroundColor Cyan
