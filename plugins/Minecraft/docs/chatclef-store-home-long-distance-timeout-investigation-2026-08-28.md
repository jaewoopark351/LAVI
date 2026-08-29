<!-- 20260828_openai: Documented the long-distance STORE_HOME candidate-budget exhaustion and bounded diagnostics direction without changing runtime behavior. -->
<!-- 20260828_openai: Strengthened direct timeout counters, reserved boundary budgets, operation caps, progress state, and artifact identity after CONDITIONAL PASS review. -->
<!-- 20260828_openai: Recorded the controlled runtime reproduction that directly proved candidate timeout during active navigation and the reviewed phase-scoped timeout direction. -->
<!-- 20260828_openai: Fixed future timeout-clock ownership, semantic reset, sticky local handoff, stable reason, and pending-precedence contracts after final review. -->
<!-- 20260828_openai: Recorded the separately authorized phase-scoped timeout source implementation; build and runtime verification remain pending. -->
<!-- 20260829_openai: Split directly affected state, slot, transfer, fingerprint, and diagnostic responsibilities while preserving STORE_HOME behavior. -->
<!-- 20260829_openai: Folderized the LAVI-owned StoreHomeTask workflow into focused candidate, session, operation, initialization, context, and timeout collaborators. -->
<!-- 20260829_openai: Completed the thin-facade split with dedicated task composition, lifecycle, and view packages. -->
<!-- 20260829_openai: Recorded the clean forced build, matching deployed JAR, and two successful phase-scoped STORE_HOME runtime operations while retaining PARITY_UNPROVEN. -->
<!-- 20260829_openai: Corrected the final direct-test topology, decision-order scope, and operation-specific runtime claim. -->
<!-- 20260829_openai: Recorded registry parity, direct STORE_HOME semantic gates, and the new clean forced build without extending deployment or runtime claims. -->
<!-- 20260829_openai: Reconciled the final record with the latest clean build, deployed artifact, runtime completion, and source commit evidence. -->

# ChatClef STORE_HOME Long-Distance Timeout Investigation

문서 상태: `DIRECT_CAUSE_PROVEN_PHASE_SCOPED_TIMEOUT_BUILD_VERIFIED_DEPLOY_FILE_IDENTITY_VERIFIED_RUNTIME_BEHAVIOR_OBSERVED_PARITY_UNPROVEN_REGISTRY_PARITY_VERIFIED_DIRECT_TESTS_VERIFIED_LATEST_CLEAN_BUILD_VERIFIED_SOURCE_COMMITS_CREATED`

기준일: 2026-08-28, 상태 갱신일: 2026-08-29

이 문서는 Fabric ChatClef 1.20.1에서 장거리 `@goto -745 60`은 완료됐지만
같은 집 좌표대의 `@store_home`은
`NO_USABLE_TRUSTED_DESTINATION`으로 종료된 회차를 분석한다.

이 문서의 최초 범위는 이미 완료된 diagnostics-only 구현, clean forced build, 동일 JAR
배포와 통제 runtime 재현 결과를 기록하는 docs-only 작업이었다. 이후 별도 사용자
승인으로 phase-scoped timeout behavior와 focused test source를 구현했다. 2026-08-29
추가 승인으로 clean forced build, 동일 JAR 배포, Minecraft runtime 재현과 로그 검토까지
완료했으며 현재 검증과 Git 기록 상태는 §12와 §12.2가 소유한다.

## 1. 판정 요약

이번 `@store_home`의 직접 실패 원인은 통제 재현에서 증명됐다. 세 trusted candidate는
각각 `candidateTicks=2400`에서 `candidateTimeoutConditionMatched=true` 판정을 받은 뒤
실제로 `candidate_timeout`으로 제거됐다. 세 판정 모두
`operationTimeoutConditionMatched=false`였고, Baritone path와 candidate goal이 active인
상태에서 최근 player movement와 best-distance improvement가 관찰됐다.

초기 회차에는 다음 산술 증거만 있어 candidate budget exhaustion을 강하게 지지하는
단계에 머물렀다.

```text
StoreHomeTask 시작 clientTickId: 229087
terminal clientTickId:          236289
elapsed clientTickId 차이:      7202 ticks

MAX_CANDIDATE_TICKS:            2400
trusted candidate 수:           3
2400 * 3:                       7200 ticks
root 시작/terminal 경계 오차:   2 ticks
```

후속 diagnostics-only 재현은 이 추론의 미관찰 경계를 직접 닫았다.

```text
candidate 1 timeout:
    candidateTicksBeforeDecision=2400
    operationTicksBeforeDecision=2400
    last progress=12 client ticks 전

candidate 2 timeout:
    candidateTicksBeforeDecision=2400
    operationTicksBeforeDecision=4800
    last progress=2 client ticks 전

candidate 3 timeout:
    candidateTicksBeforeDecision=2400
    operationTicksBeforeDecision=7200
    last progress=1 client tick 전

all three:
    candidateTimeoutConditionMatched=true
    operationTimeoutConditionMatched=false
    baritonePathingActive=true
    goalMatchesCandidatePosition=true
    screenName=none
    exactBindingEverObserved=false
    containerSessionState=NO_ACTIVE_SESSION
```

따라서 observed runtime의 직접 흐름은 다음으로 확정한다.

```text
candidate A lifetime budget 소진
    -> candidate B
candidate B lifetime budget 소진
    -> candidate C
candidate C lifetime budget 소진
    -> trusted_candidates_exhausted
    -> NO_USABLE_TRUSTED_DESTINATION
```

세 후보 timeout 뒤 operation은 `operationTicks=7201`에서
`NO_USABLE_TRUSTED_DESTINATION`, `reason=trusted_candidates_exhausted`로 종료됐다.
`STORE_HOME_CANDIDATE_ACTIVATED`는 0건이고 저장된 아이템도 0개였다. 그러므로 이번
회차의 직접 원인은 Baritone stall, exact binding 실패 또는 operation timeout이 아니다.
장거리 navigation과 local open/binding을 하나의 후보별 고정 lifetime으로 제한하고,
실제 진행 상태를 기존 timeout decision에 반영하지 않는 의미 충돌이다.

이 판정은 “timeout 의미를 고치면 상자 open과 binding도 반드시 성공한다”까지
증명하지 않는다. Local interaction 경계는 이번 실행에서 도달하지 않았으므로 behavior
수정 뒤 별도 검증해야 한다.

## 2. Evidence identity

초기 산술 증거를 제공한 요청 identity는 다음과 같다.

```text
requestId:     lavi-input-ko-d85048c34d25438fafb4b126e9bcb0d3
correlationId: lavi-1eff03236c504552af685948b17e02f6
command:       store_home
source:        lavi_chat_mic_router
root Task:     lavi.minecraft.task.container.home.execution.StoreHomeTask
```

Direct timeout boundary를 제공한 통제 재현 identity는 다음과 같다.

```text
requestId:      lavi-input-ko-15758f4811b54e83a9d0949dab5ce117
correlationId:  lavi-1a9a2be198f5400ca649bc544e0d4745
operationId:    3781
runManifestId:  store-home-runtime-02563f05-02a4-487e-aadb-0dd010b8d78f
command:        store_home
source:         lavi_chat_mic_router
root Task:      lavi.minecraft.task.container.home.execution.StoreHomeTask
diagnostics:    BOUNDARY
```

검토한 evidence는 다음 파일의 같은 실행 구간이다.

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\config\lavi\automatic-deposit-trusted-destinations.json
```

분석 당시 보존한 read-only bundle은 다음 위치에 있다. 이 bundle은 조사 편의를 위한
local evidence이며 tracked source 또는 build input으로 간주하지 않는다.

```text
logs/chatgpt_storehome_goto_2026-08-28/
```

관련 current worktree source의 timeout 상수와 control flow도 read-only로 대조했다.
통제 재현에 사용한 build output과 active CurseForge JAR은 다음 file identity로
일치했다.

```text
file:   chatclef-1.20.1-0.18.23.jar
size:   7,007,701 bytes
sha256: B5C63F0D5D8D3043151A10107BB5C7CB213188651483F62CCFD6E1F147496A55
runtime code source:
    C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\chatclef-1.20.1-0.18.23.jar
