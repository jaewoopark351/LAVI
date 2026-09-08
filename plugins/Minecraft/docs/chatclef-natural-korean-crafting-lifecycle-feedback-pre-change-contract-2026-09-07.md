<!-- 20260907_kpopmodder: Defined the natural Korean crafting lifecycle contract, implemented its core, and recorded the remaining source-known closure gaps. -->
<!-- 20260907_kpopmodder: Kept conversational wording separate from transport state, gameplay-effect proof, and diagnostics-only evidence. -->
<!-- 20260907_kpopmodder: Reconciled offline Python/Java tests, the clean forced build, and the separately unverified deployment/runtime boundary. -->
<!-- 20260907_kpopmodder: Linked the docs-only all-item/all-command generalization while retaining this exact profile as the implemented baseline. -->
<!-- 20260907_kpopmodder: Corrected source-known running-wire, STOP-arbitration, TTS-receipt, response-generation, UI-badge, and delivery-diagnostic gaps found during documentation review. -->
<!-- 20260907_kpopmodder: Reconciled migration into the generalized lifecycle and closure of all six exact-slice boundaries. -->
<!-- 20260907_kpopmodder: Linked the later exact-profile runtime reproduction of the Python/Gradio duplicate-presentation defect. -->
<!-- 20260908_kpopmodder: Reconciled the shared Python UI correction and pending live-browser retest. -->
<!-- 20260908_kpopmodder: Reconciled the later narrow STORE_HOME evidence profile without broadening GET proof. -->

# ChatClef Natural Korean Crafting Lifecycle Feedback Contract and Implementation Record

Date: 2026-09-07

## 2026-09-07 generalized-implementation reconciliation

This exact `get diamond_pickaxe 1` craft-intent slice is now a compatibility
profile inside the applied generalized lifecycle implementation. Current
authority is the
[General Natural Korean Command Lifecycle Feedback Implementation Record](chatclef-general-natural-korean-command-lifecycle-feedback-implementation-record-2026-09-07.md).

All six source-known boundaries listed by this historical record are closed in
repository source and focused tests: initial progress sequencing, STOP terminal
arbitration, separate TTS enqueue/play receipts, late-terminal non-preemption,
UI-only Minecraft provenance, and lifecycle diagnostic-kind coverage. The
strong completion claim remains deliberately narrow: generalized
single-target GET may use authoritative acquisition-delta evidence. A later,
separately owned trusted-translation STORE_HOME profile may use its strict typed
completion evidence; multi-target/bracket GET and every other command retain
cautious terminal wording unless separately proven.

Final offline integration verification is complete in the implementation
record, with one unrelated pre-existing repository `HEAD`-hash contract
failure recorded separately. Deployment and Minecraft runtime verification in
that generalized implementation checkpoint are `NOT_RUN`. Historical test
counts, artifact hashes, future-tense requirements,
and gap descriptions below document the earlier exact-slice checkpoint and are
not the current generalized verification result.

A later user-run `get diamond_pickaxe 1` reproduction passed command execution,
authoritative item delta, terminal correlation, output, and TTS exactly once,
but repeated the same terminal card in Gradio Chatbot. The verified defect is
shared async UI presentation infrastructure rather than this exact crafting
profile. See the
[Gradio Routed-Response Duplicate Presentation Investigation](chatclef-gradio-routed-response-duplicate-presentation-investigation-2026-09-07.md).
Its shared Python source fix is `IMPLEMENTED_VERIFIED_OFFLINE` through the real
Gradio postprocess/JSON/preprocess boundary. The live browser smoke retest is
still `NOT_RUN`.

## 1. Document status

This document preserves the pre-change contract for task 6 and its historical
exact-slice repository checkpoint below. It
defines the user-visible wording, evidence gates, ownership, package
boundaries, tests, and rollback units without treating unit-test fixtures as
live cross-language or TTS proof.

