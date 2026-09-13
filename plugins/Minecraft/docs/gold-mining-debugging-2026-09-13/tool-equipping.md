<!-- 20260913_kpopmodder: Specify exact selected-slot equipping without changing the generic item-based equipment contract. -->
# 선택한 도구를 정확히 장착하기

상태: `IMPLEMENTED_PENDING_RUNTIME`. 후속 요청의 구현·자동 검증 상태는 [구현 기록](implementation.md)을 따른다. 아래는 검수한 설계이며 [근거](evidence.md)는 수정 전 실행이다.

## 문제와 현재 경계

PlayerInteractionFixChain이 선택한 inventory 19의 손상도 0 삽 대신, 같은 종류의 19·20번을 연속 교환한 끝에 손상도 1111 삽이 장착됐다. 소스는 선택 슬롯을 받지만 실제 실행에서는 Item 종류로 슬롯 목록을 다시 얻는다.

| 현재 소스 | 확인 지점 |
| --- | --- |
| [PlayerInteractionFixChain](../../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/chains/PlayerInteractionFixChain.java) `getPriority()`, 77~107행 | 실제 선택 슬롯과 스택 확보 후 Item 및 진단용 expected source를 전달 |
| [SlotHandler](../../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/control/SlotHandler.java) `forceEquipItem`, 179~217행 | 이미 같은 Item이면 반환; 나머지는 index 1을 선택하고 일치하는 모든 슬롯 교환 |
| 같은 파일 `forceEquipSlot`, 305~307행 | 현재 선택된 칸으로 SWAP하는 낮은 수준 동작. void 반환은 장착 성공 증거가 아님 |
| [StorageHelper](../../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/util/helpers/StorageHelper.java) `getBestToolSlot` | 현재 화면의 플레이어 인벤토리에서 기존 적격성·보호·속도 정책으로 후보 선택 |

이 문서는 후보 선택 정책을 새로 만드는 작업이 아니다. 선택 결과가 실제 장착까지 유지되는 계약을 추가한다. Item 종류만 받는 기존 호출자 전체를 함께 바꾸지 않는다.

## 장착 계약

요청 목적을 **hotbar에 사용 가능하게 배치하기**와 **해당 도구를 현재 손으로 선택하기**로 구분한다. 전자는 역할별 배치·적격성으로 완료되며 손에 계속 들고 있을 필요가 없다. 후자는 해당 입력 소유권이 유효한 실행 시점의 실제 선택까지 확인한다. 이 구분은 아래 장착 어댑터의 내부 계약이며 새 외부 명령·프로토콜을 뜻하지 않는다.

1. 실제 선택 owner가 요청 목적, source 슬롯 좌표계, 예상 스택 값, 현재 플레이어/화면 수명과 목적지 hotbar를 장착 실행에 전달한다. 진단 이벤트에서 이 값을 역으로 읽지 않는다.
2. 입력 직전 source가 같은 플레이어의 main 인벤토리 0~35에 실제로 대응하는지 확인한다. 현재 handler 객체·syncId·window slot과 실제 backing inventory/index를 함께 검증하며 `Slot.isSlotInPlayerInventory()` 하나로 증명하지 않는다. 이 helper는 PlayerScreenHandler의 제작·장비 등 표시 슬롯에도 true를 반환할 수 있다. 이번 어댑터는 커서·제작 결과·장비·외부 컨테이너 칸을 main 슬롯으로 재해석하지 않는다. 예상 Item·개수·손상도·NBT 값과 사용 가능 조건도 검사한다. 서로 같은 값의 스택은 고유 객체 식별 증거가 아니며 ‘선택한 source에 한 번 작용했다’는 실행 근거와 분리한다.
3. source가 유효한 hotbar에 이미 있으면 배치 목적에는 입력이 필요 없고, 손 선택 목적에는 기존 입력 소유권 안의 선택 변경만으로 충분한지 확인한다. 바깥에 있으면 배치 판단이 허용한 destination 0~8에 필요한 단일 SWAP을 한다. 같은 Item의 다른 source를 순회하지 않는다. destination의 현재 값·예약, 두 칸의 교환 가능 여부와 기존 destination 물품이 source로 손실 없이 돌아가는지도 실행 직전에 검증한다.
4. 기존 GUI·커서·슬롯 입력 제한과 클릭 경로를 보존한다. 화면 전환, source 변경, 커서 점유 등으로 계약을 만족하지 못하면 부작용 없이 명시적인 미실행/실패 관측값을 반환한다. 임의 버리기나 추가 클릭으로 해결하지 않는다.
5. 입력 호출의 반환, 로컬 반영, 후속 인벤토리 확인을 구분한다. 배치 목적은 도구가 기대 hotbar에서 적격한지 확인하고, 손 선택 목적은 실제 손 선택도 별도로 확인한다. 즉시 완료된 선택 이후 정상적인 엔진 선택 변경까지 영구히 같은 손으로 고정하지 않는다. 서버 반영을 기다릴 필요가 있는 경로는 operation에 결합된 제한된 확인으로 처리하고 반복 SWAP하지 않는다.
6. STOP·root/월드/화면 교체 후 늦은 확인은 기존 operation을 성공시키거나 새 작업을 변경하지 않는다.

