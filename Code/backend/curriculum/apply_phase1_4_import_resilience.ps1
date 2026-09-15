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

$parserTarget = Join-Path $root "src\main\java\com\scse\curriculum\syllabus\importer\parser\SyllabusPdfParser.java"
$syllabusTarget = Join-Path $root "src\main\java\com\scse\curriculum\syllabus\entity\Syllabus.java"
$bookTarget = Join-Path $root "src\main\java\com\scse\curriculum\book\entity\Book.java"

$databaseRoot = Resolve-Path (Join-Path $root "..\..\database")
$migrationTarget = Join-Path $databaseRoot "migration\V20260903_03__expand_import_text_fields.sql"
$verifyTarget = Join-Path $databaseRoot "verify_phase1_4.sql"

foreach ($required in @($parserTarget, $syllabusTarget, $bookTarget)) {
    if (-not (Test-Path $required)) {
        Write-Host "ERROR: Required source file not found: $required" -ForegroundColor Red
        Write-Host "Nothing changed."
        return
    }
}

$parserCurrent = Get-Content -Raw $parserTarget
if (-not $parserCurrent.Contains("parseVietnameseCourseHeader") -or
    -not $parserCurrent.Contains("parseReadingList")) {
    Write-Host "ERROR: Current PDF parser is not the expected mixed-format parser. Nothing changed." -ForegroundColor Red
    return
}

$syllabusContent = Get-Content -Raw $syllabusTarget
$bookContent = Get-Content -Raw $bookTarget
$nl = [Environment]::NewLine

function Replace-InMemoryRequired {
    param(
        [string]$Content,
        [string]$Pattern,
        [string]$Replacement,
        [string]$Label
    )

    $matches = [regex]::Matches(
        $Content,
        $Pattern,
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )

    if ($matches.Count -ne 1) {
        throw "Expected exactly one match for $Label, found $($matches.Count). Nothing changed."
    }

    return [regex]::Replace(
        $Content,
        $Pattern,
        $Replacement,
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )
}

# Validate and prepare ALL entity changes in memory before writing any source file.
$syllabusUpdated = Replace-InMemoryRequired `
    -Content $syllabusContent `
    -Pattern '@Column\s*\(\s*name\s*=\s*"workload_total"\s*\)\s*private\s+String\s+workloadTotal\s*;' `
    -Replacement ('@Column(name = "workload_total", columnDefinition = "TEXT")' + $nl + '    private String workloadTotal;') `
    -Label 'Syllabus.workloadTotal'

$syllabusUpdated = Replace-InMemoryRequired `
    -Content $syllabusUpdated `
    -Pattern '@Column\s*\(\s*name\s*=\s*"workload_contact"\s*\)\s*private\s+String\s+workloadContact\s*;' `
    -Replacement ('@Column(name = "workload_contact", columnDefinition = "TEXT")' + $nl + '    private String workloadContact;') `
    -Label 'Syllabus.workloadContact'

$syllabusUpdated = Replace-InMemoryRequired `
    -Content $syllabusUpdated `
    -Pattern '@Column\s*\(\s*name\s*=\s*"workload_private"\s*\)\s*private\s+String\s+workloadPrivate\s*;' `
    -Replacement ('@Column(name = "workload_private", columnDefinition = "TEXT")' + $nl + '    private String workloadPrivate;') `
    -Label 'Syllabus.workloadPrivate'

$bookUpdated = Replace-InMemoryRequired `
    -Content $bookContent `
    -Pattern '@Column\s*\(\s*nullable\s*=\s*false\s*,\s*length\s*=\s*500\s*\)\s*private\s+String\s+title\s*;' `
    -Replacement ('@Column(nullable = false, columnDefinition = "TEXT")' + $nl + '    private String title;') `
    -Label 'Book.title'

$bookUpdated = Replace-InMemoryRequired `
    -Content $bookUpdated `
    -Pattern '@Column\s*\(\s*length\s*=\s*500\s*\)\s*private\s+String\s+author\s*;' `
    -Replacement ('@Column(columnDefinition = "TEXT")' + $nl + '    private String author;') `
    -Label 'Book.author'

$bookUpdated = Replace-InMemoryRequired `
    -Content $bookUpdated `
    -Pattern '@Column\s*\(\s*length\s*=\s*255\s*\)\s*private\s+String\s+publisher\s*;' `
    -Replacement ('@Column(columnDefinition = "TEXT")' + $nl + '    private String publisher;') `
    -Label 'Book.publisher'

$bookUpdated = Replace-InMemoryRequired `
    -Content $bookUpdated `
    -Pattern '@Column\s*\(\s*length\s*=\s*1000\s*\)\s*private\s+String\s+url\s*;' `
    -Replacement ('@Column(columnDefinition = "TEXT")' + $nl + '    private String url;') `
    -Label 'Book.url'

# Only now create a backup and write all prepared changes.
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = Join-Path $root "backup_phase1_4_import_resilience_$timestamp"
New-Item -ItemType Directory -Path $backupDir | Out-Null

Copy-Item $parserTarget (Join-Path $backupDir "SyllabusPdfParser.java")
Copy-Item $syllabusTarget (Join-Path $backupDir "Syllabus.java")
Copy-Item $bookTarget (Join-Path $backupDir "Book.java")
if (Test-Path $migrationTarget) {
    Copy-Item $migrationTarget (Join-Path $backupDir "V20260903_03__expand_import_text_fields.sql")
}

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Copy-Item (Join-Path $scriptRoot "SyllabusPdfParser.java") $parserTarget -Force
Set-Content -Path $syllabusTarget -Value $syllabusUpdated -Encoding UTF8
Set-Content -Path $bookTarget -Value $bookUpdated -Encoding UTF8
Copy-Item (Join-Path $scriptRoot "V20260903_03__expand_import_text_fields.sql") $migrationTarget -Force
Copy-Item (Join-Path $scriptRoot "verify_phase1_4.sql") $verifyTarget -Force

Write-Host ""
Write-Host "PHASE 1.4 IMPORT RESILIENCE APPLIED SUCCESSFULLY" -ForegroundColor Green
Write-Host "Branch: $branch" -ForegroundColor Cyan
Write-Host "Backup: $backupDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Parser fixes:" -ForegroundColor Cyan
Write-Host "- Workload fields are parsed only from the semantic workload block."
Write-Host "- Wrapper text 'Workload (incl. contact hours...)' can no longer become workload_contact."
Write-Host "- Empty workload cells become NULL instead of absorbing neighboring labels."
Write-Host "- Reading-list numbering accepts 1., 1), and [1]."
Write-Host "- Four-digit years such as '(amended in 2025)' are never treated as bibliography indexes."
Write-Host "- Trailing access/resource notes are not misclassified as publisher text."
Write-Host ""
Write-Host "Schema/JPA fixes:" -ForegroundColor Cyan
Write-Host "- workload_total/contact/private -> TEXT"
Write-Host "- book title/author/publisher/url -> TEXT"
Write-Host "- No existing DB row was changed by this apply script."
Write-Host ""
Write-Host "NEXT 1: .\mvnw.cmd clean compile" -ForegroundColor Yellow
Write-Host "NEXT 2: after BUILD SUCCESS run .\run_phase1_4_docker.ps1" -ForegroundColor Yellow
