<!-- 20260908_kpopmodder: Defined the Python-only contextual active-command busy-response follow-up before source changes. -->
<!-- 20260908_kpopmodder: Kept command rejection, active ownership, STOP precedence, and Java behavior unchanged. -->
<!-- 20260908_kpopmodder: Required explicit questions and conflicting commands to share one verified active-status projection contract and renderer. -->
<!-- 20260909_kpopmodder: Reconciled the implemented Python-only policy and offline verification without promoting live runtime evidence. -->

# ChatClef Python Contextual Active-Command Busy Response Pre-Change Contract

Date: 2026-09-08

## 1. Document status

This document records the historical pre-change contract for one separately
requested presentation follow-up to the implemented contextual STATUS-query
work. A later explicit request authorized the Python implementation, which is
now implemented and verified offline. Reading or editing this document alone
still does not authorize runtime launch, commit, or push.

```text
DOCUMENT_TYPE: PRE_CHANGE_IMPLEMENTATION_CONTRACT
STATUS: IMPLEMENTED_VERIFIED_OFFLINE_LIVE_NOT_RUN
REVIEWED_REPOSITORY_ROOT: C:\Vtuber_Souorce_Code\LAVI
REVIEWED_BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
REVIEWED_HEAD: d8426f4b703755fa1f62a946b281db61367dbc60

BACKEND_SCOPE: FABRIC_CHATCLEF_PYTHON_ONLY
POLICY_SCOPE: TRUSTED_TYPED_PRE_SUBMIT_COMMAND_BUSY_PRESENTATION
STATUS_CONTRACT_RESERVED_ROLLBACK_SLOT: RB6

CURRENT_BUSY_ADMISSION_REASON: minecraft_command_busy
CURRENT_VISIBLE_BUSY_TEXT: [Minecraft] 다른 마인크래프트 명령을 실행 중이라 지금은 새 명령을 보낼 수 없어요.
TARGET_VISIBLE_BUSY_TEXT: SAME_TEXT_AS_VERIFIED_ACTIVE_COMMAND_STATUS

ATTEMPTED_SECOND_COMMAND_ADMISSION: REJECTED
ATTEMPTED_SECOND_COMMAND_SUBMISSIONS: ZERO
ATTEMPTED_SECOND_COMMAND_QUEUEING: NONE
ATTEMPTED_SECOND_COMMAND_RETRY_OR_REPLAY: NONE
ACTIVE_COMMAND_IDENTITY_REPLACEMENT: NONE
ACTIVE_COMMAND_CANCELLATION: NONE

STOP_PRECEDENCE: UNCHANGED_FIRST
TRUSTED_STOP_PRESENTATION: TERMINAL_ONLY
RAW_OR_LEGACY_BEHAVIOR_CHANGE: NONE
WIRE_SCHEMA_CHANGE: NONE
JAVA_SOURCE_CHANGE: NONE
MINECRAFT_TASK_BEHAVIOR_CHANGE: NONE

EXISTING_STATUS_QUERY_OFFLINE_VERIFICATION: PASSED
EXISTING_LAVI_CHAT_SINGLE_RUNTIME_ROUND: PASSED_PUMPKIN_PIE_STATUS_UI_TTS_STOP
FULL_CHAT_AND_FINAL_MIC_LIVE_GATE: NOT_RUN
NEW_CONTEXTUAL_BUSY_POLICY_RUNTIME: NOT_RUN
NEW_CONTEXTUAL_BUSY_POLICY_PYTHON_SOURCE_CHANGE: IMPLEMENTED_PYTHON_ONLY
NEW_CONTEXTUAL_BUSY_POLICY_PYTHON_TESTS: PASS_MINECRAFT_1229_SKIP_2_PLUS_14003_SUBTESTS
NEW_CONTEXTUAL_BUSY_POLICY_LLM_TTS_TESTS: PASS_159_PLUS_61_SUBTESTS
NEW_CONTEXTUAL_BUSY_POLICY_FULL_SELECTED_REGRESSION: PASS_2530_SKIP_4_DESELECT_1_PLUS_15087_SUBTESTS
FULL_REGRESSION_DESELECTED_TEST: PREEXISTING_JAVA_HEAD_HASH_FIXTURE_DRIFT
SANDBOX_ONLY_SQLITE_FAILURE_RETEST: PASS_OUTSIDE_SANDBOX
NEW_CONTEXTUAL_BUSY_POLICY_RUFF: PASSED
NEW_CONTEXTUAL_BUSY_POLICY_GRADLE_BUILD: NOT_REQUIRED_NOT_RUN
IMPLEMENTATION_WORKTREE: UNCOMMITTED
CURRENT_IMPLEMENTATION_TASK_COMMIT_OR_PUSH: NOT_PERFORMED
```

## 2. User-visible decision

While one supported Minecraft command is active, these two inputs must produce
the same current-task sentence when their separately frozen inspections agree
on the exact active identity, immutable descriptor, phrase profile, and
response-driving lifecycle state:

```text
active command A: get diamond_pickaxe 1
active lifecycle state: running

user: 지금 뭐 해?
assistant: 다이아 곡괭이 만드는 중이야

user: 호박 파이 만들어줘
assistant: 다이아 곡괭이 만드는 중이야
```

The second input is still an imperative command request. It is not relabelled
as a STATUS question. In this example it reaches the existing ordinary
translation and busy admission boundary, is rejected there, and submits
nothing. Equivalent exact typed pre-submit busy results from the existing
generic-crafting-defaults and trusted H5 owners are covered at the shared
trusted-response boundary. Only the response projection changes.

The attempted `호박 파이` command must not:

- replace the active diamond-pickaxe descriptor;
- acquire a Java request identity;
- enter a Python or Java command queue;
- cancel or preempt the active command;
- be retried after the active command finishes;
- produce START or TERMINAL lifecycle output;
- trigger any additional LLM call, route retry, retranslation, or paraphrase
  after the existing route has produced the typed busy result. The ordinary or
  generic route's existing one-pass translation before its busy precheck remains
  unchanged.

After the active command completes or is explicitly stopped, a later user
command is evaluated normally as a new input.

## 3. Relation to the implemented STATUS contract

The implemented
[Python Contextual Active-Command Status Response Pre-Change Contract](chatclef-python-contextual-active-command-status-response-pre-change-contract-2026-09-08.md)
owns question classification, exact contextual claim, STATUS publication
custody, and natural progress rendering. Its Section 4.4 intentionally retained
the generic response for a genuine new command, and its rollback table reserved
`RB6` for a separately requested and documented context-rich busy policy.

