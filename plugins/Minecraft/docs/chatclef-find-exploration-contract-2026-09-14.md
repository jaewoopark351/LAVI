<!-- 20260914_kpopmodder: Document the user-requested observe/explore/reobserve/stop/report-or-approach extension without changing executable code. -->
# Fabric ChatClef FIND exploration contract

Date: 2026-09-14. Status: `VERIFIED_AUTOMATED / NOT_RUNTIME_VERIFIED`.

The user requested this flow: observe the requested villager; if absent, explore
and observe again; when found, stop exploration; then report coordinates or
perform the explicitly requested approach. This document records that new
direction and the later authorized source implementation. The original
documentation-only work unit performed no executable changes. The later user
request authorizes source, tests, companion logs and repository build verification;
deployment, game execution, external-instance writes, cache mutation, rollback,
commit and push remain outside the current work unit.

Repository: `C:\Vtuber_Souorce_Code\LAVI`; comparison HEAD:
`861adc005552a93f66d0299275de42ae71f587ab`; branch:
`test/automatic-deposit-checkpoint-20260831`. Existing dirty source and documents
are preserved. [AGENTS.md](../../../AGENTS.md),
[backend separation](minecraft-backend-separation.md),
[upstream integration direction](chatclef-carryon-integration-direction.md) and
[cache troubleshooting](chatclef-baritone-cache-troubleshooting.md) still apply.
Fabric ChatClef is the only backend in scope.

## Precedence and current source

For the mob exploration extension, this contract supersedes the earlier
requirements that a complete local miss immediately ends FIND, report never
requests movement, and approach uses only an initially observed target within
64 blocks. The superseded requirements describe the preceding Tracker source,
not a defective implementation of this new contract. All other input, catalog,
STOP, task lifetime, privacy, result verification and approach safety contracts
remain applicable. See the [implementation record](chatclef-find-implementation-contract-2026-09-14.md)
and [tracker implementation record](chatclef-find-tracker-reuse-direction-2026-09-14.md).

The preceding Tracker entity report read live client membership without
a default radius and did not explore. Its entity approach still observed
within 64 blocks. Neither the existing 236-test result nor an existing JAR
establishes implementation or acceptance of this exploration extension. The
historical 72-visit/zero-match villager miss's root cause remains `UNKNOWN`.

## Intent and supported targets

| Request | Mob behavior contract |
| --- | --- |
| `@find entity minecraft:villager` | Observe; if missing, explore within the frozen limits and reobserve; stop exploration when found and report the live coordinates. |
| `@find entity minecraft:villager report` | The same discovery and coordinate-report behavior with explicit mode. |
| `@find entity minecraft:villager approach` | Use the same discovery flow; stop exploration when found, then attempt a safe approach to that same target. |
| `마을 주민 찾아줘` | Compile to entity/report through the existing Korean input path. |
| `마을 주민 찾아서 가까이 가줘` | Compile to entity/approach through the same path. |

Exploration is automatic for accepted mob FIND requests. No new command mode,
argument or implicit follow-up command is introduced. Chat and trusted final
microphone recognition use the same interpretation and admission checks.
Ambiguous or unsupported input is rejected or clarified before submission; it
does not start exploration, GET, mining or acquisition.

The first extension covers currently registered eligible mobs, including a
registered modded mob addressed by its actual runtime ID. It is not limited to
villagers. Entity report and approach share the Tracker client-observable
discovery policy without a default 64-block cutoff. Approach discovery and
same-target revalidation must use that revised policy; approach route and safe
range limits remain separate and do not become unlimited.

Exact other-player name search remains included, with its current 64-block local
scope. Dropped-item report remains 64 blocks and block report remains 32 blocks.
Those paths do not acquire exploration in this first extension. Item approach
remains rejected. Inventory, container contents, obtaining items, locating a
village structure or assuming a villager exists in a village are separate
features. A previous report cannot silently supply a later request's target.

Report can move while looking for an absent mob. Once a target is selected,
report requests no further travel toward it. Approach may request further
supported movement after exploration cleanup. Automatic defense and survival
can move in either mode under their existing owners.

## One parent and finite internal observations

```text
existing input / admission / runUserTask
  -> one FIND parent and operation lifetime
     -> complete bounded local observation
        -> no match: owned bounded exploration + periodic new observations
        -> match: latch candidate; stop exploration and descendants
     -> prove owned cleanup and revalidate the same live target
        -> report: commit coordinates
        -> approach: existing safe movement, cleanup and live arrival checks
  -> one immutable outcome + matching parent completion -> Korean UI / TTS
```

The parent owns the single command root, operation ID, mode, frozen request and
binding, total limits, phase, latched target and final outcome. It uses the
existing [entity observation owner](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/observation/entity/MinecraftFindEntityObservation.java)
for actual matching, deterministic ranking and live revalidation. It must not
submit a new user command for each scan or exploration restart.

An internal complete miss is scan evidence, not a command terminal. The current
[FindObservationOperation](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/observation/FindObservationOperation.java)
commits an immutable result on its first miss and can spend `REPORT_TERMINAL`
capacity. Do not reuse that terminal-producing report operation unchanged as
the exploration loop. Reuse its bounded query and validation collaborators,
with the smallest necessary internal scan boundary. Internal observations may
log their result but cannot publish a completion, close the parent trace, spend
its protected outcome slot or speak a miss before exploration has ended.

