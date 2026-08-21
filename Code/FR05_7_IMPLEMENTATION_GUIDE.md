# FR-05.7 — Escalation khi quá hạn nộp đề cương

## 1. Phạm vi đã triển khai

Khi một deadline đang hoạt động đã qua thời điểm `deadline_at`, hệ thống tự động:

1. Lấy toàn bộ `class_section` đang hoạt động của đúng năm học và học kỳ.
2. Xác định các môn chưa có đề cương ở trạng thái `SUBMITTED`, `UNDER_REVIEW` hoặc `APPROVED`.
3. Gom dữ liệu theo giảng viên và bộ môn của môn học.
4. Gửi cho từng `DEPT_HEAD` đúng phạm vi bộ môn mà họ quản lý.
5. Gửi cho toàn bộ `DEAN` đang hoạt động với phạm vi toàn khoa.
6. Tạo notification trong hệ thống và đưa email vào transactional outbox.
7. Lưu snapshot audit log để truy vết và khóa gửi trùng.

## 2. Quy tắc nghiệp vụ chính

- Mốc escalation được cấu hình theo số ngày sau hạn, ví dụ `0,1,3,7,14`.
- Mốc `0` được gửi ngay sau khi thời điểm deadline vừa qua.
- Scheduler mặc định chạy mỗi giờ tại phút 15, múi giờ `Asia/Ho_Chi_Minh`.
- Nếu server dừng ở đúng ngày của một mốc, lần chạy lại sẽ gửi bù mốc gần nhất đã đến hạn.
- Gửi thủ công dùng khóa riêng `-1`, không làm mất lượt gửi tự động.
- Một người nhận chỉ nhận một lần cho cùng `deadline + revision + scope + milestone`.
- Một người nhận lỗi không làm dừng những người nhận khác.
- Khi Admin thay đổi lịch hoặc cấu hình mốc, `revision` tăng để lịch mới được phép gửi hợp lệ.
- Trưởng bộ môn chỉ nhận dữ liệu thuộc bộ môn của họ; Trưởng khoa nhận toàn bộ khoa.

## 3. Migration database

Sao lưu database trước, sau đó chạy:

```powershell
Get-Content ".\database\fr05_7_deadline_escalation.sql" |
  docker exec -i curriculum_mysql mysql -uroot -proot curriculum_iu
```

Tên container, tài khoản và database cần thay theo môi trường thực tế.

Migration sẽ:

- Thêm `syllabus_deadline.escalation_days` nếu chưa tồn tại.
- Chuẩn hóa deadline cũ về mặc định `0,1,3,7,14`.
- Tạo bảng `syllabus_deadline_escalation_log`.
- Tạo unique constraint chống gửi trùng và các index phục vụ truy vấn.

## 4. Cấu hình backend

Các biến môi trường có thể dùng:

```properties
DEADLINE_ESCALATION_ENABLED=true
DEADLINE_ESCALATION_STARTUP_CHECK=true
DEADLINE_ESCALATION_ZONE=Asia/Ho_Chi_Minh
DEADLINE_ESCALATION_CRON=0 15 * * * *
```

`STARTUP_CHECK=true` giúp hệ thống kiểm tra và gửi bù ngay khi backend khởi động.

## 5. Dữ liệu bắt buộc để escalation đúng người

### Trưởng bộ môn

- `user_account.role = 'DEPT_HEAD'`
- `user_account.is_active = true`
- `user_account.instructor_id` phải trỏ tới một instructor đang hoạt động.
- Instructor đó phải có `department_id` đúng bộ môn quản lý.

### Trưởng khoa

- `user_account.role = 'DEAN'`
- `user_account.is_active = true`

### Phân công giảng dạy

- `class_section.is_active = true`
- `academic_year` và `semester` trùng deadline.
- Có `course_id`, `instructor_id` hợp lệ.
- Course cần được gán `department_id` để xác định đúng Trưởng bộ môn.

## 6. Giao diện quản trị

### Cấu hình deadline

Vào:

