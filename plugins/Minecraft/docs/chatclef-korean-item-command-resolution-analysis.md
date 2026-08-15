<!-- 20260815_kpopmodder: Documented Korean item and mining command resolution failures before implementation. -->

# ChatClef Korean Item Command Resolution Analysis

Date: 2026-08-15

This document records the read-only analysis for Korean Minecraft item requests
such as:

```text
다이아몬드 가져와줘
돌 10개 가져와줘
다이아몬드 캐줘
철 10개 캐줘
레드스톤 5개 구해줘
```

It is documentation only. It does not approve Python behavior changes, Java
changes, wire protocol changes, DTO changes, ChatClef / AltoClef engine
changes, build execution, Minecraft launch, runtime reproduction, commit,
push, broad refactoring, or automatic command replay.

## Scope

The issue is in the Python-owned Fabric ChatClef natural-language path:

```text
MinecraftChatClefInputIntentGate
  -> KoreanChatClefRuleParser
  -> KoreanItemPhraseResolver
  -> ChatClefTargetCatalog
  -> ChatClefCommandCompiler
  -> FabricChatClefAdapter
  -> Java bridge
```

The current evidence does not require Java, DTO, payload, or ChatClef engine
changes. The Java bridge should continue to receive already-compiled ChatClef
DSL commands.

## Verified Current Failures

The current code resolves only a small fixed item alias set such as:

```text
철괴 -> iron_ingot
금괴 -> gold_ingot
구운 소고기 -> cooked_beef
스테이크 -> cooked_beef
나무 -> log
```

The following failures were reproduced through the existing natural-language
service:

| Input | Current intent | Item phrase | Quantity | Current result |
| --- | --- | --- | --- | --- |
| `다이아몬드 가져와줘` | `get_item` | `다이아몬드` | `1` | `unknown_item_phrase` |
| `돌 10개 가져와줘` | `get_item` | `돌` | `10` | `unknown_item_phrase` |
| `다이아몬드 캐줘` | `unknown` | empty | none | `unknown_intent` |
| `철 10개 캐줘` | `get_item` | `철 캐줘` | `10` | `unknown_item_phrase` |
| `레드스톤 5개 구해줘` | `get_item` | `레드스톤` | `5` | `unknown_item_phrase` |
| `철괴를 10개 가져와줘` | `get_item` | `철괴를` | `10` | `unknown_item_phrase` |
| `다이아몬드를 가져와줘` | `get_item` | `다이아몬드를` | `1` | `unknown_item_phrase` |

The expected already-working equipment case still resolves:

```text
다이아몬드 곡괭이 하나 가져와 -> get diamond_pickaxe 1
```

## Root Causes

### Fixed item aliases are too narrow

`korean_item_aliases.json` currently contains only a small curated list. Common
standalone item names such as `다이아몬드`, `돌`, `조약돌`, `레드스톤`, and
`석탄` are not fixed item aliases, so they cannot resolve to catalog targets
such as `diamond`, `stone`, `cobblestone`, `redstone`, or `coal`.

### Material aliases are not standalone item aliases

`korean_material_aliases.json` is currently used for equipment composition.
For example:

```text
다이아몬드 + 곡괭이 -> diamond_pickaxe
철 + 삽 -> iron_shovel
금 + 도끼 -> golden_axe
```

Those material values must not be blindly reused as standalone item targets.
The ChatClef catalog contains standalone resource targets such as:

```text
diamond
stone
cobblestone
coal
redstone
iron_ingot
gold_ingot
leather
```

It does not expose raw material values such as these as standalone resource
targets:

```text
iron
golden
wooden
netherite
```

Therefore a generic material fallback would be unsafe. It could accidentally
compile invalid commands such as:

```text
get iron 10
get golden 10
get wooden 10
get netherite 1
```

Prefer explicit fixed item aliases for standalone item requests.

### Mining verbs are missing from the gate and parser

The input gate does not include Korean mining or acquisition verbs such as:

```text
캐줘
캐와
캐오기
채굴해줘
채굴해
```

The rule parser's GET verb expression also does not include those verbs. As a
result, `다이아몬드 캐줘` does not become a GET_ITEM request at all.

