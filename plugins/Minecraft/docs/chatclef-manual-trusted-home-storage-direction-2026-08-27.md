<!-- 20260827_openai: Documented the explicit-request-only manual trusted home storage direction. -->
<!-- 20260827_openai: One-time directional review approved the design with focused safety and V1-scope clarifications. -->
<!-- 20260827_openai: Recorded the authorized H1-H4 source implementation without claiming build or runtime verification. -->

# ChatClef Manual Trusted Home Storage Direction

문서 상태: `H1_H4_SOURCE_IMPLEMENTED_BUILD_AND_RUNTIME_NOT_VERIFIED`

작성 기준일: 2026-08-27

이 문서는 Fabric ChatClef 1.20.1에서 사용자가 명시적으로 요청했을 때만
등록된 trusted destination을 집 보관소로 사용해 인벤토리를 정리하는 방향을
정의한다.

이 문서 자체는 Java 또는 JSON 수정, build, test, JAR 배포, Minecraft 실행,
commit 또는 push를 승인하지 않는다. 각 작업은 별도의 사용자 승인이 필요하다.

이후 2026-08-27 사용자 요청은 H1-H4의 Java source, focused test source와 이 문서
상태 갱신을 명시적으로 승인했다. 이 승인은 Gradle test/build, H7 JAR 배포,
Minecraft 실행, commit 또는 push까지 확대되지 않는다.

2026-08-27 1회 방향성 검수 결과는 `APPROVE WITH DIRECTIONAL CHANGES`다.
이 판정은 아래에 반영한 cursor 안전 gate, safety-chain interruption 의미,
trusted registry 재검증, V1 기본값과 범위 고정을 조건으로 한다. 요구사항이
다시 바뀌지 않는 한 동일 방향의 추가 docs-only 재검수는 만들지 않는다.

## 1. 최우선 불변조건

다음 조건은 다른 편의 기능이나 자동화 정책보다 우선한다.

```text
사용자의 명시적 요청 없음
    -> 인벤토리가 가득 차도 home storage를 시작하지 않음
    -> trusted destination으로 이동하지 않음
    -> container를 열거나 아이템을 옮기지 않음

사용자의 명시적 요청 있음
    -> 하나의 canonical STORE_HOME 요청 생성
    -> 등록된 exact trusted destination만 사용
    -> 보유 loadout과 reserve를 제외한 아이템을 가능한 한 모두 저장
```

인벤토리 점유율, high-water threshold, idle 상태, 시간 경과 또는 trusted
registry 변경은 `STORE_HOME` 시작 권한이 아니다.

AI는 사용자를 대신해 저장 시작 시점을 결정하지 않는다. 요청을 받은 뒤
어느 trusted destination을 먼저 시도할지, 어느 stack을 보유할지, 상자가
가득 찼을 때 다음 후보로 이동할지는 고정된 정책에 따라 실행할 수 있다.

## 2. 기존 기능과의 의미 분리

세 명령 의미를 혼합하지 않는다.

```text
manual @deposit_all
    -> 기존 ChatClef 수동 전체 저장 의미

inventory-pressure automatic deposit
    -> 인벤토리 압박을 자동 감지해 slot relief를 시도하던 별도 방향

manual STORE_HOME
    -> 사용자 요청으로 집의 trusted storage에 inventory를 정리하는 새 의미
```

`STORE_HOME`은 기존 `@deposit_all`을 재정의하지 않는다. 기존 automatic
planner의 free-slot 목표, working-set recovery, valuable/general fallback
분류도 재사용하지 않는다.

이 기능의 canonical 명령 이름은 설계상 `@store_home`으로 둔다. 실제 명령
등록 전에는 구현되지 않은 이름이며, 명령 catalog 충돌을 확인해야 한다.

## 3. 입력 경계

모든 입력 방식은 동일한 request와 Task factory에 도달해야 한다.

