<!-- 20260819_kpopmodder: Defined the bounded diagnostics-only plan for the bare deposit non-terminating loop. -->
<!-- 20260831_openai: Linked the later post-checkpoint shared admission and bounded terminal-accounting direction while preserving this plan's historical Store event provenance. -->

# ChatClef Bare Deposit Diagnostics-Only Plan

Date: 2026-08-19

Status:

```text
design status: DOCUMENTED
implementation status: PARTIALLY_IMPLEMENTED_DIAGNOSTICS_ONLY
implementation completeness: 0 canonical event families complete
runtime evidence: LIVE_PREFIX_REPRODUCTION_RECORDED
root cause: still unverified
behavior change: prohibited
wire change: prohibited
```

이 문서는 `LAVI_TEST_Fabric01`의 bare/local `deposit` 실행이
`StoreInAnyContainerTask`에서 약 50분 46초 동안 자율 종료하지 못하고
`StopCommand`로 취소된 사건의 마지막 성공 경계와 최초 실패 경계를 다음
재현에서 관찰·검증하기 위한 diagnostics-only 설계다.

Canonical incident evidence:

```text
plugins/Minecraft/docs/chatclef-bare-deposit-container-handoff-loop-investigation.md
```

Required policy and operational references:

```text
AGENTS.md
plugins/Minecraft/docs/minecraft-backend-separation.md
plugins/Minecraft/docs/chatclef-carryon-integration-direction.md
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
plugins/Minecraft/docs/chatclef-baritone-cache-troubleshooting.md
plugins/Minecraft/docs/chatclef-fabric-build-verification.md
plugins/Minecraft/docs/chatclef-engine-divergence-record.md
plugins/Minecraft/docs/chatclef-post-completion-store-loop-investigation.md
plugins/Minecraft/docs/chatclef-bare-deposit-diagnostics-reproduction-2026-08-19-r1.md
```

The task-lifecycle document owns the generic event and bounded-logging rules;
the Carry On direction owns upstream modification and approval gates; the cache
runbook owns operational cause exclusion; the build runbook owns any later
authorized build; the divergence record owns any later upstream diagnostic
hunk; and the post-completion document is historical 2026-08-07 context only.

이 문서는 Java 또는 Python source 변경, build, Minecraft 실행, runtime
재현, dependency 변경, wire payload 변경, commit 또는 push를 승인하지
않는다. 설계 자체와 실제 구현 상태를 구분한다. 아래 canonical contract는
완료 기준이며, 현재 source 상태는 `Implementation Status Ledger`가 권위다.
그 ledger에서 `SOURCE_PRESENT_PARTIAL` 또는 `IMPLEMENTED_EXTENSION_PARTIAL`로
표시되지 않은 제안 항목은 `PROPOSED_NOT_IMPLEMENTED`다.

## Goal And Non-Goals

다음 재현에서 하나의 operation chain으로 아래 경계를 연결하는 것이
목표다.

```text
DepositCommand origin
  -> Store parent branch decision
  -> Store root route/acquisition child reconciliation
  -> filtered child search
  -> pursuit decision
  -> filtered-target callback reference/reset boundary
  -> target-action child reconciliation
  -> crafting-table cost / route target / route branch / interaction target
  -> Baritone goal / calculation / adoption
  -> container interaction
  -> GUI and cache observation
  -> transfer selection
  -> slot mutation
  -> ContainerStoredTracker delta
  -> Store root progress or terminal state
```

이 설계가 답해야 하는 질문은 다음과 같다.

```text
마지막으로 성공한 경계는 어디인가?
명시적으로 실패한 최초 경계는 어디인가?
실패가 아니라 관측이 끊긴 최초 경계는 어디인가?
동일한 target과 attempt가 다음 계층까지 유지됐는가?
detail cap 이후에도 같은 반복이 계속됐는가?
실제 container slot 효과가 있었는가?
```

이 단계의 목표가 아닌 것은 다음과 같다.

```text
loop를 자동으로 중단하는 것
retry 또는 timeout을 추가하는 것
container 또는 crafting-table 선택을 바꾸는 것
Baritone goal, path, process owner를 바꾸는 것
slot 이동 또는 tracker 계산을 바꾸는 것
NPE를 복구하거나 숨기는 것
deposit 성공 또는 실패 의미를 바꾸는 것
```

## Evidence And Source Authority

이 설계의 사건 사실은 incident 문서의 finalized log manifest와 bounded
line recount를 따른다. 핵심 사실은 다음과 같다.

```text
bare/local DepositCommand origin confirmed
Store task identity remained stable
natural completion not observed
explicit StopCommand cancellation after about 3046.404 seconds
Store progress detail exhausted cap=256 near the start of the run
2,492 primary CONTAINER_TASK_TARGET_DECISION records
2,492 distinct DoCraftInTableTask instances reached onTick
2,491 primary CONTAINER_TASK_BRANCH records, all return_open_table_task
Baritone calculate/adoption detail covered only generations 11-290
six NPE episodes, with only the first carrying the full UserBlockRangeTracker stack
cobblestone notStored changed 317 -> 256 and the final detailed checkpoint remained 256
storedCountByTarget did not prove a successful deposit
```

사용자 전달문에는 이후 `317`로 돌아왔다는 표현도 있었지만 canonical bounded
recount에서는 마지막 detailed checkpoint가 `256`이고, 그 뒤 repeat summary에는
target별 exact count가 없다. 따라서 이 plan은 `317 -> 256 -> 317` 복귀를
확인 사실로 채택하지 않는다. 다음 재현에서 `STORE_TARGET_STATE_CHANGE`와 final
summary가 이 미관측 구간을 직접 보강해야 한다.

현재 repository source와 당시 loaded runtime JAR의 byte-for-byte 동일성은
증명되지 않았다. 실제 source 적용 전에는 다음을 다시 확인해야 한다.

```text
repository HEAD and Java working-tree status
current source method boundaries
clean forced build artifact hash
active CurseForge instance JAR hash and code-source marker
```

이 확인은 build 또는 runtime 실행을 자동 승인하지 않는다.

## Existing Events And Proposed Changes

기존 event 이름은 evidence continuity를 위해 유지한다.

| Event | Status | Plan |
| --- | --- | --- |
| `DEPOSIT_COMMAND_INVOCATION_DECISION` | `EXISTING` | local `storeOperationId`를 발급하고 Store task identity에 bind하도록 field와 diagnostic registry 연결만 보강 |
| `USER_TASK_CHAIN_TASK_ORIGIN_DECISION` | `EXISTING` | root assignment provenance를 그대로 사용 |
| `USER_TASK_CHAIN_CANCEL_REQUESTED` | `EXISTING` | true stop 전에 root와 `storeOperationId`를 bind해 explicit cancel provenance를 전달 |
| `STORE_IN_ANY_CONTAINER_START` | `EXISTING` | 같은 `storeOperationId`를 initial/resume activation과 연결 |
| `STORE_IN_ANY_CONTAINER_STOP` | `EXISTING_CALLBACK` | `onStop()` 관측은 유지하되 true `Task.stop()`과 temporary `Task.interrupt()`를 구분하는 terminal authority로 단독 사용하지 않음 |
| `STORE_IN_ANY_CONTAINER_BRANCH` | `EXISTING` | 기존 evidence 이름 보존; full-run counter의 source로 사용 |
| `STORE_IN_ANY_CONTAINER_PROGRESS_STATE` | `EXISTING` | 기존 capped detail로 유지; 새 full-run aggregate authority로 사용하지 않음 |
| `CONTAINER_TASK_TARGET_DECISION` | `EXISTING` | route correlation field를 additive하게 보강 |
| `CONTAINER_TASK_BRANCH` | `EXISTING` | route correlation field를 additive하게 보강 |
| `CRAFTING_TABLE_ROUTE_RETRY_SUMMARY` | `EXISTING` | semantic fingerprint로 교정하고 unique instance는 counter로 분리 |
| `TASK_CHILD_RECONCILIATION` | `EXISTING_MINING_ONLY` | 현재 payload와 gate가 Mine/Destroy 전용이므로 변경하지 않음 |
| `CONTAINER_OPEN_ATTEMPT_OBSERVED` | `EXISTING` | 기존 `BlockInteractionDiagnostics` observer를 Store operation context에 연결 |
| `CONTAINER_OPEN_RETURN_OBSERVED` | `EXISTING` | 새 interaction event 없이 기존 반환 관측을 Store attempt와 상관 |
| existing `BARITONE_*` events | `EXISTING` | Store context field와 cap-independent aggregate를 보강 |

새 start event인 `STORE_DEPOSIT_OPERATION_START`는 만들지 않는다. 시작
경계는 기존 `DEPOSIT_COMMAND_INVOCATION_DECISION`과
`STORE_IN_ANY_CONTAINER_START`를 같은 `storeOperationId`로 연결해
표현한다.

새로 제안하는 event는 다음과 같다.

```text
STORE_CONTAINER_PARENT_CANDIDATE_DECISION
STORE_TASK_LIFECYCLE_BOUNDARY
STORE_CONTAINER_FILTERED_SEARCH_RESULT
STORE_BLOCK_SCANNER_FILTER_SUMMARY             conditional second-pass event
STORE_CONTAINER_PURSUIT_DECISION
STORE_CONTAINER_TARGET_CALLBACK_DECISION
STORE_TASK_CHILD_RECONCILIATION
STORE_CRAFT_ROUTE_EVALUATION_ENTERED
CRAFTING_TABLE_COST_POLICY_DECISION
CRAFTING_TABLE_INTERACTION_TARGET_DECISION
STORE_CONTAINER_ACCESS_DECISION
STORE_CONTAINER_TRANSFER_DECISION
STORE_CONTAINER_EFFECT_OBSERVATION
STORE_TARGET_STATE_CHANGE
STORE_BARITONE_CONTEXT_BOUND
STORE_BARITONE_LATE_COMPLETION_SUMMARY          conditional post-terminal event
USER_BLOCK_RANGE_NULL_INPUT_OBSERVED          targeted P2 event
BARITONE_PATH_EXCEPTION                       conditional second-pass event
STORE_DEPOSIT_CHECKPOINT_SUMMARY
STORE_DEPOSIT_TERMINAL_SUMMARY
STORE_DEPOSIT_EFFECT_SUMMARY
STORE_BARITONE_OPERATION_SUMMARY
STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY
STORE_DEPOSIT_TERMINAL_GROUP_RESERVE_EXHAUSTED  bounded control fallback
```

## Implementation Status Ledger

이 section은 canonical 설계를 현재 source 수준으로 낮추지 않고, 2026-08-19
첫 partial diagnostics patch batch가 실제로 구현한 범위와 남은 gap을 기록한다. 상세 runtime
증거는 다음 별도 문서가 권위다.

Pre-implementation canonical design snapshot:

```text
bytes: 115,613
SHA-256: 08b9110333d5fc56ce555ef2d6acdbf560d2343fc3e698702d9f6e512e54562f
```

현재 update는 implementation/runtime ledger를 추가하며, 위 snapshot의
canonical completion contract를 partial source 수준으로 완화하지 않는다.

```text
plugins/Minecraft/docs/chatclef-bare-deposit-diagnostics-reproduction-2026-08-19-r1.md
```

현재 구현 판정:

```text
PARTIALLY_IMPLEMENTED_DIAGNOSTICS_ONLY
NOT_REPRODUCTION_COMPLETE
NOT_READY_FOR_BEHAVIOR_FIX
NOT_READY_FOR_MERGE
root cause: still unverified
```

계획된 신규/targeted event 24개 중 source에 event 이름과 emission 경계가
생긴 것은 9개다. 9개 모두 canonical schema, boundedness, correlation 또는
terminal contract가 불완전하므로 `SOURCE_PRESENT_PARTIAL`이다. canonical
contract를 완전히 충족한 신규 event family는 아직 0개다.

| Event or extension | Current status | Runtime observation in R1 | Material limitation |
| --- | --- | --- | --- |
| `DEPOSIT_COMMAND_INVOCATION_DECISION` Store merge | `IMPLEMENTED_EXTENSION_PARTIAL` | observed once | local operation ID는 연결됐지만 전체 canonical operation ledger는 미구현 |
| `STORE_IN_ANY_CONTAINER_START` Store merge | `IMPLEMENTED_EXTENSION_PARTIAL` | observed once | activation/start fields 일부만 구현 |
| `STORE_IN_ANY_CONTAINER_STOP` callback merge | `IMPLEMENTED_EXTENSION_PARTIAL` | not observed before live cutoff | callback-only 의미는 유지하지만 terminal authority 전체는 미구현 |
| `USER_TASK_CHAIN_CANCEL_REQUESTED` correlation | `SOURCE_PRESENT_PARTIAL` | not observed before live cutoff | existing cancel event payload에 exact operation/cancel/root-generation field가 없고 hidden boolean marker만 기록 |
| `STORE_TASK_LIFECYCLE_BOUNDARY` | `SOURCE_PRESENT_PARTIAL` | 256 emitted records | STOP/INTERRUPT placement는 있으나 activation/tombstone/idempotent terminal contract와 bounded coverage가 불완전 |
| `STORE_TASK_CHILD_RECONCILIATION` | `SOURCE_PRESENT_PARTIAL` | 256 emitted records | `ROOT_ROUTE`/`TARGET_ACTION`/`SEARCH_FALLBACK` role을 구분하지 못하고 256 이후 suppression coverage가 없음 |
| `STORE_CONTAINER_TRANSFER_DECISION` | `SOURCE_PRESENT_PARTIAL` | zero before live cutoff | 일부 transfer return만 관찰하며 missing-item/source/destination authority와 canonical enum이 불완전 |
| `STORE_CONTAINER_EFFECT_OBSERVATION` | `SOURCE_PRESENT_PARTIAL` | zero before live cutoff | mutation ID, requested-item filter, ROOT/TARGET dedupe, baseline/net/high-watermark가 없음 |
| `USER_BLOCK_RANGE_NULL_INPUT_OBSERVED` | `SOURCE_PRESENT_PARTIAL` | observed once before the first logged NPE | pre-dereference 관찰은 동작하지만 `lastActiveState()` 기반 operation 연결은 exact immutable binding이 아님 |
| terminal four-summary names | `SOURCE_PRESENT_PARTIAL` | zero because the Store root was still active at cutoff | atomic reserve, idempotence, complete aggregate projection, fallback control event가 없음 |
| parent / filtered search / pursuit / target callback events | `PROPOSED_NOT_IMPLEMENTED` | not available | raw-parent to filtered-child handoff를 증명할 수 없음 |
| craft route entry / cost / interaction target events | `PROPOSED_NOT_IMPLEMENTED` | not available | repeated `return_open_table_task`의 최초 미관측 경계를 좁힐 수 없음 |
| container access and Store-bound open-event merge | `PROPOSED_NOT_IMPLEMENTED` | not available | GUI/cache/access 경계가 Store attempt와 연결되지 않음 |
| Store-bound Baritone context and aggregate | `PROPOSED_NOT_IMPLEMENTED` | not available | generation/result/adoption/NPE를 exact attempt에 연결할 수 없음 |
| `STORE_DEPOSIT_CHECKPOINT_SUMMARY` | `PROPOSED_NOT_IMPLEMENTED` | not available | detail cap 이후 full-run progress를 복원할 수 없음 |
| scanner summary / late Baritone summary / path exception | `CONDITIONAL_DEFERRED` | not available | 앞선 최소 slice로도 gap이 남을 때만 검토 |

Canonical D-slice status mapping:

| Slice | Current status | Test/runtime status | Exact limitation |
| --- | --- | --- | --- |
| D0a context | `PARTIAL` | focused tests missing | operation context와 mutable counters가 canonical focused types로 완전히 분리되지 않음 |
| D0b binding | `PARTIAL` | focused tests missing | task/tracker binding은 있으나 descendant retirement, per-operation cap, eviction coverage 없음 |
| D0c budget | `PARTIAL_NONCANONICAL` | focused cap tests missing | event-name별 256 gate만 있고 5000 session/critical reserve contract 없음 |
| D0d emission | `NOT_IMPLEMENTED` | tests missing | 별도 atomic/reserved emission owner 없음 |
| D0e Baritone context/counter/late summary | `NOT_IMPLEMENTED` | race tests missing | Store attempt와 generation을 bind하지 않음 |
| D0f existing diagnostic-state cleanup | `NOT_IMPLEMENTED` | cleanup tests missing | Store progress/crafting state의 authoritative cleanup 없음 |
| D1 invocation binding | `PARTIAL` | R1 observed | one local operation ID는 발급되지만 full ledger fields가 불완전 |
| D2 explicit cancel provenance | `PARTIAL` | R1 not observed | hidden boolean marker만 있고 exact cancel payload correlation 없음 |
| D3a stop lifecycle/tombstone | `PARTIAL` | descendant records observed; root terminal unobserved | BEGIN/END hook은 있으나 canonical tombstone/idempotence 없음 |
| D3b interrupt lifecycle | `PARTIAL` | R1 not observed | hook은 있으나 activation/resume contract가 불완전 |
| D4a root activation | `PARTIAL` | R1 observed | start merge만 구현 |
| D4b parent/root-state observation | `NOT_IMPLEMENTED` | unavailable | parent candidate와 target-state event 없음 |
| D4c natural terminal observation | `PARTIAL` | R1 not observed | cached finish result를 받지만 canonical atomic finalizer 없음 |
| D4d stop callback snapshot | `PARTIAL` | R1 not observed | callback merge만 구현 |
| D5 valid-container rejection | `NOT_IMPLEMENTED` | unavailable | rejection role/reason aggregate 없음 |
| D6a filtered result | `NOT_IMPLEMENTED` | unavailable | active retained child search result 없음 |
| D6b retained-pursuit validation | `NOT_IMPLEMENTED` | unavailable | validation role/result 없음 |
| D7 scanner summary | `CONDITIONAL_DEFERRED` | unavailable | previous slices 이후 gap이 남을 때만 검토 |
| D8 pursuit | `NOT_IMPLEMENTED` | unavailable | transition/returned action 없음 |
| D8b target callback/reset | `NOT_IMPLEMENTED` | unavailable | actual reference comparison/reset outcome 없음 |
| D9 Task child reconciliation | `PARTIAL_NONCANONICAL` | R1 observed; focused tests missing | generic root/descendant only, canonical role와 null-clear outcome 불완전 |
| D10a-D10c craft route/cost/branch | `NOT_IMPLEMENTED` | unavailable | existing unbound route events만 존재 |
| D11a-D11b craft interaction projections | `NOT_IMPLEMENTED` | unavailable | Store attempt context projection 없음 |
| D12a-D12b interaction binding/merge | `NOT_IMPLEMENTED` | unavailable | existing open events와 operation join 없음 |
| D13 container access | `NOT_IMPLEMENTED` | unavailable | GUI/cache/access decision 없음 |
| D14a target predicate | `PARTIAL` | R1 not observed | actual predicate snapshot 일부만 있고 shared mutation authority 없음 |
| D14b missing-item boundary | `NOT_IMPLEMENTED` | unavailable | missing-item return observation 없음 |
| D14c transfer decision | `PARTIAL_NONCANONICAL` | R1 not observed | 일부 return만, slot identity와 canonical enum 불완전 |
| D14d canFit comparison | `CONDITIONAL_DEFERRED` | unavailable | source authority가 필요할 때만 검토 |
| D15 effect observation | `PARTIAL_NONCANONICAL` | R1 not observed | mutation identity/dedupe/baseline/oracle 불완전 |
| D16 Baritone owners | `NOT_IMPLEMENTED` | unavailable | generation/result/adoption aggregate 없음 |
| D17 null-input observation | `PARTIAL_NONCANONICAL` | R1 observed once | pre-dereference 관찰은 있으나 exact immutable context가 아님 |
| D18 path exception | `CONDITIONAL_DEFERRED` | unavailable | safe existing propagation boundary 미확정 |
| D19 bounded reproduction | `PERFORMED_WITH_INCOMPLETE_PATCH` | live prefix recorded | canonical cap, terminal, effect, and first-failure coverage 미충족 |

이 patch batch는 여러 canonical D-slice와 upstream lifecycle owner를 한 번에
가로질렀다. 위 표는 그 사실을 정당화하거나 이후 slice의 자동 진행을 승인하지
않는다. 앞으로는 `Implementation Slices And Stop Gates`의 한 slice씩 진행하고,
각 slice의 focused test와 종료 보고를 완료한 뒤 다음 승인을 판단한다.

현재 LAVI-owned source 구조는 다음 package를 새로 사용한다.

```text
lavi/minecraft/diagnostics/container/store/deposit/
    StoreDepositDiagnostics.java              compatibility facade/orchestrator
    context/                                   operation identity and aggregate state
    binding/                                   task/tracker binding
    budget/                                    current emission gate
    event/                                     flat event field construction
```

이는 upstream-derived classes를 이동하거나 분할하지 않는다. 다만 현재 facade,
binding, budget, event model은 canonical responsibility and boundedness contract를
완료한 상태가 아니다. 특히 다음 gap은 문서상 merge blocker다.

```text
StoreDepositEmissionGate
    actual: event-name별 session-global distinct key 256, exception signature 16
    missing: total session cap 5000, per-operation family budgets, checkpoints,
             suppression accounting, 4936/64 critical partition

terminal summary group
    actual: four event names를 reserve 없이 순차 직접 방출한 뒤 purge
    missing: atomic four-slot reservation, idempotent finalization,
             terminal/control reserve, reserve-exhausted fallback

binding registry
    actual: descendant Task identity를 strong map에 계속 추가
    missing: per-operation bound, descendant retirement, eviction coverage record

correlation
    actual: NPE를 last active Store operation에 시간상 연결
    missing: immutable generation/task/attempt binding; 추측 연결 실패 시 unbound 처리

effect and coverage semantics
    actual: one effect callback만 있어도 effectObservationComplete=true가 될 수 있음
    missing: complete observation predicate, requested-target filtering,
             durable effect oracle, complete implemented/unimplemented family ledger
```

`max_emission=session_cap=5000` 같은 payload 문구는 실제 gate 구현의 증거로
사용하지 않는다. current source의 gate code와 live emitted/suppressed evidence가
authority다. 새 reproduction에서 lifecycle과 child reconciliation이 각각 256개에서
더 이상 방출되지 않았지만, 새 gate는 별도 cap-reached 또는 suppression summary를
남기지 않았다.

Diagnostics-only source review에서는 return value, Task selection, retry, timeout,
input, Baritone goal/path, click/transfer 또는 원래 NPE propagation을 바꾸는 hunk를
찾지 못했다. 그러나 global synchronized registry/gate와 unbounded descendant
binding의 장시간 성능 및 thread-interleaving 영향까지 검증됐다는 뜻은 아니다.

Authorized verification already completed for this partial slice:

```text
clean forced offline Gradle build: PASS
139 actionable tasks: 139 executed
built 1.20.1 JAR and active CurseForge JAR: byte-for-byte SHA-256 match
runtime code-source path: active LAVI_TEST_Fabric01 mods JAR
BOUNDARY reproduction: observed as a live, non-finalized prefix
focused StoreDeposit helper tests: NOT IMPLEMENTED
terminal summary runtime validation: NOT OBSERVED
```

## Ownership And Responsibility Split

Upstream-derived observation boundaries:

```text
adris.altoclef.commands.DepositCommand
adris.altoclef.commands.BlockScanner
adris.altoclef.chains.UserTaskChain
adris.altoclef.tasks.container.StoreInAnyContainerTask
adris.altoclef.tasks.DoToClosestBlockTask
adris.altoclef.tasks.AbstractDoToClosestObjectTask
adris.altoclef.tasksystem.Task
adris.altoclef.tasks.container.DoStuffInContainerTask
adris.altoclef.tasks.container.DoCraftInTableTask
adris.altoclef.tasks.container.AbstractDoToStorageContainerTask
adris.altoclef.tasks.container.StoreInContainerTask
adris.altoclef.tasks.container.PickupFromContainerTask        conditional only
adris.altoclef.tasks.container.ContainerStoredTracker
adris.altoclef.trackers.UserBlockRangeTracker
```

이 파일들은 일반 refactor 또는 folderization 대상이 아니다. 필요한
경계가 LAVI-owned layer에서 관찰되지 않을 때만 기존 계산 결과를 전달하는
최소 method-level hunk를 검토한다.

별도로 다음은 existing LAVI-owned diagnostic owners다. Upstream Task behavior
owner로 재분류하지 않고, 각 파일의 기존 event 의미와 non-deposit 관측을
보존한 채 focused diagnostic state/correlation hunk만 검토한다.

```text
lavi.minecraft.diagnostics.BlockInteractionDiagnostics
lavi.minecraft.diagnostics.container.ContainerTaskDiagnosticFields
lavi.minecraft.diagnostics.container.ContainerTaskDiagnostics
lavi.minecraft.diagnostics.container.ContainerTaskEmissionLimiter
lavi.minecraft.diagnostics.container.store.StoreInAnyContainerProgressDiagnostics
lavi.minecraft.diagnostics.container.crafting.CraftingTableRouteRetryDiagnostics
lavi.minecraft.diagnostics.mining.baritone.BaritonePathCalculationDiagnostics
lavi.minecraft.diagnostics.mining.baritone.BaritoneProcessControlDiagnostics
```

새 LAVI-owned component는 다음 책임 경계를 사용한다.

```text
lavi/minecraft/diagnostics/container/store/deposit/
    context/
        StoreDepositOperationContext.java
            immutable operation/attempt/target-role correlation snapshot only
        StoreDepositOperationState.java
            current semantic snapshot only
        StoreDepositOperationCounters.java
            fixed-key full-run counters and transition matrices only
        StoreDepositBoundaryLedger.java
            bounded last-success/first-failure/first-unobserved evidence only
        StoreDepositSummaryFactory.java
            immutable checkpoint and terminal projection only
        StoreDepositBoundary.java
            ordered boundary values only

    binding/
        StoreOperationStateRepository.java
            active operation state retention only
        StoreRootStopTombstoneRepository.java
            bounded root STOP BEGIN-to-END tombstone retention only
        StoreTaskChildBindingRegistry.java
            root/descendant/candidate/active-child binding only
        StoreSlotMutationCorrelationRegistry.java
            weak-identity bounded slot-mutation correlation only
        StoreInteractionCorrelationRegistry.java
            bounded interaction-attempt to immutable Store context binding only
        StoreDepositBindingFacade.java
            delegation to the focused binding owners only

    baritone/
        StoreBaritoneGenerationContextRegistry.java
            thread-safe immutable generation context and TTL retention only
        StoreBaritoneGenerationCounters.java
            worker-safe fixed-key generation/result counters only
        StoreBaritoneLateSummaryFactory.java
            immutable bounded late-completion projection only
        StoreBaritoneDiagnosticFacade.java
            delegation to the three focused Baritone diagnostic owners only

    budget/
        StoreDetailBudget.java
            per-operation state-change family budget only
        StoreCheckpointBudget.java
            interval, unchanged-skip, and per-operation checkpoint budget only
        StoreCriticalReserveBudget.java
            thread-safe atomic session terminal/late/exception/control reserve only
        StoreDiagnosticBudgetFacade.java
            delegation to the three focused budget owners only

    emission/
        StoreDepositDiagnosticEmitter.java
            stateless immutable-field serialization through the existing ChatClef logger only
        StoreDepositDiagnosticObserver.java
            route already-computed observations to focused collaborators only
        StoreDepositCancelProvenanceObserver.java
            bind one UserTaskChain cancel invocation to a registered root only
```

각 top-level Java type은 별도 파일을 사용한다. 기존
`lavi.minecraft.diagnostics.container.store` 파일을 이동하거나 재구성하지
않고 새 operation-correlation component만 `deposit` 하위 package로 묶는다.

각 type의 허용 책임은 합치지 않는다.

```text
StateRepository       active operation state retention and removal only
StopTombstoneRepo     root STOP BEGIN-to-END tombstone retention only
TaskChildRegistry     Task/candidate/active-child binding only
SlotRegistry          weak-LRU slot-mutation correlation only
InteractionRegistry   bounded interaction-attempt correlation only
GenerationRegistry    immutable generation/TTL correlation only
BindingFacade         delegation only; owns no retention policy
GenerationCounters    worker-safe fixed-key Baritone aggregate update only
LateSummaryFactory    immutable post-terminal Baritone projection only
BaritoneFacade        delegation only; owns no state or counters
State                 current semantic snapshot update only
Counters              full-run fixed-key aggregate update only
BoundaryLedger        bounded boundary evidence update only
DetailBudget          state-change family emission decision only
CheckpointBudget      checkpoint emission decision only
CriticalBudget        terminal/late/exception/control reserve decision only
BudgetFacade          delegation only; owns no counters or emission
SummaryFactory        immutable summary projection only
Emitter               field serialization and existing logger call only
Observer              delegation only; no behavior decision
CancelObserver        pre-stop user-chain cancel binding only
```

가져서는 안 되는 책임:

```text
branch or target selection
predicate result selection
retry, timeout, blacklist, or fallback policy
Task equality, replacement, completion, or cancellation decision
Baritone goal, path, process, or adoption decision
container interaction or slot selection
tracker acceptance or delta decision
exception recovery or conversion
```

현재 `Task.tick()`의 reconciliation hook은 mining-specific facade를 통해
Mine/Destroy만 관찰한다. Store support를 추가할 때 mining facade에 Store
policy를 직접 섞지 않는다. 하나의 기존 hook에서 focused mining observer와
focused Store observer로 전달하는 LAVI-owned diagnostic router를 평가한다.
`Task.tick()`에는 중복 hook을 여러 개 추가하지 않는다.

## Correlation Identity

Bare/local deposit에는 Fabric WebSocket `request_id`와 `correlation_id`가
없다. 가짜 protocol identifier를 만들거나 wire payload에 field를 추가하지
않는다.

Local diagnostic identity:

| Field | Meaning |
| --- | --- |
| `storeOperationId` | `DepositCommandDiagnostics.logInvocation()`에서 한 번 발급하고 생성된 Store task에 bind하는 local diagnostic ID |
| `storeTaskInstanceId` | 기존 diagnostic task instance identity |
| `candidateDecisionSequence` | Store root가 새 candidate decision을 계산할 때마다 증가하는 sequence |
| `storeDecisionSequence` | Store root의 semantic branch decision마다 증가하는 sequence |
| `branchEpoch` | effective branch가 실제로 바뀔 때만 증가 |
| `storeAttemptId` | candidate가 active child로 install/replace되거나 branch/target epoch가 commit될 때 발급하고 replacement/terminal까지 유지하는 active attempt ID |
| `childLifecycleId` | candidate가 아니라 실제 active child lifecycle에 bind된 ID |
| `routeEvaluationId` | one diagnostic identity issued before the existing cost evaluation and reused by the later target/branch observations |
| `filteredSearchId` | 실제 filtered scanner call identity |
| `pursuitDecisionId` | pursuit retain/select/wander decision identity |
| `interactionAttemptId` | target-specific interaction attempt identity |
| `slotMutationId` | 동일 `SlotClickChangedEvent`를 여러 tracker가 관찰할 때 공유하는 local ID |
| `goalRequestId` | Store-bound Baritone goal request diagnostic ID |
| `calculationGeneration` | 기존 Baritone generation identity |
| `effectObservationVersion` | accepted/rejected slot 또는 tracker effect가 관찰될 때 증가 |
| `expectedDepositEffectVersion` | requested item의 positive delta가 `TARGET_CONTAINER` predicate에 accept될 때만 증가 |
| `inventoryObservationVersion` | target inventory/notStored state가 바뀔 때 증가 |

`slotMutationId`는 같은 event object를 관찰하는 tracker들에 한 번만
발급한다. event object lifetime을 넘겨 보관하지 않고 terminal 또는 bounded
cleanup에서 제거한다.

`DiagnosticEventEmitter`가 자동으로 붙이는 공통 envelope field:

```text
traceId
clientTickId
eventSequence
taskInstanceId
threadName
```

각 Store observer가 payload에 추가하는 operation field:

```text
storeOperationId
storeTaskInstanceId
candidateDecisionSequence
storeDecisionSequence
branchEpoch
storeAttemptId
taskClass
parentTaskInstanceId
targetRole
dimensionId
targetPosition
commandContextAvailable
commandRequestId
commandCorrelationId
```

Bare deposit에서는 마지막 세 command-context field가 기존 의미대로
`false` 또는 blank일 수 있다. blank를 local identity로 채우지 않는다.
Envelope field를 개별 helper가 payload에 다시 직렬화하지 않는다.

Candidate-only event는 `candidateDecisionSequence`를 사용하고 아직 active
attempt가 없으면 `storeAttemptId`를 blank로 둔다. Reconciliation이 candidate를
install/replace한 순간 하나의 active `storeAttemptId`를 bind하며, retained
child와 그 Baritone generation은 그 ID를 유지한다. Tick마다 새 attempt ID를
발급하지 않는다.

`effectObservationVersion`은 negative, rejected, wrong-container 또는
non-requested observation도 구분해 증가할 수 있다. Loop의 useful-effect
판정에는 사용하지 않는다. `expectedDepositEffectVersion`만 requested item의
positive `TARGET_CONTAINER`-accepted delta에서 증가한다. Root-any-container
positive delta는 engine accounting evidence지만 target effect verification과
동일시하지 않는다.

### Canonical State And Target Identity

Target/state hash의 canonical input은 다음 순서로 만든다.

```text
dimension registry ID
requested target/catalogue identity
sorted matching-item registry IDs
requested count
current inventory count
operation-baseline stored count
normalized target role and BlockPos when present
```

`requested target/catalogue identity`는 요청 의미와 catalogue entry를 식별하고,
`sorted matching-item registry IDs`는 그 target에 실제로 대응하는 concrete item
집합을 canonical order로 기록한다. 두 값은 같은 의미로 중복해서 쓰지 않는다.

UTF-8 canonical text에 fixed `SHA-256`을 적용하고 lowercase hex로 기록한다.
`Object.hashCode()`, identity hash, unordered set/map rendering 또는
`toString()`을 state hash로 사용하지 않는다. Position identity와 dedupe
fingerprint에는 반드시 `dimensionId`를 포함해 dimension이 다른 같은 좌표를
합치지 않는다.

## Diagnostics Mode Contract

```text
normal runtime startup: OFF
explicit investigation: BOUNDARY
VERBOSE: not required by this plan; separate explicit authorization only
overlay/HUD: independent from diagnostics mode
```

