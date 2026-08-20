# NGB Java 21 migration plan

Companion to [`README.md`](README.md) (the containerised dev environment) and
[`TEST-BASELINE.md`](TEST-BASELINE.md) (what already fails before you touch anything).
Written 2026-08-20 against commit `82d67867` on branch `java_21`.

**This document is the specification.**
[`JAVA21-MIGRATION-EXECUTION.md`](JAVA21-MIGRATION-EXECUTION.md) is the operating procedure —
the per-phase session prompts, the per-phase caveats, and the progress table. Start there if
you are about to run a phase; come back here for what the phase actually is.

This document is the execution script for the migration. It is written to be picked up by
someone — or some session — with no prior context beyond this repository.

---

## 0. How to use this document

- **Phases are ordered by dependency, not by preference.** Each phase ends at a state that
  the dev environment can verify. Do not start phase N+1 until phase N's exit criteria are
  met, unless the phase text says the work is independent.
- **One commit per phase** on the `java_21` branch, message prefixed `[migration N]`.
  Sub-commits within a phase are fine; the phase boundary is what must be green.
- **Every version number below is a target, not a fact.** Library versions move. At the
  start of each phase, resolve the actual latest patch release of each target and check its
  own release notes. Where I am not certain a specific version pairing works, the text says
  **VERIFY**.
- **Never interpret a red test as new breakage without checking `TEST-BASELINE.md`.**
  Phase 0 rewrites that baseline; from then on, the baseline recorded at the end of Phase 0
  is the reference.

### Decisions already taken

These were settled with the project owner before this plan was written. They are not open
for re-litigation during execution; if one turns out to be unworkable, stop and report
rather than silently choosing differently.

| # | Decision |
|---|---|
| D1 | Target **Spring Boot 3.5.x** (Spring Framework 6.2, Spring Security 6.4), not Boot 4.x. |
| D2 | **Drop the WAR distribution.** Fat JAR only. |
| D3 | **Keep both database flavours** (H2 and PostgreSQL), upgrade H2 to 2.x and Flyway to 10/11, keep the build-time flavour switch. |
| D4 | **Keep JWT** as the CLI/API credential. Auth modes after migration: anonymous, and SAML (+ JWT for machines). Drop OAuth2. |
| D5 | **Drop:** HDFS/Hadoop, Electron desktop app, GA4GH/Google Genomics, the legacy `person` package, the Google PaLM 2 LLM provider, `spring-security-oauth2`, the Singularity recipe, and the SonarQube plugin. |
| D6 | **Keep** all other external integrations: Cloud Pipeline, BLAST server, UCSC BLAT, NCBI/Uniprot/Ensembl/OpenTargets/RCSB, BioPAX/SBGN pathways, AWS S3, Azure Blob, Azure OpenAI, custom LLM endpoint. |
| D7 | **Modernise both** the JRE-bundled Windows/Linux distributions and CI (AppVeyor → GitHub Actions on JDK 21). |
| D8 | **Lucene:** go to a current version and require a **documented full reindex**. No stepwise IndexUpgrader tool. Add a startup guard that fails with a clear message on an unreadable index. |
| D9 | **htsjdk:** upgrade to the latest Java-21-compatible release and **delete all customisations** — the four forked reader classes go, the code uses stock htsjdk. Separate, late phase. |
| D10 | **Index cache is dropped**, not reimplemented. `server.index.cache.enabled` and the `indexCache` region disappear with the fork. Note the remote-file performance risk in the docs; revisit only with a measurement. |
| D11 | **Cache provider → Caffeine** (EhCache 2 must go: Spring 6 removed `org.springframework.cache.ehcache`). |
| D12 | **Client is frozen.** Node stays at 14.17.5 in the toolbox, AngularJS untouched, npm tree untouched. Only the minimum needed for a modern Gradle to invoke the existing npm build. |
| D13 | **Test baseline is fixed first** (Phase 0). Tests stay on JUnit 4 via the vintage engine — no JUnit 5 conversion of the 139 test classes. |
| D14 | Plan lives in the repo; execution is one branch (`java_21`), one commit per phase. |

---

## 1. Where we are starting from

Measured, not assumed. All paths relative to the repository root.

### Scale

| | |
|---|---|
| `server/catgenome` | 1,084 Java files / ~159k lines (945 main, 139 test) |
| `server/ngb-cli` | 166 Java files / ~17k lines |
| `client` | ~1,900 files, ~121k lines of JS (AngularJS 1.5) |
| `export-templates/target-identification` | a **second, separate frontend** — Preact + TypeScript + Vite 4 + Tailwind 3, ~40 source files. Builds a single-file HTML template into `server/catgenome/src/main/resources/export/target`, used by the target-identification report export. |
| Spring XML bean definitions | 47 files (`applicationContext*.xml` + 42 `dao/*-dao.xml`) |
| Flyway migrations | 58 per flavour (h2, postgres) — **already divergent**, see README |
| Lombok-annotated files | 472 |
| Files touching htsjdk | 117 |
| Files touching Lucene | 104 |

### The three walls (as measured by `make probe-java21`)

1. **Gradle 3.3 only runs on JDK 8.** `Could not determine java version from '21.0.11'`.
2. **Lombok 1.16.16 dies under javac 21** — `ExceptionInInitializerError` from the
   annotation processor hitting internal javac APIs.
3. **The JDK-8-built jar does not boot on JDK 21** even without recompiling: Spring 4.3's
   cglib fails during context refresh with
   `InaccessibleObjectException: ... module java.base does not "opens java.lang"`.

Walls 1 and 2 are independent — fixing Gradle alone just exposes lombok.

### Current stack

| Component | Current | Notes |
|---|---|---|
| Java | 8 (implicit — no `sourceCompatibility` set anywhere) | |
| Gradle | 3.3 (wrapper, Jan 2017) | |
| Spring Boot | 1.5.2.RELEASE | |
| Spring Framework | 4.3.7.RELEASE | |
| Spring Security | 4.2.2.RELEASE | |
| SAML | `spring-security-saml2-core` 1.0.2 (OpenSAML 2, EOL) | 589-line config class |
| Servlet | `javax.servlet` 3.1.0, Tomcat 8.5-era embedded | |
| Lombok | 1.16.16 | |
| Flyway | 3.2.1 | |
| H2 | 1.3.176 (2014) | |
| PostgreSQL | pinned to 9.6 because Flyway 3.2.1 rejects newer | |
| Connection pool | c3p0 0.9.5.1 | XML-defined `ComboPooledDataSource` |
| Cache | EhCache 2.10.1 via `org.springframework.cache.ehcache` | |
| Lucene | 6.6.0 | |
| htsjdk | 2.2.4 (2016) | plus 4 forked reader classes |
| API docs | `com.mangofactory:swagger-springmvc:1.0.2` + `com.wordnik` annotations | Swagger 1.x |
| AWS SDK | v1 1.11.704 (EOL) | 6 files |
| POI | 3.16 | |
| Excel/CSV/JSON | opencsv 5.8, Jackson 2.12.5, gson 2.10.1 | |
| Test | JUnit 4.12, Mockito 1.10.19, unitils 3.4.2 (3 files, `ReflectionAssert` only) | |
| Static analysis | Checkstyle 7.2, PMD 5.5.1 with `rulesets/java/*.xml` paths | |
| Node | 14.17.5, webpack 5, `com.moowork.node` 1.1.1 | |
| Docs | mkdocs, `readthedocs` theme | |
| CI | AppVeyor, `Previous Ubuntu1604`, jdk 8 / python 3.9 / node 14 | |

### Good news found during research

- **No `sun.*` or `com.sun.*` imports.** Nothing depends on removed JDK internals.
- **No `SecurityManager` / `AccessController` usage.** Nothing hits the deprecated-for-removal
  security manager.
- **No `finalize()` overrides.**
- **No identifiers colliding with Java 9+ restricted keywords** (`var`, `record`, `yield`,
  `sealed`, `permits`, bare `_`).
- **No JPA/Hibernate.** Persistence is Spring JDBC (`JdbcTemplate`) with SQL in XML bean
  properties. The 42 `dao/*-dao.xml` files are plain `<bean>` definitions against the
  unversioned `spring-beans.xsd` and should survive the Spring 6 move essentially untouched.
- **The S3/Azure htsjdk extension points are public SPIs**, not forks:
  `EnhancedUrlHelper implements htsjdk.tribble.util.URLHelper` (registered via
  `ParsingUtils.registerHelperClass`) and
  `NgbSeekableStreamFactory implements ISeekableStreamFactory` (installed via
  `SeekableStreamFactory.setInstance`). These survive D9. Only the *readers* are forked.
- **IGV is barely used** — 3 classes (`BasicFeature`, `Exon`, `UCSCGeneTableCodec`) despite
  being a heavy jitpack dependency.
- **`commons-fileupload` is effectively unused** in main code: `CommonsMultipartResolver`
  appears only in the WAR's `WEB-INF/applicationContext.xml` (dropped by D2) and one test
  XML. Spring Boot's default `StandardServletMultipartResolver` covers the one
  `MultipartFile` usage in `AbstractRESTController`.

### Latent bugs found (fix them where the phase text says so)

- `database.jdbc.url=jdbc:h2:mem:test_catgenome;DB_CLOSE_ON_EXIT=FALSE'` — **stray trailing
  apostrophe**, in three test property files (`profiles/h2/test-catgenome.properties`,
  `src/test/resources/test-catgenome-acl.properties`,
  `src/test/resources/test-catgenome-auth.properties`). H2 1.3 tolerates it. H2 2.x is very
  unlikely to. Fix in Phase 5 at the latest; harmless to fix in Phase 0.
- The two Flyway script sets seed **different predefined role IDs** (h2:
  `ROLE_WIG_MANAGER`=8, `ROLE_SEG_MANAGER`=9; postgres: `ROLE_MAF_MANAGER`=9,
  `ROLE_SEG_MANAGER`=10). Already documented in the README. Do not assume the two schemas
  are identical at any point.
- `spring-boot-devtools` is a **`compile`-scope dependency**, so it ships in the production
  jar. Move it to `developmentOnly` or drop it (Phase 2).
- `CustomAwareAuthenticationSuccessHandler` does
  `StringUtils.replace(savedRequest.getRedirectUrl(), "8443", "8080")` — a hardcoded port
  rewrite in the post-SAML-login redirect. Delete it in Phase 4.

Found while running Phase 0 (all confirmed against a clean `make test-pg`; **none of them
are fixed** — they are outside Phase 0's task list and are recorded here so a later phase
does not mistake them for its own breakage):

- `TargetGeneDao.loadTargetGenes` interpolates an id set straight into `WHERE target_id IN
  (...)` with no empty-set guard, so an empty set produces `IN ()`. H2 1.3 accepts it;
  PostgreSQL rejects it. `TargetManager.load(TargetQueryParams)` calls it unconditionally,
  so **any filter that matches no target 500s on PostgreSQL**
  (`TargetManagerTest.filterTargetsByOwnerTest`). Real production bug, not a test bug.
- `catgenome.task_organism.organism` / `task_excl_organism.organism` are `character varying`
  in the PostgreSQL script set and numeric in the H2 one, while
  `BlastTaskDao.deleteOrganisms` emits `where organism = 1`. Another schema divergence to
  add to the role-id one above; it will have to be settled in Phase 5.
- `catgenome.vcf.multi_sample` is `NOT NULL` in the PostgreSQL script set and nullable in the
  H2 one, and `VcfFileDao` inserts `null` for it (4 failures in `BookmarkDaoTest` /
  `VcfFileDaoTest`). Same class of divergence.
- `UrlValidatorService.isRemotePath` does not list `GA4GH`, so a GA4GH variant-set id is run
  through `validateLocalPath` and rejected against `ngs.data.root.path` — GA4GH registration
  cannot succeed in *any* configuration (with `ngs.data.root.path=/`, as in
  `profiles/jar`, it fails on "Server file system browsing is not allowed" instead). Moot
  after Phase 1 removes GA4GH; noted because it is the cause of
  `VcfManagerTest.testLoadSmallScaleVcfFileGa4GH`, not something environmental.

---

## 2. Target stack

