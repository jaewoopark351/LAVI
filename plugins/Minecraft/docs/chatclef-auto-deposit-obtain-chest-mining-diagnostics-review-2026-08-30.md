<!-- 20260830_openai: Documented the corrected observer-contamination finding and evidence-gated obtain-chest/mining diagnostics direction without authorizing source changes. -->
<!-- 20260830_openai: Added the later 23:26 KST reproduction verdict separating the strongly attributed Carry On pickup preceding the log-confirmed entry into OBTAIN_CHEST from the still-unproven secondary Baritone stall. -->
<!-- 20260831_openai: Marked the pre-implementation source baseline as historical, fixed rotated-log provenance, and aligned the observable-boundary verdict with the root-cause patch gate. -->

# ChatClef Automatic Deposit Obtain-Chest Mining Diagnostics Review

Date: 2026-08-30
Last reviewed: 2026-08-31
Scope: Fabric ChatClef 1.20.1 automatic-deposit pre-slot acquisition, mining, and Baritone diagnostics

## 1. 문서 상태와 권한

이 문서는 automatic deposit 재현이 slot action 이전의 `OBTAIN_CHEST` 하위 경로에서
정체된 사건을 대상으로, 현재 로그 해석을 보정하고 다음 bounded diagnostics-only
관찰 계약을 기록한다.

```text
INCIDENT_SCOPE: EARLIER_OBTAIN_CHEST_MINING_CAPTURE
DOCUMENT_STATUS: DOCUMENTATION_ONLY
FINAL_ROOT_CAUSE: UNPROVEN
ACTIVE_CHILD_CHURN: UNPROVEN
OBSERVER_CONTAMINATION_MECHANISM: CURRENT_WORKTREE_SOURCE_CONFIRMED
RUNTIME_OBSERVER_CONTAMINATION: RUNTIME_COMPATIBLE_INFERENCE
STRONGEST_RUNTIME_HYPOTHESIS: BARITONE_SEMANTIC_STALL_WITH_PROGRESS_RESET
STRONGEST_RUNTIME_HYPOTHESIS_STATUS: INFERENCE
BEHAVIOR_FIX: BLOCKED
SOURCE_EDIT: NOT_AUTHORIZED_BY_THIS_DOCUMENT
TEST_OR_BUILD: NOT_RUN_AND_NOT_AUTHORIZED_BY_THIS_DOCUMENT
MINECRAFT_LAUNCH_OR_REPRODUCTION: NOT_RUN_AND_NOT_AUTHORIZED_BY_THIS_DOCUMENT
DEPLOY_OR_FILE_COPY: NOT_PERFORMED
COMMIT_OR_PUSH: NOT_PERFORMED
WIRE_PROTOCOL_CHANGE: NONE
DEPENDENCY_OR_VERSION_CHANGE: NONE
```

> §§1-17 전체는 20:09 KST earlier mining capture와 당시의 pre-implementation
> dirty-working-tree source snapshot을 기록한 historical baseline이다. 특히 §5.2,
> §10.4, §11과 §16의 `현재`·`current`·`future` 표현, line range와 SHA-256은 그
> 관찰 시점에만 유효하며 later 23:26 reproduction 또는 현재 working tree의 source
> 상태로 확대하지 않는다. 같은 날 23:26 KST에 시작된 later reproduction은 §18에서
> 별도 correlation과 runtime artifact로 판정한다. §§10-15의 observation-purity 및
> behavior safety gate는 §18이 명시적으로 investigation 순서만 좁히는 범위 밖에서는
> 계속 유효하다.

이 문서의 작성 승인은 Java, test source, Gradle, resource, JAR, CurseForge instance,
Minecraft runtime, commit 또는 push 변경 승인이 아니다. 향후 diagnostics-only source
작업, build, 배포, runtime reproduction과 behavior fix는 각각 별도 범위와 권한으로
다룬다.

## 2. 문서 관계와 소유 범위

이 문서는 이번 incident의 runtime evidence, observer-contamination 판정과 다음
diagnostics-only 적용 순서만 소유한다. Generic event envelope, reusable Task lifecycle
규칙과 event family의 canonical 이름은
[ChatClef Task Lifecycle Diagnostics](chatclef-task-lifecycle-diagnostics.md)가 계속
소유한다.

이번 incident가 추적하는 경계는 다음과 같다.

```text
automatic deposit
-> OBTAIN_CHEST
-> MineAndCollectTask.MineOrCollectTask
-> DestroyBlockTask candidate / active-child reconciliation
-> DestroyBlockTask finish evaluation origin
-> Baritone goal, calculation, adoption and semantic progress observation
```

다음 문서의 기존 계약을 대체하거나 소급 수정하지 않는다.

- [ChatClef Automatic Deposit Slice A Diagnostics Contract](chatclef-auto-deposit-slice-a-diagnostics-contract-2026-08-30.md)는 transfer, slot action/mutation, tracker, movement invalidation과 store terminal identity를 소유한다.
- [ChatClef Automatic Deposit Transfer / Movement / Carry On Diagnostics Review](chatclef-auto-deposit-transfer-movement-carryon-diagnostics-review-2026-08-30.md)는 별도 transfer/movement/Carry On incident chronology를 소유한다.
- [ChatClef Task Lifecycle Diagnostics](chatclef-task-lifecycle-diagnostics.md)는 일반 Task, mining, DestroyBlock과 Baritone 진단 규칙을 소유한다.
- [ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)는 optional Carry On, upstream 보존, diagnostics-first와 behavior gate를 소유한다.
- [ChatClef @deposit_all Ocean Loop Diagnostics Plan](chatclef-deposit-all-ocean-loop-diagnostics-plan-2026-08-26.md)는 automatic pressure composition의 역사와 기존 activation ledger를 소유한다.
- [ChatClef Diagnostics Refactoring Backlog](chatclef-diagnostics-refactoring-backlog-2026-08-29.md)은 diagnostics facade extraction과 refactoring gate를 소유하며, 이번 incident는 refactoring 근거로 사용하지 않는다.

현재 실행은 Slice A의 slot-action boundary에 도달하기 전에 container-acquisition
descendant에서 정체됐다. 이것은 Slice A transfer contract가 잘못됐거나 로드되지
않았다는 증거가 아니다.

Generic mining reconciliation과 Store route reconciliation은 합치지 않는다.

```text
TASK_CHILD_RECONCILIATION
    generic Mine/Destroy candidate-to-active reconciliation

STORE_TASK_CHILD_RECONCILIATION
    automatic Store route lifecycle reconciliation
```

## 3. 기준선과 증거 등급

### 3.1 Earlier pre-implementation 문서화 작업의 저장소 기준선

```text
repository: C:\Vtuber_Souorce_Code\LAVI
branch: minecraft-plugin-fix/alto-clef-infinite-loop
local HEAD: 14ba9b443f0bc11d6860a25d7fd3b8b916d95a04
worktree: DIRTY - existing user changes preserved
remote GitHub HEAD equivalence: NOT_CHECKED
source inspection: READ_ONLY
test execution: NO
build execution: NO
Minecraft reproduction: NO
```

아래 source 판정은 당시 dirty working tree에서 직접 읽은 bytes에 대한 판정이다. local
`HEAD` commit 전체의 source와 동일하다고 확대하지 않는다.

### 3.2 외부 ChatGPT ZIP 검토의 provenance

사용자가 전달한 검토문은 외부 ChatGPT가 별도 ZIP snapshot을 기준으로 검토했다고
명시한다. 이번 Codex 첨부에는 ZIP 자체가 없고 검토문만 있다.

```text
external review input available to Codex:
  pasted-text.txt

pasted-text.txt SHA-256:
  DEC8A9477EF90121C19EB8721AB6ECFE1B596BB1A6E5874064F04481B3F3C672

ZIP path available to Codex: NO
ZIP SHA-256 available to Codex: NO
ZIP line-number provenance: ZIP_SNAPSHOT_ONLY
ZIP/current-worktree equivalence: NOT_ESTABLISHED_BY_EXTERNAL_REVIEW
ZIP/runtime-JAR source equivalence: NOT_ESTABLISHED
```

외부 검토의 줄 번호를 현재 working tree 또는 GitHub HEAD 줄 번호로 인용하지 않는다.
외부 ZIP 줄 번호는 `file + class + method` locator로만 보존한다. 당시 working tree에서
직접 대조한 항목은 §5.2와 §16에 기록한 당시 line range와 source fingerprint를 함께
사용한다. Source edit 뒤에는 line range가 이동할 수 있으므로 hash와 method locator도
같이 보존한다.

### 3.3 증거 등급

| 등급 | 의미 |
| --- | --- |
| `RUNTIME_LOG_CONFIRMED` | 2026-08-30 Fabric01의 명시된 capture path 또는 hash가 기록된 rotated archive의 관찰 구간·검색식에서 직접 확인한 사실 (`[근거 있음]`) |
| `CURRENT_WORKTREE_SOURCE_CONFIRMED` | Earlier 문서화 pass 당시 dirty working tree의 명시된 line range와 bytes를 직접 읽어 확인한 historical source 사실 (`[근거 있음]`). 현재 source와 같다는 뜻이 아니다. |
| `ZIP_SOURCE_REVIEW_REPORTED` | 외부 ChatGPT가 ZIP에서 확인했다고 보고했지만 ZIP bytes를 이번 작업에서 재검증하지 않은 사실 |
| `RUNTIME_COMPATIBLE_INFERENCE` | 해당 source snapshot의 mechanism과 runtime pattern이 양립하지만 deployed bytecode/source-snapshot equivalence 또는 호출 origin이 미확정인 추정 (`[추정]`) |
| `INFERENCE` | 확인된 사실과 양립하지만 동일 causal identity로 직접 관찰하지 못한 가설 (`[추정]`) |
| `STRONG_TEMPORAL_ATTRIBUTION` | 같은 interaction의 직전·직후 carry-state 변화, clicked-target 제거와 즉시 이어진 route 변화가 pickup attribution을 강하게 지지하지만 exact carried-block API identity 또는 내부 activation owner는 확인되지 않은 상태 |
| `UNPROVEN` | 현재 증거로 occurrence, owner 또는 원인을 확정할 수 없음 (`[확인 불가]`) |
| `NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION` | 값을 얻으려면 lifecycle predicate나 상태 영향 API를 다시 호출해야 하므로 의도적으로 수집하지 않음 |
| `BARITONE_BINARY_SOURCE_REQUIRED` | 실제 runtime Baritone binary와 동일한 source/decompile이 없어 내부 branch 의미를 확정할 수 없음 |

## 4. Earlier capture의 보정된 핵심 결론

Earlier capture에서 10만 회 이상 반복된 다음 패턴은 actual active-child churn의
증거가 아니다.

```text
destroy_block_is_finished
mine_or_collect_return_destroy_block_task
destroy_block_is_finished
mine_or_collect_return_destroy_block_task
...
```

Pre-implementation source snapshot과 earlier 로그가 지지하는 더 정확한 모델은 다음과
같다.

