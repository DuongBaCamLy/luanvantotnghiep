$ErrorActionPreference = 'Stop'
$projectRoot = 'E:\Luận Văn Tốt Nghiệp\Code'
$frontRoot = Join-Path $projectRoot 'frontend\src'
$backRoot = Join-Path $projectRoot 'backend\curriculum\src\main\java\com\scse\curriculum'
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$backupRoot = Join-Path $projectRoot ("_language_backup_batch4_" + $stamp)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Update-TextFile {
  param([string]$Root,[string]$RelativePath,[array]$Pairs)
  $path = Join-Path $Root $RelativePath
  if (-not (Test-Path $path)) { Write-Host "SKIP: $RelativePath" -ForegroundColor Yellow; return }
  $backupPath = Join-Path $backupRoot $RelativePath
  $backupDir = Split-Path $backupPath -Parent
  New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
  Copy-Item $path $backupPath -Force
  $content = [System.IO.File]::ReadAllText($path)
  $count = 0
  foreach ($pair in $Pairs) {
    $old = [string]$pair[0]; $new = [string]$pair[1]
    if ($content.Contains($old)) { $content = $content.Replace($old,$new); $count++ }
  }
  [System.IO.File]::WriteAllText($path,$content,$utf8NoBom)
  Write-Host ("UPDATED: $RelativePath ($count replacement patterns)") -ForegroundColor Green
}

New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
Write-Host "Backup: $backupRoot" -ForegroundColor Cyan

Update-TextFile $frontRoot 'components\syllabus\CloneSyllabusDialog.tsx' @(
  @('Clone từ ${source.versionLabel} (${source.academicYear} - ${source.semester ?? ""})','Clone from ${source.versionLabel} (${source.academicYear} - ${source.semester ?? ""})'),
  @('Vui lòng chọn phân công của học kỳ đích.','Please select the target-semester teaching assignment.'),
  @('Vui lòng nhập năm học và học kỳ đích.','Please enter the target academic year and semester.'),
  @('Không thể nhân bản đề cương.','Unable to clone the syllabus.'),
  @('Nhân bản đề cương sang học kỳ mới','Clone Syllabus to a New Semester'),
  @('Toàn bộ General Info, CLO, CLO–PLO, Topics, Topic–CLO,','All General Information, CLOs, CLO–PLO mappings, Topics, Topic–CLO mappings,'),
  @('Assessment, Assessment–CLO và Reading List sẽ được sao chép.','Assessments, Assessment–CLO mappings, and the Reading List will be copied.'),
  @('Nguồn: {source.versionLabel} · {source.academicYear} · {source.semester}','Source: {source.versionLabel} · {source.academicYear} · {source.semester}'),
  @('Phân công học kỳ đích','Target-Semester Assignment'),
  @('Đang tải phân công...','Loading assignments...'),
  @('Không có phân công đang hoạt động, cùng môn và chưa gắn đề cương.','No active assignment for the same course is available without an attached syllabus.'),
  @('Admin cần tạo ClassSection của học kỳ mới trước.','An administrator must create the ClassSection for the new semester first.'),
  @('Chọn phân công đích','Select target assignment'),
  @(' — Nhóm ${assignment.groupNumber}',' — Group ${assignment.groupNumber}'),
  @('Năm học đích','Target Academic Year'),
  @('Học kỳ đích','Target Semester'),
  @('Tóm tắt thay đổi','Change Summary'),
  @('Hủy','Cancel'),
  @('Đang nhân bản...','Cloning...'),
  @('Nhân bản và chỉnh sửa','Clone and Edit')
)

Update-TextFile $frontRoot 'components\syllabus\ImportSyllabusDialog.tsx' @(
  @('Không thể import file.','Unable to import the file.'),
  @('Import đề cương thành công.','Syllabus imported successfully.'),
  @('Import syllabus từ Word/Excel','Import Syllabus from Word/Excel'),
  @('Xem trước dữ liệu, kiểm tra lỗi rồi mới xác nhận ghi vào bản Draft.','Preview the data and review validation issues before importing it into the Draft.'),
  @('Chọn file .docx hoặc .xlsx, tối đa 10 MB','Select a .docx or .xlsx file, up to 10 MB'),
  @('Đang đọc file...','Reading file...'),
  @('Chọn file','Choose File'),
  @('{preview.errorCount} lỗi, {preview.warningCount} cảnh báo','{preview.errorCount} errors, {preview.warningCount} warnings'),
  @('>Mức</th>','>Severity</th>'),
  @('>Phần</th>','>Section</th>'),
  @('>Dòng</th>','>Row</th>'),
  @('>Thông báo</th>','>Message</th>'),
  @('Xem trước thông tin chung','General Information Preview'),
  @('>Hủy</Button>','>Cancel</Button>'),
  @('Đang import...','Importing...'),
  @('Xác nhận import','Confirm Import')
)

Update-TextFile $frontRoot 'components\syllabus\SubmissionValidationDialog.tsx' @(
  @('Đề cương đã sẵn sàng để nộp','The syllabus is ready for submission'),
  @('Chưa thể nộp đề cương — còn ${validation.errorCount} lỗi','The syllabus cannot be submitted — ${validation.errorCount} errors remaining'),
  @('Tổng trọng số đánh giá','Total Assessment Weight'),
  @('Workload khai báo','Declared Workload'),
  @('Tổng {validation.workloadTotal ?? "—"} · Liên hệ {validation.workloadContact ?? "—"} · Tự học {validation.workloadPrivate ?? "—"}','Total {validation.workloadTotal ?? "—"} · Contact {validation.workloadContact ?? "—"} · Self-study {validation.workloadPrivate ?? "—"}'),
  @('Topics: liên hệ {validation.topicContactHours ?? 0} · tự học {validation.topicPrivateHours ?? 0}','Topics: contact {validation.topicContactHours ?? 0} · self-study {validation.topicPrivateHours ?? 0}'),
  @('Tất cả điều kiện bắt buộc đều đạt','All mandatory requirements are satisfied'),
  @('Khi xác nhận nộp, hệ thống sẽ tạo snapshot bất biến và chuyển đề cương đến Trưởng bộ môn.','When you submit, the system will create an immutable snapshot and forward the syllabus to the Head of Department.'),
  @('Đến mục này','Go to this section'),
  @('"Hủy" : "Đóng"','"Cancel" : "Close"'),
  @('Sửa lỗi đầu tiên','Fix First Error'),
  @('Đang nộp...','Submitting...'),
  @('Xác nhận nộp đề cương','Confirm Submission')
)

Update-TextFile $frontRoot 'components\syllabus\SyllabusDiffDetails.tsx' @(
  @('Tên học phần','Course Name'),
  @('Năm học / Khóa áp dụng','Academic Year / Applicable Cohort'),
  @('Phân loại học phần','Course Designation'),
  @('Nhóm học phần','Course Types'),
  @('Học kỳ','Semester'),
  @('Ngôn ngữ','Language'),
  @('Mối liên hệ học phần','Course Relationship'),
  @('Phương pháp giảng dạy','Teaching Methods'),
  @('Tổng khối lượng học tập','Total Workload'),
  @('Giờ học có hướng dẫn','Contact Hours'),
  @('Giờ tự học','Self-study Hours'),
  @('Học phần tiên quyết','Prerequisites'),
  @('Mục tiêu học phần','Course Objectives'),
  @('Hình thức thi','Examination Forms'),
  @('Yêu cầu thi','Examination Requirements'),
  @('Ngành','Major'),
  @('Ghi chú','Notes'),
  @('Mã','Code'),
  @('Mô tả tiếng Việt','Vietnamese Description'),
  @('Mô tả','Description'),
  @('Mức năng lực','Competency Level'),
  @('Mức Bloom','Bloom Level'),
  @('Thứ tự trong tuần','Order Within Week'),
  @('Thứ tự','Order'),
  @('PLO liên kết','Mapped PLOs'),
  @('Tuần','Week'),
  @('Tên tiếng Việt','Vietnamese Name'),
  @('Tên','Name'),
  @('Giờ lý thuyết','Lecture Hours'),
  @('Giờ thực hành','Lab Hours'),
  @('Loại nội dung','Topic Type'),
  @('Phương pháp dạy','Teaching Method'),
  @('Hoạt động học','Learning Activity'),
  @('Loại đánh giá','Assessment Type'),
  @('Tỷ trọng','Weight'),
  @('Điểm tối thiểu','Minimum Score'),
  @('Điểm tối đa','Maximum Score'),
  @('Mức đóng góp','Contribution Level'),
  @('Trọng số đóng góp','Contribution Weight'),
  @('Nội dung','Topic'),
  @('Mức giảng dạy','Teaching Level'),
  @('Thành phần đánh giá','Assessment Component'),
  @('Tỷ lệ đóng góp','Contribution Percentage'),
  @('Tên tài liệu','Resource Title'),
  @('Tác giả','Author'),
  @('Nhà xuất bản','Publisher'),
  @('Năm xuất bản','Publication Year'),
  @('Phiên bản','Edition'),
  @('Đường dẫn','URL'),
  @('Loại tài liệu','Resource Type'),
  @('Mức sử dụng','Usage Type'),
  @('Không có thay đổi thuộc tính chi tiết.','No detailed field changes.'),
  @('>Trường</th>','>Field</th>'),
  @('>Phiên bản cũ</th>','>Previous Version</th>'),
  @('>Phiên bản mới</th>','>New Version</th>'),
  @('{total} thay đổi','{total} changes'),
  @('Thêm mới ({added.length})','Added ({added.length})'),
  @('Đã xóa ({removed.length})','Removed ({removed.length})'),
  @('Đã cập nhật ({modified.length})','Modified ({modified.length})'),
  @('Phiên bản cũ','Previous Version'),
  @('Phiên bản mới','New Version'),
  @('Kết quả so sánh','Comparison Results'),
  @('Bao gồm General Info, CLO, Topic, Assessment, Reading List và toàn bộ mapping.','Includes General Information, CLOs, Topics, Assessments, Reading List, and all mappings.'),
  @('{totalChanges} thay đổi','{totalChanges} changes'),
  @('Hai phiên bản không có khác biệt về nội dung.','The two versions have no content differences.'),
  @('Thông tin chung','General Information'),
  @('{Object.keys(diff.generalInfoDiff).length} thay đổi','{Object.keys(diff.generalInfoDiff).length} changes'),
  @('Năng lực: ${item.competencyLevel}','Competency: ${item.competencyLevel}'),
  @('Ma trận CLO–PLO','CLO–PLO Matrix'),
  @('Mức: ${item.level}','Level: ${item.level}'),
  @('Trọng số: ${item.contributionWeight}','Weight: ${item.contributionWeight}'),
  @('Nội dung / Topics','Topics'),
  @('Tuần ${item.weekNumber}','Week ${item.weekNumber}'),
  @('Thứ tự ${item.orderInWeek}','Order ${item.orderInWeek}'),
  @('Ma trận Topic–CLO','Topic–CLO Matrix'),
  @('Mức: ${item.teachingLevel}','Level: ${item.teachingLevel}'),
  @('Thành phần đánh giá','Assessment Components'),
  @('Tỷ trọng: ${item.weightPercent}%','Weight: ${item.weightPercent}%'),
  @('Thứ tự ${item.orderIndex}','Order ${item.orderIndex}'),
  @('Ma trận Assessment–CLO','Assessment–CLO Matrix'),
  @('Đóng góp: ${item.contributionPercent}%','Contribution: ${item.contributionPercent}%'),
  @('Sử dụng: ${item.usageType}','Usage: ${item.usageType}')
)

