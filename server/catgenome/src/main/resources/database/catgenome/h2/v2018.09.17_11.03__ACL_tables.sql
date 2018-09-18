CREATE TABLE catgenome.user(
    id BIGINT NOT NULL primary key,
    name text NOT NULL,
    CONSTRAINT unique_key_user_name UNIQUE (name)
);

CREATE TABLE catgenome.role(
    id BIGINT NOT NULL primary key,
    name text NOT NULL,
    CONSTRAINT unique_key_role_name UNIQUE (name)
);

CREATE TABLE catgenome.user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT unique_key_user_roles UNIQUE (user_id,role_id),
    CONSTRAINT user_roles_user_id_fk FOREIGN KEY (user_id) REFERENCES pipeline.user (id),
    CONSTRAINT user_roles_role_id_fk FOREIGN KEY (role_id) REFERENCES pipeline.role (id)
);

INSERT INTO catgenome.user (id, name) VALUES (1, '${default.admin}');

INSERT INTO catgenome.role (id, name) VALUES (1, 'ROLE_ADMIN');
INSERT INTO catgenome.role (id, name) VALUES (2, 'ROLE_USER');
INSERT INTO catgenome.role (id, name) VALUES (3, 'ROLE_REFERENCE_MANAGER');
INSERT INTO catgenome.role (id, name) VALUES (4, 'ROLE_BAM_MANAGER');
INSERT INTO catgenome.role (id, name) VALUES (5, 'ROLE_VCF_MANAGER');
INSERT INTO catgenome.role (id, name) VALUES (6, 'ROLE_GENE_MANAGER');
INSERT INTO catgenome.role (id, name) VALUES (7, 'ROLE_BED_MANAGER');
INSERT INTO catgenome.role (id, name) VALUES (8, 'ROLE_CYTOBANDS_MANAGER');
INSERT INTO catgenome.role (id, name) VALUES (9, 'ROLE_MAF_MANAGER');
INSERT INTO catgenome.role (id, name) VALUES (10, 'ROLE_SEG_MANAGER');

INSERT INTO catgenome.user_roles (user_id, role_id) VALUES (1, 1);

CREATE SEQUENCE catgenome.S_USER START WITH 2 INCREMENT BY 1;
CREATE SEQUENCE catgenome.S_ROLE START WITH 100 INCREMENT BY 1;

CREATE TABLE catgenome.acl_sid(
    id bigserial not null primary key,
    principal boolean not null,
    sid text not null,
    constraint unique_uk_1 unique(sid,principal)
);

CREATE TABLE catgenome.acl_class(
    id bigserial not null primary key,
    class varchar(100) not null,
    constraint unique_uk_2 unique(class)
);

INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.reference.Reference');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.project.Project');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.FeatureFile');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.reference.Bookmark');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.bucket.Bucket');

CREATE TABLE catgenome.acl_object_identity(
    id bigserial primary key,
    object_id_class bigint not null,
    object_id_identity bigint not null,
    parent_object bigint,
    owner_sid bigint,
    entries_inheriting boolean not null,
    constraint unique_uk_3 unique(object_id_class,object_id_identity),
    constraint foreign_fk_1 foreign key(parent_object)references acl_object_identity(id),
    constraint foreign_fk_2 foreign key(object_id_class)references acl_class(id),
    constraint foreign_fk_3 foreign key(owner_sid)references acl_sid(id)
);

CREATE TABLE catgenome.acl_entry(
    id bigserial primary key,
    acl_object_identity bigint not null,
    ace_order int not null,
    sid bigint not null,
    mask integer not null,
    granting boolean not null,
    audit_success boolean not null,
    audit_failure boolean not null,
    constraint unique_uk_4 unique(acl_object_identity,ace_order),
    constraint foreign_fk_4 foreign key(acl_object_identity) references acl_object_identity(id),
    constraint foreign_fk_5 foreign key(sid) references acl_sid(id)
);