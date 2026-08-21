# Database Design Report
## Curriculum Management & Compliance Dashboard — SCSE, International University VNU-HCM

**Stack:** Spring Boot · MySQL 8 · React  
**Accreditation standard:** ASIIN (CLO-PLO level: I / D / A)  
**Version:** 2.0 (thiết kế lại hoàn toàn từ schema cũ `digit_curriculum`)  
**Ngày hoàn thành:** 2025

---

## 1. Tổng quan thiết kế

### Vấn đề với schema cũ (`schema_old.sql`)

| Vấn đề | Mô tả |
|--------|-------|
| Không có Syllabus versioning | `learning_outcome` gắn trực tiếp vào `course` — cập nhật CLO là ghi đè lịch sử |
| Không có Cohort management | Chỉ có `student.batch` (int) — không track được chương trình theo năm tuyển sinh |
| Dual CLO system | Song song `learning_outcome` + `asiin_clo` — patch nhiều lần không refactor |
| CLO-PLO level vô nghĩa | Cột `level INT` không có constraint — không phân biệt I/D/A |
| Không có Approval workflow | Không có bảng nào track quá trình phê duyệt đề cương |
| Encoding hỗn độn | Mix `utf8`, `utf8mb4`, `latin1` trong cùng một database |

### Nguyên tắc thiết kế mới

1. **Course là entity bất biến** — thông tin metadata (tên, tín chỉ, mã) không thay đổi  
2. **Syllabus là entity versioned** — mỗi Course có nhiều Syllabus version, chỉ 1 `is_current = TRUE`  
3. **CLO thuộc Syllabus, không thuộc Course** — CLO thay đổi theo version  
4. **Program/Cohort tách biệt** — nhiều cohort song song theo từng Program  
5. **Toàn bộ `utf8mb4`** — hỗ trợ tiếng Việt đầy đủ  
6. **COMMENT tiếng Việt** — mọi bảng/cột có `COMMENT` song ngữ

---

## 2. Kiến trúc Schema — 11 Domain

```
DOMAIN 1: Identity & Auth
  user_account

DOMAIN 2: Tổ chức & Nhân sự
  department · instructor

DOMAIN 3: Ngành & Chương trình
  major · program_type · program · cohort

DOMAIN 4: Chuẩn đầu ra CTĐT (PLO)
  plo

DOMAIN 5: Học phần
  course · course_type · course_program · course_relationship

DOMAIN 6: Đề cương (versioned)
  syllabus

DOMAIN 7: Chuẩn đầu ra học phần & Ma trận CLO-PLO
  clo · clo_plo_mapping

DOMAIN 8: Nội dung đề cương
  topic · topic_clo · book · syllabus_book

DOMAIN 9: Kế hoạch đánh giá
  assessment_component · assessment_clo

DOMAIN 10: Quy trình phê duyệt (3 bước)
  approval_request

DOMAIN 11: Lớp học & Sinh viên
  class_section · student · enrollment · student_score · audit_log
```

---

## 3. Các thiết kế đặc trưng

### 3.1 Syllabus Versioning

```sql
syllabus (
  id, course_id,
  version_number   INT,          -- tăng tự động
  version_label    VARCHAR(50),  -- 'v2022.1', 'v2024.2'
  status           ENUM('DRAFT','SUBMITTED','UNDER_REVIEW',
                        'REVISION_REQUESTED','APPROVED',
                        'REJECTED','ARCHIVED'),
  is_current       BOOLEAN,      -- chỉ 1 bản TRUE mỗi course
  UNIQUE KEY uq_syllabus_version (course_id, version_number)
)
```

Khi approve version mới:
```sql
UPDATE syllabus SET is_current = FALSE WHERE course_id = ?;
UPDATE syllabus SET is_current = TRUE  WHERE id = ?;
```

### 3.2 CLO-PLO Mapping (ASIIN I/D/A)

```sql
clo_plo_mapping (
  clo_id, plo_id,
  level  ENUM('I','D','A'),  -- Introduce / Develop / Apply
  contribution_weight FLOAT  -- 0.0 – 1.0
)
```

Ma trận này là dữ liệu cốt lõi cho Compliance Dashboard — phục vụ báo cáo ASIIN.

### 3.3 Approval Workflow — 3 bước

```
STEP1_DEPT_HEAD  →  STEP2_PROG_COORDINATOR  →  STEP3_DEAN
   Trưởng BM           Điều phối CTĐT            Trưởng Khoa
```

```sql
approval_request (
  syllabus_id, step ENUM('STEP1_DEPT_HEAD','STEP2_PROG_COORDINATOR','STEP3_DEAN'),
  status ENUM('PENDING','APPROVED','REJECTED','REVISION_REQUESTED'),
  reviewed_by, comment, created_at, resolved_at
)
```

