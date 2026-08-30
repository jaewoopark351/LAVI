<!-- 20260826_openai: Reviewed section 26.10 automatic-deposit item classification and resource JSON policy ownership. -->
<!-- 20260826_kpopmodder: Documented a bounded diagnostics-only plan for the deposit_all ocean loop. -->
<!-- 20260826_kpopmodder: Recorded D0-D4 runtime evidence and the next bounded diagnostics-only slices. -->
<!-- 20260826_kpopmodder: Closed the diagnostic phase with D7-D9 runtime evidence and the candidate-consistency direction. -->
<!-- 20260826_kpopmodder: Recorded the implemented B0.1 candidate-consistency source boundary. -->
<!-- 20260826_kpopmodder: Recorded post-commit runtime evidence and the same-target child lifecycle review failure. -->
<!-- 20260826_kpopmodder: Recorded lifecycle closure and the focused automatic deposit_all implementation. -->
<!-- 20260826_kpopmodder: Recorded that generic ResourceTask container pickup is currently source-disabled. -->
<!-- 20260829_openai: Recorded the current-source automatic pressure-deposit composition regression, evidence levels, and forward-only restoration gate without authorizing source changes. -->
<!-- 20260829_openai: Reconciled the independent review with the live source, including runtime-scoped identity, exact StoreHome root authority, initialization atomicity, runner ownership, and real TaskRunner reconciliation without authorizing implementation. -->
<!-- 20260829_kpopmodder: Recorded the authorized forward-only production composition restoration and current focused/targeted test evidence without claiming a clean build or runtime artifact. -->
<!-- 20260830_openai: Linked the focused transfer, movement-progress, and Carry On incident review while preserving the dated composition evidence ledger. -->

# ChatClef @deposit_all Ocean Loop Diagnostics Plan

문서 상태: `AUTO_PRESSURE_COMPOSITION_SOURCE_RESTORED_FOCUSED_AND_TARGETED_TESTS_PASSED_CLEAN_BUILD_AND_RUNTIME_PENDING`

작성 기준일: 2026-08-26, 상태 갱신일: 2026-08-29

> 2026-08-27 방향 안내: 사용자가 명시적으로 요청했을 때만 trusted destination에
> inventory를 정리하는 `STORE_HOME`은 이 automatic-deposit 기록과 분리한다.
> 해당 기능에는 [Manual Trusted Home Storage Direction](chatclef-manual-trusted-home-storage-direction-2026-08-27.md)이 우선한다.
>
> 2026-08-29 최신 상태 안내: §26의 automatic policy와 §28.1-§28.13의 dormant 판정은
> 역사적 근거로 유지한다. 사용자가 승인한 forward-only 복구 뒤의 현재 source/test 판정은
> §28.14가 소유한다. 현재 worktree는 pressure chain을 production runtime에 정확히 한 번
> 조립하고 exact-binding tracker 뒤에 tick한다. Clean forced build, JAR provenance와 Minecraft
> 33/36 runtime 재현은 아직 수행하지 않았다.

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
수정과 검증이 끝날 때까지 구현하지 않는 것이 기존 gate였다. 이 gate는 후속 focused
commit과 Minecraft runtime 증거로 해제됐다.

```text
lifecycle fix commit:                  673d21de Fix deposit_all store task generation stability
clean forced build:                    PASS, 96 tests, zero failures/errors/skips
build log:                             .codex-build/logs/admin-build-1.20.1-20260826-153221.log
1.20.1 JAR SHA-256:                    7327DCB083462C6EA759C2A6D224A1C13B62E02D58D0D88E4E553EC7C9E3A92C
Minecraft operation:                  store-deposit-39819
Minecraft terminal:                   NATURAL_FINISH, 53471 ms
same-target generation reuse:         observed
storeGenerationCreated=false:         120 observations
storeGenerationCreated=true:          3 observations across target epochs
old OPEN_EXISTING/OBTAIN oscillation:  not observed
durable storage effect oracle:        still not verified; effectVerified=false
```

자동 실행은 다음 LAVI-owned 경계로 구현한다.

```text
lavi/minecraft/task/container/deposit/
    DepositAllInventoryTargetSelector
        -> bare @deposit_all과 자동 실행이 같은 아이템 선택 정책을 사용

lavi/minecraft/task/container/deposit/auto/
    DepositAllInventoryPressureSnapshot
        -> 메인 인벤토리 36칸의 occupied slot snapshot과 4/5 판정
    DepositAllInventoryPressureReader
        -> client thread에서 occupied slot 수 관찰
    DepositAllInventoryPressureStateMachine
        -> ARMED -> RUNNING -> WAIT_FOR_REARM 전이
    DepositAllAutoConflictGuard
        -> 기존 수동 deposit route가 있을 때 중복 자동 Task 생성 차단
    DepositAllInventoryPressureChain
        -> 한 번의 DepositAllTask root 생성과 terminal/interruption 소유
    DepositAllAutoEntrypoint
        -> chain을 정확히 한 번 등록하고 runner 밖에서 4/5 경계를 관찰
    DepositAllAutoDiagnostics
        -> 등록, trigger, 상태 전이만 bounded logging
```

정확한 trigger 경계는 occupied main-inventory slot `29 / 36`이다. 계산은 부동소수점
비교가 아니라 `occupied * 5 >= total * 4`로 수행한다.

```text
below 4/5
    -> ARMED

ARMED + first observation at or above 4/5
    -> existing manual deposit route 확인
    -> bare deposit_all과 동일한 depositable target 계산
    -> target이 있으면 같은 DepositAllTask를 직접 한 번 생성
    -> RUNNING

RUNNING
    -> 같은 root Task instance 유지
    -> command string 실행 없음
    -> 중복 Task 생성 없음

task terminal 또는 chain interruption
    -> WAIT_FOR_REARM

WAIT_FOR_REARM + still at or above 4/5
    -> 아무 작업도 생성하지 않음

WAIT_FOR_REARM + below 4/5
    -> ARMED
```

chain priority는 `51`이다. 기존 `UserTaskChain` priority `50`의 작업은 자동 저장 동안
일시 중단됐다가 기존 scheduler lifecycle로 재개되고, food/unstuck/player-defense 등
`55+` safety chain은 자동 저장을 선점한다. safety chain이 선점하면 자동 저장은 자기
root만 중단하고 `WAIT_FOR_REARM`으로 이동한다. TaskRunner, Baritone, 전역 input, goal,
path를 직접 중단하거나 정리하지 않는다.

4/5 관찰은 Fabric end-client-tick에서 수행하므로 평상시 TaskRunner가 꺼져 있어도
threshold crossing을 놓치지 않는다. ChatClef가 켜져 있고 자동 Task를 시작해야 할 때
runner가 꺼져 있으면 기존 `TaskRunner.enable()` 경계로 한 번 활성화한다. 자동 chain은
완료나 중단 시 TaskRunner, Baritone, 전역 input, goal 또는 path를 직접 중단하거나
정리하지 않는다. ChatClef가 꺼져 있으면 새 자동 Task를 시작하지 않으며, 월드 이탈은
현재 자동 root만 종료하고 `WAIT_FOR_REARM`으로 이동한다.

현재 source 상태:

```text
automatic chain source:               implemented in current worktree
focused state-machine tests:           added, not run in this change
automatic diagnostic-source test:      added, not run in this change
clean forced build for auto change:    NOT RUN; separate authorization required
JAR deployment for auto change:        NOT RUN
Minecraft 4/5 trigger reproduction:    NOT RUN
```

`9785e17b`는 원래 후보 불일치를 제거하고 runtime 증거를 만든 중간 checkpoint로
보존한다. lifecycle 수정은 focused follow-up `673d21de`로 닫혔고, 자동 실행 source와
focused test는 현재 별도 worktree 변경으로 유지한다. force rewrite나 broad revert는
하지 않는다.

이 커밋은 behavior, diagnostics, tests, docs를 54개 파일에 함께 담아 Git commit 자체는
atomic rollback 단위가 아니다. 기존 `DoToClosestBlockTask` diagnostics hunk의 논리적
rollback 절차는 `chatclef-engine-divergence-record.md`에 유지하며, 실제 rollback 시에는
해당 hunk만 명시적으로 되돌린다.

이 26절이 재검수와 다음 수정 방향의 최종 canonical record다. 같은 내용을 다시
검수하는 새 문서, 새 진단 계획서 또는 대규모 설계 문서를 만들지 않는다. 이후에는
이 절의 status, test, build, runtime 결과만 갱신한다.

### 26.7 상자에 저장된 재료 활용의 현재 상태

현재 `ResourceTask`에는 이미 열어 캐시한 컨테이너에서 필요한 아이템을 찾고
`PickupFromContainerTask`로 회수하는 분기가 존재한다. 그러나 이 분기의 진입 조건인
`allowContainers`의 기본값은 `false`이며, 현재 source 전체에 이를 `true`로 설정하는
활성 호출은 없다. `CollectSticksTask`에는 `false`를 명시하는 호출만 존재한다.

따라서 현재 상태는 다음과 같이 기록한다.

```text
container cache and pickup implementation: SOURCE_PRESENT
ResourceTask allowContainers default:       false
active setAllowContainers(true) call:       NONE
generic resource chest pickup route:        SOURCE_DISABLED
resourceChestLocateRange effect on route:   INACTIVE_WHILE_DISABLED
```

`resourceChestLocateRange`의 기본값이 `500`인 것은 이 분기를 활성화하지 않는다.
현재 ChatClef가 컨테이너 내용을 캐시할 수 있다는 사실과, 일반 자원 획득 Task가 그
캐시에서 재료를 가져온다는 것은 별개의 상태다.

이 항목은 현재 비활성 상태만 기록한다. `allowContainers`의 변경, 전역 활성화,
새 컨테이너 활용 코드, 동작 수정, 테스트, 빌드, 배포, 커밋 또는 푸시를 승인하거나
계획하지 않는다.

### 26.8 ZIP (48) focused lifecycle 재검수 판정

이 절은 업로드된 ZIP (48)을 기준으로 전달받은 focused 재검수 결과를 기록한다.
공개 브랜치는 보조 대조에만 사용됐다. 기존 26.6의 clean build와 Minecraft
`NATURAL_FINISH` 기록은 보존하지만, 그 실행은 safety-chain interruption/resume과
automatic natural-finish cleanup을 검증하지 않았다. 따라서 release와 자동 4/5 trigger
배포에 관한 최신 판정은 이 절을 따른다.

```text
평상시 same-target progress 안정화:        PASS
stable notStored snapshot 소유:            PASS
shared Task 경계 무변경:                   PASS

수동 @deposit_all interrupt/resume:         FAIL
자동 trigger NATURAL_FINISH cleanup:        FAIL
실제 generic reconciliation 테스트:        FAIL
현재 ZIP 전체 release 승인:                REJECT
```

#### 26.8.1 Blocker 1: 수동 interrupt/resume generation ownership

현재 `DepositAllTask.onStart()`와 `DepositAllTask.onStop(Task interruptTask)`는
`_storeTaskGeneration.clear()`를 무조건 호출한다. 그러나 AltoClef의 `Task.onStop()`은
terminal stop뿐 아니라 chain의 일시 interruption에도 호출된다.

```text
helper active child = A
Task.sub actual child = A

safety chain이 UserTaskChain을 선점
    -> DepositAllTask.interrupt()
    -> onStop()에서 helper generation clear
    -> Task.sub와 interrupted child A는 보존

UserTaskChain 재개
    -> DepositAllTask.reset()
    -> Task.reset()은 Task.sub를 보존
    -> onStart()에서 helper generation clear
    -> helper가 같은 target과 snapshot으로 child B 생성
```

저장 progress 전에 이 경로가 발생하면 production `StoreInContainerTask.isEqual()`은
동일한 target, flag, snapshot을 가진 A와 B를 값 기준으로 같다고 판정한다. generic
reconciliation은 기존 A를 계속 실행하지만 helper는 실행되지 않은 B를 active
generation으로 소유한다.

```text
실제로 Task.sub에서 실행되는 child: A
generation helper가 소유하는 child: B
결과: scheduler child와 helper child identity 분리
```

이 상태가 항상 즉시 무한 루프를 만든다고 단정하지 않는다. 그러나 다음 lifecycle
불일치는 정적으로 확정된다.

1. `generationId`와 `storeGenerationCreated`가 실제 실행 child를 가리키지 않을 수 있다.
2. `clearIfFinishedWithRemainingWork()`가 실제 A 대신 실행되지 않은 B를 검사한다.
3. 실행되지 않은 B의 `storedItems`는 대개 `null`이므로 remaining-work refresh가 막힐 수 있다.
4. chain preemption 뒤 parent가 active child generation을 소유한다는 불변조건이 깨진다.

focused 후속 수정의 최소 범위는 `DepositAllTask` lifecycle에서 start/stop 시 무조건
generation을 지우지 않고 temporary interruption 동안 같은 generation을 보존하는 것이다.
generation clear는 다음 명시적 route 경계에서만 수행한다.

```text
selected target 변경 또는 무효화
outside-70
fallback 전환
finished child + verified remaining work
```

`interruptTask == null`은 terminal stop과 interruption의 구분 근거로 사용하지 않는다.
`SingleTaskChain.onInterrupt()`도 `mainTask.interrupt(null)`을 호출하기 때문이다.

#### 26.8.2 Blocker 2: 자동 NATURAL_FINISH cleanup

현재 `DepositAllInventoryPressureChain.onTaskFinish()`는 finished root의 참조를
`mainTask = null`로 직접 버린다. 이 경로는 root `Task.stop()`을 호출하지 않으므로
다음 cleanup이 생략된다.

```text
DepositAllTask.onStop()
    -> root ContainerStoredTracker.stopTracking()
    -> target, generation, progress cleanup

Task.stop()의 recursive child stop
    -> StoreInContainerTask.onStop()
    -> child ContainerStoredTracker.stopTracking()
    -> active descendant cleanup
```

`ContainerStoredTracker.startTracking()`은 전역 EventBus subscription을 등록하고,
`stopTracking()`에서만 해제한다. 따라서 자동 실행이 자연 완료할 때 root와 child의
stale listener가 남고, 재무장 후 반복 실행에서 누적될 수 있다.

focused 후속 수정은 `onTaskFinish()`에서 직접 `mainTask = null`을 쓰는 대신 기존
`SingleTaskChain.setTask(null)` 또는 동등한 root-only stop-and-clear 경계를 사용하는
것이다. 이 경계는 자동 chain이 소유한 root와 descendant만 종료해야 한다.

다음 전역 정리는 추가하지 않는다.

```text
TaskRunner.disable()
Baritone global cancel
전역 input clear
UserTaskChain 중단
전역 goal 또는 path cleanup
```

#### 26.8.3 현재 테스트 공백과 필수 검증

현재 `DepositAllTaskStoreGenerationTest`는
`storeTaskForSelectedTarget(...)` helper를 직접 호출하므로 다음 production lifecycle을
통과하지 않는다.

```text
DepositAllTask.onStart() / onStop()
Task.interrupt() / reset() / tick()
Task.sub reconciliation
previous child stop
actual child replacement
```

또한 test double의 `isEqual()`은 identity 비교지만 production
`StoreInContainerTask.isEqual()`은 target, flag, snapshot의 값 동등성을 사용한다.
따라서 서로 다른 A/B instance가 production equality에서 같아지는 이번 결함을 현재
테스트로 재현할 수 없다.

수동 경로의 필수 generic reconciliation 테스트:

1. same-target child A를 시작한다.
2. deposit progress 전에 parent를 interrupt한다.
3. parent를 reset하고 resume한다.
4. helper active child와 실제 `Task.sub`가 모두 A인지 확인한다.
5. phantom child B 생성과 generation ID 증가가 없는지 확인한다.
6. previous child replacement와 stop이 모두 0인지 확인한다.
7. resume 뒤 실제 child terminal과 root remaining-work refresh가 정상인지 확인한다.

자동 경로의 필수 chain 테스트:

1. automatic root가 자연 완료한다.
2. root `onStop()`이 정확히 한 번 호출된다.
3. active child stop이 정확히 한 번 호출된다.
4. root와 child tracking subscription이 해제된다.
5. `mainTask`가 `null`이고 상태가 `WAIT_FOR_REARM`인지 확인한다.
6. TaskRunner와 전역 Baritone cleanup이 호출되지 않았는지 확인한다.