```text
@store_home
    -> STORE_HOME request

chat: "지금 아이템 다 집에 가져다 놔"
    -> STORE_HOME intent
    -> STORE_HOME request

microphone: STORE_HOME intent
    -> STORE_HOME request
```

채팅과 마이크가 별도의 planner 또는 executor를 소유하면 안 된다. 가능하면
명령 문자열을 다시 만들어 재파싱하기보다 Fabric ChatClef orchestration이
같은 typed request 또는 같은 Task factory를 호출한다.

V1의 필수 입력은 명시적 `@store_home` 명령이다. 자연어 chat과 microphone
intent mapping은 같은 핵심 동작이 검증된 뒤 연결하는 후속 입력 adapter다.

## 4. UserTask 계약

`@store_home`은 사용자의 새 현재 의도다.

- 기존 `@get`, `@goto`, 제작 등의 현재 UserTask는 기존 명령 실행 경계에서
  정상적으로 교체한다.
- 기존 UserTask를 storage 완료 뒤 자동 재개하지 않는다.
- command 수락 시 cursor stack이 비어 있어야 한다. 비어 있지 않으면 item을
  임의 slot에 놓거나 drop하지 않고 inventory를 변경하지 않은 채
  `CURSOR_NOT_EMPTY`로 거절한다.
- food, defense, unstuck 등 더 높은 우선순위 safety chain은 기존 규칙대로
  `STORE_HOME`을 일시 선점할 수 있다. 이 일시 선점 자체는 terminal이 아니다.
- 같은 `StoreHomeTask`가 재개될 때 world, dimension, 현재 trusted registry, 열린
  screen과 inventory manifest를 다시 검증한다. 모두 유효하면 계속하고, 하나라도
  달라졌으면 `CONTEXT_CHANGED` 또는 `MANIFEST_STALE`로 부분 종료한다.
- 사용자의 새 명시적 UserTask가 `STORE_HOME`을 교체하면 `INTERRUPTED`로 종료하며
  다시 자동 재개하지 않는다.
- `STORE_HOME`이 `TaskRunner`, `UserTaskChain`, Baritone, 전역 input 또는 다른
  chain을 직접 정리하거나 중단하지 않는다.
- Task가 직접 획득한 input, cursor transaction 또는 operation-local 상태만
  cleanup할 수 있다.
- background thread, `Thread.sleep`, 독립 timer 또는 tick 밖 Minecraft state
  mutation을 만들지 않는다.

root `StoreHomeTask`가 operation ID, phase, immutable manifest, destination queue,
timeout, interruption과 terminal result를 소유한다. child task는 자신에게
위임된 navigation, open 또는 exact transfer 단계만 소유한다.

## 5. 저장 목표

이 기능은 필요한 몇 개의 slot만 확보하는 relief operation이 아니다.

```text
목표:
    manifest의 모든 STORE_HOME step을 가능한 한 실행

정상 종료:
    모든 STORE_HOME step 완료

부분 종료:
    모든 trusted destination 소진
    destination capacity 소진
    접근 가능한 destination 없음
    safety-chain 재개 시 context 또는 manifest 안전성 상실
    사용자의 새 UserTask에 의한 명시적 교체
```

목표 slot 수를 달성했다는 이유로 남은 manifest를 조기 종료하지 않는다.

## 6. V1 아이템 disposition

`STORE_HOME`에서는 귀중품과 일반품을 목적지별로 나누지 않는다. 다음
disposition만 사용한다.

```text
KEEP_LOADOUT
KEEP_RESERVE
KEEP_EXPLICIT
STORE_HOME
DEFER_UNSAFE
```