```

Runtime manifest 자체는 repository/source/build-input identity가 외부 property로 완전히
공급되지 않아 `artifactParity=PARITY_UNPROVEN`을 정직하게 유지했다. 따라서 이 문서는
runtime이 직접 출력한 counter와 branch 결과를 observed behavior 증거로 사용하고,
manifest가 확인하지 못한 complete source identity를 추정하지 않는다. Build output과
deployed/runtime code-source path의 file-level SHA-256 일치는 별도 read-only 검증 결과다.

## 3. Trusted destination과 runtime timeline

해당 instance에는 같은 world/dimension의 enabled destination 세 개가 있었다.

| Destination ID | Exact position |
| --- | --- |
| `td_1bf57c47e35f1f5a9e871b53` | `-745,69,60` |
| `td_44206907b0cfd8b0a5ad8d3e` | `-746,70,59` |
| `td_0855c956959993aec70d7fac` | `-746,70,60` |

Destination ID는 `worldKey|dimension|x|y|z` canonical identity의 기존 SHA-256
표시 규칙과 설정 좌표를 대조해 확인했다.

초기 회차에서 관찰된 흐름은 다음과 같다.

| Evidence boundary | Runtime observation |
| --- | --- |
| dispatch | `ACCEPT_REQUEST`, elapsed 약 1 ms |
| first sampled candidate | `td_1bf...`, `NAVIGATE_AND_OPEN`, `session=-1`, elapsed 약 5.0 s |
| second sampled candidate ID | `td_442...`, `NAVIGATE_AND_OPEN`, `session=-1`, elapsed 약 120.6 s |
| third sampled candidate ID | `td_085...`, `NAVIGATE_AND_OPEN`, `session=-1`, elapsed 약 241.0 s |
| local terminal | `NO_USABLE_TRUSTED_DESTINATION`, `reason=trusted_candidates_exhausted`, elapsed 약 360.1 s |
| typed terminal | `storedItems=0`, `touchedStacks=0`, `remainingStacks=22` |

세 후보 모두 관찰 시점에 `NAVIGATE_AND_OPEN`, `session=-1`이었다. 따라서 이 회차에서
trusted-container session activation, exact transfer 또는 paired-delta confirmation에
진입했다는 증거는 없다.

통제 재현에서는 후보별 decision과 실제 rejection이 다음처럼 직접 연결됐다.

| Candidate | Target | Start position / distance² | Timeout position / distance² | Last progress | Decision |
| ---: | --- | --- | --- | ---: | --- |
| 1 | `-745,69,60` | `650,69,467` / `2,111,674` | `488,62,260` / `1,560,338` | 12 ticks 전 | candidate 2400 / operation 2400; candidate true, operation false |
| 2 | `-746,70,60` | `488,62,260` / `1,562,820` | `282,62,191` / `1,074,009` | 2 ticks 전 | candidate 2400 / operation 4800; candidate true, operation false |
| 3 | `-746,70,59` | `282,62,191` / `1,074,272` | `88,62,94` / `696,845` | 1 tick 전 | candidate 2400 / operation 7200; candidate true, operation false |

세 decision은 모두 기존 counter 증가와 `>=` 평가 뒤, candidate 제거 side effect 전에
`capturedBeforeDecisionSideEffects=true`로 기록됐다. 이어지는
`STORE_HOME_CANDIDATE_REJECTED`는 실제 제거와 다음 후보 선택 또는 queue exhaustion을
별도 event로 확인했다. 마지막 후보 timeout 당시에도 target까지 3차원 직선거리 기준
약 835 blocks가 남아 있었고 screen, supported handler, exact binding과 container session은
한 번도 관찰되지 않았다. 이는 local interaction 실패가 아니라 navigation 중 고정
candidate lifetime 소진임을 보여준다.

```text
operation start:  21:53:38, clientTickId=1985
candidate 1 end:  21:55:38, clientTickId=4384
candidate 2 end:  21:57:38, clientTickId=6784
candidate 3 end:  21:59:38, clientTickId=9184
terminal:         21:59:38, clientTickId=9185
bridge elapsed:   about 360.145 seconds
terminal result:  NO_USABLE_TRUSTED_DESTINATION
stored items:     0
remaining stacks: 23
```

Source의 2,400은 wall-clock millisecond가 아니라 `StoreHomeTask.onTick()`이 active한
동안 증가하는 candidate tick 예산이다. 반면 2절의 7,202는 두 lifecycle event의
`clientTickId` 차이다. Root가 그 사이 모든 client tick에서 active였다는 direct counter
event는 없다. 정상적인 20 tick/s 환경에서는 2,400 active tick이 명목상 약 120초지만,
pause, lag, safety-chain 선점과 실제 tick rate가 있으면 wall-clock 또는 client-tick
delta와 정확히 같다고 가정하지 않는다. 이번 회차에서는 sampled candidate 변화와 약
360초 terminal이 그 명목 환산에 거의 일치했다.

## 4. `@goto`와 `@store_home`의 계약 차이

### 4.1 `@goto -745 60`

두 좌표 형식의 `@goto`는 `GetToXZTask`를 만들고 Baritone `GoalXZ`를 사용한다.
완료 조건은 현재 block X/Z가 target X/Z와 같은지다.

```text
required:
    X == -745
    Z == 60

not required:
    target Y 도달
    상호작용 가능한 면 확보
    상자 바라보기
    right click
    container screen/handler open
    exact trusted destination binding
```

같은 X/Z target의 `GetToXZTask` 자연 완료가 서로 다른 실행에서 세 번 관찰됐다.

```text
881.165 seconds
980.684 seconds
601.682 seconds
```

각 완료는 `finishTriggerHint=natural_task_state_transition`과
`actuallyDone=true`를 기록했다. 마지막 실행의 start/finish `clientTickId`는 각각
`241904`와 `253938`이므로 elapsed client-tick 차이는 12,034다.

### 4.2 `@store_home`

`StoreHomeCandidateAttempt`는 후보마다 다음 child를 만든다.

```text
new InteractWithBlockTask(candidate.position())
```

`StoreHomeTask`는 exact open-container binding이 없으면
`NAVIGATE_AND_OPEN`에서 같은 child를 반환한다. 이 경로는 단순 X/Z 도달 외에
상호작용 가능한 접근, reach, 시선, right click, 실제 screen/handler open과
exact destination binding을 요구한다.

Generic `InteractWithBlockTask.isFinished()`는 현재 `false`를 반환한다. 이 사실만으로
버그라고 판정하지 않는다. 현재 구조에서는 parent `StoreHomeTask`가 exact GUI binding과
operation terminal을 소유한다. 따라서 STORE_HOME 전용 요구를 만족시키기 위해 generic
child의 완료 계약을 바꾸지 않는다.

두 명령은 completion predicate와 child lifecycle이 다르므로 동일 조건의 직접 성능
A/B가 아니다. `@goto` 성공은 상자 open 성공을 보장하지 않는다. 다만 정상적인 장거리
이동이 601~980초 걸릴 수 있다는 관찰은 후보 전체 lifetime 2,400 tick이 장거리
navigation을 조기에 자를 수 있음을 보여준다.

통제 재현은 이번 실행에서 두 명령의 직접 결과 차이를 더 좁혔다. `@store_home`은
stricter local interaction과 binding 조건을 검사해서 실패한 것이 아니라, 그 조건에
도달하기 전에 parent `StoreHomeTask`의 후보별 2,400-tick timeout으로 종료됐다.
`@goto`에는 이 STORE_HOME candidate lifetime이 없다. 따라서 일반 completion contract의
차이와 별개로 이번 장거리 실패의 직접 차이는 StoreHome-owned candidate timeout이다.

## 5. Timeout control flow와 정책 긴장

Read-only current dirty source의 관련 상수는 다음과 같다.

```text
MAX_CANDIDATE_TICKS = 2400
MAX_OPERATION_TICKS = 12000
```

Candidate tick은 navigation, open 시도와 container session lifetime 전체에서 증가한다.
2,400 tick에 도달하면 pending transfer 유무에 따라
`candidate_timeout_with_pending_transfer` 또는 `candidate_timeout` 경로로 간다.
Pending이 없으면 current candidate를 제외하고 다음 후보를 선택한다.

초기 실행의 elapsed client-tick delta는 7,202이므로 source의 12,000 active
operation-tick limit과 정렬되지 않았다. 통제 재현은 이를 직접 확인했다. 세 candidate
decision에서 `operationTimeoutEvaluated=true`였지만
`operationTimeoutConditionMatched=false`였고, terminal 직전 operation counter는
7,201이었다. 따라서 이번 terminal의 원인을 operation timeout이라고 쓰지 않는다.

그러나 candidate limit만 크게 늘리는 future patch도 충분하지 않다. 마지막
`@goto` 자연 완료의 start/finish elapsed `clientTickId` 차이는 12,034였고 다른 두
실행은 wall-clock상 더 길었다.
완료 조건이 다른 점을 감안해도 12,000 tick global operation budget은 장거리
STORE_HOME의 다음 제한이 될 수 있다.

Canonical trusted destination 정책은 같은 world/dimension의 enabled destination에
절대 거리 상한을 두지 않고 거리를 ordering에만 사용한다. Fixed finite timeout 자체가
그 정책과 논리적으로 양립 불가능한 것은 아니지만, 진행 중인 장거리 navigation까지
절대 lifetime으로 잘라내면 사실상의 거리 상한을 만들 수 있다. 이번 evidence는 이
implementation gap 후보를 실제 runtime에서 드러냈다.

## 6. Baritone NPE의 분리 판정

초기 간접 회차(`requestId=lavi-input-ko-d85048c34d25438fafb4b126e9bcb0d3`,
`correlationId=lavi-1eff03236c504552af685948b17e02f6`)의 `latest.log`에는 다음 경계가
한 번 기록됐다.

```text
19:00:51
event=USER_BLOCK_RANGE_NULL_INPUT_OBSERVED
UserBlockRangeTracker.updateState
getBlockState(null)
Baritone Pathing exception: NullPointerException
```

Current source는 `bpos == null`을 관찰해 log한 뒤 같은 `bpos`를
`world.getBlockState(bpos)`에 전달한다. 따라서 null dereference는 실제 defect
evidence다.

이 exception은 해당 path calculation을 실패시키거나 재계산 비용을 만들었을 수 있다.
하지만 그 뒤에도 Baritone path output이 이어졌고, 이후 sampled candidate timing과
전체 elapsed client-tick 산술도 2,400-tick source budget 설명에 계속 정렬됐다.
그러므로 NPE를 이번 elapsed `clientTickId` 7,202 terminal의 주원인, 완전히 무해한
경고 또는 timeout 변경의 일부로 단정하지 않는다.

이 문제는 STORE_HOME timeout과 별도 change unit이다. `UserBlockRangeTracker`는
upstream-derived engine 경계이므로 future behavior fix가 필요하면
[ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)의
engine-divergence gate, 별도 source 승인과 독립 회귀 검증을 거친다. 이번 문서는 exact
hunk 또는 behavior 변경을 승인하지 않는다.

같은 evidence set의 다른 시간대에 나타난 scanner collection
`ArrayIndexOutOfBoundsException`도 이 null defect와 자동으로 같은 원인으로 묶지 않는다.

## 7. Diagnostics-only 보강 계약과 runtime 결과

초기 회차에서 직접 빠졌던 evidence는 “각 후보가 timeout 순간 어디에 있었고 계속 진행
중이었는가”와 “실제 Task-owned counter가 어느 비교에서 timeout branch를 선택했는가”였다.
Timeout 값을 바꾸지 않는 diagnostics-only source가 다음 event family를 LAVI-owned
STORE_HOME 경계에 추가했고, 통제 재현에서 필요한 event가 실제 관찰됐다. 이 절은 구현
전 계약과 구현 뒤 검증 기준을 함께 보존한다.

```text
STORE_HOME_CANDIDATE_ATTEMPT_STARTED
STORE_HOME_CANDIDATE_PROGRESS_SUMMARY
STORE_HOME_CANDIDATE_TIMEOUT_DECISION
STORE_HOME_OPERATION_TIMEOUT_DECISION
STORE_HOME_CANDIDATE_REJECTED
STORE_HOME_CANDIDATE_ACTIVATED
STORE_HOME_OPERATION_TERMINAL_SUMMARY
DIAGNOSTIC_SESSION_CAP_REACHED
```

통제 재현의 operation-local 결과는 다음과 같다.

```text
STORE_HOME_RUN_MANIFEST:                 1
STORE_HOME_OPERATION_STARTED:            1
STORE_HOME_CANDIDATE_ATTEMPT_STARTED:    3
STORE_HOME_CANDIDATE_TIMEOUT_DECISION:   3
STORE_HOME_CANDIDATE_REJECTED:           3
STORE_HOME_CANDIDATE_ACTIVATED:          0
STORE_HOME_OPERATION_TERMINAL_SUMMARY:   1

