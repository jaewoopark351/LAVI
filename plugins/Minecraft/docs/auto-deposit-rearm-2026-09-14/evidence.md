<!-- 20260914_kpopmodder: Preserve the auto-deposit rearm incident's log evidence, artifact provenance, and observation limits separately from the proposed behavior change. -->
# 자동보관 재실행 문제의 실행 근거

## 상태와 자료 범위

이 문서는 기존 실행 로그와 소스 조사 결과를 보존한다. 새 재실행 수정안은 **PLAN_ONLY**이며, 이번 문서 작업에서 코드·테스트·설정·원본 로그를 수정하지 않았다. 새 수정안의 테스트·빌드·배포·Minecraft 재현은 **NOT_RUN**이다. 설계와 적용 범위는 [묶음 안내](README.md)를 따른다.

핵심 근거는 로컬 [전달 묶음 manifest](../../../../logs/chatgpt_auto_deposit_rearm_20260914_011954/manifest.json), [당시 조사 보고](../../../../logs/chatgpt_auto_deposit_rearm_20260914_011954/01_EVIDENCE_KO.md), [질문](../../../../logs/chatgpt_auto_deposit_rearm_20260914_011954/CHATGPT_QUESTION_KO.md), [이벤트 색인](../../../../logs/chatgpt_auto_deposit_rearm_20260914_011954/excerpts/event-index.json)이다. 이 `logs/` 묶음은 **Git ignored 로컬 자료**이므로 저장소 복제만으로 함께 전달되지 않는다. 재현 가능한 핵심 필드는 아래에 보존하며 원문 로그 전체를 문서에 복사하지 않는다.

묶음은 2026-09-14 **01:19:54 KST**부터 파일별로 순차 수집했고 **01:23:56 KST**에 포장을 마쳤다. 실행 중 로그를 서로 다른 시점에 읽었으므로 파일 사이의 완전 동시 스냅샷이 아니다. 아래 행 번호는 묶음에 보존된 `latest.log` 원본 기준이며, 같은 줄 순서를 유지한 UTF-8 변환본에도 대응한다.

## 실행·소스 식별

| 항목 | 수집 당시 근거 |
| --- | --- |
| 인스턴스 | `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01` |
| 런타임 | Minecraft 1.20.1 / Fabric Loader 0.19.3; `latest.log` 1행 |
| 설치 JAR | `mods/chatclef-1.20.1-0.18.23.jar`, 8,773,970 bytes |
| 설치 JAR SHA-256 | `5ae4159a2af60714ff3cc6373dd7fa0047f02a9ced8353a1e7ba58abe48a38a1` |
| Git HEAD | `b3f00712605c00d97be4f7c04e16e0f6784f9d1f` |
| 브랜치 | `minecraft-plugin-fix/alto-clef-infinite-loop` |
| 수집 당시 Git 상태 | manifest의 `git_status`는 빈 문자열 |
| bridge 세션 | `fabric-chatclef-b39705ad514e44f7baeb01d26dec4868`; handshake 수락은 246행 |
| Java 명령 연결 세대 | 명령 문맥의 `commandConnectionGeneration=1` |
| LAVI 어댑터 연결 세대 | 앱 원본 4347행의 같은 bridge 세션에 `generation=3`. Java 내부 세대와 별도 소유자의 값이며 서로 같은 수치라고 가정하지 않음 |
| 첫 자동보관이 발생한 사용자 명령 | `lavi-input-ko-118465def48d4d3e9c6c8dd33bbc7d5e`, `get gold_ingot 10` |
| 첫 요청 correlation | `lavi-4df785bc40084b699e67db12e4de6a5e` |
| 자동보관 실행 | `auto-deposit-1`, `operationEpoch=1` |

설치 JAR 식별은 당시 실제 설치 파일을 기록한 manifest의 증거다. JAR 자체는 이 전달 묶음에 포함되지 않았다. 현재 문서 작성은 새 JAR 빌드·배포나 설치 파일의 재해시 검증을 수행한 것이 아니다. 이전 소스 구현·346개 테스트·빌드 기록은 [금 채굴 구현 기록](../gold-mining-debugging-2026-09-13/implementation.md)의 과거 검증이며 새 재실행 수정안의 검증으로 사용하지 않는다.

| 보존 입력 | bytes / 행 수 | 인코딩 | SHA-256 |
| --- | --- | --- | --- |
| `logs/latest.log` | 13,807,818 / 5,148 | CP949 | `a9152444e6cb815463298c478cf0a0b5e5092f508635ad771df8449e83665c75` |
| `logs/stdout-logs.txt` | 14,671,872 / 15,208 | UTF-8 | `a639a4091b153571a27122ff01144ba2ee9c6e55f579272f168f6e29eff8dac1` |
| `logs/instance_audit.txt` | 3,046,766 / 600 | UTF-8 | `e9d097605c21b9cdee9acbd14c801ef60143c4cd5cbf2c6d03833b10a3585429` |

