
INSERT INTO catgenome.user (id, name) SELECT nextval('catgenome.s_user'), '${default.admin}' WHERE '${default.admin}' != '';
INSERT INTO catgenome.user_role (user_id, role_id) SELECT 1, 1 WHERE '${default.admin}' != '';
INSERT INTO catgenome.user_role (user_id, role_id) SELECT 1, 2 WHERE '${default.admin}' != '';