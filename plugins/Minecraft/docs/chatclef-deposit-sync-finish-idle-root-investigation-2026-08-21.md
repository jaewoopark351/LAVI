<!-- 20260821_kpopmodder: Reviewed v2; tightened source/runtime provenance, generic ownership classification, result-fidelity, and late-evidence boundaries. -->
<!-- 20260821_openai: Reviewed-v4 narrowed first-patch late-event scope and added pending idle-assignment guards. -->

# ChatClef Deposit Sync-Finish Idle Root Investigation - 2026-08-21

Date: 2026-08-21 KST

Status:

```text
incident evidence status: CONFIRMED_FOR_THIS_INCIDENT
root cause class: Java bridge command-root ownership misclassification after synchronous finish
documentation review: PASS_AFTER_REVIEWED_V4
deployed build jar == installed mods jar: CONFIRMED_BY_INCIDENT_EVIDENCE
reviewed source -> deployed jar provenance: NOT_RECORDED_IN_THIS_DOCUMENT
behavior fix: not implemented
Java code change: none in this documentation pass
Python code change: none in this documentation pass
protocol version change: none
command_result.data shape change: none in this documentation pass
build or runtime reproduction: not run by this documentation pass
```

This document records the evidence for the 2026-08-21 LAVI command hang where
Minecraft showed ChatClef as idle, but LAVI rejected follow-up commands as busy.
It is documentation only. It does not approve Java changes, Python changes,
Gradle build, Minecraft launch, runtime reproduction, active-command release,
commit, or push.

## Scope

This incident belongs to the Fabric ChatClef path only:

```text
LAVI -> Fabric adapter -> Fabric mod -> ChatClef / AltoClef
```

