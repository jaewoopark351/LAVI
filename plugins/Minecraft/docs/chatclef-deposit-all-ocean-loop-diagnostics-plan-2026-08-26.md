<!-- 20260826_kpopmodder: Documented a bounded diagnostics-only plan for the deposit_all ocean loop. -->
<!-- 20260826_kpopmodder: Recorded D0-D4 runtime evidence and the next bounded diagnostics-only slices. -->
<!-- 20260826_kpopmodder: Closed the diagnostic phase with D7-D9 runtime evidence and the candidate-consistency direction. -->
<!-- 20260826_kpopmodder: Recorded the implemented B0.1 candidate-consistency source boundary. -->
<!-- 20260826_kpopmodder: Recorded post-commit runtime evidence and the same-target child lifecycle review failure. -->

# ChatClef @deposit_all Ocean Loop Diagnostics Plan

문서 상태: `B0_1_RUNTIME_OBSERVED_SAME_TARGET_CHILD_LIFECYCLE_FIX_REQUIRED_RELEASE_BLOCKED`

작성 기준일: 2026-08-26

이 문서는 바다에서 bare `@deposit_all`이 저장 단계로 수렴하지 않았지만 육지와
나무가 있는 곳에서는 정상 완료된 재현을 바탕으로 진단 범위를 고정했고, 이후
D7-D9 runtime 증거로 확정한 root cause와 최소 동작 수정 방향을 기록한다.

이 문서 자체는 향후 Java 수정, 빌드, JAR 배포, Minecraft 재실행, 재현, 커밋 또는
푸시를 승인하지 않는다. 2026-08-26 사용자의 각각의 명시적 요청에 따라 D0-D9
diagnostics-only 구현, clean forced build, 활성 인스턴스 JAR SHA-256 확인과 Minecraft
runtime 로그 확인이 수행됐다. 이번 갱신에는 사용자가 승인한 B0.1 동작 소스와 focused
test source 구현 결과를 포함한다. 이후 별도 사용자 승인으로 clean forced build와
focused test, CurseForge 배포, Minecraft 재현, 커밋 및 푸시를 완료했다. 현재 문서는
그 runtime 결과와 후속 lifecycle 재검수의 `FAIL` 판정을 함께 기록한다. 이번 문서
갱신은 Java 수정, 추가 빌드, 재배포, Minecraft 실행, 커밋 또는 푸시를 수행하거나
승인하지 않는다.

## 1. 관련 기준 문서

이 문서는 다음 문서를 대체하지 않는 날짜별 실행 계획이다.

- [@deposit_all B0 parity plan](chatclef-deposit-all-b0-parity-plan-2026-08-26.md)
- [Bare deposit diagnostics-only plan](chatclef-bare-deposit-diagnostics-plan.md)
- [Bare deposit container handoff investigation](chatclef-bare-deposit-container-handoff-loop-investigation.md)
- [Task lifecycle diagnostics](chatclef-task-lifecycle-diagnostics.md)
- [Baritone cache troubleshooting](chatclef-baritone-cache-troubleshooting.md)
- [ChatClef / Carry On integration direction](chatclef-carryon-integration-direction.md)

정식 event schema와 bounded logging 원칙은
`chatclef-bare-deposit-diagnostics-plan.md`가 소유한다. 이 문서는 2026-08-26
`@deposit_all` 재현으로 새로 확인된 증거, 현재 구현과 정식 설계의 차이, 그리고
최소 보강 순서만 소유한다.

## 2. 보호 경계

향후 진단 보강에서도 기존 `@deposit` 동작 경로는 보존한다.

의도적으로 수정하지 않을 원본 파일:

```text
adris/altoclef/commands/DepositCommand.java
adris/altoclef/tasks/container/StoreInAnyContainerTask.java
```

동작 변경 금지 범위:

```text
아이템 선택 조건
컨테이너 검색 조건과 순서
거리 상수 50/70
던전 상자 판정
BlockScanner unreachable 판정
상자 획득, 제작 및 배치 분기
Task 선택, equality, replacement, completion
retry, timeout, cooldown 및 fallback
Baritone goal, path 및 process ownership
input 상태와 cleanup
컨테이너 click 및 slot transfer
예외 전파와 terminal 판정
bridge wire payload와 command lifecycle
```

진단 구현이 필요해도 먼저 새 `DepositAllCommand`, 새 `DepositAllTask`, 그리고
LAVI-owned diagnostics helper 안에서 해결한다. 공용 upstream 경계가 꼭 필요한
경우에는 별도의 최소 diagnostics-only hunk로 분리하고, 그 필요성을 다시 보고한
뒤 승인받는다.

## 3. 로그 증거

확인한 활성 인스턴스 로그:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\instance_audit.txt
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
```

2026-08-26 00:51 KST 확인 시점의 크기:

| 파일 | 크기 |
| --- | ---: |
| `latest.log` | 61,668,564 bytes |
| `instance_audit.txt` | 2,139,939 bytes |
| `stdout-logs.txt` | 70,250,496 bytes |

로그는 0 byte가 아니며 충분한 runtime 기록을 포함한다. 동시에 현재 출력량이 이미
매우 크므로, 새 진단은 per-tick 출력이 아니라 operation별 상태 변화와 집계만
허용한다.

### 3.1 바다 비수렴 operation

```text
storeOperationId: store-deposit-25978
root identity:    34aa32d9
root class:       adris.altoclef.tasks.container.DepositAllTask
start:            00:34:01, latest.log line 29537
terminal:         explicit StopCommand, approximately 00:44:49
duration:         647,751 ms
transfer:         observed count 0
```

시작 위치는 약 `-628.431/61.669/106.500`이었다. 요청 대상은 다음 7개 그룹이었다.

```text
cobblestone x84
moss_block x13
cobbled_deepslate x19
andesite x4
azalea x1
flowering_azalea x1
sand x6
```

관측된 반복:

```text
OBTAIN_CHEST
  -> wood pursuit around -691,64,120

OPEN_EXISTING
  -> parent raw candidate -679,59,105
  -> child filtered result -533,50,126

