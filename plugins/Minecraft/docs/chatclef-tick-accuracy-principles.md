<!-- 20260731_kpopmodder: ChatClef / Carry On 화로 handoff 조사 뒤 tick 기반 정확도 원칙을 기록했다. -->
<!-- 20260903_openai: Separated pre-GUI Carry On pickup risk from the exact post-GUI three-boundary slot-input gate. -->
<!-- 20260903_kpopmodder: Set the GUI gate workflow to implementation, bounded-log reinforcement, and verification without an approval pause. -->
<!-- 20260903_openai: Added the exact interaction-consumption, open-child quiescence, full-path suppression, explicit serial, and one-time permission rules. -->
<!-- 20260903_kpopmodder: Distinguished a raw inter-tick TAIL observation from an accepted candidate with a proven serial K. -->

# ChatClef Tick 정확도 원칙

## 한 줄 요약

Minecraft AI 자동화는 속도보다 정확도가 우선이다. 각 Task는 이전 Task의 결과가 실제 Minecraft 상태에 반영된 것을 확인한 뒤 다음 Task로 넘어가야 하며, 기다림에는 반드시 bounded tick timeout을 둬서 무한루프와 무한대기를 막아야 한다.

## 핵심 원칙

ChatClef / AltoClef 자동화는 빠른 자동 클릭기가 아니라 tick 기반 state machine이어야 한다.

Minecraft는 정상 20 TPS 기준으로 1 tick이 약 50 ms다. 사람 눈에는 거의 동시에 일어난 것처럼 보여도, 실제로는 다음 상태들이 서로 다른 tick 경계에서 반영될 수 있다.

```text
Task onStop cleanup
input press / release
playerSneaking 상태
world block state
server -> client GUI open packet
currentScreenHandler 변경
Baritone pathing state
Carry On carrying state
```

따라서 AI는 다음 흐름을 목표로 해야 한다.

```text
이전 Task cleanup 확인
-> 게임 상태 안정화 확인
-> 다음 interaction 시도
-> 기대한 상태 변화 확인
-> terminal success 또는 bounded failure
```

반대로 다음 흐름은 위험하다.

```text
이전 Task가 끝났다고 판단
-> 즉시 다음 target 클릭
-> click result=SUCCESS를 실제 성공으로 간주
```

## 왜 비동기 멀티스레드가 아니라 main tick인가

Minecraft client 상태 대부분은 client main thread / client tick 흐름에서 안전하게 읽고 변경해야 한다.

```text
player 위치와 sneaking 상태
world block state
inventory / slot state
currentScreenHandler
input pressed / released 상태
server packet 처리 결과
Baritone pathing state
Carry On carrying state
```

이런 상태를 별도 background thread에서 동시에 읽거나 바꾸면 다음 문제가 더 쉽게 생긴다.

```text
아직 갱신되지 않은 상태를 읽음
반쯤 갱신된 상태를 읽음
서버 GUI open packet 처리 전 상태를 실패로 오해함
input release / press 순서가 꼬임
ScreenHandler가 null이거나 이전 값인 상태를 읽음
희귀 race, NPE, ConcurrentModificationException 발생
```

비동기로 빼도 되는 것은 Minecraft live state를 직접 건드리지 않는 작업이다.

```text
로그 파일 분석
외부 API 요청
명령 파싱
순수 계산
진단 결과 후처리
```

Minecraft 상태를 읽거나 바꾸는 핵심 자동화는 main tick 안에서 천천히, 정확하게 처리하는 편이 안전하다.

## Task 경계 원칙

어떤 Task가 input, path, goal, cleanup을 소유했다면, 다음 Task는 이전 Task가 끝났다는 이유만으로 그 소유 상태가 이미 정리됐다고 가정하면 안 된다.

민감한 경계에서는 다음 순서를 지킨다.

