<!-- 20260815_kpopmodder: Documented v2 Korean item alias expansion across get/equip/deposit/give before implementation. -->
<!-- 20260815_chatgpt: Synchronized design authority, reviewed coverage provenance, and Phase 0 gates with the test strategy and merge-blocker documents. -->
<!-- 20260818_kpopmodder: Linked the post-restore live lifecycle and fail-closed preflight status without expanding item-action scope. -->

# ChatClef Korean Item Action Alias V2 Plan

Date: 2026-08-15

This document records the documentation-only v2 design for expanding Korean
Minecraft item names across ChatClef item commands:

```text
get
equip
deposit
give
```

It is documentation only. It does not approve Python behavior changes, Java
changes, wire protocol changes, DTO changes, ChatClef / AltoClef engine
changes, build execution, Minecraft launch, runtime reproduction, commit,
push, broad refactoring, or automatic command replay.

## Source Materials

This document summarizes the ChatGPT-provided design bundle supplied by the
user:

```text
C:\Users\jaewo\Downloads\IMPLEMENTATION_TEST_PLAN.md
C:\Users\jaewo\Downloads\korean_item_aliases.v2.coverage.json
C:\Users\jaewo\Downloads\chatclef_item_command_target_policy.draft.json
C:\Users\jaewo\Downloads\korean_item_aliases.default_policy.draft.json
C:\Users\jaewo\Downloads\korean_item_aliases.v2.draft.json
C:\Users\jaewo\Downloads\korean_item_aliases.v2.validation.json
```

The files above are reference material. They are not automatically trusted as
runtime configuration, and this document does not copy them into the runtime
resource directory.

A later ChatGPT conditional review approved the responsibility boundaries in
this document, but did not approve immediate full implementation. That review
requires an additional contract-freeze phase before expanding beyond the
current `GET_ITEM` behavior.

## Related Documents And Authority

Read and commit this plan together with:

```text
plugins/Minecraft/docs/chatclef-korean-test-strategy.md
plugins/Minecraft/docs/chatclef-korean-post-review-merge-blockers.md
plugins/Minecraft/docs/chatclef-korean-item-command-resolution-analysis.md
plugins/Minecraft/docs/chatclef-command-lifecycle-and-threading.md
plugins/Minecraft/docs/fabric-chatclef-bridge-protocol-v1.md
plugins/Minecraft/docs/fabric-chatclef-live-runtime-preflight-plan.md
plugins/Minecraft/docs/fabric-chatclef-live-runtime-process-lifecycle-plan.md
```

Authority is split as follows:

```text
this plan:
  Korean item/action UX design, command-specific policies, and phase order

test strategy:
  test evidence, artifact schemas, source/hash authority, coverage algorithm,
  catalog parser provenance, CI scope, and live-test safety

post-review merge blockers:
  current pass/fail status and the implementation work required before merge

live-runtime preflight plan:
  exact one-shot approval, endpoint/process/world gates, submission uncertainty,
  and PreflightDecision/LiveRunObservation structure

live-runtime process lifecycle index:
  fixed audit snapshots, ownership runbooks, and cross-document navigation
```

When wording conflicts, the test strategy controls test/provenance/coverage
contracts, the preflight plan controls live-run admission and result structure,
and the post-review document controls current merge status. The three Korean
ChatClef documents remain one core documentation unit; the linked live-runtime
documents must be updated in the same docs-only change whenever their shared
live-test contract changes.

The 2026-08-18 post-restore audit does not change item/action UX policy or phase
order. Live-test implementation status, exact pass semantics, approval,
preflight, no-replay policy, terminal/runtime-completion terminology, and
gameplay-effect oracles are intentionally not duplicated here. The test
strategy, post-review merge blockers, and live-runtime preflight plan own those
contracts against audited implementation baseline `4239c23` and must be
re-audited before a later source/test commit is described as current.

## Goal

The user-facing goal is broad item coverage:

```text
If the user says a Minecraft item name in Korean, LAVI should resolve it to a
ChatClef item target whenever ChatClef can actually accept that target.
```

The implementation target is narrower and safer:

```text
Korean natural language
  -> command intent and slots
  -> shared concrete item lexicon
  -> command-specific target policy
  -> validated resolved action
  -> prefixless ChatClef DSL
```

The Python output contract remains prefixless:

```text
get diamond 10
equip iron_chestplate
deposit diamond 2
give Steve diamond 3
```

Python must not emit:

```text
@get diamond 10
@equip iron_chestplate
@deposit diamond 2
@give Steve diamond 3
```

The Java dispatcher can add the current ChatClef command prefix when needed.

## Runtime Lexicon Boundary

`korean_item_aliases.json` should remain a flat runtime map:

```text
plugins/Minecraft/fabric/chatclef/intent/resources/korean_item_aliases.json
```

Its sole responsibility is:

```text
Korean exact-compact item phrase
  -> one concrete ChatClef catalog target
```

Examples:

```json
{
  "다이아몬드": "diamond",
  "철": "iron_ingot",
  "철 주괴": "iron_ingot",
  "철 흉갑": "iron_chestplate",
  "벽돌": "bricks",
  "벽돌 아이템": "brick"
}
```

Unqualified cooked/raw food defaults are not frozen by this example. Food
entries such as `소고기 -> cooked_beef` remain proposed default-policy entries
until the UX policy below explicitly freezes them.

The runtime item alias file must not contain command modes, command shortcuts,
player names, quantities, or raw ChatClef DSL.

Do not put these into the shared item lexicon:

```text
잡템 -> all
갑옷 -> armor_set
철 갑옷 -> iron
Steve에게 -> Steve
```

Those are command-specific context rules.

## Shared Alias Consumers

The same concrete item lexicon can be reused by multiple commands:

| Command | Shared alias usage | Extra policy needed |
| --- | --- | --- |
| `get` | concrete resource target | quantity and optional item-list parsing |
| `equip` | explicit equippable target | armor-set shortcut policy |
| `deposit` | specific item target | bare deposit mode and quantity policy |
| `give` | single item target | recipient slot and player-name validation |

Examples below separate reviewed runtime alias availability from
command-specific behavior. Rows explicitly marked planned or pending must not be
treated as verified runtime behavior until the relevant Phase 0 capability
verification is complete.

| Korean phrase | Shared target | Runtime alias status | Command-specific status |
| --- | --- | --- | --- |
| `다이아몬드` | `diamond` | present in the reviewed `cfc170a` runtime alias baseline | preserve as a current GET regression |
| `철` | `iron_ingot` | present in the reviewed `cfc170a` runtime alias baseline | preserve as a current GET shorthand; `equip iron` is allowed only in material + armor-set context such as `철 갑옷` |
| `철 흉갑` | `iron_chestplate` | planned v2 alias | explicit EQUIP target after equipment capability verification |
| `잡템` | none | must not be stored in the flat item alias map | deposit context may compile bare `deposit` after UX wording policy is frozen |

## Command-Specific Policy