Each scan freezes the player's current XYZ as its local ranking origin and
borrows the current live client-world iterable synchronously. World, player,
dimension and catalog binding must still match the original parent. Refreshing
the local center does not renew a deadline or replenish cumulative limits.
Do not retain a live iterator across ticks or copy/refresh all entities merely
to support the loop. All actual iteration initiated by FIND is counted.

First discovery means the nearest among eligible mobs actually evaluated in the
first completed synchronous membership pass that has a match, ranked from that
scan's frozen origin with the existing stable UUID tie rule. Entity state and
position are read at each visit; this is not an immutable simultaneous world
snapshot. Normal client ticks cannot interleave with this non-yielding pass.
Reentrant membership mutation invalidates its completeness or produces typed
read/binding failure; it cannot silently become a completed frozen snapshot.
Log the actual membership-pass and sampling times. Final revalidation checks
the selected entity, not a newly ranked set of all entities. Discovery does not
mean the globally nearest mob, the first iterator entry or the nearest in every
unvisited area. Once latched, that candidate is not replaced by a closer mob
seen during cleanup or approach. A partial scan, read failure or exhausted
scan cannot establish discovery even if its prefix contains a candidate.

Only a complete zero-match scan may start or continue discovery exploration.
Incomplete observation, invalid binding or read failure ends through the
existing typed non-success path; it does not authorize a retry. Reobservation
after a complete miss is a planned search phase, not transport replay.

## Finite limits and origins

The following retain the earlier proposed design defaults. The later authorized
implementation unit below is authoritative for frozen source constants, clock
origins, cooperative work slices and native movement containment.
They are not user-supplied numbers, current source constants or gameplay-verified
values. A later implementation must record and freeze its concrete values before
source changes; adjustments must explain their effect on this contract. No new
GUI setting, global configuration change or command argument is implied.

| Limit | Proposed value and ownership |
| --- | --- |
| Whole parent lifetime | 300 monotonic seconds from the accepted FIND user-root submission boundary defined below; includes search, defense interruptions, cleanup and any approach. |
| Discovery phase | At most 180 monotonic seconds from that boundary, including the initial scan, exploration, pauses, exploration cleanup and final discovery revalidation. |
| One local entity scan | At most five seconds and 4,096 actual `next()` visits; additionally constrained by remaining parent/phase time and cumulative visits. |
| Scan count | At most 181 started scans, including the initial scan. |
| Cumulative entity visits | At most 262,144 actual visits across all scans; partial and failed scans still count. |
| Reobservation cadence | Successive scan starts are at least one monotonic second apart; no concurrent scans or catch-up burst after a pause. |
| Exploration displacement | At most 512 blocks in horizontal Euclidean distance from the original admission XZ. |
| Cumulative sampled horizontal movement | At most 1,024 blocks summed between client-tick position samples during discovery, including defense. This bounds the sampled measure, not actual route length. |
| Exploration starts | At most eight actual process/route starts, including starts after preemption; never replenish on child `onStart`. |
| Owned movement without progress | At most 30 active exploration seconds since the last verified meaningful progress. Progress resets only this interval; defense pauses it, and child restart does not reset it. |
| Approach phase | Retain the 120-second maximum value, but start this new phase budget at validated post-exploration handoff, capped by remaining parent time. This intentionally changes the current approach operation's first-start origin. |

Freeze the parent monotonic timestamp on the Java client thread after successful
FIND-specific admission and target resolution, immediately before submitting its
single user root through `runUserTask`. Capture it once and propagate it into
the operation; do not capture it again at construction, first `onStart`, a scan,
child restart or resume. Use the same monotonic clock domain for all budgets.
WebSocket acceptance and request `deadline_ms` are separate existing boundaries;
their wall-clock timestamps are not interchangeable with this monotonic value.
The preceding Tracker source started observation and approach clocks on their first Task
start; admission timing and the approach phase origin above are planned changes.

Discovery ends only after exploration is quiet and the same target passes final
discovery revalidation. A candidate latched at 179 seconds whose cleanup ends at
185 seconds has exhausted the proposed 180-second discovery deadline: no report
success or approach handoff is allowed. After a valid handoff before that
deadline, discovery's budget is no longer applied; the approach phase and parent
budgets remain active. Expiry stops admission of new FIND work and selects
non-success; safe retirement still requires its actual lifecycle evidence.

If a Fabric request carries an earlier `deadline_ms`, preserve that outer
deadline and its existing classification; do not extend or bypass it to obtain
the proposed 300 seconds. The existing bridge deadline path explicitly permits
`task_may_still_be_running`: terminal transport evidence alone does not stop
the root or prove cleanup. The later implementation must inspect and reconcile
FIND-owned retirement on that path without changing unrelated command policy.
Do not promise a FIND terminal profile after an outer terminal was committed.

The original admission origin anchors discovery displacement; sampled movement
is a rolling sum across that phase, not distance from that origin. Continue
those measurements through discovery cleanup; after validated handoff, approach
uses its existing finite route footprint and safety envelope. The discovery
limits are not reapplied as an entity-existence radius or silently expanded to
new approach rules. The per-scan frozen origin is used only for local ranking.
Record these policies explicitly. Do not rank from the old admission position while
claiming the nearest mob in a newly visited local area.

Before each scan and visit, enforce all applicable remaining limits. Exactly
4,096 available entities may complete a local scan; an additional available
entity exhausts it without consuming that entity. Initial scans, repeated scans,
child restarts and defense resumption share the same parent counters. Cleanup
and revalidation cannot turn expiry into success.

