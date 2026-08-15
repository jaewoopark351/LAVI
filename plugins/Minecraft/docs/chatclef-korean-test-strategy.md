<!-- 20260815_kpopmodder: Documented the Korean ChatClef test strategy before expanding alias and action support. -->

# ChatClef Korean Test Strategy

Date: 2026-08-15

This document records the documentation-only test strategy for Korean Fabric
ChatClef command handling.

It is documentation only. It does not approve Python behavior changes, Java
changes, wire protocol changes, DTO changes, ChatClef / AltoClef engine
changes, test execution, Minecraft launch, runtime reproduction, commit, push,
or automatic command replay.

## Related Documents

Read this document with:

```text
plugins/Minecraft/docs/chatclef-korean-item-command-resolution-analysis.md
plugins/Minecraft/docs/chatclef-korean-item-action-alias-v2-plan.md
plugins/Minecraft/docs/chatclef-command-lifecycle-and-threading.md
plugins/Minecraft/docs/fabric-chatclef-bridge-protocol-v1.md
```

The alias v2 plan owns item/action design. This document owns how that design
should be tested without confusing current working behavior, future features,
and live Minecraft side effects.

## Scope

The test strategy applies to the Python-owned Fabric ChatClef path:

```text
Korean or natural-language input
  -> input gate
  -> rule or LLM translation
  -> item/action resolution
  -> prefixless ChatClef DSL
  -> router submission boundary
  -> Fabric ChatClef adapter
```

The current scope is Fabric ChatClef only. Tests in this scope must not import
Forge MineMind fixtures, start a Forge backend, fall back to MineMind, or create
shared Fabric/Forge runtime ownership.

## Three Separate Test Axes

Keep these concerns separate:

| Axis | Purpose | Must pass now? |
| --- | --- | --- |
| Current correctness | Lock the behavior that is implemented now | yes |
| Alias coverage snapshot | Show how broad the current alias set is | yes, without requiring full coverage |
| Support/planned matrix | Track commands that exist in ChatClef but are not yet Korean natural-language features | yes, without forcing future features to pass |

Do not mix these axes. Mixing them can make CI fail because of planned features,
or can make partial alias coverage look like "all Minecraft items are
supported."

## Prefix Ownership Contract

Python natural-language handling must emit prefixless ChatClef DSL.

Examples:

```text
다이아몬드 가져와줘 -> get diamond 1
돌 10개 가져와줘 -> get stone 10
철 주괴 10개 가져와줘 -> get iron_ingot 10
```

It must not emit:

```text
@get diamond 1
@get stone 10
@get iron_ingot 10
```

The `@` prefix belongs to raw Minecraft / ChatClef command entrypoints or the
Java dispatcher boundary, not to the Python Korean natural-language compiler.

| Path | `@` allowed? | Test rule |
| --- | --- | --- |
| Python natural-language compiler output | no | assert prefixless DSL |
| natural-language router to adapter request | no | adapter request must not start with `@` |
| raw ChatClef command UI input | yes | do not globally ban raw `@get` fixtures |
| generic adapter or transport fixture | yes when testing raw command transport | keep separate from natural-language tests |
| Java dispatcher internal execution | yes if needed | dispatcher owns adding a missing prefix |

Recommended contract names:

```text
test_natural_language_compiler_emits_prefixless_dsl
test_router_preserves_prefixless_compiled_command
test_raw_command_entrypoint_may_accept_prefixed_command
test_dispatcher_does_not_double_prefix_existing_command
```

## Test Order

Add or maintain tests in this order:

1. Architecture and prefix ownership contracts.
2. Small unit tests for gate, acquisition verb matcher, parser, resolver,
   catalog, and compiler.
3. Offline Korean-to-DSL integration tests.
4. Router exactly-once submission tests with fake or recording adapters.
5. Alias/catalog contract tests.
6. Alias coverage snapshot tests.
7. Command support matrix tests.
8. Loopback transport tests without a real Minecraft client.
9. Opt-in live Minecraft tests.

The goal is quick failure localization. If a planned phrase such as `철 10개 캐줘` fails, the test suite
should show whether the failure came from gate selection, parser phrase
extraction, alias resolution, catalog validation, compiler serialization, or
router submission.

## Current Committed-Baseline Golden Cases

These cases represent committed-baseline GET behavior only. Do not include
working-tree-only alias expansion or mining/acquisition verbs here until Phase
0 proves the source and the focused tests are committed.

| Korean input | Expected DSL |
| --- | --- |
| `철괴 가져와줘` | `get iron_ingot 1` |
| `철 주괴 10개 가져와줘` | `get iron_ingot 10` |
| `금괴 가져와줘` | `get gold_ingot 1` |
| `구운 소고기 가져와줘` | `get cooked_beef 1` |
| `원목 가져와줘` | `get log 1` |

Current evidence tests:

```text
tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py
tests/minecraft_chatclef/lavi_input/test_korean_input_to_chatclef_submission_path.py
tests/minecraft_chatclef/alias_contract/test_get_alias_translation_priority.py
```

Keep the assertions separated by responsibility.

Offline Korean-to-DSL tests prove:

```text
status == VALIDATED
executable == true
command == exact expected prefixless DSL
not command.startswith("@")
```

Instrumented pipeline tests prove:

```text
translation call count == 1
resolver call count == 1 when the test instruments resolver separately
compiler call count == 1 when the test instruments compiler separately
```

Router submission tests prove:

```text
VALIDATED + connected + idle -> submit count == 1
false positive / unknown / invalid / disconnected / busy -> submit count == 0
rejected adapter result -> total submit count == 1 and retry count == 0
all paths -> submit count <= 1
submitted command is exact prefixless DSL
```

Do not use only `submit count <= 1` for a successful route. That would allow a
bug where the router validates a command but never submits it.

## Planned And Future Korean Action Matching

Do not treat mining/acquisition verb tests as committed-baseline regressions
unless the matching source and tests are proven in Phase 0.

Planned or source-proven GET acquisition and mining phrases:

```text
다이아몬드 캐줘 -> get diamond 1
석탄 5개 캐와줘 -> get coal 5
철 10개 캐줘 -> get iron_ingot 10
```

