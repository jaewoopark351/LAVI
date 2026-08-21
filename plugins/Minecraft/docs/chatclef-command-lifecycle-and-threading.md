<!-- 20260806_kpopmodder: Documented Fabric ChatClef command lifecycle, ownership, and thread-affinity boundaries. -->
<!-- 20260820_kpopmodder: Documented nonterminal lifecycle evidence publication and the diagnostics-only stale-active reconciliation boundary. -->
<!-- 20260820_kpopmodder: Specified exception-safe Python reconciliation commit, late-terminal tombstone handling, and post-release admission quarantine. -->
<!-- 20260820_kpopmodder: Clarified monotonic lifecycle evidence acceptance, release-trigger purity, and restart-safe admission gating. -->
<!-- 20260821_kpopmodder: Split lifecycle source-contract proof, stable fingerprint fields, progression cursor, and Python-local snapshot boundaries. -->
<!-- 20260821_kpopmodder: Clarified Java/local result naming, canonical qualification, local UNKNOWN data placement, readiness gating, and late-result immutability. -->

# ChatClef Command Lifecycle And Threading

Date: 2026-08-06

Last updated: 2026-08-21

This document describes the current Fabric ChatClef command lifecycle and the
threading boundaries that must be preserved while debugging command hangs,
unknown terminal results, delayed task starts, or missing command completion.

It is documentation only. It does not approve Java changes, Python behavior
changes, Gradle changes, dependency changes, Minecraft launch, runtime
reproduction, active-command release, unknown reconciliation, commit, or
push.

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
plugins/Minecraft/docs/chatclef-python-command-orchestration-plan.md
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
last Java-originated command result (`last_java_result`)
Python-local effective command result (`local_effective_result`)
wire, local-admission, and audit snapshots used by their respective callers
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
  -> Java sends initial status=running dispatch evidence
  -> root Task is bound to the command when observable
  -> Java may send additive status=running lifecycle evidence updates
  -> terminal condition is observed
  -> Java sends one terminal command_result
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

### Nonterminal Lifecycle Evidence Publication

The Java bridge may publish nonterminal lifecycle evidence through additional
`command_result` envelopes whose status is `running`. These updates use the
same request, session, generation, and correlation identity as the active
command and are validated by Python before replacing `last_java_result`.

This uses the existing `command_result` channel only. It does not add a new
message type, wire status, terminal status, or required top-level schema field.
Any lifecycle fields are additive diagnostic data. A separate Java-originated
bridge status message is not the ownership authority, and Java-local logging
alone is not sufficient when Python must evaluate the active command. The
existing Java-facing `details.commands.last_result` field may surface the latest
accepted Java evidence only when that field is backed exclusively by
`last_java_result`. Python-local synthetic results, tombstones, quarantine, and
shadow-candidate state must never be serialized into that wire field. Local
admission and audit snapshots may expose `last_java_result` and
`local_effective_result` separately. Local logs remain supplementary audit
evidence.

The safe publication boundary is the client-tick nonterminal path after
`FabricChatClefCommandOutcomeClassifier` returns a nonterminal decision. This
keeps the classifier, terminal outbox, TaskFinishedEvent semantics, and
ChatClef engine behavior unchanged. Nonterminal evidence must use the ordinary
result sender, not the terminal outbox.

Recommended diagnostic stages:

```text
finish_callback_observed_nonterminal:
  finish_callback_received=true
  task_finished_event_received=false
  quiescence.qualified=false

stable_request_quiescence_observed:
  dispatch_returned=true
  finish_callback_received=true
  task_finished_event_received=false
  waiting_reason=waiting_for_task_finished_event
  quiescence.qualified=true
```

`markCommandFinish()` should record callback state only. It should not send
WebSocket evidence directly from the finish callback. A following client tick
should capture the runtime task and ownership snapshot and publish at most one
application-level nonterminal evidence update per stage for the command. A send
failure may be logged, but it must not trigger command retry, replay, stop,
terminal classification, or ownership release.

Stable quiescence is not just repeated `IdleTask`. It requires the same active
execution context, `dispatch_returned=true`, `finish_callback_received=true`,
`task_finished_event_received=false`,
`waiting_reason=waiting_for_task_finished_event`, unchanged root assignment,
generation, task, and selected-chain signature, observations on distinct client
ticks, and a configured minimum stable observation window. If runtime
observation is unavailable or the signature changes, the quiescence window is
unqualified or reset. The observation window is diagnostic evidence only; it
is not a command timeout and must not trigger stop, retry, replay, release, or a
follow-up command.

