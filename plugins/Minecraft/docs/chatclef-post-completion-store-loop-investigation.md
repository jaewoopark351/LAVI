<!-- 20260807_kpopmodder: Documented the post-completion StoreInAnyContainerTask loop and diagnostic log flood. -->

# ChatClef Post-Completion Store Loop Investigation

Date: 2026-08-07

This document records the current interpretation of the
`LAVI_TEST_Fabric01` log window where Fabric ChatClef bridge commands completed
successfully, but AltoClef continued running a storage/container task and
diagnostics continued to grow quickly.

It is documentation only. It does not approve Java behavior changes, Python
behavior changes, diagnostic source changes, Gradle changes, dependency
changes, Minecraft launch, runtime reproduction, commit, or push.

## Scope

```text
Fabric ChatClef bridge command lifecycle
AltoClef UserTaskChain task origin after command completion
StoreInAnyContainerTask / CraftInTableTask / DoStuffInContainerTask loops
Baritone path calculation and adoption evidence
ToolSet, tool-save-policy, InventorySubTracker diagnostic log volume
```

Related documents:

```text
plugins/Minecraft/docs/chatclef-command-lifecycle-and-threading.md
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
plugins/Minecraft/docs/chatclef-baritone-cache-troubleshooting.md
plugins/Minecraft/docs/chatclef-command-payload-map-audit.md
plugins/Minecraft/docs/chatclef-resource-target-retry-thrashing-analysis.md
plugins/Minecraft/docs/chatclef-bare-deposit-container-handoff-loop-investigation.md
```

The 2026-08-19 bare `deposit` incident is a separate evidence set. Its Store
task origin is proven, and its source/log audit corrects several tempting
over-interpretations: fallback `closestContainerPresent=false` is not proof of
scanner absence, `branchChangeCount` is defined as a composite-signature
counter, `costToMakeNew=Infinity` is an intentional policy sentinel, and task
instance evidence must distinguish start/tick from a complete stop/restart
cycle. Use the dedicated incident document rather than transferring this
2026-08-07 hypothesis directly to that run.

## Evidence Snapshot

Primary local logs:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
C:\Vtuber_Souorce_Code\LAVI\logs\20260807_201343_log.txt
```

Observed file growth during the investigation:

```text
latest.log      reached at least 74,309,985 bytes by 20:34:52 KST
stdout-logs.txt reached at least 82,554,880 bytes by 20:34:52 KST
LAVI app log    stopped growing around 20:31:17 KST in this sample
```

The three bridge commands in this window reached terminal command results:

```text
get gold_ingot 10
  Java latest.log: 20:17:57 event=terminal_result_sent
  LAVI log:        20:17:57 status=completed ok=True

get cooked_beef 10
  Java latest.log: 20:23:49 event=terminal_result_sent
  LAVI log:        20:23:49 status=completed ok=True

get iron_ingot 10
  Java latest.log: 20:26:50 event=terminal_result_sent
  LAVI log:        20:26:50 status=completed ok=True
```

The Java terminal payloads included:

```text
task_finished_event_received=true
termination_kind=finished
result_reason=terminal_result_sent
result_fidelity=callback_plus_matching_user_task_event
```

After terminal completion, later boundary logs showed:

```text
commandContextAvailable=false
commandContextError=no_active_command
commandRequestId=
commandCorrelationId=
commandText=
```

This is evidence that the Fabric bridge command context had been cleared. It
is not evidence of a bridge command waiting loop.

## Current Classification

Treat the current symptom as two overlapping problems:

```text
Behavior loop:
    StoreInAnyContainerTask
      -> chest acquisition
      -> CraftInTableTask / DoCraftInTableTask
      -> existing crafting_table routing
      -> Baritone path calculation or adoption retry

Diagnostic log flood:
    the behavior loop repeatedly calls ToolSet, tool-save-policy,
    InventorySubTracker, and container/path diagnostics on hot paths.