operationDiagnosticAcceptedCount:        35
operationProgressAcceptedCount:          24
operationDiagnosticSuppressedEventCount: 7173
operationDiagnosticCapEventEmitted:      false
sessionSuppressedEventCountForOperation: 0
taskBehaviorAffectedByDiagnostics:       false
```

대량 suppressed count는 매 tick 상태를 파일에 출력하지 않고 candidate당 progress cap,
operation progress cap과 semantic deduplication이 작동했다는 뜻이다. Timeout decision,
실제 rejection과 terminal은 sampled progress와 분리돼 모두 남았다.

기존 `STORE_HOME_MANIFEST_STALE` event 또는 failure stage를 timeout 진단에 재사용하지
않는다. Timeout은 manifest identity와 다른 operation/candidate lifetime 책임이다.
기존 exception/error channel도 유지하며 timeout event에 full stack을 복제하지 않는다.

### 7.1 Shared identity, client clock와 Task counter

모든 STORE_HOME timeout event는 existing command-context appender를 정확히 한 번
사용하고 다음 identity를 중복 없이 연결한다.

```text
runManifestId
operationId
commandContextAvailable
commandRequestId / commandCorrelationId
commandSessionId / commandConnectionGeneration
candidateAttemptOrdinal / candidateCount / candidateQueueRemaining
candidateId / destinationId / candidatePosition
phase / topLevelTask / childTaskClass / childTaskRunId / childTaskRunOrdinal
```

Command context가 없으면 `commandContextAvailable=false`와 기존 unavailable 표현을
사용한다. 새 request/correlation ID, fake operation ID 또는 `0` identity를 만들어
연결하지 않는다.

Elapsed client tick과 Task-owned counter는 서로 다른 field로 같은 capture에서
기록한다.

```text
clientTickId
operationStartClientTickId
candidateStartClientTickId
elapsedOperationClientTicks
elapsedCandidateClientTicks

operationTicks
candidateTicks
maxOperationTicks
maxCandidateTicks
```

계산 의미는 다음처럼 고정한다.

```text
elapsedOperationClientTicks
    = clientTickId - operationStartClientTickId

elapsedCandidateClientTicks
    = clientTickId - candidateStartClientTickId

operationTicks
    = StoreHomeTask가 보유한 current operation tick counter의 read-only snapshot

candidateTicks
    = StoreHomeCandidateAttempt가 보유한 current candidate tick counter의
      read-only snapshot
```

Client tick delta로 `operationTicks` 또는 `candidateTicks`를 대신 계산하지 않는다.
Task-owned counter로 wall-clock 시간을 만들지도 않는다. Field capture는 같은 client
tick의 timeout comparison 직전에 수행하되 counter 증가 순서, comparison 순서 또는
branch 결과를 바꾸지 않는다.

`childTaskRunOrdinal`은 operation-local monotonic diagnostic ordinal이다.
`childTaskRunId`는 `operationId + candidateAttemptOrdinal + childTaskRunOrdinal`의 stable
semantic tuple에서 파생하며 JVM identity hash나 opaque object 문자열을 사용하지 않는다.
두 값 모두 Task equality 또는 scheduling에 사용하지 않는다.

### 7.2 Candidate start

후보마다 `STORE_HOME_CANDIDATE_ATTEMPT_STARTED`를 정확히 한 번 emit한다. 7.1의 공통
field와 함께 최소한 다음을 기록한다.

```text
playerStartPosition
distanceMetricVersion=block_pos_3d_squared_v1
initialDistanceSquared3d
childTaskClass / childTaskRunId
baritonePathingActive
baritoneCalculationActive
normalizedGoalType / normalizedGoalTarget
```

`block_pos_3d_squared_v1`은 player와 candidate의 integer block position에 대해
`dx*dx + dy*dy + dz*dz`를 계산한다. Square root를 적용하지 않는다. 다른 거리 정의가
필요하면 같은 field의 의미를 바꾸지 않고 새 metric version과 새 field를 사용한다.

### 7.3 Bounded progress summary

매 tick emit하지 않는다. Candidate당 sampled progress/detail은 최대 8회이고,
operation 전체의 non-terminal sampled detail hard cap은 64회다. 기존 framework 또는
configuration이 더 엄격하면 더 엄격한 cap을 사용한다.

같은 candidate의 detail은 기본 200 client tick보다 자주 emit하지 않는다. Exact binding,
handler 또는 normalized path/goal state의 첫 meaningful transition처럼 놓치면 안 되는
one-time state change는 coarse interval과 분리할 수 있지만 candidate당 8회와 operation당
64회 detail hard cap은 넘지 않는다.

```text
player block position 변화
bestDistanceSquared3d의 coarse bucket 개선
Baritone pathing/calculation normalized state transition
normalized goal type/target transition
exact binding 또는 supported handler의 첫 state transition
긴 경과 boundary
```

Progress summary는 7.1의 clock/counter field와 함께 다음 bounded state를 기록한다.

```text
playerStartPosition / playerCurrentPosition
distanceMetricVersion=block_pos_3d_squared_v1
initialDistanceSquared3d
currentDistanceSquared3d
bestDistanceSquared3d

lastPlayerMoveClientTickId
lastBestDistanceImprovementClientTickId
ticksSincePlayerMove
ticksSinceBestDistanceImprovement
progressKind

