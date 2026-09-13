<!-- 20260913_kpopmodder: Record the runtime observer-capacity crash and its documentation-only repair plan. -->
# 진단 관찰자 등록 한도 크래시 — 원인과 수정 계획

## 현재 상태와 범위

**2026-09-13 20:19:38 KST 크래시 원인을 기준으로 후속 구현을 진행했다.** 관찰자 상한 32, owner별 원자 등록·실패 격리와 도구 진단 호출 보호를 구현했다. 아래 크래시 당시의 16개 상한·실행 해시는 역사적 증거로 보존한다. 최신 검증 상태는 이 문서 마지막의 구현·검증 기록을 따른다.

적용 대상은 Fabric ChatClef 1.20.1의 LAVI 소유 진단 관찰자 등록·초기화 경계다. 도구 선택·교체 조건, 채굴·보관 정책, Task 우선순위, 자동방어, Baritone 경로·입력 소유권, 명령 결과·Chat/TTS 동작은 유지한다. Forge/MineMind나 공통 Minecraft 런타임을 추가하지 않는다.

관련 기준: [AGENTS.md](../../../AGENTS.md), [백엔드 분리](minecraft-backend-separation.md), [ChatClef 통합 방향](chatclef-carryon-integration-direction.md), [기존 로그 구현·검증 기록](chatclef-resource-observation-implementation-2026-09-13.md), [1.20.1 빌드 검증](chatclef-fabric-build-verification.md).

## 실제 실행 증거

소스 비교 기준 HEAD는 `dfe29ef8547ee85fa736b75b208b126fc73f0b8b`다. 아래 행번호와 파일 크기는 크래시 후 읽기 감사 시점의 값이며, 이후 로그 회전·실행으로 달라질 수 있다. 시각은 별도 표시가 없으면 KST다.

인스턴스 루트는 `C:/Users/jaewo/curseforge/minecraft/Instances/LAVI_TEST_Fabric01`이다. 실제 감사 파일명은 `logs/instance_audit.txt`다.

| 증거 | 내용 |
| --- | --- |
| `logs/instance_audit.txt:597` | `2026-09-13T11:11:18Z` = 20:11:18 실행. 크래시 전 새 launcher 실행으로 바뀐 기록 없음 |
| `logs/latest.log:245` | 20:11:35 handshake 승인, session `fabric-chatclef-0f46871959f044ac8a5652c4c27efed4`, server generation 2 |
| `logs/latest.log:847` | 20:18:38 월드 접속, 플레이어 좌표 약 `(942.55, 64, -1818.21)` |
| `logs/latest.log:1871` | 20:19:36 `BEST_TOOL_SLOT_DECISION`, 대상 `minecraft:chest`, 선택한 도구 `diamond_axe`, inventory slot 33 |
| `logs/latest.log:1872` | 20:19:37 `Unreported exception thrown!` |
| `logs/latest.log:1888` | `IllegalStateException: Diagnostic session lifecycle observer capacity exhausted.` |
| `crash-reports/crash-2026-09-13_20.19.38-client.txt:4,7,22` | 20:19:38 보고서, `Unexpected error`, `ExceptionInInitializerError`와 위 원인 예외 |
| `logs/stdout-logs.txt:5741,5749` | 같은 크래시 보고서 저장, `[EXIT] code=-1, terminatedByApp=false` |
| LAVI `logs/20260913_164007_log.txt:1327,1328` | 20:19:39 close frame 없이 같은 연결 해제. 해제 직전 active command/request/message ID 모두 null |

Minecraft `latest.log`는 3,369,850 bytes, `stdout-logs.txt`는 3,687,239 bytes, `instance_audit.txt`는 3,017,699 bytes였다. LAVI는 비어 있는 별칭 파일 대신 실제 timestamp 파일을 읽었다. 이 파일은 후속 읽기에서 592,613 bytes였고 20:20:24까지 기록됐다.

