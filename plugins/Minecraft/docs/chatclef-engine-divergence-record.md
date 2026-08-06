# ChatClef Engine Divergence Record

<!-- 20260730_kpopmodder: Recorded ChatClef engine divergence status for Carry On diagnostics review. -->

## Current Status

Verification status: APPLIED_NOT_BUILT

Active behavior-changing engine divergence: PRESENT

Active diagnostics-only engine divergence: PRESENT, default OFF after the current REQUEST CHANGES cleanup.

The previous diagnostics commit placed Carry On-specific imports, fields, observation, and logging inside
`adris.altoclef.tasks.InteractWithBlockTask`. The cleanup removed that Carry On-specific engine coupling
and returned Carry On diagnostics to the LAVI-owned optional integration namespace.

The current active behavior-changing divergences are:

- The bounded post-place container handoff in `adris.altoclef.tasks.container.DoStuffInContainerTask`.
- The Baritone worker tool-save policy snapshot boundary in `adris.altoclef.util.helpers.StorageHelper`.

Both are engine divergences because the classes are generic upstream-derived ChatClef / AltoClef workflow helpers.

## Upstream Baseline Provenance

Upstream repository: UNVERIFIED

Release or tag: UNVERIFIED

Exact commit: UNVERIFIED

Imported subtree: `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**`

Baseline version label observed in runtime logs: `altoclef 1.20.1-0.18.23`

Verification method: runtime log observation only; no upstream repository comparison completed in this pass.

Known LAVI-specific divergences: PARTIALLY_VERIFIED

## Upstream Provenance Completion Checklist

This record must not claim exact upstream provenance until each item below has
direct evidence:

```text
upstream repository URL
upstream branch, release, or tag
exact upstream commit
import date or baseline creation date
Minecraft version
Yarn mappings version
Fabric Loader version used for build
Fabric API version used for build
Loom or build plugin version
Java toolchain version
ChatClef / AltoClef version label
Baritone artifact identity
Baritone version label observed at runtime
LAVI-owned Java package roots
modified upstream-derived files
purpose of each divergence
build verification command and result
runtime verification instance and result
rollback unit for each divergence
```

Current local evidence is limited to:

```text
fabric.mod.json contact homepage/sources = https://github.com/MiranCZ/altoclef
gradle.properties mod_version = 0.18.23
gradle.properties archives_base_name = chatclef
runtime log label = altoclef 1.20.1-0.18.23
runtime log label = baritone 1.10.1-9-geace2ad1-dirty
```

These values are useful identifiers, but they are not proof of the exact
upstream commit. Until upstream comparison is completed, keep the upstream
repository, release/tag, and exact commit fields marked `UNVERIFIED`.

When provenance is completed, record the comparison method and the local command
or artifact used to prove it. Do not use a successful build or runtime launch as
upstream provenance proof.

## Active Behavior-Changing Divergence Record

Review baseline SHA: `a15037399aef1619dc8430b478c42443a47049bf`

Modified engine file:

- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/container/DoStuffInContainerTask.java`

Modified class: `adris.altoclef.tasks.container.DoStuffInContainerTask`

Modified methods:

- `onStart`
- `onTick`
- `onStop`

Behavior change:

- After `PlaceBlockNearbyTask` reports finished, `DoStuffInContainerTask.onTick()` returns `null` once to let the
  previous child Task stop through the existing lifecycle.
- That first `null` return is the lifecycle handoff barrier that lets `PlaceBlockNearbyTask.onStop()` release the
  SNEAK input it owns.
- After the barrier, the task enters a post-place handoff phase and observes SNEAK stability for a bounded budget.
- If `sneakHeld=false` and `playerSneaking=false`, the phase returns to `IDLE` and the existing container interaction
  flow continues in the same tick.
- If either SNEAK state remains true until the budget is exhausted, the phase returns to `IDLE`, records one handoff
  budget diagnostic when diagnostics are enabled, and the existing container interaction flow continues in the same
  tick.

Exact stability budget:

- `POST_PLACE_STABILITY_MAX_WAIT_TICKS = 3`

Input ownership:

- Unchanged.
- The hunk does not acquire or release any input.
- It does not force-release `Input.SNEAK`.

Retry ownership:

- Unchanged.
- No retry loop, blacklist, cooldown, or fallback policy is added.

Baritone ownership:

- Unchanged.
- No Baritone goal, path, process, or cancellation behavior is added.

Global input release:

- None.

Carry On-specific engine coupling:

- None.
- `DoStuffInContainerTask` does not import Carry On types and does not contain Carry On policy.

Affected generic subclasses and workflows:

- Furnace
- Smoker
- Blast Furnace
- Crafting Table
- Anvil
- Smithing Table
- Any other workflow extending `DoStuffInContainerTask`

Rollback unit:

- Revert the exact bounded post-place handoff hunk in `DoStuffInContainerTask.java`.
- Do not use `git reset`, broad checkout, broad restore, or cleanup commands as rollback.

Build result:

- NOT_RUN for the current working-tree hunk at the time this record was updated.

Runtime reproduction result:

- NOT_RUN for the current working-tree hunk at the time this record was updated.

Verification status:

- APPLIED_NOT_BUILT

## Active Behavior-Changing Divergence Record: Tool Save Policy Snapshot Boundary

Review baseline SHA: `3794684fdf22c2d28b7796d17d00e25f2f651525`

Modified engine file:

- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/util/helpers/StorageHelper.java`