```text
REVIEWED_REPOSITORY_ROOT: C:\Vtuber_Souorce_Code\LAVI
REVIEWED_BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
REVIEWED_HEAD: c5968582a826e7fb10b857e7c50388c5e3ec93d2
STATUS: MIGRATED_TO_GENERAL_IMPLEMENTATION_VERIFIED_OFFLINE
REVIEWED_WORKTREE_STATE: DIRTY_EXISTING_CHANGES_PRESERVED

CURRENT_STATUS_WIRE_COMPATIBILITY: CLOSED_SOURCE_AND_FOCUSED_TESTS
CURRENT_STOP_RESPONSE_ARBITRATION: CLOSED_SOURCE_AND_FOCUSED_TESTS
OUTPUT_DISPATCH_AND_FAKE_SINK_INVOCATION: VERIFIED_OFFLINE_AT_MOST_ONCE
TTS_ENQUEUE_AND_PLAY_RECEIPTS: CLOSED_SOURCE_AND_FOCUSED_TESTS
CURRENT_RESPONSE_GENERATION_NONPREEMPTION: CLOSED_SOURCE_AND_FOCUSED_TESTS
UI_MINECRAFT_BADGE: CLOSED_SOURCE_AND_FOCUSED_TESTS
CURRENT_DELIVERY_DIAGNOSTIC_KIND_ALLOWLIST: CLOSED_SOURCE_AND_FOCUSED_TESTS

BACKEND_SCOPE: FABRIC_CHATCLEF_ONLY
FORGE_MINEMIND_SCOPE: NOT_APPROVED_NOT_CREATED
PYTHON_PRODUCTION_CHANGE_IN_THIS_TASK: APPLIED_LAVI_OWNED_BOUNDARIES
JAVA_PRODUCTION_CHANGE_IN_THIS_TASK: APPLIED_OBSERVATION_ONLY_GET_EFFECT_PROJECTION
TEST_SOURCE_CHANGE_IN_THIS_TASK: APPLIED
PYTHON_FOCUSED_TESTS: PASS_37_PLUS_14_SUBTESTS
PYTHON_MINECRAFT_SUITE: PASS_930_SKIP_2_PLUS_3476_SUBTESTS
PYTHON_ADJACENT_ROUTING_TESTS: PASS_33
PYTHON_ROOT_MINECRAFT_GLOB: 216_PASS_1_PREEXISTING_HEAD_HASH_CONTRACT_FAILURE
JAVA_1_20_1_TESTS: PASS_823_FAILURES_0_ERRORS_0_SKIPPED_1
BUILD_IN_THIS_TASK: CLEAN_FORCED_BUILD_PASSED_171_TASKS_EXECUTED
DEPLOYMENT_IN_THIS_TASK: NOT_RUN
MINECRAFT_RUNTIME_IN_THIS_TASK: NOT_RUN
COMMIT_OR_PUSH_IN_THIS_TASK: NONE
```

The focused feature code is responsibility-split across
`input/status/crafting/`, `response/crafting_lifecycle/`, and
`transport/command_feedback/crafting/`, with the existing external
output-dispatch composition boundary wired by
`minecraft_crafting_terminal_response_wiring.py`. At this historical
checkpoint, the Java bridge added only the exact `get diamond_pickaxe 1`
inventory-delta projection under
`bridge/command/result/effect/get/`; it does not change Task behavior.

START and STATUS publications receive ordered one-shot permits while the
existing command lock owns lifecycle bookkeeping. Output occurs outside that
lock. START failure retires the response lifecycle, STATUS failure retires only
that query permit, and a terminal response waits for all earlier publications.
The Python tracker accepts nonterminal state only with a strictly increasing
positive integer `evidence_sequence`. At this historical checkpoint, the Java
initial `running/dispatch_started` result omitted that field. The generalized
implementation now emits sequence `1`, reserves `2+` for later evidence, and
covers the serialized Java-to-Python path with a shared fixture.

The final required command was run with JDK 21 from the runtime root:

```powershell
.\gradlew.bat clean build --rerun-tasks
```

It completed successfully with 171 executed tasks. The 1.20.1 test XML contains
823 tests, zero failures, zero errors, and one skip. The remapped artifact is
`versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar` with SHA-256
`264C1C92499C90CCBE386616EC06A3566F59D7BDB2A202EAA135E159F8D18A2E`.
Deployment, Minecraft launch, and live-world verification were `NOT_RUN` in
that historical implementation/build task. The later user-run observation is
recorded in the generalized reconciliation and duplicate-presentation
investigation linked above.

The companion refactoring work requested with this feature is independently
owned by
[Targeted Diagnostics Refactoring and Folderization Implementation Record](chatclef-targeted-diagnostics-refactoring-folderization-plan-2026-09-07.md).
The natural-response feature, including its LAVI-owned Java GET-effect
projection, and the Java diagnostics refactors are separate change, test, and
top-level rollback families. Neither depends on completing the other.

