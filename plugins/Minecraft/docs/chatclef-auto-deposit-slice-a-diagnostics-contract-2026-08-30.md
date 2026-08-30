<!-- 20260830_openai: Separated the first bounded automatic-deposit diagnostics slice into an evidence-gated identity, observation, lifecycle, and test contract without authorizing behavior changes. -->

# ChatClef Automatic Deposit Slice A Diagnostics Contract

Date: 2026-08-30
Scope: Fabric ChatClef 1.20.1 automatic deposit diagnostics only

## 1. 문서 상태와 권한

이 문서는 automatic deposit의 transfer, tracker, movement invalidation과 Task
reconciliation을 같은 operation ledger로 연결하기 위한 Slice A 진단 계약이다.
동작 수정안이나 구현 완료 기록이 아니다.

```text
DOCUMENT_DIRECTION: PASS_WITH_REQUIRED_CORRECTIONS
FINAL_ROOT_CAUSE: UNPROVEN
SLICE_A_PRIORITY: PASS
IMPLEMENTATION_STATUS: NOT_STARTED_BY_THIS_DOCUMENTATION_TASK
IMMEDIATE_BEHAVIOR_CHANGE: NOT_AUTHORIZED
STORE_HOME: OUT_OF_SCOPE
```

이 문서가 존재하는 사실은 Java 구현, test 통과, build 통과, artifact 동일성 또는
runtime path 증명이 아니다. 향후 결과는 다음 축을 분리해 보고한다.

```text
SOURCE_EQUIVALENCE
TEST_SOURCE_PRESENT
TEST_PASSED_CURRENT
BUILD_PASSED_CURRENT
RUNTIME_PATH_PROVEN_CURRENT
```

이번 문서 작업은 Markdown 문서와 상호 링크만 다룬다. Java, JSON, resource, test
source, build, JAR, Minecraft runtime, commit 또는 push를 건드리지 않는다.

## 2. 문서 관계와 증거 등급

Incident chronology와 현재 root-cause 판정은
[ChatClef Automatic Deposit Transfer / Movement / Carry On Diagnostics Review](chatclef-auto-deposit-transfer-movement-carryon-diagnostics-review-2026-08-30.md)가
소유한다. automatic pressure composition의 역사와 activation ledger는
[ChatClef @deposit_all Ocean Loop Diagnostics Plan](chatclef-deposit-all-ocean-loop-diagnostics-plan-2026-08-26.md)이
소유한다. facade extraction과 folderization 순서는
[ChatClef Diagnostics Refactoring Backlog](chatclef-diagnostics-refactoring-backlog-2026-08-29.md)이
소유한다.

이 문서의 증거 등급은 다음과 같다.

| 등급 | 의미 |
| --- | --- |
| `SOURCE_CONFIRMED` | 현재 production source에서 직접 확인한 mechanism이다. |
| `LOG_CONFIRMED` | 로컬 원본 로그 또는 그 원본에서 만든 exact-line evidence ledger로 직접 확인했다. |
| `REVIEW_REPORTED` | 독립 검토가 보고했지만 같은 원본 bytes를 이번 검토자가 직접 재검증하지 않았다. |
| `INFERENCE` | 확인된 사실들과 양립하지만 같은 causal identity로 직접 관찰하지 못했다. |
| `UNPROVEN` | 현재 증거로 원인, owner 또는 exact occurrence를 확정할 수 없다. |
| `UNAVAILABLE` | 현재 경계에서 값을 안전하게 관찰할 수 없어 만들지 않는다. |

독립 검토자는 저장소에 보존된 sanitized excerpt bundle의 raw bytes를 다시
해시하거나 provenance를 재검증하지 않았다. 독립 검토는 source mechanism과 문서
내부 일관성을 보강하지만 기존 `LOG_CONFIRMED` ledger를 대체하지 않는다.

값을 안전하게 관찰할 수 없으면 `false`, `0`, 빈 문자열, `minecraft:air` 또는
추측한 enum으로 꾸미지 않는다. `UNAVAILABLE`, `OBSERVATION_INCOMPLETE` 또는 명시적
coverage gap으로 남긴다.

## 3. Source-confirmed mechanism

### 3.1 Aggregate target과 physical source stack

