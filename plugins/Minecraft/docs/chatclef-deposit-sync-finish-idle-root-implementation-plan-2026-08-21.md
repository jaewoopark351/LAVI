<!-- 20260821_kpopmodder: Added pre-implementation plan for the sync-finish unchanged-idle-root Java bridge fix. -->
<!-- 20260821_openai: Reviewed and tightened ownership, stability, fidelity, event-association, testability, and provenance contracts. -->
<!-- 20260821_openai: Reviewed-v4 narrowed first patch scope and separated new idle-root ownership from terminal policy. -->
<!-- 20260821_openai: Reviewed-v5 restored first-callback and bound-root-consumer contracts and narrowed the remaining HOLD to pre-change evidence and approval. -->

# ChatClef Deposit Sync-Finish Idle Root Implementation Plan - 2026-08-21

Date: 2026-08-21 KST

Status:

```text
plan status: REVIEWED_NOT_APPROVED
review revision: reviewed-v5
incident document: chatclef-deposit-sync-finish-idle-root-investigation-2026-08-21.md
source code change: not performed by this document
test change: not performed by this document
build/runtime reproduction: not performed by this document
behavior implementation approval: HOLD_PENDING_PRE_CHANGE_REPORT_AND_USER_APPROVAL
approval required before implementation: yes
```

This is the pre-implementation plan for the confirmed
`DepositCommand` synchronous-finish / unchanged `IdleTask` false-busy incident.
It is documentation only. It does not approve Java changes, Python changes,
tests, Gradle build, Minecraft launch, runtime reproduction, active-command
release, commit, or push.

## Review Baseline And Evidence Limits

This review compared the plan with the attached source archive:

```text
archive: LAVI-minecraft-plugin-fix-alto-clef-infinite-loop (37).zip
archive SHA-256: f48e8ec8e63e44c0cc30de437622bcb68853b1274d40620ebac6b5a5341beef8
original proposal SHA-256: eb142c775fb988e962e738fbc37b5a160e5535a6669532890218122954a4f39e
reviewed-v5 input plan SHA-256: b1b7ebd3b09901b25b1084b333042fe07efef1a0a7919d1917e211c6cfae3c1e
companion investigation SHA-256 reported by Codex: 49628cc0af62602e8062e1ae75d6e49603f78caadec53ba99f8a4b4fc0bf66b2
companion investigation exact bytes independently reviewed here: no
```

The reported companion hash is recorded, but the exact document bytes were not
attached or located in the available conversation/library files. Its contents
and two-document alignment are therefore not independently verified by this
review. The pre-change report must provide the exact repository file and hash.

The reviewed archive supports the incident's Java bridge misbinding diagnosis,
the current hard-coded `result_fidelity` serializer, the existing terminal outbox,
and the normal Python terminal ownership path.

The reviewed archive does not contain source or tests for the runtime strings
below:

```text
ELIGIBLE_DRY_RUN
would_reconcile_to_unknown
active_release_performed
dry_run_only_no_cas_release
stable_request_quiescence
root-chain-v1
2026-08-20.java-nonterminal-evidence.v1
```

It also does not contain the three reconciliation test paths named in the
original proposal. Those items may exist in a newer local baseline, but they
are not source-proven by the reviewed archive. They are explicitly out of scope
for this Java patch while Python reconciliation mutation remains disabled. The
pre-change report must not create Java/Python work merely to reproduce runtime
log names. Runtime log strings alone are not an implementation contract.

The archive-scoped registered-command audit through `AltoClefCommands.init()`
and the LAVI-owned `OverlayCommandRegistrar` found no registered command that
calls the bridge-visible final `finish()` callback and then asynchronously
assigns a later `UserTaskChain` root. Task-producing commands call
`runUserTask(...)` synchronously; immediate commands finish without scheduling a
later root. This result must be repeated against the exact edit baseline before
implementation.

The same archive contains a separate no-callback shape: `SetGammaCommand`
returns without calling `finish()`. That lifecycle gap is not this
synchronous-finish incident and must remain out of scope for the first patch.

## Goal

Fix the LAVI-owned Java bridge lifecycle classification so a command that
finishes synchronously without creating a new user root cannot bind a
pre-existing unchanged `IdleTask` as a command-owned root.

The expected user-visible result for the incident shape is:

```text
Java sends one terminal command_result with status=unknown
Python accepts that exact terminal through the normal accept_result path
Python active command clears
the next user command is not rejected as minecraft_command_busy
no gameplay success is claimed
```

## Non-Goals

This plan must not:

```text
modify adris/** upstream-derived engine behavior
change DepositCommand grammar or insufficient-item behavior
change UserTaskChain TaskFinishedEvent publication
change IdleTask semantics
add a timeout, retry, replay, StopCommand, cleanup, or follow-up command
change v1 command_result status values
enable Python reconciliation mutation
change persistent-idle terminal policy
solve the general retired-root/newer-active-command race in the first patch
infer success from IdleTask, finish callback, or root stability
infer global engine, Baritone, input, or gameplay quiescence from UserTaskChain root stability
use DepositCommand, StoreInAnyContainerTask absence, or log text as production predicates
route PREEXISTING_UNCHANGED_IDLE_ROOT through completedWithoutUserTask()
reclassify unrelated NO_ROOT_VISIBLE behavior without a separate audit
add a new result_fidelity value without truthfully mapping every emitted lifecycle result path
claim static source-contract tests are behavioral or race-condition proof
fix or reclassify commands that never invoke their finish callback
```