The all-item/all-command response requirement originated in
[General Natural Korean Command Lifecycle Feedback Pre-Change Contract](chatclef-general-natural-korean-command-lifecycle-feedback-pre-change-contract-2026-09-07.md).
That broader implementation is now applied, and this exact profile remains its
legacy compatibility characterization rather than the current implementation
ceiling. See the generalized implementation record for current evidence.

## 2. Authority and narrow supersession

This document narrows only the lifecycle wording for the exact trusted
Korean craft-intent `GET_ITEM diamond_pickaxe 1` profile. It does not rewrite
the historical status or verification ledger in
[Korean Command Feedback, Generic Crafting, and Stop Contract](chatclef-korean-command-feedback-crafting-stop-pre-change-contract-2026-09-05.md).

The earlier rule remains true: an accepted submission is not completion, so
the start response must never say that the item is already made. This document
adds a separately gated running response with synthetic predicate coverage only
and a completion response with offline acquisition-delta proof. It also
narrows the broader reply design in
[Python Command Orchestration Plan](chatclef-python-command-orchestration-plan.md).

At the historical exact-slice checkpoint, this profile was the only implemented
narrow exception to the earlier contract's Section 7.3 literal `[Minecraft]`
response prefix and Section 17 exclusion of ordinary-command terminal TTS. Its
three bodies used the existing external output composition boundary, while
every other immediate command response, prefix, and terminal-delivery rule
remained unchanged. The generalized implementation now supersedes that
exact-only delivery scope with separate UI-only Minecraft provenance and
distinct TTS enqueue/play receipts for its registered response profiles.

The Java GET-delta projection in Section 10 began as a separately gated evidence
unit for this exact profile and was later generalized to supported
single-target GET descriptors. It is not implicit approval for a generic
ordinary-command effect observer or terminal narrator. Strong success wording
remains disabled whenever the applicable evidence is unavailable or fails
verification.

The following contracts remain authoritative and are not weakened:

- [Minecraft Backend Separation](minecraft-backend-separation.md)
- [Fabric ChatClef Bridge Protocol V1](fabric-chatclef-bridge-protocol-v1.md)
- [Command Lifecycle and Threading](chatclef-command-lifecycle-and-threading.md)
- [Korean ChatClef Test Strategy](chatclef-korean-test-strategy.md)
- [ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)

## 3. Exact user-visible contract

For the exact trusted request `다이아 곡괭이 만들어줘`, whose validated
intent is `GET_ITEM`, whose compiled command is `get diamond_pickaxe 1`, and
whose acquisition verb classification is `craft`, the response body is:

| Lifecycle boundary | Exact Korean response body | Required evidence |
| --- | --- | --- |
| Submission accepted and exact active owner committed | `다이아 곡괭이 만들어 줄게` | Accepted send plus matching Python active-command ownership |
| User asks while that exact request is authoritatively running | `다이아 곡괭이 만드는 중이야` | Read-only status query plus matching trusted `running` result |
| Requested acquisition effect is verified after matching completion | `다이아 곡괭이 다 만들었어` | Matching `completed` terminal plus target-item acquisition-delta proof |

These strings are the exact conversational bodies. The body and any TTS text
must not contain the literal `[Minecraft]` prefix. The UI must receive a
separate typed Minecraft source badge or channel label. At this historical
checkpoint that presentation metadata was `NOT_IMPLEMENTED`; the generalized
implementation has since wired and focused-tested it, while keeping it out of
the spoken sentences.

For this target, the preferred conversational label is exactly `다이아 곡괭이`,
even though the current canonical display resource says `다이아몬드 곡괭이`.
The implemented renderer owns that explicit display decision; it must not alter item
resolution or the canonical command target.

Initial scope is exactly `diamond_pickaxe` with quantity `1`, and the response
omits that quantity. Every other target or quantity retains its existing
response behavior until a separately documented phrase and effect-oracle
extension is approved. The renderer stores the exact bounded label when the
request is admitted; it must not echo arbitrary raw input or reconstruct the
label by parsing the DSL at terminal time.

## 4. Meaning of the three phrases

### 4.1 `만들어 줄게`

