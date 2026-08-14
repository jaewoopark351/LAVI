<!-- 20260809_kpopmodder: Documented the cooked-beef GoalFollowEntity calculation investigation before behavior changes. -->

# ChatClef Cooked Beef Entity Path Calculation Investigation

Date: 2026-08-09

This document records the current read-only diagnosis for the Fabric ChatClef
`get cooked_beef 10` investigation.

It is documentation only. It does not approve Java behavior changes, Python
behavior changes, command lifecycle changes, Fabric bridge payload changes,
task completion changes, Baritone engine changes, GoalFollowEntity behavior
changes, target selection changes, timeout changes, path adoption changes,
cache deletion, fallback behavior, diagnostics source changes, Gradle changes,
Minecraft launch, runtime reproduction, commit, or push.

## Scope

This applies to the Fabric ChatClef runtime and diagnostics:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
plugins/Minecraft/docs/chatclef-baritone-cache-troubleshooting.md
```

The immediate investigation command was:

```text
command=get cooked_beef 10
normalized_command=@get cooked_beef 10
request_id=lavi-input-ko-ddd5d05a8ba44fdba37cf652e861fab8
correlation_id=lavi-9b915731c1404b37a55923359afc7faf
session_id=fabric-chatclef-99e36899cb0c4ec881e771c8b8efecef
```

Primary inspected logs:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\instance_audit.txt
C:\Vtuber_Souorce_Code\LAVI\logs
```

## Current Conclusion

The safest current conclusion is:

```text
E. root cause not proven; diagnostics need refinement before a behavior fix
```

The directly observed boundary is a weaker form of:

```text
C. calculationGeneration=568 has a calculate start event, but no observed
   calculate completion event or path adoption event in the inspected logs.
```

Do not state that generation 568 is proven to be stuck inside the A* search
loop. The current logs do not prove that. They prove only that the normal
`AbstractNodeCostSearch.calculate()` return and adoption boundary were not
observed for that generation.

Use this wording:

```text
completion event was not observed for generation 568
```

Avoid this wording until proven:

```text
unfinished generation
permanent A* livelock
worker thread hang
```

## Command Lifecycle Result

The command lifecycle is not the proven failing boundary for this incident.

Observed lifecycle state while the command was still running:

```text
finish_callback_received=false
task_finished_event_received=false
waiting_reason=waiting_for_task_finished_event
dispatch_returned=true
result_fidelity=callback_plus_matching_user_task_event
result_reason=waiting_for_terminal_condition
```

The bound root task was still active:

```text
root_task_identity=2dbd612b
root_task_class=adris.altoclef.tasks.container.SmeltInFurnaceTask
root_task_description=<Doing stuff in [furnace] container: [[cooked_beef] x 20]>
root_task_active=true
```

The selected task path remained:

```text
SmeltInFurnaceTask#2dbd612b
  > DoSmeltInFurnaceTask#58294cd2
  > KillAndLootTask#7b458572
  > KillEntitiesTask#783e7efa
  > KillEntityTask#7503f068
  > GetToEntityTask#77a703be
```

Interpretation:

```text
The bridge is waiting because the user task has not reached a terminal state.
This is not evidence that Fabric bridge completion was missed.
```

Do not change command lifecycle ownership, command result payload shape, Python
DTO handling, or task finished event matching for this incident.

<!-- 20260814_kpopmodder: Added the cooked-beef progress-stall logging plan after the latest runtime log review. -->

## 2026-08-14 Progress-Stall Reclassification

This section records the later `get cooked_beef 10` runtime evidence from the
Fabric01 instance. It updates investigation priority for the latest observed
run, but it does not prove a behavior fix.

Documentation status:

```text
documentation only
does not approve Java behavior changes
does not approve Python behavior changes
does not approve command lifecycle changes
does not approve terminal result changes
does not approve task timeout, retry, cancellation, pathing, or target selection changes
does not approve Gradle build, Minecraft launch, commit, push, or merge
```

Latest observed command:

```text
time=2026-08-14 21:23:21
command=get cooked_beef 10
normalized_command=@get cooked_beef 10
request_id=lavi-input-ko-3474117fd76d49c2960ecb46b4d371cc
correlation_id=lavi-11f41761b9d149878fd56f43cd47f6a0
session_id=fabric-chatclef-3bf86be7687d4bc3b7ad090f6aacc9c8
root_task_class=adris.altoclef.tasks.container.SmeltInFurnaceTask
root_task_identity=40c5d8d
root_task_description=<Doing stuff in [furnace] container: [[cooked_beef] x 30]>
```

Latest observed evidence:

```text
Python result status for this request: running only
completed result for this request: not observed
failed result for this request: not observed
connection detached events: 0
terminal_result_send_failed events: 0
waiting_for_terminal_condition events in latest.log: 85
VISIBLE_TASK_RETURN do_smelt_in_furnace_return_material_task events in latest.log: 6072
VISIBLE_TASK_RETURN do_smelt_in_furnace_return_material_task events after cooked_beef accept: 4369
same command context observed after accept: yes
same root task identity observed after accept: yes
this_or_child_timed_out=true observed later: yes
TimeoutWanderTask observed later: yes
```

Use this classification:

```text
operational progress stall / livelock
SmeltInFurnaceTask material-acquisition subtree is not reaching terminal state
```

Avoid this stronger wording until proven:

```text
permanent tight infinite loop
WebSocket disconnect bug
terminal result send bug
Fabric bridge lost the completion event
Baritone A* permanent worker-thread hang
```

Interpretation:

```text
The bridge is still waiting because the AltoClef root task has not reached a
terminal state. The current evidence does not show a terminal result that was
created and then lost. The terminal result appears not to have been created yet.
```

This makes command lifecycle, WebSocket disconnect handling, and terminal send
ordering lower priority for this specific cooked-beef run. The first observable
stall boundary is earlier in the task tree.

## Current First Observable Boundary

The first observable static boundary is:

```text
SmeltInFurnaceTask.DoSmeltInFurnaceTask material-insufficient gate
  -> returns material acquisition child
  -> material target is beef
  -> candidate child is KillAndLootTask or a descendant of it
```

This is not necessarily the root cause. Returning a material acquisition task
when raw material is insufficient is a normal branch decision.

The first failure invariant to prove is:

```text
KillAndLootTask or a descendant runs for a long interval
  -> raw beef count does not increase
  -> cooked beef count does not increase
  -> furnace input/output state does not make forward progress
```

Do not infer actual child restart count from `VISIBLE_TASK_RETURN` alone.
`DoSmeltInFurnaceTask` may create a new candidate object every tick while the
TaskRunner keeps the existing active child when semantic equality holds.

The next diagnostic pass must distinguish:

```text
candidate child identity
active child identity before tick
candidate equals active child
replacement actually applied
active child identity after tick
active child age
```

## Cooked-Beef Target Count Check

The command requested `get cooked_beef 10`, while the root task description
showed:

```text
[[cooked_beef] x 30]
```

Do not treat `x30` as wrong until the accepted-time inventory state is known.
It may be an expected target total if the player already had 20 cooked beef
when the command was accepted.

Before a behavior fix, confirm:

```text
acceptedCookedBeefCount
requestedIncrement
calculatedTargetTotal
targetCookedBeefCount
currentCookedBeefCount
```

Expected normal example:

```text
acceptedCookedBeefCount=20
requestedIncrement=10
calculatedTargetTotal=30
```

If accepted cooked-beef count was not 20, then target calculation becomes a
higher-priority boundary than material acquisition.

## Diagnostics-Only Plan For The Next Pass

Do not add more lifecycle or WebSocket logs for this symptom until material
progress is observable. The current missing evidence is inside the smelting and
loot-acquisition subtree.

All diagnostics below must be bounded. They must emit on state transitions,
inventory deltas, active-child changes, terminal events, exceptions, or a
sampled no-progress heartbeat such as 200 ticks without progress. They must not
emit unchanged per-tick snapshots.

### P0: SMELT_MATERIAL_PROGRESS_SNAPSHOT

Preferred location:

```text
SmeltInFurnaceTask.DoSmeltInFurnaceTask.onTick()
at the material/fuel/output operation gate
```

Emit when:

```text
gate changes
raw material count changes
cooked output count changes
furnace input/output/fuel relevant state changes
active child changes
no material progress for 200 ticks
terminal or exception occurs
```

Required fields:

```text
requestId
correlationId
rootTaskIdentity
doSmeltTaskIdentity
targetCookedItem
targetCookedCount
acceptedCookedCount
requestedIncrement
currentCookedInventoryCount
materialsNeeded
currentRawMaterialInventoryCount
rawMaterialInFurnaceInput
cookedItemInFurnaceOutput
furnaceFuelItem
furnaceFuelCount
currentSelectedGate
currentSelectedBranch
lastMaterialProgressTick
ticksSinceMaterialProgress
progressReason
```

Suggested `currentSelectedGate` values:

```text
GET_MATERIAL
GET_FUEL
MOVE_MATERIAL_TO_INPUT
MOVE_FUEL_TO_FUEL_SLOT
WAIT_FOR_SMELT
COLLECT_OUTPUT
UNKNOWN
```

This event should answer whether the task is truly making no inventory or
furnace-slot progress, or whether progress is happening but not reaching the
terminal condition.

### P1: SMELT_CHILD_SELECTION_STATE

Preferred location:

```text
the boundary that returns the next child task from DoSmeltInFurnaceTask
```

Required fields:

```text
requestId
rootTaskIdentity
doSmeltTaskIdentity
candidateTaskClass
candidateTaskIdentity
candidateSemanticTarget
activeChildBeforeClass
activeChildBeforeIdentity
activeChildRunId
candidateEqualsActiveChild
replacementApplied
activeChildAfterClass
activeChildAfterIdentity
activeChildAgeTicks
```

Interpretation:

```text
candidateEqualsActiveChild=true and replacementApplied=false
  -> candidate allocation is noisy but child execution may be stable

candidateEqualsActiveChild=false and replacementApplied=true every tick
  -> material acquisition may be repeatedly restarted before it can progress
```

This is required because repeated `VISIBLE_TASK_RETURN` alone does not prove
actual active-child replacement.

### P2: KILL_AND_LOOT_ENTITY_DISCOVERY

Preferred location:

```text
KillAndLootTask or the closest-entity selection boundary it owns
```

Emit on:

```text
entityFound false -> true
entityFound true -> false
selected target change
target invalidated
search-wander branch selected
kill branch selected
```

Required fields:

```text
requestId
rootTaskIdentity
killAndLootTaskIdentity
entityFound
trackedCowCount
loadedCowCount
selectedCowUuid
selectedCowRuntimeId
selectedCowAlive
selectedCowRemoved
selectedCowPosition
playerPosition
distanceToCow
selectedBranch
activeChildIdentity
```

Suggested `selectedBranch` values:

```text
SEARCH_WANDER
KILL_TARGET
WAIT_FOR_ENTITY_TRACKER
UNKNOWN
```

If `entityFound=false` persists, the failure boundary is likely search or
entity availability. If `entityFound=true` persists but distance does not
decrease, movement or pathing becomes more likely.

### P3: TIMEOUT_WANDER_PARENT

Preferred location:

```text
TimeoutWanderTask start boundary or the parent that returns it
```

Required fields:

```text
requestId
rootTaskIdentity
timeoutWanderTaskIdentity
directParentClass
directParentIdentity
directParentSemanticKey
selectedCowUuid
entityFound
distanceToWander
wanderReason
sourceTaskPath
```

Interpretation:

```text
TimeoutWanderTask directly under KillAndLootTask
  -> likely no cow target found, search fallback

TimeoutWanderTask under GetToEntityTask or movement descendant
  -> target may exist, but approach/path/movement progress failed
```

Do not interpret `this_or_child_timed_out=true` as proof that a command-level
deadline expired. It only proves a timeout-like task or descendant condition is
present in the task tree.

### P4: KILL_LOOT_PICKUP_PROGRESS

Preferred locations:

```text
KillEntityTask attack boundary
loot item observation boundary
inventory delta boundary
```

Required fields:

```text
requestId
selectedCowUuid
targetHealth
targetHealthChanged
attackAttempted
targetDeathObserved
beefItemEntityObserved
beefDropPosition
pickupAttempted
pickupSucceeded
rawBeefInventoryBefore
rawBeefInventoryAfter
```

This separates:

```text
cannot find cow
can find cow but cannot reach cow
can reach cow but cannot damage cow
cow dies but no beef drops
beef drops but pickup fails
pickup succeeds but smelt progress does not observe inventory delta
```

