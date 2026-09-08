<!-- 20260908_kpopmodder: Documented the requested STOP single-response policy and the verified STORE_HOME strong-evidence rollout gap. -->
<!-- 20260908_kpopmodder: Kept STOP accepted-START publication policy and STORE_HOME effect verification as separate implementation and rollback units. -->
<!-- 20260908_kpopmodder: Preserved Java, wire, Minecraft task, Gradio identity, admission, and transport ownership. -->
<!-- 20260908_kpopmodder: Reconciled the Python implementation, offline verification, and the distinct live Gradio suppressed-stream input-lock follow-up. -->
<!-- 20260908_kpopmodder: Reconciled the Gradio-only completion correction while preserving STOP and STORE_HOME rollback units. -->

# ChatClef Python STOP Single-Response and STORE_HOME Verified-Terminal Pre-Change Contract

Date: 2026-09-08

## 1. Document status

This document began as one user-requested STOP product-policy change and one
runtime-verified STORE_HOME strong-evidence rollout gap, together with their
bounded implementation contracts. The later Python-only implementation and
offline verification are now recorded in Section 11. A subsequent user-run
runtime confirmed the terminal-only visible STOP response but exposed a
distinct Gradio suppressed-stream input lock. Its Python-only correction and
offline verification are recorded in Section 12 and in the dedicated
investigation. This remains neither a Minecraft
command-execution failure nor a Java result-loss defect.

```text
DOCUMENT_TYPE: PRE_CHANGE_CONTRACT_WITH_IMPLEMENTATION_AND_RUNTIME_FOLLOWUP
CURRENT_BEHAVIOR_AND_ROLLOUT_GAP_EVIDENCE_STATUS: VERIFIED_FROM_USER_RUN_LOGS_AND_SOURCE
IMPLEMENTATION_STATUS: THREE_PYTHON_ROLLBACK_UNITS_IMPLEMENTED_AND_VERIFIED_OFFLINE
BACKEND_SCOPE: FABRIC_CHATCLEF_ONLY
CHANGE_SCOPE: LAVI_OWNED_PYTHON_LIFECYCLE_FEEDBACK_AND_LOCAL_CHAT_GRADIO_BOUNDARY

STOP_CHANGE_UNIT: TRUSTED_KOREAN_STOP_ACCEPTED_START_PUBLICATION_SUPPRESSION
STOP_COMPOSITION_ORDER_HARDENING: TERMINAL_AND_DELIVERY_SINKS_BEFORE_INPUT_EXPOSURE
STORE_HOME_CHANGE_UNIT: TRUSTED_TRANSLATION_VERIFIED_TERMINAL_EVIDENCE_AND_RENDERING
STORE_HOME_ROLLOUT_SLICE: COMPLETED_SUCCESS_AND_ZERO_WORK_ONLY
STORE_HOME_PARTIAL_FAILURE_NATURAL_MAPPING: OPEN_FOLLOW_UP
EVIDENCE_EVALUATION_FAILURE_CONTAINMENT: SEPARATE_COMMON_HARDENING_UNIT
STOP_AND_STORE_HOME_ROLLBACK_UNITS: SEPARATE
GRADIO_COMPLETION_CHANGE_UNIT: LOCAL_CHAT_EMPTY_STREAM_COMPLETION_ADAPTER
GRADIO_COMPLETION_ROLLBACK_UNIT: SEPARATE_FROM_STOP_AND_STORE_HOME
STOP_CURRENT_BEHAVIOR: ACCEPTED_START_SUPPRESSED_TERMINAL_ONLY
STORE_HOME_CURRENT_BEHAVIOR: VERIFIED_STRONG_EVIDENCE_PROFILE_IMPLEMENTED_OFFLINE
STOP_LIVE_VISIBLE_RESPONSE_STATUS: PASS_ONE_TERMINAL_ONLY
STOP_PRE_FIX_LIVE_CHAT_INPUT_RECOVERY_STATUS: FAIL_GRADIO_SUPPRESSED_EMPTY_STREAM
STOP_POST_FIX_OFFLINE_GRADIO_COMPLETION_STATUS: PASS
STOP_POST_FIX_LIVE_CHAT_INPUT_RECOVERY_STATUS: NOT_RUN
STORE_HOME_POST_IMPLEMENTATION_LIVE_RUNTIME_STATUS: NOT_RUN

COMMAND_ADMISSION_CHANGE: NONE
COMMAND_TRANSPORT_CHANGE: NONE
JAVA_SOURCE_CHANGE: NONE
WIRE_SCHEMA_CHANGE: NONE
MINECRAFT_TASK_BEHAVIOR_CHANGE: NONE
GRADIO_PRESENTATION_IDENTITY_CHANGE: NONE
GENERAL_COMMAND_START_TERMINAL_POLICY_CHANGE: NONE
RAW_OR_LEGACY_STOP_CHANGE: NONE
FORGE_OR_MINEMIND_CHANGE: NONE

ORIGINAL_DOCUMENTATION_ONLY_CHANGE: APPLIED
IMPLEMENTATION_FOLLOWUP_SOURCE_CHANGE: PYTHON_ONLY
IMPLEMENTATION_FOLLOWUP_TEST_CHANGE: PYTHON_ONLY
IMPLEMENTATION_FOLLOWUP_OFFLINE_VERIFICATION: PASS_WITH_RECORDED_UNRELATED_FULL_SUITE_LIMITATIONS
JAVA_BUILD_FOR_IMPLEMENTATION: NOT_REQUIRED_PYTHON_ONLY
DEPLOYMENT_FOR_IMPLEMENTATION: NOT_RUN
LIVE_USER_RUN_REVIEW: STOP_RETEST_REVIEWED_READ_ONLY
COMMIT_OR_PUSH: NONE
```

Both changes are Python-side, but they have different owners and causes:

- Before this implementation, STOP correctly followed the prior two-phase
  contract by publishing two intentional lifecycle responses for one accepted
  request. The applied policy now supersedes that STOP-specific START
  publication with one terminal response.
- Before this implementation, STORE_HOME received a valid successful terminal
  result while the generalized lifecycle evidence registry deliberately left
  it in the cautious rollout. The applied profile now accepts only the strict
  verified evidence defined in Section 5.

