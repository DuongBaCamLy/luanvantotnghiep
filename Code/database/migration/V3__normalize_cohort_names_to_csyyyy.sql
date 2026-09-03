-- Cohort identity is derived from entry_year.  Preserve IDs and every foreign
-- key relationship while normalising legacy values such as K24/K2024.
UPDATE cohort
SET name = CONCAT('CS', entry_year)
WHERE name IS NULL
   OR name <> CONCAT('CS', entry_year);