위 경로가 반복되고 실제 transfer 경계에는 도달하지 않음
```

`STORE_IN_ANY_CONTAINER_PROGRESS_DIAGNOSTIC_CAP_REACHED`는 00:35:05,
`latest.log` line 31789에서 `session cap=256`으로 발생했다. 따라서 이후 같은
Minecraft 세션의 operation은 이 legacy progress detail을 충분히 받을 수 없다.

### 3.2 육지 정상 완료 operation

```text
storeOperationId: store-deposit-53935
root identity:    1d922c41
root class:       adris.altoclef.tasks.container.DepositAllTask
start:            00:45:12, latest.log line 50017
terminal:         NATURAL_FINISH, latest.log line 50750
duration:         32,596 ms
```

시작 위치는 약 `-713.521/67/98.638`이었으며 바다 operation과 같은 7개 대상
그룹을 사용했다. 이 operation은 가까운 나무를 획득한 뒤 상자를 제작하고,
`-770,66,125`에 배치한 상자로 7개 대상 그룹을 이동한 뒤 자연 완료됐다.

주요 증거:

```text
00:45:40  PLACE_CONTAINER_NEARBY
00:45:41  target container -770,66,125
00:45:41  cobblestone transfer selected
00:45:42  moss_block and cobbled_deepslate transfer selected
00:45:43  andesite transfer selected
00:45:44  azalea and flowering_azalea transfer selected
00:45:45  sand transfer selected
00:45:45  NATURAL_FINISH
```

terminal aggregate:

```text
parentCandidateDecisionCount=652
filteredSearchResultCount=361
pursuitDecisionCount=923
craftRouteEventCount=33
transferDecisionCount=82
exceptionObservationCount=0
```

이 결과는 `@deposit_all` 등록, 새 root Task 실행, 상자 획득/제작/배치,
`StoreInContainerTask` 진입, 전송 선택 및 자연 완료 경로가 실제 runtime에서
작동함을 증명한다.

## 4. 증명된 것과 미증명인 것

### 증명됨

1. `@deposit_all`은 `DepositAllCommand`에서 `DepositAllTask`를 실행한다.
2. 같은 대상 목록으로 바다에서는 비수렴했고 육지에서는 자연 완료됐다.
3. 바다 operation은 `OBTAIN_CHEST`와 `OPEN_EXISTING` 계열 경로를 반복했다.
4. 바다의 parent raw 위치와 child filtered 위치는 서로 달랐다.
5. 육지에서는 상자 재료 획득과 배치가 끝난 뒤 실제 transfer 결정이 이어졌다.
6. 두 operation 모두 해당 Store 진단에서 exception은 관측되지 않았다.

### 아직 미증명

1. 바다의 `-679,59,105` 후보가 Store predicate에서 탈락했는지 여부와 정확한
   탈락 사유
2. fallback tick에서 scanner가 실제로 후보를 반환하지 않았는지, 후보가 있었지만
   거리 조건에서 탈락했는지 여부
3. raw candidate와 filtered result 차이를 만든 첫 scanner/predicate 경계
4. 상자 획득 child가 교체될 때의 정확한 branch epoch와 교체 사유
5. Baritone generation의 생성, 완료 및 adoption이 이 반복의 최초 원인인지 여부
6. container slot delta를 근거로 한 durable effect 검증
7. 동일한 시작 상태에서 bare `@deposit`과 bare `@deposit_all`의 완전한 runtime
   parity

## 5. 현재 로그 해석 보정

### 5.1 fallback의 `rawClosestContainerPresent=false`

현재 `DepositAllTask`는 한 번 계산한 local `closest`를 branch 판단에는 사용하지만,
`PLACE_CONTAINER_NEARBY`와 `OBTAIN_CHEST` 진단 호출에는 `rawClosest=null`을
전달한다.

따라서 현재 fallback event의 다음 값은 scanner absence를 증명하지 않는다.

```text
rawClosestContainerPresent=false
rawClosestContainerPosition=unavailable
```

실제 의미는 현재 구현상 다음 둘을 구분하지 못하는 `UNAVAILABLE`이다.

```text
scanner가 후보를 반환하지 않음
scanner 후보는 있었지만 50/70 거리 조건이 branch를 열지 못함
```

이 구분이 첫 번째 보강 대상이다. 진단을 위해 `getNearestBlock()`을 다시 호출하면
안 되고, branch를 결정한 기존 local `closest`와 boolean을 그대로 전달해야 한다.

### 5.2 명령 이름 오표기

현재 `DepositAllCommand`가 기존 `DepositCommandDiagnostics`를 그대로 호출하므로
로그에는 다음처럼 기록된다.

```text
commandName=deposit
requestSource=BARE_DEPOSIT_COMMAND
taskClass=adris.altoclef.tasks.container.DepositAllTask
callerSummary=adris.altoclef.commands.DepositAllCommand#call:117 ...
```

실제 실행 명령은 class와 caller로 확인되지만 명시 필드는 잘못됐다. 향후에는
다음 값으로 구분한다.

```text
commandName=deposit_all
requestSource=BARE_DEPOSIT_ALL_COMMAND
```

기존 `@deposit`은 계속 `deposit`과 `BARE_DEPOSIT_COMMAND`를 사용해야 한다.

### 5.3 progress cap과 operation 간 간섭

현재 두 제한이 session 전역으로 동작한다.

```text
StoreInAnyContainerProgressDiagnostics: session cap 256
StoreDepositDetailBudget: event별 key cap 256과 session detail cap
```

긴 바다 operation이 cap을 먼저 소비해 뒤의 육지 operation에는
`STORE_IN_ANY_CONTAINER_PROGRESS_STATE`가 남지 않았고, lifecycle/pursuit detail도
앞 operation의 key 사용량에 영향을 받았다. terminal aggregate는 남았지만 두
operation의 동일한 경계를 공정하게 비교하기 어렵다.

### 5.4 `effectVerified=false`

육지 operation은 transfer 결정과 `NATURAL_FINISH`를 남겼지만 terminal summary는
다음과 같이 기록됐다.

```text
effectObservationCount=0
effectVerified=false
effectVerificationAuthority=NOT_VERIFIED_NO_DURABLE_ORACLE
```

이는 육지 실행 실패를 뜻하지 않는다. 현재 durable effect oracle이 구현되지
않았다는 coverage gap이다. 바다 반복의 최초 원인을 찾는 데 필수는 아니므로
첫 보강 slice에는 포함하지 않는다.

## 6. 진단 목표

다음 재현 한 번으로 아래 질문에 답할 수 있어야 한다.

```text
1. root가 실제로 받은 raw candidate는 무엇이었는가?
2. raw candidate가 어떤 range 결과로 어느 branch를 선택했는가?
3. filtered scan은 몇 개 후보를 평가했고 Store predicate가 왜 거절했는가?
4. raw와 filtered target의 관계는 무엇인가?
5. branch 변경 시 이전 child와 다음 child는 무엇이며 실제 교체됐는가?
6. 상자 획득 경로는 어디까지 진행된 뒤 교체됐는가?
7. 마지막 성공 경계와 첫 실패 또는 첫 미관측 경계는 어디인가?
```

모든 event는 같은 `storeOperationId`에 묶고, candidate 결정과 실제 child 수명을
구분하기 위해 다음 sequence를 사용한다.

```text
candidateDecisionSequence
branchEpoch
filteredSearchId
storeAttemptId
childLifecycleId
```

Task identity는 payload의 보조 정보로만 남긴다. dedupe key나 무제한 map key로
사용하지 않는다.

## 7. 보강 순서

각 slice는 별도로 구현하고 검증한다. 앞 slice가 원인을 충분히 증명하면 뒤
slice는 추가하지 않는다.

### D0. 명령 attribution과 operation budget

목표:

```text
deposit과 deposit_all을 로그에서 즉시 구분
긴 operation이 다음 operation의 detail을 고갈시키지 않음
terminal summary reserve는 계속 session hard cap 아래 유지
```

계획:

1. 기존 `DepositCommandDiagnostics.logInvocation(...)` signature는 유지한다.
2. 실제 command name과 request source를 받는 overload를 추가한다.
3. 기존 `DepositCommand` 호출은 변경하지 않고 기존 overload가 `deposit`을
   기본값으로 사용하게 한다.
4. `DepositAllCommand`만 새 overload에 `deposit_all`과
   `BARE_DEPOSIT_ALL_COMMAND`를 전달한다.
5. noncritical detail은 operation별 hard cap과 family sub-cap을 갖게 한다.
6. session 전체 cap `5000`과 critical reserve `64`는 유지한다.
7. operation budget state는 natural finish 또는 true root stop에서 제거한다.

정식 budget 계약:

```text
operation detail hard cap: 256
parent/filtered/pursuit family: 44
child reconciliation family: 44
craft route family: 44
container access/transfer/effect family: 44
Baritone correlation family: 44
root/descendant lifecycle family: 16
```

legacy `STORE_IN_ANY_CONTAINER_PROGRESS_STATE`는 첫 slice에서 의미나 cap을 바꾸지
않는다. 새 Store operation checkpoint와 terminal aggregate를 authoritative
비교 자료로 사용한다.

### D1. parent decision truth preservation

대상은 새 `DepositAllTask.onTick()`의 이미 계산된 local 값이다.

추가 또는 보정할 field:

```text
closestEvaluated
rawClosestPresent
rawClosestPosition
rawClosestBlockType
rawClosestScannerSource=UNFILTERED_BY_STORE_PREDICATE
closestWithin50Evaluated
closestWithin50
currentChestTryBefore
currentTryWithin70Evaluated
currentTryWithin70
rawClosestEqualsCurrentTryByValue
selectedBranch
rangeDecisionOutcome
previousBranch
branchChanged
branchEpoch
notStoredStateHash
```

허용할 `rangeDecisionOutcome`:

```text
RAW_CLOSEST_WITHIN_50
CURRENT_TRY_WITHIN_70
BOTH_RANGE_CONDITIONS
NO_RAW_CLOSEST
RAW_PRESENT_BUT_OUTSIDE_RANGES
NOT_EVALUATED_EARLY_GET_MISSING_TARGET
```

구현 원칙:

```text
기존 getNearestBlock() 결과를 한 번만 사용
기존 boolean을 logging과 branch 양쪽에서 동일하게 사용
fallback event에도 null placeholder 대신 실제 local closest 전달
branch 조건과 순서 변경 금지
추가 scanner 또는 predicate 호출 금지
```

즉시 detail은 첫 결정, branch 변경, raw 위치 변경, 50/70 결과 변경에서만
방출한다. 나머지는 operation counter에만 반영한다.

### D2. filtered predicate rejection aggregate

첫 구현 범위는 Store의 `validContainer`가 이미 평가하는 사유로 제한한다.

허용할 결과:

```text
ACCEPTED
CHEST_ABOVE_BLOCKED_UNBREAKABLE
CONTAINER_CACHE_FULL
CACHED_DUNGEON_CHEST
SPAWNER_NEAR_CHEST
UNKNOWN
```

`STORE_CONTAINER_FILTERED_SEARCH_RESULT`에 추가할 aggregate:

```text
filteredSearchId
originatingParentDecisionSequence
originatingParentRawClosestPosition
candidateEvaluationCount
predicateAcceptedCount
predicateRejectedCount
rejectionCountsByReason
firstRejectedPosition
firstRejectedReason
lastRejectedPosition
lastRejectedReason
filteredResultPosition
rawAndFilteredRelation
scannerFilterDetailAvailable
scannerFilterCoverageGap
```

허용할 relation:

```text
SAME_POSITION
DIFFERENT_POSITION
RAW_PRESENT_FILTERED_NONE
RAW_NONE_FILTERED_PRESENT
BOTH_NONE
```

predicate의 기존 short-circuit 위치에서 이미 결정된 reason만 collector에
전달한다. 탈락 이유를 얻기 위해 조건을 다시 평가하거나 world/cache를 다시
조회하지 않는다.

`BlockScanner` 내부의 block-state mismatch와 unreachable 제외는 첫 slice에서
추측하지 않는다. Store predicate aggregate만으로 원인이 밝혀지지 않을 때에만
별도 `STORE_BLOCK_SCANNER_FILTER_SUMMARY`를 검토한다.

filtered scan의 시작과 종료를 정확히 묶기 위해 공용
`DoToClosestBlockTask.getClosestTo()` 관측이 꼭 필요하다면, 다음 조건의 단일
diagnostics-only hunk로 분리한다.

```text
DepositAll operation에만 활성화
기존 scanner call 주변에서 collector scope begin/end만 수행
return value와 exception 전파 유지
per-candidate log 금지
getNearestBlock() 또는 predicate 추가 호출 금지
isValid() pursuit 검증은 첫 hunk에서 수정하지 않음
```

이 공용 hunk는 이 문서만으로 승인되지 않는다. D1 로그만으로 답이 나오지 않는지
먼저 확인한 뒤 정확한 method와 diff hunk를 다시 보고한다.

### D3. branch와 child handoff

기존 `STORE_TASK_CHILD_RECONCILIATION`을 재사용하고 다음 correlation만 보강한다.

```text
originatingBranchEpoch
candidateDecisionSequence
previousBranch
nextBranch
activeChildClassBefore
candidateChildClass
activeChildClassAfter
subTasksEqual
canInterruptEvaluated
canInterrupt
replacementApplied
previousChildStopCalled
reconciliationRole
```

현재 dedupe key의 Task instance identity 의존을 제거하고 다음 semantic key를
사용한다.

```text
storeOperationId
+ reconciliationRole
+ previousBranch
+ nextBranch
+ previous child class
+ next child class
+ semantic outcome
```

상자 획득 진행은 이미 계산된 값만 사용해 다음처럼 관측한다.

```text
fallbackContainerItemPresent
requestedContainerItem
active acquisition child class
current pursuit target and player distance when already available
OBTAIN_CHEST -> PLACE_CONTAINER_NEARBY transition
```

진단을 위해 원목, 판자 또는 recipe를 새로 검색하지 않는다. 현재 관측값만으로
부족할 때는 coverage gap으로 남긴다.

### D4. checkpoint

긴 반복에서 detail cap 이후에도 전체 흐름을 잃지 않도록 정식 설계의
`STORE_DEPOSIT_CHECKPOINT_SUMMARY`를 사용한다.

```text
interval: 1200 client ticks
maximum: 64 per operation
unchanged aggregate면 skip
```

최소 field:

```text
elapsedTicks
currentBranch
branchCountsSinceLastCheckpoint
branchTransitionCountsSinceLastCheckpoint
currentRawCandidate
currentFilteredCandidate
currentPursuit
childReplacementCountSinceLastCheckpoint
notStoredStateHash
transferDecisionCountSinceLastCheckpoint
lastSuccessfulBoundary
firstExplicitFailureBoundary
firstUnobservedBoundary
detailSuppressedCountsByFamily
```

checkpoint는 timeout, retry, stop 또는 recovery를 수행하지 않는다.

### D5. 성공 effect oracle

바다 반복 원인이 D1-D4로 확인된 뒤 별도 slice로 검토한다. 목표는 육지처럼
명백히 완료된 실행에서 `effectVerified=false`만 남는 혼동을 줄이는 것이다.

필요 증거:

```text
요청 target별 시작 player inventory count
target container와 연결된 실제 slot delta
요청 target별 terminal player inventory count
ContainerStoredTracker가 인정한 positive delta
관측 누락 또는 overflow 여부
```

단순한 click 요청, transfer Task 선택 또는 `NATURAL_FINISH`만으로 durable effect를
꾸며내지 않는다. 완전한 oracle이 없으면 `PARTIAL` 또는 `UNAVAILABLE`로 유지한다.

### D6. Baritone context

D1-D4가 candidate handoff를 설명하지 못할 때만 검토한다.

```text
generation id
goal owner and target
calculation start/result
path adoption outcome
replacement or cancellation owner
late completion after branch change
```

일반 Baritone per-tick 출력, 전체 path node 목록, 전역 goal cancellation 또는
recovery behavior는 추가하지 않는다.

## 8. 책임 분리

기존 624-line `StoreDepositDiagnostics` facade에 모든 책임을 더 넣지 않는다.
향후 구현 시 새 책임은 기존 package 구조 안에서 분리한다.

예상 LAVI-owned 구조:

```text
lavi/minecraft/diagnostics/command/deposit/
    DepositCommandDiagnostics
        invocation emission only
    DepositCommandDiagnosticFields
        field assembly only

lavi/minecraft/diagnostics/container/store/deposit/candidate/
    StoreContainerCandidateDiagnostics
        candidate observation lifecycle and bounded emission only
    StoreContainerCandidateObservation
        one immutable aggregate snapshot only
    StoreContainerCandidateRejectionReason
        fixed semantic reason set only

lavi/minecraft/diagnostics/container/store/deposit/budget/
    StoreDepositOperationDetailBudget
        per-operation family budgets only
    StoreDepositCriticalBudget
        session terminal/exception/control reserve only
    StoreDepositEmissionGate
        delegation and summary projection only