This is a future-intent acknowledgement. It means LAVI accepted responsibility
for the exact request and committed the corresponding active command owner. It
does not assert that Java has emitted `running`, that crafting-grid interaction
has started, or that the item already exists.

Translation success alone, a locally allocated request ID, or a socket send
attempt is not sufficient. The response is emitted only after the existing
ordinary submission boundary returns accepted and the same identity is visible
as the active owner.

### 4.2 `만드는 중이야`

This response is on demand. It is not a periodic announcement. It may be
returned only when all of the following are true:

1. The new utterance is admitted as a trusted Korean crafting-status question.
2. The question resolves to the active tracker's exact target, or is an
   unqualified question asking what Minecraft is currently making.
3. Session ID, connection generation, request ID, and command message ID still
   match the active owner.
4. The latest result was accepted by the existing ordinary
   `accept_result_and_reconcile()` transaction while its active websocket,
   pre-result owner snapshot, session, generation, request, and message all
   matched.
5. That accepted result has `status=running` and
   `result_reason=dispatch_started`.
6. The tracker has not retired, entered UNKNOWN quarantine, or accepted a
   terminal result.

These were the intended truth conditions at the historical checkpoint, not
then-current proof that the real wire could satisfy them.
`CraftingFeedbackTracker.record_nonterminal()` additionally requires
`evidence_sequence` to be an exact positive integer, but the checkpoint Java
initial `running/dispatch_started` payload omitted it. Existing checkpoint tests
manually add that value or substitute a fake snapshot. Implementation must
either emit a monotonic Java sequence or define one closed, one-shot Python
rule for the initial dispatch result, then prove it with a real serialized
Java-to-Python fixture before `만드는 중이야` is marked wire-compatible. If
Java emits initial sequence `1`, all later evidence in that same execution must
share the sequence owner and continue at `2+`; a second counter restarting at
`1` is invalid.

The later `running` evidence stages `finish_callback_observed_nonterminal` and
`stable_request_quiescence_observed` are callback/quiescence observations, not
proof that item production remains in progress. They and every unlisted
running reason are excluded from the exact progress sentence.

The status route submits zero commands, changes zero task/input/path owners,
performs zero retry, and does not extend any timeout. One admitted question
produces at most one response.

If the request is accepted but no matching `running` evidence has arrived, a
truthful response such as `다이아 곡괭이 만드는 작업이 시작됐는지 확인 중이야`
may be used. It must not use the exact progress sentence. An explicit Minecraft
status question may say `지금 만드는 중인 아이템은 없어` only when the
existing authoritative ordinary owner and Java lifecycle evidence jointly
prove idle. Missing Python context, disconnect, UNKNOWN quarantine, or terminal
effect verification does not prove idle; those states use
`지금 제작 상태를 확인하지 못했어`. Unrelated conversation falls through to
the existing route.

Status admission and terminal acceptance share the existing ordinary
`command_lock` as their linearization boundary. Under that lock, the status
route reads the owner and tracker together and claims one progress-response
permission. If terminal acceptance wins first, progress publishes zero times.
If the status claim wins first, it may publish the one response authorized by
that atomic snapshot; the terminal then follows normally. No unlocked stale
snapshot may authorize progress.

### 4.3 `다 만들었어`

This sentence makes a gameplay-effect claim. The following evidence is all
required:

```text
exact session/generation/request/message identity match
active websocket object matches
existing accept_result_and_reconcile outcome.accepted == true
terminal before_snapshot contains the same live active owner and tracker
status == completed
result_reason == matching_task_finished
result_fidelity == callback_plus_matching_user_task_event
effect_kind == get_acquisition_delta
target_item == tracker.canonical_target == diamond_pickaxe
requested_count == tracker.requested_count == 1
before_target_count is authoritative
after_target_count is authoritative
after_target_count - before_target_count >= requested_count
first winner of the tracker's terminal-response CAS
```

The matching Task-finished terminal is necessary supporting evidence. The
implemented LAVI-owned Java projection now supplies authoritative before/after
target-item counts when the client/world/player binding remains exact. The
completion sentence stays disabled for unavailable, partial, or stale evidence.

The conversational word `만들었어` means that the acquisition request which
originated from a craft verb produced the requested item quantity. It does not
claim that ChatClef used a crafting table rather than existing inventory,
pickup, trade, or another valid acquisition path. Proving the physical crafting
path would require a separate observer and is not part of this task.

