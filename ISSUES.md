# Known defects, not yet fixed

Four defects found while preparing the 3.0.0 platform release and deliberately left alone there:
each one is older than that release, none is a regression it introduced, and fixing any of them is a
behaviour change that has nothing to do with moving to Java 21. They are written up here rather than
fixed silently so that they can be filed, argued about and scheduled on their own.

Each section is meant to be filed as an issue as it stands — the heading is the title, the body is
the body. Line numbers are as of the 3.0.0 release.

- [1. `isRemotePath` tests for `ftsp:`, so `ftp://` paths are read as local files](#1-isremotepath-tests-for-ftsp-so-ftp-paths-are-read-as-local-files)
- [2. Properties read through `#{catgenome[…]}` cannot be overridden on the command line](#2-properties-read-through-catgenome-cannot-be-overridden-on-the-command-line)
- [3. The OpenAI client sends the API key in a header named `bearer`](#3-the-openai-client-sends-the-api-key-in-a-header-named-bearer)
- [4. `GOOGLE_MED_PALM2` is an accepted LLM provider with nothing behind it](#4-google_med_palm2-is-an-accepted-llm-provider-with-nothing-behind-it)
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

## 3. The OpenAI client sends the API key in a header named `bearer`

**Where.** `server/catgenome/src/main/java/com/epam/catgenome/manager/llm/OpenAIClient.java:59-66`

```java
public OpenAIClient(final String openAIKey, final String endpoint) {
    final List<Header> headers = Collections.singletonList(new Header("bearer", openAIKey));
    this.client = new OpenAIClientBuilder()
            .credential(new KeyCredential(openAIKey))
            .endpoint(endpoint)
            .clientOptions(new HttpClientOptions().setHeaders(headers))
            .buildClient();
}
```

**What happens.** Every request on this path carries an extra header, literally `bearer: <api key>`,
*in addition to* whatever the `KeyCredential` sets (`api-key` for an Azure endpoint,
`Authorization: Bearer …` for `api.openai.com`). No service reads a header called `bearer`, so it
authenticates nothing; it is almost certainly a mangled `Authorization: Bearer <key>`.

This constructor is reached only from `CustomOpenAILLMClient`, i.e. from the `llm.custom.type=openai`
configuration.

**Why it matters.** The credential is duplicated into a non-standard header that goes to whatever
`llm.custom.url` points at, and through any proxy in between — a place where request headers are
routinely logged. It is a needless second copy of a secret, on the one code path where the endpoint
is operator-supplied rather than Azure's.

**Suggested fix.** Delete the `clientOptions(…)` line: `.credential(new KeyCredential(key))` already
authenticates, and the one-argument constructor above it does exactly that. If the intent was to
support an endpoint that wants a raw bearer token, spell it correctly —
`new Header("Authorization", "Bearer " + key)` — and say in a comment which service needs it.

---

## 4. `GOOGLE_MED_PALM2` is an accepted LLM provider with nothing behind it

**Where.** `server/catgenome/src/main/java/com/epam/catgenome/entity/llm/LLMProvider.java:28`

```java
public enum LLMProvider {
    OPENAI_GPT_35, OPENAI_GPT_40, GOOGLE_MED_PALM2, CUSTOM;
}
```

**What happens.** No handler is registered under `GOOGLE_MED_PALM2`, and nothing else in the server
or the client mentions it — `git grep -rn GOOGLE_MED_PALM2 -- server client` finds the declaration
and nothing more. Because it is a valid enum value, `?provider=GOOGLE_MED_PALM2` binds successfully
on all three `/restapi/llm/*` endpoints, and the failure happens later, in `LLMService.getHandler`
(`manager/llm/LLMService.java:99-102`):

```java
Assert.notNull(handler, provider + " is not supported.");
```

which `ExceptionHandlerAdvice` turns into NGB's generic error result —
`{"status":"ERROR","message":"GOOGLE_MED_PALM2 is not supported."}`. Were the value not in the enum,
Spring's parameter binding would have rejected the request with a 400 before any handler ran.

3.0.0 removed the sibling `GOOGLE_PALM_2` — the one that had a handler and a client entry — when
Google retired the API. This one was already an orphan before that and was left as it was.

**Why it matters.** Small: the client does not offer it, so only a direct API caller can reach it,
and the error message is at least accurate. But it is an enum value advertising a provider that
cannot work, and the OpenAPI schema of the three `/llm/*` endpoints — and so any client generated
from it — offers four providers where there are three.

**Reproduce.** `POST /restapi/llm/chat?provider=GOOGLE_MED_PALM2` with any message body.

**Suggested fix.** Delete the value. It is not persisted anywhere — no column holds an `LLMProvider`
— so there is no migration; check the OpenAPI schema and the client's model list afterwards, both of
which already list only the three real providers.

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
