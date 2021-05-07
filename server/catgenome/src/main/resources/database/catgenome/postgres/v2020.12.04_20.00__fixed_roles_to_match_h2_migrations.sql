-- bringing roles on Postgres in sync with H2 version
DELETE FROM catgenome.user_role WHERE role_id IN (SELECT id FROM catgenome.role WHERE name = 'ROLE_CYTOBANDS_MANAGER');
DELETE FROM catgenome.user_role WHERE role_id IN (SELECT id FROM catgenome.role WHERE name = 'ROLE_MAF_MANAGER');
DELETE FROM catgenome.user_role WHERE role_id IN (SELECT id FROM catgenome.role WHERE name = 'ROLE_SEG_MANAGER');

DELETE FROM catgenome.role WHERE name = 'ROLE_CYTOBANDS_MANAGER';
DELETE FROM catgenome.role WHERE name = 'ROLE_MAF_MANAGER';

INSERT INTO catgenome.role (id, name, predefined) VALUES (8, 'ROLE_WIG_MANAGER', true);
UPDATE catgenome.role SET id = 9 WHERE name = 'ROLE_SEG_MANAGER';
