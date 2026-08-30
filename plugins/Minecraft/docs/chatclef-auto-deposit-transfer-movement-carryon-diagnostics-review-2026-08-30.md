<!-- 20260830_openai: Reconciled the automatic-deposit transfer, movement-progress, and Carry On incident against the current source and sanitized runtime evidence without authorizing a behavior change. -->
<!-- 20260830_openai: Applied the independent evidence review corrections for tracker roles, notStored semantics, movement invalidation versus child reconciliation, exact-fit destination selection, and Slice A ownership. -->
<!-- 20260830_openai: Added the 23:26 KST follow-up evidence that strongly attributes the chest removal observed after the placement route and the OBTAIN_CHEST transition to one Carry On pickup interaction. -->
<!-- 20260831_openai: Hardened rotated-log provenance, historical incident scope, evidence grades, and the pre-patch ownership gate after a read-only cross-document audit. -->

# ChatClef Automatic Deposit Transfer / Movement / Carry On Diagnostics Review

Date: 2026-08-30
Last reviewed: 2026-08-31

## 권한과 범위

이 문서는 2026-08-30 automatic deposit / Carry On 재현 묶음과 독립 검토를
현재 저장소 문맥에 대조한 documentation-only 기록이다. 사용자의 이번
"문서화 작업 진행" 요청은 이 Markdown 문서와 필요한 상호 링크의
생성·수정만 승인한다.

이 문서는 다음 작업을 승인하지 않는다.

```text
Java, JSON, resource 또는 test source 수정
diagnostics 구현
gameplay, Task, timeout, retry, click 또는 Carry On behavior 변경
dependency, version, wire protocol, diagnostics default 또는 HUD 변경
test 또는 Gradle build 실행
JAR 복사 또는 배포
Minecraft 실행 또는 재현
commit, push, reset, clean, stash 또는 checkout
```

아래 event, file, method, test 및 reproduction 항목은 향후 증거 수집 후보와
종료 조건이다. 구현 승인이나 완료 판정이 아니다. `AGENTS.md`의 standing
diagnostics-only authorization은 향후 실제 investigation/debug source task가
그 조건을 충족할 때 적용할 수 있지만, 현재의 명시적인 documentation-only
요청을 source edit 승인으로 확대하지 않는다.

```text
INCIDENT_SCOPE: COBBLESTONE_TRANSFER_MOVEMENT_CAPTURE
REVIEW_DIRECTION: PASS - bounded diagnostics-only next slice
FINAL_ROOT_CAUSE: UNPROVEN
IMMEDIATE_BEHAVIOR_CHANGE: NOT AUTHORIZED
IMPLEMENTATION_STATUS: NOT STARTED BY THIS DOCUMENTATION TASK
```

> 위 상태 블록은 이 문서가 처음 검토한 cobblestone transfer/movement incident의
> 역사적 판정이다. 같은 날 23:26 KST에 시작된 별도 `get diamond 1` 후속 재현의
> 최신 판정은 문서 끝의 **2026-08-30 23:26 KST 후속 재현** section이 소유한다.
> 두 재현의 root-cause 상태를 하나로 합치지 않는다. 앞 section의
> `SOURCE_CONFIRMED`도 그 earlier review 당시 dirty-working-tree snapshot 판정이며,
> later diagnostics implementation 뒤 현재 source나 deployed JAR equivalence로
> 확대하지 않는다.

## 문서 소유권

이 문서는 2026-08-30 incident evidence reconciliation과 다음 bounded
diagnostics-only observation contract만 소유한다.

- automatic inventory-pressure composition의 역사, activation, policy 및
  lifecycle 기준은
  [ChatClef @deposit_all Ocean Loop Diagnostics Plan](chatclef-deposit-all-ocean-loop-diagnostics-plan-2026-08-26.md)의
  section 28이 소유한다.
- ChatClef engine, Carry On optional boundary, Task와 input/path ownership,
  diagnostics-before-behavior 규칙은
  [ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)이
  소유한다.
- `StoreDepositDiagnostics`의 구조적 extraction과 no-growth 순서는
  [ChatClef Diagnostics Refactoring Backlog](chatclef-diagnostics-refactoring-backlog-2026-08-29.md)이
  소유한다.
- 첫 diagnostics-only 구현 단위의 identity, payload, lifecycle, boundedness 및
  characterization test 계약은
  [ChatClef Automatic Deposit Slice A Diagnostics Contract](chatclef-auto-deposit-slice-a-diagnostics-contract-2026-08-30.md)가
  소유한다.
- manual trusted `@store_home`의 planner, exact binding, transfer, timeout 및
  typed result 계약은
  [ChatClef Manual Trusted Home Storage Direction](chatclef-manual-trusted-home-storage-direction-2026-08-27.md)이
  소유한다.

이 문서는 위 문서들의 기존 판정이나 날짜별 evidence ledger를 소급해
덮어쓰지 않는다.

## 검토 기준선과 증거 출처

읽기 전용 검토 당시 저장소 기준선은 다음과 같다.

```text
repository: C:\Vtuber_Souorce_Code\LAVI
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: 14ba9b443f0bc11d6860a25d7fd3b8b916d95a04
worktree: DIRTY - existing user/source/document changes preserved
source inspection: YES
sanitized exact-line log inspection: YES
test execution for this review: NO
build execution for this review: NO
Minecraft reproduction for this review: NO
commit or push: NO
```

재현 묶음은 다음 로컬 파일로 보존돼 있다. 전체 `stdout-logs.txt`는 개인정보와
관계없는 대량 화면 로그를 포함하므로 ZIP에 넣지 않았고, 원본 line number를
보존한 sanitized exact-line excerpt와 원본 SHA-256 inventory를 사용한다.

```text
bundle:
  logs/chatgpt_auto_deposit_carryon_20260830_002411.zip

bundle SHA-256:
  B365D5D017999BB77EBB47CAEE7F4732D634379C79F1BE8AB2D5E203C4E44FEF

question and attachment guide:
  logs/chatgpt_auto_deposit_carryon_20260830_002411/CHATGPT_QUESTION_KO.md
  logs/chatgpt_auto_deposit_carryon_20260830_002411/README_ATTACHMENTS.md

source inventory:
  logs/chatgpt_auto_deposit_carryon_20260830_002411/source_inventory.tsv

primary transfer/movement excerpt:
  logs/chatgpt_auto_deposit_carryon_20260830_002411/excerpts/
    07_cobblestone_cursor_and_movement_failure.log
```

파일 수준에서 build output JAR과 active CurseForge instance의 JAR은 같은
크기와 SHA-256으로 기록됐다.

```text
size: 7,145,129 bytes
SHA-256: 573F067BA9E0E0ABE794FA49247AE5A34450B6B49893737913A12C7C99EB0498
classification: FILE_ARTIFACT_MATCH_CONFIRMED_FOR_CAPTURE
```

이 동일성은 이번 문서 작업에서 clean forced build를 실행했다는 뜻이 아니며,
현재 dirty source가 그 JAR과 일치한다는 뜻도 아니다.

독립 검토자는 이 저장소에 보존된 sanitized excerpt bundle의 raw bytes와
`source_inventory.tsv`를 직접 받은 것이 아니라 질문서와 검토용 문서만 받은
상태라고 명시했다. 따라서 독립 검토 자체는 source mechanism과 문서 내부
일관성을 재검수한 증거이며, raw-line provenance를 다시 해시한 증거가 아니다.
독립 검토 자체의 runtime chronology는 `REVIEW_REPORTED`로 유지한다. 이 문서의
`LOG_CONFIRMED` 표시는 Codex가 로컬 exact-line excerpt와 전체 로그를 직접 읽은
별도 evidence ledger다. 어느 한계를 다른 증거 축에 전이하지 않는다.

## 판정 언어

이 문서는 source/build/runtime provenance label과 인과 판정을 분리한다.

| 판정 | 의미 |
| --- | --- |
| `SOURCE_CONFIRMED` | 현재 worktree의 해당 코드 경로와 순서를 직접 확인했다. |
| `LOG_CONFIRMED` | 해당 incident가 명명한 sanitized exact-line excerpt 또는 hash와 cut-off가 기록된 archive prefix의 raw line에서 상태나 전이를 직접 확인했다. 한때 읽은 live path가 이후 보존되지 않은 사실만으로는 이 판정에 충분하지 않다. |
| `RUNTIME_LOG_CONFIRMED` | Shared incident status block에서 사용하는 `LOG_CONFIRMED`의 cross-document alias이며 증거 요건은 같다. |
| `REVIEW_REPORTED` | 질문서 또는 독립 검토가 보고했지만 첨부 exact-line excerpt만으로 완결된 경계를 직접 증명하지 못했다. |
| `INFERENCE` | source mechanism과 runtime chronology가 양립하지만 동일 operation에서 직접 상관되지 않았다. |
| `STRONG_TEMPORAL_ATTRIBUTION` | 같은 interaction의 carry-state edge, clicked-target 제거, GUI 미개방과 즉시 이어진 route 변화가 pickup attribution을 강하게 지지하지만 exact carried-block API identity 또는 activation owner는 확인되지 않았다. `TARGET_VERIFIED`와 같지 않다. |
| `UNPROVEN` | 현재 증거로 원인, owner 또는 정확한 횟수를 결정할 수 없다. |
| `UNAVAILABLE` | 해당 값을 관찰한 필드나 안전한 read-only 경계가 없어 값을 만들지 않는다. |
| `OUT_OF_SCOPE` | 해당 incident가 그 entry path나 Task를 실행한 증거를 포함하지 않는다. |

