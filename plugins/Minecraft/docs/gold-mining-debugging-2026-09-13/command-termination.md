<!-- 20260913_kpopmodder: Document the planned bridge terminal handoff for an observed user-root replacement. -->
# root 교체 시 명령 종료 연결

상태: `IMPLEMENTED_PENDING_RUNTIME`. 후속 요청의 소스·자동 검증 결과와 확정 reason은 [구현 기록](implementation.md)을 따른다.
배포·Minecraft 재현은 `NOT_RUN`이다. 아래는 최초 설계이며 기존 실행 로그·소스 검토와 새 검증 결과를 구분한다.

[설계 목차](README.md)와 [실행 근거](evidence.md)를 함께 읽는다.

## 목적과 범위

명령에 결합된 실제 user root가 교체됐을 때, bridge가 더 이상 오지 않을 정상 완료 이벤트를 계속 기다리지 않게 한다.
금 채굴의 도구 반복과 보호 좌표 NPE는 별도 수정 단위다. root 교체를 금 수집 성공으로 처리하지 않는다.
대상은 Fabric ChatClef 1.20.1의 기존 명령 lifecycle이다. 새 전송 경로나 엔진 스케줄러는 만들지 않는다.
upstream의 `UserTaskChain`·`SingleTaskChain` 변경은 필요한 최소 경계로 한정하고, 먼저 bridge 내부 연결로 해결 가능한지 검토한다.

## 기존 실행에서 확인한 사실

실행 자료는 `logs/chatgpt_handoff/gold_mining_20260913_223205/ChatGPT_full_evidence_final.zip`의 현재 세션 사본이다.
아래 줄 번호는 그 패키지의 `latest.log`와 `normalized_utf8/latest.log`에 대응한다.

- 14652~14656줄, 22:26:21: `originIdleCommand=idle`, `NON_IDLE_COMMAND`, 새 `IdleTask` 할당이 기록됐다.
- 기존 gold root `2771e44f`가 새 root `25f3bd37`로 교체됐다. 명령을 입력한 UI나 주체는 단정하지 않는다.
- 14654줄: 중단 관측의 준비 재진입 누계는 1,216회, `rawGoldSignedQuantityChange=0`이다.
- 14655줄: 진단 scope의 `RESOURCE_OBSERVATION_TERMINAL / USER_ROOT_REPLACED`가 출력됐다.
- 이후 14702줄, 22:27:52: 결합된 기존 root는 stopped지만 `task_finished_event_received=false`, `finish_callback_received=false`로 대기했다.
- 22:27:51~53 저장·서버 종료와 stdout의 종료 코드 0은 후반 프로세스 종료 기록이다. gold 명령 성공의 근거는 아니다.

진단 scope 종료는 관측 수명을 닫은 사건이다. bridge 명령 결과의 확정·전송과 동일하지 않으며 행동 신호로 재사용하지 않는다.

## 현재 소스의 연결

다음 경로의 기준은 `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/`다.

| 파일·메서드 | 현재 동작과 검토 이유 |
| --- | --- |
| `adris/altoclef/chains/UserTaskChain.java:111`, `runTask` | 129줄에서 `currentOnFinish`를 새 값으로 바꾼 뒤 136줄에서 `setTask`를 호출한다. |
| `adris/altoclef/chains/SingleTaskChain.java:85`, `setTask` | 실제 변경 분기에서 기존 `mainTask.stop(task)` 후 새 root로 교체한다. 이 경로는 옛 root의 `onTaskFinish`를 호출하지 않는다. |
| `adris/altoclef/chains/UserTaskChain.java:155`, `onTaskFinish` | 콜백 호출과 `TaskFinishedEvent` 발행은 별도 완료 경로에 있다. |
| `lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandOutcomeClassifier.java:34` | 명령 소유 root는 완료 관측과 콜백이 없으면 대기한다. 단순 stopped 상태로 이 대기를 해소하지 않는다. |
| `lavi/minecraft/fabric/chatclef/bridge/command/diagnostics/FabricChatClefTaskStateReader.java:16` | `currentTaskOrNull`은 조회 예외도 null로 반환한다. 이것만으로 실제 root 부재를 판단할 수 없다. |
| `lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/observation/FabricChatClefTaskTerminationObservationDrainer.java:9`, `drain` | 한 tick에 최대 32개 관측만 처리한다. 종료 평가 전에 drain을 호출해도 해당 명령의 완료 관측이 큐에 남을 수 있다. |
| `lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/observation/FabricChatClefTaskTerminationObservationHandler.java:42`, `handle` | dequeue 시점의 current execution을 선택하고, 61줄에서 root 일치 여부를 확인하기 전에 관측을 저장한다. |
| `lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandExecutionState.java:166`, `markTaskFinishedObservation` | 기존 관측과 terminal Task snapshot을 새 관측으로 덮어쓴다. 이벤트 결합을 검증하는 경계가 필요하다. |

