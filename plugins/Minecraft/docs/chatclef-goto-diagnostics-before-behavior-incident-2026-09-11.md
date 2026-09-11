<!-- 20260911_kpopmodder: Recorded the failed GOTO runtime acceptance, diagnostics-first rule violation, unresolved rollback boundary, and evidence gate for any future implementation. -->

# ChatClef GOTO Diagnostics-Before-Behavior Incident Record

Date: 2026-09-11

## 1. 현재 판정

이 문서는 Fabric ChatClef 1.20.1 GOTO 작업에서 발생한 진단 선행 규칙 위반,
실제 런타임 실패, 잘못된 검증 주장, 불명확한 원복 경계를 기록한다. 목적은
실패한 구현을 성공한 기반으로 재사용하거나 같은 추정 기반 행동 변경을 반복하지
않도록 하는 것이다.

```text
DOCUMENT_TYPE: INCIDENT_POSTMORTEM_RECOVERY_AND_RECURRENCE_PREVENTION_RECORD
STATUS: OPEN_FAILED_RUNTIME_ACCEPTANCE_ROOT_CAUSE_UNPROVEN
BACKEND_SCOPE: FABRIC_CHATCLEF_ONLY
INCIDENT_COMMAND: goto -500 80 16
INCIDENT_REQUEST_ID: lavi-input-ko-518bfb8efa254076a1f88a7dacdb36df
INCIDENT_CORRELATION_ID: lavi-a807e05d1acd415aa7cc8167049aa4e4
INCIDENT_OPERATION_ID: goto-23bd64e9-118b-4c3f-8b21-649cabf0afb2
INCIDENT_ROOT_TASK: lavi.minecraft.task.movement.gotoxyz.LaviGotoTask#3bfb6e0a
REVIEWED_REPOSITORY_ROOT: C:\Vtuber_Souorce_Code\LAVI
REVIEWED_BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
REVIEWED_HEAD: 92760206652ce155b482f3ddc85505702f9d811e
LOCAL_UPSTREAM_TRACKING_REF_AT_REVIEW: 92760206652ce155b482f3ddc85505702f9d811e
REMOTE_FETCH_PERFORMED_IN_THIS_REVIEW: NO
WORKTREE_PROVENANCE: DIRTY_MIXED_TRACKED_AND_UNTRACKED
WORKTREE_COUNT_SNAPSHOT_SCOPE: BEFORE_THIS_DOCUMENTATION_TASK
TRACKED_MODIFIED_FILES_AT_REVIEW: 39
TRACKED_COUNT_COMMAND: git diff --name-only
UNTRACKED_FILES_AT_REVIEW: 272
UNTRACKED_COUNT_COMMAND: git ls-files --others --exclude-standard
UNTRACKED_GOTO_OR_MOVEMENT_RELATED_FILES_AT_REVIEW: 270
RELATED_COUNT_FILTER: case-insensitive path regex goto|navigation|movement
BEHAVIOR_CHANGE_BEFORE_RUNTIME_CAUSE_PROOF: YES
DIAGNOSTICS_FIRST_RULE_COMPLIANCE: CONFIRMED_PROCESS_FAILURE
FOCUSED_TEST_OUTCOME: PASSED
FOCUSED_TEST_LIVE_FAILURE_MECHANISM_EVIDENCE_STATUS: NOT_PROVEN_RUNTIME
DEPLOYED_JAR_IDENTITY_STATUS: VERIFIED_ARTIFACT
DEPLOYED_JAR_IDENTITY_SCOPE: MATCHED_BUILD_ARTIFACT_BY_SIZE_AND_SHA256
GOTO_LIVE_ACCEPTANCE_STATUS: FAILED_RUNTIME_ACCEPTANCE
NAVIGATION_ROOT_CAUSE_STATUS: UNKNOWN
GOTO_A3_F_FINITE_EARLY_FAILURE_STATUS: FAILED_RUNTIME_ACCEPTANCE
GOTO_TERMINAL_COUNTER_EVENT_MAPPING_STATUS: NOT_PROVEN_RUNTIME
GOTO_COUNTER_SEMANTIC_GAPS_EVIDENCE: SOURCE_PROVEN
GOTO_FINITE_COUNTER_EXACT_TICK_CAUSAL_CHAIN_STATUS: UNKNOWN
GOTO_A2_EXACT_ARRIVAL_STATUS: NOT_EXERCISED
GOTO_BUILD_REQUIREMENT_EMISSION_STATUS: VERIFIED_RUNTIME
GOTO_BUILD_REQUIREMENT_EVENT_COUNT: 240
GOTO_BUILD_REQUIREMENT_EVIDENCE_STATUS: NOT_PROVEN_RUNTIME
GOTO_BUILDING_MATERIAL_READINESS_EMISSION_STATUS: VERIFIED_RUNTIME
GOTO_BUILDING_MATERIAL_READINESS_EVENT_COUNT: 16
GOTO_BUILDING_MATERIAL_READINESS_EVIDENCE_STATUS: NOT_PROVEN_RUNTIME
GOTO_EXECUTION_TRANSITION_EMISSION_STATUS: FAILED_RUNTIME_ACCEPTANCE
GOTO_EXECUTION_TRANSITION_OBSERVED_EVENT_COUNT: 0
NONTERMINAL_EMISSIONS_BEFORE_TERMINAL_PHASE_CHANGE: 256
NONTERMINAL_EMISSION_COUNT_EVIDENCE: DERIVED_FROM_OPERATION_SCOPED_EVENT_COUNTS
GOTO_EXECUTION_TRANSITION_BUDGET_STARVATION_EVIDENCE: SOURCE_PROVEN
GOTO_TERMINAL_DECISION_EMISSION_STATUS: VERIFIED_RUNTIME
GOTO_TERMINAL_DECISION_EVENT_COUNT: 1
GOTO_TERMINAL_DECISION_VERIFIED_SCOPE: OVERALL_TIMEOUT_DELIVERY_ONLY
GOTO_A4_BEHAVIOR_GATE_STATUS: NOT_EXERCISED
GOTO_A4_BEHAVIOR_GATE_REASON: TERMINAL_PAYLOAD_MATERIAL_STATE_REMAINED_NULL_AND_NO_FINITE_FAILURE_BRANCH_COMMITTED
GOTO_A5_MATERIAL_ACQUISITION_STATUS: NOT_EXERCISED
GOTO_A5_MATERIAL_ACQUISITION_REASON: NO_PER_COMMAND_OPT_IN
EVENT_COUNT_LOG_SOURCE: ACTIVE_INSTANCE_LATEST_LOG
EVENT_COUNT_OPERATION_ID: goto-23bd64e9-118b-4c3f-8b21-649cabf0afb2
EXACT_SAFE_ROLLBACK_SCOPE: UNKNOWN
FAILED_IMPLEMENTATION_ROLLBACK_SCOPE: UNKNOWN
RESTORATION_SALVAGE_AUDIT_STATUS: NOT_RUN
BLANKET_RESET_RESTORE_CHECKOUT_CLEAN: PROHIBITED
FAILED_WORK_DISPOSITION: REFERENCE_ONLY
MAXIMUM_NORMAL_SOURCE_CHANGE_CLASSIFICATION: DIAGNOSTICS_ONLY
CLASSIFICATION_IS_NOT_AUTHORIZATION: YES
SOURCE_CHANGE_AUTHORIZATION_FROM_THIS_DOCUMENT: NO
NEXT_ALLOWED_BEHAVIOR_CHANGE: BLOCKED_PENDING_RUNTIME_BOUNDARY_PROOF
PRODUCTION_CODE_CHANGE_IN_THIS_DOCUMENTATION_TASK: NONE
BUILD_IN_THIS_DOCUMENTATION_TASK: NOT_RUN
DEPLOYMENT_IN_THIS_DOCUMENTATION_TASK: NOT_RUN
RUNTIME_IN_THIS_DOCUMENTATION_TASK: NOT_RUN
COMMIT_AT_INITIAL_DOCUMENTATION_CAPTURE: NOT_RUN
PUSH_AT_INITIAL_DOCUMENTATION_CAPTURE: NOT_RUN
```