Full-run aggregate와 registry도 명시적으로 활성화된 BOUNDARY investigation
session에서만 유지한다. Operation 중 mode가 바뀌거나 diagnostic state가
재초기화되면 behavior에는 영향을 주지 않고
`coverageCompleteThroughTerminal=false`와 mode transition을 coverage summary에
기록한다.

## Target Roles

같은 좌표라도 어느 decision boundary에서 나온 값인지 구분한다.

```text
STORE_PARENT_RAW_CONTAINER
STORE_CHILD_FILTERED_CONTAINER
STORE_ACTIVE_PURSUIT
CRAFTING_ROUTE_TARGET
CRAFTING_COST_POLICY_TARGET
CRAFTING_INTERACTION_TARGET
STORAGE_INTERACTION_TARGET
CONTAINER_TRANSFER_TARGET
BARITONE_GOAL_TARGET
```

Target equality는 role을 제거한 value equality와 role을 포함한 semantic
identity를 모두 기록한다. object reference equality는 보조 evidence이며
position value equality를 대신하지 않는다.

## P0: Operation Aggregate And Terminal Coverage

이 단계가 먼저 구현되지 않으면 detail event를 추가해도 cap 이후의
50분 전체를 다시 잃는다.

처리 순서는 항상 다음과 같아야 한다.

```text
observe already-computed behavior result
  -> bind or lookup Store context
  -> update full-run aggregate
  -> update last/first boundary evidence
  -> apply detail emission gate
  -> emit detail or increment suppressed counter
```

Emission gate가 aggregate update보다 먼저 실행되어서는 안 된다.

### Existing Start Events

`DEPOSIT_COMMAND_INVOCATION_DECISION` 추가 field:

```text
storeOperationId
operationOrigin=LOCAL_DEPOSIT_COMMAND
storeTaskInstanceId
explicitItemListProvided
selectedItemTargetCount
initialTargetStateHash
```

`STORE_IN_ANY_CONTAINER_START` 추가 field:

```text
storeOperationId
originBindingAvailable
originBindingSource=DEPOSIT_COMMAND_INVOCATION_DECISION|UNBOUND
startTick
operationStartTick
activationStartTick
storeActivationEpoch
activationKind=INITIAL|RESUME_AFTER_INTERRUPT
initialTargetGroupCount
initialTargetTotalItemCount
initialTargetStateHash
```

Binding이 없으면 다른 operation과 억지로 연결하지 않고
`originBindingAvailable=false`를 남긴다. `Task.interrupt()` 뒤 같은 Store
object가 다시 `onStart()`에 들어오면 새 operation ID와 initial baseline을
발급하지 않고 activation epoch만 증가시킨다.

### `STORE_TASK_LIFECYCLE_BOUNDARY`

Placement:

```text
Task.stop() and Task.interrupt(), at the same already-computed boundaries as
stop_begin/stop_end and interrupt_begin/interrupt_end
outside the existing VERBOSE-only log guard
only when the task is registered to a Store operation
```

기존 generic `ChatClefDiagnostics.logTaskTransition()`은 VERBOSE guard 안에
있으므로 BOUNDARY authority로 재사용할 수 없다. 그 log의 mode/semantics는
바꾸지 않고, 같은 source 지점에서 registered Store-only observer를 guard
밖으로 한 번 호출한다.

Fields:

```text
storeOperationId
storeActivationEpoch
lifecycleAction=STOP|INTERRUPT
lifecyclePhase=BEGIN|END
taskActiveBefore
lifecycleTaskRole=ROOT_STORE|DESCENDANT
interruptTaskInstanceId
interruptTaskClass
onStopCallbackExpected
terminalPending
rootStopTombstoneRetained
operationFinalized
```

`INTERRUPT`는 suspension/checkpoint이며 terminal이 아니다. Operation ID,
baseline, counters와 active-attempt correlation을 유지하고 다음 `onStart()`를
`RESUME_AFTER_INTERRUPT`로 기록한다. 기존
`STORE_IN_ANY_CONTAINER_STOP terminal=true` payload는 callback이 실행됐다는
역사적 필드일 뿐 true lifecycle terminal authority로 사용하지 않는다.
오직 `lifecycleTaskRole=ROOT_STORE`인 true `STOP` 또는 실제 natural finish만
root operation을 finalize한다. Descendant STOP/INTERRUPT는 child lifecycle
counter만 갱신한다.

Root true STOP은 two-phase로 처리한다.

```text
Task.stop() ROOT_STORE BEGIN
    -> terminalPending=true
    -> bounded root-stop tombstone retained

StoreInAnyContainerTask.onStop()
    -> callback snapshot and existing event only
    -> no summary finalization and no registry purge

descendant stop callbacks
    -> remain correlatable through the tombstone

Task.stop() ROOT_STORE END
    -> idempotent terminal summaries finalized
    -> main operation/task/child bindings purged
    -> bounded in-flight Baritone context retained separately
```

Natural finish는 첫 실제 `isFinished()==true` 평가 안에서 즉시 idempotent
finalize하고 operation state를 정리한다. Root STOP END가 관찰되지 않으면
`UNKNOWN_STOP`이나 완료를 추측하지 않고 tombstone expiry 시 coverage를 false로
남긴다. Tombstone은 최대 16개/session, 최대 6000 client ticks로 제한하며,
expiry는 engine cleanup을 호출하지 않는다.

### Explicit User-Chain Cancel Provenance

`EXPLICIT_STOP_CORRELATED`를 offline 추측으로 만들지 않는다.
`UserTaskChain.cancel()`에서 기존 `beginCancel()` 뒤, root stop 앞에 focused
Store observer를 한 번 호출해 다음 이미 계산된 값을 registry에 bind한다.

```text
cancelInvocationId
rootBeforeStopInstanceId
rootAssignmentId
rootGeneration
storeOperationId
rootMatchesRegisteredStore
willCallOnTaskFinish
cancelCallerSummary
```

뒤따르는 root `Task.stop()`이 같은 root/operation/cancel invocation과 일치할
때만 `EXPLICIT_STOP_CORRELATED`를 사용한다. Binding이 없거나 mismatch면
summary는 `UNKNOWN_STOP`이며 offline log correlation만 허용한다.

### Terminal Summary Group

다음 summary는 일반 detail cap과 별도의 reserved budget을 사용한다.

```text
STORE_DEPOSIT_TERMINAL_SUMMARY
STORE_DEPOSIT_EFFECT_SUMMARY
STORE_BARITONE_OPERATION_SUMMARY
STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY
```

Operation당 각 event는 최대 한 번만 확정한다. 기존
`STORE_IN_ANY_CONTAINER_STOP`은 cap-free callback event로 그대로 남긴다.
위 네 event는 같은 operation과 terminal tick에 연결되는 aggregate
companion group이며 lifecycle event를 대체하거나 중복 생성하지 않는다.
Finalizer는 첫 group member를 방출하기 전에 critical reserve 네 칸을 원자적으로
선점한다. 네 칸을 모두 확보하지 못하면 group member는 하나도 방출하지 않고,
최대 한 개의 bounded control/coverage record에 누락을 집계한다. Partial terminal
group은 허용하지 않으며, 예약 과정은 engine thread를 기다리게 하거나 engine
lifecycle을 변경하지 않는다.

그 fallback record의 canonical event는
`STORE_DEPOSIT_TERMINAL_GROUP_RESERVE_EXHAUSTED`다. 이것은 네 summary group의
member가 아니며 terminal sub-reserve를 사용하지 않는다. Operation당 최대 한 번,
session control reserve에서 최대 8번만 방출한다.

```text
summaryGroupId
storeOperationId
storeTaskInstanceId
terminalTrigger
reason=TERMINAL_SUMMARY_RESERVE_EXHAUSTED
requestedTerminalSummarySlots=4
reservedTerminalSummarySlots=0
terminalSummaryGroupEmitted=false
terminalSummaryGroupSuppressedCount
terminalSummaryReserveUsed
terminalSummaryReserveLimit
controlReserveUsedBefore
controlReserveLimit
coverageComplete=false
```

Control reserve마저 소진됐으면 새 bypass event를 만들지 않고 fixed
`terminalSummaryGroupSuppressedCount`와 in-memory coverage=false만 갱신한다.
Reserve가 모두 소진된 뒤 이 사실의 추가 log emission은 보장하지 않으며, 어떤
경우에도 engine behavior에 사용하지 않는다.
`isFinished()`를 추가 호출하지 않는다. 해당 method의 기존 실제 평가 결과가
처음 true가 된 호출 안에서 idempotent summary finalizer를 실행한다. 현재
`SingleTaskChain` natural-finish 경로는 old root에 `stop()`을 보장하지 않기
때문이다. True root STOP은 `Task.stop()` BEGIN에서 pending으로 표시하고,
`onStop()`에서는 snapshot만 취한 뒤 `Task.stop()` END에서 summary와 cleanup을
확정한다. `Task.interrupt()`의 `onStop()` callback은 summary를 확정하거나
registry를 지우지 않는다. Natural finish 또는 root STOP END 중 먼저 온
권위 있는 경계가 summary를 한 번만 확정한다.

`STORE_DEPOSIT_TERMINAL_SUMMARY` fields:

```text
summaryGroupId
storeOperationId
storeTaskInstanceId
startTick
endTick
durationTicks
durationMillis
terminalTrigger
naturalFinishObserved
lastFinishResult
lastFinishResultTick
activationCount
interruptCount
resumeCount
trueStopObserved
explicitStopCorrelated
interruptTaskInstanceId
interruptTaskClass
finalActiveChildInstanceId
finalActiveChildClass
totalParentDecisionCount
totalTargetCallbackCount
targetCallbackReferenceMismatchCount
progressResetReturnedNormallyCount
branchCountByType
branchTransitionMatrix
branchEpochCount
openObtainOpenCycleCount
longestNoEffectCycleCount
unreachableRequestPlannedCount
unreachableRequestReturnedNormallyCount
filteredSearchResultCounts
rawAndFilteredRelationCounts
filteredSearchPredicateRejectionReasonCounts
pursuitValidationPredicateRejectionReasonCounts
scannerFilterReasonCountsWhenAvailable
scannerFilterCoverageGapCount
pursuitValidationReasonCounts
pursuitTransitionCounts
pursuitNextActionCounts
reconciliationOutcomeCountsByRole
craftRouteEvaluationEnteredCount
craftCostPolicyReasonCounts
craftRouteTargetOutcomeCounts
craftRouteBranchDecisionCounts
cursorReturnBranchCounts
craftInteractionRelationCounts
craftInteractionNextActionCounts
craftScreenHandlerMatchedCount
containerAccessActionCounts
containerAccessFailureReasonCounts
transferActionCounts
transferSelectionCoverageGapCounts
lastSuccessfulBoundary
lastSuccessfulBoundaryAttemptId
lastSuccessfulBoundaryBranch
lastSuccessfulBoundaryTargetRole
lastSuccessfulBoundaryTargetDimension
lastSuccessfulBoundaryTargetPosition
firstExplicitFailureBoundary
firstFailureAttemptId
firstFailureBranch
firstFailureTargetRole
firstFailureTargetDimension
firstFailureTargetPosition
firstFailureAttemptLastSuccessfulBoundary
firstUnobservedBoundaryAfter
lastObservedAttemptId
lastObservedAttemptBranch
lastObservedAttemptBoundary
diagnosticClassification
```

`durationTicks`가 gameplay elapsed의 권위 있는 값이다. `durationMillis`는
operation 시작과 terminal의 `System.nanoTime()` 또는 기존 단조 시간 source로
계산한 diagnostic elapsed이며 wall-clock/time-of-day가 아니다. 어느 값도
behavior, timeout 또는 terminal 판정에 사용하지 않는다.

위 aggregate map은 fixed enum/role/reason domain만 사용한다. Detail/checkpoint
emission이 거부돼도 observation 직후 먼저 증가하고, terminal summary가 그
full-run 값을 그대로 투영한다. Position, task identity, exception message 또는
동적 문자열을 map key로 쓰지 않는다. Conditional scanner/transfer detail이
없으면 0으로 꾸미지 않고 별도 coverage-gap count를 남긴다. Effect 분포는
`STORE_DEPOSIT_EFFECT_SUMMARY`, Baritone/exception 분포는
`STORE_BARITONE_OPERATION_SUMMARY`, emission completeness는 coverage summary가
각각 소유한다.

Ledger는 unbounded attempt history를 보관하지 않는다. Operation 최초 explicit
failure snapshot과 현재/마지막 attempt snapshot만 보존한다. 이후 attempt의
success가 최초 failure의 attempt/branch/target context를 덮어쓰지 않는다.

Allowed `terminalTrigger` values:

```text
NATURAL_FINISH_OBSERVED
EXPLICIT_STOP_CORRELATED
STOPPED_WITH_REPLACEMENT_TASK
UNKNOWN_STOP
```

현재 plan에는 active operation을 증명해 flush하는 별도 shutdown hook이
없으므로 `CLIENT_SHUTDOWN_WHILE_ACTIVE`를 추측하지 않는다. 그런 trigger는
정확한 existing shutdown boundary가 별도 audit으로 증명된 뒤에만 추가한다.

`interruptTask=null`만으로 `STOP_COMMAND`를 추측하지 않는다. 기존
UserTaskChain cancel/stop ledger와 같은 tick/operation으로 상관된 경우에만
`EXPLICIT_STOP_CORRELATED`를 사용한다.

`STORE_DEPOSIT_EFFECT_SUMMARY` fields:

```text
effectObservationVersion
expectedDepositEffectVersion
inventoryObservationVersion
uniqueSlotMutationCount
trackerObservationCountByRole
acceptedPositiveDeltaCountByTrackerRole
acceptedNegativeDeltaCountByTrackerRole
acceptedZeroDeltaCountByTrackerRole
predicateAcceptedCountByTrackerRole
predicateRejectedCountByTrackerRole
sameMutationObservedByBothRolesCount
rootGrossPositiveDeltaByItemSorted
rootGrossNegativeDeltaByItemSorted
rootNetDeltaByItemSorted
rootPositiveHighWatermarkByItemSorted
targetTrackerGrossPositiveDeltaByItemSorted
targetTrackerGrossNegativeDeltaByItemSorted
targetTrackerNetDeltaByItemSorted
trackerDecisionMismatchCount
initialTargetStateHash
finalTargetStateHash
targetStateReturnCount
rootTargetAdvanceCount
finalNotStoredGroupCount
finalNotStoredTotalItemCount
effectObservationComplete
effectVerification
```

Item map은 item ID 순으로 정렬하고 requested target items만 포함한다. Tracker delta는
`SlotClickChangedEvent`를 통한 engine observation이지 서버에 영구 저장된
deposit의 증명이 아니다. 따라서 `effectObservationComplete`와
`effectVerification`을 분리하고, 관측만으로 성공을 추측하지 않는다.

Allowed `effectVerification` values:

```text
NOT_VERIFIED
EXPECTED_TRANSIENT_EFFECT_OBSERVED
VERIFIED_BY_AUTHORIZED_DURABLE_ORACLE
```

이 plan에는 server-durable inventory/container oracle이 없으므로 현재
diagnostics만으로는 `VERIFIED_BY_AUTHORIZED_DURABLE_ORACLE`을 사용할 수 없다.
Requested item의 positive target-container delta가 있으면 최대
`EXPECTED_TRANSIENT_EFFECT_OBSERVED`다. `effectObservationComplete=true`는
operation baseline, ROOT/TARGET tracker binding, relevant SlotClick callback,
mode continuity, aggregate registry, terminal snapshot이 모두 누락 없이
관찰되고 eviction/cap coverage gap이 없을 때만 가능하다. 하나라도 불완전하면
false로 남긴다.

`STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY` fields:

```text
firstObservedTickByFamily
lastObservedTickByFamily
firstDetailTickByFamily
lastDetailTickByFamily
detailEmittedByFamily
detailSuppressedByFamily
detailCapReachedByFamily
checkpointEmittedCount
terminalSummaryEmittedByType
unboundChildCount
unboundBaritoneGenerationCount
lastDetailGeneration
lastAggregatedGeneration
coverageCompleteThroughTerminal
observedCountByFamily
truncatedSampleCountByFamily
missingBoundaryCountByType
fullRunAggregateProjectionCompleteByFamily
exceptionAggregateCoverageComplete
modeAtOperationStart
modeTransitionCount
registryEvictionCountByType
sessionCapReached
criticalReserveUsedByType
```

`coverageCompleteThroughTerminal=false`이면 event 부재를 runtime failure로
해석하지 않는다.

## P0: Parent Candidate And Root State

### `STORE_CONTAINER_PARENT_CANDIDATE_DECISION`

Placement:

```text
StoreInAnyContainerTask.onTick()
at the early GET_MISSING_TARGET return with closestEvaluated=false
otherwise after closest, closestWithinRange, and currentTryWithinExtraRange
are computed and before the selected branch returns a child
```

Fields:

```text
rawClosestPresent
rawClosestPosition
rawClosestBlockType
rawClosestScannerSource=UNFILTERED_BY_STORE_PREDICATE
closestEvaluated
closestWithin50Evaluated
closestWithin50
currentChestTryBefore
currentTryWithin70Evaluated
currentTryWithin70
rawClosestEqualsCurrentTryByValue
selectedBranch
rangeDecisionOutcome
fallbackItemOutcome
progressCheckEvaluated
progressCheckOk
unreachableRequestPlanned
unreachableRequestTarget
unreachableRequestReturnedNormally
notStoredEvaluated
notStoredTargetGroupCount
notStoredTotalItemCount
notStoredStateHash
inventoryObservationVersion
effectObservationVersion
expectedDepositEffectVersion
```

Allowed `rangeDecisionOutcome` values:

```text
NOT_EVALUATED_EARLY_GET_MISSING_TARGET
RAW_CLOSEST_WITHIN_50
CURRENT_TRY_WITHIN_70
BOTH_RANGE_CONDITIONS
NO_RAW_CLOSEST
RAW_PRESENT_BUT_OUTSIDE_RANGES
```

Allowed `fallbackItemOutcome` values:

```text
NOT_EVALUATED_OPEN_EXISTING
HAS_CONTAINER_BLOCK_ITEM
OBTAIN_CONTAINER_ITEM
GET_MISSING_TARGET
```

현재 fallback event처럼 `closest=null`을 전달한 뒤 scanner absence로
해석하지 않는다. 실제 local `closest`와 이미 계산된 range booleans를
보존한다. 거리값은 보조 field일 뿐 branch input authority는 기존 boolean
결과다.

`UNFILTERED_BY_STORE_PREDICATE`는 Store의 `validContainer` predicate를 쓰지
않았다는 뜻일 뿐이다. BlockScanner의 block-state 및 unreachable filtering은
여전히 적용된다. `closestEvaluated=false`인 early branch에서는 raw/range
value를 false나 null로 꾸미지 않고 `UNAVAILABLE`로 기록한다.
같은 early `GET_MISSING_TARGET` 경로는 `notStored` 계산 전이므로
`notStoredEvaluated=false`이며 count/hash도 `UNAVAILABLE`이다. 이를 0 또는
빈 hash로 꾸미지 않는다.

`unreachableRequestPlanned`는 void call 직전 관측일 뿐 실행 증명이 아니다.
같은 target으로 기존 `requestBlockUnreachable(..., 2)`가 정상 return한 직후
bounded aggregate의 `unreachableRequestReturnedNormally`를 갱신한다. Exception
handling이나 call ordering은 바꾸지 않는다.

Immediate detail conditions:

```text
first decision
selected branch changed
raw closest value changed
50/70 range boolean changed
raw/currentTry value equality changed
progressCheckOk changed
notStored, effect-observation, or expected-effect version changed
```

나머지 tick은 counter만 갱신한다.

### `STORE_TARGET_STATE_CHANGE`

Placement:

```text
StoreInAnyContainerTask.onTick()
immediately after the existing notStored calculation
```

Fields:

```text
changedTarget
previousInventoryCount
currentInventoryCount
previousNotStoredCount
currentNotStoredCount
rootTrackedStoredCount
rootPositiveDepositTotal
rootNetContainerDelta
rootPositiveDepositHighWatermark
cursorItem
cursorCount
inventoryObservationVersion
effectObservationVersion
expectedDepositEffectVersion
changeClassification
```

Allowed classifications:

```text
INVENTORY_COUNT_DECREASED
INVENTORY_COUNT_INCREASED
TARGET_COUNT_CLAMP_CHANGED
TRACKED_STORAGE_PROGRESS
STATE_RETURNED_TO_PREVIOUS_VALUE
```

중간 event에는 바뀐 target만 기록한다. 전체 target snapshot은 start,
checkpoint의 bounded hash/count, final summary에만 기록한다.
`notStored` 감소가 cursor, crafting, placement, 또는 다른 inventory 변화를
통해 발생할 수 있으므로 `STORE_TARGET_STATE_CHANGE`만으로 deposit effect를
확정하지 않는다.

## P1: Filtered Search And Pursuit

### `STORE_CONTAINER_FILTERED_SEARCH_RESULT`

Placement:

```text
DoToClosestBlockTask.getClosestTo()
around the one existing filtered getNearestBlock call
only when the task is registered as STORE_CHILD_FILTERED_CONTAINER
```

각 actual scanner call은 detail 방출 여부와 무관하게 full-run aggregate에 한
번 반영한다. Detail event는 새 semantic state, 첫 rejection reason 또는 첫
명시적 failure에서만 방출하며 scanner call마다 방출하지 않는다.

Fields:

```text
filteredSearchId
activeRouteChildInstanceId
activeRouteChildCreationSequence
activeRouteChildOriginatingCandidateDecisionSequence
activeRouteChildBoundNotStoredStateHash
originatingParentStoreDecisionSequence
searchOrigin
targetBlockKinds
originatingParentRawClosestPresent
originatingParentRawClosestPosition
filteredResultPresent
filteredResultPosition
rawAndFilteredRelation
candidateEvaluationCount
predicateAcceptedCount
predicateRejectedCount
rejectBlockedAboveUnbreakableCount
rejectContainerCacheFullCount
rejectCachedDungeonCount
rejectSpawnerNearbyCount
scannerFilterDetailAvailable
scannerFilterCoverageGap
firstRejectedPosition
firstRejectedReason
lastRejectedPosition
lastRejectedReason
rejectionExampleOverflowCount
```

이 field는 이번 root tick에서 생성됐다가 `isEqual()`로 폐기된 candidate가
아니라 실제 filtered scan을 수행한 retained/active `DoToClosestBlockTask`와 그
생성 시점의 parent decision/raw candidate/`notStored` capture를 가리킨다. 최신
discarded candidate 값을 active scan에 붙이지 않는다.

Allowed relations:

```text
SAME_POSITION
DIFFERENT_POSITION
RAW_PRESENT_FILTERED_NONE
RAW_NONE_FILTERED_PRESENT
BOTH_NONE
```

Rejection reason은 `DoToClosestBlockTask`만으로 모두 알 수 없다. 실제
`StoreInAnyContainerTask`의 `validContainer` predicate가 이미 평가하는
다음 결과를 collector에 기록한다.

```text
CHEST_ABOVE_BLOCKED_UNBREAKABLE
CONTAINER_CACHE_FULL
CACHED_DUNGEON_CHEST
SPAWNER_NEAR_CHEST
```

`DoToClosestBlockTask.getClosestTo()`가 직접 보장할 수 있는 것은 Store
predicate rejection aggregate와 final result다. BlockScanner는 그 안에서
block-state mismatch와 scanner-unreachable filtering도 수행하지만 이
결과는 현재 caller에 노출되지 않는다. 반면
`CHUNK_UNLOADED_ASSUME_VALID`은 scanner candidate filtering이 아니라
`DoToClosestBlockTask.isValid()`의 retained-pursuit 재검증 경계다. 이를 이
event에 섞지 않는다.

같은 local `validContainer` lambda가 두 caller에서 사용되므로 collector는
evaluation role을 분리한다.

```text
FILTERED_SEARCH
    DoToClosestBlockTask.getClosestTo()의 existing scanner predicate evaluation

PURSUIT_VALIDATION
    DoToClosestBlockTask.isValid()의 existing retained-pursuit predicate evaluation
```

각 caller는 기존 scanner/predicate 호출 직전에 diagnostic role만 bind하고
exception-transparent `try/finally`에서 반드시 clear한다. Normal-completion flag는
기존 호출이 정상 반환한 뒤에만 true가 되며, `finally`는 false인 role bucket의
coverage만 false로 갱신한다. 이 `finally`는 diagnostic role bookkeeping만
정리하고 원 exception을 catch, suppress, convert 또는 replace하지 않는다.
Predicate를 두 번 호출하거나 return/call count를 바꾸지 않는다.
`STORE_CONTAINER_FILTERED_SEARCH_RESULT`의 predicate count/reason은
`FILTERED_SEARCH` bucket만 투영하고, `PURSUIT_VALIDATION` bucket은
`STORE_CONTAINER_PURSUIT_DECISION`의 validation fields/reason으로만 투영한다.
Role을 증명할 수 없으면 두 bucket 중 하나로 추측하지 않고 `UNAVAILABLE` 및
coverage gap으로 둔다.

금지 사항:

```text
getNearestBlock() second call for logging
validContainer.test() second call for logging
candidate per-event emission
predicate return-value modification
exception catching around scanner or predicate
```

Collector는 실제 predicate evaluation 동안 counter와 bounded first/last
example만 보관한다. reason을 안전하게 얻을 수 없으면 `UNKNOWN`을 기록하고
state를 재조회하지 않는다.

### Conditional `STORE_BLOCK_SCANNER_FILTER_SUMMARY`

Store predicate aggregate와 final result만으로 최초 탈락 경계를 구분할 수
없을 때만 별도 slice로 검토한다.

Placement:

```text
BlockScanner.getNearestBlock(Block, Predicate<BlockPos>, Vec3d)
inside the one existing candidate loop
aggregate into the caller's filteredSearchId without per-candidate emission
```

Fields:

```text
filteredSearchId
blockKind
trackedCandidateCount
blockStateMismatchCount
storePredicateEvaluatedCount
storePredicateRejectedCount
scannerUnreachableEvaluatedCount
scannerUnreachableCount
eligibleCandidateCount
selectedPosition
sampledRejectedPositions
sampleOmittedCount
```

Existing short-circuit order를 그대로 보존한다. Store predicate가 false이면
그 candidate에서 `isUnreachable()`을 진단 때문에 추가 호출하지 않는다.
BlockScanner observer가 없으면 해당 count를 0으로 꾸미지 않고
`scannerFilterDetailAvailable=false`와 coverage gap을 남긴다.

### `STORE_CONTAINER_PURSUIT_DECISION`

Placement:

```text
AbstractDoToClosestObjectTask.onTick()
after existing pursuit validation/selection
before invoking getGoalTask(), getWanderTask(), or returning null
only for a Store-registered task
```

Fields:

```text
filteredSearchId
pursuitDecisionId
currentlyPursuingBefore
filteredCandidate
currentlyPursuingAfter
pursuitValidationEvaluated
pursuitValidationResult
pursuitValidationReason
pursuitChunkLoadedEvaluated
pursuitChunkLoaded
pursuitPredicateEvaluated
pursuitPredicateResult
pursuitPredicateEvaluationRole=PURSUIT_VALIDATION
pursuitPredicateRejectionReason
pursuitBlockMatchEvaluated
pursuitBlockMatchResult
heuristicCachePresent
heuristicComparisonEvaluated
oldHeuristicIsBetter
gotConsiderablyCloser
pursuitTransition
nextAction
```

Allowed validation reasons:

```text
CHUNK_UNLOADED_ASSUME_VALID
STORE_PREDICATE_REJECTED
BLOCK_CHECK_FAILED_REASON_UNAVAILABLE
VALID
UNAVAILABLE
```

Allowed transitions:

```text
RETAIN_CURRENT_PURSUIT
SELECT_NEW_PURSUIT
RETRY_OLD_PURSUIT
INVALIDATE_PURSUIT
NO_TRANSITION
```

Allowed next actions:

```text
INVOKE_GOAL_CALLBACK
RETURN_WANDER_TASK
RETURN_NULL_WAIT
```

한 tick에 validation, transition, next action이 함께 일어날 수 있으므로 하나의
`decision` scalar로 합치지 않는다. Short-circuit로 평가되지 않은 값은
false가 아니라 `...Evaluated=false`와 `UNAVAILABLE`로 남긴다.
`isBlockAtPosition()` false는 caller에서 block mismatch와 its internal
unreachable/chunk checks를 분리할 수 없으므로
`BLOCK_CHECK_FAILED_REASON_UNAVAILABLE`보다 강하게 분류하지 않는다.

이 generic upstream boundary는 separate minimal-hunk review가 필요하다.
Parent/filtered evidence만으로 실패 경계를 증명할 수 있으면 이 second-pass
hunk를 추가하지 않는다.

### `STORE_CONTAINER_TARGET_CALLBACK_DECISION`

이 event는 사건 가설에서 중요한 실제 reference-comparison 경계를 관찰한다.
Parent raw closest와 `_currentChestTry`의 reference 비교가 아니라,
`AbstractDoToClosestObjectTask.onTick()`이 pursuit를 commit한 뒤
`StoreInAnyContainerTask.onTick()`에서 만든 target callback을 실제 호출할 때,
filtered child target `blockPos`와 실행되는 기존 `_currentChestTry != blockPos`
비교가 행동 authority다. Pursuit가 없고 wander/null 경로라면 callback은
호출되지 않으며 이 경계는 `NOT_APPLICABLE`이다.

Placement:

```text
StoreInAnyContainerTask.onTick()
inside the existing goal/target callback
at the existing _currentChestTry != blockPos comparison
before and after the existing progress reset/current-try assignment block
```

Fields:

```text
storeOperationId
storeAttemptId
filteredSearchId
activeRouteChildInstanceId
activeRouteChildCreationSequence
activeRouteChildOriginatingCandidateDecisionSequence
activeRouteChildBoundNotStoredStateHash
filteredTargetPosition
currentTryBefore
filteredTargetSameReferenceAsCurrentTry
filteredTargetEqualsCurrentTryByValue
progressResetBecauseReferenceChanged
progressResetReturnedNormally
currentTryAfter
returnedTargetTaskInstanceId
returnedTargetTaskClass
```

Callback field는 이번 tick에 새로 만들어졌지만 equal 판정으로 폐기될 수 있는
candidate가 아니라, 실제 tick 중인 active `DoToClosestBlockTask`와 그 lambda가
생성 시 capture한 `notStored` snapshot을 가리킨다. 그래야 retained child의 stale
capture 가설을 검증할 수 있다.

기존 reference 비교 결과를 local boolean으로 정확히 한 번 캡처하고, 기존
분기에도 같은 boolean을 그대로 사용한다. `progressResetReturnedNormally`는
기존 reset 호출이 정상 return한 뒤에만 counter로 갱신한다. 새 비교로 behavior
분기를 만들거나 progress checker를 다시 호출하지 않고, 예외를 catch하지도
않는다. Parent event의 raw/value equality는 보조 correlation일 뿐 이 callback
결과를 대신하지 않는다.

Reference가 같아 reset을 호출하지 않은 경우
`progressResetReturnedNormally=UNAVAILABLE`이다. Reset이 정상 return하면 true로
관찰하고, exception을 catch해 false event로 바꾸지 않는다.

Immediate detail은 첫 callback, reference-result 변화, filtered target 변화,
progress reset 발생에만 방출한다. 모든 callback은 bounded fixed counter에 먼저
반영한다. 이 경계가 관찰되면 attempt ladder의
`TARGET_CALLBACK_COMMITTED`가 advance한다.

## P1: Child Reconciliation

현재 mining용 `TASK_CHILD_RECONCILIATION`은 position/blacklist 중심의 별도
schema와 Mine/Destroy hard gate를 가진다. 이를 Store payload로 넓히면 한
event 이름에 이질적인 schema가 섞이고 mining facade가 Store policy를
소유하게 된다. 따라서 같은 기존 `Task.tick()` capture point에서
LAVI-owned diagnostic router가 focused Store observer를 호출하고, Store 쪽은
새 `STORE_TASK_CHILD_RECONCILIATION` event를 사용한다.

현재 `Task.tick()`에는 이미 다음 값이 한 위치에 모여 있다.

```text
newSub.isEqual(sub)
canBeInterrupted(sub, newSub)
replacementApplied
previousChildStopCalled
active child before and after
candidate discarded because equal
```

Store-bound fields:

```text
parentTaskInstanceId
parentTaskClass
reconciliationRole
candidateDecisionSequence
candidateNotStoredStateHash
activeChildBeforeInstanceId
activeChildBeforeClass
activeChildCreationSequence
activeChildBoundNotStoredStateHash
candidateChildInstanceId
candidateChildClass
activeChildAfterInstanceId
activeChildAfterClass
isEqualResult
canInterruptEvaluated
canInterruptPreviousChild
replacementApplied
previousChildStopCalled
candidateDiscardedBecauseEqual
reconciliationOutcome
```

Allowed reconciliation roles:

```text
ROOT_ROUTE
TARGET_ACTION
SEARCH_FALLBACK
DESCENDANT_OTHER
```

`ROOT_ROUTE`는 `StoreInAnyContainerTask`가 반환한 route/acquisition child의
설치·retention을 뜻한다. `TARGET_ACTION`은 그 active route child가 filtered
target callback 또는 crafting/container route에서 반환한 action child를
뜻한다. No-pursuit wander 경로는 `SEARCH_FALLBACK`으로 분리한다. 이 층을
합쳐 하나의 `CHILD_RECONCILED` boundary로 쓰지 않는다.

Allowed outcomes:

```text
NEW_CHILD_INSTALLED
EQUAL_CANDIDATE_DISCARDED
PREVIOUS_CHILD_REPLACED
PREVIOUS_CHILD_RETAINED_UNINTERRUPTIBLE
```

Full-run counters:

```text
candidateChildObservedCount
candidateChildCreatedSequenceCount
activeChildInstalledCount
reconciliationRoleCounts
activeChildInstanceSampleCount
activeChildInstanceOmittedCount
equalCandidateDiscardCount
replacementAppliedCount
previousChildStopCount
uninterruptibleRetentionCount
```

Semantic fingerprint:

```text
storeOperationId
+ parentTaskClass
+ reconciliationRole
+ activeChildBeforeClass
+ candidateChildClass
+ reconciliationOutcome
```

