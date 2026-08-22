# NGB containerised dev environment

A self-contained environment for working on NGB, with no JDK, Node, Gradle or Python
installed on the host. Everything runs in containers: the build toolchain, the application,
both database flavours, a real SAML identity provider and an S3-compatible object store.

It was written for the **Java 21 migration**, which has now landed — Phases 0–9 are in
`JAVA21-MIGRATION-PLAN.md` and `JAVA21-MIGRATION-EXECUTION.md`, kept as the record of what
was changed and why. What is left here is a working environment, not migration scaffolding:
the JDK-switching, the two database flavours, the SAML stack, the MinIO profile and the three
`verify-*` scripts are all things the ordinary loop needs.

```
cd .devenv
make init          # dirs + .env, prints the /etc/hosts line you need
make image         # build the toolbox image (~10 min first time)
make jar           # build dist/catgenome-h2.jar
make up            # run it -> http://ngb.dev.local:8080/catgenome
```

`make help` lists every target.

## Why it looks like this

Three facts about the current codebase drive the whole design:

1. **The build's JDK is not the host's JDK.** The wrapper was pinned to Gradle 3.3 on JDK 8
   when this environment was written; it is now Gradle 8.14.5, the server module builds on a
   **JDK 21** toolchain and `server/ngb-cli` on 17. The image's default is 21, so nothing has
   to select a JDK for ordinary work; it carries **two** (`with-java17` / `with-java21`, or
   `use-java 17|21` in a shell) because ngb-cli's toolchain is the older one. The app
   container picks its JDK at runtime via `NGB_JAVA_VERSION`, which defaults to 21 — the jar
   is Java 21 bytecode and will not load on anything older. The JDK 8 that used to be here as
   well, and be the default, went with migration Phase 9.
2. **The database flavour is chosen at build time**, not at runtime
   (`-Pdatabase=h2|postgres` swaps `applicationContext-flyway.xml`), so there are two
   jars and two app services.
3. **SAML compares URLs byte-for-byte.** Every service therefore listens on the *same*
   port inside and outside its container, and hostnames go through `/etc/hosts` rather
   than `localhost`.

## Services

| Service | Profile | Purpose | Endpoint |
|---|---|---|---|
| `builder` | default | Toolbox: JDK 21 + 17, Node 14.17.5, mkdocs, muscle. All Gradle/npm work happens here | — |
| `certs` | default | One-shot: JKS keystore (HTTPS + SAML signing) and the JWT RSA keypair | — |
| `ngb-h2` | default | NGB on H2 | `:8080` http *or* `:8443` https |
| `ngb-pg` | `pg` | NGB on PostgreSQL | `:8090` http *or* `:8493` https |
| `postgres` | `pg` | PostgreSQL 16 (`PG_VERSION`, see note below), DBs `ngb` + `ngb_test` | `:5432` |
| `test-pg` | `pg` | Unit tests against PostgreSQL | — |
| `idp` | `saml` | Keycloak 26 as SAML 2.0 IdP, realm `ngb` preloaded | `:8081` |
| `idp-metadata` | `saml` | One-shot: pulls the IdP descriptor into `secrets/` | — |
| `cli` | `cli` | `ngb-cli` wired to the running server | — |
| `minio` | `cloud` | S3-compatible object store, so `s3://` and `sws://` work without an AWS account | `:9000` api, `:9001` console |
| `minio-init` | `cloud` | One-shot: creates the bucket and uploads the track fixtures | — |

Host entries (`make hosts` prints this):

```
127.0.0.1 ngb.dev.local ngb-pg.dev.local idp.dev.local
```

## Typical loops

**Build and run on H2**

```bash
make jar            # full: client bundle + mkdocs + server, tests skipped
make up
make logs
```

`make jar-fast` rebuilds only the server module (skips the UI bundle and mkdocs) once
you've done one full build — that's the loop you'll use while changing Java code
(~45 s versus ~7 min). `make jar-pg-fast` is the same shortcut for the PostgreSQL jar.

**Tests**

```bash
make test                    # all server unit tests on H2
make test-one T=VcfManagerTest
make test-pg                 # same suite against PostgreSQL
make lint                    # checkstyle + pmd
make cli-test                # CLI<->server integration suite, all 145 rows
```

`make cli-test` needs `make jar cli-build` first and starts a server of its own on port 8080,
so `make stop` before you run it. Its fixtures are generated from the server module's test
resources by `e2e/cli/prepare_test_data.sh` — until migration Phase 9 they were downloaded
from a host that had stopped resolving, which is why the suite had been unrunnable for years.