Update-TextFile $frontRoot 'components\syllabus\SyllabusForm.tsx' @(
  @('label: "Học kỳ 1"','label: "Semester 1"'),
  @('label: "Học kỳ 2"','label: "Semester 2"'),
  @('label: "Học kỳ 3"','label: "Semester 3"'),
  @('label: "Học kỳ 4"','label: "Semester 4"'),
  @('label: "Học kỳ 5"','label: "Semester 5"'),
  @('label: "Học kỳ 6"','label: "Semester 6"'),
  @('label: "Học kỳ hè"','label: "Summer Semester"'),
  @('label: "Học kỳ 7"','label: "Semester 7"'),
  @('label: "Học kỳ 8"','label: "Semester 8"'),
  @('label: "Môn tự chọn"','label: "Elective"'),
  @('name: "Khóa (Academic Year)"','name: "Academic Year / Cohort"'),
  @('name: "Chuyên ngành (Major)"','name: "Major"'),
  @('name: "Course Type (Cơ sở, Chuyên ngành...)"','name: "Course Type"'),
  @('name: "Content (Topics) - Phải có ít nhất 1 chủ đề"','name: "Content (Topics) - At least one topic is required"'),
  @('name: "Assessment Plan - Phải có ít nhất 1 đánh giá với Trọng số > 0"','name: "Assessment Plan - At least one assessment with a weight greater than 0 is required"'),
  @('Vui lòng điền đầy đủ ','Please complete all '),
  @(' trường còn thiếu:\n\n',' missing fields:\n\n'),
  @('Môn học của Faculty phải được lấy từ phân công. Không được tự tạo môn mới trong form đề cương.','Faculty members must use the course from their teaching assignment. New courses cannot be created from the syllabus form.'),
  @('nameVn: courseNameVn || "Môn học mới"','nameVn: courseNameVn || "New Course"'),
  @('Không thể tự động tạo môn học mới. Vui lòng tạo môn học trong mục Quản lý môn học trước.','Unable to create a new course automatically. Create the course in Course Management first.'),
  @('Không thể lưu môn học tiên quyết / liên quan. Vui lòng kiểm tra backend hoặc dữ liệu môn học.','Unable to save prerequisite or related-course data. Check the backend service and course data.'),
  @('Mã môn, tên tiếng Anh, tín chỉ và thông tin cơ bản.','Course code, English name, credits, and basic information.'),
  @('Khóa áp dụng (Cohort)','Applicable Cohort'),
  @('placeholder="Chọn khóa"','placeholder="Select cohort"'),
  @('Chưa tải được danh sách khóa','Unable to load cohort list'),
  @('Chuyên ngành (Major)','Major'),
  @('placeholder="Chọn chuyên ngành"','placeholder="Select major"'),
  @('Chưa tải được danh sách chuyên ngành','Unable to load major list'),
  @('Phiên bản (Version)','Version'),
  @('* Vui lòng điền thông tin trường này (Please fill out this field)','* Please fill out this field'),
  @('Môn học tiên quyết / liên quan (Bản đồ đào tạo)','Prerequisite / Related Courses (Curriculum Map)'),
  @('Chọn môn học có sẵn để tự động tạo mũi tên trên "Bản đồ đào tạo" — không cần chỉnh tay.','Select existing courses to automatically generate relationship arrows on the Curriculum Map.'),
  @('"Tiên quyết"','"Prerequisite"'),
  @('"Song hành"','"Corequisite"'),
  @('"Khuyến nghị"','"Recommended"'),
  @('"Tương đương"','"Equivalent"'),
  @('Chưa chọn môn nào','No courses selected'),
  @('placeholder="Chọn môn học..."','placeholder="Select course..."'),
  @('>Tiên quyết</SelectItem>','>Prerequisite</SelectItem>'),
  @('>Song hành</SelectItem>','>Corequisite</SelectItem>'),
  @('>Khuyến nghị</SelectItem>','>Recommended</SelectItem>'),
  @('>Tương đương</SelectItem>','>Equivalent</SelectItem>'),
  @('<Plus className="size-4 mr-1" /> Thêm','<Plus className="size-4 mr-1" /> Add'),
  @('placeholder="Chọn học kỳ"','placeholder="Select semester"'),
  @('Đang xử lý...','Processing...')
)

Update-TextFile $frontRoot 'components\syllabus\SyllabusPdfPreviewDialog.tsx' @(
  @('Không thể tạo bản xem trước PDF. Vui lòng kiểm tra dữ liệu đề cương và thử lại.','Unable to generate the PDF preview. Check the syllabus data and try again.'),
  @('Không thể tạo bản xem trước PDF.','Unable to generate the PDF preview.')
)

Update-TextFile $frontRoot 'components\syllabus\SyllabusTable.tsx' @(
  @('Bạn có chắc chắn muốn xóa đề cương này?','Are you sure you want to delete this syllabus?'),
  @('Không thể kiểm tra điều kiện nộp đề cương.','Unable to validate the syllabus submission requirements.'),
  @('Đề cương đã được nộp thành công!','Syllabus submitted successfully!'),
  @('Không thể nộp đề cương.','Unable to submit the syllabus.'),
  @('Xem chi tiết','View Details'),
  @('Chỉnh sửa bản nháp','Edit Draft'),
  @('Nộp đề cương','Submit Syllabus'),
  @('Xóa bản nháp','Delete Draft'),
  @('Mã môn','Course Code'),
  @('Tên môn','Course Name'),
  @('Trạng thái','Status'),
  @('Ngành','Major'),
  @('Người tạo','Created By'),
  @('Hành động','Actions'),
  @('Không có dữ liệu đề cương phù hợp.','No matching syllabus data.'),
  @('Ngành: {metadata.major}','Major: {metadata.major}'),
  @('>Có</span>','>Yes</span>'),
  @('>Không</span>','>No</span>'),
  @('title="Xem chi tiết"','title="View details"'),
  @('title="Chỉnh sửa bản nháp"','title="Edit draft"'),
  @('title="Nộp đề cương"','title="Submit syllabus"'),
  @('title="Xóa bản nháp"','title="Delete draft"')
)

Update-TextFile $frontRoot 'components\syllabus\SyllabusToolbar.tsx' @(
  @('Tìm mã hoặc tên môn học...','Search by course code or name...'),
  @('placeholder="Học kỳ"','placeholder="Semester"'),
  @('Tất cả học kỳ','All Semesters'),
  @('Học kỳ 1','Semester 1'),
  @('Học kỳ 2','Semester 2'),
  @('Học kỳ hè','Summer Semester'),
  @('placeholder="Khóa học"','placeholder="Cohort"'),
  @('Tất cả các khóa','All Cohorts'),
  @('placeholder="Chuyên ngành"','placeholder="Major"'),
  @('Tất cả ngành','All Majors'),
  @('placeholder="Trạng thái"','placeholder="Status"'),
  @('Tất cả trạng thái','All Statuses'),
  @('Bản nháp','Draft'),
  @('Đã nộp chờ duyệt','Submitted'),
  @('Đã duyệt','Approved'),
  @('Bị từ chối','Rejected')
)