`REVIEW_DIRECTION=PASS`는 `IMPLEMENTATION_AUTHORIZATION`이 아니다.

## Earlier cobblestone incident 판정 요약

| 질문 | 판정 | 근거와 제한 |
| --- | --- | --- |
| Automatic pressure chain이 이 재현에서 실제 실행됐는가 | `LOG_CONFIRMED` | 33/36에서 automatic maintenance가 시작됐고 기존 user command context가 함께 기록됐다. |
| target `cobblestone x10`인데 cursor가 `x64`가 될 수 있는가 | `SOURCE_CONFIRMED` + `LOG_CONFIRMED` | `ItemTarget x10`은 aggregate 목표이고 현재 source-slot 선택은 matching physical stack 전체를 집는다. |
| `x10 -> cursor x64` 자체가 오류의 증거인가 | `SOURCE_CONFIRMED` | 현재 executor가 만들 수 있는 정상 중간 상태이므로 그 상태만으로 오류를 증명하지 않는다. 단, 그 뒤 한 attempt의 종료 cursor는 별도로 검증해야 한다. |
| GUI transfer 중 parent movement check가 평가될 수 있는가 | `SOURCE_CONFIRMED` | parent `onTick()`이 child tick보다 먼저 실행되고 selected target이 있으면 checker가 평가된다. |
| 실제로 `MOVEMENT_PROGRESS_FAILED`가 두 번 발생했는가 | `LOG_CONFIRMED` | tick 52877 및 53358에서 candidate/store generation이 무효화되고 acquisition branch로 돌아갔다. |
| 위 두 invalidation에서 `_progressChecker.check(mod)`가 false였는가 | `SOURCE_CONFIRMED` + `LOG_CONFIRMED` | 현재 source에서 `MOVEMENT_PROGRESS_FAILED`는 해당 check가 false일 때만 설정된다. |
| 6초 distance-progress window가 두 false 결과의 정확한 내부 원인인가 | `INFERENCE` | DISTANCE/MINING mode, 동일 transfer attempt, elapsed, baseline/reset provenance를 한 identity로 묶는 로그가 부족하다. |
| `stored=0/10`이 유지된 정확한 원인은 무엇인가 | `UNPROVEN` | slot action, accepted slot delta, cursor, destination, tracker state의 하나의 attempt ledger가 없다. |
| `notStored x9, x8`이 `10 - stored` 형태의 잔량인가 | `SOURCE_CONFIRMED` | 아니다. tracker 미충족 상태에서 player inventory/cursor와 conversion input을 포함한 현재 available count가 original target보다 작을 때 request를 cap한 결과일 수 있다. |
| Carry On이 정확히 어느 상자를 집었는가 | `UNPROVEN` | carry edge와 strong temporal attribution은 있으나 carried target identity가 unavailable인 구간이 있다. |
| 상자를 정확히 세 번 craft했는가 | `UNPROVEN` | 반복 acquisition/place route는 보이지만 inventory snapshot만으로 craft transaction 횟수를 단정할 수 없다. |
| automatic maintenance 종료 뒤 원래 user Task가 재개·자연 종료했는가 | `LOG_CONFIRMED` | 전체 로그 감사에서 automatic 구간 종료, user Task 재개, tick 54644 natural completion 및 다음 terminal 전송을 확인했다. 다만 이 전이를 하나의 causal handoff identity로 묶은 ledger는 없다. |
| 최종 occupied count가 명시적으로 28로 기록됐는가 | `UNAVAILABLE` | 다섯 whole-stack 전송 산술은 28/36을 강하게 지지하지만 final occupied-count 전용 필드는 없다. |
| 전체 `33 -> 28` relief와 exact resume handoff가 같은 identity로 증명됐는가 | `REVIEW_REPORTED` + `UNPROVEN` | 결과 보고와 개별 후속 경계는 있으나 operation부터 handoff까지 이어지는 단일 인과 ledger는 없다. |
| `@store_home`이 이번 incident의 원인 또는 실행 주체인가 | `OUT_OF_SCOPE` | 이 재현 excerpt에는 `StoreHomeTask`가 없고 automatic maintenance / DepositAll 경로만 있다. StoreHome behavior는 변경·재분류하지 않는다. |

Earlier cobblestone incident의 당시 최종 판정은 다음과 같다.

```text
INCIDENT_SCOPE: COBBLESTONE_TRANSFER_MOVEMENT_CAPTURE
FINAL ROOT CAUSE: UNPROVEN
DIAGNOSTICS-ONLY NEXT SLICE: PASS
IMMEDIATE GAMEPLAY FIX: FAIL / NOT AUTHORIZED
STORE_HOME IMPACT CLAIM: OUT_OF_SCOPE / NOT ESTABLISHED
```

## Source-confirmed mechanism 1: aggregate target과 physical stack

관련 source:

```text
adris/altoclef/tasks/slot/MoveItemToSlotTask.java
```

현재 구현에서 `ItemTarget x10`은 특정 inventory slot의 `10개 stack`을
가리키지 않는다. cursor가 비어 있으면 matching inventory slots에서 source를
다시 고르고, 고른 physical stack 전체를 `PICKUP`한다. 현재 비교식은 목표 이상인
후보끼리도 count가 큰 후보로 교체될 수 있으므로 `[10,64]`에서 `x64`가 선택될
수 있다. 이 결과는 주석의 "smallest count over target" 설명과도 일치하지 않지만,
이번 diagnostics 단계에서 selector behavior를 수정하지 않는다.

cursor count와 destination의 현재 count 합이 target을 넘으면 destination에
right-click으로 한 개씩 놓는다. destination이 정확히 10에 도달하면 child의
`isFinished()`가 true가 된다. 따라서 빈 destination에 한 번의 연속 attempt로
`64`를 집고 정확히 `10`을 놓았다면 cursor `54`가 기대된다.

재현에서는 첫 후보에서 cursor가 `64 -> 17`까지 감소한 뒤에도
`notStored=[[cobblestone] x10]`, `stored=0/10`이 유지됐다. `x64` 시작 자체는
설명되지만 `x17`까지의 진행과 parent progress field가 `stored=0/10`에 머문
내부 원인은 설명되지 않는다. 이 둘을 같은 원인으로 합치지 않는다.

### Source-confirmed secondary mechanism: exact-fit destination 재사용 거부

`StoreInContainerTask`는 source stack 전체가 들어갈 destination을
`getSlotThatCanFitInOpenContainer(stack, false)`로 다시 고른다. 현재
`InventorySubTracker.getSlotsThatCanFit(...)`는 이미 일부가 든 stackable slot을
`roomLeft > sourceStackCount`일 때만 승인하고 `>=`는 승인하지 않는다. empty slot은
별도 경로로 후보에 들어간다.

따라서 첫 right-click 뒤 `destination=1`, `cursor=63`이면 기존 destination의
`roomLeft=63`은 exact fit인데도 탈락하고 다른 empty slot이 선택될 수 있다. 새
destination은 `MoveItemToSlotTask.isEqual()`에서도 다른 child로 취급될 수 있다.

```text
source mechanism: SOURCE_CONFIRMED
runtime destination churn in this incident: UNPROVEN
TARGET_CONTAINER completion failure caused by this mechanism: UNPROVEN
```

이 mechanism은 cursor가 여러 destination에 한 개씩 놓이며 `64 -> 17`처럼 오래
감소한 관찰과 양립하지만, 아직 runtime 원인으로 승격하지 않는다. Slice A는
destination selection kind, source count, roomLeft와 child replacement를 같은
chronology에 묶는다. diagnostics 단계에서 `>` 비교를 `>=`로 고치지 않는다.

또한 이 executor의 `[10,64]` physical-source 선택은 section 28에 남아 있는
planner `[64,32]` break/deferred defect와 다른 mechanism이며 별도 change unit이다.

## Source-confirmed mechanism 2: stored tracker는 accepted signed delta를 누적

관련 source:

```text
adris/altoclef/tasks/container/DepositAllTask.java
adris/altoclef/tasks/container/ContainerStoredTracker.java
adris/altoclef/tasks/container/StoreInContainerTask.java
```

