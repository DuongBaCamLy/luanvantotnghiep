ALTER TABLE user_account
MODIFY COLUMN role ENUM(
    'ADMIN',
    'INSTRUCTOR',
    'DEPT_HEAD',
    'PROGRAM_COORDINATOR',
    'DEAN',
    'DEAN_SECRETARY',
    'STUDENT'
) NOT NULL COMMENT 'Vai trò trong hệ thống';
