<!-- 20260815_kpopmodder: Documented v2 Korean item alias expansion across get/equip/deposit/give before implementation. -->
<!-- 20260815_chatgpt: Synchronized design authority, reviewed coverage provenance, and Phase 0 gates with the test strategy and merge-blocker documents. -->
<!-- 20260818_kpopmodder: Linked the post-restore live lifecycle and fail-closed preflight status without expanding item-action scope. -->
<!-- 20260819_kpopmodder: Added reviewed-baseline Korean craft-wording, colloquial equipment alias, canonical display, and catalog coverage contracts. -->
<!-- 20260819_chatgpt: Reconciled e08af source classification, cross-document authority, and safe junk-deposit policy. -->
<!-- 20260820_kpopmodder: Split full Korean command registry authority from item-action alias planning. -->
<!-- 20260820_chatgpt: Clarified full-registry authority, docs-only provenance, readiness gates, and handoff synchronization. -->
<!-- 20260829_openai: Reconciled the historical 20-command baseline with the current 22-command Java surface without expanding item-action aliases. -->

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
this document, but did not approve immediate full implementation. Before any
later code implementation is authorized, the full-command registry/lifecycle
prerequisite and the item-action Phase 0 contract gates must both be closed and
the user must separately approve the code-changing phase.

## Related Documents And Authority

Read this plan with:

```text
plugins/Minecraft/README.md
plugins/Minecraft/docs/chatclef-python-korean-command-registry-plan.md
plugins/Minecraft/docs/chatclef-python-command-orchestration-plan.md
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
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
  Korean item/action language, concrete aliases, canonical Korean display,
  command-capability boundaries, and item-action phase order

Python Korean command registry plan:
  versioned registered-command snapshot: 20 at the reviewed historical baseline
  and 22 in the current Java surface, plus per-command lifecycle kind,
  resolver domain, safety tier, confirmation mode, allowed input source, and
  public enablement axes

Python command orchestration plan:
  evidence-bounded Korean responses, operation context, lifecycle sequencing,
  and exactly-once primary-command submission

Python inventory cleanup preflight contract:
  inventory evidence, protected-item policy, targeted cleanup planning,
  fresh post-cleanup verification, and fail-closed primary admission

test strategy:
  test evidence, artifact schemas, source/hash authority, coverage algorithm,
  catalog parser provenance, CI scope, and live-test safety

post-review merge blockers:
  current implementation/CI status, remaining blockers, and merge decision

historical resolution analysis:
  historical diagnosis only; it has no current normative authority

plugins/Minecraft/README.md:
  reading map and ownership summary only; it does not own detailed contracts

live-runtime preflight plan:
  exact one-shot approval, endpoint/process/world gates, submission uncertainty,
  and PreflightDecision/LiveRunObservation structure

live-runtime process lifecycle index:
  fixed audit snapshots, ownership runbooks, and cross-document navigation
```

When wording conflicts, this plan controls Korean item/action language, alias
policy, and item-target capability. The command registry plan controls the full
versioned registration taxonomy (20 at the reviewed historical baseline and 22
in the current Java surface) and each command's lifecycle kind, resolver
domain, safety tier, confirmation mode, allowed input source, readiness axes,
and public enablement. The orchestration plan controls user-response evidence
and operation sequencing; the cleanup contract controls automatic or
junk-policy cleanup; the test strategy controls tests, provenance, and coverage;
and the post-review document controls current implementation and merge status.
The historical analysis and README cannot override any normative contract.

For the 2026-08-19 authority migration, the seven user-listed documents were one
docs-only change unit. Later changes must update and commit every directly
affected normative document together. Linked live-runtime documents need an
update only when their live-run contract actually changes.

The current 22-command Java surface includes raw-only `deposit_all` and the
separately governed `store_home`. Neither expands this item/action alias plan.
This plan adds no `deposit_all` Korean alias, parser/compiler branch, admission
source, bridge route, gameplay claim, or public exposure.

For the 2026-08-20 command-registry split, the current docs-only change unit is:

```text
plugins/Minecraft/README.md
plugins/Minecraft/docs/chatclef-python-korean-command-registry-plan.md
plugins/Minecraft/docs/chatclef-korean-item-action-alias-v2-plan.md
plugins/Minecraft/docs/chatclef-python-command-orchestration-plan.md
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
plugins/Minecraft/docs/chatclef-korean-test-strategy.md
plugins/Minecraft/docs/chatclef-korean-post-review-merge-blockers.md
```

The historical resolution analysis remains linked but is not part of this
2026-08-20 change unit unless its historical-status contract changes.

The 2026-08-18 post-restore audit remains a live-runtime-specific audit against
baseline `4239c23`. It does not replace the item/action source baseline below.
Live-test pass semantics, approval, preflight, terminal/runtime-completion
terminology, and gameplay-effect oracles remain owned by the test strategy,
post-review merge blockers, and live-runtime preflight plan.

