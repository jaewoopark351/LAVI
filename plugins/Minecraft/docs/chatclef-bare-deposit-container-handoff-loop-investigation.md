<!-- 20260819_kpopmodder: Documented the bare deposit container-handoff loop without changing runtime behavior. -->

# ChatClef Bare Deposit Container Handoff Loop Investigation

Date: 2026-08-19

이 문서는 `LAVI_TEST_Fabric01`에서 bare `deposit` 명령 하나가
`StoreInAnyContainerTask`에 진입한 뒤 자연 완료되지 않고 반복된 사건을
기록한다.

현재 판정은 다음과 같다.

```text
분류: 명시적 취소 전까지 자율 종료하지 않은 실질적 비종료 반복
가장 가능성 높은 실패 영역:
    StoreInAnyContainerTask의 기존-container 거리/branch commitment
      -> chest 획득과 crafting-table child lifecycle의 조정
      -> Baritone goal/path ownership과 container interaction
      -> ContainerStoredTracker가 관찰할 수 있는 슬롯 이동

유력한 하위 가설:
    parent raw candidate와 child filtered candidate의 불일치

확정되지 않은 사항:
    위 영역 안에서 최초로 실패한 정확한 하위 단계와 조건
```

이 문서는 문서화와 읽기 전용 진단 결과만 기록한다. Java 또는 Python
behavior 변경, diagnostics source 변경, wire payload 변경, Gradle 변경,
dependency 변경, Minecraft 실행, runtime 재현, cache 삭제, commit 또는
push를 승인하지 않는다.

## Scope And Related Documents

적용 범위:

```text
Fabric ChatClef bridge command context
AltoClef DepositCommand
StoreInAnyContainerTask
DoToClosestBlockTask / AbstractDoToClosestObjectTask
CraftInTableTask / DoCraftInTableTask
Baritone goal, path calculation, adoption, and exploration ownership
container interaction and tracked storage side effect
bounded StoreInAnyContainer diagnostics
```

관련 문서:

```text
plugins/Minecraft/docs/chatclef-post-completion-store-loop-investigation.md
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
plugins/Minecraft/docs/chatclef-baritone-cache-troubleshooting.md
plugins/Minecraft/docs/chatclef-resource-target-retry-thrashing-analysis.md
plugins/Minecraft/docs/chatclef-carryon-integration-direction.md
```

이 사건은 2026-08-07 post-completion store loop와 증상이 비슷하지만,
시간창과 task 출처가 다른 별도 증거 집합이다. 이번 사건에서는 Store task의 출처가
단일 bare `deposit` 호출로 확인되며, Fabric bridge command 완료 뒤 잘못
남은 child task로 분류하지 않는다.

여기서 `bare deposit`은 `explicitItemListProvided=false`인 기본 15-target
deposit을 뜻한다. 원문 command text는 보존되지 않았으므로 사용자가
실제로 `@` prefix를 입력했는지는 이 로그로 판정하지 않는다.

## Evidence Provenance

Primary runtime evidence:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\instance_audit.txt
C:\Vtuber_Souorce_Code\LAVI\logs\20260819_124318_log.txt
```

Evidence handling:

```text
latest.log
    canonical event sequence and line references used below

stdout-logs.txt
    the same Minecraft session's Log4j XML/CDATA serialization
    independent second execution evidence로 계산하지 않음

instance_audit.txt
    CurseForge instance/launch provenance
    command lifecycle or storage-side-effect evidence로 사용하지 않음

LAVI application log
    Fabric bridge commands and Python-side connection state 확인용
    bare deposit의 owner는 아님
```

Finalized Minecraft-session artifact manifest:

```text
latest.log
    bytes: 34,302,252
    lines: 31,429
    last write: 2026-08-19 14:23:19.496 KST
    SHA-256: 784f19997b47f224890ecb2e9321c8cddf2c5f3f17624adef50fc15c5385b811

stdout-logs.txt
    bytes: 39,686,073
    lines: 94,088
    last write: 2026-08-19 14:23:20.811 KST
    SHA-256: b150f8dc306879e452e85f989c88dd7f3923f6101b8b77ed42c3ef05c049c2a5