### P5: BARITONE_ENTITY_PATH_PHASE

Only add this after `entityFound=true` and a stable selected cow have been
observed. Do not make Baritone the first logging target for this latest run.

Required fields when applicable:

```text
requestId
targetUuid
goalType
goalIdentity
pathGeneration
calculationScheduled
calculationStarted
calculationReturned
pathBuilt
postProcessReturned
pathAdoptionStarted
pathAdoptionCompleted
calculationCancelled
forceCancelReason
workerThread
distanceTrend
```

This event is useful only after the target-discovery layer proves that a target
exists and movement toward that target should be happening.

### P6: DIAGNOSTIC_CAP_STATE

When a diagnostic event appears absent, first prove it was enabled and not
suppressed.

Required fields:

```text
diagnosticMode
eventEnabled
eventSuppressed
suppressionReason
sessionEmissionCount
sessionHardCap
bucketEmissionCount
bucketCap
lastAcceptedEvent
```

Missing logs are not evidence until diagnostic enablement, rate limiting, and
hard-cap state are known.

## Behavior-Fix Gate For This Symptom

Do not implement a behavior fix until the next logs identify which of these is
true:

```text
target count calculation is wrong
raw material never increases
raw material increases but furnace input does not
furnace input increases but cooked output does not
cooked output increases but terminal condition does not observe it
candidate child is restarted repeatedly
active child is stable but entity discovery never finds a cow
entity is found but approach distance does not improve
entity is reached but attack/death/drop/pickup does not progress
Baritone path calculation or adoption fails after a stable target is known
```

The preferred first diagnostics are P0 and P1. P2 and P3 are next if P0 proves
that material count is not changing. P5 should remain conditional on a proven
stable entity target.

## Player Position Pattern

The bot spent a long interval around this position:

```text
playerPosition ~= -1358.509 / 41.0 / -375.127
```

A conservative stall window starts around:

```text
time=01:23:35
clientTickId=12830
```

Later tail logs showed brief movement near `02:15`, followed by a return to the
same coordinate band. This supports a user-facing stuck symptom, but it does
not by itself identify the code boundary.

## Baritone Calculation Summary

For the `get cooked_beef 10` command window, path calculation event counts were:

```text
BARITONE_CALCULATION_SCHEDULED=539
BARITONE_CALCULATION_WORKER_STARTED=539
BARITONE_PATHFINDER_CALCULATE_STARTED=539
BARITONE_PATHFINDER_CALCULATE_COMPLETED=538
BARITONE_PATH_ADOPTION_DECISION=538
```

Generation 568 had start events but no observed completion/adoption events:

```text
time=01:43:23
clientTickId=36584
calculationGeneration=568
pathfinderIdentity=5212566
workerThreadName=pool-8-thread-4
workerThreadId=225
firstSegment=true
pathStart=BetterBlockPos{x=-1359,y=41,z=-376}
expectedSegmentStart=BetterBlockPos{x=-1359,y=41,z=-376}
requestedGoalType=adris.altoclef.util.baritone.GoalFollowEntity
requestedGoalSummary=adris.altoclef.util.baritone.GoalFollowEntity#12ecd89:adris.altoclef.util.baritone.GoalFollowEntity@12ecd89
primaryTimeoutMs=500
failureTimeoutMs=2000
currentExecutorSummary=none
nextExecutorSummary=none
inProgressMatchesWorkerPathfinder=true
```

This is the main evidence for the current weak C boundary:

```text
calculate start observed
calculate return not observed
path adoption not observed
```

## Corrected Interpretation Of Baritone Stdout

The following stdout lines must not be treated as worker heartbeat evidence:

```text
movements considered
Open set size
PathNode map size
Path goes for ... blocks
```

These lines can be emitted after the main A* search loop has reached its result
selection phase. `Path goes for ... blocks` can appear around best-path
selection and path construction, before `AbstractNodeCostSearch.calculate()`
returns.

Therefore this inference is unsafe:

```text
pool-8-thread-4 keeps printing those lines
  -> generation 568 is continuously alive inside the A* search loop
```

The missing completion event could instead be explained by one of these:

```text
actual A* search stall
Path predecessor reconstruction stall
Path construction stall
Path.postProcess stall
movement assembly or calculateCost stall
loaded chunk or sanity check stall
calculate RETURN diagnostic suppression
diagnostic session hard-cap exhaustion
stdout from another calculation incorrectly attributed to generation 568
uncaught Error or worker termination before the RETURN diagnostic
```

The next evidence pass must separate these possibilities before any behavior
change is proposed.

## Target Churn Is Currently Lower Priority

Target or task churn remains possible, but it is currently lower priority than
the calculation phase boundary.

Rationale:

```text
AbstractDoToClosestObjectTask may allocate a new KillEntityTask candidate each tick.
Candidate allocation alone does not prove active child replacement.
TaskRunner compares the candidate with the active child by isEqual().
KillEntityTask should be equal when the target entity is the same.
GetToEntityTask should be equal when the entity and close-enough distance are the same.
```

If the target cow were repeatedly changing after the stall began, an expected
flow would be:

```text
new KillEntityTask is not equal to active KillEntityTask
  -> active child stops
  -> nested GetToEntityTask.onStop()
  -> PathingBehavior.forceCancel()
  -> new GetToEntityTask starts
  -> forceCancel on the new path boundary
```

In the inspected logs:

```text
BARITONE_PATHING_FORCE_CANCEL_BOUNDARY count after command start=30
last observed forceCancel tick=8506
conservative stall tick=12830
forceCancel count after conservative stall tick=0
```

If diagnostic gates were still emitting normally, this makes repeated target
churn less likely. If the diagnostic session cap had been exhausted, absence of
forceCancel logs is weaker evidence.

## Dynamic Goal Risk Remains Unproven

`GoalFollowEntity` is a plausible structural risk, but it is not yet proven as
the root cause.

Risk model:

```text
GoalFollowEntity reads current entity position during heuristic or isInGoal calls.
PathNode stores estimatedCostToGoal when the node is created.
If the cow moves during one search, older nodes and newer nodes may be scored
against different target snapshots.
The final goal test may read yet another current target position.
```

That can make search ordering unstable or more expensive.

The current logs do not show:

```text
targetUuid
target position at calculation schedule
target position at calculation start
target position during calculation
target position at completion
target displacement
target velocity
target alive or removed state
```

Therefore the current logs cannot prove whether generation 568 followed one
moving cow, switched cows, chased an unreachable cow, or suffered from terrain
or cache-related reachability.

## Terrain, World, And Cache Remain Unproven

Terrain, collision, world state, or Baritone cache issues cannot be excluded.
They are not currently proven.

Missing fields:

```text
target block position
player to target Y difference
target chunk loaded state
nearby block or passability state
dimension and world identity
current movement class
path destination
cache source or cache freshness signal
```

If target UUID and target position are stable while search state grows
abnormally, terrain/cache/reachability moves up in priority. If the target is
moving and target snapshots diverge during a single calculation, the dynamic
GoalFollowEntity hypothesis moves up.

## Existing-Log Checks Before Source Changes

Before adding source diagnostics, re-check the existing logs for the following.

### Generation 568 Start Fields

Confirm these exact fields from the existing start events:

```text
pathfinderIdentity
firstSegment
pathStart
pathfinderStart
primaryTimeoutMs
failureTimeoutMs
currentExecutorSummary
nextExecutorSummary
inProgressMatchesWorkerPathfinder
```

If `firstSegment=false` in another case, the calculation may be plan-ahead
while an existing path is executing. For the inspected generation 568 evidence,
`firstSegment=true` was observed.

### In-Progress Continuity

After generation 568 starts, look for later snapshots that can prove whether
the same pathfinder identity remained in progress:

```text
BARITONE_GOAL_REQUEST_DECISION.inProgressSummary
BARITONE_PATHING_FORCE_CANCEL_BOUNDARY.inProgressSummary
other pathing snapshots that include pathfinderIdentity and finished state
```

Absence of a completion line is not the same as proof that the same in-progress
pathfinder object remained active.

### Stdout Grouping

Group stdout by timestamp and thread:

```text
timestamp
thread name
Starting to search...
movements considered
Open set size
PathNode map size
Path goes for...
Took ... ms
next calculation start
```

If multiple complete stdout groups appear on the same worker thread after
generation 568, the interpretation that one generation occupied that thread
the whole time becomes weaker.

