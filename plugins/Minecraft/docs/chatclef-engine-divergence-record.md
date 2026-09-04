# ChatClef Engine Divergence Record

<!-- 20260730_kpopmodder: Recorded ChatClef engine divergence status for Carry On diagnostics review. -->

## Current Status

Verification status: MIXED; use each divergence record's own build and runtime status

Active behavior-changing engine divergence: PRESENT

Active diagnostics-only engine divergence: PRESENT, default OFF after the current REQUEST CHANGES cleanup.

The previous diagnostics commit placed Carry On-specific imports, fields, observation, and logging inside
`adris.altoclef.tasks.InteractWithBlockTask`. The cleanup removed that Carry On-specific engine coupling
and returned Carry On diagnostics to the LAVI-owned optional integration namespace.

The current active behavior-changing divergences are:

- The Block break mixin runtime target fallback in `adris.altoclef.mixins.BlockModifiedByPlayerMixin`.
- The title screen entry mixin runtime target fallback in `adris.altoclef.mixins.EntryMixin`.
- The player damage mixin runtime target fallback in `adris.altoclef.mixins.PlayerDamageMixin`.
- The entity animation swing mixin runtime target fallback in `adris.altoclef.mixins.EntityAnimationSwungMixin`.
- The Baritone movement helper infested-block redirect runtime target fallback in
  `adris.altoclef.mixins.MovementHelperMixin`.
- The bounded post-place container handoff in `adris.altoclef.tasks.container.DoStuffInContainerTask`.
- The Baritone worker tool-save policy snapshot boundary in `adris.altoclef.util.helpers.StorageHelper`.
- The default-preserving accepted-input, interaction-observer, container-factory,
  parent-retention, and client-tick-boundary seams used by the exact-container
  GUI three-later-tick gate.
- The exact-container route integration in the upstream-derived general-storage
  and regular-furnace Task owners, including ancestor retention while the gate
  forbids parent mutation.

These are engine divergences because the classes are generic upstream-derived ChatClef / AltoClef workflow helpers.

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

## Active Behavior-Changing Divergence Record: Block Break Mixin Runtime Target Fallback

Review baseline SHA: `aa1484188d8f76266f081850ed19c6beb320fe61`

Modified engine file:

- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/mixins/BlockModifiedByPlayerMixin.java`

Modified class: `adris.altoclef.mixins.BlockModifiedByPlayerMixin`

Modified method:

- `onBlockBroken`

Verified reason:

- `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log` from
  `2026-08-07 15:02:05 KST` showed Minecraft aborting during mixin apply.
- The failing boundary was `altoclef.mixins.json:BlockModifiedByPlayerMixin`, where
  `@Inject` on `onBlockBroken` could not find target method `onBreak` in `net.minecraft.class_2248`.
- The built 1.20.1 jar's `chatclef-refmap.json` did not contain `BlockModifiedByPlayerMixin` or `onBreak`,
  so the runtime attempted to resolve the named method directly against the intermediary runtime class.

First failing boundary:

- Mixin target resolution for `BlockModifiedByPlayerMixin` before Minecraft reached the ChatClef entrypoints.

Behavior change:

- For the `MC <= 12002` preprocessed branch, the injection annotation now targets the intermediary runtime method
  `method_9576` directly with `remap = false`.
- The `MC > 12002` branch continues to target named `onBreak` through the existing remap path.
- The injected method body, `BlockBrokenEvent` publication, command lifecycle payloads, and diagnostics payloads are
  unchanged.

Input ownership:

- Unchanged.
- No input is acquired, released, or force-cleared.

Retry ownership:

- Unchanged.
- No retry loop, timeout, blacklist, cooldown, or fallback policy is added.

Baritone ownership:

- Unchanged.
- No Baritone goal, path, process, or cancellation behavior is added.

Carry On-specific engine coupling:

- None.
- The hunk contains no Carry On imports, version policy, state observation, retry policy, or cleanup behavior.

Generic behavior preserved:

- The same block break event hook is still injected at `HEAD`.
- The hunk only changes target-name resolution for the affected preprocessed runtime branch.

Rollback unit:

- Revert only the annotation branch hunk in `BlockModifiedByPlayerMixin.java`.
- Do not use `git reset`, broad checkout, broad restore, or cleanup commands as rollback.

Build result:

- `.\gradlew.bat clean build --rerun-tasks` completed successfully on `2026-08-07`.
- Result: `BUILD SUCCESSFUL in 2m 29s`, `139 actionable tasks: 139 executed`.
- Verified final `1.20.1` jar class annotation with `javap`: `method=["method_9576"]`, `remap=false`.

Runtime reproduction result:

- NOT_RUN after this hunk at the time this record was updated.

Verification status:

- BUILT_NOT_RUNTIME_REPRODUCED

## Active Behavior-Changing Divergence Record: Title Screen Entry Mixin Runtime Target Fallback

Review baseline SHA: `aa1484188d8f76266f081850ed19c6beb320fe61`

Modified engine file:

- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/mixins/EntryMixin.java`

Modified class: `adris.altoclef.mixins.EntryMixin`

Modified method:

- `init`

Verified reason:

- `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log` from
  `2026-08-07 15:15:46 KST` showed Minecraft aborting during mixin apply after the block break mixin target fallback
  allowed startup to proceed further.
- The failing boundary was `altoclef.mixins.json:EntryMixin`, where `@Inject` on `init` could not find target method
  `Lnet/minecraft/class_442;init()V` in `net.minecraft.class_442`.
- The built 1.20.1 jar's refmap mapped `EntryMixin` `init()V` to the unresolved runtime target
  `Lnet/minecraft/class_442;init()V`, while adjacent generated refmaps and runtime evidence point to the intermediary
  target `method_25426()V` for this title screen initialization hook.

First failing boundary:

- Mixin target resolution for `EntryMixin` before Minecraft reached the ChatClef entrypoints or LAVI bridge handshake.

Behavior change:

- For the `MC >= 12001 && MC <= 12002` preprocessed branch, the injection annotation now targets the intermediary
  runtime method `method_25426()V` directly with `remap = false`.
- The `MC > 12002` and `MC < 12001` branches continue to target named `init()V` through the existing remap path.
- The injected method body, one-time `_initialized` guard, `Debug.logMessage("Global Init")`, and
  `TitleScreenEntryEvent` publication are unchanged.

Input ownership:

- Unchanged.
- No input is acquired, released, or force-cleared.

Retry ownership:

- Unchanged.
- No retry loop, timeout, blacklist, cooldown, or fallback policy is added.

Baritone ownership:

- Unchanged.
- No Baritone goal, path, process, or cancellation behavior is added.

Carry On-specific engine coupling:

- None.
- The hunk contains no Carry On imports, version policy, state observation, retry policy, or cleanup behavior.

Generic behavior preserved:

- The same title screen initialization event hook is still injected at `HEAD`.
- The hunk only changes target-name resolution for the affected preprocessed runtime branch.

Rollback unit:

- Revert only the annotation branch hunk in `EntryMixin.java`.
- Do not use `git reset`, broad checkout, broad restore, or cleanup commands as rollback.

Build result:

- `.\gradlew.bat clean build --rerun-tasks` completed successfully on `2026-08-07`.
- Result: `BUILD SUCCESSFUL in 1m 42s`, `139 actionable tasks: 139 executed`.
- Verified final `1.20.1` jar class annotation with `javap`: `method=["method_25426()V"]`, `remap=false`.

Runtime reproduction result:

- NOT_RUN after this hunk at the time this record was updated.

Verification status:

- BUILT_NOT_RUNTIME_REPRODUCED

## Active Behavior-Changing Divergence Record: Player Damage Mixin Runtime Target Fallback

Review baseline SHA: `aa1484188d8f76266f081850ed19c6beb320fe61`

Modified engine file:

- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/mixins/PlayerDamageMixin.java`

Modified class: `adris.altoclef.mixins.PlayerDamageMixin`

Modified method:

- `applyDamage`

Verified reason:

- The `LAVI_TEST_Fabric01` latest launch log from `2026-08-07 15:27:31 KST` failed while applying
  `altoclef.mixins.json:PlayerDamageMixin`.
- Mixin could not find `Lnet/minecraft/class_746;damage(Lnet/minecraft/class_1282;F)Z` in
  `net.minecraft.class_746`.
- The previous block-break and title-screen mixin target fallbacks allowed startup to advance to this next mixin
  target failure.

First failing boundary:

- Mixin target resolution before the Minecraft client reaches the ChatClef bridge handshake.

Behavior change:

- For the `MC >= 12001 && MC <= 12002` preprocessor branch, the injection target now uses
  `method_5643(Lnet/minecraft/class_1282;F)Z` with `remap=false`.
- Other preprocessor branches keep the existing `damage` remap path.
- The injected body still only publishes `PlayerDamageEvent` with the original `DamageSource` and amount.

Input ownership:

- Unchanged.
- The hunk does not acquire or release any input.

Retry ownership:

- Unchanged.
- No retry loop, blacklist, cooldown, or fallback policy is added.

Baritone ownership:

- Unchanged.
- No Baritone goal, path, process, or cancellation behavior is added.

Generic behavior preserved:

- The same client-player damage event hook is still injected at `HEAD`.
- The hunk only changes target-name resolution for the affected preprocessed runtime branch.

Rollback unit:

- Revert only the annotation branch hunk in `PlayerDamageMixin.java`.
- Do not use `git reset`, broad checkout, broad restore, or cleanup commands as rollback.

Build result:

- `.\gradlew.bat clean build --rerun-tasks` completed successfully on `2026-08-07`.
- Result: `BUILD SUCCESSFUL in 2m 16s`, `139 actionable tasks: 139 executed`.
- Verified final `1.20.1` jar class annotation with `javap`:
  `method=["method_5643(Lnet/minecraft/class_1282;F)Z"]`, `remap=false`.

Runtime reproduction result:

- NOT_RUN after this hunk at the time this record was updated.

Verification status:

- BUILT_NOT_RUNTIME_REPRODUCED

## Active Behavior-Changing Divergence Record: Entity Animation Swing Mixin Runtime Target Fallback

Review baseline SHA: `aa1484188d8f76266f081850ed19c6beb320fe61`

Modified engine file:

- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/mixins/EntityAnimationSwungMixin.java`

Modified class: `adris.altoclef.mixins.EntityAnimationSwungMixin`

Modified method:

- `onEntityAnimation`

Verified reason:

- The `LAVI_TEST_Fabric01` latest launch log from `2026-08-07 15:34:58 KST` failed while applying
  `altoclef.mixins.json:EntityAnimationSwungMixin`.
- Mixin could not find `Lnet/minecraft/class_634;onEntityAnimation(Lnet/minecraft/class_2616;)V` in
  `net.minecraft.class_634`.
- The local `1.20.1` Yarn mappings identify this hook as
  `method_11160(Lnet/minecraft/class_2616;)V`, and adjacent generated refmaps such as `1.20.2` and `1.21.1` already
  map `onEntityAnimation` to that intermediary target.

First failing boundary:

- Mixin target resolution after the Fabric ChatClef bridge entrypoint registers but before Minecraft finishes client
  initialization and before the bridge can reach a stable runtime session.

Behavior change:

- For the `MC >= 12001 && MC <= 12002` preprocessor branch, the injection target now uses
  `method_11160(Lnet/minecraft/class_2616;)V` with `remap=false`.
- Other preprocessor branches keep the existing `onEntityAnimation` remap path.
- The injected body still only reads the entity id and animation id, then publishes `EntitySwungEvent` for main-hand or
  off-hand swing animations.

Input ownership:

- Unchanged.
- The hunk does not acquire or release any input.

Retry ownership:

- Unchanged.
- No retry loop, blacklist, cooldown, or fallback policy is added.

Baritone ownership:

- Unchanged.
- No Baritone goal, path, process, or cancellation behavior is added.

Generic behavior preserved:

- The same entity animation packet hook is still injected at `HEAD`.
- The hunk only changes target-name resolution for the affected preprocessed runtime branch.

Rollback unit:

- Revert only the annotation branch hunk in `EntityAnimationSwungMixin.java`.
- Do not use `git reset`, broad checkout, broad restore, or cleanup commands as rollback.

Build result:

- `.\gradlew.bat clean build --rerun-tasks` completed successfully on `2026-08-07`.
- Result: `BUILD SUCCESSFUL in 2m 13s`, `139 actionable tasks: 139 executed`.
- Verified final `1.20.1` jar class annotation with `javap`:
  `method=["method_11160(Lnet/minecraft/class_2616;)V"]`, `remap=false`.

Runtime reproduction result:

- NOT_RUN after this hunk at the time this record was updated.

Verification status:

- BUILT_NOT_RUNTIME_REPRODUCED

## Active Behavior-Changing Divergence Record: Movement Helper Infested-Block Redirect Runtime Target Fallback

Review baseline SHA: `e46552b`

Modified engine file:

- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/mixins/MovementHelperMixin.java`

Modified class: `adris.altoclef.mixins.MovementHelperMixin`

Modified method:

- `allowInfested`

Baseline file hash:

- SHA-256 `5579322A5E71DE3220FDE1FC51726ED2018A2D807BCA4B8BEDEE2E189C933BC3`

Verified reason:

- The `LAVI_TEST_Fabric01` crash report from `2026-08-07 15:44:52 KST` failed after Minecraft startup,
  LAVI bridge registration, and WebSocket handshake succeeded.
- The failing boundary was `altoclef.mixins.json:MovementHelperMixin`, where the redirector
  `allowInfested(Lnet/minecraft/class_2680;)Lnet/minecraft/class_2248;` failed injection with
  `(0/1) succeeded. Scanned 0 target(s).`
- The previous generated `1.20.1` jar annotation kept the redirect target as
  `Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;`, while the included 1.20.1 Baritone jar's
  runtime bytecode calls `Lnet/minecraft/class_2680;method_26204()Lnet/minecraft/class_2248;` at both candidate
  `avoidBreaking` call sites.

First failing boundary:

- Runtime Mixin target resolution for `MovementHelperMixin` when Baritone `MovementHelper` is loaded from the
  pathing/cache execution path.

Behavior change:

- For the `MC >= 12001` preprocessor branch, the redirect now targets the runtime intermediary method
  `Lnet/minecraft/class_2680;method_26204()Lnet/minecraft/class_2248;` directly with `remap=false`.
- The target method selector remains `avoidBreaking`; this Baritone helper has no competing `avoidBreaking` overload in
  the included 1.20.1 jar.
- The existing `ordinal = 1` choice is preserved for the 1.20.1+ branch.
- The `MC < 12001` branch keeps the previous named `BlockState.getBlock()` redirect target and `ordinal = 0`.
- The handler body still only maps `InfestedBlock` to its regular block before returning the original block otherwise.

Input ownership:

- Unchanged.
- No input is acquired, released, or force-cleared.

Retry ownership:

- Unchanged.
- No retry loop, blacklist, cooldown, timeout, or fallback policy is added.

Baritone ownership:

- Unchanged.
- No Baritone goal, path, process, cancellation, path selection, or cache behavior is added.

Carry On-specific engine coupling:

- None.
- The hunk contains no Carry On imports, version policy, state observation, retry policy, or cleanup behavior.

Generic behavior preserved:

- The same `avoidBreaking` infested-block redirect remains in place.
- The hunk changes only runtime namespace resolution for the existing redirect target.
- The redirect handler body and ordinal semantics are unchanged for the affected branch.

Rollback unit:

- Revert only the annotation branch hunk in `MovementHelperMixin.java`.
- Do not use `git reset`, broad checkout, broad restore, or cleanup commands as rollback.

Build result:

- `.\gradlew.bat clean build --rerun-tasks` completed successfully on `2026-08-07`.
- Result: `BUILD SUCCESSFUL in 1m 47s`, `139 actionable tasks: 139 executed`.
- Verified final `1.20.1` jar class annotation with `javap`:
  `method=["avoidBreaking"]`, `target="Lnet/minecraft/class_2680;method_26204()Lnet/minecraft/class_2248;"`,
  `ordinal=1`, `remap=false`.
- Verified included 1.20.1 Baritone jar `MovementHelper.avoidBreaking` bytecode contains the matching
  `class_2680.method_26204()` invocation at the preserved ordinal.

Runtime reproduction result:

- NOT_RUN after this hunk at the time this record was updated.

Verification status:

- BUILT_NOT_RUNTIME_REPRODUCED

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

## Active Diagnostics Divergence Record: Bare Deposit Operation Correlation First Partial Patch Batch

Date: 2026-08-19

Review baseline SHA: `a41c54c72f239fe4759cf255d415eaa7805e82af`

The built and observed JAR came from a dirty working-tree snapshot based on
that repository HEAD. The baseline identifies the comparison point; it does
not claim that the partial diagnostic patch was committed.

Verification status:

```text
PARTIALLY_IMPLEMENTED_DIAGNOSTICS_ONLY
BUILT_AND_LIVE_PREFIX_OBSERVED
NOT_CANONICAL_REPRODUCTION_COMPLETE
NOT_READY_FOR_MERGE
root cause: still unverified
```

Canonical design and exact implementation ledger:

```text
plugins/Minecraft/docs/chatclef-bare-deposit-diagnostics-plan.md
```

Runtime evidence:

```text
plugins/Minecraft/docs/chatclef-bare-deposit-diagnostics-reproduction-2026-08-19-r1.md
```

Modified upstream-derived observation boundaries:

```text
adris/altoclef/chains/SingleTaskChain.java
    SingleTaskChain.onTick(): observe the already evaluated natural-finish result
