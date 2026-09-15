$ErrorActionPreference = "Stop"

$root = Get-Location
$branch = (git branch --show-current).Trim()

if ([string]::IsNullOrWhiteSpace($branch)) {
    Write-Host "ERROR: Cannot determine current Git branch. Nothing changed." -ForegroundColor Red
    return
}

if ($branch -eq "main" -or $branch -eq "teacher-review-phase1") {
    Write-Host "ERROR: Protected branch '$branch'. Nothing changed." -ForegroundColor Red
    return
}

$targets = @(
    (Join-Path $root "src\main\java\com\scse\curriculum\book\entity\Book.java"),
    (Join-Path $root "src\main\java\com\scse\curriculum\syllabus\entity\Syllabus.java")
)

foreach ($target in $targets) {
    if (-not (Test-Path $target)) {
        Write-Host "ERROR: Missing file: $target. Nothing changed." -ForegroundColor Red
        return
    }
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = Join-Path $root "backup_phase1_4_utf8_bom_$timestamp"
New-Item -ItemType Directory -Path $backupDir | Out-Null

foreach ($target in $targets) {
    Copy-Item $target (Join-Path $backupDir ([System.IO.Path]::GetFileName($target)))
}

$changed = 0

foreach ($target in $targets) {
    [byte[]]$bytes = [System.IO.File]::ReadAllBytes($target)

    $hasBom = $bytes.Length -ge 3 `
        -and $bytes[0] -eq 0xEF `
        -and $bytes[1] -eq 0xBB `
        -and $bytes[2] -eq 0xBF

    if ($hasBom) {
        $newBytes = New-Object byte[] ($bytes.Length - 3)
        [System.Array]::Copy($bytes, 3, $newBytes, 0, $bytes.Length - 3)
        [System.IO.File]::WriteAllBytes($target, $newBytes)
        Write-Host "Removed UTF-8 BOM: $target" -ForegroundColor Green
        $changed++
    }
    else {
        Write-Host "No UTF-8 BOM found: $target" -ForegroundColor Cyan
    }
}

Write-Host ""
Write-Host "PHASE 1.4 UTF-8 BOM HOTFIX COMPLETED" -ForegroundColor Green
Write-Host "Branch: $branch" -ForegroundColor Cyan
Write-Host "Backup: $backupDir" -ForegroundColor Cyan
Write-Host "Files changed: $changed" -ForegroundColor Cyan
Write-Host ""
Write-Host "No Java logic was changed." -ForegroundColor Cyan
Write-Host "No database data was changed." -ForegroundColor Cyan
Write-Host ""
Write-Host "NEXT: .\mvnw.cmd clean compile" -ForegroundColor Yellow