Candidate/task instance ID, tick, timestamp, object `toString()`은 fingerprint에
포함하지 않는다. 기존 reconciliation hook은 child `onStart()` 자체를
관찰하지 않으므로 `started`라고 표현하지 않는다. Install count는
reconciliation outcome에서 계산하고 instance identity는 bounded sample과
omitted count로만 유지한다.

`StoreInAnyContainerTask`는 tick마다 새 `DoToClosestBlockTask` candidate를
만들 수 있고, 현재 `DoToClosestBlockTask.isEqual()`은 target block 종류만
비교한다. 따라서 equal candidate가 폐기될 때 active child가 최초 생성
시점의 `notStored` lambda snapshot을 계속 보유하는지 위 네 sequence/hash
field로 관찰한다. 이는 stale-capture 가설을 검증할 뿐이며 `isEqual()`, lambda,
child replacement를 변경하지 않는다.

## P1: Crafting-Table Route Triad

현재 서로 다른 세 후보를 같은 target이라고 가정하지 않는다.

```text
DoStuffInContainerTask route nearest
DoCraftInTableTask cost-policy nearest
openTableTask filtered interaction nearest
```

실제 한 `DoStuffInContainerTask.onTick()` evaluation의 순서는 다음과 같다.

```text
issue diagnostic-only routeEvaluationId
STORE_CRAFT_ROUTE_EVALUATION_ENTERED
getCostToMakeNew()
  -> CRAFTING_TABLE_COST_POLICY_DECISION
CONTAINER_TASK_TARGET_DECISION
CONTAINER_TASK_BRANCH, including the two proposed post-click null-return reasons
```

`routeEvaluationId`는 cost call 직전에 발급해 같은 task/tick의 이미 계산된
cost, target decision, final branch를 묶는 local diagnostic identity다. Cost를
다시 계산하거나 나중 branch 결과를 앞당겨 추측하지 않는다.

### `STORE_CRAFT_ROUTE_EVALUATION_ENTERED`

`DoCraftInTableTask` 또는 nested acquisition task가 active라는 사실만으로 cost
path 진입을 추측하지 않는다. `CraftInTableTask.onTick()`과
`DoStuffInContainerTask.onTick()`에는 output/collect/place/post-place/open-container
early return이 있으므로 다음 exact boundary에서만 cost ladder를 활성화한다.

Placement:

```text
DoStuffInContainerTask.onTick()
after all existing early returns before route cost evaluation
immediately before the one existing getCostToMakeNew() call
only for an exact Store-bound crafting-table route
```

Fields:

```text
storeOperationId
storeAttemptId
routeEvaluationId
routeTaskInstanceId
routeTaskClass
nearestPresent
nearestPosition
costToWalk
hasContainerBlockItem
placeTaskActive
placeTaskFinished
```

이 event 뒤 existing cost call을 그대로 실행한다. Cost method를 다시 부르거나
early return을 우회하지 않는다. Event가 없고 early-return evidence가 있으면
cost boundary는 `NOT_APPLICABLE`이며 missing/failure로 세지 않는다.

### Extend `CONTAINER_TASK_TARGET_DECISION` And `CONTAINER_TASK_BRANCH`

Common additive fields known at `CONTAINER_TASK_TARGET_DECISION` time:

```text
storeOperationId
storeDecisionSequence
storeAttemptId
routeEvaluationId
routeTaskInstanceId
routeTaskClass
routeNearestSource
routeNearestPresent
routeNearestPosition
routeNearestDistanceHeuristic
routeNearestCachedBefore
costToWalk
costToMakeNew
placeForceElapsedBeforeReset
justPlacedElapsed
placeTaskActive
placeTaskFinished
placeTaskPlaced
routeOutcomeFinalized=false
```

Branch-only fields, populated from values actually evaluated before that
specific existing return:

```text
routeNearestCachedAfterEvaluated
routeNearestCachedAfter
placeForceResetEvaluated
placeForceResetThisTick
selectedRouteBranch
branchReturnKind
```

Target-decision 시점에는 cache-after, reset 여부, selected branch가 아직
계산되지 않았다. 이를 false, old cache 또는 예상 branch로 채우지 않는다.
`CONTAINER_TASK_BRANCH`에서 short-circuit 때문에 값이 평가되지 않았다면
corresponding `...Evaluated=false`와 `UNAVAILABLE`을 사용한다.

`routeNearestSource` values:

```text
BLOCK_SCANNER
OVERRIDE_CONTAINER_POSITION
PLACE_TASK_PLACED
NONE
```

`CONTAINER_TASK_TARGET_DECISION` 뒤에는 현재 두 cursor-click null return이
`CONTAINER_TASK_BRANCH`를 남기지 않는다. 같은 existing event 이름을 다음 두
경계에 bounded/additive로 보강한다.

```text
after the existing throwaway click returns, before existing null return
    reason=return_after_cursor_throwaway_click
    decision=RETURN_NULL_AFTER_CURSOR_THROWAWAY_CLICK

after the existing move click returns, before existing null return
    reason=return_after_cursor_move_click
    decision=RETURN_NULL_AFTER_CURSOR_MOVE_CLICK
```

이미 계산된 cursor stack, selected slot, `routeEvaluationId`만 기록한다. Click을
다시 호출하거나 return을 바꾸거나 새 catch를 추가하지 않는다. 이 두 event가
아직 구현되지 않은 상태에서 target-decision 뒤 branch가 없으면 explicit
failure로 분류하지 않고 branch coverage gap으로 둔다. Cursor branch가
관찰되면 그 tick의 target-action child reconciliation은 `NOT_APPLICABLE`이다.

### `CRAFTING_TABLE_COST_POLICY_DECISION`

Placement:

```text
DoCraftInTableTask.getCostToMakeNew()
after the existing closestCraftingTable local is computed
at each existing return boundary
```

Fields:

```text
routeEvaluationId
doCraftTaskInstanceId
costPolicyNearestPresent
costPolicyNearestPosition
costPolicyWithin40
logsCheckEvaluated
logsCheckResult
planksCheckEvaluated
planksCountWhenEvaluated
costPolicyReason
returnedCostToMakeNew
returnedCostInfinite
routeNearestPosition
routeNearestEqualsCostPolicyNearest
```

Allowed reasons:

```text
TABLE_WITHIN_40
LOG_OR_PLANKS
FALLBACK_100
```

`getCostToMakeNew()` 또는 scanner를 추가 호출하지 않는다. Existing local
value와 existing return value만 관찰한다. 기존 `has LOG || plankCount >= 4`
short-circuit를 그대로 따라, log가 true면 진단을 위해 plank count를 추가로
조회하지 않는다. `Infinity`는 exception이 아니라 의도된 existing policy
sentinel로 기록한다.

### `CRAFTING_TABLE_INTERACTION_TARGET_DECISION`

Placement:

```text
the DoToClosestBlockTask created as DoStuffInContainerTask.openTableTask
after its actual filtered result
only when registered as CRAFTING_INTERACTION_TARGET
```

Fields:

```text
routeEvaluationId
filteredSearchId
routeNearestPosition
costPolicyNearestPosition
interactionNearestPresent
interactionNearestPosition
interactionEqualsRouteNearest
interactionEqualsCostPolicyNearest
currentlyPursuingBefore
currentlyPursuingAfter
pursuitTransition
nextAction
```

Allowed actions:

```text
INVOKE_GOAL_CALLBACK
RETURN_WANDER_TASK
RETURN_NULL_WAIT
```

Retain/select/invalidate는 `pursuitTransition`에 기록한다. D8은 callback 호출
전 경계이므로 actual returned Task를 꾸미지 않고 `nextAction`만 기록한다.
Callback이 만든 target action은 이후 `TARGET_ACTION` reconciliation과 existing
interaction event로 증명한다. `openTableTask`의 custom predicate는 always true;
여기서 “filtered”는 Store `validContainer`가 아니라 BlockScanner의 block-state
및 unreachable filtering을 뜻한다.

이 event는 D6의 exact filtered-result observation과 D8의 exact pursuit/next-action
observation을 같은 `routeEvaluationId`/`filteredSearchId`로 묶는 LAVI-owned
projection이다. 별도 scanner, predicate, pursuit 또는 callback 호출을 하지
않는다. Boundary ledger에서는 `CRAFT_INTERACTION_TARGET_DECIDED`로 사용한다.

### `CRAFT_SCREEN_HANDLER_MATCHED` Boundary Projection

새 screen event를 만들거나 interaction RETURN의 immediate `screenAfter`를
성공 authority로 사용하지 않는다. Exact producer는 다음 existing path다.

```text
DoCraftInTableTask.isContainerOpen()
    -> existing CraftingScreenHandler instanceof result

DoStuffInContainerTask.onTick()
    -> existing containerOpen local is true
    -> existing CONTAINER_TASK_BRANCH
       reason=return_container_sub_task
       decision=RETURN_CONTAINER_SUB_TASK
```

Active task가 exact Store-bound `DoCraftInTableTask`이고 위 existing branch가
같은 active-child context에서 관찰될 때만 boundary를 advance한다. 이 branch는
cost call보다 앞선 early return이므로 `routeEvaluationId`는
`UNAVAILABLE_EARLY_CONTAINER_OPEN`이며 coverage gap이 아니다. Existing
`containerOpen` evaluation을 다시 호출하지 않고, BlockInteraction RETURN이나
이전 tick의 cost/route event로 screen success를 추측하지 않는다.

### Existing Craft Route Fingerprint Correction

현재 `CraftingTableRouteRetryDiagnostics.routeKey()`는 task identity를
fingerprint에 포함하고, generic `ContainerTaskDiagnosticFields.repeatKey()`도
instance를 포함한 task summary를 key에 사용한다. 이 사건처럼
`DoCraftInTableTask` instance가 계속 바뀌면 semantic state가 같아도 새
bucket이 되어 cap을 빠르게 소진하고 diagnostic state map도 증가한다.

하지만 generic `ContainerTaskDiagnosticFields.repeatKey()`를 deposit 때문에
바꾸지 않는다. Unbound/non-deposit `ContainerTaskDiagnostics`와 limiter의 기존
key, event, cap 의미를 그대로 보존한다. 다음 semantic key는 새 Store-specific
budget/projection과 exact `storeOperationId`에 bind된
`CraftingTableRouteRetryDiagnostics` state에만 적용한다.

Store-bound diagnostics-only 보강:

```text
remove task identity from the dedupe fingerprint
retain task class and semantic route outcome
count observed creation/installation sequences and keep bounded identity samples
do not change route selection or task equality
```

새 key는 고정 semantic field만 사용한다.

```text
storeOperationId
+ event family
+ targetRole
+ normalized target position
+ route outcome
+ raw cost relation
+ rejection or failure reason
```

긴 문자열을 잘라 equality key로 사용하는 방식을 새 full-run counter에
사용하지 않는다.

## P1: Container Access, Transfer, And Effect

### `STORE_CONTAINER_ACCESS_DECISION`

Placement:

```text
AbstractDoToStorageContainerTask.onTick()
at each existing return boundary
```

Fields:

```text
targetPresent
targetPosition
currentContainerTypeBefore
currentContainerTypeAfter
targetChunkLoadedEvaluated
targetChunkLoaded
targetBlockTypeEvaluated
targetBlockType
screenHandlerMatchEvaluated
screenHandlerMatchesCurrentContainerType
containerCacheLookupEvaluated
containerCachePresent
isChestEvaluated
isChestResult
blockAboveSolidEvaluated
blockAboveSolidResult
blockAboveBreakableEvaluated
blockAboveBreakableResult
selectedAction
returnedTaskInstanceId
returnedTaskClass
```

Allowed actions:

```text
SEARCH_WANDER
RETURN_CONTAINER_OPEN_SUBTASK
DESTROY_BLOCK_ABOVE
INTERACT_WITH_CONTAINER
```

이 method의 조건은 short-circuit된다. 각 `...Evaluated` field가 false면
대응 value는 `UNAVAILABLE`로 기록한다. 진단을 채우기 위해 screen handler,
cache, chunk, block 또는 above-block state를 추가 조회하지 않는다.

### `STORE_CONTAINER_TRANSFER_DECISION`

Placement:

```text
StoreInContainerTask.onTick()
at the existing GET_MISSING_ITEM return

StoreInContainerTask.onContainerOpenSubtask()
after existing source and destination selection results are known
before each existing return
```

Fields:

```text
targetContainerPosition
containerCacheIdentity
containerCacheFullState
itemTarget
targetRequiredCount
rootTrackerStoredCount
targetContainerTrackerStoredCount
inventoryItemCount
openContainerItemCount
sourceCandidateSlotCount
bestSourceSlotPresent
bestSourceSlot
bestSourceItem
bestSourceCount
destinationSlotPresent
destinationSlot
sourceRejectionBreakdownAvailable
destinationCapacityValueAvailable
selectionCoverageGap
selectedAction
returnedMoveTaskInstanceId
```

Allowed actions:

```text
GET_MISSING_ITEM
NO_TARGET_REMAINING
DESTINATION_FULL
MOVE_TASK_SELECTED
NO_TRANSFER_CANDIDATE_FOUND
```

`NO_TARGET_REMAINING`은 existing unstored-target list가 실제로 empty로 평가된
경우에만 사용한다. Source 또는 destination 후보가 loop 안에서 거부된 뒤
최종 `null`이면 `NO_TRANSFER_CANDIDATE_FOUND`로 기록하고, 즉시
`NO_SOURCE_SLOT`을 추측하지 않는다. Existing `toMoveTo.isEmpty()` return은
계산된 그대로 `DESTINATION_FULL`로 기록한다.

현재 `StoreInContainerTask.onContainerOpenSubtask()`가 직접 보유한 값만으로는
`PickupFromContainerTask.getBestSlotToTransfer()` 내부의 source rejection 수,
destination capacity rejection 수, exact free capacity를 알 수 없다. 따라서
이 값들을 0으로 꾸미거나 capacity predicate를 다시 호출하지 않는다.
`sourceRejectionBreakdownAvailable=false`,
`destinationCapacityValueAvailable=false`, `selectionCoverageGap`으로 한계를
표현한다. 이후 그 경계가 꼭 필요하면 `PickupFromContainerTask`의 실제 기존
selection-comparison/canFit 평가 지점에 별도 conditional observer slice를
설계해야 하며 이 first pass에는 포함하지 않는다. 첫 후보에는 canFit 평가가
없고 이후 후보도 comparison boolean만 제공하므로, 그 observer조차 exact free
capacity나 전체 rejection count의 authority가 아니다.

전체 slot 목록과 full NBT는 기록하지 않는다. 이미 계산된 source/destination
slot index, ownership, item ID와 count만 기록한다.

### `STORE_CONTAINER_EFFECT_OBSERVATION`

Placement:

```text
ContainerStoredTracker.startTracking()
inside the existing SlotClickChangedEvent callback
```

Tracker roles:

```text
ROOT_ANY_CONTAINER
TARGET_CONTAINER
```

Fields:

```text
slotMutationId
trackerInstanceId
trackerRole
expectedContainerPosition
lastBlockPosInteraction
predicateLastInteractionEvaluated
predicateLastInteractionPresent
predicateLastInteractionPosition
predicateTargetPosition
predicateMatchReason
slotIdentity
slotIndex
slotInPlayerInventory
acceptPredicateEvaluated
acceptPredicateResult
observationOutcome
beforeItem
beforeCount
afterItem
afterCount
deltaComponentCount
deltaComponent1Item
deltaComponent1Amount
deltaComponent1TrackerLifetimeBefore
deltaComponent1OperationBaseline
deltaComponent1OperationNet
deltaComponent1OperationGrossPositive
deltaComponent1OperationGrossNegative
deltaComponent1OperationPositiveHighWatermark
deltaComponent2Item
deltaComponent2Amount
deltaComponent2TrackerLifetimeBefore
deltaComponent2OperationBaseline
deltaComponent2OperationNet
deltaComponent2OperationGrossPositive
deltaComponent2OperationGrossNegative
deltaComponent2OperationPositiveHighWatermark
effectObservationVersion
expectedDepositEffectVersion
inventoryObservationVersion
screenHandlerClass
screenSyncId
```

Allowed outcomes:

```text
REJECT_PLAYER_INVENTORY_SLOT
REJECT_TARGET_PREDICATE
ACCEPT_POSITIVE_DELTA
ACCEPT_NEGATIVE_DELTA
ACCEPT_ZERO_DELTA
ACCEPT_ITEM_REPLACEMENT
```

기존 short-circuit 의미를 보존한다.

```text
player inventory slot
    -> do not evaluate the deposit predicate

non-player slot
    -> evaluate the existing predicate exactly once
    -> reuse that exact boolean for behavior and diagnostics
```

Item replacement는 old item의 negative component와 new item의 positive
component로 분리한다. 하나의 `computedDelta`로 합치지 않으며 event당 component는
최대 두 개다. 각 component는 자신의 item ID와 operation-baseline 대비 net,
gross positive/negative, positive high-watermark를 가진다.
순서는 replacement일 때 old item negative가 component 1, new item positive가
component 2다. Same-item count change는 component 1 하나만 사용하고 component 2는
`UNAVAILABLE`이다. Empty stack component는 만들지 않는다.
`ContainerStoredTracker._totalDeposited`는 `startTracking()`에서 clear되지
않으므로 lifetime total을 operation effect로 사용하지 않는다. Registry가
operation bind 시점의 baseline을 보관하고 summary에는 baseline 대비 net과
positive high-watermark만 사용한다.