**Read [`TEST-BASELINE.md`](TEST-BASELINE.md) before you trust a red run.** `make lint` is
green and so is `make cli-test` (129 passed, 0 failed, 16 skipped); the unit suite is 545 tests
with **1 failure on each flavour** — `GffManagerTest.testLoadGenesTranscript`, which asserts on a
biotype Ensembl returns over REST. A second failure in `PdbDataManagerTest.testParse` is also a
pass: it reads a live PDB entry and flaps. That file records both and why, so you can tell new
breakage from old, and it lists the two preconditions the numbers depend on: `make test-pg` wants
`make reset-pg` first, and `make test` wants an empty `../contents/`.

**Three checks the unit suite cannot give you**

The unit suite never boots a server: it drives the managers directly, against a Spring test
context and a temporary contents directory. So there are three scripts that register data over
REST against a **running** instance and print what came back. They were written for migration
Phases 6, 7 and 8, and they are kept — they are the only thing that exercises the parsers, the
Lucene indexes and the S3 client end to end.

```bash
make verify-tracks    # a track of every type: BED, GFF/GTF, GenePred, VCF, BedGraph, BigWig,
                      # SEG, BAM, CRAM, tabix, and remote URLs including one whose HEAD is 403
make verify-lucene    # all 18 Lucene-backed read paths: search, targets, coverage, taxonomy
make verify-cloud     # s3:// and sws:// out of MinIO, diffed against a local read
```

Each takes a base URL as its first argument (`bash scripts/verify-lucene.sh
http://localhost:8090/catgenome` points one at the PostgreSQL instance). `verify-tracks` wants
`scripts/prepare-track-fixtures.sh` run once; `verify-lucene` wants the registered data
described in [`fixtures/README.md`](fixtures/README.md), and a missing prerequisite shows up as
an empty result — so read the counts, not just the exit code. Run all three after touching a
parser, an index or a launcher.

**Which JDK the server runs on**

```bash
make up                        # JDK 21 — the only version that runs the jar
NGB_JAVA_VERSION=17 make up    # UnsupportedClassVersionError: the jar is class file 65
```

The switch is kept even though only one value works, because the image does carry a second
JDK for `server/ngb-cli`, and because `JAVA_VERSION=8` now says so (`must be 17 or 21`)
instead of pointing the launcher at a directory Phase 9 deleted.

`JAVA_EXTRA_OPTS` in `.env` is **empty** and should stay that way. It used to carry four
`--add-opens` for EhCache 2's reflective heap sizing; Phase 3 replaced EhCache with Caffeine and
dropped them. A new `--add-opens` requirement appearing here is a signal — on Spring 6 it usually
means a stale dependency, not a JDK problem.

Historical note, since it is what this environment was built to demonstrate: the original JDK
8-built jar does **not** boot on JDK 21 — Spring 4.3's cglib dies during context refresh with
`InaccessibleObjectException: Unable to make protected final java.lang.Class
java.lang.ClassLoader.defineClass(...) accessible` at `ReflectUtils.<clinit>`. That was the runtime
wall on top of the two build walls. All three are behind us as of Phase 3.

**SAML SSO**

```bash
make up-saml     # Keycloak + NGB over HTTPS
make smoke-saml  # browser-less end-to-end login, asserts the authorities NGB derived
```

`make smoke-saml` walks the whole web SSO profile with no browser and prints what NGB
made of the assertion, so you can verify SAML from a terminal (and it works before the
`/etc/hosts` entries exist, because it runs inside the docker network):

```
[3] POST SAMLResponse to https://ngb.dev.local:8443/catgenome/saml/SSO
  username    : NGBADMIN@NGB.DEV.LOCAL
  authorities : ['ROLE_USER', 'ROLE_ADMIN', 'NGB_ADMINS']
SAML SSO OK
```

Use `make smoke-saml U=ngbuser@ngb.dev.local P=user` for the non-admin user. This is the
check that tells you the SAML stack still works after touching it.

When it *doesn't* work, the packaged `log4j2.xml` is the first obstacle: its console appender
is pinned to `ERROR`, so Spring Security says nothing. Point the app at the debug config in
this directory instead — it logs `org.springframework.security` at DEBUG and Boot's servlet
filter mappings, which is what most SAML failures come down to:

```bash
NGB_JAVA_VERSION=21 AUTH_MODE=saml \
  JAVA_EXTRA_OPTS="-Dlogging.config=file:/opt/ngb/bin/log4j2-debug.xml" \
  docker-compose up -d --force-recreate ngb-h2
```

