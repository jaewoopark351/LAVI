<!-- 20260801_kpopmodder: Drafted the inert v1 wire contract for the Fabric ChatClef bridge. -->
<!-- 20260821_kpopmodder: Clarified that Python-local reconciliation state must not leak into Java-facing status snapshots. -->
<!-- 20260905_kpopmodder: Documented the additive Fabric ChatClef STOP-control capability and metadata/data profile without a v1 version or message-type change. -->
<!-- 20260905_kpopmodder: Recorded final offline STOP-profile regression and clean-build evidence. -->
<!-- 20260906_kpopmodder: Distinguished the later verified STOP/bridge runtime from the downstream wooden-button engine StackOverflow. -->
<!-- 20260906_kpopmodder: Recorded the downstream button fix's offline-built/runtime-unverified follow-up without changing the bridge contract. -->
<!-- 20260906_kpopmodder: Recorded matching-JAR generic-button success from Chat and final microphone without changing the bridge contract. -->
<!-- 20260907_kpopmodder: Implemented and offline-verified additive exact GET inventory-delta evidence for natural crafting feedback. -->
<!-- 20260907_kpopmodder: Linked the docs-only all-command effect-profile proposal without changing the current v1 wire or exact GET projection. -->
<!-- 20260907_kpopmodder: Documented the existing STORE_HOME data profile and the source-known initial-running sequence compatibility gap. -->
<!-- 20260908_kpopmodder: Reconciled Python's strict STORE_HOME success consumer without changing the v1 wire profile. -->

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

The current worktree also implements an optional Fabric ChatClef STOP-control
profile inside the same v1 `command_request` / `command_result` envelopes.
That additive profile is `IMPLEMENTED_VERIFIED_OFFLINE`: the full Python suite
passed 2093 tests with 4 skips and 4496 subtests, the 1.20.1 Java test XML
records 216 suites and 762 tests with 0 failures, 0 errors, and 1 skip, and the
required clean forced Gradle build executed all 171 tasks. The fresh 1.20.1
runtime JAR SHA-256 is
`0C8E7F774C52D79DE5BD1BB9B0E516F6E75C5C20CF5D73414EDD6D8154128161`.
This evidence was offline only at the close of that verification ledger;
deployment and Minecraft runtime were then `NOT_RUN`. A later matching-JAR
session verified the STOP profile through its closed terminal and dispatched
ordinary crafting commands. Its subsequent `get wooden_button 1`
`StackOverflowError` occurred inside the assigned AltoClef resource Task, after
bridge admission and root binding, and is not a v1 envelope or STOP-control
failure. See
[ChatClef Wooden Button Stack Overflow Pre-Change Root-Cause Report](chatclef-wooden-button-stack-overflow-pre-change-report-2026-09-06.md).
The minimum downstream recipe-mask correction is now applied. Its focused Java
suite passes 5 tests without skips, the refreshed clean forced build executes
all 171 tasks, and the corrected mixed-provenance 1.20.1 JAR has SHA-256
`488C195B8C87919E349549DD5B0766D05D669222BB63FCCDEDD0E38FDB2861B2`.
The active test-instance JAR is byte-identical. Generic wooden-button requests
from Chat twice and an exact `VoiceInput` final transcript once all reached
natural terminal completion without `StackOverflowError`; the microphone path
also delivered and played its Korean TTS feedback once. The downstream
divergence is `PARTIALLY_VERIFIED` because live explicit-oak and stone-button
comparisons remain unexercised. This downstream change does not alter the v1
envelope, STOP-control profile, transport, or dispatch contract.
The profile does not add a top-level message type, change `protocol_version`,
or extend the common DTO field set.

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

`RECONCILED_UNKNOWN` is not a v1 `status` value and must not be added to this
status list. A Python-local stale active-command reconciliation, when
separately approved, uses the existing `status=unknown` value with `ok=false`.
The reconciliation action may be recorded only inside `CommandResultDTO.data`,
for example:

```text
status=unknown
ok=false
data.result_reason=terminal_reconciliation_gap
data.reconciliation_action=RECONCILED_UNKNOWN
data.gameplay_effect=UNVERIFIED
```

