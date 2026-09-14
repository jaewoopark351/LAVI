<!-- 20260913_kpopmodder: Document the observed gold-mining tool loop, unobserved automatic-deposit decisions, and bounded diagnostics requirements; documentation only. -->
<!-- 20260913_kpopmodder: Review BOUNDARY-mode coverage, side-effect-free observation, effect provenance, and bounded first-event retention. -->

# 금 채굴 도구 교체 반복·자동보관 미실행 — 로그 보강 계획

<!-- 20260913_kpopmodder: Point the later runtime review to a separate behavior-repair design without rewriting this logging-only checkpoint. -->
22:12 후속 실행의 정확한 도구 장착 불일치·단축바 충돌과 root 교체 시 1,216회 누계는 [후속 디버깅 설계](gold-mining-debugging-2026-09-13/README.md)에 기록했다. 그 실행의 자동보관은 31/36칸으로 기준 미충족이었다. 아래 16시대 관측·미확정 항목과 로그 전용 구현 범위는 당시 기록으로 유지한다. 후속 동작 수정은 최초 `PLAN_ONLY`에서 이후 구현으로 진행됐으며 [구현과 검증 기록](gold-mining-debugging-2026-09-13/implementation.md)에 별도로 남겼다.

<!-- 20260914_kpopmodder: Preserve the historical unknown cause while linking the later directly observed rearm failure. -->
2026-09-14의 별도 실행에서는 `33→준비35→보관후30→WAIT→36`과 재무장 차단을 확인했다. [자동보관 종료 후 재실행 설계](auto-deposit-rearm-2026-09-14/README.md)에 원인·상태 전이·실행 한도·STOP/자동방어 검증 계획을 분리한다. 아래 표의 자동보관 원인 UNKNOWN은 2026-09-13 당시 관측을 뜻하며 새 실행까지 미확정이라는 뜻이 아니다. 새 재실행 동작은 아직 `NOT_IMPLEMENTED`다.

## 1. 목적과 범위

사용자가 보고한 금 채굴 반복과 인벤토리 공간 부족 상황에서, **도구 교체가 왜 채굴을 다시 중단시키는지**, **자동보관은 왜 시작하지 않거나 다시 실행되지 않는지**를 같은 요청의 기록으로 설명할 수 있게 한다.

초기 요청은 문서화였고, 후속 **로그 보강만 구현** 요청에 따라 관측 코드를 추가했다. 확정한 구현 수치와 검증 상태는 [구현 기록](chatclef-resource-observation-implementation-2026-09-13.md)을 따른다. 이미 첫 요청의 도구 충돌이 확인됐더라도 이 문서를 근거로 동작을 수정하지 않는다. 이는 이번 작업의 범위이며 다른 기능에 일반적인 진단 선행 의무를 새로 부과하는 규칙이 아니다.

| 항목 | 상태 |
| --- | --- |
| 대상 | Fabric ChatClef 1.20.1 |
| 저장소·소스 검토 기준 | `C:\Vtuber_Souorce_Code\LAVI`, `dfe29ef8547ee85fa736b75b208b126fc73f0b8b` |
| 이번 변경 | 문서 및 후속 로그 전용 구현 |
| 이 계획의 로그 보강 | `IMPLEMENTED` — 검증 상태는 구현 기록 참조 |
| 코드·테스트·설정·Mixin 변경 | 관측 hook·helper·테스트 및 Builder Mixin 등록 |
| 테스트·빌드 / 배포·새 런타임 | 구현 기록 참조 / `NOT_RUN` |
| 기존 첫 금 요청의 도구 충돌 | 로그에서 관측, 현재 소스 분기와 대조 |
| 두 번째 금 요청의 동일 충돌 여부 | `UNKNOWN` — 상세 관측 제한 |
| 금 요청 중 자동보관 미실행 원인 | `UNKNOWN` — 평가·대기 상태 연결 부족 |

[AGENTS.md](../../../AGENTS.md), [백엔드 분리](minecraft-backend-separation.md), [ChatClef 연동 방향](chatclef-carryon-integration-direction.md), [Task 진단 기준](chatclef-task-lifecycle-diagnostics.md)을 적용한다. 기존 dirty 문서 3개(README, 캐시 문제 문서, 빈 이동 목록 크래시 계획)의 변경을 보존한다.

**자동방어·회피·반격·생존 우선순위는 유지한다.** 도구 선택, 단축바 배치, 준비 완료 조건, Task 교체·중단, 경로 계산·취소, 자동보관 임계값·재실행 조건·아이템 보호·체인 우선순위, Chat/TTS·명령 결과를 바꾸지 않는다. 아이템 버리기, 강제 보관, 재시도·timeout 추가, 캐시 변경, 게임 제어, 원복·커밋·푸시도 이번 작업에 포함하지 않는다.

## 2. 기존 사건 근거

### 2.1 파일과 관측 시점

