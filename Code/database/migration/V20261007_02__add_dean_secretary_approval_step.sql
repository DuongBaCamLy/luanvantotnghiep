SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- ============================================================
-- Add Dean Secretary as the active workflow Step 2.
--
-- Current workflow:
-- STEP1_DEPT_HEAD
--      -> STEP2_DEAN_SECRETARY
--      -> STEP3_DEAN
--
-- STEP2_PROG_COORDINATOR is intentionally retained as a legacy
-- enum value so historical approval records remain readable.
--
-- This migration does NOT rewrite approval history.
-- ============================================================
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            
ALTER TABLE approval_request
    MODIFY COLUMN step ENUM(
        'STEP1_DEPT_HEAD',
        'STEP2_PROG_COORDINATOR',
        'STEP2_DEAN_SECRETARY',
        'STEP3_DEAN'
    )
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci
        NOT NULL
    COMMENT 'Bước phê duyệt hiện tại',
    COMMENT = 'Nhật ký phê duyệt đề cương — workflow 3 bước: Bộ môn → Thư ký Khoa → Trưởng Khoa';


-- ============================================================
-- Refresh the approval-status view for the current workflow.
--
-- STEP2_PROG_COORDINATOR remains stored/readable in historical
-- approval_request records, but it is no longer the active
-- Step 2 represented by this workflow-status view.
-- ============================================================

CREATE OR REPLACE VIEW v_syllabus_approval_status AS
SELECT
    s.id AS syllabus_id,
    c.course_code AS course_code,
    c.name AS course_name,
    s.version_number AS version_number,
    s.version_label AS version_label,
    s.status AS syllabus_status,
    s.is_current AS is_current,

    COALESCE(
        NULLIF(TRIM(ua.full_name), ''),
        ua.username
    ) AS author,

    ar_s1.status AS step1_status,
    ar_s1.resolved_at AS step1_resolved_at,

    ar_s2.status AS step2_status,
    ar_s2.resolved_at AS step2_resolved_at,

    ar_s3.status AS step3_status,
    ar_s3.resolved_at AS step3_resolved_at,

    s.created_at AS created_at,
    s.approved_at AS approved_at

FROM syllabus s

JOIN course c
    ON c.id = s.course_id

JOIN user_account ua
    ON ua.id = s.created_by

LEFT JOIN approval_request ar_s1
    ON ar_s1.syllabus_id = s.id
   AND ar_s1.syllabus_version_number = s.version_number
   AND ar_s1.step = 'STEP1_DEPT_HEAD'

LEFT JOIN approval_request ar_s2
    ON ar_s2.syllabus_id = s.id
   AND ar_s2.syllabus_version_number = s.version_number
   AND ar_s2.step = 'STEP2_DEAN_SECRETARY'

LEFT JOIN approval_request ar_s3
    ON ar_s3.syllabus_id = s.id
   AND ar_s3.syllabus_version_number = s.version_number
   AND ar_s3.step = 'STEP3_DEAN';