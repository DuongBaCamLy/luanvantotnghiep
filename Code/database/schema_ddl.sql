-- ============================================================
-- schema_ddl.sql  —  DDL ONLY (tables + indexes + FK)
-- Run this FIRST before views.sql and seed_data.sql
-- ============================================================
-- =============================================================================
-- CURRICULUM MANAGEMENT & COMPLIANCE DASHBOARD
-- SCSE – International University, VNU-HCM
-- Accreditation: ASIIN (CLO-PLO level: I=Introduce / D=Develop / A=Apply)
-- Stack: Spring Boot + MySQL 8 + React
-- Encoding: utf8mb4 toàn bộ
-- Convention: snake_case tiếng Anh, COMMENT tiếng Việt đầy đủ
-- Author: Thesis Project — SCSE IU
-- Version: 2.0 (thiết kế lại từ schema_old.sql)
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO';

DROP DATABASE IF EXISTS curriculum_iu;
CREATE DATABASE curriculum_iu
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE curriculum_iu;

-- =============================================================================
-- DOMAIN 1: TỔ CHỨC & NHÂN SỰ
-- =============================================================================

CREATE TABLE department (
  id            INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã khoa/bộ môn',
  code          VARCHAR(20)  NOT NULL                COMMENT 'Mã viết tắt, VD: SCSE, MECH',
  name          VARCHAR(255) NOT NULL                COMMENT 'Tên khoa (tiếng Anh)',
  name_vn       VARCHAR(255) NOT NULL                COMMENT 'Tên khoa (tiếng Việt)',
  is_active     BOOLEAN      NOT NULL DEFAULT TRUE   COMMENT 'Còn hoạt động không',
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_department_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Khoa / Bộ môn trong trường';


CREATE TABLE instructor (
  id            INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã giảng viên (nội bộ)',
  staff_code    VARCHAR(50)  NOT NULL                COMMENT 'Mã cán bộ theo hệ thống nhà trường',
  full_name     VARCHAR(255) NOT NULL                COMMENT 'Họ và tên đầy đủ',
  email         VARCHAR(255) NOT NULL                COMMENT 'Email công tác',
  degree        VARCHAR(100)                         COMMENT 'Học vị: PhD, MSc, BSc...',
  academic_rank VARCHAR(100)                         COMMENT 'Chức danh: GS, PGS, Lecturer...',
  department_id INT          NOT NULL                COMMENT 'Thuộc khoa nào',
  is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_instructor_staff_code (staff_code),
  UNIQUE KEY uq_instructor_email (email),
  KEY idx_instructor_department (department_id),
  CONSTRAINT fk_instructor_department FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Giảng viên / Cán bộ giảng dạy';


CREATE TABLE user_account (
  id            INT           NOT NULL AUTO_INCREMENT COMMENT 'Mã tài khoản',
  username      VARCHAR(100)  NOT NULL                COMMENT 'Tên đăng nhập',
  email         VARCHAR(255)  NOT NULL                COMMENT 'Email đăng nhập',
  password_hash VARCHAR(255)  NOT NULL                COMMENT 'Mật khẩu đã mã hóa (BCrypt)',
  role          ENUM(
                  'ADMIN',              -- Quản trị hệ thống
                  'INSTRUCTOR',         -- Giảng viên (soạn đề cương)
                  'DEPT_HEAD',          -- Trưởng bộ môn (duyệt bước 1)
                  'PROGRAM_COORDINATOR',-- Điều phối chương trình (duyệt bước 2)
                  'DEAN',               -- Trưởng khoa (duyệt bước 3)
                  'STUDENT'             -- Sinh viên (xem)
                ) NOT NULL              COMMENT 'Vai trò trong hệ thống',
  instructor_id INT                     COMMENT 'Liên kết giảng viên (NULL nếu là sinh viên/admin)',
  is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
  last_login    TIMESTAMP               COMMENT 'Lần đăng nhập cuối',
  created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_user_username (username),
  UNIQUE KEY uq_user_email (email),
  KEY idx_user_instructor (instructor_id),
  CONSTRAINT fk_user_instructor FOREIGN KEY (instructor_id) REFERENCES instructor(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Tài khoản đăng nhập hệ thống';


-- =============================================================================
-- DOMAIN 2: NGÀNH & CHƯƠNG TRÌNH ĐÀO TẠO
-- =============================================================================

CREATE TABLE major (
  id        INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã ngành',
  code      VARCHAR(20)  NOT NULL                COMMENT 'Mã ngành: CS, IT, DS, CE...',
  name      VARCHAR(255) NOT NULL                COMMENT 'Tên ngành (tiếng Anh)',
  name_vn   VARCHAR(255) NOT NULL                COMMENT 'Tên ngành (tiếng Việt)',

  PRIMARY KEY (id),
  UNIQUE KEY uq_major_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Ngành đào tạo: CS / IT / DS / CE';


CREATE TABLE program_type (
  id          INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã loại chương trình',
  code        VARCHAR(50)  NOT NULL                COMMENT 'Mã: STANDARD, ADVANCED, HONORS...',
  name        VARCHAR(255) NOT NULL                COMMENT 'Tên loại chương trình',

  PRIMARY KEY (id),
  UNIQUE KEY uq_program_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Loại chương trình đào tạo';


CREATE TABLE program (
  id                  INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã chương trình đào tạo',
  code                VARCHAR(50)  NOT NULL                COMMENT 'Mã CTĐT, VD: CS-2021, IT-2023',
  name                VARCHAR(255) NOT NULL                COMMENT 'Tên CTĐT (tiếng Anh)',
  name_vn             VARCHAR(255) NOT NULL                COMMENT 'Tên CTĐT (tiếng Việt)',
  major_id            INT          NOT NULL                COMMENT 'Thuộc ngành nào',
  program_type_id     INT          NOT NULL                COMMENT 'Loại chương trình',
  department_id       INT          NOT NULL                COMMENT 'Khoa quản lý',
  accreditation_body  VARCHAR(100)                         COMMENT 'Tổ chức kiểm định: ASIIN, AUN-QA...',
  total_credits       INT                                  COMMENT 'Tổng số tín chỉ yêu cầu',
  duration_years      INT          NOT NULL DEFAULT 4      COMMENT 'Thời gian đào tạo (năm)',
  valid_from          DATE         NOT NULL                COMMENT 'Áp dụng từ năm học',
  valid_to            DATE                                 COMMENT 'Hết hiệu lực (NULL = còn hiệu lực)',
  is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_program_code (code),
  KEY idx_program_major (major_id),
  KEY idx_program_dept (department_id),
  CONSTRAINT fk_program_major FOREIGN KEY (major_id) REFERENCES major(id),
  CONSTRAINT fk_program_type FOREIGN KEY (program_type_id) REFERENCES program_type(id),
  CONSTRAINT fk_program_department FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Chương trình đào tạo — mỗi ngành có thể có nhiều CTĐT theo năm áp dụng';


CREATE TABLE cohort (
  id          INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã khóa tuyển sinh',
  program_id  INT          NOT NULL                COMMENT 'Theo CTĐT nào',
  entry_year  YEAR         NOT NULL                COMMENT 'Năm nhập học, VD: 2021',
  name        VARCHAR(100) NOT NULL                COMMENT 'Canonical cohort code, always CSYYYY (VD: CS2021)',
  description VARCHAR(500)                         COMMENT 'Ghi chú về khóa',
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,

  PRIMARY KEY (id),
  UNIQUE KEY uq_cohort_program_year (program_id, entry_year),
  KEY idx_cohort_program (program_id),
  CONSTRAINT fk_cohort_program FOREIGN KEY (program_id) REFERENCES program(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Khóa tuyển sinh — nhiều cohort chạy song song theo từng CTĐT';


-- =============================================================================
-- DOMAIN 3: CHUẨN ĐẦU RA CHƯƠNG TRÌNH (PLO / ELO)
-- Theo ASIIN: Program Learning Outcomes
-- =============================================================================

CREATE TABLE plo (
  id             INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã chuẩn đầu ra CTĐT',
  program_id     INT          NOT NULL                COMMENT 'Thuộc CTĐT nào',
  code           VARCHAR(20)  NOT NULL                COMMENT 'Mã PLO, VD: PLO1, PLO2...',
  description    TEXT         NOT NULL                COMMENT 'Mô tả chuẩn đầu ra (tiếng Anh)',
  description_vn TEXT                                 COMMENT 'Mô tả chuẩn đầu ra (tiếng Việt)',
  category       VARCHAR(100)                         COMMENT 'Nhóm PLO: Knowledge/Skill/Attitude (theo ASIIN)',
  version_number INT          NOT NULL DEFAULT 1      COMMENT 'Phiên bản PLO (khi CTĐT điều chỉnh)',
  is_active      BOOLEAN      NOT NULL DEFAULT TRUE   COMMENT 'PLO này còn áp dụng không',
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_plo_program_code_ver (program_id, code, version_number),
  KEY idx_plo_program (program_id),
  CONSTRAINT fk_plo_program FOREIGN KEY (program_id) REFERENCES program(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Chuẩn đầu ra chương trình đào tạo (PLO/ELO) — theo chuẩn ASIIN';


-- =============================================================================
-- DOMAIN 4: HỌC PHẦN
-- =============================================================================

CREATE TABLE course (
  id            INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã học phần (nội bộ DB)',
  course_code   VARCHAR(50)  NOT NULL                COMMENT 'Mã học phần chính thức, VD: IT093IU',
  name          VARCHAR(255) NOT NULL                COMMENT 'Tên học phần (tiếng Anh)',
  name_vn       VARCHAR(255) NOT NULL                COMMENT 'Tên học phần (tiếng Việt)',
  department_id INT          NOT NULL                COMMENT 'Khoa sở hữu học phần',
  credit_theory INT          NOT NULL DEFAULT 3      COMMENT 'Số tín chỉ lý thuyết',
  credit_lab    INT          NOT NULL DEFAULT 0      COMMENT 'Số tín chỉ thực hành',
  course_level  ENUM(
                  'UNDERGRADUATE',  -- Đại học
                  'GRADUATE'        -- Sau đại học
                ) NOT NULL DEFAULT 'UNDERGRADUATE'   COMMENT 'Bậc đào tạo',
  description   TEXT                                 COMMENT 'Mô tả tổng quan học phần',
  is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_course_code (course_code),
  KEY idx_course_department (department_id),
  CONSTRAINT fk_course_department FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Học phần — thông tin bất biến, không thay đổi theo semester';


CREATE TABLE course_type (
  id      INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã loại học phần',
  code    VARCHAR(50)  NOT NULL                COMMENT 'Mã: REQUIRED, ELECTIVE, GENERAL...',
  name    VARCHAR(255) NOT NULL                COMMENT 'Tên loại (tiếng Anh)',
  name_vn VARCHAR(255) NOT NULL                COMMENT 'Tên loại (tiếng Việt)',

  PRIMARY KEY (id),
  UNIQUE KEY uq_course_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Phân loại học phần trong chương trình: Bắt buộc / Tự chọn / Đại cương...';


CREATE TABLE course_program (
  id               INT  NOT NULL AUTO_INCREMENT COMMENT 'Mã phân công học phần vào CTĐT',
  course_id        INT  NOT NULL                COMMENT 'Học phần',
  program_id       INT  NOT NULL                COMMENT 'Chương trình đào tạo',
  cohort_id        INT                          COMMENT 'Áp dụng riêng cho khóa này (NULL = tất cả khóa)',
  course_type_id   INT  NOT NULL                COMMENT 'Loại học phần trong CTĐT này',
  semester_suggest INT                          COMMENT 'Học kỳ đề xuất (1-8)',
  year_suggest     INT                          COMMENT 'Năm học đề xuất (1-4)',
  is_required      BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Bắt buộc hay không',

  PRIMARY KEY (id),
  UNIQUE KEY uq_course_program_cohort (course_id, program_id, cohort_id),
  KEY idx_cp_course (course_id),
  KEY idx_cp_program (program_id),
  KEY idx_cp_cohort (cohort_id),
  CONSTRAINT fk_cp_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_cp_program FOREIGN KEY (program_id) REFERENCES program(id),
  CONSTRAINT fk_cp_cohort FOREIGN KEY (cohort_id) REFERENCES cohort(id) ON DELETE SET NULL,
  CONSTRAINT fk_cp_course_type FOREIGN KEY (course_type_id) REFERENCES course_type(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Phân công học phần vào chương trình đào tạo — hỗ trợ nhiều cohort song song';


CREATE TABLE course_relationship (
  id              INT  NOT NULL AUTO_INCREMENT COMMENT 'Mã quan hệ học phần',
  course_id       INT  NOT NULL                COMMENT 'Học phần chính',
  related_course_id INT NOT NULL               COMMENT 'Học phần liên quan',
  relation_type   ENUM(
                    'PREREQUISITE',   -- Học trước (bắt buộc pass)
                    'COREQUISITE',    -- Học song song
                    'RECOMMENDED',    -- Nên học trước (không bắt buộc)
                    'EQUIVALENT'      -- Học phần tương đương
                  ) NOT NULL          COMMENT 'Loại quan hệ giữa hai học phần',

  PRIMARY KEY (id),
  UNIQUE KEY uq_course_relationship (course_id, related_course_id, relation_type),
  KEY idx_cr_course (course_id),
  KEY idx_cr_related (related_course_id),
  CONSTRAINT fk_cr_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_cr_related FOREIGN KEY (related_course_id) REFERENCES course(id),
  CONSTRAINT chk_no_self_ref CHECK (course_id != related_course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Quan hệ giữa các học phần: tiên quyết / song hành / tương đương';


-- =============================================================================
-- DOMAIN 5: ĐỀ CƯƠNG MÔN HỌC (SYLLABUS VERSIONING)
-- Đây là lõi hệ thống — mỗi học phần có nhiều phiên bản đề cương
-- =============================================================================

CREATE TABLE syllabus (
  id             INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã đề cương',
  course_id      INT          NOT NULL                COMMENT 'Học phần',
  program_id     INT          NOT NULL                COMMENT 'Chương trình được chọn khi phân công',
  cohort_id      INT          NOT NULL                COMMENT 'Khóa tuyển sinh được chọn khi phân công',
  version_number INT          NOT NULL DEFAULT 1      COMMENT 'Số phiên bản (tăng tự động)',
  version_label  VARCHAR(50)                          COMMENT 'Nhãn phiên bản, VD: v1.0, v2.0',
  academic_year  VARCHAR(20)                          COMMENT 'Năm học áp dụng, VD: 2023-2024',
  status         ENUM(
                   'DRAFT',             -- Bản nháp (giảng viên đang soạn)
                   'SUBMITTED',         -- Đã nộp (chờ bộ môn duyệt)
                   'UNDER_REVIEW',      -- Đang xem xét
                   'REVISION_REQUESTED',-- Yêu cầu chỉnh sửa
                   'APPROVED',          -- Đã duyệt hoàn toàn
                   'REJECTED',          -- Bị từ chối
                   'ARCHIVED'           -- Lưu trữ (không còn dùng)
                 ) NOT NULL DEFAULT 'DRAFT' COMMENT 'Trạng thái đề cương trong quy trình',
  is_current     BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT 'Là phiên bản hiện hành không (chỉ 1 bản TRUE mỗi học phần)',
  created_by     INT          NOT NULL                COMMENT 'Giảng viên soạn đề cương',
  approved_by    INT                                  COMMENT 'Người duyệt cuối (Trưởng khoa)',
  submitted_at   TIMESTAMP                            COMMENT 'Thời điểm nộp lên',
  approved_at    TIMESTAMP                            COMMENT 'Thời điểm được duyệt',
  change_summary TEXT                                 COMMENT 'Tóm tắt thay đổi so với phiên bản trước',
  notes          TEXT                                 COMMENT 'Ghi chú nội bộ',
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_syllabus_version (course_id, version_number),
  KEY idx_syllabus_course (course_id),
  KEY idx_syllabus_created_by (created_by),
  KEY idx_syllabus_status (status),
  CONSTRAINT fk_syllabus_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_syllabus_created_by FOREIGN KEY (created_by) REFERENCES user_account(id),
  CONSTRAINT fk_syllabus_approved_by FOREIGN KEY (approved_by) REFERENCES user_account(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Đề cương môn học — có versioning đầy đủ, mỗi học phần có nhiều phiên bản';


-- =============================================================================
-- DOMAIN 6: CHUẨN ĐẦU RA HỌC PHẦN (CLO) & MA TRẬN CLO-PLO
-- Theo ASIIN: Course Learning Outcomes
-- Level: I=Introduce, D=Develop, A=Apply
-- =============================================================================

CREATE TABLE clo (
  id             INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã chuẩn đầu ra học phần',
  syllabus_id    INT          NOT NULL                COMMENT 'Thuộc đề cương nào',
  code           VARCHAR(20)  NOT NULL                COMMENT 'Mã CLO, VD: CLO1, CLO2...',
  description    TEXT         NOT NULL                COMMENT 'Mô tả chuẩn đầu ra (tiếng Anh)',
  description_vn TEXT                                 COMMENT 'Mô tả chuẩn đầu ra (tiếng Việt)',
  competency_level ENUM(
                    'KNOWLEDGE',   -- Kiến thức
                    'SKILL',       -- Kỹ năng
                    'ATTITUDE'     -- Thái độ (KSA taxonomy)
                  )                                   COMMENT 'Nhóm năng lực: Knowledge/Skill/Attitude',
  bloom_level    ENUM(
                    'REMEMBER', 'UNDERSTAND', 'APPLY',
                    'ANALYZE', 'EVALUATE', 'CREATE'
                  )                                   COMMENT 'Mức độ theo thang Bloom',
  order_index    INT          NOT NULL DEFAULT 1      COMMENT 'Thứ tự hiển thị trong đề cương',

  PRIMARY KEY (id),
  UNIQUE KEY uq_clo_syllabus_code (syllabus_id, code),
  KEY idx_clo_syllabus (syllabus_id),
  CONSTRAINT fk_clo_syllabus FOREIGN KEY (syllabus_id) REFERENCES syllabus(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Chuẩn đầu ra học phần (CLO) — gắn với từng phiên bản đề cương';


CREATE TABLE clo_plo_mapping (
  id                   INT   NOT NULL AUTO_INCREMENT COMMENT 'Mã ánh xạ CLO-PLO',
  clo_id               INT   NOT NULL                COMMENT 'Chuẩn đầu ra học phần',
  plo_id               INT   NOT NULL                COMMENT 'Chuẩn đầu ra chương trình',
  level                ENUM(
                         'I',  -- Introduce: Giới thiệu/Làm quen
                         'D',  -- Develop: Phát triển/Luyện tập
                         'A'   -- Apply: Áp dụng/Thành thạo
                       ) NOT NULL                    COMMENT 'Mức độ đóng góp theo ASIIN: I/D/A',
  contribution_weight  FLOAT NOT NULL DEFAULT 1.0    COMMENT 'Trọng số đóng góp (0.0-1.0, mặc định 1.0)',
  notes                TEXT                          COMMENT 'Ghi chú về mối liên hệ CLO-PLO',

  PRIMARY KEY (id),
  UNIQUE KEY uq_clo_plo (clo_id, plo_id),
  KEY idx_cpm_clo (clo_id),
  KEY idx_cpm_plo (plo_id),
  CONSTRAINT fk_cpm_clo FOREIGN KEY (clo_id) REFERENCES clo(id) ON DELETE CASCADE,
  CONSTRAINT fk_cpm_plo FOREIGN KEY (plo_id) REFERENCES plo(id),
  CONSTRAINT chk_cpm_weight CHECK (contribution_weight > 0 AND contribution_weight <= 1.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Ma trận CLO-PLO — mức độ theo ASIIN: I(Introduce)/D(Develop)/A(Apply)';


-- =============================================================================
-- DOMAIN 7: NỘI DUNG ĐỀ CƯƠNG
-- Chủ đề (Topic), Tài liệu, Phương pháp giảng dạy
-- =============================================================================

CREATE TABLE topic (
  id                INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã chủ đề/tuần học',
  syllabus_id       INT          NOT NULL                COMMENT 'Thuộc đề cương nào',
  week_number       INT          NOT NULL                COMMENT 'Tuần thứ mấy (1-16)',
  order_in_week     INT          NOT NULL DEFAULT 1      COMMENT 'Thứ tự trong tuần (nếu nhiều topic/tuần)',
  name              VARCHAR(500) NOT NULL                COMMENT 'Tên chủ đề (tiếng Anh)',
  name_vn           VARCHAR(500)                         COMMENT 'Tên chủ đề (tiếng Việt)',
  teaching_hours    INT          NOT NULL DEFAULT 3      COMMENT 'Số giờ lý thuyết',
  lab_hours         INT          NOT NULL DEFAULT 0      COMMENT 'Số giờ thực hành',
  self_study_hours  INT          NOT NULL DEFAULT 6      COMMENT 'Số giờ tự học',
  topic_type        ENUM(
                      'LECTURE',       -- Lý thuyết
                      'LAB',           -- Thực hành
                      'SEMINAR',       -- Seminar/Thảo luận
                      'EXAM',          -- Kiểm tra
                      'PROJECT',       -- Dự án
                      'SELF_STUDY'     -- Tự học
                    ) NOT NULL DEFAULT 'LECTURE'         COMMENT 'Loại hình dạy học',
  teaching_method   TEXT                                 COMMENT 'Phương pháp giảng dạy',
  learning_activity TEXT                                 COMMENT 'Hoạt động học tập của sinh viên',
  notes             TEXT                                 COMMENT 'Ghi chú thêm',

  PRIMARY KEY (id),
  KEY idx_topic_syllabus (syllabus_id),
  KEY idx_topic_week (syllabus_id, week_number),
  CONSTRAINT fk_topic_syllabus FOREIGN KEY (syllabus_id) REFERENCES syllabus(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Kế hoạch giảng dạy theo tuần — nội dung từng buổi học';


CREATE TABLE topic_clo (
  topic_id       INT  NOT NULL COMMENT 'Chủ đề/tuần học',
  clo_id         INT  NOT NULL COMMENT 'CLO được giảng dạy trong tuần này',
  teaching_level ENUM(
                   'I',  -- Introduce: Giới thiệu lần đầu
                   'D',  -- Develop: Phát triển thêm
                   'A'   -- Apply: Yêu cầu áp dụng thành thạo
                 ) NOT NULL DEFAULT 'I' COMMENT 'Mức độ giảng dạy CLO trong tuần này (ASIIN)',

  PRIMARY KEY (topic_id, clo_id),
  KEY idx_tc_clo (clo_id),
  CONSTRAINT fk_tc_topic FOREIGN KEY (topic_id) REFERENCES topic(id) ON DELETE CASCADE,
  CONSTRAINT fk_tc_clo FOREIGN KEY (clo_id) REFERENCES clo(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'CLO nào được dạy ở tuần nào, ở mức độ nào (I/D/A)';


CREATE TABLE book (
  id         INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã tài liệu',
  title      VARCHAR(500) NOT NULL                COMMENT 'Tên tài liệu/sách',
  author     VARCHAR(500)                         COMMENT 'Tác giả',
  publisher  VARCHAR(255)                         COMMENT 'Nhà xuất bản',
  year       INT                                  COMMENT 'Năm xuất bản',
  edition    VARCHAR(50)                          COMMENT 'Lần xuất bản/phiên bản',
  isbn       VARCHAR(30)                          COMMENT 'Mã ISBN',
  url        VARCHAR(1000)                        COMMENT 'Link truy cập (nếu có)',
  book_type  ENUM(
               'TEXTBOOK',    -- Giáo trình chính
               'REFERENCE',   -- Tài liệu tham khảo
               'SUPPLEMENTARY'-- Tài liệu bổ sung
             ) NOT NULL DEFAULT 'REFERENCE'       COMMENT 'Loại tài liệu',

  PRIMARY KEY (id),
  KEY idx_book_isbn (isbn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Tài liệu học tập / Danh mục sách tham khảo';


CREATE TABLE syllabus_book (
  syllabus_id  INT     NOT NULL COMMENT 'Đề cương',
  book_id      INT     NOT NULL COMMENT 'Tài liệu',
  usage_type   ENUM(
                 'REQUIRED',    -- Bắt buộc
                 'RECOMMENDED', -- Khuyến nghị
                 'SUPPLEMENTARY'-- Tham khảo thêm
               ) NOT NULL DEFAULT 'RECOMMENDED'  COMMENT 'Mức độ sử dụng tài liệu',
  order_index  INT NOT NULL DEFAULT 1             COMMENT 'Thứ tự trong danh mục tài liệu',

  PRIMARY KEY (syllabus_id, book_id),
  KEY idx_sb_book (book_id),
  CONSTRAINT fk_sb_syllabus FOREIGN KEY (syllabus_id) REFERENCES syllabus(id) ON DELETE CASCADE,
  CONSTRAINT fk_sb_book FOREIGN KEY (book_id) REFERENCES book(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Danh mục tài liệu sử dụng trong đề cương';


-- =============================================================================
-- DOMAIN 8: KẾ HOẠCH ĐÁNH GIÁ (ASSESSMENT SCHEME)
-- =============================================================================

CREATE TABLE assessment_component (
  id             INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã thành phần đánh giá',
  syllabus_id    INT          NOT NULL                COMMENT 'Thuộc đề cương nào',
  name           VARCHAR(255) NOT NULL                COMMENT 'Tên hình thức đánh giá (VD: Midterm Exam)',
  name_vn        VARCHAR(255)                         COMMENT 'Tên tiếng Việt (VD: Kiểm tra giữa kỳ)',
  assessment_type VARCHAR(100) NOT NULL COMMENT 'Loại hình đánh giá nhập theo syllabus nguồn',
  weight_percent  FLOAT        NOT NULL                COMMENT 'Trọng số phần trăm (tổng các thành phần = 100)',
  min_score       FLOAT        NOT NULL DEFAULT 0      COMMENT 'Điểm tối thiểu để qua môn thành phần này',
  max_score       FLOAT        NOT NULL DEFAULT 100    COMMENT 'Điểm tối đa',
  order_index     INT          NOT NULL DEFAULT 1      COMMENT 'Thứ tự trong kế hoạch đánh giá',

  PRIMARY KEY (id),
  KEY idx_ac_syllabus (syllabus_id),
  CONSTRAINT fk_ac_syllabus FOREIGN KEY (syllabus_id) REFERENCES syllabus(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Thành phần đánh giá trong đề cương — VD: Quiz 10%, Midterm 30%, Final 50%';


CREATE TABLE assessment_clo (
  assessment_component_id INT   NOT NULL COMMENT 'Thành phần đánh giá',
  clo_id                  INT   NOT NULL COMMENT 'CLO được đánh giá',
  contribution_percent    FLOAT NOT NULL DEFAULT 100.0 COMMENT 'Tỷ lệ đóng góp của hình thức này vào CLO (%)',

  PRIMARY KEY (assessment_component_id, clo_id),
  KEY idx_aclo_clo (clo_id),
  CONSTRAINT fk_aclo_component FOREIGN KEY (assessment_component_id) REFERENCES assessment_component(id) ON DELETE CASCADE,
  CONSTRAINT fk_aclo_clo FOREIGN KEY (clo_id) REFERENCES clo(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Ánh xạ hình thức đánh giá → CLO: hình thức nào đánh giá CLO nào (ma trận Assessment-CLO)';


-- =============================================================================
-- DOMAIN 9: QUY TRÌNH PHÊ DUYỆT ĐỀ CƯƠNG (3 BƯỚC)
-- Bước 1: Trưởng Bộ môn → Bước 2: Điều phối CTĐT → Bước 3: Trưởng Khoa
-- =============================================================================

CREATE TABLE approval_request (
  id            INT  NOT NULL AUTO_INCREMENT COMMENT 'Mã yêu cầu phê duyệt',
  syllabus_id   INT  NOT NULL                COMMENT 'Đề cương cần phê duyệt',
  step          ENUM(
                  'STEP1_DEPT_HEAD',        -- Bước 1: Trưởng Bộ môn xem xét
                  'STEP2_PROG_COORDINATOR', -- Bước 2: Điều phối CTĐT xem xét
                  'STEP3_DEAN'              -- Bước 3: Trưởng Khoa phê duyệt
                ) NOT NULL                  COMMENT 'Bước phê duyệt hiện tại',
  status        ENUM(
                  'PENDING',            -- Chờ xem xét
                  'APPROVED',           -- Đã phê duyệt bước này
                  'REJECTED',           -- Từ chối
                  'REVISION_REQUESTED'  -- Yêu cầu chỉnh sửa
                ) NOT NULL DEFAULT 'PENDING' COMMENT 'Kết quả phê duyệt bước này',
  requested_by  INT  NOT NULL                COMMENT 'Người gửi yêu cầu (giảng viên)',
  reviewed_by   INT                          COMMENT 'Người xem xét (NULL nếu chưa ai xem)',
  comment       TEXT                         COMMENT 'Nhận xét/Yêu cầu chỉnh sửa của người duyệt',
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm gửi yêu cầu',
  resolved_at   TIMESTAMP                    COMMENT 'Thời điểm có kết quả',

  PRIMARY KEY (id),
  KEY idx_ar_syllabus (syllabus_id),
  KEY idx_ar_status (status),
  KEY idx_ar_step (step),
  CONSTRAINT fk_ar_syllabus FOREIGN KEY (syllabus_id) REFERENCES syllabus(id),
  CONSTRAINT fk_ar_requested_by FOREIGN KEY (requested_by) REFERENCES user_account(id),
  CONSTRAINT fk_ar_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES user_account(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Nhật ký phê duyệt đề cương — workflow 3 bước: Bộ môn → CTĐT → Trưởng Khoa';


-- =============================================================================
-- DOMAIN 10: SINH VIÊN & LỚP HỌC
-- =============================================================================

CREATE TABLE student (
  id           INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã sinh viên (nội bộ DB)',
  student_code VARCHAR(20)  NOT NULL                COMMENT 'MSSV chính thức',
  full_name    VARCHAR(255) NOT NULL                COMMENT 'Họ và tên đầy đủ',
  email        VARCHAR(255) NOT NULL                COMMENT 'Email sinh viên',
  cohort_id    INT          NOT NULL                COMMENT 'Thuộc khóa nào',
  user_id      INT                                  COMMENT 'Tài khoản hệ thống (nếu có)',
  is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_student_code (student_code),
  KEY idx_student_cohort (cohort_id),
  CONSTRAINT fk_student_cohort FOREIGN KEY (cohort_id) REFERENCES cohort(id),
  CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Sinh viên — gắn với khóa (cohort) để track theo chương trình đào tạo';


CREATE TABLE class_section (
  id             INT          NOT NULL AUTO_INCREMENT COMMENT 'Mã lớp học phần',
  course_id      INT          NOT NULL                COMMENT 'Học phần',
  syllabus_id INT NULL COMMENT 'Đề cương sử dụng học kỳ này; có thể NULL trước khi Faculty tạo Draft',
  instructor_id  INT          NOT NULL                COMMENT 'Giảng viên phụ trách',
  semester       INT          NOT NULL                COMMENT 'Học kỳ: 1, 2, 3 (hè)',
  academic_year  VARCHAR(20)  NOT NULL                COMMENT 'Năm học: VD 2023-2024',
  group_number   INT          NOT NULL DEFAULT 1      COMMENT 'Số nhóm/lớp (nhóm lý thuyết)',
  lab_group      INT                                  COMMENT 'Số nhóm thực hành (NULL nếu không có lab)',
  max_students   INT          NOT NULL DEFAULT 50     COMMENT 'Sĩ số tối đa',
  room           VARCHAR(50)                          COMMENT 'Phòng học',
  schedule       VARCHAR(255)                         COMMENT 'Lịch học (mô tả)',
  section_type   ENUM(
                   'THEORY',    -- Lý thuyết
                   'LAB',       -- Thực hành
                   'COMBINED'   -- Kết hợp
                 ) NOT NULL DEFAULT 'THEORY'          COMMENT 'Loại lớp',
  is_active      BOOLEAN      NOT NULL DEFAULT TRUE,

  PRIMARY KEY (id),
  UNIQUE KEY uq_class_section_context (course_id, program_id, cohort_id, instructor_id, semester, academic_year, group_number),
  KEY idx_cs_course (course_id),
  KEY idx_cs_program (program_id),
  KEY idx_cs_cohort (cohort_id),
  KEY idx_cs_syllabus (syllabus_id),
  KEY idx_cs_instructor (instructor_id),
  KEY idx_cs_year_sem (academic_year, semester),
  CONSTRAINT fk_cs_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_cs_program FOREIGN KEY (program_id) REFERENCES program(id),
  CONSTRAINT fk_cs_cohort FOREIGN KEY (cohort_id) REFERENCES cohort(id),
  CONSTRAINT fk_cs_syllabus FOREIGN KEY (syllabus_id) REFERENCES syllabus(id),
  CONSTRAINT fk_cs_instructor FOREIGN KEY (instructor_id) REFERENCES instructor(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Lớp học phần mở theo từng học kỳ — gắn với phiên bản đề cương đang dùng';


CREATE TABLE enrollment (
  id               INT  NOT NULL AUTO_INCREMENT COMMENT 'Mã đăng ký học',
  student_id       INT  NOT NULL                COMMENT 'Sinh viên',
  class_section_id INT  NOT NULL                COMMENT 'Lớp học phần',
  enrolled_at      DATE NOT NULL                COMMENT 'Ngày đăng ký',
  status           ENUM(
                     'ENROLLED',    -- Đang học
                     'DROPPED',     -- Đã rút môn
                     'COMPLETED',   -- Hoàn thành
                     'FAILED'       -- Không đạt
                   ) NOT NULL DEFAULT 'ENROLLED' COMMENT 'Trạng thái học',

  PRIMARY KEY (id),
  UNIQUE KEY uq_enrollment (student_id, class_section_id),
  KEY idx_enroll_class (class_section_id),
  CONSTRAINT fk_enroll_student FOREIGN KEY (student_id) REFERENCES student(id),
  CONSTRAINT fk_enroll_class FOREIGN KEY (class_section_id) REFERENCES class_section(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Đăng ký học — sinh viên nào học lớp nào';


CREATE TABLE student_score (
  id                      INT   NOT NULL AUTO_INCREMENT COMMENT 'Mã điểm',
  enrollment_id           INT   NOT NULL                COMMENT 'Sinh viên trong lớp này',
  assessment_component_id INT   NOT NULL                COMMENT 'Thành phần đánh giá (Quiz/Midterm/Final...)',
  raw_score               FLOAT                         COMMENT 'Điểm thô (trước quy đổi)',
  final_score             FLOAT                         COMMENT 'Điểm cuối (đã quy đổi theo thang điểm)',
  is_absent               BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Vắng thi/nộp không',
  remark                  TEXT                          COMMENT 'Ghi chú (vắng có phép, nộp muộn...)',
  recorded_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recorded_by             INT                           COMMENT 'Người nhập điểm',

  PRIMARY KEY (id),
  UNIQUE KEY uq_student_score (enrollment_id, assessment_component_id),
  KEY idx_ss_assessment (assessment_component_id),
  CONSTRAINT fk_ss_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollment(id),
  CONSTRAINT fk_ss_assessment FOREIGN KEY (assessment_component_id) REFERENCES assessment_component(id),
  CONSTRAINT fk_ss_recorded_by FOREIGN KEY (recorded_by) REFERENCES user_account(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Điểm sinh viên theo từng thành phần đánh giá — cơ sở tính CLO attainment';


-- =============================================================================
-- DOMAIN 11: AUDIT LOG (NHẬT KÝ THAY ĐỔI)
-- =============================================================================

CREATE TABLE audit_log (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Mã bản ghi audit',
  table_name   VARCHAR(100) NOT NULL                COMMENT 'Bảng bị thay đổi',
  record_id    INT          NOT NULL                COMMENT 'ID bản ghi bị thay đổi',
  action       ENUM('CREATE','UPDATE','DELETE','STATUS_CHANGE') NOT NULL COMMENT 'Loại hành động',
  old_value    JSON                                 COMMENT 'Giá trị cũ (JSON)',
  new_value    JSON                                 COMMENT 'Giá trị mới (JSON)',
  changed_by   INT          NOT NULL                COMMENT 'Người thực hiện',
  changed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip_address   VARCHAR(45)                          COMMENT 'Địa chỉ IP',
  user_agent   VARCHAR(500)                         COMMENT 'Trình duyệt/thiết bị',

  PRIMARY KEY (id),
  KEY idx_audit_table_record (table_name, record_id),
  KEY idx_audit_user (changed_by),
  KEY idx_audit_time (changed_at),
  CONSTRAINT fk_audit_user FOREIGN KEY (changed_by) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT = 'Nhật ký thay đổi hệ thống — phục vụ kiểm toán và truy vết';


-- =============================================================================

SET FOREIGN_KEY_CHECKS = 1;