Update-TextFile $frontRoot 'components\syllabus\editor\SyllabusEditorShell.tsx' @(
  @('label:"Thông tin chung"','label:"General Information"'),
  @('description:"Thông tin cơ bản của môn học"','description:"Basic course information"'),
  @('label:"Chuẩn đầu ra (CLO)"','label:"Course Learning Outcomes (CLO)"'),
  @('description:"Ma trận liên kết CLO với PLO của ngành"','description:"Map CLOs to the program PLOs"'),
  @('label:"Nội dung giảng dạy"','label:"Teaching Content"'),
  @('shortLabel:"Nội dung"','shortLabel:"Topics"'),
  @('description:"Phân bổ nội dung theo tuần học"','description:"Distribute topics by teaching week"'),
  @('label:"Phương pháp đánh giá"','label:"Assessment Plan"'),
  @('shortLabel:"Đánh giá"','shortLabel:"Assessment"'),
  @('description:"Thành phần điểm và trọng số"','description:"Assessment components and weights"'),
  @('label:"Tài liệu tham khảo"','label:"Reading List"'),
  @('shortLabel:"Tài liệu"','shortLabel:"Reading"'),
  @('description:"Sách giáo khoa và tài liệu bổ sung"','description:"Textbooks and supplementary materials"'),
  @('label:"Bản nháp"','label:"Draft"'),
  @('label:"Đã nộp"','label:"Submitted"'),
  @('label:"Đã duyệt"','label:"Approved"'),
  @('label:"Bị từ chối"','label:"Rejected"'),
  @('label:"Cần chỉnh sửa"','label:"Revision Requested"'),
  @('label:"Đang xem xét"','label:"Under Review"'),
  @('label:"Đã lưu trữ"','label:"Archived"'),
  @('Không thể kiểm tra điều kiện nộp đề cương.','Unable to validate the syllabus submission requirements.'),
  @('Đề cương đã được nộp thành công!','Syllabus submitted successfully!'),
  @('Không thể nộp đề cương.','Unable to submit the syllabus.'),
  @('Xem trước PDF','Preview PDF'),
  @('Nộp đề cương','Submit Syllabus'),
  @('Đang tải nội dung đề cương...','Loading syllabus content...')
)

Update-TextFile $frontRoot 'components\syllabus\editor\tabs\Section1GeneralInfo.tsx' @(
  @('Đã thêm ràng buộc học phần!','Course relationship added successfully!'),
  @('Không thể thêm ràng buộc học phần','Unable to add the course relationship'),
  @('Vui lòng chọn môn học trước / môn ràng buộc','Please select the current course and related course first'),
  @('Không thể chọn chính môn hiện tại làm môn tiên quyết','The current course cannot be selected as its own prerequisite'),
  @('Đã lưu thông tin chung!','General information saved successfully!'),
  @('Thông tin môn học (từ hệ thống)','Course Information (System Data)'),
  @('Mã môn học','Course Code'),
  @('Tên môn học','Course Name'),
  @('Giảng viên soạn','Prepared By'),
  @('Trạng thái','Status'),
  @('"Đã duyệt"','"Approved"'),
  @('"Đã nộp"','"Submitted"'),
  @('"Bị từ chối"','"Rejected"'),
  @('"Bản nháp"','"Draft"'),
  @('Môn học tiên quyết / liên quan','Prerequisite / Related Courses'),
  @('"Tiên quyết"','"Prerequisite"'),
  @('"Song hành"','"Corequisite"'),
  @('"Khuyến nghị"','"Recommended"'),
  @('"Tương đương"','"Equivalent"'),
  @('Chưa có ràng buộc nào','No course relationships'),
  @('placeholder="Chọn môn học..."','placeholder="Select course..."'),
  @('>Tiên quyết</SelectItem>','>Prerequisite</SelectItem>'),
  @('>Song hành</SelectItem>','>Corequisite</SelectItem>'),
  @('>Khuyến nghị</SelectItem>','>Recommended</SelectItem>'),
  @('>Tương đương</SelectItem>','>Equivalent</SelectItem>'),
  @('+ Thêm','+ Add'),
  @('Thông tin phiên bản đề cương','Syllabus Version Information'),
  @('Năm học áp dụng','Applicable Academic Year'),
  @('Khóa theo phân công giảng dạy.','Locked to the teaching assignment.'),
  @('Nhãn phiên bản','Version Label'),
  @('Số phiên bản','Version Number'),
  @('Tóm tắt thay đổi so với kỳ trước','Change Summary from Previous Version'),
  @('Mô tả những điểm thay đổi chính so với phiên bản trước...','Describe the main changes from the previous version...'),
  @('Ghi chú nội bộ','Internal Notes'),
  @('Ghi chú thêm cho trưởng bộ môn / hội đồng...','Additional notes for the Head of Department / review committee...'),
  @('Đang lưu...','Saving...'),
  @('Lưu thông tin chung','Save General Information'),
  @('Thông tin bắt buộc trước khi nộp','Required Information Before Submission'),
  @('Mô tả vị trí/vai trò của học phần trong chương trình...','Describe the role of this course in the curriculum...'),
  @('Loại môn học','Course Type'),
  @('Học kỳ','Semester'),
  @('Chuyên ngành','Major'),
  @('Ngôn ngữ giảng dạy','Language of Instruction'),
  @('Quan hệ trong CTĐT','Curriculum Relationship'),
  @('Nhập ''Không có'' nếu không áp dụng.','Enter ''None'' if not applicable.'),
  @('Phương pháp giảng dạy','Teaching Methods'),
  @('Total = Contact + Private Study và phải khớp tổng giờ trong Topics.','Total = Contact + Private Study and must match the total hours in Topics.'),
  @('Môn tiên quyết','Prerequisites'),
  @('Mục tiêu môn học','Course Objectives'),
  @('Hình thức đánh giá/thi','Assessment / Examination Forms'),
  @('Yêu cầu học tập và thi','Learning and Examination Requirements'),
  @('Rubrics (không bắt buộc)','Rubrics (Optional)'),
  @('Lưu toàn bộ thông tin bắt buộc','Save All Required Information'),
  @('Đề cương đã được duyệt','Syllabus Approved'),
  @('Duyệt bởi ','Approved by '),
  @('<> · Ngày {new Date(syllabus.approvedAt).toLocaleDateString("vi-VN")}</>','<> · Date {new Date(syllabus.approvedAt).toLocaleDateString("en-US")}</>')
)

Update-TextFile $frontRoot 'components\syllabus\editor\tabs\Section3CLOs.tsx' @(
  @('"Bắt buộc"','"Required"'),
  @('Xóa ${code}? Mapping CLO-PLO liên quan cũng sẽ bị xóa.','Delete ${code}? Related CLO–PLO mappings will also be deleted.'),
  @('Đang tải CLO...','Loading CLOs...'),
  @('Chuẩn đầu ra môn học (CLO) theo Bloom''s Taxonomy','Course Learning Outcomes (CLOs) Based on Bloom''s Taxonomy'),
  @('Mỗi CLO phải khai báo mức độ nhận thức (Bloom) và loại năng lực (Knowledge/Skill/Attitude).','Each CLO must specify a Bloom cognitive level and competency type (Knowledge/Skill/Attitude).'),
  @('CLO sẽ được mapping với PLO của ngành ở bước tiếp theo.','CLOs will be mapped to the program PLOs in the next step.'),
  @('Danh sách CLO','CLO List'),
  @('Thêm CLO','Add CLO'),
  @('Chưa có CLO nào','No CLOs Available'),
  @('Nhấn "Thêm CLO" để bắt đầu','Click "Add CLO" to begin'),
  @('Thêm CLO mới','Add New CLO'),
  @('Mã CLO','CLO Code'),
  @('placeholder="Chọn mức Bloom..."','placeholder="Select Bloom level..."'),
  @('Mô tả (Tiếng Anh)','Description (English)'),
  @('Mô tả (Tiếng Việt)','Description (Vietnamese)'),
  @('placeholder="Sinh viên có khả năng..."','placeholder="Students will be able to..."'),
  @('Loại năng lực','Competency Type'),
  @('placeholder="Chọn loại năng lực..."','placeholder="Select competency type..."'),
  @('Lưu CLO','Save CLO'),
  @('>Hủy</','>Cancel</'),
  @('"Kiến thức"','"Knowledge"'),
  @('"Kỹ năng"','"Skill"'),
  @('"Thái độ"','"Attitude"')
)

