<!-- 20260831_openai: Recorded the post-checkpoint separation, shared diagnostics-admission direction, bounded terminal-accounting gate, test-only handoff coverage, and release matrix without authorizing source or runtime work. -->
<!-- 20260831_openai: Incorporated external review corrections as Stage 1 closure inputs for scope anti-bypass, terminal reconciliation, snapshot provenance, and release coverage without marking Stage 1 complete or authorizing source work. -->
<!-- 20260831_openai: Documented an optional, separately authorized Python harness for final-JAR runtime orchestration and evidence collection without replacing Java tests or authorizing implementation or execution. -->
<!-- 20260831_openai: Reconciled the post-checkpoint Java test-only/build state and tightened the Python harness around reuse-first architecture, hermetic pytest, explicit transport modes, fixture/setup separation, run-scoped evidence integrity, and phased source/live gates. -->
<!-- 20260901_kpopmodder: Clarified the Java/Python/Minecraft/operator evidence ownership split and the evidence-driven runtime-harness implementation order. -->
<!-- 20260902_openai: Linked the still-unimplemented H5 bulk-trust command to the exact-fixture safety contract. -->
<!-- 20260902_openai: Tightened the still-unimplemented H5 bulk-trust safety boundary around fail-closed parsing, exact fixture ownership, no harness submission, and downstream revision-triggered automatic reevaluation. -->
<!-- 20260903_kpopmodder: Applied the canonical GUI-gate continuous implementation, bounded-log, and verification workflow while preserving historical checkpoint approvals. -->
<!-- 20260903_openai: Aligned the automatic storage GUI boundary with one-shot interaction correlation, safe open-child quiescence, exact route types, explicit tick serials, full-path suppression, and one-time permission; recorded independent dirty H5 provenance. -->
<!-- 20260904_kpopmodder: Recorded the verified H5 crosshair source/test/build/deployment/runtime baseline and separated the docs-only player-position follow-up; commit and push remain open. -->
<!-- 20260904_kpopmodder: Recorded the current H5 player-position source/test/build/deployment and partial runtime registration evidence while retaining the earlier crosshair baseline as historical provenance. -->

# ChatClef Automatic Deposit Post-Checkpoint Work Separation Direction

Date: 2026-08-31
Scope: Fabric ChatClef 1.20.1 automatic-deposit checkpoint follow-up and budgeted investigation diagnostics

## 1. 문서 상태와 승인 경계

이 문서는 automatic-deposit handoff 체크포인트 뒤의 작업 경계와 검증 계약을
기록한다. 구현 완료 보고서나 release 승인 문서가 아니다.

현재 container GUI exact-binding/three-later-tick 작업에는 canonical Carry On 방향의
continuous workflow가 우선한다. 사용자가 해당 구현을 직접 요청하면 source/config/test
구현, bounded logging, focused tests와 required clean build를 단계별 승인 대기 없이
연속 수행한다. 이 문서의 과거 `CURRENT_AUTHORIZATION`, Stage별 승인 및 checkpoint 표는
당시 provenance를 보존하는 역사 기록이며 현재 GUI gate를 다시 정지시키지 않는다.
외부 JAR 배포, Minecraft/live-world 실행, commit과 push는 현재 요청이 정확히 포함할 때만
수행한다.

이 override는 아래 Stage 1-6, `CURRENT_AUTHORIZATION`과 action table을 삭제하지 않지만 의미를
명확히 제한한다. 그 row들은 automatic-deposit diagnostics/harness/H5 당시 작업의 provenance이며,
현재 exact GUI stabilization source/test/required clean build에 새 승인 중단점을 만들지 않는다.
반대로 GUI override는 diagnostics backlog, H5, Python harness, 외부 배포, Minecraft runtime,
commit 또는 push를 자동 승인하지 않는다.

아래 fenced status block의 checkpoint, Stage, `380 PASSED`, harness `NOT_IMPLEMENTED` 값은
2026-08-31 역사 snapshot이다. 현재 test/build/harness evidence로 인용하지 않는다. 이후 상태는
§15의 implementation/build delta와 §16.4의 2026-09-01 live evidence delta가 소유한다. 단,
아래 H5 current-worktree row는 2026-09-04 player-position 구현·검증까지 갱신된 current qualifier다.

```text
STATUS_BLOCK_CLASSIFICATION: HISTORICAL_2026_08_31_CHECKPOINT_SNAPSHOT_WITH_2026_09_04_H5_PLAYER_POSITION_DELTA
LATEST_TEST_BUILD_HARNESS_EVIDENCE_POINTER: SECTION_15_AND_SECTION_16_4
DOCUMENT_STATUS: DIRECTION_AND_PRECHANGE_CONTRACT
EXTERNAL_REVIEW_RESULT: PASS_FOR_STAGE_1_PRECHANGE_CONTRACT_AFTER_HARNESS_BOUNDARY_CORRECTIONS
DOCUMENT_REVIEW_DISPOSITION: PASS_TO_BEGIN_SEPARATELY_AUTHORIZED_STAGE_1_CLOSURE
POST_CHECKPOINT_DIRECTION_OWNER: THIS_DOCUMENT
BASELINE_BRANCH: test/automatic-deposit-checkpoint-20260831
BASELINE_COMMIT: a722ac2a17a813b62e605eaeac0fc9f96f7a1b5e
BASELINE_CLASSIFICATION: HANDOFF_FUNCTIONAL_CHECKPOINT
HANDOFF_PRODUCTION_STATUS: FROZEN
DIAGNOSTICS_CORE_STATUS: CONTRACT_INCOMPLETE; SOURCE_CHANGE_NOT_STARTED_BY_THIS_DOCUMENT
STAGE_1_STATUS: OPEN
STAGE_2_SOURCE_EDIT_GATE: BLOCKED
TOOL_SELECTION_SHAPING_STATUS: SEPARATE_FOLLOW_UP
CONTAINER_TYPE_LIFECYCLE_STATUS: TEST_ONLY_SOURCE_ENHANCED; CLEAN_FORCED_BUILD_PASS_RECORDED; FINAL_JAR_RUNTIME_NOT_RUN
POST_CHECKPOINT_JAVA_TEST_STATUS: 380 PASSED; 1 EXISTING SKIPPED
POST_CHECKPOINT_TEST_SCOPE: CONTAINER GENERATION/CANDIDATE TRANSITION AND OPERATION ISOLATION
POST_CHECKPOINT_PRODUCTION_HANDOFF_DIFF: 0 REPORTED
POST_CHECKPOINT_BUILD_SCOPE: TEST_ONLY TREE VERIFICATION; NOT FINAL DIAGNOSTICS RC
PYTHON_RUNTIME_HARNESS_STATUS: DOCUMENTED_OPTION; NOT_IMPLEMENTED; NOT_EXECUTED
PYTHON_RUNTIME_HARNESS_PROPOSED_SOURCE: tests/minecraft_chatclef/runtime/automatic_deposit/**
PYTHON_RUNTIME_EVIDENCE_ROOT: test/test_Isolation/automatic_deposit_runtime/<new-run-id>/**
PYTHON_RUNTIME_HARNESS_IMPLEMENTATION_GATE: PHASED; HERMETIC CORE AFTER STAGE_1; FEATURE BINDING AFTER STAGE_2/3 + STAGE_4; LIVE EXECUTION AFTER FINAL JAR
RELEASE_READY: NO

H5_BULK_TRUST_DOCUMENT_SNAPSHOT: DIRECTION_ONLY; SOURCE_NOT_STARTED_AT_2026-09-02_REVIEW
H5_2026_09_02_AREA_COMMAND_BEHAVIOR: TRAILING_ARGUMENTS_NOT_FAIL_CLOSED; MAY_EXECUTE EXISTING SINGLE REGISTRATION
H5_CURRENT_SOURCE_STATUS: PLAYER_BLOCK_POSITION_IMPLEMENTED_IN_CURRENT_WORKTREE; EXACT_FORMS_FAIL_CLOSED
H5_CURRENT_DETERMINISTIC_TEST_STATUS: PASS; 1.20.1=707 TESTS; 0 FAILURES; 0 ERRORS; 1 EXISTING SKIPPED; ALL 11 CONFIGURED VERSION TEST TASKS PASSED
H5_CURRENT_FOCUSED_TEST_STATUS: INCLUDED_IN_FULL_SUITE; SEPARATE_CURRENT_FOCUSED_COUNT_NOT_RECORDED
H5_CURRENT_CLEAN_FORCED_BUILD_STATUS: PASS; 171 OF 171 ACTIONABLE TASKS EXECUTED
H5_CURRENT_1_20_1_ARTIFACT_SHA256: 24DD4C8DEBA968D0206CA3D68A227C57D513E55769F1BB51F5943F549989CC27
H5_CURRENT_DEPLOYMENT_STATUS: VERIFIED_IN_LAVI_TEST_FABRIC01; ACTIVE_JAR_SHA256_MATCH
H5_CURRENT_MINECRAFT_RUNTIME_STATUS: PLAYER_POSITION_BATCH_REGISTRATION_LIVE_VERIFIED; PARTIAL_ACCEPTANCE
H5_CURRENT_COMMIT_PUSH_STATUS: NOT_PERFORMED_AT_DOCUMENT_SNAPSHOT
H5_BATCH_COMMAND_EXECUTION: LIVE_UPDATED_AT_2026-09-04_21:32:57_KST; SCANNED_4096; PHYSICAL_44; LOGICAL_22; NEW_9; EXISTING_13; REVISION_1_TO_2; REGISTRY_13_TO_22
H5_PLAYER_POSITION_ANCHOR_STATUS: IMPLEMENTED; TESTED; BUILT; DEPLOYED; RUNTIME_OBSERVED
H5_OPEN_RUNTIME_ACCEPTANCE: NO_CHANGE_RERUN + PLAYER_MOVE_RANGE_SHIFT + EXPLICIT_EDGE_TESTS + NEWLY_ADDED_DESTINATION_SELECTION NOT_RUN

MARKDOWN_DOCUMENTATION: AUTHORIZED_FOR_THIS_TASK
AUTHORIZATION_ROWS_CLASSIFICATION: HISTORICAL_CHECKPOINT_PROVENANCE; NOT_A_CURRENT_GUI_GATE
CURRENT_AUTHORIZATION: MARKDOWN_ONLY
FURTHER_JAVA_JSON_RESOURCE_OR_TEST_SOURCE_CHANGE: NOT_AUTHORIZED_BY_THIS_DOCUMENT
PYTHON_TEST_HARNESS_SOURCE_CHANGE: NOT_AUTHORIZED_BY_THIS_DOCUMENT
FURTHER_TEST_OR_BUILD_EXECUTION: NOT_AUTHORIZED_BY_THIS_DOCUMENT
JAR_DEPLOYMENT_OR_MINECRAFT_RUNTIME: NOT_AUTHORIZED_BY_THIS_DOCUMENT
COMMIT_OR_PUSH: NOT_AUTHORIZED_BY_THIS_DOCUMENT
```

### 1.1 Current exact GUI stabilization boundary

Automatic-deposit history does not broaden the current GUI gate. The first
implementation mapping remains:

```text
in-scope general storage:
    minecraft:chest, minecraft:trapped_chest
    exact screen: GenericContainerScreen
    exact handler: GenericContainerScreenHandler

parallel regular-furnace route owned outside automatic deposit:
    minecraft:furnace
    exact screen: FurnaceScreen
    exact handler: FurnaceScreenHandler

unchanged:
    minecraft:barrel, every shulker box, minecraft:smoker,
    minecraft:blast_furnace and every unlisted block/screen/handler
```

For an in-scope storage attempt, exactly one matching `BlockInteractEvent` must
be observed after `WORLD_OPEN_REQUESTED` and before its TAIL candidate, match the
immutable target/world/dimension, and be consumed by only that candidate. The
TAIL callback captures an immutable candidate snapshot and
`candidateClientTickSerial` only. It does not grant permission or perform child
cleanup. Once consumed, the event remains permanently spent for that attempt
even if the candidate is later rejected. A retry requires a new
attempt/correlation and matching event.

Before `GUI_BOUND`, the route parent must make its open child quiescent so it
cannot tick, repeat `interactBlock`, close/replace the screen, or perform delayed
input/path work, and must finish only the operation-owned normal child cleanup.
Any binding change during that handoff discards the candidate. During
`GUI_STABILIZING`, the parent, current/former child, observer, cleanup, fallback
and every other reachable path are blocked from slot/cursor/screen/interact/input
and Baritone mutation; no mutation-capable child tick or stop may run. A child
cleanup that releases global or otherwise unowned state is not operation-owned
cleanup. Operation-local validation, serial, deduplication, counter, invalidation
and permission bookkeeping may change because it does not mutate gameplay.

The TAIL candidate tick `K` and route-owned promotion tick `B` are both excluded,
and the `K`-to-`B` gap is never backfilled. The ledger must prove current-tick
serial publication before route promotion and chosen-boundary observation of the
same serial. The bound attempt owns immutable `boundClientTickSerial` and initializes
`lastCountedBoundarySerial` to it. For observed serial `S`, `S <= bound` or
`S <= lastCounted` never counts. Only a strictly later distinct boundary may
fully validate, increment once, and store `S`. Boundary #3 publishes an
attempt/binding-scoped one-time permission only. The next normal route Task
evaluation must revalidate the complete live predicate and route-specific
transfer preconditions. Permission consumption and entry into the existing
PICKUP/button lifecycle are one logical commit. If entry cannot be established,
do not consume permission; return a typed invalidation or terminal result and
perform no slot action.

