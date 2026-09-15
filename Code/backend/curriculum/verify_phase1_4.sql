SELECT
    table_name,
    column_name,
    column_type,
    is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
      (table_name = 'syllabus'
       AND column_name IN ('workload_total','workload_contact','workload_private'))
      OR
      (table_name = 'book'
       AND column_name IN ('title','author','publisher','url'))
  )
ORDER BY table_name, column_name;

-- Expected: every row above has column_type = text.
