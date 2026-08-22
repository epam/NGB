-- Converges CATGENOME.TASK_ORGANISM.ORGANISM and CATGENOME.TASK_EXCL_ORGANISM.ORGANISM with
-- the h2 script set.
--
-- v2021.05.12_12.00__task.sql declared the column as
--     postgres: ORGANISM VARCHAR(250) NOT NULL
--     h2:       ORGANISM BIGINT NOT NULL
-- The Java is written against the h2 shape: BlastTask.organisms is a List<Long> of NCBI tax
-- ids, BlastTaskDao binds them as longs and reads them back with rs.getLong. On postgres the
-- bind is coerced to text on the way in and getLong parses it on the way out, which works
-- until something compares or orders by the column - hence the 2 BlastTaskDaoTest failures on
-- postgres only.
--
-- Every value ever written here came from a Long, so the cast cannot lose anything. It is
-- deliberately left unguarded: if a row somehow holds non-numeric text the migration fails
-- loudly rather than discarding it, and the operator note in
-- docs/md/installation/database-upgrade.md says what to do about it.
ALTER TABLE CATGENOME.TASK_ORGANISM
    ALTER COLUMN ORGANISM TYPE BIGINT USING ORGANISM::BIGINT;
ALTER TABLE CATGENOME.TASK_EXCL_ORGANISM
    ALTER COLUMN ORGANISM TYPE BIGINT USING ORGANISM::BIGINT;