```text
/admin/settings
```

Admin cấu hình:

- Năm học
- Học kỳ
- Thời điểm deadline
- Mốc reminder trước hạn
- Mốc escalation sau hạn
- Trạng thái hoạt động

### Trung tâm Escalation

Vào:

```text
/admin/escalations
```

Trang cung cấp:

- Danh sách kỳ đang quá hạn.
- Tổng số giảng viên và môn chưa nộp.
- Phân tích theo từng bộ môn.
- Danh sách lãnh đạo sẽ nhận cảnh báo.
- Cảnh báo thiếu Trưởng bộ môn hoặc Trưởng khoa.
- Chạy kiểm tra mốc hiện tại.
- Gửi escalation thủ công.
- Nhật ký snapshot và trạng thái email outbox.

## 7. API mới

```http
GET  /api/admin/syllabus-deadlines/{id}/escalation-preview
POST /api/admin/syllabus-deadlines/{id}/escalate-now?force=false
POST /api/admin/syllabus-deadlines/{id}/escalate-now?force=true
GET  /api/admin/syllabus-deadlines/{id}/escalation-logs
```

Tất cả endpoint chỉ dành cho `ADMIN`.

## 8. Kịch bản kiểm thử chấp nhận

### TC-01 — Chưa quá hạn

- Deadline còn trong tương lai.
- Bấm chạy escalation.
- Kết quả: không gửi, thông báo deadline chưa quá hạn.

### TC-02 — Đúng phạm vi bộ môn

- CS và IT đều có giảng viên chưa nộp.
- Có Trưởng bộ môn CS, không có Trưởng bộ môn IT, có Trưởng khoa.
- Kết quả:
  - Trưởng bộ môn CS chỉ nhận dữ liệu CS.
  - Trưởng khoa nhận CS + IT.
  - UI cảnh báo IT chưa có Trưởng bộ môn.

### TC-03 — Đề cương đã nộp

- Syllabus có trạng thái `SUBMITTED`, `UNDER_REVIEW` hoặc `APPROVED`.
- Kết quả: không xuất hiện trong danh sách quá hạn.

### TC-04 — Chống gửi trùng

- Chạy cùng một mốc hai lần.
- Kết quả: lần đầu tạo notification/email; lần sau ghi nhận `skippedAlreadySent`.

### TC-05 — Gửi bù

- Mốc cấu hình `0,1,3,7`.
- Server khởi động khi deadline đã quá 2 ngày.
- Kết quả: gửi mốc ngày 1; không bỏ lỡ hoàn toàn.

### TC-06 — Lỗi một người nhận

- Giả lập một delivery lỗi.
- Kết quả: những recipient còn lại vẫn được xử lý; scheduler có thể thử lại recipient lỗi.

### TC-07 — Audit log

- Sau khi gửi, kiểm tra bảng `syllabus_deadline_escalation_log`.
- Kết quả: có snapshot recipient, role, scope, revision, mốc, số ngày quá hạn, giảng viên, môn học, notification và email status.

## 9. Kiểm thử nhanh trên môi trường development

UI không cho tạo một deadline mới trong quá khứ. Để kiểm thử riêng trên database development, có thể tạm cập nhật một deadline hiện có:

```sql
UPDATE syllabus_deadline
SET deadline_at = DATE_SUB(NOW(), INTERVAL 3 DAY),
    is_active = TRUE,
    revision = revision + 1
WHERE id = <DEADLINE_ID>;
```

Sau đó mở `/admin/escalations`, chọn deadline và bấm **Chạy mốc hiện tại** hoặc **Escalate thủ công**.

Không dùng câu lệnh này trực tiếp trên production nếu không có phê duyệt nghiệp vụ.

## 10. Kiểm tra build

Frontend:

```powershell
cd frontend
npm install
npm run build
```

Backend:

```powershell
cd backend\curriculum
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Nếu sao chép project giữa Windows và Linux, cần xóa `frontend/node_modules` rồi cài lại để native package đúng hệ điều hành.