instance_audit.txt
    bytes: 1,856,229
    lines: 476
    last write: 2026-08-19 12:43:46.043 KST
    SHA-256: 4f29211fda2fbda32dcef56900f950ef7d7a037cd9377ea6977f4049ead0be98
```

All three requested `LAVI_TEST_Fabric01` artifacts are non-empty. A recursive
read-only inventory also found all 12 requested `latest.log`,
`instance_audit.txt`, and `stdout-logs.txt` files across the four matching
CurseForge instances to be non-empty. Only the `LAVI_TEST_Fabric01` artifacts
above belong to this incident.

Count and line-number scopes:

```text
previous interim audit cutoff: latest.log line 31,327 at 13:56:01 KST
finalized latest.log EOF: line 31,429 at 14:23:19 KST
deposit command/lifecycle window: lines 4,283-31,324
post-command idle and orderly shutdown tail: lines 31,325-31,429
```

The exact incident counts below were recomputed against the finalized file and
remain scoped to the deposit command/lifecycle window. Unless a count is
explicitly labelled as a loose literal search, an event count matches the
primary structured field `level=BOUNDARY event=<name>` rather than every line
whose nested summary payload happens to repeat the event name.

The LAVI application log was still being appended by unrelated application
features after Minecraft exited, so it is not assigned a whole-file hash here.
The cross-correlation below uses its bounded Minecraft bridge records through
line 1,263. An independent review package must include the original runtime
artifacts, or an immutable copy matching the manifest above; this document and
a source ZIP alone cannot independently reproduce the runtime counts.

Loaded runtime identity:

```text
Minecraft instance: LAVI_TEST_Fabric01
Minecraft: 1.20.1
Fabric Loader: 0.19.3
ChatClef runtime JAR:
    C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\chatclef-1.20.1-0.18.23.jar
runtime JAR bytes: 6,399,813
runtime JAR last write: 2026-08-15 02:14:56.060 KST
runtime JAR SHA-256:
    c52f33e31ae10887ce4a280d0f97bac059fd5465e0ee1c8d6a1c8bfdac330531
diagnostics marker: 20260731_post_place_handoff_p2
```

Current source audit identity:

```text
repository root: C:\Vtuber_Souorce_Code\LAVI
repository HEAD: 2f6ab5f1d1983a54987908b3f8bce3db4040e9ac
Java audit root:
    plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java
git status --short -- <Java audit root>: no entries
git diff --name-only -- <Java audit root>: no entries
git diff --cached --name-only -- <Java audit root>: no entries
git ls-files --others --exclude-standard -- <Java audit root>: no entries
```

The Java-diff statement is a local repository observation covering staged,
unstaged, and untracked files at audit time. A detached source archive that
does not contain `.git` metadata cannot independently prove this working-tree
state merely from its archive comment or displayed commit identifier.

The loaded JAR now has an artifact hash, but no build produced from the current
source was generated and hash-matched to it during this read-only audit.
Current source is therefore corroborating structural evidence, not proof that
every current source byte was present in the loaded JAR.

### LAVI Bridge Cross-Correlation

The auxiliary LAVI application log identifies the same bridge session without
claiming ownership of the local deposit:

```text
LAVI log lines 111 and 114, 12:44:27
    server started at ws://127.0.0.1:4316
    generation 1 client connected
    session=fabric-chatclef-ff162a0e39b741cfb0131aff21454bf2

latest.log lines 856-857, 12:44:27
    generation 1 connected
    the same session identifier was accepted by the handshake

LAVI log lines 147-153 and 370-402
    the two bridge-owned requests are get iron_helmet 1 and get iron_sword 1
    both have request, correlation, and session identifiers
    both reach completed results

LAVI log line 1,263, 14:23:19
    the same session and generation disconnect with active_request=null
```

There is no LAVI `command gate` or `command result` record for deposit. By
contrast, `latest.log` line 4,283 records the normalized local
`commandName=deposit` with `commandContextAvailable=false` and blank request,
correlation, and session identifiers. The raw chat text was not retained, so
this still does not prove whether the user typed an `@` prefix.

## Verified Timeline

The relevant `latest.log` timeline is:

```text
12:44:27 lines 856-857
    Fabric bridge generation 1 connected and handshake accepted.