| 상태 | V1 disposition | trusted 저장 |
| --- | --- | --- |
| 현재 착용 중인 armor | `KEEP_LOADOUT` | 금지 |
| 현재 offhand | `KEEP_LOADOUT` | 금지 |
| 주력 곡괭이 1개 | `KEEP_LOADOUT` | 금지 |
| 주력 도끼 1개 | `KEEP_LOADOUT` | 금지 |
| 주력 삽 1개 | `KEEP_LOADOUT` | 금지 |
| 주력 검 또는 근접 무기 1개 | `KEEP_LOADOUT` | 금지 |
| 사용자가 명시적으로 pin한 stack | `KEEP_EXPLICIT` | 금지 |
| survival reserve에 필요한 stack | `KEEP_RESERVE` | 금지 |
| 여분 도구, 무기와 미착용 armor | `STORE_HOME` | 허용 |
| diamond, iron, gold, redstone, gunpowder, bone meal | `STORE_HOME` | 허용 |
| 일반 block, 광물, 제작 재료 | `STORE_HOME` | 허용 |
| reserve 초과 음식, 횃불과 화살 | `STORE_HOME` | 허용 |
| loadout이 아닌 이름/인챈트/trim/custom metadata stack | `STORE_HOME` | exact 검증 시 허용 |
| stale fingerprint, 지원하지 않는 slot mapping | `DEFER_UNSAFE` | V1 금지 |

`STORE_HOME`은 explicit request이므로 `filled_map`, `written_book`, 내용물이 있는
shulker box 같은 고유 stack도 원칙적으로 집에 보관할 수 있다. 단, exact source
slot과 전체 metadata fingerprint를 검증할 수 있을 때만 이동한다.

unknown 또는 modded item을 자동으로 버리거나 일반 상자로 보내지 않는다.
fingerprint와 slot mapping을 안전하게 만들 수 있으면 `STORE_HOME`, 그렇지 않으면
`DEFER_UNSAFE`로 유지한다.

`KEEP_EXPLICIT`은 기존 source에 이미 사용 가능한 pin 또는 명시적 protection signal이
있을 때만 적용한다. `STORE_HOME` V1을 위해 새 pin command, pin JSON 또는 별도 pin UX를
추가하지 않는다. 그런 signal이 없으면 이 disposition은 사용하지 않아도 된다.

## 7. 주력 도구 선택

도구는 재질마다 하나씩이 아니라 역할마다 하나를 유지한다.

```text
pickaxe:      1
axe:          1
shovel:       1
melee weapon: 1
```

검이 없으면 한 개의 주력 axe가 axe와 melee 역할을 함께 만족할 수 있다.
괭이는 기본 loadout 역할이 아니므로 별도 사용자 정책이 없으면 저장 대상이다.

주력 도구 comparator는 deterministic해야 하며 단일 임의 가중치보다 다음
lexicographic 순서를 우선 검토한다.

1. 실제로 해당 역할을 수행하는가
2. 내구도가 critical 상태가 아닌가
3. 역할 capability 또는 mining tier
4. 역할 관련 enchantment utility
5. 남은 내구도 비율
6. 남은 내구도 절대량
7. 현재 선택 중인가
8. logical inventory slot 순서

권장 critical 기준은 남은 내구도가 최대 내구도의 10% 미만인 경우다.
비-critical 후보가 있으면 critical 후보를 주력으로 선택하지 않는다.

Fortune과 Silk Touch처럼 목적이 다른 후보에 객관적인 우열이 없으면 현재
선택된 후보를 우선하고, 그래도 결정되지 않으면 낮은 logical slot을 사용한다.
사용자별 specialty preference는 V1 이후 별도 opt-in 정책으로 둔다.

## 8. V1 armor 정책

V1은 현재 착용 중인 armor를 그대로 `KEEP_LOADOUT`으로 유지한다.
미착용 armor는 exact 검증 후 `STORE_HOME`으로 분류한다.

`@store_home`은 V1에서 다음을 하지 않는다.

- 더 좋은 armor 자동 탐색과 교체
- elytra와 chestplate 자동 비교
- Curse of Binding 장비 교체
- protection enchantment의 상황별 최적화
- 장비 교체 실패 rollback

