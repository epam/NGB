# NGB containerised dev environment

A self-contained environment for working on NGB, with no JDK, Node, Gradle or Python
installed on the host. Everything runs in containers: the build toolchain, the application,
both database flavours, a real SAML identity provider and an S3-compatible object store.

Two other documents live beside this one: [`TEST-BASELINE.md`](TEST-BASELINE.md), which is what a
green run looks like and which failures are expected, and [`SECURITY-SCAN.md`](SECURITY-SCAN.md),
which is how to scan the built artefacts for known vulnerabilities. Known open defects are in
[`ISSUES.md`](../ISSUES.md) at the repository root.

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

1. **The build's JDK is not the host's JDK.** Gradle is 8.14.5, the server module builds on a
   **JDK 21** toolchain and `server/ngb-cli` on 17. The image's default is 21, so nothing has
   to select a JDK for ordinary work; it carries **two** (`with-java17` / `with-java21`, or
   `use-java 17|21` in a shell) because ngb-cli's toolchain is the older one. The app
   container picks its JDK at runtime via `NGB_JAVA_VERSION`, which defaults to 21 — the jar
   is Java 21 bytecode and will not load on anything older.
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
resources by `e2e/cli/prepare_test_data.sh` — they used to be downloaded from a host that stopped
resolving, which is why the suite had been unrunnable for years, so keep it network-free.

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
REST against a **running** instance and print what came back. They are the only thing that
exercises the parsers, the Lucene indexes and the S3 client end to end.

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
JDK for `server/ngb-cli`, and an unsupported value says so (`must be 17 or 21`) instead of
pointing the launcher at a directory that does not exist.

`JAVA_EXTRA_OPTS` in `.env` is **empty** and should stay that way. It used to carry four
`--add-opens` for EhCache 2's reflective heap sizing; EhCache was replaced by Caffeine, which
reflects on nothing. A new `--add-opens` requirement appearing here is a signal — on Spring 6 it
usually means a stale dependency, not a JDK problem.

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

That round trip is the check for the JWT half of the security stack. `make cli-token` is the only
way to get a token here without a browser, and `make cli-test` cannot stand in for it: that suite
runs with `AUTH_MODE=none`.

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
- **NGB is pointed at MinIO under the honest name `minio`**, and that is itself part of the check.
  `EnhancedUrlHelper` used to decide whether to tolerate the 403 on `HEAD` by matching the hostname
  against `.*s3.*\.amazonaws\.com`, so under any other name every pre-signed read came back empty
  and the profile had to alias MinIO as `s3.amazonaws.com` to work at all. That tolerance is keyed
  to the URL's own SigV4 signature now; path 4 above passing under the name `minio` is what proves
  it. `verify-tracks.sh` still stages a fake `s3.amazonaws.com` in a container of its own, to
  exercise the hostname clause that is deliberately kept for real AWS.

This profile is also what caught the two cloud defects fixed in `16e00527`: a pre-signed read
returning an empty file, and every bgzip'd feature file in cloud storage failing to register with
`invalid uncompressedLength: -1`. Neither was visible to the unit suite, which stubs the S3 client
away.

Azure has no equivalent and `az://` is **unverified**: `AzureBlobClient` hard-codes
`https://<account>.blob.core.windows.net`, so Azurite cannot be pointed at it without changing
NGB code, and there is no Azure account here. The code went through the same SDK-agnostic
changes as the S3 paths, but nothing has read a byte over `az://` — see "Known gaps" in
[`TEST-BASELINE.md`](TEST-BASELINE.md).

## Auth modes

`AUTH_MODE` in `.env`:

- `none` — no security. Fastest loop; use it for everything except auth work.
- `saml` — Keycloak SSO for the browser **plus** JWT for the CLI.

There is no JWT-only `AUTH_MODE`, but the two halves are not welded together — JWT needs nothing
from the SAML side, so `jwt.security.enable=true` with `saml.security.enable=false` (via
`ngb/override.properties`) boots and answers 401 instead of redirecting to an IdP. SAML mode does
force `security.acl.enable=true`, because `SamlUserDetailsService` is conditional on it.

## Configuration

The app's properties are rendered at container start into
`/opt/ngb/config/catgenome.properties` from `ngb/catgenome.properties.tpl` (+
`ngb/auth-saml.properties.tpl` in SAML mode). That file is loaded *after* the defaults
baked into the jar, so it wins.

For ad-hoc tweaks without touching the templates, create **`ngb/override.properties`** —
it is appended last. Restart the container to apply.

## If you touch the schema scripts

There are two sets of them, `database/catgenome/h2/` and `database/catgenome/postgres/`, and
nothing in the build compares them. They had silently diverged in three places — the seeded
predefined roles, `VCF.MULTI_SAMPLE`'s nullability and `TASK_ORGANISM.ORGANISM`'s type — and one
of those divergences meant **no PostgreSQL database could migrate past 2024.02.19 at all**, for two
years, because a role was inserted at a hardcoded `id = 10` that PostgreSQL had already given to
`ROLE_SEG_MANAGER`. They were converged on the H2 shape by the `v2026.08.21_12.*` scripts on the
PostgreSQL side (H2 is the reference because `DefaultRoles`, `docs/md/user-guide/um-overview.md` and
the DAO code all agree with it). Four rules came out of that:

1. **Write the same change into both sets, and run `make test-pg` as well as `make test`.** The
   PostgreSQL-only failures in [`TEST-BASELINE.md`](TEST-BASELINE.md) are the specification for what
   convergence means: if one of them comes back, the sets have drifted again.
2. **Never hardcode an id.** `(SELECT MAX(id) + 1 FROM catgenome.role)` and `COALESCE(MAX(x), 0)` —
   an id that happens to be free on one flavour is taken on the other, and a `MAX()` over an empty
   table is `NULL`.
3. **Bring it up on an empty database.** `make reset-ngb-data` (H2) and `make reset-pg` (PostgreSQL)
   then `make up`/`make up-pg` — a migration that only ever runs against a database that already has
   rows is a migration nobody has tested. That is exactly how both of the bugs above were found, and
   neither CI nor a developer with an existing `catgenome.h2.db` would have hit them.
4. **Do not edit an already-applied migration.** Flyway fails validation with a checksum mismatch
   wherever the old version ran. Fresh dev databases do not care; anywhere else needs a
   `flyway repair` or a new schema. Add a forward script instead.
5. **Rebuild the jar before you start a server on it.** The scripts are packaged *resources*, so
   `make up` runs whatever is inside `dist/catgenome-h2.jar` — not what is in the tree. A new script
   with no `make jar-fast` (or `make jar-pg-fast` for `up-pg`) in front of it simply does not run,
   and the symptom is a database that looks like the migration is broken rather than absent. `make
   test` / `make test-pg` build their own classpath and are immune, which is what makes it easy to
   miss. `unzip -l dist/catgenome-h2.jar | grep BOOT-INF/classes/database/catgenome/h2/` settles it.

Note also that the manager roles the security expressions name are not all seeded — see
[`ISSUES.md`](../ISSUES.md), issue 5. Adding a seed for one of them is a schema change on both
flavours, with rule 2 applying.

## Notes and gotchas

- **PostgreSQL is 16** (`PG_VERSION` in `.env`). Changing it needs `make reset-pg` — a data
  directory written by one major version is unreadable by the next, and `reset-pg` destroys
  the cluster rather than migrating it. For a real database the procedure is
  `docs/md/installation/database-upgrade.md`.
- **H2 is 2.3.232**, and 2.x cannot open a 1.3.176 file: an existing `catgenome.h2.db` written
  by NGB 2.x needs the `SCRIPT TO` / `RUNSCRIPT FROM` round trip in the same document.
  `make reset-ngb-data` sidesteps it by throwing the database away.
- **Memory.** `server/catgenome/build.gradle` sets `-XX:MaxDirectMemorySize=7937m` for
  the test JVM, and the app defaults to a 2 GB heap. Your colima VM currently has
  ~12 GB / 6 CPU; if the test task gets killed, give colima more memory
  (`colima stop && colima start --memory 16`) or lower `NGB_HEAP`.
- **arm64.** All base images are multi-arch, but external BLAST binaries aren't. If one
  misbehaves, add `platform: linux/amd64` to that single service. A Java dependency can have
  the same problem: watch for a native library with no `aarch64` entry in its jar — htsjdk 5
  manages snappy-java 1.1.10.5, which does ship `native/Linux/aarch64`, and a pin that drags
  it back below that will fail here and nowhere else.
- **Remote tracks re-read their index on every request.** There is no index cache. Measured:
  an 88 kB `.bai` behind an `http://` URL costs 263 kB of index traffic *per* `bam/track/get`,
  and a local-vs-remote VCF track is 0.01 s vs 0.24 s. It scales with index size, not window
  size, so a human WGS `.bai` of 5–10 MB is paid on every pan. Local files are unaffected. If
  you reintroduce a cache, that is where the numbers to beat come from.
- **`muscle`** is installed if the distro has it for your architecture; target-
  identification alignment needs it.
- **BLAST / LLM / NCBI** integrations point at external services (`blast.server.url`,
  `llm.*`, `ncbi.api.key`) and are left unset — add them to `override.properties` if a
  change touches those paths.
- The first `make jar` downloads the Gradle distribution (8.14.5), the npm dependency tree and
  mkdocs into named volumes; later builds reuse them. `make reset` throws those away too.
- **Two Gradle targets cannot run at once**, in the same container or in different ones: they share
  `GRADLE_USER_HOME=/cache/gradle`, and the second one dies before it compiles anything with
  `Timeout waiting to lock journal cache (/cache/gradle/caches/journal-1) … Owner PID: <n>`. It is
  a lock, not a corruption — wait for the first to finish and re-run. Only the `verify-*` scripts and
  `make smoke` are safe alongside a build, because they are `curl` from the host.