The affected boundary is the LAVI-owned Java command bridge lifecycle:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/**
```

The incident is not a Forge/MineMind issue and does not justify shared backend
code, shared bridge code, or a protocol version change.

## Incident Identity

User command:

```text
다이아몬드 2개 상자에 넣어줘
```

Python translation and bridge command:

```text
deposit diamond 2
```

Observed active identity:

```text
request_id: lavi-input-ko-a7d4870bb90a403f82802ff24dabe65a
command_message_id / correlation_id: lavi-07c558d1edbd45ed9cf145445480dd35
session_id: fabric-chatclef-645e2fe373054544903a848fe6753e1c
connection_generation: 1
```

Primary evidence files:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
C:\Vtuber_Souorce_Code\LAVI\logs\20260821_174029_log.txt
```

## Evidence And Provenance Boundary

The incident evidence states that the built jar and the installed CurseForge
mods jar have the same SHA-256. That proves copy identity between those two jar
files. By itself, it does not prove which Git commit, worktree state, or source
archive produced the built jar.

This document also reviews the matching source branch shape. However, it does
not record all of the following as one reproducible provenance tuple:

```text
Git commit and branch
clean or dirty worktree state
exact Gradle invocation and JAVA_HOME
built jar SHA-256
installed mods jar SHA-256
reviewed source archive SHA-256
```

Therefore, the incident causal chain is confirmed from the supplied logs plus
the reviewed source shape, while exact reviewed-source-to-runtime provenance
must be re-established before a behavior patch is built and deployed.

## Confirmed Evidence

### 1. Deposit short-circuited before a Store task was created

`stdout-logs.txt:2412` contains the exact ChatClef system message:

```text
Insuffucient items in inventory to deposit. We still need: diamond x 2.
```

In the current 2026-08-21 active log pair, `rg -c "StoreInAnyContainerTask"`
found no `StoreInAnyContainerTask` match. This is corroborating negative
evidence, not standalone proof. Together with the exact insufficient-item log
and the source branch below, it supports the conclusion that this specific
command did not create or submit the normal storage root task.

The source path confirms the branch shape:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/DepositCommand.java
```

Relevant source evidence:

```text
line 86: logs insufficient inventory
line 87: finish()
line 88: return
line 113: StoreInAnyContainerTask storeTask = new StoreInAnyContainerTask(...)
line 117: mod.runUserTask(storeTask, this::finish)
```

Therefore, on the insufficient-item path, `DepositCommand` calls `finish()` and
returns before `StoreInAnyContainerTask` is constructed or submitted.

### 2. The IdleTask was pre-existing, not command-owned

`latest.log:235` records the initial user task root:

```text
assignedRootClass=adris.altoclef.tasks.movement.IdleTask
assignedRootIdentity=2d011ebe
rootAssignmentId=user-root-1
rootGeneration=1
runningIdleTask=true
```

The command lifecycle then reused the same root identity around dispatch:

```text
latest.log:872
  task_before_dispatch=IdleTask#2d011ebe
  bound_root_task unavailable
  dispatch_returned=false
  finish_callback_received=false

latest.log:874
  finish_callback_received=true
  callback_current_task=IdleTask#2d011ebe
  user_task_root_assignment_id=user-root-1
  user_task_root_generation=1
  user_task_running_idle=true

latest.log:876
  dispatch_returned=true
  task_after_dispatch=IdleTask#2d011ebe
  bound_root_task=IdleTask#2d011ebe

latest.log:877 and later
  waiting_reason=waiting_for_task_finished_event
  task_finished_event_received=false
  current_task_matches_bound_root_task=true
```

The log ordering is also significant: `finish_callback_received=true` appears
before `dispatch_returned=true`. This matches a synchronous callback during
`CommandExecutor.execute(...)`, followed by post-dispatch binding of whatever
task was already current.

The task identity string is diagnostic evidence. The unchanged
`rootAssignmentId=user-root-1` and `rootGeneration=1` are the stronger ownership
evidence that no new UserTaskChain root assignment occurred around dispatch.

This is the important ownership mismatch: the bridge treated the already-running
idle root as the command's root merely because it was the current task after
dispatch returned.

### 3. UserTaskChain does not publish a TaskFinishedEvent for running idle

The current `UserTaskChain.java` source contains the relevant guard:

```text
line 217: willPublishTaskFinishedEvent = actuallyDone && !runningIdleTask
line 220: if (!runningIdleTask) {
line 229:     TaskFinishedEvent finishedEvent = new TaskFinishedEvent(...)
```

For this incident, the root snapshot repeatedly reports
`user_task_running_idle=true`. Waiting for a `TaskFinishedEvent` from that idle
root is therefore not a valid terminal strategy.

### 4. Python correctly remained fail-closed without an accepted terminal result

`logs/20260821_174029_log.txt:123-124` shows that Python accepted and sent the
command:

```text
event=command_accepted
event=command_send_succeeded
command=deposit diamond 2
```

`logs/20260821_174029_log.txt:128` shows the status/reconciliation evidence retained for the active request:

```text
status=running
result_reason=stable_request_quiescence_observed
finish_callback_received=true
task_finished_event_received=false
bound_root_task=IdleTask#2d011ebe
current_root_task=IdleTask#2d011ebe
stable_request_quiescence.qualified=true
gameplay_effect=UNVERIFIED
```

`logs/20260821_174029_log.txt:132` shows the next route rejected as busy:

```text
reason=minecraft_command_busy
error=active_command
message=Fabric ChatClef command already active:
  lavi-input-ko-a7d4870bb90a403f82802ff24dabe65a
```

`logs/20260821_174029_log.txt:133` shows Python dry-run reconciliation found
the stale-active shape, but did not mutate ownership:

```text
identity_quality=EXACT
reconciliation_decision=ELIGIBLE_DRY_RUN
would_reconcile_to_unknown=true
active_release_performed=false
release_guard=dry_run_only_no_cas_release
gameplay_effect=UNVERIFIED
```

Python acquired local active-command ownership when the command was submitted
and had not accepted an exact matching terminal result. Rejecting the next
route as `minecraft_command_busy` was therefore the intended fail-closed guard,
not the primary defect.

The reviewed source snapshot does not contain the exact runtime strings
`ELIGIBLE_DRY_RUN`, `would_reconcile_to_unknown`,
`active_release_performed`, or `dry_run_only_no_cas_release`. Those fields are
log-observed evidence for this incident, but the implementation and feature
flag that produced them require a separate source audit before any Python
mutation is enabled.

## Correct Causal Chain

```text
deposit diamond 2
  -> DepositCommand inventory validation detects missing diamond x 2
  -> DepositCommand calls finish() synchronously and returns
  -> no StoreInAnyContainerTask is created
  -> Java bridge observes the pre-existing IdleTask after dispatch
  -> Java bridge binds that IdleTask as the command root
  -> classifier sees hasBoundRootTask=true
  -> classifier waits for TaskFinishedEvent
  -> UserTaskChain does not publish TaskFinishedEvent for running idle
  -> Java never sends a terminal command_result
  -> Python keeps the active command
  -> follow-up LAVI commands are rejected as minecraft_command_busy
```

The earlier statement "Python gate is stale" is only the downstream symptom.
The stronger root-cause statement is:

```text
The LAVI-owned Java bridge misclassified a pre-existing stable IdleTask as the
new command-owned root after a synchronous command finish with no user task.
```

## What This Does Not Prove

This incident does not prove:

```text
ChatClef engine is still executing a deposit task
StoreInAnyContainerTask itself failed
absence of a StoreInAnyContainerTask log is a valid production bridge predicate
TaskFinishedEvent publishing is defective
Python should clear active ownership without Java terminal coordination
Python dry-run reconciliation is safe to enable without source audit
the command completed successfully
the deposit gameplay effect succeeded
the reviewed source snapshot is the exact source that produced the deployed jar
IdleTask or UserTaskChain should be changed
DepositCommand grammar should be changed
```

The correct lifecycle terminal classification for the bridge is not
`completed`. The bridge cannot prove a verified gameplay effect. The safe
terminal status is `unknown`; this retires command ownership but does not claim
gameplay success. A later explicit contract may classify the insufficient-item
branch as semantic `failed` or `rejected`, but only if that reason is carried
through a source-backed command-result channel rather than inferred from the
finish callback.

## Fix Direction

The primary fix should live in the LAVI-owned Java bridge, not in upstream
`adris/**` engine classes. The bridge classification must remain command-agnostic:
`DepositCommand`, `StoreInAnyContainerTask`, and the insufficient-item text are
incident evidence and test-fixture inputs, not production lifecycle predicates.

The bridge should represent root ownership explicitly, for example:

```text
COMMAND_OWNED_ROOT
PREEXISTING_UNCHANGED_IDLE_ROOT
NO_ROOT_VISIBLE
OWNERSHIP_UNKNOWN
```

`PREEXISTING_UNCHANGED_IDLE_ROOT` may be selected only when all required typed
before/after ownership snapshots are available and the ownership evidence
agrees:

```text
before and after root task are the same task instance
before and after user_task_root_identity are equal
before and after user_task_root_assignment_id are equal
before and after user_task_root_generation are equal
before and after user_task_running_idle are true
before and after next_task_idle_flag are false
no new UserTaskChain root assignment was observed
```

`IdleTask` class equality is corroborating evidence only. It must not replace
exact task-instance, assignment-id, generation, and snapshot-availability
checks. A same-class different instance or any assignment/generation change is
not an unchanged pre-existing root.

A newly assigned `IdleTask` is not excluded from command ownership merely
because it is idle-shaped or engine-marked as running idle. If coherent
before/after evidence proves a new or changed assignment, ownership and terminal
policy must be handled separately. This investigation fixes only the
dispatch-before pre-existing automatic idle-root case and does not change the
existing persistent-idle terminal contract.

Terminal eligibility is a separate lifecycle decision. The incident-specific
UNKNOWN branch additionally requires:

```text
active execution/context still matches exactly
dispatch_returned == true
finish_callback_received == true
finish_callback_observed_before_dispatch_return == true
task_finished_event_received == false for this execution
no command-owned root is bound
stable idle-root observation qualified on distinct END_CLIENT_TICK observations
next_task_idle_flag remained false and unchanged
```

Stable idle-root observation must be computed by the Java client-tick-owned
lifecycle from typed snapshots. It proves only that the same automatic idle-root
ownership evidence remained unchanged across a small delayed-assignment race
buffer. It does not prove global engine quiescence, Baritone inactivity, input
inactivity, or absence of gameplay effects. A Python dry-run verdict must not
be imported as the Java terminal authority. The later implementation plan must
identify the exact stable idle-root fields, minimum distinct-tick count, reset
conditions, and tests; it must not use an arbitrary sleep or wall-clock-only
timeout.

`next_task_idle_flag` is pending-assignment evidence for this branch. If it is
true or changes during the observation window, the implementation must reset the
window and fail closed for the incident path.

When the guarded classification is satisfied, the Java bridge must not retain
the pre-existing IdleTask as a command-owned bound root. It should emit exactly
one terminal `unknown` through the existing terminal outbox.

The intended semantic result is:

```text
status=unknown
ok=false
data.result_reason=finish_callback_without_new_command_owned_root
data.result_fidelity=callback_without_matching_user_task_event
terminal decision=pre-existing idle root was not command-owned
```

The first behavior patch should use the minimum payload-shape change that still
preserves the actual boundary. The reviewed Java source currently contains
`unknownAfterFinish()` with the more generic reason:

```text
data.result_reason=finish_callback_without_verified_success
```

That reason is too broad for this boundary. The implementation should add a
dedicated result factory path, such as
`unknownFromPreexistingUnchangedIdleRoot()`, and emit:

```text
data.result_reason=finish_callback_without_new_command_owned_root
data.result_fidelity=callback_without_matching_user_task_event
```

Additional fields such as `data.terminal_reason`, `data.detail_reason`, or
`data.gameplay_effect` are not required in the first behavior patch. If the
richer fields are added to `command_result.data`, that is an additive
payload-shape change and the payload map, protocol documentation, fixtures, and
consumers must be updated together even though the protocol version remains v1.

A separate required invariant is result fidelity. The reviewed payload map
currently emits:

```text
result_fidelity=callback_plus_matching_user_task_event
```

for every lifecycle payload. That value would be false for this
pre-existing-idle UNKNOWN because no matching `TaskFinishedEvent` exists. The
implementation must emit
`result_fidelity=callback_without_matching_user_task_event` for this path and
must never claim `callback_plus_matching_user_task_event` here.

After the terminal send succeeds, Java may clear both queue and lifecycle
ownership using the existing send-before-clear ordering. Python should then
accept the exact matching terminal `unknown` through the normal `accept_result`
path and clear its active command. A failed or stale terminal send must leave
Java ownership active and must not open new-command admission.

Late evidence handling in the first behavior patch is incident-only. At
minimum:

```text
PREEXISTING_UNCHANGED_IDLE_ROOT + no boundRootTask
incoming TaskFinishedEvent is not attached to the execution
incoming TaskFinishedEvent is audit-only
incoming TaskFinishedEvent resets the stable idle-root observation window
incoming TaskFinishedEvent cannot become success/failure/mismatch evidence
incoming TaskFinishedEvent cannot trigger retry, replay, StopCommand, cleanup, or follow-up submission
```

Because `TaskFinishedEvent` has no request ID, a general old-command-root event
arriving while a newer command is active is a separate race class. A bounded
retired-root/tombstone ledger may be appropriate for that later problem, but it
is not part of the first incident patch. For this incident-specific path, no
command-owned root was ever created; the implementation must preserve that
stronger fact rather than pretending the old IdleTask was retired command work.

Python dry-run reconciliation remains useful as a fallback and audit layer, but
it should stay dry-run for this patch unless its exact mutation source, compare-
and-set ownership guard, Java retirement evidence, and late-terminal contract
are reviewed separately.

## Proposed Files For A Later Fix

Expected core Java bridge files, subject to an exact hunk plan:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandDispatcher.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandExecution.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandExecutionState.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandResultFactory.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandLifecycleCoordinator.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandOutcomeClassifier.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefTaskOwnershipSnapshot.java
```

Conditional payload files are required if result fidelity or additive result
fields change:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandDiagnosticPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandLifecyclePayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/payload/FabricChatClefCommandLifecyclePayloadMap.java
```

Typed `result_fidelity` also requires the documentation/audit surface to move
together:

```text
plugins/Minecraft/docs/fabric-chatclef-bridge-protocol-v1.md
plugins/Minecraft/docs/chatclef-command-lifecycle-and-threading.md
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
plugins/Minecraft/docs/chatclef-command-payload-map-audit.md
```

Runtime verification must not infer correctness only from absent logs. The
first behavior patch should emit structured Java diagnostics for:

```text
root_ownership_classification
finish_callback_first_observation
stable_idle_root_observation.qualified
stable_idle_root_observation.distinct_tick_count
stable_idle_root_observation.stable_duration_ms
stable_idle_root_observation.reset_reason
task_finished_event_association
```

These may remain log-only lifecycle/detail diagnostics in the first patch; the
minimum wire payload stays limited to `result_reason` and `result_fidelity`.

A new focused value object for ownership classification is preferable to adding
a second responsibility to an existing class. Retired-root tombstones belong to
a separate follow-up if that broader race is approved. Do not add fields to
unrelated transport or engine classes merely to avoid creating a small
single-purpose type.

Upstream-derived files intentionally left unchanged:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/DepositCommand.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/chains/UserTaskChain.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/movement/IdleTask.java
```

## Test Requirements Before Behavior Change

A later implementation should add focused coverage for:

```text
sync-finish command with unchanged pre-existing idle root does not bind that root
sync-finish command with unchanged pre-existing idle root emits UNKNOWN, not COMPLETED
StoreInAnyContainerTask absence is not used as a production classifier predicate
same IdleTask class but different task instance does not qualify
same task identity with changed assignment id or generation does not qualify
unavailable or contradictory ownership snapshots fail closed
stable idle-root observation requires the approved number of distinct client ticks
next_task_idle_flag true or changed resets stable idle-root observation and fails closed for this branch
new root assignment, including a new IdleTask assignment, is not classified as pre-existing unchanged idle
new command-owned root assignment preserves existing terminal policy
pre-existing-idle UNKNOWN does not claim callback_plus_matching_user_task_event fidelity
UNKNOWN terminal uses the existing terminal outbox and send-before-clear ordering
terminal send failure leaves Java active ownership uncleared
matching UNKNOWN clears Python active through normal accept_result
request/session/generation/correlation mismatch cannot clear Python ownership
incident-path unbound TaskFinishedEvent is audit-only and resets stable idle-root observation
broader retired-root/newer-active-command races are not claimed solved by the first patch
normal command that creates a real root still waits for its matching TaskFinishedEvent
explicit idle command that creates a new IdleTask assignment is not treated as pre-existing idle
persistent configured idle behavior remains unchanged
StoreInAnyContainerTask-backed deposit remains unchanged
no automatic replay, retry, cleanup, StopCommand, or follow-up submission is introduced
```

Runtime verification after an approved fix must use the existing Fabric
ChatClef build verification runbook. A Gradle build alone is not runtime proof.
The verification record must bind the reviewed source commit/worktree, built jar
SHA-256, installed mods jar SHA-256, Minecraft launch, and incident-specific log
assertions into one evidence tuple.

## Approval Boundary

This document approves no source behavior change. Before implementing the
bridge fix, the next step must explicitly report:

```text
engine behavior being preserved
LAVI-owned bridge boundary
exact files and minimal hunks
source-to-runtime provenance tuple
why upstream adris/** files remain unchanged
root ownership classification states and fail-closed UNKNOWN case
exact stable idle-root observation fields, distinct-tick threshold, and reset conditions
terminal status and exact command_result.data encoding
result_fidelity contract for the no-TaskFinishedEvent UNKNOWN path
incident-only event association gate and broader retired-root ledger exclusion
source audit for finish-then-async-root command shapes and no-callback command exclusions
compiled Java test or deterministic harness plan, including any test-infrastructure hunk
tests to run
build and runtime verification plan
```

Then stop for explicit user approval. Do not enable Python reconciliation
mutation, modify `adris/**`, build, launch Minecraft, commit, or push as part of
this documentation-only step.
