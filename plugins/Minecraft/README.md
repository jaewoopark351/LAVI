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

For the dated Store/container incident evidence, read the matching incident
analysis instead of applying one run's hypothesis to another:

```text
plugins/Minecraft/docs/chatclef-post-completion-store-loop-investigation.md
    2026-08-07 post-completion Store/Craft/Baritone loop

plugins/Minecraft/docs/chatclef-bare-deposit-container-handoff-loop-investigation.md
    2026-08-19 bare deposit branch/lifecycle loop and explicit StopCommand end

plugins/Minecraft/docs/chatclef-resource-target-retry-thrashing-analysis.md
    2026-08-08 resource target/path retry thrashing that eventually completed
```

For the bounded diagnostics-only design and its current slice-by-slice
implementation status, read:

```text
plugins/Minecraft/docs/chatclef-bare-deposit-diagnostics-plan.md
```

For the first live prefix captured with the partial diagnostics JAR, read:

```text
plugins/Minecraft/docs/chatclef-bare-deposit-diagnostics-reproduction-2026-08-19-r1.md
    separate run; live prefix, not a finalized terminal record
```

For payload typing and map-shape stability, read:

```text
plugins/Minecraft/docs/chatclef-command-payload-map-audit.md
```

For Python-only command replies, Korean mining phrases, transport result
events, stale active-command reconciliation, and inventory preflight cleanup
planning, read:

```text
plugins/Minecraft/docs/chatclef-python-command-orchestration-plan.md
```

The orchestration plan owns stale-active UNKNOWN reconciliation and
active-ownership release. Releasing active ownership never proves command
completion or gameplay success.

For the stale active-command terminal reconciliation gap, Java should publish
additive nonterminal lifecycle evidence through the existing
`command_result` channel with `status=running`. Python may store that validated
running result as `details.commands.last_result`, but active ownership remains
held until a terminal result or a separately approved Python-local
`RECONCILED_UNKNOWN` compare-and-release path runs.

`RECONCILED_UNKNOWN` is a Python-local action, not a new bridge status. The
wire/DTO result remains `status=unknown`, `ok=false`, with
`gameplay_effect=UNVERIFIED`. A Python release does not prove Java
`FabricChatClefCommandQueue.active` has retired, so new command admission must
remain quarantined until authoritative evidence proves Java queue and lifecycle
retirement. A late terminal, connection-generation replacement, or recovery
completion may clear that gate only when its source contract and tests prove it
establishes Java retirement; otherwise it is supporting evidence only.
The next implementation plan must define the monotonic lifecycle
`evidence_sequence` acceptance rules, authoritative transport admission gate,
and reconnect/restart fail-closed strategy before any code change.

2026-08-21 review update: the Python stale-active direction is
`CONDITIONAL PASS / PRODUCTION ON BLOCKED`. A default-OFF shadow classifier and
test-injected guarded mutation path may be implemented, but production config
must not enable release unless a separate runtime readiness gate proves
authoritative Java retirement evidence or restart-safe quarantine capability.
Python-local reconciliation state must be split away from Java-facing wire
status snapshots: synthetic UNKNOWN, tombstones, and admission quarantine must
not appear in `handshake_ack` or `status_snapshot` payloads sent to Java. The
phrase `다이아 곡괭이 만들어줘` already validates to `get diamond_pickaxe 1`; this
incident remains classified as stale active-command admission blocking, not a
Korean compiler gap.

For the full 20-command Korean registry, command-by-command lifecycle
classification, safety tiers, confirmation modes, resolver domains, and public
enablement axes, read:

```text
plugins/Minecraft/docs/chatclef-python-korean-command-registry-plan.md
```

For the Python-only inventory-full cleanup preflight contract, protected-item
policy, post-cleanup verification, and fail-closed primary-submit gate, read:

```text
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
```

For the historical diagnosis of Korean standalone item aliases,
mining/acquisition verb parsing, and Minecraft-active routing boundaries, read:

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

For Korean ChatClef test strategy, prefixless DSL ownership, alias coverage
snapshots, command support matrix rules, and opt-in live test tiers, read:

```text
plugins/Minecraft/docs/chatclef-korean-test-strategy.md
```

For the post-implementation Korean ChatClef merge-blocker review, including
Java contract hash authority, CI coverage, quantity validation,
TranslationResultDTO safety, live result correlation, and alias snapshot
follow-up, read:

```text
plugins/Minecraft/docs/chatclef-korean-post-review-merge-blockers.md
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

Diagnostics-only design and implementation status:
  chatclef-bare-deposit-diagnostics-plan.md

Diagnostics reproduction evidence:
  chatclef-bare-deposit-diagnostics-reproduction-2026-08-19-r1.md

Policy and investigation direction:
  chatclef-carryon-integration-direction.md

Audit and active snapshot:
  chatclef-command-payload-map-audit.md
  chatclef-engine-divergence-record.md

Incident analyses:
  chatclef-cooked-beef-entity-path-calculation-investigation.md
  chatclef-post-completion-store-loop-investigation.md
  chatclef-bare-deposit-container-handoff-loop-investigation.md
  chatclef-resource-target-retry-thrashing-analysis.md

Python orchestration planning:
  chatclef-python-command-orchestration-plan.md

Python Korean command registry planning:
  chatclef-python-korean-command-registry-plan.md

Python inventory cleanup preflight contract:
  chatclef-python-inventory-cleanup-preflight-contract.md

Korean item command diagnosis:
  chatclef-korean-item-command-resolution-analysis.md

Korean item action alias v2 planning:
  chatclef-korean-item-action-alias-v2-plan.md

Korean ChatClef test strategy:
  chatclef-korean-test-strategy.md

Korean ChatClef post-review merge blockers:
  chatclef-korean-post-review-merge-blockers.md

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
Korean command interpretation
deterministic user response rendering
Python-local command orchestration
inventory preflight evidence evaluation
targeted cleanup planning
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
nonterminal status=running lifecycle evidence publication
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

Responsibility-split Korean ChatClef tests:

```text
tests/minecraft_chatclef/alias_contract/
tests/minecraft_chatclef/catalog_coverage/
tests/minecraft_chatclef/command_catalog/
tests/minecraft_chatclef/input_gate/
tests/minecraft_chatclef/korean_translation/
tests/minecraft_chatclef/lavi_input/
tests/minecraft_chatclef/lifecycle/
tests/minecraft_chatclef/runtime/
```

Run tests only when the current task explicitly authorizes tests or when the
test run is part of the requested implementation work. Documentation edits do
not automatically authorize a Minecraft launch or runtime reproduction.

## Investigation Rule Of Thumb

If the exact last successful boundary, first failing boundary, and triggering
state are not proven, stop at diagnostics or documentation.

Do not use an unproven timeout, retry, global cancellation, forced input
release, Baritone cache deletion, or broad refactor as the first fix.
