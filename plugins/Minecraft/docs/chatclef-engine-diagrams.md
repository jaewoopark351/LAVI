# ChatClef 엔진 다이어그램

상태: 문서 전용.

이 문서는 현재 ChatClef / AltoClef 엔진 구조를 다이어그램으로 이해하기 위한
참고 문서다. Carry On 구현 방향은 별도 canonical 문서가 우선한다.

- [ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)
- [ChatClef 구조 개요](chatclef-structure-overview.md)

이 문서는 Java 구현, diagnostic log 삽입, behavior change, build, commit,
push를 승인하지 않는다.

## 1. 전체 엔진 지도

```mermaid
flowchart TB
    Fabric[fabric.mod.json<br/>entrypoint]
    Alto[AltoClef]
    Init[onInitializeLoad]

    Commands[CommandExecutor<br/>사용자 명령]
    Runner[TaskRunner<br/>active chain 선택]
    Trackers[TrackerManager<br/>storage, entity, block, chunk]
    Controllers[Controller layer<br/>InputControls, SlotHandler, PlayerExtraController]
    Chains[TaskChain들<br/>User, survival, defense, interaction fix]
    Tasks[Task tree<br/>parent task + child task]
    Baritone[Baritone<br/>pathing, custom goal, input override]
    Minecraft[Minecraft client state<br/>world, player, screen, inventory]

    Fabric --> Alto --> Init
    Init --> Commands
    Init --> Runner
    Init --> Trackers
    Init --> Controllers
    Init --> Chains
    Runner --> Chains
    Chains --> Tasks
    Tasks --> Baritone
    Tasks --> Controllers
    Trackers --> Minecraft
    Controllers --> Minecraft
    Baritone --> Minecraft
```

요점: `AltoClef`는 엔진의 중심 조립 지점이다. 명령, TaskRunner, tracker,
controller, chain을 만들고 서로 연결한다.

## 2. 핵심 객체지향 class diagram

```mermaid
classDiagram
    class AltoClef {
        -TaskRunner taskRunner
        -UserTaskChain userTaskChain
        -InputControls inputControls
        +onInitializeLoad()
        +runUserTask(Task task)
        +runUserTask(Task task, Runnable onFinish)
        +stop()
        +getTaskRunner()
        +getInputControls()
    }

    class TaskRunner {
        -List chains
        -TaskChain cachedCurrentTaskChain
        -boolean active
        +tick()
        +enable()
        +disable()
        +addTaskChain(TaskChain chain)
        +getCurrentTaskChain()
    }

    class TaskChain {
        +tick()
        +stop()
        +onInterrupt(TaskChain other)
        +getPriority()
        +isActive()
        +getName()
    }

    class SingleTaskChain {
        -Task mainTask
        -boolean interrupted
        +setTask(Task task)
        +getCurrentTask()
        #onTaskFinish(AltoClef mod)
    }

    class UserTaskChain {
        -Runnable currentOnFinish
        -Stopwatch taskStopwatch
        +runTask(AltoClef mod, Task task, Runnable onFinish)
        +cancel(AltoClef mod)
        #onTaskFinish(AltoClef mod)
    }

    class Task {
        -Task sub
        -boolean first
        -boolean active
        -boolean stopped
        +tick(TaskChain parentChain)
        +stop(Task interruptTask)
        +interrupt(Task interruptTask)
        +isFinished()
        #onStart()
        #onTick()
        #onStop(Task interruptTask)
        #isEqual(Task other)
        #toDebugString()
    }

    class InteractWithBlockTask {
        -ItemTarget toUse
        -BlockPos target
        -Input interactInput
        -TimerGame clickTimer
        +isFinished()
        #onStart()
        #onTick()
        #onStop(Task interruptTask)
        #isEqual(Task other)
        +getCurrentReach()
    }

    class DestroyBlockTask {
        -BlockPos pos
        -boolean isMining
        +isFinished()
        #onStart()
        #onTick()
        #onStop(Task interruptTask)
        #isEqual(Task other)
    }

    AltoClef *-- TaskRunner
    AltoClef *-- UserTaskChain
    AltoClef *-- InputControls
    TaskRunner o-- TaskChain
    TaskChain <|-- SingleTaskChain
    SingleTaskChain <|-- UserTaskChain
    SingleTaskChain o-- Task : mainTask
    Task o-- Task : child sub
    Task <|-- InteractWithBlockTask
    Task <|-- DestroyBlockTask
```

