<!-- 20260801_kpopmodder: Drafted the inert v1 wire contract for the Fabric ChatClef bridge. -->

# Fabric ChatClef Bridge Protocol V1

Draft status: Phase 1 contract skeleton only. This document defines the wire
shape shared by LAVI and the future Fabric ChatClef bridge. It does not approve
transport, WebSocket, Java runtime, command execution, or GUI work.

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

Phase 1 Fabric ChatClef default:

```text
backend_id = fabric_chatclef
enabled = false
connected = false
lifecycle_state = not_implemented
detail = Fabric ChatClef bridge is not implemented in Phase 1.
details = {}
last_error_code = null
last_error_message = null
```

`not_implemented` lifecycle state describes the missing Phase 1 transport. It
is not a last runtime error.

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

Backend-specific error codes are intentionally absent from Phase 1.