Recommended additive lifecycle evidence fields include:

```text
result_reason
evidence_sequence
dispatch_returned
finish_callback_received
task_finished_event_received
waiting_reason
quiescence.qualified
quiescence.observation_count
quiescence.first_client_tick_id
quiescence.last_client_tick_id
quiescence.stable_duration_ms
quiescence.signature_version
gameplay_effect=UNVERIFIED
```

`evidence_sequence` should increase monotonically within one Java command
execution. Python status polling must only read the accepted sequence; it must
not create a new observation or increment Java quiescence counters.

Python acceptance of additive lifecycle evidence must be monotonic for one
exact active identity:

```text
lower sequence than the latest accepted lifecycle evidence
  -> reject
  -> do not regress last_java_result

same sequence + same lifecycle fingerprint
  -> idempotent no-op

same sequence + conflicting lifecycle fingerprint
  -> reject
  -> emit explicit conflict diagnostics

higher sequence
  -> accept only after normal websocket, session, generation, request, and
     correlation validation
```

The implementation plan must define the exact lifecycle fields included in that
fingerprint. A matching terminal result remains stronger than additive
nonterminal evidence even when the terminal envelope does not carry
`evidence_sequence`. Conversely, the future `RECONCILED_UNKNOWN` release path
must fail closed when the qualifying stable-quiescence evidence has no accepted
sequence or when that sequence changes before CAS commit.

Java must not report `identity_quality=EXACT`, `safe_to_release`, command
completion, task success, gameplay success, or a reconciled terminal result.
Exact identity quality belongs to Python because Python compares the received
envelope against active command ownership. A `running` evidence update must
never clear active ownership.

Python must receive these updates through the existing `accept_result` identity
validation path. An accepted `running` update may replace
`last_java_result`, but it must leave the active request and
`local_effective_result` unchanged. Rejected stale-session, stale-generation,
wrong-request, or wrong-correlation evidence must not replace
`last_java_result`.

If Java-local lifecycle evidence is newer than Python's accepted `last_java_result`,
the state remains an evidence publication, transport, or acceptance gap until
the missing boundary is proven. It is not evidence of command completion,
gameplay success, or a safe ownership release.

#### 2026-08-21 Source-Contract And Cursor Correction

The production `RECONCILED_UNKNOWN` mutation path is blocked until the exact
Java evidence producer is tied to the same source and build identity as the
logs under review. Before enabling mutation outside controlled tests, preserve:

```text
sequence=1 raw command_result envelope
sequence=2 raw command_result envelope
Java source commit that produced both envelopes
runtime JAR/source/build identity that produced the log evidence
```

If the repository baseline under review does not contain the producer for
`evidence_sequence`, `lifecycle_evidence.version`,
`lifecycle_evidence.stage`, `stable_request_quiescence`,
`same_session_generation`, and `request_root_reappeared`, then Python may only
implement strict parsing, feature-OFF shadow audit, synthetic fixture-based
unit tests, and test-injected guarded mutation. Production mutation remains
blocked.

The behavior gate must be modeled explicitly:

```text
requested_enabled =
  config.reconcile_stale_deposit_to_unknown_enabled

runtime_ready =
  an injected capability proving that guarded mutation is permitted

effective_enabled =
  requested_enabled and runtime_ready
```

`runtime_ready` must not be writable through ordinary configuration or an
environment mapping. Production composition keeps it `false` while the source
contract and restart-safe admission gate remain unproven. Controlled tests may
inject it as `true`; setting the requested feature flag alone must never enable
production mutation. `effective_enabled` controls mutation only. Feature-OFF
shadow classification may still run, but its observation state must remain
bounded and behavior-neutral, must be discarded on active-identity, session,
generation, terminal, or coordinator-instance replacement, and must never seed
a later enabled commit.

`evidence_sequence` and envelope message IDs are not stable fingerprint fields.
They are progression cursors. The reconciliation model must keep these groups
separate:

```text
active_identity_key:
  websocket object identity
  request_id
  session_id
  connection_generation
  command_message_id / envelope.correlation_id
  active command
  active source

stable_evidence_fingerprint:
  lifecycle_evidence.version
  lifecycle_evidence.stage
  dispatch_returned
  finish_callback_received
  task_finished_event_received
  waiting_reason
  classification
  terminal_status
  gameplay_effect
  normalized bound-root task projection
  normalized current-root task projection
  stable_request_quiescence.qualified
  same_session_generation
  request_root_reappeared

progression_cursor:
  previous_sequence
  current_sequence
  previous_envelope_message_id
  current_envelope_message_id
```

The initial guarded mutation test path should require exact `1 -> 2`
progression, the same stable evidence fingerprint, the same expected active
object, distinct nonblank envelope message IDs, and no intervening
contradictory or nonqualifying running evidence. Broader acceptance such as
`1 -> 3` is a later design question.

Strict raw typing is required before a running envelope can qualify:

```text
type(payload.request_id) is str
type(payload.ok) is bool and payload.ok is True
payload.status == "running"
payload.data is Mapping
type(evidence_sequence) is int
type(dispatch_returned) is bool
type(finish_callback_received) is bool
type(task_finished_event_received) is bool
```

Do not rely on `CommandResultDTO` coercion for the mutation predicate. A raw
payload value such as `"false"`, `"true"`, `1`, `0`, `2.0`, or `"2"` must not
qualify as typed lifecycle evidence.

The source-contract, active-identity, stable-fingerprint, progression-cursor,
and strict-typing requirements in this correction are mandatory parts of the
same canonical reconciliation qualification predicate as the diagnostics-only
conditions below. Neither section is sufficient by itself.

### Diagnostics-Only Reconciliation Boundary

The first implementation stage is diagnostics-only and dry-run. Python may
evaluate whether the latest accepted evidence would qualify for reconciliation,
but it must not create a terminal result or clear active ownership. It also
must not retry or replay the command, stop the task, submit a follow-up
command, or infer gameplay success.

Required dry-run output includes:

```text
reconciliation_mode=dry_run
identity_quality=EXACT or a blocking non-EXACT value
would_reconcile_to_unknown=true|false
active_release_performed=false
blocked_reason=<nonblank reason when false>
gameplay_effect=UNVERIFIED
```

`would_reconcile_to_unknown=true` is allowed only when all of the following are
true for the current active command and all mandatory source-contract, cursor,
fingerprint, distinct-message-ID, active-object-identity, no-contradiction, and
strict-raw-typing requirements above are also satisfied:

```text
Python accepted the evidence through the existing identity-validation path
identity_quality=EXACT
latest accepted status=running
latest accepted result_reason=stable_request_quiescence_observed
latest accepted evidence_sequence still matches
dispatch_returned=true
finish_callback_received=true
task_finished_event_received=false
stable request quiescence is qualified
stable request quiescence satisfies the command profile snapshot threshold
stable request quiescence satisfies the command profile duration threshold
stable request quiescence signature_version is supported
lifecycle_evidence_stage=stable_request_quiescence_observed
waiting_reason=waiting_for_task_finished_event
command profile explicitly allows unknown reconciliation
no stronger accepted terminal result exists
terminal decision/send is not already observed or in flight
```

The busy-command diagnostic should include the current active identity and the
latest accepted `last_java_result` lifecycle evidence so that a blocked command can
be distinguished from missing Java publication, rejected Python acceptance, or
a failed reconciliation condition. Re-reading the same status snapshot must
not increment Java quiescence observations.

#### Wire, Local-Admission, And Audit Snapshot Separation

Snapshot consumers must not share one ambiguous command-state serialization:

```text
wire_snapshot:
  used by Java-facing handshake acknowledgements and status responses
  preserves the existing wire schema
  exposes only actual Java-originated result state
  excludes local_effective_result, synthetic UNKNOWN, shadow candidates,
  tombstones, and admission quarantine

local_admission_snapshot:
  used by Python router, UI, extension, and transport admission checks
  may expose active ownership, last_java_result, local_effective_result, and
  admission quarantine
  remains read-only and must not trigger release or quarantine clearing

audit_snapshot:
  used by bounded diagnostics and tests
  may additionally expose reconciliation candidates and tombstone metadata
  remains read-only and must not affect command behavior
```

