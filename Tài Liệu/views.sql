-- ============================================================
-- views.sql  —  Analytical views for dashboard
-- Run AFTER schema_ddl.sql and seed_data.sql
-- ============================================================
USE curriculum_iu;

CREATE OR REPLACE VIEW v_clo_plo_matrix AS
SELECT
    s.id                AS syllabus_id,
    s.version_label,
    c.course_code,
    c.name              AS course_name,
    cl.code             AS clo_code,
    cl.description      AS clo_description,
    p.code              AS plo_code,
    p.description       AS plo_description,
    m.level             AS mapping_level,        -- I / D / A
    m.contribution_weight
FROM clo_plo_mapping m
JOIN clo cl         ON cl.id = m.clo_id
JOIN plo p          ON p.id  = m.plo_id
JOIN syllabus s     ON s.id  = cl.syllabus_id
JOIN course c       ON c.id  = s.course_id
ORDER BY c.course_code, cl.code, p.code;

-- View: Trạng thái phê duyệt của từng đề cương
CREATE OR REPLACE VIEW v_syllabus_approval_status AS
SELECT
    s.id                AS syllabus_id,
    c.course_code,
    c.name              AS course_name,
    s.version_number,
    s.version_label,
    s.status            AS syllabus_status,
    s.is_current,
    CONCAT(i.full_name, ' (', i.degree, ')') AS author,
    ar_s1.status        AS step1_status,         -- Bộ môn
    ar_s1.resolved_at   AS step1_resolved_at,
    ar_s2.status        AS step2_status,         -- CTĐT
    ar_s2.resolved_at   AS step2_resolved_at,
    ar_s3.status        AS step3_status,         -- Trưởng Khoa
    ar_s3.resolved_at   AS step3_resolved_at,
    s.created_at,
    s.approved_at
FROM syllabus s
JOIN course c ON c.id = s.course_id
JOIN user_account ua ON ua.id = s.created_by
JOIN instructor i    ON i.id  = ua.instructor_id
LEFT JOIN approval_request ar_s1
    ON ar_s1.syllabus_id = s.id AND ar_s1.step = 'STEP1_DEPT_HEAD'
LEFT JOIN approval_request ar_s2
    ON ar_s2.syllabus_id = s.id AND ar_s2.step = 'STEP2_PROG_COORDINATOR'
LEFT JOIN approval_request ar_s3
    ON ar_s3.syllabus_id = s.id AND ar_s3.step = 'STEP3_DEAN';

-- View: CLO coverage — CLO nào được dạy bao nhiêu tuần, ở mức nào
CREATE OR REPLACE VIEW v_clo_coverage AS
SELECT
    cl.syllabus_id,
    cl.code             AS clo_code,
    cl.description,
    COUNT(tc.topic_id)  AS num_topics_covered,
    SUM(t.teaching_hours) AS total_teaching_hours,
    GROUP_CONCAT(DISTINCT tc.teaching_level ORDER BY tc.teaching_level) AS levels_covered
FROM clo cl
LEFT JOIN topic_clo tc ON tc.clo_id = cl.id
LEFT JOIN topic t      ON t.id = tc.topic_id
GROUP BY cl.id, cl.syllabus_id, cl.code, cl.description;

-- View: Compliance dashboard — tỷ lệ đề cương đã duyệt theo CTĐT
CREATE OR REPLACE VIEW v_program_compliance_summary AS
SELECT
    pr.id               AS program_id,
    pr.code             AS program_code,
    pr.name             AS program_name,
    COUNT(DISTINCT cp.course_id)                           AS total_courses,
    COUNT(DISTINCT CASE WHEN s.status = 'APPROVED' THEN s.course_id END) AS approved_courses,
    COUNT(DISTINCT CASE WHEN s.status = 'DRAFT' OR s.status IS NULL
                        THEN cp.course_id END)             AS pending_courses,
    ROUND(
      COUNT(DISTINCT CASE WHEN s.status = 'APPROVED' THEN s.course_id END)
      / COUNT(DISTINCT cp.course_id) * 100, 1
    )                                                       AS approval_rate_pct
FROM program pr
JOIN course_program cp ON cp.program_id = pr.id
LEFT JOIN syllabus s   ON s.course_id = cp.course_id AND s.is_current = TRUE
GROUP BY pr.id, pr.code, pr.name;