실제 인스턴스는 `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01`이다. `instance\_audit.txt`로 전달된 파일은 실제 `logs\instance_audit.txt`로 확인했다. 아래 파일은 실제 내용을 읽었으며 빈 파일로 처리하지 않았다.

| 파일 | 이번 문서 작성 중 읽은 snapshot |
| --- | --- |
| [Minecraft latest.log](C:/Users/jaewo/curseforge/minecraft/Instances/LAVI_TEST_Fabric01/logs/latest.log) | 11,041,003 bytes, 12,373행; 내부 시각 16:39:15~17:09:08 KST |
| `logs/stdout-logs.txt` | 앞선 교차 조사에서 13,168,640 bytes; 자동보관·도구 사건 출력을 교차 확인 |
| `logs/instance_audit.txt` | 2,978,943 bytes; 07:39:11 UTC의 1.20.1/Fabric 실행 시도 기록. 도구·보관 판단 로그는 아님 |
| [LAVI 20260913_164007_log.txt](C:/Vtuber_Souorce_Code/LAVI/logs/20260913_164007_log.txt) | 508,712 bytes, 875행; 내부 마지막 시각 17:08:30 KST |

위 크기는 서로 다른 읽기 시점의 값이다. 실행 중 파일은 계속 늘어나고 재실행 때 교체될 수 있다. 파일 열거의 크기·수정 시각이 실제 읽은 내용보다 오래된 경우도 확인했다. 최신 상태를 판단할 때 내부 타임스탬프와 세션 식별자를 함께 확인한다. 아래 행 번호는 이 세션의 관측 위치이며 미래의 `latest.log`에도 같은 사건이 있다는 보장이 아니다.

문서 작성 중 메모리에 읽은 전체 byte snapshot의 SHA-256은 다음과 같다. 원본을 수정하거나 별도 로그 복사본을 생성하지 않았으며, 아래 해시가 이후 늘어난 파일 전체와 일치한다고 주장하지 않는다.

```text
latest.log, 11,041,003 bytes:
7a061873e286fccc7e8a2d6c73f5289a24cc1e77b2e335d3e0571e2530e59d43

20260913_164007_log.txt, 508,712 bytes:
d9ca0999a294b73481b346e2786c6791c029224416d8041d8ee329329d78aac4
```

현재 소스 대조와 기존 런타임 관측을 구분한다. 이번 문서 작업에서 실행 JAR·메모리 클래스와 소스의 완전한 일치를 새로 검증하지 않았으며, 새 진단의 실행 증거도 없다. ScreenVision의 자동 화면 요약은 게임 판단·원인 근거로 사용하지 않는다.

### 2.2 요청과 시간 순서

```text
command: get gold_ingot 10
session: fabric-chatclef-516cb678e7ee4496b9637c034679d148

첫 요청 (16:56:30):
request: lavi-input-ko-38c4707b121e4eba9d9d535145a63a62
correlation: lavi-735692cde6b44cb0bb1d1eb534ff0f76

재요청 (16:58:45):
request: lavi-input-ko-892b692bdaa14ff7922be8e3bf60c7d0
correlation: lavi-a50d7f91c92943438d7a7ed409579749
```

LAVI/ownership의 connection generation은 1, Java dispatch/command context의 generation은 2로 관측됐다. 두 값을 같은 필드로 합치거나 임의 보정하지 않는다. 요청 ID·correlation·세션을 사용하고 generation은 해당 필드의 출처를 보존한다. 이 차이 자체를 채굴 반복의 원인으로 해석하지 않는다.

| KST / Minecraft 행 | 관측 사실 |
| --- | --- |
| 16:39:49 / 971 | 금 요청 전 자동보관 계획: `occupiedSlots=35/36`, `READY`, 예상·목표 확보 7칸, 일반 컨테이너 대상 |
| 16:40:18 / 1533~1536 | 위 자동보관의 종료 요약: `UNKNOWN_STOP`, `naturalFinishObserved=false`, `transferDecisionCount=0`, `effectVerified=false`. 실제 7칸 확보를 입증하지 못함 |
| 16:56:30 / 3072 이후 | 첫 `get gold_ingot 10` 전달·실행. LAVI 476~488행과 연결 |
| 16:56:37 / 3406~3407 | Baritone 하강 segment `SUCCESS_SEGMENT`, movement 41개, `ADOPTED_AS_CURRENT`; Y64에서 Y23까지의 경로 계산·채택이며 도착 증거는 아님 |
| 16:56:38 / 3418~3420 | `(940,63,-1816)` 잔디를 파려고 다이아 삽 장착. 내부 hotbar index 1(화면 두 번째 칸)이 돌 곡괭이→삽으로 변경 |
| 같은 tick 20289 / 3422 | 부모가 `accessToolReady=true`, `accessToolHotbarVisible=false`를 읽고 `MOVE_ACCESS_PICKAXE_TO_HOTBAR` 선택 |
| 16:56:38 / 3435~3436 | 준비 Task가 기존 `DestroyBlockTask`를 중단. 최종 금광석 목표는 `(940,11,-1816)` |
| 16:56:39 / 3613~3615 | 돌 곡괭이를 inventory index 19→hotbar index 1로 이동, 다음 tick `READY` |
| 16:56:38~16:57:36 / 3436~7000 | 같은 목표의 고유 Destroy 실행 80개(run 12~91)가 `PrepareMiningOperationToolsTask`에 의해 중단. 단순 중복 출력 80행이 아님. 해당 수명 요약의 채굴 진척은 0 |
| 16:57:36 / 7006 | `DIAGNOSTIC_SESSION_CAP_REACHED`: 일반 예산 4,936개 소진. 총 상한 5,000 중 당시 critical 사용 27개, 예약 잔여 37개 |
| 16:58:06 | 첫 금 요청은 명시적인 사용자 STOP으로 cancelled. LAVI 523행 |
| 16:58:10~17 | 사이에 `get chest 1` 실행·완료. 상자 제작 완료는 자동보관 성공 증거가 아님 |
| 16:58:45 | 금 재요청 running. LAVI 602~614행 |
| 17:04:26 / 11405 | 재요청 341,813ms 경과 후에도 같은 채굴 자식의 완료 대기 |

