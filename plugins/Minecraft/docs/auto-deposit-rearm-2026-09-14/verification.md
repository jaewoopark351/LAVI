<!-- 20260914_kpopmodder: Documented planned auto-deposit rearm verification without changing or executing runtime code. -->
<!-- 20260914_kpopmodder: Replaced registration-count exhaustion with defense-first verification that preserves actual failure and no-progress evidence. -->

# 자동보관 재실행 검증 계획

상태: **IMPLEMENTED / LOCAL_VERIFICATION_PASS / LIVE_NOT_RUN**. 최종 자동보관 focused 테스트267/267개와 1.20.1 한정 clean·remap 검증을 통과했다. 실제 게임·배포·Mixin 검증은 실행하지 않았다. 이 문서는 [설계](design.md)의 검증 기준과 현재 결과를 구분하며, 정확한 명령·입력·산출물은 [구현 기록](implementation.md)에 기록한다. 기존 관측과 소스 근거는 [증거](evidence.md)에 보존하며, 그 기록으로 새 테스트·빌드·배포·게임 검증을 대신하지 않는다.

## 1. 검증 상태와 미확정 값

| 대상 | 현재 상태 | 해석 |
|---|---|---|
| 기존 로그와 현재 소스 대조 | READ_ONLY_REVIEWED | 기존 WAIT 유지 원인과 검토할 호출 경계를 확인한 증거다. 새 동작의 성공 증거는 아니다. |
| 기존 테스트 346개 통과 기록 | HISTORICAL_RECORD | 제공된 이전 검증 기록이다. 이번 문서 작업에서 재실행하지 않았으며 재실행 설계 검증으로 재사용하지 않는다. |
| 현재 소스·focused 회귀 작성 | IMPLEMENTED | 아래 실제 실행과 런타임 수락을 구분한다. |
| 테스트 JVM 실제 Bootstrap | PASS | 테스트 전용 접근성 설정에서 실제 레지스트리 1,255개 항목 확인. 게임 시작·Mixin 수락이 아니다. |
| 최종 자동보관 focused 회귀 집합 | PASS | 시작267·성공267·실패0·aborted0·skipped0·containerFailures0. 아래12개는 경계 그룹 수이며 테스트 메서드 수가 아니다. |
| 최종 후보 Java 1.20.1 한정 clean·remap | PASS | 최종 실행 exit0, 2분45초, 36개 task 실행. 입력2,130개 해시와 HEAD 불변. |
| `validateAccessWidener` task | NO-SOURCE | 검증할 소스가 없어 실행하지 않은 항목이며 access widener 검증 PASS가 아니다. |
| 버전 미지정 일반 clean build 전체 | NOT_RUN | 타 버전 Java 컴파일·산출물 task를 포함하지 않는 명령으로 검증했다. |
| 새 산출물과 배포 JAR 바이트 동일성 | NOT_RUN | 이전 manifest나 설치 경로만으로 새 배포를 확인했다고 쓰지 않는다. |
| 새 JAR의 게임 시작·Mixin 적용 | NOT_RUN | 빌드 성공과 별도 확인한다. |
| 같은 세션의 재실행·WAIT 복구·STOP·안전 선점 | NOT_RUN | Heartbeat, Idle 화면, 프로세스 재시작만으로 통과 처리하지 않는다. |

최종 근거는 [실행 로그](../../../../logs/auto_deposit_1201_Build_20260914_033306_455/verification.log), [입력 manifest](../../../../logs/auto_deposit_1201_Build_20260914_033306_455/source-manifest.json), [산출물 검사](../../../../logs/auto_deposit_1201_Build_20260914_033306_455/artifact-verification-final.json)다. 기본 클래스와 LAVI 클래스는 Java17 target(major61), 테스트 코드·agent 혼입0개, 타 Minecraft 버전 컴파일·산출물 task0개를 확인했다. Jackson의 기존 `META-INF/versions/19/` 클래스3개는 버전별 의존성으로 별도 기록하며 LAVI target 변경으로 취급하지 않는다. 전체 게임과 Mixin은 이 headless 검증의 대상이 아니다.

이전 문서의 **총 root 등록 2회 제한과 유한한 안전 재개 평가 횟수 소진으로 차단하는 제안은 철회**한다. 자동방어는 자동보관보다 항상 우선한다. 방어 선점 자체, 그에 따른 Task 재등록과 안전 재평가는 보관 실패·무진척 한도를 소비하지 않는다. 등록·선점 횟수를 관측할 수는 있지만 그 횟수를 보관 실패로 바꾸거나 재개 금지 조건으로 사용하지 않는다.