설치된 `mods/chatclef-1.20.1-0.18.23.jar`와 저장소의 `versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar`는 모두 8,598,123 bytes이며 SHA-256은 다음과 같다.

```text
ac7a63d40897af585a3e8da121223d56de48be9e4136012b2dee694d24cf54e6
```

이는 마지막 19:48 빌드가 실제 인스턴스에 반영됐다는 증거다. 사용자가 실행한 결과를 읽은 것이며 에이전트가 배포·게임 실행을 수행한 것으로 기록하지 않는다.

새 LAVI 연결 이후 명령 전송 기록은 없다. 연결 해제 JSON에 남은 `last_java_result`의 이전 금 채굴 요청은 generation 1 / 이전 세션 소유다. 이를 이번 크래시의 명령으로 결합하지 않는다. LAVI의 연결 오류는 Java 크래시 다음 초의 기록이며 직접 원인은 Java 예외 스택으로 판단한다.

## 확인된 원인과 실패 경계

다음 경로는 runtime의 `src/main/java/` 기준이다.

| 등록 위치 | 기준 HEAD | 크래시 당시 소스 |
| --- | ---: | ---: |
| `lavi/minecraft/diagnostics/ChatClefDiagnostics.java:79,82,85,88,89` | 4 | 5 |
| `lavi/minecraft/diagnostics/container/store/deposit/StoreDepositDiagnostics.java:129,135` | 2 | 2 |
| `lavi/minecraft/fabric/chatclef/bridge/runtime/FabricChatClefBridgeComponents.java:103,108,113,118,123,128` | 6 | 6 |
| `lavi/minecraft/diagnostics/toolselect/BestToolSlotDiagnostics.java:33` | 1 | 1 |
| `lavi/minecraft/diagnostics/toolselect/ToolSavePolicySnapshotDiagnostics.java:28` | 1 | 1 |
| `lavi/minecraft/diagnostics/toolselect/ToolEquipDiagnostics.java:42` | 1 | 1 |
| `lavi/minecraft/diagnostics/baritone/correlation/BuilderTraceRegistry.java:26` | 0 | 1 |
| **합계** | **15** | **17** |

이번 로그 보강에서 `ObservationDiagnostics.lifecycleObserver()`와 Builder 상태 정리 observer가 하나씩 추가됐다. 기존 `ToolEquipDiagnostics`의 observer 등록 자체는 새로 추가된 코드가 아니다.

`lavi/minecraft/diagnostics/session/lifecycle/registration/DiagnosticSessionObserverRegistry.java:11`의 상한은 **16**으로 유지됐다. `register()`는 기존 목록의 `contains(observer)`이면 중복 등록을 생략하고, 이미 16개가 있으면 20~21행에서 위 `IllegalStateException`을 던진다. 정상 구성은 서로 다른 17개 등록을 요구한다.

```text
TaskRunner.tick:55 — 원래 getPriority() 호출
  → PlayerInteractionFixChain.getPriority:97 — EQUIP_REQUEST 진단 호출
  → ToolEquipDiagnostics.<clinit>:42 — 최초 클래스 초기화 중 observer 등록
  → ChatClefDiagnostics.registerSessionLifecycleObserver:148~150
  → DiagnosticSessionLifecycleRegistry.register:16~17
  → DiagnosticSessionObserverRegistry.register:20~21 — 16개 상한 초과 예외
  → ExceptionInInitializerError가 게임 tick으로 전파
```

실제 `forceEquipItem()`은 `PlayerInteractionFixChain.java:106`에 있으므로 이 호출에서는 도구 교체 전에 실패했다. 로그 보강의 진단 초기화가 원래 게임 동작을 중단시킨 결함이다.

런타임에서 먼저 등록된 16개의 정확한 순서는 남아 있지 않다. 다만 실패한 등록이 ToolEquip이고, 등록 직전 목록이 상한에 도달했다는 사실은 예외 조건과 스택으로 확인된다. 이번 증거로 이전 금 채굴 반복·자동보관 미실행·Builder 빈 이동 목록의 원인까지 확정하지 않는다.

