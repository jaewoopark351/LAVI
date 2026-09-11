<!-- 20260909_kpopmodder: Defined the docs-only Korean GOTO, new FIND, and full registered-command input-coverage contract. -->
<!-- 20260909_kpopmodder: Separated Python grammar recovery, LAVI-owned Java FIND behavior, and per-command public rollout into dependency-ordered rollback units. -->
<!-- 20260909_kpopmodder: Bounded "all targets" by runtime registries, command capability, observation scope, and fail-closed ambiguity handling. -->
<!-- 20260909_kpopmodder: Reconciled GOTO ownership, finite FIND lifecycle proof, Fabric result projection, public-coverage metrics, and rollout dependencies after source review. -->
<!-- 20260911_kpopmodder: Reclassified the mixed-worktree GOTO grammar addendum after live navigation failure and linked the diagnostics-first incident gate. -->

# ChatClef Korean GOTO, FIND, and All-Command Coverage Pre-Change Contract

Date: 2026-09-09

## 1. Document status

This document records the requested behavior against the historical reviewed
HEAD before implementation. It does not claim that `@find`, direct `@go`, or
Korean input for every registered command works in the present worktree.

```text
DOCUMENT_TYPE: PRE_CHANGE_IMPLEMENTATION_CONTRACT
HISTORICAL_REVIEWED_HEAD_STATUS: DOCUMENTED_NOT_IMPLEMENTED
CURRENT_STATUS_OVERRIDE: SECTION_1_1_IMPLEMENTATION_ADDENDUM
REVIEWED_REPOSITORY_ROOT: C:\Vtuber_Souorce_Code\LAVI
REVIEWED_BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
REVIEWED_HEAD: d84ff7ccf2bc7a8b1cfae0baed21c929efe478f2
REVIEWED_WORKTREE_AT_START: CLEAN

BACKEND_SCOPE: FABRIC_CHATCLEF_ONLY
FORGE_MINEMIND_SCOPE: NONE
WORKSTREAM_A: KOREAN_GOTO_COORDINATE_GRAMMAR_RECOVERY
WORKSTREAM_B: NEW_LAVI_OWNED_FIND_COMMAND
WORKSTREAM_C: KOREAN_INPUT_COVERAGE_FOR_ALL_REGISTERED_COMMANDS
REVIEWED_HEAD_REGISTERED_COMMAND_COUNT: 26
REVIEWED_HEAD_PUBLIC_KOREAN_INPUT_COMMAND_COUNT: 10
REVIEWED_HEAD_PARSER_READY_COMMAND_COUNT: 12
REVIEWED_HEAD_GAMEPLAY_EFFECT_VERIFIABLE_COMMAND_COUNT: 2
REVIEWED_HEAD_FEEDBACK_PHRASE_PROFILE_COUNT: 26
REVIEWED_HEAD_FIND_COMMAND: ABSENT
REVIEWED_HEAD_GO_COMMAND_ALIAS: ABSENT
REVIEWED_HEAD_CANONICAL_COORDINATE_COMMAND: goto
TARGET_REGISTERED_COMMAND_COUNT_WITH_FIND_AND_WITHOUT_GO_ALIAS: 27

PRODUCTION_SOURCE_CHANGE_IN_THIS_DOCUMENTATION_TASK: NONE
TEST_SOURCE_CHANGE_IN_THIS_DOCUMENTATION_TASK: NONE
DOCUMENTATION_CHANGE: README_AND_THIS_PRE_CHANGE_CONTRACT
JAVA_BUILD_IN_THIS_DOCUMENTATION_TASK: NOT_RUN
JAVA_BUILD_REASON: DOCUMENTATION_ONLY_NO_JAVA_CHANGE
PYTHON_TESTS_IN_THIS_DOCUMENTATION_TASK: NOT_RUN
DEPLOYMENT: NOT_RUN
MINECRAFT_RUNTIME: NOT_RUN
COMMIT_AT_INITIAL_DOCUMENTATION_CAPTURE: NOT_RUN
PUSH_AT_INITIAL_DOCUMENTATION_CAPTURE: NOT_RUN
```

### 1.1 Implementation addendum

The status block above remains the historical snapshot for the reviewed HEAD
and its documentation-only task. The later mixed, uncommitted working-tree
snapshot must not be read as a committed implementation unit or as successful
GOTO navigation. Its current evidence classification is:

```text
IMPLEMENTATION_ADDENDUM_STATUS: MIXED_WORKTREE_REFERENCE_ONLY
WORKSTREAM_A_GRAMMAR_SOURCE_STATUS: PRESENT_IN_MIXED_UNCOMMITTED_WORKTREE
WORKSTREAM_A_TRANSLATION_RUNTIME_STATUS: VERIFIED_RUNTIME
WORKSTREAM_A_TRANSLATION_VERIFIED_SCOPE: INCIDENT_COMMAND_ONLY
WORKSTREAM_A_FOCUSED_TEST_STATUS: NOT_RUN
WORKSTREAM_A_FOCUSED_TEST_HISTORICAL_REPORT: PASSED_NOT_REPRODUCED_IN_THIS_DOCUMENT_REVIEW
WORKSTREAM_B_STATUS: DOCUMENTED_NOT_IMPLEMENTED
WORKSTREAM_B_PHASE: FUTURE_WORK
WORKSTREAM_C_STATUS: DOCUMENTED_NOT_IMPLEMENTED
WORKSTREAM_C_PHASE: FUTURE_WORK
CURRENT_GAMEPLAY_EFFECT_VERIFIABLE_COMMAND_COUNT_STATUS: NOT_RUN
CURRENT_GAMEPLAY_EFFECT_VERIFIABLE_COMMAND_COUNT_REASON: NOT_RECOUNTED_DO_NOT_INCREMENT_FROM_HISTORICAL_2
GOTO_LIVE_ACCEPTANCE_STATUS: FAILED_RUNTIME_ACCEPTANCE
NAVIGATION_ROOT_CAUSE_STATUS: UNKNOWN
GOTO_A3_F_FINITE_EARLY_FAILURE_STATUS: FAILED_RUNTIME_ACCEPTANCE
GOTO_TERMINAL_COUNTER_EVENT_MAPPING_STATUS: NOT_PROVEN_RUNTIME
GOTO_COUNTER_SEMANTIC_GAPS_EVIDENCE: SOURCE_PROVEN
GOTO_FINITE_COUNTER_EXACT_TICK_CAUSAL_CHAIN_STATUS: UNKNOWN
GOTO_A2_EXACT_ARRIVAL_STATUS: NOT_EXERCISED
GOTO_BUILD_REQUIREMENT_EMISSION_STATUS: VERIFIED_RUNTIME
GOTO_BUILD_REQUIREMENT_EVENT_COUNT: 240
GOTO_BUILD_REQUIREMENT_EVIDENCE_STATUS: NOT_PROVEN_RUNTIME
GOTO_BUILDING_MATERIAL_READINESS_EMISSION_STATUS: VERIFIED_RUNTIME
GOTO_BUILDING_MATERIAL_READINESS_EVENT_COUNT: 16
GOTO_BUILDING_MATERIAL_READINESS_EVIDENCE_STATUS: NOT_PROVEN_RUNTIME
GOTO_EXECUTION_TRANSITION_EMISSION_STATUS: FAILED_RUNTIME_ACCEPTANCE
GOTO_EXECUTION_TRANSITION_OBSERVED_EVENT_COUNT: 0
GOTO_EXECUTION_TRANSITION_BUDGET_STARVATION_EVIDENCE: SOURCE_PROVEN
NONTERMINAL_EMISSIONS_BEFORE_TERMINAL_PHASE_CHANGE: 256
NONTERMINAL_EMISSION_COUNT_EVIDENCE: DERIVED_FROM_OPERATION_SCOPED_EVENT_COUNTS
GOTO_TERMINAL_DECISION_EMISSION_STATUS: VERIFIED_RUNTIME
GOTO_TERMINAL_DECISION_EVENT_COUNT: 1
GOTO_TERMINAL_DECISION_VERIFIED_SCOPE: OVERALL_TIMEOUT_DELIVERY_ONLY
GOTO_A4_BEHAVIOR_GATE_STATUS: NOT_EXERCISED
GOTO_A4_BEHAVIOR_GATE_REASON: TERMINAL_PAYLOAD_MATERIAL_STATE_REMAINED_NULL_AND_NO_FINITE_FAILURE_BRANCH_COMMITTED
GOTO_A5_MATERIAL_ACQUISITION_STATUS: NOT_EXERCISED
GOTO_A5_MATERIAL_ACQUISITION_REASON: NO_PER_COMMAND_OPT_IN
EVENT_COUNT_LOG_SOURCE: ACTIVE_INSTANCE_LATEST_LOG
EVENT_COUNT_OPERATION_ID: goto-23bd64e9-118b-4c3f-8b21-649cabf0afb2
EXACT_SAFE_ROLLBACK_SCOPE: UNKNOWN
RESTORATION_SALVAGE_AUDIT_STATUS: NOT_RUN
CURRENT_FIND_COMMAND: ABSENT
CURRENT_GO_COMMAND_ALIAS: ABSENT
```

The 2026-09-11 incident directly verifies only that one trusted Korean XYZ input
was compiled to the existing canonical `goto x y z`, admitted, dispatched, and
bound to its root Task. It did not reach the target. Exact arrival was not
exercised, finite early-failure handling failed runtime acceptance, material
readiness diagnostic emission occurred but its evidence was incomplete. A4
non-entry is established separately by the final timeout payload's null
usable/required material state and the controller's persistent A4 state/terminal
data flow, not by the observation-only readiness records. Bounded material
acquisition was independently ineligible because the command had no opt-in
suffix. See the
[GOTO diagnostics-before-behavior incident record](chatclef-goto-diagnostics-before-behavior-incident-2026-09-11.md).

The working-tree grammar unit did not add a direct `@go` alias. Workstreams B
and C retain the future sequence below and do not become authorized or
implemented from this narrow translation evidence. Exact file/hunk provenance
and an independently executable rollback unit have not been established.

