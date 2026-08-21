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
| Static analysis | Checkstyle 10.x/11.x, PMD 7.x (**ruleset needs a full rewrite**) | certain — but PMD 7 needs Gradle 8.6+, so Phase 2 lands on 6.55.0 with the rewritten ruleset and **Phase 3 bumps it to 7.x**; see the Phase 2 findings |
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
     **Answered by Phase 0 itself — see note 3 below: it is neither, it is live Ensembl data.
     Phase 7 confirmed it did not move.**
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

**Phase 2 execution findings** *(written as the phase was carried out)*

- **VERIFY resolved — `spring-security-saml2-core` 1.0.10 IS binary-compatible with Spring
  Security 5.7.11** (the version Boot 2.7.18 manages). This was the load-bearing assumption of
  the whole waypoint, so it was checked before any code was touched, and not by reading release
  notes: every `CONSTANT_Class` / `Methodref` / `Fieldref` / `InterfaceMethodref` entry in all
  **111** classes of the extension jar was extracted from the constant pool and resolved against
  Spring Security 5.7.11 (`core`, `web`, `config`, `crypto`, `acl`) plus Spring Framework 5.3.31,
  walking superclasses and interfaces. Result: **0 missing classes, 0 missing or changed
  members.** The one apparent miss,
  `org.springframework.security.providers.ExpiringUsernameAuthenticationToken`, is shipped
  *inside the extension jar itself* — the extension squats on the `org.springframework.security`
  package. Note the POM *declares* `spring-security-{core,web,config}:4.2.13.RELEASE`; that is a
  declaration, not a constraint, and the Boot BOM overrides it to 5.7.11 transitively. The same
  check run against the **1.0.2** jar currently in the build is also clean, so both versions are
  viable — useful to know, because it makes 1.0.10 a revertible choice.
  Binary compatibility is not behaviour: `make up-saml` + `make smoke-saml` remains the real
  acceptance test for this phase.
- **`org.opensaml:opensaml:2.6.6` is NOT on Maven Central.** Central carries 2.5.3, 2.6.0,
  2.6.1 and 2.6.4 only, and 1.0.10 requires 2.6.6 exactly. It *is* served by
  `https://build.shibboleth.net/maven/releases/` (HTTPS, 200 for both POM and jar), which is
  the canonical OpenSAML host and which **Phase 4 task 1 already anticipates needing** for
  OpenSAML 4. So bumping the extension 1.0.2 → 1.0.10 costs one new repository. The fallback,
  if that repository is ever unavailable, is to stay on **1.0.2.RELEASE** (opensaml 2.6.1, on
  Central) — verified compatible with Spring Security 5.7.11 by the same check.
- **The insecure BioPAX repository stays, re-declared as HTTPS.** *(Corrects an earlier finding
  in this list which said the repository was dead and could be deleted — that was wrong, and the
  build proved it: dropping the repository made `org.sbgn:libsbgn:0.2` unresolvable.)* The three
  BioPAX/PathwayCommons artifacts the plan names do resolve from Maven Central
  (`org.biopax.paxtools:paxtools-core:5.1.0`, `:sbgn-converter:5.0.0`,
  `pathwaycommons:chilay-sbgn:3.0.0`), but the module also declares **`org.sbgn:libsbgn:0.2`**
  directly, and `sbgn-converter` needs it transitively — and that artifact is on **no** other
  repository (not Central, not Sonatype, not EBI). `https://www.biopax.org/m2repo/releases/`
  serves it fine; only the `http://` scheme was the Gradle 7 problem. So the fix is one character
  plus a `content { includeGroup "org.sbgn" }` block to keep the repository from being consulted
  for anything else. No `allowInsecureProtocol`, no vendoring. The risk-register row stays open:
  a single-source dependency on a research-group web server.
- **Boot 2.7.18's BOM would silently upgrade H2 and Flyway if their pins were dropped.** It
  manages `h2` → **2.1.214** and `flyway-core` → **8.5.13**, both of which belong to Phase 5,
  not here. The pins on `com.h2database:h2:1.3.176` and `org.flywaydb:flyway-core:3.2.1` are
  therefore **load-bearing and must stay** through Phase 2 — an explicitly declared version
  beats a BOM-managed one, so keeping them written out is sufficient. Same reasoning applies to
  the `org.postgresql:postgresql:9.4-1206-jdbc4` pin (BOM would say 42.3.8). Task 5's "delete
  the pins the BOM now manages" is about `versionSpring`, `versionSpringSecurity`,
  `versionJackson`, `versionSlf4j`, `versionJUnit`, `versionMockito` and the two `ext[...]`
  overrides — **not** about the persistence layer.
- **The resource-copy restructure is safe: there are no path collisions.** `copyConfiguration`
  wrote into `$buildDir/resources/main` behind `processResources`'s back, which is what the plan
  warned would trip Gradle 7's overlap checks. Checked before rewriting it, and every file it
  produces lands where `src/main/resources` has nothing: `catgenome.properties` and `log4j.xml`
  (from `profiles/<profile>/`), `version.properties`, `conf/catgenome/applicationContext-flyway.xml`
  and `conf/catgenome/dao/dao-helper.xml` (from `profiles/<database>/`). So the copies could be
  folded into `processResources`/`processTestResources` as ordinary `from` specs with no
  duplicate-strategy decision to make and no change in which file wins. Also worth recording:
  the old `include "*/"` pattern was **not** a directories-only filter — Gradle expands a
  trailing `/` to `/**` and `**` matches zero segments, so it matched top-level files too. It
  was a no-op and is gone.
- **`net.saliman.properties` was only ever providing `project.filterTokens`.** Confirmed by
  grep: `filterTokens` is the sole symbol either module uses from it, and `gradle.properties`
  (its other reason to exist) was deleted in Phase 1. Replaced with a plain
  `ext.filterTokens` map in both modules, as the plan recommended.
- **VERIFY resolved — `fast-classpath-scanner` 2.0.9 works on JDK 17 and does not need
  bumping.** The plan flagged it because it predates JDK 9 and the CLI's whole command dispatch
  depends on it: `CommandManager` scans for `@Command` classes and throws
  "command handler not found" if the scan comes back empty. Tested for real rather than
  reasoned about — built `installDist` on the 17 toolchain and ran `bin/ngb version`, which is
  served by `VersionCommandHandler` (`@Command(command = {"version"})`) and therefore only
  answers if the scan found it. It printed `2.8.0`. 2.20.2 (last 2.x) was tried first and also
  works, so the bump is available if a later phase needs it, but it is not required here and
  library bumps belong to Phase 8. Left at 2.0.9.
- **`checkstyle { }` needs an explicit `configDirectory` in both modules.** Gradle 4 changed the
  default from `<project>/config/checkstyle` to `<rootProject>/config/checkstyle`, which does not
  exist here, so every `checkstyleMain` failed with
  `Unable to create a Checker: configLocation {/workspace/config/checkstyle/checkstyle.xml}`.
  Both modules keep their own (byte-identical) copy, so both now set
  `configDirectory = file("config/checkstyle")`. PMD was unaffected — it already names its
  rulesets with a project-relative `files(...)`.
- **The `regression` task in `server/ngb-cli/build.gradle` was dead and is deleted, not fixed.**
  The plan's task 12 asks for its `'script\\regression_test.sh'` argument to be corrected to a
  forward slash. But `server/ngb-cli/script/` does not exist, and `git log` shows
  `regression_test.sh` was removed back in the 2.5 release; no `make` target, script or other
  Gradle task invokes `regression`. Correcting the separator would have produced a task that
  still cannot run.
- **Sub-commit (a) genuinely does not build, and the reason is exactly the one predicted.** The
  Boot 1.5.2 plugin fails at *plugin application* under Gradle 7.6 with
  `Failed to apply plugin 'org.springframework.boot' > Configuration with name 'runtime' not
  found` — it reaches for the `runtime` configuration that Gradle 7 removed. Because that
  happens while configuring `server:catgenome`, and Gradle configures every project in
  `settings.gradle` before running anything, *no* task in the build can run at (a) — not even
  ones in unrelated modules. To keep (a) from being committed entirely unverified, it was
  checked with `server:catgenome` temporarily dropped from `settings.gradle` (restored
  afterwards), which confirmed: root/`client`/`ngb-cli`/`target-identification` all configure
  clean; `:server:ngb-cli:build` compiles main + test on the 17 toolchain with Lombok 1.18.46
  as an `annotationProcessor`, and produces the fixed-name `ngb-cli.tar.gz` / `ngb-cli.zip` from
  `archiveFileName` instead of the old blank-the-version-and-rename hack; `:server:ngb-cli:test`
  passes; and `:client:buildUI` and
  `:export-templates:target-identification:buildTemplate` both succeed as plain `Exec` tasks
  against the toolbox's Node 14.17.5. `--configure-on-demand` is what makes per-module runs
  possible; it does not help root-project tasks such as `buildDocs`, which still need every
  project configured.
- **Lombok 1.16.16 → 1.18.46, and the version stays pinned in `ext`.** 1.16.16 cannot run on
  any JDK past 8 (it reaches into `com.sun.tools.javac` internals that JEP 396 closed), so this
  is not a routine bump but a precondition for the 17 toolchain. Lombok is compile-time only, so
  the version is the build's to choose rather than the Boot BOM's, and pinning it keeps Phase 3's
  JDK 21 move independent of whatever Boot 3.5 happens to manage.
- **Boot 2.6's default path matcher breaks `swagger-springmvc` outright.** The app started up
  under Boot 2.7 and then died in `finishRefresh` with a bare `NullPointerException` in
  `RegexRequestMappingPatternMatcher.patternConditionsMatchOneOfIncluded`. Boot 2.6 switched Spring
  MVC's default from `AntPathMatcher` to `PathPatternParser`, which leaves
  `RequestMappingInfo.getPatternsCondition()` **null**; `com.mangofactory:swagger-springmvc` 1.0.2
  dereferences it while scanning the request mappings. Fixed by pinning the old matcher in
  `application.properties`:
  ```
  spring.mvc.pathmatch.matching-strategy=ant-path-matcher
  ```
  `AppMVCConfiguration.setUseSuffixPatternMatch(false)` is an `AntPathMatcher`-only setting too, so
  this keeps both consistent. **The property goes away in Phase 3 with the Swagger → springdoc
  move**, which is also when `setUseSuffixPatternMatch` disappears — do not forget it, or the whole
  API will silently start matching paths by different rules.
- **Spring 5.2 dropped `charset=UTF-8` from JSON responses, which is 40 test assertions.**
  `MediaType.APPLICATION_JSON_UTF8` is deprecated in 5.2 and the Jackson converter no longer lists
  the charset parameter among its supported media types (RFC 8259 fixes JSON to UTF-8, so it was
  redundant). Responses are still UTF-8; only the header changed. 39 controller tests assert on
  `AbstractControllerTest.EXPECTED_CONTENT_TYPE`, so they were fixed by changing that one constant
  to `MediaType.APPLICATION_JSON_VALUE`. The 40th, `BamControllerTest.bamTrackGetTest`, failed the
  *other* way round — `BamController.loadTrackStream` sets the header by hand on its
  `ResponseBodyEmitter`, so that one is a main-source change from `APPLICATION_JSON_UTF8` to
  `APPLICATION_JSON` (it has to happen anyway: the constant is gone in Spring 6).
- **Boot 2's `MockitoTestExecutionListener` runs before dependency injection, which breaks
  `VcfManagerTest`.** All 13 of its tests failed in `@Before` because Boot 2.x contributes a
  listener at order **1950** that calls `MockitoAnnotations.openMocks()` — i.e. *before*
  `DependencyInjectionTestExecutionListener` (2000) has populated the fields. The class has ten
  `@Spy @Autowired` fields, so Mockito tried to instantiate the spied managers from scratch, and
  they have no no-arg constructor. Boot 1.5's listener only handled `@MockBean`, hence no problem
  until now. Fixed by declaring `@TestExecutionListeners` explicitly with spring-test's own default
  eight, which leaves out the two spring-boot-test adds; declared listeners *replace* the defaults
  and are not re-sorted. The alternatives were both worse: `@SpyBean` and plain
  `@Autowired VcfManager` would each mutate a bean in a context that nine other test classes share.
  The spies here are pure wiring — the class contains no `Mockito.when`/`verify` at all.
- **`MockitoAnnotations.initMocks(x)` is `openMocks(x).close()` in Mockito 4 — it releases the
  mocks it just created.** With the listener fixed, `VcfManagerTest` still failed, now with
  `MockitoException: Failed to release mocks`, caused by
  `NotAMockException: Argument should be a mock, but is: class BiologicalDataItemManager`. Mockito 4
  implements the deprecated `initMocks` as `openMocks(testClass).close()`, and that close breaks on
  `@Spy` fields holding Spring beans. The plan's exit criteria already calls for
  `initMocks` → `openMocks`; done at all **12** call sites, not just the failing one, since the
  immediate-release semantics is wrong everywhere and the method is deprecated in Mockito 4.
- **The `org.mockito.internal.matchers` uses are Hamcrest matchers, not argument matchers — the
  exit criteria's advice to port them to `org.mockito.ArgumentMatchers` is wrong for this site.**
  `ToolsControllerTest` passes `new Equals(...)` / `new Find(...)` to
  `jsonPath(...).value(...)`. That worked because Mockito 1's matchers implemented
  `org.hamcrest.Matcher`; from Mockito 2 they implement `org.mockito.ArgumentMatcher`, which
  `jsonPath()` knows nothing about — so the call bound to the `value(Object)` overload instead and
  compared the payload against the matcher's `toString()` (hence `expected:<"/tmp/...">` *with*
  quotes). Ported to `org.hamcrest.Matchers.is(...)` and `Matchers.matchesPattern(...)`, which is
  what the assertion always meant. Nothing here needs `ArgumentMatchers`.
- **The Lombok bump silently removed every implicit Jackson creator in the code base.** This is the
  subtlest thing in the phase and it does not fail at build time. Six
  `NGBSessionSharingSecurityTest` tests failed with `AccessDeniedException`, and the cause was
  neither Spring Security nor the ACL layer:
  ```
  InvalidDefinitionException: Cannot construct instance of `...entity.session.NGBSessionValue`
      (no Creators, like default constructor, exist)
  ```
  Lombok emitted `@java.beans.ConstructorProperties` on generated constructors up to **1.16.16**
  and stopped doing so by default in 1.16.20. Jackson uses that annotation as an implicit
  properties-based creator, so `@Value @Builder` classes with no no-arg constructor deserialised
  fine on Lombok 1.16.16 and stop dead on 1.18.46. About **90** classes carry `@Builder` without a
  no-arg constructor and roughly a third of them are deserialised from JSON — external-database
  responses (OpenTargets, PharmGKB, DGIdb, TTD, HomoloGene, NCBI), controller request bodies,
  `NGBSessionValue`. Only the session one had a test to catch it, and even there the failure was
  disguised: `PermissionHelper.sessionIsReadable` catches the `IOException` and returns `false`, so
  a parse error presents as "permission denied". Fixed globally with a new **`server/lombok.config`**
  carrying `lombok.anyConstructor.addConstructorProperties = true`, which restores the 1.16.16
  behaviour for both modules. Annotating the affected classes with `@Jacksonized` instead was
  rejected: the affected set cannot be enumerated reliably from the source, and everything missed
  would be a silent runtime failure. Note for Phase 3: `lombok.config` is **not** an input Gradle
  tracks for incremental compilation — after editing it, delete `build/classes` or the change will
  appear not to work.
- **EhCache 2.10.1 cannot size its caches on JDK 16+, and this is a production bug, not a test
  bug.** 11 tests (`EhCacheTest`, `IndexHeaderCacheTest`, `TestAbstractFeatureReader`) failed with
  `InaccessibleObjectException`. `conf/catgenome/ehcache.xml` configures `indexCache` with
  `maxBytesLocalHeap="100M"`, which makes EhCache size every entry by walking its object graph
  reflectively — JEP 396 forbids that for `java.base` fields. It needs one `--add-opens` per
  package, and the packages follow from what is actually cached: `java.util`,
  `java.util.concurrent` (EhCache's own store is a `ConcurrentHashMap`),
  `java.util.concurrent.atomic` and `java.io` (the htsjdk index objects hold a `File`). Note that
  `--add-opens java.base/java.util` does **not** cover `java.util.concurrent`, which in turn does
  not cover `java.util.concurrent.atomic`. The flags now live in two places: `test { jvmArgs }` in
  `server/catgenome/build.gradle`, and `JAVA_REQUIRED_OPTS` in `.devenv/ngb/entrypoint.sh` —
  deliberately *not* in `.env`'s `JAVA_EXTRA_OPTS`, because they are not optional and because
  `--add-opens` makes JDK 8 refuse to start, so they have to be conditional on the JDK. Both go
  away in Phase 3 step 6 (D11, EhCache → Caffeine). The `INFO [AgentLoader] Failed to attach to VM
  and load the agent` line in the logs is EhCache failing to load its sizing agent and falling back
  to reflection; it is a symptom of the same thing.
- **`EhCacheTest` needed a fix of its own, and the `--add-opens` list is not what made it pass.**
  Its `TestIndexCache` was a non-static inner class, so every cached instance held a `this$0`
  reference to the test, and through the test's `@Autowired ApplicationContext` to the entire Spring
  container — which the sizing walker then traversed on every `put`, eventually reaching
  `jdk.internal.loader.BuiltinClassLoader` (a package it would be wrong to open). Made `static`. On
  JDK 8 the same bug merely mis-sized every entry by megabytes, silently.
- **Boot 2's `SpringBootMockResolver` breaks `@Spy` over an AOP-proxied bean, which is what all 13
  `VcfManagerTest` failures were.** The symptom is Mockito rejecting its own spy:
  `NotAMockException: Argument should be a mock, but is: class ...BiologicalDataItemManager`, thrown
  from `openMocks`. `spring-boot-test` registers `SpringBootMockResolver` under
  `mockito-extensions/org.mockito.plugins.MockResolver`, and Mockito 2+ passes every object through
  the registered resolvers (`MockUtil.resolve`) before looking up its handler. That resolver unwraps
  anything implementing `Advised` to its ultimate target — and a spy created over a CGLIB proxy is a
  *subclass* of the proxy, hence itself `Advised`, so Mockito resolves the spy back to the raw bean,
  finds no handler and fails the class. It only bites the `@Spy @Autowired` fields whose beans are
  proxied (`@Transactional`: `vcfFileManager`, `biologicalDataItemManager`,
  `referenceGenomeManager`); the ten other `@Spy` users in the suite spy unproxied objects and are
  unaffected. Fixed by unwrapping with `AopTestUtils.getUltimateTargetObject` *before* `openMocks`,
  so the spies wrap plain beans. The spies are load-bearing — they are the injection candidates for
  `@InjectMocks VcfManager` — so they cannot simply be dropped; and losing the transactional advice
  on them changes nothing observable, because every spied method is `Propagation.REQUIRED` or
  `SUPPORTS` and the test methods already run inside a transaction. Two Mockito diagnostics worth
  remembering: Mockito strips its own frames from stack traces (a test-source
  `org/mockito/configuration/MockitoConfiguration` overriding `cleansStackTrace()` to `false`
  restores them, and without it this trace points at the `openMocks` line and nothing else), and
  `NotAMockException`'s message prints the class of the *resolved* object, not of the argument.
- **VERIFY resolved behaviourally — SAML SSO works on Boot 2.7.18 / Spring Security 5.7.11, and
  `make smoke-saml` prints `SAML SSO OK` on JDK 17.** Getting there took three fixes, all of them
  Boot 2 defaults rather than SAML problems; the OpenSAML 2 stack itself needed no code changes at
  all. The three are recorded in the next three entries. The `security.acl.enable=true` profile is
  the only one that exercises the ACL layer, method security and SAML, so none of this shows up in
  `make up` / `make smoke` — run `make up-saml` before believing a Boot change is finished.
- **`SecurityFilterAutoConfiguration` must stop being excluded, or SAML authenticates and then
  forgets.** Symptom: the whole web SSO profile succeeds — `AuthNResponse;SUCCESS`,
  `SAMLProcessingFilter Set SecurityContextHolder to ExpiringUsernameAuthenticationToken
  [... Granted Authorities=[ROLE_USER, ROLE_ADMIN, NGB_ADMINS]]`, 302 to `/catgenome/` — and the
  very next request is anonymous again, so the browser bounces back to the IdP forever. What gave it
  away was that no `FilterChainProxy` "Securing POST /saml/SSO" line appeared at all, while
  "Securing GET /" did: `/saml/**` was never entering the security chain. Boot's
  `ServletContextInitializerBeans` prints the answer:
  ```
  Mapping filters: filterRegistrationBean order=2147483647, ..., samlEntryPoint order=2147483647,
    samlIDPDiscovery ..., samlWebSSOProcessingFilter ..., samlFilter ...,
    springSecurityFilterChain urls=[/*] order=2147483647, requestLoggingFilter ...
  ```
  `SAMLSecurityConfiguration` declares ten `Filter` beans that are only meant to be reached through
  `samlFilter` *inside* the chain, but every `Filter` bean is also auto-registered with the servlet
  container in its own right. With `SecurityFilterAutoConfiguration` excluded (as Boot 1.5 had it),
  `springSecurityFilterChain` is just another such bean at `LOWEST_PRECEDENCE`, so the tie is broken
  by bean-definition order — which happened to favour it under Boot 1.5 and does not under Boot 2.7.
  The standalone `SAMLProcessingFilter` then served `/saml/SSO` outside the chain, with no
  `SecurityContextPersistenceFilter` to store the result. Un-excluding the auto-configuration puts
  the chain back at order **-100** via `DelegatingFilterProxyRegistrationBean` (which also stops the
  raw bean being registered twice), and it contributes nothing else: its one bean is
  `@ConditionalOnBean(name = "springSecurityFilterChain")`, so `AUTH_MODE=none` is unaffected —
  verified with `make smoke` afterwards. The alternative, wrapping each of the ten filters in a
  disabled `FilterRegistrationBean`, is ten times the code for the same effect.
- **The ACL configuration has always been circular; Boot 2.6 made it fatal.**
  `security.acl.enable=true` failed at startup with
  `messageSource → metaDataSourceAdvisor → aclSecurityConfiguration (field private
  JdbcMutableAclService AclSecurityConfiguration.jdbcMutableAclService) ↔ jdbcMutableAclService
  defined in acl-dao.xml`. The `jdbcMutableAclService` bean in the `@ImportResource`d
  `conf/catgenome/acl-dao.xml` is `autowire="constructor"` over `LookupStrategy` and the EhCache-backed
  `aclCache()`, both `@Bean`s of `AclSecurityConfiguration` — so it cannot exist until that
  configuration class does, while the `@Autowired` field required it to exist first. Fixed by taking
  it as a parameter of the one `@Bean` method that uses it
  (`permissionEvaluator(JdbcMutableAclService)`), which is resolved only when that bean is built, by
  which time `lookupStrategy()` and `aclCache()` are available; `createExpressionHandler()` now asks
  the context for the `PermissionEvaluator`, the same idiom it already used for `PermissionHelper`.
  No `@Lazy`, no proxy.
- **One circular reference is not ours to fix, so `spring.main.allow-circular-references=true`
  stays until Phase 3.** `spring-security-saml2-core` 1.0.10 annotates
  `SAMLEntryPoint.setSamlDiscovery` and `SAMLDiscovery.setSamlEntryPoint` with
  `@Autowired(required=false)` — verified in the jar's constant pool, not assumed — so two of its
  own classes are wired to each other, and both directions are used at runtime (the entry point
  forwards to discovery when no IdP is determined; discovery returns to the entry point's processing
  URL). Suppressing either injection means changing SAML behaviour, which is the one thing a
  waypoint must not do. So the global flag goes in `application.properties` with a comment, *after*
  every cycle in NGB's own code was cut properly rather than masked (four `@Lazy` annotations and
  the ACL fix above). **Remove the property in Phase 3**, which deletes the OpenSAML 2 stack
  outright.
- **Spring Security 5's `StrictHttpFirewall` is the default now, and a literal `//` in a path is a
  500.** 4.2 used `DefaultHttpFirewall`, which normalised such paths. Nothing in NGB's own routes
  produces `//`, but `ngb-cli` builds its URLs as `getServerUrl() + getRequestUrl()`, so a server
  URL configured with a trailing slash now yields `RequestRejectedException` instead of working. Not
  changed here — Phase 2 does not touch the CLI's URL handling — but it is a behaviour change users
  can hit, and it belongs in the release notes.
- **Scheduling survived the loss of `@EnableScheduling`, and the proof is an error in the log.**
  The `ERROR [TaskUtils$LoggingErrorHandler] Unexpected error occurred in scheduled task /
  IllegalArgumentException: Patented protein sequences database not available` from
  `ProteinPatentsScheduledService` is environmental (the dev environment has no BLAST database), but
  it can only appear if `@Scheduled` methods are still being run — i.e. the XML
  `task:annotation-driven` really is the superset of the annotation that was removed.
- **Swagger still answers after the path-matcher pin:** `/catgenome/api-docs` → 200 with the full
  resource listing, and `/catgenome/docs` → 200. Worth checking explicitly, because the
  `swagger-springmvc` NPE it works around happens during `finishRefresh` and a half-initialised
  Swagger would otherwise be invisible until someone opened the UI.
- **`BlatSearchManagerTest` now fails for a reason outside the migration: UCSC redirects HTTP to
  HTTPS.** `testFind` and `testFindBlatReadSequence` die with
  `ExternalDbUnavailableException: Unexpected HTTP status: 302 Found` for
  `http://genome.cse.ucsc.edu/cgi-bin/hgBlat?...`. Confirmed with `curl` that the host now answers
  302 to `https://`; `blat.search.url` is plain `http://` in seven property files and the diff does
  not touch any of them, and `HttpURLConnection` has never followed cross-protocol redirects on any
  JDK. So this is external drift that also breaks BLAT search in production, recorded in
  `TEST-BASELINE.md` rather than papered over. Fixing it means changing the default URL to `https`
  (and belongs to whichever phase is allowed to change behaviour, not this one).