Focused regression and bounded logs must cover at least: missing/duplicate or
reused interaction observations; child quiescence and cleanup; candidate change
during handoff; exact screen/handler mismatch; opening/stale/duplicate boundary
serials; mutation suppression across every reachable participant at counts
0/1/2 and the third callback; full next-evaluation revalidation; one-time
permission/retry isolation; preserved action type/button; and unchanged
Barrel/shulker/smoker/blast/unlisted and generic interaction routes. Use the
canonical names in
[Task Lifecycle Diagnostics](chatclef-task-lifecycle-diagnostics.md#canonical-exact-gui-gate-log-fields)
verbatim and correlate `operationId`, `openAttemptId`, `correlationId`,
`matchingBlockInteractEventObserved`, `matchingBlockInteractEventCount`,
`matchingBlockInteractEventConsumed`, `duplicateBlockInteractEventRejected`, `openChildIdentity`,
`openChildQuiescent`, `openChildCleanupComplete`, `screenTypeExpected`, `screenTypeActual`,
`screenTypeMatched`, `handlerTypeExpected`, `handlerTypeActual`, `handlerTypeMatched`,
`screenObjectIdentity`, `handledScreenHandlerIdentity`, `playerHandlerIdentity`, `capturedSyncId`,
`liveSyncId`, `candidateClientTickSerial`, `boundClientTickSerial`, `clientTickBoundarySerial`,
`lastCountedBoundarySerial`, `candidateTickExcluded`, `boundPromotionTickExcluded`,
`sameBoundaryDuplicateSuppressed`, `stableLaterBoundaries`, `fullGuiBoundPredicate`, `guiInputAllowed`,
`slotMutationSuppressed`, `reachableMutationPath`, `reachableMutationKind`,
`suppressedMutationOwner`, `permissionAvailable`, `permissionFullRevalidationPassed`,
`permissionConsumed`, `permissionReuseRejected`, `transferLifecycleEntryCommitted`, `slotActionOwner`, `slotActionType`, and
`slotButton`. Do not emit
unchanged per-tick or per-slot polling logs.

최초 방향 문서 작성 전 읽기 전용 preflight는 다음과 같았다.

```text
cwd: C:\Vtuber_Souorce_Code\LAVI
git root: C:/Vtuber_Souorce_Code/LAVI
branch: test/automatic-deposit-checkpoint-20260831
HEAD: a722ac2a17a813b62e605eaeac0fc9f96f7a1b5e
upstream recorded locally: origin/test/automatic-deposit-checkpoint-20260831
ahead/behind against locally recorded upstream: 0/0
working tree before documentation edits: clean
live remote freshness: not checked; no fetch performed
```

최초 review-correction pass 시작 시에는 같은 branch/HEAD에서 이 방향 문서와 세 backlink
문서의 예상된 미커밋 Markdown 변경만 있었고 Java, test source, resource 또는 build output
변경은 없었다. 이는 최초 문서 보정 시점의 역사적 provenance다.

그 뒤 2026-08-31 당시 별도 승인된 test-only work에서 production handoff hunk는 유지한 채 container
generation/candidate 전환과 operation isolation의 Java test source만 보강되었다. 현재 작업에서
제공된 당시 상태에 따르면 clean forced build가 완료되었고 Java test는 380개가 통과했으며 기존
1개가 skipped다. 이 결과는 post-checkpoint test-only tree의 자동 검증 증거이며 Stage 2/3
diagnostics 구현 뒤의 final RC 검증이나 Minecraft runtime 증거가 아니다. 최종 JAR은 아직
CurseForge에 배포하거나 Minecraft에서 검증하지 않았다.

이번 재검토와 Markdown 보정 자체에서는 source, test source, build, JAR 또는 Minecraft
runtime을 변경하거나 실행하지 않는다. 위 post-checkpoint 결과는 제공된 current-state
provenance로 기록하며, exact Java diff, build log와 artifact hash를 이 문서가 독립적으로
재검증했다고 주장하지 않는다. 과거 checkpoint 증거와 future final-RC evidence를 섞지 않는다.

승인 경계는 action별로 독립적이다.

| Action | Required authority |
| --- | --- |
| 현재 Markdown 변경 | 현재 사용자 요청으로 승인 |
| Stage 1 추가 조사와 문서 closure | 해당 조사·문서 범위의 별도 사용자 지시 |
| 추가 production 또는 Java test-source 수정 | 명시적인 source-edit 승인 |
| 격리된 Python runtime-harness 파일 생성 또는 수정 | 해당 test-harness source-edit의 별도 승인 |
| Python harness 실행과 runtime 증거 수집 | 별도 script/test-execution 및 필요한 runtime 승인 |
| focused 또는 full test 실행 | 별도 test-execution 승인 |
| 추가 clean forced build | 별도 build 승인 |
| JAR 복사 또는 배포 | 별도 deployment 승인 |
| Minecraft launch 또는 runtime reproduction | 별도 runtime 승인 |
| commit | 별도 commit 승인 |
| push | 별도 push 승인 |

이 표는 automatic-deposit checkpoint 작업 당시 action provenance다. 현재 exact GUI
stabilization 요청에는 §1.1과 canonical Carry On continuous workflow가 이미 적용되므로,
표의 generic source/test/build row가 그 기능의 중간 승인 gate로 다시 작동하지 않는다.
H5, diagnostics/harness, 배포, runtime, commit과 push에는 표의 원래 범위가 유지된다.

## 2. 문서 관계와 부분 supersession

이 문서는 `a722ac2...` 이후의 work separation, shared budgeted-investigation
admission, Store bounded terminal-accounting acceptance, Stage 1 closure gate와
release matrix의 canonical owner다. Handoff의 과거 source/runtime 증거와 generic
lifecycle, optional Carry On 및 backend-separation 정책은 아래 companion 문서가 계속
소유한다.

다음 문서가 각 증거와 정책을 계속 소유한다.

- [Post-Place Handoff Pre-Change Report](chatclef-auto-deposit-post-place-handoff-pre-change-report-2026-08-31.md)는
  `a722ac2...` handoff source, build, artifact와 캡처 runtime 증거를 소유한다.
- [Automatic Deposit Slice A Diagnostics Contract](chatclef-auto-deposit-slice-a-diagnostics-contract-2026-08-30.md)는
  automatic-deposit identity, observation, lifecycle과 diagnostics-only 불변조건을 소유한다.
- [Bare Deposit Diagnostics-Only Plan](chatclef-bare-deposit-diagnostics-plan.md)은
  Store diagnostic event와 historical `4936 + 64`, four-summary group 설계의 provenance를 소유한다.
- [Diagnostics Refactoring Backlog](chatclef-diagnostics-refactoring-backlog-2026-08-29.md)은
  기존 facade extraction과 source-equivalence 기록을 소유한다.
- [Obtain-Chest / Mining Diagnostics Review](chatclef-auto-deposit-obtain-chest-mining-diagnostics-review-2026-08-30.md)는
  mining-local gate와 기존 session-wide exactly-once cap-event 제안의 provenance를 소유한다.
- [Task Lifecycle Diagnostics](chatclef-task-lifecycle-diagnostics.md)은 generic Task,
  child, Baritone 관측 경계를 소유한다.
- [ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)은
  optional Carry On class-loading, 상태 전환, 미설치 fallback과 success predicate를 소유한다.
- [Minecraft Backend Separation](minecraft-backend-separation.md)은 Fabric-only ownership과
  Forge/MineMind 비공유 경계를 소유한다.
- [Fabric ChatClef 1.20.1 Build Verification](chatclef-fabric-build-verification.md)은
  나중에 별도로 승인된 clean forced build와 artifact 검증 절차를 소유한다.
- repository root의 `AGENTS.md`가 diagnostics-only, bounded logging, upstream 보존,
  shared-state ownership과 승인 경계의 최상위 규칙이다.

이 문서는 기존 기록 전체를 덮어쓰지 않는다. 향후 별도 승인되는
`Diagnostics Core Boundedness` correctness slice에 대해서만 다음 소유권을
부분적으로 supersede한다.

| 기존 기록 | 그대로 보존하는 내용 | 이 문서가 새 canonical direction으로 두는 내용 |
| --- | --- | --- |
| Store-specific `5000 / 4936 / 64` | 당시 구현과 test의 역사적 사실 | Store가 Fabric ChatClef investigation session 전체의 canonical cap owner라는 해석은 폐기한다. Store cap은 필요하면 local pre-filter가 된다. |
| Mining 또는 다른 subsystem의 별도 `5000` | subsystem별 기존 diagnostics provenance | 같은 canonical session 이름과 cap event를 각 subsystem이 독립 소유하지 않는다. |
| 첫 8개 Store terminal group 보존 | 당시 reserve 산술과 current-source behavior | 8개 뒤 terminal 결과를 완전히 버리는 의미는 새 bounded terminal accounting 계약으로 대체한다. |
| diagnostics refactor의 facade와 event inventory | public facade, event meaning과 source-equivalence 기록 | 새 correctness slice의 session admission과 terminal accounting은 단순 folderization/refactor와 분리한다. |

다음은 supersede하지 않는다.

```text
event의 gameplay 의미와 terminal classification
Task selection, scheduling, completion과 ownership
input, Baritone goal/path, click와 container transfer
exception propagation과 cleanup
wire protocol과 command result schema
diagnostics OFF/BOUNDARY/VERBOSE 기본 의미
overlay/HUD 독립성
handoff production behavior
```

## 3. Handoff 체크포인트 동결

기준 커밋은 다음이다.

```text
a722ac2a17a813b62e605eaeac0fc9f96f7a1b5e
subject: test: checkpoint automatic-deposit handoff and diagnostics
```

커밋 기록과 companion report에는 다음 checkpoint 증거가 있다.

```text
clean forced build: historical pass for the checkpoint
tests: 378 total; 377 passed; 1 skipped non-handoff test
artifact/deployed JAR hash parity: historical checkpoint evidence
captured Minecraft post-place handoff: observed
held-chest stall in the captured scenario: not observed
all configurations and container combinations: not claimed
release complete: no
```

캡처된 정상 경계는 placement child stop/clear, 한 tick의 null barrier, 다음 tick
`OPEN_EXISTING`, transfer와 maintenance logical terminal이다. 이 증거는 캡처된
CHEST + Carry On 설치 상태의 시나리오에 한정한다.

Post-checkpoint test-only 상태는 다음처럼 별도 기록한다.

```text
production handoff hunk/meaning change: 0 reported
Java test-source scope: container generation/candidate transition and operation isolation
clean forced build: pass recorded
Java tests: 380 passed; 1 existing skipped
final JAR deployment: not performed
Minecraft runtime verification for the new tree: not performed
release qualification: not established
```

이 자동 검증은 checkpoint handoff production을 다시 여는 증거가 아니며, Stage 2/3 뒤
최종 JAR에 필요한 Stage 5/6 검증을 선취하지 않는다.

다음 repo-relative 위치의 checkpoint handoff 관련 hunk, method와 전용
collaborator 의미를 동결한다.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/
  adris/altoclef/tasks/container/DepositAllTask.java
    -> retained/ephemeral placement-owner and post-place handoff integration hunks
  lavi/minecraft/task/container/deposit/handoff/**
    -> dedicated handoff collaborators
  lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceTask.java
    -> general/trusted child-factory composition hunks
  lavi/minecraft/task/container/deposit/auto/maintenance/child/AutoDepositGeneralTaskFactory.java
    -> general handoff-enabled construction hunk
  lavi/minecraft/task/container/deposit/auto/maintenance/child/AutoDepositTrustedTaskFactory.java
    -> separate trusted construction path with no general handoff injection
```

라인 번호는 이후 unrelated 변경으로 이동할 수 있으므로 freeze identity는 다음 exact
method/hunk ledger로 판정한다.

| File | Frozen handoff method/hunk and meaning |
| --- | --- |
| `adris/altoclef/tasks/container/DepositAllTask.java` | owner/handoff를 받는 constructor와 pair validation, 기존 constructor의 ephemeral/disabled default, `onTick()`의 `deferAfterCompletedPlacement()` early boundary, placement fallback의 `_placementTaskOwner.getOrCreate(...)`, `deferAfterCompletedPlacement()` |
| `lavi/minecraft/task/container/deposit/handoff/DepositAllPlacementTaskOwner.java` | `ephemeral()`, `retaining()`, `getOrCreate(...)`, `currentTask()`, `clear(...)`, `retainingIdentity()`의 current identity semantics |
| `lavi/minecraft/task/container/deposit/handoff/DepositAllPostPlaceHandoff.java` | `disabled()`, `singleTick()`, `shouldDefer(...)`, `enabled()`의 exact-once single-tick semantics |
| `lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceTask.java` | constructor의 general/trusted factory composition, `generalTasks()`, `trustedTask()` |
| `lavi/minecraft/task/container/deposit/auto/maintenance/child/AutoDepositGeneralTaskFactory.java` | constructors와 `create(...)`의 operation-local retaining/single-tick composition |
| `lavi/minecraft/task/container/deposit/auto/maintenance/child/AutoDepositTrustedTaskFactory.java` | constructor와 `create(...)`의 trusted 경로 분리 및 general handoff 미주입 |

Diagnostics core, tool-selection shaping 또는 handoff test-only 작업을 이유로 위
handoff 관련 hunk와 의미를 수정하지 않는다. 이 동결은 unrelated future work까지
전체 파일 단위로 영구 금지한다는 뜻이 아니다. 이후 test-only 시나리오가 실패하면
같은 작업에서 production fix를 섞지 않고 `HANDOFF_DEFECT_FOUND`로 별도 보고한다.

## 4. 현재 diagnostics 불일치

### 4.1 Shared admission 부재

현재 `DiagnosticEventEmitter`는 verbose, boundary와 bounded boundary text를
출력하지만 Fabric ChatClef investigation session 전체의 admission budget을
소유하지 않는다. `ChatClefDiagnostics.logVerboseLine()`은 이 emitter를 거치지 않고
직접 출력한다.

따라서 현재 source에서 Store의 `TOTAL_SESSION_CAP = 5000`을 근거로
Fabric ChatClef investigation diagnostics 전체가 5000 event 이하라고 주장할 수 없다.

### 4.2 Store-local detail cap

현재 Store budget은 다음 산술을 사용한다.

```text
Store TOTAL_SESSION_CAP: 5000
Store NONCRITICAL_DETAIL_CAP: 4936
Store CRITICAL_RESERVE_CAP: 64
```

`StoreDepositOperationDetailBudget`은 Store noncritical detail admission만 세며,
최초 shared session-admission suppression을 claim하거나 canonical
`DIAGNOSTIC_SESSION_CAP_REACHED`를 정확히 한 번 출력할 owner가 아니다.

### 4.3 Terminal group exhaustion

현재 `StoreDepositCriticalBudget`은 최대 8개의 operation ID에 four-event terminal
group을 예약한다. `StoreDepositTerminalSummaryEmitter`는 8개가 소진되면
`STORE_DEPOSIT_TERMINAL_GROUP_RESERVE_EXHAUSTED` control event를 시도한 뒤 실제
terminal/effect/Baritone/coverage summary를 출력하지 않고 operation state를 purge한다.

현재 `StoreDepositTerminalSummaryEmitterTest`는 이 terminal summary 유실을
characterization success로 고정한다. 이 테스트는 역사적 current behavior의 증거로는
유효하지만 새 terminal-preservation acceptance contract로는 사용할 수 없다.

### 4.4 Tool-selection 출력

현재 `DiagnosticDeduplicator`는 key별 직전 fingerprint와 완전히 같은 연속 event만
제거한다. class 자체에는 time/window rate limit, session admission, suppression
summary 또는 key-store size limit이 없다. target position 또는 후보 fingerprint가
계속 바뀌면 반복 event가 계속 admission 후보가 될 수 있다.

Checkpoint runtime report는 high-volume tool-selection output과 per-store terminal
group reserve exhaustion을 별도 미완료 문제로 기록한다. 이는 캡처된 handoff 실패의
증거가 아니라 diagnostics boundedness의 release blocker다.

## 5. Cap 주장의 정확한 범위

안전한 headline contract는 다음이다.

```text
For one Minecraft client runtime, one shared admission authority admits at most
5000 budgeted structured investigation diagnostic events for Fabric ChatClef
1.20.1. The 64-event critical reserve is inside that total.
```

`모든 LAVI 로그`, `전체 process log`, `Minecraft 전체 출력`이 5000 이하라고
표현하지 않는다. 기존 정책은 diagnostics OFF에서도 existing operational lifecycle,
warning, error, crash와 terminal-failure log를 유지한다. 이를 investigation cap으로
새로 억제하면 diagnostics-only가 아니라 기존 운영 로그 의미의 변경이 된다.

### 5.1 Budget 포함 후보

정확한 producer inventory를 source 변경 전에 고정해야 하지만, 원칙적으로 다음
LAVI-owned structured investigation event가 budget 대상이다.

```text
BOUNDARY/VERBOSE investigation detail
bounded checkpoint and suppression summary
Store, mining, container, inventory and tool-selection investigation events
optional Carry On state-observation investigation events
budgeted terminal, exception, coverage and cap-control summaries
direct diagnostic output path whose sole purpose is investigation observation
```

### 5.2 Budget 제외

다음은 `<= 5000 budgeted event` claim 밖에 둔다.

```text
Minecraft/Fabric/AltoClef ordinary operational logs
existing always-on lifecycle, warning, error, crash and terminal-failure logs
overlay/HUD visibility notices
user-facing chat, command result and bridge payload output
non-diagnostic application/service output
```

이 제외는 중요 로그를 무제한 추가해도 된다는 뜻이 아니다. 각 로그의 기존 policy와
별도 boundedness가 계속 적용된다. 첫 source change 전에는 모든 LAVI diagnostic
producer를 inventory하고 각 event를 `BUDGETED_INVESTIGATION`,
`ALWAYS_ON_OPERATIONAL` 또는 `NOT_DIAGNOSTIC` 중 정확히 하나로 분류한다.

한 admitted structured event는 bounded formatter를 통해 한 physical line으로
출력하는 것을 기본으로 한다. 일반 exception stack trace나 외부 logger의 multiline
출력을 event count와 같은 것으로 꾸미지 않는다.

### 5.3 Runtime-purpose 분류와 anti-bypass

분류 기준은 logger level, event 이름, formatter 또는 최종 sink가 아니라 checkpoint에서
그 output이 수행하던 runtime purpose다. Investigation producer를 `warning`, `error`,
`terminal-failure`, `operational` 또는 다른 cap 제외 이름으로 바꾸어 shared admission을
우회하지 않는다. 새 output은 이름만으로 `ALWAYS_ON_OPERATIONAL`이 될 수 없으며, 기존
always-on operational 의미를 실제로 이어받는다는 source provenance가 필요하다.

하나의 call site가 operational과 investigation 두 목적을 함께 갖는 경우에는 producer
inventory에서 두 logical output을 별도 row로 기록한다.

```text
existing operational companion
  -> 기존 mode eligibility와 의미 유지
  -> investigation cap 대상 아님

investigation projection
  -> BUDGETED_INVESTIGATION
  -> local pre-filter 뒤 shared admission exactly once
  -> formatter/emitter가 다시 admission하지 않음
```

Operational companion을 새로 복제하거나 severity만 올려 같은 investigation payload를 cap
밖에서 이중 출력하지 않는다. 한 logical budgeted event는 producer에서 formatter까지의
전체 경로에서 shared admission을 정확히 한 번만 소비한다. Stage 1 producer inventory에는
최소 `file`, `method`, `eventName`, `runtimePurpose`, `modeEligibility`, `localPreFilter`,
`sharedAdmissionCall`, `physicalEmitter`, `operationalCompanion`을 기록한다.

## 6. Diagnostics session과 state ownership

### 6.1 Session lifetime

방향성 계약은 다음과 같다.

```text
one Minecraft client runtime process = one diagnostics session
new session: client process restart
deterministic test reset seam: allowed for tests only
production reset command or configuration: not part of the first slice
```

다음 경계는 session을 reset하지 않는다.

```text
OFF / BOUNDARY / VERBOSE mode change
world leave or join
dimension change
trace change
command or operation start/end
disconnect or reconnect inside the same client runtime
```

Mode toggle이 budget reset을 겸하면 cap을 반복 우회할 수 있다. 따라서 diagnostic
mode owner와 session lifetime owner를 분리한다. Production에서 명시적 reset 기능이
필요하다면 command/config/default 의미를 다루는 별도 승인 작업으로 둔다.

이때 유지되는 것은 session budget, session identity와 bounded cumulative accounting뿐이다.
Operation-local binding, active-operation state, tombstone와 sample은 각 authoritative
bounded lifecycle에서 expire/clear해야 하며 world/dimension/reconnect 경계를 넘어
재사용하지 않는다. `session reset 없음`을 stale operation state 보존으로 해석하지 않는다.

별도의 stable `diagnosticSessionId`를 채택한다. Diagnostics session owner가 process-lifetime
session 생성 시 정확히 한 번 만들고, client process restart 또는 deterministic test reset
때만 교체한다. 다음 identity와 alias하지 않는다.

```text
bridge transport session_id
connection or handshake identity
command requestId
traceId or correlationId
Store operationId or automatic autoOperationId
```

Bridge `session_id`는 reconnect 또는 handshake 수명을 가질 수 있지만
`diagnosticSessionId`는 같은 client process의 reconnect 뒤에도 유지된다. 이 값은 bounded
additive log-only provenance field이며 wire schema, command result 또는 gameplay identity를
바꾸지 않고 semantic dedupe fingerprint에도 넣지 않는다. Stage 1에서는 opaque ID 형식,
common event-envelope insertion point, composition-owner 생성 지점, shutdown과
deterministic test-reset seam만 exact file/method 수준으로 닫는다.

### 6.2 Mode eligibility와 active-operation transition

Mode eligibility는 local pre-filter와 shared admission보다 먼저 판정한다.

```text
OFF에서 비활성인 BUDGETED_INVESTIGATION event
  -> admission attempt 0
  -> admitted count 증가 0
  -> suppression count 증가 0
  -> canonical cap trigger 0

ALWAYS_ON_OPERATIONAL output
  -> 기존 OFF/BOUNDARY/VERBOSE 의미 유지
  -> investigation event의 companion이라는 이유로 억제하거나 중복하지 않음
```

Mode 전환은 `diagnosticSessionId`, admitted/suppressed counters 또는 process-lifetime Store terminal
ledger를 reset하지 않는다. 다만 OFF를 stale operation state 보존 근거로 사용하지 않는다.
Active Store operation에 대한 bounded accounting은 다음처럼 다룬다. OFF에서는 현재
Slice A의 complete-no-op 의미를 보존하며 investigation ledger도 mutate하지 않는다.

```text
enabled -> OFF linearization
  -> OFF publish 전에 active operation-local binding/sample/tombstone을 한 번 invalidate
  -> process-lifetime Store ledger에 PARTIAL_MODE_DISABLED coverage gap을 한 번 기록
  -> invalidation은 diagnostics bookkeeping만 정리하며 Task/input/path/container state를 건드리지 않음

OFF가 효력을 가진 뒤
  -> raw investigation event admission/emission 0
  -> fingerprint/rate/suppression/terminal ledger mutation 0
  -> pre-OFF binding revive/reuse 0

OFF -> BOUNDARY/VERBOSE
  -> 새 observation epoch 사용
  -> 아직 active인 operation은 새 bounded binding으로만 관찰
  -> pre-OFF context는 UNAVAILABLE이며 MODE_TRANSITION_COVERAGE_GAP을 유지
  -> OFF 동안 놓친 terminal이나 detail을 나중에 합성하지 않음

OFF에서 시작하고 끝난 operation
  -> investigation ledger 등록과 suppression accounting 0
  -> 나중에 terminal을 소급 생성하거나 totalTerminalObserved에 포함하지 않음

OFF에서 시작해 BOUNDARY/VERBOSE에서 처음 관찰된 active operation
  -> 새 bounded binding을 만들 수 있음
  -> pre-observation context는 UNAVAILABLE bucket과 coverage gap으로 명시
```

따라서 `totalTerminalObserved`는 모든 gameplay terminal의 전역 수가 아니라 해당
diagnostics session의 Store ledger에 등록되어 authoritative finalization을 관찰한 logical
terminal 수다. 더 정확히는 investigation accounting이 eligible한 시점에 authoritative
finalization boundary가 전달된 unit만 포함한다. Automated mode-transition test seam은
OFF/BOUNDARY/VERBOSE를 모두 다루되, production runtime에 존재하지 않는 mode 변경 command를
새로 요구하지 않는다.

Eligibility race는 Stage 1에서 `modeEpoch/eligibilityToken` 검증 또는 전체 경로의
client-thread serialization 중 하나로 정확히 고정한다. 어떤 방식을 택하든 순서는 다음을
증명해야 한다.

```text
initial OFF -> local fingerprint/rate state mutation 0
enabled eligibility token -> local pre-filter가 같은 mode epoch에서만 state를 읽고 갱신
shared owner -> admission 전에 같은 token/epoch를 재검증
epoch mismatch 또는 OFF 전환 선행 -> MODE_INELIGIBLE
MODE_INELIGIBLE -> shared suppression count 0, canonical cap trigger 0, payload capture 0
```

Mode transition과 producer path가 경쟁하면 chosen linearization이 어느 쪽이 먼저인지
결정한다. OFF가 먼저면 producer state mutation은 0이다. Producer가 먼저면 admission
disposition까지 old epoch에서 닫고, granted emission과 그 completion/failure accounting도
OFF publish 전에 settle한 뒤 old-epoch state를 enabled->OFF invalidation에서 폐기한다.
OFF 효력 뒤로 formatter/sink call이나 admission/emission accounting을 넘기지 않는다.

Terminal path에는 일반 producer보다 강한 교차 계약을 적용한다. Stage 1의 선택은
`SERIALIZED_TERMINAL_MODE_LINEARIZATION`으로 고정한다.

```text
same non-I/O synchronization domain, serialized with mode transition
  -> mode/epoch eligibility check
  -> authoritative token/finalizer acceptance
  -> T += 1; A += 1
  -> shared four-slot reservation disposition
       denied: A -= 1; S += 1
       granted: A -= 1; G += 1; P += 1; register old-epoch emission lease

outside the accounting lock
  -> bounded formatting and physical emission for a granted lease

same synchronization domain
  -> P -= 1; C += 1 or F += 1
  -> release the old-epoch emission lease

enabled -> OFF
  -> stop new eligibility from entering the old epoch
  -> defer OFF publication until every old-epoch admission disposition and emission lease settles
  -> invalidate operation-local diagnostics state and record the one declared coverage gap
  -> publish OFF
```

따라서 OFF가 먼저 linearize되면 authoritative diagnostic finalizer, `T/A/G/S/P/C/F`,
shared admission과 physical emission이 모두 0이다. Enabled terminal path가 먼저면 `A`가
mode 전환 사이에 고립되거나 `P`가 OFF 효력 뒤에 정산되는 경로가 없다. Logger I/O는
accounting lock 밖에서만 수행한다. Stage 1은 기존 client-thread serialization으로 이를
증명하거나, gameplay/engine thread를 block하지 않는 diagnostics-owned mechanism의 exact
owner와 file/method를 제시해야 한다. 둘 중 어느 것도 증명하지 못하면 Stage 1은 OPEN이고
Stage 2는 BLOCKED다.

### 6.3 Single-responsibility admission authority

`global diagnostics manager`나 mutable service locator를 만들지 않는다. Shared owner는
오직 다음만 소유한다.

```text
session identity and lifetime
ordinary and critical admission counts
canonical cap-event claim
bounded admitted/suppressed counters by fixed event family
```

다음 책임은 별도 owner로 유지한다.

```text
formatting and physical emission
subsystem semantic fingerprinting, rate limiting and sampling
Store-specific terminal classification and accounting
Mining-specific or tool-selection-specific summaries
payload construction and domain observation
```

권장 ownership은 composition-root-owned long-lived instance와 명시적 dependency
delegation이다. Public/implicit mutable static state, service locator 또는 subsystem이
각자 생성한 pseudo-session owner를 추가하지 않는다. 기존 static facade를 통해
접근해야 한다면 내부 owner의 construction, readers, writers, synchronization, reset과
shutdown을 명시한다. Composition-root injection으로 implicit/static process-wide mutable
access를 피할 수 없다면 source work 전에 `AGENTS.md` shared-state exception report를
완료하고 작업을 중단한 뒤 명시적 승인을 받아야 한다. 이 보고에는 owner, readers,
writers, initialization, reset, shutdown, reconnect, thread model, synchronization과
tests를 포함한다.

State는 private, bounded, synchronized 또는 atomic이며 log admission만 바꾼다.
thread scheduling, event ordering, observed operation 또는 exception propagation을
변경하지 않는다.

## 7. 5000 hard cap과 canonical cap event

### 7.1 숫자 불변식

초기 방향은 기존 값을 유지한다.

```text
hard session cap:       5000 budgeted events
ordinary ceiling:       4936 budgeted events
critical reserve:         64 budgeted events, inside 5000

0 <= admittedBudgetedEvents <= 5000
ordinary + critical <= 5000
```

Reserve는 5000 뒤에 추가되는 bypass bucket이 아니다. 어떤 priority event도 hard cap을
뚫지 않는다. Store, mining, tool-selection과 다른 subsystem의 event는 canonical
admission을 정확히 한 번만 통과하며 local gate와 shared gate의 double admission도
금지한다.

### 7.2 Critical priority

정확한 64-slot numeric partition은 Stage 1에서 producer inventory 뒤 고정한다. 임의의
숫자를 먼저 정하여 Store, mining, StoreHome, exception 또는 coverage event를 굶기지
않는다.

```text
CRITICAL_PARTITION_STATUS: OPEN
SUM_OF_ALL_CRITICAL_SUBQUOTAS_REQUIRED: 64
STAGE_2_BEFORE_EXACT_NUMERIC_TABLE: BLOCKED
```

최소 priority는 다음이다.

1. canonical cap/session accounting
2. abnormal terminal, exception과 coverage gap
3. routine terminal accounting
4. bounded suppression/control summary

Canonical cap event 전용 slot은 다른 critical traffic이 소비할 수 없다. Abnormal
terminal이 먼저 발생한 routine success 때문에 전부 밀리지 않도록 routine,
abnormal과 aggregate quota를 분리한다. 정확한 slot 수, aggregate cadence와
mode-eligible final snapshot slot은 아직 구현 승인 전 결정 gate다.

Stage 1의 numeric table에는 다음 row와 column을 빠짐없이 둔다.

| Required pool | Unit | Slots | Required rule |
| --- | --- | --- | --- |
| canonical cap | single event | exactly 1 | permanently non-borrowable |
| mode-eligible final snapshot (`CLEAN_TEARDOWN` or `TEST_RESET`) | single event | exactly 1 | permanently non-borrowable; OFF에서는 attempt 자체가 없음 |
| abnormal Store terminal groups | four-event group | positive multiple of 4 | routine traffic이 차용 금지 |
| routine Store terminal groups | four-event group | multiple of 4 | abnormal pool을 차용 금지 |
| exception/coverage | single event or explicitly declared group | exact value required | abnormal visibility를 starvation하지 않음 |
| aggregate/checkpoint | single event | exact value required | periodic, milestone, first-suppression ownership 구분 |
| non-Store terminal/coverage | declared unit | exact value required | Store ledger와 owner/total을 합산하지 않음 |
| suppression/control | single event | exact value required | canonical cap name 재사용 금지 |

실제 table은 각 row에 `eventFamilies`, `slots`, `unitSize`, `borrowFrom`, `lendTo`,
`nonBorrowable`, `exhaustionOutcome`을 기록하고 합계가 정확히 64임을 증명한다. Borrowing
graph는 acyclic이어야 한다. Routine은 abnormal/cap/final-snapshot capacity를 빌릴 수
없으며, abnormal이 routine의 unused capacity를 빌리는 경우에도 정확히 4-slot 단위로만
한다.

Four-event group pool은 항상 4의 배수를 유지하고 admission과 borrowing도 4-slot 단위이므로
그 pool 자체에 1~3 fragment를 만들지 않는다. 4개 slot을 원자적으로 제공할 수 없으면
group 전체를 admission 전에 억제한다. 남은 1~3 slot은 명시적인 single-event pool에서만
존재할 수 있고 해당 single-event family에 쓰거나 미사용으로 남긴다. Single-event pool의
fragment를 terminal-group pool에 합쳐 쓰지 않는다. Critical subquota exhaustion은 local accounting/coverage
결과이며, shared hard-cap도 실제로 막은 경우가 아니면 canonical shared-admission cap trigger가
아니다.

### 7.3 Admission과 physical emission 상태

Hard cap은 granted budgeted event slot 수에 대한 계약이다. Formatter/logger/sink가 durable
output을 항상 성공시킨다는 계약이 아니다.

```text
ADMISSION_GRANTED
  shared slot이 atomic하게 commit되고 hard-cap accounting에 포함됨

SUPPRESSED_BEFORE_ADMISSION
  shared slot이 없으며 formatter/sink를 호출하지 않음

EMISSION_COMPLETED
  admitted bounded record 또는 group의 모든 예정 sink call이 정상 반환함
  deliveryStatus=UNVERIFIED; durable write를 증명하지 않음

EMISSION_FAILED_AFTER_ADMISSION
  admission은 성공했으나 formatter/sink completion이 detectably incomplete함
```

Admission 뒤 실패한 slot은 refund하지 않고 diagnostics를 보완하려고 재시도하지 않는다.
Formatter/sink failure 관측을 이유로 gameplay, Task, Baritone 또는 container exception을 새로
catch/suppress하지 않는다. Diagnostic formatter가 자기 bounded formatting 실패만 좁게
보고할 수 있으며 observed engine exception 의미를 소비하지 않는다. `PrintStream`처럼 I/O
failure를 정상 반환 뒤 숨길 수 있는 sink에서는 `EMISSION_COMPLETED`가 오직
`EMISSION_CALLS_RETURNED`를 뜻하며 delivery/durability evidence가 아니다.

### 7.4 `DIAGNOSTIC_SESSION_CAP_REACHED` exactly-once

Canonical event의 owner는 shared session admission authority 하나뿐이다.

```text
otherwise-emittable budgeted event가 shared capacity 때문에 거절되지 않음
  -> canonical claim count 0

otherwise-emittable ordinary event가 4936 ordinary ceiling 때문에 처음 거절되고
critical reserve가 남아 있음
  -> trigger=ORDINARY_CEILING_RESERVE_ACTIVE

otherwise-emittable ordinary 또는 critical event에 사용할 eligible non-cap shared slot이
없고 dedicated cap token은 여전히 reserve/commit 가능한 상태에서 처음 거절됨
  -> trigger=SHARED_HARD_CAP

first shared-capacity trigger wins
  -> subsystem, operation, thread 또는 뒤의 rejection 수와 무관하게 claim exactly 1

dedupe/rate/family/operation/critical-subquota-only suppression
  -> canonical trigger 0
```

Canonical event 자신도 permanently pre-reserved된 전용 critical slot을 소비하며 hard cap
안에 포함된다. 한 synchronized/atomic admission operation이 다음을 하나의 linearization
boundary에서 수행한다.

```text
mode eligibility confirmation
-> slot decision
-> shared suppression accounting
-> first-trigger claim
-> dedicated cap-slot reservation/commit
-> immutable admission decision return
```

Subsystem과 emitter 사이에 check-then-set을 나누지 않는다. Canonical record는 이미 확보한
internal token으로 출력하며 ordinary/shared admission을 재귀 호출하지 않는다. Formatter나
sink 실패 뒤 claim/admission을 재시도하지 않는다.

Exactly-once를 다음 상태로 분리한다.

```text
capEventClaimed
capEventAdmissionGranted
capEventEmissionPending
capEventEmissionCompleted
capEventEmissionFailedAfterAdmission
```

Claim과 admission은 session당 0-or-1이며 다음 식을 만족한다.

```text
0 <= capEventClaimed <= 1
capEventClaimed == capEventAdmissionGranted
capEventAdmissionGranted
  == capEventEmissionPending
   + capEventEmissionCompleted
   + capEventEmissionFailedAfterAdmission
```

식 위반은 coverage gap이다. Physical completion은 sink failure 때문에 0-or-1이며
무조건 exactly-one 또는 durable delivery라고 과장하지 않는다. Payload는 최소한 다음을 구분한다.

```text
diagnosticSessionId
hardCap=5000
ordinaryBudget=4936
trigger=ORDINARY_CEILING_RESERVE_ACTIVE|SHARED_HARD_CAP
admittedTotalBefore
admittedTotalAfter
ordinaryUsedBefore/After
criticalUsedBefore/After
reserveRemainingBefore/After
firstSuppressedEvent
firstSuppressedSubsystem
rejectedPriority
rejectedFamily
bounded admitted/suppressed counts by subsystem or fixed event family
```

Subsystem-local 제한은 canonical 이름을 재사용하지 않는다. 필요하면 다음처럼 별도
이름과 owner를 둔다.

```text
DIAGNOSTIC_FAMILY_CAP_REACHED
TOOL_SELECTION_RATE_LIMIT_REACHED
STORE_DETAIL_BUDGET_EXHAUSTED
```

Ordinary ceiling 이후에는 nonterminal ordinary detail만 억제하고 eligible critical
event는 reserve를 사용한다. Critical quota까지 소진되면 hard cap을 우회하지 않는다.
Store terminal은 8절의 bounded ledger에 계속 accounting한다. 다른 subsystem은 Stage 1에서
별도의 bounded accounting owner가 증명된 경우에만 같은 보존을 주장하며, owner가 없으면
명시적 coverage gap으로 남긴다.

### 7.5 Counter와 sequence overflow

모든 session, admission, suppression, terminal, overflow, snapshot revision/sequence와
terminal sequence counter는 non-negative saturating `long` 계약을 사용한다.

```text
increment/add가 Long.MAX_VALUE를 넘으려 함
  -> 값은 Long.MAX_VALUE에 고정
  -> counterSaturated=true
  -> fixed saturatedCounterFlags에 해당 counter 기록
  -> wraparound, 음수 전환 또는 sequence 재사용 금지
```

Saturated sequence를 새 identity처럼 재사용하지 않는다. 관련 saturation flag가 false일
때만 reconciliation equality를 exact로 주장한다. 하나라도 포화되면
`reconciliationStatus=SATURATED`로 두고 fabricated equality를 출력하지 않는다.

`sum(known counts)`를 계산할 때도 같은 saturating add를 사용한다. 합계 계산이 포화되면
fixed `RECONCILIATION_SUM` flag를 세우고 equality를 exact로 판정하지 않는다. Terminal 또는
snapshot sequence가 `Long.MAX_VALUE`에 도달한 뒤에는 같은 MAX 값을 새 sequence로
재사용하지 않고 `sequenceAvailable=false`, `sequenceUnavailableReason=UNAVAILABLE_SATURATED`
로 전환한다. `ledgerRevision`은 saturation 전에는 strict increase, 포화 뒤에는
non-decreasing이다.

### 7.6 Direct-output migration order

Ordinary budgeted path는 다음 순서를 사용한다.

```text
mode eligibility
-> already-computed semantic local fingerprint
-> local rate/dedupe/per-operation pre-filter
-> if local pre-filter denies: local suppression accounting and stop
-> if local pre-filter passes: exactly one shared admission request
-> if shared admission denies: shared owner alone records shared suppression and stop
-> if granted, passive/lazy bounded payload capture
-> bounded formatting
-> physical emission
-> completion/failure accounting
```

Terminal path는 §6.2의 mode-transition serialization 안에서 다음 순서를 사용한다.

```text
mode/epoch eligibility check
-> authoritative finalizer
-> one Store ledger transaction records terminal and admission-pending state
-> terminal priority/group request
-> one atomic four-slot shared reservation settles admission pending before mode transition can publish OFF
-> granted reservation records emission-pending state
-> bounded formatting/emission of four records
-> completion/failure transaction settles emission pending before OFF publish
```

Local cap은 ordinary detail을 shared gate 전에 줄일 수 있지만 authoritative terminal을 ledger
accounting 전에 버릴 수 없다. `ChatClefDiagnostics.logVerboseLine()`, lifecycle/warning
output, `DiagnosticEventEmitter`, Carry On direct warning fallback, mining/container/StoreHome
cap emitters와 Store bounded logger를 producer inventory에서 개별 분류한다. Shared emitter를
무조건 gate하여 always-on operational output을 억제하거나, direct path를 그대로 남겨
shared claim을 우회하지 않는다. Global `System.out`/`Debug` interception은 사용하지 않는다.

## 8. Bounded terminal accounting

### 8.1 보존 범위와 authoritative logical unit

고정된 5000-event cap 아래에서 무한한 operation의 four-event raw terminal group을
모두 출력할 수는 없다. 따라서 보존 계약은 다음과 같다.

```text
모든 logical terminal raw line 보존: 보장하지 않음
eligible Store-root authoritative terminal의 bounded accounting 반영: 보장 대상
모든 과거 operation ID의 개별 재구성: 보장하지 않음
```

현재 source에서 four-summary Store group의 정확한 unit과 first/only authoritative
transition은 다음이다.

```text
ledger unit: STORE_ROOT_OPERATION
identity: StoreDepositOperationContext.operationId
authoritative state transition:
  StoreDepositOperationState.markTerminalFinalized() false -> true
owner/call boundary:
  StoreDepositTerminalSummaryEmitter.emitAndPurge(...)
competing triggers for the same unit:
  NATURAL_FINISH
  ROOT_STOP_END
```

두 trigger 중 먼저 accepted된 `false -> true` transition만 `totalTerminalObserved`를
output admission 전에 정확히 한 번 증가시킨다. 나중 trigger는 같은 Store root를 다시
terminalize하지 않는다. Ledger는 gameplay finalizer, Task completion owner 또는 새 terminal
classifier가 아니라 이 existing diagnostic finalization boundary의 bounded accounting
collaborator다.

현재 `markTerminalFinalized()`는 plain boolean transition이다. Stage 1은 두 trigger가 같은
client-thread lifecycle에서 serialized된다는 caller evidence를 제시하거나, diagnostics-only
state transition 자체만 atomic하게 만드는 exact hunk를 제안해야 한다. 이를 위해 Task
scheduling, callback thread, gameplay ordering 또는 exception behavior를 바꾸지 않는다.

기존 finalization owner가 duplicate attempt를 거절한 경우에는 terminal을 다시 열거나
재분류하거나 purge하지 않고 diagnostic-only duplicate-attempt counter만 bounded하게
증가시킬 수 있다. 이 counter는 unique terminal total이나 classification/scope count를
증가시키지 않는다.

다음 boundary는 같은 `STORE_ROOT_OPERATION` total을 증가시키지 않는다.

| Boundary | Relationship to Store-root total |
| --- | --- |
| maintenance logical terminal | `autoOperationId`를 쓰는 automatic-run companion scope; 별도 ledger |
| pressure-owned run close | automatic lifecycle companion; 별도 total |
| `RUNNING -> WAIT_FOR_REARM` 또는 coverage close | automatic lifecycle/coverage companion; 별도 total |
| transfer attempt, route child 또는 interaction close | child/attempt observation; Store-root terminal 아님 |
| `AutoDepositTrustedStoreTask.finish(...)` outcome | trusted task outcome companion; root lifecycle finalizer와 중복 집계 금지 |
| user Task/command terminal과 bridge result | command lifecycle ledger; `diagnosticSessionId` 또는 Store total과 합산 금지 |
| four terminal summary records | 한 Store-root terminal의 projections; 각각 total 증가 금지 |

한 automatic maintenance run에 Store root가 여러 개 있을 수 있고, 한 Store root에서 여러
companion terminal/coverage event가 파생될 수 있다. `operationId`, `autoOperationId`, command
`requestId`와 bridge `session_id`는 correlation할 수 있지만 서로 다른 ledger total을 더해
`all terminals`라는 수치를 만들지 않는다.

### 8.2 Missing binding, eviction과 unique identity gate

현재 source는 `bindings.stateFor(task) == null`이면 authoritative Store finalizer까지 도달하지
않는다. 따라서 source 변경 전 상태에서 registry overflow, missing binding 또는 eviction이
발생해도 모든 unique terminal이 `totalTerminalObserved`에 들어간다고 주장할 수 없다.

Stage 1은 ancillary context registry와 분리된 bounded authoritative operation token을 terminal
finalization까지 보존하는 exact owner/lifetime/bound를 증명해야 한다. 그 token이 증명되면
classification, scope 또는 context가 없어도 unique finalization total은 증가하고 세부값만
UNAVAILABLE bucket으로 내려간다.

Exactness를 주장하는 동안 unfinalized authoritative token을 ancillary registry eviction
대상으로 삼지 않는다. Stage 1은 supported gameplay의 최대 concurrent Store-root operation
수를 source/caller evidence로 bound하고 token capacity가 그 bound를 수용함을 증명해야 한다.
그 bound를 넘거나 token allocation이 실패하면 조용히 oldest token을 버리지 않고 즉시
`COVERAGE_GAP`으로 전환하며 Stage 2 acceptance는 fail이다.

Stable non-reused token 없이 들어온 contextless callback은 unique terminal로 추측하지 않는다.

```text
terminalBoundaryWithoutContextCount += 1
ledgerCoverageGapCount += 1
reconciliationStatus = COVERAGE_GAP
totalTerminalObserved: unchanged because unique unit was not proven
```

`terminalBoundaryWithoutContextCount`는 unique terminal 수가 아니라 duplicate가 포함될 수
있는 contextless boundary callback observation 수다.

이 fallback은 acceptance 성공이 아니라 결함의 bounded visibility다. Stage 1에서 stable token
보존을 bounded하게 증명하지 못하면 terminal-preservation contract는 열려 있고 Stage 2
implementation 승인을 요청하지 않는다. Active-registry overflow나 eviction을 조용히
terminal 유실로 바꾸거나 contextless callback을 임의 dedupe하여 exact total로 꾸미지 않는다.

### 8.3 Ledger fields와 reconciliation

최소 accounting은 다음을 포함한다.

```text
totalTerminalObserved
terminalSequence
fullTerminalGroupAdmissionPending
fullTerminalGroupAdmissionGranted
fullTerminalGroupSuppressedBeforeAdmission
fullTerminalGroupEmissionPending
fullTerminalGroupEmissionCompleted
fullTerminalGroupEmissionFailedAfterAdmission
duplicateFinalizationAttempts
fixed terminal-classification counts
terminalClassificationUnavailableCount
fixed terminal-scope counts
terminalScopeUnavailableCount
terminalContextAvailableCount
terminalContextUnavailableCount
terminalBoundaryWithoutContextCount
ledgerCoverageGapCount
first and last suppressed operation identity, bounded text
first and last tick or sequence
recent K abnormal terminal samples in a fixed-size ring buffer
omittedSampleCount
reconciliationStatus=EXACT|COVERAGE_GAP|SATURATED
```

`manual`과 `automatic general`은 current source에서 proven Store-root registration path다.
`trusted`는 Stage 1에서 exact trusted-child operation token과 root finalizer binding을 증명한
경우에만 Store-root scope다. 증명되지 않으면 `AutoDepositTrustedStoreTask.finish(...)` outcome은
trusted companion ledger에 남고 Store-root total에 넣지 않는다. 어느 경우에도 internal
outcome event를 root lifecycle terminal과 이중 집계하지 않는다. Shared admission authority는
이 domain enum을 알지 않고 generic priority와 admission 결과만 소유한다.
Unknown/unavailable state를 success, cancel 또는 error로 임의 분류하지 않는다.

Saturation flag가 false이고 unique token coverage가 complete일 때 다음 식이 항상 닫혀야
한다.

```text
T = totalTerminalObserved
A = fullTerminalGroupAdmissionPending
G = fullTerminalGroupAdmissionGranted
S = fullTerminalGroupSuppressedBeforeAdmission
P = fullTerminalGroupEmissionPending
C = fullTerminalGroupEmissionCompleted
F = fullTerminalGroupEmissionFailedAfterAdmission

T == A + G + S
G == P + C + F

T == sum(known terminal-classification counts)
   + terminalClassificationUnavailableCount

T == sum(known terminal-scope counts)
   + terminalScopeUnavailableCount

T == terminalContextAvailableCount
   + terminalContextUnavailableCount
```

Settled boundary에서는 `A == 0`, `P == 0`이므로 reviewer의 최종식인 `T == G + S`,
`G == C + F`가 성립한다. In-flight snapshot도 모순 없이 닫히도록 다음 transition을 사용한다.

```text
accepted authoritative finalizer ledger transaction
  -> T, A, terminalSequence, classification/scope/context와 ledgerRevision 갱신

shared reservation denied
  -> A -= 1; S += 1

shared four-slot reservation granted
  -> A -= 1; G += 1; P += 1

all intended emission calls returned
  -> P -= 1; C += 1; deliveryStatus=UNVERIFIED

detectable formatting/emission failure
  -> P -= 1; F += 1; ledgerCoverageGapCount += 1
```

각 화살표의 ledger mutation은 해당 owner 안에서 atomic transaction이다. Logger I/O를 ledger
lock 안에서 실행하지 않는다. Reservation 또는 emission code가 비정상 종료해 A/P가 남으면
snapshot이 그 in-flight state를 드러내고 reconciliation은 `COVERAGE_GAP`으로 내려간다.

Missing classification/scope/context는 terminal 자체를 삭제하지 않고 각 UNAVAILABLE bucket으로
내린다. 반면 stable token 없이 uniqueness도 확인할 수 없는 callback은 위 식의 T에 넣지 않고
`terminalBoundaryWithoutContextCount`와 coverage gap으로 별도 보고한다. Counter saturation이
발생하면 `SATURATED`, 그 밖의 gap이 있으면 `COVERAGE_GAP`, 모든 식과 identity coverage가
닫힐 때만 `EXACT`다.

Memory contract는 다음과 같다.

```text
fixed-domain counters only
no unbounded operation-ID set
fixed-size sample ring
bounded text/key length
bounded subsystem/family registry
bounded authoritative-token registry with explicit lifetime and overflow outcome
```

### 8.4 Four-event terminal group

한 Store terminal의 네 summary는 admission 단계에서 다음 중 하나여야 한다.

```text
four slots atomically reserved -> fullTerminalGroupAdmissionGranted
four raw summaries all suppressed before formatter -> fullTerminalGroupSuppressedBeforeAdmission
```

두 개만 admission하고 나머지를 버리는 partial admission은 금지한다. 이는 physical
I/O 네 줄의 완전한 원자성을 보장한다는 뜻은 아니다. Formatting 또는 logger failure가
발생하면 `fullTerminalGroupEmissionFailedAfterAdmission`과 coverage gap으로 다룬다. 네
예정 sink call이 모두 정상 반환했을 때만 `fullTerminalGroupEmissionCompleted`다. Admission
뒤 slot refund나 diagnostic retry를 하지 않으며, 새 catch/suppression으로 observed engine
exception 의미를 바꾸지 않는다.

Historical current source의 8-group 한도는 새 acceptance 값이 아니다. Stage 1에서 정한
group quota가 소진된 뒤 full group을 생략할 수는 있지만 다음은 허용하지 않는다.

```text
terminal 발생 사실의 완전한 유실
classification/scope aggregate 누락
routine success quota가 abnormal terminal visibility를 전부 소진
unbounded operation identity retention
reserve-exhausted control 하나만 남기고 accounting state purge
```

### 8.5 Aggregate snapshot provenance

Aggregate snapshot은 delivery base가 필요한 delta가 아니라 cumulative contract로 고정한다.
모든 snapshot admission은 reservation linearization 시점에 BOUNDARY 또는 VERBOSE일 때만
허용한다. `CLEAN_TEARDOWN`과 `TEST_RESET`도 같은 mode eligibility를 적용하며 OFF이면 아래
`SKIPPED_MODE_OFF` 규칙을 따른다.

```text
diagnosticSessionId
snapshotKind=PERIODIC|MILESTONE|FIRST_SUPPRESSION|CLEAN_TEARDOWN|TEST_RESET
snapshotSequence
previousCreatedSnapshotSequence|NONE
previousEmissionCallsReturnedSnapshotSequence|NONE
ledgerRevision
coveredThroughTerminalSequence
accountingMode=CUMULATIVE
snapshotSelfAccounting=ADMISSION_INCLUDED_EMISSION_OUTCOME_EXCLUDED
fullTerminalGroupAdmissionPending
fullTerminalGroupEmissionPending
all reconciliation counters and status
coverageStatus and fixed coverage-gap flags
counterSaturated and saturatedCounterFlags
```

`ledgerRevision`은 terminal, duplicate, admission/suppression, emission completion/failure,
coverage gap과 saturation을 포함한 모든 accounting mutation 때 증가한다. Snapshot slot을
reserve/commit한 뒤 같은 ledger transaction에서 `snapshotSequence`를 생성하고 immutable
snapshot을 capture하며, 이 transaction도 `ledgerRevision`을 한 번 증가시킨다.
`coveredThroughTerminalSequence`는 snapshot이 포함하는 가장 높은 authoritative terminal
sequence이며 generic event sequence가 아니다. T에 이미 반영된 admission/emission-pending
terminal도 포함하고 A/P counter로 그 상태를 드러낸다. Snapshot은 ledger linearization
boundary 안에서 immutable하게 capture하고 formatting/emission은 그 뒤 수행한다.

Snapshot 자신의 critical-slot admission은 자기 cumulative payload에 포함하지만, 자기
emission completion/failure는 capture 뒤 발생하므로 다음 snapshot에 반영한다.
`previousCreatedSnapshotSequence`는 직전 capture를, 선택적
`previousEmissionCallsReturnedSnapshotSequence`는 모든 emission call이 정상 반환한 가장
최근 snapshot을 뜻한다. 정상 반환은 durable delivery 증명이 아니다.

다음 종류를 구분한다.

```text
PERIODIC/MILESTONE
  -> Stage 1에서 exact cadence와 quota 확정

FIRST_SUPPRESSION
  -> 첫 terminal-group suppression의 bounded cumulative evidence

CLEAN_TEARDOWN
  -> mode-eligible일 때만 diagnostic state clear 전에 capture/emission 시도
  -> permanently non-borrowable final-snapshot slot 사용

TEST_RESET
  -> mode-eligible일 때 old session final snapshot admission/emission attempt가
     COMPLETED 또는 FAILED로 정산된 뒤 reset
  -> delivery failure 때문에 reset을 영구 block하지 않음
  -> 다음 session은 새 diagnosticSessionId와 zeroed bounded state

OFF CLEAN_TEARDOWN
  -> snapshot admission/emission/accounting 0
  -> process teardown의 old state object 폐기는 OFF-time ledger mutation으로 세지 않음

OFF TEST_RESET
  -> out-of-band test-control result=SKIPPED_MODE_OFF
  -> snapshot admission/emission/accounting, ledgerRevision 증가와 새 diagnostic event 0
  -> old test-session object를 폐기하고 새 diagnosticSessionId의 zeroed state를 생성
```

`SKIPPED_MODE_OFF`는 test harness가 관찰하는 control result이며 snapshot kind, diagnostic
event 또는 in-session counter가 아니다. OFF에서 final snapshot을 예외적으로 허용하지
않으므로 §6.2의 complete-no-op 계약을 보존한다.

In-memory ledger는 OS kill, JVM crash, 전원 차단 또는 abrupt process termination 직전 값의
exact physical persistence를 보장할 수 없다. 마지막 성공 snapshot 이후의 gap을 crash-time
limitation으로 명시하고 `모든 terminal이 로그에 영구 보존됨`이라고 표현하지 않는다.

## 9. 작업 단위 분리

### A. Handoff checkpoint freeze

```text
baseline: a722ac2a17a813b62e605eaeac0fc9f96f7a1b5e
production handoff changes: 0
classification: handoff-functional checkpoint, not release-ready
```

### B. Diagnostics Core Boundedness

하나의 correctness review unit로 다룬다.

```text
budgeted producer inventory and classification
session lifetime and admission ownership
shared-session 5000 / ordinary 4936 / critical 64 invariant
exact 64-slot partition, borrowing and fragmentation
canonical cap claim/admission exactly-once for ordinary-ceiling or hard-cap rejection
process-lifetime diagnosticSessionId distinct from bridge session_id
mode eligibility and OFF no-op accounting
direct budgeted-output path migration without global System.out interception
Store-root authoritative terminal unit and bounded ledger
classification/scope/context UNAVAILABLE reconciliation
four-event terminal-group admission all-or-none plus physical-emission outcome
cumulative aggregate snapshot provenance and clean-teardown slot
saturating counters and sequence behavior
legacy terminal-loss characterization test replacement
mixed-subsystem black-box contract tests
```

Session admission과 Store terminal ledger는 별도 single-responsibility component로
유지하되, reserve/admission 순서가 맞물리므로 같은 correctness review unit에서
검증한다. Handoff production, gameplay Task, Forge/MineMind와 wire protocol은 제외한다.

### C. Tool-selection Diagnostic Shaping

Diagnostics core 뒤의 별도 review unit다.

```text
tick/window rate limit
semantic decision fingerprint
coarse unchanged sampling
bounded suppression summary
bounded dedupe/rate state
meaningful decision or selected-tool transition immediate visibility
```

Tool-selection event는 B 단계부터 shared admission을 통과해야 하지만, fingerprint와
rate policy는 C 단계에서 바꾼다. B와 C를 한 변경에 섞어 cap 효과와 signal-shaping
효과를 모호하게 만들지 않는다. C가 끝나기 전에는 hot-path bounded logging 준수를
완료했다고 판정하지 않는다.

### D. Handoff container-type lifecycle test-only

현재 제공된 상태에서는 container generation/candidate 전환과 operation isolation의 Java
test source가 이미 test-only로 보강되었고, clean forced build 및 `380 passed / 1 existing
skipped`가 기록되었다. 이 문서 재검토는 해당 Java diff나 build log를 재실행하지 않으며,
final JAR runtime qualification은 여전히 Stage 6에 남는다.

이 work unit의 허용 범위는 다음 test package뿐이다.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/
  lavi/minecraft/task/container/deposit/handoff/**
```

완료 조건은 다음과 같다.

1. CHEST 의미의 placement child가 scheduler-owned active 상태다.
2. 다음 parent tick 전 요청 후보가 BARREL 의미로 바뀐다.
3. 기존 active child identity를 유지하며 BARREL factory는 호출되지 않는다.
4. 원래 child 완료 시 실제 scheduler-owned child만 정확히 한 번 stop한다.
5. 같은 tick에는 새 placement/open child를 시작하지 않고 null barrier를 정확히 한 번 둔다.
6. 다음 tick에는 controlled route-evaluation probe가 다시 호출된다.
7. 다음 tick의 결과를 무조건 BARREL로 고정하지 않는다. 새로 놓인 CHEST가 유효하면
   CHEST를 다시 선택할 수 있다.
8. 이전 owner/handoff state가 다음 generation으로 누수되지 않는다.
9. D change unit이 시작 직전 commit/tree 대비 새로 추가하는 `src/main/**` diff는 0개다.
10. D-owned build, dependency와 config diff는 0개다.

Registry-free fixture가 실제 `Blocks.CHEST/BARREL` bootstrap을 안정적으로 제공하지
못하면 unit test는 서로 다른 stable semantic candidate를 사용하고, 실제 CHEST/BARREL
차이와 actual world/route 재평가는 §11.1 R1a/R1b/R2a/R2b runtime에서 증명한다. 새 contract test의
skip은 pass로 간주하지 않는다.

D는 diagnostics Stage 1/2/3의 선행 또는 후행 조건이 아니다. 별도 test-source와 execution
승인 아래 독립 진행할 수 있으며, 그 결과는 diagnostics contract를 충족하거나 우회하지
않는다. 현재 기록된 test-only 보강과 build 결과는 D의 automated evidence로만 취급한다.
실패하면 같은 unit에서 production을 고치지 않고 `HANDOFF_DEFECT_FOUND`로 종료한다.

## 10. 향후 테스트 계약

### 10.1 Diagnostics core black-box 계약

별도 구현 승인 뒤 최소한 다음을 직접 검증한다.

```text
zero otherwise-emittable shared-capacity rejection -> canonical claim/admission count 0
first ordinary-ceiling rejection -> trigger ORDINARY_CEILING_RESERVE_ACTIVE exactly once
first true shared-hard-cap rejection -> trigger SHARED_HARD_CAP exactly once
ordinary-ceiling-first and hard-cap-first -> separate fresh diagnosticSession fixtures
ordinary/hard-cap races across simultaneous/sequential subsystems -> first trigger wins exactly once
fake formatter/sink failure after cap admission -> no claim/admission retry and no recursion
capEventClaimed == capEventAdmissionGranted == 0-or-1
capEventAdmissionGranted
  == capEventEmissionPending
   + capEventEmissionCompleted
   + capEventEmissionFailedAfterAdmission
dedupe/rate/local-family suppression only -> canonical cap event count 0
mixed budgeted emitters -> admitted total <= 5000
critical reserve is included inside 5000
mode/world/dimension/trace/operation transition
  -> sessionResetCount == 0
  -> diagnosticSessionId unchanged
  -> cumulative admitted/suppressed counts unchanged except for newly observed events
OFF-only operation
  -> fingerprint/rate/admission/suppression/investigation-ledger mutation 0
  -> budgeted investigation output 0
  -> always-on operational meaning unchanged
BOUNDARY <-> VERBOSE through deterministic test seam
  -> same diagnosticSessionId and accumulated budgets
  -> only future-event eligibility/detail changes
enabled -> OFF -> enabled active operation
  -> old binding/sample/tombstone invalidated exactly once before OFF publish
  -> pre-OFF binding revive/reuse 0 and stale state 0
  -> no OFF-time terminal synthesis
  -> explicit MODE_TRANSITION_COVERAGE_GAP when observation resumes
mode eligibility race -> one modeEpoch linearization; MODE_INELIGIBLE is not suppression/cap trigger
terminal finalizer vs enabled->OFF race
  -> OFF-first: diagnostic finalizer and T/A/G/S/P/C/F mutations 0
  -> terminal-first: A and P settle to 0 before OFF publish; no post-OFF formatter/sink call
deterministic test reset -> new session identity and zeroed bounded state
mode-eligible deterministic test reset
  -> final snapshot attempt settles COMPLETED or FAILED before old-session disposal
OFF deterministic test reset
  -> SKIPPED_MODE_OFF out-of-band result; snapshot admission/emission/accounting 0
  -> new session identity and zeroed bounded state without an old-session ledger mutation
every budgeted direct path -> exactly one shared admission
every operational companion -> shared admission count 0
always-on operational lifecycle/warning/error/crash contract -> unchanged
critical partition sum == 64
routine and abnormal group pools are multiples of four
one-to-three residual slots exist only in declared single-event pools
single-event residual plus terminal-group pool -> partial terminal-group admission 0
borrowing follows the declared acyclic direction and four-slot quantum
```

Terminal stress는 적어도 8/9 경계와 1,000개 이상의 logical terminal을 포함한다.
더 큰 synthetic count는 runtime 시간을 늘리지 않는 direct ledger fixture로 검증한다.

```text
totalTerminalObserved == unique token-proven authoritative Store-root finalizations
totalTerminalObserved
  == fullTerminalGroupAdmissionPending
   + fullTerminalGroupAdmissionGranted
   + fullTerminalGroupSuppressedBeforeAdmission
fullTerminalGroupAdmissionGranted
  == fullTerminalGroupEmissionPending
   + fullTerminalGroupEmissionCompleted
   + fullTerminalGroupEmissionFailedAfterAdmission
settled boundary -> both pending counts 0 and reviewer final equations hold
totalTerminalObserved
  == sum(known classification counts)
   + terminalClassificationUnavailableCount
totalTerminalObserved
  == sum(known scope counts)
   + terminalScopeUnavailableCount
totalTerminalObserved
  == terminalContextAvailableCount
   + terminalContextUnavailableCount
full group partial admission == 0
abnormal terminal visibility is not starved by early routine successes
bounded map/set/ring sizes remain within declared limits
unfinalized authoritative-token overflow -> COVERAGE_GAP and acceptance failure, not eviction
duplicate finalization attempt changes only the bounded duplicate counter
suppressed full group still changes aggregate accounting
missing ancillary context with stable terminal token -> total increments and UNAVAILABLE/gap fields close
contextless boundary without stable token -> no fabricated unique total; explicit coverage gap
snapshotSequence, ledgerRevision and coveredThroughTerminalSequence are monotonic and linked
snapshot accountingMode == CUMULATIVE and reconciliation matches its coverage flag
snapshot self-accounting includes its admission but excludes its later emission outcome
mode-eligible clean teardown snapshot precedes clear and uses its non-borrowable slot
OFF teardown/reset -> no snapshot attempt; test reset reports out-of-band SKIPPED_MODE_OFF
Long.MAX_VALUE - 1 counter and reconciliation-sum boundaries -> saturation without wraparound
sequence saturation -> sequenceAvailable=false and no MAX identity reuse
trusted Store-root scope -> exact registration/finalizer token proof or companion-ledger-only result
```

Source-string `contains(...)`만으로 shared-admission 계약을 증명하지 않는다. 가능한 경우 실제
emitter output, admission result와 ledger state를 함께 검증한다.

### 10.2 Diagnostics-only behavior invariant

OFF, BOUNDARY, VERBOSE와 cap/suppression 상태는 다음을 바꾸지 않는다.

```text
return values and Task selection/order/completion
retry, timeout, cooldown and fallback
input acquisition/release
Baritone goal/path ownership or cancellation
container click, cursor, slot transfer and GUI lifecycle
exception propagation and cleanup
thread scheduling and tick ordering
Carry On state or compatibility outcome
wire payload and command terminal result
overlay/HUD visibility
```

### 10.3 Tool-selection shaping 계약

```text
unchanged high-frequency decision: bounded per documented window
meaningful selection transition: immediate one event
position churn alone: no unbounded output
suppression summary: count plus first/last tick and stable fingerprint
dedupe/rate state: bounded
eligible critical records are protected from ordinary/tool-selection churn within the fixed reserve
critical-quota exhaustion -> proven terminal ledger accounts omission or explicit coverage gap remains; no hard-cap bypass
```

### 10.4 Handoff focused command 후보

다음은 별도 test 실행 승인이 난 뒤 사용할 후보이며 이번 문서 작업에서 실행하지
않는다.

```powershell
cd C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1
.\gradlew.bat :1.20.1:test `
  --tests 'lavi.minecraft.task.container.deposit.handoff.*' `
  --tests 'lavi.minecraft.task.container.deposit.auto.maintenance.child.AutoDepositGeneralTaskFactoryTest' `
  --no-daemon
```

이 targeted 명령의 성공은 clean build, deployment, compatibility 또는 runtime evidence가
아니다.

## 11. Release/merge risk-based representative matrix

모든 차원의 무제한 Cartesian product 대신 다음 risk-based minimum을 사용한다. Historical
checkpoint evidence와 future final-RC evidence를 합산하지 않는다.

```text
HISTORICAL_CHECKPOINT_EVIDENCE
  commit: a722ac2a17a813b62e605eaeac0fc9f96f7a1b5e
  artifact SHA-256:
    84C6634433D7402B2935ADD6E4028F43BD3782D2E4038E00D30206092E9CA839
  qualified:
    CHEST + Carry On 2.1.2.7 installed/unbound
    captured automatic-general place/handoff/open/transfer/terminal flow
  release use:
    preserved historical checkpoint evidence only

FINAL_RC_EVIDENCE
  commit/tree: FUTURE_VALUE
  ChatClef artifact SHA-256: FUTURE_FINAL_RC_SHA256
  requirement: every R/P row records this same final hash and loaded code source
  current status: NOT_RUN
```

### 11.1 새-container placement pair coverage

R1a/R2b 대각선만으로 container type과 Carry On branch의 독립성을 증명하지 않는다.
Existing-container 경로는 placement/handoff를 거치지 않으므로 빠진 pair의 대체 증거가
아니다.

각 placement row는 nearby supported container 0개와 inventory의 supported placement
candidate가 requested `CHEST` 또는 `BARREL` 하나뿐인 fixture를 사용한다. Installed row는
release target인 Carry On `2.1.2.7` unbound configuration 또는 Stage 1에서 별도 승인한 exact
version/binding/configuration으로 고정한다.

아래 `공통 필수 증거`는 한 runtime log source가 전부 소유한다는 뜻이 아니다. Placement child
identity, scheduler-owned stop exactly once, same-parent-tick null barrier, next-tick route evaluation과
generation isolation은 `JAVA_DETERMINISTIC` evidence가 소유한다. Actual placement/open/transfer, loaded
mod/linkage와 final artifact behavior는 `RUNTIME_LOG` 또는 `ARTIFACT_PREFLIGHT` evidence가 소유한다.
각 row PASS는 두 증거 계층을 같은 final source/artifact provenance에 연결했을 때만 성립한다.

| ID | 진입 경로 | 컨테이너 | Carry On | 공통 필수 증거 |
| --- | --- | --- | --- | --- |
| R1a | automatic general, 새 컨테이너 배치 | CHEST | 설치됨; exact version/config | requested type, placement child identity/correlation, actual child stop/clear exactly once, same-parent-tick null barrier, next-tick fresh route evaluation, open, confirmed transfer, logical terminal, stale state 0 |
| R1b | automatic general, 새 컨테이너 배치 | CHEST | 미설치 | R1a 공통 flow와 absent evidence |
| R2a | automatic general, 새 컨테이너 배치 | BARREL | 설치됨; exact version/config | R1a 공통 flow와 installed evidence |
| R2b | automatic general, 새 컨테이너 배치 | BARREL | 미설치 | R1a 공통 flow와 absent evidence |

Installed row는 loaded Carry On JAR/version/config, fabricated pickup success 0과 의도하지 않은
carry transition 0을 기록한다. Absent row는 loaded-mod evidence의 부재,
`carryOnLoaded=false`, `carryState=ABSENT` 또는 Stage 1에서 확정한 동등 explicit state와
`NoClassDefFoundError`/`NoSuchMethodError`/`NoSuchFieldError` 등 optional-linkage failure 0을
기록한다.

R1b와 R2a runtime을 생략하려면 Stage 1에서 container type과 optional Carry On branch가
독립이라는 source trace와 automated integration test를 먼저 승인 가능한 evidence로
제출한다. 그 proof가 없으면 네 runtime row가 모두 필수다.

### 11.2 나머지 runtime rows

| ID | 고정 fixture | 필수 증거 |
| --- | --- | --- |
| R3 | automatic general, existing CHEST, Carry On 미설치, BOUNDARY | `PLACE_CONTAINER_NEARBY`, `OBTAIN_CHEST`, post-place barrier 0; direct open, confirmed transfer, logical terminal |
| R4 | manual `@deposit_all`, 새 CHEST, Carry On 미설치 | manual ephemeral owner와 handoff-disabled; automatic handoff helper 진입 0; command/Store 정상 완료 |
| R5 | trusted automatic, existing registered BARREL, Carry On 설치 | exact trusted binding, general handoff 미주입, confirmed transfer, `AutoDepositTrustedStoreOutcome.ALL_STORED` 또는 Stage 1 동등 terminal |
| R6 | diagnostics deterministic stress와 bounded runtime sample | 8개 초과 terminal, routine 뒤 abnormal, separate fresh-session ordinary-ceiling-first/hard-cap-first fixtures, mode transition, snapshot reconciliation, gameplay unchanged |
| R7 | BOUNDARY, active automatic Store 중 user `@stop` | cancel 직전 correlation, user action tick, Stage 1 authoritative Store-root finalizer exactly once, cancelled/abnormal count와 quota visibility exactly once, global-stop cleanup 결과, 명시적 runner restart/rearm 뒤 fresh operation stale state 0 |
| R8a | trusted candidate A full, B capacity 있음 | A=`NO_WHOLE_STACK_CAPACITY`/`live_gui_has_no_whole_stack_capacity`, A inventory/cursor delta 0, next trusted B selected, B confirmed transfer, final `ALL_STORED`, general fallback 0 |
| R8b | 모든 trusted candidates full | 각 candidate의 capacity rejection, false `ALL_STORED`/cursor residue 0, final `CANDIDATES_EXHAUSTED`/`trusted_candidates_exhausted`, trusted-only 잔여 유지, general fallback 0, existing operation/candidate timeout bounds 유지 |
| R9a | automatic activation, nearby container와 inventory container item 없음 | `OBTAIN_CHEST`, `requestedItem=Items.CHEST`, `requestedCount=1` -> acquisition -> place -> handoff -> open -> confirmed transfer -> logical terminal |
| R9b | acquisition 불가능 fixture | Stage 1에서 정한 external observation/watchdog bound 안에서 existing bounded refusal/terminal 여부 관찰; test watchdog는 gameplay timeout이 아님; 계속 실행되면 `ACQUISITION_UNBOUNDED_DEFECT` release blocker, diagnostics unit에서 behavior fix 금지 |
| R10 | pressure threshold지만 protected/reserved loadout만 존재 | reason=`no_safe_surplus` 또는 `no_trusted_destination_or_safe_surplus`, state=`NO_SAFE_SURPLUS_WAIT`, Store-root task/terminal/ledger·placement·transfer 0, unchanged fingerprint rerun 0, existing low-water/fingerprint/policy-change rearm만 허용, diagnostic state leak 0 |

R6은 runtime과 deterministic black-box stress를 구분한다. Synthetic test에서는
OFF/BOUNDARY/VERBOSE transition을 직접 검증한다. Checkpoint source의 production runtime
toggle은 OFF/BOUNDARY를 제공하고 VERBOSE는 process start의
`lavi.chatclef.diagnostics` property 또는 `LAVI_CHATCLEF_DIAGNOSTICS` environment value로
선택한다. 따라서 full BOUNDARY/VERBOSE 왕복은 deterministic seam에서만 검증하고, runtime은
supported launch mode와 기존 toggle을 사용하거나 별도 process run으로 나눈다. 별도 launch는
새 `diagnosticSessionId`다. 현재 toggle은 overlay와 결합되어 있으므로 overlay state를
diagnostics mode 또는 overlay/diagnostics 독립성 증거로 사용하지 않는다. 그 toggle의
runtime 결과는 legacy coupled-command behavior 관찰로만 분류한다.

R6 minimum은 다음이다.

```text
at least 8 routine-success terminals, followed by at least 1 abnormal terminal
ordinary-ceiling-first fresh-session fixture
  -> ordinary admission pressure through the 4936 boundary
  -> first otherwise-emittable ordinary rejection commits the canonical-cap token
     with ORDINARY_CEILING_RESERVE_ACTIVE
hard-cap-first fresh-session fixture
  -> exhaust all eligible non-cap/non-final-snapshot capacity:
     admitted=4998, reserved(cap=1, finalSnapshot=1)
  -> reject one event that still passes mode, local/family and priority eligibility
     but has no eligible non-cap shared slot
  -> commit the permanently reserved canonical-cap token with SHARED_HARD_CAP:
     admitted=4999, reserved(cap=0, finalSnapshot=1)
  -> require a mode-eligible CLEAN_TEARDOWN or TEST_RESET snapshot admission:
     admitted=5000, reserved(cap=0, finalSnapshot=0)
  -> final snapshot emission outcome may be COMPLETED or FAILED_AFTER_ADMISSION
admittedBudgetedEvents <= 5000
canonical claim/admission total == 1 per fresh fixture/session
triggerReason == first eligible rejection
  (ORDINARY_CEILING_RESERVE_ACTIVE or SHARED_HARD_CAP)
any later rejection -> no second claim/admission
diagnosticSessionId unchanged across supported same-process transitions
sessionResetCount == 0 for same-process mode/world/operation transitions
separate process launch -> new diagnosticSessionId
full terminal-group partial admission == 0
cumulative emitted snapshot reconciliation and coverage status match
gameplay result and Task ordering unchanged
```

R7의 user `@stop`은 현재 TaskRunner disable, all-chain stop과 Baritone force-cancel을
수행하므로 이 row에서 unrelated Baritone path 보존을 주장하지 않는다. Operation-local 또는
unrelated-path preservation은 controlled interruption을 사용하는 P3에서 별도로 검증한다.
R9b가 behavior defect를 발견해도 diagnostics-only 또는 matrix 작업이 bounded refusal,
timeout, retry, cancellation이나 terminal policy를 새로 구현하도록 승인하지 않는다.

전체 storage release claim에는 다음 preservation gate도 포함한다.

| ID | Preservation gate |
| --- | --- |
| P1 | manual `@store_home`이 registered trusted container에서 실제 transfer와 typed terminal까지 완료한다. |
| P2 | chest, door/trapdoor, bed, button/lever, block placement, item use와 generic right-click이 유지된다. |
| P3 | controlled interruption, death, dimension change, disconnect/reconnect 뒤 stale owner/handoff/input/path state가 없고 operation이 소유하지 않은 state를 diagnostic cleanup이 변경하지 않는다. |
| P4 | Carry On 설치 상태의 exact binding/version/config를 기록하고 pickup `NOT_CARRYING -> CARRYING`, placement `CARRYING -> NOT_CARRYING` 전환을 확인한다. 미설치 상태에서는 generic ChatClef가 load 및 동작한다. |
| P5 | R3와 equivalent fixture를 diagnostics OFF로 실행한다. Runtime은 budgeted investigation output과 cap event 0, open/transfer/gameplay terminal 및 existing always-on operational 의미의 BOUNDARY-control 동등성을 증명한다. `fingerprint/rate/investigation-ledger mutation`과 shared admission/suppression delta 0은 Stage 2 `JAVA_DETERMINISTIC` evidence가 소유하며 로그 부재만으로 추론하지 않는다. Overlay state로 OFF를 추론하지 않는다. |

Checkpoint artifact는 CHEST + Carry On 2.1.2.7 설치/unbound configuration에서 R1a
flow를 역사적으로 검증했다. Diagnostics 변경 뒤 final JAR에 대한 R1a는 `NOT_RUN`이다.
R1b/R2a/R2b/R3-R10, P1-P5 또는 전체 container 조합도 아직 증명하지 않는다.

Diagnostics source가 바뀐 뒤에는 handoff production 파일을 건드리지 않았더라도 final
JAR로 R1a를 다시 검증한다. Diagnostics는 tick cost와 exception boundary에 영향을 줄 수
있으므로 checkpoint JAR의 runtime 증거를 새 release JAR에 그대로 승계하지 않는다.

Container claim은 다음처럼 제한한다.

```text
runtime minimum: CHEST and BARREL
current TRAPPED_CHEST and full shulker-color runtime qualification: not established
TRAPPED_CHEST separate runtime omission requires prior automated proof of the same placement predicate/open path
SHULKER_BOX full-support claim requires one representative-color runtime plus explicit scope statement
  and source+automated proof that all colors share the same placement/open path
otherwise: mark TRAPPED_CHEST/shulker as not runtime-qualified by this matrix
```

## 12. 단계와 완료 기준

### Stage 0 - checkpoint freeze

```text
status: complete as a handoff-functional checkpoint
checkpoint handoff-related hunk/meaning changes: prohibited in follow-up units
release-ready claim: prohibited
```

### Stage 1 - pre-change contract closure

Source 수정 전에 다음 open decision을 닫는다.

```text
exact producer inventory, runtime-purpose classification, exclusions and anti-bypass evidence
dual-purpose operational companion/investigation projection handling
composition owner, initialization, shutdown and thread model
required diagnosticSessionId format, common envelope insertion point and test-reset seam
OFF/BOUNDARY/VERBOSE mode-epoch linearization, pre-OFF invalidation and coverage-gap behavior
ordinary-ceiling/hard-cap trigger, claim/admission/pending equations, cap-slot token and non-recursion
numeric critical 64-slot partition, acyclic borrowing and fragmentation table
STORE_ROOT_OPERATION identity, authoritative finalizer and companion-ledger non-summing rule
bounded authoritative token lifetime, gameplay concurrency bound and overflow acceptance failure
Store terminal counter domains, UNAVAILABLE buckets and abnormal sample-ring K
admission/emission pending-to-settled transitions and reconciliation equations
trusted Store-root binding proof or companion-ledger-only classification
cumulative aggregate snapshot cadence, sequence/revision/covered-through and self-accounting provenance
all snapshot-kind mode eligibility, mode-eligible clean-teardown guarantee,
  OFF `SKIPPED_MODE_OFF` semantics and abrupt-crash limitation
saturating counter/sequence/reconciliation-sum behavior
direct output migration order and double-admission prevention
R1a/R1b/R2a/R2b Carry On/container pair proof or four-row runtime requirement
R3-R10/P1-P5 exact fixture and evidence ledger
exact proposed production/test file, method and minimal hunk set
frozen handoff hunk zero-change audit and rollback unit
focused test list, fake-sink seam and no-skip rule
```

각 항목을 테스트 가능한 문장과 exact proposed file set으로 보고한다. Implicit/static
process-wide mutable owner가 필요하면 `AGENTS.md` shared-state exception report를 먼저
제출하고 중단하여 명시적 승인을 받는다. Stage 1 closure report와 별도의 사용자 승인이
없으면 `STAGE_1_STATUS`는 계속 `OPEN`이다. Stage 1 완료는 source/test-source edit 또는
test/build/runtime 승인이 아니다.

향후 Stage 1 closure는 dated HTML marker와 별도 appended status section으로 기록한다.
현재 OPEN/pre-change 상태 블록을 소급 수정하거나 closure 증거로 재해석하지 않는다.

#### External patch / ZIP intake gate

외부 ChatGPT/reviewer patch, ZIP, backup, 이전 branch 또는 AI-generated source는
[AGENTS.md previous-work reference-only gate](../../../AGENTS.md#previous-failed-ai-work-is-reference-only)에
따른 reference-only artifact다.
Canonical source, approved file set 또는 implementation authorization이 아니다.

금지:

```text
ZIP을 repository 위에 overlay/extract
전체 파일 복사 또는 교체
git apply, merge, cherry-pick 또는 old patch 전체 재적용
외부 package/class 구조를 authoritative로 채택
```

필수:

```text
archive manifest와 provenance를 먼저 읽기 전용으로 확인
a722ac2와 current tree를 method/diff-hunk 단위로 비교
각 hunk를 `evidence-backed and reusable as-is` /
  `idea-only and requiring a fresh minimal implementation` /
  `a hypothesis requiring diagnostic logs` /
  `unsafe and to be discarded`로 분류
Stage 1 closure 및 별도 source-edit 승인 뒤에만 exact file/method/hunk set 안의
  승인된 최소 hunk를 current tree에 새로 적용
Section 3 frozen handoff hunk 변화 0 확인
```

여기서 manual merge는 Git merge가 아니라 검토·승인된 최소 hunk의 수동 재적용을 뜻한다.

### Stage 2 - Diagnostics Core Boundedness

Stage 2-6의 승인 문장은 diagnostics core, tool-selection, frozen handoff, harness와 final
runtime matrix의 역사 범위에만 적용한다. 현재 exact GUI stabilization의 source/test/required
clean build는 이 Stage ladder에 편입하지 않고 §1.1 continuous override를 따른다.

Source/test-source edit와 focused test execution은 각각 별도 승인이다. Source edit 승인
뒤 B unit를 구현할 수 있지만 test 실행, build, runtime, commit 또는 push까지 승인된
것으로 해석하지 않는다. Gameplay behavior change 또는 upstream engine refactor가
필요해지면 중단하고 범위를 다시 승인받는다.

### Stage 3 - Tool-selection Diagnostic Shaping

C unit의 source/test-source edit와 test 실행도 각각 별도 승인한다. B의 shared admission을
재설계하거나 gameplay tool choice를 바꾸지 않는다.

### Stage 4 - Handoff test-only

D는 diagnostics Stage 1/2/3과 독립된 test-only gate다. 현재 제공된 상태에서는 container
generation/candidate 전환과 operation isolation test source 보강, clean forced build와
`380 passed / 1 existing skipped`가 완료된 것으로 기록한다. Production handoff hunk와
의미는 동결 상태를 유지하며 final JAR Minecraft runtime은 아직 수행하지 않았다.

추가 D test-source edit와 test 실행은 다시 별도 승인한다. D-owned change unit이 시작 직전
parent/tree 대비 새로 추가하는 `src/main/**`, build, dependency 또는 config diff는 0이어야
한다. 병렬로 별도 승인된 B/C production diff를 D defect로 오인하지 않는다. D test가
handoff lifecycle contract를 실패할 때만 `HANDOFF_DEFECT_FOUND`로 별도 보고한다.

### Stage 5 - automated verification

별도 실행 승인 뒤 focused tests, mixed-source stress tests, 전체 unit tests와 clean forced
build를 수행한다. Required clean command는 build runbook을 따른다.

```powershell
cd C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1
.\gradlew.bat clean build --rerun-tasks
```

Build success만으로 runtime Mixin injection 또는 final artifact 동작을 주장하지 않는다.
현재 기록된 post-checkpoint clean forced build는 Stage 4 test-only tree 검증이다. Stage 2/3
source가 바뀐 뒤에는 같은 command와 final source/deployed artifact identity로 Stage 5를 다시
수행해야 하며, 현재 결과를 future final RC로 승계하지 않는다.

### Stage 6 - final artifact runtime matrix

별도 launch/deployment 승인 뒤 fresh final JAR, deployed JAR SHA-256, loaded code source,
Minecraft logs와 R1a-R10/P1-P5 결과를 기록한다. Stage 6이 끝나기 전에는
`RELEASE_READY` 또는 merge/release 승인을 주장하지 않는다.

#### Stage 6.1 - 선택적 Python runtime automation harness 설계·실행 경계

Stage 6의 반복 실행과 증거 수집은 별도 승인된 여러 Python 파일로 보조할 수 있다.
이 harness는 Stage 4의 Java handoff test-only 허용 경로에 포함되지 않으며, 이 문서만으로
파일 생성·수정·실행이 승인되지 않는다. Production Java, Java test source, Gradle 설정,
dependency, JAR 내용과 frozen handoff hunk는 변경하지 않는다.

승인될 경우의 격리 경계와 책임 분리는 다음을 기본안으로 한다. 추적 가능한 harness
source와 로컬 실행 evidence를 분리한다. `test/test_Isolation/**`는 `.gitignore` 대상이고
기본 pytest에서도 제외되므로 source를 그 아래에 두거나 그 결과를 Stage 5 test evidence로
주장하지 않는다. `.gitignore` 또는 `pyproject.toml` 변경은 이 기본안에 필요하지 않으며,
필요해질 경우 별도 config-edit 승인을 받는다.

새 feature package를 만들기 전에 기존 established runtime infrastructure를 exact symbol 수준으로
inventory하고 재사용한다. 다음 package가 generic ownership의 우선 후보며, 같은 책임의
feature-local parallel implementation을 기본안으로 만들지 않는다.

```text
tests/minecraft_chatclef/runtime/preflight/**
tests/minecraft_chatclef/runtime/observation/**
tests/minecraft_chatclef/runtime/submission/**
tests/minecraft_chatclef/runtime/batch/**
```

Automatic-deposit package는 feature-specific scenario declaration, runtime oracle/assertion, matrix
composition과 shared result schema로 표현할 수 없는 최소 manifest field만 소유한다. Existing
preflight, log identity/snapshot, terminal observation, one-shot submission, reconciliation 또는 batch
result component가 같은 계약을 충족하면 그대로 사용한다. 새 feature-local `preflight/`,
`observation/`, `submission/` 또는 generic `result_writer`를 만들려면 기존 component의 정확한
불충족 계약과 별도 source-edit 범위를 먼저 제시해야 한다.

다음 구조는 최대 후보 responsibility map이며 생성해야 할 최소 파일 목록이 아니다. 첫 승인된
row에 필요한 최소 파일부터 시작하고, 독립 state/type/responsibility가 실제로 생길 때만
분리한다.

```text
tracked feature-specific harness source candidate:
  tests/minecraft_chatclef/runtime/automatic_deposit/
    __init__.py
    scenario/**
      -> R1a-R10/P1-P5의 data-only fixture/expectation declaration
    oracle/**
      -> automatic-deposit-specific event correlation and side-effect-free verdict rules
    evidence/**
      -> shared runtime result로 표현할 수 없는 feature-specific manifest/result extension only
    orchestration/**
      -> approved row sequencing and fail-closed composition only
    test_*.py
      -> fakes/fixtures만 쓰는 hermetic deterministic harness tests
    run_approved_automatic_deposit_matrix.py
      -> explicit live entrypoint; pytest test module 아님

ignored, never-overwritten local evidence:
  test/test_Isolation/automatic_deposit_runtime/<new-run-id>/
```

Scenario module은 fixture 선언과 기대 결과를 소유하지만 world, inventory, snapshot, trusted
destination 또는 profile을 직접 변경하지 않는다. Oracle은 input evidence를 mutate하지 않는
pure verdict를 기본으로 한다. Evidence extension은 기존 result schema와 충돌하거나 같은 값을
중복 소유하지 않는다. Orchestration은 transport, preflight, observation 또는 evidence parser를
재구현하지 않고 existing component를 조합한다.

Tracked `test_*.py`는 default pytest에서 실행돼도 Minecraft, LAVI, Gradio, loopback endpoint 또는
외부 process에 연결하지 않고, command 제출·world/profile mutation·ignored evidence write를
수행하지 않는 hermetic test여야 한다. Live execution은 `run_approved_automatic_deposit_matrix.py`
한 entrypoint로만 시작한다. Existing live/mutating opt-in, exact approval record와 row-specific
authority 중 하나라도 없으면 command나 runtime mutation 없이 `verdict=INCONCLUSIVE`,
`reason=NOT_AUTHORIZED_OR_OPT_IN_MISSING`으로 종료한다. `SKIPPED`는 pytest collection/control 결과일
수는 있어도 Stage 6 matrix row verdict로 사용하지 않는다. Baseline의 existing live gates는
`LAVI_MINECRAFT_RUNTIME_TESTS=1`과
`LAVI_MINECRAFT_RUNTIME_MUTATING=1`이다. H0 inventory에서 이 gate를 우회하거나 같은 의미의
feature-local environment switch를 새로 만들지 않는다. 향후 exact gate 이름이 별도 승인된 공통
runtime change로 바뀌면 common preflight owner를 따라가고 문서 provenance를 갱신한다.

새 transport를 만들지 않는다. 다음 existing supervised boundary를 재사용한다.

```text
tests.minecraft_chatclef.runtime.submission.gradio_runtime_gateway.LaviGradioRuntimeGateway
  -> read status: /on_refresh_click_2
  -> approved LAVI command submit: /on_submit_korean_command_click
tests.minecraft_chatclef.runtime.supervised_live_run.run_supervised_live_command
tests.minecraft_chatclef.runtime.preflight.approved_live_run_ticket.ApprovedLiveRunTicket
```

각 row는 command 이전에 다음 transport mode 중 정확히 하나를 고정한다. Mode가 없거나 둘 이상
해석되면 command를 보내지 않고 `INCONCLUSIVE`로 종료한다.

```text
SUPERVISED_LAVI_SUBMIT
  -> existing LAVI route가 exact serialized command를 그대로 전달한다는 proof 필요
  -> run_supervised_live_command와 existing one-shot/reconciliation boundary 사용
  -> approved invocation을 exactly once만 submit

OPERATOR_MANUAL_OBSERVE_ONLY
  -> Python은 submit_korean_command와 run_supervised_live_command를 호출하지 않음
  -> operator가 Minecraft 안에서 직접 command 실행
  -> harness는 read-only preflight, exclusive run directory, pre-run log cursor와 이후 correlation만 관찰
  -> timing만으로 exactly-once를 추정하지 않으며 duplicate/uncertain operator action은 INCONCLUSIVE

NO_COMMAND_AUTOMATIC_TRIGGER
  -> Python submit call 0
  -> operator-confirmed fixture/trigger와 post-cursor automatic operation correlation만 관찰
```

Runtime row에서 supervised submit 후보는 R4=`@deposit_all`, R7=`@stop`, P1=`@store_home`으로
제한한다. 이 direct command가 existing Gradio/LAVI route로 exact하게 전달된다는 source/runtime
proof가 없으면 `OPERATOR_MANUAL_OBSERVE_ONLY`를 사용한다. Natural-language route를 사용할 때도
row에서 승인된 exact serialized input, expected canonical command와 fingerprint를 ticket에 고정하며
actual routed command가 증명되지 않으면 PASS로 판정하지 않는다.

R5/R8의 `@auto_deposit_trust`와 `@auto_deposit_untrust <exact-destination-id>`는 mutating fixture
setup이며 row execution allowlist가 아니다. Test operator 또는 별도 승인된 setup ticket이 row
cursor 전에 수행하고 manifest가 resulting trusted-destination state를 검증한다. Placeholder
`[destinationId]`는 실행 가능한 승인 command가 아니다. `@auto_deposit_trusted_list` 같은 read-only
verification도 exact route proof와 별도 승인 없이는 Python이 제출하지 않는다.

H5 `@auto_deposit_trust area 16x16`, `@auto_deposit_trust 반경 16x16`,
`@자동보관등록 영역 16x16`, `@자동보관등록 반경 16x16`도 mutating fixture setup으로 분류한다.
2026-09-02 문서 snapshot에서는 direction만 있고 source가 구현되지 않았다. 그 snapshot의
zero-argument trust handler는 trailing argument를 fail closed하지 않아 English form이 기존 single
registration으로 떨어질 수 있었고, Korean form은 당시 command registry에 없었다. 이는 dated
historical baseline이며 현재 source 상태가 아니다.

2026-09-04 19:03 KST에 검증된 pre-player-position crosshair baseline source에는 exact
English/Korean grammar, crosshair-only anchor, loaded-only fixed volume scan, exact batch allowlist,
reciprocal double-chest normalization, strict one-transaction repository mutation, bounded result와
next-tick automatic-pressure revision observation이 연결되어 있었다. 당시 H5 focused tests 66개와
전체 1.20.1 tests 684개가 통과했다. 당시 clean forced build도 171/171 actionable tasks 실행으로
통과했고, 생성된 1.20.1 JAR SHA-256은
`3DF3582702C28A35BD83E5AACA82B945A060CE00988E89C800538444FF49612F`다. 첫 시도의 1.18.2
실패는 `TitleScreen` 생성자가 headless 환경에서 null `MinecraftClient`를 참조한 test-fixture
결함이었고, production selector 실행 전의 실패였다. 기존 `TestObjects.allocate` fixture로 바꾼 뒤
전체 multi-version build가 통과했다.

같은 SHA-256의 JAR은 `LAVI_TEST_Fabric01`에서 2026-09-04 18:51:28 KST launch됐고, 19:03:22 KST
crosshair baseline batch가 4096 positions complete scan, 신규 6개와 기존 3개, revision `1 -> 2`,
registry `7 -> 13`으로 `UPDATED`됐다. 별도 19:03:36 invocation은 unavailable crosshair anchor에서
scan/repository mutation 0의 `INVALID_ANCHOR`로 fail closed했고 list는 13개를 유지했다. 이후 신규
destination 하나를 manual `store_home`이 사용해 171 items를 보관했다. 이는 player-position 변경 전
crosshair baseline의 dated deployment/runtime evidence다. 현재 player-position source, build 또는
runtime 상태로 인용하지 않는다.

현재 player-position implementation은 1.20.1의 707 tests와 전체 11개 configured-version test task,
그리고 171/171 clean forced build를 통과했다. 생성된 1.20.1 JAR은 8,112,941 bytes이고 SHA-256은
`24DD4C8DEBA968D0206CA3D68A227C57D513E55769F1BB51F5943F549989CC27`이다.
`LAVI_TEST_Fabric01` active JAR과 이 hash가 일치한다.

2026-09-04 21:32:57 KST runtime에서 `@auto_deposit_trust area 16x16`은
`anchorSource=PLAYER_BLOCK_POSITION`, `anchorPos=-743,69,65`, `coverageComplete=true`,
`scannedPositionCount=4096`, `physicalSupportedBlockCount=44`, `logicalDestinationCount=22`,
`newlyRegisteredCount=9`, `alreadyRegisteredCount=13`, `repositoryRevisionBefore=1`,
`repositoryRevisionAfter=2`, `totalRegistryCountBefore=13`, `totalRegistryCountAfter=22`의
`UPDATED`로 종료됐다. 이는 player-position batch registration의 live evidence다.
`downstreamAutomaticReevaluationPossible=true`는 실제 automatic deposit 실행 증거가 아니며, 반복
`NO_CHANGE`, 이동 후 anchor 변경, 명시적 경계 테스트와 신규 등록 destination 선택은 아직 실행하지
않았다. Operator/harness setup allowlist 승격도 계속 별도 runtime evidence gate로 남는다.

구현된 뒤에도 broad batch는 R5/R8의 exact fixture 준비를 자동으로 대체하지 않는다. Setup manifest는
command 전후의 전체 exact destination set, double-chest logical dedupe, excluded-container registration 0건,
registration truncation 0건과 예상하지 않은 추가·삭제 0건을 검증한다. H5 registration은 임의 64개로
자르지 않지만, downstream automatic operation의 기존 64-candidate snapshot은 별도 정책으로 남는다.

Command handler 자체는 loaded block-state scan, 한 repository transaction과 bounded output만 소유하고
Task/path/click을 만들지 않는다. 다만 effective repository revision 변경은 기존 automatic-pressure
chain의 다음 tick 재평가를 유발할 수 있으므로, 그 뒤 automatic run이 시작되면 registration command의
직접 side effect가 아니라 별도 downstream lifecycle와 evidence owner로 기록한다. `NO_CHANGE`는 H5-owned
revision change를 만들지 않는다.

H5의 Korean `@자동보관등록` command name은 `영역 16x16` 또는 `반경 16x16`을 요구하는
batch-only direct alias이며 LAVI-owned Java command scope다. 무인자 Korean invocation은 승인하지 않는다.
Python chat/microphone natural-language route와 Fabric wire protocol은 별도 범위다. Canonical H5
grammar, fixed half-open volume, exact block allowlist, double-chest identity, all-or-none repository
mutation과 deferred bulk undo 계약은
[ChatClef Manual Trusted Home Storage Direction §13](chatclef-manual-trusted-home-storage-direction-2026-08-27.md#13-trusted-등록-ux와-json)이
소유한다.

현재 player-position batch anchor, scanner precondition, result field와 empty-volume result의 scoped
override는 [H5 Player-Position Anchor Implementation Contract and Verification Record](chatclef-h5-player-position-anchor-pre-change-contract-2026-09-04.md)가
소유한다. Java source는 `PLAYER_BLOCK_POSITION`을 사용하며 test/build/deployment와 한 번의 live
registration이 확인됐다. 남은 runtime matrix는 해당 문서의 open acceptance로 유지한다.

Harness가 허용되는 역할은 다음으로 제한한다.

```text
LAVI, Minecraft, active Fabric instance와 final JAR identity preflight
source/deployed JAR SHA-256 및 duplicate ChatClef JAR evidence 수집
existing supervised command transport가 proof된 row의 allowlisted command를 exactly once 실행 보조
operator-manual/no-command row의 read-only preflight와 post-cursor observation
latest.log와 새 crash report의 run-scoped 수집
R1a-R10/P1-P5 fixture별 runtime-owned event, correlation, transfer와 terminal 판정
Java deterministic evidence와 runtime evidence의 provenance link
Mixin/linkage error 검색
PASS / FAIL / INCONCLUSIVE 및 evidence path를 포함한 새 결과 보고서 생성
관찰 전용 watchdog에 의한 unbounded-run 식별
```

Harness는 다음 경계를 넘지 않는다.

```text
Java deterministic test를 Python 결과로 대체
로그에 없는 same-parent-tick 또는 lifecycle 사실을 화면 동작만으로 PASS 처리
shared cap 5000, ordinary ceiling 4936, critical reserve 64 또는 terminal-group 원자성을
  Minecraft 한 번의 실행만으로 증명
default pytest 수집 중 live endpoint 연결, command 제출 또는 runtime evidence 생성
관찰 watchdog을 gameplay timeout, retry, cancel 또는 terminal policy로 사용
GUI key/mouse injection을 기본 command transport로 사용
process 시작·종료, transport 재설정 또는 raw WebSocket/socket/HTTP fallback
operator-manual/no-command mode에서 run_supervised_live_command 또는 submit API 호출
row allowlist 밖의 명령 전송 또는 승인된 command의 자동 재실행
trusted destination 등록/해제를 row execution 안에서 무승인 fixture mutation으로 수행
test fixture를 만들기 위해 production hook, cheat bridge 또는 wire schema 추가
JAR, mods profile, Carry On 설치 상태, world 또는 configuration을 승인 없이 변경
world/save/snapshot 생성·복사·복원·이름 변경·삭제를 harness가 수행
기존 evidence directory나 결과 파일 덮어쓰기
기존 generic preflight/observation/submission/batch 책임을 feature package에 평행 재구현
연결 실패, 필드 누락, harness exception 또는 불충분한 로그를 product FAIL/HANDOFF_DEFECT_FOUND로 분류
```

Handoff의 generation/candidate 전환과 single-tick barrier는 Stage 4 Java test가 deterministic
proof를 소유한다. Python harness는 final JAR에서 CHEST/BARREL의 실제 placement/open/transfer
흐름을 반복하고 증거를 모으는 orchestration layer일 뿐이다. Diagnostics cap과 terminal
accounting correctness도 Stage 2의 deterministic Java black-box test가 소유하며, Python은
R6의 bounded runtime sample과 log reconciliation만 보조한다.

각 matrix assertion은
`evidenceOwner=JAVA_DETERMINISTIC|RUNTIME_LOG|ARTIFACT_PREFLIGHT|OPERATOR_FIXTURE|HARNESS_CONTROL|GAMEPLAY_OBSERVATION`
중 하나를 선언한다. Python harness는 독립 evidence owner가 아니라 이 owner들의 typed evidence를
수집·검증·결합하는 orchestration 역할이다. Canonical JUnit XML과 operator fixture reference는
exact artifact/reference를 링크할 뿐 Python이 Java 내부 불변식이나 초기 fixture 사실을 새로
증명하지 않는다. Overall row PASS는 모든 required owner의 evidence가 모였을 때만 가능하며,
runtime log 부재를 Java proof로 꾸미거나 Java-owned 내부 불변식을 화면 관찰로 대체하지 않는다.

LAVI process가 실행 중이라는 사실만으로 runtime precondition이 충족되지는 않는다. Active
Fabric 1.20.1 Minecraft process, test world 진입, expected final JAR hash, command transport 연결과
fixture 준비를 별도로 증명해야 한다. Carry On 설치/미설치 row는 실행 중 hot swap하지 않고
각각 별도 profile 또는 별도 process launch로 실행한다. World와 inventory를 변경하는 scenario는
복제된 test world와 row별 초기 snapshot을 사용하며, 해당 runtime mutation도 승인 범위에
포함돼야 한다. Test operator 또는 별도로 승인된 외부 준비 단계가 fixture 생성, trusted-destination 등록/해제,
인벤토리 구성과 snapshot 복원을 소유하고, harness는 준비 완료 manifest를 검증할 뿐이다.
Profile 전환, Carry On 설치/제거/config 변경, JAR 교체와 world 복원은 harness 책임이 아니다.
World 이름만 같다는 이유로 snapshot 복원을 추정하지 않고 operator-confirmed snapshot identity와
row-specific fixture fingerprint가 없으면 `INCONCLUSIVE`다.

각 run manifest는 broad search나 과거 실행 추론 없이 다음 absolute identity를 고정한다.

```text
evidence schema version와 exclusive run ID
repository root, Git commit/tree identity와 dirty-state evidence
harness source commit/tree 또는 content fingerprint
source final JAR path/hash
active CurseForge instance root
active instance의 exact mods directory와 deployed ChatClef JAR path/hash
loaded ChatClef code-source identity와 duplicate ChatClef JAR enumeration result
exact latest.log path, pre-run file identity/cursor와 post-run replacement/truncation/rotation result
exact crash-reports directory와 run 시작 뒤 새 파일 목록
LAVI process identity와 exact Gradio endpoint
Minecraft process identity, Fabric backend, instance와 world identity
Carry On loaded JAR/version/config 또는 explicit absent evidence
diagnostics mode, final artifact matrix row ID와 assertion evidence-owner map
transportMode, approval identity, exact command/input fingerprints와 one-shot invocation ID
  또는 OPERATOR_MANUAL_OBSERVE_ONLY/NO_COMMAND_AUTOMATIC_TRIGGER marker
one-shot guard/reconciliation status
operator-confirmed fixture/snapshot identity and fixture fingerprint
run start/end UTC와 observation completion boundary
```

Run directory는 어떤 mutating submit보다 먼저 exclusive-create하며 이미 존재하면 새 이름으로
자동 fallback하지 않고 실행을 중단한다. Pre-submit manifest/evidence write가 실패하면 command를
보내지 않는다. Submit 뒤 evidence write, parser 또는 sink가 실패하면 `verdict=INCONCLUSIVE`,
`reason=HARNESS_ERROR`, `reconciliationRequired=true`로 남기고 자동 재실행하지 않는다.

`latest.log`가 cursor 뒤 truncate, replace 또는 rotate되거나 마지막 record가 incomplete이면
해당 evidence를 과거 파일이나 timestamp 추정으로 메우지 않고 `INCONCLUSIVE`로 판정한다. Raw
log, crash report와 absolute local path를 포함한 evidence는 ignored local root에만 둔다. Commit,
외부 공유 또는 review attachment가 필요하면 별도 승인된 sanitized export를 만들고 secret, user
path, private endpoint와 불필요한 process detail을 제거한다.

각 판정은 다음 의미를 사용한다.

```text
PASS: required owner별 fixture/artifact/Java/runtime evidence가 모두 증명되고 필수 판정이 일치
FAIL: required fixture와 artifact identity가 증명된 실행에서 product contract 위반이 직접 관찰됨
INCONCLUSIVE: 연결, fixture, identity, log field, operator action 또는 observation evidence가 불충분하거나 harness 자체가 실패함
  -> reason=HARNESS_ERROR|PARSER_AMBIGUITY|EVIDENCE_WRITE_FAILURE|LOG_REPLACED 등 fixed reason 사용
HANDOFF_DEFECT_FOUND: FAIL 중 frozen handoff lifecycle contract 위반이 직접 증명된 경우만 사용
```

Harness 구현 또는 결과는 Stage 1 closure, Stage 2/3 deterministic diagnostics tests,
Stage 4 Java handoff tests, Stage 5 clean forced build 또는 final-JAR identity를 충족하거나
우회하는 증거가 아니다. Harness 자체는 선택 사항이며, 승인하지 않아도 Stage 1-5를 막지
않고 Stage 6 matrix는 수동으로 수행할 수 있다. Applicable gate와 final hash가 확정되기 전에는
live release matrix row를 실행하지 않는다.

Harness source 승인은 두 단계로 분리한다.

```text
H0 - hermetic harness core
  earliest gate: Stage 1 closure and separate Python test-source approval
  scope: existing runtime-component reuse inventory, scenario/result schema, fail-closed transport-mode
         selection, fake gateway/log fixtures, pure oracle, exclusive evidence-directory behavior
  prohibited: final event-name assumptions, live endpoint access, command submit, runtime row execution

H1 - feature-specific runtime binding
  earliest gate: Stage 2/3 emitted schema and Stage 4 deterministic contracts stabilized
  scope: final event/correlation parser, row-specific assertions, matrix composition and manifest binding
  final JAR hash required for source edit: no
  final JAR hash required for live row execution: yes
```

권장 순서는 Stage 1 closure -> 선택적 H0 source-edit와 hermetic tests -> Stage 2/3 별도
구현·deterministic test -> Stage 4 evidence link -> 선택적 H1 source-edit와 hermetic tests ->
Stage 5 clean forced build -> final JAR/hash 확정 및 별도 deployment -> 별도 script/runtime 승인 ->
read-only preflight -> row별 operator fixture 확인 -> scenario 실행 -> evidence/report 검토다.
Harness 승인은 clean build, JAR 배포, Minecraft launch, Carry On profile 변경, fixture setup, commit
또는 push 승인을 포함하지 않는다.

## 13. 명시적 non-goals

이 문서와 첫 diagnostics correctness slice에는 다음을 포함하지 않는다.

```text
handoff production fix or redesign
TaskRunner/UserTaskChain/SingleTaskChain behavior change
PlayerInteractionFixChain change
Baritone goal/path/input behavior change
container transfer, cursor or click semantics change
Carry On behavior or dependency/version change
diagnostics startup default or overlay/HUD behavior change
production diagnostics reset command/configuration
wire protocol or command result schema change
global System.out/Debug interception
upstream-derived source refactor or package move
Forge/MineMind implementation, placeholder or shared runtime owner
dependency, Gradle, Minecraft, Fabric or Java version change
blind patch application, archive overlay or wholesale file replacement
existing runtime preflight/observation/submission/batch의 feature-local parallel reimplementation
이번 Markdown 작업에서 Python runtime harness 구현 또는 실행
```

## 14. 현재 판정

<!-- 20260831_kpopmodder: Replaced the three approval-wait checkpoints with evidence-driven test, log, fix, and runtime verdict criteria. -->

```text
HANDOFF_CHECKPOINT: PRESERVE
HANDOFF_PRODUCTION: FROZEN
HANDOFF_CONTAINER_TYPE_TEST: TEST_ONLY_SOURCE_ENHANCED; AUTOMATED PASS RECORDED; FINAL RUNTIME PENDING
POST_CHECKPOINT_JAVA_TEST: 385 PASSED; 1 EXISTING SKIPPED
POST_CHECKPOINT_CLEAN_FORCED_BUILD: PASS RECORDED; NOT FINAL DIAGNOSTICS RC
HANDOFF_COMPLETION_COMMIT: READY_FOR_SCOPED_TEST_ONLY_COMMIT; CONTAINER-CANDIDATE TYPE/GENERATION BOUNDARY TEST EVIDENCE RECORDED

DIAGNOSTICS_DIRECTION: SHARED_BUDGETED_INVESTIGATION_ADMISSION
DIAGNOSTICS_SESSION_CONTRACT: PARTIALLY_LOCKED; STAGE_1 DECISIONS REMAIN
DIAGNOSTIC_SESSION_ID: REQUIRED_PROCESS_LIFETIME_ID; BRIDGE_SESSION_ID_ALIAS_PROHIBITED
CANONICAL_CAP_EVENT: EXACTLY_ONCE_CLAIM_AND_ADMISSION; PHYSICAL_COMPLETION_IS_0_OR_1
TERMINAL_PRESERVATION: TOKEN_PROVEN_BOUNDED_ACCOUNTING, NOT UNLIMITED RAW OUTPUT
TOOL_SELECTION_SHAPING: SEPARATE_REQUIRED_FOLLOW_UP
DIAGNOSTICS_COMPLETION_COMMIT: PENDING_SESSION_CAP_AND_TERMINAL_SUMMARY TEST/LOG/FIX/RETEST EVIDENCE; NOT WAITING_FOR PRE-IMPLEMENTATION APPROVAL

FINAL_DIAGNOSTICS_AUTOMATED_VERIFICATION: NOT_RUN
PYTHON_RUNTIME_HARNESS: DOCUMENTED_OPTION; NOT_IMPLEMENTED; NOT_EXECUTED
PYTHON_HARNESS_SOURCE_GATE: H0 AFTER STAGE_1; H1 AFTER STAGE_2/3 SCHEMA; LIVE RUN AFTER FINAL JAR
FINAL_ARTIFACT_RUNTIME_MATRIX: NOT_RUN
MERGE_RELEASE_VERDICT: PENDING_MANUAL/TRUSTED/CARRY_ON_ABSENT RUNTIME MATRIX EVIDENCE
```

위 세 checkpoint는 사전 승인 대기열이 아니다. 각 범위는 먼저 deterministic test를 보강하고,
필요한 bounded log를 보강한 뒤, Java test 결과와 Minecraft runtime 물증으로 현재 동작을
판정한다. 물증이 결함을 가리킬 때만 verified first-failing boundary의 최소 owner에서 코드를
수정하고 동일 test와 동일 runtime scenario를 다시 실행한다.

Handoff 완료 커밋은 container candidate 종류·generation 경계 test evidence로 판정한다.
Diagnostics 완료 커밋은 session cap·terminal summary의 test/log/fix/retest evidence로 판정한다.
전체 merge/release는 manual, trusted, Carry On 미설치 등을 포함한 final artifact runtime matrix의
PASS/FAIL/INCONCLUSIVE 결과로 판정한다. Stage 1의 미확정 diagnostics 계약은 이 세 범위의
test-source 보강, bounded observation 또는 runtime evidence 수집을 전역 차단하지 않는다.

## 15. 2026-08-31 current implementation and build evidence delta

<!-- 20260831_kpopmodder: Appended current implementation evidence without rewriting the historical OPEN and approval-gate records above. -->

이 절은 위 Section 1, Section 12, Section 14에 기록된 당시의 계약 상태와
`380 passed`/`385 passed` 중간 결과를 소급 변경하지 않는다. 아래 값이 현재 source,
deterministic test, clean build 및 artifact identity에 대한 최신 상태다. 이 갱신은 Minecraft
runtime PASS, release-ready 또는 merge-ready 선언이 아니다.

```text
CHECKPOINT_COMMIT: a722ac2a17a813b62e605eaeac0fc9f96f7a1b5e
HANDOFF_PRODUCTION_DIFF: 0
HANDOFF_PRODUCTION: FROZEN

STRICT_OFF_CLEAN_TEARDOWN_P0: CLOSED_BY_ELIGIBILITY_LEASE
STAGE_2_SESSION_CAP_TERMINAL: IMPLEMENTED_AND_JAVA_VERIFIED
STAGE_3_TOOL_SELECTION_SHAPING: IMPLEMENTED_AND_JAVA_VERIFIED
HISTORICAL_DIAGNOSTICS_PLAN_ALL_SLICES_COMPLETE: NOT_CLAIMED

JAVA_1_20_1: 476 TOTAL; 475 PASSED; 0 FAILED; 0 ERRORS; 1 EXISTING SKIPPED
JAVA_1_21_1: 476 TOTAL; 475 PASSED; 0 FAILED; 0 ERRORS; 1 EXISTING SKIPPED
EXISTING_SKIP: AutoDepositCategoryReservePolicyTest.allocatesFuelAsOneCategoryTotalInsteadOfPerItem

CLEAN_FORCED_BUILD_COMMAND: .\gradlew.bat clean build --rerun-tasks
CLEAN_FORCED_BUILD: BUILD SUCCESSFUL; 171 OF 171 ACTIONABLE TASKS EXECUTED
FRESH_1_20_1_JAR: versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar
FRESH_1_20_1_JAR_SHA256: 7D52F24AFA397064EC97F199297B6F8421BAD7392CE0398C35188C3F8627CD11

ACTIVE_INSTANCE_JAR_SHA256: 84C6634433D7402B2935ADD6E4028F43BD3782D2E4038E00D30206092E9CA839
ARTIFACT_IDENTITY: MISMATCH
PYTHON_RUNTIME_HARNESS_SOURCE: PRESENT; 86 FILES; 21 TEST FILES
PYTHON_HERMETIC_AND_LIVE_ENABLEMENT_AUDIT: PASS
PYTHON_AUTOMATIC_DEPOSIT_TEST: 83 OF 83 PASSED
PYTHON_REUSED_COMMON_TESTS: 221 OF 221 PASSED; PREFLIGHT 88; OBSERVATION 20; SUBMISSION 44; BATCH 69
PYTHON_TOTAL_TEST: 304 OF 304 PASSED
PYTHON_LIVE_ENTRYPOINT: run_automatic_deposit_matrix.py
PYTHON_LIVE_ENTRYPOINT_DEFAULT: INCONCLUSIVE; SUBMIT_CALL_COUNT 0; PROCESS EXIT 2
PYTHON_LIVE_NETWORK_MINECRAFT_WORLD_MUTATION: 0
FINAL_ARTIFACT_RUNTIME_MATRIX: NOT_RUN
CURRENT_RUNTIME_VERDICT: INCONCLUSIVE_ARTIFACT_MISMATCH
MERGE_RELEASE_VERDICT: NOT_READY
COMMIT_PUSH_DEPLOYMENT: NOT_PERFORMED
```

Stage 2의 현재 구현은 process-lifetime diagnostics session에서 shared hard cap `5000`,
ordinary ceiling `4936`, critical reserve `64`, canonical cap event admission 및 final snapshot
accounting을 Java 결정적 테스트로 검증한다. Stage 3의 현재 구현은 tool-selection
dedupe/rate-limit/suppression 상태를 shared admission 아래에서 bounded하게 유지하며 OFF에서는
관련 diagnostic state를 변경하지 않는 계약을 Java 테스트로 검증한다. 이는 과거 diagnostics
계획에 열거된 모든 D-slice나 deposit root cause가 완료됐다는 뜻이 아니다.

Terminal four-record group에 대해 보장하는 범위는 token claim과 admission의 all-or-none이다.
Sink가 중간에 실패하면 physical write는 `0..4`일 수 있으며 slot refund 또는 자동 retry는 없다.
Final snapshot이 출력되는 그 line 자체는 self-accounting상 `pending=1` 및 delivery
`UNVERIFIED`를 보일 수 있다. Emit 호출이 반환된 뒤의 in-memory snapshot에서
`pending=0`, `completed=1`로 닫히는 사실과 구분해서 해석해야 한다.

독립 재감사에서 AutoDepositPolicy와 StoreHome diagnostics의 shared ledger, budget 및 state
mutation은 eligibility read lease 안으로 이동한 것으로 확인됐다. 반면 frozen
`DoStuffInContainerTask.beginPostPlaceOpenIntentIfNeeded`에는 mode 확인 뒤 leased begin 전에
호출자 flag를 설정하는 경합이 남아 있다. 같은 handoff 도중 diagnostics가 OFF로 바뀌면 재활성화
뒤 intent 관측이 누락될 수 있는 `OBSERVABILITY_GAP`이며, 현재 물증만으로 gameplay defect라고
분류하지 않는다. Handoff production 동결 때문에 이 P1은 deferred 상태로 남긴다.

Fresh JAR과 active CurseForge instance JAR의 hash가 다르므로 현재 열린 Minecraft에서 얻는
결과는 이 절의 source/build를 검증할 수 없다. Fresh JAR의 안전한 배포, Minecraft 재시작,
loaded code-source 확인 및 fixture별 runtime evidence가 같은 artifact hash에 연결된 뒤에만
Stage 6 row를 PASS/FAIL로 판정한다. 그 전에는 `NOT_RUN` 또는 `INCONCLUSIVE`다.

Python harness는 Java 내부 계약을 대신 판정하지 않는다. Source/deployed/runtime-loaded JAR,
mods directory, Carry On loader/config, exact latest.log cursor, canonical JUnit XML provenance,
fixture 및 gameplay before/after world·inventory snapshot을 typed evidence로 결합한다. 증거가
누락되거나 교체·회전·truncate·상호 불일치하면 PASS를 만들지 않고 `INCONCLUSIVE`로 닫힌다.
실제 runtime bridge의 strict `automatic-deposit-runtime-artifact/v1` evidence, 필요한 JUnit XML
provenance properties 및 operator fixture/gameplay evidence는 아직 없으므로 LIVE matrix를 실행하지
않았다. 현재 단일 non-test entry point 이름은 사전 승인 queue가 아니라 evidence gate를 나타내는
`run_automatic_deposit_matrix.py`이며, transport 구현은 의도적으로 연결돼 있지 않다.

## 16. 2026-09-01 runtime 책임 분리와 다음 실행 순서

이 절은 Section 12와 Section 15에 흩어진 검증 책임을 한곳에 고정한다. 과거 상태와 실행 기록을
소급 변경하지 않으며, 이 절 이후의 테스트 보강과 runtime 판정에서는 아래 책임 분리를 canonical
방향으로 사용한다.

Section 12의 단계별 사전 승인 문구는 당시 상태와 권한 경계를 설명한 역사 기록으로 보존한다.
이 절 이후 repository 내부의 test 보강, bounded log 보강, evidence-backed 최소 수정과 동일
test/runtime 재검증의 완료 조건은 사전 승인 기록이 아니라 evidence gate다. 다만 이 문구는
`AGENTS.md` 또는 현재 사용자 지시가 별도 권한을 요구하는 build, Minecraft launch, deployment,
profile/world mutation, commit 또는 push를 자동으로 허가하지 않는다.

### 16.1 검증 계층과 evidence owner

모든 계약을 Python 하나로 검증하지 않는다. 각 사실은 그 사실을 직접 관찰하거나 결정적으로
재현할 수 있는 계층이 소유한다.

| 검증 계층 또는 역할 | 직접 소유하거나 수집·판정하는 검증 | 소유하지 않는 검증 |
| --- | --- | --- |
| `JAVA_DETERMINISTIC` | generation과 candidate 전환, operation 간 상태 격리, child 수명주기와 정리, single-tick barrier, shared cap 산술, admission quota, terminal accounting, OFF 불변식 | 실제 Minecraft 월드에서 컨테이너가 열리고 아이템이 이동했다는 사실 |
| `PYTHON_HARNESS` 수집·orchestration 역할 (`evidenceOwner` 아님) | LAVI/Minecraft preflight, scenario 순서, byte-cursor 이후 로그 수집, schema별 identity 결합, typed result 검증, owner별 evidence materialization과 PASS/FAIL/INCONCLUSIVE 보고 | 독립 evidence owner가 되거나 Java 내부 상태를 로그 부재만으로 추정하거나 실제 gameplay 결과를 합성하는 것 |
| `HARNESS_CONTROL` | invocation/run identity, submit call count `0` 또는 `1`, no-submit/exactly-once 계약, one-shot guard와 reconciliation 상태 | 명령이 gameplay 목표를 달성했다는 사실이나 누락된 runtime event를 대신 증명하는 것 |
| `ARTIFACT_PREFLIGHT` | source/deployed/runtime-loaded code-source identity, mods directory와 duplicate JAR, backend·instance identity, 실제 Carry On installed/absent JAR·version·config identity | gameplay 성공이나 Java 내부 계약을 대신 판정하는 것 |
| `OPERATOR_FIXTURE` | 의도한 row와 실행 전 inventory pressure, protected/reserved item 구성, trusted destination 위치·용량, CHEST/BARREL 후보 조건, 요청한 Carry On profile, world snapshot identity | 실제 loaded profile이나 실행 후 제품 결과를 대신 판정하는 것 |
| `RUNTIME_LOG` | structured event와 typed lifecycle/terminal result, request·correlation·operation 연계, 실제 admission/emission·terminal reason | durable world/inventory 변화나 Java 내부 cap 산술을 단독으로 증명하는 것 |
| `GAMEPLAY_OBSERVATION` | 실행 전후 world·inventory 변화, 실제 placement/open/transfer/stop 효과와 금지된 부작용의 부재 | artifact identity, submit 횟수, 동일 tick 내부 순서 또는 로그에만 존재하는 terminal event를 대신 증명하는 것 |

Python harness는 이미 생성된 canonical JUnit XML과 artifact provenance를 읽어 manifest에 연결할
수 있지만 Java-owned 불변식을 실행하거나 재판정하지 않는다. Minecraft runtime 로그를 파싱할
수 있지만 `GAMEPLAY_OBSERVATION`의 world·inventory 전후 증거가 없으면 durable gameplay effect를
단정하지 않는다. Operator fixture가 준비됐다는 기록만으로 제품 동작이 성공했다고 판정하지
않는다. 각 row가 요구하는 evidence owner의 필수 증거가 같은 source, deployed, runtime-loaded
JAR identity에 연결될 때만 row를 `PASS`로 판정한다.

### 16.2 구현 및 재검증 우선순위

현재 우선순위는 다음과 같다.

1. out-of-row raw StoreHome one-shot 실행에서 이미 관찰된 diagnostics terminal-summary failure
   signature를 Java 회귀 테스트로 고정하고 해결한다.
   - `STORE_HOME_OPERATION_TERMINAL_SUMMARY`가 `SUMMARY` 문자열 선검사 때문에
     `AGGREGATE_CHECKPOINT`로 분류되고 aggregate quota `8` 소진 뒤
     `FAMILY_QUOTA_EXHAUSTED`로 억제된 producer/classifier 경계를 검증한다.
   - exact StoreHome terminal event의 의도된 family 또는 explicit producer classification을
     결정하고, aggregate quota 소진 뒤에도 unrelated aggregate family에 의해 굶지 않는지
     admission과 bounded emission을 검증한다.
   - 다른 `*_SUMMARY` event까지 무조건 terminal family로 바뀌지 않는지 함께 검증한다.
   - 이 단일 StoreHome terminal-summary 분류 결함은 Stage 2의 four-record terminal group,
     shared cap, final snapshot과 pending accounting 계약과 분리한다.
   - 최소 diagnostics owner만 수정한 뒤 focused Java test, 전체 Java test, clean forced build,
     JAR identity 확인과 동일 Minecraft scenario 재실행으로 닫는다.
2. `latest.log` production runtime-log parser/binding을 Python adapter로 구현한다.
   - 기존 `latest_log_cursor_reader.py`와 공통 preflight/observation/submission 책임을 재사용한다.
   - bounded parser는 완전한 cursor 이후 record만 받는다. 한 record에 모든 ID가 존재한다고
     가정하지 않고 event schema별 required/present identity tuple을 manifest·bridge·log 사이에서
     검증하여 같은 실행으로 결합한다.
   - Java terminal event와 bridge typed result를 상호 검증한다. 유효한 artifact와 fixture에서
     필수 terminal event의 잘못된 family 분류 또는 quota rejection이 직접 관찰되면 해당
     diagnostics assertion은 `FAIL`이다. 로그 교체·절단·회전, partial payload, parser ambiguity,
     필수 identity 또는 observation 누락처럼 원인을 증명할 수 없을 때만 `INCONCLUSIVE`로 닫는다.
3. 각 Minecraft runtime row를 구현하고 실행한다.
   - 현재 catalog의 R4=`@deposit_all`, R7=`@stop`, P1=`@store_home`은
     `OPERATOR_MANUAL_OBSERVE_ONLY`를 유지한다. Exact serialized command route와 one-shot 계약이
     증명되고 catalog·manifest·hermetic test가 함께 갱신된 row만 이후
     `SUPERVISED_LAVI_SUBMIT`으로 전환할 수 있다.
   - R7은 active operation 중 일반 supervised preflight가 요청을 거부하는 경계와 충돌하므로,
     현재 manual observation을 유지하거나 별도의 active-operation-safe interruption transport가
     증명되기 전에는 generic one-shot runner로 실행하지 않는다.
   - inventory pressure에 의한 automatic deposit은 `NO_COMMAND_AUTOMATIC_TRIGGER`로 실행하며
     Python submit call은 0이어야 한다.
   - manual observation, trusted-full fallback, CHEST/BARREL 전환, Carry On 미설치와 일반
     interaction 회귀는 row별 독립 fixture manifest와 artifact-preflight record를 사용하되,
     비교 대상 row는 동일 final JAR hash에 연결한다. Carry On installed/absent는 별도
     profile/process를 사용한다.

이 순서는 다음과 같이 요약한다.

```text
diagnostics terminal-summary failing evidence와 Java 회귀 테스트
-> 최소 diagnostics 수정
-> Java 재검증, clean build, JAR identity 및 동일 runtime 재현
-> Python production-log adapter와 hermetic fake-log/status 테스트
-> row별 operator fixture 준비
-> Python orchestration을 통한 final-JAR Minecraft runtime matrix
-> owner별 증거 결합과 PASS / FAIL / INCONCLUSIVE 판정
-> frozen handoff lifecycle 위반이 직접 증명된 FAIL만 HANDOFF_DEFECT_FOUND로 별도 태깅
```

### 16.3 현재 runtime harness 경계

Python에서 LAVI를 거쳐 Fabric ChatClef Java AI로 명령을 전달하고 bridge request terminal
status를 관찰하는 연결 자체는 사용할 수 있다. 그러나 이 연결 성공만으로 전체 runtime
harness가 완성된 것은 아니다. 다음이 모두 연결되기 전까지 전체 runtime matrix는 미완성이다.

```text
production runtime-log event/correlation parser and binding
row-specific log and typed-result oracle
operator-confirmed fixture manifest
world and inventory before/after evidence
Carry On installed/absent profile evidence
source/deployed/runtime-loaded JAR identity
scenario별 no-submit / exactly-once-submit 계약
```

따라서 현재 단계에서는 개별 command lifecycle 또는 typed transfer 결과를 확인할 수 있어도,
필수 terminal 로그나 fixture·gameplay 증거가 빠진 row를 전체 automatic-deposit runtime `PASS`로
승격하지 않는다. 전체 runtime harness 완료 여부는 Python 파일 수나 연결 가능 여부가 아니라,
위 책임 소유자별 증거가 final JAR의 각 matrix row에서 실제로 결합되는지로 판정한다.

### 16.4 2026-09-01 live runtime evidence delta

이 절은 Section 15의 2026-08-31 시점 기록을 소급 변경하지 않는다. 아래 값은 그 이후 수행된
배포·Python 검증과 one-shot StoreHome runtime smoke evidence이며, Section 15에서 `current`로
표현된 artifact mismatch, transport 미연결과 runtime 미실행 상태를 이 시점부터 대체한다.

```text
EVIDENCE_DATE: 2026-09-01
STAGE_2_SESSION_CAP_TERMINAL_CORE: IMPLEMENTED_AND_JAVA_VERIFIED
STAGE_2_STORE_HOME_TERMINAL_INTEGRATION: SOURCE_AND_LOG_FAILURE_SIGNATURE_FOUND; STRICT_ARTIFACT_BINDING_OPEN

SOURCE_1_20_1_JAR_SHA256: 7D52F24AFA397064EC97F199297B6F8421BAD7392CE0398C35188C3F8627CD11
ACTIVE_INSTANCE_JAR_SHA256: 7D52F24AFA397064EC97F199297B6F8421BAD7392CE0398C35188C3F8627CD11
ARTIFACT_IDENTITY: SOURCE_AND_DEPLOYED_MATCH
RUNTIME_LOADED_CODE_SOURCE: NOT_SEPARATELY_PROVEN

PYTHON_AUTOMATIC_DEPOSIT_TEST: 83 OF 83 PASSED
PYTHON_REUSED_COMMON_TESTS: 229 OF 229 PASSED; PREFLIGHT 91; OBSERVATION 20; SUBMISSION 49; BATCH 69
PYTHON_TOTAL_TEST: 312 OF 312 PASSED

PYTHON_RAW_ONE_SHOT_RUNTIME: EXECUTED_OUT_OF_ROW
RAW_ONE_SHOT_INVOCATION_ID: p1-store-home-raw-20260901-0104-01
TRANSPORT_MODE: RAW_OUT_OF_ROW_ONE_SHOT
RAW_INVOCATION_SUBMIT_CALL_COUNT: 1
RAW_INVOCATION_AUTOMATIC_RESUBMIT_COUNT: 0
REQUEST_ID: lavi-gui-e3b06ec87f234c26b4c8e635dc2978c3
CORRELATION_ID: lavi-430f15a7b3524c52aa7038a0f3623c24
SESSION_ID: fabric-chatclef-fed969bb334343b5bcc545eacffab768
CONNECTION_GENERATION: 1
PRE_RUN_LATEST_LOG_BYTE_OFFSET: 1567213
STORE_HOME_BRIDGE_TYPED_RESULT: COMPLETED
STORE_HOME_RESULT_REASON: paired_delta_confirmed
STORE_HOME_BRIDGE_REPORTED_STORED_ITEMS: 787
STORE_HOME_BRIDGE_REPORTED_REMAINING_STACKS: 0
STORE_HOME_BRIDGE_REPORTED_GOAL_SATISFIED: true
STORE_HOME_TERMINAL_LIFECYCLE: OBSERVED_BY_BRIDGE_TYPED_RESULT
STORE_HOME_ACTIVE_REQUEST: CLEARED
STORE_HOME_OPERATION_TERMINAL_SUMMARY: ABSENT

STORE_HOME_BRIDGE_REPORTED_COMMAND_SMOKE_RESULT: PASS
DIAGNOSTICS_TERMINAL_SUMMARY_FAILURE_SIGNATURE: OBSERVED
DIAGNOSTICS_TERMINAL_SUMMARY_STRICT_ASSERTION: INCONCLUSIVE_ARTIFACT_PROVENANCE
STRICT_P1_MATRIX_ROW: NOT_FORMALLY_RUN; CURRENT_EVIDENCE INCONCLUSIVE
STRICT_MATRIX_ROWS_COMPLETED: 0
FINAL_ARTIFACT_RUNTIME_MATRIX: NOT_FORMALLY_RUN; INCOMPLETE
MERGE_RELEASE_VERDICT: NOT_READY
DEPLOYMENT: PERFORMED; SOURCE_AND_ACTIVE_JAR_HASH_MATCH
COMMIT_PUSH: NOT_PERFORMED
```

이 failure signature는 bridge typed result가 terminal status와 787개 저장을 보고한 사실과
별개다. Source inspection상 `STORE_HOME_OPERATION_TERMINAL_SUMMARY`에는 `SUMMARY`와 `TERMINAL`이
모두 포함되지만 현재 classifier는 `SUMMARY/CHECKPOINT/SNAPSHOT`을 `TERMINAL`보다 먼저 검사한다.
Raw run의 cursor 이후 로그에서는 두 개의 `TOOL_SAVE_POLICY_SNAPSHOT_PUBLISHED`와 여섯 개의
`INVENTORY_SUBTRACKER_DIAGNOSTIC_REPEAT_SUMMARY`가 aggregate quota `8`을 사용한 뒤 StoreHome
terminal-summary가 보이지 않았다. Source classifier와 이 log signature는
`AGGREGATE_CHECKPOINT`의 `FAMILY_QUOTA_EXHAUSTED` 경계를 지목한다. 이 rejection reason은 별도
runtime log record에서 직접 읽은 값이 아니라 classifier와 quota admission code에서 결정적으로
귀결되는 결과다.

따라서 bridge-reported command smoke만 `PASS`다. Durable StoreHome gameplay 결과는
`GAMEPLAY_OBSERVATION`이 없으므로 아직 PASS가 아니며, 필수 diagnostics terminal-summary의 strict
runtime assertion도 runtime-loaded code-source provenance가 없어 `INCONCLUSIVE`다. Source와 log가
지목한 classifier/quota failure signature는 Java 회귀 테스트의 입력 물증으로 사용한다. 전체
automatic-deposit matrix는 `NOT_FORMALLY_RUN / INCOMPLETE / NOT_RELEASE_READY`다. Bridge request
terminal status는 diagnostics event의 admission 또는 physical emission을 대신 증명하지 않는다.
다음 구현 단위는 이 exact producer/classifier 경계의 Java 회귀 테스트, 최소 diagnostics 수정,
동일 artifact build/deployment 및 loaded code-source proof를 포함한 동일 runtime 재검증이다.