위 세 파일은 수집한 원본과 바이트가 같은 사본이다. LAVI 앱 자료는 `logs/20260913_211419_log.txt`의 원본 **4310~5398행**을 번호와 함께 발췌한 별도 파일이며 전체 앱 원본과 바이트가 같지 않다. 해당 발췌 SHA-256은 `66acd04c4acfe921000f40fa1270fb0132bb89d4e1b8e2e36fc5a37a09f7c574`이고, 전체 앱 원본 해시는 manifest에 없다.

## 시간순 직접 관측

| 시각 / latest 행 | 관측과 의미 |
| --- | --- |
| 00:36:16 / 2427, 2498 | 점유 33/36, `expectedFreedSlots=5`, 일반 보관 5단계·신뢰 보관 0단계의 계획. `ARMED→RUNNING`, `planningStatus=READY` |
| 00:36:16 / 2534 | 자동보관의 상자 확보 → 판자 8개 준비 → 원목 채굴 경로가 현재 Task chain에 나타남 |
| 00:37:06~07 / 3474, 3489 | 인벤토리 21번 슬롯의 조약돌 6개, 24번 슬롯의 참나무 판자 8개가 각각 직접 관측됨 |
| 00:37:10 / 3543~3578 | 전송 직전 `scanId=5194`의 `targetMap=PLAYER` 36슬롯을 재계산하면 35칸 점유 |
| 00:37:11~13 / 아래 전송 표 | 흙 두 스택·달걀·이끼 카펫·철 도끼의 5개 소스 슬롯이 비워지고 상자 측 수량 증가가 함께 관측됨 |
| 00:37:13 / 3888 | `VERIFY_WORKING_SET→VERIFY_FREE_SLOTS`, `deficitTypeCount=0` |
| 00:37:13 / 3890~3893 | 시작 33칸 대비 종료 30칸, 순감소 3칸으로 `PARTIAL_RELIEF`. 이어 `VERIFY_FREE_SLOTS→DONE`, `RUNNING→WAIT_FOR_REARM` |
| 00:37:41 / 4527 | 다시 점유 33/36이지만 WAIT 유지, 새 계획 미평가 |
| 00:41:04 / 4709 | 점유 36/36, `thresholdReached=true`인데 WAIT 유지 |
| 00:49:28 / 4985 | 마지막 직접 자동보관 판정도 36/36·WAIT, low-water 미충족, trusted revision 1→1, 새 working-set·계획 미평가 |

### 전송 5칸과 순감소 3칸

계획 당시 `AUTO_DEPOSIT_POLICY_STACK_FACT`의 `physicalStackLocation=MAIN`은 33개이고 ARMOR 4개는 별도다. 비어 있던 MAIN 슬롯 21·24는 전송 직전에 다음과 같이 바뀌었다.

```text
[3555 / 00:37:10] scanId=5194 targetMap=PLAYER inventorySlot=21 itemId=minecraft:cobblestone itemCount=6
[3558 / 00:37:10] scanId=5194 targetMap=PLAYER inventorySlot=24 itemId=minecraft:oak_planks itemCount=8
[3565 / 00:37:10] scanId=5194 targetMap=PLAYER inventorySlot=31 itemId=minecraft:air itemCount=0
```

집계는 [전송 직전 인벤토리 추출](../../../../logs/chatgpt_auto_deposit_rearm_20260914_011954/excerpts/pre-transfer-inventory-5194.json)에 보존했다. **같은 scanId의 상자 슬롯 27개는 제외**하고 `targetMap=PLAYER`의 인벤토리 0~35만 센 결과가 35/36이다.

| 보관 작업 / 물품 | 클라이언트 양쪽 변화 행 | 확인한 수량과 소스 슬롯 |
| --- | --- | --- |
| `store-deposit-2556` / 흙 | 3607~3608, 3633~3634 | 처음 64개, 이어 15개; 누계 79개. 소스 슬롯 19·8 |
| `store-deposit-5240` / 달걀 | 3712~3713 | 2개. 소스 슬롯 11 |
| `store-deposit-5266` / 이끼 카펫 | 3792~3793 | 2개. 소스 슬롯 14 |
| `store-deposit-5294` / 철 도끼 | 3872~3873 | 1개. 소스 슬롯 34 |