adris/altoclef/chains/UserTaskChain.java
    UserTaskChain.cancel(AltoClef): mark the current root as an explicit-cancel candidate
adris/altoclef/tasks/container/ContainerStoredTracker.java
    ContainerStoredTracker.startTracking(): observe the existing slot-change callback and predicate result
adris/altoclef/tasks/container/StoreInAnyContainerTask.java
    StoreInAnyContainerTask.onStart(): bind the existing root stored-item tracker
adris/altoclef/tasks/container/StoreInContainerTask.java
    StoreInContainerTask.onStart(): observe the target-container predicate and bind the target tracker
    StoreInContainerTask.onContainerOpenSubtask(...): observe selected transfer return boundaries
adris/altoclef/tasksystem/Task.java
    Task.tick(TaskChain): observe child reconciliation in the non-null and null candidate branches
    Task.stop(Task): observe STOP BEGIN/END boundaries
    Task.interrupt(Task): observe INTERRUPT BEGIN/END boundaries
adris/altoclef/trackers/UserBlockRangeTracker.java
    UserBlockRangeTracker.updateState(): observe null BlockPos immediately before the original dereference
```

Modified existing LAVI-owned diagnostic boundaries:

```text
lavi/minecraft/diagnostics/command/deposit/DepositCommandDiagnosticFields.java
lavi/minecraft/diagnostics/command/deposit/DepositCommandDiagnostics.java
lavi/minecraft/diagnostics/container/store/StoreInAnyContainerDiagnostics.java
```

New LAVI-owned package:

```text
lavi/minecraft/diagnostics/container/store/deposit/StoreDepositDiagnostics.java
lavi/minecraft/diagnostics/container/store/deposit/binding/StoreDepositBindingRegistry.java
lavi/minecraft/diagnostics/container/store/deposit/budget/StoreDepositEmissionGate.java
lavi/minecraft/diagnostics/container/store/deposit/context/StoreDepositOperationContext.java
lavi/minecraft/diagnostics/container/store/deposit/context/StoreDepositOperationState.java
lavi/minecraft/diagnostics/container/store/deposit/event/StoreDepositEventFields.java
```

Diagnostic intent:

```text
issue one local storeOperationId for a bare DepositCommand
merge that ID into the Store root start/stop callback fields
observe generic Task stop/interrupt and child reconciliation boundaries
observe selected StoreInContainer transfer returns
observe ContainerStoredTracker slot-change callbacks
observe a null UserBlockRangeTracker BlockPos immediately before the original dereference
emit placeholder terminal/effect/Baritone/coverage summary names when the root finalizes
```

Behavior intended to remain unchanged:

```text
return values
Task selection, ordering, completion, equality, replacement, and interruption semantics
retry, timeout, cooldown, and fallback policy
input state and ownership
Baritone goal, path, process, adoption, and cancellation
container click, cursor, screen, slot, and transfer behavior
original NPE propagation
wire payload, acknowledgement, and command terminal status
```

Source review found no intentional changes to those behavior owners. The
existing predicate, inventory query, `isFinished()` result, and null dereference
are not called a second time for diagnostics. This is diagnostics-only intent,
not proof of zero runtime impact.

Build evidence:

```text
command:
    .\gradlew.bat clean build --rerun-tasks --no-daemon --offline
JDK:
    Eclipse Adoptium 21.0.12.8
result:
    BUILD SUCCESSFUL in 3m 11s
    139 actionable tasks: 139 executed
1.20.1 JAR bytes:
    6,431,821
1.20.1 JAR SHA-256:
    e8c097a75dc205954224bb22e406f33ab9141a0f59de83e5d50c0235a28520bf
active CurseForge JAR SHA-256:
    e8c097a75dc205954224bb22e406f33ab9141a0f59de83e5d50c0235a28520bf
```

Runtime prefix evidence:

```text
one DEPOSIT_COMMAND_INVOCATION_DECISION
one STORE_IN_ANY_CONTAINER_START
same storeOperationId=store-deposit-549
256 STORE_TASK_CHILD_RECONCILIATION records
256 STORE_TASK_LIFECYCLE_BOUNDARY records
one USER_BLOCK_RANGE_NULL_INPUT_OBSERVED before the first logged NPE
Store root still active at the fixed prefix cutoff
terminal summary group not observed
```

Known divergence and correctness gaps:

```text
the slice spans multiple upstream lifecycle owners rather than one isolated boundary
the required investigation marker is absent from the first new upstream diagnostic block in this patch batch
child roles do not distinguish ROOT_ROUTE/TARGET_ACTION/SEARCH_FALLBACK
NPE operation correlation uses last-active temporal inference, not exact immutable provenance
event-specific 256-key gates do not implement the canonical 5000 session cap and critical reserve
descendant Task bindings have no per-operation retirement/cap
terminal four-summary emission is sequential, not atomically reserved or idempotently finalized
effectObservationComplete and expected-effect semantics can overclaim incomplete observation
focused helper, race, cap, cleanup, and terminal-summary tests are absent
```

The live prefix confirms that this source is loaded and that some boundaries
emit. It also confirms that lifecycle and reconciliation detail becomes blind
early while the Store loop continues. It does not prove the parent raw candidate,
filtered search, pursuit, target callback, craft interaction, Baritone generation,
slot effect, or earliest failure boundary.

Rollback unit:

```text
revert only the bare-deposit diagnostic calls/imports in the listed upstream-derived files
revert only the Store-operation field merge in the listed existing LAVI diagnostics
remove the new LAVI-owned deposit diagnostics package as one coherent diagnostic unit
do not use git reset, broad checkout, broad restore, or repository cleanup
```

No behavior-changing fix is authorized by this record. Correct the canonical
boundedness/correlation gaps and obtain focused test evidence before another
long reproduction or merge decision.

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

## 2026-08-26 Deposit-All Filtered Scanner Observation Scope

Repository HEAD at implementation baseline:

```text
c865cb3a39b70a451915140804b15e2f9439ef3d
```

Modified upstream-derived file:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/DoToClosestBlockTask.java
```

Baseline Git blob:

```text
f4e3f9b2874ba52d7d0c7e3e3be50050802f21fd
```

Current file SHA-256 at static verification:

```text
064C897E63C5AB3D66B344001BA0B10E9898E51E853A1256664E96348685DDE0
```

Modified method:

```text
DoToClosestBlockTask#getClosestTo(AltoClef, Vec3d)
```

Divergence category: bounded diagnostics-only observation.

Exact hunk:

```text
around the existing predicate-aware getNearestBlock(...) call only:
    beginFilteredSearchObservation(this)
    existing getNearestBlock(...) call, still exactly once
    endFilteredSearchObservation(this, completedNormally, targetBlocks) in finally
```

Activation boundary:

```text
requestSource == BARE_DEPOSIT_ALL_COMMAND
current parent branch == OPEN_EXISTING
observed task identity == the current direct ROOT_ROUTE child
diagnostics mode permits BOUNDARY observation
```

The LAVI-owned collector remains inactive for ordinary `@deposit`, unrelated
`DoToClosestBlockTask` instances, and resource descendants such as wood search.
It aggregates only outcomes already produced by the copied
`DepositAllTask.validContainer` predicate. It does not log each candidate.

Behavior preserved by inspection:

```text
scanner invocation count and arguments
scanner return value
predicate evaluation order and short-circuit positions
exception propagation
Task selection, equality, replacement, completion, and ownership
retry, timeout, cooldown, fallback, and cleanup
Baritone goals, paths, process ownership, and cancellation
input, screen, container click, cursor, slot, and transfer behavior
```

Build and runtime evidence:

```text
run id: 20260826-012347
command: .\gradlew.bat clean build --rerun-tasks --no-build-cache --no-daemon --stacktrace --offline
result: BUILD SUCCESSFUL in 2m 51s
Gradle exit code: 0
tasks: 171 actionable tasks, 171 executed
1.20.1 compileJava, compileTestJava, test, remapJar, and build: executed successfully
1.20.1 JAR: versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar
JAR bytes: 6528692
JAR SHA-256: 5BEB70FDD6D1BFD62D32BE968A00A440FE4A7C5EE12B64F1470493821954F55E
non-fatal warning: IdeaWin64.dll native filesystem initialization was unavailable
final outcome: BUILD_PASSED_RUNTIME_NOT_VERIFIED
Minecraft reproduction not run
```

Rollback unit:

```text
remove only the begin/end diagnostic scope and local completion flag from
DoToClosestBlockTask#getClosestTo(); remove the matching LAVI-owned collector
entry points only if the deposit-all diagnostic slice is rolled back as a unit
do not revert unrelated diagnostics or use broad Git restoration
```

No behavior-changing fix, build, deployment, reproduction, commit, or push is
authorized by this record.

### 2026-08-26 D7 incomplete-scan extension

