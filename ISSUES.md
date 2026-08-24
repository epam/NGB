# Known defects, not yet fixed

Four defects, deliberately left alone rather than fixed in passing, so that they can be filed, argued
about and scheduled on their own. The first three were found while preparing the 3.0.0 platform
release: each is older than that release, none is a regression it introduced, and fixing any of them
is a behaviour change that has nothing to do with moving to Java 21. The fourth is different — it is
a one-line wart in the repository's own tooling, introduced by the 3.0.0 work itself, and it is here
because it quietly misleads anyone searching the tree.

Each section is meant to be filed as an issue as it stands — the heading is the title, the body is
the body. Line numbers are as of the 3.0.0 release.

- [1. `isRemotePath` tests for `ftsp:`, so `ftp://` paths are read as local files](#1-isremotepath-tests-for-ftsp-so-ftp-paths-are-read-as-local-files)
- [2. Properties read through `#{catgenome[…]}` cannot be overridden on the command line](#2-properties-read-through-catgenome-cannot-be-overridden-on-the-command-line)
- [3. Six manager roles that `@PreAuthorize` requires are not created by any installation](#3-six-manager-roles-that-preauthorize-requires-are-not-created-by-any-installation)
- [4. `.gitignore:17` is not a valid glob, so `ripgrep` searches the directories it protects](#4-gitignore17-is-not-a-valid-glob-so-ripgrep-searches-the-directories-it-protects)
- [Documentation errors, already corrected](#documentation-errors-already-corrected)

---

## 1. `isRemotePath` tests for `ftsp:`, so `ftp://` paths are read as local files

**Where.** `server/catgenome/src/main/java/com/epam/catgenome/util/NgbFileUtils.java:174-175`

```java
public static boolean isRemotePath(String path) {
    return path.startsWith("http:") || path.startsWith("https:") || path.startsWith("ftsp:");
}
```

`ftsp:` is a transposition of `ftp:`. No path can begin with it, so the third test is dead and every
`ftp://` path is classified as local.

**What happens.** Ten call sites in six files ask this question, and each of them takes the local
branch for an FTP URL:

* `BiologicalDataItemResourceType.getTypeFromPath` (`:166`) returns `FILE` instead of `URL`, so the
  registration is stored as a local file;
* `FastaUtils.openBufferedReader` (`:38`) opens `new FileInputStream("ftp://…")` — `FileNotFoundException`;
* `FastaUtils.getContentLength` (`:67`) returns `new File("ftp://…").length()`, which is `0`;
* `ReferenceManager` (`:521, 536, 553`) then tries to index and GC-content-scan it as a local FASTA;
* `FileManager:2156` and `UrlValidatorService:132` likewise.

So registering a reference or a file from an FTP URL fails, or half-succeeds with a zero length, in
a way that reads like a missing file rather than an unsupported protocol.

**Why it matters.** Genome and annotation files are still commonly published over FTP (NCBI, Ensembl,
UCSC). A user pasting such a URL gets *resource not found* and no hint that the protocol is the
problem. The intent of the code is clearly that FTP should work.

**Reproduce.** `ngb reg_ref ftp://ftp.ensembl.org/…/genome.fa` — or any registration with an `ftp://`
path — and note the local-filesystem error.

**Suggested fix.** Decide which of the two the project wants, because the typo hides the choice:

1. **Make FTP work as the code intends**: `path.startsWith("ftp:")`. `htsjdk` and
   `java.net.URL` both handle `ftp://`, so `openBufferedReader` and `getContentLength` should work
   as they do for `http://`. This is a behaviour change: paths that used to be rejected start being
   accepted, and `getTypeFromPath` starts returning `URL` for them, which is what is persisted in
   `BIO_DATA_ITEM.TYPE`.
2. **Drop the branch** and reject `ftp://` explicitly with a message that says so.

Either way, add a test — `NgbFileUtilsTest` has no case for this method — and check the user
documentation, which mentions no protocol restriction beyond "URL".

---

## 2. Properties read through `#{catgenome[…]}` cannot be overridden on the command line

**Where.** `server/catgenome/src/main/resources/conf/catgenome/applicationContext-config.xml:9-21`
defines a `PropertiesFactoryBean` named `catgenome`, and 39 SpEL expressions in 18 classes read
settings out of that bean directly rather than through Spring's `Environment`.

**What happens.** The bean reads `classpath:catgenome.properties`,
`${CATGENOME_CONF_DIR}/catgenome.properties`, `./config/catgenome.properties` and
`${conf}/catgenome.properties`. Nothing else feeds it. So for these keys:

* `--conf=/dir` works, because it selects one of those files;
* `--ngs.data.root.path=/data` on the command line does **nothing**;
* the same key in `application.properties` does **nothing**;
* an environment variable or a `SPRING_APPLICATION_JSON` entry does **nothing**;

and in every case in silence — the default stays in force and nothing is logged. Keys read the
ordinary way, with `@Value("${…}")`, do honour all of the above, so which mechanism a given property
uses decides whether it can be overridden, and there is no way to tell from the outside.

The 36 affected keys, from
`git grep -h "catgenome\[" -- server/catgenome/src/main`:

```
bam.max.coverage.range              lucene.index.max.size.grouping
bam.max.reads.count                 ngb.bam.streaming.thread.keep-alive
bam.regions.count                   ngs.data.root.path
blat.search.output.type             path.style.access.enabled
blat.search.sort.order              request.async.timeout
blat.search.type                    saml.user.attributes
blat.search.url                     saml.user.role.mapping
externaldb.ncbi.max.results         search.features.internal.max.results
externaldb.proxy.host               search.features.max.results
externaldb.proxy.password           search.indexer.buffer.size
externaldb.proxy.port               server.tomcat.max-connections
externaldb.proxy.user               short.link.expired.period
file.browsing.allowed               static.resources.cache.paths
file.download.whitelist.host        static.resources.cache.period
files.download.directory.path       url.browsing.allowed
files.download.max.m.byte.size      use.embedded.tomcat
files.download.max.minutes          vcf.filter.whitelist
jwt.token.expiration.seconds        version
```

**Why it matters.** `ngs.data.root.path` and `file.browsing.allowed` decide which part of the
filesystem NGB will serve; `server.tomcat.max-connections` and `request.async.timeout` look exactly
like Spring Boot settings, which *are* command-line overridable. An operator who sets one of them the
documented Spring Boot way gets no error and no effect — including in a container, where passing
arguments is often the only configuration mechanism available.

**Suggested fix.** Register `catgenome.properties` as a Spring `PropertySource` (a
`@PropertySource`/`ConfigDataLocation` on the application, or `spring.config.additional-location`)
and replace the `#{catgenome['x']}` lookups with `@Value("${x}")`. Then the whole file participates
in Boot's normal precedence order and every key behaves the same way. The two things to watch are
`version`, which comes from `version.properties` through the same bean, and the fact that
`<context:property-placeholder properties-ref="catgenome" order="1"/>` currently gives the file
lower precedence than the environment for `${…}` placeholders — so a switch changes precedence for
nothing, but it does have to be verified per key.

**Meanwhile**, this is written down in `docs/md/installation/standalone.md` and in the 3.0.0 release
notes as a known limitation, which is the whole of the mitigation today.

---

## 3. Six manager roles that `@PreAuthorize` requires are not created by any installation

**Where.** `server/catgenome/src/main/java/com/epam/catgenome/security/acl/SecurityExpressions.java`
and `security/acl/customexpression/NGBMethodSecurityExpressionRoot.java:85-110` name the roles;
`entity/user/DefaultRoles.java` and the seed scripts under
`server/catgenome/src/main/resources/database/catgenome/{h2,postgres}/` decide which ones exist.

**What happens.** A fresh installation of either flavour has ten predefined roles: `ROLE_ADMIN`,
`ROLE_USER`, and the `REFERENCE`, `BAM`, `VCF`, `GENE`, `BED`, `WIG`, `SEG` and `TARGET` managers.
The security expressions name six more, and none of them is ever created:

| Role | Named by | Guards |
|---|---|---|
| `ROLE_HEATMAP_MANAGER` | `SecurityExpressions` | all seven write/read methods of `HeatmapSecurityService` |
| `ROLE_PATHWAY_MANAGER` | `SecurityExpressions` | pathway registration, deletion and `PUT /restapi/pathway/index` |
| `ROLE_LINEAGE_TREE_MANAGER` | `SecurityExpressions` | lineage-tree registration and deletion |
| `ROLE_PROJECT_MANAGER` | `SecurityExpressions` | most of `ProjectSecurityService` |
| `ROLE_BUCKET_MANAGER`, `ROLE_BOOKMARK_MANAGER` | `hasSpecificRole(AclClass)` | `GET /restapi/permissions` for those entity types |

So every one of those expressions reduces to `ROLE_ADMIN` (plus, where the expression offers it, the
entity's owner). `DefaultRoles` lists `ROLE_HEATMAP_MANAGER` with a `null` id, which is a code-level
enumeration, not a seed — nothing inserts it.

**Why it matters.** It is the same defect 3.0.0 already fixed once, in a narrower form: PostgreSQL
never seeded `ROLE_WIG_MANAGER`, so on a PostgreSQL install nobody but an administrator could satisfy
`WigSecurityService`'s checks, and that was treated as a production bug and fixed with a convergence
migration. The six above are the same thing on both flavours. Concretely, an operator cannot
delegate heatmap, pathway, lineage-tree or project management to a non-administrator, and
`docs/md/installation/lucene-reindex.md` tells them to use `ROLE_PATHWAY_MANAGER` for the pathway
reindex call — a role that does not exist.

**Reproduce.** On a fresh install, `GET /restapi/role/loadAll` returns ten roles and none of the
six. Grant a user every role there is and `POST /restapi/heatmap` still comes back denied.

**Workaround, which nothing documents.** An administrator can create them:
`POST /restapi/role/create?roleName=ROLE_PATHWAY_MANAGER` — `RoleManager.getValidName` adds the
`ROLE_` prefix if it is left off — and then `POST /restapi/role/{id}/assign`. The expressions match
by name, so a hand-created role works exactly like a predefined one.

**Suggested fix.** Seed the six as predefined roles in both script sets, the way
`v2026.08.21_12.20__align_predefined_roles_with_h2.sql` seeded `ROLE_WIG_MANAGER` on PostgreSQL —
one forward migration per flavour, ids allocated with `(SELECT MAX(id) + 1 FROM catgenome.role)`
rather than hardcoded, since hardcoding an id is what made the two flavours diverge in the first
place. Then add them to `DefaultRoles` and to the predefined-role table in
`docs/md/user-guide/um-overview.md`, which currently lists only the roles that really exist. Decide
per role whether the feature is meant to be delegable at all — for one of them the answer was no, and
it has already been acted on: `ROLE_MAF_MANAGER` guarded a format whose REST controller was deleted
in 2018, and the `MAF` branch of `hasSpecificRole` was dropped with the rest of MAF support in
3.0.0. That is why this finding names six roles and not the seven it was filed with.

---

## 4. `.gitignore:17` is not a valid glob, so `ripgrep` searches the directories it protects

**Where.** `.gitignore:17`, added by `82d67867` "Java 21 dev env prep"

```
/server/catgenome/${*
```

**What happens.** The line exists for a real reason: the two test profiles used to leave
directories in the source tree named literally after unresolved `${…}` properties — six Lucene index
directories plus `contents/ncbi/` — and the pattern ignores them (see the second condition in
[`.devenv/TEST-BASELINE.md`](.devenv/TEST-BASELINE.md)). Git accepts it: `git check-ignore -v`
attributes such a path to this line and `git status` stays clean.

`ripgrep` does not. It parses the line as a glob, in which `${` opens an alternate group that is
never closed, and prints on stderr — on **every** invocation, anywhere in the tree:

```
rg: ./.gitignore: line 17: error parsing glob '/server/catgenome/${*': unclosed alternate group; missing '}' (maybe escape '{' with '[{]'?)
```

It then drops that one pattern and keeps the other forty — on ripgrep 14.1.1 a partial parse failure
is not fatal — so `dist/`, `docs/site/`, `node_modules/` and `server/catgenome/build/` do stay out of
a search. What does not stay out is anything under `server/catgenome/${…}/`.

**Why it matters.** On a tree that has those directories — any checkout where the test suite was run
before they stopped being written, and they are gitignored so nothing ever cleans them up — `rg`
reads Lucene segment files and stale test output as though they were source. A symbol you have just
deleted turns up in a months-old index and looks exactly like a missed call site, while Git,
`git grep` and `git status` all agree the files are not in the repository. The error line is easy to
miss because it goes to stderr while the matches go to stdout, and most people are reading the
matches — and once that line is being skipped over, so is every other notice `rg` puts there.

**Not this defect, but the other half of why the standing advice in
[`.devenv/README.md`](.devenv/README.md) is to search with `git grep`:** `rg` also skips
dot-directories unless it is given `--hidden`, so `.devenv/` — the whole build environment, its
README and `TEST-BASELINE.md` — and `.github/` are invisible to a plain `rg` regardless of this line.

**Reproduce.**

```bash
rg --files > /dev/null                     # the parse error, on stderr

D='server/catgenome/${lucene.index.directory}'
mkdir -p "$D" && echo 'MafFile' > "$D/decoy.txt"
git check-ignore -v "$D/decoy.txt"         # .gitignore:17 - git ignores it
git status --porcelain | grep decoy        # empty
git grep -l MafFile | grep decoy           # empty
rg -l MafFile | grep decoy                 # server/catgenome/${lucene.index.directory}/decoy.txt
rm -rf "$D"
```

**Suggested fix.** Escape the brace, which is what `ripgrep`'s own hint asks for, so the line means
the same thing to both tools:

```
/server/catgenome/$[{]*
```

Git reads `[{]` as a one-character class matching `{`, and `ripgrep` then parses the line and honours
it. Escaping the `$` instead — `[$]{*` — does **not** work: the unclosed `{` is the whole problem,
and `rg` reports the identical error. Verify with the reproduction above: no parse error,
`git check-ignore` still attributing the decoy to line 17, and `rg -l` no longer finding it. Until
it is fixed, search this tree with `git grep`.

---

## Documentation errors, already corrected

Found in the same pass, and fixed rather than filed, because they were false statements in pages that
were being edited anyway. Recorded here so they are not re-reported, and because each says something
about an installation that is still out there.

| What was wrong | Where it was | Reality |
|---|---|---|
| "The docker image works out of `/opt/ngb`, not `/opt/catgenome`" — presented as a 3.0.0 change | `release-notes/3.0.0/3.0.0.md`, `installation/docker.md` | The image has used `/opt/ngb` since 2.7.1 at least. `/opt/catgenome` only ever existed in the documentation, so anyone who followed it mounted empty directories and **was never persisting anything**. |
| "a region has to be resolvable, from `AWS_DEFAULT_REGION`…", and `export AWS_DEFAULT_REGION=…` | `release-notes/3.0.0/3.0.0.md`, `installation/standalone.md` | `AWS_DEFAULT_REGION` is an AWS **CLI** variable. The string does not occur in the v2 SDK jars, nor in the v1 jars of previous releases. The SDK reads `AWS_REGION`; anyone following the old instruction was falling through to `~/.aws/config`. |
| "Supported PostgreSQL: 9.6 – 17" | `release-notes/3.0.0/3.0.0.md`, `installation/standalone.md`, `installation/database-upgrade.md` | That is the JDBC driver's range. NGB is built, tested and upgrade-rehearsed against 16 only; 17 has never been run, and 9.6 is a *source* version for the dump rather than one NGB was run against. |
| "Registration of GA4GH datasets is gone from the client and the API" | `release-notes/3.0.0/3.0.0.md` | GA4GH was never in the web client — `git grep -il ga4gh develop -- client` is empty. It existed server-side and in the CLI's resource types. |
| Nothing told an operator what happens to rows already registered from HDFS or GA4GH | nowhere | `BIO_DATA_ITEM.TYPE` 5 and 6 are now rejected as they are read, which fails the whole request and so every dataset containing such a file. `installation/database-upgrade.md` now carries the query and the procedure, and it has to be done **before** upgrading. |