후속 17:07대 관측에서는 금 요청의 종료 대기 로그에 선택된 `IdleTask`가 함께 나타났다. 이는 이전 채굴 화면과 다른 시점이다. 마지막 화면과 예전 Task 상태를 무기한 동일시하거나, Idle만으로 금 채집 완료·STOP 원인을 단정하지 않는다. 명령 종료 전달의 별도 원인은 이번 도구·보관 진단 계획에서 수정 대상으로 확장하지 않는다.

### 2.3 확인된 범위와 미확정 범위

- 첫 금 요청의 **삽 장착→접근용 곡괭이의 단축바 이탈→부모 준비 재진입→채굴 중단** 연결은 실제 로그에 있다. 이를 단순 no-path나 곡괭이 미보유로 설명하지 않는다.
- 재요청의 내부 교체·중단 기록은 부족하다. 첫 요청과 같은 원인이라고 자동으로 확정하지 않는다.
- 자동보관은 이 세션에서 이미 한 번 계획·실행된 기록이 있다. 그 작업은 금 요청 전이며 command context도 연결되지 않았다. 당시 35/36을 금 작업의 현재 점유율로 재사용하지 않는다.
- 금 첫 요청의 자동보관 평가 기록은 **일반 로그 한도에 도달하기 전부터 부족**했다. 이후 한도에 따른 억제와 구분하며 모든 공백을 cap 때문이라고 하지 않는다. 현재 소스의 VERBOSE 전용 출력 제약도 5절에서 별도로 다룬다.
- 사용자의 인벤토리 화면은 대부분 차 있고 약 2칸이 비어 보인다. 약 34/36은 화면 해석이며 런타임 pressure snapshot이 아니다. 완전 포화 여부와 자동보관 조건 충족 여부를 별도로 관측해야 한다.
- 이전 자동보관 뒤 `WAIT_FOR_REARM`에 머물렀을 가능성은 아직 가설이다. 금 작업 시점의 실제 상태·진입 이유·해제 판단을 추가로 연결해야 한다.

## 3. 현재 소스의 관측 경계

### 3.1 도구 준비와 채굴

[PrepareThenMineRawGoldTask](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/integration/mining/operation/PrepareThenMineRawGoldTask.java)는 매 tick 도구 준비 상태를 평가하고 준비가 아니면 준비 Task를 반환한다. [MiningOperationToolState](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/integration/mining/operation/MiningOperationToolState.java)의 준비 조건에는 접근용 곡괭이의 단축바 노출이 포함된다. [MoveMiningToolToHotbarTask](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/integration/mining/operation/MoveMiningToolToHotbarTask.java)는 결정된 슬롯으로 도구를 옮긴다. 현재 소스 분기는 첫 요청 로그와 부합한다.

| 관측 경계 | 필요한 기록 |
| --- | --- |
| 기존 `TOOL_SELECTION_DECISION`·`TOOL_EQUIP_REQUEST/RESULT` | 실제 선택 소유자·호출 경계, 접근 블록/최종 광석 목표 구분, equip attempt ID, 선택한 원본 슬롯·아이템·내구도, 실제 이동 전후 슬롯과 아이템, 기존 반환 결과 |
| 부모 준비 판단 | 이전/현재 READY 여부, target/access 역할별 후보 필터의 실제 결과(미보유·종류 부적합·보호 정책·내구도 부족·단축바 이탈), 실제 선택한 준비 단계, 이미 실행 중인 자식, 해당 판단과 연결된 직전 equip attempt |
| 준비용 단축바 이동 | source/destination index와 좌표계, 이동 전후 실제 도구, 원래 호출의 정상 반환/예외 관측과 이후 슬롯·READY 상태. `forceEquipSlot(Slot)`은 void이므로 없는 성공 반환값을 만들지 않음. 아이템 종류 일치와 정확한 선택 스택 일치를 구분 |
| 자식 반환·교체·중단 | 요청/부모/자식 실행 ID, interrupter, 중단 전후 Task 상태, 기존 경로 채택·취소 경계와 owner, 같은 자식 재사용인지 새 실행인지 |
| 진행 관측 | 실제 플레이어 좌표, 목표·도달 가능 거리 관측, 실제 블록 파괴 진행과 원석 수량 변화, 동일 목표에서 준비 재진입·중단·재시작 누계 |

