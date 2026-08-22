# Upgrading the NGB database

This release changes the database layer. **A fresh installation needs nothing from this
page** — start NGB and it creates everything itself. Read on only if you are upgrading an
NGB instance that already has a database.

| | Previous releases | This release |
|---|---|---|
| H2 | 1.3.176 | 2.3.232 |
| PostgreSQL JDBC driver | 9.4-1206 | 42.7.x |
| Supported PostgreSQL | 9.6 (older Flyway refused to connect to anything newer) | 9.6 – 17, tested on 16 |
| Flyway (schema versioning) | 3.2.1 | 11.7.2 |

Two of those upgrades cannot be done by NGB on its own, because they change the on-disk
format of the database itself:

* **H2 2.x cannot open a database file written by H2 1.3.** You must export and re-import
  it — see [H2](#h2) below.
* **PostgreSQL cannot read a data directory from an earlier major version.** If you are
  also moving the server from 9.6 to 16, you must dump and restore — see
  [PostgreSQL](#postgresql) below.

Everything else *is* automatic. In particular you do not have to touch Flyway: the first
start converts the schema history in place, as described under
[what NGB does on first start](#what-ngb-does-on-first-start).

**Back up first.** Stop NGB, then copy the whole database aside before doing anything
here. Every procedure below is one-way. Rehearse it on the copy if you can: the conversion has
been exercised against a purpose-built pre-3.0.0 database with real content, but not against a
production-sized one, so how long it takes at your scale is not known.

## Before you convert: files registered from HDFS or GA4GH

`hdfs://` paths and GA4GH datasets are no longer supported resource types (see the
[release notes](../release-notes/3.0.0/3.0.0.md#removed)). The two ids they were stored under
stay reserved in the database, and a row that still holds one is now rejected as it is read:

```
Biological data item resource type 5 (HDFS) is no longer supported. Files registered with it
have to be unregistered and, if still needed, re-registered from a supported resource type.
```

The rejection happens in the row mapper, so it fails the whole request — opening *any* dataset
that contains such a file, not only that file. Check whether you have any, while the old version
is still running:

```sql
SELECT bio_data_item_id, name, path, type
FROM catgenome.biological_data_item
WHERE type IN (5, 6);   -- 5 = HDFS, 6 = GA4GH
```

The usual answer is no rows, and then there is nothing to do here. Otherwise, for each row:
remove the file from every dataset that holds it (`ngb remove_dataset <dataset> <file>`), then
unregister it (`ngb del_file <id>`). If the data is still wanted, re-register it afterwards from
a supported location — local, `http(s)://`, `s3://`, `sws://` or `az://`.

**Do this on the old release**, before converting. It is the last version that can read those
rows, so once you have upgraded they can no longer be removed through the API or the UI and the
only way back is the backup.

## H2

The default configuration (`database.jdbc.url=jdbc:h2:file:/opt/catgenome/H2/catgenome`)
stores the database in `/opt/catgenome/H2/`. H2 1.3 wrote `catgenome.h2.db`; H2 2.x writes
`catgenome.mv.db`, so the two never overwrite each other and the old file is its own
backup.

You need both H2 jars. The new one ships inside the NGB jar; the old one is on Maven
Central as `com.h2database:h2:1.3.176`.

1. **Stop NGB.**

2. **Export the old database** with the *old* H2 jar. It has to be the old jar: the new one
   cannot open the file.

   ```bash
   java -cp h2-1.3.176.jar org.h2.tools.Script \
       -url  jdbc:h2:file:/opt/catgenome/H2/catgenome \
       -user catgenome -password '' \
       -script /tmp/catgenome-export.sql
   ```

3. **Patch two column declarations in the export.** Only needed if you use heatmap tracks,
   but harmless either way:

   ```bash
   sed -i 's/_CELL_VALUE DECIMAL,/_CELL_VALUE DOUBLE PRECISION,/' /tmp/catgenome-export.sql
   ```

   `HEATMAP.MIN_CELL_VALUE` and `HEATMAP.MAX_CELL_VALUE` were declared as bare `DECIMAL`,
   and the two H2 versions read that differently: 1.3 kept the stored scale, while 2.x takes
   it to mean "no decimal places" and rounds every value to a whole number as the dump is
   imported. NGB re-declares both columns as `DOUBLE PRECISION` on first start, but by then
   the import has already happened, so the scale has to be fixed in the dump. Those are the
   only two `DECIMAL` columns in the schema, so the command above cannot match anything else.

4. **Move the old files aside** so the new database starts from an empty directory:

   ```bash
   mkdir /opt/catgenome/H2-1.3-backup
   mv /opt/catgenome/H2/catgenome.*.db /opt/catgenome/H2-1.3-backup/
   ```

5. **Import into a 2.x database** with the *new* H2 jar:

   ```bash
   java -cp h2-2.3.232.jar org.h2.tools.RunScript \
       -url  'jdbc:h2:file:/opt/catgenome/H2/catgenome;NON_KEYWORDS=END,USER,VALUE' \
       -user catgenome -password '' \
       -script /tmp/catgenome-export.sql
   ```

   `NON_KEYWORDS=END,USER,VALUE` is not optional. H2 2.x made those three words reserved,
   and the NGB schema uses all three as identifiers — the `USER` table, and `VALUE` and
   `END` columns. Without it the import fails on the first `CREATE TABLE CATGENOME.USER`
   with `Syntax error ... expected "identifier" [42001-232]`. You only need it on this
   command line; NGB itself issues the equivalent `SET NON_KEYWORDS` on every connection it
   opens, so **no change to `catgenome.properties` is required**.

6. **Start NGB.** It picks up `catgenome.mv.db` and converts the schema history (below).

7. Once you are satisfied, delete `/opt/catgenome/H2-1.3-backup/` and
   `/tmp/catgenome-export.sql`.

## PostgreSQL

Nothing in this release *forces* a PostgreSQL server upgrade — the new driver still talks
to 9.6. But the old Flyway was the only reason NGB was held at 9.6, and 9.6 has been out
of support since November 2021, so this is the moment to move.

**Move to 16.** That is the version this release is built and tested against — in CI, in the
containerised development environment, and in the 9.6 → 16 upgrade this page describes. 17 is
inside the JDBC driver's supported range, which is where the "9.6 – 17" above comes from, but NGB
has not been run against it.

If you are staying on your current server, skip to
[what NGB does on first start](#what-ngb-does-on-first-start); there is nothing to do here.

To move to PostgreSQL 16, dump and restore in the ordinary way:

1. **Stop NGB.**

2. **Dump from the old server.** `pg_dumpall` also carries the roles, which matters for
   step 4:

   ```bash
   pg_dumpall -h old-host -U catgenome > /tmp/ngb-dumpall.sql
   ```

3. **Restore into the new server:**

   ```bash
   psql -h new-host -U postgres -f /tmp/ngb-dumpall.sql
   ```

4. **Reset the NGB database password.** This step is easy to miss and the failure looks
   like a wrong password. PostgreSQL 9.6 hashed passwords with md5; 16 defaults to
   `scram-sha-256` and its default `pg_hba.conf` will not accept an md5 hash, so the
   password that came across in the dump cannot be used to log in. Set it again on the new
   server, which stores it in the new format:

   ```sql
   ALTER ROLE catgenome WITH PASSWORD 'the same password as before';
   ```

5. **Point `catgenome.properties` at the new server** (`database.jdbc.url`) and start NGB.

### PostgreSQL-only schema convergence

NGB's PostgreSQL and H2 schemas had drifted apart over the years. This release converges
them on the H2 shape, which is the one the application code is written against. Four
migrations run automatically against a PostgreSQL database and one of them is visible to
administrators:

* **The predefined roles change.** PostgreSQL was seeding `ROLE_CYTOBANDS_MANAGER` and
  `ROLE_MAF_MANAGER`, which no NGB release has ever used on H2 and which are not part of
  the documented role set. They are **deleted, along with any grants of them** — if a user
  held `ROLE_MAF_MANAGER`, they lose it, and MAF file management falls back to
  administrators, as it always has on H2. Conversely `ROLE_WIG_MANAGER` was **missing** on
  PostgreSQL, which was a real defect: nothing but an administrator could manage WIG files.
  It is now created. Roles you created yourself are not touched.

  If any of your users held `ROLE_MAF_MANAGER`, decide before upgrading how those people
  should keep working — normally by granting the relevant permissions on the MAF datasets
  directly. And grant `ROLE_WIG_MANAGER` to whoever was supposed to have had it: until now,
  on PostgreSQL, only an administrator could manage WIG files.

  **The numeric ids of two surviving roles change** as the deleted ones make way:
  `ROLE_SEG_MANAGER` moves from 10 to 9 and `ROLE_TARGET_MANAGER` from 11 to 10, with their
  grants carried across. Nothing inside NGB refers to a role by number — the UI and
  `DefaultRoles` both go by name — but a script of your own that assigns roles by id through
  the API is now naming a different role, so check for one before you start.

* `VCF.MULTI_SAMPLE` becomes nullable, and `TASK_ORGANISM.ORGANISM` /
  `TASK_EXCL_ORGANISM.ORGANISM` change from `VARCHAR(250)` to `BIGINT`. Neither is visible
  through the UI or the API. The type change casts the stored values, which have always
  been NCBI taxonomy ids written from a numeric field. In the unlikely event that a row
  holds something non-numeric, the migration stops with
  `invalid input syntax for type bigint` and NGB will not start — fix or delete that row
  and start again, rather than working around the cast, because a non-numeric value there
  cannot have come from NGB.

* `BAM_COVERAGE.COVERAGE` changes from `NUMERIC` to `DOUBLE PRECISION`, which is what H2 has
  always used and what the application reads. No stored value changes.

One further migration runs on **both** flavours: `HEATMAP.MIN_CELL_VALUE` and
`MAX_CELL_VALUE` become `DOUBLE PRECISION` instead of bare `DECIMAL`. On PostgreSQL that is
a change of declared type only. On H2 it also matters for the dump — see step 3 of the H2
procedure above.

## What NGB does on first start

The tool NGB uses to version its schema, Flyway, went from 3.2.1 to 11.7.2. Two things
about the way it records applied migrations changed in between, and NGB handles both by
itself the first time it starts. **You do not need to run any Flyway command.**

* The layout of the history table changed (Flyway 4 dropped its `version_rank` column and
  moved the primary key to `installed_rank`). NGB detects the old layout and converts it.
* The checksum algorithm changed, so every migration recorded by the old version looks
  modified to the new one. NGB realigns the recorded checksums straight after converting
  the table.

You will see this in the log, once, on the first start after the upgrade:

```
WARN  Found a Flyway 3 schema history table CATGENOME.schema_version, written by NGB 2.7.x
      or earlier. Converting it to the current layout and realigning checksums; this happens
      once, on the first start of the upgraded server.
WARN  Schema history of CATGENOME converted and checksums realigned.
```

These two lines go to standard output — `catalina.out` under Tomcat, `docker logs` for the
container image. They are the only warnings NGB prints there; everything else on that stream
is an error.

Subsequent starts log nothing of the sort. If instead you see

```
Cannot convert the Flyway 3 schema history table ...
```

NGB will refuse to start rather than migrate a database it cannot reason about. That
should not happen on a database only NGB has written; if it does, restore your backup and
report it with the log.

## The other upgrade step

This release also changes the search-index format, which is a separate one-off procedure with its
own page: [Rebuilding the Lucene indexes](lucene-reindex.md). Nothing on this page depends on it
and it does not depend on anything here, but NGB will refuse to start until it is done, so do both
in the same maintenance window.

## Verifying the upgrade

Start NGB and check that the datasets and reference genomes you had before are all listed.
On the API, `GET /restapi/reference/loadAll` should return the same references as before
the upgrade, and the log should contain neither a Flyway error nor a `Migration ... failed`
line.