`철 10개 캐줘` only becomes GET_ITEM because the quantity parser notices
`10개`; the mining verb remains in the extracted item phrase as `철 캐줘`, so
the resolver still fails.

### Object particles can remain in the item phrase

The parser removes the GET verb by replacing it with a space and then tries to
remove a final `을` or `를`. Because the verb replacement can leave trailing
space, the final particle regex does not match.

Examples:

```text
철괴를 10개 가져와줘 -> item_phrase = 철괴를
다이아몬드를 가져와줘 -> item_phrase = 다이아몬드를
```

This means alias additions alone are not enough. The parser must also normalize
or strip after verb removal before final object-particle removal.

## Target Resolution Policy

Use this future policy for standalone item requests:

```text
다이아몬드 -> diamond
돌 -> stone
조약돌 -> cobblestone
레드스톤 -> redstone
석탄 -> coal
철 -> iron_ingot
금 -> gold_ingot
나무 -> log
```

`돌 -> stone` and `조약돌 -> cobblestone` are compatible with the current
catalog because both `stone` and `cobblestone` are present targets.

`다이아몬드 -> diamond` is compatible with the current resolver priority because
equipment composition runs before fixed item aliases. This preserves:

```text
다이아몬드 곡괭이 -> diamond_pickaxe
다이아몬드 도끼 -> diamond_axe
```

Fixed item aliases should remain exact compact matches. Do not convert them to
substring matches.

## Acquisition Verb Policy

Mining words such as `캐줘` and `채굴해줘` should map to the existing GET_ITEM
intent. Do not introduce a new mining wire intent for this request.

The command meaning remains:

```text
user Korean item request
  -> GET_ITEM intent
  -> validated catalog target
  -> prefixless ChatClef DSL command
```

For example:

```text
다이아몬드 캐줘 -> get diamond 1
돌 10개 캐줘 -> get stone 10
철 10개 캐줘 -> get iron_ingot 10
```

If a response wants to say "캐오는 중" instead of "가져오는 중", that should be a
Python-side response wording concern, not a new Java command type.

## Shared Verb Matching

The gate and parser should not drift apart again. A future implementation
should put the Korean acquisition verb definitions in one Python-owned helper,
for example:

```text
plugins/Minecraft/fabric/chatclef/intent/korean_acquisition_verb_matcher.py
```

The matcher should use explicit Korean verb forms or regex word boundaries
appropriate for Korean command phrases. Avoid broad substring checks such as:

```text
"캐" in text
```

That can create false positives for unrelated words such as:

```text
캐나다
캐릭터
캐시
```

## Router Boundary For Minecraft-Active Behavior

The gate and parser should stay pure:

```text
Gate:
  lexical Minecraft-command candidate check

Parser:
  intent, quantity, item phrase, coordinate, follow, stop, idle extraction

Resolver:
  Korean phrase to one catalog target

Compiler:
  validated slots to safe ChatClef DSL

Router:
  connected state, active command policy, fall-through, and submission
```

Do not pass Minecraft connection state into `should_consider()`. Connection
state can be stale between a precheck and final submission. The final authority
for one-active-command and sendability remains the adapter, transport, and
connection ownership layer.

Recommended future routing policy:

| Translation status | Minecraft connected and idle | Minecraft disconnected or unavailable |
| --- | --- | --- |
| `validated` | submit once | handled connection error by default |
| `unknown` | LLM fall-through | LLM fall-through |
| `ambiguous` | LLM fall-through or explicit clarification | LLM fall-through or explicit clarification |
| `unsupported` | LLM fall-through or explicit unsupported reply | LLM fall-through or explicit unsupported reply |
| `invalid` | handled rejection | handled rejection |
| `internal_error` | handled error | handled error |

If a future setting allows disconnected Minecraft commands to fall through to
the general LLM, that setting should be explicit. The default should not make
validated Minecraft commands disappear into normal chat when Minecraft is
offline.

## Prefixless Command Contract

The Python compiler currently emits prefixless ChatClef DSL:

```text
get diamond 1
get stone 10
get iron_ingot 10
```

It should not emit:

```text
@get diamond
```