## 이전 오류·출력 한도·검증과의 구분

| 항목 | 의미 |
| --- | --- |
| 19:30 `InvalidMixinException` | Builder의 잘못된 inherited `@Shadow` 필드 연결로 게임 초기화 중 Mixin 적용 실패. 앞선 별도 수정 대상 |
| 20:19 `ExceptionInInitializerError` | 게임·월드 진입 후 일반 Java 진단 클래스 초기화에서 observer 등록 한도 초과. 이번 수정 대상 |
| 20:11의 상세 출력 256회 제한 | 반복 사건의 로그 출력 억제. observer 개수 16 제한과 서로 다른 소유자·한도 |
| 전체 출력 5,000 슬롯 제한 | 물리 진단 출력 예산. 이번 예외는 디스크 파일 크기나 이 출력 예산의 소진으로 발생한 것이 아님 |
| Mixin 변환 8/8 통과 | 대상 바이트코드 변환 검증. 실제 17개 observer의 전체 등록·지연 초기화 실행은 검사 범위에 포함되지 않았음 |
| focused 286개 성공 / 1개 중단 | 당시 개별 검증 결과를 보존. 관찰자 전체 구성의 런타임 안전성을 보장한 결과가 아님 |

Mixin의 client tick 연결 메서드가 이번 스택에 있지만, 직접 예외를 던진 곳은 일반 Java registry다. 이번 사건만을 근거로 Mixin 설정이나 필드 연결을 다시 변경하는 계획은 없다.

## 초기 수정 계획 — 후속 구현 반영

### 1. 전체 등록 구성과 유한한 용량

수정값은 현재 17개를 모두 수용할 수 있는 **32개 고정 상한**이다. 초기 문서화 때에는 미구현이었으며, 후속 구현에서 실제 registry에 적용했다. production 등록 목록과 정리 책임을 함께 관리하고, 새 observer가 추가되면 전체 구성 검증에서도 증가가 드러나게 한다. 같은 observer의 재등록은 슬롯을 추가 소비하지 않고, 서로 다른 owner를 임의로 같은 것으로 취급하지 않는다.

이 상한 조정은 observer 저장소에만 적용한다. 출력 256/5,000 한도와 메모리·요청 scope 제한을 함께 올리지 않는다. 요청·월드 변경 때 중복 observer를 계속 추가하거나 무제한 목록으로 바꾸지 않는다.

### 2. 등록 성공과 진단 사용 가능 상태

등록 API가 성공·이미 등록됨·용량 초과·잘못된 입력을 구분한 결과를 전달하도록 설계한다. 결과 이름과 구체적인 타입은 구현 시 기존 API와 함께 정한다. production 초기화에서는 예상 가능한 등록 거절을 정적 초기화 예외로 전파하지 않는다.

등록이 성공한 진단 owner만 세션 상태를 수집·출력한다. 거절된 owner는 해당 진단 상태를 무효화하고 수집·출력을 중단해야 한다. 이미 만들어진 핸들이나 지연 callback도 이 owner의 사용 가능 상태를 확인해 무효화 이후 상태를 다시 쌓지 않도록 한다. 등록 누락을 무시한 채 상태를 계속 쌓으면 OFF·세션 변경 시 오래된 관측이 남으므로 허용하지 않는다. 정상 구성 17개가 모두 등록되는 것이 기본 합격 조건이며, 실패 격리는 정상 진단 누락을 통과시키는 수단이 아니다.

기존 Bridge 6개는 진단용 ledger/map/pending/projection/terminal 증거를 관리하고 StoreDeposit 2개도 진단 상태·snapshot 정리를 담당한다. 이 등록 오류를 처리하면서 bridge 생성 전체, 실제 command queue, session guard, Task/result outbox, 전송·재연결을 건너뛰지 않는다. 등록에 성공한 owner의 기존 mode-OFF 무효화와 teardown의 snapshot 확보 → 출력 시도 → 정리 순서를 보존한다. StoreDeposit의 정리와 snapshot 기여자 두 등록을 모두 유지하고, 출력 호출 반환을 실제 파일 저장 성공으로 취급하지 않는다.