기존 자동 테스트가 확인한 28/36, 29/36 threshold와
`ARMED -> RUNNING -> WAIT_FOR_REARM`, 고점 중복 trigger 방지, 저점 rearm은 유효하다.
그러나 natural-finish cleanup, safety-chain interruption, UserTaskChain resume, conflict
guard, entrypoint 단일 등록은 아직 release 증거가 아니다.

#### 26.8.4 보존 경계와 release gate

이번 재검수에서 다음 방향은 통과했다.

1. 정상 same-target progress에서 같은 `StoreInContainerTask` instance를 재사용한다.
2. target 채택 당시 `notStored`를 복사해 stable snapshot으로 고정한다.
3. target 변경, 무효화, fallback, finished child와 remaining work를 generation 교체 경계로 둔다.
4. bare `@deposit_all`과 자동 실행이 `DepositAllInventoryTargetSelector`를 공유한다.
5. `occupied * 5 >= total * 4`와 29/36 trigger 경계가 정확하다.
6. safety chain 선점 시 자동 chain이 `stopOwnedRun()`으로 자기 root만 중단한다.

다음 shared 경계는 수정하지 않는다.

```text
Task.java
StoreInContainerTask.java and StoreInContainerTask.isEqual()
StoreInAnyContainerTask.java
DepositCommand.java
DoToClosestBlockTask.java
TaskRunner and Baritone
UserTaskChain
```

기존 `9785e17b`와 `673d21de`는 force rewrite하지 않는다. interruption 수정은 focused
follow-up으로 분리한다. 위 두 blocker와 orchestration test가 닫히기 전까지 자동 4/5
trigger 변경은 commit, clean forced build, JAR 배포 또는 Minecraft release 대상으로
승인하지 않는다.

이 절은 새 동작 수정, 테스트 실행, 빌드, 배포, 커밋 또는 푸시를 승인하지 않는다.
새 검수 문서나 새 로그 계획서를 만들지 않고 이후 판정과 증거도 이 canonical 26절만
갱신한다.

#### 26.8.5 focused lifecycle follow-up source 상태

사용자 승인에 따라 두 blocker의 focused source 수정과 검증 source를 현재 worktree에
적용했다. shared Task lifecycle과 기존 `@deposit` 경계는 변경하지 않았다.

```text
DepositAllTask start/stop generation preservation:      IMPLEMENTED
automatic NATURAL_FINISH root stop-and-clear:           IMPLEMENTED
automatic entrypoint duplicate-registration guard:     IMPLEMENTED
production-value-equality helper test double:           UPDATED
generic interrupt/reset/reconciliation test:            ADDED, NOT RUN
automatic natural-finish cleanup test:                  ADDED, NOT RUN
automatic conflict-guard test:                          ADDED, NOT RUN
automatic entrypoint single-registration test:          ADDED, NOT RUN
clean forced build:                                     NOT RUN
JAR deployment and Minecraft reproduction:              NOT RUN
release verdict:                                        REJECT UNTIL VERIFIED
ResourceTask allowContainers:                           false, UNCHANGED
```

`DepositAllTask`는 lifecycle `onStart()`와 `onStop()`에서 generation을 무조건
clear하지 않는다. target 무효화, fallback, outside-70, finished child와 verified
remaining work 같은 기존 explicit route 경계의 clear는 유지한다.

`DepositAllInventoryPressureChain.onTaskFinish()`는 finished root 참조를 보관한 뒤
`setTask(null)`을 호출해 root와 active descendant를 기존 scheduler 경계로 정리하고,
그 다음 `WAIT_FOR_REARM`으로 전이한다. TaskRunner, Baritone, 전역 input, goal 또는 path
cleanup은 추가하지 않았다.

검증 source는 helper와 orchestration 책임을 분리한다. generic reconciliation 테스트는
실제 `Task.tick()`, `interrupt()`, `reset()`과 retained `Task.sub`를 통과하며, automatic
chain 테스트는 자연 완료 시 root와 child stop 및 tracker cleanup을 확인하도록 작성했다.
테스트 실행과 clean forced build는 별도 승인 전까지 수행하지 않는다.

### 26.9 자동 저장 working-set 보존과 bounded recovery 구현 계약

<!-- 20260826_kpopmodder: Recorded the canonical implementation direction for preserving an active user task's working set during automatic deposit. -->

이 절은 인벤토리 4/5 자동 저장이 활성 사용자 Task의 작업 재료를 함께 저장한 뒤 같은
재료를 처음부터 다시 채집하는 문제의 canonical 구현 방향이다. 치명적인 방향 오류는
없으며 별도의 새 설계 문서나 반복적인 docs-only 재검수는 만들지 않는다. 이후 구현,
테스트, build 및 runtime 결과는 이 26절에만 이어서 기록한다.

현재 root cause는 다음과 같이 확정한다.

```text
automatic chain priority:                 51
UserTaskChain priority:                   50
current automatic target policy:          armor slots와 ToolItem만 제외
active UserTask working-set reservation:  없음

결과:
automatic deposit_all이 활성 UserTask를 선점
    -> 다이아몬드, 원목, 판자, 음식과 제작 중간재까지 저장
    -> 동일 UserTask root가 재개돼도 필요한 재료가 사라짐
    -> 기존 자원 수집 fallback이 처음부터 다시 실행됨
```

따라서 근본 수정은 주변 상자를 먼저 뒤지는 기능이 아니라 자동 저장이 활성 UserTask의
working set을 침범하지 않게 하는 것이다. 상자 회수는 reservation 누락, 작업 단계 전환,
또는 저장 후 검증에서 확인된 부족분만 보완하는 후순위 경로다.

#### 26.9.1 단계별 적용 순서

구현은 다음 순서를 고정한다.

```text
Phase 1: active non-idle UserTask 중 destructive full deposit 보류
Phase 2: immutable working-set reservation 계산 후 surplus만 저장
Phase 3: destination manifest -> cache -> 실제 상자 순회로 부족분 회수
```

Phase 1은 안전 차단책이다. 활성 사용자 작업 중 자동 `DepositAllTask`를 만들지 않으므로
현재 작업 재료를 모두 잃는 회귀를 즉시 막는다. 다만 인벤토리가 `36 / 36`까지 찰 수
있으므로 최종 동작은 아니며, Phase 2에서 안전하게 저장 가능한 surplus만 선택해 공간을
확보한다.

Phase 3은 일반 자원 계획기를 새로 만드는 단계가 아니다. 우선 이번 자동 operation 때문에
발생한 부족분만 복구한다. snapshot 시점부터 이미 부족했던 자원까지 회수하는 확장은 이
경로가 안정화된 뒤 별도로 평가하며, 회수 실패 시 기존 UserTask의 일반 채집 및 제작
fallback을 그대로 사용한다.

#### 26.9.2 threshold 신호를 소비하지 않는 defer 규칙

현재 `DepositAllInventoryPressureStateMachine`은 `WAIT_FOR_REARM`에서 인벤토리가 임계치
아래로 내려가야만 `ARMED`로 돌아간다. 활성 Task를 이유로
`markThresholdSuppressed()` 또는 `transitionArmedToWaiting()`을 호출하면, 그 Task가
끝난 뒤에도 인벤토리가 계속 4/5 이상인 경우 자동 저장이 다시 실행되지 않을 수 있다.

따라서 인벤토리가 임계치 이상이고 아직 안전한 자동 저장 plan을 만들 수 없으면
`observe()`보다 먼저 defer한다.

```text
snapshot = pressureReader.read(...)

snapshot < 4/5
    -> active Task 여부와 관계없이 stateMachine.observe(snapshot)
    -> WAIT_FOR_REARM의 정상 rearm 허용

snapshot >= 4/5 + active non-idle UserTask + Phase 1
    -> log DEFERRED_ACTIVE_USER_TASK
    -> observe() 호출 없음
    -> threshold 신호 소비 없음

snapshot >= 4/5 + Phase 2 safe plan 생성 성공
    -> stateMachine.observe(snapshot)
    -> THRESHOLD_REACHED일 때만 operation 시작
```

Phase 2에서도 다음 상태는 모두 fail-closed defer이며 threshold 신호를 소비하지 않는다.

```text
current selected chain != UserTaskChain
UserTask root 또는 실행 경로 snapshot 불일치
지원하지 않는 active Task 포함
안전하게 저장할 surplus 없음
working-set 계산 중 월드, 차원 또는 root identity 변경
```

수동 `@deposit_all`, 기존 manual deposit conflict 처리, 저점 rearm 및 한 번 실행 후
`WAIT_FOR_REARM` 전이는 이 defer 규칙과 별개로 유지한다.

#### 26.9.3 immutable WorkingSetSnapshot

정확한 reservation 단위는 단순한 active root의 `ItemTarget`이 아니라 자동 operation
전용 immutable `WorkingSetSnapshot`이다. 자동 chain이 UserTask를 선점하기 전, 다음
조건이 참일 때만 snapshot을 만든다.

```text
runner.getCurrentTaskChain() == userTaskChain
```

snapshot은 다음 값을 즉시 복사해 소유한다.

```text
UserTask root object identity
List.copyOf(userTaskChain.getTasks())로 복사한 root-to-leaf 실행 경로
world와 dimension identity
snapshot epoch
현재 인벤토리의 concrete Item별 수량
현재 BotBehaviour.isProtected(item) 결과
활성 Task들의 명시적 목표 수량
현재 제작, 제련 및 수집 경로의 입력 재료와 중간재 수량
resolver support 결과: SUPPORTED 또는 UNSUPPORTED
```

`TaskChain.getTasks()`는 tick마다 비워지고 다시 채워지는 실행 캐시이므로 live 참조를
보관하지 않는다. 다른 고우선순위 chain이 이미 선택된 상태에서는 실행 경로와 보호
정보가 낡았을 수 있으므로 snapshot을 추정하지 않고 defer한다.

전역 `Task` 또는 `ResourceTask` interface는 변경하지 않는다. LAVI-owned resolver가
concrete Task adapter를 통해 이미 노출된 정보를 읽는다.

```text
ResourceTask
    -> getItemTargets()

CraftInTableTask
    -> getRecipeTargets(), 남은 제작 횟수와 입력 재료

SmeltInFurnaceTask / SmeltInSmokerTask / SmeltInBlastFurnaceTask
    -> getTargets(), 입력 재료와 연료

명시적으로 지원한 특수 Task
    -> 해당 adapter의 보수적 requirement

지원하지 않는 Task
    -> reserve none이 아니라 UNSUPPORTED
    -> 자동 저장 defer
```

root의 최종 결과물만 보는 것은 허용하지 않는다. 예를 들어 갑옷 제작 Task의 최종
`ItemTarget`만으로는 현재 필요한 다이아몬드, 판자, 원목 또는 crafting intermediate를
보존할 수 없다. root 목표와 현재 active leaf까지의 즉시 입력을 함께 합산한다.

현재 `BotBehaviour`가 보호하는 concrete Item은 현재 보유 수량 전체를 예약한다. 여러
`ItemTarget`의 match 범위가 겹칠 때 V1은 과소 예약보다 보수적인 과다 예약을 허용한다.
공간 최적화보다 작업 재료 보존을 우선한다.

각 concrete Item의 기본 수량 계약은 다음과 같다.

```text
reservedCount = min(preDepositInventoryCount, requiredWorkingSetCount)
surplusCount  = max(0, currentInventoryCount - reservedCount)

depositCount <= surplusCount
```

#### 26.9.4 수동 full-deposit과 자동 surplus 선택 분리

현재 `DepositAllInventoryTargetSelector`에 reservation을 넣지 않는다. 이 selector를
수정하면 수동 `@deposit_all`의 기존 full-deposit 의미까지 바뀐다.

새 LAVI-owned 책임은 다음처럼 분리한다.

```text
lavi/minecraft/task/container/deposit/auto/working/
    WorkingSetSnapshot
        -> immutable root, path, inventory, requirement와 reservation 결과

    ActiveTaskWorkingSetResolver
        -> concrete Task adapter를 조합하고 SUPPORTED/UNSUPPORTED 판정

    AutoDepositSurplusTargetSelector
        -> WorkingSetSnapshot의 reserved count를 제외한 surplus ItemTarget 생성

    adapter/
        -> ResourceTask, crafting, smelting 등 지원 Task별 requirement 해석
```

기존 selector와 새 selector의 계약은 다음과 같다.

```text
DepositAllInventoryTargetSelector
    -> 수동 @deposit_all의 기존 full-deposit 정책 유지

AutoDepositSurplusTargetSelector
    -> 활성 UserTask가 있는 자동 operation에서 surplus만 선택
```

지원하지 않는 Task, snapshot 불일치 또는 surplus 0개를 기존 full-deposit selector로
fallback하지 않는다. 해당 tick의 자동 저장을 defer한다.

#### 26.9.5 단방향 자동 maintenance operation

Phase 2부터 자동 chain은 한 번의 operation을 소유하는 LAVI-owned parent Task를 사용한다.

```text
lavi/minecraft/task/container/deposit/auto/maintenance/
    AutoDepositMaintenanceTask
    AutoDepositMaintenancePhase

SNAPSHOT
    -> DEPOSIT_SURPLUS
    -> VERIFY_WORKING_SET
    -> deficit == 0: DONE
    -> deficit > 0: RECOVER
    -> DONE
```

`DEPOSIT_SURPLUS`는 기존 `DepositAllTask`에 surplus `ItemTarget[]`만 전달해 재사용한다.
명령 문자열을 다시 실행하거나 새로운 UserTask root를 만들지 않는다.

phase는 단방향이며 다음 전이를 금지한다.

```text
RECOVER -> DEPOSIT_SURPLUS
RECOVER -> 새 automatic operation
동일 후보 무제한 재방문
```

회수한 품목은 동일 auto epoch에서 다시 deposit 대상이 될 수 없다. `RECOVER` 진입과
동시에 deposit phase는 영구적으로 닫힌다.

#### 26.9.6 confirmed destination manifest

기존 `ContainerStoredTracker`는 실제 slot delta를 품목별 총량으로 추적하지만 destination
좌표는 보존하지 않는다. 자동 operation은 별도의 LAVI-owned manifest tracker를 사용해
실제로 저장이 확인된 항목만 기록한다.

```text
lavi/minecraft/task/container/deposit/auto/recovery/
    AutoDepositDestinationManifest
    AutoDepositDestinationManifestTracker

manifest entry:
    world/dimension
    container BlockPos
    concrete Item
    confirmed positive slot delta
    automatic operation epoch
```

manifest는 클릭 요청, GUI open 요청 또는 캐시 예상값만으로 만들지 않는다. 실제 target
container에서 확인된 positive slot delta만 기록한다. `StoreInContainerTask.isEqual()`을
변경하지 않으며, operation-local tracker가 이미 선택된 target 좌표와 slot-change event를
상관시킨다.

#### 26.9.7 Container Tooltips 없는 bounded recovery

회수 후보의 방문 우선순위는 다음과 같다.

```text
1. 이번 automatic operation의 confirmed destination manifest
2. 기존 ContainerCache가 요청 품목을 가진다고 주장하는 현재 차원 위치
3. BlockScanner에서 발견한 제한 거리 내의 나머지 컨테이너
```

이 순서는 신뢰 순서가 아니라 방문 우선순위다. manifest와 cache도 실제 현재 수량의
권위 있는 증거가 아니다. 모든 후보는 물리적으로 이동해 열고, 서버가 보낸 GUI slot을
확인한 뒤에만 회수한다. 따라서 Container Tooltips 또는 다른 서버 모드가 필요하지 않다.

후보 정책은 다음을 보장한다.

```text
현재 world와 dimension만 사용
설정된 제한 거리 안의 지원 컨테이너만 사용
tier 사이 동일 BlockPos deduplicate
보호 대상 및 접근 불가능 후보 제외
GUI 미수신, stale cache, 실제 수량 부족은 bounded retry 후 blacklist
blacklist는 operation-local
world 또는 dimension 변경 시 operation, queue와 blacklist 폐기
```

기존 `PickupFromContainerTask`는 특정 좌표의 컨테이너를 열고 GUI slot에서 품목을
회수하므로 재사용할 수 있다. 다만 target count는 이번에 가져올 delta가 아니라 회수 뒤
인벤토리에 있어야 할 절대 수량으로 전달한다.

```text
pickupTargetCount = currentInventoryCount + currentWorkingSetDeficit
withdrawCount <= currentWorkingSetDeficit
```