`ContainerStoredTracker`는 cursor 수량을 직접 성공 수량으로 사용하지 않는다.
player inventory가 아닌 slot에 대해 acceptance predicate를 평가하고, 승인된 slot의
before/after change를 누적한다.

```text
same item:
  after.count - before.count

item replacement:
  -before.count
  +after.count
```

그러므로 정확한 표현은 `positive delta only`가 아니라 `accepted signed
container-slot delta`다. 관찰된 deposit 성공 수량을 증가시키는 것은 양의 accepted
delta이지만, tracker 자체는 음의 변화와 replacement도 기록한다.

이 경로에는 역할이 다른 tracker가 동시에 존재한다.

```text
ROOT_ANY_CONTAINER tracker:
  owner = DepositAllTask
  emitted trackerRole = ROOT_ANY_CONTAINER
  acceptance = every non-player slot; predicate is slot -> true
  projection = parent storedCountByTarget and notStored

TARGET_CONTAINER tracker:
  owner = StoreInContainerTask
  emitted trackerRole = TARGET_CONTAINER
  acceptance = event-time lastBlockPosInteraction BlockPos equals targetContainer
  projection = child target-container progress and completion
  limitation = BlockPos equality is not exact handler/syncId identity proof
```

두 tracker의 identity, binding과 누적값은 서로 대체할 수 없다. target-container
predicate가 false였다는 가설만으로 모든 non-player slot을 받는
`ROOT_ANY_CONTAINER` tracker의 `stored=0/10`을 설명할 수 없다. 각 tracker가 해당
`SlotClickChangedEvent` 시점에 실제 subscription-active였는지도 별도 증거다.

현재 로그는 cursor 변화와 `stored=0/10`을 보여 주지만, 동일
`transferAttemptId` 아래에서 다음 항목을 완전하게 결합하지 못한다.

```text
physical source slot and stack
destination handler, syncId and slot
click action and button
cursor before and after
destination stack before and after
slotActionId and slotMutationId shared by both tracker observations
slotInPlayerInventory classification
trackerRole=ROOT_ANY_CONTAINER or TARGET_CONTAINER
tracker identity, subscription generation/active state and target binding
event-time lastBlockPosInteraction input for TARGET_CONTAINER
accept predicate result for that tracker
accepted signed delta for that tracker
each tracker total before and after
ROOT_ANY_CONTAINER storedCountByTarget
player inventory, cursor, conversion-input and combined available counts
hasItem result and notStored input/output
```

따라서 `ROOT_ANY_CONTAINER`/`TARGET_CONTAINER` tracker binding, target predicate, slot mapping,
event timing 또는 child 재시작 중 어느 것이 원인인지 아직 결정하지 않는다.

현재 `SlotClickChangedEvent`는 local `ScreenHandler.internalOnSlotClick` 전후의 slot
변화를 publish한다. 이 signed delta는 local click mutation의 증거이며 server
reconciliation까지 살아남은 durable effect를 자체적으로 증명하지 않는다. 가능한
경우 같은 handler/syncId의 bounded stable observation 또는 server slot update와
연결한다. 그렇지 못하면 다음처럼 사실을 꾸미지 않는다.

```text
mutationObservationSource=
  LOCAL_INTERNAL_CLICK | SERVER_SLOT_UPDATE | POST_ACTION_STABLE | UNAVAILABLE
durableEffect=UNAVAILABLE
```

### `notStored` 수량의 정확한 source 의미

`ContainerStoredTracker.getUnstoredItemTargetsYouCanStore(...)`는 일반적인
`targetCount - storedCount`를 계산하지 않는다.

```text
tracker가 target을 이미 충족
  -> 대상 제외

tracker가 target을 아직 충족하지 못하고 available >= original target
  -> original target count 유지

tracker가 target을 아직 충족하지 못하고 0 < available < original target
  -> available count로 cap

available == 0
  -> 대상 제외; ROOT_ANY_CONTAINER stored total이 0이어도 root finish 가능
```

여기서 available은 player inventory와 cursor를 포함하며, 현재 crafting/furnace
conversion input이 별도로 더해질 수 있다. 따라서 `stored=1/10`인데 available이
10 이상이면 `notStored x10`이 유지될 수 있고, `stored=0/10`인데 available이 9이면
`notStored x9`이 될 수 있다. tick 53744 이후의 `x9`, `x8`은 durable storage
progress가 아니라 그 tick의 `currently-available-and-unstored request cap` 감소만
직접 증명한다.

## Source-confirmed mechanism 3: parent movement checker와 candidate invalidation

관련 source:

```text
adris/altoclef/tasks/container/DepositAllTask.java
adris/altoclef/tasksystem/Task.java
adris/altoclef/util/progresscheck/MovementProgressChecker.java
```

generic Task lifecycle은 parent `onTick()` 결과를 먼저 계산한 뒤 child를 tick한다.
`DepositAllTask.onTick()`은 selected target이 유효한 동안
`_progressChecker.check(mod)`를 평가한다. 현재 source에서
`MOVEMENT_PROGRESS_FAILED`는 이 호출이 false를 반환한 branch에서만 설정된다.
따라서 tick 52877과 53358의 local trigger가 `check=false`였다는 사실은
`SOURCE_CONFIRMED + LOG_CONFIRMED`다.

기본 distance mode에는 6초와 0.1 block 조건이 있고 eating/mining에는 별도
reset/진행 경계가 있지만, 두 runtime invocation이 DISTANCE였는지 MINING이었는지,
정확한 baseline·elapsed·fail count·reset owner가 무엇이었는지는 현재 로그에 없다.
GUI transfer가 false 결과를 직접 만들었다는 인과도 아직 `INFERENCE`다.

checker가 false를 반환하면 현재 source는 다음 작업을 한다.

```text
selected target을 unreachable로 요청
targetInvalidationReason=MOVEMENT_PROGRESS_FAILED
selected target clear
store-task generation clear
movement checker reset
```

중요한 보정은 위 branch가 selected candidate/store generation의 invalidation
decision이라는 점이다. 이것만으로 현재 active route child가 그 자리에서 실제
stop·replacement됐다고 단정할 수 없다. 실제 child close는 이후 generic Task
reconciliation의 `subTasksEqual`, `canInterrupt`, replacement와 previous-child stop
경계에서 별도로 관찰해야 한다.

이것은 per-item `DepositAllTask` store root의 terminal도, 상위
`AutoDepositMaintenanceTask` terminal도 아니다. 같은 per-item root는 chest
acquisition 또는 다음 candidate branch로 계속될 수 있다. 향후 diagnostics는
selected-candidate invalidation, route-child reconciliation/close, per-item root close를
서로 다른 event와 identity로 기록해야 한다.

또한 diagnostics budget 소진이나 diagnostic coverage abort도 gameplay root
terminal이 아니다. 별도 diagnostic-ledger close/coverage event로만 표현해야 하며
Task result를 바꾸면 안 된다.

default slot action delay만 고려하면 destination에 10회를 right-click하는 시간이
반드시 6초를 넘는다고 단정할 수 없다. exact cause를 증명하려면 이미 계산된 check
invocation/result, mode, target adoption, baseline/reset, GUI 진입, slot attempt,
selected-candidate invalidation과 이후 child reconciliation을 동일 causal ledger에
묶어야 한다.

## Source-confirmed mechanism 4: Carry On attribution과 context ambiguity

관련 source:

```text
lavi/minecraft/diagnostics/container/store/deposit/interaction/
  StoreDepositInteractionContext.java
  StoreDepositInteractionBindingRegistry.java
  StoreDepositInteractionDiagnosticFields.java

lavi/minecraft/integration/carryon/container/
  CarryOnContainerPickupEvidence.java
  CarryOnContainerPostconditionClassifier.java
  CarryOnContainerPostconditionWindow.java
```

현재 Store interaction HEAD capture와 downstream binding lookup은 각 단계의 여러
실패 원인을 모두 `null`로 합친다.

```text
no active DepositAll operation
no active task or target
branch is not OPEN_EXISTING
no active attempt
task is outside the current route
unrelated target

downstream lookup:
  expired binding
```

downstream에서는 이것들이 `NO_EXACT_ACTIVE_ROUTE_AND_TARGET_BINDING` 하나로
보일 수 있다. 향후 typed diagnostic verdict는 시점별로 분리해야 한다.

```text
HEAD capture verdict:
  capture 당시 operation, route, branch, attempt, target 판정

RETURN / lookup verdict:
  BINDING_EXPIRED_AFTER_HEAD
  ROUTE_CHANGED_AFTER_HEAD
  target mismatch after HEAD
```

`BINDING_EXPIRED_AFTER_HEAD`나 `ROUTE_CHANGED_AFTER_HEAD`를 HEAD capture가 이미
알았던 것처럼 기록하면 안 된다.