`ItemTarget x10`은 특정 slot의 `x10` stack identity가 아니다. 현재
`MoveItemToSlotTask`는 matching inventory slot을 다시 고르고 선택한 physical stack
전체를 cursor로 집을 수 있다. 따라서 다음 두 값을 합치지 않는다.

```text
aggregate target: cobblestone x10
selected physical source: cobblestone x64
```

`x10 -> cursor x64`는 현재 source가 만들 수 있는 중간 상태이므로 그 사실만으로
오류를 증명하지 않는다. 빈 destination에 target 10을 하나의 연속 child가 정확히
채웠다면 기대 terminal cursor는 x54다. runtime에서 관찰된 x17까지의 경로는 별도
transfer ledger가 필요하다.

### 3.2 Exact-fit destination 재사용 거부

`StoreInContainerTask`는 다음 경계로 source stack 전체가 들어갈 destination을
고른다.

```text
getSlotThatCanFitInOpenContainer(stack, false)
```

현재 `InventorySubTracker.getSlotsThatCanFit(...)`의 non-empty stackable slot 조건은
다음과 같다.

```text
acceptPartial || roomLeft > item.getCount()
```

비교는 `>=`가 아니라 strict `>`다. 따라서 다음 fixture에서는 기존 destination이
exact fit인데도 거절될 수 있다.

```text
destination count = 1
source/cursor count = 63
roomLeft = 63
acceptPartial = false
```

empty slot은 별도 후보가 될 수 있고, destination이 바뀌면
`MoveItemToSlotTask.isEqual()`이 false가 되어 child replacement 후보가 될 수 있다.

```text
strict exact-fit mechanism: SOURCE_CONFIRMED
runtime destination churn in this incident: UNPROVEN
cursor 64 -> 17 caused by destination churn: UNPROVEN
TARGET_CONTAINER completion failure caused by this mechanism: UNPROVEN
```

Slice A는 selector 입력과 이미 계산된 선택 결과만 관찰한다. 이 comparator를
수정하거나 selector를 진단 목적으로 다시 실행하지 않는다.

### 3.3 Tracker role과 predicate

Production payload의 exact role 이름은 다음 두 개다.

```text
ROOT_ANY_CONTAINER
TARGET_CONTAINER
```

`ROOT`로 줄이거나 새로운 role 이름으로 바꾸지 않는다.

```text
ROOT_ANY_CONTAINER
  owner: DepositAllTask
  predicate: slot -> true
  effective acceptance: every non-player slot
  projection: parent storedCountByTarget / notStored

TARGET_CONTAINER
  owner: StoreInContainerTask
  acceptance:
    event-time lastBlockPosInteraction BlockPos == targetContainer
```

`TARGET_CONTAINER` predicate는 BlockPos equality다. exact handler identity, syncId 또는
server-side container binding을 증명하지 않는다.

두 tracker가 같은 mutation을 관찰했다고 주장하려면 다음을 분리해 보존한다.

```text
trackerIdentity
subscriptionGeneration
subscriptionActiveAtMutation
slotMutationId
predicateEvaluated
predicateResult
signedDelta
trackerTotalBefore
trackerTotalAfter
```

### 3.4 Local mutation과 durable effect

`SlotClickChangedEvent`는 local `ScreenHandler.internalOnSlotClick` 전후의 slot 변화를
publish한다. 이 event의 signed delta는 local mutation evidence이며 그 자체로
server-confirmed durable storage가 아니다.

```text
mutationObservationSource =
  LOCAL_INTERNAL_CLICK
  SERVER_SLOT_UPDATE
  POST_ACTION_STABLE
  UNAVAILABLE
```

같은 handler/syncId의 bounded stable observation 또는 server reconciliation을
관찰하지 못하면 다음처럼 남긴다.

```text
durableEffect=UNAVAILABLE
```

Local click mutation을 confirmed storage success로 승격하지 않는다.

### 3.5 `notStored`의 정확한 의미

`ContainerStoredTracker.getUnstoredItemTargetsYouCanStore(...)`는 단순한
`target - stored` 계산이 아니다.

```text
tracker가 target을 이미 충족
  -> target 제거

tracker 미충족 + available >= original target
  -> original target count 유지

tracker 미충족 + 0 < available < original target
  -> available count로 cap

available == 0
  -> target 제거
  -> stored total이 0이어도 root finish 가능
```

