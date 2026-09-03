-- A cohort code is scoped by its curriculum program. Examples:
-- CS-2021 + 2024 -> CS2024; IT-2021 + 2024 -> IT2024.
-- IDs and all foreign-key relationships remain unchanged.
UPDATE cohort c
JOIN program p ON p.id = c.program_id
SET c.name = CONCAT(
    REGEXP_REPLACE(
        REGEXP_REPLACE(UPPER(TRIM(p.code)), '[-_]?[0-9]{4}$', ''),
        '[^A-Z0-9]',
        ''
    ),
    c.entry_year
)
WHERE c.name <> CONCAT(
    REGEXP_REPLACE(
        REGEXP_REPLACE(UPPER(TRIM(p.code)), '[-_]?[0-9]{4}$', ''),
        '[^A-Z0-9]',
        ''
    ),
    c.entry_year
);