Carry On 쪽은 exact carried identity 중 하나라도 일치하면 target identity evidence를
가질 수 있지만, 모든 identity가 unavailable인 상태에서 carry edge만 있으면
`STRONG_TEMPORAL_ATTRIBUTION`이 될 수 있다. 현재 classifier는 이 strong 결과를
`targetRemoved` 또는 `targetStillMatches`보다 먼저 선택한다.

따라서 다음 조합은 exact target pickup의 증거가 아니다.

```text
NOT_CARRYING -> CARRYING observed
carried target identity unavailable
clicked crafting table or chest still present
```

향후 diagnostic classification은 target-retained/mismatch를 보존해 ambiguous로
보고해야 한다. 이것은 아직 behavior fix 승인이나 classifier source 수정 승인이
아니다.

## Runtime-confirmed chronology

primary excerpt에서 직접 확인한 `storeOperationId=store-deposit-114354`의 핵심
순서는 다음과 같다.

```text
tick 52635
  cobblestone x10 store operation 시작

tick 52638
  cursor cobblestone x64
  destination chest GUI active
  notStored x10, stored 0/10

tick 52873
  cursor cobblestone x17
  notStored x10, stored 0/10 유지

tick 52877
  targetInvalidationReason=MOVEMENT_PROGRESS_FAILED
  selected candidate/store generation invalidation
  acquisition branch로 전환
  actual route-child stop/replacement identity는 미관찰

tick 52998
  다음 chest GUI에서 cursor cobblestone x64

tick 53358
  두 번째 MOVEMENT_PROGRESS_FAILED
  selected candidate/store generation invalidation
  acquisition branch로 전환
  actual route-child stop/replacement identity는 미관찰

tick 53744 이후
  notStored x9, x8 ... 로 감소
  currently-available-and-unstored request cap이 감소
  durable container progress를 뜻하지 않음
```

질문서는 전체 automatic maintenance가 다음 5개 aggregate target/plan step을
처리해 occupied slots를 33에서 28로 낮췄다고 보고한다.

```text
clay ball x36
dirt x12
cobblestone x10
flint x6
oak planks x3
```

첨부 excerpt와 전체 로그 감사에는 위 per-item `DepositAllTask` sequence, automatic
구간 종료 뒤 `get emerald 10` user Task 재개, tick 54644 natural completion과 다음
terminal 전송이 존재한다. 이 후속 lifecycle은 `LOG_CONFIRMED`다. 그러나 automatic
operation, pressure-chain owned-run close, final occupied-slot snapshot과 user-task resume를
한 causal handoff identity로 묶은 완결 ledger는 없다.

따라서 다음을 분리한다.

```text
later user-task natural completion and terminal: LOG_CONFIRMED
automatic interval ended and user task resumed: LOG_CONFIRMED
explicit final occupiedCount=28 field: UNAVAILABLE
five whole-stack transfers -> 28/36 arithmetic: strongly supported inference
overall 33 -> 28 plus same-identity handoff ledger: REVIEW_REPORTED / UNPROVEN
```

위 항목을 exact physical stack, durable accepted container delta 또는 완결된 handoff
identity로 승격하지 않는다.

그러므로 이번 incident를 "automatic deposit이 전혀 실행되지 않았다"고 해석할 수는
없지만, 첨부 excerpt만으로 전체 automatic operation의 성공을 확정할 수도 없다.

Carry On 쪽에서는 `NOT_CARRYING -> CARRYING`과
`CARRY_ON_PICKUP_STRONGLY_ATTRIBUTED` event가 확인된다. 그러나 carried block
identity가 unavailable인 evidence는 exact chest ownership을 증명하지 못한다.
"sneak=false인데 왜 pickup이 발생했는가"와 정확한 click/input owner도 현재
로그로는 미증명이다.

## 보정된 causal model

현재 증거와 모순되지 않는 가장 좁은 가설은 다음과 같다.

```text
aggregate target x10
-> physical source x64 선택
-> destination에 one-by-one click
-> parent stored/notStored field가 기대대로 advance하지 않거나 transfer가 반복
-> selected target을 보유한 parent가 stationary movement check를 계속 평가
-> progress check false / MOVEMENT_PROGRESS_FAILED
-> current candidate/store generation invalidation
-> generic reconciliation에서 route child stop/replacement 여부 결정
-> chest acquisition 또는 다음 candidate
```

증거 수준은 단계별로 다르다.

```text
aggregate x10 -> physical x64: SOURCE_CONFIRMED + LOG_CONFIRMED
one-by-one destination action: SOURCE_CONFIRMED
exact-fit stackable destination rejection by strict `>`: SOURCE_CONFIRMED
exact-fit rejection caused runtime destination churn: UNPROVEN
parent progress field stored=0/10 유지: LOG_CONFIRMED
notStored x9/x8 means available request-cap decrease: SOURCE_CONFIRMED + LOG_CONFIRMED
ROOT_ANY_CONTAINER/TARGET_CONTAINER tracker 내부 원인: UNPROVEN
parent movement check mechanism: SOURCE_CONFIRMED
두 check=false triggers and candidate invalidations: SOURCE_CONFIRMED + LOG_CONFIRMED
actual route-child reconciliation for each invalidation: UNPROVEN
parent progress 정체가 movement failure를 직접 일으킴: INFERENCE
movement failure가 반복 acquisition으로 이어짐: SOURCE_CONFIRMED + LOG_CONFIRMED
Carry On exact target/action owner: UNPROVEN
```

이 causal model은 행동 수정의 근거가 아니라 다음 진단 slice의 관찰 순서를
정하는 working hypothesis다.

## Operation identity를 합치지 않는 계약

향후 diagnostics는 최소 세 층의 identity를 구분해야 한다.

### A. Policy identity

```text
autoOperationEpoch
inventorySnapshotId
autoPlanId
itemDecisionId
```

이 층은 "왜 cobblestone x10이 선택됐는가"를 소유한다. protection, working-set,
reserve, classification, surplus와 destination decision을 immutable snapshot으로
기록하되 실제 planner input이나 결과를 바꾸지 않는다.

### B. Maintenance / store / transfer identity

```text
maintenanceGenerationId
autoChildOperationId
storeOperationId
selectedCandidateGenerationId
storeAttemptId
routeChildLifecycleId
transferAttemptId
slotActionId
slotMutationId
trackerRole and trackerIdentity
```

이 층은 physical source selection, container binding, slot action, accepted delta,
`ROOT_ANY_CONTAINER`/`TARGET_CONTAINER` tracker binding, movement-check observation,
selected-candidate invalidation과 실제 route-child reconciliation을 소유한다.
movement checker의 개별 평가에는 별도 `progressCheckInvocationId`를 사용한다.
`MOVEMENT_PROGRESS_FAILED`는 invalidation decision이지 그 자체로 child close,
per-item store root close 또는 automatic maintenance close가 아니다.

### C. Carry On interaction / provenance identity

```text
chestAcquisitionGenerationId
interactionId
postconditionWindowId
carryObservationSequence
target provenance and identity availability
```

이 층은 click target, HEAD/RETURN, `+1/+2/+5` observation, carry edge,
target-retained/removed와 chest provenance를 소유한다. 시간적으로 가깝다는 이유만으로
policy decision이나 store candidate의 root cause로 승격하지 않는다.

## 다음 bounded diagnostics-only slice

아래 순서는 제안일 뿐이며 구현 승인이 아니다. 각 slice는 독립적인 책임과 test
gate를 가져야 하고, `StoreDepositDiagnostics` 한 파일을 다시 키우지 않는다.

### Slice A - transfer attempt / movement / candidate-close correlation

가장 먼저 필요한 증거이며 상세 계약의 canonical owner는 다음 문서다.

[ChatClef Automatic Deposit Slice A Diagnostics Contract](chatclef-auto-deposit-slice-a-diagnostics-contract-2026-08-30.md)

이 incident 문서는 다음 핵심만 중복해 고정한다.

```text
one auto operation -> one maintenance generation -> per-item roots
one transfer attempt -> one or more slot actions -> one or more slot mutations
same slotMutationId -> both tracker observations when both were subscription-active
local click mutation != durable/server-confirmed effect
progress check false -> selected-candidate invalidation
selected-candidate invalidation != route-child reconciliation/close
maintenance logical terminal != pressure-chain owned-run close
diagnostic coverage close is orthogonal to gameplay lifecycle
```

관찰 목적으로 progress check/reset/setProgress, slot-fit selector, inventory scan 또는
tracker predicate를 다시 호출하지 않는다. 기존 generic slot event가 VERBOSE-only라는
사실도 유지하며, BOUNDARY projection은 별도의 bounded observer 계약을 따라야 한다.

### Slice B - immutable per-item policy decision

`cobblestone x10` 선택 이유를 증명한다.

```text
inventory snapshot identity
exact physical stacks and protected slot flags
working-set count and reason
category reserve count and reason
classification and confidence
current count, protected count and computed surplus
destination class
accepted/rejected/deferred reason
```

