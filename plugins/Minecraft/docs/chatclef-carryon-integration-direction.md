<!-- 20260730_kpopmodder: Added this document to preserve the ChatClef/Carry On integration direction before code changes. -->
<!-- 20260903_openai: Documented the exact GUI binding and three-later-client-tick input gate. -->
<!-- 20260903_kpopmodder: Replaced per-step stop gates with a continuous implementation, bounded-log reinforcement, and verification workflow. -->
<!-- 20260903_kpopmodder: Clarified the post-build furnace failure and separated raw inter-tick TAIL observations from accepted candidate tick K. -->

# ChatClef / Carry On Integration Direction

이 문서는 Minecraft 플러그인에서 ChatClef/AltoClef와 Carry On 연동을 다룰 때의 복원 기준선, 설계 방향, 진단 순서를 정리한다.
현재 목적은 ChatClef를 LAVI 전용 엔진으로 개조하는 것이 아니라, ChatClef 0.18.23 기준 동작을 가능한 한 보존하면서 필요한 Carry On 연동을 얇고 소유권이 명확한 layer로 붙이는 것이다.

이 문서 자체를 읽는 것만으로 새 작업이 시작되지는 않는다. 그러나 사용자가 구현을 직접
요청하면 같은 요청 범위 안의 Java/config/test 구현, bounded diagnostic logging 보강,
focused test와 required clean build를 별도 단계 정지 없이 연속 수행한다.

문서 수정만 요청된 작업은 문서만 수정한다. 구현 요청은 dependency/version 변경, 외부
프로그램 설치, 관리자·전역 설정 변경, 파괴적 정리, 외부 instance 배포, commit 또는 push를
자동으로 포함하지 않는다. 이 항목들은 단계별 허가 문제가 아니라 현재 요청의 범위를 벗어나는
별도 작업이다.

## Authority And Scope

이 문서는 AGENTS.md Section 0의 Minecraft ChatClef scoped override를 구체화한다.

다음 범위에서는 AGENTS.md Section 0의 scoped safety rules와 이 문서가 Section 29의 일반 강제 리팩토링, 폴더화, 상속 검토 규칙보다 우선한다.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
ChatClef / AltoClef lifecycle
Baritone interaction
Baritone input / goal / path ownership
Carry On optional integration
해당 범위의 diagnostics 및 root-cause investigation
```

이 우선순위는 upstream class의 분해, 이동, rename, repackage, hierarchy 변경, 대규모 lifecycle override를 허가하지 않는다.
충돌이 있을 때는 더 보수적이고 더 안전한 규칙을 적용한다.

이 override는 upstream-derived code의 기준선과 구조를 보존하기 위한 것이다. 새 LAVI-owned
GUI gate code에는 독립된 책임이 둘 이상이면 focused type으로 분리하고 의미 있는 package에
배치하는 일반 책임 분리 규칙을 계속 적용한다. 이 규칙을 upstream-derived class의 이동,
분해 또는 repackage 근거로 사용하지 않는다.

## Core Principle

ChatClef/AltoClef는 third-party automation engine으로 취급한다.
LAVI Minecraft plugin은 그 엔진을 호출하고 조율하는 외부 orchestration layer로 둔다.
Carry On 연동은 항상 설치되어 있다고 가정하지 않는 optional capability layer로 설계한다.

권장 구조는 다음과 같다.

```text
ChatClef / AltoClef        existing automation engine
LAVI Minecraft plugin      orchestration / command layer
Carry On integration       optional adapter or task-local layer
Diagnostics                passive observer for failure boundaries
Root-cause fix             minimum hunk after evidence proves the boundary
```

## Composition First

ChatClef 핵심 lifecycle을 상속으로 덮어쓰는 방향은 기본적으로 피한다.

피해야 하는 방향:

- `AltoClef`, `TaskRunner`, global chain을 상속하거나 재작성한다.
- `InteractWithBlockTask.isFinished()`를 Carry On 전용 완료 조건으로 전역 변경한다.
- `PlayerInteractionFixChain`에 Carry On 전용 retry, cooldown, fallback을 전역 삽입한다.
- Baritone pathing 전체나 TaskRunner 전체를 중단한다.
- 모든 우클릭, SNEAK, JUMP, 이동 입력을 강제로 해제한다.
- package 이동, class rename, upstream-derived class 분해로 기준선 비교를 어렵게 만든다.

우선 검토할 방향:

- 작은 adapter
- optional integration wrapper
- task-local helper
- passive diagnostic observer
- Carry On 상태 조회 boundary
- 소유권이 명확한 Carry On 전용 parent Task

새 Task가 기존 ChatClef Task abstraction을 상속해야 한다면, 그것은 upstream framework의 established Task contract에 참여하기 위한 최소한의 상속이어야 한다.
그 상속은 engine-wide lifecycle ownership을 대체하거나 확장하는 근거가 될 수 없다.

## Task Ownership

각 Task는 자신이 획득하거나 생성한 상태만 정리해야 한다.

원칙:

- input을 획득한 Task가 자기 input만 해제한다.
- retry를 시작한 Task가 retry 횟수와 stop reason을 소유한다.
- custom goal 또는 path를 만든 Task가 자기 goal/path만 취소한다.
- parent Task가 완료 조건을 소유한다면 child Task의 전역 `isFinished()`를 변경하지 않는다.
- interruption과 cleanup은 idempotent해야 하며, 여러 번 호출되어도 다른 Task 상태를 훼손하지 않아야 한다.
- operation state를 static field나 global singleton에 저장하지 않는다.

Carry On 전용 parent Task를 새로 구현하려면 먼저 현재 ChatClef Task contract를 읽고 다음 호출 관계를 확인한다.

- `onStart`
- `onTick`
- `onStop`
- `isFinished`
- `isEqual`
- `toDebugString`
- child Task 반환과 교체 과정
- parent Task interruption 과정

Task equality가 잘못되면 새 Task가 기존 Task로 잘못 취급되거나, 반대로 매 tick 새 Task로 인식될 수 있다.
이 문제를 현재 원인으로 단정하지 말고, 로그와 현재 구현으로 먼저 확인한다.

## Explicit Carry On Capability States

Carry On capability 결과는 최소한 다음 상태로 구분한다.

```text
ABSENT
AVAILABLE_NOT_CARRYING
AVAILABLE_CARRYING
INCOMPATIBLE
STATE_UNREADABLE
OBSERVATION_FAILED
```

`ABSENT`:

- Carry On-specific operation을 시작하지 않는다.
- 일반 ChatClef class loading과 기존 generic interaction은 유지한다.
- optional capability 부재를 정상적인 상태로 취급한다.

`AVAILABLE_NOT_CARRYING`:

- Carry On을 사용할 수 있고 현재 carrying 상태가 아님을 의미한다.

`AVAILABLE_CARRYING`:

- Carry On을 사용할 수 있고 현재 carrying 상태임을 의미한다.

`INCOMPATIBLE`:

- 설치는 확인되었지만 현재 bridge가 안전하게 지원하지 못한다.
- 성공 처리하지 않는다.
- blind retry하지 않는다.
- generic right-click retry로 자동 fallback하지 않는다.

`STATE_UNREADABLE`:

- carrying state를 확인할 수 없다.
- 성공 처리하지 않는다.
- blind retry하지 않는다.
- generic right-click retry로 자동 fallback하지 않는다.

`OBSERVATION_FAILED`:

- 상태 관찰 도중 예외 또는 명시적 실패가 발생했다.
- 원인과 예외를 진단 정보로 보존한다.
- 성공 처리하지 않는다.
- 현재 Carry On-specific operation만 typed failure로 종료한다.

Carry On 미설치 시 기존 ChatClef 전체를 실패시키는 것과, Carry On-specific operation 실행 중 상태 조회 실패를 generic click으로 우회하는 것은 서로 다른 문제다.
전자는 optional dependency 부재를 안전하게 다루는 문제이고, 후자는 실패한 typed operation을 증거 없이 성공 또는 fallback으로 변환하는 문제다.

## Carry On Success Criteria

Carry On 성공은 클릭 시도나 click result가 아니라 실제 Carry On 상태 전환으로만 판단한다.

```text
Pickup success:
NOT_CARRYING -> CARRYING