destination manifest 후보에는 다음 상한도 적용한다.

```text
withdrawCountAtDestination <= confirmedDepositedCountAtDestination
```

cache 또는 새로 발견한 컨테이너에는 manifest 상한이 없지만 GUI에서 확인한 실제 수량과
현재 부족분보다 많이 가져오지 않는다.

회수 lifecycle은 별도 Task가 소유한다.

```text
lavi/minecraft/task/container/deposit/auto/recovery/
    RecoverReservedItemsTask
        -> immutable WorkingSetSnapshot
        -> remaining deficits
        -> ordered and deduplicated candidate queue
        -> current candidate
        -> stable open/pickup child
        -> per-candidate retry와 timeout
        -> operation-local blacklist
        -> SATISFIED / EXHAUSTED / CANCELLED
```

후보 queue는 operation 시작 또는 tier 전환 시 한 번만 만든다. 현재 후보의 child는 성공
또는 terminal failure까지 같은 instance를 유지한다. 실패한 후보를 blacklist한 뒤 다음
후보로 단방향 이동하며, 모든 후보가 끝나면 `EXHAUSTED`로 종료하고 기존 UserTask가 자체
채집 fallback을 계속한다.

#### 26.9.8 interruption과 원래 UserTask 재개 계약

automatic operation 시작 전 UserTask root object identity를 snapshot한다. 자동 저장을
위해 명령을 다시 실행하거나 `runUserTask()`로 새 root를 만들지 않는다.

현재 ChatClef interruption은 진정한 freeze/resume가 아니다.

```text
higher-priority chain selected
    -> UserTaskChain.onInterrupt()
    -> 동일 root와 child에 interrupt()
    -> onStop()

UserTaskChain selected again
    -> 동일 root object reset()
    -> 다음 tick에 onStart()
```

따라서 검증 계약은 다음과 같다.

```text
UserTask root identity:                 MUST PRESERVE
command/callback ownership:             MUST PRESERVE
command re-execution:                    MUST NOT OCCUR
new UserTask root creation:              MUST NOT OCCUR
child identity and open GUI progress:    NOT GUARANTEED
onStop/onStart re-entry:                 EXPECTED ENGINE SEMANTICS
```

working-set snapshot은 반드시 interrupt 전에 외부 immutable 객체로 완성한다. safety chain
등 더 높은 priority가 automatic maintenance를 중단하면 V1은 부분 operation을 재개하지
않는다. 현재 automatic root, manifest, queue와 blacklist를 terminal abort하고 원래
UserTask에 fallback한다. TaskRunner, UserTaskChain, Baritone, 전역 input, goal 또는 path를
직접 취소하거나 정리하지 않는다.

#### 26.9.9 보호 경계

다음 shared 또는 upstream-derived 경계는 이 구현에서 변경하지 않는다.

```text
ResourceTask.allowContainers == false
TaskCatalogue
CollectRecipeCataloguedResourcesTask
CollectPlanksTask and MineAndCollectTask
DepositCommand.java
StoreInAnyContainerTask.java
StoreInContainerTask.java and StoreInContainerTask.isEqual()
Task.java
TaskRunner and UserTaskChain
DoToClosestBlockTask.java
Baritone
```

전역 `allowContainers=true`, static/global resource policy, 전체 `@get` 의미 변경 또는
Container Tooltips hard dependency를 추가하지 않는다. 새 책임은 LAVI-owned automatic
deposit package 안에서 composition으로 분리한다.

수동 `@deposit_all`은 기존 full-deposit 의미를 유지한다. 일반 `@get`과 기존 `@deposit`
동작도 변경하지 않는다.

#### 26.9.10 필수 검증 계약

Phase 1:

1. non-idle `@get` 실행 중 occupied slot이 `29 / 36` 이상이어도 자동
   `DepositAllTask` 생성 수가 0이다.
2. UserTask root identity가 유지되고 command가 다시 실행되지 않는다.
3. state가 잘못 `WAIT_FOR_REARM`으로 이동하지 않고 threshold 신호가 보존된다.
4. UserTask 종료 후에도 `29 / 36` 이상이면 다음 tick에 자동 저장을 시작한다.

Phase 2:

1. 인벤토리 `36 / 36`에서 active recipe의 원목, 판자, 입력 재료와 중간재는 예약량
   이하로 저장되지 않는다.
2. junk surplus만 저장하고 최소 한 slot을 확보한다.
3. unsupported Task, stale snapshot 또는 surplus 없음은 full deposit으로 fallback하지 않는다.
4. `depositCount(item) <= inventoryCount(item) - reservedCount(item)`을 모든 품목에 대해
   만족한다.
5. protected item의 손실이 0이고 수동 `@deposit_all` 결과는 기존과 같다.

Phase 3:

1. destination manifest가 실제 좌표와 confirmed positive delta만 기록한다.
2. 보호되거나 접근 불가능한 manifest 후보는 bounded retry 뒤 blacklist한다.
3. stale cache 후보를 연 뒤 GUI 불일치를 확인하면 다음 후보로 한 번만 이동한다.
4. 다음 미확인 컨테이너의 실제 GUI에서 품목을 찾으면 정확한 부족분만 회수한다.
5. manifest, cache와 주변 후보가 모두 실패하면 `EXHAUSTED`로 종료하고 동일 UserTask
   root가 기존 채집 fallback을 계속한다.
6. 같은 좌표를 반복 방문하지 않고 동일 auto epoch에서 회수 품목을 다시 저장하지 않는다.
7. `ResourceTask.allowContainers`는 `false`, 일반 `@get` 동작은 기존 상태를 유지한다.

runtime 검증에서는 다음 경계를 하나의 auto operation epoch로 연결한다.

```text
threshold observed or deferred
-> working-set snapshot result
-> surplus plan
-> DepositAllTask start and terminal
-> post-deposit deficit verification
-> manifest/cache/scanner candidate tier
-> GUI revalidation
-> exact inventory increase or candidate failure
-> recovery terminal reason
-> original UserTask root resume
```

로그는 기존 bounded diagnostics 정책을 따르며 후보별 매 tick 출력이나 무제한 slot dump를
추가하지 않는다.

#### 26.9.11 현재 상태와 다음 작업 gate

```text
root-cause direction:                              ACCEPTED
threshold-latch defer rule:                       DOCUMENTED
WorkingSetSnapshot and surplus contract:          DOCUMENTED
bounded physical container recovery:              DOCUMENTED
Container Tooltips dependency:                    NOT REQUIRED
ResourceTask.allowContainers global true:         REJECTED
runtime source implementation for this section:   IMPLEMENTED, NOT BUILT
focused tests for this section:                   ADDED, NOT RUN
clean forced build and Minecraft reproduction:    NOT RUN
```

이 절은 구현 방향을 확정하지만 source 수정, 테스트 실행, build, JAR 배포, Minecraft 실행,
commit 또는 push 자체를 승인하지 않는다. 사용자가 구현을 별도로 요청하면 추가 설계 문서나
동일 내용의 재검수 문서를 만들지 않고 다음 focused 순서로 진행한다.

```text
1. active non-idle + threshold high: 신호를 소비하지 않고 defer
2. auto-only working-set resolver와 surplus selector
3. immutable AutoDepositMaintenanceTask: DEPOSIT_SURPLUS -> VERIFY
4. confirmed destination manifest
5. manifest -> cache -> physical traversal bounded recovery
```

### 26.9.12 2026-08-26 implementation record

The implementation is contained in the existing LAVI-owned automatic deposit boundary.

```text
lavi/minecraft/task/container/deposit/auto/working/
    -> immutable working-set resolution and concrete-item surplus selection

lavi/minecraft/task/container/deposit/auto/maintenance/
    -> one-way DEPOSIT_SURPLUS -> VERIFY_WORKING_SET -> RECOVER lifecycle

lavi/minecraft/task/container/deposit/auto/recovery/
    -> confirmed destination manifest, current-dimension bounded candidates,
       physical GUI revalidation, exact deficit withdrawal, and operation-local exhaustion
```

`DepositAllInventoryPressureChain` now defers before `stateMachine.observe()` when an active
non-idle user task cannot produce a supported, stable working-set plan or has no safe surplus.
When a plan is available, the chain passes only the surplus targets to
`AutoDepositMaintenanceTask`. Recovery never transitions back to deposit in the same operation.

The following boundaries remain unchanged by this implementation:

```text
ResourceTask.allowContainers == false
manual @deposit_all target selection
DepositCommand and StoreInAnyContainerTask
TaskCatalogue and ordinary @get behavior
StoreInContainerTask and StoreInContainerTask.isEqual()
Task, TaskRunner, UserTaskChain, DoToClosestBlockTask, and Baritone
```

Focused source tests were added for reservation arithmetic, crafting-input and protected-item
requirements, conservative alternative matching, surplus bounds, snapshot immutability, and
confirmed manifest accounting. No Gradle command, clean build, JAR deployment, Minecraft launch,
commit, or push was performed as part of this implementation step.

### 26.10 자동 deposit 상시 보호, reserve와 신뢰 저장소 정책 계약

<!-- 20260826_kpopmodder: Recorded the canonical automatic-deposit retention policy after runtime evidence showed valuables, equipment, logs, and fuel entering the surplus plan. -->

이 절은 `get ladder 1` 중 자동 저장 대상에 `diamond`, `diamond_chestplate`,
`dark_oak_log`, `coal` 등이 포함된 runtime 관측을 바탕으로 자동 저장의 다음 정책 경계를
확정한다. 현재 working-set만 기준으로 보면 이 결과는 구현 오류가 아니라 정책의 결과다.
그러나 자동 저장의 목적은 수동 명령과 다르므로, 작업에 당장 필요하지 않다는 이유만으로
장비, 고유 스택, 상시 휴대 자원과 귀중품까지 임의의 상자에 넣는 것은 허용하지 않는다.

이 절이 26.9의 canonical 후속 계약이다. 같은 내용을 별도 설계 문서로 복제하거나
반복적인 docs-only 재검수 문서를 만들지 않는다. 이후 source 구현, test, build와 runtime
증거도 방향이 바뀌지 않는 한 이 26절에만 이어서 기록한다.

#### 26.10.1 수동 명령과 자동 안전장치의 의미 분리

두 진입점의 계약은 다음처럼 고정한다.

```text
manual @deposit_all
    -> 사용자가 명시적으로 요청한 광범위한 저장
    -> 기존 DepositAllInventoryTargetSelector 의미 유지

automatic deposit
    -> 현재 작업을 망치지 않으면서 inventory pressure를 완화하는 안전장치
    -> active 또는 idle UserTask 여부와 관계없이 자동 전용 보호 정책 적용
```

자동 경로는 다음 세 보호 계층과 목적지 제약을 합성한다.

```text
stack-level hard protection
current working-set reservation
category reserve lower bound
conditional destination policy for fungible valuables
```

일반적인 수량형 아이템의 보호량은 합산이 아니라 최댓값이다.

```text
protectedCount(item) = min(
    currentInventoryCount(item),
    max(currentWorkingSetCount(item), categoryReserveAllocation(item))
)

depositableCount(item) = max(
    0,
    currentInventoryCount(item) - protectedCount(item)
)
```

예를 들어 작업이 원목 16개를 요구하고 원목 category reserve도 16개이면 32개가 아니라
16개를 보호한다. Reserve는 작업이 소비하지 못하는 영구 재고가 아니며, 자동 저장이
침범하지 않는 하한선이다. 보유량이 reserve보다 적어도 자동 저장이 새 자원을 제작하거나
채집해서 reserve를 채우지 않는다.

Hard-protected stack은 위 수량 계산보다 먼저 제외한다. 귀중품은 별도의 신뢰 목적지
조건까지 만족할 때만 depositable이 된다.

#### 26.10.2 문서화 시점의 현재 source 사실

이 절을 기록한 시점의 로컬 working tree는 다음 상태다.

```text
DepositAllInventoryPressureSnapshot threshold:  9 / 10
36-slot trigger boundary:                        33 occupied slots
rearm implementation:                            동일 threshold 아래, 즉 32 이하
active UserTask automatic policy:                Item별 working-set reservation
idle automatic policy:                           manual full-deposit selector로 fallback
inventory snapshot granularity:                  Map<Item, Integer>
automatic surplus exclusion:                     ToolItem 전체
BotBehaviour protection:                         Item별 보유 수량 reserve
NO_SAFE_SURPLUS behavior:                        changed-only defer log, state latch 없음
trusted destination registry:                    없음
success metric:                                  저장 ItemTarget 중심, 확보 슬롯 목표 없음
```

`DepositAllInventoryPressureSnapshot.java`와 관련 테스트의 9/10 변경은 현재 수정된 working
tree에서 확인했다. 26.9의 4/5와 `29 / 36` 표현은 당시 구현 및 검수의 역사적 기록으로
남긴다. 이 절 이후의 목표 정책은 `33 / 36` high-water trigger를 기준으로 한다.

현재 `PlayerInventorySnapshotReader`는 동일 `Item`의 여러 stack을 총수량으로 합치고,
`AutoDepositSurplusTargetSelector`는 `ItemTarget`을 만든다. 따라서 평범한
`diamond_chestplate`와 이름 또는 인챈트가 있는 `diamond_chestplate`를 현재 plan만으로는
구분할 수 없다.

또한 `StoreInContainerTask`는 `ItemTarget` match로 source slot을 고르므로 같은 Item의
보호 stack과 저장 가능 stack이 함께 있을 때 aggregate target만 전달하면 보호 stack을
고르지 않는다고 증명할 수 없다. Stack-level 보호를 구현했다고 판정하려면 planning뿐
아니라 실제 transfer source 선택까지 같은 stack disposition을 강제해야 한다.

이 절의 새 정책은 아직 source에 구현되지 않았다. 위 현재 사실과 아래 목표 계약을
혼동하지 않는다.

#### 26.10.3 immutable automatic deposit plan

자동 chain은 threshold를 소비하거나 UserTask를 선점하기 전에 immutable plan을 완성한다.
Plan의 판정 순서는 다음과 같다.

```text
1. inventory stack/slot snapshot과 context identity 고정
2. hard-protected stack 분류
3. current UserTask working-set 요구량 계산
4. automatic deposit 자체의 최소 운영 working-set 계산
5. category reserve를 가능한 최소 stack 수에 배정
6. 귀중품 destination eligibility 판정
7. exact deposit source와 목표 free-slot 수 산출
8. context와 stack 상태 재검증 후 operation 시작
```

Plan은 최소한 다음 disposition을 구분한다.

```text
HARD_PROTECTED
WORKING_SET_RESERVED
CATEGORY_RESERVED
CONDITIONAL_VALUABLE
DEPOSITABLE_SURPLUS
UNCLASSIFIED_CONSERVATIVE
```

한 stack이 여러 조건에 해당하면 가장 보수적인 disposition을 사용한다. 지원하지 않는 Task,
불완전한 recipe requirement, context 변경, stack 재검증 실패는 보호 항목 없음으로 해석하지
않고 fail-closed한다. Canonical ID, Minecraft tag/runtime category 또는 별도 automatic policy로
안전하게 분류할 수 없는 vanilla/modded item도 일반 surplus로 추정하지 않고
`UNCLASSIFIED_CONSERVATIVE`로 자동 저장에서 제외한다.

활성 UserTask working-set 외에도 자동 저장 자신이 필요한 최소 운영 자원을 포함한다.
사용 가능한 destination이 없어 상자를 제작하고 배치해야 한다면 상자 1개 또는 이를 만들
최소 목재를 같은 operation에서 먼저 저장하지 않도록 보호한다.

같은 UserTask epoch에서 방금 저장했다가 recovery 또는 일반 Task로 다시 얻은 item은 같은
epoch의 다음 자동 plan에서 즉시 다시 저장하지 않는다. Recovery가 반복되면 정상 복구로
간주하지 않고 surplus 분류 오류로 판정한다.

#### 26.10.4 stack-level hard protection

Hard protection은 Item 종류 전체가 아니라 개별 stack과 slot 상태를 먼저 판정한다.

기본 hard-protected 대상은 다음과 같다.