armor 최적화는 storage와 별도의 사용자 의도이므로, 필요하면 이후
`optimize loadout` 단계 또는 별도 명령으로 설계한다.

## 9. Survival reserve

V1 기본값은 별도 profile UX 없이 `SAFE`로 확정한다.

| 범주 | SAFE reserve | 조건 |
| --- | ---: | --- |
| 안전한 일반 음식 | 합계 16 | 유해하거나 특별 보존 대상인 음식은 제외하고, 적합한 한 종류에 집중 |
| 횃불 | 32 | 한 stack 우선 |
| 화살 | 32 | retained bow 또는 crossbow가 있을 때 |
| 물 양동이 | 1 | 이미 보유한 경우 |

`MINIMAL` profile은 armor, offhand와 주력 도구만 유지하고 위 reserve를 0으로
두는 후속 opt-in 정책이다. V1 구현을 위해 profile 선택 UI나 새 설정 체계를 만들지
않는다.

V1 exact transfer가 partial-stack split을 안전하게 보장하지 못하면 reserve가
포함된 stack 전체를 유지할 수 있다. 예를 들어 음식 64개 한 stack에 reserve
16이 필요하면 64개 stack 전체를 보유하는 결과를 허용한다.

## 10. Exact-slot manifest

같은 Item의 여러 stack 중 특정 stack만 남겨야 하므로 aggregate `ItemTarget`은
최종 source-selection 권한자가 될 수 없다.

```text
logical slot 5
    좋은 diamond_pickaxe
    -> KEEP_LOADOUT

logical slot 17
    낮은 내구도 diamond_pickaxe
    -> STORE_HOME
```

`ItemTarget(diamond_pickaxe x1)`은 어느 slot을 선택할지 보장하지 못한다.
보호 stack을 다른 slot으로 옮겨 놓는 것만으로도 충분하지 않다.

각 manifest step은 최소한 다음을 보유한다.

```text
logicalPlayerInventorySlot
stackFingerprint
expectedCount
transferMode
disposition
dispositionReason
loadoutPlanRevision
```

manifest에는 현재 `ScreenHandler`의 slot ID가 아니라 player inventory의 logical
slot을 저장한다. container가 열린 뒤 LAVI-owned slot resolver가 logical slot을
현재 handler slot ID로 변환한다.

fingerprint에는 최소한 다음 identity를 포함한다.

- canonical item ID
- damage
- 전체 component 또는 NBT의 canonical hash
- custom name과 lore
- enchantments
- armor trim
- custom attributes와 CustomModelData
- portable-container contents

count는 fingerprint와 분리한다. 전송 후 count가 감소해도 같은 stack identity의
진행으로 검증할 수 있어야 한다.

## 11. Exact-slot 전송 계약

operation 시작 전 cursor stack은 비어 있어야 한다. 실행 중 예기치 않게 cursor가
비어 있지 않게 되면 추가 click을 수행하지 않고 현재까지 검증된 결과만 보존한 채
`CURSOR_NOT_EMPTY` 또는 대응하는 context terminal로 종료한다.

각 click 직전에 다음을 모두 검증한다.

- 같은 world와 dimension
- 같은 `STORE_HOME` operation
- 열린 GUI가 현재 trusted destination과 정확히 결합됨
- logical slot이 현재 handler slot으로 정확히 변환됨
- 현재 stack fingerprint가 planned fingerprint와 일치
- 현재 count가 예상 범위와 일치
- 해당 step이 여전히 `STORE_HOME`
- cursor가 비어 있음

하나라도 실패하면 같은 Item의 다른 slot을 대신 검색해 이동하지 않는다.
해당 operation은 `MANIFEST_STALE`, `CONTEXT_CHANGED` 또는 대응하는 typed partial
result로 종료한다.