```text
MineOrCollectTask#getGoalTask(block target)가 호출될 때 새 DestroyBlockTask candidate를 생성
-> 같은 target candidate는 Task.tick()에서 기존 active child와 비교됨
-> diagnostics가 이전 goalTask.isFinished()를 별도 평가할 수 있음
-> VISIBLE_TASK_RETURN fingerprint가 새 candidate identity를 포함
-> 반복 invocation의 candidate allocation이 semantic state change처럼 출력될 수 있음
```

따라서 다음 두 문장을 분리한다.

```text
new DestroyBlockTask candidate allocated on each applicable getGoalTask invocation:
  CURRENT_WORKTREE_SOURCE_CONFIRMED

getGoalTask invoked on every runtime tick:
  UNPROVEN_WITHOUT_TICK_SEQUENCE_RECONCILIATION

active DestroyBlockTask replaced every tick:
  UNPROVEN
```

`DestroyBlockTask.isFinished()`에서 얻은 `isAir=true`는 그 호출이 수행한 client-world
read의 실제 결과다. 다만 그 호출이 active lifecycle, diagnostic probe, 이미 정지한
Task 또는 다른 operation/world context 중 어디에서 발생했는지는 현재 로그에 없다.

이 보정은 automatic deposit 정체 자체를 부정하지 않는다. 정체가 slot action 이전에
발생했다는 사실은 유지하지만, 첫 실패 owner를 Task churn으로 확정했던 해석은 철회한다.

## 5. Earlier capture에서 확정된 사실

### 5.1 Earlier runtime log에서 확정된 사실

Primary Minecraft evidence corpus:

| 파일 | bytes | 관찰 범위 |
| --- | ---: | --- |
| `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\2026-08-30-2.log.gz` | compressed 10,375,432; decompressed 321,815,646 | earlier runtime evidence의 canonical rotated archive; first timestamp `17:18:45`, last timestamp `20:09:46` KST |
| capture-time pointer `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log` | 321,815,646 at capture | 이후 회전돼 현재 같은 path의 bytes와 line number를 evidence로 사용하지 않음 |
| capture-time mirror `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt` | 389,763,282 at capture | last write `20:09:47` KST; 아래 핵심 count가 당시 `latest.log`와 동일했지만 frozen canonical carrier로 사용하지 않음 |
| `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\instance_audit.txt` | 2,416,981 | last write `17:18:41` KST; instance/launch context only |
| `C:\Vtuber_Souorce_Code\LAVI\logs\20260830_171917_log.txt` | dynamic, not frozen | LAVI application context only; 계속 기록될 수 있어 size를 evidence로 고정하지 않으며 Minecraft event count에 사용하지 않음 |

Earlier runtime archive identity:

```text
path:
  C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\2026-08-30-2.log.gz
compressed bytes:
  10,375,432
compressed SHA-256:
  9E0F781D15F4EAE8F10B07F9CBDA5846F902A2E8D7B743DE6240DA110F63C7C4
uncompressed bytes:
  321,815,646
uncompressed SHA-256:
  54C5D368D01698A0C837215B22E30429D479A66C01F665136F7319EAC592D4FD
uncompressed logical lines:
  402,092
```

2026-08-30 documentation pass에서 계산한 exact fixed-string count를 이 archive의
UTF-8 decompressed content에서 다시 검증한 결과는 다음과 같다. 0은 위 archive 전체
관찰 범위에서 해당 문자열을 찾지 못했다는 뜻이며, 가능한 모든 동의어·예외 형태의
부재를 뜻하지 않는다.

| 검색식 | count | 해석 |
| --- | ---: | --- |
| `event=VISIBLE_TASK_FINISHED_CHECK reason=destroy_block_is_finished` | 183,335 | finish result event 반복 |
| `event=VISIBLE_TASK_RETURN reason=mine_or_collect_return_destroy_block_task` | 183,351 | candidate return event 반복 |
| `targetPosition=-1011,84,-162` | 169,343 | 반복 birch-log target correlation |
| `event=STORE_DEPOSIT_SLOT_ACTION` | 0 | inspected run이 Slice A slot-action boundary에 도달했다는 증거 없음 |
| `event=STORE_DEPOSIT_SLOT_MUTATION` | 0 | inspected run의 slot mutation 증거 없음 |
| `event=MINING_OPERATION_TERMINAL_SUMMARY` | 0 | mining operation close/abort summary 자체가 아직 없음 |
| `terminalReason=TIMEOUT` | 0 | exact timeout marker 미관측 |
| `Mixin apply failed` / `MixinApplyError` | 0 / 0 | 이 두 exact Mixin failure marker 미관측 |
| `NoClassDefFoundError` | 0 | exact class-linkage marker 미관측 |
| `OutOfMemoryError` / `Java heap space` | 0 / 0 | 이 두 exact OOM marker 미관측 |

첫 `get emerald 30` request는 `17:36:05`에 보이고, 같은 command context는 종료 직전
`20:09:46`의 마지막 command-context-bearing BOUNDARY line에도 남아 있다. 이것만으로
command scheduler의 active 상태나 terminal callback 부재를 확정하지 않는다.
Inspected diagnostic event는 `level=BOUNDARY`를 기록한다. `STORE_DEPOSIT_TERMINAL_SUMMARY`는
전체 파일에 8건 존재하므로 Store operation terminal과 이 문서가 요구하는 mining
operation terminal을 혼동하지 않는다.

Earlier documentation pass의 artifact identity는 당시 다음 두 실제 경로에서 별도로
다시 계산했다.

| artifact | 절대 경로 | bytes | SHA-256 |
| --- | --- | ---: | --- |
| earlier local build output | `C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar` | 7,262,957 | `F73ADE284AD1FE43D49639B5A3E0FA05342E74BBFC6F0109F8855483F2DE2B4C` |
| earlier deployed instance JAR | `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\chatclef-1.20.1-0.18.23.jar` | 7,262,957 | `F73ADE284AD1FE43D49639B5A3E0FA05342E74BBFC6F0109F8855483F2DE2B4C` |

JAR hash 일치는 deployed artifact와 위 local build artifact의 byte identity를
증명한다. 외부 ZIP source, 현재 dirty working-tree source 또는 local `HEAD`가 이 JAR의
source와 동일하다는 증거는 아니다. 따라서 source에서 확인한 observer mechanism이
실행 JAR에서 같은 방식으로 발생했다는 판정은 현재 `RUNTIME_COMPATIBLE_INFERENCE`다.

### 5.2 Pre-implementation dirty-working-tree snapshot에서 확정된 source 사실

#### Diagnostic argument가 lifecycle predicate를 먼저 평가

`AbstractDoToClosestObjectTask#onTick()`은 verbose `logEvent()` 인자로 다음 supplier를
전달한다 (`AbstractDoToClosestObjectTask.java:63-69`).

```text
goalTask != null && goalTask.isFinished()
```

Java는 `logEvent()` 진입 전에 인자를 평가하고, `ChatClefDiagnostics.logEvent()`의
VERBOSE gate는 메서드 내부에 있다 (`ChatClefDiagnostics.java:160-164`). BOUNDARY는
OFF가 아니므로 formatter가 supplier evaluation을 허용한다
(`DiagnosticFormatterFacade.java:35-39`). 따라서 출력이 최종 suppress되더라도
`goalTask.isFinished()`는 먼저 호출될 수 있다.

이 호출은 scheduler가 이미 수행한 completion result를 기록하는 것이 아니라,
diagnostics field를 만들기 위한 별도 lifecycle predicate 평가다.

#### Candidate allocation과 active reconciliation은 별도 사건

`MineAndCollectTask.MineOrCollectTask#getGoalTask()`는 block target에 대해 새
`DestroyBlockTask` candidate를 생성하고 반환한다
(`MineAndCollectTask.java:363-402`). `Task#tick()`은 그 candidate와 기존 active child의
`isEqualResult`, interruptibility와 replacement 여부를 계산한다
(`Task.java:68-135`). 이 source는 invocation마다 allocation됨을 증명하지만 해당 method가
runtime에서 매 tick 호출됐다는 사실까지 단독으로 증명하지 않는다.

`DestroyBlockTask#isEqual()`은 target position이 같을 때 true를 반환한다
(`DestroyBlockTask.java:664-680`). 따라서 같은 target의 새 object allocation은
정상적으로 기존 active child 유지와 candidate 폐기로
끝날 수 있다.

`Task#tick()`은 이미 다음 결과를 `MiningPathDiagnostics`에 전달한다.

```text
activeChildBefore
candidateChild
subTasksEqual
canInterruptEvaluated
canInterrupt
replacementApplied
previousChildStopCalled
activeChildAfter
candidateDiscardedBecauseEqual
```

Reconciliation 보강을 위해 logger가 `isEqual()`, `canBeInterrupted()` 또는
`isFinished()`를 다시 호출하거나 `Task.java`에서 값을 다시 계산할 필요가 없다.

#### VISIBLE fingerprint가 candidate identity를 semantic change로 사용

`VisibleTaskDiagnostics#logReturnTask()`는 `taskSummary(nextTask)`를 state key에
포함한다. 현재 task summary는 task class와 instance/run identity를 포함한다. 따라서
target과 semantic outcome이 같아도 새 candidate identity가 매번 fingerprint를
바꾼다 (`VisibleTaskDiagnostics.java:39-42`).

Candidate identity는 allocation 횟수 확인용 payload가 될 수 있지만 semantic dedupe
fingerprint가 되어서는 안 된다.

`VISIBLE_TASK_FINISHED_CHECK`에도 별도 증폭 mechanism이 있다. Dedupe state는
`WeakHashMap<Task, ...>`의 task별 bucket에 저장되고, `logFinishedCheck()`는 boolean
result만 state key로 사용한다 (`VisibleTaskDiagnostics.java:17-18,45-77`). 새 candidate는
자기 bucket에서 항상 첫 관측이므로 동일 `isAir=true`도 다시 출력될 수 있다. 이것은
해당 candidate가 inactive였거나 scheduler가 실제 교체했다는 증거가 아니다.

#### Finish predicate는 AIR 한 가지

`DestroyBlockTask#isFinished()`는 기존 world read 한 번에서 `BlockState.isAir()`를
계산해 그대로 반환한다. 현재 method 안의 VISIBLE finished-check는 result를 기록하지만
호출 origin과 active lifecycle 여부를 기록하지 않는다
(`DestroyBlockTask.java:647-654`). 같은 method의 unbounded
`Debug.logInternal("Block at position ... is air")`도 매 호출 출력될 수 있으므로 bounded
replacement projection만 정리해서는 고빈도 출력이 완전히 사라지지 않는다. 또한
`isEqual()`의 unbounded result log도 reconciliation마다 반복될 수 있다
(`DestroyBlockTask.java:664-680`). 두 upstream log hunk는 각각 별도 stop-gated
diagnostics cleanup 후보이며 동작 결과를 바꾸면 안 된다.

#### Destroy lifecycle evidence가 generic visible event와 중복

현재 START/STOP 경계는 generic `VISIBLE_TASK_LIFECYCLE`와 dedicated
`DESTROY_BLOCK_LIFETIME` 양쪽에 기록된다
(`DestroyBlockTask.java:230-243,598-610,627-636`). Churn 판정의 authoritative event는
run ID와 lifetime을 소유하는 `DESTROY_BLOCK_LIFETIME`으로 둔다. Generic visible
lifecycle을 계속 유지할지는 모든 caller 영향 audit 뒤 별도 결정하며, 두 event count를
합산해 START/STOP 수로 사용하지 않는다.

