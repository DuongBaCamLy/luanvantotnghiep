ALTER TABLE user_account
    ADD COLUMN managed_major_id INT NULL AFTER instructor_id,
    ADD CONSTRAINT fk_user_account_managed_major
        FOREIGN KEY (managed_major_id) REFERENCES major(id);

CREATE INDEX idx_user_account_managed_major
    ON user_account (role, managed_major_id, is_active);

-- Migrate existing heads from real approval/program context. No username or ID is hard-coded.
UPDATE user_account u
JOIN (
    SELECT ar.requested_by AS user_id, MIN(p.major_id) AS major_id
    FROM approval_request ar
    JOIN syllabus s ON s.id = ar.syllabus_id
    JOIN course_program cp ON cp.syllabus_id = s.id
    JOIN program p ON p.id = cp.program_id
    WHERE ar.step = 'STEP3_DEAN' AND p.major_id IS NOT NULL
    GROUP BY ar.requested_by
) scope ON scope.user_id = u.id
SET u.managed_major_id = scope.major_id
WHERE u.role = 'DEPT_HEAD' AND u.managed_major_id IS NULL;
