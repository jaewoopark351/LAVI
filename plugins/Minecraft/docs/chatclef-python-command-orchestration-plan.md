<!-- 20260815_kpopmodder: Documented the Python-only plan for ChatClef command replies, Korean mining phrases, and inventory preflight cleanup. -->

# ChatClef Python Command Orchestration Plan

Date: 2026-08-15

This document records the pre-implementation plan for three requested Fabric
ChatClef behaviors:

```text
1. Emit a short LAVI-side reply when a Minecraft command is routed.
2. Treat Korean mining phrases such as "철 10개 캐줘" as existing GET_ITEM
   commands.
3. Handle inventory-full cleanup from Python only, using existing ChatClef
   commands, without adding Java behavior.
```

It is documentation only. It does not approve Python behavior changes, Java
changes, wire protocol changes, DTO changes, build execution, Minecraft launch,
runtime reproduction, commit, push, broad refactoring, file moves, or automatic
replay logic.

## Scope

Current implementation scope remains Fabric ChatClef only:

```text
LAVI
  -> Fabric adapter
    -> Fabric ChatClef bridge
      -> ChatClef / AltoClef
```

Do not add Forge, MineMind, shared backend code, shared Java runtime, shared
session registries, shared reconnect managers, or shared GUI components for
this work.

The plan applies to the Python-owned Fabric ChatClef path:

```text
plugins/Minecraft/fabric/chatclef/**
llm_core/**
app_core/composition_core/**
```

The current v1 wire DTOs and Java bridge remain unchanged:

```text
plugins/Minecraft/common/dto/**
plugins/Minecraft/common/protocol/**
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/**
```

## Non-Goals

Do not implement any of these as part of the first documentation-approved
phase:

```text
Java inventory cleanup behavior
ChatClef / AltoClef Task changes
Baritone path, goal, or input cleanup changes
CommandResultDTO shape changes
BridgeEnvelopeDTO shape changes
new wire message types
automatic retry after timeout or unknown status
automatic replay after a primary command has been accepted
inventory-full inference from logs or long-running tasks
unconditional deposit
TTS calls directly from Minecraft transport code
LLM generation of raw ChatClef DSL
```

## Current Code Observations

The current code largely matches the proposed boundary, but some required
extension points do not exist yet.

### LAVI Reply Path

`MinecraftChatClefInputRouter` already creates a technical routed response:

```text
[Minecraft] command sent: get gold_ingot 8
```

The response is returned through `MinecraftChatClefInputRouteDecision`, but
`LLM.predict_wrapper()` currently yields the routed response and returns. It
does not route the response through the normal LAVI output path:

```text
LLM.send_output()
  -> Translate.receive_input()
  -> TTS.receive_input()
```

Therefore direct chat UI output is possible, but the existing routed response
is not guaranteed to reach Translate/TTS listeners.

The reply feature should be implemented as a general external-response
dispatch API on the LLM side, not as a Minecraft-specific branch inside TTS or
transport code.

### Korean Mining Phrase Gap

The current Korean command path has these gaps:

```text
Input: 철 10개 캐줘

Intent gate:
  "캐줘", "캐와", "캐오기", and "채굴해줘" are not trigger terms.

Rule parser:
  _GET_VERB_RE does not include mining/acquisition verbs such as "캐줘" or
  "채굴해줘".

Item aliases:
  korean_item_aliases.json contains "철괴" and "철 주괴", but not the standalone
  alias "철".
```

`korean_material_aliases.json` maps standalone "철" to the material `iron`,
which supports equipment composition such as "철 곡괭이". That material alias
does not make standalone "철" resolve to `iron_ingot`.

The safe translation target for standalone "철" is currently `iron_ingot`,
because the public target catalog exposes `iron_ingot` while `raw_iron` or
`iron_ore` are not guaranteed public ChatClef targets.

### Natural-Language Double Translation

The current input router first calls:

```text
translate_natural_language_command(text)
```

and then calls:

```text
handle_natural_language_command(request)
```

The handler translates again. With only deterministic rule parsing this usually
produces the same result, but future LLM extraction could make the two results
diverge. A later implementation should expose a Python-only API that submits a
previously validated translation without re-translating:

```text
submit_translated_command(request, validated_translation)
```

### Command Submission Flow

The current immediate submission flow is:

```text
user text or GUI command
  -> CommandRequestDTO
  -> MinecraftFabricChatClefExtension.handle_command()
  -> FabricChatClefAdapter.submit_command()
  -> FabricChatClefWebSocketServer.submit_command()
  -> FabricChatClefConnectionOwnership.begin_command()
  -> BridgeEnvelopeDTO(message_type=command_request)
  -> asyncio.run_coroutine_threadsafe(_send_envelope)
  -> immediate CommandResultDTO(status=accepted) on send success
```

The Python transport allows only one active command. If an active request
exists, a second command is rejected before it reaches Java.

### Command Result Flow

The current Java-to-Python result flow is:

```text
Java command_result envelope
  -> FabricChatClefWebSocketServer._handle_command_result()
  -> CommandResultDTO.from_mapping(envelope.payload)
  -> FabricChatClefConnectionOwnership.accept_result()
  -> last_result snapshot update
  -> active command cleared for terminal statuses
  -> diagnostics log
```

`accept_result()` validates:

```text
active websocket
active command existence
connection generation
session_id
request_id
correlation_id
```

Only accepted results should be observed by future orchestration. Rejected
stale results, wrong-session results, wrong-generation results, wrong-request
results, and wrong-correlation results must not advance a workflow.

There is currently no Python event publisher that lets a higher-level
orchestrator subscribe to accepted command results or disconnect events.

### Inventory Snapshot Gap

The current Python bridge status contains command/session fields such as:

```text
active_request_id
active_command
active_command_message_id
active_command_source
active_age_ms
last_result
session_id
connection_generation
```

It does not currently provide reliable inventory fields such as:

```text
free_inventory_slots
occupied_slots
inventory_items
inventory_full
```

Therefore Python cannot currently decide that inventory is full from bridge
data alone. A first implementation must treat inventory capacity as
`UNKNOWN` unless a separate reliable Python inventory provider is explicitly
introduced.

## Required Architecture

The safe responsibility split is:

```text
Korean input
  -> Intent Gate / Rule Parser / Alias Resolver
  -> one validated ChatClef command
  -> Python Command Orchestrator
       -> user-facing reply rendering
       -> optional inventory preflight cleanup
       -> sequential command submission
       -> accepted result event handling
  -> FabricChatClefAdapter
  -> existing Java bridge
  -> existing ChatClef / AltoClef engine
```

All new command sequencing belongs in Python. Java remains the executor of a
single command request at a time, not the owner of multi-step LAVI workflows.

## Reply Rendering

Add a deterministic response renderer before considering any LLM paraphrasing:

```text
plugins/Minecraft/fabric/chatclef/response/
  chatclef_command_response_renderer.py
```

The renderer should consume already-validated facts:

```text
phase
intent_type
display_item
quantity
submission_status
error_code
terminal_status
```

It must not parse raw DSL such as `get iron_ingot 10` to recover meaning.

Example reply policy:

```text
primary accepted:
  철 10개 캐러 갈게요.

cleanup starting:
  인벤토리부터 정리하고 철 10개 캐러 갈게요.

already busy:
  지금 다른 작업을 하고 있어요.

bridge disconnected:
  마인크래프트 연결이 끊겨 있어요.

cleanup failed:
  인벤토리를 정리하지 못해서 작업을 시작하지 않았어요.

primary completed:
  철 10개 수집 끝났어요.

primary failed:
  철 수집에 실패했어요.

primary unknown:
  철 수집 결과를 확인하지 못했어요.
```