Placement success:
CARRYING -> NOT_CARRYING
```

다음 상태는 성공으로 처리하지 않는다.

- Carry On 미설치
- Carry On version 확인 불가
- Carry On state unreadable
- Carry On API exception
- 우클릭 시도만 확인되고 상태 전환이 없음
- 상태를 관찰하지 않는 blind retry

Carry On이 없거나 상태를 읽을 수 없으면 기존 ChatClef 동작으로 fallback하거나, 실패 이유를 로그로 남기고 상위 Task로 제어권을 반환해야 한다.
단, Carry On-specific operation이 이미 시작된 뒤의 상태 조회 실패를 generic right-click retry로 자동 우회하지 않는다.

## Container GUI-First Three-Tick Input Contract

이 section은 2026-09-03 사용자 결정인 container GUI binding과 post-open three-client-tick
input gate를 구현 전에 고정했으며, 현재 구현·검증 상태는 아래에서 별도로 기록한다. 여기서
“3 tick 뒤 Shift 우클릭”은 구현 용어가 아니다.
정확한 계약은 **이미 열린 exact GUI에 대한 route-owned screen-local slot action을 허용하는
시점**이다. 물리적인 `SNEAK` 키 입력, right mouse button 합성 또는 같은 block에 대한 두 번째
`interactBlock` 호출을 새로 도입하지 않는다.

```text
CONTRACT_STATUS: DIRECTION_FIXED
IMPLEMENTATION_SEQUENCE:
    IMPLEMENT
    -> REINFORCE_BOUNDED_LOGS
    -> VERIFY
PER_PHASE_PAUSE_GATE: REMOVED
DOCUMENT_CONTRACT_SCOPE: GUI_EXACT_BIND_AND_THREE_LATER_BOUNDARIES
```

Current evidence status on 2026-09-03:

```text
WORKTREE_IMPLEMENTATION: PRESENT
REPOSITORY_TESTS_AND_CLEAN_BUILD: PASSED
MATCHING_JAR_REGULAR_FURNACE_RUNTIME: FAILED_WITH_NO_ACCEPTED_TAIL_CANDIDATE_RECORD_BEFORE_ORDINARY_CEILING
CURRENT_RUNTIME_STATUS: FAILED_REPRODUCED_FIRST_FAILING_BOUNDARY_DIRECT_OBSERVATION_PENDING
CURRENT_CAUSE_CLASSIFICATION: LEADING_SOURCE_AND_LOG_HYPOTHESIS
NEXT_IN_SCOPE_EVIDENCE: BOUNDED_SOURCE_TO_HUB_TO_GATE_DIAGNOSTICS
```

This status does not weaken the fixed behavior contract. The live GUI was
observed independently, while no candidate, `GUI_BOUND`, permission, or
transfer-commit record was admitted before the ordinary diagnostic ceiling.
Post-ceiling exact-gate detail is unobserved, so the capture alone does not
prove the whole-run transfer-commit count; the user-observed iron smelting did
not proceed. The canonical incident evidence and next diagnostic work are recorded in
[implementation ledger Section 18](chatclef-exact-container-gui-three-tick-implementation-ledger-2026-09-03.md#18-post-build-furnace-runtime-reproduction-and-diagnostic-reinforcement-plan)
and the
[source-to-hub-to-gate diagnostic contract](chatclef-task-lifecycle-diagnostics.md#screen-tail-source-to-hub-to-coordinator-diagnostic-contract).

### Non-Negotiable Operation Order

확정된 순서는 다음과 같다.

```text
IDLE
-> OPEN_ATTEMPT_CREATED
-> WORLD_OPEN_REQUESTED
-> one ChatClef-owned normal world right-click for that exact attempt
-> exactly one matching BLOCK_INTERACT_OBSERVED for that attempt
-> raw ScreenOpenEvent TAIL source observation
-> SCREEN_TAIL_CANDIDATE snapshot only after active-serial and correlation acceptance
-> route parent makes the open child quiescent
-> normal operation-owned open-child cleanup completes
-> full predicate satisfied: GUI_BOUND
-> GUI_STABILIZING(stableLaterBoundaries=0)
-> later client-tick boundary #1, same binding
-> later client-tick boundary #2, same binding
-> later client-tick boundary #3, same binding
-> GUI_INPUT_ALLOWED
-> owning route's next normal evaluation revalidates and consumes permission once
-> owning route may issue its existing screen-local slot action
-> SUCCESS or typed terminal failure
```

An accepted `ScreenOpenEvent` TAIL may establish only
`SCREEN_TAIL_CANDIDATE`; it does not itself establish `GUI_BOUND`. A raw TAIL
observation may be rejected before candidate creation. The open callback is
observation-only and may not issue a slot action.

More precisely, a raw TAIL source observation is not yet a candidate and need
not have a tick `K`. If source or hub observes it with
`activeClientTickSerialPresent=false`, it remains a `BETWEEN_TICKS`
diagnostic observation with `candidateClientTickSerial=-1`; the current
diagnostics-only stage must not retain, replay, forward, or backfill it. Tick
`K` exists only after the event is delivered under proven current-tick serial
ownership and the coordinator accepts it as the attempt's candidate. Any
inter-tick behavior correction requires direct source→hub→gate evidence and an
exact ownership-preserving hunk; until then the gate remains fail-closed.

Only the accepted-candidate path captures an immutable candidate snapshot,
including its `candidateClientTickSerial`. A candidate must not become
`GUI_BOUND` until the route parent proves that the open child can no longer tick, repeat the world
interaction, close or replace the screen, or perform delayed input/path work. Any normal cleanup for
that open child must complete before `GUI_BOUND`; cleanup must not be deferred into
`GUI_STABILIZING`. This handoff permits only cleanup already owned by the operation and does not permit
global or unowned input, screen, goal or path cleanup. If the candidate binding changes while waiting
for this handoff, discard the candidate instead of binding it.

`one ... normal world right-click` is an operation-local target contract, not a claim about the
current generic `InteractWithBlockTask` implementation and does not place that global class inside the
modification scope.
The current generic task can request additional clicks while a parent keeps returning it. Each storage
and processing route must therefore identify the parent/retry owner that can issue one open attempt,
observe its result, and decide whether a new attempt is allowed.

A retry is a new attempt. It must receive a new `openAttemptId` and a new `correlationId`, reset
`stableLaterBoundaries` to zero, and discard every permission or snapshot from the previous attempt.
A diagnostic correlation value must not be inserted into Task `isEqual()` merely to force replacement.

### World Input And GUI Input Are Different Domains

```text
World open request:
    target block interaction
    normal right-click / interactBlock
    no ChatClef-owned SNEAK for the in-scope chest/furnace route

GUI-local action after permission:
    clickSlot or the route's existing screen-handler action
    exact bound handler and syncId
    no second world interaction
```

For the in-scope chest/furnace open operation, ChatClef-owned world
`SNEAK + CLICK_RIGHT` is forbidden for the entire open attempt, including retries. This does not ban a
user's manual Carry On input globally and does not permit global release of a SNEAK state that the
operation did not acquire.

The gate preserves the existing slot contract of each route:

```text
manual trusted-home transfer:
    preserve QUICK_MOVE and button 0

existing storage / furnace processing transfer:
    preserve the current PICKUP / button contract at each actual call site

all routes:
    do not convert the transfer into physical Shift+right-click
    do not replace QUICK_MOVE with PICKUP or PICKUP with QUICK_MOVE merely to share the gate
```

During `GUI_STABILIZING`, every route participant is mutation-blocked, including the parent Task,
current or former child Task, observer, cleanup path and fallback path. None may call `clickSlot`,
register a slot action, move the cursor stack, close, reopen or replace the screen, call
`interactBlock`, press or release input, or create, replace or cancel a Baritone goal/path. A child must
not be ticked or stopped during stabilization when that lifecycle call can perform one of those
mutations. The observer may only read and compare the required binding identities and advance its own
diagnostic/gate bookkeeping.

### Critical Limit: This Gate Does Not Prevent The First Carry On Pickup

This is a post-open settlement gate. It starts only after `GUI_BOUND`.

If Carry On server-side `keyPressed` remains stale `true`, the first client-side normal world click may
still be consumed as a Carry On pickup even when local SNEAK observation is false. In that case:

```text
container GUI does not open
-> no valid ScreenOpenEvent TAIL candidate for the requested target
-> GUI_BOUND is not established
-> GUI_STABILIZING does not start
-> no three-tick permission can be inherited or fabricated
```

Therefore this gate is not a deterministic first-pickup prevention mechanism. Deterministically
excluding chest/furnace pickup requires a separate Carry On policy such as exact `forbiddenTiles`
configuration or a dedicated Carry activation key. This post-open GUI task does not silently include a
Carry On configuration, key binding, dependency, or compatibility-policy change.

The existing pre-GUI post-placement SNEAK wait in `DoStuffInContainerTask` is also a separate contract.
It has a bounded wait and currently can proceed when that budget is exhausted. It must not be described
as equivalent to deterministic Carry On pickup prevention or to this fixed post-GUI three-boundary gate.

### Exact Initial Route, Target And GUI Mapping

The first implementation is limited to the following exact mappings:

```text
manual trusted-home and in-scope general storage:
    target blocks:
        minecraft:chest
        minecraft:trapped_chest
    exact screen class:
        GenericContainerScreen
    exact handler class:
        GenericContainerScreenHandler

