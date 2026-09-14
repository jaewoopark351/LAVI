<!-- 20260914_kpopmodder: Record the requested automatic-deposit implementation candidate and separate source, build, and live evidence. -->
# 자동보관 재실행 구현 기록

상태: **IMPLEMENTED / LOCAL_VERIFICATION_PASS**. 최종 소스 후보의 자동보관 focused 테스트 **267/267개**와 1.20.1 한정 clean·remap 산출물 검증을 통과했다. 최종 빌드 전후 입력 파일 2,130개의 해시와 HEAD가 일치한다. 배포·게임 시작/Mixin·실제 게임 검증은 **NOT_RUN**이다. 아래 실제 실행 기록은 이전 테스트·JAR 기록이나 테스트 JVM의 Bootstrap 통과와 구분한다.

[설계](design.md)와 [증거](evidence.md)는 구현 전 요구사항과 관측을 보존한다. 그 문서에 남은 `NOT_IMPLEMENTED`나 미확정 수치가 현재 작업을 문서 전용으로 제한하지 않는다. 아래 선택은 사용자 구현 요청 범위의 소스 후보 결정이며, 과거 런타임 원인이 새 코드로 검증됐다는 뜻이 아니다.

## 작업과 변경 경계

대상은 `C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1`의 Fabric ChatClef 자동보관이다. 구현·필요한 회귀 테스트·bounded 로그·문서와 저장소 내부 검증을 수행한다. 빌드 대상은 1.20.1로 한정하며 dependency, Java target, Minecraft/Fabric/ChatClef 버전, 사용자 설정을 바꾸지 않는다.

외부 CurseForge 인스턴스 복사, JAR 배포, Minecraft 실행·게임 조작·월드 변경·캐시 변경, commit/push는 이 구현 작업에서 수행하지 않는다. 원본 로그와 이전 산출물도 수정하지 않는다. 새 소스 후보의 검증 산출물은 저장소 안의 별도 실행 기록으로 남긴다.

## 종료 결과와 재실행 연결

`AutoDepositRunResult`는 종료 이유, 개별 root 시작·종료 점유, 필수 자식 완료, working-set 상태, cleanup 결과, 기존 relief 분류를 불변 값으로 보존한다. 점유 조회 불가·서로 다른 슬롯 범위는 유효한 0칸 변화로 바꾸지 않는다. cleanup 전 후보는 불변으로 남기고, 자기 트리 cleanup 뒤 점유와 필수품을 다시 관측하여 별도의 최종 결과를 한 번 확정한다. cursor의 물품이 cleanup 중 main으로 돌아오면 이 최종 점유에 반영한다. 정상 또는 `REPLAN_REQUIRED` 후보여도 최종 필수품이 부족·조회 불가이거나 점유 범위를 비교할 수 없으면 그 사실에 맞는 비성공 결과가 된다. 기존 실패 이유를 일반 종료 callback이나 나중의 정상 관측으로 성공으로 바꾸지는 않는다.

상위 `AutoDepositRunLedger`는 정확한 Task 객체와 현재 root를 대조한다. 같은 정리 단위의 원래 시작 점유와 압력 과정의 실행 기록은 방어 재개에도 유지한다. 이전 root의 결과가 현재 root와 일치하지 않으면 정리 완료나 재무장에 반영하지 않는다. 후속 정리 단위는 새 시작 점유를 사용한다.

중단 경계의 최종 필수품 유지가 `SATISFIED` 또는 `NOT_APPLICABLE`로 확정됐다면 해당 root의 예약은 정산된 것으로 보존한다. 이후 방어 중 음식 소비 등을 과거 보관의 미복구 물품으로 다시 계산하지 않는다. `DEFICIT`·`UNAVAILABLE`·`NOT_EVALUATED`라면 원래 root의 예약을 보존하고, 재개 전에 그 예약의 충족을 확인한다. 조회 불가는 실제 부족이나 충족으로 대신하지 않는다.

