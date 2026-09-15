<!-- #20260916_kpopmodder: Record collected runtime evidence separately from the earlier documentation and build handoff. -->

# 자동 보관: 기존 실행 근거와 A 검증 준비 — 2026-09-16

## 1. 이번에 확인한 결론

**기존 실행에서 자동 시작, 클라이언트 상자 수량 증가, 비정상 종료와 cleanup 결과까지 확인했다. 통제된 시나리오 A의 통과는 아직 확인하지 않았다.**

사용자 요청은 필요한 관측만 보완하고 가까운 상자의 자동 보관 한 번을 판정하는 것이다. 기존 진단에 필요한 관측 경로가 이미 있으며, 이번 원본 로그에서도 실제 출력이 확인됐다. 따라서 카운터·성공 판정·재발동 정책을 바꾸거나 관측 코드를 중복 추가하지 않았다. GUI의 정확한 위치 연결과 서버 반영 등 부족한 항목은 아래처럼 미검증으로 남긴다.

- 조사 기준 HEAD: `4c1e8988df6daf2dce8a046fa4406d2c5ce731ae`.
- 저장소: `C:\Vtuber_Souorce_Code\LAVI`; Fabric ChatClef 1.20.1.
- 기존 변경 보존: 프로젝트 이해 README와 이전 검증 인수인계 문서.
- 이번 수행: 소스·계약 조사, 실행 프로세스/배포 파일 읽기, 기존 로그 사본 수집·분석, 이 문서 추가.
- 이번 미수행: production/test/config 변경, 빌드, 배포, 게임 조작, 프로세스 재시작, 커밋·푸시.
- 관련 지침: [AGENTS.md](../../../../AGENTS.md), [백엔드 분리](../../../../plugins/Minecraft/docs/minecraft-backend-separation.md), [ChatClef/Carry On 경계](../../../../plugins/Minecraft/docs/chatclef-carryon-integration-direction.md), [기존 자동 보관 관측 계약](../../../../plugins/Minecraft/docs/chatclef-auto-deposit-slice-a-diagnostics-contract-2026-08-30.md).

이전 문서의 “미확인”은 당시 문서화 작업의 상태다. [이전 인수인계](2026-09-16-verification-handoff.md)의 과거 빌드·테스트 기록을 이번 실행 검증으로 바꾸어 쓰지 않는다.

## 2. 실행과 수집 식별

| 항목 | 실제 확인 |
| --- | --- |
| 프로세스 | PID `46980`, 시작 `2026-09-16T01:05:37.0853100+09:00` |
| 실제 gameDir | `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01`; 실행 인자의 `--gameDir`에서 읽음 |
| 출력 | 인스턴스 `logs/latest.log`, `logs/stdout-logs.txt`; BOUNDARY 모드의 Minecraft 표준 출력 로그 |
| 수집 위치 | [20260916-013555-464](../../../../test/test_Isolation/minecraft_auto_deposit_scenario_a/20260916-013555-464/capture.json) |
| 원본 보존 | [latest.log 사본](../../../../test/test_Isolation/minecraft_auto_deposit_scenario_a/20260916-013555-464/raw/latest.log) 8,330,016 bytes; [stdout 사본](../../../../test/test_Isolation/minecraft_auto_deposit_scenario_a/20260916-013555-464/raw/stdout-logs.txt) 9,125,888 bytes |
| 수집 무결성 | 두 파일 모두 복사 중 크기·수정 시각 변화 없음. 수집 메타데이터의 사본 SHA-256과 재계산 결과 일치. 원본이 기록하던 열린 파일이라는 한계는 유지 |
| 로그 시각 | Asia/Seoul, UTC+09:00; 아래 행 번호는 보존한 `latest.log` 기준 |
| 수집 성격 | `EXISTING_LOG_BASELINE_NOT_CONTROLLED_REPRODUCTION`; 이번 에이전트가 시작한 게임 재현이 아님 |

배포 파일과 저장소 빌드 파일은 모두 `chatclef-1.20.1-0.18.23.jar`, 크기 9,011,739 bytes, SHA-256:

```text
74D93416193799748EA9004ED84E023C513521ABF9E7862F074D5FB1B7A61480
```