이 snapshot은 policy decision이 끝난 직후의 출력이어야 하며 planner/executor의
새 입력이 되어서는 안 된다. 전체 NBT, 이름, lore 또는 민감한 metadata 원문은
출력하지 않고 기존 bounded fingerprint 경계를 재사용한다.

### Slice C - typed interaction verdict / Carry On / chest provenance

```text
HEAD capture verdict
RETURN or lookup verdict
interactionId and exact target position
store operation/attempt identity when exact binding exists
chest acquisition generation and provenance
carry state before and after
identity evidence availability
target removed/still present/mismatch
postcondition classification basis
```

기존 `+1/+2/+5` postcondition window를 유지한다. retry, click, timeout, input,
Carry On API invocation 또는 cleanup을 추가하지 않는다.

### Slice D - terminal hierarchy and diagnostic coverage ledger

각 lifecycle owner는 실제 natural finish, cancel, interruption, world leave 또는 기존
lifecycle terminal에서만 닫는다. 이 구조는 한 번씩 이어지는 선형 chain이 아니다.
하위 scope는 한 상위 scope 안에서 여러 번 열리고 닫힐 수 있으며 diagnostic coverage는
gameplay Task 계층과 직교한다.

```text
slot action / slot mutation close:
  GUI action 또는 changed-slot observation fact

transfer-attempt close:
  source/destination/cursor attempt fact

selected-candidate/store-generation invalidation:
  local route decision fact; child stop을 아직 뜻하지 않음

route-child reconciliation/close:
  active child의 실제 stop/replacement fact

per-item DepositAll store-root close:
  one aggregate ItemTarget lifecycle fact

automatic-maintenance logical terminal:
  AutoDepositMaintenanceTask DONE/CANCELLED fact

pressure-chain owned-run close:
  chain-owned root stop/clear and RUNNING -> WAIT_FOR_REARM fact

diagnostic coverage close:
  observation completeness fact; gameplay terminal과 독립

budget suppression summary:
  logging fact only
```

critical terminal summary와 coverage status는 bounded reserve로 보존하되, cap 소진이
Task의 terminal reason이나 control flow를 바꾸지 않아야 한다.

## 책임과 package 경계

향후 source task가 승인되면 responsibility가 둘 이상인 경우 LAVI-owned
collaborator로 분리한다. 이름은 설계 후보일 뿐 생성 승인이 아니다.

```text
diagnostics/container/store/deposit/
  transfer/     physical source, slot action and accepted-delta observation
  route/        candidate generation, movement boundary and attempt close
  interaction/  HEAD/RETURN typed binding verdict
  terminal/     store-root/maintenance terminal hierarchy and diagnostic coverage ledger

task/container/deposit/auto/diagnostics/
  immutable policy snapshot and item-decision observation

integration/carryon/container/
  existing optional state/evidence/postcondition boundary
```

기존 `StoreDepositDiagnostics` public static facade와 upstream-facing call sites는
유지한다. `StoreContainerRouteState`는 여러 mutable owner로 분해하지 않고 한
synchronized aggregate owner를 유지한다. immutable snapshot 또는 pure formatter만
그 밖으로 추출할 수 있다.

## 필수 characterization 및 direct tests

Slice A의 canonical test matrix는
[ChatClef Automatic Deposit Slice A Diagnostics Contract](chatclef-auto-deposit-slice-a-diagnostics-contract-2026-08-30.md)의
16개 grouped scenario가 소유하며, 그 안에서 독립 검토의 24개 contract assertion을
빠짐없이 고정한다. 이 문서는 Slice B-D 및 entry-path 불변 계약만 요약한다.

```text
1. HEAD capture verdict와 RETURN/lookup verdict 분리
2. strong temporal edge + target retained + identity unavailable은 exact attribution 아님
3. immutable policy decision snapshot이 planner/executor 입력이나 결과를 바꾸지 않음
4. manual @deposit_all, @deposit and @store_home event attribution 불변
```

test source가 존재하는 것은 `TEST_SOURCE_PRESENT`일 뿐이다. 실제 command, exact
result, failures, skipped count와 current source identity가 기록되기 전에는
`TEST_PASSED_CURRENT`로 승격하지 않는다. test deselection, retry 또는 약화는
허용하지 않는다.

## Runtime reproduction gate

향후 별도 승인이 있으면 diagnostics patch, focused tests, targeted Java tests,
clean forced build, artifact deployment identity를 먼저 완료한 뒤 한 번의 bounded
reproduction으로 다음 순서를 확인한다.

```text
known occupied-slot threshold crossing
known physical stack layout, including aggregate target smaller than a stack
one controlled existing chest candidate
automatic plan/item decision
store operation and candidate attempt start
transfer attempt ledger
movement-check observations without extra calls
selected-candidate invalidation and route-child reconciliation, or confirmed transfer
per-item DepositAll store-root terminal
automatic-maintenance logical terminal
pressure-chain owned-run close and WAIT_FOR_REARM transition
preserved user Task resume and terminal
diagnostic coverage close recorded independently
```

기록할 최소 provenance:

```text
source HEAD and dirty/clean state
clean forced build command and result
built JAR path, size and SHA-256
deployed JAR path, size and SHA-256
active instance and loaded mod identity
diagnostics mode and budgets
automatic operation through terminal identity
expected and observed event counts
```

한 entry path의 결과를 manual command, Korean chat, microphone 또는 StoreHome
runtime proof로 일반화하지 않는다.

## `@store_home` 및 upstream 보호 경계

향후 automatic-only correlation은 active automatic maintenance/child identity가
정확히 존재할 때만 붙어야 한다. shared `DepositAllTask` 안에서 관찰해야 하더라도
automatic context가 없으면 complete no-op이어야 한다.

다음 영역은 이번 incident diagnostics의 수정 대상이 아니다.

```text
StoreHomeTask, planner, executor, timeout, result or manifest behavior
manual @deposit_all or @deposit selection and transfer behavior
TaskRunner, UserTaskChain or global Task lifecycle
InteractWithBlockTask completion
PlayerInteractionFixChain
Baritone goal, path, input, retry or ownership
StoreInContainerTask transfer semantics
Carry On dependency/version or state-changing API
automatic threshold 33/36 and low-water 28/36
policy, reserve, protection or trusted-destination decisions
```

필요한 관찰 경계가 upstream-derived source 내부에만 있다면 `AGENTS.md`가 허용하는
최소 method/hunk diagnostics-only divergence인지 먼저 판정한다. 여러 upstream
lifecycle owner, behavior change 또는 broad refactor가 필요하면 구현하지 않고
중단·보고한다.

## 종료 조건

diagnostics 단계는 다음 질문을 한 operation ledger로 답하기 전까지 끝나지 않는다.

```text
왜 이 item과 exact surplus count가 선택됐는가?
어느 physical source stack을 집었는가?
어느 destination slot에 어떤 action을 보냈는가?
cursor와 destination은 어떻게 변했는가?
ROOT_ANY_CONTAINER와 TARGET_CONTAINER tracker가 같은 slotMutation에서 각각 어느 signed delta를 승인 또는 거부했는가?
notStored는 ROOT_ANY_CONTAINER stored total과 inventory/cursor/conversion available 중 어떤 입력으로 결정됐는가?
movement checker는 어느 invocation/mode에서 false를 반환했고 baseline/elapsed/reset provenance는 무엇인가?
그 false 결과가 selected candidate를 무효화한 뒤 실제 route child reconciliation은 어떻게 끝났는가?
새 chest acquisition은 어느 generation에서 왜 시작됐는가?
Carry On edge는 정확한 target identity를 가졌는가?
per-item store root와 automatic maintenance는 각각 어떻게 종료됐는가?
automatic maintenance와 user Task resume은 같은 handoff identity로 연결되는가?
```

그 전의 상태는 다음과 같다.

```text
specific source mechanisms: SOURCE_CONFIRMED
transfer/movement runtime boundaries: LOG_CONFIRMED
later user-task natural completion and terminal: LOG_CONFIRMED
explicit final occupiedCount=28: UNAVAILABLE
overall 33 -> 28 maintenance result and same-identity resume handoff: REVIEW_REPORTED / UNPROVEN
cross-boundary causal identity: UNPROVEN
final root cause: UNPROVEN
behavior fix: PROHIBITED AT THIS STAGE
next direction in a future investigation/debug source task when the AGENTS.md
standing diagnostics-only authorization applies, or explicit authorization is otherwise required:
  SMALLEST BOUNDED DIAGNOSTICS-ONLY SLICE A
```

## Earlier cobblestone incident 독립 재검수 반영 결과