observer 개수와 진단 owner 개수는 동일하지 않다. 한 owner가 둘 이상의 observer를 필요로 하면 그 필수 등록들이 모두 성공해야 수집·출력을 활성화한다. 일부만 성공한 상태도 해당 owner의 등록 실패로 처리하고, 이미 등록된 callback 역시 비활성 상태를 존중하게 한다. 다른 정상 owner의 등록이나 상태를 함께 제거하지 않는다.

### 3. 게임 호출부로 진단 초기화 실패가 새지 않는 경계

`ToolEquipDiagnostics`는 메서드 본문에 들어가기 전에 `<clinit>`에서 실패했다. 메서드 내부에만 예외 처리를 추가하면 이번 실패를 막지 못한다. 위험한 등록을 예외를 던지지 않는 초기화 경로로 정리하고, 필요한 경우 LAVI 소유의 좁은 진단 호출 경계에서 진단 클래스 로딩 실패도 격리한다. 초기화가 이미 실패한 클래스의 후속 `NoClassDefFoundError`도 검증한다.

진단을 사용할 수 없을 때는 기존 미관측 값인 equip attempt ID `-1` 등의 계약을 확인해 유지하고, 같은 인자로 원래 도구 교체가 정확히 한 번 진행되도록 한다. `getPriority()`를 다시 호출하거나 실제 장착·경로 변경·Task 선택을 예외 처리 블록으로 묶지 않는다. 게임 동작에서 발생한 원래 예외는 그대로 전파하며 일반 게임 예외를 포괄적으로 삼키지 않는다.

등록 실패 기록은 실패한 owner, 원인, 당시 등록 수·상한·초기화 경계, 해당 진단의 사용 가능 여부를 제한된 횟수로 남긴다. 실패한 registry나 초기화 중인 진단 클래스로 다시 진입하는 재귀 로깅을 피한다. 출력 자체가 실패해도 게임 동작은 유지하며, 출력 호출 반환과 실제 로그 파일 저장은 따로 검증한다. 모드·사용자 설정을 자동으로 바꾸지 않는다.

### 4. 책임별 배치와 변경 단위

| 책임 | 계획 위치 |
| --- | --- |
| 유한한 등록 목록·등록 결과 | 기존 `diagnostics/session/lifecycle/registration/` |
| owner의 사용 가능 상태·진단 상태 무효화 | 해당 진단 owner 및 기존 lifecycle helper |
| 등록 실패 형식·제한된 출력 | 기존 diagnostics 출력 계층의 focused helper |
| 도구 진단 초기화·호출 격리 | 기존 `diagnostics/toolselect/`의 focused facade/helper |
| 전체 구성·지연 초기화·실패 격리 검증 | 관련 session/toolselect 테스트 및 독립 JVM 검증 |

새로 수정하는 LAVI 소유 코드에 독립된 책임이 둘 이상이면 파일을 분리하고 의미 있는 기존 폴더를 사용한다. upstream `TaskRunner`·`PlayerInteractionFixChain`의 이동·분해·상속 변경이나 scheduler 재설계는 계획하지 않는다. 필요한 최소 진단 hook 외의 기존 게임 동작을 바꾸지 않는다.

이 수정은 observer 등록·초기화 문제의 한 단위로 리뷰할 수 있게 유지한다. 기존 금 채굴·자동보관·Builder 관측 변경을 함께 원복하거나 다른 GOTO/FIND 변경을 섞지 않는다. 이 문서는 구현 방향이며 새로운 일반 승인 절차나 원인 미확인 기능 전체의 구현 금지 규칙을 추가하지 않는다.

