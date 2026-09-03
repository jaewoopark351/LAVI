<!-- 20260806_kpopmodder: Documented the ChatClef task lifecycle diagnostic runbook for command root, child task, and Baritone loop investigations. -->
<!-- 20260831_openai: Linked the post-checkpoint shared diagnostics-admission, bounded terminal-accounting, and separated validation direction. -->
<!-- 20260903_openai: Added the exact GUI-bound, three-later-client-tick lifecycle audit and pre-change report gate. -->
<!-- 20260903_kpopmodder: Converted the GUI-gate pre-change stop into a continuous implementation, bounded-log, and verification ledger. -->
<!-- 20260903_openai: Aligned the GUI gate with open-child quiescence, one-shot interaction correlation, full reachable-path suppression, exact route types, explicit boundary serials, and one-time permission consumption. -->
<!-- 20260903_kpopmodder: Defined bounded source-to-hub-to-coordinator diagnostics for the live inter-tick ScreenOpenEvent TAIL gap. -->
<!-- 20260903_kpopmodder: Corrected runtime evidence and made the screen-dispatch schema, budgets, summaries, and test isolation implementable. -->

# ChatClef Task Lifecycle Diagnostics

Date: 2026-08-06

This runbook documents how to investigate Fabric ChatClef / AltoClef task
lifecycle loops before making a behavior fix.

It is documentation, not a work request by itself. When the user directly asks
for the exact container GUI-bound three-later-tick feature, that request follows
the continuous implementation workflow in the canonical Carry On direction:
implementation, bounded-log reinforcement, tests, and clean-build verification
without per-stage approval pauses. Dependency/version changes, external runtime
mutation, cache deletion, commit, and push remain outside that implicit scope.

## Scope

