SET @DEFAULT_ADMIN = '${default.admin}';

INSERT INTO catgenome.user (id, name) SELECT nextval('catgenome.s_user'), @DEFAULT_ADMIN  WHERE @DEFAULT_ADMIN != '';
INSERT INTO catgenome.user_role (user_id, role_id) SELECT 1, 1 WHERE @DEFAULT_ADMIN != '';
INSERT INTO catgenome.user_role (user_id, role_id) SELECT 1, 2 WHERE @DEFAULT_ADMIN != '';