#### Diagnostic-only `WorldHelper.canBreak()`는 passive read가 아님

`WorldHelper#canBreak()`는 내부에서 `interactionPaused`를 false로 설정하고 이전 값으로
복원한다. 현재 다음 diagnostics 경로가 detail field 생성을 위해 이 helper를 다시
호출한다 (`WorldHelper.java:236-252`).

```text
DestroyNavigationDiagnostics
MineTargetGoalRequestDiagnostics
MineTargetSelectionDiagnostics
ReconciliationTaskSnapshot
```

복원 여부와 관계없이 state-changing helper를 observation 목적으로 호출하는 것은
diagnostics-only 불변 조건에 맞지 않는다. Gate 이후에도 호출하면 안 된다. 행동 경로가
이미 계산해 전달한 값이 없다면 다음처럼 남긴다.

```text
worldCanBreak=NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION
```

#### Deep snapshot이 emission gate보다 먼저 생성됨

현재 `TaskChildReconciliationDiagnostics`는 `activeBefore`, `candidate`, `activeAfter`
deep snapshot 세 개를 만든 뒤 fingerprint와 emission gate를 평가한다. Snapshot은 world
state, scanner와 `WorldHelper.canBreak()` 등을 재조회한다
(`TaskChildReconciliationDiagnostics.java:27-57`,
`ReconciliationTaskSnapshot.java:84-104`).

파일 출력이 suppress돼도 hot-path read와 state-influencing helper 호출은 이미
발생한다. 필요한 순서는 다음과 같다.

```text
already-computed local/identity/result
-> cheap semantic fingerprint
-> bounded permit
-> permitted event only: passive detail capture
-> emit
```

같은 원칙은 reconciliation 하나에만 적용되지 않는다. 현재
`DestroyBlockPhaseDiagnostics`도 unchanged phase 판정 전에 world/progress와 Baritone
snapshot을 캡처한다 (`DestroyBlockPhaseDiagnostics.java:24-45`). Existing mining event
family 전체를 call-site별로 감사하고, cheap state-change gate보다 앞선 deep capture를
각 LAVI-owned observer에서 제거해야 한다.

#### Pathing boolean이 progress checker를 reset

Pre-implementation source snapshot에서 다음 두 checker는
`pathingBehavior.isPathing()`이 true이면 reset된다.

```text
MineAndCollectTask.MineOrCollectTask progressChecker
DestroyBlockTask _moveChecker
```

현재 line range는 parent `MineAndCollectTask.java:319-359`, child
`DestroyBlockTask.java:317-323`이다. Existing `MOVEMENT_PROGRESS_CHECK_RESULT`는
`resetReason=unavailable_without_checker_internals`만 기록하므로
(`MovementProgressDiagnostics.java:39-55`) runtime에서 두 reset이 실제로
`BARITONE_PATHING` 사유로 발생했는지는 아직 직접 증명되지 않았다.

`isPathing=true`가 실제 executor/player 진행을 뜻하지 않는데 계속 true라면 progress
failure, unreachable request와 terminal로 이어질 시간이 누적되지 않을 수 있다. 이
mechanism은 source-confirmed지만 이번 runtime에서 실제로 semantic stall이 발생했다는
결론은 아직 `INFERENCE`다.

## 6. 아직 확정하면 안 되는 주장

```text
actual active DestroyBlockTask was replaced every tick
DestroyBlockTask.onStart() ran every tick
all observed isFinished calls came from scheduler lifecycle
BlockScanner stale cache is the direct cause
Baritone executor was making meaningful progress
customGoalActive owned the correct target and operation
Carry On pickup directly caused the current mining loop
the earlier Baritone NPE contaminated the current operation
player Y=20 and target Y=84 distance itself caused failure
same-target VISIBLE count proves task churn
```

## 7. 원인 가설 우선순위

| 순위 | 가설 | earlier review 판정 |
| --- | --- | --- |
| P0 | Diagnostic field evaluation과 candidate-scoped identity/bucket이 candidate 관측과 출력량을 증폭 | source mechanism은 `CURRENT_WORKTREE_SOURCE_CONFIRMED`; runtime occurrence는 `RUNTIME_COMPATIBLE_INFERENCE` |
| P1 | Active child는 유지되지만 Baritone이 `isPathing=true` semantic stall에 빠지고 두 checker가 reset | 가장 강한 실제 실패 `INFERENCE` |
| P2 | `Task#tick()`에서 same-target candidate가 실제 active child로 반복 교체 | 영향은 크지만 `UNPROVEN` |
| P3 | Parent target read와 active finish read 사이 AIR/non-AIR 모순 | `UNPROVEN` |
| P4 | Baritone goal submission, calculation, adoption 또는 executor ownership 경계 단절 | `UNPROVEN` |
| P5 | BlockScanner async publication 또는 filter invariant 문제 | 후순위 `UNPROVEN` |
| P6 | Carry On 또는 다른 mod가 동일 tick/target block state를 변경 | 동일 correlation 증거 전까지 낮음 |
| P7 | 과거 Baritone NPE가 현재 generation을 오염 | 현재 직접 증거 없음 |

P0는 로그 신뢰도를 훼손한 diagnostic defect다. P0가 automatic deposit runtime 정체의
최종 원인이라는 뜻은 아니다.

## 8. 기존 이벤트 재사용과 필요한 보강

Pre-implementation source snapshot에는 다음 event family가 이미 있다. 새 이름으로
복제하거나 rename하지
않고 기존 evidence continuity를 유지한다.

| 기존 이벤트 | 현재 상태 | 향후 diagnostics-only 방향 |
| --- | --- | --- |
| `MINE_TARGET_GOAL_REQUEST` | 존재 | 기존 local target state와 decision을 전달하고 logger 재조회·`canBreak()` 호출 제거 |
| `TASK_CHILD_RECONCILIATION` | 존재 | `Task#tick()`이 이미 계산한 equality/replacement를 사용하고 gate 전에 deep snapshot 생성 금지 |
| `DESTROY_BLOCK_LIFETIME` | 존재 | 실제 START/STOP만 기록해 candidate allocation과 active run 구분 |
| `DESTROY_BLOCK_PHASE_TRANSITION` | 존재 | phase의 semantic state change만 기록하고 unchanged tick 출력 금지 |
| `MOVEMENT_PROGRESS_CHECK_RESULT` | 존재하지만 reset origin 없음 | behavior branch가 이미 아는 `resetObservedBeforeCheck`와 `resetReason`만 전달; checker 재호출·reset 추가 금지 |
| `BARITONE_GOAL_PATH_TRANSITION` | 존재 | active Destroy run, target와 schedule-time operation correlation 보강 |
| `BARITONE_GOAL_REQUEST_DECISION` | 존재 | runtime binary와 일치하는 API 의미가 확인된 실제 result만 사용 |
| `BARITONE_CALCULATION_*` | 존재 | generation schedule 시 immutable correlation을 복사하고 worker에서 global context 재조회 금지 |
| `BARITONE_PATH_ADOPTION_DECISION` | 존재 | current/next executor adoption result와 정상 cancel/goal change를 구분 |
| `BARITONE_EXECUTOR_PROGRESS_SNAPSHOT` | 존재 | event rename 없이 semantic progress classification과 bounded heartbeat 보강 |
| `VISIBLE_TASK_RETURN` | 존재하지만 현재 mining call-site에서 noisy | 우선 `MineOrCollectTask` call-site를 기존 `MINE_TARGET_GOAL_REQUEST`로 대체; global helper fingerprint 변경은 모든 caller audit 뒤 검토 |
| `VISIBLE_TASK_FINISHED_CHECK` | result만 있고 task별 bucket 때문에 새 candidate마다 첫 event가 될 수 있음 | unbounded 병행 이벤트를 만들지 말고 bounded finish-origin projection으로 대체 |
| `VISIBLE_TASK_LIFECYCLE` | dedicated Destroy lifetime과 중복 | churn evidence는 `DESTROY_BLOCK_LIFETIME`만 authoritative; 중복 count 합산 금지 |
| `Debug.logInternal` in Destroy finish/equality | unbounded upstream text | `DESTROY_FINISH_EVALUATION`/`TASK_CHILD_RECONCILIATION`과 중복되지 않게 각 exact hunk를 별도 cleanup 후보로 stop-gate |

실질적으로 새 semantic projection이 필요한 핵심은 다음 하나다.

```text
DESTROY_FINISH_EVALUATION
```

이 이름은 제안이며 source 생성 승인이 아니다. 기존 `VISIBLE_TASK_FINISHED_CHECK`와
동시에 무제한 출력하는 새 event가 되어서는 안 된다. 기존 단일 world read와 return
result를 재사용해 finish origin과 active lifecycle을 분리하는 bounded replacement
projection이어야 한다.

`MINING_OPERATION_TERMINAL_SUMMARY` 같은 operation summary는 operation owner와 close
boundary가 증명된 뒤 검토한다. 첫 source slice에서 새 global mutable operation
registry를 만들지 않는다. 기존 terminal/cancel/world-leave/session-close 경계에 owner가
이미 있다면 summary emission만 별도 제안할 수 있다.

## 9. Existing payload preservation and additive observation delta

이 절은 새 canonical schema를 정의하지 않는다. 현재 emitter와 각 event payload의
field name은 evidence continuity의 일부이므로 rename, removal 또는 alias duplication을
하지 않는다. 아래 delta는 모두 additive-only이며 Minecraft bridge wire protocol 변경이
아니다.

### 9.1 기존 emitter envelope와 correlation ownership

BOUNDARY emitter가 이미 자동 제공하는 canonical base field는 다음과 같다
(`DiagnosticEventEmitter.java:57-82,95-117`).

```text
traceId
clientTickId
eventSequence
taskInstanceId
taskRunId
parentTaskRunId
threadName
level
event
reason
taskClass
```

VERBOSE emitter는 `event` 대신 기존 `eventType`과 `phase`를 사용한다
(`DiagnosticEventEmitter.java:21-50`). `clientTick`, `eventName` 같은 중복 alias를
추가하지 않는다.

다음 값은 해당 owner가 이미 보유하고 passive하게 전달할 수 있을 때만 additive
correlation 후보가 된다.

```text
emissionKind=FIRST|STATE_CHANGE|ANOMALY|HEARTBEAT|TERMINAL
commandRequestId
commandCorrelationId
commandSessionId
dimension
worldIdentity
targetPosition
destroyTaskRunId
baritoneGenerationId
suppressedCount
budgetRemaining
```

값이 안전하게 없으면 binding, registration 또는 activation을 추가하지 않고
`UNAVAILABLE`로 남긴다. Logger가 correlation을 얻으려고 다음을 수행하면 안 된다.

```text
bindChild()
registerOperation()
activateOperation()
parent task mutation
global current-command replacement
Store binding/registry lookup from generic mining diagnostics
```

`storeOperationId`와 Store binding은 `STORE_TASK_CHILD_RECONCILIATION` 등 Store observer가
소유한다. Generic `TASK_CHILD_RECONCILIATION`과 mining/Baritone logger는 command context,
parent task/run, Destroy run, dimension/world와 target만 소유한다. Exact Store link가 기존
read-only 경계에 없으면 `UNAVAILABLE`로 남기고 사후 event join을 사용한다.