## Reviewed Source Baseline And Implementation Classification

This documentation review classifies source against the fixed archive baseline:

```text
e08af63948a3fa4675c70279db59c2a70b00a332
```

This is a source-audited classification. It does not claim that this docs-only
change executed tests, built Java, launched Minecraft, or reproduced runtime
behavior. The older `cfc170a` alias snapshot and `4239c23` live audit remain
scoped evidence records, not replacements for the overall `e08af639` baseline.
At `e08af639`, `korean_item_aliases.json` remains byte-identical to the reviewed
`cfc170a` alias snapshot, so its 19-alias / 9-target coverage figures still apply
to that resource file only.

Source-present at `e08af639`:

```text
- shared Korean acquire / craft-wording / mining-verb matcher
- GET_ITEM routing for 가져와, 구해, 얻어, 만들어줘, 제작해줘,
  캐줘, 캐와, 캐오기, and 채굴해줘 forms
- quantity removal -> verb removal -> soft-word removal -> whitespace
  normalization -> trailing 을/를 removal
- prefixless single-item GET compilation: get <target> <count>
- source-resolvable GET behavior including 철 10개 캐줘 / 캐오기 / 캐와줘
- current flat aliases including 철 -> iron_ingot and 다이아몬드 -> diamond
- current equipment components including 바지, 신발, and 헬멧
- router-side single-pass submission and unresolved-submission reconciliation gate
```

Not source-present at `e08af639` and therefore still planned:

```text
- emerald and torch Korean item aliases
- 갑바, 레깅스, and 모자 equipment-component aliases
- whole-phrase equipment-composition enforcement; the current resolver still
  accepts material/equipment substring containment
- canonical Korean display-name resource and deterministic response renderer
- Korean EQUIP_ITEM / DEPOSIT_ITEM / GIVE_ITEM compiler support
- explicit Minecraft responses for unknown / ambiguous / unsupported outcomes
- reliable Python inventory snapshot provider
- protected-item cleanup planner, targeted cleanup orchestration, and fresh
  post-cleanup inventory-effect verification
```

Reported post-baseline working-tree implementation carried forward from the
2026-08-19 follow-up:

```text
- emerald and torch fixed Korean aliases are source-present
- 갑바, 레깅스, and 모자 equipment-component aliases are source-present
- equipment composition now requires full compact-phrase consumption
- canonical Korean display-name resource is source-present for the initial
  response cases
- deterministic Python response rendering is source-present for route decisions
- Python cleanup policy and post-cleanup admission helpers are source-present;
  reliable inventory snapshot collection and automatic cleanup execution remain
  planned
```

This is a carried-forward working-tree report, not a replacement for the fixed
`e08af639` baseline and not an assertion that the 2026-08-20 docs-only change
implemented or re-ran any Python source or test. Current implementation status is
owned by the post-review merge-blocker document; current test evidence is owned
by the test strategy and must be re-audited when the working tree changes.

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

GET acquisition, including Korean craft wording, EQUIP, DEPOSIT, and GIVE share
the same concrete item lexicon. Craft is not a separate registered ChatClef
command in this runtime.

The same concrete item lexicon can be reused by multiple command families:

| Command | Shared alias usage | Extra policy needed |
| --- | --- | --- |
| `get` | concrete resource target | quantity and optional item-list parsing |
| `equip` | explicit equippable target | armor-set shortcut policy |
| `deposit` | specific item target | specific-item quantity policy; junk handling remains a Python policy mode and Java bare-deposit exposure is separate |
| `give` | single item target | recipient slot and player-name validation |

Examples below separate reviewed runtime alias availability from
command-specific behavior. Rows explicitly marked planned or pending must not be
treated as verified runtime behavior until the relevant Phase 0 capability
verification is complete.

| Korean phrase | Shared target or policy | Status at `e08af639` | Command-specific status |
| --- | --- | --- | --- |
| `다이아몬드` | `diamond` | current fixed alias; unchanged from the reviewed `cfc170a` alias snapshot | preserve as a current GET regression |
| `철` | `iron_ingot` | current fixed alias; unchanged from the reviewed `cfc170a` alias snapshot | preserve as a current GET shorthand; `equip iron` is allowed only in material + armor-set context such as `철 갑옷` |
| `철 흉갑` | `iron_chestplate` | current equipment composition, not a planned flat alias | source-present for GET; explicit EQUIP remains future work pending capability verification |
| `철바지` / `철신발` / `철헬멧` | `iron_leggings` / `iron_boots` / `iron_helmet` | current equipment compositions | preserve source-present GET behavior; explicit EQUIP remains future work |
| `철갑바` / `철 레깅스` / `철모자` | corresponding iron armor target | current working-tree equipment compositions | covered by current GET regressions |
| `에메랄드` / `횃불` | `emerald` / `torch` | current working-tree fixed aliases | covered by current GET regressions |
| `잡템` | Python junk-deposit policy mode | contextual policy only; no concrete target and no direct DSL | hand off to the cleanup contract; never compile directly to bare `deposit` |