`TARGET_CONTAINER`의 accept predicate lambda는
`StoreInContainerTask.onStart()`에서 정의·subscribe될 뿐, 그 시점에는 평가되지
않는다. 실제 `lastBlockPosInteraction` 조회와 predicate 평가는 나중에 기존
`ContainerStoredTracker.startTracking()`의 `SlotClickChangedEvent` callback이
`_acceptDeposit.test(slot)`을 호출할 때 lambda body에서 정확히 한 번 일어난다.
그 lambda body에서 실제 snapshot, boolean, 다음 reason을 함께 capture해 같은
callback의 diagnostic observation으로 전달한다.

```text
LAST_INTERACTION_ABSENT
POSITION_MISMATCH
MATCH
```

`onStart()`에서 predicate를 미리 호출하지 않고, `ContainerStoredTracker`
callback에서도 last interaction 또는 predicate를 다시 조회하지 않는다.
`ROOT_ANY_CONTAINER` tracker는 predicate가 항상 true이므로 이 target-match
field를 `UNAVAILABLE`로 둔다.

같은 `SlotClickChangedEvent` mutation을 ROOT와 TARGET tracker가 모두 관찰할
수 있으므로 `slotMutationId`는 두 observer에서 공유한다. Summary의
`uniqueSlotMutationCount`는 mutation을 한 번만 세고,
`trackerObservationCountByRole`은 각 tracker 관측을 따로 센다.
`sameMutationObservedByBothRolesCount`로 중복 관측을 명시하며, role별 accepted
counter를 합쳐 unique mutation 수로 오인하지 않는다.

Immediate detail conditions:

```text
first accepted positive delta
first accepted delta per item
first target-predicate rejection reason
root/target tracker disagreement
negative delta
item replacement
delta concurrent with screen or sync-id transition
```

나머지는 aggregate counter만 갱신한다.

### Reuse Existing Container-Open Events

Minecraft interaction 경계에는 새 이벤트를 만들지 않는다. 기존
`BlockInteractionDiagnostics`가 방출하는 다음 이벤트를
`storeOperationId`, `storeAttemptId`, `interactionAttemptId`, `targetRole`,
`targetPosition`과 연결한다.

```text
CONTAINER_OPEN_ATTEMPT_OBSERVED
CONTAINER_OPEN_RETURN_OBSERVED
```

`interactionAttemptId`는 새 ID가 아니라 existing `BlockInteractionContext`의
`interactionId()`를 그대로 재사용하는 이름 또는 1:1 alias다. HEAD/attempt와
RETURN은 같은 existing ID를 써야 하며 별도 counter로 두 ID를 생성하지 않는다.

정확한 연결 순서는 다음과 같다.

```text
existing observer callback
    -> bind interactionAttemptId to one immutable Store context snapshot

BlockInteractionDiagnostics.emitInteractionEvent()
    -> read-only lookup of that exact interactionAttemptId
    -> merge context fields into the existing attempt/return payload
```

Observer callback이 event payload를 직접 바꿀 수 있다고 가정하지 않는다.
`emitInteractionEvent()`의 기존 emission 지점에서 focused registry를 읽는
최소 additive merge가 필요하다. Binding이 없으면
`storeContextAvailable=false`로 남기고 최근 Store operation을 추측하지 않는다.
Observer와 emitter는 이미 계산된 interaction input/return만 읽으며 click을
재호출하거나 screen/container state를 변경하지 않는다.

## P1: Baritone Context And Aggregate

Baritone detail emission이 cap에 걸려도 Store-bound aggregate는 계속
갱신되어야 한다.

### `STORE_BARITONE_CONTEXT_BOUND`

Placement candidates:

```text
BaritonePathCalculationDiagnostics.logGoalRequestDecision()
BaritonePathCalculationDiagnostics.logCalculationScheduled()
```

Fields:

```text
storeOperationId
storeAttemptId
routeEvaluationId
goalRequestId
calculationGeneration
pathfinderIdentity
pathingBehaviorIdentity
contextBindingSource
contextAvailable
contextBindingConfidence
targetRole
targetPosition
requestedGoalType
requestedGoalSummary
processOwnerAtBind
```

Allowed binding sources:

```text
CURRENT_STORE_TASK
ACTIVE_CHILD_CONTEXT
ROUTE_TARGET_MATCH
UNAVAILABLE
```

Allowed confidence values:

```text
EXACT_ACTIVE_CHILD_CONTEXT
EXACT_GENERATION_PROPAGATION
ROOT_CONTEXT_ONLY
TARGET_MATCH_ONLY
UNBOUND
```

`CURRENT_STORE_TASK`만 있으면 defense/other chain이 실제 path owner일 수 있어
`ROOT_CONTEXT_ONLY`다. 좌표만 맞는 `ROUTE_TARGET_MATCH`는
`TARGET_MATCH_ONLY`다. 이 둘은 correlation hint로만 남기고 exact Store
attribution, last-success 또는 first-failure 판정에 사용하지 않는다. Exact
판정에는 active Task stack/child context 또는 immutable generation
propagation이 필요하다.

Worker thread에서 시간상 가장 가까운 Store event를 찾아 추측하지 않는다.
Client thread scheduling boundary에서 Minecraft 또는 Task state를 한 번
snapshot한 immutable context를 `calculationGeneration` 또는
pathfinder identity에 bind하고 worker/adoption event가 같은 snapshot을
이어받아야 한다. Worker는 Minecraft/Task state를 다시 읽지 않는다. Bind할
수 없으면 `contextAvailable=false`로 남긴다.

### Baritone Worker / Terminal Concurrency Contract

Client thread와 Baritone worker가 같은 mutable operation state를 동시에
갱신하지 않는다.

```text
client thread only
    StoreDepositOperationState
    StoreDepositBoundaryLedger
    root/child lifecycle binding
    terminal finalizer and summaryGroupId issuance

worker-safe diagnostic owner only
    StoreBaritoneGenerationContextRegistry
    immutable generation context
    fixed-key atomic generation/result counters
    bounded late-event/expiry counters
    StoreCriticalReserveBudget atomic reservation state
    stateless immutable critical-event emission
```

Worker event는 client-owned Task/state를 읽거나 `StoreDepositBoundaryLedger`를
직접 갱신하지 않는다. Thread-safe generation registry에 exact immutable context와
counter만 반영한다. Terminal finalizer는 engine thread나 worker를 기다리거나
lock으로 막지 않고, client thread에서 diagnostic-only idempotent compare-and-set
후 현재 generation snapshot과 `inFlightAtTerminalCount`를 확정한다. 그 뒤 worker
event는 terminal summary를 다시 쓰지 않고 bounded late counter와 conditional
late summary에만 반영한다.

Summary snapshot과 late registry cleanup이 경합하면 한 generation은 atomic
state transition으로 terminal-before 또는 late-after 중 정확히 한 쪽에만
분류한다. Overflow/expiry/race ambiguity는 coverage를 false로 만들 뿐 engine
Task, Baritone process, thread scheduling 또는 terminal timing을 바꾸지 않는다.

`StoreCriticalReserveBudget`은 client terminal finalizer와 worker exception
observer가 공유하는 유일한 critical-budget mutable owner다. Terminal group은
단일 compare-and-set으로 네 칸을 all-or-none 예약하고, worker의 첫 exception
signature는 단일 칸을 같은 atomic ledger에서 예약한다. 두 예약은 linearizable
하며 partial terminal reservation은 없다. 성공한 reservation은 immutable token으로
`StoreDepositDiagnosticEmitter`에 전달한다. Emitter는 shared mutable builder나
Task/Minecraft state를 읽지 않고 immutable fields만 직렬화하며, event identity
발급도 thread-safe existing diagnostic identity path를 사용한다. 어느 경로도 다른
thread를 기다리거나 engine lock, scheduling, exception propagation을 바꾸지 않는다.

Additive context fields for existing events:

```text
BARITONE_GOAL_REQUEST_DECISION
BARITONE_CALCULATION_SCHEDULED
BARITONE_CALCULATION_WORKER_STARTED
BARITONE_PATHFINDER_CALCULATE_STARTED
BARITONE_PATHFINDER_CALCULATE_COMPLETED
BARITONE_PATH_ADOPTION_DECISION
BARITONE_PATHING_FORCE_CANCEL_BOUNDARY
BARITONE_PROCESS_CONTROL_TICK
```

`STORE_BARITONE_OPERATION_SUMMARY` fields:

```text
goalRequestObservedCount
goalRequestAcceptedCount
goalRequestRejectedCount
goalRequestRejectedCountByReason
calculationScheduledCount
workerStartedCount
calculateStartedCount
calculateCompletedCount
resultTypeCounts
resultPathPresentCount
resultPathAbsentCount
adoptionOutcomeCounts
firstBoundGeneration
lastBoundGeneration
lastDetailGeneration
lastAggregatedGeneration
boundGenerationCount
scheduledWithoutStartCount
startedWithoutCompletionCount
completedWithoutAdoptionCount
unboundGenerationCount
forceCancelBoundaryCount
processOwnerTransitionCounts
inFlightAtTerminalCount
exceptionEpisodeCount
exceptionCountBySignatureBounded
exceptionSignatureSampleOmittedCount
userBlockRangeNullInputObservedCount
correlatedExceptionCount
unboundExceptionCount
firstExceptionTick
firstExceptionSignature
lastExceptionTick
lastExceptionSignature
exceptionAggregateCoverageComplete
```

Exception aggregate는 detail/critical reserve보다 먼저 fixed counter를 갱신한다.
Signature map은 first 16 semantic signatures와 `OTHER`/omitted count로 제한하며
unbounded exception-message key를 만들지 않는다. 따라서 detail cap 이후에도
episode 총수, correlation 성공/실패, 첫/마지막 signature/tick은 terminal
summary에 남는다.

Root stop에서는 task-to-operation binding 같은 diagnostic bookkeeping만
제거한다. 이미 scheduling된 generation의 immutable context는 bounded
retention으로 늦은 worker/adoption event까지 보존한다. Post-terminal
retention은 최대 6000 client ticks이며 operation당 256, session당 1024 context
cap을 함께 적용한다. TTL, entry cap 또는 session end에서 미해결 context는
expire count로 전환하고 coverage를 false로 둔다. 이 cleanup은 Baritone
process, goal, path, Task 또는 thread를 취소하지 않는다.

Terminal 이후 bound in-flight generation이 모두 해소되거나 diagnostic-only
retention이 만료된 경우에만 다음 bounded companion을 한 번 방출할 수 있다.

```text
STORE_BARITONE_LATE_COMPLETION_SUMMARY
    storeOperationId
    inFlightAtTerminalCount
    lateWorkerStartedCount
    lateCalculateCompletedCount
    lateAdoptionCount
    lateExceptionCount
    expiredContextCount
    unresolvedAtExpiryCount
    coverageComplete
```

이 companion을 위해 Task 또는 Baritone을 기다리거나 terminal을 지연하지
않는다. 안전한 existing diagnostic maintenance hook이 없으면 event를 새로
만들지 않고 terminal summary의 `inFlightAtTerminalCount`와 coverage gap만
남긴다.

Result and adoption counts distinguish at least:

```text
SUCCESS_TO_GOAL
SUCCESS_SEGMENT
CANCELLATION
EXCEPTION
OTHER

ADOPTED_AS_CURRENT
ADOPTED_AS_NEXT
NO_PATH_CANCELLATION
NO_PATH_EXCEPTION
RESULT_NOT_OBSERVED
NO_PATH_RESULT_NOT_ADOPTABLE
PATH_PRESENT_NOT_ADOPTED_OR_DISCARDED
OTHER
```

## P2: Exception And Null-Input Correlation

가장 좁고 안전한 첫 P2 관측은 아래
`USER_BLOCK_RANGE_NULL_INPUT_OBSERVED`다. `BARITONE_PATH_EXCEPTION`은 새
catch 없이 사용할 수 있는 기존 전파 경계를 별도 source audit으로 찾은
경우에만 추가한다. 해당 경계를 못 찾으면 구현하지 않고 coverage gap으로
남긴다.

### Conditional Second Pass: `BARITONE_PATH_EXCEPTION`

이 event는 현재 구현에 존재하지 않는다. `AbstractNodeCostSearch`의 normal
return diagnostic은 exception이 발생한 경우 실행되지 않으므로 기존
exception boundary가 이미 있다고 표현하지 않는다.

새 broad catch를 추가하지 않는다. Existing Baritone path-calculation
exception이 이미 관찰되고 전파되는 정확한 boundary를 먼저 확인한 뒤,
그 boundary에서만 context를 additive하게 기록한다.

Fields:

```text
exceptionEpisodeId
exceptionClass
exceptionMessage
stackIncluded
exceptionSignature
repeatCount
storeOperationId
storeDecisionSequence
branchEpoch
storeAttemptId
routeEvaluationId
parentBranch
parentRawCandidate
activeTaskInstanceId
activeTaskClass
activeChildInstanceId
activeChildClass
targetRole
targetPosition
goalRequestId
calculationGeneration
pathfinderIdentity
pathingBehaviorIdentity
requestedGoalType
requestedGoalSummary
processOwner
exploreProcessInControl
```

Full stack fingerprint:

```text
storeOperationId
+ exceptionClass
+ top stack boundary
+ targetRole
```

한 fingerprint의 첫 event만 full stack를 포함한다. 반복은 counter와
checkpoint/final summary로 기록한다.
원 2026-08-19 50분 46초 사건의 여섯 NPE 중 첫 episode만
UserBlockRangeTracker stack을 포함했다. 나머지 다섯 episode를 같은 원인으로
묶지 않으며, 다음 재현에서도 matching exception signature/context가 있을
때만 같은 episode family로 집계한다. 이는 별도 R1 live prefix에서 관측된
일곱 NPE episode의 수치와 합산하지 않는다.

### First Targeted P2: `USER_BLOCK_RANGE_NULL_INPUT_OBSERVED`

이 event는 현재 source에 부분 구현되어 R1 live prefix에서 한 번 관측됐다.
다만 현재 `lastActiveState()` 기반 operation 연결은 exact immutable provenance가
아니므로 canonical targeted P2 contract는 아직 미완료다. 구현 상태의 권위는
위 Implementation Status Ledger를 따른다.

Placement:

```text
UserBlockRangeTracker.updateState()
inside the existing removeIf evaluation
immediately before the existing null BlockPos dereference
```

Fields:

```text
nullInputObserved=true
consumerBoundary
contextAvailable
contextBindingConfidence
trackedBlockCategory
sourceCollectionIdentity
sourceCollectionSize
elementPositionOrNull
nullProducer=UNOBSERVED
sourceIndex=UNOBSERVED
calculationGeneration
goalRequestId
storeDecisionSequence
branchEpoch
routeEvaluationId
activeTaskInstanceId
activeChildInstanceId
targetRole
targetPosition
storeOperationId
storeAttemptId
```

허용되는 동작은 null을 기록한 뒤 기존 evaluation을 계속하는 것뿐이다.

```text
no continue
no return
no remove-on-null behavior change
no fallback BlockPos
no new catch
no exception conversion or suppression
```

원래 NPE가 발생할 state라면 diagnostic 후에도 동일한 NPE가 발생하고
기존 방식으로 전파되어야 한다.
Store/generation context를 exact immutable binding에서 찾지 못하면 관련 field를
blank로 두고 `contextAvailable=false`로 기록한다. 시간상 가까운 Store
operation을 추측해 붙이지 않는다.

## Bounded Logging And Cap Design

### 2026-08-31 post-checkpoint ownership note

The Store-specific numbers and event design below remain historical source and
test provenance. For a separately approved future diagnostics correctness
slice, [ChatClef Automatic Deposit Post-Checkpoint Work Separation Direction](chatclef-automatic-deposit-post-checkpoint-direction-2026-08-31.md)
supersedes only these meanings:

```text
canonical Fabric ChatClef investigation-session admission owner
canonical DIAGNOSTIC_SESSION_CAP_REACHED ownership
terminal-group exhaustion as permission to lose all terminal accounting
```

The linked direction is the canonical future acceptance contract; this note
does not change source or authorize implementation or execution.

현재 구현 사실:

```text
Store progress detail session cap: 256
Store branch/repeat session cap: 512
Crafting route retry session cap: 256
Container task session cap: 5000
Mining diagnostic session cap: 5000
```

이 수치는 현재 evidence 설명이며 이 문서에서 변경되지 않는다.

새 Store deposit diagnostics는 네 계층을 사용한다.

Store-specific investigation events have one session hard cap:

```text
total Store deposit diagnostic session cap: 5000 events
noncritical detail/checkpoint ceiling: 4936
critical reserve: 64
    terminal summary events: at most 32
    late Baritone completion summaries: at most 8
    first exception-signature events: at most 16
    cap/suppression/coverage control events: at most 8
```

이 통합 cap과 reserve는 현재 기존 gate가 제공하지 않으므로 focused
`StoreDetailBudget`, `StoreCheckpointBudget`, `StoreCriticalReserveBudget`이
각자의 숫자만 소유하고 delegation-only `StoreDiagnosticBudgetFacade`가
조합해야 한다. 기존 normal lifecycle, warning, error 또는 crash log를 이
investigation cap으로 억제하지 않는다.

