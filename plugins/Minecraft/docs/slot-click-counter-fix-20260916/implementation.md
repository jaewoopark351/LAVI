<!-- #20260916_kpopmodder: Record the explicitly requested 1.20.1 slot-event bridge correction and separate implementation, build, and live evidence. -->

# 1.20.1 슬롯 변경 이벤트 연결 수정

## 범위와 근거

사용자가 일반 클릭의 hook와 native 호출을 함께 수정하고, 책임 분리·폴더화, 테스트, 1.20.1 빌드, 수정본의 자동 보관 A 검증을 요청했다. 이전 설명/관측 전용 범위에서 기능 수정 요청으로 전환됐다. 커밋·푸시는 요청하지 않았다.

- 기준 HEAD: `4c1e8988df6daf2dce8a046fa4406d2c5ce731ae`.
- 기존 사용자 문서 변경은 보존한다.
- [수정 전 실행·산출물 근거](../../../../docs/codex/project-understanding/minecraft-auto-deposit/2026-09-16-runtime-evidence.md).
- [원본 소스와 빌드 JAR 보존](../../../../test/test_Isolation/slot_click_counter_fix_20260916/20260916-020023-936/before.json).
- 기존 `SlotClickMixin.java` SHA-256: `BBAFC74AB29A602D9ACEBC227E7B07218D7B6E5B7F1120B74CB521859CCB08D4`.
- 분류: **BEHAVIOR**. 기존 슬롯 변화 이벤트를 실제 일반 클릭에 연결하는 기능 수정이며, 진단 전용 수정으로 표시하지 않는다.

## 수정 전 엔진 변경 기록

| 항목 | 기록 |
| --- | --- |
| evidence mode | RUNTIME_REPRODUCTION — 기존 프로세스 PID 46980의 보존 로그. 통제된 A는 아님 |
| reproduction / correlation | `20260916-013555-464`, `auto-deposit-1`, `store-deposit-1421`, tick 795–827 |
| 확인한 현상 | client handler의 흙 +124, root stored counter 0, GENERAL_CHILD_UNCONFIRMED, cleanup true |
| 마지막 확인 경계 | 일반 PICKUP의 CLICK_API_HEAD/RETURN과 후속 client tick의 paired delta |
| 좁혀진 경계 | `adris/altoclef/mixins/SlotClickMixin.java`, `slotClick`, 기존 45–123행. legacy producer→EventBus 경계가 출력되지 않음 |
| 정적 원인 근거 | packaged selector가 internalOnSlotClick 내부 재귀를 겨냥함. 실제 1.20.1의 일반 PICKUP은 해당 QUICK_CRAFT 분기를 지나지 않음 |
| 남은 불확실성 | 기존 실행의 최종 transformed bytecode, 정확한 GUI 위치 연결, 서버 반영. 수정 후 근거로 수정 전 실행의 미확인을 덮지 않음 |
| 기존 계약 | ScreenHandler 원래 클릭 처리 후 달라진 각 슬롯의 before/after 이벤트. 카운터는 클라이언트 변화량이며 서버 ACK가 아님 |
| 부모/자식 | AutoDepositMaintenanceTask → DepositAllTask → 슬롯 이동 자식. 완료는 기존 maintenance 및 tracker predicate가 소유 |
| containment | LAVI 명령/부모 Task에서 추정 증가시키면 실제 슬롯 변화 계약을 우회함. Carry On 경계는 이 일반 클릭과 무관. before/after 호출 경계는 Mixin에서만 연결하고 이벤트 처리/진단 구현은 LAVI에 분리 |
| 엔진 hunk | 한 upstream 파일의 1.20.1 slotClick redirect 분기 + private native Shadow 연결. 다른 버전 legacy 분기는 보존 |
| native 처리 | 외곽 onSlotClick의 internal 호출 한 곳만 redirect, Shadow로 원래 internal 처리 1회. QUICK_CRAFT의 내부 재귀는 native 소유 |
| 입력·retry·timeout·path·cleanup | 소유자/정책 변경 없음. 원래 예외 전파 유지. 새 전역 mutable state 없음 |
| client/server 경계 | client thread + local ClientPlayerEntity + current handler에서만 이벤트 발행. 통합 서버·다른 handler/thread에서는 native 처리만 1회 |

