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
- For newly added LAVI-owned command lifecycle, ownership, task observation, or
  diagnostic payload classes, prefer typed Java payload objects or records from
  the start, then serialize to the existing map shape only at the log,
  diagnostic-data, or transport edge.
- Do not use new raw `Map<String, Object>` payload assembly as the primary
  implementation for those new classes unless the class is explicitly a
  compatibility adapter, request metadata boundary, protocol parser, or final
  serialization edge.
- Do not use this new-class typing rule as permission to mass-type existing
  command lifecycle, ownership, or task observation payloads while their log
  field names and nested shapes are active debugging evidence.
- Keep Python DTOs as the receiving authority for the current v1 protocol.

## Current Wire Contracts

### Bridge Envelope

Java:

```text
FabricChatClefBridgeEnvelope
FabricChatClefBridgeJson
FabricChatClefResultEnvelopeSender
protocol/handshake/FabricChatClefHandshakePayload
protocol/handshake/FabricChatClefHandshakeCapabilitiesPayload
protocol/handshake/FabricChatClefHandshakeMetadataPayload
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
compatible with `BridgeEnvelopeDTO.from_mapping()`. The Fabric ChatClef
handshake payload is typed in `protocol/handshake/*` and expands to the same
`payload.capabilities` and `payload.metadata` map shape only at the envelope
serialization edge.

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
protocol/result/FabricChatClefCommandResultEnvelopeFactory
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
classification, outbox delivery, the result sender interface, and command result
envelope construction. The protocol edge still serializes the same map shape in
`protocol/result/FabricChatClefCommandResultEnvelopeFactory` so the public
payload continues to satisfy Python `CommandResultDTO`.

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
command/ownership/payload/FabricChatClefCommandOwnershipPayloadMap
command/ownership/payload/session/*
command/ownership/payload/session/detach/*
command/result/FabricChatClefCommandResultDataPayload
command/result/FabricChatClefCommandResultDataMapPayload
command/result/FabricChatClefCommandResultPayload
command/result/FabricChatClefCommandResultPayloadMap
command/result/FabricChatClefCommandResultStatus
command/diagnostics/FabricChatClefCommandDiagnosticDetailsPayload
command/observation/FabricChatClefTaskSnapshot
command/observation/FabricChatClefBoundRootTaskRelationshipPayload
command/observation/FabricChatClefTaskRuntimeObservationPayload
command/observation/payload/FabricChatClefBoundRootTaskRelationshipPayloadMap
command/observation/payload/FabricChatClefTaskRuntimeObservationPayloadMap
command/observation/payload/FabricChatClefTaskOwnershipSnapshotPayloadMap
command/observation/payload/FabricChatClefTaskSnapshotPayload
command/observation/payload/FabricChatClefTaskSnapshotPayloadMap
command/observation/payload/runtime/*
command/observation/payload/ownership/*
command/observation/payload/relationship/*
command/observation/payload/snapshot/*
command/observation/payload/snapshot/state/*
command/execution/FabricChatClefCommandDiagnosticResultPayload
command/lifecycle/FabricChatClefCommandDeadlinePayload
command/lifecycle/FabricChatClefCommandLifecyclePayload
command/lifecycle/FabricChatClefLifecycleDetailsPayload
command/lifecycle/details/*
command/lifecycle/payload/FabricChatClefCommandDeadlinePayloadMap
command/lifecycle/payload/FabricChatClefCommandLifecyclePayloadMap
command/lifecycle/payload/FabricChatClefCommandTerminationObservationPayload
command/lifecycle/payload/FabricChatClefCommandTerminationObservationPayloadMap
command/lifecycle/details/FabricChatClefTaskFinishedEventDetailsPayload
command/lifecycle/details/payload/*
command/lifecycle/details/payload/exceptiondetail/*
command/lifecycle/details/payload/finish/*
command/lifecycle/details/payload/finish/current/*
command/lifecycle/details/payload/finish/runtime/*
command/lifecycle/details/payload/queue/*
command/lifecycle/details/payload/taskfinished/*
command/lifecycle/details/payload/taskfinished/match/*
command/lifecycle/details/payload/taskfinished/runtime/*
command/lifecycle/details/payload/terminal/*
command/lifecycle/details/payload/waiting/*
command/lifecycle/details/payload/waiting/current/*
command/lifecycle/observation/FabricChatClefTaskFinishedObservationPayload
command/lifecycle/observation/payload/FabricChatClefTaskFinishedObservationPayloadMap
command/diagnostics/details/FabricChatClefEmptyCommandDiagnosticDetailsPayload
command/diagnostics/details/payload/FabricChatClefEmptyCommandDiagnosticDetailsPayloadMap
command/diagnostics/payload/FabricChatClefCommandDiagnosticLogPayload
command/diagnostics/payload/log/*
```

These helpers centralize field names and keep typed values local until the
existing `Map<String, Object>` serialization edge. Command result `data` now
travels through `FabricChatClefCommandResultDataPayload` for lifecycle,
ownership, deadline, and diagnostic-result payloads before the final map
expansion. Command lifecycle detail objects pass through
`FabricChatClefCommandDiagnosticDetailsPayload` before
`FabricChatClefCommandDiagnosticLogPayload` expands them for logging and
`FabricChatClefCommandDiagnostics` emits the log line. They do not rename
emitted keys, change status values, or alter lifecycle, timeout, retry, task
observation, or ownership behavior.

Empty diagnostic detail maps are represented by
`FabricChatClefEmptyCommandDiagnosticDetailsPayload` before final log
serialization. The diagnostics facade no longer keeps a raw
`Map<String, Object>` detail adapter or accepts raw-map detail overloads, so new
lifecycle call sites must provide typed detail payloads while preserving the
existing `details` object shape.

The command lifecycle detail facade now delegates event-specific detail
payloads to `command/lifecycle/details/*` so each lifecycle event owns its own
field serialization before the final diagnostic map edge.

The lifecycle detail value objects now keep event-specific values in
`command/lifecycle/details/*` and delegate final map key ownership to
`command/lifecycle/details/payload/*`. This keeps active detail fields such as
`exception_type`, `queue_active_request_id`, `decision_reason`, `terminal_sent`,
and `task_finished_event` unchanged while separating event value ownership from
map serialization.

Empty lifecycle detail payloads also use
`FabricChatClefLifecycleDetailsPayload.empty()` before expanding to the same
`{}` details map at the log edge. This prevents new lifecycle call sites from
assembling raw empty `Map<String, Object>` details directly while preserving the
current emitted shape.

Optional task-finished observations in command lifecycle result data use
`command/lifecycle/observation/FabricChatClefTaskFinishedObservationPayload`
before expanding to the existing `task_finished_event_received` boolean and
`task_finished_observation` object. The final map expansion is isolated under
`command/lifecycle/observation/payload/*`. Missing observations still serialize
as `task_finished_observation={}`.

Command diagnostic log payloads keep `FabricChatClefCommandDiagnosticLogPayload`
as the facade used by the diagnostics emitter. Execution, context, and shared
`event` / `details` field assembly now live under
`command/diagnostics/payload/log/*`, and the diagnostics facade no longer
accepts new raw `Map<String, Object>` detail overloads. Existing emitted log
keys and detail object shapes remain unchanged.

Command lifecycle result data now keeps the lifecycle value object in
`command/lifecycle/FabricChatClefCommandLifecyclePayload` and delegates final
map field ownership to `command/lifecycle/payload/*`. This folderization keeps
the existing `result_fidelity`, ownership, task snapshot, and
`task_finished_observation` shapes unchanged while separating lifecycle state
from the serialization edge.

Deadline exceeded result data follows the same lifecycle payload pattern:
`FabricChatClefCommandDeadlinePayload` preserves the value wrapper while
`command/lifecycle/payload/FabricChatClefCommandDeadlinePayloadMap` owns the
existing `automation_cancelled`, `task_may_still_be_running`, and
`late_terminal_event_will_be_ignored` map keys.

Termination observations now keep their value wrapper in
`FabricChatClefCommandTerminationObservationPayload` and delegate final field
ownership to `command/lifecycle/payload/FabricChatClefCommandTerminationObservationPayloadMap`.
The emitted `completion_source`, `termination_kind`, stop-state, duration, and
`task` fields remain unchanged.

Command ownership and task observation value objects follow the same pattern:
the public value object remains in its existing package, while final map key
ownership lives under `command/ownership/payload/*` and
`command/observation/payload/*`. Task snapshot field ownership is split between
the typed snapshot payload and `FabricChatClefTaskSnapshotPayloadMap` so
`available`, `class_name`, `identity`, task-state, `current_task`, and
`bound_root_task` fields stay unchanged while the Map edge remains isolated.
Command ownership session fields are split under
`command/ownership/payload/session/*` so `request_id`, `correlation_id`,
`session_id`, `connection_generation`, `accepted_at_ms`, `detached`, and
`detached_reason` stay unchanged while request/session metadata and detach
state are written by separate helpers. Request id, correlation id, session id,
connection generation, and acceptance timestamp ownership fields are further
split under
`command/ownership/payload/session/request/*` and
`command/ownership/payload/session/connection/*` without changing emitted
keys.
Detach ownership fields are further split under
`command/ownership/payload/session/detach/*` so `detached` and
`detached_reason` remain separately owned without changing emitted keys.
Task ownership snapshot field groups are further split under
`command/observation/payload/ownership/*` so capture metadata, UserTask root
fields, and selected-chain fields can change internally without renaming the
existing emitted keys.
Runtime task observation fields are split under
`command/observation/payload/runtime/*` so capture timing/thread fields and
the `current_task` / `ownership` nested payload fields remain separately owned
while the existing emitted shape stays unchanged.
Bound-root relationship fields are split under
`command/observation/payload/relationship/*` so candidate task snapshots,
`bound_root_task`, and the suffix-derived match/reason fields remain separately
owned while preserving the current emitted keys and nesting.
Task snapshot field writers are split under
`command/observation/payload/snapshot/*` so summary fields such as `available`,
`class_name`, `description`, `identity`, and `error` stay separate from
runtime state fields such as `task_state_available`, `task_active`,
`task_stopped`, `this_or_child_timed_out`, and `task_state_error` without
changing the emitted snapshot shape. Snapshot summary fields now split
availability/error and identity writers under
`command/observation/payload/snapshot/summary/*` while preserving the same
summary keys. The availability/error summary writer delegates the `available`
and `error` keys to separate field writers without changing their emitted names.
The identity summary writer delegates the `class_name`, `description`, and
`identity` keys to separate field writers under
`command/observation/payload/snapshot/summary/identity/*` without changing their
emitted names.
Snapshot runtime state fields now split availability, boolean state flags, and
state error writers under `command/observation/payload/snapshot/state/*` while
preserving the same `task_state_available`, `task_active`, `task_stopped`,
`this_or_child_timed_out`, and `task_state_error` keys. Boolean state flags now
delegate `task_active`, `task_stopped`, and `this_or_child_timed_out` to
separate field writers under `command/observation/payload/snapshot/state/flags/*`.
Task-finished lifecycle detail field writers are split under
`command/lifecycle/details/payload/taskfinished/*` so the
`task_finished_event`, bound-root match fields, `finish_callback_received`, and
`runtime` fields keep their existing log shape while each detail group owns its
own final Map writes.
Task-finished bound-root matching and runtime fields are further split under
`command/lifecycle/details/payload/taskfinished/match/*` and
`command/lifecycle/details/payload/taskfinished/runtime/*` so match flags,
relationship fields, match reasons, callback state, and runtime snapshots stay
separate without changing emitted keys.
Waiting and finish-callback lifecycle detail writers are split under
`command/lifecycle/details/payload/waiting/*` and
`command/lifecycle/details/payload/finish/*` so waiting reason, current-task
relationship fields, callback current-task fields, and runtime fields remain
separate while preserving the existing diagnostic detail keys.
Finish-callback current-task relationship and runtime snapshot fields are
further split under `command/lifecycle/details/payload/finish/current/*` and
`command/lifecycle/details/payload/finish/runtime/*` while preserving the
existing diagnostic detail keys.
Waiting current-task relationship and match-reason fields are further split
under `command/lifecycle/details/payload/waiting/current/*` while preserving
the existing diagnostic detail keys.
Terminal-result lifecycle detail writers are split under
`command/lifecycle/details/payload/terminal/*` so the `terminal_sent` and
`lifecycle_cleared` ordering fields stay separate while preserving the existing
diagnostic detail keys.
Queue context mismatch lifecycle detail writers are split under
`command/lifecycle/details/payload/queue/*` so `queue_active_present` and
`queue_active_request_id` stay separately owned while preserving the existing
diagnostic detail keys.
Exception lifecycle detail writers are split under
`command/lifecycle/details/payload/exceptiondetail/*` so `exception_type` and
`exception_message` keep their existing diagnostic detail keys while separating
exception classification from exception text serialization.

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

Typing direction: keep event-specific detail payloads flexible, but do not add
raw `Map<String, Object>` overloads to the diagnostics facade. Temporary
diagnostic fields should be represented by a typed detail payload and expanded
only at the existing log serialization edge.

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
   expand to the existing map shape only in
   `FabricChatClefCommandDiagnosticLogPayload`.
6. Keep diagnostic log field names stable while routing temporary detail fields
   through typed detail payloads.
7. Keep request `metadata` and Python UI dictionaries flexible unless a
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
- Do not modify the existing command lifecycle, ownership, or task observation
  payload classes or their `toMap()` shapes merely to attach a new
  investigation field.
- Use separate bounded diagnostic events for new DestroyBlockTask, Baritone
  path, blacklist, and movement-progress observations.
- Use existing `DiagnosticCommandContextSnapshot` / command-context helper
  fields on those new events instead of appending command context fields to
  lifecycle payloads.
- Deduplication and emission budgets may affect only logging volume.
- Diagnostic budgets are not timeouts and must not change command lifecycle.
- `behavior_effect` must remain `none` for diagnostics-only changes.
- Do not turn an observation failure into success.
- Do not add a `try/catch` around observed engine behavior solely to protect a
  diagnostic event.
- Formatter-local protection is allowed only for malformed optional diagnostic
  values and must not suppress ChatClef, AltoClef, Baritone, input, transport,
  or container exceptions.

First-pass event names for the current DestroyBlockTask / Baritone path
investigation:

```text
TASK_CHILD_RECONCILIATION
MINE_TARGET_SELECTION_TRANSITION
BARITONE_EXISTING_CANCEL_BOUNDARY
DESTROY_NAVIGATION_STATE_TRANSITION
BARITONE_GOAL_PATH_TRANSITION
MOVEMENT_PROGRESS_CHECK_RESULT
BLOCK_UNREACHABLE_REQUEST
BLOCK_BLACKLIST_STATE_CHANGED
```

These names are diagnostic event contracts only. They do not change
`command_request`, `command_result`, `StatusSnapshotDTO`, lifecycle payload,
ownership payload, or task runtime observation payload shapes.

For these events, exclude the following from dedupe fingerprints:

```text
clientTickId
timestamp
candidate task instance ID
System.identityHashCode
opaque toString
exact per-tick player position
random operation ID
```

Preferred fingerprint dimensions include:

```text
commandCorrelationId
event
ownerTaskClass
activeChildClass
targetPosition
navigationState
customGoalActive
baritonePathing
pathPresent
isEqualResult
replacementApplied
scannerUnreachable
```

## Evidence File List

Java protocol and command files:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/protocol/FabricChatClefBridgeEnvelope.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/protocol/FabricChatClefBridgeJson.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/protocol/FabricChatClefBridgeMessageFactory.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/protocol/handshake/FabricChatClefHandshakePayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/protocol/handshake/FabricChatClefHandshakeCapabilitiesPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/protocol/handshake/FabricChatClefHandshakeMetadataPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandRequest.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/request/FabricChatClefCommandRequestFields.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandResult.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandContext.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/ownership/FabricChatClefCommandOwnershipPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandResultSender.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/transport/FabricChatClefResultEnvelopeSender.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/protocol/result/FabricChatClefCommandResultEnvelopeFactory.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandDiagnosticPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandLifecyclePayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/details/FabricChatClefTaskFinishedEventDetailsPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefBoundRootTaskRelationshipPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefTaskRuntimeObservationPayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/observation/FabricChatClefTaskSnapshot.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandDeadlinePayload.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandTerminationObservation.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/diagnostics/FabricChatClefCommandDiagnostics.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/diagnostics/payload/FabricChatClefCommandDiagnosticLogPayload.java
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