The same existing `finally` hunk now passes the already available `targetBlocks`
array to the LAVI-owned observer. When the existing scanner call does not return
normally, the observer emits one bounded `FILTERED_SCAN_DID_NOT_COMPLETE`
diagnostic from evidence already collected before the failure.

This extension does not add `catch`, suppress or convert the scanner exception,
repeat the scanner call, invoke the predicate again, or change the scanner return
value. The earlier `20260826-012347` evidence predates this extension.

Subsequent separately authorized build and runtime evidence:

```text
run id: 20260826-023409
command: .\gradlew.bat clean build --rerun-tasks --no-build-cache --no-daemon --stacktrace --offline
result: BUILD SUCCESSFUL in 2m 7s
Gradle exit code: 0
tasks: 171 actionable tasks, 171 executed
1.20.1 JAR bytes: 6558376
1.20.1 source JAR SHA-256: 76876A5AC793BB3090F6AE8FA7CDCC6122FB447D751CB530B62541E0896DC72C
deployed JAR count: 1
deployed JAR SHA-256: 76876A5AC793BB3090F6AE8FA7CDCC6122FB447D751CB530B62541E0896DC72C
Minecraft launch: 2026-08-26 02:38:27 +09:00
loaded mod: altoclef 1.20.1-0.18.23
relevant Mixin, injection, descriptor or linkage failure: none observed
new crash report: none
```

The bounded observer produced an exact D7 result for
`storeOperationId=store-deposit-479`: the raw candidate at `-645,51,147`
was visited and rejected as `CHEST_ABOVE_BLOCKED_UNBREAKABLE`, while the
filtered scan selected `-527,52,125`. D9 checkpoints correlated 232 raw
50-block crossings with 232 parent branch changes and zero transfer decisions.
These observations proved a parent/child candidate inconsistency without
changing scanner, predicate, Task, input, path, interaction, or cleanup behavior.

Current status:

```text
DIAGNOSTICS_BUILD_PASSED_DEPLOYED_HASH_MATCHED_RUNTIME_OBSERVED
```

No behavior-changing fix, additional build, commit, or push was performed by
the documentation update that recorded this evidence.

### 2026-08-26 commit packaging clarification

The preceding no-commit statement describes the state at the time that specific
documentation update was made. The diagnostics hunk was later included in:

```text
9785e17b feat(minecraft): add stable deposit_all container targeting
54 files changed, +6878 / -198
remote state at clarification: HEAD == origin/minecraft-plugin-fix/alto-clef-infinite-loop
```

That commit also contains behavior, LAVI-owned diagnostics, tests, and documents,
so the Git commit is not the atomic rollback unit for this upstream divergence.
The hunk-level `Rollback unit` procedure above remains authoritative: remove
only the bounded `DoToClosestBlockTask#getClosestTo()` observer scope and
its matching LAVI-owned entry points when that diagnostic slice is intentionally
retired. Do not use a broad commit revert to remove this one upstream hunk.

This clarification changes provenance only. It does not reclassify the hunk as
behavior-changing and does not authorize source edits, build, deployment,
runtime reproduction, commit, or push.

## 2026-08-31 Automatic-Deposit Post-Placement One-Tick Handoff

<!-- 20260831_openai: Conservatively recorded the automatic-deposit-only post-place handoff because DepositAllTask is a LAVI-created behavior-preserving copy in the upstream engine namespace. -->

Pre-change repository baseline:

```text
HEAD:
  14ba9b443f0bc11d6860a25d7fd3b8b916d95a04

worktree:
  DIRTY - preserve all pre-existing user source and diagnostics changes

DepositAllTask.java pre-change SHA-256:
  26A7E7B1FD2470DBAE5454CE9444DC8DD56AAF34CA3F0E213501CE198F9F15AA
```

Modified behavior-owning file:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/
  adris/altoclef/tasks/container/DepositAllTask.java
```

Provenance classification:

```text
introduced by:
  9785e17b2a9bd87839e33436c2b72f416cce5dd8
  feat(minecraft): add stable deposit_all container targeting

subsequent file commits:
  673d21de1551362760bba1f12e98fedfd06f5e8b
  696265deb3159c7b02e743dd73f3f3c77201af53

ownership:
  LAVI-created integration file

ancestry:
  introduced as a behavior-preserving copy of the store-in-any-container flow

classification:
  conservative behavior-changing engine/integration divergence
```

`DepositAllTask` was created by LAVI, but its package and behavior ancestry are
inside the preserved `adris.altoclef` engine surface. This record therefore does
not use LAVI ownership to hide a lifecycle behavior change. Exact upstream
comparison remains `UNVERIFIED`.

Verified failing boundary:

```text
reproduction:
  auto-deposit-carryon-chest-20260830-232646

game tick:
  998

runtime transition:
  actual PlaceBlockNearbyTask stop/replacement
  -> StoreInContainerTask
  -> InteractWithBlockTask in the same client-tick task path

result:
  interaction SUCCESS without GUI
  -> at tick 999 Carry On NOT_CARRYING -> CARRYING
  -> clicked chest -> air / targetRemoved=true
  -> automatic deposit OPEN_EXISTING -> OBTAIN_CHEST
```

Installed Carry On 2.1.2.7 bytecode establishes that unbound-key reconciliation
runs at `ClientTickEvents.END_CLIENT_TICK`, its false transition sends
`ServerboundCarryKeyPressedPacket(false)`, and block pickup rejects
`keyPressed=false`. ChatClef's task runner executes from the
`MinecraftClient.tick` HEAD path. The runtime log did not directly record the
packet; successful pickup requiring `keyPressed=true` is a necessary-condition
deduction.

Exact behavior hunk:

```text
DepositAllTask public construction seam:
  DepositAllTask(boolean,
                 DepositAllPlacementTaskOwner,
                 DepositAllPostPlaceHandoff,
                 ItemTarget...)

DepositAllTask#onTick():
  inspect the retained actual PlaceBlockNearbyTask once through
  deferAfterCompletedPlacement()

  when DepositAllPostPlaceHandoff.shouldDefer(...) accepts the completed active
  placement transition:
    clear the placement owner
    return null exactly once

  obtain the placement fallback child through
  DepositAllPlacementTaskOwner#getOrCreate(...)

existing constructors:
  ephemeral placement ownership and disabled handoff

injectable constructor invariant:
  only ephemeral/disabled or retaining/enabled pairs are accepted

only enabled injection point:
  AutoDepositGeneralTaskFactory#create
```

The required marker near the exact behavior hunk is:

```java
//20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
```

Intentionally changed behavior:

```text
automatic general deposit only, after its actual placement child finishes:
  one parent call returns null
  existing Task.tick null-child handling stops and clears that placement child
  no replacement store/open child is ticked in that same parent call
  existing store/open behavior resumes on the next client tick
```

Behavior intentionally left unchanged:

```text
manual @deposit_all constructors and allocation behavior
automatic trusted destination flow
manual @store_home flow
furnace DoStuffInContainerTask handoff
PlaceBlockNearbyTask completion and cleanup code
StoreInContainerTask and AbstractDoToStorageContainerTask
InteractWithBlockTask and PlayerInteractionFixChain
Task and TaskRunner scheduling implementation
Carry On dependency, settings, key binding, and state
retry, timeout, terminal classification, and fallback policy
generic GUI success predicates
global input ownership and cleanup
Baritone goal, path, process, and cancellation ownership
wire protocol and diagnostics defaults
```

LAVI-owned containment files:

```text
lavi/minecraft/task/container/deposit/handoff/
  DepositAllPlacementTaskOwner.java
  DepositAllPostPlaceHandoff.java

lavi/minecraft/task/container/deposit/auto/maintenance/child/
  AutoDepositGeneralTaskFactory.java
```

The remaining automatic-maintenance extractions recorded in the pre-change
report are LAVI-owned responsibility separation, not additional engine
divergences. They must preserve the existing maintenance phase, outcome,
manifest, recovery, relief, and diagnostics behavior.

Ownership impact:

```text
placement identity:
  per-DepositAllTask owner, no static state

completion:
  unchanged PlaceBlockNearbyTask predicate

one-tick deferral:
  per-DepositAllTask handoff, enabled only by the automatic general factory

input acquisition/release:
  unchanged PlaceBlockNearbyTask ownership and onStop cleanup

retry / timeout / terminal:
  unchanged; the handoff owns none

custom goal / path:
  none added or cancelled

interruption:
  unchanged Task.tick null-child path

global state:
  none
```

Regression scope required before a runtime-success claim:

```text
retained actual placement identity versus fresh equal candidates
one-shot null barrier and stop exactly once
no open child in the barrier call; normal open child on the next call
manual @deposit_all unchanged
automatic existing-container path unchanged
trusted/store_home paths unchanged
furnace path unchanged
Carry On absent class loading
generic right-click and container opening unchanged
Baritone ownership unchanged
parent interruption/resume does not lose the retained actual child identity
```

Verification status when this entry was written:

```text
source implementation verification:
  NOT PERFORMED BY THIS DOCUMENTATION PASS