The three workstreams were designed as conceptually separate future units. A
small Python GOTO input fix must not silently authorize every raw command, and
the new FIND behavior must not be hidden inside `@attack`, `@scan`, or a generic
Baritone lifecycle change. Their intended tests, implementation boundaries,
rollout, and rollback ordering are not an executable rollback manifest for the
current mixed worktree.

> [!IMPORTANT]
> Unless Section 1.1 explicitly overrides a fact, every use of "current",
> "currently", "existing", or "today" in Sections 2 through 16 describes the
> historical `REVIEWED_HEAD` `d84ff7ccf2bc7a8b1cfae0baed21c929efe478f2`
> snapshot and its intended future contract, not the present worktree or live
> runtime. Section 1.1 and the linked incident record own the later status.
> Section 17 remains a current normative decision gate, but factual baseline
> statements inside it are explicitly scoped to `REVIEWED_HEAD`.

## 2. Requested user-visible behavior

The intended behavior is:

```text
LAVI Chat or final microphone transcript
  -> deterministic Minecraft intent ownership
  -> Korean phrase and target resolution
  -> typed validation and command-specific safety policy
  -> canonical prefixless ChatClef command
  -> existing Fabric bridge adds the active @ prefix
  -> exact Java command executes
  -> correlated lifecycle response is shown and spoken once
```

Representative requests include:

```text
100, 64, -30 좌표로 가줘
x 100 y 64 z -30으로 가줘
마을 주민 찾아줘
가까운 상자 블록 찾아줘
떨어진 다이아몬드 아이템 찾아줘
좀비 3마리 공격해
자동 보관 장소 목록 보여줘
밝기를 1.5로 설정해
```

“한국어 지원” means that trusted LAVI Chat input and a final microphone
transcript can express the command and every translatable semantic argument in
Korean. Opaque identifiers such as an exact player username, namespaced mod ID,
stable destination ID, or confirmation token retain their validated exact
form. It does not mean that 26 new Korean `@명령어` spellings are implicitly
added to the native Java command parser. Existing raw/native command forms stay
compatible unless a separately named alias is explicitly approved.

Interim speech-recognition hypotheses are never executable input. Only the
existing trusted final-transcript source may reach command admission.

## 3. Authority and companion documents

This document owns only the future input-coverage and FIND behavior described
here. Existing documents keep their current authorities:

- [Minecraft Backend Separation](minecraft-backend-separation.md) owns the
  Fabric/Forge boundary. This work is Fabric ChatClef only.
- [Python Korean Command Registry Plan](chatclef-python-korean-command-registry-plan.md)
  owns the current registry axes, safety tiers, source metadata, and exact
  source-registration reconciliation.
- [Python Command Orchestration Plan](chatclef-python-command-orchestration-plan.md)
  owns active-command admission, command barriers, correlation, and no-replay
  behavior.
- [General Natural Korean Command Lifecycle Feedback Implementation Record](chatclef-general-natural-korean-command-lifecycle-feedback-implementation-record-2026-09-07.md)
  owns the current 26-profile response/lifecycle implementation.
- [Korean Item Action Alias V2 Plan](chatclef-korean-item-action-alias-v2-plan.md)
  owns the current item-alias policy and action-specific item restrictions.
- [Korean Test Strategy](chatclef-korean-test-strategy.md) owns the broader
  trusted-input, parser, bridge, and runtime verification policy.
- [ChatClef Command Lifecycle and Threading](chatclef-command-lifecycle-and-threading.md)
  owns Java task/thread lifecycle invariants.
- [ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)
  owns the preserved upstream-engine boundary, composition-first rule, strict
  Task/goal/path/input ownership, and last-resort engine-divergence gate. FIND
  does not add a Carry On dependency.
- [Fabric ChatClef Bridge Protocol V1](fabric-chatclef-bridge-protocol-v1.md)
  owns the existing command request/result envelope, status values, additive
  `data` profiles, and compatibility rules.
- [Fabric ChatClef 1.20.1 Build Verification](chatclef-fabric-build-verification.md)
  owns the future clean forced build and artifact/runtime evidence chain when
  Java FIND or a direct `@go` alias is implemented.
- [ChatClef / Baritone Cache Troubleshooting](chatclef-baritone-cache-troubleshooting.md)
  must be applied before classifying pathfinding loops, impossible movement,
  or wrong-target travel as a new behavior defect, especially after a world is
  copied, restored, replaced, or renamed.

The authoritative current command-name artifacts are:

- `KoreanChatClefCommandRegistry._REGISTERED_COMMANDS` in
  `plugins/Minecraft/fabric/chatclef/command_registry/korean_command_registry.py`;
- `tests/minecraft_chatclef/command_catalog/chatclef_registered_commands.snapshot.json`;
- `tests/minecraft_chatclef/command_catalog/chatclef_command_support_matrix.json`;
- Java registration sources under
  `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java`.

The support-matrix value `IMPLEMENTED` is not equivalent to public Korean
execution. In the current artifact it includes `follow` and `idle`, which are
parser-ready but fail closed before Python admission. Every future report and
test must use the independent readiness axes instead of one overloaded status.

## 4. Source-reviewed current baseline

### 4.1 Exact current command set

The reviewed source contains these 26 registered names:

```text
attack
auto_deposit_trust
auto_deposit_trusted_list
auto_deposit_untrust
chatclef
deposit
deposit_all
equip
follow
food
gamer
gamma
get
give
goto
hero
idle
locate_structure
meat
overlay
reload_settings
resetmemory
scan
stop
store_home
자동보관등록
```

At `REVIEWED_HEAD`, `find` and `go` appeared in none of the Java command
constructors, Python registry rows, registry snapshots, or support-matrix rows.
The canonical coordinate command was `goto`.

### 4.2 Independent readiness axes

All 26 rows are source-registered and all 26 already have a Korean lifecycle
phrase profile. That response coverage does not grant input execution.

| Command | Parser | Python admission | Bridge lifecycle | Strong effect proof | Public Korean input |
| --- | --- | --- | --- | --- | --- |
| `attack` | no | no | no | no | no |
| `auto_deposit_trust` | yes | yes | yes | no | yes |
| `auto_deposit_trusted_list` | no | no | no | no | no |
| `auto_deposit_untrust` | no | no | no | no | no |
| `chatclef` | no | no | no | no | no |
| `deposit` | yes | yes | yes | no | yes |
| `deposit_all` | no | no | no | no | no |
| `equip` | yes | yes | yes | no | yes |
| `follow` | yes | no | no | no | no |
| `food` | yes | yes | yes | no | yes |
| `gamer` | no | no | no | no | no |
| `gamma` | no | no | no | no | no |
| `get` | yes | yes | yes | yes | yes |
| `give` | yes | yes | yes | no | yes |
| `goto` | yes | yes | yes | no | yes |
| `hero` | no | no | no | no | no |
| `idle` | yes | no | no | no | no |
| `locate_structure` | no | no | no | no | no |
| `meat` | yes | yes | yes | no | yes |
| `overlay` | no | no | no | no | no |
| `reload_settings` | no | no | no | no | no |
| `resetmemory` | no | no | no | no | no |
| `scan` | no | no | no | no | no |
| `stop` | yes | yes | yes | no; specialized STOP validation | yes |
| `store_home` | yes | yes | yes | yes | yes |
| `자동보관등록` | no | no | no | no | no |

Current totals are therefore:

```text
source registered:             26
lifecycle phrase profile:      26
parser ready:                  12
Python admission ready:        10
bridge lifecycle ready:        10
public Korean input:           10
strong gameplay-effect proof:   2
```

This distinction is mandatory in the implementation. Adding a Korean phrase,
an LLM prompt example, or a response renderer row is never enough to make a
command public.

### 4.3 Current target coverage is not universal

The existing item resources expose three different facts:

```text
ChatClef catalog canonical targets:         591
Korean input alias phrases:                 564
unique canonical targets reached by alias: 553
canonical targets without fixed input alias: 38
Korean display-name resource keys:          591
```

The 38 targets not reached by a fixed Korean input alias consist of 13
canonicalized-legacy targets, 19 generic groups, and 6 targets marked
`UNSUPPORTED` by the public-alias policy. The current GET resolver can still
accept all 591 canonical IDs. A display label is not proof of a Korean input
alias, and the 585 non-`UNSUPPORTED` policy count is not an execution allowlist.

The current shared item-action translator applies only to GET, EQUIP, DEPOSIT,
and GIVE, and every command keeps its own capability restrictions. EQUIP, for
example, has a narrower equipment gate. There is currently no equivalent
Korean entity/mob alias catalogue for ATTACK or FIND.

### 4.4 Current typed-input ceiling

The current `ChatClefIntentType` exposes 12 executable intent types plus
`UNKNOWN`, and the compiler supports those 12 executable types. The current
LLM prompt advertises only the narrower `get_item`, `food`, `meat`, `goto`,
`follow`, `idle`, `stop`, and `unknown` output set. Neither that prompt nor an
LLM-produced command name is execution authority.

All-command coverage therefore requires focused typed classifiers, immutable
slot contracts, validation, and compiler dispatch for each newly exposed
command or stable family. It must not be implemented by merely widening the
prompt's allowed command-name list.

## 5. Workstream A — Korean GOTO coordinate grammar

### 5.1 Historical reviewed-HEAD failure boundary

At `REVIEWED_HEAD`, the Python rule parser recognized three signed integers separated by
whitespace, immediately followed by an optional direction particle and an
`이동`/`가` verb. The text normalizer converts commas to spaces, so the rule
parser alone recognizes this form:

```text
100, 64, -30으로 가줘
```

It did not work end to end from trusted Chat/final microphone at that snapshot.
The input-intent ownership gate recognized `이동` and `좌표`, but did not claim a
bare coordinate phrase followed only by `가`/`가줘`. In that case the parser's
GOTO result was never reached by the Minecraft command route.

The following requested forms do not match because `좌표` or axis labels occur
inside the coordinate phrase:

```text
100, 64, -30 좌표로 가줘
x 100 y 64 z -30으로 가줘
x=100, y=64, z=-30 좌표로 가줘
100 64 -30 좌표로 이동해
```

At that reviewed HEAD, `좌표 100 64 -30으로 가줘` could match only because the
regular expression searched for the numeric tail. That incidental behavior was
not a complete grammar contract.

