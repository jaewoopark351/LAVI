<!-- 20260806_kpopmodder: Documented the ChatClef task lifecycle diagnostic runbook for command root, child task, and Baritone loop investigations. -->

# ChatClef Task Lifecycle Diagnostics

Date: 2026-08-06

This runbook documents how to investigate Fabric ChatClef / AltoClef task
lifecycle loops before making a behavior fix.

It is documentation only. It does not approve Java changes, Gradle changes,
dependency changes, Minecraft launch, runtime reproduction, cache deletion,
commit, push, or a behavior-changing fix.

## Scope

This applies to the Fabric ChatClef runtime and LAVI bridge:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
plugins/Minecraft/fabric/chatclef/**
plugins/Minecraft/common/dto/**
```

Relevant boundaries:

```text
LAVI command lifecycle
Fabric ChatClef command bridge
UserTaskChain / SingleTaskChain
TaskFinishedEvent publication and observation
Task parent-child selection and replacement
DoCraftInTableTask / DoToClosestBlockTask / InteractWithBlockTask
MineAndCollectTask / DestroyBlockTask
Baritone custom goal and path ownership
container open and click observation
tool selection and block breakability observation
```

This runbook complements, but does not replace:

```text
chatclef-command-lifecycle-and-threading.md
chatclef-command-payload-map-audit.md
chatclef-baritone-cache-troubleshooting.md
chatclef-carryon-integration-direction.md
fabric-chatclef-bridge-protocol-v1.md
```

## When To Use This

Use this runbook when the game is running and tasks are ticking, but a command
does not converge to a clear terminal result.

Observed examples include:

```text
command result becomes unknown with result_reason=task_identity_mismatch
TaskFinishedEvent observation reports termination_kind=cancelled_without_task
Minecraft continues running a task after LAVI reports an unknown terminal result
command lifecycle remains waiting_for_terminal_condition for a long time
deadline_ms is null or no deadline_exceeded event is present
parent Task repeatedly returns the same kind of child Task
DestroyBlockTask repeats around the same ore or block target
DoCraftInTableTask repeatedly returns return_open_table_task
screenName=none while a container-open child keeps being returned
baritonePathing=false while customGoalActive toggles or remains active
Baritone repeatedly calculates long paths or large movement/node sets
the same command behaves differently on another world or after a world reset
```

Do not treat this pattern as a build or Mixin failure unless current logs show
current startup or runtime evidence such as:

```text
critical Mixin injection failure
NoSuchMethodError / NoSuchFieldError / LinkageError
class loading failure
Minecraft crash report
fatal exception before ChatClef task ticking begins
```

Past build or Mixin failures are context, not proof for the current symptom.

## Preserve Existing Payload Shapes

The existing command lifecycle, ownership, and task observation payloads are
active diagnostic evidence. Do not rename keys or change map shapes while they
are being used to diagnose a lifecycle failure.

If new information is needed, prefer additive diagnostic events or additive
fields in a clearly scoped log-only payload. Do not change the v1 bridge wire
contract, command result status values, request keys, or Python DTO contract.

In particular, do not change these existing meanings while investigating:

```text
request_id
correlation_id
session_id
connection_generation
normalized_command
request_command
result_reason
result_fidelity
ownership
task_before_dispatch
task_after_dispatch
bound_root_task
terminal_task
task_finished_event_received
task_finished_observation
elapsed_ms
deadline_ms
deadline_exceeded
current_task_matches_bound_root_task
```

## First Separation

Do not merge these into one theory too early:

```text
Bridge lifecycle observation problem:
    LAVI may attach a stale, null, or unrelated TaskFinishedEvent to the
    current command.

In-game task loop problem:
    ChatClef / AltoClef may repeatedly replace or restart a child task, lose
    Baritone path ownership, or keep pursuing a target that never reaches the
    interaction or break boundary.
```

They can happen together, but they need separate evidence.

## Investigation Order

### 1. Confirm The Current Log Window

Record file sizes and modified times for:

```text
latest.log
stdout-logs.txt
instance_audit.txt
LAVI logs directory
```

Record the exact command context:

```text
command
request_id
correlation_id
session_id
connection_generation
accepted_at_ms
dispatch_started_ms
deadline_ms
```

If logs are large, use the command accept time and request id to isolate the
window. Do not summarize the symptom from a screenshot alone.

### 2. Rule Out Current Build Or Crash Failure

Search the current log window for current fatal errors:

```text
Critical injection failure
NoSuchMethodError
NoSuchFieldError
LinkageError
Minecraft has crashed
Caused by:
[.../FATAL]
```

Generic mod startup warnings or data fixer messages do not by themselves prove
the current task loop is a build failure.

### 3. Check Deadline And Terminal State

Capture:

```text
deadline_ms
deadline_exceeded
elapsed_ms
task_finished_event_received
finish_callback_received
result_reason
failure_type
failure_message
bound_root_task
current_task
current_task_matches_bound_root_task
this_or_child_timed_out
```

If `deadline_ms=null`, the command has no LAVI command-level deadline. A long
elapsed time is not evidence that a timeout failed.

Task-local progress checks, movement checks, and blacklist behavior are
separate from command-level timeout.

### 4. Verify Root Assignment

Add or inspect a root assignment ledger before modifying behavior.

Required event examples:

```text
USER_TASK_ROOT_ASSIGNED
USER_TASK_ROOT_CANCEL_REQUESTED
USER_TASK_ROOT_STOPPED
USER_TASK_ROOT_FINISH_PUBLISHED
```

Required fields:

```text
requestId
correlationId
sessionId
connectionGeneration
normalizedCommand
clientTick
eventSequence
rootGeneration
previousRootClass
previousRootIdentity
incomingRootClass
incomingRootIdentity
assignedRootClass
assignedRootIdentity
taskActive
taskStopped
assignmentReason
callerOrigin
rootBeforeStop
rootAfterStop
taskPassedToOnTaskFinish
cancelCaller
cancelReason
```

Use `System.identityHashCode(task)` or the existing identity convention to
distinguish actual object instances.

Evidence to look for:

```text
get command dispatches a non-get root task
expected root is replaced by another user task root
cancel happens immediately before a null TaskFinishedEvent
root identity changes without an expected command or owner
```

### 5. Correlate TaskFinishedEvent Queue

`task_identity_mismatch` and `cancelled_without_task` do not prove that the
current Minecraft task actually failed. They may indicate a stale or null
TaskFinishedEvent was attached to the active command.

Log the enqueue and dequeue sides of task finish observation:

```text
TASK_FINISHED_EVENT_ENQUEUED
TASK_FINISHED_EVENT_DEQUEUED
TASK_FINISHED_EVENT_CLASSIFIED
```

Required fields:

```text
observationSequence
queueDepthBefore
queueDepthAfter
observedAtMs
observedClientTick
dispatchStartedMs
dispatchStartedTick
observationAgeMs
eventTaskPresent
eventTaskClass
eventTaskIdentity
terminationKind
activeRequestId
activeConnectionGeneration
boundRootClass
boundRootIdentity
currentUserRootClass
currentUserRootIdentity
eventPredatesDispatch
matchesBoundRootByIdentity
matchesCurrentRootByIdentity
matchesRootGeneration
classification
```

Fast classification:

```text
observedAtMs < dispatchStartedMs
    -> stale event attached to a newer command is strongly suspected

eventTaskPresent=false immediately after a cancel path
    -> null TaskFinishedEvent publication path is suspected

matchesBoundRootByIdentity=false and matchesCurrentRootByIdentity=true
    -> bound root may be stale or replaced

matchesBoundRootByIdentity=false and matchesCurrentRootByIdentity=false
    -> unrelated or stale observation is suspected
```

### 6. Compare Parent Target, Child Target, And Baritone Goal

For container and mining loops, the parent task's selected target is not enough.
The child task and Baritone may be pursuing a different target.

Log only when a target or owner changes, plus bounded summaries.

Required fields:

```text
parentClass
parentIdentity
parentNearestPosition
parentCachedContainerPosition
parentSelectedTarget
parentSelectionReason
returnedChildClass
returnedChildIdentity
returnedChildTarget
existingChildClass
existingChildIdentity
existingChildTarget
interactWithBlockTarget
destroyBlockTarget
baritoneGoalTarget
baritonePathEndpoint
targetBlockState
targetChunkLoaded
straightLineDistance
actualCalculatedPathLength
worldHelperCanReach
scannerMarkedUnreachable
blacklistState
```

Decision signals:

```text
parent target differs from child target
    -> target handoff boundary is suspect

child target changes repeatedly
    -> target oscillation is suspect

Baritone goal differs from child target
    -> path ownership or goal replacement boundary is suspect

straight distance is low but path length is high
    -> terrain, cache, or heuristic mismatch is suspect
```

### 7. Log Child Retain Or Replace Decisions

Repeated child object creation in logs is not by itself a bug. Task equality may
retain an existing child. The decisive boundary is whether the active child is
retained, replaced, cleared, interrupted, or stopped.

Suggested event:

```text
TASK_CHILD_DECISION
```

Required fields:

```text
parentClass
parentIdentity
existingChildClass
existingChildIdentity
returnedChildClass
returnedChildIdentity
isEqualResult
canInterrupt
decision
decisionReason
existingChildLifetimeTicks
returnedChildTarget
existingChildTarget
```

Expected decision values:

```text
RETAIN_EXISTING_CHILD
REPLACE_CHILD
CLEAR_CHILD
INTERRUPT_CHILD
NO_CHILD
```

If the same target causes continuous `REPLACE_CHILD`, inspect task equality and
target construction. If the same child is retained but pathing restarts,
inspect Baritone ownership.

For the current gold-ingot mining loop, distinguish candidate allocation from
actual active-child replacement:

```text
MineOrCollectTask may create a new DestroyBlockTask candidate every tick.
Task.tick() may still retain the active child when isEqual() reports true.
Repeated return-task logs are not proof of real child churn.
```

Use a more specific event name for this investigation:

```text
TASK_CHILD_RECONCILIATION
```

Additional fields:

```text
activeChildBeforeInstanceId
candidateChildInstanceId
isEqualResult
replacementApplied
activeChildAfterInstanceId
previousChildStopCalled
candidateDiscardedBecauseEqual
activeChildBeforeTargetPosition
candidateTargetPosition
activeChildAfterTargetPosition
```

Do not include candidate instance ID, tick, timestamp, or opaque `toString()`
values in the dedupe fingerprint. Candidate instance IDs may be emitted as
fields, but they should not make unchanged semantic state look like a new
state every tick.

### 8. Log Interact Lifecycle Boundaries

For `InteractWithBlockTask` and tasks that compose it, log lifecycle changes
instead of every tick.

Suggested event:

```text
INTERACT_LIFECYCLE
```

Required fields:

```text
phase
interactIdentity
target
targetBlockState
lifetimeTicks
parentClass
parentIdentity
interruptTask
baritonePathingBefore
baritonePathingAfter
customGoalActiveBefore
customGoalActiveAfter
forceCancelCalled
setGoalAndPathCalled
```

Suggested phases:

```text
START
STOP
TARGET_CHANGED
STATUS_CHANGED
CLICK_STATUS_CHANGED
```

If `START` and `STOP` repeat rapidly for the same target, child replacement or
interruption is suspect. If they do not repeat but pathing restarts, inspect
Baritone process ownership and lost-control events.

### 9. Log Baritone Path Ownership

Do not cancel or restart Baritone for diagnostics. Observe existing calls.

Suggested event:

```text
BARITONE_PATH_OPERATION
```

Required fields:

```text
pathOperationId
pathEvent
callerTaskClass
callerTaskIdentity
target
goal
customGoalActiveBefore
customGoalActiveAfter
pathingBefore
pathingAfter
processInControl
cancelCaller
calculatedPathLength
movementCount
openSetSize
pathNodeMapSize
elapsedMs
```

Suggested path events:

```text
SET_GOAL_AND_PATH
LOST_CONTROL
FORCE_CANCEL
PATH_CALCULATION_STARTED
PATH_CALCULATION_COMPLETED
PATH_EXECUTION_STARTED
PATH_FAILED
PATH_CANCELLED
PATH_STATE_CHANGED
```

Decision signals:

```text
same child identity but repeated SET_GOAL_AND_PATH
    -> Baritone process state or lost-control boundary is suspect

FORCE_CANCEL immediately follows child replacement
    -> Task replacement may be destroying path progress

large path calculation repeatedly targets the same endpoint
    -> terrain, cache, or target-selection boundary is suspect
```

### 9.1 DestroyBlockTask To Baritone Path Boundary

For the current gold-ingot loop, the highest-priority boundary is not command
lifecycle, `TaskFinishedEvent`, or `DestroyBlockTask.isFinished()`.

The highest-priority boundary is:

```text
DestroyBlockTask owns or observes an active Baritone custom goal
  -> Baritone has an actually present/adopted path
  -> Baritone pathing starts or the no-path/failure state is observable
```

The suspicious state is:

```text
customGoalActive=true
baritonePathing=false
pathPresent unknown
goalMatchesCurrentTarget unknown
calculation state unknown
active process owner unknown
```

Do not assume `DestroyBlockTask.isFinished()` is wrong when the target block is
still present. For a target block state such as `minecraft:deepslate_gold_ore`,
`isFinished()` returning false is expected. The failure boundary is navigation
to the target, path adoption, or progress observation before block completion.

Suggested event:

```text
DESTROY_NAVIGATION_STATE_TRANSITION
```

Required fields:

```text
targetPosition
targetBlockId
targetBlockState
blockStillExists
chunkLoaded
worldCanBreak
playerPosition
distanceSq
horizontalDistanceSq
verticalDelta
reachPresent
playerOnGround
playerTouchingWater
foodChainNeedsToEat
inNetherPortal
safeToCancel
navigationState
customGoalActive
baritonePathing
pathPresent
currentMovementPresent
ticksRemainingInSegment
goalType
goalSummary
goalMatchesTarget
stateEnteredTick
stateElapsedTicks
```

Suggested `navigationState` values:

```text
PATHING_ACTIVE
IN_RANGE_BREAK_GATE
WAIT_CUSTOM_GOAL_ACTIVE_NO_PATHING
SET_GOAL_AND_PATH_OBSERVED
NO_GOAL_PATH_ABSENT
UNSTUCK_TASK_ACTIVE
PORTAL_ESCAPE
```

Emit immediately when this state first appears:

```text
customGoalActive=true
baritonePathing=false
pathPresent=false
reachPresent=false
```

If the state is unchanged, emit only a bounded summary at most once per 200
game ticks or 10 seconds per correlation.

Suggested Baritone path-state event:

```text
BARITONE_GOAL_PATH_TRANSITION
```

Required fields:

```text
transition
goalType
goalSummary
goalMatchesTarget
customGoalActive
baritonePathing
pathPresent
currentMovementPresent
ticksRemainingInSegment
planningStartTick
planningElapsedTicks
existingCancellationReason
existingFailureReason
activeProcessOwner
```

Suggested `transition` values:

```text
GOAL_SUBMITTED
GOAL_BECAME_ACTIVE
CALCULATION_STARTED
CALCULATION_SUCCEEDED
CALCULATION_FAILED
PATH_BECAME_PRESENT
PATHING_STARTED
PATHING_STOPPED
PATH_DISAPPEARED
GOAL_CLEARED
```

Use only public read-only Baritone state in the first pass. Do not touch
Baritone source only to expose internal calculation state. If public snapshots
cannot distinguish calculating, failed, and active-idle states, report the
first-pass result before proposing one minimal Baritone process transition
logging hunk.

### 9.2 Existing Force-Cancel Boundaries

Observe existing `forceCancel()` calls before and after. Do not add, move,
remove, or reorder cancellation calls for diagnostics.

Suggested event:

```text
BARITONE_EXISTING_CANCEL_BOUNDARY
```

Known sources to distinguish:

```text
DESTROY_ON_START
DESTROY_ON_STOP
MINE_OR_COLLECT_PROGRESS_FAILURE
```

Required fields:

```text
cancelSource
targetPosition
baritonePathingBefore
customGoalActiveBefore
pathPresentBefore
currentMovementPresentBefore
ticksRemainingInSegmentBefore
goalTypeBefore
goalSummaryBefore
goalMatchesTargetBefore
baritonePathingAfter
customGoalActiveAfter
pathPresentAfter
currentMovementPresentAfter
ticksRemainingInSegmentAfter
goalTypeAfter
goalSummaryAfter
goalMatchesTargetAfter
```

Important evidence pattern:

```text
before: pathing=true, customGoalActive=true
forceCancel
after:  pathing=false, customGoalActive=true
same target immediately reselected
```

That pattern points to the boundary between existing cancellation,
reselection, and Baritone active-idle/path adoption. It does not by itself
authorize a new cancellation, retry, timeout, or blacklist change.

### 10. Container Open And Click Boundary

For container loops such as crafting table or furnace open attempts, capture the
click-to-GUI transition.

Suggested event:

```text
CONTAINER_INTERACTION_OBSERVATION
```

Required fields:

```text
interactIdentity
target
containerTarget
reachable
lookingAtTarget
crosshairHitType
crosshairBlockPos
tryPressAccepted
rightClickHeld
playerSneaking
sneakHeld
mainHandItem
offHandItem
screenName
screenHandlerClass
screenHandlerSyncId
currentScreenChanged
carryState
clickStatusBefore
clickStatusAfter
observationWindowTick
```

Suggested click status changes:

```text
CANT_REACH -> WAIT_FOR_CLICK
WAIT_FOR_CLICK -> CLICK_ATTEMPTED
CLICK_ATTEMPTED -> GUI_OPEN
CLICK_ATTEMPTED -> OBSERVATION_WINDOW_EXPIRED
```

Decision signals:

```text
CLICK_ATTEMPTED never appears
    -> movement, reachability, or look boundary is suspect

CLICK_ATTEMPTED and tryPressAccepted=true but no GUI opens
    -> interaction packet, input, server response, or Carry On interception is suspect

screenName=none and screenHandlerClass=PlayerScreenHandler
    -> no container GUI is currently open
```

### 11. Mining And DestroyBlock Boundary

For mining loops such as `DestroyBlockTask` repeating around an ore target,
capture the target and progress state when it changes or fails progress checks.

Suggested event:

```text
DESTROY_BLOCK_TARGET_STATE
```

Required fields:

```text
destroyTaskIdentity
targetPosition
targetBlockState
targetStillExists
targetChunkLoaded
playerPosition
distanceToTarget
reachPresent
reachable
lookingAtTarget
breakingBlockState
breakProgress
selectedTool
selectedToolDamage
selectedToolMaxDamage
toolDecisionReason
toolSavePolicyDecision
canMineWithSelectedTool
baritoneGoalTarget
baritonePathing
customGoalActive
progressCheckerState
progressCheckerReason
unreachableRequestState
blacklistState
```

Decision signals:

```text
same target, same child retained, no break progress
    -> reach, look, input, or block-breakability boundary is suspect

targetStillExists=false while task keeps targeting it
    -> world scanner or cache state is suspect

progress checker repeatedly blacklists nearby targets
    -> target selection and blacklist ownership are suspect

low durability tool is selected for required ore
    -> tool saver and mining requirement boundary is suspect
```

### 12. Mining Target, Progress, And Blacklist Events

For the gold-ingot mining loop, add separate events for target selection,
movement progress, unreachable requests, and actual blacklist state. Do not
combine these with command lifecycle payloads.

Suggested event:

```text
MINE_TARGET_SELECTION_TRANSITION
```

Required fields:

```text
previousPursuitType
previousPursuitPosition
closestBlockPosition
closestBlockDistanceSq
closestDropEntityId
closestDropPosition
closestDropDistanceSq
selectedPursuitType
selectedPursuitPosition
targetChanged
selectionReason
targetBlockId
targetBlockState
blockStillMatchesRequestedType
chunkLoaded
worldCanBreak
scannerUnreachable
localBlacklistContains
heuristicCachePresent
cachedHeuristic
cachedBestDistanceSq
```

Suggested `selectionReason` values:

```text
INITIAL_SELECTION
CURRENT_PURSUIT_UNCHANGED
CURRENT_PURSUIT_INVALIDATED
NEW_CLOSEST_NOT_IN_HEURISTIC_CACHE
CACHED_HEURISTIC_BETTER
CONSIDERABLY_CLOSER
DROP_CLOSER
INTERACTION_PAUSED_DROP_ONLY
NO_CANDIDATE
```

Suggested event:

```text
MOVEMENT_PROGRESS_CHECK_RESULT
```

Required fields:

```text
checkerOwner
checkerCallIndex
checkEvaluated
checkResult
failureTransition
mode
lastResetTick
elapsedTicksSinceReset
resetReason
playerPosition
startPlayerPosition
playerDisplacementSinceReset
controllerBreakingBlock
breakingBlockPosition
breakingProgress
lastBreakingBlockPosition
lastBreakingBlockNowAir
distanceTimeoutSeconds
minimumDistance
mineTimeoutSeconds
minimumMineProgress
allowedAttempts
moveCheckFirstEvaluated
moveCheckFirstResult
stuckCheckEvaluated
stuckCheckResult
moveCheckSecondEvaluated
moveCheckSecondResult
```

Required `checkerOwner` values:

```text
DESTROY_MOVE
DESTROY_STUCK
MINE_OR_COLLECT
```

For `DestroyBlockTask`, preserve the existing two `_moveChecker.check()` calls
and short-circuit ordering. Label their observed results with
`checkerCallIndex`. Do not call `check()` an extra time only to fill a log
field.

Suggested event:

```text
BLOCK_UNREACHABLE_REQUEST
```

Required fields:

```text
requestSource
targetPosition
targetBlockId
requestedAllowedFailures
scannerUnreachableBefore
playerPosition
distanceSq
currentMiningRequirement
activeDestroyTaskInstanceId
candidateDestroyTaskInstanceId
customGoalActive
baritonePathing
pathPresent
```

Suggested `requestSource` values:

```text
MINE_OR_COLLECT_PROGRESS_FAILURE
DESTROY_MOVE_CHECK_FAILURE
DESTROY_WATER_FAILURE
PILLAGER_WOOL
```

Suggested event:

```text
BLOCK_BLACKLIST_STATE_CHANGED
```

Required fields:

```text
entryCreated
failureCountBefore
failureCountAfter
allowedFailuresBefore
requestedAllowedFailures
allowedFailuresAfter
unreachableBefore
unreachableAfter
currentDistanceSq
bestDistanceSqBefore
bestDistanceSqAfter
currentMiningRequirement
bestToolBefore
bestToolAfter
resetApplied
resetReason
```

Suggested `resetReason` values:

```text
NEW_ENTRY
DISTANCE_IMPROVED
TOOL_IMPROVED
DISTANCE_AND_TOOL_IMPROVED
NONE
```

Important blacklist interpretation:

```text
requestBlockUnreachable(pos, 2)
    -> not unreachable until failureCount > 2, unless reset logic changes state

requestBlockUnreachable(pos)
    -> not unreachable until failureCount > 4, unless reset logic changes state
```

The local `MineOrCollectTask` blacklist set is not sufficient proof that target
selection excludes a block. Log the actual scanner or blacklist before/after
state before considering a blacklist behavior change.

### 13. Tool Boundary Is Secondary Until Reach

For the observed deepslate-gold case, tool saver is not the first suspected
boundary while the target remains far below the player and `reachPresent=false`.

Treat tool selection as the primary boundary only after:

```text
target is stable
Baritone goal matches target
pathing or path adoption reaches the target
reachPresent=true
breaking does not start or progress remains zero
```

Then reuse existing tool diagnostics where possible and add only missing fields
such as:

```text
requiredMiningRequirement
selectedToolSlot
selectedToolItem
selectedToolDamage
selectedToolMaxDamage
selectedToolSuitable
saveToolDecision
saveToolReason
selectionOutcome
```

## Current Gold-Ingot Loop Checklist

For the local `get gold_ingot 10` symptom, current logs already prove:

```text
command is running, not crashed
deadline_ms=null
no deadline_exceeded terminal result
root task can remain CollectGoldIngotTask
child chain reaches MineAndCollectTask and DestroyBlockTask
material target is [raw_gold] x 10
inventoryMaterialCount can remain below required count
targetBlockState can be minecraft:deepslate_gold_ore
baritonePathing can be false while customGoalActive is true
```

This is currently expected task-chain behavior:

```text
CollectGoldIngotTask chooses SmeltInFurnaceTask in the overworld.
SmeltInFurnaceTask requests material collection when raw gold is insufficient.
raw_gold=5 and materialsNeeded=10 means MineAndCollectTask -> DestroyBlockTask
is a normal child path, not a command lifecycle failure by itself.
```

Current logs do not yet prove:

```text
whether per-tick DestroyBlockTask candidates become real active-child replacement
whether InteractWithBlockTask START/STOP repeats
whether Baritone goal target equals the DestroyBlockTask target
whether a Baritone path is present or adopted after the custom goal is active
whether the Baritone state is calculating, failed, active-idle, or controlled by another process
whether existing forceCancel calls leave customGoalActive=true but pathing=false
whether setGoalAndPath is cancelled by child replacement, lost control, or another owner
whether blacklist requests become real target exclusion
whether progress checker failure is distance progress, mine progress, or stuck logic
whether the target block still exists every time it is selected
whether reach/look/click/break progress ever advances
whether tool-save policy changes the mining decision
whether old TaskFinishedEvent observations are attached to the active command
```

Do not fix this by adding a command timeout. A timeout would only hide the
failure boundary unless the owner of timeout and cleanup has been proven.

## Current Container Loop Checklist

For the local crafting-table/container symptom, current logs may show:

```text
topLevelTask=StoreInAnyContainerTask
DoCraftInTableTask returns return_open_table_task
screenName=none
screenHandlerClass=PlayerScreenHandler
container target is crafting_table
costToMakeNew=Infinity
hasContainerBlockItem=true
Baritone calculates a long path despite a nearby straight-line target
```

Current logs do not yet prove:

```text
whether the root task was assigned by the expected command
whether the parent-selected table equals the child interaction target
whether Baritone goal equals the child target
whether click was attempted
whether click input was accepted
whether GUI open was observed after the click
whether child replacement repeatedly force-cancelled pathing
```

Do not assume Carry On is the primary cause when carry state is
`AVAILABLE_NOT_CARRYING` and no click attempt or Carry On state transition
evidence is present.

## Bounded Logging Rules

All events in this runbook must follow the scoped diagnostics-only policy:

```text
log boundary events
log material state changes
log terminal, cancellation, exception, and refusal reasons
log bounded summaries for repeated unchanged state
do not emit every tick unchanged
do not emit every Baritone node or movement expansion
do not include candidate instance IDs, ticks, timestamps, or random operation IDs in dedupe fingerprints
do not add a retry, timeout, blacklist, fallback, cleanup, or cancellation
do not change Task selection, isFinished, isEqual, return values, or ordering
do not call side-effecting Minecraft, Baritone, Carry On, input, or container APIs
  merely to fill a log field
```

Use the same request and correlation identifiers across command, root task,
child task, path operation, click observation, exception, and terminal events.

## Decision Table

```text
Observed result:
    observedAtMs < dispatchStartedMs
Suspected boundary:
    stale TaskFinishedEvent attached to a newer command

Observed result:
    eventTaskPresent=false immediately after cancel
Suspected boundary:
    UserTaskChain cancel / finish publication lost the task reference

Observed result:
    command is get gold_ingot but root becomes StoreInAnyContainerTask
Suspected boundary:
    wrong root assignment, root replacement, stale observation, or wrong runtime jar

Observed result:
    same parent root, child target changes repeatedly
Suspected boundary:
    target oscillation or child replacement

Observed result:
    same child identity, repeated setGoalAndPath
Suspected boundary:
    Baritone process ownership, lost control, or path state reset

Observed result:
    CLICK_ATTEMPTED never appears
Suspected boundary:
    movement, reachability, look, or target selection

Observed result:
    CLICK_ATTEMPTED and tryPressAccepted=true, but no GUI opens
Suspected boundary:
    interaction packet, input, server response, or Carry On interception

Observed result:
    targetStillExists=false while task keeps targeting the block
Suspected boundary:
    scanner, cached block state, or Baritone/world cache

Observed result:
repeated long path calculations after world replacement
Suspected boundary:
    Baritone disk cache or map-specific terrain/pathing

Observed result:
    customGoalActive=true, baritonePathing=false, pathPresent=false, reachPresent=false
Suspected boundary:
    DestroyBlockTask custom goal to Baritone path adoption

Observed result:
    candidate ID changes every tick, active child ID is stable, isEqual=true, replacement=false
Suspected boundary:
    diagnostic fingerprint or candidate allocation noise, not real Task churn

Observed result:
    active child ID changes repeatedly for the same or alternating target
Suspected boundary:
    real child replacement, target reselection, or equality mismatch

Observed result:
    unreachable request repeats but unreachableAfter=false
Suspected boundary:
    blacklist threshold not reached or distance/tool reset is clearing failure count

Observed result:
    goal matches target, custom goal active, path absent, calculation state unavailable
Suspected boundary:
    Baritone calculation, no-path, or active-idle state needs read-only observation
```

## Behavior Fix Gate

Before proposing a root-cause patch, prove:

```text
last successful boundary
first failing boundary
owner of the state to be changed
root command and root task identity
parent task and child task identity
target handoff from parent to child
Baritone goal/path owner
input or click owner, if relevant
timeout owner, if relevant
cleanup owner, if relevant
fallback behavior that must remain unchanged
```

If any of these remain unknown, stop at diagnostics.

Do not apply these as speculative fixes:

```text
add a command timeout
add a retry loop
add a blacklist or cooldown
globally cancel Baritone pathing
globally release input
change InteractWithBlockTask.isFinished()
change TaskRunner or chain scheduling
change DestroyBlockTask.isFinished()
change DestroyBlockTask._moveChecker.check() order
change blacklist thresholds
add, remove, move, or reorder existing forceCancel calls
touch Baritone source in the first diagnostic pass
change payload field names or status values
delete Baritone cache while Minecraft is running
catch and suppress Throwable around observed engine behavior
```

## Related Documents

```text
chatclef-command-payload-map-audit.md
    Existing command lifecycle payload shape and typing guardrails.

chatclef-baritone-cache-troubleshooting.md
    Operational checks for world replacement, Baritone disk cache, and
    long-running pathing or mining symptoms.

chatclef-carryon-integration-direction.md
    Engine boundary, ownership, diagnostics-only, and root-cause patch gates.

fabric-chatclef-bridge-protocol-v1.md
    Stable bridge request/result wire protocol.
```