실제 부족이 확인되면 새 보관을 먼저 실행하지 않고 **회수만 수행하는 새 root**를 만든다. `AutoDepositRecoveryResumeState`는 원래 필수품 snapshot·보관 출처 manifest·이전 자식 완료 증거를 보존하고 월드·차원·사용자 root·이전 cleanup을 검증한다. 옛 슬롯 자식을 재사용하지 않으며, 현재 검증용 계획의 전송 대상을 비워 일반·trusted 보관이 먼저 실행되지 않게 한다. 기존 회수 경로가 부족을 복구한 뒤 자기 트리를 정리하면 `REPLAN_REQUIRED`로 같은 정리 단위를 넘긴다. 이는 보관 성공이나 별도 후속 단위 완료가 아니다. 다음 선택 경계에서 현재 계획과 필수품을 다시 계산하고, 원래 보관 자식까지 끝났다면 최종 검증만, 남은 보관이 있다면 유효한 새 계획을 실행한다. 회수 실패·조회 불가·컨텍스트 변경도 각각 보존한다.

현재 working-set 재검사는 자동보관이 실제 선택된 뒤에도 수행한다. 기존 resolver의 사용자 체인 선택 요구를 전역 해제하지 않고, 호출한 정확한 자동보관 owner가 선택된 경우만 허용하는 overload를 사용한다. 사용자 root가 그대로이고 stopped·Idle 상태가 아니며, cached path의 모든 항목이 현재 root의 실제 자식 트리에 속하는지 검증한다. 이전 root나 끊어진 자식의 cached 요구를 새 계획에 사용하지 않는다.

`AutoDepositRearmPolicy`는 정상 완료 뒤 33칸 미만의 종료 점유를 보존하고, 다음 33칸 이상 관측을 새 수요로 처리한다. 정상 low-water인 28칸 이하도 포함한다. 여전히 33칸 이상인 부분 정리는 같은 압력 과정의 제한 안에서 후속 평가한다. 실패 뒤 low-water·새 계획·Task 교체·무기 교체로 실패 이력과 실행 한도를 지우지 않는다.

실패 재확인 조건과 현재 계획의 정확한 대상은 구분한다. 상자 부족·포화·접근 등 목적지 관련 실패는 해당 일반/trusted 목적지의 관측으로 재확인하며, 음식이나 무기를 얻었다는 이유로 상자 실패를 해제하지 않는다. 보관 가능 물품 부족·정리 효과 없음처럼 물품 변화가 관련된 경우에는 일반 물품 수량·현재 요구와 별도로 안전한 음식·장비 surplus의 증가 근거도 사용할 수 있다.

음식·장비 증가 근거는 main·armor·offhand 합계가 같은 압력 과정에서 기록한 최댓값을 넘고, 그 물품이 **현재 보호 정책을 통과한 보관 대상**일 때만 낸다. 장비 착용·손 교체·내구도 변화·음식 소비·기존 수량으로 회복은 새 획득 근거가 아니다. Task·계획·방어 재개로 최댓값을 초기화하지 않으며, 새 압력 과정 자체도 물리적인 조건 개선으로 만들지 않는다. 물품 추적은 64종으로 제한하고 한도를 넘었다고 임의의 새 증가 근거를 발급하지 않는다. 이 수량 관측은 외부 획득 경로를 입증했다는 주장과는 다르다.

