<!-- 20260820_kpopmodder: Split the full Korean ChatClef command registry plan from item-action alias planning. -->

# ChatClef Python Korean Command Registry Plan

Date: 2026-08-20

This document records the docs-only plan for making the registered ChatClef
command surface understandable from Korean natural language without changing
Java, ChatClef, AltoClef, Baritone, DTOs, or the v1 wire payload.

It is documentation only. It does not approve Python behavior changes, Java
changes, DTO changes, wire-protocol changes, build execution, Minecraft launch,
runtime reproduction, tests, commit, push, automatic replay, broad refactoring,
or public exposure of a command family.

## Authority

This plan owns the Korean command registry contract:

```text
20 registered command snapshot
command-by-command Korean intent grammar
Python slot schema
slot domain
prefixless serializer identifier
exposed Java grammar subset
lifecycle kind
safety tier
confirmation mode
allowed input source
multi-axis implementation status
```

Related documents keep narrower ownership:

```text
chatclef-korean-item-action-alias-v2-plan.md:
  GET/EQUIP/DEPOSIT/GIVE item language, 591 item target classification,
  concrete Korean item aliases, canonical Korean display, equipment composition,
  and item-action target capability.

chatclef-python-command-orchestration-plan.md:
  response wording, exactly-once submission, confirmation state machine,
  persistent-command admission, stop/cancellation handling, and command result
  lifecycle policy.

chatclef-python-inventory-cleanup-preflight-contract.md:
  Python-owned inventory evidence, junk cleanup policy, disposable/protected
  target planning, targeted cleanup command shape, post-cleanup verification,
  and fail-closed primary admission.

chatclef-korean-test-strategy.md:
  source-backed command snapshot tests, command lifecycle compatibility matrix,
  resolver coverage tests, safety/confirmation tests, and live-test gates.

chatclef-korean-post-review-merge-blockers.md:
  implementation status, open blockers, and merge decision.
```

When wording conflicts, this registry plan controls command taxonomy and
per-command exposure classification; the item-action plan controls item aliases
and item-action grammar; the orchestration plan controls runtime sequencing; the
cleanup contract controls junk cleanup; the test strategy controls how the
claims are proven.

## Source Baseline

The reviewed source baseline for this registry split is:

```text
e08af63948a3fa4675c70279db59c2a70b00a332
```

At that baseline, a command can be source-registered without being safe or
complete as a Korean public feature.

## Registered Command Snapshot Contract

The current registered command count is `20`:

| Command | Owner | Registration source | Initial registry status |
| --- | --- | --- | --- |
| `attack` | `chatclef_java` | `AltoClefCommands` | raw only |
| `chatclef` | `chatclef_java` | `AltoClefCommands` | raw only |
| `deposit` | `chatclef_java` | `AltoClefCommands` | planned item-action phase |
| `equip` | `chatclef_java` | `AltoClefCommands` | planned item-action phase |
| `follow` | `chatclef_java` | `AltoClefCommands` | Korean parser/compiler present; persistent lifecycle unresolved |
| `food` | `chatclef_java` | `AltoClefCommands` | Korean parser/compiler present |
| `gamer` | `chatclef_java` | `AltoClefCommands` | raw only; long-running/high impact |
| `gamma` | `chatclef_java` | `AltoClefCommands` | raw only; callback lifecycle unresolved |
| `get` | `chatclef_java` | `AltoClefCommands` | Korean parser/compiler present |
| `give` | `chatclef_java` | `AltoClefCommands` | planned item-action phase |
| `goto` | `chatclef_java` | `AltoClefCommands` | Korean parser/compiler present |
| `hero` | `chatclef_java` | `AltoClefCommands` | raw only; persistent combat |
| `idle` | `chatclef_java` | `AltoClefCommands` | Korean parser/compiler present; persistent lifecycle unresolved |
| `locate_structure` | `chatclef_java` | `AltoClefCommands` | raw only |
| `meat` | `chatclef_java` | `AltoClefCommands` | Korean parser/compiler present |
| `overlay` | `lavi_overlay` | `OverlayCommandRegistrar` | raw only |
| `reload_settings` | `chatclef_java` | `AltoClefCommands` | raw only; admin/control |
| `resetmemory` | `chatclef_java` | `AltoClefCommands` | raw only; admin/control |
| `scan` | `chatclef_java` | `AltoClefCommands` | raw only |
| `stop` | `chatclef_java` | `AltoClefCommands` | Korean parser/compiler present; active-command cancellation lane unresolved |