| 검토 항목 | 반영 판정 | 이 문서의 보정 |
| --- | --- | --- |
| 전체 방향 | `PASS_WITH_REQUIRED_CORRECTIONS` | Diagnostics-first와 behavior gate를 유지했다. |
| Tracker 의미 | `CORRECTED` | Exact role을 `ROOT_ANY_CONTAINER`/`TARGET_CONTAINER`로 고정하고 subscription 및 BlockPos predicate 한계를 추가했다. |
| `notStored` 의미 | `CORRECTED` | 저장 잔량이 아니라 available-and-unstored request cap일 수 있음을 source 식과 함께 고정했다. |
| Movement lifecycle | `CORRECTED` | Check-false, candidate invalidation과 실제 route-child reconciliation/close를 분리했다. |
| Durable effect | `CORRECTED` | Local internal-click mutation과 stable/server-confirmed effect를 분리했다. |
| Exact-fit mechanism | `ADDED_AS_SOURCE_CONFIRMED` | Strict `roomLeft > sourceCount`를 기록하되 runtime 원인과 comparator 수정은 미승격했다. |
| Slice A | `SUFFICIENT_AFTER_MINIMUM_ADDITIONS` | 상세 계약을 별도 문서로 분리하고 10개 core identity, 필수 field, stop gate와 16 grouped/24 assertion test matrix를 고정했다. |
| Final root cause | `UNPROVEN` | Behavior fix는 계속 금지한다. |

```text
INCIDENT_SCOPE: COBBLESTONE_TRANSFER_MOVEMENT_CAPTURE
DOCUMENT_DIRECTION: PASS_WITH_REQUIRED_CORRECTIONS
FINAL_ROOT_CAUSE: UNPROVEN
SLICE_A_PRIORITY: PASS
IMMEDIATE_BEHAVIOR_CHANGE: NOT_AUTHORIZED
STORE_HOME: OUT_OF_SCOPE
```

## 2026-08-30 23:26 KST 후속 재현: automatic-deposit chest pickup

### 후속 재현 범위와 권한

이 section은 같은 날짜의 더 늦은 live runtime evidence를 별도 incident ledger로
추가한다. 앞 section의 cobblestone transfer/movement 판정은 소급 변경하지 않는다.

```text
document-local reproduction id:
  auto-deposit-carryon-chest-20260830-232646

command:
  get diamond 1

capture-time active log path:
  C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log

observed command interval:
  2026-08-30 23:26:46 KST onward

capture path status:
  live at capture; mutable pointer subsequently rotated at midnight

canonical rotated evidence artifact:
  C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\2026-08-30-3.log.gz
  compressed bytes: 231,308
  compressed SHA-256: 8D6ECD3C88E3687676EDC9FBDB91FAA180A72109F5F18138B8DEBE72984A20F1
  uncompressed bytes: 5,748,673
  uncompressed SHA-256: 33A28794CC7372FCC1C48A95AFCA634C3A21D5998D55F8E386C48213F9AF0A8D
  uncompressed LF-terminated lines: 7,086

documentation evidence cut-off within the uncompressed archive:
  2026-08-30 23:51:32 KST
  decompressed line 5720
  uncompressed prefix bytes through line 5720: 4,966,473
  uncompressed prefix SHA-256: 1F59EC5EAE0EC502EF04EB9ADEE771CE75B8BF8C8BBCE258086A0E935FDA7EA1

latest cited diagnostic evidence:
  decompressed archive line 5713 at 2026-08-30 23:51:31 KST

log provenance limitation:
  line references below are UTF-8 decompressed archive lines
  archive content continues after the documentation cut-off

repository:
  C:\Vtuber_Souorce_Code\LAVI
branch:
  minecraft-plugin-fix/alto-clef-infinite-loop
HEAD observed before this documentation edit:
  14ba9b443f0bc11d6860a25d7fd3b8b916d95a04
worktree:
  DIRTY - existing user/source/document changes preserved

repository edit for this follow-up conclusion:
  Markdown only
Java/test/resource edit:
  NO
build/test/Minecraft launch performed by this documentation task:
  NO
behavior change authorized by this section:
  NO
```

이 addendum의 line number는 capture 당시 `latest.log`의 순서를 보존한다. 재검수와
향후 인용의 canonical byte source는 위 rotated archive의 UTF-8 decompressed
stream이며, 이 판정의 고정 범위는 전체 archive가 아니라 line 5720에서 끝나는
hash-addressed prefix다.

Active runtime artifact identity observed read-only:

```text
path:
  C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\chatclef-1.20.1-0.18.23.jar
bytes:
  7,348,369
last write:
  2026-08-30 23:19:18.236 KST
SHA-256:
  D9D680CC7FF261165547D20B2D2AC87B3983C8EE5F98F5C7402BA077AAE5202F
```

이 identity는 active JAR을 특정한다. 이 documentation task가 clean forced build를
실행했다거나 dirty source 전체와 JAR이 byte-equivalent하다는 뜻은 아니다.

### Correlation ledger

| Identity | Value |
| --- | --- |
| `traceId` | `trace-1` |
| request | `lavi-input-ko-ab307775f95c42b59ba8288c8b098313` |
| command correlation | `lavi-a5c58863133143e590dfc83673d598b3` |
| session | `fabric-chatclef-ff34ac70d8694d30917f06ddc1f1d66b` |
| user root identity | `43384a21` |
| `autoOperationId` | `auto-deposit-1` |
| maintenance generation | `auto-deposit-1-maintenance-1` |
| automatic child | `auto-deposit-1-child-1` |
| pressure-owned run | `auto-deposit-1-pressure-run-1` |
| store operation | `store-deposit-1831` |
| store task identity | `c2f9673` |
| clicked-container interaction | `1852` |
| `taskRunId` / `parentTaskRunId` | `unavailable` |

`interactionId=1852`의 interaction observer에는
`storeContextAvailable=false`와
`NO_EXACT_ACTIVE_ROUTE_AND_TARGET_BINDING`이 기록됐다. 따라서 interaction event와
`store-deposit-1831`을 잇는 typed direct binding은 `UNPROVEN`이다. 다만 같은
command correlation, tick, clicked position, selected automatic chain과 Task path는
보존돼 있으며 아래 outer temporal/state-transition chain을 직접 재구성할 수 있다.

### Exact runtime chronology

| Rotated archive decompressed line | Tick | Event | 직접 관찰된 사실 |
| ---: | ---: | --- | --- |
| 1018-1025 | 695 | bridge queue/dispatch 및 user-root assignment | `get diamond 1`이 request/correlation/session과 함께 `MineAndCollectTask` root로 수락됐다. |
| 1414 | 877 | `MINING_DIAGNOSTIC_SUPPRESSION_SUMMARY` | command correlation의 mining detail `256/256`이 automatic deposit 시작 전에 이미 소진됐다. session은 `688/5000`이었다. |
| 1479 | 987 | `AUTO_DEPOSIT_POLICY_SNAPSHOT` | inventory pressure `33/36`, relief 5, 첫 general target `dirt x22`로 automatic run이 시작됐다. |
| 1552 | 988 | `STORE_IN_ANY_CONTAINER_START` | automatic child가 `store-deposit-1831`의 `DepositAllTask`를 시작했다. |
| 1556 | 988 | `STORE_IN_ANY_CONTAINER_PROGRESS_STATE` | `dirt 0/22`, branch `return_place_container_nearby`, child `PlaceBlockNearbyTask`, container item `chest`였다. |
| 1671 | 998 | `STORE_CONTAINER_PARENT_CANDIDATE_DECISION` | 직전 `PLACE_CONTAINER_NEARBY` / `PlaceBlockNearbyTask` 실행 구간 뒤 `-1033,9,-149`의 chest가 `FILTERED_SCAN` candidate로 관찰돼 `OPEN_EXISTING` 대상으로 선택됐다. 이 row 하나는 exact placement completion/position binding을 증명하지 않는다. |
| 1681-1682 | 998 | route reconciliation | `CANDIDATE_INSTALLED`; active child가 `PlaceBlockNearbyTask -> StoreInContainerTask`로 실제 교체됐고 previous child stop이 관찰됐다. |
| 1685 | 998 | `CONTAINER_OPEN_ATTEMPT_OBSERVED` | `interactionId=1852`, chest `-1033,9,-149`, main-hand open 시도가 기록됐다. |
| 1686-1687 | 998 | return snapshot / `CONTAINER_OPEN_RETURN_OBSERVED` | `interactResult=SUCCESS`였지만 GUI는 없고 handler `class_1723`, syncId 0이 유지됐다. Carry On은 `AVAILABLE_NOT_CARRYING`, target은 여전히 chest였다. `sneakHeld`, raw sneak key와 player sneaking은 모두 false였다. |
| 1689 | 999 | independent inventory scan | 같은 automatic task path에서 Carry On 상태가 `AVAILABLE_CARRYING`으로 관찰됐다. |
| 1694-1698 | 999 | candidate invalidation 및 parent decision | selected target이 `UNSUPPORTED_CONTAINER`로 무효화되고 `OPEN_EXISTING -> OBTAIN_CHEST`; `requestedItem=chest`, `dirt 0/22`가 됐다. line 1695의 terminal은 `CANDIDATE_INVALIDATION` scope이며 store root나 maintenance terminal이 아니다. |
| 1704 | 999 | `STORE_TASK_CHILD_RECONCILIATION` | active child가 `StoreInContainerTask -> CraftInTableTask`로 실제 교체돼 새 chest 제작/획득 경로에 들어갔다. |
| 1721 | 999 | terminal `CONTAINER_OPEN_INTERACTION_OUTCOME_WINDOW` | 같은 `interactionId=1852`에서 `NOT_CARRYING -> CARRYING`, clicked target `chest -> air`, `targetRemoved=true`, GUI/handler 변화 없음, `outcome=CARRY_ON_PICKUP_STRONGLY_ATTRIBUTED`가 기록됐다. |
| 1740 / 1768 | 1010 / 1241 | Destroy progress | 새 chest 재료용 birch-log acquisition에서 `baritonePathing=true`에 따라 movement checker reset이 기록된 뒤 progress check failure가 관찰됐다. 이 두 line은 Baritone pathing 자체가 reset됐음을 증명하지 않는다. |
| 5713 | 30399 | stable progress summary | 같은 store task가 `return_obtain_chest_item`, `dirt 0/22`, `noProgressTicks=29400`, 동일 player position으로 계속 정체됐다. |

