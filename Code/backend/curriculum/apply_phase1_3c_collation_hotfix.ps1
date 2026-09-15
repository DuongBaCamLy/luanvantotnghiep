$ErrorActionPreference = "Stop"

$root = Get-Location
$databaseRoot = Resolve-Path (Join-Path $root "..\..\database")
$target = Join-Path $databaseRoot "migration\V20260903_02__reconcile_cs2026_course_catalog.sql"
$source = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "V20260903_02__reconcile_cs2026_course_catalog.sql"

if (-not (Test-Path $target)) {
    Write-Host "ERROR: target migration not found. Nothing changed." -ForegroundColor Red
    return
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backup = "$target.backup_collation_$timestamp"
Copy-Item $target $backup

Copy-Item $source $target -Force

Write-Host ""
Write-Host "PHASE 1.3C COLLATION HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Backup: $backup" -ForegroundColor Cyan
Write-Host ""
Write-Host "Fixes:" -ForegroundColor Cyan
Write-Host "- Temp reconciliation table now uses utf8mb4_unicode_ci."
Write-Host "- Temp table uses InnoDB instead of MEMORY."
Write-Host "- Course-code comparisons use explicit utf8mb4_unicode_ci."
Write-Host "- No Java source was changed."
Write-Host ""
Write-Host "NEXT: rerun the reconciliation SQL through Docker." -ForegroundColor Yellow
