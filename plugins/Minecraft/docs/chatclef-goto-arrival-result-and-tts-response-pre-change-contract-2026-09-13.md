<!-- #20260913_kpopmodder: Record observed GOTO completion feedback, a scoped arrival-result contract, and a separate TTS latency investigation without implementation. -->
<!-- #20260913_kpopmodder: Review failure-result consistency, evidence limits, and separate synthesis/output latency from terminal queue wait. -->

# GOTO 도착 결과와 한국어 종료 응답 연결 계약

Date: 2026-09-13

후속 구현은 [도착·실패 결과 구현 기록](chatclef-goto-arrival-result-implementation-2026-09-13.md)을
참조한다. 아래 문서화 당시의 로그·미구현·검증 상태는 역사적 기록으로 보존한다.
GOTO의 강제 DIAGNOSTICS_ONLY 선행 제한은 사용자가 이후 삭제했으며,
현재 작업 순서는 AGENTS.md의 `GOTO implementation scope and verification`을 따른다.

## 1. 목적과 현재 작업 범위

사용자는 LAVI Chat에 `500 90 -928로 가줘`를 입력한 뒤 실제 도착을 보고했다.
첨부한 Minecraft F3 화면도 `Block: 500 90 -928`을 표시한다. 그러나 LAVI와 TTS는
`좌표 500, 90, -928로 가는 작업은 끝났는데, 도착했는지는 확인하지 못했어`라고 응답했다.

이 문서는 해당 로그의 읽기 전용 검토 결과와 후속 구현 계약을 기록한다.
이번에 LAVI가 받은 결과는 일반 자연 완료였고, 검토한 소스에는 도착 판정값을 확정 문구로
표현하는 연결이 없다. 수신 결과와 출력 문장은 이 소스 흐름에 부합하지만 실행 JAR과
소스의 동일성까지 검증한 것은 아니다. 로그 양을 늘리는 것만으로 현재 소스의 문구 선택이
바뀌지는 않는다. 별도로 관찰한 출발 TTS 지연은 결과 의미 연결과 다른 조사 항목으로 다룬다.

```text
DOCUMENT_TYPE: PRE_CHANGE_CONTRACT_WITH_OBSERVED_RUNTIME_EVIDENCE
ACTIVE_REQUEST: DOCUMENTATION_ONLY
REVIEWED_HEAD: 99af65f2cb0609de31fae24d2f1704aa6853f659
REVIEWED_BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
REVIEWED_WORKTREE: DIRTY_38_FILES_AT_DOCUMENTATION_START
BACKEND_SCOPE: FABRIC_CHATCLEF_1_20_1_ONLY
PROPOSED_CHANGE_CLASSIFICATION: NONE
MAXIMUM_NORMAL_SOURCE_CHANGE_CLASSIFICATION: NONE
INPUT_ACCEPTANCE_EVIDENCE: ONE_LIVE_CHAT_CASE_VERIFIED_RUNTIME
PHYSICAL_ARRIVAL_EVIDENCE: USER_REPORTED_AND_SCREENSHOT_OBSERVED
TASK_ARRIVED_TERMINAL_EVIDENCE: UNKNOWN
RECEIVED_RESULT_EVIDENCE: VERIFIED_RUNTIME_GENERIC_COMPLETION_ONLY
RESPONSE_GAP_EVIDENCE: SOURCE_PROVEN_AND_MATCHING_RUNTIME_TEXT
ACTIVE_ARTIFACT_SHA256: UNKNOWN
ACTIVE_ARTIFACT_TO_REVIEWED_SOURCE_IDENTITY: UNKNOWN
NAVIGATION_ROOT_CAUSE_STATUS: UNKNOWN
ACTUAL_MICROPHONE_STT_ACCEPTANCE: NOT_RUN_IN_REVIEWED_CASE
ARRIVAL_RESULT_RESPONSE_IMPLEMENTATION: NOT_IMPLEMENTED
TTS_LATENCY_ROOT_CAUSE: UNKNOWN
SOURCE_TEST_CONFIGURATION_CHANGES_IN_THIS_TASK: NONE
BUILD_DEPLOYMENT_RUNTIME_EXECUTION_IN_THIS_TASK: NOT_RUN
COMMIT_AND_PUSH_IN_THIS_TASK: NOT_RUN
```