That synthetic Python-local result must not be interpreted as Java command
completion, Java task failure, deposit success, deposit failure, or verified
gameplay effect. If a matching Java terminal result arrives later, it is handled
by Python's reconciliation tombstone policy; it does not define a new wire
status and must not automatically replace the committed synthetic UNKNOWN,
mutate a newer active command, or open new-command admission.

Python-local synthetic UNKNOWN, reconciliation tombstones, and admission
quarantine are not wire protocol state. They must not be included in
`handshake_ack.payload.status`, `status_snapshot.payload`, or any other
Java-facing status response. A Python implementation may expose that state only
through Python-local router/UI/admission/audit snapshots that are not sent over
the Fabric ChatClef WebSocket protocol.

### Nonterminal Lifecycle Evidence Updates

Java may send additive `command_result` envelopes with `status=running` to
publish nonterminal lifecycle evidence for the currently active command. This
uses the existing `command_result` message type and existing
`CommandResultDTO.data` map. It does not introduce a new v1 message type,
public status value, DTO field, or status snapshot schema.

Python must validate the websocket, session, generation, request, and
correlation identity exactly as it does for any other command result. An
accepted `running` result may replace `details.commands.last_result`, but it
must not clear active command ownership. Only accepted terminal statuses may
clear the active command.

Allowed nonterminal diagnostic stages include:

```text
result_reason=finish_callback_observed_nonterminal
result_reason=stable_request_quiescence_observed
status=running
classification=nonterminal_diagnostic
gameplay_effect=UNVERIFIED
```

These updates are observation only. They must not be named or interpreted as
`command_completed`, `terminal_candidate_confirmed`, `gameplay_success`,
`task_success`, or `safe_to_release`. The Java bridge reports raw lifecycle
facts; Python remains responsible for exact active-ownership identity checks
and any later Python-local stale active-command reconciliation.

Java nonterminal evidence should remain `status=running` even when it reports
`finish_callback_received=true`, `task_finished_event_received=false`, and a
stable request quiescence window. Java does not decide
`identity_quality=EXACT`, does not decide `RECONCILED_UNKNOWN`, and does not
clear Python active ownership.

When present, `data.evidence_sequence` is additive lifecycle evidence metadata.
It should increase monotonically within one Java command execution and helps
Python decide which accepted nonterminal lifecycle evidence is newer. It is not
a new top-level DTO field, not required on terminal results, and not a wire
authorization for active release. Python-local sequence ordering, lifecycle
fingerprints, CAS, tombstones, and admission quarantine remain implementation
details outside this v1 wire contract.

Current compatibility note: Java now publishes the initial exact
`running/dispatch_started` result with `evidence_sequence=1`, and the same
execution-wide sequence source assigns `2+` to later finish/quiescence
observations. Python still rejects missing, boolean, zero, negative, duplicate,
or decreasing values. A shared serialized-envelope fixture is consumed by both
the Java producer test and the Python DTO/reconciliation test, so this rule is
verified at the real wire shape rather than only through hand-built Python
snapshots.

### Existing STORE_HOME Result Data Profile

The current Java bridge may decorate an ordinary STORE_HOME result's
`CommandResultDTO.data` with these exact six operation-specific fields in
addition to generic lifecycle data:

```text
operation=store_home
store_home_result=<closed result name>
stored_items=<nonnegative exact integer>
remaining_stacks=<nonnegative exact integer>
reason=<nonempty bounded stable reason>
goal_satisfied=<literal boolean>
```

The Python parser recognizes this specialized claim when either
`operation=store_home` or `store_home_result` is present. A malformed claim is
owned by the STORE_HOME validator and must not fall through to a generic
success renderer. It requires a known closed result, rejects booleans as
integers, requires `goal_satisfied == (store_home_result == COMPLETED)`, requires
`remaining_stacks=0` for COMPLETED, and requires `stored_items>0` for every
`PARTIAL_` result. A COMPLETED result with `stored_items=0` means there was
nothing to store; cursor, stale-manifest, context-change, unavailable-target,
unconfirmed-transfer, and interrupted outcomes are not success.

