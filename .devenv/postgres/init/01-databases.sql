-- The `ngb` database and the `catgenome` role come from POSTGRES_DB / POSTGRES_USER.
-- `ngb_test` is what server/catgenome/profiles/postgres/test-catgenome.properties expects.
CREATE DATABASE ngb_test OWNER catgenome;

-- Flyway (applicationContext-flyway.xml) creates and owns the `catgenome` schema in both.
