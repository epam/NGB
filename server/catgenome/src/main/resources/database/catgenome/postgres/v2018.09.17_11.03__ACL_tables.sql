CREATE TABLE catgenome.user (
    id BIGINT NOT NULL primary key,
    name VARCHAR(1024) NOT NULL,
    attributes VARCHAR(1000000),
    CONSTRAINT unique_key_user_name UNIQUE (name)
);

CREATE TABLE catgenome.security_group (
  id BIGINT NOT NULL PRIMARY KEY,
  name VARCHAR(1024) NOT NULL UNIQUE
);

CREATE TABLE catgenome.role (
    id BIGINT NOT NULL primary key,
    name VARCHAR(1024) NOT NULL,
    predefined BOOLEAN DEFAULT FALSE NOT NULL,
    user_default BOOLEAN DEFAULT FALSE NOT NULL,
    CONSTRAINT unique_key_role_name UNIQUE (name)
);

CREATE TABLE catgenome.user_security_group (
    user_id BIGINT NOT NULL REFERENCES catgenome.user (id),
    group_id BIGINT NOT NULL REFERENCES catgenome.security_group (id),
    CONSTRAINT unique_key_user_groups UNIQUE (user_id,group_id)
);

CREATE TABLE catgenome.user_role (
    user_id BIGINT NOT NULL REFERENCES catgenome.user (id),
    role_id BIGINT NOT NULL REFERENCES catgenome.role (id),
    CONSTRAINT unique_key_user_roles UNIQUE (user_id,role_id)
);

INSERT INTO catgenome.role (id, name, predefined, user_default) VALUES (1, 'ROLE_ADMIN', true, false);
INSERT INTO catgenome.role (id, name, predefined, user_default) VALUES (2, 'ROLE_USER', true, true);
INSERT INTO catgenome.role (id, name, predefined) VALUES (3, 'ROLE_REFERENCE_MANAGER', true);
INSERT INTO catgenome.role (id, name, predefined) VALUES (4, 'ROLE_BAM_MANAGER', true);
INSERT INTO catgenome.role (id, name, predefined) VALUES (5, 'ROLE_VCF_MANAGER', true);
INSERT INTO catgenome.role (id, name, predefined) VALUES (6, 'ROLE_GENE_MANAGER', true);
INSERT INTO catgenome.role (id, name, predefined) VALUES (7, 'ROLE_BED_MANAGER', true);
INSERT INTO catgenome.role (id, name, predefined) VALUES (8, 'ROLE_CYTOBANDS_MANAGER', true);
INSERT INTO catgenome.role (id, name, predefined) VALUES (9, 'ROLE_MAF_MANAGER', true);
INSERT INTO catgenome.role (id, name, predefined) VALUES (10, 'ROLE_SEG_MANAGER', true);

CREATE SEQUENCE catgenome.S_USER START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE catgenome.S_SECURITY_GROUP START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE catgenome.S_ROLE START WITH 100 INCREMENT BY 1;

CREATE TABLE catgenome.acl_sid (
    id bigserial not null primary key,
    principal boolean not null,
    sid VARCHAR(1024) not null,
    constraint unique_uk_1 unique(sid,principal)
);

CREATE TABLE catgenome.acl_class(
    id bigserial not null primary key,
    class varchar(100) not null,
    constraint unique_uk_2 unique(class)
);

INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.reference.Reference');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.project.Project');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.vcf.VcfFile');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.bam.BamFile');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.gene.GeneFile');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.maf.MafFile');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.bed.BedFile');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.seg.SegFile');
INSERT INTO catgenome.acl_class (class) VALUES ('com.epam.catgenome.entity.wig.WigFile');
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

ALTER TABLE catgenome.biological_data_item ADD owner TEXT NULL;
UPDATE catgenome.biological_data_item SET owner = 'Unauthorized';
ALTER TABLE catgenome.biological_data_item ALTER COLUMN owner SET NOT NULL;

ALTER TABLE catgenome.project ADD owner TEXT NULL;
UPDATE catgenome.project SET owner = 'Unauthorized';
ALTER TABLE catgenome.project ALTER COLUMN owner SET NOT NULL;

ALTER TABLE catgenome.bookmark ADD owner TEXT NULL;
UPDATE catgenome.bookmark SET owner = 'Unauthorized';
ALTER TABLE catgenome.bookmark ALTER COLUMN owner SET NOT NULL;

ALTER TABLE catgenome.bucket ADD owner TEXT NULL;
UPDATE catgenome.bucket SET owner = 'Unauthorized';
ALTER TABLE catgenome.bucket ALTER COLUMN owner SET NOT NULL;