`SingleTaskChain.onInterrupt`의 방어·보관 선점은 실제 user root 교체와 구분한다.
새 Task를 전달했어도 `setTask`의 동등성 검사로 기존 root가 유지될 수 있으므로, 전달 인자나 로그 이름만 보고 교체로 판정하지 않는다.
다만 `currentOnFinish`는 이 검사보다 먼저 바뀐다. **같은 root 유지와 완료 callback 소유권 유지는 같은 조건이 아니다.** 같은·동등 Task 재할당은 실제 root 교체와 구별하는 호환성 검증 대상이며, 기존 완료 callback의 보존 또는 유효한 완료 관측 연결을 별도로 확인해야 한다. callback만 바뀌는 경로와 늦은 이벤트의 잘못된 결합은 소스에서 확인한 추가 위험 경계이며, 위 실행에서 실제 발생한 원인으로 단정하지 않는다.

## 제안하는 종료 관측 계약

1. dispatch가 반환됐고 명령 소유 root가 결합된 현재 execution에만 적용한다.
2. 클라이언트 lifecycle tick에서 실제 `UserTaskChain.getCurrentTask()`의 조회 결과를 확보한다.
3. 조회 성공 여부, 엔진·체인 존재, 실제 root 존재, root 객체 식별을 분리한다. 해당 execution의 엔진·체인·월드 수명과 기존 연결 generation이 유효한지도 검증한다.
4. 결합된 옛 root와 현재 root의 관계를 확인한다. 선택된 체인이나 leaf Task의 변경은 종료 근거가 아니다.
5. 같은 요청·세션·현재 execution에 유효한 교체 관측을 불변 값으로 보존하고 기존 terminal 평가에 전달한다.
6. 기존 정상 완료 관측 및 이미 확정된 결과를 존중하고, 확정된 결과를 늦은 관측으로 덮어쓰지 않는다.

`currentTaskOrNull()==null`, 비활성 체인, 문자열 identity만으로 교체·제거를 확정하지 않는다.
조회 실패, mod/체인 부재, 조회에 성공한 실제 root 부재는 서로 다른 상태다. 월드 종료·연결 해제도 해당 수명의 기존 경로와 대조한다.
현재 `ownershipEvidence()`의 조회 성공·실패와 root 참조 구분은 활용하되, 엔진 부재·체인 부재·정상 조회에서의 root 부재·조회 실패를 반드시 구별한다. 현재 조회 구현이 mod/체인 부재를 null root로 합칠 수 있으므로 그대로 부재의 근거로 사용하지 않는다. 월드·엔진·연결 수명 변경을 같은 수명 안의 root 교체로 판정하지 않는다.
실제 root 제거를 지원할 경우에도 동일한 확인 조건을 적용하며, 이번 IdleTask 교체 사례와 근거를 구분한다.

## 기존 lifecycle와 결과 소유권 유지

현재 `FabricChatClefCommandLifecycleTickCoordinator.onEndClientTick`의 순서를 유지한다.

```text
결과 전송 완료 처리 → active context 동기화 → TaskFinished 관측 drain → 현재 execution 종료 평가
```