The existing Java-facing `details.commands.last_result` wire field, when
present, must be derived from `last_java_result` only. A Python-local synthetic
UNKNOWN must never be echoed to Java through a handshake or status response.

#### Future Release Trigger And Read-Path Purity

If a future behavior patch is separately approved, reconciliation evaluation and
commit may start only at the ownership boundary immediately after
`accept_result` accepts exact-identity additive lifecycle evidence with:

```text
status=running
result_reason=stable_request_quiescence_observed
```

That evaluation should run in the same ownership transaction or a dedicated
ownership method that holds the same Python command lock. The following paths
must remain read-only and must not trigger active release:

```text
status snapshot read
UI refresh
busy-command rejection
new command submission attempt
router precheck
polling
```

The synthetic UNKNOWN is Python-local state. The implementation must not create
a fake Java envelope, recursively call `accept_result`, or route the synthetic
result through the Java transport path.

`RECONCILED_UNKNOWN` is an internal name for the future reconciliation action,
not a new v1 wire status. If separately approved, the synthetic terminal result
uses the existing `status=unknown` value. All reconciliation-specific fields
must remain inside the existing `CommandResultDTO.data` mapping; this patch must
not add a new top-level DTO or protocol field. The canonical local shape is:

```text
status=unknown
ok=false
error_code=null
message=Command terminal state could not be reconciled.
data.result_reason=terminal_reconciliation_gap
data.detail_reason=finish_callback_without_task_finished_event_after_stable_idle
data.reconciliation_action=RECONCILED_UNKNOWN
data.result_origin=python_stale_active_reconciliation
data.source_status=running
data.java_terminal_observed=false
data.gameplay_effect=UNVERIFIED
```

The comparison, synthetic result write, reconciliation tombstone write,
admission-quarantine write, and active-command release must occur as one
lock-protected compare-and-release operation inside Python ownership. The lock
must compare the full active identity, active token, latest accepted evidence
sequence, latest accepted nonterminal status, and reconciliation predicate. It
must store the synthetic UNKNOWN result, bounded tombstone, and admission
quarantine before clearing active ownership, so the post-lock snapshot cannot
show an idle active slot without both the UNKNOWN outcome and the gate that
justify it.

For that future operation, `no stronger accepted terminal result exists` means
that no stronger terminal result has been accepted before the Python ownership
lock commits the comparison and release. This is a commit-time fact; it is not
a claim that a delayed Java terminal result can never arrive later.

#### Future Python Reconciliation Commit Invariants

The separately approved release path must be exception-safe as well as
lock-protected. Before mutating ownership state, it should construct all
immutable replacement values needed for the commit. The preferred commit is one
immutable command-state aggregate swap:

```text
next_state = current_state.with_reconciled_unknown(...)

if current_state.active_command is not expected_active:
  return cas_failed

command_state = next_state
```

The state swap must publish active clear, local effective UNKNOWN, tombstone,
quarantine, and candidate reset together. A multi-store mutation followed by
rollback is a fallback only if the implementation can prove no post-mutation
assignment can fail or that rollback itself cannot fail.

A failed or stale compare-and-release attempt must leave all of these
unchanged:

```text
active command ownership
last_java_result
local_effective_result
reconciliation tombstones
admission quarantine
```

The successful post-lock state for this reconciliation is:

```text
active_request_id=null
last_java_result=<latest actual Java result, not synthetic>
local_effective_result.status=unknown
local_effective_result.data.result_reason=terminal_reconciliation_gap
local_effective_result.data.reconciliation_action=RECONCILED_UNKNOWN
local_effective_result.data.gameplay_effect=UNVERIFIED
reconciliation tombstone present for the released full identity
admission_quarantine.active=true
admission_quarantine.java_context_retirement_verified=false
```

Logging, callback publication, and user-facing response formatting should use an
immutable post-commit snapshot outside the ownership lock. They must not submit
a command, stop a task, or perform a second ownership mutation inline.

#### Bounded Reconciliation Tombstone And Late Result Policy

The first release implementation must keep reconciliation tombstones
Python-local, process-local, non-persistent, and bounded by both an explicit
maximum count and an explicit TTL. The implementation plan must name the two
finite configuration constants before code is approved. Expiration should
remove expired entries first and then evict the oldest `reconciled_at_ms` entry
when the count limit is exceeded. Tombstone eviction removes audit memory only;
it must not by itself clear admission quarantine or prove Java retirement.