V1 transfer mode는 exact source slot의 whole-stack `QUICK_MOVE`를 우선한다.
container가 일부만 수용하면 source count 감소량을 확인하고 남은 수량을 다음
trusted destination에서 계속할 수 있다.

전송 성공은 다음 paired delta가 실제로 확인된 경우에만 인정한다.

```text
source slot count 감소 또는 empty
AND trusted container의 대응 stack delta 증가
AND cursor empty
```

click 요청, client animation, child Task 종료 또는 source 감소만으로 성공 처리하지
않는다. cursor 기반 정밀 stack 분할은 V1 범위에서 제외한다.

## 12. Trusted destination 정책

후보는 다음 조건을 모두 만족해야 한다.

```text
exact destination으로 명시적 등록
AND enabled
AND 같은 worldKey
AND 같은 dimension
AND operation-local blacklist에 없음
```

실행 순서는 다음과 같다.

1. operation 시작 시 후보 snapshot을 만들어 안정적인 시도 순서를 고정
2. 가까운 후보부터 선택
3. 이동 또는 전송 직전에 현재 repository에서 같은 destination ID가 여전히
   등록되고 enabled인지 재검증
4. operation 중 해제되거나 disabled된 후보는 사용하지 않고 제외
5. operation 중 새로 등록된 후보는 현재 snapshot에 합류시키지 않고 다음 명령에서 사용
6. 실제로 이동
7. exact destination과 결합된 container GUI open 검증
8. 실제 server GUI slot으로 capacity 검증
9. 전송 직전에도 registration/enabled 상태를 한 번 더 확인한 뒤 manifest step 실행
10. full, missing, unreachable 또는 timeout이면 현재 operation에서 제외
11. 다음 후보로 단방향 이동

후보 A를 제외한 뒤 B와 C를 시도하고 다시 A로 돌아가지 않는다. cache capacity와
접근성은 ordering hint일 뿐 성공 증거가 아니다.

모든 trusted destination이 소진되면 남은 아이템을 inventory에 유지한다.
다음 fallback은 금지한다.

- 가까운 일반 상자
- scanner 또는 cache가 발견한 미등록 상자
- 새 상자 제작과 배치
- 임시 container
- 다른 차원 portal 이동

manual home storage는 자동 압박 해소보다 먼 이동을 의도할 수 있으므로 기존
automatic trusted max distance를 재사용하지 않는다. V1은 같은 worldKey와 같은
dimension의 모든 활성 trusted destination을 후보로 두고 절대 거리 상한을 두지
않으며, 거리는 시도 순서에만 사용한다. 각 후보에는 기존 navigation/progress 경계와
동등한 bounded no-progress 또는 timeout 판정을 적용하고, 실패하면 다음 후보로 이동한다.
다른 dimension 이동과 portal 탐색은 계속 제외한다.

## 13. Trusted 등록 UX와 JSON

기존 exact destination JSON registry를 유지한다.

```text
schemaVersion
worldKey
dimension
x / y / z
enabled
```

capacity, path cost, retry, current operation, item policy 또는 trusted area radius는
destination JSON에 저장하지 않는다.

V1 loadout, SAFE reserve와 item disposition은 Java source의 고정 정책으로 구현하며
별도 item-policy JSON, profile JSON 또는 pin JSON을 추가하지 않는다. 따라서 이 기능이
계속 사용하는 JSON은 기존 exact trusted destination 좌표 registry뿐이다.

반경 8블록 등록은 storage operation과 분리된 batch snapshot UX다.

```text
anchor:
    사용자가 바라본 지원 container

scan:
    현재 존재하고 로드된 chest / trapped chest / barrel

result:
    여러 exact destination entry를 한 transaction으로 등록

future container:
    자동 trusted로 승격하지 않음
```

persistent trusted area는 V1 범위에 포함하지 않는다. 기존 단일 exact registration,
unregistration과 list 명령의 의미를 변경하지 않는다.

## 14. 재사용 경계

현재 LAVI-owned trusted implementation에서 다음 책임은 재사용 후보다.