Update-TextFile $frontRoot 'components\syllabus\editor\tabs\Section4PLOMapping.tsx' @(
  @('Đang tải ma trận...','Loading mapping matrix...'),
  @('Chưa có CLO nào','No CLOs Available'),
  @('Vui lòng thêm CLO ở tab "Chuẩn đầu ra (CLO)" trước','Add CLOs in the "Course Learning Outcomes (CLO)" tab first'),
  @('Chưa có PLO nào trong hệ thống','No PLOs Available'),
  @('Admin hoặc Trưởng khoa cần tạo PLO trước','An administrator or Dean must create PLOs first'),
  @('Mức độ liên kết:','Mapping Level:'),
  @('Introduce – Giới thiệu','Introduce'),
  @('Develop – Phát triển','Develop'),
  @('Achieve – Đạt được','Achieve'),
  @('Nhấn vào ô để chuyển mức: Trống → I → D → A → Trống','Click a cell to cycle the level: Empty → I → D → A → Empty'),
  @('Cảnh báo: ','Warning: '),
  @(' chưa được mapping với PLO nào.',' are not mapped to any PLO.'),
  @('Đề cương sẽ không thể nộp cho đến khi hoàn thành mapping.','The syllabus cannot be submitted until all required mappings are completed.'),
  @('Ma trận CLO – PLO','CLO–PLO Matrix'),
  @('Đã mapping đầy đủ','All CLOs Mapped'),
  @('Tổng CLO/PLO','CLO/PLO Total'),
  @('${clo.code} × ${plo.code}: ${level || "Không có"}','${clo.code} × ${plo.code}: ${level || "None"}')
)

Update-TextFile $frontRoot 'components\syllabus\editor\tabs\Section5Topics.tsx' @(
  @('Vui lòng nhập tên nội dung','Please enter a topic name'),
  @('Xóa nội dung này?','Delete this topic?'),
  @('Đang tải nội dung...','Loading topics...'),
  @('Tổng chủ đề','Total Topics'),
  @('Tiết LT','Lecture Hours'),
  @('Tiết TH','Lab Hours'),
  @('Tiết tự học','Self-study Hours'),
  @('Nội dung giảng dạy theo tuần','Weekly Teaching Content'),
  @('Thêm nội dung','Add Topic'),
  @('Chưa có nội dung giảng dạy','No Teaching Topics Available'),
  @('Nhấn "Thêm nội dung" để bắt đầu','Click "Add Topic" to begin'),
  @('Tuần {week}','Week {week}'),
  @('{byWeek[week].length} nội dung','{byWeek[week].length} topics'),
  @('Tên nội dung (EN)','Topic Name (EN)'),
  @('Tên nội dung (VN)','Topic Name (VN)'),
  @('LT (tiết)','Lecture Hours'),
  @('TH (tiết)','Lab Hours'),
  @('Tự học','Self-study'),
  @('>Loại</Label>','>Type</Label>'),
  @('Phương pháp giảng dạy','Teaching Method'),
  @('Hoạt động học','Learning Activity'),
  @('>Lưu</','>Save</'),
  @('>Hủy</','>Cancel</'),
  @('LT: {topic.teachingHours} tiết','Lecture: {topic.teachingHours} hours'),
  @('TH: {topic.labHours} tiết','Lab: {topic.labHours} hours'),
  @('Tự học: {topic.selfStudyHours} tiết','Self-study: {topic.selfStudyHours} hours'),
  @('Thêm nội dung mới','Add New Topic'),
  @('Tuần số','Week Number'),
  @('Loại nội dung','Topic Type'),
  @('Tên nội dung (Tiếng Anh)','Topic Name (English)'),
  @('Tên nội dung (Tiếng Việt)','Topic Name (Vietnamese)'),
  @('placeholder="Giới thiệu về..."','placeholder="Introduction to..."'),
  @('Hoạt động học tập','Learning Activity'),
  @('Lưu nội dung','Save Topic')
)

Update-TextFile $frontRoot 'components\syllabus\editor\tabs\Section6Assessment.tsx' @(
  @('"Bắt buộc"','"Required"'),
  @('"Phải > 0"','"Must be > 0"'),
  @('"Vượt quá 100%"','"Exceeds 100%"'),
  @('Xóa thành phần đánh giá này?','Delete this assessment component?'),
  @('Đang tải thành phần đánh giá...','Loading assessment components...'),
  @('Tổng trọng số: {totalWeight}% / 100%','Total Weight: {totalWeight}% / 100%'),
  @('Tổng trọng số đã hợp lệ. Đề cương có thể được nộp.','The total weight is valid. The syllabus can be submitted.'),
  @('Tổng trọng số các thành phần đánh giá phải đúng bằng 100%.','The total assessment weight must equal 100%.'),
  @('Các thành phần đánh giá','Assessment Components'),
  @('Thêm thành phần','Add Component'),
  @('Chưa có thành phần đánh giá','No Assessment Components'),
  @('Nhấn "Thêm thành phần" để bắt đầu','Click "Add Component" to begin'),
  @('Thêm đánh giá mới','Add New Assessment'),
  @('Tên thành phần (EN)','Component Name (EN)'),
  @('Tên thành phần (VN)','Component Name (VN)'),
  @('placeholder="Thi giữa kỳ..."','placeholder="Midterm Exam..."'),
  @('Loại đánh giá','Assessment Type'),
  @('Trọng số (%)','Weight (%)'),
  @('Điểm tối thiểu','Minimum Score'),
  @('Điểm tối đa','Maximum Score'),
  @('>Lưu</','>Save</'),
  @('>Hủy</','>Cancel</')
)

Update-TextFile $frontRoot 'components\syllabus\editor\tabs\Section7ReadingList.tsx' @(
  @('label: "Bắt buộc"','label: "Required"'),
  @('label: "Khuyến nghị"','label: "Recommended"'),
  @('label: "Bổ sung"','label: "Supplementary"'),
  @('Vui lòng chọn tài liệu','Please select a resource'),
  @('Vui lòng nhập tên tài liệu','Please enter the resource title'),
  @('Xóa tài liệu "${title}" khỏi đề cương này?','Remove resource "${title}" from this syllabus?'),
  @('Đang tải tài liệu tham khảo...','Loading reading list...'),
  @('Tài liệu tham khảo cho đề cương','Syllabus Reading List'),
  @('Chọn tài liệu đã có trong thư viện hoặc tạo tài liệu mới, sau đó gắn vào đề cương với loại sử dụng phù hợp.','Select an existing resource from the library or create a new one, then assign the appropriate usage type.'),
  @('Danh sách tài liệu','Resource List'),
  @('{syllabusBooks.length} tài liệu','{syllabusBooks.length} resources'),
  @('Thêm tài liệu','Add Resource'),
  @('Chưa có tài liệu tham khảo','No Reading Resources'),
  @('Nhấn "Thêm tài liệu" để bắt đầu','Click "Add Resource" to begin'),
  @('Tác giả: {book.author}','Author: {book.author}'),
  @('Năm: {book.year}','Year: {book.year}'),
  @('Ấn bản: {book.edition}','Edition: {book.edition}'),
  @('Mở liên kết tài liệu','Open Resource Link'),
  @('Thêm tài liệu tham khảo','Add Reading Resource'),
  @('Cách thêm','Add Method'),
  @('Chọn từ thư viện có sẵn','Select from Existing Library'),
  @('Tạo tài liệu mới','Create New Resource'),
  @('Loại sử dụng','Usage Type'),
  @('Tài liệu trong thư viện','Library Resource'),
  @('placeholder="Chọn tài liệu..."','placeholder="Select resource..."'),
  @('Không còn tài liệu khả dụng','No resources available'),
  @('Tên tài liệu','Resource Title'),
  @('placeholder="Nhập tên sách / bài báo / tài liệu..."','placeholder="Enter a book, article, or resource title..."'),
  @('Tác giả','Author'),
  @('Nhà xuất bản','Publisher'),
  @('>Năm</Label>','>Year</Label>'),
  @('Ấn bản','Edition'),
  @('>Hủy</','>Cancel</'),
  @('Đang lưu...','Saving...'),
  @('Lưu tài liệu','Save Resource')
)

Update-TextFile $frontRoot 'pages\syllabus\SyllabusCreatePage.tsx' @(
  @('Tạo đề cương thành công. Hệ thống đã gắn Draft vào phân công của bạn.','Syllabus created successfully. The Draft has been linked to your teaching assignment.'),
  @('Không thể tạo đề cương. Vui lòng kiểm tra phân công, học kỳ và năm học.','Unable to create the syllabus. Check the teaching assignment, semester, and academic year.'),
  @('Tạo đề cương môn học mới','Create New Course Syllabus'),
  @('Faculty chỉ có thể tạo đề cương cho môn, học kỳ và năm học đã được phân công.','Faculty members can only create syllabi for assigned courses, semesters, and academic years.'),
  @('Import từ đề cương cũ (tùy chọn)','Import from Previous Syllabus (Optional)'),
  @('-- Chọn đề cương để sao chép --','-- Select a syllabus to copy --'),
  @('Đang tải dữ liệu...','Loading data...'),
  @('Phân công được phép tạo đề cương','Eligible Teaching Assignment'),
  @('Đang tải phân công...','Loading assignments...'),
  @('Bạn không có phân công đang hoạt động nào chưa gắn đề cương. Admin cần tạo phân công trước,','You do not have any active teaching assignments without an attached syllabus. An administrator must create an assignment first,'),
  @('để trống trường Đề cương, rồi bạn mới có thể tạo Draft.','leave the Syllabus field empty, and then you can create a Draft.'),
  @('placeholder="Chọn phân công"','placeholder="Select assignment"'),
  @(' — Nhóm ${assignment.groupNumber}',' — Group ${assignment.groupNumber}'),
  @('Chưa có phân công hợp lệ để tạo đề cương.','No eligible teaching assignment is available for syllabus creation.'),
  @('Đang đồng bộ dữ liệu từ đề cương đã chọn...','Synchronizing data from the selected syllabus...')
)