### Layer A: Full-Run Counters

출력 여부와 무관하게 operation terminal까지 갱신한다.

```text
branch counts and transition matrix
candidate and rejection counts
filtered-target callback/reference-result and progress-reset counts
pursuit decisions
child reconciliation outcomes
DoCraft candidate/start/stop counts when observable
route/cost/interaction mismatch counts
Baritone generation/result/adoption counts
container access and transfer outcomes
slot effect and tracker acceptance counts
exception signature counts
```

Counter key는 enum, normalized item ID, bounded target role처럼 고정된 semantic
domain만 사용한다. Task instance마다 map key를 만들지 않으며 모든 counter는
detail/checkpoint gate보다 먼저 갱신한다.

### Layer B: State-Change Detail

Operation 전체 state-change detail hard cap은 256이다. Family별 sub-cap은
서로 독립적이지만 합계가 이 hard cap을 넘지 않는다.

| Event family | Proposed detail budget per operation |
| --- | ---: |
| root/descendant lifecycle transition | 16 |
| parent / filtered / pursuit | 44 |
| child reconciliation | 44 |
| crafting route / cost / interaction | 44 |
| container access / transfer / effect | 44 |
| Baritone correlation | 44 |
| first exception signature | 16, from critical reserve |

한 family의 churn이 다른 family의 effect evidence budget을 소진하지
않는다. Detail은 first observation, new semantic state, first reason, first
explicit failure, first accepted delta, new exception signature에서만
방출한다.

### Layer C: Periodic Checkpoint

Event:

```text
STORE_DEPOSIT_CHECKPOINT_SUMMARY
```

Proposed policy:

```text
interval: 1200 client ticks
maximum: 64 checkpoints per operation
skip when no aggregate counter or semantic state changed
```

Fields:

```text
elapsedTicks
currentBranch
branchCountsSinceLastCheckpoint
transitionCountsSinceLastCheckpoint
currentRawCandidate
currentFilteredCandidate
currentTargetCallbackPosition
targetCallbackReferenceMatchState
currentPursuit
currentRouteTarget
currentInteractionTarget
effectObservationVersion
expectedDepositEffectVersion
expectedPositiveTargetDeltaSinceLastCheckpoint
notStoredStateHash
inventoryObservationVersion
childReplacementCountSinceLastCheckpoint
targetCallbackCountSinceLastCheckpoint
targetCallbackProgressResetReturnedNormallyCountSinceLastCheckpoint
baritoneCompletedCountSinceLastCheckpoint
baritoneAdoptedCountSinceLastCheckpoint
exceptionCountSinceLastCheckpoint
lastSuccessfulBoundary
firstExplicitFailureBoundary
```

### Layer D: Critical Reserve

General detail cap을 우회하되 무제한은 아니다.

```text
each terminal summary event: at most once per operation
terminal summary event emissions: at most 32 per session total
terminal group events: exactly the four named summaries when applicable
complete terminal groups: at most 8 per session
late Baritone completion summaries: at most 8 per session
first exception signatures: at most 16 per session
cap/suppression/coverage controls: at most 8 per session
```

Terminal group의 all-or-none 네 칸 예약 실패는 위 control bucket의
`STORE_DEPOSIT_TERMINAL_GROUP_RESERVE_EXHAUSTED`로만 보고한다. 이 event는 partial
`STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY`가 아니며 terminal group count에
포함하지 않는다.

`USER_BLOCK_RANGE_NULL_INPUT_OBSERVED`와 conditional
`BARITONE_PATH_EXCEPTION`의 첫 unique signature는 같은 16-event exception
sub-reserve를 공유한다. Signature별 첫 event 뒤 반복은 fixed counter로만
집계하며 별도 bypass bucket을 만들지 않는다.

Lifecycle detail은 first STOP/INTERRUPT transition, first resume, action change,
root/descendant role change에서만 방출한다. 반복 transition은 fixed counter와
checkpoint/terminal summary에만 남긴다. Critical sub-reserve가 소진되면 해당
control reserve 안에서 가능한 한 번의 bounded coverage warning만 남기고
추가 bypass를 허용하지 않는다.

### Representative Samples

각 family의 position/sample set은 최대 16개만 유지한다.

```text
first 4 representative samples
last 8 representative samples
first sample per explicit failure reason until the 16-sample cap
omittedCount and truncated=true when additional samples exist
```

전체 target 목록, full inventory, full container, full NBT, full path node
list를 보관하지 않는다. Truncated list에는 `omittedCount`를 포함한다.
`exceptionMessage`, goal/process/task summary와 optional value rendering은 각
512 characters로 제한하고 `truncated=true`, `originalLength`를 남긴다. Unique
exception signature의 첫 occurrence에 허용되는 stack은 기존 exception
diagnostic contract를 따르며 반복 stack은 재출력하지 않는다.

### Dedupe Rules

금지 fingerprint material:

```text
task instance ID
System.identityHashCode
game tick
timestamp
random UUID
opaque Object.toString()
unordered collection rendering
full target list
```

Preferred semantic key:

```text
storeOperationId
+ event family
+ targetRole
+ dimensionId
+ normalized target position
+ semantic outcome
+ rejection or failure reason
```

`storeOperationId`는 operation bucket을 구분하는 데 사용할 수 있지만 매
log attempt마다 새로 발급하면 안 된다. Unique instance와 attempt 수는
aggregate counter로 유지한다.

### Diagnostic State Bounds And Cleanup

Emission cap만 두고 state map을 무제한으로 키우지 않는다.

```text
active operation states: maximum 16 per session
root STOP tombstones: maximum 16 per session and 6000 client ticks
candidate bindings: maximum 64 per operation; remove after reconciliation
active child bindings: maximum 64 per operation; remove on replace/true stop
slot-event identity map: weak-identity bounded LRU, maximum 256 per session
interaction-attempt bindings: maximum 256 per session
in-flight generation contexts: maximum 256 per operation and 1024 per session
post-terminal generation retention: maximum 6000 client ticks
semantic dedupe buckets: maximum 252 per operation
position/identity samples: maximum 16 per family
```

Fingerprint bucket cap을 `computeIfAbsent`보다 먼저 검사한다. Cap 이후에는
새 position/task key를 만들지 않고 fixed counter, omitted count, eviction count,
coverage flag만 갱신한다. Candidate binding은 reconciliation 직후, stopped or
replaced child binding은 해당 lifecycle 뒤, generation binding은 adoption 뒤
제거한다. Natural finish에서는 즉시 operation state를 제거한다. Root true
STOP에서는 BEGIN tombstone을 유지하고 END에서 main state를 제거하며,
temporary interrupt에서는 모두 유지한다.

Exact distinct count를 위해 unbounded ID set을 두지 않는다. Creation/install
sequence counter와 bounded identity sample을 사용한다. Registry overflow 또는
eviction은 behavior에 영향을 주지 않고 coverage summary에 기록한다.

기존 diagnostics state도 함께 bounded audit 대상이다.

```text
StoreInAnyContainerProgressDiagnostics.STATES
    -> current source can create state before the later emission cap decision
    -> future diagnostics slice must check a semantic bucket bound before insert
    -> clean at authoritative root terminal and session teardown
    -> overflow becomes fixed omitted/coverage counters only

CraftingTableRouteRetryDiagnostics.STATES
    -> current source can create state before the later emission cap decision
    -> exact Store-bound state uses the Store semantic bucket bound before insert
    -> exact Store-bound state may clean at authoritative root terminal
    -> unbound/non-deposit state preserves its existing key until session teardown
    -> overflow becomes fixed omitted/coverage counters only

StoreInAnyContainerEmissionLimiter.repeatStates
    -> indirectly bounded today by its 512 emission check
    -> exact Store-bound state may be removed at authoritative root terminal

ContainerTaskEmissionLimiter.repeatStates
    -> indirectly bounded today by its generic 5000 emission check
    -> preserve unbound/non-deposit key and event semantics
    -> session-teardown accounting only; a Store root terminal must not purge generic state
```

기존 event 이름과 의미는 유지한다. `CraftingTableRouteRetryDiagnostics` key에서
task identity를 semantic fingerprint로 쓰지 않고 identity는 payload/counter에만
남긴다. 이 helper는 deposit 전용이 아니므로 root terminal cleanup/key migration은
`storeOperationId`에 exact bind된 state에만 적용하고 unbound/non-deposit
crafting route의 기존 event 의미와 수명은 보존한다. 이 정리는 diagnostic
state 수명만 바꾸며 engine Task나 retry 수명에는 영향을 주면 안 된다.

## Loop Or Stall Classification

경과 시간만으로 loop 원인을 판정하지 않는다.

Attempt-level outcome은 exact owner의 reason과 supersession context를 보존한다.
다음은 matching attempt의 explicit unsuccessful/refusal evidence 후보이며, 기존
owner가 non-superseded unsuccessful outcome으로 명시한 경우에만
`firstExplicitFailureBoundary`로 승격한다.

```text
matching path calculation explicitly reports a non-superseded no-path/error
matching adoption explicitly reports a non-superseded unsuccessful outcome
existing transfer owner explicitly reports no valid source or destination full
tracker predicate explicitly rejects an actually observed matching mutation
expected GUI failure is emitted by an already-existing bounded failure/timeout owner
```

다음은 그 자체로 attempt failure가 아닌
`NORMAL_NON_TERMINAL_OR_SUPERSEDED_OUTCOME`이다.

```text
filtered result absent; the existing owner may return a wander route
interaction CANT_REACH; the existing owner starts or continues a movement goal
container cache missing; the existing owner may fall through to interaction
goal request accepted=false with CURRENT_PATH_PRESENT or IN_PROGRESS_PRESENT
goal request accepted=false with UNKNOWN_REJECTED_OR_ALREADY_IN_GOAL
calculation cancellation or non-adoption that may be supersession/owner change
PATH_PRESENT_NOT_ADOPTED_OR_DISCARDED without an exact unsuccessful reason
```

이 결과들은 반복·무효과 classification의 입력이 될 수 있지만 후속 exact
failure evidence 없이 attempt failure나 operation terminal이 되지 않는다.
일반적인 GUI 미관측도 existing bounded failure producer가 없으면 failure가 아니라
`firstUnobservedBoundaryAfter`다. Event가 없다는 사실만으로 failure를 만들지
않으며 coverage가 끊긴 경우에도 같은 inference field를 사용한다.

Operation-level `REPEATING_NO_EFFECT_PATTERN`은 다음이 모두 관찰된 경우에만
diagnostic classification으로 사용할 수 있다.

```text
same storeOperationId remains active
same finite semantic state cycle repeats
transition matrix confirms the cycle
child/route/path attempt counters continue to increase
expectedDepositEffectVersion does not increase
target state does not advance or returns to a previous state
natural finish and explicit operation failure remain absent
repetition continues until an external StopCommand or other terminal boundary
aggregate coverage continues after detail caps
```

예시 cycle:

```text
OPEN_EXISTING
  -> FILTERED_NONE_OR_NO_USEFUL_EFFECT
  -> OBTAIN_CHEST
  -> CRAFT_TABLE_ROUTE
  -> OPEN_EXISTING
```

Eight observed cycles may be used as a diagnostic anomaly threshold. This
number must not become a retry limit, timeout, cancellation trigger, or Task
failure condition.

`REPEATING_NO_EFFECT_PATTERN`과 `UNKNOWN`은 diagnostic classification일
뿐이며 gameplay terminal, timeout, cancellation 또는 command result를 만들지
않는다.

Matching attempt가 non-superseded `NO_PATH` unsuccessful outcome으로 명시돼도
root가 같은 fallback을 계속 반복하면 두 결과를 동시에 기록한다.

```text
attempt-level: explicit unsuccessful outcome
operation-level: non-terminating retry cycle
```

## Boundary Model

Boundary는 하나의 전역 선형 사다리로 합산하지 않는다. 최소한
`OPEN_EXISTING`과 `OBTAIN_CHEST` branch별 attempt ladder를 따로 유지하고,
같은 `storeAttemptId` 안에서만 last-success/first-failure를 비교한다.

Boundary vocabulary has no global order; branch ladders below own chronology.

| Boundary | Advance evidence |
| --- | --- |
| `PARENT_BRANCH_DECIDED` | raw candidate and exact existing branch booleans recorded |
| `ROOT_ROUTE_CHILD_RECONCILED` | Store root's route/acquisition candidate and active child after reconciliation recorded |
| `FILTERED_SEARCH_COMPLETED` | actual filtered result and bounded rejection summary recorded |
| `PURSUIT_DECIDED` | pursuit after existing validation/selection and the next action recorded before callback/wander/null |
| `TARGET_CALLBACK_COMMITTED` | actual filtered-target reference comparison and existing progress-reset/current-try result recorded |
| `TARGET_ACTION_CHILD_RECONCILED` | route child's callback/target action candidate and active child after reconciliation recorded |
| `SEARCH_FALLBACK_CHILD_RECONCILED` | no-pursuit wander child reconciliation recorded when that fallback is selected |
| `CRAFT_ROUTE_EVALUATION_ENTERED` | exact `STORE_CRAFT_ROUTE_EVALUATION_ENTERED` emitted immediately before the existing cost call |
| `CRAFT_COST_POLICY_EVALUATED` | actual `getCostToMakeNew()` return reason recorded |
| `CRAFT_ROUTE_TARGET_DECIDED` | existing `CONTAINER_TASK_TARGET_DECISION` recorded after cost evaluation |
| `CRAFT_ROUTE_BRANCH_DECIDED` | matching existing `CONTAINER_TASK_BRANCH` recorded after target decision |
| `CRAFT_INTERACTION_TARGET_DECIDED` | open-table task's actual filtered target, pursuit transition, and next action recorded |
| `GOAL_REQUEST_ACCEPTED` | exactly Store-bound goal decision explicitly accepted; `accepted=false` is not a failure without a non-superseded unsuccessful reason |
| `PATH_CALCULATION_COMPLETED` | matching generation has `SUCCESS_TO_GOAL` or `SUCCESS_SEGMENT`; cancellation/no-path/exception is explicit failure only when its exact reason proves this attempt unsuccessful rather than superseded |
| `PATH_ADOPTED` | matching adoption is `ADOPTED_AS_CURRENT` or `ADOPTED_AS_NEXT`; non-adoption is explicit failure only with a matching non-superseded unsuccessful reason |
| `INTERACTION_ATTEMPTED` | target-specific interaction attempt recorded |
| `CRAFT_TABLE_INTERACTION_ATTEMPTED` | crafting-table target interaction recorded when that subpath is entered |
| `CRAFT_SCREEN_HANDLER_MATCHED` | expected crafting-table GUI observed |
| `STORAGE_SCREEN_HANDLER_MATCHED` | expected storage-container GUI observed |
| `CONTAINER_CACHE_RESOLVED` | target storage position cache present |
| `TRANSFER_TASK_SELECTED` | already-computed source and destination selection recorded |
| `EXPECTED_SLOT_MUTATION_OBSERVED` | matching requested item has a positive non-player target-container delta |
| `TARGET_TRACKER_POSITIVE_DELTA` | target-container predicate accepted the matching positive delta |
| `ROOT_TARGET_STATE_ADVANCED` | root stored/notStored state advanced |

이 vocabulary를 모든 branch에 선형으로 강제하지 않는다.

| Branch / subpath | Ordered required or entered-conditional boundaries | Explicitly not applicable until a later branch |
| --- | --- | --- |
| `OPEN_EXISTING` | `PARENT_BRANCH_DECIDED -> ROOT_ROUTE_CHILD_RECONCILED`; active route child가 tick되면 `FILTERED_SEARCH_COMPLETED -> PURSUIT_DECIDED`; `nextAction=INVOKE_GOAL_CALLBACK`일 때만 `TARGET_CALLBACK_COMMITTED -> TARGET_ACTION_CHILD_RECONCILED`; no-pursuit wander를 실제 선택하면 callback 대신 `SEARCH_FALLBACK_CHILD_RECONCILED`; goal/path/adoption/interaction은 실제 movement subpath에 들어간 경우 conditional; storage screen/cache/transfer/effect/root-state는 container-open subpath에 들어간 경우 conditional | craft cost/route/table-interaction |
| `OBTAIN_CHEST` | `PARENT_BRANCH_DECIDED -> ROOT_ROUTE_CHILD_RECONCILED`; nested craft task reconciliation만으로 cost path를 요구하지 않는다. Exact `STORE_CRAFT_ROUTE_EVALUATION_ENTERED`가 관찰된 경우에만 `CRAFT_ROUTE_EVALUATION_ENTERED -> CRAFT_COST_POLICY_EVALUATED -> CRAFT_ROUTE_TARGET_DECIDED -> CRAFT_ROUTE_BRANCH_DECIDED`가 conditional; cursor-click branch면 target-action child는 N/A다. Matching open-table branch 뒤 `CRAFT_INTERACTION_TARGET_DECIDED`가 관찰되고 `nextAction=INVOKE_GOAL_CALLBACK`이면 `TARGET_ACTION_CHILD_RECONCILED`; 실제 이동/상호작용을 선택하면 goal/path/adoption/`CRAFT_TABLE_INTERACTION_ATTEMPTED -> CRAFT_SCREEN_HANDLER_MATCHED`가 conditional | storage screen/cache/transfer/effect/root-state; 다음 `OPEN_EXISTING` attempt 전에는 요구하지 않음 |
| `GET_MISSING_TARGET` | `PARENT_BRANCH_DECIDED -> ROOT_ROUTE_CHILD_RECONCILED`; 실제 acquisition subpath 경계만 conditional | parent raw/filtered container search와 storage transfer/effect 경계 |

