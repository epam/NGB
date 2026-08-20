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
pinned (Gradle 3.3 needs JDK 8; the database flavour is a build-time switch) and the host is
not set up for it. Start from [`.devenv/README.md`](.devenv/README.md); everything is a
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

## Java 21 migration in progress (branch `java_21`)

A phased migration off Java 8 / Spring Boot 1.5 is underway. If you are working on it:

- [`.devenv/JAVA21-MIGRATION-PLAN.md`](.devenv/JAVA21-MIGRATION-PLAN.md) is the
  specification — current-state inventory, the binding decision table, Phases 0–9.
- [`.devenv/JAVA21-MIGRATION-EXECUTION.md`](.devenv/JAVA21-MIGRATION-EXECUTION.md) is the
  operating procedure — progress table, session settings, per-phase prompts and caveats.

Standing rules while it runs:

1. The decisions in the plan's "Decisions already taken" table are binding. If one turns out
   to be unworkable, stop and say so rather than quietly choosing differently.
2. Do one phase at a time. Do not start the next.
3. A phase is done when its exit criteria have actually been run and passed — not when they
   look like they would pass.
4. Never re-baseline a failing test to make it green. Fix the cause, or explain why it is a
   deliberate documented exclusion and record it in `TEST-BASELINE.md`.
5. The plan marks items **VERIFY** because research could not settle them. Resolve each
   against reality when you reach it, and if reality differs from the plan's assumption, say
   so before working around it.
6. Write findings and decisions into the plan document as you make them. These phases are
   long enough to be compacted mid-flight; anything only in the conversation is lost.
7. One commit per phase (sub-commits within a phase are fine), prefixed `[migration N]`.
