<!-- 20260808_kpopmodder: Documented the gold-ingot path/target retry thrashing diagnosis and next read-only target timeline analysis. -->

# ChatClef Resource Target Retry Thrashing Analysis

Date: 2026-08-08

This document records the corrected diagnosis for the Fabric ChatClef
`get gold_ingot 10` investigation.

It is documentation only. It does not approve Java behavior changes, Python
behavior changes, command lifecycle changes, Fabric bridge payload changes,
Baritone engine changes, diagnostics changes, Gradle changes, Minecraft launch,
runtime reproduction, cache deletion, commit, or push.

## Scope

This applies to the Fabric ChatClef runtime and diagnostics for resource
collection tasks:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
plugins/Minecraft/docs/chatclef-baritone-cache-troubleshooting.md
```

The immediate investigation command was:

```text
command=get gold_ingot 10
normalized_command=@get gold_ingot 10
request_id=lavi-input-ko-0494fb0755d242a18dc3217e021c3bb3
correlation_id=lavi-35e9843329154603a902a4e37e97ac51
session_id=fabric-chatclef-74a43cb2d0c24c29b34593fe23ea1d17
```

## Corrected Diagnosis

The correct diagnosis is not a permanent livelock.

The observed behavior was:

```text
39-minute path/target retry thrashing
```

The command eventually completed:

```text
completed_at=2026-08-08 23:47:58 KST
status=completed
ok=true
result_reason=matching_task_finished
completion_source=altoclef_task_finished_event
duration_seconds=2344.513999938965
```

The earlier "infinite loop" wording is valid only as an operator-facing stuck
symptom while the command is still running. It is not the verified internal
failure mode for this evidence set.

## Command Lifecycle Result

The command lifecycle is not the failing boundary for this incident.

Observed terminal evidence:

```text
dispatch_returned=true
task_finished_event_received=true
finish_callback_received=true
terminal_result_sent=true
result_fidelity=callback_plus_matching_user_task_event
event_task_matches_bound_root_task=true
event_task_bound_root_match_reason=same_task_instance
```

Python also accepted the terminal result:

```text
[MinecraftFabricChatClef] command result received
request=lavi-input-ko-0494fb0755d242a18dc3217e021c3bb3
status=completed
ok=True
message=ChatClef user task reached natural completion.
```

Do not change command lifecycle ownership, bridge result payload shape, command
result status values, task completion conditions, or Python DTO handling for
this incident.

## Baritone Calculation Summary

The Baritone worker did not hang.

Observed calculation generation summary:

```text
completed generation count=192
missing completed count=0
result count total=192
```

Result counts:

```text
FAILURE=89
CANCELLATION=48
SUCCESS_SEGMENT=26
SUCCESS_TO_GOAL=29
```

Path presence counts:

```text
resultPathPresent=false: 137
resultPathPresent=true: 55
```

Path adoption counts:

```text
NO_PATH_RESULT_NOT_ADOPTABLE=89
NO_PATH_CANCELLATION=47
ADOPTED_AS_CURRENT=49
ADOPTED_AS_NEXT=6
```

All 55 generations with a path were adopted:

```text
ADOPTED_AS_CURRENT + ADOPTED_AS_NEXT = 55
```

Therefore, the current evidence does not support:

```text
single pathfinder worker hang
path result produced but never adopted
general path adoption failure
```

The heavy cost is before or around path success:

```text
no-path failures
cancellations
target reselection
movement failure after adopted paths
blacklist and unreachable transitions
```

## One Adoption-Outcome Gap

There is one count mismatch to resolve in the next read-only analysis:

```text
CANCELLATION result count=48
NO_PATH_CANCELLATION adoption outcome count=47
```

Find the one cancellation generation without a recorded adoption outcome and
classify it as one of:

```text
normal cancellation path that does not emit adoption decision
diagnostics gap
log extraction gap
```

Do not add diagnostics before confirming whether the existing logs already
identify this generation.

## Active Child Reconciliation Summary

Repeated `DestroyBlockTask` candidate allocation is not proof of active child
churn. `MineOrCollectTask` may allocate a new candidate every tick while
`Task.tick()` retains the existing active child through `isEqual()`.

Observed reconciliation counts:

```text
TASK_CHILD_RECONCILIATION total=139
REUSED_ACTIVE_CHILD_CANDIDATE_EQUAL=94
REPLACED_ACTIVE_CHILD=44
REPLACEMENT_BLOCKED_PREVIOUS_CHILD_NOT_INTERRUPTIBLE=1
replacementApplied=false=95
replacementApplied=true=44
```

This means:

```text
candidate ID churn alone is noise
actual active Destroy child replacement did occur 44 times
```

The next analysis must determine whether those 44 replacements were normal
blacklist-driven target transitions or unstable target-selection churn.

## Progress And Blacklist Summary

The progress and blacklist layer was not completely inactive.

Observed counts:

```text
MOVEMENT_PROGRESS_CHECK_RESULT=460
BLOCK_UNREACHABLE_REQUEST=34
BLOCK_BLACKLIST_STATE_CHANGED=56
```

Movement result counts:

```text
PASS=388
PROGRESS_FAILURE=35
MOVE_CHECK_FAILED=34
STUCK_CHECK_FAILED=3
```

The current evidence points to a long sequence of target failure, cancellation,
blacklist or unreachable updates, and target reselection. It does not yet prove
that blacklist state was ignored.

## BlockOptionalMeta NPE

The `BlockOptionalMeta` NPE observed at `2026-08-08 23:14:46 KST` remains a
separate Baritone failure mode to track, but this incident does not yet prove
that it caused the path/target retry thrashing.

Relevant evidence:

```text
BlockOptionalMeta NPE observed at 23:14:46
calculationGeneration=73 in the same window completed
calculationGeneration=73 resultType=SUCCESS_TO_GOAL
calculationGeneration=73 adoptionOutcome=ADOPTED_AS_CURRENT
```

Do not treat this NPE as the first failing boundary for this command unless a
target-level timeline or later evidence connects it to the same worker,
generation, target exclusion state, or repeated failed target selection.

## Next Read-Only Analysis

Before any diagnostics or behavior change, reconstruct a target-level timeline
from the existing logs.

For each coal `targetPosition`, collect:

```text
first selected time
last selected time
selection count
cumulative active time
linked calculationGeneration count
FAILURE count
CANCELLATION count
SUCCESS_SEGMENT count
SUCCESS_TO_GOAL count
MOVEMENT_PROGRESS_CHECK_RESULT counts
BLOCK_UNREACHABLE_REQUEST count
blacklist add time
blacklist remove or reset time
cases where the target was selected while blacklisted
active child replacements entering that target
active child replacements leaving that target
```

Classify each of the 44 `REPLACED_ACTIVE_CHILD` events:

```text
previous child class
candidate child class
parentTaskClass
previousTargetPosition
candidateTargetPosition
same coordinate / adjacent coordinate / different coordinate
progress failure immediately before replacement
unreachable request immediately before replacement
blacklist state immediately before replacement
whether replacement triggered Baritone cancellation
whether parentTaskClass is MineOrCollectTask or a wrapper
```

For each no-path generation, connect the next goal request:

```text
same target requested again
adjacent target requested
different target requested
time until next request
blacklist state at next request
cancellation owner
```

## Target-Level Decision Table

Use this table after the target timeline is reconstructed.

```text
Observed result:
    same BlockPos selected while blacklisted or immediately after blacklist