They must therefore remain independently reviewable, testable, and reversible.

Related authority:

- [General Natural Korean Command Lifecycle Feedback Implementation Record](chatclef-general-natural-korean-command-lifecycle-feedback-implementation-record-2026-09-07.md)
  records the implemented general lifecycle baseline;
- [General Natural Korean Command Lifecycle Feedback Pre-Change Contract](chatclef-general-natural-korean-command-lifecycle-feedback-pre-change-contract-2026-09-07.md)
  owns the count-bearing STORE_HOME wording and command-wide cautious-evidence
  baseline;
- [Korean Command Feedback, Crafting Defaults, and Trusted STOP Contract](chatclef-korean-command-feedback-crafting-stop-pre-change-contract-2026-09-05.md)
  owns the prior intentional accepted-START plus verified-terminal STOP policy;
- [Gradio Routed-Response Duplicate Presentation Investigation](chatclef-gradio-routed-response-duplicate-presentation-investigation-2026-09-07.md)
  owns actual same-identity UI retry/deduplication behavior;
- [Gradio Suppressed-Response Input Lock Investigation](chatclef-gradio-suppressed-response-input-lock-investigation-2026-09-08.md)
  owns the distinct empty current-input stream failure exposed after accepted
  STOP START publication was suppressed;
- [Fabric ChatClef Bridge Protocol v1](fabric-chatclef-bridge-protocol-v1.md)
  owns the existing six-field typed STORE_HOME result contract;
- [Minecraft Backend Separation](minecraft-backend-separation.md) keeps this
  work inside the Fabric ChatClef Python adapter and out of Forge MineMind.

## 2. Reviewed authority and evidence

The repository and user-run evidence were reviewed read-only before this
contract was written.

```text
REVIEWED_REPOSITORY_ROOT: C:\Vtuber_Souorce_Code\LAVI
REVIEWED_BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
REVIEWED_HEAD: c5968582a826e7fb10b857e7c50388c5e3ec93d2
REVIEWED_WORKTREE_STATE: DIRTY_EXISTING_CHANGES_PRESERVED
LOG_REVIEW_SNAPSHOT_AT_LOCAL: 2026-09-08T02:55:56+09:00
LOG_SIZE_VALUES: SNAPSHOT_NOT_LIVE_INVARIANTS

ACTIVE_INSTANCE: C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01
LATEST_LOG_SIZE_BYTES: 12336898
STDOUT_LOG_SIZE_BYTES: 13602816
INSTANCE_AUDIT_SIZE_BYTES: 2697962
LAVI_LOG_SIZE_BYTES: 858831
```

The files were not empty. `instance_audit.txt` ended before the two later
reproductions, so the authoritative incident evidence is the active instance's
`stdout-logs.txt` together with LAVI's `20260908_011816_log.txt`. Raw request,
session, and message identifiers are intentionally omitted from this document.

### 2.1 STOP evidence

For one trusted Korean STOP request, the same input event produced two distinct
Python lifecycle publications:

```text
01:51:40 command_start -> "멈출게"
01:51:41 stop_terminal -> "멈췄어"
```

The LAVI log records the first lifecycle response and its direct Chat UI
delivery at lines 637 and 640, then the matching terminal response and queued
UI delivery at lines 647 and 650. TTS accepted and played each sentence once.
An earlier STOP at approximately 01:51:05 reproduces the same sequence.

Java accepted one STOP request, executed it once, and returned one correlated
STOP-control terminal result. The interrupted original command also has its
separate cancelled result, which the existing terminal arbitrator suppresses
from user-facing feedback. The two distinct Python publications fully explain
the two observed cards; there is no evidence of a repeated Java STOP command,
repeated delivery of the same STOP-control terminal, or repeated presentation
of one UI identity.

### 2.2 STORE_HOME evidence

For this reproduced request, the Java terminal result proves that the
STORE_HOME operation succeeded:

```text
result=COMPLETED
storedItems=909
touchedStacks=25
remainingStacks=0
reason=paired_delta_confirmed
```

The active instance `stdout-logs.txt` records this result at line 21790, the
first terminal send attempt at line 21851, and
`matching_task_finished` at line 21857. LAVI's log line 933 records the
correlated Python result with all of the following:

```text
status=completed
ok=true
operation=store_home
store_home_result=COMPLETED
stored_items=909
remaining_stacks=0
goal_satisfied=true
result_reason=matching_task_finished
result_fidelity=callback_plus_matching_user_task_event
```

The cautious sentence was nevertheless selected at LAVI log line 934. For this
reproduced request, the sentence therefore does not indicate missing Minecraft
evidence. It indicates that the Python generalized lifecycle does not yet
consume the existing typed STORE_HOME evidence as a strong terminal profile.

## 3. Verified causes

The current ownership boundaries used for this finding are:

```text
STOP accepted route outcome:
  plugins/Minecraft/fabric/chatclef/input/stop/routing/
    stop_control_route_outcome_builder.py
STOP phrase authority:
  plugins/Minecraft/fabric/chatclef/response/stop/
    stop_control_response_renderer.py
STOP terminal publication:
  app_core/composition_core/component_wiring/
    minecraft_stop_terminal_response_wiring.py
STOP terminal-before-input composition order:
  app_core/composition_core/component_wiring/
    component_event_listener_wiring.py
UI presentation identity:
  llm_core/routed_response/presentation/ui/
    routed_response_ui_presentation_identity.py

General terminal evidence registry and facade:
  plugins/Minecraft/fabric/chatclef/transport/command_feedback/lifecycle/evidence/
    command_terminal_evidence_profile_registry.py
    command_terminal_evidence_evaluator.py
Terminal fact creation:
  plugins/Minecraft/fabric/chatclef/transport/command_feedback/lifecycle/result/
    command_feedback_result_coordinator.py
Immutable terminal fact:
  plugins/Minecraft/fabric/chatclef/transport/command_feedback/lifecycle/terminal/
    command_terminal_fact.py
General Korean terminal wording:
  plugins/Minecraft/fabric/chatclef/response/command_lifecycle/
    command_lifecycle_response_renderer.py
  plugins/Minecraft/fabric/chatclef/response/command_lifecycle/grammar/
    korean_command_phrase_renderer.py
  plugins/Minecraft/fabric/chatclef/response/command_lifecycle/grammar/boundary/
    korean_command_terminal_renderer.py
Existing typed STORE_HOME authority:
  plugins/Minecraft/fabric/chatclef/result/store_home/
    store_home_terminal_payload.py
Existing specialized STORE_HOME response boundary:
  plugins/Minecraft/fabric/chatclef/response/store_home/
    store_home_command_response_renderer.py
```