The initial reply path should use deterministic templates only. A future LLM
paraphraser may be added behind the same interface, but it must receive only
confirmed facts and may change tone only. It must not change command status,
command text, lifecycle state, retry policy, or cleanup policy.

Recommended LLM-side dispatch boundary:

```text
emit_external_response(
    text,
    source="minecraft_chatclef",
    send_output=True,
    send_full_output=False,
    remember_history=False,
)
```

This API should:

```text
show the message in the LAVI chat/console surface
send the message through the existing output listener path
preserve response-generation behavior when needed by TTS queues
avoid calling the selected LLM provider
avoid reinserting the message through llm.receive_input()
avoid saving the message to ordinary chat history by default
```

## Korean Mining Phrase Handling

Do not add a new mining command type for this phase. Treat Korean mining
phrases as another way to request the existing GET_ITEM intent:

```text
철 10개 캐줘
  -> intent_type = get_item
  -> target = iron_ingot
  -> quantity = 10
  -> command = get iron_ingot 10
```

Use a shared matcher so the gate and parser do not drift:

```text
plugins/Minecraft/fabric/chatclef/intent/
  korean_acquisition_verb_matcher.py
```

Minimum responsibilities:

```text
matches(text)
classify(text)   # acquire | mining | craft, if classification is useful
strip(text)
```

Initial phrases to recognize:

```text
캐줘
캐 주세요
캐와
캐 와
캐와줘
캐 와줘
캐오기
캐 와서 가져와
채굴해줘
채굴해 주세요
채굴해
채굴해서 가져와
```

Do not use a broad substring trigger for `캐`. It can falsely match phrases
such as:

```text
캐나다 여행 이야기해줘
캐릭터 10개 만들어볼까
캐시가 10개 남았어
```

The parser should normalize item phrases in this order:

```text
1. remove quantity
2. remove acquisition/mining verb phrase
3. normalize spaces
4. remove trailing object particle 을/를
5. remove soft words such as 좀, 제발, 주세요, 줘
6. normalize spaces again
```

Add the standalone alias:

```json
"철": "iron_ingot"
```

The resolver should preserve equipment priority:

```text
철 곡괭이 -> iron_pickaxe
철 도끼   -> iron_axe
철        -> iron_ingot
철괴      -> iron_ingot
철 주괴   -> iron_ingot
```

## Python Command Orchestrator

Add a Python-owned orchestrator only after the reply and transport-event
boundaries are clear:

```text
plugins/Minecraft/fabric/chatclef/orchestration/
  minecraft_chatclef_command_orchestrator.py
```

The orchestrator owns:

```text
operation creation
inventory preflight evaluation
cleanup command submission
waiting for cleanup terminal result
primary command first submission
terminal result handling
duplicate result suppression
exactly-once start and terminal replies
automatic replay prevention
busy response while an operation is active
```

It must not own:

```text
Korean parsing
DSL compiling
WebSocket JSON parsing
Java task manipulation
direct TTS calls
direct Minecraft engine calls
```

Suggested operation context:

```text
operation_id
original_text
validated_translation
primary_command
primary_request_id
cleanup_request_id
state
cleanup_attempt_count
primary_was_accepted
start_response_emitted
terminal_response_emitted
```

Each child command must have its own request ID:

```text
lavi-mc-op-abc-cleanup-1
lavi-mc-op-abc-primary-1
```

Do not reuse request IDs, command message IDs, session IDs, or connection
generations as operation IDs.

## Transport Event Publisher

The current transport validates command results and logs them, but it does not
publish accepted results to a higher-level Python workflow.

Add a Python-local publisher:

```text
plugins/Minecraft/fabric/chatclef/events/
  fabric_chatclef_transport_event_publisher.py
```

Event kinds:

```text
command_result_accepted
connection_lost
server_stopped
```

Event payload should wrap existing objects instead of changing DTOs:

```text
result: CommandResultDTO
session_id
connection_generation
command_message_id
received_at_ms
```