방어 전 실제 보관 실행에서 확인한 실패·진척·누적 무진척은 보존한다. 방어 대기 시간은 보관 실행·무진척 시간에 더하지 않는다. 안전 회복은 안전 조건만 해제하며 상자 부족·포화·접근 실패를 해결했다고 취급하지 않는다. 구현 후보는 실제 실행 12,000 tick, 연속 무진척 2,400 tick, 압력 과정당 기본 정리 단위 3개, 관련 조건 개선에 대한 최대 2회 추가 허가를 사용한다. 각 추가 허가는 6,000 실행 tick과 정리 단위 1개를 추가하며 과거 누적 실행·무진척 기록을 삭제하지 않는다. 정확한 측정과 소유 경계는 [구현 기록](implementation.md)의 상수 표를 따른다. **이 값은 소스 후보의 초기 한도이며 런타임 조정·검증은 NOT_RUN**이다. 한도 직전·도달·실패 원인과 관련된 실제 조건 변화를 검증해야 한다.

실패 뒤 재확인에는 원인과 관련된 실제 변화가 필요하지만 성공을 미리 증명할 필요는 없다. 접근 조건 변화나 새 목적지 후보를 제한적으로 확인한 뒤 같은 실패가 남으면 다시 대기한다. 아래 개선·회복 시나리오도 이 의미이며, 성공을 확인하기 전에는 아무 시도도 못 하게 하는 계약이 아니다.

## 2. 기존 테스트와 변경 대상 계약

아래 경로는 현재 저장소에 존재함을 읽기로 확인했다. 기존 기대값을 한꺼번에 새 기대값으로 치환하지 않고, 정상 결과·실패·선점·STOP을 구분해 확장한다.

| 기존 테스트 | 검토하거나 보존할 계약 |
|---|---|
| [DepositAllInventoryPressureStateMachineTest.java](../../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureStateMachineTest.java) | 기존 실행 종료 후 32칸 관측은 재무장하지 않고 28칸에서 재무장한다. 정상 relief의 32→33 허용은 의도적인 계약 변경이다. 실패·STOP에도 같은 완화를 적용하지 않는다. |
| [DepositAllInventoryPressureChainLifecycleTest.java](../../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureChainLifecycleTest.java) | 자연 종료의 자기 트리 정리, 안전 선점, never-ticked root, 비활성 runner의 시작, 월드 이탈·ChatClef 비활성화를 실제 chain 경계에서 검증한다. |
| [AutoDepositMaintenanceSlotReliefTest.java](../../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceSlotReliefTest.java) | 점유 관측으로 FULL/PARTIAL/NO_SLOT_RELIEF를 구분하는 기존 의미를 유지하고 typed 실행 결과와 연결한다. |
| [AutoDepositFreeSlotVerifierTest.java](../../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/maintenance/relief/AutoDepositFreeSlotVerifierTest.java) | 현재 읽기 불가를 시작 점유·0칸·NO_SLOT_RELIEF로 만드는 테스트가 있다. 실제 무진척과 UNAVAILABLE을 분리하는 계약으로 변경한다. |
| [AutoDepositMaintenanceChildRegistrationOrderTest.java](../../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceChildRegistrationOrderTest.java) | 기존 child 등록·lifecycle 연결을 보존한다. child의 단순 stopped를 정상 성공으로 대체하지 않는다. |
| [DepositAllStoreHomeConflictDecisionOrderTest.java](../../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/DepositAllStoreHomeConflictDecisionOrderTest.java) | 수동 store_home 충돌 판단 순서를 보존한다. |
| [DepositAllStoreHomeTaskRunnerHandoffTest.java](../../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/DepositAllStoreHomeTaskRunnerHandoffTest.java) | 수동 작업의 runner 소유권과 handoff를 보존한다. |
| [AutoDepositRuntimeTickSequenceTest.java](../../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/lifecycle/AutoDepositRuntimeTickSequenceTest.java) | 실제 tick 순서 및 composition 경계를 사용한다. 상태 조회를 위해 getPriority를 추가 실행하지 않는다. |
| [AutoDepositSurplusTargetSelectorTest.java](../../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/working/AutoDepositSurplusTargetSelectorTest.java) | 현재 working-set을 보호한 surplus 선택을 유지한다. |
| [AutoDepositTrustedDestinationSelectorTest.java](../../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/trusted/AutoDepositTrustedDestinationSelectorTest.java) | trusted 경로를 일반 보관으로 강등하지 않는다. |