Meaningful progress must be an authoritative movement fact, such as reaching an
admitted waypoint or entering a newly observed search area after real travel.
Repeated scans of the same area, path recalculation, jitter and diagnostic output
do not reset it. The later movement implementation must define the exact measured
predicate and bound any planning/worker/waypoint work it initiates.

Position sampling must use an existing client-tick observation boundary that
still runs while FIND is preempted; its parent Task alone does not. Retain only
operation-owned previous position, accumulated distance and progress evidence.
Count finite same-binding displacement, including defense and teleport jumps;
never assume an unavailable or non-finite sample is zero. A sampling gap prevents
claiming complete movement accounting and must stop further exploration through
typed non-success or the existing cautious evidence-gap path. Tick samples can
miss curved travel between samples; do not call their sum a hard actual-travel
bound. Define the concrete area/waypoint and real-travel predicate for meaningful
progress before implementation; repeated scans or newly cached data alone do
not establish it.

These limits bound FIND's search intent and measured work. Post-call deadline
checks do not interrupt a blocking engine call. A measured displacement check
also does not prove that a native path or asynchronous goal cannot overshoot.
Validate admitted movement boundaries and the native work owner before claiming
a hard spatial or per-tick guarantee. If that cannot be contained safely, end
with typed non-success rather than silently loosening the limit.

The five-second observation deadline is not permission to block a client tick
for five seconds. Before implementation, specify a separate practical synchronous
work and soft-time policy at the actual query/diagnostic owners. Exhaustion makes
the pass incomplete and cannot authorize partial discovery or exploration retry.
Individual engine/mod getters can still block; record that limitation. A
resumable snapshot would require a concrete membership/generation contract
before changing the synchronous live-iterator policy.

## Movement reuse and safety

ATTACK's [closest-object parent](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/AbstractDoToClosestObjectTask.java)
returns `new TimeoutWanderTask(true)` when there is no candidate. That explains
how it can load new areas and later observe a resident; it does not know an
unloaded resident's coordinates in advance or guarantee a resident will exist.

Do not directly attach [TimeoutWanderTask](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/movement/TimeoutWanderTask.java)
or [GetToEntityTask](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/movement/GetToEntityTask.java).
Wander's default infinite-distance form does not finish on a progress timeout.
It resets progress/origin/counters on start, can return `KillEntitiesTask` when
stuck, and can use an unstuck child that issues `CLICK_LEFT`. Its startup also
handles inventory/cursor items and its cleanup uses broad `forceCancel`.
Removing the eventual attack call leaves those other side effects intact.

Reuse the existing engine's exploration/path calculation and Task scheduling
through the smallest focused Fabric-owned movement adapter whose effects and
cleanup can satisfy this contract. Discovery orchestration, movement ownership
and diagnostic delivery are separate responsibilities. Use focused packages
for newly independent implementations, following the user's refactoring request
and AGENTS.md's owner-based rule. Do not create a generic backend framework,
parallel scheduler, pathfinder or global mutable operation context.

Exploration issues no FIND-owned attack, kill, target interaction, collection,
cursor discard, equipment change, mining or placement fallback. It must not
enable unsafe global settings or disable defense. Supported routes must have
proven permitted movement and finite calculation/work boundaries. Unsupported
terrain or a route requiring prohibited side effects may fail as unreachable.
Do not claim that normal native exploration automatically has those safeguards.
This restriction is specific to FIND-owned movement; existing automatic defense
and survival retain their own policies and diagnostics. Ordinary Minecraft
proximity pickup while moving can still occur; absence of a collection Task does
not prove that no dropped item entered the inventory.

Read-only local inspection establishes that native `IExploreProcess` has an
`explore(x,z)` entry but no finite radius/deadline API. Its `onLostControl` clears
the exploration origin; that alone is not path/worker quiescence proof. Broad
pathing `forceCancel` affects shared pathing and is not an operation-specific
worker join. The exact safe process/goal/worker cancellation and containment
boundary remains `UNKNOWN` until the later owner-local implementation inspection
and verification. This design does not authorize changing those engine APIs.

## Discovery handoff, defense and STOP

When a completed scan selects a mob, latch its UUID, current registry type and
binding internally. Stop issuing exploration work immediately. Request only
owned exploration child/descendant and process/path/input cleanup, then wait for
proof of quiescence before reporting or starting approach. Parent `onStop` runs
before descendant teardown; it cannot by itself prove cleanup. Returning no
child, observing `isActive=false`, losing process control or clearing an origin
alone is insufficient evidence that old inputs or worker goals cannot return.

After cleanup, give external STOP, root retirement/replacement and binding
changes priority. Then revalidate the same live target's identity, type,
liveness, world/dimension, player and catalog/resource context. Report commits
its current coordinates only at this boundary. Approach hands that exact target
to the existing safe movement owner once; it does not rerun discovery as a new
command. Preserve its dry flat route, visibility, safe-distance, input lease,
planning and cleanup checks. Target loss or unsupported approach does not restart
exploration or select a substitute.

Freeze a separate movement-planning origin at validated handoff, using the then
current player XYZ and the unchanged world/player/catalog identity. Keep the
original admission origin and local selection origin as separate evidence; do
not overwrite them to refresh budgets. Seed the existing movement collaborator
with this binding and the validated latched candidate. Do not submit a new
approach user root or let its old embedded observation operation select again.
Remove the old mob identity-revalidation 64-block cutoff consistently. A live
target outside the retained finite route footprint is an unsupported approach
(`UNREACHABLE`), not `TARGET_LOST` solely because of that old cutoff.