```text
현재 장착 중인 모든 방어구
현재 offhand stack
현재 선택된 main-hand/hotbar stack
기능별 주력 도구 1개: pickaxe, axe, shovel
주력 근접 무기 1개
현재 사용 대상으로 선택된 shield, bow, crossbow 또는 trident
현재 사용 대상으로 선택된 각 방어구 slot의 주력 장비 1개
BotBehaviour.isProtected(item)에 해당하는 보유 stack
사용자가 명시적으로 pin한 stack
보존 가치가 있는 이름, enchantment, lore, custom attribute 또는 CustomModelData stack
작성된 책, 위치 정보가 있는 지도, 내용물이 있는 portable container
```

NBT나 이름이 없어도 희소하거나 회수 비용이 큰 기능성 품목은 별도 rare-item policy로
분류한다. 예를 들어 `elytra`, `totem_of_undying`, `netherite_upgrade_smithing_template`,
`enchanted_golden_apple` 같은 품목은 현재 장착, 선택 또는 pin 상태이면 `HARD_PROTECTED`,
그 외의 평범한 예비품이면 trusted destination 전용으로 둔다.

모든 `ToolItem`이나 모든 방어구를 무조건 보호하지 않는다. 기능별로 가장 적합하고 사용
가능한 주력품 한 개를 보호하고, 평범한 중복품은 다른 조건이 없으면 surplus가 될 수 있다.

`diamond_chestplate`의 예시는 다음과 같다.

```text
현재 장착, 유일한 주력품 또는 가장 좋은 주력품  -> HARD_PROTECTED
이름, enchantment 또는 특별 속성 존재            -> HARD_PROTECTED
평범한 중복 예비품                                 -> trusted destination에 저장 가능
```

단순 `hasNbt()`는 hard protection의 충분조건으로 사용하지 않는다. Damage, 표준 potion
state 등 정상 게임 상태도 NBT/component로 표현될 수 있으므로 고유성과 보존 가치가 있는
metadata를 구분해야 한다.

Stack-level transfer를 바로 제공할 수 없는 첫 safe slice에서는 동일 Item 종류에
hard-protected stack과 일반 stack이 섞여 있으면 그 Item 종류 전체를 자동 저장에서
제외한다. Aggregate `ItemTarget`만으로 평범한 stack만 옮긴다고 추정하지 않는다.
향후 exact stack-aware transfer가 필요하면 자동 전용 LAVI-owned 경계에 두며 shared
`StoreInContainerTask`의 generic 의미를 변경하지 않는다.

#### 26.10.5 category reserve 기본 계약

Reserve는 개별 item ID마다 반복 적용하지 않고 category 전체에 배정한다. 여러 원목과
여러 음식 종류가 각각 slot을 차지하도록 보호하지 않으며, 가능한 한 적은 종류와 stack에
집중한다.

V1 초기 정책 기준은 다음과 같다.

| Category | 기본 reserve | 조건 |
| --- | ---: | --- |
| 안전한 건축 블록 | 64 | 조약돌, 흙, 네더랙 등 한 종류 우선. 모래와 자갈 같은 낙하 블록 제외 |
| 원목 계열 | 16 | 한 원목 종류를 우선하고, 보유량이 부족해도 reserve를 채우기 위해 새로 채집하지 않음 |
| 판자 | 원목이 없을 때 32 | 원목 reserve와 합산하지 않는 fallback. 원목 16과 판자 32를 등가량이라고 해석하지 않음 |
| 막대기 | 0 | 현재 제작 working-set이 요구할 때만 보호 |
| 석탄과 목탄 | 합계 16 | 종류별 16이 아니라 category 합계 |
| 횃불 | 32 | 탐사와 안전용 한 stack 우선 |
| 안전한 조리 음식 | 합계 16 | 여러 종류를 조금씩 남기지 않고 가장 적합한 종류 우선 |
| 물 양동이 | 1 | 이미 보유 중일 때만 보호하고 새로 만들지 않음 |
| 빈 상자 또는 통 | 1 | 자동 저장이 새 destination을 필요로 할 때만 선택적으로 보호 |
| 화살 | 32 | 보호된 bow 또는 crossbow가 있을 때만 |
| 폭죽 | 32 | elytra를 사용하거나 보호하고 있을 때만 |
| 엔더 진주 | 4 | 탐사 profile에서만 선택적으로 보호 |
| 철, 금, 레드스톤, 청금석 | 0 | working-set 필요량 외에는 저장 가능 |
| diamond, emerald, netherite 계열 | 0 | reserve 대신 26.10.6의 trusted destination 적용 |

일반 reserve는 대략 5~7 slots 안에 모이는 것을 목표로 한다. Reserve 적용 뒤 수량만
줄고 occupied slot이 그대로이면 자동 저장의 목적을 달성하지 못한 것이다. Category의
구체적인 대표 item 선택과 수량은 설정 가능하게 만들 수 있지만, 설정이 없다는 이유로
수동 full-deposit 정책으로 fallback하지 않는다.

#### 26.10.6 fungible valuables와 trusted destination

`diamond`, `diamond_block`, `diamond_ore`, `deepslate_diamond_ore`, `emerald`,
`emerald_block`, `emerald_ore`, `deepslate_emerald_ore`, `ancient_debris`, `netherite_scrap`,
`netherite_ingot`, `netherite_block` 같은 교환 가능한 귀중품 형태와 평범한 high-tier 중복
장비는 hard-protected도 아니고 일반 destination으로 갈 수 있는 평범한 surplus도 아니다.
이 목록은 command alias catalog의 포함 여부가 아니라 별도 automatic policy의 canonical ID,
tag 또는 명시 predicate를 기준으로 완전하게 관리한다.

기본 정책은 다음과 같다.

```text
working-set이 요구하는 수량
    -> inventory에 보호

working-set 초과 수량 + trusted destination 존재
    -> trusted destination에만 자동 저장 가능

trusted destination 없음
    -> 현재 inventory에 유지
    -> NO_TRUSTED_DESTINATION 또는 다른 surplus도 없으면 NO_SAFE_SURPLUS
```

Trusted destination은 최소한 다음 조건을 만족해야 한다.

```text
사용자가 지정한 기지 또는 창고이거나 명시적으로 trusted로 등록됨
world, dimension과 BlockPos가 지속적으로 기록됨
나중에 다시 찾을 수 있음
던전 loot container나 우연히 발견한 임의 container가 아님
현재 접근 가능성과 충분한 용량이 GUI 기준으로 확인됨
임시 배치 container라면 session 뒤에도 위치 기록이 보존됨
```

26.9의 `AutoDepositDestinationManifest`는 operation-local recovery 증거이며 trusted storage
registry가 아니다. 가장 가까운 상자, 현재 cache에 있는 상자 또는 자동 operation이 방금
배치한 상자를 자동으로 trusted로 승격하지 않는다.

이름 또는 enchantment가 있는 diamond 장비는 귀중품 목적지 정책보다 hard protection이
우선한다. 평범한 중복 diamond 장비만 trusted destination 후보가 될 수 있다.

Trusted destination 기능이 구현되기 전의 안전한 기본값은 귀중품을 자동 저장에서
제외하는 것이다. 임의의 주변 상자에 넣은 뒤 위치를 잃는 동작으로 fallback하지 않는다.

#### 26.10.7 idle automatic entry 계약

활성 UserTask가 없다는 이유로 automatic deposit이 manual `@deposit_all` 정책으로
fallback하지 않는다.

Idle 자동 진입은 다음처럼 계산한다.

```text
working-set:       empty
hard protection:  enabled
category reserve: enabled
valuable policy:  enabled
```

따라서 `DepositAllInventoryPressureChain.startFullDeposit()`이 현재 manual selector를
사용하는 경로는 목표 정책과 일치하지 않는다. 변경 시 manual selector 자체를 수정하지
않고 idle을 포함한 모든 automatic entry가 같은 immutable auto policy plan을 사용하게
한다.

#### 26.10.8 NO_SAFE_SURPLUS latch와 threshold hysteresis

안전하게 저장할 surplus가 없으면 hard protection, working-set 또는 reserve를 자동으로
깨지 않는다.

```text
safe plan 없음
    -> NO_SAFE_SURPLUS
    -> 기존 UserTask 즉시 재개
    -> current decision fingerprint 저장
    -> 의미 있는 상태 변화 전까지 같은 fingerprint 재평가 금지
```

현재 `deferChanged("no_safe_surplus", ...)`는 중복 로그만 억제하며 매 tick resolver와
selector 계산을 막는 상태 latch가 아니다. 구현 시 명시적인 waiting state 또는 동등한
state-machine 소유 latch가 필요하다. 단순 cooldown만으로 같은 상태의 느린 반복을 만들지
않는다.

Decision fingerprint는 최소한 다음 의미 상태를 포함한다.

```text
world와 dimension identity
UserTask root identity와 working-set epoch
policy-relevant inventory fingerprint: stack identity/metadata, disposition,
reserve/working-set boundary와 새로 비울 수 있는 source slot
equipped loadout와 BotBehaviour protection revision
reserve policy revision
trusted destination revision와 capacity state
```

재평가는 다음 사건 중 하나가 발생했을 때만 허용한다.

```text
occupied slots가 low-water mark 아래로 내려감
UserTask root 또는 working-set 변경
새 stack 생성/소멸, disposition 변경, reserve/working-set 경계 통과 또는
새로 비울 수 있는 surplus slot 발생
장착품 또는 주력 loadout 변경
BotBehaviour protection 또는 reserve 설정 변경
trusted destination의 지정, 발견 또는 capacity 변경
world 또는 dimension 변경
사용자의 명시적 retry 요청
```

보호된 stack의 단순 count 증가처럼 disposition과 예상 free-slot 결과를 바꾸지 않는 변화는
fingerprint를 바꾸지 않는다. 매번의 pickup을 의미 있는 rearm 사건으로 취급해 느린
`NO_SAFE_SURPLUS` 반복을 만들지 않는다.

현재 의도한 `9 / 10` high-water trigger는 36-slot main inventory에서 `33 / 36`이다.
Rearm은 같은 threshold 바로 아래가 아니라 별도 low-water mark를 사용한다.

```text
trigger:                 occupied >= 33 / 36
normal rearm target:     occupied <= 28 / 36
operation success goal:  가능하면 28 이하 또는 최소 5~8 free slots 확보
```

`33 -> 32 -> item 1개 획득 -> 33` 진동을 허용하지 않는다. Time passage만으로 latch를
해제하지 않으며 cooldown은 의미 있는 상태 변화 뒤의 burst 억제에만 사용할 수 있다.

보호 정책 때문에 UserTask도 inventory space 부족으로 진행할 수 없으면 자동으로 hard
protection을 완화하지 않고 typed terminal `INVENTORY_POLICY_BLOCKED`를 사용한다. 사용자는
수동 `@deposit_all`, reserve 축소, 보호 해제 또는 trusted destination 지정 중 하나를
선택할 수 있다. 향후 opt-in `EMERGENCY_TRIM`을 도입하더라도 category reserve만 단계적으로
낮출 수 있고 hard protection과 current working-set은 침범하지 않는다.

#### 26.10.9 성공 판정은 저장 수량이 아니라 확보 slots

자동 저장의 1차 성공 기준은 몇 개의 item을 이동했는지가 아니라 실제로 몇 개의 inventory
slot을 비웠는지다.

Plan은 다음 순서를 우선한다.

```text
전체 stack을 옮겨 즉시 비울 수 있는 slot
동일 destination stack에 merge돼 source slot을 비울 수 있는 품목
reserve category를 최소 종류와 stack 수로 통합한 뒤의 surplus
부분 수량 이동만 일어나 occupied slot이 그대로인 후보는 후순위
```

완료 뒤 occupied slot delta를 검증하고 high-water에서 충분히 멀어졌는지 확인한다.
Item count 감소만 확인하고 성공으로 분류하지 않는다. 목표 free-slot 수를 만들 수 없지만
일부 안전한 surplus는 저장한 경우 partial relief와 full success를 구분한다.

#### 26.10.10 책임 분리와 구현 경계

기존 auto package의 working-set, maintenance와 recovery 책임은 유지한다. 새 정책은 다음
LAVI-owned 경계로 분리하는 방향을 사용한다.

```text
lavi/minecraft/task/container/deposit/auto/policy/
    AutoDepositStackSnapshot
        -> slot, stack metadata와 immutable content fingerprint

    AutoDepositHardProtectionPolicy
        -> equipped, primary loadout, BotBehaviour, pin과 unique metadata 판정

    AutoDepositCategoryReservePolicy
        -> category별 lower bound와 최소-stack allocation

    AutoDepositValuableDestinationPolicy
        -> fungible valuable 분류와 trusted destination eligibility

    AutoDepositPlan
        -> exact dispositions, depositable quantities와 free-slot goal

    AutoDepositDecisionFingerprint
        -> NO_SAFE_SURPLUS latch 재평가 경계
```

기존 책임과의 연결은 다음과 같다.

```text
ActiveTaskWorkingSetResolver
    -> current UserTask requirement만 제공

automatic policy layer
    -> stack hard protection + working-set + reserve + destination 합성

DepositAllInventoryPressureChain
    -> active와 idle 모두 같은 automatic plan 사용
    -> threshold와 latch state 소유

AutoDepositMaintenanceTask
    -> 승인된 plan의 단방향 실행과 postcondition 검증
```

현재 aggregate `ItemTarget` 경로가 stack disposition을 보존하지 못하면 다음 순서를
사용한다.

```text
first safe slice:
    mixed protected/unprotected Item 종류 전체를 자동 저장에서 제외

later exact slice, only if required:
    auto-only slot-aware transfer owner 추가
    click 전 stack fingerprint 재검증
    shared StoreInContainerTask 의미는 변경하지 않음
```

다음 경계는 계속 변경하지 않는다.

```text
manual @deposit_all selector와 명령 의미
DepositCommand.java
StoreInAnyContainerTask.java
StoreInContainerTask.java and StoreInContainerTask.isEqual()
ResourceTask.allowContainers == false
TaskCatalogue and ordinary @get behavior
Task, TaskRunner, UserTaskChain and Baritone
```

Trusted storage registry는 operation-local destination manifest와 별도 책임이다. 좌표와
차원 persistence, user designation과 capacity revalidation을 한 클래스에 섞지 않는다.

#### 26.10.11 주요 되먹임과 실패 위험

| 위험 | 차단 계약 |
| --- | --- |
| 저장 직후 다음 recipe 단계가 같은 재료를 다시 채집 | 지원되지 않는 working-set은 fail-closed하고 중간재와 연료 포함. 같은 UserTask epoch 재획득품 재저장 금지 |
| 이름 또는 enchantment stack 손실 | Item 합산 전 stack-level 판정. Mixed Item은 exact transfer 전까지 전체 제외 |
| reserve가 여러 종류에 흩어져 slot을 비우지 못함 | category 합계와 대표 stack 우선. 성공을 free-slot delta로 판정 |
| 자동 deposit이 상자 제작용 목재를 스스로 저장 | automatic operation working-set에 최소 container resource 포함 |
| 33과 32 사이 threshold 진동 | 33 high-water와 28 low-water 분리 |
| 귀중품이 여러 임시 상자로 분산 | trusted destination 전용. 미등록 시 inventory 유지 |
| 모든 도구와 모든 NBT stack 과보호 | 현재 장착품, 기능별 주력 1개와 실제 고유 metadata를 구분 |
| recovery가 매번 발생해 정상 경로가 됨 | 반복 recovery를 surplus policy defect로 terminal 분류 |

#### 26.10.12 필수 검증 계약

정책 구현 뒤에는 최소한 다음 focused 검증을 통과해야 한다.

1. 같은 Item인 평범한 흉갑과 이름 또는 enchantment 흉갑이 함께 있을 때 특별 stack은
   이동하지 않는다.
2. 장착품, offhand, selected hotbar와 기능별 주력 도구는 유지되고 평범한 중복품만
   destination policy에 따라 저장된다.
3. BotBehaviour-protected Item은 보유량 전체가 자동 저장에서 제외된다.
4. `working-set=16`, `reserve=16`이면 보호량은 32가 아니라 16이다.
5. 여러 원목, 음식, 석탄과 목탄 reserve가 item별이 아니라 category 합계로 계산된다.
6. reserve 미달 상태가 새로운 crafting, mining 또는 collection Task를 시작하지 않는다.
7. active UserTask와 idle 상태 모두 같은 automatic policy를 사용하고 수동 selector로
   fallback하지 않는다.