Suspected boundary:
    MineOrCollectTask or block scanner exclusion propagation

Observed result:
    nearby unreachable coal targets are selected in a cluster loop
Suspected boundary:
    per-BlockPos blacklist works, but target-cluster retry thrashing is not bounded

Observed result:
    active Destroy child replacement occurs before calculation cancellation
Suspected boundary:
    target selection stability or hysteresis

Observed result:
    path is adopted, then movement failure blacklists the target
Suspected boundary:
    path execution, reachability, terrain, or world/cache state

Observed result:
    each target is excluded normally and new targets are explored until success
Suspected boundary:
    extreme but valid world terrain/search cost, not a verified behavior bug
```

## Diagnostics-Only Gate

If existing logs can answer the target-level questions above, do not change
source.

Only propose a diagnostics-only source change when one of these remains
materially unobservable:

```text
target selection while blacklisted cannot be proven or ruled out
active child replacement cannot be connected to target position
no-path generation cannot be connected to the next goal request
cancellation owner cannot be identified
movement failure cannot be connected to blacklist state
the single missing adoptionOutcome cancellation cannot be explained
```

Any diagnostics-only change must preserve behavior:

```text
no return-value change
no task selection change
no isEqual or isFinished change
no retry, timeout, cooldown, blacklist, or fallback change
no new forceCancel or reordered forceCancel
no input or path ownership change
no command lifecycle payload shape change
no bridge wire protocol change
no broad exception catch or suppression
```

The first diagnostics candidate, if needed, should be additive target-level
observation around `MineAndCollectTask` target selection and existing blacklist
state, not a Baritone engine behavior change.

## Behavior Fix Placement Reminder

Do not patch behavior yet.

If a future target-level timeline proves a root cause, prefer the narrowest
owner in this order:

```text
1. operational cache reset or world-state procedure, if cache/terrain mismatch is proven
2. MineAndCollectTask target exclusion or scanner handoff, if blacklisted targets are reselected
3. target-cluster retry suppression, if per-BlockPos blacklist works but cluster thrashing repeats
4. target selection stability or hysteresis, if active child replacement drives cancellations
5. minimal Baritone engine hunk only if path execution/adoption evidence proves no narrower owner
```

These are not approved fixes. They are placement rules for a later evidence
review.

## Prohibited Fixes For This Evidence Set

Do not use this incident to justify:

```text
command timeout addition
global Baritone cancel
global retry or cooldown
TaskRunner lifecycle change
UserTaskChain or SingleTaskChain behavior change
DestroyBlockTask.isFinished() change
InteractWithBlockTask.isFinished() change
Fabric bridge result payload change
Python CommandResultDTO.data shape change
Baritone path adoption behavior change
BlockOptionalMeta source change
```

The current safest hypothesis is still outside command lifecycle and outside
the first Baritone engine patch boundary:

```text
MineAndCollectTask target selection and exclusion boundary
```

That hypothesis remains unproven until the target-level timeline is complete.