요점: `Task` 상속은 ChatClef의 established contract에 참여하기 위한 것이다.
하지만 `AltoClef`, `TaskRunner`, global chain을 상속해서 새 엔진을 만들면
엔진 lifecycle을 fork하게 되므로 금지한다.

## 3. TaskRunner chain 선택 sequence

```mermaid
sequenceDiagram
    participant Tick as client tick
    participant Runner as TaskRunner
    participant ChainA as TaskChain A
    participant ChainB as TaskChain B
    participant Current as cachedCurrentTaskChain

    Tick->>Runner: tick()
    Runner->>Runner: active와 inGame 확인
    Runner->>ChainA: isActive()
    ChainA-->>Runner: true 또는 false
    Runner->>ChainA: getPriority()
    ChainA-->>Runner: priority
    Runner->>ChainB: isActive()
    ChainB-->>Runner: true 또는 false
    Runner->>ChainB: getPriority()
    ChainB-->>Runner: priority
    Runner->>Runner: 가장 높은 priority chain 선택
    alt 선택 chain이 이전 current와 다름
        Runner->>Current: onInterrupt(newChain)
    end
    Runner->>Runner: cachedCurrentTaskChain 갱신
    Runner->>ChainB: tick()
```

요점: TaskRunner는 직접 Task를 고치는 곳이 아니라 active chain을 선택하고
interrupt를 전달하는 scheduler다.

## 4. Task lifecycle state diagram

```mermaid
stateDiagram-v2
    [*] --> NewTask
    NewTask --> Start: first tick
    Start --> Active: onStart()
    Active --> Tick: onTick()
    Tick --> ChildReturned: child Task 반환
    Tick --> NoChild: null 반환
    ChildReturned --> CompareChild: isEqual()
    CompareChild --> StopOldChild: child 변경 필요
    CompareChild --> TickChild: 같은 child 유지
    StopOldChild --> TickChild: old child stop(new child)
    TickChild --> Active
    NoChild --> StopExistingChild: 기존 child 있음
    StopExistingChild --> Active
    NoChild --> Active: 기존 child 없음
    Active --> Interrupted: interrupt()
    Interrupted --> Start: 다음 tick에 다시 onStart()
    Active --> Stopped: stop()
    Stopped --> [*]
```

요점: parent Task가 child를 반환하면 `Task.tick()`이 child 교체와 stop 흐름을
관리한다. child 동일성은 `isEqual()`이 결정한다.

## 5. parent / child Task 교체 sequence

```mermaid
sequenceDiagram
    participant Parent as Parent Task
    participant OldChild as 기존 child
    participant NewChild as 새 child
    participant Chain as parent TaskChain

    Chain->>Parent: tick(parentChain)
    Parent->>Parent: onTick()
    Parent-->>NewChild: 새 child 반환
    Parent->>NewChild: isEqual(old child)
    alt 동일하지 않고 interruption 가능
        Parent->>OldChild: stop(new child)
        Parent->>Parent: sub = new child
    else 동일함
        Parent->>Parent: 기존 sub 유지
    end
    Parent->>NewChild: tick(parentChain)
```

요점: Carry On parent Task를 만들 때 매 tick 새 correlationId를 `isEqual()`에
넣으면 child가 계속 새 Task로 취급될 수 있다. correlationId는 log identity,
`isEqual()`은 semantic operation identity로 분리해야 한다.

## 6. UserTaskChain run / finish 흐름

