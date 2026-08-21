<!-- 20260821_kpopmodder: Added pre-change report for the sync-finish unchanged-idle-root Java bridge fix. -->
<!-- 20260821_openai: Reviewed v2; added dirty-worktree preservation, exact execute-call callback timing, non-atomic evidence limits, log-only payload ownership, deterministic test seams, and staged approval gates. -->
<!-- 20260821_openai: Reviewed v3 evidence boundary; verified reviewed-v5 plan bytes, corrected stale plan provenance, and kept edit approval on hold pending independent raw-evidence review. -->
<!-- 20260821_openai: Reviewed v4 package; verified raw-v2 manifest and baseline mechanics, identified missing companion/audit evidence, and kept edit-only approval on HOLD pending a narrow addendum. -->

# ChatClef Deposit Sync-Finish Idle Root Pre-Change Report - 2026-08-21

Date: 2026-08-21 KST

Status:

```text
report status: REVIEWED_CONDITIONAL_PASS
review revision: reviewed-v4-package-review
review input SHA-256: 2D9D4D3E5BCFC404A36CA92D9FE7D2440F0288635A7BCB6BF4850DB40819F288
design review: PASS
implementation plan alignment: PASS_BYTE_FOR_BYTE
raw-v2 manifest integrity: PASS_11_OF_11
source/test/document target baseline mechanics: PASS
cross-document provenance: PARTIAL_PASS
companion investigation exact bytes: MISSING_FROM_PACKAGE
repository/registered-command raw audit: MISSING_FROM_PACKAGE
source code change in this pass: none
test source change in this pass: none
Gradle/build/test execution in this pass: none
runtime/JAR/Minecraft execution in this pass: none
behavior implementation approval: HOLD_PENDING_EVIDENCE_ADDENDUM_AND_EXPLICIT_USER_APPROVAL
build/test execution approval: not granted by this document
runtime/JAR deployment approval: not granted by this document
next required action: add the exact companion investigation bytes, an exact-baseline repository/registered-command audit, and a manifest addendum; then request edit-only implementation approval
```

This report substantially closes the pre-implementation design package requested
by the reviewed-v5 implementation plan. The review corrections and pre-edit gates
below are normative. It does not edit Java, Python, tests, Gradle build files,
runtime jars, or Minecraft state.

## Review Evidence Boundary

This review independently inspected the attached v3 ZIP and all files under its
raw-v2 evidence directory. The verified package identity is:

```text
attached package ZIP SHA-256:
  EFD168B537C9476F85AA3556013D234F2214E3B604CD184087021D2730461951
implementation plan SHA-256:
  7AB95468FB3BEAFC1F522A782A8281D428DCE0DBCE1910F81939E5E24602D9D5
pre-change report review input SHA-256:
  2D9D4D3E5BCFC404A36CA92D9FE7D2440F0288635A7BCB6BF4850DB40819F288
pre-edit evidence summary review input SHA-256:
  6DB56F7E24AF339578FA0CE68B7D02ADA5BB50CD457B86926C04B24FB0516A1F
raw-v2 manifest SHA-256:
  5E20F0827916C059549E7E6178EC1BE01FEAA1287D127E2111F185C461785C20
```

The implementation plan is byte-for-byte identical to reviewed-v5 final. The
raw-v2 manifest contains 11 unique relative paths, and the raw-v2 directory
contains exactly those same 11 regular files:

```text
manifest rows: 11
raw-v2 regular files: 11
missing manifest targets: 0
unlisted raw-v2 files: 0
duplicate manifest paths: 0
byte-size mismatches: 0
SHA-256 mismatches: 0
```

The manifest is therefore a valid, independently checkable integrity boundary
for the supplied raw-v2 directory. The first evidence directory without the
`-v2` suffix remains superseded and is not mixed into this validation.

The source archive and documents continue to support the dispatch, callback,
bound-root, event-association, detach-cancellation, and fixed-fidelity design
analysis. Where the implementation plan uses the coarser
`BEFORE_DISPATCH_RETURN`/`AFTER_DISPATCH_RETURN` wording, the pre-change
report's executor-invocation marker and
`DURING_EXECUTOR_EXECUTE`/`AFTER_EXECUTOR_EXECUTE` distinction remain the
normative implementation refinement.