This document owns that separately documented `RB6` contract.

It narrowly changes the presentation after an existing exact typed pre-submit
command-busy rejection. It does not broaden what counts as a STATUS question
and does not change the original STATUS route's source, grammar, correlation,
permit, or acknowledgement contract.

The shared-text requirement means:

```text
separately frozen inspections
+ exact same active identity
+ equivalent immutable descriptor and command phrase profile
+ same response-driving lifecycle state
-> byte-identical Korean response_text
```

The input event identity and route metadata remain different because one input
is a read-only STATUS question and the other is a rejected imperative command.
Their snapshots, permits, and acknowledgements also remain independently owned
per input. Text equality must not erase provenance or imply shared object
identity. If A changes from pending (accepted but not yet proven running) to
running, reaches a later progress state, or becomes terminal between the two
inspections, the parity precondition no longer holds and different state-correct
wording is not a parity failure.

## 4. Exact scope

### 4.1 Eligible input

The contextual busy response is eligible only when all of these are true:

1. the input is a final, one-shot trusted event;
2. its source is LAVI Chat or final microphone input;
3. Korean eligibility and the existing source proof are valid;
4. the STOP owner has declined the input and the STATUS classifier has found it
   lexically unrelated, so the existing command-owner sequence may continue;
5. an existing trusted route owner reaches the common submission precheck under
   its current ordinary, generic-crafting-defaults, or H5 ordering;
6. that owner returns the exact pre-submit typed rejection with `handled=True`,
   `reason=minecraft_command_busy`, canonical `result.ok is False`, and
   `result.error=active_command`;
7. the busy decision's status has yielded one immutable observation of the
   active session, connection generation, request identity, and command-message
   identity;
8. one command-lock inspection proves that observation still agrees with the
   current connection, active websocket and command owner, lifecycle context,
   descriptor, admission grant, and active owner token;
9. the immutable active descriptor passes the existing status descriptor
   eligibility validation;
10. the active lifecycle state has evidence sufficient for the existing
    deterministic STATUS renderer;
11. that same locked inspection issues exactly one raw lifecycle STATUS
    publication permit, and the existing post-lock STATUS handoff then creates
    and attaches its exact acknowledgement;
12. later, the existing trusted authorizer issues at most one current-input
    response-emission capability from the same still-live input proof.

The attempted command may be different from or duplicate the active command.
Both are typed busy conflicts and remain zero-submit. The response always
describes the actual active command, never the rejected attempted command.

A post-submit transport rejection such as `invalid_request` or an
`already active` message is not this fact. Implementations must not inspect
free-form error text or reclassify post-submit failures as contextual busy.

Ordinary and generic-crafting-defaults routes have completed their existing
translation/validation before this busy result. H5 is intentionally different:
its claimed route runs the common submission precheck before translation and
translation admission. Therefore an H5 exact typed busy result proves only that
the already claimed candidate was blocked; it does not prove that attempted B
would later translate or validate. The contextual decorator must not invoke H5
translation to fill that gap and must never mention B's target or validity.

### 4.2 Command and item coverage

“Every command and every item” has the same bounded meaning as the implemented
general lifecycle layer:

- every queryable ordinary command-lifecycle profile currently registered whose
  existing accepted ingress or route freezes an eligible active descriptor;
- every item or target already accepted by that command's existing resolver;
- future profiles only after they provide the same descriptor and phrase
  contract.

This universal phrase coverage describes active command A. Attempted command B
only needs the exact typed busy fact from its existing route order because the
response makes no claim that B is valid or accepted; B remains blocked and is
re-evaluated normally only if the user submits it again after A becomes
terminal.

It does not mean unknown DSL, unsupported aliases, arbitrary native ChatClef
commands, or commands lacking an eligible lifecycle profile. Trusted STOP is a
special control lane and remains terminal-only.

No target-specific `diamond_pickaxe`, `iron_pickaxe`, `pumpkin_pie`, or other
item branch may be added. The active descriptor and shared profile renderer are
the only phrase authority.

### 4.3 Sources

| Source | New contextual busy presentation |
| --- | --- |
| Exact `(lavi_chat_ui, lavi_chat_ui, chat_submit, final=True)` B event with its matching unspent proof | Eligible with matching active evidence |
| Exact `(voice_input_final, VoiceInput, final_transcript, final=True)` B event with its matching unspent proof | Eligible with matching active evidence |
| Partial/interim microphone transcript | Ineligible; existing source policy remains unchanged |
| Butler whisper or background automation | Ineligible; existing source policy remains unchanged |
| ScreenVision or OCR observation | Ineligible; existing source policy remains unchanged |
| Untrusted or lookalike event object | Ineligible; no new response authority |
| Valid-looking event with foreign, cross-event, cross-owner, or spent proof | Ineligible; no new response authority |
| Raw/legacy GUI or direct ChatClef input | Unchanged; decorator is not entered |
| Native in-game ChatClef command | Unchanged; no Python current-input response identity |
| Internal/system event | Ineligible; any future source requires a separate contract |

Only attempted command B must use one of the two exact trusted source tuples.
Active command A may come from any existing accepted ingress that produced a
descriptor eligible for the explicit STATUS contract, including a typed or
bounded command-name-only descriptor. Display text alone never establishes
cross-source correlation.

## 5. Route precedence and command authority

The required order is:

```text
trusted final input
  -> STOP owner
     -> exact STOP: existing stop_control terminal-only path
  -> explicit STATUS-question owner
     -> exact claim/addressed result: existing STATUS response path
     -> validated prefixless candidate without a claim: conversation bypass;
        skip every later Minecraft-command owner
     -> lexical non-STATUS only: continue
  -> remaining command-owner dispatch in the existing order
     -> generic-crafting-defaults where applicable
     -> Minecraft intent gate
     -> trusted H5 where applicable
     -> ordinary translation/submission otherwise
  -> route owner returns its validated route decision
     -> exact typed pre-submit minecraft_command_busy:
          centralized trusted-response contextual-busy coordinator/decorator
          attempted command submission = 0
          active identity unchanged
          freeze the observed busy identity
          try one locked STATUS inspection and raw permit
             -> pre-permit unavailable/failure: existing generic busy response
             -> permit issued: existing post-lock acknowledgement handoff
                -> success: current-task natural response
                -> handoff failure: fail permit false and suppress
     -> every other decision: unchanged
```

The admission fact remains:

```text
reason=minecraft_command_busy
ok=false
error=active_command
```