regular furnace processing:
    target block:
        minecraft:furnace
    exact screen class:
        FurnaceScreen
    exact handler class:
        FurnaceScreenHandler

explicitly unchanged:
    minecraft:barrel
    minecraft:shulker_box and every colored shulker box
    minecraft:smoker
    minecraft:blast_furnace
    every unlisted container, screen and handler
```

Broad `HandledScreen`, `ContainerType`, shared container lists or analogous inventory shapes must not
expand this mapping. A screen or handler that is broadly compatible but is not the exact route mapping
is rejected.

### Exact GUI_BOUND Predicate

`GUI_BOUND` must not be inferred from `interactBlock` return value, `tryPressAccepted=true`, elapsed
time, one screen class name, one handler class name, or a changed syncId alone.

The current `ScreenOpenEvent` payload contains only `screen` and `preOpen`. The active route-owned attempt
must supply the target, world, dimension, operation and attempt identity. A TAIL event with
`preOpen=false` can become `GUI_BOUND` only when all of the following are true in the same active attempt:

```text
active owning operation still exists
active openAttemptId and correlationId still match the route's current attempt
the attempt observed exactly one matching BlockInteractEvent after WORLD_OPEN_REQUESTED
the observed BlockInteractEvent precedes this TAIL candidate and matches target, world and dimension
the observed BlockInteractEvent has not been consumed by an earlier candidate
this TAIL candidate consumes that interaction observation exactly once
world click target matches the attempt's immutable target position
world object identity matches the attempt snapshot
dimension matches the attempt snapshot
target block still belongs to the route's explicitly enumerated in-scope target set
event.screen != null
MinecraftClient.currentScreen == event.screen
event.screen instanceof HandledScreen<?>
event.screen class matches the exact screen class mapped for this route
HandledScreen.getScreenHandler() object == player.currentScreenHandler object
captured handler object is the live player handler object
captured handler syncId == live handler syncId
handler class matches the exact handler class mapped for this route
TAIL candidate falls within the bounded open-wait window owned by the route
route-owned open child is quiescent and cannot tick or repeat the interaction
normal operation-owned open-child cleanup completed before GUI_BOUND
```

Once the candidate consumes the matching interaction observation, that observation is permanently
spent for the lifetime of the attempt. Rejecting the candidate during child quiescence, cleanup or
binding promotion must not return the observation to pending state or let a later TAIL candidate reuse
it. A retry requires a new `openAttemptId`, correlation identity and matching `BlockInteractEvent`.
If the existing child stop/cleanup path would release global or otherwise unowned input, screen, goal
or path state, that path is not operation-owned cleanup and cannot satisfy this predicate. Keep the
handoff in a narrow composed owner, or record a `SOURCE_PROVEN_CONTRACT_GAP` and use only the minimal
behavior-preserving generic seam permitted by this document.

The implementation ledger must restate the exact mapping above. “Chest/furnace” is a scope label, not
permission to include every `ContainerType`, every `GenericContainerScreenHandler`, Barrel, shulker box,
hopper, dispenser, crafting table, brewing stand, or another block by analogy. Every unlisted route is
outside this scoped implementation.

Chat, inventory, pause, unrelated container screens, crosshair-inferred targets, a handler changed for
another operation, or a GUI opened after the active attempt was invalidated must be rejected.

### Three Distinct Later Client-Tick Boundaries

Neither the TAIL-candidate tick `K` nor the later route-owned `GUI_BOUND` promotion tick `B` is one of
the three stabilization ticks. `B` may equal `K` or follow it, but no boundary between them is counted
retroactively. A raw
expression such as `currentGameTick - T_open >= 3` is not sufficient by itself because it can hide an
off-by-one error, duplicate callback, callback-order dependency, or a TAIL event observed after the
counter owner already advanced for that tick.

The implementation must define and prove a monotonic client-tick-boundary serial that advances exactly
once per chosen client tick boundary. The serial model must expose the identity of the client tick in
which `GUI_BOUND` is established, not merely the most recently completed END callback.

Each bound attempt owns these immutable or monotonic values:

```text
boundClientTickSerial:
    serial of the client tick in which GUI_BOUND is established
    immutable for that binding

lastCountedBoundarySerial:
    initialized to boundClientTickSerial
    updated only after one distinct later boundary is accepted

count rule for observed boundary serial S:
    S <= boundClientTickSerial          -> validate only, do not count
    S <= lastCountedBoundarySerial      -> duplicate/stale, do not count
    S >  lastCountedBoundarySerial      -> validate once, increment by exactly one, then store S
```

Missing serial values are not backfilled from a numeric difference. The implementation must prove the
registration order that lets the TAIL candidate, later route-owned promotion and the selected
client-tick callback identify their exact tick serials. The implementation ledger must prove
`current-tick serial publication -> route-owned promotion -> chosen boundary observation of that same
serial`. An existing counter that advances only at END must not be reused blindly as the current tick
identity at TAIL or Task HEAD. Conceptually:

```text
accepted TAIL candidate is captured with proven serial K:
    candidateClientTickSerial = serial for tick K
    matching BlockInteractEvent becomes permanently spent for this attempt
    GUI_BOUND is not yet established

route-owned promotion establishes GUI_BOUND during tick B after child quiescence/cleanup:
    boundClientTickSerial = serial for tick B
    stableLaterBoundaries = 0

boundary belonging to the same tick B:
    validate binding only
    stableLaterBoundaries remains 0

first distinct later boundary:
    validate full binding
    stableLaterBoundaries = 1

second distinct later boundary:
    validate full binding
    stableLaterBoundaries = 2

third distinct later boundary:
    validate full binding
    stableLaterBoundaries = 3
    permission state may transition to GUI_INPUT_ALLOWED

subsequent normal evaluation by the owning route:
    revalidates the complete live binding and route-specific transfer preconditions
    atomically commits permission consumption together with entry into the existing transfer lifecycle
    if lifecycle entry cannot be established, does not consume and returns a typed invalidation/terminal reason
```

The event/tick observer itself remains gameplay-mutation-free even on the third boundary. It may update
only operation-local validation, serial, deduplication, counter, invalidation and permission bookkeeping.
The actual slot action
must remain owned by the existing route lifecycle after it observes `GUI_INPUT_ALLOWED`.

`GUI_INPUT_ALLOWED` is a one-time transition permission tied to the exact operation, attempt, target,
screen object, handler object and syncId. On the next normal Task evaluation, the route must revalidate
the complete live `GUI_BOUND` predicate and every route-specific transfer precondition before consuming
it. Failed revalidation clears the permission without a slot action. Permission consumption and entry
into that route's existing transfer lifecycle are one logical commit. If the transition cannot be
created, leave the permission unconsumed, fail closed with a typed invalidation or terminal reason, and
perform no slot action. Successful consumption does not become a reusable token for another attempt or
binding and does not change the route's existing QUICK_MOVE/PICKUP sequence.

Every count must correspond to a distinct later boundary. Re-entering the observer on the same boundary,
reading the same tick number twice, wall-clock delay, render frames, server ticks, task calls, packet
callbacks, or screen event count must not increment the stabilization counter.

The implementation ledger and bounded ordering logs must show the actual registration and invocation order between:

```text
MinecraftClient.setScreen TAIL publication
chosen client tick boundary callback
binding gate observation
storage or processing parent Task evaluation
slot mutation call site
```

If the same-tick boundary cannot be distinguished reliably in the current lifecycle, keep permission
fail-closed, implement or reinforce the narrowest explicit boundary serial and bounded ordering logs,
and continue available verification. Do not compensate by guessing an extra delay or by allowing an
early slot click.

### Invalidation, Failure And Retry

The gate fails closed. It must immediately clear permission, counter and captured binding when any of the
following occurs:

```text
screen closes, including setScreen(null)
screen object changes
HandledScreen handler object changes
player.currentScreenHandler object changes
captured or live syncId changes
world object changes
dimension changes
target position changes
target block leaves the explicitly enumerated in-scope route family
owning operation changes, stops, finishes or is interrupted
openAttemptId or correlationId changes
open-wait timeout occurs before GUI_BOUND
stability validation fails before permission
```

Suggested diagnostic/terminal concepts, not prescribed Java enum or class names:

```text
GUI_OPEN_TIMEOUT
SCREEN_TAIL_CANDIDATE_REJECTED
SCREEN_CLOSED_BEFORE_STABLE
GUI_STABILITY_INVALIDATED
OPERATION_INTERRUPTED
```

A timeout or invalidation never authorizes another world click, an early slot click, global input
cleanup, forced screen close, Baritone cancellation, or success. If the existing parent policy permits a
retry, that parent must start a new attempt and correlation from zero.

### Pre-Implementation Source Evidence And Non-Owners

At the pre-implementation checkpoint, the tree provided reusable observation
seams, but no class had yet been proven to own the complete defined gate. This
subsection preserves that historical containment analysis; current implementation
and runtime status are recorded above and in the implementation ledger.

```text
ClientOpenScreenMixin
    published ScreenOpenEvent at MinecraftClient.setScreen HEAD and TAIL

