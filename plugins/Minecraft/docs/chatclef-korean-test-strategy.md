<!-- 20260815_kpopmodder: Documented the Korean ChatClef test strategy before expanding alias and action support. -->
<!-- 20260815_chatgpt: Reconciled source provenance, activation chains, coverage parser authority, matrix maturity, and documentation-set acceptance. -->
<!-- 20260818_kpopmodder: Reconciled the post-restore live lifecycle test, existing request correlation, and missing fail-closed preflight. -->
<!-- 20260818_kpopmodder: Fixed audited-baseline live semantics, no-replay handling, and terminal/runtime/gameplay success separation. -->
<!-- 20260818_kpopmodder: Aligned gameplay observation completeness and expected/partial/prohibited effect semantics with the final-reviewed preflight schema. -->
<!-- 20260819_kpopmodder: Added canonical submission, reconciliation latch, and split terminal/checkpoint/advancement regression contracts. -->
<!-- 20260829_openai: Versioned the registered-command count and kept current pass/fail ownership in the merge-blocker document. -->
<!-- 20260829_openai: Added the current deposit_all exact-set gate and topology-correct STORE_HOME decision-order and identity test contracts. -->
<!-- 20260905_kpopmodder: Reconciled the implemented H5 provenance route, 26-row catalog, Java test-only proof, and offline verification. -->
<!-- 20260829_openai: Recorded the final focused registry synchronization result without promoting runtime evidence. -->
<!-- 20260904_kpopmodder: Added the docs-only H5 Korean Chat/microphone test contract and recorded the activation-aware 26-command catalog gap. -->
<!-- 20260905_kpopmodder: Reconciled H5 receipt-commit race tests and the two distinct Java test-only obligations. -->

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
plugins/Minecraft/docs/chatclef-python-korean-command-registry-plan.md
plugins/Minecraft/docs/chatclef-korean-item-action-alias-v2-plan.md
plugins/Minecraft/docs/chatclef-python-command-orchestration-plan.md
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
plugins/Minecraft/docs/chatclef-korean-post-review-merge-blockers.md
plugins/Minecraft/docs/chatclef-h5-auto-deposit-trust-korean-chat-microphone-pre-change-contract-2026-09-04.md
plugins/Minecraft/README.md
plugins/Minecraft/docs/chatclef-command-lifecycle-and-threading.md
plugins/Minecraft/docs/fabric-chatclef-bridge-protocol-v1.md
plugins/Minecraft/docs/fabric-chatclef-live-runtime-preflight-plan.md
plugins/Minecraft/docs/fabric-chatclef-live-runtime-process-lifecycle-plan.md
```

The Python Korean command registry plan owns the source-backed Java
registered-command snapshot: 20 commands at its reviewed baseline, 22 at the
dated 2026-08-29 closure, and an activation-aware target of 26 after the
separate H5 registrar. Production Python metadata and artifacts are now
synchronized at 26. It also owns command-by-command resolver
domains, lifecycle kinds, safety tiers,
confirmation modes, allowed input sources, and public enablement axes. The alias
v2 plan owns item/action design, command-specific Korean UX policy, canonical
aliases, display wording, capability gates, and phased item/action
implementation order. The Python command orchestration plan owns user-facing
response evidence, lifecycle wording, single-pass submission, and primary
command sequencing. The inventory cleanup contract owns inventory evidence,
protected-item policy, targeted cleanup, fresh post-cleanup verification, and
fail-closed primary admission. This document owns tests, artifact schemas,
source and hash authority, coverage calculation authority, CI scope, and live
Minecraft safety gates. The post-review merge-blocker document owns current
implementation/CI status and the merge decision. The historical analysis is
diagnosis only, and the README is navigation only.

The 2026-08-19 documentation migration touched seven directly affected files as
one documentation-only consistency update. Later changes do not have to modify
all seven files; they must update the owning normative documents whenever a
shared contract changes.

For test evidence, runtime coverage terminology, reviewed GET acquisition
behavior, Java activation and source snapshot authority, and focused CI
acceptance, this document supersedes older or conflicting wording in the alias
v2 plan. For current pass/fail and merge status, the post-review merge-blocker
document is authoritative.

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

The goal is quick failure localization. If a supported phrase such as
`철 10개 캐줘` fails, the test suite should show whether the failure came from
gate selection, parser phrase extraction, alias resolution, catalog validation,
compiler serialization, or router submission.

## Reviewed Implementation Golden Cases At cfc170a

These cases represent reviewed Korean GET behavior at implementation test
commit `cfc170ac024a46ea943bffcaf289a91ff6bc5ee7`. Do not call this "current
HEAD" after this documentation is committed, because HEAD will move.

Do not mix this reviewed implementation baseline with the older historical
baseline from `c912ff6491711c33dbf2e66320e634196f866ae8`.

| Korean input | Expected DSL |
| --- | --- |
| `철괴 가져와줘` | `get iron_ingot 1` |
| `철 주괴 10개 가져와줘` | `get iron_ingot 10` |
| `금괴 가져와줘` | `get gold_ingot 1` |
| `구운 소고기 가져와줘` | `get cooked_beef 1` |
| `원목 가져와줘` | `get log 1` |
| `다이아몬드 가져와줘` | `get diamond 1` |
| `다이아몬드 캐줘` | `get diamond 1` |
| `돌 10개 가져와줘` | `get stone 10` |
| `조약돌 10개 캐줘` | `get cobblestone 10` |
| `석탄 5개 캐와줘` | `get coal 5` |
| `철 10개 캐줘` | `get iron_ingot 10` |

Current source evidence tests:

```text
tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py
tests/minecraft_chatclef/lavi_input/test_korean_input_to_chatclef_submission_path.py
tests/minecraft_chatclef/alias_contract/test_get_alias_translation_priority.py
tests/test_minecraft_chatclef_korean_command_integration.py
tests/test_minecraft_chatclef_korean_rule_parser.py
tests/test_minecraft_chatclef_item_phrase_resolver.py
```

Alias-wide evidence also lives in:

```text
tests/minecraft_chatclef/alias_contract/test_all_runtime_aliases_translate.py
```

Keep the assertions separated by responsibility. The `golden_case_ids` listed
later in this document are planned contract IDs until a repository-owned case
table with those IDs is committed.

Planned shared case artifact shape:

```python
SUPPORTED_KOREAN_TRANSLATION_CASES = [
    {
        "id": "nl-get-diamond-basic",
        "command": "get",
        "text": "다이아몬드 가져와줘",
        "expected": "get diamond 1",
    },
]
```

The same source must be used by:

```text
translation tests
support matrix contract tests
documentation snapshot generation
golden_case_id validation
```

Planned golden IDs for reviewed implementation cases:

| ID | Korean input | Expected DSL |
| --- | --- | --- |
| `nl-get-iron-ingot-basic` | `철괴 가져와줘` | `get iron_ingot 1` |
| `nl-get-iron-ingot-spaced-quantity` | `철 주괴 10개 가져와줘` | `get iron_ingot 10` |
| `nl-get-gold-ingot-basic` | `금괴 가져와줘` | `get gold_ingot 1` |
| `nl-get-cooked-beef-basic` | `구운 소고기 가져와줘` | `get cooked_beef 1` |
| `nl-get-log-basic` | `원목 가져와줘` | `get log 1` |
| `nl-get-diamond-basic` | `다이아몬드 가져와줘` | `get diamond 1` |
| `nl-get-diamond-mining` | `다이아몬드 캐줘` | `get diamond 1` |
| `nl-get-stone-quantity` | `돌 10개 가져와줘` | `get stone 10` |
| `nl-get-cobblestone-mining-quantity` | `조약돌 10개 캐줘` | `get cobblestone 10` |
| `nl-get-coal-mining-quantity` | `석탄 5개 캐와줘` | `get coal 5` |
| `nl-get-iron-mining-quantity` | `철 10개 캐줘` | `get iron_ingot 10` |

Until this artifact exists, tests must not claim that `golden_case_ids` are
already present in the repository's test data.

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

## Translation Result Invariants

Router submission tests must include malformed translation results, not only
normal `VALIDATED` and false-positive paths.

Required DTO and submission invariants:

```text
type(executable) is bool
executable == status.executable
```

When `executable` is `true`:

```text
status == VALIDATED
command is str
command.strip() is non-empty
command is prefixless
command contains no CR or LF
command contains no @
command contains no # comment delimiter
command contains no semicolon
command contains no quote
command contains no control character
intent is present
compiler can reproduce the same command from the validated structured intent
  and canonical resolution output when that intent requires item/action
  resolution
```

Any invariant failure must produce:

```text
adapter submit count == 0
retry count == 0
no LLM fall-through after a malformed validated command
```

Required malformed regression vectors:

| Translation result shape | Expected result |
| --- | --- |
| `executable="false"` | reject, submit `0` |
| `status=VALIDATED, executable=True, command=None` | reject, submit `0` |
| `status=VALIDATED, executable=True, command=""` | reject, submit `0` |
| `status=VALIDATED, executable=True, command="   "` | reject, submit `0` |
| `status=VALIDATED, executable=True, command="@get diamond 1"` | reject, submit `0` |
| `status=VALIDATED, executable=True, command="get diamond 1\n@stop"` | reject, submit `0` |
| `status=VALIDATED, executable=True, command="get diamond 1; stop"` | reject, submit `0` |
| `status=VALIDATED, executable=True, command="get diamond 1 # stop"` | reject, submit `0` |
| `status=VALIDATED, executable=True, command="get \"diamond\" 1"` | reject, submit `0` |
| `status=VALIDATED, executable=True, command="get diamond 1\u0000"` | reject, submit `0` |
| `status=VALIDATED, executable=False` | reject, submit `0` |
| `status=UNKNOWN, executable=True` | reject, submit `0` |
| `status=FALSE_POSITIVE, executable=True` | reject, submit `0` |
| `status=INVALID, executable=True` | reject, submit `0` |

Reproduction invariant by command family:

```text
GET/EQUIP/DEPOSIT/GIVE:
  recompiled_command =
    compiler.compile(validated_intent, canonical_resolution_if_applicable)

GOTO/FOLLOW/IDLE/STOP/FOOD/MEAT:
  recompiled_command =
    compiler.compile(validated_intent)

all executable paths:
  recompiled_command == supplied_command