위 commit/push field는 이 문서의 최초 작성 직전 snapshot만 뜻한다. 이후 문서 전용
commit `0897d846`이 이 기록을 추가했고 `049d61b2`가 관련 상태 문서를 조정했다. 이
문서 commit 이력은 GOTO source, build 또는 runtime 검증 근거가 아니다.

현재 구현 판정은 `FAILED_RUNTIME_ACCEPTANCE`이다. 이는 저장소가 복구 불가능하다는
뜻이 아니라, 현재 GOTO 행동 구현을 검증된 기반으로 인정할 수 없다는 뜻이다.

다음 세 가지를 혼동하지 않는다.

1. 진단보다 행동을 먼저 구현한 절차 위반은
   `CONFIRMED_PROCESS_FAILURE`이다.
2. 실제 failed-calculation, wander, blacklist 반복과 조기 typed terminal의 부재는
   `[VERIFIED_RUNTIME]`이고, reader/state의 의미 계약 gap은 `[SOURCE_PROVEN]`이다.
   terminal의 no-path/no-progress counter는 consecutive 값이고, wander/recalculation은
   각각 filtered rising-edge/generation 값이며, 네 값 모두 reset될 수 있다. 따라서 raw
   engine event와 1:1로 대응한다고 가정할 수 없다. 정확한 event-to-counter mapping과
   최종 counter를 만든 tick별 인과 사슬은 `[UNKNOWN]`이다.
3. Baritone이 목표 부근의 남은 상승 구간 `(-501,70,17) -> (-500,80,16)`을
   완성하지 못한 단일 원인은 여전히 `UNKNOWN`이다. X/Z도 각각 1블록 다르므로 이
   구간을 순수 수직 경로로 단정하지 않는다.

## 2. 증거 분류 규칙

이 문서에서 사용하는 증거 label은 다음과 같다.

- `[VERIFIED_RUNTIME]`: 같은 correlation 또는 operation의 실제 런타임 로그에서 확인
- `[VERIFIED_ARTIFACT]`: 크기, hash 또는 명시된 artifact 비교로 확인
- `[SOURCE_PROVEN]`: 현재 소스의 직접적인 조건과 데이터 흐름으로 확인
- `[CONFIRMED_PROCESS]`: 대화·작업 기록으로 절차 수행 또는 위반을 확인
- `[INFERENCE]`: 증거와 일치하지만 아직 직접 관찰되지 않은 설명
- `[UNKNOWN]`: 현재 로그와 소스로 확정할 수 없음
- `[NOT_RUN]`: 해당 작업을 수행하지 않음

기능 단위와 절차의 outcome/status label은 다음과 같다.

- `NOT_EXERCISED`: 해당 runtime branch 또는 기능 단위가 실행되지 않음
- `NOT_PROVEN_RUNTIME`: 실행 관련 관찰은 있으나 runtime 계약을 입증할 증거가 불완전
- `FAILED_RUNTIME_ACCEPTANCE`: 요구된 live behavior가 acceptance를 충족하지 못함
- `CONFIRMED_PROCESS_FAILURE`: 작업 기록으로 절차 위반이 확인됨

빌드 성공, 테스트 통과, 예외 부재, JAR 동일성은 각각의 사실만 증명한다. 이 중
어느 것도 런타임 실패 원인이나 행동 수정의 정확성을 자동으로 증명하지 않는다.

## 3. 사용자 요청과 기대된 처리

사용자는 다음 wildcard discovery 경로와 exact active-instance 경로의 로그가
0바이트가 아니라고 명시하고, 코드를 수정하지 말고 로그를 확인해 달라고 요청했다.

```text
C:\Users\jaewo\curseforge\minecraft\Instances\*\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\*\logs\instance_audit.txt
C:\Users\jaewo\curseforge\minecraft\Instances\*\logs\stdout-logs.txt
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\instance_audit.txt
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
C:\Vtuber_Souorce_Code\LAVI\logs
```

사용자는 목표 가까이에 이미 있었다는 사실도 명시했다. 따라서 올바른 순서는
다음과 같았어야 한다.

1. 지정된 파일의 존재, 크기, 갱신 시각을 확인한다.
2. 기존 로그를 먼저 끝까지 조사한다.
3. 동일 request, correlation, operation, root Task를 연결한다.
4. 마지막 성공 경계와 최초 실패 경계를 확인한다.
5. 관찰할 수 없는 경계를 정확히 목록화한다.
6. 원인이 미확인이라면 행동을 바꾸지 않는 최소 bounded diagnostics만 추가한다.
7. 동일 조건을 재현한 후에만 최소 행동 수정을 검토한다.

실제로는 기존 로그를 충분히 읽기 전에 다른 근거리 좌표 대조 테스트를 제안했고,
그보다 앞선 구현 단계에서는 런타임 원인 증명 없이 행동 변경을 추가했다.

## 4. 사건 타임라인

| 시각 또는 순서 | 사건 | 판정 |
| --- | --- | --- |
| 런타임 재현 전 | GOTO route parent, 유한 종료, 재료 판단과 획득 관련 행동이 정적 추정과 focused tests를 근거로 구현됨 | `[SOURCE_PROVEN]` + `CONFIRMED_PROCESS_FAILURE` |
| artifact `LastWriteTime` 2026-09-11 12:17:13 +09:00; 비교는 후속 로그 감사 중 수행 | 해당 build JAR과 active-instance JAR의 크기·SHA-256 일치 확인 | `[VERIFIED_ARTIFACT]` |
| 12:23:57 | Minecraft 1.20.1과 Fabric Loader 0.19.3 실행 시작 | `[VERIFIED_RUNTIME]` |
| 12:29:21 | 한국어 입력 `-500 80 16 으로 가줘`가 `goto -500 80 16`으로 변환·접수됨 | `[VERIFIED_RUNTIME]` |
| 12:29:21 | `LaviGotoTask#3bfb6e0a`가 command correlation에 연결됨 | `[VERIFIED_RUNTIME]` |
| 이후 | `(-490,64,8)`에서 X/Z 목표 부근을 거쳐 `(-501,70,17)`까지 이동 | `[VERIFIED_RUNTIME]` |
| 이후 | 목표 부근의 남은 상승 구간 `(-501,70,17) -> (-500,80,16)`에서 path failure, `TimeoutWanderTask`, blacklist attempt가 반복됨 | `[VERIFIED_RUNTIME]` |
| 12:39:23 | 12,000 active ticks, 약 601.7초 후 전체 active-tick limit만 작동 | `[VERIFIED_RUNTIME]` |
| 로그 감사 후 | 실제 반복에도 조기 typed terminal이 없었고, observer가 descendant 존재를 간접 표본화할 뿐 authoritative episode transition·identity·attempt·reset cause를 기록하지 않아 raw event와 terminal counter의 mapping을 재구성할 수 없음을 확인 | `[VERIFIED_RUNTIME]` + `[SOURCE_PROVEN]` + `[UNKNOWN]` |
| 로그·소스 교차검사 후 | 같은 operation의 build 240건과 readiness 16건이 local nonterminal cap 256을 먼저 소진해 controller tick이 만든 terminal-quiescing transition 기록이 0건이 된 별도 진단 전달 결함을 확인 | `[VERIFIED_RUNTIME]` + `[SOURCE_PROVEN]` |
| 대화 후반 | exact inverse hunk와 provenance 없이 선택 원복을 먼저 권고함 | `CONFIRMED_PROCESS_FAILURE` |
| 현재 | reset, restore, checkout, clean, 캐시 변경 및 코드 원복은 실행하지 않음 | `[NOT_RUN]` |

정확한 최초 구현 시각과 문서 작성 전 감사 snapshot의 311개 변경 파일 각각의 작성
주체 및 단계별 provenance는 확정되지 않았다.

## 5. 로그와 artifact 증거

### 5.1 로그 파일은 비어 있지 않았다