The compiler treats `@` as dangerous slot text and rejects it. If the Java side
or ChatClef executor needs a command prefix internally, that prefix belongs at
the Java bridge or executor boundary, not in the Korean natural-language
compiler.

## Current Double Translation Risk

The current router translates once for routing:

```text
translate_natural_language_command(text)
```

Then it calls:

```text
handle_natural_language_command(request)
```

The handler translates the same text again before submission. This is mostly
stable while the translator is deterministic, but it can become unsafe if a
future LLM extractor is added or if mutable context affects translation.

A future Python-only extension API should submit an already validated
translation without re-translating:

```text
submit_translated_command(request, validated_translation)
```

## Future Files To Modify

The expected future implementation can stay Python-only:

```text
plugins/Minecraft/fabric/chatclef/input/minecraft_chatclef_input_intent_gate.py
plugins/Minecraft/fabric/chatclef/input/minecraft_chatclef_input_router.py
plugins/Minecraft/fabric/chatclef/extension/minecraft_fabric_chatclef_extension.py
plugins/Minecraft/fabric/chatclef/intent/korean_chatclef_rule_parser.py
plugins/Minecraft/fabric/chatclef/intent/korean_item_phrase_resolver.py
plugins/Minecraft/fabric/chatclef/intent/resources/korean_item_aliases.json
plugins/Minecraft/fabric/chatclef/intent/korean_acquisition_verb_matcher.py
```

The following should remain untouched unless later evidence proves a separate
need:

```text
plugins/Minecraft/common/dto/**
plugins/Minecraft/common/protocol/**
plugins/Minecraft/common/schema/**
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/**
```

## Test Plan For Later Implementation

Reuse and extend the existing Python tests:

```text
tests/test_minecraft_chatclef_korean_rule_parser.py
tests/test_minecraft_chatclef_item_phrase_resolver.py
tests/test_minecraft_chatclef_natural_language_service.py
tests/test_minecraft_chatclef_korean_command_integration.py
tests/test_minecraft_chatclef_command_compiler.py
tests/test_minecraft_chatclef_input_router.py
```

Add or update cases for:

```text
다이아몬드 가져와줘 -> get diamond 1
다이아몬드 캐줘 -> get diamond 1
다이아몬드를 가져와줘 -> get diamond 1
돌 10개 가져와줘 -> get stone 10
돌을 10개 캐줘 -> get stone 10
조약돌 10개 캐줘 -> get cobblestone 10
레드스톤 5개 구해줘 -> get redstone 5
석탄 5개 캐와줘 -> get coal 5
철 10개 캐줘 -> get iron_ingot 10
금 3개 캐줘 -> get gold_ingot 3
다이아몬드 곡괭이 하나 가져와 -> get diamond_pickaxe 1
캐나다 여행 얘기하자 -> no Minecraft route
캐릭터 만들어줘 -> no invalid ChatClef item submission
```

Also add router tests for:

```text
validated + connected + idle -> submit once
validated + disconnected -> handled connection error by default
unknown -> LLM fall-through
invalid -> handled rejection
busy/rejected/disconnected/accepted -> no automatic retry or replay
already validated translation -> submitted without translating a second time
```

## Implementation Order

Use this order when the user later approves code changes:

1. Add focused failing tests for current behavior.
2. Add or centralize acquisition verb matching.
3. Update the gate to recognize safe acquisition verbs.
4. Update the parser to remove acquisition verbs and trailing object particles
   correctly.
5. Add explicit fixed item aliases and verify every target against the catalog.
6. Preserve equipment-before-fixed-alias resolver priority.
7. Keep compiler output prefixless.
8. Add the single-translation submission path in the extension and router.
9. Add connected-aware router policy without changing Java or DTOs.
10. Run the focused Python test set.

Do not combine this with inventory cleanup, Java bridge changes, or ChatClef
engine changes.

## Risks To Preserve

Future implementation should explicitly guard against:

```text
duplicate translation
duplicate command submission
precheck-to-submit connection races
stale status snapshots
shutdown while routing
active-command TOCTOU races
automatic retry after busy or disconnected
automatic replay after accepted
alias collisions between item and equipment phrases
invalid raw material targets
accidental @ prefix emission from Python
over-broad mining verb substring matches
```

