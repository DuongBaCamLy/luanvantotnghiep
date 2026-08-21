# FR-05.6 — Deadline theo học kỳ và nhắc trước hạn

## 1. Phạm vi đã triển khai

FR-05.6 được triển khai theo đúng yêu cầu: Admin cấu hình deadline nộp syllabus theo **năm học + học kỳ**, hệ thống tự xác định giảng viên còn thiếu đề cương và nhắc trước hạn.

Luồng hoàn chỉnh:

1. Admin tạo một cấu hình deadline cho từng cặp `academicYear + semester`.
2. Admin cấu hình các mốc nhắc, ví dụ `14, 7, 3, 1, 0` ngày.
3. Scheduler chạy mỗi ngày lúc 08:00 theo `Asia/Ho_Chi_Minh` và một lần khi backend khởi động.
4. Hệ thống lấy các `ClassSection` đang hoạt động của đúng năm học/học kỳ.
5. Hệ thống gom theo giảng viên và môn học, chỉ giữ các môn chưa có syllabus ở trạng thái đã nộp.
6. Mỗi người nhận được tạo:
   - thông báo trong ứng dụng;
   - email trong transactional outbox;
   - reminder log để chống gửi trùng.
7. Dashboard giảng viên hiển thị deadline, số ngày còn lại và trạng thái của từng môn.

## 2. Quy tắc nghiệp vụ chính

### 2.1 Một deadline cho một học kỳ

Khóa duy nhất:

```text
academic_year + semester
```

Không thể tạo hai deadline cho cùng một năm học và học kỳ.

### 2.2 Trạng thái được xem là đã nộp

Một môn không bị nhắc khi có ít nhất một syllabus liên kết với phân công trong đúng học kỳ ở một trong các trạng thái:

```text
SUBMITTED
UNDER_REVIEW
APPROVED
```

Các trạng thái như chưa tạo, `DRAFT`, `REVISION_REQUESTED`, `REJECTED` vẫn được xem là chưa hoàn thành deadline.

### 2.3 Nhắc bù sau downtime

Ví dụ cấu hình `14, 7, 3, 1, 0` ngày. Nếu server không chạy ở mốc 7 ngày và khởi động lại khi còn 6 ngày, hệ thống gửi bù mốc 7 ngày. Khóa chống trùng bảo đảm mốc đó không bị gửi lại ở các lần chạy tiếp theo.

### 2.4 Chống gửi trùng

Khóa idempotency:

```text
deadline_id + deadline_revision + recipient_user_id + days_before
```

`INSERT IGNORE` được dùng để claim lượt gửi ở mức database, nên hai scheduler hoặc hai node chạy đồng thời vẫn không gửi trùng.

### 2.5 Revision

`revision` tăng khi Admin thay đổi:

- năm học;
- học kỳ;
- thời điểm deadline;
- các mốc nhắc;
- trạng thái bật/tắt.

Lịch mới có thể gửi reminder hợp lệ mà không xung đột log của lịch cũ.

### 2.6 Gửi thử thủ công

Nút **Gửi thử ngay** dùng `days_before = -1`, tách khỏi các mốc scheduler `0..60`. Vì vậy việc Admin kiểm thử đúng vào mốc 7 ngày hoặc 1 ngày không làm mất lượt nhắc tự động.

Một lượt gửi thử chỉ được tạo một lần cho mỗi `deadline revision + người nhận`. Muốn thử lại, Admin có thể cập nhật lịch để tạo revision mới.

### 2.7 Ranh giới với FR-05.7

FR-05.6 chỉ gửi trước hạn hoặc đúng thời điểm deadline. Sau hạn, chức năng không tiếp tục gửi reminder; escalation lên Trưởng bộ môn/Dean thuộc FR-05.7.

## 3. Database

Chạy file:

```text
database/fr05_6_syllabus_deadlines.sql
```

File tạo hai bảng:

- `syllabus_deadline`;
- `syllabus_deadline_reminder_log`.

Trước khi chạy trên dữ liệu thật, nên backup database.

Cấu hình `SYLLABUS_DEADLINE` cũ trong `system_config` không thể tự động chuyển đổi vì không chứa năm học và học kỳ. Sau migration, tạo lại deadline tại `/admin/settings`.

## 4. Cấu hình môi trường

Các biến mới:

```env
DEADLINE_REMINDER_ENABLED=true
DEADLINE_REMINDER_STARTUP_CHECK=true
DEADLINE_REMINDER_ZONE=Asia/Ho_Chi_Minh
DEADLINE_REMINDER_CRON=0 0 8 * * *
```

Ý nghĩa:

- `ENABLED`: bật/tắt scheduler toàn hệ thống;
- `STARTUP_CHECK`: chạy kiểm tra ngay khi backend khởi động;
- `ZONE`: múi giờ dùng để tính ngày còn lại;
- `CRON`: lịch chạy scheduler.

Email dùng cấu hình transactional outbox hiện có. Với local development, Mailpit mặc định ở SMTP port `1025` và giao diện port `8025`.

## 5. API Admin

Base path:

```text
/api/admin/syllabus-deadlines
```