```mermaid
sequenceDiagram
    participant API as AltoClef.runUserTask()
    participant Chain as UserTaskChain
    participant Runner as TaskRunner
    participant Main as mainTask
    participant Alto as AltoClef

    API->>Chain: runTask(mod, task, onFinish)
    Chain->>Runner: enable()
    Chain->>Chain: taskStopwatch.begin()
    Chain->>Chain: setTask(task)
    Chain->>Main: reset()
    loop client tick
        Runner->>Chain: tick()
        Chain->>Main: isFinished()
        alt not finished
            Chain->>Main: tick(parentChain)
        else finished or stopped
            Chain->>Chain: onTaskFinish(mod)
            Chain->>Alto: stop() 또는 idle command 처리
        end
    end
```

요점: 사용자 명령으로 실행되는 최상위 Task 완료는 `UserTaskChain`이 처리한다.
그래서 Carry On 완료 조건은 별도 parent Task의 `isFinished()`로 드러나야 한다.

## 7. InteractWithBlockTask 세부 흐름

```mermaid
flowchart TB
    Start[onStart]
    Reset[moveChecker, stuckCheck, wanderTask, clickTimer reset]
    PathCancel[pathing forceCancel]
    Tick[onTick]
    Portal{nether portal 안?}
    Stuck{stuck 또는 progress 실패?}
    Item{필요 item 충족?}
    Wander{wander active?}
    Goal[interact goal 생성]
    Click[rightClick()]
    Cant[CANT_REACH]
    Wait[WAIT_FOR_CLICK]
    Attempt[CLICK_ATTEMPTED]
    SetGoal[customGoalProcess.setGoalAndPath]
    LostControl[customGoalProcess.onLostControl]
    WanderTask[wanderTask 반환]
    Null[null 반환]
    Stop[onStop]
    Cleanup[pathing forceCancel, SNEAK release]
    Finish[isFinished = false]

    Start --> PathCancel --> Reset --> Tick
    Tick --> Portal
    Portal --> Stuck
    Stuck -- stuck --> WanderTask
    Stuck -- ok --> Item
    Item -- 부족 --> GetItem[TaskCatalogue.getItemTask]
    Item -- 충족 --> Wander
    Wander -- active --> WanderTask
    Wander -- inactive --> Goal --> Click
    Click --> Cant --> SetGoal --> Null
    Click --> Wait --> LostControl --> Null
    Click --> Attempt --> LostControl --> Null
    Attempt --> Timer{clickTimer elapsed?}
    Timer -- yes --> WanderTask
    Timer -- no --> Null
    Stop --> Cleanup
    Tick --> Finish
```

요점: 이 Task는 접근, 바라보기, 클릭 시도를 담당한다. 성공 판정 자체를
항상 소유하는 구조가 아니기 때문에 Carry On 완료 판정을 여기에 전역으로
넣으면 안 된다.

## 8. rightClick boundary sequence

```mermaid
sequenceDiagram
    participant Task as InteractWithBlockTask
    participant Storage as StorageHelper / SlotHandler
    participant Look as LookHelper
    participant Input as InputControls
    participant Game as Minecraft client

    Task->>Storage: 열린 screen과 cursor stack 확인
    alt screen 처리 필요
        Task->>Storage: slot click 또는 closeScreen()
        Storage-->>Task: WAIT_FOR_CLICK
    else target reach 확인
        Task->>Look: getCurrentReach()
        alt reachable 아님
            Task-->>Task: CANT_REACH
        else reachable
            Task->>Look: isLookingAt(target)
            alt 아직 target을 안 봄
                Task->>Look: lookAt(rotation)
                Task-->>Task: WAIT_FOR_CLICK
            else target을 보고 있음
                Task->>Storage: forceEquipItem 또는 deequip
                Task->>Input: tryPress(interactInput)
                Input->>Game: key pressed one frame
                Task-->>Task: CLICK_ATTEMPTED
            end
        end
    end
```

요점: `CLICK_ATTEMPTED`는 클릭 시도이지 Carry On 성공이 아니다. Carry On
성공은 state transition으로 별도 관찰해야 한다.

## 9. InputControls one-frame press lifecycle

