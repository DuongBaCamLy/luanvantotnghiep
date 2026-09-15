param(
    [switch]$ApplySrsTaxonomy
)

$ErrorActionPreference = "Stop"

$container = "curriculum_mysql"
$db = "curriculum_iu"
$dbUser = "root"
$dbPassword = "root"

# IMPORTANT: mysql client flags below are quoted because PowerShell does not
# interpolate variables correctly when they are concatenated into bare tokens.

$backendRoot = Get-Location
$databaseRoot = Resolve-Path (Join-Path $backendRoot "..\..\database")
$reconcileSql = Join-Path $databaseRoot "migration\V20260903_02__reconcile_cs2026_course_catalog.sql"
$verifySql = Join-Path $databaseRoot "verify_phase1_3.sql"
$taxonomySql = Join-Path $databaseRoot "migrate_course_type_to_srs_3_groups.sql"
$backupDir = Join-Path $databaseRoot "backups"

if (-not (Test-Path $reconcileSql) -or -not (Test-Path $verifySql)) {
    Write-Host "ERROR: Phase 1.3C SQL files are missing. Nothing changed." -ForegroundColor Red
    return
}

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne "true") {
    Write-Host "ERROR: Docker container '$container' is not running." -ForegroundColor Red
    Write-Host "Start the MySQL Docker service first. Nothing changed."
    return
}

Write-Host ""
Write-Host "DOCKER MYSQL FOUND" -ForegroundColor Green
docker ps --filter "name=$container" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Backup before any DB mutation.
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupName = "curriculum_iu_before_phase1_3c_$timestamp.sql"
$containerBackup = "/tmp/$backupName"
$localBackup = Join-Path $backupDir $backupName

Write-Host ""
Write-Host "Creating database backup..." -ForegroundColor Cyan
docker exec $container sh -c "mysqldump -u$dbUser -p$dbPassword --single-transaction --routines --triggers $db > $containerBackup"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: mysqldump failed. No migration executed." -ForegroundColor Red
    return
}
docker cp "${container}:${containerBackup}" "$localBackup"
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $localBackup) -or (Get-Item $localBackup).Length -le 0) {
    Write-Host "ERROR: Backup copy failed. No migration executed." -ForegroundColor Red
    return
}
Write-Host "Backup OK: $localBackup" -ForegroundColor Green

Write-Host ""
Write-Host "Current course_type taxonomy:" -ForegroundColor Cyan
docker exec $container mysql "-u$dbUser" "-p$dbPassword" -D $db -t -e "SELECT id, code, name FROM course_type ORDER BY id;"

$taxonomyCodes = docker exec $container mysql -N -B -u$dbUser -p$dbPassword -D $db -e "SELECT UPPER(TRIM(code)) FROM course_type ORDER BY id;"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Cannot read course_type. No migration executed." -ForegroundColor Red
    return
}

$codes = @($taxonomyCodes | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$hasActive = ($codes -contains "GENERAL") -and ($codes -contains "COMPULSORY") -and ($codes -contains "ELECTIVE")
$hasLegacy = @($codes | Where-Object { $_ -in @("FOUNDATION","CORE","THESIS","INTERNSHIP") }).Count -gt 0

if (-not $hasActive -or $hasLegacy) {
    if (-not $ApplySrsTaxonomy) {
        Write-Host ""
        Write-Host "STOPPED SAFELY: database taxonomy is not yet the active SRS 3-group taxonomy." -ForegroundColor Yellow
        Write-Host "Nothing after the backup was changed."
        Write-Host ""
        Write-Host "To apply the project's existing taxonomy migration and then continue, rerun:" -ForegroundColor Yellow
        Write-Host 'powershell -ExecutionPolicy Bypass -File ".\run_phase1_3c_docker.ps1" -ApplySrsTaxonomy'
        return
    }

    if (-not (Test-Path $taxonomySql)) {
        Write-Host "ERROR: migrate_course_type_to_srs_3_groups.sql not found. Reconciliation stopped." -ForegroundColor Red
        return
    }

    Write-Host ""
    Write-Host "Applying existing SRS course_type migration..." -ForegroundColor Cyan
    Get-Content -Raw $taxonomySql | docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" --show-warnings $db
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: SRS taxonomy migration failed. Reconciliation NOT executed." -ForegroundColor Red
        return
    }

    Write-Host "SRS taxonomy migration completed." -ForegroundColor Green
}

Write-Host ""
Write-Host "Running Phase 1.3C CS2026 reconciliation..." -ForegroundColor Cyan
Get-Content -Raw $reconcileSql | docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" --show-warnings $db
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Phase 1.3C migration failed. Check the error above." -ForegroundColor Red
    Write-Host "Backup remains at: $localBackup"
    return
}

Write-Host ""
Write-Host "Running verification..." -ForegroundColor Cyan
Get-Content -Raw $verifySql | docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" -t $db
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Verification query failed." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "PHASE 1.3C DATABASE RECONCILIATION COMPLETED" -ForegroundColor Green
Write-Host "Expected verification:" -ForegroundColor Cyan
Write-Host "- reconciled_cs2026_mapping_count = 12"
Write-Host "- active_count = 3"
Write-Host "- legacy_count = 0"
Write-Host "- duplicate query returns zero rows"
Write-Host "- Chemistry uses canonical CH011IU/CH011; no CHE011IU course is created"
Write-Host ""
Write-Host "Backup: $localBackup" -ForegroundColor Cyan
