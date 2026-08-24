# NGB — working notes for Claude

New Genome Browser: a Spring server that indexes and serves genomic file formats (BAM/CRAM,
VCF, BED, WIG, GFF/GTF, SEG) to a browser client.

| Path | What |
|---|---|
| `server/catgenome` | the server — Spring Boot, Spring JDBC (no JPA), Lucene feature indexes, htsjdk parsing |
| `server/ngb-cli` | standalone Java CLI, talks to the server's REST API |
| `client` | AngularJS 1.5 web client |
| `export-templates/target-identification` | a second, separate frontend (Preact + Vite) that builds the target-identification report template |
| `docs` | mkdocs site |
| `.devenv` | containerised build/test/run environment — **use this, see below** |

Default branch is `develop`.

## Building and testing

**Builds and tests run inside the `.devenv` containers, not on the host.** The toolchain is
pinned (the toolbox image installs JDK 21 for the server and 17 for the CLI, Gradle comes from
the wrapper at 8.14.5, and the database flavour is a build-time switch) and the host is not set
up for it. Start from [`.devenv/README.md`](.devenv/README.md); everything is a
`make` target run from `.devenv/`:

```
make jar        # full build (~7 min)      make test      # H2 unit tests (~4 min)
make jar-fast   # server only (~45 s)      make test-pg   # PostgreSQL unit tests (~7 min)
make up         # run it                   make lint      # checkstyle + pmd
make smoke      # is it answering?         make help      # everything else
```

Two things that will otherwise cost you an hour:

- **Pass an explicit timeout on build and test commands.** The default is 120 s; the commands
  above take 4–7 minutes. A timeout is not a build failure.
- **`make test` and `make lint` are not green on a clean checkout.** The known-failing set is
  recorded in [`.devenv/TEST-BASELINE.md`](.devenv/TEST-BASELINE.md). Check there before
  concluding you broke something.

Never create a git worktree for work in this repo: `.devenv/docker-compose.yml` bind-mounts
`../:/workspace`, so a worktree elsewhere on disk is invisible to the containers and every
`make` target would silently run against the original tree.

## Where things are written down

| Document | What it is |
|---|---|
| [`.devenv/README.md`](.devenv/README.md) | the dev environment — services, targets, the stack, and which target exercises each part of it |
| [`.devenv/TEST-BASELINE.md`](.devenv/TEST-BASELINE.md) | what a green run looks like, which failures are expected and why, and what a green run still does not tell you |
| [`.devenv/SECURITY-SCAN.md`](.devenv/SECURITY-SCAN.md) | how to scan the built artefacts for known vulnerabilities, which findings are open and which must not be "fixed" |
| [`.devenv/fixtures/README.md`](.devenv/fixtures/README.md) | the gitignored NGB 2.x databases and Lucene 6 indexes the upgrade path is tested against, and how to rebuild them |
| [`ISSUES.md`](ISSUES.md) | known defects, each written up ready to be filed, deliberately not fixed |
| `docs/md/` | the published documentation. Anything an operator has to do belongs here, not in these notes — `release-notes/`, `installation/database-upgrade.md`, `installation/lucene-reindex.md` |

Two rules:

1. **Never re-baseline a failing test to make it green.** Fix the cause, or explain why it is a
   deliberate documented exclusion and record it in
   [`.devenv/TEST-BASELINE.md`](.devenv/TEST-BASELINE.md).
2. **A pin in `build.gradle` with a comment on it is deliberate.** Several dependencies are held
   at a version that is not the newest, and the comment says what breaks if you move it — read it
   before bumping, and if it turns out to be wrong, say so rather than quietly changing it.
