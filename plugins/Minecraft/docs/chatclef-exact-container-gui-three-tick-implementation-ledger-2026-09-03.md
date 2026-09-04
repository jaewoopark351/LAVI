# ChatClef Exact-Container GUI Three-Tick Implementation Ledger

<!-- 20260903_kpopmodder: Recorded the applied exact-GUI gate, ownership, scope, and completed repository-local verification. -->
<!-- 20260903_kpopmodder: Added the deployed furnace failure evidence and the diagnostics-only source-to-hub-to-gate logging plan. -->
<!-- 20260903_kpopmodder: Corrected runtime/build evidence and completed the diagnostic schema, ownership, budget, and test-isolation plan. -->
<!-- 20260904_kpopmodder: Recorded the implemented bounded diagnostic checkpoint and corrected the active-source/runtime status without claiming a gate behavior fix. -->

## 1. Ledger Status

This ledger describes the Java source present in the working tree on
2026-09-03. The first block preserves the implementation-time checkpoint before
deployment. The post-build block immediately below records the later deployment
and failed Minecraft runtime reproduction; the earlier `NOT_RUN` values are
historical and are not the current runtime status.

```text
implementation:
  PRESENT_IN_WORKTREE

existing exact-gate lifecycle logging:
  PRESENT_IN_WORKTREE

source-to-hub-to-gate diagnostic reinforcement:
  IMPLEMENTED_AS_BOUNDED_DIAGNOSTICS_CHECKPOINT
  EXACT_GATE_INTAKE_NOT_PRESENT_IN_ACTIVE_SOURCE

focused test sources:
  PRESENT_AND_PASSED (61 / 61)

clean forced Gradle build:
  PASSED

test execution:
  PASSED (723 total, 722 passed, 1 skipped, 0 failures, 0 errors)

built remapped Fabric 1.20.1 JAR:
  VERIFIED_AND_HASHED

JAR deployment and active-instance hash comparison:
  NOT_RUN

Minecraft runtime reproduction:
  NOT_RUN

runtime acceptance status:
  BUILD_PASSED_RUNTIME_NOT_VERIFIED

commit and push:
  NOT_PERFORMED
```

Post-build runtime update:

```text
active-instance deployment:
  VERIFIED_IDENTICAL_TO_BUILT_JAR

built and active JAR SHA-256:
  A526C8FFD2A7ED0E78018D3180FE195AA280F1CB364EDDFEA28BCD12E268055D

Minecraft runtime reproduction:
  REPRODUCED_FAILED - regular furnace GUI opened; user-observed iron smelting did not proceed

last log-confirmed required forward-progress prerequisite before TAIL:
  EXACT_GUI_BLOCK_INTERACTION_OBSERVED

later observed timeout/control records:
  EXACT_GUI_OPEN_CHILD_GLOBAL_CLEANUP_SKIPPED (164)
  EXACT_GUI_INVALIDATED (164)
  EXACT_GUI_PARENT_RETRY_AUTHORIZED (164)

first missing required exact-gate boundary:
  EXACT_GUI_SCREEN_TAIL_CANDIDATE

suspected failing boundary:
  EXPECTED_TAIL_PRODUCER_OR_DELIVERY_TO_EXACT_GATE_INTAKE

root-cause confidence:
  LEADING_SOURCE_AND_LOG_HYPOTHESIS
  DIRECT_SOURCE/HUB/GATE_DECISION_LOG_PENDING

current runtime acceptance status:
  FAILED_REPRODUCED_FIRST_FAILING_BOUNDARY_DIRECT_OBSERVATION_PENDING

diagnostic reinforcement source implementation:
  PRESENT - generic container source/transport/task/slot/reconciliation observation
  EXACT_GATE_BEHAVIOR_SOURCE_ABSENT

diagnostic reinforcement tests/build/runtime:
  PASSED_AND_RUNTIME_OBSERVED - 630 tests, 0 failures, clean 171/171 tasks
```

The initial workflow was implementation, bounded log reinforcement, then verification.
Repository-local focused tests, the full Fabric 1.20.1 test suite, and the
canonical clean forced build passed. Deployment, Minecraft launch, runtime
Mixin acceptance, and live route reproduction had not yet run at that
checkpoint and are not implied by the build result. A later matching-JAR live
reproduction failed at or before exact-gate TAIL intake as detailed in Section
18. That later evidence supersedes only the historical runtime `NOT_RUN`
status; it does not invalidate the recorded build/test facts.

## 2. Baseline and Provenance

```text
active repository root:
  C:\Vtuber_Souorce_Code\LAVI

runtime root:
  plugins/Minecraft/runtime/chatclef_fabric_1.20.1

repository HEAD used as the working-tree comparison baseline:
  39ea27c83c6e5808535d68aef5a8b4c33548f002

worktree:
  DIRTY before and during this implementation

comparison basis:
  current files and git diff against the recorded HEAD

exact upstream ChatClef / AltoClef repository, tag, and commit:
  UNVERIFIED

backend scope:
  Fabric ChatClef 1.20.1 only

Forge / MineMind scope:
  EXCLUDED
```

