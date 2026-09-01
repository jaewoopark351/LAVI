<!-- 20260901_openai: Recorded the initial diagnostics-only plan and the later separately authorized source implementation without authorizing behavior, build, deployment, runtime, commit, or push work. -->

# ChatClef Iron Pickaxe Resource-Acquisition Bounded Diagnostics Plan

- Date: 2026-09-01 KST
- Status: `DIAGNOSTICS_SOURCE_IMPLEMENTED / STATIC_AND_HERMETIC_PYTHON_VERIFIED; JAVA_BUILD_AND_LIVE_RUNTIME_UNVERIFIED`
- Root-cause status: `UNKNOWN`
- Behavior-change authority: `NONE`

## 1. Authority And Scope

Sections 1 through 17 preserve the original diagnostics direction and
pre-implementation gate for the observed `@get iron_pickaxe 1` incident.
Section 18 records the later, separately authorized diagnostics-only source
implementation. This document still does not authorize a behavior fix, a
dependency change, a build, deployment, Minecraft restart, runtime
reproduction, cache mutation, commit, or push.

The approved scope of this document is the Fabric ChatClef 1.20.1 path only:

```text
LAVI -> Fabric adapter -> Fabric mod -> ChatClef / AltoClef
                                      ChatClef / AltoClef -> Baritone
```

Baritone is shown only as an engine dependency below ChatClef/AltoClef; it is
not another loader/backend sibling.

Forge and MineMind are outside this document. Production handoff behavior is
also outside this document and remains frozen.

The incident must not be reduced to a crafting-table-only problem. Correlated
evidence confirms this resource-acquisition prefix:

```text
CraftInTableTask
  -> DoCraftInTableTask
  -> CollectRecipeCataloguedResourcesTask
  -> CollectIronIngotTask
  -> SmeltInFurnaceTask / DoSmeltInFurnaceTask
  -> MineAndCollectTask / MineOrCollectTask
  -> DestroyBlockTask
```

Later crafting, furnace/container interaction, placement, and command-terminal
paths are candidate downstream boundaries for future association. The raw
`BlockOptionalMeta` placement stack was uncorrelated, so it is not part of the
confirmed command-owned path.

The plan therefore covers correlation and observation across recipe planning,
iron-resource acquisition, mining, smelting, crafting-table or furnace
interaction, placement fallback, concurrent chain ownership, and terminal
classification. It does not change any of those behaviors.

## 2. Policy Baseline

This plan follows the scoped rules in `AGENTS.md`, especially:

```text
unknown cause -> diagnostics only
boundary and state-change events -> allowed
unchanged per-tick output -> prohibited
diagnostic counters and bounded summaries -> allowed
retry, timeout, blacklist, input, path, Task, and terminal behavior changes -> prohibited
terminal, exception, mismatch, and cap events -> reserved emission budget
```

It also follows these companion documents:

```text
minecraft-backend-separation.md
chatclef-carryon-integration-direction.md
chatclef-baritone-cache-troubleshooting.md
chatclef-task-lifecycle-diagnostics.md
chatclef-resource-target-retry-thrashing-analysis.md
chatclef-korean-item-action-alias-v2-plan.md
chatclef-python-command-orchestration-plan.md
```

The older `chatclef-baritone-cache-troubleshooting.md` discussion of a possible
`BlockOptionalMeta` containment direction does not authorize that source work
for this incident. The user's current decision is narrower: iron-pickaxe work
stays diagnostics-only.

## 3. Repository And Runtime Evidence Boundary

The documentation audit used this repository state:

```text
repository root: C:\Vtuber_Souorce_Code\LAVI
branch: test/automatic-deposit-checkpoint-20260831
HEAD: a722ac2a17a813b62e605eaeac0fc9f96f7a1b5e
baseline commit: a722ac2a17a813b62e605eaeac0fc9f96f7a1b5e
working tree: dirty and preserved
```

Runtime evidence was read from:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\instance_audit.txt
C:\Vtuber_Souorce_Code\LAVI\logs\20260901_192621_log.txt
```

The current status payload does not prove that the running JVM loaded bytes
identical to the current working-tree source. The audit also did not find a
`BlockOptionalMeta.java` source file in the current checkout, even though the
runtime stack contains `baritone.api.utils.BlockOptionalMeta`. Consequently:

```text
loaded JAR == current source: UNVERIFIED
BlockOptionalMeta source provenance in this checkout: UNVERIFIED
exact direct BlockOptionalMeta edit path: UNRESOLVED
```

No direct `BlockOptionalMeta` source edit may be proposed from an outdated path
or inferred runtime line number.

## 4. Incident Identity

### 4.1 Loop-like run terminated by connection detach

```text
command: get iron_pickaxe 1
requestId: lavi-input-ko-be43cfb9e4f9477e96bfa264e8e5bd49
correlationId: lavi-694526187fe14fd39c3db61449c6d902
connectionGeneration: 1
command start: 19:22:37 KST
root task: adris.altoclef.tasks.container.CraftInTableTask
root assignment: user-root-4
connection reset: 19:25:29 KST
observed termination: cancelled_without_task
this_or_child_timed_out: false
detach reason: websocket_error
terminal decision reason: task_finished_event_identity_mismatch
terminal send result: no connected socket
```

This run was an effective stall or loop-like run until connection reset. It was
not proven infinite, and it did not exit through a gameplay timeout.

### 4.2 Later natural completion

```text
command: get iron_pickaxe 1
requestId: lavi-input-ko-72e0476b12884bbbae9ea9a14b97c59d
correlationId: lavi-911ec8bc2b794e17b17bf765321be6e0
connectionGeneration: 2
command start: 19:44:52 KST
natural Task finish: 19:46:17 KST
elapsed: 85.544 seconds
cancelInvocationId: 0
terminationKind: finished
this_or_child_timed_out: false
terminal reason: matching_task_finished
terminalSent: true
lifecycleCleared: true
```

The successful retry proves that the later command naturally completed. It
does not prove the causal owner of the earlier loop-like run, nor does it prove
that a cache, JVM state, target-selection defect, or concurrent chain problem
was fixed.

## 5. Confirmed Evidence From The Earlier Run

The following facts are confirmed by persisted logs.

1. At command dispatch, the bridge bound the request and correlation IDs to a
   `CraftInTableTask` root.
2. The command text requested one additional iron pickaxe. Current source
   `GetCommand#getItems` passes that target through
   `AgentCommandUtils.addPresentItemsToTargets`, which adds the current
   inventory-only count before creating the root Task. The same runtime window
   recorded `mainHandStack=1 iron_pickaxe`, and the root description showed
   `[[iron_pickaxe] x 2]`. Those observations are consistent with the
   established additional-acquisition contract, not evidence of a count defect.
   Loaded-JAR byte identity with this source remains unverified.
3. The observed resource path requested raw iron for smelting and selected
   `minecraft:iron_ore` or `minecraft:deepslate_iron_ore` as mining inputs.
4. At 19:22:37, structured mining diagnostics recorded position
   `-525,120,-1067` as an iron-ore mining target. The event recorded:

   ```text
   targetBlockId/state: minecraft:iron_ore
   chunkLoaded: true
   scannerUnreachable: false
   localBlacklistContains: false
   requested blocks: iron_ore, deepslate_iron_ore
   ```

5. The shared ordinary diagnostic budget reached its ceiling at 19:22:44:

   ```text
   event: DIAGNOSTIC_SESSION_CAP_REACHED
   hardCap: 5000
   ordinaryBudget: 4936
   firstSuppressedEvent: VISIBLE_TASK_RETURN
   ```

6. The mining correlation detail cap reached 256 at 19:22:48.
7. At 19:22:56, an uncorrelated raw stack trace recorded:

   ```text
   RuntimeException
     -> ExecutionException
       -> NullPointerException

   BlockOptionalMeta.getManager
   BlockOptionalMeta.drops
   BlockOptionalMeta.getStackHashes
   BlockOptionalMeta.<init>
   PlaceBlockTask$PlaceStructureSchematic.desiredState:300
   BuilderProcess$2.partOfMask
   BuilderProcess.fullRecalc
   BuilderProcess.onTick
   ```