Future operation budget의 preferred key는 이미 전달된 command correlation이다. 그것이
없으면 `parentTaskRunId + destroyTaskRunId + dimension + worldIdentity + targetPosition`의
passive correlation bucket을 사용한다. 이 값도 불완전하면 conservative
`session-unattributed` bucket으로 합치며 새 Store dependency나 global current-operation
registry를 만들지 않는다.

### 9.2 `MINE_TARGET_GOAL_REQUEST`

pre-implementation source payload의 canonical field name을 그대로 보존한다
(`MineTargetGoalRequestDiagnostics.java:32-79`). 핵심 existing subset은 다음과 같다.

```text
decisionOutcome
parentTaskClass
parentTaskInstanceId
returnedTaskClass
returnedTaskInstanceId
targetPosition
previousMiningPosition
miningPositionAfterDecision
targetChangedFromPreviousMiningPosition
targetRelationToPreviousMiningPosition
requestedBlockIds
targetBlockId
targetBlockState
blockStillMatchesRequestedType
chunkLoaded
worldCanBreak
scannerUnreachableBefore
localBlacklistContainsBefore
localBlacklistSize
```

`targetBlockState`, `targetBlockId`와 필요 시 additive `targetIsAir`는
`getGoalTask()`가 이미 읽은 local `targetState`에서만 가져온다. Candidate allocation
identity는 기존 `returnedTaskInstanceId` payload로 보존하되 fingerprint에서는 제외한다.
행동 경로가 계산하지 않은 `worldCanBreak`는 재평가하지 않고
`NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION`으로 기록한다. 기존 이름을
`targetStateId`, `selectedTarget`, `candidateTaskInstanceId` 같은 alias로 바꾸지 않는다.

### 9.3 `TASK_CHILD_RECONCILIATION`

현재 `ReconciliationDiagnosticPayload`의 canonical field name을 보존한다
(`ReconciliationDiagnosticPayload.java:45-123`). 핵심 existing subset은 다음과 같다.

```text
parentTaskInstanceId
activeChildBeforeInstanceId
activeChildBeforeTargetPosition
candidateChildInstanceId
candidateTargetPosition
candidateOutcome
reconciliationClassification
candidateBecameActive
activeChildRetained
activeChildChanged
candidateAndActiveShareTarget
candidateDifferentInstanceSameTarget
candidateWasActiveBefore
candidateDiscardedAsAllocationNoise
sameTargetReplacementApplied
isEqualResult
canInterruptEvaluated
canInterruptPreviousChild
replacementApplied
candidateDiscardedBecauseEqual
previousChildStopCalled
activeChildAfterInstanceId
activeChildAfterTargetPosition
parentLocalBlacklistSize
```

실제 lifecycle join에 필요하지만 현재 payload에 없는 다음 세 값은 기존 registry가
already-known run ID를 passive하게 제공할 수 있을 때만 additive 후보로 둔다.

```text
activeChildBeforeRunId
candidateChildRunId
activeChildAfterRunId
```

기존 `reconciliationClassification` 값은 다음을 유지한다
(`ReconciliationOutcome.java:113-139`).

```text
CANDIDATE_ALLOCATION_NO_ACTIVE_CHURN
SAME_TARGET_ACTIVE_CHILD_REPLACED
ACTIVE_CHILD_REPLACED
ACTIVE_CHILD_RETAINED
REPLACEMENT_BLOCKED
ACTIVE_CHILD_CHANGED_WITHOUT_REPLACEMENT
```

`activeBeforeInstanceId`, `candidateInstanceId`, `canInterruptResult`,
`activeAfterInstanceId`, `classification` 같은 alias를 추가하지 않는다. 이 event를 위해
`isEqual()`, `canBeInterrupted()` 또는 `isFinished()`를 다시 호출하지 않는다. Active
churn 판정에는 identity, semantic target와 `Task#tick()`이 이미 계산한 결과면 충분하며
기본적으로 deep world snapshot이 필요 없다.

### 9.4 Proposed `DESTROY_FINISH_EVALUATION`

이 event는 아직 존재하지 않는 bounded replacement projection이다. §9.1의 existing
emitter envelope를 재사용하고 다음 값만 additive payload로 검토한다.

```text
finishEvaluationId
evaluationOrigin=ACTIVE_TASK_LIFECYCLE|PARENT_DIAGNOSTIC_PROBE|UNKNOWN_CALLER
diagnosticTaskContext
callerTaskInstanceId
evaluatedTaskInstanceId
evaluatedTaskRunId
evaluatedTaskActive
evaluatedTaskStopped
callerSameAsEvaluatedTask
targetPosition
chunkLoaded
blockStateId
isAirResult
entryClientTickId
exitClientTickId
```

`blockStateId`와 `isAirResult`는 기존 `DestroyBlockTask#isFinished()`의 단일 read와
return result를 사용한다. 두 번째 `getBlockState()` 또는 `isFinished()` 호출을
추가하지 않는다.

`evaluationOrigin`은 object/run/task context로 직접 증명할 수 있을 때만 구체적으로
분류한다. Thread name, 문자열 또는 stack 추정만으로 active caller를 만들지 않는다.
불명확하면 `UNKNOWN_CALLER`를 유지한다. Hot-path stack walking과 reflection discovery를
추가하지 않는다.

### 9.5 `DESTROY_BLOCK_LIFETIME`

현재 dedicated event의 field name을 유지한다.

```text
phase=START|STOP
destroyTaskInstanceId
destroyTaskRunId
targetPosition
startedAtTick
stoppedAtTick
lifetimeTicks
interruptTaskClass
interruptTaskSemanticKey
forceCancelSource
```

실제 same-target churn은 같은 target에서 `START A -> STOP A -> START B`처럼 matching
run transition으로 나타나야 한다. Candidate allocation이 많아도 dedicated START가 한
번뿐이면 active-child churn이 아니다. Generic `VISIBLE_TASK_LIFECYCLE` count는 여기에
더하지 않는다.

### 9.6 Existing Baritone events

Existing Baritone event family는 event 이름과 현재 field를 바꾸지 않고 Destroy identity,
goal generation과 semantic progress를 연결한다. 필요한 observation은 다음과 같다.

```text
destroyTaskInstanceId
destroyTaskRunId
targetPosition
goalType / goalSummary / goal semantic target
customGoalActiveBefore / customGoalActiveAfter
baritoneGenerationId
currentExecutorIdentity / nextExecutorIdentity / inProgressIdentity
executorPathPosition / executorDestination
playerBlockPosition / playerDisplacementSinceLast
targetDistance / targetDistanceDelta
ticksSinceExecutorAdvance
ticksSincePlayerMovement
ticksSinceTargetDistanceImprovement
ticksSinceAnyProgress
cancelRequested
calcFailedLastTick
progressReason
progressClassification
```

Current `BARITONE_EXECUTOR_PROGRESS_SNAPSHOT` heartbeat는 100틱이며 기존 reason은
`NO_PROGRESS_HEARTBEAT_100_TICKS`, `EXECUTOR_POSITION_ADVANCED`, `PLAYER_MOVED`,
`TARGET_DISTANCE_IMPROVED`, `EXECUTOR_PROGRESS_STATE_CHANGED`다
(`BaritoneExecutorProgressState.java:8-18,73-113,197-213`). 이것은 current-source 사실이지
허용된 future contract가 아니다. Scoped bounded policy는 unchanged summary를 최대
200틱 또는 10초당 한 번으로 제한하므로, 다음 build/reproduction 전에 event 이름은
유지한 채 heartbeat cadence를 최소 200틱/10초로 harden해야 한다. Effective interval을
100으로 오표기하는 기존 reason도 그대로 둘 수 없으며, consumer compatibility를
확인해 truthful reason 또는 interval field로 이행한다. `SEMANTIC_STALL`은 stable
run/goal에서 no-progress가 200틱 이상이고 reset provenance까지 연결됐을 때만 additive
derived classification으로 검토한다. `isPathing=true` 자체를 progress로 분류하지 않으며
classification은 timeout, cancel, reset, blacklist, retry 또는 terminal을 발생시키지
않는다.

Baritone worker thread에서는 calculation schedule 시점의 immutable record만 사용한다.

```text
commandRequestId
commandCorrelationId
destroyTaskInstanceId
destroyTaskRunId
targetPosition
dimension
worldIdentity
baritoneGenerationId
requestedGoalSummary
```

Worker completion 시점에 global current command/task context를 다시 읽으면 다른
operation에 결과를 잘못 귀속할 수 있다. Calculation/adoption evidence에는 기존 field를
보존하면서 다음 의미가 빠진 경우에만 additive candidate를 검토한다.

```text
phase
resultType
pathPresent
pathIdentity
pathSource
pathDestination
pathLength
elapsedMillis
adoptionOutcome
normalCancelProvenance
goalChangeProvenance
```

`accepted`, calculation result, path identity와 adoption branch의 정확한 의미는 실제
runtime Baritone binary SHA-256과 일치하는 source JAR 또는 decompile 결과가 확인된
뒤에만 계약한다. 그 전에는 `BARITONE_BINARY_SOURCE_REQUIRED` 또는 `NOT_CAPTURED`로
둔다.

### 9.7 Existing `MOVEMENT_PROGRESS_CHECK_RESULT` reset delta

기존 payload의 `checkerOwner`, `checkerCallIndex`, `checkEvaluated`, `checkResult`,
`failureTransition`과 `resetReason`을 보존한다. P1 검증에 필요한 최소 additive field와
기존 field에 전달할 값은 행동 branch가 이미 아는 다음 local뿐이다.

```text
resetObservedBeforeCheck
resetReason=BARITONE_PATHING|OTHER_EXISTING_REASON|NOT_RESET|UNAVAILABLE
```

Parent reset branch와 child reset branch는 서로 다른 upstream method owner다. 각 owner가
기존 reset 직후 passive boolean/reason을 전달하는 최소 hunk를 별도 적용 단위로
검토한다. Logger는 `isPathing()`, `reset()` 또는 `check()`를 재호출하지 않는다.

### 9.8 Deferred `MINING_OPERATION_TERMINAL_SUMMARY`

이 event는 pre-implementation source snapshot에 없으며 operation owner와 close
boundary를 먼저 증명해야
한다. Behavior fix 전에는 정상 terminal뿐 아니라 기존 cancel, world unload 또는 session
close 경계에서 operation당 bounded summary 한 번이 필요하다. Cleanup 행동을 추가하거나
순서를 바꾸지 않고, 이미 존재하는 close boundary에 emission만 연결할 수 있을 때 다음
aggregate를 보존한다.

```text
startClientTickId
endClientTickId
durationTicks
terminalReason
candidateAllocationCount
actualReplacementCount
candidateAllocationNoiseCount
destroyStartCount
destroyStopCount
finishEvaluationCountByOrigin
finishTrueCountByOrigin
goalRequestCount
goalAcceptedCount
calculationGenerationCount
calculationOutcomeCounts
maxNoProgressTicks
scannerSelectionChangeCount
firstDivergenceBoundary
lastSuccessfulBoundary
budgetExhausted
suppressedCountsByEvent
unmatchedTaskLifetimes
coverageFlags
```

