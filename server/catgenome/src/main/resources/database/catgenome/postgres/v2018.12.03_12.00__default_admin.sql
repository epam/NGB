INSERT INTO catgenome.user (id, name) VALUES (catgenome.s_user.nextval, '${default.admin}');
INSERT INTO catgenome.user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO catgenome.user_role (user_id, role_id) VALUES (1, 2);