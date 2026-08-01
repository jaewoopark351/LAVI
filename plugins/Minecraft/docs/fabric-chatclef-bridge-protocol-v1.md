<!-- 20260801_kpopmodder: Drafted the inert v1 wire contract for the Fabric ChatClef bridge. -->

# Fabric ChatClef Bridge Protocol V1

Draft status: Phase 5 Fabric ChatClef GUI on top of Phase 4 command dispatch.
This document defines
the v1 wire shape shared by LAVI and the Fabric ChatClef Java bridge. Phase 2
owns the Python-side Fabric WebSocket server, session registry, handshake, and
status responses. Phase 3 owns the Fabric Java WebSocket client, reconnect
timing, and handshake/status/error exchange. Phase 4 owns command_request
transport, Java-side queueing, and client-tick command dispatch. Phase 5 adds
only LAVI-side Fabric ChatClef GUI controls and does not change the wire
protocol.

## Envelope

Every bridge message uses a `BridgeEnvelopeDTO`:

```text
protocol_version: 1
message_type: one BridgeMessageType wire value
message_id: unique ID for this envelope
correlation_id: nullable message_id this envelope answers
session_id: nullable connection session ID
timestamp_ms: Unix epoch milliseconds
payload: message-type-specific object
```

`message_id` identifies the envelope itself. `request_id` belongs only to a
Minecraft command request/result. The first handshake may have a null
`session_id`.

## Message Types

The protocol-neutral message types are:

```text
handshake
handshake_ack
command_request
command_result
status_request
status_snapshot
event
error
```

Fabric, Forge, ChatClef, and MineMind-specific message types are not part of
this draft.

## Command Request

`CommandRequestDTO` fields:

```text
request_id
command
source
deadline_ms
metadata
```

`deadline_ms` is nullable and is an absolute Unix epoch millisecond deadline.
Exceeding a deadline does not imply automatic retry, automatic command replay,
or automatic ChatClef task cancellation.

## Command Result

`CommandResultDTO` fields:

```text
request_id
ok
status
error_code
message
data
```

`status` is one of:

```text
accepted
running
completed
rejected
failed
cancelled
deadline_exceeded
unknown
```

`ok` is true only for `accepted`, `running`, and `completed`. When there is no
error, `error_code` is null. There is no `none` error code.

## Status Snapshot

`StatusSnapshotDTO` fields:

```text
backend_id
enabled
connected
lifecycle_state
detail
details
last_error_code
last_error_message
```

Phase 2 Fabric ChatClef default:

```text
backend_id = fabric_chatclef
enabled = false
connected = false
lifecycle_state = disabled
detail = Fabric ChatClef bridge is disabled.
details.endpoint = ws://127.0.0.1:4316
last_error_code = null
last_error_message = null
```

When `MinecraftFabricChatClef` is enabled and the Python server is started,
`lifecycle_state` is `disconnected` until the Fabric Java client connects. A
successful handshake changes the Python-side status to `connected`.

After Phase 4, `submit_command()` returns `accepted` once the Python transport
sends the request to the active Java bridge session. Final command status is
reported later by a `command_result` envelope from Java.

## Generic Error Codes

The generic bridge error codes are:

```text
not_implemented
not_connected
bridge_disabled
invalid_request
invalid_message_type
unsupported_protocol_version
deadline_exceeded
internal_error
```

Backend-specific error codes are intentionally absent from v1.

## Phase 2/3 Handshake

The Fabric client connects to the LAVI-owned Python WebSocket server and sends
a `handshake` envelope. The first handshake may omit `session_id`; the server
then assigns one and answers with `handshake_ack`.

Minimum successful handshake response:

```text
message_type = handshake_ack
correlation_id = original handshake message_id
session_id = assigned or supplied Fabric session id
payload.accepted = true
payload.session_id = same session id
payload.status = StatusSnapshotDTO
```

`status_request` receives `status_snapshot`. Other message types currently
receive an `error` envelope with `not_implemented` unless a later approved
phase assigns behavior.

The Phase 4 Java bridge handshake declares:

```text
payload.capabilities.fabric_chatclef_bridge = true
payload.capabilities.chatclef_command_dispatch = true
payload.metadata.backend = fabric_chatclef
payload.metadata.loader = fabric
payload.metadata.phase = phase_4_tick_dispatch
```

The Java bridge must not call `TaskRunner`, Baritone APIs, input override APIs,
path/goal APIs, or Minecraft player/world mutation APIs during Phase 4 command
transport. `CommandExecutor.execute(...)` is allowed only from the dedicated
client-tick dispatcher.

## Phase 4 Command Flow

Python sends `command_request` only when there is an active Fabric Java bridge
session and no active command request:

```text
message_type = command_request
payload = CommandRequestDTO
```

Java WebSocket callbacks must only decode, validate, and enqueue. They must not
touch Minecraft client state, AltoClef state, Baritone state, input state,
paths, or goals.

`END_CLIENT_TICK` owns dispatch:

```text
queue.peek
engine readiness check
queue.poll
CommandExecutor.execute(command, onFinish, onException)
command_result send
```

Only one command may be pending or active. Disconnection clears the Java queue
and does not replay the command on reconnect.

## Phase 5 GUI Boundary

The Fabric ChatClef GUI is a LAVI-side caller of the existing Phase 4 protocol.
It does not introduce new bridge messages, protocol versions, or backend-neutral
GUI helpers.

The GUI may:

- read `StatusSnapshotDTO` through the Fabric ChatClef plugin or extension
- submit a user-entered command through `MinecraftFabricChatClefExtension`
- display the immediate `accepted` or `rejected` submit result
- display the latest status JSON, including `details.commands.last_result`

The GUI must not:

- start the WebSocket server from `create_ui()`
- create threads, sockets, reconnect loops, or lifecycle state
- reuse Player2 endpoint `127.0.0.1:4315`
- create Forge/MineMind placeholders or shared Minecraft GUI code
- access ChatClef, AltoClef, Baritone, player/world state, input, paths, or
  goals directly