38개 파일은 이번 문서화 시작 전 `git status --porcelain=v1 --untracked-files=all` 기준이다.
그 안의 기존 Python·테스트·문서 변경을 보존한다. 사용자가 먼저 실행한 게임의 기존 로그를
읽은 것이며, 문서화 중 새로운 게임 명령 실행·게임 실행·실험을 수행한 것은 아니다.
원본 로그·스크린샷을 덮어쓰거나 저장소에 복사하지 않는다.

관련 규칙은 [AGENTS.md](../../../AGENTS.md),
[backend 분리](minecraft-backend-separation.md),
[ChatClef/Carry On 통합 방향](chatclef-carryon-integration-direction.md),
[GOTO 사고 기록](chatclef-goto-diagnostics-before-behavior-incident-2026-09-11.md),
[현행 V3.1 계약](chatclef-goto-upward-material-preflight-contract-2026-09-12.md)을 따른다.
이번 응답 불일치의 확인은 과거 navigation 실패 원인이나 모든 GOTO 경로의 성공을 증명하지 않는다.

## 2. 실제 읽은 로그와 증거 한계

실제 인스턴스 디렉터리는 `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01`이다.
감사 로그 이름은 `logs\instance_audit.txt`이며, `instance\_audit.txt`라는 하위 경로가 아니다.
아래 크기와 LastWriteTime은 앞선 읽기 검토 중 한 시점의 관측값이다. 실행 중 파일은 계속 증가할 수 있으므로
불변 파일 snapshot, 고정 해시 또는 현재 크기로 간주하지 않는다.

| 파일 | 관측 크기 | 파일 LastWriteTime(KST) | 사용 범위 |
| --- | ---: | --- | --- |
| 인스턴스 `logs\latest.log` | 1,313,633 B | 2026-09-13 12:55:03 | 명령·root·자연 종료·결과 전송 |
| 인스턴스 `logs\instance_audit.txt` | 2,949,876 B | 2026-09-13 12:45:08 | 실행 시도·인스턴스 구성 |
| 인스턴스 `logs\stdout-logs.txt` | 1,503,232 B | 2026-09-13 12:54:43 | 같은 종료 이벤트의 stdout 기록 |
| `C:\Vtuber_Souorce_Code\LAVI\logs\20260913_124458_log.txt` | 74,831 B | 2026-09-13 12:53:47 | 입력 변환·수신 결과·응답·TTS |

세 Minecraft 로그 모두 내용이 있었다. 파일 하나의 일시적인 0바이트 메타데이터나 오래된
LAVI 빈 로그를 근거로 이번 실행의 로그가 없다고 판단하지 않는다. 감사 로그 590행의
`2026-09-13T03:45:08Z`는 KST 12:45:08의 Minecraft 1.20.1/Fabric 실행 시도이며,
GOTO 도착이나 실행 중 JAR의 SHA-256을 증명하지 않는다.

증거 표기는 다음처럼 구분한다.

- `VERIFIED_RUNTIME`: 실제 읽은 로그에 해당 값·이벤트가 기록되어 있음.
- `SOURCE_PROVEN`: 현재 저장소 소스에서 해당 분기·데이터 흐름을 확인함.
- `USER_REPORTED_AND_SCREENSHOT_OBSERVED`: 사용자 보고와 첨부 F3 화면으로 목표 블록 위치를 확인함.
- `UNKNOWN`: Task의 확정 도착 terminal, 실행 JAR과 현재 소스의 동일성 등 이번 검토로 입증하지 못한 항목.

스크린샷은 파일로 새로 저장하지 않았으며, 대화 첨부 이미지를 근거로 한다.
로그의 `completed`를 스크린샷 대신 도착 증거로 쓰거나, 스크린샷을 같은 root의
`arrived()` 값이 전송됐다는 증거로 쓰지 않는다.

## 3. 이번 Chat 실행의 연결된 기록