The extension may mirror the same fields in `status.data` and `details`. If
both locations claim STORE_HOME, all six values must agree exactly. The current
typed parser/renderer is not a dedicated asynchronous STORE_HOME lifecycle
tracker and does not by itself validate the entire outer DTO. Any natural
terminal publisher must first pass ordinary active-owner reconciliation. A
strong COMPLETED success additionally requires DTO `status=completed`,
`ok=true`, and `error_code=null`; if it consumes the extension wrapper,
top-level `error` must also be null. Typed non-success outcomes follow a closed
outcome/status matrix instead—`CURSOR_NOT_EMPTY`, for example, may accompany
the pre-existing-unchanged-idle-root UNKNOWN path—and must never be rendered as
success.

### Single-Target GET Effect Evidence

The current Java bridge implements an observation-only effect projection for a
closed, prefixless or single-`@`, single-target `get <target> <positive-count>`
request. It resolves the target through the existing TaskCatalogue matcher,
captures the sum across inventory and cursor stacks, and decorates the existing
matching-task terminal result. Bracket-list/multi-target GET forms, malformed
forms, and non-GET commands do not receive positive effect evidence.

The authoritative v1 profile is nested under the existing
`CommandResultDTO.data` map:

```text
effect_profile_id=fabric_chatclef_get_acquire_delta
effect_profile_version=1
effect_kind=get_acquisition_delta
effect_payload:
  target_item=<validated command target>
  target_match_ids=<sorted, unique minecraft:item ids; 1..2048 entries>
  quantity_semantics=ACQUIRE_DELTA
  requested_delta=<positive exact integer>
  before_target_count=<nonnegative integer or null>
  after_target_count=<nonnegative integer or null>
  target_count_delta=<integer or null>
  effect_observation_status=authoritative
      | before_unavailable
      | after_unavailable
      | stale_world_binding
      | unavailable
  effect_observation_reason=<bounded stable reason>
```

For backward compatibility only, the exact normalized command
`get diamond_pickaxe 1` also mirrors these legacy flat fields:

```text
effect_kind=get_acquisition_delta
target_item=diamond_pickaxe
requested_count=1
before_target_count=<integer or null>
after_target_count=<integer or null>
target_count_delta=<integer or null>
effect_observation_status=authoritative
    | before_unavailable
    | after_unavailable
    | stale_world_binding
    | unavailable
effect_observation_reason=<bounded stable reason>
```

The before observation is captured on the Minecraft client thread before the
command executor can mutate inventory. The after observation is captured once
when the matching Task-finished completion payload is constructed. Both counts
must refer to the same live world and player object identity for the status to
be `authoritative`; unavailable reads and a changed binding use null for any
unproven count or delta instead of fabricating zero. The match-id set is copied,
sorted, deduplicated, and bounded before it is emitted.

These fields are additive evidence, not a new status or success decision.
Python may say a strong GET completion sentence only after its existing
websocket/session/generation/request/message reconciliation accepts the result,
the lifecycle reason and fidelity are exact, the descriptor agrees with the
entire closed profile, and the authoritative delta is at least the requested
quantity. If nested and legacy flat fields coexist, every duplicated value must
agree. The separate Python STORE_HOME evaluator now consumes the existing typed
STORE_HOME profile for only its strictly verified trusted-translation
`COMPLETED`/zero-work slice. Every other non-GET profile remains cautious at
terminal unless its own typed effect oracle is added; this does not make those
accepted commands silent. No Java Task selection, completion rule, retry,
input, path, or command action is changed by this observation.

The all-item/all-command response implementation and the boundary between
strong GET/STORE_HOME evidence and cautious terminal narration are recorded in
[General Natural Korean Command Lifecycle Feedback Pre-Change Contract](chatclef-general-natural-korean-command-lifecycle-feedback-pre-change-contract-2026-09-07.md).

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