Each tombstone key must contain the full released transport identity:

```text
session_id
connection_generation
request_id
command_message_id matching envelope correlation_id
```

The tombstone must also retain the accepted evidence sequence used by the CAS,
reconciliation time, synthetic UNKNOWN reason, profile, and bounded late-result
audit metadata. The evidence sequence is commit provenance; it is not required
from a later terminal envelope unless that envelope already carries the same
additive field.

A terminal result whose full identity still matches the current active command
must take the normal terminal acceptance path. Tombstone lookup is only for a
result that does not match any current active command. A single result must not
be processed both as the current result and as late-result audit evidence.

After `RECONCILED_UNKNOWN` is committed, an exact-identity later terminal may
be associated with the tombstone for audit only. A later `running`, `accepted`,
duplicate, or otherwise nonterminal result for the released identity must not
attach as terminal lineage; it is classified as
`late_nonterminal_after_reconciliation` and is reject/audit-only. A malformed
late result is also reject/audit-only when enough released identity remains to
correlate it safely. If any late result carries evidence-lineage data, a
mismatch blocks tombstone association.

Every post-reconciliation late-result path must leave behavior state unchanged.
It must not:

```text
replace last_java_result
replace local_effective_result=RECONCILED_UNKNOWN
clear or overwrite a current or newer active command
repeat active-command release
refresh or extend tombstone retention
clear admission quarantine or mark Java retirement verified
change gameplay_effect from UNVERIFIED
trigger retry, replay, StopCommand, cleanup, recovery, or a follow-up command
open new-command admission by itself
```

A late result with a stale session, stale generation, wrong request ID, wrong
correlation ID, or nonmatching evidence lineage must not attach to the
tombstone and must not mutate ownership state.

#### Post-Release Admission Quarantine

Python reconciliation retires Python stale active ownership only. It does not
retire Java `FabricChatClefCommandQueue.active`, clear the Java lifecycle
execution, reset the connection, or prove that Java can accept the next
command. Therefore the same commit that clears Python active ownership must
activate a Python-owned new-command admission quarantine for the released full
identity. `active_request_id=null` alone must not be interpreted as bridge
readiness.

The authoritative quarantine check must run at the shared Python transport
command-admission boundary before active ownership or an active token is
created and before any command envelope is sent. Router, UI, status snapshots,
and busy prechecks may report the same quarantine state, but they are
supplementary and must not be the only enforcement. A quarantine rejection must
be side-effect-free:

```text
no active command created
no envelope sent
no retry or replay
no StopCommand
no cleanup or follow-up command
```

New command admission remains blocked until authoritative evidence proves that
the prior Java command queue and lifecycle context have been retired. A
matching late terminal, a connection-generation replacement, or completion of
an explicit recovery/reset path may count as retirement evidence only when the
applicable source contract and tests prove that the signal establishes Java
context retirement. Otherwise it is supporting evidence only and the quarantine
remains active. Merely requesting recovery/reset is not sufficient; its
verified completion is required.

The first implementation may provide process-lifetime, in-memory quarantine
only. Generic disconnect/reconnect, connection generation change, ordinary
ownership clear, and server stop/start within the same Python process must not
clear that quarantine. Python process restart, tombstone expiration, or
tombstone eviction is not Java retirement evidence. Tombstone non-persistence
must not be used as a quarantine bypass.

The implementation plan must state whether admission quarantine is persisted or
reconstructed after process restart. If Python can restart while Java remains
alive and neither persisted quarantine nor authoritative reconnect-time
retirement proof exists, `runtime_ready` remains false and the release feature
must remain disabled outside controlled tests.

Python-local admission/audit snapshots and busy diagnostics should expose at
least:

```text
admission_quarantine.active
admission_quarantine.reason
admission_quarantine.released_identity
admission_quarantine.java_context_retirement_verified
admission_quarantine.retirement_evidence_kind
admission_quarantine.activated_at_ms
```

Java-facing handshake acknowledgements and status responses must use the
`wire_snapshot` contract above. They must exclude Python-local synthetic UNKNOWN,
reconciliation tombstones, admission quarantine, and shadow-candidate state.

Clearing the quarantine must not revise the synthetic UNKNOWN into success,
completion, failure, or verified gameplay effect. It only changes whether a new
command may be admitted.

