<!-- 20260907_kpopmodder: Recorded and implemented the responsibility and folderization plan for the requested Store diagnostics targets. -->
<!-- 20260907_kpopmodder: Applied characterization-first test splitting, stable facades, and one mutable lifecycle owner. -->
<!-- 20260907_kpopmodder: Reconciled focused/full tests, the clean forced build, and the separately unverified deployment/runtime boundary. -->
<!-- 20260907_kpopmodder: Qualified mixed-worktree verification, Java visibility, observation purity, and rollback evidence after source/test review. -->
<!-- 20260908_kpopmodder: Refreshed the final integrated test/artifact evidence and recorded the independent source rollback commits. -->

# ChatClef Targeted Diagnostics Refactoring and Folderization Implementation Record

Date: 2026-09-07

## 1. Document status

This document preserves the requested pre-change plan and records the completed
repository refactor. It identifies the source-proven candidates, exact
responsibilities, destination packages, compatibility boundaries,
implementation order, tests, and rollback units.

```text
REVIEWED_REPOSITORY_ROOT: C:\Vtuber_Souorce_Code\LAVI
REVIEWED_BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
REVIEWED_HEAD: c5968582a826e7fb10b857e7c50388c5e3ec93d2
REVIEWED_WORKTREE_STATE: DIRTY_EXISTING_CHANGES_PRESERVED
PRECHANGE_HASH_BASIS: PRE_REFACTOR_WORKTREE_SNAPSHOT_NOT_HEAD_BLOB
STATUS: IMPLEMENTED_VERIFIED_OFFLINE_ON_MIXED_WORKTREE

BACKEND_SCOPE: FABRIC_CHATCLEF_ONLY
FORGE_MINEMIND_SCOPE: NOT_APPROVED_NOT_CREATED
PRODUCTION_SOURCE_CHANGE_IN_THIS_TASK: APPLIED_LAVI_OWNED_RESPONSIBILITY_SPLITS
TEST_SOURCE_CHANGE_IN_THIS_TASK: APPLIED_CHARACTERIZATION_AND_FOCUSED_TESTS
FILE_MOVE_OR_DELETE_IN_THIS_TASK: ORIGINAL_UMBRELLA_TEST_DELETED_AFTER_EXACT_MIGRATION_LEDGER
CHANGE_INVENTORY: 38_JAVA_PATHS_9_PRODUCTION_29_TEST
PRODUCTION_INVENTORY: 7_NEW_2_MODIFIED
TEST_INVENTORY: 24_NEW_4_MODIFIED_1_DELETED
NEW_DIAGNOSTICS_TEST_METHODS: 56
MODIFIED_EXISTING_R3_TEST_METHODS: 8_BEFORE_10_AFTER
FOCUSED_JAVA_TESTS: PASS_373_FAILURES_0_ERRORS_0_SKIPPED_1_MIXED_6_FILTER_SET
FULL_JAVA_1_20_1_TESTS: PASS_843_FAILURES_0_ERRORS_0_SKIPPED_1
BUILD_IN_THIS_TASK: CLEAN_FORCED_BUILD_PASSED_171_TASKS_EXECUTED
BUILD_PROVENANCE: MIXED_DIRTY_WORKTREE_INTEGRATION_EVIDENCE
ISOLATED_DIAGNOSTICS_ONLY_BUILD: NOT_RUN
DEPLOYMENT_IN_THIS_TASK: NOT_RUN
MINECRAFT_RUNTIME_IN_THIS_TASK: NOT_RUN
SOURCE_ROLLBACK_COMMITS: R1_331D68E4_R2_1086B490_R3_4742D0A0
PUSH_AT_VERIFICATION_SNAPSHOT: NOT_YET_RUN
```

R1 first recorded the original umbrella test's exact SHA-256 in this document,
then recorded all 17 test methods and the historical 16 scenarios/24 assertions
in an executable migration ledger. Only then was
`StoreDepositSliceADiagnosticsContractTest.java` removed and its coverage split
across responsibility packages for transfer, mutation, binding/effect,
lifecycle/route, Carry On, automatic terminal accounting, budget/context, and
post-place handoff.

R2 retains `StoreDepositAutomaticLifecycleLedger` as the public facade and the
only synchronized boundary. The new `terminal/automatic/` package separates
the facade port, identity/LRU registry, per-run state, terminal coverage, and
pure completeness evaluator while preserving the nested API, caps, counters,
normalization, touch/eviction order, and diagnostics-OFF clear semantics.

R3 retains `StoreHomeTimeoutDiagnostics` as the public strict-OFF facade. One
`StoreHomeTimeoutLifecycleCoordinator` owns mutable transition order, and the
side-effect-free `StoreHomeCandidateObservationCollector` reads live snapshots
and assembles fingerprints/evidence without owning lifecycle mutation. Existing
emitters and the source-inspected exception boundaries remain unchanged; final
matching-candidate cleanup follows terminal/rejection emission. No second lock
or mutable coordinator was added.