12:49:27 line 1619
    preceding get iron_helmet 1 reached terminal_result_sent
    lifecycle_cleared=true

13:01:45 line 4221
    preceding get iron_sword 1 reached terminal_result_sent
    lifecycle_cleared=true

13:03:49 line 4283, clientTickId=23522
    exactly one DEPOSIT_COMMAND_INVOCATION_DECISION
    taskInstanceId=1340
    task identity=7945adfb
    explicitItemListProvided=false
    selectedItemTargetCount=15
    commandContextAvailable=false
    commandContextError=no_active_command

13:03:49 line 4291, clientTickId=23523
    exactly one STORE_IN_ANY_CONTAINER_START for the same task identity

13:03:49 lines 4292-4293
    first observed branch=return_open_existing_container
    raw logged position=-2350,-1,1705
    closestWithinRange=true
    storedCountByTarget is zero for every target

13:03:53 lines 4404-4405, clientTickId=23604
    first observed progress signature using return_obtain_chest_item
    dungeonChestCacheSize=25
    nonDungeonChestCacheSize=0
    storedCountByTarget remains zero for every target

13:04:04 line 4566, clientTickId=23819
    cobblestone in notStored changed from 317 to 256
    storedCountByTarget still reported cobblestone=0/317
    the cause and destination of the 61-item difference are unresolved

13:05:21 lines 8988-8991
    generation 290 emitted the last matching calculation/adoption pair
    generation 291 emitted worker-started and scheduled records only
    later actual Baritone work is not fully covered by matching structured
    calculation-completed/adoption records

13:06:07 line 9477, clientTickId=26284
    totalObservationCount=2762
    branchChangeCount=255
    notStoredCount=15
    cobblestone in notStored remains 256
    storedCountByTarget remains zero for every target

13:06:07 line 9480
    store progress diagnostic session cap 256 reached
    this cap stopped detail emission, not the Store task

13:09:20 lines 11044-11076
    first Baritone path-calculation NullPointerException episode
    this first stack identifies UserBlockRangeTracker
    -> AltoClefSettings.shouldAvoidBreaking

13:17:48, 13:22:16, 13:27:43, 13:30:19, 13:41:07
    five additional Baritone Pathing exception episodes
    these later events contain only a generic NullPointerException line
    and must not be assigned to UserBlockRangeTracker without a stack

13:09:20 line 11077 and after each later episode
    the same top-level Store task continued after the exception

13:46:55 line 28636
    notStoredCount=15 remained in a repeat summary
    detailed per-target stored counts were no longer emitted after the cap

13:54:36 lines 31299-31302
    StopCommand called AltoClef.stop()
    USER_TASK_CHAIN_CANCEL_REQUESTED
    STORE_IN_ANY_CONTAINER_STOP
    task identity 7945adfb cleared
    duration_seconds=3046.4039998054504

13:54:36 line 31324
    bridge ignored the cancelled_without_task TaskFinishedEvent
    because there was no active LAVI command to own it

14:23:18-14:23:19 lines 31401-31429
    player logout, server save, and orderly client shutdown
    bridge stopped and WebSocket close status=1000 was handled as bridge stopping

stdout-logs.txt line 94088
    process wrapper recorded EXIT code=0, terminatedByApp=false
