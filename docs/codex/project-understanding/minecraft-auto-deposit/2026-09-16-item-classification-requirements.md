<!-- #20260916_kpopmodder: Preserve pre-change evidence and record the authorized policy revision 2 implementation and verification. -->

# 자동 보관 아이템 분류 변경 요구사항·구현 기록 — 2026-09-16

## 1. 문서화 당시 상태와 범위

처음 작성할 때는 **요구사항 문서화만 완료하고 분류 변경은 미구현 상태**였다. 사용자는 이후 같은 분류 계약의 구현·테스트·1.20.1 빌드를 직접 요청했다. 아래 1~6절은 구현 전 요구사항과 관측 기록이며, 현재 진행 상태와 검증 결과는 **7절**에 기록한다.

- 대상: LAVI / Minecraft Fabric ChatClef **1.20.1**.
- 저장소: `C:\Vtuber_Souorce_Code\LAVI`.
- 기준 HEAD: `4c1e8988df6daf2dce8a046fa4406d2c5ce731ae` + 기존 미커밋 변경. 이 문서 작업은 기존 소스·테스트·스크립트 변경을 보존한다.
- 문서화 당시 정책: `revision: 1`, 파일 SHA-256 `76659763EE1FD4D58A2410DADE76794EB4A2410DC475ABB68DCC7B1C36CC2B66`.
- 당시 작업은 이 문서 추가·검수 수정과 [문서 목록](../README.md) 연결이었다. 당시에는 운영 코드·정책 JSON·테스트·AGENTS.md 수정, 빌드, 배포, 게임 조작, 커밋·푸시를 수행하지 않았다.
- 다음 작업은 그때의 사용자 요청과 이미 승인된 범위를 따른다. 문서 저장 자체가 구현·빌드·배포·실게임 실행 권한을 추가하지 않는다.

## 2. 사용자가 요청한 분류

| 아이템 | 정확한 Minecraft ID | 구현 전 분류 | 요청한 분류와 처리 |
| --- | --- | --- | --- |
| 응회암 | `minecraft:tuff` | `UNKNOWN` | 조약돌 수준의 일반 자원. 일반 상자의 자동 보관 대상에 포함 |
| 자갈 | `minecraft:gravel` | `UNKNOWN` | 일반 보관 대상에 포함. 낙하 블록의 안전 건축재 제외 규칙 유지 |
| 안산암 | `minecraft:andesite` | `UNKNOWN` | 조약돌 수준의 일반 자원. 일반 상자의 자동 보관 대상에 포함 |
| 화강암 | `minecraft:granite` | `UNKNOWN` | 조약돌 수준의 일반 자원. 일반 상자의 자동 보관 대상에 포함 |
| 섬록암 | `minecraft:diorite` | `UNKNOWN` | 조약돌 수준의 일반 자원. 일반 상자의 자동 보관 대상에 포함 |
| 사탕수수 | `minecraft:sugar_cane` | `GENERAL_SURPLUS` | 현재 일반 보관 분류 유지 |
| 청금석 | `minecraft:lapis_lazuli` | `GENERAL_SURPLUS` | `CONDITIONAL_VALUABLE`. 기존 귀중품 경로를 사용하여 적격 신뢰 상자에만 보관 |

### 보관 등급과 비축 기준의 구분

이 문서는 사용자의 “조약돌 수준”을 **일반 상자에 자동 보관할 수 있는 자원 등급**으로 기록한다. `safeBuildingIds`는 자동 보관 정책에서 건축재로 남길 수량을 정하는 비축 카테고리이며, 일반 보관 분류에도 사용된다. 실제 이동·블록 배치 엔진의 재료 선택을 직접 제어하는 목록으로 취급하지 않는다.