```text
INPUT_TEXT: 500 90 -928로 가줘
TARGET_XYZ: 500, 90, -928
SOURCE: lavi_chat_ui
EVENT_ID: 77cb26c14730005184ab2196ff161bbf
REQUEST_ID: lavi-input-ko-e81c45674cec49b2a8ebc4488ee5f834
COMMAND_CORRELATION_ID: lavi-8fa675881e26473684c492959be3b20f
SESSION_ID: fabric-chatclef-85c3a8caf4c64eca905c3439f6cad5db
CONNECTION_GENERATION: 1
BOUND_ROOT: lavi.minecraft.task.movement.gotopreflight.PreparedGotoTask#22ebb1fc
GOTO_OPERATION_ID: UNKNOWN
```

행 번호는 이번 검토 당시 파일 기준이며, 로그 rotation 뒤에도 같은 파일이라고 가정하지 않는다.

| 시각(KST) | 근거 | 관측 내용 |
| --- | --- | --- |
| 12:52:49 | LAVI 135~136행 | `valid_xyz`, `original_xyz_matched`, `x=500 y=90 z=-928` |
| 12:52:49 | latest 1110, 1112행 | `goto 500 90 -928` 접수, 기존 `@goto` dispatch |
| 12:52:49 | LAVI 141~148행 | `좌표 500, 90, -928로 갈게` 생성·TTS 큐 등록·합성 요청 |
| 12:53:01 | LAVI 150, 152행 | 출발 음성 파일 생성, winsound 재생 호출 직전 기록 |
| 12:53:03 | latest 1209, 1213행 / stdout 3430행 | callback과 같은 root의 Task 종료 이벤트; `cancelInvocationId=0`, 자연 종료 hint |
| 12:53:03 | latest 1223, 1226행 / stdout 3460행 | `matching_task_finished`, `termination_kind=finished` |
| 12:53:03 | latest 1224, 1230행 / stdout 3481행 | terminal 전송 시도 1, `terminal_sent=true`, `lifecycle_cleared=true` |
| 12:53:03 | LAVI 173행 | `status=completed`, `ok=True`, `result_reason=matching_task_finished` 수신 |
| 12:53:03 | LAVI 174~178행 | 도착 미확인 문장 생성, UI와 TTS 큐에 전달 |
| 12:53:07 | LAVI 179~183행 | 출발 음성 재생 완료, 종료 문장의 큐 처리·합성 요청 |
| 12:53:11~12:53:19 | LAVI 184~189행 | 종료 음성 생성·재생; `tts_playback delivered=true reason=played` |

LAVI가 받은 결과의 `result_fidelity`는 `callback_plus_matching_user_task_event`다.
Minecraft lifecycle 진단 snapshot의 `result_fidelity=unknown`을 실제 송수신 payload의
값으로 대체하지 않는다. 수신 결과에는 `arrived`나 GOTO의 확정 성공·실패 의미가 없고,
현재 `latest.log`와 `stdout-logs.txt`에서 명시적인 `ARRIVED` 이벤트도 확인되지 않았다.

이번 요청의 시작·종료 응답은 LAVI 181, 189행에 각각 한 번의 재생 완료로 기록된다.
이는 이 요청의 관측이며, 중복 terminal·재전달·STOP 경합에서도 최대 1회라는 검증은 아니다.
텍스트는 TTS 이전에 이미
`도착했는지는 확인하지 못했어`였으며, TTS가 그 판단을 새로 만들었다는 근거는 없다.
음성 녹음을 청취한 검사는 아니므로 `-928` 발음이나 음질이 정상/비정상이라고 판정하지 않는다.

## 4. 현재 소스에서 확인한 응답 원인