## Engine Behavior Being Preserved

The ChatClef / AltoClef engine remains the owner of:

```text
CommandExecutor command parsing and callback timing
DepositCommand inventory validation and finish() behavior
UserTaskChain root assignment and TaskFinishedEvent publication
IdleTask persistence
TaskRunner scheduling
Baritone path, goal, and input ownership
```

The bridge only changes how LAVI classifies command-root ownership after
dispatch. It does not alter Minecraft gameplay, inventory, container transfer,
pathing, task selection, or task completion.

## Why `adris/**` Is Left Unchanged

The confirmed failing boundary is not inside `DepositCommand`,
`UserTaskChain`, or `IdleTask`.

The upstream-derived engine behaved consistently with its current contracts:

```text
DepositCommand:
  insufficient inventory -> finish(); return;
  no StoreInAnyContainerTask is created

UserTaskChain:
  running idle roots do not publish TaskFinishedEvent

IdleTask:
  persistent idle task remains active
```

The defect is that the LAVI Java bridge treated the current task after dispatch
as command-owned without proving a new command-owned root assignment. Therefore
the fix belongs under:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/**
```

and not under:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/**
```

## Source Ownership Classification

Expected modified files are LAVI-owned bridge files:

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
```

Expected new LAVI-owned helper files, if implementation confirms they keep the
hunks smaller:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefRootOwnershipClassification.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefRootOwnershipClassifier.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefPreexistingIdleRootStabilityGate.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandResultFidelity.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefTaskOwnershipEvidence.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefFinishCallbackObservation.java
```

The pre-change report must also enumerate every typed lifecycle-details payload
and payload-map file needed to emit the new log-only fields. Those files must
not appear later as hidden incidental changes.

Explicitly out of scope for the first incident patch unless separately
approved:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefRetiredCommandRootLedger.java
```

The first patch must implement only the incident-specific event-association
gate. A general retired-root/tombstone ledger is a separate behavior approval.

Upstream-derived files intentionally left unchanged:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/DepositCommand.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/chains/UserTaskChain.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/movement/IdleTask.java
```

Required documentation updates in the same behavior patch, because the
`result_fidelity` value domain and terminal reason semantics change even when
the v1 envelope shape does not:

```text
plugins/Minecraft/docs/fabric-chatclef-bridge-protocol-v1.md
plugins/Minecraft/docs/chatclef-command-lifecycle-and-threading.md
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
plugins/Minecraft/docs/chatclef-command-payload-map-audit.md
```

## Minimum Hunk Plan

### 1. Capture one coherent before-dispatch evidence set

Files:

```text
FabricChatClefCommandDispatcher.java
FabricChatClefTaskStateReader.java
FabricChatClefTaskOwnershipSnapshotReader.java
FabricChatClefTaskOwnershipEvidence.java
```

Current source creates execution with only a task snapshot:

```text
new FabricChatClefCommandExecution(context, command, taskStateReader.captureCurrentTaskSnapshot())
```

Current `currentTaskOrNull()` and `ownershipSnapshot()` calls read the
`UserTaskChain` root independently. Calling them consecutively on one client
tick reduces risk but does not make the pair one coherent observation.

Plan:

```text
before executor.execute:
  FabricChatClefTaskOwnershipEvidence beforeEvidence =
      taskStateReader.captureOwnershipEvidence()
  pass beforeEvidence into FabricChatClefCommandExecution

captureOwnershipEvidence:
  read UserTaskChain and its raw current root once
  derive the task snapshot and root ownership fields from that same root read
  record one capturedClientTick and one capture timestamp
  return raw Task reference + typed snapshot + typed ownership snapshot
```

The capture runs on the client tick thread and is evidence only. It must not
mutate the task chain or reread the root through separate helper calls while
constructing one evidence object.

### 2. Classify root ownership after dispatch without using callback state

Files:

```text
FabricChatClefCommandDispatcher.java
FabricChatClefCommandLifecycleCoordinator.java
FabricChatClefCommandExecution.java
FabricChatClefCommandExecutionState.java
FabricChatClefRootOwnershipClassifier.java
FabricChatClefRootOwnershipClassification.java
```

Current source calls:

```text
lifecycleCoordinator.markDispatchReturned(execution, taskStateReader.currentTaskOrNull())
```

Current state stores that task directly as `boundRootTask`.

Plan:

```text
after executor.execute returns:
  FabricChatClefTaskOwnershipEvidence afterEvidence =
      taskStateReader.captureOwnershipEvidence()
  coordinator.markDispatchReturned(execution, afterEvidence)

inside execution state:
  classify candidate root from before/after coherent evidence only
  do not use finish_callback_received, TaskFinishedEvent, elapsed time, or command name
  set boundRootTask only for COMMAND_OWNED_ROOT
  retain observedPostDispatchRoot separately for diagnostics and the stability gate
```

Ownership classification is a snapshot relation. Terminal eligibility is a
separate lifecycle decision made later.

### 3. Latch the first finish-callback observation

Files:

```text
FabricChatClefCommandLifecycleCoordinator.java
FabricChatClefCommandExecution.java
FabricChatClefCommandExecutionState.java
FabricChatClefLifecycleDetailsPayload.java
FabricChatClefFinishCallbackObservation.java
```

Current state stores only `finishCallbackReceived=true` and overwrites
`terminalTask` on every callback. That cannot prove the first callback's order
relative to dispatch return, and a duplicate callback can rewrite the evidence
used by the synchronous-finish branch.

Plan:

```text
on the first finish callback only:
  atomically latch BEFORE_DISPATCH_RETURN or AFTER_DISPATCH_RETURN
  retain the raw callback task reference and typed task snapshot
  retain captured client-tick id, monotonic observation time, and thread name
  set finishCallbackReceived=true

on duplicate callbacks:
  increment and log a duplicate-callback audit counter
  do not rewrite first timing classification, task evidence, tick, time, or thread
  do not create another terminal decision or reopen admission
```

The incident UNKNOWN branch requires the first observation to be
`BEFORE_DISPATCH_RETURN`. A later duplicate must neither upgrade an original
after-return observation nor disqualify an original before-return observation.

### 4. Add root ownership states

New value:

```text
COMMAND_OWNED_ROOT
PREEXISTING_UNCHANGED_IDLE_ROOT
NO_ROOT_VISIBLE
OWNERSHIP_UNKNOWN
```

Required meaning:

```text
COMMAND_OWNED_ROOT:
  coherent before/after evidence attributes a non-null new or changed root
  assignment/object identity to the synchronous dispatch window; whether that
  owned root is finite, persistent, idle, or event-terminating is a separate
  terminal-policy question

PREEXISTING_UNCHANGED_IDLE_ROOT:
  the exact same root object, assignment id, generation, and running-idle state
  existed before and after dispatch

NO_ROOT_VISIBLE:
  no current root task is visible after dispatch; existing behavior remains
  unchanged and is not revalidated by this incident fix

OWNERSHIP_UNKNOWN:
  required snapshots are unavailable, contradictory, or cannot prove ownership
```

An `IdleTask` or `runningIdleTask=true` root is not automatically
non-command-owned. If coherent before/after evidence proves a new or changed
root assignment after dispatch, that root may be command-owned even when the
root is idle-shaped. Ownership classification and terminal policy are separate:
this incident patch must not change the existing persistent-idle terminal
contract merely because a newly assigned root is an `IdleTask`.

Fail-closed behavior:

```text
OWNERSHIP_UNKNOWN must not produce COMPLETED
OWNERSHIP_UNKNOWN must not clear active ownership
OWNERSHIP_UNKNOWN may continue waiting or become UNKNOWN only through a separate approved predicate
```

### 5. Add a pre-existing-idle terminal eligibility branch

Files:

```text
FabricChatClefCommandOutcomeClassifier.java
FabricChatClefCommandLifecycleCoordinator.java
FabricChatClefPreexistingIdleRootStabilityGate.java
```

Plan:

```text
if rootOwnershipClassification == PREEXISTING_UNCHANGED_IDLE_ROOT:
  if dispatch has not returned:
    waiting_for_dispatch_return
  if finish callback has not been received:
    waiting_for_command_callback
  if finish callback was first observed after dispatch returned:
    do not use the synchronous-finish incident branch; fail closed or use a separately approved path
  if the exact active execution/context identity no longer matches:
    fail closed without mutation
  if an unbound TaskFinishedEvent is observed:
    audit it, reset the stability window, and do not attach it as command evidence
  if next_task_idle_flag is true or changes:
    reset the stability window and fail closed for this incident branch
  if the idle-root stability window is not qualified:
    waiting_for_preexisting_idle_root_stability
  if the stability window is qualified and terminal send is ready:
    terminal UNKNOWN through the existing terminal outbox
```

This branch must not call `completedWithoutUserTask()` and must not claim a
gameplay result.

### 6. Gate TaskFinishedEvent association before mutating execution state

File:

```text
FabricChatClefCommandLifecycleCoordinator.java
```

For `PREEXISTING_UNCHANGED_IDLE_ROOT`, no command-owned root exists. Therefore
an event that happens to arrive while this execution is active must not be
stored through `markTaskFinishedObservation()` merely because of timing.

Minimum incident rule:

```text
PREEXISTING_UNCHANGED_IDLE_ROOT + no boundRootTask:
  log event as unbound/audit-only
  do not attach event to the execution
  do not treat it as success, failure, mismatch terminal, cleanup, or admission evidence
```

Known retired-root filtering for other command-owned roots may use the bounded
ledger described later. Unknown mismatch behavior outside this incident must
not be silently changed in the first patch.

### 7. Use existing terminal outbox and send-before-clear ordering

Files:

```text
FabricChatClefCommandLifecycleCoordinator.java
FabricChatClefCommandResultOutbox.java
```

Plan:

```text
reuse resultOutbox.sendTerminal(execution, ...)
do not clear Java queue/lifecycle ownership until async send completion succeeds
if send submission or async completion fails, keep Java active ownership
do not open Java command admission until the exact active context is cleared
```

`FabricChatClefCommandResultOutbox.java` already contains the core
send-before-clear ordering, so this plan should not rewrite it broadly.

### 8. Audit every bound-root consumer, including detach cancellation

Files:

```text
FabricChatClefCommandDispatcher.java
FabricChatClefCommandExecution.java
FabricChatClefCommandExecutionState.java
FabricChatClefCommandLifecycleCoordinator.java
FabricChatClefCommandOutcomeClassifier.java
FabricChatClefCommandResultFactory.java
FabricChatClefCommandDiagnosticPayload.java
```

`boundRootTask` is not only an outcome-classifier input. The reviewed source
also uses `matchesBoundRootTask(...)` during connection detach to decide whether
to call `AltoClef.cancelUserTask()`. Reclassifying the old `IdleTask` therefore
changes cancellation ownership as well as terminal classification.

Required contract:

```text
PREEXISTING_UNCHANGED_IDLE_ROOT:
  boundRootTask remains absent
  connection detach must not cancel the pre-existing IdleTask
  diagnostics must state that no command-owned root matched

COMMAND_OWNED_ROOT:
  existing exact-task detach cancellation remains available
  a matching current root may still be cancelled under the existing detach contract

all result/diagnostic payloads:
  distinguish command-owned bound root from observed post-dispatch root
  must not present the pre-existing IdleTask as command-owned evidence
```

The pre-change report must enumerate every actual-baseline use of
`boundRootTask`, `hasBoundRootTask`, `matchesBoundRootTask`, and
`boundRootMatchReason`. Outcome classification, event association, diagnostics,
and detach cancellation must agree on one ownership classification.

## `PREEXISTING_UNCHANGED_IDLE_ROOT` Predicate

The production ownership predicate must be command-agnostic. It must not
inspect:

```text
command name == deposit
StoreInAnyContainerTask absence
Insuffucient items log text
Korean input text
finish_callback_received
TaskFinishedEvent presence
elapsed time
```

Required ownership conditions:

```text
execution exists
taskBeforeDispatch object is non-null
taskAfterDispatch object is non-null
taskBeforeDispatch == taskAfterDispatch by Java object identity
before/after ownership snapshots are available
before/after evidence are captured on the dispatch/client thread in the same client tick
before/after user_task_root_identity are equal and nonblank
before/after user_task_root_assignment_id are equal and valid
before/after user_task_root_generation are equal and nonnegative
before/after user_task_running_idle == true
before/after next_task_idle_flag == false
before/after root class evidence is consistent with IdleTask
```

The raw object identity and engine idle flag are authoritative together. A
snapshot class-name string is corroborating diagnostic evidence, not a safe
replacement for raw object identity. Prefer `instanceof IdleTask` if a concrete
class check is used.

Ownership disqualifiers:

```text
same IdleTask class but different object instance
same task identity string but changed assignment id
same task identity string but changed generation
ownership snapshot unavailable
current task unavailable
blank, unavailable, or contradictory assignment evidence
root assignment changed
root generation changed
running idle flag false before or after
next_task_idle_flag true before or after
next_task_idle_flag changes
post-dispatch root is a new or changed root assignment
```

Terminal eligibility is evaluated separately and requires:

```text
activeExecution is the same execution object
commandQueue.activeContext is the same context object as execution.context
dispatch_returned == true
finish_callback_received == true
finish_callback_observed_before_dispatch_return == true
no command-owned root is bound
task_finished_event_received == false for this execution
terminal result has not been sent
terminal send is not already in flight
stable idle-root observation is qualified
```

Request, correlation, session, and connection-generation fields must still be
logged and compared for diagnostics, but exact context object identity is the
primary Java CAS/admission guard.

## Stable Idle-Root Observation Contract

This gate proves only that the same automatic idle root ownership evidence
remained unchanged long enough to avoid racing a delayed root assignment. It
does not prove global engine quiescence, lack of Baritone activity, lack of
input changes, or absence of gameplay effects.

The gate must be computed on Java `END_CLIENT_TICK` observations. A Python
dry-run result is not Java terminal authority.

This time window is only a race buffer. It cannot by itself prove that a command
will never assign a root later. The incident branch is therefore limited to a
finish callback observed during the synchronous `executor.execute(...)` call,
before dispatch return. The reviewed archive audit found no registered command
that calls the bridge-visible final `finish()` callback and then schedules a
`UserTaskChain` root asynchronously. The pre-change report must repeat that
audit against the actual source being edited; any newly discovered command with
that shape is excluded until it has an explicit ownership signal. Commands that
never invoke the callback, including the reviewed `SetGammaCommand` shape, are
outside this branch.

Provisional thresholds, to be implemented as named constants and covered by
deterministic tests rather than claimed as existing source constants:

```text
minimum distinct END_CLIENT_TICK ids: 3
minimum monotonic duration: 500 ms
maximum age of the newest ownership snapshot: 1000 ms
```

Use client-tick ids for distinctness and a monotonic clock such as
`System.nanoTime()` for elapsed duration. Wall-clock timestamps remain
diagnostic only.

Required stable signature fields:

```text
execution object identity
active queue context object identity
finish-callback-before-dispatch-return flag
request_id
session_id
connection_generation
correlation_id
waiting reason
taskBeforeDispatch raw object identity
taskAfterDispatch/current task raw object identity
user_task_root_identity
user_task_root_assignment_id
user_task_root_generation
user_task_running_idle
next_task_idle_flag
```

The following fields may be logged but must not be equality requirements for
this incident gate because scheduler selection can change independently of
UserTaskChain root ownership:

```text
selected_chain_class
selected_chain_identity
selected_chain_task_path
task_runner_active
Baritone process/path/input state
```

Reset conditions:

```text
execution object changes
active queue context changes
finish callback is first observed after dispatch return
request/session/generation/correlation changes
waiting reason changes away from the approved pre-existing-idle wait reason
an unbound TaskFinishedEvent is observed
terminal send starts
current task becomes unavailable
ownership snapshot becomes unavailable
current task differs from the before-dispatch idle object
assignment id changes
generation changes
running idle flag changes
next_task_idle_flag becomes true
next_task_idle_flag changes
contradictory root state appears
```

The stability window must not:

```text
sleep
poll from a background thread
call state-changing Minecraft, AltoClef, Baritone, input, or container APIs
submit retry/replay/cleanup commands
change Task selection
```

## `UNKNOWN` Terminal Payload

The first implementation should prefer the minimum payload-shape change while
still preserving the actual reason.

Required wire status:

```text
status=unknown
ok=false
```

Required result data:

```text
data.result_reason=finish_callback_without_new_command_owned_root
data.result_fidelity=callback_without_matching_user_task_event
```

The implementation may reuse the existing human-readable message:

```text
Fabric ChatClef command callback finished, but Minecraft goal success was not verified.
```

Use a dedicated factory method such as
`unknownFromPreexistingUnchangedIdleRoot()` rather than silently repurposing a
generic method whose result reason does not identify this boundary.

The semantic terminal decision is:

```text
the pre-existing unchanged idle root was not command-owned
gameplay effect remains unverified
```

Fields not required in the first behavior patch:

```text
data.terminal_reason
data.detail_reason
data.gameplay_effect
```

If those fields are added, it becomes an additive `command_result.data` shape
change and the payload map, protocol/lifecycle documents, fixtures, and Python
consumers that assert data shape must be updated together.

Even without adding fields, the new `result_reason` and `result_fidelity` value
semantics must be documented in the v1 protocol and lifecycle documents. The
protocol version may remain v1 because the envelope and status set do not
change.

The companion investigation document must encode the same minimum first-patch
contract and must treat `terminal_reason`, `detail_reason`, and
`gameplay_effect` as optional later additive fields. Codex reported companion
SHA-256 `49628cc0af62602e8062e1ae75d6e49603f78caadec53ba99f8a4b4fc0bf66b2`,
but those exact bytes were not supplied to this review. The pre-change report
must provide the file or an exact diff before claiming cross-document
alignment.

## `result_fidelity` Contract

Current reviewed source emits this fixed value for every lifecycle payload:

```text
result_fidelity=callback_plus_matching_user_task_event
```

That value is false not only for this incident UNKNOWN path, but also for
running, exception, deadline, no-user-task, and identity-mismatch paths. The
patch must therefore map every emitted lifecycle result path truthfully; it
must not special-case only the new UNKNOWN result while leaving the serializer
lying elsewhere.

Minimum evidence-value matrix:

```text
dispatch_started:
  dispatch_started_only

finish_callback_without_new_command_owned_root:
  callback_without_matching_user_task_event

callback_completed_without_user_task:
  callback_without_user_task

matching_task_finished / matching_task_stopped / task_observation_unclassified:
  callback_plus_matching_user_task_event

task_identity_mismatch:
  callback_plus_nonmatching_user_task_event

command_exception:
  command_exception_observed

dispatch_exception:
  dispatch_exception_observed

deadline_exceeded:
  deadline_without_verified_terminal

unmapped diagnostic/duplicate path:
  unknown
```

The serialized values in this matrix are frozen for the first patch. Java enum
constant names may follow normal repository naming conventions, but every
factory path must have an explicit mapping and executable tests. Fidelity values
describe the evidence authorizing the emitted result, not the terminal reason.
No value may imply a matching `TaskFinishedEvent` unless that event actually
supports the result.

Minimum hunk:

```text
remove the fixed RESULT_FIDELITY_VALUE from FabricChatClefCommandLifecyclePayloadMap
pass a typed/validated fidelity from FabricChatClefCommandResultFactory
through FabricChatClefCommandDiagnosticPayload and FabricChatClefCommandLifecyclePayload
serialize the supplied value at the existing result_fidelity key
```

Regression requirements:

```text
the pre-existing unchanged idle UNKNOWN path never emits callback_plus_matching_user_task_event
running results never claim terminal callback/event evidence
exception and deadline paths never claim matching TaskFinishedEvent evidence
matching completion preserves callback_plus_matching_user_task_event
all factory result reasons have a fidelity test
```

## Late `TaskFinishedEvent` Handling

`TaskFinishedEvent` does not carry request id, session id, connection
generation, or command correlation id. Event timing alone is therefore not
command ownership.

Incident-path rule:

```text
PREEXISTING_UNCHANGED_IDLE_ROOT has no command-owned bound root
any TaskFinishedEvent observed in that state is unbound/audit-only
the event is not attached to the execution and cannot decide its terminal result
```

General retired-root rule, only if separately approved:

```text
late evidence for a known retired command-owned root is audit-only
late evidence must not overwrite committed UNKNOWN
late evidence must not clear or overwrite a newer active command
late evidence must not trigger retry, replay, StopCommand, cleanup, or follow-up submission
```

For the incident path, the pre-existing `IdleTask` must never be registered as a
retired command root.

The first incident patch should not implement a broad retired-root ledger. It
may claim only this incident guarantee:

```text
PREEXISTING_UNCHANGED_IDLE_ROOT + no boundRootTask:
  incoming TaskFinishedEvent is not attached to the execution
  incoming TaskFinishedEvent is audit-only
  incoming TaskFinishedEvent resets the stable idle-root window
  incoming TaskFinishedEvent is not success, failure, mismatch, cleanup, or admission evidence
```

A bounded retired-root ledger is a separate follow-up approval. If later
approved, it must store raw task object identity rather than relying only on an
`identityHashCode` string, and it must record:

```text
request_id
session_id
connection_generation
correlation_id
terminal status
terminal reason
terminal sent timestamp
retired command-owned root object identity, if one existed
bounded TTL and maximum entry count
late event audit count
```

Provisional audit-only bounds:

```text
maximum entries: 64
TTL: 10 minutes
```

Expiration must not imply Java readiness, Python readiness, command success,
or permission to replay. The first patch must not claim a complete solution for
all historical late-root races.

## Structured Runtime Diagnostics Required For Verification

Runtime verification must not rely only on the absence of
`StoreInAnyContainerTask` or `TaskFinishedEvent` log lines. The Java bridge
must emit structured lifecycle/detail diagnostics for the incident path, without
expanding `command_result.data` beyond the minimum wire payload:

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
detach_cancel_action, when a detach path is exercised
```

These are log-only diagnostics in the first patch unless a separate wire
payload expansion is approved.

## Python Reconciliation Boundary

This patch must not enable Python reconciliation mutation.

Allowed Python behavior:

```text
normal accept_result handling for the exact Java terminal UNKNOWN
existing fail-closed active command ownership
existing dry-run diagnostics only if their current source is proven
```

Forbidden Python work in this patch:

```text
enabling any stale-active reconciliation mutation flag
adding production CAS release
changing quarantine behavior
creating a synthetic Python UNKNOWN for this incident
changing wire/local snapshot separation
```

The reviewed source archive does not contain the runtime reconciliation strings
or the three named reconciliation test files from the original proposal.
Before referring to a specific flag or test as existing, locate it in the
actual implementation baseline and record its path and commit. Do not create or
modify Python reconciliation code merely to make the plan's candidate names
exist.

## Tests To Add Or Update

Static Python source-contract tests are useful guardrails, but they do not
execute Java state transitions, event ordering, or terminal-send races. A
behavior implementation must not be approved with source-string assertions
alone.

### 1. Existing source-contract guardrails

```text
tests/test_minecraft_fabric_chatclef_java_bridge_contract.py
  dispatcher captures pre/post raw task and ownership evidence
  root classifier contains no DepositCommand, StoreInAnyContainerTask, or log-string predicate
  pre-existing idle root is not assigned to boundRootTask
  first finish-callback observation is latched and duplicate callbacks cannot overwrite it
  detach cancellation uses only command-owned boundRootTask evidence
  fixed RESULT_FIDELITY_VALUE serializer is removed
  outbox send-before-clear call remains present
  adris/** files are not modified
```

### 2. Required compiled Java behavior tests or deterministic harness

Preferred test targets:

```text
FabricChatClefRootOwnershipClassifierTest
FabricChatClefFinishCallbackObservationTest
FabricChatClefPreexistingIdleRootStabilityGateTest
FabricChatClefCommandOutcomeClassifierTest
FabricChatClefCommandResultFidelityTest
FabricChatClefTaskFinishedEventAssociationTest
FabricChatClefBoundRootDetachOwnershipTest
```

The classifier and gate should consume small immutable evidence objects so the
core state matrix can be tested without launching Minecraft. Required cases:

```text
one coherent capture derives raw root and snapshot fields from the same root read
same raw IdleTask object + same assignment/generation + running idle -> PREEXISTING_UNCHANGED_IDLE_ROOT
same class but different object -> not pre-existing
same object but changed assignment or generation -> not pre-existing
new/changed IdleTask root assignment is not pre-existing merely because it is idle-shaped
unavailable or contradictory snapshots -> OWNERSHIP_UNKNOWN
next_task_idle_flag true or changed disqualifies the pre-existing idle branch
callback state does not alter root ownership classification
first callback before dispatch return remains authoritative after a later duplicate
first callback after dispatch return cannot be upgraded by a later duplicate
duplicate callbacks do not overwrite terminal-task evidence or create duplicate terminals
callback observed after dispatch return does not qualify the synchronous-finish incident branch
source audit finds no registered command that calls finish and then asynchronously assigns a root, or such commands are explicitly excluded
registered no-callback command shapes remain explicitly out of scope
three calls in one client tick do not qualify stability
three distinct ticks before minimum duration do not qualify
minimum distinct ticks plus monotonic duration qualify
root/context/event change resets stability
pending idle assignment evidence resets stability
unbound event in pre-existing-idle state is audit-only and is not attached
pre-existing idle ownership does not trigger cancelUserTask on connection detach
real command-owned bound root preserves existing detach cancellation
qualified branch emits UNKNOWN, not COMPLETED
terminal send failure leaves Java active ownership uncleared
all result factory paths emit truthful result_fidelity
```

The reviewed Gradle files contain no proven Java unit-test framework. The
pre-change report must select one executable strategy and show its exact impact:

```text
Option A: scoped JUnit 5 support for the 1.20.1 project plus src/test/java tests
Option B: a deterministic pure-Java harness invoked by an existing repository test runner
```

The selected strategy must execute the ownership, callback-latch, stability,
event-association, detach-cancellation, send-failure, and fidelity matrices.
Any Gradle or harness infrastructure is an explicit reviewed hunk. Static text
tests alone are not acceptable.

### 3. Python ownership tests

Existing path:

```text
tests/minecraft_chatclef/lifecycle/test_connection_ownership_terminal_states.py
  exact Java UNKNOWN clears active through normal accept_result
  stale request/correlation/session/generation cannot clear newer ownership
```

Any reconciliation-specific tests in a newer local baseline must remain
default-off, but their exact paths must be source-proven first.

### 4. Required end-to-end assertions

```text
sync-finish command with unchanged pre-existing idle root does not bind that root
sync-finish command with unchanged pre-existing idle root emits UNKNOWN, not COMPLETED
result_reason == finish_callback_without_new_command_owned_root
result_fidelity == callback_without_matching_user_task_event
same IdleTask class but different task instance does not qualify
same task identity with changed assignment id or generation does not qualify
unavailable or contradictory ownership snapshots fail closed
stability requires distinct client ticks and minimum monotonic duration
callback-after-dispatch-return does not qualify this synchronous-finish branch
duplicate callback does not rewrite the first callback observation or emit a second terminal
next_task_idle_flag true or changed resets stability and fails closed for this branch
new root assignment, including a new IdleTask assignment, is not classified as pre-existing unchanged idle
new command-owned root assignment preserves existing terminal policy
unbound TaskFinishedEvent does not become command evidence for the incident path
connection detach does not cancel the pre-existing unchanged IdleTask
connection detach still cancels an exact matching command-owned root under the existing contract
terminal send failure leaves Java active ownership uncleared
matching UNKNOWN clears Python active through normal accept_result
request/session/generation/correlation mismatch cannot clear Python ownership
normal command that creates a real non-idle root still waits for matching TaskFinishedEvent
explicit persistent idle behavior is not misclassified as pre-existing automatic idle
StoreInAnyContainerTask-backed deposit remains unchanged
no-callback command behavior remains out of scope and is not silently reclassified
no automatic replay, retry, cleanup, StopCommand, or follow-up submission is introduced
```

## Build And Runtime Provenance Plan

Before any source edit, record the edit baseline:

```text
repository root
Git branch
Git commit
clean or dirty worktree status
`git status --short` output
reviewed source/archive SHA-256 or equivalent tree provenance
actual locations of all planned Java, test, and documentation files
archive-scoped registered-command audit repeated against this baseline
exact companion investigation file and SHA-256 used for cross-document alignment
```

After an approved implementation, before a build is treated as evidence,
record:

```text
exact modified files
exact documentation files changed
exact structured diagnostic fields added
exact Gradle command
JAVA_HOME
Gradle exit code
1.20.1 built jar path
1.20.1 built jar SHA-256
CurseForge instance root
installed mods jar path
installed mods jar SHA-256
duplicate ChatClef jar check
Minecraft launch time
latest.log path and hash or bounded prefix hash
stdout-logs.txt path and hash or bounded prefix hash
crash report check
runtime DIAGNOSTICS_RUNTIME_IDENTITY line
```

Clean forced build command, only after separate build approval:

```powershell
cd C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build --rerun-tasks
```

Runtime reproduction, only after separate runtime approval:

```text
1. deploy the freshly built 1.20.1 jar to the active CurseForge instance
2. confirm built jar SHA-256 equals installed mods jar SHA-256
3. launch Minecraft and confirm the intended jar identity in logs
4. confirm Java queue and Python ownership are both idle before submission
5. ensure player inventory lacks diamond x 2
6. submit "다이아몬드 2개 상자에 넣어줘"
7. verify the finish callback was observed before dispatch returned
8. verify no new StoreInAnyContainerTask/root assignment was created for the incident fixture
9. verify the old IdleTask was classified PREEXISTING_UNCHANGED_IDLE_ROOT and never bound as command-owned
10. verify the stability gate qualified on distinct client ticks and monotonic duration
11. verify next_task_idle_flag stayed false and did not change during the qualified window
12. verify structured diagnostics report root_ownership_classification, first callback observation, duplicate count, stability count/duration/reset reason, and event association
13. verify the first callback observation is BEFORE_DISPATCH_RETURN and duplicate count is zero for the incident fixture
14. verify Java sends exactly one status=unknown terminal
15. verify result_reason=finish_callback_without_new_command_owned_root
16. verify result_fidelity=callback_without_matching_user_task_event
17. verify no TaskFinishedEvent was attached as command evidence for this incident path
18. verify terminal send completion clears the exact Java queue/lifecycle context
19. verify Python accepts the matching UNKNOWN and clears its exact active command
20. in one fresh status snapshot, verify Java active context is null and Python active_request_id is null
21. submit a second benign command and verify it is accepted rather than rejected as minecraft_command_busy
22. run one normal command that creates a real non-idle root and verify the matching TaskFinishedEvent path still works
23. verify a new IdleTask assignment is not misclassified as PREEXISTING_UNCHANGED_IDLE_ROOT solely because it is idle-shaped
24. verify detach ownership tests preserve cancellation for a real bound root and skip cancellation for the pre-existing idle root
25. inspect latest.log/stdout/LAVI logs for Mixin failures, duplicate terminals, stale event attachment, unintended idle cancellation, retry/replay, StopCommand, or cleanup side effects
```

Expected verification outcome:

```text
RUNTIME_VERIFIED only if clean build passed, installed jar hash matched,
Minecraft loaded the intended jar, executable tests passed, no relevant
Mixin/runtime failure appeared, the incident-specific UNKNOWN was observed,
both Java and Python ownership cleared for the exact identity, and the normal
matching-root completion path remained intact.
```

## Rollback Unit

Preferred rollback after implementation:

```text
one independent commit containing only this Java bridge fix and its tests
or exact inverse hunks for the modified LAVI-owned bridge files
```

Do not use:

```text
git reset --hard
git clean -fd
broad checkout/restore of unrelated dirty files
```

## Remaining HOLD Reasons Before Behavior Implementation

The incident diagnosis, Java ownership boundary, UNKNOWN status, incident wire
payload, result-fidelity matrix, incident-only event-association scope,
`adris/**` exclusion, and broader-ledger exclusion are settled by this plan.
Python reconciliation mutation remains out of scope and is not a blocker while
it stays disabled.

Behavior implementation remains on HOLD only until the following are supplied:

```text
1. exact current repository root, branch, commit, worktree status, and file locations
2. exact minimal hunks re-derived against that baseline, including coherent evidence capture, first-callback latch, structured diagnostics, and every bound-root consumer
3. repeated baseline audit confirming no registered finish-then-async-root command shape and explicitly excluding no-callback command shapes
4. selected executable Java test strategy with exact Gradle or harness infrastructure hunk
5. exact companion investigation file/hash proving the two documents share the same first-patch wire and late-event scope
6. explicit user approval for the listed Java, test, and documentation edits
```

Built-jar and installed-jar hashes, Minecraft launch evidence, and runtime logs
are post-implementation verification gates. They cannot be completed before
source editing, but no deployment or runtime claim may be made without them.

## Approval Gate

Before code implementation, submit one pre-change report that closes the six
remaining HOLD items above and wait for explicit user approval. Do not silently
reopen the frozen result-fidelity matrix or incident-only event-association
scope during implementation. Further wording-only revision is not required
unless the exact edit baseline contradicts one of these contracts.

The pre-change report must include:

```text
exact current repo root, branch, commit, worktree state, and file locations
coherent same-read evidence capture hunk
pure ownership classifier hunk
first-callback latch and duplicate-callback audit hunk
structured diagnostic fields and emission points
all boundRootTask consumers and detach-cancellation effects affected by the ownership split
source audit for finish-then-async-root command shapes
explicit scope exclusion for no-callback command shapes
incident-only event association gate
explicit exclusion of the broader retired-root ledger from the first patch
compiled Java test or deterministic harness plan
any required test-infrastructure hunk
exact companion investigation file and verified SHA-256
exact Java/test/documentation files proposed for modification
```

Approval for this document is not approval to:

```text
edit Java
edit Python
run tests
run Gradle build
copy jars
launch Minecraft
commit
push
```

The implementation should stop immediately if it appears to require:

```text
adris/** behavior changes
TaskRunner, UserTaskChain, IdleTask, or DepositCommand modification
protocol status value changes
Python reconciliation mutation
global input/path/goal cleanup
automatic retry/replay
implementation supported only by static source-string tests
unproven reuse of runtime-only quiescence/reconciliation constants or test paths
broader retired-root ledger work without separate approval
```
