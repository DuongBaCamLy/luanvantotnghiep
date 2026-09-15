$ErrorActionPreference = "Stop"

$container = "curriculum_mysql"
$db = "curriculum_iu"
$dbUser = "root"
$dbPassword = "root"

$backendRoot = Get-Location
$databaseRoot = Resolve-Path (Join-Path $backendRoot "..\..\database")
$migrationSql = Join-Path $databaseRoot "migration\V20260903_03__expand_import_text_fields.sql"
$verifySql = Join-Path $databaseRoot "verify_phase1_4.sql"
$backupDir = Join-Path $databaseRoot "backups"

if (-not (Test-Path $migrationSql) -or -not (Test-Path $verifySql)) {
    Write-Host "ERROR: Phase 1.4 SQL files are missing. Nothing changed." -ForegroundColor Red
    return
}

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne "true") {
    Write-Host "ERROR: Docker container '$container' is not running. Nothing changed." -ForegroundColor Red
    return
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupName = "syllabus_book_before_phase1_4_$timestamp.sql"
$containerBackup = "/tmp/$backupName"
$localBackup = Join-Path $backupDir $backupName

Write-Host ""
Write-Host "Creating targeted syllabus/book backup..." -ForegroundColor Cyan
docker exec $container sh -c "mysqldump -u$dbUser -p$dbPassword --single-transaction $db syllabus book > $containerBackup"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: targeted backup failed. Migration NOT executed." -ForegroundColor Red
    return
}

docker cp "${container}:${containerBackup}" "$localBackup"
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $localBackup) -or (Get-Item $localBackup).Length -le 0) {
    Write-Host "ERROR: backup copy failed. Migration NOT executed." -ForegroundColor Red
    return
}
Write-Host "Backup OK: $localBackup" -ForegroundColor Green

Write-Host ""
Write-Host "Applying Phase 1.4 text-column migration..." -ForegroundColor Cyan
Get-Content -Raw $migrationSql |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" --show-warnings $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Phase 1.4 migration failed. Check the error above." -ForegroundColor Red
    Write-Host "Backup remains at: $localBackup"
    return
}

Write-Host ""
Write-Host "Verifying column types..." -ForegroundColor Cyan
Get-Content -Raw $verifySql |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" -t $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: verification failed." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "PHASE 1.4 DATABASE MIGRATION COMPLETED" -ForegroundColor Green
Write-Host "Expected: all 7 listed columns have column_type = text." -ForegroundColor Cyan
Write-Host "Backup: $localBackup" -ForegroundColor Cyan