최초 교체 사유와 의미 있는 상태 변화를 남기고 반복은 집계한다. `READY→준비→READY` 왕복 횟수와 무진척 기간은 **진단용**이다. 반복 횟수·시간이 특정 값에 도달해도 로그가 STOP·재시도·다른 도구 선택·준비 생략을 결정하지 않는다. 무진척 판정은 관측한 값·기간·누락 여부를 함께 남기고 확인 불가를 0 진척으로 바꾸지 않는다.

실제 `MiningOperationToolPolicy.evaluate()`의 입력·필터 결과·반환 state를 관측한다. 기존 후보 설명의 `ToolSavePolicyDiagnostics.computedDecision()`처럼 진단 중 재계산한 결과를 실제 선택 당시의 정책 결정으로 인용하지 않는다. 과거 값의 출처를 구분하고, 새 관측은 실제 결정 경계에서 이미 얻은 값을 사용한다. 원석 수량 증가는 별도의 수량 변화 관측이다. 대상 블록 파괴→드롭→습득이 연결되지 않으면 해당 목표의 채굴 성공·진척으로 귀속하지 않는다.

### 3.2 인벤토리 압력과 자동보관을 하지 않는 결정

[PressureReader](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureReader.java)는 `PlayerInventory.main`의 점유 슬롯 수를 읽는다. 장비·offhand·제작창 슬롯 수와 섞지 않는다. [PressureSnapshot](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureSnapshot.java)의 현재 조건은 다음과 같다.

| 현재 소스 조건 | main 슬롯이 36개일 때 |
| --- | --- |
| 점유율 9/10 이상 | 33칸 이상이면 발동 평가 기준 충족 |
| 점유율 7/9 이하 | 28칸 이하이면 대기 상태에서 재실행 준비로 돌아갈 수 있는 low-water 조건 |

이는 현재 조건을 설명한 것으로 임계값 변경 제안이 아니다. 빈 슬롯과 기존 스택에 합쳐 담을 여유는 다른 값이다. 이 사건의 금 원석·접근 도구에 필요한 공간을 관측한다면 관련 아이템의 안전한 read-only 집계와 조회 상태를 분리하고, 기존 발동 계산을 바꾸지 않는다.

[PressureChain](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureChain.java)과 [StateMachine](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureStateMachine.java)의 **early return을 포함한 기존 결정**을 연결한다.

[AutoDepositRuntime](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/AutoDepositRuntime.java)의 tick 진입과 [TickSequence](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/lifecycle/AutoDepositRuntimeTickSequence.java)의 binding 관측→pressure 평가 호출도 연결한다. 등록·입구가 관측됐는지, pressure 평가에 도달했는지, 도달한 뒤 어떤 이유로 반환했는지를 구분한다. 기존 호출 순서를 바꾸거나 빠진 호출을 진단이 대신 실행하지 않는다. 단순 기록 부재만으로 callback 미실행이나 예외를 확정하지 않는다.

| 관측 경계 | 필요한 기록 |
| --- | --- |
| 평가 진입·읽기 불가 | 평가 sequence, tick, in-game/bridge 활성 상태, snapshot 유무·조회 실패 이유, 점유/전체/빈 슬롯, 슬롯 집계 범위·캡처 시각 |
| 임계값 판단 | threshold/low-water의 실제 값과 결과, 기존 `thresholdPending`과 state 전후, 선택된 분기 |
| 대기 상태 | `WAIT_FOR_REARM`/`NO_SAFE_SURPLUS_WAIT`에 들어간 이유·시각·이전 보관 operation, 현재 대기 기간, 재평가 허용/거부 사유, fingerprint/등록 revision의 의미 있는 변경 여부 |
| 기존 작업·체인 확인 | 현재 user root와 실제 선택된 chain, Idle 여부, 기존 보관/StoreHome 중복 여부, `user_task_chain_not_selected` 등 기존 차단 이유 |
| 필요한 물품 판단 | [WorkingSetResolver](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/working/ActiveTaskWorkingSetResolver.java)의 지원 여부·실제 거부 이유, 어떤 Task 경로를 기준으로 어떤 수량을 보호했는지 |
| 계획 판단 | [PolicyEngine](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/policy/AutoDepositPolicyEngine.java)의 READY/보류/실패와 이유, 보호·보관 가능 물품 집계, 예상 확보 슬롯, 대상 없음·상황 변경 구분 |

