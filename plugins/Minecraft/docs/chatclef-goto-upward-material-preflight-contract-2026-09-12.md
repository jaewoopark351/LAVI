# Direct-XYZ GOTO: V3.1 frozen-target handoff

Date: 2026-09-13
Base checkpoint: `3a97f8ec76fb02ddaf047849afeebe2b869dae7f`
Status: SOURCE_CANDIDATE; ISOLATED_TESTS_PASSED; FULL_BUILD_NOT_RUN; RUNTIME_NOT_RUN

## Current contract

This section supersedes only the conflicting navigation order, exact approach,
and material threshold statements in the historical V2 record below. It records
the user's later V3.1 agreement; it does not rewrite any historical evidence or
establish the root cause of unrelated navigation failures.

1. Preserve `GotoCommand.call()` admission and the existing native-first
   `PreparedGotoTask` phases. Existing terrain navigation and underground staircase
   excavation run before optional local material acquisition. A positive Y delta
   alone must not force separate mining.
2. Preserve V3.1 loaded aerial-column classification, nearby preparation-area
   checks, selected anchor, and bounded mining/drop/pickup implementation. No exact
   `foundation.up()` waypoint is required before mining. Geometry is not proof of
   an optimal path or of a native navigation failure's cause.
3. At preparation selection, freeze `placementDemand` and `plan.requiredHeld()`.
   If acquisition starts, collect to the plan's placement estimate plus three
   reserve blocks. If the placement demand is already held before acquisition,
   skip acquisition instead of mining just for the reserve.
4. At handoff, keep operation-owned child cleanup and two consecutive quiet ticks,
   root/world/player/target/settings validation, usable inventory recount, aerial
   evidence revalidation, and the stable dry exit requirement.
5. Set `minimum = collected ? plan.requiredHeld() : placementDemand`.
   A changed `remainingEstimate` must not raise or lower that minimum. Log held
   count, frozen minimum, live estimate, policy, and quantity result. Actual held
   count below the frozen minimum still fails without starting navigation.
6. On successful validation, construct one fresh existing `GetToBlockTask` for
   the original XYZ and nullable dimension. Keep the same parent and operation.
   No command-string retry, new parent operation, final-to-preparation loop, new
   timeout, cache modification, Mixin change, or dependency change is included.
7. Preserve exact arrival after child cleanup and binding revalidation. A
   successful handoff or a child FINISHED message is not itself arrival.

## Evidence boundaries for this patch

- The uploaded original log records target 34 / held 34 / re-estimate 43 and
  target 32 / held 32 / re-estimate 33 handoff failures. Later manual requests
  arrive under different operation IDs; this is not automatic handoff acceptance.
- Original production code fails both focused regression cases in an isolated
  harness. Patched production parent/lifecycle/plan/inventory code passes 49
  isolated cases (257 assertions) against explicit API and engine doubles.
- Geometry, mining, GetToBlockTask pathfinding, game physics, and the complete
  scheduler are not live-tested by the doubles. Isolated compilation is not a
  full dependency-backed Gradle/Loom build or Mixin verification.
- Full clean build, installed JAR identity, deployment, live Minecraft execution,
  Windows worktree writes, commit and push: NOT_RUN by this artifact task.
- Real acceptance still requires one user command and one operation through
  ACQUIRE_START -> PREPARED -> HANDOFF_CHECK -> RESUME_ORIGINAL -> ARRIVED,
  plus sufficient-material, underground, and STOP regression checks.
- The patch does not guarantee the frozen estimate is sufficient for every real
  route. Real final-navigation failures remain separate evidence to investigate.

## Historical V2 record (verbatim; not the current behavior contract)

The original record below is preserved for provenance. Its status, build attempt,
harness counts, approach waypoint, and timeout statements describe V2 only.

---

# Direct-XYZ GOTO: Navigation-first revision (V2)

Date: 2026-09-12
Status: SOURCE_CANDIDATE; REAL_BUILD_BLOCKED; RUNTIME_NOT_RUN