## Craft Wording Is GET Acquisition

Korean craft wording does not introduce a separate `CRAFT_ITEM` intent or a new
craft DSL command. ChatClef's Java command set uses `GetCommand` for resource
acquisition and item crafting.

Contract:

```text
만들어줘 / 제작해줘
  -> Python action wording: craft
  -> executable command family: GET
  -> DSL: get <target> <count>
```

Current reviewed craft-wording example:

```text
철 삽 하나 만들어 -> get iron_shovel 1
```

Required regressions for the reported post-`e08af639` working-tree
implementation:

```text
횃불 만들어줘 -> get torch 1
철갑바 만들어줘 -> get iron_chestplate 1
철 레깅스 만들어줘 -> get iron_leggings 1
```

The three cases above are not `e08af639` successes. Their current source/test
status is a carried-forward report whose authority remains the post-review
merge-blocker document and test strategy.

Do not emit `craft torch 1`, `@craft`, or any new Java craft command.

This mapping applies only after Minecraft item/action context is established and
a concrete target resolves. Generic `만들어줘` wording with an unknown or
non-Minecraft target must not submit ChatClef. This contract defines
command-family selection and serialization only. It does not prove that every
resolved target is obtainable or craftable, does not bypass command-specific
capability checks, and does not prove terminal completion or a gameplay effect.

## Input Alias Versus Canonical Korean Display Name

Input aliases and user-facing canonical display names are separate
responsibilities.

| Input alias | Target | Canonical display |
| --- | --- | --- |
| `철갑바` | `iron_chestplate` | `철 흉갑` |
| `철바지` | `iron_leggings` | `철 레깅스` |
| `철신발` | `iron_boots` | `철 부츠` |
| `철모자` | `iron_helmet` | `철 투구` |
| `철헬멧` | `iron_helmet` | `철 투구` |

Recommended Python-owned runtime resource:

```text
plugins/Minecraft/fabric/chatclef/intent/resources/korean_item_display_names.json
```

Responsibility:

```text
concrete target -> canonical Korean display name
```

The response renderer should use canonical display names. It should not echo a
colloquial input such as `갑바` when the canonical item name is `철 흉갑`.

## Equipment Component Alias Policy

The following words form the desired equipment component alias contract, not a
flat concrete item alias map:

```json
{
  "갑바": "chestplate",
  "흉갑": "chestplate",
  "레깅스": "leggings",
  "바지": "leggings",
  "각반": "leggings",
  "부츠": "boots",
  "신발": "boots",
  "투구": "helmet",
  "헬멧": "helmet",
  "모자": "helmet"
}
```

Placement:

```text
plugins/Minecraft/fabric/chatclef/intent/resources/korean_equipment_aliases.json
```

Status at `e08af639` and after the 2026-08-19 follow-up:

```text
component aliases at e08af639:
  흉갑, 바지, 각반, 부츠, 신발, 투구, 헬멧

additional working-tree component aliases:
  갑바, 레깅스, 모자

working-tree resolver hardening:
  require full compact-phrase consumption instead of substring containment
```

Component aliases such as `갑바` and `모자` are valid only when the full phrase
resolves as material plus equipment component:

```text
철갑바 -> valid
철모자 -> valid
모자 만들어줘 -> do not infer iron_helmet
```

## Layered Resolution Precedence

Action/context policy, concrete target resolution, catalog membership, and
command capability are separate:

```text
Layer 1: action/context policy
  acquisition / equip / deposit / give
  armor-set mode
  junk-deposit policy mode
  recipient extraction

Layer 2: concrete target resolver
  1. equipment composition
  2. fixed item alias
  3. unsupported
  4. unknown

Layer 3: catalog membership and canonical target validation

Layer 4: command-specific capability validation
  GET / EQUIP / DEPOSIT / GIVE
```

The Layer 2 order remains fixed as equipment composition -> fixed item alias ->
unsupported -> unknown. `AMBIGUOUS` is a Layer 1 policy result, not a
concrete-target fallback. A concrete alias resolution is not executable until
Layers 3 and 4 also pass.

Equipment composition must consume the whole compact phrase as material plus
equipment component. Do not accept accidental substring matches:

```text
철 + 갑바 -> valid
다이아몬드 + 곡괭이 -> valid
철학 + 모자 -> invalid
금요일 + 바지 -> invalid
```

Existing combinations such as `diamond_pickaxe` must remain valid.

## Catalog-Driven Alias Coverage

The reviewed runtime snapshot remains intentionally narrow:

```text
catalog targets: 591
runtime aliases at e08af639/cfc170a: 19
covered targets at e08af639/cfc170a: 9
coverage at e08af639/cfc170a: 1.52%
reported post-e08af working-tree runtime aliases: 21
reported post-e08af working-tree covered targets: 11
reported post-e08af working-tree coverage: 1.86%
```

