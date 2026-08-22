# NGB — working notes for Claude

New Genome Browser: a Spring server that indexes and serves genomic file formats (BAM/CRAM,
VCF, BED, WIG, GFF/GTF, SEG, MAF) to a browser client.

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

## The Java 21 migration (branch `java_21`) — done, not yet merged

NGB 3.0.0 moved off Java 8 / Spring Boot 1.5 across Phases 0–9 on `java_21`, followed by four
vulnerability fixes. Nothing is left to execute; what remains is the merge into `develop`. Four
documents in `.devenv` are the record, and comments in `build.gradle`, the CI workflow and a
couple of source files cite them by name:

| Document | What it is |
|---|---|
| [`JAVA21-MIGRATION-SUMMARY.md`](.devenv/JAVA21-MIGRATION-SUMMARY.md) | why the diff is safe — every behaviour change an upgrader meets, in three buckets, with the evidence for each |
| [`JAVA21-VULNERABILITY-REVIEW.md`](.devenv/JAVA21-VULNERABILITY-REVIEW.md) | the shipped stack's advisories (§1–§6) and what the four fixes closed (§7) |
| [`JAVA21-MIGRATION-PLAN.md`](.devenv/JAVA21-MIGRATION-PLAN.md) | the specification — current-state inventory, the decision table D1–D14, Phases 0–9 and their findings |
| [`JAVA21-MIGRATION-EXECUTION.md`](.devenv/JAVA21-MIGRATION-EXECUTION.md) | how the sessions were run — progress table, session settings, per-phase prompts and caveats |

Two of the migration's rules outlive it:

1. The decisions in the plan's "Decisions already taken" table are binding — several dependency
   pins, the CI workflow and `LuceneIndexVersionException`'s javadoc cite them by id, so changing
   one is a documentation change too. If one turns out to be unworkable, stop and say so rather
   than quietly choosing differently.
2. Never re-baseline a failing test to make it green. Fix the cause, or explain why it is a
   deliberate documented exclusion and record it in
   [`.devenv/TEST-BASELINE.md`](.devenv/TEST-BASELINE.md).

Upgrade instructions for operators are in the published docs, not here:
`docs/md/release-notes/3.0.0/`, `docs/md/installation/database-upgrade.md` and
`docs/md/installation/lucene-reindex.md`.