8. trusted destination이 없으면 diamond를 임의의 nearby container에 저장하지 않는다.
9. trusted destination은 world, dimension, position과 capacity가 모두 일치할 때만 사용된다.
10. `NO_SAFE_SURPLUS` 뒤 동일 fingerprint에서는 resolver와 selector를 매 tick 다시
    실행하지 않는다.
11. `33 / 36`에서 한 번 실행하고 `32 / 36`만으로 rearm하지 않으며 `28 / 36` 이하 또는
    명시된 의미 상태 변화에서만 재평가한다.
12. 완료 판정은 실제 free-slot delta를 포함하고 item count만 감소한 결과와 구분한다.
13. 자동 operation이 새 상자를 필요로 할 때 최소 상자 또는 목재 working-set을 먼저
    저장하지 않는다.
14. recovery가 같은 UserTask epoch에서 반복되면 정상 성공이 아니라 policy defect로
    분류한다.
15. 수동 `@deposit_all`, 기존 `@deposit`, 일반 `@get`, shared Task와 Baritone 동작은
    기존 상태를 유지한다.

Runtime 관측은 한 automatic operation ID로 다음 경계를 연결하되 bounded logging 정책을
유지한다.

```text
pressure snapshot and high-water decision
-> stack classification summary
-> working-set and category reserve allocation
-> trusted destination decision
-> planned free slots
-> exact transfer or conservative refusal
-> actual free-slot delta
-> NO_SAFE_SURPLUS latch or terminal result
-> original UserTask resume
```

#### 26.10.13 현재 상태와 다음 gate

```text
manual/automatic semantic split:                 ACCEPTED
stack hard protection direction:                 IMPLEMENTED IN DIRTY WORKTREE
working-set plus category reserve max rule:       IMPLEMENTED IN DIRTY WORKTREE
fungible valuables trusted-only policy:           IMPLEMENTED IN DIRTY WORKTREE
idle automatic policy unification:                IMPLEMENTED IN DIRTY WORKTREE
NO_SAFE_SURPLUS state latch:                      IMPLEMENTED IN DIRTY WORKTREE
9/10 high-water source constants:                 IMPLEMENTED, 33 / 36
separate low-water rearm:                         IMPLEMENTED, 28 / 36
free-slot success metric:                         IMPLEMENTED IN DIRTY WORKTREE
trusted destination registry:                    IMPLEMENTED AS INSTANCE-OWNED JSON STORE
trusted destination in-game registration command: IMPLEMENTED IN DIRTY WORKTREE, NOT BUILT
trusted sequential live-container execution:      IMPLEMENTED IN DIRTY WORKTREE, NOT BUILT
resource JSON reuse boundary:                     ACCEPTED, DOCUMENTED
unclassified vanilla/modded item default:          FAIL_CLOSED, DOCUMENTED
rare functional item destination policy:           DOCUMENTED
source or test changes for section 26.10:         PRESENT, STATIC REVIEW ONLY
build, deployment and Minecraft reproduction:     NOT RUN FOR THIS SECTION
```

구현된 자동 경로는 `lavi/automatic-deposit-policy.json`을 전용 정책 자료로 사용하고,
trusted destination은 Fabric config 아래
`lavi/automatic-deposit-trusted-destinations.json`에 world, dimension, position과 enabled 상태를
명시적으로 저장한다. 발견한 상자나 operation-local manifest를 자동으로 trusted로 승격하지 않는다.
현재 dirty worktree에는 이 JSON store를 게임 안에서 갱신하는 `@auto_deposit_trust`,
`@auto_deposit_untrust [destinationId]`, `@auto_deposit_trusted_list` command가 구현되어 있다.
동일한 composition root가 만든 repository 인스턴스를 automatic policy, command registrar와 trusted
저장 실행 Task에 주입한다. 이 상태는 아직 Gradle build와 Minecraft runtime 검증을 거치지 않았으므로
배포 완료 상태로 판정하지 않는다.

```json
{
  "schemaVersion": 1,
  "destinations": [
    {
      "worldKey": "singleplayer:World Name",
      "dimension": "OVERWORLD",
      "x": 0,
      "y": 64,
      "z": 0,
      "enabled": true
    }
  ]
}
```

멀티플레이 world key는 `multiplayer:<lowercase server address>` 형식이며, 실제 값은 automatic policy
plan 진단의 `persistentWorldKey` 필드로 확인한다. JSON 변경은 repository revision으로 감지하지만,
등록되지 않은 발견 상자는 귀중품 목적지로 사용하지 않는다.

공용 `StoreInContainerTask`가 aggregate ItemTarget만 받는 첫 safe slice에서는 mixed protected/plain
Item 종류 전체를 제외한다. 같은 일반 Item의 여러 stack은 64를 넘는 하나의 목표로 합치지 않고,
보호 수량을 침범하지 않으면서 통째로 비울 수 있는 stack별 step으로 나눈다. 계획은 일반 surplus를
먼저 배정하고 목표 relief가 부족할 때만 trusted-only step을 추가한다. Trusted step이 실제로 필요하면
예약한 trusted 용량을 일반 저장이 먼저 점유하지 않도록 trusted step을 실행한 뒤 일반 step을 실행한다. 이 source 상태는 focused
테스트가 추가된 상태지만 Gradle 실행, 다중 버전 preprocess/compile, JAR 배포와 Minecraft runtime
검증 전에는 완료 판정이나 커밋 판정을 내리지 않는다.

다음 구현 요청이 있을 때는 새 정책 검수 문서를 다시 만들지 않고 이 절의 계약을 기준으로
focused source 범위를 먼저 확정한다. Stack-level transfer enforcement, trusted storage와
state-machine hysteresis는 서로 다른 lifecycle 책임이므로 한 번에 shared engine 경계를
넓히지 않는다. 각 slice는 기존 automatic package 안의 LAVI-owned composition으로 구현하고
수동 명령과 upstream-derived shared Task는 보존한다.

##### 26.10.13.1 trusted destination 등록 명령 방향

<!-- 20260827_openai: Recorded the reviewed exact-container registration direction for trusted automatic-deposit destinations. -->

방향성 검수 결과, 첫 구현은 집이나 기지의 영역 전체가 아니라 사용자가 명시적으로 지정한
개별 container 좌표를 trusted destination으로 등록한다. Trusted destination은 편의상 발견된
상자 목록이 아니라 귀중품의 저장 허용 경계다. 따라서 영역 내부의 새 container, scanner가
발견한 container, cache에 남은 container 또는 operation-local destination manifest를 자동으로
trusted로 승격하지 않는다.

게임 내 첫 명령 surface는 다음 세 가지로 제한한다.

```text
@auto_deposit_trust
    -> 현재 열린 screen이 지원되는 block container이고 현재 world/dimension의 exact BlockPos와
       결합할 수 있으면 그 container를 우선 등록
    -> 위 조건을 만족하는 열린 container가 없으면 사용자가 바라보는 지원 block container를 등록

@auto_deposit_untrust [destinationId]
    -> 인자가 없으면 trust와 같은 exact-target 규칙으로 열린 container를 우선하고,
       없으면 바라보는 등록 container를 해제
    -> destinationId가 있으면 파괴되었거나 접근할 수 없는 등록도 직접 해제

@auto_deposit_trusted_list
    -> stable destinationId, worldKey, dimension, BlockPos, enabled와 관측 상태 출력
```

등록과 해제 결과에는 최소한 `destinationId`, `worldKey`, `dimension`, `BlockPos`를 출력한다.
목록 순번은 정렬에 따라 달라질 수 있으므로 영속적인 해제 식별자로 사용하지 않는다.
`destinationId`는 world identity, dimension과 좌표에서 결정적으로 만들고 같은 container를
반복 등록해도 중복 entry를 만들지 않는다.

등록 시점에는 현재 대상이 지원되는 실제 block container인지 확인하지만, 등록 시점의 남은 용량을
영구적인 사실로 저장하지 않는다. 열린 screen을 등록 대상으로 사용할 때는 현재 interaction과
exact BlockPos의 결합이 증명된 경우만 허용하며, last-opened 위치, cache 또는 nearest-container
추정으로 trusted 좌표를 만들지 않는다. Minecraft client의 cache는 열어 본 시점의 관측일 뿐이며,
열지 않았거나 이후 내용이 바뀐 container의 현재 용량을 증명하지 못한다. 목록 상태도 이 한계를
숨기지 않고 다음처럼 구분한다.

```text
KNOWN_AVAILABLE
KNOWN_FULL
MISSING
KNOWN_UNREACHABLE
UNKNOWN_OR_STALE
```

`KNOWN_AVAILABLE`과 `KNOWN_FULL`도 관측 시점과 함께 표시할 수 있는 cache 상태이며, 실제 저장
직전에는 서버가 제공한 열린 container GUI slot을 기준으로 용량을 다시 검증한다. 접근 가능성도
사전 추정만으로 성공 처리하지 않는다.

여러 trusted destination은 사전 후보 ordering과 실제 저장 acceptance를 분리한다.

```text
사전 후보 gate:
    same worldKey
    -> same dimension
    -> enabled registration
    -> supported container가 관측됐거나 아직 실제 검증 가능한 상태
    -> operation-local failure blacklist에 없음

사전 시도 순서:
    가까운 거리 우선
    -> 거리가 같은 경우 최근에 확인된 남은 용량을 hint로 사용

선택한 후보의 실제 acceptance:
    등록된 exact BlockPos의 지원 container인지 재검증
    -> 실제로 열기
    -> 서버가 제공한 container GUI slot으로 필요한 수용 가능량 재검증
    -> 실패하면 operation-local blacklist에 넣고 다음 후보 시도
```

Cache상 capacity와 accessibility는 사전 ordering의 hint일 뿐 최종 성공 증거가 아니다. 모든 trusted
container를 먼저 열어 전역 용량 순위를 만드는 동작도 요구하지 않는다. 선택한 상자를 열었을 때
가득 찼거나 사라졌거나 접근할 수 없으면 해당 automatic operation에서만 제외하고 다음 trusted
destination을 시도한다. 같은 실패 후보를 무제한 재선택하지 않는다.

사용 가능한 trusted destination이 없으면 trusted-only 귀중품을 일반 container로 fallback하지
않는다. 일반 surplus만 처리하고 귀중품은 inventory에 유지한 채 보호 상태로 정상 대기한다.
재시도는 trusted registry revision, 유효한 capacity 관측 또는 다른 기존 decision fingerprint
변화가 있을 때만 허용한다.

집 또는 기지 영역 등록은 첫 구현 범위에서 제외한다. 이후 필요하면 기존 exact-container
provider를 대체하지 않고 별도의 `trusted area` destination provider로 추가한다. 영역 내부에
있다는 사실만으로 새 container를 자동 신뢰할지 여부도 그 후속 기능에서 별도로 승인해야 한다.

현재 dirty worktree 구현은 기존 instance-owned repository와 selector를 재사용하며 다음 경계로
책임을 분리한다.

```text
AutoDepositRuntime
    -> policy, command와 execution에 동일 repository instance 주입

trusted/command/
    -> trust / untrust / trusted_list
    -> exact binding이 없고 ChatScreen 외 화면이 열려 있으면 stale crosshair fallback 거부

trusted/interaction/
    -> block interaction과 열린 screen의 exact position binding 계약
    -> command target과 trusted execution의 live acceptance에 동일 증거 제공

trusted/execution/
    -> 거리순 immutable candidate queue
    -> operation-local rejection map
    -> exact-open 전에는 후보별 고정 InteractWithBlockTask만 실행
    -> exact-open 이후에만 후보별 고정 StoreInContainerTask 실행
    -> exact-open 좌표에서만 player inventory net loss와 container net gain의 교집합으로 transfer 확인
    -> 실제 GUI에 whole-stack 수용 공간이 없거나 진행이 없으면 다음 후보로 단방향 이동
```

열린 screen 등록은 같은 world/dimension의 지원 container block interaction이 20 client tick 안에
해당 screen handler와 결합된 경우만 허용한다. 결합을 증명하지 못하면 열린 container GUI를
last-opened, cache 또는 nearest-container로 추정하지 않는다. 화면이 닫혀 있거나 명령 입력용
`ChatScreen`인 상태에서만 현재 crosshair의 지원 container를 등록 대상으로 사용한다. 다른 화면은
crosshair가 stale일 수 있으므로 fallback을 거부한다.

Trusted 저장 실행도 같은 exact-open binding을 주입받는다. 실제 GUI 수용량 확인과 transfer delta는
활성 후보 좌표가 이 binding과 일치할 때만 인정하며, upstream `ContainerSubTracker`의
`getLastBlockPosInteraction()` fallback은 trusted 성공 증거로 사용하지 않는다.

후보 snapshot에는 uncached 또는 cached-full 상태도 hint와 함께 남겨 실제 GUI 검증 기회를 준다.
로드된 좌표에서 container가 사라졌거나 기존 안전 eligibility를 통과하지 못한 경우만 사전 제외한다.
한 operation의 immutable 후보 snapshot은 거리순 최대 64개로 제한한다.
실행 중 등록이 해제되거나 container가 사라지거나 unreachable로 판정되면 해당 operation에서만
제외하고 다음 후보로 이동한다. 모든 후보가 실패하면 trusted-only 귀중품은 inventory에 남고,
일반 surplus 단계만 계속한다. 후보별 2400 tick, 열린 GUI 무진행 200 tick과 trusted operation 전체
6000 tick 상한을 적용하며, 상한 종료도 일반 container fallback 성공으로 바꾸지 않는다.
완료 후 `WAIT_FOR_REARM` 상태에서는 같은 등록과 시간 경과로 재시도하지 않는다. 실제 trusted
repository revision이 바뀐 경우에만 명시적 policy-change 신호로 한 번 재무장하여, 인벤토리가
계속 high-water 이상이어도 새 등록 또는 해제를 반영한 plan을 다시 계산한다.

전역 `ResourceTask.allowContainers=true`, 수동 `@deposit_all`, 기존 `@deposit`, 일반 `@get`,
shared `StoreInContainerTask`, `Task`, `TaskRunner` 또는 Baritone 동작은 변경하지 않는다. 현재 상태는
focused source와 test가 존재하는 static-review 단계이며, clean forced build, JAR 배포와 Minecraft
runtime 재현 전에는 완료 또는 커밋 가능 판정을 내리지 않는다.

#### 26.10.14 resource JSON과 automatic deposit policy data 경계

현재 첨부 resource JSON은 한국어 명령 해석과 canonical target 연결에는 재사용할 수 있지만,
automatic deposit의 보호, reserve 또는 trusted-destination 의미를 소유하지 않는다.

```text
korean_item_aliases.json
    -> 한국어 item 이름을 알려진 canonical target에 연결

korean_item_display_names.json
    -> canonical target의 한국어 표시 이름 제공

chatclef_target_canonicalization.json
    -> legacy target 이름을 현재 canonical target으로 정규화

korean_equipment_aliases.json / korean_material_aliases.json
    -> "다이아 곡괭이" 같은 언어 조합 해석 보조

chatclef_item_command_target_policy.json
    -> DIRECT_ITEM, DIRECT_BLOCK, GENERIC_GROUP, UNSUPPORTED 같은 command-target 지원 상태
```

위 자료에서 재사용할 수 있는 것은 canonical ID 정규화, 표시 이름과 명령 alias다. Alias가
존재하거나 `DIRECT_ITEM` 또는 `DIRECT_BLOCK`으로 분류됐다는 사실은 gameplay 안전성,
희소성, reserve 수량 또는 저장 목적지 신뢰도를 뜻하지 않는다.

현재 catalog-driven resource set은 591 target 중심이며 유효한 Minecraft 1.20.1 품목 중
`firework_rocket`, `trident`, `elytra`, `totem_of_undying`, `netherite_scrap`, `written_book`,
`filled_map` 등 일부를 automatic policy의 완전한 근거로 제공하지 않는다. 따라서 catalog에
없는 품목을 일반 surplus로 추정하지 않는다.

Automatic policy의 판정 자료는 다음처럼 분리한다.

```text
canonical exact ID 또는 별도 policy list
    -> valuables, rare functional items, exact water bucket/torch/firework rules

Minecraft tag 또는 runtime item/block category
    -> logs, planks, coals, arrows, armor/tool/weapon role,
       FoodComponent, BlockItem과 safe-building property

runtime ItemStack state
    -> equipped slot, selected/offhand, custom name, enchantment, lore,
       armor trim, custom attributes/model data, map identity,
       portable-container contents와 stack fingerprint

runtime task/behaviour state
    -> BotBehaviour protection, current working-set, crafting intermediate,
       smelting input/fuel과 automatic operation working-set
```