- **A running instance logs a stack trace every minute that is not yours.**
  `ProteinPatentsScheduledService.searchPatents` throws
  `IllegalArgumentException: Patented protein sequences database not available`, because the devenv
  ships no BLAST patents database (`blast.server.url` is unset, above). Harmless, and unrelated to
  whatever you were doing when it appeared — but it is every 60 s
  (`@Scheduled(fixedRateString = "${targets.sequence.patents.search.rate:60000}")`, and no profile
  overrides it), so it buries anything else in the log. Set that property in
  `override.properties` if you need to read the log around a startup.
- **There is no `make smoke-pg`.** `make smoke` probes the H2/SAML ports only (8080 and 8443), so
  for the PostgreSQL instance use `curl http://localhost:8090/catgenome/restapi/version`, which
  should answer `{"payload":"3.0.0","status":"OK"}`. The `verify-*` scripts do take a base URL, so
  those are `bash scripts/verify-tracks.sh http://localhost:8090/catgenome`.
- **`make up` on an existing container can come up with no published ports at all** if the Docker
  daemon restarted underneath it: `docker inspect` shows `HostConfig.PortBindings` set and
  `NetworkSettings.Ports` empty, and every request is refused while the container looks healthy.
  `docker-compose rm -sf ngb-h2 && make up` fixes it.
- **Search this tree with `git grep`, not `rg`.** Two independent reasons, neither of them loud.
  `rg` skips dot-directories unless it is given `--hidden`, so **this file, everything else in
  `.devenv/`, and `.github/` are invisible to it** — a search that never looked at the build
  environment reads exactly like one that found nothing to fix in it. And `.gitignore:17` is not a
  valid glob, so `rg` prints a parse error, drops that one pattern and searches the `${…}`
  directories an old test run can have left in `server/catgenome/` — Lucene segment files and stale
  test output, where a symbol you have just deleted still occurs. See
  [`ISSUES.md`](../ISSUES.md), issue 6.
- **Sweep for a format or a feature with `git grep -i <string>`, not `\b<string>\b`.** Camel-case
  identifiers have no word boundary after the word: `\bmaf\b` matches `MAF`, `maf-file-dao.xml` and
  `case MAF` but not `mafDataService`, which is how an Angular service registration referring to a
  just-deleted file survives a sweep that looked complete. The word-boundary form also hides
  `MafManager`, `MafFile` and `makeMafDir`. Take the extra hits and read them.

## The stack, and what exercises each part

Versions live in `server/catgenome/build.gradle` — the `ext { … }` block and the dependency
declarations, each non-obvious pin with a comment saying why it is what it is. The table below is
the other map worth having: from a part of the stack to the target that will tell you if you broke
it. Note how many of them need a **running** server — `make test` never starts one, so it cannot
make those checks at all.

| Part | Version | Checked by |
|---|---|---|
| Build | Gradle 8.14.5, lombok 1.18.46; JDK 21 toolchain for the server, 17 for `ngb-cli` | `make jar`, `make lint` |
| Application | Spring Boot 3.5.16 on Spring 6.2, Spring Security 6, embedded Tomcat 10, `jakarta.*` | `make up` + `make smoke` |
| API docs | springdoc 2.8.17 — OpenAPI 3 at `/swagger-ui/index.html` and `/v3/api-docs` | `make smoke` |
| Database | Flyway 11.7.2 (+ `flyway-database-postgresql`, both from the Boot BOM), H2 2.3.232, PostgreSQL 16 on driver 42.7.x | `make test`, `make test-pg`, and a start on an empty database |
| SAML and JWT | `spring-security-saml2-service-provider` on OpenSAML 4.3.2, `com.auth0:java-jwt` 4.6.0 | `make up-saml` + `make smoke-saml`, `make saml-verify-signing`, `make cli-token` |
| Search indexes | Lucene 9.12.3 | `make verify-lucene` — all 18 read paths |
| File formats | htsjdk 5.0.0, POI 5.5.1, biojava 7.2.6 | `make verify-tracks` — one track of every type |
| Cloud storage | AWS SDK v2 2.54.1 (S3 and Swift), azure-storage-blob 12.35.0 | `make verify-cloud` — `s3://` and `sws://`; `az://` has never been run against a live service |

Two properties of that stack that otherwise look like bugs:

* **Lucene 9 cannot read an index written by Lucene 6**, and NGB refuses to start on one rather
  than misreading it — it names the directories and says what rebuilds each. The operator
  procedure is `docs/md/installation/lucene-reindex.md`. `fixtures/pre-migration/lucene6/` holds
  the last Lucene 6 indexes that will ever exist, kept so that refusal and the rebuild path can be
  tested against something real.
* **A feature file in object storage must be bgzip-compressed and tabix-indexed.** A plain
  VCF/BED/GFF/SEG/BEDGRAPH reads fine from a local path and over `http(s)://`, but not from
  `s3://`, `sws://` or `az://`; `verify-cloud.sh` uses a bgzip'd VCF for exactly this reason, and
  it is documented for operators under *Configure for working with AWS S3* in
  `docs/md/installation/standalone.md`.

The Flyway schema history itself *is* converted automatically, by `FlywayMigrator` — it handles
both the Flyway 10 checksum algorithm and the new schema-history table shape. The H2 and
PostgreSQL data files are not: that is the operator procedure in
`docs/md/installation/database-upgrade.md`.