childTaskClass / childTaskRunId
baritonePathingActive / baritoneCalculationActive
normalizedGoalType / normalizedGoalTarget
screenClass / handlerClass / syncId
exactBindingMatched / exactBindingEverObserved
supportedHandlerEverObserved
containerSessionOrdinal / manifestActive / pendingTransfer
pathingExceptionCount
suppressedProgressSummaryCount
```

`progressKind`는 최소 다음 normalized value만 사용한다.

```text
PLAYER_MOVED
BEST_DISTANCE_IMPROVED
PATHING_STATE_CHANGED
GOAL_STATE_CHANGED
BINDING_STATE_CHANGED
NO_NEW_PROGRESS
```

이 값들은 diagnostics-only bookkeeping이다. Timeout, candidate selection, Task return,
retry, exact binding 또는 Baritone 결정을 읽거나 바꾸는 입력으로 사용하지 않는다.
정상 우회 경로는 잠시 target에서 멀어질 수 있으므로 distance 개선만으로 future behavior
progress를 정의하지 않는다.

### 7.4 Timeout decision과 candidate result boundary

Timeout condition을 계산한 직후, candidate reject 또는 operation terminal branch가
상태를 바꾸기 직전에 decision event를 정확히 한 번 emit한다.

```text
STORE_HOME_CANDIDATE_TIMEOUT_DECISION
STORE_HOME_OPERATION_TIMEOUT_DECISION
```

Decision event는 7.1과 7.3의 latest bounded state에 더해 다음 exact comparison을
기록한다.

```text
timeoutScope=CANDIDATE|OPERATION
decisionClientTickId
candidateTicksBeforeDecision
operationTicksBeforeDecision
maxCandidateTicks
maxOperationTicks

candidateTimeoutEvaluated
candidateTimeoutConditionMatched
operationTimeoutEvaluated
operationTimeoutConditionMatched
timeoutComparisonOperator=GREATER_THAN_OR_EQUAL
timeoutEvaluationOrder=OPERATION_THEN_CANDIDATE
capturedBeforeDecisionSideEffects=true

pendingTransfer
plannedTimeoutAction=REJECT_CANDIDATE|FINISH_TRANSFER_UNCONFIRMED|FINISH_EXHAUSTED
plannedRejectionReason / plannedTerminalReason
diagnosticCaptureStatus / diagnosticErrorClass
```

Operation timeout branch가 먼저 일어나 candidate condition을 평가하지 않았다면
`candidateTimeoutEvaluated=false`로 기록한다. 평가하지 않은 값을 false result로
위장하지 않는다. Candidate timeout boundary에서도 같은 원칙으로 각 condition의
evaluated/matched를 분리한다.

`operationTicksBeforeDecision`과 `candidateTicksBeforeDecision`은 해당 `onTick()`에서
기존 counter increment가 끝난 뒤 기존 `>=` comparison과 branch side effect 직전에
잡은 Task-owned 값이다. `*ConditionMatched`는 log emission 뒤 새로 재계산하지 않고
기존 branch가 평가한 boolean을 그대로 기록한다. 해당 timeout scope에서 값이
존재하지 않거나 평가되지 않았으면 fake `0` 대신 evaluated flag와 `unavailable`을
사용한다.

Candidate가 실제로 제외되면 `STORE_HOME_CANDIDATE_REJECTED`, exact trusted-container
session activation까지 성공하면 `STORE_HOME_CANDIDATE_ACTIVATED`를 각각 해당 boundary에서
한 번 emit한다. Operation 종료에는 기존 typed terminal을 바꾸지 않는
`STORE_HOME_OPERATION_TERMINAL_SUMMARY`를 한 번 연결한다.

```text
candidateAttemptOrdinal / candidateQueueRemaining
destinationId / candidatePosition
rejectionReason 또는 activationResult
exactBindingMatched
screenClass / handlerClass / syncId
containerSessionOrdinal / manifestRevision / manifestActive
pendingTransfer
operationResult / terminalReason when terminal
```

Timeout decision과 rejection/activation/terminal은 progress sampling으로 대체하지 않는다.
Decision event emit 실패도 원래 branch 실행을 막거나 결과를 바꾸면 안 된다.

### 7.5 Cap, reserved budget와 fingerprint

다음 emission은 sampled progress/detail cap과 별도의 reserved boundary budget을 사용한다.

```text
CANDIDATE_ATTEMPT_STARTED
CANDIDATE_TIMEOUT_DECISION
OPERATION_TIMEOUT_DECISION
CANDIDATE_REJECTED 또는 CANDIDATE_ACTIVATED
OPERATION_TERMINAL_SUMMARY
first unique EXCEPTION boundary
DIAGNOSTIC_SESSION_CAP_REACHED
```

Candidate당 progress/detail 최대 8회와 operation당 non-terminal sampled detail 최대
64회가 먼저 소진돼도 위 boundary를 누락하지 않는다. 기존 더 엄격한 값이 없을 때
per-operation detailed-event hard cap은 256이고, 그중 lifecycle decision, terminal,
first unique exception과 cap-reached summary를 위한 reserved boundary budget은 최소
32다. Progress/detail은 이 reserved 32를 소비하지 않는다.

Existing session/correlation hard cap에도 terminal, first unique exception과 cap-reached
summary를 위한 reserved slot을 둔다. Session hard cap에 도달하면
`DIAGNOSTIC_SESSION_CAP_REACHED`를 session당 정확히 한 번 emit하고 다음을 bounded
payload로 남긴다.

```text
operationId
capScope
configuredCap
emittedEventCount
suppressedEventCount
suppressedEventCountByFamily
terminalReservationAvailable
exceptionReservationAvailable
```

후보 수가 매우 많아 reserved boundary도 per-operation hard cap에 도달하면 silently
drop하지 않는다. Cap-reached와 terminal summary에 first/last observed candidate,
total candidate boundary count, emitted count와 family별 suppressed count를 bounded
aggregate로 남긴다. 이 aggregate도 full candidate list, raw path 또는 scanner dump를
포함하지 않는다.

Cap 도달은 추가 diagnostic detail만 억제한다. 다음 behavior를 유발하거나 변경하지
않는다.

```text
Task 종료 또는 완료
candidate 제외 또는 activation
timeout, retry 또는 fallback
Baritone cancel, goal/path/input 변경
terminal result/reason 변경
exception suppression 또는 변환
```

Event fingerprint는 다음 stable semantic field만 사용한다.

```text
event family
operationId
candidateAttemptOrdinal
destinationId
phase
childTaskClass
normalized pathing/calculation state
normalized goal type/target
exact binding state
timeout/rejection reason
coarse distance bucket
```

다음 dynamic 또는 opaque value는 fingerprint에 넣지 않는다. 필요한 값은 bounded
payload에만 둔다.

```text
clientTickId 또는 wall-clock timestamp
exact elapsed tick
exact player position 또는 exact distance
System.identityHashCode
opaque Object.toString
raw path, node list 또는 full scanner dump
```

반복 exception stack을 timeout payload에 넣지 않는다. 첫 unique exception의 full stack은
기존 error/crash channel에 보존하고 이후 같은 signature는 dedupe/count한다.

### 7.6 Run/artifact identity manifest

Historical incident JAR과 current dirty source를 다시 혼동하지 않도록 operation event는
immutable `runManifestId`만 참조하고, full identity는 run당 한 번의 evidence manifest에
기록한다.

```text
runId / runManifestId
operationId / commandRequestId / commandCorrelationId
repositoryHead
relevantSourceTreeIdentity
worktreeDirty
dirtyDiffSha256 또는 buildInputManifestSha256
builtJarPath / builtJarSize / builtJarSha256
deployedJarPath / deployedJarSize / deployedJarSha256
runtimeCodeSourcePath / runtimeCodeSourceIdentity / runtimeJarSha256
javaVendor / javaVersion / javaRuntimePath
minecraftVersion / fabricLoaderVersion / chatClefVersion
diagnosticsMode
boundedLoggingLimits
worldSnapshotId / inventorySnapshotId
runStartedAt / runEndedAt
```

확인하지 못한 identity는 `UNVERIFIED`로 남기고 current source에서 추정하지 않는다.
Relevant untracked build input이 하나라도 있으면 `dirtyDiffSha256`만으로 complete identity를
주장하지 않는다. 이 경우 `buildInputManifestSha256`은 `repositoryHead`, normalized
staged/unstaged patch와 path순으로 정렬한 relevant untracked file의 `path + SHA-256`을
포함하는 complete build-input manifest에서 계산한다.
Hashing, diff 수집 또는 filesystem scan을 per-tick Task path에서 수행하지 않는다. Build,
deploy와 launch evidence가 만든 immutable manifest를 재사용하며 operation event마다 full
manifest를 반복하지 않는다.

Built, deployed와 runtime code-source identity 중 하나라도 불일치하거나 확인할 수 없으면
current source의 exact hunk가 runtime behavior를 만들었다는 cross-artifact 결론은
중단하고 `PARITY_UNPROVEN`으로 유지한다. 다만 같은 runtime operation이 직접 출력한
Task-owned counter, timeout decision, 실제 rejection과 terminal event는 그 실행의 observed
behavior 증거로 사용할 수 있다. Source parity가 없다는 이유로 runtime event를 지우거나,
반대로 runtime event만으로 current dirty source의 complete identity를 주장하지 않는다.
이 manifest 계약은 build, deploy, runtime 실행 또는 외부 파일 수정을 승인하지 않는다.

### 7.7 Diagnostics behavior-preservation gate

보강이 diagnostics-only로 유지되려면 다음을 모두 보존해야 한다.

```text
MAX_CANDIDATE_TICKS / MAX_OPERATION_TICKS 값과 증가 순서
Task-owned counter 값과 timeout comparison/evaluation 순서
Task return과 parent/child selection
candidate ordering, rejection과 terminal result
exact binding과 container activation
retry/fallback
pending transfer와 cursor 처리
Baritone input, goal, path와 cancellation
exception propagation과 cleanup
wire protocol과 typed terminal
```

Observed engine behavior 주위에 새 `try/catch`를 두지 않는다. Diagnostic formatter는
자기 bounded formatting failure만 격리할 수 있고 원래 timeout, Task, Baritone 또는
container exception을 consume하거나 변환하지 않는다.

Diagnostics mode `OFF`에서는 investigation-only event가 없어야 한다. `BOUNDARY`는 위
경계와 bounded summary만 emit하고 unchanged-state tick polling을 하지 않는다.
`VERBOSE`도 payload, rate, dedupe, per-candidate, per-operation과 session cap을 우회하지
않는다.

## 8. Reviewed future behavior direction, implementation not approved

Direct runtime evidence 뒤의 최종 방향 검수 결과는 `PASS`다. 기본 해법은 `2400`을 큰
상수로 바꾸거나 progress 때마다 단일 candidate lifetime을 무조건 초기화하는 것이
아니다. 장거리 navigation과 local interaction을 phase-scoped timeout으로 분리한다.

```text
NAVIGATE_TO_CANDIDATE
    purpose:
        trusted candidate의 interaction neighborhood까지 이동
    timeout meaning:
        candidate 총 수명이 아니라 의미 있는 progress가 없었던 연속 시간
    keep candidate when:
        bounded jitter threshold를 넘는 recent actual player movement
        OR bounded distance epsilon을 넘는 recent best-distance improvement
    reject when:
        player movement와 best-distance improvement가 모두 bounded window 동안 없음
    candidate rejection reason:
        candidate_navigation_no_progress