## 검증 계획과 완료 조건

| 검증 | 요구 결과 |
| --- | --- |
| 전체 production 구성 | 실제 17개 observer 등록이 한 구성에서 함께 성공하고 누락 없음. 단순히 가짜 observer 17개를 넣는 용량 테스트로 대체하지 않음 |
| 지연 초기화 순서 | ToolEquip을 마지막에 처음 사용해도 정상. 순서를 바꾼 검증은 별도 JVM 등으로 이미 초기화된 클래스의 영향을 제거 |
| 중복·경합 등록 | 기존 중복 계약 보존, 상한 밖 등록 없음, 등록 결과와 목록 일치 |
| 한 owner의 부분 등록 실패 | 필수 observer 일부가 거절되면 그 owner의 수집·출력 비활성화. 이미 등록된 callback도 상태를 다시 만들지 않고 다른 정상 owner는 유지 |
| 의도적인 상한 초과 | 32개 수용 후 추가 owner의 거절 상태 확인. 게임 tick·도구 교체는 중단되지 않고 해당 진단은 상태를 누적하지 않음 |
| 초기화·출력 실패 | 진단의 초기화 오류와 후속 클래스 접근 오류, 실패 로그 출력 오류를 주입해도 원래 장착 호출의 인자·횟수·반환 및 원래 게임 예외 보존 |
| OFF/ON·세션 종료·새 요청 | 성공한 owner의 기존 정리·snapshot 동작 보존, 실패한 owner의 이전 관측 상태 미잔존, 늦은 callback이 폐기된 상태를 복구하거나 새 요청에 오결합하지 않음, bridge 동작 소유권 유지 |
| 기존 회귀 | 도구 선택·자동방어·Task 우선순위·채굴·보관 조건·미관측 ID 처리 보존 |
| 빌드·패키징 | 후속 구현 범위에 맞는 1.20.1 fresh build와 JAR hash/class/config 검증. [빌드 검증 기준](chatclef-fabric-build-verification.md) 적용 |
| 실제 Minecraft | 해당 후속 작업에 게임 검증이 포함되면 동일 JAR의 월드 진입 → 상자 대상 파괴 시도 → 도구 자동 교체를 확인. 최초 ToolEquip 초기화 시점을 포함 |
| 실제 파일 증거 | 등록 결과·필요 진단 사건·종료 기록의 파일 관측, 신규 크래시 보고서 및 launcher exit 확인. heartbeat만으로 장착 검증을 대신하지 않음 |

기존 버전 지정 `:1.20.1:build`가 다른 버전 컴파일을 끌어온 사실과, 이전 작업에서 1.20.1 실행 JAR을 별도 `clean`/`remapJar` 경로로 만들었던 사실은 구현 기록에 남아 있다. 후속 빌드에서도 실제 작업 그래프와 실행한 검증 범위를 명시하고 전체 빌드 성공으로 바꾸어 보고하지 않는다. 초기 문서화 단계에서는 빌드하지 않았으며 후속 실행은 아래에 기록한다.

완료 기준은 **정상 구성의 전체 등록 성공 + 등록/진단 초기화 실패 시 게임 동작 유지 + 정리·상태 소유권 보존**이다. 실제 장착까지 검증한 결과와 자동 검증·빌드 결과를 각각 기록한다.

## 후속 구현·검증 기록

사용자의 후속 디버깅 요청으로 구현했다. 기준 HEAD와 기존 gold/deposit/Builder 관측 변경을 보존한 mixed dirty worktree다. 이 등록 오류 수정이 기존 금 채굴 반복·보관 미실행을 해결했다고 간주하지 않는다.

