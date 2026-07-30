<!-- 20260730_kpopmodder: Added this document to preserve the ChatClef/Carry On integration direction before code changes. -->

# ChatClef / Carry On Integration Direction

이 문서는 Minecraft 플러그인에서 ChatClef/AltoClef와 Carry On 연동을 다룰 때의 복원 기준선, 설계 방향, 진단 순서를 정리한다.
현재 목적은 ChatClef를 LAVI 전용 엔진으로 개조하는 것이 아니라, ChatClef 0.18.23 기준 동작을 가능한 한 보존하면서 필요한 Carry On 연동을 얇고 소유권이 명확한 layer로 붙이는 것이다.

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
이 문제를 원인으로 단정하지 말고, 로그와 현재 구현으로 먼저 확인한다.

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
```

같은 값이 매 tick 반복되는 로그는 rate limiting 또는 state-change 기반으로 설계한다.
로그가 많다는 이유로 실패 boundary를 증명하는 핵심 필드는 생략하지 않는다.

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
