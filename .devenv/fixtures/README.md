# `.devenv/fixtures/` — databases and indexes written by NGB 2.x (not committed)

Everything under this directory is gitignored except this file (`/.devenv/fixtures/*` plus
`!/.devenv/fixtures/README.md` in the repo's `.gitignore`). The contents are local state:
binary database files and a scratch Gradle project. This README is the recipe for rebuilding
them, which is the part worth keeping — and `scripts/verify-lucene.sh` points at it for the
data it expects, so it is committed.

They exist so that the one thing no unit test can cover is coverable: **taking a database and a
`contents/` tree written by NGB 2.x and running the current jar against them**, which is what
every operator upgrading to 3.0.0 does (`docs/md/installation/database-upgrade.md` and
`docs/md/installation/lucene-reindex.md`).

None of it can be rebuilt from the current tree — the files have to be *written* by code that
still uses Flyway 3.2.1, H2 1.3.176, the PostgreSQL 9.4 driver and Lucene 6.6.0. That means
checking out a pre-3.0.0 commit **in place** (worktrees are invisible to the `.devenv`
containers, see `CLAUDE.md`).

## `pre-migration/`

Captured at `5680365b`, the last commit before the Flyway/H2/PostgreSQL bump, with the
containers stopped first so the H2 file is compacted and lock-free.

| Path | What |
|---|---|
| `h2/catgenome.h2.db` | H2 1.3.176 database, cleanly shut down |
| `h2/catgenome.trace.db` | its trace log, kept only because H2 writes it |
| `h2/ngb-h2-contents.tar.gz` | the matching `/opt/ngb/contents` tree (Lucene indexes, `.nginx` etc.) |
| `pg/pg96-dumpall.sql` | `pg_dumpall` of the 9.6 cluster — this is the one to restore from |
| `pg/pg96-datadir.tar.gz` | raw 9.6 `PGDATA`, kept so a 9.6 container can be resurrected |
| `pg/ngb-pg-contents.tar.gz` | the matching `/opt/ngb/contents` tree |

Keep a second copy **outside the repository** —
`pre-migration-fixtures-5680365b.tar.gz` somewhere in your home directory — because
`make reset-pg`, `make reset-ngb-data` and changing `PG_VERSION` all destroy the live volumes,
and a `git clean -xdf` takes this directory with it.

### How they were made

```sh
git checkout 5680365b            # in place; do not use a worktree
cd .devenv
make jar-fast                    # H2 flavour, still Flyway 3.2.1
make up && make smoke
# register real data through the CLI so the fixture is not just an empty schema:
#   test_ref, dm6, demo_bam, demo_vcf, demo_genes, demo_vcf_dm6, dataset demo_ds
docker-compose stop ngb-h2       # clean shutdown => compacted, lock-free .h2.db
docker cp "$(docker-compose ps -q ngb-h2)":/opt/ngb/H2/catgenome.h2.db fixtures/pre-migration/h2/

make jar-pg-fast
make up-pg && make smoke-pg
# register the same data again against the PostgreSQL instance
docker-compose exec -T postgres pg_dumpall -U catgenome > fixtures/pre-migration/pg/pg96-dumpall.sql
docker-compose stop ngb-pg postgres
docker run --rm -v ngb-dev_pg-data:/d -v "$PWD/fixtures/pre-migration/pg":/o alpine \
    tar czf /o/pg96-datadir.tar.gz -C /d .
```

Verify the H2 one is genuine rather than an empty schema before trusting it: it should have
59 tables in `CATGENOME`, 60 rows in `CATGENOME."schema_version"` (the Flyway 3 baseline plus
59 migrations), and rows in `CATGENOME.REFERENCE`. Note the quoting — Flyway 3 created the
history table as quoted lowercase, so `catgenome.schema_version` does **not** resolve.

## `pre-migration/lucene6/`

Captured at `6be43c24`, the last commit whose Lucene is 6.6.0. It is what the startup guard is
tested against: starting the current jar on a `contents/` restored from here has to produce the
clear "rebuild these indexes" message for both a global index and a per-file feature index —
not an `IndexFormatTooOldException` stack trace.

That guard cannot be tested any other way. **Lucene 9 cannot write a 6.x index**, so this
fixture is the only Lucene 6 index that will ever exist again, and `make reset`,
`make reset-ngb-data` and `rm -rf ../contents` all destroy the live copies.

| Path | What |
|---|---|
| `ngb-h2-contents.tar.gz` | the whole `/opt/ngb/contents` tree — 21 MB, 16 Lucene 6 index directories, see below |
| `ngb-h2-db.tar.gz` | the matching `/opt/ngb/H2/catgenome.mv.db`, taken after `docker-compose stop ngb-h2` so it is compacted and lock-free |
| `contents-listing.txt` | `tar tzf` of the above, for grepping without unpacking |
| `verify-lucene-6.6.0.txt` | output of `.devenv/scripts/verify-lucene.sh` against this data on Lucene 6.6.0 — the 2.x baseline the post-reindex run has to match |
| `verify-lucene-9.12.3-after-reindex.txt` | the same 18 probes after the upgrade and the documented reindex. **Byte-identical to the line above** — that a rebuild returns the same answers, and not merely that it succeeds, is the thing the unit suite cannot express |
| `test-tree-lucene6-dirs.tgz` | the *test* suite's Lucene 6 indexes, from `server/catgenome/` — `contents/ncbi/` plus the six `${…index.directory}` directories the two test profiles used to leave in the source tree. 107 K. Nothing writes them there any more; see the second condition in [`TEST-BASELINE.md`](../TEST-BASELINE.md) if an old tree of yours still has them |
| `from-test-suite/` | an earlier, smaller capture: just `taxonomy/` and `targets/` as written by `make test`. Superseded by the tarballs; kept because it is 324 K and needs no unpacking |

Keep a second copy outside the repository here too —
`lucene6-fixture-6be43c24.tar.gz` — because a `git clean -xdf` would take this directory with it
and, as above, there is no way to regenerate it.

The database and the contents tree have to be restored **together**: the per-file feature
index paths embed `BiologicalDataItem` ids, and the target/pathway/coverage rows in the
database are what the global indexes are keyed against.

### What is in it

All seven index types NGB writes: every `*.index.directory` property in
`server/catgenome/profiles/*/catgenome.properties`, plus the per-file indexes under
`files.base.directory.path`:

```
42/VCF/1/index.luc   42/VCF/2/index.luc   42/genes/1/index.luc     per-file feature indexes
taxonomy/            homologene/          ncbi/gene.ids/           global
pathway/             coverage/  (41 MB, segments_27x)
targets/{opentargets.target, opentargets.disease, opentargets.disease.association,
         opentargets.drug.association, dgidb.drug.association,
         pharmgkb.gene, pharmgkb.drug, pharmgkb.drug.association, pharmgkb.disease}
```

`targets/ttd.*` is absent: the repo ships no TTD fixture. `HomologManager` (`/homolog/import`)
has no index directory at all — it is database-backed, despite sitting next to Homologene.

That these really are Lucene 6 is checkable without a JVM — `xxd -l 96 taxonomy/segments_1`:

```
00000000: 3fd7 6c17 0873 6567 6d65 6e74 7300 0000  ?.l..segments...
00000020: ab01 3106 0600 0000 0000 0000 0005 0000  ..1.............
00000050: 6365 6e65 3632 ffff ffff ffff ffff 0000  cene62..........
```

`segments` format version `6` (`SegmentInfos.VERSION_53`), writer version `06 06 00` = 6.6.0,
per-segment codec `Lucene62`. Lucene 9's `MIN_SUPPORTED_MAJOR` is 8, so it rejects this with
`IndexFormatTooOldException` — which is exactly the throw NGB's own guard pre-empts, with
`LuceneIndexVersionException` and a message naming the directory and what rebuilds it.

### How it was made

The registered files come from the `pre-migration/` recipe above (`test_ref`, `dm6`,
`demo_bam`, `demo_vcf`, `demo_genes`, `demo_vcf_dm6`, datasets `demo_ds` and `demo_ds_dm6`) —
registering a VCF and a GTF into a dataset is what writes the per-file feature indexes. The
global indexes on top of that are one script:

```sh
git checkout 6be43c24            # in place; do not use a worktree
cd .devenv
make jar-fast && make up && make smoke
scripts/build-global-indexes.sh  # stages /ngs/index-sources, then the 7 import calls
scripts/verify-lucene.sh         # 18 probes; all must pass before capturing
docker-compose stop ngb-h2       # clean shutdown => compacted, lock-free catgenome.mv.db
OUT=$PWD/fixtures/pre-migration/lucene6; mkdir -p "$OUT"
docker run --rm -v ngb-dev_ngb-h2-contents:/d:ro -v "$OUT":/o alpine \
    tar czf /o/ngb-h2-contents.tar.gz -C /d .
docker run --rm -v ngb-dev_ngb-h2-db:/d:ro -v "$OUT":/o alpine \
    tar czf /o/ngb-h2-db.tar.gz -C /d .
```

`build-global-indexes.sh` stages `server/catgenome/src/test/resources/{taxonomy,homologene,
ncbi,opentargets,pharmgkb,dgidb,pathway}` into `.devenv/data/ngs/index-sources/` — a byte-for-
byte copy, verified with `diff -rq`. It has to be staged there because `ngb-h2` mounts only
`./data/ngs:/ngs`, not `/workspace`.

### Restoring it

```sh
cd .devenv
docker-compose stop ngb-h2
F=$PWD/fixtures/pre-migration/lucene6
docker run --rm -v ngb-dev_ngb-h2-contents:/d -v "$F":/f:ro alpine \
    sh -c 'rm -rf /d/* && tar xzf /f/ngb-h2-contents.tar.gz -C /d'
docker run --rm -v ngb-dev_ngb-h2-db:/d -v "$F":/f:ro alpine \
    sh -c 'rm -rf /d/* && tar xzf /f/ngb-h2-db.tar.gz -C /d'
docker-compose up -d ngb-h2 && make logs
```

## `probe/`

Scratch harness for questions that can only be answered by running two incompatible Flyway/H2
versions side by side. Kept because re-deriving it is an hour's work and the next Flyway or H2
bump will want the same trick.

A one-file Gradle project with **two** configurations, `oldStack` and `newStack`, so that
Flyway 3.2.1 + H2 1.3 and Flyway 11.7.2 + H2 2.3 can coexist without dependency resolution
collapsing them to one version. `./gradlew cp` writes `cp-old.txt` and `cp-new.txt`.

| File | Answers |
|---|---|
| `old/ParseOld.java` | how Flyway **3.2.1** splits a migration filename into version + description, using its own `MigrationInfoHelper` |
| `new/ParseNew.java` | the same for Flyway **11.7.2**, using its own `ResourceNameParser` — diff the two outputs |
| `new/FlywayProbe.java` | drives Flyway 11.7.2 with NGB's real configuration against any database: prints `info`, `validate`, then optional `repair` / `revalidate` / `migrate` steps passed as trailing arguments |

Run it against a copy, never against a fixture:

```sh
cd .devenv && docker-compose exec builder bash -lc '
  cd /workspace/.devenv/fixtures/probe
  with-java21 java -cp "$(cat cp-new.txt):newcls" FlywayProbe \
    "jdbc:h2:file:/path/to/copy;NON_KEYWORDS=END,USER,VALUE" catgenome "" CATGENOME \
    "filesystem:/workspace/server/catgenome/src/main/resources/database/catgenome/h2" \
    repair revalidate migrate'
```

What it established, and so why `FlywayMigrator` is the shape it is: the two versions parse all
118 migration filenames into identical version + description pairs, so **no script had to be
renamed** — `diff-h2.txt` and `diff-postgres.txt` are empty because that was the answer. What did
have to be handled in code is the Flyway 10 checksum algorithm and the new schema-history table
shape, both of which `FlywayMigrator` converts in place on first start.

What is left here is the harness plus those outputs (`A.txt`/`B.txt` are the two parsers'
verdicts on all 118 filenames; `schema-*.txt` are the pre/post-import H2 column dumps). The
scratch working copies of the databases and of the migration script directories were deleted on
purpose: stale copies of `database/catgenome/{h2,postgres}` sitting next to the real ones are a
trap, and every database here is re-derivable from `pre-migration/`. Copy a fixture again if you
need one.