The feature must not convert that fact into success. It changes only the
trusted user-facing response attached to the rejected decision. The decorator
runs in `TrustedKoreanInputRouteCoordinator.route()` immediately after
`route_invoker.route_trusted(...)`, and before both
`status_publication_custody_guard.claim(decision)` and
`feedback_renderer.render(decision)`. This allows the existing custody guard to
take the newly attached STATUS acknowledgement before any static busy renderer
can replace the text. It is not a new route owner and must not alter STOP,
STATUS, generic-crafting, H5, or ordinary dispatch precedence.

## 6. Truth and evidence contract

The busy result alone proves only that a command owner blocked admission. It is
not sufficient evidence for a sentence ending in `…중이야`.

Strong current-task wording must come from the same lifecycle truth used by the
implemented STATUS renderer:

- one exact active lifecycle context;
- an immutable eligible command descriptor;
- exact active owner-token agreement;
- matching session and connection generation;
- nonterminal state;
- accepted lifecycle evidence for the existing pending phrase, or running
  lifecycle evidence with a profile-approved result reason for `…중이야`;
- no active admission quarantine;
- no already-claimed terminal state.

The exact busy decision is a time-of-check observation, not a permanent claim
on whichever command happens to be active later. Immediately after
`route_trusted()` returns at the shared trusted-response boundary, and before
custody claim or feedback rendering, a dedicated immutable
`CommandBusyObservedIdentity` must extract the precheck-observed values:

```text
active_session_id
active_generation
active_request_id
active_command_message_id
```

These values come only from the canonical
`decision.result.status.details.commands` mapping with exact bounded field
types. The free-form `message`, attempted translation, and
`active_command_reconciliation` diagnostic are not identity authority. A
missing, blank, boolean-as-integer, malformed, or oversized value makes the
observation ineligible without raising.

Under one existing command-lock interval, the busy-status inspector then
revalidates all of the following against that observation:

- current connection session, generation, and active websocket;
- current active command object's websocket, session, generation, request, and
  command-message identity;
- lifecycle context owner-token object identity;
- lifecycle context session, generation, request, and message identity;
- the same frozen descriptor and admission-grant identity;
- connected, non-quarantined, nonterminal lifecycle evidence.

Only that same lock interval may select the existing immutable
`CommandFeedbackLifecycleSnapshot` and issue its one raw STATUS publication
permit. It must not create or attach the acknowledgement inside the lock. The
existing `CommandStatusPublicationHandoff` performs that attachment after the
lock is released with `query=None` because this input is not a STATUS question,
and owns raw-permit cleanup if attachment fails. Its exact typed
`CommandStatusPublicationHandoffFailure` result is converted only to the
contextual-busy suppressed decision; it is not reinterpreted as a pre-permit
generic-busy fallback.
If A ends and C becomes active between the original busy observation and this
inspection, the identities do not match: the response may fall back to the
generic busy sentence, but it must never describe C as though C caused B's
earlier rejection.

The attempted command's validated route result may establish that the input is
a genuine supported command, but it must never supply the target label, verb,
quantity, or state used to describe the active work.

Example:

```text
active descriptor: get diamond_pickaxe 1
attempted command: get pumpkin_pie 1

valid response subject: 다이아 곡괭이
invalid response subject: 호박 파이
```

Before a STATUS permit is issued, malformed, stale, unavailable, mismatched,
terminal, or unsupported active evidence returns the original nonempty generic
busy decision unchanged. It therefore preserves exactly one generic response
candidate; only a later existing authorizer or publisher failure may reduce
actual delivery to zero. If the raw permit is issued but the existing post-lock
handoff cannot create its diagnostic custody/acknowledgement or attach the
acknowledgement to the snapshot, that handoff resolves the raw permit false and
the contextual path suppresses; there is no acknowledgement on which to carry a
visible fallback. After an acknowledgement is attached, its custody contract
governs failure. A renderer, presentation-detail projector, or decision-
assembly failure may produce only the fixed cautious STATUS sentence `지금
마인크래프트 작업 상태를 확인하지 못했어` under that exact acknowledgement
and the same
`(command_busy_current_work, command_status, current_input)` identity. If that
same-acknowledgement fallback cannot be constructed or preserved, custody
resolves the acknowledgement false, suppresses the failed current-task product,
and releases any staged terminal without replay. The system must not guess a
task from raw command text, the attempted translation, Java logs, prior UI
cards, or an LLM.

## 7. Shared response projection

The explicit STATUS route and contextual busy path must converge on the
existing immutable `CommandFeedbackLifecycleSnapshot`, frozen descriptor, and
deterministic phrase renderer. No second projection may duplicate lifecycle or
descriptor fields.

The response text must not be copied, reimplemented, or special-cased in a new
busy renderer. After eligibility succeeds, the coordinator calls the existing
`CommandLifecycleResponseRenderer.render_status(snapshot, query=None)` and
reuses the existing presentation-detail projector.

Illustrative results are:

| Active descriptor | Running response |
| --- | --- |
| `get diamond_pickaxe 1` | `다이아 곡괭이 만드는 중이야` |
| `get pumpkin_pie 1` | `호박 파이 만드는 중이야` |
| another eligible item command | Existing profile-owned target phrase |
| another eligible non-item command | Existing profile-owned action phrase |

The table does not create new hard-coded response cases. The current registry
and renderer remain authoritative for exact wording.

For observability, the response should preserve distinct provenance such as:

```text
reason=minecraft_command_busy
route_kind=command_busy_current_work
response_kind=command_status
delivery_mode=current_input
```

The exact visible `response_text` is nevertheless identical to the explicit
STATUS response produced from a separately frozen projection with equivalent
active identity, descriptor, phrase profile, and response-driving lifecycle
state.

## 8. Publication, UI, and TTS

A healthy eligible conflicting-command path produces exactly one user-facing
product:

```text
response facts: 1
assistant cards: 1
output listener deliveries: 1
TTS queue admissions: 1 when TTS is enabled
TTS completed playbacks: 1 when playback succeeds
new command submissions: 0
```

Across success and every failure path, each sink is at-most-once. A failure
after STATUS permit issuance may intentionally produce zero current-task
products; it must never fabricate or replay one merely to satisfy an exact
count.

The implementation must reuse the existing synchronized lifecycle snapshot and
STATUS publication permit, acknowledgement, failure custody, and terminal FIFO.
It must not read mutable command state from `render_busy()` or create a parallel
publication mechanism. Output-delivered acknowledgement resolves true exactly
once; a failed publication resolves false exactly once and releases any staged
terminal through the existing lifecycle owner.

The publication authorities and handoffs remain distinct:

1. the locked busy-status inspector issues one raw lifecycle STATUS permit;
2. the existing post-lock `CommandStatusPublicationHandoff` creates the exact
   acknowledgement and attaches it to the snapshot, or resolves the raw permit
   false on failure;
3. the contextual coordinator renders and decorates while its local handoff
   guard owns that acknowledgement, then the guard transfers ownership only to
   the exact custody returned by the existing outer STATUS custody claim;
4. after outer custody has claimed that acknowledgement and feedback rendering has
   preserved the text, the existing trusted authorizer may issue one
   current-input response-emission capability from the live proof.

The contextual-busy decorator neither consumes the proof nor creates the
response-emission capability. It must preserve the original
`reason=minecraft_command_busy`, canonical `result.ok is False`,
`result.error=active_command`, and exact acknowledgement object while changing
only the permitted response and route metadata.

From the instant the existing post-lock handoff attaches an acknowledgement
until the decorated decision is accepted by the outer
`CommandStatusPublicationCustodyGuard`, the contextual handoff guard owns that
exact acknowledgement. This requires an explicit two-phase seam in
`TrustedKoreanInputRouteCoordinator`: first prepare the contextual decision,
then conditionally call a narrow `claim_and_transfer(...)` operation that wraps
the existing outer `claim` callback. The transfer succeeds only when the outer
claim returns custody carrying the exact acknowledgement object and preserved
route identity. A render/decorate/return/outer-claim exception, `None` custody,
or identity mismatch resolves the acknowledgement false exactly once and
returns the dedicated immutable contextual-busy suppressed decision defined
below. It must not return the existing STATUS emergency singleton, whose reason
and route identity describe a different failure contract. Successful transfer
performs no acknowledgement call. A non-contextual decision has no local
handoff token and continues through the existing direct outer claim unchanged.

The dedicated suppressed decision is prebuilt before runtime failure injection
and has this closed shape:

```text
handled=true
reason=minecraft_command_busy
response_text=""
result={ok: false, error: active_command}  # deeply immutable canonical minimum
translation={}
publish_external_response=false
response_source=minecraft_chatclef
response_emission_capability=None
suppress_response=true
route_kind=command_busy_current_work
response_kind=command_status
response_publication_acknowledgement=None
presentation_detail_log=""
```

It preserves the canonical typed admission fact without retaining mutable
status details or correlation identity. The call-local original route decision
remains the sole input to feature-admission diagnostics. The route selector must
preserve this sentinel's empty text by checking its route kind before the
reason-based generic BUSY renderer, and the authorizer must therefore leave it
suppressed with no capability.

This contextual singleton owns only a typed raw-handoff failure or an
acknowledgement-attached failure before successful outer-custody transfer. Once
transfer succeeds, the existing `CommandStatusPublicationCustodyGuard` retains
its current general STATUS emergency decision for trusted feedback,
authorization, proof-close, and identity failures; the outer dispatcher retains
its existing `RoutedInputDispatchOutcome.suppressed()` failure result. Those
post-transfer outcomes do not expose the canonical busy fields, but they do not
alter the original rejected admission, which the call-local feature-admission
decision has already preserved for logging. Do not add route-aware fallback
selection to those existing generic guards for this feature.

Six existing closed boundaries require explicit narrow wiring:

- `TrustedKoreanFeedbackRouteSelector` recognizes
  `route_kind=command_busy_current_work` before its reason-based BUSY mapping,
  so the already rendered current-task text is preserved;
- `CommandStatusPublicationCustodyPolicy._ROUTE_KINDS` recognizes that exact
  route kind, so the existing guard immediately owns its STATUS
  acknowledgement;
- `RoutedInputExternalResponsePublisher` includes that exact route kind in its
  Minecraft presentation-metadata allowlist;
- `CommandFeedbackDeliverySchema.ROUTE_KINDS` and
  `ROUTE_RESPONSE_DELIVERY_IDENTITIES` recognize the exact
  `(command_busy_current_work, command_status, current_input)` identity so every
  sink diagnostic remains canonical rather than being normalized to `invalid`;
- `TtsLifecycleResponseIdentityClassifier._ALLOWED_IDENTITIES` recognizes that
  same exact tuple so the otherwise valid current-task response is not discarded
  as unrelated before TTS queue admission;
- the trusted route coordinator retains a call-local reference to the original
  route decision before contextual decoration and supplies that original only
  to `log_feature_admission()`. This preserves ordinary/H5 scope A and generic-
  defaults scope B in `MinecraftKoreanFeatureIdentityClassifier` without
  misclassifying the new presentation route as `none` or flattening every origin
  to scope A. The decorated decision remains authoritative for response
  delivery. Do not add `command_busy_current_work` as a blanket scope-A alias.

The healthy `command_busy_current_work` response must use the existing Minecraft
presentation badge. On that positive route, the literal `[Minecraft]` is badge
metadata only and must not occur in the bare STATUS response body or TTS text.
A pre-permit ineligible evaluation instead returns the existing generic busy
decision byte-for-byte, including any prefix already owned by that legacy
response. A healthy positive path must not also emit the old generic busy
sentence. Gradio serialization or UI retry must not replay output or TTS.

No hidden Gradio no-op is needed on the healthy visible path. If post-permit
custody, authorization, or dispatcher failure intentionally suppresses the
current-task product, the existing zero-stream completion adapter may terminate
the Gradio submit without creating an assistant card, output, or TTS. It remains
a transport completion signal, not a fallback response.

## 9. Race and exception behavior

### 9.1 Non-STOP active-command terminal race

The active-status projection and its publication ownership must be selected
under the existing command lifecycle synchronization boundary.

```text
contextual-busy publication claim wins first
  -> terminal is staged behind that exact STATUS permit
  -> acknowledgement resolves published=true or published=false once
  -> staged terminal is then released exactly once by the lifecycle owner

terminal wins first
  -> no late STATUS permit and no current-task sentence
  -> return the original nonempty generic busy response candidate unchanged;
     later existing delivery remains at most once
```

The attempted command remains rejected in both cases and is never automatically
retried after terminal delivery.

### 9.2 STOP race

STOP remains the first route owner. An exact trusted STOP must never be rendered
as a contextual-busy response. The existing tracker, stop barrier, correlation,
terminal validator, terminal CAS, `멈췄어` single publication, and Gradio
completion behavior remain unchanged.