Safe implementation order:

```text
1. Java publishes additive status=running lifecycle evidence.
2. Python receives it only through the existing accept_result identity path.
3. Python busy logs expose the latest accepted lifecycle evidence.
4. Python adds dry-run reconciliation evaluation and keeps release disabled.
5. A separately approved, default-disabled, feature-gated, deposit-only Python
   patch may add the exception-safe CAS commit: synthetic status=unknown
   result, bounded tombstone, admission quarantine, and Python active-command
   release.
6. New-command admission remains blocked until separately proven authoritative
   Java context-retirement evidence clears the quarantine.
```

That fifth step is still Python-local. It retires Python stale ownership only;
it does not retire Java `FabricChatClefCommandQueue.active`, clear the Java
lifecycle execution, reset the connection, or prove that Java can accept the
next command. New command admission after Python release remains blocked until
authoritative evidence proves the prior Java queue and lifecycle context have
retired.

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
  -> Python stores the accepted Java terminal in last_java_result
  -> Python updates local_effective_result through the normal terminal path
  -> Python clears active command for terminal status
```

Duplicate Java terminal sends must be ignored. Late TaskFinishedEvent signals
for a detached or already-cleared command must be logged as stale or late
observation, not used to mutate unrelated command state.

<!-- 20260814_kpopmodder: Documented Fabric ChatClef command lifecycle hazards and the implemented ownership guards. -->

## Command Lifecycle Hazard Guards

Status as of 2026-08-14: the Fabric Java bridge now has source-level guards for
the three hazards below. Runtime validation still requires the separate clean
Gradle build, copied jar, Minecraft launch, and log review flow defined in the
build verification runbook. This section does not approve Gradle build,
Minecraft launch, commit, push, or merge.

### Disconnect Must Not Clear Ownership Before Task State Is Resolved

Guarded risk:

```text
WebSocket disconnect or error
  -> Java active command context cleared
  -> lifecycle active execution cleared
  -> AltoClef UserTaskChain root task may still be running
  -> Baritone pathing or target pursuit may still continue
```

This can produce the diagnostic shape:

```text
commandContextAvailable=false
commandContextError=no_active_command
UserTaskChain still has the previous command root task
```

That state must be treated as a lifecycle ownership defect, not as proof that
the Minecraft task has safely stopped.

Implemented resolution:

```text
Disconnect observed
  -> enqueue an immutable client-tick-owned control event
  -> mark matching pending contexts detached and remove them
  -> keep the active detached context visible until client-tick handling
  -> cancel the bound root task on the Minecraft client tick
  -> observe the actual task stop or terminal condition
  -> clear command ownership only after the detach policy reaches its terminal point
```

While a detached or orphan root task may still exist, the bridge must not accept
or dispatch a new command as if the previous command had no remaining runtime
state.

### Terminal Result Send Must Complete Before Clear

Guarded risk:

```text
terminal condition classified
  -> terminalSent=true
  -> Java active command context cleared
  -> result envelope send attempted
  -> send may fail because socket is null, generation is stale, or async send fails
```

This can produce split-brain ownership:

```text
Java believes the command was terminal and cleared
Python never receives the terminal command_result
Python still considers the request running
```

Implemented terminal send ordering:

```text
TERMINAL_READY
  -> command_result payload built
  -> SEND_IN_FLIGHT
  -> explicit send outcome observed
  -> SEND_SUCCEEDED only after an explicit send outcome reports success
  -> Java records terminal_result_sent
  -> Java clears command ownership
```

Failures such as stale completion, null socket, generation mismatch, synchronous
send failure, or asynchronous send failure must not be logged as
`terminal_result_sent`. The source-level guard records an explicit send failure
outcome and keeps command ownership uncleared when the terminal send fails.

### WebSocket Callback Must Not Read Live Engine State

Guarded risk:

```text
WebSocket callback thread
  -> disconnect or error handling
  -> command queue mutation
  -> live task ownership snapshot
  -> AltoClef / UserTaskChain / TaskRunner / Task state traversal
  -> diagnostic payload formatting and synchronous log output
```

This violates the intended thread boundary. Live ChatClef, AltoClef, TaskRunner,
Task, Baritone, Minecraft client, input, path, goal, and screen state must be
read on the Minecraft client tick unless an immutable snapshot has already been
published by the correct owner.

Implemented thread-affinity direction:

```text
WebSocket callback:
    decode
    validate
    create immutable command/control event
    enqueue only