## 5. Outcomes that must not claim completion

The exact completion sentence is forbidden for:

- `accepted` without matching running or terminal evidence;
- `running`;
- `callback_completed_without_user_task`;
- `rejected`, `failed`, `cancelled`, or `deadline_exceeded`;
- wire or locally reconciled `unknown`;
- stale session, generation, request, message, target, or quantity;
- duplicate or late terminal results;
- incomplete or unavailable gameplay observation;
- final item total without a trusted pre-command count;
- target-count delta smaller than the requested additive GET quantity.

Safe terminal bodies are deterministic and separate from the success body:

| Outcome | Safe example |
| --- | --- |
| Matching completed terminal, effect unavailable | `다이아 곡괭이 만드는 작업은 끝났는데, 다 만들어졌는지는 확인하지 못했어` |
| Failed after accepted `dispatch_started` | `다이아 곡괭이 만들다가 실패했어` |
| Failed without accepted `dispatch_started` | `다이아 곡괭이 만들지 못했어` |
| Cancelled for a reason other than the user-owned STOP flow | `다이아 곡괭이 만들기가 중단됐어` |
| Deadline exceeded | `다이아 곡괭이 만들기가 시간 안에 끝나지 않았어` |
| Unknown or contradictory evidence | `다이아 곡괭이 만들기 결과는 확인하지 못했어` |

None of these outcomes automatically retries, replays, or resubmits the
original command.

`cancelled` with exact `result_reason=user_stop_requested` is audit and
reconciliation evidence only. The specialized STOP terminal owns the one
user-visible stop sentence. The generalized coordinator now uses exact live
STOP-owner arbitration, closing the duplicate-response gap recorded at this
checkpoint.

## 6. Admission scope

This feature is limited to the same proven trusted Korean origins as the
existing scoped command feedback:

| Input path | `source` | `provider_id` | `event_kind` | `final` |
| --- | --- | --- | --- | ---: |
| LAVI Chat final submit | `lavi_chat_ui` | `lavi_chat_ui` | `chat_submit` | `true` |
| Microphone final recognition | `voice_input_final` | `VoiceInput` | `final_transcript` | `true` |

It is not newly enabled for microphone partial/interim input, `direct_typed`,
legacy GUI provenance, external chat, stream chat, ScreenVision, arbitrary
plugins, LLM-origin intents, non-Korean input, or raw ChatClef DSL.

The crafting lifecycle profile requires all of these immutable facts at initial
admission:

```text
intent.kind == GET_ITEM
KoreanAcquisitionVerbMatcher.classify(original_text) == craft
validated canonical target == diamond_pickaxe
validated requested quantity == 1
existing trusted-ingress proof consumed by the existing route owner
ordinary command admission accepted exactly once
```

Generic `get` is not enough. `get` can mean mining, gathering, pickup, trading,
or use of existing inventory, so mining/acquire verbs must retain their existing
generic wording.

## 7. Status-question route

The initial deterministic grammar is intentionally narrow:

```text
마크 지금 뭐 만들고 있어?
마크 뭐 만드는 중이야?
다이아 곡괭이 만들고 있어?
지금 다이아 곡괭이 만드는 중이야?
```

Normal punctuation and spacing may use the existing Korean normalizer. A
target-qualified question must resolve through the existing item resolver and
match the active tracker's canonical target. Substring matching, raw-text-only
matching, and LLM classification are not authorization.

The route order is:

```text
trusted Korean input admission
  -> STOP control route
  -> crafting status-query route
  -> existing generic-crafting-defaults route
  -> existing Minecraft intent gate and ordinary routes
```

STOP remains higher priority. The status-query route runs before ordinary busy
rejection because its entire purpose is to observe an already-active command.
It cannot create or replace an active owner. A mismatched item question must
not disclose or mutate another operation through a guessed request identity.

## 8. Pre-change source gaps and implemented lifecycle state

### 8.1 Core pre-change gaps addressed by the current source

The reviewed HEAD source already compiled `다이아 곡괭이 만들어줘` to
`get diamond_pickaxe 1` and could classify its acquisition verb as `craft`.
Before this implementation, the complete feedback behavior was not present:

- the ordinary parser/intent path did not preserve the `craft` classification
  as immutable command-lifecycle context;
