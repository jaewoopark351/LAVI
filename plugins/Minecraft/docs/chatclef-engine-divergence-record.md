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