- **Spring Security 5 binds the ACL object identity as a *string*, and NGB's `object_id_identity` is
  `bigint` — four PostgreSQL-only failures.** `AclPermissionSecurityServiceTest.set/deletePermissionsTest`,
  `BamSecurityServiceTest.saveBamTest` and `DataItemSecurityServiceTest.deleteFileByBioItemId` all
  ended in `SQLSTATE 25P02 "current transaction is aborted"` on an unrelated `select id from
  catgenome.acl_sid ...`, i.e. a cascade. The originating error was only visible in PostgreSQL's own
  log: `ERROR: operator does not exist: bigint = character varying`. Cause, verified by disassembling
  both jars rather than assumed: `JdbcMutableAclService.retrieveObjectIdentityPrimaryKey`,
  `.createObjectIdentity` and `JdbcAclService.findChildren` pass
  `ObjectIdentity.getIdentifier().toString()` in 5.7.11 where 4.2.2 passed the raw `Serializable`
  (a `Long`). That came in with the `aclClassIdSupported` feature, whose reference schema declares
  `object_id_identity` as `varchar`; NGB's is `bigint not null` in both script sets, and NGB's own
  `LookupStrategyImpl` binds it with `ps.setLong`. H2 1.3 coerces silently, PostgreSQL refuses.
  `JdbcMutableAclService` then *swallows* the `DataAccessException` (`catch → return null`), which is
  why the symptom surfaced one statement later. Fixed by casting the parameter — not the column — in
  the three affected queries in `conf/catgenome/acl-dao.xml` (`CAST(? AS BIGINT)`), so the unique
  index on `(object_id_class, object_id_identity)` is still usable and both flavours take the same
  SQL. Overriding the three library methods in `JdbcMutableAclServiceImpl` was the alternative;
  rejected because `findChildren` would mean copying private helpers (`mapObjectIdentityRow`,
  `AclClassIdUtils`) that will move again in Spring Security 6. Note this was reachable only because
  Phase 0 made the PostgreSQL suite exercise the ACL code at all.
- **PostgreSQL keeps microseconds, and JDK 9+ `LocalDateTime.now()` does not — three more
  PostgreSQL-only failures.** `BlastTaskDaoTest.testSaveTask`/`testUpdateTask` and
  `ActivityDaoTest.shouldCreateReadDeleteActivity` compare a saved instance with the reloaded one and
  got `expected:<...T20:38:12.677146279> but was:<...677146>`. On JDK 8 the default `Clock` ticked in
  milliseconds, so the round trip was lossless by accident; from JDK 9 `now()` carries nanoseconds,
  which `timestamp` cannot store (H2 1.3 can, hence H2 stayed green). The two fixtures now build
  their timestamps with `.truncatedTo(ChronoUnit.MICROS)`, so the assertions stay exact equality and
  still catch a real DAO break — rather than being loosened to an approximate comparison. No
  production code compares a persisted `LocalDateTime` for equality; the stored value has always been
  truncated on PostgreSQL.
- **`.devenv` needed three edits for the JDK, not one.** `GRADLE_H2` in the `Makefile` becomes
  `with-java17 ./gradlew --no-daemon`, but `make test-pg` does not go through it — the `test-pg`
  service in `docker-compose.yml` carries its own `command:` array, which also has to be
  prefixed with `with-java17`. The third is the `cli` service: the CLI jar is now built by the 17
  toolchain, so the container needs `JAVA_VERSION: ${CLI_JAVA_VERSION:-17}` and
  `cli/entrypoint.sh` needs to select a JDK from it. Deliberately a *new* variable rather than
  reusing `NGB_JAVA_VERSION`, which `.env` pins to 8 and which selects the JDK the *server jar*
  runs on — the two are independent, and reusing it made the CLI start on 8 and die with
  `UnsupportedClassVersionError`. `entrypoint.sh` also had to replace its
  `/usr/local/bin/ngb` symlink with a wrapper that exports `JAVA_HOME`, because
  `docker-compose exec` starts from the *image* environment and never sees what the entrypoint
  exported. The toolbox image still defaults to JDK 8, which stays correct until Phase 9
  retires it.
- **Gradle's Ant-based tooling needs one `--add-opens` on JDK 17, in a root
  `gradle.properties`.** Every `checkstyleMain`/`pmdMain` died in
  `DefaultIsolatedAntBuilder` → `PreferenceCleaningGroovySystemLoader`, which reflects into
  `java.util.prefs.AbstractPreferences` to stop the Ant classloader leaking preference threads.
  So `org.gradle.jvmargs` in a new repo-root `gradle.properties` carries
  `--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED` (with the heap settings that were
  previously implicit). Note this file is *not* the one Phase 1 deleted — that was
  `net.saliman.properties`' input, keyed on `filterTokens`; this one configures the Gradle JVM
  and has nothing to do with the plugin. Unlike the EhCache flags this is a build-only concern:
  the shipped jar never touches `java.prefs`.