- the active Python command did not retain the parsed item phrase, canonical
  target, quantity, original source event, or terminal response grant;
- the input route had no deterministic crafting-status question owner;
- the ordinary command result handler validated/reconciled results but did not
  publish a user terminal response;
- the ordinary completed result did not provide authoritative
  before/after target-item counts;
- the display resource used `다이아몬드 곡괭이`, not the requested
  conversational label.

The checkpoint source addressed those core absences but had not yet closed the
six cross-boundary gates in Sections 1 and 15. The generalized implementation
has since closed them.

The existing STOP terminal publisher is a composition example only. Its
tracker, barrier, statuses, and delivery authority cannot be reused for this
exact ordinary crafting profile.

### 8.2 Immutable lifecycle context and state machine

At accepted submission, store one immutable crafting feedback context:

```text
source event ID and admitted source tuple
session ID
connection generation
request ID
command message ID
validated canonical command
intent kind
acquisition verb class == craft
canonical target item
bounded spoken item label
requested additive quantity
authoritative pre-command target count, when available
accepted timestamp
```

Keep the changing values in a separate synchronized tracker state:

```text
latest accepted lifecycle status
latest accepted result reason and evidence sequence
UNKNOWN quarantine state
start-response delivery CAS
progress-response claim for the current status question
terminal-response delivery CAS
```

The ordinary command contract already allows at most one active request, so the
new owner also holds at most one live crafting feedback context. It is
instance-owned, not static/global, and cannot become a second command-admission
owner.

Before the trusted initial-input proof closes, accepted submission also binds
one non-forgeable terminal-response grant to the exact event, source, session,
generation, request, message, target, quantity, and tracker identity. A later
terminal may consume that grant once through the result coordinator. It cannot
reconstruct delivery authority from a source string, raw text, DSL, result
payload, or copied identifiers. The one live grant retires with its tracker, so
there is no unbounded response ledger.

```text
NONE
  -> ACCEPTED_TRACKED
  -> RUNNING_TRACKED
  -> TERMINAL_EFFECT_VERIFYING
  -> VERIFIED_COMPLETED
  -> RETIRED

ACCEPTED_TRACKED or RUNNING_TRACKED
  -> FAILED | CANCELLED | DEADLINE_EXCEEDED | UNKNOWN_QUARANTINED
  -> RETIRED only under the existing matching ownership rules
```

This is the intended state machine. In the recorded checkpoint wire,
`RUNNING_TRACKED` was not reached from the initial Java dispatch frame because
that frame lacks the sequence required by the Python tracker.

Later or duplicate results may update audit evidence only when the existing
transport contract permits it. They cannot reopen a retired tracker, publish a
second terminal response, clear a newer owner, or convert UNKNOWN into success.

## 9. Implemented responsibility and package boundaries

New code belongs only to LAVI-owned Fabric/Python boundaries. It must not be
placed in `common`, Forge/MineMind, or upstream-derived `adris/**` classes.

| Responsibility | Implemented location | Must not own |
| --- | --- | --- |
| Deterministic status-question recognition and target match | `plugins/Minecraft/fabric/chatclef/input/status/crafting/` | command submission, output delivery, terminal mutation |
| Immutable response context and one-live-context tracker | `plugins/Minecraft/fabric/chatclef/transport/command_feedback/crafting/` | Java task lifecycle, retry, LLM calls |
| Lifecycle/result correlation and terminal one-shot decision | `plugins/Minecraft/fabric/chatclef/transport/command_feedback/crafting/` | UI/TTS implementation, gameplay mutation |
| Korean phase rendering | `plugins/Minecraft/fabric/chatclef/response/crafting_lifecycle/` | status classification, transport ownership |
| External output-dispatch composition | `app_core/composition_core/component_wiring/minecraft_crafting_terminal_response_wiring.py` | TTS receipt/playback, UI badge, result truth classification, resubmission |
| Authoritative GET-delta projection for this exact profile | `lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get` | generic result policy, diagnostics policy, task selection, crafting behavior |

Use separate small classes for classifier, immutable context, tracker, renderer,
result coordinator, and delivery authorizer. Do not create a generic manager or
a shared mutable lifecycle base class.

The completion publisher may follow the existing STOP publisher's one-shot
composition pattern, but it must be a separate ordinary-crafting component.
It must not reuse STOP state, STOP barriers, or STOP result semantics.