This applies to the Fabric ChatClef runtime and LAVI bridge:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
plugins/Minecraft/fabric/chatclef/**
plugins/Minecraft/common/dto/**
```

The generic lifecycle runbook may inspect the common DTO layer, but the exact
container GUI stabilization behavior does not add or change a common DTO,
wire-protocol field, message ordering, status value, or acknowledgement.

Relevant boundaries:

```text
LAVI command lifecycle
Fabric ChatClef command bridge
UserTaskChain / SingleTaskChain
TaskFinishedEvent publication and observation
Task parent-child selection and replacement
DoCraftInTableTask / DoStuffInContainerTask / DoToClosestBlockTask / InteractWithBlockTask
SmeltInFurnaceTask furnace arbitration and make-new cost decisions
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

### 10. Container World-Click, GUI Binding, Stability, And Slot-Input Boundaries

For a container loop, do not collapse world reach, click acceptance, a screen
callback, exact GUI ownership, three-boundary stability, and a slot mutation
into one `GUI_OPEN` result. They are separate lifecycle boundaries.

This section records the 2026-09-03 implementation contract. A direct user
implementation request covers its repository-local source/config/test changes,
bounded logging, focused tests, and required clean build as one continuous
workflow. External runtime, deployment, commit, and push remain outside that
scope unless the active request names them.

Current evidence status: the working-tree implementation, repository tests,
and clean build passed, but the matching-JAR regular-furnace runtime failed
before any accepted exact-gate TAIL candidate was recorded. The requested GUI
became live independently; the first failing source/hub/gate boundary remains
directly unobserved. Incident counts and the next diagnostics-only work are in
[implementation ledger Section 18](chatclef-exact-container-gui-three-tick-implementation-ledger-2026-09-03.md#18-post-build-furnace-runtime-reproduction-and-diagnostic-reinforcement-plan).

#### Fixed Lifecycle Model

Suggested diagnostic phase names, not prescribed Java enum names:

```text
OPEN_ATTEMPT_CREATED
WORLD_OPEN_REQUESTED
WORLD_CLICK_OBSERVED
SCREEN_TAIL_CANDIDATE
OPEN_CHILD_QUIESCING
OPEN_CHILD_QUIESCENT
GUI_BOUND
GUI_STABILIZING_0
GUI_STABILIZING_1
GUI_STABILIZING_2
GUI_INPUT_ALLOWED
SLOT_ACTION_ISSUED
TERMINAL
```

The fixed operation order is:

```text
one route-owned normal world right-click for one open attempt
-> observe exactly one matching BlockInteractEvent for that attempt
-> raw ScreenOpenEvent TAIL source observation
-> only active-serial and exact-correlation acceptance creates the candidate,
   whose snapshot consumes that event once
-> route parent stops or quiesces the open child through its normal lifecycle so it cannot click again
-> operation-owned normal open-child cleanup completes
-> exact target/world/dimension/screen/handler/syncId/operation-attempt binding
-> do not count TAIL-candidate tick K or GUI_BOUND-promotion tick B
-> validate the same binding at three distinct later client-tick boundaries
-> transition permission to GUI_INPUT_ALLOWED
-> next normal Task evaluation fully revalidates and commits permission consumption with transfer entry
-> allow the existing route-owned screen-local slot action
```

A retry creates a new `openAttemptId` and `correlationId`; it inherits no
binding, count, or permission from the previous attempt.

`GUI_BOUND` is not allowed while the open child can still reach another world
interaction. The parent must first observe the exact matching
`BlockInteractEvent` once, require the matching TAIL candidate to consume that
pending match once, and let the existing child replacement/stop lifecycle reach
a quiescent state. Quiescent means that
the old child is no longer tickable by the route, has no pending route-owned
right-click, and cannot be returned again for the same attempt. This is not
permission to change generic `InteractWithBlockTask.isFinished()`, equality, or
global input cleanup.

The phrase “Shift right-click after three ticks” must not be used as an
implementation instruction. The gate does not authorize another
`interactBlock`, physical SNEAK injection, right-button synthesis, or a change
to the existing slot action. Trusted-home `QUICK_MOVE`/button 0 and current
storage/furnace `PICKUP`/button contracts remain route-specific.

#### First-Pickup Limitation

The post-open gate begins only after `GUI_BOUND`. If Carry On server-side
`keyPressed` is stale true, the first normal world click may be consumed as a
pickup. Then no requested GUI opens, `GUI_BOUND` does not exist, and no
stabilization phase starts.

Deterministic prevention of that initial pickup requires a separate Carry On
policy such as exact `forbiddenTiles` configuration or a dedicated Carry key.
This post-open task does not silently include such a configuration or key
change. Do not report a
post-open three-boundary gate as proof that the initial pickup problem is
solved.

The existing `DoStuffInContainerTask` post-placement SNEAK wait is a separate
pre-GUI bounded handoff. It currently can proceed after its budget is
exhausted. It is not the new fixed post-GUI gate and is not deterministic
Carry On pickup prevention.

#### Raw ScreenOpenEvent TAIL And Accepted Candidate Are Distinct

Current source facts:

```text
ClientOpenScreenMixin
    publishes ScreenOpenEvent at MinecraftClient.setScreen HEAD and TAIL

ScreenOpenEvent
    contains only screen and preOpen

ClientInteractWithBlockMixin / BlockInteractEvent
    expose a BlockHitResult interaction boundary
    do not expose owning operation or attempt identity
```

Therefore `preOpen=false` proves only that `MinecraftClient.setScreen` reached
TAIL for that requested screen argument. It does not by itself prove target,
world, dimension, handler, syncId, current operation, or current attempt.

A raw TAIL source observation is not yet `SCREEN_TAIL_CANDIDATE` and does not
necessarily own a tick `K`. If the producer or hub observes it while no active
client-tick serial exists, record `clientTickWindowState=BETWEEN_TICKS`,
`activeClientTickSerialPresent=false`, and
`candidateClientTickSerial=-1`. The diagnostics-only stage must not retain,
replay, forward, or backfill that event. Tick `K` is defined only after the
event is delivered under proven current-tick serial ownership and the
coordinator accepts it as the attempt's candidate. Any inter-tick
behavior correction requires direct source→hub→gate evidence and an exact
ownership-preserving design; until then candidate creation remains fail-closed.

For one attempt, only one `BlockInteractEvent` observed after
`WORLD_OPEN_REQUESTED` and before its TAIL candidate, whose world, dimension and
exact `BlockHitResult` target match the immutable attempt target, may satisfy the
world-click boundary. Keep it as a pending observation until the matching TAIL
candidate atomically consumes it once. Duplicate events, a later event, a
different target, or a crosshair fallback must not replace it or refresh the
open-wait window. Only an accepted TAIL candidate captures an immutable
candidate snapshot, including its proven `candidateClientTickSerial`; it does
not finish child cleanup or publish permission.

Consumption permanently spends that event for this attempt. If the captured
candidate is later rejected, the event is not returned to pending state and a
later TAIL candidate cannot reuse it. A retry starts a new attempt/correlation
and must observe a new matching event.

A captured TAIL candidate may later be promoted to `GUI_BOUND` only if the
active route-owned attempt proves all of these together at that route-owned
promotion boundary:

```text
owning operation is still active
openAttemptId and correlationId match the current attempt
the attempt observed exactly one matching BlockInteractEvent after WORLD_OPEN_REQUESTED
the event preceded this TAIL candidate and matched target, world and dimension
this TAIL candidate consumed that still-unconsumed event exactly once
the route-owned open child is stopped or otherwise proven quiescent
operation-owned normal child cleanup completed before GUI_BOUND
immutable target position matches the observed world-click target
world object identity matches
dimension matches
target block is in the route's explicit in-scope target set
event.screen is non-null
MinecraftClient.currentScreen == event.screen
event.screen instanceof HandledScreen<?>
event.screen class matches the exact screen class mapped for this route
HandledScreen.getScreenHandler() object == player.currentScreenHandler object
captured handler object remains the live player handler object
captured handler syncId == live handler syncId
handler class matches the exact handler class mapped for this route
candidate is inside the route-owned bounded open-wait window
```

If any candidate binding identity changes while the parent is quiescing the
open child or completing its normal cleanup, discard the candidate and end its
association with the permanently spent event. Do not restore that event to
pending state, bind the replacement screen, or inherit the old attempt's event.
If the existing stop/cleanup path releases global or otherwise unowned input,
screen, goal or path state, it is not operation-owned cleanup. Keep the handoff
in a narrow composed owner; if source proves that impossible, record the
contract gap and use only the minimal behavior-preserving generic seam allowed
by the canonical direction.

The initial exact target/type mapping is:

```text
minecraft:chest and minecraft:trapped_chest
    screen class: GenericContainerScreen
    handler class: GenericContainerScreenHandler

minecraft:furnace
    screen class: FurnaceScreen
    handler class: FurnaceScreenHandler
```

Match the actual runtime class and the exact handler object/syncId; do not accept
only a superclass name or broad `ContainerType`. Barrel, every shulker box,
smoker, blast furnace, hopper, dispenser, dropper, ender chest, entity inventory,
crafting/brewing screens, modded containers, and every unlisted block or handler
remain outside this gate.

Do not prove `GUI_BOUND` using only:

```text
interactBlock ActionResult
tryPressAccepted
screenName
screen class
handler class
syncId changed
elapsed time
ContainerSubTracker classification
crosshair fallback
CarryOnContainerExpectedGui.expectedGuiOpened
```

#### Three Distinct Later Client-Tick Boundaries

Neither the tick containing the accepted TAIL candidate nor the tick in which
the route later promotes that candidate to `GUI_BOUND` is one of the three
stable later boundaries. Counting is anchored to `boundClientTickSerial`, not
wall time or a possibly earlier candidate serial. A raw
`currentTick - openTick >= 3` test is insufficient without a proven boundary
serial and callback order.

The implementation ledger must identify a monotonically increasing current-tick
identity and its chosen boundary observation, and the bounded logs and tests
must show `current-tick serial publication -> route-owned promotion -> chosen
boundary observation of that same serial`. If the stored counter advances only
at END, a TAIL callback or Task HEAD must not mislabel the last completed END
serial as the identity of the currently executing tick or reuse that existing
counter blindly.

The attempt records two distinct serial fields:

```text
boundClientTickSerial
    serial of the client tick in which route-owned promotion establishes GUI_BOUND
    immutable for that binding

lastCountedBoundarySerial
    most recent distinct later boundary counted for this attempt
    initialized to boundClientTickSerial
```

A boundary is countable only when its serial is strictly greater than both
`boundClientTickSerial` and `lastCountedBoundarySerial`. After successful full
binding validation, update `lastCountedBoundarySerial` and increment once.

Conceptual trace:

```text
accepted TAIL candidate with proven serial K captures candidateClientTickSerial and immutable candidate:
    matching BlockInteractEvent is consumed once
    GUI_BOUND is not yet established

route-owned promotion in tick B after open-child quiescence/cleanup establishes GUI_BOUND:
    stableLaterBoundaries=0
    boundClientTickSerial=serial for tick B
    lastCountedBoundarySerial=boundClientTickSerial

boundary belonging to binding tick B:
    validate only, count remains 0

first distinct later boundary B+1:
    validate full binding, count=1

second distinct later boundary B+2:
    validate full binding, count=2

third distinct later boundary B+3:
    validate full binding, count=3
    permission may become GUI_INPUT_ALLOWED

subsequent normal evaluation, normally Task HEAD at B+4, by the existing route owner:
    revalidates the full binding and route-specific transfer preconditions
    commits one-time permission consumption together with transfer-lifecycle entry
    may then issue the existing slot action
```

The entire route-reachable execution path is gameplay-mutation-free at counts
0, 1, 2 and 3, not only the gate/event/tick observer. Operation-local
validation, serial, deduplication, counter, invalidation and permission
bookkeeping may change. The parent must short-circuit
before any `super.onTick()`, child return, fallback, helper, chain, or callback
that could call `clickSlot`, register a slot action, mutate the cursor, close or
replace the screen, call `interactBlock`, modify input, or change Baritone
state. Permission transition and slot mutation remain separate ownership
boundaries. Existing global behavior must remain unchanged; the route audit
must prove that no independently reachable owner can bypass the gate. After
`GUI_BOUND` and before permission consumption, do not tick or stop a current or
former child when that lifecycle call can perform any forbidden mutation.

Duplicate observation of the same boundary, render frames, wall-clock delay,
server ticks, Task invocation count, packet callback count, or screen-event
count must not advance `stableLaterBoundaries`.

`GUI_INPUT_ALLOWED` is an attempt-bound, one-time permission, not a durable
boolean capability. The later-boundary callback may only make it available. On
the next normal evaluation, the owning route must fully revalidate the operation,
attempt, target, world, dimension, screen object, handler object, syncId, exact
screen/handler type, open-child quiescence, and empty/valid route preconditions
before consuming the permission once. Permission consumption and entry into the
existing route-specific transfer lifecycle are one logical commit. If entry
cannot be established, do not consume the permission; invalidate or terminate
with a typed reason and perform no slot action. A failed revalidation invalidates
it; a second consumer or later evaluation cannot reuse it.

#### Binding Invalidation And Typed Failure

Clear the captured binding, stability count, and input permission immediately
when any of these changes:

```text
screen object
HandledScreen handler object
player.currentScreenHandler object
captured or live syncId
world object
dimension
target position or in-scope target family
owning operation
openAttemptId or correlationId
```

Also invalidate on `setScreen(null)`, screen replacement, Task interruption,
owner stop/finish, open timeout, or failed stability observation.

Suggested diagnostic outcomes, not prescribed Java enums:

```text
GUI_OPEN_TIMEOUT
SCREEN_TAIL_CANDIDATE_REJECTED
SCREEN_CLOSED_BEFORE_STABLE
GUI_STABILITY_INVALIDATED
OPERATION_INTERRUPTED
```

Every result fails closed. Timeout or invalidation does not authorize an
unsafe click, early slot mutation, global input release, forced screen close,
Baritone cancellation, fallback success, or inherited retry state.

#### Current Reusable Evidence Is Not A Shared Behavior Owner

`AutoDepositOpenContainerBindingTracker` is a useful storage-specific
correlation precedent, but current source does not satisfy the full contract:

```text
present:
    pending BlockInteractEvent to ScreenOpenEvent TAIL correlation
    world, dimension, position, current screen and player handler snapshots
    broad ContainerType compatibility

missing:
    operation-local openAttemptId and retry identity
    HandledScreen.getScreenHandler() object equality check
    explicit captured/live syncId field comparison
    three later-boundary counter and GUI_INPUT_ALLOWED permission
```

Its target policy is also incompatible with the in-scope target set:

```text
AutoDepositTrustedContainerSupport
    delegates to StoreInContainerTask.CONTAINER_BLOCKS

that block list includes:
    chest, trapped chest, Barrel, shulker boxes

that block list excludes:
    furnace processing blocks
```

Do not promote that tracker unchanged to a chest/furnace common authority. It
would include out-of-scope Barrel/shulker behavior and omit furnace behavior.
H5 bulk trust registration owns loaded-block scan and repository mutation only;
it must not own this GUI lifecycle.

`CarryOnContainerExpectedGui` and `ContainerSubTracker` are broad diagnostics or
legacy tracking boundaries. Their visible-screen, handler-change, broad target,
or crosshair-fallback rules are not strict current-operation GUI binding.

#### Non-Blocking Route Owner Audit

Audit these routes separately rather than naming one global owner:

```text
manual trusted-home storage:
    StoreHomeCandidateAttempt
    StoreHomeCandidateNavigationStep.tick
    HomeStorageContainerActivationGate
    HomeStorageQuickMoveIssuer

other storage-container path:
    AbstractDoToStorageContainerTask.onTick
    concrete StoreInContainerTask callers and their parent/retry lifecycle

furnace processing path:
    DoStuffInContainerTask.onStart/onTick/onStop
    SmeltInFurnaceTask.DoSmeltInFurnaceTask
    smoker/blast-furnace variants only if explicitly included and proven
```

Current source indicates:

```text
StoreHomeCandidateAttempt owns one reusable open child for one candidate
StoreHomeCandidateNavigationStep activates as soon as exact binding matches
HomeStorageQuickMoveIssuer performs QUICK_MOVE with button 0
AbstractDoToStorageContainerTask enters onContainerOpenSubtask when its broad
    ContainerType predicate matches
DoStuffInContainerTask enters containerSubTask when isContainerOpen succeeds
InteractWithBlockTask(BlockPos) is non-shift, isFinished() is false, and may be
    returned repeatedly until its parent changes state
```

These are candidate seams, not blanket authority to change generic behavior. A hunk in
`AbstractDoToStorageContainerTask`, `DoStuffInContainerTask`, or another
upstream-derived engine file is an engine divergence and must remain minimal,
recorded, reversible, and verified in the continuous workflow. Do not start with `InteractWithBlockTask`,
`PlayerInteractionFixChain`, `TaskRunner`, global input, or global Baritone
handling.

#### Suggested Additive Observation Event

The existing event name may remain:

```text
CONTAINER_INTERACTION_OBSERVATION
```

##### Canonical Exact GUI-Gate Log Fields

The field names below are the canonical schema for newly added exact GUI-gate
boundary records, focused-test fixtures, parsers and runtime evidence. Use them
verbatim; do not introduce shortened aliases such as `observedBoundarySerial`,
`screenIdentity`, `fullBindingValid`, or `duplicateEventRejected`. This does not
rename unrelated existing payload keys. Add a canonical field during bounded-log
reinforcement only when the existing observer cannot supply it.

Canonical fields (emit only where applicable to the boundary record):

```text
operationId
openAttemptId
correlationId
diagnosticSessionId
diagnosticBoundaryActivationId
diagnosticFlushReason
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
screenOpenEventIdentity
screenEventStage
clientTickWindowState
activeClientTickSerialPresent
activeClientTickSerial
lastIssuedClientTickSerial
lastPublishedClientTickBoundarySerial
guiContainerEventHubIdentity
registeredGuiListenerCount
dispatchEligibleGuiListenerCount
completedGuiListenerCallbackCount
skippedInactiveAfterSnapshotGuiListenerCount
screenEventDisposition
screenEventDropReason
gateOutcome
coordinatorOutcome
operationContextAvailable
activeAttemptPresent
gatePhaseBefore
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
eventScreenIsCurrentScreen
eventHandlerIsPlayerHandler
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
diagnosticBudgetScope
diagnosticDetailObservationCount
diagnosticDetailDedupeSuppressedCount
diagnosticDetailLocalAdmissionCount
diagnosticDetailLocalCapSuppressedCount
diagnosticDetailSharedAdmissionRejectedCount
diagnosticDetailPhysicalEmissionCount
firstObservedGameTick
lastObservedGameTick
lastSuccessfulBoundary
firstFailingBoundary
screenTailSourceObservedCount
screenTailHubDroppedCount
screenTailHubDispatchStartedCount
screenTailHubDispatchCompletedCount
screenTailHubNoActiveListenerCount
screenTailHubInactiveAfterSnapshotSkipCount
screenTailGateReceivedCount
screenTailGateDecisionCount
screenTailGateNoActiveAttemptCount
screenTailCoordinatorAcceptedCount
screenTailCoordinatorRejectedCount
screenTailCoordinatorNoStateChangeCount
finalCheckpointEmissionAttempted
operationFinalCheckpointAdmissionRequestCount
operationFinalCheckpointAdmittedCount
operationFinalCheckpointPhysicalEmissionCount
operationAggregateRegistrationRejectedCount
diagnosticEvidenceCompleteness
diagnosticEvidenceGapReasons
omittedCount
```

Within one attempt, `matchingBlockInteractEventConsumed` is monotonic: once true,
candidate rejection must not return it to false or permit reuse.
`transferLifecycleEntryCommitted=true` is valid only in the same logical commit
that changes `permissionConsumed` to true; neither field may claim success alone.

Do not read a state-changing Carry On API or perform a slot/input action merely
to populate a field.

##### Screen TAIL Source-To-Hub-To-Coordinator Diagnostic Contract

The 2026-09-03 regular-furnace runtime capture proves that the requested
`FurnaceScreen` and `FurnaceScreenHandler` became live, while no exact-gate TAIL
candidate was recorded. Source inspection also shows a previously unobservable
drop boundary: `GuiContainerEventHub.onScreen()` returns when the active
client-tick serial is unavailable. The current evidence classification is:

```text
LOG_CONFIRMED:
    165 exact-furnace open requests and 165 matching world interactions
    164 FurnaceScreen/FurnaceScreenHandler next-tick observations with changed syncId
    the 165th GUI_OPEN_DELAYED record belongs to an unrelated crafting-table screen
    before the ordinary ceiling, no admitted EXACT_GUI_SCREEN_TAIL_CANDIDATE,
        GUI_BOUND, permission, or EXACT_GUI_TRANSFER_ENTRY_COMMITTED record
    post-ceiling exact-gate detail is unobserved; the user-observed outcome is
        that iron smelting did not proceed
    no BOUNDARY-visible generic slot-request record; generic physical clickSlot
        counting is VERBOSE-only and is therefore UNOBSERVED_IN_BOUNDARY

LEADING_SOURCE_AND_LOG_HYPOTHESIS:
    setScreen TAIL may be arriving after client tick RETURN/endTick and before
        the next client tick HEAD/beginTick
    the hub may therefore discard the event because no active serial exists

DIRECTLY_UNVERIFIED:
    execution of the TAIL producer for the affected event
    hub receipt and its exact early-return reason
    whether any listener received that same event
```

Do not upgrade the inference to a confirmed root cause until one bounded
reproduction observes the applicable boundaries in order:

```text
ClientOpenScreenMixin TAIL source
-> GuiContainerEventHub receipt and disposition
-> ExactContainerGuiGate intake, when dispatch starts
-> coordinator evaluation and any existing candidate/invalidation lifecycle record
-> ExactContainerGuiGate decision, when the complete gate path returns normally
-> GuiContainerEventHub dispatch completion, when the fan-out loop returns normally
```

Use these canonical BOUNDARY event names:

```text
EXACT_GUI_SCREEN_TAIL_SOURCE_OBSERVED
EXACT_GUI_SCREEN_TAIL_HUB_DECISION
EXACT_GUI_SCREEN_TAIL_GATE_INTAKE
EXACT_GUI_SCREEN_TAIL_GATE_DECISION
EXACT_GUI_SCREEN_TAIL_HUB_DISPATCH_COMPLETED
EXACT_GUI_SCREEN_DISPATCH_SUPPRESSION_SUMMARY
EXACT_GUI_OPERATION_FINAL_CHECKPOINT
EXACT_GUI_SCREEN_SESSION_FINAL_CHECKPOINT
```

`EXACT_GUI_SCREEN_TAIL_SOURCE_OBSERVED` is emitted immediately before the
existing synchronous `EventBus.publish` call at `setScreen` TAIL. It proves
only that the producer ran. These exact-screen events apply to TAIL only and
carry `eventPreOpen=false`; the existing HEAD event contract remains
unchanged.

While diagnostics mode is `BOUNDARY`, every valid `ScreenOpenEvent` reaching
that TAIL source is eligible for a diagnostic source observation subject to the
uncorrelated screen-activation budget; source admission must not depend on an
active exact-GUI operation or attempt because neither identity is available
there. The source record therefore carries no fabricated operation/task
correlation. It also does not retain, queue, replay, or backfill the event when
the active tick serial is unavailable downstream.

The canonical value sets are:

```text
screenEventStage:
    TAIL_SOURCE_PRE_PUBLISH
    TAIL_HUB_PRE_DISPATCH
    TAIL_GATE_INTAKE
    TAIL_GATE_DECISION
    TAIL_HUB_POST_DISPATCH

screenEventDisposition:
    OBSERVED
    DROPPED
    DISPATCH_STARTED
    RECEIVED
    PROCESSED
    DISPATCH_COMPLETED

screenEventDropReason:
    NONE
    ACTIVE_CLIENT_TICK_SERIAL_UNAVAILABLE
    NO_ACTIVE_GUI_LISTENER

gateOutcome:
    NOT_APPLICABLE
    NOT_EVALUATED
    NO_ACTIVE_ATTEMPT
    COORDINATOR_CALLED

coordinatorOutcome:
    NOT_CALLED
    ACCEPTED_TAIL_CANDIDATE
    RETURNED_INVALIDATE
    RETURNED_NO_STATE_CHANGE

diagnosticBudgetScope:
    NOT_APPLICABLE
    OPERATION_ACTIVATION_DETAIL
    UNCORRELATED_SCREEN_ACTIVATION_DETAIL
    LOCAL_DETAIL_EXEMPT_AGGREGATE_CHECKPOINT
    LOCAL_DETAIL_EXEMPT_SUPPRESSION_SUMMARY
    LOCAL_DETAIL_EXEMPT_FINAL_SNAPSHOT_PROJECTION

diagnosticFlushReason:
    NOT_APPLICABLE
    MODE_OFF
    CLEAN_TEARDOWN_FINAL_SNAPSHOT
```

Use `UNCORRELATED_SCREEN_ACTIVATION_DETAIL` for every source/hub record and for
gate intake/decision without operation context. Use
`OPERATION_ACTIVATION_DETAIL` only when the gate record has a proven operation
context. Final checkpoints and suppression summaries use their explicit
local-detail-exempt values; exemption from the local 256 cap does not bypass
the applicable shared-session subquota.

For this contract, `operationContextAvailable=true` is deliberately narrower
than “an operation lifecycle has started.” At the entry of
`ExactContainerGuiGate.onScreen`, before intake logging, the active-attempt
guard, or the coordinator call, capture one immutable method-local entry
snapshot: active-attempt presence, operation/attempt/correlation identities,
and `gatePhaseBefore`. Context is proven only when that entry snapshot has an
active attempt and its `operationId` equals the entry-time live gate lifecycle's
`operationId`. Intake and a normal-return decision for the same callback must
reuse this exact snapshot; never recompute context from state the coordinator
may have invalidated. The snapshot is discarded when the callback returns and
must not become retained attempt state.

When entry context is false, the intake and subsequent `NO_ACTIVE_ATTEMPT`
decision both report `operationContextAvailable=false`, leave
operation/attempt/correlation identity unavailable, and use the uncorrelated
screen-activation budget and aggregate. Do not copy an otherwise live lifecycle
`operationId` into those records. When entry context is true, a
`RETURNED_INVALIDATE` decision remains correlated to that entry operation and
operation budget even though the coordinator invalidated the live attempt
before decision logging; its operation aggregate records the rejection.

Use this exact mapping:

| Event | Stage | Disposition | Drop reason | Gate/coordinator outcome |
| --- | --- | --- | --- | --- |
| source observed | `TAIL_SOURCE_PRE_PUBLISH` | `OBSERVED` | `NONE` | `NOT_APPLICABLE` / `NOT_CALLED` |
| hub cannot dispatch without active serial | `TAIL_HUB_PRE_DISPATCH` | `DROPPED` | `ACTIVE_CLIENT_TICK_SERIAL_UNAVAILABLE` | `NOT_APPLICABLE` / `NOT_CALLED` |
| hub has no eligible GUI listener | `TAIL_HUB_PRE_DISPATCH` | `DROPPED` | `NO_ACTIVE_GUI_LISTENER` | `NOT_APPLICABLE` / `NOT_CALLED` |
| hub begins fan-out | `TAIL_HUB_PRE_DISPATCH` | `DISPATCH_STARTED` | `NONE` | `NOT_APPLICABLE` / `NOT_CALLED` |
| gate entry | `TAIL_GATE_INTAKE` | `RECEIVED` | `NONE` | `NOT_EVALUATED` / `NOT_CALLED` |
| gate has no active attempt | `TAIL_GATE_DECISION` | `PROCESSED` | `NONE` | `NO_ACTIVE_ATTEMPT` / `NOT_CALLED` |
| coordinator returns normally | `TAIL_GATE_DECISION` | `PROCESSED` | `NONE` | `COORDINATOR_CALLED` / its exact typed outcome |
| snapshot loop returns normally after invoking each registration still active at its turn | `TAIL_HUB_POST_DISPATCH` | `DISPATCH_COMPLETED` | `NONE` | `NOT_APPLICABLE` / `NOT_CALLED` |

`EXACT_GUI_SCREEN_TAIL_HUB_DECISION` is emitted once per receiving hub
instance before either the existing early return or fan-out. It records
`registeredGuiListenerCount` and the snapshot-time
`dispatchEligibleGuiListenerCount`; it must not claim that a callback has
completed. Do not emit one hub decision per listener.

`EXACT_GUI_SCREEN_TAIL_GATE_INTAKE` is emitted before the active-attempt guard
or coordinator call. `EXACT_GUI_SCREEN_TAIL_GATE_DECISION` is emitted only
after that gate path returns normally and supplements rather than replaces the
existing `EXACT_GUI_SCREEN_TAIL_CANDIDATE` and `EXACT_GUI_INVALIDATED`
lifecycle records. `EXACT_GUI_SCREEN_TAIL_HUB_DISPATCH_COMPLETED` is emitted
only after the snapshot fan-out loop returns normally. A registration present
in the snapshot is invoked only if it is still active at its turn; report both
`completedGuiListenerCallbackCount` for callbacks actually invoked and returned
normally and `skippedInactiveAfterSnapshotGuiListenerCount` for registrations
deactivated after the snapshot. A listener or coordinator exception must
propagate unchanged: do not place observed fan-out/coordinator behavior inside
a new catch or finally merely to force a decision/completion record. When
diagnostic admission remains available, absence of the later record is evidence
that the fan-out path did not return normally.

The existing defensive `event == null` return remains behavior-preserving but
is not a canonical TAIL evidence result: `EventBus.publish` dereferences the
event before the hub can receive it. Do not confuse a null event object with a
valid `ScreenOpenEvent` whose `screen` field is null for screen closure.

The same `screenOpenEventIdentity` may connect source, hub, and gate
records for the same synchronous event object, and
`guiContainerEventHubIdentity` distinguishes receiving hub instances. Both are
auxiliary identity-hash evidence and are not globally unique join keys. They
must not become operation identity, Task equality input, retry key, or dedupe
fingerprint fields. Correlate them only with synchronous ordering, thread,
adjacent diagnostic event sequence, and the bounded screen/handler snapshot.
If that evidence is ambiguous, report incomplete correlation rather than
introducing a mutable event-identity registry or fabricating an `operationId`,
`openAttemptId`, or `correlationId`.

`clientTickWindowState` has these diagnostic meanings:

```text
ACTIVE_BEFORE_RETURN
ACTIVE_BOUNDARY_CLAIMED
BETWEEN_TICKS
UNAVAILABLE
```

The value must come from one side-effect-free serial-state snapshot. When no
active serial exists, set `activeClientTickSerialPresent=false` and emit
`activeClientTickSerial=-1` and `clientTickBoundarySerial=-1`. A previous
issued/completed serial may be logged only in its own
`lastIssuedClientTickSerial` or `lastPublishedClientTickBoundarySerial` field.
It must never be copied into `candidateClientTickSerial`,
`activeClientTickSerial`, or `clientTickBoundarySerial`, and a future serial
must never be predicted.

`gameTick` remains an ordering aid from the existing diagnostics clock. It is
not proof that a `GuiClientTickSerialState` window is active and must not be
used as a replacement serial when `activeClientTickSerialPresent=false`.

For source, hub, and rejected gate/coordinator records, populate the existing screen
and handler fields from the raw `ScreenOpenEvent` and the same live client
snapshot rather than from `attempt.binding()`. In particular, preserve
`screenObjectIdentity`, `screenTypeActual`, `handledScreenHandlerIdentity`,
`playerHandlerIdentity`, `liveSyncId`, `eventScreenIsCurrentScreen`, and
`eventHandlerIsPlayerHandler`. `screenTailCandidate=true` remains valid only
after the coordinator accepts the candidate.

These records are observation only. Their implementation must not retain or
replay the event, extend serial lifetime, move a callback, change listener
fan-out, add a new catch around observed behavior, alter retry/timeout state,
create `GUI_BOUND` or permission, or mutate a slot, cursor, screen, input,
Task, goal, or path.

Use the existing shared admission path and these bounds:

```text
normal investigation mode: BOUNDARY
normal runtime mode: OFF
per-event payload cap: 8192 UTF-8 bytes
correlated exact-GUI detail cap: 256 total per operation within one continuous
    BOUNDARY activation across existing gate lifecycle detail plus new gate
    intake/decision detail; retry does not reset it within that activation
uncorrelated exact-screen detail cap: 256 total per continuous BOUNDARY activation
    across all source/hub detail plus gate intake/decision records that have no
    operation context
first new semantic stage/disposition/reason: emit immediately
unchanged repeat summary: at most once per 200 game ticks or about 10 seconds
bounded reason/fingerprint buckets: at most 32, sorted, with omittedCount
shared session hard cap: 5000
ordinary ceiling: 4936
critical reserve: 64
```

Do not create a second independent 5000-event budget. The local 256-detail
budgets are subordinate admission controls, not new sessions. A continuous
`BOUNDARY` activation starts when BOUNDARY becomes active and ends at OFF or
clean teardown; give it a diagnostic-only `diagnosticBoundaryActivationId`.
Turning OFF clears every local budget/aggregate. Re-enabling BOUNDARY inside
the same production `diagnosticSessionId` creates a new activation and does not
pretend that the prior activation's local state still exists. The screen
diagnostics facade creates the activation identity lazily on the first eligible
BOUNDARY observation and never retains it through OFF. Process each
observation in this order: update diagnostic-only counters, semantic dedupe,
local detail-budget admission, existing shared-session admission, then record
the physical-emission result. Select the operation budget only when operation
context exists; otherwise source, hub, gate-intake, and gate-decision detail use
the uncorrelated activation budget. Final-checkpoint and suppression-summary requests
do not consume the local 256 detail allowance, but they still consume their
existing shared critical subquota. The 64-slot reserve is partitioned rather
than fungible: current source provides 8 aggregate-checkpoint slots, 8
non-store-terminal slots, and 6 suppression-control slots among the other
reserved families. Do not promise a physical summary after its classified
shared subquota is exhausted.

The dedupe fingerprint uses stable semantic fields such as event family,
stage, route/target family, `eventPreOpen`, screen/handler class,
`clientTickWindowState`, disposition, outcome, and reason. Keep tick/time,
serial, syncId, attempt/correlation UUID, and every object identity out of the
fingerprint even when they remain useful payload fields.

Stateful dedupe and the 200-tick unchanged-repeat summary have the same scope as
their local detail budget. The gate-owned per-operation/activation aggregate
owns one correlated dedupe state and summary schedule across that operation's
retries. `ExactContainerGuiScreenDiagnosticRuntime` owns a separate
uncorrelated dedupe state and summary schedule for one screen BOUNDARY
activation. Neither state may suppress another operation or survive its own
activation. The operation checkpoint emit-once flag belongs to its individual
operation/activation segment; the mutually exclusive mode-OFF versus clean-
teardown screen flush state belongs to the screen activation aggregate.

`EXACT_GUI_OPERATION_FINAL_CHECKPOINT` is owned by the per-operation
diagnostic aggregate composed into `ExactContainerGuiGate`. Create that
aggregate only while BOUNDARY is active. Within one continuous activation,
retain it across `authorizeRetry()` and request checkpoint emission once from
`beginOwnerStop()` after the existing final lifecycle observation and before
operation stop/listener unregister. Attempt invalidation or parent retry does
not flush it. Repeated stop/close calls do not submit a second admission request
for the same operation/activation segment. “Once” means one shared-admission
request for that segment, not guaranteed physical output; the existing
aggregate-checkpoint subquota remains authoritative.

The checkpoint payload may carry
`finalCheckpointEmissionAttempted=true`, because that fact is known before the
call. Admission and physical-emission outcomes exist only after the emitter
returns: record them in the returned diagnostic result, test projection, and
later activation/session counters, then purge the operation aggregate. Never
backfill `admitted` or `completed` into the already submitted checkpoint. The
physical checkpoint record's presence is itself evidence that its emission
completed.

Checkpoint emission is diagnostics-only and must not obstruct owner cleanup.
`beginOwnerStop()` invokes it through a non-throwing diagnostic wrapper and uses
cleanup `finally` blocks so aggregate purge, lifecycle-observer unregister, the
existing `operationLifecycle.stop`, and GUI-listener unregister still run when
exact-GUI field formatting or diagnostic dispatch fails. A returned shared
`DiagnosticDispatchResult` is accounted normally; a local
`RuntimeException`/`LinkageError` is converted to a typed diagnostic-only
failure result and is never rethrown into the Task lifecycle. This isolation is
specific to diagnostic checkpoint work: exceptions from the observed
listener/coordinator path still propagate unchanged as specified above.

The per-operation checkpoint consists of this exact correlation envelope and
gate-owned aggregate evidence. Its envelope is:

```text
diagnosticSessionId
diagnosticBoundaryActivationId
operationId
gameTick
threadName
routeOwnerClass
routeOwnerIdentity
diagnosticFlushReason=NOT_APPLICABLE
diagnosticBudgetScope=LOCAL_DETAIL_EXEMPT_AGGREGATE_CHECKPOINT
```

Its aggregate fields are:

```text
screenTailGateReceivedCount
screenTailGateDecisionCount
screenTailCoordinatorAcceptedCount
screenTailCoordinatorRejectedCount
screenTailCoordinatorNoStateChangeCount
diagnosticDetailObservationCount
diagnosticDetailDedupeSuppressedCount
diagnosticDetailLocalAdmissionCount
diagnosticDetailLocalCapSuppressedCount
diagnosticDetailSharedAdmissionRejectedCount
diagnosticDetailPhysicalEmissionCount
firstObservedGameTick
lastObservedGameTick
lastSuccessfulBoundary
firstFailingBoundary
diagnosticEvidenceCompleteness
diagnosticEvidenceGapReasons
omittedCount
terminalReason
finalCheckpointEmissionAttempted
```

Because the aggregate spans retries, it must not select or infer one
`openAttemptId`, `correlationId`, `retryIndex`, target snapshot, or permission
identity for this checkpoint.

Any per-reason counts are normalized, sorted, limited to 32 entries, and carry
`omittedCount` when truncated. These counters affect logging only and must not
be reused as gameplay retry, timeout, or terminal state.

Strict OFF cleanup uses the existing diagnostics-session lifecycle boundary.
Each live operation aggregate registers as a cleanup-only
`DiagnosticSessionLifecycleObserver` while its BOUNDARY activation is active;
the existing bounded observer registry and `ChatClefDiagnostics` facade gain a
non-throwing `tryRegister` path plus idempotent unregister support. Existing
registration behavior remains unchanged for existing callers. The exact-GUI
operation creates/retains its aggregate only after `tryRegister` succeeds. If
capacity rejects registration, retain no operation aggregate, increment only
the screen-activation aggregate's
`operationAggregateRegistrationRejectedCount`, mark its evidence
with the `PARTIAL_LIFECYCLE_OBSERVER_CAPACITY` gap reason, and preserve gameplay
behavior. Owner stop
unregisters after checkpoint-result accounting. `beforeModeOff()` and
`afterCleanTeardownSnapshotAttempt(...)` clear only that aggregate's diagnostic
state and unregister it. The registry is not exposed to the hub and must not be
queried for an active operation, correlation ID, retry decision, or gameplay
state. If the same gameplay operation is first observed again in a later
BOUNDARY activation, create a new aggregate segment and report
the `PARTIAL_MODE_TRANSITION` gap reason; derive the primary completeness value
by the precedence below. Retain no bridge state through OFF merely to join the
two segments.

`ExactContainerGuiScreenDiagnosticRuntime` itself implements
`DiagnosticSessionLifecycleObserver`. During `ChatClefDiagnostics` static
composition, `ChatClefDiagnostics` constructs the one runtime, calls the
non-throwing `tryRegister` exactly once before exposing the source/hub/gate
facades, and owns its registration lifetime. A successful registration remains
installed across OFF/BOUNDARY toggles; `beforeModeOff()` clears only the
runtime's current activation state, `finalSnapshotFields()` projects that state
at clean teardown, and `afterCleanTeardownSnapshotAttempt(...)` clears it before
the registration is idempotently released with the diagnostic session. Do not
register once per activation. If initial lifecycle registration is unavailable,
leave that runtime disabled and empty for the session; do not throw into
Minecraft initialization, weaken the GUI gate, or retain partial activation
state.

Do not add a broad static active-operation registry merely to attach operation
IDs to a hub-level drop. If an event cannot be tied to exactly one operation by
an already active, side-effect-free listener context, leave the operation fields
unavailable. An instance-owned screen-activation aggregate records only
uncorrelated source/hub/gate counts, reasons, first/last tick, suppression,
prior operation-checkpoint result counts, and physical-emission outcomes; it
stores no operation ID or Task reference.

Its uncorrelated counters include `screenTailSourceObservedCount`, the hub
drop/start/completion and inactive-after-snapshot counts,
`screenTailGateNoActiveAttemptCount`, and bounded per-reason buckets. A
`NO_ACTIVE_ATTEMPT` decision must never be copied into a per-operation
checkpoint merely to make its count available. It always has
`operationContextAvailable=false` under the active-attempt rule above.

The prior-operation result counters are
`operationFinalCheckpointAdmissionRequestCount`,
`operationFinalCheckpointAdmittedCount`, and
`operationFinalCheckpointPhysicalEmissionCount`. They summarize completed
emitter calls only; they are not backfilled into an operation checkpoint.

The named screen-session checkpoint and clean-final-snapshot projection contain
only activation-level diagnostic evidence:

```text
diagnosticSessionId
diagnosticBoundaryActivationId
diagnosticFlushReason
diagnosticBudgetScope
screenTailSourceObservedCount
screenTailHubDroppedCount
screenTailHubDispatchStartedCount
screenTailHubDispatchCompletedCount
screenTailHubNoActiveListenerCount
screenTailHubInactiveAfterSnapshotSkipCount
screenTailGateReceivedCount
screenTailGateDecisionCount
screenTailGateNoActiveAttemptCount
diagnosticDetailObservationCount
diagnosticDetailDedupeSuppressedCount
diagnosticDetailLocalAdmissionCount
diagnosticDetailLocalCapSuppressedCount
diagnosticDetailSharedAdmissionRejectedCount
diagnosticDetailPhysicalEmissionCount
operationFinalCheckpointAdmissionRequestCount
operationFinalCheckpointAdmittedCount
operationFinalCheckpointPhysicalEmissionCount
operationAggregateRegistrationRejectedCount
firstObservedGameTick
lastObservedGameTick
diagnosticEvidenceCompleteness
diagnosticEvidenceGapReasons
omittedCount
finalCheckpointEmissionAttempted
```

It must not contain or infer `operationId`, `openAttemptId`, `correlationId`,
Task/owner identity, retry state, target, or permission state. For the clean
teardown projection, `finalCheckpointEmissionAttempted` is not asserted because
no nested named checkpoint is attempted.

The two aggregate-flush paths are distinct and share one emit-once/flush state:

```text
BOUNDARY -> OFF:
    beforeModeOff uses try/finally
    try: request one named EXACT_GUI_SCREEN_SESSION_FINAL_CHECKPOINT
         with diagnosticFlushReason=MODE_OFF,
         diagnosticBudgetScope=LOCAL_DETAIL_EXEMPT_AGGREGATE_CHECKPOINT,
         and finalCheckpointEmissionAttempted=true
         account for its returned dispatch result when the call returns
    finally: clear activation aggregate, budget, dedupe, summary, and emit-once state

clean teardown while BOUNDARY:
    do not start a nested named diagnostic dispatch
    contribute the aggregate through finalSnapshotFields to the existing
        DIAGNOSTIC_SESSION_FINAL_SNAPSHOT
    diagnosticFlushReason=CLEAN_TEARDOWN_FINAL_SNAPSHOT and do not assert
        finalCheckpointEmissionAttempted
    diagnosticBudgetScope=LOCAL_DETAIL_EXEMPT_FINAL_SNAPSHOT_PROJECTION
    afterCleanTeardownSnapshotAttempt clears the aggregate regardless of outcome
```

The two paths must not request duplicate checkpoints. Remaining operation
checkpoints are never fabricated from the screen aggregate.
When source/hub evidence cannot be tied to exactly one operation, add the
`PARTIAL_UNCORRELATED_HUB_EVENT` gap reason to the screen activation aggregate,
preserve every other applicable screen gap, and derive that aggregate's primary
completeness value by the precedence below. Do not copy this reason into a
per-operation checkpoint merely from timing or adjacency. Missing correlation
is evidence, not permission to guess.

Use these canonical completeness values where applicable:

```text
COMPLETE_FOR_BOUNDARY_ACTIVATION
PARTIAL_MODE_TRANSITION
PARTIAL_UNCORRELATED_HUB_EVENT
PARTIAL_LOCAL_DETAIL_CAP
PARTIAL_SHARED_ADMISSION
PARTIAL_LIFECYCLE_OBSERVER_CAPACITY
```

More than one gap can apply. `diagnosticEvidenceGapReasons` is the exact sorted
set of all applicable `PARTIAL_*` values above (maximum five; no truncation or
`omittedCount`). `diagnosticEvidenceCompleteness` is
`COMPLETE_FOR_BOUNDARY_ACTIVATION` only when that set is empty. Otherwise it is
the first applicable value in this fixed precedence, used only as a compact
primary status: `PARTIAL_LIFECYCLE_OBSERVER_CAPACITY`,
`PARTIAL_MODE_TRANSITION`, `PARTIAL_UNCORRELATED_HUB_EVENT`,
`PARTIAL_LOCAL_DETAIL_CAP`, then `PARTIAL_SHARED_ADMISSION`. The precedence does
not erase lower-priority reasons and is not a gameplay-severity ranking.

Suggested phase transitions:

```text
WORLD_OPEN_REQUESTED -> WORLD_CLICK_OBSERVED
WORLD_CLICK_OBSERVED -> SCREEN_TAIL_CANDIDATE
SCREEN_TAIL_CANDIDATE -> OPEN_CHILD_QUIESCING
OPEN_CHILD_QUIESCING -> OPEN_CHILD_QUIESCENT
OPEN_CHILD_QUIESCENT -> GUI_BOUND
SCREEN_TAIL_CANDIDATE -> CANDIDATE_REJECTED
GUI_BOUND -> GUI_STABILIZING_0
GUI_STABILIZING_0 -> GUI_STABILIZING_1
GUI_STABILIZING_1 -> GUI_STABILIZING_2
GUI_STABILIZING_2 -> GUI_INPUT_ALLOWED
any active phase -> typed invalidation/timeout/interruption
```

Suggested decision signals:

```text
WORLD_CLICK_OBSERVED never appears
    -> movement, reach, look, parent-child selection, or click request boundary

WORLD_CLICK_OBSERVED appears and the requested GUI becomes live, but no TAIL
source record appears
    -> inspect setScreen TAIL Mixin execution and diagnostic admission

TAIL source appears but no hub decision appears
    -> inspect EventBus subscription, hub construction, and synchronous delivery

hub decision has screenEventDisposition=DROPPED and
screenEventDropReason=ACTIVE_CLIENT_TICK_SERIAL_UNAVAILABLE
    -> the inter-tick active-serial drop is directly confirmed; do not yet change
       serial lifetime or retain/replay the event in a diagnostics-only patch

hub decision has screenEventDisposition=DISPATCH_STARTED but no gate intake appears
    -> inspect listener snapshot/order, wrong hub instance, and an earlier callback exception

gate intake appears but no gate decision appears
    -> inspect the active-attempt guard/coordinator boundary and propagated exception

gate decisions appear but no hub dispatch-completed record appears while
diagnostic admission remains available
    -> inspect a later invoked listener exception and inactive-after-snapshot
       skips; do not fabricate completion

gate decision has gateOutcome=NO_ACTIVE_ATTEMPT
    -> only proves that no active attempt existed at gate receipt; it does not
       encode before-first-attempt, between-retries, post-invalidation,
       retirement/owner-stop, or unrelated-TAIL cause
    -> inspect adjacent attempt create/activate/invalidate/retire and
       owner-lifecycle records; do not infer operation identity or blame slot transfer

gate decision has coordinatorOutcome=RETURNED_INVALIDATE
    -> inspect its exact correlation or binding reason and raw event snapshot

WORLD_CLICK_OBSERVED appears, no TAIL source appears, and no requested GUI
becomes live
    -> interaction packet/server response/Carry On interception remains possible;
       the post-open gate never started

TAIL candidate appears but fullGuiBoundPredicate=false
    -> inspect the exact failed identity; do not call this GUI_OPEN

GUI_BOUND appears but stableLaterBoundaries advances on candidate tick K or promotion tick B
    -> tick-boundary ownership or off-by-one error

same boundary advances the count twice
    -> non-monotonic or duplicate callback accounting

slot mutation appears while count < 3
    -> premature route mutation and gate-owner violation

binding changes but guiInputAllowed remains true
    -> fail-closed invalidation defect

retry reuses attempt/correlation/count
    -> retry ownership defect
```

#### Mandatory Non-Blocking Implementation Ledger And Regression Mapping

Before source changes, capture a non-blocking implementation ledger containing:

```text
baseline/archive or HEAD identity and preserved worktree paths
prior reviewed-document hashes and a statement that any changed document bytes require a new manifest
exact commands/root Tasks and exact target block IDs for each included route
operation, attempt, click, timeout, retry, binding, mutation and cleanup owners
current-tick serial publication -> accepted setScreen TAIL candidate K -> route promotion B
    -> same-B boundary observation -> B+1/B+2/B+3 -> next Task evaluation -> slot call ordering proof
exact repository-relative files, classes, methods and minimal implementation hunks
LAVI-owned versus upstream-derived classification for every affected file
failure/invalidation/retry behavior and rollback unit
all current affected clickSlot/action-type/button call sites
exact test files and scenario-to-test mapping
verification commands, prerequisites, expected results, and executed or `NOT_RUN` status
```

The regression implementation and verification must include at least:

```text
HEAD does not bind
null/unrelated/non-HandledScreen candidates reject
screen/handler object and syncId mismatch reject
world/dimension/target/operation/attempt mismatch reject
TAIL-candidate tick K and GUI_BOUND-promotion tick B are not counted
three distinct later boundaries are required
same boundary cannot count twice
no slot mutation at stable counts 0, 1 or 2
third boundary changes permission only
screen close/replacement and every identity change invalidate
interruption invalidates
retry starts with a new attempt/correlation and zero count
only the matching BlockInteractEvent between request and TAIL is consumed, exactly once
rejected candidate cannot restore or reuse its permanently spent BlockInteractEvent
open child and its operation-owned normal cleanup are quiescent before GUI_BOUND
binding change during child quiescence rejects the candidate
exact GenericContainerScreen/GenericContainerScreenHandler mapping is required for chest/trapped chest
exact FurnaceScreen/FurnaceScreenHandler mapping is required for regular furnace
trusted-home QUICK_MOVE button 0 is unchanged
storage/furnace PICKUP/button contracts are unchanged at actual call sites
in-scope world open attempt uses no ChatClef-owned SNEAK
Carry On absence preserves loadability and generic behavior
stale initial Carry On pickup yields no GUI_BOUND
Barrel and every unlisted container route remain unchanged
generic right-click, doors, trapdoors, beds, buttons, levers, block placement,
    item use, Task replacement and Baritone pathing remain unchanged
parent, current/former child, observer, cleanup and fallback have no reachable mutation while stabilizing
boundClientTickSerial is immutable and lastCountedBoundarySerial suppresses stale/duplicate boundaries
next normal Task evaluation fully revalidates and consumes one attempt-bound permission once
permission consumption and transfer-lifecycle entry commit together; failed entry performs neither
```

A checksum manifest attests only the exact document bytes it names. When these
repository documents change, regenerate any in-repository manifest included in
the active documentation or implementation work before claiming that an earlier
external review hash covers the current text. This checksum update is not a
phase-approval gate. An external archive or Downloads-path artifact is updated
only when the active request includes that external write. The current modified
documentation therefore cannot be described as byte-identical to the earlier
reviewed bundle, and implementation must not depend on an external archive or
machine-specific manifest path.

After capturing the ledger, continue directly with the smallest implementation,
bounded logging, focused tests, and required clean build when the active request
asks for implementation. The ledger is a traceability and rollback record, not
an approval checkpoint. External runtime, deployment, commit, and push are run
only when the active request includes those exact actions.

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

## 2026-08-07 Container Loop Checklist

This checklist is scoped to the 2026-08-07 post-completion evidence window:

```text
plugins/Minecraft/docs/chatclef-post-completion-store-loop-investigation.md
```

For that local crafting-table/container symptom, the logs may show:

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

Do not transfer the origin or field interpretations in this historical
checklist to the 2026-08-19 bare deposit incident. That separate evidence set
proves the DepositCommand origin and is documented here:

```text
plugins/Minecraft/docs/chatclef-bare-deposit-container-handoff-loop-investigation.md
plugins/Minecraft/docs/chatclef-bare-deposit-diagnostics-plan.md
plugins/Minecraft/docs/chatclef-bare-deposit-diagnostics-reproduction-2026-08-19-r1.md
```

The first file is the finalized incident evidence record; the exact root cause
remains unverified. The second is the canonical bounded diagnostics-only design
and implementation-status ledger. The third is a separate live-prefix
reproduction made with the first partial diagnostics patch batch; it is not a
finalized terminal record. Use the plan ledger rather than assuming that every
proposed event is implemented.

Do not assume Carry On is the primary cause when carry state is
`AVAILABLE_NOT_CARRYING` and no click attempt or Carry On state transition
evidence is present.

## Current Furnace Arbitration And Destroy Churn Checklist

Use this section for symptoms where a furnace-backed task such as
`get cooked_beef 10` keeps running inside `SmeltInFurnaceTask`, while logs show
both existing-furnace open attempts and make-new-container material collection.

Documentation status:

```text
documentation only
does not approve Java behavior changes
does not approve cost, timer, retry, path, blacklist, input, or payload changes
```

### Leading Hypothesis

Do not treat repeated `stone` or `cobblestone` mining logs as the primary root
cause until the container arbitration boundary is proven stable.

The current leading boundary is:

```text
DoStuffInContainerTask furnace arbitration
  -> actual child replacement
  -> DestroyBlockTask stop/start
  -> existing forceCancel
  -> repeated stone target restart
```

The suspected flow is:

```text
costToWalk < costToMakeNew
  -> OPEN_EXISTING_CONTAINER

player position or route cost changes near the threshold
  -> costToWalk > costToMakeNew
  -> placeForceTimer reset
  -> GET_CONTAINER_ITEM
  -> furnace material collection starts
  -> MineAndCollectTask returns DestroyBlockTask for stone/cobblestone
  -> Baritone path calculation can still report SUCCESS_TO_GOAL

cost or timer state changes again
  -> OPEN_EXISTING_CONTAINER
  -> furnace acquisition subtree is interrupted
  -> DestroyBlockTask.onStop()
  -> existing Baritone path forceCancel

GET_CONTAINER_ITEM is selected again
  -> same nearby stone can be selected again
  -> a new DestroyBlockTask can start
```

In this pattern, the problem may not be that stone cannot be mined. The problem
may be that the stone-mining child does not live long enough to reach break
progress.

### Current Evidence Limits

Existing `CONTAINER_TASK_TARGET_DECISION` and `CONTAINER_TASK_BRANCH` events are
not enough to prove per-tick branch oscillation.

The current container diagnostics limiter can emit a first detail event and
then a 200-tick summary for repeated equivalent states. Seeing both
`OPEN_CONTAINER` and `GET_CONTAINER_ITEM` in the same command window proves both
paths were observed, but it does not prove the exact transition count, exact
transition tick, or actual active-child replacement by itself.

Before a behavior fix, prove this chain:

```text
FURNACE branch changes GET -> OPEN
  -> furnace acquisition child is stopped
  -> DestroyBlockTask STOP is observed
  -> forceCancelSource=DESTROY_ON_STOP
  -> blockStillExists=true
  -> breakEverStarted=false
  -> same stone target starts in a later DestroyBlockTask
```

If that chain is present, the first failing boundary is likely container
arbitration or child replacement, not stone breakability.

### First-Pass Diagnostic Events

Add these only as diagnostics. They must not change return values, selected
tasks, timer calls, retry behavior, blacklist behavior, Baritone goals, input
state, command lifecycle payload shapes, or existing cleanup.

#### FURNACE_CONTAINER_ROUTE_TRANSITION

Preferred location:

```text
DoStuffInContainerTask.onTick()
after costToWalk / costToMakeNew are calculated
after placeForceTimer reset intent is known
before returning OPEN, GET, PLACE, or handoff child
```

If the existing `ContainerTaskDiagnostics` already receives enough fields from
`DoStuffInContainerTask`, prefer a LAVI-owned stateful observer inside that
diagnostics helper. The observer may run before the existing emission limiter so
suppressed details still update transition counters.

Required fields:

```text
decisionSequence
previousEffectiveBranch
effectiveBranch
branchChanged
branchAgeTicks
branchTransitionCount
rawCostRelation
costToWalk
costToMakeNew
costDelta
costDeltaBand
placeForceElapsedBeforeReset
placeForceDurationBeforeReset
placeForceResetThisTick
placeForceElapsedAfterReset
placeForceDurationAfterReset
placeForceHoldRemainingSeconds
justPlacedElapsed
justPlacedDurationSeconds
nearestSource
nearestPresent
nearestPosition
nearestBlockState
nearestChunkLoaded
nearestCanReach
nearestScannerUnreachable
cachedContainerPositionBefore
cachedContainerPositionAfter
overrideContainerPosition
placeTaskPlaced
playerPosition
playerDistanceSqToNearest
horizontalDistanceSqToNearest
verticalDeltaToNearest
hasContainerBlockItem
containerBlockItemCount
candidateChildClass
candidateChildSemanticKey
```

Suggested `rawCostRelation` values:

```text
NO_NEAREST
WALK_COST_LOWER
WALK_COST_EQUAL
WALK_COST_HIGHER
```

Suggested `effectiveBranch` values:

```text
OPEN_EXISTING_CONTAINER
GET_CONTAINER_ITEM
PLACE_CONTAINER
CONTINUE_ACTIVE_PLACE
CONTAINER_ALREADY_OPEN
WAIT_POST_PLACE_HANDOFF
```

Suggested `costDeltaBand` values:

```text
LE_MINUS_1
MINUS_1_TO_0
ZERO_TO_PLUS_1
GT_PLUS_1
INFINITE
```

Do not include these values in the dedupe fingerprint:

```text
exact costToWalk double
exact playerPosition double
gameTick
eventSequence
timestamp
Task instance ID
calculation worker ID
```

Use only semantic values in the fingerprint, for example:

```text
previousEffectiveBranch
effectiveBranch
costDeltaBand
nearestPosition
placeForceResetThisTick
hasContainerBlockItem
```

#### FURNACE_MAKE_COST_SNAPSHOT

Preferred location:

```text
SmeltInFurnaceTask.DoSmeltInFurnaceTask.getCostToMakeNew()
each existing return boundary
```

Emit source transitions only. Do not call `getCostToMakeNew()` again for
diagnostics.

Required fields:

```text
costToMakeNew
costSource
furnaceCacheHasContents
cachedMaterialSlot
cachedFuelSlot
cachedOutputSlot
burningFuelCount
burnPercentage
cacheUpdatedThisTick
cacheLastUpdatedTick
cacheAgeTicks
cacheSourceFurnacePosition
cobblestoneCount
woodRequirementMetInventory
furnaceBlockItemCount
```

Suggested `costSource` values:

```text
FURNACE_CACHE_NON_EMPTY
COBBLESTONE_INVENTORY_COST
WOOD_TOOL_FALLBACK_50
NO_WOOD_TOOL_FALLBACK_100
```

A suspicious local signature is:

```text
costSource=WOOD_TOOL_FALLBACK_50
furnaceCacheHasContents=false
cobblestoneCount<=8
woodRequirementMetInventory=true
costToMakeNew=50.0
```

This can keep the make-new cost near the threshold while cobblestone is still
being collected.

#### FURNACE_OPERATION_GATE_TRANSITION

Preferred location:

```text
SmeltInFurnaceTask.DoSmeltInFurnaceTask.onTick()
only when the high-level operation gate changes
```

Do not emit the full gate snapshot every tick.

Required fields:

```text
previousGate
currentGate
inventoryMaterialCount
materialsNeeded
materialGateSatisfied
inventoryFuelCount
fuelNeeded
fuelGateSatisfied
materialsAccessible
containerFlowEligible
inventoryOutputCount
```

Suggested `currentGate` values:

```text
GET_MATERIAL
GET_FUEL
MOVE_ACCESSIBLE_MATERIAL
ENTER_CONTAINER_FLOW
```

This event distinguishes a true container arbitration loop from a later
material, fuel, or output movement loop.

#### DESTROY_BLOCK_LIFETIME

Preferred locations:

```text
DestroyBlockTask.onStart()
DestroyBlockTask.onStop()
```

Required fields:

```text
destroyTaskRunId
destroyTaskInstanceId
phase=START|STOP
targetPosition
targetBlockState
blockStillExists
startedAtTick
stoppedAtTick
lifetimeTicks
parentContainerDecisionSequence
parentEffectiveBranch
interruptTaskClass
interruptTaskSemanticKey
customGoalActiveBefore
baritonePathingBefore
pathPresentBefore
calculationGenerationBefore
forceCancelSource
customGoalActiveAfter
baritonePathingAfter
pathPresentAfter
pathSuccessObserved
pathingStartedObserved
reachEverPresent
breakEverStarted
maximumBreakingProgress
blockBecameAir
inventoryCobblestoneAtStart
inventoryCobblestoneAtStop
```

Suggested `forceCancelSource` values:

```text
DESTROY_ON_START
DESTROY_ON_STOP
MINE_OR_COLLECT_PROGRESS_FAILURE
```

The decisive fields are:

```text
interruptTaskClass
parentEffectiveBranch
lifetimeTicks
breakEverStarted
blockStillExists
```

Example container-arbitration signature:

```text
effectiveBranch changes GET_CONTAINER_ITEM -> OPEN_EXISTING_CONTAINER
DestroyBlockTask STOP
interruptTaskClass=DoToClosestBlockTask
blockStillExists=true
breakEverStarted=false
lifetimeTicks is small
forceCancelSource=DESTROY_ON_STOP
```

#### DESTROY_BLOCK_PHASE_TRANSITION

Preferred location:

```text
DestroyBlockTask.onTick()
after the existing reach calculation
```

Emit only when the phase changes.

Suggested phases:

```text
TARGET_SELECTED
GOAL_SUBMITTED
PATH_CALCULATION_SUCCEEDED
PATHING_STARTED
PATHING_STOPPED
GOAL_REACHED_NO_REACH
REACH_ACQUIRED
BREAK_REQUESTED
BREAK_PROGRESS_STARTED
BLOCK_BECAME_AIR
UNREACHABLE_REQUESTED
TASK_INTERRUPTED
```

Required fields:

```text
previousPhase
currentPhase
phaseAgeTicks
targetPosition
targetBlockState
blockStillExists
playerPosition
distanceSqToTarget
horizontalDistanceSq
verticalDelta
reachPresent
isCloseToMoveBack
customGoalActive
baritonePathing
pathPresent
currentMovementPresent
goalMatchesTarget
calculationGeneration
lastPathCalculationResult
playerOnGround
playerTouchingWater
foodChainNeedsToEat
safeToCancel
controllerBreakingBlock
breakingBlockPosition
breakingProgress
leftClickForced
leftClickHeld
mainHandStack
bestToolStack
bestToolSuitable
savePolicyDecision
```

This event must distinguish:

```text
SUCCESS_TO_GOAL -> TASK_INTERRUPTED
SUCCESS_TO_GOAL -> GOAL_REACHED_NO_REACH
SUCCESS_TO_GOAL -> REACH_ACQUIRED -> BREAK_REQUESTED -> breakingProgress stays 0
SUCCESS_TO_GOAL -> REACH_ACQUIRED -> BREAK_PROGRESS_STARTED -> BLOCK_BECAME_AIR
```

Those timelines have different owners and must not be collapsed into one
"mining failed" explanation.

#### MINE_PROGRESS_FAILURE_CONTEXT

Preferred location:

```text
MineAndCollectTask.MineOrCollectTask.onTick()
the existing branch where progressChecker.check() returned false
```

Do not call `progressChecker.check()` a second time for diagnostics. Capture the
existing return value and already-computed state.

Required fields:

```text
targetPosition
targetBlockState
blockStillExists
progressCheckResult
progressCheckerMode
elapsedTicksSinceReset
progressCheckerResetReason
playerStartPosition
playerCurrentPosition
playerDisplacement
baritonePathing
customGoalActive
pathPresent
lastPathCalculationResult
calculationGeneration
activeDestroyTaskRunId
destroyPhase
destroyLifetimeTicks
scannerUnreachableBefore
blacklistFailureCountBefore
blacklistFailureCountAfter
inventoryCobblestoneCount
```

Use this only after the container branch is stable enough to rule out
interruption-driven Destroy restarts.

### Task.tick Is A Second-Pass Option

Do not add a generic `Task.tick()` observer first.

Use it only if `DestroyBlockTask.onStop()` cannot prove which parent branch or
candidate child caused interruption, for example:

```text
interruptTask=null
interruptTask is only a high-level wrapper
the relationship between branch transition and child stop is still ambiguous
```

If needed, the observer must be gated to container acquisition/open subtrees and
must not emit generic hot-path logs.

Required fields:

```text
parentTaskRunId
activeChildBeforeClass
activeChildBeforeRunId
candidateChildClass
candidateChildSemanticKey
isEqualResult
canInterruptPreviousChild
replacementApplied
previousChildStopCalled
activeChildAfterClass
activeChildAfterRunId
containerDecisionSequence
```

### Furnace Arbitration Decision Table

```text
Observed result:
    GET -> OPEN followed by Destroy STOP, open-table interrupt, block still exists,
    breakEverStarted=false
Suspected boundary:
    furnace route arbitration is cancelling the material collection child

Observed result:
    GET_CONTAINER_ITEM remains stable for 100-200 ticks and the same Destroy run
    remains active
Suspected boundary:
    container arbitration is probably not the first boundary

Observed result:
    same Destroy run, SUCCESS_TO_GOAL, then GOAL_REACHED_NO_REACH
Suspected boundary:
    GoalNear arrival and actual block reach boundary

Observed result:
    same Destroy run, reachPresent=true, breakingProgress remains 0
Suspected boundary:
    input, selected tool, look, or block interaction boundary

Observed result:
    same Destroy run, break starts, block becomes air, cobblestone count increases
Suspected boundary:
    stone mining is healthy; inspect crafting, inventory, or furnace flow above it

Observed result:
    branch is stable GET, then progress failure and blacklist changes
Suspected boundary:
    MineAndCollect progress or reachability ownership

Observed result:
    branch does not change but Destroy start/stop repeats
Suspected boundary:
    target reselection or intermediate task churn

Observed result:
    candidate Destroy objects are new but the active Destroy run is stable
Suspected boundary:
    allocation/logging noise, not real child restart
```

Expected healthy timeline:

```text
FURNACE_CONTAINER_ROUTE_TRANSITION
  -> DESTROY_BLOCK_LIFETIME START
  -> GOAL_SUBMITTED
  -> PATH_CALCULATION_SUCCEEDED
  -> PATHING_STARTED
  -> REACH_ACQUIRED
  -> BREAK_PROGRESS_STARTED
  -> BLOCK_BECAME_AIR
  -> inventoryCobblestoneCount increases
```

Suspicious container-boundary timeline:

```text
FURNACE_CONTAINER_ROUTE_TRANSITION GET -> OPEN
  -> DESTROY_BLOCK_LIFETIME STOP
  -> forceCancelSource=DESTROY_ON_STOP
  -> blockStillExists=true
  -> breakEverStarted=false
  -> FURNACE_CONTAINER_ROUTE_TRANSITION OPEN -> GET
  -> same stone target starts again
```

### Furnace Arbitration Bounded Logging

Use the existing diagnostics mode and caps where possible:

```text
mode=BOUNDARY only unless the user explicitly requests verbose diagnostics
first 4 new transitions emit detail immediately
repeated equivalent transitions are summarized
summary every 200 ticks or about 10 seconds
per-correlation detail cap=256
session hard cap=5000
reserve at least 32 terminal, exception, and cap events
```

Recommended summary fields:

```text
windowStartTick
windowEndTick
openBranchTicks
getContainerBranchTicks
openToGetTransitionCount
getToOpenTransitionCount
placeForceResetCount
minimumCostDelta
maximumCostDelta
costThresholdCrossCount
childReplacementCount
destroyStartCount
destroyStopCount
destroyInterruptedBeforeReachCount
destroyInterruptedBeforeBreakCount
pathSuccessCount
reachAcquiredCount
breakStartedCount
blockBecameAirCount
suppressedDetailCount
```

Diagnostics must not add extra calls to:

```text
progressChecker.check()
Task.isEqual()
DestroyBlockTask.isFinished()
forceCancel()
setGoalAndPath()
getCostToMakeNew()
getNearestBlock()
```

Capture existing call results in local variables or pass already-computed state
to observers.

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
    normal world click is followed by target removal/carry transition and no TAIL candidate
Suspected boundary:
    initial Carry On interception before GUI_BOUND; the post-open three-boundary gate never started

Observed result:
    ScreenOpenEvent TAIL appears but full GUI binding predicate is false
Suspected boundary:
    unrelated/stale screen, handler object mismatch, syncId mismatch, target/world/dimension mismatch,
    or operation-attempt correlation failure; do not classify as GUI_OPEN

Observed result:
    GUI_BOUND count advances during candidate tick K/promotion tick B or the same boundary counts twice
Suspected boundary:
    tick serial/callback ordering defect or off-by-one stabilization accounting

Observed result:
    slot mutation occurs before three distinct later boundaries complete
Suspected boundary:
    route parent entered its transfer subtask before GUI_INPUT_ALLOWED

Observed result:
    screen/handler/syncId/world/operation changes but permission remains true
Suspected boundary:
    fail-closed invalidation defect

Observed result:
    retry inherits prior binding, correlation, or stable-boundary count
Suspected boundary:
    open-attempt identity and retry ownership defect

Observed result:
    Barrel or an unlisted container is gated by the chest/furnace policy
Suspected boundary:
    target policy was copied from AutoDepositTrustedContainerSupport without explicit scope isolation

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

For an unknown-cause defect, keep the unproven behavior unchanged and reinforce
bounded diagnostics. For the explicitly specified GUI-bound three-boundary
feature, implement the source-proven portion first, reinforce logs, and continue
verification; keep only the unresolved portion fail-closed.

For the GUI-bound three-boundary contract, the implementation ledger and
verification evidence must cover these route-specific items before the related
behavior is treated as complete:

```text
exact storage commands/root Tasks and their parent/retry owner
exact furnace-processing commands/root Tasks and their parent/retry owner
exact target block IDs and exact handler classes included per route
operation-local openAttemptId and correlation lifecycle
one normal world click per attempt without changing generic interaction globally
setScreen TAIL and chosen client-tick-boundary ordering
opening-tick exclusion and duplicate-boundary suppression
full screen/HandledScreen handler/player handler/syncId/world/dimension/target binding
permission invalidation on every identity or lifecycle change
all affected slot mutation call sites and preserved action type/button
proof that Barrel and unlisted containers remain unchanged
exact proposed unit/integration test files and rollback unit
```

Do not name `AutoDepositOpenContainerBindingTracker` as the shared behavior
owner without addressing its current target-policy mismatch: it includes
Barrel/shulker storage and excludes furnace processing. Do not name
`CarryOnContainerExpectedGui`, `ContainerSubTracker`, `InteractWithBlockTask`,
`PlayerInteractionFixChain`, `TaskRunner`, global input, or global Baritone
handling as the first behavior owner without direct source evidence and the
applicable last-resort report.

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

chatclef-resource-target-retry-thrashing-analysis.md
    Corrected gold-ingot diagnosis and target-level timeline plan for
    path/target retry thrashing after command lifecycle and path adoption are
    ruled out.

chatclef-post-completion-store-loop-investigation.md
    2026-08-07 post-completion Store/Craft/Baritone incident and the bounded
    diagnostics introduced for that investigation.

chatclef-bare-deposit-container-handoff-loop-investigation.md
    2026-08-19 bare deposit evidence, corrected Store progress-field
    interpretation, and the container branch/lifecycle coordination boundary.

chatclef-bare-deposit-diagnostics-plan.md
    Historical/current-source provenance for bounded operation correlation,
    cap-independent aggregate, and terminal-summary partial implementation.

chatclef-bare-deposit-diagnostics-reproduction-2026-08-19-r1.md
    Separate live-prefix reproduction showing which partial diagnostics emitted,
    where the new detail caps became blind, and which first-failure boundaries
    remain unobserved.

chatclef-auto-deposit-obtain-chest-mining-diagnostics-review-2026-08-30.md
    Corrected obtain-chest/mining observer-contamination finding, current runtime
    evidence, and the next additive bounded diagnostics order.

chatclef-automatic-deposit-post-checkpoint-direction-2026-08-31.md
    Post-checkpoint handoff freeze, shared budgeted investigation admission,
    bounded terminal accounting, tool-selection separation, test-only
    container-type coverage, and release-matrix gates.

chatclef-carryon-integration-direction.md
    Engine boundary, exact GUI binding, three-later-client-tick input gate,
    ownership, diagnostics-only, and root-cause patch gates.

chatclef-exact-container-gui-three-tick-implementation-ledger-2026-09-03.md
    Deployed furnace failure counts, exact evidence paths, diagnostics-only
    source plan, focused tests, and one-reproduction decision matrix.

chatclef-engine-divergence-record.md
    Historical exact-GUI engine seams and the post-build matching-JAR runtime
    status update.

fabric-chatclef-bridge-protocol-v1.md
    Stable bridge request/result wire protocol.
```

## 2026-08-22 Sync-Finish Idle-Root Diagnostics

For the incident branch, diagnostics remain under the existing lifecycle log
`details` boundary rather than expanding `command_result.data` beyond the
approved `result_reason` and `result_fidelity` fields.

Relevant diagnostic fields:

```text
root_ownership_classification
finish_callback_first_observation
finish_callback_duplicate_count
stable_idle_root_observation
task_finished_event_association
```

`stable_idle_root_observation` proves only a bounded race buffer: at least three
distinct `END_CLIENT_TICK` observations, at least 500 ms of monotonic stability,
and newest ownership evidence no older than 1000 ms. It does not prove gameplay
success, global engine quiescence, Baritone idleness, input cleanup, or absence
of later work.

## 2026-08-31 automatic-deposit evidence pointer

The latest shared-session cap, terminal-accounting, tool-selection shaping,
artifact-identity, live-evidence, and remaining frozen-handoff observability
status is recorded in Section 16.4 of
`chatclef-automatic-deposit-post-checkpoint-direction-2026-08-31.md`. That
implementation evidence does not alter the lifecycle investigation rules in
this document, prove a gameplay fix, or constitute a final-JAR runtime PASS.