Then open <https://ngb.dev.local:8443/catgenome> (accept the self-signed certificate) and
sign in as `ngbadmin@ngb.dev.local` / `admin` (NGB admin, via `security.default.admin`)
or `ngbuser@ngb.dev.local` / `user`. Keycloak's admin console is at
<http://idp.dev.local:8081> (`admin`/`admin`).

**Logging out in a browser does not work here, and it is the dev setup's fault.** Single logout
POSTs a `<LogoutRequest>` form to the IdP, which in this environment is plain HTTP while NGB is
HTTPS — a mixed-content form submission, which browsers block (`net::ERR_BLOCKED_BY_CLIENT`,
`Mixed Content: … contains a form that targets an insecure endpoint`), leaving you on an error
page with both sessions still live. Login is unaffected: those hops are top-level redirects.
Either use `make smoke-saml`, which walks the same profile with no browser and no mixed-content
policy, or start Chrome with `--allow-running-insecure-content
--unsafely-treat-insecure-origin-as-secure=http://idp.dev.local:8081`, after which the logout
round trip completes and Keycloak asks for credentials again.

What's wired up: NGB's SP metadata is at `/catgenome/saml/metadata`, Keycloak's IdP
descriptor is fetched into `secrets/idp-metadata.xml` (NGB reads it from a *file*), the
realm's protocol mappers emit exactly the attribute names
`saml.user.attributes`/`saml.authorities.attribute.names` expect, and Keycloak group
membership (`NGB_ADMINS`, `NGB_USERS`) arrives as NGB authorities.

One thing about SAML mode that looks arbitrary but isn't: **exactly one port answers at a
time.** `none` → HTTP 8080; `saml` → HTTPS 8443. The app exposes a single connector, so the
other port is simply not listening. `make smoke` prints both and says which is live.

**Verifying the signing path.** NGB signs its `<AuthnRequest>`s and its SP metadata with the
`ngb-saml` key from `secrets/ngb-keystore.jks`, but `saml.client.signature` is **off** in the
committed realm: the certificate is generated per machine by `make certs`, so it cannot be
inlined in `keycloak/realm-ngb.json`, and `--import-realm` would have nothing to validate
against on a first start. Turn it on for a run with `make saml-verify-signing`, which pushes
`secrets/ngb-saml-cert.pem` into the Keycloak client, flips the flag and then runs the login
again — this time Keycloak rejects an `<AuthnRequest>` NGB did not sign with the advertised key.
Do that after any change to the SAML configuration; with the flag off, a broken signature looks
exactly like a working one. `make up-saml` re-imports the realm, so the flag goes back off.

Keycloak's H2 database is intentionally *not* on a volume — it runs as uid 1000 and
can't write a root-owned named volume. `--import-realm` rebuilds the realm from
`keycloak/realm-ngb.json` on every start, so edit that file for changes you want to keep;
anything you click together in the admin console is gone on restart.

**Registering data / using the CLI**

Drop files under `.devenv/data/ngs/` (bind-mounted at `/ngs`, and `ngs.data.root.path`
points there), then:

```bash
make cli
ngb reg_ref /ngs/grch38/Homo_sapiens.GRCh38.fa --name GRCh38
ngb reg_file GRCh38 /ngs/demo/sample.vcf
```

If you have no data at hand, the repo's own test fixtures work for a round-trip check:

```bash
cp ../server/catgenome/src/test/resources/templates/A3.fa data/ngs/
make cli
ngb reg_ref /ngs/A3.fa --name SMOKE_A3 && ngb list_ref
```

That path was verified end to end (CLI → server → H2): the reference registers and
`/restapi/reference/1/loadChromosomes` reports chromosome `A1` at 56,400 bp, so the FASTA
was really parsed and not just recorded.

In SAML mode the CLI needs a JWT. `make cli-token` prints one — it logs in over SAML the same way
`make smoke-saml` does and then calls `/restapi/user/token`, which signs a token for whoever is
asking. Put it in `CLI_TOKEN` in `.env`, or `ngb set_token <token>` inside the container. Two
adjustments the CLI needs in that mode, because the server is then on HTTPS with a self-signed
certificate:

```bash
make up-saml
token=$(make -s cli-token)
docker-compose exec \
  -e JAVA_OPTS="-Djavax.net.ssl.trustStore=/workspace/.devenv/secrets/ngb-keystore.jks -Djavax.net.ssl.trustStorePassword=changeit" \
  cli bash -lc "ngb set_srv https://ngb.dev.local:8443/catgenome && ngb set_token $token && ngb list_ref"
```

That round trip is the check for the JWT half of the security stack. `make cli-token` is
permanent tooling, not a migration artefact — it is the only way to get a token here without
a browser, and `make cli-test` cannot stand in for it: that suite runs with `AUTH_MODE=none`.

**Cloud storage: reading tracks over `s3://` and `sws://`**

There are no AWS credentials here, so MinIO stands in for S3. It needs no NGB code change — the AWS
SDK v2 takes an endpoint override from a system property, which v1 could not:

```bash
scripts/prepare-track-fixtures.sh   # once: stages the BAM and VCF minio-init uploads
make up-cloud                       # MinIO + bucket + fixtures, NGB restarted pointed at it
make verify-cloud                   # reads tracks out of it and diffs against a local read
```

`make verify-cloud` covers five paths: registered `s3://` and `sws://` files (ranged
`GetObject` through `S3SeekableStream`, virtual-host and path-style respectively),
`/dataitem/{id}/downloadUrl` (the pre-signer, checked for `GET` 200 and `HEAD` 403 — a
pre-signed URL is signed for one method, and NGB's remote reader depends on tolerating that
403), the non-registered `fileUrl=s3://…` path that pre-signs and reads over https, and a
bgzip'd VCF, which is the case that reads an object right to its last byte. Every read is
compared byte for byte with the same window read from disk.

Two things worth knowing before you extend it:

- **Register with `"type":"S3","indexType":"S3"`.** The scheme in the path is not enough for a
  registration request; without the type NGB opens the path as a local file.
- **NGB is pointed at MinIO under the honest name `minio`.** Phase 8 had to alias it as
  `s3.amazonaws.com` instead, because `EnhancedUrlHelper` decided whether to tolerate the 403 on
  `HEAD` by matching the hostname against `.*s3.*\.amazonaws\.com` — so under any other name every
  pre-signed read came back empty. Phase 9 keyed that tolerance to the URL's own SigV4 signature;
  path 4 above passing under the name `minio` is what proves it. `verify-tracks.sh` still stages a
  fake `s3.amazonaws.com` in a container of its own, to exercise the hostname clause that is
  deliberately kept for real AWS.

The two defects this profile exposed in Phase 8 — that pre-signed read returning an empty file,
and every bgzip'd feature file in cloud storage failing to register with
`invalid uncompressedLength: -1` — are fixed (`16e00527`); see "Finding 1" and "Finding 2" in
the plan document for what they were.

Azure has no equivalent and `az://` is **unverified**: `AzureBlobClient` hard-codes
`https://<account>.blob.core.windows.net`, so Azurite cannot be pointed at it without changing
NGB code, and there is no Azure account here. The code went through the same SDK-agnostic
changes as the S3 paths, but nothing has read a byte over `az://`. See TEST-BASELINE.md.

## Auth modes

`AUTH_MODE` in `.env`:

- `none` — no security. Fastest loop; use it for everything except auth work.
- `saml` — Keycloak SSO for the browser **plus** JWT for the CLI.

There is no JWT-only `AUTH_MODE`, but the two halves are no longer welded together: before
migration Phase 4 `JWTSecurityConfiguration` autowired `SAMLAuthenticationProvider` and
`SAMLEntryPoint`, so JWT alone would not start; now it needs nothing from the SAML side, and
`jwt.security.enable=true` with `saml.security.enable=false` (via `ngb/override.properties`)
boots and answers 401 instead of redirecting to an IdP. SAML mode still forces
`security.acl.enable=true`, because `SamlUserDetailsService` is conditional on it.

## Configuration

The app's properties are rendered at container start into
`/opt/ngb/config/catgenome.properties` from `ngb/catgenome.properties.tpl` (+
`ngb/auth-saml.properties.tpl` in SAML mode). That file is loaded *after* the defaults
baked into the jar, so it wins.

For ad-hoc tweaks without touching the templates, create **`ngb/override.properties`** —
it is appended last. Restart the container to apply.

## The schema bugs this environment found

Kept because they are the reason two migration scripts and three PostgreSQL forward
migrations look the way they do, and because one of the findings is still open.

Bringing the environment up on **empty** databases — which no CI job and no developer with an
existing `catgenome.h2.db` had done for a while — turned out to be the check nothing else was
doing. Two committed migrations could not run:

1. `v2024.03.21_19.00__blast_task_id.sql` did
   `ALTER SEQUENCE CATGENOME.S_TASK RESTART WITH (SELECT MAX(TASK_ID) + 1 FROM CATGENOME.TASK)`.
   On a fresh H2 install `TASK` is empty, so the restart value is `NULL` and Flyway aborts;
   on PostgreSQL it is worse, because a subquery in `RESTART WITH` is a syntax error on any
   database, empty or not. Now `COALESCE(MAX(TASK_ID), 0)` on H2 and `setval(...)` on
   PostgreSQL.
2. `v2024.02.19_18.00__acl_target_manager_role.sql` inserted `ROLE_TARGET_MANAGER` at a
   hardcoded `id = 10`, which is free on H2 and taken by `ROLE_SEG_MANAGER` on PostgreSQL — so
   **no PostgreSQL database could migrate past 2024.02.19 at all.** Now
   `(SELECT MAX(id) + 1 FROM catgenome.role)`; `DefaultRoles.ROLE_TARGET_MANAGER` carries a
   `null` id, so no Java code depends on the number.

The second one was a symptom: the two flavours' script sets had silently diverged in three
places — the seeded predefined roles, `VCF.MULTI_SAMPLE`'s nullability and
`TASK_ORGANISM.ORGANISM`'s type. Phase 5 converged them on the H2 shape with forward
migrations on the PostgreSQL side (`v2026.08.21_12.*`), H2 being the reference because
`DefaultRoles`, `docs/md/user-guide/um-overview.md` and the DAO code all agree with it. So
`MAX(id) + 1` resolves to 10 on both flavours now, and all 59 migrations apply cleanly on both.

**Still open, and wider than persistence:** one of those divergences was a production bug, not
a test artefact — PostgreSQL never seeded `ROLE_WIG_MANAGER`, so on a PostgreSQL install
nothing but an administrator could satisfy `WigSecurityService`'s `@PreAuthorize`. Phase 5
fixed that role. It did not fix the rest of the pattern:
`NGBMethodSecurityExpressionRoot.hasSpecificRole` also names MAF/BUCKET/PROJECT/BOOKMARK
manager roles and `DefaultRoles` names `ROLE_HEATMAP_MANAGER`, none of which either flavour
seeds, so those `@PreAuthorize` checks are unsatisfiable for everyone but an admin on every
install. That is an authorisation question rather than a migration one, and it was left alone
deliberately.

One procedural note that outlives the migration: editing an already-applied migration makes
Flyway fail validation with a checksum mismatch wherever the old version ran. Fresh dev
databases do not care; anywhere else needs a `flyway repair` or a new schema.

## Notes and gotchas

- **PostgreSQL is 16 since migration Phase 5** (`PG_VERSION` in `.env`); it was pinned to
  9.6 because Flyway 3.2.1 refused to run against anything newer. Changing `PG_VERSION`
  still needs `make reset-pg` — a 9.6 data directory is unreadable by 16, and `reset-pg`
  destroys the cluster rather than migrating it. For a real database the procedure is
  `docs/md/installation/database-upgrade.md`.
- **H2 is 2.3.232 since migration Phase 5**, and 2.x cannot open a 1.3.176 file: an
  existing `catgenome.h2.db` needs the `SCRIPT TO` / `RUNSCRIPT FROM` round trip in the
  same document. `make reset-ngb-data` sidesteps it by throwing the database away.
- **Memory.** `server/catgenome/build.gradle` sets `-XX:MaxDirectMemorySize=7937m` for
  the test JVM, and the app defaults to a 2 GB heap. Your colima VM currently has
  ~12 GB / 6 CPU; if the test task gets killed, give colima more memory
  (`colima stop && colima start --memory 16`) or lower `NGB_HEAP`.
- **arm64.** All base images are multi-arch, but external BLAST binaries aren't. If one
  misbehaves, add `platform: linux/amd64` to that single service. The `snappy-java 1.0.3-rc3`
  pin that used to be the other example of this is gone since migration Phase 7 — htsjdk 5
  manages snappy 1.1.10.5, which ships `native/Linux/aarch64`.
- **Remote tracks re-read their index on every request** since migration Phase 7, which
  dropped the index cache (decision D10). Measured: an 88 kB `.bai` behind an `http://` URL
  costs 263 kB of index traffic *per* `bam/track/get`, and a local-vs-remote VCF track is
  0.01 s vs 0.24 s. It scales with index size, not window size. See the Phase 7 findings in
  `JAVA21-MIGRATION-PLAN.md` if you are deciding whether to reintroduce a cache.