Two required evidence boundaries are not independently closed by this package:

```text
1. the exact investigation document bytes reported as
   49628CC0AF62602E8062E1AE75D6E49603F78CAADEC53BA99F8A4B4FC0BF66B2
   are not included in the ZIP

2. the raw-v2 directory has no exact-baseline repository/registered-command
   audit output containing capture time, repo/branch/HEAD command output,
   audited source hashes, command-registration search output, and the
   SetGammaCommand no-callback exclusion
```

The pre-change report contains a narrative registered-command audit, but that is
not the raw independently reproducible audit artifact previously required for
Approval unit A.

## Current Repository Baseline

```text
repository root: C:\Vtuber_Souorce_Code\LAVI
git toplevel: C:/Vtuber_Souorce_Code/LAVI
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: 332bc53bb86afa2efa0f973f845a55f870e2486f
worktree state: dirty
```

The worktree is not clean. Current dirty state includes existing modified
Minecraft docs, Python Fabric ChatClef files, LAVI-owned Java bridge files,
tests, generated resource files, untracked reconciliation tests, and untracked
Gradle cache folders. The implementation must not revert unrelated dirty files.

The currently dirty LAVI-owned Java bridge files relevant to this incident are:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandExecution.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandExecutionState.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandResultFactory.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandLifecycleCoordinator.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefTaskOwnershipSnapshot.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/runtime/FabricChatClefBridgeComponents.java
```

Any implementation must re-open these files at edit time and work with the
existing dirty content instead of overwriting or reverting it.

### Mandatory Dirty-Worktree Pre-Edit Snapshot

Because incident-relevant Java files are already dirty, branch/HEAD alone is not
a sufficient edit baseline. Before changing any file, Codex must record:

```text
full git status --short --untracked-files=all output
git diff --name-status output
tracked/untracked state for every planned existing file
SHA-256 of every planned existing file before editing
path-scoped git diff for every already-modified tracked file
byte copy or equivalent hash-backed baseline for every untracked file that will be edited
exact list of files that are dirty but intentionally excluded from the patch
```

After editing, the implementation report must provide the before/after SHA-256
and path-scoped diff for each touched existing file. It must prove that pre-existing
dirty hunks were preserved. Do not use `git checkout`, `git restore`, `git reset`,
`git clean`, or broad file replacement.

`FabricChatClefBridgeComponents.java` is currently reported dirty but is not in
the original proposed modification list. The implementation must either keep it
byte-identical or explicitly add a narrow object-graph wiring hunk and include its
pre-edit hash/diff. It must not be changed incidentally.

## Document Provenance

```text
implementation plan repo path:
  plugins/Minecraft/docs/chatclef-deposit-sync-finish-idle-root-implementation-plan-2026-08-21.md
implementation plan SHA-256 independently verified from this package:
  7AB95468FB3BEAFC1F522A782A8281D428DCE0DBCE1910F81939E5E24602D9D5
reviewed-v5 byte alignment:
  PASS

pre-change report package input SHA-256:
  2D9D4D3E5BCFC404A36CA92D9FE7D2440F0288635A7BCB6BF4850DB40819F288
pre-edit evidence summary package input SHA-256:
  6DB56F7E24AF339578FA0CE68B7D02ADA5BB50CD457B86926C04B24FB0516A1F
raw-v2 manifest SHA-256:
  5E20F0827916C059549E7E6178EC1BE01FEAA1287D127E2111F185C461785C20

investigation repo path reported by Codex:
  plugins/Minecraft/docs/chatclef-deposit-sync-finish-idle-root-investigation-2026-08-21.md
investigation SHA-256 reported by Codex:
  49628CC0AF62602E8062E1AE75D6E49603F78CAADEC53BA99F8A4B4FC0BF66B2
investigation exact bytes included in this package:
  no
```

The raw baseline table is internally useful for source preservation, but two
meta-document rows predate final package assembly:

```text
raw table pre-change report SHA-256:
  982C86B8E4C107CE131AF938984C1E5BF05D17DA686008717EB13A2ED9E0C88F
current package pre-change report SHA-256:
  2D9D4D3E5BCFC404A36CA92D9FE7D2440F0288635A7BCB6BF4850DB40819F288

raw table pre-edit summary SHA-256:
  F0138B3AC0D8EBB208AFDF8341D41328D1DADAA2422CF3087A2D13BF0471C58D
