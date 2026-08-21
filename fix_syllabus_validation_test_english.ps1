param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"

$testFile = Join-Path $ProjectRoot `
    "Code\backend\curriculum\src\test\java\com\scse\curriculum\syllabus\service\SyllabusSubmissionValidationServiceTest.java"

if (-not (Test-Path $testFile)) {
    throw "Không tìm thấy file test: $testFile"
}

$utf8 = [System.Text.UTF8Encoding]::new($false)
$content = [System.IO.File]::ReadAllText($testFile, $utf8)

$replacements = @(
    @('"Thông tin chung"', '"General Information"'),
    @('"Chuẩn đầu ra (CLO)"', '"Course Learning Outcomes (CLO)"'),
    @('"Nội dung giảng dạy"', '"Teaching Content"'),
    @('"Phương pháp đánh giá"', '"Assessment Plan"'),
    @('"Tài liệu tham khảo"', '"Reading List"')
)

$changed = 0

foreach ($pair in $replacements) {
    $old = $pair[0]
    $new = $pair[1]

    if ($content.Contains($old)) {
        $content = $content.Replace($old, $new)
        $changed++
        Write-Host "UPDATED: $old -> $new" -ForegroundColor Green
    }
    else {
        Write-Host "SKIP: không còn $old" -ForegroundColor Yellow
    }
}

if ($changed -eq 0) {
    Write-Host ""
    Write-Host "Không có chuỗi tiếng Việt nào cần đổi. Có thể test đã được sửa." -ForegroundColor Yellow
}
else {
    [System.IO.File]::WriteAllText($testFile, $content, $utf8)
    Write-Host ""
    Write-Host "Đã cập nhật $changed expectation trong test." -ForegroundColor Cyan
}

Write-Host ""
Write-Host "File:" $testFile
Write-Host "Sau đó chạy:" -ForegroundColor Cyan
Write-Host 'cd ".\Code\backend\curriculum"'
Write-Host '.\mvnw.cmd clean test'