2026-09-11 13:03경 확인한 snapshot은 다음과 같다. 로그는 실행 중 계속 증가할 수
있으므로 크기와 아래 line locator는 해당 조사 snapshot에만 적용된다. 실제로
13:20경 `latest.log`는 2,945,304 bytes, `stdout-logs.txt`는 3,407,872 bytes,
LAVI 로그는 128,600 bytes로 증가해 있었다. 현재 활성 로그를 나중에 다시 열어 같은
line number가 같은 내용을 가리킨다고 가정하면 안 된다.

| 파일 | 크기 | 이번 사건에서 확인한 역할 |
| --- | ---: | --- |
| `latest.log` | 2,813,977 bytes | Task, GOTO, Baritone path와 terminal 경계 |
| `stdout-logs.txt` | 3,252,224 bytes | wander, blacklist attempt와 전체 stdout 사건 |
| `instance_audit.txt` | 2,804,541 bytes | CurseForge instance 변경 이력; navigation 원인의 직접 증거는 아님 |
| `logs/20260911_122344_log.txt` | 106,460 bytes | 한국어 명령 변환, request lifecycle, 최종 terminal payload |

이 파일들을 0바이트이거나 볼 수 없는 로그로 취급하면 안 된다.

후속 증거 캡처는 `captured_at`, file size, `LastWriteTime`을 기록하고, 가능하면 활성
파일과 분리한 immutable snapshot의 SHA-256 또는 사건 구간의 bounded excerpt와
hash를 보존해야 한다. 이번 13:03 snapshot의 전체 파일 hash는 수집하지 않았으므로
소급해 만들어 내지 않는다.

### 5.2 실행 JAR 동일성

다음 build artifact와 active CurseForge JAR은 크기와 SHA-256이 동일했다.

```text
BUILD:
C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar

ACTIVE:
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\chatclef-1.20.1-0.18.23.jar

SIZE: 8751903 bytes
SHA256: 339C4665FEBE6C74742B3F2CFA82698AED0778003298D16D862398F422553550
SHARED_LAST_WRITE_TIME: 2026-09-11 12:17:13.229 +09:00
```

따라서 두 exact artifact는 조사 시점에 size, SHA-256과 `LastWriteTime`이 일치했다.
Minecraft launch와 명령 실행 시각은 그 뒤였으므로 이번 런타임 로그와 active artifact의
연결을 지지한다. 이 동일성은 배포 artifact identity를 증명하지만 행동 수정의
정확성을 증명하지는 않는다.

### 5.3 주요 evidence locator

| 파일과 위치 | 확인 내용 |
| --- | --- |
| `logs/20260911_122344_log.txt:162` | 원문, `goto -500 80 16` 변환, request와 correlation 접수 |
| `latest.log:1126-1130` | `LaviGotoTask` root 할당과 command binding |
| `latest.log:1135` | 시작 위치 `(-490,64,8)`과 목표 `(-500,80,16)` |
| `latest.log:1201-1203` | `(-500,64,16)` 도달 후에도 authoritative calculation/wander/material 경계가 unavailable |
| `latest.log:1169,1220` | calculation generation 1과 2의 `SUCCESS_SEGMENT` 및 각 segment 종점 |
| `latest.log:1276,1319,1343,1366,1390,1414` | calculation generation 3~8의 `FAILURE`와 no-path non-adoption |
| `latest.log:1438` | calculation generation 9의 `CANCELLATION` |
| `stdout-logs.txt:4075` | 첫 `Failed to make progress`, `TimeoutWanderTask` episode와 `Try 1 / 4` 시작. raw stdout 행 자체에는 correlation ID가 없으며, 같은 active GOTO 시간 구간·target과 인접한 correlated lifecycle 기록으로 사건에 연결됨 |
| `stdout-logs.txt:4157` | 실제 Task path `LaviGotoTask > GetToBlockTask > TimeoutWanderTask` |
| `stdout-logs.txt:7144` | 같은 active GOTO 시간 구간과 target에 `Try 15 / 4` 기록. raw 행 자체에는 correlation ID가 없음 |
| `latest.log:2492` | `OVERALL_ACTIVE_TICK_LIMIT_REACHED`, 최종 위치 `(-428,62,7)` |
| `logs/20260911_122344_log.txt:288` | 12,000 ticks, 601.7초, 최종 counter와 `deadline_exceeded` payload |
| `logs/20260911_122344_log.txt:289-293` | UI와 TTS failure feedback 전달 |

## 6. 실제 런타임에서 확인된 사실

- `[VERIFIED_RUNTIME]` 한국어 입력은 정확히 `goto -500 80 16`으로 변환됐다.
- `[VERIFIED_RUNTIME]` request, correlation, operation과 `LaviGotoTask` root가 연결됐다.
- `[VERIFIED_RUNTIME]` 시작 위치는 `(-490,64,8)`이었다.
- `[VERIFIED_RUNTIME]` X/Z 목표 부근과 `y=70`까지는 이동했다.
- `[VERIFIED_RUNTIME]` 목표 `(-500,80,16)`에는 도착하지 못했다.
- `[VERIFIED_RUNTIME]` path calculation generation 1과 2는 segment를 만들었다.
- `[VERIFIED_RUNTIME]` generation 3~8은 `FAILURE`, generation 9는
  `CANCELLATION`이었다.
- `[VERIFIED_RUNTIME]` `GetToBlockTask` 아래 `TimeoutWanderTask`가 실제로
  활성화됐다.
- `[VERIFIED_RUNTIME]` stdout에는 `Try 1 / 4`부터 `Try 15 / 4`까지 기록됐다.
- `[VERIFIED_RUNTIME]` 플레이어는 최종적으로 `(-428,62,7)`까지 목표에서
  멀어졌다.
- `[VERIFIED_RUNTIME]` 조기 path-unreachable 또는 movement-stalled 종료가 아니라
  12,000 active ticks의 전체 제한만 작동했다.
- `[VERIFIED_RUNTIME]` 실패 결과는 LAVI에 `deadline_exceeded`로 전달됐다.
- `[VERIFIED_RUNTIME]` 해당 실패의 UI와 TTS feedback도 전달됐다.

기능 단위 판정은 다음과 같이 분리한다.

| 기능 단위 | 현재 판정 | 근거 |
| --- | --- | --- |
| 한국어 입력 변환, command 접수와 root binding | `VERIFIED_RUNTIME` | 같은 request/correlation의 변환·dispatch·root 연결 |
| build JAR과 active-instance JAR 동일성 | `VERIFIED_ARTIFACT` | 동일 size와 SHA-256 |
| 12,000-active-tick terminal projection 및 UI/TTS 전달 | `VERIFIED_RUNTIME` | 같은 operation의 terminal payload와 feedback |
| A3-F 유한 조기 실패와 typed terminal | `FAILED_RUNTIME_ACCEPTANCE` | failed-calculation/wander/blacklist 반복 뒤에도 요구된 조기 terminal 없이 전체 timeout만 작동 |
| A3-F terminal counter와 engine event mapping | `NOT_PROVEN_RUNTIME` | no-path/no-progress는 consecutive, wander/recalculation은 filtered edge/generation이며 모두 reset 가능; raw failure/`Try` 횟수와 1:1 대응하지 않고 owner transition/reset log도 없음 |
| A2 exact arrival | `NOT_EXERCISED` | 목표에 도착하지 못해 arrival commit 경계 미실행 |
| A3D build-requirement/readiness 진단 방출 | `VERIFIED_RUNTIME` | operation-scoped `latest.log`에 각각 240건과 16건 |
| A3D material-readiness 증거 | `NOT_PROVEN_RUNTIME` | `observationComplete=false`; route usability, reserve, authoritative placement와 inventory 증거 불완전 |
| A3D execution-transition 진단 방출 | `FAILED_RUNTIME_ACCEPTANCE` | 같은 operation에서 0건; 256개 local nonterminal cap이 transition 전에 소진됨 |
| A4 material-readiness 행동 gate | `NOT_EXERCISED` | terminal payload의 usable/required material state가 `null`로 남았고 finite-failure branch terminal도 없었음; Section 8.8의 source/runtime data-flow 증거 참조 |
| A5 bounded material acquisition | `NOT_EXERCISED` | accepted command에 per-command opt-in이 없었음 |