unit tests:
  NOT RUN

clean forced Gradle build:
  NOT RUN / NOT AUTHORIZED

JAR deployment and hash verification:
  NOT RUN / NOT AUTHORIZED

Minecraft runtime reproduction:
  NOT RUN / NOT AUTHORIZED

commit and push:
  NOT PERFORMED / NOT AUTHORIZED
```

Rollback unit:

```text
remove only the automatic-enabled constructor injection from
  AutoDepositGeneralTaskFactory

remove only the completed-placement one-shot null branch and owner-backed
  placement creation from DepositAllTask

remove the two handoff helpers only when no remaining caller uses them

preserve all pre-existing dirty diagnostics hunks and unrelated LAVI-owned
  maintenance refactoring
```

Do not roll this divergence back with `git reset`, `git checkout --`,
`git restore`, file replacement, or a broad commit revert.

The authoritative evidence, ownership ledger, exact proposed file set, test
matrix, and remaining gaps are in
[ChatClef Automatic Deposit Post-Place Handoff Pre-Change Report](chatclef-auto-deposit-post-place-handoff-pre-change-report-2026-08-31.md).

### 2026-08-31 source application update

The automatic-general-only handoff and its LAVI-owned responsibility separation
were subsequently applied. Static source review found one enabled production
injection point, no Carry On or global input/Baritone dependency in the changed
scope, no missing required source marker, no trailing whitespace, and no missing
final newline. The injectable constructor also rejects mismatched ownership and
handoff policies.

Focused tests were added for placement identity, exact-once deferral, actual
Task reconciliation ordering, automatic-only composition, manual defaults, and
free-slot verdicts. At that source-application checkpoint they were not run
because Gradle/build execution had not been authorized. Clean build, deployment,
runtime reproduction, commit, and push were all unperformed at that checkpoint.
The subsequent separately authorized verification is recorded below.

### 2026-08-31 post-implementation verification update

<!-- 20260831_openai: Recorded subsequent clean-build, deployment-hash, and captured-runtime evidence for the automatic post-place handoff divergence. -->

This update supersedes only the current verification status. It does not rewrite
the pre-change authorization boundary or the `when this entry was written`
status above.

```text
clean forced command:
  .\gradlew.bat clean build --rerun-tasks

clean forced build:
  PASSED
  BUILD SUCCESSFUL in 4m 43s
  exit code 0
  171 actionable tasks; 171 executed

Minecraft 1.20.1 tests:
  PASSED_WITH_ONE_SKIPPED_NON_HANDOFF_TEST
  117 suites
  378 total; 377 passed; 0 failures; 0 errors; 1 skipped

focused owner / handoff / lifecycle suites:
  12 passed; 0 failures; 0 errors; 0 skipped

automatic-general factory and free-slot verifier suites:
  5 passed; 0 failures; 0 errors; 0 skipped

built and active JAR bytes:
  7,360,897

built and active JAR SHA-256:
  84C6634433D7402B2935ADD6E4028F43BD3782D2E4038E00D30206092E9CA839

artifact deployment:
  VERIFIED - built and active instance hashes match

Minecraft runtime reproduction:
  OBSERVED

post-place handoff symptom:
  VERIFIED_FOR_THE_CAPTURED_SCENARIO_ONLY

all worlds, configurations, and container combinations:
  NOT CLAIMED

bounded diagnostics compliance:
  NOT VERIFIED - separate high-volume and terminal-reserve findings remain

commit and push:
  NOT PERFORMED
```

The active session loaded the expected instance JAR and emitted diagnostics
source marker `20260731_post_place_handoff_p2`. The strongest Store-correlated
automatic-general runtime sequence was `auto-deposit-1` /
`store-deposit-379`:

```text
tick 291  actual PlaceBlockNearbyTask STOP_BEGIN / STOP_END and child clear
tick 292  next-tick OPEN_EXISTING and chest interaction SUCCESS
tick 293  GUI_OPEN_DELAYED; handler changed; transfer began
tick 299  transfer terminal
tick 341  MAINTENANCE_LOGICAL_TERMINAL / free_slot_postcondition_observed
```

`auto-deposit-5` repeated the cleanup-only handoff, next-tick open, GUI,
transfer, maintenance terminal, and later user-task natural completion. Its
adjacent chest-to-air hand snapshots were not independently Store-bound and are
treated only as supporting evidence.

Carry On 2.1.2.7 remained `AVAILABLE_NOT_CARRYING` across that handoff and no
pickup transition was observed. This verifies that the added one-tick boundary
worked in the captured scenario. It does not prove every manual, trusted,
Carry-On-absent, or alternative-container path.

One earlier operation in the same session emitted a Baritone
`BlockOptionalMeta.getManager/drops` resource-reload exception, after which the
operation and session continued to completion. The exception was non-terminal in
this capture; this verification does not classify it as fixed. It also does not
close the separate diagnostics-volume issue: the session
did not emit `DIAGNOSTIC_SESSION_CAP_REACHED`, emitted high-volume tool-selection
events, and exhausted the per-store terminal-group reserve.

The detailed build, artifact, tick, terminal, and limitation ledger is recorded
in [ChatClef Automatic Deposit Post-Place Handoff Pre-Change Report](chatclef-auto-deposit-post-place-handoff-pre-change-report-2026-08-31.md#16-post-implementation-verification-update).

## 2026-09-03 Exact-Container GUI Three-Later-Tick Gate

<!-- 20260903_kpopmodder: Recorded the exact-container GUI gate and its minimum upstream seams. -->

### Implementation baseline and verification state

```text
repository HEAD used as the working-tree comparison baseline:
  39ea27c83c6e5808535d68aef5a8b4c33548f002

worktree at record time:
  DIRTY - pre-existing user and concurrent agent changes preserved

exact upstream repository, tag, and commit:
  UNVERIFIED

source state:
  PRESENT_IN_WORKTREE

focused tests:
  PASSED - 61 total / 61 passed / 0 skipped / 0 failed / 0 errors

full Fabric 1.20.1 tests:
  PASSED - 723 total / 722 passed / 1 skipped / 0 failed / 0 errors

clean forced Gradle build:
  PASSED - .\gradlew.bat clean build --rerun-tasks
  3m 49s / 171 tasks executed / Gradle 8.8
  Eclipse Temurin JDK 21.0.12+8-LTS

canonical remapped runtime JAR:
  C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar
  size: 8,132,655 bytes
  mtime: 2026-09-03 18:57:31 +09:00
  SHA-256: A526C8FFD2A7ED0E78018D3180FE195AA280F1CB364EDDFEA28BCD12E268055D

artifact verification status:
  BUILD_PASSED_RUNTIME_NOT_VERIFIED

JAR deployment:
  NOT_RUN

Minecraft runtime reproduction:
  NOT_RUN

runtime Mixin injection verification:
  NOT_RUN

commit and push:
  NOT_PERFORMED