`available`에는 player inventory와 cursor가 포함되며 현재 conversion input이 더해질
수 있다.

```text
stored=1/10 + available>=10
  -> notStored x10 가능

stored=0/10 + available=9
  -> notStored x9 가능

stored=0/10 + available=0
  -> target이 notStored에서 사라질 수 있음
```

따라서 다음 표현은 금지한다.

```text
notStored x9/x8 == 저장하고 남은 수량
notStored 감소 == durable container progress
```

정확한 표현은 `currently-available-and-unstored request cap 감소`다.

### 3.6 `MOVEMENT_PROGRESS_FAILED`

Current production source에서 `MOVEMENT_PROGRESS_FAILED`는
`_progressChecker.check(mod)`가 false를 반환한 branch에서만 설정된다.

```text
direct local trigger:
  progress-check return false

immediate effect:
  selected target/store generation invalidation
```

현재 직접 증명되지 않은 값은 다음과 같다.

```text
DISTANCE 또는 MINING mode
정확한 baseline과 reset owner
elapsed와 retry/fail count
정확한 6초/0.1 조건이 false를 만든 과정
GUI transfer가 progress-check failure를 직접 만들었는지 여부
```

Candidate invalidation과 active route-child stop/replacement는 같은 사건이 아니다.
Route-child stop/replacement는 generic Task reconciliation에서 별도로 관찰한다.

## 4. Operation과 lifecycle identity

### 4.1 Gameplay hierarchy

```text
automatic pressure-chain owned run
└─ AutoDepositMaintenanceTask
   ├─ per-item DepositAllTask root #1
   │  ├─ selected candidate/store generation #1
   │  │  ├─ route child
   │  │  └─ transfer attempts
   │  ├─ selected candidate/store generation #2
   │  └─ per-item root terminal
   ├─ per-item DepositAllTask root #2
   └─ maintenance logical terminal

diagnostic coverage ledger: orthogonal to the gameplay hierarchy
```

다음 경계를 서로 합치지 않는다.

```text
slot action / slot mutation
transfer-attempt close
selected-candidate/store-generation invalidation
route-child reconciliation/close
per-item DepositAll root close
AutoDepositMaintenanceTask logical terminal
pressure-chain owned-run close
RUNNING -> WAIT_FOR_REARM transition
diagnostic coverage close
budget suppression summary
```

Candidate invalidation을 route-child close나 per-item root terminal로 기록하지 않는다.
Diagnostic coverage close를 maintenance terminal로 기록하지 않는다. Budget suppression
summary는 logging fact일 뿐 gameplay terminal이 아니다.

### 4.2 Required identity chain

```text
autoOperationEpoch
maintenanceGenerationId
autoChildOperationId
storeOperationId
selectedCandidateGenerationId
storeAttemptId
routeChildLifecycleId
transferAttemptId
slotActionId
slotMutationId
```

Movement checker invocation은 별도 identity다.

```text
progressCheckInvocationId
```

Identity 규칙:

- child identity는 parent identity와 함께 기록한다.
- 같은 mutation을 두 tracker가 관찰하면 같은 `slotMutationId`를 사용한다.
- 하나의 action에서 여러 mutation이 발생하면 같은 `slotActionId`와 서로 다른 ordered
  `slotMutationId`를 사용한다.
- candidate generation invalidation 후 새 candidate를 선택하면
  `selectedCandidateGenerationId`를 새로 만든다.
- candidate 교체만으로 maintenance 또는 pressure-chain identity를 초기화하지 않는다.
- suppression이나 coverage close가 gameplay identity를 변경하지 않는다.

## 5. Required observation fields

### 5.1 공통

```text
diagnosticsMode
gameTick
dimension
topLevelTask
activeParentTask
activeChildTask
autoOperationEpoch
maintenanceGenerationId
autoChildOperationId
storeOperationId
selectedCandidateGenerationId
storeAttemptId
routeChildLifecycleId
transferAttemptId
slotActionId
slotMutationId
observationComplete
missingBoundaries
```

### 5.2 Transfer selection / begin

```text
physicalSourceSlot
physicalSourceItemId
physicalSourceCount
aggregateTargetItemId
aggregateTargetCount
destinationPosition
handlerIdentity
syncId
handlerRevision
destinationSlot
destinationSelectionKind=
  STACKABLE_EXISTING | EMPTY_SLOT | OTHER
sourceCountBefore
destinationCountBefore
roomLeft
acceptPartial=false
cursorCountBefore
```