```

The best current separation is:

```text
Bridge command lifecycle: completed
Python active command: cleared
AltoClef UserTaskChain: still running another task
Behavior loop owner: Store/Craft/Baritone side
Log flood owner: diagnostic emission and limiter side
```

Do not describe this window as a ChatClef command lifecycle loop unless new
logs show a later command that remains active without a terminal result.

## Leading Behavior Hypothesis

The strongest current hypothesis is a crafting-table arbitration loop while
trying to obtain a chest for `StoreInAnyContainerTask`.

The observed pattern is consistent with:

```text
StoreInAnyContainerTask needs a container
  -> no usable container block item is available
  -> it requests a chest
  -> chest acquisition needs crafting
  -> CraftInTableTask looks for an existing crafting_table
  -> an existing table is within the local "nearby" radius
  -> costToMakeNew=Infinity
  -> existing table is always preferred over making a new one
  -> Baritone cannot produce/adopt/reach a useful path to that table
  -> the same table is selected again
```

The most important log shape is:

```text
topLevelTask=StoreInAnyContainerTask
taskChain=<Storing in any container: ...> Obtaining a chest item ...
taskClass=adris.altoclef.tasks.container.DoCraftInTableTask
reason=return_open_table_task
nearestPosition=-2646, 69, 702
containerTarget=crafting_table
costToWalk=about 144
costToMakeNew=Infinity
hasContainerBlockItem=false
baritonePathing=false
```

The straight-line distance from the observed player position around
`-2624,70,672` to the table at `-2646,69,702` is roughly 37 blocks. That is
inside a 40-block nearby-table check. If `CraftInTableTask.getCostToMakeNew()`
returns `Double.POSITIVE_INFINITY` whenever an existing table is nearby, then
the later comparison:

```text
costToWalk > costToMakeNew
```

will never select "make a new table" for this candidate:

```text
144 > Infinity == false
```

This does not prove the behavior bug by itself. It identifies the first
high-value boundary to verify: distance-nearby table preference must be checked
against actual path result, path adoption result, blacklist state, and
interaction progress.

## Store Task Origin Must Be Proven For This 2026-08-07 Window

Do not assume the Store task is a leftover child from a completed `get`
command. The current evidence fits a newly started AltoClef user or idle task
better than an uncleared Fabric bridge command.

The next logs should prove whether `StoreInAnyContainerTask` came from:

```text
an idleCommand such as deposit/stash
a separate user or test command
an internal callback
a wrong handoff from the completed command
```

Required task-origin evidence:

```text
USER_TASK_CHAIN_TASK_FINISHED_EVENT_PUBLISH_END
USER_TASK_CHAIN_IDLE_COMMAND_BEGIN
USER_TASK_CHAIN_SIGNAL_NEXT_IDLE
USER_TASK_CHAIN_RUN_TASK_ENTER
USER_TASK_CHAIN_RUN_TASK_ASSIGNED
USER_TASK_CHAIN_IDLE_COMMAND_END
```

Required fields:

```text
idleCommand
incomingTask
runningIdleTask
mainTaskAfterIdleCommand
nextTaskIdleFlag
taskOrigin
originCommand
originRequestId
originCorrelationId
```

Expected pattern if the Store task is an idle/background task:

```text
get task publishes TaskFinishedEvent
  -> USER_TASK_CHAIN_IDLE_COMMAND_BEGIN idleCommand=deposit or stash-like command
  -> USER_TASK_CHAIN_RUN_TASK_ENTER incomingTask=StoreInAnyContainerTask
  -> USER_TASK_CHAIN_RUN_TASK_ASSIGNED runningIdleTask=true
