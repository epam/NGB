CREATE SEQUENCE IF NOT EXISTS CATGENOME.S_BLAST_DATABASE START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS CATGENOME.BLAST_DATABASE (
    DATABASE_ID BIGINT NOT NULL PRIMARY KEY,
    NAME        TEXT NULL,
    PATH        TEXT NOT NULL,
    TYPE        BIGINT NOT NULL,
    SOURCE      BIGINT NOT NULL
);