The registry snapshot must prove actual registration. Do not infer command
support by scanning all Java `Command` subclasses. Commented or unregistered
classes, including `StashCommand` and other unregistered helper commands, are
not part of the registered Korean command surface.

## Status Axes

Do not use one `IMPLEMENTED` label to mean end-to-end Korean command support.
Every command must track these axes separately:

```text
SOURCE_REGISTERED
KOREAN_PARSE_COMPILE_READY
PYTHON_ADMISSION_READY
BRIDGE_LIFECYCLE_READY
GAMEPLAY_EFFECT_VERIFIABLE
PUBLIC_KOREAN_ENABLED
```

Meanings:

| Axis | Meaning |
| --- | --- |
| `SOURCE_REGISTERED` | The command is registered in the Java/Fabric command registration chain. |
| `KOREAN_PARSE_COMPILE_READY` | Python can parse Korean wording and serialize a validated prefixless DSL command. |
| `PYTHON_ADMISSION_READY` | Python precheck, connected/idle/busy policy, exactly-once submission, and no-replay behavior are defined and tested for this command. |
| `BRIDGE_LIFECYCLE_READY` | The command has a source-backed terminal/cancellation lifecycle that the bridge can observe without guessing. |
| `GAMEPLAY_EFFECT_VERIFIABLE` | Python has an approved oracle for the command's expected effect, or the command is explicitly classified as no-effect/read-only. |
| `PUBLIC_KOREAN_ENABLED` | The command may be exposed to ordinary Korean user input under its safety and confirmation policy. |

`KOREAN_PARSE_COMPILE_READY` alone does not imply public enablement.

## Lifecycle Kinds

Each command must be classified with one lifecycle kind:

```text
IMMEDIATE
FINITE_TASK
PERSISTENT_TASK
INTERRUPT_COMMAND
LONG_RUNNING_TASK
CONTROL_PLANE
```

Known lifecycle cautions:

| Command | Caution |
| --- | --- |
| `idle` | `IdleTask.isFinished()` is persistent, not a normal finite completion. |
| `follow` | Following has no ordinary natural completion while the target remains valid. |
| `hero` | Combat/search behavior is persistent or open-ended. |
| `stop` | Java can stop work, but Python submission through the normal active-command lane while another command is active is not proven. |
| `gamma` | The command changes client state, but bridge terminal callback behavior must be source-proven before public enablement. |
| `gamer` | Very long-running/high-impact command; do not expose without strong confirmation and cancellation proof. |
| `scan` | Java may finish quickly, but Python must not claim it has a structured coordinate result unless that result is captured by an approved evidence path. |

## Input Source Policy

Allowed source values:

```text
DIRECT_TYPED_USER
DIRECT_VOICE_USER
LLM_TRANSLATED_USER_INPUT
ORCHESTRATOR_GENERATED
INTERNAL_AUTOMATION
```

High-risk commands must not be executed from LLM or orchestrator autonomy. At
minimum, these commands require direct local user intent and command-bound
confirmation before any future public enablement:

```text
attack
gamer
chatclef
reload_settings
resetmemory
```

## Command Grammar Targets

The following table is a design target, not a current implementation claim.