Aggregate target, source candidates와 selected physical stack을 독립적으로 보존한다.

### 5.3 Slot action / mutation

```text
clickActionType
clickButton
clickSequence
mutationObservationSource
slotInPlayerInventory
cursorBefore
cursorAfter
sourceBefore
sourceAfter
destinationBefore
destinationAfter
stableOrServerSnapshotAvailable
durableEffect
```

Tracker observation은 role별로 다음을 가진다.

```text
trackerRole
trackerIdentity
subscriptionGeneration
subscriptionActiveAtMutation
trackerTargetBinding
lastBlockPosInteractionAtEvent
predicateEvaluated
predicateResult
signedDelta
trackerTotalBefore
trackerTotalAfter
```

Parent projection 입력과 출력은 다음과 같다.

```text
rootStoredCountByTarget
playerInventoryAvailableCount
cursorAvailableCount
conversionInputAvailableCount
combinedAvailableCount
hasItem
notStoredInput
notStoredOutput
```

Predicate, available count와 `notStored` 결과는 production이 이미 계산한 값이나 그
계산 경계의 immutable snapshot을 관찰한다. diagnostics를 위해 predicate를 다시
호출하거나 다른 inventory scan으로 값을 재구성하지 않는다.

### 5.4 Movement / invalidation / reconciliation

```text
progressCheckInvocationId
progressCheckEvaluated
progressCheckOk
progressMode
progressBaseline
progressElapsed
progressResetProvenance
selectedTargetBefore
selectedTargetAfter
selectedCandidateGenerationBefore
selectedCandidateGenerationAfter
activeRouteChildBefore
activeMoveItemChildBefore
candidateNextChild
candidateNextDestination
subTasksEqual
canInterrupt
replacementApplied
previousChildStopCalled
activeChildAfter
nextBranch
```

`progressMode`, baseline, elapsed 또는 reset provenance가 기존 state/mutation point에서
관찰되지 않으면 `UNAVAILABLE`로 남긴다. Progress algorithm을 복제해서 provenance를
만들지 않는다.

### 5.5 Terminal과 handoff

```text
terminalScope
terminalReason
closingIdentity
parentIdentity
childIdentity
nextLifecycleState
ownedRunClosed
maintenanceLogicalTerminal
diagnosticCoverageClosed
suppressedEventCount
userTaskResumeObserved
userTaskNaturalCompletionObserved
```

각 scope를 별도로 표현하며 하나의 terminal event로 합쳐 원인을 꾸미지 않는다.

### 5.6 Carry On correlation

Slice A가 기존 side-effect-free observer에서 값을 안전하게 읽을 수 있을 때만 다음을
correlation field로 포함할 수 있다.

```text
carryOnLoaded
carryOnVersion
carryStateBefore
carryStateAfter
carryTarget
inputOwner
```

정확한 target, state 또는 input owner를 관찰하지 못하면 `UNAVAILABLE`로 둔다.
Carry On state 변경, retry, input release, timeout 또는 fallback을 추가하지 않는다.

## 6. Diagnostics-only 불변 조건과 stop gate

Slice A는 다음을 수행하지 않는다.

```text
progress checker 추가 check/reset/setProgress 호출
progress algorithm 재구현
slot-fit selector 재실행
추가 inventory scan
TARGET_CONTAINER predicate 재평가
EventBus publish/subscription order 변경
Task selection/order/completion 변경
diagnostics를 위한 Task close/interrupt/retry
timeout/cooldown 변경
container click 또는 cursor state 변경
Baritone goal/path/input 변경
Carry On interaction/state 변경
TaskRunner/UserTaskChain 변경
StoreHome behavior/lifecycle/timeout 변경
```

Diagnostic code는 이미 계산된 값 또는 기존 mutation point에서 immutable snapshot으로
확보한 값만 사용한다. 관찰을 위해 lifecycle ordering, synchronization, exception
propagation 또는 return value를 변경하지 않는다.

필요한 값이 public state나 한 개의 narrow mutation point에서 안전하게 관찰되지 않고,
여러 upstream lifecycle owner를 수정하거나 behavior를 바꿔야 한다면 구현을 중단하고
coverage gap을 보고한다.