OPEN_AND_BIND_CANDIDATE
    entry:
        interaction 가능한 local neighborhood 진입 또는 long-distance path 종료 뒤
        실제 local open/binding 시도 시작
    timeout meaning:
        right click, screen open, supported handler와 exact binding을 위한 bounded local budget
    candidate rejection reason:
        candidate_local_interaction_timeout 또는 기존 exact-binding rejection reason

operation 전체
    normal active progress:
        absolute 12000-tick lifetime만으로 종료하지 않음
    bounded failure:
        operation-wide no-progress 경계, reason=operation_no_progress
    final safety:
        충분히 큰 별도 emergency hard cap, reason=operation_emergency_hard_cap
        candidate rejection이 아니라 operation-level terminal
```

`baritonePathingActive`, `goal active` 또는 `calculation active` 하나만으로 progress를
인정하지 않는다. 이 flag는 stale하게 남을 수 있다. 반대로 straight-line distance만
사용하면 장애물을 우회하며 잠시 target에서 멀어지는 정상 이동을 stall로 오판할 수
있다. 따라서 recent actual player movement와 recent best-distance improvement를 함께
보고 path/goal/calculation state는 보조 context로만 사용한다.

Timeout clock과 reset 의미는 숫자와 별개로 다음처럼 고정한다.

```text
clock basis
    candidate navigation no-progress clock
    local interaction clock
    operation no-progress clock
    operation emergency hard-cap elapsed
        -> StoreHomeTask가 실제 active root로 해당 phase에서 onTick된 tick만 소비
        -> safety-chain 선점 또는 root 미실행 tick은 소비하지 않음
        -> resume 뒤 같은 attempt와 누적 clock을 계속 사용

semantic progress
    bounded jitter threshold를 넘는 actual player movement
    OR bounded distance epsilon을 넘는 best-distance improvement

progress가 아닌 것
    path/goal active flag 자체
    path calculation/recalculation/re-adoption 자체
    goal resubmission 자체
    candidate 선택 또는 switch 자체
    timestamp 변화
    sub-threshold position jitter 또는 epsilon 미만 distance 변화

candidate switch
    새 candidate-attempt-local state와 navigation no-progress clock 시작
    local interaction clock은 UNSTARTED로 두고 handoff 때 정확히 한 번 시작
    operation no-progress와 emergency hard-cap elapsed reset 금지
    confirmed metrics와 operation failure history reset 금지

OPEN_AND_BIND_CANDIDATE handoff
    같은 candidate attempt에서 local interaction clock 시작은 정확히 한 번
    interaction-neighborhood 경계 진동으로 reset 금지
    screen/handler/binding flicker 또는 반복 click으로 reset 금지
    path/goal update로 reset 금지
    navigation으로 돌아가야 하면 누적 local elapsed를 보존하거나
        명시적인 새 candidate-attempt generation을 만들어 silent restart 금지

pending precedence
    candidate 또는 operation timeout 때 pending 결과가 불확실하면
        기존 TRANSFER_UNCONFIRMED가 우선
    다음 candidate, replan 또는 추가 click 금지
```

Stable reason은 위 네 lower-snake value로 보존한다. 새 `StoreHomeResult` enum은 필수가
아니지만 phase별 candidate rejection과 operation terminal reason을 일반
`candidate_timeout`/`operation_timeout` 하나로 다시 합치지 않는다. Emergency hard cap은
현재 candidate가 unusable하다는 증거가 아니며 다음 candidate로 진행시키지 않는다.
이 네 reason은 future behavior 계약이다. §1~§7에 기록한 실제 재현 로그의
`candidate_timeout`은 historical evidence이므로 이름을 바꾸지 않는다.

Progress window, interaction neighborhood predicate, local interaction budget,
operation no-progress window, emergency hard cap, movement jitter threshold와 distance
epsilon 숫자는 이번 evidence와 검수만으로 확정하지 않는다. 값은 focused deterministic
test와 별도 behavior 승인 전에 명시해야 한다. Active-root clock basis, semantic reset,
candidate-switch reset 범위, sticky local handoff, stable reason과 pending precedence는
숫자 선택이 아니라 구현 전 필수 계약이다. Diagnostic `candidateTicks`와
`operationTicks`는 관찰값으로 유지할 수 있지만 navigation 중 absolute lifetime
decision과 같은 의미로 계속 사용하지 않는다.

이 방향에서도 ownership은 다음처럼 유지한다.

```text
StoreHomeTask
    -> phase transition, operation/candidate timeout decision, terminal과 exact binding success 소유

StoreHomeCandidateAttempt 또는 작은 LAVI-owned collaborator
    -> candidate-local progress snapshot, last progress tick과 local phase tick 소유

InteractWithBlockTask
    -> 기존 generic approach/look/click behavior 유지

Baritone / TaskRunner / UserTaskChain / PlayerInteractionFixChain
    -> STORE_HOME 전용 timeout 정책을 소유하지 않음
