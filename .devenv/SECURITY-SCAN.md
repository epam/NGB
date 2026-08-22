# Scanning NGB for known vulnerabilities

How to re-run the dependency scan, what it reports today, and which findings are deliberate. Last
run on **2026-08-22** against the 3.0.0 artefacts.

## Running it

Trivy, from its own container, over the **built artefacts** — not over the source tree. A source
scan is nearly useless here: `trivy fs` at the repository root finds 26 npm findings in 150 packages
and **zero Java ones**, because there are no Gradle lockfiles in this build. It only sees
`client/package-lock.json` and `export-templates/target-identification/package-lock.json`.

Two things to get right:

* **Use `trivy rootfs` for jars, not `trivy fs`.** Trivy's JAR analyzer is not enabled in `fs` mode
  — `trivy fs` over a directory of jars reports `Number of language-specific files num=0` and finds
  nothing. `rootfs` and `image` both enable it. `fs` is correct for the npm lockfiles.
* Trivy reads *inside* the Spring Boot fat jar: every Java finding's `PkgPath` is
  `catgenome-h2.jar/BOOT-INF/lib/<jar>`, and exploding the jar and scanning the directory gives the
  identical package and finding count. No need to unpack anything.

```bash
cd .devenv && make jar          # -> dist/catgenome-h2.jar, dist/ngb-cli.tar.gz
W=$HOME/ngb-vulnscan; mkdir -p "$W"/{jars,cli,cache,out}   # outside the repo; Docker Desktop does not share /tmp
cp ../dist/catgenome-h2.jar "$W/jars/"
tar xzf ../dist/ngb-cli.tar.gz -C "$W/cli"

trivy() { docker run --rm -v "$W/cache:/root/.cache/trivy" -v "$W/out:/out" "$@"; }
IMG=aquasec/trivy:latest

trivy -v "$W/jars:/scan" $IMG rootfs --scanners vuln --exit-code 0 --format table -o /out/jars.table /scan
trivy -v "$W/cli:/scan"  $IMG rootfs --scanners vuln --exit-code 0 --format table -o /out/cli.table  /scan
trivy -v "$PWD/../client:/scan" $IMG fs --scanners vuln --exit-code 0 --format table -o /out/client.table /scan
```

Add `--skip-db-update --skip-java-db-update` after the first call if you want every scan in a run to
see the same database, and **record the database's `UpdatedAt`** with the results — otherwise a
"new" finding cannot be told from a database that moved. Compare CVE ids against the tables below,
not counts.

For the runtime image, build it and scan that — it is the only scan that sees the OS packages:

```bash
make jar cli-build                                    # -> dist/catgenome.jar, dist/ngb-cli.tar.gz
cp ../dist/catgenome.jar ../dist/ngb-cli.tar.gz ../docker/core/
docker build -t ngb:scan ../docker/core
rm ../docker/core/catgenome.jar ../docker/core/ngb-cli.tar.gz     # leave no trace in the tree
trivy -v /var/run/docker.sock:/var/run/docker.sock $IMG image --scanners vuln --exit-code 0 ngb:scan
```

`make jar` and `make jar-pg` differ only in a build-time resource profile — both drivers are
declared unconditionally — so the two fat jars contain identical sets of nested jars and scan
identically. One flavour is enough.

## What it reports today

| Target | Findings | Packages |
|---|--:|--:|
| `catgenome-h2.jar` / `catgenome-psql.jar` | **2** (1 CRITICAL, 1 HIGH) | 253 |
| `ngb-cli` distribution | **2** (1 MEDIUM, 1 LOW) | 20 |
| image (`eclipse-temurin:21-jre-jammy` + both of the above) | **33** = 29 OS + 4 Java | 416 |
| `client/`, production dependencies | 26 (4 H, 18 M, 4 L) | 144 |
| `client/`, with dev dependencies | 116 (3 C, 44 H, 54 M, 15 L) | 793 |
| `export-templates/target-identification`, with dev dependencies | 43 (17 H, 22 M, 4 L) | 178 |

Recorded with Trivy 0.74.0 and a vulnerability DB from 2026-08-22, on aarch64. **The amd64 image has
not been scanned**; the Ubuntu package set and every Java finding are architecture-independent, so
the list transfers, but the digests do not.

## The four Java findings, and why each is still open

