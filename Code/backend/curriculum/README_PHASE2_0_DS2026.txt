PHASE 2.0 — DS2026 EXACT CURRICULUM RECONCILIATION

Official source:
Hồ sơ CTĐT Khóa 2026 - Khoa học dữ liệu - 14.08.2026 Full.pdf

Verified DS2026:
- Program id resolved through cohort DS2026
- cohort id = 14
- 11 red import items are all official DS2026 curriculum courses

Already in Course Catalog; only DS2026 mapping is missing:
PE021IU, IT076IU, IT093IU, IT164IU, IT153IU

Missing from Course Catalog and therefore need Course + DS2026 mapping:
IT178IU Probability and Statistics
IT176IU Algorithmic Statistics
IT172IU Machine Learning
IT173IU Big Data Analytics
IT169IU Time Series Analysis
IT170IU Natural Language Processing

Official DS2026 placement:
PE021IU  GENERAL     Semester 2 / Year 1 / HK2 / required
IT178IU  GENERAL     Semester 2 / Year 1 / HK2 / required
IT176IU  GENERAL     Semester 4 / Year 2 / HK4 / required
IT172IU  COMPULSORY  Semester 5 / Year 3 / HK5 / required
IT173IU  COMPULSORY  Semester 6 / Year 3 / HK6 / required
IT169IU  ELECTIVE    no fixed semester
IT076IU  ELECTIVE    no fixed semester
IT170IU  ELECTIVE    no fixed semester
IT093IU  ELECTIVE    no fixed semester
IT164IU  ELECTIVE    no fixed semester
IT153IU  ELECTIVE    no fixed semester

Safety:
- reconciliation is INSERT-only
- existing Course/CourseProgram rows are never updated/deleted
- duplicate canonical/base codes abort the transaction
- runner creates a targeted course + course_program backup
- cleanup is separate and locked to exactly the current 35 DS2026 DRAFT rows