Publish only after `FabricChatClefConnectionOwnership.accept_result()` returns
accepted. Rejected stale or mismatched results must not be published.

For terminal results, publish after the Python active command has been cleared.
This preserves the existing single-active-command boundary before an
orchestrator decides what to do next.

The adapter may expose only narrow subscription methods:

```text
subscribe_transport_events(callback)
unsubscribe_transport_events(callback)
```

Transport event callbacks must not submit another command inline. They should
enqueue events for an orchestrator worker to process.

## State Machine

Recommended initial orchestrator states:

```text
IDLE
PREFLIGHT_EVALUATION
CLEANUP_SUBMITTING
WAITING_CLEANUP_TERMINAL
PRIMARY_SUBMITTING
WAITING_PRIMARY_TERMINAL
COMPLETED
FAILED_BEFORE_PRIMARY
FAILED
MANUAL_RECOVERY_REQUIRED
ABORTED
```

Safe transitions:

```text
IDLE
  -> PREFLIGHT_EVALUATION

PREFLIGHT_EVALUATION
  -> PRIMARY_SUBMITTING when inventory is AVAILABLE
  -> PRIMARY_SUBMITTING when inventory is UNKNOWN
  -> CLEANUP_SUBMITTING when inventory is FULL and targeted cleanup is safe
  -> FAILED_BEFORE_PRIMARY when inventory is FULL and cleanup is unsafe

CLEANUP_SUBMITTING
  -> WAITING_CLEANUP_TERMINAL when cleanup is accepted
  -> FAILED_BEFORE_PRIMARY when cleanup is immediately rejected

WAITING_CLEANUP_TERMINAL
  -> PRIMARY_SUBMITTING only when cleanup completed
  -> FAILED_BEFORE_PRIMARY for rejected, failed, cancelled, deadline_exceeded, or unknown
  -> ABORTED on disconnect

PRIMARY_SUBMITTING
  -> WAITING_PRIMARY_TERMINAL when primary is accepted
  -> FAILED_BEFORE_PRIMARY when primary is immediately rejected

WAITING_PRIMARY_TERMINAL
  -> COMPLETED on completed
  -> FAILED on failed, cancelled, deadline_exceeded, or unknown
  -> MANUAL_RECOVERY_REQUIRED on explicit inventory-full terminal evidence
  -> ABORTED on disconnect
```

When inventory state is `UNKNOWN`, preserve existing behavior by submitting the
primary command without cleanup. Do not run automatic cleanup from unknown
inventory evidence.

### Cleanup Then Primary Gate

The orchestrator may submit the primary command after cleanup only when all of
these are true:

```text
cleanup request was accepted
cleanup terminal status is completed
active command has been cleared by transport ownership
the operation ID still matches
cleanup_attempt_count <= 1
primary_was_accepted is false
```

If any condition is false, do not submit the primary command automatically.

### Replay Guard

The most important safety assertion:

```text
if primary_was_accepted == true:
    the same primary_command must never be automatically submitted a second time
```

This guard is required because ChatClef `get item count` behaves like an
additional acquisition target relative to current inventory in the existing
Java command implementation. Replaying `get iron_ingot 10` after partial
progress can request 10 more items, not the remaining amount.

## Inventory Snapshot Provider

Use a three-state model:

```text
AVAILABLE
FULL
UNKNOWN
```

Suggested snapshot shape:

```text
free_slots: int | None
occupied_slots: int | None
items: item/count list
source: screen_vision | external | manual | none
confidence: float
observed_at_ms: int
state: AVAILABLE | FULL | UNKNOWN
```

The default provider should be fail-safe:

```text
NoReliableInventorySnapshotProvider -> UNKNOWN
```

Do not infer inventory-full from:

```text
latest.log strings
long task duration
pathing stalls
unknown terminal result
deadline_exceeded terminal result
last_result polling alone
```

