<!-- 20260829_openai: Revised the diagnostics refactoring backlog to separate source presence, test/build evidence, and runtime proof; corrected candidate priority; preserved upstream and worktree boundaries. -->
<!-- 20260829_openai: Linked the separately documented automatic pressure-deposit composition regression without treating documentation as re-enablement approval. -->
<!-- 20260829_openai: Selectively reconciled the archive-only independent review without deleting the live dirty-worktree implementation evidence or combining diagnostics refactoring with automatic-pressure restoration. -->
<!-- 20260830_openai: Scoped the dormant activation row to its original snapshot and linked the later automatic-deposit incident diagnostics review. -->
<!-- 20260907_kpopmodder: Linked the implemented targeted ledger, Slice A test, and residual StoreHome folderization record while preserving this historical snapshot. -->
<!-- 20260907_kpopmodder: Qualified the follow-up verification as mixed-worktree integration evidence rather than an isolated rollback proof. -->

# ChatClef Diagnostics Refactoring Backlog - Evidence-Gated Revision

Date: 2026-08-29

The current-source 2026-09-07 follow-up for
`StoreDepositAutomaticLifecycleLedger`,
`StoreDepositSliceADiagnosticsContractTest`, and the residual
`StoreHomeTimeoutDiagnostics` work is owned by
[Targeted Diagnostics Refactoring and Folderization Implementation Record](chatclef-targeted-diagnostics-refactoring-folderization-plan-2026-09-07.md).
That record does not replace this document's historical evidence. Its recorded
StoreHome size and partial-extraction status describe the exact pre-change
source; the same record separately captures the completed refactor and offline
verification. That verification was performed on the complete dirty worktree,
whose Gradle source set also contained the separately owned natural-response
Java GET-effect/result-projection slice; it is cross-unit integration evidence,
not an isolated diagnostics-only build or a combined source rollback unit.
Deployment and Minecraft runtime verification remain `NOT_RUN`.

## Review basis and authority

This document supersedes the earlier 2026-08-29 backlog draft for planning
purposes. It remains documentation only. It does not authorize Java source
changes, behavior changes, build or test execution, runtime reproduction,
commit, push, reset, clean, stash, checkout, or any other non-document worktree
mutation.

The reviewed source snapshot is:

```text
14ba9b443f0bc11d6860a25d7fd3b8b916d95a04
```

The supplied review archive did not contain `.git` metadata, so that archive by
itself could not prove the live branch, current `HEAD`, upstream divergence,
dirty worktree state, untracked files, deployed JAR identity, or whether the
archive exactly matched the user's local repository.

Before applying this documentation-only revision in the live repository, the
following read-only preflight was observed:

```text
cwd: C:\Vtuber_Souorce_Code\LAVI
git root: C:/Vtuber_Souorce_Code/LAVI
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: 14ba9b443f0bc11d6860a25d7fd3b8b916d95a04
upstream: origin/minecraft-plugin-fix/alto-clef-infinite-loop
git status --short: ?? plugins/Minecraft/docs/chatclef-diagnostics-refactoring-backlog-2026-08-29.md
git diff --name-status: no output
git diff --cached --name-status: no output
```

The `??` entry is the already-created untracked documentation file being revised
by this document-only task.

Review actions performed for this revision:

```text
static source inspection: YES
static test-source inspection: YES
production reference scan: YES
source line count: YES, nonblank physical lines
build execution: NO
test execution: NO
runtime reproduction: NO
commit or push: NO
```

Historical build and runtime records in companion repository documents remain
historical evidence. They must not be promoted to current verification merely
because this backlog references them.

## Evidence model

Every future report in this area must keep the following questions separate:

1. Does the source or registration path exist?
2. Did the relevant test set and authorized build pass for the exact source and
   artifact under review?
3. Did the exact user-visible runtime path execute and produce the expected
   terminal evidence?

Use precise evidence labels instead of a single broad `verified` claim:

| Label | Meaning |
| --- | --- |
| `SOURCE_CONFIRMED` | The class, call path, registration, or guard is present in the inspected source snapshot. |
| `SOURCE_CONFIRMED_DORMANT` | The implementation exists, but the inspected production composition has no construction/tick path for it. |
| `SOURCE_CONFIRMED_ACTIVE` | The inspected source contains the production construction and tick path. This does not imply test, build, artifact, or runtime success. |
| `TEST_SOURCE_PRESENT` | Relevant test source exists, but no test result is implied. |
| `TEST_PASSED_CURRENT` | The named test command passed against the exact current source. Command and result must be recorded. |
| `BUILD_PASSED_CURRENT` | The authorized build passed against the exact current source. Artifact identity must be recorded when deployment matters. |
| `HISTORICAL_RUNTIME_EVIDENCE` | A named companion record reports a prior runtime result. This does not prove the current source or artifact. |
| `RUNTIME_PATH_PROVEN_CURRENT` | The exact entry path, artifact, operation, and terminal evidence were reproduced for the current task. |
| `NOT_ASSESSED` | The review did not establish the point. |

A test for a Task does not prove command registration. A command registration
test does not prove chat or microphone routing. A direct `@store_home` runtime
does not prove Korean chat routing, and Korean chat routing does not prove
microphone parity. These paths must be reported separately.

## Scope and provenance