ScreenOpenEvent
    carried only screen and preOpen
    did not carry target, world, dimension, handler, syncId, operation or attempt identity

ClientInteractWithBlockMixin / BlockInteractEvent
    exposed an interactBlock boundary and BlockHitResult
    did not identify the owning operation or open attempt
```

`AutoDepositOpenContainerBindingTracker` was a useful algorithm/data precedent, not a ready common gate:

```text
it correlates one pending BlockInteractEvent with ScreenOpenEvent TAIL
it snapshots world, dimension, target, screen and player handler object
it checks current screen, current player handler and broad ContainerType compatibility

it does not own an operation-local openAttemptId or retry identity
it does not verify HandledScreen.getScreenHandler() object identity
it does not capture and compare a syncId field explicitly
it does not count three later stable client-tick boundaries
its target policy delegates to StoreInContainerTask.CONTAINER_BLOCKS
that policy includes chest, trapped chest, Barrel and shulker boxes
that policy excludes furnace processing blocks
```

Because of that target mismatch, moving this tracker unchanged into a chest/furnace common authority
would both expand the gate to out-of-scope Barrel/shulker routes and omit furnace routes. Reuse must be
limited to proven concepts or a proven route-local collaborator with an explicit target
policy. H5 bulk trust registration must not acquire this tracker or gate; H5 owns loaded-block scan and
repository mutation only.

`CarryOnContainerExpectedGui.expectedGuiOpened` is a broad diagnostics classifier. It can accept a visible
screen or a changed handler/syncId and includes many target kinds. It is not the strict `GUI_BOUND`
predicate and must not become behavior authority without replacing its broad assumptions.

`ContainerSubTracker` also has broad screen classification and target fallback behavior. It may support
legacy tracking but is not proof that the current operation opened the exact requested target.

The implementation ledger inspected these route seams separately:

```text
manual trusted-home storage:
    StoreHomeCandidateAttempt
    StoreHomeCandidateNavigationStep.tick
    HomeStorageContainerActivationGate
    HomeStorageQuickMoveIssuer

other storage-container flow:
    AbstractDoToStorageContainerTask.onTick
    concrete StoreInContainerTask parent/child callers and retry owner

furnace processing flow:
    DoStuffInContainerTask.onStart/onTick/onStop
    concrete SmeltInFurnaceTask.DoSmeltInFurnaceTask
    smoker/blast-furnace routes were not included