### Get

Natural-language examples:

```text
다이아몬드 10개 가져와줘 -> get diamond 10
돌 64개 가져와줘 -> get stone 64
원목 20개와 돌 10개 가져와줘 -> get [log 20, stone 10]
```

Post-implementation note: the reviewed implementation has source and tests for
GET mining/acquisition phrases such as `캐줘`, `캐와`, and `채굴해줘`. Treat
those as reviewed GET regression coverage for the branch under review. Future
GET/EQUIP/DEPOSIT/GIVE action precedence remains a separate phase.

The existing `GET_ITEM` intent can remain the base for single-item get. Multi
item support requires explicit item-request list parsing and compiler support.

After a `get` command is accepted, automatic replay remains forbidden because
the current Java command can treat repeated `get item count` as an additional
target instead of exact remaining quantity.

### Equip

Java `EquipCommand` treats these single tokens as full armor-set shortcuts:

```text
leather
iron
gold
diamond
netherite
```

Important distinction:

```text
철 -> iron_ingot
철 갑옷 -> equip iron
철 흉갑 -> equip iron_chestplate
금 -> gold_ingot
금 갑옷 -> equip gold
금 도끼 -> golden_axe
```

The values `iron`, `gold`, `diamond`, `leather`, and `netherite` are equip
shortcut tokens in equip context. They are not general item alias targets.

Initial explicit equip allowlist should be conservative:

```text
*_helmet
*_chestplate
*_leggings
*_boots
```

The Java command checks whether resolved targets are actually equipment, so a
Python-side capability check should prevent obvious invalid submissions such as:

```text
돌 입어줘
```

### Deposit

Natural-language examples:

```text
잡템 상자에 넣어줘 -> deposit
다이아몬드 2개 상자에 넣어줘 -> deposit diamond 2
돌 32개 보관해줘 -> deposit stone 32
```

Bare Java `deposit` is broad. Source inspection shows that the zero-argument
path builds targets through `getAllNonEquippedOrToolItemsAsTarget`, excluding
`PlayerSlot.ARMOR_SLOTS` and `ToolItem` stacks. Spare armor that is merely in a
normal inventory slot is therefore not the same category as equipped armor.
Bare `deposit` can include useful items such as food, torches, fuel, ingots,
and blocks.
Therefore `잡템` must be a deposit-mode policy, not a fake item alias.

Initial unresolved quantity policy:

```text
다이아몬드 상자에 넣어줘 -> ambiguous until policy is chosen
다이아몬드 전부 넣어줘 -> do not invent a count without inventory evidence
```

### Give

Natural-language examples after Phase 0 recipient and player-name policy are
verified:

```text
다이아몬드 3개 Steve에게 줘 -> give Steve diamond 3
Steve한테 다이아몬드 3개 줘 -> give Steve diamond 3
다이아몬드 Steve에게 줘 -> give Steve diamond 1
```

The recipient marker and give verb should both be required:

```text
<player>에게 + 줘
<player>한테 + 줘
```

The sentence below must not be classified as give:

```text
다이아몬드 줘
```

It lacks a recipient and can overlap with ordinary get/request phrasing.

## Default Policy For Ambiguous Names

The v2 direction is to prefer broad user coverage. Ambiguous names should not
always be excluded. Instead, a default policy may choose a common default and
add explicit qualified aliases.

Examples from the draft policy:

```json
{
  "벽돌": {
    "decision": "default",
    "default_target": "bricks",
    "explicit_aliases": {
      "벽돌 아이템": "brick",
      "벽돌 블록": "bricks"
    }
  },
  "석영": {
    "decision": "default",
    "default_target": "quartz",
    "explicit_aliases": {
      "네더 석영": "quartz",
      "석영 블록": "quartz_block"
    }
  },
  "소고기": {
    "decision": "default",
    "decision_status": "proposed_not_frozen",
    "default_target": "cooked_beef",
    "explicit_aliases": {
      "생소고기": "beef",
      "익히지 않은 소고기": "beef",
      "구운 소고기": "cooked_beef",
      "익힌 소고기": "cooked_beef",
      "스테이크": "cooked_beef"
    }
  }
}
```

Contextual entries are not flattened into the runtime item alias map:

```json
{
  "갑옷": {
    "decision": "contextual",
    "contexts": {
      "equip_with_material": "full_armor_set_shortcut"
    }
  },
  "잡템": {
    "decision": "contextual",
    "contexts": {
      "deposit": "bare_deposit"
    }
  }
}
```

Review entries require a user policy decision before runtime inclusion:

```text
목재
닭고기
돼지고기
양고기
토끼고기
대구
연어
```

## Historical Runtime Alias Baseline Snapshot

This snapshot describes the older committed Git blob at
`c912ff6491711c33dbf2e66320e634196f866ae8`, not the reviewed runtime alias
file and not the proposed v2 draft seed.

Hash authority for this documentation pass:

```text
SHA-256 over Git blob bytes at the documented baseline commit.
Do not hash CRLF-converted working-tree bytes.
Do not mix LF/CRLF-normalized hashes with Git-blob hashes in one snapshot.
```

```text
snapshot_kind: runtime_baseline
snapshot_authority: git_blob_bytes
snapshot_baseline_commit: c912ff6491711c33dbf2e66320e634196f866ae8
coverage_algorithm_version: 1
catalog_parser_source_commit: cfc170ac024a46ea943bffcaf289a91ff6bc5ee7
catalog_parser_source_path: plugins/Minecraft/fabric/chatclef/intent/chatclef_target_catalog.py
catalog_parser_source_sha256: bca08b987851fb9a27f0dcb06cd95d05c264fd0d276540d0bbd5cfd9d1e50893
catalog_path: plugins/Minecraft/runtime/chatclef_fabric_1.20.1/CataloguedResources.txt
catalog_sha256: 4591ae83151dd2643943dfdcbb2e89a53d26de393c583902370c3f4d3c57188a
alias_source_path: plugins/Minecraft/fabric/chatclef/intent/resources/korean_item_aliases.json
alias_source_sha256: fd27685fa50d3364add9d9df5a0675c52c5b13ff862c07ef429d5e67055b45fd
catalog_target_count: 591
alias_count: 11
covered_target_count: 4
missing_target_count: 587
coverage_percent: 0.68
catalog_outside_targets: 0
compact_target_conflicts: 0
```

## Reviewed Runtime Alias Baseline Snapshot

This snapshot describes the reviewed runtime alias file at
`cfc170ac024a46ea943bffcaf289a91ff6bc5ee7`. It is a reviewed baseline, not a
future moving HEAD snapshot:

```text
snapshot_kind: runtime_reviewed_baseline
snapshot_authority: git_blob_bytes
reviewed_source_commit: cfc170ac024a46ea943bffcaf289a91ff6bc5ee7
coverage_algorithm_version: 1
catalog_parser_source_commit: cfc170ac024a46ea943bffcaf289a91ff6bc5ee7
catalog_parser_source_path: plugins/Minecraft/fabric/chatclef/intent/chatclef_target_catalog.py
catalog_parser_source_sha256: bca08b987851fb9a27f0dcb06cd95d05c264fd0d276540d0bbd5cfd9d1e50893
catalog_path: plugins/Minecraft/runtime/chatclef_fabric_1.20.1/CataloguedResources.txt
catalog_sha256: 4591ae83151dd2643943dfdcbb2e89a53d26de393c583902370c3f4d3c57188a
alias_source_path: plugins/Minecraft/fabric/chatclef/intent/resources/korean_item_aliases.json
alias_source_sha256: 9dcf8b8fde97ab60abeceaee7ce0938eda030e80689f1d58d04d5e797b5896df
catalog_target_count: 591
alias_count: 19
covered_target_count: 9
missing_target_count: 582
coverage_percent: 1.52
catalog_outside_targets: 0
compact_target_conflicts: 0
```

Do not call both the historical baseline and reviewed baseline
`runtime_current`. A true current snapshot must be recomputed from HEAD.

Both snapshots use coverage algorithm version `1` and the reviewed catalog
parser contract at `cfc170a` to interpret their catalog and alias blobs. The
historical artifact still reads the catalog and alias blobs from `c912ff...`;
recording the reviewed parser provenance does not claim that the same parser
implementation existed in the historical commit.

## Proposed V2 Draft Snapshot

This is the proposed seed bundle, not the runtime resource. Source hashes below
are recorded to prevent stale draft, policy, generator, validation, and coverage
reports from being mixed together.

```text
draft_alias_source_path: C:\Users\jaewo\Downloads\korean_item_aliases.v2.draft.json
draft_alias_source_sha256: 8b3541bba76c13ef7f1c35eb7cccb043d22c8ae3deac7a94482ff83dfd5f42f0
default_policy_source_path: C:\Users\jaewo\Downloads\korean_item_aliases.default_policy.draft.json
default_policy_source_sha256: e5f85cfef338138b3e99a7e2e86e27f7e378132bbca217df6c6f256ef78f311f
target_policy_source_path: C:\Users\jaewo\Downloads\chatclef_item_command_target_policy.draft.json
target_policy_source_sha256: 595da76bdb23274c41de170ad662b85d21d98b42da2ee444eb74563055de61f7
canonicalization_policy_source_path: <not supplied in the reviewed draft bundle; required before Phase 1 implementation>
canonicalization_policy_source_sha256: <fill when the repository-owned canonicalization source is committed>
validation_report_path: C:\Users\jaewo\Downloads\korean_item_aliases.v2.validation.json
validation_report_sha256: 4f28210bbd559bea6047c545a6a28cf05b83c17e7f8b916064234ff468e760e6
coverage_report_path: C:\Users\jaewo\Downloads\korean_item_aliases.v2.coverage.json
coverage_report_sha256: ba7f8960ec423db86a80fcd283cb46278c86e2b981c0512ffd9f9359395f919d
generator_path: C:\Users\jaewo\Downloads\generate_korean_item_aliases.py
generator_sha256: f19ce90755c6be3fceafe7e322dc25012b09d6c53e700ad3c4a4b6904b376c3f
generator_schema_version: 2
coverage_algorithm_version: 1
catalog_parser_source_commit: cfc170ac024a46ea943bffcaf289a91ff6bc5ee7
catalog_parser_source_path: plugins/Minecraft/fabric/chatclef/intent/chatclef_target_catalog.py
catalog_parser_source_sha256: bca08b987851fb9a27f0dcb06cd95d05c264fd0d276540d0bbd5cfd9d1e50893
```

The `C:\Users\jaewo\Downloads\...` paths above are historical provenance for
the reviewed draft bundle only. Phase 1 generator work must define
repository-owned source paths before CI or deterministic generation relies on
them.

Recommended Phase 1 repository-owned source paths:

```text
plugins/Minecraft/tools/chatclef_aliases/sources/korean_item_aliases.curated.json
plugins/Minecraft/tools/chatclef_aliases/sources/korean_item_aliases.default_policy.json
plugins/Minecraft/tools/chatclef_aliases/sources/chatclef_target_canonicalization.json
plugins/Minecraft/tools/chatclef_aliases/sources/chatclef_item_command_target_policy.json
```

After those files exist, the generator and CI should use repository-owned
sources as authority. Download paths may remain in reports only as historical
input provenance.

The supplied v2 validation reports:

```text
schema_version: 2
status: valid_draft
catalog_target_count: 591
base_alias_count: 174
default_policy_flat_alias_count: 23
merged_alias_count: 191
merged_target_count: 167
exact_alias_conflicts: 0
compact_target_conflicts: 0
redundant_compact_aliases: 0
targets_missing_from_catalog: 0
policy_targets_missing_from_catalog: 0
```

The supplied coverage report is not full coverage:

```text
status: draft_seed_not_full_coverage
catalog_target_count: 591
alias_count: 191
covered_target_count: 167
missing_target_count: 424
coverage_percent: 28.26
```

Therefore the v2 draft is a validated seed, not the final "all items" alias
set.

## Conditional Review Result

The v2 direction is conditionally approved as a phased Python-only design.
These boundaries should remain unchanged:

```text
korean_item_aliases.json
  -> flat concrete Korean item lexicon only

command-specific policies
  -> equip shortcuts, deposit modes, give recipients, quantities, and routing

Python compiler
  -> prefixless ChatClef DSL only

Java / DTO / payload / ChatClef engine
  -> unchanged for this plan
```

The approval is not a green light to implement all of `get`, `equip`,
`deposit`, and `give` immediately. The following work can start after local
code changes are approved:

| Area | Status |
| --- | --- |
| alias generator / validator / compact index | approved to implement |
| existing GET alias expansion | approved after focused regression tests |
| EQUIP natural-language support | requires capability matrix first |
| DEPOSIT natural-language support | requires deposit quantity and bare-mode policy first |
| GIVE natural-language support | requires recipient grammar and player validation first |
| full 591 target coverage claim | not approved; draft is seed coverage only |

## Phase 0 Contract Freeze Required

Before implementing the broader action set, freeze these contracts in
documentation and tests.

### Java Command Grammar

Confirm the accepted ChatClef grammar for each command:

| Command | Contract to verify |
| --- | --- |
| `get` | single versus multi-item syntax, count omission, target shape |
| `equip` | full-set shortcut tokens, explicit equipment target support |
| `deposit` | bare deposit, specific item syntax, count omission, multi-item support |
| `give` | recipient position, player-name syntax, single versus multi-item support |
| dispatcher | exact boundary where a prefix is added to prefixless Python DSL |

Do not let Python emit a command form merely because it is convenient to
generate. The compiler must emit only verified Java-compatible DSL.

Use square brackets for optional arguments in this document:

```text
get <item> [count]
get [<item> [count], ...]
deposit
deposit <item> [count]
give <item> [count]
give <username> <item> <count>
```

