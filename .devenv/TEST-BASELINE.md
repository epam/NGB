# Test baseline for the Java 21 migration

Recorded on 2026-08-20 at the end of **migration Phase 0**, inside this environment (JDK 8,
Gradle 3.3, H2 1.3.176 / PostgreSQL 9.6, aarch64/colima). **These tests still fail on the
current code.** From here on this list is the reference: a run that is green except for this
list is a pass. Anything else is your own breakage.

| Suite | Command | Result |
|---|---|---|
| H2 | `make test` | 537 tests, **2 failed**, 26 skipped (~3.5 min) |
| PostgreSQL | `make test-pg` | 537 tests, **10 failed**, 26 skipped (~3.5 min) |
| Static analysis | `make lint` | **green** — checkstyle and pmd both clean (~20 s) |

Phase 0 took this from 19 H2 / 65 PostgreSQL / red lint. What it fixed is at the bottom.

**Two conditions the numbers depend on.** Get either wrong and you will see extra failures
that are not yours:

1. **`make test-pg` needs a clean database.** `ngb_test` lives in the persistent `pg-data`
   volume and several tests do not clean up after themselves, so a second run on a dirty
   volume shows 19 failures instead of 10 — 6 in `HeatmapManagerTest` ("File with name
   'loadHeatmapTest' already exists"), `UrlShorterManagerTest`
   (`expected:<95d52dd9> but was:<alias>`), `TargetManagerTest.filterTargetsByOwnerTest`,
   `RoleDaoTest`. Run `make reset-pg` first. Fixing that self-cleanup is worth doing but is
   not in the migration's path.
2. **`make test` needs a clean `contents/`.** `/contents/` in the repo root is gitignored
   scratch space (`files.base.directory.path`, plus the taxonomy and targets Lucene indexes).
   Leftovers there used to make `HomologeneManagerTest.searchTest` pass and
   `TargetManagerTest.loadTargetsTest` fail; both are now self-contained, but if you get an
   inexplicable Lucene result, `rm -rf ../contents` and re-run.

## H2: 2 failures

| Class | Cause | Survives the migration? |
|---|---|---|
| `GffManagerTest.testLoadGenesTranscript` | `expected:<protein_coding> but was:<protein_coding_CDS_not_defined>`. **Live external data, not a fixture problem** — see the note below. | Yes. Unaffected by every phase; only an Ensembl change or a decision about network tests will move it. |
| `VcfManagerTest.testLoadSmallScaleVcfFileGa4GH` | `AccessDeniedException: Parameter path doesn't fall into 'ngs.data.root.path'`. `UrlValidatorService.isRemotePath` does not list `GA4GH`, so a variant-set id is validated as a local filesystem path. | No — **disappears in Phase 1**, which deletes GA4GH. |

### Why `GffManagerTest.testLoadGenesTranscript` is not a fixture bug

The migration plan assumed this was "fixture vs parser expectation, likely to move again in
Phase 7 when htsjdk changes". It is neither. Evidence:

- The assertion reads `testTranscript.getBioType()`. `setBioType` is called from exactly one
  place in the codebase: `ExtenalDBUtils.java:148`, from an `EnsemblExonVO` — i.e. from the
  **Ensembl REST response**, never from the local GTF.
- The fixture `Homo_sapiens.GRCh38.83.sorted.chr21-22.gtf` contains `protein_coding` 75,207
  times and `protein_coding_CDS_not_defined` **zero** times.
- `protein_coding_CDS_not_defined` is a biotype Ensembl introduced in release 110 (2023).

So `geneTrackManager.loadGenesTranscript` is a live-network call (the same test also asserts
on UniProt domains, secondary structure and PDB entries, and is wrapped in
`catch (GeneReadingException e) { logger.info("database unavailable"); }`). Editing the
fixture or the parser cannot fix it, and htsjdk cannot move it. The real choice is whether
network-dependent assertions belong in the unit suite at all — the same question
`PdbDataManagerTest` raises. Deliberately left red rather than loosened, because
`protein_coding_CDS_not_defined` is a *different* biotype (no CDS defined), not a rename of
`protein_coding`, so weakening the assertion would hide a real semantic change.

### Also network-dependent — passes today, may not tomorrow

`PdbDataManagerTest.testParse` parses live RCSB PDB data and asserted
`expected:<B> but was:<A>` when the previous baseline was taken. It passes as of this
recording. Treat a failure there as external drift, not regression.

## PostgreSQL: 10 failures

All 10 are pre-existing, and 8 of them are the **two Flyway script sets having diverged**.
None are caused by the environment. See "Latent bugs found" in `JAVA21-MIGRATION-PLAN.md`.

| Count | Class | Cause | Survives the migration? |
|---|---|---|---|
| 4 | `BookmarkDaoTest.testSaveLoadBookmark`, `testAllItemTypes`, `VcfFileDaoTest.testSaveLoadVcfFile`, `testSaveLoadSamples` | `catgenome.vcf.multi_sample` is `NOT NULL` in the PostgreSQL script set and nullable in the H2 one; `VcfFileDao` inserts `null`. | Should be settled in **Phase 5** when the two script sets are reviewed. |
| 2 | `BlastTaskDaoTest.testDeleteOrganisms`, `testDeleteExclOrganisms` | `task_organism.organism` / `task_excl_organism.organism` are `character varying` in the PostgreSQL script set and numeric in the H2 one, while `BlastTaskDao.deleteOrganisms` emits `where organism = 1`. `ERROR: operator does not exist: character varying = integer`. | Same — **Phase 5**. |
| 1 | `RoleDaoTest.testLoadRolesWithUsers` | `expected:<11> but was:<12>` — the two script sets seed a different number of predefined roles (already documented in `README.md`). | Same — **Phase 5**. |
| 1 | `TargetManagerTest.filterTargetsByOwnerTest` | `TargetGeneDao.loadTargetGenes` emits `WHERE target_id IN ()` for an empty id set. H2 1.3 accepts it, PostgreSQL does not. **Real production bug**: any target filter matching nothing fails on PostgreSQL. | Yes, until someone fixes the DAO. Nothing in the migration touches it. |
| 1 | `GffManagerTest.testLoadGenesTranscript` | Same live-Ensembl failure as on H2. | Yes. |
| 1 | `VcfManagerTest.testLoadSmallScaleVcfFileGa4GH` | Same GA4GH failure as on H2. | No — **Phase 1**. |

The PostgreSQL suite now exercises the ACL/JWT/auth code, which it previously could not
(see below). Treat a new context-startup failure in `*SecurityServiceTest` /
`JwtAuthenticationTest` / `AuthManagerTest` as real.

## Static analysis: green

`make lint` runs `checkstyleMain pmdMain` and both pass. One non-fatal checkstyle **warning**
remains (`CustomChatResponse.java:44`, member name `finish_reason` — it mirrors a JSON field).
Reports: `server/catgenome/build/reports/{checkstyle,pmd}/main.html`.

Keep it green. PMD and checkstyle both move in Phase 2, and a ruleset rewrite on top of an
already-red baseline would hide real regressions.

## What Phase 0 changed

| Fix | Effect |
|---|---|
| Deleted the stale committed Tribble index `templates/Felis_catus.vcf.idx` (recorded 5,898 bytes for a 5,995-byte file, and embedded the original author's absolute path) | −5 `VcfManagerTest` failures on both flavours |
| `TestAbstractFeatureReader` now builds its own Tribble index over a temp-dir copy in `@Before` | Keeps `requireIndex = true` satisfiable without that fixture. htsjdk only auto-builds a missing index on the managed `FileManager` path, **not** in `TribbleIndexedFeatureReader(..., requireIndex = true, ...)` — deleting the `.idx` alone breaks `testTribbleConstructors`. |
| `BlastTaskDaoTest` now sets `blastTaskId` (the column has been `NOT NULL` since `v2024.03.21_19.00__blast_task_id.sql`, which backfilled existing rows with `BLAST_TASK_ID = TASK_ID`) | −9 failures on both flavours |
| Removed the `database.*` block from `test-catgenome-acl.properties` and `test-catgenome-auth.properties` | −≈45 PostgreSQL failures. Those files hardcoded `jdbc:h2:mem:test_catgenome`, and because `@TestPropertySource` feeds the Environment — which `PropertySourcesPlaceholderConfigurer` consults *before* its local `properties-ref` map — they shadowed the flavour-matched `test-catgenome.properties` that `applicationContext-test.xml` already loads from `profiles/<flavour>/`. Deleting the block is enough; the files did not have to be duplicated per flavour. |
| `TargetManagerTest.loadTargetsTest` no longer asserts manager-level paging | −1 on both flavours. Commit `cdb4b2bb` (Feb 2024) deliberately moved paging out of `TargetManager.load` into `TargetController.loadTargets`, so a page is cut from the ACL-filtered list; the test was never updated. It was *not* leftover Lucene state, as previously assumed. |
| `HomologeneManagerTest` builds the taxonomy Lucene index in `@Before` | −1. `searchHomologenes` → `setGeneSpeciesNames` → `taxonomyManager.searchOrganismsByIds` opens a suite-shared index directory the repo does not ship, so the test only passed when `TaxonomyManagerTest` happened to run first. Built in setup rather than committed as a fixture, because a committed Lucene index would become unreadable in Phase 6 (D8). |
| Rewrote the 4 PMD violations instead of suppressing them: `EnsemblDataManager.fetchNcbiId` null-checks the deserialised VO rather than catching `NullPointerException`; `AlignmentManager.processParasiteTargets` catches `IOException \| ParseException` | `make lint` green |
| Fixed the stray trailing apostrophe in the H2 JDBC URL | Removes a landmine for H2 2.x in Phase 5 |
| Added JDK 17 to the toolbox (`with-java17`, `use-java 17`) and to the app entrypoint / compose (`NGB_JAVA_VERSION=17`) | Phase 2 runs on 17 |

## Reproducing

```bash
make reset-pg && make test-pg   # PostgreSQL - the reset matters, see above
rm -rf ../contents && make test # H2 - the clean scratch dir matters, see above
make lint
make test-one T=VcfManagerTest  # single class
```

HTML reports land in `server/catgenome/build/reports/tests/test/index.html`, XML in
`server/catgenome/build/test-results/`. Note that both suites write to the same directory, so
whichever ran last is what you are reading.
