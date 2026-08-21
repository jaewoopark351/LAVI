<!-- 20260815_kpopmodder: Documented the Python-only plan for ChatClef command replies, Korean mining phrases, and inventory preflight cleanup. -->
<!-- 20260819_kpopmodder: Recorded the implemented single-pass canonical submission and reconciliation boundary. -->
<!-- 20260819_kpopmodder: Bound Korean response and cleanup planning to reviewed archive e08af639 and split inventory cleanup into its own fail-closed contract. -->
<!-- 20260820_kpopmodder: Linked command orchestration to the full Korean command registry lifecycle axes. -->
<!-- 20260820_kpopmodder: Documented stale-active-command reconciliation for callback-without-TaskFinishedEvent false-busy states. -->
<!-- 20260820_kpopmodder: Added implementation-plan boundaries for monotonic lifecycle evidence, release-trigger purity, and restart-safe admission quarantine. -->
<!-- 20260821_kpopmodder: Added conditional-pass v2 guardrails for wire/local snapshots, source-contract proof, runtime readiness gating, and immutable state-swap CAS. -->

# ChatClef Python Command Orchestration Plan

Date: 2026-08-15

This document records the design and reviewed-baseline status for the requested
Fabric ChatClef Python orchestration behaviors:

```text
1. Emit a short LAVI-side reply when a Minecraft command is routed.
2. Treat Korean mining phrases such as "철 10개 캐줘" as existing GET_ITEM
   commands.
3. Handle inventory-full cleanup from Python only, using existing ChatClef
   commands, without adding Java behavior.
```

It is documentation only. It does not approve Python behavior changes, Java
changes, wire protocol changes, DTO changes, build execution, Minecraft launch,
runtime reproduction, commit, push, broad refactoring, file moves, or automatic
replay logic.

## Document Authority And Reviewed Baseline

Reviewed archive baseline:

```text
e08af63948a3fa4675c70279db59c2a70b00a332
```

This document owns Python user replies, lifecycle wording, operation context,
single-pass submission, and the high-level command-orchestration flow.

The full 20-command Korean registry, per-command resolver domains, lifecycle
kinds, safety tiers, confirmation modes, allowed input sources, and public
enablement axes are owned by:

```text
plugins/Minecraft/docs/chatclef-python-korean-command-registry-plan.md
```

Implemented at the reviewed archive baseline:

```text
shared Korean acquisition matcher
철 10개 캐줘 / 캐오기 / 캐와줘 family
prefixless GET compilation
single-pass submission and reconciliation latch
```

Planned, not implemented at that baseline:

```text
deterministic Korean response renderer
normal LAVI output listener dispatch
accepted-result event publisher
command orchestrator
inventory snapshot provider
automatic cleanup
```

Current working-tree implementation after the 2026-08-19 follow-up:

```text
deterministic Korean route response renderer
canonical Korean display-name lookup for initial GET responses
Python-only targeted cleanup policy helper
post-cleanup primary admission gate helper
```

Still planned after that follow-up:

```text
normal LAVI output listener dispatch beyond router response_text
accepted-result event publisher
long-lived command orchestrator
reliable inventory snapshot provider
automatic cleanup execution
```

Inventory cleanup details are intentionally not owned here. The fail-closed
cleanup evidence, protected-item policy, postcondition, and no-replay contract
are owned by:

```text
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
```

## 2026-08-21 Conditional-Pass V2 Guardrails

The stale active-command reconciliation design is conditionally approved only
for a Python-local, default-disabled implementation. The current production
state is still:

```text
default-OFF shadow classifier implementation: allowed
test-injected guarded mutation implementation: allowed
production config True -> mutation: forbidden
production ON readiness: blocked
Java, DTO, and v1 protocol mutation: forbidden
Korean compiler change for "다이아 곡괭이 만들어줘": not needed
```

The immediate user-visible failure for "다이아 곡괭이 만들어줘" is classified as
stale active-command admission blocking, not Korean compiler failure and not
Minecraft crafting execution failure. The uploaded baseline already validates
that phrase as:

```text
input=다이아 곡괭이 만들어줘
status=validated
command=get diamond_pickaxe 1
intent=get_item
```

Do not add a dedicated craft compiler path for this stale-active patch. Add
only regression coverage proving that the phrase translates before precheck,
does not submit while busy or quarantined, and submits `get diamond_pickaxe 1`
when the bridge is idle.

### Wire/Local Snapshot Separation

The current status snapshot is used by more than Python UI. It may be embedded
in `handshake_ack` and `status_snapshot` WebSocket payloads sent to Java.
Therefore Python-local reconciliation state must not be written into the
shared wire snapshot.

The next implementation must split command state views explicitly:

```text
wire_snapshot:
  Java-facing status payload
  contains only actual Java-originated command result state
  excludes synthetic UNKNOWN
  excludes tombstones
  excludes admission quarantine

local_admission_snapshot:
  Python router/UI/admission view
  may expose local_effective_result
  may expose process-lifetime quarantine
  never sent to Java

audit_snapshot:
  bounded Python diagnostics view
  may include reconciliation decisions, tombstones, and candidate state
  never sent to Java
```

The command result model must distinguish:

```text
last_java_result:
  the latest actual Java command_result accepted by Python
  eligible for wire status exposure

local_effective_result:
  Python's local lifecycle interpretation
  may be synthetic status=unknown with RECONCILED_UNKNOWN
  forbidden from handshake/status WebSocket payloads sent to Java
```

Any older wording in this document that refers to storing synthetic UNKNOWN in
`details.commands.last_result` or exposing quarantine through a generic status
snapshot is superseded by this separation. Synthetic UNKNOWN and quarantine may
appear only in Python-local admission or audit snapshots.

Required wire/local leakage tests:

```text
test_wire_status_snapshot_excludes_python_reconciliation_state
test_handshake_ack_does_not_echo_synthetic_unknown
test_status_response_does_not_include_quarantine
test_local_status_snapshot_exposes_effective_unknown_and_quarantine
```

### Evidence Source Contract Blocker

The production mutation path is blocked until the exact Java producer contract
is fixed to the same source/build identity as the logs under review. Before any
non-test mutation can be enabled, capture and preserve:

```text
sequence=1 raw command_result envelope
sequence=2 raw command_result envelope
Java source commit that produced both envelopes
runtime JAR/source/build identity that produced the log evidence
```

If the current repository baseline does not contain the producer for
`evidence_sequence`, `lifecycle_evidence.version`,
`lifecycle_evidence.stage`, `stable_request_quiescence`,
`same_session_generation`, and `request_root_reappeared`, then only these work
items are allowed:

```text
strict parser/classifier
feature-OFF shadow audit
synthetic fixture-based unit tests
test-injected guarded mutation
```

Production mutation remains blocked until the source contract and runtime
identity are reconciled.

### Identity, Fingerprint, And Cursor Split

Do not treat `evidence_sequence` as part of the stable lifecycle fingerprint.
The model must be split into three concepts:

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

The first behavior patch should require a narrow progression:

```text
first accepted sequence == 1
second accepted sequence == 2
same stable evidence fingerprint
same expected active object
different nonblank envelope.message_id
no intervening contradictory or nonqualifying running evidence
```

Raw envelope typing must be checked before a result can qualify:

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

Do not accept string booleans, numeric booleans, float sequences, string
sequences, or `bool` as an integer evidence sequence.

### Effective Feature Gate

The feature flag name for the requested patch is:

```text
reconcile_stale_deposit_to_unknown_enabled
```

The default is `False`. However, default OFF is not enough by itself. The
implementation must compute:

```text
requested_enabled = config.reconcile_stale_deposit_to_unknown_enabled
runtime_ready = authoritative Java retirement acknowledgement available
                or restart-safe quarantine capability available
effective_enabled = requested_enabled and runtime_ready
```

Current production composition must keep:

```text
runtime_ready=false
effective_enabled=false
blocked_reason=java_retirement_evidence_unavailable
```

If config requests the feature while `runtime_ready=false`, the system must
remain in shadow audit mode and must not release active ownership. The risky
flag should use a strict boolean parser for this setting only. Do not reuse a
permissive truthiness parser that accepts arbitrary non-empty objects as true.

### Reconciliation Collaborators And Atomic State Swap

Parsing, classification, policy, tombstone retention, quarantine policy, and
audit outcome construction should live in focused reconciliation collaborators.
The command ownership/state owner should perform only exact identity validation
and one lock-protected atomic compare-and-commit.

Recommended ownership shape:

```text
ActiveCommandReconciliationCoordinator:
  parses raw payload evidence
  classifies deposit-only stable evidence
  prepares immutable decision/outcome

CommandLifecycleStateStore:
  owns one immutable command-state aggregate
  compares active object identity
  swaps the entire state reference on commit
```

