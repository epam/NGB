-- id 10 is already taken on this flavour (ROLE_SEG_MANAGER, seeded by
-- v2018.09.17_11.03__ACL_tables.sql - the postgres seeding differs from h2, which has
-- ROLE_WIG_MANAGER at 8 and ROLE_SEG_MANAGER at 9), so take the next free id instead.
-- DefaultRoles.ROLE_TARGET_MANAGER carries a null id, so nothing depends on the value.
INSERT INTO catgenome.role (id, name, predefined)
VALUES ((SELECT MAX(id) + 1 FROM catgenome.role), 'ROLE_TARGET_MANAGER', true);