일반 월드 우클릭, 문/침대/버튼/블록 배치, Carry On capability, Baritone 경로 제어는 이 hunk의 호출 대상이 아니다. 이들 경로의 실게임 회귀는 실행 전까지 NOT_RUN이다. 영향받는 슬롯 action(PICKUP, QUICK_MOVE, SWAP, QUICK_CRAFT 등)의 호출·변화·예외와 기존 자동 보관 회귀를 우선 검사한다.

## 책임과 파일

Java 루트: `runtime/chatclef_fabric_1.20.1/src/main/java/`.

- `adris/altoclef/mixins/SlotClickMixin.java`: MC==12001 native 호출 연결. Minecraft 내부 동작 재구현 없음.
- `lavi/minecraft/inventory/slotclick/SlotClickEventBridge.java`: 호출 한 번에 한정한 snapshot/diff/기존 EventBus 발행. 서버·다른 클라이언트 context의 이벤트 혼입 방지.
- `lavi/minecraft/diagnostics/container/store/deposit/counter/SlotClickDiagnosticScope.java`: 기존 자동 작업/GUI/counter 진단 경로의 호출 범위. 기능 이벤트 발행이나 카운터 결정은 소유하지 않음.
- `src/test/java/lavi/minecraft/inventory/slotclick/`: 실제 Minecraft 슬롯/기존 EventBus/tracker를 사용하는 회귀 및 fixture 책임 분리.
- `src/test/mixinApplication/.../slotclick/`: 기존 IsolatedMixinService를 이용한 실제 Sponge 적용 확인. 새 transformer/검증 프레임워크를 만들지 않음.
- 기존 focused init script와 PowerShell 검증 스크립트: 해당 검사 선택과 소스 manifest에만 연결.
- 기존 `src/test/java/lavi/minecraft/testsupport/HeadlessMinecraftClientSession.java`: 회귀 실행에서 드러난 테스트 객체의 타입 위반 교정. `Object`를 플레이어 필드에 강제로 넣던 코드를 실제 `ClientPlayerEntity` 할당으로 바꾸고, 인벤토리·월드는 비어 있는 기존 headless 상태로 유지.

기존 tracker 합산, 정상 완료 조건, STOP/방어/보호 아이템/working-set/KEEP_LOADOUT/재발동/실행 예산은 수정하지 않는다. snapshot은 현재 클릭의 지역 상태이며 이후 호출이나 세션에 보관하지 않는다.

## 동반 관측과 검증

기존 BOUNDARY `StoreCounterDiagnostics`/EventBus probe/tracker/GUI 관측을 재사용한다. 새로운 writer, queue, session reset 또는 cap 증가는 없다.

한 admitted store-counter scope에서 요구하는 경계는 다음과 같다. runtime 식별자는 상관관계 필드이며 signature가 아니다.

```text
SlotClickDiagnosticScope / PRODUCER / OUTER_REDIRECT_ENTER
SlotClickDiagnosticScope / PRODUCER / OUTER_ACTION_RETURN
  또는 같은 owner / PRODUCER / OUTER_ACTION_NOT_RETURNED
EventBus / PUBLICATION 및 실제 recipient/callback 경계
ContainerStoredTracker / COUNTER_UPDATE / 실제 delta 분기
기존 tracker completion/종료 및 maintenance AFTER_CLEANUP 결과
```

기존 scope의 FIRST 32개·도메인 scope 4개와 공유 session reservation을 그대로 사용한다. 서버/off-thread native-only 경로에는 client observation scope를 만들지 않는다. 진단 OFF/BOUNDARY/출력 실패가 native 호출과 이벤트 발행에 영향을 주지 않도록 검사한다. 새 테스트에서 scope 입장 거절·용량 소진을 명시적으로 유발한 검사는 하지 않았다. 일반 lifecycle 결과와 상세 trace의 보장 범위는 서로 다르며, cap 또는 sink가 부족하면 실제 출력은 미검증이다.