교체 확인은 이 순서 안의 종료 평가 전 단계에 두고, 정상 완료 뒤 자동 Idle 전환을 교체 실패로 오인하지 않도록 한다.
단, **tick당 최대 32개 drain의 호출 순서만으로 이 조건이 충족되지는 않는다.** 교체 관측 시점까지 이미 접수된 해당 실행의 완료 근거가 아직 처리되지 않았다면 교체 결과 확정을 보류한다. 그 시점까지의 유한한 관측 구간을 구별할 수 있는 순서 정보 또는 동등한 완료 소유자 경계를 사용하고, 해당 구간 처리 후 유효한 정상 완료 근거와 교체 근거를 평가한다. 이후 들어오는 무관한 이벤트가 보류 구간을 계속 늘려서는 안 된다. 한 tick의 무제한 drain이나 임의 timeout으로 결과를 강제 확정하는 방식을 계약으로 삼지 않는다.

이벤트는 **execution 상태에 저장하기 전에** 결합된 root·실행·세션 수명에 맞는지 검증한다. dequeue 당시 current execution이라는 이유만으로 결합하지 않는다. 접수 경계의 소유권 정보와 기존 결합 근거를 대조하고, 옛 root의 지연 이벤트나 소유권이 불명확한 관측은 새 실행의 상태·이미 확보한 유효 완료 관측을 덮어쓰지 못하게 한다. 같은 root 객체가 재사용된 경우도 root 참조 하나만으로 서로 다른 명령 수명을 합치지 않는다.

`FabricChatClefCommandTerminalEvaluator`의 `executionStore.current() == execution` 확인을 유지한다.
기존 `TerminalResultDispatcher → ResultOutbox → context`의 단일 terminal 확정과 재전송·중복 방지를 재사용한다.
단일 확정은 통신 시도가 한 번이라는 뜻이 아니다. 전송 결과가 불명확하거나 실패하면 기존 정책에 따라 같은 식별자·확정 payload로 재시도하고, LAVI의 논리적 종료 처리와 Chat/TTS 종료 효과가 중복되지 않는지 검증한다.
전송 완료 후 해당 execution만 해제하는 기존 `clearIfCurrent` 경로를 보존한다.
정상 완료 callback이나 `TaskFinishedEvent`를 가짜로 호출하지 않으며, 새 명령의 root를 중단하거나 콜백을 교체하지 않는다.
같은·동등 Task 재할당의 callback 소유권 규칙은 이 경계 검토에 포함한다. 실제 root가 유지됐다는 이유만으로 완료 전달까지 보장됐다고 판단하지 않고, 소실된 callback만 영구히 기다리지 않는지 확인한다. callback 변경 자체를 정상 완료·실제 root 교체로 간주하는 우회는 허용하지 않는다.

## 결과 의미와 구현 시 확정할 항목

| 상황 | 결과 계약 |
| --- | --- |
| 기존 정상 완료 근거 충족 | 기존 정상 완료 경로를 유지한다. |
| 명시적 사용자 취소 근거가 결합됨 | 기존 사용자 취소 계약에 따라 `CANCELLED`를 사용한다. |
| 명확한 root 교체, 명시적 취소 근거 없음 | 성공으로 보내지 않는다. 기존 비성공 상태 중 어떤 상태·reason으로 매핑할지 구현 시 프로토콜과 수신 측을 대조해 확정한다. |
| 조회 실패·체인 부재·소유권 불명 | root 교체로 추정하지 않는다. 기존 불명·연결 수명 처리와 구분한다. |
| 방어·자동보관 선점, 같은 user root 유지 | 이 관측 때문에 원래 명령을 종료하지 않는다. |

단순 교체를 `CANCELLED`나 `COMPLETED`로 일괄 매핑하는 것은 합의된 계약이 아니다.
교체를 확인한 시점·옛 root·현재 root·요청·세션을 보존하고, 늦은 callback과 event는 기존 실행만 참조하게 한다.

## 책임 분리와 최소 수정 후보