새 테스트는 `.../deposit/auto/`의 `rearm/`, `budget/`, `maintenance/result/`, `admission/`, `lifecycle/`, `lifecycle/cleanup/` 등 실제 책임과 대응하는 폴더에 둔다. [격리된 focused 검증 설정](../../runtime/chatclef_fabric_1.20.1/src/test/autoDeposit/auto-deposit-tests.init.gradle)은 자동보관 회귀와 공통 테스트 지원 코드를 1.20.1 대상으로 컴파일하며 기존 Jupiter 실행기를 재사용한다. 기존 pressure·policy·working-set·trusted 회귀도 포함한다. 클래스 작성·컴파일·실행 성공은 각각 구분해 기록한다.

실제 Bootstrap의 named-package 접근 보완은 [테스트 전용 실행 환경](../../runtime/chatclef_fabric_1.20.1/src/test/autoDeposit/README.md)에 분리한다. 정확한 6개 메서드 접근성만 조정하며 원본 JAR·생산 코드·메서드 동작은 변경하지 않는다. `autoDepositVerifyMinecraftBootstrap`가 실제 초기화에 실패하면 전체 focused 검증도 실패해야 한다. 테스트의 실패·aborted·skip을 통과로 바꾸는 완화는 하지 않는다.

## 3. 반드시 검증할 자동 회귀 12개 경계

순수 정책 테스트만 통과시켜 완료하지 않는다. 결과 캡처·Task 시작·중단·runner 활성화·실제 실패와 무진척 기록은 실제 생산 호출 경계를 통과하는 focused integration으로 검증한다. 의미 있는 입력 차이를 주고 시작 횟수, 소유 Task, cleanup 결과와 동작 trace를 비교한다. 같은 실제 보관 실행 사이에 방어 선점만 추가한 비교에서는 실패·진척·누적 무진척의 의미가 같아야 한다.

