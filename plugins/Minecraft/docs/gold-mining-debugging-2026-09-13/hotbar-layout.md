<!-- 20260913_kpopmodder: Plan operation-owned coexistence of mining and temporary tools while preserving defense. -->
# 작업 도구의 단축바 공존

상태: `IMPLEMENTED_PENDING_RUNTIME`. 후속 구현·자동 검증은 [구현 기록](implementation.md)을 따른다. [목차](README.md), 수정 전 [근거](evidence.md), [정확한 장착](tool-equipping.md)을 함께 읽는다.

## 문제와 유지 조건

현재 일반 장착 경로와 ACCESS 돌 곡괭이의 기본 위치가 hotbar index 1이다. 삽이 곡괭이를 밀어내면 부모가 준비 자식을 다시 선택하고 기존 채굴 자식을 중단한다. 정확한 도구를 한 번만 장착하더라도 이 배치 충돌은 남을 수 있다.

Baritone 기본 ToolSet은 hotbar 0~8만 검사한다. 따라서 `MiningOperationToolState.ready()`에서 hotbar 조건을 지우거나 최초 준비 후 검증을 전부 생략하는 해결책을 채택하지 않는다. **손에 든 도구는 삽·무기로 바뀔 수 있고, 채굴에 필요한 적격 곡괭이는 엔진이 사용할 수 있는 칸에 유지돼야 한다.**

현재 `MiningOperationToolPolicy`의 TARGET 잔여 내구도 기준 `max(1, targetCount - targetInventoryCount) + 4`, ACCESS 돌 곡괭이 최소 잔여 내구도 32, 기존 `shouldSaveStack`·적합성 필터는 별도 변경하지 않는다. 이 값들은 채굴 도구 내구도 정책이며 GOTO 재료 확보 정책과 무관하다.

## 수정 후보 경계

| 파일 | 책임 |
| --- | --- |
| [MiningOperationToolState](../../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/integration/mining/operation/MiningOperationToolState.java) `preferredHotbarSlotFor`, `ready`, `nextStep` | 현 위치·역할·적격성 표현. 충돌하지 않는 배치 판단과 결과 연결 |
| [MoveMiningToolToHotbarTask](../../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/integration/mining/operation/MoveMiningToolToHotbarTask.java) `onTick` | 선택 source와 destination의 검증된 이동·반영 확인 |
| [PrepareMiningOperationToolsTask](../../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/integration/mining/operation/PrepareMiningOperationToolsTask.java) | 배치 필요와 실제 도구 획득 필요 구분 |
| [PrepareThenMineRawGoldTask](../../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/integration/mining/operation/PrepareThenMineRawGoldTask.java) `onResourceTick` | 이미 유효한 배치에서는 채굴 자식 유지, 실제 적격성 상실 시 필요한 준비 |

## 배치·복구 계약

1. 실제 작업 수명에 결합해 TARGET, ACCESS, 일반 임시 장착 위치를 구분한다. 기존 적격 hotbar 도구는 안정적으로 유지한다. 이번 로그의 TARGET index 6은 예시이며 전역 고정 규칙으로 만들지 않는다.
2. 기존 엔진이 쓰는 칸, 도구·아이템 보호, 작업 예약과 현재 화면을 확인해 충돌 없는 후보를 선택한다. source가 이미 유효한 hotbar라면 불필요하게 이동시키지 않는다.
3. hotbar 9칸이 차 있어도 일반 인벤토리의 source와 안전한 칸을 SWAP할 수 있는지 평가한다. 이동 금지·예약 항목은 피하고 임의 버리기·보관 강제 실행으로 해결하지 않는다. 슬롯 수가 찼다는 이유만으로 이동 가능성을 0으로 간주하지 않는다.
4. 실행은 [정확한 장착 계약](tool-equipping.md)의 **hotbar 배치 목적**에 연결한다. 계획을 만들었다는 이유로 READY를 확정하지 않고 실제 반영된 도구와 적격성을 재검증한다. 두 곡괭이가 동시에 손에 들려 있어야 하는 조건은 없으며, 준비를 위해 selectedSlot을 잠시 변경하는 낮은 수준 동작과 계속 그 도구를 손에 들도록 강제하는 정책을 구분한다.
5. 실제 도구 소실·마모·보호 조건 변경과 일시적인 선점을 구분한다. 유효한 대체 도구가 있으면 불필요한 획득 Task를 시작하지 않는다.
6. 안전한 배치가 없으면 명시적인 배치 불가 결과와 operation 소유의 유한한 복구/종료 방침을 사용한다. 같은 준비 자식을 매 tick 무한 재선택하지 않는다. 복구 필요 여부, 횟수·확인 대기 상한과 기존 비성공 결과 연결은 후속 구현에서 source 변경 전에 구체화하고 테스트한다. 단순 cooldown만 넣어 충돌을 숨기지 않는다.
7. 자동방어·자동보관 선점 시 해당 체인의 동작을 허용한다. 선점 중에는 채굴 배치의 미완료 action·후속 확인을 이유로 추가 교환이나 곡괭이 재선택을 하지 않는다. 복귀 후 현재 도구와 예약을 다시 읽고 이미 반영된 이동을 반복하지 않게 한다. 방어 중 곡괭이 강제 선택, 공격 차단, 선점을 준비 실패·명령 종료로 해석하는 동작은 금지한다.
8. STOP·root 교체·월드 변경 시 해당 작업의 배치/예약 상태만 정리한다. 다른 작업의 입력·예약·경로를 전역 해제하지 않는다.

