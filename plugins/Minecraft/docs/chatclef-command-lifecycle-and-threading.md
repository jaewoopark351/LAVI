<!-- 20260806_kpopmodder: Documented Fabric ChatClef command lifecycle, ownership, and thread-affinity boundaries. -->

# ChatClef Command Lifecycle And Threading

Date: 2026-08-06

This document describes the current Fabric ChatClef command lifecycle and the
threading boundaries that must be preserved while debugging command hangs,
unknown terminal results, delayed task starts, or missing command completion.

It is documentation only. It does not approve Java changes, Python behavior
changes, Gradle changes, dependency changes, Minecraft launch, runtime
reproduction, commit, or push.

## Scope

```text
plugins/Minecraft/common/dto/**
plugins/Minecraft/common/protocol/**
plugins/Minecraft/fabric/chatclef/**
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/**
```

The runtime tree also contains upstream-derived ChatClef / AltoClef engine
code. This document records how LAVI observes and drives that engine through
the Fabric bridge. It does not redefine TaskRunner, Baritone, or generic
ChatClef ownership.

## Related Documents

```text
plugins/Minecraft/README.md
plugins/Minecraft/docs/fabric-chatclef-bridge-protocol-v1.md
plugins/Minecraft/docs/chatclef-command-payload-map-audit.md
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/LAVI_INTEGRATION.md
```

## Public Status Values

The v1 protocol exposes these Python-side lifecycle states through
`StatusSnapshotDTO.lifecycle_state`:

```text
disabled
disconnected
connected
stopped
failed
```

These are UI and protocol status values. They are not the same thing as a Java
command execution state or a ChatClef Task state.

Conceptually, a connection may pass through internal phases such as:

```text
disabled
  -> stopped
  -> starting
  -> disconnected
  -> handshaking
  -> connected
  -> disconnected
  -> stopped
```

Only the public status values above should be treated as the v1 wire contract.
Do not add new public status values without updating both protocol documents
and Python DTO handling.

## Connection Ownership

Python owns the Fabric ChatClef WebSocket server:

```text
FabricChatClefWebSocketServer
FabricChatClefConnectionOwnership
FabricChatClefSessionRegistry
```

Python connection ownership includes:

```text
server enabled/disabled state
asyncio loop and server thread
active WebSocket identity
active session ID
connection generation
single active command request
last accepted terminal command result
status snapshots used by UI and extension callers
```

Java owns the Fabric-side WebSocket client:

```text
FabricChatClefBridgeClient
FabricChatClefBridgeEntrypoint
```

Java connection ownership includes:

```text
client connection to the Python endpoint
handshake envelope
reconnect timing inside the Java bridge client
command_result envelope sending
status and error envelope handling local to the Java bridge
```

The Python server is the authority for which Java session is active. A stale
session, stale generation, wrong request ID, or wrong correlation ID must not
clear the active command.

## Command Transport Lifecycle

The intended command lifecycle is:

```text
raw text accepted by LAVI
  -> Python command request DTO created
  -> Python ownership begins active command
  -> command_request envelope sent to active Java session
  -> Java WebSocket callback decodes and validates
  -> Java command queue receives request
  -> Fabric END_CLIENT_TICK sees engine ready
  -> request is polled from queue
  -> command executor dispatches ChatClef command
  -> root Task is bound to the command when observable
  -> terminal condition is observed
  -> Java sends command_result once
  -> Python accept_result validates ownership
  -> Python clears active command only for accepted terminal statuses
```

Use this lifecycle language when reading logs:

```text
received
validated
python_active
sent
java_enqueued
dispatch_ready
dispatched
root_task_bound
waiting_for_terminal_condition
terminal_observed
result_sent
python_result_accepted
cleared
```

These lifecycle labels are diagnostic language. They do not replace the v1
`CommandResultDTO.status` values.

## Wire Command Result Statuses

The v1 command result statuses remain:

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

`ok` is true only for:

```text
accepted
running
completed
```

Terminal statuses clear the active Python command only after the result passes
session, generation, request ID, and correlation validation.

## Deadline Semantics

`deadline_ms` is nullable and is an absolute Unix epoch millisecond deadline.

Important constraints:

```text
deadline_ms = null means no command-level deadline was requested
deadline_exceeded does not imply automatic retry
deadline_exceeded does not imply automatic command replay
deadline_exceeded does not imply automatic ChatClef task cancellation
deadline_exceeded does not prove the current Task stopped
```