```

The focused suite, full Fabric 1.20.1 suite, and canonical clean forced build
prove source compilation and automated-test compatibility for this worktree.
Inspection of the unclassified remapped runtime JAR also found
`ClientTickMixin.class`, `ExactContainerGuiGate.class`,
`ExactContainerGuiAttemptState.class`, and `fabric.mod.json` in the artifact.
They do not prove deployment, runtime Mixin injection, or live Minecraft
behavior. The resulting status is `BUILD_PASSED_RUNTIME_NOT_VERIFIED`; those
runtime stages remain `NOT_RUN`.

### Classified minimum generic engine seams

The following files are upstream-derived engine surfaces or new generic seam
types placed in the upstream engine namespace. Their default behavior is
preserved for callers that do not opt into the exact GUI gate.

| File and baseline Git blob | Exact seam | Behavior-preserving default |
| --- | --- | --- |
| `adris/altoclef/control/InputControls.java` at `d6e30cbf5b75df03cc0c0154355af186eb9e933b` | `tryPressAndReportAccepted(Input)` reports whether the existing press request passed `_waitForRelease`; the existing `void tryPress(Input)` delegates to it. | Existing callers keep the void API and the same press, auto-release, and suppression effects. |
| `adris/altoclef/tasks/InteractWithBlockTask.java` at `8621e1ddcd54b6da983060d447606cf4bb1ba1a9` plus new `adris/altoclef/tasks/interaction/InteractWithBlockLifecycleObserver.java` | One observer can be supplied by composition. It is notified only after a newly accepted interaction input remains held. The stop hook can decide whether this exact child may run the legacy global cleanup. | `InteractWithBlockLifecycleObserver.NONE` does nothing and returns `false`; ordinary tasks therefore retain the existing `forceCancel()` and SNEAK-release stop path. No engine subclass was added. |
| `adris/altoclef/tasks/AbstractDoToClosestObjectTask.java` at `1498fd0dbbed3b5b6dc1a8feae0571c5a060721e` plus new `adris/altoclef/tasksystem/ParentTaskRetention.java` | An already selected child that implements the narrow predicate can request identity retention before closest-object scanning, heuristic work, goal replacement, or wandering. | A null child or any child not implementing the interface returns `false`; ordinary closest-object evaluation is unchanged. |
| `adris/altoclef/tasksystem/Task.java` at `6842ced52ae86d168c09962e59f438a60c1e8b76` | `getRetainedChildForParentEvaluation()` exposes the already reconciled `sub` only when that same child implements `ParentTaskRetention` and currently requests retention. | It is a read-only protected query. It does not start, stop, replace, or tick a child, and returns `null` for every non-participating child; scheduler reconciliation remains unchanged. |
| `adris/altoclef/tasks/ResourceTask.java` at `bbf31d83dae014f053f446653e545a876fc059d7` | Propagates an exact-furnace descendant's retention before cursor, screen, scanner, fallback, or replacement work, and exposes that active descendant to its own parent through the same predicate. | With no retained descendant, the original resource acquisition and fallback evaluation runs unchanged. |
| `adris/altoclef/tasks/speedrun/beatgame/BeatMinecraftTask.java` at `948dfda062ede2df9eae96c7a3f6b84a46e235bb` | Propagates exact-furnace descendant retention before speedrun policy can equip items, alter Baritone settings, close screens, or replace the route. | With no retained descendant, the original speedrun evaluation and policy mutations run unchanged. |
| `adris/altoclef/tasks/container/AbstractDoToStorageContainerTask.java` at `a1f7067fdf1b83c4ff101f50023535485495f80f` | `isContainerOpenForTarget(...)` and `createContainerOpenTask(...)` are overridable seams at the existing open check and open-child construction points. | The defaults call the original broad `ContainerType.screenHandlerMatches(...)` predicate and create the original `new InteractWithBlockTask(targetPos)`. |
| `adris/altoclef/tasks/container/DoStuffInContainerTask.java` at `38dde0ec21baa1e6d566aff16379db9d0b3794c8` | The existing closest-block opener uses `this::createContainerOpenTask`; the new factory can be overridden by the regular-furnace owner. | The default factory returns the same `new InteractWithBlockTask(targetPos)`. Smoker, blast-furnace, and other subclasses do not opt in. This hunk is distinct from the previously recorded post-place handoff in the same file. |
| `adris/altoclef/mixins/ClientTickMixin.java` at `622e72e0cff130f98fd6ba794f473fe0fa5b4d62` | A per-`MinecraftClient` serial opens at `tick` HEAD, and one boundary event is claimed and published at `tick` RETURN before the serial is closed. The Mixin implements the narrow LAVI-owned `GuiTickRuntimeAccess`. | The existing `ChatClefDiagnostics.onClientTickHead()` and `ClientTickEvent` publication remain in their existing HEAD path. The new RETURN callback performs gate bookkeeping and listener publication only; it does not click, press input, close a screen, select a Task, or mutate a Baritone goal/path. |

The required divergence marker is present at the generic hunk boundaries:

```java
//20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
```

### Route-owner and ancestor integration

The generic seams alone do not delay any container action. The following
behavior-owning files opt the enumerated in-scope routes into the gate or preserve their child
identity through the reachable parent chain:

| File and baseline Git blob | Integration responsibility |
| --- | --- |
| `adris/altoclef/tasks/container/StoreInContainerTask.java` at `3556435f056382d112c33fe73c27cb140d9d5d2f` | Owns the `GENERAL_STORAGE` gate, selects the exact open wrapper only for chest/trapped chest, suppresses parent mutation, performs full revalidation, and commits one permission before returning the existing `MoveItemToSlotFromInventoryTask`. Barrel and shulker targets use the base factory and legacy flow. |
| `adris/altoclef/tasks/container/SmeltInFurnaceTask.java` at `4add6a0a0cbbd447d99c56facb6de9abeca83fd6` | Owns the `REGULAR_FURNACE` gate, retains the inner container Task before `ResourceTask` fallback, validates material/fuel/accessibility before permission consumption, and enters the existing PICKUP-based furnace flow. Smoker and blast furnace are not changed through this route. |
| `adris/altoclef/tasks/container/StoreInAnyContainerTask.java` at `abd6d7c3ed74bb627cce9451bf8ca09f0a09ebda` and `StoreInStashTask.java` at `f8fe4ebe120bf8d738d6bc2d1673febb56754ae7` | Cache the actual closest-container route, exact `StoreInContainerTask` leaf, and pinned target, then return that same route identity before acquisition, scanner, progress, placement, travel, or fallback work. The references survive a normal `Task.interrupt` because the scheduler preserves its reconciled child; they are cleared only when normal evaluation selects a replacement branch. Stash recreates the leaf only after recomputing remaining work and detecting a target or snapshot change, while the any-container route preserves its original callback-captured work semantics. |
| `adris/altoclef/tasks/container/DepositAllTask.java` at `e8a121de9aa5e6bc9c7ab667299d0de3d01c7ab2` | Propagates retention from its stable generated store child before scanner, progress, placement, or fallback work. This is LAVI-created code in the engine namespace and remains conservatively classified as an engine/integration divergence. |

LAVI-owned automatic-deposit parents propagate the same narrow retention
predicate. `AutoDepositTrustedStoreTask` now returns its sole
`StoreInContainerTask` child and no longer owns a duplicate ungated
`InteractWithBlockTask`. `AutoDepositMaintenanceTask` returns a protected
trusted or general child before context and phase evaluation. These LAVI-owned
changes are containment code, not additional upstream engine seams.

### Exact ordering and behavior change

The intentional behavior change is limited to the enumerated in-scope exact routes:

```text
normal world-open input accepted during active client tick
  -> exactly one same-target BlockInteractEvent in that same active serial
  -> raw ScreenOpenEvent TAIL observation
  -> only a serial-proven accepted candidate is candidate K and is consumed once
  -> open wrapper and nested interaction child stop
  -> operation-owned input/goal/path resources prove quiescent
  -> live exact binding revalidated and promoted at client RETURN boundary B
  -> count three distinct RETURN boundaries strictly later than B
  -> later boundary #3 sets GUI_INPUT_ALLOWED only
  -> next normal Task evaluation revalidates the complete binding
  -> one attempt-scoped permission is committed before existing transfer work