If the committed baseline lacks `캐줘`, `캐와`, or `채굴해줘` recognition, these
belong to separate implementation or restoration work, not to current
regression preservation.

The future Phase 3 action matcher should distinguish two responsibilities:

```text
GET mining/acquisition verb recognition:
  source-proven existing behavior or newly approved implementation work

cross-command action precedence:
  cross-command action precedence for GET, EQUIP, DEPOSIT, and GIVE
```

Future tests should therefore separate:

```text
GET mining/acquisition regression tests
cross-command action precedence tests
```

This prevents duplicate matcher work and prevents a future implementation from
removing existing mining phrases while adding equip/deposit/give support.

## False Positive And No-Submit Cases

Negative examples should prove "no adapter submission." They do not all need to
fail at the same layer.

Examples:

```text
캐나다 여행 얘기하자
캐릭터 만들어줘
캐시 확인해줘
오늘 다이아몬드가 예쁘다
돌 10개가 창고에 있어
마인크래프트에서 다이아몬드 캐는 법 알려줘
```

Some phrases may pass a broad lexical candidate gate but fail later during
translation, item resolution, action validation, or catalog validation. The
stable contract is:

```text
adapter submit count == 0
no automatic retry
no accidental raw ChatClef command
```

## Alias And Catalog Contracts

Hard contract tests should validate every alias currently present. They should
not require every Minecraft item to already have a Korean alias.

Required hard contracts:

```text
every runtime alias target exists in the ChatClef catalog
no empty alias
no empty target
target syntax is lowercase letters, digits, and underscores
compact aliases do not map to conflicting targets
fixed aliases use exact compact matching, not substring matching
material pseudo-targets are not standalone item targets
contextual policy entries are not flattened into runtime item aliases
compiler output for alias GET is prefixless
dangerous characters such as @, #, semicolon, and newline are rejected
deterministic generation produces byte-identical runtime JSON
```

Contextual non-flattening tests should verify:

```text
"잡템" not in runtime item aliases
"갑옷" not in runtime item aliases
"철 갑옷" not in runtime item aliases as an equip shortcut
runtime alias values do not contain bare_deposit
runtime alias values do not contain full_armor_set_shortcut
runtime alias values do not contain armor_set or all pseudo-targets
```

Dynamic alias test shape:

```text
for each Korean alias and target in runtime aliases:
  resolve(alias) == target
  compile(GET_ITEM, target, 1) == "get <target> 1"
  compiled command does not start with "@"
```

This means:

```text
All currently registered aliases are valid.
```

It does not mean:

```text
All Minecraft items are already supported in Korean.
```

## Multi-Item GET Contracts

Java `ItemList` grammar and Python canonical serialization are separate test
responsibilities.

Source-backed Java parser fixtures:

```text
get [log 20, stone 10]
  -> accepts two targets

get [diamond 1, coal 5]
  -> accepts explicit one-count and multi-count elements

get [stone 2, stone 3]
  -> duplicate target count is summed to stone 5

get [stone, stone 3]
  -> omitted count defaults to 1 and duplicate target count is summed to stone 4

get []
  -> rejected or documented as invalid before Python emits it

get [[stone 1]]
  -> rejected as invalid nested bracket structure
```

Do not assert Java `HashMap` iteration order as a stable command contract.

Python canonical multi-item GET output policy:

```text
use bracketed ItemList syntax
separate entries with comma-space
emit every count explicitly
merge duplicate canonical targets by summing counts
keep the first occurrence position for a merged target
emit prefixless DSL only
```

Single-item canonical count output policy:

```text
GET always emits explicit count, for example get diamond 1
GIVE always emits explicit count, for example give Steve diamond 1
specific DEPOSIT emits explicit count only after Korean quantity policy resolves it
bare DEPOSIT emits exactly deposit
```

Item-action count validation:

```text
source-backed Java parser boundary:
  ItemList and Arg<Integer> parse counts with Integer.parseInt
  accepted parse range is Java signed 32-bit int:
    -2147483648 through 2147483647
  non-integer text and values outside that parse range fail Java parsing
  ItemList duplicate targets are summed with int addition
  ItemList does not provide an overflow guard for duplicate-target sums

Python natural-language compiler contract:
count must be a positive integer
minimum count is 1
maximum count is 2147483647
zero count rejects
negative count rejects
non-integer count rejects
single count overflow rejects before adapter submission
duplicate multi-item GET sum must be computed before emission
duplicate-target sum overflow rejects before adapter submission
duplicate multi-item GET sum must remain in the Java-compatible positive range
new ItemActionResolution or multi-item paths must not bypass this validation
```

## Coverage Snapshot

Coverage snapshot tests should report breadth without turning partial coverage
into a failure.

Keep runtime coverage and proposed v2 draft coverage separate. The supplied v2
draft values `191`, `167`, `424`, and `28.26` describe the proposed validated
seed from the v2 planning bundle. They are not the current runtime resource
unless that draft is explicitly generated into:

```text
plugins/Minecraft/fabric/chatclef/intent/resources/korean_item_aliases.json
```

Current runtime snapshot fields should describe the actual runtime file:

```json
{
  "schema_version": 1,
  "snapshot_kind": "runtime_current",
  "snapshot_authority": "git_blob_bytes",
  "snapshot_baseline_commit": "c912ff6491711c33dbf2e66320e634196f866ae8",
  "catalog_path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/CataloguedResources.txt",
  "catalog_sha256": "4591ae83151dd2643943dfdcbb2e89a53d26de393c583902370c3f4d3c57188a",
  "alias_source_path": "plugins/Minecraft/fabric/chatclef/intent/resources/korean_item_aliases.json",
  "alias_source_sha256": "fd27685fa50d3364add9d9df5a0675c52c5b13ff862c07ef429d5e67055b45fd",
  "raw_catalog_total": 591,
  "raw_catalog_covered_targets": 4,
  "raw_catalog_missing_targets": 587,
  "raw_catalog_coverage_percent": 0.68,
  "direct_alias_count": 11,
  "direct_user_facing_target_total": null,
  "resolved_user_facing_targets": null,
  "unresolved_user_facing_targets": null,
  "user_facing_coverage_percent": null,
  "catalog_outside_targets": 0,
  "compact_conflicts": 0
}
```