- **DIVERGENCE from the decision table: PMD is 6.55.0, not 7.x, because Gradle 7.6 cannot run
  PMD 7.** Gradle's `Pmd` task drives the tool through `net.sourceforge.pmd.ant.PMDTask`, which
  PMD 7 removed; PMD 7 support landed in Gradle **8.6**. Confirmed against the tool, not the
  release notes: with `toolVersion = "7.0.0"` the task fails at
  `ClassNotFoundException: net.sourceforge.pmd.ant.PMDTask`. The decision ("Checkstyle 10.x/11.x,
  PMD 7.x") is therefore met at Phase 3, not here, and this was agreed before the ruleset was
  rewritten. Very little is lost by waiting: PMD 6 and 7 share the same
  `category/java/*.xml` layout, so the rewrite below is the real work and Phase 3 is a
  `toolVersion` bump plus a handful of rule renames (`UnnecessaryImport` and
  `EmptyControlStatement` already exist in 6.55; the notable 7-only replacement is
  `UnnecessaryBoxing` for the deprecated `BooleanInstantiation`).
- **`ruleSetFiles` does not replace `ruleSets`, and Gradle's default for `ruleSets` is no longer
  harmless.** First run of the rewritten ruleset reported **1690** violations from rules the
  project never asked for. Cause: `PmdExtension.ruleSets` defaults to
  `["category/java/errorprone.xml"]` — the whole category — where under Gradle 3 it defaulted to
  the much narrower `java-basic`, and `ruleSetFiles` is *additive* to it rather than a
  replacement. Both modules now set `ruleSets = []` explicitly. Worth remembering as a general
  shape: a Gradle-version bump can change what a *default* means without any config being wrong.
- **The second ruleset, `pmd-ruleset-feature-index-manager.xml`, never did what it claimed and is
  deleted.** With `ruleSets = []` the count fell to 116 — which was every real violation reported
  exactly twice, proven by identical `line`/`column` pairs in the XML report. The file re-declared
  the entire rule list with `AvoidCatchingGenericException` off and an `include-pattern` for
  `FeatureIndexManager.java` and `util/TestUtils.java`, so every rule shared with the main ruleset
  ran twice. The patterns cannot have been working either, and this was verified by disassembling
  `RuleSets.class` from `pmd-core-6.55.0.jar`: `apply()` runs `ruleChain.apply(...)` *before* it
  consults each ruleset's `applies(File)`, and nearly every PMD rule is rule-chain based, so
  per-ruleset `include-pattern`/`exclude-pattern` are silently ignored for them. The file was also
  simply obsolete — `FeatureIndexManager` catches only `IOException` today, and
  `util/TestUtils.java` lives under `src/test`, which `pmdMain` never scans. Same reasoning
  removed the two `<exclude-pattern>` entries from the main ruleset. **If a future phase needs a
  per-file exemption, use `@SuppressWarnings("PMD.RuleName")` or `//NOPMD`, not file patterns.**
- **All 59 remaining violations (58 server + 1 CLI) were real findings from PMD 5 blind spots,
  fixed at the source; nothing suppressed.** Five distinct detections, none of them stylistic noise:
  `AppendCharacterWithChar` on `.append("\t")` — PMD 5's XPath only matched single-character
  literals and an escape sequence is two characters in the source (50 sites across `GffFeature`,
  `GtfFeature`, `NggbMafFeature`, and one `"\n"` in the CLI's `Project`);
  `InefficientEmptyStringCheck` on `line.trim().isEmpty()` → `isBlank()` in five codecs; `AvoidFieldNameMatchingMethodName` on `GiemsaStain.value` vs `value()` (renamed the
  private field to `stainName`, public API untouched); `UnnecessarySemicolon` on a stray `};` in
  `AbstractAssociationManager`; and `UseLocaleWithCaseConversions` on
  `!"true".equals(localLogout.toLowerCase().trim())` in `OptionalSAMLLogoutFilter` — a real
  locale bug (Turkish `I`), fixed by `equalsIgnoreCase`, which removes the case conversion
  altogether rather than passing a `Locale` to it.
- **Two rules were consciously narrowed, and this is the whole of what was "disabled".**
  (1) `SuspiciousConstantFieldName` (old `naming.xml`) has no PMD 6 equivalent that does only what
  it did; its successor is `FieldNamingConventions`, which would enforce field naming generally and
  therefore contradict this project's long-standing exclusion of `VariableNamingConventions`.
  Dropped rather than smuggled in as a much broader rule. (2) `ClassNamingConventions` in PMD 6
  gained `utilityClassPattern`/`abstractClassPattern` defaults (`[A-Z][a-zA-Z0-9]*(Utils?|Helper|Constants)`
  and `Abstract[A-Z]\w*`) that PMD 5 did not have; both are set back to plain
  `[A-Z][a-zA-Z0-9]*`, so the rule checks what it always checked. Everything else maps across:
  the old `AbstractNaming` and `MisleadingVariableName` rules no longer exist at all, and
  `UnnecessaryImport`, `ControlStatementBraces`, `EmptyControlStatement` and
  `UnnecessarySemicolon` each subsume several deleted rules. Both rulesets carry this mapping as a
  header comment so the next reader does not have to re-derive it.
- **Checkstyle 7.2 → 11.1.0 needs exactly three config edits.** The DTD reference goes to
  `configuration_1_3.dtd`; `LineLength` moves from `TreeWalker` to `Checker` (it became a
  `Checker` child in 8.24) and loses its `tabWidth` property, so `tabWidth` is declared on both
  `Checker` and `TreeWalker`; and `LeftCurly` loses `maxLineLength` (removed in 8.0). Both modules
  keep byte-identical copies, so both were edited. The `severity=warning` at `Checker` level is
  why this is a low-risk bump — Gradle's `Checkstyle` task defaults to `maxWarnings =
  Integer.MAX_VALUE`, so the 37 server / 6 CLI warnings do not fail the build. That is a
  pre-existing choice, recorded in `TEST-BASELINE.md`, not something this phase introduced.
- **Sub-commit (a) introduced a packaging regression that only the CLI container caught, and it
  would have broken the Docker image.** Replacing the old blank-the-version-and-rename hack with
  `archiveFileName = 'ngb-cli.tar.gz'` produced the right file name and the wrong *contents*: the
  Distribution plugin derives the archive's root directory from the archive naming properties, so
  the tarball unpacked into a directory literally called `ngb-cli.tar.gz/`. `docker/core/Dockerfile`
  (`ENV CLI_HOME $INSTALL_DIR/ngb-cli/bin/`) and `docs/md/cli/installation.md` both expect
  `ngb-cli/bin/ngb`. Fixed with `archiveBaseName = 'ngb-cli'` + `archiveVersion = ''` (+
  `archiveExtension`), which gives both the fixed file name and the `ngb-cli/` root. Lesson for
  Phase 9: the jar building is not the same as the distribution being usable — unpack it.
- **The `make cli-test` exit criterion cannot be met: its fixture host no longer exists.**
  `e2e/integration_tests.sh` `wget`s its test data from
  `http://ngb.opensource.epam.com/distr/data/tests/`, and that name is **NXDOMAIN** from both the
  container and the host (`opensource.epam.com` itself resolves; the `ngb` host does not) —
  `wget: unable to resolve host address`, exit 4. This is external decay, unrelated to the
  migration, and it is *not* worked around: the target now documents it and it is recorded in
  `TEST-BASELINE.md`. In its place the CLI↔server contract was exercised for real on JDK 17
  through the `cli` container against `make up`, using fixtures from the repo:
  `reg_ref` (including the duplicate-name negative case), `list_ref`, `reg_file`, `search`,
  `reg_dataset`, `del_file` on a file used by a dataset (negative case), `del_dataset`,
  `del_file`, and a `loadChromosomes` REST call returning `A1 / 56,400 bp`. That covers
  registration, listing, search, deletion and the error paths — the same ground `testcases.csv`
  covers — but through hand-driven commands rather than `integrationCliTest`. **Phase 9 owns the
  real fix**: either host the fixtures somewhere live or generate them, since `cli-tests.gradle`
  and `testcases.csv` are otherwise sound and Groovy 3 parses them fine.

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
- ~~`make cli-test` passes.~~ **Not achievable and not achieved** — the fixture host
  `ngb.opensource.epam.com` no longer exists, so the target cannot start. Substituted with a
  hand-driven CLI↔server round-trip on JDK 17; both are recorded in `TEST-BASELINE.md`, and
  Phase 9 owns hosting the fixtures again.

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
- `make cli-test` — **unrunnable since Phase 2** (its fixture host is NXDOMAIN; see
  `TEST-BASELINE.md`). Drive the CLI by hand against `AUTH_MODE=none` instead, as Phase 2 did.
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

#### Phase 3 execution findings

Written as they were made. Phase 2's exit criteria were re-confirmed first: `jar-fast` and
`lint` green, H2 `make test` 528/**4** failed/21 skipped — the 3 baseline failures plus
`PdbDataManagerTest.testParse`, which `TEST-BASELINE.md` already documents as flapping on
live RCSB data.

**Versions resolved (VERIFY items in tasks 1, 2, 9, and the PMD carry-over from Phase 2).**
All checked against Maven Central / the Gradle release feed on 2026-08-21:

| Thing | Plan said | Reality | Chosen |
|---|---|---|---|
| Gradle | 8.14.x | 8.14.5 is the latest 8.x | **8.14.5** |
| Spring Boot | 3.5.x | 3.5.16 | **3.5.16** |
| springdoc | `2.x` | latest is 3.1.0, but 3.x targets **Boot 4** | **2.8.17** (the current 2.x, the Boot-3 line) |
| PMD | "7.x" | 7.26.0 | **7.26.0** |
| Checkstyle | stays 11.1.0 | 14.0.0 exists | **11.1.0** — the decision table pins it |
| `io.spring.dependency-management` | — | 1.1.7 already in use, still current | unchanged |
| Caffeine | — | 3.2.4; Boot 3.5's BOM manages it | **no explicit pin** |
| JAXB runtime | `org.glassfish.jaxb:jaxb-runtime` | Central's "release" for `jakarta.xml.bind-api` is 4.1.0-M1 | **Boot-managed 4.0.x**, no pin |

**Rename surface measured** (`javax.` imports across `server/`, before any edit):

| Package | Files | Imports | Action |
|---|---|---|---|
| `javax.servlet` | 24 | 46 | → `jakarta.servlet` |
| `javax.xml.bind` | 64 | 282 | → `jakarta.xml.bind` |
| `javax.xml.xpath` (16), `javax.xml.parsers` (12), `javax.net.ssl` (11), `javax.annotation` (9), `javax.xml.datatype` (5), `javax.sql` (3), `javax.xml.stream` (2), `javax.xml.namespace` (2), `javax.naming{,.ldap}` (3) | | 63 | **left alone** — still JDK packages |

No `javax.` reference exists in any XML or `.properties` file, so the rename is Java-source-only.

**Security blast radius mapped (task 5).** SAML is fully contained in
`app/SAMLSecurityConfiguration.java`, `app/JWTSecurityConfiguration.java` and
`security/saml/**`. The `com.auth0:java-jwt` dependency is confined to `security/jwt/**`;
`entity/security/{JwtRawToken,JwtTokenClaims}` are plain entities with no auth0 import and are
used by `UserContext`, `UserSecurityService`, `UserController` and `AuthManager`, so they stay.
The single real coupling out of the deleted set is `AuthManager`'s constructor-injected
`JwtTokenGenerator`.

**Swagger surface measured (task 9).** 41 files import `com.wordnik` (39 `@Api`, 40
`@ApiOperation`, 39 `@ApiResponse(s)`, 1 `@ApiModelProperty`); only `config/SwaggerConfig.java`
imports `com.mangofactory`.

**Logging format checked (task 10).** All five `log4j.xml` files (`profiles/{dev,jar,release,staging}`
and `src/test/resources`) are **log4j 1.x format** — `<!DOCTYPE log4j:configuration SYSTEM
"log4j.dtd">`, `<log4j:configuration>`, `org.apache.log4j.*` appender classes, `<param name=.../>`.
So the plan's "converted to log4j2 format if it is still log4j-1 format — **check**" resolves to:
yes, they need converting.

**Sub-commit (a): Gradle 8.14.5 + PMD 7.26.0 + Phase 2 carry-overs.** Landed on its own, before any
Boot 3 work: the Boot 2.7.18 Gradle plugin configures and builds fine on Gradle 8.14.5, so the
toolchain move does not have to be entangled with the framework move.

*Phase 2 carry-over #4 resolved.* The `--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED` in the
root `gradle.properties` is no longer needed — Gradle 8.14 supplies it to its own daemon. Removed,
and `make lint` is green without it.

*The Boot BOM poisons the PMD tool classpath.* `io.spring.dependency-management` applies the Boot
BOM to **every** configuration in the project, including the `pmd` configuration Gradle assembles to
run the analyser. PMD 7 needs `commons-lang3 >= 3.13` for `EnumUtils.getEnumMap(Class, Function)`;
Boot 2.7.18's BOM pins 3.12.0, and `pmdMain` then dies with a `NoSuchMethodError` before it reads a
source file. The fix is a `resolutionStrategy.eachDependency` rule on `configurations.pmd`, **not** a
`pmd "org.apache.commons:commons-lang3:3.20.0"` dependency declaration: Gradle's `PmdPlugin` adds
the analyser jars themselves through `Configuration.defaultDependencies`, which is skipped the
moment the configuration has any declared dependency, so declaring one trades the `NoSuchMethodError`
for `ClassNotFoundException: net.sourceforge.pmd.PMD`. The analyser's classpath is not the
application's; keeping the app's managed version off it is the right shape regardless.

*Two rule references had to move* (both rulesets, server and ngb-cli):
`performance.xml/BooleanInstantiation` was removed in PMD 7 → `codestyle.xml/UnnecessaryBoxing`
(wider: it also flags redundant `valueOf`/`xxxValue` on the other wrapper types), and
`AvoidCatchingGenericException` moved from `design.xml` to `errorprone.xml`. **A stale rule reference
fails silently:** PMD logs `Cannot load ruleset … An XML validation error occurred` and
`No files to analyze`, and Gradle still reports `BUILD SUCCESSFUL`. Worth knowing before trusting a
green PMD run after any ruleset edit — check that the report has a non-zero file count.

*Four kept rules are scheduled for removal in PMD 8* (warnings on every run, both modules):
`codestyle/GenericsNaming`, `errorprone/AvoidCatchingThrowable`, `errorprone/AvoidCatchingNPE`,
`errorprone/AvoidLosingExceptionInformation`. None has a named successor. They work in 7.x and each
is a rule this project wants, so they stay; PMD 8 is where that has to be answered.

*Violation counts, all fixed at source — no suppressions except one:*

| Task | Violations | Notes |
|---|---|---|
| `catgenome:pmdMain` | 79 | 33 self-qualifier removals, 6 redundant `final`, the rest below |
| `catgenome:pmdTest` | 33 | 28 self-qualifier, 3 unused import, 1 redundant `final`, 1 `AvoidCatchingGenericException` |
| `ngb-cli:pmdMain` | 9 | |
| `ngb-cli:pmdTest` | 18 | all same-package imports |

**`pmdTest` and `checkstyleTest` have never run in this repo's workflow**, which is why 51 of those
violations existed before PMD 7 and would have been reported by 6.55 just the same (every rule
involved exists in 6.55 under the paths the Phase 2 ruleset already used). `make lint` runs
`checkstyleMain pmdMain` only, and `make jar` passes `-PnoTest`, which switches `buildJar`'s task
list from `clean build` to `clean bootJar` — so `check`, and with it the test-source analysis, is
never reached. Phase 3 ran them for the first time. This also corrects a Phase 2 note now amended in
`server/catgenome/build.gradle`: it said `util/TestUtils.java` no longer needed the
`AvoidCatchingGenericException` exemption "because it lives under src/test, which pmdMain never
scans" — true of `pmdMain`, but `pmdTest` scans it.

*The one suppression.* `util/TestUtils.assertFail` carries
`@SuppressWarnings("PMD.AvoidCatchingGenericException")`. Its `TestTask.doTest()` is declared
`throws Exception` and the helper's whole job is to catch whatever comes out and compare its class
against a caller-supplied list, so a narrower catch cannot express what the method is for. This is
the same exemption the deleted `pmd-ruleset-feature-index-manager.xml` was trying to grant, stated
locally where it can be seen.

*Six of the fixes were not mechanical and are recorded because a naive fix would have changed
behaviour:*

- `FeatureIndexManager` — `UnnecessaryBoxing` on `filterForm.getPageSize().doubleValue()` (×5).
  Deleting `.doubleValue()` would have turned double division into **integer** division, because the
  other operand is an `int` count. Replaced with a `(double)` cast, which PMD reports under a
  different rule (`UnnecessaryCast`) that does not fire here.
- `entity/FeatureFile` — `BooleanGetMethodName` on `getCompressed()`. Renaming it to `isCompressed()`
  would have left the class-level Lombok `@Getter` free to generate `getCompressed()` alongside it:
  two accessors for one property, which is how Jackson conflicts get made (Lombok generates `getX`
  for a `Boolean` field, not `isX`). The six hand-written accessors on this class were byte-for-byte
  what `@Getter`/`@Setter` already generate, so they were deleted instead; all 26 `getCompressed()`
  call sites keep working.
- `entity/vcf/VcfFilterForm` — renaming `getIsExon()` to `isExon()` then tripped
  `AvoidFieldNameMatchingMethodName` against the field `isExon`, so the field became `exon`. The JSON
  property the client sends is `"exon"` either way; it was *also* reachable as `"isExon"` before,
  through the `setIsExon()` Lombok generated for the old field name, and nothing sends that.
- The other `getX`→`isX` renames are safe for the wire format because Jackson accepts an `is`-getter
  for both `boolean` and `Boolean`, so the JSON property name is unchanged.
  `java.beans.Introspector` and `BeanUtils.copyProperties`, which do **not**, are unused in this
  repo — checked. Renamed: `BookmarkItemVO.isCompressed`, `NCBISummaryVO.isMultipleAuthors`,
  `TrackQuery.isCollapsed`, `BamQueryOption.isShowClipping`/`isShowSpliceJunction`, `Read.isStand`,
  `VcfFilterForm.isExon`, and in ngb-cli `PermissionGrantRequest.isPrincipal`,
  `RegistrationRequest.isDoIndex`/`isNoGCContent`.
- `ExceptionAsFlowControl` in `AbstractFeatureReader` and `AbstractEnhancedFeatureReader` — the
  `TribbleException` thrown for a non-Ascii codec was caught by the same `try`'s
  attach-the-source clause and rethrown. Reader construction moved into a private helper so the
  `try` only translates failures; the exception still reaches that clause and still comes out with
  the source attached. Same rule in `FeatureIterator.initStream`, where a bare `EOFException` was
  thrown to be wrapped by the `IOException` clause below it — now the wrapped exception is raised
  where the condition is found, with the same message and the `EOFException` as its cause.
- `util/ProteinSequenceUtils.RnaCodonTable` and `ngb-cli entity/BiologicalDataItemFormat` both
  carried `UnusedPrivateField` state that nothing ever read. In the codon table it was an "extended
  title" and a full amino acid name, and the errors that had accumulated in them prove nobody read
  them: `PRO`'s extended title was `"Ser"` and `THR`'s full name had a trailing space. In the CLI
  enum it was a numeric `id` with no getter whose values were exactly declaration order 1..22, so
  the ordinal already says everything it said; the format goes to the server by name. Both deleted,
  with the full names kept as comments since that is all they ever were.
  `HeatmapAnnotationType.from` caught `IllegalArgumentException` from `valueOf` only to throw another
  `IllegalArgumentException` with a friendlier message (`AvoidThrowingNewInstanceOfSameException`);
  it now matches the name instead, same exception type and message.

*Gradle 8's newer JaCoCo agent needs a fifth `--add-opens` for EhCache's sizer.* `make test` went
from 4 failures to **28** on Gradle 8.14.5 with no source or JVM-flag change: 24 new
`InaccessibleObjectException: Unable to make field private final byte[] java.lang.String.value
accessible`, thrown from `ObjectGraphWalker.getAllFields` under `EhCacheBasedIndexCache.putInCache`.
The test worker's command line is byte-identical between 7.6.6 and 8.14.5 apart from
`-Dorg.gradle.native=false` (7.6 only) and the **JaCoCo agent version — 0.8.8 on Gradle 7.6, 0.8.13
on Gradle 8.14.5**. Pinning `jacoco { toolVersion = '0.8.8' }` under Gradle 8 makes the failures go
away, which identifies the agent as the trigger; why the newer instrumentation puts a `String` in the
sized graph is not pinned down. **The pin is not the fix** — JaCoCo 0.8.8 cannot read Java 21 class
files, so it would fail the moment the toolchain moves later in this phase. Added
`--add-opens=java.base/java.lang=ALL-UNNAMED` to the test JVM args instead; `make test` is back to
528/**4**. All five flags go away with EhCache → Caffeine (D11) later in this phase. The running app
is unaffected — it has no JaCoCo agent — so `JAVA_REQUIRED_OPTS` in `.devenv/ngb/entrypoint.sh` still
needs only the original four.

*Gradle 8 deprecations found, two of them Gradle-9-blocking.* `GradleBuild.buildFile` (root
`build.gradle`, `buildJar`/`buildCli`/`buildAll`) is deprecated for removal in Gradle 9, and Gradle's
own advice is to use `dir` — which all three tasks already set to the same directory. Removed; the
nested builds' task graphs are identical with and without it (checked on `buildCli`), and `make jar`
is green. Still outstanding for whenever Gradle 9 arrives, and **not** touched here:
`Project.exec(Closure)` in `client/build.gradle:18` and
`export-templates/target-identification/build.gradle:15` (→ `ExecOperations.exec(Action)`), plus a
long tail of Groovy space-assignment nags (`group "com.epam"`, `url "…"`) that Gradle 10 removes.

**Sub-commit (b): Boot 3.5.16, Spring 6, jakarta, Jetty 12, log4j 2, security-as-anonymous.** Tasks
3–11 of the phase. What follows is what the plan did not predict; the mechanical parts (BOM version,
`javax.servlet`/`javax.xml.bind` renames, `spring-boot-starter-*` swaps, springdoc, HikariCP, Caffeine,
JUnit-vintage on the JUnit 5 platform) went as written and are visible in the diff.

*Divergence, task 3: `javax.annotation` had to be renamed too, contrary to the rename table above.*
The table put `javax.annotation` (9 files) in the "left alone — still JDK packages" row. It is not a
JDK package: `javax.annotation.PostConstruct`/`PreDestroy` were part of Java EE, shipped in the JDK
only between 6 and 10, and left with the rest of `java.xml.ws.annotation` in Java 11. Spring 6 honours
**only** `jakarta.annotation.PostConstruct` — `CommonAnnotationBeanPostProcessor` no longer looks for
the javax names at all. Left alone, this compiles (the API jar is on the classpath transitively) and
then silently never runs the eight `@PostConstruct` initialisers, which is a far worse failure than a
compile error: `FileManager`, `BedManager`, `PathwayManager`, `BlastRequestManager`,
`CloudPipelineManager`, `NCBIDataManager`, `TaskExecutorService`, `AzureBlobClient`, plus
`NGBRegistrationUtils` in the tests. All nine renamed to `jakarta.annotation`, whose API jar Boot
manages. Nothing else in that row moved: `javax.xml.xpath/parsers/datatype/stream/namespace`,
`javax.net.ssl`, `javax.sql` and `javax.naming` are genuinely still in the JDK and are untouched.

*Divergence, task 3: one file stays on `javax.xml.bind` on purpose.* `manager/pathway/PathwayManager`
builds a `JAXBContext` over `org.sbgn.bindings` by hand, and the classes in that package — from
`org.sbgn:libsbgn:0.2` — are annotated with **javax**.xml.bind. A jakarta `JAXBContext` reads jakarta
annotations only, so it would bind them as unannotated POJOs; the compiler said as much, 17 ×
`unknown enum constant XmlAccessType.FIELD / class file for javax.xml.bind.annotation.XmlAccessType
not found`. Three dependencies are in this position (found by scanning their class files in the
Gradle cache for `javax/xml/bind`): `org.sbgn:libsbgn:0.2` — a 2011 jar whose POM declares no
dependencies at all, because JAXB was still in the JDK then — `org.biopax.paxtools:sbgn-converter:5.0.0`,
which marshals SBGN back out through libsbgn (`L3ToSBGNPDConverter.writeSBGN`, the `POST
/pathway/biopax` endpoint), and `org.biojava:biojava-structure:4.2.0`. None has a jakarta release, and
replacing them is a dependency-refresh job, not a Phase 3 one. So both JAXB stacks are on the
classpath: jakarta 4.0 (Boot-managed) for our own 64 binding classes, and `javax.xml.bind:jaxb-api:2.3.1`
+ `com.sun.xml.bind:jaxb-impl:2.3.9` for theirs. They share no package and no service file. The
runtime is `com.sun.xml.bind:jaxb-impl` rather than `org.glassfish.jaxb:jaxb-runtime:2.3.x` because
the latter is the same `group:artifact` as the jakarta 4.x runtime and Gradle would resolve the pair
to one version. `PathwayManagerTest` covers the read path with a real `.sbgn` file; the `.owl`
(BioPAX) path has no test.

*Spring 6 removed `@Required` — 184 annotations across 20 files, and a startup check with them.*
`org.springframework.beans.factory.annotation.Required` and its `RequiredAnnotationBeanPostProcessor`
were deprecated in 5.1 and deleted in 6.0. Every XML-configured DAO used it on its setters (19 DAOs in
main plus `util/AclTestDao` in the tests) to assert at context-refresh time that each of its SQL query
properties had been set from `conf/catgenome/dao/*.xml`. There is no Spring 6 equivalent — the
official answer is constructor injection, which for 19 setter-injected beans wired across 47 XML files
is its own migration. The annotations and imports are gone and the properties are now plain optional
setters. **Behavioural loss, recorded deliberately:** a query missing or misspelled in a DAO's XML
used to fail the context on startup with the offending property named; it now surfaces as an NPE the
first time that query is used. Nothing in the test suite covers it either way.

*Spring 6 also removed the no-message `Assert` overloads — 42 call sites.*
`Assert.isTrue(boolean)` and `Assert.notNull(Object)` were deprecated in 4.3.7 and deleted in 6.0
(they still existed in Spring 5.3, which is why Phase 2 compiled). 36 sites in main and 6 in tests
needed a message written for them; each got one describing the condition, and the several that carry a
side effect inside the assertion (`Assert.isTrue(file.createNewFile(), …)` in `FileManager`,
`DownloadFileManager`, `FeatureSorterFactory`) keep it, since the file creation is the point.
`BedGraphCodec`'s `tokens.length == 4` got a `BED_GRAPH_COLUMNS` constant on the way past, because
PMD's `AvoidLiteralsInIfCondition` would otherwise have flagged the new message string's `+ 4`.

*Two dependencies turned out to be accidental transitives of things this phase deleted.*
`org.apache.commons:commons-lang` **2** was never declared in `build.gradle` at all, yet 12 files
imported it: it arrived through the mangofactory Swagger stack, and vanished with it. Those imports
moved to their lang3 equivalents (`StringUtils`, `ArrayUtils`, `time.DateUtils` — same methods, same
semantics); `lang.math.RandomUtils` became `ThreadLocalRandom` in the two tests that used it, because
lang3's `RandomUtils` has a different signature set and is deprecated in current releases. Likewise
`org.bouncycastle.util.Strings.toLowerCase` in `GenePredUtils` came in with OpenSAML and left with it,
replaced by `Locale.ROOT` lowercasing — which is what the Bouncy Castle helper did. The explicit
`commons-lang3 3.0` pin (2011) is gone at the same time; the BOM manages that version.

*Other Boot-3 / Spring-6 relocations hit on the way, each documented at its site:* Jetty 12 deleted
`AbstractHandler` and split the servlet API out of `jetty-server` into `org.eclipse.jetty.ee10`, so
the tests' local file server is now an `HttpServlet` in a `ServletContextHandler`
(`controller/util/UrlTestingUtils`); Boot 3.2 moved `JarLauncher` to
`org.springframework.boot.loader.launch` (`springBootlauncherClass`, three bundle tasks);
`MockMvcRequestBuilders.fileUpload` is gone in favour of the identical `multipart`
(`CytobandControllerTest`); slf4j 2 has no `slf4j-log4j12`, which is what forced
`spring-boot-starter-log4j2` and the rewrite of the five log4j-1-format `log4j.xml` files as
`log4j2.xml` (and `-Dlog4j.configuration` → Boot's `logging.config` in `.devenv`); Boot's `@MockBean`
is deprecated for removal in 3.4, so `ExternalDBControllerTest` uses Spring's own `@MockitoBean`.

*A pre-existing bug in `AclSecurityConfiguration.roleHierarchy()`, left alone.* The method calls
`RoleHierarchyImpl.setHierarchy` sixteen times (one literal, one `forEach` over seven manager roles,
one joined `==` string, another `forEach` over the same seven), and every call **replaces** the whole
hierarchy rather than adding to it, so only the last one — `ROLE_*_MANAGER > ROLE_USER`, seven times
over, the last of which wins — has ever taken effect. It behaves
the same way on Spring Security 5 and 6, so it is not a migration regression and fixing it here would
change authorisation behaviour inside a phase whose security story is already "anonymous only". Noted
for Phase 4, which rewrites this file's neighbourhood anyway. (`RoleHierarchyImpl.setHierarchy` is
itself deprecated in Security 6.3 in favour of `RoleHierarchyImpl.withRolesFromHierarchy`, which is
the natural place to fix it.)

*A note for Phase 4.* The deleted SAML and JWT configurations' lists of unsecured resources referred
to Swagger 1's `/api-docs`; when they come back from git they must point at springdoc's
`/v3/api-docs/**` and `/swagger-ui/**` instead, or the API docs will be behind authentication.

*Unrelated breakage found and not fixed:* `bundleWindows`/`bundleLinux` in
`server/catgenome/build.gradle` bundle a **JRE 8** fetched from
`http://download.oracle.com/otn-pub/java/jdk/8-b132/`, an Oracle OTN path that has not served an
anonymous download for years, and their launcher scripts hard-code `jre1.8.0/bin/java`. Both tasks are
therefore already broken on `develop`; neither is part of `make jar`, and this phase leaves them as
found. Whoever revives them needs a JDK 21 runtime, not a patch.

*The AWS SDK v1 could not start under Boot 3.5's Jackson — found at first boot, not at compile time.*
`make jar` was green and the context then died with
`BeanInstantiationException` → `NoSuchFieldError: PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE`
thrown from `EC2MetadataUtils.<clinit>` in aws-java-sdk-core **1.11.704** (2020), reached from
`Application.s3Client(Application.java:111)`. Jackson deleted that constant in 2.12; Boot 3.5 manages
2.19. Because NGB builds the S3 client on every startup, S3 configured or not, this failed the whole
application before a single request was served — the first thing `make smoke` hits. Fixed by moving
`aws-java-sdk-s3`/`-sts` to **1.12.797**, the last v1 release, whose bytecode asks for
`PropertyNamingStrategies.UPPER_CAMEL_CASE` and falls back to the old constant on `LinkageError`, so it
works either way (verified with `javap` before editing). Deliberately still SDK **v1**: D-table says
v2, and that is a six-file API rewrite belonging to the dependency-refresh phase.

*Spring Security 6 broke every NGB security expression, and the fix is one method.* With the ACL wiring
otherwise complete, 16 tests failed identically —
`SpelEvaluationException: EL1004E: Method call: Method isAllowed(...) cannot be found on type
MethodSecurityExpressionRoot` (`NGBSessionSharingSecurityTest` ×8, `ProjectSecurityServiceTest` ×4,
`DataItemSecurityServiceTest` ×3, `AclPermissionSecurityServiceTest` ×1). `javap` on
spring-security-core 6.5.11 explains it: Security 6 added
`DefaultMethodSecurityExpressionHandler.createEvaluationContext(Supplier<Authentication>, MethodInvocation)`
and made it what the method-security interceptors call, and that overload builds its root through a
**private** `createSecurityExpressionRoot(Supplier, MethodInvocation)` — so
`NGBMethodSecurityExpressionHandler`'s `protected createSecurityExpressionRoot(Authentication,
MethodInvocation)` override, which is what installs `NGBMethodSecurityExpressionRoot` and its
`isAllowed`/`hasPermission…` family, was never reached and every expression evaluated against the stock
root. The handler now overrides the supplier-based method and delegates to the `Authentication`-based
overload (still public and non-deprecated in 6.5, and the only path through
`createSecurityExpressionRoot`). Cost: the laziness the supplier was added for — the `Authentication` is
now resolved for every secured invocation. Every expression in this application reads it anyway. This is
the trap to remember in Phase 4: a *silent* fallback to the wrong expression root, with no bean-wiring
error to point at it.

*PMD 7's `AvoidDuplicateLiterals` fired on the Assert messages this phase wrote.* The four
`Assert.isTrue(file.createNewFile(), "Failed to create file " + …)` sites in `FileManager` are one
literal repeated four times, which PMD 6.55 tolerated at its old threshold and 7.26.0 does not; hoisted
to a `FAILED_TO_CREATE_FILE` constant. Worth knowing when writing the remaining phases' messages: any
literal used four times over needs a constant.

**Exit criteria, as actually run** (all on the final tree, JDK 21 / Temurin 21.0.11, Boot 3.5.16):

| Criterion | Result |
|---|---|
| `make jar`, `make jar-pg` | green |
| `NGB_JAVA_VERSION=21 make up` + `make smoke` | HTTP 200, `{"payload":"2.8.0","status":"OK"}`; `JAVA_EXTRA_OPTS` empty — all four EhCache `--add-opens` gone |
| `make test` (H2) | 519 tests, **3** failed, 21 skipped — the documented baseline |
| `make test-pg` (after `make reset-pg`) | 519 tests, **11** failed — the documented baseline |
| `make lint` | green: Checkstyle 11.1.0 37 warnings / 0 errors, PMD 7.26.0 clean. Plus `checkstyleTest`/`pmdTest` on catgenome and all four tasks + `build -x test` on `server/ngb-cli` |
| `make cli-test` | **unrunnable**, unchanged since Phase 2 (fixture host `ngb.opensource.epam.com` is NXDOMAIN). Driven by hand instead — see below |
| CLI by hand | the full `TEST-BASELINE.md` recipe on a wiped database: `reg_ref`, duplicate `reg_ref` fails "already exists", `list_ref`, `reg_file`, `search`, `reg_dataset`, `del_file` fails "used in projects: test_ds", `del_dataset` then `del_file` succeed, `search` finds nothing after. CLI on JDK 17, server on 21 |
| UI: reference / genes / VCF / BAM tracks | all four render — see below |
| springdoc | `/v3/api-docs` serves the spec and `/swagger-ui/index.html` renders it (OAS 3.1, "CATGenome Browser REST API") |

The UI was driven in a real browser (headless Chrome over CDP, real mouse events, 1600×1000) against a
**wiped** H2 database populated only through the CLI, so this also covers Flyway 3.2.1 creating the
schema from nothing under Boot 3. Clicking the dataset checkbox opened the summary view — variants by
chromosome, types and quality, i.e. the Lucene feature index — and clicking through to the chromosome
drew the reference nucleotide-density track, the GENE track (the `PGLYRP4` model, exons and strand
arrows) and the VCF track; a second dataset at `X:12,584,100-12,584,900` drew the BAM coverage
histogram with expanded reads and mismatches over a base-resolution reference. No HTTP status ≥ 400 and
no JS error in any of those runs.

One trap for whoever repeats this: headless Chrome has no WebGL by default, PixiJS 6 refuses the canvas
fallback, and the result is a browser window where the reference track paints but the GENE, VCF and BAM
tracks are silently **blank** — indistinguishable from a server-side failure, and it produced exactly
that false alarm here. Launch Chrome with
`--enable-unsafe-swiftshader --use-gl=angle --use-angle=swiftshader` and they appear.

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
  `ngb list_ref`), and the CLI works in SAML mode — by hand, because `make cli-test` itself is
  unrunnable; see `TEST-BASELINE.md`.
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

#### Phase 4 execution findings

Written as they were made.

**Task 1 — OpenSAML artifact availability. The plan's assumption is wrong, and so is the
comment Phase 3 left in `build.gradle`: the Shibboleth repository IS required.**
Checked against the live repositories on 2026-08-21. Spring Boot 3.5.16 manages Spring
Security **6.5.11** (the latest 6.5.x), whose `spring-security-saml2-service-provider` POM
depends on `org.opensaml:opensaml-saml-api:4.3.2` and `opensaml-saml-impl:4.3.2`:

| Coordinate | Maven Central | `build.shibboleth.net/maven/releases` |
|---|---|---|
| `org.opensaml:opensaml-saml-api:4.3.2` | **404** | 200 |
| `org.opensaml:opensaml-saml-impl:4.3.2` | **404** | 200 |
| `org.opensaml:opensaml-core:4.3.2` | **404** | 200 |
| `net.shibboleth.utilities:java-support:8.4.2` | **404** | 200 |

Central's `org/opensaml/opensaml-saml-api/` directory stops at **4.0.1** — Shibboleth
published 4.0.x to Central and then went back to publishing only to their own repository.
So `https://build.shibboleth.net/maven/releases/` goes back into `repositories`, scoped with
`content { includeGroup }` to `org.opensaml` + `net.shibboleth*` so it cannot shadow Central,
and the Phase 3 comment claiming "brings OpenSAML 4/5 from Central and needs no extra
repository" is corrected in place. Net effect: the repository Phase 3 removed for OpenSAML 2
comes back for OpenSAML 4, for a different reason.

**Task 2 — the endpoint-path question, resolved: every legacy path is configurable.**
Read against the real 6.5.11 sources (`-sources.jar` from Central), not the reference docs:

| Legacy (OpenSAML 2 ext) | Spring Security 6.5.11 default | Configurable back to the legacy path? |
|---|---|---|
| `/saml/metadata` (`MetadataDisplayFilter`) | `/saml2/service-provider-metadata/{registrationId}`, `/saml2/metadata{,/{registrationId}}` | **Yes.** `saml2Metadata(m -> m.metadataUrl("/saml/metadata"))`. `Saml2MetadataConfigurer.metadataUrl` asserts only `hasText` — no `{registrationId}` required; with a single registration `BaseOpenSamlMetadataResolver` emits a plain `<md:EntityDescriptor>`, the same document shape as the legacy filter |
| `/saml/SSO` (ACS, `CustomSAMLProcessingFilter`) | `/login/saml2/sso/{registrationId}` | **Yes.** `saml2Login(l -> l.loginProcessingUrl("/saml/SSO"))`. `Saml2LoginConfigurer.loginProcessingUrl` also asserts only `hasText` (the `{registrationId}` assertion that existed in 5.x is gone), and the same matcher is fed to the CSRF-ignore list. The registration still resolves without a path variable: `BaseOpenSamlAuthenticationTokenConverter` tries (1) the AuthnRequest stored in the session, (2) `{registrationId}` from the path, (3) `findUniqueByAssertingPartyEntityId(Response.Issuer)` — (1) covers SP-initiated login, (3) covers IdP-initiated |
| `/saml/SingleLogout` (SLO receiver) | `/logout/saml2/slo` | **Yes.** `saml2Logout(l -> l.logoutRequest(r -> r.logoutUrl(...)).logoutResponse(r -> r.logoutUrl(...)))` |
| `/saml/logout` (SP-initiated logout) | `/logout` | **URL yes, method no** — see below |
| `/saml/login` (`SAMLEntryPoint`) | `/saml2/authenticate?registrationId={id}` | **Yes, with a caveat**: `authenticationRequestUriQuery` *asserts* that the value contains `{registrationId}` in the path or the query, so the exact string `/saml/login` is not available; `/saml/login/{registrationId}` is. This URL is SP-internal — it appears in no metadata and no IdP registration — so it is the one path where the compatibility argument does not apply |
| `/saml/SSOHoK`, `/saml/discovery` | no equivalent | Not ported. Holder-of-Key and IdP discovery were wired by the legacy config but unused: one registration, one IdP, and nothing in NGB or the dev realm references either |

The one genuine wrinkle is **`/saml/logout`**: the client hardcodes it as a browser
navigation (a `GET`) in two places — `client/client/dataServices/data-service.js:186` and
`client/client/app/shared/components/ngbMainToolbar/ngbMainToolbar.component.js:41` — while
`Saml2LogoutConfigurer.createLogoutMatcher()` builds `RequestMatcherFactory.matcher(HttpMethod.POST, logoutUrl)`
with the method **hardcoded to POST**, so a GET never reaches
`Saml2RelyingPartyInitiatedLogoutFilter`. Nothing in the DSL relaxes that. The legacy
`SAMLLogoutFilter` matched `/saml/logout/**` on any method and performed global (SLO) logout.

**IdP metadata from a file** still works out of the box: `RelyingPartyRegistrations.fromMetadataLocation`
is not deprecated in 6.5.11 and resolves through a `DefaultResourceLoader`, so it accepts
`file:` and `classpath:` locations and HTTPS URLs. Caveat for `server.ssl.metadata`, whose
current dev value is the bare path `/secrets/idp-metadata.xml`: a schemeless value is read by
`DefaultResourceLoader` as a *classpath* location, so the configuration has to prepend `file:`
when the property carries no scheme.

**The two decisions taken on the back of the table above** (asked because both are
externally visible, answered 2026-08-21, binding for the rest of the migration):

1. **URL layout: preserve the legacy paths.** `/saml/metadata`, `/saml/SSO`,
   `/saml/SingleLogout` and `/saml/logout` stay exactly as they are, so no existing customer
   IdP registration has to be touched. No dual-path support and no Spring defaults alongside
   them. The single unavoidable change is the SP-internal AuthnRequest endpoint,
   `/saml/login` → `/saml/login/{registrationId}`, i.e. `/catgenome/saml/login/ngb`.
2. **Logout: change the client to POST.** `saml2Logout()` is used as designed, with its
   POST-only matcher, and the two hardcoded `window.location` navigations in the AngularJS
   client become form POSTs. The alternative — reimplementing
   `Saml2RelyingPartyInitiatedLogoutFilter` to accept GET — was rejected: that class is a
   private static inner class of `Saml2LogoutConfigurer`, so it is not public API and a
   copy of it would have to be maintained against every Security 6.x release.

**Task 3 — `com.auth0:java-jwt` 3.1.0 → 4.6.0. Less of a break than the plan implies, but
one silent behaviour change.** Checked against the 4.6.0 sources:

| Legacy call | 4.6.0 |
|---|---|
| `Algorithm.RSA512(privateKey)` / `(publicKey)` | still there — `RSA512(RSAKey)` resolves the public/private halves by `instanceof`. Not deprecated. Compiles unchanged |
| `JWT.create()`, `withHeader(Map)`, `withIssuedAt(Date)`, `withJWTId`, `withSubject`, `withClaim(String, Integer)`, `withArrayClaim(String, String[])`, `sign(Algorithm)` | all unchanged |
| `JWT.require(alg).build().verify(token)` | unchanged; `Verification.build()` now returns the `com.auth0.jwt.interfaces.JWTVerifier` *interface* (the implementation's constructor is package-private), which the legacy code never named |
| `DecodedJWT.getIssuedAt()/getExpiresAt()` → `Date` | unchanged (`…AsInstant()` variants were added alongside) |
| **`Claim.isNull()`** | **changed.** 4.x added `isMissing()` and redefined `isNull()` as `!isMissing() && data.isNull()`. In 3.x an *absent* claim came back as a `NullClaim` whose `isNull()` was `true`; in 4.x an absent claim has `isNull() == false`. `JwtTokenVerifier` guards its two optional claims with `if (!getClaim(CLAIM_ROLES).isNull())`, so a naive port would enter the branch for a token with no `roles`/`groups`, store `null`, and then NPE in `validateRequiredClaims` — a 500 instead of a 401, from a security filter. Ported as `isMissing() || isNull()`, which is exactly the 3.x semantics |

Also fixed while porting, none of it optional: `javax.annotation.PostConstruct` →
`jakarta.annotation.PostConstruct`; `org.apache.commons.lang.StringUtils` → `lang3` (Phase 3
dropped commons-lang 2); Guava `Strings.isNullOrEmpty`/`ImmutableMap.of` → `StringUtils` and
`Map.of` (Guava is only a transitive dependency here and there is no reason to depend on it
for this); `new Long(userId)` → `Long.valueOf(userId)`.

**The plan's Phase 4 text says the JWT tokens are RS256. They are RS512** —
`Algorithm.RSA512` in both the generator and the verifier, and the header of both fixture
tokens in `test-catgenome-auth.properties` decodes to `{"alg":"RS512","typ":"JWT"}`. Nothing
follows from it, but the plan is wrong.

**Finding 2 from Phase 3 — `AclSecurityConfiguration.roleHierarchy()`. The replacement API is
not called what the hand-over said, and part of the intended hierarchy was never expressible.**
In 6.5.11 the replacement for the deprecated `setHierarchy` is
`RoleHierarchyImpl.fromHierarchy(String)` (plus a `withDefaultRolePrefix()`/`withRolePrefix()`
builder); there is no `withRolesFromHierarchy`. Two things fall out of reading the parser:

- `buildRolesReachableInOneStepMap` splits each **line** on `\s+>\s+`. That is the whole
  grammar — so the sixteen `setHierarchy` calls have to become one newline-separated string.
- **`==` is not part of that grammar and never was.** The line
  `ROLE_REFERENCE_MANAGER == ROLE_BAM_MANAGER == …`, which was meant to make the seven
  manager roles mutually equivalent, parses to a single token and contributes nothing. It
  cannot be expressed as a hierarchy either: mutual implication between two roles is a cycle,
  and `buildRolesReachableInOneOrMoreStepsMap` throws `CycleInRoleHierarchyException` for it.
  So that line is dropped rather than translated, and this is noted here because it is the
  one piece of the original intent that is *not* being restored.
- What was actually in effect until now: the last `setHierarchy` call wins, and the last one
  is the final iteration of `managerRoles.forEach(role -> …+ " > " + ROLE_USER)`, so the
  entire live hierarchy has been **`ROLE_SEG_MANAGER > ROLE_USER`** and nothing else. In
  particular `ROLE_ADMIN` has not implied `ROLE_USER`.

**What changed, as required by the hand-over.** The hierarchy is now the fifteen edges the old
code was trying to declare: `ROLE_ADMIN > ROLE_USER`, and for each of the seven manager roles
`ROLE_ADMIN > ROLE_<X>_MANAGER` and `ROLE_<X>_MANAGER > ROLE_USER`. Authorisation consequences,
all of them widenings: an admin now passes checks written against `ROLE_USER` or against any
manager role, a manager passes checks written against `ROLE_USER`, and — because
`SidRetrievalStrategyImpl` expands a principal's authorities through the same hierarchy — ACL
entries granted to `ROLE_USER` now also apply to admins and managers. Registered users already
receive `ROLE_USER` from `RoleManager.getDefaultRolesIds`, so in practice the effective new grant
is `ROLE_ADMIN` over the manager roles. Nothing narrows.

**Task 2 as built — three places where the reading above turned out to be wrong when run.**

1. **`saml2Metadata().metadataUrl(...)` is not usable after all.** The path is right but the
   configurer builds its `OpenSaml4MetadataResolver` internally with signing left at the default
   of *off*, and the extension signed its published metadata
   (`ExtendedMetadata.setSignMetadata(true)`). So the configuration passes its own
   `RequestMatcherMetadataResponseResolver` — over an `OpenSaml4MetadataResolver` with
   `setSignMetadata(true)` — to `metadataResponseResolver(...)` instead, and carries the
   `/saml/metadata` matcher on that resolver.
   **`setUsePrettyPrint(false)` is load-bearing, and this was settled by experiment, not by
   reading.** With pretty-printing on, the metadata signature does not verify:
   `SerializeSupport.prettyPrintXML` inserts whitespace text nodes into the same DOM the enveloped
   signature was computed over, canonicalisation keeps them, and the digest no longer matches.
   Turning it on made `VerifyXmlSignature` report `SignedInfo signature: false, reference: false`;
   turning it back off made it pass. It also matches what `MetadataDisplayFilter` served, which was
   not pretty-printed either.
2. **The `file:`-prefix note above is not what shipped, and the reason is worth recording.**
   Prefixing a schemeless `server.ssl.metadata` and handing it to
   `RelyingPartyRegistrations.fromMetadataLocation` works, but the first boot attempt — with a
   `FileSystemResourceLoader`, which looked like the more faithful reading of
   `new FilesystemMetadataProvider(new File(...))` — failed with
   `FileNotFoundException: secrets/idp-metadata.xml` for the configured
   `/secrets/idp-metadata.xml`: **`FileSystemResourceLoader.getResourceByPath` strips the leading
   `/`** and resolves the rest against the working directory. `FileSystemResource` does not. So
   both the metadata file and the key store go through one `resource(String)` helper that mirrors
   how Boot itself resolves `server.ssl.key-store` — a prefixed location (`file:`, `classpath:`,
   any URL) through the loader, anything else as a plain filesystem path — which keeps the
   existing property values working whichever form they are in.
3. **`authnRequestsSigned` has to be set explicitly.** It defaults to `false` in Spring Security
   and is *not* derived from the asserting party's `WantAuthnRequestsSigned`, whereas the
   extension's `MetadataGenerator` defaulted it to true and therefore always signed. Left at the
   default, NGB would silently stop signing `<AuthnRequest>`s — and no IdP that does not require
   signatures would complain.

**The one product bug that only running found: Spring Security 6 filters `ERROR` dispatches, so
a REST 401 came back as a 302.** An unauthenticated `GET /restapi/user/current` was answered by
the JWT chain with `RestAuthenticationEntryPoint`'s `sendError(401)`, the container then dispatched
to `/error`, and *that* dispatch went through the filter chains again. `/error` is not under
`/restapi/**`, so it matched the SAML chain, which requires authentication — the client received
`302 → /saml/login/ngb` instead of `401`. That breaks every non-browser client, `ngb-cli`
included: it looks at the status code. Fixed with
`authorizeHttpRequests(a -> a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll())` on the
SAML chain. Nothing in the pre-migration code needed this, because Spring Security 4 only filtered
`REQUEST` dispatches by default.

**Task 4 as built — two chains, not the single chain with two entry points the plan asks for.**
JWT is order 1 with `securityMatcher("/restapi/**")` and SAML is order 2 with `/**`. The
coupling the task is really about is gone either way: `JWTSecurityConfiguration` no longer
autowires anything from the SAML side, only reads `saml.security.enable` and, when it is on,
points `/restapi/navigate` — the single REST path a browser opens — at
`SAMLSecurityConfiguration.AUTHENTICATION_REQUEST_URL` through
`defaultAuthenticationEntryPointFor`. So `jwt.security.enable=true` with
`saml.security.enable=false` now starts and answers 401, which was the point. Two chains rather
than one because a single chain would have to carry `saml2Login`'s and `saml2Logout`'s filters on
`/restapi/**` as well, would have to be conditional on the *combination* of the two properties
rather than on one each, and would need its whole entry-point selection expressed as matchers
inside one `DelegatingAuthenticationEntryPoint`; as it is, the only path needing that treatment is
`/restapi/navigate`, inside the JWT chain. Note that the SAML chain matches `/**` *explicitly*:
`WebSecurityFilterChainValidator` rejects an implicit `AnyRequestMatcher` chain in any position
but last, and `NoSecurityConfiguration` (order 3) is one.

**Task 5 done: the port rewrite is deleted and the dev environment is back on the conventional
8443.** `CustomAwareAuthenticationSuccessHandler` was `SavedRequestAwareAuthenticationSuccessHandler`
copied verbatim with one addition, `StringUtils.replace(redirectUrl, "8443", "8080")`; it is not
reimplemented, the plain Spring class is used. `.devenv` follows: `entrypoint.sh`'s `HTTPS_PORT`
default and `docker-compose.yml`, `.env.example`, `keycloak/realm-ngb.json` (client id, base URL,
ACS and SLO URLs — the client id is the SAML entity id, so this is a realm edit, not a redirect
tweak), and the two "looks arbitrary but isn't" notes in `README.md` and
`ngb/auth-saml.properties.tpl` are gone. `entrypoint.sh`'s refusal of `AUTH_MODE=saml` is
removed with them.

**Tasks 6 and 7.** `AclSecurityConfiguration` was never disabled, so this reduced to the JWT
side, as the hand-over said: `AuthManager` takes `JwtTokenGenerator` by constructor again and
`issueTokenForCurrentUser` mints tokens instead of throwing, and
`src/test/resources/applicationContext-test.xml` component-scans `com.epam.catgenome.security.jwt`
again (`JwtTokenGenerator` is a component that every test context with an `AuthManager` needs;
`JwtTokenVerifier` is not — it is a bean of `JWTSecurityConfiguration`). `UserContext` needed two
changes for SAML: it implements `Saml2AuthenticatedPrincipal` as well as `UserDetails`, because
`Saml2LogoutConfigurer` gates SP-initiated single logout on
`principal instanceof Saml2AuthenticatedPrincipal` and reads the registration id and session
indexes for the `<LogoutRequest>` off that interface — a principal without it logs out locally and
never tells the IdP — and its `attributes` field is retyped from `Map<String, String>` to the
`Map<String, List<Object>>` the interface returns. Nothing in the tree read the old field.

**Findings 1 and 3 from Phase 3.** The unsecured-resource list is now
`/swagger-ui/**`, `/v3/api-docs/**`, `/error-401.html`, on the SAML chain — verified anonymously:
all three answer 200 in SAML mode. The pre-migration list also carried `/`, `/index.html` and
Swagger 1's `/api-docs/**`; the first two are pointless (nothing is unauthenticated in a browser
context that needs SSO to be useful) and the third was the stale path the hand-over flagged.
`JWTSecurityConfiguration` deliberately does not repeat the list: its chain is scoped to
`/restapi/**` and none of those paths is under it, so the entries there could never have had any
effect. `NGBMethodSecurityExpressionHandler` was not touched — its
`createEvaluationContext(Supplier<Authentication>, MethodInvocation)` override stands as Phase 3
left it.

**New verification tooling in `.devenv`,** because none of the signing path was checked by
anything before: `make saml-verify-signing` verifies the SP metadata signature with the JDK's
XML-DSIG (`scripts/VerifyXmlSignature.java`, standalone single-file source, no dependencies), then
uploads `secrets/ngb-saml-cert.pem` to the Keycloak client and flips `saml.client.signature` on
(`scripts/saml-client-signature.py`, Keycloak admin REST) and re-runs the login, so the
`<AuthnRequest>` signature is checked by the IdP for real. `make cli-token`
(`scripts/jwt-token.py`) logs in over SAML and prints the JWT `/restapi/user/token` issues, which
is what makes the CLI criterion checkable by hand now that `make cli-test` is unrunnable.
`scripts/saml-login-test.py` gained the logout leg of the profile and a `SAML_KEEP_SESSION`
escape hatch for `jwt-token.py`.

**Externally visible divergences, for the release notes.** Everything an existing deployment
would notice, in one place:

| Behaviour | Before (OpenSAML 2 extension) | Now |
|---|---|---|
| `GET /saml/logout` | global logout: `<LogoutRequest>` to the IdP, both sessions dropped | **local only** — clears the NGB session and lands on `/`; the IdP session survives, so the next request silently re-authenticates. `POST /saml/logout` does the single logout. The bundled client was changed to POST (`client/client/utils/saml-logout.js`); any other caller of that URL has to be too |
| `/saml/logout?local=true` | skipped the IdP round trip | not ported — a `GET` is now exactly that |
| `/saml/login` | SP-internal AuthnRequest endpoint | `/saml/login/{registrationId}` = `/saml/login/ngb`. Not in any metadata; matters only to a deployment that bookmarked it |
| Post-login redirect | the saved URL | the saved URL with `?continue` appended — Spring Security 6's `HttpSessionRequestCache` marks the replayed request with `matchingRequestParameterName=continue` by default. Cosmetic, and left alone rather than switched off, because switching it off re-opens the redirect loop it exists to prevent |
| Logout landing page | the SP's own `/` | `/`, but only because `logout().logoutSuccessUrl("/")` says so: `Saml2LogoutConfigurer.configure()` copies the plain `LogoutConfigurer`'s success handler onto `Saml2LogoutResponseFilter`, and the Spring default is `/login?logout`, which NGB has no page for |
| SP metadata signing key | the key store's default key, i.e. the **HTTPS** key (`server.ssl.keyAlias`), while messages were signed with `saml.sign.key` | both are `saml.sign.key`. Spring Security keeps one credential list for signing and for advertising; the old split meant published metadata named a key NGB never signed with. An IdP configured from NGB's metadata now gets the right certificate; one configured by hand needs no change |
| `/saml/SSOHoK`, `/saml/discovery`, `/saml/web/**` | mapped | gone. One IdP, no Holder-of-Key support |
| `saml.lb.*`, `saml.validate.url.without.scheme` | honoured | ignored. Endpoint locations are absolute values built from `saml.base.url`; `server.forward-headers-strategy` covers reverse proxies |

`LOGOUT_RESPONSE_SKEW` (120 s) has nowhere to go either — `OpenSaml4LogoutResponseValidator` does
not time-validate logout responses at all — while the assertion skew (1200 s) and
`saml.authn.max.authentication.age` are both honoured, the latter by an extra validator chained
onto the default one, since Spring Security has no equivalent check.

**Exit criteria, as actually run.** Every number below was measured on this branch, not predicted:

| Criterion | Command | Result |
|---|---|---|
| Build and lint clean | `make jar`, `make lint` | BUILD SUCCESSFUL. One new PMD violation appeared and was fixed rather than suppressed: `AvoidFieldNameMatchingMethodName` on `SAMLSecurityConfiguration`'s `authnRequestBinding` field, so the accessor is now `singleSignOnServiceBinding()` |
| H2 unit tests | `make test` | **526 tests, 3 failed, 21 skipped** — 519 + the 7 restored security tests, and the 3 are the documented live-data failures (`GffManagerTest.testLoadGenesTranscript`, two `BlatSearchManagerTest`) |
| PostgreSQL unit tests | `make reset-pg && make test-pg` | **526 tests, 11 failed, 21 skipped** — the 9 documented PG failures plus the same 2 BLAT ones. `PdbDataManagerTest` passed on both flavours |
| SAML login, admin | `make smoke-saml U=ngbadmin@ngb.dev.local P=admin` | whole web SSO profile plus single logout; authorities `['ROLE_USER', 'ROLE_ADMIN', 'NGB_ADMINS']` |
| SAML login, plain user | `make smoke-saml U=ngbuser@ngb.dev.local P=user` | same, authorities `['ROLE_USER', 'NGB_USERS']` |
| Signing path exercised | `make saml-verify-signing` | SP metadata signature validates under the JDK's XML-DSIG, and the login above was then re-run with Keycloak's `saml.client.signature = true`, i.e. with the IdP actually checking the `<AuthnRequest>` signature. Confirmed set on the client through the admin API before the run — a `make up-saml` re-imports the realm and turns it back off, so this has to be re-flipped each time |
| JWT / CLI | `make cli-token` then `ngb set_token … && ngb list_ref` | 648-character RS512 token; `list_ref` returned both dev references. This stands in for `make cli-test`, which cannot run (dead `cli-test` host) |
| No-auth mode still works | `make up` then `make smoke` | http 200, https not listening, version 2.8.0 |

**Browser verification, and the one thing the dev environment gets in the way of.** Driven through
a real Chrome over the DevTools protocol (throwaway script, not committed), against
`https://ngb.dev.local:8443/catgenome/`, for both users:

- unauthenticated visit → `/saml/login/ngb` → Keycloak's login form; credentials typed into
  Keycloak's own page → back on `…/catgenome/?continue#/`, title `NGB`, `<ngb-main-toolbar>`
  rendered, WebGL alive;
- `/restapi/user/current` over the browser session returns the expected authorities and
  `enabled: true`, and `/restapi/reference/loadAll` returns `dm6, test_ref`, so an ACL-enabled
  read works off a SAML principal;
- the toolbar's power button (`.logout-button`, `$ctrl.logout`) performs the profile the client
  change was for: `POST /catgenome/saml/logout` → 200 auto-submit page → `POST` `<LogoutRequest>`
  to the IdP → Keycloak `POST`s the `<LogoutResponse>` to `/catgenome/saml/SingleLogout` → `/` →
  `/saml/login/ngb` → the IdP **asks for credentials again**, so both sessions are gone.

The obstacle is worth writing down because the next person to click Logout in a browser against
the dev stack will hit it: the dev IdP is plain HTTP (`http://idp.dev.local:8081`) while NGB is
HTTPS, so the auto-submitted `<LogoutRequest>` form is a mixed-content POST and Chrome kills it —
`net::ERR_BLOCKED_BY_CLIENT`, with `Mixed Content: … contains a form that targets an insecure
endpoint` in the console, and the tab lands on `chrome-error://chromewebdata/`. Nothing to do with
NGB: login is unaffected because those hops are top-level GET redirects (and the assertion POST is
http → https, an upgrade). Launching Chrome with `--allow-running-insecure-content
--unsafely-treat-insecure-origin-as-secure=http://idp.dev.local:8081` makes the hop go through and
the profile completes as above. A production deployment, IdP on HTTPS, never sees it.

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

### Phase 5 execution findings

Written as they were established, before implementation. Everything below was measured, not
inferred — the harness that produced it is `.devenv/fixtures/probe/` (gitignored scratch;
see `.devenv/fixtures/README.md`).

#### Versions the Boot 3.5.16 BOM manages (resolved)

Read out of `spring-boot-dependencies-3.5.16.pom`:

| Artifact | Pinned at (pre-Phase-5) | BOM-managed |
|---|---|---|
| `org.flywaydb:flyway-core` | 3.2.1 | **11.7.2** |
| `com.h2database:h2` | 1.3.176 | **2.3.232** |
| `org.postgresql:postgresql` | 9.4-1206-jdbc4 | **42.7.11** |
| `com.zaxxer:HikariCP` | (already BOM) | 6.3.3 — done in Phase 3, not this phase |

**Flyway 11.7.2, not a pinned 10.x.** Nothing in the upgrade needs 10.x semantics, and taking
the BOM version means `spring.flyway.*` and the Boot auto-configuration agree with each other
by construction. `flyway-database-postgresql:11.7.2` is in the BOM and must be added
explicitly. **There is no `flyway-database-h2`** — H2 support is still inside `flyway-core`,
so the "same check for H2" in task 2 resolves to "nothing to add".

#### VERIFY (task 3): does Flyway 11 parse the legacy version format identically? — **Yes**

This was the item that could have invalidated the whole phase, so it was settled first, two
ways, and against a database migrated by the *old* code rather than a fresh one.

1. **Parser-to-parser.** `old/ParseOld.java` calls Flyway 3.2.1's own
   `MigrationInfoHelper.extractVersionAndDescription(name, "v", "__", ".sql")`;
   `new/ParseNew.java` calls Flyway 11.7.2's own
   `new ResourceNameParser(new FluentConfiguration().sqlMigrationPrefix("v")).parse(name)`.
   Output is `<filename>|<version>|<description>`. For all **118** scripts (59 h2 + 59
   postgres) the two runs are **byte-identical**.
   `v2016.11.21_17.58__Database_create_script.sql` → version `2016.11.21.17.58`, description
   `Database create script` — matching the rows actually present in both real databases.
2. **Against real old-code databases.** Flyway 11.7.2 `info` reports **60 entries,
   applied=60, notApplied=0** on both the restored PostgreSQL 9.6 dump and the
   export/imported H2 1.3 file. So applied-migration history does **not** break, and
   `validateMigrationNaming`/`MigrationVersion` need no special handling.

**Conclusion: the phase's approach is valid.** No need to stop.

#### Two things that *do* break on an existing database (both proven, both fixable)

**(a) Every checksum changed between Flyway 3.2.1 and 11.7.2.** `validate` fails with **51 of
59** mismatches on PostgreSQL and the same on H2 — the checksum algorithm changed, the
scripts did not. `flyway.repair()` realigns every row on the Flyway-3-shaped table, after
which `validate` returns `validationSuccessful=true, invalid=0` and `migrate` reports
`migrationsExecuted=0`. So **repair is unavoidable on every existing database**, whatever
else Phase 5 does. This matters for decision 1 below.

**(b) The Flyway 3 schema-history table cannot be written to by Flyway 11, and `repair` does
not fix that.** Flyway 3 created `schema_version` with `version_rank INTEGER NOT NULL` (no
default) and the primary key on `version`; Flyway 4+ dropped `version_rank`, moved the PK to
`installed_rank` and made `version` nullable. `repair` only touches checksums. Adding one
synthetic new migration therefore dies in
`JdbcTableSchemaHistory.doAddAppliedMigration`:

```
PostgreSQL: ERROR: null value in column "version_rank" of relation "schema_version"
            violates not-null constraint
H2 2.3.232: NULL not allowed for column "version_rank"  [23502-232]
```

Both engines fail on the *first* new migration, so an operator sees it immediately rather
than silently — but the failure modes differ and the H2 one is worse:

- **PostgreSQL** rolls the whole migration back. History stays at 60 rows, no orphan objects.
- **H2** leaves the migration's DDL committed with **no history row for it**. Measured: the
  synthetic migration's `PROBE_NEW_MIGRATION` table survived in the schema after the insert
  failed. An operator who retries without cleaning up hits "object already exists".

⇒ **A one-time legacy schema-history conversion is required**, run before `migrate`. Design
adopted: a Boot `FlywayMigrationStrategy` bean that (i) detects the legacy shape by looking
for a `version_rank` column, (ii) if found, logs loudly, converts the table to the Flyway
10/11 shape and calls `flyway.repair()`, (iii) then calls `flyway.migrate()`. Conditional,
not unconditional — an unconditional `repair()` on every startup would permanently disable
the checksum safety net that is the reason to run Flyway at all.

#### H2 2.3.232: what the existing scripts and data actually need

**Reserved words — three, and no script edits needed.** H2 2.x promoted `USER`, `VALUE` and
`END` to reserved words, and the schema uses all three: table `CATGENOME.USER`, column
`VALUE` (`catgenome.metadata`), column `END` (`catgenome.session`). Renaming them would ripple
into the 42 `dao/*-dao.xml` files and the `.sql` history alike. Instead the JDBC URL carries
`NON_KEYWORDS=END,USER,VALUE`, which restores 1.x behaviour for exactly those three
identifiers and nothing else. That string has to appear everywhere an H2 URL does
(`.devenv/docker-compose.yml`, the three `profiles/h2/test-*.properties`, packaged defaults,
docs).

**The 1.3 → 2.x export/import works, with that one setting.** `SCRIPT TO` under 1.3.176
produced a 58,896-byte script; `RUNSCRIPT FROM` under 2.3.232 rejected it at
`CREATE CACHED TABLE CATGENOME.USER(` and then imported it **clean** once `NON_KEYWORDS` was
set — 59 CATGENOME tables and all 60 `schema_version` rows preserved.

**The 59 h2 scripts do *not* run on 2.x unedited.** A fresh-install run died on migration 1.
Iterating to green found **three classes of purely syntactic breakage, 10 occurrences in 4
files** — and nothing else:

| # | What 1.3 tolerated | 2.x requires | Where |
|---|---|---|---|
| 1 | trailing `,` before the closing `)` of `CREATE TABLE` | remove it | `v2016.11.21_17.58__Database_create_script.sql` (×2), `v2017.07.04_11.20__EPMCMBI-1810_short_url.sql`, `v2021.08.27_16.00__issue_534_session_sharing.sql` |
| 2 | `SEQ.nextVal` attribute form | `nextval('seq')` (function form, already used elsewhere in the same file set) or `NEXT VALUE FOR` | `v2017.02.16_18.00__EPMCMBI-1553_Create_ref_index.sql`, `v2018.12.03_12.00__default_admin.sql` |
| 3 | `CREATE SEQUENCE … START 1 INCREMENT 1` | `START WITH 1 INCREMENT BY 1` | `v2018.09.17_11.03__ACL_tables.sql` (×4) |

Notably **not** a problem, despite the plan's list: `IDENTITY` semantics, `MERGE` syntax,
implicit conversions, `information_schema` casing, and the `nextval('schema.seq')` *function*
form (which 2.x still supports). With those 10 edits, `migrate` from empty reports
`migrationsExecuted=59`.

**The edited fresh install and the export/imported upgrade produce the same schema.** This is
the safety check that makes editing the scripts defensible, so it was run: a full
`information_schema.columns` dump (table, ordinal, name, type, length, nullability, default)
of both databases diffs to **zero differences across all 58 application tables**. The only
lines that differ are the `schema_version` table's own shape (finding (b), expected) and the
synthetic probe table. So the three edits are semantics-preserving in fact, not just in
intention.

#### PostgreSQL 16.15: the operator gotcha

Restoring a 9.6 `pg_dumpall` into PostgreSQL 16 succeeds, and then **every connection fails**:

```
FATAL: password authentication failed for user "catgenome"
```

The dump carries md5 password hashes; PostgreSQL 16 ships `scram-sha-256` in the default
`pg_hba.conf` and will not authenticate an md5-hashed role against it. Fix is one statement
per role after restore (`ALTER ROLE catgenome WITH PASSWORD '…'`). This belongs in the
release note, not just here — it looks like a credentials bug and is not one.

#### Decision 1 — H2 2.x script strategy: **edit the 59 scripts. Not a squashed baseline.**

This **diverges from the plan's recommendation** (task 4 says "(b) is cleaner given the file
format already forces an export/import"), so the reasoning is recorded in full.

The plan's argument rests on the export/import making checksum breakage free. Measurement
shows the premise is true but the conclusion does not follow, for three reasons:

1. **"Editing breaks checksums" is no longer a cost.** Finding (a): all 59 checksums are
   already invalid on every existing database purely because the algorithm changed between
   Flyway 3 and 11, so `repair` runs regardless. Editing 4 of the 59 scripts adds *nothing*
   to the upgrade procedure. The main argument for the baseline evaporates.
2. **A squashed baseline makes validation permanently worse.** An existing database has 59
   applied rows. Replace the 59 scripts with one baseline and every one of those rows becomes
   a *missing* migration, so `validate` fails forever unless `ignoreMigrationPatterns` is
   loosened to `*:missing` in production config. Trading a working checksum check for a
   permanently muted one is the wrong direction for the phase with the highest data-loss
   risk.
3. **The edits are 10 lines of syntax; a baseline is ~700 lines of new, unreviewed DDL.** And
   it would need to be *two* baselines, because the flavours have diverged — and hand-writing
   the PostgreSQL one is pure downside, since PostgreSQL has no on-disk format change and no
   forced export/import to piggyback on. Any transcription error in a generated baseline is
   silent until a customer hits it. The 10 edits, by contrast, are verified equivalent by the
   schema diff above.

Secondary but real: keeping the 59 files keeps `git log` on the schema, and keeps the two
flavours diffable file-by-file — which is exactly what decision 2 needs.

#### Decision 2 — diverged-schema failures: **fixing all seven**

All seven PostgreSQL failures that `TEST-BASELINE.md` attributes to the two script sets
having drifted are fixed in this phase, none deferred:

| Divergence | Failures | Fix |
|---|---|---|
| `vcf.multi_sample` nullable in postgres, `NOT NULL DEFAULT FALSE` in h2 | 4 (`BookmarkDaoTest`, `VcfFileDaoTest`) | new postgres migration bringing it to the h2 definition |
| `task_organism.organism` typed differently | 2 (`BlastTaskDaoTest`) | new postgres migration aligning the type to h2 |
| predefined-role rows differ (ids/count) | 1 (`RoleDaoTest.testLoadRolesWithUsers`) | new migration aligning the postgres seed rows to h2 |

Done as **forward migrations on the postgres side**, not by editing applied scripts: h2 is
taken as the reference because the DAO layer and the Java entities were written against it,
and a new migration is the only form of fix that reaches databases that already exist. The
three syntactic H2 edits above are the sole exception to "do not edit applied scripts", and
they are justified by the schema-diff equivalence proof, not by convenience.

`TargetManagerTest.filterTargetsByOwnerTest` is out of this table — it is a genuine DAO bug
(`WHERE target_id IN ()` when the id list is empty), unrelated to persistence versions. If
fixed, it is a separate sub-commit.

#### Fixtures captured before anything destructive (and one corrected premise)

`.devenv/fixtures/pre-migration/` (gitignored, plus a copy outside the repo at
`~/ngb-phase5-fixtures/`) holds the two pre-migration databases the exit criteria need, taken
at `5680365b` with the containers cleanly stopped first:

- `h2/catgenome.h2.db` — H2 1.3.176, cleanly shut down. Verified genuine: 59 CATGENOME
  tables, `catgenome."schema_version"` with 60 rows and Flyway 3.2.1 checksums, and real
  registered data (`test_ref`, `dm6`, `demo_bam`, `demo_vcf`, `demo_genes`, `demo_vcf_dm6`).
- `pg/pg96-dumpall.sql` and `pg/pg96-datadir.tar.gz` — PostgreSQL 9.6.
- the matching `/opt/ngb/contents` trees for both.

**Correction to the prompt's premise:** the live `pg-data` volume did *not* contain a
populated `ngb` database — `make up-pg` had never been run against it, so only `ngb_test`
(left by `make test-pg`) had a Flyway history. The PostgreSQL fixture is therefore one that
was *built*: `make jar-pg-fast` at HEAD (still Flyway 3.2.1), `make up-pg`, then registering
`test_ref`, `dm6`, `demo_bam`, `demo_vcf_dm6` and dataset `demo_ds` through the CLI, then
capturing. It is a real old-code database; it just is not the one that was sitting on disk.

Incidental H2 facts worth not rediscovering: Flyway 3 created the history table as **quoted
lowercase** `CATGENOME."schema_version"` with quoted lowercase columns, so
`select version from catgenome.schema_version` does not resolve — it must be
`select "version" from catgenome."schema_version"`. And the `user` table is uppercase `USER`.

#### Phase 4's exit criteria, re-measured before starting

All four actually run at `5680365b`, against a stashed (clean) tree for the SAML one:

| Criterion | Baseline | Measured | |
|---|---|---|---|
| `make lint` | green | green | ✅ |
| `make test` | 526 / 3 / 21 | 526 / **4** / 21 | ✅ extra is `PdbDataManagerTest.testParse` |
| `make reset-pg && make test-pg` | 526 / 11 / 21 | 526 / **12** / 21 | ✅ same, all 9 documented PG failures present |
| `make up-saml && make smoke-saml` | green, both users | admin recognised as admin, plain user plain, SLO both | ✅ |

`PdbDataManagerTest.testParse` is the live-data flapper `TEST-BASELINE.md` already says to
"expect either way", so both suites are at baseline. Two operating notes: `make up-saml`
returns before the server has finished booting, so `smoke-saml` straight after it fails with
`Connection refused` — wait for `Started Application` in the log; and the plain user needs
`make smoke-saml U=ngbuser@ngb.dev.local P=user`, the target only does the admin by default.

#### What the phase changed, file by file

Java and configuration:

- `server/catgenome/build.gradle` — the three pins deleted, so h2, postgresql and flyway-core
  take the BOM's 2.3.232 / 42.7.11 / 11.7.2; `flyway-database-postgresql` added.
- `dao/FlywayMigrator.java` (new) — replaces the `org.flywaydb.core.Flyway` bean, which Flyway
  10 made unusable from XML by removing the setter API, and carries the one-time schema-history
  conversion + `repair`, guarded on `version_rank` actually being present.
- the four `profiles/{h2,postgres}/{,test-}applicationContext-flyway.xml` — repointed at it.
- `conf/catgenome/applicationContext-database.xml` — Hikari `connectionInitSql` issuing
  `SET NON_KEYWORDS END,USER,VALUE`, SpEL-gated on the JDBC URL starting `jdbc:h2:`.

Migration scripts — 10 syntactic edits in 6 h2 files (not 4; the count in the H2-breakage
table above is of distinct *problems*, and `ACL_tables` carries four occurrences of one):

| File | Edit |
|---|---|
| `h2/v2016.11.21_17.58__Database_create_script.sql` | 2 trailing commas |
| `h2/v2017.07.04_11.20__EPMCMBI-1810_short_url.sql` | 1 trailing comma |
| `h2/v2021.08.27_16.00__issue_534_session_sharing.sql` | 1 trailing comma |
| `h2/v2017.02.16_18.00__EPMCMBI-1553_Create_ref_index.sql` | `TABLE.nextVal` → `nextval('seq')` |
| `h2/v2018.12.03_12.00__default_admin.sql` | `TABLE.nextval` → `nextval('seq')` |
| `h2/v2018.09.17_11.03__ACL_tables.sql` | 4 × `START 1 INCREMENT 1` → `START WITH 1 INCREMENT BY 1` |

The two test-only scripts (`src/test/resources/database/test/{h2,postgres}/`) were checked
against all three problem classes and need nothing.

Three new postgres-only convergence migrations, `v2026.08.21_12.{00,10,20}`, for
`multi_sample`, `organism` and the roles. The role one is the substantial one: it drops
`ROLE_CYTOBANDS_MANAGER` and `ROLE_MAF_MANAGER` with their grants, renumbers the survivors
onto `DefaultRoles`' ids carrying `USER_ROLE` rows with them, and adds the missing
`ROLE_WIG_MANAGER` at 8. Renumbering parks ids in a +1000 range first so no intermediate state
collides with the primary key, and drops/re-adds `user_role_role_id_fkey` for the duration —
all inside Flyway's transaction. Operator-created roles are untouched, `S_ROLE` starts at 100.
Every step is a no-op when already aligned, so a fresh install (which still runs the original
divergent seeding first) and an existing database converge to identical rows; that is why the
already-applied seed script was left alone rather than corrected in place.

Environment and documentation:

- `.devenv/.env`, `.env.example`, `docker-compose.yml` — `PG_VERSION=16`. The compose
  `${PG_VERSION:-9.6}` fallback stays 9.6 deliberately: it only fires for a `.env` predating
  this phase, which wants the old server.
- `.devenv/README.md` — services table, the "pinned to 9.6" gotcha, a new H2 2.x gotcha, the
  role-divergence note marked settled, and checkpoint 4 ticked.
- `docs/md/installation/database-upgrade.md` (new, in the mkdocs nav under Installation) — the
  operator procedure, which is where the release-note-shaped content lives rather than only
  here. Covers the H2 `Script`/`RunScript` round trip with `NON_KEYWORDS` on the *tool's* URL,
  the PostgreSQL `pg_dumpall`/restore, the md5→scram `ALTER ROLE` step, what the automatic
  schema-history conversion logs, and — the part with user-visible consequences — that
  `ROLE_MAF_MANAGER` holders lose that role.
- `docs/md/installation/standalone.md` — points at it.

One PMD violation came out of the new class (`"ALTER TABLE "` four times, `AvoidDuplicateLiterals`),
fixed with a constant. Checkstyle stays at its baseline 37 warnings.

#### Two H2 2.x behaviour changes the migration scripts did not reveal

The script-level work above was proved out by migrating and diffing schemas, which is why it
looked complete. It was not: the first `make test` on H2 2.3.232 came back **526 / 12 / 21**
against a baseline of 4, and the eight extra failures were two H2 behaviour changes in *query*
and *column* semantics rather than in DDL syntax. Both are genuine data-correctness bugs on
H2 2.x, neither is a test artefact, and neither would have been found by anything short of
running the suite.

**1. Row-value `IN` lists are mis-typed (5 failures).** `MetadataDao.getItems` builds

```sql
WHERE (entity_id, entity_class) IN ((1,'PROJECT'),(1,'GENE'))
```

H2 1.3.176 evaluated that correctly. H2 2.3.232 tries to convert `'PROJECT'` to the type of
the *first* column and fails the whole query with `Data conversion error converting
"PROJECT"` / `NumberFormatException` `[22018-232]`. Reproduced standalone on a two-column
table, so it is H2's optimiser (`Expression.optimizeCondition`), not anything about NGB's
schema. The fix is to spell the list as a `VALUES` subquery —
`IN (VALUES (1,'PROJECT'),(1,'GENE'))` — which H2 2.x, and PostgreSQL, both read as intended.
`metadata-dao.xml` holds the only row-value `IN` in the codebase; grep for
`\([a-z_]+ *, *[a-z_]+ *\) +IN +\(` confirms it. The four `ProjectManagerTest` /
`ProjectControllerTest` failures were this same query reached through the project tree.

**2. Bare `DECIMAL` means scale 0 (1 failure, and silent data loss).** `HEATMAP.MIN_CELL_VALUE`
and `MAX_CELL_VALUE` were declared `DECIMAL` with no precision or scale. H2 1.3 kept the
inserted value's own scale; H2 2.x reads the declaration as `NUMERIC(100000, 0)` and rounds
every value to a whole number — `HeatmapManagerTest.createHeatmapTest` got `0.0` where it
wanted `0.001273579`. PostgreSQL's unconstrained `NUMERIC` keeps the scale, which is why the
column had survived this long. `Heatmap.minCellValue` is a `Double` read with `rs.getDouble`,
so `DOUBLE PRECISION` is both the honest type and the fix, applied to **both** flavours by
`v2026.08.21_12.30`. `ALTER COLUMN ... SET DATA TYPE` is accepted by H2 2.x and PostgreSQL
alike, verified on both.

This one has a consequence for the upgrade path that no migration can fix: the 1.3 `SCRIPT`
dump writes the column back out as bare `DECIMAL`, so `RUNSCRIPT` into 2.x rounds the stored
values *at import time*, before NGB starts and before the migration runs. The operator
document therefore has a `sed` step between export and import. Those are the only two
`DECIMAL` columns in the h2 schema, so the substitution is exact.

**A fourth script-set divergence, found while chasing the above** and not in
`TEST-BASELINE.md`'s seven because no test catches it: `BAM_COVERAGE.COVERAGE` is `DOUBLE` on
h2 and `NUMERIC` on postgres, against a `Float` field. Converged to h2 by
`v2026.08.21_12.40`. Worth stating plainly: the seven test-visible divergences were the ones
that had a test; this phase's schema diff is what found the eighth.

#### Two things only a real server start caught

Both test suites were at baseline before either of these was known. Neither is reachable from
the unit tests, because those build plain Spring contexts from the XML and never go through
Boot auto-configuration or the shipped logging profile.

**1. Boot's `FlywayAutoConfiguration` collides with NGB's own bean.** `make reset-ngb-data &&
make up` started the container and then failed the context:

```
The bean 'flyway', defined in class path resource [org/springframework/boot/autoconfigure/
flyway/FlywayAutoConfiguration$FlywayConfiguration.class], could not be registered. A bean
with that name has already been defined in class path resource
[conf/catgenome/applicationContext-flyway.xml] and overriding is disabled.
```

The auto-configuration is `@ConditionalOnClass(Flyway.class)` and was inert against
flyway-core 3.2.1; with flyway-core 11 it activates and contributes a bean named `flyway`,
which is what `applicationContext-flyway.xml` calls NGB's `FlywayMigrator`. Fixed by adding
`FlywayAutoConfiguration.class` to `Application.java`'s `@SpringBootApplication(exclude = …)`.

Two fixes that look adequate and are not:

- *Rename NGB's bean.* The guard is `@ConditionalOnMissingBean(Flyway.class)` and
  `FlywayMigrator` is not a `Flyway`, so Boot would still build its own — with
  `flyway_schema_history` as the table and `classpath:db/migration` as the location. It would
  find no migrations and create a second, empty history table next to the real one.
- *`spring.flyway.enabled=false`.* Works, but puts something the application cannot tolerate
  being switched on into a property file an operator edits.

**2. The shipped log configuration discards `WARN`, so the schema-history conversion was
invisible.** `profiles/jar/log4j2.xml` and `profiles/release/log4j2.xml` put a
`ThresholdFilter level="ERROR"` on every appender they have. `FlywayMigrator`'s two conversion
notices are `WARN`, so on the first start after an upgrade they went nowhere — and this is a
one-time, irreversible conversion of an operator's schema history, i.e. exactly the event that
must leave a trace. `docs/md/installation/database-upgrade.md` told operators to look for lines
that could never appear.

Rather than weaken the document, the phase added a second console appender with a `WARN`
threshold and bound *only* `com.epam.catgenome.dao.FlywayMigrator` to it (`additivity="false"`,
plus the error appender so a future `ERROR` from it is still filed). Applied to the `jar`,
`release` and `staging` profiles; `dev`'s console has no threshold and already showed them. The
global ERROR-only policy is deliberate and is left alone. Verified against both flavours: the
two lines now appear on stdout, verbatim as the document quotes them, and a second start prints
nothing.

#### The upgrade path, as actually exercised

Both fixtures were run end-to-end against the built jar, following
`docs/md/installation/database-upgrade.md` literally rather than a paraphrase of it. Each step
of the document was executed as written; the results below are what corrected it.

**H2 1.3.176 → 2.3.232**, from `fixtures/pre-migration/h2/catgenome.h2.db` (a real
Flyway-3.2.1-migrated database with `dm6` and `test_ref` registered):

- `org.h2.tools.Script` with the 1.3.176 jar produced an 851-line dump; the documented `sed`
  matched exactly the 2 expected `_CELL_VALUE DECIMAL,` lines and nothing else.
- `RUNSCRIPT` with `NON_KEYWORDS=END,USER,VALUE` imported it clean.
- Before NGB started, the imported database was confirmed to still carry the genuine Flyway 3
  layout — `version_rank` first, `version NOT NULL` — with 60 applied rows. This matters: it
  is the conversion's actual input, not a reconstruction of it.
- First start logged the two conversion notices and left `installed_rank` first, `version`
  nullable, `version_rank` gone, **61 rows** (the 60 originals plus
  `v2026.08.21_12.30`), 0 failures.
- `reference/loadAll`, `project/loadMy`, `reference/3/loadChromosomes` and `dataitem/search`
  all return the pre-upgrade data. A restart logs nothing further.

**PostgreSQL 9.6.24 → 16.15**, from `fixtures/pre-migration/pg/pg96-dumpall.sql`:

- `psql -f` into a virgin 16.15 cluster: one error, `role "catgenome" already exists`, from the
  container having provisioned the role. Nothing else.
- The md5-vs-scram trap in step 4 of the document is real and was confirmed, not assumed. The
  dump's `ALTER ROLE … PASSWORD 'md5…'` overwrites the working password with a 9.6 md5 hash;
  `pg_authid` then reads `md5a…`, and a connection from another container fails with
  `password authentication failed for user "catgenome"` against the default
  `host all all all scram-sha-256`. After `ALTER ROLE catgenome WITH PASSWORD …` the hash reads
  `SCRAM-SHA-256$` and the connection succeeds. Local loopback keeps working throughout because
  the container's `pg_hba.conf` trusts `127.0.0.1`, which is exactly what makes this easy to
  miss.
- First start: notices logged, history converted, **65 rows** (60 originals plus the five new
  migrations), 0 failures; `reference/loadAll` and `project/loadMy` return the pre-upgrade data.

**The role renumbering was tested against grants, not just against role rows.** The captured
fixture only had `ROLE_ADMIN` and `ROLE_USER` granted, whose ids do not move, so the first run
left the risky half of `v2026.08.21_12.20` unproven. The path was redone with three seeded
users holding the roles that do move — `ROLE_SEG_MANAGER` (id 10), `ROLE_TARGET_MANAGER` (11)
and `ROLE_MAF_MANAGER` (9). After the upgrade:

| User held | Before | After |
|---|---|---|
| `ROLE_SEG_MANAGER` | 10 | 9 — grant followed |
| `ROLE_TARGET_MANAGER` | 11 | 10 — grant followed |
| `ROLE_MAF_MANAGER` | 9 | no roles — deleted with the role, as documented |

`user_role_role_id_fkey` is back in place afterwards. The two-phase `+1000` renumber is what
makes this work without violating the FK mid-statement.

#### Carry-overs that turned out to be moot

The three test JDBC URLs said to carry a stray trailing apostrophe do not. `profiles/h2/
test-catgenome.properties` reads `jdbc:h2:mem:test_catgenome;DB_CLOSE_ON_EXIT=FALSE` with no
apostrophe, and `src/test/resources/test-catgenome-acl.properties` and `-auth.properties` no
longer declare a datasource at all — `[migration 0] Give the ACL/auth tests a flavour-matched
datasource` moved it out. Nothing to fix; recorded so the next person does not go looking.

#### One optional fix taken

`TargetManagerTest.filterTargetsByOwnerTest`, the last non-network failure on PostgreSQL and a
pre-existing bug unrelated to the migration. `TargetManager.load(TargetQueryParams)` enriches
its results with two `IN (…)` queries built from the matched target ids; when the filter matches
nothing, both render as `target_id IN ()`. PostgreSQL rejects it. Fixed with an early return
when `targets` is empty — one guard covering both `targetGeneDao.loadTargetGenes` and
`getIdentifications`, rather than a guard in each DAO. Committed separately.

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

### Phase 6 execution findings

Written as the phase ran. Where reality differed from the plan text above, this section wins.

#### Decision: Lucene 9.12.3, not 10.x

Taken at the start of the phase, as the plan asks. Latest available at the time were **9.12.3**
(the terminal release of the 9 line) and **10.5.1**. Measured, not assumed — both jars pulled
from Maven Central and inspected in the builder:

| | 9.12.3 | 10.5.1 |
|---|---|---|
| `Version.MIN_SUPPORTED_MAJOR` | 8 | 9 |
| Class file version | 55 (Java 11), multi-release jar | 65 (**Java 21**) |
| `IndexReader.document(int)` | present (deprecated) + `storedFields()` | **removed** |

Reasoning:

1. **Neither can read Lucene 6**, so the operator cost — one full reindex — is identical. That
   is what D8 already commits us to and it is not a differentiator.
2. **10.x reads 9.x indexes** (`MIN_SUPPORTED_MAJOR = 9`). So 9.12.3 is not a dead end: a
   later 9→10 bump needs no second reindex, no second customer-facing upgrade step, and no
   second entry in the upgrade docs. Splitting the jump is therefore free in operator terms
   and cheaper in review terms.
3. The 6→9 delta is already three majors across 104 files. Lucene's own upgrade advice is one
   major at a time; taking four at once, on top of a tree that has just moved to Spring Boot 3
   and Java 21, spends risk budget for no user-visible gain.
4. 10.x removes `IndexReader.document(int)` (22 call sites in 11 files here) and reworks the
   `FacetsCollector` / `facet.sortedset` API that `FeatureIndexDao`'s grouping uses. All
   mechanical, all avoidable this phase.

The honest cost: 9.12.3 gets no further upstream fixes, since 9 is closed. Accepted — Lucene
here is an embedded index library, not a network-facing parser, and the alternative being
compared against is 6.6.0 from 2017.

**Follow-up recorded:** bump to Lucene 10.x after the migration. It needs no reindex; it needs
`storedFields()` at 22 sites, the facets rework, and Java 21 as a floor (which we have).

#### VERIFY resolved: the Spring Boot 3.5.16 BOM does not manage Lucene

`grep -i lucene spring-boot-dependencies-3.5.16.pom` → no matches. So `versionLucene` in
`server/catgenome/build.gradle` stays an explicit pin; this is one of the libraries Boot does
not know about.

Also worth knowing: `lucene-queries` and `lucene-sandbox` arrive transitively today (they are
not among the six declared artifacts) and are where `TermsQuery` and some facet internals
live. Both moved in 7.x/9.x, so the transitive arrival has to become explicit or the
call sites have to move to the core replacements.

#### The Lucene 6 fixture (captured before anything was changed)

`.devenv/fixtures/pre-migration/lucene6/` — the whole `/opt/ngb/contents` tree plus the
matching `catgenome.mv.db`, written by 6.6.0 at `6be43c24`. **This is the only Lucene 6 index
that will ever exist again**; Lucene 9 cannot write one. See `.devenv/fixtures/README.md` for
what is in it, the recipe, and how to restore it. Second copy at
`~/ngb-phase5-fixtures/lucene6-fixture-6be43c24.tar.gz`.

All seven index types are present. Confirmed Lucene 6 without a JVM: `taxonomy/segments_1`
declares `segments` format version 6, writer `06 06 00`, per-segment codec `Lucene62`.

Two reusable scripts came out of it, both of which the reindex procedure needs:

- `.devenv/scripts/build-global-indexes.sh` — stages the seven source dumps out of
  `server/catgenome/src/test/resources/` into `.devenv/data/ngs/index-sources/` (the ngb-h2
  container mounts only `./data/ngs:/ngs`, **not** `/workspace`) and issues the import calls.
- `.devenv/scripts/verify-lucene.sh` — 18 probes, one per Lucene read path, `/version` through
  `POST /target/identification`. All 18 green on 6.6.0; that run is saved as
  `verify-lucene-6.6.0.txt` and is the baseline the post-reindex run has to match.

#### VERIFY resolved: which rebuild paths exist today

Read out of the controllers, not assumed.

| Index | Rebuild trigger today |
|---|---|
| Feature index, VCF | `GET /vcf/{id}/index?createTabixIndex=` |
| Feature index, GFF/GTF | `GET /gene/{id}/index?full=&createTabixIndex=` |
| Feature index, BED | `GET /bed/{id}/index` |
| Feature index, SEG / MAF / WIG | n/a — **not a gap**, see below |
| Taxonomy | `PUT /taxonomy/upload?taxonomyFilePath=` |
| Homologene | `PUT /homologene/import?databasePath=` |
| NCBI | `PUT /externaldb/ncbi/genes/import?path=`, `.../genes/info/import?path=` |
| Targets | `PUT /target/import/{opentargets,dgidb,pharmGKB,ttd}` |
| Pathway | `POST /pathway` / `POST /biopax` — **registration, not reindex**: owns a database row |
| BAM coverage | `POST /bam/coverage` — same problem, owns a database row |

The CLI reaches the feature-index ones generically: `FileIndexingHandler` builds
`/{format}/{id}/index` from `item.getFormat().toString().toLowerCase()`, so `ngb index_file`
works for exactly the formats that have the endpoint.

`HomologManager` (`PUT /homolog/import`) has **no** index directory — it is database-backed,
despite sitting next to Homologene. It is not one of the seven.

`targets/ttd.*` never got built in the fixture: the repo ships no TTD fixture data.

**SEG / MAF / WIG have no feature index at all**, so the absent endpoint is not a gap. Only three
`makeIndexFor*` paths exist — `FeatureIndexManager.makeIndexForVcfReader`,
`makeIndexForBedReader` and `GeneRegisterer` — and `grep -rl 'lucene\|FeatureIndex' manager/seg
manager/maf manager/wig` returns nothing. `FileManager.determineFilePathFormat` maps SEG/MAF/WIG
to a directory only because `deleteFileFeatureIndex` and friends are written generically over
`FeatureFile`.

#### The two real gaps: pathway and BAM coverage

Both write their Lucene document only as a side effect of *registering* a database row
(`PathwayManager.createPathway` → `writeLucenePathwayIndex`, `BamCoverageManager.create` →
`writeCoverageIntervals`), and both delete it only as a side effect of deleting the row. So there
is no way to rebuild the index for rows that already exist: `DELETE` + `POST` loses the id, and
for pathways it also loses the description and species links. Closed in this phase with two new
endpoints, both idempotent and both able to run over every row or one:

- `PUT /restapi/pathway/index?pathwayId=` — re-reads each registered pathway's SBGN/BioPAX file
  from its stored `path`. Cheap.
- `PUT /restapi/bam/coverage/index?coverageId=` — re-scans the BAM with `SamLocusIterator`, the
  same work `POST /bam/coverage` does. **Long-running**; the fixture's single 100 bp-step coverage
  index is 41 MB.

#### The one index with no rebuild path, and why it stays that way

`targets/genes` and `targets/gene.fields` (`TargetGeneManager`, `TargetGeneFieldManager`) are not
an index over anything — they are the **only** copy of the gene table a user uploads with
`POST /target/{id}/genes/import` (Excel/CSV, custom columns). `TargetGeneManager.importData` and
`create` write Lucene documents and nothing else; `setIds` only draws ids from a database
sequence, and `TargetGeneDao.saveTargetGenes` is called from `TargetManager` (targets created with
inline genes) but never from `TargetGeneManager`. Losing the index loses the data.

It is left as a documented limitation rather than closed, on two grounds:

1. **No released version can be affected.** `TargetGeneManager` arrived in `fa54b6b8`
   (2024-01-16, "Upload target from Excel with custom columns"); the newest tag is `v2.7.1`,
   which predates it. `git tag --contains fa54b6b8` is empty. Only someone tracking `develop`
   can have such an index.
2. **Rescuing it would need a Lucene 6 reader**, i.e. a second artifact built against the old
   jars in a separate classloader (same `org.apache.lucene.*` packages, so they cannot coexist
   in one). That is disproportionate for an unreleased feature.

The recovery step — re-upload the spreadsheet — is documented in `docs/md/installation/`
alongside the rest of the procedure, and the guard names the directory like any other.

#### Measured: nothing can rewrite a Lucene 6 directory in place

The obvious hope was that a full rebuild could simply overwrite the stale directory, making the
operator procedure one API call. It cannot. Probed against the fixture with lucene-core 9.12.3:

| Operation on a Lucene 6 directory | Result |
|---|---|
| `SegmentInfos.readLatestCommit` | `IndexFormatTooOldException` |
| `DirectoryReader.open` | `IndexFormatTooOldException` |
| `new IndexWriter(d, cfg)` — `CREATE_OR_APPEND` | `IndexFormatTooOldException` |
| `new IndexWriter(d, cfg)` — **`OpenMode.CREATE`** | `IndexFormatTooOldException` |

Lucene 9's `IndexWriter` reads the existing commit even when told to create, so `OpenMode.CREATE`
is no escape. `FSDirectory.open` resolves to `MMapDirectory` on this JVM; a *missing* directory
gives `IndexNotFoundException` from the readers and succeeds for the writer.

Consequences, and what the code does about them:

- **Per-file feature indexes need no manual step.** All three reindex entry points already call
  `fileManager.deleteFileFeatureIndex(file)` before rebuilding (`VcfManager.reindexVcfFile`,
  `GffManager.reindexGeneFile`, `BedManager.reindexBedFile`), and that deletes the whole
  `index.luc` directory. So `GET /vcf/{id}/index` works on a Lucene 6 index as-is.
- **The global imports would have failed**, because every one of them opens the writer
  `CREATE_OR_APPEND` and *then* calls `writer.deleteAll()` — the constructor throws first. Fixed
  by routing exactly those five sites (`HomologeneManager:206`, `NCBIGeneInfoManager:102`,
  `TTDDrugAssociationManager:76`, `TaxonomyManager:160`, `AbstractIndexManager.importData:152`)
  through `LuceneIndexUtils.openWriterForRebuild`, which probes the commit point first and, if it
  is unreadable, discards the unreadable Lucene files before opening the writer.

  It has to probe rather than open-and-recover, which is how it was written first: an
  `IndexWriterConfig` is single-use — `IndexWriter`'s constructor claims it on its first
  statement, *before* it reads the commit — so a second attempt with the same config dies with
  `IllegalStateException: do not share IndexWriterConfig instances across IndexWriters` whatever
  the directory now looks like. The retry could never have worked. Nothing noticed during the
  live reindex because the documented procedure deletes the stale directories first, so the
  recovery path was never entered; `LuceneIndexUtilsTest` entered it and it failed immediately.
  A reminder that "the endpoint returned OK" is not the same as "the error path works".

  This is not a licence to delete data. A call site that says `open(CREATE_OR_APPEND)` followed
  by `deleteAll()` has already declared everything in that directory disposable — the discard
  throws away exactly what `deleteAll()` was about to. It only fires on
  `IndexFormatTooOld/TooNew`, only for files matching Lucene's own naming (`segments*`,
  `pending_segments*`, `_*`), never recursively, never on the directory itself, and it logs at
  WARN through an appender that is not filtered to ERROR. Anything else in the directory
  survives.

#### The guard, as built

Three new classes plus one message block, all named after what they do rather than after the
migration, because they outlive it — the next Lucene bump needs them unchanged.

| Where | What |
|---|---|
| `util/LuceneIndexUtils` | every `FSDirectory.open`, `DirectoryReader.open` and `new IndexWriter` in the tree now goes through here, so the version failure is translated in exactly one place |
| `app/LuceneIndexVersionCheck` | `InitializingBean` that walks the six configured global roots and throws if any index is stale |
| `app/LuceneIndexVersionFailureAnalyzer` | Spring Boot `FailureAnalyzer`, registered in `META-INF/spring.factories`, so the refusal prints as `APPLICATION FAILED TO START` + `Description:` with **no stack trace** |
| `exception/LuceneIndexVersionException` | carries the directory, so `ExceptionHandlerAdvice` can report the per-file case through the API |

Decisions inside it worth knowing:

- **A root is walked, not probed.** `targets.index.directory` and `ncbi.index.directory` are
  parents of 15 and 2 leaf indexes; the other four *are* the index. One walk to depth 2, treating
  any directory holding a `segments*` file as an index, covers both shapes, skips the BioPAX and
  per-target data directories that share those roots, and needs no edit when a leaf is added.
- **`IndexNotFoundException` is deliberately not wrapped.** An absent or empty index directory is
  a normal state that several managers already catch and turn into an empty result. Wrapping it
  would turn a fresh installation into a startup failure.
- **A directory that cannot be read at all** (permissions, broken mount) logs
  `warn.lucene.index.check.failed` and does not stop startup. That is not the stale-index case,
  and refusing to start over it would be a new failure mode rather than a guard.
- **The check fails startup; the per-file case does not.** Six global roots can be checked in
  milliseconds and a server whose taxonomy index cannot be opened is not serving anything useful
  from it. Feature indexes are per registered file — thousands on a real installation — so they
  are caught on first read instead, and the rest of NGB keeps working.
- **The rebuild hint lives in one place** (`LuceneIndexVersionCheck.rebuildHint`,
  `LuceneIndexUtils.featureIndexRebuildHint`) and says the same thing as the table in
  `docs/md/installation/lucene-reindex.md`. The per-file hint is derived from the path —
  `<base>/<ref>/<TYPE>/<id>/index.luc` — so it names the exact call, `GET /restapi/vcf/5/index`,
  with no database lookup from a context that may not have one.

`INFO` on the way through, because "nothing was printed" is not evidence a check ran:
`info.lucene.index.check.passed` (n directories under these roots are readable),
`info.lucene.index.check.empty` (the roots exist but hold no index — a new installation, **and the
state between deleting a stale index and rebuilding it**) and `info.lucene.index.check.skipped`
(nothing configured). The middle one was added after the first live run: the guard was reporting
"no index directories are configured" straight after a deletion, which is both wrong and alarming.

#### Log4j2: the appenders had to change, again

Same trap Phase 5 hit. Every appender in `profiles/{jar,release,staging}/log4j2.xml` filters at
ERROR, so the guard's INFO and WARN lines were invisible in the very situation they exist for.
`migration-stdout` (added in Phase 5) now also carries `com.epam.catgenome.app.LuceneIndexVersionCheck`
and `com.epam.catgenome.util.LuceneIndexUtils` at INFO. Without that the discard WARN — the one
line that says data was thrown away — went nowhere.

#### The defect the reindex found: `chr` was indexed twice, incompatibly

`PUT /restapi/bam/coverage/index` failed on the first document with

```
Inconsistency of field data structures across documents for field [chr] of doc [0].
index options: expected 'DOCS_AND_FREQS_AND_POSITIONS', but it has 'DOCS'.
```

`BamCoverageManager.write` added `chr` as a `TextField` *and* as a `SortedStringField`, and
`SortedStringField` extends `StringField` — so the same field name arrived with two different
`IndexOptions`. Lucene 6 merged them silently; Lucene 9 refuses the document. Fixed by keeping
the `TextField` (which the chromosome filter's `QueryParser` matches) and replacing the
`SortedStringField` with a plain `SortedDocValuesField` + `StoredField`, which is all the sort
actually needed. Nothing read the second term.

**This was not a reindex-only bug.** The same `write` runs from `POST /restapi/bam/coverage`, so
on the bumped Lucene no coverage track could be registered at all — a fresh installation, no
upgrade involved. It was invisible to `make test` (no test registers a coverage track) and to
`make up` (no coverage track is registered at boot); it took reindexing the fixture to hit it.
Third phase in a row where the real defect surfaced only under a real server against real data.

The generalisation, for Phase 7 onwards: **Lucene 9 requires every document to agree on a field's
`IndexOptions`, not just on its `DocValuesType` and point dimensions.** The audit for other
instances came back clean — `BigVcfDocumentBuilder`, `VcfDocumentBuilder` and `TargetGeneManager`
do switch field classes per runtime value type, but those pairs differ in `DocValuesType`, which
Lucene 6 already rejected, so they cannot have been reachable.

#### `--enable-native-access=ALL-UNNAMED`

Lucene 9's `MMapDirectory` uses the `java.lang.foreign` FFM API, so on JDK 21 (where it is still
a preview-adjacent restricted API) every start printed three `WARNING: A restricted method in
java.lang.foreign.Linker has been called` lines. Added to `.devenv/ngb/entrypoint.sh`. The
manifest attribute (`Enable-Native-Access: ALL-UNNAMED`) that would fix this without a flag only
exists from JDK 24, so a flag it is.

**Phase 9 must carry this into `docker/core/Dockerfile` and the generated start scripts**, or
every shipped launcher prints the warnings. Recorded in the Phase 9 task list.

`jdk.incubator.vector` was deliberately *not* added. It only accelerates vector search, which NGB
does not use, and `--add-modules jdk.incubator.vector` prints its own incubator warning.

#### Testing the guard without a checked-in fixture

`src/test/java/com/epam/catgenome/util/StaleLuceneIndex.java` writes a `segments_1` containing
nothing but a codec header declaring format version **6** — the value Lucene 6.6.0 wrote. Every
path into the guard reaches the failure through `SegmentInfos`, which reads that header before
anything else, so the synthetic directory fails in the same place, with the same exception, as
the 21 MB fixture. Verified against the real thing afterwards (below).

That keeps the promise the phase brief made: no binary Lucene index in the test tree. A committed
one would have been invalidated by this very phase, and the next bump would leave a fixture
nobody could regenerate.

14 tests in two classes: `LuceneIndexVersionCheckTest` (7) on the startup half — refusal text,
count, per-leaf rebuild call, and the four states that must *not* fail startup: unconfigured,
absent, index-free, current — and `LuceneIndexUtilsTest` (7) on the lazy half plus the rebuild
writer and the exact hit count. They assert on the message text, since the message is the
deliverable.

`HomologeneManagerTest` needed no change: Phase 3 already had it build the taxonomy index in
`@Before`, which is the pattern the plan asked for.

#### The suite carries Lucene indexes over from the previous run — including the previous *Lucene*

The first full `make test` after the upgrade came back **540 / 13 / 21** against a baseline of 3.
Nine of the ten extra failures were `LuceneIndexVersionException` caused by
`IndexFormatTooOldException`, and none of them were the code's doing: the tests write their global
Lucene indexes into the *working tree* and never clean up, so the directories written by the
pre-upgrade runs of 2026-08-20 were still there, still Lucene 6.

| Directory (relative to `server/catgenome/`) | Failures |
|---|---|
| `${pathway.index.directory}` | `PathwayManagerTest` ×6 |
| `${bam.coverage.index.directory}` | `BamCoverageManagerTest.testCreateCoverage`, `testSearchCoverage` |
| `contents/ncbi/gene.ids` | `HomologeneManagerTest.searchTest` |

Yes, those directory names are literal. `profiles/{h2,postgres}/test-catgenome.properties` define
`taxonomy`, `targets` and `ncbi` but not `pathway`, `homologene` or `bam.coverage` — the `jar`
profile defines all six — and `applicationContext-test.xml` sets
`ignore-unresolvable="true"` on its `<context:property-placeholder>`, so for the missing three the
*unexpanded* string becomes the path. `ncbi` missed the scratch directory for a different reason:
it was `./contents/ncbi/`, relative to the test JVM's working directory, which is
`server/catgenome/`, while `taxonomy` and `targets` use `@rootDirPath@` (the Gradle root, i.e. the
repo root). That predates this migration by years — the repo's `.gitignore:13` has carried
`/server/catgenome/${*` for as long as the pattern has existed. What Phase 6 changes is the
consequence: before the upgrade a stale index there was silently reusable, and now it is a hard
failure.

Fixed rather than only documented, because the phase's own exit criterion is a reproducible
`make test`: all four properties now resolve under `@rootDirPath@/contents/`, so
`rm -rf ../contents` is finally sufficient — which is what `TEST-BASELINE.md` has claimed for five
phases. Verified by re-running both suites and checking that nothing appears in the source tree:
six index roots under `contents/`, none beside it. Test resources only; the `jar`, `release`,
`staging` and `dev` profiles are untouched.

Deleting the stale directories is likewise fixing the cause, not re-baselining: they are gitignored
scratch holding data written by a Lucene that no longer exists. Archived to
`.devenv/fixtures/pre-migration/lucene6/test-tree-lucene6-dirs.tgz` first, since they are the
test-side half of the last Lucene 6 indexes that will ever be written here, and recorded as a
one-time cleanup step in `TEST-BASELINE.md` for anyone arriving with an older working tree.

Two details worth keeping, because they show the guard behaving as designed rather than by luck.
`${homologene.index.directory}` and `contents/ncbi/gene.ids` came out of that same run at format
version **10** — Lucene 9 rewrote them, because those managers open their writer through
`openWriterForRebuild`, which discarded the unreadable files and rebuilt. The pathway and coverage
indexes stayed at 6 because those two are incremental writers, which must *not* discard. And
`HomologeneManagerTest` failed on `gene.ids` in the same run that repaired it: test ordering — it
read before `NCBIGeneIdsManagerTest` wrote.

#### Exit criteria, as run

| Criterion | Result |
|---|---|
| `make lint` | green — pmd clean, checkstyle **37 warnings in 15 files, 0 errors**, i.e. unchanged from Phase 5, and none of them in the new code. PMD did fail first, on 18 `UnnecessaryImport` violations left by the `LuceneIndexUtils` routing (`FSDirectory` / `Paths` no longer referenced in 13 managers); removed |
| `make test` | 540 tests, **3 failed**, 21 skipped — the 14 new tests, and only the documented network failures. Run twice: once at 3 (`BlatSearchManagerTest` ×2, `GffManagerTest`) and once at 4, the extra being `PdbDataManagerTest.testParse`, which the baseline records as flapping either way |
| `make reset-pg && make test-pg` | 540 tests, **3 failed**, 21 skipped — one *better* than the recorded baseline of 4, `PdbDataManagerTest` having flapped green. The two flavours now have the identical three failures, all network |
| Guard message, global index | `APPLICATION FAILED TO START`, 14 directories listed with a rebuild call each, 0 stack-trace lines in the whole start |
| Guard message, per-file index | `{"status":"ERROR"}` naming `/opt/ngb/contents/42/genes/1/index.luc` → `GET /restapi/gene/1/index?full=true`; server kept serving. Re-run on the committed tree for a VCF as well (`42/VCF/1/index.luc` → `GET /restapi/vcf/1/index`, `ngb index_file 1`), and that capture is the one in the documentation |
| Documented reindex on the fixture | 3 per-file + 6 global imports + the 2 new endpoints, then `Lucene index version check passed: 14 …` |
| Post-reindex behaviour | `verify-lucene.sh` **byte-identical** to the 6.6.0 baseline, saved as `verify-lucene-9.12.3-after-reindex.txt` |
| Procedure documented | `docs/md/installation/lucene-reindex.md`, in `mkdocs.yml`, cross-linked from `database-upgrade.md` and `standalone.md` |

Both guard messages ended in `Full procedure: https://epam.github.io/NGB/installation/lucene-reindex/`
until the last review pass, which is a URL nobody has ever published: `docs/mkdocs.yml` sets
`use_directory_urls: false`, so the built site has `.html` pages, and the only published-docs link
in the repo (`README.md:24`) points at `ngb.opensource.epam.com`, the host that stopped resolving
(see `TEST-BASELINE.md`, `make cli-test`). Now `Full procedure:
docs/md/installation/lucene-reindex.md`, which is how Phase 5 refers to its own procedure from
`FlywayMigrator:181`. Both transcripts in the documentation were re-captured from a running server
afterwards rather than edited, and the sweep re-run: still byte-identical to the 6.6.0 baseline.

One extra live check, after `openWriterForRebuild` was fixed: the **genuine** Lucene 6 taxonomy
index was copied back over the running server's `contents/taxonomy`, read (guard message, no
stack trace), then re-imported *without deleting anything* — `Discarded 4 unreadable Lucene
file(s) … The directory itself and any non-Lucene file in it were left alone`, `{"status":"OK"}`,
and the taxonomy read correctly afterwards. That is the recovery path the unit test exercises
synthetically, confirmed against the real format.

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
- `make test`, `make test-pg` at or better than the Phase 6 baseline. ~~Expect
  `GffManagerTest.testLoadGenesTranscript` to move here~~ — **this was wrong, corrected during
  execution.** The `protein_coding` vs `protein_coding_CDS_not_defined` expectation looks like a
  parser-upgrade casualty and is not one: Phase 0 note 3 established that the value comes from the
  live Ensembl REST response, and htsjdk cannot move it. Leave it red. If the *shape* of its
  failure changes in a way that implicates the parser, that is a finding — see the execution
  findings below, where the shape it did fail in turned out to be evidence the parser is fine.
- End-to-end, in the UI: BAM alignments (including a `.cram` if available), VCF, BED,
  BedGraph, WIG, GFF/GTF genes, GenePred, SEG, MAF, and a Tabix-indexed file. This phase can
  break file parsing in ways unit tests miss.
- A remote (S3 or HTTP URL) track still loads.

### Phase 7 execution findings

#### Decision: htsjdk **5.0.0**, not "latest 4.x"

The plan's table says "latest 4.x (requires Java 17+) — VERIFY latest at Phase 7". Measured
against `maven-metadata.xml` (not the Maven Central search API, which was stale and still
showed 4.3.0 as latest): the latest release is **5.0.0**, `lastUpdated 20260501201626`.

5.0.0 was taken. What it changes relative to 4.3.0, measured by diffing the two artifacts
against the 95 htsjdk types NGB imports:

- **Removed:** SRA support only — `htsjdk.samtools.SRA*`, `htsjdk.samtools.sra.*`, and the
  internal CRAM `rans` classes moved. NGB imports **none** of them.
- **Dependencies dropped:** `ngs-java` and `nashorn-core` (the latter is a supply-chain win —
  it was there for SRA). Added: `com.fulcrumgenomics:jlibdeflate:0.1.0`.
- Both 4.3.0 and 5.0.0 are class-file major 61, i.e. a Java 17 floor. Fine on 21.

**arm64 is clean either way**, which substantiates task 7 (drop the `snappy-java:1.0.3-rc3`
pin): jlibdeflate 0.1.0 ships `native/linux-aarch64` and `native/osx-aarch64`, and
snappy-java 1.1.10.5 — the version htsjdk 5 manages — ships `native/Linux/aarch64`. The
README's arm64 caveat about snappy can go.

Fallback if 5.0.0 misbehaves: 4.3.0, same API for everything NGB uses.

#### VERIFY resolved: the `URLHelper` SPI survives, but its **registration API is gone**

The plan (and the phase prompt) asked whether `URLHelper`/`HTTPHelper`/`FTPHelper` and
`ParsingUtils.registerHelperClass` survive, or were replaced by NIO filesystem providers.
Measured against the 5.0.0 sources:

- `htsjdk.tribble.util.URLHelper`, `HTTPHelper`, `FTPHelper` — **all still present**, not
  deprecated. So `EnhancedUrlHelper` and its 403-tolerant `S3Helper` compile and behave
  unchanged. The pre-signed-S3-URL path is safe.
- `ParsingUtils.registerHelperClass(Class)` — **removed.** It is replaced by
  `ParsingUtils.setURLHelperFactory(URLHelperFactory)`, where `URLHelperFactory` is a
  one-method interface `URLHelper getHelper(URL)`. The stock default is
  `RemoteURLHelper::new`.

Consequence: the registration must be reimplemented as a `URLHelperFactory`, and it has to
move — today the only `registerHelperClass` call is inside
`AbstractEnhancedFeatureReader.getFeatureReader` (line 92), a file this phase deletes, and it
re-registers on *every* reader open.

#### VERIFY resolved: `ISeekableStreamFactory` survives, and the new method is a `default`

`htsjdk.samtools.seekablestream.ISeekableStreamFactory` and
`SeekableStreamFactory.setInstance` both still exist. 5.0.0 adds a fifth method,
`getStreamFor(String path, Function<SeekableByteChannel, SeekableByteChannel> wrapper)`, but
it is a **`default`** method that delegates to `getStreamFor(String)` when the wrapper is
null and throws otherwise. NGB never passes a wrapper, so `NgbSeekableStreamFactory` compiles
and behaves unchanged and keeps routing `s3://`, `sws://` and `az://` to
`S3SeekableStreamFactory` / `AzureSeekableStreamFactory`.

#### What the fork actually customised (diffed against stock htsjdk 2.2.4 sources)

Seven things, only the first of which the plan anticipated:

1. The `EhCacheBasedIndexCache indexCache` parameter threaded through all four
   `getFeatureReader` overloads and both reader constructors, plus a `TribbleIndexCache` inner
   holder (index + `FeatureCodecHeader` + codec), `retrieveIndex(...)` and a cached
   `readHeader()`. This is D10's cache; it goes.
2. **`ParsingUtils.openInputStream` → `com.epam.catgenome.util.IOHelper.openStream` and
   `ParsingUtils.resourceExists` → `IOHelper.resourceExists`.** Not cache plumbing — see the
   next section.
3. **`IndexFactory.loadIndex` → `com.epam.catgenome.util.IndexUtils.loadIndex`.** Same
   reason.
4. Generic parameter renamed `SOURCE` → `S`; `codec` made non-final.
5. Magic numbers extracted to constants (`BUFFERED_STREAM_SIZE=512000`,
   `POSITIONAL_BUFFERED_STREAM_SIZE=1000`, `MAX_BUFFER_SIZE=100000000`,
   `MIN_BUFFER_SIZE=2000000`). The `advanceBlock()` expression is semantically identical to
   stock; no behaviour change.
6. `catch (Exception)` narrowed to `catch (IOException)` in places.
7. **`TribbleIndexedFeatureReader` imports `org.testng.Assert`** and carries a dead
   tautology in `WFIterator` — `long skippedBytes = pbs.skip(header.getHeaderEnd()); if
   (skippedBytes == 0) { Assert.assertEquals(skippedBytes, 0); }`. A test library leaking into
   main source since 2016. It goes with the fork.

#### The plan's assumption was wrong: the fork also carries **cloud-scheme** support

The plan's delete list assumes the fork's only customisation is the index cache, so the eight
files can be replaced by stock types. That is not so. Items 2 and 3 above exist because
`IOHelper` and `IndexUtils` understand three schemes htsjdk does not:

```java
// IOHelper.openStream / resourceExists
if (S3Client.isS3Source(path))          -> S3Client        (s3://, sws://)
else if (AzureBlobClient.isAzSource(path)) -> AzureBlobClient (az://)
else                                     -> ParsingUtils
```

These are first-class NGB resource types (`BiologicalDataItemResourceType.S3` / `.AZ`),
reachable from the BED, VCF and GFF managers, and `BedManager` explicitly supports
auto-indexing and histogram building for them.

What happens on stock htsjdk 5, path by path:

| Read path | Stock 5.0.0 uses | Cloud schemes? |
|---|---|---|
| `TabixFeatureReader` (data + header) | `SeekableStreamFactory` | **works** — hooked by `NgbSeekableStreamFactory` |
| `TabixReader` (tabix index + data) | `SeekableStreamFactory` only | **works** |
| `TribbleIndexedFeatureReader.query(...)` | `SeekableStreamFactory` | **works** |
| `AbstractFeatureReader.isTabix(...)` | `ParsingUtils.resourceExists` | fixable — `setComponentMethods` hook |
| `TribbleIndexedFeatureReader.readHeader()` | `ParsingUtils.openInputStream` | **breaks** |
| `TribbleIndexedFeatureReader.WFIterator` | `ParsingUtils.openInputStream` | **breaks** |
| `TribbleIndexedFeatureReader.loadIndex()` | `ParsingUtils.resourceExists` + `IndexFactory.loadIndex` | **breaks** (auto-discovered index only) |

`ParsingUtils.openInputStream` in htsjdk ≥ 4 is no longer scheme-agnostic:

```java
final IOPath path = new HtsPath(uri);
if (path.hasFileSystemProvider()) { ...NIO... }
else if (SeekableStreamFactory.canBeHandledByLegacyUrlSupport(uri)) { ...URLHelper... }
else throw new IOException("No FileSystemProvider available to handle path: " + ...);
```

and `canBeHandledByLegacyUrlSupport` is gated on a hardcoded, private
`Set.of("http", "https", "ftp")`. So the only extension point for a new scheme is an
installed `java.nio.file.spi.FileSystemProvider`.

**NGB cannot install one.** `FileSystemProvider.installedProviders()` loads services from
`ClassLoader.getSystemClassLoader()`. NGB ships as a Spring Boot fat jar, whose classes live
in `BOOT-INF/` and are loaded by `LaunchedClassLoader`, which the system class loader cannot
see; there is also no public API to add a provider at runtime. A `META-INF/services` entry in
NGB's own jar would simply never be read.

**Resolution taken** — stated here rather than chosen quietly, because it narrows a shipped
capability:

- Delete all eight fork files, as the exit criterion requires.
- Preserve everything htsjdk still offers a hook for:
  - `ParsingUtils.setURLHelperFactory(...)` for `EnhancedUrlHelper` (403 tolerance on
    pre-signed S3 URLs), registered **once** at startup instead of per reader open.
  - `AbstractFeatureReader.setComponentMethods(...)` — a public, non-deprecated static hook —
    with an NGB `ComponentMethods` whose `isTabix` uses `IOHelper.resourceExists`, so tabix
    detection still works for `s3://` / `sws://` / `az://`.
  - `SeekableStreamFactory.setInstance(NgbSeekableStreamFactory)`, unchanged.
  - ~~Pre-load cloud indexes with NGB's own `IndexUtils.loadIndex` and hand htsjdk the
    `Index` object where a call site can, rather than letting `IndexFactory` open the path.~~
    **Dropped — it buys nothing.** Measured on the 5.0.0 bytecode:
    `TribbleIndexedFeatureReader(String, FeatureCodec, Index)` delegates to the
    `(String, FeatureCodec, boolean)` constructor, which calls `readHeader()` before the caller's
    `Index` is even assigned — and `readHeader()` is one of the two `ParsingUtils.openInputStream`
    call sites. A plain cloud feature file therefore fails in the constructor whatever route the
    index took, so handing over a pre-loaded `Index` would add a code path that changes no
    outcome. The narrowing below is the whole of the answer.
- **Accept and document one narrowing:** a feature file on `s3://`, `sws://` or `az://` must
  now be **block-compressed and tabix-indexed**. A plain (non-bgzip) cloud feature file fails
  with `No FileSystemProvider available to handle path: s3://...` wrapped in
  `TribbleException.MalformedFeatureFile` — loud, not silent. Cloud bgzip+tabix files are
  fully supported, and are the only sensible way to serve a track from object storage anyway:
  the old path called `S3Client.loadFully`, i.e. it downloaded the **entire object** for every
  header read and every whole-file scan.
  Documented for operators in `docs/md/installation/standalone.md` (the AWS S3 section) and in
  `docs/md/cli/command-reference.md`, whose `reg_file` example registered a plain `.vcf` from
  `s3://` and no longer does. **Not exercised against real object storage** — there are no AWS or
  Azure credentials in this environment, so both the narrowing and the `s3://` bgzip+tabix path
  that still works are read off the 5.0.0 sources and bytecode, not measured. The `http` half of
  the same question *is* measured, against a staged 403 (below).

Three alternatives were considered and rejected; if this narrowing turns out to matter, they
are the options:

1. **An NIO `FileSystemProvider` for the cloud schemes** — the route htsjdk intends. Blocked
   by the fat-jar class-loading problem above unless NGB's launcher changes, and it is
   ~400 lines of new AWS/Azure-adjacent code, which Phase 8 fences off.
2. **Rewrite `s3://` to a pre-signed `https://` URL at reader-open time.** NGB already has
   `S3Client.generatePresignedUrl`, and `EnhancedUrlHelper.S3Helper` exists precisely to make
   such URLs work — so this would restore the capability in ~15 lines *and* replace whole-object
   downloads with ranged GETs. Rejected here because it changes how bytes are authenticated
   and fetched, has no Azure equivalent, and is a design decision, not a mechanical migration.
3. **Keep one minimal reader** — stock 5.0.0's `TribbleIndexedFeatureReader` with two lines
   swapped. Rejected: it re-forks, against D9, and against this phase's exit criterion that
   the package hold only `EnhancedUrlHelper`.

#### Task 6 resolved: IGV brings no htsjdk, and its codec still fits

`com.github.igvteam:igv:v2.6.3` was the phase's main dependency risk. Measured, not assumed:

- The published pom declares **zero dependencies**, and `igv-v2.6.3.jar` contains **zero
  `htsjdk/` entries**. There is no duplicate or shaded htsjdk to reconcile — no exclusion is
  needed, and reimplementing `UCSCGeneTableCodec` locally is off the table.
- `UCSCGeneTableCodec` → `UCSCCodec<T extends htsjdk.tribble.Feature> extends
  htsjdk.tribble.AsciiFeatureCodec<T>`, so it compiles against whatever htsjdk NGB resolves.
  Diffing `FeatureCodec`, `AbstractFeatureCodec` and `AsciiFeatureCodec` between 2.2.4 and
  5.0.0: the only changes are a type-parameter rename and two **`default`** methods
  (`getTabixFormat()`, `getPathToDataFile(String)`). A 2016-compiled subclass still satisfies
  the 5.0.0 interface, at compile time and at link time.
- Two side observations, neither blocking: the igv jar ships `log4j2.xml`, `log4j2_all.xml`
  and `log4j2_debug.xml` **at its root** (a possible log-config clash if anything ever
  classpath-scans for them), and `BasicFeature` holds a `private static
  org.apache.log4j.Logger` — log4j **1.x**. `log4j:log4j:1.2.17` is on the runtime classpath
  transitively today, so it resolves; the GenePred track verification confirms it at runtime.

The `org.jetbrains.bio:big` htsjdk exclusion still holds and is still needed — that one *does*
declare htsjdk, and the exclusion is what keeps a second version off the classpath.

#### Dependency graph after the bump, as resolved

`./gradlew :catgenome:dependencies` in the builder, runtime classpath:

- `com.github.samtools:htsjdk:5.0.0` — resolved once, no conflict arrows.
- `com.fulcrumgenomics:jlibdeflate:0.1.0` (new, htsjdk's), `org.xerial.snappy:snappy-java:1.1.10.5`
  (htsjdk-managed — the explicit `1.0.3-rc3` pin is **gone**), `org.apache.commons:commons-jexl:2.1.1`.
- `ngs-java` and `nashorn-core` are **off** the classpath: they came with SRA support, which
  5.0.0 dropped.
- `org.iq80.snappy:snappy:0.4` still appears from an unrelated branch of the tree (not htsjdk's).

Both new natives ship arm64 builds (`jlibdeflate`: `native/linux-aarch64`, `native/osx-aarch64`;
`snappy-java 1.1.10.5`: `native/Linux/aarch64`), which is what lets the `.devenv/README.md`
arm64 caveat about `snappy-java 1.0.3-rc3` go away.

#### Where the three SPI hooks ended up: `util/HtsjdkSpi`

The fork re-registered `EnhancedUrlHelper` on **every reader open**, from inside
`AbstractEnhancedFeatureReader.getFeatureReader` — a file this phase deletes. All three hooks now
install once, from a new `com.epam.catgenome.util.HtsjdkSpi`:

- `ParsingUtils.setURLHelperFactory(EnhancedUrlHelper::new)` — the 403 tolerance for pre-signed
  S3 URLs, unchanged in behaviour.
- `AbstractFeatureReader.setComponentMethods(new NgbComponentMethods())` — `isTabix` over
  `IOHelper.resourceExists`, so `s3://` / `sws://` / `az://` tabix detection survives. It differs
  from stock in that one call and nothing else: stock `IOUtil.hasBlockCompressedExtension(String)`
  already strips an http query string, so the fork's extra `?`-handling was redundant by 5.0.0.
- `SeekableStreamFactory.setInstance(NgbSeekableStreamFactory.getInstance())`.

Two placement details, both load-bearing:

- `install()` is **static and idempotent**, called from a `@PostConstruct`. The hooks are global
  JVM state, and several tests build more than one Spring context in a run.
- The class sits in `com.epam.catgenome.util`, not next to `Application`. `applicationContext-test.xml`
  scans `com.epam.catgenome.util` but not the `app` package, so a component declared beside
  `Application` would install the hooks in production and silently not in the unit suite — the
  worst of the two failure modes, because it makes the suite green on a path production does not
  take.

#### htsjdk 5.0.0 API deltas that surfaced while repointing the call sites

None of these are in the plan's list of "APIs that have moved"; they are what the compiler
actually objected to, after `javap` on the 5.0.0 jar in each case:

| Break | Fix |
|---|---|
| `IndexFactory.IndexType.getIndexType()` (the accessor for the index class) is gone | `IndexUtils.loadIndex` now calls `IndexType.getIndexType(stream).createIndex(stream)` and drops its reflection entirely — the new API is what the reflection was emulating |
| `IntervalTreeMap.{containsOverlapping, getOverlapping, containsContained, getContained}` widened their parameter from `Interval` to `Locatable`, so four `NggbIntervalTreeMap` methods stopped overriding anything | widened the four overrides to `Locatable` |
| `Index.write(File)` is now a `default` method that `throws IOException` | `FileManager.makeBedIndex` / `makeSegIndex` declare `throws IOException` |
| `SamReaderFactory.referenceSequence` has both `File` and `Path` overloads, so a bare `null` is ambiguous | `BamCoverageManager` passes `(File) null` |
| `org.testng` is no longer on the test classpath — it was a **transitive of htsjdk 2.2.4**, which is also how `org.testng.Assert` leaked into the fork's main source (finding 7 above) | `DiskBasedListTest` moved to `org.junit.Assert`, with all ten argument pairs swapped: TestNG is `(actual, expected)`, JUnit is `(expected, actual)` |
| `org.apache.commons.compress.utils.CountingInputStream` is gone | see below |

The commons-compress one was the only non-mechanical fix. htsjdk 5.0.0 brings
commons-compress **1.26.0**, which (a) removed that class and (b) is the first release to declare
a hard, compile-scope dependency on **commons-io 2.15.1** — which the explicit `commons-io:2.4`
pin in `build.gradle` was silently downgrading (`2.15.1 -> 2.4`, "selected by rule"). Left alone
that is a `NoClassDefFoundError` inside commons-compress on the CRAM bzip2/lzma paths, at runtime,
on a code path no unit test reaches. The pin is now **exactly 2.15.1**: 2.16.0 deprecates
`org.apache.commons.io.input.CountingInputStream`, which is what `FeatureSeekableStream` moved to.

`htsjdk.samtools.cram.io.CountingInputStream` was the obvious in-house replacement and was
rejected on inspection: it increments the counter *before* the `read()`, so EOF is counted as a
byte — which `FeatureSeekableStream.position()`/`eof()` are built on — and it wraps `IOException`
in `RuntimeIOException`.

Side effect worth knowing: commons-io's counter counts **skipped** bytes itself, so
`FeatureSeekableStream.CountingWithSkipInputStream` has nothing left to override. It survives as a
bare subclass only because `util/aws/S3SeekableStream` and `util/azure/AzureBlobSeekableStream`
construct it by name, and those are Phase 8's. It can collapse into its parent when they are next
touched.

#### Task 5 resolved: one more verbatim copy of htsjdk, `util/PositionalOutputStream`

The six utilities the plan lists, checked one at a time:

| File | Verdict |
|---|---|
| `util/PositionalOutputStream` | **a verbatim copy** — its own javadoc said "Copied from HTSJDK". Deleted; `GeneRegisterer` and `FileManager` now import `htsjdk.samtools.util.PositionalOutputStream`, which is API-identical (and `final`, which costs nothing — nothing subclassed it) |
| `util/BlockCompressedDataInputStream`, `util/BlockCompressedDataOutputStream` | genuinely *extend* htsjdk classes rather than copy them — keep |
| `util/FeatureSeekableStream` | NGB-specific (progress accounting over a seekable stream) — keep, with the commons-io change above |
| `util/FeatureInputStream` | NGB-specific — keep |
| `util/sort/**`, `util/motif/**` | public htsjdk API only, no internals duplicated — keep |

#### The index cache also backed **BAM indexes**, and its removal exposed a latent bug

D10 is written as if the cache held only Tribble/Tabix feature-file indexes. It did not:
`BamHelper` cached the raw index **byte arrays** for S3 and Azure BAMs, via a `BamIndex implements
IndexCache` inner class. That is the concrete shape of D10's performance risk — for a remote BAM
the index is now fetched on every request — and it is recorded in the fetchers' comments so the
cost is visible at the place that pays it.

Removing it surfaced a bug that the cache had been hiding: the **uncached** branch of the Azure
fetcher passed the `az://` path to `S3Client`, so it could only ever have failed. Any Azure BAM
whose index missed the cache — the first request after a restart, or any request with the cache
disabled — hit it. `fetchAZBamIndex` now uses `azureBlobClient.loadFully`. This is the second
time in this migration that a cache has been found masking a broken slow path.

#### The upgrade's one real regression: htsjdk's length probe became a `HEAD` request

Found by the live verification, not by the suite — exactly the failure mode the phase prompt
predicted. A VCF served from a URL whose `HEAD` is answered with 403 (i.e. a pre-signed S3 URL)
was refused with `TribbleException.MalformedFeatureFile: We never saw the required CHROM header
line`, and the server's request log showed **four `HEAD` → 403 and not one `GET`**.

The cause, from the 2.2.4 and 5.0.0 bytecode:

- `htsjdk.samtools.util.HttpUtils.getHeaderField` never set a request method in 2.2.4, so the
  default `GET` applied. Since 3.0 it sets `HEAD`.
- `SeekableHTTPStream`'s constructor measures the resource with it and, on any failure,
  records `contentLength = 0` without complaining. `read()` returns -1 as soon as
  `position >= contentLength`. So a 403 on `HEAD` is indistinguishable from an empty file: no
  request is ever issued for the data, and the codec reports the file as malformed.

`EnhancedUrlHelper` was **not** the problem — the file is byte-identical to before the upgrade
and still compiles. Its 403 tolerance only ever covered `exists()`; the data reads and the
length probe bypass the `URLHelper` SPI entirely and go straight to `SeekableHTTPStream`. Under
htsjdk 2.2.4 that did not matter, because the probe was a `GET`. Restoring parity therefore
needed more than keeping the helper:

| Site | Fix |
|---|---|
| the length probe itself | new `IOHelper.getContentLength(URL)` — a `GET`, disconnected as soon as the status line and headers are in hand, so no body is read |
| `EnhancedUrlHelper.S3Helper` | overrides `getContentLength()` to use it. Stock `HTTPHelper.getContentLength()` is HEAD-based and returns -1 on any non-200 |
| the seekable stream | new `util/UrlSeekableStream`, a `FeatureSeekableStream` over a `URLHelper`, reading through `openInputStreamForRange` (a ranged `GET`; **un-deprecated in 5.0.0**, it is how a `URLHelper` reads bytes). `SeekableHTTPStream.contentLength` is private and `HttpUtils.getHeaderField` is static with a hardcoded `HEAD`, so the stream had to be replaced rather than patched, and htsjdk 5 ships no alternative HTTP seekable stream |
| the routing | `NgbSeekableStreamFactory` sends URLs matching `EnhancedUrlHelper.isSignedS3Url` to `UrlSeekableStream`. **Every other `http(s)` URL stays on stock htsjdk** — the narrowest change that restores the behaviour |
| `manager/reference/io/FastaUtils` | second site of the same bug, and it would have been silent: it called `HttpUtils.getHeaderField` directly, and `FastaSequenceFile` clamps `endByte` to the length it reports, so a reference registered from a pre-signed URL would have returned `new byte[0]` for every sequence read. Now `IOHelper.getContentLength` |

Verified against a staged 403 (`.devenv/scripts/fake-remote-files.py`): the track now returns
the identical first variation as the same file read from disk, and the server log reads
`HEAD → 403`, then `GET → 200` for the length, then ranged `206`s for the data.

Noticed while tracing the call sites, and **left alone** because fixing it is a behaviour change
outside this phase: `NgbFileUtils.isRemotePath` (line 175) tests
`startsWith("http:") || startsWith("https:") || startsWith("ftsp:")` — `ftsp`, not `ftp`. So an
`ftp://` path has always been treated as a local file by every caller of that method, including
`FastaUtils.getContentLength(String)` and `FastaUtils.openBufferedReader`. One character, and it
means FTP references have never worked the way the docs imply.

#### D10's cost, measured

Task 4 asked for numbers. Measured on the running H2 dev server against a container serving
`.devenv/data/ngs` over HTTP on the `ngb-dev` network (`/tmp` script, not committed; the fake
server logs every request, which is where the byte counts come from).

Five identical `vcf/track/get` calls for the same 15 kb window of `CantonS.vcf.gz`
(141-byte `.tbi`), once over `http://` and once from local disk:

| | per request | index bytes per request |
|---|---|---|
| local file | 0.01–0.02 s | 0 (page cache) |
| remote `http://` | 0.23–0.26 s | 141 — the whole `.tbi`, re-fetched every time |

Three identical `bam/track/get` calls for a 100 bp window of the 24 MB test BAM
(87,824-byte `.bai`) over a URL whose `HEAD` is refused:

- the `.bai` is read **3 times per track load**, in full: **263,472 bytes per request**,
  every request, for ever. With the cache it was fetched once and then served from memory.
- the BAM itself: 2 ranged requests per load, ~4.2 MB actually delivered for a 100 bp window,
  because `UrlSeekableStream` (like `S3SeekableStream` before it) opens a range from the
  current offset to EOF and the reader closes it early. That is pre-existing NGB behaviour,
  not a D10 effect.

So the shape of the risk is: **the index cost is now linear in the number of track requests**,
and it is paid in full for `s3://` / `az://` too, where `BamHelper` used to cache the index
byte array (see the section above). 263 kB per request for an 88 kB index scales with index
size, not window size — a human WGS `.bai` is 5–10 MB, so the same panning session that cost
one index download now costs one per pan. Numbers recorded so a decision to reintroduce a
cache has a basis; nothing here is regressive relative to the *first* request, which is what
a cold cache always paid.

Task 4 also asked for a release-notes entry. `docs/md/release-notes/` holds one directory per
*released* version, the newest being 2.8.0, and there is no unreleased file to add to — inventing
a version number is not this phase's call. The operator-visible facts went where an operator will
actually look instead: the `server.index.cache.enabled` property is gone from
`docs/md/installation/standalone.md` (and from all six property files), the cloud bgzip+tabix
requirement is documented there and in the CLI reference, and the cost above is in
`.devenv/README.md`. Whoever cuts the next release should fold this and Phases 0–6 into its notes.

#### Live verification: every track type, through a running server

The suite exercises the managers with a Spring test context and never boots the server, so it
is not evidence that an instance still parses a BAM. `.devenv/scripts/verify-tracks.sh` is the
second pass, and is committed so it can be re-run: it registers each fixture over REST, asks
for a window, prints a real record from the response, and compares the pairs where a parser
regression would hide. `prepare-track-fixtures.sh` stages the fixtures into `/ngs/tracks`.

All eleven types answered with real data (`OK: every track type answered with data`):

| Track | What came back |
|---|---|
| BED, plain and bgzip+tabix | 20 blocks, first 35459–35461 `ENSFCAG00000011704` |
| GFF/GTF, plain and bgzip+tabix | 1 gene `ENSFCAG00000011704` 35459–46532, 2 transcripts |
| GenePred | 13 genes, first `ND3` 10351–10698 (also proves the IGV codec and its log4j 1.x logger still resolve at runtime) |
| VCF (bgzip+tabix) | 115 variations |
| WIG — BedGraph | 1001 blocks, values to 1.0 |
| WIG — BigWig | 5001 blocks, values to 958.0 |
| SEG | `GenomeWideSNP_416532`, 3 segments |
| BAM | 416 reads, first `15M1I216M` with its `differentBase` list |
| CRAM | 416 reads, **identical to the BAM**, `differentBase` included |
| remote plain `http://` VCF | identical to the local VCF |
| remote 403-on-`HEAD` VCF and BAM | identical to the local VCF and BAM |

Two notes on coverage:

- **There was no CRAM fixture in the repo**, so one was made rather than skipped:
  `.devenv/scripts/BamToCram.java` converts `agnX1.09-28.trim.dm606.realign.bam` against
  `dm606.X.fa` with htsjdk itself, giving `p7_agnX1.cram` (2,419,002 bytes, 45,237 records)
  and a 120-byte `.crai`. It lives in `.devenv/data/ngs`, not in `src/test/resources` — see
  Phase 0 note 1: a generated index under `templates/` gets auto-discovered by other tests.
  The CRAM-vs-BAM comparison is the strongest single check in the script, because
  `differentBase` is computed against NGB's own registered reference, so agreement means
  reference-compressed decoding is intact end to end.
- **MAF has no REST surface to verify through.** `MafController` and `MafSecurityService` were
  deleted in `562b6a6d` (Dec 2018); `MafManager` is reachable only from Java, and
  `MafManagerTest.testRegisterMaf` is the whole of its coverage. Stated rather than dropped
  from the list.

#### `GffManagerTest.testLoadGenesTranscript`: this phase's own exit criterion was wrong

The exit criteria above said "**Expect `GffManagerTest.testLoadGenesTranscript` to move here**".
It did not, and it could not — Phase 0 note 3 had already settled that (`setBioType` is fed only
from the Ensembl REST response). Corrected in place, in both this section's criteria and the
execution doc's canned Phase 7 block, so the next reader is not sent looking for a parser bug.

It failed in this phase's runs in its *other* documented shape, the NPE — and that shape is
positive evidence for the parser rather than against it. `NullPointerException: ...
Gene.getTranscripts() is null` at `GffManagerTest.java:455` means the assertions **before** it
passed: `featureList.getBlocks()` was non-empty and `getBlocks().get(0)` was a `Gene`. So stock
htsjdk parsed 75,207 records of `Homo_sapiens.GRCh38.83.sorted.chr21-22.gtf` into genes exactly as
the fork did; what failed is `GeneTrackManager.loadGenesTranscript` line 197, where
`getTranscriptFromDB` threw `ExternalDbUnavailableException`, the `catch` added a
`GeneTranscript(gene, message)` and left `gene.transcripts` null. Strictly downstream of the
parser, strictly the network.

Worth recording for whoever eventually deals with the network tests: the mocking in that test
cannot work as written. `httpDataManager` is a `@Mock` injected into local `@Spy` instances of
`PdbDataManager` / `EnsemblDataManager` / `UniprotDataManager`, but the call under test goes
through the `@Autowired` Spring `geneTrackManager`, which holds the **real** beans. The fixtures
`ensembl_id_ENSG00000177663.json` and `uniprot_id_ENST00000319363.xml` are loaded and then never
consulted.

#### Two Phase 6 tests were already red when this phase started

`make test` found five failures where the baseline documents three. The two extra were
`LuceneIndexVersionCheckTest.refusesToStartOnAnIndexAnEarlierReleaseWrote` and
`LuceneIndexUtilsTest.readingAStaleFeatureIndexNamesTheFileAndTheCallThatRebuildsIt`, both
asserting that the refusal message mentions `installation/lucene-reindex/` — the mkdocs URL form —
against a message that says `docs/md/installation/lucene-reindex.md`. Both halves were committed
together in `c76ff2ee`, so the assertion has never held; Phase 6's numbers were recorded before the
message text was settled and the suite was not re-run after. Not a Phase 7 regression: neither file
is touched by this phase, and the assertion is a deterministic `String.contains`.

Fixed on the test side, deliberately, and this is not a re-baselining: the assertion's intent is
"the message points at the reindex procedure", and the pointer that exists is the repo path — the
doc is at `docs/md/installation/lucene-reindex.md`, three javadocs (`LuceneIndexVersionCheck`,
`BamCoverageManager`, `PathwayManager`) cite that same form, and there is no `site_url` in
`mkdocs.yml` for the URL form to resolve against. Both assertions now expect the full repo path,
which is stricter than what they asked for before.

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
- `make test`, `make test-pg`, `make lint` all green (and `make cli-test`, if Phase 9's fixture
  hosting has happened by then — see `TEST-BASELINE.md`) against the Phase 7
  baseline.
- Excel export, protein/PDB views, LLM target summaries, S3 and Azure track loading all
  verified by hand.

### Phase 8 execution findings

#### Corrections to the table above

Three rows describe a state that Phases 0–7 already changed:

- **AWS SDK** was at v1 **1.12.797**, not 1.11.704. Phase 0 bumped it to the last v1 release to
  get the application past `EC2MetadataUtils.<clinit>` (see `TEST-BASELINE.md`). v1 went
  end-of-support in December 2025 either way, so the target is unaffected.
- **Swagger annotations**: **41** files import `com.wordnik.swagger.annotations.*`, not 42.
- **Misc**: `commons-lang3:3.0` and `commons-io:2.4` are already gone — lang3 has been
  Boot-managed since Phase 3, commons-io is pinned at exactly 2.15.1 (deliberately: 2.16.0
  deprecates `CountingInputStream`, which four seekable-stream classes use). `aspectjweaver`
  also already lost its 1.8.8 pin in Phase 2. And the `snappy-java:1.0.3-rc3` pin the row
  implies is gone: Phase 7 dropped it, htsjdk 5 manages snappy 1.1.10.5.

#### Divergences: three claims in the table that do not match the code

1. **biojava's consumers are not `manager/protein`, `manager/pdb`, `ConsensusSequenceUtils` or
   `ProteinSequenceUtils`.** Those four contain zero biojava imports. The real consumers are
   `manager/genbank/{GenbankManager, GenbankUtils, NGBGenbankReader}`, a single `DNASequence`
   import in `manager/reference/ReferenceManager`, and `manager/genepred/GenePredManagerTest`.
   `org.biojava.nbio.structure` has **no** direct import anywhere, although `biojava-structure`
   is declared — it is on the classpath as one of the things that justify the javax JAXB stack
   (`javax.xml.bind:jaxb-api` + `com.sun.xml.bind:jaxb-impl`), alongside libsbgn and
   sbgn-converter. Consequence for the exit criteria: "protein/PDB views verified" is **not** a
   biojava check. Genbank parsing and GenePred are.
2. **There is no GA release of `azure-ai-openai`.** Published versions are `1.0.0-beta.1` …
   `1.0.0-beta.16`. The only move available is beta.2 → beta.16; "beta → GA changed package
   names" cannot be acted on.
3. **The `org.apache.commons.collections` (v3) usage is not in `JWTSecurityConfiguration`.** It
   is in nine files — `entity/gene/GeneFilterForm`, `dao/BiologicalDataItemDao`,
   `dao/heatmap/HeatmapDao`, `dao/reference/SpeciesDao`,
   `manager/externaldb/homologene/HomologeneManager`, `manager/reference/ReferenceManager`, and
   in ngb-cli `AbstractHTTPCommandHandler`, `PrintPermissionsHelper`, `UrlGeneratorHandler`.

#### AWS SDK v1 → v2 (commit 1)

**Version: 2.54.1, via the BOM rather than per-artifact pins.** v2 is 477 co-released modules
and mixing versions across them does not work, so `software.amazon.awssdk:bom` is imported as a
second `mavenBom` next to Boot's. This is safe: the AWS BOM manages nothing but its own
`software.amazon.awssdk` artifacts, so it cannot move anything Boot manages — Jackson and slf4j
in particular, both of which the SDK depends on.

**The name clash is the reason this code looks odd.** NGB's own class is
`com.epam.catgenome.util.aws.S3Client` and the SDK's is
`software.amazon.awssdk.services.s3.S3Client`. Eight other files refer to NGB's class by simple
name, so the SDK type is spelled out in full inside `util/aws/S3Client.java` rather than
renaming NGB's class.

**Three API shifts drove the rewrite:**

- *Presigning left the client.* `AmazonS3.generatePresignedUrl(request)` became a separate
  `S3Presigner`, which carries its own region, credentials and endpoint configuration. Hence the
  parallel field pair per cloud type in `util/aws/S3Client` (`s3`/`s3Presigner`,
  `swiftStack`/`swiftStackPresigner`) and the `getPresigner(CloudType)` mirror of
  `getAws(CloudType)`. `PresignedRequest.expiration()` reports the real expiry, so
  `BiologicalDataItemDownloadUrl.expires` no longer has to be computed alongside the request.
  `ResponseHeaderOverrides.withContentDisposition(...)` became
  `GetObjectRequest.responseContentDisposition(...)`.
- *URI parsing hangs off a client.* `AmazonS3URI` was a standalone parser; v2's equivalent is
  `client.utilities().parseUri(URI)` returning an `S3Uri` whose `bucket()`/`key()` are
  `Optional`. Verified against the 2.54.1 sources: it accepts the `s3://bucket/key` form and
  percent-decodes, exactly as `AmazonS3URI` did. The `Optional`s are unwrapped by two helpers
  that throw `IllegalArgumentException` on a keyless URI — v1 returned null there and failed
  later.
- *Ranges and errors.* `GetObjectRequest.setRange(a, b)` became `.range("bytes=a-b")` (both ends
  inclusive, as before); `getObject` returns a `ResponseInputStream<GetObjectResponse>` instead
  of `S3Object`+`getObjectContent()`; `AmazonS3Exception.getStatusCode()` became
  `S3Exception.statusCode()`, and `NoSuchKeyException` (404) extends `S3Exception`, so
  `isFileExisting`'s single catch still covers both 403 and 404. `org.apache.http.HttpStatus`
  gave way to `software.amazon.awssdk.http.HttpStatusCode`.

Smaller translations: `new AWSCredentialsProviderChain(new ProfileCredentialsProvider("sws"))`
→ `ProfileCredentialsProvider.create("sws")`; `EndpointConfiguration(host, region)` →
`endpointOverride(URI)` + `region(Region.of(...))` — v2 rejects an endpoint without a scheme
where v1 defaulted to https, so `endpointUri()` adds `https://` when the configured
`swift.stack.endpoint.url` has no scheme; `setPathStyleAccessEnabled` →
`S3Configuration.builder().pathStyleAccessEnabled(...)`; `com.amazonaws.util.IOUtils` →
`org.apache.commons.io.IOUtils` (same `toByteArray(InputStream) throws IOException`, so the
three call sites are unchanged).

`Utils.getTimeForS3URL()` now returns a `Duration` instead of an expiry `Date`, because that is
what `GetObjectPresignRequest.signatureDuration` takes.

**The transport shape needed measuring, not guessing.** The `s3` POM declares no HTTP client,
but its `services` parent POM declares `apache5-client` **and** `netty-nio-client` at *runtime*
scope, so every v2 service module drags both in whether asked or not. Consequences:

- An explicit `apache-client` (httpclient 4.x) declaration is pointless next to that:
  `ClasspathSdkHttpServiceProvider` ranks `Apache5SdkHttpService` above `ApacheSdkHttpService`
  above `UrlConnection`, so apache5 wins regardless. (In 2.54.1 finding several sync
  implementations is a debug log and a priority choice, not the hard failure older v2 versions
  had.) `apache5-client` is therefore declared explicitly instead — to state which transport is
  in use, not to change it.
- The httpclient 4.5 that v1's `aws-java-sdk-core` used to put on the classpath, and that 16
  files still import as `org.apache.http.*`, now arrives from opensaml/shibboleth and
  google-http-client instead. Verified present on both `compileClasspath` and
  `runtimeClasspath` after the swap.
- `netty-nio-client` is excluded from both `s3` and `sts`: it is the async transport, nothing
  here builds an async client. It is **not** what puts Netty in the fat jar — the Azure SDK does
  that via `azure-core-http-netty` and reactor-netty — so this saves the adapter, not Netty.
- The `s3` POM declares `org.mockito:mockito-junit-jupiter` at *compile* scope (an upstream
  packaging slip, still present in 2.54.1). Excluded; a mocking framework has no business on the
  server's runtime classpath.
- `sts` stays runtime-only, as `aws-java-sdk-sts` was: no NGB code imports it, it is what the
  default credential chain needs to honour a `role_arn` profile or an EKS web-identity token.

Two behaviour details preserved deliberately: the constructor still swallows a failure to build
the default client (`SdkException`, the supertype of the `SdkClientException` v1 threw when no
region could be resolved) and logs "S3 services will be unavailable" — every NGB startup builds
this bean, so a machine with no AWS configuration has to come up rather than fail. And
`isFileExisting` still bypasses the `fileSizes` Guava cache, as v1 did, so the `S3Exception`
arrives unwrapped rather than inside an `UncheckedExecutionException`.

Finally, `profiles/dev/log4j2.xml`: `<Logger name="com.amazonaws">` became
`<Logger name="software.amazon.awssdk">`, and an `org.apache.hc` muzzle was added — the SDK's
wire logging now goes through httpclient 5, which the existing `org.apache.http` muzzles do not
cover. Those muzzles stay: NGB's own REST calls still use httpclient 4.5.

#### commons-io 2.15.1 → 2.22.0, and the counting stream (commit 2)

The pin existed for one reason, recorded in Phase 7: `FeatureSeekableStream` wrapped its inner
data stream in commons-io's `CountingInputStream` to know how far into the stream reads had got,
and 2.16.0 deprecated that class. Bumping the version and leaving the usage would have traded a
pin for a deprecation warning, so the usage went first.

**`BoundedInputStream`, the replacement commons-io's own deprecation notice points at, is not
one.** Checked against the 2.22.0 sources: `CountingInputStream` overrides `skip` and adds the
skipped bytes to its count, `BoundedInputStream` does not override `skip` at all, so its
`getCount()` would under-report by however much was skipped and `position()` would drift.
(Correcting a claim carried in the earlier notes, which had it the other way round.) Its
single-argument constructor is also deprecated in favour of a builder whose `get()` throws
`IOException`, which the three subclasses' non-throwing construction sites cannot use as they
stand.

So the count moved into `FeatureSeekableStream` itself — a field incremented in `read()` and
`read(byte[], int, int)`, which is all a decorator was ever doing here. Nothing is lost: this
class does not delegate `skip`, so a `skip` on it runs `InputStream.skip`, which reads into a
throwaway buffer through `read(byte[], int, int)` and is still counted. (htsjdk agrees that this
does not matter in practice — `SeekableStream` does not override `skip` either, and
`SeekableBufferedStream.skip`, which is what wraps these streams in the reader stack, either
skips inside its own buffer or calls `seek`; it never reaches the wrapped stream's `skip`.)

That removes a wrapper object and a virtual call from every byte of every cloud and remote read,
and it lets `CountingWithSkipInputStream` go — the bare `CountingInputStream` subclass with
nothing left to override that survived Phase 7 only because two subclasses constructed it by
name. In its place the parent exposes the two things the subclasses actually need:
`closeDataStream()` (null-tolerant, for the first call) and `serveFrom(InputStream)`, which
installs the stream and restarts the count. The subclasses keep their existing close-then-open
ordering and their existing exception behaviour — `S3SeekableStream` and
`AzureBlobSeekableStream` still wrap a close failure in `RuntimeIOException` because their
`recreateInnerStream()` is called from a constructor and cannot throw; `UrlSeekableStream`'s
still propagates.

commons-io is now `2.22.0` and unpinned in spirit, but the version is still stated explicitly:
Spring Boot 3.5's BOM does not manage commons-io, and it must stay at or above what
commons-compress 1.26.0 (htsjdk 5's transitive) asks for.

Not fixed here, and not introduced here: `NgbFileUtils:212`, `TargetExportHTMLManager:228`,
`NCBISequenceManager:165` and 15 `FileUtils.writeStringToFile` calls in
`UpdateFilePathManagerTest` use commons-io overloads that default to the platform charset. Those
were already deprecated at 2.15.1, so the bump does not change their status; fixing them means
deciding a charset per call site, which is not this phase's business.

Verified: `make test` 534/4 failed/21 skipped (the four documented), `make lint` clean at 37
warnings in 14 files, and `verify-tracks.sh` green — `signed s3 vcf`, `signed s3 bam`, `http
vcf` and `cram` all byte-identical to their local equivalents, which is the check that matters
since all three rewritten subclasses are on that path.

#### POI 3.16 → 5.5.1 (commit 3)

Latest is 5.5.1, resolved rather than guessed (`poi:5.+` through `dependencyInsight`, then pinned).
The API break is the one the table predicted and no more: `Cell.getCellTypeEnum()` — the interim
name POI 3.15 gave the typed getter while `getCellType()` still returned an `int` — was removed in
POI 4 and its behaviour returned to `getCellType()`. Two call sites, both in `TargetGeneManager`
(the numeric-Tax-ID assertion at 776 and the `getCellValue` switch at 844). `ExcelExportUtils` and
`TargetExportXLSManager` needed no change at all: `createSheet`, `createRow`, `createCell`,
`setCellValue`, `createCellStyle`, `XSSFWorkbook.createFont`, `setBold`, `autoSizeColumn` and
`Workbook.write` are all unchanged.

**A pin that had to move with it: commons-collections4.** POI 5.5.1 requests 4.5.0; the build
declared 4.1 (2015). Gradle's default highest-wins would have taken 4.5.0, but the
`io.spring.dependency-management` plugin resolves the Maven way, so a version declared in the build
script beats a newer transitive request — POI would have run against a library four minor versions
older than it was compiled against, with no build-time signal. Bumped to 4.6.0 (latest) in this
commit rather than in the version-backlog commit. NGB only uses
`CollectionUtils`/`ListUtils`/`MapUtils`/`SetUtils` from it, all unchanged. The same mechanism is
worth remembering for the Azure commit: it is why the explicit `reactor-core:3.5.3` line
*downgrades* reactor from 3.7.19.

Other transitive movement: `poi-ooxml-schemas:3.16` + `xmlbeans:2.6.0` (2011) → `poi-ooxml-lite`
+ `xmlbeans:5.3.0`; `commons-compress` rises 1.26.0 → 1.28.0 (POI asks for a newer one than htsjdk
does), which asks for commons-io 2.20.0 and so is covered by commit 2's 2.22.0; `curvesapi` 1.04 →
1.08; `SparseBitSet`, `commons-codec` and `log4j-api` join. `commons-codec` resolves to 1.18.0
rather than the 1.20.0 POI asks for, because Spring Boot's BOM manages it — a managed version, not
an accident.

**Verified by hand, because these paths have no test coverage at all** (no xlsx fixture exists and
no test mentions XSSF or the import endpoint). Both POI surfaces were exercised through REST
against the running server, before the bump and after, on identical inputs:

- *Write:* `GET /target/report?targetId=…&genesOfInterest=ENSG00000012048&translationalGenes=ENSG00000139618`
  on a target holding BRCA1/BRCA2. Both builds returned a valid 12-sheet workbook (~30 KB) that
  openpyxl reads. Comparing sheet names, dimensions, header text, header boldness and sampled data
  rows — including all 353 rows of live RCSB structure data in "Structures (PDB)" — the two are
  identical, down to POI still silently truncating "Associated Diseases(Open Targets)" to the
  31-character sheet-name limit rather than throwing.
- *The one visible difference:* `autoSizeColumn` widths come out uniformly narrower under POI 5, by
  a constant factor of 6/7 (e.g. 8.008 → 6.867 characters) — POI's default character width, which
  autofit divides by, changed. Cosmetic, and never environment-independent anyway since autofit
  measures with AWT font metrics against whatever fonts the container has.
- *Read:* `POST /target/genes/import/4` with an xlsx built by openpyxl — deliberately a foreign
  producer, not POI — with cells covering every branch of `getCellValue` (STRING, NUMERIC, BOOLEAN,
  blank), an `AG 10090` additional-genes column and a numeric metadata column. POI 5 produced
  byte-for-byte the same imported records as POI 3.16: same gene IDs, `taxId` 9606, priorities
  HIGH/LOW, `additionalGenes {ENSMUSG00000059552: 10090}`, metadata `NumericMeta`/`BoolMeta`. A
  second fixture with a text Tax ID still fails with "Tax ID should be numeric", so the rewritten
  assertion was checked from both directions.
  (Aside, not a POI matter: metadata `42` reads back as `"42.0"` because a metadata field whose
  `TargetGeneField.filterType` is RANGE is stored in Lucene as a `FloatPoint`/`StoredField` float.
  Same before and after.)

`FileFormat` is only CSV and TSV, so `/target/export/{geneId}` is not an Excel path — the report
endpoint and the gene import are the whole of NGB's POI surface, and both are now verified.

#### biojava 4.2.0 → 7.2.6, and one dependency deleted outright (commit 4)

Latest is 7.2.6 (checked against Central's `maven-metadata.xml`, not guessed); its class files are
Java 11 bytecode (major 55). The table asked for "6/7" — 7 it is, because there is no reason to stop
at 6: NGB uses two packages of it and both are stable across the range.

**`biojava-structure:4.2.0` was declared and imported nowhere.** Zero references in any `.java`,
`.xml` or `.properties` file in the repo. NGB's protein/PDB feature does not parse structures at
all: it stores the PDB file and the client renders it with miew, and the RCSB metadata comes over
REST through `PdbDataManager` (jettison). So that line is deleted rather than bumped, and with it
one of the three justifications Phase 3 recorded for keeping the `javax.xml.bind` stack alongside
the jakarta one. The stack still stays — `org.sbgn:libsbgn:0.2` and paxtools' `sbgn-converter` are
still javax-annotated, and D6 freezes both — but the comment in `build.gradle` now names two
dependencies, not three.

**The forester exclusion is not a choice; it is an upstream packaging defect.**
`biojava-genome → biojava-alignment → org.biojava.thirdparty:forester`, and forester's POM has
never been resolvable: 1.038 (which biojava 4.2.0 asked for) declared `openchart` with a *relative*
`systemPath`, which is why every Gradle invocation on this repo has been printing "Errors occurred
while building effective model" for years; 1.039 (which biojava 7 asks for) declares
`openchart:openchart:1.4.2` as an ordinary dependency, and that artifact is published nowhere — so
what was a warning under biojava 4 becomes a hard `Could not find openchart:openchart:1.4.2` under
biojava 7. Excluding `org.biojava.thirdparty:forester` fixes the failure and silences the
long-standing warning at the same time.

**But NGB *did* reach into forester, exactly once, and the compiler caught the claim that it did
not.** `HeatmapManager` statically imported `org.forester.io.parsers.util.ParserUtils.createReader`
— not for anything phylogenetic (the newick parsing has always been libnewicktree's
`net.sourceforge.olduvai.treejuxtaposer.TreeParser`), but as a convenience factory. Disassembled
(`javap -p -c`), its whole body is a type switch over `BufferedReader` constructors: `File`/`String`
→ `exists`/`isFile`/`canRead` checks, each throwing an `IOException`, then
`new BufferedReader(new FileReader(file))`; `InputStream` → `new BufferedReader(new
InputStreamReader(is))`; `StringBuffer`/`StringBuilder` → `StringReader`; anything else →
`IllegalArgumentException`. Both call sites were replaced with the idiom the same class already uses
in five other methods (`try (Reader reader = …; BufferedReader r = new BufferedReader(reader))`):

- `readTree(InputStream, String)` → `new InputStreamReader(is)`. The stream is a DB blob from
  `heatmapDao.loadHeatmapRowTree`/`loadHeatmapColumnTree`; the `path` argument only names the tree.
- `checkTree(Set<String>, String)` → `new FileReader(path)`, preceded by NGB's own
  `NgbFileUtils.getFile(path)` — which is `Assert.isTrue(file.isFile() && file.canRead(), …)`,
  i.e. forester's three checks in NGB's idiom and with NGB's localised message. Every one of the
  four call sites (`createHeatmap` twice through `readFileContent`, `updateRowTree`,
  `updateColumnTree`) already called `getFile` first, so this is belt-and-braces; it makes the
  method self-validating rather than dependent on caller discipline. No test asserts forester's
  error strings.

`HeatmapManagerTest` covers both branches through `src/test/resources/heatmap/tree.txt`:
registration with `rowTreePath`/`columnTreePath` exercises `checkTree`, and `getTree` exercises
`readTree`.

**The one API break in the bump**, and the only one in the whole of NGB:
`GenbankSequenceParser.getDatabaseReferences()` now returns `Map<String, List<DBReferenceInfo>>`
where 4.2.0 returned `Map<String, ArrayList<DBReferenceInfo>>` — one local variable in
`NGBGenbankReader.process` (the taxonomy-ID lookup). Everything else compiled untouched:
`GenbankUtils` (`DNASequenceCreator`, `DNACompoundSet`, `AccessionID`), `GenbankManager`
(`FeatureInterface`, `Qualifier`, `FastaWriterHelper`, `genome.parsers.gff.Location`),
`ReferenceManager` and `GenePredManagerTest`'s `GFF3Reader`.

**Verified against the previous version byte-for-byte, because the tests do not check the output.**
`GffManagerTest.testRegisterGbk`/`testRegisterGbf` and `ReferenceManagerTest.testGenbankRegister`
only assert that registration succeeded and produced a `.gff` — a silently different Genbank parse
would pass them. In particular `GenbankManager.genbankToGff` reads `f.getSource()` as a *raw
location string* ("`191..7309`", "`<7306..11679`") and splits it on `\..|,`, and takes the strand
from `f.getLocations()`; both are exactly the kind of thing a parser rewrite moves. So a temporary
throwaway JUnit test (deleted after use; it needed nothing but
`new MessageHelper(new ResourceBundleMessageSource("catgenome-messages"))`, since
`GenbankManager.genbankToGff` touches no Spring bean) dumped, for `templates/KU131557.gbk`: the
produced GFF3, the FASTA that `genbankToFasta` writes through `FastaWriterHelper`, and a full parse
report — sequence key, `getSequenceId`, accession, original header, description, taxonomy ID,
length, sequence head, and every feature's type, raw source, strand, location and qualifier keys.
Run under 4.2.0 and under 7.2.6 (`git stash` between the two, so the same fixture and the same NGB
code): **all three files identical**, including `taxonomy=taxon:28344`, the 12 features, and the
`<7306..11679` partial-location string that the regex depends on.

`make test`: 534 tests, 4 failed, 21 skipped — the three documented failures plus
`PdbDataManagerTest.testParse`, which failed as `expected:<B> but was:<A>` on `record.getChainId()`,
i.e. RCSB returned 1JSP's chains in the other order. That test reaches the live RCSB API and imports
no biojava. `make lint`: 14 files, 37 warnings, 0 errors, pmd clean.

#### Azure Storage Blob 12.14.0 → 12.35.0, azure-identity 1.4.4 → 1.18.4 (commit 5)

Done through **`com.azure:azure-sdk-bom:1.3.8`**, not two pins, for the reason the AWS commit gave:
azure-storage-blob and azure-identity share `azure-core`, `azure-core-http-netty` and `azure-json`,
those are co-released, and a mismatch across them surfaces as a `NoSuchMethodError` inside the SDK.
The BOM holds 102 entries and every one of them is `com.azure`/`com.azure.resourcemanager` — checked,
because a BOM that reached into Jackson or Netty would fight Boot's. It carries only GA libraries,
which is why `azure-ai-openai` (beta-only) keeps an explicit version.

Resolved after the import: azure-storage-blob 12.35.0, azure-identity 1.18.4, azure-core 1.58.1,
azure-core-http-netty 1.16.5, azure-storage-common 12.34.0, azure-json 1.5.1. Netty stays at
Boot's 4.1.135.Final throughout, and reactor-netty at 1.2.18.

**The `reactor-core:3.5.3` line is deleted, and it was doing the opposite of what it looked like.**
`git log -S` traces it to a962d051 (Oct 2021), the commit that moved NGB from
`com.microsoft.azure:azure-storage-blob:10.4.0` to `com.azure:azure-storage-blob:12.14.0` to fix an
OOM; it pinned reactor 3.4.11 to get something newer than azure-core then asked for. Boot has
managed reactor since Phase 3, at 3.7.19 — so under `io.spring.dependency-management`'s Maven-style
resolution the declared 3.5.3 was *downgrading* reactor by two minors. Removing the line moves
reactor 3.5.3 → 3.7.19 and preserves the 2021 intent rather than reversing it.

No NGB code changed: `BlobServiceClientBuilder.endpoint/credential/buildClient`,
`StorageSharedKeyCredential`, `ClientSecretCredentialBuilder`, `DefaultAzureCredentialBuilder`
(`.managedIdentityClientId`, `.tenantId`), `getBlobContainerClient`, `getBlobClient`,
`openInputStream(BlobRange, null)`, `exists()`, `getProperties().getBlobSize()` and
`generateSas(BlobServiceSasSignatureValues)` are all unchanged across 21 minor versions.
`BamHelper.fetchAZBamIndex` stays on `azureBlobClient.loadFully`, and `AzureBlobSeekableStream` is
untouched since commit 2.

What can be verified without an Azure account:

- The three existing tests (`ApplicationTest`, `CredentialConfigurerTest`,
  `AzureCredentialConfigurationTest`) build a real `BlobServiceClient` through each of the three
  credential shapes. They pass — so client construction, shared-key credential parsing and both
  azure-identity builders still work under 1.18.4.
- **The SAS download URL, which is the one Azure call that needs no network** (it is an HMAC over a
  canonical string). A throwaway test, deleted after use, reflected into the private
  `AzureBlobClient.buildBlobDownloadUrl` with Azurite's well-known development key and dumped the
  URL under both versions. Identical apart from the service version: same host, container and blob
  path, same `sr=b`, same `sp=r` (the `setAddPermission(false)`/`setWritePermission(false)` calls
  contribute nothing, as before), same `se` at +1 day; `sv` moves `2020-10-02` → `2026-06-06`, which
  is the bump doing its job.
- What remains is the network half — `exists`, `getProperties`, `openInputStream` and therefore
  `az://` track loading. See "Cloud verification" below for how far that got: note that
  `AzureBlobClient` hard-codes `https://%s.blob.core.windows.net` with no endpoint override, so
  Azurite cannot be pointed at without changing NGB code.

`make test`: 534 tests, 3 failed, 21 skipped — exactly the documented three;
`PdbDataManagerTest.testParse` passed this run, which confirms the fourth failure in commit 4 was
the RCSB flap. `make lint`: 14 files, 37 warnings, pmd clean.

#### azure-ai-openai 1.0.0-beta.2 → 1.0.0-beta.16 (commit 6)

**This is not an optional companion to commit 5, it is a consequence of it.** beta.2 (June 2023) was
built against azure-core 1.40.0; the `azure-sdk-bom` import moves azure-core to 1.58.1 underneath it.
Leaving the LLM client on beta.2 would be an untested 18-minor-version skew inside one SDK — exactly
the `NoSuchMethodError` shape the BOM was imported to avoid.

**There is no GA to move to.** The version list on Central runs beta.1 (2023-05-22) … beta.16
(2025-03-26) and stops; Microsoft has not shipped a 1.0.0 and has not published anything since. So
the pin stays explicit and outside the BOM, which carries GA artifacts only, and this library is the
one place in NGB where "latest" means "latest beta". It has to be bumped by hand whenever the storage
bump moves azure-core again.

`manager/llm/OpenAIClient` is the only file in the repo that imports `com.azure.ai.openai.*`
(`CustomLLMApiClient` and `CustomOpenAILLMClient` go through it, or through retrofit). Two API breaks,
both landing in that file, both introduced in **beta.6**:

- **A chat request message went from one class to a class per role.** beta.2 had
  `new ChatMessage(ChatRole.fromString(role)).setContent(text)`; from beta.6 there is a
  `ChatRequestMessage` interface with `ChatRequestUserMessage`, `ChatRequestSystemMessage`,
  `ChatRequestAssistantMessage`, `ChatRequestDeveloperMessage`, `ChatRequestToolMessage` and
  `ChatRequestFunctionMessage` under it, each taking the content in its constructor. `LLMRole` has
  exactly three values (`USER`, `SYSTEM`, `ASSISTANT`) and the old code fed
  `name().toLowerCase(ROOT)` to `ChatRole.fromString`, so a three-arm `switch` in a private
  `toRequestMessage` reproduces the previous mapping exactly, with `user` as the default arm.
- **`NonAzureOpenAIKeyCredential` is gone**, folded into azure-core's `KeyCredential`. Both
  constructors used it, and both now pass `new KeyCredential(key)`. That is the only replacement
  available — but it is not a like-for-like one, and the difference is worth spelling out, because it
  changes what one of NGB's two constructors does. See below.

##### The `NonAzureOpenAIKeyCredential` → `KeyCredential` swap changes `llm.custom.type=openai`

In beta.2, supplying a `NonAzureOpenAIKeyCredential` selected a whole separate client: `buildClient()`
branched on that field and returned an `OpenAIClient` wrapping `NonAzureOpenAIClientImpl(pipeline,
serializer)` — note the absent endpoint argument. That impl has a `public static final String
OPEN_AI_ENDPOINT` and stamps `https://api.openai.com/v1` into every request. **So under beta.2 the
`.endpoint(endpoint)` call in NGB's two-argument constructor was dead code**: `CustomOpenAILLMClient`,
i.e. `llm.custom.type=openai` with `llm.custom.url=…`, ignored the configured URL and talked to
api.openai.com, with `Authorization: Bearer <llm.custom.token>`. The URL was, in effect, only an
on/off switch for the feature.

beta.16 has one client impl, so the endpoint is honoured, and the auth header is chosen by a private
`useNonAzureOpenAIService()` — `endpoint == null || endpoint.startsWith("https://api.openai.com/v1")`
— rather than by the credential class:

| | endpoint honoured? | auth header |
|---|---|---|
| beta.2, `OpenAIClient(key)` | n/a | `Authorization: Bearer` |
| beta.2, `OpenAIClient(key, url)` | **no**, hard-coded api.openai.com | `Authorization: Bearer` |
| beta.16, `OpenAIClient(key)` | n/a, defaults to api.openai.com | `Authorization: Bearer` |
| beta.16, `OpenAIClient(key, url)` | **yes** | `api-key` (Azure convention) |

The one-argument constructor — `OpenAIChatGPT35`, `OpenAIChatGPT40`, the paths that actually get used
— is unchanged in both columns. The two-argument one changes twice over, and both changes come from
the SDK, not from a choice made here: there is no beta.16 API that both takes an endpoint and keeps
the non-Azure auth shape.

This is left as the SDK does it, deliberately, and flagged rather than papered over. Restoring beta.2's
behaviour would mean re-implementing a bug (accept a URL, then ignore it), and beta.16's behaviour is
the one the configuration plainly intends for an Azure OpenAI resource. But it does mean a deployment
that set `llm.custom.type=openai` with a self-hosted OpenAI-compatible URL was silently being served
by api.openai.com and will now get Azure-shaped requests (`/openai/deployments/{model}/chat/
completions?api-version=…`, `api-key`) against its own host. **That configuration should move to
`llm.custom.type=custom`**, which is NGB's own retrofit `CustomLLMApiClient` and is the OpenAI-
compatible-proxy path. A release note, not a code change; the alternative is a rewrite of
`CustomOpenAILLMClient` and its `LLMProvider` wiring, which is outside this phase.

Untouched while in there: the two-argument constructor also adds `new Header("bearer", key)` through
`clientOptions`, i.e. a header literally named `bearer` carrying the raw key. It is pre-existing, it
is almost certainly a mangled attempt at `Authorization: Bearer`, and it is additive to whatever the
credential policy sets, under both versions.

One further rename: `ChatCompletions.getCreated()` → `getCreatedAt()` (`OffsetDateTime` now, not a
Unix second count). It appears only inside a `log.debug` placeholder. `setMaxTokens(Integer)`,
`setTemperature(Double)`, `setN(Integer)` and `ChatChoice.getMessage().getContent()` are unchanged and
none of them is deprecated in beta.16 — checked with `javap -v`, since `max_tokens` is deprecated in
the OpenAI REST API itself and a Java deprecation would have been worth following.

Resolved after the bump: azure-ai-openai's own `azure-core:1.55.3 → 1.58.1` and
`azure-core-http-netty:1.15.11 → 1.16.5`, i.e. it now shares one azure-core with storage and identity,
which was the point.

**Not verified, and cannot be here: that a chat completion actually comes back.** There are no LLM
keys in this environment, and every path into this class needs a live key —
`OpenAIChatGPT35`/`OpenAIChatGPT40` reach `api.openai.com`, `CustomOpenAILLMClient` an operator's own
endpoint. The compile-time surface is fully covered and the credential/endpoint wiring is verified in
bytecode as above, but the request/response round trip is not, and "LLM target summaries" therefore
goes on the unverified list with the cloud items. See "Cloud verification" below.

`make test`: 534 tests, 3 failed, 21 skipped — the documented three. `make lint`: 14 files,
37 warnings, pmd clean.

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
   **Add `--enable-native-access=ALL-UNNAMED` to every launcher** — `docker/core/Dockerfile`,
   `docker/demo/*`, the generated Windows and Linux start scripts, and the run commands in
   `docs/md/installation/`. Phase 6 put it in `.devenv/ngb/entrypoint.sh`, which covers the dev
   environment only. Lucene 9's `MMapDirectory` calls `java.lang.foreign`, so without the flag
   every start prints three `WARNING: A restricted method in java.lang.foreign.Linker has been
   called` lines. The manifest attribute that would remove the need for a flag
   (`Enable-Native-Access: ALL-UNNAMED`) is JDK 24+, so it cannot be used here.
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
- `make test`, `make test-pg`, `make lint`, `make cli-test` (**this phase has to make it runnable
  again**), `make smoke`, `make smoke-saml`
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
make cli-test         # CLI <-> server integration suite (unrunnable: dead fixture host)

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