The downstream path already present at that snapshot was:

```text
typed GOTO intent {x, y, z}
  -> Java-int range validation
  -> prefixless `goto x y z`
  -> Fabric bridge adds active prefix
  -> existing Java GotoCommand
  -> existing GetToBlockTask for XYZ
```

Therefore ordinary Chat/final-microphone coordinate recovery is a focused
Python candidate/ownership-gate, grammar, and test change. It does not require
a Java source change or Gradle build when the canonical command remains
`goto`.

### 5.2 Required accepted forms

The future dedicated coordinate parser must accept at least:

```text
100 64 -30으로 가줘
100,64,-30으로 가줘
100, 64, -30 좌표로 가줘
좌표 100, 64, -30으로 이동해줘
x 100 y 64 z -30으로 가줘
x=100, y=64, z=-30 좌표로 가줘
엑스 100 와이 64 제트 마이너스 30 좌표로 가줘
(100, 64, -30)으로 이동해
```

Spacing and punctuation differences produced by final speech transcription
may be normalized, but the parser must still prove exactly one X, one Y, and
one Z integer. Axis labels include the closed aliases `x`/`엑스`, `y`/`와이`,
and `z`/`제트`. Coordinate values in the first rollout use Arabic digits with
an optional `+`/`-` sign or a closed `플러스`/`마이너스` token immediately
before the digits. Axis-labelled forms may appear in X/Y/Z order only in the
first rollout. Reordering labels is a separate extension unless the typed
parser proves uniqueness and deliberately supports it.

### 5.3 Fail-closed coordinate rules

The first rollout supports absolute integer XYZ coordinates only. It rejects
without submission when any of these conditions holds:

- one or more coordinates are missing;
- an axis appears twice or an extra fourth number is present;
- a value is a decimal, NaN, infinity, or outside signed Java `int` range;
- a coordinate is expressed only as an unrestricted Korean number-word phrase
  rather than the first rollout's bounded signed-digit grammar;
- Minecraft relative coordinates such as `~`, local coordinates such as `^`,
  or any explicit dimension qualifier is requested; the first rollout targets
  the current dimension only;
- punctuation leaves an ambiguous sign or thousands separator;
- the utterance contains two complete competing coordinate triples;
- the dedicated classifier/parser does not consume the whole utterance apart
  from closed politeness and punctuation suffixes, including a trailing second
  action such as `100 64 -30으로 가고 좀비 공격해`;
- a claimed LAVI Chat/final-microphone event lacks its existing trusted final
  eligibility proof, or the microphone event is interim.

Malformed input produces one deterministic clarification/rejection and zero
Java submissions. It is not repaired by an unconstrained LLM guess.
The new grammar's required rollout matrix is LAVI Chat plus final microphone;
it does not remove or widen the current authorized `direct_typed` and
`lavi_gui_korean` GOTO paths.

### 5.4 `@goto` versus `@go`

The canonical command is `@goto`. Korean Chat/microphone support does not need
an `@go` alias because the Python compiler emits `goto x y z`.

Recommended first implementation:

```text
direct @goto: unchanged
Korean Chat/final mic: expanded
direct @go: not added
```

If direct native `@go` is later required, it is a separate Java compatibility
alias and rollback unit. It must delegate to the established GOTO parsing/task
contract without creating a second movement implementation. That optional
alias requires Java tests and the mandatory clean forced build.

## 6. Workstream B — new `@find` command

### 6.1 Why FIND is a new command

No current `@find` command exists. Existing commands cover only narrower
behaviors:

- `@attack <name> [count]` compares a player name or an entity untranslated
  name, then runs a kill-and-drop-collection task. Its behavior must not be
  reused by FIND.
- `@scan <block>` resolves a vanilla `Blocks` field, checks the current block
  scanner, logs the nearest known position, and finishes. It is block-only,
  does not approach the block, and is not proof of global discovery.
- `@locate_structure` supports only `stronghold` and `desert_temple` in the
  current Java enum.

FIND therefore needs its own typed command, resolver, operation owner,
terminal result, diagnostics, Python translation, and lifecycle evidence.
Only narrow matching/observation ideas may be reused through composition.

### 6.2 Canonical direct grammar

The proposed unambiguous direct form is:

```text
@find <kind> <target>
```

Initial required target kinds are:

```text
entity     an eligible mob type
player     an exact player name
block      a block type
item       a dropped ItemEntity stack type
```

The initial public `entity` kind means registered mob types, including eligible
modded mobs. It does not automatically include every `EntityType`: players,
dropped items, projectiles, area-effect clouds, markers, vehicles, paintings,
experience orbs, and transient/internal entities are outside that kind unless
a later command-specific capability explicitly admits them. Player and dropped
item identity remain owned by their separate `player` and `item` kinds.

`structure` is a conditional extension, not an initial required kind. If it is
approved, it covers only a structure supported by an explicitly verified
structure resolver. Otherwise Korean structure requests keep routing to the
existing `locate_structure` capability.

Examples:

```text
@find entity minecraft:villager
@find entity minecraft:zombie
@find player PlayerName
@find block minecraft:diamond_ore
@find item minecraft:diamond
```

Conditional form after structure-scope approval:

```text
@find structure stronghold
```

FIND requires a dedicated target grammar. The current raw scalar feedback
grammar and Python generic target compiler are both limited to ASCII
alphanumeric/underscore targets; the compiler is also lowercase-only. Neither
accepts the namespace colon in `namespace:path`. They must not be globally
widened as a side effect. FIND validation owns namespaced syntax, length
bounds, allowed characters, target kind, and canonicalization.

### 6.3 Korean intent and ambiguity contract

Representative natural forms are:

```text
마을 주민 찾아줘                 -> entity minecraft:villager
가까운 좀비 찾아줘               -> entity minecraft:zombie
플레이어 Alex 찾아줘             -> player Alex
다이아 원석 블록 찾아줘          -> block minecraft:diamond_ore
떨어진 다이아몬드 아이템 찾아줘 -> item minecraft:diamond
엔드 요새 찾아줘                 -> structure stronghold, if approved and supported
```

The current Minecraft input-intent gate does not claim `찾아`/`찾아줘`, so
FIND ownership must be added before the generic outer conversation path. The
FIND classifier must also remain distinct from the GET acquisition classifier.
`찾아줘` does not mean `가져와`, `캐줘`, `주워`, or `공격해`.

When one Korean name resolves to more than one kind, the system must ask for a
deterministic clarification and submit nothing. For example:

```text
다이아 찾아줘
  -> 다이아몬드 원석 블록을 찾을지,
     떨어진 다이아몬드 아이템을 찾을지 확인
  -> command submission 0
```

Target-kind precedence must never silently turn an ambiguous noun into an
attack, mining, GET, pickup, or block-search request.

Ambiguity is owned entirely by the Python `FindTargetResolution` pre-admission
path and produces zero Java submissions. It is not a Java `FindOutcome`
terminal reason. The canonical direct grammar already requires an explicit
kind and a validated target ID/name.

Bare `요새 찾아줘` must not silently mean `stronghold`; it remains ambiguous
until a closed label is selected. For player FIND, classification may inspect a
normalized copy for Korean verbs, but the exact player token is extracted from
the original text with case preserved, length/character validated, and bound
to the resolved live player identity. Lowercasing the shared normalized text
must not alter a username.

### 6.4 Meaning of “all mobs, blocks, and items”

“All” has two separate meanings and both must be reported:

1. Vocabulary coverage: resolve every target type that the active Minecraft
   runtime registry and the command-specific capability expose, including a
   safe namespaced-ID fallback when no approved Korean label exists.
2. Instance discovery: find an actual matching instance only within the
   operation's proven observation scope and, only for a separately approved
   mode, its bounded exploration scope.

Vocabulary coverage does not prove that an instance exists, is loaded, is in
the current dimension, or is reachable. A missing result within loaded chunks
does not prove that the target is absent from the world.

The target catalogues must be domain-specific:

| Domain | Identity authority | Actual observation boundary |
| --- | --- | --- |
| mob | eligible mob subset of the active `ENTITY_TYPE` registry plus approved Korean aliases | current loaded client-world mob entity, terminally revalidated |
| player | exact bounded player name and live player entity | currently loaded/connected player entity, terminally revalidated |
| block | active `BLOCK` registry plus approved Korean aliases | currently loaded block position and exact block state; bounded exploration only if separately approved |
| dropped item | active `ITEM` registry plus approved Korean aliases | currently loaded ItemEntity, terminally revalidated; not inventories or unopened containers |
| structure, conditional | explicit supported structure resolver | resolver-specific proof after scope approval; current legacy command supports only two types |

BlockScanner or EntityTracker cache data may nominate a candidate, but an
unloaded, stale, removed, or dimension-mismatched candidate is only a hint. The
base mode has no authority to load its chunk. `FOUND_AND_REPORTED` requires
terminal revalidation against the current client world and exact dimension;
otherwise the operation returns a cautious non-success reason.

At operation start, base FIND snapshots the current dimension and player
position as its search origin. Before source work, each kind must receive a
fixed loaded-scope query radius, candidate cap, elapsed-time budget, and either
a bounded existing index or a bounded enumeration strategy. The operation
must not load chunks or scan the whole world. Exhausting a bound without
proving a complete no-match returns `OBSERVATION_BOUNDS_EXHAUSTED`, not a found
result or a global-absence claim.

Candidate ranking uses squared Euclidean distance from that immutable origin
snapshot. Equal-distance mob/player/dropped-item candidates sort by stable UUID
and equal-distance block candidates sort lexicographically by `(x, y, z)`;
registry or collection iteration order is never a tie-breaker. The reported
position is the terminally revalidated `BlockPos`: exact block position for a
block, and the floored current entity position for a mob, player, or dropped
item. A moving entity's stale selection-time position is not reported as its
terminal location, while the stable candidate identity remains unchanged.

Modded targets without an approved Korean translation remain usable by their
validated namespaced IDs. Duplicate Korean labels, block/item name collisions,
and registry collisions return an explicit ambiguity result instead of choosing
the first match.