The three working-tree figures above are a carried-forward reported snapshot,
not a fixed commit baseline. The test strategy owns the coverage artifact and
provenance; the post-review document owns whether that snapshot is current.

Manual additions alone cannot satisfy the "all Minecraft items in Korean"
request. "All 591 targets" means every catalog target is classified; it does
not mean every raw catalog target receives an unconditional public Korean alias.
Future coverage should use layered, deterministic inputs:

```text
1. version-pinned Minecraft 1.20.1 Korean language source
2. direct vanilla item/block target mapping
3. curated colloquial aliases
4. ambiguous/default policy
5. ChatClef synthetic/group target canonicalization
6. command-specific capability policy
7. deterministic generated runtime alias map
```

Coverage metrics must distinguish:

```text
raw catalog coverage:
  alias coverage across all 591 catalog targets

requestable concrete coverage:
  alias coverage across concrete item/block targets that can be requested
```

Every raw catalog target must receive exactly one primary classification before
the project claims catalog-classification completeness:

```text
DIRECT_ITEM
DIRECT_BLOCK
CANONICALIZED_LEGACY
GENERIC_GROUP
COMMAND_INTERNAL
AMBIGUOUS_POLICY
UNSUPPORTED
UNRESOLVED
```

Classification completeness is necessary but not sufficient for an "all items
supported in Korean" claim. `UNSUPPORTED` and `UNRESOLVED` remain explicit
coverage gaps, and command-specific public readiness still requires catalog,
capability, lifecycle, safety, confirmation, and public-enablement gates.

Coverage acceptance must separate current preservation from
post-implementation cases:

| Target | Korean phrase status | Required result |
| --- | --- | --- |
| `emerald` | `에메랄드` source-present in the current working tree | fixed alias regression |
| `torch` | `횃불` source-present in the current working tree | fixed alias regression |
| `iron_leggings` | current through `철바지` / `철각반` / `철 레깅스` | preserve component regression |
| `iron_chestplate` | current through `철 흉갑` / `철갑바` | preserve component regression |
| `iron_boots` | current through `철신발` / `철 부츠` | preserve current composition coverage |
| `iron_helmet` | current through `철헬멧` / `철 투구` / `철모자` | preserve component regression |
| `diamond_pickaxe` | current through `다이아몬드 곡괭이` | preserve existing composition regression |

