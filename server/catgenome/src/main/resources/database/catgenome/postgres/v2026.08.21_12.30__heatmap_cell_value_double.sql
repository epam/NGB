-- Types CATGENOME.HEATMAP's cell-value bounds as what the application actually stores in
-- them.
--
-- v2021.09.14_12.00__heatmap.sql declared MIN_CELL_VALUE and MAX_CELL_VALUE as bare DECIMAL.
-- Heatmap.minCellValue / .maxCellValue are Double and HeatmapDao reads them with
-- rs.getDouble, so a decimal type was never the intent - and bare DECIMAL means different
-- things to different engines. H2 1.3.176 kept whatever scale the inserted value had; H2
-- 2.3.232 reads it as NUMERIC(100000, 0), i.e. **scale zero**, and silently rounds every
-- value to an integer. That is HeatmapManagerTest.createHeatmapTest failing with
-- expected:<0.001273579> but was:<0.0> on H2 2.x, and it is a data bug, not a test bug.
-- PostgreSQL's unconstrained NUMERIC keeps the scale, which is why only H2 showed it.
--
-- DOUBLE PRECISION is spelled the same in both script sets and is what rs.getDouble wants.
-- The cast of existing values is exact: they all came from a Java double.
--
-- Applied to both flavours so the two script sets stay identical here; on PostgreSQL it is a
-- no-op in behaviour, only in declared type.
ALTER TABLE CATGENOME.HEATMAP ALTER COLUMN MIN_CELL_VALUE SET DATA TYPE DOUBLE PRECISION;
ALTER TABLE CATGENOME.HEATMAP ALTER COLUMN MAX_CELL_VALUE SET DATA TYPE DOUBLE PRECISION;