낮은 점유율, 이미 RUNNING, 읽기 불가, 재무장 대기, 같은 fingerprint 유지 등으로 **아무 Task도 시작하지 않은 결정**도 첫 발생·사유 변화·제한된 대기 요약에 남긴다. 루트 명령이 바뀌었을 때 진단 연결을 갱신하되, 그것만으로 기존 state를 ARMED로 바꾸거나 보관을 다시 실행하지 않는다.

게임 밖·bridge OFF·RUNNING 분기는 pressure 읽기 전에, 저점유·대기 분기는 working-set/policy 평가 전에 반환할 수 있다. 건너뛴 필드는 `NOT_EVALUATED`와 마지막 평가 단계·반환 이유로 기록한다. 이것은 조건 불충족 `false`, 조회 시도 실패, 연결 정보 `UNAVAILABLE`과 다르다. 로그 값을 채우려고 건너뛴 pressure 조회·정책·fingerprint 평가를 추가 실행하지 않으며, 이전 snapshot을 쓸 때는 이전 값임과 캡처 시각을 표시한다.

### 3.3 발동 이후 실제 보관과 공간 확보

| 관측 경계 | 필요한 기록 |
| --- | --- |
| 계획→실행 Task 등록 | 평가 sequence, policy epoch, auto operation/pressure run, parent root, 계획 target 수, 실제 Task 등록 여부 |
| scheduler 선택·중단 | 기존 평가의 실제 활성 결과·local priority·최종 선택과 평가 sequence/phase. pressure 평가 당시 cached chain과 scheduler의 최종 선택을 구분. 준비되지 않은 체인도 우선순위 51로 실행 중이라고 표시하지 않음 |
| 컨테이너 후보 | 탐색한 후보 수, 제외 사유별 집계, 선택한 목적지·차원·route child, 후보 없음/접근 실패/가득 찬 상자/보호 제한 구분 |
| 접근·열기·이동 | 실제 이동·GUI 결합·슬롯 전송의 기존 결과와 거부 이유. 클릭 요청과 서버에서 관측한 반영을 구분 |
| 종료·공간 결과 | 보관 시작/종료의 같은 범위 점유율과 부호 있는 순변화, 전송으로 입증된 이동 수량·공간 효과, 목표 대비 부족분, 완료/중단·관측 불가, 후속 state와 재무장 조건 |
| 채굴 복귀 | 보관 전후 user root/자식의 실제 연결, 원래 채굴 재개·다른 명령으로 교체·Idle을 구분 |

기존 자동보관·transfer·effect 진단을 우선 재사용한다. 계획상 7칸 확보, transfer 호출 성공, Task 종료만으로 실제 공간 확보를 주장하지 않는다. 기존 [DepositAllAutoDiagnostics](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllAutoDiagnostics.java)의 `FREE_SLOT_POSTCONDITION` 같은 경계를 이어서 관측한다. 자동방어가 선택됐다면 그 사실과 이후 복귀를 기록할 뿐 우선순위를 낮추거나 방어를 막지 않는다.

현재 `actualFreedSlots`는 `max(0, 시작점유-종료점유)`로 계산된다. 이 값은 음수 변화를 숨기며, 보관이 그 변화를 일으켰다는 증거까지 제공하지 않는다. 기존 필드의 의미를 바꾸지 않고 원시 점유율·부호 있는 순변화와 전송 효과의 입증 상태를 별도로 남긴다. 동일 전송 attempt·대상·GUI 결합 아래 플레이어 쪽 감소와 컨테이너 쪽 증가가 연결되는지 확인하고, 소비·제작·드롭·사용자 조작·동시 습득 등 다른 변화가 섞이면 미확정 또는 부분 관측으로 남긴다. 인벤토리를 잠그거나 외부 변화를 차단해서 증거를 만들지 않는다.

## 4. 상관관계와 상태 보존

각 사건은 diagnostic activation/instance, request/correlation/session, 출처가 구분된 connection generation, client tick·단조 시각, event sequence로 연결한다. 추가 연결은 책임별로 둔다.

- 도구: mining operation, parent/root, equip attempt, 실제 source/destination 슬롯, child run, 최종 target.
- 자동보관: evaluation sequence, pressure snapshot sequence, policy epoch, auto operation/pressure run, route child, 전송 attempt.
- 공통: 판단 당시 root/chain과 결과 소비 당시 root/chain을 별도 보존. 명령 이전 자동보관은 `UNBOUND` 또는 실제 출처로 기록.

출력 순서나 같은 클래스 이름만으로 인과관계를 만들지 않는다. 자동보관이 시작되지 않은 경우에도 평가 ID가 있어야 하며, 실제 생성되지 않은 auto operation ID를 꾸며 붙이지 않는다. 자동보관은 진단 activation 안에서 여러 명령에 걸쳐 대기할 수 있으므로 대기 진입 출처와 현재 평가 요청을 별도로 유지한다.

