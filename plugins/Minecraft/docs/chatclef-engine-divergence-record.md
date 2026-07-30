# ChatClef Engine Divergence Record

<!-- 20260730_kpopmodder: Recorded ChatClef engine divergence status for Carry On diagnostics review. -->

## Current Status

Verification status: BUILT_NOT_REPRODUCED

Active behavior-changing engine divergence: NONE KNOWN

Active diagnostics-only engine divergence: NONE KNOWN after the current REQUEST CHANGES cleanup.

The previous diagnostics commit placed Carry On-specific imports, fields, observation, and logging inside
`adris.altoclef.tasks.InteractWithBlockTask`. The current cleanup removes that Carry On-specific engine coupling
and returns Carry On diagnostics to the LAVI-owned optional integration namespace.

## Upstream Baseline Provenance

Upstream repository: UNVERIFIED

Release or tag: UNVERIFIED

Exact commit: UNVERIFIED

Imported subtree: `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**`

Baseline version label observed in runtime logs: `altoclef 1.20.1-0.18.23`

Verification method: runtime log observation only; no upstream repository comparison completed in this pass.

Known LAVI-specific divergences: PARTIALLY_VERIFIED

## REQUEST CHANGES Cleanup Record

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