별도 automatic-deposit policy data는 canonical ID, Minecraft tag와 제한된 runtime predicate를
키로 사용해 최소한 다음 의미를 소유한다.

```text
hard-protected rare/common exception
trusted-destination-only valuable and rare item
category reserve membership and default quantity
safe-building allow/deny policy
food suitability and harmful/special-food exclusion
known general-surplus allow policy
unknown/modded fail-closed default
```

한국어 alias와 내부 정책은 분리한다. 번역 또는 별칭 추가, 동의어 충돌, command catalog
coverage 변화가 inventory 안전 정책을 바꾸면 안 된다. 내부 policy는 canonical ID와 runtime
상태를 기준으로 결정하고, 한국어 resource는 사용자 입력과 표시를 그 canonical ID에 연결하는
경계로만 유지한다.

## 27. 2026-08-27 manual trusted home storage 방향 분리

<!-- 20260827_openai: Linked the explicit-request-only manual home storage direction without rewriting the automatic-deposit evidence history. -->

사용자가 명시적으로 요청했을 때 집의 trusted destination에 inventory를 정리하는 새 기능은
이 문서의 inventory-pressure automatic operation과 분리한다.

새 기능의 canonical 설계 문서는 다음이다.

- [Manual Trusted Home Storage Direction](chatclef-manual-trusted-home-storage-direction-2026-08-27.md)

`STORE_HOME`에 대해서는 다음 불변조건이 이 문서의 automatic policy보다 우선한다.

```text
사용자 요청 없음
    -> inventory가 가득 차도 STORE_HOME Task를 시작하지 않음
    -> 별도 automatic pressure Task의 현재 상태와 복구 계약은 §28에서만 판정

사용자 명시 요청
    -> manual StoreHome UserTask만 시작
    -> exact trusted destination만 사용
    -> 일반 container fallback 없음
    -> loadout/reserve 외 모든 안전하게 식별 가능한 exact stack 저장
```

기존 automatic source와 diagnostics 기록은 조사와 rollback 근거로 보존할 수 있지만,
manual V1의 activation path가 아니다. 이 방향 변경은 현재 Java behavior가 이미 변경됐다는
뜻이 아니며, automatic entrypoint 비활성화, `@store_home`, exact-slot executor, build와
runtime 검증은 모두 별도 구현 및 승인 대상이다.

## 28. 2026-08-29 automatic inventory-pressure production composition 회귀 판정

### 28.1 범위와 승인 상태

이 절은 현재 source, `740aa616` 전후 Git 이력, 기존 테스트 source와 2026-08-29
Minecraft 로그 점검을 대조해 automatic inventory-pressure deposit이 시작되지 않는
이유를 고정한다. 다음 작업을 수행하거나 승인하지 않는다.

```text
Java 또는 JSON source 수정
test source 수정 또는 실행
Gradle build
JAR 복사 또는 배포
Minecraft 실행 또는 재현
commit 또는 push
```

현재 요구사항에서 복구 대상으로 보는 것은 사용자가 직접 실행하는 `@deposit_all`이나
`@store_home`이 아니라 다음 독립 subsystem이다.

```text
inventory occupied-slot pressure
    -> automatic-only policy plan
    -> AutoDepositMaintenanceTask
    -> safe surplus / trusted-only storage
    -> actual free-slot postcondition
```

`STORE_HOME`은 계속 explicit-request-only다. Automatic pressure subsystem을 이후 별도
승인으로 복구하더라도 inventory pressure가 `StoreHomeTask`를 생성하거나 `STORE_HOME`
planner, timeout, manifest 또는 terminal contract를 재사용한다는 뜻이 아니다.

### 28.2 증거 기준선

이번 문서 갱신의 read-only 기준선은 다음과 같다.

```text
repository root: C:\Vtuber_Souorce_Code\LAVI
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: 14ba9b443f0bc11d6860a25d7fd3b8b916d95a04
causal integration commit: 740aa61683ad4cab1f7b3d4dff3e2c86befec983
commit subject: feat(chatclef): add manual trusted home storage
commit size: 82 files changed, 5,749 insertions, 158 deletions
```

현재 working tree에는 별도의 `StoreDepositDiagnostics` 리팩토링 source와 test 변경이
존재한다. 이 절은 그 변경을 automatic pressure 원인의 일부로 취급하지 않으며 수정,
흡수, 되돌리기 또는 정리하지 않는다.

증거 수준은 다음처럼 분리한다.

| 증거 | 관찰 | 판정 |
| --- | --- | --- |
| 현재 `DepositAllAutoEntrypoint` | Fabric `END_CLIENT_TICK` callback 하나가 `AutoDepositRuntime`만 lazy-create하고 `runtime.onEndClientTick()`을 호출한다. | `SOURCE_CONFIRMED_DORMANT` |
| 현재 `AutoDepositRuntime` | trusted command, `StoreHomeCommandRegistrar`, `AutoDepositOpenContainerBindingTracker`만 소유·tick한다. Pressure chain과 policy engine 필드가 없다. | `SOURCE_CONFIRMED_DORMANT` |
| 현재 `AutoDepositPolicyCompositionRoot.createRuntime()` | repository 하나만 생성해 request-only runtime에 전달하며 automatic policy engine을 만들지 않는다. | `SOURCE_CONFIRMED_DORMANT` |
| 현재 registration test | `lazyRegistrationCreatesOneRuntimeAndNoAutomaticPressureChain`이 runner의 pressure chain 수 `0`을 요구한다. | `TEST_SOURCE_PRESENT`; 실행 결과는 이 절에서 주장하지 않음 |
| `740aa616` 이전 entrypoint | policy engine과 `DepositAllInventoryPressureChain`을 생성하고 매 tick `chain.onEndClientTick()`을 호출했다. | `GIT_HISTORY_CONFIRMED` |
| `740aa616` 이후 source와 주석 | pressure chain 비활성화와 request-only runtime 분리가 명시됐다. | `GIT_HISTORY_CONFIRMED` |

이 절에서 `SOURCE_CONFIRMED_DORMANT`는 automatic 관련 class는 존재하지만 production
construction/tick이 없음을 확인했다는 뜻이다. 이후 복구 source에 production composition과
tick이 실제 연결된 경우에만 `SOURCE_CONFIRMED_ACTIVE`를 사용한다. 두 label 모두 test,
build, deployed artifact 또는 runtime 성공을 함의하지 않는다.

2026-08-29에 읽기 전용으로 확인한 build output JAR과 active instance JAR은 다음
file-level identity가 같았다.

```text
file: chatclef-1.20.1-0.18.23.jar
size: 7,136,832 bytes
sha256: E24D73C60370C265AC30A6DDDE78D22560A779B7B78883DBF33DB62CBC0D780F
active instance log: C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
latest.log last observed write: 2026-08-29 20:05:55 KST
```

같은 로그에서 다음 문자열의 관찰 횟수는 모두 `0`이었다.

```text
AUTO_DEPOSIT_ALL_CHAIN=0
automatic_chain_registered=0
automatic_task_started=0
automatic_surplus_plan_created=0
immutable_automatic_plan_created=0
high_water_threshold_crossed=0
FREE_SLOT_POSTCONDITION=0
Automatic Deposit All=0
```

이 결과는 현재 실행에서 automatic path가 관찰되지 않았다는 보강 증거다. 로그 부재만으로
원인을 추정하지 않는다. 직접 원인은 현재 source graph에 chain construction과 tick call이
없다는 것이다. Build output과 deployed JAR의 file identity는 확인됐지만 complete
repository/source/build-input manifest provenance는 없으므로 다음 판정을 유지한다.

```text
FILE_ARTIFACT_MATCH_CONFIRMED: YES
SOURCE_TO_ARTIFACT_COMPLETE_PROVENANCE: UNVERIFIED
AUTOMATIC_RUNTIME_SUCCESS_PATH: NOT PROVEN
```

### 28.3 확정 원인 문장

현재 source-level root cause는 다음으로 고정한다.

> `740aa616`의 manual `STORE_HOME`/trusted-storage 통합 과정에서 automatic
> inventory-pressure chain의 production composition이 의도적으로 제거됐고, 현재
> 요구사항을 기준으로 그 설계 결정이 automatic storage 기능 회귀가 됐다.

현재 production 경로는 다음 단계에 도달할 수 없다.

```text
pressure snapshot
-> 33/36 high-water 판정
-> working-set resolution
-> automatic policy engine
-> safe surplus plan
-> AutoDepositMaintenanceTask 제출
-> free-slot postcondition
```

따라서 정책이 cobblestone 또는 cobbled deepslate를 거절한 것이 primary cause가 아니다.
정책 판정 자체가 호출되지 않는다. Chain 내부 로그를 먼저 더 추가해도 그 class가 호출되지
않으므로 현재 미실행 원인을 더 좁히지 못한다.

### 28.4 manual `@deposit_all`과 automatic policy 분리

수동 `@deposit_all`은 automatic pressure policy가 아니다. 수동 경로의 selector와
`DepositAllTask`에는 cobblestone/deepslate를 먼저 저장하는 명시적인 priority contract가
없다. 이번 원인 판정과 이후 최소 복구에서 다음은 변경 대상이 아니다.

```text
DepositAllCommand
DepositAllInventoryTargetSelector
StorageHelper target ordering
DepositAllTask
StoreInContainerTask
```

Automatic policy JSON에는 `minecraft:cobblestone`, `minecraft:cobbled_deepslate`,
`minecraft:deepslate` 등이 `safeBuildingIds`로 존재한다. `safeBuilding=64` reserve는 각
아이템마다 64개가 아니라 safe-building category 전체에 배분되는 64개다. Allocation은
item별 총수량 내림차순, 동률이면 item ID 순서로 결정된다.

이 policy와 33/36 high-water, 28/36 low-water source가 존재한다는 사실은 production에서
chain이 활성화됐다는 증거가 아니다.

### 28.5 전체 revert와 옛 entrypoint 복사 금지

`740aa616` 전체 revert는 automatic chain뿐 아니라 manual `STORE_HOME`, trusted repository,
등록 명령, exact-container binding, planner/executor 및 관련 테스트까지 제거하므로 복구
단위로 사용하지 않는다.

커밋 이전 `DepositAllAutoEntrypoint` hunk를 현재 source에 그대로 복사하는 것도 금지한다.
옛 구조는 별도로 만든 policy engine/repository와 binding `UNAVAILABLE` 경로를 사용할 수
있다. 현재 StoreHome runtime과 다른 repository 또는 binding instance를 사용하면 동일
JSON path를 읽어도 revision, in-memory state와 exact-container evidence가 분리된다.

```text
full commit revert: REJECTED
old entrypoint wholesale copy: REJECTED
current object graph forward restoration: DOCUMENTED, NOT AUTHORIZED
```

### 28.6 forward-only composition 계약

별도 source 구현 승인이 있을 때의 최소 object graph는 다음과 같다.

```text
DepositAllAutoEntrypoint
    -> existing Fabric END_CLIENT_TICK callback exactly one
    -> AutoDepositRuntime lazy creation exactly one

AutoDepositPolicyCompositionRoot
    -> AutoDepositTrustedDestinationRepository exactly one
    -> AutoDepositPolicyEngine using that exact repository
    -> AutoDepositRuntime receiving repository + engine

AutoDepositRuntime
    -> AutoDepositOpenContainerBindingTracker exactly one
    -> trusted command registrar
    -> StoreHomeTaskFactory
    -> DepositAllInventoryPressureChain exactly one
         - mod.getTaskRunner()
         - the same policy engine
         - the same trusted repository
         - the same exact-open-container binding tracker
```

Runtime tick 순서는 기존 command registration의 idempotent contract를 보존하며 다음으로
고정하는 방향을 우선한다.

```text
trusted command registration
-> STORE_HOME command registration
-> exact-open-container tracker start
-> exact-open-container tracker tick
-> pressure chain tick
```

Tracker를 먼저 갱신해야 같은 tick의 automatic trusted execution이 가장 최근 exact binding을
읽을 수 있다. 이 순서는 구현 전에 direct test로 고정한다.

`DepositAllInventoryPressureChain`의 4-argument constructor는 policy engine의 repository와
execution repository가 같은 객체가 아니면 거부한다. 이 identity는 편의가 아니라 현재
source contract다.

`TaskChain` constructor가 `runner.addTaskChain(this)`를 이미 수행하므로 pressure chain을
생성한 뒤 `runner.addTaskChain(chain)`을 다시 호출하지 않는다. Fabric callback을 하나 더
등록하거나 TaskRunner priority를 변경하지 않는다.

### 28.7 `STORE_HOME` conflict 보완 계약

현재 `DepositAllAutoConflictGuard`가 explicit storage conflict로 인식하는 Task는 다음
세 종류다.

```text
DepositAllTask
StoreInAnyContainerTask
StoreInContainerTask
```

`StoreHomeTask`는 포함되지 않는다. 현재 decision order도 함께 고려해야 한다.

```text
active user task의 UserTaskChain selection 검사
-> existing deposit conflict 검사
-> working-set resolution
```

StoreHome이 현재 selected user root이면 conflict 인식 실패 뒤 working-set resolver가
지원하지 않는 root로 처리해 `NO_SAFE_SURPLUS_WAIT`에 잘못 latch할 수 있다. Safety chain이
StoreHome을 잠시 선점한 상태에서는 conflict guard에 도달하기 전에
`user_task_chain_not_selected`로 latch할 수 있다. 따라서 `StoreHomeTask`를 기존 class set에
추가하는 것만으로 모든 preemption 경계를 닫았다고 판정하지 않는다.

따라서 composition 복구와 같은 최소 change unit에서 automatic side가 정확한
`StoreHomeTask`를 user-owned storage route로 인식하고, 해당 판정이 두 generic
`NO_SAFE_SURPLUS` 경계보다 먼저 적용되는지를 direct test로 고정해야 한다. 기존 세 deposit
route의 decision order를 넓게 바꾸지 않는 exact StoreHome early gate 또는 동등한 좁은
분류를 우선 검토한다.

```text
StoreHome -> automatic conflict guard dependency: 없음
automatic chain -> StoreHomeTask conflict recognition: 필요
InteractWithBlockTask 전체 conflict 처리: 금지
```

두 방향의 계약은 다음과 같다.

```text
A. StoreHome root가 UserTaskChain에 먼저 assigned
   -> automatic task submission 0
   -> NO_SAFE_SURPLUS 오분류 0
   -> existing_store_home_task처럼 명확한 suppression reason
   -> 기존 storage-conflict episode와 같은 WAIT_FOR_REARM 우선

B. automatic maintenance가 먼저 active한 뒤 StoreHome 요청
   -> automatic context mismatch로 automatic-owned task만 종료
   -> 다음 runner selection에서 UserTaskChain/StoreHome 실행
   -> 양쪽 container open/click/transfer 중첩 0
```

A는 automatic-side StoreHome conflict classification 계약이다. B는 그 guard가 아니라
`AutoDepositMaintenanceTask.onTick()`의 첫 `plan.context().matches(mod)` 검사와
`automatic_context_changed/CANCELLED` 전이를 사용한다. Pressure chain은 `RUNNING`일 때
`onEndClientTick()`에서 조기 return하므로 다음 순서를 direct test로 증명해야 한다.

```text
StoreHome root submitted while automatic chain is RUNNING
-> 더 높은 safety chain이 없으면 priority 51 automatic chain이 UserTaskChain보다 먼저 tick
-> maintenance context mismatch observed before phase-specific child/container work
-> automatic task becomes CANCELLED
-> chain-owned task terminal/clear
-> UserTaskChain selects the submitted StoreHome root
```

Safety chain이 그 사이 둘을 선점하면 `DepositAllInventoryPressureChain.onInterrupt()`가
`stopOwnedRun()`으로 automatic-owned task만 중단하는 별도 기존 경로를 사용한다. 이때
automatic container click은 추가로 발생하지 않고, 아직 selected되지 않은 StoreHome의
state/timeout clock도 소비되지 않으며, safety 종료 뒤 UserTaskChain이 StoreHome을 선택하는지
별도 direct test로 고정한다. 이 기존 contract가 성립하지 않으면 pressure-chain `RUNNING`
early-return이나 TaskRunner priority를 추측으로 바꾸지 않고, 실제 마지막 성공/첫 실패
boundary를 다시 관찰한다.