```

The observed run therefore lasted about 50 minutes 46 seconds and ended only
after an explicit `StopCommand` cancellation. It did not reach natural Store
completion in the recorded window. The Minecraft process then remained up for
about another 28 minutes 43 seconds before an orderly shutdown. That later
disconnect is neither an in-command disconnect nor a deposit terminal result.

## What Is Proven

The evidence directly proves all of the following:

1. One bare `deposit` invocation created one `StoreInAnyContainerTask` root.
2. The deposit was not submitted through an active LAVI WebSocket command
   context. Its request, correlation, session, source, and command text fields
   were empty with `no_active_command`.
3. There is no duplicate deposit invocation in this session.
4. Task instance `1340`, identity `7945adfb`, remained the same top-level Store
   operation until explicit cancellation.
5. Both `return_open_existing_container` and `return_obtain_chest_item` progress
   signatures were observed.
6. A partial inventory-target state change is visible: cobblestone in
   `notStored` fell from 317 to 256 while `storedCountByTarget` remained
   `0/317`, and the final detailed checkpoint still reports 256 in
   `notStored`. This proves a recorded state difference, not that 61
   cobblestone reached a container; its cause and destination remain
   unresolved.
7. At the last bounded Store progress detail, all tracked
   `storedCountByTarget` values were still zero and all 15 target groups were
   still present in `notStored`. A later repeat summary at 13:46:55 still had
   `notStoredCount=15`, but did not contain exact per-target stored counts.
8. No natural Store finish or success event was emitted before cancellation.
9. The diagnostic hard cap did not stop the behavior loop.
10. Six Baritone pathing NPE episodes occurred after the repeating behavior had
   already started, and the same top-level operation continued afterward. Only
   the first episode has a stack that identifies `UserBlockRangeTracker`.
11. The deposit window contains no fatal crash or connection loss. The later
    player logout, bridge stop, status-1000 WebSocket close, and process exit
    belong to the orderly session shutdown after the deposit was cancelled.

This is sufficient to classify the user-visible behavior as an effective
non-terminating loop or stall. It is not a mathematical proof that the task
could never eventually finish under every future world state.

## Current Source Boundary

### 1. Parent Uses A Raw Container Candidate

`StoreInAnyContainerTask` first performs an unfiltered nearest-block lookup:

```text
StoreInAnyContainerTask.java:125
    getNearestBlock(StoreInContainerTask.CONTAINER_BLOCKS)
```

The parent enters the open-existing-container branch when a raw candidate is
present and either of these range checks passes:

```text
StoreInAnyContainerTask.java:126-128
    raw closest is within 50 blocks
    OR _currentChestTry is within 70 blocks
```

The 70-block condition is not explicitly bound to equality between the newly
returned raw `closest` candidate and `_currentChestTry`. This is a structural
risk, but the current log does not prove that a mismatched pair triggered a
specific transition in this incident.

The first observed transitions align closely with the 50-block gate. Using the
logged chest position center around `-2349.5,-0.5,1705.5`:

```text
latest.log:4292  OPEN    distance about 47.632
latest.log:4404  OBTAIN  distance about 50.045
latest.log:4441  OPEN    distance about 49.979
latest.log:4484  OBTAIN  distance about 50.032
```

This is a strong numerical correlation with the 50-block threshold, not direct
proof that the same raw candidate crossed the threshold and caused each
transition. The obtain-branch diagnostic discards the local raw candidate, so
it cannot say whether an obtain tick had no raw candidate or had a candidate
outside the allowed range.

### 2. Child Performs A Separate Filtered Search

After the parent chooses the open branch, it returns a
`DoToClosestBlockTask` with `validContainer`:

```text
StoreInAnyContainerTask.java:157-166
```

The predicate can reject a candidate because:

```text
a chest is obstructed above and cannot be cleared
the container cache says it is full
a chest is classified as a dungeon chest that should be avoided
```

`DoToClosestBlockTask` does not consume the parent's raw `closest` value.
Without a custom search function it performs another lookup:

```text
DoToClosestBlockTask.java:57-63
    getNearestBlock(pos, isValid, targetBlocks)
```

If the child search has no candidate, `AbstractDoToClosestObjectTask` returns a
wander task:

```text
AbstractDoToClosestObjectTask.java:178-183
```

The resulting semantic gap is:

```text
parent raw lookup says a container block exists
    -> parent selects OPEN_EXISTING_CONTAINER

child filtered lookup may reject that block or choose another block
    -> child may pursue a different target or wander

parent next tick again makes its branch decision from a fresh raw lookup
```

The log's `dungeonChestCacheSize=25` and `nonDungeonChestCacheSize=0` shortly
after the first open branch corroborate that the child predicate evaluated and
rejected dungeon-chest candidates. Existing logs still do not record a complete
same-tick chain proving that every filtered candidate was rejected or that this
was the only cause of the later loop.

### 3. Actual Storage Success Is Later

Branch selection is not storage success. The full success path requires:

```text
filtered child target selected
    -> target retained as the active pursuit
    -> matching Baritone goal/path calculated and adopted
    -> target reached
    -> container interaction attempted
    -> matching GUI/screen handler opened
    -> slot transfer performed
    -> ContainerStoredTracker observes the expected delta