Defense and survival preempt FIND. Suspend only FIND-owned exploration or
approach activity and retain the parent phase, latched target and all budgets.
After safety returns, revalidate binding and, if already latched, the target.
Discovery without a latched target may resume within the remaining limits.
Never cancel a defender's or new root's path from delayed FIND cleanup.

`onStop(null)` may be temporary preemption; it is not user STOP evidence. Child
`onStart` after reset cannot reset parent budgets. STOP/root cancellation remains
authoritative over late scan, discovery, cleanup and completion callbacks.
Failed or unavailable cleanup cannot permit coordinates or approach handoff.
At expiry, stop admitting new work and select non-success; do not manufacture
`quiet=true`, a finished Task or matching natural completion to force a terminal
FIND profile. If the existing lifecycle cannot prove matching parent completion
and effect containment, use its cautious UNKNOWN/evidence-gap classification.
No unbounded cleanup retry is allowed. A hard wall-clock cleanup guarantee needs
actual cancellation/work containment, not a post-call timer.

Quiescence means that no old-phase descendant or pending result can mutate
inputs, goals, paths, world or inventory. A worker still calculating is not a
worker-termination fact; it may be contained only if every later effect/result
commit is proven fenced to the retired phase. Define that resource-specific
proof before claiming cleanup. Never report released resources merely because
cancellation was requested.

The single parent prioritizes externally committed STOP/root retirement and
binding loss before a new FIND decision. Among new internal outcomes, a proven
cleanup/read failure selects `INTERNAL_ERROR`; applicable deadline expiry then
selects `TIMEOUT`, followed by work/spatial bounds and route/target results.
Retain secondary causes in diagnostics. An already assigned outcome or external
terminal is never rewritten by a later error, timeout or callback.

## Results, Korean presentation and wire compatibility

Keep existing v1 report/approach profiles, closed payload fields, target-kind
scope literals and callback plus matching user-parent completion requirements.
`loaded_entities` describes client-local observed mobs across the completed
local scans; it is not a claim that every point inside the displacement bound,
every unloaded chunk or the whole world was searched. No `explore` mode, radius,
phase, new result literal or extra payload key is added by this document.

| Authoritative outcome | Existing result and required meaning |
| --- | --- |
| Live target selected, exploration quiet, report checks passed | `FOUND_AND_REPORTED`; report current coordinates. |
| Same target safely reached and owned movement quiet | `FOUND_AND_IN_SAFE_RANGE` or `ALREADY_IN_SAFE_RANGE`. |
| A finite native search plan genuinely finishes, every promised local scan is complete and all are misses | `NOT_OBSERVED_IN_LOADED_SCOPE`; not observed in the areas actually checked. An initial miss alone is insufficient for mob FIND. |
| Partial scan or scan/visit/travel/start limit reached | `OBSERVATION_BOUNDS_EXHAUSTED`; search was incomplete or reached its configured limit. |
| Discovery, approach or parent deadline expires | `TIMEOUT`; name the exhausted phase in the bounded reason. |
| Exploration cannot obtain a supported route or loses progress before discovery | `UNREACHABLE`; could not continue exploration, without claiming a target was found. |
| Target found but its supported approach fails | `UNREACHABLE`; target was found but approach could not complete. |
| Same target lost or live identity cannot be verified | Existing `TARGET_LOST` or `CANDIDATE_NOT_REVALIDATABLE`, as selected by the authoritative owner. |
| Binding/root interruption, invalid target or read/cleanup failure | Existing `INTERRUPTED`, `INVALID_TARGET` or `INTERNAL_ERROR` as applicable. User STOP uses the existing single cancellation response. |

Unsatisfied outcomes retain the existing ban on candidate identity, dimension
and coordinates. The current wire accepts bounded free-text `reason`; it does
not carry independent phase/discovery proof for an unsatisfied outcome. Do not
infer that a mob was found from `UNREACHABLE`, mode or arbitrary reason text.

For the new mob parent, freeze a closed terminal reason/mode/result matrix
before source implementation. The initial proposed combinations below are
bounded reason strings inside the existing field, not extra payload keys or
new result values. Further owner-specific reasons must be explicitly enumerated
in the same matrix; substring matching or an open-text fallback cannot establish
discovery or phase facts.

