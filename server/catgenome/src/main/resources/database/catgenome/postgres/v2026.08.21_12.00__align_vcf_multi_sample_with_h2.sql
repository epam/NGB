-- Converges CATGENOME.VCF.MULTI_SAMPLE with the h2 script set (Java 21 migration, phase 5).
--
-- v2021.12.03_12.00__multi_sample_vcf.sql added the column as
--     postgres: MULTI_SAMPLE BOOLEAN DEFAULT FALSE NOT NULL
--     h2:       MULTI_SAMPLE BOOLEAN
-- The Java is written against the h2 shape: VcfFile.multiSample is a boxed Boolean and
-- VcfFileDao binds it straight through, so a VcfFile registered without the flag set sends
-- NULL and the NOT NULL constraint rejects the insert. That is why BookmarkDaoTest and
-- VcfFileDaoTest fail on postgres only (4 tests) - the divergence is a postgres-side bug,
-- not a test problem.
--
-- Existing rows are left alone: everything already stored is a real true/false and keeps its
-- value. Only the constraint and the default are dropped, so postgres now accepts NULL for
-- "not stated" exactly as h2 does.
ALTER TABLE CATGENOME.VCF ALTER COLUMN MULTI_SAMPLE DROP NOT NULL;
ALTER TABLE CATGENOME.VCF ALTER COLUMN MULTI_SAMPLE DROP DEFAULT;