**`WORKING_SET_DEFICIT`로 실제 종료된 뒤의 필수품 복구는 별도 근거**를 사용한다. 종료 소유자인 chain이 최종 부족 결과를 받은 순간 `captureWorkingSetFailure(maintenance.snapshot())`로 실패한 원래 `reservedCounts`를 보존한다. `AutoDepositWorkingFailureEvidence`는 같은 월드·차원·사용자 root에서 원래 예약 전체가 복구됐는지 확인한다. 비교 수량은 실제 working-set 검증과 같은 **main + cursor**이며, 위의 surplus 증가용 main·armor·offhand 합계와 섞지 않는다. 보호된 음식·장비도 원래 부족분이 모두 돌아왔다면 제한된 재확인의 관련 근거가 될 수 있다. 현재 계획이 부족한 물품을 요구 목록에서 줄이거나 없앤 것만으로 복구됐다고 판정하지 않는다. 일부 회복·조회 불가·컨텍스트 불일치는 전체 복구 근거가 아니며, 반복 소비·복귀로 같은 허가를 다시 발급하지 않는다. 원래 예약은 64종으로 제한하고 이 근거는 일반 물품 변화나 목적지 실패의 조건 키를 바꾸지 않는다.

같은 월드라도 사용자 root가 바뀌어 기록된 `WORKING_SET_DEFICIT`의 원래 예약과 현재 관측을 결합할 수 없으면 조건은 `UNKNOWN_CONDITION`이다. 이를 새 복구 근거나 허가로 바꾸지 않으며, 이 조건만으로 실패 기록 초기화·추가 허가 소비·새 실행을 하지 않는다. 월드 자체가 바뀐 경우의 기존 컨텍스트 초기화 경로는 이 판정과 별개다.

일반 보관에서는 기존 tracker가 실제 확인한 target 충족과 저장 수량을 읽는다. `DepositAllTask`에 추가하는 `automaticStoredTargetsSatisfied()`와 `automaticStoredCount()`는 이를 전달하는 좁은 getter이며 기존 일반 명령의 `isFinished()`, 상자 선택, 이동·배치 정책을 바꾸지 않는다. 전송 개수·종료 점유·working-set 유지가 서로 다른 증거라는 계약을 유지한다. 서버 ACK가 없다는 이유만으로 정상 소유자가 확인한 결과를 취소하지 않으며, 반대로 ACK가 관측됐다고 꾸미지도 않는다.

## 실행 선택과 자동방어

통합 경계는 다음 순서로 연결한다.

1. END_CLIENT_TICK에서는 현재 조건으로 자동보관 후보를 평가하고, 실행 가능한 경우 priority 51 후보임을 표시한다. 이 단계에서 실행 root를 미리 등록·tick하지 않는다.
2. 정상 `TaskRunner`가 각 체인의 기존 `getPriority()`를 한 번씩 평가한다. 기존 자동방어·먹기 등의 부수 동작을 재호출하지 않는다.
3. 자동보관이 실제로 선택된 `onTick`에서 현재 실행권·수동 보관·working-set·보관 계획을 다시 검사한다. 그 뒤에만 정리 단위와 root를 결합하고 실행한다.
4. 방어·생존 체인이 선택되거나 기존 cached 생존 입력 요구가 있으면 자동보관이 양보한다. 안전해진 뒤 현재 상태로 재평가하고, 방어 대기·등록 횟수는 실패나 무진척 한도로 소비하지 않는다.

`MobDefenseChain`은 priority 0에서도 force-field 공격·방패 입력을 수행할 수 있고, `FoodChain`은 먹기를 수행한 뒤 음의 무한대 priority를 반환할 수 있다. 따라서 선택 체인 이름이나 수치만으로 안전을 판단하지 않으며 `AutoDepositSafetyAdmission`에서 기존 cached 방어·화재·먹기·후렴과 사용 상태도 확인한다.

STOP은 `AutoDepositExecutionControl`에 유지한다. 정상 초기 Idle과 명시적 STOP을 구분하고, STOP 뒤 자동보관 자신의 `runner.enable()`로 차단을 해제하지 않는다. 기존 사용자 명령 경로의 runner 재활성화를 관측한 경우만 제어 재개로 다룬다. 이 관측은 runner의 stop/disable 처리가 끝난 정상 tick 경계에서 수행한다.

명시적 STOP이나 실제 사용자 root 교체는 이전 정리 단위를 취소한다. 새 명령을 과거 root의 필수품·시작 점유에 묶지는 않되, 이미 소비한 실행·무진척 한도와 확인된 실패 이력은 유지한다. 방어·자동화 비활성화로 잠시 양보한 경우는 같은 정리 단위의 중단으로 구분한다.