An item inside an unopened chest, another player's inventory, or an unloaded
container is not a dropped item and is not globally discoverable by this
contract. Container-content search, recipe/resource acquisition, and mining
belong to separate capabilities.

### 6.5 FIND behavior and completion

The request says “find” but does not decide whether success means reporting a
location or moving to the target. Implementation must not silently add
movement. The safe required base mode is:

```text
all enabled target kinds
  -> LOCATE_AND_REPORT
  -> resolve one exact kind and kind-appropriate canonical target
  -> run one finite read-only FIND observation Task
  -> inspect bounded current known/loaded candidates
  -> select nearest eligible candidate
  -> revalidate the same target identity
  -> retain one immutable FindOutcome
  -> emit the matching TaskFinishedEvent
  -> publish one typed position result
```

The base mode performs no FIND-owned movement, exploration, input, goal, or
path mutation. “Not observed” is limited to current known/loaded evidence and
is never rendered as global absence.

An optional `LOCATE_AND_APPROACH` mode may be added only after explicit product
approval for a distinct request such as `찾아서 가까이 가줘`. It then requires:

```text
resolved non-item target
  -> explicitly bounded exploration in the current dimension, if enabled
  -> nearest eligible candidate selection
  -> domain-specific safe stand-off approach
  -> same-target identity revalidation
  -> one typed safe-range terminal result
```

Dropped-item FIND remains locate/report-only in the initial feature even if
approach is approved for other kinds. A final stand-off distance does not prove
that the movement path avoided Minecraft's automatic pickup range. A later
dropped-item approach mode requires a separate path-wide no-pickup contract and
runtime evidence.

Neither the base mode nor the conditional approach mode changes dimension.
Cross-dimension FIND is a separate future behavior request.

If `LOCATE_AND_APPROACH` is approved, already being within the safe distance is
a valid zero-work success. In every mode, strong success requires the same
target identity to be revalidated at terminal time and the selected completion
mode to be satisfied; a search request, path request, timer expiry, or Java
callback alone is not success evidence.

FIND is a locate/report operation by default. A separately approved approach
mode may mutate only its explicitly owned movement resources. FIND must not:

- attack or kill an entity;
- mine or break a block;
- interact with a block or open a container;
- request or treat dropped-item pickup as FIND success;
- create a GET/acquisition task;
- change dimension;
- use an unbounded wander task;
- directly stop the entire TaskRunner, cancel unrelated Baritone work, or add
  FIND-specific global cleanup.

Because the finite observation uses the established `runUserTask` path, the
pre-existing `UserTaskChain` completion path may still invoke its configured
generic `AltoClef.stop()`/Baritone cancellation/key clearing. That inherited
engine behavior is not owned or expanded by FIND. The FIND Task and its
collaborators add no extra global cleanup; removing or changing the generic
UserTaskChain behavior would be a separate reviewed lifecycle/engine change.

If approach mode is approved, hostile-entity and creeper stand-off distances
must avoid deliberate close engagement. If a safe path or stand-off point
cannot be established, the operation fails cautiously instead of converting
risk into success.

Before any approach behavior is implemented, exact exploration radius, time
budget, and per-kind stand-off distances must be fixed as tested configuration.
No default may be `Float.POSITIVE_INFINITY`, endless wandering, or an unbounded
retry loop.

### 6.6 FIND ownership and terminal reasons

A new LAVI-owned FIND operation owner owns:

```text
operation/correlation identity
resolved target kind and kind-appropriate canonical identity
candidate selection and stable candidate identity
completion mode
locate/report completion predicate
target-lost handling
STOP/interruption cleanup
typed terminal result
fallback decision
```

The base locate/report mode owns no input, goal, or path. FIND operation state
must be request-local or task-local and must not use mutable static/global
state.

The current bridge classifies a synchronous finish callback with no new
command-owned root and no matching `TaskFinishedEvent` as cautious `unknown`.
Therefore the base path uses one finite, LAVI-owned, read-only FIND observation
Task. It has no child Task, input, goal, path, movement, retry, or exploration;
it stores one immutable `FindOutcome`, becomes finished only after terminal
revalidation or a typed non-success, and supplies the matching
`TaskFinishedEvent` required by the existing bridge proof model. Callback-only
completion must not produce strong `FOUND_AND_REPORTED` evidence, and the
generic unchanged-idle-root UNKNOWN rule must not be weakened. Any future
command-specific immediate-query proof path is a separate reviewed protocol
and lifecycle change.

`FindOutcome` is assigned once. Normal `onStop` after a finished observation
must not overwrite its found or typed non-success result; STOP/interruption may
retire only an unsettled operation, and late callbacks cannot replace the
terminal outcome.

If `LOCATE_AND_APPROACH` is separately approved, a LAVI-owned finite parent
task additionally owns the exploration radius, elapsed-time budget, attempt
count, movement child, safe-distance predicate, and fallback. Before it starts,
the contract must name which exact parent or child acquires each input, goal,
and path, and the same proven owner must release only that resource during
normal completion, STOP, interruption, and failure.

In that conditional mode, the existing upstream-derived `GetToEntityTask` must
not be reused directly as FIND's movement child. It has no finite
`isFinished()` contract and its current start/stop/tick paths can call global
Baritone `forceCancel()`, release inputs, or invoke goal/explore
`onLostControl()`. That conflicts with FIND's rule that it may clean up only
resources whose ownership it proves. Use a FIND-local finite movement owner or
a source-proven narrow existing seam whose acquisition and cleanup ownership
is exact. If that cannot be contained in LAVI-owned code, apply the documented
last-resort engine-divergence gate and stop before a broad upstream change.
Block movement may compose an established near-block seam only after the same
goal/path ownership proof, while FIND retains candidate identity and terminal
ownership.

The current Python lifecycle registry is keyed by command name. Before an
approach mode is added, it must either prove that both report and approach use
one compatible finite-operation lifecycle or add validated descriptor-mode
lifecycle dispatch. One command-name row must not silently claim contradictory
immediate and finite-task lifecycle kinds.

Initial terminal reasons must distinguish at least:

```text
FOUND_AND_REPORTED
INVALID_TARGET
NOT_OBSERVED_IN_LOADED_SCOPE
OBSERVATION_BOUNDS_EXHAUSTED
TARGET_LOST
CANDIDATE_NOT_REVALIDATABLE
STOPPED
INTERRUPTED
INTERNAL_ERROR
```

An approved approach mode additionally uses:

```text
FOUND_AND_IN_SAFE_RANGE
ALREADY_IN_SAFE_RANGE
SEARCH_BOUNDS_EXHAUSTED
UNREACHABLE
TIMEOUT
```

`NOT_OBSERVED_IN_LOADED_SCOPE`, `OBSERVATION_BOUNDS_EXHAUSTED`, and
`SEARCH_BOUNDS_EXHAUSTED` mean only that the operation did not prove a target
within its evidence boundary. They must not be rendered as “this target does
not exist in the world.”

If trusted STOP interrupts FIND, `STOPPED` is internal retirement evidence for
the original FIND claim. It must follow the existing STOP retirement/quarantine
rules and must not emit a second user-facing FIND terminal. UI, output, and TTS
publish only the one validated STOP terminal response, `멈췄어`.

### 6.7 Required base Task inheritance ledger

The base report Task has this narrow, source-proven inheritance exception:

```text
Proposed subclass:
  lavi.minecraft.find.observation.task.FindObservationTask
Exact base class:
  adris.altoclef.tasksystem.Task
Exact methods to override:
  onStart, onTick, onStop, isFinished, isEqual, toDebugString
Why composition alone is insufficient:
  runUserTask ownership and its matching TaskFinishedEvent are the current
  bridge proof for a strong command-owned terminal
Lifecycle owned by the base:
  established Task scheduling, TaskFinishedEvent publication, and unchanged
  generic UserTaskChain post-task cleanup
Lifecycle newly owned by the subclass:
  one bounded read-only observation, terminal revalidation, one immutable
  FindOutcome, and local STOP/interruption retirement
Inputs, goals, paths, and cleanup affected:
  none; the Task acquires and releases no gameplay input, goal, or path
Generic behavior that could regress:
  task identity/correlation and completion observation only; ATTACK, SCAN,
  GOTO, IdleTask, generic scheduling, and global cleanup remain unchanged
Upstream comparison impact:
  no upstream-derived file move, split, subclass replacement, or broad hunk
Separate hierarchy scope required:
  yes for any approach/movement Task; it needs its own ownership ledger
```

This exception permits participation in the existing `Task` contract only. It
does not authorize an `AltoClef`, `TaskRunner`, input-owner, or Baritone
lifecycle subclass.

### 6.8 FIND presentation contract

FIND uses bounded deterministic Korean templates, not an LLM rewrite:

| Fact | Required visible wording shape |
| --- | --- |
| accepted base request | `<검증된 대상>을 찾아볼게` |
| verified nearest report | `가장 가까운 <검증된 대상>은 x, y, z에 있어` |
| `NOT_OBSERVED_IN_LOADED_SCOPE` | `지금 확인 가능한 범위에서는 <검증된 대상>을 찾지 못했어` |
| `OBSERVATION_BOUNDS_EXHAUSTED` | `확인 범위를 모두 살피지 못해서 <검증된 대상>을 찾았다고 말할 수 없어` |
| `TARGET_LOST` or `CANDIDATE_NOT_REVALIDATABLE` | `찾던 <검증된 대상>의 위치를 마지막에 확인하지 못했어` |
| non-STOP `INTERRUPTED` or `INTERNAL_ERROR` | `<검증된 대상> 찾기를 끝내지 못했어` |
| `STOPPED` after trusted STOP | no FIND response; only validated `멈췄어` |
| ambiguous kind before submission | `<선택지 A>을 찾을지 <선택지 B>을 찾을지 알려줘` |
| malformed or insufficient terminal evidence | `<검증된 대상> 찾기 결과를 확인하지 못했어` |

The renderer applies the established Korean particle renderer rather than
hard-coding `을` or `은`. Target text comes from the immutable accepted request
projection; an exact player display preserves the correlated original request
token and never trusts a raw terminal payload or log field. Coordinates come
only from the verified terminal evidence projection. A wrong correlation
creates no response. Each retained claim reaches UI, output, and TTS once, and
a STOPped FIND yields no FIND terminal in addition to `멈췄어`.