| Proposed reason | Mode | Allowed existing result / phase evidence |
| --- | --- | --- |
| `discovery_target_revalidated_and_exploration_quiet` | report | `FOUND_AND_REPORTED`; completed discovery and owned cleanup proof. |
| `same_target_safe_range_and_owned_cleanup` | approach | `FOUND_AND_IN_SAFE_RANGE` or `ALREADY_IN_SAFE_RANGE`; completed discovery, same-target arrival and cleanup proof. |
| `finite_search_plan_completed_no_match` | either | `NOT_OBSERVED_IN_LOADED_SCOPE`; a finite plan's completed local scopes are all misses. |
| `local_observation_deadline_exhausted`, `local_entity_visit_limit_exhausted`, `cumulative_entity_visit_limit_exhausted`, `scan_count_limit_exhausted` | either | `OBSERVATION_BOUNDS_EXHAUSTED`; discovery observation/work limit. |
| `exploration_displacement_limit_exhausted`, `exploration_sampled_movement_limit_exhausted`, `exploration_start_limit_exhausted` | either | `OBSERVATION_BOUNDS_EXHAUSTED`; discovery movement/admission limit. |
| `discovery_deadline_exhausted` | either | `TIMEOUT`; discovery, including exploration cleanup/revalidation, did not finish in time. |
| `approach_deadline_exhausted` | approach | `TIMEOUT`; validated discovery handoff occurred and approach's phase time expired. |
| `parent_deadline_exhausted` | either | `TIMEOUT`; parent time expired; does not alone assert discovery. |
| `exploration_route_unavailable`, `exploration_no_progress` | either | `UNREACHABLE`; exploration could not continue; does not assert discovery. |
| `post_discovery_approach_unreachable` | approach | `UNREACHABLE`; verified discovery/handoff occurred but supported approach failed. |
| `selected_target_lost`, `selected_identity_unverifiable` | either | `TARGET_LOST` or `CANDIDATE_NOT_REVALIDATABLE`, respectively; latched target no longer passes the corresponding live check. |
| `world_player_or_catalog_binding_changed`, `root_ownership_lost` | either | `INTERRUPTED`; authoritative binding/ownership loss, unless an external cancellation has already committed. |
| `observation_read_failed`, `owned_cleanup_failed`, `movement_sample_unavailable` | either | `INTERNAL_ERROR`; actual owner failure/evidence unavailable; cleanup is never assumed complete. |

The producer derives reason from an immutable authoritative terminal-phase and
discovery snapshot bound to this operation/root/request. The projection checks
that snapshot as well as the existing matching completion and resource binding.
A post-discovery approach reason is invalid for report or without verified
discovery/handoff. The receiver accepts only the defined combinations and uses
them for cautious phase-aware Korean text; unknown or inconsistent combinations
are rejected or quarantined, never spoken as discovery. This remains an
authoritative producer assertion, not independent recipient observation of the
Minecraft world. The existing wire alone cannot provide the latter proof.

Keep an explicit compatible allowlist for legacy reasons and unchanged non-mob
paths. Legacy handling must not infer new exploration-phase facts; document its
existing cautious renderer behavior separately. Do not silently broaden a
closed validator to accept arbitrary phase claims. The current native and
Python renderers primarily select text by `find_result`; both need the matching
reason-aware contract in the later implementation.

The later implementation must update reason-aware Korean screen/TTS rendering
and command descriptions together. Before exploration, give the same concise
meaning for both input channels: `주변에서 찾지 못해 탐험하며 다시 확인할게요.`
This is an optional deduplicated progress notice, not terminal or success proof.
On report success, say where the verified target currently is; on exhaustion,
say the checked areas or search limit, never `월드에 없습니다`. Distinguish
exploration failure from post-discovery approach failure. One final verified
meaning is delivered once through the existing presentation owners.

## Companion logs and finite delivery

This documentation edit has no execution boundary: new log implementation and
actual-output tests are `NOT_APPLICABLE` for this work unit. The later feature
must implement and verify its companion logs in the same source task.

The behavioral parent owns counters and decisions. Capture actual before/delta/
after counts, original and local origins, elapsed/remaining phase and parent
time, started scans/processes, cumulative visits/travel, no-progress evidence,
scope completeness, candidate selection, cleanup, revalidation and terminal
reason. Movement owns process/path/input acquisition, suspension and cleanup
facts. Reuse current query exclusion summaries and at most three bounded samples;
exclude raw conversation/microphone text, player names, UUIDs and NBT.

Before source edits, enumerate `REQUIRED_CAUSAL_OWNER_BOUNDARY_SET` from actual
owners: owner + event family + semantic transition/result/reason + necessary
phase role. Protect the first initial miss-to-exploration transition, first
completed reobservation miss, first discovery, exploration stop request and
quiescence, target revalidation, applicable defense suspend/resume, approach
handoff and operation/trace terminal. Also include applicable query, safety,
input and cleanup failure boundaries; family names alone are insufficient.
Scan indices, positions, ticks and random operation IDs remain correlation
values, not new reservation signatures for every repeat. Initial observation,
discovery-loop observation and final handoff are distinct causal phase roles.

The current 5,000-slot shared cap and four FIND traces of 32 slots are the
starting constraint. Of those 32, 30 protect first semantic boundaries and two
protect the assigned outcome and trace retirement. Exploration plus approach
may need more distinct signatures than the existing trace. Demonstrate a complete
per-path bound before claiming the old reservation suffices. If required coverage
cannot fit, document the smallest exact reservation redistribution inside the
unchanged shared hard cap and preserve other owners' critical capacity; do not
drop a required boundary, consume terminal slots for intermediate scans, widen
the cap or silently downgrade required events to ordinary detail.

Read-only coverage inspection found that an existing maximum output fixture
already uses 28 first boundaries plus the two terminal records. Adding query
and exploration phase boundaries cannot be assumed to fit the remaining two
first-boundary slots. A concrete proposal is to retain the 128 FIND-reserved
slots but admit at most two protected 64-slot traces per existing process-lifetime
diagnostic session,
each with 62 first-boundary slots plus outcome and retirement. This is a
reservation redistribution proposal, not a change to functional admission or
the current implementation. It reduces the number of fully protected operations
in a session; closing a trace does not refund capacity. The exact semantic
matrix must fit the proposed trace before implementation, and existing other
critical owners' reservations must remain intact.