- 돌 5종의 일반 보관 분류 추가를 기록하며, `safeBuildingIds` 편입이나 비축 수량 변경까지 확정한 요구로 확대하지 않는다.
- 현재 건축재 비축량 `64`는 **카테고리 전체 합계**다. 각 돌 종류마다 64개를 남기는 규칙이 아니다.
- 특히 자갈은 일반 보관 후보가 되더라도, 자동 보관 정책의 `isSafeBuilding`이 낙하 블록을 비축 카테고리에서 제외하는 조건은 유지한다. 이 요구로 이동·블록 배치 엔진의 설정이나 판정을 변경하지 않는다.
- 현재 작업에 필요한 수량, 보호 아이템, 주력 장비 등 기존 보호·예약 조건은 계속 우선한다. 일반 보관 분류가 모든 스택의 무조건 전송을 뜻하지 않는다.

### 청금석의 귀중품 처리 의미

귀중품 분류는 보관 목적지를 제한한다. 적격 신뢰 상자가 없으면 일반 상자로 보내지 않고 소지한다. 신뢰 상자가 있어도 기존 계획이 일반 자원만으로 목표 확보 슬롯을 채우면 해당 회차에 청금석을 선택하지 않을 수 있다. “귀중품으로 변경”을 “항상 먼저 보관”으로 해석하지 않는다.

스크린샷과 앞선 설명에서 특정한 청금석 아이템은 `minecraft:lapis_lazuli`이며, 이 문서의 변경 대상도 해당 ID로 한정한다. 청금석 광석·심층암 청금석 광석·청금석 블록, 가공 석재·계단·반 블록 등의 파생 아이템으로 범위를 자동 확장하지 않는다.

## 3. 구현 전 소스에서 확인한 동작

소스 링크는 작업 트리의 현재 파일을 가리킨다. 분류표의 구현 전 내용은 7절의 보존 원본으로 대조하고, 변경하지 않은 Java 판정 경로는 해당 소스에서 확인한다.

1. [정책 JSON](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/resources/lavi/automatic-deposit-policy.json): 돌 5종은 일반 목록·허용 접미사·안전 건축재 목록에 해당하지 않는다. 사탕수수와 청금석은 `generalSurplusIds`에 들어 있다.
2. [분류기](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/policy/AutoDepositItemClassificationPolicy.java): 귀중품·일반 자원 등으로 확인되지 않는 평범한 블록은 `UNKNOWN`을 반환한다. 자동 보관의 건축재 비축 대상 판정은 목록 등록과 낙하 블록 여부를 확인한다.
3. [계획 생성기](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/policy/AutoDepositPlanBuilder.java): `UNKNOWN`을 `UNCLASSIFIED_CONSERVATIVE`, 목적지 `NONE`으로 처리한다. 일반 자원은 일반 상자 후보, 귀중품은 `TRUSTED_ONLY` 후보로 분리한다.
4. 같은 계획 생성기는 보호량·예약량을 제외한 온전한 스택을 후보로 만든다. 일반 후보를 먼저 선택하고 부족한 목표 슬롯만 신뢰 상자 전용 후보로 채운다. 후보는 예상 확보 슬롯 수 내림차순 → 전송 수량 내림차순 → 아이템 ID 순이다. 현재 스택별 후보의 예상 확보 슬롯은 각각 1이므로 같은 분류 안에서는 사실상 큰 스택이 먼저다.
5. [카테고리 비축 정책](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/policy/AutoDepositCategoryReservePolicy.java): 해당 카테고리에서 많이 가진 종류부터 총 비축량을 배분한다. 계획 생성기의 보호 수량 계산은 아래 두 분기로 구분한다.

   - 같은 아이템에 강제 보호(`hard protection`) 스택이 하나라도 있으면 그 아이템의 main 인벤토리 보유 수량 전체를 보호하고 분류·전송 후보 계산을 건너뛴다.
   - 그 외에는 `min(main 보유 수량, max(working-set 예약량, 카테고리 비축량))`을 보호한다. 두 예약량을 합산하지 않으며 보유 수량을 넘겨 보호량을 기록하지 않는다.

6. [정책 로더](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/policy/AutoDepositPolicyLoader.java)는 JAR의 `/lavi/automatic-deposit-policy.json` 리소스를 읽는다. 문서만 저장해도 실행 중인 게임 정책이 바뀌는 구조가 아니다.