```mermaid
sequenceDiagram
    participant TickPre as onTickPre()
    participant Task as Task
    participant Input as InputControls
    participant Key as Minecraft KeyBinding
    participant TickPost as onTickPost()

    TickPre->>Input: 이전 tick의 toUnpress 처리
    Input->>Key: setPressed(false)
    Task->>Input: tryPress(CLICK_RIGHT)
    alt waitForRelease에 이미 있음
        Input-->>Task: 아무것도 하지 않음
    else 새 press 가능
        Input->>Key: setPressed(true)
        Input->>Key: onKeyPressed(defaultKey)
        Input->>Input: toUnpress에 추가
        Input->>Input: waitForRelease에 추가
    end
    TickPost->>Input: waitForRelease clear
```

요점: `tryPress()`는 한 프레임 press에 가깝다. 단순히 키가 눌렸다는 사실만으로
operation ownership이나 성공을 증명할 수 없다.

## 10. PlayerInteractionFixChain 영향 지도

```mermaid
flowchart TB
    Chain[PlayerInteractionFixChain]
    Priority[getPriority]
    Active{inGame?}
    UserTask{UserTaskChain active?}
    BetterTool[breaking 중 더 좋은 tool equip]
    SneakCheck{SNEAK held?}
    SneakTimer{shiftDepressTimeout elapsed?}
    ReleaseSneak[SNEAK release]
    RefreshTimer{inventory refresh timer elapsed?}
    Refresh[refreshInventory]
    Cursor{cursor stack held?}
    MoveCursor[slot click으로 cursor stack 처리]
    Screen{open screen + look changed?}
    CloseScreen[closeScreen 또는 cursor 처리]
    End[negative priority 반환]

    Chain --> Priority --> Active
    Active -- no --> End
    Active -- yes --> UserTask
    UserTask -- yes --> BetterTool
    BetterTool --> SneakCheck
    SneakCheck -- yes --> SneakTimer
    SneakTimer -- yes --> ReleaseSneak
    SneakCheck -- no --> RefreshTimer
    ReleaseSneak --> RefreshTimer
    RefreshTimer -- yes --> Refresh --> End
    RefreshTimer -- no --> Cursor
    Cursor -- yes --> MoveCursor --> End
    Cursor -- no --> Screen
    Screen -- yes --> CloseScreen --> End
    Screen -- no --> End
```

요점: 이 chain은 cursor, screen, tool, SNEAK 등 전역 interaction 환경을
다룬다. Carry On 전용 retry나 cleanup을 여기에 넣으면 일반 상호작용 전체가
영향받을 수 있다.

## 11. Baritone goal / path 영향 지도

```mermaid
flowchart TB
    Task[Task]
    Pathing[pathingBehavior]
    CustomGoal[customGoalProcess]
    Explore[exploreProcess]
    Builder[builderProcess]
    InputOverride[inputOverrideHandler]

    ForceCancel[forceCancel]
    SetGoal[setGoalAndPath]
    LostControl[onLostControl]
    ClearKeys[clearAllKeys]
    ForceInput[setInputForceState]

    Task --> Pathing --> ForceCancel
    Task --> CustomGoal --> SetGoal
    Task --> CustomGoal --> LostControl
    Task --> Explore --> LostControl
    Task --> Builder --> LostControl
    Task --> InputOverride --> ForceInput
    Task --> InputOverride --> ClearKeys

    Interact[InteractWithBlockTask] --> Pathing
    Interact --> CustomGoal
    Destroy[DestroyBlockTask] --> Pathing
    Destroy --> CustomGoal
    Destroy --> Builder
    Destroy --> InputOverride
    UserFinish[UserTaskChain finish] --> Pathing
    UserFinish --> InputOverride
    AltoStop[AltoClef.stop] --> Pathing
    AltoStop --> InputOverride
```

요점: Baritone과 input override는 여러 Task와 chain이 만지는 shared resource다.
Carry On operation이 소유하지 않은 path, goal, input을 전역 해제하면 안 된다.