8. Plain stdout, after the structured detail caps were exhausted, recorded this
   retry sequence for `class_2338{x=-525, y=120, z=-1067}`:

   ```text
   19:23:07.334  Try 1 / 4
   19:23:25.577  Try 2 / 4
   19:23:47.025  Try 3 / 4
   19:23:51.574  Try 4 / 4
   19:24:08.375  Try 5 / 4
   ```

   Each record also contained `Failed, blacklisting and wandering.` The plain
   messages prove repeated unreachable/blacklist requests at that coordinate,
   but they do not prove which nested Task owned every attempt after the cap.
9. A command-lifecycle snapshot at 19:22:53 observed the selected chain as the
   automatic-deposit maintenance chain while the user command root remained
   bound. Subsequent automatic-deposit terminal records also carried the same
   iron request and correlation IDs. A later snapshot returned to the user
   chain. This proves that command IDs were ambient context, not Task-ownership
   evidence; it does not prove that automatic deposit caused the symptom.
10. The final stop was caused by WebSocket connection reset and detach. The
    lifecycle explicitly recorded `this_or_child_timed_out=false`.

## 6. Operator Evidence And Its Limit

The operator-provided screenshot from the earlier window showed an
`InteractWithBlockTask` near the same coordinate while the visible block label
appeared to be a chest. That screenshot is useful evidence of the observed
symptom, but it is not stored as a correlated runtime event in the repository.

The persisted structured logs do not currently prove all of the following at
the screenshot tick:

```text
expected target role
actual world block state
Task that selected the position
Task that initiated the interaction
selected chain and child ownership
scanner/cache source used for selection
blacklist state before and after the attempt
```

Therefore this document does not call the position a crafting-table target, a
chest target, or a stale iron-ore target at that later tick. That relation must
be captured directly in a future bounded reproduction.

## 7. Facts That Remain Unproven

The following are hypotheses or coverage gaps, not verified root causes:

```text
Baritone disk world cache was stale
BlockOptionalMeta JVM state was poisoned
the BlockOptionalMeta exception caused the later retry sequence
the retry sequence caused the BlockOptionalMeta exception
the coordinate changed from iron ore to chest during the operation
automatic deposit changed or replaced the target block
the crafting-table route selected the same coordinate
Task equality caused real child replacement
the fifth retry exceeded an intended gameplay timeout
the loaded JAR implemented a different quantity contract from current source
current working-tree source exactly matches the loaded runtime JAR
```

The old run and the later successful run must remain separate evidence units.

## 8. Existing Diagnostic Coverage And Gaps

| Event or boundary | Current source status | Earlier-run evidence | Incident-specific gap |
| --- | --- | --- | --- |
| command lifecycle and UserTaskChain root events | existing | request, correlation, root, detach, and terminal recorded | recipe count and resource-attempt summary absent |
| `MINE_TARGET_SELECTION_TRANSITION` | existing | 2 correlated events before cap | later target role/state changes hidden after cap |
| `MINE_TARGET_GOAL_REQUEST` | existing | correlated initial goal request | no cap-surviving aggregate |
| `TASK_CHILD_RECONCILIATION` | existing for mining | 8 correlated events before cap | full acquisition-chain ownership is not summarized |
| `DESTROY_NAVIGATION_STATE_TRANSITION` | existing | 2 correlated events before cap | later reach/interaction transition hidden |
| `BARITONE_GOAL_PATH_TRANSITION` | existing | 4 correlated events before cap | no final owner/path summary after cap |
| `MOVEMENT_PROGRESS_CHECK_RESULT` | existing | 6 correlated events before cap | stdout retries are not bound to the exact owner |
| `BLOCK_UNREACHABLE_REQUEST` | existing source | 0 correlated persisted events for this run | request count and owner missing after cap |
| `BLOCK_BLACKLIST_STATE_CHANGED` | existing source | 0 correlated persisted events for this run | before/after threshold state missing |
| `CONTAINER_OPEN_ATTEMPT_OBSERVED` | existing source | 0 correlated persisted events for this run | expected target role is not bound to actual clicked block |
| `CONTAINER_OPEN_RETURN_OBSERVED` | existing source | 0 correlated persisted events for this run | click-to-screen result absent |
| `CRAFTING_TABLE_ROUTE_RETRY_SUMMARY` | existing source | 0 correlated persisted events for this run | current route/deduplication key includes Task identity and is not sufficient for this command |
| `BLOCK_OPTIONAL_META_MANAGER_FAILURE` | historical documented name; not found in current source and classified as ordinary detail by the current name-based classifier | raw stack only | not reserve-eligible after ordinary-cap exhaustion; no command, chain, Task, stage, or cache-write context |
| `DIAGNOSTIC_SESSION_CAP_REACHED` | existing | one cap event recorded | material post-cap transitions need aggregate and reserved terminal coverage |

The current crafting-table route key includes Task identity. The canonical
lifecycle runbook prohibits instance identity, tick, timestamp, or a random ID
from making every semantically unchanged repeat unique. This is a diagnostics
design tension to test before any future implementation; it is not evidence of
a gameplay defect.

## 9. Diagnostics-Only Goal

A future reproduction should be reconstructable as one command-scoped chain:

```text
command accepted and correlated
  -> root recipe target established
  -> additional-acquisition quantity decision recorded
  -> active resource requirement selected
  -> parent and child Task reconciled
  -> target role and expected block set recorded
  -> actual target block observed at existing selection boundary
  -> Baritone goal/path owner recorded
  -> reach, interaction, screen, or mining progress transition recorded
  -> existing unreachable/blacklist decision recorded
  -> placement fallback or BlockOptionalMeta boundary recorded if entered
  -> chain-owner transition recorded if another chain becomes selected
  -> natural finish, timeout, cancel, disconnect, or exception summarized
```

The trace must answer:

```text
Did the loaded runtime quantity decision match the source-backed additional-acquisition contract?
Which resource requirement was active at each transition?
Who selected each target coordinate and for which target role?
What block did the selecting code expect?
What block did the world expose at selection, path, reach, and click boundaries?
Did the same semantic target survive parent-child reconciliation?
Who owned each unreachable request and blacklist attempt?
Did the active chain change while the command remained bound?
Was BlockOptionalMeta entered by the same correlated operation and Task path?
What was the last successful boundary?
What was the first explicit failure boundary?
What was only unobserved because a detail cap was reached?
What actually ended the command?
```

## 10. Reuse-First Event Contract

Do not duplicate the existing mining, Baritone, interaction, command-lifecycle,
or cap events. Reuse them with the same `commandRequestId` and
`commandCorrelationId` and project only the missing command-scoped facts.

### 10.1 Association gate: correlation is not ownership

A command context attached to a log event proves transport correlation only.
It does not prove that the selected chain or emitting Task is owned by that
command. This distinction is material in the earlier run: at 19:22:53 the
selected chain was `DepositAllInventoryPressureChain` while the iron-pickaxe
user root remained bound, and later automatic-deposit terminal records carried
the same command IDs.

Every candidate observation must therefore receive one immutable association
status before it can enter a command-scoped ledger:

```text
COMMAND_ROOT_DESCENDANT
CONCURRENT_CHAIN_UNOWNED
UNKNOWN
```

`COMMAND_ROOT_DESCENDANT` requires all of the following evidence from existing
lifecycle observations:

```text
request, correlation, session, and connection-generation IDs match exactly
the bound rootAssignmentId, rootGeneration, and root object identity match
the selected chain at the observation is the UserTaskChain
the emitting Task object identity belongs to the chain's actual untruncated path
the actual parent-child lineage connects that identity to the bound root
the lineage has not been retired, replaced, or invalidated before the observation
the evidence was captured at the same event boundary, or an immutable association
token was captured before an asynchronous handoff
```

The command IDs alone are insufficient. A matching Task class is also
insufficient because automatic-deposit work can enter crafting, placement, and
container Tasks of the same classes used by a user command. If the selected
chain is a concurrent non-user chain and the emitting Task object identity is
proven to belong to that chain's actual path, classify the event as
`CONCURRENT_CHAIN_UNOWNED`. A rendered `selectedChainTaskPath` string is display
evidence, not classifier input. A truncated path, class-only match, cross-tick or
cross-thread observation without a captured association token, or any other
lineage gap is `UNKNOWN`; do not infer ownership.