따라서 확인된 plumbing 단위를 보존해 기록할 수는 있지만, 이를 합쳐 GOTO 행동
전체를 성공이나 부분 성공으로 판정할 수 없다. 사용자가 요구한 도착 동작과 유한
실패 판정의 전체 live acceptance는 `FAILED_RUNTIME_ACCEPTANCE`이다.

## 7. 유한 조기 종료 실패와 counter mapping 미입증

최종 effect payload에는 다음 값이 기록됐다.

```text
elapsed_ticks=12000
no_path_ticks=0
no_progress_ticks=11
wander_count=0
recalculation_count=1
terminal_result=TIMEOUT
evidence_reason=OVERALL_ACTIVE_TICK_LIMIT_REACHED
```

같은 사건 시간 구간에는 generation 3~8의 path failure, `TimeoutWanderTask`,
`Try 1 / 4`부터 `Try 15 / 4`까지의 반복이 존재한다. 그러나 no-path/no-progress는
consecutive counter이고, wander/recalculation은 각각 filtered rising-edge/generation
counter이며, 네 값 모두 reset될 수 있다. 누계 raw event 수가 아니므로 두 숫자 집합을
직접 동일시하거나 불일치 자체를 1:1 모순으로 단정하지 않는다.

확정된 acceptance 실패는 반복이 계속됐는데도 요구된
`PATH_UNREACHABLE`/`MOVEMENT_STALLED` 조기 terminal이 발생하지 않고 전체 timeout만
작동했다는 점이다. source에는 실제 lifecycle을 놓치거나 reset할 수 있는 의미 gap이
있지만, 정확한 event-to-counter mapping과 tick별 인과 사슬은 owner 경계 로그가 없어
`UNKNOWN`이다.

## 8. 소스로 확인된 counter 관찰·필터·reset 계약 gap

### 8.1 실제 no-path를 `CALCULATING`으로 가림

[MinecraftGotoNavigationObservationReader.java](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/movement/gotoxyz/execution/observation/MinecraftGotoNavigationObservationReader.java)는
`inProgress`, `next`, 또는 `customGoalProcess().isActive()` 중 하나가 참이면
`CALCULATING`을 반환한다. 실패 계산이 끝나 현재 path가 없어도 custom goal이
active이면 authoritative `NO_PATH`로 분류되지 않는다.

[GotoFiniteState.java](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/movement/gotoxyz/finite/GotoFiniteState.java)는
`NO_PATH`가 아닌 모든 관찰에서 `noPathTicks`를 초기화한다. 이 조합은 반복된 path
failure가 finite observation에서 누락되고 `noPathTicks`가 다시 0으로 초기화될 수 있는
`[SOURCE_PROVEN]` mechanism이며 최종값과 일치한다. 다만 그 최종값을 만든 tick별
적용 사슬은 `[UNKNOWN]`이다.

### 8.2 목표에서 멀어지는 이동도 progress로 분류

`GotoFiniteState.observeProgress`는 다음 조건을 사용한다.

```text
progressObserved = blockChanged || targetDistanceDecreased
```

따라서 목표에서 멀어지는 wander라도 블록 좌표가 달라지면 progress가 된다.
progress가 참이면 wander와 recalculation counter도 초기화된다. 위치 변화 자체는
목표를 향한 진전의 증거가 아니다.

### 8.3 authoritative wander episode 경계를 관찰하지 않음

`MinecraftGotoNavigationObservationReader`는 실제 `TimeoutWanderTask`의 시작과
종료 transition이 아니라 `movementChild.thisOrChildAreTimedOut()`를 wander 상태로
사용한다. 이 값은 descendant에 `TimeoutWanderTask`가 존재하는지를 간접 표본화한다.
실제 Task path에 `TimeoutWanderTask`가 있었지만 terminal `wander_count`는 0이었다.
최종값 0은 중간 reset이 있었다면 가능하므로 그 자체가 raw event와의 수치 모순은
아니다. 다만 이 간접 값만으로는 authoritative episode transition, identity, attempt,
increment 또는 reset cause를 증명하거나 terminal 값까지 reconcile할 수 없다.

현재 `Task.thisOrChildAreTimedOut()` 구현은 실제 descendant 중
`TimeoutWanderTask`가 있는지를 반환한다. 하지만 persistent wander child의 동일
instance 재사용과 실제 episode start/stop 경계를 별도로 기록하지 않으므로
rising-edge counter의 권위 있는 attempt source가 되지 못한다.

### 8.4 서로 다른 generation을 동일 의미로 사용

reader가 유한 상태에 전달하는 generation은 실제 Baritone calculation generation이
아니라 `placementRequirement.pathGeneration()`이다. 실패 계산에는 채택된 path가
없을 수 있으므로 실제 generation 3~8을 이 값으로 셀 수 없다. 실제 다수 계산과
terminal `recalculation_count=1`을 1:1 비교할 수 없으며, 문서화된 mapping과 reset
경계가 없다는 사실은 이 ownership mismatch와 일치한다.

위 소스 조건들은 raw engine lifecycle과 다른 counter-specific 관찰·필터·reset 계약과
coverage gap을 직접 보여 준다. 다만 각 runtime tick에서 어느 조건이 최종값
`0/11/0/1`을 만들었는지는 owner transition log가 없어 아직 완전히 재구성되지 않았다.

### 8.5 blacklist 표시 한도는 Task 종료 조건이 아님

`CustomBaritoneGoalTask`가 progress failure 때 persistent `TimeoutWanderTask`를
반환하고 `GetToBlockTask.onWander`가 blacklist update를 요청한다. blacklist는 거리
개선에 따라 reset될 수 있으며 `Try N / allowed`를 출력한다. 그러나
`GetToBlockTask.isFinished()`는 goal과 dimension만 확인하므로 `Try 4 / 4` 초과 자체가
이 Task를 끝내지 않는다. 이 owner 관계를 확인하지 않고 표시 숫자만 유한 종료
정책으로 해석하면 안 된다.

### 8.6 focused test가 실제 실패 궤적을 검증하지 않음

[GotoFiniteStateTest.java](../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/movement/gotoxyz/finite/GotoFiniteStateTest.java)는
위치 변경 후 counter가 초기화되는 현재 계약을 통과시킨다. 그러나 실제 사건처럼
목표에서 멀어지는 block change, 실제 nested `TimeoutWanderTask`, path가 채택되지
않은 failed calculation generation, `Try 15 / 4` 상황을 함께 재현하지 않는다.

테스트가 구현된 가정을 확인했을 뿐 실제 엔진 실패 lifecycle을 확인하지 못했다.

### 8.7 execution-transition 진단은 local budget에 의해 누락됨

사건 operation `goto-23bd64e9-118b-4c3f-8b21-649cabf0afb2`만 대상으로
`latest.log`를 집계하면 다음과 같다. `stdout-logs.txt`는 같은 console stream의
복제이므로 이 수치에 더하지 않는다.

```text
GOTO_BUILD_REQUIREMENT_OBSERVED: 240
GOTO_BUILDING_MATERIAL_READINESS: 16
GOTO_MATERIAL_ACQUISITION_TRANSITION: 0
GOTO_TERMINAL_DECISION: 1
LOCAL_NONTERMINAL_EMISSIONS_BEFORE_TERMINAL_PHASE_CHANGE: 256
COUNT_EVIDENCE: DERIVED_FROM_OPERATION_SCOPED_LATEST_LOG_EVENTS
```