Do not add a timeout, retry, cancellation, or cleanup behavior until the owner
of that state is proven.

## Java Dispatch Boundary

Java WebSocket callbacks must only perform:

```text
message decode
protocol validation
request DTO construction
queue insertion
error envelope creation for invalid bridge messages
```

They must not touch:

```text
Minecraft client world/player mutation
AltoClef task state
TaskRunner lifecycle
Baritone APIs
input override state
container clicks
screen state
path or goal state
```

Minecraft-client-thread command execution is owned by the Fabric
`END_CLIENT_TICK` dispatcher.

## Task Lifecycle Observation

The command bridge observes ChatClef task completion through multiple signals:

```text
command executor callback
bound root task identity
TaskFinishedEvent
current Task snapshot
exception callback
deadline check
```

The command should not be considered complete merely because dispatch returned.
Dispatch returning only means the command was accepted by the local Java
executor path.

When debugging `unknown` terminal results, collect:

```text
request_id
correlation_id
session_id
connection_generation
normalized_command
bound_root_task
task_before_dispatch
task_after_dispatch
terminal_task
task_finished_event_received
task_finished_observation
current_task_matches_bound_root_task
result_reason
result_fidelity
elapsed_ms
```

## Terminal And Clear Ordering

The desired terminal ordering is:

```text
terminal condition classified
  -> command_result payload built
  -> result envelope sent once
  -> Java command lifecycle records terminal_sent
  -> Java active command lifecycle is cleared
  -> Python receives command_result
  -> Python validates session/generation/request/correlation
  -> Python stores last command result
  -> Python clears active command for terminal status
```

Duplicate Java terminal sends must be ignored. Late TaskFinishedEvent signals
for a detached or already-cleared command must be logged as stale or late
observation, not used to mutate unrelated command state.

## Thread Affinity Matrix

```text
Boundary                         Thread owner                 Allowed work
Python UI callback               UI/framework thread          build request, call extension
Python transport send            asyncio server loop          send envelope, update transport state
Python ownership validation      Python server/loop context   validate result ownership, clear active command
Java WebSocket callback          bridge WebSocket thread      decode, validate, enqueue
Fabric END_CLIENT_TICK           Minecraft client thread      dispatch command, observe client state
ChatClef Task tick               Minecraft client thread      existing Task lifecycle behavior
Baritone worker                  Baritone worker thread       Baritone path calculation only
Java result outbox               bridge/client boundary       serialize and send terminal result
Diagnostics formatter            caller thread                format observation without behavior effects
```

If a diagnostic needs data from another owner, prefer a snapshot or an
already-published immutable value. Do not cross from Baritone worker code into
live Minecraft client trackers merely to make a log richer.

## State Ownership Matrix

```text
State                            Owner
WebSocket server lifecycle        Python FabricChatClefWebSocketServer
connection generation             Python FabricChatClefConnectionOwnership
active Python command             Python FabricChatClefConnectionOwnership
Java command queue                Java FabricChatClefCommandQueue
Java active command lifecycle     Java command lifecycle coordinator
bound root task identity          Java command execution/lifecycle state
TaskRunner lifecycle              ChatClef / AltoClef engine
child task selection              ChatClef / AltoClef engine
input override acquisition        existing owning Task or input chain
input override release            same owner that acquired it
Baritone goal/path ownership      existing Baritone or Task owner
diagnostic emission budget        diagnostics layer only
```

Diagnostics may own counters, fingerprints, suppression counts, and emission
budgets only when those values affect logging volume alone.

## Diagnostic Event Shape

When adding command lifecycle diagnostics, keep the payload additive and stable:

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

`behavior_effect` must be `none` for diagnostics-only events.

The full map-shape guidance lives in:

```text
plugins/Minecraft/docs/chatclef-command-payload-map-audit.md
```

## Failure Classification Hints

Do not collapse these into one theory too early:

```text
build or Mixin startup failure
runtime crash after Minecraft starts
bridge transport/session failure
command lifecycle terminal mismatch
ChatClef parent-child Task loop
Baritone path cache or target mismatch
container GUI failure
Carry On optional state observation failure
```

A current log must prove the active class of failure. A past Mixin crash does
not prove the current task loop is a build problem, and a successful build does
not prove command lifecycle ownership is correct.