## 7. Bounded logging 계약

```text
normal runtime: OFF
explicit investigation: BOUNDARY
VERBOSE: explicit bounded reproduction only
overlay/HUD: diagnostics mode와 독립
```

- unchanged tick/slot polling을 기록하지 않는다.
- operation begin/end, state transition, mutation, invalidation, reconciliation과 terminal
  boundary만 기록한다.
- dedupe fingerprint, per-operation budget, session hard cap과 suppression summary를 쓴다.
- suppression은 gameplay state, lifecycle 또는 identity에 영향을 주지 않는다.
- cap에 도달해도 terminal reason과 coverage gap은 bounded summary로 남긴다.
- OFF는 기존 lifecycle, warning, error 또는 crash log를 억제하지 않고 Slice A detail만
  complete no-op으로 만든다.
- BOUNDARY와 VERBOSE 모두 Task selection, return value, timing과 click count가 OFF와
  같아야 한다.

기존 generic slot logging의 일부는 VERBOSE-only다. BOUNDARY에서 자동으로 보일
것이라고 가정하지 않는다. 기존 event를 재사용하더라도 Slice A projection 자체가
canonical bounded logging policy를 만족해야 한다.

## 8. Direct characterization test contract

아래 16개 grouped scenario는 독립 검토의 24개 contract assertion을 빠짐없이 묶는다.
`16개`라는 이유로 assertion 범위를 축소하지 않는다.

1. Aggregate target와 physical source 분리 `[assertion 1]`

   `aggregate x10 + physical [x10,x64]`에서 target과 선택 source identity/count를 각각
   고정한다.

2. Partial physical transfer `[assertion 2]`

   `physical/cursor x64 + empty destination + target x10`에서 한 정상 연속 attempt의
   terminal cursor x54를 고정한다. Durable effect는 별도 판정한다.

3. Exact-fit comparator와 frozen-target churn `[assertions 3-4]`

   `destination x1 + source x63 + roomLeft 63 + acceptPartial=false`에서 현재 `>` 조건의
   거절과 selection kind를 고정한다. TARGET tracker가 advance하지 않는 fixture에서
   destination 변화, `MoveItemToSlotTask.isEqual()` 결과와 child replacement도 함께
   관찰한다.

4. One action, multiple mutations `[assertion 5]`

   하나의 `slotActionId` 아래 여러 ordered `slotMutationId`가 존재할 수 있음을 고정한다.

5. Same mutation과 durable reconciliation `[assertions 6-7]`

   `ROOT_ANY_CONTAINER`와 `TARGET_CONTAINER`가 같은 mutation을 관찰할 때 같은
   `slotMutationId`를 사용한다. Local click 뒤 server rollback과 post-action stable
   state를 구분하며, stable/server proof가 없으면 `durableEffect=UNAVAILABLE`이다.

6. Subscription generation `[assertion 8]`

   Tracker start/stop/re-subscribe 사이의 generation과
   `subscriptionActiveAtMutation`을 고정한다.

7. Signed delta와 predicate asymmetry `[assertions 9-10]`

   Positive, negative, replacement mutation을 두 tracker role에서 고정한다. 같은
   non-player mutation에서 `TARGET_CONTAINER=false`여도
   `ROOT_ANY_CONTAINER=true`일 수 있음을 함께 고정한다.

8. `notStored` semantics `[assertions 11-13]`

    Tracker satisfied 제거, available가 target 이상일 때 original count 유지,
    partial available cap과 zero available 제거를 source와 동일하게 고정한다.

9. Available-count composition `[assertion 14]`

    Player inventory, cursor와 conversion input이 combined available count에 반영되는
    경계를 고정한다.

10. Parent-before-child ordering `[assertion 15]`

    현재 production의 parent `onTick`과 child tick 순서를 바꾸지 않고 고정한다.

11. Check-false, reconciliation과 observation no-op `[assertions 16-18]`

    Progress-check false가 selected-candidate invalidation을 만들고 실제 child
    stop/replacement는 별도 reconciliation 결과임을 고정한다. Diagnostics가
    check/reset/setProgress를 추가 호출하지 않으며, 읽을 수 없는 baseline/reset
    provenance를 알고리즘 재실행 없이 coverage gap으로 남기는 것도 고정한다.