Update-TextFile $frontRoot 'pages\syllabus\SyllabusDetailPage.tsx' @(
  @('DRAFT: "Bản nháp"','DRAFT: "Draft"'),
  @('SUBMITTED: "Đã nộp"','SUBMITTED: "Submitted"'),
  @('UNDER_REVIEW: "Đang xem xét"','UNDER_REVIEW: "Under Review"'),
  @('APPROVED: "Đã duyệt"','APPROVED: "Approved"'),
  @('REJECTED: "Bị từ chối"','REJECTED: "Rejected"'),
  @('REVISION_REQUESTED: "Cần chỉnh sửa"','REVISION_REQUESTED: "Revision Requested"'),
  @('ARCHIVED: "Đã lưu trữ"','ARCHIVED: "Archived"'),
  @('ID đề cương không hợp lệ.','Invalid syllabus ID.'),
  @('Đang tải đề cương...','Loading syllabus...'),
  @('Không tìm thấy đề cương hoặc bạn không có','The syllabus was not found or you do not have'),
  @('quyền truy cập.','permission to access it.'),
  @('Không thể xuất PDF đề cương.','Unable to export the syllabus PDF.'),
  @('Năm học {syllabus.academicYear}','Academic Year {syllabus.academicYear}'),
  @('Chưa xác định học kỳ','Semester not specified'),
  @('Phiên bản {syllabus.versionLabel}','Version {syllabus.versionLabel}'),
  @('Lịch sử review','Review History'),
  @('Xem trước PDF','Preview PDF'),
  @('Xuất PDF','Export PDF'),
  @('So sánh V','Compare V'),
  @('label="Chuyên ngành"','label="Major"'),
  @('label="Cập nhật"','label="Updated"'),
  @('label="Người tạo"','label="Created By"'),
  @('label="Người duyệt"','label="Approved By"'),
  @('Chưa duyệt','Not Approved'),
  @('Thông tin học phần','Course Information'),
  @('label="Loại môn học"','label="Course Type"'),
  @('label="Ngôn ngữ"','label="Language"'),
  @('label="Quan hệ CTĐT"','label="Curriculum Relationship"'),
  @('label="Phương pháp giảng dạy"','label="Teaching Methods"'),
  @('label="Điều kiện tiên quyết"','label="Prerequisites"'),
  @('Thông tin phiên bản','Version Information'),
  @('label="Phiên bản hiện hành"','label="Current Version"'),
  @('? "Có"','? "Yes"'),
  @(': "Không"',': "No"'),
  @('label="Ngày nộp"','label="Submitted At"'),
  @('label="Ngày duyệt"','label="Approved At"'),
  @('label="Tóm tắt thay đổi"','label="Change Summary"'),
  @('label="Ghi chú"','label="Notes"'),
  @('"vi-VN"','"en-US"')
)

Update-TextFile $frontRoot 'pages\syllabus\SyllabusDiffPage.tsx' @(
  @('Vui lòng chọn phiên bản đề cương khác để so sánh.','Select another syllabus version to compare.'),
  @('Đang tải dữ liệu so sánh...','Loading comparison data...'),
  @('Không tải được dữ liệu so sánh. Hãy kiểm tra hai phiên bản có cùng học phần và bạn có quyền xem cả hai phiên bản hay không.','Unable to load comparison data. Verify that both versions belong to the same course and that you have access to both versions.'),
  @('So sánh phiên bản đề cương','Syllabus Version Comparison')
)

Update-TextFile $frontRoot 'pages\syllabus\SyllabusEditPage.tsx' @(
  @('Phiên bản chỉ đọc','Read-only Version'),
  @(' đang ở trạng thái ',' is currently '),
  @('Chỉ phiên bản Draft mới được phép chỉnh sửa.','Only Draft versions can be edited.'),
  @('Quay lại danh sách phiên bản','Back to Version List'),
  @('Cập nhật đề cương thành công!','Syllabus updated successfully!'),
  @('Lỗi khi cập nhật đề cương: ','Unable to update the syllabus: '),
  @('Form Đề cương Học phần','Course Syllabus Form'),
  @('← QUAY LẠI DANH SÁCH','← BACK TO LIST')
)

Update-TextFile $frontRoot 'pages\syllabus\SyllabusEditorPage.tsx' @(
  @('ID đề cương không hợp lệ','Invalid syllabus ID'),
  @('Quay lại','Back'),
  @('Đang tải đề cương...','Loading syllabus...'),
  @('Không tìm thấy đề cương','Syllabus Not Found'),
  @('Đề cương này không tồn tại hoặc bạn không có quyền truy cập.','This syllabus does not exist or you do not have permission to access it.')
)

Update-TextFile $frontRoot 'pages\syllabus\SyllabusListPage.tsx' @(
  @('Đang tải danh mục đề cương...','Loading syllabus catalog...'),
  @('Đã xảy ra lỗi','An Error Occurred'),
  @('Không thể tải dữ liệu đề cương môn học. Vui lòng thử lại sau.','Unable to load course syllabus data. Please try again later.'),
  @('Bạn có chắc chắn muốn xóa đề cương này không?','Are you sure you want to delete this syllabus?'),
  @('Đã xóa đề cương thành công!','Syllabus deleted successfully!'),
  @('Xóa đề cương thất bại.','Unable to delete the syllabus.'),
  @('Vui lòng chọn Chương trình đào tạo trước khi xem bản đồ.','Select a curriculum program before opening the map.'),
  @('Chỉ đề cương đã được phê duyệt mới có thể xuất bản PDF chính thức.','Only approved syllabi can be exported as official PDFs.'),
  @('Không thể xuất PDF đề cương. Vui lòng thử lại.','Unable to export the syllabus PDF. Please try again.'),
  @('Bạn có chắc muốn nộp đề cương ${item.courseCode} để Trưởng bộ môn duyệt không?','Are you sure you want to submit syllabus ${item.courseCode} for Head of Department review?'),
  @('Đã nộp đề cương. Đề cương đang chờ Trưởng bộ môn duyệt.','Syllabus submitted successfully. It is awaiting Head of Department review.'),
  @('Không thể nộp đề cương.','Unable to submit the syllabus.'),
  @('<span className="text-slate-400">SCSE</span> / QUẢN LÝ ĐỀ CƯƠNG','<span className="text-slate-400">SCSE</span> / SYLLABUS MANAGEMENT'),
  @('Danh mục{" "}','Syllabus{" "}'),
  @('Đề cương Học phần','Catalog'),
  @('Xem bản đồ đào tạo','View Curriculum Map'),
  @('Tạo đề cương mới','Create New Syllabus'),
  @('BỘ LỌC','FILTERS'),
  @('Lịch sử phê duyệt đề cương','Syllabus Approval History'),
  @('Chọn nút “Lịch sử” tại đề cương cần xem để','Select the “History” button for a syllabus to'),
  @('hiển thị toàn bộ các lần review và nhận xét.','view all review rounds and comments.'),
  @('Đóng hướng dẫn','Close Guide'),
  @('Tìm theo mã, tên môn, giảng viên...','Search by course code, course name, or instructor...'),
  @('placeholder="Khóa"','placeholder="Cohort"'),
  @('Khóa: Tất cả','Cohort: All'),
  @('placeholder="Ngành"','placeholder="Major"'),
  @('Tất cả ngành','All Majors'),
  @('placeholder="Học kỳ"','placeholder="Semester"'),
  @('Tất cả HK','All Semesters'),
  @('>Mã môn</th>','>Course Code</th>'),
  @('>Tên học phần</th>','>Course Name</th>'),
  @('>Khóa</th>','>Cohort</th>'),
  @('>Ngành</th>','>Major</th>'),
  @('>Học kỳ</th>','>Semester</th>'),
  @('>Trạng thái</th>','>Status</th>'),
  @('>Người tạo</th>','>Created By</th>'),
  @('>Thao tác</th>','>Actions</th>'),
  @('Không có đề cương nào phù hợp với bộ lọc.','No syllabi match the selected filters.'),
  @('Xem lịch sử phê duyệt ${item.courseCode}','View approval history for ${item.courseCode}'),
  @('>Lịch sử</','>History</'),
  @('title="Nộp đề cương"','title="Submit syllabus"'),
  @('title="Xuất PDF chính thức"','title="Export official PDF"'),
  @('Xuất PDF chính thức cho ${item.courseCode}','Export official PDF for ${item.courseCode}'),
  @('title="Nhân bản sang học kỳ mới"','title="Clone to a new semester"'),
  @('title="Chỉnh sửa bản nháp"','title="Edit draft"'),
  @('title="Xóa bản nháp"','title="Delete draft"')
)

