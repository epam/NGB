-- Drops the legacy 'person' authentication model. It was fully superseded by
-- CATGENOME.USER / CATGENOME.ROLE (added by the ACL_tables migration) and stored a plaintext
-- PASSWORD column. PERSON's only foreign key pointed at
-- PERSON_ROLE, and nothing outside these two tables referenced either of them, so the drop
-- needs no other change.
DROP TABLE IF EXISTS CATGENOME.PERSON;
DROP TABLE IF EXISTS CATGENOME.PERSON_ROLE;
DROP SEQUENCE IF EXISTS CATGENOME.S_PERSON;