```

The manual trusted-home route already has a LAVI-owned candidate attempt and exact-activation boundary,
but that does not prove it is the owner for every storage workflow. `AbstractDoToStorageContainerTask`
and `DoStuffInContainerTask` are upstream-derived lifecycle owners that currently enter their container
subtask as soon as their broad open predicate succeeds. Any behavior hunk in those files is an engine
divergence. The evidence, minimal-hunk, record, rollback, and regression
requirements of the last-resort gate remain mandatory; for the exact GUI
continuous workflow they are a non-blocking implementation record rather than
a phase-approval stop.

`InteractWithBlockTask(BlockPos)` currently requests a normal, non-shift click, has
`isFinished() == false`, and can be returned repeatedly by its parent. Its global lifecycle, completion,
input cleanup, equality or retry behavior must not be changed merely to implement this route-local gate.
`PlayerInteractionFixChain`, `TaskRunner`, global input handling and global Baritone handling are not
first-change candidates.

### Mandatory Non-Blocking Implementation Ledger

Before editing Java, configuration or test source, Codex must capture the following implementation
ledger. The ledger preserves scope, ownership and rollback evidence; it is a nonblocking safety record
and must not pause an active implementation request.

1. **Baseline and worktree preservation**
   - repository HEAD or archive identity used for the audit
   - exact three documentation files already changed
   - all unrelated modified/untracked paths that must remain untouched

2. **Exact route and target inventory**
   - every command/root Task that can reach the intended storage path
   - every command/root Task that can reach the intended furnace processing path
   - exact block IDs and exact handler classes included for each route
   - explicit proof that Barrel and every unlisted container remain unchanged

3. **Owner and retry map per route**
   - operation owner
   - openAttemptId/correlation issuer
   - world-click owner
   - open-wait timeout owner
   - retry owner and retry budget
   - GUI binding/stability owner
   - slot mutation owner
   - interruption and cleanup owner

4. **Event and tick ordering proof**
   - `setScreen` TAIL event publication point and payload limits
   - selected client-tick boundary and monotonic serial source
   - how TAIL-candidate tick K and GUI_BOUND-promotion tick B are identified and both excluded
   - order relative to route Task evaluation and slot mutation

5. **Exact files and minimum implementation hunks**
   - repository-relative path, class, method and current line range
   - whether each file is LAVI-owned or upstream-derived
   - exact responsibility added or changed
   - exact behavior intentionally preserved
   - why a narrower existing owner cannot contain each implementation hunk
   - independently revertible rollback unit

6. **Failure and retry behavior**
   - candidate rejection, open timeout, close-before-stable, binding invalidation and interruption
   - permission/counter disposal on every failure
   - new-attempt identity and zeroed counter on retry
   - no global input/path/screen cleanup

7. **Slot contract inventory**
   - every affected `clickSlot` or equivalent call site
   - current button and `SlotActionType`
   - proof that no mutation can execute in `GUI_STABILIZING`
   - proof that QUICK_MOVE/PICKUP/button semantics remain unchanged after permission

8. **Regression and verification plan**
   - exact unit/integration test files to add or modify
   - scenarios listed below
   - exact verification commands, prerequisites and expected results
   - executed results, or an exact `NOT_RUN` reason when an external runtime is not in scope

When the active request explicitly asks for implementation, completing this ledger is followed
immediately by:

1. the smallest ownership-preserving implementation
2. bounded state-change and boundary logging reinforcement
3. unit/integration verification and the required clean forced build
4. bounded runtime validation and log inspection when the exact runtime target is available and in scope

Do not pause between these steps merely to restate the scope. If an uncertain engine behavior cannot
be changed safely, keep that behavior fail-closed, add the narrowest bounded observation, continue all
available verification, and report the remaining gap. Stop only when continuing would require a
material scope expansion, destructive action, dependency/version change, external installation,
commit, push or deployment outside the active request.

### Minimum Regression Matrix For Implementation

The implementation ledger must map each scenario to an exact test owner:

```text
ScreenOpenEvent HEAD alone never binds
TAIL with null or unrelated screen is rejected
TAIL with non-HandledScreen is rejected
TAIL without exactly one matching, unconsumed BlockInteractEvent is rejected
one BlockInteractEvent cannot bind two TAIL candidates
rejected candidate leaves its consumed BlockInteractEvent permanently spent for that attempt
screen object mismatch is rejected
wrong exact screen class is rejected even when it is a HandledScreen
HandledScreen handler object != player handler is rejected
wrong exact handler class is rejected even when it is broadly container-compatible
captured/live syncId mismatch is rejected
world, dimension, target or operation mismatch is rejected
unsupported/out-of-scope target is rejected
open child becomes quiescent and completes only operation-owned cleanup before GUI_BOUND
candidate mutation during the open-child handoff prevents GUI_BOUND
TAIL-candidate tick K and GUI_BOUND-promotion tick B are not counted
three distinct later boundaries are required
same boundary observed twice is not double-counted
no clickSlot or slot registration occurs at stable counts 0, 1 or 2
counts 0, 1 and 2 execute no cursor, screen, interactBlock, input or Baritone mutation
parent, current/former child, cleanup and fallback paths are mutation-blocked during stabilization
no mutation-capable child tick or stop occurs after GUI_BOUND and before permission consumption
third boundary changes permission only; observer makes no slot or other gameplay mutation
next normal Task evaluation revalidates the full binding and route transfer preconditions
permission consumption and transfer-lifecycle entry commit together; failed entry performs neither
permission is attempt/binding-scoped, consumed once and cannot transfer to a retry
screen close/replacement clears permission and count
handler/syncId/world/dimension/operation change clears permission and count
interruption clears permission and count
retry receives a new attempt/correlation and inherits zero ticks
trusted-home QUICK_MOVE button 0 remains unchanged
storage/furnace PICKUP and button contracts remain unchanged at actual call sites
normal world open uses no ChatClef-owned SNEAK for the in-scope route
Carry On absent keeps generic behavior loadable
initial stale Carry On key pickup produces no GUI_BOUND and no stabilization state
Barrel, every shulker box, smoker, blast furnace and every unlisted route remain unchanged
generic doors, trapdoors, beds, buttons, levers, block placement and item use remain unchanged
Task replacement, Baritone pathing and unowned input are not globally modified
```

A compile-only result cannot prove this contract. The verification stage must report binding identity,
distinct boundary serials, suppressed mutations, final permission and terminal reason without claiming
that the post-open gate solved the separate initial stale-key pickup problem. If live runtime validation
is outside the active request, record it as `NOT_RUN` rather than pausing the workflow.

## State Transition Attribution

Pickup과 placement의 상태 전환은 성공 판정의 필수 조건이지만, 그 자체만으로 target-level 성공을 완전히 증명하지 못할 수 있다.

상태 전환은 가능한 경우 같은 operation과 target에 귀속되어야 한다.
최소 correlation 대상:

- correlationId
- operationType
- player identity
- target dimension
- target position
- target type
- click tick
- transition tick
- carried object identity when available

Carry On API가 boolean carrying state만 제공하고 target identity를 제공하지 않는다면 그 제한을 명시한다.
확인하지 못한 target identity를 확인된 사실처럼 보고하지 않는다.

상태 전환 관찰은 bounded observation window 안에서 수행되어야 한다.
정확한 tick 한도는 현재 단계에서 임의로 정하지 않고, 실제 로그와 재현 결과로 결정한다.

## Input, Goal And Path Ownership

input ownership은 현재 boolean input state만으로 판단하지 않는다.

operation이 input을 변경하기 전에 최소한 다음을 추적해야 한다.

- previous input state
- operation이 실제로 input을 획득하거나 변경했는지
- owner identity, token, handle 또는 Task identity
- exact release or restore path

다음은 소유권 증거가 아니다.

- 현재 SNEAK이 true임
- 현재 CLICK_RIGHT가 true임
- 현재 custom goal이 존재함
- 현재 Baritone이 pathing 중임

소유권이 증명되지 않으면 global input release를 하지 않는다.

custom goal과 path도 다음 중 확인 가능한 ownership evidence가 있을 때만 취소할 수 있다.

- object identity
- owner handle
- 생성 Task
- operation identity
- 기존 API가 제공하는 ownership mechanism

기존 API가 ownership을 제공하지 않는다면 diagnostics 단계에서 그 제한을 보고하고, 전역 cleanup을 구현하지 않는다.

## Optional Dependency Class-Loading Boundary

generic ChatClef 또는 LAVI class에서 Carry On type을 다음 위치에 노출하지 않는다.

- field declaration
- method parameter
- method return type
- generic signature
- annotation
- superclass
- implemented interface
- static initializer
- class literal
- exception type

mod presence와 version은 가능한 경우 기존 mod-loader metadata boundary로 확인한다.
version-specific 상태 접근에 reflection이 필요하면 하나의 좁은 optional bridge 안에 격리한다.

optional bridge는 다음을 지켜야 한다.

- game state를 변경하지 않고 capability를 확인
- reflected class, method, parameter, return type 검증
- 명시적인 capability result 반환
- lookup 또는 invocation failure를 success로 변환하지 않음
- 예외와 실패 이유를 진단 정보로 보존
- detection을 위해 side-effect method를 호출하지 않음
- 매 tick 전체 classpath 또는 reflection discovery를 반복하지 않음

reflection resolution의 lifecycle과 cache 여부는 실제 mod lifecycle을 확인한 뒤 결정한다.
static global cache를 무조건 사용하지 않는다.

## Correlation Identity Versus Task Equality

correlationId는 diagnostic logging identity다.
Task `isEqual()`은 semantic operation identity다.

새 correlationId를 무조건 `isEqual()`에 포함하지 않는다.
correlationId 때문에 매 tick 새 Task로 인식되면 안 된다.
`isEqual()` 구현 전에 현재 TaskRunner가 equality를 Task 유지, 교체, interruption에 어떻게 사용하는지 실제 코드와 로그로 확인한다.

Carry On Task의 semantic equality 후보는 실제 contract 확인 후 검토하되, 다음 요소를 고려할 수 있다.

- PICKUP 또는 PLACEMENT operation type
- dimension
- target position
- target type
- expected carry state

위 요소를 현재 원인이나 확정 구현으로 단정하지 않는다.

## PlayerInteractionFixChain Risk

`PlayerInteractionFixChain`은 특정 Carry On 작업뿐 아니라 일반 상호작용 전체에 영향을 줄 수 있다.
따라서 이 위치에 Carry On 전용 처리를 넣는 것은 기본적으로 위험 변경으로 분류한다.

변경 전 확인해야 하는 기존 기능:

- 상자 및 container 열기
- 문과 trapdoor
- 침대
- 버튼과 레버
- 블록 배치
- 아이템 사용
- 일반 우클릭 이동
- Baritone pathing
- Task 완료
- interruption과 cleanup

이 chain이 실제 실패 boundary라는 증거가 없으면 behavior fix를 넣지 않는다.

## Thread And Tick Context

Minecraft client state, world state, player state, Baritone state, input override state, Carry On state는 기존 Minecraft client tick 또는 현재 Task lifecycle이 실행되는 안전한 context에서만 읽거나 변경한다.

진단이나 retry를 위해 다음을 새로 만들지 않는다.

- background thread
- `Timer`
- `ScheduledExecutorService`
- busy loop
- polling thread
- `Thread.sleep` 기반 retry
- tick과 동기화되지 않은 비동기 상태 변경

현재 실행 thread가 안전한지 확인되지 않으면 behavior를 바꾸지 말고 thread name, game tick, caller Task를 diagnostic log 후보로 먼저 기록한다.

## Continuous Implementation Flow

명시적인 구현 요청은 다음 workflow를 하나의 연속 작업으로 실행한다.

```text
Stage A  read-only baseline, ownership and exact-scope capture
Stage B  smallest implementation at the proven route-owned boundary
Stage C  bounded state-change and boundary log reinforcement
Stage D  unit and integration tests plus required clean forced build
Stage E  bounded runtime validation and log inspection when available and in scope
Stage F  final evidence, uncertainty and rollback report
```

Stage 사이에는 별도 계획·적용 확인을 위한 정지 절차를 두지 않는다.

이 GUI exact-bind/three-later-boundary 계약은 사용자가 지정한 기능 계약이다. Source audit로
owner, callback order와 mutation boundary가 입증되면 Stage B에서 먼저 구현할 수 있으며,
일반적인 unknown-root-cause runtime reproduction을 선행 조건으로 만들지 않는다.

구현 중 새로운 불확실성이 발견되면 추측성 behavior를 추가하지 않는다. 이미 증명된 범위는
유지하고 해당 경계의 bounded logging을 보강한 뒤 같은 workflow 안에서 검증하고 수정한다.
외부 runtime을 사용할 수 없거나 현재 요청 범위 밖이면 가능한 테스트와 clean build까지
완료하고 runtime 상태를 `NOT_RUN`으로 정확히 보고한다.

## Scope Boundary For Continuous Work

하나의 명시적 구현 요청은 설명된 기능 범위 안에서 source/config/test 수정, bounded logging,
테스트, 필수 clean build와 안전한 in-scope bounded runtime 검증을 포함한다.

다음은 자동으로 포함하지 않는다.

- dependency, Minecraft, Fabric, Loader, Loom, Gradle, Java 또는 Carry On version 변경
- 외부 프로그램 설치, 관리자 권한 또는 전역 설정 변경
- 파괴적 cleanup이나 광범위 rollback
- commit, push, publish 또는 deployment
- 요청과 무관한 route나 backend 확대

이는 반복 단계 gate가 아니라 작업 범위 경계다. 필요한 권한이나 정보가 실제로 달라질 때만
blocker로 보고한다.

<!-- 20260730_kpopmodder: Recorded the initial provisional Carry On design directions. -->

## Implementation Direction And Evidence Refinement

아래 구조는 구현 방향이다. Existing namespace convention, Task contract, installed Carry On
version과 available evidence를 확인한 뒤 세부 package, class, method 이름은 조정할 수 있지만,
ownership, optional dependency, fail-closed 및 engine-boundary 규칙은 약화하지 않는다.

### Optional Bridge Placement

Carry On optional bridge는 현재 Fabric Java module의 `src/main/java` 안에 두되, `adris.altoclef` namespace 밖의 LAVI-owned namespace에 둔다.

물리적 경계는 다음처럼 유지한다.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/
    adris/altoclef/**
        upstream-derived ChatClef engine

    <LAVI-owned-namespace>/minecraft/integration/carryon/**
        LAVI-owned optional integration
```