Update-TextFile $frontRoot 'pages\syllabus\CohortSyllabusDiffPage.tsx' @(
  @('Tên học phần','Course Name'),
  @('Năm học / Khóa áp dụng','Academic Year / Applicable Cohort'),
  @('Mô tả tiếng Việt','Vietnamese Description'),
  @('Mô tả','Description'),
  @('Thứ tự trong tuần','Order Within Week'),
  @('Thứ tự','Order'),
  @('Tuần','Week'),
  @('Tên tiếng Việt','Vietnamese Name'),
  @('Giờ lý thuyết','Lecture Hours'),
  @('Giờ thực hành','Lab Hours'),
  @('Giờ tự học','Self-study Hours'),
  @('Loại topic','Topic Type'),
  @('Phương pháp dạy','Teaching Method'),
  @('Hoạt động học','Learning Activity'),
  @('Ghi chú','Notes'),
  @('Loại đánh giá','Assessment Type'),
  @('Tỷ trọng','Weight'),
  @('Điểm tối thiểu','Minimum Score'),
  @('Điểm tối đa','Maximum Score'),
  @('Loại môn','Course Type'),
  @('Học kỳ gợi ý','Suggested Semester'),
  @('Năm gợi ý','Suggested Year'),
  @('Bắt buộc','Required'),
  @('Không tải được dữ liệu bộ lọc so sánh.','Unable to load comparison filter data.'),
  @('Vui lòng chọn chương trình, khóa gốc và khóa đích.','Select a program, source cohort, and target cohort.'),
  @('Khóa gốc và khóa đích phải khác nhau.','Source and target cohorts must be different.'),
  @('Vui lòng chọn học phần và đủ 2 phiên bản đề cương.','Select a course and two syllabus versions.'),
  @('Phiên bản gốc và phiên bản đích phải khác nhau.','Source and target versions must be different.'),
  @('Không thể tải dữ liệu so sánh. Vui lòng kiểm tra lại dữ liệu đã chọn.','Unable to load comparison data. Check the selected values.'),
  @('Không có thay đổi chi tiết.','No detailed changes were found.'),
  @('Thêm mới','Added'),
  @('Đã xóa','Removed'),
  @('Cập nhật','Modified'),
  @('Mã môn','Course Code'),
  @('Tên môn','Course Name'),
  @('Thay đổi','Changes'),
  @('Trạng thái','Status'),
  @('Xem thay đổi đề cương →','View Syllabus Changes →'),
  @('So sánh phiên bản','Version Comparison'),
  @('Trung tâm so sánh','Comparison Center'),
  @('Loại so sánh','Comparison Type'),
  @('Đề cương học phần','Course Syllabus'),
  @('Chương trình đào tạo','Curriculum Program'),
  @('Chương trình','Program'),
  @('Chọn chương trình','Select program'),
  @('Khóa gốc','Source Cohort'),
  @('Chọn khóa gốc','Select source cohort'),
  @('Khóa đích','Target Cohort'),
  @('Chọn khóa đích','Select target cohort'),
  @('Học phần','Course'),
  @('Chọn học phần','Select course'),
  @('Phiên bản gốc','Source Version'),
  @('Chọn bản gốc','Select source version'),
  @('Phiên bản đích','Target Version'),
  @('Chọn bản đích','Select target version'),
  @('ĐANG SO SÁNH...','COMPARING...'),
  @('SO SÁNH','COMPARE')
)

Update-TextFile $frontRoot 'components\syllabus\SyllabusDiffDetails.tsx' @(
  @('Name tài liệu','Resource Title'),
  @('Edition cũ','Previous Version'),
  @('Edition mới','New Version')
)

Update-TextFile $frontRoot 'components\syllabus\SyllabusForm.tsx' @(
  @('* Vui lòng chọn ít nhất 1 loại môn học (Please check at least one)','* Please select at least one course type'),
  @('Chưa tải được danh sách học kỳ','Unable to load semester list'),
  @('Chuẩn đầu ra môn học và Mức độ năng lực (Competency level).','Course learning outcomes and competency levels.'),
  @('>Nội dung</th>','>Description</th>'),
  @('<Plus className="mr-1.5 size-4" /> Thêm CLO','<Plus className="mr-1.5 size-4" /> Add CLO'),
  @('<Plus className="mr-1.5 size-4" /> Thêm Topic','<Plus className="mr-1.5 size-4" /> Add Topic'),
  @('Hình thức kiểm tra, yêu cầu khóa học và tài liệu tham khảo.','Examination forms, course requirements, and references.'),
  @('<Plus className="mr-1.5 size-4" /> Thêm tài liệu','<Plus className="mr-1.5 size-4" /> Add Reference'),
  @('Lịch trình giảng dạy chi tiết theo tuần.','Detailed weekly teaching schedule.'),
  @('<Plus className="mr-1.5 size-4" /> Thêm Tuần học','<Plus className="mr-1.5 size-4" /> Add Teaching Week'),
  @('Phân bổ trọng số của các hình thức đánh giá cho từng CLO.','Distribute assessment weights across CLOs.'),
  @('* Phải có ít nhất 1 đánh giá với Hình thức và Trọng số &gt; 0','* At least one assessment with a type and weight greater than 0 is required'),
  @('<Plus className="mr-1.5 size-4" /> Thêm Assessment','<Plus className="mr-1.5 size-4" /> Add Assessment'),
  @('Thay đổi sẽ tạo phiên bản mới','Changes will create a new version'),
  @('<X className="size-4 mr-2" /> Hủy','<X className="size-4 mr-2" /> Cancel'),
  @('<Save className="size-4 mr-2" /> Lưu V3','<Save className="size-4 mr-2" /> Save V3')
)

Update-TextFile $frontRoot 'components\syllabus\SyllabusPdfPreviewDialog.tsx' @(
  @('Xem trước đề cương theo mẫu SCSE','SCSE Syllabus Preview'),
  @('Dữ liệu được tạo trực tiếp từ hệ thống','Data generated directly from the system'),
  @('Làm mới','Refresh'),
  @('Mở tab mới','Open in New Tab'),
  @('Tải PDF','Download PDF'),
  @('Đang dựng bản PDF...','Generating PDF...'),
  @('Hệ thống đang tổng hợp CLO, PLO, nội dung và đánh giá.','The system is compiling CLOs, PLOs, topics, and assessments.'),
  @('Không thể hiển thị PDF','Unable to Display PDF'),
  @('Thử lại','Try Again')
)

Update-TextFile $frontRoot 'components\syllabus\SyllabusTable.tsx' @(
  @('Đã duyệt','Approved'),
  @('Đã nộp','Submitted'),
  @('Từ chối','Rejected'),
  @('Cần chỉnh sửa','Revision Requested'),
  @('Đã lưu trữ','Archived'),
  @('Bản nháp','Draft'),
  @('Tên đề cương','Syllabus Title'),
  @('Chuyên ngành','Major'),
  @('Học kỳ','Semester'),
  @('Năm học','Academic Year'),
  @('Hiện hành','Current')
)

Update-TextFile $frontRoot 'components\syllabus\editor\tabs\Section3CLOs.tsx' @(
  @('Nhấn "Add CLO" để bắt đầu','Click "Add CLO" to begin'),
  @('Add CLO mới','Add New CLO'),
  @('Hủy','Cancel')
)

Update-TextFile $frontRoot 'components\syllabus\editor\tabs\Section4PLOMapping.tsx' @(
  @('Tổng','Total')
)

Update-TextFile $frontRoot 'components\syllabus\editor\tabs\Section5Topics.tsx' @(
  @('Nhấn "Add Topic" để bắt đầu','Click "Add Topic" to begin'),
  @('Lưu','Save'),
  @('Hủy','Cancel'),
  @('Self-study: {topic.selfStudyHours} tiết','Self-study: {topic.selfStudyHours} hours'),
  @('Add Topic mới','Add New Topic'),
  @('Learning Activity tập','Learning Activity')
)

Update-TextFile $frontRoot 'components\syllabus\editor\tabs\Section6Assessment.tsx' @(
  @('Nhấn "Add Component" để bắt đầu','Click "Add Component" to begin'),
  @('Lưu','Save'),
  @('Hủy','Cancel')
)

Update-TextFile $frontRoot 'components\syllabus\editor\tabs\Section7ReadingList.tsx' @(
  @('Nhấn "Add Resource" để bắt đầu','Click "Add Resource" to begin'),
  @('Add Resource tham khảo','Add Reading Resource'),
  @('Hủy','Cancel')
)

Update-TextFile $frontRoot 'pages\syllabus\SyllabusListPage.tsx' @(
  @('Lịch sử','History')
)

Update-TextFile $backRoot 'syllabus\exception\SyllabusSubmissionValidationException.java' @(
  @('Đề cương chưa đủ điều kiện để nộp.','The syllabus does not meet the submission requirements.')
)

Update-TextFile $backRoot 'syllabus\dto\CreateSyllabusRequest.java' @(
  @('Vui lòng chọn năm học/khóa áp dụng','Please select the academic year / applicable cohort'),
  @('Vui lòng chọn học kỳ','Please select the semester')
)

Update-TextFile $backRoot 'syllabus\pdf\SyllabusPdfDataLoader.java' @(
  @('Không tìm thấy đề cương.','Syllabus not found.')
)

Update-TextFile $backRoot 'syllabus\pdf\SyllabusPdfFontProvider.java' @(
  @('Không thể khởi tạo font PDF.','Unable to initialize the PDF font.')
)

Update-TextFile $backRoot 'syllabus\pdf\SyllabusPdfRenderer.java' @(
  @('Không thể tạo PDF đề cương.','Unable to generate the syllabus PDF.'),
  @('Không thể tạo PDF đề cương: ','Unable to generate the syllabus PDF: ')
)