- **`muscle`** is installed if the distro has it for your architecture; target-
  identification alignment needs it.
- **BLAST / LLM / NCBI** integrations point at external services (`blast.server.url`,
  `llm.*`, `ncbi.api.key`) and are left unset — add them to `override.properties` if a
  change touches those paths.
- The first `make jar` downloads the Gradle distribution (8.14.5 since migration Phase 3), the
  npm dependency tree and mkdocs into named volumes; later builds reuse them. `make reset`
  throws those away too.

## Migration checkpoints this environment was built to verify

All five are done. Kept as an index: each one names the phase that closed it, what the version
ended up being, and the target that re-checks it — which is the part still worth having.

1. ☑ Gradle 3.3 → 8.x (wrapper, `compile`→`implementation`, `bootRepackage`→`bootJar`),
   lombok → ≥ 1.18.30. Nothing compiled on JDK 21 before this, and the two were
   independent walls — fixing Gradle alone just exposed the lombok one. *Phases 2–3;
   now Gradle 8.14.5, lombok 1.18.46, JDK 21 toolchain for the server module.*
2. ☑ Spring Boot 1.5 → 3.x, Spring Security 4 → 6, `javax.*` → `jakarta.*`. *Phases 2–3;
   now Boot 3.5.16 on Spring 6.2. `make up` + `make smoke` is the check.*
3. ☑ `spring-security-saml2-core` (OpenSAML 2, EOL) → Spring Security's SAML2 support —
   `SAMLSecurityConfiguration.java` is a full rewrite. *Phase 4; now
   `spring-security-saml2-service-provider` on OpenSAML 4.3.2, and `com.auth0:java-jwt` 4.6.0
   for the JWT half. All four externally visible endpoints keep the OpenSAML 2 extension's
   URLs, so existing IdP registrations do not have to be re-pointed. `make up-saml` +
   `make smoke-saml`, and `make saml-verify-signing` for the signing path, are the checks.*
4. ☑ Flyway 3.2.1 → 10.x and H2 1.3.176 → 2.x (schema/SQL differences), then
   `PG_VERSION=16`. Verify with `make test-pg` on both flavours. *Phase 5; now Flyway
   11.7.2 (+ `flyway-database-postgresql`), H2 2.3.232, driver 42.7.x, PostgreSQL 16. The
   upgrade of an existing database is not automatic on the H2/PostgreSQL side — see
   `docs/md/installation/database-upgrade.md`. The Flyway schema history *is* converted
   automatically, by `FlywayMigrator`.*
5. ☑ Lucene 6.6 → 9.x, htsjdk, POI 3.16, and the Swagger annotations. *Phases 3 and 6–8,
   four separate pieces:*
   - *Swagger: Phase 3 replaced `mangofactory` with springdoc, at `/swagger-ui/index.html`
     and `/v3/api-docs`; Phase 8 rewrote the 1.3-era `@ApiOperation`/`@ApiResponses`
     annotations as OpenAPI 3 and dropped the last `com.wordnik` jar.*
   - *Lucene: Phase 6, now 9.12.3. Every index NGB wrote before it has to be rebuilt — the
     server refuses to start otherwise and says which directories and what rebuilds each, see
     `docs/md/installation/lucene-reindex.md`. `make verify-lucene` checks all 18 read paths,
     and `fixtures/pre-migration/lucene6/` holds the last Lucene 6 index that will ever exist,
     for testing the upgrade against.*
   - *htsjdk: Phase 7, now 5.0.0, with the forked reader package and the index cache deleted.
     `make verify-tracks` loads a track of every type. Two consequences worth knowing: a
     feature file on `s3://`/`sws://`/`az://` must now be bgzip+tabix, and remote index reads
     are no longer cached (see the D10 note above).*
   - *POI: Phase 8, 3.16 → 5.5.1, along with the AWS SDK v1 → v2 move that `make verify-cloud`
     exists to check.*

Phase 9 closed the migration off the checkpoint list: Docker images and the JRE-bundled
distributions on Temurin 21, AppVeyor replaced by GitHub Actions, the docs brought forward, and
the version set to 3.0.0. This environment's own migration scaffolding went with it — the JDK 8
in the image, and `make probe-java21`, which demonstrated the first checkpoint's two walls by
running Gradle 3.3 and lombok 1.16.16 under JDK 21 and failing. Both walls are gone, so the
target measured nothing.