The existing iron-pickaxe `CraftResourceTerminalLedger` and related terminal
diagnostics remain diagnostics-only and exact-command-specific. They are not a
behavior authority, user-response tracker, or reusable completion publisher.

## 10. Additive effect-evidence boundary

Source inspection confirmed that the ordinary result lacked target-count proof,
so the implementation adds only a narrow LAVI-owned projection to the existing
`CommandResultDTO.data` extension surface. It:

1. Capture the canonical target and an authoritative target-item count before
   the accepted command can mutate inventory.
2. Capture the matching terminal target-item count on the Minecraft client
   thread.
3. Project typed target, requested quantity, before count, after count, and
   delta into the matching result.
4. Mark unavailable, partial, stale, or failed observation explicitly instead
   of fabricating zero or success.
5. Remain observation-only; it must not alter Task selection, completion,
   crafting, input, pathing, retry, timeout, or cleanup.

This uses the existing v1 result data extension point. It does not require a
new top-level message type, a new wire status, a protocol-version increase, or
an edit to upstream ChatClef/AltoClef classes. If the before count cannot be
captured before mutation, the completion claim remains unavailable.

## 11. Response delivery contract

- The winning start permit invokes the configured response publisher at most
  once and only after accepted ownership commit.
- Each admitted status question invokes at most one response publication and
  never creates a command.
- One matching terminal result invokes at most one terminal publication.
- Duplicate/late results invoke zero additional publications.
- Chat and final microphone use the same renderer and truth predicate.
- Delivery calls the normal external response/output dispatcher at most once
  per winning permit and invokes the LLM zero additional times. Dispatcher
  return does not prove a downstream listener accepted the payload, because
  `LLMEventDispatcher.send_output()` isolates listener exceptions.
- A Minecraft lifecycle response is not recursively submitted as input and is
  not added to ordinary user/assistant chat history by default.
- A failed output listener does not change transport state or retry policy.
- TTS queue acceptance and playback now have distinct TTS-owned receipts. The
  UI-only Minecraft badge remains separate from response and TTS text.
- Late terminal delivery is now non-preempting and does not advance global LLM
  response generation or drop newer unrelated speech.

## 12. Historical exact-slice characterization and regression coverage

### 12.1 Parser and admission

- The exact Chat and final-microphone `diamond_pickaxe` quantity-one craft
  request creates one context.
- Other targets and quantities create zero new lifecycle-profile contexts and
  retain their existing response behavior.
- Acquire/mining verbs, raw DSL, LLM intents, untrusted sources, partial voice,
  and non-Korean input create zero contexts.
- `diamond_pickaxe` renders the conversational label `다이아 곡괭이` while
  preserving the canonical target.
- Quantity one is omitted from the exact body.

### 12.2 Start response

- Translation-only, rejected, disconnected, busy, exception, and unknown send
  paths emit zero start-success sentences.
- Accepted plus exact active-owner commit emits the exact start body once.
- A repeated callback or duplicate request cannot publish or submit twice.

### 12.3 Status query

- STOP remains higher priority.
- Exact generic and target-qualified questions use the read-only route.
- Accepted-only does not say `만드는 중이야`.
- A synthetic exact matching `running/dispatch_started` fixture with a valid
  `evidence_sequence` says it once for each admitted query.
- At that historical checkpoint, the real Java initial-running serialization
  omitted `evidence_sequence`; a cross-language compatibility test was still
  required. The generalized implementation now emits sequence `1` and covers
  that serialized boundary.
- Callback/quiescence running stages never say `만드는 중이야`.
- Target mismatch, stale generation, retired owner, terminal owner, and UNKNOWN
  quarantine never use the progress sentence.
- Every status-query fixture sends zero `command_request` envelopes and mutates
  zero ownership state.
- Status/terminal races are linearized under the ordinary command lock; a
  terminal-first race publishes zero stale progress responses.

### 12.4 Terminal response

- Only an already-accepted ordinary reconciliation outcome with matching active
  websocket and before-snapshot owner may reach the completion coordinator.
- Matching completed plus verified additive target delta publishes the exact
  completion body once.
- Completed without a trustworthy before count uses the cautious body.
- Insufficient delta, callback-only completion, every non-completed terminal,
  malformed data, stale identity, duplicate, and late result publish no false
  completion body.
