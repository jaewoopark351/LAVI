<!-- 20260815_kpopmodder: Documented the Python-only plan for ChatClef command replies, Korean mining phrases, and inventory preflight cleanup. -->
<!-- 20260819_kpopmodder: Recorded the implemented single-pass canonical submission and reconciliation boundary. -->
<!-- 20260819_kpopmodder: Bound Korean response and cleanup planning to reviewed archive e08af639 and split inventory cleanup into its own fail-closed contract. -->

# ChatClef Python Command Orchestration Plan

Date: 2026-08-15

This document records the design and reviewed-baseline status for the requested
Fabric ChatClef Python orchestration behaviors:

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

## Document Authority And Reviewed Baseline

Reviewed archive baseline:

```text
e08af63948a3fa4675c70279db59c2a70b00a332
```

This document owns Python user replies, lifecycle wording, operation context,
single-pass submission, and the high-level command-orchestration flow.

Implemented at the reviewed archive baseline:

```text
shared Korean acquisition matcher
철 10개 캐줘 / 캐오기 / 캐와줘 family
prefixless GET compilation
single-pass submission and reconciliation latch
```

Planned, not implemented at that baseline:

```text
deterministic Korean response renderer
normal LAVI output listener dispatch
accepted-result event publisher
command orchestrator
inventory snapshot provider
automatic cleanup
```

Inventory cleanup details are intentionally not owned here. The fail-closed
cleanup evidence, protected-item policy, postcondition, and no-replay contract
are owned by:

```text
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
```

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

## Hard Prohibitions

This work MUST NOT:

```text
modify any file under
  plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/**
modify plugins/Minecraft/common/dto/**
modify plugins/Minecraft/common/protocol/**
modify plugins/Minecraft/common/schema/**
add, remove, rename, or reinterpret any v1 wire message or payload field
add fields to CommandRequestDTO, CommandResultDTO, StatusSnapshotDTO,
  or BridgeEnvelopeDTO
add a new Java craft command or change GetCommand semantics
change DepositCommand, EquipCommand, GiveCommand, or ChatClef command grammar
change ChatClef, AltoClef, Baritone task selection, retry, timeout,
  completion, container transfer, or goal behavior
implement Korean parsing, aliases, responses, junk policy, or orchestration
  in Java
infer inventory-full from logs, task duration, path stalls, timeout,
  deadline, or UNKNOWN results
infer gameplay effect from ACCEPTED, RUNNING, terminal COMPLETED,
  active-request clearing, or connection state alone
use argument-less deposit as automatic junk cleanup
generate placeholder cleanup quantities
automatically retry, replay, rerun, or resubmit cleanup or primary commands
submit a follow-up command inline from a WebSocket callback
let an LLM generate raw ChatClef DSL or decide workflow state transitions
emit an @ prefix from the Python natural-language compiler
put contextual pseudo-targets such as 잡템, 갑옷, player names, quantities,
  or command modes into korean_item_aliases.json
introduce Forge MineMind fallback or shared Fabric/Forge orchestration
```

If an implementation appears to require any prohibited change, stop the phase
and open a separate evidence-backed design review. Do not expand this scope
implicitly.

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

### Historical Korean Mining Phrase Gap Before cfc170a

Before the reviewed Korean GET fixes, the Korean command path had these gaps.
Do not read this historical section as the current behavior at reviewed archive
baseline `e08af63948a3fa4675c70279db59c2a70b00a332`:

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

### Natural-Language Single-Pass Translation

At reviewed archive baseline `e08af639`, the Python router already preserves the
single-pass translation boundary:

```text
MinecraftChatClefTranslationBoundary.translate_once(text)
  -> one validated translation
MinecraftChatClefSubmissionBoundary.submit_once(request, validated_translation)
  -> submit_translated_command(request, validated_translation)
```

Future response rendering, orchestration, cleanup preflight, or event publishing
must preserve this invariant. Do not re-run natural-language translation in the
submission layer, and do not let cleanup planning reinterpret the raw Korean
input after a validated primary command has already been produced.

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
runtime_completion_status
gameplay_observation_complete
expected_gameplay_effect_verified
prohibited_effect_absence_verified
```

It must not parse raw DSL such as `get iron_ingot 10` to recover meaning.

Example reply policy:

```text
primary accepted:
  철 10개 수집 명령을 제출했어요.

cleanup starting:
  인벤토리 정리 명령을 제출했어요. 정리 결과를 확인하기 전에는 철 수집 명령을 보내지 않아요.

already busy:
  지금 다른 작업을 하고 있어요.

bridge disconnected:
  마인크래프트 연결이 끊겨 있어요.

cleanup failed:
  인벤토리를 정리하지 못해서 작업을 시작하지 않았어요.

primary completed:
  철 10개 수집 명령의 완료 응답을 받았어요. 아이템 증가는 별도로 확인해야 해요.

primary gameplay effect verified:
  철 10개 수집 결과를 확인했어요.

primary failed:
  철 수집에 실패했어요.

primary unknown:
  철 수집 결과를 확인하지 못했어요.