The proposed v2 draft snapshot should be a separate artifact:

```json
{
  "schema_version": 1,
  "snapshot_kind": "proposed_v2_draft",
  "alias_source_path": "C:\\Users\\jaewo\\Downloads\\korean_item_aliases.v2.draft.json",
  "alias_source_sha256": "8b3541bba76c13ef7f1c35eb7cccb043d22c8ae3deac7a94482ff83dfd5f42f0",
  "default_policy_source_path": "C:\\Users\\jaewo\\Downloads\\korean_item_aliases.default_policy.draft.json",
  "default_policy_source_sha256": "e5f85cfef338138b3e99a7e2e86e27f7e378132bbca217df6c6f256ef78f311f",
  "target_policy_source_path": "C:\\Users\\jaewo\\Downloads\\chatclef_item_command_target_policy.draft.json",
  "target_policy_source_sha256": "595da76bdb23274c41de170ad662b85d21d98b42da2ee444eb74563055de61f7",
  "canonicalization_policy_source_path": "<not supplied in the reviewed draft bundle; required before Phase 1 implementation>",
  "canonicalization_policy_source_sha256": "<fill when the repository-owned canonicalization source is committed>",
  "validation_report_path": "C:\\Users\\jaewo\\Downloads\\korean_item_aliases.v2.validation.json",
  "validation_report_sha256": "4f28210bbd559bea6047c545a6a28cf05b83c17e7f8b916064234ff468e760e6",
  "coverage_report_path": "C:\\Users\\jaewo\\Downloads\\korean_item_aliases.v2.coverage.json",
  "coverage_report_sha256": "ba7f8960ec423db86a80fcd283cb46278c86e2b981c0512ffd9f9359395f919d",
  "generator_path": "C:\\Users\\jaewo\\Downloads\\generate_korean_item_aliases.py",
  "generator_sha256": "f19ce90755c6be3fceafe7e322dc25012b09d6c53e700ad3c4a4b6904b376c3f",
  "generator_schema_version": 2,
  "raw_catalog_total": 591,
  "raw_catalog_covered_targets": 167,
  "raw_catalog_missing_targets": 424,
  "raw_catalog_coverage_percent": 28.26,
  "direct_alias_count": 191,
  "catalog_outside_targets": 0,
  "compact_conflicts": 0
}
```

Generation provenance tests should require these source hashes before accepting
a generated alias or coverage artifact:

```text
catalog SHA-256
ko_kr.json SHA-256
generator schema version
curated policy SHA-256
default policy SHA-256
canonicalization policy SHA-256
item-command target policy SHA-256
```

The `C:\Users\jaewo\Downloads\...` values are historical provenance for the
reviewed draft bundle. Phase 1 generator and CI work should use repository-owned
sources after they are created:

```text
plugins/Minecraft/tools/chatclef_aliases/sources/korean_item_aliases.curated.json
plugins/Minecraft/tools/chatclef_aliases/sources/korean_item_aliases.default_policy.json
plugins/Minecraft/tools/chatclef_aliases/sources/chatclef_target_canonicalization.json
plugins/Minecraft/tools/chatclef_aliases/sources/chatclef_item_command_target_policy.json
```

Download paths may remain in reports as historical input provenance, but they
must not be the long-term CI source of truth.

Suggested artifact names:

```text
korean_item_aliases.runtime.snapshot.json
korean_item_aliases.v2_draft.snapshot.json
```

The `user_facing_*` fields remain `null` until the generator and catalog
classifier can distinguish direct user-facing targets, generic targets, legacy
synonyms, command-internal targets, and canonicalized targets.

Coverage tests should verify:

```text
recomputed snapshot matches the committed snapshot
snapshot schema is valid
catalog-outside targets == 0
compact conflicts == 0
coverage is at or above the committed floor
the draft is not reported as full support
```

Keep non-regression floors separate from snapshots. Suggested files:

```text
korean_alias_coverage.snapshot.json
korean_alias_coverage.floor.json
```

Suggested floor shape:

```json
{
  "snapshot_kind": "runtime_current",
  "minimum_raw_covered_targets": 4
}
```

The authoritative non-regression value is the integer covered-target count.
Percentages are derived display values only because rounding can create
unnecessary failures.

Define raw coverage as:

```text
raw_catalog_covered_targets
  = unique final runtime alias target values
    intersected with the current ChatClef catalog target set

raw_catalog_missing_targets
  = raw_catalog_total - raw_catalog_covered_targets

raw_catalog_coverage_percent
  = round(raw_catalog_covered_targets / raw_catalog_total * 100, 2)
```

This raw count covers flat runtime alias targets only. It does not include
future command-specific contextual policy, equipment shortcuts, deposit modes,
give recipients, or unresolved user-facing canonicalization until those sources
are explicitly added to the coverage classifier.

When the classifier later enables user-facing coverage, use canonical targets
as the denominator instead of direct targets only:

```text
canonical_user_facing_target_total
  = size(canonical_user_facing_targets)

canonical_user_facing_targets
  = canonicalize(direct_user_facing_targets union generic_policy_targets)
    minus canonicalize(command_internal_only_targets)

resolved_canonical_user_facing_targets
  = canonical_user_facing_targets intersect targets_resolvable_from_korean

user_facing_coverage_percent
  = round(
      size(resolved_canonical_user_facing_targets)
      / canonical_user_facing_target_total
      * 100,
      2
    )
```

Canonicalized legacy targets are reported as `legacy_target -> canonical_target`
mappings. They are not subtracted again after canonicalization and are not
separate denominator targets.

Classifier invariants:

```text
each raw catalog target has exactly one primary classification
all user-facing coverage set operations compare canonical target IDs
command_internal_only_targets means targets with no user-facing direct or
  generic-policy source
if a canonical target has at least one user-facing source, an internal alias to
  the same canonical target must not remove the whole canonical target
targets_resolvable_from_korean contains concrete canonical catalog targets only
command modes, shortcut tokens, recipients, quantities, and routing statuses
  never count as item coverage
```

Lowering the floor is a separate review decision. Do not hide a coverage
regression by updating the snapshot and floor together.