| 책임 | 구현 위치와 동작 |
| --- | --- |
| 등록·원자성 | `diagnostics/session/lifecycle/registration/`: 고정 32개, 명시적 등록 결과, 중복 슬롯 소비 없음, owner의 전체 그룹 검증 후 등록. 실패한 owner를 다시 등록해도 복구하지 않음 |
| 상태·콜백 | `DiagnosticOwnerRegistration` 및 lifecycle callback wrapper: 사용 가능 상태와 진단 쓰기·정리 직렬화. 실패한 owner의 수집·snapshot·지연 callback 차단 |
| 실패 출력 | `registration/emission/`: ChatClef 정적 초기화에 재진입하지 않는 stderr 출력. 프로세스당 최대 32회이며 일반 사건 출력 예산을 늘리지 않음 |
| 실제 등록 구성 | Foundation 4 + Observation 1 + StoreDeposit 2 + Fabric crafting 6 + toolselect 3 + Builder 1 = 17. crafting 등록 경로는 `crafting/lifecycle/`로 분리해 실제 bridge 조립과 동일한 경로를 테스트함 |
| 도구 진단 호출 | `toolselect/call/`: 최초 `ExceptionInInitializerError`와 이후 `NoClassDefFoundError`까지 진단 호출 안에서 격리. snapshot은 `toolselect/snapshot/`으로 분리. 원래 장착 호출·반환·게임 예외 경계는 유지 |
| 늦은 관측 | ToolEquip 단일 활성 진단 attempt, BestTool 생성 세대 검사, Builder ledger 영구 무효화. Observation pin 및 실패 누계 쓰기는 `SESSION → owner → local state` 순서로 OFF와 직렬화 |

실패 출력의 성공은 파일 저장 증명이 아니다. owner를 끈 상태에서 게임 action 전체를 catch하지 않으며, `getPriority()`를 진단용으로 다시 호출하지 않는다. 새로운 런타임 설정·의존성·버전·자동방어 정책은 추가하거나 변경하지 않았다.

검증은 실제 production 관찰자를 새 JVM에서 초기화하는 세 가지 순서(ToolEquip 마지막, 역순, Bridge 먼저), 실제 나머지 16개가 등록된 뒤 잔여 용량을 채워 ToolEquip 등록을 거절시키는 경우를 포함한다. 전체 등록 성공 검사를 가짜 관찰자 17개로 대신하지 않았다. 원자 부분 실패·중복·잘못된 등록·OFF/ON·teardown·늦은 callback·출력 실패도 별도로 검사한다.

실제 장착 함수의 바이트코드에서 호출 1회, 기존 descriptor, 진단 catch/callback 밖에 존재함을 검사하고, 진단 실패 시 인자·반환·게임 예외가 보존되는 호출 경계 테스트를 함께 실행한다. 이 검사는 실제 월드에서 도구를 교체했다는 증거와 구분한다.

### 최종 검증 결과

| 검증 | 결과와 증거 |
| --- | --- |
| 1.20.1 clean 패키징 | **PASS**, 20:54:41~20:57:01 KST, 31개 task 실행, 2m 19s. `logs/build/observer-registration-1201-20260913-205440.log` 및 `.result.json` |
| 자동 검사 | **315 시작 / 314 통과 / 0 실패 / 1 중단**, 99개 클래스·107개 test/support source. `test/test_Isolation/minecraft/resource_observation/output/342bb5ae616647839aa1c711537433fb/` |
| 중단 1개 | 기존 `AutoDepositCategoryReservePolicyTest.allocatesFuelAsOneCategoryTotalInsteadOfPerItem`: named Minecraft 레지스트리 bootstrap에서 `SimpleRegistry`가 `RegistryEntry.Reference.setRegistryKey`에 접근하며 `IllegalAccessError`. 검증 스크립트 exit 1을 보존하며 전체 GREEN으로 보고하지 않음 |
| 신규 검사 | 등록/owner/출력 17개 + 도구 호출/상태 8개 + 전체 구성 1개(새 JVM 4회) + Builder 무효화 1개 + Observation 동시성 1개 = **28개 통과** |
| 실제 전체 등록 | `tool-last`, `reverse`, `bridge-first` 모두 **count=17**. 실제 앞선 16개 + 잔여 용량을 채운 `tool-overflow`는 count=32에서 ToolEquip을 typed `CAPACITY_EXHAUSTED`로 거절하고 수집 0회 유지. 같은 output의 `tmp/observer-graph-*.log` |
| Mixin 변환 | 최종 remapped JAR와 포함 Baritone로 **8개 대상 적용 / 0 실패**. `test/test_Isolation/minecraft/mixin_application/output/bda6824b3dbf4998990157d12d473c18/`. 격리 JVM의 실제 Sponge/MixinExtras 변환이며 live Minecraft 실행은 아님 |
| 배포 | **VERIFIED_SHA256**, `LAVI_TEST_Fabric01/mods/chatclef-1.20.1-0.18.23.jar`를 아래 JAR로 교체하고 동일 해시 확인. 직전 javaw 프로세스 0개. 구형 JAR은 저장소의 `logs/build/chatclef-1.20.1-0.18.23.before-observer-fix-ac7a63d4.jar`에 보존 |
| 수정 후 실제 게임 | **NOT_RUN / 앱 제어 연결 불가**. 월드 진입·상자 파괴 시도·실제 도구 교체 미확인. 시작·heartbeat를 도구 교체 성공으로 대체하지 않음 |