## 소유 트리 정리와 생존 입력 보존

현재 엔진 호출 순서는 `전체 priority 평가 → 이전 체인 onInterrupt → 선택 체인 tick`이다. 방어는 priority 평가 안에서 무기·입력을 이미 선택하지만, 새 방어 이동 자식은 선택 체인의 tick에서 실행한다. 기존 자동보관 자식의 `onStop()`은 Baritone path 취소·입력 해제를 수행할 수 있다. cleanup을 다음 tick까지 미루면 새 방어 path를 취소할 위험이 있다.

`AutoDepositSurvivalCleanup.run(mod, cleanup)`은 이 동기 handoff에서 기존 자기 트리를 한 번 정리하고 이미 확인된 생존 입력 상태만 보존한다.

| 기존 생존 소유자의 상태 | 보존 대상 |
| --- | --- |
| 방패 사용 | 현재 SNEAK·CLICK_RIGHT 키 상태 |
| 화재 제거 | 현재 Baritone CLICK_LEFT 강제 상태 |
| 먹기·후렴과 사용 | 현재 CLICK_RIGHT 키 상태 |

그 밖의 이동 키·기존 보관 키·이전 무기 슬롯·이전 path는 복원하지 않는다. 키 복원은 `KeyBinding.setPressed`만 사용하며 새 key-press 이벤트를 발행하지 않는다. cleanup 예외는 삼키지 않고, 이미 캡처한 생존 입력은 `finally`에서 복원한다. 이 경계의 자동 테스트는 실제 게임의 방어·이동 수락 증거와 구분한다.

## 소스 후보의 한도와 측정 경계

수치 소유자는 [AutoDepositExecutionBudget.java](../../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/budget/AutoDepositExecutionBudget.java)다. 아래 값은 기존 trusted 작업의 6,000 tick·후보 정체 2,400 tick 범위를 참고한 초기 후보이며, 실제 Minecraft에서 조정하거나 검증한 값이 아니다.

| 상수 | 값 | 실제 의미 |
| --- | --- | --- |
| `BASE_EXECUTION_TICKS` | 12,000 | 같은 압력 과정에서 실제 선택된 자동보관 실행 tick의 기본 누적 상한 |
| `NO_PROGRESS_TICKS` | 2,400 | 실제 실행에서 확정된 저장·필수품 회수 진척 없이 이어진 tick 상한. 방어 대기 제외 |
| `BASE_COMPLETED_UNITS` | 3 | 최초 정리를 포함한 기본 정리 단위 종료 수 상한. root 등록·방어 재개 횟수가 아님 |
| `MAX_RECOVERY_GRANTS` | 2 | 실패 원인과 관련된 서로 구분되는 변화에 대한 추가 허가의 최대 수 |
| `RECOVERY_EXECUTION_TICKS` | 6,000 | 추가 허가 1개가 더하는 누적 실행 한도. 정리 단위 상한도 1개 증가 |

한도를 모두 확장해도 압력 과정의 총 실제 실행 상한은 24,000 tick, 종료 정리 단위는 5개다. 같은 조건의 반복 관측이나 A↔B 전환으로 추가 허가를 반복 발급하지 않는다. 다른 유효 목적지와 실제 수용·surplus 조건 변화의 의미는 재평가 입력에서 구분하며, 단순 Task·선택 슬롯 identity를 허가로 쓰지 않는다.