Current Java `GiveCommand` grammar has two modes:

```text
Butler/current-user mode:
  give <item> [count]

explicit recipient mode:
  give <username> <item> <count>
```

Do not document Java explicit-recipient mode as
`give <username> <item> [count]`. With the current `ArgParser` default
thresholds, `give Steve diamond` is not a valid explicit-recipient shorthand.
Python may still canonicalize Korean recipient phrases to `give Steve diamond
1` because that emits the required explicit count.

Java accepted grammar and Python canonical serialization are separate
contracts. Current GET output and planned Phase outputs should be documented
separately:

```text
Current GET:
  always emit an explicit count, including default one-count requests
  다이아몬드 가져와줘 -> get diamond 1

Planned Phase 6 GIVE:
  always emit an explicit count, including default one-count requests
  Steve에게 다이아몬드 줘 -> give Steve diamond 1

Planned Phase 5 specific DEPOSIT:
  emit an explicit count only after Korean quantity policy resolves the count
  다이아몬드 2개 넣어줘 -> deposit diamond 2

Planned Phase 5 bare DEPOSIT:
  emit exactly deposit

Planned Phase 7 multi-item GET:
  use brackets, separate entries with comma-space, emit every count explicitly,
  merge duplicate canonical targets by summing counts, and keep the first
  occurrence position for the merged target
```

### Phase 0 Verification Matrix

This table records the current source-level contract evidence and the planned
test assertion for each row. Phase 0 is still not complete until these planned
tests are implemented, pass, and no unresolved contract remains.

| Contract item | Source-verified result | Evidence source | Python policy | Contract test |
| --- | --- | --- | --- | --- |
| `get` item syntax | `get <item> [count]` parses through `ItemList`; omitted count defaults to `1` | `GetCommand.java`; `ItemList.java` | single Korean GET may compile `get target count` | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_get_single_item_grammar_contract`; assert `get diamond` defaults to count `1` and `get diamond 3` preserves count `3` |
| `get` quantity semantics | `count` is an additional requested amount for the command, not an idempotent ensure-total target | `GetCommand.java`; `AgentCommandUtils.addPresentItemsToTargets`; command lifecycle docs | once a primary GET is accepted, do not automatically replay the same command to "top up" inventory | `tests/minecraft_chatclef/lifecycle/test_item_action_routing_status.py::test_accepted_get_is_not_replayed_for_quantity_recovery`; assert total submit count `== 1` and resubmit count `== 0` for accepted + completed, failed, cancelled, deadline exceeded, unknown, disconnect, and duplicate terminal result cases |
| `get` multi-item syntax | Java accepts `get [<target> [count], ...]`; each element is `<target>` or `<target> <count>`; omitted element count defaults to `1`; duplicate targets are summed through `items.put(item, items.getOrDefault(item, 0) + count)`; Java return order is not a Python output contract | `ItemList.java` | multi-item GET may later compile to a deterministic bracketed list; do not mix with single-item phases | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_get_item_list_grammar_contract`; assert Java grammar separately from Python canonical serialization, including `get [log 20, stone 10]`, `get [diamond 1, coal 5]`, `get [stone 2, stone 3] -> stone 5`, `get [stone, stone 3] -> stone 4`, empty-list rejection, and nested-bracket rejection |
| `equip` full-set shortcut | exactly `leather`, `iron`, `gold`, `diamond`, `netherite` | `EquipCommand.java` | only material + armor-set context may emit these shortcut tokens | `tests/minecraft_chatclef/command_catalog/test_item_command_capability_policy.py::test_equip_set_shortcuts_match_java_contract`; assert only those five shortcut tokens are accepted |
| `equip` explicit target | parses `ItemList`, then rejects any matched item that is not `Equipment` | `EquipCommand.java` | use a conservative explicit equipment allowlist before submission | `tests/minecraft_chatclef/command_catalog/test_item_command_capability_policy.py::test_explicit_equip_targets_require_equipment_capability`; assert armor items pass and `stone` rejects as `UNSUPPORTED_ACTION_TARGET` |
| `equip` multi-item exposure | Java can parse an `ItemList`, but Python v2 should not expose arbitrary multi-item equip initially | `EquipCommand.java`; `ItemList.java` | support armor-set shortcut or single explicit equipment only in initial EQUIP phase | `tests/minecraft_chatclef/command_catalog/test_item_command_capability_policy.py::test_python_v2_does_not_expose_multi_item_equip_initially`; assert multi explicit equip phrase is unsupported until separately approved |
| bare `deposit` | zero arguments produce `null` `ItemList`; command excludes `PlayerSlot.ARMOR_SLOTS` and `ToolItem` stacks, not every armor item by type | `DepositCommand.java`; `Arg.java`; `ArgParser.java` | only compile bare `deposit` for approved deposit-mode Korean phrases | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_bare_deposit_contract_matches_source_predicate`; assert the source-backed truth table exactly: equipped armor slot false, spare armor inventory true, `ToolItem` false, food true, torch true, fuel true, ingot true, ordinary block true |
| specific `deposit` count | `deposit <item> [count]` parses through `ItemList`; omitted count defaults to `1` | `DepositCommand.java`; `ItemList.java` | Java default is known, but Korean no-count wording remains a UX policy decision | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_deposit_java_default_count_contract_is_one`; assert Java contract is `1` while Korean no-count policy remains separately tested |
| `deposit` multi-item exposure | Java can parse bracketed `ItemList`, but Python v2 initial deposit should support bare or single specific item only | `DepositCommand.java`; `ItemList.java` | mark multi-item deposit as intentionally unsupported until a later explicit phase | `tests/minecraft_chatclef/command_catalog/test_item_command_capability_policy.py::test_python_v2_deposit_multi_item_is_intentionally_unsupported_initially`; assert `deposit [diamond 2, stone 10]` is not emitted by Korean v2 compiler |
| `give` Butler/current-user syntax | no explicit username uses `give <item> [count]`; omitted count defaults to `1` through Java defaulting | `GiveCommand.java`; `Arg.java`; `ArgParser.java` | Korean natural-language GIVE should not rely on Butler/current-user mode | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_give_butler_current_user_mode_contract`; assert `give diamond` and `give diamond 3` are Butler/current-user forms, not explicit-recipient forms |
| `give` explicit recipient syntax | explicit recipient uses `give <username> <item> <count>`; count is required for this mode | `GiveCommand.java`; `Arg.java`; `ArgParser.java` | Korean GIVE with recipient must compile a count-bearing command such as `give Steve diamond 1` or `give Steve diamond 3` | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_give_explicit_recipient_requires_count_contract`; assert `give Steve diamond 1` is valid explicit-recipient shape and `give Steve diamond` is not documented as explicit-recipient shorthand |
| `give` multi-item support | Java `GiveCommand` accepts a single `String item`, not an `ItemList` | `GiveCommand.java` | Python v2 GIVE supports one recipient plus one item only | `tests/minecraft_chatclef/command_catalog/test_item_command_capability_policy.py::test_give_multi_item_is_not_supported_by_v2_contract`; assert no Korean phrase compiles to `give Steve [diamond 2, stone 10]` |
| `give` no-recipient mode | no username can fall back to current Butler user, otherwise warns and stops | `GiveCommand.java` | Korean natural-language GIVE should require an explicit recipient and not rely on Butler context | `tests/minecraft_chatclef/lifecycle/test_item_action_routing_status.py::test_korean_give_requires_explicit_recipient`; assert `다이아몬드 줘` does not compile as GIVE |
| player name syntax policy | Java treats username as a string and performs loaded-player validation at runtime; `ArgParser` treats `#` as a comment delimiter | `GiveCommand.java`; `Arg.java`; `ArgParser.java` | Python rejects empty names, whitespace, line breaks, `#`, and DSL separators; Java owns actual loaded-player validation | `tests/minecraft_chatclef/lifecycle/test_item_action_routing_status.py::test_player_name_policy_is_minimal_syntax_only`; assert injection-shaped names, including `#` comment truncation attempts, reject and ordinary names pass to Java-owned validation |
| player loaded validation | runtime rejects if `EntityTracker.isPlayerLoaded(username)` is false | `GiveCommand.java` | Python may syntax-validate names, but runtime loaded-player validation remains Java-owned | `tests/minecraft_chatclef/lifecycle/test_item_action_routing_status.py::test_loaded_player_validation_is_not_claimed_by_python`; assert Python does not claim loaded-player success |
| prefix insertion | Fabric dispatcher trims command and prepends current ChatClef prefix only if missing | `FabricChatClefCommandDispatcher.java`; `CommandExecutor.java` | Python compiler remains prefixless | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_bridge_dispatcher_owns_prefix_insertion`; assert Python emits `get diamond 1`, not `@get diamond 1` |
| Java contract provenance | the strategy-owned `reviewed_source_commit`, Git-blob SHA-256 registry, activation chains, and HEAD-drift checks must match before Python contract fixtures are trusted | `chatclef-korean-test-strategy.md`; reviewed Java source registry | stop Phase 0 contract validation if reviewed provenance or HEAD drift fails | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract_provenance.py::test_java_contract_source_hashes_match_documented_baseline`; assert reviewed commit provenance and HEAD source hashes match the strategy-owned artifact |
| contextual policy non-flattening | contextual command policies are not concrete item aliases | `korean_item_aliases.json`; default/contextual policy source | keep `잡템`, `갑옷`, armor-set shortcuts, `bare_deposit`, and `full_armor_set_shortcut` out of the flat runtime alias map | `tests/minecraft_chatclef/alias_contract/test_korean_alias_generation_contract.py::test_contextual_policy_entries_are_not_flattened_into_item_aliases`; assert contextual keys and pseudo-policy values are absent from concrete runtime item entries |