- instance-owned exact destination repository
- world/dimension/BlockPos identity와 stable destination ID
- persistence 실패 시 mutation 거절
- trusted registration, unregistration과 list command
- candidate filtering, ordering과 operation-local blacklist
- exact-open container binding
- 실제 GUI capacity acceptance
- source와 destination paired delta tracking

현재 `AutoDepositTrustedStoreTask`의 destination queue, navigation, open과 GUI
validation 방향은 참고 또는 composition할 수 있다. 그러나 generic `ItemTarget`
source selection은 `STORE_HOME`의 최종 전송기로 재사용하지 않는다.

공유 upstream `StoreInContainerTask`, `DepositAllTask`, `Task`, `TaskRunner`,
`UserTaskChain`, `PlayerInteractionFixChain` 또는 Baritone 의미를 변경하지 않는다.

## 15. LAVI-owned 구성 방향

정확한 class 수와 package는 source audit 후 최소화하되 책임 경계는 다음과 같다.

```text
StoreHomeCommand
    -> canonical request를 한 번 생성

StoreHomeTask
    -> 전체 operation phase, interruption, timeout과 terminal 소유

HomeLoadoutPlanner
    -> logical slot별 KEEP / STORE_HOME 판정

HomeStorageManifest
    -> immutable exact-slot plan과 fingerprint 소유

HomeStorageExecutor
    -> 현재 trusted GUI에서 exact source slot 전송과 검증

HomeStorageScreenSlotResolver
    -> logical player slot을 current handler slot으로 변환
```

새 코드는 `adris/**`가 아닌 LAVI-owned namespace에 둔다. 새 Task가 기존
`Task` contract에 참여하기 위한 상속은 허용하지만 engine-wide lifecycle을
override하지 않는다. composition을 우선하며, 단순 DTO마다 불필요한 manager나
framework를 만들지 않는다.

## 16. 기존 inventory-pressure chain 처리

사용자의 explicit request-only 요구와 automatic trigger는 양립할 수 없다.

권장 상태는 다음과 같다.

```text
automatic source:
    현재 조사와 rollback을 위해 보존 가능

automatic entrypoint registration:
    disabled

end-client-tick pressure observation:
    disabled

automatic Task creation:
    explicit future enable 전에는 불가능

STORE_HOME dependency on automatic chain:
    none
```

planner가 빈 결과를 반환하도록 두는 것만으로는 충분하지 않다. 사용자의 요청이
없을 때 inventory snapshot, threshold observation과 automatic root creation 자체가
일어나지 않는 것을 test로 고정해야 한다.

기존 automatic source를 삭제할지는 수동 V1 구현과 분리된 후속 cleanup 결정이다.
기존 dirty worktree의 lifecycle, diagnostics와 test 변경을 새 manual feature와 한
diff에서 광범위하게 삭제하지 않는다.

## 17. Terminal result

최소 terminal 의미는 다음을 구분한다.

```text
COMPLETED
PARTIAL_TRUSTED_CAPACITY_EXHAUSTED
PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE
NO_USABLE_TRUSTED_DESTINATION
NO_TRUSTED_CAPACITY
CURSOR_NOT_EMPTY
MANIFEST_STALE
CONTEXT_CHANGED
INTERRUPTED
```

일부 저장 후 모든 상자가 가득 찬 경우의 사용자 메시지 예:

```text
집 정리 부분 완료: 14개 stack을 저장했고 3개 stack이 남았습니다.
등록된 trusted storage에 더 이상 빈 공간이 없습니다.
```

아무것도 저장하지 못한 경우 inventory를 변경하지 않았는지 함께 보고한다.
capacity 부족과 missing/unreachable은 같은 `FAILED`로 합치지 않는다.

## 18. Diagnostics

normal runtime에서는 경계와 terminal 중심의 bounded diagnostics만 사용한다.
매 tick 또는 매 slot unchanged-state logging은 금지한다.