진척은 기존 동작 소유자가 확인한 저장 수량 증가와 **별도로 확인한 필수품 회수 수량 증가**에서 얻는다. 회수를 저장 전송량이나 확보한 빈칸으로 합치지 않는다. 부모 tick 뒤 자식이 전송·회수하고 다음 부모 tick 전에 선점된 경우도 종료 경계에서 마지막 확정 수량을 비교하므로 진척을 놓치거나 두 번 가산하지 않는다. 의미 있는 진척 또는 원인 관련 재확인 허가는 연속 무진척 구간의 재평가 근거가 될 수 있지만, 이미 사용한 총 실행 tick·총 무진척·종료 정리 단위 이력을 삭제하지 않는다. 방어 선점·회수 후 `REPLAN_REQUIRED`·재등록 자체는 정리 단위 종료 한도를 소비하지 않는다. 회수 root가 실제로 행동한 tick은 같은 과정의 실행 한도에 포함한다. 상자·재료 확보가 끝나지 않는 첫 자식도 실제 누적 실행과 무진척 상한의 적용 대상이다.

## 책임별 파일·폴더

| 경계 | 책임 |
| --- | --- |
| `deposit/auto/admission/` | 현재 실행권, 수동 보관 충돌, 현재 working-set에 따른 계획 입장 |
| `deposit/auto/admission/conditions/`, `conditions/gain/` | 실패 원인별 조건 관측과 안전한 음식·장비 수량 증가 근거 |
| `deposit/auto/admission/conditions/working/` | 실제 필수품 부족으로 끝난 원래 예약과 main+cursor 전체 복구 근거 |
| `deposit/auto/rearm/` | 정상 새 수요·같은 정리 재개·실패 관련 재평가와 목적지/조건 이력 |
| `deposit/auto/budget/` | 실제 실행·무진척·정리 단위·추가 허가 한도 |
| `deposit/auto/lifecycle/` | STOP 제어, 생존 요구 관측, 정확한 root 결과 연결 |
| `deposit/auto/lifecycle/cleanup/` | 생존 입력 캡처·불변 값·복원·native port·cleanup 호출의 분리 |
| `deposit/auto/maintenance/result/` | 불변 결과와 한 번의 확정 |
| `deposit/auto/maintenance/child/` | 기존 자식 생성·이미 결정된 완료 이유 해석 |
| `deposit/auto/maintenance/relief/`, `deposit/auto/maintenance/working/` | 점유 비교의 관측 가능성, working-set 유지 검증, 원래 회수 근거를 새 회수 root에 전달 |
| `deposit/auto/working/` | 현재 선택·실제 사용자 트리에 결합된 working-set 재검사 |

기존 엔진 `TaskRunner`, `Task`, `SingleTaskChain`, 자동방어 클래스는 재작성·분해하지 않는다. upstream-derived 클래스의 변경은 보관 tracker getter 같은 필요한 연결에 한정한다. 새 LAVI 코드의 독립 책임은 기존 또는 위의 의미 있는 폴더 안에서 분리한다. 진단 정보와 출력 성공 여부를 재시도·정상 완료·cleanup의 동작 입력으로 사용하지 않는다.

## 검증과 남은 확인

| 단계 | 상태 | 완료에 필요한 증거 |
| --- | --- | --- |
| 소스·테스트 작성과 통합 | IMPLEMENTED | 변경 경계·root 결합·현재 계획 재검사·한도 연결; 런타임 수락과 구분 |
| 테스트 전용 실제 레지스트리 Bootstrap | PASS | 2026-09-14 03:12:58 실행, exit 0, `PASS items=1255`; 전체 게임 실행과 구분 |
| 최종 후보 focused 테스트 컴파일 | PASS | 최종 1.20.1 소스 및 선택한 회귀 클래스 강제 재컴파일 |
| 최종 후보 focused 테스트 실행 | PASS | 시작267·성공267·실패0·aborted0·skipped0·containerFailures0 |
| 최종 후보 1.20.1 한정 clean·remap | PASS | exit0, 2분45초, 36개 task 실행; 아래 정확한 명령과 JAR 해시 |
| `validateAccessWidener` | NO-SOURCE | 검증할 소스가 없으며 실제 access widener 검증 PASS가 아님 |
| 버전 미지정 일반 `clean build` 전체 | NOT_RUN | 다른 Minecraft 버전 빌드를 요청 범위에 넣지 않음 |
| 배포 JAR 동일성 | NOT_RUN | 활성 인스턴스 JAR과 새 산출물의 바이트 해시 대조 |
| 게임 시작·Mixin 적용 | NOT_RUN | 새 JAR을 실제 로드한 시작/Mixin 로그 |
| 실제 재실행·방어·STOP·보관 결과 | NOT_RUN | [검증 계획](verification.md)의 게임 시나리오별 로그·실제 공간 확보 |

