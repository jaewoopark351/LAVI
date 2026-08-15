<!-- 20260806_kpopmodder: Added the Minecraft plugin documentation entrypoint and reading map. -->

# LAVI Minecraft Plugin

This directory contains the Minecraft integration owned by LAVI.

This document is the first reading point for Minecraft work. It is
documentation only. It does not approve Java changes, Python behavior changes,
Gradle changes, dependency changes, Minecraft launch, runtime reproduction,
cache deletion, commit, or push.

## Current Implementation Scope

Supported now:

```text
LAVI
  -> Fabric adapter
    -> Fabric ChatClef bridge
      -> ChatClef / AltoClef
        -> Baritone
```

Not implemented now:

```text
LAVI
  -> Forge adapter
    -> Forge MineMind
```

Fabric ChatClef and Forge MineMind are independent sibling backends. Do not add
Forge, MineMind, shared Java runtime, shared WebSocket server, shared reconnect
manager, shared session registry, shared diagnostics runtime, or shared GUI
work before a separate explicit approval for that backend.

## Backend Ownership Boundary

The common layer may contain only backend-neutral contracts:

```text
DTOs
wire protocol enums
JSON schema
error codes
documentation
neutral interfaces
```

The common layer must not own:

```text
WebSocket or HTTP transport
threading or asyncio loops
session registry
reconnect policy
runtime diagnostics implementation
config loaders
GUI implementation
Fabric runtime behavior
Forge runtime behavior
ChatClef adapter behavior
MineMind adapter behavior
```

## Folder Ownership Map

```text
plugins/Minecraft/common/dto/**
```

LAVI backend-neutral wire DTOs. These are the Python-side authority for the
current v1 bridge payload shape.

```text
plugins/Minecraft/common/protocol/**
plugins/Minecraft/common/schema/**
```

Backend-neutral protocol names, status values, and schemas.

```text
plugins/Minecraft/fabric/chatclef/**
```

LAVI-owned Python side of the Fabric ChatClef integration. This includes the
adapter, config, diagnostics, extension, input routing, intent extraction,
session ownership, WebSocket transport, and UI glue for Fabric ChatClef.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
```

Upstream-derived ChatClef / AltoClef Fabric runtime with LAVI compatibility
patches and LAVI-owned bridge or integration entrypoints. Treat this tree as a
preserved third-party-derived baseline unless a file is proven LAVI-owned.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/**
```

