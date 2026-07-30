<!-- 20260730_kpopmodder: Added this document to preserve the ChatClef/Carry On integration direction before code changes. -->

# ChatClef / Carry On Integration Direction

이 문서는 Minecraft 플러그인에서 ChatClef/AltoClef와 Carry On 연동을 다룰 때의 복원 기준선, 설계 방향, 진단 순서를 정리한다.
현재 목적은 ChatClef를 LAVI 전용 엔진으로 개조하는 것이 아니라, ChatClef 0.18.23 기준 동작을 가능한 한 보존하면서 필요한 Carry On 연동을 얇고 소유권이 명확한 layer로 붙이는 것이다.

이 문서는 Java 구현, diagnostics patch, Carry On adapter 구현, Task 추가, root-cause fix를 승인하지 않는다.
각 Phase는 별도의 사용자 승인이 필요하며, 설계 문서 추가 승인을 다음 Phase 승인으로 해석하지 않는다.

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

Carry On 전용 parent Task를 새로 제안하려면 먼저 현재 ChatClef Task contract를 읽고 다음 호출 관계를 확인한다.

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

현재 실행 thread가 안전한지 확인되지 않으면 behavior를 바꾸지 말고 thread name, game tick, caller Task를 diagnostic log 후보로 먼저 제안한다.

## Phase Flow

작업은 다음 순서로만 진행한다.

```text
Phase 0   read-only baseline audit
Phase 0-B failed-work salvage analysis, only when exact prior artifact exists
Phase 1   diagnostics-only patch proposal
Phase 1A  diagnostics-only patch application, after explicit approval
Phase 2   reproduce and prove last successful boundary / first failing boundary
Phase 3   minimum root-cause patch after evidence
```

Phase 0에서는 파일 수정, build, test, Minecraft 실행, dependency 변경, patch 적용을 하지 않는다.
Phase 1에서도 behavior 변경은 금지되며, diagnostics-only patch는 return value, Task 완료 조건, retry, timeout, cooldown, input state, Baritone goal/path, fallback 결과를 바꾸면 안 된다.
Phase 3은 실패 boundary가 로그와 재현으로 증명된 뒤에만 검토한다.

## Separate Approval For Every Phase

모든 Phase는 별도 사용자 승인을 요구한다.

다음 승인은 서로 다른 승인이다.

- 설계 문서 추가 승인
- baseline audit 승인
- diagnostics-only patch 제안 승인
- diagnostics-only patch 적용 승인
- build 또는 runtime reproduction 승인
- root-cause patch 제안 승인
- root-cause patch 적용 승인
- commit 승인
- push 승인

한 Phase의 승인을 다음 Phase 승인으로 해석하지 않는다.

각 Phase 종료 시:

1. 실제 확인된 증거 보고
2. 남은 불확실성 보고
3. 다음 Phase에서 제안하는 정확한 파일 경로 보고
4. behavior가 바뀌는지 여부 보고
5. 반드시 정지

## Required Evidence Before Root-Cause Patch

root-cause patch를 제안하려면 최소한 다음이 증명되어야 한다.

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

하나라도 빠지면 root cause는 추정 또는 확인 불가로 유지하고 diagnostics 단계에서 멈춘다.

## Diagnostic Log Fields

diagnostic 설계에는 최소한 다음 필드를 고려한다.

```text
correlationId
gameTick
threadName
topLevelTask
childTask
previousTask
nextTask
targetType
targetId
targetPosition
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
retryOwner
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

## Patch Scope Stop Gates

다음 중 하나가 필요해 보이면 자동으로 구현하지 않고 멈춰서 범위가 넓어진 이유를 보고한다.

- diagnostics-only 단계에서 upstream-derived Java 파일 둘 이상 수정
- `InteractWithBlockTask`와 `PlayerInteractionFixChain` 동시 수정
- `TaskRunner` 변경
- `AltoClef` 변경
- global chain 변경
- generic input handling 변경
- package 이동
- 기존 class rename
- 기존 Task hierarchy 변경

이 경우 실패 boundary가 아직 충분히 격리되지 않은 신호로 보고 diagnostics 단계로 돌아간다.

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