current package pre-edit summary SHA-256:
  6DB56F7E24AF339578FA0CE68B7D02ADA5BB50CD457B86926C04B24FB0516A1F
```

Those historical rows do not invalidate the captured Java, Gradle, test-target,
or four behavior-document baselines. They do mean the package does not yet form
one closed cross-document provenance graph. Before Approval unit B, a small
addendum must record the current report/summary hashes, include the exact
investigation file, and bind the missing raw audit artifact to the same
repo/branch/HEAD/capture timestamp. The original raw-v2 directory should remain
immutable; the addendum should supplement rather than silently rewrite it.

## Pre-Edit Evidence Package Review Boundary

### Independently verified as PASS

```text
raw-v2 manifest covers 11 of 11 raw files with exact size and SHA-256
full git status output is present
full git diff --name-status output is present
638 status entries are partitioned into 626 intentionally excluded entries and 12 incident-package entries
planned-existing-file baseline contains 22 paths
8 planned tracked files were already dirty
path-scoped diff payload contains exactly those same 8 real diff sections
planned-new-file baseline contains 15 paths and records each as absent
JUnit Jupiter is pinned to org.junit.jupiter:junit-jupiter:5.10.2
no Mockito or additional mocking framework is planned
FabricChatClefBridgeComponents.java is required to remain byte-identical
implementation plan alignment evidence matches the 7AB95468... plan
```

The raw status/exclusion partition and the planned-existing/path-scoped-diff
sets are internally consistent. This is sufficient to preserve the captured
source/test/document target baseline during a later edit-only pass, provided the
worktree is rechecked immediately before the first edit.

### Still required to complete Approval unit A

```text
exact investigation document bytes whose SHA-256 is 49628CC0...66B2
repository-and-registered-command-audit.txt (or equivalent), containing:
  capture date/time and timezone
  exact commands and complete outputs for repository root, branch, HEAD, and worktree state
  SHA-256 of AltoClefCommands.java and OverlayCommandRegistrar.java
  exact registered-command search/audit output
  explicit result for finish-then-async-root command shapes
  explicit SetGammaCommand no-callback exclusion
provenance/manifest addendum containing:
  current plan/report/summary/investigation hashes
  supplemental audit relative path, byte size, and SHA-256
  statement that raw-v2 remains immutable and authoritative for its original 11 files