If ScreenVision is later used, it should return `FULL` only when the player
inventory screen and slot occupancy are confidently observed from a recent
snapshot. Otherwise it must return `UNKNOWN`.

## Cleanup Policy

Add a separate policy object:

```text
plugins/Minecraft/fabric/chatclef/orchestration/
  chatclef_inventory_cleanup_policy.py
```

Recommended modes:

```text
off
targeted
deposit_all_non_gear
```

Default should be `off` or `targeted` with reliable inventory snapshots.
`deposit_all_non_gear` must be explicit opt-in only.

Do not treat argument-less `deposit` as "store junk only". The existing
ChatClef command can store many non-gear items, including food, torches, fuel,
building blocks, ingots, and materials needed by the active task.

Targeted cleanup must protect at least:

```text
current primary target
materials required for the primary target
tools
armor
food
torches
fuel
movement/building essentials
rare items
user-protected item list
```

Do not create placeholder quantities such as:

```text
deposit dirt 9999
```

Cleanup should be limited per operation:

```text
max_cleanup_attempts = 1
```

## Implementation Phases

### Phase A: Korean Phrases And Start Replies

```text
1. Add korean_acquisition_verb_matcher.py.
2. Connect gate and rule parser to the shared matcher.
3. Add "철": "iron_ingot" alias.
4. Add parser/resolver/compiler tests for Korean mining phrases.
5. Add deterministic response renderer.
6. Add a general LLM external-response dispatch API.
```

This phase must not add orchestration or inventory cleanup.

### Phase B: Python Transport Events

```text
1. Add Fabric ChatClef transport event publisher.
2. Publish only accepted command results.
3. Publish terminal events after Python active command clear.
4. Publish disconnect/server-stopped events.
5. Expose narrow adapter subscription methods.
6. Test stale result suppression and subscriber exception isolation.
```

This phase must not automatically submit cleanup or primary follow-up commands.

### Phase C: Orchestrator Without Cleanup

```text
1. Add operation context and state.
2. Consume transport events through a worker queue.
3. Submit primary commands only.
4. Emit start and terminal replies exactly once.
5. Implement busy/disconnect/unknown/replay guards.
```

### Phase D: Inventory Preflight Cleanup

```text
1. Add InventorySnapshotProvider interface.
2. Default provider returns UNKNOWN.
3. Add cleanup policy.
4. Support targeted cleanup only with reliable FULL evidence.
5. Submit primary only after cleanup completed and replay guard passes.
6. Keep cleanup attempt count capped at one.
```

## Existing Tests To Reuse

Korean parsing and routing:

```text
tests/test_minecraft_chatclef_input_router.py
tests/test_llm_minecraft_input_router.py
tests/test_minecraft_chatclef_command_compiler.py
tests/test_minecraft_chatclef_korean_command_integration.py
tests/test_minecraft_chatclef_korean_gui.py
tests/test_minecraft_chatclef_korean_intent_schema.py
tests/test_minecraft_chatclef_korean_rule_parser.py
tests/test_minecraft_chatclef_natural_language_service.py
tests/test_minecraft_chatclef_item_phrase_resolver.py
```

Fabric ChatClef transport and contracts:

```text
tests/test_minecraft_fabric_chatclef_transport.py
tests/test_minecraft_fabric_chatclef_java_bridge_contract.py
tests/test_minecraft_fabric_chatclef_adapter_skeleton.py
tests/test_minecraft_fabric_chatclef_extension.py
tests/test_minecraft_fabric_chatclef_gui.py
tests/test_minecraft_bridge_protocol.py
```

LLM output/listener behavior:

```text
tests/test_event_listener_isolation.py
tests/test_llm_memory_bridge.py
tests/test_tts_queue_worker.py
```

## New Tests To Add Later

Suggested new or expanded tests:

```text
tests/test_minecraft_chatclef_korean_acquisition_verbs.py
tests/test_minecraft_chatclef_command_response_renderer.py
tests/test_llm_external_response_dispatcher.py
tests/test_minecraft_fabric_chatclef_transport_events.py
tests/test_minecraft_chatclef_command_orchestrator.py
tests/test_minecraft_chatclef_inventory_cleanup_policy.py
```

Required Korean success cases:

```text
철 10개 캐줘
철을 10개 캐줘
철 10개 캐와
철을 10개 캐와줘
철 10개 캐오기
철 10개 채굴해줘
철을 10개 채굴해 주세요
철 캐줘 -> quantity=1
```

All should compile to:

```text
get iron_ingot 10
```

Required false positives:

```text
캐나다 여행 이야기해줘
캐릭터 10개 만들어볼까
캐시가 10개 남았어
오늘 사과 10개 먹었어
```

Required reply/output tests:

```text
accepted primary emits one start reply
immediate rejected does not emit "going" phrasing
busy emits one busy reply
disconnected emits one connection error reply
routed response reaches Translate listener exactly once
routed response reaches TTS listener exactly once through existing path
LLM provider is not called
llm.receive_input recursion does not happen
unknown intent falls through to normal LLM path
Minecraft reply is not saved to ordinary chat history by default
```

Required transport-event tests:

```text
accepted running result publishes one event
accepted terminal result publishes after active command clear
wrong request_id does not publish
wrong session_id does not publish
wrong correlation_id does not publish
old connection_generation does not publish
duplicate terminal result does not publish twice
subscriber exception does not stop the server
disconnect while active publishes connection_lost
event callback does not submit inline
```

Required orchestrator tests:

```text
inventory AVAILABLE -> primary submitted once
inventory UNKNOWN -> primary submitted once, no cleanup
inventory FULL with safe targeted cleanup -> cleanup completed, then primary once
cleanup rejected/failed/cancelled/deadline/unknown -> primary not submitted
cleanup disconnect -> primary not submitted
primary completed -> terminal reply once
primary failed/cancelled/deadline/unknown -> terminal reply once, no replay
primary inventory-full -> no replay, manual recovery required
new user command during operation -> busy, no concurrent submit
cleanup attempts capped at one
app restart does not replay prior operation
```

## Payload, DTO, And Java Feasibility

The requested behavior is feasible without changing:

```text
CommandRequestDTO
CommandResultDTO
StatusSnapshotDTO
BridgeEnvelopeDTO
BridgeMessageType
v1 JSON schema
Fabric Java bridge envelope shape
ChatClef / AltoClef engine code
```

New Python event wrappers and orchestration state should sit outside the v1
wire protocol. They may reference DTO objects but must not add fields to them.

## Implementation Risks To Recheck

Before implementation, recheck these risks against the current branch:

```text
deadlock:
  Do not submit the next command from the WebSocket result callback or event
  loop thread.

duplicate submission:
  Do not allow more than one active command and do not replay accepted primary
  commands.

shutdown race:
  Event subscribers and orchestrator workers must detach cleanly on stop.

stale result:
  Do not advance an operation from mismatched request, session, correlation, or
  connection generation.

duplicate terminal:
  Terminal replies must be emitted exactly once.

inventory uncertainty:
  UNKNOWN must not trigger cleanup.

deposit safety:
  Argument-less deposit is not safe as a default junk cleanup.

LLM coupling:
  LLM paraphrasing must never decide state transitions or generate raw
  ChatClef commands.

history pollution:
  Minecraft acknowledgement replies should not be stored as ordinary chat
  history by default.
```

## Final Boundary

The implementation must preserve this rule:

```text
Korean input expands only into existing validated ChatClef commands.
User-facing replies are emitted by the Python/LAVI output pipeline.
Inventory cleanup is Python-owned preflight orchestration only.
After a primary command is accepted, automatic replay of that command is
forbidden unless a later approved design provides exact remaining quantity or a
safe idempotent command contract.
```