| 번호 | 입력·경계 | 필요한 판정과 증거 |
|---|---|---|
| 1 | 기존 점유 기록인 시작 33→준비 관측 35→종료 30을 사용하되, 필수 자식 완료·working-set 충족·cleanup을 갖춘 유효한 정상 결과는 별도 회귀 fixture의 전제로 구성. 다음 pressure 관측은 곧바로 33이며 반복됨. | 종료 결과의 30을 보존하여 임계값 아래 관측을 잃지 않는다. 같은 종료 기준점의 재상승을 한 번만 새 기회로 소비하며, 단순 반복 관측으로 중복 시작하지 않는다. READY·제어·안전·보호 조건을 확인하고 방어 뒤의 재개는 9번 계약으로 구분한다. 개별 외부 획득 증명이 없어도 정상 재상승을 차단하지 않는다. 순감소는 3이며, 옛 DONE이 새 정상 완료 계약까지 증명했다고 쓰지 않는다. |
| 2 | 정상 결과의 32→33과, 이전 과정의 후속 실행 한도를 사용한 뒤 정상 완료28→33을 검증. 실패·중단 후 32→33 및 실패33→사용자 정리28→33은 별도 입력이며, 실패 목적지 불변·관련 조건 변화·무진척 한도 도달·STOP을 구분. | 정상 완료 후 33칸 미만에서 재상승하면 28칸 이하도 새 압력 과정의 한도를 적용한다. 이전 과정의 소진량만으로 정상 새 수요를 차단하지 않는다. 실패 후 low-water는 실패·누적 무진척·제어 차단을 지우지 않으며 같은 원인에는 해당 실행을 반복하지 않는다. 관련 변화는 제한적으로 재확인하고 특정 실패를 다른 유효 목적지에 확대하지 않는다. STOP→28→33에서 시작은 0회다. |
| 3 | 정상 부분 정리 36→34, 이후에도 34 이상 유지. 소량 전송은 계속되지만 신규 유입으로 점유가 줄지 않는 후속 정리 반복과, 실제 무진척·실패·중간 방어 선점을 분리. | 같은 압력 과정의 실제 실패·진척·무진척과 후속 실행 제한을 유지한다. 일부 전송이나 새 계획으로 새 압력 구간을 만들지 않으며, 진척이 있어도 실제 후속 단계의 유한 한도에 도달하면 추가 연속 실행을 제한한다. 방어 재개·root 재등록 자체는 후속 단계 소비가 아니며 기록을 초기화하지 않는다. 무진척 상한과 후속 단계 상한을 구분하고 방어 횟수만으로 차단하지 않는다. |
| 4 | 실제 전송이 있지만 준비 유입으로 signed net relief가 0 또는 음수인 경우, 필수 pressure/자식 결과 조회 불가, 선택적 전송 상세·서버 ACK만 없는 경우. | 전송 관측과 순감소를 분리한다. 음수·0을 전송 실패로 단정하거나 관측 불가를 실제 0·성공으로 위장하지 않는다. 필수 완료 정보가 UNKNOWN/UNAVAILABLE이면 정상 relief 기반 재무장은 불가하다. 동작 소유자가 확인한 유효한 정상 결과는 선택적 상세·서버 ACK 부재만으로 거부하지 않는다. |
| 5 | 새 동작의 같은 세션에서 과거 정상 결과가 없는 legacy/unknown WAIT + 36/36. READY, NO_SAFE, STOP, 실행 권한 불명확 및 평가 후 등록 전·첫 tick 전 선점의 각 분기. | legacy 상태의 최초 확인과 동일 실패 조건의 재확인을 구분한다. READY도 현재 안전·제어·보호 조건과 등록 직전 재검증을 통과해야 시작한다. NO_SAFE는 시작 0회다. 안전 재평가에는 횟수 소진 차단을 두지 않지만, 안전 회복만으로 동일 실패 조건의 복구 확인이나 실행을 반복하지 않는다. 과거 30/28 관측을 만들지 않는다. |
| 6 | NO_SLOT_RELIEF, 일반 상자 없음·가득 참·접근 불가, 판자/원목/상자 확보 정체. 첫 child 하나가 끝나지 않는 경우 포함. | 실제 실패·누적 무진척을 동작 소유자가 기록하여 정체를 끝낸다. 동일 실패 조건을 새 child로 포장하거나 방어 재개로 초기화하지 않는다. 목적지/원인 등 필요한 범위에 차단을 적용하고 원인과 관련된 실제 조건 변화 때 제한적으로 재확인한다. 같은 실패가 확인되면 다시 대기하며 자기 트리만 정리한다. 수동 보관·방어 동작을 변경하지 않는다. |
| 7 | child 재생성, general 단계 교체, onStart/Task.reset, Idle, 새 명령 root, plan epoch, fingerprint A↔B, 단축바·선택 손 변경, 방어 뒤 Task 재등록. | 각 변화 자체는 실패·무진척을 추가하지도 기존 기록을 지우지도 않는다. 같은 실행의 상자 준비·원목/판자 유입만으로 새 압력 구간이나 실패 회복을 만들지 않는다. 실제 진척·실패가 확인된 경우는 해당 동작 근거로 별도 반영한다. 유효한 정상 종료·cleanup 뒤의 재상승은 1번의 별도 경로다. |
| 8 | 실패/차단 대기에서 보호를 유지한 safe surplus 생성, 현재 working-set 변경, 보관소 수용·접근 조건 변화. 실패 상자 A와 별개의 후보 B, 변화 후 재확인도 실패하는 경우, 같은 변화 반복 관측, 안전 회복만 있는 경우. | 차단 원인과 관련된 실제 변화로 제한적으로 재확인하되 성공을 사전 보장하도록 요구하지 않는다. 같은 실패가 남으면 다시 대기하고 같은 기회를 반복 소비하지 않는다. 안전 회복·명령/working-set identity 변경만으로 실패를 해제하지 않는다. A의 실패는 필요한 범위에만 남기고 다른 조건이 유효한 B를 A 때문에 차단하지 않는다. 정상 재상승에 실패 회복 요건을 일괄 적용하지 않고 현재 working-set을 쓴다. |
| 9 | 등록 전, 등록됐지만 never-ticked인 root, 실행 중, 종료 직전에 자동방어·먹기 등 안전 선점. 실제 실패가 없는 반복 선점과 실제 실패·누적 무진척 사이에 삽입한 반복 선점을 각각 비교. | 자동방어가 항상 먼저 실행되며 기존 공격·회피·먹기를 유지한다. 활성화된 자기 트리의 cleanup은 한 번, never-ticked root는 기존 계약대로 detach하며 가짜 stop callback을 만들지 않는다. 방어의 무기 선택·입력·경로를 cleanup이 복원하거나 취소하지 않는다. 선점·재등록·안전 재평가 횟수로 실패 한도를 소비하거나 재개를 영구 차단하지 않는다. 반대로 기존 실제 실패·진척·누적 무진척을 지우지도 않는다. 방어 대기의 실행·무진척 시간 가산과 추가 getPriority 호출은 없다. |
| 10 | RUNNING·ARMED·각 WAIT 및 READY 판정 뒤 등록 직전에 명시적 STOP. 다음 tick, low-water, trusted revision 변경, 복구 permit, 명시적 재개를 순서대로 입력. | STOP 차단 중 새 자동 Task 등록·runner.enable은 0회다. 오래된 READY를 사용하지 않으며 정상 Idle과 명시적 STOP을 구분한다. 명시적 제어 재개가 허용되더라도 실패 예산 초기화와 동일시하지 않는다. |
| 11 | 수동 store_home/deposit 충돌, trusted child 실패, general child 중단, recovery 뒤 필수품 deficit, 도구·무기·장비·음식·필수 재료만 있는 상태. | 충돌 중 자동보관 시작 0회, trusted→일반 강등 0회, 보호 해제·임의 폐기 0회다. 빈칸이 늘어도 child 실패나 필수품 부족을 정상 결과로 재분류하지 않는다. 수동 작업 종료만으로 실행 허가를 발급하지 않는다. |
| 12 | 자연 완료 후 setTask(null) cleanup, stopped 경유 onTaskFinish, 같은 정리 단위를 재개한 새 root와 이전 root의 늦은 결과, world/session 교체. 같은 입력을 diagnostics OFF·출력 예외·한도 소진으로 반복. | typed terminal은 한 번 확정되고 cleanup이 정상 결과를 CANCELLED로 덮어쓰지 않는다. 같은 목적이어도 root 세대는 구분하여 이전 콜백이 새 root를 종료·재무장하지 못하게 한다. 이전에 확정한 기록은 재개 전에 한 번만 계승하며 중복 가산하지 않는다. 세션 교체는 취소·무효화로 기록한다. 진단 상태와 무관하게 실제 시작·정리 횟수, STOP, 예산·상태 전이와 생존 행동 trace가 같다. |

