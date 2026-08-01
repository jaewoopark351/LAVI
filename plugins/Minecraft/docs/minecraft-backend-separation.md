<!-- 20260801_kpopmodder: Documented the hard Fabric ChatClef / Forge MineMind backend split. -->

# Minecraft Backend Separation

This document is the detailed design companion for the backend separation gate
in `AGENTS.md`.

## Hard Boundary

Minecraft backends are independent sibling backends:

```text
LAVI -> Fabric adapter -> Fabric mod -> ChatClef / AltoClef
LAVI -> Forge adapter  -> Forge mod  -> MineMind
```

Only the Fabric ChatClef path is in the current implementation scope.
Forge/MineMind paths, placeholders, config keys, GUI tabs, tests, enum values,
or module IDs must not be created until that backend receives separate
approval.

## Allowed Common Layer

`plugins/Minecraft/common/**` may contain only:

- backend-neutral protocol contracts
- typing protocols or abstract interface contracts
- DTOs
- JSON schema
- generic error codes
- serialization rules

It must not contain:

- WebSocket, HTTP, socket, thread, or asyncio implementation
- session registry, reconnect manager, retry policy, or lifecycle worker
- config loader, file IO, environment reads, or logger implementation
- GUI helpers
- Fabric, Forge, ChatClef, AltoClef, or MineMind concrete integration
- loader detection or backend selection logic

## Completed Phase 1 Scope

Phase 1 added:

- this separation document
- the Fabric ChatClef protocol draft document
- common protocol/interface/DTO/schema files
- a fail-closed Fabric ChatClef adapter and minimal config DTO
- tests for DTO/schema parity, backend separation, and fail-closed behavior

Phase 1 did not modify:

- `app_core/**`
- `config/modules*.json`
- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**`
- Java runtime files or `fabric.mod.json`
- Player2APIService or AICommandBridge
- GUI, WebSocket, session, reconnect, or command execution code

## Completed Phase 2 Scope

Phase 2 added only the Fabric ChatClef Python backend transport path:

- Fabric ChatClef optional plugin manifest entry
- Fabric ChatClef adapter lifecycle facade
- Fabric ChatClef WebSocket server
- Fabric ChatClef session registry
- Fabric ChatClef GameExtension registration
- targeted tests for default disabled behavior and local handshake

Phase 2 must not add:

- Forge or MineMind folders, module IDs, config keys, GUI tabs, or tests
- Java Fabric bridge files
- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**` behavior changes
- ChatClef command dispatch
- GUI components
- shared WebSocket/session/reconnect implementations in `common`

Command submission remains fail-closed until Phase 4. A connected Fabric
bridge client may complete handshake/status exchange, but `submit_command()`
still returns `not_implemented` until tick-dispatched Java command execution is
separately implemented.

## Completed Phase 3 Scope

Phase 3 added only the Fabric ChatClef Java bridge handshake/reconnect path:

- a LAVI-owned Fabric Java bridge package under
  `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/`
- a Java WebSocket client using the JDK HTTP/WebSocket API
- Fabric-specific endpoint configuration with default `ws://127.0.0.1:4316`
- v1 handshake send and `handshake_ack`, `status_snapshot`, and `error`
  receive handling
- reconnect timing owned by Fabric ChatClef bridge code only
- one `fabric.mod.json` main entrypoint registration hunk

Phase 3 must not add:

- ChatClef command dispatch
- `CommandExecutor`, `TaskRunner`, AltoClef lifecycle, or Baritone access
- Minecraft player/world/state mutation from WebSocket callbacks
- Forge/MineMind folders, module IDs, config keys, GUI tabs, or tests
- shared Java modules, shared WebSocket clients, or shared reconnect managers

Phase 3 declared `chatclef_command_dispatch=false` in handshake capabilities.
It proved transport connectivity before command execution was approved.

## Completed Phase 4 Scope

Phase 4 adds only the Fabric ChatClef command queue and client tick dispatch
path:

- Python `submit_command()` sends one `command_request` envelope to the active
  Fabric Java bridge session.
- Python keeps one active request and rejects a second command until a terminal
  `command_result` is received.
- Java WebSocket callbacks decode and validate JSON only, then enqueue
  `command_request`.
- Java `END_CLIENT_TICK` dispatch polls the queue, checks engine readiness, and
  calls `CommandExecutor.execute(...)`.
- The dispatcher permits only one pending or active command at a time.
- Disconnection clears pending/active Java commands so reconnect never replays a
  non-idempotent command automatically.

Phase 4 must not add:

- GUI components
- Forge/MineMind folders, module IDs, config keys, GUI tabs, or tests
- shared command queues, shared WebSocket clients, or shared reconnect managers
- `TaskRunner`, Baritone, input override, path, goal, or global cleanup changes
- automatic retry or command replay
- `Thread.sleep`, blocking I/O, or Minecraft API calls inside WebSocket callbacks

The Java bridge declares `chatclef_command_dispatch=true` only after command
execution is moved to the client tick dispatcher.

## Current Phase 5 Scope

Phase 5 adds only the Fabric ChatClef Gradio GUI path:

- a Fabric ChatClef-owned panel under `plugins/Minecraft/fabric/chatclef/ui/`
- AppComposer UI wiring that calls the panel only when
  `MinecraftFabricChatClef` is enabled and instantiated
- status display for endpoint, lifecycle state, connection state, and raw
  status JSON
- command input that submits through `MinecraftFabricChatClefExtension` when it
  is registered, preserving runtime command/result recording
- plugin adapter fallback only for isolated UI tests or direct plugin usage

Phase 5 must not add:

- Forge/MineMind folders, module IDs, config keys, GUI tabs, or tests
- GUI helpers in `plugins/Minecraft/common/**`
- transport startup, thread creation, socket startup, or server shutdown from
  `create_ui()`
- shared GUI components for Fabric and Forge
- Player2 endpoint reuse
- ChatClef/AltoClef engine, TaskRunner, Baritone, input, path, or goal changes

The Fabric ChatClef WebSocket server remains owned by
`MinecraftFabricChatClefExtension.start()` through the Fabric adapter. The GUI
only reads status and submits user commands.

## Lifecycle Ownership

Future Fabric ChatClef lifecycle ownership belongs to the Fabric ChatClef
extension and its Fabric-owned adapter, transport, and session objects. Future
Forge MineMind lifecycle ownership must be implemented separately in the Forge
backend.

Fabric and Forge must not share a WebSocket server, session registry, reconnect
manager, Java module, Gradle project, GUI component, runtime state, or test
fixture.

## Phase Boundary

This document does not approve additional GUI work beyond the Fabric ChatClef
Phase 5 panel, Forge/MineMind work, later backend phases, commit, push, or
merge.