`LaviGotoTask.onTick`은 A3D tick snapshot을 `executionController.tick()`보다 먼저
호출한다. build-requirement와 readiness observer, 그리고 controller tick 뒤의
execution-transition observer는 모두 같은 `GotoA3dEmissionBudget.admitNonterminal`
pool을 사용한다. 이 pool은 256건 뒤 거부하며 transition용 slot을 따로 보존하지
않는다. controller가 overall timeout을 commit하며 phase를
`QUIESCING_FOR_TERMINAL`로 바꾼 뒤 transition observer가 호출됐을 때는 이미
`240 + 16 = 256`이었다. 따라서 이 terminal-quiescing transition의 물리 방출이
막힌 mechanism은 `[SOURCE_PROVEN]`이고, 0건은 `[VERIFIED_RUNTIME]`이다.

이 hook은 모든 `GotoExecutionPhase` 변경을 포괄하지 않는다. 오직
`executionController.tick()` 전후에 관찰되는 controller-tick-owned 변경만 보며,
그 전에 return하는 arrival freeze와 다른 lifecycle 경로의 resume/stop 변경은 이
hook 범위 밖이다. local `TERMINAL_CAP=32`는 256개 nonterminal pool 안의 예약분이
아니라 별도 local cap이고, 실제 terminal 방출은 공용 session/family admission에도
종속된다. 이 진단 전달 결함은 navigation 실패의 단일 원인과 별개이며, navigation
root cause는 계속 `UNKNOWN`이다.

### 8.8 readiness 진단 방출은 A4 행동 실행 증거가 아님

`GotoExecutionController.tickNavigation`은 overall timeout을 먼저 검사하고, 그
검사를 통과한 tick에서만 finite navigation failure를 계산한다. non-null failure가
있으면 A4 최초 진입 전에 `latestMaterials`와 `latestReadiness`를 설정한다. `READY`는
complete material snapshot과 proven positive requirement를 모두 요구하므로, 이
branch가 한 번이라도 실행된 뒤 timeout에 도달했다면 terminal payload의
`usable_building_material_count`와 `required_building_material_count`가 둘 다
non-null이어야 한다. controller에는 이 두 A4 state를 다시 null로 초기화하는 retry
경로가 없고, `READY` retry는 새 navigation child generation도 만든다. 반대로 이
no-opt-in upward run에서 non-`READY` A4 결과는 즉시 sticky non-timeout terminal을
commit한다.

실제 최종 payload는 `TIMEOUT`, `OVERALL_ACTIVE_TICK_LIMIT_REACHED`,
`usable_building_material_count=null`, `required_building_material_count=null`,
`material_acquisition_attempts=0`이었고, operation-scoped lifecycle log에는
`GetToBlockTask#63795af6` 하나만 나타났다. 따라서 timeout의 순서만이 아니라 이
source/runtime data flow 전체를 근거로 A4 behavior gate를 `NOT_EXERCISED`로 판정한다.

반면 16개의 `GOTO_BUILDING_MATERIAL_READINESS`는 controller와 독립된 A3D tick
snapshot observer가 방출했고 모두 `observationComplete=false`였다. 그러므로 진단
방출은 `VERIFIED_RUNTIME`, material-readiness evidence는 `NOT_PROVEN_RUNTIME`, A4
행동 branch는 `NOT_EXERCISED`로 서로 분리한다. A5는 accepted command의
per-command opt-in 부재만으로도 독립적으로 `NOT_EXERCISED`였으며, A4 미진입은 추가
경로 증거일 뿐 A5 status의 필수 근거로 합치지 않는다.

## 9. 아직 확인되지 않은 사실

다음 항목은 계속 `UNKNOWN`으로 유지한다.

- `y=70` 부근에서 `y=80`까지 path가 완성되지 않은 단일 원인
- 목표의 feet `y=80`, support `y=79`, head `y=81`과 인접 접근 칸의 정확한
  collision, passable, loaded 상태
- 해당 calculation의 `allowBreak`, `allowPlace` 및 관련 Baritone 설정값
- 실제 route에 사용할 수 있었던 throwaway/building material 수량
- blacklist가 허용 횟수 뒤에도 반복된 정확한 owner와 reset 조건
- `TimeoutWanderTask`의 authoritative start, stop, attempt, reason transition
- 월드가 복사, 복원, 교체 또는 개명됐다면 Baritone cache가 영향을 줬는지 여부
- 최초 실패를 발생시킨 정확한 메서드와 조건
- 문서 작성 전 감사 snapshot의 311개 변경 중 사용자 작업과 이번 실패 구현의
  exact file/hunk provenance
- 안전하게 제거할 수 있는 exact inverse hunk 집합

`target voxel=minecraft:air`만으로 목표가 설 수 없는 좌표라고 판단할 수 없다. 발
위치가 air인 것은 정상일 수 있으며 support, headroom, collision과 접근 경로를 함께
관찰해야 한다.

Baritone cache가 읽히고 쓰였다는 사실은 확인됐지만 cache staleness 또는 corruption이
이번 실패 원인이라는 증거는 없다.

## 10. 위반한 AGENTS.md 규칙

### 10.1 Diagnostics before behavior

`AGENTS.md`는 원인 미확인 조사에서 diagnostic observer 또는 최소 logging hunk를
행동 수정보다 우선하도록 요구한다. 이 순서를 지키지 않았다.

### 10.2 Unknown-cause behavior gate

`AGENTS.md`는 exact last successful boundary, first failing boundary, triggering
state가 입증되지 않으면 behavior change를 금지한다. 이번 구현 전에는 세 항목이
입증되지 않았다.

### 10.3 Diagnostic-only first pass

원인이 입증되지 않았을 때 첫 source change는 runtime behavior를 바꾸지 않는
diagnostics-only여야 한다. timeout, retry, blacklist, cancellation, fallback을 증거
없이 추가하지 말라는 규칙도 존재했다. 이번 GOTO 문제는 exact container GUI-open
stabilization 예외에 해당하지 않는다.

### 10.4 기존 로그 우선 조사 실패

사용자가 정확한 non-empty log path와 read-only 요청을 제공했음에도 기존 로그를
먼저 조사하지 않았다. 사용자가 이미 가까운 위치였다는 조건을 확인하지 않고 다시
근거리 좌표 테스트를 제안했다.

### 10.5 검증 수준 과장

focused tests와 build를 runtime cause proof처럼 취급했다. 실제 live acceptance 전에는
`SOURCE_IMPLEMENTED`, `FOCUSED_TESTED`, `NOT_RUNTIME_VERIFIED` 이상으로 주장하면
안 됐다. live run 이후에는 분명히 `FAILED_RUNTIME_ACCEPTANCE`로 내려야 했다.

### 10.6 rollback 준비와 변경 격리 실패

진단, 행동, command plumbing, 결과 projection, 테스트가 독립 rollback unit으로
격리되지 않았다. 실패 후 exact file/hunk 범위를 확정하지 않은 채 선택 원복을 먼저
권고했다.

## 11. 원복 범위를 확정할 수 없는 이유

이 문서 작성 전 snapshot에서 `git diff --name-only`로 센 tracked modified file은
39개였고, `git ls-files --others --exclude-standard`로 센 untracked file은 272개였다.
untracked path에 case-insensitive `goto|navigation|movement` regex를 적용하면 270개가
일치했지만, 파일명 일치만으로 작성 주체나 재사용 가능성을 판정할 수 없다. 이 숫자는
현재 worktree의 실시간 총계가 아니라 문서화 작업 전 감사 snapshot이다.

다음 종류가 한 worktree에 섞여 있다.

- 사용자가 먼저 작업한 한국어 GOTO 문법과 admission 변경
- command, correlation, root binding과 terminal result plumbing
- side-effect-free diagnostics
- GOTO finite termination behavior
- building-material readiness와 acquisition behavior
- 테스트와 문서

사고 감사 snapshot 당시 `HEAD`와 로컬 upstream tracking ref는
`92760206652ce155b482f3ddc85505702f9d811e`로 일치했다. 이 값은 당시 비교 가능한
committed point일 뿐 이 문서를 읽는 현재 시점의 `HEAD`, tracking ref 또는 최신
GitHub 상태를 뜻하지 않는다. 그 감사에서는 remote fetch도 수행하지 않았다.

이 지점으로 현재 worktree를 직접 되돌리면 사용자 변경과 확인된 중립 plumbing도
함께 손실될 수 있다. 따라서 다음 명령과 동등한 광범위 원복은 금지한다.