5·9·10번은 다음 순서로 검증한다. 등록 횟수의 한도가 아니라 실제 보관 실행에서 생긴 실패·진척·무진척의 보존과 현재 제어 조건을 비교한다.

| 사건 순서 | 보존할 기록과 다음 판정 |
|---|---|
| 현재 계획 확인 → 등록 전 안전 선점 | 방어를 우선한다. 보관이 실행되지 않았다면 선점 자체의 추가 실패·무진척은 0이다. 안전 회복 뒤 현재 제어·계획·기존 실패 조건을 확인하되 안전 회복을 실패 해소로 사용하지 않는다. |
| root 등록 → 첫 tick 전 선점 → 안전 회복 → 같은 경계에서 반복 선점 | 보관 실제 실행이 없으면 등록·선점 횟수가 늘어도 그 때문에 실패·무진척을 추가하지 않는다. 방어 횟수만으로 영구 차단하지 않고 현재 조건이 유효해졌을 때 재개할 수 있다. 이미 존재하던 실패 기록은 유지한다. |
| 정리 시작36→보관32→방어→재개32→정상 완료32 | 필수 정상 완료 조건이 유효한 fixture에서 같은 정리 단위의 감소량은 4다. 개별 root 관측은 각각 36→32와 32→32로 남긴다. 재개 root의 0만으로 전체를 무진척 처리하거나 이전 4칸을 두 번 합산하지 않는다. 이후 재충전33은 정상 새 수요다. |
| 정리36→34 정상 종료→별도 후속 정리34→34 | 같은 압력 과정의 제한은 유지하지만 후속 단위의 순변화는 0이다. 이전 단위의 2칸 감소를 후속 단위의 진척으로 다시 세어 무진척 제한을 피하지 않는다. |
| 실제 보관에서 실패·무진척 확인 → 방어 선점 → Task 재등록 검토 | 방어 전 실제 실패와 누적 무진척을 그대로 대조한다. 안전 회복만으로 같은 상자/원인에 대한 차단을 해제하거나 무진척 기록을 0으로 만들지 않는다. 실제 실행 정체는 Task를 바꿔도 이어서 제한한다. |
| 실패 상자 A가 유지됨 → 안전 회복 → A의 수용 조건 개선 또는 별개의 유효 상자 B 확인 | 안전 회복만 있을 때는 A를 다시 시도하지 않는다. 실제 관련 개선은 제한적으로 재확인하며, B는 A의 실패를 전역 차단으로 확대하지 않고 현재 보호·제어·안전 조건에 따라 판단한다. |
| READY → 등록 직전 STOP | STOP 이후 새 등록·runner.enable은 0회. READY나 안전 회복이 실행 권한을 대체하지 못하며 STOP은 방어 선점과 구분한다. |