Modified class: `adris.altoclef.util.helpers.StorageHelper`

Modified method:

- `shouldSaveStack`

Baseline file hash before this hunk: `1fe86541debf14bb896e861013252b1ae792cddf`

Companion LAVI-owned files:

- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/integration/toolselect/ToolSavePolicyEntrypoint.java`
- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/integration/toolselect/snapshot/ToolSavePolicySnapshot.java`
- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/integration/toolselect/snapshot/ToolSavePolicySnapshotProvider.java`
- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/integration/toolselect/snapshot/ToolSavePolicySnapshotPublisher.java`
- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/toolselect/ToolSavePolicySnapshotDiagnostics.java`

Verified reason:

- Runtime logs from `LAVI_TEST_Fabric01` on `2026-08-05 21:40:27` showed
  `pool-8-thread-4` entering `StorageHelper.shouldSaveStack -> ItemStorageTracker.hasItem ->
  InventorySubTracker.updateState` from Baritone `ToolSet.getBestSlot`.
- The same tracker identity was being rebuilt by the Render thread, with `INVENTORY_SUBTRACKER_SCAN_OVERLAP_DETECTED`,
  worker `SHARED_RESET_BEGIN/END`, and a following `InventorySubTracker.registerItem` NPE.

First failing boundary:

- Baritone worker path-calculation code crossed into AltoClef live client inventory tracker rebuild through
  `StorageHelper.shouldSaveStack`.

Behavior change:

- Client-thread callers keep the existing live `mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE)` behavior.
- Non-client-thread callers no longer call live `ItemStorageTracker` from `shouldSaveStack`.
- Non-client-thread callers read the latest immutable client-published `ToolSavePolicySnapshot` instead.
- If the snapshot is not ready, the worker fails closed by preserving the iron pickaxe instead of silently treating
  `hasDiamondPickaxe` as false.
- Existing low-durability `+8`, `+30`, diamond-related block, and mining-requirement predicates are preserved.

Input ownership:

- Unchanged.
- No input is acquired, released, or force-cleared.

Retry ownership:

- Unchanged.
- No retry loop, timeout, blacklist, cooldown, or fallback policy is added.

Baritone ownership:

- Pathing algorithm, worker lifecycle, path cancellation, goal ownership, and thread count are unchanged.
- The hunk only changes the tool-save policy data source used by Baritone worker callers.

Carry On-specific engine coupling:

- None.
- The hunk contains no Carry On imports, version policy, capability state, retry policy, or cleanup behavior.

Generic behavior preserved:

- `TaskRunner`, `AltoClef`, `PlayerInteractionFixChain`, `InteractWithBlockTask`, and `InventorySubTracker` lifecycle
  behavior are unchanged.
- Existing client-thread tool selection behavior is intentionally retained.

Rollback unit:

- Revert only the `StorageHelper.shouldSaveStack` snapshot-read hunk.
- Remove the LAVI-owned tool-save snapshot entrypoint/provider/publisher/diagnostic files and the matching
  `fabric.mod.json` entrypoint line if this divergence is rolled back.
- Do not use `git reset`, broad checkout, broad restore, or cleanup commands as rollback.

Build result:

- NOT_RUN for the current working-tree hunk at the time this record was updated.

Runtime reproduction result:

- NOT_RUN for the current working-tree hunk at the time this record was updated.

Verification status:

- APPLIED_NOT_BUILT

## Active Diagnostics Divergence Record

Review baseline SHA: `a15037399aef1619dc8430b478c42443a47049bf`

Modified engine or engine-adjacent files:

- `adris/altoclef/tasksystem/Task.java`
- `adris/altoclef/control/InputControls.java`
- `adris/altoclef/control/SlotHandler.java`
- `adris/altoclef/mixins/WorldBlockModifiedMixin.java`
- `lavi/minecraft/diagnostics/ChatClefDiagnostics.java`
- `lavi/minecraft/integration/carryon/CarryOnRuntimeStateObserver.java`

Behavior intent:

- Diagnostics default to OFF unless explicitly enabled by `-Dlavi.chatclef.diagnostics=boundary`,
  `-Dlavi.chatclef.diagnostics=verbose`, `LAVI_CHATCLEF_DIAGNOSTICS=boundary`, or
  `LAVI_CHATCLEF_DIAGNOSTICS=verbose`.
- Unknown diagnostic mode values are treated as OFF.
- AltoClef `logLevel=ALL` no longer implicitly enables LAVI diagnostics.
- High-frequency diagnostic entry points return early when diagnostics are OFF.
- When diagnostics are enabled, `ChatClefDiagnostics` logs one `DIAGNOSTICS_RUNTIME_IDENTITY` boundary event with
  the active output mode, source marker, class code source, code source last-modified time, and implementation version.
- Verbose slot diagnostics now report non-window or out-of-range slots as `not_read#reason=...` before reading the
  backing `ScreenHandler` slot. This keeps diagnostic stack inspection from causing the existing upstream
  `Screen Slot Error (ignored)` warning for `PlayerSlot.UNDEFINED` / `windowSlot=-999`.

