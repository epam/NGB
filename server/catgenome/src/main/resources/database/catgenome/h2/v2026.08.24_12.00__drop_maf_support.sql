-- Drops MAF (TCGA Mutation Annotation Format) support: the CATGENOME.MAF table, its S_MAF
-- sequence, and every row that could still reference a MAF biological data item.
--
-- MAF was reachable only from Java after MafController was deleted in 562b6a6d (2018-12-10), so no
-- NGB released since has been able to register a MAF file over REST. Any row this script finds was
-- written by NGB 2.7.x or earlier. The code that read those rows - MafManager, MafFileDao, the MAF
-- parser, and the MAF branches of FileManager and DataItemManager - is removed in this release, so
-- leaving the rows would leave items that no endpoint can load, delete or repair.
--
-- The purge is automatic and irreversible, and there is no operator pre-step: take a backup before
-- upgrading, as docs/md/installation/database-upgrade.md says.
--
-- Statement order is load-bearing. Everything keyed on FORMAT IN (13, 14) has to run while those
-- BIOLOGICAL_DATA_ITEM rows still exist, each foreign key is cleared from the leaf inwards, and
-- CATGENOME.MAF is dropped before the rows it points at, because maf_bio_data_item_id_fkey and
-- MAF_INDEX_ID_FKEY both reference BIOLOGICAL_DATA_ITEM.
--
-- Three things this script deliberately does not do:
--
--   * It does not edit the Database_create_script that creates CATGENOME.MAF and S_MAF. That script
--     is released and its Flyway checksum is fixed, so a fresh install still creates the table and
--     this script drops it again.
--   * It does not touch ROLE_MAF_MANAGER. PostgreSQL's row was already deleted by
--     v2026.08.21_12.20__align_predefined_roles_with_h2.sql, and H2 never had one.
--   * It cannot delete the MAF files and their tabix indexes under <contents>/maf. SQL does not
--     reach the filesystem; the operator is told to remove that directory in
--     docs/md/installation/database-upgrade.md.
--
-- Format ids 13 (MAF) and 14 (MAF_INDEX) stay reserved in BiologicalDataItemFormat and must never
-- be reassigned: a database restored from an older backup can still hold them, and reusing either
-- id would silently reinterpret those rows as some other format.
--
-- Every DROP uses IF EXISTS, so an install created from a future baseline that never had the table
-- does not fail here.

-- 1. Metadata. ENTITY_CLASS holds AclClass.name() and is read back with AclClass.valueOf, so a
--    surviving 'MAF' row throws once the enum constant is gone.
DELETE FROM CATGENOME.METADATA WHERE ENTITY_CLASS = 'MAF';

-- 2. ACL, leaves first: entries, then any identity that named a MAF identity as its parent, then
--    the identities, then the class row itself (acl_object_identity.object_id_class -> acl_class).
DELETE FROM CATGENOME.ACL_ENTRY
WHERE ACL_OBJECT_IDENTITY IN (
    SELECT ID FROM CATGENOME.ACL_OBJECT_IDENTITY
    WHERE OBJECT_ID_CLASS IN (
        SELECT ID FROM CATGENOME.ACL_CLASS WHERE CLASS = 'com.epam.catgenome.entity.maf.MafFile'));

UPDATE CATGENOME.ACL_OBJECT_IDENTITY SET PARENT_OBJECT = NULL
WHERE PARENT_OBJECT IN (
    SELECT ID FROM CATGENOME.ACL_OBJECT_IDENTITY
    WHERE OBJECT_ID_CLASS IN (
        SELECT ID FROM CATGENOME.ACL_CLASS WHERE CLASS = 'com.epam.catgenome.entity.maf.MafFile'));

DELETE FROM CATGENOME.ACL_OBJECT_IDENTITY
WHERE OBJECT_ID_CLASS IN (
    SELECT ID FROM CATGENOME.ACL_CLASS WHERE CLASS = 'com.epam.catgenome.entity.maf.MafFile');

DELETE FROM CATGENOME.ACL_CLASS WHERE CLASS = 'com.epam.catgenome.entity.maf.MafFile';

-- 3. Everything that can hold a reference to a MAF biological data item: dataset items, bookmark
--    items, and a reference genome's annotation files.
DELETE FROM CATGENOME.PROJECT_ITEM
WHERE REFERRED_BIO_DATA_ITEM_ID IN (
    SELECT BIO_DATA_ITEM_ID FROM CATGENOME.BIOLOGICAL_DATA_ITEM WHERE FORMAT IN (13, 14));

DELETE FROM CATGENOME.BOOKMARK_ITEM
WHERE BIO_DATA_ITEM_ID IN (
    SELECT BIO_DATA_ITEM_ID FROM CATGENOME.BIOLOGICAL_DATA_ITEM WHERE FORMAT IN (13, 14));

DELETE FROM CATGENOME.GENOME_ANNOTATION_DATA_ITEM
WHERE BIO_DATA_ITEM_ID IN (
    SELECT BIO_DATA_ITEM_ID FROM CATGENOME.BIOLOGICAL_DATA_ITEM WHERE FORMAT IN (13, 14));

-- 4. The table, before the rows it points at.
DROP TABLE IF EXISTS CATGENOME.MAF;

-- 5. The file rows (format 13) and their index rows (format 14).
DELETE FROM CATGENOME.BIOLOGICAL_DATA_ITEM WHERE FORMAT IN (13, 14);

-- 6. The sequence that fed MAF_ID.
DROP SEQUENCE IF EXISTS CATGENOME.S_MAF;