1. 이전 child Task가 기존 Task lifecycle을 통해 stop / cleanup 할 기회를 준다.
2. 로그로 증명된 경우, cleanup이 반영될 tick 경계를 보장한다.
3. cleanup 전 상태가 아니라 cleanup 후 게임 상태를 확인한다.
4. 추가 안정화가 필요하면 작은 bounded tick budget을 사용한다.
5. 사용자가 직접 키를 누르거나 다른 Task/mod가 상태를 잡고 있을 수 있으므로 무한히 기다리지 않는다.
6. budget 초과 시 terminal diagnostic을 1회 남기고, owning Task의 기존 lifecycle로 빠져나간다.

하지 말아야 할 것:

```text
전역 input 강제 release
전체 Baritone path / goal cancel
TaskRunner 전체 stop
무제한 retry
무제한 timeout
관측 실패를 success로 변환
```

## 화로 / Carry On 사례: 서로 다른 두 경계

화로 interaction에서는 최소 두 종류의 timing boundary를 분리해야 한다. 이름에 모두 “3 tick”이
들어간다는 이유로 같은 대기라고 취급하면 안 된다. 숫자가 같다고 계약까지 같은 것은 아니다.

### 1. GUI가 열리기 전: world click과 Carry On pickup 경계

가능한 실패 흐름:

```text
PlaceBlockNearbyTask가 화로 설치
-> stopPlacing / onStop에서 local SNEAK release 요청
-> 다음 container owner가 normal world right-click 요청
-> local playerSneaking 또는 Carry On server-side keyPressed가 아직 stale true
-> Carry On이 world click을 GUI open이 아니라 block pickup으로 소비
-> FurnaceScreenHandler가 열리지 않음
-> GUI_BOUND가 성립하지 않음
```

`interactBlock result=SUCCESS`나 `tryPressAccepted=true`는 GUI가 열렸다는 뜻이 아니다. 또한 local
SNEAK이 false라는 사실만으로 Carry On server-side key state까지 false라고 증명할 수 없다.

현재 `DoStuffInContainerTask`의 post-place SNEAK 안정화는 bounded pre-GUI handoff다. 현재 구현은
최대 wait budget이 소진되면 경고를 남기고 진행할 수 있으므로, 이것을 deterministic Carry On
pickup 방지책으로 과장하지 않는다.

상자·화로 pickup을 결정적으로 금지하려면 Carry On `forbiddenTiles` 또는 dedicated Carry key처럼
activation을 분리하는 별도 정책이 필요하다. 이 post-open GUI gate 작업은 그 설정 변경을
암묵적으로 포함하지 않으며 별도 기능 범위로 유지한다.

### 2. GUI가 열린 뒤: exact binding과 slot mutation 경계

이 계약은 명시적인 구현 요청이 있으면 다음 순서로 연속 수행한다.

```text
정확한 route-owned gate 구현
-> bounded state-change/boundary 로그 보강
-> focused test와 clean forced build 검증
-> exact runtime target이 현재 범위에 있으면 bounded runtime 검증
```

단계 사이에 별도 사용자 승인 대기 지점을 두지 않는다. Runtime target이 없거나 외부 실행이
현재 요청 범위 밖이면 가능한 저장소 내부 검증까지 완료하고 runtime은 `NOT_RUN`으로 기록한다.

새 3-tick gate는 최초 world pickup을 막는 장치가 아니다. strict `GUI_BOUND`가 성립한 뒤에만
시작하는 post-open settlement contract다.