Do not add this as a current passing test:

```text
all 591 catalog targets have Korean aliases
```

The catalog can contain generic, legacy, internal, or command-only targets.
Future full-support gates should use unresolved canonical user-facing targets,
not the raw catalog total.

## Offline Test Determinism

Tier 1 offline tests must not call external LLMs, OpenAI APIs, network
services, Minecraft, Java processes, or a running LAVI UI.

Rules:

```text
rule parser path -> test directly
resolver/compiler path -> test directly
router path -> fake or recording adapter only
LLM extractor path -> deterministic fake or stub
external model/network request -> forbidden
```

This keeps Korean alias and command tests stable when the network is offline or
when a model would otherwise produce nondeterministic output.

## Korean Test Source Readability

Korean test inputs should be stored as real UTF-8 Korean literals in Python
test source files.

Prefer:

```python
"다이아몬드 가져와줘"
```

Avoid:

```python
"\ub2e4\uc774\uc544\ubaac\ub4dc \uac00\uc838\uc640\uc918"
```

The escaped form executes to the same runtime value, but it makes test reviews
hard and hides the natural-language contract being tested. Do not introduce a
wrapper class only to pair Korean display text with escaped text in test cases.
The parser, resolver, router, and service tests should receive the same kind of
Korean string that a user would type.

If escaped text is needed for a failure message, snapshot, or debug report, use
a small output-only helper at that call site, for example:

```python
text.encode("unicode_escape").decode("ascii")
```

Generated JSON or snapshot artifacts that should be read by humans should use
`ensure_ascii=False` unless the artifact's explicit purpose is to verify escaped
serialization.

## Command Support Matrix

ChatClef can register commands that Python Korean natural language does not yet
support. Every registered command should have a matrix row so the project does
not confuse "registered in Java" with "implemented in Korean."

The matrix must use exactly one current value for each axis. Do not write
values such as `PLANNED or RAW_ONLY`; those cannot become stable tests.

Command owner values:

```text
chatclef_java
lavi_overlay
```

Natural-language status values:

```text
IMPLEMENTED
PLANNED
RAW_ONLY
EXPLICIT_UNSUPPORTED
```

Execution effect values:

```text
READ_ONLY
CLIENT_STATE
TASK_MUTATING
MOVEMENT_MUTATING
INVENTORY_MUTATING
WORLD_MUTATING
DANGEROUS_MUTATING
UNKNOWN_UNTIL_VERIFIED
```

`LIVE_ONLY` is not a natural-language status. Live behavior belongs to
`effect_class` and `live_test_tier`.

Verification state values:

```text
VERIFIED
PLANNED
SOURCE_REVIEW_REQUIRED
NOT_APPLICABLE
```

`behavior_verified` is always boolean. Do not mix values such as `yes`,
`planned`, or explanatory text into that field. Use `verification_state` and
`verification_note` for non-boolean context.

Matrix row shape:

```json
{
  "command": "gamma",
  "owner": "chatclef_java",
  "natural_language_status": "RAW_ONLY",
  "effect_class": "CLIENT_STATE",
  "live_test_tier": 4,
  "live_test_allowed": false,
  "live_test_condition": "CLIENT_STATE_RESTORE_REQUIRED",
  "live_test_note": "Requires explicit opt-in and state restoration.",
  "behavior_verified": true,
  "verification_state": "VERIFIED",
  "verification_note": "Source reviewed for client-state effect only.",
  "golden_tests": []
}
```

For commands whose effect is unknown, do not assign a live tier yet:

```json
{
  "command": "scan",
  "owner": "chatclef_java",
  "natural_language_status": "RAW_ONLY",
  "effect_class": "UNKNOWN_UNTIL_VERIFIED",
  "live_test_tier": null,
  "live_test_allowed": false,
  "live_test_condition": "SOURCE_REVIEW_REQUIRED",
  "live_test_note": "Live tests are blocked until effect classification is verified.",
  "behavior_verified": false,
  "verification_state": "SOURCE_REVIEW_REQUIRED",
  "verification_note": "Review implementation before assigning any live tier.",
  "golden_tests": []
}
```

Registered command authority must also be explicit. The matrix should be backed
by source snapshot artifacts before the matrix contract tests are implemented.
Required future artifacts:

```text
tests/minecraft_chatclef/command_catalog/chatclef_registered_commands.snapshot.json
tests/minecraft_chatclef/command_catalog/chatclef_command_support_matrix.json
```

Snapshot shape:

```json
{
  "schema_version": 1,
  "sources": {
    "chatclef_java_registered_commands": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/AltoClefCommands.java",
      "sha256": "dc03102ecb1659f1ed0d0fc55852864c887541f6d5287f5ad39fd7aed563d5f7"
    },
    "lavi_overlay_commands": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/overlay/command/OverlayCommand.java",
      "sha256": "ee3a5dcda914fb2c82a5776efe2963d4efa72cd0226b172ec285110fdba23e0f"
    }
  },
  "commands": [
    {
      "name": "get",
      "owner": "chatclef_java"
    },
    {
      "name": "overlay",
      "owner": "lavi_overlay"
    }
  ]
}
```

The `commands` list in the real snapshot must include every registered command,
not only the two abbreviated examples above. Matrix tests should fail when a
registered command has no matrix row, or when a matrix row points to no
registered source unless it is explicitly marked as a virtual command.

The artifact paths and schema above are planned Phase 0 artifacts. Phase 0 is
not complete until those JSON files are actually committed and contract tests
prove that they contain every registered command, actual source hashes, exact
owners, boolean `live_test_allowed`, and command-specific `golden_case_ids`.

Java item-command contract provenance must also be tested before trusting
Python fixtures:

```text
tests/minecraft_chatclef/command_catalog/test_java_item_command_contract_provenance.py
```

Required assertion:

```text
java_contract_baseline_commit matches the documented baseline
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/GetCommand.java hash matches the documented contract source hash
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/EquipCommand.java hash matches the documented contract source hash
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/DepositCommand.java hash matches the documented contract source hash
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/GiveCommand.java hash matches the documented contract source hash
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandDispatcher.java hash matches the documented contract source hash
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commandsystem/ItemList.java hash matches the documented contract source hash
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commandsystem/Arg.java hash matches the documented contract source hash
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commandsystem/ArgParser.java hash matches the documented contract source hash
contract fixture validation stops if any source hash drifts
```