Owner가 증명되지 않으면 global mutable registry를 만들어 summary를 가장하지 않는다.
그 경우 behavior gate는 계속 닫힌 상태로 남는다.

## 10. Observation purity와 bounded logging 계약

### 10.1 Observation purity

Logger와 formatter는 다음 engine/lifecycle method를 다시 호출하지 않는다.

```text
Task.isFinished()
Task.isEqual()
canBeInterrupted()
WorldHelper.canBreak()
progressChecker.check()
progressChecker.reset()
forceCancel()
setGoalAndPath()
BlockScanner predicate
Carry On state mutation API
```

`safeValue()`로 재호출을 감싸는 것도 허용하지 않는다. Formatter가 exception을 문자열로
변환하더라도 observed engine call의 exception origin과 propagation을 오염시킬 수 있다.

### 10.2 Emission 조건

다음 경우만 출력한다.

```text
operation에서 최초 관측
semantic state change
invariant violation 또는 anomaly
unchanged progress summary는 최대 200틱 또는 10초당 한 번
terminal 또는 bounded abort summary
```

다음 값은 출력 조건이나 fingerprint로 사용하지 않는다.

```text
unchanged candidate allocation만 새 state로 만드는 ephemeral candidate identity
event sequence
game tick
timestamp
raw floating-point player position
suppression count
budgetRemaining
```

이 값들은 허가된 bounded payload에는 포함할 수 있다. Active child/run, executor, goal
owner identity의 실제 변화는 lifecycle transition을 구분하는 semantic 값일 수 있으므로
전역 금지하지 않는다. `replacementApplied=true`, dedicated START/STOP과 active run 변화는
같은 classification이 반복돼도 bounded detail 또는 aggregate count로 보존한다.

### 10.3 Gate 순서

```text
already-computed behavior result
-> cheap semantic fingerprint from locals and semantic identity
-> budget/dedupe permit
-> permitted event only: passive detail capture
-> bounded emit
```

Permit 이후라도 state-changing API를 호출할 수 없다.

### 10.4 Earlier implementation snapshot과 proposed budget 분리

현재 generic `MiningDiagnosticEventGate`에 구현된 값은 다음 세 가지뿐이다
(`MiningDiagnosticEventGate.java:10-12,48-68`).

```text
CURRENT_IMPLEMENTED
summary interval: 200 ticks
detail limit per bucket: 256
session hard cap: 5000
```

Pre-implementation source snapshot에는 operation cap, per-tick/sliding-window cap,
active-operation cap 또는
terminal reserve가 없다. 다음 값은 외부 검토를 보존한 향후 gate-hardening 제안이며
`PROPOSED_NOT_IMPLEMENTED`다. Generic mining의 현재 canonical 값처럼 인용하지 않는다.

```text
normal runtime: diagnostics OFF
explicit investigation: BOUNDARY
VERBOSE: explicit bounded reproduction only

PROPOSED_NOT_IMPLEMENTED
operation/correlation detail cap: 256
session hard cap: 5000
terminal/exception/cap reserve: at least 32; 64 proposed for Store-budget parity
session cap signal: exactly one DIAGNOSTIC_SESSION_CAP_REACHED from reserved budget
simultaneous diagnostic correlation buckets: 16
per correlation per client tick: 8 events
per correlation per 20-tick sliding window: 32 events
```

Proposed event-family partition:

| event family | 출력 조건 | proposed cap | 추가 제한 |
| --- | --- | ---: | --- |
| `MINE_TARGET_GOAL_REQUEST` | 최초, target/state/outcome 변경 | 16 | same-target summary 200틱 |
| proposed `DESTROY_FINISH_EVALUATION` | origin/result/state 변경 | 16 | target당 client tick당 최대 1 |
| `TASK_CHILD_RECONCILIATION` | classification 변경 또는 실제 replacement | 24 | unchanged summary 200틱 |
| `DESTROY_BLOCK_LIFETIME` | 실제 START/STOP | 32 | unmatched count는 terminal에 보존 |
| conditional proposed `TASK_CHAIN_TRANSITION` | chain identity 변경 | 8 | 정상 tick begin/end 출력 금지 |
| `BARITONE_GOAL_REQUEST_DECISION` | request/accepted 결과 변경 | 16 | same-goal 반복은 summary |
| `BARITONE_CALCULATION_*` | generation phase별 한 번 | 32 | 동일 결과 반복 generation은 aggregate |
| `BARITONE_EXECUTOR_PROGRESS_SNAPSHOT` | semantic 변화 또는 unchanged progress summary | 32 | state change는 즉시; unchanged heartbeat는 최소 200틱/10초 |
| conditional `BLOCK_SCANNER_FILTER_SUMMARY` | selected/no-result 전환 또는 anomaly | 8 | state change/anomaly는 즉시; unchanged summary만 최소 200틱/10초 |
| deferred `MINING_OPERATION_TERMINAL_SUMMARY` | operation/correlation close당 한 번 | reserve | 일반 detail cap으로 탈락 금지 |

Terminal, exception, cap status와 coverage gap은 일반 detail cap에 의해 유실되면 안
된다. Budget과 suppression은 gameplay state, scheduling, completion 또는 terminal
reason을 바꾸지 않는다.

## 11. Earlier diagnostics-only 적용 계획 (historical)

아래 순서는 구현 계획이며 이 문서가 적용을 승인하지 않는다. Upstream-derived 파일
여러 개를 한 번에 수정하지 않는다.

### D0. Source equivalence와 upstream observer-contamination stop gates

1. 외부 ZIP, pre-implementation working tree와 runtime JAR source equivalence가 필요하면 별도
   provenance audit로 확인한다.
2. `AbstractDoToClosestObjectTask#onTick()`의 diagnostic-only
   `goalTask.isFinished()` 호출 제거를 한 upstream minimal hunk로 검토한다.
3. `MineOrCollectTask#getGoalTask()`의 noisy `VISIBLE_TASK_RETURN` call-site는 existing
   `MINE_TARGET_GOAL_REQUEST`로 대체하거나 제거하는 별도 upstream hunk로 검토한다.
   Global `VisibleTaskDiagnostics` fingerprint 변경은 모든 caller 영향 audit 전에는 첫
   선택이 아니다.
4. `DestroyBlockTask#isFinished()`의 unbounded AIR text와 `isEqual()`의 unbounded result
   text는 서로 다른 exact hunk로 stop-gate한다. Dedicated bounded event가 같은 evidence를
   보존한 뒤에만 각 legacy text 제거를 제안한다.
5. `VISIBLE_TASK_LIFECYCLE`과 `DESTROY_BLOCK_LIFETIME` 중복은 dedicated lifetime을
   authoritative evidence로 먼저 고정한다. Generic event 삭제/변경은 모든 caller audit가
   필요한 별도 compatibility 결정이다.

위 upstream hunk들은 서로 같은 적용 단위로 묶지 않는다.

### D1. LAVI-owned observation purity와 gate hardening

1. 네 mining diagnostics 경로의 `WorldHelper.canBreak()` 재호출을 event family별로
   제거하거나 `NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION`으로 바꾼다.
2. `TaskChildReconciliationDiagnostics`와 `DestroyBlockPhaseDiagnostics`를 포함해
   unchanged-state 판정 전 deep snapshot을 만드는 hot path를 call-site별로 감사한다.
3. 다음 build/reproduction 전에 per-correlation detail cap 256, total session cap 5000,
   terminal/exception/suppression/cap reserve 최소 32를 구현한다. Session cap 도달 시
   session-wide exactly-once `DIAGNOSTIC_SESSION_CAP_REACHED`를 reserve에서 emit하고
   non-terminal detail만 suppress해야 한다. Cap claim은 per-bucket detail budget에
   종속되거나 그 budget을 먼저 소비하면 안 된다.
4. Current `MINING_DIAGNOSTIC_GATE_EXHAUSTED`와 canonical cap event의 consumer
   compatibility를 같은 LAVI-owned gate-hardening 단위에서 해결한다. 두 cap event를
   중복 emit하지 않으며, canonical exactly-once event와 reserve acceptance를 다음
   reproduction 뒤로 미루지 않는다.

### D2. Existing parent-return event hardening

기존 `MINE_TARGET_GOAL_REQUEST`를 재사용한다. 기존 local `targetState`를 observer로
전달하려면 `MineAndCollectTask.MineOrCollectTask#getGoalTask()`의 upstream call-site
hunk가 필요할 수
있다. 이 hunk는 D0의 다른 upstream hunk와 별도 승인 단위다.

Hunk 없이 passive 값을 전달할 수 없다면 진단 logger가 world/scanner/predicate를 다시
평가하지 않고 해당 field를 `NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION`으로 둔다.

### D3. Existing reconciliation event hardening

현재 `Task#tick()`은 필요한 equality/replacement 결과를 이미 LAVI logger로 전달한다.
따라서 첫 방향은 `Task.java`를 수정하지 않고 LAVI-owned
`TaskChildReconciliationDiagnostics`와 snapshot/emission 순서를 정리하는 것이다.

Cheap classification으로 permit을 얻기 전에는 deep snapshot을 만들지 않는다.
Candidate allocation noise와 actual replacement count를 분리한다.

### D4. Finish origin과 active lifecycle

먼저 현재 LAVI task context로 origin을 신뢰성 있게 판별할 수 있는지 읽기 전용 audit를
한다. 충분하지 않다면 `DestroyBlockTask#isFinished()`의 기존 single-read hunk에서
bounded finish evaluation을 전달하는 별도 upstream diagnostics-only divergence를
제안한다.

D4는 D0 또는 D2의 upstream hunk와 자동으로 묶지 않는다. `isFinished()`의 legacy
unbounded text 제거도 finish projection hunk에 자동으로 합치지 않는다.

### D5. Existing movement-progress reset provenance

기존 `MOVEMENT_PROGRESS_CHECK_RESULT`를 재사용한다. Parent
`MineAndCollectTask.MineOrCollectTask` reset branch와 child `DestroyBlockTask` reset
branch가 이미 알고
있는 `resetObservedBeforeCheck`와 `resetReason=BARITONE_PATHING`만 전달한다. 두 파일은
별도 upstream hunk이며 logger에서 checker를 재호출하거나 reset하지 않는다.

### D6. Existing Baritone event correlation과 semantic progress

기존 Mixin/event를 재사용하고 event 이름을 rename하지 않는다. Runtime Baritone binary
hash와 matching source/decompile을 먼저 대조한다. Schedule-time correlation,
calculation/adoption outcome과 executor semantic progress에서 실제로 빠진 필드만
LAVI-owned diagnostics 경계에 제안한다.

Baritone source, dependency, version, goal/path behavior를 변경하지 않는다.

### D7. Owner-gated terminal/abort summary

D2-D6이 같은 command/parent/Destroy correlation으로 연결되고 existing close owner가
증명된 뒤에만 §9.8 aggregate를 제안한다. Normal terminal이 없더라도 기존 cancel,
world unload 또는 session close에서 한 번 emit하되 cleanup behavior와 ordering은
바꾸지 않는다. Owner가 없으면 새 global registry를 만들지 않고 hard-evidence gate를
닫아 둔다.