`iron_leggings` should normally come from equipment composition such as
`철 + 레깅스`, not from a broad one-off flat alias for every armor phrase.
`emerald` and `torch` are fixed/generated item-alias targets.

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
잡템 상자에 넣어줘 -> Python junk-deposit policy mode; no direct DSL
다이아몬드 2개 상자에 넣어줘 -> deposit diamond 2
돌 32개 보관해줘 -> deposit stone 32
```

Bare Java `deposit` is broad. Source inspection shows that the zero-argument
path builds targets through `getAllNonEquippedOrToolItemsAsTarget`, excluding
`PlayerSlot.ARMOR_SLOTS` and `ToolItem` stacks. Spare armor that is merely in a
normal inventory slot is therefore not the same category as equipped armor.
Bare `deposit` can include useful items such as food, torches, fuel, ingots,
and blocks.

Therefore `잡템` must be a Python-owned policy mode, not a fake item alias and
not a direct synonym for bare `deposit`. This plan owns only Korean recognition
and item/action policy. The cleanup contract owns inventory evidence, protected
items, exact targeted `deposit <target> <count>` planning, post-cleanup effect
verification, and fail-closed admission of any following primary command.
Automatic inventory-full cleanup and user-requested `잡템` cleanup must not use
bare `deposit`. If reliable evidence cannot produce a safe targeted plan, no
cleanup command is submitted.

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
      "deposit": "python_junk_deposit_policy"
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
`deposit`, and `give` immediately. No row below authorizes code changes until
the registry/lifecycle prerequisite and item-action Phase 0 gates are complete
and the user separately approves the implementation phase.

| Area | Status |
| --- | --- |
| alias generator / validator / compact index | design-approved in principle; implementation remains blocked by the registry/lifecycle prerequisite, item-action Phase 0, and separate user approval |
| existing GET alias expansion | design-approved after focused regression gates; implementation still requires separate user approval |
| EQUIP natural-language support | requires capability matrix and command-registry readiness first |
| DEPOSIT natural-language support | requires specific-item quantity policy, junk-policy mode, explicit broad-deposit exposure policy, and command-registry readiness first |
| GIVE natural-language support | requires recipient grammar, player validation, and command-registry readiness first |
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

Java bare DEPOSIT grammar (documented, not exposed by default):
  exact Java-compatible shape is deposit
  Korean `잡템` wording and automatic cleanup emit no bare command
  any future Korean broad-deposit exposure requires separate explicit approval

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
| bare `deposit` | zero arguments produce `null` `ItemList`; command excludes `PlayerSlot.ARMOR_SLOTS` and `ToolItem` stacks, not every armor item by type | `DepositCommand.java`; `Arg.java`; `ArgParser.java` | document the Java grammar, but do not expose it through `잡템` or automatic cleanup; any separate broad-deposit phrase requires explicit future approval | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_bare_deposit_contract_matches_source_predicate`; assert the source-backed truth table exactly, and a separate Python policy test must assert that `잡템` and automatic cleanup emit no bare `deposit` |
| specific `deposit` count | `deposit <item> [count]` parses through `ItemList`; omitted count defaults to `1` | `DepositCommand.java`; `ItemList.java` | Java default is known, but Korean no-count wording remains a UX policy decision | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_deposit_java_default_count_contract_is_one`; assert Java contract is `1` while Korean no-count policy remains separately tested |
| `deposit` multi-item exposure | Java can parse bracketed `ItemList`, but Python v2 initially exposes only single specific-item deposit; broad bare grammar remains unexposed by default | `DepositCommand.java`; `ItemList.java` | mark multi-item deposit as intentionally unsupported until a later explicit phase | `tests/minecraft_chatclef/command_catalog/test_item_command_capability_policy.py::test_python_v2_deposit_multi_item_is_intentionally_unsupported_initially`; assert `deposit [diamond 2, stone 10]` is not emitted by Korean v2 compiler |
| `give` Butler/current-user syntax | no explicit username uses `give <item> [count]`; omitted count defaults to `1` through Java defaulting | `GiveCommand.java`; `Arg.java`; `ArgParser.java` | Korean natural-language GIVE should not rely on Butler/current-user mode | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_give_butler_current_user_mode_contract`; assert `give diamond` and `give diamond 3` are Butler/current-user forms, not explicit-recipient forms |
| `give` explicit recipient syntax | explicit recipient uses `give <username> <item> <count>`; count is required for this mode | `GiveCommand.java`; `Arg.java`; `ArgParser.java` | Korean GIVE with recipient must compile a count-bearing command such as `give Steve diamond 1` or `give Steve diamond 3` | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_give_explicit_recipient_requires_count_contract`; assert `give Steve diamond 1` is valid explicit-recipient shape and `give Steve diamond` is not documented as explicit-recipient shorthand |
| `give` multi-item support | Java `GiveCommand` accepts a single `String item`, not an `ItemList` | `GiveCommand.java` | Python v2 GIVE supports one recipient plus one item only | `tests/minecraft_chatclef/command_catalog/test_item_command_capability_policy.py::test_give_multi_item_is_not_supported_by_v2_contract`; assert no Korean phrase compiles to `give Steve [diamond 2, stone 10]` |
| `give` no-recipient mode | no username can fall back to current Butler user, otherwise warns and stops | `GiveCommand.java` | Korean natural-language GIVE should require an explicit recipient and not rely on Butler context | `tests/minecraft_chatclef/lifecycle/test_item_action_routing_status.py::test_korean_give_requires_explicit_recipient`; assert `다이아몬드 줘` does not compile as GIVE |
| player name syntax policy | Java treats username as a string and performs loaded-player validation at runtime; `ArgParser` treats `#` as a comment delimiter | `GiveCommand.java`; `Arg.java`; `ArgParser.java` | Python rejects empty names, whitespace, line breaks, `#`, and DSL separators; Java owns actual loaded-player validation | `tests/minecraft_chatclef/lifecycle/test_item_action_routing_status.py::test_player_name_policy_is_minimal_syntax_only`; assert injection-shaped names, including `#` comment truncation attempts, reject and ordinary names pass to Java-owned validation |
| player loaded validation | runtime rejects if `EntityTracker.isPlayerLoaded(username)` is false | `GiveCommand.java` | Python may syntax-validate names, but runtime loaded-player validation remains Java-owned | `tests/minecraft_chatclef/lifecycle/test_item_action_routing_status.py::test_loaded_player_validation_is_not_claimed_by_python`; assert Python does not claim loaded-player success |
| prefix insertion | Fabric dispatcher trims command and prepends current ChatClef prefix only if missing | `FabricChatClefCommandDispatcher.java`; `CommandExecutor.java` | Python compiler remains prefixless | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract.py::test_bridge_dispatcher_owns_prefix_insertion`; assert Python emits `get diamond 1`, not `@get diamond 1` |
| Java contract provenance | the strategy-owned `reviewed_source_commit`, Git-blob SHA-256 registry, activation chains, and HEAD-drift checks must match before Python contract fixtures are trusted | `chatclef-korean-test-strategy.md`; reviewed Java source registry | stop Phase 0 contract validation if reviewed provenance or HEAD drift fails | `tests/minecraft_chatclef/command_catalog/test_java_item_command_contract_provenance.py::test_java_contract_source_hashes_match_documented_baseline`; assert reviewed commit provenance and HEAD source hashes match the strategy-owned artifact |
| contextual policy non-flattening | contextual command policies are not concrete item aliases | `korean_item_aliases.json`; default/contextual policy source | keep `잡템`, `갑옷`, junk-deposit modes, armor-set shortcuts, and other policy tokens out of the flat runtime alias map | `tests/minecraft_chatclef/alias_contract/test_korean_alias_generation_contract.py::test_contextual_policy_entries_are_not_flattened_into_item_aliases`; assert contextual keys and pseudo-policy values are absent from concrete runtime item entries |

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
  canonical user-facing targets with catalog membership and
  supports_get(target) == true

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
| `잡템 상자에 넣어줘` | Python junk-deposit policy mode; no direct DSL; require safe targeted plan under the cleanup contract |
| `전부 상자에 넣어줘` | unsupported or clarification |
| `인벤토리 비워줘` | unsupported or clarification |
| `다이아몬드 2개 넣어줘` | `deposit diamond 2` |
| `다이아몬드 넣어줘` | ambiguous until no-count policy is frozen |

The bare-deposit truth table below documents Java reality and explains why the
broad form is not a safe junk-cleanup primitive. It does not approve exposing
bare `deposit` through Korean `잡템` wording or through automatic cleanup.

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
inventory snapshot and an explicit policy. A cleanup terminal status of
`completed` is also insufficient by itself; fresh post-cleanup inventory
evidence must prove the required effect before any following primary command is
eligible for submission.

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

alias_resolution_coverage
  = size(resolved_canonical_user_facing_targets)
    / canonical_user_facing_target_total

get_actionable_user_facing_targets
  = canonical_user_facing_targets intersect supports_get_targets

resolved_get_actionable_targets
  = get_actionable_user_facing_targets intersect targets_resolvable_from_korean

get_actionable_coverage
  = size(resolved_get_actionable_targets)
    / size(get_actionable_user_facing_targets)
```