Update-TextFile $backRoot 'syllabus\importer\service\SyllabusImportServiceImpl.java' @(
  @('Vui lòng chọn file Word hoặc Excel.','Please select a Word or Excel file.'),
  @('Kích thước file tối đa là 10 MB.','The maximum file size is 10 MB.'),
  @('Chỉ hỗ trợ file .docx hoặc .xlsx.','Only .docx and .xlsx files are supported.'),
  @('Không thể đọc file: ','Unable to read the file: '),
  @('Dữ liệu import còn lỗi. Vui lòng xem trước và sửa trước khi xác nhận.','The imported data contains errors. Review and correct them before confirming.'),
  @('Chỉ có thể import vào đề cương DRAFT.','Import is only allowed for a DRAFT syllabus.'),
  @('Không tìm thấy sheet General Info.','General Info sheet not found.'),
  @('Không tìm thấy sheet CLO.','CLO sheet not found.'),
  @('Không tìm thấy sheet Topics.','Topics sheet not found.'),
  @('Không tìm thấy sheet Assessments.','Assessments sheet not found.'),
  @('Không tìm thấy sheet Reading List.','Reading List sheet not found.'),
  @('Không có dữ liệu để import.','No data is available for import.'),
  @('Mã CLO không được trống.','CLO code is required.'),
  @('Mã CLO bị trùng: ','Duplicate CLO code: '),
  @('CLO chưa có mô tả.','CLO description is missing.'),
  @('Tên thành phần đánh giá không được trống.','Assessment component name is required.'),
  @('Trọng số không được trống.','Weight is required.'),
  @('Tổng trọng số hiện là ','The current total weight is '),
  @('%, nên bằng 100%.','%; it should equal 100%.'),
  @('Tuần học không được trống.','Week number is required.'),
  @('Tên chủ đề không được trống.','Topic name is required.'),
  @('Tên tài liệu không được trống.','Resource title is required.'),
  @('Giá trị không hợp lệ: ','Invalid value: '),
  @('. Cho phép: ','. Allowed values: ')
)

Update-TextFile $backRoot 'syllabus\service\SyllabusAccessService.java' @(
  @('Bạn không được phép xem đề cương này. ','You are not authorized to view this syllabus. '),
  @('Giảng viên chỉ được xem môn được phân công; ','Instructors may only view assigned courses; '),
  @('Trưởng bộ môn chỉ được xem môn thuộc bộ môn mình.','Heads of Department may only view courses in their own department.'),
  @('Chỉ giảng viên được phân công môn này hoặc Admin ','Only the instructor assigned to this course or an Admin '),
  @('mới được chỉnh sửa đề cương.','may edit this syllabus.'),
  @('mới được tạo đề cương.','may create a syllabus.'),
  @('Vui lòng chọn phân công giảng dạy ','Please select a teaching assignment '),
  @('trước khi tạo đề cương.','before creating a syllabus.'),
  @('Không tìm thấy phân công giảng dạy.','Teaching assignment not found.'),
  @('Tài khoản chưa được liên kết ','The account is not linked '),
  @('với hồ sơ giảng viên.','to an instructor profile.'),
  @('Bạn không được phân công phụ trách ','You are not assigned to '),
  @('nhóm lớp đã chọn.','the selected class section.'),
  @('Bạn không có phân công ACTIVE khớp môn, năm học và học kỳ đã chọn.','You do not have an ACTIVE assignment matching the selected course, academic year, and semester.'),
  @('Đề cương nguồn không hợp lệ.','The source syllabus is invalid.'),
  @('mới được nhân bản đề cương.','may clone a syllabus.'),
  @('Vui lòng chọn phân công của học kỳ đích.','Please select the target-semester assignment.'),
  @('Không tìm thấy phân công học kỳ đích.','Target-semester assignment not found.'),
  @('Chỉ được clone đề cương sang phân công của cùng môn học.','A syllabus may only be cloned to an assignment for the same course.'),
  @('Tài khoản chưa được liên kết với hồ sơ giảng viên.','The account is not linked to an instructor profile.'),
  @('Bạn không được phân công phụ trách học kỳ đích đã chọn.','You are not assigned to the selected target semester.'),
  @('Không được clone bản Draft của giảng viên khác.','You cannot clone another instructor''s Draft.'),
  @('Bạn chỉ được duyệt đề cương thuộc đúng bộ môn mình phụ trách.','You may only review syllabi from the department you manage.'),
  @('Chỉ Trưởng khoa mới được xử lý bước phê duyệt này.','Only the Dean may process this approval step.'),
  @('Bạn không có quyền xử lý bước phê duyệt này.','You are not authorized to process this approval step.'),
  @('Bạn không được phép xem yêu cầu phê duyệt này.','You are not authorized to view this approval request.'),
  @('Vai trò của bạn không được xem hàng chờ ở bước này.','Your role is not authorized to view the queue for this step.'),
  @('Tài khoản chưa được liên kết với hồ sơ giảng viên/bộ môn.','The account is not linked to an instructor/department profile.'),
  @('Không tìm thấy hồ sơ giảng viên của tài khoản.','No instructor profile was found for this account.'),
  @('Hồ sơ giảng viên chưa được gán bộ môn.','The instructor profile is not assigned to a department.'),
  @('Bộ môn của môn học chưa có tài khoản Trưởng bộ môn đang hoạt động.','The course department does not have an active Head of Department account.'),
  @('Phân công đã ngừng hoạt động nên ','The teaching assignment is inactive, so '),
  @('không thể tạo đề cương.','the syllabus cannot be created.'),
  @('Phân công chưa được gắn môn học.','The teaching assignment is not linked to a course.'),
  @('Phân công chưa được gắn giảng viên.','The teaching assignment is not linked to an instructor.'),
  @('Phân công chưa có năm học hợp lệ.','The teaching assignment does not have a valid academic year.'),
  @('Phân công chưa có học kỳ hợp lệ.','The teaching assignment does not have a valid semester.'),
  @('Phân công này đã có đề cương. ','This teaching assignment already has a syllabus. '),
  @('Hãy mở đề cương hiện có để chỉnh sửa.','Open the existing syllabus to edit it.'),
  @('Năm học đích không khớp với phân công đã chọn.','The target academic year does not match the selected assignment.'),
  @('Học kỳ đích không khớp với phân công đã chọn.','The target semester does not match the selected assignment.'),
  @('Môn học trong form không khớp ','The course in the form does not match '),
  @('với phân công đã chọn.','the selected teaching assignment.'),
  @('Năm học trong form không khớp ','The academic year in the form does not match '),
  @('Học kỳ trong form không khớp ','The semester in the form does not match '),
  @('Học kỳ của phân công không hợp lệ.','The assignment semester is invalid.'),
  @('Vui lòng chọn học kỳ.','Please select a semester.'),
  @('Môn học chưa được gán bộ môn nên chưa thể phân quyền.','The course is not assigned to a department, so authorization cannot be evaluated.'),
  @('Vui lòng chọn năm học/khóa áp dụng đúng với phân công.','Select the academic year / applicable cohort that matches the teaching assignment.'),
  @('Vui lòng chọn học kỳ đúng với phân công.','Select the semester that matches the teaching assignment.')
)

Update-TextFile $backRoot 'syllabus\service\SyllabusContentGuard.java' @(
  @(' đã được nộp và là bất biến. ',' has already been submitted and is immutable. '),
  @('Hãy Clone để tạo một bản DRAFT mới.','Clone it to create a new DRAFT.'),
  @(' chỉ được tạo giữa các thành phần ',' may only be created between components '),
  @('thuộc cùng một syllabus.','belonging to the same syllabus.')
)

Update-TextFile $backRoot 'syllabus\service\SyllabusServiceImpl.java' @(
  @('Đề cương không thuộc đúng môn học ','The syllabus does not belong to the correct course '),
  @('trong chương trình đào tạo','in the curriculum program'),
  @('Không thể clone Topic-CLO vì CLO nguồn không tồn tại trong bản sao.','Unable to clone Topic–CLO mapping because the source CLO does not exist in the copy.'),
  @('Không thể clone Assessment-CLO vì CLO nguồn không tồn tại trong bản sao.','Unable to clone Assessment–CLO mapping because the source CLO does not exist in the copy.'),
  @('Môn học này đã có một version đang chờ phê duyệt.','This course already has a version pending approval.'),
  @('Chỉ bản DRAFT mới được phép chỉnh sửa hoặc xóa. ','Only a DRAFT may be edited or deleted. '),
  @('Version đã nộp là bất biến; hãy Clone để tạo bản nháp mới.','Submitted versions are immutable; clone the syllabus to create a new Draft.'),
  @('Vui lòng chọn học kỳ hoặc phân công đích để nhân bản đề cương.','Select a target semester or teaching assignment before cloning the syllabus.'),
  @('Chỉ version REVISION_REQUESTED mới được tạo bản Draft chỉnh sửa.','A revision Draft can only be created from a REVISION_REQUESTED version.')
)