Primary inspection scope:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/**
```

Excluded from routine refactoring by default:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/**
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/baritone/**
```

The `lavi/**` namespace is an inspection boundary, not automatic proof that
every file is ordinary LAVI-owned production code. `AGENTS.md` requires
provenance inspection before ownership classification. Before any source edit,
classify every affected file as LAVI-owned, upstream-derived, vendored,
generated, or third-party-derived and apply the narrowest applicable rule.

Existing upstream-facing call sites are a hard compatibility boundary. A facade
extraction must not be used as a reason to rename, move, or rewrite call sites
inside `adris/**` or `baritone/**`.

## Current feature activation and evidence status

The following matrix describes only what this review established from the
inspected source and named companion records.

| Feature or path | Source and registration | Test/build evidence in this review | Runtime evidence in this review | Current classification |
| --- | --- | --- | --- | --- |
| Manual `@deposit_all` | Command source and registration are present in the inspected source. | No test or build was run for this document revision. | Not reproduced. | `SOURCE_CONFIRMED`; runtime `NOT_ASSESSED`. |
| Manual `@store_home` | The Fabric entrypoint creates `AutoDepositRuntime`; the runtime registers trusted-container commands and `StoreHomeCommandRegistrar`. | Relevant test source exists; no test or build was run for this document revision. | Companion documents contain historical direct-command and Korean-chat runtime records. This review did not reproduce them. Microphone parity remains unproven by those records. | `SOURCE_CONFIRMED`, `TEST_SOURCE_PRESENT`, and separately labeled `HISTORICAL_RUNTIME_EVIDENCE`. |
| Automatic inventory-pressure deposit | `DepositAllInventoryPressureChain` exists in source, but no production construction or registration path was found in this snapshot. The entrypoint explicitly keeps the pressure chain disabled. | Registration/lifecycle test source exists; no test was run for this document revision. | Not reproduced and not expected to be active. | `SOURCE_CONFIRMED_DORMANT`. Do not silently re-enable it. |
| `AutoDepositTrustedStoreTask` | Source exists. The only production reference found outside the class is through `AutoDepositMaintenanceTask`, which is referenced by the disabled pressure chain. | No direct test execution was performed for this document revision. | No active production entry path was established by this review. | `SOURCE_CONFIRMED_DORMANT` implementation detail while the pressure chain remains disabled. |

Do not collapse these rows into the statement `deposit/store is verified`. The
names are similar, but their activation paths and evidence are different.

The user has separately reopened the question of restoring the independent
automatic inventory-pressure subsystem. The current-source cause and the
forward-only restoration gate are recorded in section 28 of
[ChatClef @deposit_all Ocean Loop Diagnostics Plan](chatclef-deposit-all-ocean-loop-diagnostics-plan-2026-08-26.md).
That documentation does not change this backlog's current activation matrix:
the pressure chain remains dormant in the currently inspected source. If a
separately authorized source change composes it, classify that snapshot as
`SOURCE_CONFIRMED_ACTIVE` but test/build/runtime-unproven; do not continue calling it
dormant, and do not call it restored until current tests/build, deployment
identity, and runtime proof are completed.
Manual `STORE_HOME` remains explicit-request-only and must not become the
automatic pressure Task.

### Later dirty-worktree activation note - 2026-08-30

The `SOURCE_CONFIRMED_DORMANT` row above is intentionally scoped to the
pre-restoration source snapshot inspected for the original backlog revision.
It is not the classification of the later dirty-worktree composition recorded
in section 28.14 of
[ChatClef @deposit_all Ocean Loop Diagnostics Plan](chatclef-deposit-all-ocean-loop-diagnostics-plan-2026-08-26.md).
That later snapshot is separately classified as follows:

```text
SOURCE_CONFIRMED_ACTIVE: YES
TEST_SOURCE_PRESENT: YES
TEST_PASSED_CURRENT: YES, within the exact commands and limitations recorded there
CLEAN FORCED BUILD FOR THAT RESTORATION RECORD: NOT ESTABLISHED
AUTOMATIC RUNTIME ACTIVITY OBSERVED IN THE 2026-08-30 CAPTURE: LOG_CONFIRMED
CURRENT SOURCE-TO-ARTIFACT RUNTIME PATH PROOF: UNPROVEN
FINAL INCIDENT ROOT CAUSE: UNPROVEN
```

The focused evidence reconciliation and next bounded diagnostics-only contract
are recorded in
[ChatClef Automatic Deposit Transfer / Movement / Carry On Diagnostics Review](chatclef-auto-deposit-transfer-movement-carryon-diagnostics-review-2026-08-30.md).
The detailed Slice A identity, observation-field, lifecycle, stop-gate, and
`16 grouped scenarios / 24 contract assertions` contract is recorded separately
in
[ChatClef Automatic Deposit Slice A Diagnostics Contract](chatclef-auto-deposit-slice-a-diagnostics-contract-2026-08-30.md).
This status note does not authorize activation, diagnostics implementation,
behavior changes, tests, build, deployment, runtime reproduction, commit, or
push through the refactoring backlog.

## Independent archive-review reconciliation - 2026-08-29

The independent v2 review was performed against an archive without `.git` metadata and without
the later live dirty-worktree diagnostics extraction recorded under `Implementation evidence`
below. Its architecture and evidence-label corrections are useful, but the reviewed copy must
not replace this live document wholesale or delete evidence that the archive could not inspect.

The automatic-pressure and diagnostics-refactor workstreams remain separate:

```text
automatic pressure restoration
    canonical owner: ocean-loop diagnostics plan section 28
    scope: composition, registration, tick order, StoreHome conflict and lifecycle proof

diagnostics refactoring
    canonical owner: this backlog
    scope: behavior-preserving extraction behind stable facades
```

This backlog does not authorize automatic-pressure activation. Conversely, a separately
authorized pressure restoration must not be used to split `DepositAllInventoryPressureChain`,
move its package, add a manager hierarchy, refactor `StoreDepositDiagnostics`, or rewrite
upstream-facing call sites. Do not use a diagnostics refactor as an indirect activation path.

For the archive snapshot inspected by that independent review, the classification remains
`SOURCE_CONFIRMED_DORMANT` because production construction and tick were absent there. The later
live dirty-worktree snapshot is governed by the dated note above and is
`SOURCE_CONFIRMED_ACTIVE`. `TEST_PASSED_CURRENT`, `BUILD_PASSED_CURRENT`, deployed-artifact
identity and `RUNTIME_PATH_PROVEN_CURRENT` remain independent gates and must use only the evidence
recorded for the same source snapshot.

## Line-count method

The observed sizes below are pre-extraction nonblank physical source lines in snapshot
`14ba9b443f0bc11d6860a25d7fd3b8b916d95a04`. They are not total file line counts, and the
later live dirty-worktree extraction can have different current counts. Line count is a triage
signal only; it is not proof that a class should be split.

## Candidate triage

### Tier A - no-growth boundaries and evidence-gated extraction candidates

| Order | Class | Nonblank lines | Static observation | Decision |
| --- | --- | ---: | --- | --- |
| A1 | `lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics` | 1138 | The name appears in 18 production files in the snapshot, including 10 files under `adris/**`. Lifecycle, interaction, route, transfer, effect, and terminal emission remain concentrated in the facade. | Highest-priority no-growth boundary. Keep the public static facade and extract only one proven event family at a time after characterization tests exist. |
| A2 | `lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics` | 972 | Operation state, candidate lifecycle, progress, timeout decisions, activation, rejection, and terminal emission are coordinated here, while several helper subpackages already exist. | No new responsibility. Characterize event order, state ownership, and timeout decisions before extracting a narrow emitter or pure helper. |
| A3 | `lavi.minecraft.diagnostics.mining.baritone.BaritonePathCalculationDiagnostics` | 809 | Static concurrent registries, thread-local correlation, generation identity, path-build observation, post-processing observation, adoption-decision observation, and cancellation observation are coupled. Four production callers are diagnostic mixins under `adris/**`. | Preserve all public static signatures and upstream mixin call sites. Split only after correlation/cleanup tests and, when authorized, a bounded runtime comparison. |

### Tier B - conditional candidates, not line-count refactors

| Order | Class | Nonblank lines | Risk or existing structure | Decision |
| --- | --- | ---: | --- | --- |
| B1 | `lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields` | 583 | It has one coherent purpose: building ordered flat event fields. Direct contract coverage for field order and omission behavior was not established by this review. | Split only when an active event-family change makes review noise materially worse, and only after ordered field-contract tests exist. |
| B2 | `lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState` | 461 | Cross-field invariants are protected by one `synchronized` owner. Branch epochs, route-child identity, active attempts, checkpoints, and counters move together. | Do not split into independent mutable state owners. First extract immutable snapshots, pure formatters, or value objects while retaining one lock and one aggregate owner. |
| B3 | `lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator` | 455 | The package already contains an outcome classifier, root-ownership classifier, user-task-finished observer, preexisting-idle stability gate, quiescence tracker, result outbox, and evidence publisher. | Refactor only for a concrete change. Do not create duplicate observer, quiescence, completion, or outbox layers merely to reduce file length. |

### Tier C - stable facades or dormant code; leave alone by default

| Class | Nonblank lines | Reason to leave stable or frozen |
| --- | ---: | --- |
| `lavi.minecraft.diagnostics.container.home.StoreHomeManifestStaleDiagnostics` | 408 | It is already a bounded one-shot coordinator with injected context/event collaborators, snapshot assemblers, a private baseline record, a payload cap, and direct tests. The earlier proposed classes largely duplicate existing concepts. |
| `lavi.minecraft.diagnostics.ChatClefDiagnostics` | 386 | It is already a compatibility facade over existing mode, trace, task registry, event emitter, command-context, formatting, container, and block-interaction delegates. Its name appears across 142 production files in the snapshot. Do not add another layer of `*Facade` wrappers or churn call sites. |
| `lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureChain` | 374 | Dormant in the inspected production registration graph. Keep it outside this refactor backlog until a separately authorized source restoration requires changes and obtains its own evidence. |
| `lavi.minecraft.task.container.deposit.auto.trusted.execution.AutoDepositTrustedStoreTask` | 324 | No active production entry path was established outside the dormant automatic chain. Freeze it with that subsystem unless a separately approved task reopens automatic trusted storage. |

## Approved direction for Tier A candidates

The following are architectural seams to investigate, not pre-approved class
names or an instruction to create every listed package.

### 1. `StoreDepositDiagnostics`

Keep `StoreDepositDiagnostics` as the compatibility-preserving public static
facade. Existing public call sites, especially those in `adris/**`, must not
move or change merely because implementation is extracted.

Potential seams, one at a time:

```text
operation and root lifecycle
interaction binding and observation
candidate route, filtered search, and pursuit observation
transfer decision and effect observation
terminal summary assembly and emission
```

Before the first extraction, add or establish characterization coverage for:

```text
public static method signatures
event names
ordered payload keys and values
field omission and unavailable-value behavior
emission gates, detail budgets, and terminal reservations
OFF / BOUNDARY / VERBOSE behavior
operation identity and command-context attachment
terminal exactly-once or grouped-emission behavior where applicable
```

Reuse existing collaborators and vocabulary. Do not create a new type that
merely renames or wraps any of the following existing concepts without proving
a distinct responsibility:

```text
StoreDepositInteractionContext
StoreDepositInteractionBindingRegistry
StoreDepositEventFields
StoreDepositEmissionGate
StoreDepositTerminalReservation
StoreDepositOperationContext
StoreDepositOperationState
StoreContainerRouteState
```

The first implementation change should extract one event family only. A large
package tree created in one commit would make equivalence harder to prove and is
not authorized by this backlog.

The automatic-deposit Slice A contract does not authorize a facade-wide
refactor. If a later diagnostics-only task needs new transfer, route, or terminal
projection, add only the smallest characterized collaborator behind the existing
facade. Keep `ROOT_ANY_CONTAINER` and `TARGET_CONTAINER` payload terminology,
local-versus-durable mutation meaning, and gameplay-versus-coverage lifecycle
separation exactly as specified in the Slice A contract.

Preservation constraints:

```text
public static signatures: preserve
event names: preserve
ordered payload fields and value semantics: preserve
emission gates, budgets, and reservation order: preserve
diagnostic mode behavior: preserve
Task return values and lifecycle behavior: do not change
container click, cursor, Baritone, and TaskRunner behavior: do not change
upstream-derived call sites: do not edit for facade cleanup
```

### 2. `StoreHomeTimeoutDiagnostics`

Keep one coordinator as the owner of operation state, candidate catalog,
progress lifecycle, operation/candidate tick accounting, decision order, and
terminal ordering. Extracted helpers must be stateless, pure, or operate through
a single explicitly owned aggregate. Do not create competing mutable owners of
candidate or timeout state.

Required characterization boundaries before extraction:

```text
operation-start event and baseline
candidate-start and candidate-progress sequence
candidate timeout versus operation timeout precedence
capacity rejection versus timeout rejection
activation transition
terminal event and evidence assembly
operation and candidate tick ownership
```

Preserve timeout values, policy, decision order, event order, payload semantics,
and all `STORE_HOME` Task behavior.

### 3. `BaritonePathCalculationDiagnostics`

This code observes Baritone behavior; it must not become an owner of Baritone
scheduling, pathing, adoption, cancellation, or timeout policy. Use wording such
as `worker-scheduling observation/correlation` and `adoption-decision
observation`, not wording that implies diagnostics controls those decisions.

Keep a stable static facade for existing mixin call sites. If state is extracted,
exactly one registry must own the concurrent record maps, active calculation
thread-local, generation sequence, capacity bound, and cleanup rules.

Required evidence before and after extraction:

```text
record identity and generation correlation tests
thread-local scope and cleanup tests
path-build and post-process correlation tests
adoption and cancellation observation tests
capacity-bound and stale-record cleanup tests
authorized bounded runtime trace comparison for mixin/concurrency ordering
```

No Baritone goal, path, input, adoption, cancellation, timeout, or Task behavior
may change.

## Guardrails for Tier B candidates

### `StoreDepositEventFields`

Any mechanical extraction must preserve the exact ordered `Object[]` field
sequence, keys, value semantics, null/unavailable representation, and omission
behavior. `Byte-for-byte compatible` is required only where a serialized
renderer or snapshot contract actually exists; otherwise compare the ordered
field representation directly.

### `StoreContainerRouteState`

Retain a single synchronization boundary and aggregate owner. Do not distribute
branch epoch, route-child identity, active store attempt, checkpoint sequence,
and related counters across independently locked mutable collaborators. The
safe first steps are immutable snapshots, pure formatting, or value-object
extraction backed by cross-field atomicity tests.

### `FabricChatClefCommandLifecycleCoordinator`

Do not duplicate these existing responsibilities:

```text
FabricChatClefCommandOutcomeClassifier
FabricChatClefRootOwnershipClassifier
FabricChatClefUserTaskFinishedObserver
FabricChatClefPreexistingIdleRootStabilityGate
FabricChatClefStableRequestQuiescenceTracker
FabricChatClefCommandResultOutbox
FabricChatClefNonterminalLifecycleEvidencePublisher
```

A future extraction needs a concrete residual responsibility and focused tests.
Possible seams must be proven from the current coordinator rather than selected
from an aspirational package diagram.

## Worktree and upstream preservation gate

Before any future source task, record read-only preflight evidence:

```text
git status --short
git branch --show-current
git rev-parse HEAD
git rev-parse --abbrev-ref --symbolic-full-name @{upstream}
git diff --name-status
git diff --cached --name-status
```

If an upstream is absent or a command fails, report that exact result instead of
inventing a state. Do not run `reset`, `clean`, `stash`, `checkout`, mass format,
or broad generated-file cleanup. Do not overwrite or absorb unrelated user
changes. Restrict edits to the separately approved file set.

For every affected source file:

1. classify provenance;
2. explain why the change belongs at that boundary;
3. preserve upstream-derived files unless the smallest diagnostic-only hunk is
   separately justified and authorized;
4. keep public facades stable when upstream-derived callers depend on them.

## Verification gates for a future refactor

A future refactor is complete only when its report contains three separate
sections.

### A. Source equivalence

```text
exact files changed
public signatures before and after
event-name inventory
ordered payload-field comparison
state owner and synchronization comparison
call-site comparison
proof that no production registration or behavior was enabled or disabled
```

### B. Test and build evidence

```text
characterization tests added or identified
focused test command and exact result
targeted Java test command and exact result
authorized clean build command and exact result
artifact path, size, and SHA-256 when deployment or runtime evidence follows
```

A test file merely existing is `TEST_SOURCE_PRESENT`, not
`TEST_PASSED_CURRENT`. A historical build record is not a current build result.

### C. Runtime-path evidence

Runtime reproduction is required when the extraction touches mixin ordering,
thread-local or concurrent correlation, command lifecycle, registration, Task
identity, timeout order, terminal emission, or user-visible routing.

Record separately:

```text
entry path: direct command, chat, microphone, or other
exact command or utterance
source HEAD and deployed artifact identity
operation/request identity
expected event sequence
observed event sequence and counts
terminal status and reason
payload or digest comparison where relevant
```

Proof for one entry path must not be generalized to another.

## Suggested implementation order

1. Make no Java change as part of this backlog update.
2. Preserve the live worktree and record the read-only preflight evidence.
3. For `StoreDepositDiagnostics`, inventory signatures, event names, ordered
   payload fields, budgets, and existing tests.
4. Add the smallest missing characterization tests.
5. Extract one event family behind the unchanged facade.
6. Run only the focused tests first, then the separately authorized targeted
   suite and build.
7. Reproduce only the affected runtime path when authorization and artifact
   identity are available.
8. Consider `StoreHomeTimeoutDiagnostics` as a separate task after the first
   extraction is proven behavior-preserving.
9. Consider `BaritonePathCalculationDiagnostics` only with explicit
   concurrency/mixin evidence and runtime authorization.
10. Leave Tier B classes unchanged unless a concrete active change justifies an
    extraction.
11. Do not refactor or re-enable the dormant automatic pressure-deposit subsystem through this
    backlog. Any separately authorized restoration is governed by the ocean-loop plan §28 and
    remains a distinct change unit.

## Non-goals

This backlog does not propose or authorize:

```text
STORE_HOME behavior changes
automatic deposit re-enablement through this diagnostics-refactoring backlog
manual @deposit_all or @deposit behavior changes
TaskRunner changes
Baritone goal, path, input, adoption, cancellation, or timeout changes
InteractWithBlockTask changes
StoreInContainerTask transfer-semantics changes
wire-protocol changes
diagnostic mode default changes
overlay or HUD coupling changes
upstream call-site migration
broad folderization or mass renaming
build, test, runtime reproduction, deployment, commit, or push
```

## Implementation evidence - 2026-08-29

After the documentation-only revision above, the user separately authorized a
source refactor and folderization task. The implementation was performed as a
sequence of characterization-test, extraction, and focused-test steps in one
uncommitted worktree. This is not a clean-build or runtime-verification record.

### A. Source equivalence

`StoreDepositDiagnostics` remains the compatibility facade. Its exact set of
30 public static method signatures is fixed by
`StoreDepositDiagnosticsFacadeContractTest`.

The facade owns one shared `StoreDepositBindingRegistry` and one shared
`StoreDepositEmissionGate` and passes those exact instances to the extracted
collaborators. `StoreContainerRouteState` remains the single synchronized owner
of candidate-route state. No `adris/**` or `baritone/**` caller was edited.

The following LAVI-owned implementation responsibilities were extracted:

```text
terminal/
  StoreDepositTerminalSummaryEmitter

interaction/
  StoreDepositInteractionDiagnostics

lifecycle/
  StoreDepositOperationRegistrationDiagnostics
  StoreDepositRootLifecycleDiagnostics
  StoreDepositTrackerBindingDiagnostics
  StoreDepositTaskLifecycleDiagnostics
  StoreDepositChildReconciliationDiagnostics

transfer/
  StoreDepositTransferDiagnostics

effect/
  StoreDepositEffectDiagnostics

route/
  StoreDepositPursuitDiagnostics
  StoreDepositTargetCallbackDiagnostics
  StoreDepositContainerRouteEventDiagnostics
```

The pre-extraction and post-extraction diagnostic-identifier inventories are identical. This is
not a claim that all 17 values are emitted event names: `STORE_IN_ANY_CONTAINER_TASK` is a
root-task-kind value, while the other 16 entries are event names.

```text
STORE_BARITONE_OPERATION_SUMMARY
STORE_CONTAINER_EFFECT_OBSERVATION
STORE_CONTAINER_FILTERED_SEARCH_RESULT
STORE_CONTAINER_PARENT_CANDIDATE_DECISION
STORE_CONTAINER_PURSUIT_DECISION
STORE_CONTAINER_TARGET_CALLBACK_DECISION
STORE_CONTAINER_TRANSFER_DECISION
STORE_CRAFT_ROUTE_EVALUATION_ENTERED
STORE_DEPOSIT_CHECKPOINT_SUMMARY
STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY
STORE_DEPOSIT_EFFECT_SUMMARY
STORE_DEPOSIT_TERMINAL_GROUP_RESERVE_EXHAUSTED
STORE_DEPOSIT_TERMINAL_SUMMARY
STORE_IN_ANY_CONTAINER_TASK
STORE_TASK_CHILD_RECONCILIATION
STORE_TASK_LIFECYCLE_BOUNDARY
USER_BLOCK_RANGE_NULL_INPUT_OBSERVED
```

Direct and facade-level contracts cover terminal grouped-emission order,
interaction operation/attempt identity, lifecycle-to-terminal order, child
handoff state-before-dedupe behavior, transfer state-before-dedupe behavior,
effect snapshot consumption, pursuit/target/route counters, ordered payload
segments, OFF behavior, and shared operation identity.

No command registration, diagnostic default, Task selection, Task completion,
container click, cursor state, timeout, Baritone state, TaskRunner state,
`InteractWithBlockTask`, manual deposit behavior, STORE_HOME behavior, automatic
pressure-deposit activation, or wire protocol was changed.

The facade is not reported as a fully completed thin facade. Parent-candidate
decision, filtered-search observation, checkpoint emission, and the existing
null-input exception boundary remain in it. Candidate scan collection uses
ThreadLocal begin/observe/end/take semantics coupled to branch epoch and the
single synchronized route aggregate. That cluster remains evidence-gated until
cross-boundary scope-consumption, checkpoint acknowledgement, mismatch, OFF
transition, and sequence/epoch tests exist.

The positive-delta, negative-delta, and item-replacement `ItemStack` effect
branches remain source-equivalence inspected but do not yet have registry-free
direct unit fixtures. They are not promoted to runtime proof.

### B. Test and build evidence

Current focused command:

```powershell
& 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot\bin\java.exe' `
  '-Xmx64m' '-Xms64m' '-Dorg.gradle.appname=gradlew' `
  '-classpath' '.\gradle\wrapper\gradle-wrapper.jar' `
  'org.gradle.wrapper.GradleWrapperMain' `
  ':1.20.1:test' `
  '--tests' 'lavi.minecraft.diagnostics.container.store.deposit.*' `
  '--no-daemon'
```

Observed result:

```text
BUILD SUCCESSFUL in 41s
46 actionable tasks: 8 executed, 38 up-to-date
tests: 54
failures: 0
errors: 0
skipped: 0
classification: TEST_PASSED_CURRENT
```

This Gradle test-task success is not `BUILD_PASSED_CURRENT`. The required clean
forced command `./gradlew.bat clean build --rerun-tasks` was not run, no JAR was
copied, and no artifact size or SHA-256 was recorded.

### C. Runtime-path evidence

No Minecraft launch, direct command, chat, microphone, Mixin-ordering trace, or
deployed-artifact comparison was performed for this refactor. Therefore:

```text
RUNTIME_PATH_PROVEN_CURRENT: NO
artifact parity: NOT ASSESSED
runtime behavior equivalence: NOT PROVEN BY THIS TASK
```

## Status

```text
reviewed snapshot: 14ba9b443f0bc11d6860a25d7fd3b8b916d95a04
live repository HEAD at implementation evidence update: 14ba9b443f0bc11d6860a25d7fd3b8b916d95a04
document status: IMPLEMENTATION_EVIDENCE_APPENDED
source changes: YES - LAVI-owned diagnostics structural extraction
public static facade signatures: 30, exact-set contract PASS
diagnostic identifier inventory: 17 (16 event names + 1 root-task-kind), pre/post sets identical
behavior changes: NONE IDENTIFIED
focused test status: TEST_PASSED_CURRENT, 0 failures
clean forced build: NOT RUN
runtime verification: NOT RUN
artifact identity: NOT RECORDED
commit or push: NOT PERFORMED
```
