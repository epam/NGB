-- Converges CATGENOME.BAM_COVERAGE.COVERAGE with the h2 script set (Java 21 migration,
-- phase 5).
--
-- v2022.02.16_16.00__coverage.sql declared it as
--     postgres: COVERAGE NUMERIC NOT NULL
--     h2:       COVERAGE DOUBLE NOT NULL
-- BamCoverage.coverage is a Float, so h2 is again the one that matches the application. No
-- test catches this divergence - PostgreSQL's unconstrained NUMERIC round-trips the value
-- correctly, unlike the HEATMAP columns in the previous migration - so it is a tidy-up rather
-- than a fix. It is here because leaving one known difference behind would mean the next
-- person cannot trust that the two sets are the same.
ALTER TABLE CATGENOME.BAM_COVERAGE
    ALTER COLUMN COVERAGE SET DATA TYPE DOUBLE PRECISION;
