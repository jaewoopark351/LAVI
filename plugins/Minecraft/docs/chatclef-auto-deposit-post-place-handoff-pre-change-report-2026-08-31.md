<!-- 20260831_openai: Recorded the evidence, ownership, containment, regression, and rollback gate for the automatic-deposit-only post-placement handoff. -->
<!-- 20260831_openai: Linked the post-checkpoint diagnostics, test-only container-type, and release-validation work separation without changing the captured handoff evidence. -->

# ChatClef Automatic Deposit Post-Place Handoff Pre-Change Report

Date: 2026-08-31

## 1. Decision and authorization boundary

The evidence is sufficient for one narrow behavior change:

```text
actual automatic-deposit PlaceBlockNearbyTask finishes
-> DepositAllTask returns null once
-> the existing Task scheduler stops and clears that actual child
-> the existing PlaceBlockNearbyTask cleanup releases its owned SNEAK input
-> Carry On receives one END_CLIENT_TICK reconciliation opportunity
-> the normal DepositAllTask scan/open path resumes on the next client tick
```

This is an automatic-general-deposit-only handoff barrier. It is not a generic
Carry On fix, a new interaction success rule, a retry, a timeout, or a Baritone
recovery mechanism.

The user's later implementation request authorizes the source, test, and
documentation changes listed in this report. It does not authorize any of the
following:

```text
Gradle build or test execution
JAR deployment
Minecraft launch or runtime reproduction
dependency, version, config, or wire-protocol change
commit or push
reset, clean, stash, checkout, restore, or broad rollback
```

Status at the pre-change boundary:

```text
NARROW_AUTO_GENERAL_POST_PLACE_HANDOFF_GATE: SATISFIED
BROADER_CARRY_ON_ENGINE_FIX_GATE: NOT SATISFIED
GENERIC_INTERACTION_CHANGE_GATE: NOT SATISFIED
SECONDARY_BARITONE_STALL_FIX_GATE: NOT SATISFIED
BUILD_AND_RUNTIME_VERIFICATION: NOT AUTHORIZED / NOT RUN
```