### 보정된 인과 판정

이 후속 재현에서 exact placement ownership과 carried-block identity를 아래처럼
제한하면, 다음 outer transition은 더 이상 단순 가설이 아니다.

```text
automatic pressure activation
-> dirt x22 DepositAll
-> placement path 실행 뒤 같은 위치의 chest가 새 candidate로 관찰됨
-> 같은 위치를 OPEN_EXISTING 대상으로 채택
-> chest open interaction 1852
-> interactResult=SUCCESS, 그러나 GUI/handler 미개방
-> 다음 tick Carry On NOT_CARRYING -> CARRYING
-> 같은 clicked position chest -> air / target removed
-> selected container UNSUPPORTED_CONTAINER invalidation
-> OPEN_EXISTING -> OBTAIN_CHEST
-> CraftInTable / planks / log acquisition
-> dirt stored 0/22 상태로 장기 정체
```

증거 수준은 다음처럼 제한한다.

```text
command dispatch and automatic activation:
  LOG_CONFIRMED

PlaceBlockNearbyTask execution, later same-position chest selection and actual route-child handoff:
  LOG_CONFIRMED

that Task placed that exact chest at that exact position:
  INFERENCE - strongly supported sequence, but no exact placement terminal/target binding

click returned SUCCESS without expected GUI:
  LOG_CONFIRMED

NOT_CARRYING -> CARRYING at +1 tick:
  LOG_CONFIRMED

clicked target chest -> air / removed at +1 tick:
  LOG_CONFIRMED

Carry On pickup attribution for interaction 1852:
  STRONG_TEMPORAL_ATTRIBUTION

exact carried-block API identity:
  UNAVAILABLE
  observedCarriedBlockId=unavailable
  targetIdentityMatchesCarriedBlock=false

OPEN_EXISTING -> OBTAIN_CHEST and CraftInTable child replacement:
  LOG_CONFIRMED

long-lived dirt 0/22 obtain-chest stall:
  LOG_CONFIRMED
```

따라서 “상자를 든 직후 automatic deposit이 `OPEN_EXISTING`에서 `OBTAIN_CHEST`로
이탈한 이유”의 **observable outer route-divergence boundary**는 충분히 확인됐다.
정확한 carried-block identity가 API로 제공되지 않았으므로 `TARGET_VERIFIED`라고
과장하지 않고 `STRONG_TEMPORAL_ATTRIBUTION`을 유지한다. 같은 interaction의
before/after carry state, clicked-block removal, GUI non-open과 즉시 이어진 store
branch change는 이 observable boundary를 다시 `UNKNOWN_CAUSE`로 되돌리지 않는다.

이 판정은 “왜 Carry On이 sneak=false click을 pickup으로 처리했는가”라는 primary
activation root cause나, 이후 Baritone이 왜 장기 정체했는가라는 secondary root cause를
확정하지 않는다. Observable boundary 확정과 root-cause patch eligibility를 합치지
않는다.

### 여전히 미확정인 범위

다음 항목은 위 1차 판정을 확대해 확정하지 않는다.

```text
왜 Carry On 2.1.2.7이 sneak=false인 이 click을 pickup으로 처리했는가
Carry On 내부의 exact activation method, config branch 또는 action owner
exact carried block id/state/NBT identity
interactionId=1852와 store-deposit-1831의 typed direct binding
automatic child의 taskRunId / parentTaskRunId
새 chest acquisition descendant에서 Baritone이 이동하지 않은 exact inner cause
전체 per-item store root / maintenance / pressure-owned run terminal
```

특히 line 1414의 correlation detail cap은 automatic deposit 시작 전 mining 상세를
소진했다. 이후 generic store/interaction event로 primary outer chain은 보였지만,
새 chest용 mining의 calculation/adoption/executor 내부 원인을 완결된 같은-correlation
ledger로 증명하지 못했다. 이것은 primary Carry On 경계를 다시 미확정으로 만드는
이유가 아니라 secondary Baritone stall의 별도 coverage gap이다.

후속 `OBTAIN_CHEST` descendant의 candidate churn, movement-check 의미, Baritone
coverage gap과 cache-troubleshooting gate의 canonical 판정은
[ChatClef Automatic Deposit Obtain-Chest Mining Diagnostics Review](chatclef-auto-deposit-obtain-chest-mining-diagnostics-review-2026-08-30.md)의
section 18이 소유한다. 이 문서는 Carry On interaction에서 그 secondary mining
route로 진입한 outer handoff만 소유한다.

### 화로 handoff 선례와 이번 경로의 차이

과거 화로 incident도 `interactResult=SUCCESS`, GUI 미개방, target block removal,
Carry On `CARRYING`과 container 재획득이라는 같은 표면 증상을 보였다. 당시에는
`DoStuffInContainerTask`의 다음 원칙으로 처리했다.

```text
PlaceBlockNearbyTask finish
-> return null once so the previous child onStop cleanup runs
-> observe SNEAK stability for at most 3 ticks
-> continue the existing open flow
-> existing SmeltInFurnaceTask predicate requires an actual FurnaceScreenHandler
```

마지막 `FurnaceScreenHandler` predicate는 post-place handoff hunk가 새로 만든 조건이
아니라 `SmeltInFurnaceTask.isContainerOpen()`이 이미 소유하던 성공 계약이며, hunk가
그 계약을 보존했다. Handoff의 exact behavior와 runtime furnace failure boundary는
[ChatClef Engine Divergence Record](chatclef-engine-divergence-record.md)와
[ChatClef Tick Accuracy Principles](chatclef-tick-accuracy-principles.md)에 기록돼 있다.

그 hunk는 SNEAK을 전역 force-release하지 않았고 Carry On type, retry, Baritone
cancel 또는 `InteractWithBlockTask.isFinished()` 변경을 넣지 않았다.

이번 automatic deposit은 다음 별도 route를 사용한다.

```text
DepositAllTask
-> PlaceBlockNearbyTask
-> StoreInContainerTask
-> AbstractDoToStorageContainerTask
-> InteractWithBlockTask
```

이 route는 `DoStuffInContainerTask`의 bounded post-place handoff를 통과하지 않는다.
실제 runtime에도 `POST_PLACE_HANDOFF`, `POST_PLACE_STABILITY` 또는
`CONTAINER_GUI_OPENED` event가 없었다. 따라서 화로 처리는 lifecycle handoff와 실제
GUI 성공 판정의 설계 선례로 참고할 수 있지만, 코드를 복사하거나 같은 residual
SNEAK 원인으로 단정할 근거는 아니다. 이번 click에서는 세 SNEAK 관찰값이 모두
false였으므로 exact activation trigger는 계속 `UNPROVEN`이다.

### Diagnostics stop decision

Primary Carry On observable boundary를 같은 방식으로 다시 증명하기 위한 broad/global
runtime logging은 더 이상 필요하지 않다. 그러나 canonical Carry On evidence gate가
요구하는 exact Task와 method, triggering state, last-success/first-failure boundary,
retry/input/goal-path/cleanup ownership과 generic-interaction preservation evidence는
root-cause patch proposal 전에 별도 evidence/ownership report로 충족해야 한다.
Read-only source/ownership audit로 그 증거를 채울 수 없을 때는 별도 승인된 smallest
targeted diagnostics가 여전히 필요할 수 있다. `broad logging 불필요`를 `모든 추가
관찰 불필요`로 확대하지 않는다.

