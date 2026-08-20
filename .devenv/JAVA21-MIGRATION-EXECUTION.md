# Java 21 migration — how to run the sessions

Companion to [`JAVA21-MIGRATION-PLAN.md`](JAVA21-MIGRATION-PLAN.md), which is the
specification. This file is the *operating procedure*: ready-to-paste prompts, one per
session, plus the per-phase caveats worth adding to them.

The migration is designed as **one session per phase**, each ending in a commit that leaves
the build green against the recorded baseline. Phase 2 is large enough to want sub-commits;
Phases 4 and 5 are the two where a session is expected to come back with a question rather
than a finished phase.

---

## Progress

Tick these off as they land. Keep this table current — it is the fastest way for a fresh
session to know where it is.

| Phase | What | Commit | Done |
|---|---|---|---|
| 0 | Baseline stabilisation + JDK 17 in the toolbox | | ☐ |
| 1 | Remove dropped functionality (HDFS, GA4GH, desktop, WAR, `person`, OAuth2, PaLM 2, Singularity, Sonar) | | ☐ |
| 2 | Gradle 7.6 + Spring Boot 2.7.18 + Lombok on JDK 17 | | ☐ |
| 3 | Spring Boot 3.5 + jakarta + Gradle 8 on JDK 21, security reduced to anonymous | | ☐ |
| 4 | SAML2 + JWT on Spring Security 6 | | ☐ |
| 5 | Flyway 10/11, H2 2.x, PostgreSQL 16, HikariCP | | ☐ |
| 6 | Lucene + reindex procedure + startup guard | | ☐ |
| 7 | htsjdk latest, fork deleted, index cache dropped | | ☐ |
| 8 | Remaining libraries and API polish | | ☐ |
| 9 | Packaging, CI, docs, release | | ☐ |

---

## Session 1 — kickoff (Phase 0)

The plan document may still be untracked; this prompt has the session commit it first, so
the specification is on the branch before any code moves.

```
Execute Phase 0 of the Java 21 migration.

Read .devenv/JAVA21-MIGRATION-PLAN.md first — it is the complete
specification, written for this purpose. Also read .devenv/README.md
(the containerised build/test environment) and .devenv/TEST-BASELINE.md
(what already fails before you touch anything). Work on the current
java_21 branch.

Rules for this and every migration session:

- The 14 decisions in the plan's "Decisions already taken" table are
  binding. Do not re-open them. If one turns out to be unworkable,
  stop and tell me rather than silently choosing differently.
- Do only the phase I name. Do not start the next one.
- Verify with the .devenv Makefile targets the phase lists. A phase is
  not done until its exit criteria actually pass — run them, don't
  reason about them.
- The plan marks 12 items VERIFY (versions, API availability). Resolve
  each against reality when you reach it. If reality differs from what
  the plan assumes, say so before working around it.
- Never re-baseline a failing test to make it pass. If a test fails,
  either fix the cause or explain why it's a deliberate documented
  exclusion.
- Report honestly: if something is blocked or you skipped it, say which
  and why. Finish everything else.

Start by committing the plan document itself (and this execution guide)
if they are untracked, then do Phase 0. End with one commit per logical
step, message prefixed "[migration 0]", and the rewritten
TEST-BASELINE.md that Phase 0 requires.
```

---

## Sessions 2+ — per-phase prompt

Paste this, substituting the phase number, then append the phase's caveat block from the
next section if it has one.

```
Continue the Java 21 migration: execute Phase N.

Read .devenv/JAVA21-MIGRATION-PLAN.md for the phase spec and the
binding decision table, .devenv/JAVA21-MIGRATION-EXECUTION.md for where
the migration currently stands, and .devenv/TEST-BASELINE.md for the
current expected-failure list. Branch: java_21.

Same rules as before: this phase only; decisions in the table are not
open for revision; resolve the phase's VERIFY items against reality and
flag any divergence before working around it; verify with the .devenv
make targets the phase lists and don't declare done until the exit
criteria pass; never re-baseline a failure to make it green.

Confirm the previous phase's commit is in place and its exit criteria
still hold before you start. Commit as "[migration N] ...", update
TEST-BASELINE.md at the phase boundary if the numbers moved, and tick
the phase off in the Progress table of
.devenv/JAVA21-MIGRATION-EXECUTION.md.
```

---

## Per-phase caveats to append

Append these to the per-phase prompt. Each one exists because the phase has a specific way
of going wrong quietly.

### Phase 1

```
Two things in Phase 1 need care beyond the deletion lists:

- Removing HDFS(5) and GA4GH(6) from BiologicalDataItemResourceType
  leaves numeric gaps that must NOT be closed — DOWNLOAD(7) and AZ(8)
  keep their ids because they are persisted. Existing rows with type 5
  or 6 become unmappable; detect and report them clearly rather than
  letting getById return null into an NPE.
- Before deleting the legacy `person` package, confirm the client talks
  to manager/user's controller and not to controller/person. If any UI
  path resolves there, port it first and tell me.
```

### Phase 2

```
Phase 2 is the largest phase and has no compiling midpoint. Work it as
three sub-commits: (a) Gradle 7.6 + dependency configurations + Lombok
+ toolchain, (b) Spring Boot 2.7.18, (c) static analysis. Only (c) has
to leave the build green; tell me before moving between sub-commits.

The load-bearing assumption of this phase is that
spring-security-saml2-core 1.0.10 works with Spring Security 5.x, so
SAML survives the waypoint. Verify that early. If it doesn't hold, stop
and tell me — the fallback is to run this phase with SAML disabled, but
that is my call, not yours.

The PMD 5→7 ruleset rewrite will surface new violations. Fix them.
Do not blanket-suppress: Phase 0 left `make lint` green, so anything
new is either a real finding or a rule worth consciously disabling —
and if you disable one, say which and why.
```