### 3.4 Cohort — Nhiều khóa song song

```sql
cohort (program_id, entry_year, name)
-- CS2021, CS2022, IT2021, DS2021...

course_program (
  course_id, program_id,
  cohort_id        INT NULL,     -- NULL = áp dụng cho tất cả khóa
  semester_suggest INT,          -- 1–8
  year_suggest     INT,          -- 1–4
  is_required      BOOLEAN
)
```

---

## 4. Seed Data — Parse từ Program Specification PDF

### Thống kê dữ liệu thực

| Bảng | Rows | Nguồn |
|------|-----:|-------|
| `program` | 3 | CS-2021, IT-2021, DS-2021 |
| `plo` | 18 | 6 PLO × 3 programs |
| `course` | 110 | Unique, deduped từ 3 programs |
| `course_program` | 192 | Với `semester_suggest` và `year_suggest` |
| `syllabus` | 38 | 1 per course (best version) |
| `clo` | 139 | CLO 1–5 per syllabus |
| `clo_plo_mapping` | 64 | Ma trận CLO-PLO, level I/D/A |
| `assessment_component` | 125 | Quiz/Mid/Final với % |
| `assessment_clo` | 147 | Assessment → CLO contribution |
| `topic` | 481 | Kế hoạch tuần (weeks 1–16) |
| `topic_clo` | 371 | CLO phân phối theo tuần |
| `book` | 100 | Reading list deduped |
| **TOTAL** | **~1,900** | |

### Compliance Dashboard Output

| Program | Total Courses | Syllabi đã duyệt | Tỷ lệ |
|---------|----------:|----------:|------:|
| CS-2021 | 56 | 29 | 51.8% |
| IT-2021 | 84 | 29 | 34.5% |
| DS-2021 | 52 | 19 | 36.5% |

---

## 5. Views phân tích

| View | Mục đích |
|------|---------|
| `v_clo_plo_matrix` | Ma trận CLO-PLO đầy đủ — vẽ heatmap trên dashboard |
| `v_syllabus_approval_status` | Trạng thái phê duyệt từng đề cương qua 3 bước |
| `v_clo_coverage` | CLO được dạy bao nhiêu tuần, ở mức I/D/A nào |
| `v_program_compliance_summary` | % đề cương đã duyệt per program — KPI dashboard |

---

## 6. Cách triển khai

### Chạy lần đầu (fresh install)
```bash
mysql -u root -p -e "CREATE DATABASE curriculum_iu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p curriculum_iu < schema_ddl.sql   # Tạo 28 tables
mysql -u root -p curriculum_iu < seed_data.sql    # Insert ~1,900 rows
mysql -u root -p curriculum_iu < views.sql        # Tạo 4 analytical views
```

### Reset và chạy lại
```bash
mysql -u root -p -e "DROP DATABASE IF EXISTS curriculum_iu; CREATE DATABASE curriculum_iu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
# Sau đó chạy lại 3 lệnh trên
```

> ⚠️ **Lưu ý:** Không chạy `seed_data.sql` hai lần trên cùng một database — sẽ gây lỗi duplicate primary key.  
> ⚠️ Password hash trong `user_account` là placeholder — cần thay bằng BCrypt hash thực khi deploy.

### Spring Boot configuration
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/curriculum_iu?useUnicode=true&characterEncoding=utf8mb4
    username: root
    password: your_password
  jpa:
    hibernate:
      ddl-auto: validate   # KHÔNG dùng create/update — đã có schema_ddl.sql
    properties:
      hibernate.dialect: org.hibernate.dialect.MySQL8Dialect
```

---

## 7. Các lỗi gặp phải và bài học

| Lỗi | Nguyên nhân | Fix |
|-----|------------|-----|
| Error 1048 `name_vn cannot be null` | `course.name_vn NOT NULL` nhưng parser trả về NULL cho 110 courses | Build map 80+ tên VN + fallback tên EN |
| Error 1062 duplicate syllabus | 1 course xuất hiện ở CS + IT + DS → 3 syllabus cùng `(course_id, version=1)` | Dedup: 1 syllabus per course (lấy version giàu data nhất) |
| Error 1062 duplicate CLO PRIMARY | `seed_data.sql` bị chạy lần 2 trên DB đã có data | Drop + recreate DB trước mỗi lần seed |
| Error 1064 syntax near `', 'KNOWLEDGE'` | CLO description có text artifact từ PDF parser bị lẫn vào; `re.sub` cắt sai vị trí khi description chứa `;` | Clean artifact + rebuild file từ split-point cố định thay vì regex replace |
| Assessment weight ≠ 100% | PDF chỉ in một số thành phần đánh giá | Bổ sung 21 rows cho 11 môn để đủ 100% |