Legacy targets canonicalized into a covered target should be reported as
`legacy_target -> canonical_target` mappings. They are not subtracted again
after canonicalization and are not reported as missing aliases.

Alias resolution coverage only proves that Korean wording resolves to canonical
catalog targets. GET actionable coverage additionally requires command
capability evidence from the target policy; do not treat a Korean-resolvable
target as executable for GET until `supports_get(target)` is true.

Classifier invariants:

```text
each raw catalog target has exactly one primary classification
all set operations above compare canonical target IDs, not mixed raw/canonical IDs
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

## Current `e08af639` Code Gap

At the reviewed archive baseline, the Python natural-language command path
already contains parser/compiler support for these intent families:

```text
GET_ITEM
FOOD
MEAT
GOTO
FOLLOW
IDLE
STOP
```

This list is a legacy parser/compiler-readiness summary only. It does not prove
Python admission readiness, bridge lifecycle readiness, gameplay-effect
verifiability, or public Korean enablement. The command registry plan owns those
independent readiness axes for every registered command.

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
3. fixed item alias exact-compact lookup
4. unsupported
5. unknown
```

Fixed aliases must remain exact-compact matches. Do not use substring matching.
Command-specific contextual phrases, ambiguity, shortcut modes, and recipient
extraction are Layer 1 action/context policy, not flat concrete item aliases.

## Implementation Order

Recommended order when code changes are later approved:

0. Registry Phase 0 prerequisite - close the versioned registration contract
   (20 at the reviewed historical baseline and 22 in the current Java surface),
   lifecycle, resolver-domain, safety, confirmation, allowed-source, readiness,
   and public-enablement contracts in
   `chatclef-python-korean-command-registry-plan.md`. This is a hard gate: no
   later implementation phase starts while a required registry/lifecycle
   contract remains unresolved, and item-action grammar alone never proves
   public command enablement.
1. Phase 0 - freeze Java grammar, command capability, default policy, deposit
   safety, routing status, normalization, provenance, and coverage metrics.
2. Phase 1 - define generated, curated, default-policy, and canonicalization
   sources; finish generator, validator, coverage classification, deterministic
   output, and compact-collision checks.
3. Phase 2 - expand aliases for the current GET path while preserving the
   current parser, resolver priority, compiler shape, existing
   FOOD/MEAT/GOTO/FOLLOW/IDLE/STOP parser/compiler regressions, and current
   GET mining/acquisition regressions.
4. Phase 3 - generalize action precedence across GET, EQUIP, DEPOSIT, and GIVE
   with a shared action matcher, resolved action model, command schemas,
   resolution statuses, submission outcomes, and single-pass translation
   boundaries. This phase must not reclassify already-supported GET
   mining/acquisition phrases as future-only behavior.
5. Phase 4 - add EQUIP support with closed full-set shortcut mapping and
   explicit equipment capability checks.