검증 항목:

1. 실제 bridge·EventBus·tracker와 테스트용 native callback으로 호출 1회, source→cursor stored 0, cursor→container stored 64, changed slot당 event 1회 확인.
2. no-op, native 예외, server/non-current/off-thread 경계. 모든 `SlotActionType`의 callback 위임을 검사하되, vanilla의 각 클릭 알고리즘을 실행한 검사는 아님.
3. OFF/BOUNDARY 및 실제 출력 실패 조건에서 같은 기능 결과. 실제 formatter·gate·sink 출력 확인.
4. named 및 packaged 1.20.1 ScreenHandler에 실제 Sponge Mixin 적용, outer redirect와 private native 호출·원래 내부 재귀 확인.
5. 기존 PowerShell의 clean/forced 1.20.1 전용 빌드와 focused 회귀. 다른 버전 빌드로 범위를 넓히지 않음.
6. 수정 JAR의 실행에서 A를 관찰해 일부 전송/압력/정상 종료/cleanup을 각각 판정. 수정 전 로그를 수정본 검증에 사용하지 않음.

## 되돌림 단위

이 변경만의 inverse diff가 단위다. Mixin의 MC==12001 새 분기/Shadow 연결을 제거해 보존 원본과 같게 하고, 이 작업에서 새로 만든 bridge/diagnostic scope/test 및 검증 연결만 역적용한다. headless fixture 교정은 독립된 테스트 hunk로 구분한다. 다른 버전 legacy 본문, 기존 패치와 사용자 문서 변경은 보존한다. 전역 reset/checkout/clean은 하지 않는다. 실제 되돌림은 별도 요청 시 당시 파일·hunk 상태를 재확인한다.

## 결과

| 단계 | 결과와 근거 | 증명 범위 |
| --- | --- | --- |
| 구현 | 1.20.1 outer hook/private native 위임, 기능·진단 책임 분리 완료 | 다른 버전 legacy 본문은 보존 원본과 일치. tracker/완료 정책 변경 없음 |
| 수정 전 JAR 대조 | **EXPECTED_FAIL** — [실제 Mixin 검사 로그](../../../../test/test_Isolation/minecraft/slot_click_mixin_application/output/a7f8edaaaf784278ac6321b869759b0e/smoke.log) | 기존 내부 클릭 hook이 native 본문을 바꾸는 구조를 새 검사가 검출. 수정본 실패나 수정 전 실게임 재현 결과가 아님 |
| 수정본 named Mixin | **PASS** — [실제 Sponge 적용 로그](../../../../test/test_Isolation/minecraft/slot_click_mixin_application/output/282402b7cf73476e9346eeea4e8c402a/smoke.log) | outer redirect 1곳, private native 위임 1곳, native 본문·QUICK_CRAFT 내부 재귀 유지. 실게임 호출 횟수는 미검증 |
| focused 회귀 | **279/279 PASS**, 실패·중단·건너뜀 0 — [결과](../../../../logs/auto_deposit_1201_Tests_20260916_021840_501/result.json), [로그](../../../../logs/auto_deposit_1201_Tests_20260916_021840_501/verification.log) | 새 클릭·카운터 테스트 11개와 기존 자동 보관 회귀. 실제 테스트 sink 출력·출력 실패 시 기능 결과도 확인 |
| 1.20.1 clean/forced 빌드 | **PASS**, 3분 16초 — [결과·JAR 해시](../../../../logs/auto_deposit_1201_Build_20260916_021933_519/result.json), [로그](../../../../logs/auto_deposit_1201_Build_20260916_021933_519/verification.log) | 새 PowerShell 창 PID `46592`, 기존 `verify-auto-deposit-1201.ps1 -Mode Build`. 빌드 실행에서도 회귀 279/279 통과 |
| 수정 JAR packaged Mixin | **PASS** — [실제 remapped JAR/refmap 적용 로그](../../../../test/test_Isolation/minecraft/slot_click_mixin_application/output/2fde0d8540d64c35b390c3fdc98a9d7c/smoke.log) | `method_7593` 외곽 진입 → `method_30010` native 1개 위임 경계, native 본문·QUICK_CRAFT 내부 재귀 유지 |
| 배포·수정본 실게임 A | **NOT_RUN** | 기존 Minecraft 프로세스 PID `46980`은 수정 전 JAR 실행. 사용자 게임 준비 응답 및 수정본 실행 근거가 없음 |