```

`K` and `B` are different concepts. A raw TAIL observation made while no active
client-tick serial exists has no candidate serial, is not `K`, and must not be
retained, replayed, or backfilled into a later tick. An accepted candidate `K`
and `B` may share a serial when cleanup is already complete by that tick's
RETURN, but candidate capture never counts as a later boundary. `B` initializes
`boundClientTickSerial` and
`lastCountedBoundarySerial`; only strictly greater, non-duplicate serials can
advance the count.

No slot click, cursor mutation, second world interaction, physical SNEAK input,
screen close/reopen, Task selection, retry, fallback, or Baritone goal/path
mutation is performed by `ScreenOpenEvent`, `BlockInteractEvent`, or client
RETURN callbacks. The third later boundary grants permission only.

### Correlation, cleanup, and fail-closed boundaries

- The accepted-input seam creates one operation-local attempt with immutable
  operation, open-attempt, and correlation identifiers.
- The matching block event must use the pinned target and accepted-input client
  serial. A target mismatch, serial mismatch, or second matching event
  invalidates the attempt.
- The first accepted or rejected TAIL candidate permanently spends the matching
  interaction. A retry requires a new attempt/correlation identity.
- The exact wrapper composes the existing `InteractWithBlockTask`; it does not
  subclass it or override engine-wide Task lifecycle.
- The exact stop hook suppresses the legacy global `forceCancel`/SNEAK cleanup
  only for the exact wrapper's owned interaction. Promotion still requires the
  wrapper and nested interaction to be stopped/inactive and CLICK_RIGHT,
  SNEAK, portal-forward, custom-goal, and pathing state to be quiescent.
- World object, dimension, pinned target position/block identity, screen object
  and exact class, handled-screen handler/player-handler identity, captured
  handler identity, and sync id are revalidated fail-closed.
- Invalidated pre-transfer attempts may be retired only through the owning
  parent's explicit retry path. Each attempt retains its own bounded
  open/quiescence wait.
- A consumed permission is never reused. If the complete live binding becomes
  invalid during `TRANSFER_ACTIVE`, the owner retires the entire old attempt
  and starts a wholly fresh attempt, correlation identity, and retry generation
  against the same pinned target after recomputing remaining work. This permits
  recovery without turning the invalidated committed attempt into a permanent
  no-op loop.

### Existing behavior preserved outside the enumerated in-scope routes

```text
one initial normal right-click world interaction
trusted-home transfer: button 0 + QUICK_MOVE
general-storage transfer: existing PICKUP button 0/1 contract
regular-furnace transfer: existing PICKUP call-site contracts
barrel and shulker: legacy general-storage/trusted-home open path
smoker and blast furnace: legacy DoStuffInContainerTask path
ordinary InteractWithBlockTask observer and stop cleanup
ordinary AbstractDoToClosestObjectTask reevaluation
TaskRunner selection and scheduling
Carry On optionality and class loading
global input, goal, path, and cleanup ownership
```

The gate is a post-GUI-open stability mechanism. It does not claim to fix a
stale Carry On key state that consumes the initial world interaction before any
GUI opens.

### Regression scope and rollback unit

Focused source and state-machine tests cover serial identity,
same-boundary suppression, exact event consumption, TAIL rejection,
open-child quiescence, route allowlists, parent retention, one-time permission,
regular-furnace preconditions, trusted-home route pinning, and absence of the
duplicate automatic trusted open owner. The focused run passed all 61 tests.
The full Fabric 1.20.1 run passed 722 of 723 tests with one skipped test
and no failures or errors. The canonical
`.\gradlew.bat clean build --rerun-tasks` run also passed in 3m 49s with all 171
tasks executed under Gradle 8.8 and Eclipse Temurin JDK 21.0.12+8-LTS.

The unclassified remapped runtime JAR was recorded at
`C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar`
with size 8,132,655 bytes, modification time
`2026-09-03 18:57:31 +09:00`, and SHA-256
`A526C8FFD2A7ED0E78018D3180FE195AA280F1CB364EDDFEA28BCD12E268055D`.
Artifact inspection found `ClientTickMixin.class`,
`ExactContainerGuiGate.class`, `ExactContainerGuiAttemptState.class`, and
`fabric.mod.json`. Deployment, Minecraft launch, live-world reproduction, and
runtime Mixin injection verification were not run, so the status remains
`BUILD_PASSED_RUNTIME_NOT_VERIFIED`, not runtime acceptance.

Rollback must be hunk-scoped and dependency ordered:

1. Disconnect `GENERAL_STORAGE`, `REGULAR_FURNACE`, trusted-home, and ancestor
   retention callers from the LAVI-owned exact GUI collaborators.
2. Revert only the accepted-result, observer, parent-retention, factory, and
   client-RETURN seam hunks listed above.
3. Remove the two new generic seam interfaces only after no caller references
   them.
4. Remove the LAVI-owned exact GUI packages and tests only as the same feature
   rollback unit.

Do not revert the whole files, the prior diagnostics, the existing post-place
handoff, or unrelated dirty work. Do not use `git reset`, broad checkout,
restore, cleanup, or a broad commit revert.

The complete LAVI-owned file, route, state, test, and verification inventory is
recorded in
[ChatClef Exact-Container GUI Three-Tick Implementation Ledger](chatclef-exact-container-gui-three-tick-implementation-ledger-2026-09-03.md).

### Post-Build Runtime Reproduction Update

<!-- 20260903_kpopmodder: Recorded the matching-JAR furnace failure and diagnostics-only evidence plan without adding a behavior divergence. -->
<!-- 20260903_kpopmodder: Corrected runtime counts, evidence strength, and the exact source-to-gate observation boundary after immutable-log re-review. -->

The `BUILD_PASSED_RUNTIME_NOT_VERIFIED` statements above are the historical
implementation-time checkpoint. A later run deployed the same 8,132,655-byte
JAR to the active `LAVI_TEST_Fabric01` instance; the built and active files both
had SHA-256
`A526C8FFD2A7ED0E78018D3180FE195AA280F1CB364EDDFEA28BCD12E268055D`.

```text
CURRENT_RUNTIME_STATUS: FAILED_REPRODUCED_FIRST_FAILING_BOUNDARY_DIRECT_OBSERVATION_PENDING
CURRENT_CAUSE_CLASSIFICATION: LEADING_SOURCE_AND_LOG_HYPOTHESIS
```

The regular-furnace route then failed in live Minecraft. The logs recorded 165
exact-GUI open requests and 165 matching block interactions. Generic screen
diagnostics recorded 165 delayed GUI observations in total: 164 furnace
`class_3858` observations and one unrelated crafting-table `class_1714`
observation. Before the ordinary diagnostic ceiling, they recorded zero
admitted `EXACT_GUI_SCREEN_TAIL_CANDIDATE`, `EXACT_GUI_BOUND`, permission, or
transfer-commit events. There were 164
`gui_open_or_quiescence_timeout` invalidations, 164 open-child cleanup-skip
records, and 164 parent retry authorizations before the ordinary diagnostic
event ceiling was reached while the critical reserve remained available.

No generic slot-click boundary record appears in this BOUNDARY-mode capture,
but `ChatClefDiagnostics.logSlotClick(...)` is VERBOSE-only. Post-ceiling
exact-gate detail is also unobserved. Therefore this evidence proves only that
no exact-gate transfer commit or gated transfer entry was admitted in the
pre-ceiling evidence window; it does not prove the whole-run physical
`clickSlot` or transfer-commit count. The user-observed outcome remains that
iron smelting did not proceed.

The last proven forward-progress boundary is the matching block interaction.
The first missing required boundary is at the expected `ClientOpenScreenMixin`
TAIL producer or somewhere between that producer and exact-gate intake; current
logs do not prove that TAIL publication itself ran. Source inspection shows
that `GuiContainerEventHub.onScreen()` silently returns when no active
client-tick serial exists, so an inter-tick TAIL drop is the leading source-and-
log hypothesis. It is not a directly logged root cause: the TAIL source log is
VERBOSE-only and the hub early return has no decision record.

The next in-scope source work is diagnostics-only. It will observe the TAIL
source, hub disposition, and gate/coordinator outcome through bounded LAVI-owned
diagnostic collaborators while deliberately preserving the current event
delivery/drop result, serial lifecycle, listener order, retry, timeout, Task,
slot, input, screen, and Baritone behavior. No new behavior-changing engine
divergence has been applied by this documentation update. Once those observations
identify the boundary, any exact-contract correction proceeds under the active
continuous implementation, bounded-log, test, and clean-build workflow without
a separate phase-approval pause.

Incident counts, evidence paths, ownership, future files, tests, and the
one-reproduction decision matrix are in
[Section 18 of the implementation ledger](chatclef-exact-container-gui-three-tick-implementation-ledger-2026-09-03.md#18-post-build-furnace-runtime-reproduction-and-diagnostic-reinforcement-plan).
Canonical event/field/boundedness rules are in
[Screen TAIL Source-To-Hub-To-Coordinator Diagnostic Contract](chatclef-task-lifecycle-diagnostics.md#screen-tail-source-to-hub-to-coordinator-diagnostic-contract).

## 2026-09-04 Bounded Container-GUI Diagnostics-Only Reinforcement

<!-- 20260904_kpopmodder: Recorded the bounded container-GUI diagnostic divergence and matching live-runtime evidence without claiming a behavior fix. -->

Review baseline SHA: `46dd942ed504e6d0209c2fc57e6336c2e10ac950`

Review branch: `minecraft-plugin-fix/alto-clef-infinite-loop`

```text
CURRENT_DIAGNOSTIC_STATUS: BUILD_PASSED_RUNTIME_OBSERVED
CURRENT_ARTIFACT_STATUS: BUILD_AND_ACTIVE_INSTANCE_BYTE_IDENTICAL
CURRENT_BEHAVIOR_STATUS: EXACT_GUI_GATE_NOT_PRESENT_IN_ACTIVE_SOURCE_OR_CURRENT_JAR
DIVERGENCE_CLASSIFICATION: DIAGNOSTICS_ONLY
```

This working-tree change adds bounded observation of the existing container
screen, Task, slot-action, local-delta, server-reconciliation, and root-terminal
flow. It does not add, restore, or repair the exact-container GUI gate.

At this baseline, source, Git-tree, and remapped-JAR inspection found no active
`ExactContainerGuiGate`, `GuiContainerEventHub`, `GuiClientTickSerialState`,
`ExactContainerGuiOpenTask`, or exact-route gate integration. The earlier
8,132,655-byte / `A526C8...` artifact record is a historical transient artifact
checkpoint and must not be treated as the current source or artifact identity.

### Scope and ownership

The atomic runtime change contains exactly 66 paths:

| Ownership | Existing modified | New | Total |
| --- | ---: | ---: | ---: |
| Upstream/engine-namespace Java | 7 | 1 diagnostic Mixin | 8 |
| LAVI-owned main Java | 5 | 42 | 47 |
| Mixin resource configuration | 1 | 0 | 1 |
| LAVI-owned tests/support | 0 | 10 | 10 |
| Total | 13 | 53 | 66 |

The 42 new LAVI-owned production files are responsibility-separated below
`lavi/minecraft/diagnostics/container/gui/` into facade, budget, correlation,
dispatch, emission, lifecycle, runtime, screen, slot, task, and tick packages.
The ten new test paths contain nine JUnit test classes and one shared immutable
test-snapshot support file.

### Exact engine-namespace observation hunks

| File | Exact method or seam | Observation and preserved behavior |
| --- | --- | --- |
| `adris/altoclef/chains/UserTaskChain.java` | `onTaskFinish(AltoClef)` | Observes root assignment and terminal decision after `actuallyDone` is computed. It does not change callback execution, `TaskFinishedEvent`, idle selection, input cleanup, or Baritone cancellation. |
| `adris/altoclef/control/SlotHandler.java` | `clickWindowSlot(int,int,SlotActionType)` | Opens a method-local probe around the existing controller call. Handler, sync ID, slot, button, action type, call, and exception policy are unchanged. |
| `adris/altoclef/eventbus/EventBus.java` | `publish(T)` | Observes screen-listener eligibility, start, normal return, inactive skip, class-cast failure, and full-loop completion. Listener order, membership, deletion, invocation, and callback-exception propagation remain unchanged. |
| `adris/altoclef/eventbus/Subscription.java` | `diagnosticCallbackClassName()` | Supplies a bounded callback-class label without invoking, retaining, deleting, or reordering the callback. |
| `adris/altoclef/mixins/ClientOpenScreenMixin.java` | `onScreenOpenEnd(...)` | Observes the same TAIL `ScreenOpenEvent` immediately before its existing publication without retaining, replaying, suppressing, or reordering it. |
| `adris/altoclef/mixins/ClientTickMixin.java` | `clientTick(...)`, `clientTickReturn(...)` | Records HEAD, the existing published boundary, and RETURN. It creates no permission and changes no Task, input, goal, path, or world interaction. |
| `adris/altoclef/mixins/SlotClickMixin.java` | redirected `slotClick(...)` | Observes a local slot mutation only after the existing before/after comparison detects it. Mutation and event behavior are unchanged. |
| new `adris/altoclef/mixins/diagnostics/ClientScreenHandlerUpdateDiagnosticMixin.java` | two post-apply S2C injections | Observes slot/full-inventory reconciliation after Minecraft applies it. It does not modify, cancel, replace, acknowledge, or synthesize a packet. Exact descriptors use `require=1`, `allow=1` for the supported 1.20.1 mapping. |
| `src/main/resources/altoclef.mixins.json` | one client-Mixin registration | Registers the S2C observer once without changing any dependency or version. |

The upstream observation hunks carry the required marker:

```java
//20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
```

The first LAVI-owned diagnostic implementation block carries the required
diagnostics marker. Its historical Carry On wording is retained as project
policy and does not classify this incident as Carry On interception:

```java
//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
```

Existing LAVI-owned integration is limited to observation and composition:

- `BlockInteractionDiagnostics` supplies input-state and interaction snapshots.
- `ChatClefDiagnostics` composes lifecycle, tick, Task, and command-context observation.
- `ContainerTaskDiagnostics` mirrors the already-computed reconciliation result.
- `DiagnosticBoundedEventFormatter` protects required diagnostic identity values.
- `HomeStorageQuickMoveIssuer` observes its existing direct `QUICK_MOVE` call.

Static inspection found no `clickSlot`, `interactBlock`, `setScreen`, input
mutation, Task selection, retry/timeout mutation, or Baritone goal/path mutation
call in the new diagnostic package.

### Build, tests, and artifact evidence

The canonical command completed successfully:

```bat
.\gradlew.bat clean build --rerun-tasks
```

```text
BUILD SUCCESSFUL
171 actionable tasks
171 executed
tests=630
failures=0
errors=0
skipped=1
new container-GUI diagnostic tests=31 passed
```

The remapped build artifact and active deployed artifact are byte-identical:

```text
size: 8,013,409 bytes
modified: 2026-09-04 11:47:44.141 +09:00
SHA-256: EBA0552F33A18AE3A55E4BE5D8CE006E10308EA2A271F55BF2B585EB900B6D93
```

The runtime identity record resolves the code source to the same deployed JAR,
and the active instance contains no duplicate loadable ChatClef JAR.

### Live-runtime evidence and finding

Evidence was read from the active `LAVI_TEST_Fabric01` instance:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\instance_audit.txt
C:\Vtuber_Souorce_Code\LAVI\logs\20260904_153122_log.txt
```