```

새 `StoreHomeResult` enum은 필수가 아니다. 기존 typed result, 누적 failure와 operation
terminal precedence를 유지하면서 candidate/operation stable reason만 phase별로 구분한다.

`GetToXZTask`를 먼저 실행한 뒤 interaction child로 교체하는 workaround는 기본 방향이
아니다. XZ-only goal은 Y, 상호작용 면과 exact binding을 모르므로 기존
`InteractWithBlockTask`의 책임을 대체하지 못한다. 문제의 verified owner는 이동기 자체가
아니라 active child를 고정 lifetime으로 자른 parent `StoreHomeTask`다.

다음은 모두 behavior change이며 이번 문서가 구현을 승인하지 않는다.

```text
2400 또는 12000 상향, 제거 또는 decision 의미 변경
distance-scaled timeout
progress-aware no-progress timer
local interaction timeout 분리
operation no-progress와 emergency hard cap 분리
candidate rejection reason 변경
candidate ordering/retry/fallback 변경
InteractWithBlockTask 완료 조건 변경
Baritone goal/path/input/cancel 변경
UserBlockRangeTracker behavior fix
```

## 9. Verification result와 later behavior contract

### 9.1 Diagnostics-only verification result

Diagnostics implementation은 static review 뒤 canonical clean forced build와 통제 runtime
재현을 통과했다. 관찰 또는 검토된 범위와 아직 미실행인 branch를 구분한다.

| Scenario | Result |
| --- | --- |
| candidate start | PASS; 세 candidate마다 start event 1 |
| client clock 대 Task counter | PASS; elapsed client fields와 Task-owned counter 별도 기록 |
| candidate timeout | PASS; side effect 전 decision 1과 실제 reject outcome 1이 candidate마다 연결됨 |
| operation timeout state | PASS for observed non-match; candidate decision 세 번 모두 evaluated=true, matched=false |
| long movement | PASS; per-tick file emission 없이 candidate progress cap 8, operation progress 24/64 |
| operation detail cap | PASS for observed run; accepted 35/256, reserved terminal 유지 |
| suppression | PASS; progress 7,173건 억제 요약, terminal/decision 손실 0 |
| fingerprint/static behavior-preservation | PASS in source audit and clean build |
| deployed artifact | PASS at file level; build/deployed JAR size와 SHA-256 일치 |
| runtime loading | PASS; expected runtime code source, Mixin/FATAL/crash evidence 0 |
| manifest complete source parity | `PARITY_UNPROVEN`; repository/source/build-input identity 미공급 |
| diagnostics OFF, cap reached, GUI activation, pending timeout, capture failure | 이번 통제 회차에서 미실행; 기존 계약 유지 |

Build와 runtime 검증은
[Fabric ChatClef 1.20.1 Build Verification](chatclef-fabric-build-verification.md)의 clean
forced build와 active instance 확인 절차를 따랐다. 파일-level 동일성은 확인됐지만 runtime
manifest가 완전한 source identity를 갖지 못한 사실을 지우지 않는다.

### 9.2 Later behavior regression contract

다음 matrix는 현재 구현된 phase-scoped timeout behavior와 focused JUnit의 regression
contract다. §12의 확정 window, threshold, epsilon과 stable reason을 expected value로 쓴다.
다만 기존 lower-level timeout lifecycle/checkpoint test는
`StoreHomeTaskLifecycleController`의 production call order 자체를 직접 실행해 고정하지
않는다. 그 direct coverage와 shared-object identity gate는 §12.1에 별도로 남긴다.

| Scenario | Expected behavior |
| --- | --- |
| candidate counter가 기존 2400을 넘었지만 recent actual progress 존재 | candidate 유지; absolute lifetime rejection 0 |
| safety-chain이 어떤 timeout window보다 오래 선점 | 모든 StoreHome-owned clock 미산입; resume 뒤 같은 attempt와 누적 clock 계속 |
| path/goal active 또는 recalculation/re-adoption/goal resubmission만 있고 semantic progress 없음 | progress reset 0; bounded window 뒤 `candidate_navigation_no_progress`로 candidate 제외 |
| position jitter와 distance 변화가 각각 threshold/epsilon 미만 | progress reset 0 |
| 정상 우회로 direct distance가 잠시 증가하지만 player movement 존재 | 즉시 timeout 0; navigation 계속 |
| actual progress 없이 candidate switch만 발생 | 새 candidate navigation state 초기화, local clock은 UNSTARTED; operation no-progress/emergency elapsed와 confirmed metrics/failure history reset 0 |
| interaction neighborhood 진입 | navigation no-progress clock과 분리된 local interaction clock을 해당 candidate attempt에서 정확히 한 번 시작 |
| neighborhood 경계 진동, screen/handler/binding flicker, 반복 click 또는 path/goal update | 같은 attempt의 local elapsed silent reset 0; navigation 복귀 시 누적 elapsed 보존 또는 명시적 새 attempt generation |
| local phase에서 screen/binding이 bounded budget 동안 없음 | `candidate_local_interaction_timeout`으로 candidate 제외 |
| operation counter가 기존 12000을 넘지만 active long-distance progress 존재 | 기존 absolute operation timeout만으로 terminal 0 |
| operation-wide semantic progress가 bounded window 동안 없음 | `operation_no_progress`; 누적 결과에 따른 기존 terminal precedence 유지 |
| emergency hard cap 도달 | `operation_emergency_hard_cap` operation-level terminal; candidate rejection/unusable evidence 0 |
| candidate 또는 operation timeout 시 pending 결과가 불확실 | `TRANSFER_UNCONFIRMED` 우선; 다음 candidate/replan/추가 click 0 |
| phase별 네 timeout 경로 | `candidate_navigation_no_progress`, `candidate_local_interaction_timeout`, `operation_no_progress`, `operation_emergency_hard_cap`을 서로 다른 stable reason으로 보존 |
| 모든 candidate가 실제 navigation no-progress 또는 local interaction 실패 | 기존 `NO_USABLE_TRUSTED_DESTINATION` terminal 의미와 precedence 유지 |
| terminal 뒤 lifecycle | `IdleTask` 전환 정확히 한 번; duplicate terminal/submission 0 |

추가로 pending transfer, cursor, trust loss, world/dimension change, exact binding, confirmed
delta와 interruption 계약은 기존 focused test matrix를 그대로 통과해야 한다. Behavior
patch가 `InteractWithBlockTask`, Baritone goal/path/input, candidate ordering, typed result 또는
IdleTask lifecycle을 바꾸지 않았는지는 diff/static gate로 확인한다.

## 10. 닫힌 질문과 남은 미확정 사항

통제 재현으로 다음 질문은 닫혔다.

```text
candidate timeout 직전 위치와 best progress: 직접 관찰됨
continuous progress 대 stall: 세 candidate 모두 recent progress 관찰됨
Task-owned counter와 timeout comparison: candidate 2400에서 direct match
operation timeout: observed run에서 false
interaction neighborhood 진입: 도달하지 못함
screen/handler/exact binding/container session: 관찰되지 않음
terminal 뒤 IdleTask: 정상 lifecycle
```

후속 direction review로 다음 behavior 계약도 문서 수준에서 닫혔다.

```text
clock basis: StoreHomeTask가 실제 active root로 onTick된 tick만 소비
progress reset: jitter threshold를 넘는 actual movement 또는 epsilon을 넘는 best-distance improvement만 허용
candidate switch: operation 누계와 no-progress/emergency state를 reset하지 않음
local handoff: candidate attempt당 정확히 한 번 시작하며 silent reset 금지
stable timeout reasons: candidate_navigation_no_progress, candidate_local_interaction_timeout,
                        operation_no_progress, operation_emergency_hard_cap
pending uncertainty: TRANSFER_UNCONFIRMED 우선
```

2026-08-29 phase-scoped artifact runtime은 다음 경계도 닫았다.

```text
screen open / exact trusted binding: observed
trusted-container session activation: observed
capacity rejection followed by next candidate: observed
paired-delta transfer and terminal COMPLETED: observed twice
old 2,400 absolute candidate lifetime after active progress: not observed
```

다음은 여전히 미확정이다.

```text
세 @goto와 STORE_HOME의 시작 위치 및 world state가 동일했는지
stale Baritone disk cache가 도착 시간에 관여했는지
UserBlockRangeTracker NPE가 실제 도착 시간을 얼마나 늘렸는지
incident와 current dirty worktree의 complete source/build-input parity
실제 장시간 safety-chain 선점과 네 timeout reason 발동의 runtime matrix
pending timeout에서 추가 container click 0회 runtime evidence
```

World copy, restore 또는 replacement 이력이 있으면 behavior fix 전에
[ChatClef / Baritone Cache Troubleshooting](chatclef-baritone-cache-troubleshooting.md)에
따라 exact world cache 상태를 별도 통제한다. 현재 evidence만으로 stale cache를
원인 또는 무관한 요소로 확정하지 않는다.

## 11. Approval gate

이번 investigation에서 diagnostics-only source, clean forced build, matching JAR 배포,
Minecraft 통제 재현과 runtime log/crash review는 각각 당시의 별도 사용자 요청에 따라
완료됐다. 이 이력은 후속 작업에 대한 standing authorization이 아니다.

이번 문서화 이후에도 다음은 각각 별도 승인이다.

```text
phase-scoped timeout 숫자, movement jitter threshold, best-distance epsilon과 exact predicate 확정
focused behavior test source edit
timeout behavior source edit
behavior test 또는 Gradle 실행
behavior JAR 배포와 Minecraft runtime reproduction
UserBlockRangeTracker 또는 다른 upstream-derived source edit
commit
push
```

2026-08-28 후속 사용자 승인으로 위 목록 중 phase-scoped 숫자 확정, focused test
source 작성과 timeout behavior source 수정은 수행됐다. Test/Gradle 실행, clean forced
build, JAR 배포, Minecraft runtime reproduction, commit과 push는 이번 승인에 포함되지
않았고 계속 별도 gate로 남는다.

2026-08-29 별도 사용자 승인으로 behavior test, clean forced build, active instance JAR
배포, Minecraft runtime 재현과 로그 검토를 수행했다. 이 후속 이력도 commit, push,
upstream-derived source 변경 또는 추가 runtime matrix에 대한 standing authorization은
아니다.

Timeout diagnostics는 LAVI-owned `StoreHomeTask`/candidate 경계에서 우선 관찰한다.
Generic `InteractWithBlockTask`, TaskRunner, UserTaskChain,
`PlayerInteractionFixChain` 또는 Baritone engine-wide behavior를 건드려야만 관찰할 수
있는 상황이면 자동 확대하지 않고 정확한 미관찰 경계와 필요한 범위를 먼저 보고한다.

## 12. Phase-scoped timeout source implementation status

2026-08-28 후속 behavior 승인에 따라 현재 dirty worktree에 다음 source를 구현했다.

```text
candidate navigation no-progress: 2,400 StoreHome active-root ticks
candidate local interaction:       2,400 StoreHome active-root ticks
operation no-progress:            12,000 StoreHome active-root ticks
operation emergency hard cap:    120,000 StoreHome active-root ticks
movement jitter threshold:           0.5 block
best-distance epsilon:                1.0 block
behavior progress distance metric: player Vec3d to candidate block center,
                                   3D Euclidean blocks