12. HEAD와 RETURN/lookup verdict 분리 `[assertion 19]`

    HEAD capture 당시 판정과 이후 binding expiry, route change 또는 target mismatch를
    서로 다른 시점의 typed verdict로 고정한다.

13. Carry On temporal attribution 제한 `[assertion 20]`

    `NOT_CARRYING -> CARRYING` edge가 있어도 target이 retained이고 carried identity가
    unavailable이면 exact target attribution이 아님을 고정한다.

14. Terminal scope 분리 `[assertion 21]`

    Transfer, route child, per-item root, maintenance logical terminal,
    pressure-chain owned-run close, `RUNNING -> WAIT_FOR_REARM`과 diagnostic coverage의
    exact counts를 각각 고정한다.

15. Diagnostics modes와 boundedness `[assertion 22]`

    OFF/BOUNDARY/VERBOSE, dedupe, rate, payload, session cap과 suppression summary를
    고정한다.

16. Shared observer no-op과 entry-path 불변 `[assertions 23-24]`

    Automatic context가 없을 때 shared observer가 complete no-op이며 manual
    `@deposit_all`, `@deposit`, `@store_home` attribution이 바뀌지 않음을 고정한다.

위 grouped scenario가 덮는 최소 24개 assertion은 다음과 같다.

```text
1  aggregate x10 + physical [10,64] selection
2  x64 -> target10 terminal cursor x54
3  strict exact-fit rejection and empty-slot selection
4  frozen target destination churn / MoveItem replacement
5  one action -> ordered multiple mutations
6  same mutation -> both tracker roles
7  local mutation -> rollback/stable distinction
8  tracker subscription generation and active state
9  signed delta positive/negative/replacement
10 TARGET false while ROOT_ANY_CONTAINER accepts
11 stored=1/10 + available>=10 -> notStored x10
12 stored=0/10 + available=9 -> notStored x9
13 stored=0/10 + available=0 -> target removal/root finish possible
14 cursor and conversion-input available composition
15 parent tick before child tick
16 check=false invalidation separate from child close
17 no extra check/reset/setProgress
18 unavailable progress provenance remains a coverage gap
19 HEAD capture verdict separate from RETURN/lookup verdict
20 temporal Carry On edge + retained target + unavailable identity is not exact attribution
21 lifecycle scopes have separate exact counts
22 OFF/BOUNDARY/VERBOSE, dedupe, rate, payload and session cap
23 automatic context absent -> shared observers complete no-op
24 manual @deposit_all/@deposit/@store_home attribution unchanged
```

기존 exact-set, ordering 또는 identity assertion을 약화하거나 삭제하지 않는다. Test
deselection, retry 또는 flaky 허용으로 통과시키지 않는다.

## 9. Responsibility와 package 경계

책임이 둘 이상이면 LAVI-owned collaborator로 분리하되, source task 승인 전에는
아래 이름을 생성 지시로 해석하지 않는다.

```text
lavi/minecraft/diagnostics/container/store/deposit/
  transfer/
    physical source, destination selection, slot action/mutation, tracker observation

  route/
    candidate generation, movement result, child reconciliation

  terminal/
    per-item root, maintenance, pressure-owned-run and coverage projections
```

기존 `StoreDepositDiagnostics` public static facade와 upstream-facing call sites는
유지한다. `StoreContainerRouteState`는 여러 mutable owner로 쪼개지 않고 하나의
synchronized aggregate owner를 유지한다. Immutable snapshot, pure projection과
bounded formatter만 적절한 LAVI-owned package로 추출한다.

한 번에 package tree 전체를 만들지 않는다. Characterization coverage가 있는 event
family 하나씩 facade 뒤로 추출한다.

## 10. Bounded runtime proof gate

향후 별도 승인 아래 Slice A를 구현·검증한다면 한 reproduction에서 다음 causal
ledger를 연결해야 한다.

```text
pressure-chain owned run
-> maintenance generation
-> per-item DepositAll root
-> selected candidate/store generation
-> route child
-> transfer attempt
-> slot action
-> slot mutation
-> both tracker observations
-> local/stable/server effect classification
-> movement check result
-> selected-candidate invalidation
-> Task reconciliation
-> per-item root close
-> maintenance logical terminal
-> owned-run close / WAIT_FOR_REARM
-> user-task resume and terminal

diagnostic coverage close: recorded independently
```

