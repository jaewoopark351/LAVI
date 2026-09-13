<!-- #20260913_kpopmodder: Track the requested GOTO terminal-result implementation, ownership, and separate verification results. -->

# GOTO 도착·실패 결과와 한국어 대사 연결 구현

Date: 2026-09-13
Status: IMPLEMENTED_AND_REPOSITORY_VERIFIED; LIVE_RUNTIME_NOT_RUN

## 요청과 기준

이번 요청은 이동 Task의 확정 결과를 Java → LAVI → Chat/TTS 대사로 연결하는
구현과 저장소 내부 검증이다. TTS 지연 개선은 별도 작업으로 유지한다.
사용자가 제거한 GOTO 진단 선행 제한 대신 [AGENTS.md](../../../AGENTS.md)의
`GOTO implementation scope and verification`을 적용한다. 연결 문서의 이전
DIAGNOSTICS_ONLY 선행·런타임 원인 입증 조건은 이번 구현의 선행 조건이 아니다.
기존 사고 증거와 당시 검증 상태는 수정하지 않는다.

기준 HEAD: `99af65f2cb0609de31fae24d2f1704aa6853f659`.
현재 작업 트리의 한국어 입력·진단·문서 및 AGENTS 변경을 보존한다.
V3.1 길찾기, 재료 확보, frozen handoff minimum, STOP 및 기존 정리를 유지한다.
Fabric ChatClef 1.20.1 동일 차원 direct XYZ만 새 결과의 대상이다.
Prepared 경로 선택은 현재 V3.1의 기존 조건을 따른다. 목표 Y만으로 경로를
새로 선택하지 않고, 실제 생성된 Prepared 또는 일반 XYZ Task의 결과를 연결한다.

2026-09-13 14:07:23의 `500 80 -950` 실행에서 `PreparedGotoTask#64cddc1a`는
ARRIVED를 확정했다. 동일 요청의 Java 진단은 `owner_arrived=true`와
`goal_satisfied=UNAVAILABLE`을 기록했고, Python은 `profile_not_verified`로
미확인 문장을 생성했다. 이는 이번 결과 전달 누락의 증거이며, 과거 일반
navigation 실패의 원인이나 새 구현의 런타임 검증은 아니다.

## 소유권과 독립 변경 단위

- Java Task: `task/movement/gotoresult/`의 불변 binding/outcome과 legacy 관찰
  Task를 분리한다. Prepared의 기존 도착·실패 확정 순간에 snapshot을 저장한다.
  legacy는 기존 `super.isFinished()`의 결과를 관찰하고 기존 자연 종료 이벤트에서
  정리 완료를 결합한다. bridge에서 `isFinished()`를 새로 호출하지 않는다.
- Java bridge: execution의 원래 Task 결합, dispatch 후 기존 `dispatch_started`
  running 결과에 포함한 binding, 기존 자연 종료 결과의 GOTO projection을 각각
  소유한다. running은 추가 전송 없이 기존 한 개와 sequence=1을 유지한다. `GotoCommand`,
  `PreparedGotoTask`, `FabricChatClefCommandTerminationObservation`, execution
  state/result factory와 dispatch coordinator의 해당 경계만 연결한다.
- Python binding: `fabric/chatclef/result/goto/`의 immutable DTO, decoder,
  context matcher와 lifecycle binding 전용 collaborator. 같은 요청의 running
  결과에서 Task/operation/목적지를 한 번 보존한다.
- Python evidence/response: GOTO 검증기, 성공 projection, 별도 실패 projection,
  허용된 실패 이유의 한국어 renderer를 분리한다. 기존 terminal claim과 UI/TTS
  전달 소유권을 사용한다. 일반 완료·구형·잘못된 증거는 기존 신중한 응답을 유지한다.
- 검증: Java owner/result tests, Python binding/evidence/response/lifecycle tests,
  `test/test_Isolation/minecraft/goto_arrival_result/`의 역할별 PowerShell 검증 도구.