Verify actual formatter/sink output in `BOUNDARY`, ordinary-cap pressure,
repeated reobservation, OFF and output failure. Reserved capacity establishes
admission, not persisted output. Diagnostic mode, admission, suppression,
serialization or failure never changes target selection, exploration starts,
limits, retry, cleanup or outcome. Report missing delivery as unverified.

The two-by-64 allocation above is the historical planning proposal. The authorized
implementation unit below uses one complete 128-entry trace after capacity review.

## Implementation and acceptance checklist

Before the later source patch, identify exact admission, scan, native exploration,
child stop, path/input/worker cleanup, same-target approach and presentation
owners; inspect the current callers and test seams. Record concrete limits,
required boundary signatures, safe native movement containment and exact reversible
hunks against the current mixed worktree. Reuse focused owners; preserve the
existing Tracker getter and upstream lifecycle unless a justified smallest
engine boundary change separately passes its policy gate.

Focused tests must cover immediate discovery without exploration; initial miss
then discovery after travel; repeated complete misses; nearer selection in the
current local scan; vanilla and actually registered modded mobs; distant discovery
in both modes; all finite limits; partial scans with a seen candidate; read failure;
target loss during cleanup/approach; no combat/mining/placement/cursor fallback;
owned cleanup before report/handoff; defense and child reset without renewed
budgets; STOP/root replacement and late worker results; strict reason/schema
and matching-completion evidence; Korean UI/TTS parity; and unchanged player,
item and block scopes. Capture actual required output and prove diagnostic
failure/non-interference separately from behavioral tests.

Repository tests and required forced clean builds follow the existing
[build runbook](chatclef-fabric-build-verification.md). Record canonical
multi-version failure separately from exact 1.20.1 build/test success; unchanged
NBT/enchantment compatibility errors do not authorize unrelated GOTO edits.
Deployment, live-world travel, matching fresh JAR identity, runtime injection,
actual FIND logs and audible voice checks remain separate acceptance evidence.
Cache presence alone is not a cause or permission to mutate it.

Evidence at the earlier documentation-review boundary: source `NOT_IMPLEMENTED`; tests/build/deployment/
live acceptance `NOT_RUN`; native safe movement/worker quiescence `UNKNOWN`.
This documentation review verifies document consistency and links only.

## Authorized implementation unit (2026-09-14)

The later user request authorizes repository source, focused tests, companion
diagnostics, documentation and forced clean verification. Earlier documentation
reviews below remain historical. No deployment, live-world execution, cache
mutation, commit or push is authorized by this implementation request.

The pre-edit preservation snapshot is
`logs/find_exploration_implementation_20260914_220332_843/pre-edit-manifest.json`
(96 snapshot entries covering 86 unique paths, HEAD
`861adc005552a93f66d0299275de42ae71f587ab`, empty index).
The independently reviewable change comprises new `find/exploration/operation`,
`find/exploration/movement` and `find/exploration/task` owners; command admission,
client-tick sampling and explicit operation retirement; entity observation policy;
phase-bound result validation/rendering; companion diagnostics and their tests.
Preserve all preceding Tracker and input changes. No upstream movement class or
global scheduler is rewritten. An inverse is the exact new hunks against this
snapshot plus only newly created exploration files; no rollback is performed.

### Frozen implementation limits and movement containment

- Parent 300 seconds and discovery 180 seconds start once at Java FIND submission,
  before `runUserTask`. Discovery includes initial observation, defense pauses,
  exploration cleanup and final handoff revalidation. Approach starts after
  validated handoff and has 120 seconds, still bounded by the parent deadline.
- Each observation permits 4,096 actual candidate visits, a 5-second deadline and
  a practical 50-millisecond synchronous work slice. The slice is cooperative;
  it cannot interrupt a blocking client getter. A partial scan never succeeds.
- At most 181 scan starts, one second between starts, 262,144 cumulative visits,
  eight route starts including resumes, 512 horizontal displacement and 1,024
  sampled horizontal travel. Client tick samples continue during discovery-phase
  defense and through final handoff validation; discovery movement counters freeze
  at validated approach handoff, so later approach motion is not added to them.
  Missing/non-finite samples or gaps over five seconds fail cautiously; sampled
  travel is not a claim about an unsampled curved trajectory.
- Thirty active seconds without a verified full waypoint arrival ends
  exploration. Defense pauses only this active-progress timer.
- The selector checks 16 loaded candidates (eight directions at distances 24
  and 48; implementation ceiling 24). Each route uses existing native AStar
  calculate-only planning within 64 blocks, 128 steps and 50,000 context reads.
  Capture permits 128 columns per tick and a cooperative 2-millisecond slice;
  native calculation has the existing cooperative 5-millisecond budget.

`IExploreProcess` is not submitted: its shared cancellation has no operation-only
worker join. FIND instead selects loaded waypoints and reuses existing native
flat-path calculation plus exact input leases. No global goal/path, asynchronous
worker, attack, left-click escape, block mutation or cursor discard is installed.
Supported exploration is loaded, dry, flat terrain; an unavailable safe route
ends with an exploration failure. It does not promise terrain-independent or
unloaded-world exploration. Admission-origin containment is checked before
waypoint and step admission, as well as by cumulative client-tick sampling.