STOP 뒤 재활성화는 수정 전 소스의 `startTask → runner.enable`과 WAIT/ARMED의 지속 차단 부재로 확인한 **가능 경로**다. 기존 로그에서 실제 STOP 뒤 재활성화가 발생했다고 확인한 것은 아니다. 후보 구현은 별도 STOP 제어와 정리 단위 취소를 연결하며, 10번 테스트에서 실제 호출 경계를 검증한다.

### 3.1 현재 구현의 추가 확인 경계

아래는 위 12개 그룹을 후보 구현의 실제 호출 경계에 연결한 추가 기준이다. 별도 성공 건수로 합산하지 않으며, 최종 suite의 결과와 테스트 선택 목록으로 확인한다.

| 입력·경계 | 검증할 결과 |
| --- | --- |
| 후보 종료32 → cleanup 중 cursor 복귀 → 최종 종료33 | 후보32는 불변으로 보존한다. 최종 결과는33을 사용하고 잘못된 임계값 하회·새 수요 근거를 만들지 않는다. |
| 정상/REPLAN_REQUIRED 후보 → cleanup 뒤 필수품 부족·조회 불가·점유 범위 불일치 | 최종 비성공 이유를 보존한다. 후보 자체를 사후 수정하거나 조회 불가를0·충족으로 대신하지 않는다. 이미 확정한 실패를 나중의 정상 관측으로 성공으로 바꾸지 않는다. |
| 중단 최종 필수품 SATISFIED/NOT_APPLICABLE → 방어 중 음식 소비 | 과거 예약은 정산된 상태다. 나중의 생존 소비를 과거 보관의 회수 부족으로 만들지 않고 현재 계획에 반영한다. |
| 원래 예약 DEFICIT → 안전 회복 → 회수 전용 새 root → 회수 완료 | 원래 snapshot·manifest·사용자 root·월드·차원·cleanup을 검증한다. 옛 슬롯 자식 재사용과 새 보관 선실행은0회다. 회수 후 REPLAN_REQUIRED로 현재 계획을 재평가하며 보관 성공·후속 단위 완료로 오인하지 않는다. |
| 회수 중 재선점, 회수 실패, manifest/월드/사용자 root 불일치 | 새 회수 자식을 사용하면서 원래 출처와 실제 실패·누적 한도를 유지한다. 불일치하거나 증거를 읽지 못한 경우 실제 부족·성공으로 대체하지 않는다. STOP/새 사용자 root 취소는 원래 실패 이력과 소비 한도를 유지한다. |
| 부모 tick → 자식의 실제 저장/회수 → 다음 부모 tick 전 선점 | 마지막 확인된 수량을 종료 경계에서 한 번 반영한다. 저장량과 회수량은 별도이며 회수를 저장된 물품·빈칸 확보로 합산하지 않는다. 회수 전용 root도 실제 실행 tick 한도를 사용하지만 REPLAN_REQUIRED 자체는 완료 단위를 소비하지 않는다. |
| 음식·장비 착용/손 이동/소비/내구도 변화와 실제 안전 surplus 증가 비교 | main·armor·offhand 합계를 사용하여 기존 수량 변화와 이동을 구분한다. 기록한 최댓값을 넘고 현재 보호 정책을 통과한 보관 대상일 때 관련 물품 실패를 제한적으로 재확인한다. 목적지 실패는 이 증가만으로 해제하지 않는다. |
| 안전 물품 수량 감소→기존 최고 수량 복귀, 새 Task·방어 재개·압력 과정 교체,64종 추적 한도 | 무관한 변화로 획득 근거나 추가 허가를 반복 발급하지 않는다. 새 압력 과정의 정상 한도와 물리적인 조건 개선을 구분하며, 추적 포화는 거짓 증가 근거가 아니다. |
| WORKING_SET_DEFICIT 최종 결과 → 원래 예약 캡처 → 보호된 음식·장비를 일부/전체 복구 | 최종 부족 결과를 받은 chain이 원래 reservedCounts를 캡처한다. 같은 월드·차원·사용자 root의 main+cursor에서 전체 예약이 복구됐을 때만 조건이 바뀐다. 일부 회복·조회 불가·새 계획의 요구량 축소·armor 이동만으로 완전 복구를 만들지 않는다. |
| 원래 필수품 복구 후 재확인 → 재소비·복귀 반복, 다른 root/월드/차원, 64종 예약 한도 | 동일 복구 조건의 추가 허가를 반복 소비하지 않는다. 컨텍스트가 다르거나 예약 근거가 없으면 복구 키가 없으며, 일반 surplus·목적지 실패의 키는 이 근거로 바뀌지 않는다. |
| 기록된 WORKING_SET_DEFICIT → 같은 월드의 다른 사용자 root → 원래 예약 증거 없음 | UNKNOWN_CONDITION이며 새 실행·복구 허가·추가 허가 소비·실패/한도 초기화가 없다. 월드 교체의 기존 컨텍스트 초기화 경로를 이 경우와 혼동하지 않는다. |