| Component | Target | Confidence |
|---|---|---|
| Java | **21** (LTS), via Gradle toolchains | certain |
| Gradle | **8.14.x** (Phase 3+); **7.6.x** as the Phase 2 waypoint | VERIFY exact patch |
| Spring Boot | **3.5.x** (Phase 3+); **2.7.18** as the Phase 2 waypoint | see §3 Phase 2 for why |
| Spring Framework | 6.2.x (managed by Boot) | certain |
| Spring Security | 6.4.x (managed by Boot) | certain |
| SAML | `spring-security-saml2-service-provider` (OpenSAML 4.x) | VERIFY OpenSAML artifact availability |
| Servlet | `jakarta.servlet` 6.x, Tomcat 10.1.x embedded (managed by Boot) | certain |
| Lombok | **≥ 1.18.30** (first release with JDK 21 support); take latest | certain |
| Flyway | 10.x or 11.x (Boot 3.5 manages a version — prefer the managed one) | VERIFY H2 2.x support |
| H2 | 2.x | certain that 1.3.176 must go — Flyway 10 will not run on it |
| PostgreSQL | 16 (`PG_VERSION=16` in `.devenv/.env`) | certain |
| Connection pool | HikariCP (Boot default) replacing c3p0 | certain |
| Cache | Caffeine (D11) | certain |
| Lucene | 9.12.x — **decide 9.x vs 10.x at the start of Phase 6** | see Phase 6 |
| htsjdk | latest 4.x (requires Java 17+) | VERIFY latest at Phase 7 |
| API docs | `springdoc-openapi-starter-webmvc-ui` 2.x | certain |
| AWS SDK | v2 (`software.amazon.awssdk:s3`, `:sts`) | certain |
| POI | 5.x | certain |
| Test | JUnit 4.13.2 on the JUnit 5 platform via `junit-vintage-engine`; Mockito 5.x | certain |
| Static analysis | Checkstyle 10.x/11.x, PMD 7.x (**ruleset needs a full rewrite**) | certain |
| Node | **14.17.5, unchanged** (D12) | certain |
| CI | GitHub Actions, JDK 21 | certain |

---

## 3. Phases

### Phase 0 — Baseline stabilisation and environment prep

**Goal:** a trustworthy red/green signal, and a toolbox that can run every JDK the
migration passes through. **No library or language version changes to the application.**

**Why first:** the migration's only real safety net is the test suite. Right now that net
reports 19 H2 / 65 PostgreSQL failures, and on PostgreSQL it says *nothing at all* about
ACL or auth code (≈45 of the 65 failures are the same context-startup failure). Starting a
Spring 6 migration with that signal means you cannot tell your own breakage from the
existing kind.

**Tasks**

1. **Delete `server/catgenome/src/test/resources/templates/Felis_catus.vcf.idx`.**
   It is a stale committed Tribble index: it records a source size of 5,898 bytes while
   `Felis_catus.vcf` is 5,995, so htsjdk seeks to a stale offset and reads a partial line.
   It even embeds the original author's absolute path. htsjdk rebuilds the index when it is
   absent. → fixes 5 `VcfManagerTest` failures on both flavours.
2. **Set `blastTaskId` in `BlastTaskDaoTest`.** Commit `f72202ba` added the column as
   `NOT NULL` and made the DAO insert it, but the test never populates it. → fixes 9
   failures on both flavours.
3. **Give the security/ACL/JWT tests a flavour-matched datasource.**
   `AbstractSecurityTest`, `AbstractACLSecurityTest` and `JwtAuthenticationTest` load
   `src/test/resources/test-catgenome-acl.properties` / `test-catgenome-auth.properties`,
   which hardcode `jdbc:h2:mem:test_catgenome`. With `-Pdatabase=postgres` the Flyway script
   set is swapped to the PostgreSQL one, so PostgreSQL SQL is applied to an H2 database and
   the context fails (`Function "SETVAL" not found`). Move those two files under
   `profiles/<flavour>/` (which `profiles.gradle` already copies into
   `build/resources/test`) or otherwise parameterise the JDBC URL by `-Pdatabase`. → fixes
   ≈45 PostgreSQL failures and makes the PostgreSQL suite meaningful for auth code.
4. **Fix the 4 pre-existing PMD violations** rather than suppressing them —
   `EnsemblDataManager.java:108` (`AvoidCatchingNPE` ×2, `AvoidCatchingGenericException`)
   and `AlignmentManager.java:169` (`AvoidCatchingGenericException`). Both are
   `catch (Exception | NullPointerException)` around external-service calls. Doing this now
   means `make lint` is green *before* the PMD version changes in Phase 2, so a ruleset
   rewrite cannot hide a real regression.
5. **Fix the stray apostrophe** in the three `jdbc:h2:mem:...DB_CLOSE_ON_EXIT=FALSE'` URLs.
6. **Add JDK 17 to the toolbox image** (`.devenv/toolbox/Dockerfile`) alongside 8 and 21:
   `with-java17` / `use-java 17`, mirroring the existing helpers. Phase 2 runs on 17.
   Also allow `NGB_JAVA_VERSION=17` in `.devenv/docker-compose.yml` and the app entrypoint.
7. **Add a `make probe` style target for the new baseline** if useful, and re-run
   `make test`, `make test-pg`, `make lint`.
8. **Rewrite `TEST-BASELINE.md`** with the new numbers. Expect roughly 4 H2 / 11 PostgreSQL
   remaining failures. For each remaining failure, state whether it is expected to survive
   the migration:
   - `VcfManagerTest.testLoadSmallScaleVcfFileGa4GH` — **will disappear in Phase 1** (GA4GH
     is being removed).
   - `GffManagerTest.testLoadGenesTranscript` (`protein_coding` vs
     `protein_coding_CDS_not_defined`) — fixture vs parser expectation; likely to move again
     in Phase 7 when htsjdk changes. Decide now whether to fix the fixture or the assertion.
   - `HomologeneManagerTest.searchTest` — needs a prebuilt Lucene taxonomy index the repo
     does not ship. Either ship a fixture index or mark the test as requiring one. **Note:
     any committed Lucene fixture index will become unreadable in Phase 6** (D8) — prefer
     building it in test setup.
   - `PdbDataManagerTest.testParse` — parses live RCSB data; inherently flaky. Consider
     tagging it so it can be excluded from the migration signal.
   - `TargetManagerTest.loadTargetsTest` — leftover state in a shared targets Lucene index;
     fix the test isolation.

**Exit criteria**

- `make test`, `make test-pg`, `make lint` all run, and every failure is enumerated in the
  rewritten `TEST-BASELINE.md` with a reason.
- `make lint` is **green**.
- `with-java17 java -version` works in the builder.
- `make up` + `make smoke` still serve the app on JDK 8; `make up-saml` + `make smoke-saml`
  still complete an SSO login.

**Risk:** low. Nothing about the application changes.

#### Phase 0 outcome (executed 2026-08-20)

**Done.** Result: **2 H2 / 10 PostgreSQL** failures (the plan predicted ~4 / ~11), `make lint`
green, all three JDKs selectable in the toolbox and in the app entrypoint, `make up` +
`make smoke` and `make up-saml` + `make smoke-saml` both verified on JDK 8. Every remaining
failure is enumerated with a cause in `TEST-BASELINE.md`; the newly-found production bugs are
in "Latent bugs found" above.

Four things did not match the plan's assumptions. Read these before trusting the
corresponding Phase 1/5/7 text:

1. **Task 1 — "htsjdk rebuilds the index when it is absent" is only half true.** It holds for
   the managed `FileManager` path, which is why 5 `VcfManagerTest` cases went green. It does
   *not* hold for `new TribbleIndexedFeatureReader(path, codec, requireIndex = true, cache)`,
   so deleting `Felis_catus.vcf.idx` broke
   `TestAbstractFeatureReader.testTribbleConstructors` — a failure the plan did not predict.
   Fixed by building the index in `@Before` over a copy in a private temp directory, rather
   than in `src/test/resources/templates`, where a generated index would be auto-discovered
   by other tests. `Felis_catus.idx` (no `.vcf`) was **kept**: `IndexHeaderCacheTest`,
   `VcfControllerTest` and `VcfManagerTest` reference it by name as a URL-served fixture.
2. **Task 3 needed no file moves.** `applicationContext-test.xml` already loads
   `test-catgenome.properties` as the `catgenome` properties bean, and `profiles.gradle`
   already copies that file from `profiles/<flavour>/`, so it is flavour-matched. The acl/auth
   property files were merely *shadowing* it: `@TestPropertySource` contributes to the
   Environment, which `PropertySourcesPlaceholderConfigurer` consults **before** its local
   `properties-ref` map. Deleting their `database.*` block was sufficient, and avoids
   duplicating the large JWT keypair into two flavour directories. Phase 4's note 6 still
   applies.
3. **`GffManagerTest.testLoadGenesTranscript` is not a fixture-vs-parser problem and Phase 7
   will not move it.** `setBioType` is fed from exactly one place, `ExtenalDBUtils.java:148`,
   i.e. from the **Ensembl REST response**; the GTF fixture contains zero
   `protein_coding_CDS_not_defined` entries against 75,207 `protein_coding`. It is live
   external data drifting (Ensembl release 110 added that biotype), in the same category as
   `PdbDataManagerTest`. Left red deliberately — the new value means "no CDS defined", so it
   is a semantic change, not a rename, and loosening the assertion would hide it. **Open
   question for the owner: whether network-dependent assertions belong in the unit suite at
   all.** Until that is answered, these two tests are permanent documented reds.
4. **`TargetManagerTest.loadTargetsTest` was not leftover Lucene state.** Commit `cdb4b2bb`
   moved paging out of `TargetManager.load` into `TargetController.loadTargets` on purpose, so
   that a page is cut from the ACL-filtered list; the test kept asserting the old contract.
   Fixed by asserting the current one. The real leftover-state coupling was elsewhere:
   `HomologeneManagerTest.searchTest` only passed when `TaxonomyManagerTest` had already
   built the shared taxonomy index, and on PostgreSQL 9 further failures appear on a dirty
   `pg-data` volume. Both are now documented in `TEST-BASELINE.md`.

Two conditions the recorded numbers depend on, and which any later phase must reproduce:
`make test-pg` requires `make reset-pg` first, and `make test` requires an empty `/contents/`.

---

### Phase 1 — Subtractive: remove dropped functionality

**Goal:** delete everything from D5 while still on Gradle 3.3 / JDK 8, so the removals are
verified by the *existing, known-good* toolchain.

**Why here:** every file removed now is a file that does not have to be migrated three
times over. Hadoop 2.2.0 in particular is a large transitive tree (and a known source of
reflection breakage on modern JDKs), and GA4GH is 28 files of reader code. Doing this before
the toolchain moves also means any breakage is unambiguously yours.

**1a. HDFS / Hadoop**

- Remove `compile group: "org.apache.hadoop", name: "hadoop-client", version: "2.2.0"` from
  `server/catgenome/build.gradle`.
- Delete `server/catgenome/src/main/java/com/epam/catgenome/util/HdfsSeekableInputStream.java`.
- `manager/bam/BamHelper.java`: remove the four `org.apache.hadoop.*` imports, the
  `HdfsSeekableInputStream` import, the `case HDFS:` branches at ~499 and ~521, and the
  `getHDFSIndex` / `getHDFSSamInputResource` methods.
- `entity/BiologicalDataItemResourceType.java`: remove the `HDFS(5)` constant and its
  `idMap.put`. **Do not renumber `GA4GH(6)`, `DOWNLOAD(7)`, `AZ(8)`** — those ids are
  persisted in the database.
- `server/ngb-cli/.../entity/BiologicalDataItemResourceType.java`: same removal.
- Remove `hdfs.file.path` / `hdfs.index.path` from
  `profiles/{h2,postgres}/test-catgenome.properties` and
  `src/test/resources/test-catgenome-{acl,auth}.properties`.
- `manager/bam/BamManagerTest.java`: remove the HDFS test path.

**1b. GA4GH / Google Genomics**

28 files reference it. Remove the reader family and the resource type:

- `manager/vcf/reader/VcfGa4ghReader.java`, and the GA4GH branches in
  `AbstractVcfReader`, `VcfReader`, `VcfManager`.
- The GA4GH entity/DTO set: `CallSet`, `CallSetSearch`, `GenotypeGA4GH`,
  `ReferenceBasesGA4GH`, `ReferenceBound`, `ReferenceGA4GH`, `ReferenceSet`, `VariantCall`,
  `VariantGA4GH`, `VariantSet`, `VariantSetMetadata`, `VariantsGA4GH`, `VariantsSearch`
  (locate them by `grep -rli ga4gh`).
- `exception/Ga4ghResourceUnavailableException.java`.
- GA4GH branches in `ReferenceManager`, `ReferenceGenomeManager`, `FileManager`,
  `NibDataReader`, `TrackType`.
- `constant/Constants.java`: `URL_GOOGLE_GENOMIC_API`, `GA4GH_MAX_BASE_SIZE`,
  `GA4GH_MAX_SIZE`, the Google API key constant and the GA4GH POST field constants.
- `entity/BiologicalDataItemResourceType.java` + the CLI copy: remove `GA4GH(6)`; again,
  **keep the numeric gap**.
- Tests: `ReferenceControllerTest`, `ReferenceManagerTest`, `VcfManagerTest` — remove the
  GA4GH cases.