도구 반복의 진단 scope는 요청과 그 요청의 준비·채굴 부모 실행에 결합한다. 같은 부모 아래 `DestroyBlockTask`가 중단·재시작돼도 새로운 scope로 만들거나 누계를 초기화하지 않는다. child run은 scope 내부의 반복 식별자다. 요청에 연결되지 않은 자동보관은 실제 instance/state 수명의 별도 scope를 사용하고 평가 tick마다 새 scope를 만들지 않는다.

소유자가 이미 읽거나 결정한 값을 불변 snapshot으로 넘긴다. 진단을 위해 정책 평가, `isFinished`, 도구 장착, 슬롯 이동, Task stop, 경로 요청을 다시 호출하지 않는다. 추가 read-only 조회는 부작용·스레드 적합성을 확인하고 `UNAVAILABLE`·조회 오류를 0/false/성공으로 바꾸지 않는다.

[TaskRunner](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasksystem/TaskRunner.java)의 기존 `isActive()` 결과, local `priority`·`maxPriority`, 최종 선택을 캡처한다. 로그용 `getPriority()`·`isActive()` 추가 호출로 chain을 재평가하지 않는다. 실제 `PlayerInteractionFixChain.getPriority()`는 도구 장착을, `MobDefenseChain.getPriority()`는 Task·입력·방어 상태 변경을 수행할 수 있으므로 단순 getter로 취급하지 않는다.

현재 상태·최근 전환은 출력 허용 여부를 검사하기 **전에**, 진단 활성화 범위에서 제한적으로 갱신한다. [기존 lazy emission](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/mining/emission/MiningDiagnosticEmissionOrchestrator.java)의 supplier 안에서만 갱신하면 출력 거부 시 이력이 사라질 수 있다. formatter는 동결된 값만 읽는다.

개별 도구 operation 종료 시 종료 snapshot을 확보한 뒤 그 operation 전용 이력을 정리한다. 자동보관의 대기 진입 출처는 해당 대기 상태가 유지되는 동안 별도의 제한된 불변 metadata로 보존하고, 원래 명령 종료만으로 지우지 않는다. OFF·월드/activation 교체 때 해당 진단 상태를 정리하며, 늦은 callback이 새 activation이나 새 요청에 과거 이력을 잘못 붙이지 못하게 한다.

최근 ring과 별도로 scope의 최초 인과 연결(실제 equip→부모 판단→자식 중단) 및 자동보관 대기 진입 snapshot을 유한한 고정 슬롯에 보존한다. active/retired scope의 수용·보관 상한을 나누고, 새 scope 때문에 진행 중인 대기 출처나 최초 인과 기록을 조용히 퇴출하지 않는다. 수용 불가·출처 소실은 `coverageComplete=false`와 원인·누계를 남긴다. 진단 상태 갱신·복사의 동기화 범위를 작게 유지하고 게임/정책 호출과 출력은 진단 lock 밖에서 수행한다.

## 5. 로그 한도와 장시간 반복 관측

현재 [ChatClefDiagnostics.logEvent()](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/ChatClefDiagnostics.java)는 VERBOSE에서만 출력한다. `DepositAllAutoDiagnostics`의 상태 전환·미실행·공간 결과와 TaskRunner의 여러 기존 로그가 이 경로를 사용하므로, helper가 있다는 이유로 BOUNDARY 모드에서도 관측된다고 가정하면 안 된다. 이는 현재 소스에서 확인한 출력 제약이며, 금 요청의 실제 자동보관 상태가 무엇이었는지까지 증명하지 않는다.

새 필수 관측은 현재 조사 모드인 BOUNDARY에서 출력되는 최소 이벤트로 연결하고 그 분류·예약을 검증한다. 전체 VERBOSE를 켜거나 일반 `logEvent()`의 전역 의미를 바꾸지 않는다. `MODE_FILTERED`와 local/shared cap을 별도 누락 이유로 취급하며 OFF에서는 새 게임 관측·출력을 시작하지 않는다.

현재 사건의 일반 출력 한도 4,936개 소진과 채굴의 [local budget](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/mining/budget/MiningDiagnosticSessionBudget.java)은 별도 제한이다. local을 통과해도 shared admission에서 거부될 수 있다. 기존 critical reserve의 존재는 새 도구/보관 이벤트의 전용 예약 확보를 뜻하지 않는다.

후속 구현에서 activation 범위, 유한한 필수 signature 목록, local/shared 양쪽의 분류·예약 소유자·필요 슬롯 수를 먼저 확정한다. 최소 signature는 첫 준비 재진입 사유, 첫 실제 자식 중단 사유, 첫 반복/무진척 요약, 자동보관의 각 서로 다른 미실행 사유, 대기 진입·해제, 보관·요청 종료 관측을 구별한다. 무작위 실행 ID만 바뀌었다고 매번 필수 최초 사건으로 취급하지 않는다.