방어·먹기·보관 등 다른 owner가 입력을 선점하면, 뒤늦게 관측한 손 불일치를 배치 실패로 처리하거나 곡괭이를 재선택하지 않는다. 아직 확정되지 않은 요청은 실행 전 미실행, 적용된 배치, 손 선택 확인 보류/선점됨을 구분하고 해당 owner가 다시 실행 가능할 때 현재 상태로 재검증한다. 기존 action이 이미 적용됐거나 적용 여부가 불명확하면 같은 교환을 다시 보내거나 역교환하지 않는다. 이때도 실제 도구 소실·미반영을 성공으로 가정하지 않는다.

최초 교환 반영 확인과 이후 정상 사용에 따른 손상도·NBT 변화는 다른 사건이다. 이미 정확한 반영을 확인한 도구가 채굴로 마모됐다고 장착 실패로 소급 변경하지 않는다. 최초 반영 자체를 확인하지 못했다면 손상도가 늘었다는 이유만으로 올바른 source였다고 추정하지 않고 미확정 상태로 남긴다. 기존 실행 조건과 정상 입력 owner의 결과를 사용하며, 선택 체인 조회나 추가 getPriority 평가로 새 전역 입력 정책을 만들지 않는다.

caller의 `SAME_ITEM_TYPE_SKIP`은 실행 요청을 만들기 전의 기존 후보 교체 정책이다. 첫 수정에서는 이 분기를 유지하고 `EQUIP_REQUEST`로 결정된 경로를 정확한 source 실행에 연결한다. 해당 생략은 실행 성공이 아니다. 반대로 정확한 source 요청을 받은 새 어댑터는 종류만 같다는 이유로 다른 스택을 성공으로 인정하지 않는다. 향후 같은 종류 도구 사이의 적극적 교체가 필요하다면 그 선택 정책은 별도 근거·테스트가 필요한 변경으로 구분한다.

## 책임 분리와 범위

새 LAVI 구현이 필요하면 `integration/toolselect/` 아래에 장착 요청 값, 입력 직전 검증, 단일 장착 실행, 반영 확인을 책임별 타입으로 둔다. 구체 파일명은 후속 구현 시 기존 도우미와 중복 여부를 확인해 확정한다. 임시 도구의 목적지를 정하는 책임은 [작업 배치](hotbar-layout.md)가 맡는다.

upstream에는 해당 도구 선택 경로의 최소 연결만 둔다. 장착용 전역 thread·logger·static registry를 만들지 않으며 getPriority를 추가 호출하지 않는다. 변경 단위는 exact-slot 장착 경로와 그 테스트로 한정하고 다른 Item 기반 호출은 보존한다.

공통화는 검증된 source→destination 이동까지로 한정하고, active hand 선택은 요청 목적의 별도 동작이다. 현재 `forceEquipSlot()`은 selectedSlot을 목적지로 사용하므로 그대로 호출하는 것만으로 이 분리가 성립하지 않는다. 기존 `forceEquipItem(Item)`·`ItemTarget`의 먹기 제한, 커서·보조손 경로는 이번 exact 어댑터의 main-slot 계약으로 일괄 변경하지 않는다.

## 관측과 검증

기존 bounded 진단을 재사용해 선택/실행/반영을 같은 attempt에 연결한다. source·destination 좌표계, 예상/실제 값, 수행한 action 수, 미실행 사유, 호출 반환과 사후 확인을 구분한다. 전체 NBT를 로그에 노출하지 않고 필요한 비교 결과를 남긴다. 진단 실패가 동작을 바꾸지 않아야 한다.

| 자동 테스트 | 기대 결과 |
| --- | --- |
| 같은 종류 삽 2개, 서로 다른 손상도·NBT | 선택 source만 한 번 장착, 나머지 도구에 연속 SWAP 없음 |
| 선택 후 source의 값 변경 | 잘못된 도구를 장착하거나 성공 처리하지 않음 |
| source가 hotbar에 있음 / source=destination | 필요한 선택만 수행, 자기 교환·불필요한 밀어내기 없음 |
| 같은 Item이 이미 손에 있지만 요청 스택 값이 다름 | 단순 Item 일치만으로 요청 성공 판정하지 않음 |
| destination이 선택 후 다른 도구·예약으로 바뀜 | 오래된 배치로 새 owner의 도구를 밀어내지 않음 |
| PlayerScreenHandler 제작·장비·커서/특수 슬롯 / handler·syncId 변경 | 잘못된 좌표 변환으로 main 슬롯을 클릭하지 않음 |
| GUI·커서·입력 제한 / 반영 지연 | 기존 제한 준수, 중복 클릭·임의 버리기 없음 |
| 배치 직후 방어·먹기 선점 / 손 선택 확인 전 선점 | 적격 hotbar 배치를 손 불일치만으로 실패 처리하지 않음; 공격·먹기 방해 및 중복/역교환 없음 |
| 정확한 반영 확인 후 정상 마모 / 최초 반영 미확정 | 사후 마모를 장착 실패로 소급하지 않음; 불명확한 변경을 성공 근거로 사용하지 않음 |
| SAME_ITEM_TYPE_SKIP / generic Item·ItemTarget·커서·보조손 호출 | 기존 미실행/먹기/장착 계약 보존, exact 요청의 성공과 구분 |
| STOP·월드/root 교체 후 늦은 확인 | 새 작업에 영향 없음, 이전 요청 중복 성공 없음 |

실게임에서는 여러 삽이 있는 기존 조건에서 실제 선택·장착 도구가 일치하는지 확인한다. 이 단계만으로 채굴 반복이나 금괴 10개 전체 성공을 선언하지 않는다. 후속 소스·자동 테스트 결과는 [구현 기록](implementation.md), 실게임은 `NOT_RUN`이다.
