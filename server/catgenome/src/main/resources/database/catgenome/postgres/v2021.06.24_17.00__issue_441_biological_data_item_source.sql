ALTER TABLE CATGENOME.BIOLOGICAL_DATA_ITEM ADD COLUMN SOURCE VARCHAR(500);
UPDATE CATGENOME.BIOLOGICAL_DATA_ITEM item SET SOURCE = PATH;
ALTER TABLE CATGENOME.BIOLOGICAL_DATA_ITEM ALTER COLUMN SOURCE SET NOT NULL;