Rollback after partially mutating multiple stores is discouraged. Instead,
prepare an immutable `next_state` containing:

```text
active_command=None
last_java_result unchanged or updated only from actual Java result
local_effective_result=synthetic UNKNOWN
bounded tombstone inserted
process-lifetime admission quarantine active
candidate sequence state cleared
```

Then, while holding the command lock:

```text
if current_state.active_command is not expected_active:
  return cas_failed

command_state = next_state
```

No post-release state may expose an idle active slot without the corresponding
local UNKNOWN and quarantine in the Python-local admission snapshot.

### Quarantine Lifetime

The first patch may implement process-lifetime quarantine only. It must not
claim restart fail-closed behavior.

```text
process-lifetime quarantine: allowed
restart-persistent quarantine: not implemented
restart fail-closed: not satisfied
production ON readiness: blocked
```

The following are not Java retirement evidence and must not clear quarantine:

```text
WebSocket disconnect
new handshake
connection generation change
server stop/start
status refresh
tombstone expiry
tombstone eviction
```

Current patch scope must not add a dormant quarantine journal placeholder.
Durable quarantine journaling is a separate persistence task that needs its own
crash-consistency, startup-recovery, corruption-handling, and approval plan.

### Additional Required Test Names

Add these tests to the implementation checklist before approval:

```text
test_sequence_two_without_sequence_one_does_not_release
test_duplicate_envelope_message_id_does_not_advance_evidence
test_duplicate_sequence_does_not_advance_evidence
test_out_of_order_sequence_does_not_release
test_bool_evidence_sequence_is_rejected
test_float_or_string_evidence_sequence_is_rejected
test_raw_top_level_ok_string_does_not_qualify
test_unknown_evidence_version_does_not_release
test_unknown_evidence_stage_does_not_release
test_fingerprint_change_between_sequence_one_and_two_does_not_release
test_intervening_nonqualifying_running_result_resets_candidate
test_precheck_accept_then_transport_quarantine_blocks
test_disconnect_does_not_clear_quarantine
test_new_handshake_does_not_clear_quarantine
test_generation_replacement_does_not_clear_quarantine
test_server_stop_start_does_not_clear_process_lifetime_quarantine
test_wire_status_excludes_local_effective_unknown
test_wire_status_excludes_quarantine
test_handshake_ack_does_not_echo_reconciliation_state
test_local_status_includes_last_java_result_and_effective_result_separately
test_invalid_reconciliation_flag_value_is_rejected
test_requested_true_without_runtime_readiness_does_not_mutate
test_requested_true_with_test_readiness_allows_guarded_mutation
test_feature_off_observation_does_not_seed_later_enabled_instance
test_late_running_matching_tombstone_is_audit_only
test_tombstone_eviction_does_not_clear_quarantine
test_quarantined_diamond_pickaxe_make_phrase_translates_but_does_not_submit
```

## Command Registry Lifecycle Boundary

Do not treat a Korean parser/compiler row as end-to-end command readiness. The
orchestrator may only publicly enable a command family when the command registry
has separately classified these axes:

```text
SOURCE_REGISTERED
KOREAN_PARSE_COMPILE_READY
PYTHON_ADMISSION_READY
BRIDGE_LIFECYCLE_READY
GAMEPLAY_EFFECT_VERIFIABLE
PUBLIC_KOREAN_ENABLED
```

This distinction matters for persistent or control commands. For example,
`idle`, `follow`, and `stop` can have Korean parser/compiler support while still
requiring source-backed lifecycle or cancellation proof before broad public
enablement. The orchestration state machine must preserve exactly-once
submission and no-replay behavior even when a command is parser-ready but not
bridge-lifecycle-ready.

## Scope

Current implementation scope remains Fabric ChatClef only:

```text
LAVI
  -> Fabric adapter
    -> Fabric ChatClef bridge
      -> ChatClef / AltoClef
```

Do not add Forge, MineMind, shared backend code, shared Java runtime, shared
session registries, shared reconnect managers, or shared GUI components for
this work.

The plan applies to the Python-owned Fabric ChatClef path:

```text
plugins/Minecraft/fabric/chatclef/**
llm_core/**
app_core/composition_core/**
```

The current v1 wire DTOs and Java bridge remain unchanged:

```text
plugins/Minecraft/common/dto/**
plugins/Minecraft/common/protocol/**
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/**
```

## Non-Goals

Do not implement any of these as part of the first documentation-approved
phase:

```text
Java inventory cleanup behavior
ChatClef / AltoClef Task changes
Baritone path, goal, or input cleanup changes
CommandResultDTO shape changes
BridgeEnvelopeDTO shape changes
new wire message types
automatic retry after timeout or unknown status
automatic replay after a primary command has been accepted
inventory-full inference from logs or long-running tasks
unconditional deposit
TTS calls directly from Minecraft transport code
LLM generation of raw ChatClef DSL
```

## Hard Prohibitions

This work MUST NOT:

```text
modify any file under
  plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/**
modify plugins/Minecraft/common/dto/**
modify plugins/Minecraft/common/protocol/**
modify plugins/Minecraft/common/schema/**
add, remove, rename, or reinterpret any v1 wire message or payload field
add fields to CommandRequestDTO, CommandResultDTO, StatusSnapshotDTO,
  or BridgeEnvelopeDTO
add a new Java craft command or change GetCommand semantics
change DepositCommand, EquipCommand, GiveCommand, or ChatClef command grammar
change ChatClef, AltoClef, Baritone task selection, retry, timeout,
  completion, container transfer, or goal behavior
implement Korean parsing, aliases, responses, junk policy, or orchestration
  in Java
infer inventory-full from logs, task duration, path stalls, timeout,
  deadline, or UNKNOWN results
infer gameplay effect from ACCEPTED, RUNNING, terminal COMPLETED,
  active-request clearing, or connection state alone
use argument-less deposit as automatic junk cleanup
generate placeholder cleanup quantities
automatically retry, replay, rerun, or resubmit cleanup or primary commands
submit a follow-up command inline from a WebSocket callback
let an LLM generate raw ChatClef DSL or decide workflow state transitions
emit an @ prefix from the Python natural-language compiler
put contextual pseudo-targets such as 잡템, 갑옷, player names, quantities,
  or command modes into korean_item_aliases.json
introduce Forge MineMind fallback or shared Fabric/Forge orchestration
```

If an implementation appears to require any prohibited change, stop the phase
and open a separate evidence-backed design review. Do not expand this scope
implicitly.

## Current Code Observations

The current code largely matches the proposed boundary, but some required
extension points do not exist yet.

### LAVI Reply Path

`MinecraftChatClefInputRouter` already creates a technical routed response:

```text
[Minecraft] command sent: get gold_ingot 8
```

The response is returned through `MinecraftChatClefInputRouteDecision`, but
`LLM.predict_wrapper()` currently yields the routed response and returns. It
does not route the response through the normal LAVI output path:

```text
LLM.send_output()
  -> Translate.receive_input()
  -> TTS.receive_input()
```

Therefore direct chat UI output is possible, but the existing routed response
is not guaranteed to reach Translate/TTS listeners.

The reply feature should be implemented as a general external-response
dispatch API on the LLM side, not as a Minecraft-specific branch inside TTS or
transport code.

### Historical Korean Mining Phrase Gap Before cfc170a

Before the reviewed Korean GET fixes, the Korean command path had these gaps.
Do not read this historical section as the current behavior at reviewed archive
baseline `e08af63948a3fa4675c70279db59c2a70b00a332`:

```text
Input: 철 10개 캐줘

Intent gate:
  "캐줘", "캐와", "캐오기", and "채굴해줘" are not trigger terms.

Rule parser:
  _GET_VERB_RE does not include mining/acquisition verbs such as "캐줘" or
  "채굴해줘".

Item aliases:
  korean_item_aliases.json contains "철괴" and "철 주괴", but not the standalone
  alias "철".
```

`korean_material_aliases.json` maps standalone "철" to the material `iron`,
which supports equipment composition such as "철 곡괭이". That material alias
does not make standalone "철" resolve to `iron_ingot`.

The safe translation target for standalone "철" is currently `iron_ingot`,
because the public target catalog exposes `iron_ingot` while `raw_iron` or
`iron_ore` are not guaranteed public ChatClef targets.

### Natural-Language Single-Pass Translation

At reviewed archive baseline `e08af639`, the Python router already preserves the
single-pass translation boundary:

```text
MinecraftChatClefTranslationBoundary.translate_once(text)
  -> one validated translation
MinecraftChatClefSubmissionBoundary.submit_once(request, validated_translation)
  -> submit_translated_command(request, validated_translation)
```

Future response rendering, orchestration, cleanup preflight, or event publishing
must preserve this invariant. Do not re-run natural-language translation in the
submission layer, and do not let cleanup planning reinterpret the raw Korean
input after a validated primary command has already been produced.

