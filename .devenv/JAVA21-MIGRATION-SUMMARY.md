# Java 21 migration — what changed, and what behaves differently

Written after Phase 9, verified against the tree at `78dffedc` (branch `java_21`). Every claim below
was re-checked against the code, not taken from the plan documents; where a document and the code
disagree, the code wins and the disagreement is recorded in
[§3, Defects and documentation errors](#3-defects-and-documentation-errors).

**Which document owns what.**
[`docs/md/release-notes/3.0.0/3.0.0.md`](../docs/md/release-notes/3.0.0/3.0.0.md) is the published
operator-facing 3.0.0 release note — the thing to hand to a user; this file is the migration's own
record of every behaviour change with the evidence behind it, including five places where the release
note or the installation docs assert something the code does not do (D1-D5) and four defects found
while checking and deliberately left alone (D6-D9).

- [1. What the migration changed](#1-what-the-migration-changed)
- [2. What behaves differently](#2-what-behaves-differently)
  - [Bucket 1 — requires action at upgrade](#bucket-1--requires-action-at-upgrade)
  - [Bucket 2 — visible change, no action needed](#bucket-2--visible-change-no-action-needed)
  - [Bucket 3 — alarming in the diff, internal](#bucket-3--alarming-in-the-diff-internal)
- [3. Defects and documentation errors](#3-defects-and-documentation-errors)
- [4. Not verified](#4-not-verified)

---

## 1. What the migration changed

Nine phases, 57 commits, 581 files, +21,435 / −17,217 (`git diff develop..HEAD --shortstat`;
`git log --oneline develop..java_21 | wc -l`). No track type, panel or client feature was altered,
added or removed — the client is frozen at AngularJS 1.5 / Node 14.17.5 by decision D12 and was
touched in nine files, of which four are build plumbing and three are user-visible at all (2.32-2.34:
a dead LLM provider entry, the SAML logout POST, and a desktop-only branch).

The rest of the diff is the server: a jakarta/Boot 3 rewrite of the wiring, four subsystems replaced
outright (SAML, Flyway, Lucene, AWS), and — decision D5 — five features removed rather than migrated:
**HDFS** and **GA4GH** as resource types (1.8), the **Electron desktop** packaging (2.34), the unused
**`person`** tables (2.4), **PaLM 2** (2.32), and **WAR** deployment (1.10). **OAuth2**, the
**Singularity** recipe and the **Sonar** configuration went with them; none had a user-facing
surface.

### The stack

| | Before (`develop`) | Now (`java_21`) |
|---|---|---|
| JDK | 8 | **21** (CLI: 17 toolchain) |
| Gradle | 3.3 | 8.14.5 |
| Spring Boot / Framework | 1.5.2 / 4.3.7 | 3.5.16 / 6.2 |
| Spring Security | 4.2.2 + `spring-security-saml2-core` 1.0.2 (OpenSAML 2) | 6.5.11 + OpenSAML 4.3.2 |
| Servlet API | `javax.servlet`, Tomcat 8, jar **or WAR** | `jakarta.servlet`, Tomcat 10, jar only |
| Flyway | 3.2.1 | 11.7.2 |
| H2 | 1.3.176 (2014) | 2.3.232 |
| PostgreSQL | 9.6 (driver 42.x) | 16.15 (driver 42.7.12) |
| Connection pool / cache | c3p0 / EhCache 2.10.1 | HikariCP 6.3.3 / Caffeine |
| Lucene | 6.6.0 | 9.12.3 |
| htsjdk | 2.2.4 + a fork of four reader classes | stock 5.0.0 |
| AWS SDK | v1 1.11.704 (EOL) | v2 2.54.1 (BOM) |
| API docs | Swagger 1.x (mangofactory + `swagger-ui` 2.0.24 webjar) | springdoc-openapi 2.8.17 (OpenAPI 3.1) |
| Logging | log4j 1.2.17 via `slf4j-log4j12`, `log4j.xml` | log4j2 2.25.5 via `log4j-slf4j2-impl`, `log4j2.xml`, plus `log4j-1.2-api` for the two libraries that still compile against log4j 1 |
| CI | AppVeyor (`Previous Ubuntu1604`, `test: off`) | GitHub Actions, tests actually run |
| Also bumped | POI 3.16, biojava, jackson, java-jwt, Checkstyle, PMD | POI 5.5.1, biojava 7.2.6, jackson 2.21.5, java-jwt 4.6.0, Checkstyle 11.1.0, PMD 7.26.0 |

### The phases

From the progress table in [`JAVA21-MIGRATION-EXECUTION.md`](JAVA21-MIGRATION-EXECUTION.md):

| Phase | What | Commits |
|---|---|---|
| 0 | Baseline stabilisation + JDK 17 in the toolbox | `064a766e`..`2e21c3d2` |
| 1 | Remove dropped functionality (HDFS, GA4GH, desktop, WAR, `person`, OAuth2, PaLM 2, Singularity, Sonar) | `ce8542ae` |
| 2 | Gradle 7.6 + Spring Boot 2.7.18 + Lombok on JDK 17 | `cd2ab292`..`4eacb473` |
| 3 | Spring Boot 3.5 + jakarta + Gradle 8 on JDK 21, security reduced to anonymous | `1dd9546b`..`ccd1a812` |
| 4 | SAML2 + JWT on Spring Security 6 | `8a9a7fa3` |
| 5 | Flyway 11.7.2, H2 2.3.232, PostgreSQL 16.15 | `dda304bd`..`66b8d7d6` |
| 6 | Lucene 9 + reindex procedure + startup guard | `c76ff2ee` |
| 7 | htsjdk latest, fork deleted, index cache dropped | `ac04780d` |
| 8 | Remaining libraries and API polish | `925d5cdb`..`0334569e` |
| 9 | Packaging, CI, docs, release 3.0.0 | `16e00527`..HEAD |

### The numbers

From [`TEST-BASELINE.md`](TEST-BASELINE.md), all measured on this branch:

| Suite | Command | Result |
|---|---|---|
| Server unit, H2 | `make test` | 545 tests, **1 failed**, 21 skipped |
| Server unit, PostgreSQL | `make reset-pg && make test-pg` | 545 tests, **1 failed**, 21 skipped — the two flavours are now identical, number for number |
| Static analysis | `make lint` | green: PMD clean, checkstyle 37 warnings in 14 files, 0 errors |
| CLI integration | `make cli-test` | 145 rows, **129 passed, 0 failed**, 16 skipped — runnable again for the first time since 2018 |
| CLI unit | `./gradlew -p server/ngb-cli test` | 135 tests, 0 failed |

The single failure is `GffManagerTest.testLoadGenesTranscript`, which fetches a GTF from Ensembl over
the network; `PdbDataManagerTest.testParse` queries RCSB and flaps. Both are excluded in CI with
`-PexcludeNetworkTests` and documented in `TEST-BASELINE.md`. Before the migration the suite had
**no CI at all** — AppVeyor built with `-PnoTest`, set `test: off`, and then uploaded an empty
JaCoCo report ([`.github/workflows/build.yml:8-10`](../.github/workflows/build.yml)).

---

## 2. What behaves differently

### Bucket 1 — requires action at upgrade

**An existing installation breaks or loses data without these.** Nothing here is optional, and none of
it can be done by NGB on its own. All of it is one-time. Take a copy of the database and of
`files.base.directory.path` first: a converted database cannot be opened by NGB 2.x.

---

#### 1.1 An H2 1.3 database file cannot be opened at all

**What changed.** H2 1.3.176 → 2.3.232. The 2.x engine cannot read a 1.3 file: it is a different
on-disk format, and there is no in-place upgrade. Three of NGB's own identifiers also became reserved
words in 2.x — the table `CATGENOME.USER`, the column `METADATA.VALUE` and the column `SESSION.END`.

**Who notices.** Every operator upgrading an H2 installation, which is the default and therefore most
of them.

**What they have to do.** The export/import in
[`docs/md/installation/database-upgrade.md`](../docs/md/installation/database-upgrade.md): `SCRIPT TO`
run by the **1.3.176** jar, then `RUNSCRIPT FROM` run by the 2.x jar with
`;NON_KEYWORDS=END,USER,VALUE` appended to the *tool's* JDBC URL. There is a mandatory `sed` step
between the two — see 1.2. NGB itself needs no URL change: Hikari issues
`SET NON_KEYWORDS END,USER,VALUE` on every connection, SpEL-gated on the URL starting `jdbc:h2:`
([`applicationContext-database.xml:46-47`](../server/catgenome/src/main/resources/conf/catgenome/applicationContext-database.xml)).

**Evidence.** Exercised end to end against a real pre-migration fixture, not inferred from the version
bump: `SCRIPT TO` under 1.3.176 produced a 58,896-byte script, `RUNSCRIPT FROM` under 2.3.232 rejected
it at `CREATE CACHED TABLE CATGENOME.USER(` and imported it clean once `NON_KEYWORDS` was set
([`JAVA21-MIGRATION-PLAN.md:2286-2293`](JAVA21-MIGRATION-PLAN.md), fixture description at 2388).

---

#### 1.2 Heatmap cell bounds silently round to integers if the H2 import is done naively

**What changed.** `HEATMAP.MIN_CELL_VALUE` and `MAX_CELL_VALUE` were declared as bare `DECIMAL`
(2021). H2 1.3.176 kept whatever scale the inserted value had; H2 2.3.232 reads bare `DECIMAL` as
`NUMERIC(100000, 0)` — **scale zero** — and rounds every value to an integer on import.

**Who notices.** Anyone with heatmaps, and only after the fact: the import succeeds, and the tracks
then render with bounds of `0.0`.

**What they have to do.** Rewrite the two column declarations in the exported script before importing
it (the `sed` step in `database-upgrade.md`). A new migration,
`v2026.08.21_12.30__heatmap_cell_value_double.sql`, retypes the columns to `DOUBLE PRECISION` on both
flavours, but it runs *after* the import, so it cannot recover values the import already rounded.

**Evidence.** `HeatmapManagerTest.createHeatmapTest` failing on H2 2.x with
`expected:<0.001273579> but was:<0.0>`, recorded in the migration comment of
[`v2026.08.21_12.30__heatmap_cell_value_double.sql`](../server/catgenome/src/main/resources/database/catgenome/postgres/v2026.08.21_12.30__heatmap_cell_value_double.sql)
and in [`JAVA21-MIGRATION-PLAN.md:2515`](JAVA21-MIGRATION-PLAN.md).

---

#### 1.3 A PostgreSQL 9.6 dump restores into 16, then refuses every password

**What changed.** PostgreSQL 9.6 → 16.15. The restore itself works; the password hashes do not.
A 9.6 dump carries **md5** hashes and PostgreSQL 16 ships `scram-sha-256` in the default `pg_hba.conf`
for host connections.

**Who notices.** Every operator upgrading a PostgreSQL installation. It is easy to miss because
loopback/trust connections keep working — only NGB's `host` connection fails, with
`FATAL: password authentication failed for user "catgenome"`.

**What they have to do.** `ALTER ROLE catgenome WITH PASSWORD '…'` after the restore, re-hashing under
the new algorithm. Step 4 of `docs/md/installation/database-upgrade.md`.

**Evidence.** Confirmed against a real 9.6 → 16 restore, not assumed; the hash was inspected before
and after the `ALTER ROLE` ([`JAVA21-MIGRATION-PLAN.md:2327`, `2597-2601`](JAVA21-MIGRATION-PLAN.md)).

---

#### 1.4 Lucene 9 cannot read a Lucene 6 index, and the server refuses to start on one

**What changed.** Lucene 6.6.0 → 9.12.3, a three-major-version jump. Lucene supports reading one
major version back, so 9 cannot read 6 and cannot upgrade it. `lucene-backward-codecs` is
deliberately **not** on the classpath — it only buys 8 → 9
([`server/catgenome/build.gradle:389`](../server/catgenome/build.gradle)). Opening the directory with
`OpenMode.CREATE` is not an escape either: Lucene reads the existing segments before it truncates.
A new startup check, `LuceneIndexVersionCheck`, walks the six configured global index roots and fails
the boot with a `LuceneIndexVersionFailureAnalyzer` message rather than letting the first search die
inside a stack trace.

**Who notices.** Every operator with an existing `files.base.directory.path`. The server will not come
up until the global indexes are dealt with; per-file feature indexes are caught lazily, on the first
search that touches one, with a message naming the file and the call that fixes it.

**What they have to do.** The procedure in
[`docs/md/installation/lucene-reindex.md`](../docs/md/installation/lucene-reindex.md) — delete each
stale index directory and re-run its rebuild call. Two of those calls are new endpoints added by this
migration because no rebuild path existed: `PUT /restapi/pathway/index`
([`PathwayController.java:136`](../server/catgenome/src/main/java/com/epam/catgenome/controller/pathway/PathwayController.java))
and `PUT /restapi/bam/coverage/index`
([`BamCoverageController.java:86`](../server/catgenome/src/main/java/com/epam/catgenome/controller/bam/BamCoverageController.java)).
The BAM coverage one re-scans the BAM and is long-running.

**Evidence.** [`LuceneIndexVersionCheck.java:52-79`](../server/catgenome/src/main/java/com/epam/catgenome/app/LuceneIndexVersionCheck.java)
(what is checked and why), `:93-98` (the six roots: `taxonomy`, `homologene`, `pathway`,
`bam.coverage`, `targets`, `ncbi` `.index.directory`), `:250,253` (the rebuild hints in the failure
message).

---

#### 1.5 One index has no rebuild path and losing it loses data

**What changed.** `targets/genes` and `targets/gene.fields` are not an index over anything — they are
the **only** copy of the gene table a user uploads with `POST /restapi/target/genes/import/{targetId}`
(**Import genes** in the UI). `TargetGeneManager.importData` writes Lucene documents and nothing else;
the spreadsheet is not stored.

**Who notices.** Only someone tracking `develop`. `TargetGeneManager` arrived in `fa54b6b8`
(2024-01-16) and `git tag --contains fa54b6b8` is empty, so **no released version can be affected** —
the newest tag is `v2.7.1`.

**What they have to do.** Before upgrading, make sure the xlsx/csv/tsv files uploaded for every target
are still on hand, and re-upload each after the reindex. If they are gone, the targets come back with
an empty gene list and have to be re-populated by hand before identification reports will run. The
targets themselves — names, species, identifiers, imported associations — are in the database and are
unaffected.

**Evidence.** [`lucene-reindex.md:147-171`](../docs/md/installation/lucene-reindex.md);
[`JAVA21-MIGRATION-PLAN.md:2823-2840`](JAVA21-MIGRATION-PLAN.md) for the code trace
(`TargetGeneDao.saveTargetGenes` is never called from `TargetGeneManager`) and for why it was left as
a documented limitation.

---

#### 1.6 On PostgreSQL, two predefined roles are deleted and the rest are renumbered

**What changed.** The 2018 ACL seeding wrote a different set of predefined roles on each flavour.
PostgreSQL got `ROLE_CYTOBANDS_MANAGER` (8) and `ROLE_MAF_MANAGER` (9), which no authority in the tree
mentions, and **never got `ROLE_WIG_MANAGER` at all** — so on a PostgreSQL install
`WigSecurityService`'s `hasRole("WIG_MANAGER")` could not be satisfied by anybody and only an
administrator could manage WIG files. A new migration converges PostgreSQL onto the H2 set, which is
what `DefaultRoles`, `docs/md/user-guide/um-overview.md` and `RoleDaoTest` all agree on.

**Who notices.** PostgreSQL operators, on first start after the upgrade:

- users holding `ROLE_MAF_MANAGER` **lose it**, and MAF management falls back to administrators — what
  an H2 install has always done;
- `ROLE_WIG_MANAGER` appears at id 8 and can be granted for the first time;
- the ids of `ROLE_SEG_MANAGER` (10 → 9) and `ROLE_TARGET_MANAGER` (11 → 10) change, with their grants
  carried across.

**What they have to do.** Re-grant WIG and MAF management as intended, and check any external script
or API caller that grants roles by **numeric id** — `POST /restapi/user/{id}/roles`-style calls
naming id 9 or 10 now hit a different role. Roles created by operators are untouched (`S_ROLE` starts
at 100 and only known names are mapped).

**Evidence.** [`v2026.08.21_12.20__align_predefined_roles_with_h2.sql`](../server/catgenome/src/main/resources/database/catgenome/postgres/v2026.08.21_12.20__align_predefined_roles_with_h2.sql) —
the whole rationale (lines 1-37) and the renumbering, in one transaction, a no-op on an
already-aligned database; the name→id map is at lines 48-57, where the survivors move 10 → 9 and
11 → 10. [`DefaultRoles.java:46-49`](../server/catgenome/src/main/java/com/epam/catgenome/entity/user/DefaultRoles.java)
is the reference set, and
[`WigSecurityService.java:51`](../server/catgenome/src/main/java/com/epam/catgenome/manager/wig/WigSecurityService.java)
is the `@PreAuthorize` that no PostgreSQL role could satisfy.

---

#### 1.7 On PostgreSQL, one migration will abort the start if a `TASK_ORGANISM` row holds non-numeric text

**What changed.** `TASK_ORGANISM.ORGANISM` and `TASK_EXCL_ORGANISM.ORGANISM` were `VARCHAR(250)` on
PostgreSQL and `BIGINT` on H2, while `BlastTask.organisms` is a `List<Long>` of NCBI tax ids. The new
migration casts the column with `USING ORGANISM::BIGINT`, deliberately unguarded: a row that is not a
number fails the migration loudly rather than being discarded.

**Who notices.** A PostgreSQL operator with BLAST tasks — and only if such a row exists, which it
should not, since every value ever written came from a `Long`.

**What they have to do.** If the start fails here, fix or delete the offending row and restart; the
migration is idempotent from NGB's point of view because Flyway will retry it.

**Evidence.** [`v2026.08.21_12.10__align_task_organism_with_h2.sql`](../server/catgenome/src/main/resources/database/catgenome/postgres/v2026.08.21_12.10__align_task_organism_with_h2.sql).

---

#### 1.8 Files registered from HDFS or GA4GH make their dataset unreadable

**What changed.** Resource types `HDFS(5)` and `GA4GH(6)` were removed in Phase 1. The numeric gap is
deliberately preserved — the ids are persisted in `BIO_DATA_ITEM.TYPE` and must not be reassigned —
and `getById` now throws `IllegalArgumentException` naming the removed type instead of returning
`null`.

**Who notices.** Any user opening a dataset that contains such a file: the row mapper throws while
reading it, so the whole call fails, not just that one item.

**What they have to do.** Find and unregister them **before** upgrading, and re-register from a
supported resource type if the data is still needed:

```sql
SELECT id, name, path, type FROM catgenome.bio_data_item WHERE type IN (5, 6);
```

**Evidence.** [`BiologicalDataItemResourceType.java:66-67`](../server/catgenome/src/main/java/com/epam/catgenome/entity/BiologicalDataItemResourceType.java)
(the deliberate gap), `:120-126, 135-142` (`getById` throwing, and `unsupportedTypeMessage` naming the removed
type), [`BiologicalDataItemDao.java:382`](../server/catgenome/src/main/java/com/epam/catgenome/dao/BiologicalDataItemDao.java)
and nine other row-mapper call sites across six DAOs (`git grep -c
"BiologicalDataItemResourceType.getById" -- server`: `BiologicalDataItemDao` 3,
`ReferenceGenomeDao` 3, `HeatmapDao`, `LineageTreeDao`, `PathwayDao`, `PdbFileDao` one each). **This is not documented anywhere** — see [D5](#d5).

---

#### 1.9 The server needs a JDK 21 host; the CLI needs 17

**What changed.** The jar is compiled for Java 21 and refuses to start on anything earlier with
`UnsupportedClassVersionError`. The CLI is built to a 17 toolchain. The docker image moved from
`ubuntu:16.04` + `openjdk-8-jre` to `eclipse-temurin:21-jre-jammy`; the JRE-bundled archives carry
Temurin 21.0.12+8 and therefore need glibc 2.17 or newer (RHEL 7 / Ubuntu 16.04 floor).

**Who notices.** Anyone running `java -jar` on a host JDK, and anyone whose base image is older than
jammy.

**What they have to do.** Install a Java 21 build, and add `--enable-native-access=ALL-UNNAMED` to the
command line — see 2.14 for what happens without it.

**Evidence.** [`standalone.md:13-15`](../docs/md/installation/standalone.md),
[`docker/core/Dockerfile:13`](../docker/core/Dockerfile),
[`server/ngb-cli/build.gradle`](../server/ngb-cli/build.gradle) (17 toolchain).

---

#### 1.10 WAR packaging is gone

**What changed.** Decision D2. The `war` plugin and the `catgenome.war` archive are gone; only the
executable jar and the docker image are built.

**Who notices.** Anyone deploying NGB into their own Tomcat. There is no replacement — Boot 3 targets
Tomcat 10 / jakarta, and the migration did not carry the WAR path forward.

**What they have to do.** Run the executable jar (embedded Tomcat 10) behind their existing reverse
proxy instead.

**Evidence.** `git show develop:server/catgenome/build.gradle` lines 43-45 (`apply plugin: "war"`,
`war { archiveName "catgenome.war" }`) against HEAD, where `server/catgenome/build.gradle` has no `war`
reference at all.

---

#### 1.11 `GET /saml/logout` no longer logs you out of the identity provider

**What changed.** Under the OpenSAML 2 extension, `GET /saml/logout` performed global single logout —
a `<LogoutRequest>` to the IdP, both sessions dropped. Spring Security 6's `Saml2LogoutConfigurer`
binds SLO to **POST** on that URL; the plain `LogoutConfigurer` registered on the same path handles
everything else, so a `GET` now clears the NGB session only and lands on `/`. The IdP session
survives, and the next request silently re-authenticates. `/saml/logout?local=true` was not ported —
a `GET` is now exactly that.

**Who notices.** Users on a shared machine (they appear to log out and are straight back in), and any
integration that links to or scripts `GET /saml/logout`.

**What they have to do.** Issue `POST /saml/logout`. The bundled client was changed to do so
([`client/client/utils/saml-logout.js`](../client/client/utils/saml-logout.js)); any other caller has
to be changed too.

**Evidence.** [`SAMLSecurityConfiguration.java:257-284`](../server/catgenome/src/main/java/com/epam/catgenome/app/SAMLSecurityConfiguration.java)
(`csrf` disabled, then `saml2Logout().logoutUrl("/saml/logout")` alongside
`logout().logoutUrl("/saml/logout").logoutSuccessUrl("/")`), and the live browser run through Chrome
DevTools against Keycloak recorded at
[`JAVA21-MIGRATION-PLAN.md:2059-2061, 2100+`](JAVA21-MIGRATION-PLAN.md), which drove the full POST
profile: `POST /catgenome/saml/logout` → `<LogoutRequest>` to the IdP → `<LogoutResponse>` to
`/catgenome/saml/SingleLogout` → `/`.

---

#### 1.12 SAML behind a load balancer: `saml.lb.*` is now ignored, silently

**What changed.** `saml.lb.enabled`, `saml.lb.scheme`, `saml.lb.server.name`,
`saml.lb.server.port`, `saml.lb.include.port.in.request`, `saml.lb.context.path` and
`saml.validate.url.without.scheme` existed so the extension could *derive* absolute endpoint URLs from
the incoming request. Spring Security builds them from absolute values instead — `saml.base.url`,
falling back to `server.ssl.endpoint.id` — so there is nothing to derive. The properties are not
merely unused: an unrecognised property is ignored without a word.

**Who notices.** Deployments terminating TLS at a proxy or load balancer, whose SP metadata,
assertion-consumer location and single-logout location must carry the externally visible URL.

**What they have to do.** Make sure `saml.base.url` (or `server.ssl.endpoint.id`) is the external
base URL, and set `server.forward-headers-strategy` if the application needs the forwarded scheme and
host for anything else. NGB ships **no** default for `server.forward-headers-strategy` — it is
mentioned only in a javadoc, not in any profile.

**Evidence.** `git grep -n "saml.lb" develop -- server/catgenome/src/main/java` → six `@Value`s in
`SAMLSecurityConfiguration` (lines 154-174) plus `saml.validate.url.without.scheme` at 151; none at
HEAD. [`SAMLSecurityConfiguration.java:125-131`](../server/catgenome/src/main/java/com/epam/catgenome/app/SAMLSecurityConfiguration.java)
records the reasoning, `:316` builds `baseUrl` from `samlBaseUrl` then `endpointId`.
`git grep -rn "forward-headers-strategy" HEAD -- server docs` returns only that javadoc line.

---

#### 1.13 Docker volumes: if you followed the old documentation, you were never persisting anything

**What changed.** Nothing in the image — and that is the point. The container has worked out of
`/opt/ngb` since at least v2.7.1, while the documentation told operators to mount `/opt/catgenome/H2`
and `/opt/catgenome/contents`. Those mounts were simply unused directories; the H2 database and the
parsed-file cache stayed inside the container and vanished with `docker rm`.

**Who notices.** Any operator who set up NGB from `docs/md/installation/docker.md` before 3.0.0. They
have no database to migrate — which is worth knowing before they spend a day on §1.1.

**What they have to do.** Mount `/opt/ngb/H2` and `/opt/ngb/contents`, as
[`docker.md:103-104, 121-126`](../docs/md/installation/docker.md) now says, and as
[`docker/core/Dockerfile:42-44`](../docker/core/Dockerfile) records at the `WORKDIR`. The note attached to
those lines explaining *why* the paths changed is wrong; see [D1](#d1).

**Evidence.** `git show v2.7.1:docker/core/Dockerfile` → `ENV INSTALL_DIR /opt/`,
`ENV NGB_HOME $INSTALL_DIR/ngb/`, `CMD cd $NGB_HOME && java -Xmx2G -jar catgenome.jar`; identical on
`develop`. `git show develop:docker/start-demo.sh:42-43` already mounted `/opt/ngb/contents` and
`/opt/ngb/H2`. The jar profile the image ships sets `database.jdbc.url=jdbc:h2:file:./H2/catgenome`
(`server/catgenome/profiles/jar/catgenome.properties:38`) — a *relative* path, so the database has
always been written under the working directory.

---

#### 1.14 `llm.custom.type=openai` with a self-hosted URL now really talks to that URL

**What changed.** `azure-ai-openai` 1.0.0-beta.2 → beta.16. Under beta.2, supplying a
`NonAzureOpenAIKeyCredential` selected a separate client implementation that stamped
`https://api.openai.com/v1` into every request, so the `.endpoint(url)` call in NGB's two-argument
constructor was **dead code**: `llm.custom.url` was, in effect, an on/off switch. beta.16 has one
client impl, honours the endpoint, and picks the auth header from the endpoint rather than the
credential class — so a non-`api.openai.com` URL now gets Azure-shaped requests
(`/openai/deployments/{model}/chat/completions?api-version=…`, header `api-key`).

**Who notices.** A deployment that set `llm.custom.type=openai` together with a self-hosted
OpenAI-compatible `llm.custom.url`. It was silently being served by api.openai.com and will now send
Azure-shaped requests at its own host, which will 404.

**What they have to do.** Move that configuration to `llm.custom.type=custom`, which is NGB's own
`CustomLLMApiClient` and is the OpenAI-compatible-proxy path. `llm.custom.type=openai` is for an
Azure OpenAI resource. The one-argument path — `OpenAIChatGPT35`, `OpenAIChatGPT40`, the providers
that actually get used — is unchanged.

**Evidence.** [`CustomLLMHandler.java:48, 57-61`](../server/catgenome/src/main/java/com/epam/catgenome/manager/llm/CustomLLMHandler.java)
is the dispatch — `type.equalsIgnoreCase("openai")` picks `CustomOpenAILLMClient`, anything else
(including the default, `custom`) picks `CustomLLMApiClient`. The before/after behaviour was read out
of both SDK versions' bytecode; the table is at
[`JAVA21-MIGRATION-PLAN.md:4049-4085`](JAVA21-MIGRATION-PLAN.md).

---

### Bucket 2 — visible change, no action needed

Different, but self-correcting or purely informational. Grouped by subsystem: database and startup,
URLs and authentication, configuration, cloud storage, the CLI, packaging, and — last, because there
is least to say about it — the web client.

#### Database and startup

**2.1 Flyway converts its own history table, and says so in the log.** The checksum algorithm changed
between Flyway 3.2.1 and 11.7.2 — `validate` fails with **51 of 59** mismatches, the same on both
flavours, with the scripts untouched — and Flyway 11 cannot write the Flyway 3 `schema_version` shape. `FlywayMigrator` detects the legacy `version_rank` column,
converts the table, runs `repair`, then `migrate` — no operator Flyway command, no baseline. Because
every other log4j2 appender filters at ERROR, a dedicated `migration-stdout` Console appender at INFO
is bound to `FlywayMigrator`, `LuceneIndexVersionCheck` and `LuceneIndexUtils` with
`additivity="false"`, so the WARN lines are actually visible.
*Evidence:* [`FlywayMigrator.java:44-74`](../server/catgenome/src/main/java/com/epam/catgenome/dao/FlywayMigrator.java)
(`LEGACY_RANK_COLUMN` at 86, `warn` at 116/122, `repair()` 121, `migrate()` 125);
`server/catgenome/profiles/{jar,release,staging}/log4j2.xml`;
[`JAVA21-MIGRATION-PLAN.md:2245-2250`](JAVA21-MIGRATION-PLAN.md) for the measured 51/59 and for why
repair is unavoidable on every existing database.

**2.2 Existing H2 migration scripts were edited in place.** Seven were fixed for H2 2.x syntax
(`catgenome.s_user.nextval` → `nextval('catgenome.s_user')`, a stray trailing comma). On an existing
database the versions are already applied, so only the checksums differ — which `repair` handles. Only
a fresh install runs the edited SQL.
*Evidence:* `git diff develop..HEAD -- server/catgenome/src/main/resources/database/catgenome/h2/`.

**2.3 `VCF.MULTI_SAMPLE` accepts NULL on PostgreSQL, and `BAM_COVERAGE.COVERAGE` is now `DOUBLE
PRECISION`.** Two more flavour divergences closed; existing rows keep their values.
*Evidence:* `v2026.08.21_12.00__align_vcf_multi_sample_with_h2.sql`,
`v2026.08.21_12.40__align_bam_coverage_with_h2.sql`.

**2.4 The `PERSON`, `PERSON_ROLE` and `S_PERSON` tables are dropped on first start.** No REST path
changed — the tables never had one (`git grep '@RequestMapping(value = "/person' develop` is empty),
and user and role management still answers on `/user`, `/users`, `/user/current`, `/user/{id}`,
`/role/**`.
*Evidence:* `v2026.08.20_12.00__drop_legacy_person_tables.sql` on both flavours; the removed
`person-dao.xml` import in `applicationContext-database.xml`;
[`UserController.java:68`](../server/catgenome/src/main/java/com/epam/catgenome/controller/user/UserController.java)
for `/user/current`.

**2.5 Every `database.*` property name and value is unchanged** across c3p0 → HikariCP:
`database.driver.class`, `database.jdbc.url`, `database.username`, `database.password`,
`database.max.pool.size`, `database.initial.pool.size`.
*Evidence:* `git diff develop..HEAD -- server/catgenome/profiles/*/catgenome.properties`;
`applicationContext-database.xml` maps them onto `HikariDataSource`.

#### URLs and authentication

**2.6 The API-docs spec moved; the UI path string did not.** The spec is at
`/catgenome/v3/api-docs` (OpenAPI 3.1) instead of Swagger 1's `/catgenome/api-docs`; the UI is at
`/catgenome/swagger-ui/index.html`, the same prefix as before, now served from springdoc's jar rather
than files committed under `src/main/resources` and `src/main/webapp`. Both are reachable without
authentication under SAML, as `/api-docs` was.
*Evidence:* [`OpenApiConfiguration.java:35-49`](../server/catgenome/src/main/java/com/epam/catgenome/config/OpenApiConfiguration.java);
`SAMLSecurityConfiguration.java:187-189` (`UNSECURED_RESOURCES = {"/swagger-ui/**",
"/v3/api-docs/**", "/error-401.html"}`) against `develop:.../SAMLSecurityConfiguration.java:207`,
which listed `/api-docs/**`.

**2.7 The SAML login URL is `/saml/login/ngb`.** It is the SP-internal endpoint NGB redirects an
unauthenticated browser to; it appears in no metadata and in no IdP registration, so nothing has to be
reconfigured on the IdP side. Every URL the IdP knows about — `/saml/metadata`, `/saml/SSO`,
`/saml/SingleLogout` — is unchanged.
*Evidence:* `SAMLSecurityConfiguration.java:153` (`AUTHENTICATION_REQUEST_URL = "/saml/login/" +
REGISTRATION_ID`), and the endpoint-by-endpoint table in its javadoc at 92-134.

**2.8 The post-login redirect gains `?continue`.** Spring Security 6's `HttpSessionRequestCache`
marks the replayed request with `matchingRequestParameterName=continue` by default. Cosmetic, and left
on deliberately: switching it off re-opens the redirect loop it exists to prevent.
*Evidence:* observed in the live Keycloak login — landing URL `…/catgenome/?continue#/`
([`JAVA21-MIGRATION-PLAN.md:2064, 2093`](JAVA21-MIGRATION-PLAN.md)).

**2.9 Published SP metadata now names the right signing certificate.** The extension signed messages
with `saml.sign.key` but generated metadata with the key store's default key — the HTTPS key — so
anything reading NGB's metadata to learn the message-signing key learned the wrong one. Spring
Security keeps one credential list for both, and it is `saml.sign.key`. An IdP configured *from* NGB's
metadata now gets the right certificate; one configured by hand needs no change. The property name is
unchanged.
*Evidence:* [`SAMLSecurityConfiguration.java:305-311, 320`](../server/catgenome/src/main/java/com/epam/catgenome/app/SAMLSecurityConfiguration.java);
verified with the JDK's XML-DSIG and then with Keycloak's `saml.client.signature = true`, i.e. the IdP
actually checking the `<AuthnRequest>` signature (`make saml-verify-signing`).

**2.10 `/saml/SSOHoK`, `/saml/discovery` and `/saml/web/**` are gone.** One IdP, no Holder-of-Key
support. `LOGOUT_RESPONSE_SKEW` has nowhere to go either — `OpenSaml4LogoutResponseValidator` does not
time-validate logout responses at all — while the assertion skew and
`saml.authn.max.authentication.age` are both still honoured, the latter by an extra validator.
*Evidence:* `SAMLSecurityConfiguration.java:119-134`.

**2.11 The role hierarchy now actually applies.** The old code called `RoleHierarchyImpl.setHierarchy`
sixteen times, and `setHierarchy` *replaces* rather than adds — so fifteen calls were discarded and
the only edge in effect was the last one written, `ROLE_SEG_MANAGER > ROLE_USER`. All fifteen intended
edges are in place now, built with `fromHierarchy(String)`. In practice: `ROLE_ADMIN` passes checks
written against `ROLE_USER` and the manager roles, and — because `SidRetrievalStrategyImpl` expands
authorities through the hierarchy — ACL entries granted to `ROLE_USER` now also apply to admins and
managers. The `" == "` edge from the old code is deliberately not reinstated: no version of
`RoleHierarchyImpl` ever parsed `==`, and the relation it described would have let a BAM manager act
as a VCF manager.
*Evidence:* [`AclSecurityConfiguration.java:130-168`](../server/catgenome/src/main/java/com/epam/catgenome/app/AclSecurityConfiguration.java).

**2.12 REST clients get 401 instead of a 302 to a login page.** Spring Security 6 filters ERROR
dispatches, so a `sendError(401)` from the JWT chain came back through the filters on `/error`, which
is outside `/restapi/**` and therefore landed on the SAML chain and became a redirect.
`dispatcherTypeMatchers(ERROR).permitAll()` fixes it. This is what makes `ngb-cli` usable against a
SAML+JWT server.
*Evidence:* `SAMLSecurityConfiguration.java:245-251, 262`.

**2.13 A JWT missing a required claim is now a 401, not a 500.** java-jwt 4.x returns a non-null
`Claim` for an absent claim, where 3.x returned one whose `isNull()` was true; without an `isMissing()`
check the claim list came back null and the required-claim check threw `NullPointerException`, which a
security filter turns into a 500. The signing algorithm is **unchanged** — RS512 in both versions — so
existing tokens and keys keep working.
*Evidence:* [`JwtTokenVerifier.java:88-108`](../server/catgenome/src/main/java/com/epam/catgenome/security/jwt/JwtTokenVerifier.java);
`Algorithm.RSA512` at `JwtTokenVerifier.java:73` and `JwtTokenGenerator.java:99`, identical on
`develop`.

#### Configuration

**2.14 `--enable-native-access=ALL-UNNAMED` is needed to keep the startup log quiet.** Lucene 9's
`MMapDirectory` calls `java.lang.foreign`, and Java 21 warns unless native access is granted. NGB
works either way and loses nothing else. The flag is already in the docker image, in the shipped
launchers and in the JRE-bundled archives; it has to be typed only for a hand-run `java -jar`.
*Evidence:* [`docker/core/Dockerfile:21-25`](../docker/core/Dockerfile);
the CI step that greps the container log for `restricted method` and fails the build
([`build.yml:217-223`](../.github/workflows/build.yml)).

**2.15 Removed properties are ignored in silence.** `server.index.cache.enabled` (2.16),
`llm.google.palm2.prompt.template`, and the `saml.lb.*` set (1.12) can be left in
`catgenome.properties` harmlessly, and nothing will say they do nothing.
*Evidence:* `git diff develop..HEAD -- server/catgenome/profiles/*/catgenome.properties`.

**2.16 The index cache is gone, and remote index reads are now per-request.** Decision D10: the
EhCache-backed `EhCacheBasedIndexCache` went with EhCache 2 and the htsjdk fork. Measured on a running
server against an HTTP file server: a `.tbi` for a VCF track is re-fetched on every request (141 bytes
for the test file), and a BAM's `.bai` is read three times per track load in full — **263,472 bytes per
request for an 88 kB index**, where the cache fetched it once. The cost is linear in requests and
scales with index size, not window size, so a human WGS `.bai` of 5–10 MB is paid on every pan. Local
files are unaffected (page cache). Nothing here is worse than the *first* request was before.
*Evidence:* [`JAVA21-MIGRATION-PLAN.md:3506-3536`](JAVA21-MIGRATION-PLAN.md) (the measurement, with
the fake server's request log as the source of the byte counts).

**2.17 The BLAT default is `https://genome.ucsc.edu/cgi-bin/hgBlat`.** The old default
`http://genome.cse.ucsc.edu/cgi-bin/hgBlat` no longer resolves to a usable endpoint. Neither works for
BLAT search: UCSC's public `hgBlat` answers a programmatic request with a bot-protection challenge
page, which NGB's PSL parser reads as zero hits. Not new in 3.0.0, and documented as a known issue.
*Evidence:* `git diff develop..HEAD -- server/catgenome/profiles/*/catgenome.properties`;
[`standalone.md:147-156`](../docs/md/installation/standalone.md).

**2.18 The log4j configuration file was renamed.** `log4j.xml` → `log4j2.xml`, because
`slf4j-log4j12` does not exist for slf4j 2.x: `dev`, `jar`, `release` and `staging` each have one,
and `desktop` went with the Electron packaging. An operator who shipped their own `log4j.xml` will
find it ignored — though nothing in the documentation ever mentioned the file.
*Evidence:* `git diff develop..HEAD --name-status -- server/catgenome/profiles`.

#### Cloud storage

**2.19 The AWS profile-file environment variable was renamed by the SDK.**
`AWS_CREDENTIAL_PROFILES_FILE` (v1) → `AWS_SHARED_CREDENTIALS_FILE` (v2). `AWS_ACCESS_KEY_ID`,
`AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `~/.aws/credentials` and `~/.aws/config` are read as before,
and a region was required under v1 exactly as it is under v2 — both versions swallow the failure and
log *"Unable to create S3 client, S3 services will be unavailable."*
*Evidence:* the strings in `regions-2.54.1.jar`, `sdk-core-2.54.1.jar` and
`aws-java-sdk-core-1.11.704.jar`, unpacked from the gradle cache volume;
[`S3Client.java:80-90`](../server/catgenome/src/main/java/com/epam/catgenome/util/aws/S3Client.java)
against `develop:...:69`. Note that `AWS_DEFAULT_REGION` is read by **neither** SDK — see
[D2](#d2).

**2.20 A schemeless `swift.stack.endpoint.url` is now assumed to be https.**
*Evidence:* [`S3Client.java:294-300`](../server/catgenome/src/main/java/com/epam/catgenome/util/aws/S3Client.java).

**2.21 Pre-signed S3 URLs work again, and a broken Azure path was fixed.** htsjdk 3.0 changed its
length probe from `GET` to `HEAD`, and `SeekableHTTPStream` records `contentLength = 0` on any failure
without complaining — so a URL whose `HEAD` is refused looked like an empty file and the codec reported
`We never saw the required CHROM header line`. Restored with `IOHelper.getContentLength` (a `GET`,
disconnected after the headers) and a new `UrlSeekableStream`, routed only for URLs matching
`isSignedS3Url`; every other `http(s)` URL stays on stock htsjdk. Separately, removing the index cache
exposed that the **uncached** branch of the Azure BAM index fetcher passed an `az://` path to
`S3Client` and could only ever have failed — hit by the first request after any restart. It now uses
`azureBlobClient.loadFully`.
*Evidence:* [`JAVA21-MIGRATION-PLAN.md:3468-3500`](JAVA21-MIGRATION-PLAN.md), verified against a
staged 403 (`.devenv/scripts/fake-remote-files.py`): `HEAD → 403`, then `GET → 200` for the length,
then ranged `206`s.

**2.22 A plain, uncompressed feature file in cloud storage still cannot be read.** VCF, BED, GFF/GTF,
SEG and BEDGRAPH addressed by `s3://`, `sws://` or `az://` must be bgzipped and tabix-indexed, or the
read fails with `No FileSystemProvider available to handle path:`. Not new; now documented.
*Evidence:* [`standalone.md:281-287`](../docs/md/installation/standalone.md).

#### The CLI

**2.23 Command set, output format, configuration location and console log format are unchanged.**
The Java floor is 17, and `ngb-cli.tar.gz` still unpacks to `ngb-cli/bin/ngb` (plus `ngb.bat`) with
`ngb-cli/config/server.properties` — 21 jars, 9,737,929 bytes as built here. The one functional
difference is that `HDFS` and `GA4GH` are no longer resource types (1.8).
*Evidence:* `tar -tzf server/ngb-cli/build/distributions/ngb-cli.tar.gz`;
`server/ngb-cli/src/main/resources/log4j2.xml` and `app/ConfigurationLoader.java` are unchanged from
`develop`; [`server/ngb-cli/build.gradle:118-145`](../server/ngb-cli/build.gradle)
(`applicationName = 'ngb'`, `archiveBaseName = 'ngb-cli'`, `archiveVersion = ''`, and the comment
explaining why the version is blanked rather than the file name pinned); `make cli-test` 129/129.

**2.24 The documented CLI download URL is a versioned one.** The `latest` alias in the old
instructions was NXDOMAIN.
*Evidence:* [`docs/md/cli/installation.md:22-45`](../docs/md/cli/installation.md).

#### Packaging, image, CI

**2.25 Artifact names are unchanged:** `catgenome.jar` (the H2 flavour, under its historical name),
`catgenome-h2.jar`, `catgenome-psql.jar`, `ngb-cli.tar.gz`, `ngb-docs.tar.gz`. They still land in
`s3://ngb-oss-builds/public/builds/<branch>/<version>/`, versioned in the filename, and the image is
still pushed only from a `release/*` branch as `<namespace>/ngb:<version>` — with no `latest` tag,
as before.
*Evidence:* [`build.sh:3-11`](../build.sh), [`publish.sh:65-79`](../publish.sh) against
`git show develop:publish.sh`.

**2.26 Version strings restart their build counter.** AppVeyor's `version: 2.8.0.{build}` counter is
replaced by `github.run_number`, which starts at 1 — so `3.0.0.1` follows `2.8.0.4384`. Release
publishing now needs three repository secrets configured (`AWS_PUBLISH_ROLE_ARN` — an IAM role trusted
via GitHub's OIDC provider, replacing AppVeyor's long-lived key pair — plus `DOCKERHUB_USERNAME` and
`DOCKERHUB_TOKEN`), and `vars.DOCKER_NAMESPACE` replaces AppVeyor's `DOCKER_USER` doubling as the
image namespace. Without `AWS_PUBLISH_ROLE_ARN` the publish job is skipped and a fork builds and tests
with nothing configured.
*Evidence:* [`.github/workflows/build.yml:21-32, 328-378`](../.github/workflows/build.yml),
`git show develop:.appveyor.yml`.

**2.27 The JRE-bundled archives are new and actually work.** `ngb-server-linux.tgz` (226 MB) and
`ngb-server-windows.zip` carry Eclipse Temurin 21.0.12+8, so they start on a machine with no Java.
The launcher uses `jre/` and ignores the `PATH`, and already passes
`-Xms512m -Xmx2g --enable-native-access=ALL-UNNAMED`; `NGB_SERVER_OPTS` is **appended**, so raising the
heap is one setting. The Windows archive deliberately ships only `bin\ngb-server.bat` — the POSIX
script `CreateStartScripts` generates points at a path that does not exist in a Windows JRE. Previous
releases downloaded a JRE 8 from a URL Oracle removed in 2019, and the launcher they generated could
not have found the bundled runtime anyway. These archives are built in their own CI job, on release
branches and manual runs only, and `build.sh` does not produce them.
*Evidence:* [`server/catgenome/build.gradle:645-827`](../server/catgenome/build.gradle);
[`build.yml:256-326`](../.github/workflows/build.yml) (started in a container with no JDK, and the
Windows zip's contents asserted); [`standalone.md:80-107`](../docs/md/installation/standalone.md).

**2.28 nginx is gone from the docker image.** It was installed, never configured and never started in
any commit since 2021. TLS termination, authentication and routing belong in a reverse-proxy container
in front of NGB.
*Evidence:* [`docker/core/Dockerfile:10-13`](../docker/core/Dockerfile) against
`git show develop:docker/core/Dockerfile:10-11`.

**2.29 `NGS_DATA_DIR` is a runtime setting now, and `NGB_JAVA_OPTS` holds the JVM flags.** The old
image baked `ngs.data.root.path` into `config/catgenome.properties` at **build** time, so
`-e NGS_DATA_DIR=/data` was a silent no-op. A new `entrypoint.sh` writes that file at start time from
`NGS_DATA_DIR` (default `/ngs`) unless one is already mounted, so a mounted
`/opt/ngb/config/catgenome.properties` — or the whole `/opt/ngb/config` directory — still wins.
`NGB_JAVA_OPTS` defaults to `-Xmx2G --enable-native-access=ALL-UNNAMED`; overriding it **replaces**
both flags, so an operator raising the heap has to keep the second one or the startup warnings come
back.
*Evidence:* [`docker/core/entrypoint.sh`](../docker/core/entrypoint.sh),
[`docker/core/Dockerfile:15-25, 37-49`](../docker/core/Dockerfile).

**2.30 The demo image and `start-demo.sh` read their data from S3 and stage it under `/ngs`.** The old
data host `ngb.opensource.epam.com` stopped resolving, and the references and demo data were placed
outside `ngs.data.root.path` — so the published demo image answered *"Parameter path doesn't fall into
'ngs.data.root.path'"* to its own registration commands. The demo image is also now buildable in a
narrowed form (`--build-arg REFERENCES=dm6 --build-arg DEMO_DATA=false`) instead of 3.4 GB, and the
`ngb-demo-index-cache.tar.gz` download is gone with the index cache.
*Evidence:* [`docker/demo/Dockerfile:13-26, 59-62`](../docker/demo/Dockerfile),
[`docker/start-demo.sh:7-10, 54-59`](../docker/start-demo.sh) against
`git show develop:docker/start-demo.sh:41-46`.

**2.31 Building from source needs more than a JDK.** `./gradlew buildJar` builds the client with
Node 14.17.5 and the documentation with mkdocs (pinned `mkdocs==1.5.3`, `mkdocs-material==9.5.3`).
Taking a prebuilt jar avoids both.
*Evidence:* [`standalone.md:42-45`](../docs/md/installation/standalone.md),
[`build.yml:167-184`](../.github/workflows/build.yml).

#### The web client

Nine files changed, and only these three are visible to a user. No track type, panel, dialog or
keyboard behaviour was touched (D12).
*Evidence:* `git diff develop..HEAD --stat -- client` — `build.gradle`, `package.json`,
`package-lock.json` and `webpack.config.js` are build plumbing.

**2.32 The LLM model picker no longer lists "Google PaLM2".** The provider went with D5 (Google
retired the API), so the entry is gone from the model list, from the display-name map and from the
per-model property list. `ChatGPT 3.5`, `ChatGPT 4.0` and `CUSTOM` are unchanged. A user who had
PaLM2 selected gets the picker's default; nothing is persisted server-side.
*Evidence:* `git diff develop..HEAD -- client/client/app/shared/components/ngbLLM/models.js` — three
hunks, all removals of `LLM.googlePalm2`.

**2.33 Logging out, and a 401 mid-session, submit a form instead of navigating.** Both call sites that
used to assign `window.location = …/saml/logout` now build and submit a POST form, because a `GET`
no longer performs single logout (1.11). Same URL, same outcome as before; a browser that blocks
form submission to the app's own origin would break, which none do.
*Evidence:* `ngbMainToolbar.component.js:44` and `data-service.js:187` both call
[`submitSamlLogout`](../client/client/utils/saml-logout.js).

**2.34 The desktop-only branch in the main toolbar is gone.** `process.env.__DESKTOP__` guarded
dropping the share-link components from the toolbar; the Electron packaging went with D5, and in a
browser build the flag was always false, so the surviving branch is the one that always ran.
*Evidence:* `git diff develop..HEAD -- client/client/app/shared/components/ngbMainToolbar/index.js`.

---

### Bucket 3 — alarming in the diff, internal

One line each, so a reviewer stops worrying.

- **The bulk of the 581-file diff is import lines.** 89 server files import `jakarta.*` at HEAD where
  107 imported `javax.*` on `develop`; the 89 are mechanical Phase 3 rewrites, and the `javax.*`
  imports that remain are the ones that were never renamed — `javax.xml` (39), `javax.net` (11),
  `javax.sql` (4), `javax.naming` (3). *Evidence:* `git grep -l "^import jakarta\." -- server | wc -l`
  = 89; `git grep -l "^import javax\." develop -- server | wc -l` = 107.
- **`@Required` is gone from 22 files** (Spring 6 removed the annotation). A DAO whose query is
  missing from XML no longer fails at startup — it NPEs on first use. No shipped configuration is
  affected. *Evidence:*
  `git grep -l "import org.springframework.beans.factory.annotation.Required" develop -- server | wc -l`
  = 22, the same command at HEAD = 0.
- **EhCache 2.10.1 → Caffeine** (D11). Same two cache regions, same names; EhCache 2 cannot run on
  JDK 21 (`InaccessibleObjectException`).
- **c3p0 → HikariCP** with every `database.*` property name kept, so no configuration moved (2.5).
- **The htsjdk fork of four reader classes was deleted** in favour of stock 5.0.0; parity for
  pre-signed S3 URLs was restored deliberately and narrowly (2.21).
- **`lucene-backward-codecs` is absent on purpose** — it buys 8 → 9 and would have made the startup
  guard's failure mode less honest, not more useful.
- **Two JAXB stacks are co-resident** deliberately: the tree has both generated bindings and runtime
  users, and separating them was out of scope.
- **NGB's own code no longer imports `commons-lang` 2 or Bouncy Castle** — both were accidental
  transitives (of the mangofactory Swagger stack and of OpenSAML), never declared, and 12 files plus
  `GenePredUtils` imported them anyway. The imports moved to the lang3 equivalents,
  `ThreadLocalRandom` and `Locale.ROOT` lowercasing — same methods, same semantics.
  *Evidence:* [`server/catgenome/build.gradle:247-255`](../server/catgenome/build.gradle).
  **Neither library left the build, and an earlier version of this bullet claimed they had**
  (corrected after [`JAVA21-VULNERABILITY-REVIEW.md`](JAVA21-VULNERABILITY-REVIEW.md) §6 found it):
  - **Bouncy Castle is still on the server classpath**, under `opensaml-security-api:4.3.2` and
    `cryptacular:1.2.5` — OpenSAML 4 supplies it under the renamed `jdk18on` artifact ids, which is
    presumably how it was missed. It shipped at 1.72 with 1 CRITICAL and 9 MEDIUM advisories against
    it until `1dc32276` pinned `bcprov`/`bcpkix`/`bcutil-jdk18on` to 1.84.
  - **`commons-lang` 2 left the *server* tree only.** The CLI still resolves it at 2.6, under the
    hand-declared `commons-configuration:1.10` (`server/ngb-cli/build.gradle:69`), and it carries
    CVE-2025-48924 with no fix on the 2.x line — see the review's §3.2 and §7.7.
- **`commons-collections-3.2.2.jar` is in the server fat jar as well as the CLI's.** The
  `runtimeOnly` pin below is a CLI matter, but on the server the same artifact arrives as a
  transitive and sits next to `commons-collections4-4.6.0.jar` and `collections-generic-4.01.jar`.
  3.2.2 is the release that disables the serialisation gadget by default, so it is not a finding —
  it is only invisible in a summary that discusses 3.2.2 as CLI-only.
- **`commons-validator` is pinned at 1.5.0 (2016) knowingly.** 1.7 tightened the query-string charset
  to RFC 3986, and NGB's own generated URLs contain `{ } " |` unencoded — so `POST /generateShortUrl`
  and `ngb url --alias` would start rejecting the format documented in
  `docs/md/user-guide/embedding-url.md`. The tripwire is a named test
  (`UrlShorterManagerTest…AcceptTheDocumentedEmbeddingUrlFormat`); no CVE is outstanding against
  1.5.0. *Evidence:* [`server/catgenome/build.gradle:267-280`](../server/catgenome/build.gradle).
- **`FlywayAutoConfiguration` is excluded in `Application.java`** so `FlywayMigrator` owns the
  conversion order (2.1).
- **`NGBMethodSecurityExpressionHandler` overrides the `Supplier<Authentication>` overload** — the
  signature Spring Security 6 calls; the old protected override is no longer on the call path.
  *Evidence:* [`security/acl/customexpression/NGBMethodSecurityExpressionHandler.java:41-61`](../server/catgenome/src/main/java/com/epam/catgenome/security/acl/customexpression/NGBMethodSecurityExpressionHandler.java).
- **`server/ngb-cli/build.gradle` carries `runtimeOnly commons-collections:3.2.2`** on purpose:
  dropping it makes every `ngb` invocation fail in `ConfigurationLoader.loadXmlConfiguration`.
- **The `regression` Gradle task and `google-http-client` were deleted from the CLI**; neither was
  reachable.
- **`checkstyleTest`/`pmdTest` are not run in CI.** Neither module has ever had its test sources
  linted; main is clean at 0 errors / 37 warnings and the warning count is recorded so a rise is
  visible.

---

## 3. Defects and documentation errors

Found while writing this summary, and **left alone** as the task requires. Each is a place where a
document asserts something the code at `78dffedc` does not do, or a real bug recorded and deliberately
not fixed.

<a id="d1"></a>
### D1. The docker working directory did not move — the old documentation was simply wrong

`docs/md/release-notes/3.0.0/3.0.0.md:87-89` says *"The docker image works out of `/opt/ngb`, not
`/opt/catgenome`"*, and `docs/md/installation/docker.md:106-108` says *"these were
`/opt/catgenome/H2` and `/opt/catgenome/contents` up to NGB 2.7.1. The image works out of `/opt/ngb`
from 3.0.0 on"*. Both are false. `git show v2.7.1:docker/core/Dockerfile` sets
`NGB_HOME=$INSTALL_DIR/ngb/` with `INSTALL_DIR=/opt/` and runs `cd $NGB_HOME && java … -jar
catgenome.jar`; `develop` is identical; `git show develop:docker/start-demo.sh:42-43` already mounted
`/opt/ngb/H2` and `/opt/ngb/contents`. The `/opt/catgenome` paths existed only in the documentation,
which means operators who followed it were never persisting anything. The corrected volume paths in
`docker.md` are right; the explanation attached to them is not, and it turns a pre-existing
documentation bug into an invented behaviour change. See 1.13 for the version an operator needs.

<a id="d2"></a>
### D2. `AWS_DEFAULT_REGION` is read by neither AWS SDK version

`docs/md/release-notes/3.0.0/3.0.0.md:99-102` tells operators a region *"has to be resolvable, from
`AWS_DEFAULT_REGION`, the config file or the instance metadata"*, and
`docs/md/installation/standalone.md:264-277` instructs `export AWS_DEFAULT_REGION=us-east-1`. The
string does not occur in `regions-2.54.1.jar` or `sdk-core-2.54.1.jar`, nor in v1's
`aws-java-sdk-core-1.11.704.jar` — checked by unpacking all three from the gradle cache volume. v2
reads `AWS_REGION` (`SdkSystemSetting`, `SystemSettingsRegionProvider`); v1's
`SDKGlobalConfiguration` has only `AWS_REGION_SYSTEM_PROPERTY` / `AWS_REGION_ENV_VAR` / `AWS_REGION`.
`AWS_DEFAULT_REGION` is an AWS **CLI** variable. The instruction was wrong before this release too,
so anyone following it was falling through to `~/.aws/config` — but the release note now presents it
as current guidance for the SDK v2 chain.

<a id="d3"></a>
### D3. "PostgreSQL 9.6 – 17" is a documentation claim with no test behind it

`docs/md/release-notes/3.0.0/3.0.0.md:45`, `docs/md/installation/standalone.md:127` and
`docs/md/installation/database-upgrade.md:11` all state a supported range of 9.6 to 17. The only
server ever exercised on this branch is **PostgreSQL 16.15** (`.devenv`, decision D3) and 16 in CI
(`build.yml:122-137`); the upgrade procedure was verified for 9.6 → 16 only. 17 is a plausible
inference from the driver's support matrix, not a tested claim, and 9.6 is a *source* version for the
dump, not a version NGB was run against.

<a id="d4"></a>
### D4. GA4GH was never in the client

`docs/md/release-notes/3.0.0/3.0.0.md:67` says *"Registration of GA4GH datasets is gone from the
client and the API"*. `git grep -il ga4gh develop -- client` returns nothing: on `develop` GA4GH
existed only server-side (`controller/vo/ga4gh/**`, `manager/vcf/reader/VcfGa4ghReader`,
`Ga4ghResourceUnavailableException`) and in the CLI's resource-type enum. Harmless, but it credits the
migration with a client change that never happened and that D12 would not have allowed.

<a id="d5"></a>
### D5. Nothing tells an operator what happens to existing HDFS or GA4GH rows

The release note says `hdfs://` paths *"can no longer be registered"* and that GA4GH registration is
gone — both true, and both about *new* registrations. Neither it nor `database-upgrade.md` mentions
that a row already holding `BIO_DATA_ITEM.TYPE` 5 or 6 now throws `IllegalArgumentException` in the
row mapper and takes the whole enclosing call with it, nor gives a query to find such rows. The code
went to the trouble of naming the removed type in the message
(`BiologicalDataItemResourceType.unsupportedTypeMessage`, javadoc: *"the operator has to be told which
files to re-register"*), and then the operator documentation does not carry it. See 1.8.

<a id="d6"></a>
### D6. `NgbFileUtils.isRemotePath` tests for `ftsp:`, not `ftp:`

[`NgbFileUtils.java:174-175`](../server/catgenome/src/main/java/com/epam/catgenome/util/NgbFileUtils.java):
`path.startsWith("http:") || path.startsWith("https:") || path.startsWith("ftsp:")`. One transposed
character, present long before this migration, which means every caller — including
`FastaUtils.getContentLength(String)` and `FastaUtils.openBufferedReader` — has always treated an
`ftp://` path as a local file. FTP references have never worked the way the docs imply. Noticed while
tracing htsjdk call sites in Phase 7 and left alone, because fixing it is a behaviour change.

<a id="d7"></a>
### D7. Properties read through `#{catgenome[…]}` cannot be overridden on the command line

`file.browsing.allowed`, `ngs.data.root.path`, `use.embedded.tomcat`, `version`,
`files.download.*`, `search.features.max.results` and about twenty more are read through a SpEL
lookup into a `PropertiesFactoryBean` named `catgenome`, not through Spring's `Environment`. That bean
reads the classpath copy, `${CATGENOME_CONF_DIR}`, `./config` and `${conf}` — so `--conf=/dir` works,
but `--ngs.data.root.path=/data` on the command line, or the same key in `application.properties`, is
**silently ignored**. This is pre-existing (`applicationContext-config.xml` is byte-identical to
`develop`) and undocumented; it is listed here because it is the shape of silent degradation the task
asked after, and because `docs/md/installation/standalone.md` documents command-line overrides two
sections away from properties that do not accept them.
*Evidence:* `git grep -rn "catgenome\[" -- server/catgenome/src/main` → 39 sites in 18 files, e.g.
[`FileManager.java:302-305`](../server/catgenome/src/main/java/com/epam/catgenome/manager/FileManager.java),
[`AppMVCConfiguration.java:65-74`](../server/catgenome/src/main/java/com/epam/catgenome/app/AppMVCConfiguration.java).

<a id="d8"></a>
### D8. The LLM client sends a header literally named `bearer`

`CustomOpenAILLMClient`'s two-argument path adds `new Header("bearer", key)` through `clientOptions`,
carrying the raw key, additive to whatever the credential policy sets. Pre-existing, almost certainly
a mangled `Authorization: Bearer`, unchanged by the SDK bump, and left alone
([`JAVA21-MIGRATION-PLAN.md:4088-4091`](JAVA21-MIGRATION-PLAN.md)).

<a id="d9"></a>
### D9. `GOOGLE_MED_PALM2` is still an accepted LLM provider with no implementation behind it

`LLMProvider` at HEAD is `OPENAI_GPT_35, OPENAI_GPT_40, GOOGLE_MED_PALM2, CUSTOM`. Phase 1 removed
`GOOGLE_PALM_2` — the one that had a handler and a client entry — but `GOOGLE_MED_PALM2` stayed, and
nothing in `server` or `client` refers to it: no handler is registered under it, so
`LLMService.getHandler` fails its `Assert.notNull` with *"GOOGLE\_MED\_PALM2 is not supported."*
Since it is a valid enum value, the API deserialises it and answers 500 rather than 400. Pre-existing
and unchanged — it was already an orphan on `develop` — and it is not offered by the client, so only a
direct API caller can reach it. *Evidence:*
[`LLMProvider.java:28`](../server/catgenome/src/main/java/com/epam/catgenome/entity/llm/LLMProvider.java),
`git grep -rn "GOOGLE_MED_PALM2" -- server client` (one hit, the declaration),
[`LLMService.java:99-102`](../server/catgenome/src/main/java/com/epam/catgenome/manager/llm/LLMService.java).

---

## 4. Not verified

Claims that could not be settled against reality on this host, and why. None of them is asserted
above.

- **The GitHub Actions workflow has never executed.** `.github/workflows/build.yml` is
  actionlint-clean and every job mirrors a `make` target that passes locally, but no run exists —
  the branch has not been pushed. actionlint validates syntax, not behaviour; the likely first
  failures are the Node 14.17.5 `cache: npm` step and JDK toolchain discovery in
  `.github/actions/setup-jdks`.
- **The Windows bundle has never been started.** No Windows host. CI asserts only that
  `ngb-server-windows.zip` contains `jre/bin/java.exe`, `lib/catgenome.jar` and
  `bin/ngb-server.bat`, and that it does *not* contain a POSIX launcher.
- **The x86_64 bundles have never been started.** The development host is aarch64, so only
  `-PbundleArch=aarch64` was run end to end. The four Temurin checksums are pinned and on record; the
  published artifacts are x64.
- **`az://` was never exercised against a live service.** `AzureBlobClient` builds
  `https://%s.blob.core.windows.net` from a hard-coded template, so Azurite cannot stand in, and no
  Azure subscription was available. The Azure SDK bump (12.14.0 → 12.35.0, identity 1.4.4 → 1.18.4)
  is therefore compile- and unit-verified only. The one Azure code path that was *reasoned* about and
  changed — the uncached BAM index fetch (2.21) — is also unverified against a live account.
- **The LLM round trip was never exercised.** No API keys. 1.14 is read out of both SDK versions'
  bytecode, not observed on the wire.
- **`sws://` (Swift) was not exercised against a live endpoint** either; the schemeless-URL change
  (2.20) is a code reading.
- **MAF has no REST surface at all**, so the MAF row of the track verification suite is skipped and
  nothing about MAF behaviour after the migration is verified.
- **PostgreSQL 17 was never run** (see [D3](#d3)), and neither was any PostgreSQL between 9.6 and 16.
- **Nothing publishes the JRE-bundled archives.** `build.sh` does not produce them and `publish.sh`
  only uploads what it finds in `dist/`; the `bundles` CI job attaches them to the run as artifacts.
  A release that is expected to offer them needs a step that does not exist yet.
- **No upgrade was performed on a production-sized database or index set.** The H2 and PostgreSQL
  conversions were exercised against a purpose-built pre-migration fixture (60 `schema_version` rows,
  real ACL and biological-data-item content); timings and failure modes at real scale are unknown.