## 7. Workstream C — Korean input for every registered command

### 7.1 Coverage definition

For each current command, and for FIND after its Java registration is proven,
Korean input support requires all of the following independently:

```text
source registration
closed Korean intent classification
typed DTO and schema validation
command-specific slot and target resolution
safety tier and confirmation policy
source-specific admission, including required trusted Chat/final-microphone
parity without removing existing authorized direct/raw paths
canonical prefixless serialization
bridge lifecycle correlation
terminal evidence policy
command-specific Korean START/status/terminal presentation as applicable
UI/output/TTS exactly-once delivery
positive, negative, ambiguity, source, and duplicate tests
public-Korean enablement only after all applicable gates pass
```

The current raw Java behavior and native `@` syntax remain unchanged. A
natural-language parser does not grant authority to internal automation,
interim microphone text, arbitrary GUI strings, or an untrusted caller.

Coverage tracks registered names and semantic command capabilities separately.
The current 26-name registry contains 25 semantic capabilities because the
Unicode `자동보관등록` row is a native compatibility alias for the bounded bulk
form of `auto_deposit_trust`. With FIND and without `go`, the target is 27
registered names and 26 public semantic Korean routes. The alias row is
accounted for explicitly, but trusted Chat/final microphone compiles its
meaning to canonical `auto_deposit_trust`; it does not need to serialize the
Unicode command spelling. STOP's command-specific presentation continues to
have zero visible START responses and one validated terminal response.

### 7.2 Command-by-command target contract

The following examples define intent shape, not the complete future synonym
list. Every accepted synonym must compile to the same typed command.

| Registered name / semantic route | Representative Korean request | Required special boundary |
| --- | --- | --- |
| `attack` | `좀비 3마리 공격해` | entity resolver; R3 confirmation; player targets reject by default unless a separate exact player-attack contract is approved; never confused with FIND |
| `auto_deposit_trust` | `여기를 자동 보관 장소로 등록해` | existing exact/bulk forms and R2 mutation evidence |
| `auto_deposit_trusted_list` | `자동 보관 장소 목록 보여줘` | read-only bounded list result and redacted rendering |
| `auto_deposit_untrust` | `이 보관 장소 등록 해제해` | stable destination identity; R2 mutation confirmation/evidence |
| `chatclef` | `마인크래프트 자동화 꺼` | R4 control confirmation; avoid self-lockout ambiguity |
| `deposit` | `철괴 10개 보관해` | item resolver, explicit quantity/target, existing R2 rules |
| `deposit_all` | `인벤토리 전부 보관해` | explicit all-items intent; R2 bulk inventory transfer/mutation |
| `equip` | `다이아몬드 흉갑 장착해` | equipment-only capability gate |
| `follow` | `Alex 따라가` | exact player identity; continuous lifecycle and STOP contract |
| `food` | `음식 10만큼 모아줘` | existing integer food-point semantics, not ten full hunger icons; bounded integer |
| `gamer` | `마인크래프트 엔딩까지 진행해` | reconcile stale Python `target` metadata with argument-free Java command; R3 |
| `gamma` | `밝기를 1.5로 설정해` | bounded finite number; current Java has no finish/result callback, so public remains false until an authoritative terminal/reconciliation clears active ownership; wording stays cautious until separate effect proof exists |
| `get` | `다이아 곡괭이 하나 만들어줘` | existing item/crafting resolver and quantity contract |
| `give` | `Alex에게 철괴 3개 줘` | exact player, item, quantity, and R2 give/drop evidence |
| `goto` | `100, 64, -30 좌표로 가줘` | dedicated absolute XYZ grammar from Section 5 |
| `hero` | `적대 몹 계속 정리해` | continuous/broad combat scope; R3 confirmation and STOP |
| `idle` | `가만히 있어` | continuous lifecycle, explicit resume/STOP behavior |
| `locate_structure` | `엔드 요새 찾아가` | only source-proven supported structures; bare `요새` remains ambiguous; do not claim every structure |
| `meat` | `고기 10만큼 모아줘` | existing integer food-point semantics, not ten full hunger icons; bounded integer |
| `overlay` | `오버레이 켜` | explicit on/off state and UI-only result |
| `reload_settings` | `마인크래프트 설정 다시 불러와` | R4 confirmation and bounded reload result |
| `resetmemory` | `ChatClef 대화 기록 초기화해` | R4 destructive confirmation; exact scope and terminal evidence |
| `scan` | `가까운 다이아몬드 원석 블록 좌표 알려줘` | block-only legacy query; distinguish from multi-domain FIND evidence |
| `stop` | `멈춰줘` | existing trusted control lane and terminal-only visible response |
| `store_home` | `아이템 집에 정리해` | existing strict STORE_HOME evidence evaluator |
| `자동보관등록` | `주변 16x16 자동 보관 장소 등록해` | Chat/final mic compiles to `auto_deposit_trust area 16x16`; Unicode alias remains raw/direct compatibility only |
| `find` | `마을 주민 찾아줘` | finite non-destructive loaded-scope locate-and-report operation from Section 6; approach remains disabled |

Supporting a command in Korean does not expand its Java capability. For
example, Korean `locate_structure` initially covers only the structures the
existing Java command can actually execute. Expanding that Java capability is
a separate behavior change with separate evidence.

`자동보관등록` in the table is a Korean natural-language intent, not a request
to send the Unicode Java alias through the trusted Chat/microphone transport.
That route serializes the existing canonical prefixless
`auto_deposit_trust area 16x16` form. The Unicode `@자동보관등록` spelling
remains raw/direct compatibility only.

### 7.3 Safety and confirmation

The current safety tiers are the authoritative minimum boundary. Each newly
exposed command still requires a source review of its actual side effects and
lifecycle; that review may keep or raise the tier through a separately
recorded change, but public Korean support must never lower a tier merely to
satisfy an “all commands” count.

- R0/R1 commands may execute only after exact typed validation and their
  existing admission checks.
- R2 mutation commands require the command-specific mutation scope and target
  to be explicit and bounded: exact item/count/destination where required, or
  an explicit supported bulk token such as `전부` or the fixed `area 16x16`
  form where that command already owns the bulk contract. An omitted scope is
  never inferred, and any existing confirmation requirement remains.
- R3/R4 commands initiated from Chat or final microphone require a new bounded
  confirmation receipt before execution. The confirmation must bind source,
  session, generation, request/message identity, normalized typed command,
  expiry, and one-time consumption.
- A second unrelated utterance, an interim transcript, a repeated UI event, or
  an LLM paraphrase cannot consume the confirmation.
- Rejection, timeout, cancellation, or duplicate confirmation causes zero Java
  submissions.

The existing `direct_typed_only` metadata for R3/R4 rows is a current safety
boundary. Chat/final-microphone availability for those commands cannot be
declared complete until the confirmation owner exists and is tested.

`chatclef off` has an additional recovery gate. Before it becomes public from
Chat/final microphone, tests must prove one terminal receipt is deliverable
across the disable transition, one independent user-accessible raw/direct
`@chatclef on` recovery path remains available, and confirmation cancellation
or expiry produces zero submissions.

### 7.4 Items and other argument domains

One global “translate any Korean noun to any command” resolver is prohibited.
Resolution must be scoped by command and slot:

```text
item target          -> GET/DEPOSIT/GIVE command capability
equipment target     -> EQUIP capability subset
entity/player target -> ATTACK/FOLLOW/FIND-specific resolver
block target         -> SCAN/FIND block registry
structure target     -> LOCATE_STRUCTURE, plus FIND only after structure approval
coordinate target    -> GOTO integer XYZ parser
setting value        -> CHATCLEF/GAMMA/OVERLAY typed bounds
destination identity -> auto-deposit trust/untrust/list contracts
```

All target resolvers return an immutable result such as `verified`,
`unsupported`, `ambiguous`, or `invalid`; they do not throw on malformed user
data. An ambiguous or unsupported result creates one safe clarification and
zero submissions.

Every admitted player-target route classifies intent from a normalized view but
extracts the exact player token from the original text, preserves case, and
binds it to the command-specific validated identity. FIND's player rule does
not implicitly authorize player ATTACK.

For an item-bearing command, “all items” means every target in that command's
actual capability catalogue, not every item accepted by a different command.
Each translatable target needs a unique approved Korean alias to count toward
all-Korean coverage. A validated canonical or namespaced-ID fallback keeps an
opaque or modded target reachable, but is reported separately and does not
inflate the Korean-alias metric. There is no hard-coded sample-item ceiling,
and catalogue coverage never widens EQUIP or another narrower Java capability.

### 7.5 Numeric argument contract

Numeric parsing is a focused typed responsibility, not a default-on-error
helper:

- an explicit numeric token that is malformed, out of range, duplicated, or
  not recognized by the command's closed grammar rejects with zero submission;
- a command-specific default may apply only when the quantity is genuinely
  absent, never when an explicit quantity failed to parse;
- exact integer slots accept Arabic digits and only the approved closed Korean
  numeral forms, with command-specific units such as `개`, `마리`, or `만큼`;
- booleans are not integers, and each command fixes and tests its own minimum
  and maximum;
- FOOD and MEAT quantities are integer food points, not a count of full hunger
  icons;
- GAMMA accepts only a finite decimal inside a source-reviewed explicit bound;
  NaN, infinity, overflow, and an as-yet-unfixed bound reject;
- GOTO coordinates use the signed-integer grammar and Java-int bounds in
  Section 5 rather than this quantity grammar.

The parser returns an immutable typed result and has positive, boundary,
malformed-explicit, omitted-default, unit-confusion, and overflow tests for
each command family.

## 8. Required end-to-end routing

The intended route order is:

```text
trusted LAVI Chat / final microphone input
  -> final-source and Hangul eligibility proof
  -> exact STOP owner
  -> contextual STATUS owner
  -> exact pending-confirmation owner
  -> GOTO/FIND/registered-command deterministic candidate classification
  -> typed command-specific resolution and validation
  -> active-command/busy policy
  -> command-specific confirmation/admission
  -> canonical prefixless compiler
  -> existing Fabric transport and active-owner transaction
  -> Java result reconciliation
  -> evidence evaluation once
  -> terminal CAS once
  -> UI/output/TTS presentation once
```