```

Only if task-origin logs prove an invalid handoff should command completion
cleanup be treated as the primary fix target.

## Baritone Evidence Still Needed

`baritonePathing=false` in a single boundary event does not prove Baritone is
idle. The path calculation may be in a worker thread, may have completed with
no path, or may have completed with a path that was not adopted.

Group these events by `calculationGeneration` and target position:

```text
BARITONE_GOAL_REQUEST_DECISION
BARITONE_CALCULATION_SCHEDULED
BARITONE_CALCULATION_WORKER_STARTED
BARITONE_PATHFINDER_CALCULATE_STARTED
BARITONE_PATHFINDER_CALCULATE_COMPLETED
BARITONE_PATH_ADOPTION_DECISION
```

Required fields:

```text
requestAccepted
requestRejectedReason
commandGoalSummary
currentExecutorSummary
nextExecutorSummary
inProgressSummary
resultType
resultPathPresent
resultPathSummary
elapsedMillis
adoptionOutcome
currentMatchesResultPath
nextMatchesResultPath
```

Interpretation guide:

```text
resultPathPresent=false
adoptionOutcome=NO_PATH...
    means repeated no-path or failure-to-find-path is likely.

resultPathPresent=true
adoptionOutcome=PATH_PRESENT_NOT_ADOPTED_OR_DISCARDED
    means calculation may succeed but the result is not becoming the active
    path.
```

Also inspect block unreachable and blacklist logs for the table target:

```text
BLOCK_UNREACHABLE_REQUEST
BLOCK_BLACKLIST_STATE_CHANGED
```

Required fields:

```text
targetPosition
requestedAllowedFailures
scannerUnreachableBefore
failureCountBefore
failureCountAfter
allowedFailuresAfter
unreachableAfter
resetApplied
resetReason
currentDistanceSq
bestDistanceSqAfter
```

The important suspected reset pattern is:

```text
targetPosition=-2646,69,702
resetApplied=true
resetReason=DISTANCE_IMPROVED
unreachableAfter=false
```

If that pattern repeats, the target may never become effectively unreachable
because small progress resets the failure count before the blacklist threshold
is reached.

## Diagnostic Log Flood Interpretation

`commandContextAvailable=false` must not be treated as a diagnostic gating
condition in the current implementation. It means only:

```text
there is no active Fabric bridge command context to annotate this event
```

It does not mean:

```text
do not emit this diagnostic event
```

The current log flood likely has two causes:

```text
hot-path behavior loop:
    Store/Craft/Baritone repeatedly calls tool and inventory code.

weak suppression for some diagnostics:
    high-cardinality fingerprints or per-thread/per-caller repeat keys cause
    detail and summary events to continue growing even after the command
    context is empty.
```

Observed high-volume events in this log window included:

```text
TOOL_SAVE_POLICY_SNAPSHOT_CONSUMED               8000+ lines
INVENTORY_SUBTRACKER_DIAGNOSTIC_REPEAT_SUMMARY   2500+ lines
DESTROY_BLOCK_PHASE_TRANSITION                   1200+ lines
BARITONE_GOAL_REQUEST_DECISION                    256 lines
CONTAINER_TASK_TARGET_DECISION/BRANCH             190+ lines
```

Do not interpret `BARITONE_GOAL_REQUEST_DECISION=256` as exactly 256 real
requests without checking `suppressedCount` and repeat summaries. A value of
256 can also be the detail limit per bucket.

## Minimum Future Diagnostic Work

Avoid adding broad raw logs. The current problem already produces too much
output. Future diagnostics should be state-change based or summary based.

High-value diagnostic additions, if source changes are later approved:

```text
1. Task origin at UserTaskChain.runTask()
   - one line per top-level assignment
   - proves BRIDGE_COMMAND vs IDLE_COMMAND vs INTERNAL_CALLBACK vs CONSOLE

2. Store progress state
   - emit only when toStore/notStored/storedCount/currentChildTask changes
   - include noProgressTicks and branch

3. Container target retry summary
   - one summary per stable target after repeated retries
   - include path result, adoption outcome, blacklist reset reason, and
     player distance delta
```

Suggested fields:

```text
taskRunId
taskInstanceId
taskClass
taskOrigin
originCommand
originRequestId
originCorrelationId
idleCommand
runningIdleTask

storeTaskInstanceId
toStore
notStored
inventoryCountByTarget
storedCountByTarget
containerCandidate
chestItemCount
currentChildTask
noProgressTicks
branch