Live test condition values:

```text
NONE
MUTATING_PREFLIGHT_REQUIRED
KOREAN_PHASE_REQUIRED
DANGEROUS_LIVE_APPROVAL_REQUIRED
SOURCE_REVIEW_REQUIRED
CLIENT_STATE_RESTORE_REQUIRED
COMMAND_REQUEST_FORBIDDEN
```

`live_test_allowed` remains boolean only. Conditions and explanatory text belong
to `live_test_condition` and `live_test_note`.

The current command status table is:

| Command | Owner | Natural-language status | Effect class | Live tier | Live allowed | Live condition | Verified | Verification state |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `get` | `chatclef_java` | `IMPLEMENTED` | `INVENTORY_MUTATING` | `5` | false | `MUTATING_PREFLIGHT_REQUIRED` | true | `VERIFIED` |
| `food` | `chatclef_java` | `IMPLEMENTED` | `INVENTORY_MUTATING` | `5` | false | `MUTATING_PREFLIGHT_REQUIRED` | true | `VERIFIED` |
| `meat` | `chatclef_java` | `IMPLEMENTED` | `INVENTORY_MUTATING` | `5` | false | `MUTATING_PREFLIGHT_REQUIRED` | true | `VERIFIED` |
| `goto` | `chatclef_java` | `IMPLEMENTED` | `MOVEMENT_MUTATING` | `5` | false | `MUTATING_PREFLIGHT_REQUIRED` | true | `VERIFIED` |
| `follow` | `chatclef_java` | `IMPLEMENTED` | `MOVEMENT_MUTATING` | `5` | false | `MUTATING_PREFLIGHT_REQUIRED` | true | `VERIFIED` |
| `idle` | `chatclef_java` | `IMPLEMENTED` | `TASK_MUTATING` | `5` | false | `MUTATING_PREFLIGHT_REQUIRED` | true | `VERIFIED` |
| `stop` | `chatclef_java` | `IMPLEMENTED` | `TASK_MUTATING` | `5` | false | `MUTATING_PREFLIGHT_REQUIRED` | true | `VERIFIED` |
| `equip` | `chatclef_java` | `PLANNED` | `INVENTORY_MUTATING` | `5` | false | `KOREAN_PHASE_REQUIRED` | false | `PLANNED` |
| `deposit` | `chatclef_java` | `PLANNED` | `INVENTORY_MUTATING` | `5` | false | `KOREAN_PHASE_REQUIRED` | false | `PLANNED` |
| `give` | `chatclef_java` | `PLANNED` | `INVENTORY_MUTATING` | `5` | false | `KOREAN_PHASE_REQUIRED` | false | `PLANNED` |
| `attack` | `chatclef_java` | `RAW_ONLY` | `DANGEROUS_MUTATING` | `5` | false | `DANGEROUS_LIVE_APPROVAL_REQUIRED` | false | `SOURCE_REVIEW_REQUIRED` |
| `scan` | `chatclef_java` | `RAW_ONLY` | `UNKNOWN_UNTIL_VERIFIED` | `null` | false | `SOURCE_REVIEW_REQUIRED` | false | `SOURCE_REVIEW_REQUIRED` |
| `hero` | `chatclef_java` | `RAW_ONLY` | `UNKNOWN_UNTIL_VERIFIED` | `null` | false | `SOURCE_REVIEW_REQUIRED` | false | `SOURCE_REVIEW_REQUIRED` |
| `locate_structure` | `chatclef_java` | `RAW_ONLY` | `MOVEMENT_MUTATING` | `5` | false | `SOURCE_REVIEW_REQUIRED` | false | `SOURCE_REVIEW_REQUIRED` |
| `gamma` | `chatclef_java` | `RAW_ONLY` | `CLIENT_STATE` | `4` | false | `CLIENT_STATE_RESTORE_REQUIRED` | false | `SOURCE_REVIEW_REQUIRED` |
| `reload_settings` | `chatclef_java` | `RAW_ONLY` | `UNKNOWN_UNTIL_VERIFIED` | `null` | false | `SOURCE_REVIEW_REQUIRED` | false | `SOURCE_REVIEW_REQUIRED` |
| `resetmemory` | `chatclef_java` | `RAW_ONLY` | `UNKNOWN_UNTIL_VERIFIED` | `null` | false | `SOURCE_REVIEW_REQUIRED` | false | `SOURCE_REVIEW_REQUIRED` |
| `gamer` | `chatclef_java` | `RAW_ONLY` | `UNKNOWN_UNTIL_VERIFIED` | `null` | false | `SOURCE_REVIEW_REQUIRED` | false | `SOURCE_REVIEW_REQUIRED` |
| `chatclef` | `chatclef_java` | `RAW_ONLY` | `UNKNOWN_UNTIL_VERIFIED` | `null` | false | `SOURCE_REVIEW_REQUIRED` | false | `SOURCE_REVIEW_REQUIRED` |
| `overlay` | `lavi_overlay` | `RAW_ONLY` | `CLIENT_STATE` | `4` | false | `CLIENT_STATE_RESTORE_REQUIRED` | false | `SOURCE_REVIEW_REQUIRED` |

Mutating commands remain `live_test_allowed: false` until the dedicated
backend, instance, world, and matching-terminal-result guards exist in the
actual live test implementation. Environment variables alone are not enough to
make a mutating command live-testable.

IMPLEMENTED rows must keep test evidence. Current evidence:

```json
{
  "get": {
    "golden_case_ids": [
      "nl-get-diamond-basic",
      "nl-get-diamond-mining",
      "nl-get-stone-quantity",
      "nl-get-coal-mining-quantity",
      "nl-get-iron-mining-quantity"
    ],
    "golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl",
      "tests/minecraft_chatclef/lavi_input/test_korean_input_to_chatclef_submission_path.py::test_lavi_korean_input_reaches_adapter_as_prefixless_command"
    ]
  },
  "food": {
    "golden_case_ids": [
      "nl-food-basic"
    ],
    "golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  },
  "meat": {
    "golden_case_ids": [
      "nl-meat-basic"
    ],
    "golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  },
  "goto": {
    "golden_case_ids": [
      "nl-goto-coordinate-basic"
    ],
    "golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  },
  "follow": {
    "golden_case_ids": [
      "nl-follow-player-basic"
    ],
    "golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  },
  "idle": {
    "golden_case_ids": [
      "nl-idle-basic"
    ],
    "golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  },
  "stop": {
    "golden_case_ids": [
      "nl-stop-basic"
    ],
    "golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  }
}
```