### D8. Conditional BlockScanner filter summary

D0-D7의 sanitized reproduction 뒤에도 동일 correlation/world/tick/target에서 world-state
모순이 남을 때만 검토한다.

위치별 rejection event를 출력하지 않고 호출 단위 aggregate만 허용한다.

```text
requestedBlockId
trackedCount
blockMismatchCount
predicateRejectedCount
scannerUnreachableCount
eligibleCount
selectedTarget
selectedStateFromExistingRead
selectedHeuristic
selectionChanged
scanGenerationOrRevision
```

기존 short-circuit 순서와 호출 횟수를 그대로 보존해야 한다. Block state, predicate와
`isUnreachable()`을 diagnostics 때문에 추가 호출하지 않는다.

모든 향후 upstream diagnostics hunk는 적용 전에 다음을 기록한다.

```text
ownership=upstream-derived with existing LAVI diagnostic hunk
exact file, method and current line range
required investigation marker
pre-edit source SHA-256
one minimal diff hunk
return/order/lifecycle behavior preserved
rollback at exact hunk level
separate build/reproduction authorization status
```

모든 approved upstream-derived engine diagnostics hunk에는 다음 marker가 필요하다.

```java
//20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
```

이 investigation에서 처음 추가되는 LAVI-specific Java diagnostic block에는 다음 marker도
필요하다. 첫 marker 하나로 engine-divergence marker를 대신하지 않는다.

```java
//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
```

## 12. 결과 판정 decision tree

```text
[0] Observer sanitization이 적용된 bounded reproduction
    |
    +-- TASK_CHILD_RECONCILIATION
    |     |
    |     +-- CANDIDATE_ALLOCATION_NO_ACTIVE_CHURN
    |     |     -> candidate identity 반복은 원인 후보에서 제외
    |     |
    |     +-- SAME_TARGET_ACTIVE_CHILD_REPLACED
    |           -> matching START/STOP 및 forceCancel boundary 확인
    |           -> equality/reconciliation boundary로 조사 제한
    |
    +-- parent/chain identity 자체가 반복 전환
    |     -> conditional TASK_CHAIN_TRANSITION audit
    |     -> chain identity change만 cap 8로 관찰
    |     -> TaskRunner priority/chain ownership은 read-only second pass
    |     -> TaskRunner behavior 수정은 계속 금지
    |
    +-- DESTROY_FINISH_EVALUATION
    |     |
    |     +-- inactive + diagnostic origin
    |     |     -> 기존 finish 반복은 observer artifact
    |     |
    |     +-- active + isAir=false
    |     |     -> completion 문제가 아니므로 Baritone progress 조사
    |     |
    |     +-- active + isAir=true
    |           -> same correlation/tick/world/target의 parent state와 비교
    |
    +-- Existing Baritone events
          |
          +-- goal rejected / calculation absent
          |     -> goal/process ownership boundary
          |
          +-- NO_PATH / cancellation / exception
          |     -> calculation boundary
          |
          +-- path present but not adopted
          |     -> adoption/process-control boundary
          |
          +-- path adopted and semantic progress exists
          |     -> legitimate long-distance progress; 다른 boundary 조사
          |
          +-- stable run/goal + pathing=true
                + policy-compliant >=200-tick/10-second unchanged summary
                + >=200 ticks semantic progress zero
                -> checker reset reason까지 연결되면 semantic stall 증거
```

Carry On과 과거 NPE는 D0-D8에서 동일 correlation/tick/target mutation 또는 현재
generation loss가 연결될 때만 우선순위를 올린다.

## 13. Behavior-fix 진입 hard-evidence gate

다음 공통 조건을 먼저 충족해야 한다.

```text
observer-induced isFinished call absent
identity-based visible log explosion absent or semantically bounded
diagnostic-only WorldHelper.canBreak calls absent
unchanged hot-path summaries no more than once per 200 ticks or 10 seconds
session cap test proves exactly one DIAGNOSTIC_SESSION_CAP_REACHED
terminal/exception/suppression/cap reserve >= 32 inside the 5000 total cap
fresh source/build/deployed JAR identity recorded under separate authorization
same operation terminal or bounded abort summary available
budget exhaustion absent, or exact coverage gap recorded
first divergence linked by operation + world + dimension + target
```

그 뒤 다음 중 하나가 동일 correlation에서 확인돼야 한다.

### A. 실제 same-target child churn

```text
same target
replacementApplied=true
activeAfter=candidate
previousChildStopCalled=true
matching START/STOP repeated
```

### B. Active child world-state contradiction

```text
same operation/tick/world/target
parent existing targetState=birch_log
active Destroy finish blockState=air
```

### C. Baritone semantic stall

```text
stable active child/run
stable goal
pathing=true
executor position unchanged
player displacement zero
target distance not improved
duration >= 200 ticks
parent/child checker reset reason=BARITONE_PATHING
```

### D. Baritone calculation/adoption failure

```text
same target generation
goal accepted -> repeated NO_PATH

or

path present -> PATH_PRESENT_NOT_ADOPTED_OR_DISCARDED

with no normal cancel or goal-change explanation
```

### E. Scanner invariant violation

```text
same existing block-state read
blockMatches=false
selectedTarget=same position
```

Candidate allocation count, VISIBLE event count, `isPathing=true`,
`customGoalActive=true`, player-target Y difference, earlier Carry On pickup, earlier Baritone
NPE 또는 budget-exhausted downstream absence만으로 behavior fix를 제안하지 않는다.

## 14. Non-goals와 stop gates

이번 문서가 승인하거나 제안하지 않는 것:

```text
timeout, retry, cooldown or fallback
blacklist threshold or reset behavior change
progress checker algorithm/order/reset behavior change
Task selection, equality, completion or reconciliation behavior change
TaskRunner/UserTaskChain/global chain change
DestroyBlockTask.isFinished return change
Baritone goal/path/input/cancel behavior change
PlayerInteractionFixChain change
Carry On interaction/state/dependency/version change
BlockScanner first-pass instrumentation
per-position scanner rejection logs
new global operation/session registry
wire-protocol or command-result schema change
upstream class rename/move/split/repackage
broad refactor or formatting cleanup
```

다음 중 하나가 필요하면 구현하지 않고 범위가 넓어진 이유를 보고한다.

```text
multiple upstream lifecycle owners in one diagnostics application unit
TaskRunner modification
behavior-owning Task method result/order change
new exception catch around observed engine behavior
state-changing probe for a diagnostic field
Baritone binary semantics without matching source/decompile
BlockScanner instrumentation before finish origin and semantic progress are resolved
```

## 15. 향후 검증과 별도 승인 경계

이 문서는 build 또는 runtime reproduction을 승인하지 않는다.

향후 별도 승인된 호환성 검증 build는
[Fabric ChatClef 1.20.1 Build Verification](chatclef-fabric-build-verification.md)을 따르고
runtime root에서 다음 명령을 사용한다.

```powershell
.\gradlew.bat clean build --rerun-tasks
```

Build success만으로 runtime 증거를 주장하지 않는다. 별도 승인 아래 다음을 확인해야
한다.

```text
source HEAD and dirty/clean state
exact source hashes for approved hunks
clean forced build result
built JAR path, size and SHA-256
deployed JAR path, size and SHA-256
duplicate ChatClef JAR absence
active instance and loaded mod identity
diagnostics mode and budgets
Mixin/linkage failure absence
one bounded operation from start through terminal or abort summary
expected event counts and suppression/coverage report
```

Minecraft test world가 복사, 복원, 교체 또는 rename된 경우에만
[ChatClef / Baritone Cache Troubleshooting](chatclef-baritone-cache-troubleshooting.md)을
먼저 적용한다. 현재 로그만으로 stale Baritone cache를 원인으로 단정하지 않는다.

## 16. Pre-implementation source evidence locator와 fingerprint

### 16.1 Earlier ownership classification과 pre-implementation line range

아래 path는 모두 다음 absolute runtime root에 상대적이다.

```text
C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\
```

| Runtime-root-relative path | ownership classification | pre-implementation relevant line range | 직접 지지하는 사실 |
| --- | --- | --- | --- |
| `src/main/java/adris/altoclef/tasks/AbstractDoToClosestObjectTask.java` | upstream-derived engine with existing LAVI diagnostics hunk | 63-69 | diagnostic supplier가 `goalTask.isFinished()` 평가 |
| `src/main/java/lavi/minecraft/diagnostics/ChatClefDiagnostics.java` | LAVI-owned diagnostics facade | 160-164 | VERBOSE gate가 `logEvent()` 내부에 있음 |
| `src/main/java/lavi/minecraft/diagnostics/formatting/DiagnosticFormatterFacade.java` | LAVI-owned diagnostics formatter | 35-39 | OFF가 아니면 supplier evaluation 허용 |
| `src/main/java/adris/altoclef/tasks/resources/MineAndCollectTask.java` | upstream-derived engine with existing LAVI compatibility and diagnostics hunks | 319-359; 363-402 | parent checker reset/check와 per-invocation candidate creation |
| `src/main/java/adris/altoclef/tasksystem/Task.java` | upstream-derived lifecycle owner with existing LAVI diagnostics hunk | 68-135 | candidate-to-active reconciliation 결과의 실제 owner |
| `src/main/java/adris/altoclef/tasks/construction/DestroyBlockTask.java` | upstream-derived Task with existing LAVI diagnostics hunks | 230-243; 317-323; 598-610; 627-636; 647-680 | lifetime duplication, child reset, finish/equality result와 unbounded text |
| `src/main/java/lavi/minecraft/diagnostics/tasktrace/VisibleTaskDiagnostics.java` | LAVI-owned diagnostics helper | 17-18; 39-77 | return identity fingerprint와 task-scoped finished bucket |
| `src/main/java/adris/altoclef/util/helpers/WorldHelper.java` | upstream-derived baseline utility | 236-252 | `canBreak()`가 `interactionPaused`를 일시 변경 |
| `src/main/java/lavi/minecraft/diagnostics/mining/TaskChildReconciliationDiagnostics.java` | LAVI-owned diagnostics observer | 27-57 | 세 deep snapshot이 gate 전 생성 |
| `src/main/java/lavi/minecraft/diagnostics/mining/reconciliation/ReconciliationTaskSnapshot.java` | LAVI-owned diagnostics snapshot | 28-104 | world/scanner/`canBreak()` detail capture |
| `src/main/java/lavi/minecraft/diagnostics/mining/MineTargetGoalRequestDiagnostics.java` | LAVI-owned diagnostics observer | 32-79 | existing payload names와 repeated state/predicate/`canBreak()` reads |
| `src/main/java/lavi/minecraft/diagnostics/mining/DestroyNavigationDiagnostics.java` | LAVI-owned diagnostics observer | 46-56 | navigation detail의 repeated world/`canBreak()` reads |
| `src/main/java/lavi/minecraft/diagnostics/mining/MineTargetSelectionDiagnostics.java` | LAVI-owned diagnostics observer | 74-84 | selection detail의 repeated world/scanner/`canBreak()` reads |
| `src/main/java/lavi/minecraft/diagnostics/mining/MovementProgressDiagnostics.java` | LAVI-owned diagnostics observer | 39-55 | existing reset reason이 unavailable |
| `src/main/java/lavi/minecraft/diagnostics/mining/DestroyBlockPhaseDiagnostics.java` | LAVI-owned diagnostics observer | 20-61 | unchanged phase gate 전 progress/Baritone capture |
| `src/main/java/lavi/minecraft/diagnostics/mining/MiningDiagnosticEventGate.java` | LAVI-owned diagnostics gate | 10-12; 48-68 | current 200/256/5000 limits |
| `src/main/java/lavi/minecraft/diagnostics/DiagnosticEventEmitter.java` | LAVI-owned diagnostics emitter | 21-50; 57-82; 95-117 | canonical verbose/boundary envelope fields |
| `src/main/java/lavi/minecraft/diagnostics/mining/reconciliation/ReconciliationDiagnosticPayload.java` | LAVI-owned diagnostics payload | 45-126 | current reconciliation field names |
| `src/main/java/lavi/minecraft/diagnostics/mining/reconciliation/ReconciliationOutcome.java` | LAVI-owned diagnostics classifier | 31-86; 113-139 | candidate noise와 actual replacement classification |
| `src/main/java/adris/altoclef/mixins/diagnostics/PathingBehaviorDiagnosticMixin.java` | LAVI-owned diagnostic Mixin targeting Baritone `PathingBehavior` | 44-123; 126-179 | executor HEAD/RETURN, goal, calculation, adoption와 cancel observation call-sites |
| `src/main/java/lavi/minecraft/diagnostics/mining/baritone/executor/BaritoneExecutorProgressDiagnostics.java` | LAVI-owned Baritone diagnostics observer | 14-20; 25-109 | executor snapshots, state gate와 existing event emission |
| `src/main/java/lavi/minecraft/diagnostics/mining/baritone/executor/BaritoneExecutorProgressSnapshot.java` | LAVI-owned immutable diagnostics snapshot | 87-149; 226-249; 341-369; 461-479 | schedule/tick-time executor, player와 target progress values |
| `src/main/java/lavi/minecraft/diagnostics/mining/baritone/executor/BaritoneExecutorProgressState.java` | LAVI-owned diagnostics state | 8-18; 73-113; 197-213 | current 100-tick heartbeat와 progress reasons |
| `src/main/java/lavi/minecraft/diagnostics/mining/baritone/BaritonePathCalculationDiagnostics.java` | LAVI-owned Baritone diagnostics observer | 24-137; 140-296; 451-515; 520-606 | goal, calculation generation/result, adoption와 cancel evidence |

