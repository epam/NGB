# Test baseline

What a green run looks like, so that a red one can be read. **The suite is not clean: one test
fails on current code, and a second flaps.** From here on this list is the reference — a run that
is green except for this list is a pass. Anything else is your own breakage.

Recorded inside this environment (aarch64/colima) for **NGB 3.0.0**: JDK 21, Gradle 8.14.5,
Spring Boot 3.5.16, Spring Security 6.5.11, Flyway 11.7.2, Lucene 9.12.3, htsjdk 5.0.0,
AWS SDK v2 2.54.1, POI 5.5.1, biojava 7.2.6, against H2 2.3.232 and PostgreSQL 16.15.

| Suite | Command | Result |
|---|---|---|
| H2 | `make test` | 545 tests, **1 failed**, 21 skipped (~4.5 min) |
| PostgreSQL | `make test-pg` | 545 tests, **1 failed**, 21 skipped (~4 min on an idle host; 15 min if it shares the machine with a docker build and a few running servers — the PostgreSQL suite is the one that notices) |
| Static analysis | `make lint` | **green** — pmd clean, checkstyle 37 warnings in 14 files / 0 errors (~15 s) |
| CLI integration | `make cli-test` | 145 rows, **129 passed, 0 failed, 16 skipped** (~5 min) — see [`make cli-test`](#make-cli-test) |
| CLI unit | `./gradlew -p server/ngb-cli test` | 135 tests, **0 failed**, 0 skipped (~10 s). No make target of its own: `make jar` runs them, because `buildCli` is a `clean build` unless `-PnoTest` is passed. CI runs them as a step of `test-h2`. |

**The one failure is a live-network test**: `GffManagerTest.testLoadGenesTranscript`, which asserts
on a biotype Ensembl returns over REST. `PdbDataManagerTest.testParse` is the second network test;
it usually passes but flaps with RCSB's answer, so treat a 2nd failure there as external weather.
See [Live-data failures](#live-data-failures).

Which means: **a non-network failure is always a regression.** There is no documented schema or DAO
failure to hide behind on either flavour, and the two flavours do not differ — 545 / 1 / 21 on both.

CI passes `-PexcludeNetworkTests`, which drops exactly those two tests and nothing else
(`server/catgenome/build.gradle`), so the `test-h2` and `test-pg` jobs should read
543 tests / 0 failed / 21 skipped. A red CI run is a real regression by construction.

**Three conditions the numbers depend on.** Get any of them wrong and you will see extra failures
that are not yours:

1. **`make test-pg` needs a clean database.** `ngb_test` lives in the persistent `pg-data` volume
   and several tests do not clean up after themselves, so a second run on a dirty volume shows
   extra failures — 6 in `HeatmapManagerTest` ("File with name 'loadHeatmapTest' already exists"),
   `UrlShorterManagerTest` (`expected:<95d52dd9> but was:<alias>`), and others. Run `make reset-pg`
   first. Fixing that self-cleanup is worth doing and nobody has.
2. **`make test` needs a clean `contents/`.** `/contents/` in the repo root is gitignored scratch
   space (`files.base.directory.path`, plus every global Lucene index). Leftovers there used to make
   `HomologeneManagerTest.searchTest` pass and `TargetManagerTest.loadTargetsTest` fail; both are
   self-contained now, but if you get an inexplicable Lucene result, `rm -rf ../contents` and re-run.

   That command is sufficient today and was not always. The two test profiles omitted
   `pathway.index.directory`, `homologene.index.directory` and `bam.coverage.index.directory`, and
   the test contexts resolve placeholders with `ignore-unresolvable="true"`, so those three indexes
   were written to directories in the source tree named literally
   `server/catgenome/${pathway.index.directory}` and so on — outside `contents/`, gitignored
   (`.gitignore:13`), and never cleaned. `ncbi.index.directory` was `./contents/ncbi/`, i.e.
   relative to the test JVM's working directory, which is `server/catgenome/`, so it missed too.
   All four point at `@rootDirPath@/contents/` now.

   **If your tree ran the suite before NGB 3.0.0, delete those directories once**, or you will see
   nine `LuceneIndexVersionException` failures that are not yours — they hold Lucene 6 indexes and
   Lucene 9 cannot read them:

   ```bash
   rm -rf ../contents ../server/catgenome/contents "../server/catgenome/\${"*
   ```

3. **A database that already applied the 3.0.0 schema scripts needs resetting once.** The eight
   scripts this release adds had their header comments reworded after they were first applied here,
   which changes the checksum Flyway recorded for them. `migrate` validates before it runs, so the
   next start of a database that holds the old checksums fails with `Migration checksum mismatch`.
   No released NGB carries these scripts, so no installation is affected — only a working copy that
   ran them before the rewording. `make reset-pg` clears it — both for a running PostgreSQL instance
   and for the `ngb_test` database `make test-pg` uses, which condition 1 already says to reset;
   `make reset-ngb-data` for a running H2 instance. `make test` is immune: its database is
   `jdbc:h2:mem:test_catgenome`, built from the scripts on every run.

## H2: 1 live-data failure, and 1 that flaps

| Class | Cause |
|---|---|
| `GffManagerTest.testLoadGenesTranscript` | `expected:<protein_coding> but was:<protein_coding_CDS_not_defined>`. **Live external data, not a fixture problem** — see below. It has also been seen failing with `NullPointerException: Cannot invoke "java.util.List.isEmpty()" because the return value of …`, i.e. Ensembl answered with a field missing; same test, same cause, different weather. Only an Ensembl change, or a decision about network tests, will move it. |
| `PdbDataManagerTest.testParse` | `expected:<B> but was:<A>`. Live RCSB PDB data — see [Live-data failures](#live-data-failures). **Flaps**: it passes most of the time, and 2 failures is still a pass. |

### Why `GffManagerTest.testLoadGenesTranscript` is not a fixture bug

It reads like one — a biotype the parser did not expect. It is not, and it cannot be fixed by
editing the fixture or the parser:

- The assertion reads `testTranscript.getBioType()`. `setBioType` is called from exactly one place
  in the codebase: `ExtenalDBUtils.java:148`, from an `EnsemblExonVO` — i.e. from the **Ensembl REST
  response**, never from the local GTF.
- The fixture `Homo_sapiens.GRCh38.83.sorted.chr21-22.gtf` contains `protein_coding` 75,207 times
  and `protein_coding_CDS_not_defined` **zero** times.
- `protein_coding_CDS_not_defined` is a biotype Ensembl introduced in release 110 (2023).

So `geneTrackManager.loadGenesTranscript` is a live-network call (the same test also asserts on
UniProt domains, secondary structure and PDB entries, and is wrapped in
`catch (GeneReadingException e) { logger.info("database unavailable"); }`). The real question is
whether network-dependent assertions belong in the unit suite at all — the same one
`PdbDataManagerTest` raises. It is left red rather than loosened, because
`protein_coding_CDS_not_defined` is a *different* biotype (no CDS defined), not a rename of
`protein_coding`, so weakening the assertion would hide a real semantic change.

### Live-data failures

`PdbDataManagerTest.testParse` parses live RCSB PDB data and asserts on a chain identifier the
service returns, so it will keep flipping. Treat a failure there as external drift, not a
regression — and do not "fix" it by loosening the assertion, for the same reason as
`GffManagerTest.testLoadGenesTranscript`.

`BlatSearchManagerTest.testFind` and `testFindBlatReadSequence` used to belong in this section and
do not any more, which is worth knowing about because the trap is reusable. They failed with
`ExternalDbUnavailableException: Unexpected HTTP status: 302 Found` from UCSC — and they were never
meant to reach the network at all. The class ships `blat/data/testResponse.html` and stubs
`HttpDataManager.fetchData`, but `MockitoTestExecutionListener` creates `@Mock` fields at listener
order 1950 and `DependencyInjectionTestExecutionListener` overwrites the `@InjectMocks` field with
the Spring bean at 2000 — so the stub was injected into an object that was then thrown away, and
the tests had been calling UCSC since 2017. One `MockitoAnnotations.openMocks(this)` in `@Before`
fixes it; both are offline and deterministic now. **If a `@Mock`/`@InjectMocks` test in this suite
starts depending on the network, that is where to look.**

The default `blat.search.url` was separately wrong and is now `https://genome.ucsc.edu/cgi-bin/hgBlat`
(scheme alone would not have worked: the certificate on `genome.cse.ucsc.edu:443` does not carry
that name in its SANs). BLAT search against UCSC's public endpoint still does not work from any NGB
version — UCSC answers programmatic `hgBlat` queries with a bot-protection page, which
`PSLRecordParser` reads as zero hits — so the feature needs `blat.search.url` pointed at a BLAT
service that will answer. That is in the release notes and in
`docs/md/installation/standalone.md` rather than left as a silent empty result.

## PostgreSQL: the same failures as H2

Identical, number for number. The list below is the set of PostgreSQL-only failures that **used** to
exist, kept rather than deleted because it is the specification for what the schema-convergence
migrations must keep true: if one of these comes back, the two script sets have drifted apart again.

| Count | Class | Cause | Fixed by |
|---|---|---|---|
| 4 | `BookmarkDaoTest.testSaveLoadBookmark`, `testAllItemTypes`, `VcfFileDaoTest.testSaveLoadVcfFile`, `testSaveLoadSamples` | `catgenome.vcf.multi_sample` was `NOT NULL` in the PostgreSQL script set and nullable in the H2 one; `VcfFileDao` inserts `null`. | `v2026.08.21_12.00__align_vcf_multi_sample_with_h2.sql` |
| 2 | `BlastTaskDaoTest.testDeleteOrganisms`, `testDeleteExclOrganisms` | `task_organism.organism` / `task_excl_organism.organism` were `character varying` on PostgreSQL and numeric on H2, while `BlastTaskDao.deleteOrganisms` emits `where organism = 1`. `ERROR: operator does not exist: character varying = integer`. | `v2026.08.21_12.10__align_task_organism_with_h2.sql` |
| 1 | `RoleDaoTest.testLoadRolesWithUsers` | `expected:<11> but was:<12>` — the two script sets seeded a different number of predefined roles. | `v2026.08.21_12.20__align_predefined_roles_with_h2.sql` |
| 1 | `TargetManagerTest.filterTargetsByOwnerTest` | `TargetManager.load` built `WHERE target_id IN ()` for an empty id set. H2 1.3 accepted it, PostgreSQL does not. **Was a real production bug**: any target filter matching nothing failed on PostgreSQL. | `TargetManager`, and it also stopped the test being dirty-state-sensitive |

The PostgreSQL suite exercises the ACL code, which it previously could not. Treat a new
context-startup failure in `*SecurityServiceTest` as real — those 16 tests are the only thing
standing between a broken NGB security expression and a green build.

## Static analysis: green

`make lint` runs `checkstyleMain pmdMain` on the server module with **Checkstyle 11.1.0** and
**PMD 7.26.0**, and both pass.

| Module | Command | Checkstyle | PMD |
|---|---|---|---|
| `server/catgenome` | `make lint` | 37 warnings in 14 files, **0 errors** | clean |
| `server/catgenome`, tests too | `./gradlew -p server/catgenome checkstyleMain checkstyleTest pmdMain pmdTest` | +11 warnings in 3 test files, **0 errors** | clean |
| `server/ngb-cli` | `./gradlew -p server/ngb-cli checkstyleMain checkstyleTest pmdMain pmdTest` | 6 warnings main + 1 test, **0 errors** | clean |

Reports: `server/<module>/build/reports/{checkstyle,pmd}/main.html`.

**The warnings do not fail the build**, by long-standing choice: `checkstyle.xml` sets
`severity=warning` at `Checker` level, and Gradle's `Checkstyle` task fails only on errors
(`maxWarnings` defaults to `Integer.MAX_VALUE`). Checkstyle 7.2 reported one of them; 11.1.0 reports
37 because eight years of new checks and refined defaults landed in between — mostly `[Indentation]`
in generated-looking `externaldb/bindings/*` VOs, a few `[FinalClass]`, and
`CustomChatResponse.java`'s member name `finish_reason` (it mirrors a JSON field). Nothing is
suppressed and no severity was lowered. Turning the warnings into errors is a worthwhile clean-up
and a code-style change, not a build fix.

PMD is clean with **zero** violations, and worth keeping that way: a red static-analysis baseline
hides real regressions. Two rules were consciously narrowed rather than carried across from PMD 5
(`SuspiciousConstantFieldName` dropped, `ClassNamingConventions` restored to its PMD 5 patterns) —
both reasoned about in the rulesets' own header comments. Two deprecation nags are expected on every
`ngb-cli` run: `AvoidCatchingNPE` and `AvoidLosingExceptionInformation` are "scheduled for removal
from PMD in PMD 8.0.0". They still work; whoever moves to PMD 8 replaces them. Note also that PMD 7's
`AvoidDuplicateLiterals` counts a literal used four times over as a violation where 6.55 did not.

## `make cli-test`

**145 rows: 129 passed, 0 failed, 16 skipped.**

The 16 skips are the rows whose names start with `#FAILS`, all marked that way in December 2018
(`a7d81db9`); `cli-tests.gradle` counts them and skips them.

The fixtures are **generated** from `server/catgenome/src/test/resources/templates` by
`e2e/cli/prepare_test_data.sh`, so the suite needs no network — it used to download 19 files from
`http://ngb.opensource.epam.com/distr/data/tests/` with `wget -r`, and that host is NXDOMAIN
(`opensource.epam.com` itself resolves, so it is that one name that is gone). That is why the
`cli-e2e` job in `.github/workflows/build.yml` can run it too.

Two things to know if you edit it. Expectations in `e2e/cli/testcases.csv` are dated in comments to
the commit that changed the behaviour they assert on — the file went unrun between 2018 and 2026 and
`develop` moved out from under it, so ten rows were stale rather than broken. And `example.gff.gz`
has to be **BGZF**, not plain gzip, because NGB tabix-indexes any `.gz` it registers; a plain-gzip
fixture fails at registration in a way that reads like a parser bug.

`cli-test` runs `AUTH_MODE=none` against a throwaway H2 database. The quickest check of a CLI change
against a *stateful* server is still by hand:

```bash
make up
docker-compose --profile cli up -d cli
docker-compose exec cli ngb reg_ref /ngs/A3.fa --name test_ref      # + repeat: must fail "already exists"
docker-compose exec cli ngb list_ref
docker-compose exec cli ngb reg_file test_ref /ngs/agnX1.09-28.trim.dm606.realign.bam --name test_bam
docker-compose exec cli ngb search test_bam
docker-compose exec cli ngb reg_dataset test_ref test_ds test_bam
docker-compose exec cli ngb del_file test_bam    # must fail: used in projects
docker-compose exec cli ngb del_dataset test_ds && docker-compose exec cli ngb del_file test_bam
```

Registration, listing, search, dataset composition, deletion and both error paths. Fixtures for it
live in the repo under `server/catgenome/src/test/resources/templates/`; copy them into
`.devenv/data/ngs/` (which is `/ngs` in both containers).

## What a green suite does not tell you

**It does not tell you the server boots.** The unit suites build plain Spring contexts from the XML
and never go through Boot's auto-configuration, never start Tomcat, never build an image and never
unpack an archive. Things that have been green-suite-invisible here and were found by starting a
real server:

- a Boot auto-configuration colliding with an NGB bean of the same name (`FlywayAutoConfiguration`
  vs NGB's own `flyway` bean — a hard context failure, excluded in `Application.java`);
- log configuration discarding the messages a procedure depends on (the shipped appenders filter at
  `ERROR`; one-time schema-history and Lucene-check notices went nowhere until a `WARN`-threshold
  console appender was bound to those classes);
- a Lucene index that could not be **written** because two fields in one document disagreed on
  `IndexOptions`, which Lucene 6 tolerated and 9 does not;
- htsjdk's length probe becoming a `HEAD` request, which turns a pre-signed S3 URL into a silently
  empty file.

**It does not tell you a dependency change is safe**, either: there is no xlsx fixture and no test
mentions XSSF, nothing imports `org.biojava.nbio.structure`, the S3 tests stub the client away, and
`GffManagerTest.testRegisterGbk` asserts that Genbank registration produced a file, not what is in
it. A green suite means "nothing that was covered broke".

The checks that cover the gap are scripted, and are the ones to run after touching a parser, an
index, a cloud path or the packaging:

```bash
bash scripts/prepare-track-fixtures.sh    # stage the fixtures into /ngs/tracks, once
make up && make smoke
make verify-tracks                        # every track type, through the parsers
make verify-lucene                        # all 18 Lucene read paths
make up-cloud && make verify-cloud        # s3:// and sws://, byte-for-byte against a local read
make up-saml && make smoke-saml && make cli-token   # SAML SSO -> RS512 JWT
```

`verify-tracks.sh` covers BED, GFF/GTF, GenePred, VCF, BedGraph, BigWig, SEG, BAM, CRAM, plain and
bgzip+tabix, and three remote variants including one whose `HEAD` is answered with 403; it compares
CRAM against the BAM it was made from and each remote read against the same file on disk, because
those are the comparisons a parser regression cannot survive. `verify-cloud.sh` stands **MinIO** up
as a `cloud` compose profile and reads registered `s3://` / `sws://` tracks, `fileUrl=` tracks and
pre-signed download URLs against it. `verify-lucene.sh`'s output is byte-identical to a recording
taken on Lucene 6 before the upgrade, both kept in `.devenv/fixtures/pre-migration/lucene6/`.

**MAF cannot be verified through the server at all** — `MafController` was deleted in `562b6a6d`
(Dec 2018), so `MafManagerTest.testRegisterMaf` is the whole of its coverage, and the MAF row of
`verify-tracks.sh` is skipped.

## Known gaps

Things nothing in this repository has ever verified. None of them is a known failure; they are
places where a first report from the field would be the first evidence either way.

- **The `az://` (Azure Blob) path has never been exercised against a live service.**
  `AzureBlobClient` builds `https://%s.blob.core.windows.net` from a hard-coded template, so Azurite
  cannot stand in for it, and no Azure subscription was available. Compile- and unit-verified only.
  The `s3://` / `sws://` paths that share the reader stack are covered by `verify-cloud`.
- **`sws://` (Swift) has not been read from a live endpoint** either; MinIO stands in for it.
- **The LLM round trip has never been exercised** — no API keys for any provider. The clients are
  wired and compile; nothing has been observed on the wire.
- **PostgreSQL 17 has never been run**, and neither has any version between 9.6 and 16. 16 is what
  this environment and CI run. The wider range quoted in the documentation is the JDBC driver's.
- **No upgrade has been performed on a production-sized database or index set.** Both conversions
  were exercised against a purpose-built pre-3.0.0 fixture (60 `schema_version` rows, real ACL and
  biological-data-item content) in `.devenv/fixtures/pre-migration/`; timings and failure modes at
  real scale are unknown. Rehearse on a copy.
- **`.github/workflows/build.yml` has not run on GitHub.** Every job mirrors a `make` target that
  passes in these containers and the YAML parses, but no run exists — the branch has not been
  pushed. Expect the first push to shake out runner-specific problems; the likely candidates are the
  Node 14.17.5 `cache: npm` step and JDK toolchain discovery in `.github/actions/setup-jdks`.

Defects that are known, reproducible and deliberately unfixed are in [`ISSUES.md`](../ISSUES.md)
instead. Container vulnerability scanning is in [`SECURITY-SCAN.md`](SECURITY-SCAN.md).

## Reproducing

```bash
make reset-pg && make test-pg   # PostgreSQL - the reset matters, see above
rm -rf ../contents && make test # H2 - the clean scratch dir matters, see above
make lint
make test-one T=VcfManagerTest  # single class
make cli-test                   # the CLI against a running server; needs dist/catgenome-h2.jar
```

HTML reports land in `server/catgenome/build/reports/tests/test/index.html`, XML in
`server/catgenome/build/test-results/`. Note that both database flavours write to the same
directory, so whichever ran last is what you are reading.
