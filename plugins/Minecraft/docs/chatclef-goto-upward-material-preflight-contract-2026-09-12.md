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