2026-09-03 현재 working-tree 구현과 repository-local test/clean build는 존재하고 통과했지만,
matching-JAR regular-furnace runtime에서는 GUI가 열린 뒤 ordinary diagnostic ceiling 전까지
accepted TAIL candidate, `GUI_BOUND`, permission, transfer-commit 기록이 없었다. Ceiling 이후의
exact-gate detail은 관측되지 않았으므로 이 capture만으로 전체 실행의 transfer-commit 횟수를
단정할 수 없고, 사용자 관측상 철 제련은 진행되지 않았다. 직접 확인된 최초 실패 경계는 아직
없으며 source→hub→gate BOUNDARY 진단이 필요한 상태다. 현재 증거와 다음 진단 계약은
[implementation ledger Section 18](chatclef-exact-container-gui-three-tick-implementation-ledger-2026-09-03.md#18-post-build-furnace-runtime-reproduction-and-diagnostic-reinforcement-plan)과
[Task Lifecycle Diagnostics](chatclef-task-lifecycle-diagnostics.md#screen-tail-source-to-hub-to-coordinator-diagnostic-contract)를 따른다.

```text
normal world right-click
-> WORLD_OPEN_REQUESTED 뒤, TAIL 전에 exact target의 matching BlockInteractEvent 1개를 관찰
-> raw ScreenOpenEvent TAIL source observation
-> active serial과 exact correlation을 통과해 accepted TAIL candidate가 된 경우에만
   candidate snapshot이 그 event를 exactly once consume하고 serial K를 소유
-> route parent가 open child를 quiescent 상태로 만들고 operation-owned normal cleanup 완료
-> target/world/dimension/screen/HandledScreen handler/syncId/operation-attempt 검증
-> GUI_BOUND
-> TAIL candidate tick K와 GUI_BOUND promotion tick B는 모두 count하지 않음
-> 이후 distinct client-tick boundary #1에서 동일 binding 확인
-> 이후 distinct client-tick boundary #2에서 동일 binding 확인
-> 이후 distinct client-tick boundary #3에서 동일 binding 확인
-> GUI_INPUT_ALLOWED
-> owning route의 다음 정상 lifecycle 평가에서 기존 slot action 허용
```

수락된 candidate 처리 경로만 immutable candidate snapshot을 capture한다. Open child를 callback에서
강제로 정지시키거나 cleanup을 대신 수행하지 않는다. Parent가 기존 lifecycle로 child를
정지·교체하고 같은 attempt가 다시 world click에 도달할 수 없음을 증명한 뒤에만
`GUI_BOUND`를 확정한다. 그 사이 screen/handler/world/operation identity가 바뀌면 candidate와
consumed-event association을 폐기하되, 이미 consume된 event를 pending으로 되돌리거나 같은
attempt의 다른 TAIL candidate가 재사용하게 하지 않는다. Retry는 새 attempt/correlation과 새
matching event가 필요하다. 기존 stop/cleanup이 global 또는 operation이 소유하지 않은 input,
screen, goal/path state를 변경한다면 operation-owned cleanup으로 인정하지 않는다. 먼저 좁은
composition 경계를 사용하고, source가 불가능함을 증명할 때만 canonical 문서의 최소 generic seam을
사용한다.

초기 exact allowlist와 type mapping은 다음으로 고정한다.

```text
minecraft:chest, minecraft:trapped_chest
    exact screen: GenericContainerScreen
    exact handler: GenericContainerScreenHandler

minecraft:furnace
    exact screen: FurnaceScreen
    exact handler: FurnaceScreenHandler

unchanged/out of scope
    barrel, all shulker boxes, smoker, blast furnace, hopper, dispenser,
    dropper, ender chest, entity inventory, crafting/brewing/modded/unlisted routes
```

`GUI_STABILIZING` 동안 전체 reachable path는 gameplay mutation을 하지 않는다. Gate는
operation-local validation/counter/serial/deduplication/invalidation/permission bookkeeping만
갱신할 수 있다. 이 금지는 observer에만
적용되는 것이 아니라 parent, current/former open child, normal cleanup, `super.onTick()`, fallback,
helper와 callback을 포함한 전체 reachable path에 적용한다. `clickSlot`, cursor mutation,
slot-action registration, screen close/reopen, world `interactBlock`, input press/release 또는 Baritone
state 변경에 도달하기 전에 route owner가 구조적으로 short-circuit해야 한다. `GUI_BOUND` 뒤
permission consume 전에는 그런 mutation을 수행할 수 있는 child tick 또는 stop도 호출하지 않는다.

기존 route의 input contract는 그대로 둔다.

```text
trusted-home route:
    QUICK_MOVE, button 0 유지

storage/furnace route:
    실제 call site의 기존 PICKUP/button 계약 유지

금지:
    모든 route를 물리적 Shift+right-click으로 변환
    GUI가 열린 뒤 같은 block에 interactBlock 재호출
```

GUI가 열리지 않으면 `GUI_BOUND`도, stabilization counter도, permission도 생기지 않는다. 따라서
post-open gate를 initial Carry On pickup 해결책으로 보고하면 잘못된 결론이다.

Raw TAIL source observation 자체는 아직 `SCREEN_TAIL_CANDIDATE`가 아니다. Source 또는 hub에서
`activeClientTickSerialPresent=false`인 `BETWEEN_TICKS` 관측은
`candidateClientTickSerial=-1`로만 기록하며 tick K를 부여하지 않는다. 현재 diagnostics-only
단계에서는 그 event를 보류, 재생, 재전달하거나 이전/다음 serial로 backfill하지 않는다. Tick K는
명시적인 current-tick serial 소유권 아래 hub 전달과 coordinator 검증을 통과해 candidate로 수락된
경우에만 정의된다. Inter-tick TAIL을 향후 어떻게 보존할지는 source→hub→gate 직접 증거 뒤의
exact behavior hunk에서 정하며, 그 전에는 permission을 fail-closed로 유지한다.

## GUI-open 이후 세 client-tick 경계 계산

“3 tick 대기”는 wall-clock 150 ms나 단순 task 호출 횟수가 아니다. 선택한 client-tick boundary의
**서로 다른 세 번의 후속 관찰**이다.

개념적인 순서는 다음과 같다.

```text
accepted ScreenOpenEvent TAIL candidate with proven serial K:
    immutable candidate snapshot과 candidateClientTickSerial만 capture
    matching BlockInteractEvent를 exactly once consume
    GUI_BOUND/permission은 아직 없음

route-owned promotion during tick B after open-child quiescence/cleanup:
    full predicate를 통과하면 GUI_BOUND
    stableLaterBoundaries = 0
    boundClientTickSerial = GUI_BOUND가 성립한 tick B의 chosen-boundary serial
    lastCountedBoundarySerial = boundClientTickSerial

same binding tick B의 boundary:
    binding 검증만 수행
    stableLaterBoundaries = 0

first distinct later boundary:
    full binding 유지 확인
    stableLaterBoundaries = 1

second distinct later boundary:
    full binding 유지 확인
    stableLaterBoundaries = 2

third distinct later boundary:
    full binding 유지 확인
    stableLaterBoundaries = 3
    GUI_INPUT_ALLOWED로 전이 가능

owning route의 subsequent normal evaluation:
    full live predicate와 route-specific transfer precondition을 다시 검증
    permission consume과 기존 transfer lifecycle 진입을 하나의 logical commit으로 수행
    lifecycle 진입을 만들 수 없으면 consume하지 않고 typed invalidation/terminal로 종료
    성공한 뒤에만 기존 screen-local slot action 발행 가능
```

`boundClientTickSerial`은 binding 수명 동안 immutable이다. 관찰 serial `S`가
`S <= boundClientTickSerial` 또는 `S <= lastCountedBoundarySerial`이면 검증만 하고 count하지
않는다. `S > lastCountedBoundarySerial`일 때도 full binding이 유지된 경우에만
`lastCountedBoundarySerial=S`와 counter 증가를 함께 commit한다.

실제 구현은 `current-tick serial publish -> accepted setScreen TAIL candidate K -> route Task promotion -> 같은 serial의
chosen END boundary observation -> slot mutation` 호출 순서를 current source로 확인해야 한다.
END에서만 증가하는
counter를 쓴다면 마지막 completed END serial을 TAIL이나 Task HEAD가 실행 중인 현재 tick identity로
오인하지 않는다. TAIL이 발생한 tick과 첫 countable later boundary를 구분하지 못하면 early slot
permission을 추측하지 않는다. 명시적인 serial owner와 가장 좁은 bounded ordering log를 구현하고,
permission은 fail-closed로 유지한 채 같은 workflow에서 검증한다.

Raw TAIL이 active serial 밖에서 관측되면 위 순서의 candidate K 단계에 진입한 것이 아니다. 이때
이전 completed serial이나 예상 next serial을 candidate K로 대신 사용하지 않는다.

다음은 count 근거가 아니다.

```text
currentGameTick - T_open >= 3 식 하나만 사용
System.currentTimeMillis / monotonic wall-clock 경과
render frame 수
server tick 수
Task.onTick 호출 횟수
ScreenOpenEvent 호출 횟수
같은 boundary callback의 중복 진입
```

각 boundary마다 다음 identity가 모두 같아야 한다.

```text
screen object
HandledScreen.getScreenHandler() object
player.currentScreenHandler object
captured/live syncId
world object와 dimension
target position과 explicit target family
owning operation, openAttemptId, correlationId
```

하나라도 바뀌면 counter와 permission을 즉시 폐기한다. retry는 새 attempt와 새 correlation로 0부터
시작한다. 이전 GUI의 tick을 새 GUI에 상속하지 않는다.

세 번째 boundary는 permission 전이만 할 수 있다. gate observer가 그 callback 안에서 slot mutation까지
수행하면 observation owner와 mutation owner가 합쳐지고, tick 순서 검증도 흐려진다. 실제 click은
기존 parent/transfer owner의 다음 정상 평가가 소유한다.

이 permission은 일반 boolean latch가 아니라 operation/openAttempt/binding identity에 묶인 one-time
권한이다. 다음 정상 Task 평가는 target/world/dimension/screen object/handler object/syncId/exact
screen-handler class/open-child quiescence와 route precondition을 전부 다시 확인한다. 하나라도 다르면
권한을 폐기한다. 모두 맞을 때도 permission consume과 기존 transfer lifecycle 진입을 하나의
logical commit으로 수행한다. 진입을 만들 수 없으면 consume하지 않고 typed invalidation/terminal로
끝낸다. 이미
consume된 권한을 같은 tick의 재진입이나 다음 평가가 다시 사용할 수 없다.

## Fixed Delay와 Timeout은 다른 것

새 post-open 3-boundary gate는 이미 열린 exact GUI에 적용하는 fixed minimum delay다.
GUI open timeout, retry budget, post-place SNEAK wait budget과 서로 대체하지 않는다.

```text
GUI open timeout:
    world open request 뒤 GUI_BOUND가 끝내 생기지 않는 경우의 bounded failure

post-open fixed delay:
    GUI_BOUND 뒤 exact binding을 세 later boundaries 동안 유지해야 하는 최소 조건

retry budget:
    실패 뒤 새 open attempt를 시작할 수 있는 횟수/정책
```

어느 timeout도 성공이 아니다. timeout 때문에 unsafe world click, early slot click, global input
release, forced screen close 또는 Baritone cancellation을 허용하지 않는다.

## Timeout 원칙

정확도를 위해 기다림을 추가할 때는 반드시 명시적인 제한이 있어야 한다.

위험한 형태:

```text
while playerSneaking:
    return null
```

안전한 방향:

```text
POST_PLACE_HANDOFF
-> WAIT_FOR_STABILITY for at most N ticks
-> stable이면 PROCEED
-> budget 초과면 TIMEOUT
```

timeout은 성공이 아니다.

timeout은 현재 상태를 보존하고, 한 번만 terminal diagnostic을 남긴 뒤, 가장 작은 owning Task 경계로 제어를 돌려야 한다.

## 진단 로그 원칙

진단은 timing boundary를 증명해야지, timing boundary 자체를 교란하면 안 된다.

기본 로그는 다음 정도만 남긴다.

```text
operation start
실제 state transition
first failure
terminal success 또는 terminal failure
```

GUI gate의 bounded boundary/state-change record는
[Task Lifecycle Diagnostics](chatclef-task-lifecycle-diagnostics.md#canonical-exact-gui-gate-log-fields)의
canonical 이름을 그대로 사용하며 최소한 다음 값을 포함한다.

```text
operationId, openAttemptId, correlationId
matchingBlockInteractEventObserved, matchingBlockInteractEventCount, matchingBlockInteractEventConsumed
duplicateBlockInteractEventRejected
openChildIdentity, openChildQuiescent, openChildCleanupComplete
targetBlockId, screenTypeExpected, screenTypeActual, screenTypeMatched
handlerTypeExpected, handlerTypeActual, handlerTypeMatched
screenObjectIdentity, handledScreenHandlerIdentity, playerHandlerIdentity
capturedSyncId, liveSyncId, worldIdentity, dimension
candidateClientTickSerial, boundClientTickSerial, clientTickBoundarySerial, lastCountedBoundarySerial
candidateTickExcluded, boundPromotionTickExcluded, sameBoundaryDuplicateSuppressed
stableLaterBoundaries, fullGuiBoundPredicate, guiInputAllowed, invalidationReason
slotMutationSuppressed, reachableMutationPath, reachableMutationKind, suppressedMutationOwner
permissionAvailable, permissionFullRevalidationPassed, permissionConsumed, permissionReuseRejected
transferLifecycleEntryCommitted, slotActionOwner, slotActionType, slotButton, terminalReason
```

Unchanged state를 매 tick 출력하지 않는다. Bind, count, duplicate suppression,
invalidation, permission publication/consumption과 slot-action boundary에서만 bounded하게 남긴다.

고빈도 진단은 기본 OFF 또는 강한 rate limit이 필요하다.

```text
매 tick PRE/POST snapshot
매 Task tick log
매 block update log
모든 input 요청의 caller stack
heartbeat log
정상 흐름 WARN log
```

이번 문제처럼 tick 경계 race를 조사할 때는 로그 자체가 tick 처리 시간과 입력 적용 시점을 바꿀 수 있다. 따라서 진단은 bounded, lazy, opt-in이어야 한다.

## 새 Minecraft AI Task 체크리스트

Task를 만들거나 수정하기 전에 다음을 확인한다.

```text
무엇이 실제 성공 상태인가?
click result가 아니라 어떤 Minecraft state를 확인해야 하는가?
각 input은 어느 Task가 소유하는가?
timeout은 어느 Task가 소유하는가?
retry count와 terminal reason은 누가 소유하는가?
다음 interaction 전에 어떤 cleanup이 먼저 실행돼야 하는가?
기다리는 상태가 사용자, 다른 Task, 다른 mod에 의해 계속 유지될 수 있는가?
bounded tick budget은 몇 tick인가?
budget 초과 시 무엇을 기록하고 어디로 빠져나가는가?
Carry On 같은 optional mod가 없거나 incompatible이면 어떻게 동작하는가?
diagnostics OFF 상태에서도 비용이 발생하는가?
ScreenOpenEvent TAIL을 GUI_BOUND로 과장하지 않았는가?
TAIL candidate tick K 또는 GUI_BOUND promotion tick B를 stabilization boundary로 잘못 세지 않는가?
같은 boundary를 중복 count하지 않는가?
full screen/handler/syncId/world/operation binding은 누가 소유하는가?
GUI_STABILIZING 동안 slot mutation이 구조적으로 불가능한가?
parent/current-or-former child/cleanup/fallback 전체 reachable path가 mutation-free인가?
matching BlockInteractEvent가 request와 TAIL 사이 exact target에서 한 번만 consume되는가?
open child와 normal cleanup이 GUI_BOUND 전에 quiescent인가?
chest/trapped chest와 furnace의 exact screen/handler class를 검사하는가?
boundClientTickSerial과 lastCountedBoundarySerial이 stale/duplicate boundary를 막는가?
permission 전이와 실제 slot click의 owner가 분리되어 있는가?
다음 정상 Task 평가가 full predicate를 재검증하고 one-time permission을 한 번만 consume하는가?
retry가 새 attempt/correlation과 zero count로 시작하는가?
fixed 3-boundary delay와 GUI-open timeout/retry budget을 구분했는가?
initial stale Carry On key pickup 한계를 별도 실패로 보고하는가?
Barrel과 unlisted container에 정책을 자동 확대하지 않는가?
```

기본 결론:

```text
빠르게 추측해서 진행하지 말고,
조금 느리더라도 게임 상태가 확정된 것을 확인한 뒤 진행한다.
```