```

The addendum may be narrow. A new causal-analysis or implementation-plan rewrite
is not required. Once these items are attached and their hashes match, Approval
unit A can pass without another wording-only design review.

## Current Failure Boundary Reconfirmed

The failing behavior remains:

```text
DepositCommand insufficient inventory branch calls finish(); return;
no new StoreInAnyContainerTask or UserTaskChain root is created for that branch
the LAVI Java bridge currently binds the post-dispatch current task as boundRootTask
the post-dispatch current task can be the pre-existing automatic IdleTask
the outcome classifier then waits for a TaskFinishedEvent that running idle does not publish
Java sends no terminal result
Python remains fail-closed because no exact terminal was accepted
```

This is a LAVI-owned Java bridge lifecycle ownership defect. It is not a
request to change `adris/**`, `TaskFinishedEvent` publication, Python
reconciliation mutation, retry/replay behavior, or persistent-idle terminal
policy.

## Current Source Observations

Current dispatch path:

```text
FabricChatClefCommandDispatcher.java:197
  creates FabricChatClefCommandExecution with captureCurrentTaskSnapshot()

FabricChatClefCommandDispatcher.java:224-229
  executor.execute(..., () -> markCommandFinish(execution, currentTaskOrNull()))

FabricChatClefCommandDispatcher.java:236
  markDispatchReturned(execution, currentTaskOrNull())
```

Current evidence capture issue:

```text
FabricChatClefTaskStateReader.java:15-29
  currentTaskOrNull() and captureCurrentTaskSnapshot() read UserTaskChain separately

FabricChatClefTaskStateReader.java:31-43
  runtimePayload() combines captureCurrentTaskSnapshot() and ownershipSnapshot()
  from separate reads

FabricChatClefTaskOwnershipSnapshotReader.java:22-34
  ownershipSnapshot() already exposes root, assignment id, generation,
  runningIdleTask, and nextTaskIdleFlag, but not as one shared raw-root evidence
  object with currentTaskOrNull()
```

Current callback issue:

```text
FabricChatClefCommandExecutionState.java:43-57
  markFinishCallbackReceived(...) sets finishCallbackReceived and terminalTask
  every time it is called

missing:
  first-callback-only BEFORE_DISPATCH_RETURN/AFTER_DISPATCH_RETURN latch
  duplicate callback audit count
  raw callback task reference
  callback client tick
  callback monotonic timestamp
  callback thread
```

Current bound root issue:

```text
FabricChatClefCommandExecutionState.java:65-69
  markDispatchReturned(...) always stores the candidate post-dispatch task as boundRootTask

FabricChatClefCommandOutcomeClassifier.java:15-16
  no boundRootTask enters callback_completed_without_user_task path

FabricChatClefCommandOutcomeClassifier.java:18-45
  boundRootTask enters TaskFinishedEvent waiting/matching path
```

Current event association issue:

```text
FabricChatClefCommandLifecycleCoordinator.java:222-241
  observeTaskTermination(...) attaches the TaskFinishedEvent to active execution
  before association is proven
```

Current detach cancellation consumer:

```text
FabricChatClefCommandDispatcher.java:109-123
  connection detach reads currentTask, asks lifecycleCoordinator for
  boundRootMatchReason/matchesBoundRootTask, and calls cancelUserTask() when true

FabricChatClefCommandDispatcher.java:175-182
  cancelUserTaskForDetachedCommand(...) invokes AltoClef.cancelUserTask()
```

Current result-fidelity issue:

```text
FabricChatClefCommandLifecyclePayloadMap.java:12-13
  RESULT_FIDELITY_VALUE = callback_plus_matching_user_task_event

FabricChatClefCommandLifecyclePayloadMap.java:52
  every lifecycle payload emits the fixed value

FabricChatClefCommandResultFactory.java:50-55
  unknownAfterFinish() uses result_reason=finish_callback_without_verified_success
```

## Registered Command Audit

Registered command roots inspected:

```text
AltoClefCommands.java:32-53
  GetCommand, EquipCommand, DepositCommand, GotoCommand, IdleCommand,
  HeroCommand, LocateStructureCommand, StopCommand, SetGammaCommand,
  FoodCommand, MeatCommand, ReloadSettingsCommand, ResetMemoryCommand,
  GamerCommand, FollowCommand, GiveCommand, ScanCommand,
  AttackPlayerOrMobCommand, SetAIBridgeEnabledCommand

OverlayCommandRegistrar.java:24-25
  OverlayCommand
```

Pre-change audit result against this baseline:

```text
no registered command shape was found that calls finish() and then later
asynchronously assigns a UserTaskChain root in the same command lifecycle

task-producing commands call runUserTask(..., this::finish) synchronously
immediate commands call finish() without scheduling a later root
DepositCommand insufficient inventory path calls finish(); return;
SetGammaCommand changes gamma but does not call finish()
OverlayCommand calls finish()
```

The no-callback shape represented by `SetGammaCommand` is explicitly out of
scope for the first patch. The implementation must not silently reclassify or
fix no-callback commands while fixing this synchronous-finish incident.

This audit is source-based. It must be repeated after any source drift before
behavior implementation.

## Proposed Minimal Hunks

### 1. Single-Root-Read Ownership Evidence Capture

Files:

```text
FabricChatClefTaskStateReader.java
FabricChatClefTaskOwnershipSnapshotReader.java
FabricChatClefTaskOwnershipEvidence.java
FabricChatClefTaskOwnershipSnapshot.java
FabricChatClefCommandDispatcher.java
FabricChatClefCommandExecution.java
FabricChatClefCommandExecutionState.java
```

Hunk:

```text
capture the UserTaskChain reference once
call UserTaskChain.getCurrentTask() exactly once per evidence capture
retain that raw Task reference in FabricChatClefTaskOwnershipEvidence
derive the task snapshot from that exact raw reference
read assignment id, generation, runningIdleTask, and nextTaskIdleFlag from the
same UserTaskChain reference in the same capture method and client tick
record client tick, monotonic time, wall-clock diagnostic time, and thread
add typed accessors needed by the classifier instead of classifying from toMap()
pass beforeEvidence into execution construction
close the executor.execute timing boundary before any after-dispatch read
capture afterEvidence and pass it into the ownership-classification step
do not use separate currentTaskOrNull()/ownershipSnapshot() pairs for ownership classification
```

This is a single-root-read best-effort snapshot, not an atomic engine transaction.
The diagnostic assignment/generation/flag getters are separate reads because
`adris/**` remains unchanged. Any unavailable or internally contradictory
combination must classify as `OWNERSHIP_UNKNOWN`; the document must not claim
that all engine fields were captured atomically.

### 2. Pure Root Ownership Classifier

Files:

```text
FabricChatClefRootOwnershipClassification.java
FabricChatClefRootOwnershipClassifier.java
FabricChatClefCommandExecutionState.java
```

Hunk:

```text
classify root ownership from before/after ownership evidence only
do not read finish callback state, TaskFinishedEvent state, elapsed time,
command name, StoreInAnyContainerTask logs, or user text

COMMAND_OWNED_ROOT:
  after root is non-null
  a valid new assignment is shown by a consistent assignment-id/generation transition
  the raw root may be new or may be the same object assigned again
  a newly assigned IdleTask may still be command-owned

PREEXISTING_UNCHANGED_IDLE_ROOT:
  same raw root object
  same root identity
  same assignment id
  same generation
  runningIdleTask true before and after
  nextTaskIdleFlag false before and after

NO_ROOT_VISIBLE:
  after root is null and the after evidence is otherwise available/consistent
  preserve the existing separately-audited no-root behavior

OWNERSHIP_UNKNOWN:
  unavailable or contradictory evidence
  object changed without a valid assignment/generation transition
  assignment/generation changed inconsistently
  nextTaskIdleFlag true/changed
  insufficient evidence
```

`no command-owned root is bound` remains a terminal-eligibility fact, not an
input to the ownership classifier.

### 3. First Finish-Callback Latch

Files:

```text
FabricChatClefFinishCallbackObservation.java
FabricChatClefCommandExecutionState.java
FabricChatClefCommandExecution.java
FabricChatClefCommandLifecycleCoordinator.java
FabricChatClefLifecycleDetailsPayload.java
```

Hunk:

```text
immediately before executor.execute(...):
  open an executor-execute invocation marker

in a finally boundary immediately when executor.execute(...) returns or throws:
  close the invocation marker before afterEvidence capture, logging, or other work

on first callback only:
  atomically compare-and-set the first observation
  classify it as DURING_EXECUTOR_EXECUTE or AFTER_EXECUTOR_EXECUTE from the marker
  retain raw callback task reference
  retain typed callback task snapshot
  retain client tick
  retain monotonic timestamp
  retain thread

on duplicate callbacks:
  increment an atomic duplicate count
  emit audit diagnostics
  do not rewrite the first timing/task/tick/time/thread evidence
  do not create a duplicate terminal
```

The incident UNKNOWN branch requires the first callback observation to be
`DURING_EXECUTOR_EXECUTE`. A callback arriving after `executor.execute(...)`
returns but before post-dispatch evidence is captured must be classified as
`AFTER_EXECUTOR_EXECUTE`; a later duplicate cannot upgrade it. Existing
`unknownAfterFinish(task)`-style helpers must not re-latch callback state while
constructing a result.

### 4. Dispatch Return Ownership Split

Files:

```text
FabricChatClefCommandDispatcher.java
FabricChatClefCommandLifecycleCoordinator.java
FabricChatClefCommandExecution.java
FabricChatClefCommandExecutionState.java
FabricChatClefCommandOutcomeClassifier.java
```

Hunk:

```text
closeExecutorExecuteInvocation(execution) in the immediate return/throw boundary
capture afterEvidence only after that timing boundary is closed
markDispatchEvidenceCaptured(execution, afterEvidence)
store observed post-dispatch root separately from boundRootTask
set boundRootTask only when root ownership classification is COMMAND_OWNED_ROOT
keep PREEXISTING_UNCHANGED_IDLE_ROOT with no boundRootTask
do not route PREEXISTING_UNCHANGED_IDLE_ROOT through completedWithoutUserTask()
preserve command/dispatch exception terminal behavior
```

### 5. Stable Idle-Root Gate

Files:

```text
FabricChatClefPreexistingIdleRootStabilityGate.java
FabricChatClefCommandOutcomeClassifier.java
FabricChatClefCommandLifecycleCoordinator.java
```

Hunk:

```text
observe only END_CLIENT_TICK evidence
minimum distinct client tick ids: 3
minimum monotonic stable duration: 500 ms
maximum age of newest ownership evidence: 1000 ms
require nextTaskIdleFlag false and unchanged
accept tick id and monotonic time as explicit inputs so tests do not sleep
reset on active context change, event arrival, terminal send start,
root/assignment/generation change, runningIdle flag change,
nextTaskIdleFlag true/change, stale/unavailable evidence, or contradiction
```

These are named provisional race-buffer constants and require deterministic
boundary tests. The gate proves only stable automatic idle-root evidence over a
small delayed-assignment race buffer. It does not prove global engine quiescence
or gameplay success.

### 6. Incident-Only Event Association Gate

Files:

```text
FabricChatClefCommandLifecycleCoordinator.java
FabricChatClefCommandExecutionState.java
FabricChatClefLifecycleDetailsPayload.java
```

Hunk:

```text
before markTaskFinishedObservation(...):
  inspect active execution ownership classification
  if PREEXISTING_UNCHANGED_IDLE_ROOT and no boundRootTask:
    emit unbound/audit-only diagnostic
    reset stable idle-root window
    do not attach event to execution
    do not use it as success/failure/mismatch/cleanup/admission evidence

if an observation was already attached before ownership classification:
  the incident UNKNOWN branch is disqualified and remains fail-closed
  do not erase or reinterpret that observation in this patch

for COMMAND_OWNED_ROOT:
  preserve the existing exact-root matching path

for NO_ROOT_VISIBLE, OWNERSHIP_UNKNOWN, or pre-classification observations:
  do not claim a new general association policy in this incident patch
```

The broader retired-root/newer-active-command ledger is excluded from the first
patch and requires separate approval. This patch may claim only the incident
state in which classification is complete, no command-owned root exists, and no
TaskFinishedEvent has been attached to the execution.

### 7. Detach Cancellation Consumer Preservation

Files:

```text
FabricChatClefCommandDispatcher.java
FabricChatClefCommandLifecycleCoordinator.java
FabricChatClefCommandExecution.java
FabricChatClefCommandExecutionState.java
```

Hunk:

```text
audit all consumers of:
  boundRootTask
  hasBoundRootTask
  matchesBoundRootTask
  boundRootMatchReason

PREEXISTING_UNCHANGED_IDLE_ROOT:
  has no command-owned boundRootTask
  connection detach must not cancel the pre-existing IdleTask

COMMAND_OWNED_ROOT:
  existing exact-root detach cancellation behavior is preserved
```

### 8. Result Reason And Typed Fidelity

Files:

```text
FabricChatClefCommandResultFidelity.java
FabricChatClefCommandResultFactory.java
FabricChatClefCommandDiagnosticPayload.java
FabricChatClefCommandLifecyclePayload.java
FabricChatClefCommandLifecyclePayloadMap.java
```

Hunk:

```text
add unknownFromPreexistingUnchangedIdleRoot()
emit result_reason=finish_callback_without_new_command_owned_root
emit result_fidelity=callback_without_matching_user_task_event
remove fixed RESULT_FIDELITY_VALUE serializer
thread typed fidelity from factory to payload map for every result factory path
```

First-patch fidelity matrix:

```text
dispatch_started -> dispatch_started_only
finish_callback_without_new_command_owned_root -> callback_without_matching_user_task_event
callback_completed_without_user_task -> callback_without_user_task
matching_task_finished / matching_task_stopped / task_observation_unclassified -> callback_plus_matching_user_task_event
task_identity_mismatch -> callback_plus_nonmatching_user_task_event
command_exception -> command_exception_observed
dispatch_exception -> dispatch_exception_observed
deadline_exceeded -> deadline_without_verified_terminal
unmapped diagnostic/duplicate path -> unknown
```

### 9. Structured Log-Only Diagnostics

Log-only owner files:

```text
FabricChatClefLifecycleDetailsPayload.java
one or more focused lifecycle/details/*DetailsPayload.java types
matching lifecycle/details/payload/*DetailsPayloadMap.java serializers
FabricChatClefCommandLifecycleCoordinator.java
FabricChatClefCommandDispatcher.java for detach diagnostics
```

Fields:

```text
root_ownership_classification
finish_callback_first_observation
finish_callback_duplicate_count
stable_idle_root_observation.qualified
stable_idle_root_observation.distinct_tick_count
stable_idle_root_observation.stable_duration_ms
stable_idle_root_observation.reset_reason
task_finished_event_association
bound_root_ownership_for_detach
detach_cancel_action
```

These fields must live under the existing diagnostic log `details` boundary.
`FabricChatClefCommandDiagnosticPayload`,
`FabricChatClefCommandLifecyclePayload`, and
`FabricChatClefCommandLifecyclePayloadMap` are command-result data owners; adding
the structured fields there would expand `command_result.data` and would not be
log-only. Those result-data classes may change for typed `result_fidelity`, but
must not receive the fields above without separate payload-shape approval.

## Files Proposed For Modification

Java source:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandDispatcher.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/diagnostics/FabricChatClefTaskStateReader.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/diagnostics/FabricChatClefTaskOwnershipSnapshotReader.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandExecution.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandExecutionState.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandResultFactory.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandDiagnosticPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandLifecycleCoordinator.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandOutcomeClassifier.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandLifecyclePayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefLifecycleDetailsPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/payload/FabricChatClefCommandLifecyclePayloadMap.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefTaskOwnershipSnapshot.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/runtime/FabricChatClefBridgeComponents.java (conditional: only if production wiring must change)
```

New Java helper files:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefRootOwnershipClassification.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefRootOwnershipClassifier.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefPreexistingIdleRootStabilityGate.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandResultFidelity.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefTaskOwnershipEvidence.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefFinishCallbackObservation.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/details/FabricChatClefPreexistingIdleRootDetailsPayload.java (or an equally focused existing-details extension)
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/details/payload/FabricChatClefPreexistingIdleRootDetailsPayloadMap.java (if a new details type is used)
```

Explicitly not proposed:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/**
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefRetiredCommandRootLedger.java
Python reconciliation mutation files
```

Documentation files to update with the behavior patch:

```text
plugins/Minecraft/docs/fabric-chatclef-bridge-protocol-v1.md
plugins/Minecraft/docs/chatclef-command-lifecycle-and-threading.md
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
plugins/Minecraft/docs/chatclef-command-payload-map-audit.md
```

## Executable Test Strategy

Selected strategy:

```text
Option A: scoped JUnit 5 tests inside the Fabric ChatClef 1.20.1 Gradle project
```

Reason:

```text
state transitions, duplicate callbacks, event association, detach cancellation,
send failure, and fidelity mapping cannot be proven by static source-string
tests alone
```

Infrastructure hunk requiring approval:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/build.gradle
  pin an exact JUnit Jupiter dependency coordinate/version
  configure Test tasks to useJUnitPlatform()
  do not add Mockito or another mocking framework unless separately justified
```

The exact JUnit coordinate/version must be recorded before editing
`build.gradle`; `JUnit 5` by itself is not an exact hunk. Editing test sources and
running Gradle tests are separate approval units.

Proposed test root:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/
```

Required test classes:

```text
FabricChatClefRootOwnershipClassifierTest
FabricChatClefFinishCallbackObservationTest
FabricChatClefPreexistingIdleRootStabilityGateTest
FabricChatClefCommandOutcomeClassifierTest
FabricChatClefCommandResultFidelityTest
FabricChatClefTaskFinishedEventAssociationTest
FabricChatClefBoundRootDetachOwnershipTest
```

Required executable cases:

```text
same raw IdleTask object + same assignment/generation + running idle + nextTaskIdleFlag false -> PREEXISTING_UNCHANGED_IDLE_ROOT
same class but different object without assignment transition -> OWNERSHIP_UNKNOWN
same object with a valid new assignment/generation -> COMMAND_OWNED_ROOT
partial or contradictory object/assignment/generation transition -> OWNERSHIP_UNKNOWN
new IdleTask assignment -> COMMAND_OWNED_ROOT candidate, not pre-existing merely because idle-shaped
unavailable evidence -> OWNERSHIP_UNKNOWN
nextTaskIdleFlag true or changed disqualifies/resets the incident branch
first callback DURING_EXECUTOR_EXECUTE remains authoritative after duplicate
first callback AFTER_EXECUTOR_EXECUTE cannot be upgraded by duplicate
callback in the return-to-afterEvidence gap is AFTER_EXECUTOR_EXECUTE
duplicate callbacks cannot overwrite first evidence or create duplicate terminals
three observations in one tick do not qualify stability
three distinct ticks before 500 ms do not qualify
three distinct ticks plus 500 ms qualify when newest evidence age <= 1000 ms
stale evidence or any documented contradiction resets the gate
unbound TaskFinishedEvent in pre-existing idle state is audit-only and not attached
pre-attached event disqualifies the incident UNKNOWN branch
pre-existing idle ownership does not trigger cancelUserTask on connection detach
real command-owned bound root preserves existing detach cancellation
qualified branch emits UNKNOWN, not COMPLETED
terminal send failure leaves Java active ownership uncleared
all result factory paths emit truthful result_fidelity
```

The pure classifier, callback observation, stability gate, and fidelity mapping
must be executable without launching Minecraft. Pass tick/time values into the
pure state objects rather than sleeping or calling the live client clock.
Coordinator/event/detach tests must use a small package-private decision seam or
fakes; they must not bootstrap Minecraft or call the real
`AltoClef.cancelUserTask()`. The single `UserTaskChain.getCurrentTask()` read in
the production reader may remain a source-contract guard unless an explicit
injectable reader seam is added; do not claim it as an executable test without
such a seam.

Existing Python source-contract tests may remain guardrails, but they are not
accepted as the primary proof for this behavior fix.

If JUnit dependency resolution or Fabric/Loom test wiring fails in the approved
implementation pass, stop and report the concrete blocker before switching to a
different harness strategy.

## Review Verdict

```text
incident diagnosis: PASS
LAVI-owned Java bridge ownership: PASS
command-agnostic classifier direction: PASS
UNKNOWN terminal and send-before-clear direction: PASS
Python reconciliation mutation exclusion: PASS
broader retired-root ledger exclusion: PASS
implementation plan alignment with reviewed-v5 final: PASS
raw-v2 manifest integrity: PASS
raw source/test/document target baseline mechanics: PASS
current report/summary versus raw meta-document hashes: PARTIAL_PASS_WITH_DOCUMENTED_SEQUENCE
companion investigation cross-document verification: HOLD_MISSING_EXACT_BYTES
exact-baseline registered-command audit verification: HOLD_MISSING_RAW_AUDIT
Approval unit A: PARTIAL_PASS_PENDING_TWO_SUPPLEMENTAL_FILES_AND_ADDENDUM
Approval unit B edit-only implementation: HOLD
Approval units C and D: HOLD_FOR_SEPARATE_APPROVAL
```

The remaining HOLD is evidentiary, not architectural. Do not create another
implementation-plan revision. Supply the narrow supplement, verify its manifest,
and then ask the user for Approval unit B.

## Approval Request

User approval should remain staged:

```text
Approval unit A - pre-edit evidence only:
  raw-v2 integrity and source-preservation mechanics: PASS
  exact companion investigation bytes: pending
  exact-baseline repository/registered-command raw audit: pending
  provenance/manifest addendum: pending
  no source edit

Approval unit B - edit only, only after unit A fully passes and the user approves:
  LAVI-owned Java bridge edits listed above
  exact pinned JUnit infrastructure and scoped Java test-source creation
  the four protocol/lifecycle/diagnostic documentation updates
  no test or Gradle execution

Approval unit C - verification execution, separately approved:
  run scoped executable tests and source-contract guardrails
  report exact commands and exit codes

Approval unit D - build/deploy/runtime, separately approved:
  Gradle build
  JAR copy
  Minecraft launch and incident reproduction
```

All units preserve these prohibitions:

```text
no adris/** behavior changes
no Python reconciliation mutation
no broad retired-root ledger
no timeout/retry/replay/StopCommand/cleanup behavior
no unapproved test or Gradle execution
no build, JAR copy, or Minecraft launch
no commit or push
```

Before Approval unit B, Codex should provide only the missing investigation file,
raw audit file, and hash/manifest addendum. If the live worktree has drifted from
the captured source baseline, stop and refresh the affected pre-edit hashes and
path-scoped diffs instead of adapting silently.