```

The current Store progress event proves the parent branch and tracked inventory
state. It does not, by itself, prove every boundary in this chain.

## Most Likely Failure Area And Sub-Hypothesis

The strongest evidence supports a broader coordination boundary:

```text
StoreInAnyContainerTask existing-container distance/branch commitment
    -> chest acquisition and crafting-table child lifecycle
    -> Baritone ownership, target interaction, and storage side effect
```

The parent raw-candidate to child filtered-candidate handoff is a plausible
sub-boundary inside this area, not yet the uniquely proven first fault.

This hypothesis explains the observed shape without blaming the bridge:

```text
raw container exists
    -> parent repeatedly chooses open-existing

filtered child cannot commit to a usable matching target
    -> wander, reselect, or no useful interaction progress

distance or fresh scanner state changes
    -> parent chooses obtain-chest

chest acquisition routes through an existing crafting table
    -> later Store/Craft/Baritone work continues

raw container becomes branch-eligible again
    -> cycle repeats without tracked storage
```

This remains a hypothesis because the current bounded logs do not correlate the
parent local candidate, child filtered result, active pursuit, Baritone goal,
click, GUI transition, and slot delta under one transition identity.

The obtain-chest side also shows substantial real lifecycle activity:

```text
primary structured CONTAINER_TASK_TARGET_DECISION events: 2,492
distinct DoCraft task instances reaching onTick: 2,492
costToMakeNew=Infinity on every one of the 2,492 emissions
primary structured CONTAINER_TASK_BRANCH events: 2,491
branch result on every emitted branch event: return_open_table_task
```

A loose literal search for `event=CONTAINER_TASK_TARGET_DECISION` finds 2,654
lines because 162 `CRAFTING_TABLE_ROUTE_RETRY_SUMMARY` payloads repeat that
token. The analogous loose branch count is 2,572 because 81 retry-summary
payloads repeat `event=CONTAINER_TASK_BRANCH`. These nested references are not
additional primary events. The sole target-decision identity without an
emitted matching branch is task instance `18111`, immediately before
`CONTAINER_TASK_DIAGNOSTIC_CAP_REACHED` at line 26,772. Diagnostic exhaustion
is not proof that the runtime branch itself did not execute.

Baritone was not universally unable to calculate or adopt a path. Across the
emitted structured-diagnostic coverage, mixed targets produced:

```text
emitted goal-request decision records with requestAccepted=true: 78
BARITONE_PATHFINDER_CALCULATE_COMPLETED emitted records: 280
SUCCESS_TO_GOAL: 78
SUCCESS_SEGMENT: 77
CANCELLATION results: 125
BARITONE_PATH_ADOPTION_DECISION emitted records: 280
ADOPTED_AS_CURRENT: 155
NO_PATH_CANCELLATION adoption outcomes: 125
```

The 78 goal-request lines are emitted records, not the total accepted-request
count: 77 contain suppression counts, and part of the first suppression window
started before this deposit. The 280 emitted
`BARITONE_PATHFINDER_CALCULATE_COMPLETED` records cover generations 11-290 and
pair one-to-one with 280 emitted `BARITONE_PATH_ADOPTION_DECISION` records.
Every one of those 560 records has `summary=false` and `suppressedCount=0`.
The last matching pair is at lines 8,988-8,989 at 13:05:21; generation 291 has
only worker-started and scheduled records at lines 8,990-8,991. The aggregates
therefore describe the emitted structured interval, not complete coverage of
all actual Baritone activity during the full 50-minute run. They also mix
chest, crafting-table, and other goals and cannot identify the failing target
without connecting each target position to its `calculationGeneration`.

## Required Interpretation Corrections

### `closestContainerPresent=false`

Do not treat this field as proof that the raw scanner returned no container.

In the obtain-chest fallback, `StoreInAnyContainerTask.java:200-212` passes
`null`, `false`, and `false` to the diagnostic call after the branch condition
has already failed. The diagnostic formatter then calculates:

```text
closestContainerPresent = closestContainer != null
```

Therefore, `false` means the fallback diagnostic was given no candidate value.
The raw lookup may have been empty, or it may have returned a candidate that
failed both range conditions. The field cannot distinguish those cases.

### `branchChangeCount=255`

Do not assume from the field name alone that this is a pure branch-flip
counter.

`StoreInAnyContainerProgressDiagnostics.progressSignature()` first constructs
a raw key from:

```text
branch
toStore
notStored
storedCountByTarget
child task class
closest position
currentChestTry
requested item
container block item
missing target
```

When the raw key exceeds 360 characters, the implementation returns
`key.substring(0, 360) + "..."`. The stored and compared Java string is
therefore 363 characters long: only its first 360 characters contain variable
raw-key content, and the final ellipsis is fixed. The emitted signatures at
lines 4,292, 4,404, and 9,477 are each 363 characters and end in `...`.

In this 15-target run, the retained raw-key prefix ends in the middle of
`notStored`; `storedCountByTarget`, child, closest, `currentChestTry`, requested
item, container block item, and missing target do not participate in the
effective comparison key. The counter increments when the resulting
363-character signature changes. Its current name therefore overstates the
general metric.

For this specific run, independent enumeration of the 256 emitted
`STORE_IN_ANY_CONTAINER_PROGRESS_STATE` records found:

```text
return_open_existing_container: 128
return_obtain_chest_item: 128
adjacent emitted records: exact alternation
adjacent branch flips in the capped detail sequence: 255
```

Thus 255 also happens to equal the observed branch-flip count in this capped
sequence. It is not a count for the full 50-minute run because detailed
progress emission stopped at the session cap.

### `costToMakeNew=Infinity`

Do not classify `Infinity` as an exception or malformed numeric result.

`CraftInTableTask.getCostToMakeNew()` intentionally returns
`Double.POSITIVE_INFINITY` when a crafting table is within 40 blocks. This
policy can amplify a loop if the table is visible to the scanner but cannot be
reached, adopted, opened, or blacklisted effectively. The current evidence does
not prove that the same table and the same scanner snapshot owned every route
decision.

### Repeated `DoCraftInTableTask` Instances

Do not reduce the observed `DoCraftInTableTask` identities to allocation noise,
but do not call every identity a complete stop/restart cycle either.

`Task.tick()` calls `newSub.isEqual(sub)` before replacing an active child.
In this run, the deposit-to-cancel window contains 2,492 primary structured
`CONTAINER_TASK_TARGET_DECISION` events with 2,492 distinct
`DoCraftInTableTask` instance IDs. This count deliberately excludes nested
event-name references inside retry summaries. The event is emitted inside
`DoStuffInContainerTask.onTick()`, and `Task.tick()` calls `onStart()` before
the first `onTick()`. Therefore, at least 2,492 distinct DoCraft instances
actually started and reached `onTick()`.

This is strong evidence of real DoCraft-level churn. It still does not prove
2,492 completed stop/restart cycles, the exact parent reconciliation decision
for every instance, or that this later churn was the first cause of the Store
loop.

### Baritone Pathing NPE Episodes

Treat the six NPE episodes as separate path-calculation failures and possible
later amplifiers. They are not the verified first cause of this incident
because the Store loop preceded the first episode and continued after every
episode.

Only the first episode includes a stack identifying
`UserBlockRangeTracker.updateState()` and
`AltoClefSettings.shouldAvoidBreaking()`. Do not automatically assign the five
later stackless NPE messages to the same method, and do not infer an
asynchronous scanner race or exact null producer without a direct
producer-to-consumer trace.

### Explore Process And Baritone Cache

An active explore process can be either:

```text
result:
    child found no usable candidate and chose wandering/exploration

