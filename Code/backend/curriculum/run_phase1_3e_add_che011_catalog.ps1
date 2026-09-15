$ErrorActionPreference = "Stop"

$container = "curriculum_mysql"
$db = "curriculum_iu"
$dbUser = "root"
$dbPassword = "root"

$root = Get-Location
$sqlFile = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "add_che011iu_catalog_alias.sql"

if (-not (Test-Path $sqlFile)) {
    Write-Host "ERROR: SQL file not found. Nothing changed." -ForegroundColor Red
    return
}

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne "true") {
    Write-Host "ERROR: Docker container '$container' is not running. Nothing changed." -ForegroundColor Red
    return
}

$chCount = docker exec $container mysql -N -B "-u$dbUser" "-p$dbPassword" -D $db -e "SELECT COUNT(*) FROM course WHERE UPPER(TRIM(course_code))='CH011IU';"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Cannot inspect CH011IU. Nothing changed." -ForegroundColor Red
    return
}

if ([int]$chCount -ne 1) {
    Write-Host "ERROR: Expected exactly one canonical CH011IU course, found $chCount. Nothing changed." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "Canonical CH011IU found. Applying INSERT-only CHE011IU catalog alias..." -ForegroundColor Cyan

Get-Content -Raw $sqlFile |
docker exec -i $container mysql "-u$dbUser" "-p$dbPassword" --show-warnings $db

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Phase 1.3E SQL failed. Check the error above." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "PHASE 1.3E CHE011IU CATALOG CODE COMPLETED" -ForegroundColor Green
Write-Host "- CH011IU remains the official CS2021 curriculum course." -ForegroundColor Cyan
Write-Host "- CHE011IU now exists as a separate Course Catalog code/name." -ForegroundColor Cyan
Write-Host "- CHE011IU is recorded as EQUIVALENT to CH011IU." -ForegroundColor Cyan
Write-Host "- No duplicate CourseProgram row was created." -ForegroundColor Cyan
Write-Host "- No existing Course/CourseProgram/Syllabus row was updated or deleted." -ForegroundColor Cyan
