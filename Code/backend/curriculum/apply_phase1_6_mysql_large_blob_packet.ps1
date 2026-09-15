$ErrorActionPreference = "Stop"

$container = "curriculum_mysql"
$db = "curriculum_iu"
$dbUser = "root"
$dbPassword = "root"
$targetBytes = 268435456  # 256 MiB

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne "true") {
    Write-Host "ERROR: Docker container '$container' is not running. Nothing changed." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "PHASE 1.6 - LARGE PROGRAM DOCUMENT DATABASE TRANSPORT" -ForegroundColor Cyan
Write-Host ""

Write-Host "Current MySQL packet limit:" -ForegroundColor Cyan
$current = docker exec $container mysql -N -B "-u$dbUser" "-p$dbPassword" -D $db -e "SELECT @@GLOBAL.max_allowed_packet;"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Cannot read max_allowed_packet. Nothing changed." -ForegroundColor Red
    return
}

$currentBytes = [int64]($current.Trim())
$currentMiB = [math]::Round($currentBytes / 1MB, 2)
Write-Host "  $currentBytes bytes ($currentMiB MiB)"

Write-Host ""
Write-Host "Checking source_document binary column:" -ForegroundColor Cyan
docker exec $container mysql -t "-u$dbUser" "-p$dbPassword" -D $db -e @"
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'source_document'
  AND column_name = 'content';
"@
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Cannot inspect source_document.content. Nothing changed." -ForegroundColor Red
    return
}

if ($currentBytes -ge $targetBytes) {
    Write-Host ""
    Write-Host "max_allowed_packet is already >= 256 MiB. No setting change needed." -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "Persisting max_allowed_packet = 256 MiB..." -ForegroundColor Cyan

    docker exec $container mysql "-u$dbUser" "-p$dbPassword" -D $db -e "SET PERSIST max_allowed_packet = $targetBytes;"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: SET PERSIST failed. Nothing else was changed." -ForegroundColor Red
        return
    }
}

Write-Host ""
Write-Host "Verification:" -ForegroundColor Cyan
docker exec $container mysql -t "-u$dbUser" "-p$dbPassword" -D $db -e @"
SELECT
    @@GLOBAL.max_allowed_packet AS max_allowed_packet_bytes,
    ROUND(@@GLOBAL.max_allowed_packet / 1024 / 1024, 2) AS max_allowed_packet_mib;
"@
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Verification failed." -ForegroundColor Red
    return
}

$verified = docker exec $container mysql -N -B "-u$dbUser" "-p$dbPassword" -D $db -e "SELECT @@GLOBAL.max_allowed_packet;"
if ($LASTEXITCODE -ne 0 -or [int64]($verified.Trim()) -lt $targetBytes) {
    Write-Host "ERROR: max_allowed_packet did not reach 256 MiB." -ForegroundColor Red
    return
}

Write-Host ""
Write-Host "PHASE 1.6 MYSQL LARGE-BLOB PACKET FIX COMPLETED" -ForegroundColor Green
Write-Host ""
Write-Host "Why this is needed:" -ForegroundColor Cyan
Write-Host "- IT2026 PDF is about 81.24 MiB."
Write-Host "- The importer stores the original program document bytes in source_document.content."
Write-Host "- A MySQL packet limit below the file size can reject that INSERT after parsing."
Write-Host "- 256 MiB safely covers the application's 200 MiB program-document limit."
Write-Host ""
Write-Host "IMPORTANT NEXT:" -ForegroundColor Yellow
Write-Host "1. Stop the currently running Spring Boot backend (Ctrl+C)."
Write-Host "2. Start it again: .\mvnw.cmd spring-boot:run"
Write-Host "3. Refresh the browser and Extract IT2026 again."
Write-Host ""
Write-Host "No syllabus/course/program data was modified." -ForegroundColor Cyan