정확한 역변경 단위는 이번에 추가한 파일과 위 owner 경계의 신규 hunk만이다.
이전 dirty hunk, 기존 진단 및 한국어 입력 구현은 이 단위에 포함하지 않는다.
이 문서는 원복 실행이나 기존 파일 삭제 승인이 아니다. 엔진 TaskRunner,
UserTaskChain, Baritone 및 재료 확보 알고리즘은 수정 대상이 아니다.

## 구현 전 범위·증거 기록

```text
GOTO_INCIDENT_RECORD_READ: YES
REPRODUCTION_ID: 20260913-140723-goto-500-80-minus950
ACTIVE_ARTIFACT_SHA256: 4B9CF0F9477B9899C7BC7D429CB195B474DF0A89AD6201AAE8BFD04313736887
COMMAND_CORRELATION_ID: lavi-07bd1d072ef54f809097765056618f16
OPERATION_ID: 64cddc1a
NAVIGATION_ROOT_CAUSE_STATUS: UNKNOWN
PARENT_TASK: PreparedGotoTask#64cddc1a
CHILD_TASK: GetToBlockTask#6faf102
FINITE_COUNTER_OWNER: UNKNOWN
TERMINAL_DECISION_OWNER: PreparedGotoTask.finishArrival/terminate; existing command outcome classifier
DIAGNOSTIC_BUDGET_OWNER: GotoPreparationLogBudget; existing terminal observer
REQUIRED_CAUSAL_OWNER_BOUNDARY_SET: PreparedGotoTask/ARRIVED; command terminal dispatcher/completed projection; Python/evidence_decided; Python/response_rendered
REQUIRED_CAUSAL_OWNER_BOUNDARY_EMISSION_STATUS: VERIFIED_RUNTIME
REQUIRED_TERMINAL_EMISSION_STATUS: VERIFIED_RUNTIME
FIRST_REQUIRED_PHASE_TRANSITION_EMISSION_STATUS: VERIFIED_RUNTIME
DIAGNOSTIC_SUPPRESSION_REASON: No suppression of the listed observed boundaries in this case
PATH_OR_GOAL_OWNER: Existing GetToBlockTask/Baritone
LAST_SUCCESSFUL_BOUNDARY: PreparedGotoTask ARRIVED commit and matching root natural completion
FIRST_FAILING_BOUNDARY: Existing Java generic terminal payload omits the stored GOTO outcome
TRIGGERING_STATE: Same-dimension XYZ task arrived; result remains generic completed
EXPECTED_TRANSITION: Owner result -> correlated typed payload -> verified Korean terminal response
OBSERVED_TRANSITION: Owner ARRIVED -> generic completed -> cautious Korean terminal response
RETRY_OWNER: UNKNOWN
WANDER_OWNER: UNKNOWN
EXACT_FILES_AND_HUNKS_PROPOSED: Result packages and existing integration boundaries listed in the implementation inventory below
EXACT_PROPOSED_FIX_BOUNDARY: Task outcome snapshot, Java result projection, Python validation and terminal rendering
FAILED_IMPLEMENTATION_ROLLBACK_SCOPE: NONE
PROPOSED_CHANGE_EXACT_ROLLBACK_UNIT: New result/binding/evidence/renderer/test files and their minimal owner integration hunks only
USER_APPROVED_EXACT_ROLLBACK_UNIT: NONE
PROPOSED_CHANGE_CLASSIFICATION: BEHAVIOR
ACTIVE_WORLD_IDENTITY: NOT_APPLICABLE
ACTIVE_WORLD_SAVE_PATH: NOT_APPLICABLE
WORLD_COPY_RESTORE_REPLACE_RENAME_HISTORY: NOT_APPLICABLE
BARITONE_CACHE_EVIDENCE: NOT_APPLICABLE
CACHE_TARGET_ABSOLUTE_PATH: NOT_APPLICABLE
CACHE_ACTION: NONE
TASK_STOPPED: NOT_APPLICABLE
TASK_STOPPED_EVIDENCE_STATUS: NOT_APPLICABLE
WORLD_EXITED: NOT_APPLICABLE
WORLD_EXITED_EVIDENCE_STATUS: NOT_APPLICABLE
MINECRAFT_PROCESS_CLOSED: NOT_APPLICABLE
MINECRAFT_PROCESS_CLOSED_EVIDENCE_STATUS: NOT_APPLICABLE
NOT_APPLICABLE_REASON: This unit performs repository source/tests/build work; no game/world/cache/process manipulation
ACTIVE_REQUEST_AUTHORIZES_SOURCE_EDIT: YES
ACTIVE_REQUEST_AUTHORIZES_RECOVERY_ROLLBACK: NO
ACTIVE_REQUEST_AUTHORIZES_BUILD: YES
ACTIVE_REQUEST_AUTHORIZES_DEPLOYMENT: NO
ACTIVE_REQUEST_AUTHORIZES_MINECRAFT_LAUNCH: NO
ACTIVE_REQUEST_AUTHORIZES_EXTERNAL_INSTANCE_COPY: NO
ACTIVE_REQUEST_AUTHORIZES_RUNTIME_REPRODUCTION: NO
ACTIVE_REQUEST_AUTHORIZES_LIVE_WORLD_EXECUTION: NO
ACTIVE_REQUEST_AUTHORIZES_LIVE_WORLD_BLOCK_MUTATION: NO
ACTIVE_REQUEST_AUTHORIZES_TASK_STOP: NO
ACTIVE_REQUEST_AUTHORIZES_WORLD_EXIT: NO
ACTIVE_REQUEST_AUTHORIZES_MINECRAFT_PROCESS_CONTROL: NO
ACTIVE_REQUEST_AUTHORIZES_MANUAL_CACHE_MUTATION: NO
ACTIVE_REQUEST_AUTHORIZES_COMMIT: NO
ACTIVE_REQUEST_AUTHORIZES_PUSH: NO
```