- **조회:** `TaskStateReader` 인접의 focused 코드가 성공·실패·부재·root 참조를 표현한다.
- **교체 판정:** lifecycle 인접의 별도 순수 판정이 결합 root와 현재 root의 유효한 관계를 판단한다.
- **완료 관측 결합·처리 순서:** observation 인접 코드에서 저장 전 소유권 검증과 교체 시점까지의 미처리 완료 관측을 구분한다. 큐 처리 한도는 유지하며 진단 카운터를 소유권·완료 근거로 재사용하지 않는다.
- **실행별 보존:** `CommandExecution`과 그 state가 해당 요청의 교체 관측 수명을 소유한다. 진단 registry나 공유 static에 맡기지 않는다.
- **종료 연결:** `LifecycleTickCoordinator`·`TerminalEvaluator`·`OutcomeClassifier`의 최소 연결과 기존 결과 생성 경로를 사용한다.

새 코드가 독립된 책임을 둘 이상 맡으면 파일과 의미 있는 하위 폴더를 나눈다. upstream 엔진 전체를 이동·분할하지 않는다.
정확한 파일·메서드 diff와 결과 매핑은 후속 [구현 기록](implementation.md)에 확정한다. 이 설계의 과거 로그만으로 새 구현의 런타임 완료를 뜻하지 않는다.
공유 Java 소스를 여러 Minecraft 버전으로 전처리하는 구조이므로, 새 연결과 보조 코드가 1.20.1 범위에만 적용되는지 전처리 결과까지 확인한다. 다른 버전의 동작 변경은 이 설계에 포함하지 않는다.

## 로그·자동 테스트·실제 게임 완료 기준

로그는 이미 확보한 조회·판정 결과를 기록한다. `getPriority()`·`isFinished()`를 진단을 위해 추가 호출하지 않는다.
명령·세션·옛/현재 root, 조회 상태, 교체 이유, 결과 확정·전송·중복 억제를 연결한다.
필수 최초 상태 전환과 종료는 반복 요약에 소진되지 않는 기존 예약을 사용한다. 로그 모드·실패·억제는 종료 판단에 관여하지 않는다.

| 검증 | 필요한 확인 |
| --- | --- |
| 실제 root 교체 | 옛 실행을 정해진 비성공 결과로 한 번만 종료하고 새 root를 보존한다. |
| 동등 Task 재할당·같은 root 유지 | 전달 객체나 진단 assignment 변화만으로 교체 종료하지 않는다. callback만 변경되는 경우에도 기존·새 실행의 완료 소유권을 구별하고 소실된 callback만 영구 대기하지 않는다. |
| 조회 예외·mod 부재·체인 부재·실제 root 부재 | 각 상태를 구분하고 오류를 제거 성공으로 오인하지 않는다. |
| 월드·엔진·연결 수명 변경 | 예전 실행의 조회·관측을 새 수명에 결합하거나 단순 root 교체로 오분류하지 않는다. |
| 정상 완료와 자동 Idle 전환 | 기존 callback·event 순서를 유지하고, 해당 완료 이벤트가 큐의 33번째 이후에 남아도 교체 실패를 먼저 확정하지 않는다. 무관한 후속 이벤트가 기존 관측 구간의 평가를 무한히 미루지 않는다. |
| 방어·보관 선점 | 같은 user root의 실행을 종료하지 않고 복귀가 가능하다. |
| 늦은 callback·옛 root event·중복 event·새 명령 | 새 execution의 상태와 이미 확보한 matching 완료 관측이 덮어써지지 않는다. 같은 root 재사용에서도 명령 수명을 구분한다. |
| 전송 실패·결과 불명확 후 재전송 | 동일 식별자·확정 payload를 재사용하며 논리적 종료 처리·Chat/TTS 효과가 중복되지 않는다. |
| 버전 범위 | 새 연결·보조 코드의 전처리 결과가 Fabric 1.20.1 범위를 지킨다. |

실제 게임에서는 금 명령 실행 중 idle로 root를 교체해 무한 완료 대기와 성공 오보고가 없는지 확인한다.
별도로 금 수집의 정상 완료·명시적 STOP·방어·보관 복귀를 검증하고, 전송 재시도가 있어도 같은 요청의 terminal을 LAVI가 논리적으로 한 번만 처리하는지 확인한다.
소스 검토·자동 테스트·대상 JAR 빌드·배포 동일성·실제 런타임 결과는 각각 기록하며, 이번 문서화로 대신하지 않는다.