```text
git reset --hard
git clean -fd
전체 worktree 대상 git restore
전체 worktree 대상 git checkout
```

현재 구현은 reference-only이다. exact hunk salvage audit가 끝나기 전에는 성공한
기반으로 merge, cherry-pick, 복사 또는 재적용하지 않는다.

## 12. 재발 방지 필수 게이트

### Gate A — 기존 로그 우선

사용자가 로그 경로를 제공하면 다음을 행동 수정이나 추가 재현 요청보다 먼저 한다.

1. literal path, wildcard expansion, 크기와 last-write 시각을 확인한다.
2. active instance를 식별한다.
3. correlation과 operation별로 관련 구간을 조사한다.
4. 각 파일에서 얻은 사실과 얻지 못한 사실을 구분한다.
5. 로그가 부족할 때만 missing boundary 목록을 작성한다.

출력 truncation, 잘못된 glob, 잘못된 파일 선택을 0바이트 또는 로그 부재로 해석하지
않는다.

### Gate B — Unknown root cause fail-closed

실패, hang, loop, 잘못된 terminal 또는 예기치 않은 결과가 있고 exact cause가
증명되지 않으면 즉시 다음 상태를 사용한다.

```text
INVESTIGATION_STATUS: UNKNOWN_ROOT_CAUSE
MAXIMUM_NORMAL_SOURCE_CHANGE_CLASSIFICATION: DIAGNOSTICS_ONLY
CLASSIFICATION_IS_NOT_AUTHORIZATION: YES
NEW_OR_REPLACEMENT_BEHAVIOR_CHANGE: PROHIBITED
RECOVERY_ROLLBACK: ONLY_PER_GATE_G.1
```

다음 causal evidence 중 한 항목이라도 `UNKNOWN`이거나 root cause가
`VERIFIED_RUNTIME`이 아니면 새 행동 또는 replacement behavior를 변경하지 않는다.

```text
NAVIGATION_ROOT_CAUSE_STATUS:
REPRODUCTION_ID:
ACTIVE_ARTIFACT_SHA256:
COMMAND_CORRELATION_ID:
OPERATION_ID:
PARENT_TASK:
CHILD_TASK:
FINITE_COUNTER_OWNER:
TERMINAL_DECISION_OWNER:
DIAGNOSTIC_BUDGET_OWNER:
FIRST_REQUIRED_PHASE_TRANSITION_EMISSION_STATUS:
DIAGNOSTIC_SUPPRESSION_REASON:
RETRY_OWNER:
WANDER_OWNER:
PATH_OR_GOAL_OWNER:
LAST_SUCCESSFUL_BOUNDARY:
FIRST_FAILING_BOUNDARY:
TRIGGERING_STATE:
EXPECTED_TRANSITION:
OBSERVED_TRANSITION:
EXACT_FILES_AND_HUNKS_PROPOSED:
EXACT_PROPOSED_FIX_BOUNDARY:
```

모든 source 변경은 별도로 다음 rollback evidence가 필요하다.

```text
PROPOSED_CHANGE_EXACT_ROLLBACK_UNIT:
```

`RECOVERY_ROLLBACK`은 추가로 다음 recovery evidence가 필요하다.

```text
FAILED_IMPLEMENTATION_ROLLBACK_SCOPE:
USER_APPROVED_EXACT_ROLLBACK_UNIT:
```

위 제한은 새 행동 또는 replacement behavior의 정상 gate다. 이미 실패한 행동만
제거하는 `RECOVERY_ROLLBACK`은 이 상한과 별도로 Gate G.1의 모든 provenance,
inverse-hunk, unrelated-change 보존과 explicit-approval 조건을 만족할 때만 검토한다.

### Gate C — 실제 owner 경계에서 관찰

retry, wander, timeout, path generation을 주변 snapshot으로 추정하지 않는다. 이
사건의 다음 diagnostics-only pass는 최소한 다음을 correlation 단위로 관찰해야 한다.

- `CustomBaritoneGoalTask`의 completing에서 wandering 전환과 reason
- `GetToBlockTask.onWander` 진입, target, attempt와 blacklist 전후 값
- `TimeoutWanderTask` identity, start tick, stop tick과 parent identity
- Baritone calculation generation, result, current/next/in-progress executor identity
- 이전/현재 player position과 target distance squared
- `blockChanged`, `distanceDecreased`, progress decision과 counter reset reason
- target feet, support, head와 제한된 인접 접근 칸 상태
- relevant `allowBreak`, `allowPlace`와 실제 사용 가능한 material 수량
- 모든 finite counter의 before/after와 terminal candidates
- local diagnostic budget의 event별 admission count, suppression reason과 첫
  controller-tick-owned phase transition 보존 여부

로그는 state change, counter change, threshold 접근과 terminal에 한정하고 기존 bounded
diagnostics budget을 따라야 한다. 관찰을 위해 Task, input, goal, path, retry, timeout,
fallback 또는 결과를 변경하면 안 된다.

### Gate D — counter 일관성 검사

다음 mapping-aware 불일치가 하나라도 발생하면 runtime verification은 즉시 실패다.
raw Baritone event 수와 resettable command counter가 단순히 다르다는 이유만으로
실패를 선언하지 않는다. 먼저 문서화된 increment/filter/reset mapping을 적용한다.

```text
mapped wander episode increment observed
    && no mapped reset/progress occurred afterward
    && reported wander_count omits the increment
mapped calculation-generation increment observed
    && no mapped reset/progress occurred afterward
    && reported recalculation_count omits the increment
mapped consecutive no-path sample observed
    && no mapped reset occurred afterward
    && no_path_ticks omits the sample without an explicit reason
```

`targetDistanceIncreased=true`인데 `blockChanged=true`만으로 progress가 되는 경우는 raw
count 모순이 아니라 원하는 progress semantics의 별도 acceptance 실패로 검사한다.

### Gate E — 실제 사건을 재현하는 테스트

테스트에는 최소한 다음 negative trajectory가 포함돼야 한다.

- 목표에서 멀어지는 block change는 progress가 아님
- nested `TimeoutWanderTask` start와 stop을 한 번만 계수
- adopted path가 없는 failed calculation generation 계수
- custom goal active와 actual no-path 상태 구분
- blacklist attempt owner/reset 의미와 command-level terminal counter의 mapping 또는
  의도적 독립성을 검증하고, 숫자가 같아야 한다고 가정하지 않음
- unavailable observation은 success 또는 progress로 변환되지 않음

fixture 내부에서 만든 가정을 확인하는 것만으로 실제 engine behavior 보존을 주장하지
않는다.

### Gate F — 검증 상태 언어

evidence-strength field는 다음 canonical base token 중 하나만 사용한다. count, scope,
reason, role 또는 시점은 token에 접미사로 합치지 않고 별도 field에 기록한다.

```text
VERIFIED_RUNTIME
VERIFIED_ARTIFACT
SOURCE_PROVEN
CONFIRMED_PROCESS
INFERENCE
UNKNOWN
NOT_RUN
```

live feature outcome field는 다음 중 하나만 사용한다.

```text
NOT_EXERCISED
NOT_PROVEN_RUNTIME
FAILED_RUNTIME_ACCEPTANCE
```

`NOT_PROVEN_RUNTIME`은 의도적으로 두 문맥에서 사용할 수 있는 공통 token이다. runtime
관찰은 있지만 요구된 증거 계약이 불완전한 `_EVIDENCE_STATUS`에도 사용할 수 있고,
실행 관련 관찰만으로 기능 계약을 입증하지 못한 live feature outcome에도 사용할 수
있다. field 이름과 별도 reason/scope로 두 문맥을 구분하며 `VERIFIED_RUNTIME`으로
간주하지 않는다.

process outcome과 action classification/authorization은 별도 enum이다.