위 runtime 상태는 기존 사용자 실행의 열거된 도착/결과 경계에만 해당한다.
새 native 경로의 snapshot, 신규 wire 결과와 새 한국어 응답의 runtime 상태는
아직 `NOT_RUN`이다. 원인 미확인 항목을 새 구현의 성공 증거로 사용하지 않는다.

## Wire 계약

기존 `CommandResultDTO.data`에 선택적 versioned GOTO evidence를 추가한다.
`goto_profile_id=fabric_chatclef_goto_terminal`, `goto_profile_version=1`.
`goto_binding`에는 request/correlation/session 양쪽 generation, Task와 operation,
원래 XYZ, nullable 요청 차원과 admission world dimension을 보존한다.
`goto_terminal`은 같은 binding 및 outcome, typed failure reason, goal_satisfied,
binding_valid, children_quiescent, evidence_kind, terminal_dimension을 담는다.
새 enum이나 공통 transport 구현을 만들지 않는다.

최초 `dispatch_started` running 결과 한 개에 binding을 실어 보낸다. 지원 대상
XYZ는 기존 Task admission이 끝난 뒤 이 결과를 보내며, 추가 running 메시지는
만들지 않는다. Python은 이때의 binding을 불변 context에 보존하고, terminal의
요청·세션·양쪽 연결 generation·Task·operation·목적지를 모두 비교한다.
중간에 서로 다른 binding이 들어오면 그 요청은 이후 결과로 신뢰를 복구하지 않는다.

raw XYZ는 기존 입력 parser가 인정한 일반형과 외부 괄호 한 쌍으로 감싼 형식을
포함한다. 명시한 차원은 현재 차원과 같아야 한다. XZ, Y-only, 차원-only,
차원 전환은 새 도착 evidence의 적용 대상이 아니다.

성공은 `completed/ok=true`, Task 실패는 `failed/ok=false`로 전달한다.
실패 projection은 success `verified`와 별도 채널이다. STOP, root 교체와 기존
non-success 결과의 우선순위는 유지한다. 채팅과 TTS는 같은 renderer 결과를 받는다.