시간상 인접하다는 이유만으로 공통 identity가 없는 사건을 한 원인 사슬로 합치지
않는다.

현재 runtime evidence는 다음처럼 분리한다.

```text
later user-task natural completion and terminal:
  LOG_CONFIRMED

automatic interval ended and user-task resume observation:
  LOG_CONFIRMED
  exact unified causal identity ledger remains incomplete

explicit final occupiedCount=28 field:
  UNAVAILABLE

five whole-stack transfer arithmetic consistent with 28/36:
  strongly supported inference

complete 33 -> 28 causal ledger with exact handoff identity:
  UNPROVEN
```

Transfer arithmetic이 28과 일치해도 explicit final occupied-count observation으로
승격하지 않는다.

Runtime proof 완료 조건:

- 두 tracker의 subscription과 동일 mutation 관찰이 identity로 연결된다.
- local mutation과 durable/stable/server effect가 구분된다.
- exact-fit selector 입력과 이미 계산된 결과가 추가 selector 호출 없이 관찰된다.
- movement check의 이미 계산된 결과와 뒤이은 reconciliation이 분리된다.
- maintenance terminal과 pressure-chain close가 분리된다.
- suppression이 있어도 terminal/coverage 상태가 bounded summary로 남는다.
- diagnostics OFF와 BOUNDARY에서 gameplay 결과와 call ordering이 동일하다.

## 11. Root-cause와 behavior gate

현재 확정할 수 있는 causal fact:

```text
aggregate x10은 physical x64를 선택할 수 있음
cursor x64는 current evidence ledger에 존재함
해당 구간에서 ROOT_ANY_CONTAINER summary는 stored 0/10으로 유지됨
notStored는 stored remainder가 아니라 available-count cap을 포함함
MOVEMENT_PROGRESS_FAILED는 check-return-false 전용 invalidation reason임
해당 invalidation 뒤 acquisition branch 재진입이 evidence ledger에 존재함
strict exact-fit comparator mechanism이 production source에 존재함
```

아직 확정할 수 없는 최종 원인:

```text
ROOT_ANY_CONTAINER가 accepted durable delta를 누적하지 못한 이유
각 action에서 destination slot이 실제로 변경됐는지
TARGET_CONTAINER가 cumulative 10에서 종료하지 않은 이유
어떤 mutation이 server-confirmed인지
정확한 movement mode/baseline/elapsed/reset owner
GUI transfer가 checker false를 직접 유발했는지
정확한 Carry On target/input owner
완전한 33 -> 28 maintenance/user-task handoff identity
```

```text
FINAL_ROOT_CAUSE=UNPROVEN
```

Slice A가 first failing boundary와 owner를 증명하기 전에는 comparator 수정, retry,
timeout, blacklist, cancellation, input release, path cancellation 또는 fallback을
추가하지 않는다.

## 12. Non-goals와 실행 승인 경계

이번 Slice A에 포함하지 않는다.

```text
@store_home 및 trusted-home storage
StoreHomeTask/lifecycle/controller/view
StoreHome timeout 수치와 decision order
StoreHome manifest, typed outcome 또는 projector
manual @deposit / @deposit_all 정책 변경
automatic deposit item-priority 정책 변경
exact-fit comparator 수정
StoreInContainerTask 성공 조건 변경
InteractWithBlockTask 변경
TaskRunner/UserTaskChain 변경
Baritone goal/path/input 변경
PlayerInteractionFixChain 변경
Carry On behavior fix
새 retry/replan/fallback
wire protocol 또는 artifact schema 변경
public Korean/parser/admission/bridge readiness 변경
```

`@store_home`과 automatic deposit은 서로 다른 operation/lifecycle owner로 유지한다.
Slice A operation identity나 mutable tracker state를 StoreHome에 공유하지 않는다.

이 문서의 작성 또는 review `PASS`는 다음 행동을 승인하지 않는다.

```text
Java/JSON/resource/test source 수정
test/build 실행
Minecraft runtime reproduction
JAR 복사 또는 배포
commit/push
behavior fix
```

각 단계는 별도 범위와 실제 evidence로 승인받아야 한다.