The parent owns phase transitions, deadlines, scan/start counters and the sole
terminal. The sampling owner owns measured movement and active no-progress time.
Observation owns live identity reads; exploration owns its route generation,
staged inputs and exact leases; approach retains its existing safe movement
owner. Retirement is behavioral and separate from diagnostic closure. The
exploration Task freezes the exact original incomplete `UserRootLifetime`,
verifies it on every fresh root fence, and uses identity equality so a new
command cannot retain the previous operation merely because its request is
equal. The native observer forwards the initial lifetime and rejects foreign or
completed ownership before sampling; defense with the same lifetime preserves
the original budgets. Final phase evidence binds the same operation, request
and user root. Required
companion boundaries include the first initial/loop scan, candidate latch,
exploration start/suspend/cleanup, handoff/arrival validation, causal exhaustion
or failure, assigned outcome and trace retirement. Numeric generations remain
correlation fields. Tests must demonstrate physical reserved emission and
diagnostic non-interference; missing runtime emission remains unverified.

Source implementation and repository verification are complete. The final
verification record below distinguishes successful 1.20.1 checks from the
failed canonical multiversion build. Deployment and live acceptance are `NOT_RUN`.

The implementation adds the closed bound reason
`exploration_route_work_limit_exhausted` for waypoint/native path/context work
limits; `exploration_start_limit_exhausted` exclusively means route-start count.
The exact underlying native reason remains in the owner log.
Native ten-second step deadlines retain `TIMEOUT`, using the closed reasons
`exploration_step_deadline_exhausted` before discovery and
`native_approach_step_deadline_exhausted` after verified approach handoff.
Waypoint progress requires at least 16 horizontal blocks from its route start
and two grounded, dry, low-velocity active ticks at the exact waypoint.
Diagnostic progress snapshots never reset a behavioral budget.
The existing FIND reservation family stays at 128 and the shared session budget
stays at 5,000. Because role-separated planner and all five input claim/release
boundaries cannot fit a 64-entry trace, the implementation protects one 128-entry
trace per diagnostic session (126 first-boundary entries, outcome and retirement).
Here the two dedicated slots mean the assigned `PARENT_OUTCOME` (or legacy FIND
terminal) and `TRACE_TERMINAL`; `RETIRED` and `PARENT_RETIRED` use first-boundary
slots. The semantic signature includes causal native planner result, input
release ownership/supersession, fixed observation/movement role and phase
transition. Correlation IDs and numeric scan/route indices are excluded.
Later traces explicitly report reservation exclusion and retain ordinary output
when available. This never changes functional admission or any FIND decision.
Earlier two-by-64 figures are superseded implementation proposals.

Conservative per-operation first-boundary capacity proof:

| Authoritative owner boundaries | Maximum first signatures |
| --- | ---: |
| Query and parent scan: I start/completion, D start and up to two completed meanings per owner | 10 |
| C/H/A live identity revalidation | 3 |
| Parent start, phase transitions, latch, handoff, resumable-phase suspend/resume, stop/cleanup/route/no-progress/exhaustion/retirement and approach result | 22 |
| Exploration movement/native decisions | 11 |
| Role-separated planners, including a later non-goal E result | 9 |
| Existing native approach decisions | 5 |
| Role-separated input claims/releases/loss/write rejection (conservative bound) | 50 |
| Measured movement, active clock transitions and full-waypoint reset | 5 |
| Diagnostic close `RETIRED` | 1 |
| First user-root lifetime binding, optional idempotent binding and rejected binding | 3 |
| **First boundaries** | **119** |
| Assigned outcome plus `TRACE_TERMINAL` | **2** |

This proof depends on one latched candidate, one C/H/A call each, and the first
typed native failure terminating the operation. Initial completed scope has one
meaning; D can produce repeated MISS then MATCH or one incomplete failure, never
continue after MATCH/failure. Resumable phases conservatively include INITIAL,
EXPLORING and APPROACHING; STOPPING is synchronous and ends in handoff, terminal
or retirement, and TERMINAL never resumes. The 50 input bound includes normal
five-key claims/releases in both roles and both owned/stale false-release
meanings; only one false claim, lease loss or write rejection can occur before
the first native failure ends the operation (a tighter input bound is 41).
Physical saturation/phase fixtures verify actual formatted output, not live
world execution or exhaustive generation of all conservative signatures.

Normal OFF failure and retirement warnings use the already captured parent
snapshot, including elapsed/cumulative values, actual limits and cleanup state,
through the existing formatter/logger within 2,048 bytes. They perform no new
game, clock, command or progress reads and do not depend on trace reservation.

## Final exploration verification, 2026-09-14

The final source unit preserves HEAD
`861adc005552a93f66d0299275de42ae71f587ab` and branch
`test/automatic-deposit-checkpoint-20260831`. The Git index remains empty.
No deployment, Minecraft launch, live-world action, external-instance mutation,
cache mutation, rollback, commit or push was performed.