cause or amplifier:
    an existing process retained control or prevented later goal adoption
```

The current timeline does not distinguish these directions. Likewise, stale
Baritone world cache remains an operational suspect only if world copy,
restore, replacement, rename, or cache-reset comparison evidence exists. It is
not established as the cause of this incident.

## Confidence Classification

```text
CONFIRMED
    one local bare deposit invocation
    one persistent Store root identity
    no active Fabric command context for deposit
    no duplicate deposit submission
    open-existing and obtain-chest signatures both observed
    tracked storage remained zero through the last detailed checkpoint
    cobblestone notStored changed from 317 to 256 while the tracker remained 0/317
    no natural completion before explicit StopCommand
    3046.404 seconds elapsed before cancellation
    six pathing NPE episodes occurred after the loop began and did not terminate it
    only the first NPE stack identifies UserBlockRangeTracker
    at least 2,492 distinct DoCraft instances started and reached onTick
    the later Minecraft session shutdown was orderly and exited with code 0

LEADING SUSPECT AREA
    distance/branch commitment and child-lifecycle coordination is the leading suspect area

PLAUSIBLE SUB-HYPOTHESES OR CONDITIONAL AMPLIFIERS
    raw-parent to filtered-child candidate handoff
    dungeon-chest filtering as a contributor to lack of a usable child target
    crafting-table Infinity policy if the preferred table made no useful progress