`firstUnobservedBoundaryAfter`는 해당 branch에서 `REQUIRED`이거나 실제 진입한
`CONDITIONAL` 경계 사이에서만 계산한다. `NOT_APPLICABLE` 또는 아직 진입하지
않은 conditional 경계는 failure, gap 또는 missing event로 세지 않는다.

각 attempt와 terminal summary는 다음을 구분한다.

```text
lastSuccessfulBoundary
firstExplicitFailureBoundary
firstUnobservedBoundaryAfter
```

`firstUnobservedBoundaryAfter`는 관측된 실패가 아니라 state-machine과
coverage ledger에서 도출한 inference다. Parent raw candidate와 child filtered
result가 다르다는 사실도 handoff 원인의 직접 증명이 아니라 상관 증거이며,
predicate rejection 또는 reconciliation 경계가 함께 있어야 원인으로
분류한다.

예시:

```text
lastSuccessfulBoundary=GOAL_REQUEST_ACCEPTED
firstExplicitFailureBoundary=NONE
firstUnobservedBoundaryAfter=PATH_CALCULATION_COMPLETED
```

이 경우 path calculation failure를 주장하지 않는다.

## Hypothesis Proof Matrix

| Hypothesis | Minimum required evidence |
| --- | --- |
| Parent-to-child handoff failure | raw candidate present + open-existing selected + same attempt filtered none/different position + explicit rejection summary |
| Filtered-target callback reset churn | filtered target present + actual reference comparison false + existing progress reset returned normally + same semantic target/value relation and no expected effect advancement |
| Child lifecycle failure | filtered candidate present + candidate goal task created + reconciliation discard/block + active child differs from expected target |
| Baritone failure | pursuit and child installed + matching target goal bound + exact non-superseded reject/no-path/error/adoption-failure reason; current-path, in-progress, unknown-already-in-goal, cancellation, or non-adoption alone is insufficient |
| Interaction failure | matching target-action child installed + goal/path/adoption evidence only when that movement subpath was entered + matching interaction attempted + explicit interaction failure other than `CANT_REACH`, or expected-screen failure from an already-existing bounded failure/timeout owner. `CANT_REACH` alone is movement routing, and a missing GUI event alone is only an unobserved boundary |
| Transfer failure | expected storage screen/cache resolved + explicit transfer-selection failure outcome; or move task selected + matching `TARGET_ACTION` reconciliation proves the move child installed/retained + bounded lifecycle evidence records an explicit task failure. Missing slot mutation alone is only `firstUnobservedBoundaryAfter=EXPECTED_SLOT_MUTATION_OBSERVED` or no-effect-before-terminal, not an explicit transfer failure |
| Target-tracker rejection | root tracker accepted a positive matching `slotMutationId` + target tracker rejected the same mutation with absent/mismatched last interaction |
| Root-tracker miss | target tracker accepted a positive matching `slotMutationId` + root tracker has no matching delta under complete shared-mutation coverage |
| Root accounting failure | tracker positive delta + one or more subsequent root-state evaluations under complete coverage still do not advance. If terminal occurs before another root evaluation, the boundary is unobserved rather than an explicit accounting failure |

## Java Boundary Matrix

| Class / method | Diagnostic responsibility | Phase |
| --- | --- | --- |
| `DepositCommandDiagnostics.logInvocation()` | issue and bind `storeOperationId`; extend existing invocation event | P0 |
| `UserTaskChain.cancel()` pre-stop boundary | bind existing cancel invocation and registered Store root before true stop | P0 |
| `Task.stop()` / `Task.interrupt()` | registered Store-only lifecycle action/role observer outside the VERBOSE guard | P0 |
| `StoreInAnyContainerTask.onStart()` | confirm origin binding and start aggregate | P0 |
| `StoreInAnyContainerTask.onTick()` | parent candidate/branch and target-state change | P0 |
| `StoreInAnyContainerTask.onTick()` target callback | capture the actual `_currentChestTry != blockPos` result and existing reset/assignment outcome once | P1 |
| `StoreInAnyContainerTask.isFinished()` | finalize once on the first actual true result without calling it again | P0 |
| `StoreInAnyContainerTask.onStop()` | callback snapshot only; never purge or finalize by itself | P0 |
| `StoreInAnyContainerProgressDiagnostics` state boundary | semantic pre-insert bound and authoritative terminal/session cleanup only | P0 diagnostic maintenance |
| `CraftingTableRouteRetryDiagnostics` state boundary | semantic fingerprint, pre-insert bound, and authoritative terminal/session cleanup only | P0 diagnostic maintenance |
| `StoreInAnyContainerTask.onTick()` local `validContainer` lambda | collect actual rejection reason from the one existing evaluation | P1 |
| `DoToClosestBlockTask.getClosestTo()` | Store-container or registered crafting-interaction target's final filtered result | P1 |
| `BlockScanner.getNearestBlock(Block, Predicate<BlockPos>, Vec3d)` | conditional block-state/predicate/unreachable filter summary | P1 conditional |
| `DoToClosestBlockTask.isValid()` | retained-pursuit chunk/predicate/block-check result only | P1 |
| `AbstractDoToClosestObjectTask.onTick()` | pursuit/callback/wander/null next-action decision for registered Store tasks only | P1 conditional |
| existing `Task.tick()` reconciliation hook | distinguish `ROOT_ROUTE`, `TARGET_ACTION`, and `SEARCH_FALLBACK` reconciliation with already captured values; keep mining schema unchanged | P1 |
| `DoCraftInTableTask.getCostToMakeNew()` | existing cost-policy candidate and return reason | P1 |
| `DoStuffInContainerTask.onTick()` | preserve early `containerOpen` branch evidence; otherwise issue `routeEvaluationId`, emit route-evaluation-entered, observe cost return, then extend target-decision and every post-target branch in actual order | P1 |
| LAVI-owned `StoreDepositDiagnosticObserver` interaction projection | combine D6a/D8 facts into `CRAFTING_TABLE_INTERACTION_TARGET_DECISION` without another upstream read | P1 |
| LAVI-owned `StoreDepositDiagnosticObserver` craft-screen projection | map exact Store-bound DoCraft `return_container_sub_task` branch to `CRAFT_SCREEN_HANDLER_MATCHED`; no second screen read | P1 |
| existing `BlockInteractionDiagnostics` observer callback | bind exact interaction attempt to immutable Store context | P1 |
| `BlockInteractionDiagnostics.emitInteractionEvent()` | read-only merge of exact bound Store context into existing attempt/return payload | P1 |
| `AbstractDoToStorageContainerTask.onTick()` | target/GUI/cache/interact decision | P1 |
| `StoreInContainerTask.onStart()` predicate lambda body | when the existing tracker callback later invokes it, capture that one actual target-container predicate snapshot/result/reason; do not evaluate at subscription time | P1 |
| `StoreInContainerTask.onTick()` | existing missing-item return | P1 |
| `StoreInContainerTask.onContainerOpenSubtask()` | source/destination transfer decision | P1 |
| `PickupFromContainerTask.getBestSlotToTransfer()` | conditional selection-comparison/canFit-evaluation observer only; not exact free-capacity or full rejection authority | P1 conditional |
| `ContainerStoredTracker.startTracking()` callback | one predicate evaluation and accepted/rejected slot effect | P1 |
| existing Baritone diagnostic methods | immutable generation binding and aggregate update | P1 |
| exact existing Baritone exception propagation boundary | correlated exception event, if found | P2 |
| `UserBlockRangeTracker.updateState()` | optional pre-dereference null observation only | P2 conditional |

This matrix is an architecture plan, not approval to modify all listed upstream
files in one change. Each upstream-derived boundary requires the smallest local
hunk and a separate scope review. If the first implementation slice requires
multiple upstream lifecycle owners, stop and report why the failure cannot be
observed more narrowly.

## Implementation Slices And Stop Gates

Recommended order:

```text
D0a  LAVI-owned context package and focused unit tests only
D0b  LAVI-owned binding package and focused unit tests only
D0c  LAVI-owned budget package and focused unit tests only
D0d  LAVI-owned emission package and focused unit tests only
D0e  LAVI-owned Baritone context/counter/late-summary package and race tests only
D0f  one existing diagnostic state owner at a time: pre-insert bound and terminal/session cleanup only
D1   DepositCommand invocation binding only
D2   UserTaskChain cancel provenance hook only
D3a  Task.stop() registered Store BEGIN/END lifecycle hook and tombstone only
D3b  Task.interrupt() registered Store lifecycle hook only
D4a  StoreInAnyContainerTask.onStart activation observation only
D4b  StoreInAnyContainerTask.onTick parent/root-state observation only
D4c  StoreInAnyContainerTask.isFinished natural terminal observation only
D4d  StoreInAnyContainerTask.onStop callback snapshot only
D5   StoreInAnyContainerTask validContainer rejection collection only
D6a  DoToClosestBlockTask.getClosestTo filtered-result observation only
D6b  DoToClosestBlockTask.isValid retained-pursuit validation observation only
D7   BlockScanner filter summary only, if the remaining gap requires it
D8   AbstractDoToClosestObjectTask pursuit observation only, if still needed
D8b  StoreInAnyContainerTask target callback reference/reset observation only, if pursuit evidence requires it
D9   Task.tick Store reconciliation routing only
D10a DoStuffInContainerTask routeEvaluationId plus exact route-evaluation-entered observation only
D10b DoCraftInTableTask cost-policy observation only
D10c DoStuffInContainerTask target-decision/branch event extension only
D11a LAVI-owned D6a/D8 projection into CRAFTING_TABLE_INTERACTION_TARGET_DECISION only
D11b LAVI-owned exact DoCraft return_container_sub_task projection into CRAFT_SCREEN_HANDLER_MATCHED only
D12a existing BlockInteractionDiagnostics observer binding only
D12b BlockInteractionDiagnostics existing event payload context merge only
D13  AbstractDoToStorageContainerTask access decision only
D14a StoreInContainerTask.onStart predicate-lambda body observation only; actual evaluation occurs only when the existing tracker callback invokes it
D14b StoreInContainerTask.onTick missing-item observation only
D14c StoreInContainerTask.onContainerOpenSubtask transfer observation only
D14d PickupFromContainerTask selection-comparison/canFit observation only, if still needed
D15  ContainerStoredTracker effect observation only
D16  one existing Baritone diagnostic owner at a time, with review between owners
D17  UserBlockRangeTracker null-input observation only, if still needed
D18  BARITONE_PATH_EXCEPTION only if a safe existing propagation boundary is proven
D19  separately authorized BOUNDARY reproduction and evidence review
```

각 slice 종료 시 다음을 보고하고 다음 slice로 자동 진행하지 않는다.

```text
exact files and methods changed
ownership classification
events and fields added
aggregate-before-gate confirmation
fingerprint and budgets
behavior explicitly preserved
wire unchanged
tests completed
remaining unobserved boundary
next proposed source hunk
```

Stop conditions:

```text
more than the minimum upstream hunk is required
two or more upstream lifecycle owners must change in one first-pass patch
new method signatures would carry behavior policy rather than immutable diagnostics
an observation requires calling scanner, predicate, progress checker, isEqual,
  isFinished, getCostToMakeNew, Baritone, input, or slot APIs a second time
exception observation requires a new broad catch
terminal summary would depend on changing Task completion or shutdown ordering
```

## Prohibited Behavior Fixes

다음 변경은 first failing boundary가 증명되고 별도 behavior approval을 받기
전까지 금지한다.

```text
Store 50-block or 70-block range changes
binding _currentChestTry to raw closest by force
changing the existing _currentChestTry != blockPos reference comparison
passing the parent raw candidate to the child as a behavior change
removing the child filtered rescan
holding a container candidate across ticks
dungeon-chest/cache/predicate order changes
requestBlockUnreachable count, threshold, or reset changes
Store progress checker retry changes
overall timeout or retry limit
loop detector cancellation or failure behavior
StoreInAnyContainerTask.isFinished() semantic changes
DoToClosestBlockTask.isEqual() changes
Task.canBeInterrupted or child replacement changes
Task.stop/interrupt semantics, ordering, active flag, or first flag changes
UserTaskChain cancel/onTaskFinish ordering or result changes
keeping a Task alive to wait for diagnostic finalization
DoCraftInTableTask reuse, caching, equality, or lifecycle changes
costToMakeNew=Infinity policy changes
40-block crafting-table preference or 10/100 cost changes
forcing a new crafting table or chest
ExploreProcess stop or restart
Baritone process owner, goal request, calculation, adoption, or force-cancel changes
path cancellation reclassification
UserBlockRangeTracker null skip, removal, fallback, return, or catch
new broad catch around the NPE
container interaction retry, click order, or GUI reopen changes
source/destination slot-selection changes
MoveItemToSlotFromInventoryTask behavior changes
ContainerStoredTracker predicate or delta changes
screenHandler/cache success reinterpretation
command lifecycle, DTO, schema, or wire payload changes
```

Diagnostics를 채우기 위한 다음 추가 호출도 금지한다.

```text
getNearestBlock
validContainer.test
progressChecker.check or reset
isEqual
isFinished
getCostToMakeNew
slot capacity or accept predicate
```

## Validation Required After A Future Diagnostics Patch

LAVI-owned helper tests:

```text
one operation ID per invocation
origin binding and unbound handling
parent/child/generation context propagation
monotonic decision and attempt IDs
root-route versus target-action reconciliation roles
OPEN_EXISTING chronology: root reconcile -> filtered -> pursuit -> callback -> target reconcile
craft chronology: cost -> target decision -> branch decision
craft early returns do not activate the cost ladder
post-target cursor clicks produce bounded branch evidence without a second click
callback omitted as NOT_APPLICABLE for wander/null paths
aggregate updates before emission denial
terminal summaries project every fixed full-run family counter after detail caps
family budgets remain independent
semantic dedupe ignores task identity and tick
checkpoint interval and maximum
each terminal summary emits at most once
root STOP BEGIN/onStop/descendant STOP/END tombstone sequencing
worker completion racing terminal snapshot is atomically classified once
terminal finalization never waits for or rewrites Baritone worker state
terminal reserve is bounded
registry cleanup does not affect engine state
replacement slot mutation uses ordered old-negative/new-positive components
unique mutation count does not double-count ROOT and TARGET observations
```

Static/contract checks:

```text
no return-value changes
no extra scanner or predicate calls
no extra progress-checker, isEqual, isFinished, or cost calculation calls
no Task selection or lifecycle-order changes
no Baritone goal/path/process mutations
no input or slot mutations added
no exception catch/suppression added
no wire/DTO/schema changes
existing event names preserved
```

Build and runtime validation are separate approvals. When authorized, use the
clean forced Fabric build runbook and verify the copied JAR plus runtime code
source marker before interpreting a reproduction.

## Completion Criteria For The Diagnostics Phase

The diagnostics phase is complete only when one bounded reproduction can show:

```text
one local deposit origin and one storeOperationId
parent raw candidate and exact branch booleans
root route/acquisition child reconciliation
same-attempt filtered result and rejection summary
pursuit decision before callback/wander/null
actual filtered-target reference comparison and progress-reset outcome
target-action or search-fallback child reconciliation
exact craft route-entry, cost, target-decision, branch, and interaction-target relationship
matching Baritone generation and adoption result when applicable
interaction, screen, cache, transfer, and slot effect boundaries
root tracker and target-state effect
cap-independent counters through terminal
last successful boundary
first explicit failure boundary, or a precisely named coverage gap
terminal reason without calling cancellation a successful deposit
```

If any material boundary remains unobserved, root cause remains unknown and the
next action is another bounded diagnostics-only slice, not a behavior fix.

## 2026-08-31 shared-session implementation delta

This append-only note does not rewrite the historical partial-status ledger or
claim completion of every D-slice in this plan. The current implementation adds
and deterministically verifies the shared diagnostics session cap, bounded
terminal accounting, strict-OFF state isolation, and separately owned
tool-selection shaping described in Section 15 of
[ChatClef Automatic Deposit Post-Checkpoint Work Separation Direction](chatclef-automatic-deposit-post-checkpoint-direction-2026-08-31.md).

The current Java contract is `5000` shared hard-cap slots, `4936` ordinary
slots, and `64` critical-reserve slots. Four-record terminal groups are atomic
at token claim and admission, not at the physical sink: a sink failure may
leave `0..4` physical writes and does not refund or retry slots. The emitted
final-snapshot line may self-report one pending/unverified delivery while its
post-call in-memory accounting closes to zero pending and one completed
delivery.

This delta is diagnostics-only. It does not close the still-unobserved Store,
craft, container, or Baritone boundaries listed above; it does not prove a
deposit root cause; and it does not authorize a gameplay behavior change.
Current clean-build evidence is complete, but the fresh build JAR and active
CurseForge JAR hashes differ, so final-artifact runtime validation remains
`NOT_RUN`/`INCONCLUSIVE`.