Command queue synchronized region:
    mutate queue, generation, and immutable command state only
    do not traverse live engine state
    do not emit synchronous logs while holding the queue monitor

Fabric END_CLIENT_TICK:
    dispatch commands
    process disconnect control events
    observe live AltoClef / TaskRunner / Task state
    build task snapshots
    emit diagnostics outside queue locks
```

Any fix for disconnect handling, terminal send ordering, or lifecycle
diagnostics must preserve this thread-affinity boundary.

## Thread Affinity Matrix

```text
Boundary                         Thread owner                 Allowed work
Python UI callback               UI/framework thread          build request, call extension
Python transport send            asyncio server loop          send envelope, update transport state
Python ownership validation      Python server/loop context   validate, store, clear accepted terminal
Python reconciliation decision   Python command lock          dry-run; future gated deposit-only CAS
Python late-result audit         Python command lock          exact bounded audit only; no behavior mutation
Python admission gate            Python command lock          block until authoritative Java retirement
Java WebSocket callback          bridge WebSocket thread      decode, validate, enqueue
Fabric END_CLIENT_TICK           Minecraft client thread      dispatch command, observe client state
ChatClef Task tick               Minecraft client thread      existing Task lifecycle behavior
Baritone worker                  Baritone worker thread       Baritone path calculation only
Java nonterminal result sender   bridge/client boundary       send immutable status=running evidence
Java terminal result outbox      bridge/client boundary       serialize and send terminal result
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
accepted last_java_result evidence Python FabricChatClefConnectionOwnership
local_effective_result             Python ownership command state
reconciliation policy/release     Python ownership command lock
reconciliation tombstone          Python ownership command lock
late-result audit metadata        Python ownership command lock
post-release admission quarantine Python ownership command lock
Java command queue                Java FabricChatClefCommandQueue
Java active command lifecycle     Java command lifecycle coordinator
bound root task identity          Java command execution/lifecycle state
stable quiescence evidence window Java client-tick lifecycle diagnostics
TaskRunner lifecycle              ChatClef / AltoClef engine
child task selection              ChatClef / AltoClef engine
input override acquisition        existing owning Task or input chain
input override release            same owner that acquired it
Baritone goal/path ownership      existing Baritone or Task owner
diagnostic emission budget        diagnostics layer only
```

Diagnostics may own counters, fingerprints, quiescence windows, suppression
counts, and emission budgets only when those values affect diagnostic evidence
qualification or emission. During the diagnostics-only stage, they must not
affect task selection, retry, replay, deadline, cancellation, terminal
classification, engine state, or active ownership. A separately approved
Python reconciliation policy may later consume immutable accepted evidence, but
only the Python ownership lock may write a synthetic result or release the
active command.

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
nonterminal lifecycle evidence publication or acceptance gap
stale Python active ownership or terminal reconciliation gap
command lifecycle terminal mismatch
ChatClef parent-child Task loop
Baritone path cache or target mismatch
container GUI failure
Carry On optional state observation failure
```

A current log must prove the active class of failure. A past Mixin crash does
not prove the current task loop is a build problem, and a successful build does
not prove command lifecycle ownership is correct.

## 2026-08-22 Sync-Finish Idle-Root Lifecycle Contract

The Fabric Java bridge now separates an observed post-dispatch root from a
command-owned bound root. A root is bound to the command only when the
before/after ownership evidence proves a command-owned assignment transition.

For the deposit sync-finish idle-root incident, the bridge treats the following
state as not command-owned:

```text
same raw IdleTask object before and after dispatch
same user_task_root_identity
same user_task_root_assignment_id
same user_task_root_generation
user_task_running_idle true before and after
next_task_idle_flag false before and after
finish callback first observed before dispatch returned
no matching TaskFinishedEvent attached
stable END_CLIENT_TICK evidence qualified
```

That state emits a terminal `UNKNOWN`, not `COMPLETED`. The Java terminal outbox
still owns send-before-clear ordering, so active ownership is cleared only after
the terminal send completes through the existing path. A `TaskFinishedEvent`
arriving while this pre-existing idle-root state is active is audit-only and is
not attached to the execution.