containerAttemptId
targetPosition
routeAttemptNumber
distance
costToWalk
costToMakeNew
calculationGeneration
pathResultType
pathPresent
adoptionOutcome
blacklistFailureCount
blacklistResetReason
playerDistanceDelta
sameTargetRetryCount
```

Do not add more detailed ToolSet, InventorySubTracker, or tool-save-policy
fields before reducing their emission volume. Those diagnostics already show
that the hot path is being exercised.

## Added Diagnostic Events

The follow-up diagnostics added for this investigation are log-only events.
They do not change `command_request`, `command_result`, status values, task
selection, retry, timeout, Baritone goal/path ownership, input state, container
clicks, or cleanup behavior.

```text
USER_TASK_CHAIN_TASK_ORIGIN_DECISION
    owner=user_task_chain
    mode=BOUNDARY
    trigger=runTask_before_assignment
    dedupe_key=user_task_chain_origin|...
    max_emission=one_per_runTask_call
    correlation=incomingTaskIdentity=<identity>
    payload=flat_fields
    terminal=false
    behavior_effect=none
```

Required comparison fields:

```text
previousTask
incomingTask
previousTaskWasRunningIdle
incomingMarkedIdleByNextFlag
incomingWillConsumeNextIdleFlag
incomingTaskOriginHint
incomingCommandHint
originIdleCommand
incomingOnFinishPresent
callerSummary
commandContextAvailable
commandRequestId
commandCorrelationId
commandText
commandContextError
```

This event is emitted before `UserTaskChain.runTask()` consumes
`nextTaskIdleFlag`, so it separates "previous task was idle" from "incoming
task was actually created by the idle command".

```text
STORE_IN_ANY_CONTAINER_START
STORE_IN_ANY_CONTAINER_STOP
    owner=store_in_any_container_observer
    mode=BOUNDARY
    trigger=<start|stop>
    max_emission=one_per_task_lifecycle_boundary
    correlation=storeTaskIdentity=<identity>
    payload=flat_fields
    terminal=true only for STOP
    behavior_effect=none

STORE_IN_ANY_CONTAINER_BRANCH
STORE_IN_ANY_CONTAINER_REPEAT_SUMMARY
STORE_IN_ANY_CONTAINER_DIAGNOSTIC_CAP_REACHED
    owner=store_in_any_container_observer
    mode=BOUNDARY
    trigger=<branch|repeat_summary|session_cap>
    dedupe_key=STORE_IN_ANY_CONTAINER_BRANCH|...
    max_emission=detail_per_bucket=1,session=512,summary_ticks=200
    correlation=storeTaskIdentity=<identity>
    payload=flat_fields
    terminal=false
    behavior_effect=none

STORE_IN_ANY_CONTAINER_PROGRESS_STATE
STORE_IN_ANY_CONTAINER_PROGRESS_DIAGNOSTIC_CAP_REACHED
    owner=store_in_any_container_progress_observer
    mode=BOUNDARY
    trigger=<first_observation|progress_signature_changed|
             progress_stable_summary|session_cap>
    dedupe_key=store_progress|...
    max_emission=first_change_summary_ticks=200,session=256
    correlation=storeTaskIdentity=<identity>
    payload=flat_fields
    terminal=false
    behavior_effect=none
```

Required branch comparison fields:

```text
getIfNotPresent
toStore
notStored
notStoredCount
storedCountByTarget
closestContainerPresent
closestContainerPosition
closestWithinRange
currentTryWithinExtraRange
currentChestTry
dungeonChestCacheSize
nonDungeonChestCacheSize
playerPosition
progressCheckOk
progressFailureWillRequestUnreachable
childTaskClass
requestedItem
requestedCount
containerBlockItem
missingTarget
inventoryNeed
inventoryCount
```

These events are intentionally separate from command lifecycle payloads. They
must remain additive and bounded while the current logs are active evidence.

```text
DEPOSIT_COMMAND_INVOCATION_DECISION
    owner=deposit_command_observer
    mode=BOUNDARY
    trigger=before_runUserTask
    dedupe_key=deposit_command_invocation|<task identity>
    max_emission=one_per_deposit_command_invocation
    correlation=taskToRunIdentity=<identity>
    payload=flat_fields
    terminal=false
    behavior_effect=none