| Command family | Korean grammar target | Prefixless DSL target | Initial policy |
| --- | --- | --- | --- |
| `get` | `<item> [N개] 가져와/구해/얻어/캐줘/채굴해/만들어줘/제작해줘` | `get <target> <count>` | Preserve existing GET path and no-replay rules. |
| `equip` | `<equipment> 입어/장착해/껴`; closed armor-set shortcut later | `equip <target-or-set>` | Explicit equipment first; capability-gated. |
| `deposit` | `<item> N개 상자에 넣어/보관해` | `deposit <target> <count>` | Specific item/count only at first; no bare deposit. |
| `give` | `<player>에게 <item> [N개] 줘` | `give <player> <target> <count>` | Explicit recipient required; omitted count becomes explicit `1`. |
| `goto` | `x y z로 가/이동해` | `goto <x> <y> <z>` | Keep to a frozen coordinate subset first. |
| `follow` | `<player> 따라가/팔로우해` | `follow <player>` | Do not publicly enable until persistent lifecycle/cancel contract is closed. |
| `attack` | `<mob> [N마리] 공격해/처치해` | `attack <entity> <count>` | Mob only; player target disabled by default. |
| `locate_structure` | closed structure names only | `locate_structure <structure>` | Only source-proven structures; ambiguous names reject. |
| `scan` | `<block> 스캔해/근처에서 찾아봐` | `scan <block-id>` | Block resolver only; do not reuse item aliases blindly. |
| `food` | `음식 N만큼 모아` | `food <units>` | Units are food-value policy, not item count. |
| `meat` | `고기 N만큼 모아` | `meat <units>` | Separate from item aliases. |
| `idle` | `가만히 있어/대기해` | `idle` | Persistent; public enablement blocked by lifecycle/cancel proof. |
| `stop` | `멈춰/중지/정지/그만` | `stop` | Emergency intent; must outrank other verbs, but active-command lane must be proven. |
| `gamma` | `밝기 <값>으로 설정해` | `gamma <finite-double>` | Hold until callback lifecycle and restore policy are tested. |
| `overlay` | `오버레이 켜/꺼` | `overlay on/off` | Reversible client-state command; source-gated. |
| `chatclef` | `ChatClef AI 브리지 켜/꺼` | `chatclef on/off` | Control plane; typed confirmation. |
| `reload_settings` | `ChatClef 설정 다시 불러와` | `reload_settings` | Admin/control; typed confirmation. |
| `resetmemory` | `ChatClef 대화 기록 초기화` | `resetmemory` | Two-step typed confirmation; do not expose as generic "memory reset." |
| `gamer` | `마인크래프트 엔딩까지 진행해` | `gamer` | Disabled by default. |
| `hero` | `적대적 몹 계속 정리해` | `hero` | Persistent combat; disabled until cancellation path is proven. |

`만들어줘` and `제작해줘` remain GET acquisition wording. They must not create
a new Java craft command.

## Resolver Domains

The 591 entries in `CataloguedResources.txt` are item/acquisition targets. They
must not be reused as the universal domain for every command.

| Command | Resolver domain |
| --- | --- |
| `get`, `equip`, `deposit`, `give` | item target resolver |
| `scan` | block resolver |
| `attack` | entity resolver or exact player resolver with player-target rejection policy |
| `locate_structure` | closed structure resolver |
| `goto` | coordinate and optional dimension resolver |
| `follow`, `give` | player-name resolver |
| `gamma` | finite double policy |
| `overlay`, `chatclef` | on/off toggle resolver |

Recommended Python components:

```text
KoreanItemTargetResolver
KoreanBlockTargetResolver
KoreanEntityTargetResolver
KoreanStructureResolver
KoreanDimensionResolver
KoreanToggleResolver
PlayerNameResolver
```

## 591 Target Classification Contract

"All Minecraft items in Korean" means every ChatClef catalog target is
classified. It does not mean every raw catalog line receives an unconditional
public Korean alias.

Each of the 591 catalog targets must have exactly one primary classification:

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

The item-action plan owns the generated alias artifacts. This registry plan
requires command resolvers to consume only the correct domain-specific artifact.

## Safety Tiers

| Tier | Meaning | Examples |
| --- | --- | --- |
| `R0` | emergency, read-only, or reversible | `stop`, `scan`, `overlay`, tightly bounded `gamma` |
| `R1` | ordinary finite gameplay task | `get`, `goto`, `food`, `meat`, `locate_structure`, `equip` |
| `R2` | item transfer or possible resource loss | `deposit`, `give` |
| `R3` | combat, persistent, or long-running | `attack`, `follow`, `idle`, `hero`, `gamer` |
| `R4` | control-plane/admin | `chatclef`, `reload_settings`, `resetmemory` |

Confirmation modes:

```text
NONE
DIRECT_EXPLICIT_INTENT
BOUND_CONFIRMATION
TWO_STEP_TYPED_CONFIRMATION
DISABLED_PENDING_CONTRACT
```

Confirmation must bind:

```text
command
resolved slots
input source
session
inventory snapshot id when applicable
expiry
one-time confirmation id
```

After busy, disconnect, mismatch, unknown, or timeout, confirmation is discarded.
No automatic resubmit is allowed.

## Junk Cleanup Boundary

`잡템` is not an item alias and is not a direct synonym for bare `deposit`.

Correct flow:

```text
잡템 상자에 넣어줘
  -> JUNK_CLEANUP_MODE
  -> require reliable inventory snapshot
  -> classify protected/disposable stacks
  -> calculate exact target and count
  -> submit exactly one targeted command such as deposit dirt 64
  -> require matching terminal
  -> require fresh post-cleanup inventory effect verification
```

If a safe targeted plan cannot be built:

```text
submit count == 0
```

## Phase Order

Recommended implementation order when code changes are later approved:

1. Phase 0 - registry and lifecycle contract freeze.
2. Phase 1 - typed registry infrastructure with public enablement still zero
   for new command families.
3. Phase 2 - 591 target classification and generator.
4. Phase 3 - GET catalog expansion.
5. Phase 4 - item-action compiler for EQUIP, specific DEPOSIT, and GIVE.
6. Phase 5 - cleanup shadow mode; actual cleanup submit remains zero.
7. Phase 6 - cleanup execution using targeted deposit and post-cleanup evidence.
8. Phase 7 - low-risk raw commands such as `scan`, `overlay`, and narrowly
   scoped `locate_structure`.
9. Phase 8 - persistent, combat, and long-running commands after cancellation
   and lifecycle contracts are closed.
10. Phase 9 - admin/control commands after typed confirmation and source policy
    are implemented.

## Required Test Checklist

Command registry:

```text
active registered command count is exactly 20
overlay is included from OverlayCommandRegistrar
commented StashCommand is excluded
unregistered command subclasses are excluded
duplicate command name count is 0
every command has slot schema
every command has lifecycle kind
every command has safety tier
every command has confirmation mode
every command has allowed input source
every command has serializer id or explicit disabled reason
Python serializer output contains no @ prefix
newline, semicolon, hash, and multi-command injection are rejected
```

Lifecycle:

```text
idle is not classified as a finite completed command
follow is not classified as naturally complete
hero is not classified as a one-shot command
active-command stop submission is source-backed before public enablement
gamma callback lifecycle is proven before public enablement
persistent commands have cancellation prerequisites
terminal completed and gameplay effect remain separate
scan completion does not imply Python knows scan coordinates
```

591 catalog:

```text
catalog target count is 591
every target has exactly one primary classification
classification omissions are 0
alias targets outside the catalog are 0
compact alias conflicts are 0
output is deterministic
source SHA-256 values are recorded
official Korean source hash is recorded
canonical display and input aliases are separate
generic/internal targets are not forced into direct public aliases
user-facing coverage and raw catalog coverage are separate
```

Korean parsing and safety examples:

```text
철 10개 캐줘 -> get iron_ingot 10
에메랄드 캐줘 -> get emerald 1
횃불 만들어줘 -> get torch 1
철갑바 만들어줘 -> get iron_chestplate 1
캐나다 / 캐시 / 캐릭터 -> no submit
철학 모자 -> no equipment composition
금요일 바지 -> no equipment composition
scan uses block aliases, not item aliases
attack uses entity aliases, not item aliases
요새 ambiguity is not silently stronghold
잡템 -> direct deposit compiler path count 0
automatic bare deposit output count 0
placeholder quantity output count 0
no-count specific deposit is ambiguous until policy is frozen
attack player is rejected by default
resetmemory is not triggered by generic "기억 지워"
admin commands reject LLM-generated source
confirmation cannot be reused
busy/disconnect/mismatch discards confirmation
accepted/completed/unknown never causes automatic retry or replay
```

## Hard Prohibitions

Do not:

```text
modify Java, ChatClef, AltoClef, Baritone, DTOs, protocol, schema, or wire payload
move the Python intent model into common DTOs
let an LLM generate raw ChatClef DSL
emit @ from the Python compiler
use one generic string serializer for all 20 commands without slot validation
count Java class existence as command registration
reuse the 591 item catalog as block/entity/structure/player resolver authority
force a public alias onto every raw catalog target
use substring alias matching
trust Java fuzzy fallback for scan safety
use implicit Butler/current-user mode for follow or give
treat attack player like ordinary mob attack
treat persistent commands as finite completed commands
mark gamma bridge lifecycle ready without callback proof
assume active-command stop works without source-backed tests
put 잡템 into korean_item_aliases.json
compile 잡템 to bare deposit
submit the primary command based only on cleanup completed
automatically retry, replay, rerun, or resubmit
claim gameplay success from terminal completed alone
```
