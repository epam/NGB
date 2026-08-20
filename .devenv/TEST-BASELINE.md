# Test baseline before the Java 21 migration

Recorded on 2026-08-20 at commit `a74f8346`, inside this environment (JDK 8, Gradle 3.3,
H2 1.3.176 / PostgreSQL 9.6, aarch64/colima). **These tests already fail on the current
code.** The point of writing them down is so that during the migration you can tell new
breakage from old — a green-except-this-list run is a pass.

| Suite | Command | Result |
|---|---|---|
| H2 | `make test` | 537 tests, **19 failed**, 26 skipped (~4 min) |
| PostgreSQL | `make test-pg` | 537 tests, **65 failed**, 26 skipped (~7 min) |
| Static analysis | `make lint` | checkstyle **clean**, pmd **fails: 4 violations** (~20 s) |

Neither suite is green out of the box, and both numbers are *after* the fixes described in
`README.md` (without them the app can't even start on an empty database, and `test-pg`
failed 246).

## H2: 19 failures

| Count | Class | Cause |
|---|---|---|
| 9 | `BlastTaskDaoTest` | `NULL not allowed for column "BLAST_TASK_ID"`. Commit f72202ba added the column as `NOT NULL` and made the DAO insert it, but the test never sets `blastTaskId`. Genuinely broken test, unrelated to environment. |
| 5 | `VcfManagerTest` | **Stale committed index.** `Felis_catus.vcf.idx` records a source size of 5898 bytes; `Felis_catus.vcf` is 5995. htsjdk seeks to a stale offset and reads a partial line → `TribbleException: there aren't enough columns for line chrA1`. The `.idx` even embeds the original author's absolute path (`/home/kite/workspace/...`). Verified: delete that one file and all 5 pass. |
| 1 | `VcfManagerTest.testLoadSmallScaleVcfFileGa4GH` | `AccessDeniedException: Parameter path doesn't fall into 'ngs.data.root.path'`. GA4GH/Google Genomics path handling; separate from the index issue above. |
| 1 | `GffManagerTest.testLoadGenesTranscript` | `expected:<protein_coding> but was:<protein_coding_CDS_not_defined>` — fixture vs. current parser expectations. |
| 1 | `HomologeneManagerTest.searchTest` | `IndexNotFoundException: no segments* file found in .../contents/taxonomy` — needs a prebuilt Lucene taxonomy index the repo doesn't ship. |
| 1 | `PdbDataManagerTest.testParse` | `expected:<B> but was:<A>` — parses live RCSB PDB data, so it drifts with the external service. |
| 1 | `TargetManagerTest.loadTargetsTest` | `expected:<2> but was:<3>` — leftover state in the shared targets Lucene index. |

Only the `PdbDataManagerTest` failure depends on the network. Nothing here is arm64- or
container-specific.

## PostgreSQL: 65 failures

The extra 46 relative to H2 come almost entirely from one structural problem plus one
schema divergence:

- **~45 failures — the security/ACL/JWT tests run on H2 even in the PostgreSQL suite.**
  `AbstractSecurityTest`, `AbstractACLSecurityTest` and `JwtAuthenticationTest` load
  `test-catgenome-acl.properties` / `test-catgenome-auth.properties`, which hardcode
  `jdbc:h2:mem:test_catgenome`. But `-Pdatabase=postgres` swaps the Flyway script set to
  the postgres one, so postgres SQL is applied to an H2 database and the context fails to
  start: `Function "SETVAL" not found`. Affects `NGBSessionSharingSecurityTest`,
  `ProjectSecurityServiceTest`, `AclPermissionSecurityServiceTest`,
  `UserSecurityServiceTest`, `DataItemSecurityServiceTest`, `BamSecurityServiceTest`,
  `AuthManagerTest`, `JwtAuthenticationTest`. Fixing it means giving those tests a
  flavour-appropriate datasource; until then the PostgreSQL suite says nothing about ACL
  or auth code.
- **9 failures — `BlastTaskDaoTest`**, same `blast_task_id` bug as on H2.
- The remaining ~11 (`VcfManagerTest`, `BookmarkDaoTest`, `VcfFileDaoTest`, `RoleDaoTest`,
  `GffManagerTest`, `TargetManagerTest`, `PdbDataManagerTest`,
  `ProteinSequenceManagerIntegrationTest`) overlap with the H2 list or are flavour-specific
  DAO/SQL differences.

## Static analysis: 4 pre-existing PMD violations

`checkstyleMain` is clean. `pmdMain` fails, so **`make lint` is red before you change
anything** — don't read that as your own breakage:

| File | Rule |
|---|---|
| `manager/externaldb/EnsemblDataManager.java:108` | `AvoidCatchingNPE` ×2, `AvoidCatchingGenericException` |
| `manager/target/AlignmentManager.java:169` | `AvoidCatchingGenericException` |

Both are `catch (Exception | NullPointerException)` blocks around external-service calls.
PMD 5.5.1 itself is on the upgrade list (it doesn't parse Java 9+ syntax), so expect this
to move during checkpoint 1 — worth fixing the four sites then rather than suppressing
them. Report: `server/catgenome/build/reports/pmd/main.html`.

## Suggested cleanups (not done here — outside the environment's scope)

Cheap wins, in order of payoff:

1. Regenerate or delete `server/catgenome/src/test/resources/templates/Felis_catus.vcf.idx`
   → 5 tests. htsjdk rebuilds the index when it's absent.
2. Set `blastTaskId` in `BlastTaskDaoTest` → 9 tests on both flavours.
3. Give the security/ACL/JWT tests a datasource matching `-Pdatabase` → ~45 tests on
   PostgreSQL.

That would take the H2 suite to ~4 failures and the PostgreSQL suite to ~11, which is a
usable regression signal for the migration. Worth doing *before* changing library
versions, so the migration starts from a known-good line.

## Reproducing

```bash
make test                      # H2
make test-pg                   # PostgreSQL (wipe first with `make reset-pg` for a clean run)
make test-one T=VcfManagerTest # single class
```

HTML reports land in `server/catgenome/build/reports/tests/test/index.html`, XML in
`server/catgenome/build/test-results/`.