Association evidence fields include:

```text
rootAssignmentId
rootGeneration
boundRootTaskInstanceId
commandRequestId
commandCorrelationId
commandSessionId
commandConnectionGeneration
associationCaptureClientTick
associationCaptureThread
selectedChainClass
selectedChainIdentity
sourceTaskInstanceId
emittingTaskPresentInSelectedChainPath
selectedChainPathRootMatchesBoundRoot
selectedChainPathTruncated
asyncAssociationTokenAvailable
associationStatus
associationReason
```

Object identities may prove same-boundary lineage, but they must not enter a
deduplication fingerprint or be treated as stable across a replacement.

Only `COMMAND_ROOT_DESCENDANT` observations may increment the iron-pickaxe
requirement, target-attempt, unreachable, blacklist, mismatch, or
`BlockOptionalMeta` counters. Unowned and unknown observations retain separate
bounded counts and samples:

```text
unownedObservationCount
unknownAssociationCount
boundedUnownedChainSamples
boundedUnknownAssociationSamples
```

They may explain concurrent activity, but they may not populate the command's
target history, last-success boundary, first-failure boundary, or terminal
cause.

### 10.2 Existing semantic authorities

One event family owns each fact. The new projection must not become a second
authority for the same decision:

| Fact | Existing authoritative event or boundary | Permitted additive projection |
| --- | --- | --- |
| candidate child returned by a visible Task | `VISIBLE_TASK_RETURN` | association status and resource stage; not proof of active replacement |
| mining child reconciliation only | `TASK_CHILD_RECONCILIATION` | proven active `MineOrCollectTask` / `DestroyBlockTask` relation and attempt reference |
| candidate smelting child selection | `SMELT_CHILD_SELECTION_STATE` | resource stage and association; not proof of active replacement |
| smelting material/output/slot progress | `SMELT_MATERIAL_PROGRESS_SNAPSHOT` | requirement reference only |
| furnace operation gate | `FURNACE_OPERATION_GATE_TRANSITION` | resource stage and gate reference |
| final furnace branch/candidate/nearest route | `FURNACE_CONTAINER_ROUTE_TRANSITION` | target-role reference |
| mining target selection | `MINE_TARGET_SELECTION_TRANSITION` | active attempt reference after ownership proof |
| mining goal request | `MINE_TARGET_GOAL_REQUEST` | existing attempt reference; never starts an attempt by itself |
| mining target abandonment | `MINE_TARGET_ABANDONED` | close the exact current mining tuple only after ownership proof |
| generic container pre-branch route inputs | `CONTAINER_TASK_TARGET_DECISION` | target-role candidate reference only |
| selected crafting-table child | `CONTAINER_TASK_BRANCH` | exact candidate Task reference only; not proof that the scheduler installed it |
| applied container child replacement or clear | `CONTAINER_TASK_CHILD_RECONCILIATION` | start or close a furnace/crafting-table interaction attempt only from exact Task identity and physically emitted reconciliation evidence |
| sealed container owner stop or interrupt | `CONTAINER_TASK_OWNER_STOP` | close only after physical source completion, one unambiguous sealed scope, and exact current tuple plus attempt sequence; ambiguity or suppression records a gap and derives no closure |
| actual click, block, screen, and result | existing container-open and interaction events | immutable attempt reference only |
| crafting route aggregate | `CRAFTING_TABLE_ROUTE_RETRY_SUMMARY` | aggregate reference only; never a second decision or retry count source |

`CRAFT_RESOURCE_TARGET_ROLE_TRANSITION` is only a state-change projection when
the proven association, resource stage, target role, or semantic target tuple
changes. It must not repeat the full mining, furnace, container, pathing, screen,
or interaction payload.

`TASK_CHILD_RECONCILIATION` remains mining-specific. Container interaction uses
the separate `CONTAINER_TASK_CHILD_RECONCILIATION` source seam and exact parent,
candidate, and active-child object identity. Other acquisition boundaries that
do not expose equivalent lineage remain `UNKNOWN`.

### 10.3 Resource requirement decision

Proposed additive event:

```text
CRAFT_RESOURCE_REQUIREMENT_DECISION
```

Emit only when the active requirement or quantity decision changes.

Required fields, when already available from the existing decision:

```text
commandRequestId
commandCorrelationId
rootAssignmentId
rootTaskClass
rootTaskInstanceId
requestedItem
requestedCount
currentItemCount
targetItemCount
deltaNeeded
quantityContract
recipeOutputCount
recipeCraftCount
activeRequirementItem
activeRequirementCount
requirementSource
parentTaskClass
childTaskClass
selectedBranch
associationStatus
```

Do not call recipe resolution, inventory counting, or Task selection a second
time to populate the event. For current source, `quantityContract` is
`ADDITIONAL_ACQUISITION`: a request for `1` plus a captured current count of `1`
may produce target count `2`. That explanation is valid only when the values
come from the existing decision and the loaded artifact has been proven. An
unavailable value stays explicitly unavailable.

### 10.4 Resource stage, target role, and attempt sequence

Resource progress and target purpose are separate dimensions. They must not be
combined into labels containing `OR`.

Initial `resourceStage` vocabulary:

```text
RECIPE_PLANNING
IRON_INPUT_ACQUISITION
RAW_IRON_SMELTING
FINAL_CRAFTING
PLACEMENT_SUPPORT
UNKNOWN
```

Initial `targetRole` vocabulary:

```text
IRON_ORE_BLOCK
RAW_IRON_DROP
FURNACE_ACQUISITION
FURNACE_INTERACTION
CRAFTING_TABLE_ACQUISITION
CRAFTING_TABLE_INTERACTION
PLACEMENT_SUPPORT_BLOCK
UNKNOWN
```

`UNKNOWN` is fail-closed. It does not mean that a target matched or succeeded.

`targetAttemptSequence` is owned only by the command-scoped target ledger for
`COMMAND_ROOT_DESCENDANT` observations. Unowned and unknown observations report
`targetAttemptSequence=UNAVAILABLE`. One semantic attempt begins only after an
existing authoritative owner installs or retains this active reconciled tuple,
or the same boundary otherwise proves that it is active:

```text
(resourceStage, targetRole, targetPosition, normalizedExpectedBlockIds)
```

Candidate return, candidate object-identity churn, and equal-reconciliation do
not start or increment an attempt. The sequence increments when the behavior
owner actively installs a different tuple. It also increments when an explicit
existing unreachable/blacklist abandonment or clear ends the active tuple and a
later authoritative decision reselects it. An observed owner `STOP`,
`INTERRUPT`, or command terminal also closes it. A chain switch by itself cannot
be interpreted as target abandonment.

A per-tick repeat, repeated goal submission for the same active tuple, unchanged
Task return, log admission failure, or detail suppression does not itself
increment the sequence. Suppressed detail may update only the already-proven
active tuple's bounded counters. The existing behavior owner, not diagnostics,
decides selection, abandonment, and reselection. Cleanup closes the sequence; a
late event may not reopen it.

Proposed additive event:

```text
CRAFT_RESOURCE_TARGET_ROLE_TRANSITION
```

This event binds existing target observations to their semantic role without
changing the target or scanner result.

Required fields:

```text
commandRequestId
commandCorrelationId
rootAssignmentId
targetAttemptSequence
selectedChainClass
selectedChainIsUserTaskChain
associationStatus
associationEvidence
rootGeneration
boundRootTaskInstanceId
associationCaptureClientTick
associationCaptureThread
emittingTaskPresentInSelectedChainPath
selectedChainPathRootMatchesBoundRoot
selectedChainPathTruncated
sourceEventName
sourceEventSequence
sourceTaskInstanceId
parentTaskClass
childTaskClass
previousResourceStage
resourceStage
previousTargetRole
targetRole
targetPosition
expectedBlockIds
observationBoundary
```