When Python internally reconciles a stale active command to local
`status=unknown`, the Java-facing status snapshot still reports only
Java-originated command result state. The local effective result, quarantine,
and tombstone are Python-side admission/audit state and are intentionally
outside the v1 payload shape.

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
payload.capabilities.chatclef_stop_control_v1 = true
payload.metadata.backend = fabric_chatclef
payload.metadata.loader = fabric
payload.metadata.phase = phase_4_tick_dispatch
```

`chatclef_stop_control_v1` is supported only when its JSON value is the boolean
literal `true`. The string `"true"`, the number `1`, a missing key, `false`, or
any other value is not support. Python must then reject the STOP-control request
locally with `stop_control_capability_unavailable` and must not send a control
frame.

The Java bridge must not call `TaskRunner`, Baritone APIs, input override APIs,
path/goal APIs, or Minecraft player/world mutation APIs during Phase 4 command
transport. `CommandExecutor.execute(...)` is allowed only from the dedicated
client-tick dispatcher.

## Phase 4 Ordinary Command Flow

Python sends an ordinary `command_request` only when there is an active Fabric
Java bridge session and no pending or active ordinary command request. The
additive STOP-control profile below uses the same v1 envelope type but a
separate priority lane, barrier, and quarantine lifecycle:

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

Only one ordinary command may be pending or active. Disconnection clears the
ordinary Java queue and does not replay that command on reconnect. STOP-control
admission and replay behavior are governed by the additive profile below.

## Additive STOP-Control Profile Within V1

The STOP-control lane stops current global ChatClef automation through the
existing registered `StopCommand -> AltoClef.stop()` path. It is not an
ordinary second task command and does not weaken ordinary single-active-command
admission. WebSocket callbacks validate and enqueue only; `END_CLIENT_TICK`
owns the Minecraft/ChatClef mutation.

The profile reuses these existing v1 values:

```text
protocol_version = 1
request envelope message_type = command_request
result envelope message_type = command_result
```

No `BridgeMessageType`, `CommandRequestDTO`, `CommandResultDTO`, common JSON
schema union, or backend-neutral status value is added.

### STOP-Control Request Metadata

The request payload is an ordinary `CommandRequestDTO` with:

```text
request_id = unique STOP-control request ID
command = stop
source = lavi_chat_ui | voice_input_final
deadline_ms = Python submission time + 2000 ms

metadata.request_kind = stop_control_v1
metadata.operation = stop_ai
metadata.input_event.source
metadata.input_event.provider_id
metadata.input_event.event_kind
metadata.input_event.final
metadata.input_event.event_id
metadata.server_connection_generation
metadata.target_scope = tracked_command | current_global_automation
```

`metadata.input_event` is bounded audit correlation copied only after Python's
in-process trusted-ingress and one-shot STOP claim checks. It is not a
serializable claim or independent Java authorization. No claim receipt, nonce,
registry token, or capability object crosses the wire.

For `target_scope=tracked_command`, all four fields below are required and form
one immutable original-command retirement correlation quartet:

```text
metadata.target_request_id
metadata.target_command_message_id
metadata.target_session_id
metadata.target_server_connection_generation
```

For `target_scope=current_global_automation`, all four target fields are
forbidden. Target fields correlate retirement of a Python-tracked ordinary
command; they do not narrow the global effect of `AltoClef.stop()`.

A frame with either exact marker, or `command=stop` plus presence of either
marker key, is a STOP-control candidate. Once claimed as a candidate it must not
fall through to the ordinary queue when the profile is partial or malformed.
An ordinary legacy `command=stop` request with neither marker key preserves its
pre-existing ordinary-command behavior.

### STOP-Control Result Data

A STOP-control result is an ordinary `CommandResultDTO`. Its envelope and
payload echo the request identity as follows:

```text
payload.request_id = original STOP-control request_id
envelope.correlation_id = original command_request envelope message_id
envelope.session_id = original command_request session_id
data.connection_generation = original metadata.server_connection_generation
```

Every wire-result-eligible STOP terminal carries this complete additive `data`
profile:

```text
data.request_kind = stop_control_v1
data.operation = stop_ai
data.control_outcome = stopped | rejected | unknown
data.control_reason
data.target_scope = tracked_command | current_global_automation | null
data.target_resolution = not_evaluated | exact | captured_current | none | unknown