This file records the user's later request in this conversation: prioritize existing
navigation, including staircase excavation, and demote separate material acquisition.
It is not a restoration of the missing historical contract and does not change old
incident findings. In particular, the old unconditional admission delta-Y + 3 rule
is NOT the behavior of this revision. AGENTS.md still contains that older rule;
future maintainers must distinguish this later user-requested revision from it.

Scope: Fabric ChatClef 1.20.1, upward same-dimension XYZ requests through
GotoCommand.call(). The shared movement factory, GetToBlockTask, Baritone,
BotBehaviour, scheduler, dependencies, and other coordinate/dimension routes are
unchanged. This is task-order priority, not a guarantee of staircase-optimal paths.

1. Bind the original XYZ, nullable dimension request, world, player, and root.
   Start the existing GetToBlockTask without a new material-readiness gate.
2. Never equate an upward delta, unknown count, timeout, or path failure with a
   proven material shortage. Existing excavation has priority over separate mining.
3. One optional acquisition detour is allowed when a complete active native path
   to the original goal has confirmed outstanding placements, no remaining live
   excavation, and insufficient usable inventory. Read live supported sources;
   diagnostic logs/registries do not control behavior. Counts are route-specific,
   not a global optimality proof.
4. When native navigation has stalled without a path, a conservative air-column
   alternative may be selected only for a loaded, dry, sky-visible, unsupported
   aerial goal with a fully inspected air column and safe foundation. This is NOT
   evidence of the original failure cause. Mark that cause UNKNOWN. Use native
   navigation to reach the supported approach point BEFORE acquisition. Do not
   report approach completion as arrival at the original XYZ.
5. Snapshot the acquisition origin and budget only when preparation begins.
   Budget = confirmed planned placements (or remaining air-column height) + 3.
   If enough placements are already held before acquisition starts, do not mine
   just for the reserve. Count actual accessible inventory, not destroyed blocks.
6. Preserve the existing bounded serial acquisition primitive. Add deepslate
   source/drop mapping; restrict downward source selection, preserve the anchor
   foundation, and require a supported landing below a floor source. Keep finite
   search/attempt/time limits and no item discarding or setting changes.
7. Stop operation-owned children and verify quiescence before acquisition and
   resuming navigation. Revalidate binding, settings, air-column evidence when
   applicable, and held count. Resume one fresh existing GetToBlockTask with the
   ORIGINAL XYZ and dimension. Never return to acquisition after this resume.
8. STOP/root replacement and world changes take precedence. Aerial fallback
   arrival requires supported presence at the original target after cleanup.
   Do not use the common User task FINISHED message as success evidence.
9. Native no-progress/time limits are failures with an unproven cause, not
   acquisition triggers. No new pathfinder, pillar executor, global policy,
   generic retry loop, Mixin, dependency update, or cache mutation is included.

Evidence and authorization:
- Implementation basis: e6c555225283a848ce81d52fd7be40889fb4b648 plus the generated V1
  patch provided earlier in this conversation. Windows worktree not inspected.
- Latest user authorization: provide implementation code and placement instructions.
- Actual changes: isolated container files and downloadable source/patch artifacts.
- Old navigation cause: UNKNOWN; not retroactively proven by this revision.
- Isolated harness: 69 assertions, captured Task.java plus explicit API-shaped doubles.
- Full clean Gradle build: wrapper download failed (UnknownHostException).
- Dependency-backed compilation / Loom / Mixin / deployment / game execution:
  NOT_VERIFIED / NOT_RUN.
- GitHub commit, push, Windows source write, external-instance copy: NOT_RUN.

Known limits: air-column alternative is deliberately narrow; the chosen native
route can differ after acquisition; player structures composed of ordinary terrain
blocks cannot be distinguished automatically; async fork behavior and physics still
require runtime tests. File hashes and exact patch scope are in manifest.json and
verification/patch-checks.log in the delivery package.