STOP remains higher priority than FIND, ATTACK, or any ordinary command. A
genuine second command while work is active retains the existing typed-busy
policy; it is not queued, replayed, or automatically submitted after the
active task ends.

FIND phrases must be claimed before generic outer conversation, but only when
the closed classifier proves a find verb plus a bounded target candidate.
General questions containing “찾다” that do not meet the Minecraft grammar
fall through without a Java submission.

## 9. Responsibility-based code organization for later implementation

No production files are added by this documentation task. When implementation
is approved, new LAVI-owned code with two or more responsibilities must be
split and folderized. Existing upstream-derived ChatClef files must not be
moved, renamed, split, or broadly refactored.

### 9.1 Python ownership

Recommended focused structure:

```text
plugins/Minecraft/fabric/chatclef/intent/navigation/goto/
  korean_goto_candidate_classifier.py
  korean_goto_coordinate_parser.py
  korean_goto_coordinate_parse_result.py

plugins/Minecraft/fabric/chatclef/intent/arguments/numeric/
  korean_numeric_argument_parser.py
  korean_numeric_argument_result.py

plugins/Minecraft/fabric/chatclef/intent/find/
  classification/
    korean_find_intent_classifier.py
  contracts/
    find_intent_decision.py
    find_target_kind.py
    find_target_resolution.py
  resolution/
    find_target_resolver.py
    entity/
    player/
    block/
    item/
  compilation/
    find_command_compiler.py
    find_target_identifier_validator.py

plugins/Minecraft/fabric/chatclef/transport/command_feedback/lifecycle/evidence/find/
  find_terminal_payload.py
  find_terminal_evidence_evaluator.py

plugins/Minecraft/fabric/chatclef/response/command_lifecycle/terminal/find/
  korean_find_terminal_renderer.py

plugins/Minecraft/fabric/chatclef/intent/registered_command/
  <responsibility-family>/<command>/
    classification/
    contracts/
    compilation/

plugins/Minecraft/fabric/chatclef/command_registry/coverage/
  korean_command_exposure_policy.py
  korean_command_readiness_evaluator.py

plugins/Minecraft/fabric/chatclef/command_registry/confirmation/
  command_confirmation_claim.py
  command_confirmation_authorizer.py
  command_confirmation_store.py

plugins/Minecraft/fabric/chatclef/diagnostics/intent/
  bounded_command_intent_diagnostics.py
```

Existing public APIs may remain as compatibility facades and delegate to the
new focused collaborators. Registry metadata, parser behavior, admission,
confirmation, and rendering must not be collapsed into one manager.

The `<responsibility-family>/<command>` shape is instantiated only for commands
whose implementation needs it; it is not permission to create empty folders.
Combat, settings/control, inventory/storage, navigation, and read-only query
families must keep command-specific safety and slot contracts. The current
`KoreanChatClefRuleParser`, `ChatClefIntentDTO`, and
`ChatClefCommandCompiler` remain compatibility facades/dispatchers rather than
growing into one mega-regex parser, one DTO containing every optional slot, or
one mega-compiler for all 27 commands. Immutable typed intent/projection and
serialization responsibilities are split per command or stable family, with
one primary production type per file.

Minecraft world/entity/block/item instance observation remains Java client-
thread ownership. The Python intent layer may resolve validated catalogue IDs
and later evaluate bounded terminal evidence, but it must not read or simulate
live world state.

### 9.2 Java FIND ownership

FIND is new LAVI-owned integration code, not an excuse to fork ChatClef or
rewrite AltoClef command infrastructure. Recommended package shape:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/
  lavi/minecraft/find/
    FindEntrypoint.java
    command/
      FindCommand.java
      FindCommandRegistrar.java
    contract/
      FindTargetKind.java
      FindTargetRequest.java
      FindTerminalReason.java
      FindOutcome.java
    resolution/
      entity/
      player/
      block/
      item/
    observation/
      FindObservationService.java
      task/
        FindObservationTask.java
    diagnostics/
      FindLifecycleDiagnostics.java

  lavi/minecraft/fabric/chatclef/bridge/command/result/
    profile/
      FabricChatClefCommandResultProfileDispatcher.java
    effect/find/
      FabricChatClefFindObservationResultProjector.java
      FabricChatClefFindObservationDataPayload.java