UNRESOLVED
    exact raw closest value on every fallback transition
    exact filtered child result on the same tick
    exact predicate rejection reason for the final candidate set
    active pursuit identity and whether it changed
    ExploreProcess ownership before the first missing child target
    matching Baritone calculation generation and adoption outcome
    click attempt, GUI-open transition, and slot-delta timeline
    whether any tracked item moved after the diagnostic cap
    cause and destination of the observed 61-cobblestone notStored reduction
    exact origin of the null BlockPos consumed by UserBlockRangeTracker
```

## Next Read-Only Analysis

Before changing source, reconstruct bounded timelines around:

```text
first OPEN_EXISTING_CONTAINER -> OBTAIN_CHEST transition
first OBTAIN_CHEST -> OPEN_EXISTING_CONTAINER transition
first and subsequent Baritone pathing NPE episodes
final StopCommand cancellation
```

Use a window of approximately 100 ticks before and after each transition when
the existing logs contain enough data. Correlate:

```text
Store task identity
actual local raw candidate used by the parent
closestWithinRange
currentChestTry and currentTryWithinExtraRange
filtered child candidate
predicate acceptance or rejection reason
active pursuit before and after
child reconciliation decision
ExploreProcess owner and start tick
Baritone goal target
calculationGeneration
path result and adoption outcome
interaction target
click attempt
screen and screen-handler transition
slot transfer or inventory delta
```

If existing logs can answer a question, do not add source diagnostics for it.

## Future Diagnostics Gate

No diagnostics change is approved by this document.

If a later read-only analysis leaves the candidate handoff materially
unobservable, the smallest useful diagnostic would capture values already
computed by the behavior path:

```text
STORE_CONTAINER_PARENT_CANDIDATE_DECISION
    raw candidate local value
    branch-local range predicates
    currentChestTry identity/equality

STORE_CONTAINER_FILTERED_CANDIDATE_DECISION
    child search result
    accepted/rejected candidate
    rejection reason
    active pursuit before/after

STORE_CONTAINER_EFFECT_OBSERVATION
    interaction target
    GUI transition
    slot delta
    ContainerStoredTracker delta
```

Do not call `getNearestBlock()` or `validContainer.test()` a second time merely
to populate a log field. Both scanner snapshots and the predicate's dungeon
cache side effects can make a diagnostic re-query differ from the value that
actually controlled behavior.

Any later diagnostic must remain bounded and must not change:

```text
return values
branch conditions or ordering
Task selection, equality, completion, or replacement
retry, timeout, range, cost, blacklist, or fallback policy
ExploreProcess or Baritone ownership
input state
container click or slot behavior
exception handling
wire payloads or command lifecycle status
```

## Operational Conclusion

For this evidence set:

```text
Do not retry bare deposit while the previous Store task is still active.
Use the existing stop command to end the active task when it does not converge.
Do not report the stop event as successful deposit completion.
Do not report diagnostic-cap exhaustion as task termination.
Do not treat the later orderly process exit as deposit success.
Do not interpret the 61-item notStored reduction as a verified container deposit.
Do not modify WebSocket ownership or command-result payloads for this incident.
```

The stable working conclusion is:

```text
One bare deposit created one Store task.
The task did not prove a successful storage effect or natural terminal state.
It repeated for about 50 minutes 46 seconds and stopped only through
StopCommand cancellation.
The leading suspect area is existing-container distance/branch commitment
through child lifecycle and Baritone ownership.
The raw-parent to filtered-child container-candidate handoff remains a plausible
sub-hypothesis, not the proven earliest fault.
```