### Command Submission Flow

The current immediate submission flow is:

```text
user text or GUI command
  -> CommandRequestDTO
  -> MinecraftFabricChatClefExtension.handle_command()
  -> FabricChatClefAdapter.submit_command()
  -> FabricChatClefWebSocketServer.submit_command()
  -> FabricChatClefConnectionOwnership.begin_command()
  -> BridgeEnvelopeDTO(message_type=command_request)
  -> asyncio.run_coroutine_threadsafe(_send_envelope)
  -> immediate CommandResultDTO(status=accepted) on send success
```

The Python transport allows only one active command. If an active request
exists, a second command is rejected before it reaches Java.

### Command Result Flow

The current Java-to-Python result flow is:

```text
Java command_result envelope
  -> FabricChatClefWebSocketServer._handle_command_result()
  -> CommandResultDTO.from_mapping(envelope.payload)
  -> FabricChatClefConnectionOwnership.accept_result()
  -> last_result snapshot update
  -> active command cleared for terminal statuses
  -> diagnostics log
```

`accept_result()` validates:

```text
active websocket
active command existence
connection generation
session_id
request_id
correlation_id
```

Only accepted results should be observed by future orchestration. Rejected
stale results, wrong-session results, wrong-generation results, wrong-request
results, and wrong-correlation results must not advance a workflow.

There is currently no Python event publisher that lets a higher-level
orchestrator subscribe to accepted command results or disconnect events.

### Stale Active Command Reconciliation Gap

The Python command lifecycle must distinguish command success from active-state
release. A request can become stale when Java observes a command callback but
the Python bridge never receives a matching `TaskFinishedEvent`.

The 2026-08-20 live log case is classified as:

```text
stale active command
+ terminal reconciliation gap
+ gameplay outcome UNKNOWN
```

The observed shape was:

```text
request_id=lavi-input-ko-afe485ac0e334f4cbd2717f1d889af11
source=lavi_chat_mic_router
normalized_command=@deposit diamond 2
finish_callback_received=true
task_finished_event_received=false
waiting_reason=waiting_for_task_finished_event
current/bound root task=adris.altoclef.tasks.movement.IdleTask
StopCommand observed=false
USER_TASK_CHAIN_CANCEL_REQUESTED observed=false
gameplay effect verified=false
```

A later 2026-08-20 live replay confirmed the same boundary with a newer
request:

```text
request_id=lavi-input-ko-8532b405be0d4b0e844e89dfe6d822bf
correlation_id=lavi-ba66cd0a1ad34e66b76b16b5fe18dab4
session_id=fabric-chatclef-885d3046163e4588be274270eb55935c
connection_generation=1
request_command=deposit diamond 2

Java latest.log:
  finish_callback_received=true
  dispatch_returned=true
  task_finished_event_received=false
  waiting_reason=waiting_for_task_finished_event
  current/bound/terminal task=adris.altoclef.tasks.movement.IdleTask
  terminal_result_sent observed=false

Python LAVI log:
  accepted command_result count=1
  last_result.status=running
  last_result.result_reason=dispatch_started
  last_result.finish_callback_received=false
  later route blocked with minecraft_command_busy
```

This proves that Java-local lifecycle logs alone are not sufficient input for
Python reconciliation. Python needs the latest nonterminal lifecycle evidence
to arrive through a validated bridge result or another explicitly documented
Python-readable evidence channel before it can even dry-run the release rule
accurately.

A diagnostics-only positive live reproduction then proved the full dry-run
predicate:

```text
request_id=lavi-input-ko-a110c614c1c347dda0035f6d98cfa519
session_id=fabric-chatclef-ebcd88b36d004b1bb19ae020c7b2adf4
connection_generation=1
command_message_id/correlation_id=lavi-3e702bbd25674b548505a8a722557a98
command=deposit diamond 2

identity_quality=EXACT
command_profile=deposit
dispatch_returned=true
finish_callback_received=true
task_finished_event_received=false
lifecycle_evidence_stage=stable_request_quiescence_observed
waiting_reason=waiting_for_task_finished_event
stable_request_quiescence.satisfied=true
stable_request_quiescence.consecutive_neutral_snapshots=12
stable_request_quiescence.neutral_duration_ms=543
latest accepted status=running
latest accepted result_reason=stable_request_quiescence_observed
stronger_terminal_result_present=false
would_reconcile_to_unknown=true
active_release_performed=false
```

This is enough evidence for a separately approved, deposit-only, feature-gated
Python compare-and-release implementation. It is not evidence of gameplay
success and not evidence that Java is ready for another command.

This must not be classified as `COMPLETED`, `SUCCESS`, `FAILED`, or
`CANCELLED`. The safe terminal classification is:

```text
status=unknown
ok=false
result_reason=terminal_reconciliation_gap
detail_reason=finish_callback_without_task_finished_event_after_stable_idle
reconciliation_action=RECONCILED_UNKNOWN
gameplay_effect=UNVERIFIED
```

`RECONCILED_UNKNOWN` is a Python-local reconciliation action or classification.
It is not a new v1 wire status, not a new Java `CommandResultStatus`, and not a
Java terminal result. The DTO keeps the existing `status=unknown` value.

Active command release and gameplay success are separate facts:

```text
active_request_id cleared
  != command completed
  != deposit succeeded
  != inventory effect confirmed
```

The Python lifecycle reconciler may release the active command only after all
identity, command-profile, and request-quiescence requirements are satisfied.
Identity quality is explicit:

```text
EXACT:
  expected request_id, session_id, and connection_generation are present and
  match
  expected correlation_id matches when the submission carried one
  expected invocation_id or lifecycle generation matches when present

PARTIAL:
  at least one expected identity field is missing, but no explicit mismatch is
  observed

MISMATCH:
  at least one expected identity field is present and does not match
```

Automatic UNKNOWN release is allowed only for `identity_quality=EXACT`.
`PARTIAL` and `MISMATCH` must remain `RECONCILIATION_BLOCKED`; they must not
release active ownership automatically. `normalized_command` may be logged as a
diagnostic hint, but it must not be used as an identity key because equivalent
commands can repeat.

Root ownership observations must be separated:

```text
initial_request_owned_root:
  the first root task proven to belong to this request

bound_root_recorded_for_request:
  the bridge lifecycle binding recorded for this request

current_runtime_root:
  the fresh runtime root observed during reconciliation
```

The request-owned root observation state is:

```text
OBSERVED_AND_GONE:
  a request-owned root was observed and later disappeared or changed to an
  approved neutral root

NEVER_OBSERVED:
  no request-owned root was ever observed

STILL_PRESENT:
  a request-owned root is still present

UNKNOWN:
  the root evidence is missing, stale, or ambiguous
```

Default automatic release is permitted only for `OBSERVED_AND_GONE`. `STILL_PRESENT`
and `UNKNOWN` block release. `NEVER_OBSERVED` also blocks release for ordinary
task-producing commands unless the command registry explicitly defines a
callback-without-bound-task reconciliation profile.

Neutral-root evidence is command-specific. `IdleTask` by itself is not enough,
a timeout by itself is not enough, and a finish callback by itself is not
enough. The decision requires exact request identity plus a command profile that
allows the observed root to count as request quiescence.

The command registry must expose reconciliation fields before public use of
this release path:

```text
reconciliation_profile
expected_terminal_evidence
task_binding_expected
neutral_root_classes
neutral_root_is_release_eligible
persistent_command
auto_reconcile_to_unknown_allowed
```

Example profiles:

```text
deposit:
  lifecycle_kind=FINITE_TASK
  task_binding_expected=true
  neutral_root_classes=[adris.altoclef.tasks.movement.IdleTask]
  auto_reconcile_to_unknown_allowed=true

idle:
  lifecycle_kind=PERSISTENT_TASK
  neutral_root_classes=[]
  auto_reconcile_to_unknown_allowed=false
```

Quiescence evidence may authorize only active-ownership release to UNKNOWN. It
must never contribute positive evidence toward `COMPLETED`, `SUCCESS`, runtime
completion, or gameplay-effect verification.

Request quiescence requires all of:

```text
elapsed_since_callback >= configured_grace_ms
consecutive_neutral_snapshot_count >= configured_min_neutral_snapshots
neutral_observation_duration_ms >= configured_min_neutral_duration_ms
every snapshot age <= configured_max_snapshot_age_ms
all snapshots use the same session, connection generation, and runtime epoch
request-owned root did not reappear
```

The policy metadata must be logged:

```text
reconciliation_policy_version
grace_source
configured_grace_ms
configured_min_neutral_snapshots
configured_min_neutral_duration_ms
configured_max_snapshot_age_ms
```

If these values have not been evidence-backed or configured, automatic
reconciliation release remains disabled.

Terminal precedence is:

```text
1. matching explicit terminal completed/failed/cancelled
   -> existing terminal path

2. connection lost, session replaced, or generation replaced
   -> ownership-loss path
   -> old active request released as ABORTED or UNKNOWN_CONNECTION_LOST
   -> old request must not carry into the new session

3. callback + missing TaskFinishedEvent + stable request quiescence
   -> Python-local RECONCILED_UNKNOWN action
   -> synthetic status=unknown, ok=false result
   -> Python active ownership release only

4. insufficient identity or quiescence evidence
   -> RECONCILIATION_BLOCKED
   -> automatic release forbidden
```

Recommended state transition:

```text
transport ownership lifecycle:
ACTIVE
  -> CALLBACK_GRACE
  -> RECONCILIATION_REQUIRED
  -> RECONCILED_UNKNOWN_TOMBSTONE
  -> RELEASED

operation orchestrator lifecycle:
WAITING_CLEANUP_TERMINAL
  -> TERMINATED_UNKNOWN_BEFORE_PRIMARY
  -> primary submit count = 0

WAITING_PRIMARY_TERMINAL
  -> TERMINATED_UNKNOWN
  -> replay count = 0
```

`TERMINATED_UNKNOWN` is terminal for the Python operation:
`operation_active=false`, `python_active_request_id=null`, and
`automatic_follow_up=false`. Do not collapse it into `FAILED`; failure implies
evidence that the command failed, while this path means the lifecycle result is
unresolved.

Python operation closure is not the same thing as bridge command admission.
After a Python-local UNKNOWN release, `new_command_admission_allowed` remains
false until authoritative evidence proves that the prior Java command queue and
lifecycle context are retired. A matching late terminal, connection-generation
replacement, or recovery/reset completion may be used as retirement evidence
only when the implementation invariant and tests prove that the signal occurs
after Java context retirement. Otherwise it is supporting evidence only and the
admission quarantine remains active.

The reconciler must keep this fail-closed:

```text
retry_count=0
replay_count=0
stop_submit_count=0
automatic_follow_up=blocked
new_command_admission_allowed=false until Java retirement is verified
```

The active release must be an atomic compare-and-set against the expected
ownership token:

```text
release_active_if_matches(expected_active_token, reconciled_outcome)

expected_active_token:
  request_id
  session_id
  connection_generation
  command_message_id/correlation_id when expected
  invocation_id or lifecycle generation when expected

expected_latest_evidence:
  accepted_evidence_sequence
  last_result.status=running
  last_result.result_reason=stable_request_quiescence_observed
  dispatch_returned=true
  lifecycle_evidence_stage=stable_request_quiescence_observed
  waiting_reason=waiting_for_task_finished_event
  terminal_decision_observed=false
  terminal_result_send_started=false
  terminal_result_sent=false
```

Actual reconciliation evaluation and commit may start only immediately after
`accept_result` accepts exact-identity additive lifecycle evidence whose
`status=running` and `result_reason=stable_request_quiescence_observed`. It
must run in the same ownership transaction or in a dedicated ownership method
that holds the same Python command lock.

These paths are read-only for reconciliation and must never create a synthetic
UNKNOWN or release active ownership:

```text
status snapshot read
UI refresh
busy-command rejection
new command submission attempt
router precheck
polling
```

The synthetic UNKNOWN is Python-local state. Do not create a fake Java envelope,
do not recursively call `accept_result()`, and do not send the synthetic result
through the Java transport path.

Inside the ownership lock, the implementation must verify that the active owner
still matches the expected token, verify that no stronger matching terminal
evidence was accepted first, recheck the latest accepted evidence sequence,
recheck the latest accepted nonterminal status and reconciliation predicate,
write the synthetic UNKNOWN result, write the reconciliation tombstone, activate
admission quarantine, and clear active ownership only if the same token is still
current. If the token or evidence changed, record:

```text
active_release_performed=false
reason=active_owner_or_evidence_changed_before_release
```

The synthetic result must be prepared before the active owner is cleared so
that the post-lock Python-local admission snapshot is internally consistent.
The successful local post-lock state is:

```text
active_request_id=null
last_java_result=<latest actual Java result, not synthetic>
local_effective_result.status=unknown
local_effective_result.ok=false
local_effective_result.result_reason=terminal_reconciliation_gap
local_effective_result.data.reconciliation_action=RECONCILED_UNKNOWN
local_effective_result.data.gameplay_effect=UNVERIFIED
reconciliation_tombstone_present=true
admission_quarantine.active=true
admission_quarantine.java_context_retirement_verified=false
```

The commit must be exception-safe, not merely lock-protected. Before mutating
ownership state, construct all replacement values needed for the commit. The
implementation must then use one of these equivalent safety strategies:

```text
replace one immutable ownership-state object in one assignment
or
perform only in-lock assignments that cannot raise after mutation starts
or
restore every prior value before releasing the lock if any commit step fails
```

A failed or stale compare-and-release attempt must leave all of these
unchanged:

```text
active command ownership
last_result
reconciliation tombstones
admission quarantine
```

Logging, callback publication, and user-facing response formatting should use
an immutable post-commit snapshot outside the ownership lock. They must not
submit a command, stop a task, or perform a second ownership mutation inline.

Do not automatically send `stop`, replay the same command, resend `deposit`,
continue a cleanup workflow, or advance a primary command from this state. A
late matching event after UNKNOWN release should be recorded as
`late_terminal_after_reconciliation`; it must not rewrite a user-visible success
or resurrect the active request.

Reconciliation tombstones must be Python-local, process-local,
non-persistent, and bounded by both an explicit maximum count and an explicit
TTL. The implementation plan must name finite `max_entries` and
`retention_ms` values before code is approved. Expiration should remove expired
entries first and then evict the oldest `reconciled_at_ms` entry when the count
limit is exceeded. Tombstone eviction removes audit memory only; it must not by
itself clear admission quarantine or prove Java retirement.

Late events must not clear the currently active request, mutate the current
`last_result`, publish a normal accepted terminal event, or advance any current
operation. They may only update a matching bounded tombstone:

```text
request_id
command_message_id/correlation_id
session_id
connection_generation
invocation_id
original_command
reconciled_outcome
reconciled_at_ms
late_event_count
expires_at
```

The tombstone must also retain the accepted evidence sequence used by the CAS,
reconciliation time, synthetic UNKNOWN reason, profile, and bounded late-result
audit metadata. The evidence sequence is commit provenance; it is not required
from a later terminal envelope unless that envelope already carries the same
additive field.

A terminal result whose full identity still matches the current active command
must take the normal terminal acceptance path. Tombstone lookup is only for a
terminal result that does not match any current active command. A single result
must not be processed both as the current terminal result and as late tombstone
evidence.

After a synthetic UNKNOWN commit, any later result that matches the tombstone
identity is audit-only, whether it is late running, late accepted, late
terminal, duplicate late terminal, or malformed late result. It must not mutate
`last_java_result`, `local_effective_result`, a current or newer active command,
admission quarantine, retry/replay/cleanup/follow-up state, or the tombstone
identity key.

If a matching terminal result arrives after release, the audit event should use
the tombstone and must not affect a newer active command:

```text
event=late_terminal_after_reconciled_unknown
matched_tombstone=true
newer_active_command_mutated=false
active_release_performed=false
```

After `RECONCILED_UNKNOWN` is committed, a later terminal result may be
associated with the tombstone only when the same full transport identity
matches exactly. If the late result also carries evidence-lineage data, any
mismatch must block association. In this implementation stage that late
terminal is audit evidence only. It must not:

```text
replace the committed synthetic UNKNOWN last_result
clear or overwrite a current or newer active command
repeat active-command release
change gameplay_effect from UNVERIFIED
trigger retry, replay, StopCommand, cleanup, recovery, or a follow-up command
open new-command admission by itself
```

A late result with a stale session, stale generation, wrong request ID, wrong
correlation ID, or nonmatching evidence lineage must not attach to the
tombstone and must not mutate ownership state.

`RECONCILED_UNKNOWN` is a Python-local lifecycle outcome. It must not fabricate
any Java `TaskFinishedEvent`, Java `command_result`, `CommandResultDTO(status=completed)`,
or gameplay-effect result. Keep the facts separate:

```text
last_transport_result:
  only results actually received from the bridge

last_reconciled_outcome:
  Python-local stale ownership closure
```

User-facing wording should stay deliberately limited:

```text
I could not verify the Minecraft command result. I did not mark it complete and
I did not retry it automatically. I only cleared the stale waiting state.
```

This is a Python submission/lifecycle reconciliation responsibility. Do not
solve this in the Korean alias resolver, item catalog, cleanup policy, Java
`DepositCommand`, ChatClef task engine, common DTOs, or wire payload schema.

### Java Queue Retirement Boundary