StoreHome 실패 직후 low-water 없이 automatic policy를 즉시 재평가하려면 별도
`DEFERRED_BY_STORAGE_TASK`와 같은 새 상태 전이가 필요하다. 이것은 composition 복구가
아니므로 이번 최소 방향에 포함하지 않는다.

Automatic chain은 자기 `AutoDepositMaintenanceTask`만 정리한다. `TaskRunner.disable()`,
전체 Baritone cancellation 또는 다른 UserTask cleanup을 yield 수단으로 사용하지 않는다.

### 28.8 diagnostics-first 적용 판정

일반적인 unknown-cause 장애라면 bounded diagnostics가 behavior 변경보다 먼저다. 이번
primary failure의 source call graph는 다음 직접 증거로 이미 확정됐다.

```text
source-reachable boundary: Fabric callback -> AutoDepositRuntime
first missing source boundary: DepositAllInventoryPressureChain construction/registration
causal source change: request-only runtime composition introduced by 740aa616
automatic-operation runtime last-successful boundary: NOT APPLICABLE/NOT OBSERVED
```

따라서 unreachable chain 내부에 먼저 로그를 추가하는 것은 필수 선행 작업이 아니다.
Runtime 복구 증거가 필요할 때는 기존 `AUTO_DEPOSIT_ALL/REGISTERED`, state transition,
policy plan, free-slot postcondition event를 사용하고, 부족한 경우 다음 경계만 보강한다.

```text
runtime composition completed exactly once
pressure chain registered exactly once
StoreHome conflict suppression state change
automatic task submission boundary
actual free-slot terminal postcondition
```

Tick 또는 slot마다 unchanged state를 출력하지 않는다. Hot-path event는 기존 diagnostics
mode, semantic deduplication, state-change emission, per-correlation budget와 session hard cap을
그대로 사용해야 한다. Logging은 Task 선택, state transition, retry, timeout, input,
Baritone, container click 또는 cleanup을 변경하지 않는다.

### 28.9 필수 regression gate

복구 구현 전후 테스트는 다음을 분리해 고정한다.

| 영역 | 필수 검증 |
| --- | --- |
| Composition | Fabric entrypoint 1개, runtime 1개, pressure chain 1개 |
| Identity | policy engine, chain, StoreHome이 같은 trusted repository 사용 |
| Binding | trusted commands, StoreHome, automatic chain이 같은 tracker 사용 |
| Tick order | binding tracker tick 뒤 pressure chain tick |
| Threshold | 32/36 미제출, 33/36 armed episode당 정확히 한 번 제출 |
| Episode latch | high-water 반복 tick에서 중복 제출 없음 |
| Rearm | 28/36 재무장, 29/36 미재무장 |
| StoreHome first | automatic 제출 0, NO_SAFE 오분류 0, storage suppression |
| StoreHome safety preemption | StoreHome root가 UserTaskChain에 assigned돼 있지만 safety chain이 selected여도 automatic 제출과 NO_SAFE latch 모두 0 |
| Automatic first | StoreHome 요청 뒤 maintenance context 검사가 child/container work보다 먼저 CANCELLED를 만들고, chain clear 뒤 UserTaskChain으로 양보 |
| Automatic first + safety preemption | auto `onInterrupt/stopOwnedRun`이 auto-owned task만 중단하고, StoreHome clock은 소비하지 않으며, safety 종료 뒤 UserTaskChain으로 양보 |
| Existing conflicts | 기존 세 manual/container deposit route 억제 동작 불변 |
| NO_SAFE | 동일 fingerprint 반복 평가 없음, meaningful change에서만 한 번 재평가 |
| Runner ownership | 기존 active runner를 automatic chain이 disable하지 않음 |
| Preservation | manual `@deposit_all`, `@deposit`, StoreHome timeout/result/lifecycle 불변 |
| Upstream boundary | Baritone, TaskRunner, `InteractWithBlockTask`, shared transfer Task source 변경 0 |

현재 registration test는 pressure chain 수 `0`을 요구한다. 복구 시 해당 테스트를 삭제하거나
약화하지 않고 다음 exact contract로 교체한다.

```text
lazy registration을 반복해도
-> 같은 runtime instance 유지
-> DepositAllInventoryPressureChain exactly one
-> duplicate TaskChain registration 없음
```

Focused test, targeted Java test, clean forced build와 Minecraft runtime proof는 각각 실제
실행 결과가 있을 때만 다음 evidence label로 승격한다.

```text
TEST_PASSED_CURRENT
BUILD_PASSED_CURRENT
FILE_ARTIFACT_MATCH_CONFIRMED
RUNTIME_PATH_PROVEN_CURRENT
```

### 28.10 별도 plan-builder characterization

다음은 현재 chain 미등록의 원인이 아니며 composition 복구와 동시에 행동 수정하지 않는다.

1. `AutoDepositPlannedItem.expectedFreedSlots`는 현재 step마다 `1`이다. Comparator가
   free-slot 내림차순을 사용해도 대부분 동률이고 실제 우선순위는 count와 item ID로
   넘어간다.
2. `wholeStackTransferSteps()`는 stack count를 큰 순서로 검사하며 보호량을 침범하는 첫
   stack에서 `continue`가 아니라 `break`한다.
3. 예를 들어 stack `[64, 32]`, `protectedCount=64`이면 32 stack은 통째로 제거 가능하지만
   첫 64 stack에서 중단돼 plan step이 `0`이 될 수 있다.
4. 최종 target은 physical slot identity가 아니라 `ItemTarget(item, count)`다. 따라서
   `expectedFreedSlots=1`만으로 실제 한 slot이 비었다고 증명할 수 없다.

다음 characterization을 별도 change unit의 선행 gate로 둔다.

```text
safeBuilding stacks: [64, 32]
category protected count: 64
policy-intended removable whole stack: 32

runtime postcondition:
occupiedSlotsBefore
occupiedSlotsAfter
actualFreedSlots
plannedExpectedFreedSlots
```

현재 결과를 먼저 고정하고 정책 의도와 불일치하는지 판정한 뒤 별도 승인을 받는다. 이
문제는 automatic chain이 production에서 호출되지 않는 primary regression을 설명하지 않는다.

### 28.11 예상 source 범위와 보호 경계

별도 implementation 승인 시 우선 검토할 최소 LAVI-owned source는 다음이다.

```text
DepositAllAutoEntrypoint.java
    -> 기존 단일 callback/lazy runtime 유지, stale disabled comment만 정합화

AutoDepositPolicyCompositionRoot.java
    -> repository 하나와 그 repository를 사용하는 policy engine 조립

AutoDepositRuntime.java
    -> pressure chain instance lifecycle 및 tracker-before-chain tick 소유

DepositAllAutoConflictGuard.java
    -> exact StoreHomeTask storage conflict recognition

DepositAllInventoryPressureChain.java
    -> exact StoreHome conflict가 generic chain-selection/working-set NO_SAFE보다 먼저 적용되는
       좁은 decision boundary; 다른 state-machine 순서는 유지
```

직접 관련 테스트 후보:

```text
DepositAllAutoEntrypointRegistrationTest.java
DepositAllAutoConflictGuardTest.java
DepositAllInventoryPressureChainLifecycleTest.java
AutoDepositRuntime composition/identity/tick-order direct test
```

다음 source는 이번 forward composition 복구에서 변경하지 않는다.

```text
StoreHomeTask와 그 planner/executor/timeout/lifecycle
DepositAllCommand와 DepositAllInventoryTargetSelector
DepositAllTask, StoreInContainerTask, StoreInAnyContainerTask
TaskRunner, UserTaskChain, Task, InteractWithBlockTask
Baritone goal/path/input 소유권
Python Korean public/parser/admission/bridge readiness
wire protocol과 artifact schema
```

현재 package는 entrypoint, composition root, runtime, chain, policy, trusted repository,
interaction tracker와 conflict guard를 이미 책임별 LAVI-owned type으로 분리하고 있다. 이번
문서 범위에서는 새 manager, base class, package 이동 또는 broad folderization을 요구하지
않는다. Composition lifecycle, conflict classification과 pressure-chain decision order는
각각 현재 owning type에 남겨야 한다.

### 28.12 현재 최종 판정

```text
SOURCE ROOT CAUSE:                         CONFIRMED
GIT HISTORY CAUSAL CHANGE:                 CONFIRMED AT 740aa616
POLICY REJECTION AS PRIMARY CAUSE:         REJECTED
FULL COMMIT REVERT:                        REJECTED
OLD ENTRYPOINT WHOLESALE COPY:             REJECTED
FORWARD COMPOSITION RESTORATION:           DOCUMENTED, NOT IMPLEMENTED
AUTO-SIDE STORE_HOME CONFLICT RECOGNITION: MANDATORY FOR RESTORATION
NEW LOGGING BEFORE SOURCE FIX:              NOT REQUIRED FOR PROVEN PRIMARY CAUSE
PLAN-BUILDER [64,32] ISSUE:                SEPARATE CHARACTERIZATION GATE
TEST EXECUTION FOR THIS SECTION:           NOT RUN
CLEAN BUILD FOR THIS SECTION:              NOT RUN
POST-RESTORATION MINECRAFT RUNTIME PROOF:  NOT RUN
PRODUCTION CODE CHANGES FOR THIS SECTION:  NONE AT REVIEW TIME; SUPERSEDED BY §28.14
COMMIT OR PUSH FOR THIS SECTION:           NONE
```

최종 복구 완료 판정은 source가 존재하거나 unit test 하나가 통과했다는 사실만으로 내리지
않는다. `SOURCE_CONFIRMED_ACTIVE`, current test/build evidence, deployed artifact
identity와 actual occupied-slot 감소를 포함한 `RUNTIME_PATH_PROVEN_CURRENT`를 분리해
보고한다.

### 28.13 2026-08-29 독립 재검수 보정

이 절은 독립 검토본을 현재 live source와 다시 대조한 결과다. 검토본 안의 제안은 구현
승인이 아니라 반례와 lifecycle gate 후보로만 취급했다. 이 보정에서 Java, JSON, test,
build, JAR, Minecraft runtime, commit과 push는 변경하거나 실행하지 않았다.

#### 28.13.1 최종 판정과 용어 범위

| 질문 | 재검수 판정 |
| --- | --- |
| root-cause 반례 | 현재 source graph와 `740aa616` diff를 뒤집는 material counterexample 없음 |
| `regression`의 의미 | `740aa616` 당시 요구 위반이라고 단정하지 않음. 현재 다시 요구된 automatic pressure relief 기준의 source-level 회귀 |
| 복구 방식 | 전체 revert와 옛 entrypoint 복사는 계속 거부. 현재 composition에 forward-only로 복구 |
| StoreHome conflict | exact StoreHome root assignment를 generic selected-chain/working-set 거부보다 먼저 관찰 |
| 추가 pre-fix gameplay log | 불필요. 복구 뒤 bounded runtime proof와 provenance는 필요 |
| planner `[64,32]` | composition 복구와 분리된 characterization 단위 |

기존 문맥의 shared identity를 `same operation graph`로 부르지 않는다. 정확한 범위는
`same runtime composition graph`다.

```text
runtime-scoped identity
    AutoDepositRuntime
    AutoDepositTrustedDestinationRepository
    AutoDepositPolicyEngine
    AutoDepositOpenContainerBindingTracker
    DepositAllInventoryPressureChain
    TaskRunner

automatic operation-scoped identity
    AutoDepositContextSnapshot
    AutoDepositPlan
    AutoDepositMaintenanceTask
    trusted/general candidate queue
    destination manifest
    free-slot postcondition evidence
```

Repository와 tracker를 automatic operation마다 다시 만들면 trusted command revision,
StoreHome exact-open evidence와 automatic executor 관찰이 분리된다. 반대로 context, plan,
maintenance Task와 manifest를 runtime singleton으로 승격해서도 안 된다.

#### 28.13.2 StoreHome root-assignment authority

Safety chain selection이나 cached child path보다 `UserTaskChain`에 현재 할당된 root object가
권위다. 최소 behavior 판정은 다음으로 제한한다.

```text
UserTaskChain exists
current root instanceof StoreHomeTask
current root is not the idle task
revalidation observes the same root object identity
```

`UserTaskChain.isActive()`는 현재 `mainTask != null`과 같은 의미이므로 별도 필수 증거가
아니다. 특히 `StoreHomeTask.isActive()`를 요구하지 않는다. `runTask()`가 root를 할당한
직후에는 새 Task가 아직 첫 tick을 받지 않아 inactive일 수 있기 때문이다.

다음 값은 conflict identity로 사용하지 않는다.

```text
runner.getCurrentTaskChain()
UserTaskChain.getTasks() cached path
StoreHomeTask phase, timeout, current child or manifest
exact-open binding presence
diagnostic root generation
```

필요하면 한 client-tick 동안만 다음 immutable observation을 사용할 수 있다.

```text
StoreHomeConflictObservation
    observed current world identity and dimension
    UserTaskChain identity
    StoreHome root reference
    routeKind = STORE_HOME
    selected chain and diagnostic generation  // diagnostics only
```

첫 tick 전 StoreHome이 내부에 캡처할 operation world/dimension은 현재 public behavior API로
읽을 수 없다. 따라서 world/dimension 검증은 같은 observation tick에서 현재 context가
바뀌지 않았는지만 확인하며, 관찰하지 못한 StoreHome-owned context를 추정하거나 behavior
조건으로 꾸미지 않는다. Observation은 suppression 판단 후 폐기하며 여러 tick 동안 보관해
이미 끝난 root를 계속 막지 않는다.

#### 28.13.3 callback 순서와 실제 reconciliation

다음 두 순서를 모두 실제 `TaskRunner.tick()`과 `SingleTaskChain` reconciliation을 거쳐
검증한다.

```text
A. StoreHome assignment -> pressure callback
    exact StoreHome suppression
    automatic submission 0
    NO_SAFE_SURPLUS latch 0

B. pressure callback -> StoreHome assignment
    automatic root가 이미 만들어졌을 수 있음
    더 높은 safety chain이 없으면 priority 51 automatic chain이 UserTaskChain보다 먼저 tick
    maintenance context mismatch가 child/container work 전에 CANCELLED 전이
    이후 SingleTaskChain terminal reconciliation에서 automatic root clear
    다음 eligible selection에서 UserTaskChain / StoreHome 실행
```

`SingleTaskChain`은 chain tick 시작 시점에 `mainTask.isFinished()`를 검사한 뒤, 미완료이면
그 tick에서 Task를 실행한다. 따라서 maintenance Task가 실행 중 `CANCELLED`가 된 바로 같은
tick에 chain root까지 반드시 clear된다고 가정하지 않는다. 다음 selected reconciliation
tick에서 terminal/clear가 일어날 수 있다. Test는 고정 tick 수가 아니라 다음 ordering과
identity를 직접 검증한다.

```text
context mismatch
-> automatic child/container work 0
-> automatic-owned root only terminal/clear exactly once
-> handoff 뒤 automatic container click 0
-> submitted StoreHome root identity 보존
```

#### 28.13.4 initialization atomicity와 orphan-chain 경계

`TaskChain(TaskRunner)`는 constructor 안에서 즉시 `runner.addTaskChain(this)`를 호출하고,
`TaskRunner.addTaskChain()`에는 deduplication이나 removal API가 없다. 따라서 chain 생성은
단순 객체 생성이 아니라 runner mutation이다.

`DepositAllInventoryPressureChain`의 최종 4-argument constructor는 `super(runner)` 뒤에
`mod`, policy engine, repository identity와 binding을 검증한다. 이 post-registration
검증에서 예외가 나면 constructor가 정상 반환되지 않아도 partially initialized reference가
runner에 남을 수 있다. 다음 범위를 정확히 구분한다.

```text
runner null 또는 위임 인자 평가 중 실패
    -> self-registration 전에 실패 가능

final constructor의 super(...) 이후 dependency/identity 실패
    -> orphan registration 위험
```

이는 현재 production에서 이미 duplicate chain이 발생했다는 판정이 아니다. 현재는 pressure
chain construction 자체가 0이다. Forward restoration에서 새로 열리는 latent lifecycle
위험이다.