The final required JDK 21 command from the runtime root was:

```powershell
.\gradlew.bat clean build --rerun-tasks
```

It completed successfully with 171 executed tasks. The final integrated 1.20.1
test XML contains 843 tests, zero failures, zero errors, and one skip. The
remapped artifact is
`versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar` with SHA-256
`BD051BEC090D299C086EE9A65CD3EEE52605F5C49DB5E63E287D54C3A34E150B`.
Deployment, Minecraft launch, and live-world verification remain `NOT_RUN`.

This build and artifact came from the complete dirty worktree. They therefore
prove that the diagnostics refactor and the separately owned Java
natural-response GET-effect/result-projection slice compile and regress in the
same Gradle source set; they are not an isolated diagnostics-only build or an
instruction to combine the two source/rollback units. This Gradle result does
not itself verify the natural-response Python paths. The 373-test focused
result likewise includes the 29 Java tests owned by that natural-response slice
in addition to the diagnostics, deposit-task, and Carry On filters.

The natural Korean crafting response requested at the same time is separately
owned by
[Natural Korean Crafting Lifecycle Feedback Contract](chatclef-natural-korean-crafting-lifecycle-feedback-pre-change-contract-2026-09-07.md).
The two source changes must not be combined into one implementation or rollback
unit. Their shared clean build is cross-unit integration evidence only.

## 2. Authority and historical context

This is a current-source addendum to
[Diagnostics Refactoring Backlog](chatclef-diagnostics-refactoring-backlog-2026-08-29.md).
It does not rewrite the backlog's historical snapshot. In particular, the old
`StoreHomeTimeoutDiagnostics` line count predates a partial extraction already
present in the reviewed source.

The following rules remain authoritative:

- [Minecraft Backend Separation](minecraft-backend-separation.md)
- [ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)
- [Auto-Deposit Slice A Diagnostics Contract](chatclef-auto-deposit-slice-a-diagnostics-contract-2026-08-30.md#8-direct-characterization-test-contract)
- [Automatic Deposit Post-Checkpoint Direction](chatclef-automatic-deposit-post-checkpoint-direction-2026-08-31.md#8-bounded-terminal-accounting)
- [Fabric ChatClef Build Verification](chatclef-fabric-build-verification.md)

All target files are LAVI-owned code in the Fabric ChatClef runtime tree. No
direct `adris/**` consumer was found for either production target. The upstream
engine nevertheless remains a stable compatibility boundary: this plan does
not move, split, or edit upstream-derived classes.

## 3. Normalized target list and pre-change evidence

The request named `StoreDepositAutomaticLifecycleLedger` twice. Both entries
refer to the same class and are normalized to one high-priority target; the
duplicate does not authorize two implementations or a replacement ledger.

Line counts below are the physical pre-change source counts. Hashes make the
reviewed starting point exact. They identify the pre-refactor dirty-worktree
files captured for this task; they are not asserted to be the byte hashes of
the `REVIEWED_HEAD` Git blobs. In particular, unrelated pre-existing worktree
changes were preserved rather than reset to reconstruct a clean-HEAD baseline.

| Priority | Target | Pre-change size | SHA-256 | Classification |
| --- | --- | ---: | --- | --- |
| 1 | `src/test/java/lavi/minecraft/diagnostics/container/store/deposit/StoreDepositSliceADiagnosticsContractTest.java` | 1,639 total / 1,539 nonblank | `462AB1AB3F622421A9DAB0FE8C282CFB7234CE5BD08FFC692B96821FF31F7362` | Mandatory characterization-test split |
| 2 | `src/main/java/lavi/minecraft/diagnostics/container/store/deposit/terminal/StoreDepositAutomaticLifecycleLedger.java` | 974 total / 922 nonblank | `AD5C4A4A38C490A250C91DC225545D375E07B169EFCF25740FD8C678216A68A6` | Mandatory production responsibility split |
| 3 | `src/main/java/lavi/minecraft/diagnostics/container/home/timeout/StoreHomeTimeoutDiagnostics.java` | 851 total / 822 nonblank | `00B83AF768511ECB4AE40128E11F705C5AA462F6586092E35F770371910E626E` | Follow-up after partial extraction |

The Java paths in the table are relative to:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/
```

The pre-change evidence label for all three was `SOURCE_CONFIRMED`. The status
ledger in Section 1 now records the completed source, test, build, and artifact
evidence; deployment and runtime remain separate and unrun.

## 4. Decision and implementation order

The implementation followed this mandatory work order:

```text
R1  split and strengthen StoreDepositSliceADiagnosticsContractTest
R2  refactor StoreDepositAutomaticLifecycleLedger behind its stable facade
R3  refactor residual StoreHomeTimeoutDiagnostics coordination separately
R4  focused tests, full applicable tests, required clean forced build
R5  bounded same-artifact runtime comparison only when separately authorized
```

R1 was first because the pre-change 1,639-line umbrella test contained most of the
only direct ledger characterization. Splitting production code first would
make it difficult to distinguish behavior drift from test relocation errors.

R2 and R3 are independent. Do not introduce a shared lifecycle manager, common
mutable superclass, shared lock, or generic diagnostics framework merely
because both classes have lifecycle state.

## 5. R1 - split `StoreDepositSliceADiagnosticsContractTest`

### 5.1 Why it must split

At the pre-change baseline, the class combined transfer arithmetic, slot mutation, tracker
binding, route ordering, invalidation, interaction attribution, optional Carry
On evidence, automatic lifecycle accounting, boundedness, context isolation,
and source-layout assertions. These are independent reasons to change and map
to existing production packages.

The original Slice A contract describes 16 grouped scenarios and 24 assertions.
The pre-change umbrella test also contained a later post-place handoff source contract. All
of them must have a one-to-one migration record before the umbrella file is
removed.

### 5.2 Exact method-to-destination map

| Existing test method | Destination test class |
| --- | --- |
| `aggregateTargetAndSelectedPhysicalSourceHaveSeparateIdentityAndCount` | `deposit/transfer/StoreDepositTransferSelectionContractTest.java` |
| `partialPhysicalTransferKeepsLocalArithmeticSeparateFromDurability` | `deposit/transfer/StoreDepositTransferSelectionContractTest.java` |
| `strictExactFitRejectsEqualityWhileDestinationIdentityParticipatesInChildEquality` | `deposit/transfer/StoreDepositTransferSelectionContractTest.java` |
| `oneSlotActionOwnsOrderedMutationIdentities` | `deposit/transfer/StoreDepositSlotMutationContractTest.java` |
| `bothTrackerRolesShareMutationIdentityAndLocalMutationLeavesDurabilityUnavailable` | `deposit/transfer/StoreDepositSlotMutationContractTest.java` |
| `trackerResubscriptionAdvancesGenerationWithoutLosingActiveState` | `deposit/binding/StoreDepositBindingLifecycleContractTest.java` |
| `signedDeltasCoverPositiveNegativeReplacementAndTrackerPredicateAsymmetry` | `deposit/effect/StoreDepositEffectProjectionContractTest.java` |
| `notStoredProjectionPreservesFullPartialAndZeroAvailableSemantics` | `deposit/transfer/StoreDepositTransferSelectionContractTest.java` |
| `availableCountCompositionUsesExistingCombinedValueWithoutDiagnosticRescan` | `deposit/transfer/StoreDepositTransferSelectionContractTest.java` |
| `parentBeforeChildTickOrderingRemainsUnchanged` | Mandatory split across `deposit/lifecycle/StoreDepositTaskLifecycleOrderingContractTest.java`, `task/container/deposit/auto/maintenance/AutoDepositMaintenanceChildRegistrationOrderTest.java`, and `task/container/deposit/auto/pressure/AutoDepositPressureChainSourceContractTest.java` |
| `checkFalseInvalidationDoesNotCloseRouteChildOrReconstructProgressProvenance` | `deposit/route/StoreDepositRouteInvalidationContractTest.java` |
| `headCaptureVerdictAndObservationLookupVerdictRemainTypedAndIndependent` | `deposit/interaction/StoreDepositInteractionDiagnosticFieldsTest.java` |
| `carryOnTemporalEdgeWithRetainedTargetAndUnavailableIdentityIsNotExactAttribution` | `integration/carryon/container/CarryOnContainerPickupEvidenceTest.java` |
| `automaticLifecycleScopesKeepSeparateExactCountsAndHandoffFlags` | `deposit/terminal/automatic/StoreDepositAutomaticLifecycleLedgerContractTest.java` and `StoreDepositAutomaticCoverageGapIntegrationTest.java` |
| `diagnosticsModesAndAllBoundednessControlsRemainExplicit` | Split between `deposit/budget/StoreDepositDiagnosticsBoundednessContractTest.java` and `deposit/terminal/automatic/StoreDepositAutomaticLifecycleLedgerBoundednessTest.java` |
| `sharedObserversNoOpWithoutAutomaticContextAndManualSourcesStayManual` | `deposit/lifecycle/StoreDepositAutomaticContextIsolationContractTest.java` |
| `automaticPostPlaceHandoffIsScopedToTheGeneralMaintenanceFactory` | `task/container/deposit/handoff/DepositAllPostPlaceHandoffSourceContractTest.java` |

The automatic lifecycle method is itself a suite. Its exact-count/dedupe,
same-user-root multi-run ordering, missing per-item close, wrong-identity,
unavailable-expected-identity, and coverage-gap assertions must use focused test
methods rather than remain one replacement mega-method.

The parent-before-child method must be divided: generic parent/reconciliation/
child tick ordering stays in the diagnostics lifecycle test, maintenance-child
registration moves to its maintenance owner, and pressure-chain/source-layout
assertions move to the pressure owner. The original display name and assertion
meaning remain traceable in the migration ledger.

The modes/boundedness method must also be divided. General diagnostic mode,
family/session/payload budget, critical reserve, and generic suppression cases
belong to the budget test. Terminal suppression, the 16-run access-order LRU
victim, and post-removal eviction suppression belong to the automatic-ledger
boundedness test and must exist before R2 begins.

### 5.3 Test fixture rules

- Preserve the current per-test diagnostic session reset performed by
  `@BeforeEach` and `@AfterEach`.
- Keep source-string assertions during the first mechanical split. Modernizing
  them is a later independent task.
- Move the Carry On test into its owning package so it can use a narrow
  package-visible fixture instead of reflection. Do not expose production
  internals publicly for that test.
- Prefer composition and small package-local fixtures. Do not create a shared
  base test class; there is no stable `is-a` relationship.
- Extract a shared helper only when at least two destination classes actually
  use it. Do not replace one 1,639-line test with one giant utility.
- Isolate the current `System.out` capture. Class splitting must not create
  parallel-test interference through process-global output replacement.
- Do not edit upstream source merely to keep a textual source assertion green.

### 5.4 R1 completion gate

The original file may be removed only after explicit confirmation of its exact
absolute path and after all 17 pre-change test methods, the historical 16-group /
24-assertion mapping, lifecycle reset hooks, and helper-dependent assertions
appear in their new owners. Removal is a separately reviewable final R1 hunk,
not the first step.

## 6. R2 - split `StoreDepositAutomaticLifecycleLedger`

### 6.1 Source-proven responsibilities

The current class owns all of the following:

| Pre-change region | Responsibility |
| --- | --- |
| Lines 18-32 | Capacity limits and terminal-scope vocabulary |
| Lines 34-120 | Active-run/LRU registries, Task identity bindings, run/context creation and lookup |
| Lines 122-195 and 454-499 | Expected/observed terminal identity accounting, dedupe, gaps, and overflow |
| Lines 197-375 | Maintenance close, pressure-run close, user-task resume/completion, and handoff transitions |
| Lines 377-452 | Snapshot, pending eviction, purge, suppression, and diagnostics-OFF clearing |
| Lines 501-556 | Capacity eviction, removal, LRU touch, and user-root run traversal |
| Lines 561-656 | Per-run mutable state and snapshot construction |
| Lines 658-923 | Public snapshot data plus lifecycle/identity completeness evaluation and string projection |
| Lines 925-973 | Public nested `TerminalRecord` and `ClearResult` contracts |

This is more than one independent responsibility and therefore is a mandatory
split candidate. The split is structural only: it must not change diagnostic
events, lifecycle behavior, caps, ordering, task state, input, pathing, or
terminal decisions.

### 6.2 Implemented structure

Keep this stable public boundary:

```text
lavi/minecraft/diagnostics/container/store/deposit/terminal/
  StoreDepositAutomaticLifecycleLedger.java
```

Add one cohesive implementation package:

```text
lavi/minecraft/diagnostics/container/store/deposit/terminal/automatic/
  StoreDepositAutomaticLedgerComponent.java
  StoreDepositAutomaticRunRegistry.java
  StoreDepositAutomaticRunState.java
  StoreDepositAutomaticTerminalCoverage.java
  StoreDepositAutomaticCoverageEvaluator.java
```

Responsibilities are exact:

| Type | Sole responsibility |
| --- | --- |
| Existing `StoreDepositAutomaticLifecycleLedger` | Stable synchronized facade and atomic sequencing of the existing public API |
| `StoreDepositAutomaticLedgerComponent` | Sole public facade-to-component port; sequences package-private automatic-ledger collaborators while assuming the facade lock is held |
| `StoreDepositAutomaticRunRegistry` | Identity-based Task bindings, access-order run registry, same-user-root ordering, pending eviction records, and removal |
| `StoreDepositAutomaticRunState` | One automatic run's local context, child ordinal, and lifecycle flags |
| `StoreDepositAutomaticTerminalCoverage` | One run's bounded expected/observed identities, dedupe, gaps, suppression, and overflow counters |
| `StoreDepositAutomaticCoverageEvaluator` | Pure immutable completeness and missing-boundary projection with no mutable state or logging |

Java parent and subpackages do not share package-private visibility. Therefore
only `StoreDepositAutomaticLedgerComponent` is public in the new subpackage;
the registry, run state, terminal coverage, and evaluator remain package-private
inside that same package. The component returns the facade's existing public
nested result contracts and is held only in a private facade field. A source
boundary test must prove that no production class except the facade imports or
constructs the component.

`public` is required here because Java parent packages and subpackages do not
share package-private access. It is a technical cross-package delegate, not a
language-level guarantee that another class cannot call it: its constructor and
mutating methods are publicly reachable. The supported current production call
graph is facade-only, and the source-boundary test enforces that repository
convention.

Do not create separate lifecycle, terminal, eviction, and coverage locks. The
component and its package-private helpers have no lock of their own. Every
public ledger operation remains atomic under the facade's one synchronization
boundary when invoked through the supported facade. No current production
entry point uses the component as a competing mutation owner; direct component
use would violate the source-boundary contract rather than being prevented by
Java visibility.

The existing public nested `Snapshot`, `TerminalRecord`, and `ClearResult`
types are referenced by current LAVI production/test code. The first refactor
retains their names, fields, return signatures, and semantics in the facade.
Moving them to top-level types is a separate source-breaking migration and is
not silently included in R2. They are permitted as subordinate public result
types of this stable API.

### 6.3 Exact invariants to preserve

- Task keys retain `IdentityHashMap` object-identity semantics.
- Active runs retain access-order `LinkedHashMap` behavior and the current
  `touch()` timing.
- One user root may retain multiple ordered runs; lookup and lifecycle close
  must choose in the same order as current source.
- Existing caps remain exactly `16`, `16`, `1024`, `1024`, `32`, and `256` for
  their current domains.
- Dedupe eviction permanently downgrades completeness rather than fabricating
  exact coverage.
- An active-run eviction terminal record is formed before registry removal.
- Post-removal terminal suppression and its global count remain observable.
- Snapshot construction and all counters are an atomic view of one ledger
  operation.
- `missingLifecycleBoundaries()` preserves exact field vocabulary, order, and
  identity-set equality semantics.
- Diagnostics-OFF clearing preserves its exact active-run, binding, pending
  eviction, and aggregate count result.
- `clearForModeTransition()` clears only the same five current collections. It
  does not reset `nextAutoOperationEpoch`,
  `pendingEvictionOverflowCount`, or
  `activeRunEvictionEmissionSuppressedCount`.
- The current normalization rule `null` or blank to `UNAVAILABLE` is preserved
  for dedupe keys, reasons, and missing-boundary output.
- No new static/global state, unbounded collection, background thread, retry,
  gameplay mutation, or behavior decision is introduced.

### 6.4 Known callers to preserve

Current production callers to inspect and preserve are:

```text
StoreDepositDiagnostics.java
StoreDepositAutomaticLifecycleState.java
StoreDepositAutomaticTerminalDiagnostics.java
session/StoreDepositModeStateInvalidator.java
session/snapshot/StoreDepositSessionStateSnapshotReader.java
```

Pre-change test callers included:

```text
StoreDepositSliceADiagnosticsContractTest.java
StoreDepositSessionLifecycleObserverTest.java
```

The focused ledger contract/boundedness/coverage tests named in R1 are now
present. The umbrella caller was removed only after the executable 17/16/24
migration ledger proved that every required destination token and historical
scenario/assertion marker was present. The ledger does not mechanically prove
semantic equivalence of method bodies; that evidence is supplied by the moved
characterization tests and their passing focused/full executions.

No upstream `adris/**` call site should be changed. Composition and delegation
are required; inheritance offers no valid lifecycle ownership here.

### 6.5 R2 characterization and tests

The extraction's focused characterization tests lock down:

- synchronization and current production-reference boundaries around the
  facade/component seam;
- object-identity versus equality behavior for Task bindings;
- access-order LRU touch and exact eviction victim;
- multiple runs for the same user root;
- each exact bound and overflow counter;
- exact terminal identity count, dedupe eviction, and completeness downgrade;
- eviction-record-before-remove ordering;
- post-removal suppression;
- exact missing-boundary strings and ordering;
- snapshot atomicity under concurrent callers using the one facade lock;
- diagnostics-OFF clearing and session lifecycle behavior;
- mode clear preserves the operation epoch and cumulative overflow/suppression
  counters while clearing exactly the current collection set;
- `null`/blank normalization produces the exact `UNAVAILABLE` key/value.

R2 meets its completion gate: the facade expresses public sequencing rather
than internal accounting, and every current production reference reaches the
mutable component through that facade. Existing caller compilation and source
review confirm that the facade method signatures and nested result records were
preserved, but no dedicated reflection test fixes every parameter, return type,
or nested record component as a standalone API manifest.

## 7. R3 - residual `StoreHomeTimeoutDiagnostics` split

### 7.1 Pre-change classification and current result

This is `FOLLOW_UP_AFTER_PARTIAL_EXTRACTION`, not an untouched monolith. The
current tree already contains dedicated `candidate`, `progress`, `operation`,
`terminal`, `state`, `budget`, `event`, `guard`, and `artifact` helpers. A prior
refactor extracted event-family payload assembly while intentionally leaving
one mutable timeout lifecycle owner.

The old backlog's 972-nonblank-line observation is historical. The reviewed
pre-change class had 822 nonblank lines. The implemented facade is now a narrow
compatibility boundary delegating transition ownership to the coordinator,
without recreating the existing helper packages.

### 7.2 Pre-change residual responsibilities

| Pre-change region | Responsibility |
| --- | --- |
| Lines 61-65 | Three related mutable state components |
| Lines 96-559 | Public compatibility API and strict-OFF gates |
| Lines 182-260 | Progress capture, admission, suppression, and event ordering |
| Lines 264-383 | Operation/candidate timeout decisions and evidence |
| Lines 385-559 | Rejection, activation, terminal sequencing, and cleanup |
| Lines 562-783 | Candidate start/late attach, state projection, fingerprint, and evidence pipeline |
| Lines 786-845 | Operation-start and tick accounting |

### 7.3 Implemented smallest safe structure

The implementation retains the public facade and constructor:

```text
lavi/minecraft/diagnostics/container/home/timeout/
  StoreHomeTimeoutDiagnostics.java
```

It adds only the source-proven residual owners:

```text
lavi/minecraft/diagnostics/container/home/timeout/operation/
  StoreHomeTimeoutLifecycleCoordinator.java

lavi/minecraft/diagnostics/container/home/timeout/progress/
  StoreHomeCandidateObservationCollector.java   # implemented, side-effect-free/read-only
```

The facade retains the strict-OFF check and the same public methods. The single
`StoreHomeTimeoutLifecycleCoordinator` owns the operation-scoped candidate,
progress, timeout, activation, rejection, and terminal state-transition order.
Its sole responsibility is ordered, instance-scoped orchestration of that one
mutable aggregate under the existing caller-thread sequencing. This is not a
claim of an added lock or general concurrent-call atomicity.
It composes the already-existing event-family emitters and state types; it does
not absorb their payload, formatting, budget, or event-emission responsibilities,
and those classes are not duplicated.

The implemented observation collector may only read a player/task snapshot and
assemble an immutable fingerprint/evidence projection. It cannot activate,
reject, clear, time out, emit, or mutate lifecycle state. Because snapshot
capture reads live engine state and may propagate read failures, `read-only`
does not mean referentially pure or exception-free.

Do not create separate mutable operation, candidate, progress, and terminal
coordinators. That would create competing owners and make the existing event
order impossible to reason about. Do not add locks or global state; the current
instance-scoped sequencing remains authoritative.

Because the facade and coordinator are in parent/subpackage Java packages, the
coordinator is the one public cross-package delegate type. Its public
constructor and methods are technically callable; Java visibility does not make
the facade an access-control barrier. In the current production call graph only
the facade constructs and holds it, and a source boundary test rejects any
other production import or construction. The coordinator exposes no
independent lock, singleton, static entry point, background lifecycle, or
mutable-state accessor.

The ownership change is exact:

| Concern | Before R3 | After R3 |
| --- | --- | --- |
| Public constructor/method compatibility and per-call OFF gate | `StoreHomeTimeoutDiagnostics` | `StoreHomeTimeoutDiagnostics` |
| Operation/candidate/progress/timeout transition order and single mutable aggregate | `StoreHomeTimeoutDiagnostics` | `StoreHomeTimeoutLifecycleCoordinator` |
| Event-family payload assembly and emission | Existing operation/candidate/progress/terminal helpers | Same existing helpers, unchanged |
| Player/task snapshot, fingerprint, and evidence assembly | Private facade methods | Implemented side-effect-free/read-only `StoreHomeCandidateObservationCollector` |
| Gameplay, retry, fallback, timeout policy, Task/input/path behavior | Not diagnostics-owned | Still not diagnostics-owned |

The public method families map without changing their signatures:

| Facade method family | Coordinator responsibility after R3 |
| --- | --- |
| `recordOperationStarted`, `recordCandidateCatalog` | Establish operation/catalog state and required predecessor ordering |
| `recordCandidateRejectedBeforeAttempt`, `recordCandidateStarted` | Allocate/remember candidate identity and sequence the existing candidate emitter |
| `recordProgress` | Update the same progress aggregate, apply current admission/dedupe order, and sequence the existing progress emitter |
| `recordOperationTimeoutDecision`, `recordCandidateTimeoutDecision` | Apply current timeout-precedence state transition and sequence existing decision emission |
| `recordCandidateRejected`, `recordCandidateActivated` | Apply the current reject/activate transition and cleanup order |
| `recordTerminal` | Sequence existing terminal emission and then clear the same active-candidate state |

The coordinator owns the three current operation-scoped state values as one
aggregate: operation state, candidate catalog state, and candidate progress
lifecycle. No emitter or observation collector receives write ownership of
that aggregate.

R3 must extract cohesive transitions, not copy all current private methods into
another 800-line class. State mutation remains together because event ordering
is one invariant; observation assembly and event-family payload work remain in
their dedicated owners.

### 7.4 Exact behavior to preserve

- On every public record call, diagnostics OFF returns before entering that
  call's observe, snapshot, tick, counter, emitter, or state-mutation action.
  Constructor/field initialization is not falsely claimed to allocate nothing.
- Operation-start evidence is established exactly once before any dependent
  event.
- Candidate ordinal, attempt identity, late-attach reason, and attach ordering
  remain unchanged.
- A budget-denied progress event retains the current player-only capture and
  suppression sequence.
- Candidate timeout and operation timeout preserve current precedence and tick
  calculations.
- Rejection emits and records evidence before the final cleanup of the matching
  or late-attached candidate. The preserved candidate-mismatch branch may clear
  a stale active candidate before constructing and emitting evidence for the
  requested candidate.
- Terminal emission occurs before active-candidate cleanup in the same current
  order.
- Capacity rejection, activation, candidate mismatch, terminal reason, event
  names, payload fields, field order, and boundedness remain unchanged.
- Existing `StoreHomeDiagnosticBoundary.runIfEnabled` placement and optional
  `StoreHomeDiagnosticBookkeepingGuard.runSafely` placement remain exact.
  Bookkeeping-only faults keep their existing containment, while engine-read
  `RuntimeException`/`LinkageError` propagation must not be swallowed by a
  newly broadened guard.
- The observer remains diagnostics-only and cannot choose recovery, retry,
  fallback, Task completion, input release, or Baritone goal/path behavior.

### 7.5 Stable callers

The current public API must remain source-compatible for:

```text
StoreHomeRequestInitializer
StoreHomeCandidateAttemptStarter
StoreHomeCandidateRejector
StoreHomeSessionActivator
StoreHomeTaskLifecycleController
StoreHomeTimeoutDecisionApplier
StoreHomeOperationTerminator
StoreHomeTaskAssembly
```

No task caller should learn about the new coordinator. The facade constructs or
receives it internally and remains the only task-facing diagnostics boundary.

### 7.6 Existing tests and reflection cleanup

Existing candidate, progress, operation, terminal, and strict-OFF suites were
the characterization base. R3 replaces the relevant private-field structural
coupling with direct coordinator/immutable-observation fixtures. Mutable
production state was not made public and no global test hook was added.

Focused tests must preserve:

- operation-start-before-dependent-event ordering;
- candidate start, late attach, ordinal, and identity;
- progress dedupe and budget-denied suppression;
- candidate versus operation timeout precedence;
- capacity rejection precedence;
- rejection/activation/terminal emission and clear order;
- terminal cleanup;
- strict-OFF zero mutation, including state and emitter counts;
- direct tests for boundary-disabled engine-read suppression and eligible
  engine-read `RuntimeException`/`LinkageError` propagation;
- source-inspected preservation of the bookkeeping-only guard around
  `recordCandidateCatalog`; direct bookkeeping fault injection remains
  `NOT_PROVEN`.

## 8. Folderization rules across R1-R3

- Reuse existing responsibility packages whenever they already exist.
- Create `terminal/automatic/` only for the automatic-ledger component; do
  not use it as a generic dumping ground.
- One public class or interface per new Java file.
- Keep immutable records with their actual owner; do not build a broad `model`,
  `common`, `utils`, `manager`, or `misc` package.
- Keep mutable operation state instance-owned and bounded.
- Diagnostics observation cannot become behavior authority.
- Do not deepen inheritance, add a shared diagnostics base class, or modify
  engine-wide lifecycle classes.
- Move imports and tests in the same atomic unit as a package move. Do not leave
  compatibility aliases unless an actual external reference requires one.
- Preserve unrelated dirty work if the implementation occurs on a later
  worktree.

## 9. Verification matrix and result

The corresponding source units were verified as follows:

| Unit | Minimum evidence |
| --- | --- |
| R1 test split | All destination tests pass; exact original method/assertion migration ledger; no stale umbrella references |
| R2 ledger split | Focused registry, identity, coverage, eviction, snapshot, clear, and concurrency tests; current terminal/session integration tests |
| R3 StoreHome residual split | Existing candidate/progress/operation/terminal/strict-OFF tests plus new coordinator and immutable-observation tests |
| All Java units | `git diff --check`, stale import/path search, targeted `:1.20.1:test` filters, then the canonical clean forced build |

The reported 373-test focused result used this exact six-filter set. Counts
below were reconciled from the final 1.20.1 XML, so the mixed-scope provenance
is explicit rather than being presented as a diagnostics-only run.

| Filter | Tests | Skipped | Ownership note |
| --- | ---: | ---: | --- |
| `lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get.*` | 23 | 0 | Separate natural-response GET-effect slice |
| `lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandResultEffectProjectionTest` | 6 | 0 | Separate natural-response result-projection slice |
| `lavi.minecraft.diagnostics.container.store.deposit.*` | 137 | 0 | R1/R2 diagnostics |
| `lavi.minecraft.integration.carryon.container.CarryOnContainerPickupEvidenceTest` | 1 | 0 | Migrated R1 characterization |
| `lavi.minecraft.task.container.deposit.*` | 183 | 1 | Deposit integration/regression |
| `lavi.minecraft.diagnostics.container.home.timeout.*` | 23 | 0 | R3 diagnostics |
| **Total** | **373** | **1** | Mixed six-filter integration evidence |

The post-change inventory contains 38 Java paths: nine production paths
(seven new, two modified) and 29 test paths (24 new, four modified, one
deleted). The 24 new diagnostics test sources contain 56 `@Test` methods. The
four modified pre-existing R3 test sources contain ten `@Test` methods after
the change, compared with eight at `REVIEWED_HEAD`. These are source inventory
counts, not additional claims about runtime behavior.

The required final build command, run from the exact runtime root, is:

```powershell
.\gradlew.bat clean build --rerun-tasks
```

Incremental Gradle success is not sufficient. Section 1 records the exact
command, test counts, build result, produced artifact path/hash, and separately
labels deployment and runtime. The build is mixed-worktree integration
evidence; an isolated diagnostics-only clean build remains `NOT_RUN`. Because
R2 and R3 touch terminal/timeout observation ordering, a bounded same-artifact runtime
comparison is useful but remains `NOT_RUN` unless the user explicitly includes
deployment and runtime verification.

## 10. Rollback units and responsibility review slices

```text
RB1  R1 test-class split, migration ledger, and package-local fixtures
RB2  R2 automatic-ledger facade/component extraction and focused tests
RB3  R3 StoreHome facade/coordinator/observation extraction and focused tests

R2-A  automatic-ledger registry/run-state responsibility slice
R2-B  automatic terminal accounting and pure coverage responsibility slice
R3-A  StoreHome single lifecycle-coordinator responsibility slice
R3-B  StoreHome implemented read-only observation responsibility slice
```

`RB1`, `RB2`, and `RB3` are the top-level source rollback units. `RB1` stays
behavior-neutral, `RB2` retains the stable ledger facade, and `RB3` retains the
stable StoreHome facade. `R2-A`/`R2-B` share the component/facade composition
hunks, while `R3-A`/`R3-B` share coordinator construction and calls; they are
independently reviewable responsibility slices, not independently proven
mechanical reverts. The source rollback commits are `331d68e4` for R1,
`1086b490` for R2, and `4742d0a0` for R3. A rollback must restore the parent
unit's imports and tests together without reverting unrelated changes or
touching upstream source.

## 11. Non-goals

- source or test changes outside the exact R1-R3 responsibility splits;
- changing diagnostic event names, payload fields, caps, ordering, or modes;
- changing Store/deposit/home gameplay behavior;
- changing Task completion, retry, timeout policy, input, or Baritone paths;
- replacing either production class with a generic framework or base class;
- Forge/MineMind placeholders or shared backend implementation;
- dependency, Minecraft, Fabric, Loader, Loom, Gradle, Java, ChatClef,
  AltoClef, Baritone, or Carry On version changes;
- deployment, Minecraft launch, live-world mutation, commit, push, or release.

## 12. Completion gate and current result

The repository refactor meets this gate: every responsibility has one named
owner, the two stable facades preserve their contracts, all mutable lifecycle
state has one authoritative owner per operation, every original test assertion
is accounted for by the migration map, and the six-filter and clean-build
integration evidence passes for the exact mixed dirty worktree. Dedicated
reflection coverage of every ledger API signature/record component,
bookkeeping-fault injection, an isolated diagnostics-only clean build,
deployment, and runtime verification remain separately and honestly reported
as unproven or `NOT_RUN` above.