**1c. Legacy `person` package**

Fully superseded by `manager/user` (`NGBUser` / `Role`), and it stores a plaintext
`PASSWORD` column.

- Delete `entity/person/`, `dao/person/`, `manager/person/`, `controller/person/`,
  `security/PersonService.java`.
- Delete `src/main/resources/conf/catgenome/dao/person-dao.xml` and its `<import>` line in
  `applicationContext-database.xml`.
- Delete `src/test/java/.../dao/PersonDaoTest.java` and
  `src/test/java/.../manager/PersonManagerTest.java`; remove them from
  `GenomeBrowserTestSuite` if listed.
- **Database:** `controller/person/UserController` and `RoleController` expose `/user`,
  `/users`, `/role`, `/role/loadAll` etc. Before deleting, `grep` the client
  (`client/client/dataServices/user`, `/role`) to confirm the UI talks to
  `manager/user`'s controller and not these. If any UI path resolves here, port it first.
- Add a Flyway migration (both flavours) dropping `CATGENOME.PERSON`. **Check for foreign
  keys into it first** — `grep -rn 'person' src/main/resources/database/`. If anything
  references it, leave the table and only remove the Java code; note the leftover in the
  release notes.

**1d. Electron desktop app**

- Delete the `desktop/` directory (includes `desktop/lib/H2/catgenome.h2.db`, a committed H2
  database, and `electron ^1.4.15`).
- `settings.gradle`: drop `'desktop'` from the include list.
- Root `build.gradle`: delete `buildDesktopUI`, `buildDesktopJar`, `buildDesktop`; remove
  `buildDesktop` from `buildAll`.
- `client/build.gradle`: remove the `desktop` property and the `build:desktop` branch of
  `buildUI`.
- `client/package.json`: remove the `build:desktop` and `serve:desktop` scripts.
- `client/webpack.config.js`: remove `env.desktop`, the `(desktop)` log suffix, and the
  `'process.env.__DESKTOP__'` define.