Rich observed block, scanner, goal, path, screen, and interaction values remain
owned by the existing authoritative events listed above. The projection refers
to them by stable event sequence or existing run identity rather than copying
them into a parallel truth source.

### 10.5 Expected-versus-observed mismatch

Proposed exception/coverage-reserve-eligible event:

```text
CRAFT_RESOURCE_EXPECTED_OBSERVED_MISMATCH
```

Emit only when the same existing evaluation supplies both a semantic expected
set and an actual world block that does not belong to it.

Required fields:

```text
commandRequestId
commandCorrelationId
targetAttemptSequence
associationStatus
resourceStage
targetRole
targetPosition
expectedBlockIds
observedBlockId
observedBlockStateSummary
observationBoundary
observationSource
sourceEventName
sourceEventSequence
sourceTaskInstanceId
parentTaskClass
childTaskClass
selectedChainClass
```

Do not rescan the world, rerun a predicate, reopen a screen, or invoke Baritone
to manufacture the comparison. A missing expected or observed side is
`COVERAGE_GAP`, not a mismatch. Only `COMMAND_ROOT_DESCENDANT` observations may
emit this command-scoped mismatch.

At most two unique mismatch records per correlation may request physical
emission. The stable uniqueness key is the normalized tuple of resource stage,
target role, target position, expected block IDs, observed block ID, and
observation boundary. Duplicates and the third or later unique mismatch update
only saturating suppression counters. Every physical request still passes
through shared admission; the process-wide 16-slot exception/coverage quota may
deny it.

### 10.6 Interaction projection

Reuse:

```text
CONTAINER_OPEN_ATTEMPT_OBSERVED
CONTAINER_OPEN_RETURN_OBSERVED
existing INTERACT_BLOCK boundary events
```

Project the immutable target-role and attempt context into these events. Do not
create another click, interaction, screen read, or retry. At minimum, preserve:

```text
targetAttemptSequence
associationStatus
resourceStage
targetRole
expectedBlockIds
actual targetBlockId
targetPosition
screen before and after
interaction result
command correlation
```

### 10.7 Unreachable and blacklist projection

Reuse:

```text
MOVEMENT_PROGRESS_CHECK_RESULT
BLOCK_UNREACHABLE_REQUEST
BLOCK_BLACKLIST_STATE_CHANGED
```

The command-scoped ledger should count existing calls even when ordinary detail
is suppressed, but only after `COMMAND_ROOT_DESCENDANT` lineage is proven. It
must not request another blacklist update or reinterpret an existing threshold.
Unowned and unknown calls use their separate counters.

Required aggregate facts:

```text
ownerTaskClass
targetAttemptSequence
associationStatus
resourceStage
targetRole
targetPosition
unreachableRequestCount
firstFailureCount
lastFailureCount
allowedFailures
unreachableBefore
unreachableAfter
blacklistTransitionCount
firstObservedTick
lastObservedTick
suppressedDetailCount
```

### 10.8 BlockOptionalMeta boundary

If an existing boundary already exposes a thrown engine exception, use:

```text
BLOCK_OPTIONAL_META_MANAGER_EXCEPTION
```

If diagnostics can observe boundary entry but neither normal return nor a
Throwable is available, use the non-causal coverage record:

```text
BLOCK_OPTIONAL_META_MANAGER_COVERAGE_GAP
```

The historical name `BLOCK_OPTIONAL_META_MANAGER_FAILURE` does not contain a
token selected by the current family classifier and therefore falls into
ordinary detail. It must not be relied on after ordinary-cap exhaustion.
`..._EXCEPTION` and `..._COVERAGE_GAP` are eligible for the existing
exception/coverage reserve without a special classifier branch.

`OBSERVATION_FAILED` is reserved for failure of the diagnostic observation
itself. It must not label an engine manager exception merely to obtain reserve
eligibility. Likewise, absent normal return without an already captured
Throwable is a coverage gap, not proof of an exception.

The exception event should add command and ownership context to already
captured stage and Throwable fields:

```text
commandRequestId
commandCorrelationId
selectedChainClass
associationStatus
associationEvidence
rootTaskClass
childTaskPathSummary
placeTaskClass
fallbackReason
requestedFallbackBlock
availableStateCount
stage
candidateManagerCreated
reloadCompleted
managerPublished
outerException
rootException
rootMessage
rootTopFrame
dropCacheWrite
```

The coverage-gap event contains boundary entry, normal-return presence,
Throwable availability, association evidence, and coverage reason; it must not
populate `outerException`, `rootException`, or a failure cause.

Only a proven `COMMAND_ROOT_DESCENDANT` engine exception increments the
iron-pickaxe exception count. A coverage gap increments only coverage accounting.
An exception or gap observed under a concurrent chain or indeterminate lineage
is preserved as unowned or unknown evidence and cannot be cited as the command's
first failure boundary.

Current-source limitations:

```text
BlockOptionalMeta.java is not present in the current checkout
runtime source provenance is unverified
internal manager or cache state must not be guessed
reflection or state-changing resource reload is prohibited
```

Prefer a LAVI-owned observer when it can expose the same facts. If the only
observable boundary is an upstream-derived method, the future proposal must be
one minimal diagnostics-only hunk. It must not add a broad catch, suppress or
convert the exception, publish a manager, change fallback output, or write a
cache. A before-boundary event followed by an absent normal-return event may
produce only the coverage-gap record unless the existing boundary already
exposes the Throwable. Neither event may change exception propagation.

This document does not authorize that hunk.

### 10.9 Two-phase terminal aggregate

Proposed non-Store-terminal-reserve-eligible event:

```text
CRAFT_RESOURCE_ACQUISITION_TERMINAL_SUMMARY
```

Detach is an observation boundary, not immediate permission to finalize. The
earlier sequence continued through cancellation, `TaskFinished`, identity
mismatch, terminal-send failure, and lifecycle clear after the initial detach.
The diagnostic ledger must therefore use two phases:

```text
phase 1: freeze the first authoritative primary cause, then accumulate existing outcomes
phase 2: freeze the outcome snapshot and request one summary at a finalization barrier
```

The first trigger freezes an exact-once key and primary cause:

```text
(commandSessionId, commandConnectionGeneration, commandRequestId,
 commandCorrelationId, rootAssignmentId)
```

Later callbacks, `TaskFinished`, classifier decisions, send results, cleanup,
and unbind observations may fill outcome fields but may not overwrite that
cause. Detach starts this process but emits zero summaries.

Normal-send finalization requires existing `terminal_result_sent` evidence with
`terminalSent=true` and `lifecycleCleared=true`; queue completion and context
unbind already precede that lifecycle record. Detach finalization requires both
`connection_detached_lifecycle_cleared` and a successful command-context unbind
or queue mutation for the same immutable identity. A `TaskFinished` record,
identity mismatch, send failure, lifecycle clear alone, or unbind alone is not
a sufficient barrier.

If no finalization barrier arrives, a diagnostic-only observation window closes
at the first of 200 game ticks or 10 seconds after the frozen trigger. The
existing client-tick observation path performs this check; no thread, scheduler,
sleep, or engine wait is added. It freezes one summary with
`finalizationMode=DIAGNOSTIC_FALLBACK_INCOMPLETE` and
`coverageStatus=INCOMPLETE_TERMINAL_CLEANUP_UNOBSERVED`, then replaces the
active ledger with a tombstone. This window is a logging-retention bound, not a
gameplay or command timeout, and it must not cancel, clear, retry, submit, or
send anything. A world/session retirement may close the same partial ledger
earlier. Transition to diagnostics `OFF` drops the ledger without emission.

Use only values already captured by existing decisions or the diagnostics-only
ledger. If a field cannot be observed without reevaluating behavior, record it
as explicitly unavailable. In particular, `currentItemCountAtTerminal` may use
only an already existing terminal inventory snapshot; it must not trigger a new
inventory scan.

Required fields:

```text
commandRequestId
commandCorrelationId
commandSessionId
commandConnectionGeneration
rootAssignmentId
requestedItem
requestedCount
currentItemCountAtStart
currentItemCountAtTerminal
targetItemCount
naturalTaskFinished
thisOrChildTimedOutAtFinalization
thisOrChildTimedOutEverObserved
firstTimedOutObservationTick
lastTimedOutObservationTick
cancelInvocationId
connectionDetached
detachReason
primaryTerminationCause
taskTerminationKind
terminalDecisionReason
classifiedResultStatus
classifiedResultReason
classifiedResultFidelity
evidenceConclusion
terminalSent
resultSendStatus
resultDeliveryStatus
lifecycleCleared
queueContextCleared
contextUnbindReason
finalizationBoundary
finalizationMode
terminalEmissionAttempted
terminalEmissionAdmitted
elapsedTicks
elapsedMs
requirementTransitionCount
targetAttemptCount
boundedTargetHistory
unreachableRequestCount
blacklistTransitionCount
chainOwnerTransitionCount
commandDescendantObservationCount
unownedObservationCount
unknownAssociationCount
blockOptionalMetaExceptionCount
blockOptionalMetaCoverageGapCount
lastSuccessfulBoundary
firstExplicitFailureBoundary
firstUnobservedBoundaryAfter
suppressedDetailCount
coverageStatus
```

Primary terminal causes must distinguish at least:

```text
NATURAL_TASK_FINISH
EXISTING_COMMAND_DEADLINE
EXPLICIT_CANCEL
CONNECTION_DETACH_CANCEL
COMMAND_EXCEPTION
DISPATCH_EXCEPTION
UNKNOWN
```

Result fidelity and delivery are separate axes. At minimum they must preserve
matching and nonmatching Task-finish fidelity, classifier status/reason,
disconnected-send failure, and successful-send evidence without converting any
of them into the primary cause. `INCONCLUSIVE_IDENTITY_MISMATCH` belongs in
`evidenceConclusion`, not the cause field.

For the earlier run, the required mapping is:

```text
primaryTerminationCause=CONNECTION_DETACH_CANCEL
taskTerminationKind=cancelled_without_task
terminalDecisionReason=task_finished_event_identity_mismatch
classifiedResultStatus=unknown
classifiedResultReason=task_identity_mismatch
classifiedResultFidelity=callback_plus_nonmatching_user_task_event
evidenceConclusion=INCONCLUSIVE_IDENTITY_MISMATCH
resultSendStatus=NO_SOCKET
resultDeliveryStatus=NOT_DELIVERED
lifecycleCleared=true
queueContextCleared=true
thisOrChildTimedOutAtFinalization=false
```

The earlier run is not a timeout even though nonterminal snapshots may have
observed a transient timed-out flag. Only an existing deadline/terminal decision
may select `EXISTING_COMMAND_DEADLINE`. The later run maps to
`NATURAL_TASK_FINISH` with matching-finish, successful-send, lifecycle-clear,
and queue-clear evidence.

"Exactly once" applies to the ledger's finalization and admission request. A
physical terminal record exists only if the non-Store terminal family still has
quota. Admission denial must be retained in bounded accounting and must not be
circumvented by another logger or aggregate family. Duplicate and late terminal
signals update only tombstone accounting and cannot mutate the frozen snapshot
or request a second summary.

## 11. Bounded Admission Contract

### 11.1 Shared session partition and classifier precedence

Current worktree source defines one shared process-lifetime diagnostic session.
The complete 64-slot critical reserve is:

```text
hard cap: 5000
ordinary ceiling: 4936
critical reserve: 64
canonical cap slots: 1
final snapshot slots: 1
abnormal Store terminal slots: 16
routine Store terminal slots: 8
exception/coverage slots: 16
aggregate checkpoint slots: 8
non-Store terminal slots: 8
suppression-control slots: 6
```

The loaded runtime independently logged the `5000 / 4936` cap values, but byte
identity with the current source remains unverified.

Future iron-pickaxe diagnostics must use this shared admission. Do not create an
independent cap or an event path that bypasses shared admission.

For general name-classified events, current classifier precedence is exact:

```text
1. EXCEPTION / COVERAGE_GAP / MISMATCH / OBSERVATION_FAILED / LINKAGE_FAILURE
   -> EXCEPTION_COVERAGE
2. SUPPRESSION / RATE_LIMIT / CAP_REACHED / BUDGET_EXHAUSTED / RESERVE_EXHAUSTED
   -> SUPPRESSION_CONTROL
3. TERMINAL
   -> NON_STORE_TERMINAL
4. SUMMARY / CHECKPOINT / SNAPSHOT
   -> AGGREGATE_CHECKPOINT
5. otherwise
   -> ORDINARY_DETAIL
```

Therefore `CRAFT_RESOURCE_ACQUISITION_TERMINAL_SUMMARY` is classified as
`NON_STORE_TERMINAL`, because terminal precedes summary.
`CRAFT_RESOURCE_EXPECTED_OBSERVED_MISMATCH` is classified as
`EXCEPTION_COVERAGE`. A hypothetical name containing both `TERMINAL` and
`MISMATCH` would also consume exception/coverage quota; this plan prohibits such
an ambiguous combined event name. Canonical cap, final snapshot, and the two
Store-terminal families remain explicitly owned by their existing producers.

Additional precedence examples:

```text
STORE_HOME_OPERATION_TERMINAL_SUMMARY -> NON_STORE_TERMINAL
ANY_TERMINAL_MISMATCH -> EXCEPTION_COVERAGE
ANY_TERMINAL_BUDGET_EXHAUSTED -> SUPPRESSION_CONTROL
BLOCK_OPTIONAL_META_MANAGER_FAILURE -> ORDINARY_DETAIL
BLOCK_OPTIONAL_META_MANAGER_EXCEPTION -> EXCEPTION_COVERAGE
BLOCK_OPTIONAL_META_MANAGER_COVERAGE_GAP -> EXCEPTION_COVERAGE
```

Reserve eligibility is not a delivery guarantee. When a family quota is
exhausted, its event is denied even if other critical families retain slots.
Critical subquotas are fixed and non-borrowing. Store-terminal admissions use
atomic four-record units; the other families use one slot per admission.

### 11.2 Emission and suppression controls

Mandatory controls:

```text
first unique state: emit immediately
unchanged progress: at most once per 200 game ticks or 10 seconds
per-correlation detail cap: no larger than the existing 256-event limit
shared session hard cap: 5000
all physical events: pass through shared admission
terminal, exception, mismatch, cap, and suppression events: use only their family quota
terminal aggregate state: may keep bounded updates after ordinary suppression
terminal aggregate physical record: emitted only if terminal-family admission succeeds
```

Mismatch traffic must not monopolize the process-wide exception/coverage
family. Per correlation, only the first two unique mismatch signatures may ask
for physical emission, and the iron-pickaxe producer as a whole may request at
most eight unique mismatch admissions per diagnostic session. Further unique or
duplicate mismatches update bounded, saturating counters only. A shared denial
increments `mismatchAdmissionDeniedCount` once for that fingerprint; no
alternative logger, summary family, or raw stdout path may retry it. These local
limits reserve no slot and do not guarantee emission.

Stable fingerprint fields may include:

```text
event family
command correlation
root and child Task classes
target role
normalized target position
expected and observed block IDs
requirement item and counts
semantic branch or transition
refusal, mismatch, exception, or terminal reason
```

Do not include:

```text
game tick or timestamp
Task or object identity hash
random UUID generated per log attempt
opaque Object.toString output
unordered collection rendering
full inventory, full NBT, or full world snapshot
stack trace in the fingerprint
```

### 11.3 Proposed hard bounds for diagnostic-only state

These are required design limits for any later implementation; they are not
implemented by this document:

| State | Hard limit |
| --- | ---: |
| simultaneously active iron-pickaxe command ledgers | 8 |
| retired-correlation tombstones | 8 |
| total active-ledger plus tombstone registry entries | 16 |
| attempt-transition detail eligibility per ledger | 64 |
| retained current semantic attempt tuple per ledger | 1 |
| retained target-history samples per ledger | 8 |
| unique exception signatures per ledger | 4 |
| unique exception signatures across this producer | 16 |
| unique mismatch signatures retained per ledger | 2 |
| unique mismatch signatures across this producer | 8 |
| mismatch signatures allowed to request emission per ledger | 2 |
| retained unowned-chain samples per ledger | 8 |
| retained unknown-association samples per ledger | 8 |
| Task classes in one retained path | 8 |
| expected block IDs in one retained set | 8 |
| opaque request or correlation ID | 360 UTF-8 bytes |

