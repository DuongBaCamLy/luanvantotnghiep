-- The programme PDF reading list can contain long bibliographic entries.
-- Align the live table with schema_ddl.sql and the JPA entity.
ALTER TABLE book
    MODIFY COLUMN title VARCHAR(500) NOT NULL,
    MODIFY COLUMN author VARCHAR(500) NULL,
    MODIFY COLUMN url VARCHAR(1000) NULL;