6. Phase 5 - add DEPOSIT support with frozen specific-item quantity behavior,
   Python junk-policy recognition, and broad-deposit exposure disabled by
   default.
7. Phase 6 - add GIVE support with recipient extraction, player-name
   validation, and single-item/count compiler support.
8. Phase 7 - add multi-item GET only after single-item action behavior is
   stable.
9. Phase 8 - integrate router lifecycle behavior: translate once, compile once,
   submit once, no busy/rejected retry, and no accepted-command replay.

Do not combine alias/action implementation with Java bridge changes, DTO
changes, inventory-cleanup orchestration, or ChatClef engine changes. Automatic
cleanup remains a separately approved phase governed by
`chatclef-python-inventory-cleanup-preflight-contract.md`.

## Phase Exit Criteria

Each phase must satisfy its exit criteria before the next command family starts.

| Phase | Exit criteria |
| --- | --- |
| Phase 0 | every grammar and capability row has source evidence, Python policy, concrete test file/function/assertion, and implemented passing contract tests |
| Phase 1 | generated from actual catalog sources; catalog-outside targets `0`; compact conflicts `0`; deterministic output; source hashes recorded |
| Phase 2 | committed GET alias expansion passes focused alias-only tests; GET/FOOD/MEAT/GOTO/FOLLOW/IDLE/STOP parser/compiler regressions pass; reviewed `e08af639` mining/acquisition cases remain green; these checks do not promote registry lifecycle or public-enablement axes, while only new verb forms require separate approval |
| Phase 3 | Korean GET mining/acquisition verb tests pass as source-proven existing behavior or newly approved implementation work; cross-command action precedence tests for GET/EQUIP/DEPOSIT/GIVE pass separately from alias-only tests; resolution status and submission status are separated; exactly-once tests pass |
| Phase 4 EQUIP | full-set shortcuts and explicit equipment capability tests pass; unsupported equipment targets reject explicitly |
| Phase 5 DEPOSIT | specific-item quantity and junk-policy behavior are frozen and tested; `잡템` and automatic cleanup emit no bare `deposit`; broad-deposit exposure remains disabled unless separately approved |
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
잡템 상자에 넣어줘 -> Python junk-deposit policy mode; direct DSL absent
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

Reviewed `e08af639` GET acquisition / mining matcher cases:
철 10개 캐줘 -> get iron_ingot 10
철 10개 캐오기 -> get iron_ingot 10
철 10개 캐와줘 -> get iron_ingot 10
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
future-only section. Historical baseline notes for `c912ff...` and the scoped
`cfc170a...` alias snapshot must stay separate from current `e08af639`
regression coverage. Alias expansion and cross-command
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

junk-deposit wording:
  `잡템` selects a Python policy mode, never a flat alias or direct bare command

explicit broad-deposit exposure:
  whether any separate wording may emit Java bare `deposit`; default is none

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
| `잡템 넣어줘` | Python junk-deposit policy mode; require reliable inventory evidence and a safe targeted plan; no direct bare `deposit` |
| explicit broad `deposit` wording | disabled initially; no Korean phrase emits bare `deposit` |
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

## Hard Prohibitions

This documentation and every later implementation phase under it MUST NOT:

```text
- modify Java under plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/**
- modify ChatClef, AltoClef, Baritone task selection, retry, timeout,
  completion, container-transfer, or goal behavior
- modify plugins/Minecraft/common/dto/**, common/protocol/**, or common/schema/**
- add, remove, rename, or reinterpret any v1 wire message or payload field
- add CRAFT_ITEM, a craft DSL command, or a new Java craft command
- implement Korean parsing, canonical display, response rendering, junk policy,
  or orchestration in Java
- put 잡템, 갑옷, recipients, quantities, command modes, or shortcut tokens in
  korean_item_aliases.json
- compile Korean junk-policy wording or automatic inventory cleanup directly to
  the broad zero-argument `deposit` command
- use argument-less deposit as the inventory-full cleanup primitive
- invent cleanup targets or placeholder quantities
- infer inventory-full from logs, task duration, timeout, deadline, path stalls,
  terminal UNKNOWN, or missing evidence
- advance the primary command from cleanup terminal completion alone, or when
  cleanup/post-cleanup evidence is stale, mismatched, or UNKNOWN
- retry, replay, rerun, or automatically resubmit cleanup or primary commands
- submit a follow-up command inline from a WebSocket callback
- infer gameplay effect from VALIDATED, ACCEPTED, RUNNING, terminal COMPLETED,
  active-request clearing, or connection state alone
- treat legacy `IMPLEMENTED` wording or `KOREAN_PARSE_COMPILE_READY` as proof of
  Python admission, bridge lifecycle, gameplay effect, or public enablement
- bypass the command registry's lifecycle, safety, confirmation, allowed-source,
  readiness-axis, or public-enablement gate for any item command
- emit an @ prefix from the Python natural-language compiler
- let an LLM generate raw ChatClef DSL or control workflow state transitions
- introduce Forge MineMind fallback or shared Fabric/Forge orchestration
```