각 최종 전송 관측은 플레이어 수량 감소와 상자 수량 증가가 대응하고 `pairedDeltaObserved=true`, 커서 수량 0을 기록한다. 흙 계획은 64+10개였지만 실행 시 둘째 스택은 15개였다. 이 수량 변화도 계획 이후 인벤토리가 변했다는 관측이며 추가 5개의 정확한 획득 경위를 증명하지 않는다.

```text
시작 점유 33 → 준비 중 점유 35 → 5개 소스 슬롯 비움 → 종료 점유 30
[3890 / 00:37:13] observedEvent=AUTO_DEPOSIT_FREE_SLOT_POSTCONDITION observedReason=PARTIAL_RELIEF
startingOccupiedSlots=33 endingOccupiedSlots=30 actualFreedSlots=3 signedFreedSlotDelta=3 expectedFreedSlots=5
slotDeltaAttribution=NOT_PROVEN_BY_OCCUPANCY_CHANGE
```

따라서 전송으로 비운 소스 슬롯 5칸과 시작 대비 순감소 3칸은 양립한다. `actualFreedSlots`를 5로 바꾸거나 결과를 FULL_RELIEF로 바꿀 근거가 아니다. “5칸 중 2칸은 전송에 실패했다”는 해석도 이 기록과 맞지 않는다.

전송 증거는 **클라이언트 live handler 관측**이다. `serverAcknowledgementObserved=false`, `targetScreenAssociationProven=false`, GUI 전송 로그의 `operationId=unavailable` 한계를 유지한다. store 작업의 전송 선택, 동일 tick·소스 슬롯·handler·물품 기록을 대조한 연결이며 완전한 작업 ID 결합 또는 서버 ACK가 아니다. 별도 store effect summary의 `effectObservationCount=0`, `effectVerified=false`는 해당 집계 경계에서 효과 검증이 성립하지 않았다는 기록이므로 실제 전송 0회의 증거로 사용하지 않는다.

### 다시 찼을 때의 직접 판정

```text
[3893 / 00:37:13] observedReason=automatic_task_terminal previousState=RUNNING nextState=WAIT_FOR_REARM occupiedSlots=30 lowWaterReached=false
[4985 / 00:49:28] observedEvent=AUTO_DEPOSIT_DECISION observedReason=waiting_for_rearm
stateBefore=WAIT_FOR_REARM stateAfter=WAIT_FOR_REARM occupiedSlots=36 freeSlots=0 totalSlots=36
thresholdReached=true lowWaterReached=false trustedRevisionChanged=false waitOriginTrustedRevision=1 currentTrustedRevision=1
workingSetStatus=NOT_EVALUATED planningStatus=NOT_EVALUATED waitElapsedTicks=14709
```

뒤 관측의 명령 요청 ID는 첫 보관 요청과 달라진다. 4709행은 `lavi-input-ko-5bb4b4f9ea5e40ada6587641ca5c7544`, 4985행은 `lavi-input-ko-60b9b08f967944d5889f0ce3bc25d44c`이며 bridge 세션은 동일하다. 이를 하나의 요청이 계속 실행됐다고 적지 않는다. 새 요청 문맥에서도 기존 재무장 대기가 남아 있다는 관측이다.

묶음의 마지막 latest 행은 **01:19:01 Heartbeat**다. 마지막 직접 자동보관 판정은 **00:49:28**이며, 이후 파일 증가·Heartbeat·Idle 화면만으로 내부 상태가 동일하게 유지됐다고 확정하지 않는다.

## 소스에서 확인한 원인 경계

공통 소스 경로는 `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/`이다. 아래는 새 구현 제안이 아니라 기준 HEAD의 현재 계약이다.

| 소스 | 확인한 연결 |
| --- | --- |
| [DepositAllInventoryPressureSnapshot](../../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureSnapshot.java) | 발동 33칸 이상, low-water 28칸 이하. 33칸 시작의 목표 relief는 5칸 |
| [AutoDepositFreeSlotVerifier](../../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/relief/AutoDepositFreeSlotVerifier.java) | 전송 횟수가 아니라 시작·종료 점유의 순차이로 FULL/PARTIAL/NO_SLOT_RELIEF를 분류 |
| [AutoDepositMaintenanceTask](../../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceTask.java) | VERIFY_FREE_SLOTS에서 결과를 보존한 뒤 DONE으로 종료 |
| [DepositAllInventoryPressureChain](../../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureChain.java) | onTaskFinish에서 결과 구분 없이 WAIT로 연결. WAIT 검사에서 새 working-set·계획 평가 전에 반환 |
| [DepositAllInventoryPressureStateMachine](../../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureStateMachine.java) | 실행 종료 후 WAIT. low-water 관측 또는 trusted 정책 변경 경로로 재무장 |