This block is the historical pre-change status, not the current verification
status. See [Section 16](#16-post-implementation-verification-update) for the
subsequent authorized build, deployment, and Minecraft evidence.

This report advances only the narrow handoff gate. It does not rewrite the
historical status blocks in the canonical incident review.

## 2. Inspection baseline

```text
repository:
  C:\Vtuber_Souorce_Code\LAVI

repository HEAD before the patch:
  14ba9b443f0bc11d6860a25d7fd3b8b916d95a04

worktree:
  DIRTY - existing user source and document changes must be preserved

DepositAllTask.java pre-change SHA-256:
  26A7E7B1FD2470DBAE5454CE9444DC8DD56AAF34CA3F0E213501CE198F9F15AA

AutoDepositMaintenanceTask.java pre-change SHA-256:
  1E7EB814A8C91F84E40A768D370ED44B56A79A0EA46A668923EC23DEAFEBDBE9
```

The hashes identify the dirty pre-change files inspected for this report. They
are not clean-Git blob hashes and must not be used to discard the user's existing
diagnostics work.

## 3. Installed Carry On and input configuration evidence

The active test instance was inspected read-only.

```text
instance:
  C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01

installed Carry On JAR:
  C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\carryon-fabric-1.20.1-2.1.2.7.jar

bytes:
  443,033

SHA-256:
  8D25FB164FC4CC15CD9123A915C880C55527EFE3FEC26B2C7FEBF09E65C6EBDD

options file:
  C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\options.txt

options line 123:
  key_key.carry.desc:key.keyboard.unknown
```

The options entry directly proves that the Carry On key is unbound in this
instance. It does not by itself prove the server-side `keyPressed` value at the
failed click.

## 4. Installed-JAR bytecode evidence

JDK 17 `javap -c -p` was used read-only against the installed JAR identified
above. This is artifact evidence for the exact installed version, not a guess
from another Carry On release.

### 4.1 Client reconciliation timing

The installed bytecode directly shows:

1. `tschipp.carryon.events.ClientEvents.registerEvents()` registers its callback
   on Fabric `ClientTickEvents.END_CLIENT_TICK`.
2. That callback invokes
   `tschipp.carryon.CarryOnCommonClient.checkForKeybinds()`.
3. In the unbound-key branch, `checkForKeybinds()` derives the requested state
   from the player's sneak state.
4. When the derived state becomes false while Carry On's client carry-key state
   is true, it calls
   `tschipp.carryon.client.keybinds.CarryOnKeybinds.onCarryKey(false)` and mirrors
   `CarryOnData.setKeyPressed(false)` locally.
5. `CarryOnKeybinds.onCarryKey(boolean)` creates a
   `tschipp.carryon.networking.serverbound.ServerboundCarryKeyPressedPacket` and
   sends it to the server.
6. `ServerboundCarryKeyPressedPacket.handle(player)` writes that boolean to the
   server-side `CarryOnData.setKeyPressed(boolean)` state.

Therefore the installed mod has an exact false-state packet path, and that path
runs at `END_CLIENT_TICK`, after ChatClef's task work described below.

### 4.2 Pickup necessary condition

The installed bytecode also directly shows:

1. `tschipp.carryon.events.CommonEvents.registerEvents()` registers a Fabric
   `UseBlockCallback`.
2. Its server-side block-use branch calls
   `tschipp.carryon.common.carry.PickupHandler.tryPickUpBlock(...)`.
3. `tryPickUpBlock(...)` first calls `PickupHandler.canCarryGeneral(...)`.
4. `canCarryGeneral(...)` returns false when
   `CarryOnData.isKeyPressed()` is false.

Thus a successful Carry On block pickup through this callback necessarily had
`keyPressed=true` at that server-side eligibility check. The runtime log did not
print that boolean directly; the value is a required-condition deduction from
the installed bytecode plus the observed pickup result.

## 5. ChatClef lifecycle ordering evidence

Current source inspection directly establishes this order:

```text
MinecraftClient.tick HEAD
-> ClientTickMixin publishes ClientTickEvent
-> AltoClef.onClientTick()
-> taskRunner.tick()
-> later, Fabric ClientTickEvents.END_CLIENT_TICK callbacks
-> CarryOnCommonClient.checkForKeybinds()
```

`Task.tick()` has two relevant existing contracts:

- If a non-equal new child is accepted, it calls `sub.stop(newSub)`, installs the
  new child, and ticks the new child in the same parent call.
- If the parent returns `null`, it calls `sub.stop()`, clears `sub`, and does not
  tick a replacement child in that parent call.

`PlaceBlockNearbyTask.onStop(...)` already calls `stopPlacing()`. That method
already releases only `Input.SNEAK` acquired by the placement task and gives up
the existing builder process through its established `onLostControl()` cleanup.
The proposed patch does not add another input release or another Baritone
cancellation.

This proves the exact containment seam: a one-time `null` result gives the
existing placement child a cleanup-only tick boundary before any new store/open
child can be ticked.

## 6. Runtime evidence and evidence grades

Canonical evidence artifact:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\2026-08-30-3.log.gz
compressed SHA-256:
  8D6ECD3C88E3687676EDC9FBDB91FAA180A72109F5F18138B8DEBE72984A20F1
uncompressed SHA-256:
  33A28794CC7372FCC1C48A95AFCA634C3A21D5998D55F8E386C48213F9AF0A8D
reproduction id:
  auto-deposit-carryon-chest-20260830-232646
```

Relevant correlation:

```text
traceId: trace-1
command correlation: lavi-a5c58863133143e590dfc83673d598b3
autoOperationId: auto-deposit-1
maintenance generation: auto-deposit-1-maintenance-1
automatic child: auto-deposit-1-child-1
pressure-owned run: auto-deposit-1-pressure-run-1
store operation: store-deposit-1831
store task identity: c2f9673
interactionId: 1852
```

### 6.1 Direct runtime observations

| Archive lines | Tick | Directly logged fact |
| ---: | ---: | --- |
| 1552-1556 | 988 | The automatic child started `DepositAllTask`; its branch returned `PlaceBlockNearbyTask` for a chest. |
| 1671 | 998 | The newly available chest position `-1033,9,-149` was selected as `OPEN_EXISTING`. |
| 1681-1682 | 998 | The actual child changed from `PlaceBlockNearbyTask` to `StoreInContainerTask`, and the previous child stop was recorded. |
| 1683 | 998 | The store path immediately proceeded toward `InteractWithBlockTask` in the same tick. |
| 1685-1687 | 998 | Interaction `1852` returned `SUCCESS`, but no container GUI opened; local sneak observations were false and Carry On was not yet carrying. |
| 1689 | 999 | Carry On was observed as carrying on the next tick. |
| 1694-1704 | 999 | The clicked target became unsupported/air, the route changed `OPEN_EXISTING -> OBTAIN_CHEST`, and the store child entered chest acquisition. |
| 1721 | 999 | The interaction outcome window recorded `NOT_CARRYING -> CARRYING`, `chest -> air`, and `targetRemoved=true`, with no GUI transition. |

The log directly proves same-tick placement-child replacement and opening, the
next-tick carry transition and target removal, and the resulting deposit route
divergence. It does not directly print the Carry On key packet or server-side
`keyPressed` field.

### 6.2 Deductions constrained by direct evidence

The following are deductions, not log fields:

1. Because installed `PickupHandler.canCarryGeneral()` rejects
   `keyPressed=false`, the observed Carry On pickup required server-side
   `keyPressed=true` at the block-use callback.
2. Because ChatClef opened the chest during the `MinecraftClient.tick` HEAD task
   path and Carry On reconciles the unbound key only at `END_CLIENT_TICK`, that
   open attempt ran before Carry On's false-state reconciliation opportunity for
   that tick.
3. Returning `null` once after the actual placement child finishes prevents a
   replacement store/open child from being ticked in the same parent call and
   lets the already-existing placement cleanup precede the Carry On end-tick
   reconciliation.

The report does not claim that the failed reproduction directly logged a false
packet being sent or processed. The patch creates the missing ordering boundary;
a separately authorized runtime reproduction must verify the packet-era outcome.

Still unavailable or intentionally unclaimed:

```text
exact carried block API identity or NBT
typed direct binding between interactionId=1852 and store-deposit-1831
taskRunId / parentTaskRunId for the automatic child
wire-level timestamp of the true or false Carry On packet
the independent inner cause of the later Baritone obtain-chest stall
```

These gaps block broader fixes, but none is required to contain the verified
automatic post-placement ordering hazard.

## 7. Last successful and first failing boundaries

```text
Verified symptom:
  automatic deposit places/chooses a chest, immediately opens it, Carry On picks
  it up instead of a GUI opening, and the deposit route falls into OBTAIN_CHEST

Last successful boundary:
  the actual PlaceBlockNearbyTask completed the placement route and the new chest
  became an OPEN_EXISTING candidate

First failing lifecycle boundary:
  at game tick 998, DepositAllTask allowed PlaceBlockNearbyTask ->
  StoreInContainerTask -> InteractWithBlockTask to proceed in the same client-tick
  task call before Carry On's END_CLIENT_TICK key-state reconciliation

Observed incorrect result:
  interaction SUCCESS without GUI; at tick 999 the target was removed and Carry
  On changed NOT_CARRYING -> CARRYING
```

The patch boundary is lifecycle ordering, not the generic meaning of
`interactResult=SUCCESS` and not Carry On's internal success contract.

## 8. Required design report

### Engine behavior being preserved

- Existing `Task.tick()` replacement, interruption, and `null`-child semantics
  remain unchanged.
- Existing `PlaceBlockNearbyTask.isFinished()`, `onStop()`, SNEAK release, and
  builder cleanup remain unchanged.
- Existing `StoreInContainerTask`, `AbstractDoToStorageContainerTask`,
  `InteractWithBlockTask`, and actual ScreenHandler success contracts remain
  unchanged.
- Existing fallback to obtain a chest remains unchanged after the handoff; it is
  not invoked by the barrier itself.

### LAVI-owned orchestration boundary

`AutoDepositMaintenanceTask` creates automatic general deposit children through
`AutoDepositGeneralTaskFactory#create`. That factory is the only point that
injects a retaining placement owner and an enabled post-placement handoff into
`DepositAllTask`.

Existing public `DepositAllTask` constructors keep an ephemeral placement owner
and a disabled handoff. Manual `@deposit_all` therefore does not enter the new
barrier.

### Optional Carry On boundary

The new code has no Carry On import, reflection, version check, state reader,
packet call, retry, timeout, or success criterion. Carry On remains optional.
When Carry On is absent, automatic general deposit still performs one cleanup
boundary after placing a container and then resumes the existing open flow.

### Diagnostic observation boundary

The evidence comes from existing structured lifecycle, store, interaction, and
Carry On observation plus read-only installed-JAR analysis. The patch does not
add a diagnostic observer and does not turn diagnostics into recovery control.

### Owning parent and child Tasks

```text
maintenance orchestration owner:
  AutoDepositMaintenanceTask

automatic general deposit owner:
  DepositAllTask created by AutoDepositGeneralTaskFactory#create

placement child:
  the retained, actual PlaceBlockNearbyTask instance returned to Task.tick

post-barrier open path:
  existing StoreInContainerTask -> AbstractDoToStorageContainerTask ->
  InteractWithBlockTask
```

Fresh equal placement candidates must not be mistaken for the active child.
`DepositAllPlacementTaskOwner` retains the exact automatic placement candidate
that it supplied until the one-time barrier consumes and clears it.

### Completion owner and predicate

- `PlaceBlockNearbyTask` continues to own its completion predicate.
- `DepositAllTask#deferAfterCompletedPlacement()` checks the retained actual placement
  child once and passes the already obtained result to
  `DepositAllPostPlaceHandoff.shouldDefer(...)`.
- The handoff owns only whether the parent returns `null` for that one completed
  placement transition. It does not own deposit completion, GUI success, Carry
  On success, or maintenance completion.

### Input owner and release path

- `PlaceBlockNearbyTask` remains the SNEAK acquisition owner.
- `Task.tick()` remains the interruption/stop caller.
- `PlaceBlockNearbyTask.onStop()` remains the release owner.
- The handoff never calls an input API.

### Retry owner and terminal reason

No retry is added. Existing task owners keep their existing retry and terminal
behavior. The handoff is consumed once and is not a retry counter, timeout, or
terminal classifier.

### Custom goal or path owner and cleanup path

No custom goal or path is created. No global path cancellation is added. The
existing placement child keeps its existing builder ownership and
`onLostControl()` cleanup.

### Carry On absence fallback

No capability is required to construct or execute the task. The same automatic
route resumes on the next tick whether Carry On is absent, present, incompatible,
or unreadable. No unavailable Carry On state is converted into success.

### Carry state observation contract

Existing diagnostics may continue to classify `NOT_CARRYING -> CARRYING` as a
pickup transition. The handoff does not read or mutate carry state and does not
use carry state as its completion predicate.

### Why composition is safer than inheritance

The state is local to one `DepositAllTask` operation and consists of two narrow
collaborators: actual placement identity ownership and one-shot deferral. No
subclass of `AltoClef`, `TaskRunner`, `PlayerInteractionFixChain`, or another
engine-wide lifecycle owner is needed. Composition avoids a parallel engine,
global hook, Carry On dependency, and inheritance-owned cleanup ambiguity.

## 9. Exact proposed files and hunks

### Existing production files to modify

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/
  adris/altoclef/tasks/container/DepositAllTask.java
  lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceTask.java
```

`DepositAllTask.java` receives only these behavior-relevant changes:

1. Add the public overload
   `DepositAllTask(boolean, DepositAllPlacementTaskOwner,
   DepositAllPostPlaceHandoff, ItemTarget...)`.
2. Reject mismatched ownership/handoff pairs so the injectable seam permits only
   ephemeral/disabled or retaining/enabled composition.
3. Keep all existing constructors behavior-compatible by supplying an ephemeral
   owner and disabled handoff.
4. In `onTick()`, use `deferAfterCompletedPlacement()` to evaluate the retained actual
   placement child once; when `shouldDefer(...)` returns true, clear the owner and
   return `null` once.
5. In the existing placement fallback, obtain the placement child through
   `DepositAllPlacementTaskOwner#getOrCreate(...)`.

No other `DepositAllTask` branch is intentionally changed.

`AutoDepositMaintenanceTask.java` remains the phase orchestrator and delegates
construction, manifest/recovery, free-slot verification, and diagnostic details
to the focused LAVI-owned collaborators below. Its existing public/package test
seams and phase semantics must remain compatible.

### New LAVI-owned production files

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/
  handoff/DepositAllPlacementTaskOwner.java
  handoff/DepositAllPostPlaceHandoff.java
  auto/maintenance/child/AutoDepositGeneralTaskFactory.java
  auto/maintenance/child/AutoDepositTrustedTaskFactory.java
  auto/recovery/AutoDepositDestinationManifestLifecycle.java
  auto/recovery/AutoDepositWorkingSetRecovery.java
  auto/maintenance/relief/AutoDepositFreeSlotVerdict.java
  auto/maintenance/relief/AutoDepositFreeSlotVerifier.java
  auto/maintenance/diagnostics/AutoDepositMaintenanceDiagnostics.java
```

Responsibility boundaries:

| Type | Single owned responsibility |
| --- | --- |
| `DepositAllPlacementTaskOwner` | Own stable or ephemeral placement-task identity policy. |
| `DepositAllPostPlaceHandoff` | Own the enabled/disabled one-shot deferral state. |
| `AutoDepositGeneralTaskFactory` | Construct automatic general `DepositAllTask` children with the enabled handoff. |
| `AutoDepositTrustedTaskFactory` | Construct trusted automatic storage children without general-route policy. |
| `AutoDepositDestinationManifestLifecycle` | Own destination manifest tracker start/stop lifecycle. |
| `AutoDepositWorkingSetRecovery` | Own working-set snapshot/deficit recovery composition. |
| `AutoDepositFreeSlotVerdict` | Represent the immutable free-slot verification result. |
| `AutoDepositFreeSlotVerifier` | Evaluate free-slot relief without owning phase transitions. |
| `AutoDepositMaintenanceDiagnostics` | Own bounded maintenance diagnostic registration/transition/terminal emission only. |

### Exact planned test files

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/
  lavi/minecraft/task/container/deposit/handoff/DepositAllPlacementTaskOwnerTest.java
  lavi/minecraft/task/container/deposit/handoff/DepositAllPostPlaceHandoffTest.java
  lavi/minecraft/task/container/deposit/handoff/DepositAllPostPlaceHandoffLifecycleTest.java
  lavi/minecraft/task/container/deposit/auto/maintenance/child/AutoDepositGeneralTaskFactoryTest.java
  lavi/minecraft/task/container/deposit/auto/maintenance/relief/AutoDepositFreeSlotVerifierTest.java
```

Existing tests that may receive compatibility assertions:

```text
StoreDepositSliceADiagnosticsContractTest.java
AutoDepositMaintenanceSlotReliefTest.java
```

Manifest, recovery, and diagnostics delegation remains covered by the existing
orchestration/contract tests; add a focused test only if the refactor exposes an
uncovered semantic boundary.

### Existing upstream or global files intentionally left unchanged

```text
adris/altoclef/tasksystem/Task.java
adris/altoclef/tasksystem/TaskRunner.java
adris/altoclef/tasks/construction/PlaceBlockNearbyTask.java
adris/altoclef/tasks/container/StoreInContainerTask.java
adris/altoclef/tasks/container/AbstractDoToStorageContainerTask.java
adris/altoclef/tasks/InteractWithBlockTask.java
adris/altoclef/chains/PlayerInteractionFixChain.java
adris/altoclef/tasks/container/DoStuffInContainerTask.java
```

No file move, class rename, Task hierarchy redesign, or generic extension hook
is proposed.

## 10. Ownership ledger

| Concern | Owner after the patch | Evidence or rule |
| --- | --- | --- |
| Correlation | Existing automatic-deposit diagnostics (`autoOperationId` and child generation) | No new global registry. |
| Parent Task | Automatic `DepositAllTask` | It alone chooses its next child and the one-time `null` result. |
| Placement child identity | Per-task `DepositAllPlacementTaskOwner` | Retains the actual supplied instance; no static state. |
| Placement completion | Existing `PlaceBlockNearbyTask.isFinished()` | Predicate is unchanged and evaluated once for handoff. |
| One-tick barrier | Per-task `DepositAllPostPlaceHandoff` | Enabled only by `AutoDepositGeneralTaskFactory#create`. |
| Click initiation | Existing `InteractWithBlockTask` path on the next tick | No click API is added or moved. |
| SNEAK acquisition/release | Existing `PlaceBlockNearbyTask` | Existing `onStop()` releases its own input. |
| Retry count/reason | Existing downstream task owners | No retry state is introduced. |
| Timeout/terminal reason | Existing task and maintenance owners | Handoff has no clock or terminal result. |
| Custom goal/path | Existing placement/store descendants | Handoff creates and cancels none. |
| Interruption entry | Existing `Task.tick()` | The `null` branch stops and clears the current child. |
| Cleanup entry | Existing `PlaceBlockNearbyTask.onStop()` | Handoff only exposes the scheduler boundary. |
| Fallback | Existing `DepositAllTask` chest-obtain branch | Its conditions and result remain unchanged. |
| Global state | None | Helpers are instance-local; no static operation state. |

## 11. Regression and preservation matrix

| Scenario | Expected effect of the patch |
| --- | --- |
| Automatic general deposit after it places a container | Exactly one cleanup-only parent result, then normal scan/open on the next tick. |
| Automatic general deposit opening an already-existing container | No handoff because no retained completed placement child exists. |
| Manual `@deposit_all` | Existing constructors use ephemeral/disabled policy; behavior unchanged. |
| Trusted automatic destination | Uses `AutoDepositTrustedTaskFactory`; general handoff is not injected. |
| Manual `@store_home` / trusted storage | Outside the injection point; behavior unchanged. |
| Furnace flow | Existing `DoStuffInContainerTask` handoff remains independent and unchanged. |
| Carry On absent | No linkage dependency; one automatic post-placement cleanup boundary, then normal flow. |
| Carry On present or incompatible | No Carry On API call or fabricated success; normal flow resumes after the boundary. |
| Generic right-click, doors, trapdoors, beds, buttons, levers, item use | Generic interaction files are unchanged. |
| Block placement | The actual placement Task and predicate remain unchanged; only its parent handoff is delayed. |
| Baritone pathing | No global cancel or new goal/path; only existing placement cleanup runs. |
| Task interruption/resume | The retained actual child identity must survive ordinary parent interruption; no onStart/onStop reset may replace it with a fresh equal candidate. |
| Failure cleanup | Existing Task/child cleanup remains authoritative. |

## 12. Required tests before a runtime claim

Static/unit coverage must establish:

1. Retaining mode returns the same placement task for the same active request.
2. Ephemeral mode preserves existing manual allocation behavior.
3. A fresh equal candidate cannot replace the tracked actual active identity.
4. The handoff defers exactly once for active-and-finished placement and not for
   inactive or unfinished placement.
5. Clearing the owner prevents stale handoff state from leaking to a later
   placement.
6. A lifecycle harness observes placement active, one `null` barrier,
   placement stop exactly once, no open child in that call, and open child on the
   next call.
7. `AutoDepositGeneralTaskFactory#create` is the only enabled injection point.
8. Manual constructors, trusted tasks, and existing maintenance phase/outcome
   semantics remain unchanged.
9. Existing diagnostics contract and free-slot relief tests remain compatible.

Unit tests cannot prove Fabric `END_CLIENT_TICK` network delivery. A separately
authorized clean forced build, deployment hash check, and Minecraft reproduction
are required before claiming the runtime symptom is fixed.

## 13. Conservative divergence classification and rollback

`DepositAllTask.java` was created by LAVI commit
`9785e17b2a9bd87839e33436c2b72f416cce5dd8` as a behavior-preserving copy of the
store-in-any-container flow and was later changed by `673d21de...` and
`696265de...`. It is LAVI-created integration code, but it lives in the
`adris.altoclef` namespace and preserves upstream-derived engine behavior.
Therefore the `onTick()` handoff is conservatively recorded as a
behavior-changing engine/integration divergence.

The rollback unit is not a commit and not the whole dirty file. It is only:

```text
remove the automatic-only constructor injection from AutoDepositGeneralTaskFactory
remove the completed-placement one-shot null branch and owner-backed placement
  creation from DepositAllTask
remove the two handoff helper files only when no remaining caller uses them
preserve every pre-existing diagnostics hunk and unrelated refactor
```

Do not use `git reset`, `git checkout --`, `git restore`, broad commit revert, or
file replacement as rollback. Verification after rollback requires the manual,
automatic existing-container, trusted, furnace, and placement lifecycle tests,
plus a separately authorized runtime reproduction if runtime claims are needed.

## 14. Remaining evidence and stop conditions

The narrow patch may proceed, but all of these remain outside its claim:

```text
exact packet transit timing during the failed reproduction
exact carried block identity
generic Carry On activation policy outside this installed version/configuration
secondary Baritone OBTAIN_CHEST stall root cause
global interaction or TaskRunner changes
Carry On config or key-binding changes
```

If implementation requires changing `Task`, `TaskRunner`,
`PlaceBlockNearbyTask`, `InteractWithBlockTask`, `PlayerInteractionFixChain`,
Carry On settings, global input cleanup, or global Baritone state, stop. That is
a broader patch whose gate is not satisfied by this report.

## 15. Source application update

The narrow source change described above was subsequently applied in the same
authorized implementation task.

Implemented boundaries:

```text
automatic general DepositAllTask:
  retaining placement identity
  active-and-finished completion check
  one null cleanup barrier
  normal route resumes on the next parent tick

manual DepositAllTask:
  ephemeral placement identity
  disabled handoff

maintenance refactor:
  phase orchestration remains in AutoDepositMaintenanceTask
  construction, manifest lifecycle, working-set recovery, slot relief, and
  diagnostics delegate to the focused files listed in section 9
```

The injectable constructor rejects mismatched owner/handoff pairs. The retained
owner also refuses to replace an active scheduler-owned placement merely because
the next candidate names a different container block; replacement is allowed
after that task is inactive or stopped.

Static verification completed after application:

```text
reviewed implementation/document/test files: 20
missing files: 0
trailing-whitespace findings: 0
missing final newlines: 0
scoped git diff --check: PASS
forbidden Carry On / global SNEAK / generic interaction / TaskRunner / Baritone
  references in the handoff and maintenance scope: 0
enabled single-tick production injection points: 1
required new-production-file markers: 9 of 9 present
```

Focused unit, lifecycle, composition, free-slot, and source-contract tests were
added, but at that source-application checkpoint they were not executed because
Gradle/build execution had not been authorized. No JAR had been built or
deployed, Minecraft had not been launched, and no commit or push had been
performed at that checkpoint. Runtime symptom resolution therefore remained
unverified then. The subsequent separately authorized verification is recorded
below without rewriting that historical status.

## 16. Post-implementation verification update

<!-- 20260831_openai: Recorded the separately authorized clean build, artifact provenance, and captured Minecraft runtime verification without rewriting the pre-change evidence state. -->

The user subsequently authorized a clean forced build and performed the
Minecraft reproduction. This section supersedes only the current verification
status; the pre-change and source-application status blocks above remain
historical records of what had been authorized and observed at those times.

### Build and test evidence

```text
build log:
  plugins/Minecraft/runtime/chatclef_fabric_1.20.1/codex-build-logs/
    chatclef-fabric-1.20.1-build-20260831-013336.log

command:
  .\gradlew.bat clean build --rerun-tasks

result:
  BUILD SUCCESSFUL in 4m 43s
  exit code 0
  171 actionable tasks; 171 executed

Minecraft 1.20.1 test result:
  117 suites
  378 total
  377 passed
  0 failures
  0 errors
  1 skipped
```

The skipped test was
`AutoDepositCategoryReservePolicyTest.allocatesFuelAsOneCategoryTotalInsteadOfPerItem()`.
The 1.20.1 test runtime could not allocate the registry-free `Item` identities
used by that policy test, so `TestItems.item()` aborted it through
`TestAbortedException`. It is not a handoff test. The focused placement-owner,
post-place handoff, lifecycle, automatic-general factory, and free-slot tests
were included in the successful test run and passed.

### Artifact provenance

```text
built JAR:
  plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/
    chatclef-1.20.1-0.18.23.jar

active instance JAR:
  C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\
    chatclef-1.20.1-0.18.23.jar

bytes, both artifacts:
  7,360,897

SHA-256, both artifacts:
  84C6634433D7402B2935ADD6E4028F43BD3782D2E4038E00D30206092E9CA839

deployment verdict:
  VERIFIED - built and active artifact hashes match
```

The fresh session emitted diagnostics source marker
`20260731_post_place_handoff_p2` from that active instance JAR. Together with the
matching artifact hash and recorded code-source path and timestamp, this binds
the captured runtime to the hash-matched active artifact. It does not by itself
prove every runtime behavior.

### Captured Minecraft runtime evidence

The authoritative gameplay evidence is the session beginning at
`2026-08-31 01:40:12 KST` in:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\instance_audit.txt
```

The clearest Store-correlated lifecycle was `auto-deposit-1` /
`store-deposit-379`:

```text
tick 291:
  PlaceBlockNearbyTask STOP_BEGIN and STOP_END
  parent reconciliation clears the actual placement child
  candidate child is none; previousChildStopCalled=true

tick 292:
  return_open_existing_container
  the new chest at -1034,10,-148 is selected
  chest interaction return=SUCCESS

tick 293:
  STORE_DEPOSIT_TRANSFER_BEGIN
  outcome=GUI_OPEN_DELAYED
  expectedGuiOpened=true
  screenHandlerChangedObserved=true

tick 299:
  STORE_DEPOSIT_TRANSFER_TERMINAL

tick 341:
  terminalScope=MAINTENANCE_LOGICAL_TERMINAL
  terminalReason=free_slot_postcondition_observed
```

`auto-deposit-5` independently exercised the same boundary and supplied
adjacent inventory snapshots:

```text
tick 11845  PLACE_CONTAINER_NEARBY; adjacent snapshot mainHandItem=chest
tick 11846  adjacent snapshot mainHandItem=air
tick 11847  taskChain="Completing placed-container child handoff"
tick 11848  return_open_existing_container; interaction return=SUCCESS
tick 11849  GUI_OPEN_DELAYED and STORE_DEPOSIT_TRANSFER_BEGIN
tick 11917  MAINTENANCE_LOGICAL_TERMINAL / free_slot_postcondition_observed
tick 15510  USER_TASK_NATURAL_COMPLETION and diagnostic coverage close
```

The `mainHandItem=chest` and `mainHandItem=air` observations came from adjacent
interaction snapshots whose Store context was unavailable. They are consistent
with placement completion when combined with the correlated placement
stop/clear and next-tick `OPEN_EXISTING` lifecycle, but they are not a standalone
typed binding to the Store operation.

All five automatic operations in this session reached
`MAINTENANCE_LOGICAL_TERMINAL` with
`terminalReason=free_slot_postcondition_observed`. During the verified handoff,
Carry On 2.1.2.7 remained `AVAILABLE_NOT_CARRYING`; no pickup transition was
observed. Baritone did not own an active path or custom goal at the
placement-to-open boundary.

### Current verification status and limits

```text
SOURCE_IMPLEMENTATION_VERIFICATION:
  PASSED

UNIT_TESTS_1_20_1:
  PASSED_WITH_ONE_SKIPPED_NON_HANDOFF_TEST
  378 total; 377 passed; 0 failures; 0 errors; 1 skipped

CLEAN_FORCED_GRADLE_BUILD:
  PASSED

JAR_DEPLOYMENT_AND_HASH:
  VERIFIED

MINECRAFT_RUNTIME_REPRODUCTION:
  OBSERVED

ORIGINAL_HELD_CHEST_STALL_IN_CAPTURED_RUN:
  NOT OBSERVED

POST_PLACE_HANDOFF:
  VERIFIED_FOR_THE_CAPTURED_SCENARIO_ONLY

CURRENT_FAILURE_BOUNDARY:
  NO FAILURE BOUNDARY OBSERVED IN THE CAPTURED SCENARIO

ALL_CONFIGURATIONS_AND_CONTAINER_COMBINATIONS:
  NOT CLAIMED

COMMIT_AND_PUSH:
  NOT PERFORMED
```

One earlier operation in the same session encountered a Baritone
`BlockOptionalMeta.getManager/drops` resource-reload exception. The operation
and session continued to completion afterward, so the exception was non-terminal
in this capture. It did not recur at the verified handoff, and the logs do not
connect it to the former held-chest symptom.

This verification also does not close the separate bounded-diagnostics issue.
The session produced high-volume tool-selection output and exhausted the
per-store terminal-group reserve. Those are follow-up diagnostics-quality
findings, not evidence that the captured post-place handoff failed.

## 17. Post-checkpoint next-work direction

This report remains the canonical evidence record for the captured handoff and
must not be reinterpreted as a release-complete declaration. The work-separation
direction and acceptance gates for bounded diagnostics correctness,
tool-selection shaping, test-only container-type lifecycle coverage, and the
final release matrix are recorded in
[ChatClef Automatic Deposit Post-Checkpoint Work Separation Direction](chatclef-automatic-deposit-post-checkpoint-direction-2026-08-31.md).

That follow-up freezes the production handoff implementation at checkpoint
`a722ac2a17a813b62e605eaeac0fc9f96f7a1b5e`. It does not retroactively change
the source, build, artifact, or runtime evidence recorded here, and its
documentation status does not authorize source edits, tests, builds, runtime
reproduction, commit, or push.

## 18. 2026-08-31 post-checkpoint artifact distinction

The `84C6634433D7402B2935ADD6E4028F43BD3782D2E4038E00D30206092E9CA839`
artifact and captured Minecraft evidence above remain valid historical evidence
for checkpoint `a722ac2a17a813b62e605eaeac0fc9f96f7a1b5e`. They are not evidence for the
later diagnostics implementation.

The later clean forced build produced
`versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar` with SHA-256
`7D52F24AFA397064EC97F199297B6F8421BAD7392CE0398C35188C3F8627CD11`. The active
CurseForge instance still contained the historical `84C6...` artifact at the
time of inspection, so artifact identity was `MISMATCH` and the final-JAR
Minecraft matrix was not run. This report therefore makes no new runtime,
release, or merge claim for the later artifact. The current evidence ledger is
maintained in Section 15 of
[ChatClef Automatic Deposit Post-Checkpoint Work Separation Direction](chatclef-automatic-deposit-post-checkpoint-direction-2026-08-31.md).

Production handoff source remains unchanged relative to the checkpoint. No
commit, push, or deployment was performed for the later diagnostics build.