### 16.2 Selected pre-implementation source fingerprints

다음 SHA-256은 2026-08-30 earlier documentation pass에서 읽은 pre-implementation
dirty-working-tree bytes의 selected fingerprint다. 표는 근거 파일 중 핵심 subset이며
외부 ZIP 또는 local `HEAD` commit 전체의 hash가 아니다. Later diagnostics implementation
뒤 현재 source와 다시 같다는 뜻이 아니며, current locator나 current hash로 사용하지
않는다.

| Runtime-root-relative path | SHA-256 |
| --- | --- |
| `src/main/java/adris/altoclef/tasks/AbstractDoToClosestObjectTask.java` | `DD152EDDB320441FB16EF5DABF31176F237C910FA7965B32259CCAC0A205B97F` |
| `src/main/java/adris/altoclef/tasks/resources/MineAndCollectTask.java` | `8E876FE6950EA5153ABD3451568337E6B627D2DAAFA69CBD23003B267325DEAF` |
| `src/main/java/adris/altoclef/tasksystem/Task.java` | `F79466A5BDE8DDC3E40455CAD904505C0267CD4AB9D58D36A6AA3FFE679E12A5` |
| `src/main/java/adris/altoclef/tasks/construction/DestroyBlockTask.java` | `8BDCA1E1BA521A5301B3600B034D34D4C9018F1394F840C32CA1D1D67FF1F2B2` |
| `src/main/java/adris/altoclef/util/helpers/WorldHelper.java` | `5D74F23D28C8187650DEE099A2F221E04DB8510183A3D601DCF4B1B5AFE05D13` |
| `src/main/java/lavi/minecraft/diagnostics/ChatClefDiagnostics.java` | `74122F8C96779F5A8776C7BC25CD23CB0EEDA2888975510317897A1A791E827C` |
| `src/main/java/lavi/minecraft/diagnostics/tasktrace/VisibleTaskDiagnostics.java` | `E4A51D055D4FB326B979D1357F94793D8287D9083477BB8FC72E49CBE46ADECA` |
| `src/main/java/lavi/minecraft/diagnostics/mining/TaskChildReconciliationDiagnostics.java` | `38CE38E8228D4DBF9AF508677340B1BDC13CB28894498275E004D2EB24A64417` |
| `src/main/java/lavi/minecraft/diagnostics/mining/MiningDiagnosticEventGate.java` | `0D63951D33C69E9580447CFEF6F1E2A39E9C815AB2B2AC84F16A473FB9592F45` |
| `src/main/java/lavi/minecraft/diagnostics/formatting/DiagnosticFormatterFacade.java` | `AADA7C62E88F67628DCBD0F2EE47B48A98DB0F4B2B55635CC671961C08BBFA6D` |
| `src/main/java/lavi/minecraft/diagnostics/DiagnosticEventEmitter.java` | `939E0A380E369909ECA4685F7490A4F43AC10A5F42999061D3E5F600090A5F7A` |
| `src/main/java/lavi/minecraft/diagnostics/mining/MineTargetGoalRequestDiagnostics.java` | `0159D88FAA2566622940744F3E4DFA92812C6884DECFEDE3E2EA59708B6F0537` |
| `src/main/java/lavi/minecraft/diagnostics/mining/reconciliation/ReconciliationDiagnosticPayload.java` | `AC4EA3EB2261814E90754DFB0F0DD8CF8A6E93D698A41331A73F0F8BCFC5E979` |
| `src/main/java/lavi/minecraft/diagnostics/mining/reconciliation/ReconciliationOutcome.java` | `EBA57F4C8E3682051BD0C64CA3CD37EF32D6A215560753A957294BBC678DDF92` |
| `src/main/java/lavi/minecraft/diagnostics/mining/reconciliation/ReconciliationTaskSnapshot.java` | `54F9A1307B5A8CB98F481C8C2F9F890C008E7DB278FBF85E595D71C8BCBD65DF` |
| `src/main/java/lavi/minecraft/diagnostics/mining/MovementProgressDiagnostics.java` | `AC571F3EC3F7ABC947B690EC91FF0AA66A3216A8CAF9EB3990C9848B6BB742DD` |
| `src/main/java/lavi/minecraft/diagnostics/mining/DestroyBlockPhaseDiagnostics.java` | `F142C395C02069660E7AA38DD612BDE03F58E696E5925E36183B2484B9254757` |
| `src/main/java/lavi/minecraft/diagnostics/mining/DestroyBlockDiagnosticState.java` | `839F08CB6936184F5F3AA0FE2436352A353ACBF0F7386B06F939B3286D884902` |
| `src/main/java/lavi/minecraft/diagnostics/mining/baritone/executor/BaritoneExecutorProgressState.java` | `9BCFFE73A40F6522EC7F52D7B29C325738A796B4D4DE7CF18E17406A68FC116E` |
| `src/main/java/lavi/minecraft/diagnostics/mining/DestroyNavigationDiagnostics.java` | `7BA48426076AD3A67B15ACF92B3FDDE9E30C84236C9C1DCBC4AAE36CBE1BC2E5` |
| `src/main/java/lavi/minecraft/diagnostics/mining/MineTargetSelectionDiagnostics.java` | `87AE32A4FD7C76CDFB3F94CD0572B2BBCAAEAF3CCB80E4BA569E08175B89EE4B` |
| `src/main/java/adris/altoclef/mixins/diagnostics/PathingBehaviorDiagnosticMixin.java` | `9849158F05391CE8CC386FFC9DBBD4FB844C91AB130ECB09CCB7E7BFBD60A0D3` |
| `src/main/java/lavi/minecraft/diagnostics/mining/baritone/executor/BaritoneExecutorProgressDiagnostics.java` | `DBA6EEABF31EA4834B155E886528B0CB0E376A6606BCEEBA4D45C5E588B15F52` |
| `src/main/java/lavi/minecraft/diagnostics/mining/baritone/executor/BaritoneExecutorProgressSnapshot.java` | `CA5DD327225747C782A8F742F5190D733C53451D4FE266232F81856A44F00DC7` |
| `src/main/java/lavi/minecraft/diagnostics/mining/baritone/BaritonePathCalculationDiagnostics.java` | `315FF8178D76F1D2D3E34D580581C64622BC8DD3E446F70D4981D12F86706A9D` |

## 17. Earlier capture의 최종 판정

```text
INCIDENT_SCOPE: EARLIER_OBTAIN_CHEST_MINING_CAPTURE
earlier capture의 10만 회 이상 visible pattern:
  RUNTIME_LOG_CONFIRMED
  VISIBLE_TASK_FINISHED_CHECK=183335
  VISIBLE_TASK_RETURN=183351

pattern과 양립하는 source mechanisms:
  per-invocation candidate allocation
  + diagnostic isFinished evaluation 가능 경로
  + candidate identity/task-scoped bucket amplification
  CURRENT_WORKTREE_SOURCE_CONFIRMED

동일 mechanisms가 deployed JAR에서 runtime pattern을 실제 생성:
  RUNTIME_COMPATIBLE_INFERENCE

actual active-child churn:
  UNPROVEN

observer contamination mechanism in pre-implementation source snapshot:
  CURRENT_WORKTREE_SOURCE_CONFIRMED

observer contamination occurrence in inspected runtime:
  RUNTIME_COMPATIBLE_INFERENCE

strongest actual runtime hypothesis:
  stable active child/run
  + Baritone isPathing=true
  + zero semantic executor/player/target progress
  + parent/child checker reset

strongest actual runtime hypothesis status:
  INFERENCE

smallest safe next direction at that historical review boundary:
  source/JAR equivalence as needed
  -> observer sanitization
  -> LAVI-owned purity and gate hardening
  -> candidate versus active reconciliation
  -> finish evaluation origin
  -> checker-reset provenance + Baritone semantic progress

BlockScanner and Carry On:
  DEFERRED UNTIL SAME-CORRELATION EVIDENCE

behavior fix:
  PROHIBITED UNTIL HARD-EVIDENCE GATE
```

Earlier review 당시 root cause는 미확정이었다. 위 `smallest safe next direction`의
diagnostics work는 이후 source/runtime baseline에서 진행됐으므로 current backlog나
현재 source 상태로 읽지 않는다. 그 결과로 얻은 later reproduction 판정은 §18이
소유한다.

## 18. 23:26 KST 후속 재현이 mining 판정에 미치는 영향

### 18.1 Canonical incident ledger

후속 재현의 exact interaction/store chronology, correlation과 artifact identity는
[ChatClef Automatic Deposit Transfer / Movement / Carry On Diagnostics Review](chatclef-auto-deposit-transfer-movement-carryon-diagnostics-review-2026-08-30.md)의
`2026-08-30 23:26 KST 후속 재현: automatic-deposit chest pickup` section이 소유한다.
이 문서는 그 증거가 `OBTAIN_CHEST`·mining 가설의 우선순위를 어떻게 바꾸는지만
기록한다.

