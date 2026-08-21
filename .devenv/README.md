# NGB containerised dev environment

A self-contained environment for working on NGB — in particular for the **Java 21
migration** — with no JDK, Node, Gradle or Python installed on the host. Everything runs
in containers: the build toolchain, the application, both database flavours, and a real
SAML identity provider.

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
   when this environment was written; since migration Phase 3 it is Gradle 8.14.5, the server
   module builds on a **JDK 21** toolchain and `server/ngb-cli` on 17. The toolbox image
   therefore still carries **three JDKs** (`with-java8` / `with-java17` / `with-java21`, or
   `use-java 8|17|21` in a shell) and the app container still picks its JDK at runtime via
   `NGB_JAVA_VERSION` — which now defaults to **21**, because the jar is Java 21 bytecode and
   will not load on anything older. 8 and 17 are kept for bisecting against the earlier phases.
2. **The database flavour is chosen at build time**, not at runtime
   (`-Pdatabase=h2|postgres` swaps `applicationContext-flyway.xml`), so there are two
   jars and two app services.
3. **SAML compares URLs byte-for-byte.** Every service therefore listens on the *same*
   port inside and outside its container, and hostnames go through `/etc/hosts` rather
   than `localhost`.

## Services

| Service | Profile | Purpose | Endpoint |
|---|---|---|---|
| `builder` | default | Toolbox: JDK 8 + 17 + 21, Node 14.17.5, mkdocs, muscle. All Gradle/npm work happens here | — |
| `certs` | default | One-shot: JKS keystore (HTTPS + SAML signing) and the JWT RSA keypair | — |
| `ngb-h2` | default | NGB on H2 | `:8080` http *or* `:8443` https |
| `ngb-pg` | `pg` | NGB on PostgreSQL | `:8090` http *or* `:8493` https |
| `postgres` | `pg` | PostgreSQL 16 (`PG_VERSION`, see note below), DBs `ngb` + `ngb_test` | `:5432` |
| `test-pg` | `pg` | Unit tests against PostgreSQL | — |
| `idp` | `saml` | Keycloak 26 as SAML 2.0 IdP, realm `ngb` preloaded | `:8081` |
| `idp-metadata` | `saml` | One-shot: pulls the IdP descriptor into `secrets/` | — |
| `cli` | `cli` | `ngb-cli` wired to the running server | — |

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
make cli-test                # CLI<->server integration suite (downloads test data)
```

**Read [`TEST-BASELINE.md`](TEST-BASELINE.md) before you trust a red run.** `make lint` is
green; neither test suite quite is (3 failures on H2, 11 on PostgreSQL after migration
Phase 3 — five of those are network tests). That file records each failure and why, so during
the migration you can tell new breakage from old — and it lists the two preconditions the
numbers depend on: `make test-pg` wants `make reset-pg` first, and `make test` wants an empty
`../contents/`.

**Which JDK the server runs on**

```bash
make up                       # JDK 21 — the default since migration Phase 3
NGB_JAVA_VERSION=8 make up    # or 17: only useful against a jar built by that phase
```

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

That round trip is the check for the JWT half of the security stack (`make cli-test` cannot be: its
fixtures were hosted on `ngb.opensource.epam.com`, which no longer resolves).

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

## Two source fixes were needed to boot at all

Bringing this environment up on empty databases exposed two real migration bugs. Neither
is caused by the container setup — any fresh install hits them.

### 1. `v2024.03.21_19.00__blast_task_id.sql` (both flavours)

```sql
ALTER SEQUENCE CATGENOME.S_TASK RESTART WITH (SELECT MAX(TASK_ID) + 1 FROM CATGENOME.TASK);
```

- **H2**: on a fresh install `TASK` is empty, `MAX(TASK_ID)` is `NULL`, and H2 1.3.176
  rejects the restart value — Flyway aborts and the app never starts. Fixed with
  `COALESCE(MAX(TASK_ID), 0)`.
- **PostgreSQL**: worse — PostgreSQL doesn't accept a subquery in `RESTART WITH` at all,
  so this is a syntax error on *any* database, empty or not. Replaced with
  `SELECT setval('CATGENOME.S_TASK', COALESCE((SELECT MAX(TASK_ID) FROM CATGENOME.TASK), 0) + 1, false);`

Both replacements were verified against H2 1.3.176 and PostgreSQL 9.6.

### 2. `v2024.02.19_18.00__acl_target_manager_role.sql` (PostgreSQL only)

```sql
INSERT INTO catgenome.role (id, name, predefined) VALUES (10, 'ROLE_TARGET_MANAGER', true);
```

The two flavours seed different predefined roles: h2 has `ROLE_WIG_MANAGER` at 8 and
`ROLE_SEG_MANAGER` at 9, PostgreSQL has `ROLE_MAF_MANAGER` at 9 and `ROLE_SEG_MANAGER`
at **10**. So the hardcoded `id = 10` works on h2 and violates `role_pkey` on
PostgreSQL — meaning no PostgreSQL database could migrate past 2024.02.19 at all. Now
`(SELECT MAX(id) + 1 FROM catgenome.role)`; `DefaultRoles.ROLE_TARGET_MANAGER` carries a
`null` id, so no Java code depends on the number. Only the PostgreSQL file was touched,
so h2 checksums are untouched. All 59 migrations now apply cleanly on both flavours.

Note that editing an already-applied migration makes Flyway 3.2.1 fail validation with a
checksum mismatch on databases where the old version ran — irrelevant for fresh dev
databases, but it needs a `flyway repair` (or a fresh schema) anywhere the broken
version somehow got recorded. That divergence in the seeded role IDs is also worth
keeping in mind for the Flyway upgrade: the two flavours' schemas are not identical.

**Settled in migration Phase 5.** The three places the two script sets had diverged —
the seeded roles, `VCF.MULTI_SAMPLE`'s nullability and `TASK_ORGANISM.ORGANISM`'s type —
are now converged on the h2 shape by forward migrations on the PostgreSQL side
(`v2026.08.21_12.*`), so `MAX(id) + 1` above resolves to 10 on both flavours. h2 was the
reference because `DefaultRoles`, `docs/md/user-guide/um-overview.md` and the DAO code all
agree with it. One of those divergences was a real production bug rather than a test
artefact: PostgreSQL never seeded `ROLE_WIG_MANAGER`, so on a PostgreSQL install nothing
but an administrator could satisfy `WigSecurityService`'s `@PreAuthorize`. Still open and
wider than persistence: `NGBMethodSecurityExpressionRoot.hasSpecificRole` also names
MAF/BUCKET/PROJECT/BOOKMARK manager roles, and `DefaultRoles` names
`ROLE_HEATMAP_MANAGER`, none of which either flavour seeds.

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
- **arm64.** All base images are multi-arch, but some dependencies aren't
  (`snappy-java 1.0.3-rc3`, external BLAST binaries). If one
  misbehaves, add `platform: linux/amd64` to that single service.
- **`muscle`** is installed if the distro has it for your architecture; target-
  identification alignment needs it.
- **BLAST / LLM / NCBI** integrations point at external services (`blast.server.url`,
  `llm.*`, `ncbi.api.key`) and are left unset — add them to `override.properties` if a
  change touches those paths.
- The first `make jar` downloads the Gradle distribution (8.14.5 since migration Phase 3), the
  npm dependency tree and mkdocs into named volumes; later builds reuse them. `make reset`
  throws those away too.

## Migration checkpoints this environment is built to verify

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
5. ☐ Lucene 6.6 → 9.x, htsjdk, POI 3.16. Verify with `make test` and by clicking through
   tracks in the UI. *Phases 6–8. The `mangofactory` Swagger half of this one is done —
   Phase 3 replaced it with springdoc, at `/swagger-ui/index.html` and `/v3/api-docs`.*

`make probe-java21` demonstrated the first checkpoint's two walls by running Gradle 3.3 and
lombok 1.16.16 under JDK 21 and failing. Both are gone, so the target no longer measures
anything; Phase 9 removes it.