Python-local `RECONCILED_UNKNOWN` releases only Python's stale active ownership.
It does not automatically clear Java `FabricChatClefCommandQueue.active` or the
Java lifecycle execution. In the current boundary, Java retires its command
context only through the existing terminal send/complete path or detach
handling.

Therefore a first behavior patch must not assume that a Python release makes
the bridge ready for another command. The safe post-release state is:

```text
Python:
  active_request_id=null
  last_java_result=<latest actual Java result, not synthetic>
  local_effective_result.status=unknown
  reconciled_unknown_tombstone=<full identity>
  admission_quarantine.active=true
  admission_quarantine.reason=java_command_context_retirement_unverified
  admission_quarantine.java_context_retirement_verified=false
  admission_quarantine.retirement_evidence_kind=none

Java:
  old commandQueue.active may still be present
  old lifecycle execution may still be present
```

The authoritative quarantine check must live at the common Python transport
command-admission boundary that every command submission caller must pass. It
must run before:

```text
active ownership creation
active token allocation
command envelope send
```

Router, UI, and busy prechecks may read the same quarantine state to produce a
clear user-facing block reason, but they are not the authoritative enforcement
point. Quarantine rejection is side-effect-free:

```text
active command created=false
envelope_sent=false
retry_performed=false
replay_performed=false
stop_submitted=false
cleanup_or_follow_up_submitted=false
```

New command admission remains blocked until authoritative evidence proves that
the prior Java command queue and lifecycle context have been retired. These
signals may count only when their source contract and tests prove Java context
retirement:

```text
matching late terminal result after proven Java clear ordering
trusted Java evidence says the active command context is absent
connection_generation replacement after proven old-context retirement
separately approved Java context retirement completion
verified completion of an explicit recovery/reset path
```

Merely requesting recovery/reset is not sufficient. Quarantine clearing changes
only whether a new command may be admitted; it must not revise the committed
UNKNOWN into success, completion, failure, or verified gameplay effect.

The following are not Java retirement evidence by themselves:

```text
Python process restart
generic disconnect
reconnect
connection generation change
ordinary ownership clear
tombstone expiration or eviction
tombstone missing because tombstones are non-persistent
```

The implementation plan must state whether admission quarantine persists across
or is reconstructed after reconnect and Python process restart. If Python can
restart while Minecraft/Java remains alive and no persisted quarantine marker or
authoritative reconnect-time Java retirement proof exists, the release feature
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

Java-facing `handshake_ack` and `status_snapshot` payloads must not expose
admission quarantine, reconciliation tombstones, or Python-local synthetic
UNKNOWN state.

This keeps the initial behavior patch scoped to Python stale-ownership
reconciliation. Java-side retirement, automatic connection reset, and immediate
post-release command admission are separate approval targets.

### Java Running Lifecycle Evidence Publication

The safest diagnostics-only Java/Python boundary is for Java to publish
additive nonterminal lifecycle evidence through the existing
`command_result` channel with `status=running`.

Do not add a new message type, new public status, Java-originated status
snapshot channel, or terminal outbox path for this evidence. Java already uses
`command_result` for accepted command updates, and Python already validates
websocket, session, generation, request, and correlation before accepting a
result. An accepted `running` update may refresh
`details.commands.last_result` while preserving active command ownership.

Recommended nonterminal stages:

```text
finish_callback_observed_nonterminal:
  status=running
  finish_callback_received=true
  task_finished_event_received=false
  lifecycle_evidence.quiescence.qualified=false
  gameplay_effect=UNVERIFIED

stable_request_quiescence_observed:
  status=running
  finish_callback_received=true
  task_finished_event_received=false
  lifecycle_evidence.quiescence.qualified=true
  gameplay_effect=UNVERIFIED
```

The first stage proves that the Java finish callback evidence crossed the
transport boundary. The second stage proves that Java observed a stable
nonterminal quiescence window. Python dry-run reconciliation may treat only the
second stage as an UNKNOWN-release candidate input, and only after Python's own
EXACT identity and command-profile checks pass.

Stable quiescence must not mean "IdleTask was seen once" or "status was polled
multiple times." Java evidence should include a bounded observation window:

```text
first_client_tick_id
last_client_tick_id
first_observed_at_ms
last_observed_at_ms
observation_count
stable_duration_ms
signature_version
evidence_sequence
```

The stable signature should cover the command identity and runtime ownership
that Java can observe:

```text
dispatch_returned=true
finish_callback_received=true
task_finished_event_received=false
waiting_reason=waiting_for_task_finished_event
active execution/context unchanged
terminal decision absent
terminal send not started
runtime observation available
current task is an approved neutral root
root assignment/generation unchanged
selected chain identity/class unchanged
selected_chain_task_path unchanged
observations happened on distinct client ticks
```

Python must accept additive lifecycle evidence monotonically for one exact
active identity:

```text
lower evidence_sequence than latest accepted
  -> reject
  -> do not regress last_result

same evidence_sequence + same lifecycle fingerprint
  -> idempotent no-op

same evidence_sequence + conflicting lifecycle fingerprint
  -> reject
  -> emit conflict diagnostic

higher evidence_sequence
  -> accept only after websocket, session, generation, request, and correlation
     validation
```

The implementation plan must define the lifecycle fingerprint fields before
code approval. A terminal result that passes exact identity is stronger than
nonterminal lifecycle evidence even when it does not carry an
`evidence_sequence`. A future `RECONCILED_UNKNOWN` CAS must fail closed if the
qualifying stable evidence has no accepted sequence, if that sequence regresses,
or if it changes between eligibility evaluation and commit.

If the task, root assignment, chain, session, generation, request, correlation,
or runtime observation availability changes, Java must reset or withhold the
qualified quiescence evidence. False negatives are acceptable; false positives
can release a live command and are not acceptable.

Java must report raw facts only. These fields and meanings are forbidden in the
Java evidence payload:

```text
identity_quality=EXACT
safe_to_release=true
command_completed
terminal_candidate_confirmed
gameplay_success
task_success
```

EXACT identity is Python-owned because Python compares the accepted envelope
against active command ownership. Java evidence must keep:

```text
classification=nonterminal_diagnostic
status=running
gameplay_effect=UNVERIFIED
```

Diagnostics-only work may stop at:

```text
would_reconcile_to_unknown=true
active_release_performed=false
terminal_status=NONE
gameplay_effect=UNVERIFIED
```

Actual `RECONCILED_UNKNOWN` creation and active release are a later Python
behavior change. The first implementation must be default-disabled,
feature-gated, deposit-profile-only, and Python-only. It must happen under
`FabricChatClefConnectionOwnership`'s command lock as one exception-safe
compare-and-release operation that rechecks full active identity, latest
evidence sequence, `status=running`, command-profile allowance,
profile-specific snapshot/duration thresholds, supported signature version,
`dispatch_returned=true`,
`lifecycle_evidence_stage=stable_request_quiescence_observed`,
`waiting_reason=waiting_for_task_finished_event`, and absence of a stronger
matching terminal result or in-flight terminal send. The same commit must write
the synthetic UNKNOWN result, bounded tombstone, and admission quarantine before
clearing Python active ownership.

Future diagnostics for this boundary should record, in Python-local lifecycle
logs:

```text
request_id, correlation_id, session_id, connection_generation
command_generation or invocation_id
submitted_at, accepted_at, finish_callback_received_at
task_finished_event_received_at
reconciliation_started_at, grace_deadline_at
neutral_state_first_observed_at, neutral_state_last_observed_at
terminal_decided_at, active_released_at
identity_quality
initial_bound_root_task_id/class/description/generation
bound_root_recorded_for_request
current_root_task_id/class/description/generation
request_root_observation_state
current_task_snapshot_age_ms
root_transition_count
same_neutral_root_consecutive_count
returned_to_request_owned_root_after_callback
reconciliation_profile
auto_reconcile_to_unknown_allowed
terminal evidence bitset
identity_match_result and mismatch reason
reconciliation_attempt_id, decision, decision_rule_version
grace_elapsed_ms, neutral_snapshot_count
neutral_observation_duration_ms
submit_count, retry_count, replay_count, stop_submit_count
effect_oracle_available, effect_verification_status
```

Before code approval, the implementation plan must name:

```text
audited branch or commit
Python files, classes, and methods to modify
Java, DTO, and protocol files intentionally left unchanged
feature flag exact name and default OFF behavior
canonical deposit profile predicate
lifecycle fingerprint fields
tombstone max_entries
tombstone retention_ms
quarantine persistence or reconnect/restart fail-closed strategy
immutable CAS state representation or rollback strategy
authoritative transport admission gate location
exact late-terminal dispatch ordering
Java retirement evidence source contract
unit and race test files and test case names
```

Minimum implementation tests for this path:

```text
deposit + EXACT identity + callback + stable approved IdleTask
  + no TaskFinishedEvent
  + deposit profile thresholds satisfied
  -> status=unknown, reconciliation_action=RECONCILED_UNKNOWN
  -> Python active release count 1
  -> success/retry/replay/stop/follow-up count 0
  -> new command admission remains blocked until Java retirement evidence

idle command + IdleTask
  -> neutral evidence not accepted
  -> automatic release count 0

single IdleTask snapshot
  -> release count 0

stale snapshot
  -> release count 0

identity PARTIAL
  -> release count 0

identity MISMATCH
  -> release count 0

request-owned root reappears or is still present
  -> release count 0

ordinary task-producing command with request-owned root NEVER_OBSERVED
  -> release count 0

matching TaskFinishedEvent arrives during grace
  -> ordinary terminal path
  -> reconciled UNKNOWN count 0

terminal send already started or in flight
  -> reconciled UNKNOWN count 0
  -> active release count 0

lower evidence_sequence after newer evidence accepted
  -> last_result unchanged
  -> active release count 0

same evidence_sequence + same lifecycle fingerprint
  -> idempotent no-op
  -> duplicate accepted result count 0

same evidence_sequence + conflicting lifecycle fingerprint
  -> conflict diagnostic count 1
  -> active release count 0

qualifying stable evidence without accepted evidence_sequence
  -> active release count 0

status snapshot read after qualifying evidence
  -> active release count 0

busy-command rejection after qualifying evidence
  -> active release count 0

new command submission attempt while quarantined
  -> active command created false
  -> envelope sent false
  -> retry/replay/stop/cleanup/follow-up count 0

disconnect or session replacement
  -> ownership-loss path
  -> stale-quiescence rule not used

late old event after a new request is active
  -> current active request unchanged
  -> accepted event publication count 0
  -> tombstone late_event_count +1
  -> newer active command mutated count 0

two reconciler workers race
  -> CAS active release count 1
  -> user terminal response count 1

exception during synthetic UNKNOWN commit
  -> active command ownership unchanged
  -> last_result unchanged
  -> tombstones unchanged
  -> admission quarantine unchanged

stale active token after eligibility evaluation
  -> active release count 0
  -> synthetic UNKNOWN count 0

stale accepted_evidence_sequence after eligibility evaluation
  -> active release count 0
  -> synthetic UNKNOWN count 0

terminal accepted before CAS commit
  -> ordinary terminal path wins
  -> reconciled UNKNOWN count 0

terminal arrives after CAS commit with exact tombstone identity
  -> late_terminal_after_reconciled_unknown audit count 1
  -> committed synthetic UNKNOWN unchanged
  -> active release count 0
  -> new command admission remains blocked unless Java retirement is proven

terminal arrives after CAS commit with wrong session/generation/request/correlation
  -> tombstone association count 0
  -> ownership mutation count 0

tombstone TTL expires
  -> tombstone audit memory evicted
  -> admission quarantine unchanged
  -> Java retirement not inferred

tombstone max_entries exceeded
  -> expired entries evicted first
  -> otherwise oldest reconciled_at_ms evicted
  -> admission quarantine unchanged

authoritative Java retirement evidence arrives
  -> admission quarantine may clear
  -> synthetic UNKNOWN remains UNKNOWN

matching late terminal without proven Java retirement invariant
  -> admission quarantine remains active

connection generation replacement without proven old-context retirement
  -> admission quarantine remains active

verified recovery/reset completion
  -> admission quarantine may clear
  -> synthetic UNKNOWN remains UNKNOWN

Python restart while Java may still be alive and quarantine cannot be restored
  -> feature remains disabled outside controlled tests
  -> new command admission remains fail-closed

ordinary reconnect or connection generation change without retirement proof
  -> Java retirement not inferred
  -> admission quarantine remains active
```

### Inventory Snapshot Gap

The current Python bridge status contains command/session fields such as:

```text
active_request_id
active_command
active_command_message_id
active_command_source
active_age_ms
last_result
session_id
connection_generation
```

It does not currently provide reliable inventory fields such as:

```text
free_inventory_slots
occupied_slots
inventory_items
inventory_full
```

Therefore Python cannot currently decide that inventory is full from bridge
data alone. A first implementation must treat inventory capacity as
`UNKNOWN` unless a separate reliable Python inventory provider is explicitly
introduced.

## Required Architecture

The safe responsibility split is:

```text
Korean input
  -> Intent Gate / Rule Parser / Alias Resolver
  -> one validated ChatClef command
  -> Python Command Orchestrator
       -> user-facing reply rendering
       -> optional inventory preflight cleanup
       -> sequential command submission
       -> accepted result event handling
  -> FabricChatClefAdapter
  -> existing Java bridge
  -> existing ChatClef / AltoClef engine
```

All new command sequencing belongs in Python. Java remains the executor of a
single command request at a time, not the owner of multi-step LAVI workflows.

## Reply Rendering

The current working tree includes a deterministic response renderer for
router-owned `response_text` before considering any LLM paraphrasing:

```text
plugins/Minecraft/fabric/chatclef/response/
  chatclef_command_response_renderer.py
```

The renderer should consume already-validated facts:

```text
phase
intent_type
display_item
quantity
submission_status
error_code
terminal_status
runtime_completion_status
gameplay_observation_complete
expected_gameplay_effect_verified
prohibited_effect_absence_verified
```

It must not parse raw DSL such as `get iron_ingot 10` to recover meaning.

Example reply policy:

```text
primary accepted:
  철 10개 수집 명령을 제출했어요.

cleanup starting:
  인벤토리 정리 명령을 제출했어요. 정리 결과를 확인하기 전에는 철 수집 명령을 보내지 않아요.

already busy:
  지금 다른 작업을 하고 있어요.

bridge disconnected:
  마인크래프트 연결이 끊겨 있어요.

cleanup failed:
  인벤토리를 정리하지 못해서 작업을 시작하지 않았어요.

primary completed:
  철 10개 수집 명령의 완료 응답을 받았어요. 아이템 증가는 별도로 확인해야 해요.

primary gameplay effect verified:
  철 10개 수집 결과를 확인했어요.

primary failed:
  철 수집에 실패했어요.

primary unknown:
  철 수집 결과를 확인하지 못했어요.
```

The initial reply path uses deterministic templates only. Normal external
dispatch to Translate, TTS, or other LAVI output listeners remains a separate
planned integration step. A future LLM paraphraser may be added behind the same
interface, but it must receive only confirmed facts and may change tone only. It
must not change command status, command text, lifecycle state, retry policy, or
cleanup policy.

### Response Evidence And Wording Contract

User-facing Korean replies must not claim more than Python has proved:

| Python evidence | Allowed response meaning | Forbidden overclaim |
| --- | --- | --- |
| translation validated | understood, or no reply | submitted, started |
| submission accepted | command was submitted | work started, mining started |
| trusted runtime running | runtime appears to be executing | completed |
| terminal completed | terminal response was observed | item count increased |
| gameplay effect verified | requested effect was verified | none |
| unknown | result unclear; no next step | success, completion, retry |

For example, after accepted submission the safe default is:

```text
철 10개 수집 명령을 제출했어요.
```

It is too strong to say "철 10개 캐러 갈게요" at accepted time, because Python
does not know whether AltoClef will mine, craft, trade, pick up, or satisfy the
request from existing inventory.

Recommended LLM-side dispatch boundary:

```text
emit_external_response(
    text,
    source="minecraft_chatclef",
    send_output=True,
    send_full_output=False,
    remember_history=False,
)
```

This API should:

```text
show the message in the LAVI chat/console surface
send the message through the existing output listener path
preserve response-generation behavior when needed by TTS queues
avoid calling the selected LLM provider
avoid reinserting the message through llm.receive_input()
avoid saving the message to ordinary chat history by default
```

## Korean Mining Phrase Handling

Do not add a new mining command type for this phase. Treat Korean mining
phrases as another way to request the existing GET_ITEM intent:

```text
철 10개 캐줘
  -> intent_type = get_item
  -> target = iron_ingot
  -> quantity = 10
  -> command = get iron_ingot 10
```

Use a shared matcher so the gate and parser do not drift:

```text
plugins/Minecraft/fabric/chatclef/intent/
  korean_acquisition_verb_matcher.py
```

Minimum responsibilities:

```text
matches(text)
classify(text)   # acquire | mining | craft, if classification is useful
strip(text)
```

Initial phrases to recognize:

```text
캐줘
캐 주세요
캐와
캐 와
캐와줘
캐 와줘
캐오기
캐 와서 가져와
채굴해줘
채굴해 주세요
채굴해
채굴해서 가져와
```

Do not use a broad substring trigger for `캐`. It can falsely match phrases
such as:

```text
캐나다 여행 이야기해줘
캐릭터 10개 만들어볼까
캐시가 10개 남았어
```