```text
PROCESS_OUTCOME: CONFIRMED_PROCESS_FAILURE
PROPOSED_CHANGE_CLASSIFICATION: NONE, DIAGNOSTICS_ONLY, RECOVERY_ROLLBACK, or BEHAVIOR
MAXIMUM_NORMAL_SOURCE_CHANGE_CLASSIFICATION: NONE, DIAGNOSTICS_ONLY, or BEHAVIOR
AUTHORIZATION: YES or NO
```

예를 들어 `NOT_EXERCISED_NO_OPT_IN`을 한 값으로 쓰지 않고 다음처럼 분리한다.

```text
STATUS: NOT_EXERCISED
REASON: NO_PER_COMMAND_OPT_IN
```

focused tests 통과, build 성공, matching JAR, terminal feedback는 각각 별도로 기록한다.
이들을 합쳐 root cause 또는 behavior fix의 runtime verification으로 표현하지 않는다.

### Gate G — 변경과 rollback unit 격리

행동 변경 전에 다음을 기록한다.

- clean comparison commit과 ref freshness
- 시작 전 dirty worktree 목록
- 기존 사용자 변경
- exact files, methods와 minimal hunks
- 각 hunk의 behavior effect
- exact inverse hunk 또는 rollback commit
- diagnostics와 behavior change의 별도 unit

provenance 또는 rollback unit이 불명확하면 현재 worktree에서 원복하지 않는다. 별도
clean worktree 또는 branch 사용도 사용자 승인과 repository safety gate를 먼저
충족해야 한다.

untracked path가 baseline에 없다는 사실은 작성 주체, 실패 구현 소속 또는 삭제
안전성을 증명하지 않는다. 독립적인 creation provenance가 없으면 rollback/delete
대상으로 분류하지 않는다. 제거를 검토할 때도 exact path, method/hunk 또는 whole-file
impact, unrelated-change 보존법과 사용자의 명시적 exact-action 승인이 필요하다.

#### Gate G.1 — 좁은 recovery rollback 예외

`RECOVERY_ROLLBACK`은 이미 실패한 행동 hunk를 제거하는 복구 분류이며 새 행동 수정이
아니다. root cause를 안다고 가장하지 않고도 사용할 수 있지만, 다음 조건을 모두
충족해야 한다.

여기서 strict read-only audit는 `AGENTS.md` restoration protocol의 generic no-write,
evidence-label, complete-transcript, worktree inventory, untracked-provenance와 exact
method/diff-hunk 규칙만 재사용한다. 그 protocol의 Carry On 전용 commit `76026f8`,
branch/file 목록, marker, evidence checklist와 report template는 이 GOTO baseline이
아니다. GOTO comparison baseline과 target evidence는 이 Gate G/G.1에서 별도로
입증해야 한다.

- strict read-only provenance/restoration audit가 먼저 완료됨
- known committed comparison baseline과 intended reference의 관계가 입증됨
- 최신 remote/GitHub 상태라고 주장할 경우 current read-only remote-freshness evidence가
  있으며, 그렇지 않으면 freshness를 `UNKNOWN`으로 유지함
- failed implementation의 exact file/method/hunk와 exact inverse hunk가 확인됨
- 모든 unrelated 사용자 변경을 보존하는 방법이 확인됨
- 사용자가 exact method/hunk 또는 expected impact가 명시된 whole-file removal을
  `USER_APPROVED_EXACT_ROLLBACK_UNIT`으로 승인함; path-only 승인은 불충분함
- active request에 recovery rollback 승인이 source-edit 승인과 별도로 기록됨
- rollback 자체가 독립적으로 review/revert 가능한 unit이며 replacement behavior가 없음

strict audit 중에는 rollback scope를 대화 보고서로만 제시하고 repository에 manifest를
저장하지 않는다. audit가 종료된 뒤 별도의 명시적 문서화 요청이 있을 때만 manifest를
파일로 남길 수 있다. 이 사고 문서는 completed restoration/salvage audit 또는 rollback
manifest가 아니다. 위 조건 하나라도 충족되지 않으면 recovery rollback도 blocked다.

승인된 rollback을 적용하기 직전에 `HEAD`, exact target hash, hunk context,
staged/unstaged 상태와 관련 untracked path를 다시 read-only로 확인한다. audit snapshot
또는 `USER_APPROVED_EXACT_ROLLBACK_UNIT`과 달라졌거나 hunk overlap이 생기면 기존
rollback unit과 승인은 무효다. 적용을 중단하고 strict audit과 exact-unit 승인을 다시
받는다.
latest remote/GitHub ref라는 주장에 의존하는 unit은 적용 직전 동일한 read-only
`git ls-remote` query로 remote object ID도 다시 확인한다. ref가 이동했거나 freshness
확인이 불가능하면 그 remote-dependent unit과 승인은 무효다.

### Gate H — 월드와 Baritone cache provenance

재현 전에 active world identity와 save path, copy/restore/replace/rename 이력, cache
read/write evidence를 기록한다. cache가 존재하거나 사용됐다는 사실만으로 stale,
corrupt 또는 causal이라고 판정하지 않는다.

cache rename, delete 또는 다른 mutation은 사용자가 exact cache path와 동작을
명시적으로 승인한 경우에만 가능하다. 그 전제는 active Task stop, world exit,
Minecraft process의 완전 종료와 exact resolved path 재확인이다. cache-reset run과
non-reset run은 별도 reproduction ID와 evidence로 유지한다.

manual cache path/action 승인은 Task stop, world exit, Minecraft process control 또는
runtime reproduction 권한을 자동으로 포함하지 않는다. 각 동작도 active user
request의 명시적 범위여야 한다.

재현 또는 cache 변경 전에는 최소한 다음 ledger를 채운다.

```text
ACTIVE_WORLD_IDENTITY:
ACTIVE_WORLD_SAVE_PATH:
WORLD_COPY_RESTORE_REPLACE_RENAME_HISTORY:
BARITONE_CACHE_EVIDENCE:
CACHE_TARGET_ABSOLUTE_PATH:
CACHE_ACTION: NONE, INSPECT_ONLY, or exact manual mutation
TASK_STOPPED: YES, NO, UNKNOWN, or NOT_APPLICABLE
TASK_STOPPED_EVIDENCE_STATUS: VERIFIED_RUNTIME, UNKNOWN, or NOT_APPLICABLE
WORLD_EXITED: YES, NO, UNKNOWN, or NOT_APPLICABLE
WORLD_EXITED_EVIDENCE_STATUS: VERIFIED_RUNTIME, UNKNOWN, or NOT_APPLICABLE
MINECRAFT_PROCESS_CLOSED: YES, NO, UNKNOWN, or NOT_APPLICABLE
MINECRAFT_PROCESS_CLOSED_EVIDENCE_STATUS: VERIFIED_RUNTIME, UNKNOWN, or NOT_APPLICABLE
NOT_APPLICABLE_REASON:
ACTIVE_REQUEST_AUTHORIZES_MINECRAFT_LAUNCH: YES or NO
ACTIVE_REQUEST_AUTHORIZES_RUNTIME_REPRODUCTION: YES or NO
ACTIVE_REQUEST_AUTHORIZES_LIVE_WORLD_EXECUTION: YES or NO
ACTIVE_REQUEST_AUTHORIZES_LIVE_WORLD_BLOCK_MUTATION: YES or NO
ACTIVE_REQUEST_AUTHORIZES_TASK_STOP: YES or NO
ACTIVE_REQUEST_AUTHORIZES_WORLD_EXIT: YES or NO
ACTIVE_REQUEST_AUTHORIZES_MINECRAFT_PROCESS_CONTROL: YES or NO
ACTIVE_REQUEST_AUTHORIZES_MANUAL_CACHE_MUTATION: YES or NO
```

authorization field는 항상 `YES` 또는 `NO`이며 `NOT_APPLICABLE`을 사용할 수 없다.
state/evidence field만 선택한 action과 무관한 phase일 때
`NOT_APPLICABLE_REASON`과 함께 `NOT_APPLICABLE`을 사용할 수 있다. manual cache
mutation에서는 세 prerequisite state/evidence가 모두 `YES`/`VERIFIED_RUNTIME`이어야
하므로 `NOT_APPLICABLE`일 수 없다.

