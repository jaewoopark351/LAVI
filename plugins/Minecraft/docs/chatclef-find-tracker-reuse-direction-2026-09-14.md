<!-- 20260914_kpopmodder: Document the user-requested FIND tracker-reuse direction without implementing or executing it. -->
# Fabric ChatClef FIND tracker-reuse direction

Date: 2026-09-14

This document initially recorded the user's request to document the forwarded ChatGPT proposal and try its direction in a later implementation. That initial request was documentation only and authorized no source/test/configuration edits, build, deployment, game execution, rollback, commit or push. The later implementation request and its evidence are recorded below. The user's intention to revert a failed attempt is not approval for a destructive Git command or a whole-file restoration.

Repository: `C:\Vtuber_Souorce_Code\LAVI`; documentation comparison HEAD: `861adc005552a93f66d0299275de42ae71f587ab`; branch: `test/automatic-deposit-checkpoint-20260831`. The worktree was clean before this documentation change. The [implementation contract](chatclef-find-implementation-contract-2026-09-14.md) describes the implemented checkpoint. Earlier design and verification records remain dated evidence.

The later user request explicitly authorizes implementing this direction, its
companion logs, focused tests and repository build verification. The initial
documentation-only ledger below remains historical. See the implementation record
at the end of this document for the preceding Tracker source boundaries and separate evidence.

Preceding Tracker checkpoint status: source `COMPLETE`, clean 1.20.1 production artifact verified,
focused tests `236/236 PASS`, live acceptance `NOT_RUN`. The canonical multi-version
build still fails five unchanged 1.21.1 errors. The ledger immediately below
describes the initial documentation-only request, not the accepted source state.

## Current exploration implementation follow-up