최소 operation correlation 후보:

```text
operationId
gameTick
phase
topLevelTask
childTask
worldKey
dimension
trustedDestinationId
trustedPosition
manifestRevision
logicalSourceSlot
stackFingerprintSummary
expectedCount
sourceDelta
destinationDelta
cursorState
candidateAttempt
elapsedTicks
terminalReason
```

diagnostics는 source selection, click, retry, timeout, fallback, Task completion 또는
cleanup behavior를 변경하지 않는다.

## 19. V1 acceptance tests

최소 acceptance matrix는 다음과 같다.

| Scenario | Expected |
| --- | --- |
| inventory 36/36, 사용자 요청 없음 | storage Task와 trusted 이동 0 |
| `@store_home` 입력 | 정확히 한 manual UserTask 생성 |
| 기존 `@get` 중 `@store_home` | 기존 UserTask 정상 교체, 자동 재개 없음 |
| command 수락 시 cursor stack 존재 | inventory mutation 없이 `CURSOR_NOT_EMPTY` |
| safety chain 일시 선점 뒤 context 동일 | 같은 StoreHome root가 재검증 후 계속 |
| safety chain 일시 선점 뒤 manifest 변경 | 추가 전송 없이 typed partial terminal |
| 좋은/낮은 내구도 diamond pickaxe | 좋은 exact slot 유지, 낮은 exact slot만 저장 |
| 같은 Item의 enchanted/plain stack | manifest가 선택한 exact slot만 이동 |
| 계획 후 source fingerprint 변경 | 대체 stack을 찾지 않고 `MANIFEST_STALE` |
| 현재 armor와 미착용 armor | 현재 armor 유지, 미착용 exact slots 저장 |
| current offhand | 유지 |
| food 64 한 stack, SAFE reserve 16 | V1에서 stack 전체 유지 허용 |
| trusted A 일부 공간, B 충분 | A에서 검증된 만큼 저장 후 B 계속 |
| trusted A full 또는 missing | operation-local 제외 후 다음 후보 |
| 후보 snapshot 뒤 A를 untrust/disable | A를 열거나 전송하지 않고 제외 |
| operation 중 새 trusted 등록 | 현재 operation에는 합류하지 않고 다음 명령부터 사용 |
| trusted 모두 full, 일부 저장 | typed partial capacity terminal |
| trusted 없음 | inventory 불변, no-destination terminal |
| 가까운 일반 상자 존재 | 절대 사용하지 않음 |
| 다른 dimension에만 trusted 존재 | V1에서 사용하지 않음 |
| click 후 paired delta 불일치 | 성공 처리 금지 |
| post-V1 chat/microphone adapter | command와 같은 Task factory 및 policy |
| 기존 `@deposit_all`, `@deposit`, `@get` | 기존 behavior 유지 |

focused unit tests 후 build 검증이 승인되면
`chatclef-fabric-build-verification.md`의 clean forced build와 deployed JAR hash,
Minecraft runtime log 검증을 별도로 수행한다.

## 20. V1 범위 제외

- inventory-pressure automatic trigger
- high-water/low-water/hysteresis
- automatic working-set recovery
- 일반 상자 fallback
- 발견한 상자의 자동 trusted 승격
- persistent trusted area
- cross-dimension home travel와 portal 탐색
- 새 trusted 상자 제작과 배치
- best armor 자동 교체
- elytra와 chestplate 자동 선택
- 범용 enchantment 가치 점수화
- cursor 기반 정밀 partial-stack split
- 새 pin command, pin JSON 또는 별도 pin UX
- nearby batch registration UX
- chat와 microphone intent adapter
- home storage retrieval
- generic `StoreInContainerTask` source-selection 변경
- shared Task, TaskRunner, UserTaskChain 또는 Baritone 변경
- Forge/MineMind 구현, placeholder, config 또는 test