## 4. 설명 단계에서 읽은 실제 정책 로그

앞선 읽기 전용 조사에서 다음 원본의 **2026-09-16 02:39:25 +09:00** `AUTO_DEPOSIT_POLICY_ITEM_DECISION`을 확인했다.

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
```

| 아이템 | 당시 원본 행 | itemDecisionId | 관측 수량 | 일반/신뢰 보관 선택 수량 | 관측 판정 |
| --- | --- | --- | --- | --- | --- |
| 안산암 | 1080 | `policy-2-item-3` | 111 | 0 / 0 | `UNKNOWN`, `UNCLASSIFIED_CONSERVATIVE` |
| 섬록암 | 1111 | `policy-2-item-18` | 64 | 0 / 0 | `UNKNOWN`, `UNCLASSIFIED_CONSERVATIVE` |
| 화강암 | 1115 | `policy-2-item-20` | 75 | 0 / 0 | `UNKNOWN`, `UNCLASSIFIED_CONSERVATIVE` |
| 자갈 | 1118 | `policy-2-item-21` | 45 | 0 / 0 | `UNKNOWN`, `UNCLASSIFIED_CONSERVATIVE` |
| 청금석 | 1124 | `policy-2-item-24` | 14 | 14 / 0 | `GENERAL_SURPLUS`, `DEPOSITABLE_SURPLUS` |
| 사탕수수 | 1130 | `policy-2-item-27` | 2 | 2 / 0 | `GENERAL_SURPLUS`, `DEPOSITABLE_SURPLUS` |
| 응회암 | 1132 | `policy-2-item-28` | 26 | 0 / 0 | `UNKNOWN`, `UNCLASSIFIED_CONSERVATIVE` |

`itemDecisionId`는 계획 생성기에서 `policy-<context.epoch()>-item-<항목 순번>`으로 만든다. 위 ID의 `2`는 컨텍스트의 epoch 값이며 정책 JSON의 `revision`이 아니다. 이 표를 정책 revision 2 적용 근거로 해석하지 않는다.

돌 5종은 모두 `protectedCount=0`, `workingSetCount=0`, `categoryReserveCount=0`이었다. 해당 판단에서 남긴 직접 이유는 미분류 항목의 보수적 제외다. 돌의 희귀도가 높거나 건축재 비축량이 할당된 결과로 기록하지 않는다.

이 표의 선택 수량은 **계획의 보관 대상 선정값**이다. 실제 클릭·아이템 전송·서버 반영·종료·cleanup을 증명한 값이 아니다. 해당 정책 행의 `storeOperationId`는 `UNAVAILABLE`이었으며, 이 문서에서 다른 작업 식별자를 만들어 연결하지 않는다.

이 기록은 앞선 설명 단계의 관측 요약이다. 원본 `latest.log`는 게임 실행에 따라 바뀔 수 있고, 이 문서 작업에서 별도 원본 복사본을 만들지 않았다. 당시 행 번호가 미래 원본에도 유지된다고 가정하지 않는다. 이 증거를 [이전 실행 근거](2026-09-16-runtime-evidence.md)의 PID `46980` 실행이나 통제된 A 검증으로 합치지 않는다.

## 5. 구현 계약과 확인 항목

다음은 문서화 단계에서 정한 구현 범위다. 실제 수행 여부와 결과는 7절을 따른다.

- 기존 정책 JSON을 소유자로 삼아 돌 5개 ID를 일반 보관 목록에 추가하고, 청금석 ID를 일반 목록에서 귀중품 목록으로 옮기는 방향이다. 사탕수수는 유지한다.
- 기존 분류기·계획 생성기·신뢰 상자 선택 경로를 재사용한다. 새로운 우선순위 시스템이나 관리자 계층은 이 요구를 위해 필요하지 않다.
- 이동·건축용 비축, STOP·자동방어·보호 아이템·working-set·KEEP_LOADOUT·GUI 바인딩·실행 제한·재발동 정책은 기존 계약을 유지한다.
- 추후 검증에서는 돌 5종의 일반 보관 후보 포함, 청금석의 일반 상자 제외와 신뢰 상자 조건, 사탕수수 유지, 보호·예약 수량 보존을 확인한다. 자갈이 자동 보관의 건축재 비축 카테고리에 새로 들어가지 않는지도 확인한다.
- 정책 리소스를 변경했다면 변경 후 테스트·JAR 패키징과 실제 적용 여부를 별도로 확인한다. [슬롯 이벤트 연결 수정의 기존 279개 테스트·빌드](../../../../plugins/Minecraft/docs/slot-click-counter-fix-20260916/implementation.md)는 **분류 변경 전 결과**이며 새 분류의 검증 근거로 대신 쓰지 않는다.

## 6. 이전 문서화·검수 결과

- 요청한 분류, 현재 동작, 관측 근거와 미구현 범위를 분리하여 저장.
- 문서 목록 연결 및 상대 링크·Markdown diff 확인.
- 운영 정책과 코드 변경 없음. 이 분류 요구에 대한 테스트·재빌드·배포·실게임 검증은 미수행.

### 검수 반영

2026-09-16 재검수에서 자동 보관 비축과 실제 배치 엔진의 경계를 구분하고, 보호 수량 계산의 강제 보호 분기·보유량 상한을 보완했다. 로그 ID의 epoch와 정책 revision도 구분했다. 이 검수 단계에서는 요구사항을 유지하고 구현하지 않았다.

## 7. 후속 구현과 검증

상태: **정책 적용·회귀 테스트·1.20.1 clean 재빌드·패키징 확인 완료**. 구현 전 정책에서 새 요구사항 검사의 실패를 확인한 뒤 revision 2를 적용하여 전체 291개 검사를 통과했다. 배포와 수정 정책의 실게임 검증은 **NOT_RUN**이다.

### 변경 소유자와 보존 범위

- 운영 변경 소유자: `src/main/resources/lavi/automatic-deposit-policy.json`. 분류 데이터와 revision을 변경하는 기존 정책 리소스다.
- 요구한 변경: `revision` 1 → 2, 돌 5개 ID를 `generalSurplusIds`에 추가, `lapis_lazuli`를 `generalSurplusIds`에서 `valuableIds`로 이동. 사탕수수·`safeBuildingIds`·비축 수량은 보존한다.
- 분류·보호·계획·신뢰 목적지·전송·진단은 기존의 독립된 소유자를 재사용한다. 이번 변경에서 운영 Java 클래스나 엔진 hook을 추가·분해할 이유는 없다. 테스트의 분류·계획·출력 검증은 각각 별도 파일, 공통 준비는 `support` 하위 폴더에 배치한다.
- 리소스는 Fabric ChatClef의 다중 버전 소스 트리에 있다. 이번 컴파일·JAR 생성·검증 대상은 1.20.1이며, 다른 버전의 빌드·실행 결과를 주장하지 않는다.
- [수정 전 정책 원본](../../../../test/test_Isolation/auto_deposit_classification/20260916-025750-774/before-policy.json), [작업 시작 상태](../../../../test/test_Isolation/auto_deposit_classification/20260916-025750-774/before.json)를 저장했다. 기존 슬롯 클릭 수정과 다른 미커밋 변경을 보존한다.

### 기존 진단 경로와 검증 범위

| 구분 | 소유자·조건·확인할 값 |
| --- | --- |
| 변경 값을 판정하는 경계 | 기존 분류기의 `classify`, 계획 생성기의 `prepare`/`finish`. 실제 보호량·여분·분류·일반/신뢰 후보 선정을 계산 |
| 관측 소유자 | 기존 `AutoDepositPolicyDiagnostics.log`가 확정된 계획을 `AutoDepositPolicySnapshot`/`AutoDepositPolicyItemSnapshot`으로 출력 |
| 이벤트와 상관관계 | `AUTO_DEPOSIT_POLICY_SNAPSHOT`, `AUTO_DEPOSIT_POLICY_ITEM_DECISION`, `AUTO_DEPOSIT_POLICY_STACK_FACT`; `inventorySnapshotId`, `autoPlanId`, `policyContextEpoch`, 기존 자동 작업 문맥 |
| 핵심 값 | `itemId`, `classification`, `disposition`, 보유·보호·working-set·비축·여분 수량, `selectedGeneralTargetCounts`, `selectedTrustedTargetCounts`, `destinationClass`, `decisionReason` |
| 모드와 출력 경로 | 기존 BOUNDARY eligibility lease → 공유 emission gate → bounded event formatter → 기존 출력 sink. OFF·필터·진단 예산은 기능 판정을 선택하지 않음 |
| 유한 용량 | 정책 snapshot/item/stack이 공유하는 POLICY family cap 84, operation detail cap 256, session 일반 cap 4,936. 총 session cap 5,000 중 critical reserve 64. 이벤트 payload cap 4,096 bytes. 7항목·7스택·1 snapshot의 시험은 총 15건이며 빈 진단 범위에서 실행 |
| 실제 출력 확인 | 기존 실제 출력 harness로 최종 문자열의 분류·수량·상관키, OFF/BOUNDARY 동일 기능 결과와 중복 억제를 검증. 결과는 수행 후 아래에 기록 |

이 정책 출력은 일반 상세 진단 경로이며, 저장 카운터의 reserved FIRST trace와 구분한다. 예산 소진이나 sink 실패 시 상세 출력이 보장된다고 주장하지 않는다. 이번 변경은 새 lifecycle·실패 결정·재시도·종료 경계를 만들지 않으며 기존 제어·진단 예산을 변경하지 않는다.

기존 로그에는 `policyRevision` 필드가 없다. revision 2는 실제 로더 검사와 정책 리소스·산출물 해시로 별도 확인한다. `policy-2`나 계획 ID를 revision 증명으로 대신 사용하지 않는다. 관측 코드를 새로 추가하는 대신 기존 경로가 바뀐 실제 판정값을 출력하는지 검증한다.

### 수행 결과

| 항목 | 결과와 근거 |
| --- | --- |
| 정책 적용 | revision 2. 보존 원본에서 요청한 revision·7개 아이템의 분류 계약만 대조했다. 돌 5종 추가·청금석 이동을 제외한 모든 정책 필드와 사탕수수 분류는 동일 |
| 구현 전 실패 확인 | [03:03 테스트 결과](../../../../logs/auto_deposit_1201_Tests_20260916_030308_058/result.json): revision 1에서 291개 중 283개 통과, 새 요구사항 검사 8개 실패. 기존 279개는 통과했으며 컴파일/JVM 실패가 아닌 예상한 분류·선정 불일치 |
| 수정 후 검사 | [03:04 빌드 결과](../../../../logs/auto_deposit_1201_Build_20260916_030448_374/result.json), [전체 로그](../../../../logs/auto_deposit_1201_Build_20260916_030448_374/verification.log): **291/291 통과**, 실패·중단·건너뜀·컨테이너 실패 0. 새 검사 12개 포함 |
| 실제 진단 출력 | BOUNDARY에서 snapshot 1건 + item 7건 + stack 7건의 **최종 출력 문자열 15건**을 기존 메모리 출력 수집 경로로 검증. 일반 6종과 신뢰 전용 청금석의 분류·선정 수량·상관키를 확인. 동일 계획 재출력은 억제되고, OFF에서는 출력 없이 같은 보관 대상·보호량 유지 |
| 1.20.1 빌드 | 새 PowerShell 창에서 기존 `scripts/verify-auto-deposit-1201.ps1 -Mode Build` 실행. 03:04:48~03:07:52 +09:00, Gradle `BUILD SUCCESSFUL in 2m 59s`, exit 0. `:1.20.1:clean`, `compileJava`, `jar`, `remapJar` 실행 및 `--rerun-tasks` 확인. 다른 버전 컴파일·JAR 생성 없음 |
| 소스·산출물 연결 | [소스 manifest](../../../../logs/auto_deposit_1201_Build_20260916_030448_374/source-manifest.json)의 입력 **2,213개**와 현재 파일 해시가 모두 일치. 이전 2,209개 입력 중 정책 파일 1개만 변경되고 나머지 2,208개 보존; 새 테스트 파일 4개 추가 |
| JAR 정책 확인 | [산출물 검증 기록](../../../../test/test_Isolation/auto_deposit_classification/20260916-025750-774/artifact-verification.json): JAR 내 정책이 소스와 **바이트 단위 일치**, revision 2 및 정확한 ID 분류 확인. 분류기 클래스 major 61(Java 17) 확인 |
| 배포·실게임 | **NOT_RUN**. 새 JAR의 게임 적용, 실제 적격 신뢰 상자 선택, 아이템 전송·서버 반영·종료·cleanup은 이번 테스트·빌드로 증명하지 않음 |

새 검사 코드는 기존 테스트 영역 안에서 책임별로 배치했다.

- [분류 검사](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/policy/classification/AutoDepositResourceClassificationTest.java): 실제 classpath 정책 로더, 돌·사탕수수·청금석 및 비대상 ID, 자갈의 낙하 블록 판정, 기존 건축재 비축 합계.
- [계획 검사](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/policy/classification/plan/AutoDepositResourcePlanTest.java): 일반/신뢰 목적지 분리, 일반 후보의 목표 우선 충족, 강제 보호·working-set 수량 및 온전한 스택 조건.
- [진단 출력 검사](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/policy/classification/diagnostics/AutoDepositResourcePolicyOutputTest.java): 기존 출력 경로의 최종 내용·중복 억제·OFF 동작.
- [공통 준비](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/policy/classification/support/AutoDepositClassificationFixture.java): 실제 Minecraft 아이템·인벤토리와 기존 snapshot/정책/계획 경로를 준비. 신뢰 상자의 적격 후보 snapshot은 테스트에서 제공하므로 실제 상자 탐색·적격 판정의 실게임 증명이 아니다.

진단 출력 검사는 자동 작업 lifecycle에 바인딩하지 않은 계획의 상관관계를 확인한다. 저장 카운터부터 종료까지 한 실행의 연결이나 게임 로그 파일 저장을 증명하지 않는다. 예산 소진·sink 실패의 새 주입 검사는 추가하지 않았다. 이번에 진단 구현을 바꾸지 않았으며, 그 상황의 실제 출력 여부는 미검증이다. `validateAccessWidener`의 `NO-SOURCE`도 독립된 검증 성공으로 세지 않는다.

### 생성된 1.20.1 산출물

- [chatclef-1.20.1-0.18.23.jar](../../../../plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar), **9,015,393 bytes**.
- JAR SHA-256: `9B3202D67128A3BC356AF4F9D686D0B80E7B24ACA8992696DA82E0B33B9ACA97`.
- 소스·JAR 정책 SHA-256: `394E46102E272C23345CA9422C4F70DF0E6570E89497CD9378556FAC7B54EC7D`.
- 기존 슬롯 클릭 Mixin 클래스의 해시는 이전 산출물과 동일하다. 이번 데이터 변경에서 Mixin 동작을 수정하거나 새 실게임 Mixin 검증을 수행한 것으로 보고하지 않는다.

다음 확인에는 이 JAR로 게임을 재시작하고, 가까운 일반 상자와 별도의 적격 신뢰 상자 조건에서 자동 보관의 선정·실제 수량 변화·종료를 대조해야 한다. 일반 자원만으로 확보 목표를 충족하면 청금석이 남는 것은 기존 계획 계약이다. 배포·게임 준비에 관한 사용자 응답은 아직 없으며 외부 인스턴스를 수정하지 않았다. 커밋·푸시도 수행하지 않았다.

`test/test_Isolation/`와 `logs/`의 링크는 현재 PC의 로컬 근거다. Git ignore 대상이므로 새 clone에 원본이 함께 전달된다고 가정하지 않는다.