**보호품과 이동 금지를 구분한다.** `ItemHelper.canThrowAwayStack`의 보호는 버리기 제한이고, `StorageHelper.shouldSaveStack`과 도구 정책은 채굴 사용·내구도 보존 조건이다. 이를 전부 hotbar 고정으로 해석하지 않는다. 보호품도 두 플레이어 저장칸 사이에서 손실 없이 이동할 수 있는지 검토하되, 현재 무기 사용·역할별 도구 배치·명시적인 예약을 깨지 않아야 한다. full hotbar의 안전한 SWAP은 source와 destination 물품을 모두 보존한다.

## 책임과 상태 추가 기준

LAVI의 `integration/mining/operation/` 아래 배치 계획, 실행 연결, 작업 수명 예약/정리 책임을 focused type과 의미 있는 하위 패키지로 나눈다. 현재 도구 상태 값 객체가 클릭·수명 관리까지 맡지 않게 한다. 진단 registry에 실제 예약을 저장하지 않는다.

우선 정확한 장착과 배치 충돌을 해결해 채굴 자식이 유지되는지 본다. 최초 준비/진행 중 검증 분리는 그 이후에도 필요한 상태가 확인될 때만 최소 도입한다. 큰 부모 상태 머신이나 전역 슬롯 정책을 선제적으로 추가하지 않는다.

## 관측과 검증

실제 결정한 역할별 도구·칸, 배치 이유, 변경 전후 적격성, 자식 유지/교체 이유, 배치 불가·복구 소진을 기존 bounded 로그로 기록한다. 최초 원인·누계는 자식 재시작 때문에 초기화하지 않으며 정상 무변화는 요약한다.

| 자동 테스트 | 기대 결과 |
| --- | --- |
| 돌 곡괭이 + TARGET 곡괭이 + 삽 교체 | 필요한 곡괭이와 삽 공존, 채굴 자식 반복 교체 없음 |
| 여러 적격 도구 | 기존 hotbar 후보의 안정적인 선택·배치 |
| hotbar 9칸 점유 | 안전한 SWAP 또는 명시적 배치 불가; 아이템 손실·무한 준비 없음 |
| 보호품이 있는 full hotbar / 활성 무기·역할 예약 칸 | 버리기 보호를 이동 금지로 오해하지 않음; 활성 사용·예약은 보존, 양쪽 물품 손실 없음 |
| 도구 소실·내구도/보호 기준 미충족 | 실제 재준비 수행, 유효한 대체품이 있으면 불필요한 획득 없음 |
| 배치 실행 전/후 방어·자동보관 선점 및 복귀 | 공격·보관 허용, 뒤늦은 손 확인으로 배치 실패 오판·재교환 없음, 복귀 재검증, 명령 오종료 없음 |
| root/월드 교체·STOP | operation 상태 정리, 새 작업·늦은 반영 격리 |

실게임 완료는 준비 표시가 아니라 실제 지하 접근과 블록 파괴 진행으로 확인한다. 광석 회수·제련·금괴 수량·명령 terminal은 [통합 검증](README.md)의 별도 단계다. 현재 새 동작·자동 테스트·실게임 검증은 수행하지 않았다.
