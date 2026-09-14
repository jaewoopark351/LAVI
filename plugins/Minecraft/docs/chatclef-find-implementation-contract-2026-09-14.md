<!-- 20260914_kpopmodder: Record the user-authorized FIND implementation unit separately from the earlier documentation-only review. -->
# Fabric ChatClef FIND implementation contract

The implementation request recorded here authorized repository-local implementation, companion logs, focused tests and the canonical clean build. It explicitly required focused files and packages when code has two independent responsibilities. Player-name search is included from the initial release by the user's explicit reply. Earlier documentation-only evidence remains historical.

Initial implementation checkpoint: repository `C:\Vtuber_Souorce_Code\LAVI`; comparison HEAD `2ecc1283a1e5cf48357dd0e263c2526e7bcb64e2`; branch `minecraft-plugin-fix/alto-clef-infinite-loop`. Existing dirty README and FIND design/pre-change documents were preserved. No deployment, external-instance copy, live-world execution, process control, cache mutation, commit or push was authorized in that unit. Later follow-ups retain their own checkpoint identities below.

<!-- 20260914_kpopmodder: Record the separately authorized tracker implementation without replacing initial checkpoint evidence. -->
## Exploration implementation follow-up

The later implementation request adds a single mob FIND parent with bounded
loaded-waypoint exploration and periodic live Tracker observations. Both modes
discover without a 64-block cutoff, then release exploration resources and
revalidate the same target before reporting or safe approach. The exact frozen
limits, independent owners, preservation snapshot and dry-flat-terrain limitation
are recorded in the [exploration implementation unit](chatclef-find-exploration-contract-2026-09-14.md#authorized-implementation-unit-2026-09-14).
Player, item and block searches retain their preceding paths. New parent clocks
start at Java submission; owned retirement is separate from diagnostic closure.
Final forced clean 1.20.1 verification and 309/309 focused Java tests passed.
The Python Minecraft suite passed 1,354 tests and 14,407 subtests, with two skips.
The canonical multiversion build still fails at five unchanged 1.21.1 errors.
The [final exploration verification record](chatclef-find-exploration-contract-2026-09-14.md#final-exploration-verification-2026-09-14)
contains the current source manifest, fresh JAR identity and distinct limitations.
Deployment and live acceptance are `NOT_RUN`.

## Earlier exploration planning: documentation only

The user's later documentation request records this new mob FIND flow in the
[FIND exploration contract](chatclef-find-exploration-contract-2026-09-14.md):
complete local observation; if absent, bounded exploration and reobservation;
when found, stop exploration and prove owned cleanup; then revalidate the same
target and report coordinates or perform the explicitly requested approach.
This extension is `DOCUMENTED / NOT_IMPLEMENTED`. No source, test, configuration,
build or live-game action is part of this documentation update.

For the future entity extension, omitted mode and `report` permit discovery
travel but no further intentional travel toward the discovered target;
`approach` permits the later supported safe movement. Both entity modes use
Tracker client-observable discovery without a default 64-block cutoff, including
same-target revalidation. This intentionally supersedes the existing entity
approach observation cutoff; it does not widen approach path, planning or safe
range limits. Player/item local64 and block32 scope remain unchanged and do not
gain exploration in this first extension.

A single parent retains the original root, binding, cumulative limits and final
immutable result. Each new completed local scan ranks from its frozen current
origin; the original admission origin bounds total exploration displacement.
Nearest means nearest in the first fully completed local scan with a match,
not globally nearest. Partial scans cannot establish discovery. An internal
miss cannot emit a command terminal or close the parent's diagnostic trace.
Defense/STOP, owned child/path/input cleanup and strict Korean UI/TTS projection
follow the linked contract. Its numerical limits are proposed design defaults,
not current constants or live acceptance.

The reviewed extension binds one monotonic clock at accepted Java user-root
submission. Discovery time includes exploration cleanup and live revalidation;
latching a candidate before expiry does not authorize a later expired handoff.
The 120-second maximum is retained as a new post-cleanup approach phase budget,
an intentional change from the current operation's first-start clock. Freeze a
separate planning origin at that validated handoff without changing parent
identity, scan ranking evidence or cumulative budgets. Sampled movement is a
measured limit, not proof of actual route length. The linked closed phase-reason
matrix and authoritative terminal/discovery snapshot must accompany strict
projection and Korean rendering; arbitrary reason text cannot establish finding.
Unproven cleanup cannot manufacture matching completion or a satisfied result.

The source and evidence sections below describe the preceding implementation.
Their no-exploration/no-report-movement/initial-approach requirements remain
accurate for that source; they are superseded only for the future entity
extension. Existing automated tests and packaged identity are not exploration
verification. The recorded zero-match miss's cause remains `UNKNOWN`.

## Preceding source: tracker implementation follow-up

The user subsequently authorized implementation of the
[tracker-reuse direction and implementation record](chatclef-find-tracker-reuse-direction-2026-09-14.md).
Source implementation is complete; targeted repository verification is
`VERIFIED_AUTOMATED`, and this follow-up is `NOT_RUNTIME_VERIFIED`. The initial checkpoint
behavior and verification snapshots below remain historical. The recorded
72-visit/zero-match villager miss's root cause remains `UNKNOWN`; this source change
does not retroactively establish it.

Mob searches with `target_kind=entity` and `mode=report`, including omitted mode,
now obtain live client-world membership through
[EntityTracker.getClientObservedEntities](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/trackers/EntityTracker.java).
This small, read-only client-thread getter borrows the current iterable; it does
not refresh or copy tracker caches, query exact runtime-class buckets, or apply or
mutate the movement blacklist. FIND owns exact registered mob-ID matching,
frozen-origin nearest ranking and stable ties. Initial selection and final live
revalidation no longer apply a default 64-block cutoff to entity report.

Player/item reports retain their 64-block radius, blocks retain 32, and all
approach conditions and observation radii remain unchanged. A report beyond 64
blocks does not supply or expand a later approach target. No new exploration,
acquisition, combat or report movement is added. Existing `loaded_entities` and
the other wire scope literals, field sets and profile versions are unchanged;
radius is an internal observation policy and owner-log value, not a wire field.

The existing port delegates entity and block observations to focused
`observation/entity` and `observation/block` owners. Entity iteration checks the
original five-second monotonic deadline and caps actual visits at 4,096. Complete
scope requires exhausting one synchronous live iterable and revalidating the
binding; partial/exhausted scans remain unsatisfied without coordinates. These
checks do not promise interruption of an individual engine call or a hard
per-client-tick time bound. FIND initiates no tracker refresh or membership copy.
Bounded companion logs record the no-radius policy, frozen origin, query outcome,
filter counts, selected candidate and live revalidation; diagnostics remain
observational. Their implementation, automated output verification and actual
Minecraft output are separate evidence in the linked follow-up record.

Final targeted verification: the 1.20.1 clean production compilation, remap and
access-widener checks succeeded in
[the clean production run](../../../logs/find_verification_20260914_212014_166/fabric12001.log).
That invocation ended in failure solely at five test-fixture/assertion failures;
it is not reported as a successful combined build/test invocation. After exact
test-only corrections, [the final focused run](../../../logs/find_verification_20260914_212424_425/fabric12001-focused.log)
passed with 236 tests started and successful, zero failed/aborted/skipped and zero
container failures, in 43 seconds. The final focused run is incremental test
verification, not a replacement for the separate clean production evidence. All
12 changed production-file hashes match the clean-run source manifest.

The clean 1.20.1 JAR is 9,014,621 bytes with SHA-256
`99D97BF3FA6E82DCFB596C269B92F3C14520D123F198227CB1D894BFB3B3B261`.
Its 49 FIND classes have Java class version61 and contain no test artifacts;
packaged bytecode confirms the live Tracker getter. This establishes packaged
identity and targeted automated verification, not runtime execution. The earlier
[canonical multi-version clean build](../../../logs/find_verification_20260914_211321_244/canonical.log)
remains `FAIL` at the same five unchanged 1.21.1 source errors. These log links are
local Git-ignored evidence. Deployment, live acceptance, registered modded-mob
acceptance and audible voice checks for this change are `NOT_RUN`.

The subsequently requested external-PowerShell build is recorded separately in
[the 21:30 build report](../../../logs/find_verification_20260914_213023_643/build-report.md).
Its canonical multi-version command failed with the same five unchanged 1.21.1
errors; its forced clean 1.20.1 compilation/remap/validation and focused suite
passed together with 236 started and successful, zero failures/aborts/skips or
container failures, and 36 executed tasks. The JAR hash and size above are
unchanged. This later clean acceptance replaces neither the earlier failed-run
transcripts nor the separate runtime evidence, and does not cover exploration.

## Initial checkpoint frozen behavior (historical)

`@find <entity|block|item|player> <namespace:id|playerName> [report|approach]` defaults to `report`. Chat and final trusted microphone text compile through the existing admission/lifecycle path. FIND rejection never falls through to GET, mining or acquisition. Literal player names use the existing 3..16 ASCII name contract.

Registry targets are grounded in the current runtime registry; Korean and English names come from resource translations. Verified aliases apply only to currently registered targets. Ambiguous names require a new explicit request. Dropped item entities are the only item-search scope; item approach and the empty `minecraft:air` item type are rejected. No inventory, chest-content, acquisition or new-region exploration is included.

Observation freezes the initial center/world/dimension, a 64-block entity/player/item radius or 32-block block radius, a five-second monotonic deadline, 4,096 actual entity visits, and 300,000 block cursor positions. Block work is capped at 4,096 cursor positions per tick with a soft two-millisecond work budget. Loaded-chunk checks precede block reads. Full promised loaded scope must finish before success; a candidate observed during an exhausted scan is not reported as FOUND. These are client-local observations, not server acknowledgement or proof of absence throughout the world.

Approach uses only an initially observed candidate. The original finite 120-second operation deadline survives defensive interruption and Task reset. Native calculate-only planning and native dry, flat traverse/diagonal steps are composed in a LAVI adapter; climbing, excavation, placement, jumping, item pickup and interaction are unsupported. Planning uses a soft five-millisecond native timeout, a hard 50,000 context-read bound and a 128-step path bound. Unsupported safety or routes end in a typed failure; they do not authorize another plan, Wander or exploration. Success requires current identity, dimension, kind-specific safe range, visibility and release of owned input.

Task scheduling, STOP/root retirement, defense, evasion and survival retain their existing owners. `onStop(null)` can be temporary preemption and is not STOP evidence. The immutable operation and counters survive `onStart` after reset. Native admission rejects an active nonidle user task before constructing or submitting FIND; an existing idle task remains eligible. Semantic Task equality is preserved. FIND does not call global `stop`, `forceCancel`, `clearAllKeys` or modify global settings.

## Catalog protocol

Use existing Fabric session/WebSocket owners and the existing `event` envelope type. `find_catalog_page` payload fields are `event`, `catalog_version=1`, `resource_generation`, `catalog_digest`, `connection_generation`, `page_index`, `page_count`, `record_count`, `complete=true`, and `records`. Records contain `target_kind`, `canonical_target_id`, `translation_key`, `korean_name`, `english_name`; only entity records additionally contain `eligibility=mob|non_mob|unknown`. No player names are catalogued.

Records are sorted by `(target_kind, canonical_target_id)`. NFC strings reject Unicode categories Cc, Cf, Cs, Zl and Zp; private-use and unassigned code points are permitted consistently by Java and Python. Translation keys are nonempty. IDs are at most 128 Unicode code points; keys and labels at most 256. The digest is lowercase SHA-256 over UTF-8 lines, each containing kind, ID, translation key, Korean name, English name and eligibility-or-empty, separated by TAB and followed by LF. Every record participates; there is no digest of a truncated list.

Caps: 50,000 records, 256 pages, 256 records or 64 KiB per page (whichever occurs first), eight MiB total, and ten monotonic seconds per exchange. Publish a validated complete immutable snapshot atomically. Pages are sequential with at most one pending send. Failed or incomplete exchanges cannot replace a complete snapshot; reconnect and resource replacement invalidate the old context. `find_catalog_invalidated` includes event/version/resource generation/connection generation and bounded reason `CATALOG_REPLACED|CATALOG_INCOMPLETE`. Session and connection generation are validated against authoritative existing ownership, not trusted merely because the payload contains them. A temporarily unavailable source sends invalidation at the latest observed resource generation so the receiver immediately retires that context; it never sends a stale zero generation after a published catalog.

## Terminal protocol

Only existing callback plus matching command-owned natural Task completion can project FIND evidence. Report: `fabric_chatclef_find_observation`, version 1, effect `find_observation`, mode `LOCATE_AND_REPORT`. Approach: `fabric_chatclef_find_approach`, version 1, effect `find_approach`, mode `LOCATE_AND_APPROACH`. `data.result_reason=matching_task_finished`, `data.result_fidelity=callback_plus_matching_user_task_event` are required.

Payload fields are `completion_mode`, `target_kind`, `observation_scope`, `find_result`, `find_satisfied`, `reason`. Registry targets require `canonical_target_id`, `catalog_digest`, `resource_generation` and forbid player identity. Player targets require `player_identity_digest` and forbid registry/catalog fields. The player request digest is SHA-256 of the exact literal requested name; the distinct candidate digest is SHA-256 of the observed UUID string. The former proves request binding; it does not claim an unobserved UUID on a miss. Java revalidates the actual name and UUID before satisfaction. Bound registry operations revalidate the current complete catalog digest and generation before observation, during approach and before success. Final publication revalidates that binding again: a reload after the immutable outcome was frozen yields existing UNKNOWN evidence without coordinates. The operation outcome is never rewritten by projection; native presentation gives the equivalent cautious Korean response.

Satisfied results additionally require `candidate_identity_digest`, canonical `dimension`, signed Java-int `x/y/z`; approach success also requires `safe_distance_satisfied=true`. Unsatisfied results forbid candidate identity, dimension and coordinates. Booleans are not coordinate integers. Foreign effect fields, unknown keys and mismatched mode/target/catalog/session/request evidence are rejected.

`FOUND_AND_REPORTED`, `FOUND_AND_IN_SAFE_RANGE`, `ALREADY_IN_SAFE_RANGE` are completed/satisfied. `NOT_OBSERVED_IN_LOADED_SCOPE` is completed/unsatisfied. `INVALID_TARGET`, `OBSERVATION_BOUNDS_EXHAUSTED`, `TARGET_LOST`, `CANDIDATE_NOT_REVALIDATABLE`, `INTERRUPTED`, `UNREACHABLE`, `TIMEOUT`, `INTERNAL_ERROR` are failed/unsatisfied. Existing user STOP cancellation is authoritative and emits the existing single stop response. Unknown proof remains cautious; it never becomes discovery or arrival. UI and TTS render the same verified Korean meaning using existing delivery/deduplication owners.

## Ownership and reversible unit

New Java packages: `lavi.minecraft.find/{command,model,catalog,observation,result,diagnostics,approach}`; the observation and approach Task adapters live in their respective focused packages. Main-source FIND implementation uses the existing MC12001 inactive `//$$` preprocessing convention; the raw root1.21.1 entrypoint is a no-op. New Python packages separate candidate/parse/resolve/compile, catalog validation/state, immutable binding, strict evidence and Korean rendering. Existing files receive only FIND registration, delegation, transport/composition and result projection hunks. The finite Task inherits the established Task methods solely for existing scheduling and matching completion evidence; behavior state is composed.

Input override has no native token ownership. A generic LAVI lease contract/ledger and one observational Mixin invalidate lease metadata when unchanged native input setters/clear calls execute. Native methods are neither cancelled nor modified; no FIND types enter generic engine methods. New explicit consumer claim/release operations release a key only while its exact lease remains current. The one Mixin JSON entry and its new files are an independent inverse unit. Baritone inspection baseline SHA-256: `F7B74C47B2CFC178EA9F2A3757116C738FC5A9448C2C608B4CB20108D9BD3157`.

Rollback is the inverse of new FIND files and exact FIND delegation/resource entries, plus the independently reviewable diagnostic reservation additions. It preserves all preexisting content and dirty user files; no rollback action is requested or performed.

## Companion logging and evidence

The shared diagnostic hard cap remains 5,000. Four admitted FIND traces reserve 32 single-record slots each atomically (128 total): at most 30 meaningful first owner/event/transition signatures plus one protected operation-outcome slot and one protected trace-retirement slot. Ordinary detail ceiling changes from 3,864 to 3,736; the critical reserve changes from 1,136 to 1,264. Existing other critical quotas are unchanged. Each reserved record uses the existing bounded formatter/sink and a 2,048-byte bound. Repeats and summaries cannot consume these tokens. Unused, unavailable and retired reservations are settled as non-output; accounting is not persistence evidence.

Diagnostic exclusion, OFF mode, deduplication, sink failure and reservations never change functional admission, retries, cleanup or outcomes. Normal terminal logs retain operation, actual decision values, reason and detailed-trace completeness without depending on diagnostic state. FIND-added diagnostic records exclude raw conversations, microphone data, player names, UUIDs and NBT. Command context is a closed allowlist of bounded correlation fields; raw `commandText` is not copied. Existing upstream command-log behavior is retained.

Coverage: command resolver owns validation/rejection; catalog capture/publisher/receiver own capture, generation replacement and actual transport stages; operation owns start/counters/candidate/bounds/terminal; approach owns plan/step/safety/input cleanup; existing bridge lifecycle owns matching completion/STOP; strict Python evaluator and renderer own schema rejection and Korean projection. Actual collection points are captured formatted logger output and tests of the existing transport/evidence/render path. UI display, audio playback and Minecraft log collection remain separate live evidence.

Verification status at contract creation: SOURCE_IMPLEMENTATION=IN_PROGRESS; FOCUSED_TESTS=NOT_RUN; CLEAN_BUILD=NOT_RUN; DEPLOYMENT=NOT_RUN (not requested); LIVE_GAME=NOT_RUN (not requested). Append final results after verification; do not rewrite historical review evidence as runtime acceptance.

## Reproducible repository verification

From `C:\Vtuber_Souorce_Code\LAVI`, run:

```powershell
.\venv\Scripts\python.exe -m pytest tests/minecraft_chatclef/find -q
.\scripts\verify-find-1201.ps1 -Canonical
```

The script records the required canonical `clean build --rerun-tasks` separately
from the exact1.20.1 clean/remap/validation and focused regression run. The
existing multiversion `preprocessTestCode` path also compiles the rawroot1.21.1;
the1.20.1 focused init script reuses the established named-registry bootstrap agent
and Jupiter selector runner to avoid unrelated-version compilation. It introduces
no dependency or version change. Logs use UTF-8 and outputs stay in the repository.
`-FocusedOnly` is an incremental troubleshooting mode, not clean-build proof.

## Manual game acceptance checklist (NOT_RUN)

Use a separate test world after a separately authorized matching-JAR deployment.
Repeat each Korean request through chat and final microphone recognition and
compare the verified UI and spoken meaning. This checklist is not execution
authorization and no world mutation was performed by the coding agent.

| Request | Expected behavior |
| --- | --- |
| `마을 주민 찾아줘` / `@find entity minecraft:villager` | Report an actually observed nearest eligible villager; FIND requests no movement. |
| `마을 주민 찾아서 가까이 가줘` | Approach the initially observed villager only if the supported dry flat safe route and ownership checks pass; otherwise report a typed failure. |
| `상자 블록 찾아줘` | Report the current loaded chest block, without opening it. |
| `떨어진 다이아몬드 찾아줘` | Report an actual dropped item stack, without requesting pickup. |
| `플레이어 Steve 찾아줘` | Search the exact literal player name in the current loaded scope. |
| `상자 찾아줘` | Ask to distinguish block from dropped item if both registry kinds match. |
| `떨어진 다이아몬드 찾아서 가까이 가줘` | Refuse unsupported item approach before Task submission. |
| `상자 안 다이아몬드 찾아줘` | Refuse container-content search; do not compile GET or acquisition. |

Also check complete bounded miss versus partial timeout, current task busy
rejection, automatic defense followed by resume with the original deadline,
STOP before first tick and during approach, moved/despawned targets, catalog
reload between observation and publication, and a mod target addressed by its
current registry ID. Unsupported mod/ranged/boss approach profiles fail safely.
For loaded-scope misses, do not claim absence throughout the world. Inspect
`FIND_*` lifecycle records and existing per-operation logs; compare operation,
resource/session generation and reason rather than interpreting an omitted
diagnostic trace as proof that the Task never ran. OFF/cap-excluded traces retain
bounded truthful terminal evidence but do not promise a complete detailed trace.

## Implementation-stage repository verification, 2026-09-14

This section preserves the initial implementation-stage results and action scope,
before the Tracker and bounded-exploration follow-ups. Later
read-only runtime evidence is linked above; it does not replace these build/test
results or establish successful live acceptance.

Source implementation was complete at that initial checkpoint. No commit, push,
external-instance copy or live-world action was performed.

| Evidence | Actual result |
| --- | --- |
| Python combined suite | 1,415 passed, 2 skipped, 14,547 subtests passed. Includes full `tests/minecraft_chatclef`, relevant root NL/extension/input/composition/compiler/schema/transport tests, app UI/TTS receipt wiring and backend separation. |
| Final1.20.1 clean verification | PASS; 36 tasks executed, clean/remap/validation/focused tasks forced with cache disabled. |
| Java selected regression suite | 210 started and 210 successful; zero failed, aborted, skipped or container failures. Includes FIND, input leases, root lifetime/STOP, catalog/admission/projection and shared diagnostic session tests. |
| Required canonical multiversion clean build | FAIL at `:1.21.1:compileJava`; five existing compatibility errors in unchanged source files, described below. It is not reported as a successful full build. |
| Fresh1.20.1 packaged source | 45 FIND classes, all major61 (Java17), FIND entrypoint and context filter, registered lease Mixin class/configuration and refmap present. |
| Actual Minecraft session, observation/movement acceptance and live logs | NOT_RUN. |
| Audible voice synthesis/playback | NOT_RUN; UI/TTS listener delivery was verified in the test harness. |
| Deployment/external instance/commit/push | NOT_RUN; not requested. |

Final command output and per-source hashes:
[verification output](../../../logs/find_verification_20260914_193909_030/result.json),
[canonical build log](../../../logs/find_verification_20260914_193909_030/canonical.log),
[1.20.1 clean log](../../../logs/find_verification_20260914_193909_030/fabric12001.log),
[source manifest](../../../logs/find_verification_20260914_193909_030/source-manifest.json).

Fresh artifact:
`plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar`;
9,002,306 bytes; SHA-256
`B0607BD9D6723C1DDEEE8AB346BAE120572876E44932A14F4B3FC157D8C8A027`.
This is build identity, not runtime Mixin/game acceptance.

The canonical1.21.1 errors are two `ItemStack.getNbt` uses in
`MinecraftToolEquipPort.java:71`, `ItemStack.hasNbt` at
`GotoMaterialInventory.java:67` and `:92`, and the enchantment registry-key/entry
type at `GotoMaterialSources.java:148`. `git diff HEAD` for those three exact
paths is empty. Their compatibility behavior was not modified during FIND work.
Earlier FIND Identifier and Java17 compile errors were corrected; the final
1.20.1 compile/remap and all selected tests pass. The existing wrapper-cache
permission issue was resolved through approved normal Gradle execution, not a
dependency/version or system-setting change.

Actual formatted Java stdout and Python catalog file-sink output were captured in
tests, including OFF, reservation exclusion, mode activation, sink failure and
raw player-command context filtering. Test fixtures explicitly distinguish the
new128-slot reserve from the unchanged5,000 hard cap and preserve the4998/4999/5000
cap/race/reset assertions. Injection of test data is not live Minecraft evidence.
Isolated Mixin transformation evidence is recorded separately after checking the
final packaged artifact identity.


Final isolated Mixin transformation: PASS for named inputs and the final packaged
artifact. The named Mixin/ledger bytes match the final named build exactly;
packaged transformation independently verifies the final JAR hash above.
The actual Sponge transformer adds the lease interface and four explicit APIs,
with one observer each before the unchanged setter (eight original instructions)
and clear method (four original instructions). No new native branch, cancellation
or exception interception is present. This is bytecode application proof, not a
live client/thread/gameplay test.

- [Named transformation log](../../../test/test_Isolation/minecraft/find_input_lease_mixin_application/output/3f0503495207404cb3a2af183062b411/smoke.log)
- [Final packaged transformation log](../../../test/test_Isolation/minecraft/find_input_lease_mixin_application/output/5fc29a367c334ca9a0dd9f89c779328e/smoke.log)
- [Final packaged input identities](../../../test/test_Isolation/minecraft/find_input_lease_mixin_application/output/5fc29a367c334ca9a0dd9f89c779328e/inputs.txt)

Packaged native target SHA-256:
`3E2BB4EC292891CE888901F0A0B6A4A9F8796B38447DE7646439E1EC50B3922D`;
transformed target SHA-256:
`4E05F55A7671A711E702E998D222CED6506D1A651AF0465D1F7E90EAA9FD8D83`.