The bridge connected to `ws://127.0.0.1:4316` with generation 1. Five LAVI
commands reached natural completion. The runtime emitted the new TAIL source,
EventBus transport, slot request/return, local delta, root terminal, and applied
server reconciliation observations. No relevant Mixin application or injection
error occurred.

Chest and regular-furnace observations both honestly reported:

```text
operationId=unavailable
openAttemptId=unavailable
correlationId=unavailable
operationContextAvailable=false
activeAttemptPresent=false
gateOutcome=NOT_APPLICABLE
coordinatorOutcome=NOT_CALLED
targetScreenAssociationProven=false
```

The admitted source/transport evidence included five TAIL source records, five
hub decisions, and four normal dispatch-completion records. The missing fifth
completion must not be fabricated or interpreted without its listener path.
No `EXACT_GUI_SCREEN_TAIL_CANDIDATE`, `EXACT_GUI_BOUND`, `GUI_STABILIZING`, or
`GUI_INPUT_ALLOWED` record appeared. Chest `QUICK_MOVE` and furnace `PICKUP`
work proceeded through the current ungated route. Successful command completion
therefore proves preserved legacy behavior, not the three-later-tick contract.

### Boundedness, privacy, and known gaps

The implementation bounds correlation, pending-slot, detail, semantic-bucket,
tick-summary, and UTF-8 payload state. The shared diagnostic session reached its
4936 ordinary-admission ceiling and emitted one
`DIAGNOSTIC_SESSION_CAP_REACHED` marker. At that marker the admitted total was
4954, 46 critical-reserve admissions remained, and the 5000-event hard cap had
not been reached. Terminal summaries were emitted; ordinary detail after the
ordinary ceiling is partial.

The runtime logs contain parsed game command text, world coordinates, session
and correlation UUIDs, and JVM-local object identities. They contain no token,
password, authorization header, or credential introduced by this change. The
runtime log files remained open and growing during review, so immutable evidence
hashes and clean shutdown were not claimed.

Known limitations retained by this diagnostics-only checkpoint:

1. The behavior-owning exact GUI gate is absent from the active source and JAR.
2. Chest/furnace correlation is explicitly heuristic and does not fabricate route-owned operation, attempt, target-screen, or permission identity.
3. Each accepted screen flow owns a finite local limiter, while the shared session cap is the final cross-flow bound; this is not the planned one-operation-across-retries budget.
4. `ChatClefDiagnostics` still uses the existing throwing lifecycle `register(...)` API. Registration succeeded, but the planned nonthrowing `tryRegister` and idempotent unregister path is not implemented or claimed as verified.
5. Callback-exception preservation is source-reviewed but lacks a dedicated dynamic sentinel test.
6. The strict S2C Mixin is verified on the supported 1.20.1 runtime; mapping drift remains a startup risk and its registration/file are one rollback unit.
7. A separate nonfatal `BlockOptionalMeta.getManager -> drops -> getStackHashes` exception occurred during Baritone builder recalculation. It is not attributed to this instrumentation, WebSocket dispatch, or disk world cache.

### Behavior-preservation conclusion

The diagnostic delta does not create or activate an exact attempt, promote
`GUI_BOUND`, count permission boundaries, publish or consume GUI permission,
change Task selection or reconciliation outcome, change retry/timeout/fallback,
change slot parameters, add world interaction or physical input, close or
replace a screen, or change Baritone goal/path/process ownership. Runtime shows
that existing commands continued and the observation hooks loaded. Runtime does
not prove the exact GUI contract because its behavior implementation is absent.

### Hunk-scoped rollback

Rollback is dependency ordered:

1. Disconnect the LAVI-owned calls from `ChatClefDiagnostics`, `BlockInteractionDiagnostics`, `ContainerTaskDiagnostics`, and `HomeStorageQuickMoveIssuer`.
2. Revert only the observation hunks in `UserTaskChain.onTaskFinish`, `SlotHandler.clickWindowSlot`, `EventBus.publish`, `ClientOpenScreenMixin.onScreenOpenEnd`, `ClientTickMixin.clientTick`/`clientTickReturn`, and `SlotClickMixin.slotClick`.
3. Remove `Subscription.diagnosticCallbackClassName()` after `EventBus` no longer calls it.
4. Remove only `diagnostics.ClientScreenHandlerUpdateDiagnosticMixin` from `altoclef.mixins.json`, then remove the new Mixin file.
5. Remove the 42 files under `lavi/minecraft/diagnostics/container/gui/` and ten matching test/support files only after callers are disconnected.
6. Restore the previous bounded-formatter required-key set only if no remaining payload depends on the added identity keys.

Do not revert whole upstream files, prior diagnostics, post-place handoff,
automatic-deposit work, documentation, or unrelated dirty work. Do not use a
broad reset, restore, checkout, cleanup, or commit revert.