No unrelated user change is part of this ledger's rollback unit. The upstream
engine seam record is maintained separately in
[ChatClef Engine Divergence Record](chatclef-engine-divergence-record.md#2026-09-03-exact-container-gui-three-later-tick-gate).

This ledger was still a new, Git-untracked file at the 2026-09-03 documentation
review. Tracked documents already link to it, so any later commit or packaged
documentation change must include this exact file in the same change unit.
This note does not stage or commit it.

## 3. Implemented Contract

The implementation is not "open a GUI, wait, then synthesize physical
Shift+right-click." Its contract is:

```text
create one route-owned open attempt
  -> send one normal world right-click through InteractWithBlockTask
  -> observe exactly one correlated BlockInteractEvent
  -> observe one raw ScreenOpenEvent TAIL source event
  -> receive and validate it as candidate K only with a proven active serial
  -> stop and quiesce the exact open wrapper and interaction child
  -> promote the exact binding at client RETURN boundary B
  -> validate and count distinct later RETURN boundary #1
  -> validate and count distinct later RETURN boundary #2
  -> validate and count distinct later RETURN boundary #3
  -> set GUI_INPUT_ALLOWED only
  -> on a later normal Task evaluation, fully revalidate and consume permission
  -> enter the route's existing transfer lifecycle
```

No slot action occurs in the third-boundary callback. No callback sends a
second world interaction or changes physical SNEAK input.

## 4. Ownership Model

### 4.1 Preserved engine and generic seams

ChatClef / AltoClef remains the scheduling and Task engine. LAVI does not
subclass `AltoClef`, `TaskRunner`, or another global lifecycle owner.

The minimum generic seams are:

- `InputControls.tryPressAndReportAccepted`: reports whether the already
  existing input request was newly accepted while preserving `void tryPress`.
- `InteractWithBlockLifecycleObserver`: default no-op accepted-input callback
  and default-`false` stop-cleanup decision.
- `ParentTaskRetention`: one predicate for preserving the already selected
  child while its local operation forbids parent evaluation.
- Storage and generic container open factories: defaults construct the same
  `InteractWithBlockTask` as before.
- `GuiTickRuntimeAccess`: exposes one client-instance event registration and
  active serial surface to LAVI-owned code.

### 4.2 Operation owners

| Route | Gate owner | Open child | Transfer owner |
| --- | --- | --- | --- |
| `TRUSTED_HOME` | The active manual `store_home` operation Task, with state held by its `StoreHomeCandidateAttempt` | `ExactContainerGuiOpenTask` composing one `InteractWithBlockTask` | Existing `HomeStorageQuickMoveIssuer` through the trusted-home session/executor |
| `GENERAL_STORAGE` | The target-specific `StoreInContainerTask` | `ExactContainerGuiOpenTask` composing one `InteractWithBlockTask` | Existing `MoveItemToSlotFromInventoryTask` |
| `REGULAR_FURNACE` | Outer `SmeltInFurnaceTask`, with gate held by its existing inner `DoSmeltInFurnaceTask` | `ExactContainerGuiOpenTask` created through the `DoStuffInContainerTask` factory seam | Existing regular-furnace container Task and slot call sites |

The gate owns only its operation, attempt, correlation, binding snapshot,
boundary count, permission, listener registration, and terminal reason. It
does not own global TaskRunner state, generic input cleanup, or global Baritone
state.

## 5. Routes, Exact Mappings, and Exclusions

| Route | Included blocks | Exact screen class | Exact handler class |
| --- | --- | --- | --- |
| `TRUSTED_HOME` | `CHEST`, `TRAPPED_CHEST` | `GenericContainerScreen` | `GenericContainerScreenHandler` |
| `GENERAL_STORAGE` | `CHEST`, `TRAPPED_CHEST` | `GenericContainerScreen` | `GenericContainerScreenHandler` |
| `REGULAR_FURNACE` | `FURNACE` | `FurnaceScreen` | `FurnaceScreenHandler` |

Class matching uses exact runtime class equality, not a broad subclass match.

The first implementation excludes:

```text
BARREL
all SHULKER_BOX variants
SMOKER
BLAST_FURNACE
all other generic DoStuffInContainerTask routes
```

Excluded storage targets keep the base `InteractWithBlockTask` factory and
legacy container-open predicate. The trusted-home classifier pins barrel and
shulker candidates to `LEGACY`; the route may not change after it is pinned.
Smoker and blast furnace do not override the new default factory and therefore
remain outside the gate.

## 6. Event and Tick Ordering

### 6.1 Existing event producers reused

The implementation reuses these existing engine events without changing their
producer files in this feature diff:

- `ClientInteractWithBlockMixin` publishes `BlockInteractEvent` at
  `ClientPlayerInteractionManager.interactBlock` HEAD when the hit result is
  non-null. This event proves the world-interaction call boundary, not its
  returned `ActionResult`.
- `ClientOpenScreenMixin` publishes `ScreenOpenEvent(preOpen=true)` at
  `MinecraftClient.setScreen` HEAD and `ScreenOpenEvent(preOpen=false)` at
  TAIL.

The LAVI-owned `GuiContainerEventHub` subscribes once per client-instance hub
and forwards events only while an active client-tick serial exists.

The later furnace reproduction exposed an observation gap in this sentence.
`ClientOpenScreenMixin` currently sends its source log through a VERBOSE-only
path, while `GuiContainerEventHub.onScreen()` returns without a record when the
active serial is absent. Therefore BOUNDARY logs cannot currently distinguish
"TAIL producer did not run" from "TAIL reached the hub between ticks and the
hub dropped it." Section 18 defines the diagnostics-only evidence needed to
make that distinction without changing the drop behavior.

### 6.2 Serial lifecycle

`ClientTickMixin` performs this order:

```text
MinecraftClient.tick HEAD:
  GuiClientTickSerialState.beginTick()
  initialize/access the event hub
  existing diagnostics tick-head callback
  existing ClientTickEvent publication and normal Task evaluation

MinecraftClient.tick RETURN:
  claim this active serial at most once
  publish one client-tick boundary to registered exact-GUI listeners
  endTick() in finally
```

Serials are monotonic and per `MinecraftClient` instance. Nested/reentrant
begin, duplicate boundary claims, unavailable active serials, and overflow
fail closed by withholding a boundary.

### 6.3 `K`, `B`, and the three later boundaries

- A raw `ScreenOpenEvent` TAIL is not yet `K`. If it arrives without an active
  serial, the diagnostics record `candidateClientTickSerial=-1`; the current
  behavior does not retain, replay, forward, or backfill it.
- `K` is `candidateClientTickSerial` only after a TAIL is delivered under a
  proven active serial and accepted as the attempt's candidate.
- `B` is `boundClientTickSerial`, the RETURN boundary where the TAIL candidate
  is promoted only after open-child stop and cleanup are complete.
- `lastCountedBoundarySerial` is initialized to `B`.
- A boundary counts only when it is strictly greater than both `B` and the
  last counted boundary.
- The boundary count must equal exactly three before permission becomes
  available.

`K` and `B` may be equal, but neither accepted-candidate boundary `K` nor
promotion boundary `B` counts as later boundary #1. Same-serial duplicate delivery cannot advance the
counter.

## 7. Correlation and Exact Binding

### 7.1 Input and event correlation

One attempt is created only after `InputControls` reports the normal world-open
input newly accepted during an active client tick. The attempt records:

```text
operationId
openAttemptId
correlationId
retryIndex
requestedClientTickSerial
acceptedInputClientTickSerial
pinned world / dimension / target / block
```

The matching `BlockInteractEvent` must have the pinned block position and the
same serial as the accepted input. Exactly one such event is accepted. A
different target, wrong serial, or duplicate matching event invalidates the
attempt.

The matching interaction can authorize one TAIL candidate only. Accepted and
rejected TAIL candidates both spend it permanently; retry creates a fresh
attempt and correlation identity.

### 7.2 Candidate capture and revalidation

Candidate capture and every stability/transfer revalidation check the
applicable subset of:

```text
owning operation remains active
route owner remains active
same world object identity
same dimension registry key
same pinned target position
same target block object identity
target block remains in the route allowlist
target chunk remains loaded
event screen == MinecraftClient.currentScreen
exact expected screen runtime class
screen is a HandledScreen
handled-screen handler == player.currentScreenHandler
captured handler object identity remains unchanged
exact expected handler runtime class
captured and live syncId remain unchanged
```

Chat, inventory, another chest, a stale attempt, a replaced screen/handler, or
a changed world/dimension/target fails closed.

## 8. State Machine

`ExactContainerGuiOpenAttempt` uses these phases:

```text
IDLE
WORLD_OPEN_REQUESTED
WAITING_FOR_SCREEN
SCREEN_TAIL_CANDIDATE
GUI_STABILIZING
GUI_INPUT_ALLOWED
TRANSFER_ACTIVE
INVALIDATED
OWNER_STOPPING
```

The active path is:

```text
WORLD_OPEN_REQUESTED
  accepted correlated BlockInteractEvent
WAITING_FOR_SCREEN
  accepted and spent ScreenOpenEvent TAIL candidate
SCREEN_TAIL_CANDIDATE
  stopped/quiescent child + valid live binding at RETURN B
GUI_STABILIZING
  three distinct, valid boundaries strictly later than B
GUI_INPUT_ALLOWED
  next normal Task evaluation + full validation + commitOnce
TRANSFER_ACTIVE
```

Any identity, ordering, quiescence, timeout, or binding failure transitions to
`INVALIDATED`. Owner stop clears permission and transitions to
`OWNER_STOPPING`. There is no callback transition that directly performs
transfer behavior.

## 9. Open-Child Quiescence and Mutation Freeze

After accepted input creates an attempt, `mayTickOpenChild` permanently closes
that open-child tick path for the attempt. The Task framework stops the wrapper
and nested interaction child before promotion.

The exact observer owns only the exact wrapper's stop decision. Generic
`InteractWithBlockTask` callers retain the existing global cleanup. The exact
path does not claim ownership of global `forceCancel()` or a broad SNEAK
release. Before promotion it proves:

```text
exact open wrapper stopped and inactive
nested InteractWithBlockTask stopped and inactive
CLICK_RIGHT not held
SNEAK not held
portal MOVE_FORWARD not held
custom goal process not active
Baritone pathing not active
```

From accepted input through stabilization, the route leaf blocks parent and
existing-child mutation. Ancestor propagation keeps that same leaf reachable
without executing acquisition, scanner, progress, placement, cleanup, timeout,
travel, or fallback branches:

- `AbstractDoToClosestObjectTask` retains its current goal child.
- `StoreInAnyContainerTask` and `StoreInStashTask` return their cached closest
  route first.
- `DepositAllTask` and `DepositAllStoreTaskGeneration` retain the stable store
  generation.
- `AutoDepositTrustedStoreTask` retains its sole store child; the prior
  duplicate ungated open child is removed.
- `AutoDepositMaintenanceTask` retains the active trusted/general child before
  context or phase work.
- `SmeltInFurnaceTask` retains the inner exact route before `ResourceTask`
  fallback and suppresses stop-time screen/cursor cleanup in the protected
  pre-transfer window.
- Manual trusted-home lifecycle deferral prioritizes its exact candidate before
  timeout, candidate rejection, resume, or fallback mutation.

Callbacks may update attempt bookkeeping and bounded diagnostics. They do not
perform gameplay mutation.

## 10. Transfer Entry Contracts

### 10.1 Shared one-time permission

Later boundary #3 changes the phase to `GUI_INPUT_ALLOWED` only. A normal Task
evaluation must then:

1. confirm the expected attempt and target;
2. prove the open child remains quiescent;
3. fully revalidate the live exact binding;
4. pass route-specific transfer preconditions;
5. atomically consume the attempt-scoped permission before transfer becomes
   reachable.

The first successful commit transitions to `TRANSFER_ACTIVE`. A second commit
records permission reuse rejection and returns `false`. The consumed permission
is never reused. If the live binding invalidates while `TRANSFER_ACTIVE`, the
whole attempt is retired and the same pinned target receives a fresh attempt,
correlation identity, and retry generation. The owning route recomputes its
remaining work before the new attempt can reach transfer, preventing a
permanent no-op while preserving one-time permission semantics.

### 10.2 Trusted-home

`StoreHomeSessionPreparer` completes fallible inventory capture and planning
before permission consumption. `StoreHomeSessionActivator` then commits the
exact permission, applies the prepared session state, and finalizes diagnostics
in that order. Only an active committed session reaches the existing
`HomeStorageQuickMoveIssuer`.

The existing transfer is unchanged:

```text
button: 0
slot action: QUICK_MOVE
physical SNEAK: not synthesized
second world right-click: not synthesized
```

### 10.3 General storage

`StoreInContainerTask` selects the existing
`MoveItemToSlotFromInventoryTask`, verifies a destination slot exists, and
commits permission before returning the transfer child. Its existing slot
contract remains:

```text
slot action: PICKUP
button: existing 0-or-1 task contract
```

### 10.4 Regular furnace

Before permission consumption, the regular-furnace route refreshes the open
furnace state and verifies material count, fuel availability, and material
accessibility through `RegularFurnaceTransferEntryValidator`. It then enters
the existing furnace container lifecycle. Existing furnace calls remain
PICKUP-based with their existing button values.

## 11. Retry, Timeout, Interruption, and Cleanup

- General storage and regular furnace use a maximum open/quiescence budget of
  20 client boundaries per attempt.
- Trusted-home passes its existing candidate-local interaction budget to the
  gate; this ledger does not replace that operation's timeout owner.
- Retry after pre-transfer `INVALIDATED` state proceeds only through the owning
  parent call site. Retry replaces attempt/correlation identity and resets the
  boundary count.
- A consumed transfer permission is never retried or reused. If live binding
  invalidation occurs during `TRANSFER_ACTIVE`, the owning parent retires the
  entire old attempt and creates a fresh attempt, correlation identity, and
  retry generation for the same pinned target, then recomputes remaining work.
- Owner stop unregisters the listener and clears permission state.
- Candidate rejection, terminal completion, resume replacement, and session
  teardown call operation-owned gate invalidation/close paths.
- The feature does not stop TaskRunner, cancel all Baritone work, release every
  input, or close every screen as generic cleanup.

This gate does not solve Carry On stale-key pickup before the requested GUI
opens. That remains a separate policy and reproduction boundary.

## 12. Bounded Diagnostics

`ExactContainerGuiGateDiagnostics` emits transition/boundary records through
the existing bounded diagnostics path with a per-event UTF-8 cap of 8192
bytes. It does not poll unchanged state every tick and does not choose behavior.

The records use the canonical exact GUI field names, including:

```text
operationId
openAttemptId
correlationId
gameTick
threadName
routeOwnerClass
routeOwnerIdentity
retryOwnerClass
retryIndex
target
targetBlockId
targetFamily
worldIdentity
dimension
clientTickBoundarySerial
candidateClientTickSerial
boundClientTickSerial
lastCountedBoundarySerial
eventPreOpen
matchingBlockInteractEventObserved
matchingBlockInteractEventCount
matchingBlockInteractEventConsumed
duplicateBlockInteractEventRejected
openChildIdentity
openChildState
openChildQuiescent
openChildCleanupOwner
openChildCleanupComplete
screenTailCandidate
tailCandidateSnapshotIdentity
screenObjectIdentity
screenName
screenTypeExpected
screenTypeActual
screenTypeMatched
screenIsHandled
handledScreenHandlerIdentity
playerHandlerIdentity
capturedSyncId
liveSyncId
handlerTypeExpected
handlerTypeActual
handlerTypeMatched
fullGuiBoundPredicate
stableLaterBoundaries
candidateTickExcluded
boundPromotionTickExcluded
sameBoundaryDuplicateSuppressed
guiInputAllowed
permissionAttemptIdentity
permissionAvailable
permissionFullRevalidationPassed
permissionConsumed
permissionReuseRejected
transferLifecycleEntryCommitted
slotMutationSuppressed
reachableMutationPath
reachableMutationKind
suppressedMutationOwner
slotActionOwner
slotActionType
slotButton
invalidationReason
terminalReason
carryState
```

`carryState` is explicitly `NOT_OBSERVED_POST_GUI_GATE`; the GUI stability gate
does not fabricate Carry On success.

## 13. Source Inventory

The inventory below was re-read from the actual tree while the implementation
was being finalized.

### 13.1 Modified or added generic/upstream-namespace seams

```text
src/main/java/adris/altoclef/control/InputControls.java
src/main/java/adris/altoclef/mixins/ClientTickMixin.java
src/main/java/adris/altoclef/tasks/InteractWithBlockTask.java
src/main/java/adris/altoclef/tasks/interaction/InteractWithBlockLifecycleObserver.java
src/main/java/adris/altoclef/tasks/AbstractDoToClosestObjectTask.java
src/main/java/adris/altoclef/tasks/ResourceTask.java
src/main/java/adris/altoclef/tasks/speedrun/beatgame/BeatMinecraftTask.java
src/main/java/adris/altoclef/tasksystem/Task.java
src/main/java/adris/altoclef/tasksystem/ParentTaskRetention.java
src/main/java/adris/altoclef/tasks/container/AbstractDoToStorageContainerTask.java
src/main/java/adris/altoclef/tasks/container/DoStuffInContainerTask.java
```

### 13.2 Modified route owners and ancestor propagation

```text
src/main/java/adris/altoclef/tasks/container/StoreInContainerTask.java
src/main/java/adris/altoclef/tasks/container/SmeltInFurnaceTask.java
src/main/java/adris/altoclef/tasks/container/StoreInAnyContainerTask.java
src/main/java/adris/altoclef/tasks/container/StoreInStashTask.java
src/main/java/adris/altoclef/tasks/container/DepositAllTask.java
src/main/java/lavi/minecraft/task/container/deposit/DepositAllStoreTaskGeneration.java
src/main/java/lavi/minecraft/task/container/deposit/auto/trusted/execution/AutoDepositTrustedStoreTask.java
src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceTask.java
```

### 13.3 LAVI-owned shared exact-GUI implementation

```text
src/main/java/lavi/minecraft/diagnostics/container/gui/ExactContainerGuiGateDiagnostics.java

src/main/java/lavi/minecraft/task/container/gui/binding/ExactContainerGuiBindingSnapshot.java
src/main/java/lavi/minecraft/task/container/gui/binding/ExactContainerGuiBindingValidation.java
src/main/java/lavi/minecraft/task/container/gui/binding/ExactContainerGuiBindingValidator.java
src/main/java/lavi/minecraft/task/container/gui/binding/ExactContainerGuiOpenChildQuiescence.java
src/main/java/lavi/minecraft/task/container/gui/binding/ExactContainerGuiRoute.java

src/main/java/lavi/minecraft/task/container/gui/event/GuiContainerEventHub.java
src/main/java/lavi/minecraft/task/container/gui/event/GuiContainerEventListener.java

src/main/java/lavi/minecraft/task/container/gui/gate/attempt/creation/ExactContainerGuiOpenAttemptFactory.java
src/main/java/lavi/minecraft/task/container/gui/gate/attempt/state/ExactContainerGuiAttemptState.java
src/main/java/lavi/minecraft/task/container/gui/gate/attempt/tick/ExactContainerGuiOpenTickGuard.java
src/main/java/lavi/minecraft/task/container/gui/gate/cleanup/ExactContainerGuiOpenChildCleanupHandoff.java

src/main/java/lavi/minecraft/task/container/gui/gate/ExactContainerGuiAttemptIdentity.java
src/main/java/lavi/minecraft/task/container/gui/gate/ExactContainerGuiGate.java
src/main/java/lavi/minecraft/task/container/gui/gate/ExactContainerGuiGatePhase.java
src/main/java/lavi/minecraft/task/container/gui/gate/ExactContainerGuiOpenAttempt.java

src/main/java/lavi/minecraft/task/container/gui/gate/correlation/ExactContainerGuiEventCoordinator.java
src/main/java/lavi/minecraft/task/container/gui/gate/correlation/ExactContainerGuiEventOutcome.java
src/main/java/lavi/minecraft/task/container/gui/gate/correlation/ExactContainerGuiInteractionCorrelation.java

src/main/java/lavi/minecraft/task/container/gui/gate/lifecycle/ExactContainerGuiGateReadiness.java
src/main/java/lavi/minecraft/task/container/gui/gate/lifecycle/ExactContainerGuiOpenChildResourceState.java
src/main/java/lavi/minecraft/task/container/gui/gate/lifecycle/ExactContainerGuiOperationLifecycle.java
src/main/java/lavi/minecraft/task/container/gui/gate/lifecycle/ExactContainerGuiPinnedTarget.java

src/main/java/lavi/minecraft/task/container/gui/gate/logging/ExactContainerGuiGateLogAdapter.java

src/main/java/lavi/minecraft/task/container/gui/gate/runtime/ExactContainerGuiRuntimeRegistration.java

src/main/java/lavi/minecraft/task/container/gui/gate/stabilization/ExactContainerGuiBoundaryCounter.java
src/main/java/lavi/minecraft/task/container/gui/gate/stabilization/ExactContainerGuiPermissionState.java
src/main/java/lavi/minecraft/task/container/gui/gate/stabilization/ExactContainerGuiStabilizationCoordinator.java
src/main/java/lavi/minecraft/task/container/gui/gate/stabilization/ExactContainerGuiStabilizationOutcome.java

src/main/java/lavi/minecraft/task/container/gui/gate/transfer/ExactContainerGuiTransferEntryControl.java

src/main/java/lavi/minecraft/task/container/gui/task/ExactContainerGuiOpenTask.java

src/main/java/lavi/minecraft/task/container/gui/tick/GuiActiveClientTickSerialSource.java
src/main/java/lavi/minecraft/task/container/gui/tick/GuiClientTickSerialState.java
src/main/java/lavi/minecraft/task/container/gui/tick/GuiTickRuntimeAccess.java

src/main/java/lavi/minecraft/task/container/gui/transfer/furnace/RegularFurnaceTransferEntryValidator.java
```

### 13.4 LAVI-owned trusted-home containment

Modified existing files:

```text
src/main/java/lavi/minecraft/task/container/home/execution/candidate/StoreHomeCandidateAttempt.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/navigation/StoreHomeCandidateNavigationStep.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/rejection/StoreHomeCandidateRejector.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/selection/StoreHomeCandidateAttemptStarter.java
src/main/java/lavi/minecraft/task/container/home/execution/operation/terminal/StoreHomeOperationTerminator.java
src/main/java/lavi/minecraft/task/container/home/execution/session/HomeStorageContainerSession.java
src/main/java/lavi/minecraft/task/container/home/execution/session/activation/StoreHomeSessionActivator.java
src/main/java/lavi/minecraft/task/container/home/execution/session/flow/StoreHomeSessionStep.java
src/main/java/lavi/minecraft/task/container/home/execution/task/composition/StoreHomeTaskAssembly.java
src/main/java/lavi/minecraft/task/container/home/execution/task/lifecycle/StoreHomeTaskLifecycleController.java
```

New responsibility-separated collaborators:

```text
src/main/java/lavi/minecraft/task/container/home/execution/candidate/open/StoreHomeCandidateOpenLifecycle.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/resume/StoreHomeCandidateResumeLifecycle.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/resume/StoreHomeCandidateResumeReset.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/route/StoreHomeCandidateRoute.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/route/StoreHomeCandidateRouteClassifier.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/route/StoreHomeCandidateRouteDecision.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/route/StoreHomeCandidateRoutePin.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/route/StoreHomeCandidateRoutePolicy.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/route/target/StoreHomeExactRouteTargetPin.java
src/main/java/lavi/minecraft/task/container/home/execution/candidate/transfer/StoreHomeCandidateTransferEntry.java
src/main/java/lavi/minecraft/task/container/home/execution/session/activation/commit/StoreHomeSessionCommitter.java
src/main/java/lavi/minecraft/task/container/home/execution/session/activation/finalization/StoreHomeSessionActivationFinalizer.java
src/main/java/lavi/minecraft/task/container/home/execution/session/activation/preparation/StoreHomeSessionPreparation.java
src/main/java/lavi/minecraft/task/container/home/execution/session/activation/preparation/StoreHomeSessionPreparer.java
src/main/java/lavi/minecraft/task/container/home/execution/session/activation/resume/StoreHomeSessionResumeActivator.java
src/main/java/lavi/minecraft/task/container/home/execution/session/activation/resume/commit/StoreHomeSessionRebindCommitter.java
src/main/java/lavi/minecraft/task/container/home/execution/session/activation/resume/preparation/StoreHomeSessionRebindPreparation.java
src/main/java/lavi/minecraft/task/container/home/execution/session/activation/resume/preparation/StoreHomeSessionRebindPreparer.java
src/main/java/lavi/minecraft/task/container/home/execution/task/lifecycle/gui/StoreHomeExactGuiLifecycleDeferral.java
```

Existing transfer owner intentionally unchanged:

```text
src/main/java/lavi/minecraft/task/container/home/execution/transfer/click/HomeStorageQuickMoveIssuer.java
```

### 13.5 Existing event sources intentionally unchanged

```text
src/main/java/adris/altoclef/mixins/ClientInteractWithBlockMixin.java
src/main/java/adris/altoclef/mixins/ClientOpenScreenMixin.java
src/main/java/adris/altoclef/eventbus/events/BlockInteractEvent.java
src/main/java/adris/altoclef/eventbus/events/ScreenOpenEvent.java
```

## 14. Focused Test Inventory

The following test sources are present:

```text
src/test/java/lavi/minecraft/task/container/gui/ExactContainerGuiArchitectureContractTest.java
src/test/java/lavi/minecraft/task/container/gui/ExactContainerGuiMutationAndDiagnosticsContractTest.java
src/test/java/lavi/minecraft/task/container/gui/event/GuiContainerEventHubTest.java
src/test/java/lavi/minecraft/task/container/gui/gate/ExactContainerGuiLifecycleStateTest.java
src/test/java/lavi/minecraft/task/container/gui/gate/ExactContainerGuiStateMachineTest.java
src/test/java/lavi/minecraft/task/container/gui/tick/ClientTickMixinGuiBoundaryOrderTest.java
src/test/java/lavi/minecraft/task/container/gui/tick/GuiClientTickSerialStateTest.java
src/test/java/lavi/minecraft/task/container/gui/transfer/furnace/RegularFurnaceTransferEntryValidatorTest.java
src/test/java/lavi/minecraft/task/container/retention/ParentTaskRetentionPropagationContractTest.java
src/test/java/lavi/minecraft/task/container/retention/RegularFurnaceParentRetentionContractTest.java
src/test/java/lavi/minecraft/task/container/deposit/DepositAllStoreTaskRetentionTest.java
src/test/java/lavi/minecraft/task/container/home/execution/candidate/StoreHomeExactGuiHardeningContractTest.java
src/test/java/lavi/minecraft/task/container/home/execution/candidate/route/StoreHomeCandidateRouteClassifierTest.java
src/test/java/lavi/minecraft/task/container/home/execution/candidate/route/StoreHomeCandidateRoutePinTest.java
src/test/java/lavi/minecraft/testsupport/storehome/StoreHomeCandidateAttemptFixture.java
src/test/java/adris/altoclef/tasks/container/StoreContainerParentRouteLifecycleTest.java
```

They are intended to cover:

```text
monotonic active serial and exact-once RETURN boundary
HEAD / existing ClientTickEvent / RETURN ordering
event-hub registration, unregistration, snapshot dispatch, and duplicate boundary suppression
one accepted input and exactly one matching BlockInteractEvent
accepted/rejected one-time TAIL consumption
K versus B and three strictly later boundaries
same-boundary duplicate suppression
exact screen/handler/target/world/dimension binding
open-child stop and resource quiescence
permission commit exactly once and reuse rejection
mutation-free callbacks and parent suppression ordering
default-preserving factories and excluded routes
regular-furnace transfer-entry preconditions
trusted-home route classification and immutable pin
trusted-home cleanup, activation, and lifecycle ordering
ancestor child-retention propagation
automatic trusted duplicate-open-owner removal
```

The focused relevant suite passed 61 of 61 tests. The full Fabric 1.20.1 suite
reported 723 tests: 722 passed, 1 skipped, 0 failures, and 0 errors.

The latest canonical build command was executed from the runtime root with
Gradle 8.8 on Eclipse Temurin JDK 21.0.12+8-LTS:

```text
.\gradlew.bat clean build --rerun-tasks
BUILD SUCCESSFUL in 3m 49s
171 actionable tasks: 171 executed
```

### 14.1 Canonical Artifact Evidence

The canonical build produced and the archive inspection identified this
unclassified remapped runtime JAR:

```text
absolute path:
  C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar

size:
  8,132,655 bytes

last modified:
  2026-09-03 18:57:31 +09:00

SHA-256:
  A526C8FFD2A7ED0E78018D3180FE195AA280F1CB364EDDFEA28BCD12E268055D

inspected archive entries present:
  ClientTickMixin.class
  ExactContainerGuiGate.class
  ExactContainerGuiAttemptState.class
  fabric.mod.json
```

This identifies the repository-local build artifact only. It does not prove
deployment, runtime Mixin application, Minecraft launch, or live route
acceptance.

## 15. Verification Matrix

| Verification item | Current result | Evidence required before changing it |
| --- | --- | --- |
| Static file inventory | RECORDED | Re-read tree after all concurrent source edits settle |
| Java compilation | PASSED | Canonical clean forced Gradle build completed successfully |
| Focused tests | PASSED: 61 / 61 | Executed focused relevant suite |
| Full Fabric 1.20.1 tests | PASSED: 723 total, 722 passed, 1 skipped, 0 failures/errors | Executed `:1.20.1:test` report |
| Canonical clean forced build | PASSED: 3m 49s, 171 / 171 actionable tasks executed | `.\gradlew.bat clean build --rerun-tasks`, Gradle 8.8, Eclipse Temurin JDK 21.0.12+8-LTS |
| Mixin application | PARTIAL: Minecraft launched and exact-gate/client-tick behavior ran; affected `setScreen` TAIL execution is not directly observable in BOUNDARY mode | New bounded source record plus launch log with no relevant injection failure |
| Built JAR identity | VERIFIED: unclassified remapped runtime JAR, 8,132,655 bytes, SHA-256 `A526C8FFD2A7ED0E78018D3180FE195AA280F1CB364EDDFEA28BCD12E268055D` | Absolute path, modification time, and inspected archive entries recorded in Section 14.1 |
| Active instance deployment | VERIFIED: active and built JAR size/hash equal | Both files are 8,132,655 bytes with SHA-256 `A526C8FFD2A7ED0E78018D3180FE195AA280F1CB364EDDFEA28BCD12E268055D` |
| Chest route runtime | NOT_RUN / BUILD_PASSED_RUNTIME_NOT_VERIFIED | Correlated logs showing K, B, later 1/2/3, next Task transfer |
| Trapped-chest route runtime | NOT_RUN / BUILD_PASSED_RUNTIME_NOT_VERIFIED | Same correlated sequence |
| Regular-furnace runtime | FAILED_REPRODUCED: GUI became live; no exact TAIL candidate or transfer-commit record was admitted before the ordinary ceiling; user-observed iron smelting did not proceed | Source/hub/gate decision sequence from one bounded reproduction |
| Barrel/shulker legacy regression | NOT_RUN / BUILD_PASSED_RUNTIME_NOT_VERIFIED | No gate attempt/retention and existing transfer behavior |
| Smoker/blast legacy regression | NOT_RUN / BUILD_PASSED_RUNTIME_NOT_VERIFIED | No exact-gate activation and existing transfer behavior |
| Carry On absent class loading | NOT_RUN / BUILD_PASSED_RUNTIME_NOT_VERIFIED | Successful launch and generic route behavior without Carry On |
| Generic right-click regression | NOT_RUN / BUILD_PASSED_RUNTIME_NOT_VERIFIED | Doors, buttons/levers, beds, placement, and item-use checks |
| Runtime bounded logging | PARTIAL_FAILED: existing gate boundaries were visible, but source/hub/gate TAIL intake was not; the ordinary ceiling of 4936 was reached while critical reserve remained | Diagnostics-only reinforcement in Section 18, then one fresh bounded reproduction before the ordinary ceiling |

## 16. Runtime Acceptance Sequence

A successful runtime capture must show one correlation with this order:

```text
EXACT_GUI_OPEN_REQUESTED
EXACT_GUI_BLOCK_INTERACTION_OBSERVED
EXACT_GUI_SCREEN_TAIL_CANDIDATE
EXACT_GUI_BOUND                         at B
EXACT_GUI_STABLE_LATER_BOUNDARY         count 1, serial > B
EXACT_GUI_STABLE_LATER_BOUNDARY         count 2, serial > prior
EXACT_GUI_INPUT_ALLOWED                 count 3, permission only
EXACT_GUI_TRANSFER_ENTRY_COMMITTED      later normal Task evaluation
existing route-owned transfer evidence
```

The capture must also prove:

```text
matchingBlockInteractEventCount == 1
matchingBlockInteractEventConsumed == true
openChildQuiescent == true before GUI_BOUND
boundPromotionTickExcluded == true at B
stableLaterBoundaries advances 0 -> 1 -> 2 -> 3 only
permissionFullRevalidationPassed == true at commit
permissionConsumed == true exactly once
no slot action before commit
no callback-owned SNEAK, second right-click, screen close, or Baritone mutation
```

Invalidation and legacy-route captures are equally important; they must retain
their typed terminal reason instead of being reported as success.

## 17. Rollback Boundary

Rollback is dependency ordered and hunk scoped:

1. Disconnect each exact route and ancestor retention caller.
2. Remove the LAVI-owned trusted-home containment collaborators only after the
   former composition is restored without losing unrelated user changes.
3. Revert only the minimal upstream accepted-result, observer, retention,
   factory, and client-RETURN hunks recorded in the engine divergence record.
4. Remove shared exact-GUI collaborators and tests only after no caller remains.

Do not roll back with `git reset`, `git checkout --`, `git restore`, broad file
replacement, directory deletion, or a broad commit revert. Preserve unrelated
diagnostics, automatic-deposit, post-place handoff, and user-owned dirty work.

## 18. Post-Build Furnace Runtime Reproduction And Diagnostic Reinforcement Plan

### 18.1 Scope And Evidence Status

This section records a later live failure and the next diagnostics-only source
plan. It does not claim that the suspected cause is already proven, and it does
not apply or describe a hidden behavior fix.

The active CurseForge JAR and the current built JAR were re-read as:

```text
active instance JAR:
  C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\chatclef-1.20.1-0.18.23.jar

built JAR:
  C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar

size of each:
  8,132,655 bytes

last modified for each:
  2026-09-03 18:57:31 +09:00

SHA-256 of each:
  A526C8FFD2A7ED0E78018D3180FE195AA280F1CB364EDDFEA28BCD12E268055D
```

Primary evidence paths:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\instance_audit.txt
C:\Vtuber_Souorce_Code\LAVI\logs\20260903_190005_log.txt
```

These are mutable operational paths. The evidence bytes reviewed for this
incident are pinned below so a later Minecraft launch cannot silently replace
the evidence basis:

| Evidence file | Size | Last modified | SHA-256 |
| --- | ---: | --- | --- |
| `latest.log` | 13,370,373 bytes | 2026-09-03 19:39:02 +09:00 | `3D59A4B9BFB5FAB954965A43307A12BBA17CAB387A34D060E543A866C815100E` |
| `stdout-logs.txt` | 14,365,627 bytes | 2026-09-03 19:39:06 +09:00 | `ECF29A6B56FE0350C1714250E707EE028CC4188C94D66A32FD9B03BC1A82734C` |
| `instance_audit.txt` | 2,562,316 bytes | 2026-09-03 19:00:11 +09:00 | `9D0109EBF62B05CF948EB8BB16A0B419510A1CCB5C5BBDE75CD7971FCF2346B9` |
| `20260903_190005_log.txt` | 274,770 bytes | 2026-09-03 19:44:56 +09:00 | `AE81B79B4F15264B25EEBA4F9D225AF5405408DA4DA90EA58E8DEEAE1D04CC3A` |

The first furnace attempt in `latest.log` uses:

```text
operationId=68d5e82e-2988-4619-b6bd-4d85e61c0aa2
openAttemptId=deb21e21-75b0-49b1-8ee4-cab33524bd9b
correlationId=eca2fdb4-1d25-44c6-a4b1-8e7c9f09b6c1
target=minecraft:furnace at -721,58,76
```

The capture contains these exact event totals before or including the shared
diagnostic admission limit:

| Event or observation | Count | Meaning |
| --- | ---: | --- |
| `EXACT_GUI_OPEN_REQUESTED` | 165 | Normal world-open input was accepted for 165 attempts. |
| `EXACT_GUI_BLOCK_INTERACTION_OBSERVED` | 165 | One matching furnace interaction was seen for each recorded attempt. |
| `GUI_OPEN_DELAYED` total | 165 | This broad independent observer includes 164 furnace observations and one unrelated crafting-table observation. |
| `GUI_OPEN_DELAYED` with `targetKind=furnace` and handler `class_3858` | 164 | The independent observation window saw the requested `FurnaceScreenHandler` and a changed syncId on the next tick. |
| `GUI_OPEN_DELAYED` with `targetKind=crafting_table` and handler `class_1714` | 1 | This record at `latest.log` line 4213 is not a furnace-handler observation and must not be counted as one. |
| `EXACT_GUI_OPEN_CHILD_GLOBAL_CLEANUP_SKIPPED` | 164 | Later control evidence exists after the interaction; this is not a forward-progress boundary toward TAIL acceptance. |
| `EXACT_GUI_INVALIDATED` with `gui_open_or_quiescence_timeout` | 164 | The gate timed out without a captured TAIL candidate. The final detail may be affected by the session cap. |
| `EXACT_GUI_PARENT_RETRY_AUTHORIZED` | 164 | The parent authorized a new attempt after each recorded timeout. |
| `EXACT_GUI_SCREEN_TAIL_CANDIDATE` | 0 admitted before the ordinary ceiling | No candidate record was observed in the admitted evidence window; post-ceiling exact-gate detail is unobserved. |
| `EXACT_GUI_BOUND` | 0 admitted before the ordinary ceiling | No stabilization start was observed in the admitted evidence window; post-ceiling exact-gate detail is unobserved. |
| stable-boundary/input-permission/`EXACT_GUI_TRANSFER_ENTRY_COMMITTED` records | 0 admitted before the ordinary ceiling | No logical transfer-entry commit was observed in the admitted evidence window. Permission consumption is a field on that commit, not a separate event. |
| BOUNDARY-visible generic slot-request records | 0 | Generic `logSlotClick` is VERBOSE-only, so this capture alone does not establish the total physical `clickSlot` call count. |
| `DIAGNOSTIC_SESSION_CAP_REACHED` | 1 | At log line 5475, ordinary admission reached its 4936 ceiling while partitioned critical reserve remained active; the total 5000-event hard cap was not exhausted. |

At tick 1891 the world interaction was recorded. At tick 1892 the independent
container observation reported a live `FurnaceScreen`/`FurnaceScreenHandler`
with syncId 1, yet the exact attempt still had
`candidateClientTickSerial=-1`. This rules out "the furnace GUI never opened"
for that attempt. Before the ordinary ceiling, no candidate, `GUI_BOUND`,
permission, or exact-gate transfer-commit record was admitted. Post-ceiling
exact-gate detail is unobserved, and generic slot logging is VERBOSE-only, so
this capture alone does not prove the whole-run physical `clickSlot` or
transfer-commit count. The user-observed outcome remains that iron smelting did
not proceed.

The source and log evidence strongly suggest this sequence:

```text
client tick RETURN publishes its boundary
-> GuiClientTickSerialState.endTick() clears the active serial
-> setScreen TAIL publishes ScreenOpenEvent before the next tick HEAD
-> GuiContainerEventHub.onScreen() sees no active serial and returns
-> the exact gate never receives the TAIL candidate
```

The hub's early return is currently silent, and the TAIL source record uses the
VERBOSE-only `ChatClefDiagnostics.logEvent` path. Therefore the sequence above
is a `LEADING_SOURCE_AND_LOG_HYPOTHESIS`, not a directly logged fact. The
first diagnostic goal is to confirm or reject it in one bounded reproduction.

The LAVI-side natural completion recorded about 342.55 seconds later for the
root `CraftInTableTask` is a separate command-lifecycle question. It is not
evidence that furnace input/fuel transfer or iron smelting succeeded, and it
must not be fixed or reclassified inside the exact-GUI logging patch.

### 18.2 Required Observation Chain

The diagnostics-only patch must make this synchronous chain visible in
BOUNDARY mode:

```text
EXACT_GUI_SCREEN_TAIL_SOURCE_OBSERVED
-> EXACT_GUI_SCREEN_TAIL_HUB_DECISION
-> EXACT_GUI_SCREEN_TAIL_GATE_INTAKE, when fan-out reaches the gate
-> existing EXACT_GUI_SCREEN_TAIL_CANDIDATE or typed EXACT_GUI_INVALIDATED,
   only when the coordinator produces that lifecycle result
-> EXACT_GUI_SCREEN_TAIL_GATE_DECISION, when the complete gate path returns normally
-> EXACT_GUI_SCREEN_TAIL_HUB_DISPATCH_COMPLETED, when the fan-out loop returns normally
```

In `BOUNDARY` mode, every valid `ScreenOpenEvent` reaching the TAIL source is
eligible for the source observation subject to the uncorrelated screen-activation
budget. Source logging must not require or fabricate an operation, attempt,
correlation, or Task identity. This is observation only: it must not retain,
queue, replay, or backfill an event that downstream code sees between active
client-tick serial windows.

Responsibilities are split as follows:

| Boundary | Existing owner | Required diagnostics-only responsibility |
| --- | --- | --- |
| TAIL producer | `ClientOpenScreenMixin.onScreenOpenEnd` | One minimal upstream-derived observer call immediately before the existing `EventBus.publish`; no ordering or event-payload change. |
| Event intake/fan-out | `GuiContainerEventHub.onScreen` | Record one pre-dispatch decision per receiving hub instance with registered/snapshot-eligible counts. During fan-out invoke a snapshotted registration only if it remains active at its turn; record actual completed and inactive-after-snapshot counts. Record completion only when the loop returns normally; never claim completed dispatch before fan-out. |
| Attempt intake/outcome | `ExactContainerGuiGate.onScreen` and its existing log adapter | Record intake before its no-active-attempt guard/coordinator call. Record a decision only after normal return, preserving the raw event snapshot and exact gate/coordinator outcome. |
| Correlation predicate | `ExactContainerGuiEventCoordinator.observeScreen` | Remain behavior-focused and logger-free; return its existing typed result to the gate. |
| Existing gate lifecycle logs | `ExactContainerGuiGateDiagnostics` | Join the same operation-scoped 256-detail budget; when touched, become a delegation facade rather than retaining snapshot collection, field formatting, and emission in one file. |
| Screen diagnostics pipeline | New narrow LAVI-owned collaborators under `lavi/minecraft/diagnostics/container/gui/screen/` | Keep snapshot collection, fingerprinting/deduplication, local budgets, aggregation, formatting, emission, and orchestration in focused units. |

The required LAVI-owned structure is responsibility-separated. One
orchestration facade may delegate the pipeline but owns no formatting,
admission, aggregate, or gameplay state:

```text
src/main/java/lavi/minecraft/diagnostics/container/gui/screen/
    ExactContainerGuiScreenEventDiagnostics.java
        stateless diagnostic pipeline orchestration/delegation only

    model/ExactContainerGuiScreenEventSnapshot.java
        immutable bounded observation data only
    model/ExactContainerGuiGateEntryContextSnapshot.java
        immutable method-local gate-entry correlation identity and phase only
    model/ExactContainerGuiScreenEventStage.java
        canonical stage values only
    model/ExactContainerGuiScreenEventDisposition.java
        canonical disposition values only
    model/ExactContainerGuiScreenEventDropReason.java
        canonical hub drop reason values only
    model/ExactContainerGuiScreenGateOutcome.java
        canonical gate result values only
    model/ExactContainerGuiScreenCoordinatorOutcome.java
        canonical coordinator result values only
    model/ExactContainerGuiClientTickWindowState.java
        canonical diagnostic tick-window values only
    model/ExactContainerGuiDiagnosticEvidenceCompleteness.java
        canonical complete/partial evidence values only
    model/ExactContainerGuiDiagnosticEvidenceGaps.java
        immutable maximum-five gap set and deterministic primary projection only
    model/ExactContainerGuiDiagnosticBudgetScope.java
        canonical local-budget ownership/exemption values only
    model/ExactContainerGuiDiagnosticFlushReason.java
        canonical mode-off versus clean-final-snapshot projection values only
    model/ExactContainerGuiDiagnosticEmissionResult.java
        typed non-throwing local/shared diagnostic dispatch result only

    runtime/ExactContainerGuiScreenDiagnosticRuntime.java
        one diagnostic-session-owned composition owner for the current
        BOUNDARY activation identity, uncorrelated budget, and activation aggregate

    snapshot/ExactContainerGuiScreenSnapshotCollector.java
        side-effect-free source/hub/gate snapshot collection only

    fingerprint/ExactContainerGuiScreenEventFingerprint.java
        stable semantic fingerprint construction only

    limiting/ExactContainerGuiOperationEventDeduplicator.java
        one operation/activation segment's duplicate/repeat admission only
    limiting/ExactContainerGuiScreenActivationEventDeduplicator.java
        one BOUNDARY activation's uncorrelated duplicate/repeat admission only
    limiting/ExactContainerGuiOperationDetailBudget.java
        one operation-wide 256-detail allowance across retries only
    limiting/ExactContainerGuiScreenActivationDetailBudget.java
        one continuous-BOUNDARY-activation-wide 256-detail allowance for
        uncorrelated source/hub/gate records only

    aggregation/ExactContainerGuiOperationDiagnosticAggregate.java
        gate-owned operation/activation-segment counters across retries plus
        cleanup-only diagnostic lifecycle observation
    aggregation/ExactContainerGuiScreenActivationAggregate.java
        operation-free source/hub/gate activation counters and lifecycle projection only

    formatting/ExactContainerGuiScreenEventFieldFormatter.java
        canonical bounded field projection only

    emission/ExactContainerGuiScreenEventEmitter.java
        existing shared-session admission/emission call and result projection only
```

`ChatClefDiagnostics` owns one
`ExactContainerGuiScreenDiagnosticRuntime` alongside its existing production
diagnostic session. The source, hub, and gate diagnostic facades delegate to
that same runtime, so one BOUNDARY activation cannot accidentally receive
multiple IDs or uncorrelated budgets. The runtime stores no Task, operation,
attempt, target, retry, input, screen mutation, or Baritone behavior state and
is never consulted for behavior decisions. Per-operation aggregates remain
composed into their owning `ExactContainerGuiGate`; they are not moved into this
session-level runtime.

The screen runtime implements `DiagnosticSessionLifecycleObserver`.
`ChatClefDiagnostics` constructs it and calls the same non-throwing
`tryRegister` exactly once during static diagnostic-session composition, before
the source/hub/gate facades are exposed. The successful registration persists
across OFF/BOUNDARY toggles while only its internal activation state is cleared
on OFF; `finalSnapshotFields()` and
`afterCleanTeardownSnapshotAttempt(...)` own clean-teardown projection and
cleanup, and `ChatClefDiagnostics` owns idempotent release with the diagnostic
session. It is not registered once per activation. If initial registration
cannot be established, the runtime remains disabled and retains no exact-screen
diagnostic state; source/hub/gate behavior and gameplay remain unchanged.
Capacity failure is a diagnostics-availability failure, never a reason to relax
the gate or abort Minecraft class initialization.

The existing
`src/main/java/lavi/minecraft/diagnostics/container/gui/ExactContainerGuiGateDiagnostics.java`
keeps its current public call surface as a compatibility facade if needed, but
after the operation-budget integration it may coordinate only. Snapshot
collection, canonical field projection, local admission, aggregate mutation,
and physical emission must live in the focused units above.

Strict OFF cleanup also requires a non-throwing `tryRegister` path and
idempotent unregister support in the existing
LAVI-owned diagnostic lifecycle path:

```text
src/main/java/lavi/minecraft/diagnostics/session/lifecycle/registration/DiagnosticSessionObserverRegistry.java
src/main/java/lavi/minecraft/diagnostics/session/lifecycle/DiagnosticSessionLifecycleRegistry.java
src/main/java/lavi/minecraft/diagnostics/ChatClefDiagnostics.java
```

The operation aggregate registers as a cleanup-only
`DiagnosticSessionLifecycleObserver` only while its BOUNDARY activation is
active and unregisters at owner stop, mode OFF, or clean teardown. Existing
registration behavior remains unchanged for existing callers. The exact-GUI
path retains an operation aggregate only when `tryRegister` succeeds; a bounded
capacity rejection is counted in the screen activation aggregate as
`operationAggregateRegistrationRejectedCount` with
the `PARTIAL_LIFECYCLE_OBSERVER_CAPACITY` gap reason, and must not throw into or
alter gameplay behavior. These minimal existing-type extensions own observer registration
lifetime only; they must not expose registered observers to the GUI hub or turn
the registry into an active-operation/correlation lookup.

The EventBus subscription's logical delivery lifetime must also be deterministic
before the new screen tests publish events. Add the focused LAVI-owned
`src/main/java/lavi/minecraft/task/container/gui/event/subscription/GuiContainerEventBusSubscription.java`.
It owns exactly the returned `BlockInteractEvent` and `ScreenOpenEvent`
`Subscription` handles and one idempotent `close()` that calls
`EventBus.unsubscribe` for both. `GuiContainerEventHub` retains listener
registration and synchronous fan-out only; `ClientTickMixin` remains the
client-instance composition root and retains one hub plus one subscription
owner for the `MinecraftClient` lifetime. Tests close every created
subscription owner in `finally`/`@AfterEach` and use one JUnit resource lock
for the static EventBus. Do not add a broad global `EventBus.clear/reset` test
API. Current `EventBus.unsubscribe` marks a subscription deleted but does not
prove immediate physical removal from the static topic storage. The contract is
therefore no callback/logical delivery and no stale duplicate callback after
close, not reference reclamation; tests must not claim stronger removal.

If serial internals cannot be observed through the existing read-only surface,
add only a read-only diagnostic snapshot seam beside
`GuiClientTickSerialState`. It may expose active/open/claimed and last-issued
state, but it must not open, close, claim, backfill, retain, or predict a serial.

The Java diagnostic block must retain the project-required diagnostic
marker. The minimal upstream-derived `ClientOpenScreenMixin` hunk must also
retain the engine-divergence marker at the exact observation seam:

```java
//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
//20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
```

The marker wording is preserved as required project policy; it does not
reclassify this post-GUI furnace incident as proven Carry On interception.

The canonical fields, value meanings, fingerprint exclusions, final-checkpoint schemas,
and decision tree are owned by
[ChatClef Task Lifecycle Diagnostics](chatclef-task-lifecycle-diagnostics.md#screen-tail-source-to-hub-to-coordinator-diagnostic-contract).

### 18.3 Behavior-Preservation Gate

During this first diagnostics-only implementation, preserve all current
behavior, including the suspected failure. Specifically, do not:

```text
retain, queue, replay, or defer ScreenOpenEvent
extend or move the active client-tick serial window
copy the previous or next serial into the current event
change EventBus publication or listener fan-out order
change gate acceptance, GUI_BOUND promotion, or three-boundary counting
change retry count, timeout duration/reason, Task selection, or cleanup
create permission or enter transfer lifecycle
call clickSlot, interactBlock, setScreen, or any input API for diagnostics
change Baritone goal, path, or ownership state
add a try/catch that suppresses an exception from the observed callback path
```

The source record must use the bounded BOUNDARY emitter. The existing
`SCREEN/TAIL/setScreen_end` call is VERBOSE-only and its absence from this
capture is not evidence that the TAIL callback failed to run.

### 18.4 Boundedness And Evidence Completeness

Reuse the shared diagnostic session; do not create an independent session cap:

```text
mode for the controlled reproduction: BOUNDARY
per-event UTF-8 payload cap: 8192 bytes
correlated exact-GUI detail cap: 256 total per operation within one continuous
    BOUNDARY activation across existing exact-gate lifecycle detail and new
    gate intake/decision detail; retry does not reset it within that activation
uncorrelated exact-screen detail cap: 256 total per continuous BOUNDARY activation
    across all source/hub detail plus gate intake/decision records without
    operation context
unchanged repeat summary: at most every 200 game ticks or about 10 seconds
bounded sorted reason/fingerprint buckets: at most 32 plus omittedCount
shared hard cap: 5000
shared ordinary ceiling: 4936
shared partitioned critical reserve: 64
    aggregate checkpoint slots: 8
    non-store terminal slots: 8
    suppression-control slots: 6
```

Stage, disposition, screen/handler class, active-serial presence/window state,
and typed reason form the semantic fingerprint. Tick/time, serial, syncId,
open-attempt/correlation UUID, event identity, and object identity remain
payload-only. Give each continuous BOUNDARY activation a diagnostic-only
`diagnosticBoundaryActivationId`. OFF mode performs no investigation emission
or retained diagnostic bookkeeping, so OFF ends the local activation and clears
its budgets/aggregates even though the production `diagnosticSessionId` and
shared admission authority remain. A later BOUNDARY toggle creates a new local
activation lazily on its first eligible observation; the activation ID never
survives OFF. Diagnostic formatting/emission failure must not change the observed
callback's return, fan-out, or exception behavior.

Correlated and uncorrelated stateful limiting is separate. Each gate-owned
operation/activation aggregate owns its correlated dedupe state and 200-tick
summary schedule across retries. The shared screen runtime owns a different
dedupe state and summary schedule for uncorrelated source/hub/gate detail in
one activation. Neither may suppress another operation or cross OFF. Each
operation segment owns its own checkpoint emit-once flag; the screen activation
aggregate owns the shared emit-once choice between its mode-OFF checkpoint and
clean-teardown projection.

The diagnostic processing order is fixed:

```text
update diagnostic-only observation counters
-> semantic dedupe
-> operation-local 256-detail budget when operation context exists,
   otherwise uncorrelated activation-local 256-detail budget
-> existing shared-session admission
-> physical-emission result accounting
```

Final-checkpoint and suppression-summary requests do not consume the local 256
detail allowance, but they remain subject to their classified shared subquota.
Within one continuous activation, the operation final checkpoint makes at most
one admission request per operation/activation segment; it does not promise
physical emission after the aggregate-checkpoint quota is exhausted. Operation
aggregate state begins only while BOUNDARY is active, remains across retries in
that activation, and normally flushes once at owner stop before listener
unregister. Its payload may report
`finalCheckpointEmissionAttempted=true`. Admission/completion is known only
after the emitter returns and is recorded in returned/test projection or later
activation/session counters; never backfill those outcomes into the submitted
checkpoint. The physical checkpoint record's presence is itself completion
evidence. Account for the result, then purge and unregister the aggregate.

The operation checkpoint is diagnostics-only. `beginOwnerStop()` calls it
through a non-throwing exact-GUI emission wrapper and uses cleanup `finally`
blocks so aggregate purge, observer unregister, the existing
`operationLifecycle.stop`, and GUI subscription unregister still execute if
local field formatting or diagnostic dispatch fails. Convert
`RuntimeException`/`LinkageError` to the typed diagnostic-only emission result;
do not recursively log that failure or rethrow it into the Task lifecycle.
This does not catch or alter an exception from the observed GUI listener or
coordinator callback.

Each live operation aggregate is a cleanup-only
`DiagnosticSessionLifecycleObserver`. On `beforeModeOff()` it clears and
unregisters its own diagnostic state; on clean teardown it does the same from
`afterCleanTeardownSnapshotAttempt(...)`. If a still-running gameplay operation
is observed in a later BOUNDARY activation, create a new segment and report
the `PARTIAL_MODE_TRANSITION` gap reason, then derive the primary completeness
value by the canonical precedence. Do not retain a join key through OFF. The
bounded lifecycle registry is cleanup infrastructure only
and must not be queried by the hub as an active-operation registry.

Hub-level drops may lack safe operation context. Do not introduce a global
active-operation registry to fill that gap. Keep uncorrelated source/hub/gate
counts in an instance-owned, operation-free activation aggregate. When that
screen evidence cannot be tied to exactly one operation, add
`PARTIAL_UNCORRELATED_HUB_EVENT` to that screen aggregate's
`diagnosticEvidenceGapReasons`, preserve its other gaps, and derive its primary
completeness value by the canonical precedence. Do not project the gap into a
per-operation checkpoint from timing or adjacency, fabricate operation linkage,
or retain a Task reference or operation ID in the screen aggregate.

For this diagnostic contract, capture one immutable method-local gate-entry
snapshot before intake logging, the active-attempt guard, or coordinator call.
Gate operation context is proven only when that entry snapshot has an active
attempt and its `operationId` matches the entry-time live gate lifecycle's
`operationId`; merely having started the lifecycle is insufficient. Intake and
normal-return decision reuse the same snapshot and never recompute context from
post-coordinator state. Thus `gateOutcome=NO_ACTIVE_ATTEMPT` always carries
`operationContextAvailable=false`, exposes no operation/attempt/correlation
identity, and uses the uncorrelated activation budget and aggregate. Conversely,
a coordinator `RETURNED_INVALIDATE` that entered with proven context remains in
the same operation segment and increments its rejected count even though live
attempt state changed before decision logging. Discard the snapshot on callback
return; it is not retained or replayed.

The two flush paths share one emit-once/flush state but remain distinct:

```text
BOUNDARY -> OFF:
    beforeModeOff uses try/finally
    try: request one named EXACT_GUI_SCREEN_SESSION_FINAL_CHECKPOINT with
         diagnosticFlushReason=MODE_OFF,
         diagnosticBudgetScope=LOCAL_DETAIL_EXEMPT_AGGREGATE_CHECKPOINT,
         and finalCheckpointEmissionAttempted=true
         account for the returned dispatch result when it returns
    finally: clear activation aggregate, budget, dedupe, summary, and emit-once state

clean teardown while BOUNDARY:
    do not initiate a nested named diagnostic dispatch
    contribute aggregate fields through finalSnapshotFields to the existing
        DIAGNOSTIC_SESSION_FINAL_SNAPSHOT
    diagnosticFlushReason=CLEAN_TEARDOWN_FINAL_SNAPSHOT and do not assert
        finalCheckpointEmissionAttempted
    diagnosticBudgetScope=LOCAL_DETAIL_EXEMPT_FINAL_SNAPSHOT_PROJECTION
    clear in afterCleanTeardownSnapshotAttempt regardless of the outcome
```

The paths must not request duplicate checkpoints. Their exact activation-only
field set is canonicalized in Task Lifecycle Diagnostics and excludes
operation, attempt, correlation, Task/owner, retry, target, and permission
identity/state.

Operation checkpoints use the exact envelope and aggregate field set in Task
Lifecycle Diagnostics. In particular, the envelope includes diagnostic session,
BOUNDARY activation, operation, event tick/thread, route owner, the
`NOT_APPLICABLE` flush reason, and
`LOCAL_DETAIL_EXEMPT_AGGREGATE_CHECKPOINT`; it omits attempt/correlation and
retry-specific identity because the aggregate spans retries.

Evidence completeness is multi-cause: retain the exact sorted set of all
applicable partial reasons in `diagnosticEvidenceGapReasons`, and select the
single `diagnosticEvidenceCompleteness` primary value by the canonical fixed
precedence in Task Lifecycle Diagnostics. Never overwrite one gap with another.

Run the eventual controlled reproduction in a fresh production diagnostic
session and stop after the first complete attempt/first timeout evidence
sequence. “Fresh” requires complete Minecraft JVM shutdown and relaunch;
OFF→BOUNDARY toggling changes mode and starts a new local activation, but does
not replace the production session's shared admission authority or counters.
Confirm BOUNDARY startup and a `diagnosticSessionId` different
from the exhausted capture's
`chatclef-diagnostic-b73c6c67-c00a-4d88-8e2f-1e43b1ab400f` before accepting
new evidence. `replaceOffSessionForTests` or any equivalent test-only reset is
forbidden in live reproduction. If artifact equality is false, BOUNDARY mode
is not active, the session ID is not fresh, or a cap/ordinary-ceiling event
appears before the required source/hub/gate sequence, classify the reproduction
`INCONCLUSIVE` and do not infer a behavior fix from it.

### 18.5 Diagnostics-Only Test Plan

These tests are required when the source logging patch is implemented. They
were not created or run by this documentation-only update.

The current `GuiContainerEventHubTest` exercises only client RETURN-boundary
fan-out and never drives `ScreenOpenEvent` through `onScreen`. The current
`GuiClientTickSerialStateTest` explicitly proves that `currentActiveSerial()`
is empty after `endTick()`. Therefore only the serial-window half is currently
test-proven. The hub's silent screen-event return is source-inspected but has no
existing event-delivery test joining it to an inter-tick TAIL.

Exact planned test files for the diagnostics implementation are:

```text
modify:
  src/test/java/lavi/minecraft/task/container/gui/event/GuiContainerEventHubTest.java
  src/test/java/lavi/minecraft/task/container/gui/tick/ClientTickMixinGuiBoundaryOrderTest.java

create:
  src/test/java/lavi/minecraft/task/container/gui/event/subscription/GuiContainerEventBusSubscriptionTest.java
  src/test/java/lavi/minecraft/diagnostics/session/lifecycle/registration/DiagnosticSessionObserverRegistryTest.java
  src/test/java/lavi/minecraft/diagnostics/container/gui/screen/ExactContainerGuiScreenEventContractTest.java
  src/test/java/lavi/minecraft/diagnostics/container/gui/screen/ExactContainerGuiScreenEventBudgetTest.java
  src/test/java/lavi/minecraft/diagnostics/container/gui/screen/ExactContainerGuiOperationFinalCheckpointTest.java
  src/test/java/lavi/minecraft/task/container/gui/gate/ExactContainerGuiScreenDiagnosticIntegrationTest.java
```

Every test that attaches the static EventBus binding uses the same JUnit
`@ResourceLock("chatclef-static-event-bus")` and closes the focused subscription
owner in `finally` or `@AfterEach`. Tests must not add or call a global
EventBus reset/clear seam.

| Test boundary | Required assertion |
| --- | --- |
| TAIL source order | The bounded source probe observes the same `ScreenOpenEvent` immediately before existing publication; HEAD/TAIL publication behavior is unchanged. |
| Active-tick hub delivery | An active-serial TAIL records `screenEventDisposition=DISPATCH_STARTED`; each snapshotted registration still active at its turn is invoked in current order, gate intake/lifecycle result/decision retain their real order, and hub completion appears only when the fan-out loop returns normally. |
| Inter-tick hub drop | After `endTick()` and before the next `beginTick()`, the event still reaches zero listeners in this diagnostics-only patch and records `screenEventDisposition=DROPPED` with `screenEventDropReason=ACTIVE_CLIENT_TICK_SERIAL_UNAVAILABLE` once per receiving hub instance. |
| Listener lifecycle | No-listener, register, duplicate-register, unregister, snapshot-time eligible count, actual normal-return completed count, inactive-after-snapshot skipped count, and dispositions are exact. |
| Gate intake and decision | Intake is visible before the no-active-attempt guard/coordinator call; a decision exists only after normal return. Both reuse one immutable entry snapshot. Accepted and rejected results retain its same auxiliary event and, when entry-proven, operation/attempt/correlation identity even if the coordinator invalidates live state before decision logging. |
| Rejected raw event | Exact screen, handler, player-handler, syncId, and typed rejection reason survive even though no binding snapshot is committed. |
| Serial honesty | Inter-tick records use unavailable/-1 current serial values and never backfill the previous or predict the next serial. |
| Bounded logger | OFF emits/retains no investigation state; BOUNDARY shows the first unique decision; dedupe, 200-tick summary, operation-per-activation and uncorrelated-activation 256-detail budgets, 8192-byte payload cap, sorted 32-bucket limit, shared admission, and partitioned reserve work as documented. `NO_ACTIVE_ATTEMPT` has no operation identity and uses the uncorrelated budget. |
| Activation composition | Source, hub, and uncorrelated gate records share one registered `ExactContainerGuiScreenDiagnosticRuntime`, activation ID, uncorrelated dedupe/summary, and uncorrelated budget. Every correlated operation segment owns separate dedupe/summary/checkpoint-once state and cannot be looked up through that runtime. |
| Mode lifecycle | BOUNDARY-to-OFF uses `try/finally`, emits at most one named screen-session checkpoint with the aggregate-checkpoint scope, and clears all activation state even if formatting/emission throws. Clean teardown embeds fields with the final-snapshot-projection scope in the existing global final snapshot without nested dispatch. A later activation of the same gameplay operation includes the `PARTIAL_MODE_TRANSITION` gap reason. |
| Final-checkpoint causality | The submitted operation checkpoint uses the canonical correlation envelope and aggregate fields; `finalCheckpointEmissionAttempted=true` is its only pre-call emission-result claim. Admission/completion are asserted from the emitter return or later aggregate counters and are never backfilled into the same event. |
| Operation-checkpoint failure isolation | Inject formatter and dispatch failures; the typed diagnostic result records failure, aggregate/observer cleanup and existing operation/listener cleanup still run exactly once, and no diagnostic exception reaches the Task lifecycle. |
| Evidence completeness | Simultaneous lifecycle-capacity, mode-transition, uncorrelated, local-cap, and shared-admission gaps remain in the exact sorted `diagnosticEvidenceGapReasons` set; the primary completeness field follows canonical deterministic precedence. |
| Lifecycle registry | Existing register semantics remain compatible; exact-GUI `tryRegister`, duplicate registration, bounded-capacity rejection, idempotent unregister, snapshot iteration, and unregister-during-notification are deterministic. Capacity rejection retains no operation aggregate, increments only the activation-level rejection count, and cannot affect gameplay; the hub cannot query the registry for operation correlation. |
| Callback exception preservation | A sentinel `IllegalStateException` is the same exception observed by the caller; later listeners are not called, gate intake may exist, and gate decision/hub completion do not fabricate normal return. Diagnostics ON/OFF preserve the same order and count. |
| Subscription isolation | Closing one subscription owner logically deactivates both EventBus subscriptions; sequential tests and hubs receive no stale duplicate callbacks. The test does not claim immediate physical removal from static topic storage. |
| Static mutation guard | New observer/snapshot/fingerprint/budget/aggregate/formatter/emitter code contains no slot, world interaction, screen mutation, input, Task scheduling, timeout/retry, goal, or path call. |

The existing 61 focused tests and full Fabric suite are regression inputs for a
next in-scope continuous diagnostics implementation, test, and required
clean-build workflow. Their prior passes do not
prove the new diagnostics, and this documentation-only update did not rerun
them.

### 18.6 One-Reproduction Decision Matrix

| First missing or negative boundary | Interpretation | Next scope |
| --- | --- | --- |
| Live furnace GUI, correct JAR/mode/no cap, but no source event | TAIL Mixin execution or source-probe/admission boundary | Inspect that boundary only; do not change gate behavior. |
| Source event but no hub decision | EventBus publication/subscription or client-instance hub construction | Inspect synchronous delivery and hub identity. |
| Hub records `screenEventDisposition=DROPPED`, `screenEventDropReason=ACTIVE_CLIENT_TICK_SERIAL_UNAVAILABLE`, and eligible listeners existed | Current inter-tick early-return hypothesis is directly confirmed | Only then design the smallest behavior fix for event/serial ownership. |
| Hub records `screenEventDisposition=DROPPED`, `screenEventDropReason=NO_ACTIVE_GUI_LISTENER` | Gate registration/unregistration lifetime or wrong client hub instance | Inspect owner/listener lifecycle. |
| Hub records `DISPATCH_STARTED` but no gate intake | Listener snapshot/order, wrong hub instance, or an earlier callback exception | Instrument no broader than that seam. |
| Gate intake exists but gate decision does not | Active-attempt guard/coordinator path threw before normal return | Preserve exception propagation and inspect only that boundary. |
| Gate decision has `gateOutcome=NO_ACTIVE_ATTEMPT` | No active attempt existed at gate receipt; the record does not encode whether this was before the first attempt, between retries, after invalidation/retirement/owner stop, or an unrelated TAIL | Inspect adjacent attempt create/activate/invalidate/retire and owner-lifecycle records; do not infer operation identity or blame slot transfer. |
| Gate decision has `coordinatorOutcome=RETURNED_INVALIDATE` | Existing typed correlation/binding reason is the first failing boundary | Use raw event fields and that reason; do not weaken predicates speculatively. |
| Gate decisions exist but hub completion does not while diagnostic admission remains available | A later invoked listener threw or fan-out did not return normally | Inspect listener order, inactive-after-snapshot skips, and the propagated exception; do not fabricate completion. |
| Candidate exists but `GUI_BOUND` does not | Open-child stop/quiescence/cleanup, RETURN publication, or live-binding promotion | Inspect those boundaries next. |
| `GUI_BOUND` exists but permission does not | Later-boundary ordering, duplicate suppression, or invalidation | Verify B, B+1, B+2, B+3. |
| Permission exists but transfer commit does not | Next normal Task evaluation or route precondition | Inspect furnace transfer entry. |
| Transfer commit exists but no slot request | Existing furnace child/slot-action owner | This is no longer a TAIL/gate failure. |
| Slot request exists but no inventory/container delta | Minecraft handler/server slot semantics | Only here investigate a click/action-contract failure. |

For regular furnace, the preserved slot contract is the existing `PICKUP`
button 0/1 flow. It is not trusted-home `QUICK_MOVE`, a physical Shift key, or
a second world right-click.

### 18.7 Evidence Requirement For In-Scope Behavior Correction

The diagnostics patch is complete when one bounded capture identifies the last
successful and first failing boundary without changing behavior. Direct
evidence is required before an exact behavior hunk/test design is recorded and
applied. When an implementation request is active, this evidence requirement
does not create a separate phase-approval pause; the exact correction, bounded
logging, focused tests, and required clean build continue in the same workflow.
If the hub reports the expected no-active-serial drop, the correction tests
must prove that an inter-tick TAIL is no longer lost without fabricating a tick
serial, while preserving active-tick delivery, exact correlation, candidate
event consumption, B/B+1/B+2/B+3 ordering, one-time permission, route-specific
slot contracts, excluded containers, Carry On absence, and generic interaction
behavior.

### 18.8 2026-09-04 Diagnostics Implementation And Runtime Result

The diagnostics-only follow-up was implemented and verified against review
baseline `46dd942ed504e6d0209c2fc57e6336c2e10ac950`. It adds bounded observation
of the existing screen TAIL source, synchronous EventBus transport, Task
evaluation and reconciliation, slot requests and local deltas, applied server
reconciliation, client tick boundaries, and root terminal state. It does not
create, activate, or repair an exact-container GUI gate.

Active-source and current-JAR inspection corrected an important historical
assumption in the earlier implementation checkpoint. The current Git tree and
the 2026-09-04 remapped JAR contain no `ExactContainerGuiGate`,
`GuiContainerEventHub`, `GuiClientTickSerialState`, `ExactContainerGuiOpenTask`,
or route integration for the documented gate. The earlier 8,132,655-byte
`A526C8...` artifact remains a historical transient artifact record, not the
identity of the current source or deployed runtime.

```text
CURRENT_DIAGNOSTIC_STATUS: BUILD_PASSED_RUNTIME_OBSERVED
CURRENT_ARTIFACT_STATUS: BUILD_AND_ACTIVE_INSTANCE_BYTE_IDENTICAL
CURRENT_BEHAVIOR_STATUS: EXACT_GUI_GATE_NOT_PRESENT_IN_ACTIVE_SOURCE_OR_CURRENT_JAR
DIVERGENCE_CLASSIFICATION: DIAGNOSTICS_ONLY
```

The canonical clean forced command completed successfully:

```bat
.\gradlew.bat clean build --rerun-tasks
```

```text
BUILD SUCCESSFUL
171 actionable tasks
171 executed
tests=630
failures=0
errors=0
skipped=1
new container-GUI diagnostic tests=31 passed
```

The current remapped build artifact and active instance artifact are
byte-identical:

```text
size: 8,013,409 bytes
modified: 2026-09-04 11:47:44.141 +09:00
SHA-256: EBA0552F33A18AE3A55E4BE5D8CE006E10308EA2A271F55BF2B585EB900B6D93
```

The live `LAVI_TEST_Fabric01` reproduction emitted the new source, transport,
slot, local-delta, terminal, and applied-server-reconciliation observations.
No relevant Mixin application or injection error occurred. Five LAVI commands
reached natural completion. The chest and regular-furnace screen observations
both reported the following honest absence of exact-gate context:

```text
operationId=unavailable
openAttemptId=unavailable
correlationId=unavailable
operationContextAvailable=false
activeAttemptPresent=false
gateOutcome=NOT_APPLICABLE
coordinatorOutcome=NOT_CALLED
targetScreenAssociationProven=false
```

No `EXACT_GUI_SCREEN_TAIL_CANDIDATE`, `EXACT_GUI_BOUND`, `GUI_STABILIZING`, or
`GUI_INPUT_ALLOWED` record was observed. Existing chest `QUICK_MOVE` and furnace
`PICKUP` slot work proceeded through the ungated route. Command completion is
therefore runtime proof of preserved legacy behavior, not proof of the
three-later-tick contract.

The ordinary admission ceiling of 4936 was reached and emitted one
`DIAGNOSTIC_SESSION_CAP_REACHED` marker. At that marker the admitted total was
4954, 46 critical-reserve admissions remained, and the 5000-event hard cap had
not been reached. Terminal summaries were emitted, but later ordinary detail is
partial. Correlation to the chest and furnace is explicitly heuristic and does
not fabricate operation, attempt, target-screen, or permission identity.

`ChatClefDiagnostics` still uses the existing throwing lifecycle
`register(...)` API. Registration succeeded in this run, but the planned
nonthrowing `tryRegister`/unregister contract is not implemented and is not
claimed as verified. Callback-exception preservation remains source-reviewed
rather than covered by a dedicated dynamic sentinel test. These are recorded
limitations of this investigation checkpoint, not hidden behavior changes.

At 15:41:16 a separate nonfatal
`BlockOptionalMeta.getManager -> drops -> getStackHashes` exception occurred
during Baritone builder recalculation. It is not attributed to the GUI
diagnostics, WebSocket bridge, or disk world cache. The command continued to
completion; a fresh Minecraft JVM is required before the next controlled
reproduction.

The exact engine observation hunks, ownership, behavior-preservation evidence,
boundedness limits, and hunk-scoped rollback order are recorded in
[ChatClef Engine Divergence Record](chatclef-engine-divergence-record.md#2026-09-04-bounded-container-gui-diagnostics-only-reinforcement).
