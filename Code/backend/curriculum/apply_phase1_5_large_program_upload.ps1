$ErrorActionPreference = "Stop"

$backendRoot = Get-Location
$branch = (git branch --show-current).Trim()

if ([string]::IsNullOrWhiteSpace($branch)) {
    Write-Host "ERROR: Cannot determine current Git branch. Nothing changed." -ForegroundColor Red
    return
}
if ($branch -eq "main" -or $branch -eq "teacher-review-phase1") {
    Write-Host "ERROR: Protected branch '$branch'. Nothing changed." -ForegroundColor Red
    return
}

$service = Join-Path $backendRoot "src\main\java\com\scse\curriculum\syllabus\importer\service\SyllabusImportServiceImpl.java"
$appProps = Join-Path $backendRoot "src\main\resources\application.properties"
$frontendRoot = Resolve-Path (Join-Path $backendRoot "..\..\frontend")
$dialog = Join-Path $frontendRoot "src\components\syllabus\BulkImportSyllabusDialog.tsx"
$api = Join-Path $frontendRoot "src\api\syllabusImportApi.ts"

foreach ($f in @($service, $appProps, $dialog, $api)) {
    if (-not (Test-Path $f)) {
        Write-Host "ERROR: Required file not found: $f" -ForegroundColor Red
        Write-Host "Nothing changed."
        return
    }
}

$serviceText = Get-Content -Raw $service
$propsText = Get-Content -Raw $appProps
$dialogText = Get-Content -Raw $dialog
$apiText = Get-Content -Raw $api

function Replace-One {
    param(
        [string]$Content,
        [string]$Old,
        [string]$New,
        [string]$Label
    )
    $count = ([regex]::Matches($Content, [regex]::Escape($Old))).Count
    if ($count -ne 1) {
        throw "Expected exactly one '$Label' occurrence, found $count. Nothing changed."
    }
    return $Content.Replace($Old, $New)
}

# Prepare all changes in memory first.
$serviceNew = Replace-One `
    $serviceText `
    'file.getSize() > 50L * 1024 * 1024' `
    'file.getSize() > 200L * 1024 * 1024' `
    'backend 50 MB size check'

$serviceNew = Replace-One `
    $serviceNew `
    'The program document exceeds 50 MB.' `
    'The program document exceeds 200 MB.' `
    'backend 50 MB error message'

$propsNew = Replace-One `
    $propsText `
    'spring.servlet.multipart.max-file-size=50MB' `
    'spring.servlet.multipart.max-file-size=200MB' `
    'multipart max-file-size'

$propsNew = Replace-One `
    $propsNew `
    'spring.servlet.multipart.max-request-size=55MB' `
    'spring.servlet.multipart.max-request-size=210MB' `
    'multipart max-request-size'

$propsNew = Replace-One `
    $propsNew `
    'server.tomcat.max-swallow-size=55MB' `
    'server.tomcat.max-swallow-size=210MB' `
    'Tomcat max-swallow-size'

$dialogNew = Replace-One `
    $dialogText `
    'file.size > 50 * 1024 * 1024' `
    'file.size > 200 * 1024 * 1024' `
    'frontend 50 MB size check'

$dialogNew = Replace-One `
    $dialogNew `
    'The PDF exceeds the 50 MB upload limit.' `
    'The PDF exceeds the 200 MB upload limit.' `
    'frontend 50 MB error message'

# Large programme dossiers can take longer to parse than CS2026.
$apiNew = Replace-One `
    $apiText `
    'timeout: 180_000,' `
    'timeout: 600_000,' `
    'bulk preview timeout'

# Only after every validation succeeds, back up and write.
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = Join-Path $backendRoot "backup_phase1_5_large_upload_$timestamp"
New-Item -ItemType Directory -Path $backupDir | Out-Null

Copy-Item $service (Join-Path $backupDir "SyllabusImportServiceImpl.java")
Copy-Item $appProps (Join-Path $backupDir "application.properties")
Copy-Item $dialog (Join-Path $backupDir "BulkImportSyllabusDialog.tsx")
Copy-Item $api (Join-Path $backupDir "syllabusImportApi.ts")

# Write UTF-8 WITHOUT BOM so javac/TS tooling are unaffected.
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($service, $serviceNew, $utf8NoBom)
[System.IO.File]::WriteAllText($appProps, $propsNew, $utf8NoBom)
[System.IO.File]::WriteAllText($dialog, $dialogNew, $utf8NoBom)
[System.IO.File]::WriteAllText($api, $apiNew, $utf8NoBom)

Write-Host ""
Write-Host "PHASE 1.5 LARGE PROGRAM DOCUMENT UPLOAD APPLIED" -ForegroundColor Green
Write-Host "Branch: $branch" -ForegroundColor Cyan
Write-Host "Backup: $backupDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Changed:" -ForegroundColor Cyan
Write-Host "- Frontend PDF limit: 50 MB -> 200 MB"
Write-Host "- Backend service limit: 50 MB -> 200 MB"
Write-Host "- Spring multipart file limit: 50 MB -> 200 MB"
Write-Host "- Request/Tomcat swallow limit: 55 MB -> 210 MB"
Write-Host "- Bulk preview timeout: 180 sec -> 600 sec"
Write-Host "- Files written UTF-8 without BOM"
Write-Host ""
Write-Host "No database data was changed." -ForegroundColor Cyan
Write-Host ""
Write-Host "NEXT 1 (backend): .\mvnw.cmd clean compile" -ForegroundColor Yellow
Write-Host "NEXT 2 (frontend): cd ..\..\frontend ; npm run build" -ForegroundColor Yellow
Write-Host "NEXT 3: restart BOTH backend and frontend dev servers." -ForegroundColor Yellow