### 3.1 STOP exposes two lifecycle phases by policy, not by UI duplication

`StopControlRouteOutcomeBuilder.submitted()` currently renders a successful
send as `response_kind=command_start` with `멈출게`. The independent STOP
terminal wiring later renders the correlated result as
`response_kind=stop_terminal` with `멈췄어`.

The UI presentation identity correctly includes at least the input event,
route kind, response kind, source, and badge. Consequently, the two responses
are deliberately different presentation identities even when they share the
same input event. The Gradio drain must not collapse them.

Current behavior:

```text
trusted STOP accepted
-> command_start: 멈출게
-> stop_terminal: 멈췄어
-> two UI cards and two TTS utterances
```

Required behavior:

```text
trusted STOP accepted
-> submission remains registered and correlated, but has no user-visible START
-> stop_terminal: 멈췄어
-> one UI card and one TTS utterance
```

### 3.2 STORE_HOME is excluded from strong evidence in Python

The generalized terminal profile registry currently assigns:

```text
get        -> verified / get_acquisition
store_home -> cautious / cautious_terminal
```

The new closed profile assignment is:

```text
store_home -> verified / store_home_completion
```

The common terminal evaluator supports only the GET evaluator. It also returns
false explicitly when `operation=store_home` or `store_home_result` is present.
That false value reaches the generic Korean terminal renderer, whose
STORE_HOME branch has only a cautious completed sentence.

This is the exact failed boundary:

```text
valid correlated STORE_HOME Java result
-> Python result reconciliation succeeds
-> existing typed STORE_HOME fields arrive intact
-> generalized evidence evaluator returns verified=false by policy
-> generic cautious STORE_HOME sentence
```

The repository already owns a strict typed validator at
`result/store_home/store_home_terminal_payload.py`. The correction is to adapt
that validator into the generalized lifecycle evidence path. It is not to add
another Java result, infer success from `status=completed`, or loosen the wire
contract.

## 4. STOP single-response contract

### 4.1 Exact scope

The changed policy applies only when all of the following are true:

- the LAVI-owned Korean STOP classifier accepts a trusted marked STOP phrase;
- the STOP claim and submission path accepts the request;
- the control send outcome is internally consistent and reports `accepted`;
- the accepted submitter invariant has already installed the matching STOP
  terminal tracker.

The policy does not apply to native/raw ChatClef `stop` or `@stop`, legacy
ingress, an unsafe phrase, a duplicate claim, local rejection, bridge
disconnection, unavailable capability, rejected send, or uncertain send.

`StopControlRouteOutcomeBuilder` receives the accepted submission outcome, not
the tracker registry. It must rely on that accepted submitter invariant and
must not acquire a second tracker dependency or reimplement ownership checks.

Tracker installation is not proof that the optional terminal-listener callback
has been connected. Production app composition must therefore wire the existing
STOP terminal callback and its downstream output/TTS listener and receipt
topology before it wires/exposes the Minecraft input router, `input_component`
output listener, or trusted final-voice listener. This is a composition-order
hardening, not a new admission predicate: headless and embedded callers must not
gain a presentation-dependent command gate.

For the production Fabric-extension topology, the STOP callback wiring must
return a non-`None` callable before input exposure. A missing setter, emitter,
or callback is a composition-startup failure for that topology, not a reason to
accept a trusted STOP silently. The downstream-before-input order covers the
existing `llm -> translate -> tts` listeners, lifecycle TTS receipt wiring, and
the TTS output listener; no second sink or delivery graph is introduced.

### 4.2 Accepted submission semantics

Accepted STOP submission must remain a handled route decision. It must retain
all existing non-presentation behavior:

- STOP claim creation and capacity limits;
- request/session/generation/message correlation;
- send acceptance checks;
- active-command cancellation barrier;
- STOP terminal tracker registration;
- original-command cancellation suppression;
- terminal compare-and-set and claim spending;
- no retry and no automatic resubmission.

Only the accepted START publication is suppressed. The minimal existing path
is to return the handled accepted decision with empty `response_text`; the
trusted response authorizer then projects it to `suppress_response=True`, does
not issue a response-emission capability, and prevents the generic empty-text
placeholder. Suppression means all of the following are zero for the accepted
START phase:

```text
CHAT_UI_CARD_COUNT: 0
ROUTED_OUTPUT_COUNT: 0
TTS_ENQUEUE_COUNT: 0
TTS_PLAYBACK_COUNT: 0
UI_PRESENTATION_QUEUE_COUNT: 0
RESPONSE_EMISSION_CAPABILITY_COUNT: 0
```

An empty response must not fall through to a generic `[Input routed]`
placeholder or any equivalent synthetic card.

### 4.3 Terminal semantics

The existing specialized STOP terminal response remains the sole visible
result for an accepted request:

| Correlated terminal fact | Visible response |
| --- | --- |
| the existing closed STOP success validator accepts the complete profile, including `status=completed` and `control_outcome=stopped` | `멈췄어` |
| valid rejected or deadline-exceeded no-mutation profile | existing truthful rejection/deadline sentence |
| unknown or unverifiable typed outcome | existing conservative unknown sentence |
| duplicate/spent terminal claim | no additional response |
| cancelled result belonging to the command that STOP interrupted | no STOP response and no duplicate command response |