The later [authorized exploration unit](chatclef-find-exploration-contract-2026-09-14.md#authorized-implementation-unit-2026-09-14)
records implemented bounded discovery for both entity modes, including no-radius
Tracker observation and same-target revalidation before report or safe approach.
Player/item64 and block32 scopes are unchanged. Verification of exploration is
in progress; the Tracker checkpoint's 236-test evidence and artifact hashes
remain historical evidence of that preceding source.

## Earlier exploration planning (historical documentation-only snapshot)

The user subsequently requested documentation of observe -> if absent, explore
and reobserve -> when found, stop exploration -> report coordinates or perform
the requested approach. The [FIND exploration contract](chatclef-find-exploration-contract-2026-09-14.md)
recorded the then-future extension, with source `NOT_IMPLEMENTED`
and tests/build/live acceptance `NOT_RUN` at that planning boundary.

Its initial scope is eligible mobs in both entity modes. Report permits search
travel and stops intentional target-directed travel after discovery. Approach
shares no-radius Tracker discovery and then retains the existing supported safe
movement. Player/item64 and block32 scopes are preserved without exploration.
The per-scan current origin ranks the first completed local discovery; the
original parent origin and cumulative limits bound the whole search. The later
source change must preserve one parent terminal, STOP/defense priority and
exploration quiescence before revalidation or handoff.

The tracker unit's scope and prohibitions below continue to describe that
implemented unit and its historical design. They do not prohibit the separately
implemented exploration extension. Existing tests, logs and hashes remain
evidence of their recorded source, not acceptance of exploration or proof of
the earlier zero-match cause. The new numerical exploration limits are proposed
defaults; no executable constants or wire fields are changed here.

### Initial documentation-only ledger (historical)

```text
DIRECTION_STATUS: DOCUMENTED / NOT_IMPLEMENTED
MISSED_VILLAGER_ROOT_CAUSE: UNKNOWN
NEW_DIRECTION_BUILD: NOT_RUN
NEW_DIRECTION_DEPLOYMENT: NOT_RUN
NEW_DIRECTION_LIVE_ACCEPTANCE: NOT_RUN
COMPANION_LOG_IMPLEMENTATION: NOT_APPLICABLE
NOT_APPLICABLE_REASON: This change has no execution semantics; it documents future instrumentation.
```

## Tracker unit scope and expected behavior (historical direction)

The sections below preserve the original Tracker-only direction and its later
implementation record. Statements about report-only radius removal, no exploration
and initially bounded approach apply to that checkpoint. The current exploration
unit above supersedes those mob policies without rewriting its historical evidence.

Adopt the existing engine's entity tracking and candidate-selection foundation below the attack Task lifecycle. Keep FIND's own finite observation, command admission, operation identity, result verification and Korean UI/TTS delivery. Do not make FIND a modified attack parent with only the final attack call removed.

The prospective behavior change is limited to mob searches with `target_kind=entity` and `mode=report`, including omitted mode. Remove the hidden default 64-block distance cutoff for that path. Its scope is the relevant entities currently observable by the client, with a fresh authoritative tracker view. It does not promise visibility into unloaded chunks or knowledge of every entity in the world.

| Path | Implemented checkpoint | Documented follow-up |
| --- | --- | --- |
| Mob entity report | 64-block radius from frozen admission origin | Current client-observable candidates; no default distance cutoff; no FIND movement |
| Exact player-name report | Initial release support, 64-block radius | Preserve support and radius |
| Dropped-item report | 64-block radius; actual dropped stacks only | Preserve; no inventory/container search or pickup |
| Block report | 32-block radius and bounded loaded-block cursor | Preserve |
| Explicit entity/player/block approach | Initially observed candidate and existing conservative finite movement | Preserve observation radii, safety, lifecycle and movement limits |
| Item approach | Rejected | Preserve |
| New-region exploration | Not implemented | Separate future extension; not part of this implementation direction |

For example, a valid resident already present in the client-observable candidate set at distance 80 from the frozen origin would become eligible for entity report. Under the old 64-block policy it is excluded. This is an expected difference to test, not proof that distance caused the recorded miss. An optional user-specified radius is a possible future feature; no new command argument or protocol field is approved here.

An entity report beyond 64 blocks does not expand a later approach request. Approach performs its own initially bounded observation and retains the 64-block entity radius; if the target remains outside that scope, it can return a completed miss. No previous report silently supplies a movement target or authorizes exploration.

## Existing owners and reuse boundary

The relevant source boundaries are [EntityTracker](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/trackers/EntityTracker.java), [DoToClosestEntityTask](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/entity/DoToClosestEntityTask.java), [AbstractDoToClosestObjectTask](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/AbstractDoToClosestObjectTask.java), and the existing [FIND observation Task](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/observation/task/FindObservationTask.java).

`EntityTracker.getClosestEntity()` chooses by squared distance without FIND's 64-block cutoff. However, it filters unreachable-blacklisted entities before evaluating the caller predicate and does not itself call `ensureUpdated()`. Tracker refresh occurs through other boundaries, including `entityFound()`. Entity buckets use runtime classes; requesting only an exact `MobEntity.class` bucket cannot be assumed to include all mob subclasses. The ATTACK path's candidate query and its absent-candidate exploration must be distinguished.

Before implementing, inspect the actual refresh-to-selection call chain and use existing authoritative APIs where they satisfy the complete contract. If they cannot provide fresh, bounded observation without the movement blacklist, identify the smallest necessary owner-local query change and apply the upstream modification gate. This document does not establish that such an engine edit is necessary or approve a new generic framework. Keep FIND policy, outcomes and operation state out of generic engine classes; do not reorganize upstream classes.

The query contract must satisfy all of the following:

- Search the relevant runtime subclasses from the same refreshed client-world tracker view. Preserve currently registered vanilla and modded mob IDs and existing alive/not-removed mob matching.
- Treat existence and reachability separately. An unreachable blacklist may inform movement, but must not hide an existing candidate in position-report mode. Do not clear or change the blacklist to obtain an observation.
- Select the nearest eligible candidate by the frozen admission origin and preserve the existing deterministic tie rule. Defense movement must not silently redefine the origin.
- Apply the revised entity-report distance policy consistently in initial selection and final live-target revalidation. Preserve the radius branches for player/item reports and all approach observations.
- Revalidate target identity/type, current world, dimension, player and resource/catalog binding before committing a result. Cached coordinates alone are insufficient.
- Retain the five-second monotonic observation deadline and 4,096 actual entity-visit limit. Account for actual tracker refresh and candidate iteration costs; a cap around a later predicate does not bound earlier full-world refresh work. A nearest-query method name is not proof of finite work or completeness.

Full promised scope must complete before success. Retain the existing result matrix: an incomplete or exhausted scan returns `OBSERVATION_BOUNDS_EXHAUSTED`, unsatisfied, without candidate coordinates, even if a candidate was seen. The forwarded suggestion to report partial-found results is not adopted in this follow-up. Adopting it would require separate reconciliation of result profiles, validators, UI/TTS wording and the meaning of nearest/completeness.

## Initial checkpoint source boundaries (historical)

The following describe the initial checkpoint before the separately authorized implementation below:

| Initial checkpoint owner | Initial checkpoint behavior | Required follow-up |
| --- | --- | --- |
| [MinecraftFindObservationPort.scanEntities](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/observation/MinecraftFindObservationPort.java) | Direct client-world entity iteration; entity/player/item scans use the 64-block filter | Reuse the proven observation query and remove the distance filter only for entity report |
| `MinecraftFindObservationPort.revalidate` | Applies the 64-block filter again after live identity/type validation | Use the same kind/mode policy as selection; preserve binding and identity validation |
| [FindObservationOperation.startOwned](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/observation/FindObservationOperation.java) | Emits radius 32 for blocks and 64 for every other kind | Describe entity-report's actual client-observable scope; do not continue logging a 64-block cutoff for that path |
| `FindObservationOperation.tickOwned` | Checks the deadline before and after a synchronous entity scan | Account for actual refresh/iteration work and check the deadline at the bounded work owner |
| [FindCandidateSelection.nearer](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/observation/FindCandidateSelection.java) | Uses frozen-origin squared distance, then stable UUID ordering for entity ties | Preserve the same ranking; do not inherit a different tie rule from tracker iteration order |

Define the candidate-membership and refresh tick/generation boundary used to claim complete scope before implementing the query. A fresh view and a finished scan are separate facts. If work spans ticks, keep progress and cumulative limits attached to the same operation; repeated refresh must not reset the cursor, counters or deadline. Live revalidation still decides whether the selected target remains valid, and must not start a new discovery or exploration loop.

The existing five-second deadline is an operation deadline checked by the Task; it is not proof that a blocking scan or refresh is interruptible, nor a per-client-tick time cap. Likewise, 4,096 visits around candidate filtering do not bound an earlier full-world refresh or an unbounded snapshot copy. Inspect and bound work actually initiated by FIND without redesigning the engine's unrelated automatic tracker lifecycle. Any necessary per-call/tick bound must be specified and verified with the owner-local implementation; do not claim a guarantee from checks that run only after expensive work returns.

## Wire compatibility

The existing `observation_scope` is `loaded_entities` for mob searches, `loaded_players` for players, `loaded_dropped_items` for items and `loaded_blocks` for blocks. These are target-kind identifiers, not numeric-radius declarations. The current closed result payload has no radius field and its validators do not enforce the old 64-block cutoff through candidate coordinates. The documented entity-report change can therefore retain the existing field set, profile versions and scope literals.

Keep `loaded_entities` for both entity modes; their internal observation policies differ under this direction. Do not add a radius sentinel, new scope literal, origin field or scope-policy field to the wire payload. Record the actual policy in bounded owner logs and describe the checked client scope in the result wording. Any future wire extension requires a separate contract update rather than bypassing the strict validator.

## Preserve FIND lifetime and results

Keep the current input/catalog interpretation, trusted final-ASR admission, STOP/busy checks, semantic Task equality, immutable operation identity, original deadlines and cumulative limits. Automatic defense, evasion and survival retain their owners and priority. Temporary preemption is not a terminal failure; resume only after revalidating the existing binding and target. STOP/root replacement wins over late results.

Position report requests no movement, equipment changes, killing, pickup or interaction from FIND. Automatic defense remains independently permitted by its existing policy. Keep one-time result commitment and matching Task-completion proof. Report a completed miss as "not observed in the checked client scope", never as absence throughout the world. Preserve current catalog and wire schemas, outcome distinctions, and equivalent Korean screen/voice meaning.

## Exploration exclusion in the tracker implementation unit

Do not attach the existing closest-object absent-candidate Wander fallback to report or approach. The [TimeoutWanderTask](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/movement/TimeoutWanderTask.java) can return `KillEntitiesTask` for a nearby mob when progress fails. Its default infinite-distance form does not finish merely because an internal progress timer expires. Removing a final attack call or adding an outer timer therefore does not establish a noncombat, finite FIND exploration path. [GetToEntityTask](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/movement/GetToEntityTask.java) also contains a Wander fallback; its name is not a safety guarantee.

A future explicitly requested exploration feature needs its own admitted intent, finite time/space/retry limits, noncombat absent-candidate behavior, cleanup and revalidation contract, and tests. It must preserve normal automatic defense while excluding exploration-owned attack/kill fallback. Any command or protocol extension must be reconciled before implementation. Do not create placeholder Tasks, enums, settings, directories or wire values for that future feature now.

## Bounded companion-log plan

Add or retain logs with the later source change at the actual decision owners, using the existing diagnostic delivery contract. Record observations of decisions already made; diagnostic mode, counters, suppression or emission failure must never select a target, retry, timeout, movement, cleanup or terminal outcome.

Record the resolved target kind/ID and mode; frozen origin and binding; tracker refresh status and relevant tick/time; distance-policy branch; visited and matched counts; scope completeness and the actual exhausted limit; selected candidate position/distance; final revalidation outcome; preemption/resume; and terminal reason. Required first transitions and terminal output need reserved bounded capacity that ordinary summaries cannot consume. Verify the physical output of required causal boundaries and the terminal in `BOUNDARY` mode without enabling global VERBOSE merely to make events visible.

For candidate exclusions, distinguish mutually exclusive first-rejection counters from overlapping raw-ID diagnostic counts. Use `not_evaluated` or `read_unavailable` where a check did not run or its evidence is unavailable; never invent zero counts or false flags. Log a summary and at most three candidate samples per relevant boundary. Reuse captured decision data rather than querying or refreshing the tracker again for logging. Exclude raw player names, entity UUIDs, NBT and conversation text. The existing entity-summary formatter and VERBOSE-only ATTACK events must not be reused without checking privacy and actual delivery.

If an authorized ATTACK comparison runs later, capture the actual query selection or no-candidate-to-Wander transition, its origin and snapshot timing. This is prospective instrumentation. New logs cannot retroactively prove the cause of the earlier FIND miss.

## Existing evidence and limits

The implementation-stage `NOT_RUN` entries remain valid for that stage. Later read-only inspection found the same 1.20.1 JAR in the repository build output and active CurseForge instance:

```text
JAR: chatclef-1.20.1-0.18.23.jar
BYTES: 9002306
SHA256: B0607BD9D6723C1DDEEE8AB346BAE120572876E44932A14F4B3FC157D8C8A027
INSTANCE: C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01
```

In the captured 2026-09-14 log, FIND started at 19:55:39 with entity/report, `minecraft:villager`, radius 64, five seconds and 4,096 visits. Operation `40b51ef7-f95c-41a5-b568-a8ce760796fc` recorded 72 visited, zero matched, complete scope, and `NOT_OBSERVED_IN_LOADED_SCOPE`. The 72 visits are loop visits before all eligibility filters; they are not 72 residents or 72 valid in-radius targets.

ATTACK started at 19:55:57 and its 19:56:03 Task chain included exploration/Wander. The user observed that ATTACK finds and attacks residents. The captured lines establish the Task start and exploration, but do not identify the resident selected or prove a kill. The two commands did not share a proven simultaneous origin/candidate snapshot. The exact reason for FIND's zero matches remains `UNKNOWN`; available logs do not provide the target's raw ID, distance or first rejection reason.

These facts establish a later matching-artifact FIND execution with a miss, not successful live acceptance of FIND or execution of this new direction. The targeted 1.20.1 build/test evidence and the separate canonical multi-version build failure are retained in the [local build report](../../../logs/find_verification_20260914_194635_582/build-report.md). Neither is runtime proof.

The [local captured handoff](../../../logs/find_chatgpt_handoff_20260914_200150_361/HANDOFF.md), [key excerpts](../../../logs/find_chatgpt_handoff_20260914_200150_361/KEY_LOG_EXCERPTS.txt), and [snapshot manifest](../../../logs/find_chatgpt_handoff_20260914_200150_361/snapshot-manifest.json) preserve the read-only log/artifact evidence. `logs/` is Git-ignored; these links reference local evidence, not portable committed attachments. Historical hashes and transcripts must not be rewritten as new acceptance.

## Later implementation and verification unit

Choose the smallest complete change at the current entity-query and FIND delegation/distance owners, including bounded logs, focused tests and contract updates. Preserve existing input, catalog, lease/Mixin, lifecycle, projection and movement owners. Split only genuinely independent new responsibilities under the current [AGENTS.md](../../../AGENTS.md) rule; do not add a parallel manager, transport, session, logger or backend implementation. Apply the [ChatClef upstream integration direction](chatclef-carryon-integration-direction.md) and [backend separation contract](minecraft-backend-separation.md). Fabric ChatClef remains the only approved backend.

Before a later source change, identify exact files/hunks and an independently reversible unit against the intended checkpoint. A possible unit consists of the proven necessary observation-query adaptation, entity-report delegation and distance revalidation, their companion logs/tests, and corresponding docs. Its final path/hunk set must come from source inspection; this document does not approve whole-file overwrite, deletion or rollback of unrelated changes.

Verification must cover these distinct cases:

- A visible vanilla resident at 5–10 blocks; an already observed resident beyond 64; distance/height boundaries; unreachable-but-existing candidates; and registered modded mob IDs/runtime subclasses. Compare ATTACK and FIND using captured origin/candidate timing rather than assuming sequential commands see the same state.
- Freshness, stale/removed targets, resource/world replacement, deterministic nearest selection, and actual refresh/iteration limits. Partial scans must retain the current exhausted outcome without coordinates; completed misses must state the checked scope.
- No FIND-requested movement, attack, equipment change, pickup or interaction in report mode. Preserve exact player-name support, player/item 64-block limits, block 32-block limits, item-approach rejection and all existing conservative approach conditions.
- STOP, root replacement, defensive interruption/resume and original deadline retention; one immutable result and matching completion; consistent Korean UI/TTS meaning; and actual bounded diagnostic output with truthful unavailable evidence.

### Manual acceptance examples for the later change (NOT_RUN)

Use these only after authorized deployment of an identified JAR to a separate test world. Examples assume a completed scope, valid binding and an eligible target still present at final revalidation. Neither setup nor gameplay is executed by this documentation review. Direct commands use native Minecraft chat; Korean requests use LAVI chat and final trusted microphone recognition.

| Request/setup | Expected behavior after the documented change |
| --- | --- |
| `@find entity minecraft:villager` and `@find entity minecraft:villager report`; resident observed at 5–10 blocks | Equivalent report results; FIND requests no movement or attack |
| Same entity-report commands; nearest eligible resident already client-observable beyond 64 blocks | Report the resident after complete observation and live revalidation |
| `마을 주민 찾아줘`, repeated through LAVI chat and final microphone input | Same target/mode and verified Korean meaning as direct entity report |
| `@find entity minecraft:villager approach`; only eligible resident remains beyond 64 blocks | Preserve the approach observation cutoff; completed miss, no inherited report target or exploration |
| `@find player Steve`; exact player within the existing 64-block scope | Preserve literal player-name report and existing radius |
| `@find item minecraft:diamond`; actual dropped diamond within 64 blocks | Report the stack without pickup; inventory/chest contents do not satisfy the request |
| `@find block minecraft:chest`; loaded chest within 32 blocks | Report the block without opening it; preserve the block cursor limits |
| Entity scan exhausts its actual work/deadline limit after seeing a candidate | Unsatisfied exhausted result without candidate coordinates; no retry or wandering |

Run focused verification and the required clean build only within a later authorized implementation/build scope. Record canonical build, targeted build, packaged JAR identity, deployment, live acceptance and audible voice checks separately. This documentation work executes none of them.

## Preceding Tracker implementation record: source boundaries before edit

The user subsequently requested the six-step implementation and explicitly required
focused files/packages for independent responsibilities. The active implementation
comparison is HEAD `861adc005552a93f66d0299275de42ae71f587ab`. The five modified
documentation files and this untracked direction document are preserved. No source,
test or configuration changes were present before implementation.

Evidence mode is a source-proven explicitly requested observation contract gap,
not a root-cause fix for the earlier miss. Runtime reproduction/correlation/tick
for the new query are `NOT_RUN`. Existing source proves both 64-block filters and
the start-log radius; tracker APIs either refresh unbounded collections or apply
movement reachability. Cached membership cannot be assumed fresh because tracker
dirtying precedes Task evaluation and peaceful/disabled defense may skip refresh.

The upstream unit is one additive generic method in
`adris/altoclef/trackers/EntityTracker.java`, `getClientObservedEntities()`.
Pre-edit SHA-256 is
`EB96B3F446028CF65AE00F28984F91CCDC867BB3540577D5495F8372DF082459`.
It returns current client-observed entity membership on the client thread, without
`ensureUpdated`, copies, blacklist/filter/ranking policy or mutable query state.
Read-only bytecode inspection of the cached exact 1.20.1 Yarn build10 artifact
shows `getEntities()` opens an unmodifiable live map-values iterable without a
full-world copy. FIND consumes it within one synchronous evaluation and bounds
every actual visit and deadline check. This is live source membership, not a
tracker-cache refresh or an atomic persisted snapshot.

Containment: LAVI already owns direct world reads and can enforce query bounds,
but existing public Tracker APIs cannot satisfy the requested owner reuse with
freshness, absence of reachability filtering and bounded refresh. Carry On is
unrelated. A task-local helper/parent does not change those API semantics. The
one generic source getter enables LAVI containment; it carries no FIND types or
policy. Existing tracker refresh/reset/classification/blacklist methods and
TaskRunner, defense, input, retry, timeout, goals, paths and cleanup are unchanged.
Report still creates no movement child or mutation-capable resource.

The LAVI source unit is the observation port's delegation to focused entity and
block observation packages, entity report's query/radius/revalidation logic,
operation-to-query deadline propagation and start-log scope, and bounded diagnostic
capture/delivery. Existing command admission, catalog, lease/Mixin, approach safety,
result field set and Korean rendering remain preserved. Tests exercise actual
entity readers and bounded iteration, plus existing operation/lifecycle evidence.

The exact independently reversible unit is the added Tracker method/adjacent
marker, the named FIND observation/diagnostic hunks and new focused files/tests,
and their contract updates relative to this checkpoint. No whole-file restore,
deletion of unrelated files, reset, cleanup, commit or push is performed. The
generic regression matrix covers existing tracker reachability/refresh boundaries,
FIND player/item/block/approach policies, STOP/preemption and diagnostic failure
isolation; container interaction, Carry On, doors/beds/buttons, placement/item use
and Baritone movement have no changed execution boundary and no live acceptance
claim. Source tests/build and gameplay evidence are recorded separately below.

Source implementation status: `COMPLETE`; repository verification: `VERIFIED_AUTOMATED`.
Deployment, live gameplay, audible voice,
external-instance copy, manual cache mutation, commit and push: `NOT_RUN`.

The first verification ran in `logs/find_verification_20260914_210625_418`.
Canonical clean build stopped at the same five unchanged 1.21.1 errors. Targeted
1.20.1 production compilation/remap succeeded, but focused tests returned
232 started, 210 successful and 22 failed. These results are not acceptance.
Exact failures identified test-world allocation before registry bootstrap, and
an emission-token leak after a nonfatal AssertionError bypassed the diagnostic
coordinator's existing failure settlement. The fixture allocation order is fixed.
The necessary diagnostic-only companion hunk is the formatter/sink exception
boundary in `diagnostics/session/emission/DiagnosticEmissionCoordinator.java`:
settle AssertionError through the existing `failed`/`failEmission` failure outcome,
with unchanged budgets, retries and gameplay. Its inverse catch
hunks and tests are part of this independently reversible implementation unit.
Source-query read failures also record the actual read stage/type; diagnostic
nonmob ID and sample getters remain isolated unavailable evidence. Final build
verification follows after these exact corrections and the required adjacent
engine-divergence marker.

<!-- 20260914_kpopmodder: Record final automated acceptance and retain failed runs separately from live evidence. -->
## Final Tracker implementation and verification (historical checkpoint)

| Responsibility | Current focused owner |
| --- | --- |
| Client/catalog binding and observation delegation | [MinecraftFindObservationPort](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/observation/MinecraftFindObservationPort.java) |
| Finite lifetime, actual counts and immutable outcome | [FindObservationOperation](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/observation/FindObservationOperation.java), with budget/log overloads in `FindObservationPort` |
| Live entity matching, frozen-origin selection and revalidation | [MinecraftFindEntityObservation](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/observation/entity/MinecraftFindEntityObservation.java) |
| Existing bounded block-state observation | [MinecraftFindBlockObservation](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/observation/block/MinecraftFindBlockObservation.java) |
| Pure candidate identity hashing | [FindIdentityDigest](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/model/FindIdentityDigest.java) |
| Observational exclusion counters and samples | [FindEntityScanDiagnostics](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/find/diagnostics/FindEntityScanDiagnostics.java) |
| Existing trace reservation/delivery | `FindLog`, `FindTraceReservation`, `FindLogEmitter`; nonfatal assertion settlement at the existing `DiagnosticEmissionCoordinator` formatter/sink boundaries |

Only entity report uses the additive Tracker getter and omits the radius check.
It does not call `ensureUpdated`, copy membership, retain an iterator across ticks,
modify blacklist state or reuse ATTACK movement/combat Tasks. Other entity kinds
and explicit approach preserve their existing source/radius policies. Exactly
4,096 available entities can complete; an additional available entity exhausts
the visit bound without consuming that extra entity. Deadline/binding checks
surround iteration and final live revalidation. A seen prefix candidate never
satisfies an incomplete or failed scope.

Companion owner events are `STARTED`, `ENTITY_QUERY_STARTED`,
`ENTITY_QUERY_COMPLETED`, `OBSERVATION_COMPLETED`, `ENTITY_REVALIDATION_RESULT`,
`REPORT_TERMINAL` and `TRACE_TERMINAL`, with existing suspend/resume and lifecycle
events preserved. Query summaries distinguish authoritative visit/match counts,
first-rejection counters, overlapping raw-ID comparisons and not-evaluated or
unavailable evidence. A read failure records stage/type and makes incomplete
diagnostic counters `UNAVAILABLE`. Success samples are capped at three 80-character
values with explicit truncation; read failure retains one prefix sample and marks
the others `OMITTED_READ_FAILURE`. Revalidation labels original selected and current
coordinates separately. Raw UUIDs, player names, NBT and command text are excluded.

The established BOUNDARY output route preserves the shared 5,000-slot session cap
and 128 FIND-reserved slots. Tests capture actual formatter/stdout output under
ordinary-cap pressure and with 128-character context IDs/long samples; required
first semantic boundaries and terminal survive, signatures remain complete, and
tested records stay within 2,048 bytes. OFF/BOUNDARY and failing actual diagnostic
sinks preserve results; failed tokens settle without retry/refund. These are
`VERIFIED_AUTOMATED_OUTPUT` facts, not live Minecraft emission proof.

Final focused verification: `236 started / 236 successful / 0 failed / 0 aborted /
0 skipped / 0 container failures`, completed in 43 seconds. Controlled real entity
fixtures and existing integration tests cover distant residents, subclasses,
actual iteration/deadline bounds, retained other-kind/approach policies, stale
bindings, STOP/root replacement, completion immutability and diagnostics isolation.
The subclass fixture uses a vanilla ID; separately registered modded-ID and live
voice acceptance remain `NOT_RUN`.

The earlier forced, cache-disabled 1.20.1 clean invocation compiled/remapped and
validated the fresh artifact but initially failed five fixture/assertion cases
(`236/231/5`). After test-only Random initialization and stdout-encoding corrections,
the focused rerun passed all 236. All 12 production-source hashes remained identical
to that clean invocation. Canonical clean build still fails the five unchanged
root1.21.1 NBT/enchantment errors; it is not reported as a passing full build.

Packaged JAR: `versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar`, 9,014,621
bytes, SHA-256 `99D97BF3FA6E82DCFB596C269B92F3C14520D123F198227CB1D894BFB3B3B261`.
It contains 49 FIND classes with Java-17 major version 61, all four new focused
owners, no test artifacts, and the packaged Tracker getter confirmed by `javap`.
The [local verification report](../../../logs/find_verification_20260914_212424_425/build-report.md)
links exact manifests/results and retains earlier failed runs; it is gitignored
local evidence rather than a committed acceptance record.

The independently reversible source/test unit comprises the exact focused-owner,
port/operation, Tracker and diagnostic-delivery hunks above, the actual entity
fixture/getter/query/output tests, and the coordinator/operation regression tests.
No rollback was executed. Final documentation updates preserve historical evidence
and are outside compiled inputs. Deployment, runtime Mixin injection, game/world
execution, audible microphone/TTS, external-instance writes, cache mutation,
commit and push are `NOT_RUN`. The earlier 72-visit/zero-match cause remains `UNKNOWN`.

The later user-requested external-PowerShell
[forced clean build](../../../logs/find_verification_20260914_213023_643/build-report.md)
passed exact 1.20.1 compilation/remap/validation and all 236 focused tests in the
same invocation, with 36 tasks executed and the identical JAR hash/size above.
Its canonical multi-version command still failed the same five unchanged 1.21.1
errors. This is later automated evidence for the tracker source, not a build
performed by this documentation request or acceptance of exploration.