All occurrence, suppression, refusal, and sequence counters use saturating
`long` semantics and expose a saturation flag. Reaching a collection limit may
increment an already allocated aggregate counter, but must not allocate another
key, signature, attempt record, or sample.

The ledger never retains one object per attempt. It retains one current
semantic tuple, one saturating `targetAttemptSequence`, and at most eight target
history samples. After eight, it keeps the first four and most recent four and
increments `omittedTargetSampleCount`.

The first 64 ownership-proven attempt transitions per ledger are eligible for
ordinary transition detail. Later transitions still update the one current
tuple, saturating sequence, bounded first/final samples, and terminal aggregate,
but do not request another ordinary transition-detail record.

Opaque IDs required for equality evidence are never truncated and then treated
as equal. A blank or over-360-byte ID produces `UNAVAILABLE_OVERSIZE`, forces
association to `UNKNOWN`, and cannot prove ownership. A bounded hash may serve
only as an internal deduplication key; it is never a replacement for, or a way
to reconstruct, the original ID.

When eight active ledgers exist, a ninth activation is refused
deterministically; an active ledger is never evicted to admit it. The refusal
updates one fixed process-level saturating counter and may request one
`COVERAGE_GAP` record through shared admission. Finalization removes the active
ledger and creates a tombstone. When the eight-tombstone limit is reached, only
the oldest tombstone by monotonic retirement sequence is replaced. A late event
matching a tombstone increments a bounded late-event counter and cannot reopen
the ledger. A tombstone expires after at most 200 client ticks or 10 monotonic
seconds, whichever occurs first. That expiry is diagnostic retention only. An
event whose old tombstone has expired also cannot allocate a new ledger without
a new authoritative root-activation observation.

Diagnostic state is cleaned at these existing boundaries:

```text
natural terminal finalization
detach/cancel finalization after existing lifecycle cleanup
command-context unbind or connection/session retirement fallback
session reset
world or dimension change
client-tick regression
diagnostics transition to OFF
clean process teardown
```

Strict `OFF` applies to the proposed incident producer. Every public entry point
returns before ID normalization, fingerprint construction, state capture, ledger
lookup or creation, sample/signature retention, or shared admission when
`ChatClefDiagnostics.isBoundaryEnabled()` is false. Transition to `OFF` clears
all incident ledgers and tombstones before `OFF` is published; re-enabling starts
with empty incident state. This is not a claim that the existing global static
diagnostics facade or process-lifetime admission authority allocates no fixed
objects or resets its counters. Overlay/HUD state is independent: neither
diagnostics mode nor cleanup may read or change overlay visibility.

Registry mutation is serialized by one narrow diagnostics-only owner. It may
copy a bounded immutable snapshot while locked, but formatting, admission,
physical logging, I/O, Task calls, inventory reads, and Baritone calls occur
outside the lock. It may not wait, sleep, retry, block gameplay, or reorder an
existing callback. Every emitted record carries `behavior_effect=none`.

### 11.4 Exact UTF-8 and exception-payload bounds

The shared formatter accepts a caller-selected limit; it does not define a
single global event-size constant. Every proposed physical record selects an
8,192-byte UTF-8 limit, matching existing bounded StoreHome and StoreDeposit
producers. The limit is measured after diagnostic value encoding and includes
the prefix, base, required, and optional fields. Current normalization is:

```text
field-key maximum: 96 UTF-8 bytes
initial field-value maximum: 256 UTF-8 bytes
optional overflow handling: remove optional fields from the tail first
required-value fallback limits: 128, 96, 64, 32, 16, 8, 4, then 1 UTF-8 bytes
final fallback: boundedPayloadUnavailable=true within the same 8,192-byte cap
capture marker: diagnosticCaptureStatus=complete|partial
```

Five validated opaque identity fields are exceptions to required-value
shrinking and are preserved exactly: `commandRequestId`,
`commandCorrelationId`, `commandSessionId`, `rootAssignmentId`, and
`boundRootTaskInstanceId`. A static contract verifies that the actual terminal
required-key set plus all five 360-byte worst-case IDs fits in the 8,192-byte
physical limit. Other required values may reach the 4-byte or 1-byte fallback
under an extreme payload and must then report partial capture; this is not a
claim that every required scalar is retained verbatim.

Collection and stack bounds:

```text
bounded target-history samples: at most 8, plus omittedCount
bounded Task-path summary: at most 8 classes, plus omittedCount
bounded expected-block list: sorted, normalized, and truncated with omittedCount
first bounded stack excerpt per unique exception type and boundary: at most 8 top frames
each stack-frame field: at most 256 UTF-8 bytes before formatter fallback
all stack-frame fields together: at most 2048 UTF-8 bytes before event formatting
later identical exceptions: no stack; count plus first/last observation only
```

Every truncated collection supplies `omittedCount`; every truncated scalar or
stack excerpt supplies an explicit truncation or partial-capture marker. Stack
data also records `framesAvailable`, `framesSelected`, and `framesOmitted`. The
formatter does not add collection omission fields automatically; the producer
must supply them. Stack data is optional and is captured only when an exception
already exists at an existing caught or propagated boundary. Diagnostics must
not introduce a new catch, change propagation, or throw an exception merely to
obtain a stack.

## 12. Responsibility Separation For The Diagnostics Implementation

The later separately authorized source implementation follows this planned
separation under a focused crafting/resource-acquisition diagnostics package.
A single broad manager remains prohibited.

Conceptual responsibility layout:

```text
lavi/minecraft/diagnostics/crafting/acquisition/
  scope/         exact iron-pickaxe activation and bounded registry ownership
  association/   root-descendant, concurrent-unowned, and unknown classification
  requirement/   requested/current/target count and recipe-requirement snapshots
  target/        resource-stage, target-role, and bounded attempt accounting
  terminal/      two-phase immutable finalization and one admission request
```

Existing owners remain in place:

```text
diagnostics/mining/       mining, progress, blacklist, and Baritone observations
diagnostics/interaction/  actual block interaction and screen observations
diagnostics/session/      shared admission, reserve, cap, and family classification
```

Do not move, rename, split, or repackage upstream-derived ChatClef/AltoClef
classes. Do not create a generic future diagnostics framework merely for this
incident.

Ownership classification:

```text
new lavi.minecraft diagnostics collaborators: LAVI-owned
adris.altoclef task classes: upstream-derived baseline with existing LAVI diagnostics hunks
baritone.api.utils.BlockOptionalMeta runtime class: source ownership/path unverified in checkout
```

The exact implemented layout and remaining artifact-provenance limitations are
recorded in Section 18. The initial documentation approval was not itself
treated as source approval.

## 13. Test-First Contract Applied To The Later Source Work

No tests were added or run by the initial documentation-only step. After the
separate diagnostics-source authorization, tests were written first for these
contracts; current execution evidence is recorded in Section 18.

1. A non-iron-pickaxe command does not activate the incident-scoped ledger.
2. `OFF` mode creates no incident ledger, sample, signature, fingerprint, or
   admission request; transition to `OFF` clears incident state.
3. The exact iron-pickaxe request, correlation, session, connection generation,
   and root assignment survive projections without being treated as ownership
   proof by themselves.
4. A UserTaskChain event with proven lineage from the bound root is
   `COMMAND_ROOT_DESCENDANT`.
5. The same command IDs plus actual emitting-Task membership in
   `DepositAllInventoryPressureChain` are `CONCURRENT_CHAIN_UNOWNED` and do not
   increment command-owned counters.
6. Missing, truncated, or discontinuous lineage and cross-thread evidence without
   a captured immutable token are `UNKNOWN`; matching Task classes cannot
   upgrade them.
7. Requested, current, target, and delta counts remain distinct. Under the
   source-backed additional-acquisition contract, request `1` plus captured
   current count `1` records target `2`; absent loaded-artifact proof is explicit.