If a separate STOP input arrives while the contextual-busy response's STATUS
publication permit is pending, STOP still wins its own input route and emits no
START product. When the matching STOP result passes its existing validator and
terminal CAS, exact original-owner retirement through
`clear_command_if_identity()` cancels/resets the pending command-feedback
publication lifecycle; the contextual-busy product is suppressed and cannot be
replayed. The STOP terminal publisher then emits `멈췄어` once through its
existing independent non-preempting path outside the command STATUS FIFO. Do not
model the STOP terminal as staged behind, released by, or sharing the contextual
STATUS acknowledgement. Neither input may replay the other input's UI, output,
or TTS product.

### 9.3 Exceptions

Malformed mappings, wrong field types, missing descriptors, and expected
identity/snapshot inspection failures return an ineligible evaluation rather
than raising.

An unexpected evaluator, identity-factory, inspector, renderer, presentation-
detail projector, decorator, or publication exception must:

1. preserve the zero-submit busy rejection;
2. before permit issuance, return the original nonempty generic busy decision
   unchanged as exactly one response candidate;
3. after raw permit issuance but before acknowledgement attachment, delegate to
   the existing STATUS handoff's fail-false cleanup and return the contextual-
   busy suppressed decision;
4. after acknowledgement attachment but before outer-custody transfer, keep
   that exact acknowledgement under local custody:
   use only `지금 마인크래프트 작업 상태를 확인하지 못했어` with the same
   `(command_busy_current_work, command_status, current_input)` identity when
   that fallback can retain the acknowledgement; otherwise resolve it false and
   return the contextual-busy suppressed decision;
5. after successful transfer, preserve the existing STATUS custody guard's
   general emergency decision and the dispatcher's existing suppressed outcome;
   do not introduce a route-aware post-transfer fallback;
6. release or fail any acquired publication acknowledgement exactly once;
7. record one bounded diagnostic;
8. never invoke an additional LLM/translation pass, retry routing, or resubmit
   the command after the typed busy result.

## 10. Responsibility and folder contract

This follow-up crosses more than one responsibility, so new LAVI-owned Python
code must be split by responsibility. Do not grow the existing precheck stage,
trusted route coordinator, trusted feedback facade, or status coordinator into
a second policy owner.

Proposed organization:

```text
plugins/Minecraft/fabric/chatclef/input/routing/trusted_korean/response/contextual_busy/
  __init__.py
  contextual_busy_response_evaluation.py
  contextual_busy_response_evaluator.py
  contextual_busy_response_preparation.py
  contextual_busy_response_coordinator.py
  contextual_busy_route_decision_decorator.py
  contextual_busy_suppressed_decision.py

plugins/Minecraft/fabric/chatclef/input/routing/trusted_korean/response/contextual_busy/publication/
  __init__.py
  contextual_busy_publication_handoff_guard.py

plugins/Minecraft/fabric/chatclef/input/routing/trusted_korean/response/contextual_busy/diagnostics/
  __init__.py
  contextual_busy_response_failure_record.py
  contextual_busy_response_failure_logger.py

plugins/Minecraft/fabric/chatclef/transport/command_feedback/lifecycle/status/busy/
  __init__.py
  command_busy_observed_identity.py
  command_busy_observed_identity_factory.py
  command_busy_status_inspector.py
```

Final names may be adjusted after source inspection, but the responsibility
boundaries are mandatory:

| Responsibility | Owner | Must not own |
| --- | --- | --- |
| Carry one immutable eligibility outcome and references to existing immutable facts | Contextual-busy response evaluation DTO | Lifecycle-field copies, parsing, rendering, mutation |
| Determine whether a route result is the exact eligible trusted typed busy conflict | Trusted-response contextual-busy evaluator | Route selection, lifecycle mutation, rendering, submission |
| Carry one prepared response decision plus an optional opaque local-handoff token without copying lifecycle facts | Contextual-busy response preparation DTO | Admission logging, outer custody claim, acknowledgement calls, rendering, mutation |
| Sequence eligibility, immutable busy observation, locked inspection, existing rendering, and decoration | Contextual-busy response coordinator | Route precedence, outer custody claim, command submission, inline grammar |
| Store only the already validated immutable busy identity | Command-busy observed-identity DTO | Mapping traversal, validation, lifecycle state, mutable owner objects |
| Extract and validate the exact identity seen by the busy precheck | Command-busy observed-identity factory | Input-route imports, lifecycle mutation, response text, fallback choice |
| Forward one typed atomic busy-status inspection from extension to the server API | Existing extension/facade/adapter/runtime compatibility seams | Private-field access, two independent status reads, rendering, admission policy |
| Revalidate that observation and issue one existing snapshot/raw STATUS permit under the command lock, then delegate acknowledgement attachment to the existing post-lock handoff | Command-busy status inspector/server API facade | Duplicate projections, Korean grammar, attempted-command fields, acknowledgement construction inside the lock |
| Render the existing snapshot | Existing `CommandLifecycleResponseRenderer.render_status(snapshot, query=None)` | Busy-specific grammar or state reads |
| Decorate only the already rejected typed route decision | Contextual-busy route-decision decorator | Busy creation, command submission, route precedence, or active-owner replacement |
| Provide one deeply immutable fail-closed result retaining only the canonical typed-busy fact and suppressed response identity | Contextual-busy suppressed-decision module | Dynamic identities/status payload, acknowledgement, capability, response text |
| Retain the exact acknowledgement and opaque token until `claim_and_transfer(...)` verifies outer STATUS custody, or resolve it false once on failure | Contextual-busy publication handoff guard | Rendering, permit issuance, terminal publication, retry |
| Record bounded failure facts | Dedicated contextual-busy diagnostics | Recovery, behavior choice, raw input retention |
| Assemble collaborators | Existing component graph | New global mutable registry or singleton |

The existing `inspect_status()` public API remains compatible. One narrow typed
busy-inspection port is required. The proposed forwarding chain is
`MinecraftFabricChatClefExtension.inspect_command_feedback_busy_status()` →
`MinecraftFabricChatClefCommandFeedbackFacade.inspect_busy_status()` →
`FabricChatClefAdapter.inspect_command_feedback_busy_status()` →
`FabricChatClefWebSocketServer.inspect_command_feedback_busy_status()` →
`CommandFeedbackServerApi.inspect_busy_status(observed_identity)`, or an
equivalent explicitly named chain with the same ownership. Under the command
lock, its terminal method performs the identity reinspection and may only select
the existing immutable `CommandFeedbackLifecycleSnapshot` and raw STATUS permit.
After releasing the lock it must reuse `CommandStatusPublicationHandoff` to
attach the acknowledgement with `query=None`, preserving the existing STATUS
semantics rather than returning a copied projection or creating an
acknowledgement inside the lock. The input layer must not reach through adapter,
runtime, server, command-
owner, or tracker private fields, and must not emulate this contract with a
separate status read followed by a later permit call.

