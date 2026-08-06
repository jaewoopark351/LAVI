<!-- 20260804_kpopmodder: Documented Fabric ChatClef command payload Map boundaries before Java typing work. -->

# ChatClef Command Payload Map Audit

Date: 2026-08-04

Scope:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/**
plugins/Minecraft/common/dto/**
plugins/Minecraft/fabric/chatclef/**
```

This document records where `Map<String, Object>` payloads are used before
introducing stronger Java-side types. It is an audit note, not approval to
change runtime behavior.

## Why This Exists

The Fabric ChatClef bridge currently has several different kinds of maps:

- stable v1 wire protocol payloads shared by Java and Python
- command result payloads returned from Java to LAVI
- diagnostic payloads nested inside command result `data`
- UI/status dictionaries used only by the Python extension layer
- log-only dictionaries used for debugging command ownership and lifecycle

These maps should not be typed all at once. Some are protocol boundaries whose
JSON keys must remain stable. Others are internal diagnostic structures where
additive typing can be done later without changing the wire contract.

## Guardrails

- Do not rename v1 wire keys without updating both Java and Python protocol
  DTOs and the protocol document.
- Do not change command behavior, timeout behavior, retry behavior, task
  selection, Baritone ownership, or ChatClef lifecycle while doing this typing
  work.
- Do not move or repackage upstream-derived ChatClef, AltoClef, TaskRunner, or
  Baritone code as part of payload typing.
- Prefer Java value objects or records internally, then serialize to the
  existing map shape at the transport edge.
- Keep Python DTOs as the receiving authority for the current v1 protocol.

## Current Wire Contracts

### Bridge Envelope

Java:

```text
FabricChatClefBridgeEnvelope
FabricChatClefBridgeJson
FabricChatClefResultEnvelopeSender
```

Python:

```text
BridgeEnvelopeDTO
BridgeMessageType
```

Stable keys:

```text
protocol_version
message_type
message_id
correlation_id
session_id
timestamp_ms
payload
```

Classification: hard protocol boundary.

Typing direction: keep the JSON key names and message type values unchanged.
Internal Java construction may be typed, but the serialized envelope must stay
compatible with `BridgeEnvelopeDTO.from_mapping()`.

### Command Request

Python sends:

```text
CommandRequestDTO.to_dict()
FabricChatClefWebSocketServer.submit_command()
```

Java receives:

```text
FabricChatClefBridgeJson.commandRequest(Map<String, Object>)
FabricChatClefCommandRequest
FabricChatClefCommandRequestFields
```

Stable keys:

```text
request_id
command
source
deadline_ms
metadata
```

Classification: hard protocol boundary.

Typing direction: Java already has `FabricChatClefCommandRequest`. Keep
`metadata` flexible unless a specific metadata schema is approved.
The request wire keys are centralized in `FabricChatClefCommandRequestFields`
to support gradual folderization without changing serialized field names.

### Command Result

Java sends:

```text
FabricChatClefCommandResult
FabricChatClefCommandResultPayload
FabricChatClefCommandResultStatus
FabricChatClefCommandResultFactory
FabricChatClefCommandResultOutbox
FabricChatClefResultEnvelopeSender
```

Python receives:

```text
CommandResultDTO.from_mapping()
FabricChatClefWebSocketServer._handle_command_result()
FabricChatClefConnectionOwnership.accept_result()
```

Stable keys:

```text
request_id
ok
status
error_code
message
data
```

Known status values:

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

Classification: hard protocol boundary.

Typing direction: Java command result payloads now stay typed as
`FabricChatClefCommandResultPayload` through result construction, lifecycle
classification, outbox delivery, and the result sender interface. The transport
edge still serializes the same map shape in `FabricChatClefResultEnvelopeSender`
so the public payload continues to satisfy Python `CommandResultDTO`.

## Diagnostic Payloads

### Command Result Data

Java builders:

```text
FabricChatClefCommandDiagnosticPayload.commandData()
FabricChatClefCommandDiagnosticPayload.diagnosticData()
FabricChatClefTaskSnapshot.toMap()
FabricChatClefCommandTerminationObservation.toMap()
FabricChatClefCommandExecution.duplicateTerminalData()
FabricChatClefCommandContext.ownershipPayload().toMap()
```

Important nested keys currently emitted include:

```text
result_fidelity
result_reason
dispatch_started_ms
dispatch_returned
dispatch_thread
normalized_command_length
finish_callback_received
failure_type
failure_message
ownership
task_before_dispatch
task_after_dispatch
terminal_task
bound_root_task
task_finished_event_received
task_finished_observation
request_command
request_source
normalized_command
elapsed_ms
automation_cancelled
task_may_still_be_running
late_terminal_event_will_be_ignored
```

Task snapshot keys:

```text
available
class_name
description
identity
error
task_state_available
task_active
task_stopped
this_or_child_timed_out
task_state_error
```

Runtime task observation keys:

```text
thread_name
observed_at_ms
client_tick_id
current_task
```

Task finished event lifecycle detail keys:

```text
task_finished_event
matched_bound_root_task
event_task
event_task_matches_bound_root_task
event_task_bound_root_match_reason
bound_root_task
finish_callback_received
runtime
```

Other command lifecycle detail keys now centralized in the Java helper layer:

```text
replaced_active_request_id
callback_current_task
callback_current_task_matches_bound_root_task
callback_current_task_bound_root_match_reason
runtime
decision_reason
terminal_sent
lifecycle_cleared
queue_active_present
queue_active_request_id
waiting_reason
current_task
current_task_matches_bound_root_task
current_task_bound_root_match_reason
exception_type
exception_message
```

Termination observation keys:

```text
completion_source
termination_kind
task_present
task_stopped
stop_state_available
stop_state_error
duration_seconds
observed_at_ms
observation_thread
task
```

Ownership/session keys:

```text
request_id
correlation_id
session_id
connection_generation
accepted_at_ms
detached
detached_reason
```

Classification: diagnostic contract, not core protocol.

Typing direction: additive typing is allowed later, but avoid deleting or
renaming keys while these logs are actively used to diagnose command lifecycle
issues. A future typed diagnostic object should still provide `toMap()` at the
command result boundary.

Current LAVI-owned helper placement:

```text
command/ownership/FabricChatClefCommandOwnershipPayload
command/result/FabricChatClefCommandResultDataPayload
command/result/FabricChatClefCommandResultDataMapPayload
command/result/FabricChatClefCommandResultPayload
command/result/FabricChatClefCommandResultPayloadMap
command/result/FabricChatClefCommandResultStatus
command/diagnostics/FabricChatClefCommandDiagnosticDetailsPayload
command/observation/FabricChatClefTaskSnapshot
command/observation/FabricChatClefTaskSnapshotPayload
command/observation/FabricChatClefBoundRootTaskRelationshipPayload
command/observation/FabricChatClefTaskRuntimeObservationPayload
command/execution/FabricChatClefCommandDiagnosticResultPayload
command/lifecycle/FabricChatClefCommandDeadlinePayload
command/lifecycle/FabricChatClefCommandLifecyclePayload
command/lifecycle/FabricChatClefLifecycleDetailsPayload
command/lifecycle/details/*
command/lifecycle/FabricChatClefTaskFinishedEventDetailsPayload
```

These helpers centralize field names and keep typed values local until the
existing `Map<String, Object>` serialization edge. Command result `data` now
travels through `FabricChatClefCommandResultDataPayload` for lifecycle,
ownership, deadline, and diagnostic-result payloads before the final map
expansion. Command lifecycle detail objects pass through
`FabricChatClefCommandDiagnosticDetailsPayload` before
`FabricChatClefCommandDiagnostics` expands them for logging. They do not rename
emitted keys, change status values, or alter lifecycle, timeout, retry, task
observation, or ownership behavior.

The command lifecycle detail facade now delegates event-specific detail
payloads to `command/lifecycle/details/*` so each lifecycle event owns its own
field serialization before the final diagnostic map edge.

### Lifecycle And Gate Logs

Java:

```text
FabricChatClefCommandDiagnostics
```

Python:

```text
FabricChatClefWebSocketServer._log_command_gate()
```

Classification: log-only diagnostic payloads.

Typing direction: keep flexible for now. These maps are useful precisely
because they can carry temporary diagnostic fields while command ownership is
being investigated.

## Python UI And Extension Payloads

Python extension-facing result:

```text
MinecraftFabricChatClefExtension._extension_result_payload()
FabricChatClefCommandController
```

UI-facing keys:

```text
ok
status
error
message
details
```

Important distinction:

```text
status = nested CommandResultDTO.to_dict()
details = CommandResultDTO.data
```

Classification: Python extension/UI adapter contract.

Typing direction: do not confuse this shape with the Java `command_result`
wire payload. It is intentionally adapted for Gradio and extension callers.

Status presenter and status snapshots:

```text
StatusSnapshotDTO
FabricChatClefStatusPresenter
FabricChatClefConnectionOwnership.snapshot()
```

Classification: Python status/UI contract.

Typing direction: keep generic mapping access in the presenter unless a
separate UI model refactor is approved.

## Recommended Typing Order

1. Preserve all v1 wire keys exactly as documented in
   `fabric-chatclef-bridge-protocol-v1.md`.
2. Add Java-side typing around command result status and command result payload.
3. Keep the transport edge serializing the same map shape:

```text
FabricChatClefCommandResult typed object -> toMap() -> command_result payload
```

4. Leave command result diagnostic `data` as `Map<String, Object>` until the
   command result payload itself is stable.
5. Use typed diagnostic detail wrappers at lifecycle log call sites, then
   expand to the existing map shape only in `FabricChatClefCommandDiagnostics`.
6. Type diagnostic payloads only after deciding which diagnostic fields are
   long-term contract fields and which were temporary investigation fields.
7. Keep `metadata`, `details`, and log-only dictionaries flexible unless a
   consumer contract requires a schema.

## Do Not Include In This Refactor

- no protocol version bump
- no wire key rename
- no Java/Python behavior change
- no timeout, retry, cancellation, or cleanup change
- no TaskRunner, AltoClef, Baritone, or global ChatClef lifecycle change
- no Forge/MineMind common implementation or shared transport/session layer
- no package moves in upstream-derived ChatClef code

## Related Lifecycle Diagnostic Runbook

When `task_identity_mismatch`, `cancelled_without_task`,
`waiting_for_terminal_condition`, repeated child Task selection, or Baritone
pathing loops are under investigation, keep this audit's payload-shape
guardrails and use:

```text
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
```

That runbook defines additive diagnostics for command root assignment,
TaskFinishedEvent correlation, parent-child target comparison, Interact
lifecycle, Baritone path ownership, container open attempts, and
DestroyBlockTask target state. It does not authorize changing existing payload
field names or command lifecycle behavior.

For the command lifecycle, ownership, terminal ordering, and thread-affinity
overview, also read:

```text
plugins/Minecraft/docs/chatclef-command-lifecycle-and-threading.md
```

## Diagnostic Event Emission Contract

Diagnostic events are log-only or command-result `data` observations. They are
not a new v1 wire message contract unless a later protocol document explicitly
promotes them.

Every new diagnostic event added for command lifecycle, task observation,
container behavior, Baritone pathing, tool readiness, or optional Carry On
observation must document these fields:

```text
event
owner
mode
trigger
dedupe_key
max_emission
correlation
payload
terminal
behavior_effect
```

Field meanings:

```text
event: stable diagnostic event name
owner: component that owns the observation
mode: boundary, verbose, or another existing diagnostics mode
trigger: state change or boundary that caused emission
dedupe_key: fingerprint used to suppress repeated unchanged events
max_emission: per-command or per-session emission budget
correlation: request/session/task identity values used to join logs
payload: additive event-specific detail object
terminal: true only when the event represents a terminal observation
behavior_effect: must be none for diagnostics-only events
```

Allowed `owner` examples:

```text
python_connection_ownership
python_transport
java_bridge_client
java_command_queue
java_client_tick_dispatcher
java_command_lifecycle
chatclef_task_observer
baritone_path_observer
container_observer
carryon_optional_observer
```

Correlation should include the strongest available stable identifiers:

```text
request_id
correlation_id
session_id
connection_generation
client_tick_id
task_identity
bound_root_task_identity
child_task_identity
target_position
dimension
```

Rules:

- Additive fields are allowed when investigation needs them.
- Renaming an event or existing field is not allowed while the current logs are
  being used as evidence.
- Deduplication and emission budgets may affect only logging volume.
- Diagnostic budgets are not timeouts and must not change command lifecycle.
- `behavior_effect` must remain `none` for diagnostics-only changes.
- Do not turn an observation failure into success.
- Do not add a `try/catch` around observed engine behavior solely to protect a
  diagnostic event.
- Formatter-local protection is allowed only for malformed optional diagnostic
  values and must not suppress ChatClef, AltoClef, Baritone, input, transport,
  or container exceptions.

## Evidence File List

Java protocol and command files:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/protocol/FabricChatClefBridgeEnvelope.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/protocol/FabricChatClefBridgeJson.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/protocol/FabricChatClefBridgeMessageFactory.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandRequest.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/request/FabricChatClefCommandRequestFields.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandResult.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandContext.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/ownership/FabricChatClefCommandOwnershipPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandResultSender.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/transport/FabricChatClefResultEnvelopeSender.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandDiagnosticPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandLifecyclePayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefTaskFinishedEventDetailsPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefBoundRootTaskRelationshipPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefTaskRuntimeObservationPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefTaskSnapshot.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandDeadlinePayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandTerminationObservation.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/diagnostics/FabricChatClefCommandDiagnostics.java
```

Python DTO, transport, and UI files:

```text
plugins/Minecraft/common/dto/bridge_envelope_dto.py
plugins/Minecraft/common/dto/command_request_dto.py
plugins/Minecraft/common/dto/command_result_dto.py
plugins/Minecraft/common/dto/status_snapshot_dto.py
plugins/Minecraft/common/protocol/bridge_message_type.py
plugins/Minecraft/common/protocol/command_result_status.py
plugins/Minecraft/fabric/chatclef/transport/fabric_chatclef_websocket_server.py
plugins/Minecraft/fabric/chatclef/transport/fabric_chatclef_connection_ownership.py
plugins/Minecraft/fabric/chatclef/adapter/fabric_chatclef_adapter.py
plugins/Minecraft/fabric/chatclef/extension/minecraft_fabric_chatclef_extension.py
plugins/Minecraft/fabric/chatclef/ui/fabric_chatclef_command_controller.py
plugins/Minecraft/fabric/chatclef/ui/fabric_chatclef_status_presenter.py
```