정확한 LAVI package 이름은 existing namespace convention을 확인한 뒤 확정한다.
현재 단계에서는 별도 Gradle module, 별도 Fabric mod, Python orchestration layer가 Minecraft client state를 직접 읽는 구조를 선택하지 않는다.

generic ChatClef 또는 LAVI class의 field, parameter, return type, generic signature, annotation, superclass, implemented interface, static initializer, class literal, exception type에 Carry On class를 노출하지 않는다.
mod presence와 version은 가능한 경우 Fabric Loader metadata로 관찰하고, version-specific reflection은 하나의 좁은 optional bridge에 격리한다.

### Carry On Parent Task Placement

Carry On 전용 parent Task가 필요하다는 것이 증명되면 같은 LAVI-owned namespace 아래 task package에 둔다.
`adris.altoclef.tasks` 또는 `adris.altoclef.chains`에 새 LAVI-specific Task를 추가하지 않는다.

새 Task가 `adris.altoclef.tasksystem.Task`를 상속하여 existing Task contract에 참여하는 것은 허용할 수 있다.
다만 다음 상속은 금지한다.

- `AltoClef` subclass
- `TaskRunner` subclass
- `InteractWithBlockTask` subclass
- `PlayerInteractionFixChain` subclass
- engine-wide lifecycle override

parent Task는 Carry On state reader와 generic child Task를 composition으로 사용한다.
parent Task가 completion, retry, terminal reason을 소유하고, `InteractWithBlockTask`는 approach, look, click attempt만 담당하도록 검토한다.

### Task Equality

correlationId는 logging identity이고 `isEqual()`은 semantic operation identity다.
새 correlationId를 `isEqual()`에 포함하지 않는다.

`isEqual()`을 구현하기 전에 `Task.tick`, `SingleTaskChain.setTask`, parent-child replacement, interruption 흐름을 실제 코드와 로그로 확인한다.
semantic identity 후보는 operation type, dimension, target position, target type, expected carry state이지만 현재 확정 구현으로 보지 않는다.

### Target Identity And Transition Confidence

`NOT_CARRYING -> CARRYING` 또는 `CARRYING -> NOT_CARRYING` 전환은 성공의 필요조건이다.
boolean transition만 관찰된 경우 operation-level transition은 판정할 수 있지만 target-level success는 `UNVERIFIED`일 수 있다.

transition confidence는 다음처럼 구분한다.

```text
TRANSITION_NOT_OBSERVED
TRANSITION_OBSERVED_TARGET_UNVERIFIED
TARGET_CORROBORATED
TARGET_VERIFIED
```

target identity를 확인할 수 없으면 특정 target 완료를 확정하거나 target-specific 후속 처리를 시작하지 않는다.
단, 같은 operation과 click에 귀속된 transition이 충분히 증명되면 동일 click의 무한 retry를 종료하는 제한된 patch는 검토할 수 있다.

### Input Ownership

현재 boolean pressed state는 ownership 증거가 아니다.
bounded-log reinforcement stage에서는 input cleanup behavior를 변경하지 않는다.

behavior/root-cause change에서 input 변경이 필요하면 operation-local input lease 또는 wrapper를 먼저 검토한다.
최소 추적 정보는 다음과 같다.

- previous state
- operation이 실제로 input을 변경했는지
- operation 또는 Task identity
- acquisition tick
- release path
- cleanup result

previous state가 true이면 operation이 획득한 것으로 간주하지 않는다.
snapshot만으로 다른 Task 또는 사용자 input ownership을 증명할 수 없으므로, 소유권이 확인되지 않은 input은 전역 release하지 않는다.
기존 `InputControls`에 owner-aware API를 추가하는 것은 broad global-change scope와 회귀 검증이 필요한 마지막 수단이다.

### PlayerInteractionFixChain

`PlayerInteractionFixChain`은 global boundary다.
logging-only hunk는 다음 조건으로 제한적으로 허용할 수 있다.

- behavior, return value, timer, input state 변경 없음
- state-change 또는 correlation 기반 logging
- 매 tick reflection discovery 없음
- 기존 실행 순서 변경 없음

behavior hunk는 다음이 증명된 경우에만 검토한다.

- 해당 chain action이 first failing boundary
- 같은 Carry On correlation과 연결됨
- parent Task 또는 adapter에서 격리할 수 없음
- generic interaction regression matrix 통과 가능
- Carry On absent 상태 보존

### Bounded Diagnostic Observation Order

bounded-log reinforcement의 기본 관찰 순서는 다음과 같다.

1. operation entry와 correlation
2. parent Task lifecycle
3. Carry On capability/state before
4. `InteractWithBlockTask` right-click boundary
5. bounded Carry On state after
6. child equality, replacement, interruption
7. input, custom goal, path state와 ownership evidence
8. `PlayerInteractionFixChain`
9. Task/TaskRunner global instrumentation

앞 단계에서 failure boundary가 보이면 뒤의 global instrumentation을 추가하지 않는다.
`InteractWithBlockTask.isFinished()` behavior는 변경하지 않는다.

### Document And Legacy Handling

canonical 문서는 다음 파일이다.

```text
plugins/Minecraft/docs/chatclef-carryon-integration-direction.md
```

legacy candidate는 다음 파일이다.

```text
plugins/Minecraft/docs/chatclef_carryon_integration.md
```

legacy 파일은 현재 GUI gate 작업 범위에서 수정, stage 또는 삭제하지 않는다. 향후 문서
cleanup 요청에 포함되면 exact path와 참조 부재를 다시 확인한 뒤 처리한다. Commit은
명시적 요청이 없는 한 수행하지 않는다.

### Workflow Evidence Requirements

Source/ownership audit에는 외부 Minecraft 경로가 필요하지 않다.

Bounded logging 설계에는 최소한 다음이 필요하다.

- `PICKUP` 또는 `PLACEMENT`
- exact command 또는 Task
- target block/state
- expected result
- observed loop
- existing related logs

Carry On API bridge를 설계하거나 적용하려면 다음도 필요하다.

- exact installed Carry On JAR
- exact Carry On version
- Minecraft/Fabric version

In-scope runtime reproduction 전에는 다음이 필요하다.

- Minecraft instance root
- mods directory
- Carry On JAR absolute path
- related mod list
- log path and reproduction time

### Additional Documentation Topics

canonical 또는 operational 문서에 다음을 보강한다.

- Carry On operation state machine
- ownership ledger
- regression gate matrix
- upstream divergence manifest

정확한 diagnostic method/file plan은 source/ownership audit 결과를 확인한 뒤 만든다.

## Required Evidence Before Root-Cause Patch

This section applies to an unknown-cause behavior correction. It does not force
diagnostics-first work for the explicitly specified container GUI exact-bind and
three-later-boundary feature when source evidence already proves its owner and
callback order. That feature follows the continuous implementation workflow above.

root-cause patch를 적용하려면 최소한 다음이 증명되어야 한다.

- 정확한 실패 Task
- 정확한 method
- 관련 game tick
- 실패 직전 state
- 시도한 state transition
- 마지막 성공 boundary
- 첫 실패 boundary
- Carry On 설치 여부
- Carry On 정확한 version
- 우클릭과 실제 Carry On 상태 변화의 대응
- retry owner와 attempt count
- 해당 작업이 획득한 input
- 해당 작업이 생성한 custom goal/path
- 작업 소유 상태만 cleanup할 수 있다는 증거
- TaskRunner와 Baritone 전역 상태를 건드릴 필요가 없다는 증거
- 일반 우클릭 기능이 수정 범위 밖이라는 증거