- `client/client/app/shared/components/ngbMainToolbar/index.js`: collapse the
  `if (process.env.__DESKTOP__)` branch to the non-desktop dependency list (delete lines
  20–25's conditional, keep the `else` array).
- Delete `server/catgenome/profiles/desktop/`.
- Docs: delete `docs/md/installation/desktop.md` and its `mkdocs.yml` nav entry.

**1e. WAR distribution (D2)**

- `server/catgenome/build.gradle`: delete `loadConfiguration()`'s war branch, the
  `bootRepackage { enabled = false }` / `apply plugin: "war"` path, `warExploded`, and the
  `providedRuntime` vs `compile` split for `tomcat-embed-jasper` /
  `spring-boot-starter-tomcat` (keep the `compile` form).
- Delete `server/catgenome/src/main/webapp/WEB-INF/` (`web.xml`,
  `catgenome-servlet.xml`, `applicationContext.xml`, `oauth-servlet.xml`). Note
  `src/main/webapp/swagger-ui/` is separate — see 1h.
- Root `build.gradle`: delete the `buildWar` task; remove it from `buildAll`.
- `-Pprofile` collapses to just `jar` (plus the `dev`/`staging`/`release` *config* profiles,
  which stay — they select `profiles/<name>/catgenome.properties`). Simplify
  `loadConfiguration()` accordingly.
- Docs: delete `docs/md/installation/binaries.md` ("Using Tomcat WAR") and its nav entry;
  update `docs/md/installation/overview.md`.
- Remove `commons-fileupload` (only the deleted WAR XML used `CommonsMultipartResolver`);
  also drop that bean from `src/test/resources/applicationContext-test.xml`.

**1f. OAuth2**

- Remove `spring-security-oauth2` from `build.gradle`.
- `app/Application.java`: remove the `OAuth2AutoConfiguration` import and its entry in the
  `@SpringBootApplication(exclude = {...})` list.
- `controller/ExceptionHandlerAdvice.java` and `manager/person/PersonManager.java`
  (the latter is deleted by 1c): replace the `UnauthorizedClientException` handling with an
  appropriate Spring Security exception or drop the handler.
- The migration `src/main/resources/database/catgenome/postgres/v2016.12.02_18.26__Oauth_tables.sql`
  is already applied on existing databases — **do not edit or delete it** (Flyway checksum).
  Add a new migration to drop the tables if you want them gone.

**1g. Google PaLM 2**

- Delete `manager/llm/GCPPaLM2.java` and its registration in `LLMHandler` / `LLMService`
  (grep for `PALM`, `Palm2`).
- Remove `com.google.cloud:google-cloud-aiplatform:3.37.0` from `build.gradle`.
  **Check whether `google-api-services-customsearch` and `google-http-client-jackson2`
  (used by patents/PubMed search) were relying on aiplatform's transitive
  `google-auth-library` / `gax` versions** — if so, pin them explicitly.
- Remove `llm.google.palm2.prompt.template` from all `profiles/*/catgenome.properties`.
- Remove the PaLM 2 option from the client LLM selector (grep the client for `palm`).

**1h. Singularity and SonarQube**

- Delete `singularity/`.
- Remove `id "org.sonarqube" version "2.2"` from `server/catgenome/build.gradle` and
  `server/ngb-cli/build.gradle`; delete the `systemProp.sonar.*` lines from the root
  `gradle.properties` (they point at an unreachable internal host, `10.66.128.3:9000`).
- Also remove the unused `org.akhikhl.gretty` buildscript classpath entry from
  `server/catgenome/build.gradle` — it is declared and never applied.

**Phase 1 execution findings** *(written as the phase was carried out)*

- **`hadoop-client` was the only supplier of Jettison.** `org.codehaus.jettison:jettison:1.1`
  arrived transitively through Hadoop, and four *surviving* classes import
  `org.codehaus.jettison.json.JSONObject` — `manager/externaldb/HttpDataManager`,
  `manager/externaldb/ncbi/NCBIDataManager`, `manager/externaldb/pdb/PdbEntriesManager`,
  `manager/externaldb/OpenTargetsManager`. Removing Hadoop broke `compileJava` with
  7 errors. Fixed by declaring jettison explicitly in `server/catgenome/build.gradle`,
  pinned to **1.1** — the version Hadoop 2.2.0 resolved — so Phase 1 stays behaviour-neutral.
  **1.1 is from 2009 and is affected by CVE-2022-40149/40150 (fixed in 1.5.2); replace it
  when the dependency set is refreshed** (Phase 2/3), or migrate those four classes to
  Jackson and drop it.
- **Enum gap reporting.** `BiologicalDataItemResourceType.getById()` previously returned
  `null` for an unknown id. It now throws `IllegalArgumentException` with a message that
  *names* the removed type when the id is 5 or 6 ("resource type 6 (GA4GH) is no longer
  supported. Files registered with it have to be unregistered and, if still needed,
  re-registered from a supported resource type."). The mapping lives in a
  `REMOVED_TYPE_NAMES` map exposed by `getRemovedTypeNames()`. This satisfies the
  "detect and report, do not NPE" risk item without a Flyway scan: the failure surfaces at
  the point the unmappable row is read, naming the file and the fix.
  `null` in → `null` out is preserved, because callers pass an unset type legitimately.
- **The CLI copy of the enum has a pre-existing bug, left alone.** `ngb-cli`'s
  `BiologicalDataItemResourceType.idMap` never registers `AZ(8)`, so `getById(8L)` returns
  `null` there. Its `getById` is dead code in the CLI (grep: only `getTypeFromPath` and the
  constants are used), so this is noted, not fixed — fixing it is out of Phase 1's scope.
- **`ReferenceManager.registerGenome` lost a catch clause.** `catch (InterruptedException |
  ExternalDbUnavailableException e)` became unreachable once `registerGA4GH` was deleted
  (nothing left in the `try` declares either), so javac rejected it. Removed; the
  `try`/`finally` rollback block is untouched. Behaviour-neutral for the surviving paths —
  `IOException` already propagated.
- **`ga4gh.google.*` test properties** removed from all four properties files
  (`profiles/{h2,postgres}/test-catgenome.properties`,
  `src/test/resources/test-catgenome-{acl,auth}.properties`).
- **Fixtures deleted with the tests:** `src/test/resources/externaldb/data/GA4GH_id10473*.json`
  (5 files) and `src/test/resources/templates/1000-genomes.chrMT.vcf` — a repo-wide grep
  confirmed the latter was referenced only by the now-deleted
  `VcfManagerTest.CLASSPATH_TEMPLATES_FELIS_CATUS_VCF_GOOGLE` constant.
- **`UrlValidatorService` left alone** as instructed: the `isRemotePath` GA4GH omission is
  moot now that the resource type is gone.
- **`HttpDataManager` stays.** Only the GA4GH-only `@Autowired` fields in `ReferenceManager`,
  `NibDataReader` and `VcfManager` were removed; BLAT, Ensembl, Uniprot, NCBI PUG, PDB and
  OpenTargets all still use the bean.
- **`spring-security-oauth2` was the only supplier of Jackson 1.x.**
  `org.codehaus.jackson:jackson-mapper-asl:1.9.13` came in transitively through it, and
  `util/Utils.java` and `dao/pdb/PdbFileDao.java` imported `org.codehaus.jackson.map.{ObjectMapper,
  type.TypeFactory}`. Rather than pin a 2013 library that Phase 2 would only have to remove
  again, the five import lines were switched to
  `com.fasterxml.jackson.databind.{ObjectMapper, type.TypeFactory}` — already a direct
  dependency at 2.12.5. The call sites are `writeValueAsString(Map|List)`,
  `readValue(String, HashMap.class)` and
  `readValue(String, TypeFactory.defaultInstance().constructMapType(...))`, all identical API
  and identical output between Jackson 1.9 and 2.12 for these types. This is the one place
  Phase 1 changed code that was not on a deletion list; flagged because it widens the phase
  slightly.
- **`controller/person/{UserController,RoleController}` were NOT deleted — they were moved.**
  The plan says "delete `controller/person/`", but both classes are modern: they delegate to
  `manager/user`'s `UserSecurityService` / `RoleSecurityService` and never touch
  `entity/person/Person`. They are live: `client/client/dataServices/user/user-data-service.js`
  and `role-data-service.js` call `/user`, `/users`, `/user/current`, `/role/loadAll` etc., and
  the SAML smoke test's step [4] hits `GET /restapi/user/current`. They were misfiled, not
  legacy, so they moved to `controller/user/` (package rename only — nothing imports them by
  package, Spring finds them by component scan and the URL mappings are unchanged).
- **Nothing has a foreign key *into* `CATGENOME.PERSON`.** Its only FK points outward, at
  `PERSON_ROLE`. So the drop migration
  `v2026.08.20_12.00__drop_legacy_person_tables.sql` (added for both flavours) drops `PERSON`,
  then `PERSON_ROLE`, then the `S_PERSON` sequence, and needs no other change.
- **`gradle.properties` was deleted, not emptied** — it held nothing but the two
  `systemProp.sonar.*` lines pointing at the unreachable `10.66.128.3:9000`.
- **`GOOGLE_MED_PALM2` was left in `LLMProvider`.** It is dead already — no `LLMHandler`
  implements it, the client never offers it, and `LLMService.getHandler` answers
  "GOOGLE_MED_PALM2 is not supported." Removing it is not on any deletion list, so it stays;
  it is a free deletion whenever someone wants it.
- **Dropping `google-cloud-aiplatform` shifts three resolved versions downward**, because it
  was the highest bidder for them. `guava` 32.1.3-jre → 31.1-jre (now from
  `google-api-services-customsearch`), `commons-codec` 1.16.1 → 1.15, `checker-qual` 3.42.0 →
  3.12.0 (annotations only). The plan's VERIFY question — whether
  `google-api-services-customsearch` / `google-http-client-jackson2` depended on aiplatform's
  transitive `google-auth-library` / `gax` — **resolves to no**: `google-api-client:2.2.0`,
  `google-http-client:1.44.1` and `google-oauth-client:1.34.1` are unchanged, and
  `GooglePatentManager` authenticates with an API key, never with ADC. The whole
  gRPC/protobuf/gax/auth-library tree (~35 modules) leaves the classpath. No pinning needed.

**Phase 1 exit-criteria results** *(all run, all in the containers)*

- `make jar` (`buildJar -PnoTest`, profile `jar`) **BUILD SUCCESSFUL** — this is the profile
  that used to take the now-deleted `warExploded` branch. `make cli-build` and `buildDocs` were
  run separately, because `buildJar` covers neither and Phase 1 edited both `ngb-cli/build.gradle`
  and `mkdocs.yml`: both succeed, and mkdocs reports no broken links after the two installation
  pages were removed from the nav.
- `make lint` green (only the pre-existing `CustomChatResponse.java:44` MemberName **WARN**).
- `rm -rf ../contents && make test` → 528 tests, 2 failed, 21 skipped. `make reset-pg &&
  make test-pg` → 528 tests, 10 failed, 21 skipped. Failure *identity* is what matters: the
  documented set minus `VcfManagerTest.testLoadSmallScaleVcfFileGa4GH`, i.e. 1 on H2 and 9 on
  PostgreSQL, **plus** `PdbDataManagerTest.testParse`, which is live-RCSB drift and was already
  red on unmodified Phase 0 code when Phase 0's criteria were re-verified at the start of this
  session. Nothing was re-baselined. 537 → 528 is the 9 tests deleted with the features they
  covered; `TEST-BASELINE.md` lists them.
- `make up` + `make smoke` → HTTP 200, `{"payload":"2.8.0","status":"OK"}`.
  `make up-saml` + `make smoke-saml` → `SAML SSO OK`, with step [4] `GET /restapi/user/current`
  returning `ROLE_ADMIN`/`NGB_ADMINS` — which is also the live proof that moving
  `UserController` to `controller/user/` kept the endpoint working.
- **SMOKE_A3 end-to-end** (the `BiologicalDataItemResourceType` check): `ngb reg_ref /ngs/A3.fa
  --name SMOKE_A3` registers with `"type":"FILE"`, and `/restapi/reference/3/loadChromosomes`
  answers `A1` at 56,400 bp. `FILE` handling is intact after the enum edit. (One transient
  first-request failure right after `docker-compose up -d cli` — "Failed to execute a request",
  no server-side log line, no id consumed from the sequence — the retry was clean.)

**Exit criteria**

- `./gradlew buildJar -PnoTest` (i.e. `make jar`) succeeds on JDK 8 / Gradle 3.3.
- `make test` and `make test-pg` show the Phase 0 baseline **minus** the GA4GH failure, and
  no new failures.
- `make lint` green.
- `make up` + `make smoke`; `make up-saml` + `make smoke-saml` both still pass.
- Register a reference and load chromosomes end-to-end (the README's `SMOKE_A3` check) to
  confirm the `BiologicalDataItemResourceType` edits did not break `FILE` handling.
- Update `TEST-BASELINE.md` with the post-removal numbers.

**Risks**

- **Enum id gaps.** `BiologicalDataItemResourceType.getById()` returns `null` for the
  removed 5 and 6. Any existing database row with `TYPE` 5 or 6 becomes unmappable.
  Add a Flyway migration (or a startup warning) that counts such rows and logs a clear
  message; do not silently NPE. Document it in the release notes.
- GA4GH removal touches `ReferenceManager` / `VcfManager`, which are core. Lean on
  `make test` here, and click through a VCF and a reference track in the UI.

---

### Phase 2 — Build toolchain + Spring Boot 2.7 waypoint (JDK 17)

**Goal:** Gradle 7.6.x, Lombok ≥ 1.18.30, Spring Boot 2.7.18, compiling and running on
**JDK 17**. Still `javax.*`. Still `spring-security-saml2-core`.

**Why a Boot 2.7 waypoint instead of going straight to 3.5:** there is no compiling
intermediate state between "Gradle 3.3 + Boot 1.5" and "Gradle 8 + Boot 3.5" — the Boot
Gradle plugin, the dependency-scope keywords, the packaging task and the servlet namespace
all have to move together. Going via 2.7 splits that into two steps that each end at a
*running application with a green test suite*:

- 1.5 → 2.7 is the well-documented part: `bootRepackage` → `bootJar`,
  `EmbeddedServletContainerCustomizer` → `WebServerFactoryCustomizer`,
  `WebMvcConfigurerAdapter` → `WebMvcConfigurer`, relaxed-binding changes. Spring Security
  4 → 5 also happens here, and `spring-security-saml2-core` 1.0.10 still works with Spring
  Security 5.x (**VERIFY** — this is the load-bearing assumption of the waypoint; if it
  does not hold, fold the SAML rewrite forward and run Phase 2 with SAML disabled, exactly
  as Phase 3 does).
- 2.7 → 3.5 is then purely the jakarta/Spring-6 step, which has first-class tooling
  (`spring-boot-properties-migrator`, OpenRewrite `UpgradeSpringBoot_3_x`).

**Why Gradle 7.6 and not 8 yet:** the Spring Boot 2.7 Gradle plugin officially supports
Gradle 6.8–7.x, not 8.x. **VERIFY** at execution time; if Boot 2.7's plugin turns out to be
fine on Gradle 8.14, go straight to 8 and skip the second wrapper bump in Phase 3.

**Why JDK 17 and not 21 yet:** Spring Framework 5.3 / Boot 2.7 do not officially support
Java 21. 17 is inside their supported range and still forces you to fix everything that
Java 9+ modularity breaks. Phase 3 moves to 21.

**Tasks**

1. **Gradle wrapper 3.3 → 7.6.x.** Replace the `task wrapper(type: Wrapper)` block in the
   root `build.gradle` with the modern `wrapper` task configuration, then regenerate
   `gradlew` / `gradlew.bat` / `gradle/wrapper/*`.
2. **Dependency configurations.** `compile` → `implementation` (or `api` where a module's
   consumers need it), `testCompile` → `testImplementation`, `testRuntime` →
   `testRuntimeOnly`, `providedRuntime` → gone with the WAR. Applies to both
   `server/catgenome/build.gradle` and `server/ngb-cli/build.gradle`.
3. **Lombok.** `compileOnly "org.projectlombok:lombok:<latest 1.18.3x>"` **plus**
   `annotationProcessor` for the same coordinate — from Gradle 5, `compileOnly` alone no
   longer runs the processor. Same in `ngb-cli`. Also add
   `testCompileOnly` / `testAnnotationProcessor` (472 files use Lombok; some are tests).
4. **Java toolchain.** In both Java modules:
   ```groovy
   java {
       toolchain { languageVersion = JavaLanguageVersion.of(17) }
   }
   ```
   The toolbox provides `JAVA_HOME_17` after Phase 0; point Gradle at it via
   `org.gradle.java.installations.paths` or run the build with `with-java17`.
5. **Spring Boot 2.7.18.** Plugin `id 'org.springframework.boot' version '2.7.18'` plus
   `id 'io.spring.dependency-management' version '1.1.x'` (from Boot 2.x the BOM is a
   separate plugin). Then **delete the manual version pins that the BOM now manages** —
   `versionSpring`, `versionSpringSecurity`, `versionJackson`, `versionSlf4j`,
   `versionJUnit`, `versionMockito`, the `ext['jetty.version']` and `ext['log4j2.version']`
   overrides — and keep explicit versions only for libraries Boot does not manage
   (Lucene, htsjdk, biojava, POI, paxtools, opencsv, gson, azure-*, big, igv, libnewicktree,
   libsbgn, chilay-sbgn, java-jwt, `spring-security-saml2-core`).
6. **Packaging.** `bootRepackage` → `bootJar`. `jar { archiveName "catgenome.jar" }` →
   `archiveFileName`. The root `build.gradle`'s `buildJar` / `buildCli` tasks call
   `bootRepackage` by name (and so does `.devenv/Makefile`'s `jar-fast` /
   `jar-pg-fast`) — update all three. Also update `archiveName`/`baseName`/`destinationDir`
   → `archiveFileName`/`archiveBaseName`/`destinationDirectory` in the `Zip`/`Tar` tasks
   throughout both modules and in `server/ngb-cli/build.gradle`'s `distZip`/`distTar`.
7. **`GradleBuild` tasks.** The root build orchestrates via four `type: GradleBuild` tasks
   with `startParameter.projectProperties`. This still works on Gradle 7 but is
   discouraged and interacts badly with the configuration cache. Either keep them
   (lowest risk) or convert to composite/`Exec` invocations. **Recommendation: keep, and
   note it.** `buildCli`'s `dir = "$rootDir/ngb-cli/catgenome"` is already a wrong path that
   happens not to matter — fix it to `$rootDir/server/ngb-cli`.
8. **Plugin replacements.**
   - `net.saliman.properties` 1.4.4 → drop it. Confirm what it actually provides: the build
     uses `project.filterTokens` (in `profiles.gradle` and `ngb-cli/build.gradle`), which
     comes from this plugin. Either upgrade to a Gradle-7-compatible release or replace
     `filterTokens` with a plain `Map` in the build script. **The latter is simpler** — it
     only ever holds `rootDirPath` and `version`.
   - `com.github.ManifestClasspath` 0.1.0 → drop. It worked around Windows command-line
     length limits; modern Gradle handles this.
   - `de.undercouch.download` 3.1.2 → 5.x (still needed by the JRE-bundle tasks, D7).
   - `com.github.johnrengelman.shadow` 1.2.4 (in `ngb-cli`) → 8.x. **Check whether it is
     actually used** — `ngb-cli` ships via `distTar`, and no `shadowJar` task is referenced.
     If unused, delete it.
   - `com.moowork.node` 1.1.1 → **replace with plain `Exec` tasks** invoking the `npm`
     already on the toolbox PATH. Per D12 the frontends are frozen, so the plugin's only job
     — downloading Node — is unnecessary in the container. It is applied in **two** places,
     both of which carry a copy-pasted 60-line `checkInstalledNodeVersion` /
     `isSupportedNodeVersion` comparator that can be deleted outright:
     - `client/build.gradle` — keep `npmPrune`, `npmInstall`, `buildUI` as task names so
       `.devenv/Makefile`'s `ui` target and the root `ui` task keep working.
     - `export-templates/target-identification/build.gradle` — keep `npmPrune`,
       `npmInstall`, `buildTemplate`; the root `templates` task depends on
       `:export-templates:target-identification:buildTemplate`. Also delete its
       `tasks.withType(NodeTask) { doLast { println() } }` blocks, whose comment already says
       *"can be removed after moving to Gradle 3.4"*.

     Document that a host build now requires Node 14 on PATH. **Note a latent mismatch:**
     this module pins `nodeVersion = "14.17.5"` but depends on Vite 4, whose documented
     minimum is Node 14.18. It builds today, so leave it alone (D12) — but if
     `buildTemplate` breaks, that is the reason, and the fix is a Node patch bump, not a
     Vite change.
9. **Static analysis.**
   - Checkstyle 7.2 → 10.x/11.x. Both modules have their own
     `config/checkstyle/checkstyle.xml`; expect module/attribute renames.
   - PMD 5.5.1 → 7.x. **The rulesets need a full rewrite**:
     `server/catgenome/config/pmd/pmd-ruleset.xml`,
     `pmd-ruleset-feature-index-manager.xml` and `server/ngb-cli/config/pmd/pmd-ruleset.xml`
     all use the pre-PMD-6 `rulesets/java/basic.xml` style paths, which no longer exist
     (PMD 6 moved to `category/java/*.xml`; PMD 7 changed rule names again). Rewrite them to
     the equivalent PMD 7 categories, keeping the existing exclusions
     (`VariableNamingConventions`, `ShortClassName`, `LongVariable`, `ShortVariable`,
     `AbstractNaming`, the `maximumStaticImports=6` property, the `VgPb*` file exclusion).
     Expect new violations; **fix them, do not blanket-suppress** — Phase 0 left `lint`
     green, so anything new is either a real finding or a rule you consciously disable.
10. **Repositories.**
    - `maven { url "http://www.biopax.org/m2repo/releases/" }` is **plain HTTP, which Gradle
      7+ rejects by default.** First check whether `org.biopax.paxtools:paxtools-core:5.1.0`,
      `:sbgn-converter:5.0.0` and `pathwaycommons:chilay-sbgn:3.0.0` resolve from Maven
      Central. If they do, delete the repository. If not, add
      `allowInsecureProtocol = true` (and note it as a supply-chain risk), or vendor the
      three jars into a `flatDir`.
    - The `flatDir { dirs 'lib' }` entry points at a directory that does not exist. Delete it.
11. **Spring Boot 1.5 → 2.7 source changes.** Non-exhaustive; the Boot 2.0 and 2.x
    migration guides are the reference:
    - `app/AppMVCConfiguration.java`: `WebMvcConfigurerAdapter` → implement
      `WebMvcConfigurer`; `EmbeddedServletContainerCustomizer` /
      `EmbeddedServletContainerFactory` / `TomcatEmbeddedServletContainerFactory` →
      `WebServerFactoryCustomizer<TomcatServletWebServerFactory>` /
      `TomcatServletWebServerFactory`.
    - `app/TomcatConfigurer.java` + `TomcatConfigurerImpl.java`: same factory type change.
    - `configurer.setUseSuffixPatternMatch(false)` is deprecated in 5.3 and **removed in
      Spring 6** — it can stay in Phase 2 but must go in Phase 3. `false` is the Spring 6
      default, so the call simply disappears.
    - `local.server.port` (read in `Application.startupLoggingListener`) →
      `local.server.port` is still available via `@LocalServerPort`; verify the property
      still resolves, otherwise use `WebServerInitializedEvent`.
    - Spring Security 4 → 5: `WebSecurityConfigurerAdapter` still exists (deprecated), so
      the three security configs can stay structurally as-is. Password encoding and
      `antMatchers` semantics are unchanged enough. Expect to touch
      `AclSecurityConfiguration` (Spring Security ACL 5.x) and
      `NGBMethodSecurityExpressionHandler`.
    - Add `spring-boot-properties-migrator` as a `runtimeOnly` dependency for one boot cycle;
      it prints every renamed property. Then remove it.
    - Move `spring-boot-devtools` to `developmentOnly` (or delete it).
12. **`ngb-cli` module.** Same configuration/lombok/toolchain changes. It has no Spring, so
    it should be mechanical. Bump `fast-classpath-scanner` 2.0.9 if it misbehaves on 17
    (it scans the classpath reflectively — a classic Java 9+ breakage point). **VERIFY**;
    the successor is `io.github.classgraph:classgraph`. Also fix
    `task regression(type: Exec) { args 'script\\regression_test.sh' }` — a Windows path
    separator that cannot work on Linux; the script lives at
    `server/ngb-cli/script/regression_test.sh`. And note it declares both
    `commons-collections:3.2.2` (v3) and no collections4 — align with the server module's
    choice if you touch it.
13. **`e2e/cli/cli-tests.gradle`** uses `String.execute()` and `project.hasProperty` inside
    an `ext` closure. Groovy 3 (Gradle 7) should still accept it; run `make cli-test` to
    confirm.

**Exit criteria**

- `make jar`, `make jar-pg`, `make cli-build` succeed with Gradle 7.6 on **JDK 17**.
- `make test` and `make test-pg` match the Phase 1 baseline. Mockito moves to 3.x/4.x with
  Boot 2.7 — `org.mockito.Matchers` is removed in Mockito 3, so the two
  `import static org.mockito.Matchers.*` sites and the two
  `org.mockito.internal.matchers.{Equals,Find}` usages need porting to
  `org.mockito.ArgumentMatchers`. `MockitoAnnotations.initMocks` → `openMocks`.
- `NGB_JAVA_VERSION=17 make up` boots and `make smoke` answers.
- `make up-saml` + `make smoke-saml` still complete an SSO login (the whole point of the
  waypoint).
- `make lint` green with the rewritten rulesets.
- `make cli-test` passes.

**Risks**

- **This is the largest single phase.** Budget accordingly. Consider committing it in three
  sub-commits: (a) Gradle 7.6 + configurations + lombok + toolchain, still Boot 1.5 —
  *this will not build*, so squash it if you prefer; (b) Boot 2.7; (c) static analysis.
- The `profiles.gradle` `copyConfiguration` task uses `doLast` with `copy {}` and mutates
  `filterTokens` at execution time. Gradle 7 tolerates this but it is not
  configuration-cache-safe. Do not enable the configuration cache in this phase.
- `processResources.dependsOn copyConfiguration` with a task that writes directly into
  `$buildDir/resources/main` will trip Gradle 7's stricter task-output overlap checks.
  Expect to convert `copyConfiguration` into a proper `Copy`/`ProcessResources`
  configuration rather than a `doLast` block.

---

### Phase 3 — Spring Boot 3.5, Jakarta, JDK 21 (security reduced to anonymous)

**Goal:** the application compiles and runs on **JDK 21** with Spring Boot 3.5 / Spring 6,
`jakarta.*` throughout, and `make test` green — with **SAML and JWT temporarily out of the
build**. Auth mode `none` only.

**Why security is deferred:** `spring-security-saml2-core` (OpenSAML 2) cannot work with
Spring Security 6 at all, and `SAMLSecurityConfiguration` is 589 lines of OpenSAML 2 bean
wiring — a full rewrite (Phase 4). Bundling that rewrite with a ~1,000-file namespace
migration means a single opaque red state with two unrelated causes. Splitting them gives
two attributable checkpoints.

**Tasks**

1. **Gradle 7.6 → 8.14.x** (Java 21 toolchain support landed in Gradle 8.5). Set
   `toolchain { languageVersion = JavaLanguageVersion.of(21) }`.
2. **Spring Boot plugin + BOM → 3.5.x.** Re-check which explicit versions the new BOM now
   manages and delete those pins.
3. **`javax.*` → `jakarta.*`.** Measured surface:
   - `javax.servlet.*` — 46 imports across ~30 files → `jakarta.servlet.*`. Straight rename.
   - `javax.xml.bind.*` — **282 imports**, but almost all are JAXB *annotations* in the
     generated-looking external-DB binding classes:
     `manager/externaldb/bindings/{uniprot (36 files), dbsnp (14), ecsbpdbmap (7), rcsbpbd (3)}`.
     Rename to `jakarta.xml.bind.*` and **add the runtime, which is no longer in the JDK**:
     `jakarta.xml.bind:jakarta.xml.bind-api` + `org.glassfish.jaxb:jaxb-runtime`.
     The handful of real API uses (`JAXBContext`, `Unmarshaller`, `JAXBElement`,
     `JAXBException`, `XmlRegistry`, `XmlElementDecl`) are in
     `manager/externaldb/`, `manager/externaldb/ncbi/` and `manager/pathway/`.
   - `javax.xml.{xpath,parsers,datatype,stream,namespace}` (37 imports), `javax.net.ssl`
     (11), `javax.naming.ldap` (2) — **these stay in the JDK. Do not rename them.** This is
     the most common mistake made by automated jakarta migrations; if you use OpenRewrite or
     a sed script, scope it to `javax.servlet` and `javax.xml.bind` only.
4. **Consider OpenRewrite** for the mechanical bulk: `UpgradeSpringBoot_3_5` chains the
   javax→jakarta and Boot-2→3 recipes. Run it, then review every diff — it will not know
   about the forked htsjdk readers, the XML bean files, or `profiles.gradle`.
5. **Security: reduce to anonymous.**
   - Delete `app/SAMLSecurityConfiguration.java`, `app/JWTSecurityConfiguration.java`,
     `app/CustomAwareAuthenticationSuccessHandler.java`, `security/saml/**`,
     `security/jwt/**` — **or** move them to a scratch location outside `src/`. Keep them
     retrievable: Phase 4 rewrites them and the old code is the specification for what
     the SAML attribute mapping and JWT claims must keep doing. Recommended: delete in git
     and recover from `git show` in Phase 4, so the tree stays clean.
   - Remove `spring-security-saml2-core` and `com.auth0:java-jwt` from `build.gradle`
     (both come back in Phase 4).
   - Rewrite `app/NoSecurityConfiguration.java` for Spring Security 6:
     `WebSecurityConfigurerAdapter` is **removed**, so it becomes a
     `@Bean SecurityFilterChain` using the lambda DSL
     (`http.authorizeHttpRequests(a -> a.anyRequest().permitAll()).csrf(c -> c.disable())`).
     Note `authorizeRequests` → `authorizeHttpRequests` and `antMatchers` →
     `requestMatchers`.
   - `app/SecurityConfiguration.java`: reduce the `@Import` list to
     `NoSecurityConfiguration` + `AclSecurityConfiguration`.
   - Keep `AclSecurityConfiguration` and `security/acl/**` — ACL is authorization and is
     needed by D4/D6. Spring Security 6 ACL API changes: `JdbcMutableAclServiceImpl`,
     `LookupStrategyImpl`, `PermissionGrantingStrategyImpl`,
     `NGBMethodSecurityExpressionHandler`/`Root`. `@EnableGlobalMethodSecurity` →
     `@EnableMethodSecurity`. **If ACL turns out to depend on the removed adapter in a way
     that cannot be resolved without the SAML rewrite, disable ACL here too and restore it
     in Phase 4** — record which you chose.
   - `manager/AuthManager.java` and `security/UserContext` / `BrowserUser` will need
     attention; keep them compiling against an anonymous principal.
6. **Cache: EhCache 2 → Caffeine (D11).** Spring 6 removed
   `org.springframework.cache.ehcache` entirely.
   - Delete `src/main/resources/conf/catgenome/ehcache.xml` and the three
     `EhCache*` beans in `applicationContext-cache.xml`; replace `cacheManager` with a
     Caffeine-backed `CacheManager` (a `@Bean` in a small `@Configuration`, or Boot's
     `spring.cache.caffeine.spec` properties). Two caches exist: `proteinTrack`
     (1,000 entries, 5 s TTI, FIFO) and `indexCache` (100 MB heap, 600 s TTI). Both are
     heap-only (`persistence strategy=none`), so Caffeine is a clean fit.
   - **Interim step for the index cache:** `EhCacheBasedIndexCache` is injected into the
     *forked* htsjdk readers, which are not removed until Phase 7. Keep the class and its
     five-method surface (`getFromCache`, `putInCache`, `evictFromCache`, `contains`,
     `clearCache`) and reimplement the internals on a Caffeine `Cache<String, IndexCache>`.
     ~30 lines. Rename it (e.g. `FeatureIndexFileCache`) or leave the name; Phase 7 deletes
     it and the `server.index.cache.enabled` property along with the fork.
   - `<task:annotation-driven executor="taskExecutor"/>` and the `ThreadPoolTaskExecutor`
     bean stay, but verify the `spring-task` XSD still resolves.
7. **Connection pool: c3p0 → HikariCP.** Replace the `ComboPooledDataSource` bean in
   `conf/catgenome/applicationContext-database.xml` with `HikariDataSource` (or move the
   datasource to `spring.datasource.*` properties and delete the bean). Property names
   change: `maxPoolSize` → `maximumPoolSize`, `initialPoolSize` has no direct equivalent
   (use `minimumIdle`), `driverClass` → `driverClassName`, `jdbcUrl` is the same. Keep the
   existing `database.*` property names in `catgenome.properties` so operators are not
   broken; just map them. Drop `com.mchange:c3p0`.
8. **Servlet/Tomcat.** Boot 3.5 brings Tomcat 10.1 (Jakarta Servlet 6). `tomcat.setTldSkip`
   in `AppMVCConfiguration.tomcatContainerCustomizer` may no longer exist — **VERIFY** and
   drop if so. `org.apache.catalina.webresources.StandardRoot` in `TomcatConfigurerImpl`
   should survive.
9. **API docs: mangofactory swagger → springdoc.** `com.mangofactory:swagger-springmvc:1.0.2`
   cannot work with Spring 6 MVC.
   - Delete `config/SwaggerConfig.java` and its `@Import`/`@Bean` in `AppMVCConfiguration`.
   - Add `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.x`. **springdoc generates
     documentation from the controllers with zero annotations**, so the 42 files carrying
     `com.wordnik.swagger.annotations.*` do not have to be rewritten in this phase —
     just remove the `com.wordnik` / `com.mangofactory` dependencies and delete the now-unused
     imports and annotations (a mechanical delete). Re-annotating with
     `io.swagger.v3.oas.annotations` (`@Api` → `@Tag`, `@ApiOperation` → `@Operation`,
     `@ApiResponses` → `@ApiResponses`, `@ApiModelProperty` → `@Schema`) is **Phase 8 polish**.
   - Remove the `org.webjars:swagger-ui:2.0.24` dependency and the
     `classpath:/META-INF/resources/webjars/swagery-ui/2.0.24/` resource handler in
     `AppMVCConfiguration.addResourceHandlers`; springdoc serves its own UI. Delete the
     committed `src/main/resources/static/swagger-ui/` and `src/main/webapp/swagery-ui/`
     trees, and the `swagger-ui/**` exclusion in the root `build.gradle` `ui` task.
     *(Note: the exact directory names are `swagger-ui` — check before deleting.)*
   - Update the springdoc paths in the security configs' unsecured-resource lists
     (`/swagger-ui/**`, `/api-docs/**` → springdoc's `/v3/api-docs/**`).
10. **Logging.** The build currently excludes `spring-boot-starter-logging`, `logback` and
    `log4j-slf4j-impl`, and pulls `slf4j-log4j12` + `org.apache.logging.log4j:log4j` 2.17.1
    — i.e. slf4j → log4j **1.x** bridge in front of log4j **2**, which is confused. The
    `profiles/*/log4j.xml` files are log4j configuration. Straighten this out: either
    standardise on `spring-boot-starter-log4j2` (keeping `log4j.xml`, converted to log4j2
    format if it is still log4j-1 format — **check**) or drop the exclusions and use
    Boot's default Logback. **Recommendation: log4j2**, because the `profiles/*/log4j.xml`
    files are part of the operator-facing configuration.
11. **JDK 21 runtime flags.** Once it boots, remove any `--add-opens` from
    `.devenv/.env`'s `JAVA_EXTRA_OPTS` and confirm it still boots clean. If something still
    needs an `--add-opens`, identify the library and record it — a needed `--add-opens` on
    Spring 6 usually means a stale dependency elsewhere.
12. **Test framework.** Boot 3.5's `spring-boot-starter-test` is JUnit 5 only. Per D13, add:
    ```groovy
    testImplementation 'junit:junit:4.13.2'
    testRuntimeOnly 'org.junit.vintage:junit-vintage-engine'
    test { useJUnitPlatform() }
    ```
    `@RunWith(SpringJUnit4ClassRunner.class)` / `@RunWith(MockitoJUnitRunner.class)` keep
    working under vintage. Mockito goes to 5.x (Boot-managed) — mostly compatible with the
    4.x code from Phase 2. `unitils-core` 3.4.2 is used for `ReflectionAssert` in exactly 3
    test files; if it fails on JDK 21 (it reflects heavily), inline the three assertions
    and drop the dependency.
    `src/test/resources/applicationContext-test.xml` and `catgenome-servlet-test.xml` need
    the same Spring 6 treatment as the main XML.

**Exit criteria**

- `make jar` and `make jar-pg` build with Gradle 8.14 on **JDK 21**.
- `NGB_JAVA_VERSION=21 make up` boots with `AUTH_MODE=none`; `make smoke` returns 200 and a
  version string; `JAVA_EXTRA_OPTS` is empty.
- `make test` matches the Phase 1 baseline **minus** the ACL/JWT/security tests, which are
  expected to be absent or disabled in this phase — **enumerate exactly which tests you
  disabled and why**, in the commit message and in `TEST-BASELINE.md`.
- `make test-pg` likewise.
- Click through, in the UI: a reference track, a genes track, a VCF track, a BAM track.
  Verify `/swagger-ui` (or springdoc's path) renders.
- `make cli-test` passes against `AUTH_MODE=none`.
- `make lint` green.

**Risks**

- **Highest-risk phase after Phase 2.** ~1,000 files, a security subsystem removed, and the
  cache and connection pool replaced.
- The 47 XML bean files are the quiet risk. They reference unversioned XSDs, which is good,
  but `<aop:config proxy-target-class="true"/>`, `<tx:annotation-driven>`,
  `<context:property-placeholder>` and `<cache:annotation-driven>` all need to still parse
  against Spring 6's bundled schemas. Test the context load early — it is the fastest
  possible failure signal.
- `PropertiesFactoryBean` with `file:///${CATGENOME_CONF_DIR}/catgenome.properties` and
  `@Value("#{catgenome['...']}")` SpEL — a lot of configuration flows through this
  non-standard path. Verify `.devenv/ngb/catgenome.properties.tpl` still wins over the
  jar's baked-in defaults after the upgrade.

---

### Phase 4 — Security: SAML 2 and JWT on Spring Security 6

**Goal:** restore both auth modes. Anonymous keeps working; SAML SSO works from a browser;
JWT works for `ngb-cli` and scripted REST.

**Why separate:** it is a rewrite, not a migration. OpenSAML 2 → Spring Security's own
SAML2 support (`spring-security-saml2-service-provider`, built on OpenSAML 4) has a
completely different programming model — `RelyingPartyRegistration` instead of ~30 hand-wired
OpenSAML beans. `.devenv`'s `make up-saml` + `make smoke-saml` is the acceptance test, and
it works browser-lessly, which makes this phase unusually tractable.

**What the old code does (the specification)**

Recover it from git (`git show <phase-3-parent>:server/catgenome/src/main/java/...`).
Behaviour that must be preserved:

- **SP metadata** at `/catgenome/saml/metadata` (`MetadataDisplayFilter`).
- **IdP metadata read from a file** (`FilesystemMetadataProvider`) —
  `.devenv/secrets/idp-metadata.xml`, fetched from Keycloak by the `idp-metadata` service.
- **Signing key from a JKS keystore** (`JKSKeyManager`), with
  `SAMLContextProviderCustomSignKey` overriding the signing key selection.
- **Attribute → authority mapping**: `SAMLUserDetailsServiceImpl` reads the attributes named
  by the `saml.user.attributes` and `saml.authorities.attribute.names` properties and turns
  Keycloak group membership into NGB authorities. The `make smoke-saml` output is the
  contract: `authorities : ['ROLE_USER', 'ROLE_ADMIN', 'NGB_ADMINS']` for
  `ngbadmin@ngb.dev.local`.
- **User auto-registration** via `SamlUserRegisterStrategy`.
- **`security.default.admin`** grants admin to a named principal.
- **JWT**: `JwtTokenGenerator` mints RS256 tokens (RSA keypair from
  `.devenv/secrets/`), `JwtTokenVerifier` checks them plus required claims from
  `jwt.required.claims` (`k=v` comma-separated), `JwtFilterAuthenticationFilter` is inserted
  before `UsernamePasswordAuthenticationFilter`, and `/restapi/**` is the secured path with
  a documented unsecured list.
- **`OptionalSAMLLogoutFilter`** and **`CustomSAMLProcessingFilter`** — check what they
  customise before discarding.

**Tasks**

1. Add `org.springframework.security:spring-security-saml2-service-provider` (Boot-managed
   version). **VERIFY that the transitive `org.opensaml:opensaml-*` artifacts resolve from
   Maven Central**; historically OpenSAML was only on Shibboleth's repository
   (`https://build.shibboleth.net/maven/releases/`). If resolution fails, add that repo.
2. Write a single `SecurityFilterChain`-based configuration, conditional on the same
   `saml.security.enable` property, with:
   - `saml2Login()` + a `RelyingPartyRegistrationRepository` built from the IdP metadata
     file and the JKS keystore.
   - A `Converter<ResponseToken, Saml2Authentication>` (or a custom
     `AuthenticationProvider`) that reproduces `SAMLUserDetailsServiceImpl`'s attribute →
     authority mapping and the auto-registration side effect.
   - `saml2Metadata()` for the SP descriptor endpoint. **The path changes** — Spring
     Security's default is `/saml2/service-provider-metadata/{registrationId}`, not
     `/saml/metadata`, and the SSO endpoint is `/login/saml2/sso/{registrationId}` rather
     than `/saml/SSO`. Either configure the legacy paths explicitly or update
     `.devenv/keycloak/realm-ngb.json` and `.devenv/scripts/saml-login-test.py`.
     **Prefer configuring the legacy paths**, so existing IdP registrations at customer
     sites do not all have to be reconfigured — but if that is impossible, say so and
     document the required IdP change as a breaking release note.
3. Restore JWT: re-add `com.auth0:java-jwt` (latest 4.x — note the 3.x → 4.x API change:
   `Algorithm`/`JWTVerifier` signatures moved), port `security/jwt/**` to
   `jakarta.servlet`, and register `JwtFilterAuthenticationFilter` in the filter chain via
   `addFilterBefore`.
4. **Decouple JWT from SAML.** Today `JWTSecurityConfiguration` `@Autowired`s
   `SAMLAuthenticationProvider` and `SAMLEntryPoint`, so JWT cannot start without SAML.
   The rewrite should express this as *one* filter chain with two entry points
   (SAML redirect for browser paths, `RestAuthenticationEntryPoint` for `/restapi/**`),
   which removes the coupling as a side effect. This was not requested as a feature, so do
   not advertise a JWT-only mode unless it falls out for free — but do not recreate the
   artificial dependency.
5. **Delete the hardcoded port rewrite.** `CustomAwareAuthenticationSuccessHandler` does
   `StringUtils.replace(savedRequest.getRedirectUrl(), "8443", "8080")`. With Boot 3 you can
   expose both an HTTP and an HTTPS connector, so this hack is unnecessary. Removing it lets
   `.devenv` move SAML back to the conventional 8443 — update
   `.devenv/docker-compose.yml`, `.env.example`, the `smoke`/`up-saml` targets and the
   README's two "looks arbitrary but isn't" notes.
6. Restore `AclSecurityConfiguration` if it was disabled in Phase 3, and re-enable the ACL,
   security and JWT test classes.
7. Re-check `manager/AuthManager`, `security/UserContext`, `security/BrowserUser`,
   `manager/user/{UserManager,RoleManager}` and `controller/security/PermissionController`
   against the new authentication types.

**Exit criteria**

- `make up-saml` + `make smoke-saml` prints the same username and authority list as the
  pre-migration baseline, for both `ngbadmin@ngb.dev.local` and
  (`make smoke-saml U=ngbuser@ngb.dev.local P=user`) the plain user.
- Browser login at the SAML URL works; the post-login redirect lands on the app, not a
  refused connection.
- A JWT generated in the UI authenticates `ngb-cli` (`ngb set_token`, then
  `ngb list_ref`), and `make cli-test` passes in SAML mode.
- The ACL/security/JWT test classes are re-enabled and pass on **both** flavours (Phase 0
  fixed the PostgreSQL datasource, so they now genuinely run there).
- `make up` with `AUTH_MODE=none` still works.

**Risks**

- SAML compares URLs byte-for-byte; the endpoint-path change in item 2 is the single most
  likely source of a hard-to-diagnose failure. `make smoke-saml` prints each step of the
  web SSO profile — use it rather than a browser while iterating.
- Signature validation is switched **off** in the dev Keycloak realm
  (`.devenv/keycloak/realm-ngb.json`). Turn it on at least once before declaring this phase
  done, otherwise the keystore/signing path is untested.

---

### Phase 5 — Persistence: Flyway 10/11, H2 2.x, PostgreSQL 16

**Goal:** both flavours on modern Flyway and modern engines, with a working upgrade path
from existing databases.

**Why after Phase 3/4:** Flyway 10 needs a modern Boot to be managed cleanly, and you want
a green test suite before touching 116 SQL migration scripts.

**Tasks**

1. **Flyway 3.2.1 → the Boot-3.5-managed version.** The XML bean
   (`profiles/{h2,postgres}/applicationContext-flyway.xml` and the two
   `test-applicationContext-flyway.xml`) constructs `org.flywaydb.core.Flyway` directly with
   setters and `init-method="migrate"`. **Flyway 10 removed that constructor/setter API** in
   favour of `Flyway.configure()...load()`. Replace the XML beans with either:
   - Boot's Flyway auto-configuration (`spring.flyway.*` properties: `schemas=CATGENOME`,
     `sql-migration-prefix=v`, `locations=classpath:database/catgenome/<flavour>`,
     `placeholders.default.admin=${security.default.admin}`) — **preferred**, and it keeps
     the build-time flavour switch working by templating one property; or
   - a small `@Configuration` producing a `Flyway` bean via the fluent API.
   Note `depends-on="dataSource"` and migration-before-context-refresh ordering must be
   preserved: DAOs must not run before migration.
2. **Add `flyway-database-postgresql`** — from Flyway 10 the database-specific support is a
   separate artifact. Same check for H2.
3. **Baseline/checksum handling.** Flyway 10 validates checksums, and the README already
   records that two migration files were edited to make a fresh install work
   (`v2024.03.21_19.00__blast_task_id.sql` on both flavours,
   `v2024.02.19_18.00__acl_target_manager_role.sql` on PostgreSQL). Anywhere the old
   versions were recorded, this needs `flyway repair`. Also, Flyway 10's default
   `validateMigrationNaming` and the `v` prefix with a `v2016.11.21_17.58__` version format
   need verifying — the version string contains dots and underscores. **VERIFY that Flyway
   10 parses these version numbers identically to 3.2.1**; if not, this becomes a much
   larger problem (renaming applied migrations is not an option) and you may need
   `flyway.validateMigrationNaming=false` plus a custom `MigrationVersion` handling
   decision. Test explicitly against a database migrated by the *old* code, not just a
   fresh one.
4. **H2 1.3.176 → 2.x.** This is a real SQL/DDL migration:
   - H2 2.x is much stricter: reserved words (`VALUE`, `KEY`, `YEAR`, `ROW`…), `IDENTITY`
     semantics, `MERGE` syntax, `CREATE SEQUENCE` options, implicit type conversions, and
     `information_schema` casing all changed.
   - **The on-disk format changed and H2 2.x cannot open a 1.3 database file.** Existing H2
     deployments need an export/import (`SCRIPT TO` on 1.3, `RUNSCRIPT FROM` on 2.x).
     Provide a documented procedure and, if feasible, a helper script. This is a **breaking
     upgrade note for the release**.
   - Fix the stray apostrophe in the JDBC URLs if Phase 0 did not.
   - Work through the 58 h2 migration scripts. Do **not** edit already-applied ones; where a
     script is invalid on H2 2.x, that means existing databases can never be migrated by the
     new code, and the answer is the export/import path plus a fresh-install script set.
     Decide explicitly: (a) keep the 58 scripts and make them H2-2-compatible by editing
     (breaks checksums, requires `repair`), or (b) introduce a squashed baseline script for
     H2 2.x with `flyway.baselineVersion`. **(b) is cleaner given the file format already
     forces an export/import.** Record the decision.
5. **PostgreSQL 9.6 → 16.** Set `PG_VERSION=16` in `.devenv/.env.example` and `.env`, run
   `make reset-pg`. Bump the JDBC driver from `9.4-1206-jdbc4` to a current
   `org.postgresql:postgresql` (Boot-managed). Work through PostgreSQL 10+ removals in the
   58 scripts and the DAO SQL in the 42 `dao/*-dao.xml` files — most likely offenders:
   implicit casts, `::` usage, `WITH OIDS`, sequence functions, and anything relying on
   pre-10 `pg_catalog` columns.
6. **Test datasources.** With Phase 0's fix, the ACL/auth tests now follow `-Pdatabase`;
   confirm they still do after the Flyway rework.
7. Consider whether the two script sets can be **converged** now (the divergent role IDs are
   documented in the README). Converging is out of scope unless it falls out of the H2
   baseline decision — but note the divergence in `TEST-BASELINE.md` so it is not
   rediscovered.

**Exit criteria**

- `make test` and `make test-pg` at or better than the Phase 4 baseline, on H2 2.x and
  PostgreSQL 16.
- `make reset-ngb-data && make up` — fresh H2 install migrates cleanly from empty.
- `make reset-pg && make up-pg` — fresh PostgreSQL 16 install migrates cleanly from empty.
- **Upgrade path tested:** take an H2 database and a PostgreSQL database created by
  pre-migration code and run the new jar against them. Document exactly what an operator
  must do.
- `.devenv/README.md`'s "PostgreSQL is pinned to 9.6" note and the `.env.example` comments
  are updated.

**Risks**

- Highest data-loss risk in the whole migration. Never test only the fresh-install path.
- 116 SQL files plus SQL embedded in 42 XML DAO definitions. Budget for this being slower
  than it looks.

---

### Phase 6 — Lucene

**Goal:** current Lucene, with a clear operator story for the mandatory reindex (D8).

**Decision to make at the start of this phase:** Lucene **9.12.x** or **10.x**.
10.x requires Java 21 (which we have) but moved/removed more API and leans on the Panama
vector API; 9.12.x is a smaller delta from 6.6 and still requires the same reindex. Neither
can read a Lucene 6 index. **Recommendation: 9.12.x**, and treat 10.x as an optional later
bump. Record the choice.

**Indexes affected** (all persistent, all must be rebuilt):

| Index | Property |
|---|---|
| Feature index (per feature file) | derived from `files.base.directory.path` |
| Taxonomy | `taxonomy.index.directory` |
| Homologene | `homologene.index.directory` |
| Pathway | `pathway.index.directory` |
| Targets | `targets.index.directory` |
| NCBI | `ncbi.index.directory` |
| BAM coverage | `bam.coverage.index.directory` |

**Tasks**

1. Bump Lucene and work through the API changes across the 104 files. Known 6→9 hotspots:
   - `IndexWriterConfig` / `Directory` / `SimpleFSDirectory` → `FSDirectory`/`MMapDirectory`.
   - Numeric fields: `IntPoint`/`FloatPoint` already in use (good, that is the 6.0+ API),
     but `LegacyNumericRangeQuery`-style code, `DocValues` types and `SortField` construction
     changed.
   - `BooleanQuery.Builder`, `IndexSearcher.search(Query, int)` overloads,
     `TopFieldCollector`, `Analyzer` construction, `StandardAnalyzer` stop-word handling.
   - `lucene-backward-codecs` is still needed if you want to read one-major-version-old
     indexes, but **not** for reading 6.x from 9.x. Keep the dependency only if it is
     actually load-bearing.
   - `FeatureIndexManager`, `util/IndexUtils.java` and
     `security/acl`-adjacent grouping code (`lucene.index.max.size.grouping`) are the
     largest single sites.
2. **Add a startup/first-read guard (D8).** When an existing index directory is present but
   unreadable by the new Lucene, fail with a clear, actionable message naming the directory
   and the reindex command — not an `IndexFormatTooOldException` stack trace. Cover both the
   per-file feature indexes (which should be detected lazily, per file, and reported through
   the API) and the seven global index directories (which can be checked at startup).
3. **Reindex procedure.** Establish and document how an operator rebuilds each index. Some
   already have a REST/CLI trigger (feature index reindex exists — verify); others
   (taxonomy, homologene, pathway, NCBI) are built from external data dumps. Write it up in
   `docs/md/installation/` and the release notes. If any index has no rebuild path today,
   that is a gap this phase must close.
4. `HomologeneManagerTest.searchTest` needs a taxonomy index. Build it in test setup rather
   than committing a fixture index (a committed index would be invalidated by this very
   phase).

**Exit criteria**

- `make test`, `make test-pg` at or better than the Phase 5 baseline.
- Starting against a `contents/` directory containing Lucene 6 indexes produces the clear
  guard message, not a stack trace.
- After a documented reindex, in the UI: variation search across a VCF, gene search,
  homologs search, target identification, and a BAM coverage track all work.
- The reindex procedure is in the docs.

---

### Phase 7 — htsjdk: latest, no customisations

**Goal:** stock htsjdk (latest 4.x). Delete the fork. Per D9/D10 the index cache goes with it.

**Delete these** (`server/catgenome/src/main/java/com/epam/catgenome/util/feature/reader/`):

| File | Lines | Fate |
|---|---|---|
| `AbstractFeatureReader.java` | 231 | delete — use `htsjdk.tribble.AbstractFeatureReader` |
| `AbstractEnhancedFeatureReader.java` | 173 | delete — use `htsjdk.tribble.AbstractFeatureReader.getFeatureReader(...)` |
| `TribbleIndexedFeatureReader.java` | 643 | delete — stock equivalent |
| `TabixFeatureReader.java` | 253 | delete — stock equivalent |
| `TabixReader.java` | 698 | delete — stock `htsjdk.tribble.readers.TabixReader` |
| `TabixIteratorLineReader.java` | 32 | delete — stock equivalent |
| `IndexCache.java` | 9 | delete (marker interface for the cache) |
| `EhCacheBasedIndexCache.java` (Caffeine-backed after Phase 3) | 98 | delete |

**Keep** (public htsjdk SPIs, not forks):

- `EnhancedUrlHelper.java` — implements `htsjdk.tribble.util.URLHelper`, registered via
  `ParsingUtils.registerHelperClass`. It exists to make **S3 pre-signed URLs** work: signed
  URLs return HTTP 403 to a `HEAD` request, and its `S3Helper.exists()` treats 403 as
  "exists". That behaviour must be preserved — it is not a fork, it is a legitimate
  extension. **VERIFY** that `URLHelper`/`HTTPHelper`/`FTPHelper` and
  `registerHelperClass` still exist in htsjdk 4.x; they may be deprecated in favour of NIO
  filesystem providers. If they are gone, reimplement the same 403-tolerance over the
  replacement mechanism.
- `util/NgbSeekableStreamFactory.java` — implements `ISeekableStreamFactory`, installed via
  `SeekableStreamFactory.setInstance`, and routes to S3/Azure/local. Also a supported SPI.
  Note the class body has `SeekableStreamFactory.setInstance(INSTANCE)` in a static
  initialiser **and** is registered as a Spring `@Bean` in `Application.java` — check that
  ordering still works.

**Tasks**

1. Bump htsjdk to the latest release (4.x; requires Java 17+ — fine). Note the 2.2.4 → 4.x
   gap is ~8 years: `SamReaderFactory`, `VCFCodec`/`VCFFileReader`, `Tribble`/`Tabix` index
   APIs, `IndexFactory`, `BlockCompressedInputStream`, `AbstractIterator` and
   `htsjdk.samtools.util.*` have all moved. 117 files touch htsjdk.
2. **Repoint the 41 files that import `com.epam.catgenome.util.feature.reader.*`** to the
   stock htsjdk types, and drop the `EhCacheBasedIndexCache` parameter from every
   `getFeatureReader(...)` call site. The consumers are:
   `manager/bam/BamHelper`, `manager/bed/BedManager`, `manager/FeatureIndexManager`,
   `manager/FileManager`, `manager/gene/GffManager`, `manager/gene/reader/AbstractGeneReader`,
   `manager/genepred/GenePredManager`, `manager/maf/MafManager`, `manager/seg/SegManager`,
   `manager/vcf/{VcfManager, reader/AbstractVcfReader, reader/VcfFileReader, reader/VcfReader}`,
   `manager/wig/{AbstractWigProcessor, BedGraphProcessor, FacadeWigManager, WigProcessor, reader/BedGraphReader}`,
   `util/{CachedFeatureReader, FeatureIterator, IndexUtils, Utils}`, plus the tests
   `GffManagerUnitTest`, `SortTest`, `VcfManagerTest`, `DiskBasedListTest`,
   `EhCacheDisabledIndexCacheTest`, `EhCacheTest`, `IndexHeaderCacheTest`,
   `TabixReaderTest`, `TestAbstractFeatureReader`.
3. **Remove the index cache** (D10): delete the tests that exist only to test it
   (`EhCacheTest`, `EhCacheDisabledIndexCacheTest`, `IndexHeaderCacheTest`), the
   `indexCache` Caffeine region added in Phase 3, the `server.index.cache.enabled` property
   from every `profiles/*/catgenome.properties` and test properties file, and
   `src/test/resources/test-catgenome-cache-disable.properties` if it becomes unused. The
   `proteinTrack` cache stays.
4. **Record the performance risk.** Remote-file (S3 / Azure / HTTP / FTP) track loading may
   get slower, because index files are no longer cached in memory across requests. Note it
   in the release notes and in `.devenv/README.md`. If you can measure it cheaply — load a
   VCF track from an S3 URL before and after — do, and record the numbers so a future
   decision to reintroduce caching has a basis (D10).
5. Check the other htsjdk-adjacent utilities for fork-like patches:
   `util/BlockCompressedDataInputStream`, `util/BlockCompressedDataOutputStream`,
   `util/FeatureSeekableStream`, `util/FeatureInputStream`, `util/PositionalOutputStream`,
   `util/sort/**`, `util/motif/**`. Anything that duplicates htsjdk internals should also go
   to stock where possible — the spirit of D9.
6. `com.github.igvteam:igv:v2.6.3` bundles its own htsjdk fork in some builds; it is used for
   exactly three classes (`BasicFeature`, `Exon`, `UCSCGeneTableCodec`) in the genepred path.
   **Check for a duplicate/conflicting htsjdk on the classpath** (`./gradlew dependencies`)
   and exclude it, as is already done for `org.jetbrains.bio:big`. If IGV brings an
   irreconcilable htsjdk, consider reimplementing `UCSCGeneTableCodec` locally and dropping
   the IGV dependency entirely.
7. `snappy-java:1.0.3-rc3` (2011) is pulled explicitly and has **no arm64 native** — the
   README already flags it. htsjdk 4.x manages a modern snappy; drop the explicit pin unless
   something needs it.

**Exit criteria**

- No file under `server/` imports `com.epam.catgenome.util.feature.reader.{AbstractFeatureReader,
  AbstractEnhancedFeatureReader, TabixFeatureReader, TribbleIndexedFeatureReader, TabixReader,
  TabixIteratorLineReader, IndexCache, EhCacheBasedIndexCache}` — the package should contain
  only `EnhancedUrlHelper`.
- `make test`, `make test-pg` at or better than the Phase 6 baseline. **Expect
  `GffManagerTest.testLoadGenesTranscript` to move here** — the `protein_coding` vs
  `protein_coding_CDS_not_defined` expectation is exactly the kind of thing a parser
  upgrade changes. Decide which side is right and fix it, do not just re-baseline.
- End-to-end, in the UI: BAM alignments (including a `.cram` if available), VCF, BED,
  BedGraph, WIG, GFF/GTF genes, GenePred, SEG, MAF, and a Tabix-indexed file. This phase can
  break file parsing in ways unit tests miss.
- A remote (S3 or HTTP URL) track still loads.

---

### Phase 8 — Remaining libraries and API polish

**Goal:** clear the rest of the dependency backlog. These are largely independent of each
other; do them as separate commits and verify each.

| Item | Current | Target | Notes |
|---|---|---|---|
| AWS SDK | v1 1.11.704 (EOL) | v2 | 6 files: `util/aws/{S3Client, S3SeekableStreamFactory, S3ObjectChunkInputStream}`, `manager/aws/S3Manager`, `util/azure/AzureBlobInputStream` (imports `com.amazonaws.util.IOUtils` — swap for commons-io), `manager/aws/S3ManagerTest`. v2 is a full API change (`S3Client`, `S3Presigner`, `GetObjectRequest` builders). Note `manager/aws/S3Manager` is instantiated as an XML bean via `factory-method="singleton"`. |
| POI | 3.16 | 5.x | Excel export (`manager/export`, `manager/target/export`). `CellType`/`HorizontalAlignment`/`FillPatternType` enums replaced int constants; `HSSF`/`XSSF` workbook APIs changed. |
| Swagger annotations | `com.wordnik.swagger.annotations.*` in 42 files | `io.swagger.v3.oas.annotations.*` | Optional polish — springdoc already generates docs without them (Phase 3). `@Api`→`@Tag`, `@ApiOperation`→`@Operation`, `@ApiResponse`→`@ApiResponse` (v3), `@ApiModelProperty`→`@Schema`. Mechanical; scriptable. |
| biojava | 4.2.0 | 6.x/7.x | `biojava-genome`, `biojava-structure`. Used by `manager/protein`, `manager/pdb`, `ConsensusSequenceUtils`, `ProteinSequenceUtils`. Check API churn before committing to a version. |
| Azure OpenAI | `azure-ai-openai:1.0.0-beta.2` | GA release | `manager/llm/{OpenAIClient, OpenAIChatGPT35, OpenAIChatGPT40, CustomOpenAILLMClient}`. Beta → GA changed package names. |
| Azure Blob | `azure-storage-blob:12.14.0`, `azure-identity:1.4.4` | current | `util/azure/**`, `Application.azureBlobClient`. Should be a straight bump. |
| Jackson | Boot-managed | Boot-managed | Verify `controller/JsonMapper` and `jackson-dataformat-xml` still behave; NGB registers a custom `MappingJackson2HttpMessageConverter`. |
| Reactor | `reactor-core:3.5.3` pinned explicitly | drop the pin | It is a transitive of azure-*; an explicit pin invites conflicts. |
| Lucene-adjacent | `org.jetbrains.bio:big:0.9.1` | current | BigWig/BigBed reader (Kotlin). Already excludes htsjdk — re-check after Phase 7. |
| Misc | `commons-lang3:3.0`, `commons-io:2.4`, `commons-collections4:4.1`, `commons-validator:1.5.0`, `commons-math3:3.6.1`, `opencsv:5.8`, `gson:2.10.1`, `java-jwt`, `retrofit2 converter-jackson:2.7.2`, `aspectjweaver:1.8.8` | current | `commons-lang3:3.0` (2011) and `commons-io:2.4` are the oldest. `aspectjweaver` matters for `<aop:config proxy-target-class="true"/>` — bump it. Also note `org.apache.commons.collections` (v3) is used in `JWTSecurityConfiguration`; migrate to collections4. |
| `libnewicktree`, `libsbgn`, `chilay-sbgn`, `paxtools` | as-is | as-is | Per D6 these stay. Only bump if something breaks; `paxtools` may still need the insecure repo (Phase 2 item 10). |
| `fast-classpath-scanner:2.0.9` (ngb-cli) | | `io.github.classgraph:classgraph` | If it survived Phase 2, still worth replacing — it is the abandoned predecessor. |

**Exit criteria**

- `./gradlew dependencies` shows no EOL AWS v1, no `com.wordnik`, no `com.mangofactory`,
  no `net.sf.ehcache`, no `c3p0`, no hadoop, no `spring-security-oauth2`,
  no `spring-security-saml2-core`, no duplicate htsjdk.
- `make test`, `make test-pg`, `make lint`, `make cli-test` all green against the Phase 7
  baseline.
- Excel export, protein/PDB views, LLM target summaries, S3 and Azure track loading all
  verified by hand.

---

### Phase 9 — Packaging, CI, docs, release

**Goal:** shippable.

**Tasks**

1. **Docker.** `docker/core/Dockerfile` is `ubuntu:16.04` + `openjdk-8-jre` + nginx.
   Rewrite on a JDK 21 base (`eclipse-temurin:21-jre-jammy` or a distroless equivalent).
   `docker/demo/Dockerfile` and `docker/start-demo.sh` too. Check whether nginx is actually
   used (it is installed and never obviously configured) — if not, drop it.
2. **JRE-bundled distributions (D7).** `bundleWindows`, `bundleLinux`, `downloadJreWin`,
   `downloadJreLinux`, `customStartScriptsWin`, `customStartScriptsLinux` download JRE 8
   from `http://download.oracle.com/otn-pub/java/jdk/8-b132/` with an `oraclelicense`
   cookie — dead URLs and, incidentally, a licence footgun. Rewrite against the
   **Adoptium/Temurin API** for a JDK 21 runtime, or better, use **`jlink`** via Gradle's
   toolchain support to produce a trimmed runtime image (smaller, and no download step).
   Update the `jre1.8.0` path substitutions in the start-script `doLast` blocks, and the
   `-Xms512m -Xmx2g` defaults if appropriate.
3. **CI: AppVeyor → GitHub Actions (D7).** `.appveyor.yml` pins `Previous Ubuntu1604` and
   `jdk 8, python 3.9, node 14`. Write `.github/workflows/build.yml`:
   - JDK 21 (`actions/setup-java`, Temurin), Node 14 (per D12 — pin it explicitly and note
     why), Python for mkdocs.
   - Reproduce `build.sh`: `buildJar` (H2), `buildCli`, `buildDoc`, then `buildJar
     -Pdatabase=postgres`, producing `catgenome-h2.jar`, `catgenome-psql.jar`,
     `catgenome.jar` (the H2 copy, kept for backward compatibility), `ngb-cli.tar.gz`,
     `ngb-docs.tar.gz`.
   - Run the tests (both flavours — a `postgres:16` service container makes `test-pg`
     possible in CI, which it never was on AppVeyor).
   - `jacocoTestReport` + codecov, if still wanted.
   - Port `publish.sh` (S3 upload + Docker Hub push on `release/*`) to Actions with
     OIDC/secrets. Note `codecov`'s `bash <(curl ...)` uploader is deprecated — use the
     action.
   - Delete `.appveyor.yml`.
4. **Docs.** `docs/mkdocs.yml` + `docs/md/`:
   - Remove `installation/desktop.md` and `installation/binaries.md` (Phase 1) from nav.
   - **Add a release-notes / upgrade section** covering, at minimum: the mandatory Lucene
     reindex (Phase 6), the H2 export/import (Phase 5), PostgreSQL 16, the removal of
     HDFS / GA4GH / the desktop app / the WAR, the SAML endpoint paths if they changed
     (Phase 4), and the index-cache removal (Phase 7).
   - Update `installation/overview.md`, `installation/standalone.md`,
     `installation/docker.md`.
   - Update the administrator guide if the `person`-package removal changed any REST paths.
   - Bump `site_name: New Genome Browser 2.8.0` and the `version` in the root
     `build.gradle`'s `Version` object and `client/package.json`. Decide the version number
     — a change this breaking argues for **3.0.0**.
   - `mkdocs` version in the toolbox is 1.5.3; bump if the build warns.
5. **`.devenv` final pass.** Once the migration is done the environment should describe the
   *new* reality, not the migration:
   - Toolbox: drop JDK 8 (and 17, if nothing needs it); JDK 21 becomes the default.
   - `NGB_JAVA_VERSION` defaults to 21; drop `with-java8` or keep it only if something
     still needs it.
   - `PG_VERSION=16`.
   - Delete `make probe-java21`.
   - Rewrite the "Migration checkpoints" and "Two source fixes were needed to boot at all"
     sections of `.devenv/README.md` — both are historical by then.
   - Move SAML back to 8443 if Phase 4 removed the port-rewrite hack, and update the
     "Two things about SAML mode that look arbitrary but aren't" section, which will no
     longer be true.
   - Final `TEST-BASELINE.md`: the goal is **zero** unexplained failures. Anything left must
     be a deliberate, documented exclusion.
6. **`README.md`** (root) — build instructions, prerequisites (JDK 21, Node 14, Python).

**Exit criteria**

- `bash build.sh` equivalent runs green in CI on JDK 21 and produces every artifact.
- `docker build` produces a working image; `docker run` serves the app.
- The bundled Windows and Linux distributions unpack and start without a system JDK.
- `mkdocs build` succeeds; the upgrade notes are present and accurate.
- `make test`, `make test-pg`, `make lint`, `make cli-test`, `make smoke`, `make smoke-saml`
  all green.

---

## 4. Cross-cutting risk register

| Risk | Where | Mitigation |
|---|---|---|
| **Data loss on database upgrade** | Phase 5 | Never test only fresh installs. Test H2 1.3 → 2.x export/import and PostgreSQL 9.6 → 16 with a database created by pre-migration code. Document the operator procedure before declaring the phase done. |
| **Silent index corruption / unreadable indexes** | Phase 6 | The startup guard (Phase 6 task 2) is not optional. A stack trace on a customer's index is a support incident; a clear message is a documented upgrade step. |
| **Genomic file parsing regressions** | Phase 7 | 117 files, an 8-year htsjdk gap, and unit tests that use small fixtures. Hand-verify every track type. `GffManagerTest`'s existing failure is a warning that fixture expectations and parser behaviour have already drifted once. |
| **SAML URL exactness** | Phase 4 | Spring Security 6's default SAML2 endpoint paths differ from the OpenSAML 2 extension's. Configure the legacy paths if at all possible; if not, it is a breaking change requiring every IdP registration to be updated. Use `make smoke-saml`, not a browser, to iterate. |
| **Enum ordinal drift** | Phase 1 | Removing `HDFS(5)` and `GA4GH(6)` leaves gaps that must not be closed. Existing rows with those type ids become unmappable — detect and report, do not NPE. |
| **Flyway version-string parsing** | Phase 5 | `v2016.11.21_17.58__Name.sql` is an unusual version format. If Flyway 10 parses it differently from 3.2.1, applied-migration history breaks. Verify against a real migrated database early — this could invalidate the Phase 5 approach. |
| **Insecure/dead Maven repository** | Phase 2 | `http://www.biopax.org/m2repo/releases/` is plain HTTP (Gradle 7+ blocks it) and may be gone. Resolve the three BioPAX artifacts from Central if possible; vendoring is the fallback. |
| **OpenSAML artifact availability** | Phase 4 | Historically Shibboleth-hosted rather than on Maven Central. Verify before committing to the approach. |
| **Frozen client on a modern build** | Phase 2 | D12 keeps Node 14, which means the toolbox image keeps an EOL Node and the plugin has to go. Plain `Exec` tasks are the simplest way to honour the freeze. A host build now needs Node 14 on PATH — document it. |
| **Baseline drift** | all | The migration's only safety net is the test suite. Update `TEST-BASELINE.md` at every phase boundary. A phase that ends with "a few more failures, probably fine" has not ended. |
| **Phase 2 and 3 are large** | 2, 3 | Both touch the whole tree. Neither has a compiling midpoint that is worth committing. Expect these to dominate the schedule. |

---

## 5. Explicitly out of scope

Recording these so they are not rediscovered as gaps:

- **The AngularJS client** (D12). Still AngularJS 1.5.11 with an aged npm tree afterwards.
  That is a separate project.
- **JUnit 5 conversion** of the 139 test classes (D13). Vintage engine only.
- **Converging the two Flyway script sets.** They have already diverged (different
  predefined role IDs); the migration keeps them diverged unless the Phase 5 H2 baseline
  decision happens to converge them.
- **Runtime database-flavour selection.** Still a build-time switch producing two jars (D3).
- **Reintroducing an index cache** (D10). Dropped; revisit only with a measurement.
- **Spring Boot 4.x** (D1). A follow-up if wanted.
- **Any new features.** This is a migration. If a phase tempts you into a redesign, note it
  and move on.

---

## 6. Quick reference: verification commands

All from `.devenv/`. See [`README.md`](README.md) for the full list.

```bash
make image            # rebuild the toolbox (after Dockerfile edits)
make jar              # full H2 jar: UI + docs + server, no tests
make jar-fast         # server only (~45 s vs ~7 min) - the inner loop
make jar-pg           # PostgreSQL jar
make test             # server unit tests on H2
make test-one T=Xxx   # single test class
make test-pg          # server unit tests on PostgreSQL
make lint             # checkstyle + pmd
make cli-test         # CLI <-> server integration suite

make up               # run on H2
NGB_JAVA_VERSION=21 make up
make up-pg            # run on PostgreSQL
make up-saml          # Keycloak + NGB over HTTPS
make smoke            # is it answering?
make smoke-saml       # browser-less end-to-end SSO login
make logs

make reset-ngb-data   # wipe H2 db + contents/index volumes
make reset-pg         # wipe the PostgreSQL volume
make reset            # wipe everything including caches
```