## 4. 실제 게임 시나리오

다음은 향후 구현·빌드와 해당 배포·런타임 작업이 요청된 뒤 실행할 시나리오다. 현재 수행 상태는 모두 **NOT_RUN**이다. 테스트 때문에 자동방어를 끄거나 기존 세계·Baritone 캐시를 임의로 수정하지 않는다.

| 시나리오 | 관측과 통과 기준 |
|---|---|
| 실행 JAR 확인 | 실제 활성 인스턴스·세션·Minecraft 1.20.1을 확인하고 새 빌드 산출물과 설치 JAR을 바이트 해시로 대조한다. 시작/Mixin 로그, 배포 동일성, 게임 동작 결과를 각각 기록한다. |
| 정상 relief 뒤 다음 보관 | 33→준비 증가→30→재충전 33/36과 정상 완료28→재충전33을 구분해 확인한다. 같은 세션에서 정상 결과·cleanup과 새 압력의 판단·한도를 연결하며 이전 과정의 소진량만으로 새 수요를 막지 않는다. READY가 아니면 해당 차단 이유를 기록하며 보관 시작 자체를 무조건 요구하지 않는다. |
| 고점유 부분 정리 | 36→34 이후 소량 전송·신규 유입으로 고점유가 계속되는 경우와 무진척을 구분한다. 실제 후속 단계의 한도도 유한하며 전송·새 계획으로 초기화되지 않아야 한다. 방어 재개 자체는 후속 단계로 소비하지 않는다. 실제 한도 종료 뒤 관련 조건이 그대로면 상자/재료 확보가 되살아나지 않아야 한다. |
| 실패와 보호 | 보호 물품뿐인 경우와 일반/신뢰 상자 부족·가득 참·접근/확보 불가를 분리한다. 원인·목적지별 대기와 실제 개선에 따른 제한적 재확인, 별개의 유효 보관소와 protected/trusted 정책 보존을 확인한다. |
| 자동방어·먹기 | 실제 적대 몹에 대한 공격·회피가 보관보다 우선하고 먹기가 유지되는지 확인한다. 자기 트리 정리가 방어의 무기·입력·경로를 되돌리지 않아야 한다. 반복 선점만으로 차단되지 않고, 실제 실패·진척·누적 무진척은 보존하며 방어 대기 시간은 제외한다. |
| STOP와 수동 보관 | RUNNING·ARMED·각 WAIT에서 STOP한 뒤 low-water·정책 변경이 생겨도 자동 시작하지 않는지 확인한다. 수동 store_home/deposit, 정상 Idle, 명시적 재개를 별도 단계로 기록한다. |
| 로그 독립성 | 동일한 제어 입력·재현 조건에서 진단 OFF 또는 억제 상태가 재시도 허가·예산·terminal·방어 결과를 바꾸지 않는지 비교한다. 필수 사건 누락은 관측 한계로 기록한다. |

전송 증거는 아래 수치를 각각 기록한다. 이번 기록의 **전송으로 빈 소스 슬롯 5개**와 **시작 대비 순감소 3칸**은 모순되지 않는다.

```text
시작 점유 33 → 전송 전 PLAYER 슬롯 관측 35 → 종료 점유 30
클라이언트가 관측한 전송 소스 슬롯 변화: 5개
시작 대비 순감소: 33 - 30 = 3칸
서버 ACK: 관측되지 않았다면 UNKNOWN / NOT_OBSERVED
```

새 재현이 이 숫자와 달라지면 실제 값을 보존한다. `actualFreedSlots`를 전송 개수에 맞춰 사후 수정하거나 FULL_RELIEF로 바꾸지 않는다. PLAYER 슬롯과 상자 슬롯을 같은 점유 합계에 섞지 않는다.

## 5. 같은 세션 복구와 재시작 구분