Minecraft 또는 Baritone이 정상 runtime 중 자체 cache를 자동 read/write하는 것은 예상
runtime side effect로 기록한다. 이는 manual rename, delete, reset, replacement 또는
다른 cache mutation 승인이 아니다. GOTO/A5는 이동뿐 아니라 block break/place까지
일으킬 수 있으므로 launch, reproduction, live-world execution과 live-world block
mutation 승인을 합치지 않는다.

manual cache mutation 직전에는 `TASK_STOPPED`, `WORLD_EXITED`,
`MINECRAFT_PROCESS_CLOSED`가 각각 `YES`이고 각 evidence status가
`VERIFIED_RUNTIME`이어야 하며 manual cache mutation authorization도 `YES`여야 한다.
Codex가 이 상태를 만들기 위해 Task stop, world exit 또는 process control을 수행해야
한다면 해당 action authorization도 먼저 `YES`여야 한다.

## 13. 기계적 강제 장치 제안

`AGENTS.md`와 이 문서는 필수 규칙이지만 자연어 문서만으로 위반을 기술적으로
불가능하게 만들 수는 없다. 같은 실수를 실질적으로 줄이려면 다음 후속 작업이
필요하다.

```text
MECHANICAL_GUARDS_STATUS: PROPOSED_NOT_IMPLEMENTED
```

- runtime evidence manifest가 없으면 behavior source diff를 거부하는 CI gate
- diagnostics-only 단계에서 허용된 source path와 hunk 목적을 검사하는 allowlist
- diagnostics와 behavior change를 별도 branch 또는 commit으로 요구하는 review gate
- exact rollback unit이 없는 behavior change를 거부하는 check
- `UNKNOWN` evidence field가 하나라도 있으면 behavior phase 진입을 거부하는 check
- 문서화된 increment/filter/reset mapping에 따라 owner event와 terminal counter를
  reconcile하는 integration assertion

이 문서 작성만으로 위 강제 장치가 구현됐다고 주장하지 않는다.

## 14. 안전한 복구 상태와 다음 순서

현재 허용되는 결론은 다음과 같다.

```text
CURRENT_IMPLEMENTATION_RUNTIME_OUTCOME: FAILED_RUNTIME_ACCEPTANCE
CURRENT_IMPLEMENTATION_DISPOSITION: REFERENCE_ONLY
NAVIGATION_ROOT_CAUSE_STATUS: UNKNOWN
ROLLBACK_MANIFEST_STATUS: UNKNOWN
RESTORATION_SALVAGE_AUDIT_STATUS: NOT_RUN
IN_PLACE_DESTRUCTIVE_ROLLBACK: PROHIBITED
BEHAVIOR_PATCH: BLOCKED
MAXIMUM_NORMAL_SOURCE_CHANGE_CLASSIFICATION: DIAGNOSTICS_ONLY
CLASSIFICATION_IS_NOT_AUTHORIZATION: YES
ACTIVE_DOCUMENTATION_REQUEST_SOURCE_AUTHORIZATION: NO
```

다음 순서는 제안 상태이며 아직 실행 승인을 받거나 수행한 작업이 아니다. 각 source,
rollback, build, external instance 또는 runtime 단계는 active user request가 해당 동작을
별도로 허용해야 한다.

1. repository를 변경하지 않는 strict provenance/restoration audit를 먼저 수행하고,
   결과는 대화 보고서로 제시한다.
2. 문서 작성 전 감사 snapshot의 311개 파일을 사용자 기존 변경, diagnostics,
   behavior, neutral plumbing, tests, docs로 file/hunk 단위 분류한다.
3. known committed baseline, failed hunk, exact inverse hunk와 unrelated-change 보존법을
   확인한다. strict audit 중에는 manifest 파일을 만들지 않는다.
4. exact scope가 입증되고 사용자가 exact method/hunk 또는 expected impact가 명시된
   whole-file removal을 `USER_APPROVED_EXACT_ROLLBACK_UNIT`으로 승인하면 실패한 행동만
   독립 `RECOVERY_ROLLBACK` unit으로 제거할 수 있다. path-only 또는 generic rollback
   승인은 불충분하다. 입증되지 않으면 원복하지 않는다. 이 완료는 failed hunk 제거만
   뜻하며 behavior fix, build 또는 runtime verification을 뜻하지 않는다.
5. audit 종료 뒤 사용자가 별도로 문서화를 요청한 경우에만 rollback manifest를
   저장한다.
6. 새로운 source 조사가 승인됐다면 behavior change 없이 bounded diagnostics만
   추가한다.
7. Minecraft launch, runtime reproduction, live-world execution과 live-world block
   mutation이 각각 승인됐다면 먼저 world/save/cache provenance를 기록한 뒤 같은
   월드, 좌표와 명령으로 별도 reproduction ID를 사용해 재현한다.
8. first failing owner가 확인되지 않으면 bounded log만 보강하고, 승인된 범위에서 다시
   재현한다.
9. 증명된 최초 실패 경계에만 최소 behavior 수정을 검토한다.
10. active request가 exact clean forced build를 명시적으로 승인하고 runtime 관련 각
    동작도 별도로 승인했을 때만 clean forced build, artifact hash, 동일 재현과
    mapping-aware event/counter reconciliation을 검증한다. 일반적인 "검증" 요청만으로
    clean build 권한을 추론하지 않는다.

## 15. 사고 종료 조건

다음 항목이 모두 충족되기 전에는 이 사고를 `CLOSED`로 바꾸지 않는다.

- navigation failure의 exact last successful boundary와 first failing boundary 확인
- triggering state와 retry/wander/path owner 확인
- 문서화된 increment/filter/reset mapping에 따른 owner event와 terminal counter의
  reconciliation 확인
- 첫 controller-tick-owned phase transition이 summary budget 고갈로 누락되지 않음을 확인
- 사용자 변경과 실패한 GOTO 변경의 file/hunk provenance 분류 완료
- exact rollback unit 문서화
- 추정 기반 행동 로직의 제거 또는 evidence-backed 최소 교체
- 동일 재현에서 도착 성공 또는 bounded하고 정확한 조기 실패 확인
- 현재 runtime evidence와 최종 결과 사이의 모순 없음
- 필요한 기계적 gate의 구현 여부를 실제 상태대로 기록

## 16. 책임 기록

이번 실패는 사용자가 로그 위치나 재현 조건을 제공하지 않아서 발생한 것이 아니다.

사용자는 정확한 로그 경로, 로그가 비어 있지 않다는 사실, 코드 수정 없이 로그를
확인하라는 요구, 이미 목표 가까이에 있었다는 조건을 제공했다. 기존
`AGENTS.md`에도 unknown-cause 작업에서는 diagnostics가 행동보다 먼저라는 규칙이
명시돼 있었다.

작업 절차 실패 원인은 제공된 증거와 규칙을 먼저 적용하지 않고 정적 추정을 행동
구현으로 바꾼 데 있다. 이후 exact rollback 범위를 모르는 상태에서 원복을 권고한
것도 같은 증거 선행 원칙 위반이었다. 이 기록은 그 책임을 사용자, 로그, Minecraft
또는 Baritone에 전가하지 않는다. navigation 실패의 기술적 단일 원인은 여전히
`UNKNOWN`이다.

## 17. 관련 문서

- [GOTO vertical navigation and building-material readiness historical ledger](chatclef-goto-vertical-navigation-building-material-readiness-pre-change-contract-2026-09-10.md)
- [Korean GOTO, FIND, and all-command coverage historical contract](chatclef-korean-goto-find-and-all-command-coverage-pre-change-contract-2026-09-09.md)
- [ChatClef / Baritone cache troubleshooting](chatclef-baritone-cache-troubleshooting.md)
- [ChatClef / Carry On integration direction](chatclef-carryon-integration-direction.md)
- [Task lifecycle diagnostics](chatclef-task-lifecycle-diagnostics.md)
- [Fabric ChatClef 1.20.1 build verification](chatclef-fabric-build-verification.md)