| Evidence | Actual result |
| --- | --- |
| Required canonical `clean build --rerun-tasks`, cache disabled | FAIL at `:1.21.1:compileJava`, five existing compatibility errors in unchanged files. |
| Exact 1.20.1 forced clean/remap/access-widener/focused verification | PASS, 36 tasks executed; build cache disabled. |
| Focused Java regression suite | 309 started, 309 successful; zero failed, aborted, skipped or container failures. |
| Python `pytest tests/minecraft_chatclef` | 1,354 passed, two skipped, 14,407 subtests passed. |
| Fresh packaged FIND source | 67 classes, all major61 (Java17); exploration/phase owners, FIND entrypoint, lease Mixin registration/refmap and exact 1.20.1 nested Baritone present; test harness absent. |
| Final executable source manifest | SHA-256 values match the files used for final verification. Later documentation-only edits are recorded separately. |
| Physical diagnostic saturation and lifetime binding | PASS; 79 role/meaning-separated fixture events, dedicated outcome/trace terminal, ordinary pressure, OFF/exclusion/throwing sinks, and settled reservations. These are harness results. |
| English documentation and preservation checks | PASS for seven documents; UTF-8, fences, local links, scoped whitespace checks and preserved byte snapshots. |
| Native physics, live observation/defense/STOP and runtime Mixin injection | NOT_RUN. |
| Audible voice synthesis/playback | NOT_RUN; shared UI/TTS delivery and wording verified in the Python harness. |

Final build output:
[result](../../../logs/find_verification_20260914_224220_113/result.json),
[canonical log](../../../logs/find_verification_20260914_224220_113/canonical.log),
[1.20.1 clean/test log](../../../logs/find_verification_20260914_224220_113/fabric12001.log),
[source manifest](../../../logs/find_verification_20260914_224220_113/source-manifest.json),
and [artifact inspection](../../../logs/find_verification_20260914_224220_113/artifact-inspection.json).
Python output:
[full Minecraft pytest log](../../../logs/find_exploration_implementation_20260914_220332_843/python-pytest.log).

Fresh artifact:
`plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar`;
9,073,989 bytes; SHA-256
`9520D85E2E3215160753F658859948BC68377C74353DE05E274C31D3DC3B8DCA`.
This proves build identity, not live deployment or successful movement.

The canonical errors remain two `ItemStack.getNbt` calls in
`MinecraftToolEquipPort.java:71`, `ItemStack.hasNbt` in
`GotoMaterialInventory.java:67` and `:92`, and the enchantment registry-key/entry
type at `GotoMaterialSources.java:148`. `git diff HEAD` for all three exact paths
is empty. No dependency, version, GOTO behavior or engine change was made to
resolve them.

The earlier current-unit target run failed ten diagnostic-output tests because
the existing admission owner rejected a 128-slot FIND request with its generic
32-slot maximum. The scoped 1.20.1 FIND allowance retains the existing 128 family
quota and 5,000 shared hard cap; other families retain their maximum32.
A later focused run passed 299/304 and exposed two stale output/session fixtures
plus three downstream reset failures caused by a passing fixture's unclosed
trace. Test-only scenario isolation and `finally` closure fixed those failures;
production reset semantics were not changed. The final forced run above passed
all 309 tests. Earlier restricted wrapper-cache attempts and standalone unittest
temporary-directory ACL errors are not source or runtime causal proof; the
approved normal build and existing pytest bootstrap supplied the final evidence.

Focused tests cover immediate and delayed discovery, no intermediate miss
terminal, both entity modes beyond64, live C/H/A identity, partial/read failure,
cumulative/start/time/displacement/sampled-travel/no-progress limits, original
budgets across defense, discovery-only sampling frozen at handoff, native step
TIMEOUT semantics, foreign lifetime reuse, reentrant retirement and cleanup
failure. They do not establish terrain-independent exploration or prove the
historical 72-visit/zero-match cause, which remains `UNKNOWN`.

The [preservation and documentation audit](../../../logs/find_exploration_implementation_20260914_220332_843/final-preservation-and-doc-qa.json)
records 96 snapshot entries covering86 unique paths: 60 byte-identical and26
scoped updated paths. The [review unit](../../../logs/find_exploration_implementation_20260914_220332_843/implementation-review-unit.json)
identifies19 independently attributable new files and four additional committed
diagnostic comparisons. The [pre-edit review patch](../../../logs/find_exploration_implementation_20260914_220332_843/pre-edit-to-implementation-review.patch)
contains only current changes to backed-up files and those attributable creations.
Additional HEAD comparisons are explicitly comparison evidence, not a claimed
pre-edit raw snapshot. The admission-owner addendum records its post-edit
recovered committed baseline honestly. These review artifacts execute no rollback.

## Documentation change unit

The preceding documentation-only request created this exploration contract and updated these six existing
documents through scoped additions and historical-scope clarification:

- [Minecraft README](../README.md).
- [FIND implementation contract](chatclef-find-implementation-contract-2026-09-14.md).
- [Tracker reuse direction and implementation record](chatclef-find-tracker-reuse-direction-2026-09-14.md).
- [FIND chat/microphone design](chatclef-find-chat-microphone-design-2026-09-14.md).
- [Korean GOTO/FIND/all-command historical contract](chatclef-korean-goto-find-and-all-command-coverage-pre-change-contract-2026-09-09.md).
- [Fabric bridge v1 contract](fabric-chatclef-bridge-protocol-v1.md).

Executable companion logging did not apply to that documentation-only change,
which had no execution semantics. Existing source, test and configuration
changes, Git index and refs were preserved. No source rollback or runtime action
was performed by that documentation request. The later implementation unit above
has executable companion logs and separately recorded verification.

The later documentation review amends this contract, the implementation record
and the Fabric bridge v1 document. It clarifies submission-time clocks, discovery
cleanup expiry, sampled movement, scan completeness/responsiveness, handoff
planning origin, cleanup evidence and closed phase-reason validation. Native
safe movement and worker containment remain `UNKNOWN`; no source, test, build
or game verification was performed by this review.