현재 entrypoint는 `runtimeFactory.apply(mod) -> registerCommands(mod) -> runtime field
assignment` 순서다. Field assignment 전에 밖으로 전파되는 unchecked failure가 있고 callback이
계속 실행되는 환경이라면 재시도가 가능하지만, Fabric이 반드시 다음 tick을 실행한다고
가정하지 않는다. Registrar 내부에서 처리되는 command error와 밖으로 전파되는 실패도
구분한다.

복구 구현 전 direct test는 controlled failure/retry seam으로 다음을 증명해야 한다.

```text
fallible dependency validation completes before pressure-chain construction
chain construction 뒤 stable runtime publication 전 escaping fallible step 0
failure path orphan/partially initialized pressure chain 0
successful retry 뒤 total pressure-chain registration 1
explicit second runner.addTaskChain(chain) 0
```

이 계약을 위해 shared `TaskChain`이나 `TaskRunner`를 변경하지 않는다. LAVI-owned composition
경계에서 검증과 publication 순서를 소유한다.

#### 28.13.5 runner activation ownership

현재 automatic start는 runner가 inactive일 때만 `enable()`하고 natural terminal,
interruption과 owned stop에서 `disable()`을 호출하지 않는다. `TaskRunner.disable()`은 현재
behavior stack을 먼저 pop한 뒤 모든 registered chain을 stop하므로 automatic operation이
자기 종료만을 이유로 호출해서는 안 된다.

```text
runner already active
    -> automatic terminal/interruption 뒤 global active state 변경 0

runner inactive
    -> automatic start enable exactly once
    -> automatic terminal/interruption disable 0
    -> global ChatClef stop/disable boundary만 release 가능한 owner
```

마지막 줄은 global stop이 곧 실행된다는 보장이 아니다. Auto가 유일한 enabler였으면 runner와
behavior push가 terminal 뒤에도 남을 수 있다. Balanced automatic release가 새 요구라면
별도 activation-lease 설계가 필요하며 이번 composition 복구에 포함하지 않는다.

#### 28.13.6 추가 lifecycle gate와 증거 상태

§28.9의 기존 표에 더해 다음 경계를 직접 검증한다.

```text
StoreHome root assigned but not first-ticked
StoreHome root retained while safety chain selected
StoreHome terminal/stopped but not yet cleared from UserTaskChain
automatic root not first-ticked then safety-preempted
automatic child active then safety-preempted
world leave or ChatClef disable during RUNNING
no double stop or illegal terminal transition
StoreHome exact binding A cannot satisfy automatic candidate B
GUI close and world/dimension change invalidate stale binding
manual deposit under safety preserves its existing decision order
trusted repository revision change does not duplicate one armed episode
```

StoreHome conflict를 기존 storage-conflict처럼 `WAIT_FOR_REARM`으로 보내면 StoreHome이 slot
relief 없이 끝나도 28/36 low-water 또는 기존 meaningful-change 조건 전에는 automatic deposit이
즉시 재평가되지 않는다. 즉시 재평가를 원하면 별도 상태와 별도 승인이 필요하다.

복구 뒤에는 기존 bounded marker를 우선 사용해 registration, task start, immutable plan,
StoreHome suppression, cancellation/interruption과 actual occupied-slot delta를 operation chronology로
증명한다. 별도로 branch/HEAD, dirty file inventory, source diff/hash, exact clean build command,
built/active JAR size와 SHA-256을 기록한다.

```text
INDEPENDENT ROOT-CAUSE REVIEW:              PASS
MATERIAL COUNTEREXAMPLE:                   NONE FOUND IN INSPECTED SNAPSHOT
FORWARD-ONLY COMPOSITION:                  PASS WITH LIFECYCLE GATES
STORE_HOME EARLY CONFLICT:                REQUIRED
INITIALIZATION FAILURE/RETRY CONTRACT:     REQUIRED
RUNNER ACTIVATION OWNERSHIP TEST:          REQUIRED
PRE-FIX GAMEPLAY LOGGING:                  NOT REQUIRED
POST-RESTORATION RUNTIME PROOF:             REQUIRED
PLAN-BUILDER [64,32]:                      SEPARATE CHANGE UNIT
FORWARD COMPOSITION IMPLEMENTATION:        NOT IMPLEMENTED AT REVIEW TIME; SUPERSEDED BY §28.14
AUTOMATIC-PRESSURE JAVA / TEST SOURCE:     UNCHANGED BY THIS REVIEW; SEE §28.14
TEST EXECUTION / BUILD / MINECRAFT:        NOT RUN BY THIS REVIEW
COMMIT / PUSH:                             NOT PERFORMED
```

### 28.14 2026-08-29 forward-only production composition 구현 및 현재 증거

사용자가 dormant automatic pressure path의 복구를 명시적으로 승인한 뒤, 기존 policy를
다시 작성하지 않고 현재 LAVI-owned composition graph에 연결했다. 이 절은 §28.12와
§28.13의 구현 전 판정을 대체하되 당시 source review 자체를 삭제하지 않는다.

#### 28.14.1 source graph와 initialization atomicity

현재 runtime-scoped graph는 다음 identity를 유지한다.

```text
AutoDepositPolicyCompositionRoot
    -> AutoDepositTrustedDestinationRepository exactly one
    -> AutoDepositPolicyEngine using that exact repository
    -> AutoDepositRuntime
         -> AutoDepositOpenContainerBindingTracker exactly one
         -> trusted command registrar using the same repository/tracker
         -> StoreHomeTaskFactory using the same repository/tracker
         -> DepositAllInventoryPressureChain exactly one
              -> same TaskRunner
              -> same policy engine/repository/tracker
```

`auto/composition/`의 immutable preparation records가 null, runner/mod identity와
repository mismatch를 `TaskChain` 생성 전에 검증한다. 알려진 dependency validation과
collaborator 생성 실패는 모두 PREPARE에 포함된다. `AutoDepositRuntime` constructor의 마지막
statement만 standard production `TaskRunner`에 pressure chain을 COMMIT하며, 그 뒤 알려진
fallible initialization step은 없다. 이 constructor가 반환된 뒤 entrypoint는 runtime field를
먼저 publish하고 diagnostic registration event를 기록한다. Shared `TaskChain`/`TaskRunner`에
removal, dedupe 또는 명시적인 두 번째 `addTaskChain` 호출은 추가하지 않았다. 이 판정은
임의로 예외를 던지는 test subclass나 injected factory까지 절대 무오류라고 주장하지 않는다.

Controlled failure/retry test의 현재 결과는 다음이다.

```text
repository mismatch failure -> registered pressure chain 0
null exact binding failure   -> registered pressure chain 0
controlled retry success     -> total registered pressure chain 1
repeated entrypoint attempt  -> same runtime and same chain identity
```

#### 28.14.2 tick 및 StoreHome decision order

`AutoDepositRuntimeTickSequence`는 한 tick의 핵심 순서를 다음과 같이 고정한다.

```text
AutoDepositOpenContainerBindingTracker.onEndClientTick()
-> DepositAllInventoryPressureChain.onEndClientTick()
```

33/36 high-water의 `ARMED` 경계에서는 current `UserTaskChain` root를 한 번 캡처하고 다음
순서를 사용한다.

```text
captured root identity revalidation
-> exact StoreHomeTask + non-idle suppression
-> generic selected-chain gate
-> existing manual deposit conflict
-> working-set resolution
-> existing policy planning
-> per-operation AutoDepositMaintenanceTask submission
```

StoreHome 식별에는 `StoreHomeTask.isActive()`, selected chain, cached child path, phase, timeout,
manifest, exact binding 또는 diagnostic generation을 사용하지 않는다. 첫 tick 전 root,
safety-selected 동안 보존된 root와 stopped/terminal이지만 아직 clear되지 않은 root도 같은
exact identity로 suppression된다. Replacement root가 관찰되면 stale captured root는
suppression authority를 잃는다.

실제 `TaskRunner.tick()` 통합 테스트는 다음 세 경계를 고정한다.

```text
StoreHome-first
    production pressure callback에서 automatic submission 0
    -> WAIT_FOR_REARM
    -> first eligible runner selection은 exact StoreHome root

automatic-first
    StoreHome root replacement
    -> immutable context mismatch가 child/container work 전에 CANCELLED
    -> 다음 reconciliation에서 automatic root clear
    -> 다음 eligible selection에서 exact StoreHome root

automatic-first + safety
    automatic chain onInterrupt
    -> active automatic-owned root/child를 각각 exactly once stop
    -> additional automatic child tick/container click 0
    -> StoreHome first tick/clock consumption 0 while safety owns selection
    -> safety 종료 뒤 exact StoreHome root 선택
```

World leave와 ChatClef disable도 active automatic-owned tree만 exactly once 정리하며
`TaskRunner.disable()`을 호출하지 않는다.

#### 28.14.3 보존한 policy와 별도 deferred defect

Production default는 기존 `DepositAllInventoryPressureReader`, policy engine, plan builder,
trusted/general execution과 33/28 hysteresis를 그대로 사용한다. 테스트가 실제
occupied-slot delta를 28/31/33으로 주입할 수 있도록 automatic-only read port를 두었지만
production constructor는 전과 같은 reader를 생성한다.

```text
ending occupied 28 -> FULL_RELIEF
ending occupied 31 -> PARTIAL_RELIEF
ending occupied 33 -> NO_SLOT_RELIEF
```

다음은 이번 source change에 포함하지 않았다.

```text
planner [64,32] break/physical-slot 문제
manual @deposit_all 또는 @deposit selector
StoreHome planner, transfer, timeout, terminal 또는 behavior
TaskRunner, UserTaskChain, Task, InteractWithBlockTask
Baritone goal/path/input ownership
automatic policy JSON과 threshold 33/36, low-water 28/36
```

#### 28.14.4 current test/build/runtime evidence

2026-08-29 현재 source에서 retry 또는 실패 test의 deselection 없이 실행한 결과다. 아래
`skipped=14`는 Gradle 결과 XML에 보고된 기존 registry-free Minecraft item fixture abort이며
새 skip, retry 또는 테스트 약화로 만든 수치가 아니다.

```text
focused command:
  .\gradlew.bat :1.20.1:test
    --tests 'lavi.minecraft.task.container.deposit.auto.*'

focused result:
  BUILD SUCCESSFUL in 21s
  26 test classes, 62 tests
  executed 48, skipped 14, failures 0, errors 0

targeted command scope:
  automatic deposit + manual DepositAll + StoreHome command/object identity/decision order

targeted result:
  final forced test-graph execution: --rerun-tasks, no test retry
  BUILD SUCCESSFUL in 2m 26s; 46/46 test-graph tasks executed
  38 test classes, 93 tests
  executed 79, skipped 14, failures 0, errors 0
```

14건의 abort는 registry-free `Item` identity를 만들 수 없는 이 JUnit runtime에서 발생한다.
영향 범위는 hard protection, category reserve, item classification, plan builder,
destination/recovery manifest, surplus selection과 working-set item fixture다. 따라서 이번
결과는 새 composition, decision order, TaskRunner handoff, lifecycle cleanup과 occupied-slot
delta의 current PASS 증거이며, 위 14개 item-dependent 정책 case를
`TEST_PASSED_CURRENT`로 승격하는 증거는 아니다. 해당 정책 production source는 이번
복구에서 변경하지 않았다.

§28.13.6의 추가 gate 중 root-before-first-tick, safety-selected StoreHome,
stopped-but-uncleared root, never/active automatic safety interruption, world leave/disable와
double-stop 방지는 current direct coverage다. 다음 네 항목은 이번 composition 복구의 직접
test coverage가 아니므로 `PARTIAL / NOT DIRECTLY COVERED`로 유지한다.

```text
StoreHome exact binding A vs automatic candidate B mismatch rejection
GUI close and explicit cross-dimension stale-binding invalidation
manual deposit + safety combined decision order
trusted repository revision change + production submission-count integration
```

이 결과는 targeted Java test evidence이며 clean forced build evidence가 아니다. 이번 복구
뒤 `clean build --rerun-tasks`, built/active JAR SHA-256, active CurseForge artifact와 Minecraft
33/36 automatic pressure 재현은 실행하지 않았다. 따라서 현재 증거 상태는 다음과 같다.

```text
SOURCE_CONFIRMED_ACTIVE:                 YES
TEST_SOURCE_PRESENT:                    YES
TEST_PASSED_CURRENT:                    YES
BUILD_PASSED_CURRENT:                   NO / NOT RUN FOR THIS RESTORATION
FILE_ARTIFACT_MATCH_CONFIRMED:           NO / NOT RUN FOR THIS RESTORATION
RUNTIME_PATH_PROVEN_CURRENT:             NO / NOT RUN FOR THIS RESTORATION
artifactParity:                          PARITY_UNPROVEN
PLAN-BUILDER [64,32]:                    DEFERRED SEPARATE CHANGE UNIT
COMMIT / PUSH:                           NOT PERFORMED
```

## 29. 2026-08-30 transfer / movement / Carry On incident focused review

2026-08-30 automatic pressure reproduction에서 관찰된 aggregate target과
physical cursor stack, container accepted-delta observability/correlation gap,
parent movement checker,
candidate invalidation, 반복 chest acquisition 및 Carry On attribution은 다음
별도 문서에서 source와 runtime evidence를 대조한다.

[ChatClef Automatic Deposit Transfer / Movement / Carry On Diagnostics Review](chatclef-auto-deposit-transfer-movement-carryon-diagnostics-review-2026-08-30.md)

첫 bounded diagnostics-only change unit의 identity, payload, lifecycle, stop gate 및
`16 grouped scenarios / 24 contract assertions`는 다음 별도 문서가 소유한다.

[ChatClef Automatic Deposit Slice A Diagnostics Contract](chatclef-auto-deposit-slice-a-diagnostics-contract-2026-08-30.md)

이 연결 절은 section 28의 composition/history 소유권이나 section 28.14의
2026-08-29 source/test/build/runtime ledger를 소급해 바꾸지 않는다. 이후 incident
evidence의 보정 판정은 다음과 같다.

```text
automatic pressure runtime activation in captured incident: LOG_CONFIRMED
aggregate target x10 -> physical cursor x64: SOURCE_CONFIRMED + LOG_CONFIRMED
strict `roomLeft > sourceCount` exact-fit rejection mechanism: SOURCE_CONFIRMED
runtime destination churn caused by that mechanism: UNPROVEN
parent movement checker during child GUI transfer: SOURCE_CONFIRMED
MOVEMENT_PROGRESS_FAILED at ticks 52877 and 53358: LOG_CONFIRMED
check-return-false as the direct trigger of both invalidations: SOURCE_CONFIRMED + LOG_CONFIRMED
candidate invalidation and chest reacquisition: LOG_CONFIRMED
actual route-child stop/replacement for each invalidation: UNPROVEN
distance/mining mode, baseline, elapsed and reset provenance: UNAVAILABLE
GUI transfer as the exact internal cause of both false results: INFERENCE
stored=0/10 internal cause: UNPROVEN
notStored x9/x8 as available-and-unstored request cap: SOURCE_CONFIRMED + LOG_CONFIRMED
Carry On exact target/action owner: UNPROVEN
explicit final occupiedCount=28 field: UNAVAILABLE
final automatic 33 -> 28 plus same-identity resume ledger: REVIEW_REPORTED / UNPROVEN
automatic interval end, user-task resume and later natural completion: LOG_CONFIRMED
FINAL_ROOT_CAUSE: UNPROVEN
NEXT_DIRECTION: BOUNDED DIAGNOSTICS-ONLY
IMMEDIATE_BEHAVIOR_CHANGE: NOT AUTHORIZED
```

`MOVEMENT_PROGRESS_FAILED`는 per-item `DepositAllTask` store root나 automatic
maintenance terminal이 아니라 현재 candidate/store generation의 무효화 decision이다.
실제 route-child stop/replacement는 이후 generic Task reconciliation에서 별도로
관찰한다. diagnostics budget 소진도 gameplay terminal이 아니다. 향후 event model은
slot mutation, transfer-attempt close, candidate invalidation, route-child close,
per-item store-root close, maintenance logical terminal, pressure-chain owned-run close와
diagnostic coverage close를 서로 다른 identity와 reason으로 유지해야 한다.

이번 문서화 작업은 Java, test 또는 resource source, `@store_home`, manual
`@deposit_all`, `@deposit`, TaskRunner, Baritone, `InteractWithBlockTask`,
`StoreInContainerTask`, Carry On behavior를 변경하지 않았고 test, build, JAR 배포,
Minecraft 재현, commit 또는 push를 실행하지 않았다.
