# Test baseline for the Java 21 migration

First recorded on 2026-08-20 at the end of **migration Phase 0**; re-measured at the end of
**Phase 1**, of **Phase 2**, of **Phase 3**, of **Phase 4**, of **Phase 5**, of **Phase 6** and of
**Phase 7**,
inside this environment. The first two recordings were on JDK 8 / Gradle 3.3 / Boot 1.5, the third
on JDK 17 / Gradle 7.6 / Boot 2.7.18, the next two on H2 1.3.176 / PostgreSQL 9.6; **the numbers
below are Phase 7's, on JDK 21 / Gradle 8.14.5 / Boot 3.5.16 / Spring Security 6.5.11 / Flyway
11.7.2 / Lucene 9.12.3 / htsjdk 5.0.0, against H2 2.3.232 and PostgreSQL 16.15** (aarch64/colima).
**These tests still fail on the current code.** From here on this list is the reference: a run that
is green except for this list is a pass. Anything else is your own breakage.

| Suite | Command | Result |
|---|---|---|
| H2 | `make test` | 534 tests, **3–4 failed**, 21 skipped (~2.5 min) |
| PostgreSQL | `make test-pg` | 534 tests, **3–4 failed**, 21 skipped (~4 min of tests; ~15 min including `make reset-pg` and the jar) |
| Static analysis | `make lint` | **green** — pmd clean, checkstyle 37 warnings in 14 files / 0 errors (~30 s) |
| CLI integration | `make cli-test` | **cannot run** — its fixture host no longer exists, see ["`make cli-test` is unrunnable"](#make-cli-test-is-unrunnable) |

**Every remaining failure on both flavours is a network test.** Phase 5 cleared the last of the
code-and-schema ones — see ["What Phase 5 changed"](#what-phase-5-changed) — so the two flavours now
differ only by `PdbDataManagerTest.testParse`, which flaps: at the two Phase 6 H2 runs it passed
once and failed once, on the same code, minutes apart. The failures are
`GffManagerTest.testLoadGenesTranscript` (live Ensembl), `BlatSearchManagerTest.testFind` /
`testFindBlatReadSequence` (UCSC redirects HTTP to HTTPS, new since Phase 1) and that one. See
["Live-data failures"](#live-data-failures).

Which means: **a non-network failure is now always a regression.** There is no longer a documented
schema or DAO failure to hide behind on either flavour.

Phase 0 took this from 19 H2 / 65 PostgreSQL / red lint. What it fixed is at the bottom.
Phase 1 removed 9 tests along with the functionality they covered (GA4GH, HDFS, `person`),
which is the whole of the 537 → 528 change; the only *failure* it removed is
`VcfManagerTest.testLoadSmallScaleVcfFileGa4GH`. Phase 2 fixed the seven failures the toolchain
move introduced instead of re-baselining them — see ["What Phase 2 changed"](#what-phase-2-changed).
Phase 3 removed 9 more tests with the security stack and EhCache (528 → 519) and, again, fixed
rather than re-baselined everything the upgrade broke — see
["What Phase 3 changed"](#what-phase-3-changed), which also lists the test classes Phase 4 had to
bring back. Phase 4 brought back 7 of them (519 → 526) and changed no failure count on either
flavour — see ["What Phase 4 changed"](#what-phase-4-changed). Phase 5 added no tests and removed
none, and took PostgreSQL from 11 failures to 4 by fixing the causes — see
["What Phase 5 changed"](#what-phase-5-changed). Phase 6 **added** 14 tests (526 → 540) for the
Lucene version guard and changed no failure count — see
["What Phase 6 changed"](#what-phase-6-changed). Phase 7 removed 6 tests with the index cache
(540 → 534), fixed two Phase 6 assertions that had never held, and changed no failure count — see
["What Phase 7 changed"](#what-phase-7-changed).

**Two conditions the numbers depend on.** Get either wrong and you will see extra failures
that are not yours:

1. **`make test-pg` needs a clean database.** `ngb_test` lives in the persistent `pg-data`
   volume and several tests do not clean up after themselves, so a second run on a dirty
   volume shows extra failures — 6 in `HeatmapManagerTest` ("File with name
   'loadHeatmapTest' already exists"), `UrlShorterManagerTest`
   (`expected:<95d52dd9> but was:<alias>`), and others. Run `make reset-pg` first. Fixing that
   self-cleanup is worth doing but is not in the migration's path.
   `TargetManagerTest.filterTargetsByOwnerTest` was in this list and is not any more: Phase 5
   fixed the empty-`IN`-list bug that made it dirty-state-sensitive.
2. **`make test` needs a clean `contents/`.** `/contents/` in the repo root is gitignored
   scratch space (`files.base.directory.path`, plus every global Lucene index). Leftovers there
   used to make `HomologeneManagerTest.searchTest` pass and `TargetManagerTest.loadTargetsTest`
   fail; both are now self-contained, but if you get an inexplicable Lucene result,
   `rm -rf ../contents` and re-run.

   Since Phase 6 that command is actually sufficient, which it was not before: the two test
   profiles omitted `pathway.index.directory`, `homologene.index.directory` and
   `bam.coverage.index.directory`, and the test contexts resolve placeholders with
   `ignore-unresolvable="true"`, so those three indexes were written to directories in the source
   tree named literally `server/catgenome/${pathway.index.directory}` and so on — outside
   `contents/`, gitignored (`.gitignore:13`), and never cleaned. `ncbi.index.directory` was
   `./contents/ncbi/`, i.e. relative to the test JVM's working directory, which is
   `server/catgenome/`, so it missed too. All four now point at `@rootDirPath@/contents/`.

   **If you are coming from a tree that ran the suite before Phase 6, delete those directories
   once**, or you will see nine `LuceneIndexVersionException` failures that are not yours —
   they hold Lucene 6 indexes and Lucene 9 cannot read them:

   ```bash
   rm -rf ../contents ../server/catgenome/contents "../server/catgenome/\${"*
   ```

## H2: 1 documented failure (+2 live-data)

| Class | Cause | Survives the migration? |
|---|---|---|
| `GffManagerTest.testLoadGenesTranscript` | `expected:<protein_coding> but was:<protein_coding_CDS_not_defined>`. **Live external data, not a fixture problem** — see the note below. At one Phase 6 run it failed instead with `NullPointerException: Cannot invoke "java.util.List.isEmpty()" because the return value of …`, i.e. Ensembl answered with a field missing; same test, same cause, different weather. | Yes. Unaffected by every phase; only an Ensembl change or a decision about network tests will move it. |
| `BlatSearchManagerTest.testFind`, `testFindBlatReadSequence` | `ExternalDbUnavailableException: Unexpected HTTP status: 302 Found`. UCSC redirects `http://genome.cse.ucsc.edu/cgi-bin/hgBlat` to HTTPS and `HttpURLConnection` will not follow a cross-protocol redirect — see ["Live-data failures"](#live-data-failures). | Yes, until `blat.search.url` is changed to `https`. |
| `PdbDataManagerTest.testParse` | `expected:<B> but was:<A>`. Live RCSB PDB data — see ["Live-data failures"](#live-data-failures). Passed at the Phase 2 H2 recording and failed at the PostgreSQL one; passed on both at Phase 3's. Expect it either way. | Yes, until network tests are dealt with. |

`VcfManagerTest.testLoadSmallScaleVcfFileGa4GH` was here until Phase 1 deleted GA4GH; the test
went with the feature. `UrlValidatorService.isRemotePath` was deliberately left untouched.

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

### Live-data failures

`BlatSearchManagerTest.testFind` and `testFindBlatReadSequence` started failing during Phase 2 for a
reason that has nothing to do with the migration: `blat.search.url` is
`http://genome.cse.ucsc.edu/cgi-bin/hgBlat` in seven property files, and UCSC now answers `302
Found` pointing at `https://` (confirmed with `curl`; no JDK has ever let `HttpURLConnection` follow
a cross-protocol redirect). `BlatSearchManager` turns the 302 into
`ExternalDbUnavailableException: Unexpected HTTP status: 302 Found`. **This breaks BLAT search in
production too**, not just the test. Fixing it means changing the default URL, which is a behaviour
change and so belongs to a phase allowed to make one — recorded here rather than papered over.

`PdbDataManagerTest.testParse` parses live RCSB PDB data. It asserted `expected:<B> but was:<A>`
two baselines ago, passed at the Phase 0 recording, and is red again on both flavours at the
Phase 1 recording. **It is not Phase 1's doing**: it already failed on PostgreSQL when Phase 0's
exit criteria were re-verified at the start of the Phase 1 session, before a single Phase 1
change existed. The assertion is on a chain identifier returned by the RCSB service, so it will
keep flipping. Treat a failure there as external drift, not regression — and do not "fix" it by
loosening the assertion, for the same reason as `GffManagerTest.testLoadGenesTranscript`: the
real question is whether network-dependent assertions belong in the unit suite.

## PostgreSQL: 1 documented failure (+2 live-data, +1 that flaps)

The same one as on H2, and it is a network test. **Phase 5 fixed the eight that were real**, so
PostgreSQL and H2 now have identical documented failures. The eight are kept below rather than
deleted, because they are the specification for what the convergence migrations must keep true —
if one comes back, a script set has drifted again.

| Count | Class | Cause | Status |
|---|---|---|---|
| 1 | `GffManagerTest.testLoadGenesTranscript` | Same live-Ensembl failure as on H2. | **Still fails.** Yes, survives. |
| 1 | `PdbDataManagerTest.testParse` | Same live-RCSB failure as on H2. Failed on PostgreSQL and passed on H2 at the Phase 5 recording; at the Phase 6 recording it passed on PostgreSQL, which is why that suite came in at 3 rather than 4. | **Flaps.** |
| 2 | `BlatSearchManagerTest.testFind`, `testFindBlatReadSequence` | Same UCSC HTTP→HTTPS drift as on H2. | **Still fails.** Until `blat.search.url` is changed. |
| 4 | `BookmarkDaoTest.testSaveLoadBookmark`, `testAllItemTypes`, `VcfFileDaoTest.testSaveLoadVcfFile`, `testSaveLoadSamples` | `catgenome.vcf.multi_sample` was `NOT NULL` in the PostgreSQL script set and nullable in the H2 one; `VcfFileDao` inserts `null`. | **Fixed in Phase 5** by `v2026.08.21_12.00__align_vcf_multi_sample_with_h2.sql`. |
| 2 | `BlastTaskDaoTest.testDeleteOrganisms`, `testDeleteExclOrganisms` | `task_organism.organism` / `task_excl_organism.organism` were `character varying` on PostgreSQL and numeric on H2, while `BlastTaskDao.deleteOrganisms` emits `where organism = 1`. `ERROR: operator does not exist: character varying = integer`. | **Fixed in Phase 5** by `v2026.08.21_12.10__align_task_organism_with_h2.sql`. |
| 1 | `RoleDaoTest.testLoadRolesWithUsers` | `expected:<11> but was:<12>` — the two script sets seeded a different number of predefined roles. | **Fixed in Phase 5** by `v2026.08.21_12.20__align_predefined_roles_with_h2.sql`. |
| 1 | `TargetManagerTest.filterTargetsByOwnerTest` | `TargetManager.load` built `WHERE target_id IN ()` for an empty id set. H2 1.3 accepted it, PostgreSQL does not. **Was a real production bug**: any target filter matching nothing failed on PostgreSQL. | **Fixed in Phase 5**, as a separate commit — it was never the migration's doing. |

`VcfManagerTest.testLoadSmallScaleVcfFileGa4GH` was another entry here until Phase 1 deleted
GA4GH.

The PostgreSQL suite now exercises the ACL code, which it previously could not (see below). Treat a
new context-startup failure in `*SecurityServiceTest` as real — those 16 tests are the only thing
standing between a broken NGB security expression and a green build, as Phase 3 found out the hard
way (see ["What Phase 3 changed"](#what-phase-3-changed)). `JwtAuthenticationTest` and
`AuthManagerTest` were in this sentence until Phase 3 deleted them with the JWT stack; Phase 4
restored them and all 7 of their tests pass on both flavours.

## Static analysis: green

`make lint` runs `checkstyleMain pmdMain` on the server module and both pass. Phase 2 moved the
tools to **Checkstyle 11.1.0** and PMD 6.55.0 (not PMD 7 — Gradle 7.6 cannot run it; see the
Phase 2 findings) and rewrote both rulesets for the `category/java/*.xml` layout. Phase 3 finished
that carry-over: **PMD 7.26.0**, on Gradle 8.14.5.

| Module | Command | Checkstyle | PMD |
|---|---|---|---|
| `server/catgenome` | `make lint` | 37 warnings in 14 files, **0 errors** | clean |
| `server/catgenome`, tests too | `./gradlew -p server/catgenome checkstyleMain checkstyleTest pmdMain pmdTest` | +11 warnings in 3 test files, **0 errors** | clean |
| `server/ngb-cli` | `./gradlew -p server/ngb-cli checkstyleMain checkstyleTest pmdMain pmdTest` | 6 warnings main + 1 test, **0 errors** | clean |

Reports: `server/<module>/build/reports/{checkstyle,pmd}/main.html`.

**The warnings are not new and do not fail the build**, by long-standing choice: `checkstyle.xml`
sets `severity=warning` at `Checker` level, and Gradle's `Checkstyle` task fails only on errors
(`maxWarnings` defaults to `Integer.MAX_VALUE`). Checkstyle 7.2 reported one of them; 11.1.0 reports
37 because eight years of new checks and refined defaults landed in between — mostly `[Indentation]`
in generated-looking `externaldb/bindings/*` VOs, a few `[FinalClass]`, and the original
`CustomChatResponse.java` member name `finish_reason` (it mirrors a JSON field). Nothing was
suppressed to get here and no severity was lowered. Turning the warnings into errors is a
worthwhile clean-up but it is a code-style change, not a migration step.

PMD is clean with **zero** violations: the ruleset rewrite surfaced 59 real findings and all 59 were
fixed at the source. Two rules were consciously narrowed rather than carried across
(`SuspiciousConstantFieldName` dropped, `ClassNamingConventions` restored to its PMD 5 patterns) —
both reasoned about in `JAVA21-MIGRATION-PLAN.md` and in the rulesets' own header comments.

On PMD 7 the `BooleanInstantiation` deprecation warning is gone — the rule is now `UnnecessaryBoxing`,
as its PMD 6 message asked for. Two deprecation nags remain and are expected on every `ngb-cli` run:
`AvoidCatchingNPE` and `AvoidLosingExceptionInformation` are "scheduled for removal from PMD in
PMD 8.0.0". They still work; whoever moves to PMD 8 replaces them. Note also that PMD 7's
`AvoidDuplicateLiterals` counts a literal used four times over as a violation where 6.55 did not —
the one PMD failure Phase 3 caused, in `FileManager`, was exactly that.

Keep it green. A red static-analysis baseline would hide the real regressions in Phases 3–9.

## `make cli-test` is unrunnable

Not a failure to inherit — the target cannot start. `e2e/integration_tests.sh` downloads its test
data from `http://ngb.opensource.epam.com/distr/data/tests/`, and that host does not resolve:

```
Resolving ngb.opensource.epam.com ... failed: Name or service not known.
wget: unable to resolve host address 'ngb.opensource.epam.com'   (exit 4)
```

NXDOMAIN from inside the container and from the host; `opensource.epam.com` itself resolves, so it
is that one name that is gone. Nothing in the migration caused it and nothing in the migration can
fix it — `cli-tests.gradle` and `e2e/cli/testcases.csv` are sound, and Groovy 3 parses them.
**Phase 9 owns the fix** (host or generate the fixtures).

Until then, verify the CLI by hand against a running server — this was done on JDK 17 at the end of
Phase 2 and again at the end of Phase 3 (server on JDK 21, CLI still built and run on 17, on a
database wiped with `make reset-ngb-data` so it exercises Flyway from nothing as well). It covers the
same ground as `testcases.csv`:

```bash
make up                              # server on JDK 21 since Phase 3
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

## What Phase 1 changed

Nothing was fixed and nothing was re-baselined; the suite shrank because functionality was
deleted.

| Removed with its feature | Tests |
|---|---|
| GA4GH / Google Genomics | `VcfManagerTest.testLoadSmallScaleVcfFileGa4GH` (the failure), `testGetVariantsGA4GH`, `testAllTrackGa4GH` (`@Ignore`d), `ReferenceManagerTest.getReference` (`@Ignore`d), `ReferenceControllerTest.testSaveAndGetTrackDataGA4GH` (`@Ignore`d) |
| HDFS / Hadoop | `BamManagerTest.hdfsTest` (`@Ignore`d) |
| the legacy `person` package | `PersonDaoTest`, `PersonManagerTest` (whole classes) |

Also deleted: the 5 `externaldb/data/GA4GH_id10473*.json` fixtures, the
`templates/1000-genomes.chrMT.vcf` fixture used only by the GA4GH tests, and the
`ga4gh.google.*` properties from all four test property files.

Note that `build/test-results/test/` still holds stale XML for the deleted classes — Gradle
does not remove result files for tests that no longer exist. Trust the run summary's counts,
not a `grep` over that directory.

## What Phase 2 changed

The Gradle 7.6 / Boot 2.7.18 / JDK 17 waypoint broke a good number of tests on the way through. All
of them were fixed; nothing was re-baselined, and the only new entries in the lists above are the two
`BlatSearchManagerTest` network failures, which are not the migration's doing. The full reasoning is
in `JAVA21-MIGRATION-PLAN.md` ("Phase 2 execution findings") — the test-visible fixes:

| Fix | Effect |
|---|---|
| `MockitoAnnotations.initMocks` → `openMocks` at all 12 call sites, and `VcfManagerTest` unwraps its Spring proxies with `AopTestUtils.getTargetObject` before `openMocks` | Mockito 4 implements `initMocks` as `openMocks(...).close()`, which throws `NotAMockException` on a `@Spy` field holding a Spring bean |
| `org.mockito.internal.matchers.{Equals,Find}` in `ToolsControllerTest` → `org.hamcrest.Matchers.is` / `matchesPattern` | Those were always Hamcrest matchers passed to `jsonPath().value()`; from Mockito 2 they no longer implement `org.hamcrest.Matcher`, so the call silently bound to `value(Object)` |
| New `server/lombok.config` with `lombok.anyConstructor.addConstructorProperties = true` | Restores the `@ConstructorProperties` Lombok stopped emitting in 1.16.20, i.e. every implicit Jackson creator on the ~90 `@Builder` classes. 6 `NGBSessionSharingSecurityTest` failures, but the real exposure is production JSON parsing |
| Four `--add-opens` in the test JVM args (`java.util`, `java.util.concurrent`, `java.util.concurrent.atomic`, `java.io`) and `EhCacheTest.TestIndexCache` made `static` | EhCache 2.10.1 sizes `indexCache` by walking the object graph reflectively, which JEP 396 forbids. 11 failures |
| `CAST(? AS BIGINT)` around the three `object_id_identity` parameters in `conf/catgenome/acl-dao.xml` | Spring Security 5 binds the ACL identifier as a string against a `bigint` column; PostgreSQL rejects it and the library swallows the error, so it surfaced as `25P02` on the *next* statement. 4 PostgreSQL failures (`AclPermissionSecurityServiceTest` ×2, `BamSecurityServiceTest.saveBamTest`, `DataItemSecurityServiceTest.deleteFileByBioItemId`) |
| `LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)` in the `BlastTaskDaoTest` and `ActivityDaoTest` fixtures | JDK 9+ `now()` carries nanoseconds; PostgreSQL `timestamp` keeps microseconds. The assertions stay exact equality — the fixture just stops asking for precision no database here has. 3 PostgreSQL failures |

Two things worth knowing when reading a Phase 2 run: the ACL failures are only visible because
Phase 0 made the PostgreSQL suite exercise the ACL code at all, and both PostgreSQL fixes are
PostgreSQL-only — H2 1.3 coerces the string to `bigint` and stores nanoseconds, so it never
complained.

## What Phase 3 changed

Boot 3.5.16 / Spring 6.2 / Spring Security 6.5 / JDK 21, with security reduced to anonymous by design.
528 → 519 tests, and the H2 failure count is unchanged at 3. Nothing was re-baselined.

### Removed with the security stack — Phase 4 restores exactly this set

| Path | Tests | Why it went | Phase 4 |
|---|---|---|---|
| `src/test/java/com/epam/catgenome/app/JwtAuthenticationTest.java` | 4 | Tests `JWTSecurityConfiguration` and the `JwtTokenVerifier`/`JwtAuthenticationProvider` chain, deleted this phase | **restore** |
| `src/test/java/com/epam/catgenome/manager/AuthManagerTest.java` | 3 | Asserts on the JWT `AuthManager` issues (`issueTokenForCurrentUser`, claims, expiry) | **restore** |
| `src/test/java/com/epam/catgenome/common/AbstractSecurityTest.java` | 0 | Base class for the two above (`@WithMockUser`-style setup); no `@Test` of its own | **restore** |
| `src/test/java/com/epam/catgenome/common/security/WithMockUserContext.java` | 0 | Custom `@WithSecurityContext` annotation | **restored, and it was not dead code** — this row said "referenced by nothing", which is wrong: `AuthManagerTest` annotates all three of its tests with it |
| `src/test/java/com/epam/catgenome/common/security/WithMockUserContextSecurityContextFactory.java` | 0 | Its factory | same |

Recover them with `git show <this phase's commit>^:<path>` rather than from a copy kept in the tree:
they are the specification for what the SAML attribute mapping and the JWT claims must keep doing.

Two more test cases went, and these are **not** to be restored: `EhCacheTest.testMaxSizeInBytes` and
`testToString`, which asserted on EhCache 2's byte-size bounding and on the exact text of a
`toString()` built from it. D11 replaced EhCache with Caffeine, which cannot bound a cache by the
retained size of an object graph at all; the reasoning is in the class's own javadoc. The other three
`EhCacheTest` cases still run. 4 + 3 + 2 = the 9 tests that left the suite.

Nothing was `@Ignore`d or otherwise disabled in place. The 21 skipped tests are the same 21 as in
Phase 2.

### Test-visible fixes

| Fix | Effect |
|---|---|
| `NGBMethodSecurityExpressionHandler` now overrides `createEvaluationContext(Supplier<Authentication>, MethodInvocation)`, not only the protected `createSecurityExpressionRoot` | **16 failures.** Spring Security 6 calls the supplier-based overload from its interceptors and builds the expression root through a *private* method, so NGB's root — and every `isAllowed`/`hasPermission…` expression with it — was silently bypassed: `EL1004E: Method call: Method isAllowed(...) cannot be found on type MethodSecurityExpressionRoot` in `NGBSessionSharingSecurityTest` ×8, `ProjectSecurityServiceTest` ×4, `DataItemSecurityServiceTest` ×3, `AclPermissionSecurityServiceTest` ×1 |
| `aws-java-sdk-s3`/`-sts` 1.11.704 → 1.12.797 | Not a test failure — the application would not **start**. `EC2MetadataUtils.<clinit>` reads `PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE`, deleted in Jackson 2.12; NGB builds the S3 client on every startup |
| `ExternalDBControllerTest`: Boot's `@MockBean` → Spring's `@MockitoBean` | `@MockBean` is deprecated for removal in Boot 3.4 |
| `controller/util/UrlTestingUtils`: `AbstractHandler` → `HttpServlet` in a `ServletContextHandler` | Jetty 12 deleted `AbstractHandler` and moved the servlet API into `org.eclipse.jetty.ee10` |
| `CytobandControllerTest`: `MockMvcRequestBuilders.fileUpload` → `multipart` | `fileUpload` is gone in Spring 6 |
| 6 test-side `Assert.isTrue(boolean)` / `Assert.notNull(Object)` call sites given messages | Spring 6 deleted the no-message overloads |
| `EnsemblDataManagerTest`: `new Double("0.0750799")` → `Double.valueOf` | PMD 7's `UnnecessaryBoxing` (which replaced `BooleanInstantiation`) flags the boxing constructors |
| `src/test/resources/log4j.xml` deleted, JUnit 4 kept on the JUnit 5 platform via `junit-vintage-engine` | slf4j 2 has no `slf4j-log4j12`, so the suite logs through `spring-boot-starter-log4j2`; Boot 3.5's `spring-boot-starter-test` is JUnit 5 only (D13) |
| All four `--add-opens` gone from the test JVM args | They existed only for EhCache 2's reflective sizing, which D11 removed |

## What Phase 4 changed

SAML 2 and JWT rewritten on Spring Security 6.5.11 (`spring-security-saml2-service-provider` /
OpenSAML 4.3.2, `com.auth0:java-jwt` 4.6.0) — the findings are in `JAVA21-MIGRATION-PLAN.md`.
**519 → 526 tests**, which is exactly the seven Phase 3 removed and listed above; **3 (H2) and
11 (PostgreSQL) failures, unchanged**, and the same 21 skips. Nothing was re-baselined, nothing was
`@Ignore`d, and no test was weakened to accommodate the new stack.

The seven came back as they were, with two changes:

| Change | Why |
|---|---|
| `@MockBean SAMLEntryPoint` and `@MockBean SAMLAuthenticationProvider` dropped from `JwtAuthenticationTest` and `AuthManagerTest` | They existed only because the old `JWTSecurityConfiguration` autowired both to build its entry point, so every JWT test context dragged in the SAML stack. The Security 6 chain redirects to a *URL* instead, so there is nothing left to mock — and `@MockBean` is deprecated for removal in Boot 3.4 anyway |
| `src/test/resources/applicationContext-test.xml` component-scans `com.epam.catgenome.security.jwt` again | Phase 3 commented that scan out with the package. `AuthManager` takes `JwtTokenGenerator` by constructor, so every context that has an `AuthManager` needs it; `JwtTokenVerifier` is not a component but a bean of `JWTSecurityConfiguration` |

One thing to know before writing another security test: `AbstractSecurityTest` imports the JWT,
SAML and ACL configurations, but under `test-catgenome-auth.properties` only the JWT one is active —
the other two are excluded by their `@ConditionalOnProperty`. Switching SAML on from a subclass
needs a key store and identity provider metadata on disk, because `SAMLSecurityConfiguration` reads
both eagerly while building the relying party registration.

**No unit test covers the SAML half, deliberately.** There was none before this phase either — the
OpenSAML 2 configuration was never unit-tested here — and a test worth having needs an identity
provider, a key store and a signed assertion, i.e. the environment `make smoke-saml` already
provides. So the SAML acceptance tests are the `.devenv` targets, and they are the ones to run after
touching that code:

```bash
make up-saml && make smoke-saml                              # web SSO + single logout, both users
make smoke-saml U=ngbuser@ngb.dev.local P=user
make saml-verify-signing                                     # SP metadata + AuthnRequest signatures
make cli-token                                               # a JWT, via the SAML session
```

## What Phase 5 changed

Flyway 3.2.1 → 11.7.2, H2 1.3.176 → 2.3.232, PostgreSQL 9.6 → 16.15, postgresql driver
9.4-1206 → 42.7.x — the findings are in `JAVA21-MIGRATION-PLAN.md` ("Phase 5 execution findings").
**No test was added, removed, `@Ignore`d or weakened; the count stays at 526 / 21 skipped.** H2
holds at 3 failures and PostgreSQL goes **11 → 4**, entirely by fixing causes.

### The eight non-network failures, fixed at the source

Seven were the two Flyway script sets having diverged, and the eighth was a DAO bug. All were
fixed forward, on the PostgreSQL side, with **H2 as the reference** — `DefaultRoles`,
`docs/md/user-guide/um-overview.md` and the DAO code all agree with the H2 script set, so it is
the one the application is actually written against.

| Fix | Effect |
|---|---|
| `v2026.08.21_12.00__align_vcf_multi_sample_with_h2.sql` — `VCF.MULTI_SAMPLE` loses `NOT NULL` and its default | −4 (`BookmarkDaoTest` ×2, `VcfFileDaoTest` ×2) |
| `v2026.08.21_12.10__align_task_organism_with_h2.sql` — `TASK_ORGANISM.ORGANISM` and `TASK_EXCL_ORGANISM.ORGANISM` `VARCHAR(250)` → `BIGINT` | −2 (`BlastTaskDaoTest`) |
| `v2026.08.21_12.20__align_predefined_roles_with_h2.sql` — deletes `ROLE_CYTOBANDS_MANAGER` / `ROLE_MAF_MANAGER`, adds the **missing** `ROLE_WIG_MANAGER`, renumbers the rest to match H2's 1–10 and remaps `user_role` | −1 (`RoleDaoTest.testLoadRolesWithUsers`) |
| `TargetManager.load(TargetQueryParams)` returns early when the filter matched nothing | −1 (`TargetManagerTest.filterTargetsByOwnerTest`). Separate commit; a pre-existing bug, not the migration's |

`ROLE_WIG_MANAGER` being absent on PostgreSQL was **a real production defect**, not a test
artefact: nothing but an administrator could manage WIG files there. Still open and *not* fixed
here, because no test covers it and inventing role ids is not this phase's call:
`NGBMethodSecurityExpressionRoot.hasSpecificRole` references MAF, BUCKET, PROJECT and BOOKMARK
manager roles, and `DefaultRoles` a HEATMAP one, that **neither** flavour has ever seeded.

### Two H2 2.x behaviour changes, caught only by running the suite

The DDL work was proved out by migrating and diffing schemas, which is why it looked complete.
The first `make test` on H2 2.3.232 came back **526 / 12 / 21** against a baseline of 4. Both
causes are genuine data-correctness bugs on H2 2.x, not test artefacts:

| Fix | Effect |
|---|---|
| `metadata-dao.xml`: `(entity_id, entity_class) IN (@ENTITIES@)` → `IN (VALUES @ENTITIES@)` | −7 (`MetadataDaoTest.shouldGetSeveralItems`, `ProjectManagerTest` ×4, `ProjectControllerTest` ×2). H2 2.x tries to convert `'PROJECT'` to the *first* column's type and fails the query with `Data conversion error` `[22018-232]`; 1.3.176 evaluated it correctly. Reproduced standalone, so it is H2's optimiser. `VALUES` reads correctly on both engines |
| `v2026.08.21_12.30__heatmap_cell_value_double.sql`, on **both** flavours: `HEATMAP.MIN_CELL_VALUE` / `MAX_CELL_VALUE` bare `DECIMAL` → `DOUBLE PRECISION` | −1 (`HeatmapManagerTest.createHeatmapTest`, `expected:<0.001273579> but was:<0.0>`). H2 2.x reads bare `DECIMAL` as `NUMERIC(100000, 0)` — **scale zero** — and rounds every value. PostgreSQL's unconstrained `NUMERIC` kept the scale, which is why only H2 showed it |

A ninth script-set divergence was found by the schema diff rather than by a test and converged
anyway (`BAM_COVERAGE.COVERAGE`, `NUMERIC` → `DOUBLE PRECISION`, `v2026.08.21_12.40`). Nothing
was re-baselined to get here.

### Two things the suite cannot catch

Both were found by starting a real server, and both are recorded here so the next phase does not
read a green suite as "the application boots":

- **Boot's `FlywayAutoConfiguration` collides with NGB's `flyway` bean** once flyway-core is 11
  instead of 3.2.1 — a hard context failure. Excluded in `Application.java`. The unit suites
  build plain Spring contexts from the XML and never go through auto-configuration.
- **The shipped log configuration discards `WARN`**, so `FlywayMigrator`'s one-time
  schema-history conversion notices went nowhere on a real upgrade. Fixed with a `WARN`-threshold
  console appender bound only to that class, in the `jar`, `release` and `staging` profiles.

So: after this phase, `make test` and `make test-pg` being at baseline is necessary but not
sufficient. `make up`, `make up-pg` and the upgrade procedure in
`docs/md/installation/database-upgrade.md` are the rest of it.

## What Phase 6 changed

Lucene 6.6.0 → 9.12.3, plus the startup guard and reindex procedure decision D8 asks for — the
findings are in `JAVA21-MIGRATION-PLAN.md` ("Phase 6 execution findings"). **526 → 540 tests**, all
14 of them new, and the failure count is unchanged on both flavours. Nothing was re-baselined,
nothing was `@Ignore`d, and no assertion was weakened.

### The 14 new tests

| Class | Tests | Covers |
|---|---|---|
| `app/LuceneIndexVersionCheckTest` | 7 | The startup half: the refusal text and directory count, the per-leaf rebuild call for a multi-index root, the argument-less endpoints — and the four states that must **not** stop startup (unconfigured, absent, present but index-free, written by this release) |
| `util/LuceneIndexUtilsTest` | 7 | The lazy half: a stale feature index named with the exact `ngb`/REST call that rebuilds it, derived from the path for vcf/genes/bed; the fallback for a non-feature index; the writer refusing identically to the reader; the rebuild writer discarding only Lucene's own files; and that a search counts every match, not the first 1000 |

They assert on message *text*, deliberately: on this phase the message is the deliverable. Neither
class needs a Lucene index committed to the tree — `util/StaleLuceneIndex` writes a `segments_1`
holding nothing but a codec header declaring format version 6, which every path into the guard
rejects in the same place as a real 6.6.0 index. A committed fixture would have been invalidated by
this very phase.

### Two real defects the phase found, and one test-infrastructure trap

| Fix | Effect |
|---|---|
| `BamCoverageManager` indexed `chr` as both a `TextField` and a `SortedStringField` in the same document | Not a test failure — the coverage index could not be **written** at all on Lucene 9, which requires every document to agree on a field's `IndexOptions`, not only on its `DocValuesType`. Lucene 6 accepted it and stored a mess. Found by reindexing the captured fixture, not by the suite |
| `LuceneIndexUtils.openWriterForRebuild` opened a writer, caught the version failure, discarded the files and retried with the same `IndexWriterConfig` | Found by `LuceneIndexUtilsTest`: `IndexWriter`'s constructor claims the config before it reads the commit, so the retry could only ever fail with "do not share IndexWriterConfig instances across IndexWriters". It now probes the commit point first. Invisible to the documented procedure, which deletes the directory first — and exactly the path an operator takes when they do not |
| The two test profiles omitted three `*.index.directory` properties, so the suite wrote Lucene indexes into source-tree directories named literally `${pathway.index.directory}` and never cleaned them | Nine failures on the first post-upgrade run (`PathwayManagerTest` ×6, `BamCoverageManagerTest` ×2, `HomologeneManagerTest.searchTest`) — all of them stale **Lucene 6** data from the runs of 2026-08-20, none of them the code's doing. See precondition 2 above; the properties now point into `contents/` and the directories were archived to `.devenv/fixtures/pre-migration/lucene6/test-tree-lucene6-dirs.tgz` and deleted |

That last row is the one to read twice before blaming yourself for a red run on this branch: a
Lucene upgrade turns "stale scratch index" from harmless into a hard failure, and this suite
carried scratch indexes between runs for years.

### `make test` being green still does not mean the server boots

Third phase in a row, and this time in both directions. The suite cannot see that the guard's
messages reach a terminal — every appender in the shipped log4j2 profiles filters at `ERROR`, so
the check's `WARN`/`INFO` lines went nowhere until they were bound to the `WARN`-threshold console
appender Phase 5 added — and it cannot see the `chr` defect either, because no test writes a
coverage index. The checks that do:

```bash
make up && make smoke                        # and read the log: the check reports at startup
.devenv/scripts/verify-lucene.sh             # all 18 Lucene read paths against a running server
```

`verify-lucene.sh` is Phase 6's, and its output against 9.12.3 after the documented reindex is
byte-identical to the 6.6.0 recording taken before the upgrade — both are in
`.devenv/fixtures/pre-migration/lucene6/`, which also holds the last Lucene 6 indexes that will
ever exist here, for testing the upgrade path against.

## What Phase 7 changed

htsjdk 2.2.4 → **5.0.0**, the forked reader package deleted, and the index cache deleted with it
(decisions D9 and D10) — the findings are in `JAVA21-MIGRATION-PLAN.md` ("Phase 7 execution
findings"). 117 files touch htsjdk; the failure count is unchanged on both flavours.

| Suite | Phase 6 | Phase 7 |
|---|---|---|
| H2 | 540 / 3 failed / 21 skipped | **534 / 3 / 21** |
| PostgreSQL | 540 / 3 / 21 | **534 / 3 / 21** — `PdbDataManagerTest.testParse` passed at this recording; it flaps, so 4 is also a pass |
| `make lint` | 37 warnings in 15 files, pmd clean | **37 warnings in 14 files, pmd clean** |

### The six removed tests

All of them existed only to test the cache D10 removes, so they went with it:

| Class | Tests | What it asserted |
|---|---|---|
| `util/EhCacheTest` | 3 | the `server.index.cache.enabled` property, put/evict, clear |
| `util/IndexHeaderCacheTest` | 2 | that a Tribble and a Tabix index header came back from the cache |
| `util/EhCacheDisabledIndexCacheTest` | 1 | that the property being `false` produced no cache |

`src/test/resources/test-catgenome-cache-disable.properties` existed only for the third and went
too. No other test lost coverage: the cache was a memoisation layer, and every reader path it sat
in front of is still covered by the manager tests.

### Two Phase 6 assertions that had never held

The first `make test` of this phase showed **five** failures, not three.
`LuceneIndexVersionCheckTest.refusesToStartOnAnIndexAnEarlierReleaseWrote` and
`LuceneIndexUtilsTest.readingAStaleFeatureIndexNamesTheFileAndTheCallThatRebuildsIt` both expected
the refusal message to mention `installation/lucene-reindex/` — the mkdocs URL form — against a
message that says `docs/md/installation/lucene-reindex.md`. Both halves were committed together in
`c76ff2ee`, so the assertion never passed; Phase 6's numbers were taken before the message text was
settled and the suite was not re-run afterwards. Neither file is touched by this phase and the
assertion is a deterministic `String.contains`, so it is not a Phase 7 regression.

Fixed on the test side, which is not a re-baselining: the assertion's intent is "the message points
at the reindex procedure", the pointer that exists is the repo path (three javadocs cite the same
form, and `mkdocs.yml` has no `site_url` for the URL form to resolve against), and both assertions
now expect the **full** path — stricter than before, not weaker.

### `GffManagerTest.testLoadGenesTranscript` did not move, and its failure shape is evidence

The plan expected this phase to change it; it could not, and Phase 0 had already established why
(see ["Why `GffManagerTest.testLoadGenesTranscript` is not a fixture
bug"](#why-gffmanagertesttestloadgenestranscript-is-not-a-fixture-bug)). It failed here in its other
documented shape, the NPE at `GffManagerTest.java:455` — and that shape is positive evidence for the
new parser: the assertions before it passed, so stock htsjdk parsed all 75,207 records of
`Homo_sapiens.GRCh38.83.sorted.chr21-22.gtf` into `Gene`s exactly as the fork did. What failed is
`GeneTrackManager.loadGenesTranscript` line 197, where `getTranscriptFromDB` threw
`ExternalDbUnavailableException` and the `catch` left `gene.transcripts` null. Strictly downstream of
the parser, strictly the network.

### A green suite is even weaker evidence here than usual

Fourth phase in a row. The suite reads the same fixtures through the managers with a Spring test
context; it never boots the server, so it cannot tell you an instance still parses a BAM — and it
verified nothing at all about the one regression this phase actually contained (htsjdk's length
probe becoming a `HEAD` request, which turns a pre-signed S3 URL into a silently empty file). That
was found by loading tracks through a running server, and the script that does it is committed:

```bash
.devenv/scripts/prepare-track-fixtures.sh    # stage the fixtures into /ngs/tracks
make up && make smoke
.devenv/scripts/verify-tracks.sh             # every track type, with the data it returned
```

It covers BED, GFF/GTF, GenePred, VCF, BedGraph, BigWig, SEG, BAM, CRAM, plain and bgzip+tabix, and
three remote variants including one whose `HEAD` is answered with 403; it compares CRAM against the
BAM it was made from and each remote read against the same file on disk, because those are the
comparisons a parser regression cannot survive. Two coverage notes: the CRAM fixture had to be
**created** (`.devenv/scripts/BamToCram.java` → `p7_agnX1.cram`, 45,237 records; there is none in the
repo), and **MAF cannot be verified through the server at all** — `MafController` was deleted in
`562b6a6d` (Dec 2018), so `MafManagerTest.testRegisterMaf` is the whole of its coverage.

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