The matching terminal may be delayed; the system must wait for it rather than
claiming success at send acceptance. It remains non-preempting with respect to
new LLM output and follows the existing output/TTS one-time delivery receipts.
The complete validator retains the request-kind, operation, correlation,
target quartet, generation/socket, tick, delivery, and original-command
retirement requirements in the
[Fabric ChatClef Bridge Protocol v1 STOP-Control Profile](fabric-chatclef-bridge-protocol-v1.md#additive-stop-control-profile-within-v1).

### 4.4 Local outcomes remain immediate

An outcome that never acquired a valid accepted STOP terminal path must retain
one immediate response so the user is not left without feedback. This includes
local classification rejection, in-flight/capacity rejection, disconnected
bridge, unavailable capability, rejected send, and uncertain send. These
outcomes must not later emit a success terminal. In particular,
`control_send_unknown` enters the existing quarantine, publishes its immediate
unknown sentence once, absorbs any late completed result, and publishes late
success zero times.

## 5. STORE_HOME verified-terminal contract

### 5.1 Initial rollout boundary

The first strong STORE_HOME profile is limited to an accepted
`form_kind=trusted_translation` descriptor, matching the reproduced Korean
command path. This covers the already admitted LAVI Chat and final microphone
event sources. Raw-command `lavi_gui` and command-name-only generalized
descriptors remain cautious in this change. The existing `direct_typed`
specialized STORE_HOME response path is unchanged. Promoting another
generalized descriptor form requires its own descriptor-authority and
correlation review; it must not happen implicitly through the global profile
registry.

This is an evidence-strength boundary, not a command-availability boundary.
All currently accepted commands continue to receive lifecycle feedback, while
only a command/source combination with an implemented effect proof may make a
strong success claim.

### 5.2 Descriptor agreement gate

Strong STORE_HOME evaluation requires exact agreement with the immutable
request descriptor:

```text
descriptor.command_name == store_home
normalized descriptor.command == store_home
descriptor.form_kind == trusted_translation
descriptor.detail_level == typed
descriptor.evidence_profile_id == store_home_terminal_evidence_v1
descriptor.rollout_state == verified
descriptor.command_source == descriptor.input_source
descriptor.command_source in {lavi_chat_ui, voice_input_final}
profile.success_evaluator_id == store_home_completion
```

A raw, command-name-only, source-mismatched, profile-mismatched, or cautious
descriptor is not promoted by payload contents. It remains cautious even if a
payload happens to resemble valid STORE_HOME evidence.

### 5.3 Outer lifecycle and correlation gate

STORE_HOME success must be considered only after the existing common result
acceptance and reconciliation succeeds. The result must belong to the exact
active owner and preserve all existing WebSocket/session/generation/request/
message identity checks and terminal one-time authority.

The accepted canonical `CommandResultDTO` must satisfy every outer condition:

```text
result.status == completed
result.ok is True
result.error_code is None
result_reason == matching_task_finished
result_fidelity == callback_plus_matching_user_task_event
```

No subset is sufficient. In particular, `completed`, a callback, a lack of
exception, or a nonzero stored count alone is not proof.

The existing parser/DTO boundary remains unchanged. `CommandResultDTO`
normalizes `ok` with `bool(...)` before the lifecycle evaluator receives it,
then enforces agreement with canonical status and rejects a successful status
paired with an error code. The STORE_HOME evaluator checks only the canonical
invariants above; it must not pretend that it can recover the discarded raw
`ok` type. This contract does not claim exact raw-boolean validation. Any future
wire-shape hardening is a separate common DTO/parser contract, not part of this
response-profile change.

### 5.4 Typed STORE_HOME effect gate

The result data must be parsed through the existing
`StoreHomeTerminalPayload.from_data()` authority. Its current fail-closed
invariants remain mandatory:

- `operation` is exactly `store_home`;
- `store_home_result` is a known typed result;
- `stored_items` and `remaining_stacks` are exact nonnegative integers, with
  booleans rejected as integers;
- `reason` is a nonempty string;
- `goal_satisfied` is an exact boolean;
- `goal_satisfied` is true exactly for `COMPLETED`;
- `COMPLETED` has `remaining_stacks == 0`;
- a partial result has a positive `stored_items` count.

The generalized evaluator receives the single canonical `CommandResultDTO.data`
map and therefore uses `StoreHomeTerminalPayload.from_data()`. Conflict checks
between legacy wrapper copies in `details` and `status.data` belong to the
existing `StoreHomeTerminalPayload.from_command_result()` path and remain a
separate regression contract. The new evaluator must not claim to compare raw
wrapper copies it never receives.

Before invoking that parser, the STORE_HOME evaluator must reject cross-family
marker collisions with closed forbidden-key semantics. The presence of the
`request_kind` key rejects STORE_HOME success regardless of its value;
`operation=stop_ai` also rejects it. Presence of any closed STOP-only data key
rejects it:

```text
control_outcome
control_reason
target_scope
target_resolution
requested_target_request_id
requested_target_command_message_id
requested_target_session_id
requested_target_server_connection_generation
resolved_target_request_id
resolved_target_command_message_id
resolved_target_session_id
resolved_target_server_connection_generation
target_state_before
target_state_after
original_result_delivery
stop_command_invoked
connection_generation
java_socket_generation
executed_client_tick
verified_client_tick
```

The same key-presence rule applies to this exact closed GET-reserved set at the
STORE_HOME data-map boundary:

```text
effect_profile_id
effect_profile_version
effect_payload
effect_kind
target_item
target_match_ids
quantity_semantics
requested_count
requested_delta
before_target_count
after_target_count
target_count_delta
effect_observation_status
effect_observation_reason
```

This includes keys normally owned inside a nested GET `effect_payload`: a flat
stray copy is still a family collision. Malformed or value-mutated specialized
evidence never falls through to another family's success evaluator. This gate
is required because
`StoreHomeTerminalPayload.from_data()` validates its owned fields but ignores
unrecognized extra keys.

A strong completed success additionally requires:

```text
store_home_result == COMPLETED
goal_satisfied is exact True
remaining_stacks == 0
1 <= len(trimmed reason) <= 256
```

The dedicated evaluator owns the 256-character maximum, matching the current
GET evidence-reason bound. The existing typed payload parser remains compatible
for its specialized legacy caller; an oversized reason is simply ineligible
for generalized strong success. The bounded reason remains in the immutable
projection for typed fidelity but is never rendered as user-facing text.

### 5.5 Immutable evidence result and rendering projection

The common terminal renderer must not receive mutable raw `result.data` and
must not repeat validation. The evidence facade gains an additive immutable
evaluation result:

```text
CommandTerminalEvidenceEvaluation
  verified: exact bool
  projection: immutable typed object or None
```

The new `evaluate(result, context=...)` method returns that object. The existing
`verified(result, context=...) -> bool` API remains as a compatibility facade
and delegates to `evaluate(...).verified`; no mutable last-evaluation field is
allowed. The production server currently injects
`CraftingFeedbackEffectVerifier`, so that legacy compatibility facade also gains
an additive `evaluate()` which delegates to the common evaluator; its existing
`verified()` delegates to the same evaluation path.

`CommandFeedbackResultCoordinator` prefers `evaluate()` and freezes both values
into the terminal fact while it still owns the correlated terminal transaction.
For a legacy injected collaborator that exposes only `verified()`, the
coordinator may wrap an exact-boolean return in an evaluation with
`projection=None`; a non-`bool` return fails closed instead of being truthiness
coerced. That compatibility fallback can preserve GET behavior but can never
authorize projected STORE_HOME success. It must not call both APIs for one
result.

`evaluate()` is a total, side-effect-free validation boundary for canonical DTO
values: missing fields, ordinary malformed dictionaries, and wrong scalar or
container types return `verified=False, projection=None` rather than raising.
If an injected evidence collaborator nevertheless raises `Exception`, the
coordinator contains only that evidence-evaluation failure after the terminal
claim, records a bounded diagnostic, and stages one unverified cautious fact.
It does not catch `BaseException`, retry evaluation, reclaim the terminal, or
resubmit the command. Correlation, transport, and tracker exceptions are not
broadened into this guard.

The bounded diagnostic is owned by a dedicated result-layer reporter and has a
closed schema only:

```text
event=command_terminal_evidence_evaluation_failed
command_name=<registered bounded command name or unknown>
status=<bounded terminal status>
exception_type=<bounded class name only>
fallback=unverified_cautious
```

It does not include exception text, result data, command arguments, item names,
player names, positions, session/request/message identifiers, or free-form
input. Reporter failure is isolated and cannot prevent the already-decided
cautious fact from being staged.

For a strong completed STORE_HOME success, the already frozen
`StoreHomeTerminalPayload` is the immutable projection. It contains the
validated closed result and counts needed for truthful wording; the renderer
never emits its free-form `reason`. Any non-success or insufficient-evidence
evaluation returns `verified=False` with no success projection. Reusing this
presentation-neutral result type avoids a response-to-transport dependency and
avoids creating a second almost-identical STORE_HOME DTO.

`CommandTerminalFact` receives an optional `evidence_projection` field with a
`None` default so current GET, accepted-without-callback, and test construction
paths remain compatible. For the STORE_HOME family, absence, a wrong projection
type, or `verified is not True` fails closed to the cautious wording. Existing
GET `verified=True` with `evidence_projection=None` remains valid and retains
its current renderer behavior.

### 5.6 Korean response mapping

The natural lifecycle renderer must keep `[Minecraft]` out of the spoken/body
text; Minecraft provenance remains presentation metadata and a UI badge.

| Verified terminal state | Canonical sentence |
| --- | --- |
| `COMPLETED`, `stored_items > 0`, `remaining_stacks == 0` | `아이템 {stored_items}개를 집에 정리했어` |
| `COMPLETED`, `stored_items == 0`, `remaining_stacks == 0` | `집에 정리할 아이템이 없었어` |
| correlated completed result with valid non-`COMPLETED` typed payload | existing cautious STORE_HOME completed sentence; never `다 정리했어` |
| correlated result whose outer status is failed/rejected/cancelled/deadline_exceeded/unknown | existing generalized status-specific failure sentence; never `다 정리했어` |
| correlated completed result with malformed, inconsistent, or unsupported effect evidence | `아이템을 집에 정리하는 작업은 끝났는데, 실제로 정리됐는지는 확인하지 못했어` |
| uncorrelated or identity-mismatched result | no terminal fact, UI, output, or TTS response |

The success sentence retains the count-based wording already specified by the
general lifecycle contract. The immutable projection provides that exact count
and distinguishes a real transfer from a valid zero-work completion. This
rollout closes only `COMPLETED` success and verified zero-work wording. New
partial- and failure-specific natural STORE_HOME phrases remain an open
follow-up; this slice retains the existing cautious or generalized
status-specific mappings shown above.

## 6. Responsibility and folder contract

This change must not add STORE_HOME branches to the already multi-purpose GET
implementation. When implementation begins, the LAVI-owned evidence code is
split by responsibility:

```text
transport/command_feedback/lifecycle/evidence/
  command_terminal_evidence_evaluator.py
    public facade and profile dispatch only
  command_terminal_evidence_evaluation.py
    immutable verified decision plus optional typed projection
  command_terminal_evidence_profile_registry.py
    closed profile-to-evaluator mapping only
  get/
    get_acquisition_terminal_evidence_evaluator.py
  store_home/
    store_home_terminal_evidence_evaluator.py

transport/command_feedback/lifecycle/result/
  command_feedback_result_coordinator.py
    freezes one completed evaluation inside the correlated terminal transaction
  diagnostics/
    command_terminal_evidence_failure_reporter.py
      closed bounded event for isolated evaluator failure only

transport/command_feedback/lifecycle/terminal/
  command_terminal_fact.py
    carries optional immutable evidence_projection with a compatible None default

transport/command_feedback/crafting/
  crafting_feedback_effect_verifier.py
    legacy facade forwarding evaluate() and verified() to one common evaluator

response/command_lifecycle/
  command_lifecycle_response_renderer.py
    forwards the immutable fact projection only
  grammar/
    korean_command_phrase_renderer.py
      forwards the projection without selecting lifecycle truth
    boundary/
      korean_command_terminal_renderer.py
        family dispatch and existing compatibility surface
  terminal/store_home/
    korean_store_home_terminal_renderer.py

app_core/composition_core/component_wiring/
  component_event_listener_wiring.py
    wires the existing STOP terminal and downstream delivery sinks before input
```

The existing public evaluator import and call surface should remain compatible
while its GET logic is delegated. For a completed terminal,
`CommandFeedbackResultCoordinator` calls `evaluate()` exactly once so it can
freeze both the boolean decision and optional projection; for every
non-completed terminal it freezes an unverified result with no projection and
retains the existing status-specific rendering. The STORE_HOME evaluator owns
typed evidence validation and returns the existing frozen
`StoreHomeTerminalPayload` only for strong completed success. The two response
forwarders carry that projection without revalidation or mutable state. The
STORE_HOME terminal renderer owns only Korean wording from the immutable
projection. None of these components owns command
admission, terminal correlation, transport, UI queueing, TTS receipts, or
Minecraft behavior.

The existing `StoreHomeCommandResponseRenderer` is not reused as the generalized
lifecycle renderer. It combines an older response boundary with embedded
`[Minecraft]` text. Its typed payload validator is reusable authority; its
presentation format is not.

The STOP policy change remains inside the existing STOP routing/publication
boundary. The composition root receives only the ordering hunk required to
connect its existing STOP terminal callback and downstream delivery sinks before
input exposure; it gains no business logic or lifecycle state. Neither change
justifies a generic lifecycle suppression manager, a second STOP tracker, or a
new active-command owner.

## 7. Compatibility and non-goals

The implementation must preserve:

- all 26 registered command profiles and current admission policy;
- every item/form already admitted by its existing source policy;
- normal START plus TERMINAL behavior for non-STOP finite commands;
- distinct UI identities for same-event `command_start` and terminal facts;
- the Gradio FIFO, acknowledgement, epoch, retry, and presentation identity;
- Java result projection and the six-field STORE_HOME wire payload;
- WebSocket/session/generation/request/message correlation;
- command lock, active owner, terminal CAS, and STOP-specific tracker;
- output listener and separate TTS enqueue/playback deduplication;
- no LLM recall, automatic retry, or command resubmission;
- Fabric ChatClef and Forge MineMind separation.

Explicit non-goals are:

- deduplicating cards by visible text or event ID alone;
- merging all command START and TERMINAL responses;
- treating every `completed` result as verified success;
- changing STORE_HOME transfer, inventory, trusted-container, or Task behavior;
- changing the Java DTO, protocol schema, or result-fidelity vocabulary;
- changing the common `CommandResultDTO` raw-`ok` coercion contract;
- enabling raw/legacy commands or another backend;
- deployment, Minecraft launch, or live-world mutation.

## 8. Test contract

### 8.1 Existing tests affected by the policy change

The implementation must update the assertions that encode the superseded
accepted-STOP and STORE_HOME-cautious policies:

- `tests/minecraft_chatclef/stop/test_stop_control_presentation_detail.py`
  currently expects accepted STOP to expose `command_start`;
- `tests/minecraft_chatclef/command_lifecycle/test_command_lifecycle_registry_contract.py`
  currently fixes GET as the only verified profile;
- `tests/minecraft_chatclef/command_lifecycle/test_command_feedback_descriptor_factory.py`
  currently fixes trusted STORE_HOME as cautious.

`tests/minecraft_chatclef/stop/test_stop_control_response_renderer.py` may keep
the standalone `render_start()` phrase assertion for compatibility; the
superseded behavior is that the accepted route publishes that phrase, not that
the renderer can produce it. Retiring the unused method is optional cleanup and
is not required for this behavior correction.

The cross-family GET evidence test must continue proving that STORE_HOME fields
can never fall through to GET validation. After dispatch is introduced, it must
also prove that a STORE_HOME descriptor reaches only the STORE_HOME evaluator.
Existing STOP correlation/exactly-once tests and existing typed STORE_HOME
payload tests are retained as regression authority rather than removed.

### 8.2 Required STOP cases

1. Accepted LAVI Chat trusted Korean STOP submits once and retains its
   tracker/barrier, but authorizes `suppress_response=True` and produces zero
   START UI, output, TTS enqueue, and TTS playback events.
2. Accepted final-microphone `voice_input_final` STOP has the same zero-START,
   one-terminal contract; partial/non-final microphone input remains outside
   trusted STOP admission.
3. An accepted START route outcome with empty `response_text` issues no
   response-emission capability and does not create `[Input routed]` or another
   placeholder card.
4. Production app composition wires the specialized STOP terminal callback and
   its downstream output/TTS listener and receipt topology before the Minecraft
   input router and chat/final-voice listeners are exposed. This is a
   composition-order invariant, not a new command-admission gate: headless or
   embedded callers are not rejected solely because a terminal callback is
   absent. In the production Fabric topology, callback wiring returns a
   non-`None` callable before exposure.
5. The one matching successful terminal produces exactly one `멈췄어` UI card,
   output event, TTS enqueue receipt, and TTS playback receipt.
6. A terminal that wins the existing terminal-before-submit-return race still
   produces exactly one terminal card and TTS utterance.
7. Each valid rejected and deadline-exceeded no-mutation terminal produces zero
   START publications and exactly one existing truthful terminal response.
8. Each valid uncertainty terminal produces zero START publications and one
   conservative unknown terminal response while retaining quarantine.
9. A malformed matching terminal produces zero START publications, one
   conservative unknown terminal response, and keeps the tracker/barrier in
   malformed quarantine so no later success can escape.
10. A duplicate or spent terminal produces no additional response.
11. The interrupted original command's cancellation result remains suppressed.
12. Local rejection produces its one existing immediate truthful response.
13. `control_send_unknown` produces one immediate unknown response and absorbs a
   late matching completed result with zero later success responses.
14. Raw/legacy STOP behavior is unchanged.
15. A normal non-STOP command with the same input event for START and TERMINAL
   still presents both distinct lifecycle responses once.

### 8.3 Required STORE_HOME cases

1. An accepted trusted descriptor uses the STORE_HOME verified evaluator.
2. The exact runtime sample (`COMPLETED`, 909 stored, zero remaining, goal true,
   matching terminal and fidelity) renders `아이템 909개를 집에 정리했어`
   once.
3. Verified `COMPLETED` with zero stored and zero remaining renders
   `집에 정리할 아이템이 없었어` once.
4. Canonical DTO construction retains its status/`ok`/error consistency tests.
   After that construction, wrong result reason or result fidelity fails the
   STORE_HOME evaluator independently. Exact raw `ok` typing is not claimed by
   this contract because the existing DTO coercion has already discarded it.
5. Each typed field fails closed independently: wrong operation, unknown result,
   boolean/count confusion, negative count, empty or over-256-character trimmed
   reason, wrong goal type, inconsistent goal/result, or completed with
   remaining stacks. Reason boundary tests accept exactly 256 trimmed
   characters, reject 257, and verify that surrounding whitespace is removed
   before the bound is applied.
6. A STORE_HOME payload mixed with any exact closed STOP-only or GET-reserved
   key listed in section 5.4 fails closed before typed success. Mutation tests
   cover every key independently, including `request_kind` with an arbitrary
   wrong value, `operation=stop_ai`, and flat copies of nested GET payload keys.
7. Descriptor disagreement fails closed independently: command name/text, form,
   detail, source, evidence profile, rollout, or evaluator ID.
8. A partial typed outcome never renders the completed-success sentence.
9. Every typed failure never renders the completed-success sentence.
10. Legacy wrapper copies that conflict between `details` and `status.data`
   remain rejected by the existing `from_command_result()` regression tests.
11. The source matrix is explicit: LAVI Chat and final microphone
   `form_kind=trusted_translation` descriptors are verified candidates;
   raw-command `lavi_gui` `store_home`/`@store_home` and command-name-only
   descriptors remain cautious; the separate `direct_typed` STORE_HOME response
   path remains unchanged.
12. A correlation or identity mismatch creates no terminal fact, UI response,
   output event, or TTS event.
13. A missing/wrong-type STORE_HOME `evidence_projection`, or a valid
   STORE_HOME projection paired with `verified=False`, cannot render success;
   existing verified GET with no projection remains compatible.
14. `CraftingFeedbackEffectVerifier.evaluate()` preserves the common evaluation
   and projection, while a verified-only legacy collaborator receives a
   `projection=None` compatibility result and cannot claim STORE_HOME success.
15. Separate `evaluate(...).verified` and `verified(...)` calls return the same
   boolean for the same inputs without mutable last-result state. Within one
   coordinator terminal transaction exactly one API is called exactly once;
   the coordinator never calls both.
16. Resending the same accepted result cannot bypass the existing terminal CAS
   or publish a second response.
17. UI retries cannot repeat output or TTS delivery.
18. GET verified evidence, crafting compatibility, the legacy STORE_HOME typed
   renderer, and ordinary non-STOP START plus TERMINAL behavior are unchanged.
19. Ordinary malformed dictionaries and wrong-shaped canonical data make
   `evaluate()` return `verified=False, projection=None` without raising; a
   wrong result-object type follows the same rule.
20. An injected evidence-evaluator `Exception` after terminal claim produces
   one cautious fact and bounded diagnostic, spends the claim once, and causes
   no retry, resubmission, success wording, or response loss.
21. A legacy verified-only collaborator returning `1`, a truthy object, or any
   non-exact boolean fails closed; it is not coerced to verified success.
22. Failure of the bounded evidence-failure reporter still stages the cautious
   fact exactly once and does not escape into correlation or transport logic.

## 9. Verification order

After source implementation, verification proceeds in this order:

1. focused STOP routing, presentation, tracker, and terminal tests;
2. focused STORE_HOME typed payload, descriptor, evaluator, projection, and
   renderer tests;
3. general lifecycle registry and all-profile contract tests;
4. Gradio real postprocess/preprocess round-trip regression tests;
5. Minecraft Python focused suite;
6. full Python regression appropriate to the changed integration;
7. read-only diff and log-field review.

No Java source changes are specified, so a Java build is not proof of either
fix and is not required by this Python-only contract. A live-browser and
Minecraft runtime retest remains a separate explicitly authorized step. In
that retest the acceptance criteria are:

```text
TRUSTED_STOP_VISIBLE_CARD_COUNT_PER_REQUEST: 1 TERMINAL_ONLY
TRUSTED_STOP_TTS_UTTERANCE_COUNT_PER_REQUEST: 1 TERMINAL_ONLY
STORE_HOME_SUCCESS_CARD_COUNT_PER_REQUEST: 1
STORE_HOME_SUCCESS_TEXT_FOR_REPRODUCED_COUNT_909: 아이템 909개를 집에 정리했어
DISTINCT_STOP_CONTROL_REQUEST_ID_COUNT_PER_REQUEST: 1
STOP_COMMAND_INVOCATION_COMMIT_COUNT_PER_REQUEST: 1
STORE_HOME_MATCHING_TERMINAL_SEND_ATTEMPT_COUNT_PER_REQUEST: 1
TRUSTED_STOP_GRADIO_SUBMIT_COMPLETION: PASS_WITHOUT_VISIBLE_START
TRUSTED_STOP_TEXTBOX_REENABLED: TRUE
TRUSTED_STOP_IMMEDIATE_NEXT_CHAT_SUBMIT: ACCEPTED
```

## 10. Rollback

STOP rollback restores only the accepted START publication policy in the
STOP route outcome builder and its tests. It must not roll back the STOP
tracker, barrier, correlation, terminal response, or general UI identity.

The terminal-before-input composition-order hunk is a separate
behavior-preserving hardening unit and may remain when the visible STOP policy
is reverted. If that ordering hunk itself is reverted, only the call order and
its focused composition test are restored; listener, router, and admission
implementations remain untouched.

STORE_HOME rollback first downgrades the trusted-translation registry profile
to its prior `cautious`/`cautious_terminal` state. Only after that fail-closed
registry change may it detach the STORE_HOME evaluator/projection and renderer
delegation and restore their focused assertions. It must not remove the typed
payload validator, change Java STORE_HOME results, or alter GET verification.

The GET evaluator extraction is a separate behavior-preserving structural
rollback unit. Reverting the STORE_HOME rollout may leave that extraction and
the compatible public evaluator facade in place; GET behavior and API remain
unchanged. The additive `CommandTerminalEvidenceEvaluation` and optional
`CommandTerminalFact.evidence_projection` are shared compatibility substrate.
They may remain inert after a STORE_HOME rollback and may be removed only as a
separate structural rollback after all consumers are proven absent.

Evidence-evaluator exception containment and its bounded reporter form a
separate common hardening rollback unit. They may remain after STORE_HOME is
downgraded. Reverting them must not be bundled into the STORE_HOME profile
rollback and requires proof that no terminal path can lose its cautious fact.

Because the two changes have separate causes and owners, one may be reverted
without reverting the other.

## 11. 2026-09-08 Python implementation follow-up

The requested Python-only implementation was applied in the dirty worktree
without changing Java, the wire schema, Minecraft behavior, command admission,
or dependency versions.

### 11.1 STOP terminal-only implementation

The accepted STOP route now retains its accepted submission result and typed
route identity while returning an empty START response. The existing trusted
response authorizer converts that result to `suppress_response=True`, so the
START boundary creates no direct Chat UI card, output-listener response, TTS
item, asynchronous UI presentation, or response-emission capability.

The following ownership remains unchanged:

```text
STOP tracker
command barrier
session/generation/request/message correlation
interrupted-command terminal arbitration
terminal validation
terminal compare-and-set claim
one verified "멈췄어" publication
```

Production composition was ordered so the STOP terminal callback is connected
before output/TTS listeners and receipts, which in turn are connected before
the Minecraft input router and before Chat/final-microphone ingress becomes
reachable. This is construction-order hardening only; it does not broaden
command admission.

### 11.2 STORE_HOME verified-evidence implementation

The generalized terminal lifecycle now evaluates evidence once after result
correlation and stores an optional immutable projection on the terminal fact.
The public evaluator keeps its compatible `verified()` method and delegates to
the additive `evaluate()` result. GET validation was extracted behind that
facade without changing its behavior, and the production crafting verifier
received the same compatibility shape.

The trusted-translation STORE_HOME profile now renders a positive sentence
only after the complete strong-evidence contract in Section 5 passes. The
verified immutable projection carries the validated stored-item count:

```text
stored_items > 0  -> 아이템 <count>개를 집에 정리했어
stored_items == 0 -> 집에 정리할 아이템이 없었어
```

Malformed or insufficient evidence remains cautious. Correlation mismatch
still creates no fact. Unexpected evaluator exceptions are reduced to one
cautious fact and one bounded diagnostic without retry, LLM recall, command
resubmission, or duplicate terminal-claim consumption.

Applied responsibility boundaries:

```text
transport/command_feedback/lifecycle/evidence/
  command_terminal_evidence_evaluation.py
  command_terminal_evidence_evaluator.py
  get/
  store_home/

transport/command_feedback/lifecycle/result/
  command_feedback_result_coordinator.py
  diagnostics/

transport/command_feedback/lifecycle/terminal/
  command_terminal_fact.py

response/command_lifecycle/terminal/store_home/
  korean_store_home_terminal_renderer.py

app_core/composition_core/component_wiring/
  STOP terminal, output, TTS, receipt, router, and ingress ordering
```

### 11.3 Offline verification result

The implementation follow-up recorded:

```text
focused final selection:          251 passed
Minecraft Python selection:       1060 passed, 2 skipped, 7010 subtests
Ruff for touched Python files:     PASS
git diff --check:                 PASS; line-ending warnings only
Java build:                       NOT_REQUIRED_NOT_RUN
deployment:                       NOT_RUN
Minecraft launch in that phase:   NOT_RUN
```

The broad repository run retained two separately classified limitations: an
existing HEAD-relative Java source-hash expectation in the dirty worktree and
a sandbox-only SQLite named-pipe permission failure. The named-pipe case passed
when isolated outside that sandbox boundary. Neither is acceptance evidence
for the STOP or STORE_HOME feature, and neither changes their rollback units.

## 12. Live STOP follow-up and newly discovered Gradio defect

A subsequent user-run runtime provided the first live evidence for the new
accepted-STOP publication policy:

```text
visible accepted START cards: 0
visible verified terminal cards: 1
terminal text: 멈췄어
terminal output deliveries: 1
terminal UI enqueues: 1
terminal TTS enqueues/playbacks: 1
STOP tracker/barrier/correlation: PASS
Java command retirement: PASS
ordinary owner gate after STOP: OPEN
Chat textbox recovery: FAIL
```

The live STOP result therefore passes the requested visible single-response
policy but does not pass the expanded Gradio-submit acceptance criteria added
to Section 9.

The failure is not a reason to restore `멈출게`. The accepted route correctly
suppresses the product response, but that suppression currently reaches
Gradio 6.18.0 as a synchronous generator with zero chunks. The installed
`ChatInterface._stream_fn()` converts the empty iterator's termination to
`StopAsyncIteration` and catches only `StopIteration`; the same environment
reproduces `RuntimeError: async generator raised StopAsyncIteration` without
Minecraft.

The authoritative incident analysis, remaining production-stderr observation
gap, Python-only correction boundary, and regression matrix are recorded in:

- [Gradio Suppressed-Response Input Lock Investigation](chatclef-gradio-suppressed-response-input-lock-investigation-2026-09-08.md)

The correction is now implemented as a third, separate rollback unit at the
LAVI-owned local Chat/Gradio completion boundary. The new focused adapter
passes ordinary chunks through unchanged and emits one fresh `[]` only after a
normally exhausted zero-chunk product stream. Real Gradio 6.18.0 `_stream_fn`,
Chatbot JSON round-trip, queue iterator completion, and immediate second-submit
tests pass without an assistant card. `None` and `""` were rejected by
characterization because they respectively raise during normalization or retain
an empty assistant message.

The follow-up verification recorded 11 focused adapter/Gradio passes,
`llm_core` at 110 passes plus 21 subtests, `app_core` at 9 passes, and the
Minecraft Python suite at 1060 passes, 2 skips, and 7010 subtests. Java build,
deployment, Minecraft launch, and the post-fix live-browser smoke remain
`NOT_RUN` or `NOT_REQUIRED` as applicable.

This rollback unit preserves the STOP terminal-only policy and is not bundled
with STORE_HOME evidence evaluation, asynchronous presentation identity, Java,
dependency versions, or Minecraft behavior.