## 구현 파일과 기존 연결 지점

아래 경로는 `plugins/Minecraft/`를 기준으로 한다. 각 폴더는 결과 모델, Task 관찰,
명령 결합, 검증, 문장 생성의 책임을 구분한다.

| 역할 | 새 구현 위치 |
| --- | --- |
| Java Task binding, immutable outcome, native completion 관찰 | `runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/movement/gotoresult/{binding,model,tracking}/` |
| Java 명령 결합, 최초 결과 순서, payload와 terminal projection | `runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/result/gotoresult/` |
| Python 불변 DTO, strict decoder, 원래 명령 matcher | `fabric/chatclef/result/goto/` |
| Python 최초 Task binding 소유권 | `fabric/chatclef/transport/command_feedback/lifecycle/binding/goto/` |
| Python terminal 검증 | `fabric/chatclef/transport/command_feedback/lifecycle/evidence/goto/` |
| 한국어 도착·실패 문장 | `fabric/chatclef/response/command_lifecycle/terminal/goto/` |

기존 Java 수정 지점은 `GotoCommand.call`, `PreparedGotoTask`의 결과 확정 메서드,
`FabricChatClefOrdinaryCommandDispatchCoordinator`,
`FabricChatClefCommandExecution`, `FabricChatClefCommandExecutionState`,
`FabricChatClefCommandResultFactory`, `FabricChatClefCommandTerminationObservation`의
결과 연결 hunk이다. 기존 `GotoTerminalDiagnosticProjector`는 새 payload의 확정
goal과 operation을 읽도록 보완했다.

기존 Python 수정 지점은 lifecycle facade, context, result state/coordinator,
terminal fact, evidence evaluation/registry/dispatch와 raw descriptor builder이다.
응답은 기존 lifecycle response → phrase → terminal renderer를 통해 연결한다.
진단 record/projector에는 실패 projection 여부와 GOTO 검증 이유를 추가했다.
기존 서버 구성, 한국어 input grammar, navigation/acquisition 구현의 선행 변경은
이 결과 연결 단위의 원복 대상으로 포함하지 않는다.

검증 코드는 runtime `src/test/java/`의 두 `gotoresult/` package,
저장소 `tests/minecraft_chatclef/command_lifecycle/{goto_binding,goto_terminal_result}/`,
기존 descriptor/registry 계약 검사에 둔다. 이전 진단 검사도 함께 실행한다.
빌드·Java 검증 스크립트와 증거는 저장소의
`test/test_Isolation/minecraft/goto_arrival_result/` 아래에 둔다.

## 실게임 확인 절차와 기대 응답

새 JAR 배포·재시작 후의 실게임 검증은 아직 실행하지 않았다. 테스트할 때는
아래 각 요청의 request ID, Task identity, operation ID가 running과 terminal에서
같은지 로그와 대사를 함께 확인한다. 단순 위치 스크린샷만으로 새 결과 연결의
성공을 판정하지 않는다.

| 경우 | 기대 결과 |
| --- | --- |
| 동일 차원 XYZ 도착 — Prepared 경로와 일반 native 경로 각각 | `completed/ok=true`, `ARRIVED`; “500, 80, -950 좌표에 도착했어.” |
| Prepared가 확정한 실패 | `failed/ok=false`, 알려진 실패 이유에 맞는 한국어 안내 |
| 이동 중 기존 STOP 또는 root 교체 | 기존 취소·실패·미확인 판정 유지; 뒤늦은 도착 안내 없음 |
| 구형 또는 binding 없는 일반 완료 | 기존 “도착했는지는 확인하지 못했어” 안내 |
| 다른 좌표·세션·Task 결과, 중복 또는 지연 terminal | 다른 요청에 결합하지 않고 종료 응답 중복 없음 |

