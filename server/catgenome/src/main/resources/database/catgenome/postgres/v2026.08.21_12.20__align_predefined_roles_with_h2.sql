-- Converges CATGENOME.ROLE's predefined rows with the h2 script set (Java 21 migration,
-- phase 5).
--
-- v2018.09.17_11.03__ACL_tables.sql seeded a different set on each flavour:
--     h2:       ... 7 ROLE_BED_MANAGER, 8 ROLE_WIG_MANAGER,       9 ROLE_SEG_MANAGER
--     postgres: ... 7 ROLE_BED_MANAGER, 8 ROLE_CYTOBANDS_MANAGER, 9 ROLE_MAF_MANAGER,
--                                                                10 ROLE_SEG_MANAGER
-- and v2024.02.19_18.00__acl_target_manager_role.sql then put ROLE_TARGET_MANAGER at 10 on h2
-- but at MAX(id)+1 = 11 on postgres. So postgres has 11 predefined roles to h2's 10, which is
-- the RoleDaoTest.testLoadRolesWithUsers failure.
--
-- h2 is the reference, because all three authorities agree with it and none of them mentions
-- ROLE_CYTOBANDS_MANAGER or ROLE_MAF_MANAGER:
--   * DefaultRoles - the enum the application resolves predefined roles through - ends
--     ROLE_WIG_MANAGER(8), ROLE_SEG_MANAGER(9);
--   * docs/md/user-guide/um-overview.md documents exactly h2's set;
--   * RoleDaoTest expects 10.
-- Note that the postgres seeding left ROLE_WIG_MANAGER missing entirely, which is a real
-- production bug and not only a test divergence: WigSecurityService's @PreAuthorize goes
-- through hasSpecificRole(WIG) -> hasRole("WIG_MANAGER"), so on a postgres install no role
-- can satisfy it and only an administrator can manage WIG files.
--
-- What this does, in order:
--   1. drops the two roles postgres should never have had, and any grants of them. Users who
--      held ROLE_MAF_MANAGER lose it - MAF management falls back to administrators, which is
--      what an h2 install has always done;
--   2. renumbers the survivors onto DefaultRoles' ids, carrying their grants with them. Ids
--      are parked in a disjoint range first so no intermediate state collides with the
--      primary key: 10 -> 1009 -> 9 and 11 -> 1010 -> 10 never occupy a taken id. The FK from
--      USER_ROLE is dropped for the duration and restored at the end; the whole migration is
--      one transaction, so nothing outside it observes the intermediate state. Roles created
--      by operators are untouched - S_ROLE starts at 100 and this only maps names it knows;
--   3. adds the missing ROLE_WIG_MANAGER at the id DefaultRoles gives it.
-- Every step is a no-op on a database that is already aligned, so a fresh install (which
-- still runs the original seeding first) and an existing one converge to the same rows.
-- user_role_role_id_fkey is the name postgres generates for the unnamed
-- "role_id BIGINT NOT NULL REFERENCES catgenome.role (id)" in the ACL_tables migration.
ALTER TABLE CATGENOME.USER_ROLE DROP CONSTRAINT IF EXISTS user_role_role_id_fkey;

DELETE FROM CATGENOME.USER_ROLE
WHERE ROLE_ID IN (SELECT ID FROM CATGENOME.ROLE
                  WHERE NAME IN ('ROLE_CYTOBANDS_MANAGER', 'ROLE_MAF_MANAGER'));
DELETE FROM CATGENOME.ROLE WHERE NAME IN ('ROLE_CYTOBANDS_MANAGER', 'ROLE_MAF_MANAGER');

CREATE TEMPORARY TABLE role_renumber AS
SELECT R.ID AS OLD_ID, T.NEW_ID
FROM CATGENOME.ROLE R
JOIN (VALUES ('ROLE_ADMIN', 1),
             ('ROLE_USER', 2),
             ('ROLE_REFERENCE_MANAGER', 3),
             ('ROLE_BAM_MANAGER', 4),
             ('ROLE_VCF_MANAGER', 5),
             ('ROLE_GENE_MANAGER', 6),
             ('ROLE_BED_MANAGER', 7),
             ('ROLE_WIG_MANAGER', 8),
             ('ROLE_SEG_MANAGER', 9),
             ('ROLE_TARGET_MANAGER', 10)) AS T(NAME, NEW_ID) ON T.NAME = R.NAME
WHERE R.ID <> T.NEW_ID;

UPDATE CATGENOME.ROLE R SET ID = M.NEW_ID + 1000
FROM role_renumber M WHERE R.ID = M.OLD_ID;
UPDATE CATGENOME.USER_ROLE UR SET ROLE_ID = M.NEW_ID
FROM role_renumber M WHERE UR.ROLE_ID = M.OLD_ID;
UPDATE CATGENOME.ROLE SET ID = ID - 1000 WHERE ID > 1000;

DROP TABLE role_renumber;

INSERT INTO CATGENOME.ROLE (ID, NAME, PREDEFINED, USER_DEFAULT)
SELECT 8, 'ROLE_WIG_MANAGER', TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM CATGENOME.ROLE WHERE NAME = 'ROLE_WIG_MANAGER');

ALTER TABLE CATGENOME.USER_ROLE
    ADD CONSTRAINT user_role_role_id_fkey FOREIGN KEY (ROLE_ID) REFERENCES CATGENOME.ROLE (ID);