**이번 재실행 차단의 직접 원인은 종료 후 WAIT와 그 해제 조건의 결합이다.** 30칸까지 비운 뒤 다시 33~36칸이 된 것이 기존 해제 조건을 충족하지 않았다. ARMED이면 Idle에서도 계획이 가능하므로 Idle 자체를 자동보관 불가능의 원인으로 일반화하지 않는다. 기존 테스트가 이 WAIT 계약을 명시하므로 후속 수정은 단순 누락 보완이 아니라 계약 변경으로 다룬다.

## 미확정 사항과 외부 검토

- 추가 조약돌·판자가 인벤토리에 들어온 사실과 상자 준비 Task 경로는 확인했다. 각 물품의 정확한 파괴·드롭·획득·제작 경위까지 전부 확정하지 않았으며 자동방어 드롭을 원인으로 단정하지 않는다.
- 첫 계획은 일반 보관 5단계, 신뢰 보관 0단계였다. 신뢰 상자 포화가 이번 WAIT의 원인이었다는 근거는 없다. WAIT 해제 후 안전한 surplus·사용 가능한 보관소가 있었을지는 새 계획이 미평가되어 미확정이다.
- 명시적 STOP 이후 자동보관이 runner를 재활성화하는 문제는 별도 제어 경계의 검토·검증 대상이다. **이번 로그로 재현된 사실이 아니다.** 자동방어·먹기 선점과 STOP을 이번 `PARTIAL_RELIEF→DONE`의 원인으로 추가하지 않는다. 동시에 기존 DONE 관측을 새 설계가 요구하는 typed 정상 terminal·필수품·cleanup 조건이 모두 확인됐다는 증거로 확대하지 않는다. 이 정상 조건을 설정한 회귀 입력은 과거 로그와 별개다.
- 사용자가 전달한 외부 ChatGPT 검토 원문은 `C:\Users\jaewo\Downloads\CODEX_AUTO_DEPOSIT_REARM_REVIEW_KO.md`, SHA-256 `affe4607199ea4b2cf0b91fc0437e32ffc666c908beb8bf93a96b962d4b5e0c7`이다. 전달 메시지는 `C:\Users\jaewo\.codex\attachments\972dbbaa-ded3-4143-abae-ca3cebe5e69d\pasted-text.txt`, SHA-256 `a88165cc532444fbbd3b941b8054632dcfa592c107e8a93e66fb33257a076f99`이다. 원문은 읽기와 해시 확인만 수행했으며 저장소에 복사하지 않았다.
- 외부 검토의 원인 분석·후속 설계 의견은 로컬 로그·소스와 대조할 참고 자료다. 그 문서의 별도 전체 소스 ZIP 비교 보고는 이 문서 작업의 독립 검증으로 취급하지 않는다. 제안된 실행 횟수·재평가 정책·STOP 보완 역시 새 구현 또는 런타임 성공 증거가 아니다.

## 후속 방향 검토의 반영 범위

2026-09-14 사용자는 대화에서 자동방어가 자동보관보다 반드시 우선해야 한다고 재확인하고, ChatGPT의 후속 방향 검토를 전달한 뒤 문서화를 요청했다. 이 후속 입력의 출처는 대화 본문이며 위 다운로드 파일의 내용이나 해시를 새 검토문으로 바꾸지 않는다.

반영한 방향은 방어 중단·실제 보관 실패·정상 정리 뒤 재충전을 구분하는 것이다. 방어 선점 자체는 보관 실패·무진척을 소비하지 않되, 중단 전 실제 기록은 유지한다. 기존 문서의 root 등록 총2회와 유한 안전 재개 평가 횟수 소진으로 차단하는 제안은 철회했다. 실패 범위·관련 변화·제한된 재확인과 보관 cleanup의 방어 비간섭을 [설계](design.md)와 [검증 계획](verification.md)에 반영했다.

이 변경은 **설계 방향의 갱신**이다. 위 실행에 방어 선점 때문에 보관 한도가 소진됐다는 새 증거가 생긴 것은 아니며, 새로운 로그 수집·소스 구현·게임 재현을 수행하지 않았다. 실제 한도와 측정 경계도 미확정으로 유지한다.

이 근거는 [기존 자동보관 진단 계획](../chatclef-gold-mining-tool-loop-auto-deposit-diagnostics-plan-2026-09-13.md)과 [금 채굴 실행 근거](../gold-mining-debugging-2026-09-13/evidence.md)의 과거 기록을 덮어쓰지 않는다. 이번 사건의 확정된 재실행 경계와 아직 검증되지 않은 수정안을 구분해 이어서 사용한다.