Minimal Python player-name validation blocks only structure-breaking input.
Actual loaded-player existence remains Java-owned. Block at least:

```text
space
\t
\r
\n
#
@
;
,
[
]
"
'
\
```

Do not invent a narrow Minecraft username regular expression without a verified
Minecraft or Java parser contract; modded and offline environments may differ.

Phase 0 is complete only when all rows have:

```text
actual result
evidence source
Python policy
contract test
all planned contract tests implemented and passing
unresolved contract count == 0
```

### Per-Command Target Capability

Catalog membership and command capability are separate checks.

```text
Catalog membership:
  target is known by ChatClef

Command capability:
  target is valid for the requested command
```

Recommended policy surface:

```text
ChatClefItemCommandTargetPolicy
  supports_get(target)
  supports_equip_explicit(target)
  supports_deposit(target)
  supports_give(target)
  resolve_equip_set_shortcut(material)
```

Examples:

| Target | Get | Equip | Deposit | Give |
| --- | --- | --- | --- | --- |
| `diamond` | yes | only as armor-set shortcut context | verify | verify |
| `stone` | yes | no | verify | verify |
| `iron_ingot` | yes | no | verify | verify |
| `iron_chestplate` | yes | yes | verify | verify |
| `log` | yes | no | verify generic target behavior | verify generic target behavior |
| `planks` | yes | no | verify generic target behavior | verify generic target behavior |

Initial capability should prefer verified allowlists over dynamic guessing.

Do not verify every catalog target manually. Close the remaining deposit/give
capability gaps by representative target category:

| Category | Representative targets | Required contract question |
| --- | --- | --- |
| concrete direct item | `diamond`, `iron_ingot` | can deposit/give use the exact concrete target? |
| block | `stone` | can deposit/give use a block target without `get`-only behavior? |
| equipment | `iron_chestplate` | can deposit/give use an equipment item even when EQUIP has special capability rules? |
| generic aggregate target | `log`, `planks` | does the generic target mean the same thing outside GET? |
| canonicalized legacy target | e.g. legacy alias resolved to canonical target | is the canonical target emitted, never the legacy spelling? |
| invalid target | impossible or catalog-missing target | does policy reject before submission? |

A representative success proves only that representative contract. It must not
automatically allow every target in the same broad category. A target becomes
allowed only through one of these explicit policy mechanisms:

```text
exact target allowlist
verified target category rule
canonicalized concrete target rule
explicit unsupported rule for generic aggregate or invalid targets
```

Conservative initial policy:

```text
GET:
  catalog targets with canonicalization

EQUIP:
  closed full-set shortcuts and explicit equipment allowlist

DEPOSIT/GIVE:
  verified concrete targets only
  generic aggregate targets unsupported until representative behavior is proven
```

### Default Policy Decisions

Some defaults can be adopted as long as they are recorded as LAVI policy, not
as official Minecraft naming:

```text
벽돌       -> bricks
벽돌 블록  -> bricks
벽돌 아이템 -> brick
벽돌 조각  -> brick

석영       -> quartz
네더 석영  -> quartz
석영 블록  -> quartz_block

철         -> iron_ingot
금         -> gold_ingot
나무       -> log
네더라이트 -> netherite_ingot
```

These remain explicit user-policy decisions:

```text
소고기 / 닭고기 / 돼지고기 / 양고기 / 토끼고기 / 대구 / 연어
  -> unqualified cooked default or raw default

목재
  -> log, planks, or unresolved
```

If LAVI defaults unqualified food names to edible cooked variants, apply that
policy consistently across meat and fish aliases and record it in the source
policy JSON.

### Deposit Safety Policy

Bare Java `deposit` is broad. Do not treat these as equivalent:

```text
잡템 넣어줘
전부 넣어줘
인벤토리 비워줘
다이아몬드 넣어줘
```

Recommended initial policy:

| Korean input | Initial result |
| --- | --- |
| `잡템 상자에 넣어줘` | bare `deposit`, with documented broad behavior |
| `전부 상자에 넣어줘` | unsupported or clarification |
| `인벤토리 비워줘` | unsupported or clarification |
| `다이아몬드 2개 넣어줘` | `deposit diamond 2` |
| `다이아몬드 넣어줘` | ambiguous until no-count policy is frozen |