The transport-local observed-identity factory accepts only the canonical result
mapping supplied by the input-layer coordinator. It must not import the input
route-decision type or make a route-eligibility decision; dependencies remain
directed from trusted input orchestration toward the transport lifecycle API.

No new phrase renderer is created. The contextual coordinator directly reuses
`CommandLifecycleResponseRenderer.render_status(snapshot, query=None)` and the
existing presentation-detail projector. `TrustedKoreanInputRouteCoordinator`
receives only the focused preparation plus conditional `claim_and_transfer`
seam at the exact insertion point in Section 5; it does not absorb eligibility,
rendering, fallback, or acknowledgement policy. Its only additional diagnostic
duty is to capture the original route decision in one call-local variable before
preparation and pass it to the existing feature-admission logger; it does not
copy or reinterpret that decision.

## 11. Diagnostics

Diagnostic ownership is exclusive by lifecycle interval. One failure must never
be recorded once by the contextual logger and again by the existing STATUS
logger:

1. before a raw permit exists, decision validation, identity freeze, and atomic
   reinspection failures use the contextual schema below only;
2. from raw permit through acknowledgement/snapshot attachment, a typed
   `CommandStatusPublicationHandoffFailure` uses the existing STATUS handoff
   custody or fallback observer only; the contextual logger must not re-record
   it;
3. after acknowledgement attachment but before successful outer-custody
   transfer, renderer/projector/decision/fallback failures use the diagnostic
   custody already carried by that acknowledgement;
4. after transfer, trusted feedback, authorization, proof-close, dispatcher,
   and publication failures continue through that same existing STATUS custody.

The existing STATUS failure-stage allowlists must add only the new
`contextual_busy_custody_handoff` stage needed for a thrown, absent, or identity-
mismatched outer claim. Existing `status_rendering`,
`presentation_detail_projection`, `primary_decision_assembly`, and
`fallback_decision_assembly` stages cover the other acknowledgement-bearing
pre-transfer failures. Their one `record_once()` custody then prevents a later
failure from producing a second record.

Only the pre-permit contextual interval uses this exact closed schema and
preserves only its first failure for one input:

```text
event=contextual_busy_response_failure
stage=<decision_validation|identity_freeze|locked_status_inspection>
busy_reason=minecraft_command_busy
active_command_name=<validated command profile name or none>
active_lifecycle_state=<running|pending|unavailable|none>
terminal_state=<none|unclaimed|claimed>
availability_reason=<none|no_tracked_owner|disconnected|quarantined|stale_owner|identity_mismatch|terminal_claimed|evidence_unavailable|unsupported_profile|malformed_observation>
exception_class=<ASCII class name of 1-96 characters or none>
selected_fallback=generic_busy
```

No field may contain raw user text, attempted or active correlation IDs, target
or quantity text, result/status payloads, exception messages, stack traces,
secrets, proof/capability objects, mutable descriptors, sockets, or tokens.
Unavailable values use the literal closed value `none`; they are not recovered
from raw payloads. The existing STATUS and TTS delivery schemas remain bounded;
only the one named STATUS stage value and the separately specified delivery
identity tuple are added. Feature-admission logging receives the original route
decision and must record `minecraft_command_busy` with its original scope/policy
classification even though response delivery uses `command_busy_current_work`.

Diagnostics observe the already selected outcome. They do not choose whether
to submit, release an owner, retry, cancel, or publish a response. Diagnostic
sink failure is swallowed only after the acknowledgement cleanup/fallback
decision is established, must not change that decision, and must not trigger a
compensating record through the other schema.

## 12. Test contract

### 12.1 Characterization first

Before changing behavior, freeze the current distinction:

- explicit `지금 뭐 해?` produces the active natural STATUS sentence;
- an ordinary or generic route's validated second command produces one generic
  busy sentence, while a claimed H5 candidate may reach the same typed precheck
  result before translation;
- both inputs submit zero additional commands while A is active;
- STOP stays first and terminal-only;
- disconnected, quarantine, unknown, and invalid translation responses retain
  their current wording.

### 12.2 Positive parity matrix

For every registered queryable ordinary lifecycle profile, exercise the finite
source-derived target matrix accepted by its existing ingress:

1. establish one exact active command A and hold its response-driving lifecycle
   state stable for the comparison;
2. obtain the explicit STATUS response for A from one frozen inspection;
3. send an eligible different command B and obtain its independently frozen
   busy-response inspection;
4. prove that the two inspections have different input-owned snapshot, permit,
   and acknowledgement objects but equivalent active identity, descriptor,
   phrase profile, and response-driving lifecycle state;
5. assert B receives exactly the same response text;
6. assert B submission count is zero;
7. assert A's session, generation, request, message, owner token, descriptor,
   START count, and terminal eligibility are unchanged.

Add a separate transition fixture in which A moves from pending (accepted but
not yet proven running) to running, or otherwise changes response-driving
lifecycle state, between the two inputs. It must produce the wording appropriate
to each independently observed state and must not be counted as a failed parity
comparison.

The positive decision retains `reason=minecraft_command_busy`, canonical
`ok=False/error=active_command`, one exact STATUS acknowledgement,
`route_kind=command_busy_current_work`, and
`response_kind=command_status`. When B differs from A, every B-only target and
quantity must be absent from the response body, speech text, and presentation-
detail log. When B duplicates A, lexical absence is not a valid oracle; use a
poisoned or instrumented attempted-command fixture to prove the projector and
renderer never read B's target/count fields and use only A's frozen projection.
The Minecraft badge remains presentation metadata only.

Include at least:

- item craft/acquisition examples such as `diamond_pickaxe` and `pumpkin_pie`;
- non-item command families;
- quantity-bearing descriptors;
- duplicate and different attempted commands;
- pending (accepted but not yet proven running) and running active states where
  their existing profile supports deterministic status wording.

The active-profile matrix covers the current 25 queryable ordinary profiles.
STOP remains present only in the 26-profile registry drift test and is never
admitted as a contextual active owner. Route-entry coverage separately includes
ordinary, generic-crafting-defaults, and trusted H5 inputs wherever they can
produce the same exact typed pre-submit busy decision.

Preserve the implemented STATUS target-domain boundary: exhaustively cover the
591 catalog targets for GET, DEPOSIT, DEPOSIT_ALL, and GIVE, plus every
`ChatClefEquipmentTargetComposer.all_targets()` EQUIP target. The unbounded
source-authorized raw domain outside that catalog receives representative
fallback and property coverage, not impossible enumeration. This bounds test
enumeration without narrowing the universal runtime promise for any target that
existing ingress already accepts and freezes into an eligible descriptor.