The parser should normalize item phrases in this order:

```text
1. remove quantity
2. remove acquisition/mining verb phrase
3. normalize spaces
4. remove trailing object particle 을/를
5. remove soft words such as 좀, 제발, 주세요, 줘
6. normalize spaces again
```

Add the standalone alias:

```json
"철": "iron_ingot"
```

The resolver should preserve equipment priority:

```text
철 곡괭이 -> iron_pickaxe
철 도끼   -> iron_axe
철        -> iron_ingot
철괴      -> iron_ingot
철 주괴   -> iron_ingot
```

## Python Command Orchestrator

Add a Python-owned orchestrator only after the reply and transport-event
boundaries are clear:

```text
plugins/Minecraft/fabric/chatclef/orchestration/
  minecraft_chatclef_command_orchestrator.py
```

The orchestrator owns:

```text
operation creation
inventory preflight evaluation
cleanup command submission
waiting for cleanup terminal result
primary command first submission
terminal result handling
duplicate result suppression
exactly-once start and terminal replies
automatic replay prevention
busy response while an operation is active
```

It must not own:

```text
Korean parsing
DSL compiling
WebSocket JSON parsing
Java task manipulation
direct TTS calls
direct Minecraft engine calls
```

Suggested operation context:

```text
operation_id
original_text
validated_translation
primary_command
primary_request_id
cleanup_request_id
state
cleanup_attempt_count
primary_was_accepted
start_response_emitted
terminal_response_emitted
```

Each child command must have its own request ID:

```text
lavi-mc-op-abc-cleanup-1
lavi-mc-op-abc-primary-1
```

Do not reuse request IDs, command message IDs, session IDs, or connection
generations as operation IDs.

## Transport Event Publisher

The current transport validates command results and logs them, but it does not
publish accepted results to a higher-level Python workflow.

Add a Python-local publisher:

```text
plugins/Minecraft/fabric/chatclef/events/
  fabric_chatclef_transport_event_publisher.py
```

Event kinds:

```text
command_result_accepted
connection_lost
server_stopped
```

Event payload should wrap existing objects instead of changing DTOs:

```text
result: CommandResultDTO
session_id
connection_generation
command_message_id
received_at_ms
```

Publish only after `FabricChatClefConnectionOwnership.accept_result()` returns
accepted. Rejected stale or mismatched results must not be published.

For terminal results, publish after the Python active command has been cleared.
This preserves the existing single-active-command boundary before an
orchestrator decides what to do next.

The adapter may expose only narrow subscription methods:

```text
subscribe_transport_events(callback)
unsubscribe_transport_events(callback)
```

Transport event callbacks must not submit another command inline. They should
enqueue events for an orchestrator worker to process.

## State Machine

Recommended initial orchestrator states:

```text
IDLE
PREFLIGHT_EVALUATION
CLEANUP_SUBMITTING
WAITING_CLEANUP_TERMINAL
VERIFYING_CLEANUP_EFFECT
PRIMARY_SUBMITTING
WAITING_PRIMARY_TERMINAL
COMPLETED
FAILED_BEFORE_PRIMARY
FAILED
MANUAL_RECOVERY_REQUIRED
ABORTED
```

Safe transitions:

```text
IDLE
  -> PREFLIGHT_EVALUATION

PREFLIGHT_EVALUATION
  -> PRIMARY_SUBMITTING when inventory is AVAILABLE
  -> PRIMARY_SUBMITTING when inventory is UNKNOWN
  -> CLEANUP_SUBMITTING when inventory is FULL and targeted cleanup is safe
  -> FAILED_BEFORE_PRIMARY when inventory is FULL and cleanup is unsafe

CLEANUP_SUBMITTING
  -> WAITING_CLEANUP_TERMINAL when cleanup is accepted
  -> FAILED_BEFORE_PRIMARY when cleanup is immediately rejected

WAITING_CLEANUP_TERMINAL
  -> VERIFYING_CLEANUP_EFFECT only when the matching cleanup terminal completed
  -> FAILED_BEFORE_PRIMARY for rejected, failed, cancelled, deadline_exceeded, or unknown
  -> ABORTED on disconnect

VERIFYING_CLEANUP_EFFECT
  -> PRIMARY_SUBMITTING only when fresh post-cleanup inventory evidence proves
     the required free slots and protected-item preservation
  -> FAILED_BEFORE_PRIMARY when evidence is FULL, UNKNOWN, stale, mismatched,
     incomplete, or protected-item preservation is not verified

PRIMARY_SUBMITTING
  -> WAITING_PRIMARY_TERMINAL when primary is accepted
  -> FAILED_BEFORE_PRIMARY when primary is immediately rejected

WAITING_PRIMARY_TERMINAL
  -> COMPLETED on completed
  -> FAILED on failed, cancelled, deadline_exceeded, or unknown
  -> MANUAL_RECOVERY_REQUIRED on explicit inventory-full terminal evidence
  -> ABORTED on disconnect
```

When inventory state is `UNKNOWN`, preserve existing behavior by submitting the
primary command without cleanup. Do not run automatic cleanup from unknown
inventory evidence.

### Cleanup Then Primary Gate

The orchestrator may submit the primary command after cleanup only when all of
these are true:

```text
cleanup request was accepted
matching cleanup terminal has completed
active command has been cleared by transport ownership
the operation ID still matches
cleanup_attempt_count == 1
matching request/session/correlation/generation
fresh post-cleanup snapshot available
post-cleanup free slot count >= required_free_slots
protected_item_preservation_verified == true
primary_was_accepted is false
```

If any condition is false, do not submit the primary command automatically.

The current cleanup contract adds one mandatory state between cleanup terminal
and primary submission:

```text
WAITING_CLEANUP_TERMINAL
  -> VERIFYING_CLEANUP_EFFECT
```

Primary submission after cleanup additionally requires fresh post-cleanup
inventory evidence:

```text
matching request/session/correlation/generation
fresh post-cleanup snapshot available
post-cleanup free slot count >= required_free_slots
protected_item_preservation_verified == true
```

`cleanup terminal status == completed` alone is insufficient. The detailed
contract is:

```text
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
```

### Replay Guard

The most important safety assertion:

```text
if primary_was_accepted == true:
    the same primary_command must never be automatically submitted a second time
```

This guard is required because ChatClef `get item count` behaves like an
additional acquisition target relative to current inventory in the existing
Java command implementation. Replaying `get iron_ingot 10` after partial
progress can request 10 more items, not the remaining amount.

## Inventory Snapshot Provider

Use a three-state model:

```text
AVAILABLE
FULL
UNKNOWN
```

Suggested snapshot shape:

```text
free_slots: int | None
occupied_slots: int | None
items: item/count list
source: screen_vision | external | manual | none
confidence: float
observed_at_ms: int
state: AVAILABLE | FULL | UNKNOWN
```

The default provider should be fail-safe:

```text
NoReliableInventorySnapshotProvider -> UNKNOWN
```

Do not infer inventory-full from:

```text
latest.log strings
long task duration
pathing stalls
unknown terminal result
deadline_exceeded terminal result
last_result polling alone
```

If ScreenVision is later used, it should return `FULL` only when the player
inventory screen and slot occupancy are confidently observed from a recent
snapshot. Otherwise it must return `UNKNOWN`.

## Cleanup Policy

Add a separate policy object:

```text
plugins/Minecraft/fabric/chatclef/orchestration/
  chatclef_inventory_cleanup_policy.py
```

Recommended modes:

```text
off
targeted
```

Default should be `off` or `targeted` with reliable inventory snapshots.
Any broad or "non-gear" cleanup mode is outside the current contract and
requires a separate approval. Even then, it must not mean bare `deposit` unless
a later evidence-backed contract explicitly approves that exact behavior.

Do not treat argument-less `deposit` as "store junk only". The existing
ChatClef command can store many non-gear items, including food, torches, fuel,
building blocks, ingots, and materials needed by the active task.

Targeted cleanup must protect at least:

```text
current primary target
materials required for the primary target
tools
armor
food
torches
fuel
movement/building essentials
rare items
user-protected item list
```

Do not create placeholder quantities such as:

```text
deposit dirt 9999
```

Cleanup should be limited per operation:

```text
max_cleanup_attempts = 1
```

## Implementation Phases

### Historical Phase A: Korean Phrases And Start Replies

```text
Implemented at e08af639:
1. Shared Korean acquisition matcher.
2. Gate and rule parser use the shared matcher.
3. Standalone 철 mining/acquisition phrases resolve to iron_ingot.
4. Parser/resolver/compiler regressions cover Korean mining phrases.

Still planned:
5. Deterministic response renderer.
6. General LAVI external-response dispatch API.
```

This phase must not add orchestration or inventory cleanup.

### Phase B: Python Transport Events