Source-backed bare deposit truth table:

| Category | Bare deposit target? | Source-backed contract |
| --- | --- | --- |
| equipped armor slot | no | `PlayerSlot.ARMOR_SLOTS` are excluded |
| spare armor in normal inventory | yes | not excluded merely by armor item type |
| `ToolItem` | no | `ToolItem` stacks are excluded |
| food | yes | not excluded by the bare predicate |
| torch | yes | not excluded by the bare predicate |
| fuel | yes | not excluded by the bare predicate |
| ingot | yes | not excluded by the bare predicate |
| ordinary block | yes | not excluded by the bare predicate |

Do not invent an arbitrary count such as `deposit diamond 9999` without an
inventory snapshot and an explicit policy.

### Routing Outcome Policy

Do not collapse all failures into ordinary LLM fall-through. Keep pure language
resolution separate from runtime submission state so parser and resolver code
does not need bridge connectivity knowledge.

Resolution status:

```text
UNKNOWN_LANGUAGE_INTENT
UNKNOWN_ITEM
AMBIGUOUS_ITEM
UNSUPPORTED_ACTION_TARGET
INVALID_INPUT
VALIDATED
```

Submission status:

```text
BRIDGE_DISCONNECTED
COMMAND_BUSY
ACCEPTED
REJECTED
```

Recommended behavior:

| Status | Behavior |
| --- | --- |
| `UNKNOWN_LANGUAGE_INTENT` | ordinary LLM fall-through |
| `UNKNOWN_ITEM` | fall-through or unknown-item response by policy |
| `AMBIGUOUS_ITEM` | clarification or explicit ambiguity |
| `UNSUPPORTED_ACTION_TARGET` | explicit Minecraft command rejection |
| `INVALID_INPUT` | consume and reject; no LLM fall-through |
| `BRIDGE_DISCONNECTED` | connection error |
| `COMMAND_BUSY` | busy response; no retry |
| `ACCEPTED` | command accepted by submission boundary |
| `REJECTED` | command rejected by submission boundary; no automatic retry |
| `VALIDATED` | eligible for submission exactly once if connected and idle |

`connected` means Python has an active Fabric WebSocket session accepted by the
bridge. It does not prove that a world is loaded, the player is alive, or the
ChatClef command executor is ready. Python must not infer success from
connection state alone; adapter or Java `ACCEPTED` / `REJECTED` outcomes remain
the authority for submission readiness.

Example:

```text
돌 입어줘
  -> EQUIP_ITEM + target=stone + unsupported action target
  -> "돌은 장착할 수 없어요."
```

This should not fall through to a general LLM response.

### Coverage Metric

Do not measure success as "every catalog target has a Korean alias." The
catalog can contain concrete vanilla items, generic targets, legacy synonyms,
internal aggregates, and targets users should not request directly.

Separate the coverage report into:

```text
catalog_total
direct_user_facing_targets
generic_policy_targets
canonicalized_legacy_targets
command_internal_targets
unresolved_user_facing_targets
```

Recommended final metric:

```text
canonical_user_facing_target_total
  = size(canonical_user_facing_targets)

canonical_user_facing_targets
  = canonicalize(direct_user_facing_targets union generic_policy_targets)
    minus canonicalize(command_internal_only_targets)

resolved_canonical_user_facing_targets
  = canonical_user_facing_targets intersect targets_resolvable_from_korean

user_facing_coverage
  = size(resolved_canonical_user_facing_targets)
    / canonical_user_facing_target_total
```

Legacy targets canonicalized into a covered target should be reported as
`legacy_target -> canonical_target` mappings. They are not subtracted again
after canonicalization and are not reported as missing aliases.

Classifier invariants:

```text
each raw catalog target has exactly one primary classification
all set operations above compare canonical target IDs, not mixed raw/canonical IDs
command_internal_only_targets means targets with no user-facing direct or
  generic-policy source
if a canonical target has at least one user-facing source, an internal alias to
  the same canonical target must not remove the whole canonical target
targets_resolvable_from_korean contains concrete canonical catalog targets only
command modes, shortcut tokens, recipients, quantities, and routing statuses
  never count as item coverage
```

### Normalization And Provenance

The alias generator and runtime resolver must share the same normalization
contract. Golden vectors should cover whitespace, punctuation, compact forms,
and NFKC variants.

Generation reports should record:

```text
catalog SHA-256
coverage algorithm version
catalog parser source commit, path, and SHA-256
ko_kr.json SHA-256
generator schema version
curated policy SHA-256
default policy SHA-256
canonicalization policy SHA-256
item-command target policy SHA-256
```

The production catalog parser also requires these baseline targets:

```text
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

A generated catalog or coverage artifact fails if any required baseline target
is missing, even when the total target count remains `591`.

Generated output should be deterministic. If `generated_at` exists, exclude it
from the byte-identical runtime JSON body or place it only in a report.

## Current Code Gap

At the time this plan was documented, the Python natural-language command path
already supports these intent families:

```text
GET_ITEM
FOOD
MEAT
GOTO
FOLLOW
IDLE
STOP
```

The v2 design requires additional Python intent and compiler support for:

```text
EQUIP_ITEM
DEPOSIT_ITEM
GIVE_ITEM
```

Java command implementations, bridge DTOs, v1 payload shape, and ChatClef /
AltoClef engine code do not need to change for this v2 plan. Python compiler
and resolver layers do need to expand because they currently cannot compile
natural-language equip/deposit/give actions.

## Proposed Python Components

Minimum new or expanded Python responsibilities:

```text
KoreanChatClefActionMatcher
  -> command family precedence and verb/span detection

ChatClefItemCommandTargetPolicy
  -> catalog, command capability, shortcut, and default-policy checks

KoreanEquipTargetResolver
  -> armor-set shortcut versus explicit equipment item resolution

ItemActionResolution
  -> validated command kind, target list, quantity, player name, and mode

ChatClef item-action compiler
  -> prefixless get/equip/deposit/give DSL serialization