Behavior preserved:

- No slot click branch, return value, timer, retry, input state, ScreenHandler state, or controller click call was
  changed.
- The slot-stack guard is diagnostics-only and runs only when verbose diagnostics are enabled.
- The runtime identity event is observation-only and runs once per client session when diagnostics are enabled.

Runtime evidence prompting this follow-up:

- `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log` showed successful furnace
  boundary flow, but the tested jar timestamp could not be proven from the log alone.
- The same log showed `Screen Slot Error (ignored)` / `Index -999` during `CraftInTableTask.onResourceStop`, with the
  stack passing through `SlotHandler.clickSlot` and `ChatClefDiagnostics.safeValue`.

Open P2 follow-up items:

- `TASK_INSTANCE_IDS` lifecycle cleanup.
- `TASK_RUN_IDS` lifecycle cleanup.
- `PARENT_TASK_RUN_IDS` lifecycle cleanup.
- True weak-identity diagnostic registry, if lifecycle cleanup is insufficient.
- Global mutable `traceId` should not be used as causal correlation.
- Observation session auto-restart and heartbeat policy should be redesigned or removed when verbose diagnostics are
  revisited.

## Previous REQUEST CHANGES Cleanup Record

Repository HEAD at review baseline: `3e661eeef522253b106720675f907df80281b896`

Modified file: `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/InteractWithBlockTask.java`

Modified class: `adris.altoclef.tasks.InteractWithBlockTask`

Modified methods:

- `onStart`
- `onTick`
- `onStop`

Baseline file hash: `3cee030eee154b9656b8f2c267c0a809bce3f984`

Exact LAVI hunk:

- Removed `lavi.minecraft.integration.carryon.CarryOnDiagnostics` import.
- Removed `lavi.minecraft.integration.carryon.CarryOnObservation` import.
- Removed Carry On diagnostic correlation fields.
- Removed Carry On diagnostic start logging from `onStart`.
- Removed Carry On observation and state-change logging around `rightClick(mod)` in `onTick`.
- Removed Carry On diagnostic stop logging from `onStop`.

Divergence category: diagnostics-only divergence removal.

Verified reason: REQUEST CHANGES review required generic ChatClef interaction code to contain no Carry On-specific
types, imports, version logic, state policy, or diagnostic behavior.

Evidence:

- Runtime reproduction showed furnace pickup by Carry On at `LAVI_TEST_Fabric01/logs/latest.log` lines 586-587 and
  649-650 before this cleanup.
- That evidence proves the symptom, but it does not authorize keeping Carry On-specific code in a generic engine task.

Last successful boundary: ChatClef reached `InteractWithBlockTask` and attempted right-click on a furnace.

First failing boundary: runtime evidence showed the clicked furnace position became `Block{minecraft:air}` and Carry On
state became `AVAILABLE_CARRYING` before a furnace screen opened.

Containment analysis:

- LAVI-owned parent Task or wrapper has not yet been implemented.
- Existing LAVI-owned Carry On bridge can observe optional Carry On state, but it does not yet observe the exact generic
  right-click boundary without engine wiring.
- A generic no-op observer seam is not implemented in this pass. It must be proposed separately with exact file,
  method, and hunk if external composition cannot observe the boundary.

Ownership impact:

- Input ownership: unchanged.
- Retry ownership: unchanged.
- Timeout ownership: unchanged.
- Custom goal ownership: unchanged.
- Path ownership: unchanged.
- Interruption behavior: unchanged.
- Cleanup behavior: restored to the existing `InteractWithBlockTask.onStop` cleanup path without diagnostic calls.
- Global state: no Carry On global state is attached to the engine task.

Generic behavior preserved:

- `InteractWithBlockTask.isFinished()` remains unchanged.
- `PlayerInteractionFixChain` remains unchanged.
- No global input release was added.
- No global Baritone cancellation was added beyond the existing upstream-derived method body.
- No retry, timeout, blacklist, cooldown, or fallback behavior was added.

Regression tests:

- `.\gradlew.bat :1.20.1:compileJava` completed successfully.
- Runtime regression scenarios were not rerun at the time this record was updated.

Runtime reproduction result:

- Not rerun after this cleanup at the time this record was created.

Rollback:

- Revert only the REQUEST CHANGES cleanup hunk in `InteractWithBlockTask.java` if the review requires restoring the
  previous diagnostics-only engine coupling.
- Do not use `git reset`, `git checkout --`, `git restore`, or broad cleanup commands as the rollback method.

Upstream comparison status: UNVERIFIED