252행의 `DIAGNOSTICS_RUNTIME_IDENTITY`에는 실제 diagnostics 클래스의 CodeSource가 위 인스턴스의 `mods/chatclef-1.20.1-0.18.23.jar`로 출력됐다. 해당 JAR의 수정 시각은 `2026-09-15T15:47:37.8105548Z`이고 프로세스 시작보다 앞선다. 이는 파일 일치와 해당 diagnostics 클래스의 로딩 위치에 대한 근거다. 모든 Mixin의 주입·실행 성공까지 증명하지 않는다.

원본은 외부 위치에서 읽기만 했다. 사본과 메타데이터는 저장소 내부 새 폴더에 저장했다. `test/test_Isolation/`은 Git ignore 대상이므로 새 clone에 원본 근거가 따라온다고 가정하지 않는다.

## 3. `confirmedStoredItems`의 소스상 의미

Java 소스 루트는 `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/`다.

| 소유자/경로 | 확인한 의미 |
| --- | --- |
| `AutoDepositMaintenanceTask.confirmedStoredCount()` | trusted 수량과 일반 `DepositAllTask.automaticStoredCount()`의 합 |
| 일반 `ContainerStoredTracker.trackChange()` | 플레이어 인벤토리가 아닌 슬롯의 허용된 before/after 변화로 `previous + delta`; 일반 경로는 플레이어 감소와 상자 증가를 쌍으로 검증하지 않음 |
| `SlotClickMixin.slotClick()` | 로컬 `ScreenHandler` 클릭 처리 뒤 슬롯을 비교해 `SlotClickChangedEvent` 발행. 이 이벤트가 일반 카운터의 입력 |
| `AutoDepositTrustedTransferTracker` | 같은 로컬 이벤트에서 정확한 컨테이너/world/dimension 조건과 플레이어 순감소·상자 순증가를 대조해 최솟값 반영 |

따라서 **클릭 요청 횟수도, 서버 확인 수량도 아니다. 클라이언트 슬롯 변화에서 얻은 값**이다. 이번에 이 의미나 성공 조건은 변경하지 않았다.

직접 확인한 소스:

- [일반 카운터](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/container/ContainerStoredTracker.java), [이벤트 발행 지점](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/mixins/SlotClickMixin.java).
- [maintenance 합산·최종화](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceTask.java), [trusted 전송 관측](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/trusted/execution/AutoDepositTrustedTransferTracker.java).
- [기존 카운터 진단](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/container/store/deposit/counter/StoreCounterDiagnostics.java), [기존 S2C 적용 관측](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/container/gui/slot/ContainerServerReconciliationDiagnostics.java).

S2C 관측은 서버 패킷을 handler에 적용한 뒤의 상태다. 프로토콜에 클릭별 ACK가 없으므로 이것도 특정 클릭의 서버 ACK로 표현하지 않는다. 기존 구현은 이 구분을 이미 로그 필드에 담고 있다.

## 4. 첫 자동 실행의 실제 기록

구간: `01:06:41~01:06:43`, client tick `795~827`.

```text
autoOperationId: auto-deposit-1
maintenanceGenerationId: auto-deposit-1-maintenance-1
autoChildOperationId: auto-deposit-1-child-1
pressureOwnedRunId: auto-deposit-1-pressure-run-1
storeOperationId: store-deposit-1421
root trackerId: 24
root subscriptionId: 29
maintenanceTask: 251c4060 / taskInstanceId 119
childTask: 51509537
```