```

Create `resolution/structure/` only if the structure kind is approved. Create
an `approach/task/` package containing a finite FIND-owned Task, search bounds,
and stand-off policy only if `LOCATE_AND_APPROACH` is separately approved.
Empty conditional folders or placeholder types are prohibited.

The exact class count should remain the smallest set that preserves these
responsibilities. Do not create empty framework layers. `lavi/minecraft/find/**`
and the new bridge projector/dispatcher types are new LAVI-owned code.
`src/main/resources/fabric.mod.json` is an existing resource with upstream
provenance and LAVI patches; adding the one FIND entrypoint requires a
provenance check and one minimal reviewable hunk.

`FindEntrypoint` is the isolated composition owner. It follows the existing
end-client-tick readiness pattern, waits for the AltoClef command executor,
registers through `FindCommandRegistrar` exactly once, rejects a command-name
collision, and never edits upstream `adris.altoclef.AltoClefCommands` merely
for convenience. Registration readiness, idempotence, and collision behavior
require focused tests.

`FindOutcome` remains a domain contract and imports no Fabric bridge/wire type.
The Fabric result projector depends inward on that outcome. Before FIND adds a
second special command profile branch to
`FabricChatClefCommandResultFactory`, the existing LAVI-owned result path must
be minimally extracted behind one focused profile-dispatch facade that
composes the existing STORE_HOME projector and the new FIND projector. Do not
append another unrelated command-specific responsibility directly to the
factory, and do not alter generic result semantics during that extraction.

The base locate/report operation requires the finite read-only observation
Task in Section 6.7 for lifecycle proof, but not a movement Task. Target
resolution, registry access, observation bounds, diagnostics, and wire result
projection remain composed collaborators. If approach is approved, its
separate finite FIND movement Task may extend the established `Task`
abstraction only under its own inheritance/ownership ledger. Neither Task may
subclass `AltoClef`, `TaskRunner`, or a global input or Baritone lifecycle
owner.

### 9.3 Test organization

Tests mirror ownership rather than collecting the feature in one large file:

```text
tests/minecraft_chatclef/korean_translation/navigation/goto/
tests/minecraft_chatclef/korean_translation/find/
tests/minecraft_chatclef/command_catalog/
tests/minecraft_chatclef/command_admission/source_matrix/
tests/minecraft_chatclef/command_admission/confirmation/
tests/minecraft_chatclef/command_feedback/find/

plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/
  lavi/minecraft/find/command/
  lavi/minecraft/find/resolution/
  lavi/minecraft/find/observation/
  lavi/minecraft/fabric/chatclef/bridge/command/result/effect/find/
```

Add `lavi/minecraft/find/approach/` tests only with an approved approach
mode, and add structure-resolution folders only with approved structure scope.

## 10. FIND protocol and lifecycle projection

Adding FIND changes the exact registered-command set from 26 to 27. The same
rollout must synchronize, rather than weaken, all exact-set closures:

- Java source registration and the extracted command snapshot;
- Python `KoreanChatClefCommandRegistry` metadata;
- the support matrix with independent readiness axes;
- feedback family mapping and descriptor construction;
- lifecycle kind and terminal-trigger policy;
- terminal evidence-profile registry;
- raw-form grammar and immutable descriptor projection;
- Korean phrase-profile registry;
- catalog exact-equality tests.

FIND uses an additive versioned v1 effect profile. Strong base success requires
this exact active-context identity, outer lifecycle proof, and profile shape:

```text
accepted active descriptor/context:
  command_name=find
  form_kind=trusted_translation
  input_source=lavi_chat_ui | voice_input_final
  session/generation/request/message identity=<exact active match>
CommandResultDTO:
  status=completed
  ok=true
  error_code=null
  data.result_reason=matching_task_finished
  data.result_fidelity=callback_plus_matching_user_task_event
  data.effect_profile_id=fabric_chatclef_find_observation
  data.effect_profile_version=1
  data.effect_kind=find_observation
  data.effect_payload:
    completion_mode=LOCATE_AND_REPORT
    target_kind=entity | player | block | item
    canonical_target_id=<required for registry-backed entity/block/item; forbidden for player>
    player_identity_digest=<required for player; forbidden for non-player>
    candidate_identity_digest=<required>
    dimension=<bounded canonical dimension ID>
    x=<signed Java int>
    y=<signed Java int>
    z=<signed Java int>
    observation_scope=<closed kind-appropriate loaded-scope value>
    find_result=FOUND_AND_REPORTED
    find_satisfied=true
    reason=<trimmed 1..256 characters>
```

The nested payload is deeply validated. Found-result coordinates and candidate
identity are success-only and forbidden on a no-candidate outcome. A verified
loaded-scope miss uses the same matching-task outer proof, a closed
`find_result=NOT_OBSERVED_IN_LOADED_SCOPE`, `find_satisfied=false`, the validated
request target and observation scope, no candidate/coordinate fields, and a
bounded reason. Bounds exhaustion, target loss, STOP, interruption, and
internal error each require their own closed status/outcome matrix; none is
rendered as found. GET, STORE_HOME, and STOP-only profile keys mixed into a FIND
claim make the FIND profile invalid.

Unapproved approach values and fields are not added as placeholders. If
`LOCATE_AND_APPROACH` is later approved, its protocol unit may add
`completion_mode=LOCATE_AND_APPROACH`, `safe_distance_satisfied=true`, and the
closed `FOUND_AND_IN_SAFE_RANGE`/`ALREADY_IN_SAFE_RANGE` results together with
their evaluator and compatibility tests.

These exact wire names and every non-success matrix must be recorded atomically
in the
[Fabric ChatClef Bridge Protocol V1](fabric-chatclef-bridge-protocol-v1.md)
when implemented. Raw usernames, unbounded entity names, raw microphone text,
or full registry dumps must not be placed in ordinary diagnostics or terminal
payloads.

Strong Python success rendering additionally requires exact
session/generation/request/message identity, the existing reconciliation, and
the complete profile above. Missing or malformed evidence yields a cautious
terminal response. Correlation failure yields no response. An evaluator
exception yields at most one cautious fact and a bounded diagnostic; it does
not lose the terminal claim or resubmit the command.

The active ITEM/BLOCK/ENTITY_TYPE catalogue synchronization mechanism is a
separate Fabric-only capability decision. If wire transport is selected, it
must be additive, versioned, deterministically sorted, bounded or explicitly
paged, and bound to a digest/version that Python verifies before using it. It
must not place an unbounded registry in handshake/status payloads, alter the
backend-neutral common contract, or create Forge/MineMind placeholder values.

## 11. Diagnostics contract

Diagnostics are observation only. They do not choose targets, retry, extend a
timeout, mutate a goal, or convert failure into success.

Bounded Python fields may include:

```text
event
command_name
intent_kind
target_kind
canonical_target_id_or_digest
source_kind
session_id_digest
generation
request_id_digest
decision
rejection_reason
```

Bounded Java FIND fields may include:

```text
operation_id
task_type
phase
target_kind
canonical_target_id_or_player_identity_digest
dimension
candidate_identity_digest
candidate_distance_bucket
search_radius
elapsed_millis
attempt
goal_owner
path_owner
terminal_reason
cleanup_result
```

Do not log full Chat history, raw microphone audio/transcripts, authentication
data, full player lists, full entity dumps, full world scans, or unbounded
container/item state. Repeated tick observations require transition-only or
rate-limited logging.

For movement failures after a copied or replaced world, runtime verification
must first apply the documented Baritone-cache troubleshooting decision tree.
Deleting cache data is not authorized by this contract.

## 12. Characterization and acceptance tests

### 12.1 GOTO tests

Against the historical reviewed HEAD, characterize the then-accepted form, then add:

- prove that the rule parser alone accepts `100, 64, -30으로 가줘` while the
  then-current Chat/final-microphone ownership gate does not claim it, then cover
  the focused gate correction;
- spaces, compact commas, spaced commas, parentheses, leading `좌표`, trailing
  `좌표로`, ASCII X/Y/Z labels, Korean `엑스/와이/제트` labels, and closed
  `플러스/마이너스` signed-digit forms;
- positive, zero, and negative values;
- signed Java-int minimum and maximum;
- missing, duplicate, reordered-unapproved, decimal, relative, local,
  overflow, and two-triple rejection;
- whole-utterance consumption apart from closed politeness/punctuation, with
  compound or trailing unconsumed actions rejected with zero submissions;
- identical results for trusted Chat and final microphone;
- rejection for interim microphone, untrusted source, and non-Hangul route
  confusion;
- exact single compilation to `goto x y z` and exactly one submission;
- no Java change and no `go` registration in the canonical-only rollout.

### 12.2 FIND Python tests

- exact FIND candidate ownership for `찾아`, `찾아줘`, and approved variants;
- no collision with GET, ATTACK, SCAN, LOCATE_STRUCTURE, STATUS, STOP, or outer
  conversation;
- entity, player, block, and dropped-item resolution, plus structure only when
  that conditional kind is approved;
- exact player-token case preservation from original input and live-identity
  binding despite normalized verb classification;
- every generated Korean alias and every canonical namespaced-ID fallback;
- same-label and cross-domain ambiguity with zero submission;
- invalid namespace, oversize value, malformed kind, and unsupported target;
- exact source matrix for LAVI Chat and final microphone;
- immutable descriptor, compiler, admission, lifecycle, evidence, renderer,
  UI, output, and TTS exactly once;
- exact START, found, loaded-scope miss, ambiguity, and cautious templates from
  Section 6.8, including Korean particles and no global-absence wording;
- the exact nested v1 FIND profile, unrelated-profile-key rejection, malformed
  field types, and no response for wrong correlation;
- duplicate/cancel/retry events never resubmit or replay a command.

### 12.3 FIND Java tests

- entrypoint waits for executor readiness, registers `find` idempotently,
  rejects name collision, and does not change upstream `AltoClefCommands` or
  register `go` by accident;
- domain-specific registry resolution and invalid raw-target rejection;
- eligible-mob filtering that excludes player/item/projectile/transient kinds;
- nearest eligible observed candidate selection within the same dimension,
  fixed per-kind bounds, immutable origin, squared-distance ranking, and the
  exact UUID/BlockPos tie-breakers;
- stable entity UUID/block position/item-entity identity revalidation;
- terminal BlockPos refresh/floor semantics for moving entities;
- cached-but-unloaded, stale, removed, and wrong-dimension candidates never
  produce `FOUND_AND_REPORTED`;
- the FIND Task/collaborators request no movement, goal, path, input mutation,
  or extra global cleanup;
- established `UserTaskChain` post-task cleanup remains byte-for-byte/behavior-
  equivalent to the non-FIND baseline; avoiding it is not smuggled into FIND;
- one finite read-only `FindObservationTask`, one immutable outcome, and one
  matching `TaskFinishedEvent`; callback-only unchanged-idle-root completion
  cannot create strong `FOUND_AND_REPORTED` evidence;
- normal post-finish `onStop` and late callbacks cannot overwrite the one
  immutable outcome;
- exact outer lifecycle proof and nested FIND profile projection through the
  focused bridge profile dispatcher, with GET/STORE_HOME/STOP key mixing
  rejected;
- dropped-item report mode performs zero FIND movement and zero target pickup;
- no attack, kill, block break, interaction, container open, pickup request, or
  acquisition task/effect;
- target loss, not-observed, STOP, interrupt, and exception cleanup;
- a STOPped FIND retires its original terminal presentation so UI/output/TTS
  publish only the one validated `멈췄어` response;
- exactly one retained typed terminal claim after STOP arbitration.

If approach mode is separately approved, add tests for already-near zero-work
success, safe hostile-entity stand-off, unreachable path, timeout, bounded
search exhaustion, and release of only FIND-owned goal/path/input state. Those
tests must also prove FIND never requests pickup or treats an incidental pickup
as FIND success.

### 12.4 All-command matrix tests

For every current 26-row command and FIND after registration:

- source-registration/parser/admission/bridge/effect/public axes are asserted
  independently;
- representative Korean forms and every typed slot are covered;
- original-case player identity is preserved for each player-target slot;
- canonical and Korean item/entity/block aliases are tested by domain;
- malformed explicit numeric arguments never fall back to an omitted-value
  default, and every command-specific bound/unit is tested;
- safety tier and confirmation mode cannot be bypassed;
- `chatclef off` preserves one independent raw/direct recovery path and does
  not lose or duplicate its terminal receipt;
- GAMMA remains public-disabled until `accepted -> one authoritative terminal
  reconciliation -> active owner cleared once -> immediate next command
  admitted` passes; a cautious sentence alone is not ownership retirement or
  strong effect proof;
- Chat and final microphone have parity after confirmation requirements;
- all 25 current semantic capabilities plus FIND are publicly executable from
  both trusted sources after their applicable confirmation, while the Unicode
  alias row is separately accounted for through canonical
  `auto_deposit_trust` compilation;
- raw/native command behavior remains unchanged;
- command-applicable START/status/terminal wording remains grounded in
  immutable accepted facts, and trusted STOP still produces zero visible START
  responses;
- wrong correlation produces no response;
- malformed evidence produces one cautious response at most;
- terminal claims, output, UI, and TTS are consumed once;
- no LLM recall, automatic retry, automatic replay, or command resubmission.

The existing 26-command exact-set tests must prove that an isolated FIND count
drift would fail. The activation unit updates all 27 authoritative artifacts
atomically so no committed boundary has an incoherent catalog. Tests must not
be weakened to accept arbitrary count drift.

## 13. Implementation and verification order

The implementation order is retained below. Before the failed live run, steps 1
and 2 were recorded as source-complete with focused tests in the mixed worktree;
this document review did not rerun those tests or prove an executable rollback
unit. Runtime evidence now verifies the narrow Korean translation/admission path
but not navigation or a gameplay effect. Steps 3 onward remain future work:

1. Freeze current GOTO failures, 26-command readiness axes, and absent
   `find`/`go` registrations with characterization tests.
2. Implement the focused Python GOTO ownership classifier and coordinate
   parser, including whole-utterance consumption, and verify the full source
   matrix. This is rollback unit A and requires no Java build.
3. Resolve the documented FIND base decisions: target kinds, IDs, ambiguity,
   eligibility, loaded-scope bounds, safety tier, source matrix,
   runtime-catalogue binding, lifecycle, result payload, and terminal reasons.
   Resolve approach-specific bounds only if that optional mode is approved.
4. Implement the LAVI-owned Java FIND command, resolvers, observation service,
   finite read-only observation Task, bridge profile dispatcher/projector,
   protocol update, and focused Java tests without changing upstream
   ATTACK/SCAN/GOTO behavior. Add an approach task only if that mode was
   explicitly approved.
5. Register FIND through its LAVI entrypoint and atomically synchronize the
   27-command snapshot/registry/support/lifecycle/evidence/phrase closures with
   the Python FIND row still public-disabled/cautious. Run and record B1's clean
   forced build at this coherent boundary.
6. Add Python FIND classification, resolution, compilation, admission,
   lifecycle evidence, renderer, Chat/final-microphone tests, and only then
   enable its public Korean route. This B2 unit consumes the verified B1 Java
   artifact and does not require another Java build unless Java changes.
7. Reconcile each affected row's known metadata/lifecycle gaps first,
   including `gamer` metadata and GAMMA authoritative terminal/active-owner
   retirement.
8. Expand Korean input command-by-command in safety-tier groups, keeping each
   command disabled until its parse, admission, lifecycle, evidence, source,
   and confirmation gates pass.
9. Run focused Python tests, the full Minecraft Python suite, and Ruff. For
   every Java-changing rollback unit, run its focused Java tests and one clean
   forced Fabric build; do not reuse another unit's build as isolated proof.
10. Inspect the diff, exact 27-command artifacts, fresh JAR identity, and
    available bounded logs.
11. If the active implementation request already names an exact disposable
    runtime target and reproduction, continue through that authorized runtime
    matrix without a second stage approval. Otherwise do not deploy or launch
    Minecraft and report deployment/runtime as `NOT_RUN`.

The future Java build command for FIND or a separately approved direct `@go`
alias is:

```powershell
Set-Location -LiteralPath 'C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1'
.\gradlew.bat clean build --rerun-tasks --no-build-cache --no-daemon --stacktrace
```

Documentation alone does not require this build. The Python-only GOTO
ownership/grammar change alone does not require it. New `@find` Java behavior
does.

Each build record names the Java/JDK and Gradle JVM, relevant dirty inputs,
executed test/build unit, artifact absolute path, size, modification time, and
SHA-256. A build from a mixed worktree is labelled mixed-provenance evidence.

## 14. Historical intended rollout decomposition — not an executable rollback manifest

The following table records the intended dependency ordering and conceptual
isolation of future units. It does not prove current file/hunk provenance,
identify exact inverse hunks, or authorize rollback of the mixed worktree.

| Unit | Contents | Java build |
| --- | --- | --- |
| A | focused Korean GOTO ownership/grammar and tests | no |
| B1 | Java FIND command/resolvers/read-only Task, bridge profile dispatch/projection, protocol update, entrypoint/registrar, Java tests, and exact 27-name closures with the Python FIND route public-disabled/cautious; optional approach only after approval | yes |
| B2 | Python FIND intent/admission/lifecycle/evidence/rendering, source matrix, and public enablement | no new build; requires the verified B1 artifact |
| C1 | low-risk/read-only Korean command exposures | only if Java result changes are required |
| C2 | bounded item/task Korean command exposures | only if Java lifecycle/evidence gaps require changes |
| C3 | R3/R4 confirmation infrastructure and high-risk command exposures | only if Java behavior/result changes are required |
| C4 | GAMMA authoritative terminal/active-owner retirement, sequential-admission tests, then Korean exposure; strong effect proof remains separate | yes for any Java correction |
| D | optional direct `@go` delegation plus every exact-name closure; with FIND active, the catalog changes from 27 to 28 | yes |

Intended future rollback ordering, applicable only after a strict provenance
audit establishes independently executable units and the user explicitly
approves each exact method/hunk or impact-specified whole-file unit. Path-only
or generic approval is insufficient; any failed GOTO recovery also remains
subject to the current incident record's Gate G/G.1:

- disable public exposure before removing a parser, resolver, evaluator, or
  renderer;
- roll B2 FIND public exposure back before B1; B1 may then remove the Java
  entrypoint/command/profile and restore all exact 26-name artifacts together;
- never remove a Python registry/profile row while Java registration still
  makes it part of the authoritative exact source catalog;
- rollback of FIND must not revert GOTO ownership/grammar support;
- rollback of an individual Korean command must not alter raw/native Java
  execution for other commands;
- rollback of the optional `@go` alias must restore every exact-name artifact
  and leave canonical `@goto` intact.

## 15. Completion criteria

This feature is complete only when all applicable conditions hold:

- every requested coordinate form compiles once to canonical `goto x y z` from
  both LAVI Chat and final microphone;
- malformed coordinate forms submit nothing;
- `@find` is registered exactly once and appears in every authoritative
  27-command artifact;
- Korean FIND resolves all enabled target kinds through domain-specific
  registries and aliases; the initial required kinds are eligible mobs under
  `entity`, player, block, and dropped item, while structure is conditional;
- “all” claims distinguish vocabulary coverage from bounded instance discovery;
- base FIND observes within fixed per-kind limits and reports one revalidated
  location through its finite read-only Task without FIND movement;
- dropped-item base mode performs zero FIND movement and zero target pickup;
- if approach is explicitly approved, movement-enabled kinds reach and
  revalidate a safe stand-off without requesting attack, mining, interaction,
  pickup, acquisition, or unrelated cleanup;
- GAMMA produces one authoritative terminal/reconciliation, releases its
  active owner once, and permits an immediate next command before its Korean
  route can count as public; cautious wording does not claim strong effect;
- all 25 current semantic command capabilities plus FIND have tested Korean
  intent/slot contracts and are publicly executable from both trusted LAVI Chat
  and final microphone after every applicable confirmation;
- all 27 registered-name rows are accounted for, with the duplicate Unicode
  alias explicitly mapped to canonical `auto_deposit_trust` semantics rather
  than double-counted as another capability;
- every translatable target in each command-specific capability catalogue has
  verified Korean-alias coverage, while opaque/namespaced fallback coverage is
  measured separately;
- each semantic command becomes public only after its independent readiness
  and safety gates pass;
- R3/R4 Chat/final-microphone execution requires an exact one-time confirmation;
- existing STOP, active-command busy/status, correlation, terminal CAS,
  output, UI, and TTS exactly-once behavior remains intact;
- raw/legacy commands and canonical `@goto` retain compatibility;
- focused and full Python tests pass;
- FIND Java tests and the clean forced Fabric build pass;
- deployment and live runtime are reported `NOT_RUN` unless the active request
  authorizes them; if run, their matching-JAR result is recorded independently.

## 16. Explicit non-goals

This contract does not authorize:

- Forge or MineMind files, values, packages, tests, or runtime behavior;
- a shared Fabric/Forge bridge or generic backend selector;
- Minecraft, Fabric, Loader, Loom, Gradle, Java, ChatClef, AltoClef, Baritone,
  or Carry On version changes;
- a broad refactor of upstream `GotoCommand`, `ScanCommand`,
  `AttackPlayerOrMobCommand`, `Task`, `TaskRunner`, or Baritone lifecycle code;
- global world omniscience, unopened-container inventory discovery, or a claim
  that an unobserved target does not exist;
- any FIND-requested or FIND-success-credited attack, mining, pickup,
  container interaction, or item acquisition;
- cross-dimension FIND or unapproved locate-and-approach behavior;
- infinite exploration, unbounded retry, LLM command repair, LLM recall,
  automatic resubmission, or replay after busy/terminal state;
- lowering safety tiers or bypassing confirmation to reach an “all commands”
  metric;
- adding direct `@go` in the same rollback unit as Korean GOTO grammar;
- deployment, Minecraft launch, world mutation, cache deletion, commit, push,
  or release as part of this documentation-only task.

## 17. Decisions required before behavior implementation

Decision gates are scoped by workstream so one optional feature does not block
a separately closable base unit.

### 17.1 Before base FIND source work

Decide, record, and test:

1. the approved Korean mob/block/item alias generation, per-domain eligibility,
   and collision policy for vanilla and namespaced mod targets;
2. FIND's base locate/report safety tier, confirmation mode, and allowed-source
   matrix;
3. the fixed meaning of initial item FIND as an observed dropped `ItemEntity`
   only, with inventories and container contents remaining separate;
4. exact per-kind loaded-scope query radius, candidate cap, elapsed budget, and
   bounded-index/enumeration completeness rules;
5. how a Fabric-only active runtime ITEM/BLOCK/eligible-ENTITY_TYPE catalogue
   is exported to Python and bound by version/digest so static aliases cannot
   claim a different installed registry;
6. the closed non-success outer-status/outcome matrix and the exact v1 fields
   in Section 10, recorded in the Fabric bridge protocol before activation.

The initial base kinds remain eligible mobs under `entity`, player, block, and
dropped item. Structure is disabled by default and does not block this base.

### 17.2 Before a structure FIND extension

Decide whether the structure kind is enabled, which structures the actual Java
resolver supports, and whether Korean structure phrases route to FIND or to the
existing two-value `locate_structure` command. Do not create structure packages
or registry claims before this decision.

### 17.3 Before optional approach source work

Decide:

1. whether `LOCATE_AND_APPROACH` is authorized at all and which explicit
   Korean/direct form requests it;
2. per-kind exploration radius, elapsed-time budget, and safe stand-off
   distances; dropped-item approach remains a later separate scope requiring
   path-wide no-pickup evidence;
3. whether one conservative command-level safety tier covers both modes or the
   registry gains a tested mode-specific policy extension, without allowing
   approach mode to bypass the base command's admission;
4. whether both modes use one compatible finite-operation lifecycle or a
   validated descriptor-mode lifecycle dispatcher, because the
   `REVIEWED_HEAD` registry was keyed only by command name.

### 17.4 Before numeric/settings public exposure

Decide and test each command's exact integer/decimal bounds, accepted closed
Korean numeral forms, units, and genuine omitted-value defaults. GAMMA's
semantic minimum/maximum was not fixed by the `REVIEWED_HEAD` Java source and
must be recorded before public Korean exposure.

GAMMA has a separate mandatory lifecycle gate. At `REVIEWED_HEAD`, its Java
command did not call the finish callback, and Python's accepted-submission
cautious fact does not authoritatively reconcile and release the active ordinary-command
owner. Before `public_korean=true`, a focused unit must produce one
authoritative bridge terminal/reconciliation, clear that owner once, and admit
the immediate next command. Strong gamma-effect proof is a different optional
unit; until it exists, the terminal wording remains cautious even though
ownership retirement is authoritative. Any Java lifecycle correction receives
its own tests and clean build. If source evidence requires a behavior hunk in
upstream-derived `SetGammaCommand`, apply the documented minimal-hunk
engine-divergence provenance, marker, regression, and rollback gate rather than
hiding it in Korean parsing.

### 17.5 Before R3/R4 Korean public exposure

Decide the exact Chat/final-microphone confirmation wording, expiry, identity
binding, one-time consumption, cancellation, recovery path, and terminal
delivery rules. This C3 decision does not block GOTO or a base FIND route after
that route's safety tier/source matrix has itself been classified and approved.

### 17.6 Before an optional direct `@go` alias

Explicitly decide whether native `@go` is needed in addition to canonical
`@goto`. If added with FIND active, synchronize every exact-name artifact from
27 to 28 in unit D. This decision does not block the Python GOTO input change
or FIND.

Until a workstream's applicable decisions and tests exist, documentation must
continue to call that workstream `DOCUMENTED_NOT_IMPLEMENTED`. An undecided
optional extension remains disabled; it does not block a base unit whose own
prerequisites hold and must not be presented as current runtime behavior.