### 12.3 Negative matrix

Keep three categories distinct.

Exact typed busy was created, but evaluation or atomic reinspection fails before
permit issuance. These cases preserve zero mutation and return the original
nonempty generic busy response candidate unchanged:

- missing or malformed observed identity or active descriptor;
- unsupported profile;
- stale owner token or active websocket;
- session, generation, request, or message mismatch;
- A ends or C replaces A between busy observation and inspection;
- disconnect, quarantine, or terminal claim wins that race;
- lifecycle evidence is unavailable;
- evaluator, identity-factory, or inspector exception before permit issuance.

The original route never produced exact typed busy. These cases do not enter
the decorator and retain their own existing policy/wording, not a promised
generic-busy fallback:

- initially disconnected, disabled, quarantined, or malformed bridge state;
- an invalid or unsupported attempted command whose owner rejects it before
  producing exact typed busy;
- a translation or route-specific rejection reached before exact typed busy;
- partial microphone, Butler/background, ScreenVision/OCR, or internal input;
- untrusted/lookalike event;
- raw/legacy GUI, direct ChatClef, or native in-game command;
- post-submit `invalid_request` or `already active` rejection.

Proof tests must include a matching unspent proof plus foreign, cross-event,
cross-owner, spent, and valid-looking lookalike proofs. None of the negative
proof cases may receive new response authority or be recovered by matching
display text.

After acknowledgement attachment but before outer-custody transfer, renderer,
presentation-detail projection, decorator, fallback-construction, and outer-
claim failures must retain exact local custody. The only visible failure result
in that interval is one fixed cautious STATUS sentence, `지금 마인크래프트 작업
상태를 확인하지 못했어`, under the same acknowledgement and
route/response/delivery identity. An outer-claim failure or any pre-transfer
failure that cannot preserve the acknowledgement returns the exact contextual-
busy suppressed singleton, not the general STATUS emergency singleton. Assert
its deeply immutable canonical `reason/ok/error`, empty text, absent
acknowledgement/capability, exact response identity, and zero UI/output/TTS
products.

After successful transfer, injected acknowledgement replacement/drop, route-
kind or response-kind mutation, feedback preservation failure, authorizer or
proof-close failure must follow the existing STATUS custody guard and its
general emergency decision. Both dispatcher decision-resolution failures retain
the existing `RoutedInputDispatchOutcome.suppressed()` result. Each path fails
the acknowledgement as already owned, emits no contextual fallback, and never
issues a second permit or replay.

Assert exclusive diagnostic ownership for each injected failure boundary: pre-
permit failures produce only `contextual_busy_response_failure`; typed raw-
handoff failures and every acknowledgement-bearing or post-transfer failure
produce only the existing STATUS failure record. A later failure after a
same-acknowledgement cautious fallback must be ignored by the same existing
`record_once()` custody, and diagnostic-sink failure must not fall over to the
other schema.

Exercise ordinary, request-local generic-crafting-defaults, and trusted H5
routes wherever their existing input can reach the shared pre-submit busy
boundary. A non-busy rejection from any route retains its existing response.

Characterize H5 separately because its claimed route checks busy before
translation. A safe claimed H5 candidate that receives exact typed busy may be
decorated without asserting that B is valid; translation and command submission
remain zero for that input, while command admission records the exact typed
rejection. If no active command blocks a later fresh submission, the unchanged
H5 path performs its normal translation and may still reject it. The new policy
must not move, duplicate, or invoke H5 translation merely to choose busy
wording.

### 12.4 Delivery and race tests

Assert:

- on the healthy path, one response, one UI card, one output, and at most one
  TTS enqueue/playback;
- no simultaneous old generic busy card on the positive path;
- exact acknowledgement object identity survives decoration, feedback
  preservation, authorization, and dispatch resolution;
- every sink delivery diagnostic preserves
  `(command_busy_current_work, command_status, current_input)` without any
  `invalid` normalization;
- the TTS identity classifier accepts that exact tuple, rejects every near miss,
  and the TTS input coordinator enqueues the healthy response exactly once
  without speaking the presentation badge;
- feature-admission diagnostics receive the original route decision: ordinary
  and H5 origins retain scope A / `minecraft_command_feedback_v1`, while generic-
  defaults origins retain scope B / `generic_crafting_defaults_v1`; all retain
  `reason=minecraft_command_busy` and `handled_without_activation` rather than
  becoming scope `none`;
- no START or TERMINAL for rejected B;
- UI retry never repeats output or TTS;
- terminal after contextual busy is staged behind its permit and released once
  after acknowledgement resolution;
- terminal before contextual claim prevents late active-status wording;
- exact STOP still wins before contextual busy handling;
- a separate STOP input while the contextual permit is pending suppresses STOP
  START, exact owner retirement cancels the contextual publication lifecycle,
  and the independent non-preempting STOP terminal path publishes `멈췄어` once
  without claiming shared FIFO/acknowledgement staging;
- a suppressed post-permit LAVI Chat path may use only the invisible completion
  signal, creates no empty assistant card, output, or TTS, and permits the next
  submit; final microphone routing never exposes that Gradio-only value;
- repeated status questions and repeated busy conflicts remain deterministic
  and do not mutate A.

### 12.5 Regression scope

Run, at minimum:

- new contextual-busy focused tests;
- existing contextual STATUS focused tests and all-profile/all-target matrices;
- ordinary routing and submission-precheck tests;
- trusted Korean Chat/final-microphone source tests;
- STOP, lifecycle terminal, output, TTS, and Gradio integration tests;
- Minecraft Python focused suite;
- selected `llm_core` and `tts_core` regressions;
- full Python regression and Ruff when practical.

The implemented change is Python-only. Java tests and a
Gradle build are not required unless implementation unexpectedly changes Java,
protocol, runtime artifacts, or Minecraft Task behavior.

## 13. Implementation order