사용자 요청대로 새 PowerShell 창에서 `logs/build/verify-observer-registration-1201-20260913.ps1`를 실행했다. 실행 명령은 다음과 같다.

```powershell
.\gradlew.bat :1.20.1:clean :1.20.1:remapJar --rerun-tasks --no-build-cache --no-daemon --stacktrace --offline --init-script .gradle/codex-build-init.gradle
```

다른 버전의 compile/package task는 실행하지 않았다. 다중 버전 preprocess 의존성 때문에 다른 버전의 mapping/source 변환 task는 포함된다. 이 결과는 전체 다중 버전 `clean build` 성공이 아니다. JDK 21.0.12로 빌드했고 최종 클래스 major는 **61(Java 17)**이다. 생성된 두 JAR 중 `-all.jar`가 아닌 아래 최종 remapped JAR을 검증·배포했다.

```text
JAR: plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar
Bytes: 8636858
SHA256: 7581b214eca74de730c948a5c93d02e113cc8f730a31650e71de0a193805932a
Nested Baritone SHA256: 807a467dbddede6769d9b666f176225694f8586d9237e0b62713fbda39fe0b27
```

빌드 전 입력 manifest는 `logs/build/observer-registration-final-inputs-20260913-205438.txt`다. 빌드 후 production/resource source hash는 모두 일치한다. 이후 바뀐 입력은 새 Observation lease 테스트의 domain을 허용된 `mining`으로 고친 test 파일 하나이며, 위 최종 자동 검사에서 다시 컴파일·실행했다. 새 테스트의 최초 잘못된 domain으로 발생한 실패 로그도 덮어쓰지 않았다. 앞선 중간 빌드와 자동 검사 결과 역시 최종 결과로 대체해 주장하지 않는다.

Windows 앱 제어는 초기 연결 및 재초기화 후 모두 `Computer Use native pipe is unavailable (os error 2)`로 실패했다. 이 환경에서는 게임 화면 조작을 수행하지 못했다. 실제 검증은 새 JAR로 인스턴스를 실행하고 같은 월드에서 상자를 대상으로 도구 자동 교체를 유발한 뒤, `TOOL_SELECTION_DECISION`/`TOOL_EQUIP_REQUEST`/`TOOL_EQUIP_RESULT`와 신규 크래시·launcher exit를 함께 확인해야 한다. 이어 금 채굴·자동보관을 같은 요청 ID로 재현하는 조사는 아직 수행하지 않았다. 자동방어·도구 선택·채굴·보관 정책의 변경이나 해당 기존 문제의 해결을 주장하지 않는다. 커밋·푸시는 하지 않았다.