```

`ItemActionResolution` is a Python internal model only. It is not a bridge DTO,
is not serialized into `command_request` or `command_result`, and must remain
under the Python-owned Fabric ChatClef intent or orchestration boundary. Do not
add this model to `plugins/Minecraft/common/dto/**`.

The current flat item alias resolver should preserve this order:

```text
1. normalize phrase
2. equipment composition
3. command-specific contextual phrase
4. fixed item alias exact-compact lookup
5. catalog validation
6. command-specific capability validation
```

Fixed aliases must remain exact-compact matches. Do not use substring matching.

## Implementation Order

Recommended order when code changes are later approved:

1. Phase 0 - freeze Java grammar, command capability, default policy, deposit
   safety, routing status, normalization, provenance, and coverage metrics.
2. Phase 1 - define generated, curated, default-policy, and canonicalization
   sources; finish generator, validator, coverage classification, deterministic
   output, and compact-collision checks.
3. Phase 2 - expand aliases for the current GET path while preserving the
   current parser, resolver priority, compiler shape, FOOD/MEAT/GOTO/FOLLOW
   regressions, and current GET mining/acquisition regressions.
4. Phase 3 - generalize action precedence across GET, EQUIP, DEPOSIT, and GIVE
   with a shared action matcher, resolved action model, command schemas,
   resolution statuses, submission outcomes, and single-pass translation
   boundaries. This phase must not reclassify already-supported GET
   mining/acquisition phrases as future-only behavior.
5. Phase 4 - add EQUIP support with closed full-set shortcut mapping and
   explicit equipment capability checks.
6. Phase 5 - add DEPOSIT support with frozen bare/specific mode behavior and
   count-omission policy.
7. Phase 6 - add GIVE support with recipient extraction, player-name
   validation, and single-item/count compiler support.
8. Phase 7 - add multi-item GET only after single-item action behavior is
   stable.
9. Phase 8 - integrate router lifecycle behavior: translate once, compile once,
   submit once, no busy/rejected retry, and no accepted-command replay.

Do not combine this with Java bridge changes, DTO changes, inventory cleanup
orchestration, or ChatClef engine changes.

## Phase Exit Criteria

Each phase must satisfy its exit criteria before the next command family starts.

| Phase | Exit criteria |
| --- | --- |
| Phase 0 | every grammar and capability row has source evidence, Python policy, concrete test file/function/assertion, and implemented passing contract tests |
| Phase 1 | generated from actual catalog sources; catalog-outside targets `0`; compact conflicts `0`; deterministic output; source hashes recorded |
| Phase 2 | committed GET alias expansion passes focused alias-only tests; GET/FOOD/MEAT/GOTO/FOLLOW regressions pass; reviewed cfc170a mining/acquisition cases remain green, while only new verb forms require separate approval |
| Phase 3 | Korean GET mining/acquisition verb tests pass as source-proven existing behavior or newly approved implementation work; cross-command action precedence tests for GET/EQUIP/DEPOSIT/GIVE pass separately from alias-only tests; resolution status and submission status are separated; exactly-once tests pass |
| Phase 4 EQUIP | full-set shortcuts and explicit equipment capability tests pass; unsupported equipment targets reject explicitly |
| Phase 5 DEPOSIT | no-count, bare, all, and specific deposit policies are frozen and tested |
| Phase 6 GIVE | recipient extraction, player-name syntax, and no-recipient rejection tests pass |
| Phase 7 | single-action command families are stable before adding multi-item GET |
| Phase 8 | router lifecycle tests prove no busy/rejected retry and no accepted-command replay |

## Tests To Add

Alias contract:

```text
all final alias targets exist in CataloguedResources.txt
empty alias or target rejected
target syntax is lowercase/digit/underscore
same compact alias with different target hard-fails
same compact alias with same target deduplicates
pseudo-target values are not flat item alias targets
dangerous characters and @ are rejected
dangerous player-name characters include # because Java ArgParser treats it as
  a comment delimiter
generation output is deterministic
```

Resolver priority:

```text
다이아몬드 곡괭이 -> diamond_pickaxe
다이아몬드 -> diamond
철 곡괭이 -> iron_pickaxe
철 -> iron_ingot
금 도끼 -> golden_axe
금 -> gold_ingot
레드스톤 -> redstone
레드스톤 블록 -> redstone_block
```

Command actions after the relevant Phase 0 contracts and command phases are
approved:

```text
다이아몬드 10개 가져와줘 -> get diamond 10
돌 64개 가져와줘 -> get stone 64
다이아몬드 갑옷 입어줘 -> equip diamond
철 갑옷 입어줘 -> equip iron
금 갑옷 입어줘 -> equip gold
철 흉갑 입어줘 -> equip iron_chestplate
잡템 상자에 넣어줘 -> deposit
다이아몬드 2개 상자에 넣어줘 -> deposit diamond 2
다이아몬드 3개 Steve에게 줘 -> give Steve diamond 3
Steve한테 다이아몬드 3개 줘 -> give Steve diamond 3
```

Phase-scoped GET alias and action-precedence tests must remain separate:

```text
Phase 2 GET alias expansion:
다이아몬드 가져와줘 -> get diamond 1
돌 64개 가져와줘 -> get stone 64
석탄 구해줘 -> get coal 1

Reviewed implementation GET acquisition / mining matcher cases:
다이아몬드 캐줘 -> get diamond 1
석탄 캐와 -> get coal 1
레드스톤 채굴해줘 -> get redstone 1

Future Phase 3 cross-command action precedence:
다이아몬드 캐줘 -> GET
다이아몬드 갑옷 입어줘 -> EQUIP
다이아몬드 상자에 넣어줘 -> DEPOSIT
Steve에게 다이아몬드 줘 -> GIVE
```

Do not move these reviewed GET mining/acquisition phrases back into a
future-only section. Historical baseline notes for `c912ff...` must stay
separate from current regression coverage. Alias expansion and cross-command
action precedence are still separate risks.

<!-- 20260819_kpopmodder: Kept live GET evidence semantics owned by the test strategy. -->

This alias plan does not redefine gameplay success. The additive GET delta
objective, immutable sequential batch contract, and the currently unsupported
movement/mining objective are owned by `chatclef-korean-test-strategy.md`.

Routing and lifecycle:

```text
UNKNOWN_LANGUAGE_INTENT -> fall through
UNKNOWN_ITEM -> policy-defined fall-through or unknown-item response
AMBIGUOUS_ITEM -> clarification or explicit ambiguity
UNSUPPORTED_ACTION_TARGET -> explicit command rejection
INVALID_INPUT -> reject without LLM fall-through
BRIDGE_DISCONNECTED -> connection error
COMMAND_BUSY -> no submit and no retry
ACCEPTED -> accepted by submission boundary
REJECTED -> no retry and no LLM fall-through
VALIDATED + connected + idle -> submit exactly once
busy/rejected/disconnected -> no automatic retry
translation happens once
resolver happens once
compiler happens once
accepted command is never automatically replayed
```

Phase 0 contract tests:

```text
tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py
tests/minecraft_chatclef/command_catalog/test_item_command_capability_policy.py
tests/minecraft_chatclef/alias_contract/test_korean_alias_generation_contract.py
tests/minecraft_chatclef/lifecycle/test_item_action_routing_status.py
```

These tests may validate known command grammar or source fixtures without
running or modifying Java runtime code.

Do not add flat wrapper tests that import and re-export functions from these
responsibility-owned files. If discovery needs to change, update the discovery
path or CI command explicitly.

Do not rely on Java source-text regular expressions as the primary proof of a
contract. Preferred evidence order:

```text
1. verified contract fixture
2. repository commit and source hash verification
3. Python compiler assertions against the fixture
4. focused tests against the existing Java parser when a later implementation
   phase explicitly authorizes test execution
5. source-text checks only as auxiliary evidence
```

Normalization parity:

```text
generator normalize(alias) == runtime resolver normalize(alias)
철 주괴
철주괴
철  주괴
철 주괴!
NFKC variants
```

Cross-resource precedence:

```text
철 -> iron_ingot
철 곡괭이 -> iron_pickaxe
철 갑옷 입어줘 -> equip iron
철 흉갑 입어줘 -> equip iron_chestplate
철 흉갑 가져와줘 -> get iron_chestplate 1
```

Action precedence and negative examples:

```text
Steve에게 다이아몬드 줘 -> GIVE
다이아몬드 상자에 넣어줘 -> DEPOSIT
다이아몬드 갑옷 입어줘 -> EQUIP
다이아몬드 캐줘 -> GET

다이아몬드 입어줘
  -> ambiguous or unsupported; do not compile as equip diamond without armor-set context

철 입어줘
  -> ambiguous or unsupported; do not compile as equip iron without armor-set context

금 입어줘
  -> ambiguous or unsupported; do not compile as equip gold without armor-set context

Steve에게 다이아몬드 갑옷 입어줘
  -> unsupported; do not compile as GIVE or EQUIP

다이아몬드 상자 가져와줘
  -> item phrase resolution; do not misclassify as DEPOSIT
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

Exactly-once assertions:

```text
translator call count == 1
resolver call count == 1
compiler call count == 1

VALIDATED + connected + idle:
  submit count == 1

unknown / invalid / disconnected / busy:
  submit count == 0

adapter rejected:
  total submit count == 1
  retry count == 0

accepted followed by completed / failed / cancelled / deadline exceeded /
unknown / disconnect / duplicate terminal:
  total submit count == 1
  resubmit count == 0

all paths:
  submit count <= 1

already_active result does not trigger a second submit
```

## Remaining Phase 0 Inputs

### Verified Runtime Contracts

These are determined by source evidence, not user preference:

```text
gold armor shortcut token:
  equip gold, not equip golden

get item grammar:
  single item and bracketed multi-item ItemList syntax

give player syntax:
  Butler/current-user mode uses give <item> [count]
  explicit username requires give <username> <item> <count>

deposit parser count default:
  Java defaults deposit <item> [count] omitted count to 1

command capability matrix:
  which targets are valid for get/equip/deposit/give

prefix handling:
  Fabric dispatcher owns prefix insertion for prefixless Python commands
```

### User UX Decisions

These are LAVI Korean UX policies that the user must choose:

```text
specific deposit without quantity:
  ambiguous or Java-compatible 1

bare/all deposit wording:
  allow only "잡템" or also "전부"

cooked/raw food defaults:
  소고기, 닭고기, 돼지고기, 양고기, 토끼고기, 대구, 연어

목재 default:
  log or planks

UNKNOWN_ITEM behavior:
  LLM fall-through or explicit unknown-item response

legacy/synonym target canonicalization:
  which catalog targets should never be emitted by Korean aliases
```

Recommended initial UX choices to freeze before implementation:

| Policy | Recommended initial value |
| --- | --- |
| specific deposit without quantity | `AMBIGUOUS_ITEM`; ask how many to store |
| `전부 넣어줘` | unsupported or clarification; do not map to bare `deposit` automatically |
| unqualified meat and fish | `proposed_not_frozen`: cooked target by default; explicit `생...` maps to raw target only after the user freezes this policy |
| `목재` | unresolved initially; `원목` / `나무` / `통나무` map to `log`, and `판자` / `나무판자` map to `planks` |
| `UNKNOWN_ITEM` | in connected Minecraft command context, prefer an unknown-item response; ordinary non-command language may fall through to LLM |

### Mandatory Implementation Invariants

These are acceptance criteria, not optional user preferences:

```text
resolution and submission status separation:
  which statuses fall through, reject, ask for clarification, or submit

source provenance:
  catalog, coverage algorithm, catalog parser, language file, generator,
  curated policy, and default policy hashes

Java contract and activation provenance:
  owned by chatclef-korean-test-strategy.md; includes reviewed commit,
  Git-blob source hashes, full Fabric/Mixin activation chain, and HEAD drift

deterministic output:
  identical source inputs produce byte-identical runtime JSON

normalization parity:
  generator and runtime resolver normalize aliases the same way

no wire payload expansion:
  action resolution model remains Python-internal
```

Current Java contract and activation provenance is intentionally not duplicated
in this design plan. The authoritative reviewed source registry, Git-blob
SHA-256 values, full Fabric/Mixin activation chain, item-command source list,
and HEAD-drift assertions live in:

```text
plugins/Minecraft/docs/chatclef-korean-test-strategy.md
```

The strategy must include at least the manifest, `altoclef.mixins.json`,
`EntryMixin`, `EventBus`, `TitleScreenEntryEvent`, `AltoClef`,
`AltoClefCommands`, `CommandExecutor`, command implementation/name sources, and
the LAVI overlay entrypoint/registrar/command sources. This plan must not carry a
second copy of those hashes because duplicated source snapshots drift.

## ChatGPT Handoff Summary

Use this when asking ChatGPT to continue reviewing the plan:

```text
Codex/ChatGPT reconciled the v2 Korean ChatClef item-action alias plan in:

plugins/Minecraft/docs/chatclef-korean-item-action-alias-v2-plan.md

Authority split:

- this plan owns Korean item/action design and phase order
- chatclef-korean-test-strategy.md owns tests, artifacts, source/hash authority,
  coverage algorithm/parser provenance, CI, and live safety
- chatclef-korean-post-review-merge-blockers.md owns current merge status

Frozen design boundaries:

- korean_item_aliases.json remains a flat concrete exact-compact item lexicon
- equip shortcuts, deposit modes, give recipients, quantities, and routing
  outcomes remain command-specific policy
- Python emits prefixless DSL only
- Java, bridge DTOs, wire payloads, and ChatClef/AltoClef engine code remain
  unchanged by this plan
- current reviewed GET mining/acquisition phrases at cfc170a remain regressions
- EQUIP, DEPOSIT, GIVE, and multi-item GET remain phased future work
- accepted commands are never automatically replayed

Coverage state:

- historical c912ff runtime baseline: 11 aliases, 4 covered targets, 587 missing, 0.68%
- reviewed cfc170a runtime baseline: 19 aliases, 9 covered targets, 582 missing, 1.52%
- proposed v2 seed: 191 aliases, 167 covered targets, 424 missing, 28.26%
- all accepted coverage artifacts use algorithm version 1 and record catalog
  parser commit/path/hash
- the production parser requires the ten documented baseline targets

Phase 0 is not complete until the focused tests, source-backed artifacts,
capability rules, CI scope, and merge gates in the test strategy and
post-review document are implemented and green.

Do not start EQUIP, DEPOSIT, or GIVE natural-language implementation before
Phase 0 is complete.
```