| 소유자와 소스 | 확인된 동작 |
| --- | --- |
| [PreparedGotoTask](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/movement/gotopreflight/PreparedGotoTask.java) | `finishArrival()`는 child 정리·binding·실제 목표 조건 확인 뒤 `arrived=true`와 TERMINAL 설정. 실패도 TERMINAL이 되며 `isFinished()`는 TERMINAL 여부를 반환 |
| [FabricChatClefCommandOutcomeClassifier](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandOutcomeClassifier.java) | 같은 root의 종료 이벤트와 callback, STOP 상태를 검사한 뒤 일반 `matching_task_finished` 선택; `arrived()`/`failureReason()`를 GOTO 결과로 읽지 않음 |
| [FabricChatClefCommandResultFactory](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandResultFactory.java) | `completedFromTaskFinished()`에서 일반 자연 완료 결과와 `callback_plus_matching_user_task_event` 생성 |
| [CommandFeedbackResultCoordinator](../fabric/chatclef/transport/command_feedback/lifecycle/result/command_feedback_result_coordinator.py) | 같은 요청의 결과를 조정하고 completed에 대해서만 증거 평가값을 terminal fact에 전달; failed의 GOTO 상세 이유를 검증하는 경로는 없음 |
| [CommandTerminalEvidenceProfileRegistry](../fabric/chatclef/transport/command_feedback/lifecycle/evidence/command_terminal_evidence_profile_registry.py) | `goto`가 `cautious / cautious_terminal`로 등록됨 |
| [CommandTerminalEvidenceEvaluator](../fabric/chatclef/transport/command_feedback/lifecycle/evidence/command_terminal_evidence_evaluator.py) | VERIFIED가 아닌 profile은 `verified=False`; 현행 성공 evaluator는 GET과 STORE_HOME |
| [KoreanCommandTerminalRenderer](../fabric/chatclef/response/command_lifecycle/grammar/boundary/korean_command_terminal_renderer.py) | `completed`의 `movement_goto`에 항상 도착 미확인 문장 선택; GOTO에는 `verified=True`용 도착 문장 분기도 없음. `_failed()`에는 typed 실패 projection을 전달하지 않아 현재 GOTO 실패 표현도 일반 문구임 |

확인된 공백은 **도착 의미의 결과 전달·검증·문장 선택 연결**이다. GOTO 명령 수행 실패를
뜻하지 않으며, 성공/실패 모두 가능한 Task 자연 종료를 확정 도착으로 해석할 수도 없다.
현재 소스의 이 흐름은 이번 수신 결과와 출력 문장에 부합한다. 실행 JAR과 소스의 동일성을
새로 검증한 것은 아니므로 런타임 `arrived()` 내부값까지 소스만으로 확정하지 않는다.