```text
INCIDENT_SCOPE: AUTO_DEPOSIT_CARRYON_CHEST_20260830_232646
PRIMARY_OBSERVABLE_OUTER_FAILURE_BOUNDARY:
  ESTABLISHED

PRIMARY_CAUSAL_ATTRIBUTION:
  CARRY_ON_PICKUP_STRONGLY_ATTRIBUTED

PRIMARY_ENTRY_INTO_OBTAIN_CHEST:
  RUNTIME_LOG_CONFIRMED

EXACT_CARRY_ON_ACTIVATION_OWNER_AND_METHOD:
  UNPROVEN

FINAL_ROOT_CAUSE:
  UNPROVEN

PRIMARY_ADDITIONAL_BROAD_RUNTIME_DIAGNOSTICS:
  NOT_REQUIRED_FOR_THE_OBSERVED_OUTER_BOUNDARY

PRE_CHANGE_EVIDENCE_OWNERSHIP_REPORT:
  REQUIRES_SEPARATE_AUTHORIZATION

ROOT_CAUSE_PATCH_PROPOSAL_GATE:
  NOT_YET_SATISFIED

SECONDARY_BARITONE_STALL_ROOT_CAUSE:
  UNPROVEN

SECONDARY_DIAGNOSTICS:
  DEFER_UNTIL_AFTER_SEPARATELY_APPROVED_PRIMARY_PATCH_APPLICATION_AND_REPRODUCTION
  ADD_ONLY_IF_THE_SAME_STALL_REMAINS

BEHAVIOR_FIX_APPLICATION:
  NOT AUTHORIZED BY THIS DOCUMENTATION TASK
```

향후 별도 pre-change evidence/ownership report는
[ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)의
`Required Evidence Before Root-Cause Patch`, ownership ledger와 last-resort gate를
먼저 충족해야 한다. Expected ScreenHandler/GUI open을 container success로 판정하고
Carry On `NOT_CARRYING -> CARRYING` transition을 pickup attribution으로 별도 판정하는
것은 검토할 invariant이지, 어느 한 전이를 다른 쪽 success로 변환하거나 이 문서가
특정 solution을 승인한다는 뜻이 아니다. Exact activation trigger를 residual SNEAK으로
가정하거나 화로 hunk를 복사하지 않는다. `InteractWithBlockTask`,
`PlayerInteractionFixChain`, TaskRunner, Baritone 또는 전역 input/cleanup 변경은 이
evidence로 제안·적용할 수 없다.

## 2026-08-31 pre-change evidence and ownership addendum

<!-- 20260831_openai: Linked the later installed-JAR timing proof and the narrowly authorized automatic-deposit post-place handoff without rewriting the historical incident statuses above. -->

The earlier status blocks remain historical records of the evidence available
when each section was written. A later read-only inspection of the exact
installed Carry On 2.1.2.7 JAR, the active instance key configuration, and the
current ChatClef scheduler closes only the narrow automatic post-placement
handoff gate.

The complete direct-versus-deductive evidence ledger, mandatory design report,
ownership table, exact file scope, regression matrix, and rollback unit are in
[ChatClef Automatic Deposit Post-Place Handoff Pre-Change Report](chatclef-auto-deposit-post-place-handoff-pre-change-report-2026-08-31.md).

New evidence established after the earlier documentation-only review:

```text
installed artifact:
  carryon-fabric-1.20.1-2.1.2.7.jar
  SHA-256 8D25FB164FC4CC15CD9123A915C880C55527EFE3FEC26B2C7FEBF09E65C6EBDD

active key configuration:
  key_key.carry.desc:key.keyboard.unknown

installed-bytecode timing:
  CarryOnCommonClient.checkForKeybinds runs from END_CLIENT_TICK
  the unbound-key false branch sends ServerboundCarryKeyPressedPacket(false)
  PickupHandler.canCarryGeneral rejects keyPressed=false

ChatClef timing:
  TaskRunner runs from the MinecraftClient.tick HEAD path
  a normal child replacement stops and ticks its replacement in the same call
  a null result stops and clears the previous child without ticking a replacement

existing owned cleanup:
  PlaceBlockNearbyTask.onStop -> stopPlacing -> release Input.SNEAK
```

The failed reproduction did not directly log the false packet or server-side
`keyPressed` field. However, the installed pickup code's necessary condition and
the runtime-observed `NOT_CARRYING -> CARRYING` plus `chest -> air` transition
prove that the pickup callback observed `keyPressed=true`. The tick ordering then
identifies the missing boundary: the automatic route opened the newly placed
chest before Carry On's end-tick reconciliation opportunity.

The authorized containment is therefore one `DepositAllTask` parent result of
`null` after its retained actual `PlaceBlockNearbyTask` finishes, enabled only by
`AutoDepositGeneralTaskFactory#create`. It uses the existing scheduler stop and
placement cleanup, then resumes the existing store/open route on the next tick.
It adds no Carry On API dependency and changes no generic interaction, TaskRunner,
input, or Baritone owner.

Gate status at this pre-change addendum boundary:

```text
PRE_CHANGE_EVIDENCE_OWNERSHIP_REPORT:
  COMPLETE - linked report is authoritative

NARROW_AUTO_GENERAL_POST_PLACE_HANDOFF_GATE:
  SATISFIED

DIRECT_RUNTIME_FALSE_PACKET_OBSERVATION:
  UNAVAILABLE - required-condition and ordering deduction only

BROADER_CARRY_ON_ENGINE_OR_GENERIC_INTERACTION_FIX:
  NOT AUTHORIZED / GATE NOT SATISFIED

SECONDARY_BARITONE_OBTAIN_CHEST_STALL:
  UNPROVEN / OUTSIDE THIS PATCH

BUILD, DEPLOYMENT, MINECRAFT REPRODUCTION, COMMIT, PUSH AT THIS BOUNDARY:
  NOT AUTHORIZED BY THAT IMPLEMENTATION REQUEST
```

This addendum does not convert exact carried-block identity, typed interaction-to-
store binding, or the later Baritone stall into proven facts. It also does not
authorize copying the furnace implementation, changing Carry On settings,
forcing SNEAK globally, or modifying `InteractWithBlockTask`,
`PlayerInteractionFixChain`, `Task`, or `TaskRunner`.

## 2026-08-31 separately authorized build and runtime verification update

<!-- 20260831_openai: Recorded the later authorized verification of the narrow post-place handoff while preserving the historical incident and pre-change gate states. -->

The user subsequently authorized the clean forced build and performed a fresh
Minecraft reproduction. The earlier incident classifications and pre-change
authorization statements remain historical. The current evidence for the narrow
automatic-general handoff is:

```text
UNIT TESTS FOR MINECRAFT 1.20.1:
  PASSED_WITH_ONE_SKIPPED_NON_HANDOFF_TEST
  378 total; 377 passed; 0 failures; 0 errors; 1 skipped

CLEAN FORCED BUILD:
  PASSED
  .\gradlew.bat clean build --rerun-tasks
  BUILD SUCCESSFUL in 4m 43s
  171 actionable tasks; 171 executed

BUILT / ACTIVE JAR:
  7,360,897 bytes
  SHA-256 84C6634433D7402B2935ADD6E4028F43BD3782D2E4038E00D30206092E9CA839
  HASH MATCH VERIFIED

MINECRAFT RUNTIME REPRODUCTION:
  OBSERVED

NARROW AUTO-GENERAL POST-PLACE HANDOFF:
  VERIFIED_FOR_THE_CAPTURED_SCENARIO_ONLY

HELD-CHEST STALL IN THE CAPTURED RUN:
  NOT OBSERVED

BROADER CARRY ON / GENERIC INTERACTION FIX:
  NOT AUTHORIZED / NOT REQUIRED BY THIS VERIFICATION

SECONDARY BARITONE OBTAIN-CHEST STALL:
  UNPROVEN / OUTSIDE THIS PATCH

BOUNDED DIAGNOSTICS COMPLIANCE:
  NOT VERIFIED - separate volume and terminal-reserve findings remain

COMMIT AND PUSH:
  NOT PERFORMED
```

The captured automatic-general sequence showed the actual placement child stop
and clear, followed on the next parent tick by `OPEN_EXISTING`, chest interaction
`SUCCESS`, delayed GUI observation, transfer start, and
`free_slot_postcondition_observed`. Carry On 2.1.2.7 remained
`AVAILABLE_NOT_CARRYING`; no `NOT_CARRYING -> CARRYING` pickup occurred in this
successful run.

This result verifies the narrow handoff only for the captured configuration and
scenario. It does not retroactively turn direct false-packet observation into a
fact, prove every container type or Carry On configuration, or resolve the
separate Baritone resource-reload exception and diagnostics-volume findings.
The full evidence ledger is in
[ChatClef Automatic Deposit Post-Place Handoff Pre-Change Report](chatclef-auto-deposit-post-place-handoff-pre-change-report-2026-08-31.md#16-post-implementation-verification-update).