| 경계 | 검증할 내용 | 성공으로 대체할 수 없는 것 |
|---|---|---|
| 현재 옛 JAR의 실행 중 WAIT36 | 현재 동작에서는 실제 low-water 또는 trusted 정책 변경 등 기존 해제 조건을 따른다. 새 소스 작성이 실행 중 메모리를 바꾸지 않는다. | 새 코드의 같은 세션 복구 증거가 아니다. 의미 없는 trust/untrust 반복을 상시 우회책으로 제시하지 않는다. |
| 새 동작 안에서 같은 세션의 legacy/unknown WAIT36 | 같은 런타임 인스턴스에서 과거 정상 결과를 만들지 않고 최초 상태 확인·제어 허가·실제 실패 조건과 제한적 재확인을 검증한다. 안전 재평가 횟수와 실패 한도를 혼용하지 않는다. 구현 후 유효한 진입/주입 경계와 그 근거를 기록한다. | 자동 hot reload나 기존 상태 마이그레이션 지원을 전제하지 않는다. 프로세스가 바뀌었다면 이 항목은 NOT_RUN이다. |
| 새 JAR로 프로세스·세션 재시작 | 새 state machine의 초기 상태와 새 세션 소유권을 확인한다. 이전 run 결과가 새 작업에 영향을 주지 않아야 한다. | 초기 ARMED에서 시작한 것을 같은 세션 WAIT 복구 또는 과거 보관 성공으로 표시하지 않는다. |

## 6. 상태별 증거와 완료 판정

상태 이름과 이벤트 이름은 [설계](design.md)에서 확정할 동작 계약에 연결한다. 아래 의미 구분을 진단 문자열만으로 복원하거나 진단 객체를 행동 입력으로 사용하지 않는다.

| 의미상 상태·결과 | 남겨야 할 최소 증거 |
|---|---|
| 정상 relief 뒤 압력 상승 대기 | 압력 과정·정리 단위·개별 root 세대의 결합, 정상 terminal과 working-set 충족 여부, 보존된 원래 시작·유효한 최종 종료 pressure, 다음 임계값 관측·소비 여부. 개별 root 관측값은 별도로 유지. |
| 고점유 후속 평가·실행 | 정상 결과와 현재 pressure, 같은 압력 과정의 실제 실패·진척·누적 무진척, 실제 후속 단계 소비·잔여량, 현재 계획과 실행 허용/거부 이유. 방어 재개·root 재등록만으로 새 압력이나 실패·후속 단계 소비를 만들지 않았다는 연결. |
| legacy/unknown WAIT 복구 | 과거 결과 UNKNOWN, 현재 제어 권한, 최초 확인과 관련 조건 개선에 따른 재확인 구분, 계획의 READY/거부·UNAVAILABLE 여부. |
| 무진척·보관소 실패·실제 한도 도달 | typed 원인과 목적지 등 차단 범위, 관측된 순변화와 가용성, 실제 실패·무진척 누적 근거, 한도 종료·cleanup, 동일 조건의 반복 실행이 없다는 관측 범위와 실제 개선 후 재확인 근거. |
| 안전 대기·STOP·수동 충돌 | 서로 구분된 원인, 정상 제어/선점 경계와 tick, 자기 Task 정리와 방어의 무기·입력·경로 보존. 방어 대기 시간을 제외했고 선점 자체가 실제 실패 기록을 늘리거나 지우지 않았다는 비교. STOP의 명시적 재개와 단순 안전 회복은 별도 기록. |
| 정상 완료·취소·늦은 결과 | 결과 확정 시점과 정확한 소유권, cleanup 완료, 중복/이전 run 결과의 거부 이유. |
| 관측 누락·진단 억제 | 미평가 NOT_EVALUATED, 조회 불가 UNAVAILABLE, 출력 실패·한도 억제와 실제 관측 범위. 로그 누락을 행동 미실행·성공으로 단정하지 않는다. |

필요한 최초 상태 전이·정확한 결정 사유·terminal은 반복 요약과 구별되는 bounded 출력 근거를 남긴다. 누락된 필수 사건을 억제 요약으로 통과 처리하지 않는다. 반대로 출력 성공·실패가 동작을 선택하거나 재시도를 허용해서는 안 된다.

완료 보고에서는 자동 테스트, clean build, 설치 JAR 동일성, 게임 시작/Mixin, 각 실제 시나리오를 별도 결과로 적는다. 실행하지 못한 항목은 **NOT_RUN**, 읽기/관측만으로 확정하지 못한 원인이나 값은 **UNKNOWN/UNRESOLVED**로 남긴다. 문서 검수 완료는 이 구현과 게임 검증의 완료를 뜻하지 않는다.