8. Resource stage and target role are independent fixed vocabularies with no
   combined `OR` labels.
9. Candidate return, object-identity churn, and equal reconciliation allocate no
   attempt before active-target proof; the first proven active tuple gets one
   `targetAttemptSequence`.
10. An unchanged active tuple across ticks, goal submissions, and Task returns
    keeps that sequence; a different active tuple or authoritative reselection
    after existing abandonment starts exactly one next attempt.
11. Detail suppression and shared-admission denial never increment the attempt
    sequence by themselves.
12. A semantic target role and matching observed block do not emit a mismatch.
13. A same-boundary expected/observed difference may request one mismatch without
    rescanning or changing the target.
14. Duplicate mismatch signatures emit no additional record; only the first two
    unique signatures per correlation may request emission.
15. The third unique mismatch in one correlation and the ninth unique mismatch
    from this producer in one diagnostic session update accounting only; family
    exhaustion denies emission without borrowing quota.
16. `BLOCK_OPTIONAL_META_MANAGER_EXCEPTION` and
    `BLOCK_OPTIONAL_META_MANAGER_COVERAGE_GAP` map to `EXCEPTION_COVERAGE`; the
    historical `..._FAILURE` name maps to ordinary, and an engine exception is
    never mislabeled as diagnostic `OBSERVATION_FAILED`.
17. Existing mining, furnace, container, reconciliation, and interaction events
    remain the sole authorities for their facts; the new projection cannot
    create a second conflicting observation.
18. Retry messages equivalent to `Try 1 / 4` through `Try 5 / 4` update command
    counters only when root-descendant ownership is proven; other observations
    remain unowned or unknown.
19. Ordinary detail exhaustion still allows a terminal or exception emission
    request, but a physical record exists only while that exact family quota
    remains. Aggregate or raw logging cannot bypass a denial.
20. The first unique exception records one bounded stack excerpt; repeats retain
    count and first/last observations without another stack.
21. Exception propagation remains identical; no new catch converts, suppresses,
    retries, or terminates engine behavior.
22. Eight active ledgers and eight tombstones enforce the deterministic refusal,
    retirement, late-event, and oldest-tombstone replacement rules without
    evicting an active ledger.
23. Attempt-detail, sample, exception, mismatch, Task-path, and expected-block
    limits refuse new detail or retained entries while current-state and
    saturating aggregate counters remain bounded.
24. Every payload is at most 8,192 UTF-8 bytes; key/value fallback, optional-tail
    removal, `diagnosticCaptureStatus`, truncation markers, and `omittedCount`
    remain deterministic.
25. A detach trigger freezes `CONNECTION_DETACH_CANCEL` and emits zero terminal
    summaries.
26. Detach, owned-root cancellation, `cancelled_without_task`, identity mismatch,
    and `NO_SOCKET` still emit zero summaries before cleanup and unbind.
27. Detach lifecycle clear without matching context unbind emits zero; the
    matching unbind completes exactly one finalization request.
28. Identity mismatch, transient timeout observations, and send failure cannot
    overwrite the frozen detach cause.
29. A matching natural finish, successful send, queue/context clear, and normal
    lifecycle clear produce one `NATURAL_TASK_FINISH` summary request.
30. Immediate or asynchronous send failure on a non-detach path leaves the
    diagnostic summary pending while existing ownership remains active.
31. The 200-tick/10-second diagnostic fallback produces one incomplete summary
    request and zero engine mutations, sends, cancels, retries, or clears.
32. Later cleanup, duplicate detach/callback/TaskFinished signals, and late send
    completion update tombstone accounting only and cannot re-emit or mutate the
    frozen summary.
33. Terminal-family admission denial is attempted once, recorded as denied, and
    never retried through a log storm or another family.
34. An in-flight terminal send that causes existing detach deferral cannot
    finalize until the existing send-completion and detach-cleanup outcomes are
    known or the diagnostic-only fallback closes.
35. A selected-chain transition is recorded without changing TaskRunner or
    chain priority.
36. Fingerprints exclude Task identity, tick, timestamp, random IDs, and opaque
    object strings.
37. Wire DTOs, command results, acknowledgement order, and terminal semantics
    remain byte-for-byte unchanged.

Any test that requires changing gameplay behavior to produce diagnostics is
invalid for this phase.

## 14. Runtime Acceptance Criteria For A Later Authorized Reproduction

A later reproduction may be evaluated only after separately authorized build,
deployment, Minecraft restart, and artifact provenance checks.

The trace is sufficient only when it proves or explicitly marks unavailable:

```text
loaded-artifact provenance and source-backed additional-acquisition quantity decision
full active requirement transitions
root assignment, generation, object identity, and actual descendant association
explicit unowned and unknown concurrent observations
resource stage, target role, attempt sequence, and expected block set
actual world block at each entered material boundary
Baritone goal/path relation to the target
reach, interaction, GUI, mining, or placement transition actually entered
existing unreachable and blacklist ownership
concurrent selected-chain transitions
BlockOptionalMeta association or non-association with the command
last successful boundary
first explicit failure boundary
first unobserved boundary after a cap
frozen primary terminal cause
Task termination kind, result classification/fidelity, and delivery result
lifecycle clear, queue/context unbind, and finalization boundary
family admission result and any incomplete coverage
```

One later successful run is not enough to close the earlier incident. A failing
or loop-like run with complete bounded evidence is needed to verify causality.

## 15. Prohibited Changes

This diagnostics direction must not be used to justify any of the following:

```text
adding or changing a gameplay timeout
adding or changing retry counts or timing
changing blacklist thresholds, reset, or exclusion behavior
changing target selection or scanner predicates
forcing crafting-table, furnace, chest, or ore selection
changing recipe or requested-count semantics
using ambient command IDs or matching Task classes as ownership proof
truncating opaque IDs and then treating the truncated values as equal
changing Task isFinished, isEqual, interruption, or replacement behavior
changing UserTaskChain, TaskRunner, or automatic-deposit priority
cancelling all Baritone paths or goals
releasing global input
adding a broad try/catch or catch(Throwable)
suppressing the BlockOptionalMeta exception
publishing or replacing a LootDataManager
writing fallback output into an authoritative cache
deleting or resetting Baritone cache while Minecraft is running
changing command wire DTOs or terminal payloads
bypassing shared admission or borrowing another diagnostic family quota
using a diagnostic retention window as a gameplay timeout
coupling diagnostics state to overlay or HUD state
moving, renaming, splitting, or repackaging upstream-derived classes
changing Fabric, Minecraft, ChatClef, AltoClef, Baritone, Java, or Gradle versions
```

If diagnostics show that two or more upstream lifecycle owners require changes
in the first pass, stop. That is evidence that the failure boundary is not yet
narrow enough.

## 16. Decision Gate

Current decision after the later separate diagnostics-source authorization:

```text
documentation: COMPLETE
diagnostics source: AUTHORIZED SEPARATELY AND IMPLEMENTED
behavior source: PROHIBITED WHILE ROOT CAUSE IS UNKNOWN
Java build: NOT AUTHORIZED BY THIS DOCUMENT
Minecraft restart/reproduction: NOT AUTHORIZED BY THIS DOCUMENT
commit/push: NOT AUTHORIZED
```

The following read-only audit was required and completed before the
diagnostics-only implementation. It remains required again before any wider
source proposal:

```text
current Git status and preserved dirty files
current source event producers and tests
loaded runtime JAR path and immutable hash evidence
whether BlockOptionalMeta source exists and its verified provenance
smallest LAVI-owned observation boundary
whether one upstream minimal hunk is still required
exact hot-path controls and shared-admission classification
exact proposed files, methods, and tests
```

## 17. Related Documents