[한국어 입력 연결 계약 5절](chatclef-korean-goto-chat-microphone-input-contract-2026-09-13.md#5-한국어-응답-보존과-결과-연결-제외)은
이 미연결 상태를 알고 있던 제한으로 기록하고 Java 종단 결과 연결을 당시 구현 범위에서 제외했다.
그 과거 범위를 바꾸거나 이미 구현됐다고 고쳐 쓰지 않는다. 이 문서는 별도 후속 작업의 계약이다.

## 5. 후속 도착 결과 연결 계약

### 5.1 적용 경로와 변경하지 않을 동작

첫 후속 구현의 도착 확정 대상은 **차원 전환 없는 direct XYZ GOTO**로 한정한다.
상승 시 선택되는 `PreparedGotoTask`와 같은 높이·하강 시 기존 `GetToBlockTask` 경로를
별도로 검사한다. 모든 XYZ가 Prepared를 거친다고 가정하지 않는다.
XZ-only, Y-only, dimension-only, cross-dimension 및 증거를 제공하지 않는 기존 payload는
기존 동작과 신중한 응답을 유지한다. `goto`라는 이름만으로 모든 형태를 확정 성공으로 승격하지 않는다.

V3.1 native-first 이동, 필요 시 재료 확보, frozen handoff minimum, 기존 목표 조건,
child 정리와 STOP/root/world 우선순위를 보존한다. 길찾기·채굴·배치·재시도·timeout·
Task scheduler·캐시 정책을 응답 개선에 섞지 않는다. Forge/MineMind, 새 `@go` 별칭,
dependency/version 변경이나 공통 runtime 구현도 포함하지 않는다.

### 5.2 결과 생성과 전달

1. 성공 판정의 소유자는 기존 이동 Task다. Prepared에서는 기존 `arrived()`와 typed 실패 이유를
   같은 bound root의 종료와 연결한다. bridge나 Python이 진단 문자열을 파싱해 성공을 만들지 않는다.
2. 성공/실패 의미는 owner가 이미 결정한 값을 불변 결과로 전달한다. Task 종료 자체를 바꾸거나
   `completed`라는 상위 상태를 도착 boolean으로 재사용하지 않는다.
3. 기존 STOP/cancel·실패·binding 무효화의 권한과 우선순위를 보존한다. 세계/플레이어/요청/root가
   바뀌거나 결과가 충돌하면 도착으로 승격하지 않는다. 늦은 성공이 확정된 중지·실패를 덮지 않는다.
4. legacy direct XYZ는 기존 목표 predicate와 실제 종료·정리·binding을 어떻게 증명할지 먼저
   소스에서 확정한다. Prepared 결과를 위조하거나 child `FINISHED`만으로 도착을 표시하지 않는다.
   충분한 권위 있는 증거가 없으면 기존의 미확인 응답을 유지한다.
5. 결과에는 기존 request/correlation/session/connection generation/root 결합을 보존하고,
   원래 XYZ와 차원 의미, Task 소유자가 확정한 outcome·실패 이유·증거 종류를 검증 가능하게 담는다.
   이미 존재하는 필드를 재사용하고, 정확한 DTO/schema 필드명·버전·선택성은 구현 전 검토에서 확정한다.
6. 기존 결과 schema·decoder와 호환되도록 검토한다. 신규 증거 누락·구형 payload·지원하지 않는
   버전·잘못된 타입을 성공으로 해석하지 않는다. 미검증 증거는 신중한 응답으로 처리하고,
   무관한 명령의 결과 의미나 lifecycle 상태 전체를 함께 바꾸지 않는다.

새 결과를 발행하는 Java 소유자는 **상위 status/ok와 GOTO의 확정 의미를 함께 결정**해야 한다.
후속 구현은 아래 매핑을 적용 가능한 GOTO 경로 안에서 갖춘다. 현행 소스가 이미 이 매핑을
구현했다는 뜻이 아니며, Python이 일반 `completed`를 받아 실패나 도착으로 다시 추정하지 않는다.

| Java에서 확인된 조건 | 후속 발행 계약 |
| --- | --- |
| 같은 root의 확정 도착이고 기존 ownership/STOP 검사를 통과 | `completed / ok=true`와 도착 증거; typed 실패 이유 없음 |
| 같은 root의 확정 Task 실패이며 기존 STOP이 소유한 결과가 아님 | `failed / ok=false`와 검증 가능한 typed 실패 이유 |
| 기존 명시적 사용자 STOP이 결과를 소유 | 기존 `cancelled / ok=false`와 중지 이유 보존 |
| 기존에 확정된 그 밖의 실패·거절·기한 초과 | 해당 기존 비성공 status/ok/이유 보존 |
| 새 결과를 확정하기 위한 owner/STOP 상태가 불명확하거나 snapshot 의미가 모순됨 | 기존 분류기의 대기/UNKNOWN 기준을 보존하고, 종단 UNKNOWN이면 `unknown / ok=false`; 이미 확정된 결과를 덮지 않음 |
| 새 GOTO 증거를 지원하지 않는 기존 경로·구형 결과 | 기존 status/ok를 그대로 보존; 일반 `completed`에 도착 의미를 추가하지 않음 |

`error_code`는 기존 DTO/프로토콜 계약을 지키며, Task의 상세 실패 이유를 지원되지 않는
공통 error enum으로 임의 변환하지 않는다. family별 typed 이유와 기존 일반 오류 코드를
구분해 전달한다. 실패까지 `completed`로 보내고 설명문만 바꾸는 연결은 이 계약을 충족하지 않는다.

도착 확정 시점과 결과 전송 시점의 소유권을 구분한다. 정상 완료로 user root가 해제되거나
Idle로 전환된 뒤 결과가 전송될 수 있다. 확정 시점의 world/player/root 세대와 원래 목표에
결합된 불변 terminal snapshot을 사용하고, 이후 자연스러운 root 해제만으로 확정 결과를
실패로 바꾸지 않는다. 실제 외부 교체·STOP으로 무효화된 결과와 구분해야 한다.
TTS 재생이 늦어져도 현재 플레이어 좌표를 재조회해 과거 terminal 결과를 다시 판정하지 않는다.

legacy 경로의 predicate를 Prepared의 정확 좌표·정리 조건과 동일하게 서술하거나 강화하지 않는다.
[GetToBlockTask](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/movement/GetToBlockTask.java)의
`isFinished()`는 상위 goal 판정과 선택 차원을 확인하며,
[CustomBaritoneGoalTask](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/movement/CustomBaritoneGoalTask.java)의
`isFinished()`에는 `cachedGoal` 지연 생성이 있다. 새 결과 관찰 경계에서 이 메서드를
무조건 부수 효과 없는 조회라고 가정하지 않는다. 기존 owner의 결정과 확정 snapshot을 우선 사용한다.

### 5.3 Python 검증과 사용자 응답

전용 GOTO 증거 검증은 허용된 명령 형태, 원래 XYZ/차원, 같은 요청·세션 세대·root,
종단 결과의 의미·충돌·중복 여부를 검사한다. 전역 성공 플래그나 새 shared session 상태를
만들지 않고 기존 명시적 context/DTO/terminal claim 경계를 사용한다.
기존 `CommandResultDTO`의 `status`/`ok` 일관성과 신규 GOTO 결과 의미도 함께 검증한다.
상위 failed/cancelled/unknown과 도착 증거가 충돌하면 도착으로 승격하지 않는다.
반대의 모순인 `completed + typed 실패`, `ARRIVED + 실패 이유 동시 존재`도 검사한다.
같은 요청의 모순된 증거는 확정 도착·상세 실패 설명의 근거로 사용하지 않고 기존 오류/미확인
처리를 따른다. 다른 요청의 결과와 중복 terminal은 기존 거부·중복 방지 경계에서 처리한다.

실패 안내는 성공 검증기의 `verified=True`로 해결하지 않는다. 현재 coordinator는 completed만
평가하고 `_failed()`는 GOTO 상세 이유를 받지 않으므로, 후속 구현에는 **상관관계가 검증된
failed 결과의 typed 이유 검증 → terminal fact의 명시적 실패 projection → 한국어 실패 문장**
경로도 포함한다. 허용된 실패 코드만 해석하며 원격 message나 로그 원문을 그대로 읽어주지 않는다.
상위 failed가 유효하되 상세 이유가 없거나 지원되지 않으면 기존 일반 실패 문구를 유지한다.

`CommandTerminalEvidenceProfileRegistry`의 값만 VERIFIED로 바꾸지 않는다.
검증기, descriptor/profile 전파, 결과 projection, 문장 선택과 테스트를 함께 갖춘 경로만
확정 표현을 허용한다. 기존 specialized STOP, terminal 1회 소유권, UI/TTS 전달 수명을 보존한다.

| 확인된 결과 | 사용자 응답 계약 |
| --- | --- |
| 같은 요청의 확정 도착 | `500, 90, -928 좌표에 도착했어.`처럼 원래 목적지를 포함 |
| 확정 실패 | 실제 typed 이유에 맞는 한국어 실패 안내; 알 수 없는 이유를 추정하지 않음 |
| 사용자 중지 | 기존 중지 응답과 우선순위 보존 |
| 상위 `completed`이며 일반 완료만 수신 / 증거 부족 / 지원하지 않는 경로 | 기존 도착 미확인 안내 유지 |
| 상위 `failed`이며 상세 이유 누락·미지원 | 기존 일반 실패 안내 유지 |
| 다른 요청·세대·root의 결과 / 중복 terminal | 현재 명령의 성공으로 사용하지 않고 기존 거부·중복 방지 처리 |

위 표는 후속 구현의 합격 기준이다. 이번 도착 보고 한 건으로 typed 실패 이유별 안내,
STOP 경합이나 legacy 경로까지 검증됐다고 보지 않는다.

## 6. 동반 진단과 검증 기준

추가 로그는 구현 경계를 관찰하는 용도다. Task가 확정한 결과 → Java 결과 투영 →
Python 증거 수락/거부 → 응답 종류 → UI/TTS 전달을 같은 요청으로 연결한다.
진단은 이미 결정된 값을 관찰하고, 로그 유무·실패·억제가 실행·정리·결과를 결정하지 않는다.
기존 bounded logger를 재사용하며 최초 필수 경계와 terminal을 물리적으로 남길 용량을 보존한다.
정확한 필수 owner/event/전이·사유 서명과 한도는 후속 구현의 GOTO gate에 고정한다.
원문 음성·전체 대화·매 tick 좌표 출력은 추가하지 않는다.

다음은 **아직 실행하지 않은 후속 검증 계획**이다.

| 사례 | 합격 기준 |
| --- | --- |
| Prepared 정상 도착 | 기존 성공 owner와 목표·정리·binding 증거가 일치할 때만 도착 응답 1회 |
| 같은 높이·하강 legacy XYZ | 경로별 권위 있는 도착 증거를 검증; 증거 부족 시 확정 응답 없음 |
| Prepared 실패·재료 부족·handoff 실패 | typed 실패 이유 보존; 일반 Task 종료를 도착으로 잘못 표시하지 않음 |
| 실패 결과의 끝단 연결 | Java `failed/ok=false` → 같은 요청의 typed 이유 검증 → terminal fact → 한국어 사유까지 보존; 미지원 이유는 일반 실패 문구 |
| 상위 상태와 증거 모순 | `completed + typed 실패`, 비성공 status + ARRIVED, ARRIVED + 실패 이유는 확정 문구 근거로 사용하지 않음 |
| STOP 또는 root/world/player 변경 | 기존 우선순위 보존; 늦은 결과가 성공으로 덮지 않음 |
| XZ/Y/차원 이동·구형 결과 | 기존 동작/신중한 응답 유지 |
| 다른 XYZ/차원/request/session generation/root | 도착 증거 수락 0회 |
| 누락·오류 타입·지원하지 않는 증거 버전 | 확정 도착 없음; 예외로 입력·TTS 수명 손상 없음 |
| 같은 terminal 재전달·순서가 바뀐 callback | 같은 요청의 종료 응답/UI/TTS 각각 최대 1회 |
| 로그 OFF·emission 예외·일반 detail 억제 | 같은 실행 결과와 응답; 필수 경계 관측은 별도 검증 |
| 기존 한국어 Chat/최종 음성 fixture 및 다른 명령 | 원문 결합·단일 제출·GET/STORE_HOME/STOP 회귀 보존 |
| 후속 실제 Chat 및 실제 마이크 | 원문/최종 전사 → 같은 요청/목표 → 게임 결과 → 정확한 대사·재생을 개별 검증 |

오프라인 검증은 Java owner/result/호환 계약과 Python 검증기/응답/전달 회귀를 나눠 수행한다.
Java 구현 후 build가 요청 범위에 들어오면
[Fabric build runbook](chatclef-fabric-build-verification.md)의 runtime-root 명령
`.\gradlew.bat clean build --rerun-tasks`를 따른다. incremental 성공만으로 검증하지 않는다.
깨끗한 build 성공, JAR 동일성, 배포, 런타임 로드, 실게임 결과·대사는 각각 다른 증거다.
이번 문서 작업에서는 어느 것도 새로 수행하지 않았다.

## 7. TTS 시간차는 별도 조사 항목

출발 문장의 큐 등록·처리 시작·합성 요청은 모두 12:52:49, 음성 파일 생성과 winsound
재생 호출 직전 기록은 12:53:01, 재생 종료는 12:53:07이었다. 게임 작업은 12:53:03에
종료되어 출발 안내의 재생 처리 구간과 겹쳤다. 로그는 초 단위이므로 정밀한 구간 측정값은 아니다.

| 구간 | 관측 시간차 | 해석 범위 |
| --- | --- | --- |
| 출발 안내 합성 요청(LAVI 148행) → 출력 저장(150행) | 약 12초 | HTTP 요청부터 응답 본문 파일 저장까지; 서버 내부 대기·모델 처리·통신·파일 쓰기 비중은 UNKNOWN |
| 출발 안내 출력 저장(150행) → 재생 호출 직전(152행) | 같은 초 | 오디오 장치의 실제 소리 출력 시점까지 측정한 것은 아님 |
| 종료 안내 큐 등록(178행) → 큐 처리 시작(182행) | 약 4초 | 기존 출발 안내 처리가 끝날 때까지 기다린 구간; 위 출발 합성 12초와 별개 |

[GPTSoVITS API client](../../GPTSoVITS/gpt_sovits_core/gpt_sovits_api_client.py)는 요청 직전에
`Requesting`을 남기고 응답 본문을 파일로 쓴 뒤 `Output saved`를 남긴다.
[WinsoundPlayer](../../../tts_core/winsound_player.py)는 `PlaySound` 호출 전에 재생 안내 로그를 남긴다.
따라서 출발 안내의 약 12초를 LAVI 재생 큐 대기나 장치 재생 지연으로 단정하지 않는다.
세부 원인은 아직 UNKNOWN이며 최초 합성 warm-up을 포함한 서버·통신·저장 경계의 확인이 필요하다.
TTS 서버 준비는 12:46:27, weight 로드는 12:46:27~28로 기록되어 있어
12:52의 지연을 서버 시작 시간으로 바로 설명할 수 없다.

후속 조사는 기존 enqueue → synth request → output → playback begin/end 타임스탬프를
요청별로 연결하고, 빠른 작업 종료와 연속 명령을 비교하는 범위에서 시작한다.
도착 결과 연결과 독립된 검토·변경 단위로 유지하며 이번 문서로 TTS 동작을 바꾸지 않는다.
이미 종료된 요청의 아직 발화하지 않은 출발 안내를 처리하는 정책이 필요하다면,
같은 요청의 큐 소유권·terminal 우선순위·발화 중/합성 중 상태·다른 대사 보존을 먼저 정의한다.
전역 큐 삭제, 재생 중 무조건 끊기, 지연 threshold 변경, warm-up이나 모델/GPU 설정 변경을
이 기록만으로 선택하지 않는다. 음수 좌표 발음 변경도 현재 증거의 범위 밖이다.

## 8. 구현 진입과 변경 단위

현재 요청은 문서화만 승인한다. 소스·테스트·설정 수정, build, 배포, Minecraft 실행,
외부 instance 복사, 런타임 재현, live-world 실행/블록 변경, Task stop, world exit,
process control, 수동 cache 변경, commit, push의 활성 요청 승인값은 모두 `NO`다.
기존 사용자 실행 로그를 읽을 권한과 새 실험을 수행할 권한을 합치지 않는다.

후속 구현 요청에서는 당시 HEAD·staged/unstaged·untracked 변경과 실행 artifact를 다시 대조하고,
AGENTS의 전체 GOTO gate 및 각 action authorization을 보고해야 한다. 이 문서의 결과 의미
연결 계획은 material-preflight 예외나 과거 navigation 원인 증명을 대신하지 않는다.
증거가 부족한 새로운 경계는 해당 gate의 진단 단계로 다루고, 역사적 navigation 실패를
재현·수정할 범위로 넓히지 않는다.

이번 기록은 일반 완료 결과와 도착 미확인 문구의 연결을 확인한 것이다. 실행 JAR과
검토 소스의 동일성, Task의 확정 ARRIVED terminal 및 필수 owner 경계의 방출 증거는
아직 UNKNOWN이므로, 이 문서를 결과 의미를 바꾸는 BEHAVIOR gate 통과 근거로 사용하지 않는다.
후속 요청에서 해당 소스·artifact·런타임 증거를 대조하고, gate에 필요한 증거가 부족하면
승인된 범위의 독립적인 DIAGNOSTICS_ONLY 단위로 그 경계를 확인한다.

구현의 제안 변경 단위는 다음과 같다. 정확한 파일/hunk와 독립적인 역패치는 소스 수정 전에
당시 상태를 기준으로 고정한다. 아래 목록은 적용 가능한 rollback이나 삭제 승인이 아니다.

- 도착 결과 단위: 기존 Task 성공/실패 조회 경계, Fabric status/ok·typed 결과 분류와 payload,
  GOTO 전용 Python 성공·실패 증거 검증 및 descriptor/profile/fact 전파,
  한국어 도착·실패 문장, 해당 계약 테스트·동반 진단.
- TTS 단위: 지연 원인 확인과 필요시 같은 요청의 출발 안내 수명 정책. 도착 결과 단위와 분리.
- 문서화 단위: 이 새 계약, 기존 한국어 입력 계약의 후속 확인 절, Minecraft README 연결만.

원래 이동 엔진의 판정·cleanup을 바꾸지 않고 기존 결과를 외부에 정확히 전달하는 것이
우선 목표다. 추가 경계가 필요한지는 실제 소유자와 현재 증거로 판단하며,
기존 사용자 변경에 대한 broad restore/reset/checkout/clean 또는 원본 로그 제거를 하지 않는다.