| Component | Sev | Advisory | Why |
|---|---|---|---|
| `net.sourceforge.collections:collections-generic` 4.01 | CRITICAL | CVE-2015-7501 | **No fixed version exists.** It arrives under `paxtools-core:5.1.0` and `sbgn-converter:5.0.0`, which use it for typed multimaps; excluding it means dropping BioPAX/SBGN pathway support. All three `ObjectInputStream` sites in NGB were traced and each reads bytes NGB serialised itself, so no attacker-reachable sink was found — though no bytecode call-graph was built. Left deliberately, with the reachability recorded rather than excluded blind. |
| `commons-io:commons-io` 2.8.0 | HIGH | CVE-2024-47554 | **False positive.** Trivy reads a shaded `pom.properties` inside `velocity-engine-core-2.3.jar`. The real `commons-io` on the classpath is 2.22.0, in its own jar. No version bump can make this report clean. |
| `commons-lang:commons-lang` 2.6 (CLI) | MEDIUM | CVE-2025-48924 | No fix on the 2.x line. It arrives under the CLI's hand-declared `commons-configuration:1.10`. |
| `commons-configuration:commons-configuration` 1.10 (CLI) | LOW | CVE-2025-46392 | Same — no fix on the 1.x line. Closing both means replacing `commons-configuration` 1.x with `commons-configuration2` in `ConfigurationLoader`, a CLI rewrite rather than a version bump. It would also retire the `commons-collections:3.2.2` pin below. |

## The 29 OS findings, and the base image

**Not one of them has a fixed Ubuntu 22.04 package**, so `apt-get upgrade` in the Dockerfile buys
nothing, and the count is identical to the bare `eclipse-temurin:21-jre-jammy` base — NGB adds no OS
packages with findings. Moving to `eclipse-temurin:21-jre-noble` (Ubuntu 24.04) was scanned for
comparison: 18 findings instead of 29, but **the same four MEDIUM CVEs** (CVE-2021-31879,
CVE-2025-66382, CVE-2026-13757, CVE-2026-27456) survive the move; it only drops five LOW ids and
adds one. A noble bump is defensible on jammy's support horizon. It is not a vulnerability fix, and
presenting it as one would be misleading.

## What must not be "fixed"

| Item | Why not |
|---|---|
| **`commons-validator` 1.5.0** (`server/catgenome/build.gradle`) | A deliberate pin. 1.7 tightened the query-string charset to RFC 3986, and NGB's own generated URLs carry `{ } " \|` unencoded, so bumping breaks `POST /generateShortUrl` and `ngb url --alias` against the format documented in `docs/md/user-guide/embedding-url.md`. `UrlShorterManagerTest` holds the tripwire. No advisory in any scan targets it. |
| **`runtimeOnly commons-collections:3.2.2`** (`server/ngb-cli/build.gradle`) | `commons-configuration` 1.10 declares commons-collections `<optional>true</optional>` and calls it at runtime; dropping the pin makes **every** `ngb` invocation fail at startup. 3.2.2 is the patched release — the serialisation gadget is disabled by default — so this is not the `collections-generic` problem. |
| **The client's 26 production findings** | The AngularJS client and its Node 14.17.5 toolchain are frozen. 4 of the 13 `angular` rows are fixable only by moving to AngularJS 1.6/1.7/1.8; the other 9 have no fix at all. A fix that unfreezes the client is a different project, not a patch. |

## Reading a report honestly

* **Absence of a finding is not absence of a vulnerability.** Trivy matches published advisories
  against `pom.properties`/`MANIFEST.MF` coordinates. Anything shaded without correct metadata is
  invisible to it — the `commons-io` row above is the same mechanism producing a false *positive*,
  and false negatives come just as easily. Two shipped jars carry no usable metadata at all
  (`ngb-cli-3.0.0.jar`; `igv-v2.6.3.jar` and the `flatDir` jars are matched by filename heuristics).
* **Severities are the advisories' own.** Reachability has been traced for exactly two things here
  (log4j 1's appenders, and CVE-2015-7501's deserialisation sinks), both with stated limits.
* **A closed finding means the vulnerable version is no longer shipped**, not that it was
  exploitable before.
* Bumps that touch the SAML stack, the cloud clients, the logging bridge or a compression codec need
  more than a build: see "What a green suite does not tell you" in
  [`TEST-BASELINE.md`](TEST-BASELINE.md) for the scripts that cover those paths — a scan-driven bump
  of Bouncy Castle, httpcomponents, log4j or snappy was verified with `make up-saml` +
  `make saml-verify-signing`, `make verify-cloud` and `make verify-tracks`, not with `make test`
  alone.