- Terminal publish occurs only after the existing ordinary owner has accepted
  the result according to its current ordering contract.
- No branch retries, replays, or resubmits the command.
- `cancelled/user_stop_requested` is now arbitrated against the exact live STOP
  owner, so the ordinary command result remains reconciliation/audit evidence
  and cannot produce a second terminal sentence.

### 12.5 Delivery

- Chat and final microphone retain at-most-once publication. Separate sink-owned
  enqueue and playback receipts now cover TTS delivery without treating either
  receipt as Minecraft gameplay evidence.
- Any spoken string must contain no `[Minecraft]` prefix.
- The UI-only Minecraft badge and bounded typed detail are implemented without
  altering the response or TTS body.
- LLM call count, recursive input count, and ordinary-history insertion count
  remain zero for lifecycle responses.
- Delivery tests distinguish dispatcher invocation, listener receipt, TTS
  queue acceptance, and playback rather than naming all four boundaries
  `output/TTS`.
- Delivery diagnostics must recognize
  `route_kind=crafting_lifecycle`,
  `route_kind=crafting_status_query`, and
  `response_kind=crafting_terminal`. These lifecycle kinds are now accepted by
  the bounded diagnostic policy rather than being recorded as `invalid`.
- A late asynchronous crafting TERMINAL response does not begin, advance, or
  cancel global LLM response generation and uses command-local non-preempting
  delivery. START and on-demand STATUS retain their current-input capability
  generation.

## 13. Implementation and rollback units

Implemented core and remaining closure order:

1. Characterize the existing accepted/running/terminal identity and output
   ordering without changing behavior.
2. Add the exact diamond-pickaxe quantity-one immutable response context and
   deterministic renderer.
3. Add the read-only status-query route.
4. Add one-shot terminal result handling for this exact profile with cautious
   non-success wording.
5. Add the authoritative GET-delta result projection only if the current data
   gap remains source-confirmed.
6. Enable the exact verified-completion sentence only after the effect tests
   pass.
7. Run focused Python tests, the Minecraft Python suite, focused Java tests if
   Java changed, and the required clean forced Fabric build.

Rollback is independent by unit: status query, start renderer/context,
terminal delivery, and optional effect projection can each be removed without
changing generic command submission, STOP control, or Java Task behavior. The
success renderer must be disabled before or together with any rollback of the
effect evidence it requires.

## 14. Non-goals

- Forge or MineMind code, placeholders, protocol values, configuration, or
  tests
- ChatClef/AltoClef/Baritone Task or scheduler changes
- Physical crafting-grid path proof
- automatic retry, replay, fallback target, or resubmission
- status polling that mutates or prolongs the active operation
- broad ordinary-command terminal narration
- LLM-generated lifecycle wording
- reuse of diagnostics-only ledgers as behavior authority
- deployment, Minecraft launch, live-world mutation, commit, push, or release
  outside this repository implementation scope

## 15. Historical completion gate and current reconciliation

The following six items were the closure gates at the exact-slice checkpoint:

1. make initial Java `running/dispatch_started` evidence compatible with the
   Python sequence rule and prove the serialized cross-language path;
2. suppress ordinary `cancelled/user_stop_requested` narration so the STOP
   terminal remains the sole speech owner;
3. add distinct sink-owned TTS queue-acceptance and playback-observation
   receipts before claiming either fact;
4. prevent late asynchronous crafting TERMINAL responses from advancing the
   global response generation or preempting/dropping newer unrelated LLM
   speech, while START and on-demand STATUS retain their current-input
   capability generation;
5. implement and receipt-test the required UI-only Minecraft badge without
   placing `[Minecraft]` in response or TTS text; and
6. register and test the exact `crafting_lifecycle` and
   `crafting_status_query` route kinds plus the `crafting_terminal` response
   kind in delivery diagnostics so valid start/status/terminal responses are
   not logged as `invalid`.

All six are now closed by the generalized implementation and focused tests.
This does not widen the effect oracle: only single-target GET currently has a
strong positive completion proof, while multi-target/bracket GET and non-GET
commands remain cautious. Final offline integration verification is complete;
deployment and runtime in that checkpoint remain `NOT_RUN`. The later user-run
exact-profile observation passed the command/effect boundaries but failed the
shared Gradio browser-presentation exactly-once boundary documented above.