```text
reproduction id:
  auto-deposit-carryon-chest-20260830-232646

command correlation:
  lavi-a5c58863133143e590dfc83673d598b3

auto/store/interaction:
  auto-deposit-1
  store-deposit-1831
  interactionId=1852
```

23:26 reproduction의 live `latest.log`도 documentation cut-off 뒤 회전됐다. 아래 §18
line reference는 다음 archive를 UTF-8로 decompress했을 때의 logical line number다.

```text
later rotated archive:
  C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\2026-08-30-3.log.gz
compressed bytes:
  231,308
compressed SHA-256:
  8D6ECD3C88E3687676EDC9FBDB91FAA180A72109F5F18138B8DEBE72984A20F1
uncompressed bytes:
  5,748,673
uncompressed SHA-256:
  33A28794CC7372FCC1C48A95AFCA634C3A21D5998D55F8E386C48213F9AF0A8D
uncompressed logical lines:
  7,086
documentation cut-off prefix:
  decompressed line 5720
  4,966,473 bytes
  SHA-256 1F59EC5EAE0EC502EF04EB9ADEE771CE75B8BF8C8BBCE258086A0E935FDA7EA1
```

Later reproduction은 canonical incident ledger에 기록된 deployed JAR
`D9D680CC7FF261165547D20B2D2AC87B3983C8EE5F98F5C7402BA077AAE5202F`
(7,348,369 bytes)를 사용했다. 이는 §5.1의 earlier JAR
`F73ADE284AD1FE43D49639B5A3E0FA05342E74BBFC6F0109F8855483F2DE2B4C`
(7,262,957 bytes)와 다른 runtime baseline이며, §§5.2/16의 pre-implementation
working-tree source와 byte equivalence를 뜻하지 않는다.

2026-08-31 read-only source-baseline audit는 §16의 fingerprint 25개 중 7개만 현재
path bytes와 일치하고, 16개는 hash가 달라졌으며 2개는 기존 path가 없어졌음을
확인했다. 또한 §5.2가 기록한 diagnostic `goalTask.isFinished()` 추가 평가 경로는
현재 `AbstractDoToClosestObjectTask` source에 남아 있지 않다. 이는 §§1-17을 later
source 상태로 갱신하라는 뜻이 아니라, 그 locator와 observer-contamination mechanism을
pre-implementation historical evidence로만 읽어야 한다는 baseline-transition
증거다. Current source와 deployed JAR의 byte equivalence는 이 audit에서 주장하지
않는다.

### 18.2 Earlier claim과 later-correlation 판정의 분리

§6의 다음 historical statement는 earlier capture에 그대로 남는다. 다른 correlation인
later reproduction에 같은 판정을 전이하지 않으며, 그 analogous outer route는 아래
later archive evidence로 별도 판정한다.

```text
Carry On pickup directly caused the current mining loop: UNPROVEN
```

후속 로그는 다음 연결을 직접 보였다.

```text
placement route 직후 같은 위치에서 관찰된 chest candidate가 OPEN_EXISTING으로 선택됨
-> interactionId=1852 returned SUCCESS without GUI
-> +1 tick NOT_CARRYING -> CARRYING
-> clicked chest position became air
-> selected container invalidated as UNSUPPORTED_CONTAINER
-> OPEN_EXISTING -> OBTAIN_CHEST
-> active child StoreInContainerTask -> CraftInTableTask
-> CollectPlanksTask -> MineAndCollectTask -> DestroyBlockTask
-> dirt stored 0/22 and obtain-chest branch remained stable
```

따라서 Carry On pickup은 `OBTAIN_CHEST`와 그 descendant mining에 진입한 직접 선행
경계로 `STRONG_TEMPORAL_ATTRIBUTION`된다. 반면 다음 더 깊은 주장은 여전히
`UNPROVEN`이다.

```text
Carry On이 Baritone의 calculation/adoption/executor 내부 stall 자체를 만들었다
later DestroyBlock active child가 same-target candidate로 반복 교체됐다
pathing/reset semantics가 player 무이동의 단일 최종 원인이다
stale Baritone world cache가 이 secondary stall의 원인이다
```

즉 §7의 historical hypothesis를 이 later reproduction에 적용할 때는 다음 범위로만
재분류한다.

```text
P0 observer contamination:
  earlier diagnostic-reliability defect; primary Carry On cause가 아님
P1/P2/P4:
  observed outer route-divergence 설명에서 제외;
  secondary resource-acquisition stall 후보로만 유지
P6 Carry On outer entry boundary:
  STRONG_TEMPORAL_ATTRIBUTION
P6 exact activation trigger / deep Baritone mutation owner:
  UNPROVEN
P3/P5/P7:
  later evidence로 승격되지 않음
```

Primary outer attribution은 secondary mining stall의 내부 root cause를 증명하지 않는다.

### 18.3 Candidate allocation 판정의 제한

후속 로그의 pre-cap `TASK_CHILD_RECONCILIATION`은 다음을 구분했다.

```text
tick 697 and 725:
  REUSED_ACTIVE_CHILD_CANDIDATE_EQUAL
  CANDIDATE_ALLOCATION_NO_ACTIVE_CHURN

tick 724 and 877:
  different target replacement

tick 873:
  different target candidate blocked while previous child was not interruptible

observed SAME_TARGET_ACTIVE_CHILD_REPLACED:
  none
```

이 evidence는 candidate object 생성만으로 active churn을 단정하던 해석을 기각한다.
그러나 later rotated archive의 decompressed line 1414에서 correlation mining-detail
cap `256/256`이 automatic deposit 시작 전에 소진됐다. 따라서 later chest-crafting
descendant의 reconciliation 전 구간을 같은 로그로 관찰한 것은 아니다. Secondary
mining churn을 전역적으로 `ABSENT`라고 확대하지 않는다.

### 18.4 Secondary stall evidence와 coverage gap

후속 재현에서 확인된 secondary 상태:

```text
later archive decompressed line 1740, tick 1010:
  DestroyBlockTask target=-1034,79,-144
  pathing active and move-checker reset observation

later archive decompressed line 1768, tick 1241:
  destroy_block_progress_check_failed
  player position fixed near -1032.4976/9.0/-147.7525
  baritonePathing=false
  customGoalActive=true

later archive decompressed line 5713, tick 30399:
  branch=return_obtain_chest_item
  stored dirt=0/22
  noProgressTicks=29400
  same player position
```

이것은 prolonged no-progress를 증명하지만 Baritone 내부의 first failing method 또는
goal/calculation/adoption owner까지 증명하지 않는다. 같은 command correlation의
상세 event가 cap 이후 suppressed됐으므로, 이 section은 secondary cause를 만들어내지
않는다.

Minecraft test world가 복사·복원·교체·rename됐다는 별도 사실이 있을 때는 계속
[ChatClef / Baritone Cache Troubleshooting](chatclef-baritone-cache-troubleshooting.md)을
먼저 적용한다. 그런 provenance와 cache reset 비교 없이 stale cache를 이번 원인으로
승격하지 않는다.

### 18.5 화로 처리에서 참고한 원칙

과거 화로 pickup incident의 bounded post-place handoff는 다음 설계 원칙을 제공한다.

```text
placement child cleanup을 먼저 완료
-> input stability를 bounded하게 관찰
-> 기존 normal open flow 유지
-> click SUCCESS가 아니라 실제 ScreenHandler open으로 성공 판정
-> global input release, retry 또는 Baritone cancel 금지
```

현재 automatic deposit은 다음 별도 route를 사용하며 `DoStuffInContainerTask`의 화로
handoff를 통과하지 않는다.

```text
DepositAllTask
-> PlaceBlockNearbyTask
-> StoreInContainerTask
-> AbstractDoToStorageContainerTask
-> InteractWithBlockTask
```

또한 current click에서는 `sneakHeld`, raw sneak key와 player sneaking이 모두 false였다.
따라서 화로 코드를 복사하거나 residual SNEAK을 현재 exact cause로 선언하지 않는다.
참고 대상은 lifecycle ownership과 actual-GUI postcondition 원칙이다. Bounded handoff와
기존 ScreenHandler predicate의 ownership 차이는
[ChatClef Engine Divergence Record](chatclef-engine-divergence-record.md)에 기록돼 있다.

### 18.6 Diagnostics와 behavior gate의 후속 판정

```text
INCIDENT_SCOPE: AUTO_DEPOSIT_CARRYON_CHEST_20260830_232646
PRIMARY_OBSERVABLE_OUTER_FAILURE_BOUNDARY:
  ESTABLISHED

PRIMARY_CAUSAL_ATTRIBUTION:
  CARRY_ON_PICKUP_STRONGLY_ATTRIBUTED

PRIMARY_ENTRY_INTO_OBTAIN_CHEST:
  RUNTIME_LOG_CONFIRMED

EXACT_CARRY_ON_ACTIVATION_OWNER_AND_METHOD:
  UNPROVEN

FINAL_ROOT_CAUSE:
  UNPROVEN

PRIMARY_ADDITIONAL_BROAD_RUNTIME_DIAGNOSTICS:
  NOT_REQUIRED_FOR_THE_OBSERVED_OUTER_BOUNDARY

PRE_CHANGE_EVIDENCE_OWNERSHIP_REPORT:
  REQUIRES_SEPARATE_AUTHORIZATION

ROOT_CAUSE_PATCH_PROPOSAL_GATE:
  NOT_YET_SATISFIED

SECONDARY_BARITONE_STALL_ROOT_CAUSE:
  UNPROVEN

SECONDARY_DIAGNOSTICS:
  DEFER_UNTIL_AFTER_SEPARATELY_APPROVED_PRIMARY_PATCH_APPLICATION_AND_REPRODUCTION
  ADD_ONLY_IF_THE_SAME_STALL_REMAINS

CURRENT_DOCUMENT_ACTION:
  DOCUMENTATION ONLY

BEHAVIOR_FIX_APPLICATION:
  NOT_AUTHORIZED_BY_THIS_DOCUMENTATION_TASK
```

따라서 기존 D0-D8 diagnostics backlog 전체를 primary Carry On 원인을 다시 찾기 위한
선행 조건으로 실행하지 않는다. 이것은 exact Carry On activation trigger, typed
interaction/store binding 또는 carried-block identity가 입증됐다는 뜻이 아니며,
§§10-15의 observer-purity·boundedness gate나 별도 pre-change evidence/ownership
report를 면제하지 않는다. 그 report는 automatic deposit이 소유한 가장 좁은
placement/open 경계, expected GUI/ScreenHandler success predicate, optional Carry On
state와 retry/input/goal-path/cleanup ownership을 먼저 증명해야 한다. 그 gate 전에는
root-cause patch proposal도 승인된 것으로 해석하지 않는다. `InteractWithBlockTask`,
`PlayerInteractionFixChain`, TaskRunner, global input ownership과 Baritone path/goal은
이 문서 증거로 제안하거나 수정하지 않는다.

Read-only source/ownership audit로 required evidence를 채울 수 없을 때는 별도 승인된
smallest targeted diagnostics가 필요할 수 있다. `broad diagnostics 불필요`를 `모든
추가 관찰 불필요`로 확대하지 않는다.