```

Do not rely on `bool(value)` coercion for `executable`. A string such as
`"false"` is truthy in Python and must not become an executable command.

## Current GET Acquisition Matching And Future Action Precedence

Reviewed implementation source and tests include GET acquisition and mining
phrases. Treat these as reviewed regression cases for the branch under review,
not as unverified future behavior:

```text
다이아몬드 캐줘 -> get diamond 1
석탄 5개 캐와줘 -> get coal 5
철 10개 캐줘 -> get iron_ingot 10
```

If a document needs to describe the older `c912ff...` historical baseline, name
that section `Historical Baseline At c912ff...` and keep it separate from
reviewed implementation behavior.

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

## Current e08af Acquisition Regression

The reviewed archive baseline for the current Korean orchestration follow-up is:

```text
e08af63948a3fa4675c70279db59c2a70b00a332
```

The following are existing GET acquisition regressions, not future-only
features:

```text
철 10개 캐줘 -> get iron_ingot 10
철 10개 캐오기 -> get iron_ingot 10
철 10개 캐와줘 -> get iron_ingot 10
철을 10개 채굴해줘 -> get iron_ingot 10
```

Compiler output remains prefixless and must not start with `@`.

## Canonical Korean Display Contract

Response tests should prove that input aliases and canonical display names are
separate:

```text
철갑바 response display -> 철 흉갑
철바지 response display -> 철 레깅스
철신발 response display -> 철 부츠
철모자 response display -> 철 투구
철헬멧 response display -> 철 투구
```

Display names are Python-owned presentation data. They must not be added to
common DTOs or wire payloads.

## Equipment Colloquial Alias Contract

Required representative equipment alias regressions:

```text
철갑바 만들어줘 -> get iron_chestplate 1
철 흉갑 만들어줘 -> get iron_chestplate 1
철바지 만들어줘 -> get iron_leggings 1
철 레깅스 만들어줘 -> get iron_leggings 1
철신발 만들어줘 -> get iron_boots 1
철 부츠 만들어줘 -> get iron_boots 1
철모자 만들어줘 -> get iron_helmet 1
철헬멧 만들어줘 -> get iron_helmet 1
철 투구 만들어줘 -> get iron_helmet 1
다이아몬드 곡괭이 만들어줘 -> get diamond_pickaxe 1
```

Negative composition regressions:

```text
철학 모자 must not resolve to iron_helmet
금요일 바지 must not resolve to golden_leggings
```

Craft wording remains GET acquisition:

```text
횃불 만들어줘 -> get torch 1
```

There is no separate craft DSL.

## Response Evidence And Wording Contract

Response truthfulness tests must separate lifecycle evidence:

```text
validated alone does not say submitted
accepted does not say started
running does not say completed
terminal completed does not say gameplay effect verified
gameplay effect wording requires separate observer evidence
unknown does not advance orchestration
busy/disconnected/rejected responses submit 0
UI receives the response exactly once
Translate listener receives the response exactly once when dispatch is enabled
TTS listener receives the response exactly once when dispatch is enabled
LLM provider call count is 0
llm.receive_input() recursion count is 0
ordinary chat history storage is 0 by default
```

## Inventory Cleanup Postcondition Contract

Detailed cleanup safety tests are owned by:

```text
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
```

The test strategy must include the responsibility folder when implementation is
approved:

```text
tests/minecraft_chatclef/inventory_preflight/
```

The central postcondition is:

```text
cleanup terminal completed alone -> primary 0
fresh post-cleanup AVAILABLE verified + protected preservation verified -> primary 1
protected_item_preservation_verified false or unknown -> primary 0
post-cleanup UNKNOWN -> primary 0
post-cleanup FULL -> primary 0
```

Automatic cleanup must create zero bare `deposit` commands and zero placeholder
quantities.

## Catalog-Driven Coverage Artifacts

Catalog coverage artifacts must prove at least:

```text
emerald, torch, and iron_leggings exist in the catalog
every runtime alias target exists in the catalog
generated aliases and curated aliases merge deterministically
compact alias conflict count is 0
catalog-outside target count is 0
coverage floor does not decrease
raw coverage and requestable concrete coverage are separate
ambiguous/default-policy targets are explicitly classified
alias resolution success and command capability success are separate
equip rejects non-equipment targets as explicit unsupported
craft wording compiles to GET
craft DSL is never generated
```

## Multi-Item GET Contracts

Current Java `ItemList` grammar and planned Python canonical serialization are
separate test responsibilities.

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
  -> Java parser rejects and Python compiler must reject before adapter submission

get [[stone 1]]
  -> rejected as invalid nested bracket structure
```

Do not assert Java `HashMap` iteration order as a stable command contract.

Planned Python multi-item GET canonical output policy:

```text
use bracketed ItemList syntax
separate entries with comma-space
emit every count explicitly
merge duplicate canonical targets by summing counts
keep the first occurrence position for a merged target
emit prefixless DSL only
```

Current and planned single-action count output policy:

```text
current GET emits explicit count, for example get diamond 1
planned GIVE emits explicit count, for example give Steve diamond 1
planned specific DEPOSIT emits explicit count only after Korean quantity policy resolves it
source-backed Java zero-argument DEPOSIT serializes as deposit, but Korean
  exposure and automatic cleanup use remain disabled unless separately approved
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
strict integer means type(value) is int, not isinstance(value, int)
zero count rejects
negative count rejects
non-integer count rejects
bool count rejects before int conversion
float count rejects before int conversion
None count rejects
single count overflow rejects before adapter submission
duplicate multi-item GET sum must be computed before emission
duplicate-target sum overflow rejects before adapter submission
duplicate multi-item GET sum must remain in the Java-compatible positive range
new ItemActionResolution or multi-item paths must not bypass this validation
```

Required quantity regression vectors:

| Raw value | Expected result |
| --- | --- |
| `1` | accept |
| `2147483647` | accept |
| `0` | reject, adapter submit count `0` |
| `-1` | reject, adapter submit count `0` |
| `2147483648` | reject, adapter submit count `0` |
| `True` | reject, adapter submit count `0` |
| `False` | reject, adapter submit count `0` |
| `1.0` | reject, adapter submit count `0` |
| `1.5` | reject, adapter submit count `0` |
| `"1.5"` | reject, adapter submit count `0` |
| `None` | reject, adapter submit count `0` |

String quantity `"1"` requires an explicit input-normalization policy before it
can be accepted. Do not allow it accidentally through `int(value)` coercion.

## Numeric Slot Validation Contracts

The strict integer contract applies to every numeric intent slot, not only
`quantity`.

Slot ranges:

| Slot | Accepted range |
| --- | --- |
| `quantity` | exact int `1..2147483647` |
| `food_units` | exact int `1..2147483647` |
| `x` | exact int `-2147483648..2147483647` |
| `y` | exact int `-2147483648..2147483647` |
| `z` | exact int `-2147483648..2147483647` |

All numeric slots must reject:

```text
bool
float
None unless the slot is explicitly optional and absent
overflow outside the slot range
numeric strings unless an explicit input-normalization policy accepts them
```

Raw mapping type validation must happen before DTO numeric coercion:

```text
raw mapping -> type/range validation -> DTO construction -> compiler
```

DTO code must not silently normalize invalid raw types with `int(value)`.
Once `1.5` has been converted to `1`, the compiler can no longer recover the
original invalid type.

Required non-quantity regression vectors:

| Raw slot/value | Expected result |
| --- | --- |
| `food_units=True` | reject, adapter submit count `0` |
| `food_units=1.5` | reject, adapter submit count `0` |
| `food_units=2147483648` | reject, adapter submit count `0` |
| `x=True` | reject, adapter submit count `0` |
| `y=1.5` | reject, adapter submit count `0` |
| `z="3"` | explicit numeric-string policy required |
| `x=-2147483649` | reject, adapter submit count `0` |
| `z=2147483648` | reject, adapter submit count `0` |

The same exact-type rule applies to `executable`: raw mapping validation must
reject non-bool executable values before `bool(value)` can coerce them.

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

Historical runtime baseline fields describe the older committed baseline only:

```json
{
  "schema_version": 1,
  "snapshot_kind": "runtime_baseline",
  "snapshot_authority": "git_blob_bytes",
  "snapshot_baseline_commit": "c912ff6491711c33dbf2e66320e634196f866ae8",
  "coverage_algorithm_version": 1,
  "catalog_parser_source_commit": "cfc170ac024a46ea943bffcaf289a91ff6bc5ee7",
  "catalog_parser_source_path": "plugins/Minecraft/fabric/chatclef/intent/chatclef_target_catalog.py",
  "catalog_parser_source_sha256": "bca08b987851fb9a27f0dcb06cd95d05c264fd0d276540d0bbd5cfd9d1e50893",
  "catalog_path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/CataloguedResources.txt",
  "catalog_sha256": "4591ae83151dd2643943dfdcbb2e89a53d26de393c583902370c3f4d3c57188a",
  "alias_source_path": "plugins/Minecraft/fabric/chatclef/intent/resources/korean_item_aliases.json",
  "alias_source_sha256": "fd27685fa50d3364add9d9df5a0675c52c5b13ff862c07ef429d5e67055b45fd",
  "raw_catalog_total": 591,
  "raw_catalog_covered_targets": 4,
  "raw_catalog_missing_targets": 587,
  "raw_catalog_coverage_percent": 0.68,
  "direct_alias_count": 11,
  "canonical_user_facing_target_total": null,
  "resolved_canonical_user_facing_targets": null,
  "unresolved_canonical_user_facing_targets": null,
  "alias_resolution_coverage_percent": null,
  "get_actionable_target_total": null,
  "resolved_get_actionable_targets": null,
  "unresolved_get_actionable_targets": null,
  "get_actionable_coverage_percent": null,
  "catalog_outside_targets": 0,
  "compact_conflicts": 0
}
```

Reviewed implementation runtime snapshot fields must describe the runtime file
at reviewed commit `cfc170ac024a46ea943bffcaf289a91ff6bc5ee7`, not the older
`c912ff...` baseline and not a future HEAD:

```json
{
  "schema_version": 1,
  "snapshot_kind": "runtime_reviewed_baseline",
  "snapshot_authority": "git_blob_bytes",
  "reviewed_source_commit": "cfc170ac024a46ea943bffcaf289a91ff6bc5ee7",
  "coverage_algorithm_version": 1,
  "catalog_parser_source_commit": "cfc170ac024a46ea943bffcaf289a91ff6bc5ee7",
  "catalog_parser_source_path": "plugins/Minecraft/fabric/chatclef/intent/chatclef_target_catalog.py",
  "catalog_parser_source_sha256": "bca08b987851fb9a27f0dcb06cd95d05c264fd0d276540d0bbd5cfd9d1e50893",
  "catalog_path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/CataloguedResources.txt",
  "catalog_sha256": "4591ae83151dd2643943dfdcbb2e89a53d26de393c583902370c3f4d3c57188a",
  "alias_source_path": "plugins/Minecraft/fabric/chatclef/intent/resources/korean_item_aliases.json",
  "alias_source_sha256": "9dcf8b8fde97ab60abeceaee7ce0938eda030e80689f1d58d04d5e797b5896df",
  "raw_catalog_total": 591,
  "raw_catalog_covered_targets": 9,
  "raw_catalog_missing_targets": 582,
  "raw_catalog_coverage_percent": 1.52,
  "direct_alias_count": 19,
  "canonical_user_facing_target_total": null,
  "resolved_canonical_user_facing_targets": null,
  "unresolved_canonical_user_facing_targets": null,
  "alias_resolution_coverage_percent": null,
  "get_actionable_target_total": null,
  "resolved_get_actionable_targets": null,
  "unresolved_get_actionable_targets": null,
  "get_actionable_coverage_percent": null,
  "catalog_outside_targets": 0,
  "compact_conflicts": 0
}
```

Do not call both snapshots `runtime_current`. Tests must compare the historical
baseline artifact to `snapshot_baseline_commit`, and compare the reviewed
implementation artifact to `reviewed_source_commit`.

If a "current runtime" check is needed, it must be computed dynamically from
`HEAD`, not copied from the reviewed baseline:

```text
runtime_current == recomputed from git blob bytes at HEAD
runtime_reviewed_baseline == fixed reviewed commit cfc170a
```

Current-drift tests should verify:

```text
sha256(git_blob(reviewed_source_commit, path)) == expected_sha256
sha256(git_blob(HEAD, path)) == expected_sha256
```

Only an approved source change may update both `reviewed_source_commit` and the
expected SHA-256 values.

The proposed v2 draft snapshot should be a separate artifact:

```json
{
  "schema_version": 1,
  "snapshot_kind": "proposed_v2_draft",
  "coverage_algorithm_version": 1,
  "catalog_parser_source_commit": "cfc170ac024a46ea943bffcaf289a91ff6bc5ee7",
  "catalog_parser_source_path": "plugins/Minecraft/fabric/chatclef/intent/chatclef_target_catalog.py",
  "catalog_parser_source_sha256": "bca08b987851fb9a27f0dcb06cd95d05c264fd0d276540d0bbd5cfd9d1e50893",
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
catalog parser SHA-256
ko_kr.json SHA-256
generator schema version
curated policy SHA-256
default policy SHA-256
canonicalization policy SHA-256
item-command target policy SHA-256
```

Catalog parser and coverage algorithm authority:

```json
{
  "coverage_algorithm_version": 1,
  "catalog_parser_source_commit": "cfc170ac024a46ea943bffcaf289a91ff6bc5ee7",
  "catalog_parser_source_path": "plugins/Minecraft/fabric/chatclef/intent/chatclef_target_catalog.py",
  "catalog_parser_source_sha256": "bca08b987851fb9a27f0dcb06cd95d05c264fd0d276540d0bbd5cfd9d1e50893"
}
```

The historical snapshot uses catalog and alias blobs from `c912ff...`, but its
coverage counts are interpreted and must be reproducibly recomputed with
coverage algorithm version `1` and the reviewed parser contract above. This
does not claim that the same parser implementation existed at `c912ff...`.
The reviewed baseline and dynamic HEAD artifacts use the same algorithm until
an explicitly reviewed algorithm-version change is committed.

The catalog file is not a pure one-target-per-line list. The reviewed
`CataloguedResources.txt` contains a leading description line and a blank line
before the actual targets. Do not count non-empty lines directly.

Minimum line-format contract:

```text
trim each line
skip blank lines
skip leading non-target lines until the first valid target
after the first valid target, reject every non-target line
target regex is ^[a-z0-9_]+$
reject duplicate targets
expected reviewed target count is 591
```

Production baseline-target invariant:

```text
all required baseline targets must exist
missing any required baseline target -> catalog load failure

required baseline targets:
  cooked_beef
  diamond_axe
  diamond_pickaxe
  gold_ingot
  golden_axe
  iron_ingot
  iron_shovel
  log
  netherite_sword
  wooden_axe
```

Required catalog parser regressions:

```text
descriptive first line does not count as a target
invalid line after the first valid target fails
duplicate target fails
missing any required baseline target fails
recomputed target count == 591
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

Required artifact names:

```text
korean_item_aliases.runtime_baseline.snapshot.json
korean_item_aliases.runtime_reviewed_baseline.snapshot.json
korean_item_aliases.runtime_current.generated.json
korean_item_aliases.v2_draft.snapshot.json
korean_item_aliases.runtime.floor.json
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

Keep non-regression floors separate from snapshots. Suggested floor shape:

```json
{
  "floor_kind": "runtime_coverage_floor",
  "applies_to_snapshot_kind": "runtime_reviewed_baseline",
  "minimum_raw_covered_targets": 9
}
```

The authoritative non-regression value is the integer covered-target count.
Percentages are derived display values only because rounding can create
unnecessary failures.

Lowering `minimum_raw_covered_targets` is a separate review decision. Do not
hide a coverage regression by updating the snapshot and the floor together.

The raw target floor does not protect individual Korean synonyms. User-critical
phrases such as `철괴`, `다이아몬드`, `돌`, and `구운 소고기` should also be
protected by golden translation cases or a future `protected_aliases` artifact.

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

alias_resolution_coverage_percent
  = round(
      size(resolved_canonical_user_facing_targets)
      / canonical_user_facing_target_total
      * 100,
      2
    )

get_actionable_user_facing_targets
  = canonical_user_facing_targets intersect supports_get_targets

resolved_get_actionable_targets
  = get_actionable_user_facing_targets intersect targets_resolvable_from_korean

get_actionable_coverage_percent
  = round(
      size(resolved_get_actionable_targets)
      / size(get_actionable_user_facing_targets)
      * 100,
      2
    )
```

Canonicalized legacy targets are reported as `legacy_target -> canonical_target`
mappings. They are not subtracted again after canonicalization and are not
separate denominator targets.

Alias resolution coverage proves that Korean wording maps to canonical catalog
targets. GET actionable coverage additionally requires the target policy to say
`supports_get(target) == true`; do not treat alias coverage as executable
command coverage.

Classifier invariants:

```text
each raw catalog target has exactly one primary classification
all user-facing coverage set operations compare canonical target IDs
command_internal_only_targets means targets with no user-facing direct or
  generic-policy source
if a canonical target has at least one user-facing source, an internal alias to
  the same canonical target must not remove the whole canonical target
targets_resolvable_from_korean contains concrete canonical catalog targets only
supports_get_targets contains canonical catalog targets that the current target
  policy allows for GET
command modes, shortcut tokens, recipients, quantities, and routing statuses
  never count as item coverage
```

Do not add this as a current passing test:

```text
all 591 catalog targets have Korean aliases
```

The catalog can contain generic, legacy, internal, or command-only targets.
Future full-support gates should use unresolved canonical user-facing targets,
not the raw catalog total.

## Offline Test Determinism

Tier 1 offline tests must not call external LLMs, OpenAI APIs, network
services, Minecraft, Java processes, or a running LAVI UI. Reading Java source
files from Python and running a deterministic Python source/AST extractor is
allowed; spawning `java`, Gradle, or a Minecraft process is not.

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

The detailed command registry status is multi-axis and owned by
`chatclef-python-korean-command-registry-plan.md`. The legacy
`natural_language_status` field below is only a parser/compiler readiness
summary. It must not be read as bridge lifecycle readiness, gameplay-effect
verification, or public Korean enablement.

The matrix must use exactly one current value for each axis. Do not write
values such as `PLANNED or RAW_ONLY`; those cannot become stable tests.

Command owner values:

```text
chatclef_java
lavi_overlay
lavi_store_home
```

Source kind values:

```text
registered
virtual
```

Natural-language status values:

```text
IMPLEMENTED
PLANNED
RAW_ONLY
EXPLICIT_UNSUPPORTED
```

Command registry readiness axes to test separately:

```text
SOURCE_REGISTERED
KOREAN_PARSE_COMPILE_READY
PYTHON_ADMISSION_READY
BRIDGE_LIFECYCLE_READY
GAMEPLAY_EFFECT_VERIFIABLE
PUBLIC_KOREAN_ENABLED
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

SHARED_CASE matrix row shape:

```json
{
  "command": "example_client_state_command",
  "owner": "chatclef_java",
  "source_kind": "virtual",
  "natural_language_status": "RAW_ONLY",
  "effect_class": "CLIENT_STATE",
  "live_test_tier": 4,
  "live_test_allowed": false,
  "live_test_condition": "CLIENT_STATE_RESTORE_REQUIRED",
  "live_test_note": "Requires explicit opt-in and state restoration.",
  "behavior_verified": true,
  "verification_state": "VERIFIED",
  "verification_note": "Source reviewed for client-state effect only.",
  "golden_case_ids": [],
  "golden_tests": []
}
```

Schema version `1` is independent from evidence maturity. Both
`PRE_SHARED_CASE` and `SHARED_CASE` are valid under schema version `1`;
`evidence_maturity` alone selects the allowed evidence fields. A later schema
version is required only for a structural change outside that maturity switch.

PRE_SHARED_CASE support matrix top-level shape:

```json
{
  "schema_version": 1,
  "evidence_maturity": "PRE_SHARED_CASE",
  "commands": [
    {
      "command": "get",
      "owner": "chatclef_java",
      "source_kind": "registered",
      "natural_language_status": "IMPLEMENTED",
      "effect_class": "INVENTORY_MUTATING",
      "live_test_tier": 5,
      "live_test_allowed": false,
      "live_test_condition": "MUTATING_PREFLIGHT_REQUIRED",
      "behavior_verified": true,
      "verification_state": "VERIFIED",
      "planned_golden_case_ids": [
        "nl-get-diamond-basic"
      ],
      "current_golden_tests": [
        "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
      ]
    }
  ]
}
```

Evidence maturity rules:

```text
PRE_SHARED_CASE:
  allowed evidence fields:
    planned_golden_case_ids
    current_golden_tests
  forbidden evidence fields:
    golden_case_ids
    golden_tests

SHARED_CASE:
  allowed evidence fields:
    golden_case_ids
    golden_tests
  forbidden evidence fields:
    planned_golden_case_ids
    current_golden_tests
```

The matrix artifact must fail validation when:

```text
schema_version is not the supported value 1
unknown evidence_maturity value
planned_golden_case_ids and golden_case_ids both exist
current_golden_tests and golden_tests both exist
evidence_maturity does not match the row evidence fields
commands is missing or not an array
```

`effect_class` may be a conservative safety classification. It does not mean
the exact runtime behavior has been fully verified. Exact source/runtime
verification belongs to `behavior_verified`, `verification_state`, and
`verification_note`.

For commands whose effect is unknown, do not assign a live tier yet:

```json
{
  "command": "scan",
  "owner": "chatclef_java",
  "source_kind": "registered",
  "natural_language_status": "RAW_ONLY",
  "effect_class": "UNKNOWN_UNTIL_VERIFIED",
  "live_test_tier": null,
  "live_test_allowed": false,
  "live_test_condition": "SOURCE_REVIEW_REQUIRED",
  "live_test_note": "Live tests are blocked until effect classification is verified.",
  "safety_default": "TREAT_AS_MUTATING_AND_DO_NOT_EXECUTE",
  "behavior_verified": false,
  "verification_state": "SOURCE_REVIEW_REQUIRED",
  "verification_note": "Review implementation before assigning any live tier.",
  "golden_case_ids": [],
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
  "snapshot_authority": "git_blob_bytes",
  "reviewed_source_commit": "cfc170ac024a46ea943bffcaf289a91ff6bc5ee7",
  "hash_extraction": "git show --no-textconv <commit>:<repository-relative-path>",
  "sources": {
    "fabric_mod_manifest": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/resources/fabric.mod.json",
      "sha256": "5979599569ba808c52d15c5c6206d027eaff81cf66ffdd7422c4988f35c679c4"
    },
    "altoclef_mixin_config": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/resources/altoclef.mixins.json",
      "sha256": "c70c38a48b393ddcaeb75fcc4121d538a281a8ba0316502611b0982dea930471"
    },
    "altoclef_entry_mixin": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/mixins/EntryMixin.java",
      "sha256": "8bc0113969a76dc364edc3d6e0ebb5e25099c25923314239d41bd0648164ee55"
    },
    "altoclef_event_bus": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/eventbus/EventBus.java",
      "sha256": "f84d0bb0214a9631fbfd85619466d2dfc0dbef144632c9a9c03435b50a00f6e5"
    },
    "title_screen_entry_event": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/eventbus/events/TitleScreenEntryEvent.java",
      "sha256": "0766f7fa5ae9cd488a51a54311e8c2d28d48e845de78bd60bfb18e8cb2b03988"
    },
    "altoclef_entrypoint": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/AltoClef.java",
      "sha256": "442e31cf2cca6ba2e485ed0600eb3d7949618a8649627bf6496f0a83f7ded377"
    },
    "altoclef_commands_registration": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/AltoClefCommands.java",
      "sha256": "e48fe0bc2af71253f87270c11c4e2ce44bbec34f48588bf1077c659f35cd474e"
    },
    "get_command": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/GetCommand.java",
      "sha256": "4f3b86aabd4d94eab1a97b11d8225e2852ea4407c216bab5848b542894dc461d"
    },
    "overlay_entrypoint": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/overlay/OverlayEntrypoint.java",
      "sha256": "35ecd965b671d10c08f005e8b0eaffe2c2c1ee5274524370aaf55c13b1d1cecd"
    },
    "overlay_registrar": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/overlay/command/OverlayCommandRegistrar.java",
      "sha256": "c0fa480fe302c481194a630a0a15edefdd717ea204c844e6970b38fac1792579"
    },
    "overlay_command": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/overlay/command/OverlayCommand.java",
      "sha256": "b817517d4a98bd0826889ec3a72f13d16f7d1d3d7c5963e131dae1b2de8af0ce"
    },
    "chatclef_command_executor": {
      "path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commandsystem/CommandExecutor.java",
      "sha256": "17ef21bf10e5841b4fd2c5fe0190eda1b6d9ce2e9825a5cf059fdf44b47e7136"
    }
  },
  "activation_chains": {
    "chatclef_builtins": {
      "ordered_source_ids": [
        "fabric_mod_manifest",
        "altoclef_mixin_config",
        "altoclef_entry_mixin",
        "title_screen_entry_event",
        "altoclef_event_bus",
        "altoclef_entrypoint",
        "altoclef_commands_registration",
        "chatclef_command_executor"
      ],
      "edges": [
        {
          "from_source_id": "fabric_mod_manifest",
          "relation": "declares_main_entrypoint",
          "to_source_id": "altoclef_entrypoint"
        },
        {
          "from_source_id": "fabric_mod_manifest",
          "relation": "declares_mixin_config",
          "to_source_id": "altoclef_mixin_config"
        },
        {
          "from_source_id": "altoclef_mixin_config",
          "relation": "enables_client_mixin",
          "to_source_id": "altoclef_entry_mixin"
        },
        {
          "from_source_id": "altoclef_entry_mixin",
          "relation": "publishes_title_screen_entry_event_via",
          "to_source_id": "altoclef_event_bus"
        },
        {
          "from_source_id": "altoclef_entrypoint",
          "relation": "subscribes_to_title_screen_entry_event_via",
          "to_source_id": "altoclef_event_bus"
        },
        {
          "from_source_id": "altoclef_entry_mixin",
          "relation": "constructs_event",
          "to_source_id": "title_screen_entry_event"
        },
        {
          "from_source_id": "altoclef_entrypoint",
          "relation": "on_event_calls_onInitializeLoad_then_initializeCommands",
          "to_source_id": "altoclef_commands_registration"
        },
        {
          "from_source_id": "altoclef_commands_registration",
          "relation": "registers_commands_with",
          "to_source_id": "chatclef_command_executor"
        }
      ]
    },
    "lavi_overlay": {
      "ordered_source_ids": [
        "fabric_mod_manifest",
        "overlay_entrypoint",
        "overlay_registrar",
        "overlay_command",
        "chatclef_command_executor"
      ],
      "edges": [
        {
          "from_source_id": "fabric_mod_manifest",
          "relation": "declares_main_entrypoint",
          "to_source_id": "overlay_entrypoint"
        },
        {
          "from_source_id": "overlay_entrypoint",
          "relation": "registers_end_client_tick_callback",
          "to_source_id": "overlay_registrar"
        },
        {
          "from_source_id": "overlay_registrar",
          "relation": "registers_command_when_executor_available",
          "to_source_id": "overlay_command"
        },
        {
          "from_source_id": "overlay_registrar",
          "relation": "uses_executor",
          "to_source_id": "chatclef_command_executor"
        }
      ]
    }
  },
  "commands": [
    {
      "name": "get",
      "owner": "chatclef_java",
      "source_kind": "registered",
      "manifest_source_id": "fabric_mod_manifest",
      "entrypoint_source_id": "altoclef_entrypoint",
      "activation_chain_id": "chatclef_builtins",
      "registration_source_id": "altoclef_commands_registration",
      "command_class_source_id": "get_command",
      "name_source_id": "get_command"
    },
    {
      "name": "overlay",
      "owner": "lavi_overlay",
      "source_kind": "registered",
      "manifest_source_id": "fabric_mod_manifest",
      "entrypoint_source_id": "overlay_entrypoint",
      "activation_chain_id": "lavi_overlay",
      "registration_source_id": "overlay_registrar",
      "command_class_source_id": "overlay_command",
      "name_source_id": "overlay_command"
    }
  ]
}
```

The `commands` list in the real snapshot must include every registered command,
not only the two abbreviated examples above. Matrix tests should fail when a
registered command has no matrix row, or when a matrix row points to no
registered source unless it is explicitly marked as a virtual command.

Each registered command entry must prove activation, registration, and name
ownership:

```text
manifest_source_id points to the Fabric manifest that declares the entrypoint
entrypoint_source_id points to the Fabric entrypoint implementation declared by
  the manifest
activation_chain_id points to a topologically ordered, source-backed activation chain
registration_source_id points to a source that owns the command-class
  registration call
command_class_source_id points to the command implementation class
name_source_id points to the constructor or constant that defines the command
  name
every referenced source_id and activation_chain_id exists
every source entry has path and sha256
every activation edge references existing source IDs
```

`AltoClefCommands.java` alone is not enough because it registers command
classes such as `new GetCommand()`, while the literal command name `get` lives
inside the command class constructor. `OverlayCommand.java` alone is also not
enough because overlay registration flows through `OverlayEntrypoint.java` and
`OverlayCommandRegistrar.java`.

The snapshot must prove the activation chain, not only the final registration
call:

```text
ChatClef built-in commands:
  fabric.mod.json
    -> declares adris.altoclef.AltoClef as a Fabric main entrypoint
    -> declares altoclef.mixins.json
  altoclef.mixins.json
    -> enables client mixin EntryMixin
  EntryMixin
    -> injects into TitleScreen initialization
    -> EventBus.publish(new TitleScreenEntryEvent())
  AltoClef.onInitialize()
    -> subscribes to TitleScreenEntryEvent
    -> onInitializeLoad()
    -> initializeCommands()
    -> AltoClefCommands.init()
    -> CommandExecutor.registerNewCommand(...)

Overlay command:
  fabric.mod.json
    -> declares lavi.minecraft.overlay.OverlayEntrypoint
  OverlayEntrypoint.onInitialize()
    -> registers ClientTickEvents.END_CLIENT_TICK callback
  OverlayCommandRegistrar.onEndClientTick()
    -> waits for AltoClef CommandExecutor
    -> CommandExecutor.registerNewCommand(new OverlayCommand())
```

If the manifest, mixin configuration, mixin event publication, event
subscription, activation method, registration source, command class, or name
source changes, the source-backed command snapshot must detect that drift.

Required activation-chain regressions:

```text
remove EntryMixin from altoclef.mixins.json -> contract failure
remove TitleScreenEntryEvent publication from EntryMixin -> contract failure
remove TitleScreenEntryEvent subscription from AltoClef -> contract failure
remove onInitializeLoad -> initializeCommands call -> contract failure
remove OverlayEntrypoint from fabric.mod.json -> contract failure
remove overlay tick callback or registrar call -> contract failure
```

Preferred implementation direction for a later test phase:

```text
default Tier 1 authority:
  deterministic Python source/AST extractor
  -> reads Java and JSON source without spawning a Java process
  -> follows activation and registration call chains
  -> reads each command constructor or name constant
  -> compares source-derived output with the committed snapshot

optional build-contract parity job:
  small Java harness may generate or cross-check the same snapshot
  -> runs outside default pytest and outside the default Windows focused job
  -> never becomes a hidden Tier 1 Java-process dependency

tests do not re-create the command-name dictionary by hand
```

Extractor semantic contract:

```text
do not use regex-only extraction over raw source text as the command authority
commented or disabled code must not produce registered commands
active command registration must produce a snapshot entry
snapshot command name must equal the command constructor or name constant
effective command names must be globally unique across chatclef_java,
  lavi_overlay, and lavi_store_home owners
duplicate effective command name -> contract failure
```

Required extractor regressions:

```text
active new GetCommand() -> snapshot contains get
commented // new StashCommand() -> snapshot does not contain stash
constructor name "get" -> snapshot name is get
same effective name across any registered owners -> contract failure
```

Prefer a deterministic Python parser or AST-based source extractor for the
default Tier 1 contract. A small Java harness is optional only in the separate
build-contract parity job described above. Do not use a simple regular
expression scan as the command authority. `CommandExecutor` rejects duplicate
command names at runtime, and the snapshot contract should fail before runtime
for the same collision class.

The artifact paths and schema above are planned Phase 0 artifacts. Phase 0 is
not complete until those JSON files are actually committed and contract tests
prove that they contain every registered command, actual source hashes, exact
owners, boolean `live_test_allowed`, and command-specific golden-case evidence.
Before the shared case artifact exists, that evidence is
`planned_golden_case_ids` plus `current_golden_tests`; after it exists, it is
`golden_case_ids`.

Java item-command contract provenance must also be tested before trusting
Python fixtures:

```text
tests/minecraft_chatclef/command_catalog/test_java_item_command_contract_provenance.py
```

The hash authority must be identical across documentation, fixtures, tests, and
CI:

```text
snapshot_authority == git_blob_bytes
reviewed_source_commit or snapshot_baseline_commit is a full 40-character commit SHA
hash bytes come from:
  git show --no-textconv <commit>:<repository-relative-path>
tests must not use working-tree Path.read_bytes() as the authority for Git
  source-blob contract hashes
tests must not hash CRLF-converted working-tree files
```

Hash extraction must preserve raw stdout bytes. Do not route source bytes
through a PowerShell text pipeline. A Python subprocess using raw stdout bytes
is acceptable.

If GitHub Actions needs to verify a baseline commit other than the checked-out
HEAD, checkout must make that commit available:

```yaml
- uses: actions/checkout@v4
  with:
    fetch-depth: 0
```

Required assertion:

```text
reviewed_source_commit matches the documented reviewed implementation commit
for every Java contract source:
  sha256(git_blob(reviewed_source_commit, path)) == expected_sha256
  sha256(git_blob(HEAD, path)) == expected_sha256
contract fixture validation stops if any baseline provenance or current drift
  check fails
```

Reviewed Java item-command contract source snapshot:

```json
{
  "snapshot_authority": "git_blob_bytes",
  "reviewed_source_commit": "cfc170ac024a46ea943bffcaf289a91ff6bc5ee7",
  "hash_extraction": "git show --no-textconv <commit>:<repository-relative-path>",
  "java_contract_sources": {
    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/GetCommand.java": "sha256:4f3b86aabd4d94eab1a97b11d8225e2852ea4407c216bab5848b542894dc461d",
    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/EquipCommand.java": "sha256:fc63661acb97a98c989165f6499c2216a711b66359695e64682303f03af7848a",
    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/DepositCommand.java": "sha256:4259159f77f5467401bd6b96e89c3fc8898a75b72716c9ac26f4007cd881ca80",
    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/GiveCommand.java": "sha256:b88c70e31ee2f76cd078ec691fdc3ecf8acd8ba92acca6dfcb2779c60e1d0a45",
    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commandsystem/Arg.java": "sha256:26d54316c9b3336ba63dcbe848274d5005e21cc26ca00fe75c95840e9cfe40c2",
    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commandsystem/ArgParser.java": "sha256:d2fcd9aaa8c6f59d2958545319e0c0543940f2e62875c46e2460f1ef60347cc8",
    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commandsystem/CommandExecutor.java": "sha256:17ef21bf10e5841b4fd2c5fe0190eda1b6d9ce2e9825a5cf059fdf44b47e7136",
    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commandsystem/ItemList.java": "sha256:38c442cc01a700a91c2b3af2dadb86a32ca4316a81cbbec2f7d311d6f30783ef",
    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandDispatcher.java": "sha256:957b76e4515edca4b5ed12d76b3dc27833488da41294a19495be73a6391d7f77"
  }
}
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

Rows marked `IMPLEMENTED` in the legacy natural-language column prove only the
Korean parser/compiler or current support-matrix scope described by their tests.
They do not prove natural completion, active-command cancellation, gameplay
effect, or public enablement. In particular, `idle`, `follow`, and `stop` must
keep separate lifecycle/cancellation evidence before they can be promoted in the
command registry axes.

IMPLEMENTED rows must keep test evidence. Until the shared case artifact is
committed, split planned IDs from currently existing test paths:

```json
{
  "get": {
    "planned_golden_case_ids": [
      "nl-get-iron-ingot-basic",
      "nl-get-iron-ingot-spaced-quantity",
      "nl-get-gold-ingot-basic",
      "nl-get-cooked-beef-basic",
      "nl-get-log-basic",
      "nl-get-diamond-basic",
      "nl-get-diamond-mining",
      "nl-get-stone-quantity",
      "nl-get-cobblestone-mining-quantity",
      "nl-get-coal-mining-quantity",
      "nl-get-iron-mining-quantity"
    ],
    "current_golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl",
      "tests/minecraft_chatclef/lavi_input/test_korean_input_to_chatclef_submission_path.py::test_lavi_korean_input_reaches_adapter_as_prefixless_command",
      "tests/minecraft_chatclef/alias_contract/test_all_runtime_aliases_translate.py"
    ]
  },
  "food": {
    "planned_golden_case_ids": [
      "nl-food-basic"
    ],
    "current_golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  },
  "meat": {
    "planned_golden_case_ids": [
      "nl-meat-basic"
    ],
    "current_golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  },
  "goto": {
    "planned_golden_case_ids": [
      "nl-goto-coordinate-basic"
    ],
    "current_golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  },
  "follow": {
    "planned_golden_case_ids": [
      "nl-follow-player-basic"
    ],
    "current_golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  },
  "idle": {
    "planned_golden_case_ids": [
      "nl-idle-basic"
    ],
    "current_golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  },
  "stop": {
    "planned_golden_case_ids": [
      "nl-stop-basic"
    ],
    "current_golden_tests": [
      "tests/minecraft_chatclef/korean_translation/test_korean_command_support_matrix.py::test_supported_korean_commands_translate_to_prefixless_chatclef_dsl"
    ]
  }
}
```

After the shared case artifact exists, `planned_golden_case_ids` becomes
`golden_case_ids`, and a contract test must verify that each `golden_case_id`
is present in the actual case table, not only that the test function path
exists.

Support matrix tests should verify:

```text
every registered command appears in the matrix exactly once
every matrix row points to a registered source or an explicit virtual command
actual registration chain is verified, not inferred from all Command subclasses
commented StashCommand and unregistered helper command classes are excluded
source_kind is either registered or virtual
no matrix row uses an "or" status
multi-axis registry status fields are present or linked from the command
  registry plan
behavior_verified is boolean
live_test_allowed is boolean
live_test_condition is one of the closed enum values
verification_state is one of the closed enum values
UNKNOWN_UNTIL_VERIFIED rows have live_test_tier == null
UNKNOWN_UNTIL_VERIFIED rows have live_test_allowed == false
IMPLEMENTED commands have at least one golden case
before the shared case artifact exists, IMPLEMENTED commands have at least one
  planned_golden_case_id or current_golden_tests entry
after the shared case artifact exists, IMPLEMENTED commands have at least one
  golden_case_id
every row has a planned_golden_case_ids or golden_case_ids list, depending on
  artifact maturity
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

### Canonical submit-result boundary

The Python router and the supervised one-shot runner must consume the same
canonical submit-result normalizer. The extension result is untrusted even when
it is an in-process mapping. A valid result requires:

```text
outer ok and nested status.ok are exact bool values
outer ok == nested status.ok == CommandResultStatus.ok
nested request_id is a nonblank string and matches the expected request ID
outer request_id, when present, matches the nested request_id
status and error_code are known closed-enum values
successful status has no error_code
outer error/message/details mirror nested error_code/message/data
normal output is rebuilt as fresh top-level, nested, and data mappings
```

String `"false"`, integer `0` or `1`, an unknown enum, a malformed nested
status, an accepted status with `internal_error`, or any mirror contradiction
must become `submission_outcome_unknown` with
`reconciliation_required == true`. Neither consumer may infer acceptance or
retry from a contradictory payload.

The timeout regression must use a coroutine scheduled on a real running
`asyncio` event loop. It must prove that a first send which times out but later
reaches the wire retains command ownership, rejects a second submit, never
sends the second request, and tears down with no pending task or unawaited
coroutine. A fake future that closes the coroutine immediately is useful only
for local classification and is not sufficient evidence for late-send ordering.

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
  inventory_preflight/
  input_gate/
  korean_translation/
  lavi_input/
  orchestration/
  response/
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
| inventory cleanup preflight | `tests/minecraft_chatclef/inventory_preflight/` |
| Python command orchestration | `tests/minecraft_chatclef/orchestration/` |
| deterministic response rendering | `tests/minecraft_chatclef/response/` |
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

## Windows CI Required Offline Scope

The default Windows CI must run the same offline ChatClef-focused test range
used for local merge gating. GitHub Actions must not stay green while the
ChatClef-focused suite is red.

Required offline scope:

```text
tests/minecraft_chatclef/**
tests/test_minecraft_chatclef_*.py
tests/test_llm_minecraft_input_router.py
```

Required result:

```text
0 failed
live runtime tests skipped unless explicit live opt-in is present
```

PowerShell-safe discovery should avoid Unix-only glob behavior:

```powershell
$chatClefTests = @()

$chatClefTests += Get-ChildItem `
    -LiteralPath .\tests\minecraft_chatclef `
    -Recurse `
    -Filter *.py `
    -File |
    Where-Object { $_.Name -like "test_*.py" } |
    ForEach-Object { $_.FullName }

$chatClefTests += Get-ChildItem `
    -LiteralPath .\tests `
    -Filter "test_minecraft_chatclef_*.py" `
    -File |
    ForEach-Object { $_.FullName }

$chatClefTests += ".\tests\test_llm_minecraft_input_router.py"

.\venv\Scripts\python.exe -m pytest -q @chatClefTests
```

If Java source contract tests read Git blob bytes from a documented baseline
commit, the checkout must provide that commit:

```yaml
- uses: actions/checkout@v4
  with:
    fetch-depth: 0
```

Do not include live Minecraft tests in this default offline CI scope.

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
```

Unknown-effect commands such as `scan` are not Tier 5 test targets yet. Their
safety default is `TREAT_AS_MUTATING_AND_DO_NOT_EXECUTE`, with
`live_test_tier: null` and `live_test_allowed: false` until source review
assigns a concrete effect class.

Mutating tests must target only a dedicated Fabric test instance and test
world. They must never run in default CI, never run against a personal survival
world, and never auto-retry or replay a command after timeout or failure.

Do not live-test all aliases. Alias coverage belongs in offline tests. Live
tests should use representative smoke cases by command family.

Audited implementation baseline `4239c23`의 unittest-gated mutating runtime test는
다음 값을 사용한다.

```text
LAVI_MINECRAFT_RUNTIME_TESTS=1
LAVI_MINECRAFT_RUNTIME_MUTATING=1
LAVI_MINECRAFT_RUNTIME_KOREAN_COMMAND=<optional command, default 돌 1개 가져와줘>
LAVI_MINECRAFT_RUNTIME_TIMEOUT_SEC=60
LAVI_MINECRAFT_RUNTIME_POLL_SEC=2
```

이 목록은 audited baseline의 현재 동작이지 안전한 목표 preflight 계약이 아니다.
특히 baseline test는 `LAVI_GRADIO_URL`이 없으면 `http://127.0.0.1:47860`을
사용한다. 복구 목표 계약의 mutating mode는 explicit loopback
`LAVI_GRADIO_URL`을 필수로 하고 default endpoint를 사용하지 않는다.

At the 2026-08-18 post-restore audit, baseline `4239c23` performs one Gradio
submission, records the returned `request_id`, polls status, ignores stale
request results, and waits until the same request reaches a terminal status.
The polling loop refreshes status only; it does not retry or replay the command.
This proves one Gradio submit call in the test body; it does not by itself prove
one lower adapter `command_request` without separate instrumentation or
correlation evidence.

This is a lifecycle-correlation contract, not a gameplay-success contract. The
current terminal set includes:

```text
completed
rejected
failed
cancelled
deadline_exceeded
unknown
```

The exact audited-baseline pass condition is:

```text
matching request_id
+ terminal status
+ active_request_id == null in the same refresh snapshot
+ when data is a dict and result_reason exists, result_reason is nonblank
```

Missing `result_reason` alone is not an audited-baseline failure. Its
status-specific requiredness remains `[unverified]`.

Therefore a passing audited-baseline test means that a matching terminal
lifecycle was observed. It does not mean that the command completed
successfully, that the character moved, that a block was broken, or that
inventory increased.

Mutating live tests must also gain a dedicated-environment preflight before any
future command-request implementation is expanded. Required policy variables:

```text
LAVI_GRADIO_URL=http://127.0.0.1:<explicit-port>
LAVI_MINECRAFT_EXPECTED_BACKEND=fabric_chatclef
LAVI_MINECRAFT_EXPECTED_INSTANCE=LAVI_TEST_Fabric01
LAVI_MINECRAFT_EXPECTED_WORLD=<dedicated-test-world>
```

The operator must also provide a fresh approval record bound to the exact
command text, Gradio URL, backend, instance, world/save directory, and one-shot
invocation. The default smoke candidate `돌 1개 가져와줘` still requires that
approval; any override requires separate exact approval.

Before sending `command_request`, the test must compare these expected values
with the runtime status. Mismatch behavior:

```text
backend mismatch -> fail before command_request
instance mismatch -> fail before command_request
world mismatch -> fail before command_request
```

If the live or mutating opt-in is absent, the live test skips and submits zero
commands. Once both opt-ins select a mutating run, a missing dependency/value,
unknown or inaccessible identity, stale/unstable evidence, ambiguity, or
mismatch is a deterministic preflight failure with zero submissions. Do not
silently continue or relabel selected-run evidence failure as a skip. Forge
MineMind fallback remains forbidden.

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

Do not let a single accidental environment variable run mutating tests. Do not
allow IDE, CI, a flaky-test plugin, a wrapper, or Codex automation to rerun a
mutating test automatically.

## Live Test Safety Rules

Live test execution requires explicit user approval and opt-in environment
variables. A safe live test design should identify:

```text
exact approved command and one-shot invocation
explicit loopback Gradio URL
expected backend
dedicated test instance
dedicated test world
whether command_request is sent
whether movement can occur
whether inventory can change
whether world blocks can change
whether client state can change
terminal lifecycle predicate
runtime-reported completion predicate
gameplay effect oracle and prohibited effects
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

The audited baseline `4239c23` mutating Korean runtime test is safer than an ACCEPTED-only smoke
test because it waits for a terminal result. Since `6d75c6e`, it also verifies
that the terminal result belongs to the submitted `request_id` and includes an
offline stale-result regression test. The remaining unsafe gap is the missing
fail-closed backend, instance, world, listener/process ownership, and
command-immediate TOCTOU preflight. Until that preflight is restored and
verified, mutating live tests must not run unattended. A supervised one-shot run
still requires fresh exact-tuple user approval, an explicit loopback endpoint,
manual verification of every missing identity condition, and automatic rerun
disabled.

Terminal success checks must be stricter than `active_request_id is None`,
because an active request can disappear after disconnect, session replacement,
server stop, ownership reset, or a stale terminal result from a previous
request.

Audited baseline terminal lifecycle observation requires:

```text
matching terminal CommandResultDTO observed
result request ownership matches the submitted command
submitted request_id == terminal result request_id
terminal status is recorded
active request is clear in the same refresh snapshot
if data is a dict and result_reason is present, result_reason is nonblank
```

The result hierarchy uses four independent terms:

```text
terminal lifecycle observed:
  matching request_id + terminal status + active request clear

runtime-reported completion:
  terminal lifecycle observed + status == completed

gameplay effect observed:
  expected, partial, or unexpected Minecraft state/effect observed independently
  of terminal status

end-to-end success:
  runtime-reported completion
  + gameplay observation complete
  + expected gameplay effect verified
  + prohibited effect absence verified
```

For example, a future GET effect test must define and observe its inventory or
world-state success evidence. A matching `failed`, `cancelled`, `rejected`,
`deadline_exceeded`, or runtime terminal status `"unknown"` result completes
lifecycle observation but is not runtime-reported completion. Runtime status
`"unknown"` is distinct from tri-state `unknown`, which means an observation
value could not be established. A non-completed terminal can still leave
movement, block, or inventory side effects, so partial and unexpected effects
are recorded independently and require reconciliation.

The authoritative gameplay projection in `LiveRunObservation` uses these
separate tri-state fields:

```text
gameplay_observation_complete: true | false | unknown
gameplay_effect_observed: true | false | unknown
expected_gameplay_effect_verified: true | false | unknown
partial_gameplay_effect_observed: true | false | unknown
unexpected_effect_observed: true | false | unknown
prohibited_effect_absence_verified: true | false | unknown
end_to_end_success: true | false | unknown
```

`prohibited_effect_absence_verified == true` requires complete observation of
every state surface named by the command-specific oracle. Merely failing to see
a prohibited effect is not absence evidence when the observation source is
missing or incomplete. `end_to_end_success == true` is allowed only when
runtime-reported completion, `gameplay_observation_complete`,
`expected_gameplay_effect_verified`, and
`prohibited_effect_absence_verified` are all true.

Live E2E directly observed fields:

```text
last_result.request_id
last_result.status
last_result.data.result_reason
```

The current live status surface does not expose envelope `session_id`,
`correlation_id`, or `connection_generation` inside `last_result`. Do not make
the live E2E test require fields it cannot observe.

Server ownership and loopback tests must cover:

```text
session_id
correlation_id
connection_generation
stale session result rejection
stale generation result rejection
wrong request_id result rejection
```

If a future implementation needs live E2E visibility for those ownership
fields, add Python-internal observation metadata without changing the Java wire
payload or `CommandResultDTO` shape:

```json
{
  "last_result": {
    "request_id": "...",
    "status": "completed",
    "data": {
      "result_reason": "matching_task_finished"
    }
  },
  "last_result_observation": {
    "session_id": "...",
    "correlation_id": "...",
    "connection_generation": 3
  }
}
```

Do not pass a terminal live test solely because `active_request_id` became
empty. Even after matching terminal, `active_request_id == null` proves only
that the Python request lifecycle is clear. It does not prove that the
Minecraft ChatClef Task stopped or that no partial/unexpected effect remains.

If the server may have received the submit request but accepted response was
lost through timeout, connection reset, disconnect, or malformed response,
record `submission_outcome_unknown`. Never treat that as `not submitted`, never
replay the original command, and block the next mutating run until the operator
reconciles active request, last result, request/correlation evidence, and
Minecraft state.

An observer timeout is not a terminal status, failure, or cancellation. It must
not trigger automatic `@stop`, cancel, retry, replay, or test rerun.

Required stale-result regression:

```text
previous request terminal last_result exists
new command is submitted
old last_result remains terminal
test does not pass until last_result.request_id == submitted request_id
```

Do not:

```text
start Minecraft automatically in CI
run against a personal world
execute all 591 catalog targets live
run deposit or give without prepared inventory and recipient preconditions
retry command_request automatically
replay the original command after failure
rerun a mutating test automatically from IDE, CI, flaky plugin, wrapper, or Codex
send automatic @stop or cancel after observer timeout
fallback to Forge MineMind
```

Each future gameplay-effect oracle must define, per command:

```text
pre-state
expected effect
observation source
tolerance
forbidden/prohibited effect
observation timeout
reconciliation procedure
```

The oracle maps its evidence to `gameplay_observation_complete`,
`expected_gameplay_effect_verified`, `partial_gameplay_effect_observed`, and
`prohibited_effect_absence_verified`. If the observation source is missing or
incomplete, observation completeness is false or `unknown`, expected/prohibited
evidence and end-to-end success remain `unknown`, and runtime-reported
completion must not be promoted to end-to-end success.

The current default GET batch oracle observes only the saved target-item
inventory delta. It intentionally reports
`gameplay_observation_complete == false` and leaves prohibited-effect absence
unverified because it does not observe movement, blocks, other inventory slots,
or every command-specific prohibited surface. Consequently, the production
default four-command batch must stop at the first incomplete gameplay
checkpoint. The fixture-driven four-step completion test proves ordering,
single submission, checkpoint gating, and no replay; it does not prove that the
default live observer can safely advance all four commands. Automatic four-step
live advancement requires a separately reviewed complete multi-surface oracle.

<!-- 20260819_kpopmodder: Clarified additive GET evidence and immutable batch ownership. -->

The current ChatClef GET contract is additive: the requested quantity is added
to the target derived from the inventory already present when the command
starts. Therefore the supported live objective is named
`get_acquisition_delta`, and its functional evidence is `after - before >=
requested_count`. A final-total-only check is not sufficient because an
already-present stack could make a no-op look successful.

`movement_and_mining` is a separate test objective. It requires a verified
initial shortage plus complete movement, mined-block, inventory, and prohibited
surface observers. The current harness rejects that objective before gateway
creation because those complete observers do not yet exist; terminal completion
or a positive target delta must not be relabeled as movement success.

The fixed four-step plan is represented by frozen step values. Each step must
retain the first verified listener process fingerprint, exact nonblank submitted
and terminal request IDs must match, and callbacks receive read-only environment
snapshots. A process change, malformed identity, request-ID mismatch, or attempt
to mutate the approved command stops the batch before another submission.

Offline fixtures must distinguish at least:

```text
complete observation + expected effect + prohibited-effect absence -> E2E true
completed runtime result + expected effect false -> E2E false
prohibited effect not seen + incomplete observation -> absence and E2E unknown
non-completed terminal + partial effect -> partial true, E2E false, reconcile
runtime terminal status "unknown" -> lifecycle terminal value, not tri-state unknown
```

## 2026-08-18 Post-Restore Live Runtime Status

This audited-baseline section supersedes only the live-runtime implementation
claims in older documentation-review snapshots and handoff summaries below.
It does not rewrite their historical test counts or broader merge findings.
The fixed implementation baseline is commit `4239c23`; a later source/test
commit requires a new read-only audit before this status is updated.

Implemented in audited baseline `4239c23`:

```text
explicit live/mutating opt-in
one Gradio Korean-command submission
accepted response and submitted request_id capture
matching request_id terminal polling
stale terminal rejection helper and regression test
same-refresh active_request_id clear check with matching terminal
conditional nonblank result_reason check when data is a dict and the key exists
no retry/replay inside the current test flow
```

Absent or incomplete in audited baseline `4239c23`:

```text
explicit mutating LAVI_GRADIO_URL requirement without default fallback
fresh exact command/URL/backend/instance/world/one-shot approval tuple
fail-closed expected backend/instance/world gate
selected Gradio and Fabric 4316 same-process ownership gate
hidden second LAVI detection across the effective Gradio range
exact latest.log stable-snapshot and strict UTF-8/CP949 fallback
command-immediate status and listener-ownership TOCTOU recheck
structured preflight ok/skip/fail runner and offline fixtures
deterministic fail rather than skip after mutating opt-in
submission_outcome_unknown reconciliation and next-run block
external IDE/CI/flaky/Codex automatic-rerun guard
status == completed requirement for runtime-reported completion
inventory, movement, block, or other expected/partial/unexpected effect verification
gameplay_observation_complete and prohibited_effect_absence_verified evidence
end-to-end success oracle
```

The 2026-08-16~17 documents recorded the missing preflight items as implemented
in a dirty working tree, but those changes were not committed and are absent
from audited baseline `4239c23`. They are recovery-and-reverification work, not
current implementation claims. The complete `PreflightDecision` and
`LiveRunObservation` structures are owned by
`fabric-chatclef-live-runtime-preflight-plan.md`; this strategy owns the
gameplay field semantics and command-specific oracle requirements.

## Original 2026-08-15 Implementation Status At Documentation Review

This section records the originally reported 2026-08-15 documentation-review
snapshot. It is retained for provenance and is not the current implementation
or CI status at reviewed archive baseline `e08af639`.

Reported as satisfied in the original revised documentation set:

```text
runtime baseline, reviewed implementation baseline, and fixed audited implementation baseline are separated
reviewed source SHA-256 values are recorded with Git blob-byte authority
all Related Documents in the Korean ChatClef documentation unit are present
ChatClef built-in activation provenance includes manifest, mixin config,
  EntryMixin event publication, AltoClef event subscription, and command registration
coverage snapshots identify coverage algorithm version and catalog parser provenance
catalog parsing requires all ten production baseline targets
support-matrix schema version 1 is independent from evidence maturity
Tier 1 source extraction does not spawn Java; an optional Java harness belongs to
  a separate build-contract parity job
quantity and malformed TranslationResult regression vectors are documented
Windows CI focused scope is documented
live mutating-test safety expectations are documented
```

Reported as not yet satisfied in implementation or CI at that original review
snapshot:

```text
focused ChatClef suite still reports 98 passed, 9 failed, 2 skipped, and
  258 subtests passed
the 9 failures are Java source-hash subtests
the current hash test still uses old constants and working-tree Path.read_bytes()
Windows CI does not yet execute the ChatClef-focused offline scope
Windows CI checkout does not yet use fetch-depth: 0 for source-blob baselines
registered-command JSON artifacts are not committed
support-matrix JSON artifact is not committed
golden_case_ids are not present in the actual shared test data
live backend/instance/world preflight is not implemented in audited baseline 4239c23
live matching-result request_id guard is implemented in `6d75c6e`; older review snapshot retained here for history
session/correlation/generation live observation is not exposed through last_result
```

Do not treat the implementation gate below as already satisfied.

## Documentation Acceptance

The documentation set is acceptable when the following are true:

```text
terminology and responsibility boundaries are internally consistent
historical baseline, reviewed baseline, and fixed audited implementation baseline are separated
artifact schemas contain all required fields
registered-command activation chains cover the actual Fabric manifest, mixin,
  event, entrypoint, and registration path
coverage artifacts identify algorithm version, parser path, parser hash, and parser commit
catalog parser contracts require all production baseline targets
support-matrix schema version and evidence maturity have deterministic, non-conflicting roles
Tier 1 source extraction and any optional Java harness run in explicitly separate jobs
current source-backed contracts and planned phase contracts are not mixed
Related Documents references are present in the same commit or removed
golden_case_ids are marked planned until the shared case artifact exists
result_reason observation path matches the current DTO/status projection shape
numeric slot contracts cover quantity, food_units, and goto x/y/z
future command behavior is represented by the support matrix, not failing tests
support matrix rows use one natural-language status, one effect class, and one live tier field
live_test_allowed is boolean and live_test_condition carries condition text
behavior_verified is boolean and verification_state carries non-boolean context
unknown-effect commands have null live tier and live tests disabled
registered command authority is source-backed and owner-scoped
registered command snapshot and support matrix artifact paths are defined
registered command artifacts are planned and do not make Phase 0 complete until committed and tested
raw catalog coverage breadth is visible but not treated as complete
runtime baseline, reviewed runtime baseline, dynamic current, and proposed v2
  draft coverage are separate
coverage snapshot and coverage floor are separate
coverage floor uses integer covered-target count as the authority
natural-language commands stay prefixless through the adapter boundary
raw command tests may still use @ when they are not natural-language tests
live tests are opt-in and side-effect-classified
accepted commands are never automatically replayed
Fabric ChatClef tests do not import Forge MineMind fixtures
```

## Implementation Gate Before Phase 1

Before treating Phase 0 as complete or starting the next implementation phase,
the actual implementation and CI must satisfy:

```text
focused ChatClef suite has 0 failed tests
Windows CI runs the ChatClef-focused offline scope
checkout provides source blobs needed by baseline tests, for example fetch-depth: 0
hash tests use raw Git blob bytes and updated expected hashes
registered-command snapshot artifact is committed and validates the full
  Fabric activation chains
support-matrix artifact is committed with schema_version 1 and an explicit
  evidence_maturity
coverage artifacts carry algorithm and parser provenance and pass baseline-target checks
shared golden-case artifact is committed or all planned IDs remain clearly planned
current aliases are hard-validated
Tier 1 offline tests do not call real LLMs or networks
mutating live tests require backend, instance, and world preflight before command_request
terminal lifecycle observation requires matching last_result.request_id, a terminal
  last_result.status, same-refresh active_request_id clear, and a nonblank
  last_result.data.result_reason only when that key exists in a dict data payload
runtime-reported completion additionally requires status == completed
gameplay observation records completeness plus expected, partial, unexpected,
  and prohibited-effect absence as separate tri-state fields
end-to-end success additionally requires gameplay_observation_complete,
  expected_gameplay_effect_verified, and prohibited_effect_absence_verified
  all to be true
incomplete observation never converts "prohibited effect not seen" into verified absence
mutating live tests do not end at ACCEPTED only without teardown or terminal observation
```

## 2026-08-19 Offline Regression Closure

The current LAVI-owned Python regression scope additionally locks these
boundaries:

```text
the exact approved Korean batch inputs compile to prefixless GET commands
natural-language translation occurs once before the same validated result is submitted
canonical submit output always contains matching top-level and nested request_id
only exact bool values are accepted for mirrored ok fields
malformed or contradictory submit results become UNKNOWN with reconciliation required
router UNKNOWN ownership blocks later Minecraft translation and submission
only matching validated terminal evidence clears that router ownership latch
the command that triggers successful reconciliation is still not submitted and
  must be sent again explicitly as a fresh command
submission timeout, request mismatch, duplicate evidence, or uncleared active ownership
  stops before a gameplay checkpoint
a safely correlated non-completed terminal may run a read-only gameplay checkpoint
  to preserve partial and unexpected-effect evidence, but never advances
batch advancement requires completed plus runtime completion plus a complete positive oracle
gameplay timing rejects bool, NaN, positive infinity, and negative infinity
```

The default saved-player target-inventory observer is still not a complete
gameplay or prohibited-effect oracle. Therefore, an offline fixture may prove
four-step ordering, but an actual live run must stop at its first incomplete
checkpoint. A four-command live success claim requires a separately approved,
command-correlated multi-surface oracle.

## Historical 2026-08-18 ChatGPT Handoff Summary

This block is retained for audited-baseline provenance. Its implementation
status predates the 2026-08-19 offline regression closure above and must not be
read as the current worktree status.

```text
Codex/ChatGPT reconciled the Korean ChatClef test strategy in:

plugins/Minecraft/docs/chatclef-korean-test-strategy.md

Documentation authority split:

- item/action UX design and phase order:
  chatclef-korean-item-action-alias-v2-plan.md
- test, artifact, source/hash, coverage-parser, CI, and live-safety authority:
  chatclef-korean-test-strategy.md
- current merge status and incomplete implementation work:
  chatclef-korean-post-review-merge-blockers.md

In that historical 2026-08-18 scope, the alias plan, test strategy, and
post-review merge-blocker document were treated as one documentation unit. The
2026-08-19 Python Korean command orchestration follow-up expands the directly
affected documentation authority set; see the current post-review addendum and
README reading map for that later scope.

Key frozen contracts:

1. Python Korean natural-language output remains prefixless.
2. Historical c912ff baseline, reviewed cfc170a baseline, and audited
   implementation baseline 4239c23 remain separate; later source/test commits
   require re-audit.
3. Coverage algorithm version 1 uses the reviewed catalog parser at cfc170a and
   requires the ten production baseline targets.
4. Registered-command provenance follows the full built-in activation path:
   fabric.mod.json -> altoclef.mixins.json -> EntryMixin ->
   TitleScreenEntryEvent -> AltoClef subscription -> onInitializeLoad ->
   initializeCommands -> AltoClefCommands -> CommandExecutor.
5. Overlay provenance follows manifest -> OverlayEntrypoint -> tick callback ->
   OverlayCommandRegistrar -> OverlayCommand.
6. Default Tier 1 tests may parse Java source from Python but do not spawn Java.
   An optional Java harness belongs only to a separate build-contract parity job.
7. Support-matrix schema version 1 supports PRE_SHARED_CASE and SHARED_CASE;
   evidence_maturity selects mutually exclusive evidence fields.
8. result_reason is observed as last_result.data.result_reason.
9. golden_case_ids remain planned until a shared repository-owned case artifact exists.
10. Current source-backed Java contracts remain separate from planned Python
    multi-item GET, EQUIP, DEPOSIT, and GIVE work.

Historical implementation status at that 2026-08-18 handoff remained not green:

- reported focused suite: 98 passed, 9 failed, 2 skipped, 258 subtests passed
- the 9 failures are Java source-hash subtests
- the current hash test still uses old constants and working-tree Path.read_bytes()
- Windows CI does not run the focused ChatClef offline scope
- registered-command, support-matrix, coverage, and shared golden-case artifacts
  are not committed
- live matching-request guard is implemented in `6d75c6e`
- live backend/instance/world/process preflight is not implemented in audited
  baseline 4239c23
- the audited live test observes matching terminal lifecycle, not
  runtime-reported completion or end-to-end gameplay success
- gameplay E2E requires complete observation, expected-effect verification, and
  actively verified prohibited-effect absence; partial effect remains separate

No Java, DTO, wire payload, ChatClef engine, Forge MineMind, live Minecraft
behavior, or test execution was changed by this documentation revision.
```

## 2026-08-29 Checkpoint Registered-Command And STORE_HOME Direct-Test Gate

This addendum supersedes historical current-status wording above where the
dates conflict. It augments, rather than replaces, the broader Phase 0 and CI
gate.

The command-catalog and focused-suite evidence at that checkpoint was:

```text
Java registered-command snapshot: 22
support-matrix rows:              22
production Python registry names: 21
missing production row:           deposit_all

focused subset:
  collected: 37
  passed:    36
  failed:     1

failure:
  PythonKoreanCommandRegistryTests::
  test_registry_contains_exactly_the_registered_chatclef_commands
```

The exact-set test is correct. Do not weaken it, delete
`DepositAllCommand` from source-backed evidence, or mark `deposit_all` public
merely to obtain green output. The canonical row is raw-only catalog metadata:

```text
source_registered=true
exposure=raw_only
lifecycle_kind=task
safety_tier=R2
slot_schema=items?
resolver_domain=command_specific
confirmation_mode=none
allowed_input_sources=[]
all Korean/parser/admission/bridge/gameplay/public readiness axes=false
```

The exact Python values are `slot_schema=("items?",)` and
`allowed_input_sources=()`. `items?` uses the existing optional-slot suffix
convention and is not an unresolved marker. `exposure` is derived from source
registration and readiness rather than stored as a separate
`ChatClefCommandSpec` field. The checkpoint snapshot and support-matrix schema did
not carry `slot_schema`; their obligation was exact command identity and support
classification. A dedicated production-registry spec test owns the exact slot
and readiness values without expanding either artifact schema.

The STORE_HOME evidence supplied for that checkpoint separately proved a clean Gradle build,
243 JUnit tests with zero failure/error and 14 skipped, matching built/deployed
JAR file identity, an observed runtime code-source path, and two completed
STORE_HOME operations with zero candidate/operation timeout decisions.
Operation 225 directly proves exact activation after 7,726 active ticks;
operation 30351 is a repeat completion/no-timeout observation. This does not
prove complete source-to-runtime provenance, so
`artifactParity=PARITY_UNPROVEN` remains required.

At that checkpoint, focused Java coverage before commit had to directly prove the following actual
production contracts.

```text
StoreHomeTaskLifecycleController decision order:
  emergency hard cap remains before behavior
  pending ownership, context, and cursor guards retain their current precedence
  active candidate + no session + no pending may consume the current position
    sample and exact activation before operation no-progress is re-evaluated
  that narrow no-progress boundary does not run a full navigation/session step
  after an allowed normal navigation/session step, operation no-progress is
    evaluated before candidate timeout
  pending transfer retains TRANSFER_UNCONFIRMED precedence
  every activation, rejection, and terminal transition is applied exactly once

StoreHomeTask shared object-graph wiring:
  assertSame the StoreHomeExecutionState used by the root facade,
    StoreHomeTaskLifecycleController, and StoreHomeTaskView
  assertSame the StoreHomeTimeoutLifecycle used by the root facade,
    lifecycle controller, and timeout collaborators

Command lifecycle identity integration:
  assertSame(submittedRootTask, terminationObservation.task())
  verify FabricChatClefStoreHomeResultProjector reads the typed outcome from
    that observation task
```

`StoreHomeTaskAssembly` does not create or store the root Task. The controller
and view do not own a Task back-reference, and the projector reads
`terminationObservation.task()`. Therefore a fictional
`lifecycleController.task()`/`resultProjector.task()` assertion is prohibited;
it would require production back-references solely to satisfy a test and would
weaken the thin-facade ownership boundary.

Checkpoint closure required:

```text
registered-command exact set is 22 across the Java snapshot, support matrix,
  production Python registry, and expected-count assertion
deposit_all remains raw-only with empty allowed sources and every Korean/parser/
  admission/bridge/gameplay/public readiness axis false
StoreHomeTaskLifecycleController decision-order coverage passes
StoreHomeTask shared-state/shared-timeout identity wiring coverage passes
submitted-root/termination-observation/projector identity integration passes
focused suite is rerun without deselection or retry and has 0 failures
all broader Phase 0, offline CI, artifact-provenance, and live-safety gates above
  remain in force
```

## 2026-08-29 Final Focused Registry Verification

This addendum supersedes only the earlier 2026-08-29 checkpoint-status values of
21 production registry names and 36 passed / 1 failed. It does not rewrite the
historical failing runs or the broader Phase 0, CI, live-safety, and STORE_HOME
direct Java coverage gates.

```text
registered-command snapshot: 22
support-matrix rows:          22
production registry names:   22
expected-count assertion:    22
exact command-name sets:      equal

deposit_all production spec:
  raw-only shadow metadata
  slot_schema=("items?",)
  resolver_domain=command_specific
  lifecycle_kind=task
  safety_tier=R2
  confirmation_mode=none
  allowed_input_sources=()
  source_registered=true
  Korean/parser/admission/bridge/gameplay/public readiness=false

focused exact selection:
  passed: 38
  subtests passed: 179
  failed: 0
  deselection: none
  automatic retry: none
```

The dedicated production-registry test verifies the exact metadata and keeps
`deposit_all` absent from public Korean enablement. The snapshot and support
matrix remain on their existing schemas and continue to own command identity
and support classification rather than slot metadata.

No runtime-manifest provenance or runtime behavior follows from this focused
result. Runtime manifest Git/source/build-input/runtime SHA provenance remains
`UNVERIFIED`, and `artifactParity=PARITY_UNPROVEN` remains unchanged. This
addendum records no commit or push.

## 2026-09-04 H5 Korean Chat/microphone test extension

The 22-row result above remains dated verification for its exact source and
test baseline. At the exact H5 activation source baseline recorded in the
[Python Korean Command Registry Plan](chatclef-python-korean-command-registry-plan.md#2026-09-04-h5-registrar-current-source-delta),
`AutoDepositTrustedCommandRegistrar` declares and attempts four H5 names.
Collision-free normal activation therefore targets 26 unique names. If any
English trio name collides, the registrar refuses the whole English trio and
does not attempt the Korean alias. If only the Korean alias collides after the
English trio succeeds, the three English names remain registered and only the
alias is refused. Actual runtime effective ownership remains a separate
observation:

```text
auto_deposit_trust
auto_deposit_untrust
auto_deposit_trusted_list
자동보관등록
```

Before implementation, the production Python registry, registered-command
snapshot and support matrix remained at 22. The 2026-09-05 implementation now
keeps all three at 26; the earlier passing result remains dated 22-row evidence.

The catalog test must add the actual H5 registrar activation source to its
source extraction and prove all of the following:

```text
source-declared collision-free activation target: 26
snapshot rows:                            26
support-matrix rows:                      26
production registry names:               26
exact sets:                               equal
duplicate command names:                 0
H5 owner/class/registration paths:        exact
existing registrar no-collision branch:  preserved and directly tested
existing Korean-alias collision branch:  preserved and directly tested
any-English-trio collision branch:        trust/untrust/list each run as a parameterized registrar subcase
```

Only `auto_deposit_trust` is a candidate for the new public natural-language
feature. `auto_deposit_untrust`, `auto_deposit_trusted_list` and
`자동보관등록` remain raw-only in this slice.

The former input path collapsed local Chat, VoiceInput and other loaded `Input`
providers to raw strings before the Minecraft router. The implementation now
uses a frozen envelope that preserves
`source/provider_id/event_kind/final/event_id`, routing text and an opaque
unchanged `fallback_payload` from ingress through queue and router. H5 request
creation copies the trusted source. H5 admits only these exact tuples:

```text
lavi_chat_ui / lavi_chat_ui / chat_submit / true
voice_input_final / VoiceInput / final_transcript / true
```

Twitch, YouTube, IdleThink, ScreenVision, StarCraft, unknown providers, raw
legacy strings and partial transcripts must produce handled H5 source rejection
with zero translation, submission and LLM calls. The Minecraft router remains
installed with a nullable extension dependency, so extension-unavailable H5
input fails closed while unrelated text still follows the normal LLM path.
Router import/construction failure must surface as startup/composition failure;
it must not be swallowed into a routerless LLM fallback state.

The feature-specific offline test matrix must prove:

```text
positive Korean Chat and microphone-style final transcripts
exact prefixless shorthand forms without @
trusted exact @auto_deposit_trust area 16x16 and @auto_deposit_trust 반경 16x16
  -> preserve original text and adapt once to the prefixless canonical input
the same forms with an untrusted/invalid source tuple
  -> auto_deposit_trust_input_source_not_allowed or auto_deposit_trust_input_provenance_invalid,
     zero claim/translation/submit/LLM call
every other trusted @ form, suffix or injected slot
  -> dangerous_command_slot handled rejection, zero submit, zero LLM call
fixed-size normalization for 16x16 / 16 X 16 / 16×16 / 16 곱하기 16
fullwidth/circled/NFKC compatibility forms, Unicode Zs spacing and shorthand-internal punctuation
  -> H5 guarded rejection, zero submit, zero LLM fallback
rule-only AUTO_DEPOSIT_TRUST_AREA with zero user-controlled slots
guarded decisions use UNKNOWN + exact rule_auto_deposit_trust_guard slots/reason codes
malformed/contradictory guard payload -> typed rejection, zero submit, zero LLM call
one exact compiler output: auto_deposit_trust area 16x16
LLM authority denied before and after payload validation
H5 candidate priority over STORE_HOME and DEPOSIT_ITEM
negation/question/hypothetical/deferred/compound rejection
8x8 / 16x8 / 32x32 / missing size / extra-number rejection
guarded candidate handled without LLM fallback
candidate detector is total/no-throw for string fixtures
final classifier exception -> translation_internal_error handled rejection, zero submission, zero LLM call
any H5 route-local exception after candidate identity -> handled failure, zero LLM fallback
raw CR/LF/control/non-ASCII-Zs scan precedes strip and normalization for exact and natural-language H5 input
  -> auto_deposit_trust_input_raw_control_not_allowed, zero claim/translation/submit/LLM call
one admitted immutable ingress event (source/provider_id/event_kind/final/event_id)
  -> one process-lifetime event claim -> at most one translation -> at most one submit attempt
event_id accepts only an adapter-generated built-in string matching [0-9a-f]{32};
  missing/empty/whitespace/padded/uppercase/wrong-length/non-hex/control/non-string values
  fail before claim with zero translation/submission
same event_id redelivery/concurrent claim -> zero additional translation or submission
descriptor-bound policy overwrites/ignores provider-supplied source/provider_id/event_kind/final/event_id fields
non-H5 ScreenVision dict fallback preserves the original object and all nested fields,
  including remember_history=false and observation-memory semantics
claim issues a non-serializable event/source/registry-bound receipt;
  admission inspection is non-consuming, request construction never receives the receipt,
  and only one atomic revalidation/consumption may enter H5 command submission
extension composition, router, coordinator and authorizer share the exact same process-lifetime
  claim registry object; no static/global or second submission-capable registry exists
request-factory failure abandons the receipt and submits zero
concurrent post-inspection commits permit at most one submitter entry
commit failure submits zero; submitter exception/UNKNOWN leaves the receipt spent with zero retry
every no-submit exit after claim abandons ISSUED -> SPENT exactly once;
  router/coordinator finally safety nets are idempotent, leave no live ISSUED receipt,
  and same-event redelivery remains claim/translation/submit zero
direct handle_natural_language_command forged source/metadata -> H5 submit zero
direct submit_translated_command forged source/metadata/executable translation -> H5 submit zero
missing/forged/mismatched/spent receipt and Fabric Korean UI -> H5 submit zero
Fabric Korean UI source lavi_gui_korean remains admitted only for commands that already allow it;
  STORE_HOME and H5 do not gain that source
existing public commands preserve local Chat/VoiceInput/direct/Fabric-UI cases but reject
  Twitch/YouTube/IdleThink/ScreenVision/StarCraft/unknown command candidates after generic enforcement
parser-ready, Python-admission-ready and public memberships are source-independent;
  staged/rollback fixtures keep parse/admission true + public false with submit zero,
  while current production metadata is parse/admission/bridge/public true
pre-existing busy/disconnected/reconciliation/quarantine -> current event submit zero
current submit outcome UNKNOWN -> one attempt, zero retry/replay
post-UNKNOWN later event -> submit zero until matching reconciliation
local Python accepted submission result says submitted, not completed or count-confirmed;
  it is not a Java wire command_result
direct Chat displays that bounded ACK; queued microphone ACK/TTS delivery remains NOT_IMPLEMENTED
existing GET/DEPOSIT/STORE_HOME/control regressions remain green
```

The raw-safety collaborator scans the immutable raw text for CR/LF/TAB/control
characters after source/event-ID admission and before claim or outer ASCII-space
trim. After that scan passes and the event is claimed once, the dedicated exact-input
adapter handles the two trusted whole-string `@` forms, preserves `original_text`,
and passes only the prefixless canonical text to the unchanged dangerous-text/translation service. Every other literal `@` form
remains rejected; the common dangerous-text rule is not weakened. Raw Minecraft
command tests continue to own the four direct-command forms separately.

Existing H5 command, generic callback-lifecycle and result-fidelity tests remain
supporting evidence. The new focused Java integration test now exercises exact
H5 normalized dispatch, its completion callback, matching-request terminal send
and queue retirement in one fixture, and the required clean forced build passed.
Therefore `BRIDGE_LIFECYCLE_READY=true` for this route. This test-only addition
does not change Java production behavior and does not prove registry mutation
success. It separates exact terminal branches:
`NO_ROOT_VISIBLE` produces `completed / callback_completed_without_user_task /
callback_without_user_task`, while `PREEXISTING_UNCHANGED_IDLE_ROOT` produces
`unknown / finish_callback_without_new_command_owned_root /
callback_without_matching_user_task_event`. Both require matching request ID,
one terminal send and retirement only after successful asynchronous terminal-send
completion with `outcome.succeeded()=true`.
The pre-existing-idle fixture additionally requires synchronous callback
observation before dispatch return, no `taskFinishedObservation`, and
`preexistingIdleRootStabilityQualified=true`; otherwise it has not reached that
classifier branch.

Registrar verification is covered by the focused Java test. The existing
`AutoDepositTrustedCommandRegistrarTest` no-collision and Korean-alias collision
cases remain, and the added parameterized trust/untrust/list subcases prove that
any English-trio collision refuses the whole trio and skips the Korean alias.
This is the one existing-registrar test hunk in the Java test-only scope; Java
production source remains unchanged.

Offline microphone fixtures prove only typed final-transcript semantic parity,
not physical microphone wiring or provider duplicate suppression. A physical
utterance producing exactly one provider callback remains `INCONCLUSIVE` until
live observation; only duplicate delivery of the same `event_id` is deterministically
suppressed offline. Live
acceptance follows the operator-authorized staged public rollout in the feature
contract and requires separate Chat and real microphone final-transcript runs
with one request and normalized Java dispatch each. A second run
at the same player range may correctly produce `NO_CHANGE`; use controlled
fixtures rather than classifying that as microphone failure. Generic bridge
completion is not an H5 mutation oracle. The current human-readable H5 log may
support only isolated temporal/run-scoped evidence because it has no matching
request/session/generation/correlation fields. Exact request-to-mutation binding
is `INCONCLUSIVE`, and the log must not be parsed into a public Python
success/count response.

The normative grammar, ownership, registry metadata, ACK limit and full
feature matrix are defined in
[H5 Auto-Deposit Trust Korean Chat/Microphone Pre-Change Contract](chatclef-h5-auto-deposit-trust-korean-chat-microphone-pre-change-contract-2026-09-04.md).
The 2026-09-04 addendum itself was documentation-only. The 2026-09-05 follow-up
implemented the Python route, passed offline tests and the clean Java build, and
left deployment, Minecraft/live Chat/microphone runtime, commit and push
unperformed.