### Diagnostic Gate Exhaustion

Check whether the diagnostic gate hit the session hard cap:

```text
max_emission=detail_per_bucket=256,session=5000
global session emitted event count
last gated event after generation 568
whether other gated Baritone events continued after generation 568
```

If the cap was exhausted, missing completion or forceCancel diagnostics may be
an observation gap rather than a runtime absence.

### Error Search

Search for:

```text
OutOfMemoryError
StackOverflowError
Pathing exception
Movement became impossible during calculation
Path has wrong size after cutoff
Path doubles back on itself
Mixin callback error
```

### Thread Dump

A JVM thread dump can be the most decisive read-only check when Minecraft is
still running:

```bat
jcmd -l
jcmd <Minecraft-Java-PID> Thread.print -l > thread-dump.txt
```

Inspect `pool-8-thread-4` for frames such as:

```text
AStarPathFinder.calculate0
Path.<init>
Path.postProcess
Movement.calculateCost
synchronized lock wait
diagnostic emitter
thread idle
```

Do not write thread dumps into the source tree unless explicitly requested.

## Diagnostics-Only Candidates

These are future diagnostics-only candidates. They are not approved by this
document.

### P0: Baritone Calculation Phase Boundary

This should be considered before target-level behavior diagnostics because it
answers where generation 568 stopped being observable.

Emit once per phase, not per tick:

```text
CALCULATE_ENTER
CALCULATE0_ENTER
CALCULATE0_RETURN
PATH_BUILD_ENTER
PATH_BUILD_RETURN
POST_PROCESS_ENTER
POST_PROCESS_RETURN
CALCULATE_RETURN
ADOPTION_ENTER
ADOPTION_RETURN
```

Fields:

```text
calculationGeneration
pathfinderIdentity
pathIdentity
workerThreadName
workerThreadId
phase
elapsedMillis
firstSegment
primaryTimeoutMs
failureTimeoutMs
slowPath
slowPathTimeoutMs
effectiveTimeoutMs
cancelRequested
```

This separates:

```text
A* search stall
path reconstruction stall
post-process stall
adoption stall
diagnostic RETURN omission
```

### P1: Entity Pursuit Target Change Boundary

Emit only on selection, target change, invalidation, or start/stop:

```text
initial target selected
target UUID changed
target invalidated
target marked unreachable
GetToEntityTask started
GetToEntityTask stopped
```

Fields:

```text
previousTargetUuid
newTargetUuid
entityRuntimeId
entityType
targetPosition
targetVelocity
targetAlive
targetRemoved
distanceToPlayer
selectionReason
KillEntityTask identity
GetToEntityTask identity
forceCancelExpected
forceCancelObserved
```

This validates or weakens target churn.

### P2: Entity Goal Calculation Snapshot

Emit only at calculation schedule/start/complete boundaries:

```text
calculationGeneration
goalIdentity
targetUuid
targetPositionAtSchedule
targetPositionAtStart
targetPositionAtCompletion
targetDisplacement
targetVelocity
playerPosition
distanceToTarget
targetChunkLoaded
```

This helps distinguish:

```text
dynamic moving-goal instability
stable target with terrain or reachability problem
target disappearance or invalidation
```

### P3: Diagnostic Gate Exhaustion

When the global hard cap is reached, emit one event outside the capped gate:

```text
DIAGNOSTIC_GATE_EXHAUSTED
sessionEmissions
sessionHardCap
lastAcceptedEvent
lastAcceptedGeneration
```

This event must not go through the same gate. Otherwise cap exhaustion can hide
the fact that cap exhaustion occurred.

## Prohibited Changes For This Evidence Set

Do not change:

```text
command lifecycle
Fabric bridge payload
Python CommandResultDTO.data shape
task completion conditions
TaskRunner behavior
UserTaskChain or SingleTaskChain behavior
KillEntityTask behavior
GetToEntityTask behavior
GoalFollowEntity behavior
Baritone pathfinding behavior
path timeout
path adoption
target selection
cache state
fallback or wander behavior
global forceCancel policy
global retry or cooldown
```

The current safest next step is documentation and read-only verification. If
source diagnostics are later approved, the first diagnostics should split the
calculation phase boundary before changing target selection or Baritone
behavior.