If a later implementation appears to require any prohibited change, stop that
phase and open a separate evidence-backed design review. Do not expand scope
implicitly.

## ChatGPT Handoff Summary

Use this when asking ChatGPT to continue reviewing the plan:

```text
Codex/ChatGPT reconciled the v2 Korean ChatClef item-action alias plan at the
fixed overall source baseline:

e08af63948a3fa4675c70279db59c2a70b00a332

Authority split:

- this plan owns Korean item/action language, aliases, canonical display,
  capability boundaries, and item-action phase order
- chatclef-python-korean-command-registry-plan.md owns the versioned registration
  snapshot (20 at the reviewed historical baseline and 22 in the current Java
  surface), lifecycle kind, resolver domain, safety tier,
  confirmation mode, allowed input source, readiness axes, and public enablement
- chatclef-python-command-orchestration-plan.md owns evidence-bounded Korean
  responses, lifecycle sequencing, and exactly-once primary submission
- chatclef-python-inventory-cleanup-preflight-contract.md owns inventory
  evidence, protected-item policy, targeted cleanup, fresh post-cleanup
  verification, and fail-closed primary admission
- chatclef-korean-test-strategy.md owns tests, artifacts, source/hash authority,
  coverage algorithm/parser provenance, CI, and live safety
- chatclef-korean-post-review-merge-blockers.md owns current implementation and
  merge status
- chatclef-korean-item-command-resolution-analysis.md is historical diagnosis
  only
- plugins/Minecraft/README.md is navigation and ownership summary only

Frozen design boundaries:

- korean_item_aliases.json remains a flat concrete exact-compact item lexicon
- equipment components and fixed aliases remain separate resources
- input aliases and canonical Korean display names remain separate
- craft wording uses existing GET acquisition and emits get <target> <count>
- resolver order remains equipment composition -> fixed item alias ->
  unsupported -> unknown, followed by catalog and command-capability checks
- SOURCE_REGISTERED, KOREAN_PARSE_COMPILE_READY, PYTHON_ADMISSION_READY,
  BRIDGE_LIFECYCLE_READY, GAMEPLAY_EFFECT_VERIFIABLE, and
  PUBLIC_KOREAN_ENABLED remain separate registry-owned axes; no legacy
  `IMPLEMENTED` label collapses them
- 잡템 is a Python policy mode, not an item alias and not a direct synonym for
  bare deposit
- automatic or user-requested junk cleanup uses safe targeted deposit only
  under the cleanup contract; unknown cleanup effect blocks the primary command
- Python emits prefixless DSL only
- Java, bridge DTOs, wire payloads, and ChatClef/AltoClef engine code remain
  unchanged
- accepted commands are never automatically replayed

Source classification at e08af639:

- GET acquire/craft/mining matching, 철 10개 캐줘-family source behavior,
  prefixless GET compilation, and single-pass reconciliation are source-present;
  exact test-evidence maturity remains owned by the test strategy
- emerald/torch aliases, 갑바/레깅스/모자 components, whole-phrase equipment
  composition, canonical display, deterministic route-response rendering, and
  Python cleanup policy helpers are reported source-present in the pre-existing
  post-e08af working tree; the 2026-08-20 docs-only registry change did not
  implement or re-test them
- EQUIP/DEPOSIT/GIVE Korean compilation, reliable inventory snapshots, and
  automatic cleanup execution are still planned

Coverage state:

- historical c912ff runtime baseline: 11 aliases, 4 covered targets, 587 missing, 0.68%
- cfc170a reviewed alias snapshot, unchanged at e08af639: 19 aliases, 9 covered targets, 582 missing, 1.52%
- reported post-e08af working-tree snapshot: 21 aliases, 11 covered targets, 580 missing, 1.86%; current authority remains the test strategy and post-review document
- proposed v2 seed: 191 aliases, 167 covered targets, 424 missing, 28.26%
- all accepted coverage artifacts use algorithm version 1 and record catalog
  parser commit/path/hash
- the production parser requires the ten documented baseline targets

The pre-existing post-e08af working-tree follow-up is reported to have focused
Python regressions and a runtime alias coverage snapshot for the initial GET
alias, response-rendering, and cleanup-policy helper gaps. The 2026-08-20
registry update was docs-only and did not execute those tests. Broader CI/live
gates, external response dispatch, reliable inventory snapshots, and automatic
cleanup execution remain outside this reported subset and are still governed by
the test strategy and post-review document.

Do not start EQUIP, DEPOSIT, GIVE, automatic inventory-cleanup, or any other
command implementation before the registry/lifecycle Phase 0 prerequisite and
the command's owning contracts and gates are complete.
```