focused 실행 설정은 [auto-deposit-tests.init.gradle](../../runtime/chatclef_fabric_1.20.1/src/test/autoDeposit/auto-deposit-tests.init.gradle)을 사용하며 기존 Jupiter 실행기를 재사용한다. `autoDepositCompileTests`와 `autoDepositFocusedTests`를 1.20.1 프로젝트에만 등록하고 다른 버전의 test 소스 task 의존성을 가져오지 않는다. 모든 기존 자동보관 회귀와 새 `admission`·`lifecycle`·`cleanup`·`rearm`·`budget`·결과 검증을 포함한다.

raw named Minecraft를 직접 실행하는 테스트 JVM은 Fabric 개발 실행기의 package-access 처리가 없다. [테스트 전용 Bootstrap 설정](../../runtime/chatclef_fabric_1.20.1/src/test/autoDeposit/README.md)은 기존 ASM으로 정확한 6개 메서드의 접근성만 보완하며 생산 JAR·vendor/cache JAR·메서드 동작을 바꾸지 않는다. 실제 Bootstrap 확인은 [2026-09-14 03:12:58 로그](../../../../logs/auto_deposit_1201_Bootstrap_20260914_031258_530/verification.log)에서 통과했고 최종 clean 실행에도 포함됐다. 전체 focused 통과 근거는 [최종 빌드 로그](../../../../logs/auto_deposit_1201_Build_20260914_033306_455/verification.log)의 `JUPITER_FOCUSED_RESULT`다. 초기/중간 실행 기록은 보존하되 최종 소스의 수락 근거와 혼용하지 않는다. Bootstrap이나 headless 테스트 통과를 Mixin·게임 검증의 통과로 대신하지 않는다.

### 최종 실행과 입력·산출물 식별

| 항목 | 기록 |
| --- | --- |
| 실행 | 2026-09-14 03:33:06.4637560 → 03:35:57.0452281, Asia/Seoul |
| 결과 | exit0; `BUILD SUCCESSFUL in 2m 45s`; 36개 task 실행 |
| Gradle / JVM | Gradle8.8 / Eclipse Adoptium21.0.12+8-LTS, JVM21.0.12 |
| 브랜치 | `minecraft-plugin-fix/alto-clef-infinite-loop` |
| HEAD | `b3f00712605c00d97be4f7c04e16e0f6784f9d1f` |
| 입력 동일성 | 소스·테스트·설정 등 입력2,130개 SHA-256 변경0개, HEAD 일치 |
| JAR 크기·시각 | 8,857,200 bytes / 2026-09-14T03:35:55.6112708+09:00 |
| JAR SHA-256 | `C9E65CA3667C2DC3CD9582F9B1F0FD19B38F22BBAC9EAB6EC062C6FA0E4A2B0B` |

정확한 산출물은 [chatclef-1.20.1-0.18.23.jar](../../runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar)이며 절대 경로는 다음과 같다.

```text
C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar
```

[실행 결과 JSON](../../../../logs/auto_deposit_1201_Build_20260914_033306_455/result.json), [빌드 전 입력 manifest](../../../../logs/auto_deposit_1201_Build_20260914_033306_455/source-manifest.json), [최종 산출물 검사](../../../../logs/auto_deposit_1201_Build_20260914_033306_455/artifact-verification-final.json)를 함께 보존한다. manifest에는 브랜치·HEAD·빌드 전 전체 dirty 경로92개와 입력별 해시가 있다. 기존 변경을 포함한 기록이며 모든 dirty 경로를 이번 기능 수정으로 간주하지 않는다.