| 단계 | 원본 행 | 실제 값과 판단 |
| --- | --- | --- |
| 자동 시작 | 1312–1314 | `automatic_storage_admitted`, 점유 `33/36`, `targetStepCount=5`, `ROOT_STARTED`, `requestSource=AUTO_DEPOSIT_ALL_CHAIN`. 원래 명령 문맥은 `get iron_axe 1`이며 수동 deposit 문맥이 아님 |
| root 카운터 구독 | 1315–1317, 1335 | 시작 시 pending → 같은 tick에 `APPLIED_TO_NATIVE_TOPIC`. 구독 요청과 실제 등록을 구분할 수 있음 |
| 상자 GUI 관측 | 1449 | CHEST, 대상 후보 `-4084,69,-933`, 현재 handler/type 일치, `syncId=2`. 단 `targetScreenAssociationProven=false`, `HEURISTIC_RECENT_EXACT_CONTAINER_INTERACTION`이므로 정확한 위치 연결은 미검증 |
| 첫 클릭 API 진입/반환 | 1450–1451 | `CLICK_API_HEAD/RETURN`가 실제 FIRST로 출력. `EXACT_NATIVE_OWNER_BINDING`으로 위 store/root에 연결됨. 로그 후반의 별도 UNBOUND producer scope와 혼동하면 안 됨 |
| 흙의 첫 수량 변화 | 1449, 1467, 1471–1472 | player `130→66`, container `0→64`, cursor `0`; 다음 client tick에서 paired decrease/increase `64/64` 관측 |
| 흙의 추가 수량 변화 | 1499, 1503–1504 | player `6`, container `124`, cursor `0`; 다음 client tick의 누적 paired delta `124/124`. 앞선 64에 124를 더해 세지 않음 |
| 마지막 클릭 상태 | 1519–1520 | player `6→0`, container `124`, cursor `0→6`. 이 기록은 tick 820 상태이며 cleanup 후 커서 상태가 아님 |
| 자식 완료 판정 | 1535–1537 | `notStoredOutput=REMOVED`, `finished=true`, `stopped=false`, `storedTargetsSatisfied=false`, 흙 required `64`, stored `0`, `GENERAL_CHILD_UNCONFIRMED` |
| 최종 결과/cleanup | 1554, 1556 | `AFTER_CLEANUP`, 점유 `33→30`, `childrenComplete=false`, working-set `SATISFIED`, `cleanupComplete=true`, `normalValidated=false`; root 정산도 같은 실패 사유 |

이 수량 변화의 근거는 `POST_REQUEST_CLIENT_TICK_LIVE_HANDLER`다. 즉 **클라이언트의 상자 handler에 흙 124개 증가가 관측됐다. 서버의 영속 저장 완료나 정확한 월드 상자 위치까지 확인된 것은 아니다.**

`cleanupComplete=true`는 현재 소스에서 owned task stop과 survival claim 복원 경로가 예외 없이 반환했다는 의미다. 서버 저장 완료나 모든 외부 입력 상태의 독립 검사까지 뜻하지 않는다.

### 분리 판정

| 항목 | 판정 |
| --- | --- |
| 자동 시작 | 기존 실행 로그에서 확인 |
| 일부 전송 | 클라이언트 handler에서 누적 124개 증가 확인; 서버 반영 미검증 |
| 인벤토리 압력 | 점유 3칸 감소와 종료 점유 30 확인. 발동 기준 33 미만이지만, 시작 시 5칸 정리 목표/low-water 28에는 미달. 단순 슬롯 감소를 전체 정리 성공으로 바꾸지 않음 |
| 정상 종료 | **성립하지 않음**: `GENERAL_CHILD_UNCONFIRMED`, `normalValidated=false` |
| cleanup | 위 계약 범위에서 `true` 기록 확인; cleanup 후 커서/전체 입력 상태 독립 대조 없음 |
| 통제된 A | **NOT_RUN**: 사용자 조작 부재와 준비 상태·상자 전후 수량을 독립적으로 확보한 재현이 아님 |

후속 `01:07:51`(3909행), `01:08:04`(4239행)의 자동 root도 `GENERAL_CHILD_UNCONFIRMED`, `normalValidated=false`, `cleanupComplete=true`였다. 두 건 모두 점유 `33→32`. 첫 실행의 전송 수량을 이 두 실행의 증거로 재사용하지 않는다.

## 5. 미검증과 코드 보완 판단

1. **카운터 연결의 원인:** 첫 root의 API HEAD/RETURN은 출력됐지만, 해당 구간에서 `LEGACY_REDIRECT_ENTER`, `LEGACY_ACTION_RETURN`, publication/callback/counter-update 경계의 실제 출력은 찾지 못했다. firstSignatureCount 9, overflow 0인 root trace와 카운터 0 기록은 조사 근거다. 이벤트 미출력만으로 Mixin 미주입이나 미발행을 확정하지 않는다. `SlotClickMixin`의 legacy redirect→이벤트 발행→카운터 callback 경로가 다음 확인 지점이다.
2. **GUI와 서버:** 정확한 target-screen 연결이 증명되지 않았다. 2729행의 `SERVER_SLOT_UPDATE_APPLIED`는 이후 `FURNACE`, `syncId=4`의 다른 작업이다. 자동 상자 전송의 서버 증거로 사용하지 않는다. 같은 상자 실행의 S2C 적용 또는 독립적인 서버 측 전후 대조는 미검증이다.
3. **관측 완전성:** 1551행은 `effectObservationCount=0`, `effectVerified=false`; 1553행은 부분 coverage와 억제 수를 명시한다. 이 값은 관측기의 제한이며 실제 아이템 이동 0이라는 뜻이 아니다. 1356행의 GUI terminal도 tick 797의 별도 미연결 screen event다. 이를 보관 root의 최종 결과로 사용하지 않는다.
4. **현재 프로세스의 진단 한도:** 기존 deposit scope 4개가 실제로 관측됐고, 소스의 process lifetime admission 상한도 4개다. 일부 scope의 summary 32회가 소진됐다. FIRST 잔여와 ordinary/summary 고갈은 별개지만, 새 root에 새 상세 trace가 보장되지 않는다. 모드 토글·test reset·상한 증가로 우회하지 않았다.