local handoff:
    exact trusted binding
    OR existing InteractWithBlockTask.getCurrentReach().isPresent()
```

두 progress predicate는 모두 strict comparison이다. 즉 정확히 `0.5` block 이동 또는
정확히 `1.0` block best-distance 개선은 reset하지 않고, 각각 그 값을 초과해야 한다.

Behavior state는 `lavi.minecraft.task.container.home.execution.timeout/**` 아래의
policy, navigation progress, candidate phase clock, operation clock, decision resolver와
immutable diagnostics observation으로 분리했다. `StoreHomeCandidateAttempt`는 기존
`InteractWithBlockTask` child lifecycle만, `StoreHomeOperationProgress`는 confirmed
transfer/failure 누계만 소유한다.

`execution/state/StoreHomeExecutionState`는 mutable state bag이 아니라 focused owner를
조합하는 composition root다. 실제 상태는 `state/lifecycle`, `state/operation`,
`state/context`, `state/candidate`, `state/session`, `state/reporting`에 각각 분리했다.
Root `StoreHomeTask`는 stable Task facade/API만 유지한다. Top-level operation lifecycle
순서는 `StoreHomeTaskLifecycleController`, terminal commit/report 순서는
`StoreHomeOperationTerminator`가 소유한다. Candidate validation, container activation,
manifest validation, confirmed commit, transfer execution과 timeout state/decision도 각각
기존 또는 새 focused collaborator가 소유한다. Timeout diagnostics도 event orchestration,
operation/candidate
bookkeeping, snapshot, emission budget, boundary enablement와 diagnostic-only bookkeeping
failure containment를 별도 `diagnostics/.../timeout/**` component로 나눴다. Boundary
action의 Minecraft/Baritone read 예외는 diagnostics가 catch하거나 suppress하지 않는다.

2026-08-29 후속 구조 승인으로 root에 남아 있던 독립 workflow stage도 다음처럼
folder/package 경계로 추출했다. 추가 감사에서 dependency assembly, Task lifecycle과
read-only result projection이 root에 함께 남은 것을 확인해 `execution/task/**`로 한 번 더
분리했다. `StoreHomeTask`는 기존 FQN과 Task override/public API만 유지하는 약 150줄의 thin
facade이며, 별도의 parallel Task engine이나 lifecycle subclass는 만들지 않았다.

```text
execution/initialization
    StoreHomeRequestInitializer          request acceptance/preflight/catalog
execution/context
    HomeStorageCursorStateReader         cursor 상태 read
execution/candidate/selection
    StoreHomeCandidateAttemptStarter     후보 선택과 attempt 시작
execution/candidate/navigation
    StoreHomeCandidateNavigationStep     기존 child Task 이동과 exact activation 경계
execution/candidate/rejection
    StoreHomeCandidateRejector           후보 실패 누계·queue rejection·candidate cleanup
execution/candidate/view
    StoreHomeCandidateRuntimeView        diagnostics용 read-only candidate view
execution/session/activation
    StoreHomeSessionActivator            exact trusted container session 생성
execution/session/flow
    StoreHomeSessionStep                 session validation과 transfer step 순서
execution/session/transfer
    StoreHomeTransferResultHandler       transfer result의 state commit/terminal mapping
execution/operation/pending
    StoreHomePendingOwnershipGuard       executor/session pending ownership 비교
execution/operation/reporting
    StoreHomeReportingPlanCapture        terminal reporting plan capture
execution/operation/terminal
    StoreHomeOperationTerminator         terminal commit/report/diagnostic 순서
execution/timeout/candidate
    StoreHomeCandidateTimeoutObserver    candidate progress/local-handoff 관찰
execution/timeout/decision
    StoreHomeTimeoutDecisionApplier      이미 계산된 timeout decision의 side effect 적용
execution/task/composition
    StoreHomeTaskDependencies            public constructor dependency 계약
    StoreHomeTaskAssembly                동일 state/clock 기반 object graph 조립
execution/task/lifecycle
    StoreHomeTaskLifecycleController     active-root start/tick/stop 판정 순서
execution/task/view
    StoreHomeTaskView                    live state의 debug/public result 투영
```

기존 두 공개 constructor, `result/phase/plan/outcome`, reflection test가 사용하는
`state`와 `timeoutLifecycle`, `onStart/onTick/onStop` 경계는 유지했다. 후보 거절 전
diagnostic snapshot, exact activation commit, session→operation confirmed-transfer commit,
pending 우선순위와 네 timeout reason의 처리 순서도 바꾸지 않았다.

직접 영향을 받는 LAVI-owned transfer 경계도 함께 분리했다. `execution/slot`은 행동용
slot view/inspection 계약, `execution/transfer`는 transfer status/result와 neutral pending
observation을 소유한다. `diagnostics/container/home/{slot,fingerprint,transfer}`는 각각
live slot snapshot, fingerprint metadata read, pending 변환과 stale snapshot 조립만
소유한다. `HomeStorageTransferExecutor`는 한 번의 exact transfer 순서만 조정하고,
live container/capacity 계산은 `execution/transfer/container`, click readiness/발행은
`execution/transfer/click`, pending lifecycle/검증은 `execution/transfer/pending`, paired
delta 값 계약은 `execution/transfer/delta`, 진단과 무관한 실패 관찰 값은
`execution/transfer/failure`가 각각 소유한다. Executor에서 diagnostics import와 직접
호출을 제거했으며, Task가 같은 tick에 neutral failure observation을 diagnostics stale
snapshot으로 조립한다. 기존 status/reason/click 순서는 바꾸지 않았다.

`StoreHomeTask`는 같은 child를 계속 반환하며 Baritone goal/path/input을 새로 소유하지
않는다. Candidate 전환은 candidate-local state만 새로 만들고 operation no-progress,
emergency elapsed, confirmed 결과와 failure history를 유지한다. Exact activation 또는
paired-delta confirmed commit만 operation progress를 추가로 reset하며 click request,
path/goal 상태, candidate switch와 local flicker는 reset하지 않는다.

판정 순서는 다음처럼 고정했다. Active-root tick 뒤 pending ownership, context와 cursor
guard를 확인하고, emergency hard cap은 후보 선택이나 child behavior 전에 항상 먼저
판정한다. Operation no-progress가 이미 일치하고 active candidate가 없거나 container
session/pending transfer가 있으면 새 후보 선택 또는 새 click 전에 판정한다. 반면 active
candidate가 있고 session/pending이 없는 좁은 navigation/open-binding 경계에서는 같은
tick의 현재 위치 또는 exact activation만 먼저 관찰할 수 있고, 그 semantic progress
reset 뒤 operation no-progress를 재판정한다. 이 경계에서는 full navigation/session step을
실행하지 않으므로 후보 거절, capacity 처리, queue mutation과 stopped open-child 재생성이
선행하지 않는다. 정상 navigation/session step이 허용된 경로에서는 그 step 뒤 operation
no-progress, 그 뒤 candidate timeout을 판정한다. 따라서 capacity, terminal 또는 typed
projection이 모든 timeout보다 항상 우선한다는 blanket contract는 아니다.

Stable behavior reason은 다음 네 값을 사용한다.

```text
candidate_navigation_no_progress
candidate_local_interaction_timeout
operation_no_progress
operation_emergency_hard_cap
```

Candidate reason은 해당 rejection history에 남고, operation reason은 기존 typed
`StoreHomeOutcome.reason` 경로로 전달된다. 모든 candidate 소진 시의 기존 terminal
reason `trusted_candidates_exhausted`와 typed result precedence는 변경하지 않았다.

Focused JUnit source는 boundary, 장거리 progress, 누적 movement, jitter, epsilon,
detour, sticky local handoff, activation boundary, candidate switch, safety pause,
operation no-progress, emergency hard cap, pending precedence와 네 reason을 고정한다.
Operation checkpoint test는 이 순서 predicate와 clock lifecycle을 분리해 고정하며,
`StoreHomeTask.onStop(null) -> onStart()`의 timeout snapshot 보존도 확인한다.

이 component-level coverage는 production controller의 전체 branch/call order, root facade와
controller/view/timeout collaborator가 공유하는 exact state/clock identity, 또는 submitted
root에서 termination observation과 typed projector까지 이어지는 Task identity를 직접
증명하지 않는다. 243-test clean-build 결과는 이 새 direct gate가 이미 통과했다는 의미가
아니다.

현재 분리된 증거 상태는
`CLEAN_BUILD_JUNIT_VERIFIED_DEPLOY_FILE_IDENTITY_VERIFIED_RUNTIME_BEHAVIOR_OBSERVED`다.

```text
clean command: .\gradlew.bat clean build --rerun-tasks
build result: BUILD SUCCESSFUL in 2m 45s
tasks: 171 actionable tasks, 171 executed
1.20.1 JUnit XML: 243 tests, 0 failures, 0 errors, 14 skipped

built/deployed JAR: chatclef-1.20.1-0.18.23.jar
size: 7,113,261 bytes
SHA-256: C939E5A859BB66535FADCB5522A4D9CD99AF2CAC7E90B6668A2945CA71AA7933
```

Operation `225`의 candidate 1은 `candidateActiveTicks=7726` 뒤 exact activation에
성공했다. Candidate 1과 2는 timeout이 아니라 capacity로 제외됐고 candidate 3이
`COMPLETED/paired_delta_confirmed`, `storedItems=435`, `touchedStacks=23`,
`remainingStacks=0`을 기록했다. Operation `30351`도 candidate 1 capacity fallback 뒤
candidate 2에서 `COMPLETED`, `storedItems=155`, `touchedStacks=7`,
`remainingStacks=0`을 기록했다. 두 operation의 candidate/operation timeout decision은
0건이었다. 기존 고정 2,400-tick lifetime이 진행 중 candidate를 제거하지 않음을 직접
증명하는 장거리 경계는 candidate 1이 7,726 active ticks 뒤 activation한 operation 225다.
Operation 30351은 반복 완료와 timeout decision 0의 추가 관찰이다.

Build/deployed file hash와 runtime code-source path는 일치하지만 runtime manifest의
repository/source/build-input/runtime SHA fields는 `UNVERIFIED`이므로 strict artifact
판정은 `PARITY_UNPROVEN`을 유지한다. 실제 장시간 TaskRunner/safety-chain pause, 네
timeout reason 발동과 pending timeout에서 container click 0회는 별도 runtime matrix로
남는다.

Upstream-derived `InteractWithBlockTask`, TaskRunner, UserTaskChain,
`PlayerInteractionFixChain`과 Baritone behavior source는 수정하지 않았다.

### 12.1 Final cross-review and commit gate

현재 구현 topology에 맞는 direct coverage는 다음 세 소유권 경계로 분리한다.

```text
StoreHomeTaskLifecycleController decision order
    emergency hard cap과 pending/context/cursor precedence 유지
    active candidate + no session + no pending에서만 current position/exact
      activation을 operation no-progress 재판정 전에 소비
    narrow boundary에서는 full navigation/session step 0
    normal step 뒤 operation no-progress, 그 뒤 candidate timeout
    pending은 TRANSFER_UNCONFIRMED 우선
    activation/rejection/terminal transition exactly once

StoreHomeTask shared object graph
    root/controller/view의 exact StoreHomeExecutionState assertSame
    root/controller/timeout collaborator의 exact StoreHomeTimeoutLifecycle assertSame

command lifecycle identity integration
    assertSame(submittedRootTask, terminationObservation.task())
    projector는 그 observation Task의 typed outcome을 읽음
```

`StoreHomeTaskAssembly`는 dependencies, state와 timeout lifecycle을 받아 controller object
graph를 조립할 뿐 root Task를 생성하거나 보관하지 않는다. Controller/view도 Task
back-reference를 저장하지 않고, projector는 termination observation의 Task를 읽는다.
따라서 assembly 하나에 root/controller/projector Task identity를 억지로 넣거나 테스트를
위해 새 production back-reference를 추가하지 않는다.

현재 merge/commit gate는 아래 actual closure blocker 때문에 `BLOCKED`다. Runtime manifest
provenance와 `PARITY_UNPROVEN`은 검증 범위를 제한하는 별도 evidence qualifier이며, 이를
확인된 parity로 과장해서는 안 된다.

```text
evidence qualifier:
runtime manifest provenance: UNVERIFIED
artifactParity: PARITY_UNPROVEN

actual closure blockers:
StoreHomeTaskLifecycleController direct decision-order coverage: pending
shared state/timeout identity coverage: pending
submitted-root/termination/projector identity integration: pending
Python registered-command snapshot/support matrix: 22/22
production KoreanChatClefCommandRegistry: 21; deposit_all missing
focused pytest: 37 collected, 36 passed, 1 failed
```

`deposit_all` registry synchronization과 위 direct coverage는 STORE_HOME runtime behavior
변경과 섞지 않는다. Exact-set 검사를 유지하고 focused suite를 deselection/retry 없이
0 failure로 다시 확인한 뒤에만 이 gate를 재평가한다.

### 12.2 2026-08-29 final direct-gate and clean-build verification

§12.1의 blocker 수치는 최종 검증 직전 snapshot으로 보존한다. 이후 registry parity와
세 direct semantic gate를 다음 실제 결과로 닫았다.

```text
registered-command snapshot / support matrix / production registry: 22 / 22 / 22
focused Python: 38 tests passed, 179 subtests passed, 0 failures,
                no deselection or automatic retry
full focused Python closure: 404 passed, 2 pre-existing live-test skips,
                             2973 subtests passed, 0 failures

StoreHomeTaskLifecycleController decision-order direct tests: 8 passed
StoreHomeTask shared object-graph identity tests: 2 passed
command -> termination observation -> projector identity test: 1 passed
targeted Java aggregate: 8 classes, 32 tests, 0 failures, 0 errors, 0 skipped
```

기존 `.ps1`을 visible PowerShell 새 창에서 사용해 runtime root의 정확한
`clean build --rerun-tasks`를 실행한 결과는 다음과 같다.

```text
command: .\gradlew.bat clean build --rerun-tasks
result: BUILD SUCCESSFUL in 2m 45s; 171 actionable tasks, 171 executed
full 1.20.1 JUnit: 254 tests, 0 failures, 0 errors, 14 skipped
file: versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar
size: 7,115,508 bytes
sha256: D2175E8B0F8943388324929C6A6BAC16036EA8584848C25554440100156DFC8B
log: codex-build-logs/chatclef-fabric-1.20.1-build-20260829-140028.log
```

이 254-test clean build는 §12의 기존 243-test build/runtime 기록을 대체하거나 무효화하지
않는다. 최신 JAR은 active instance의
`C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\chatclef-1.20.1-0.18.23.jar`에
같은 7,115,508-byte 크기와 SHA-256으로 배포됐다. `latest.log`는 이 code source의 로딩,
WebSocket handshake 수락, 관련 Mixin/FATAL 또는 새 crash report가 없음을 기록했다.
같은 artifact의 `store_home` operation은 `paired_delta_confirmed`로 `COMPLETED`됐고
`storedItems=137`, `remainingStackCount=0`, `capacityFailureCount=2`였다. LAVI application
log도 callback과 matching task-finished event로 같은 typed result를 확인했다.

이 file/runtime evidence는 runtime manifest의 repository/source/build-input provenance를
새로 만들지 않으므로 해당 필드는 계속 `UNVERIFIED`, `artifactParity`는
`PARITY_UNPROVEN`이다. STORE_HOME behavior, timeout 수치, Baritone, TaskRunner와
`InteractWithBlockTask`는 이번 closure에서 변경하지 않았다. 검증된 source/test는
`ab0355b1`, Windows clean-build helper는 `d0a23ea`에 각각 기록했으며 문서 commit과 push는
이 기록 뒤의 별도 Git 단계다.

## 13. Related documents

- [Manual Trusted Home Storage Direction](chatclef-manual-trusted-home-storage-direction-2026-08-27.md)
- [ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)
- [ChatClef / Baritone Cache Troubleshooting](chatclef-baritone-cache-troubleshooting.md)
- [ChatClef Task Lifecycle Diagnostics](chatclef-task-lifecycle-diagnostics.md)
- [Fabric ChatClef 1.20.1 Build Verification](chatclef-fabric-build-verification.md)