| Method | Endpoint | Mục đích |
|---|---|---|
| GET | `/` | Danh sách deadline |
| GET | `/{id}` | Chi tiết deadline |
| POST | `/` | Tạo deadline |
| PUT | `/{id}` | Cập nhật deadline |
| PATCH | `/{id}/active?active=true|false` | Bật/tắt lịch |
| GET | `/{id}/preview` | Xem trước người nhận và môn thiếu |
| POST | `/{id}/dispatch-now?force=false` | Chạy kiểm tra mốc hiện tại |
| POST | `/{id}/dispatch-now?force=true` | Gửi thử thủ công |
| GET | `/{id}/logs` | Xem 100 log gần nhất |

Tất cả API trên yêu cầu role `ADMIN`.

Ví dụ request tạo deadline:

```json
{
  "academicYear": "2026-2027",
  "semester": 1,
  "deadlineAt": "2026-09-15T23:59:00",
  "reminderDays": [14, 7, 3, 1, 0],
  "active": true
}
```

## 6. Giao diện

### Admin

Mở:

```text
/admin/settings
```

Admin có thể:

- tạo/cập nhật deadline;
- bật/tắt scheduler của từng học kỳ;
- xem trước người nhận;
- xem danh sách môn còn thiếu;
- chạy kiểm tra mốc hiện tại;
- gửi thử;
- xem reminder log và trạng thái email outbox.

### Giảng viên

Dashboard hiển thị:

- năm học và học kỳ;
- thời điểm deadline;
- số ngày còn lại;
- `NOT_CONFIGURED`, `UPCOMING`, `DUE_TODAY`, `OVERDUE`, hoặc `SUBMITTED`.

Thông báo deadline trên Header dẫn tới:

```text
/instructor/syllabus
```

## 7. Cách chạy

### Backend

Windows PowerShell:

```powershell
cd backend\curriculum
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Hoặc nếu máy đã cài Maven:

```powershell
mvn clean test
mvn spring-boot:run
```

### Frontend

Do `node_modules` phụ thuộc hệ điều hành, nên cài lại dependency trên đúng máy chạy:

```powershell
cd frontend
Remove-Item -Recurse -Force node_modules -ErrorAction SilentlyContinue
Remove-Item package-lock.json -ErrorAction SilentlyContinue
npm install
npm run build
npm run dev
```

Không cần xóa `package-lock.json` nếu lockfile hiện tại đã được quản lý ổn định và cài được bằng `npm ci`; khi đó ưu tiên:

```powershell
npm ci
npm run build
```

## 8. Kịch bản nghiệm thu

### TC-01 — Tạo deadline hợp lệ

- Tạo `2026-2027`, học kỳ 1, deadline tương lai.
- Kỳ vọng: lưu thành công, revision = 1.

### TC-02 — Trùng học kỳ

- Tạo lần hai cùng năm học và học kỳ.
- Kỳ vọng: HTTP 409, không tạo bản ghi trùng.

### TC-03 — Xem trước người nhận

- Có giảng viên được phân công 2 môn, một môn đã `SUBMITTED`, một môn `DRAFT`.
- Kỳ vọng: preview chỉ hiển thị môn `DRAFT`.

### TC-04 — Đúng mốc nhắc

- Deadline còn đúng 7 ngày, cấu hình có mốc 7.
- Kỳ vọng: tạo một notification, một email outbox và một log cho mỗi người nhận.

### TC-05 — Chống gửi trùng

- Chạy scheduler lại trong cùng revision/mốc.
- Kỳ vọng: không tạo thêm notification/email.

### TC-06 — Gửi bù

- Deadline còn 6 ngày, cấu hình có 7 và 3 ngày, mốc 7 chưa gửi.
- Kỳ vọng: gửi bù với khóa mốc 7.

### TC-07 — Gửi thử không chiếm mốc scheduler

- Khi còn đúng 7 ngày, bấm gửi thử rồi chạy scheduler.
- Kỳ vọng: log `-1` và log `7` đều tồn tại; scheduler vẫn gửi đúng mốc.

### TC-08 — Một người nhận lỗi

- Giả lập một transaction gửi lỗi.
- Kỳ vọng: các người nhận còn lại vẫn được xử lý; người lỗi có thể retry ở lần chạy sau.

### TC-09 — Deadline đã qua

- Chạy scheduler hoặc gửi thử sau deadline.
- Kỳ vọng: không gửi; response nêu rõ phần sau hạn thuộc FR-05.7.

### TC-10 — Người dùng thiếu email

- User account hợp lệ nhưng email trống.
- Kỳ vọng: notification trong app vẫn được tạo, `emailQueued = false`.

## 9. Kiểm thử tự động đã thêm

```text
DeadlineReminderTargetResolverTest
DeadlineReminderDeliveryServiceTest
SyllabusDeadlineServiceTest
```

Các test bao phủ:

- lọc đúng môn còn thiếu;
- bỏ qua Instructor không có user account;
- milestone chính xác và gửi bù;
- idempotency;
- manual key riêng;
- không gửi sau hạn;
- transaction độc lập cho từng người nhận;
- notification + email outbox + log.

## 10. Lưu ý triển khai production

- Chạy migration trước khi bật backend mới.
- Dùng một múi giờ thống nhất cho backend, database và vận hành.
- Không đặt nhiều cron ngoài hệ thống gọi cùng API nếu không cần; database vẫn chống trùng nhưng sẽ tạo tải thừa.
- Theo dõi log package `com.scse.curriculum.deadline` và bảng email outbox.
- Đảm bảo `ClassSection.academicYear` và `semester` được nhập đồng nhất với cấu hình deadline.
- FR-05.7 nên tái sử dụng bảng deadline và target resolver, nhưng cần log/escalation key riêng.