의미상 signature는 `owner/event/reason/phase`로 정의하고, 최초 출력 여부를 관리하는 key는 `activation + 수용된 안정적인 scope + signature`로 구분한다. 따라서 앞 요청의 출력이 다음 수용된 요청의 첫 사건을 억제하지 않으며, 같은 요청의 child 재시작마다 새 예약을 만들지도 않는다. scope·signature·필수 분할 이벤트·제한된 재시도에 필요한 최대 용량을 함께 계산한다. 구현·파일 검증 전 예약 확보 상태는 `NOT_VERIFIED`다.

| 제어 | 초기 설계 기준 |
| --- | --- |
| 일반 반복 출력 | 같은 상태는 집계, 대기·무진척 요약은 200 game ticks 또는 10초보다 자주 출력하지 않음 |
| 최근 이력 | scope당 최근 32개, 항목당 최대 2 KiB의 불변 값 |
| 최초 출처 보존 | scope당 최대 4개, 각각 최대 2 KiB의 별도 고정 snapshot. 진입 출처와 최근 ring을 분리 |
| scope 수용 | 초기안은 instance/activation당 active 최대 4개·retired 최대 4개. 고정 출처·종료 snapshot·누계 등 전체 metadata 상한도 구현 때 산정. 실제 owner 수에 맞춰 수치 변경 시 문서와 검증을 함께 갱신 |
| 출력 크기 | 이벤트당 최대 8 KiB, 문자열 값당 최대 256자. 전체 인벤토리·전체 경로 배열·후보별 매 tick 덤프 제외 |
| 필수 기록 | 일반 detail과 이력 dump가 소비하지 못하는 local/shared 예약 사용. 유한 signature 수와 payload 분할까지 포함해 예산 산정 |
| 억제·누락 | local/shared cap, dedup, payload cap, OFF, capture 실패, 연결 eviction, sink 실패를 구분하고 집계 |

첫 반복·미실행 이유를 먼저 출력하고 관련 최근 이력을 제한된 크기로 연결한다. 일반 상한을 소진시킨 뒤 **다음 요청의 첫 사건**도 예정된 예약 범위에서 남는지 검증한다. 예약도 유한하므로 모든 미래 요청의 무제한 출력을 보장한다고 하지 않는다. 확보 불가·예약 소진은 명시하고 재현 단위의 충분한 관측 예산을 검증한다. 한도를 우회하려고 매 tick/요청 새 fallback key나 activation을 만들거나 비종료 이벤트를 `terminal=true`로 위장하지 않는다.

각 logical event의 `captured`, `attempted`, local/shared `admitted/rejected`, sink `completed/failed/unknown`, 사후 `fileObserved`를 분리한다. local 통과만으로 출력 완료나 dedup 완료로 처리하지 않는다. 재시도를 채택한다면 동일 logical event ID·attempt 번호와 사전에 정한 유한 횟수·예약을 사용하고, 재시도 계기로 매 tick 재방출하거나 무제한 대기 큐를 만들지 않는다. sink 결과가 불명확하면 동일 ID의 중복 가능성을 표시하며, 이를 별개의 최초 사건으로 집계하지 않는다. 누락 요약조차 출력할 수 없으면 제한된 최종 상태에 남기고 증거 미확보로 보고한다.

진단 호출·출력 함수의 성공과 실제 로그 파일의 기록은 구분한다. sink/하위 큐 성공만으로 파일 증거를 확보했다고 보고하지 않는다. 종료 callback에만 핵심 기록을 맡기지 않으며, 파일 출력을 보장한다는 이유로 게임을 멈추고 flush/drain·polling 대기를 추가하지 않는다. observer 오류·OFF·예산 소진은 동작 판단에 영향을 주지 않아야 한다.

## 6. 책임별 파일·폴더 구분

후속 코드 작업에서는 기존 upstream Task 파일을 이동하거나 크게 분해하지 않고 최소 관측 연결만 둔다. LAVI 소유 파일에 독립 책임이 둘 이상 생기면 focused 파일과 의미 있는 폴더로 나눈다.

| 책임 | 적용 후보 |
| --- | --- |
| 도구 교체·준비 전환 snapshot | 기존 `diagnostics/mining/` 아래 도구/operation 진단 책임 |
| 반복 집계·최근 이력 | 도구 snapshot과 분리한 bounded 진단 상태 소유자 |
| 압력 평가·미실행·재무장 관측 | 기존 자동보관 진단에 인접한 pressure/decision 관측 책임 |
| 보관 계획·route·transfer·효과 | 기존 `diagnostics/container/store/deposit/`와 자동보관 정책 진단 재사용 |
| 필수 출력 예약·분류 | Fabric 내부 기존 진단 admission 협력자. 게임 상태 판단과 분리 |