lavi/minecraft/diagnostics/container/store/deposit/context/
    StoreDepositOperationContext
        immutable identity and request source only
    StoreDepositOperationState
        bounded counters and current semantic state only
```

진단 facade는 delegation만 수행한다. candidate reason 판정, budget 소유, field
formatting, operation identity를 한 class에 합치지 않는다.

## 9. 예상 파일 범위

첫 diagnostics-only 구현에서 검토할 파일:

```text
새 B0 경로
    adris/altoclef/commands/DepositAllCommand.java
    adris/altoclef/tasks/container/DepositAllTask.java

LAVI-owned diagnostics
    lavi/minecraft/diagnostics/command/deposit/DepositCommandDiagnostics.java
    lavi/minecraft/diagnostics/command/deposit/DepositCommandDiagnosticFields.java
    lavi/minecraft/diagnostics/container/store/deposit/StoreDepositDiagnostics.java
    lavi/minecraft/diagnostics/container/store/deposit/context/StoreDepositOperationContext.java
    lavi/minecraft/diagnostics/container/store/deposit/context/StoreDepositOperationState.java
    lavi/minecraft/diagnostics/container/store/deposit/event/StoreDepositEventFields.java
    lavi/minecraft/diagnostics/container/store/deposit/budget/**
    lavi/minecraft/diagnostics/container/store/deposit/candidate/**
```

조건부 공용 upstream 관측 지점:

```text
adris/altoclef/tasks/DoToClosestBlockTask.java
    getClosestTo()의 기존 scanner call 주위 한 hunk만 후보
```

첫 slice에서 의도적으로 수정하지 않을 파일:

```text
adris/altoclef/commands/DepositCommand.java
adris/altoclef/tasks/container/StoreInAnyContainerTask.java
adris/altoclef/tasks/container/StoreInContainerTask.java
adris/altoclef/tasks/ResourceTask.java
adris/altoclef/TaskCatalogue.java
adris/altoclef/tasksystem/Task.java
Baritone source
Fabric bridge protocol and dispatcher
Python command registry
```

구현 직전에는 이 예상 목록을 실제 diff 단위로 다시 축소해 보고한다. D0-D1에
필요하지 않은 파일은 미리 수정하지 않는다.

## 10. bounded logging 계약

허용:

```text
operation 시작과 terminal
첫 semantic state
branch, target, reason 또는 result 변경
첫 explicit failure
주기적 changed-only checkpoint
bounded terminal aggregate
고정된 reason별 first/last sample
```

금지:

```text
매 tick 같은 상태 출력
후보마다 한 줄 출력
Task instance마다 새 dedupe bucket 생성
전체 inventory, NBT 또는 path node dump
반복 stack trace
timestamp, tick, UUID 또는 identity를 dedupe 핵심값으로 사용
진단을 위한 scanner/predicate 재호출
진단용 catch로 engine exception을 삼키거나 변환
cap 도달을 gameplay terminal로 해석
```

representative sample은 family별 최대 16개로 제한한다. payload 문자열은 기존
정식 설계의 길이 제한을 따른다. detail 방출 여부와 무관하게 bounded fixed
counter는 먼저 갱신하고 terminal summary에 투영한다.

## 11. 재현 계획

향후 Java 변경, 빌드 및 Minecraft 실행을 각각 별도로 승인받은 뒤 수행한다.

### 사전 조건

```text
Minecraft 완전 종료
활성 인스턴스와 world 확인
world가 복사/복원됐다면 Baritone cache 절차 선행
clean forced build
배포 JAR SHA-256 일치 확인
중복 ChatClef JAR 없음 확인
새 Minecraft 세션에서 diagnostics BOUNDARY 활성화
```

### 시나리오 A: 바다

```text
상자 아이템 없음
접근 가능한 나무가 가까이에 없음
기록된 7개 target과 같은 inventory 상태
기록된 바다 위치 또는 동등한 fixture
bare @deposit_all 한 번 실행
branch 왕복이 2회 이상 관측되면 stop command로 종료
```

10분 이상 방치할 필요는 없다. 필요한 state transition과 rejection reason이
확보되면 즉시 종료한다.

### 시나리오 B: 육지

```text
같은 target inventory 복원
상자 아이템 없음
가까운 나무가 있는 육지 fixture
bare @deposit_all 한 번 실행
natural finish까지 관측
```

operation별 budget이므로 A와 B를 같은 Minecraft 세션에서 실행해도 각 operation의
필수 detail과 terminal summary가 남아야 한다.

### 시나리오 C: 기존 명령 회귀 확인

```text
동일한 시작 fixture 복원
bare @deposit 한 번 실행
기존 command attribution 유지
기존 branch와 결과 유지
```

이 시나리오는 새 진단이 기존 `@deposit` 동작을 바꾸지 않았음을 확인한다. 완전한
B0 parity 판정은 별도의 동일 상태 A/B 비교 절차가 소유한다.

## 12. 합격 기준

다음 조건을 모두 만족해야 첫 진단 보강이 성공이다.

1. `deposit`과 `deposit_all`이 명시 필드에서 정확히 구분된다.
2. fallback event가 실제 raw candidate와 range 결과를 보존한다.
3. filtered result에 Store predicate 평가 수와 정확한 탈락 사유가 집계된다.
4. raw/filtered relation이 같은 operation과 decision sequence로 연결된다.
5. branch 변경과 child replacement가 같은 `branchEpoch`로 연결된다.
6. 긴 operation 뒤의 다음 operation도 자체 detail과 terminal summary를 받는다.
7. 반복 상태는 counter/checkpoint로만 남고 per-tick log가 추가되지 않는다.
8. original `DepositCommand.java`와 `StoreInAnyContainerTask.java` hash가 유지된다.
9. return value, branch, Task, retry, timeout, input, path 및 cleanup은 변하지 않는다.
10. 로그가 부족하면 `UNAVAILABLE` 또는 coverage gap으로 남고 원인을 추측하지
    않는다.

## 13. 중단 조건

다음 상황에서는 구현 범위를 넓히지 말고 멈춰서 보고한다.

```text
D1만으로도 원인이 충분히 증명됨
원본 DepositCommand 또는 StoreInAnyContainerTask 수정이 필요해 보임
두 개 이상의 upstream lifecycle owner에 진단 hunk가 필요함
predicate reason을 얻으려면 같은 조건을 다시 평가해야 함
scanner 내부 관측이 behavior 또는 exception 흐름을 바꿀 가능성이 있음
operation budget 분리가 generic diagnostics 의미를 바꿈
diagnostics-only 범위에서 timeout, retry 또는 fallback 변경이 필요해짐
```

원인이 증명되기 전에는 바다 전용 timeout, 거리 제한, 후보 blacklist, 강제 상자
제작, Task 고정 또는 Baritone cancellation을 구현하지 않는다.

## 14. 현재 결론

```text
B0 실행 가능성:
    runtime에서 확인됨

육지 성공:
    상자 획득/제작/배치, transfer 선택, NATURAL_FINISH 확인

바다 비수렴:
    raw/filtered 위치 불일치, 50-block 경계 부근 branch 왕복,
    실제 root child 교체와 transfer 0회 확인

정확한 최초 실패 원인:
    아직 미확정

현재 소스 단계:
    D0-D4 diagnostics-only 구현 완료
    clean forced build 및 test 성공
    활성 인스턴스 JAR hash 일치 확인
    D0-D4 runtime 재현 및 로그 검토 완료

다음 최소 단계:
    D7 exact raw candidate verdict
    D8 delayed GUI window와 Store operation correlation
    D9 50/70 range crossing과 child interruption correlation
    clean JVM 재현 후에도 경계가 남을 때만 D6 Baritone context 검토
    D5 durable effect oracle은 비수렴 원인 확인 뒤 검토
```

이 순서를 지키면 기존 `@deposit` 원본을 건드리지 않고, `@deposit_all`의 바다
비수렴을 행동 변경 없이 좁힐 수 있다.

## 15. 2026-08-26 구현 상태

현재 구현한 범위:

```text
D0 command attribution: IMPLEMENTED
D0 per-operation detail budget: IMPLEMENTED
D1 parent raw/range/branch truth: IMPLEMENTED
D2 Store predicate rejection aggregate: IMPLEMENTED
D3 branch and child handoff correlation: IMPLEMENTED
D4 changed-only 1200-tick checkpoint: IMPLEMENTED
D5 durable effect oracle: NOT IMPLEMENTED
D6 conditional Baritone generation/goal/path diagnostics: NOT IMPLEMENTED
build/test execution: PASSED, run_id=20260826-012347
build outcome: BUILD_PASSED_ACTIVE_JAR_MATCHED_RUNTIME_REPRODUCED
active JAR SHA-256: 5BEB70FDD6D1BFD62D32BE968A00A440FE4A7C5EE12B64F1470493821954F55E
Minecraft runtime reproduction: RUN BY USER, LOGS REVIEWED
root-cause status: NOT PROVEN
D7-D9 implementation: NOT RUN, DOCUMENTED ONLY
commit/push in this documentation update: NOT RUN
```

구현 경계:

```text
DepositCommand.java: unchanged
StoreInAnyContainerTask.java: unchanged
DepositAllCommand.java: command attribution only
DepositAllTask.java: existing branch values and predicate outcomes are observed only
DoToClosestBlockTask.getClosestTo(): one diagnostics-only begin/end scope around the existing scanner call
candidate/: aggregate observation, route state, field projection
budget/: operation budget and session critical-reserve delegation
```

공용 scanner hunk는 새 검색을 수행하지 않는다. 기존 `getNearestBlock(...)` 호출을
그대로 한 번 실행하고, `BARE_DEPOSIT_ALL_COMMAND`의 `OPEN_EXISTING` 직계 route child일
때만 Store predicate 집계 scope를 연다. 나무 획득 등 같은 operation의 다른 descendant
검색은 컨테이너 filtered 결과로 집계하지 않는다.

정적 검증 당시 원본 보호 해시:

```text
DepositCommand.java:
    D4D598C6D1F2F3465A18F0A150B8294F621F3248C35DBCD97896AC75209F09B5
StoreInAnyContainerTask.java:
    7F54FE3FACE7DE1D5DFE8D45B9330A97B3B0AD80A47A7071EF4F1CBEDBC612BC
```

## 16. D0-D4 적용 후 runtime 증거

이 절은 3절의 초기 재현과 별개의 후속 실행이다. 확인한 활성 인스턴스는 계속
다음 경로다.

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01
```

`latest.log`, `stdout-logs.txt`, `instance_audit.txt`는 모두 0 byte가 아니었다.
`latest.log`와 `stdout-logs.txt`에서는 아래 두 operation에 대해 같은 핵심 경계가
확인됐다. 다른 CurseForge 인스턴스의 관련 로그는 2026년 7월 시점의 이전 기록이어서
이번 판정에 사용하지 않았다.

### 16.1 활성 JAR 확인

활성 인스턴스에 배치된 JAR과 clean forced build 산출물의 SHA-256은 같았다.

```text
5BEB70FDD6D1BFD62D32BE968A00A440FE4A7C5EE12B64F1470493821954F55E
```

따라서 아래 로그는 적어도 확인한 D0-D4 빌드와 다른 JAR에서 나온 결과로 해석하지
않는다. 다만 JAR 일치는 runtime 동작의 정당성이나 최종 root cause를 증명하지는
않는다.

### 16.2 정상 완료 operation

```text
storeOperationId:              store-deposit-882
start:                         01:29:26 KST
start player position:         approximately -764.212/64/133.657
requested target groups:       7
transfer decision count:       76
terminal:                      NATURAL_FINISH
duration:                      5,275 ms
effectVerified:                false
effectVerificationAuthority:   NOT_VERIFIED_NO_DURABLE_ORACLE
```

이 operation에서는 raw와 filtered candidate가 모두 `-770,66,125`였고 실제 transfer
결정 뒤 자연 완료됐다. `effectVerified=false`는 D5가 아직 구현되지 않았다는 뜻이며,
이 로그만으로 저장 실패를 뜻하지 않는다.

### 16.3 장기 비수렴 operation

```text
storeOperationId:              store-deposit-43622
start:                         01:44:40 KST
start player position:         approximately -806.484/-13/28.499
requested target groups:       14
last reviewed checkpoint:      11 at 01:55:40 KST
elapsed ticks:                 13,200
candidate decisions:           13,200
branchEpoch:                   493
transfer decision count:       0
terminal observed:             no
notStoredStateHash:            6b83d6c7
```

초기 경계는 다음 순서였다.

```text
raw -861,-23,48 outside 50
    -> OBTAIN_CHEST

chest item acquired
    -> PLACE_CONTAINER_NEARBY

new chest placed at -806,-19,27
    -> OPEN_EXISTING

interactResult=SUCCESS at the placed chest
expectedGuiOpened=false
screenHandlerChangedObserved=false
outcome=NO_GUI_TARGET_STILL_PRESENT
```

그 뒤 raw candidate와 filtered candidate가 다음처럼 달라졌다.

```text
raw -861,-23,48    -> filtered -783,69,84
raw -807,-15,28    -> filtered -783,69,84
raw -770,66,125    -> filtered -783,69,84
raw -679,59,105    -> filtered -783,69,84
```

filtered scan의 집계된 탈락 사유는 주로 `CACHED_DUNGEON_CHEST`였고 일부
`CHEST_ABOVE_BLOCKED_UNBREAKABLE`가 섞였다. 첫 scan에는
`SPAWNER_NEAR_CHEST`도 있었다. 그러나 현재 D2는 전체 집계와 first/last sample만
남기므로, 위 raw 좌표 각각의 정확한 탈락 사유로 연결할 수 없다.

checkpoint 6에서는 raw와 filtered가 모두 `-783,69,84`였지만 transfer는 여전히
0회였다. 따라서 raw/filtered 불일치 하나만으로 모든 비수렴을 설명할 수 없다.
컨테이너 접근, click-to-GUI 경계 또는 child 수명도 함께 관측해야 한다.

마지막으로 검토한 checkpoint 11의 interval 집계는 다음과 같다.

```text
currentBranch:                              OPEN_EXISTING
branchCountsSinceLastCheckpoint:            {OPEN_EXISTING=847, OBTAIN_CHEST=353}
branchTransitionCountsSinceLastCheckpoint:  {OPEN_EXISTING->OBTAIN_CHEST=50,
                                              OBTAIN_CHEST->OPEN_EXISTING=50}
currentRawCandidate:                        -679,59,105
currentFilteredCandidate:                   -783,69,84
childReplacementCountSinceLastCheckpoint:   99
transferDecisionCountSinceLastCheckpoint:   0
```

같은 시점의 player 위치는 약 `-628.747/62.630/102.500`이었다. raw
`-679,59,105`와의 거리는 50-block 판정 경계 부근이다. player 이동과 함께
`OPEN_EXISTING`과 `OBTAIN_CHEST`가 왕복하고 실제 root child 교체가 이어졌다는
상관관계는 확인됐다. 이 상관관계가 최초 원인인지, 다른 실패가 만든 결과인지는
아직 증명되지 않았다.

01:44:50부터 01:55:45 사이에는 `mine_or_collect_stop`이 250회 관측됐다. 이는
상자 재료 획득 descendant가 반복적으로 중단된 정황과 일치하지만, 각 stop을 정확한
range crossing과 묶는 operation-local 필드는 아직 없다.

### 16.4 진단 예산 평가

장기 operation의 D0-D4 관련 detail은 `latest.log`와 `stdout-logs.txt`에서 각각 약
118줄, checkpoint 11개, transfer 0개, terminal 0개였다. 13,200번의 parent 결정이
있었지만 candidate family cap 44와 checkpoint가 동작해 새 D0-D4 event가 매 tick
출력되지는 않았다.

전체 로그 파일이 매우 큰 주된 이유는 기존 `VISIBLE_TASK_DECISION`,
`VISIBLE_TASK_RETURN`, `VISIBLE_TASK_LIFECYCLE` 계열의 고빈도 출력이다. 이는
`@deposit_all` operation budget과 별개의 기존 진단 출력 문제다. 다음 D7-D9에
그 정리 작업을 섞지 않는다.

### 16.5 같은 JVM의 resource matching 오염 가능성

두 번째 operation 전에 같은 Minecraft JVM에서 다음 예외 계열이 관측됐다.

```text
time:       01:39:42 KST
context:    earlier get diamond_pickaxe resource path
classes:    BlockOptionalMeta / ExecutionException / NullPointerException
result:     Minecraft process did not crash
```

`chatclef-baritone-cache-troubleshooting.md`에 따르면 이 예외는 같은 JVM의 static
drop-matching cache를 오염시킬 수 있다. 따라서 두 번째 operation의
`OBTAIN_CHEST`와 resource acquisition 반복은 독립적인 확정 증거로 사용할 수 없다.
Minecraft를 완전히 종료한 clean JVM 재현이 먼저 필요하다. world를 복사하거나
복원한 사실이 없다면 이 이유만으로 Baritone disk cache를 삭제하거나 변경하지
않는다.

## 17. 현재 증명 수준

### 17.1 증명됨

1. D0 command attribution은 `deposit_all`을 정확히 구분한다.
2. operation별 budget은 긴 실행 뒤에도 다른 operation의 detail을 보존한다.
3. 비수렴 operation은 13,200 parent 결정 동안 transfer에 한 번도 진입하지 않았다.
4. 50-block 경계 부근에서 `OPEN_EXISTING`과 `OBTAIN_CHEST`가 반복됐다.
5. branch 왕복과 같은 interval에 실제 root child replacement가 반복됐다.
6. raw candidate와 filtered candidate는 여러 구간에서 달랐다.
7. raw와 filtered가 같은 checkpoint에서도 transfer 0회였으므로 후보 불일치만으로는
   전체 현상을 설명할 수 없다.
8. placed chest click은 `SUCCESS`를 반환했지만 즉시 GUI 또는 screen-handler 변화가
   관측되지 않았다.
9. D0-D4 로그 자체는 operation cap과 changed-only checkpoint를 지켰다.

### 17.2 진단 구현에서 확인된 coverage defect

`CarryOnContainerPostconditionWindow`는 원래 offset `+1`, `+2`, `+5` tick을
관측하도록 작성돼 있다. 그러나 현재 `start()`는 return snapshot을 다음 의미로
분류한다.

```text
classify(..., terminalOffset=true)
```

따라서 offset 0에서 GUI가 없고 target이 그대로 있으면 즉시
`NO_GUI_TARGET_STILL_PRESENT` terminal이 되고 후속 window를 등록하지 않는다.
현재 로그의 해당 outcome은 5-tick 관측 종료 결과가 아니라 return 직후 결과다.

이는 게임 동작의 root cause가 아니라 지연 GUI 증거를 잃는 diagnostics-only 결함이다.
소스상 후속 tick callback 등록은 이미 존재하므로, 다음 구현에서는 이 한 경계를 먼저
바로잡고 기존 window를 재사용한다.

### 17.3 아직 미증명

1. raw `-679,59,105` 등 개별 raw candidate가 Store predicate에서 평가됐는지와
   평가됐다면 정확히 어떤 reason으로 탈락했는지
2. placed chest GUI가 offset `+1`, `+2`, `+5` 중 늦게 열렸는지
3. interaction 뒤 branch나 route child가 바뀌어 GUI 대기 경계를 잃었는지
4. 50/70 판정 경계가 모든 child interruption의 직접 trigger였는지
5. 컨테이너까지의 실제 path/access 실패가 있었는지
6. 같은 JVM의 `BlockOptionalMeta` 예외가 상자 재료 획득에 영향을 줬는지
7. durable container slot delta
8. 정확한 마지막 성공 경계, 첫 실패 경계와 최종 root cause

결론은 계속 `ROOT_CAUSE_NOT_PROVEN`이다. 현재 증거로 dungeon cache, Baritone,
Carry On, 거리 조건 또는 GUI 지연 중 하나를 단독 원인으로 확정하지 않는다.

## 18. 다음 bounded diagnostics-only slice

구현 순서는 D7, D8, D9다. 한 slice가 정확한 최초 실패 경계를 증명하면 뒤 slice는
추가하지 않는다. D5와 D6는 아래 조건을 충족할 때까지 보류한다.

### D7. exact raw candidate verdict

현재 collector는 filtered scan 시작 시 이미 parent raw position을 보관하고,
`validContainer`가 실제로 평가한 각 position과 이미 결정된 reason을 받는다. 새 검색이나
predicate 재평가 없이 같은 scan 안에서 raw position과 일치하는 평가만 집계할 수 있다.

기존 `STORE_CONTAINER_FILTERED_SEARCH_RESULT`에 다음 필드를 추가하는 방향으로 제한한다.

```text
rawCandidateEvaluationObserved
rawCandidateEvaluationOrdinal
rawCandidatePredicateOutcome
rawCandidateRejectionReason
rawCandidateMatchedByValue
rawCandidateObservationCount
rawCandidateCoverage
```

허용할 outcome:

```text
RAW_ACCEPTED
RAW_REJECTED
RAW_NOT_VISITED_BY_FILTERED_SCAN
RAW_UNAVAILABLE
FILTERED_SCAN_DID_NOT_COMPLETE
```

규칙:

```text
BlockPos value equality로만 raw match 판정
validContainer의 기존 short-circuit reason을 그대로 사용
predicate 조건, dungeon cache, world 또는 scanner 재조회 금지
candidate loop 안에서 event 직접 방출 금지
scan 종료 시 aggregate 한 번만 projection
raw reason 또는 raw position이 바뀔 때만 detail 방출
```

`RAW_ACCEPTED`인데 filtered result가 다르면 Store predicate가 아닌 scanner 내부
filter/ranking 경계가 다음 관측 대상이다. `RAW_NOT_VISITED_BY_FILTERED_SCAN`이면 raw
탈락 사유를 꾸며내지 않고 scanner coverage gap으로 남긴다.

### D8. delayed GUI window와 Store operation 연결

#### D8.1 기존 postcondition window 정상화

새 관측기를 만들지 않고 기존 `CarryOnContainerPostconditionWindow`를 사용한다.
제안하는 diagnostics-only 의미는 다음과 같다.

```text
offset 0 immediate GUI or confirmed pickup
    -> terminal outcome

offset 0 no GUI and no pickup
    -> OBSERVATION_PENDING
    -> existing +1/+2/+5 observation window 등록

offset +1/+2 state change
    -> changed-only snapshot

offset +5
    -> GUI_OPEN_DELAYED, NO_GUI_TARGET_STILL_PRESENT,
       TARGET_REMOVED_WITHOUT_GUI 또는 OBSERVATION_WINDOW_EXPIRED
```

이는 timeout, retry 또는 Task 대기를 추가하는 계획이 아니다. diagnostics window의
분류와 방출만 고치며 `InteractWithBlockTask.isFinished()`, click 결과, input,
screen, route child 및 TaskRunner에는 영향을 주지 않는다.

새 Minecraft interaction event를 만들지 않고 다음 기존 event를 재사용한다.

```text
CONTAINER_OPEN_ATTEMPT_OBSERVED
CONTAINER_OPEN_RETURN_OBSERVED
CONTAINER_OPEN_INTERACTION_OUTCOME_WINDOW
```

#### D8.2 exact Store correlation

현재 interaction event에는 `interactionId`와 `postPlaceOperationId`는 있지만
`storeOperationId`가 없다. command context만으로 최근 Store operation을 추측하지
않는다. existing `interactionId`를 key로 immutable Store snapshot을 정확히 bind한다.

추가할 context fields:

```text
storeContextAvailable
storeOperationId
storeAttemptId
interactionAttemptId
candidateDecisionSequenceAtAttempt
branchEpochAtAttempt
branchAtAttempt
routeChildLifecycleIdAtAttempt
routeChildClassAtAttempt
routeChildIdentityAtAttempt
targetRole
targetPosition
contextBindingSource
contextBindingConfidence
```

후속 window outcome fields:

```text
observationOffsetTicks
branchAtObservation
branchChangedSinceAttempt
routeChildClassAtObservation
routeChildChangedSinceAttempt
screenHandlerBefore
screenHandlerAtReturn
screenHandlerAtObservation
screenHandlerSyncIdBefore
screenHandlerSyncIdAtReturn
screenHandlerSyncIdAtObservation
expectedGuiOpened
screenHandlerChangedObserved
targetStillMatchesClicked
outcome
```

binding은 정확한 active Store descendant와 interaction target이 연결될 때만 허용한다.
`lastActiveState()` fallback, 가장 최근 operation 추측, 좌표만 같은 unrelated click
결합은 금지한다. exact binding을 만들 수 없으면 `storeContextAvailable=false`와
coverage reason을 남긴다.

### D9. 50/70 range crossing과 child interruption 연결

기존 parent decision은 range boolean을 기록하지만 실제 distance와 이전 판정의 crossing을
보존하지 않는다. 이미 계산된 raw/current target과 player 위치만 사용해 다음 값을
진단 payload와 route state aggregate에 추가한다.

```text
playerPositionAtDecision
rawDistanceSquared
rawRangeThresholdSquared=2500
rawWithin50Before
rawWithin50After
rawRangeCrossing
currentTryDistanceSquared
currentTryRangeThresholdSquared=4900
currentTryWithin70Before
currentTryWithin70After
currentTryRangeCrossing
fallbackContainerItemPresent
selectedBranch
triggeringBranchEpoch
activeRouteChildBefore
activeRouteChildAfter
replacementApplied
previousRouteChildStopCalled
resourceAcquisitionInterruptedByBranchChange
```

exact distance는 payload에만 넣고 dedupe fingerprint에는 넣지 않는다. player가 조금씩
움직일 때 매 tick 새 상태로 오인하지 않도록 fingerprint는 다음 semantic 값만 사용한다.

```text
raw/current target position
inside/outside boolean
selected branch
branch epoch transition type
child reconciliation outcome
```

기존 `STORE_CONTAINER_PARENT_CANDIDATE_DECISION`과
`STORE_TASK_CHILD_RECONCILIATION`을 같은 `branchEpoch`로 연결한다. 새 generic
`MineOrCollectTask.stop` event를 추가하지 않는다. Store root가 실제로 이전
`OBTAIN_CHEST` route child를 교체했고 그 descendant stop이 기존 lifecycle binding으로
확인될 때만 `resourceAcquisitionInterruptedByBranchChange=true`로 기록한다.

detail cap 뒤에도 다음 aggregate는 checkpoint와 terminal summary에 남긴다.

```text
raw50CrossingCount
currentTry70CrossingCount
branchChangeAtRangeCrossingCount
resourceChildInterruptedAtRangeCrossingCount
firstRangeCrossingSample
lastRangeCrossingSample
```

### D6. Baritone context는 계속 조건부

D7-D9와 clean JVM 재현 뒤에도 first failing boundary가 남을 때만 기존 D6를 진행한다.
그때도 다음 state change만 operation에 bind한다.

```text
goal request
calculation generation start/result
path adoption
process owner change
path failure or cancellation owner
late completion after branch change
```

전체 path node, 매 tick pathing state, 전역 goal cancel 또는 자동 recovery는 추가하지
않는다.

### D5. durable effect oracle은 후순위

정상 operation의 저장 효과를 더 강하게 증명하는 D5는 유효하지만 현재 장기
비수렴의 최초 실패 경계를 찾는 데 선행 조건은 아니다. D7-D9 또는 조건부 D6가
비수렴 원인을 좁힌 뒤 별도 구현 범위로 유지한다.

## 19. 책임과 파일 경계

다음은 구현 승인 후 검토할 예상 범위다. 이 목록은 구현 승인이 아니며 실제 diff
직전 call chain을 다시 확인해 더 줄인다.

### 19.1 D7 candidate verdict 책임

```text
lavi/minecraft/diagnostics/container/store/deposit/candidate/
    StoreContainerCandidateCollector
        scan-local raw match와 aggregate만 소유
    StoreContainerCandidateObservation
        immutable result만 소유
    StoreContainerCandidateEventFields
        payload projection만 소유
```

기존 `DepositAllTask.validContainer`의 `observe(position, reason)` 호출은 이미 정확한
short-circuit 지점에 있으므로, D7 때문에 predicate 조건을 다시 쓰거나 helper로
옮기지 않는다.

### 19.2 D8 interaction correlation 책임

둘 이상의 책임을 한 class에 넣지 않는다.

```text
lavi/minecraft/diagnostics/container/store/deposit/interaction/
    StoreDepositInteractionContext
        one immutable interaction binding only
    StoreDepositInteractionBindingRegistry
        bounded interactionId lookup and expiry only
    StoreDepositInteractionDiagnosticFields
        existing event field projection only
    StoreDepositInteractionObserver
        exact active Store descendant binding request only
```

기존 LAVI-owned interaction 경계에서는 필요한 최소 merge만 검토한다.

```text
lavi/minecraft/diagnostics/BlockInteractionDiagnostics.java
lavi/minecraft/integration/carryon/container/CarryOnContainerInteractionLogger.java
lavi/minecraft/integration/carryon/container/CarryOnContainerPostconditionWindow.java
```

`CarryOnContainerPostconditionWindow`는 postcondition sampling만 소유하고 Store binding
상태를 소유하지 않는다. Store registry는 Carry On state를 읽거나 outcome을 분류하지
않는다.

### 19.3 D9 route correlation 책임

```text
lavi/minecraft/diagnostics/container/store/deposit/candidate/
    StoreContainerRouteState
        bounded crossing counters and first/last samples only
    StoreContainerCandidateEventFields
        parent/checkpoint payload projection only

adris/altoclef/tasks/container/DepositAllTask.java
    existing player/target/range locals를 diagnostics call에 전달하는 최소 hunk만 후보
```

### 19.4 의도적으로 수정하지 않을 파일

```text
adris/altoclef/commands/DepositCommand.java
adris/altoclef/tasks/container/StoreInAnyContainerTask.java
adris/altoclef/tasks/container/StoreInContainerTask.java
adris/altoclef/tasks/ResourceTask.java
adris/altoclef/TaskCatalogue.java
adris/altoclef/tasksystem/Task.java
PlayerInteractionFixChain
InteractWithBlockTask.isFinished()
Baritone source
Fabric bridge protocol and dispatcher
Python command registry
```

현재 D7-D9 설계에는 `DoToClosestBlockTask`의 추가 upstream hunk도 필요하지 않다.
기존 D2 scope로 raw exact verdict를 얻을 수 없는 것이 구현 직전 증명될 때만 멈춰서
별도 최소 hunk를 보고한다.

## 20. hot-path와 bounded logging 검토

| Slice | hot path | loop 내부 동작 | 즉시 방출 조건 | cap과 summary |
| --- | --- | --- | --- | --- |
| D7 raw verdict | `validContainer` candidate loop | fixed counter와 raw value match만 갱신 | loop에서는 방출 없음, scan 종료 후 raw verdict 변화 | candidate family 44, operation 256 |
| D8 window | end-client-tick의 bounded interaction window | offset `1/2/5`에서만 snapshot | return, 첫 state change, terminal | 기존 block-interaction session cap 5000, Store field 추가는 새 줄을 만들지 않음 |
| D8 binding | interaction HEAD/RETURN/outcome | exact `interactionId` lookup | 기존 event가 방출될 때 field merge | bounded registry, terminal 또는 expiry purge |
| D9 range | `DepositAllTask.onTick()` parent decision | fixed counter와 first/last sample만 갱신 | target, inside/outside, branch 또는 reconciliation 변화 | candidate/child family 각 44, checkpoint 1200 ticks |
| D6 conditional | Baritone calculation/path lifecycle | state-change counter만 갱신 | generation, owner, adoption, failure 변화 | Baritone family 44 |

공통 계약은 유지한다.

```text
operation detail hard cap: 256
session hard cap:          5000
critical reserve:          64
checkpoint interval:       1200 client ticks
unchanged per-tick log:    forbidden
per-candidate log:         forbidden
```

timestamp, exact distance, tick, instance identity와 `branchEpoch` 숫자 자체는 dedupe
fingerprint를 매번 새롭게 만드는 값으로 사용하지 않는다. counters는 detail cap과
무관하게 bounded primitive 값으로 갱신하고 checkpoint/terminal에 투영한다.

## 21. 구현 전 test 계획

### D7 unit tests

```text
raw accepted and selected
raw accepted but another filtered result selected
raw rejected for each existing reason
raw not visited by filtered scan
raw unavailable
abnormal scope completion
BlockPos value equality, not reference equality
aggregate counts unchanged from D2
```

### D8 unit tests

```text
immediate GUI -> GUI_OPENED at offset 0 and no window retained
no immediate GUI -> OBSERVATION_PENDING and window retained
GUI at +1/+2/+5 -> GUI_OPEN_DELAYED
no GUI with target retained -> terminal NO_GUI_TARGET_STILL_PRESENT at +5 only
target removed -> TARGET_REMOVED_WITHOUT_GUI
unknown target state -> OBSERVATION_WINDOW_EXPIRED
exact interactionId binding across HEAD, RETURN and outcome window
no fallback to last active Store operation
binding expiry and bounded registry eviction
```

### D9 unit tests

```text
outside->inside raw 50 crossing
inside->outside raw 50 crossing
outside->inside currentTry 70 crossing
inside->outside currentTry 70 crossing
distance jitter without boolean change does not emit detail
branch change without child replacement
branch change with actual child replacement
resource interruption remains false without exact descendant evidence
checkpoint preserves first/last crossing after detail cap
```

### 정적 보호 검증

```text
DepositCommand.java hash unchanged
StoreInAnyContainerTask.java hash unchanged
no new getNearestBlock call
no repeated validContainer predicate evaluation
no return-value or branch-order change
no Task equality/completion/retry/timeout/input/path/cleanup change
no wire schema change
```

build와 test 실행은 별도 사용자 승인을 받은 뒤 clean forced build runbook으로 수행한다.

## 22. clean runtime 재현 계획

Java diagnostics 구현과 build/deployment가 각각 승인되고 완료됐을 때만 수행한다.

### 22.1 clean JVM gate

```text
Minecraft 완전 종료
기존 Java process 종료 확인
새 Minecraft 실행
diagnostics BOUNDARY 확인
active JAR SHA-256 확인
@deposit_all 전 BlockOptionalMeta exception 부재 확인
```

world가 복사, 복원, 교체 또는 이름 변경되지 않았다면 Baritone disk cache는 건드리지
않는다. 그런 world 변경이 있었다면 Minecraft를 닫은 상태에서 cache troubleshooting
문서 절차를 별도 승인받아 적용한다.

### 22.2 짧은 비수렴 fixture

```text
이전과 같은 보호 가능한 inventory 상태 준비
상자 item 없음
raw container가 50-block 경계 부근에 보이는 위치 사용
bare @deposit_all 한 번 실행
OPEN_EXISTING <-> OBTAIN_CHEST 왕복 2회 또는 first failure evidence 확보 시 stop
```

10분 이상 방치하지 않는다. 다음 증거가 모두 나오면 재현은 충분하다.

```text
exact raw verdict
range crossing
root child reconciliation
interaction offset 0/+1/+2/+5 outcome, interaction이 있었다면
clean-JVM contamination status
```

### 22.3 placed chest click fixture

새로 배치된 chest의 첫 interaction을 별도로 확인한다.

```text
same storeOperationId
same interactionAttemptId for HEAD/RETURN/window
target - branchEpoch - route child snapshot
immediate and delayed screen-handler chronology
```

### 22.4 정상 완료 control

가까운 나무와 접근 가능한 배치 위치가 있는 control에서 같은 target inventory를
복원하고 natural finish까지 실행한다. 새 D7-D9 detail이 정상 경로를 비수렴으로
분류하지 않는지 확인한다.

## 23. 다음 보강의 합격과 중단 기준

### 합격 기준

1. 각 raw candidate가 `ACCEPTED`, 정확한 rejection reason 또는
   `RAW_NOT_VISITED_BY_FILTERED_SCAN`으로 판정된다.
2. placed chest interaction이 offset 0과 `+1/+2/+5`의 같은 `interactionId`로
   연결된다.
3. interaction이 정확한 `storeOperationId`, `branchEpoch`와 route child에 묶인다.
4. 각 branch transition이 50/70 crossing 여부 및 실제 child replacement와 연결된다.
5. detail cap 뒤에도 crossing과 replacement aggregate가 checkpoint에 남는다.
6. clean JVM 실행은 사전 `BlockOptionalMeta` 예외 유무를 명시한다.
7. 새 로그는 per-tick 또는 per-candidate 출력을 만들지 않는다.
8. 기존 `@deposit` 원본 두 파일과 runtime behavior는 변하지 않는다.
9. 마지막 성공 경계와 첫 실패 또는 첫 미관측 경계를 구분할 수 있다.

### 중단 기준

다음 중 하나면 범위를 넓히지 않고 보고한다.

```text
exact raw verdict를 얻기 위해 scanner나 predicate를 다시 호출해야 함
interaction binding이 last-active 추측에 의존함
delayed GUI 관측에 gameplay timeout 또는 retry가 필요함
InteractWithBlockTask.isFinished() 변경이 필요함
PlayerInteractionFixChain 변경이 필요함
Task.java 또는 둘 이상의 upstream lifecycle owner에 새 hunk가 필요함
Baritone context가 광범위한 source instrumentation을 요구함
진단 구현이 branch, child, input, path, cleanup 또는 exception 흐름을 바꿀 가능성이 있음
```

이 문서화 단계의 최종 판단은 다음과 같다.

```text
D0-D4 runtime observability: sufficient to prove the loop mechanism
final root cause:             not proven
next smallest evidence:      D7 exact raw verdict
parallel diagnostic gap:     D8 delayed GUI window and exact Store correlation
loop trigger correlation:    D9 range crossing and child interruption
behavior fix authorization:  none
source/build/commit/push:     not performed by this documentation update
```

## 24. D7-D9 진단 구현 상태 (2026-08-26)

이 절의 구현은 동작 수정이 아니라 BOUNDARY 진단 보강이다. 최종 원인은 아직
확정하지 않는다.

### 24.1 D7 구현

`StoreContainerCandidateCollector`가 기존 filtered scan에서 이미 평가된
`position + rejection reason`만 집계한다. scanner와 predicate를 다시 호출하지
않는다.

```text
RAW_ACCEPTED
RAW_REJECTED + exact rejection reason
RAW_NOT_VISITED_BY_FILTERED_SCAN
RAW_UNAVAILABLE
FILTERED_SCAN_DID_NOT_COMPLETE
```

scan이 정상 종료되지 않은 경우에도 기존 예외 전파는 그대로 유지하며, `finally`
경계에서 수집된 부분 관측치만 bounded event로 남긴다. 후보별 event와 매 tick
event는 추가하지 않았다.

### 24.2 D8 구현

기존 `BlockInteractionContext.interactionId`에 다음 immutable Store snapshot을
결합한다.

```text
storeOperationId
storeAttemptId
candidateDecisionSequenceAtAttempt
branchEpochAtAttempt
branchAtAttempt
routeChildLifecycleIdAtAttempt
route child class/identity
active descendant class/identity
target role/position
exact binding source/confidence
```

binding 조건은 `BARE_DEPOSIT_ALL_COMMAND + OPEN_EXISTING + 현재 route descendant
identity + current pursuit/filtered target value`가 모두 일치하는 경우뿐이다.
`lastActiveState()` fallback은 사용하지 않는다.

offset 0에서 GUI 또는 pickup이 없으면 이제 `OBSERVATION_PENDING`이며 기존
`+1/+2/+5` window가 유지된다. `+1/+2`는 직전 snapshot에서 실제 상태가 바뀐
경우만 기록하고, `+5`는 terminal outcome을 기록한다. postcondition 분류는
별도 `CarryOnContainerPostconditionClassifier`가 소유하고 window는 scheduling과
bounded lifecycle만 소유한다. active window는 최대 64개, Store interaction
binding은 최대 256개와 40 tick 수명으로 제한한다.

### 24.3 D9 구현

`candidate/range/` 패키지가 거리 snapshot, crossing 판정, aggregate, field
projection을 각각 소유한다. 실제 parent branch가 이미 계산한 50/70 boolean을
판정 기준으로 사용하고, squared distance는 payload 관측값으로만 계산한다.

```text
raw 50: INSIDE_TO_OUTSIDE / OUTSIDE_TO_INSIDE / NONE
current try 70: INSIDE_TO_OUTSIDE / OUTSIDE_TO_INSIDE / NONE
exact distance excluded from dedupe key
same target and changed boolean required for crossing
```

`resourceAcquisitionInterruptedByBranchChange=true`는 다음 조건을 모두 만족할 때만
기록한다.

```text
ROOT_ROUTE
previous branch == OBTAIN_CHEST
branch changed
replacement applied
previous route child stop requested
same client tick에서 해당 child STOP END 관측
```

crossing count, crossing 시 branch-change count, resource-child interruption count,
first/last sample은 1200 tick changed-only checkpoint와 terminal summary에 남는다.
range의 since-checkpoint 집계는 checkpoint 방출이 budget에서 승인된 뒤에만
초기화되며, 억제되면 다음 checkpoint 또는 terminal summary까지 유지된다.

### 24.4 책임 분리와 보호 결과

```text
candidate/       raw/filter aggregate and route correlation
candidate/range/ distance snapshot, crossing tracker, aggregate, event fields
interaction/     immutable binding, bounded registry, event fields, observer
Carry On classifier: postcondition classification only
Carry On window: bounded offset scheduling only
StoreDepositDiagnostics: facade and delegation only
```

정적 확인 결과:

```text
DepositCommand.java SHA-256:
D4D598C6D1F2F3465A18F0A150B8294F621F3248C35DBCD97896AC75209F09B5

StoreInAnyContainerTask.java SHA-256:
7F54FE3FACE7DE1D5DFE8D45B9330A97B3B0AD80A47A7071EF4F1CBEDBC612BC

original file diff: none
new scanner call: none
new predicate invocation: none
behavior_effect: none
```

이 절을 작성한 직후 상태는
`SOURCE_IMPLEMENTED_BUILD_NOT_RUN_RUNTIME_NOT_VERIFIED`였다. 이후 별도 사용자
승인으로 수행한 build, deployment 확인과 runtime 증거가 이 상태를 대체하며, 최종
판정은 다음 절에 기록한다.

## 25. D7-D9 runtime 판정과 B0.1 수정 방향 (2026-08-26)

이 절은 이 loop에 대한 진단 단계의 최종 기록이다. 앞 절의
`ROOT_CAUSE_NOT_PROVEN`, 추가 D7-D9 필요, build 미실행 상태는 당시의 이력으로만
읽고 현재 상태로 사용하지 않는다.

### 25.1 실행 artifact와 로그 범위

별도 승인된 외부 Windows PowerShell clean forced build 결과:

```text
run id: 20260826-023409
command: .\gradlew.bat clean build --rerun-tasks --no-build-cache --no-daemon --stacktrace --offline
Gradle exit code: 0
result: BUILD SUCCESSFUL in 2m 7s
tasks: 171 actionable tasks, 171 executed
1.20.1 JAR bytes: 6558376
1.20.1 JAR SHA-256: 76876A5AC793BB3090F6AE8FA7CDCC6122FB447D751CB530B62541E0896DC72C
```

활성 CurseForge instance에는 같은 이름의 ChatClef JAR이 하나만 있었고, source
artifact와 크기, 수정 시각, SHA-256이 모두 일치했다.

```text
instance: C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01
deployed JAR: mods\chatclef-1.20.1-0.18.23.jar
deployed SHA-256: 76876A5AC793BB3090F6AE8FA7CDCC6122FB447D751CB530B62541E0896DC72C
Minecraft launch: 2026-08-26 02:38:27 +09:00
loaded mod: altoclef 1.20.1-0.18.23
diagnostics mode: BOUNDARY
```

검토한 현재 실행 로그:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\instance_audit.txt
C:\Vtuber_Souorce_Code\LAVI\logs
```

`latest.log`와 `stdout-logs.txt`는 같은 operation evidence를 포함했다.
`instance_audit.txt`는 현재 launch와 `BOUNDARY` 설정을 확인하는 근거로 사용했다.
저장소 `logs`의 최신 일반 application log는 ScreenVision 관측 로그였고 Minecraft
Task runtime evidence로 사용하지 않았다. 그 파일에 보인 Java exception 문장은
실제 process exception이 아니라 화면 내용을 기술한 관측 text였다.

현재 launch에서 ChatClef, Mixin, injection, descriptor 또는 linkage 실패는 없었다.
`Immersive Aircraft` data fixer와 `Prefab` resource path 오류는 이 operation과 무관한
다른 mod의 startup 메시지다. 새 crash report도 생성되지 않았다.

### 25.2 확정된 operation chronology

bare command attribution은 정상이다.

```text
commandName: deposit_all
requestSource: BARE_DEPOSIT_ALL_COMMAND
root task: adris.altoclef.tasks.container.DepositAllTask
storeOperationId: store-deposit-479
operation start tick: 340
selected targets: 2 azalea, 1 dirt, 1 crafting_table, 4 oak_planks
```

최초 두 candidate 경계는 다음과 같다.

```text
parent raw candidate:      -645,51,147
parent raw range:          inside 50
parent selected branch:    OPEN_EXISTING

filtered candidate:        -527,52,125
raw/filter relation:       DIFFERENT_POSITION
evaluated candidates:      16
accepted candidates:       5
rejected candidates:       11
raw candidate verdict:     RAW_REJECTED
raw rejection reason:      CHEST_ABOVE_BLOCKED_UNBREAKABLE
current filtered range:    outside 70
```

부모는 predicate 없는 raw 최근접 상자를 기준으로 `OPEN_EXISTING`을 선택했다.
자식 `DoToClosestBlockTask`는 `validContainer` predicate에서 같은 raw 상자를
탈락시키고 먼 filtered 상자를 실제 pursuit로 선택했다.

player가 filtered 상자로 이동하면서 raw 상자의 squared distance가 `2500` 경계를
넘자 tick 394에서 부모는 `OBTAIN_CHEST`로 바뀌었다. 실제 root child는
`DoToClosestBlockTask`에서 `CraftInTableTask`로 교체됐고 이전 child의 stop도
호출됐다. resource descendant는 나무 `-696,66,110`을 pursuit로 선택했다.

나무 방향으로 이동하면 player가 raw 상자의 50-block 범위 안으로 다시 들어왔다.
부모는 `OPEN_EXISTING`으로 돌아가 resource child를 중단하고 filtered 상자
`-527,52,125` pursuit를 다시 만들었다. 이 왕복은 같은 좌표와 같은 미저장 상태로
반복됐다.

### 25.3 checkpoint 상관관계

```text
checkpoint 1, elapsed 1200 ticks:
    decisions=1200, branchEpoch=75
    branchCounts={OPEN_EXISTING=866, OBTAIN_CHEST=334}
    transitions={NONE->OPEN_EXISTING=1, OPEN_EXISTING->OBTAIN_CHEST=37, OBTAIN_CHEST->OPEN_EXISTING=37}
    raw50Crossings=74, childReplacements=373, transfers=0

checkpoint 2, elapsed 2400 ticks:
    decisions=2400, branchEpoch=151
    branchCounts={OPEN_EXISTING=847, OBTAIN_CHEST=353}
    transitions={OPEN_EXISTING->OBTAIN_CHEST=38, OBTAIN_CHEST->OPEN_EXISTING=38}
    raw50Crossings=76, childReplacements=187, transfers=0

checkpoint 3, elapsed 3600 ticks:
    decisions=3600, branchEpoch=233
    branchCounts={OPEN_EXISTING=839, OBTAIN_CHEST=361}
    transitions={OPEN_EXISTING->OBTAIN_CHEST=41, OBTAIN_CHEST->OPEN_EXISTING=41}
    raw50Crossings=82, childReplacements=82, transfers=0
```

누적 결과:

```text
raw 50-block crossings: 232
OPEN_EXISTING -> OBTAIN_CHEST: 116
OBTAIN_CHEST -> OPEN_EXISTING: 116
total branch changes after initial selection: 232
child replacements: 642
transfer decisions: 0
notStoredStateHash: d02b3657, unchanged
```

raw 50-block crossing과 branch change가 `232:232`로 일치한다. 이 operation은
container click 또는 GUI postcondition 경계까지 도달하지 않았으므로 D8 interaction
event가 없는 것은 coverage 실패가 아니라 loop가 그보다 앞에서 발생했다는 증거다.
검토 snapshot까지 root natural finish, root stop과 `terminal=true`도 없었다.

### 25.4 최종 root cause 판정

최종 판정은 `PARENT_CHILD_CANDIDATE_INCONSISTENCY`다.

```text
parent branch candidate != active child pursuit candidate
```

이 문제는 Task scheduler가 두 child 중 어느 priority가 높은지 결정하지 못한 것이
아니다. branch order는 `OPEN_EXISTING`, `PLACE_CONTAINER_NEARBY`, `OBTAIN_CHEST`
순서로 결정적이다. Task lifecycle도 부모가 반환한 서로 다른 child를 정상적으로
비교하고, interrupt 가능 여부를 확인한 뒤 실제로 교체했다.

진동을 만든 것은 우선순위가 아니라 다음 두 입력의 불일치다.

```text
parent range input: unfiltered raw nearest container
child movement input: predicate-filtered nearest valid container
```

소스에서도 `DepositAllTask`의 parent scan과 child scan이 각각 이 구조를 가진다.
복제 원본 `StoreInAnyContainerTask`에도 같은 구조가 있지만, 이번 runtime에서
`@deposit`을 같은 fixture로 실행하지 않았으므로 원본 명령의 runtime 재현까지
증명했다고 기록하지 않는다. 원본은 수정 대상에도 포함하지 않는다.

다음 가설은 이 loop의 root cause에서 제외한다.

```text
Task priority ambiguity
TaskRunner scheduling failure
child interruption failure
Baritone path calculation or adoption failure
container click or delayed GUI failure
Carry On failure
Mixin or stale JAR failure
diagnostic session cap exhaustion
```

### 25.5 B0.1 candidate consistency 방향

이 수정의 단일 불변조건은 다음과 같다.

```text
parent decision candidate == active child target
```

`DepositAllTask`가 selected valid container의 선택, 유지와 해제를 소유한다.

```text
1. raw nearest container는 diagnostics observation에만 사용한다.
2. 행동 후보는 validContainer를 통과한 filtered candidate에서 선택한다.
3. 새 후보를 채택하는 50-block 판정은 그 filtered candidate에 적용한다.
4. 이미 활성화된 후보의 70-block hysteresis도 같은 좌표에 적용한다.
5. 자식은 부모가 선택한 같은 BlockPos를 실행 목표로 사용하며 독립적으로 다른
   nearest candidate를 다시 선택하지 않는다.
6. 같은 좌표인지는 object identity가 아니라 BlockPos value equality로 판정한다.
```

같은 predicate를 부모와 자식이 각각 다시 실행하는 것만으로는 불변조건을 보장하지
못한다. 컨테이너 cache와 world state가 두 평가 사이에 바뀔 수 있고, 새 child 객체를
반환해도 Task equality 때문에 기존 active child가 유지될 수 있다. 한 candidate
selection 결과를 부모가 소유하고 active child가 그 결과를 읽도록 해야 한다.

선택한 목표는 다음처럼 실제 유효성이나 접근성 근거가 바뀔 때만 해제한다.

```text
target block is no longer a supported container
validContainer rejects the target because it is blocked, full or a dungeon chest
the existing progress/unreachable boundary explicitly rejects the target
the active target leaves the allowed 70-block continuation range
the owning DepositAllTask stops
```

선택 가능한 valid container가 새 후보 50-block 범위 안에 없으면 invalid raw 상자의
거리 변화로 `OPEN_EXISTING`을 선택하지 않는다. 기존 fallback order에 따라 가지고
있는 container block을 배치하거나 chest 획득을 계속한다.

### 25.6 의도적으로 사용하지 않을 해결책

다음은 원인을 제거하지 않고 진동을 가리는 방식이므로 적용하지 않는다.

```text
Task priority 숫자 조정
child를 강제로 non-interruptible로 변경
임의 timeout 또는 cooldown
50/70 거리 상수 확대
raw candidate 전역 blacklist
전체 TaskRunner 또는 Baritone path cancellation
ResourceTask, TaskCatalogue 또는 generic Task lifecycle 변경
```

### 25.7 구현 파일 경계

행동 변경은 새 command 경로에만 둔다.

```text
primary behavior owner:
    adris/altoclef/tasks/container/DepositAllTask.java

LAVI-owned collaborators:
    lavi/minecraft/task/container/deposit/DepositAllContainerEligibility.java
    lavi/minecraft/task/container/deposit/DepositAllContainerSelector.java
    lavi/minecraft/task/container/deposit/DepositAllContainerTargetState.java

diagnostics adjustment, only to keep raw and selected candidate field meanings truthful:
    lavi/minecraft/diagnostics/container/store/deposit/**

focused tests:
    deposit_all candidate selection, retention, invalidation and branch stability
```

의도적으로 수정하지 않을 파일:

```text
adris/altoclef/commands/DepositCommand.java
adris/altoclef/tasks/container/StoreInAnyContainerTask.java
adris/altoclef/tasks/DoToClosestBlockTask.java
adris/altoclef/tasks/container/StoreInContainerTask.java
adris/altoclef/tasks/ResourceTask.java
adris/altoclef/TaskCatalogue.java
adris/altoclef/tasksystem/Task.java
PlayerInteractionFixChain
InteractWithBlockTask.isFinished()
Baritone source
```

기존 `DoToClosestBlockTask`의 D7 diagnostics-only hunk는 별도 rollback 단위이며,
B0.1 행동 수정을 위해 넓히거나 decision hook으로 바꾸지 않는다.

### 25.8 최소 합격 기준

```text
invalid raw inside 50 + valid filtered outside 70:
    OPEN_EXISTING을 선택하지 않고 fallback branch가 raw crossing과 무관하게 유지됨

valid filtered inside 50:
    parent decision과 active child target이 같은 BlockPos임

active selected target between 50 and 70:
    같은 target과 OPEN_EXISTING continuation이 유지됨

selected target invalidated:
    명시적 사유로 해제한 뒤 한 번 재선택하며 stale target을 계속 추적하지 않음

same coordinate in a different BlockPos instance:
    같은 target으로 취급하며 progress를 reset하거나 child를 교체하지 않음

original protection:
    DepositCommand.java와 StoreInAnyContainerTask.java의 hash와 diff가 그대로임
```

runtime에서는 동일 fixture에서 raw crossing과 branch change의 `1:1` 상관관계가
사라지고, fallback이 완료되거나 실제 container interaction 경계에 도달해야 한다.
그 뒤 새 실패가 나타날 때만 이미 구현된 D8 또는 D5 관측 범위를 사용한다.

### 25.9 현재 상태와 문서 종료 조건

```text
D7-D9 source:                         implemented
D7-D9 clean forced build:             passed before B0.1
D7-D9 deployed JAR hash:              matched before B0.1
Minecraft diagnostics runtime:        observed before B0.1
root cause:                           proven
additional pre-fix logging:           not required
B0.1 direction:                       implemented
B0.1 behavior source:                 implemented
B0.1 helper test source:              implemented and passed across 11 versions
B0.1 orchestration test coverage:     missing
B0.1 clean forced build:              passed, run 20260826-132308
B0.1 Gradle command:                  clean build --rerun-tasks --no-build-cache --no-daemon --stacktrace --offline
B0.1 Gradle result:                   BUILD SUCCESSFUL in 2m 27s, 171 tasks executed
B0.1 1.20.1 JAR bytes:               6569479
B0.1 1.20.1 JAR SHA-256:             FA3F30A1C6B2121DC41958B3DFBDFE7A4D6CA9F789658ED374439A9553FD3D6B
B0.1 deployed JAR hash:               matched active CurseForge instance
B0.1 Minecraft runtime:               two NATURAL_FINISH operations observed
raw 50-block crossing after B0.1:     zero in both recorded operations
same-target active child stability:   failed; root route child replaced during progress
durable storage effect oracle:        not verified; effectVerified=false
DepositCommand.java SHA-256:          D4D598C6D1F2F3465A18F0A150B8294F621F3248C35DBCD97896AC75209F09B5
StoreInAnyContainerTask.java SHA-256:  7F54FE3FACE7DE1D5DFE8D45B9330A97B3B0AD80A47A7071EF4F1CBEDBC612BC
static diff check:                    passed
original @deposit runtime parity:     not tested in this fixture
commit:                               9785e17b
push:                                 origin/minecraft-plugin-fix/alto-clef-infinite-loop aligned
release or merge approval:            rejected pending focused lifecycle fix and review
automatic deposit_all trigger:        blocked by the same-target child lifecycle defect
```

첫 clean run `20260826-132103`은 제품 소스가 아니라 selector test fixture가 Minecraft
bootstrap 없이 `Blocks.CHEST`를 초기화해 `1.21.1:test`에서 실패했다. 테스트 double이
사용하지 않는 전역 block 상수 의존을 제거한 뒤 전체 clean build를 다시 실행했다.
두 번째 run에서는 새 selector/target-state 테스트 5개와 parent candidate collector
테스트 6개가 지원 대상 11개 버전에서 모두 failure/error 없이 통과했다.

빌드 로그와 구조화 결과:

```text
logs/build/chatclef-fabric-clean-build-20260826-132308.log
logs/build/chatclef-fabric-clean-build-20260826-132308.result.json
```

구현 결과 `DepositAllTask`가 selected target과 branch를 소유한다. 목표가 유효하고
70-block 범위 안이면 filtered scan 없이 유지하고, 목표가 없을 때만 한 번의 filtered
scan을 수행한다. 새 후보는 50-block 안에서만 채택하며 같은 좌표는 value equality로
유지한다. `OPEN_EXISTING` child의 target 좌표도 selected target과 일치한다. raw
candidate는 계속 진단에 기록하지만 행동 분기에는 사용하지 않는다.

다만 target 좌표의 안정성과 active child lifecycle의 안정성은 별개다. 현재
`DepositAllTask`는 같은 target에서도 root `notStored` 변화에 따라 새
`StoreInContainerTask`를 만들며, runtime에서 실제 root route child 교체가 관측됐다.
따라서 B0.1은 원래 후보 불일치를 해결한 중간 checkpoint로 보존하되 release 승인
상태로 분류하지 않는다. 상세 판정과 다음 최소 수정 범위는 26절이 canonical record다.

이 25절은 B0.1 후보 일관성 구현과 build 결과의 기록으로 유지한다. 같은 내용을 다시
정리하는 별도 계획서, 문서 재검수 문서 또는 추가 로그 계획서를 만들지 않는다.
후속 lifecycle 판정과 build/runtime 증거는 아래 26절만 갱신한다.

## 26. B0.1 post-commit child lifecycle 재검수

### 26.1 판정과 증거 범위

검수 기준 커밋은 다음과 같다.

```text
commit: 9785e17b feat(minecraft): add stable deposit_all container targeting
branch: minecraft-plugin-fix/alto-clef-infinite-loop
remote state at documentation update: HEAD == origin branch
commit scope: 54 files, +6878 / -198
```

외부 재검수의 ZIP 환경에서는 Gradle 배포판과 build/runtime 산출물을 독립 확인하지
못했지만, 이는 그 검수 환경의 `NOT RUN`이다. 실제 작업 PC에서는 B0.1 JAR hash가
활성 CurseForge 인스턴스와 일치했고, 아래 두 Minecraft operation이 자연 완료됐다.
따라서 이 문서의 runtime 상태는 `NOT VERIFIED`가 아니라 `OBSERVED`다.

그러나 자연 완료는 active child 안정성을 증명하지 않는다. 정적 코드와 같은
operation의 bounded lifecycle counter가 모두 같은-target child 교체를 가리키므로
최종 판정은 다음과 같다.

```text
original raw/filtered root cause:      PASS
filtered candidate unification:       PASS
50/70 selected-target retention:      PASS
B0.1 clean forced build:              PASS
B0.1 deployed JAR hash:               MATCHED
B0.1 Minecraft natural finish:        OBSERVED
same-target active child stability:   FAIL
DepositAllTask orchestration tests:   MISSING
release or merge approval:            REJECT_PENDING_FOCUSED_FIX
```

### 26.2 확정된 lifecycle 결함

현재 `DepositAllTask.onTick()`은 매 tick root tracker에서 `notStored`를 다시 계산하고,
selected target이 같더라도 새 child를 반환한다.

```java
ItemTarget[] notStored =
        _storedItems.getUnstoredItemTargetsYouCanStore(mod, _toStore);

return new StoreInContainerTask(
        fixedTarget,
        _getIfNotPresent,
        notStored
);
```

공용 `StoreInContainerTask.isEqual()`은 좌표와 flag뿐 아니라
`Arrays.equals(task.toStore, toStore)`도 비교한다. 정상 전송으로 root tracker의
미저장 수량이나 배열 구성만 바뀌어도 새 child와 active child는 unequal이 된다.
generic Task lifecycle은 이전 child를 중단하고 같은 좌표의 새 child를 시작한다.

```text
same selected target
    -> successful transfer changes root notStored
    -> new StoreInContainerTask with a different item snapshot
    -> StoreInContainerTask.isEqual() == false
    -> previous child STOP
    -> new same-target child START
```

새 child는 container-local 상태를 처음부터 시작하므로 이미 열린 GUI를 이어받는다고
가정할 수 없다. 전송 뒤 tracker 해제와 재등록, slot child 중단, GUI 닫기와 재상호작용이
반복될 수 있다. 정적 코드만으로 항상 무한 루프라고 단정하지는 않지만, 성공적인
progress가 child replacement를 일으키는 lifecycle 결함 자체는 확정됐다.

### 26.3 B0.1 runtime 상관관계

활성 JAR:

```text
bytes:   6569479
SHA-256: FA3F30A1C6B2121DC41958B3DFBDFE7A4D6CA9F789658ED374439A9553FD3D6B
```

관련 로그:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
```

```text
operation:                         store-deposit-42437
terminal:                          NATURAL_FINISH, 52354 ms
final selected target:             -770,66,125
transferDecisionCount:             28
rootRouteChildReplacementCount:    10
childReplacementCount:             43
totalRaw50CrossingCount:           0
same-target GUI sync IDs observed: 3, 4, 5

operation:                         store-deposit-76554
terminal:                          NATURAL_FINISH, 12841 ms
final selected target:             -756,2,182
transferDecisionCount:             149
rootRouteChildReplacementCount:    17
childReplacementCount:             81
totalRaw50CrossingCount:           0
same-target GUI sync IDs observed: 9 through 19
```

상세 reconciliation event에도 같은-target 교체가 직접 남아 있다.

```text
operation:                 store-deposit-42437
candidateDecisionSequence: 966
branchEpoch:               6
branch:                    OPEN_EXISTING -> OPEN_EXISTING
selected target:           -770,66,125 -> -770,66,125
active child:              StoreInContainerTask instance 307
candidate/next child:      StoreInContainerTask instance 308
subTasksEqual:             false
previousChildStopCalled:   true
range crossing:            none
```

두 operation 모두 raw 50-block crossing 없이 끝났으므로 이전
`OPEN_EXISTING <-> OBTAIN_CHEST` 거리 진동이 제거된 증거다. 동시에 같은 target에서
root route child가 여러 번 교체되고 GUI sync ID가 증가했으므로, target 좌표만
sticky이고 child generation은 sticky하지 않다는 증거다. `effectVerified=false`이므로
이 자료를 durable slot-delta 검증으로 확대 해석하지 않는다.

### 26.4 다음 최소 동작 수정

수정 책임은 `DepositAllTask` 또는 그 Task가 단독 소유하는 좁은 LAVI helper에 둔다.
selected target별로 다음 한 generation을 함께 소유한다.

```text
activeStoreTarget
activeStoreTask
activeStoreSnapshot
```

새 filtered target을 채택할 때 당시의 `notStored`를 stable snapshot으로 고정하고
`StoreInContainerTask`를 한 번만 만든다. selected target이 계속 유효하고 70-block
continuation 범위 안이면 root `notStored`가 변해도 같은 child instance를 반환한다.

active generation 교체는 다음 경계에서만 허용한다.

1. selected target 좌표가 변경됐다.
2. target이 파괴, blocked, full, dungeon 또는 unsupported 판정으로 무효화됐다.
3. 기존 progress checker가 unreachable 또는 progress failure를 확정했다.
4. target이 70-block continuation 범위를 벗어났다.
5. child가 terminal이고 root tracker로 remaining work가 검증됐다.
6. owning `DepositAllTask`가 중단됐다.

child replacement와 stop은 기존 generic scheduler lifecycle을 따른다. 부모가 같은
child를 수동 중단하거나 shared TaskRunner 상태를 직접 정리하는 방식을 추가하지 않는다.

의도적으로 수정하지 않을 경계:

```text
Task.java
StoreInContainerTask.java and StoreInContainerTask.isEqual()
StoreInAnyContainerTask.java
DepositCommand.java
DoToClosestBlockTask.java
TaskRunner and Baritone
```

특히 공용 `StoreInContainerTask.isEqual()`에서 `toStore` 비교를 제거하지 않는다.
결함의 소유자는 shared child equality가 아니라 매 tick 다른 child 계획을 만드는
`DepositAllTask`다.

### 26.5 필수 orchestration 검증

helper 단위 테스트만으로는 이 결함을 닫을 수 없다. 수정 후 실제 parent orchestration과
generic child reconciliation을 통과하는 다음 검증을 추가한다.

1. 같은 target에서 root deposit progress가 발생해도 active child identity가 같다.
2. 위 경로에서 previous child stop count와 replacement count가 모두 0이다.
3. 다중 아이템과 다중 스택을 저장하는 동안 StoreInContainerTask generation이 하나다.
4. 기존 ocean fixture에서 invalid raw가 50 안이고 valid filtered가 70 밖이면 fallback이 유지된다.
5. valid filtered가 50 안이면 parent selected target과 child target이 같다.
6. 50-70 구간에서는 같은 target과 같은 child generation을 유지한다.
7. full, blocked, unreachable 또는 outside-70 무효화는 stale child stop과 reselection을 각각 한 번만 만든다.
8. `DepositCommand.java`, `StoreInAnyContainerTask.java`, 기존 `@deposit` 경계에 behavior diff가 없다.

focused test와 clean forced build 뒤에는 Minecraft에서 다중 아이템 fixture를 재현하고
동일 target epoch의 `rootRouteChildReplacementCount=0`, 불필요한 GUI 재진입 없음,
자연 완료를 함께 확인해야 한다. 기존 `effectVerified=false` 한계는 결과 해석에 남긴다.

### 26.6 자동 실행과 문서 범위 gate

인벤토리 4/5 조건에서 자동으로 같은 `DepositAllTask`를 실행하는 chain은 이 lifecycle
수정과 검증이 끝날 때까지 구현하지 않는다. 불안정한 child generation을 자동 trigger에
연결하면 수동 실행의 재상호작용을 반복 호출 정책까지 확대할 수 있다.

`9785e17b`는 원래 후보 불일치를 제거하고 runtime 증거를 만든 중간 checkpoint로
보존한다. force rewrite나 broad revert를 하지 않고, lifecycle 수정과 orchestration
test만 focused follow-up commit으로 추가한다.

이 커밋은 behavior, diagnostics, tests, docs를 54개 파일에 함께 담아 Git commit 자체는
atomic rollback 단위가 아니다. 기존 `DoToClosestBlockTask` diagnostics hunk의 논리적
rollback 절차는 `chatclef-engine-divergence-record.md`에 유지하며, 실제 rollback 시에는
해당 hunk만 명시적으로 되돌린다.

이 26절이 재검수와 다음 수정 방향의 최종 canonical record다. 같은 내용을 다시
검수하는 새 문서, 새 진단 계획서 또는 대규모 설계 문서를 만들지 않는다. 이후에는
이 절의 status, test, build, runtime 결과만 갱신한다.
