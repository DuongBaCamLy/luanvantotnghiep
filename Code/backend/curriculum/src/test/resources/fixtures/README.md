# Isolated backend tests

Run `mvn test` with the local MySQL server running. Surefire activates `test` for
all Spring tests, including classes without `@ActiveProfiles`. The existing
`application-test.properties` points explicitly to `curriculum_iu_test`, creates
that database if absent, and recreates its schema when a test context starts.
This database is disposable; do not store development data in it. Concurrent
Maven runs must not share this test database.

`TestDatabaseSafetyGuard` is registered only on the test classpath. It runs after
configuration loading and rejects a missing test profile, a development URL,
alternate pool/JPA URLs or catalogs, and automatic SQL initialization before
Spring creates the datasource. IDE Spring-test runs must also activate `test`.
There is no fallback to `curriculum_iu`. Main application configuration is unchanged.

The PDF tests use `syllabus-import/cs2026-program.pdf` (517 pages, 25,312,580 bytes).
It was extracted offline from source-document row 7 in the existing local backup
`Code/database/backups/curriculum_iu_before_phase1_3c_20260903_162944.sql`.
SHA-256: `b24a4b9a0d052f8decc79410b92150415126bb5aabce6fa550bf4706d940f68a`.
The backup is not needed at test runtime and no test reads the demo database.

`cs2026.sql` provides two fixed curriculum examples (IT064IU and IT089IU) with
10 CLOs and 13 mappings. Database audit diagnostics now operate on these fixed
examples rather than the changing demo dataset. The source-document repository
returns the checked-in PDF fixture; parser assertions still exercise the real PDF.