## 12. container open에서 InteractWithBlockTask가 쓰이는 흐름

```mermaid
flowchart TB
    ContainerTask[AbstractDoToStorageContainerTask]
    FindTarget[getContainerTarget]
    Target{container target 있음?}
    Wander[onSearchWander]
    Open{screenHandlerMatches?}
    Subtask[onContainerOpenSubtask]
    BlockAbove{chest 위 solid block?}
    Destroy[DestroyBlockTask]
    Interact[InteractWithBlockTask targetPos]

    ContainerTask --> FindTarget --> Target
    Target -- 없음 --> Wander
    Target -- 있음 --> Open
    Open -- 이미 열림 --> Subtask
    Open -- 안 열림 --> BlockAbove
    BlockAbove -- 막힘 --> Destroy
    BlockAbove -- 열 수 있음 --> Interact
```

요점: `InteractWithBlockTask`는 Carry On뿐 아니라 container open에도 쓰인다.
따라서 이 Task의 완료 조건을 Carry On 기준으로 전역 변경하면 container 흐름도
깨질 수 있다.

## 13. DestroyBlockTask 비교 다이어그램

```mermaid
flowchart TB
    Destroy[DestroyBlockTask]
    Reach{block in reach?}
    Safe{safe to cancel pathing?}
    Mine[CLICK_LEFT input override true]
    Move[custom goal로 block 접근]
    Stop[onStop]
    Cleanup[CLICK_LEFT false<br/>SNEAK/MOVE release<br/>pathing forceCancel]
    Finish{target block is air?}

    Destroy --> Reach
    Reach -- yes --> Safe
    Safe -- yes --> Mine
    Reach -- no --> Move
    Safe -- no --> Move
    Destroy --> Finish
    Stop --> Cleanup
```

요점: `DestroyBlockTask`는 block이 air가 되면 완료되는 명확한 완료 조건을
가진다. 반면 `InteractWithBlockTask`는 generic click attempt라서 현재
`isFinished()`가 false다. Carry On에는 별도 parent completion이 필요하다.

## 14. Carry On을 붙일 수 있는 외부 경계

```mermaid
flowchart LR
    Command[LAVI command]
    CarryParent[Carry On parent Task]
    Bridge[optional Carry On bridge]
    GenericChild[InteractWithBlockTask]
    Engine[ChatClef engine]
    GlobalChain[PlayerInteractionFixChain]
    TaskRunner[TaskRunner]

    Command --> CarryParent
    CarryParent --> Bridge
    CarryParent --> GenericChild
    GenericChild --> Engine
    Engine --> TaskRunner
    Engine --> GlobalChain

    CarryParent -. 건드리지 않음 .-> TaskRunner
    CarryParent -. 건드리지 않음 .-> GlobalChain
```

요점: Carry On integration은 `TaskRunner`나 `PlayerInteractionFixChain`을
상속하거나 전역 수정하지 않고, parent Task + optional bridge + generic child
composition으로 시작해야 한다.

## 15. root-cause patch 전 증거 흐름

```mermaid
flowchart TB
    Symptom[증상 재현]
    Correlation[correlationId 부여]
    ParentLog[parent Task lifecycle log]
    Before[Carry On state before]
    Click[right-click boundary]
    After[Carry On state after]
    Replace[child replacement / isEqual log]
    Ownership[input / goal / path ownership log]
    ChainLog[PlayerInteractionFixChain log]
    Boundary{first failing boundary 증명?}
    DiagnosticsOnly[diagnostics 유지]
    RootCause[root-cause patch 제안]
    Approval[사용자 승인 대기]

    Symptom --> Correlation --> ParentLog --> Before --> Click --> After
    After --> Replace --> Ownership --> ChainLog --> Boundary
    Boundary -- 아니오 --> DiagnosticsOnly
    Boundary -- 예 --> RootCause --> Approval
```

요점: 실패 지점이 증명되기 전에는 behavior를 바꾸지 않는다. build 성공도
root cause 증명이 아니다.
