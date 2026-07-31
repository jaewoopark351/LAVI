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

## Current Phase 1 Scope

Phase 1 may add:

- this separation document
- the Fabric ChatClef protocol draft document
- common protocol/interface/DTO/schema files
- a fail-closed Fabric ChatClef adapter and minimal config DTO
- tests for DTO/schema parity, backend separation, and fail-closed behavior

Phase 1 must not modify:

- `app_core/**`
- `config/modules*.json`
- `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**`
- Java runtime files or `fabric.mod.json`
- Player2APIService or AICommandBridge
- GUI, WebSocket, session, reconnect, or command execution code

## Lifecycle Ownership

Future Fabric ChatClef lifecycle ownership belongs to the Fabric ChatClef
extension and its Fabric-owned adapter, transport, and session objects. Future
Forge MineMind lifecycle ownership must be implemented separately in the Forge
backend.

Fabric and Forge must not share a WebSocket server, session registry, reconnect
manager, Java module, Gradle project, GUI component, runtime state, or test
fixture.

## Phase Boundary

This document does not approve Phase 2, Java bridge work, runtime execution,
commit, push, or merge.