```text
1. Characterize explicit STATUS and existing genuine-busy behavior.
2. Introduce the contextual-busy evaluation and immutable observed-identity types.
3. Add the required extension/facade/adapter/runtime/server busy-inspection port
   and delegation tests; perform identity reinspection plus snapshot/raw-permit
   selection under one command lock and reuse the post-lock STATUS handoff.
4. Add the coordinator and decision decorator; reuse the existing STATUS renderer.
5. Insert the focused prepare plus conditional claim-and-transfer seam only
   after route_trusted() and before feedback rendering; retain the original
   route decision only for existing feature-admission projection.
6. Wire exact route preservation, STATUS custody, Minecraft presentation
   metadata, delivery schema, and the TTS identity classifier.
7. Add bounded mutually exclusive diagnostics, the one
   `contextual_busy_custody_handoff` STATUS stage, and stage-correct exception
   fallback/suppression.
8. Add all-profile, finite all-target, raw-domain property, source,
   identity-race, delivery, and TTS-classifier tests.
9. Run focused and full Python verification.
10. Perform live Chat and final-microphone validation only when separately requested.
```

No step may temporarily allow command B to bypass the command barrier.

## 14. Live verification contract

If live validation is separately authorized, the complete gate consists of
exactly two end-to-end rounds: one through LAVI Chat and one through final
microphone input. Each round uses a disposable/safe command target and records
both LAVI and Minecraft logs:

```text
1. Submit long-running command A and prove that it remains in one stable running
   state across the two inspections used for comparison.
2. Ask: 지금 뭐 해?
3. Record response R and its frozen active identity, descriptor, phrase profile,
   and response-driving lifecycle state.
4. Submit a different supported command B while that same running state remains
   active.
5. Verify B's separate inspection has equivalent comparison fields and the
   visible response is exactly R.
6. Verify Minecraft receives no command request for B.
7. Ask: 지금 뭐 해? again and verify the same active task remains.
8. Submit STOP and verify only 멈췄어 once.
9. Verify UI/output/TTS counts and terminal delivery.
```

If A transitions, terminates, or changes identity between the STATUS and busy
inspections, mark that round inconclusive and repeat it safely; do not treat
state-correct wording from different states as failed text parity.

Passing only Chat, only one item, or only visible UI text is partial evidence.
It does not prove the full source, profile, target, zero-submit, and sink matrix.

## 15. Rollback

Treat this feature as one independently reversible `RB6` policy:

```text
RB6a  contextual busy eligibility, observed identity, and locked inspection
RB6b  coordinator/decorator plus custody and presentation metadata wiring
RB6c  diagnostics and focused tests
RB6d  documentation links
```

These are ordered implementation and rollback slices within the single `RB6`
slot, not four additional rollback units.

Rollback order:

1. disable contextual busy presentation so `minecraft_command_busy` returns the
   existing generic sentence;
2. remove route-preservation, custody, presentation-metadata, coordinator, and
   decorator wiring, including the route-selector branch, custody-policy route
   kind, publisher presentation-metadata allowlist, and both
   `CommandFeedbackDeliverySchema.ROUTE_KINDS` and
   `ROUTE_RESPONSE_DELIVERY_IDENTITIES` entries, plus the exact
   `TtsLifecycleResponseIdentityClassifier._ALLOWED_IDENTITIES` tuple and the
   call-local original-decision admission-diagnostic projection seam;
3. remove busy inspector, observed-identity, and evaluator wiring;
4. remove now-unused focused diagnostics/types and the
   `contextual_busy_custody_handoff` value from both existing STATUS failure-
   stage allowlists, then remove the contextual-busy suppressed singleton;
5. retain this contract as a `ROLLED_BACK` history record and reconcile README
   and current-state documentation so they no longer claim the policy is active.

Rollback must not remove or weaken:

- explicit contextual STATUS questions;
- all-command lifecycle profiles and shared renderer;
- STOP terminal-only behavior and Gradio completion;
- STORE_HOME evidence evaluation;
- command barrier, tracker, correlation, reconciliation, or terminal CAS;
- existing output/TTS duplicate-prevention and at-most-once delivery guards;
- Java or Minecraft behavior.

## 16. Non-goals

- accepting, queueing, replacing, or preempting a second command;
- automatically running B after A finishes;
- changing command translation, aliases, target resolution, or quantities;
- broadening trusted input sources;
- changing disconnected, disabled, quarantined, unknown, or malformed wording;
- turning imperative commands into STATUS classifier matches;
- using an LLM to infer or paraphrase active work;
- reading Java logs to generate the live response;
- changing raw/native/legacy ChatClef behavior;
- changing STOP or STORE_HOME policies;
- changing wire DTOs, Java, ChatClef, AltoClef, Baritone, or Minecraft Tasks;
- changing Fabric/Forge backend ownership or adding MineMind work;
- building or deploying a Java JAR.

## 17. Acceptance checklist

Implementation is complete only when all of the following are true:

1. `지금 뭐 해?` and an eligible conflicting command produce identical text
   from separately frozen projections that are equivalent in active identity,
   descriptor, phrase profile, and response-driving lifecycle state.
2. The original conflicting-input admission and its feature-admission diagnostic
   remain `minecraft_command_busy` with canonical
   `ok=False/error=active_command`; the dedicated pre-transfer suppressed
   sentinel retains those fields, while existing post-transfer emergency or
   dispatcher suppression does not redefine the original admission.
3. The attempted command is submitted zero times and never queued or replayed.
4. The active command's full identity and descriptor remain unchanged.
5. All eligible profiles and all already accepted targets use shared rendering,
   with no item-specific branch; ordinary, generic-crafting-defaults, and H5
   entry paths are covered where applicable.
6. The decorated route kind is preserved before reason-based busy rendering,
   its exact STATUS acknowledgement enters existing custody, and Minecraft
   presentation metadata supplies the badge without changing speech text; the
   delivery schema and TTS identity classifier accept the exact
   route/response/mode identity, while feature-admission diagnostics retain the
   original route scope and busy reason.
7. Pre-permit missing or invalid evidence preserves the original nonempty
   generic busy response candidate exactly once. Raw-handoff or pre-transfer
   failure uses only the fixed cautious STATUS text under the same
   acknowledgement when possible, otherwise the contextual suppressed sentinel;
   post-transfer failure retains the existing STATUS emergency or dispatcher
   suppression contract.
8. STOP remains first and publishes only its verified terminal response.
9. A healthy positive path creates one response fact, one UI card, and one
   output event; when TTS is enabled and succeeds, it plays once. Retry and
   failure paths keep every sink at most once per input.
10. Exceptions attempt one bounded diagnostic through the exclusive owner for
    that lifecycle interval and produce the stage-correct fallback or
    suppression outcome without issuing another permit or response.
11. Focused, Minecraft Python, adjacent core, full Python, and Ruff verification
    results are recorded accurately.
12. Java source, protocol, Tasks, runtime JARs, and Gradle remain unchanged.
13. Live status is reported as partial until exactly one Chat round and one
    final-microphone round each prove text parity, zero submission, and sink
    counts.