필수 증거가 빠지면 behavior-changing root-cause hunk를 추측해서 만들지 않는다. 가장 좁은
bounded logging을 보강하고 가능한 verification을 계속한 뒤 같은 workflow 안에서 다시
판단한다. 이는 evidence gate이지 workflow 정지 checkpoint가 아니다.

## ChatClef Engine Modification Last-Resort Gate

For an unknown-cause defect, direct modification of upstream-derived ChatClef or AltoClef behavior is
allowed only when evidence proves that the first failing boundary is inside the engine and the defect
cannot be safely contained in a LAVI-owned layer. For the explicitly specified GUI stabilization
feature, source-level ownership and callback-order proof may establish the minimum route-local hunk;
bounded logs, tests, clean build, and available in-scope runtime validation then verify it in that order.

### Engine Divergence Categories

Any modification to upstream-derived ChatClef or AltoClef source is an engine divergence.

Classify the recorded divergence as one of the following:

1. Diagnostics-only divergence
   - adds observation only
   - does not alter behavior, lifecycle, state, ownership, return values, or exception handling

2. Behavior-changing divergence
   - changes any engine decision, state, lifecycle, completion, retry, timeout, input, goal/path, fallback, cleanup, or result

A diagnostics-only observation is not evidence for an unproven behavior-changing divergence.

### Diagnostics-Only Engine Divergence Gate

A diagnostics-only engine hunk may be used only when the required boundary cannot be observed from LAVI orchestration, the optional Carry On bridge, a task-local helper, or a LAVI-owned parent Task.

It must preserve all of the following:

- return values
- branch conditions
- branch ordering
- Task selection
- Task completion
- retry behavior
- timeout behavior
- input state
- Baritone goal and path state
- fallback behavior
- exception propagation and handling
- interruption behavior
- cleanup behavior
- lifecycle ordering

Logging must use the existing logger when one exists.
Logging must be state-change, operation, attempt, or correlation based when the method is called frequently.

A diagnostics-only engine hunk must not perform per-tick reflection discovery, state-changing probing, Carry On API invocation with side effects, or hidden recovery behavior.
After recording the exact logging hunk, apply it within the same active implementation workflow and
verify that behavior and lifecycle remain unchanged. No separate planning/application checkpoint is
required.

### Mandatory Non-Blocking Engine-Divergence Record Before Edit

Before applying a behavior-changing engine modification, record all of the following. Use
`RUNTIME_REPRODUCTION` for an unknown-cause defect or `SOURCE_PROVEN_CONTRACT_GAP` for the explicitly
specified GUI feature. Under `SOURCE_PROVEN_CONTRACT_GAP`, a reproduction identifier, correlation
identifier and runtime game tick may be recorded as `NOT_YET_RUN`; exact source file/method evidence,
Task lifecycle evidence and callback-order proof are required instead.

1. Verified symptom or source-proven contract gap
   - evidence mode: `RUNTIME_REPRODUCTION` or `SOURCE_PROVEN_CONTRACT_GAP`
   - exact reproduced behavior or exact contract behavior missing from the current source
   - reproduction identifier or `NOT_YET_RUN`
   - relevant correlation identifier or `NOT_YET_RUN`

2. Verified failing boundary or minimum source-proven seam
   - repository-relative file
   - class
   - method
   - current line range
   - game tick or `NOT_YET_RUN`
   - runtime state before/after or source-level lifecycle/callback order

3. Last successful or source-proven preserved boundary
   - last runtime operation confirmed correct, or the last current source boundary that already satisfies
     the specified contract

4. First failing or source-proven missing boundary
   - first incorrect runtime transition, or the first source boundary where the specified contract is
     absent

5. Existing engine contract
   - responsibility owned by the method
   - callers
   - parent/child Task relationship
   - completion, interruption, and cleanup semantics
   - evidence supporting the contract

6. Containment analysis
   - why LAVI orchestration is insufficient
   - why the optional Carry On bridge is insufficient
   - why a task-local helper is insufficient
   - why a LAVI-owned parent Task is insufficient

7. Exact engine hunk
   - exact file
   - exact method
   - smallest recorded line or condition change
   - behavior intentionally changed
   - behavior intentionally left unchanged

8. Ownership impact
   - input
   - retry
   - timeout
   - custom goal
   - path
   - interruption
   - cleanup
   - global state

9. Generic interaction regression matrix
   - Carry On absent
   - generic right-click
   - container opening
   - door and trapdoor
   - bed
   - button and lever
   - block placement
   - item use
   - Baritone pathing
   - Task replacement and interruption
   - cleanup after failure

10. Rollback plan
    - baseline commit and file hash
    - exact rollback unit
    - inverse hunk or independently revertible commit
    - state and logs required to verify rollback

Record this information before editing, then continue without a pause when the
hunk remains inside the active implementation scope. The record is a
traceability and rollback requirement, not a blocking gate. If evidence
supports observation only, apply only the bounded diagnostic hunk and defer the
unproven behavior change while continuing verification.

### First Behavior Patch Scope

The first direct behavior-changing engine patch should normally be limited to:

- one upstream-derived file
- one method
- one minimal hunk

This is a scope gate, not permission to compress multiple responsibilities into one file.
The limit applies to the first direct behavior-changing engine patch.
It does not allow several unrelated changes to be hidden inside one large conditional, one method, or one file.

If the language or compiler requires a separate generic contract file for a proven extension seam, report:

- the exact contract file
- the exact engine wiring file
- the exact wiring method
- why one file is insufficient
- why the default path remains behavior-preserving

Record both files and apply them as one independently revertible implementation unit when the seam is
proven and remains in scope. No separate file-creation checkpoint is required.

Treat the following as containment triggers, not automatic workflow stops:

- behavior changes in two or more upstream lifecycle classes
- behavior changes in two or more behavior-owning methods
- simultaneous changes to `InteractWithBlockTask` and `PlayerInteractionFixChain`
- `TaskRunner` modification
- `AltoClef` modification
- global input ownership modification
- global Baritone goal or path cancellation modification
- package movement
- class renaming
- Task hierarchy redesign

Do not expand into these areas merely for convenience. Re-run containment
analysis and prefer the narrowest LAVI-owned boundary. If exact evidence proves
a listed change unavoidable and it remains inside the active request, record
the divergence and continue with the minimum independently revertible hunk,
bounded logs and regression verification. Otherwise leave the unproven behavior
unchanged and report the verified partial result.

### Generic Extension Seam Rule

A generic engine extension seam may be introduced only when evidence proves that no existing
LAVI-owned boundary can observe or contain either the verified failure or the source-proven specified
feature-contract gap.

The engine-side contract must:

- contain no Carry On types or imports
- contain no Carry On version logic
- contain no Carry On retry policy
- contain no Carry On timeout policy
- contain no Carry On success criteria
- contain no Carry On cleanup policy
- preserve existing behavior when no implementation is attached
- preserve existing return values and exception semantics
- preserve existing call order and lifecycle ownership
- avoid state-changing discovery or probing
- avoid per-tick reflection discovery
- avoid introducing a general hook framework for speculative future use

Observation hooks must use a default no-op implementation.
Decision hooks must use an explicit no-override or unchanged result.

Absence of an observer or decision implementation must never be interpreted as:

- success
- Task completion
- retry authorization
- fallback authorization
- cleanup authorization

The Carry On implementation must remain in a LAVI-owned namespace and must be connected through external composition.
The generic engine must not know that the attached implementation is for Carry On.

### Carry On Policy Boundary

The following must remain outside generic ChatClef engine classes:

- Carry On class or interface types
- Carry On imports
- Carry On mod ID or version policy
- Carry On capability interpretation
- Carry On pickup and placement success criteria
- Carry On retry count
- Carry On timeout
- Carry On target attribution
- Carry On-specific blacklist or cooldown
- Carry On-specific cleanup
- Carry On-specific fallback

These responsibilities belong in a LAVI-owned optional bridge, task-local helper, parent Task, or orchestration boundary.

### Engine Divergence Marker

Place the following marker immediately next to the recorded engine hunk:

```java
//20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
```

The marker must be adjacent to a short comment or divergence reference that identifies:

- evidence or reproduction ID
- verified failing boundary
- engine baseline reference
- modified method
- behavior intentionally changed
- behavior intentionally preserved
- rollback record

The marker alone is not sufficient evidence or documentation.
Do not add this marker to untouched upstream files.
Do not add it retroactively to the entire upstream source tree.