별도 범용 Minecraft logger/backend, Forge placeholder, 무제한 registry/worker를 만들지 않는다. 도구 준비와 자동보관 동작을 한 관리자로 합치거나 관측 코드를 숨은 복구 제어기로 만들지 않는다.

## 7. 후속 검증과 완료 기준

| 검증 상황 | 기대하는 관측·보존 |
| --- | --- |
| 삽이 접근용 곡괭이를 단축바에서 밀어냄 | 실제 equip→부모 판단→자식 중단→준비 완료를 같은 tick/실행 ID로 연결 |
| 도구 없음·내구도 부족·단축바 이탈 | 실제 필터 결과와 진단 재계산을 구분. void 이동 호출의 정상 반환과 슬롯 반영을 구분하고 기존 선택 결과 유지 |
| 같은 목표의 장시간 반복 | 32개를 넘는 고유 child run에서도 최초 연결·누계 유지. child stop이 scope 종료·누계 초기화가 되지 않음 |
| 점유 32/33/34/36칸 및 low-water 28칸 | 실제 기존 threshold/재무장 결과와 슬롯 범위를 기록. 추가 비교가 동작 조건을 대체하지 않음 |
| 평가 입구 미관측·early return | 호출 도달/미관측, `NOT_EVALUATED`, 조건 false, 조회 오류를 구분하고 기존 호출 횟수·순서 보존 |
| WAIT_FOR_REARM / NO_SAFE_SURPLUS_WAIT | 이전 보관의 진입 출처, 유지·해제 이유, 루트 변경 전후 연결을 확인. 강제 재무장 없음 |
| 보관 불가·정책 미지원·문맥 변경 | 실제 소유자의 거부 이유와 보호/보관 가능 집계를 남김 |
| 보관이 발동했지만 다른 체인 선택 | 기존 local priority와 최종 선택만 관측. 도구·방어 chain getter 추가 호출 없이 기존 입력·Task 호출 횟수 보존 |
| 후보 없음·상자 가득 참·전송 거부 | 발동 판단과 목적지/전송 실패를 구분 |
| 계획 성공이나 실제 슬롯 확보 없음 | 예상·실제 효과와 종료 이유를 분리, 보관 성공을 만들지 않음 |
| 소비·제작·드롭·다른 습득이 섞임 | 부호 있는 슬롯/수량 순변화와 원인이 연결된 보관·채굴 효과를 구분 |
| BOUNDARY 모드 | 기존 VERBOSE helper의 단순 호출로 충분하다고 하지 않고 필수 관측의 실제 출력 확인. 모드 필터와 예산 거부 구분 |
| 일반 예산 소진 후 다음 금 요청 | local/shared 양쪽에서 필수 첫 이유·상태 전환·종료의 예약을 확인하고 실제 파일에서 대조 |
| local 통과 후 shared 거부·sink 결과 불명 | 부분 승인·실패를 출력 완료로 처리하지 않고, 제한된 재시도와 동일 event ID의 중복 상태를 검증 |
| active/retired scope 포화 | 진행 중인 대기 출처·최초 snapshot의 조용한 퇴출 방지, 신규 수용 실패의 coverage 표시, 늦은 callback의 재삽입 방지 |
| OFF·observer 예외·sink 실패·eviction | 도구/슬롯/Task/경로/보관 조건/명령 결과가 진단 미사용 때와 동일, 불완전한 증거는 명시 |
| STOP·새 명령·월드/activation 교체 | 실제 종료·대기 상태를 보존하며 과거 반복을 새 요청에 잘못 귀속하지 않음 |

후속 구현에서는 관측 hunk와 실제 이벤트 분류를 대조하고 focused 검증·[1.20.1 clean build](chatclef-fabric-build-verification.md)의 결과를 별도로 기록한다. 현재 구현과 실제 검증 상태는 [구현 기록](chatclef-resource-observation-implementation-2026-09-13.md)에 남긴다. 배포·게임 재현이 요청된 작업에서는 활성 JAR 일치와 실제 필수 이벤트의 파일 기록을 확인한다.

로그 보강의 완료는 **도구 반복과 자동보관 실행/미실행 결정을 같은 사건으로 추적하고, 장시간 실행의 관측 한계를 드러내며, 기존 동작을 보존하는 것**이다. 반복 해결·자동보관 수정·금 채집 성공을 완료 주장에 포함하지 않는다.

이 사건은 앞서 작성한 [BuilderProcess 빈 이동 목록 크래시 계획](chatclef-baritone-builder-empty-movements-diagnostics-plan-2026-09-13.md)과 별개다. [캐시·과거 NPE](chatclef-baritone-cache-troubleshooting.md) 또는 이전 GOTO/FIND 사건의 원인을 이번 도구/보관 문제로 옮겨 설명하지 않는다. 기존 캐시 로드 기록만으로 staleness를 확정하거나 캐시를 변경하지 않는다.
