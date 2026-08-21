# FR-05.7 Validation Report

## Đã kiểm tra thành công

- `npx tsc -b --pretty false`: thành công.
- ESLint riêng cho toàn bộ file frontend đã thay đổi trong FR-05.7: thành công, 0 lỗi và 0 cảnh báo.
- Java 21 type-check cho toàn bộ lớp backend production đã thay đổi/thêm mới: thành công.
  - Sử dụng dependency từ Spring Boot JAR hiện có trong `target`.
  - Dùng compile harness tạm thời để thay thế code Lombok sinh tự động; harness không nằm trong source bàn giao.
- Migration MySQL được viết idempotent: kiểm tra cột bằng `information_schema`, dùng `CREATE TABLE IF NOT EXISTS`, có unique key và index.
- Email HTML đã escape dữ liệu course/instructor/department trước khi render.

## Unit test đã bổ sung

- `DeadlineEscalationTargetResolverTest`
- `DeadlineEscalationServiceTest`
- `DeadlineEscalationDeliveryServiceTest`

Các test bao phủ:

- Scope riêng của Trưởng bộ môn và scope toàn khoa của Trưởng khoa.
- Loại trừ đề cương đã nộp/đang duyệt/đã duyệt.
- Cảnh báo thiếu lãnh đạo.
- Deduplicate tài khoản lãnh đạo.
- Deadline chưa quá hạn hoặc đã tắt.
- Gửi bù mốc gần nhất.
- Khóa riêng cho gửi thủ công.
- Cô lập lỗi theo từng recipient.
- Unique claim chống gửi trùng.
- Tạo notification, email outbox và finalize audit log.

## Giới hạn môi trường kiểm tra

- Maven Wrapper không thể tải Maven 3.9.16 vì container không có kết nối ra Maven Central, nên chưa chạy được `mvn test` đầy đủ trong container này.
- Vite bundling không chạy được trên thư mục `node_modules` được sao chép từ Windows vì thiếu native Rolldown binding cho Linux. TypeScript và ESLint của các file liên quan đều đã thành công.

## Lệnh xác nhận trên máy Windows dự án

```powershell
cd backend\curriculum
.\mvnw.cmd clean test

cd ..\..\frontend
Remove-Item -Recurse -Force node_modules -ErrorAction SilentlyContinue
npm install
npm run lint
npm run build
```