```

The initial reply path should use deterministic templates only. A future LLM
paraphraser may be added behind the same interface, but it must receive only
confirmed facts and may change tone only. It must not change command status,
command text, lifecycle state, retry policy, or cleanup policy.

### Response Evidence And Wording Contract

User-facing Korean replies must not claim more than Python has proved:

| Python evidence | Allowed response meaning | Forbidden overclaim |
| --- | --- | --- |
| translation validated | understood, or no reply | submitted, started |
| submission accepted | command was submitted | work started, mining started |
| trusted runtime running | runtime appears to be executing | completed |
| terminal completed | terminal response was observed | item count increased |
| gameplay effect verified | requested effect was verified | none |
| unknown | result unclear; no next step | success, completion, retry |

For example, after accepted submission the safe default is:

```text
철 10개 수집 명령을 제출했어요.
```

It is too strong to say "철 10개 캐러 갈게요" at accepted time, because Python
does not know whether AltoClef will mine, craft, trade, pick up, or satisfy the
request from existing inventory.

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
VERIFYING_CLEANUP_EFFECT
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
  -> VERIFYING_CLEANUP_EFFECT only when the matching cleanup terminal completed
  -> FAILED_BEFORE_PRIMARY for rejected, failed, cancelled, deadline_exceeded, or unknown
  -> ABORTED on disconnect

VERIFYING_CLEANUP_EFFECT
  -> PRIMARY_SUBMITTING only when fresh post-cleanup inventory evidence proves
     the required free slots and protected-item preservation
  -> FAILED_BEFORE_PRIMARY when evidence is FULL, UNKNOWN, stale, mismatched,
     incomplete, or protected-item preservation is not verified

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
matching cleanup terminal has completed
active command has been cleared by transport ownership
the operation ID still matches
cleanup_attempt_count == 1
matching request/session/correlation/generation
fresh post-cleanup snapshot available
post-cleanup free slot count >= required_free_slots
protected_item_preservation_verified == true
primary_was_accepted is false
```

If any condition is false, do not submit the primary command automatically.

The current cleanup contract adds one mandatory state between cleanup terminal
and primary submission:

```text
WAITING_CLEANUP_TERMINAL
  -> VERIFYING_CLEANUP_EFFECT
```

Primary submission after cleanup additionally requires fresh post-cleanup
inventory evidence:

```text
matching request/session/correlation/generation
fresh post-cleanup snapshot available
post-cleanup free slot count >= required_free_slots
protected_item_preservation_verified == true
```

`cleanup terminal status == completed` alone is insufficient. The detailed
contract is:

```text
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
```

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
```

Default should be `off` or `targeted` with reliable inventory snapshots.
Any broad or "non-gear" cleanup mode is outside the current contract and
requires a separate approval. Even then, it must not mean bare `deposit` unless
a later evidence-backed contract explicitly approves that exact behavior.

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

### Historical Phase A: Korean Phrases And Start Replies

```text
Implemented at e08af639:
1. Shared Korean acquisition matcher.
2. Gate and rule parser use the shared matcher.
3. Standalone 철 mining/acquisition phrases resolve to iron_ingot.
4. Parser/resolver/compiler regressions cover Korean mining phrases.

Still planned:
5. Deterministic response renderer.
6. General LAVI external-response dispatch API.
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
5. Submit primary only after cleanup completed, fresh post-cleanup inventory
   evidence verifies the required effect, protected-item preservation is
   verified, and the replay guard passes.
6. Keep cleanup attempt count capped at one.
```

Phase D is only the old high-level placeholder. The current authoritative
cleanup contract splits this into shadow-mode inventory evidence first and
automatic cleanup execution later. Do not run cleanup from UNKNOWN inventory
state, do not use bare `deposit`, and do not submit the primary command after
cleanup until fresh post-cleanup evidence proves the required free slot.

## 2026-08-19 Phase Mapping

Use these phases for the reviewed archive baseline follow-up:

| Phase | Scope |
| --- | --- |
| Phase 0 | freeze document authority, craft wording as GET, response evidence vocabulary, unknown routing policy, cleanup postconditions, no-retry/no-replay, and hard prohibitions |
| Phase 1 | preserve existing GET acquisition regressions such as `철 10개 캐줘 -> get iron_ingot 10` and false-positive no-submit cases |
| Phase 2 | add reported alias/display gaps such as 갑바, 레깅스, 모자, emerald, and torch |
| Phase 3 | add catalog-driven alias coverage pipeline without changing command orchestration |
| Phase 4 | add deterministic immediate Korean response rendering from translation/precheck/accepted facts only |
| Phase 5 | add transport events and operation orchestrator without cleanup execution |
| Phase 6 | add reliable inventory provider and cleanup shadow mode with actual submit count 0 |
| Phase 7 | enable automatic targeted cleanup only after the cleanup contract is satisfied |

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
inventory FULL with safe targeted cleanup -> cleanup completed, fresh
  post-cleanup AVAILABLE/effect verified, then primary once
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

## 2026-08-19 Single-Pass Submission Boundary

The implemented router keeps natural-language translation separate from
submission. It translates once, classifies the validated result, checks runtime
readiness, and passes the same translation into exactly one submit call. DTO
revalidation may recompile the supplied DSL at a trust boundary; that is
validation, not a second natural-language translation.

Untrusted submit responses are normalized through one shared Python
canonicalizer used by the ordinary router and one-shot test boundary. Canonical
output always carries the same request ID at the top level and in nested status.
Malformed mirrors, non-bool success flags, mismatched request IDs, missing nested
status, or contradictory outcome/error combinations become UNKNOWN with
reconciliation required. No string or numeric value is coerced into a bool.

UNKNOWN ownership is latched before another Minecraft route may translate or
submit. It is cleared only through explicit validation of matching terminal
evidence from a trusted Fabric status snapshot. The first later route may perform
that read-only reconciliation, but its triggering command is never translated
or submitted even when reconciliation succeeds; a fresh explicit command is
required. There is no automatic replay, rerun, retry, or inference of success.