```text
plugins/Minecraft/docs/minecraft-backend-separation.md
plugins/Minecraft/docs/chatclef-carryon-integration-direction.md
plugins/Minecraft/docs/chatclef-baritone-cache-troubleshooting.md
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
plugins/Minecraft/docs/chatclef-resource-target-retry-thrashing-analysis.md
plugins/Minecraft/docs/chatclef-korean-item-action-alias-v2-plan.md
plugins/Minecraft/docs/chatclef-python-command-orchestration-plan.md
plugins/Minecraft/docs/chatclef-bare-deposit-container-handoff-loop-investigation.md
plugins/Minecraft/docs/chatclef-bare-deposit-diagnostics-plan.md
plugins/Minecraft/docs/chatclef-automatic-deposit-post-checkpoint-direction-2026-08-31.md
    current working-tree-only reference at documentation time; tracking status
    must be rechecked before any future isolated commit
```

## 18. Diagnostics-Only Implementation Record (2026-09-01 through 2026-09-02)

After Section 16 was written, the user separately authorized the bounded
diagnostics source work described by this plan. That authorization did not
authorize a behavior fix, Java build, deployed-JAR replacement, Minecraft
restart, runtime reproduction, commit, or push.

The implementation is split by ownership rather than placed in one lifecycle
class:

```text
lavi/minecraft/diagnostics/crafting/acquisition/
  scope/         exact command activation, eight-scope registry, tombstones
  association/   immutable root/chain/Task evidence and bounded gap accounting
  requirement/   captured quantity decision, progress ledger, artifact-proof gate
  target/        stage, role, attempts, closure, position, failure and mismatch ledgers
  terminal/      two-phase terminal ledger, retention, immutable summary state
  event/         source vocabulary, shared-family contract, 8,192-byte formatting
  source/        side-effect-free callbacks, including container reconciliation/lifecycle

lavi/minecraft/fabric/chatclef/bridge/command/diagnostics/crafting/
  scope/ association/
  requirement/progress/
  target/
  container/activation/ container/common/ container/lifecycle/ container/reference/
  interaction/matching/ interaction/reference/
  terminal/payload/required/ terminal/payload/optional/
```

The upstream-derived engine was not reorganized. Existing source boundaries
received only the smallest diagnostics callback or ordering hunk needed to pass
already-captured values after the authoritative log. Command lifecycle details
are frozen once and the same immutable map is supplied to the authority log and
terminal projection.

Implemented fail-closed contracts:

```text
exact optional-@ `get iron_pickaxe 1` activation only
request/correlation/session/connection/root identity retained without truncation
Task path retained to eight entries; truncated lineage classifies UNKNOWN
null emitting Task classifies UNKNOWN; ambient current Task is never substituted
quantity arithmetic captured from the existing single inventory count
quantityContract=UNAVAILABLE while the loaded artifact remains unverified
source-linked projections proceed only after shared physical emission completion
requirement transitions use a command-scoped semantic progress ledger
candidate/equal/goal-only observations do not start an attempt
active DestroyBlock ownership starts an attempt
exact selected container child remains a candidate until the scheduler installs it
exact applied container replacement starts furnace/crafting-table interaction attempts
suppressed reconciliation source clears exact Task references but cannot mutate the target ledger
same-parent candidate reconciliation consumes the exact pending Task reference once
candidate/active identity divergence invalidates stale references and records a coverage gap
an already active owner slot cannot be overwritten by a later candidate or another parent
container owner stop is emitted only for an exact owner already sealed in the eight-scope registry
owner-stop projection uses the sealed scope key, never ambient command context or current Task path
one owner identity found in multiple bounded scopes consumes the ambiguous exact references fail-closed, derives no target closure, and records a gap for each still-active scope
suppressed owner-stop source drops exact Task references without a derived target closure
replacement, clear, or owner-stop closes only the exact active tuple and matching targetAttemptSequence
proven abandonment or command terminal closes the exact active tuple
first 64 attempt/reference transitions are detail-eligible
target history keeps first four plus most recent four
expected-block input over 64 entries fails closed before projection
mismatch requires a same-boundary proven active tuple and exact current-tuple match
mismatch unique output is capped at two per correlation and eight per session
the session mismatch fingerprint excludes opaque command correlation identity
missing expected/observed values increment COVERAGE_GAP accounting, not mismatch
unreachable and blacklist facts retain exact owned-attempt attribution in a failure aggregate
projection failures retained as bounded association coverage gaps
block interaction ownership uses a one-shot, same-thread, exact-position, one-tick token
interaction reuse requires stage, role, canonical position, and observed block-ID agreement
HEAD captures one immutable interaction reference and RETURN reuses that reference
terminal text retained at no more than 512 UTF-8 bytes
terminal required fields are assembled by deterministic responsibility contributors
absent terminal snapshots preserve the required schema with explicit unavailable values
detach freezes cause but does not claim an unexposed lifecycle-clear CAS result
terminal fallback closes once at 200 ticks or 10 monotonic seconds
diagnostics OFF clears the incident-owned registries
```

Tests were written before each implementation slice. The Python static contract
currently covers responsibility layout, one-type-per-file, source ordering,
forbidden behavior APIs, quantity capture, ownership, mismatch, caps, terminal
fail-closed behavior, UTF-8 retention, and single details snapshotting. Java unit
tests cover the pure registries, classifiers, ledgers, caps, history, mismatch,
exception excerpts, and two-phase terminal rules.

Known evidence limitations remain explicit rather than inferred:

```text
loaded JVM JAR SHA/source equality: UNVERIFIED
quantityContract artifact proof: UNAVAILABLE_LOADED_ARTIFACT_UNVERIFIED
sourceEventSequence: UNAVAILABLE_SOURCE_EMITTER_DOES_NOT_EXPOSE_SEQUENCE
shared physical dispatch result: AVAILABLE_ADMITTED_AND_EMISSION_COMPLETED
BlockOptionalMeta source and entry boundary: UNAVAILABLE_SOURCE_NOT_PRESENT_IN_CHECKOUT
detach lifecycle-clear CAS result: UNAVAILABLE in the existing source DTO
classified result reason not present in the existing terminal DTO: UNAVAILABLE
currentItemCountAtTerminal: UNAVAILABLE_NO_TERMINAL_INVENTORY_SNAPSHOT
lastSuccessfulBoundary: UNAVAILABLE_NOT_CAPTURED_SEPARATELY
firstExplicitFailureBoundary: UNAVAILABLE_NOT_CAPTURED_SEPARATELY
firstUnobservedBoundaryAfter: UNAVAILABLE_NOT_CAPTURED_SEPARATELY
CraftInTableTask exception before delegated DoStuffInContainerTask.onStop: UNAVAILABLE_PRE_SOURCE_COVERAGE_GAP
container interaction physical mismatch event: UNAVAILABLE_MINING_PROJECTOR_ONLY
```

The outcome-capable shared emitter separates admission from physical sink
completion. Source-linked projections use `emissionCompleted()` and never
fabricate lineage after a cap, suppression, or sink failure. The legacy void
facade remains for existing callers. `sourceEventSequence` is still unavailable
and is not inferred or reconstructed.

The five validated opaque identity fields remain exact. Under extreme terminal
payload pressure, non-identity required values may shrink through the
128/96/64/32/16/8/4/1-byte fallback sequence and
`diagnosticCaptureStatus=partial` records that loss.

Verification status for this implementation record:

```text
targeted iron-pickaxe Python static contract: 48 passed
related Fabric ChatClef Python contracts: 103 passed; one existing deprecation warning
full hermetic Minecraft runtime: 550 passed, 2 live-only skipped, 498 subtests passed
earlier checkpoint automatic-deposit hermetic slice, consecutive run 1: 300 passed, 218 subtests passed; not rerun as an isolated slice after the latest container seam
earlier checkpoint automatic-deposit hermetic slice, consecutive run 2: 300 passed, 218 subtests passed; not rerun as an isolated slice after the latest container seam
latest full Python suite in sandbox: 1677 passed, 4 skipped, 3547 subtests passed; one Windows named-pipe permission failure
latest isolated named-pipe integration test outside sandbox: 1 passed
latest scoped Ruff after the static contract addition: passed
Java unit-test sources: added, not executed
Gradle clean build: not executed; separately authorized action
deployed JAR/runtime reproduction: not executed; separately authorized action
running Minecraft JVM contains this source: no; rebuild/deploy/restart not performed
behavior source changes: none
commit/push: none
```