```

This event proves whether `DepositCommand` explicitly created the
`StoreInAnyContainerTask` later seen in `UserTaskChain.runTask()`. It is emitted
before `runUserTask()` and does not change command parsing, task selection, or
completion callback behavior.

```text
CRAFTING_TABLE_ROUTE_RETRY_SUMMARY
    owner=crafting_table_route_observer
    mode=BOUNDARY
    trigger=<first_route_observation|route_retry_summary>
    dedupe_key=crafting_table_route_retry|...
    max_emission=first_and_summary_ticks=200,session=256
    correlation=containerTaskIdentity=<identity>
    payload=flat_fields
    terminal=false
    behavior_effect=none
```

This event summarizes stable `DoCraftInTableTask` crafting-table routing
choices from the already computed `DoStuffInContainerTask` branch fields. It
does not recalculate routes, change `costToMakeNew`, change Baritone goals, or
modify container interaction.

## Behavior Fix Priority, If Later Approved

The current evidence supports this investigation order:

```text
1. Prove why StoreInAnyContainerTask started.
2. Verify CraftInTableTask nearby-table costToMakeNew=Infinity behavior.
3. Verify DoStuffInContainerTask no-path / not-adopted fallback behavior.
4. Verify chest/container acquisition retry budget and explicit failure path.
5. Verify StoreInAnyContainerTask no-progress and termination conditions.
6. Audit DoCraftInTableTask _craftCount only after table-open routing is proven.
```

Do not start by changing command completion cleanup. The current evidence says
the Fabric bridge command lifecycle completed and cleared correctly. Cleanup is
a lower-priority suspect unless task-origin logs prove the Store task was
wrongly inherited from the completed command.

The likely behavior contract needed for crafting-table selection is:

```text
prefer an existing nearby crafting table when it is actually reachable
but after repeated no-path, not-adopted, or interaction failure outcomes,
release the Infinity preference and allow a new table or another candidate
```

The likely behavior contract needed for idle/background storage is:

```text
finish on successful storage
finish when there is nothing to store
fail or abandon explicitly when no valid container can be obtained
fail or abandon explicitly when retry/no-progress budget is exhausted
```

## Log Flood Fix Priority, If Later Approved

Log-volume fixes should be treated as parallel P0 work because they protect
the evidence files and disk usage without changing command completion
semantics.

Preferred direction:

```text
TOOL_SAVE_POLICY_SNAPSHOT_CONSUMED
  -> replace global last-value dedupe with per-key limiter
  -> add repeat summary interval
  -> add session hard cap

ToolSet storage-query diagnostics
  -> reduce repeat-key cardinality
  -> normalize worker thread identity when possible
  -> bucket by redirect point and caller boundary

No-active-command background state
  -> keep lifecycle and terminal events visible
  -> make hot-path detail summary-only when commandContextAvailable=false
```

These changes, if later implemented, must not alter:

```text
Task selection
Task completion
Baritone goal/path state
retry behavior
timeout behavior
input state
container clicks
wire protocol fields
command result status
```

## Current Working Conclusion

For the 2026-08-07 evidence window only, the current best conclusion is:

```text
The bridge command lifecycle is not the active loop.
The active behavior loop is post-completion Store/Craft/Baritone work.
The log flood is a diagnostic-emission problem triggered by that loop.
The leading behavior boundary is crafting table selection where a nearby
existing table forces costToMakeNew=Infinity even when actual pathing or
adoption does not make useful progress.
The Store task origin still must be proven before modifying command cleanup.
```

Keep this distinction stable while reviewing future logs. If field names,
payload shapes, or lifecycle event names are changed during this investigation,
the current evidence chain becomes harder to compare.