### Phase 3

```
Phase 3 deliberately lands with security reduced to anonymous. When you
report back, enumerate exactly which test classes you disabled or
removed to get there, and record that list in both the commit message
and TEST-BASELINE.md — Phase 4 has to re-enable precisely that set.

Recover the deleted SAML/JWT classes from git in Phase 4 rather than
keeping them around in the tree; they are the specification for what
the attribute mapping and JWT claims must keep doing.

Scope the javax→jakarta rename to javax.servlet and javax.xml.bind
only. javax.xml.xpath/parsers/datatype/stream/namespace, javax.net.ssl
and javax.naming are still JDK packages and must not be renamed.
```

### Phase 4

```
Expect to come back to me with a question rather than a finished phase.

Spring Security 6's default SAML2 endpoint paths differ from the
OpenSAML 2 extension's (/saml2/service-provider-metadata/{id} vs
/saml/metadata, /login/saml2/sso/{id} vs /saml/SSO). Preserving the
legacy paths matters because existing customer IdP registrations would
otherwise all need reconfiguring. Find out whether the legacy paths can
be configured, and bring me the answer before committing to either
approach.

Use `make smoke-saml` to iterate, not a browser — it prints each step
of the web SSO profile. Before declaring the phase done, turn signature
validation ON in the dev Keycloak realm at least once, otherwise the
keystore and signing path are untested.
```

### Phase 5

```
This is the highest data-loss risk in the migration. Do not accept a
fresh-install-only verification. Test the upgrade path from an H2
database and a PostgreSQL database created by pre-migration code, and
write down exactly what an operator has to do.

Verify early that Flyway 10 parses the existing version format
(v2016.11.21_17.58__Name.sql) identically to Flyway 3.2.1. If it does
not, applied-migration history breaks on every existing database and
the phase's approach is invalid — stop and tell me, don't work around
it.

The plan asks you to choose between making the 58 H2 scripts H2-2.x
compatible and introducing a squashed baseline. Recommend one, tell me
which you chose and why, and record the decision in the plan document.
```

### Phase 6

```
Decide Lucene 9.12.x vs 10.x at the start of this phase and record the
choice with your reasoning in the plan document. The plan recommends
9.12.x; both require the same full reindex.

The startup/first-read guard is not optional and is not a nice-to-have.
An IndexFormatTooOldException stack trace on a customer's index is a
support incident; a message naming the directory and the reindex
command is a documented upgrade step.

If any of the seven index types has no rebuild path today, closing that
gap is part of this phase. Say so if you find one.
```

### Phase 7

```
Unit tests will not catch what this phase can break. Before declaring
it done, load each track type end-to-end through the UI: BAM (and CRAM
if available), VCF, BED, BedGraph, WIG, GFF/GTF, GenePred, SEG, MAF, a
Tabix-indexed file, and one remote (S3 or HTTP URL) track.

GffManagerTest.testLoadGenesTranscript is expected to move in this
phase. Decide which side is right — fixture or parser — and fix it.
Do not re-baseline it.

EnhancedUrlHelper's 403-tolerance exists so S3 pre-signed URLs work.
That behaviour must survive even if the htsjdk SPI it hooks into has
changed shape.
```

### Phase 8

```
These items are largely independent. Do them as separate commits and
verify each — AWS SDK v1→v2 and POI 3→5 are both full API changes and
should not share a commit.

The Swagger→springdoc annotation rewrite across 42 files is cosmetic
(springdoc already generates docs without annotations after Phase 3).
If the phase runs long, that is the part to defer — tell me rather than
rushing it.
```

### Phase 9

```
Two decisions for me in this phase: the release version number (the
plan notes that a change this breaking argues for 3.0.0 rather than
2.8.0), and whether nginx in docker/core/Dockerfile is actually used or
can be dropped.

The final pass over .devenv is part of this phase: the environment
should describe the new reality, not the migration. That means dropping
JDK 8 from the toolbox, deleting `make probe-java21`, and rewriting the
README sections that are historical by then — including the two "look
arbitrary but aren't" SAML notes, which stop being true once Phase 4
removes the hardcoded port rewrite.

The goal for TEST-BASELINE.md is zero unexplained failures. Anything
left must be a deliberate, documented exclusion.
```

---

## Resuming a phase that was interrupted

```
Resume the Java 21 migration mid-phase.

Read .devenv/JAVA21-MIGRATION-PLAN.md (spec),
.devenv/JAVA21-MIGRATION-EXECUTION.md (progress) and
.devenv/TEST-BASELINE.md. Branch: java_21.

Before doing anything: work out where the previous session actually got
to. Check git log and git status against the phase's task list, and run
the phase's verification commands to establish the current state. Tell
me what you found — what's done, what's half-done, what's untouched —
before you continue.

Then finish the phase under the same rules: binding decision table,
this phase only, exit criteria must actually pass, no re-baselining.
```

---

## If a session proposes going off-plan

The plan is research-backed but it is not infallible; twelve items are explicitly marked
VERIFY precisely because they could not be settled without doing the work. A session that
comes back with *"the plan assumes X, but X is not true, here is what I found"* is working
correctly. A session that quietly substitutes its own approach is not.

Useful reply when that happens:

```
Update .devenv/JAVA21-MIGRATION-PLAN.md to reflect what you found —
correct the assumption, record the new decision and its reasoning, and
add it to the decision table if it's the kind of thing a later phase
would otherwise re-open. Then continue.
```

The plan is a living document. Keeping it accurate is cheaper than rediscovering the same
finding three phases later.