When the matrix moves into a JSON artifact, a contract test should verify that
each `golden_case_id` is present in the actual case table, not only that the
test function path exists.

Support matrix tests should verify:

```text
every registered command appears in the matrix exactly once
every matrix row points to a registered source or an explicit virtual command
no matrix row uses an "or" status
behavior_verified is boolean
live_test_allowed is boolean
live_test_condition is one of the closed enum values
verification_state is one of the closed enum values
UNKNOWN_UNTIL_VERIFIED rows have live_test_tier == null
UNKNOWN_UNTIL_VERIFIED rows have live_test_allowed == false
IMPLEMENTED commands have at least one golden case
IMPLEMENTED commands have at least one golden_case_id
PLANNED commands do not require success golden cases yet
RAW_ONLY commands are not invented by the natural-language compiler
EXPLICIT_UNSUPPORTED commands reject clearly and submit nothing
effect_class and live_test_tier fields are present for every command
live_test_tier may be null only when live_test_allowed is false
```

Do not add permanent failing tests or broad `xfail` tests for planned commands.
Use names such as:

```text
test_planned_equip_is_not_submitted_before_equip_phase
```

Avoid names such as:

```text
test_equip_is_unsupported
```

That wording would turn a temporary planning state into a permanent contract.

## Accepted No-Replay Lifecycle

Accepted commands must not be automatically submitted again by Python.

This is especially important for `get`: ChatClef `get <item> <count>` means
"request this additional amount for this command," not "ensure the final
inventory total equals count." Replaying the same accepted command can create
extra work or duplicate resource targets.

Required lifecycle cases:

```text
accepted + running repeated status -> submit count == 1
accepted + completed terminal -> submit count == 1, resubmit count == 0
accepted + failed terminal -> submit count == 1, resubmit count == 0
accepted + cancelled terminal -> submit count == 1, resubmit count == 0
accepted + deadline exceeded terminal -> submit count == 1, resubmit count == 0
accepted + unknown terminal -> submit count == 1, resubmit count == 0
accepted + disconnect -> submit count == 1, resubmit count == 0
accepted + duplicate terminal result -> submit count == 1, resubmit count == 0
accepted + duplicate terminal result -> terminal response handling count == 1
```

Recommended test names:

```text
test_accepted_command_is_not_replayed_after_completed
test_accepted_command_is_not_replayed_after_failed
test_accepted_command_is_not_replayed_after_cancelled
test_accepted_get_is_not_replayed_after_timeout
test_accepted_get_is_not_replayed_after_unknown_result
test_accepted_command_is_not_replayed_after_disconnect
test_duplicate_terminal_result_does_not_trigger_resubmit
test_duplicate_terminal_result_is_handled_once
```

These tests belong to lifecycle or router submission responsibility, not alias
coverage.

## Test File Organization

The repository currently has legacy flat Minecraft test files and a newer
responsibility-split folder for ChatClef tests.

Do not move existing flat tests as part of alias or command work unless a
separate test-reorganization task approves that migration.

New responsibility-split tests may live under:

```text
tests/minecraft_chatclef/
  alias_contract/
  catalog_coverage/
  command_catalog/
  input_gate/
  korean_translation/
  lavi_input/
  lifecycle/
  runtime/
```

Recommended responsibility mapping:

| Responsibility | Folder |
| --- | --- |
| prefixless Korean-to-DSL contract | `tests/minecraft_chatclef/korean_translation/` |
| router to adapter submission path | `tests/minecraft_chatclef/lavi_input/` |
| false-positive no-submit guard | `tests/minecraft_chatclef/input_gate/` |
| alias resolver priority and exact matching | `tests/minecraft_chatclef/alias_contract/` |
| runtime alias coverage snapshot | `tests/minecraft_chatclef/catalog_coverage/` |
| registered command support matrix | `tests/minecraft_chatclef/command_catalog/` |
| single active command lifecycle | `tests/minecraft_chatclef/lifecycle/` |
| opt-in live or Gradio runtime checks | `tests/minecraft_chatclef/runtime/` |

If a flat file is required for compatibility with existing discovery or CI, add
the real tests to the flat file or update test discovery to include the
responsibility-owned folder. Do not create a thin flat wrapper that imports and
re-exports test functions from the folder.

Avoid this pattern:

```python
from tests.minecraft_chatclef.alias_contract.test_aliases import *
```

That can cause duplicate collection, confusing fixture scope, duplicate test
IDs, and misleading coverage counts.

Use exactly one of these approaches for a given test responsibility:

```text
keep the real tests in the existing flat file
put the real tests in the responsibility folder and include that folder in discovery
update the CI or local test command to include the folder path explicitly
```

## Live Minecraft Test Tiers

Live tests must be opt-in because they can change task state, movement,
inventory, world state, client state, or target selection.

### Tier 1 - Offline

Default test tier. Does not require Minecraft, Java process, socket connection,
or a running LAVI UI.

Includes:

```text
gate/parser/resolver/compiler tests
alias/catalog contract tests
coverage snapshot tests
support matrix tests
router tests with fake adapter
backend separation tests
```

### Tier 2 - Local Loopback

Uses Python WebSocket server and a fake Java client, without a real Minecraft
client.

May verify:

```text
handshake
session ownership
single active command
stale result rejection
disconnect
prefixless command payload preservation
```

### Tier 3 - Real Bridge Without Command

Connects to a real Fabric client but does not send `command_request`.

May verify:

```text
bridge connected
session id exists
backend_id == fabric_chatclef
status lifecycle is observable
```

This tier must require an explicit environment opt-in. Use the repository's
current live-test environment names:

```text
LAVI_MINECRAFT_RUNTIME_TESTS=1
LAVI_GRADIO_URL=http://127.0.0.1:47860
```

Connection-only tests must not send a command request.

### Tier 4 - Client State Mutation

May change client state without intentionally changing the world. Examples:

```text
overlay
gamma
```

This tier needs a stronger opt-in than connection-only tests.

Future client-state tests should use a separate opt-in from connection-only
tests. If pytest markers are introduced later, require both environment opt-in
and explicit marker selection.

### Tier 5 - Character, World, Inventory, Or Task Mutation

Treat these as mutating:

```text
get
equip
deposit
goto
idle
stop
follow
give
attack
scan until proven read-only
```

Mutating tests must target only a dedicated Fabric test instance and test
world. They must never run in default CI, never run against a personal survival
world, and never auto-retry or replay a command after timeout or failure.

Do not live-test all aliases. Alias coverage belongs in offline tests. Live
tests should use representative smoke cases by command family.

The current unittest-gated mutating runtime test uses:

```text
LAVI_MINECRAFT_RUNTIME_TESTS=1
LAVI_MINECRAFT_RUNTIME_MUTATING=1
LAVI_MINECRAFT_RUNTIME_KOREAN_COMMAND=<optional command, default 돌 1개 가져와줘>
LAVI_MINECRAFT_RUNTIME_TIMEOUT_SEC=60
LAVI_MINECRAFT_RUNTIME_POLL_SEC=2
```

Mutating live tests must also gain a dedicated-environment preflight before any
future command-request implementation is expanded. Required policy variables:

```text
LAVI_MINECRAFT_EXPECTED_BACKEND=fabric_chatclef
LAVI_MINECRAFT_EXPECTED_INSTANCE=LAVI_TEST_Fabric01
LAVI_MINECRAFT_EXPECTED_WORLD=<dedicated-test-world>
```

Before sending `command_request`, the test must compare these expected values
with the runtime status. Mismatch behavior:

```text
backend mismatch -> fail before command_request
instance mismatch -> fail before command_request
world mismatch -> fail before command_request
```

If runtime status cannot report instance or world identity yet, the mutating
test must skip or fail before command submission. Do not silently continue.
Forge MineMind fallback remains forbidden.

Future pytest-based live tests should require both environment opt-in and marker
selection, for example:

```text
env:
  LAVI_MINECRAFT_RUNTIME_TESTS=1
  LAVI_MINECRAFT_RUNTIME_MUTATING=1

markers:
  chatclef_live
  chatclef_client_state
  chatclef_mutating
```

Do not let a single accidental environment variable run mutating tests.

## Live Test Safety Rules

Live test execution requires explicit user approval and opt-in environment
variables. A safe live test design should identify:

```text
expected backend
dedicated test instance
dedicated test world
whether command_request is sent
whether movement can occur
whether inventory can change
whether world blocks can change
whether client state can change
terminal condition checked: ACCEPTED only or COMPLETED
```

Split live tests by terminal expectation:

```text
connection_only_live
  -> no command_request

client_state_submit_or_terminal
  -> allowed only for reversible client-state commands with explicit restoration

mutating_terminal_e2e
  -> waits for a terminal result and requires deterministic setup
```

Do not apply ACCEPTED-only smoke tests to mutating commands such as `get`,
`goto`, `follow`, `attack`, `equip`, `deposit`, or `give` unless one of these
conditions is true:

```text
the test owns and tears down a disposable Minecraft process and world
an explicitly approved cleanup or stop procedure runs before test exit
the test waits for a terminal command_result before ending
the command is reclassified as non-mutating by source evidence
```

The current mutating Korean runtime test follows the safer terminal pattern by
waiting for a terminal result rather than stopping immediately at `accepted`.
Future terminal success checks must be stricter than `active_request_id is
None`, because an active request can disappear after disconnect, session
replacement, server stop, or ownership reset.

Terminal success requires:

```text
matching terminal CommandResultDTO observed
result request ownership matches the submitted command
terminal status and result_reason are recorded
active request is cleared after the matching terminal result
```

Minimum matching fields:

```text
request_id
correlation_id when available
session_id
connection_generation
terminal status
result_reason
```

Do not pass a terminal live test solely because `active_request_id` became
empty.

Do not:

```text
start Minecraft automatically in CI
run against a personal world
execute all 591 catalog targets live
run deposit or give without prepared inventory and recipient preconditions
retry command_request automatically
replay the original command after failure
fallback to Forge MineMind
```

## Immediate Documentation-Level Acceptance

Before the next implementation phase, the documented test plan should make the
following true:

```text
current working behavior is represented by green tests
future command behavior is represented by the support matrix, not failing tests
support matrix rows use one natural-language status, one effect class, and one live tier field
live_test_allowed is boolean and live_test_condition carries condition text
behavior_verified is boolean and verification_state carries non-boolean context
unknown-effect commands have null live tier and live tests disabled
registered command authority is source-backed and owner-scoped
registered command snapshot and support matrix artifact paths are defined
registered command artifacts are planned and do not make Phase 0 complete until committed and tested
current aliases are hard-validated
raw catalog coverage breadth is visible but not treated as complete
runtime coverage and proposed v2 draft coverage are separate
coverage snapshot and coverage floor are separate
coverage floor uses integer covered-target count as the authority
natural-language commands stay prefixless through the adapter boundary
raw command tests may still use @ when they are not natural-language tests
Tier 1 offline tests do not call real LLMs or networks
live tests are opt-in and side-effect-classified
mutating live tests do not end at ACCEPTED only without teardown or terminal observation
mutating live tests require backend, instance, and world preflight before command_request
terminal live success requires a matching terminal result, not only active_request_id clear
accepted commands are never automatically replayed
Fabric ChatClef tests do not import Forge MineMind fixtures
```

## ChatGPT Handoff Summary

Use this when asking ChatGPT to continue reviewing the test plan:

```text
Codex documented the Korean ChatClef test strategy in:

plugins/Minecraft/docs/chatclef-korean-test-strategy.md

The strategy separates:

1. current correctness tests
2. alias coverage snapshot tests
3. command support/planned matrix tests

Python Korean natural-language output and router-to-adapter requests must be
prefixless, for example `get diamond 1`. Raw command entrypoints and transport
fixtures may still use `@get diamond 1` when they are not testing the
natural-language compiler.

The committed-baseline GET golden cases are:

- 철괴 가져와줘 -> get iron_ingot 1
- 철 주괴 10개 가져와줘 -> get iron_ingot 10
- 금괴 가져와줘 -> get gold_ingot 1
- 구운 소고기 가져와줘 -> get cooked_beef 1
- 원목 가져와줘 -> get log 1

Offline Korean-to-DSL tests assert VALIDATED, executable, exact prefixless DSL,
and no @ prefix. Router tests are separate: VALIDATED + connected + idle must
submit exactly once, no-submit cases must submit zero times, rejected adapter
results submit once and do not retry, and all paths submit at most once.

False-positive phrases such as 캐나다 여행 얘기하자, 캐릭터 만들어줘,
캐시 확인해줘, 오늘 다이아몬드가 예쁘다, 돌 10개가 창고에 있어, and
마인크래프트에서 다이아몬드 캐는 법 알려줘 must submit nothing.

Alias tests hard-validate aliases currently present, while coverage snapshot
tests record raw catalog breadth without requiring all 591 raw catalog targets
to have Korean aliases. Runtime coverage and proposed v2 draft coverage are
separate. Current runtime coverage comes from
plugins/Minecraft/fabric/chatclef/intent/resources/korean_item_aliases.json:
11 aliases, 4 unique catalog targets, 587 raw missing targets, and 0.68%
raw coverage. The 191 aliases / 167 targets / 424 missing / 28.26% values
belong to the proposed v2 draft seed until that draft is generated into the
runtime resource. user_facing_* fields stay null until the catalog classifier
exists. Coverage snapshot and coverage floor are separate files so a floor
reduction cannot be hidden inside a snapshot update. The authoritative floor is
the integer covered-target count; percent is a derived display value.

The support matrix now separates command owner, natural-language status,
effect_class, live_test_tier, live_test_allowed, live_test_condition,
live_test_note, behavior_verified, verification_state, verification_note,
golden_case_ids, and golden_tests. Natural language status values are
IMPLEMENTED, PLANNED, RAW_ONLY, and EXPLICIT_UNSUPPORTED. LIVE_ONLY was removed
from natural-language status and belongs to live test tier/effect
classification instead. behavior_verified and live_test_allowed are boolean
only; non-boolean context belongs to verification_state, verification_note,
live_test_condition, and live_test_note.

Current PLANNED Korean item-action commands are equip, deposit, and give.
Commands such as attack, scan, gamma, overlay, hero, locate_structure,
reload_settings, resetmemory, gamer, and chatclef are RAW_ONLY until a separate
Korean UX phase is approved. scan remains UNKNOWN_UNTIL_VERIFIED for effect
classification until source behavior proves whether it is read-only. Any
UNKNOWN_UNTIL_VERIFIED command keeps live_test_tier null,
live_test_allowed false, and live_test_condition SOURCE_REVIEW_REQUIRED until
source review assigns a concrete effect class and live tier.

Registered command authority is defined as future source-backed artifacts:

- tests/minecraft_chatclef/command_catalog/chatclef_registered_commands.snapshot.json
- tests/minecraft_chatclef/command_catalog/chatclef_command_support_matrix.json

These artifact paths and schemas are planned. Phase 0 is not complete until the
JSON artifacts are committed and contract tests prove that they contain every
registered command, actual source hashes, exact owners, boolean
live_test_allowed values, and command-specific golden_case_ids.

Current source hashes recorded in the document:

- AltoClefCommands.java sha256 dc03102ecb1659f1ed0d0fc55852864c887541f6d5287f5ad39fd7aed563d5f7
- OverlayCommand.java sha256 ee3a5dcda914fb2c82a5776efe2963d4efa72cd0226b172ec285110fdba23e0f

IMPLEMENTED commands must keep golden test references. Current implemented
commands are get, food, meat, goto, follow, idle, and stop. Matrix evidence
must include command-specific golden_case_ids so a shared parameterized test
function path alone cannot hide a removed case.

GET acquisition/mining matcher behavior is not committed-baseline verified
unless Phase 0 proves the implementation source and focused tests:

- 다이아몬드 캐줘 -> get diamond 1
- 석탄 5개 캐와줘 -> get coal 5
- 철 10개 캐줘 -> get iron_ingot 10

Future Phase 3 action matching means either proving or implementing GET
mining/acquisition recognition separately from GET/EQUIP/DEPOSIT/GIVE
cross-command precedence.

Live Minecraft tests are separated into offline, loopback, real-bridge
connection, client-state mutation, and character/world/inventory/task mutation
tiers. Current live env gates are LAVI_MINECRAFT_RUNTIME_TESTS=1 and
LAVI_MINECRAFT_RUNTIME_MUTATING=1. Future pytest live tests should require both
env opt-in and explicit marker selection. Mutating tests require explicit
opt-in and a dedicated Fabric test instance/world. Do not live-test every alias.
Do not run ACCEPTED-only smoke tests for mutating commands unless the test owns
teardown, cleanup, terminal observation, or source evidence reclassifies the
command as non-mutating.

Future mutating live tests must preflight:

- LAVI_MINECRAFT_EXPECTED_BACKEND=fabric_chatclef
- LAVI_MINECRAFT_EXPECTED_INSTANCE=LAVI_TEST_Fabric01
- LAVI_MINECRAFT_EXPECTED_WORLD=<dedicated-test-world>

Mismatch must fail before command_request. Terminal success must observe a
matching terminal CommandResultDTO by request_id, correlation_id when available,
session_id, connection_generation, terminal status, and result_reason before
checking that the active request cleared.

Accepted commands must not be replayed automatically after running, completed,
failed, cancelled, timeout, unknown terminal, disconnect, or duplicate terminal
result. Tier 1 offline tests must use direct rule tests or deterministic
fakes/stubs and must not call real LLMs, OpenAI APIs, networks, Minecraft,
Java, or LAVI UI.

No Java, DTO, payload, ChatClef engine, Forge MineMind, or live Minecraft
behavior was changed by this documentation step.
```