### Divergence Record

Record every planned or applied engine divergence with:

```text
Upstream component:
Engine baseline reference:
Repository HEAD at change time:
Modified file:
Modified class:
Modified method:
Baseline file hash:
Exact LAVI hunk:
Divergence category:
Verified reason:
Evidence:
Last successful boundary:
First failing boundary:
Containment analysis:
Ownership impact:
Generic behavior preserved:
Regression tests:
Runtime reproduction result:
Rollback:
Upstream comparison status:
Verification status:
```

Verification status must distinguish at least:

```text
SOURCE_RECORDED_NOT_APPLIED
APPLIED_NOT_BUILT
BUILT_NOT_REPRODUCED
PARTIALLY_VERIFIED
VERIFIED
ROLLED_BACK
```

Do not mark a divergence as `VERIFIED` when build, runtime reproduction, or the required regression checks have not been completed.

### Rollback Rule

Every engine divergence must have a rollback unit that can be applied without removing unrelated documentation, adapters, diagnostics, or other fixes.

Preferred rollback units include:

- an exact inverse hunk
- an independently revertible engine-patch commit
- an exact file-and-method restoration against the recorded baseline hash

Do not use broad reset, checkout, restore, or cleanup commands as the rollback plan.

### Continuous Engine-Divergence Workflow

An explicit implementation request covers, within its exact scope:

- the minimum diagnostics-only engine hunk when a LAVI-owned observer cannot reach the boundary
- the minimum proven behavior-changing engine hunk
- a proven generic extension contract and its wiring
- bounded logging reinforcement
- regression tests, clean build and available in-scope bounded runtime reproduction
- exact rollback of changes made by the current failed verification cycle when that rollback does not
  discard unrelated user work

These steps form one continuous workflow and do not require separate planning/application
checkpoints. After each material step, update the divergence record with evidence, uncertainty,
changed files, behavior impact and verification status, then continue to the next applicable step.

Dependency/version changes, destructive or broad rollback, commit, push and
deployment remain outside this implicit scope unless explicitly requested.

## Diagnostic Log Fields

새 exact GUI-gate boundary log, focused-test fixture, parser와 runtime evidence는
[Task Lifecycle Diagnostics](chatclef-task-lifecycle-diagnostics.md#canonical-exact-gui-gate-log-fields)의
canonical field 이름을 그대로 사용한다. 기존 unrelated payload key를 rename하지 않지만,
`observedBoundarySerial`, `screenIdentity`, `screenClass`, `handlerIdentity`, `handlerClass`,
`fullBindingValid`, `duplicateEventRejected`, `suppressedMutationType` 또는 `mutationOwner`를 새
GUI-gate alias로 만들지 않는다. 아래 broader diagnostic inventory도 겹치는 GUI field에는 canonical
이름을 사용한다.

```text
correlationId
operationId
openAttemptId
gameTick
clientTickBoundarySerial
candidateClientTickSerial
boundClientTickSerial
lastCountedBoundarySerial
stableLaterBoundaries
candidateTickExcluded
boundPromotionTickExcluded
sameBoundaryDuplicateSuppressed
gatePhaseBefore
threadName
topLevelTask
childTask
previousTask
nextTask
targetFamily
targetBlockId
target
dimension
carryOnLoaded
carryOnVersion
carryStateBefore
carryStateAfter
rightClickState
sneakState
leftClickState
clickResult
attemptCount
elapsedTicks
screenName
screenObjectIdentity
screenTypeExpected
screenTypeActual
screenTypeMatched
handledScreenHandlerIdentity
playerHandlerIdentity
handlerTypeExpected
handlerTypeActual
handlerTypeMatched
capturedSyncId
liveSyncId
worldIdentity
equippedItem
baritonePathing
customGoalOwner
stopReason
exceptionType
operationType
operationState
taskInstanceId
parentTaskInstanceId
semanticTaskIdentity
inputOwner
inputAcquiredByOperation
retryOwnerClass
retryIndex
timeoutOwner
cleanupOwner
cleanupResult
capabilityStatus
observationSource
targetBlockStateBefore
targetBlockStateAfter
carriedObjectIdBefore
carriedObjectIdAfter
transitionObservedTick
transitionAttributedToTarget
matchingBlockInteractEventObserved
matchingBlockInteractEventCount
matchingBlockInteractEventConsumed
duplicateBlockInteractEventRejected
openChildState
openChildIdentity
openChildQuiescent
openChildCleanupOwner
openChildCleanupComplete
fullGuiBoundPredicate
bindingDecision
candidateRejectReason
invalidationReason
guiInputAllowed
permissionAvailable
permissionFullRevalidationPassed
permissionConsumed
permissionReuseRejected
transferLifecycleEntryCommitted
slotMutationSuppressed
reachableMutationPath
reachableMutationKind
suppressedMutationOwner
slotActionOwner
slotActionType
slotButton
```

terminal event는 가능한 경우 다음 결과를 구분해야 한다.

```text
SUCCESS
FAILED
INTERRUPTED
RETRY_EXHAUSTED
CAPABILITY_ABSENT
CAPABILITY_INCOMPATIBLE
STATE_UNREADABLE
OBSERVATION_FAILED
```

terminal log에는 최소한 다음을 연결할 수 있어야 한다.

- correlationId
- attempts
- state before
- state after
- owned inputs released
- owned goal/path cleanup result
- unowned state left untouched
- elapsed ticks
- terminal reason

같은 값이 매 tick 반복되는 로그는 rate limiting 또는 state-change 기반으로 설계한다.
로그가 많다는 이유로 실패 boundary를 증명하는 핵심 필드는 생략하지 않는다.
GUI gate 필드는 attempt creation, matching interaction, TAIL candidate, quiescent handoff,
`GUI_BOUND`, distinct later boundary, permission transition/consumption, invalidation 및 terminal
boundary에서만 emit한다. Screen/handler/world 객체 전체를 출력하지 않고 correlation에 필요한
bounded identity token과 exact class/type만 기록한다.

## Upstream Baseline Provenance

현재 문서 단계에서 정확한 upstream provenance는 다음 형식으로만 기록한다.
확인하지 못한 값은 추측하지 않고 `UNVERIFIED`로 둔다.

```text
Upstream repository: UNVERIFIED
Release or tag: UNVERIFIED
Exact commit: UNVERIFIED
Imported subtree: plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
Verification method: UNVERIFIED
Known LAVI-specific divergences: UNVERIFIED
Verification status: UNVERIFIED
```

verification status는 다음 값으로 구분한다.

```text
VERIFIED
PARTIALLY_VERIFIED
UNVERIFIED
```

정확한 upstream commit을 실제로 확인하지 못했다면 추측하지 않는다.
일부 파일만 같다는 사실을 전체 ChatClef subtree가 동일하다는 결론으로 확대하지 않는다.

## Patch Scope Containment Triggers

다음 항목은 구현을 넓히기 전에 containment를 다시 확인해야 하는 신호다.

- bounded-logging work에서 upstream-derived Java 파일 둘 이상 수정
- `InteractWithBlockTask`와 `PlayerInteractionFixChain` 동시 수정
- `TaskRunner` 변경
- `AltoClef` 변경
- global chain 변경
- generic input handling 변경
- package 이동
- 기존 class rename
- 기존 Task hierarchy 변경

이 항목이 보이면 편의상 범위를 확대하지 않는다. LAVI-owned boundary로 다시 좁히고 필요한
bounded log와 회귀 검증을 같은 workflow에서 수행한다. 정확한 증거가 더 넓은 변경을
요구하며 그 변경도 현재 요청 범위에 포함되면 divergence record와 rollback unit을 남기고
최소 hunk로 계속 진행한다. 소유권이나 failure boundary가 입증되지 않으면 해당 behavior만
변경하지 않고 검증된 부분과 남은 gap을 보고한다. 이 section 자체는 workflow 대기 지점이
아니다.

## Build Failure Handling

Codex 환경에서 build가 불가능하면 Gradle, wrapper, Java, dependency를 자동으로 변경하지 않는다.

보고할 내용:

- 실행하려던 정확한 명령
- 실행하지 못한 이유
- 필요한 working directory
- 필요한 Java version
- 사용자가 직접 실행할 정확한 명령
- 성공 시 기대되는 결과
- 실패 시 전달받아야 할 전체 출력

사용자가 직접 빌드한 결과를 받기 전에는 컴파일 성공, 테스트 통과, 수정 완료라고 보고하지 않는다.