LAVI-owned Java bridge, diagnostics, optional integration, and overlay code
inside the Fabric runtime.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/**
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/baritone/**
```

Upstream-derived ChatClef / AltoClef / Baritone-adjacent engine code. Modify
only with the smallest evidence-backed hunk and only after the applicable
diagnostic or last-resort gate is satisfied.

```text
plugins/Minecraft/docs/**
```

Architecture, runbooks, audit notes, and divergence records for this
integration.

## End-To-End Command Flow

Current Fabric ChatClef command flow:

```text
raw user text
  -> LAVI input gate
  -> Minecraft route decision
  -> Korean or natural-language normalization
  -> rule or LLM intent extraction
  -> intent schema validation
  -> target and quantity resolution
  -> ChatClef command string
  -> MinecraftFabricChatClefExtension.submit_command()
  -> Python FabricChatClefWebSocketServer
  -> v1 bridge envelope command_request
  -> Java WebSocket callback decode and enqueue
  -> Fabric END_CLIENT_TICK dispatch
  -> FabricChatClefCommandExecutor
  -> ChatClef UserTaskChain root task
  -> ChatClef child tasks and Baritone helpers
  -> Java terminal observation or exception/deadline result
  -> v1 bridge envelope command_result
  -> Python FabricChatClefConnectionOwnership.accept_result()
  -> LAVI UI/status/extension result
```

The Java WebSocket callback is not the command executor. It must only decode,
validate, and enqueue. The client tick dispatcher owns Minecraft-client-thread
dispatch.

## Reading Order

Start with these documents:

```text
plugins/Minecraft/README.md
plugins/Minecraft/docs/minecraft-backend-separation.md
plugins/Minecraft/docs/fabric-chatclef-bridge-protocol-v1.md
plugins/Minecraft/docs/chatclef-command-lifecycle-and-threading.md
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/LAVI_INTEGRATION.md
```

For ChatClef / AltoClef runtime behavior, read:

```text
plugins/Minecraft/docs/chatclef-structure-overview.md
plugins/Minecraft/docs/chatclef-engine-diagrams.md
plugins/Minecraft/docs/chatclef-tick-accuracy-principles.md
```

For Carry On or interaction diagnostics, read:

```text
plugins/Minecraft/docs/chatclef-carryon-integration-direction.md
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
```

For pathfinding, wrong-target movement, mining stalls, or world replacement
regressions, read:

```text
plugins/Minecraft/docs/chatclef-baritone-cache-troubleshooting.md
```

For furnace/container arbitration, repeated `OPEN_CONTAINER` /
`GET_CONTAINER_ITEM` decisions, or `DestroyBlockTask` restart symptoms while a
smelting command is active, read:

```text
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
```

For payload typing and map-shape stability, read:

```text
plugins/Minecraft/docs/chatclef-command-payload-map-audit.md
```

For Python-only command replies, Korean mining phrases, transport result
events, and inventory preflight cleanup planning, read:

```text
plugins/Minecraft/docs/chatclef-python-command-orchestration-plan.md
```

For Korean standalone item aliases, mining/acquisition verb parsing, and
Minecraft-active routing boundaries, read:

```text
plugins/Minecraft/docs/chatclef-korean-item-command-resolution-analysis.md
```

For the v2 Korean item alias expansion shared by `get`, `equip`, `deposit`,
and `give`, including default-policy boundaries, command-specific policy
boundaries, conditional review notes, and the required Phase 0 contract freeze,
read:

```text
plugins/Minecraft/docs/chatclef-korean-item-action-alias-v2-plan.md
```

For upstream-derived engine divergence, read:

```text
plugins/Minecraft/docs/chatclef-engine-divergence-record.md
```

## Document Status

```text
Current contract:
  minecraft-backend-separation.md
  fabric-chatclef-bridge-protocol-v1.md
  chatclef-command-lifecycle-and-threading.md

Runtime integration runbook:
  runtime/chatclef_fabric_1.20.1/LAVI_INTEGRATION.md

Diagnostic runbooks:
  chatclef-task-lifecycle-diagnostics.md
  chatclef-baritone-cache-troubleshooting.md

Policy and investigation direction:
  chatclef-carryon-integration-direction.md

Audit and active snapshot:
  chatclef-command-payload-map-audit.md
  chatclef-engine-divergence-record.md

Python orchestration planning:
  chatclef-python-command-orchestration-plan.md

Korean item command diagnosis:
  chatclef-korean-item-command-resolution-analysis.md

Korean item action alias v2 planning:
  chatclef-korean-item-action-alias-v2-plan.md

Engine reference:
  chatclef-structure-overview.md
  chatclef-engine-diagrams.md
  chatclef-tick-accuracy-principles.md
```

`AGENTS.md` contains operating rules. It is not the architecture overview or
day-to-day reading map.

## Command Lifecycle Ownership Summary

Python owns:

```text
server start and stop
asyncio loop
WebSocket server binding
active connection generation
active session identity
single active command request
accepting or rejecting terminal command_result envelopes
UI and extension result adaptation
```

Java bridge owns:

```text
WebSocket client connection to Python
handshake payload
command_request decoding
Java-side command queue
client-tick dispatch
command root binding
terminal observation
command_result envelope sending
```

ChatClef / AltoClef owns:

```text
TaskRunner lifecycle
UserTaskChain root task selection
child task selection
generic interaction behavior
inventory/container task behavior
Baritone goal and path use through established engine paths
```

Diagnostics own observation only. Diagnostics must not choose tasks, mutate
inputs, create retries, cancel Baritone paths, change cleanup, or alter terminal
classification.

## Test Map

Protocol and backend separation:

```text
tests/test_minecraft_bridge_protocol.py
tests/test_minecraft_backend_separation_contract.py
```

Fabric ChatClef bridge and GUI:

```text
tests/test_minecraft_fabric_chatclef_transport.py
tests/test_minecraft_fabric_chatclef_java_bridge_contract.py
tests/test_minecraft_fabric_chatclef_adapter_skeleton.py
tests/test_minecraft_fabric_chatclef_composition.py
tests/test_minecraft_fabric_chatclef_extension.py
tests/test_minecraft_fabric_chatclef_gui.py
tests/test_minecraft_fabric_chatclef_korean_gui.py
```

Diagnostics contracts:

```text
tests/test_minecraft_fabric_chatclef_tasktrace_diagnostics_contract.py
tests/test_minecraft_fabric_chatclef_tool_equip_diagnostics_contract.py
tests/test_minecraft_fabric_chatclef_mining_tool_readiness_contract.py
```

Korean command parsing and routing:

```text
tests/test_minecraft_chatclef_input_router.py
tests/test_llm_minecraft_input_router.py
tests/test_minecraft_chatclef_command_compiler.py
tests/test_minecraft_chatclef_korean_command_integration.py
tests/test_minecraft_chatclef_korean_gui.py
tests/test_minecraft_chatclef_korean_intent_schema.py
tests/test_minecraft_chatclef_korean_rule_parser.py
tests/test_minecraft_chatclef_llm_intent_extractor.py
tests/test_minecraft_chatclef_natural_language_service.py
tests/test_minecraft_chatclef_item_phrase_resolver.py
```

Run tests only when the current task explicitly authorizes tests or when the
test run is part of the requested implementation work. Documentation edits do
not automatically authorize a Minecraft launch or runtime reproduction.

## Investigation Rule Of Thumb

If the exact last successful boundary, first failing boundary, and triggering
state are not proven, stop at diagnostics or documentation.

Do not use an unproven timeout, retry, global cancellation, forced input
release, Baritone cache deletion, or broad refactor as the first fix.