**현재 결정: 추가 관측 소스 변경 없음.** 기존 owner별 counter/GUI/S2C/terminal 진단을 재사용한다. 관측 중복 구현이나 로깅 구조 확장으로 위 미검증을 해소하려 하지 않는다. source instrumentation이 없으므로 이번 새 코드 테스트·재빌드는 수행하지 않았다. 과거 268개 JUnit 통과와 빌드 성공은 [이전 인수인계](2026-09-16-verification-handoff.md)에 기록된 범위로 유지한다.

정상 완료 실패 자체는 실행 로그에 있다. 다음 수정 검토 범위는 아래에서 좁힌 legacy producer와 카운터 전달 경로다. 이번 요청에서 유지하라고 한 카운터 의미·성공 판정·STOP·자동방어·보호 아이템·working-set·KEEP_LOADOUT·정확한 GUI 바인딩·실행 제한·SAME_FAILURE 정책은 변경하지 않았다.

요청한 책임 분리·폴더화는 변경 범위의 독립 책임에 적용한다. 이번에는 production 변경이 없어 리팩터링 대상이 없으며, 수집 사본은 전용 실행 폴더, 설명은 기존 자동 보관 문서 폴더에 분리했다.

### 5.1 산출물의 정적 호출구조 확인

위 SHA가 일치하는 빌드 JAR의 `adris.altoclef.mixins.SlotClickMixin`을 `javap -v -p`로 읽고, ZIP 내부의 `chatclef-refmap.json`과 `altoclef.mixins.json`을 대조했다. 파일을 추출하거나 빌드하지 않았다.

- packaged redirect의 `method`와 호출 `target`은 **둘 다 `internalOnSlotClick`**이다.
- refmap에 `internalOnSlotClick → class_1703.method_30010`, `onSlotClick → class_1703.method_7593` 매핑이 있다. refmap 누락으로 설명할 근거는 없다.
- mixins 설정은 `required=true`, `defaultRequire=1`이며 `SlotClickMixin`을 포함한다. 설정 존재만으로 모든 실행 분기에서 hook이 호출된다는 뜻은 아니다.
- redirect body의 bytecode offset 129는 `self.method_7593`(즉 `self.onSlotClick`) 호출이고, offset 262에 `EventBus.publish`가 있다.

비교한 로컬 named Minecraft JAR은 runtime 루트 아래 다음 경로다.

```text
.gradle/codex-user-home/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/
1.20.1-net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2/
minecraft-merged-1.20.1-net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2.jar

SHA256 FA7C45FA6C44D1DD8A237BF5716145171D615FCBDFB66909FAEB1B565CC84980
```

`javap -c -p net.minecraft.screen.ScreenHandler`로 확인한 구조:

1. 외곽 `onSlotClick`은 offset 6에서 `internalOnSlotClick`을 호출한다.
2. `internalOnSlotClick`의 offset 8에서 `QUICK_CRAFT`를 비교한다. 다른 action은 offset 11에서 542로 이동한다.
3. 이 메서드 내부의 유일한 `internalOnSlotClick` 재귀 호출은 offset 294다. `QUICK_CRAFT`, stage 2, 선택 슬롯 1개인 분기에서 `PICKUP`으로 호출한다.

**현재 redirect는 외곽의 일반 슬롯 처리가 아니라 QUICK_CRAFT 내부 재귀 호출에 걸려 있다.** 일반 `PICKUP` 기록에서 outer HEAD/RETURN은 있지만 legacy 경계가 없고 카운터가 0인 현상과 부합한다. 이는 소스·패키징 산출물의 정적 근거다. 실행 중 다른 모드가 적용한 최종 변환 바이트코드를 확보하지 않았으므로 전체 런타임 원인 확정과는 구분한다.