```text
1. Add Fabric ChatClef transport event publisher.
2. Publish only accepted command results.
3. Publish terminal events after Python active command clear.
4. Publish disconnect/server-stopped events.
5. Expose narrow adapter subscription methods.
6. Test stale result suppression and subscriber exception isolation.
```

This phase must not automatically submit cleanup or primary follow-up commands.

### Phase C: Orchestrator Without Cleanup

```text
1. Add operation context and state.
2. Consume transport events through a worker queue.
3. Submit primary commands only.
4. Emit start and terminal replies exactly once.
5. Implement busy/disconnect/unknown/replay guards.
```

### Phase D: Inventory Preflight Cleanup

```text
1. Add InventorySnapshotProvider interface.
2. Default provider returns UNKNOWN.
3. Add cleanup policy.
4. Support targeted cleanup only with reliable FULL evidence.
5. Submit primary only after cleanup completed, fresh post-cleanup inventory
   evidence verifies the required effect, protected-item preservation is
   verified, and the replay guard passes.
6. Keep cleanup attempt count capped at one.
```

Phase D is only the old high-level placeholder. The current authoritative
cleanup contract splits this into shadow-mode inventory evidence first and
automatic cleanup execution later. Do not run cleanup from UNKNOWN inventory
state, do not use bare `deposit`, and do not submit the primary command after
cleanup until fresh post-cleanup evidence proves the required free slot.

## 2026-08-19 Phase Mapping

Use these phases for the reviewed archive baseline follow-up:

| Phase | Scope |
| --- | --- |
| Phase 0 | freeze document authority, craft wording as GET, response evidence vocabulary, unknown routing policy, cleanup postconditions, no-retry/no-replay, and hard prohibitions |
| Phase 1 | preserve existing GET acquisition regressions such as `철 10개 캐줘 -> get iron_ingot 10` and false-positive no-submit cases |
| Phase 2 | implemented for the initial gaps: 갑바, 레깅스, 모자, emerald, and torch |
| Phase 3 | partially implemented: current runtime alias coverage snapshot updated without changing command orchestration |
| Phase 4 | implemented for router `response_text`: deterministic immediate Korean response rendering from translation/precheck/accepted facts only |
| Phase 5 | add transport events and operation orchestrator without cleanup execution |
| Phase 6 | partially implemented: cleanup policy and post-cleanup admission helper exist; reliable inventory provider and shadow-mode runtime wiring remain planned |
| Phase 7 | enable automatic targeted cleanup only after the cleanup contract is satisfied |

## Existing Tests To Reuse

Korean parsing and routing:

```text
tests/test_minecraft_chatclef_input_router.py
tests/test_llm_minecraft_input_router.py
tests/test_minecraft_chatclef_command_compiler.py
tests/test_minecraft_chatclef_korean_command_integration.py
tests/test_minecraft_chatclef_korean_gui.py
tests/test_minecraft_chatclef_korean_intent_schema.py
tests/test_minecraft_chatclef_korean_rule_parser.py
tests/test_minecraft_chatclef_natural_language_service.py
tests/test_minecraft_chatclef_item_phrase_resolver.py
```

Fabric ChatClef transport and contracts:

```text
tests/test_minecraft_fabric_chatclef_transport.py
tests/test_minecraft_fabric_chatclef_java_bridge_contract.py
tests/test_minecraft_fabric_chatclef_adapter_skeleton.py
tests/test_minecraft_fabric_chatclef_extension.py
tests/test_minecraft_fabric_chatclef_gui.py
tests/test_minecraft_bridge_protocol.py
```

LLM output/listener behavior:

```text
tests/test_event_listener_isolation.py
tests/test_llm_memory_bridge.py
tests/test_tts_queue_worker.py
```

## New Tests To Add Later

Suggested new or expanded tests:

```text
tests/test_minecraft_chatclef_korean_acquisition_verbs.py
tests/test_minecraft_chatclef_command_response_renderer.py
tests/test_llm_external_response_dispatcher.py
tests/test_minecraft_fabric_chatclef_transport_events.py
tests/test_minecraft_chatclef_command_orchestrator.py
tests/test_minecraft_chatclef_inventory_cleanup_policy.py
```

Required Korean success cases:

```text
철 10개 캐줘
철을 10개 캐줘
철 10개 캐와
철을 10개 캐와줘
철 10개 캐오기
철 10개 채굴해줘
철을 10개 채굴해 주세요
철 캐줘 -> quantity=1
```

All should compile to:

```text
get iron_ingot 10
```

Required false positives:

```text
캐나다 여행 이야기해줘
캐릭터 10개 만들어볼까
캐시가 10개 남았어
오늘 사과 10개 먹었어
```

Required reply/output tests:

```text
accepted primary emits one start reply
immediate rejected does not emit "going" phrasing
busy emits one busy reply
disconnected emits one connection error reply
routed response reaches Translate listener exactly once
routed response reaches TTS listener exactly once through existing path
LLM provider is not called
llm.receive_input recursion does not happen
unknown intent falls through to normal LLM path
Minecraft reply is not saved to ordinary chat history by default
```

Required transport-event tests:

```text
accepted running result publishes one event
accepted terminal result publishes after active command clear
wrong request_id does not publish
wrong session_id does not publish
wrong correlation_id does not publish
old connection_generation does not publish
duplicate terminal result does not publish twice
subscriber exception does not stop the server
disconnect while active publishes connection_lost
event callback does not submit inline
```

Required orchestrator tests:

```text
inventory AVAILABLE -> primary submitted once
inventory UNKNOWN -> primary submitted once, no cleanup
inventory FULL with safe targeted cleanup -> cleanup completed, fresh
  post-cleanup AVAILABLE/effect verified, then primary once
cleanup rejected/failed/cancelled/deadline/unknown -> primary not submitted
cleanup disconnect -> primary not submitted
primary completed -> terminal reply once
primary failed/cancelled/deadline/unknown -> terminal reply once, no replay
primary inventory-full -> no replay, manual recovery required
new user command during operation -> busy, no concurrent submit
cleanup attempts capped at one
app restart does not replay prior operation
```

## Payload, DTO, And Java Feasibility

The requested behavior is feasible without changing:

```text
CommandRequestDTO
CommandResultDTO
StatusSnapshotDTO
BridgeEnvelopeDTO
BridgeMessageType
v1 JSON schema
Fabric Java bridge envelope shape
ChatClef / AltoClef engine code
```

New Python event wrappers and orchestration state should sit outside the v1
wire protocol. They may reference DTO objects but must not add fields to them.

## Implementation Risks To Recheck

Before implementation, recheck these risks against the current branch:

```text
deadlock:
  Do not submit the next command from the WebSocket result callback or event
  loop thread.

duplicate submission:
  Do not allow more than one active command and do not replay accepted primary
  commands.

shutdown race:
  Event subscribers and orchestrator workers must detach cleanly on stop.

stale result:
  Do not advance an operation from mismatched request, session, correlation, or
  connection generation.

duplicate terminal:
  Terminal replies must be emitted exactly once.

inventory uncertainty:
  UNKNOWN must not trigger cleanup.

deposit safety:
  Argument-less deposit is not safe as a default junk cleanup.

LLM coupling:
  LLM paraphrasing must never decide state transitions or generate raw
  ChatClef commands.

history pollution:
  Minecraft acknowledgement replies should not be stored as ordinary chat
  history by default.
```

## Final Boundary

The implementation must preserve this rule:

```text
Korean input expands only into existing validated ChatClef commands.
User-facing replies are emitted by the Python/LAVI output pipeline.
Inventory cleanup is Python-owned preflight orchestration only.
After a primary command is accepted, automatic replay of that command is
forbidden unless a later approved design provides exact remaining quantity or a
safe idempotent command contract.
```

## 2026-08-19 Single-Pass Submission Boundary

The implemented router keeps natural-language translation separate from
submission. It translates once, classifies the validated result, checks runtime
readiness, and passes the same translation into exactly one submit call. DTO
revalidation may recompile the supplied DSL at a trust boundary; that is
validation, not a second natural-language translation.

Untrusted submit responses are normalized through one shared Python
canonicalizer used by the ordinary router and one-shot test boundary. Canonical
output always carries the same request ID at the top level and in nested status.
Malformed mirrors, non-bool success flags, mismatched request IDs, missing nested
status, or contradictory outcome/error combinations become UNKNOWN with
reconciliation required. No string or numeric value is coerced into a bool.

UNKNOWN ownership is latched before another Minecraft route may translate or
submit. It is cleared only through explicit validation of matching terminal
evidence from a trusted Fabric status snapshot. The first later route may perform
that read-only reconciliation, but its triggering command is never translated
or submitted even when reconciliation succeeds; a fresh explicit command is
required. There is no automatic replay, rerun, retry, or inference of success.