data.requested_target_request_id
data.requested_target_command_message_id
data.requested_target_session_id
data.requested_target_server_connection_generation

data.resolved_target_request_id
data.resolved_target_command_message_id
data.resolved_target_session_id
data.resolved_target_server_connection_generation

data.target_state_before = pending | active | none | not_evaluated | unknown
data.target_state_after = retired | unchanged | none | not_evaluated | unknown
data.original_result_delivery = sent | not_applicable | failed | unknown
data.stop_command_invoked = true | false
data.connection_generation
data.java_socket_generation
data.executed_client_tick
data.verified_client_tick
```

`connection_generation` is Python's server-assigned generation and participates
in result acceptance. `java_socket_generation` is a distinct positive Java-local
diagnostic fence; the two values must not be assumed equal. Tick values are
non-negative integers or null. A captured-context result may verify from tick
`T` through `T+20`; a no-context success must use
`executed_client_tick=verified_client_tick=T`.

The closed terminal classes are:

| Result class | `status` / `ok` / `error_code` | `control_outcome` | Meaning |
| --- | --- | --- | --- |
| Verified STOP | `completed` / `true` / `null` | `stopped` | `StopCommand` ran exactly once and the applicable original-command retirement evidence is complete. |
| Proved no mutation | `rejected` / `false` / `invalid_request` | `rejected` | Profile, session, generation, scope, target fields, or distinct in-flight admission rejected the request without STOP mutation. |
| Deadline before mutation | `deadline_exceeded` / `false` / `deadline_exceeded` | `rejected` | The bounded control deadline expired before STOP mutation. |
| Uncertain | `unknown` / `false` / `null` or `internal_error` | `unknown` | Observation, invocation, original-result delivery, or bounded verification could not prove a safe terminal. No automatic replay is allowed. |

Python validates the complete profile and the live tracker identity before
publishing a terminal response or releasing its STOP barrier. A partial,
contradictory, unknown-reason, stale, or untracked STOP-marked result is not
passed to the ordinary result handler. `unknown` is quarantine evidence, not a
barrier-release authority and not proof that the AI stopped.

### Compatibility Matrix

| Python side | Fabric Java side | Required behavior |
| --- | --- | --- |
| STOP-profile aware | Advertises literal `chatclef_stop_control_v1=true` | The guarded STOP-control request/result profile may be used. |
| STOP-profile aware | Capability absent, `false`, or not literal boolean `true` | Send zero STOP-control frames; return the local capability-unavailable rejection. Ordinary commands remain unchanged. |
| Legacy/unaware | STOP-profile aware | The capability is ignored. A legacy `command=stop` without marker keys follows the existing ordinary path; Java does not synthesize a control request. |
| Legacy/unaware | Legacy/unaware | Existing v1 command dispatch remains unchanged. |
| STOP-profile aware | STOP-profile aware but a control candidate is malformed | The control validator owns the candidate and rejects or quarantines it fail-closed; it never falls through as an ordinary second command. |

The capability and profile are Fabric ChatClef-owned. They do not create a
Forge/MineMind value, placeholder, transport, queue, session implementation, or
runtime dependency.

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

## 2026-08-22 Sync-Finish Idle-Root UNKNOWN

When a ChatClef command invokes the finish callback synchronously, creates no
command-owned user task root, and the Java bridge proves the post-dispatch root
is the same pre-existing automatic `IdleTask`, the terminal result remains a
normal `command_result` envelope but must not be reported as success.

Required terminal fields:

```text
status = unknown
ok = false
data.result_reason = finish_callback_without_new_command_owned_root
data.result_fidelity = callback_without_matching_user_task_event
```

This is not a protocol-version bump because the envelope, status key, and
`command_result.data` map remain v1-compatible. The new fidelity value states
that Java observed a command callback but no matching command-owned
`TaskFinishedEvent`.