후속 기능 수정의 최소 단위는 외곽 슬롯 동작의 before/after 관측과 **원래 슬롯 처리를 정확히 한 번 수행**하는 경로를 함께 다루는 것이다. 예외·반환·native 호출 순서와 기존 이벤트·카운터 의미를 보존해야 한다. 현재 body가 다시 `self.onSlotClick()`을 호출하므로 selector의 `method`만 외곽으로 옮기면 재귀 위험이 있다.

이벤트 발행 경로를 고치면 실제 카운터와 완료 동작에 영향을 준다. 따라서 관측만 보완하는 변경과 구분되는 **기능 수정**이며, 이번 전달문의 “실제 오류 발견 시 근거와 필요한 수정 범위를 제시” 원칙에 따라 이 범위를 제시하고 구현에는 섞지 않았다.

## 6. D: 현재 정책 확인만 수행

[AutoDepositRearmPolicy](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/rearm/AutoDepositRearmPolicy.java)와 [기존 정책 계약](../../../../plugins/Minecraft/docs/auto-deposit-rearm-2026-09-14/implementation.md)을 대조했다.

- 정상 완료이고 논리 작업 시작보다 점유가 줄어 종료 점유가 33 미만이면 `WAIT_RISE`.
- 이후 다시 33 이상이면 `PRESSURE_RISE`, 기존 실행 허용 조건을 충족해 실제 시작할 때 새 budget/episode.
- 정상 부분 정리 뒤 여전히 33 이상이면 같은 budget의 허용 범위에서 `FOLLOWUP`.
- 실패 뒤 수동으로 비웠다가 다시 채워도 동일 실패 조건과 소비 예산은 자동 초기화하지 않음.
- 예를 들어 **정상 종료 `33→30→33`은 재발동 가능**하다. 구형 state machine의 28칸 조건만으로 현재 정책을 설명하면 안 된다.

이번 첫 실행은 정상 완료가 아니므로 위 정상 재발동의 실게임 증거가 아니다. D 정책 변경과 D 실게임 검증은 수행하지 않았다.

## 7. 다음 검증의 최소 작업

1. 사용자에게 게임 준비를 요청했다. 공간 있는 가까운 상자와 main 인벤토리 33칸 이상, 보호 규칙 등을 제외하고 비울 수 있는 surplus가 필요하다. armor/offhand/cursor는 압력 계산에서 제외된다. 현재 정상 자동화 가용 상태와 명시 STOP 여부를 확인하고 보호 기능을 끄지 않는다.
2. 실제 조작이 가능할 때 시작 전 상자 위치·대상 종류/수량·인벤토리 점유를 기록한다. 수동 deposit 명령 없이 자동 시작을 관찰하고, 시작 후 사용자가 아이템을 이동하지 않는다. 종료 후 동일 항목과 cursor를 대조한다. 기존 실패 차단을 검사 없이 초기화하지 않는다.
3. 현재 프로세스에서는 남은 FIRST·critical 관측과 전후 수량 대조로 확보 가능한 근거부터 수집한다. 자동 보관 재현과 Minecraft 프로세스 재시작은 구분한다. 프로세스 재시작이 필요한 경우에는 필요성과 해당 작업의 권한 범위를 확인한다. 이번에는 재시작하거나 프로세스를 종료하지 않았다. 어떤 실행에서도 실제 모드·JAR·scope admission과 물리 출력부터 확인한다.
4. 로그 수집·분석은 같은 실행의 구간·operation ID·전후 수량을 연결한다. 서버 근거가 없으면 그 항목을 UNKNOWN으로 남기면서 전송/압력/정상 종료/cleanup의 나머지 판정은 진행한다.

기존 [latest.log cursor 수집기](../../../../tests/minecraft_chatclef/runtime/automatic_deposit/evidence/latest_log_cursor_reader.py)는 파일 정체성·prefix 해시·회전·잘림·부분 행을 확인한다. 다음 A에서는 이 수집 경로를 재사용하며 새 수집 프레임워크를 만들지 않는다. 저장 파일의 NBT는 저장 시점의 자료이므로 실행 중 서버 메모리 상태나 특정 요청 ACK로 대신하지 않는다.

사용자 준비 응답이 없는 현재, 통제된 A 실행은 남아 있다. 장시간 준비·접근, 전체 회귀, 정상 재발동 D까지 완료했다고 보고하지 않는다.