최종 산출물 검사에서 다른 Minecraft 버전의 Java 컴파일·산출물 task는 없었고, 필요한 새 클래스와 Minecraft manifest가 포함됐다. 테스트 코드·agent의 생산 JAR 혼입은0개다. 기본 런타임 클래스와 LAVI 클래스의 최대 major는61(Java17)이다. 최초 archive 전체 검사에서 나온 major63은 기존 Jackson의 `META-INF/versions/19/` 클래스3개였으므로, [최초 검사 기록](../../../../logs/auto_deposit_1201_Build_20260914_033306_455/artifact-verification.json)은 보존하고 최종 검사는 기본 클래스와 버전별 의존성 클래스를 구분했다. 이 분류 확인을 위해 의존성이나 빌드 입력을 변경하지 않았다.

### 1.20.1 전용 실행 명령

[전용 PowerShell 스크립트](../../../../scripts/verify-auto-deposit-1201.ps1)의 `Tests`, `Build`, `DryBuild`, `Bootstrap` 모드를 사용한다. 일반 `:1.20.1:build`도 `preprocessTestCode` 경로에서 다른 버전의 `compileJava`를 연결하는 task graph가 확인되어, 1.20.1만 빌드하라는 요구에 맞춰 아래 버전 한정 경로를 사용한다. 다른 버전의 소스 preprocessing은 필요할 수 있지만 다른 버전 Java 컴파일·JAR 생성은 실행 대상으로 삼지 않는다.

```powershell
.\gradlew.bat --no-build-cache --no-daemon --offline --stacktrace --warning-mode=summary --init-script .gradle/codex-build-init.gradle --init-script src/test/autoDeposit/auto-deposit-tests.init.gradle :1.20.1:clean :1.20.1:remapJar :1.20.1:validateAccessWidener :1.20.1:autoDepositFocusedTests --rerun-tasks
```

이 결과를 버전 미지정 `clean build` 전체 통과로 보고하지 않는다. 실제 clean/remap·focused 테스트 결과와 각 task의 실행 여부를 구분한다. `validateAccessWidener`가 `NO-SOURCE`로 끝난 것은 검증 대상 소스가 없다는 뜻이며 실제 access widener 검증 PASS로 표기하지 않는다. JDK 21 실행 환경과 Java 17 target은 그대로 유지하고 Gradle의 쓰기 cache와 임시 파일은 저장소 안으로 지정한다.

### 진단 출력의 증거 한계

immutable maintenance 결과 로그는 캡처한 명령 observation scope에 연결한다. cleanup 전 후보와 cleanup 후 최종 결과를 구분하고, 방어·비활성화·`REPLAN_REQUIRED`의 재개 가능한 결과와 정리 단위 종료도 구분한다. 출력은 실행 결과나 재시도 허가의 근거로 사용하지 않는다. 기존 명령 scope의 예약 terminal 출력 시도를 사용하므로, 여러 논리 정리 과정이 같은 명령 scope를 공유할 때 후속 개별 terminal의 물리 출력까지 모두 보장했다고 주장하지 않는다. 공유 로그 한도는 이 작업에서 늘리지 않았다. 새 필수 사건의 실제 파일 출력은 **VERIFIED_RUNTIME가 아닌 NOT_RUN**이며, 누락·억제 여부는 런타임에서 별도로 확인해야 한다.

빌드와 focused 테스트는 출력을 저장한 실제 실행 결과로만 PASS를 기록한다. Gradle 성공만으로 배포·Mixin·방어 후 복귀·새 수요의 보관 완료를 확인했다고 쓰지 않는다. 특히 이 후보의 한도가 정상적인 장거리 상자 준비를 너무 일찍 제한하는지, 같은 세션의 legacy/unknown WAIT가 의도한 경로로 회복되는지, 실제 방어와 물품 보호가 유지되는지는 런타임 확인이 남는다.