Chat과 최종 음성 입력 경로는 자동 검증에서 실제 lifecycle publication과
UI queue/TTS listener까지 연결했다. 이는 실제 마이크 인식, 음성 합성 서버,
스피커 재생 또는 Minecraft 실행을 검증한 것은 아니다.

## 검증 결과

- Java focused tests: PASS — 36 Jupiter tests; failed/aborted/skipped/containerFailures=0
- Python focused and regression tests: PASS — 473 tests, 11,198 subtests
- 1.20.1 clean forced remap: PASS — build run `20260913-150742`, exit 0
- New artifact identity: VERIFIED_LOCAL — SHA256 and class identity below
- Canonical multi-version build / Gradle test task: NOT_RUN
- Deployment / live Chat / actual microphone: NOT_RUN
- TTS latency improvement: SEPARATE_STAGE

1.20.1만 빌드하라는 기존 요청을 유지했다. 실행한 주요 Gradle 명령은
`:1.20.1:clean :1.20.1:remapJar --rerun-tasks --no-build-cache`이며, Gradle의
정리 대상은 해당 runtime `versions/1.20.1/build` 생성 출력이다.
전체 multi-version build 성공이나 Minecraft 런타임 성공으로 보고하지 않는다.

새 PowerShell 창에서 저장소의 `Invoke-1201CleanBuild.ps1`을 실행했다.
최종 실행 시각은 2026-09-13 15:07:42–15:10:38 KST이다. 기존 로컬 JDK
21.0.12로 실행했고, 새 결과 구현의 JAR class 15개는 모두 Java 17 bytecode
(major version 61)이다. 버전별 preprocess 연결은 실행되지만 다른 Minecraft
버전의 Java 컴파일/JAR 빌드를 요청하지 않았다.

최종 산출물:

```text
Path: plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar
Bytes: 8487568
SHA256: 4492EAA802CF900DCC2B5CC3F1817658691ACAC1E34D99F23A893B35A7202872
Build result: test/test_Isolation/minecraft/goto_arrival_result/runs/20260913-150742/result.json
Java test result: test/test_Isolation/minecraft/goto_arrival_result/runs/20260913-151143/result.json
Class identity: test/test_Isolation/minecraft/goto_arrival_result/runs/20260913-151143/fresh-main-verification.json
```

Java 검사는 `verify_java_result.ps1`에서 `javac --release 17`과 기존 Jupiter
runner를 사용해 새 1.20.1 main classes에 연결했다. 기존 진단 19개와 새 결과
17개 검사이며, native 완료 조건 32개 조합과 실패 이유 27개 순회도 포함한다.
검사 전후 main class 2,107개의 해시, JAR 해시, 빌드에 사용한 runtime 소스
해시가 동일함을 확인했다. Gradle 자체의 `test` task를 실행한 결과로 보고하지 않는다.

최종 통과 전 `20260913-145200` 빌드는 제거된 추가 running 메시지 구현의
request ID 접근자 오류로 실패했다. `20260913-145748` 빌드는 그 수정 후
통과했지만 괄호형 XYZ 보완 전 산출물이므로 최종 산출물로 사용하지 않는다.
앞선 실행 기록은 보존하며, 위 최종 build/test run만 완료 증거로 사용한다.

Python 최종 회귀 명령:

```powershell
.\venv\Scripts\python.exe -B -m pytest -p no:cacheprovider tests/minecraft_chatclef/command_lifecycle tests/minecraft_chatclef/lavi_input tests/minecraft_chatclef/structure tests/minecraft_chatclef/crafting_feedback tests/test_minecraft_chatclef_natural_language_service.py tests/test_minecraft_chatclef_korean_rule_parser.py -q
```

검사 범위에는 도착, 27개 알려진 실패 이유, 기존 일반 실패/완료, STOP,
좌표·Task·세션·generation 불일치, 중복·지연 전달, 잘못된 payload,
Chat/최종 음성 입력, 다른 명령의 lifecycle·응답 회귀가 포함된다.