첫 회귀 실행은 새 테스트 11개 통과 후 기존 `DepositAllStoreHomeTaskRunnerHandoffTest`에서 JVM이 충돌했다([초기 로그](../../../../logs/auto_deposit_1201_Tests_20260916_020936_620/verification.log)). 충돌 위치는 `AutoDepositWorkingSetVerifier.verify`의 `ClientPlayerEntity.getInventory()` 호출이며, 위 headless fixture가 해당 필드에 넣던 값은 `Object`였다. 테스트 fixture의 타입 교정 후 전체 279개가 통과했다. 이 문제를 게임 기능 오류나 JDK 변경 사유로 해석하지 않는다.

### 빌드 산출물과 소스 일치

- SDK: Temurin `21.0.12+8`, Gradle `8.8`. Minecraft 1.20.1의 세 변경 클래스는 class major `61`(Java 17).
- [소스 manifest](../../../../logs/auto_deposit_1201_Build_20260916_021933_519/source-manifest.json)의 입력 2,209개와 검증 스크립트 SHA-256이 빌드 후 현재 파일과 모두 일치.
- 실행한 주요 task는 `:1.20.1:clean`, `:1.20.1:remapJar`, `:1.20.1:validateAccessWidener`, `:1.20.1:autoDepositFocusedTests`, 옵션은 `--rerun-tasks --no-build-cache --no-daemon --offline`. 다중 버전 프로젝트의 전처리 의존 task는 실행됐지만 `compileJava`/`jar`/`remapJar`/`clean`은 1.20.1만 실행. `validateAccessWidener`는 `NO-SOURCE`이므로 별도 access widener 검증 성공으로 세지 않는다.
- [수정 JAR](../../runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar): 9,015,366 bytes, SHA-256 `254C219A794CFC11E5BCF29EED71A51E6B8689F21D4995BC60D968B7F43146C0`.
- 실게임 instance `LAVI_TEST_Fabric01`의 기존 JAR 해시는 여전히 `74D93416193799748EA9004ED84E023C513521ABF9E7862F074D5FB1B7A61480`. 수정 JAR을 복사하거나 게임을 재실행하지 않았다. 커밋·푸시도 수행하지 않았다.

Mixin 검사는 실제 앱 설정에서 적용 목록을 `SlotClickMixin` 하나로 좁힌 바이트코드 검사다. 전체 모드 조합의 클래스 로드와 실제 클릭 실행은 미검증이다.

빌드·Mixin 적용·단위 회귀는 수정된 JAR의 자동 시작, 전송, 압력 해소, 정상 종료, cleanup 또는 서버 반영을 입증하지 않는다. 수정본의 같은 실행에서 각각 대조할 때까지 실게임 항목은 미검증으로 유지한다.

### 다음 최소 작업

1. 현재 게임을 정상 종료하고 수정 JAR을 적용한 뒤 재실행한다. 실행 instance와 JAR 해시를 다시 대조하고 새 로그 시작점을 확보한다.
2. 접근 가능하고 여유 공간이 있는 가까운 상자, 자동 보관 가능한 여분 아이템, main 인벤토리 점유 33칸 이상을 준비한다. 수동 `deposit`은 입력하지 않고 자동 시작 후 사용자 아이템 이동을 멈춘다.
3. 같은 작업 식별자의 시작 → 클릭 → 이벤트 → 카운터 → terminal/AFTER_CLEANUP과 인벤토리·상자·cursor 전후 수량을 대조한다. 일부 전송, 압력 해소, 정상 종료, cleanup, 서버 반영을 각각 판정한다.
4. A 이후 장시간 접근·전체 모드 회귀·재발동 D는 별도 검증이다. 현재 정책 D는 기존 실행 근거 문서의 소스 확인을 유지하며 이번 수정에 섞지 않았다.