## 21. V1 확정 기본값

1회 방향성 검수에서 다음 기본값을 확정한다.

1. survival reserve: profile UX 없이 `SAFE`
2. manual home destination 거리: 같은 worldKey와 같은 dimension의 모든 enabled
   destination, 절대 거리 상한 없음, 거리순 시도와 후보별 bounded progress failure
3. V1 armor: 현재 착용 armor를 그대로 유지하고 미착용 armor만 저장

이 세 항목 외에 behavior 구현 전에 추가 사용자 정책 결정을 요구하지 않는다.
정확한 timeout tick 수, class 수와 package 이름은 source audit과 focused test에서
최소화할 구현 매개변수이며 방향성 재검수 gate가 아니다. explicit request-only,
exact trusted-only, no general fallback과 exact-slot source selection은 확정된 핵심
방향으로 취급한다.

## 22. 구현 slice와 승인 경계

V1 core는 다음 순서로 제한한다.

```text
H0  documentation and source audit
H1  automatic entrypoint disable contract and no-request tests
H2  StoreHome request/command and loadout/manifest unit tests
H3  exact logical-slot resolver and transfer executor
H4  sequential trusted destination integration and terminal results
H7  clean build, JAR deployment and Minecraft runtime reproduction
```

다음은 V1 core가 runtime에서 검증된 뒤의 별도 후속 범위다.

```text
H5  nearby batch registration UX
H6  chat and microphone intent adapters
```

각 slice는 별도 사용자 승인을 요구한다. 2026-08-27 현재 요청은 H1-H4 source와
focused test source를 함께 승인했지만, build, runtime reproduction, commit 또는
push를 승인하지 않았다. H5와 H6는 H4/H7의 완료 조건에 포함하지 않는다.

## 23. 현재 구현 상태

2026-08-27 기준 현재 상태는 다음과 같다.

```text
exact trusted destination repository:            PRESENT IN DIRTY WORKTREE
trust / untrust / trusted_list commands:          PRESENT; RUNTIME COMMANDS OBSERVED
sequential manual trusted candidate execution:    SOURCE IMPLEMENTED; RUNTIME NOT VERIFIED
@store_home command:                              SOURCE IMPLEMENTED; RUNTIME NOT VERIFIED
manual StoreHomeTask:                             SOURCE IMPLEMENTED; RUNTIME NOT VERIFIED
exact-slot manifest and executor:                 SOURCE IMPLEMENTED; RUNTIME NOT VERIFIED
nearby batch registration:                        NOT IMPLEMENTED
chat/microphone STORE_HOME adapters:              NOT IMPLEMENTED
automatic entrypoint disabled for manual policy:  SOURCE IMPLEMENTED; BUILD NOT VERIFIED
focused H1-H4 unit tests:                         SOURCE ADDED; NOT EXECUTED
clean forced Gradle build and deployed JAR:       NOT RUN
Minecraft runtime STORE_HOME reproduction:        NOT RUN
```

기존 runtime 관측은 trusted registration과 list command의 존재만 확인했다.
이번 source 구현 뒤 `@store_home` command registration, exact-slot transfer,
candidate fallback과 terminal result는 아직 build 또는 Minecraft runtime에서
확인하지 않았다.

## 24. 관련 문서

- [Minecraft Backend Separation](minecraft-backend-separation.md)
- [ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)
- [ChatClef @deposit_all Ocean Loop Diagnostics Plan](chatclef-deposit-all-ocean-loop-diagnostics-plan-2026-08-26.md)
- [Fabric ChatClef 1.20.1 Build Verification](chatclef-fabric-build-verification.md)

이 문서는 `STORE_HOME`의 activation, item disposition과 destination policy에 대해
기존 automatic-deposit 문서보다 우선한다. 기존 문서의 runtime evidence,
`@deposit_all` 진단과 trusted registry 구현 기록은 역사적·기술적 근거로 유지한다.