Update-TextFile $backRoot 'syllabus\service\SyllabusSubmissionValidationService.java' @(
  @('"Thông tin chung"','"General Information"'),
  @('"Chuẩn đầu ra (CLO)"','"Course Learning Outcomes (CLO)"'),
  @('"Nội dung giảng dạy"','"Teaching Content"'),
  @('"Phương pháp đánh giá"','"Assessment Plan"'),
  @('"Tài liệu tham khảo"','"Reading List"'),
  @('Đề cương chưa gắn với môn học.','The syllabus is not linked to a course.'),
  @('Mã môn học không được để trống.','Course code is required.'),
  @('Tên môn học tiếng Anh không được để trống.','English course name is required.'),
  @('Tên môn học tiếng Việt không được để trống.','Vietnamese course name is required.'),
  @('Số tín chỉ lý thuyết/thực hành phải hợp lệ và tổng tín chỉ phải lớn hơn 0.','Theory/lab credits must be valid and total credits must be greater than 0.'),
  @('Đề cương chưa xác định giảng viên biên soạn.','The syllabus does not specify the preparing instructor.'),
  @('Năm học/khóa áp dụng là bắt buộc.','Academic year / applicable cohort is required.'),
  @('Học kỳ là bắt buộc.','Semester is required.'),
  @('Nhãn phiên bản là bắt buộc.','Version label is required.'),
  @('Chuyên ngành áp dụng là bắt buộc.','Applicable major is required.'),
  @('Course Designation là bắt buộc.','Course Designation is required.'),
  @('Phải chọn ít nhất một loại môn học.','At least one course type must be selected.'),
  @('Ngôn ngữ giảng dạy là bắt buộc.','Language of instruction is required.'),
  @('Quan hệ của môn học trong chương trình đào tạo là bắt buộc. Có thể nhập ''Không có''.','Curriculum relationship is required. Enter ''None'' if not applicable.'),
  @('Phương pháp giảng dạy là bắt buộc.','Teaching methods are required.'),
  @('Thông tin môn tiên quyết là bắt buộc. Có thể nhập ''Không có''.','Prerequisite information is required. Enter ''None'' if not applicable.'),
  @('Mục tiêu môn học là bắt buộc.','Course objectives are required.'),
  @('Hình thức đánh giá/thi là bắt buộc.','Assessment / examination forms are required.'),
  @('Yêu cầu học tập và thi là bắt buộc.','Learning and examination requirements are required.'),
  @('Học kỳ phải có dạng HK1–HK8, Semester 1–8 hoặc Summer.','Semester must be in the format HK1–HK8, Semester 1–8, or Summer.'),
  @('Đề cương phải có ít nhất một CLO.','The syllabus must contain at least one CLO.'),
  @('CLO thứ ','CLO #'),
  @(' không hợp lệ.',' is invalid.'),
  @(' chưa có mã CLO.',' does not have a CLO code.'),
  @('Mã CLO ''','CLO code '' '),
  @(''' bị trùng.',''' is duplicated.'),
  @(''' phải có dạng ',''' must follow the format '),
  @(' chưa có mô tả chuẩn đầu ra.',' does not have an outcome description.'),
  @(' chưa chọn mức Bloom.',' does not have a Bloom level.'),
  @(' chưa chọn loại năng lực.',' does not have a competency type.'),
  @(' chưa được liên kết ',' is not mapped '),
  @('với ít nhất một PLO.','to at least one PLO.'),
  @(' có một mapping CLO–PLO ',' has an invalid CLO–PLO mapping '),
  @('không hợp lệ.','.'),
  @(' có mapping chưa xác định PLO.',' has a mapping without a PLO.'),
  @(' bị liên kết trùng với ',' has a duplicate mapping to '),
  @('Mapping từ ','Mapping from '),
  @(' đến ',' to '),
  @(' chưa chọn mức I, D hoặc A.',' does not specify I, D, or A.'),
  @('Trọng số đóng góp từ ','Contribution weight from '),
  @(' phải lớn hơn 0 và ',' must be greater than 0 and '),
  @('không vượt quá 100%.','must not exceed 100%.'),
  @('Đề cương phải có ít nhất một nội dung giảng dạy.','The syllabus must contain at least one teaching topic.'),
  @('Nội dung thứ ','Topic #'),
  @(' chưa có tên nội dung.',' does not have a topic name.'),
  @(' phải có tuần học từ 1 đến 52.',' must have a week number from 1 to 52.'),
  @(' phải có thứ tự trong tuần lớn hơn 0.',' must have an order within the week greater than 0.'),
  @('Tuần ','Week '),
  @(' đang có hai nội dung cùng thứ tự ',' contains two topics with the same order '),
  @(' phải có ít nhất một giờ học hoặc tự học.',' must have at least one teaching or self-study hour.'),
  @(' chưa chọn loại nội dung.',' does not specify a topic type.'),
  @('với ít nhất một CLO.','to at least one CLO.'),
  @(' có một mapping Topic–CLO không hợp lệ.',' has an invalid Topic–CLO mapping.'),
  @(' có mapping chưa xác định CLO.',' has a mapping without a CLO.'),
  @(' đang liên kết với CLO thuộc ',' is mapped to a CLO belonging to '),
  @('một đề cương khác.','another syllabus.'),
  @(' chưa chọn Teaching Level.',' does not specify a Teaching Level.'),
  @(' có số giờ không hợp lệ.',' has invalid hours.'),
  @('Đề cương phải có ít nhất một thành phần đánh giá.','The syllabus must contain at least one assessment component.'),
  @('Thành phần đánh giá thứ ','Assessment component #'),
  @(' chưa có tên.',' does not have a name.'),
  @(' chưa chọn loại đánh giá.',' does not specify an assessment type.'),
  @(' phải có trọng số lớn hơn 0 và không vượt quá 100%.',' must have a weight greater than 0 and not exceeding 100%.'),
  @(' phải có khoảng điểm hợp lệ: 0 ≤ điểm tối thiểu < điểm tối đa ≤ 100.',' must have a valid score range: 0 ≤ minimum score < maximum score ≤ 100.'),
  @('Tổng trọng số các thành phần đánh giá phải đúng 100%. Hiện tại: ','The total assessment weight must equal 100%. Current total: '),
  @(' có một mapping Assessment–CLO ',' has an invalid Assessment–CLO mapping '),
  @('Tỷ lệ đóng góp từ ','Contribution percentage from '),
  @('Đề cương phải có ít nhất ','The syllabus must contain at least '),
  @('một tài liệu tham khảo.','one reading resource.'),
  @('Tài liệu thứ ','Resource #'),
  @(' chưa liên kết với ',' is not linked to '),
  @('thông tin sách/tài liệu.','book/resource information.'),
  @(' chưa có tên ',' does not have a '),
  @('sách/tài liệu.','book/resource title.'),
  @(' chưa có tác giả.',' does not have an author.'),
  @(' chưa có năm xuất bản ',' does not have a valid publication year '),
  @('hợp lệ.','.'),
  @(' chưa có loại sử dụng.',' does not specify a usage type.'),
  @('Total Workload phải là số lớn hơn 0.','Total Workload must be greater than 0.'),
  @('Contact Hours phải là số không âm.','Contact Hours must be non-negative.'),
  @('Private Study phải là số không âm.','Private Study must be non-negative.'),
  @('Total Workload phải bằng Contact Hours + Private Study. Hiện tại: ','Total Workload must equal Contact Hours + Private Study. Current values: '),
  @('Contact Hours phải bằng tổng Teaching Hours + Lab Hours trong Topics. Khai báo: ','Contact Hours must equal total Teaching Hours + Lab Hours in Topics. Declared: '),
  @('Private Study phải bằng tổng Self-study Hours trong Topics. Khai báo: ','Private Study must equal total Self-study Hours in Topics. Declared: '),
  @('Total Workload phải bằng tổng toàn bộ giờ trong Topics. Khai báo: ','Total Workload must equal the total hours in Topics. Declared: '),
  @('CLO chưa xác định','Unspecified CLO'),
  @('PLO chưa xác định','Unspecified PLO'),
  @('Đề cương đã đáp ứng đầy đủ điều kiện để nộp.','The syllabus meets all submission requirements.'),
  @('Đề cương còn ','The syllabus has '),
  @(' lỗi cần sửa trước khi nộp.',' errors that must be fixed before submission.')
)

Update-TextFile $backRoot 'syllabus\service\SyllabusAccessService.java' @(
  @('"Chỉ giảng viên được phân công hoặc Admin "','"Only an assigned instructor or Admin "'),
  @('"You are not assigned to học kỳ đích đã chọn."','"You are not assigned to the selected target semester."'),
  @('"The account is not linked với hồ sơ giảng viên/bộ môn."','"The account is not linked to an instructor/department profile."')
)

Update-TextFile $backRoot 'syllabus\service\SyllabusSubmissionValidationService.java' @(
  @('"CLO code '' " + clo.getCode()','"CLO code ''" + clo.getCode()'),
  @('"Mã ''" + clo.getCode()','"Code ''" + clo.getCode()'),
  @('label + " phải có tuần học từ 1 to 52."','label + " must have a week number from 1 to 52."'),
  @('topicLabel
                        + " có một mapping Topic–CLO is invalid."','topicLabel
                        + " has an invalid Topic–CLO mapping."'),
  @('label + " có số giờ is invalid."','label + " has invalid hours."'),
  @('label + " phải có trọng số lớn hơn 0 và must not exceed 100%."','label + " must have a weight greater than 0 and must not exceed 100%."')
)

Write-Host ''
Write-Host 'Batch 4 translation completed.' -ForegroundColor Cyan
Write-Host 'No business logic, routes, repositories, or database schema were changed.' -ForegroundColor Cyan
Write-Host ('Backup folder: ' + $backupRoot) -ForegroundColor Cyan