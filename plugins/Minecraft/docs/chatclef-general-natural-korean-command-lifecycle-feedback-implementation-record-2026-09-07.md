<!-- 20260907_kpopmodder: Recorded the generalized natural Korean command-lifecycle feedback implementation. -->
<!-- 20260907_kpopmodder: Kept response narration separate from command admission, gameplay behavior, and diagnostics refactoring. -->
<!-- 20260907_kpopmodder: Recorded the later user-run Gradio duplicate-presentation defect without rewriting the historical offline verification. -->
<!-- 20260908_kpopmodder: Recorded the Python-only Gradio presentation identity correction and offline verification. -->
<!-- 20260908_kpopmodder: Linked the verified STOP publication-policy and STORE_HOME evidence-profile follow-up without rewriting this historical implementation record. -->
<!-- 20260908_kpopmodder: Reconciled the implemented STOP/STORE_HOME follow-up and final selected Python regression. -->

# ChatClef General Natural Korean Command Lifecycle Feedback Implementation Record

Date: 2026-09-07

## 1. Result

The Fabric ChatClef Python integration now gives deterministic Korean lifecycle
feedback for every currently registered command that an existing LAVI-owned
interactive ingress actually accepts. This is not a new command authority. The
existing registry, source policy, safety tier, confirmation rule, command lock,
transport identity, and Java task behavior remain authoritative.

```text
BACKEND_SCOPE: FABRIC_CHATCLEF_ONLY
REGISTERED_COMMAND_PROFILE_COUNT: 26
COMMAND_ADMISSION_CHANGE: NONE
TASK_BEHAVIOR_CHANGE: NONE
AUTOMATIC_RETRY_OR_RESUBMIT: NONE
LLM_RESPONSE_GENERATION: NONE
FORGE_OR_MINEMIND_CHANGE: NONE
DIAGNOSTICS_REFACTOR_ROLLBACK_UNIT: SEPARATE
ORIGINAL_IMPLEMENTATION_TASK_OFFLINE_VERIFICATION: IMPLEMENTED_VERIFIED_OFFLINE
PYTHON_FEATURE_INTEGRATION: PASS_187_PLUS_3978_SUBTESTS
PYTHON_MINECRAFT_SUITE: PASS_1032_SKIP_2_PLUS_6891_SUBTESTS
PYTHON_LLM_APP_TTS_ADJACENT: PASS_125_PLUS_23_SUBTESTS
PYTHON_FULL_SUITE: PASS_2285_SKIP_4_PLUS_7945_SUBTESTS_WITH_1_PREEXISTING_HEAD_HASH_CONTRACT_FAILURE
JAVA_1_20_1_FULL_TESTS: PASS_843_FAILURES_0_ERRORS_0_SKIPPED_1
CLEAN_FORCED_BUILD: PASS_171_TASKS_EXECUTED_EXIT_0
BUILD_PROVENANCE: MIXED_PROVENANCE
DEPLOYMENT_IN_ORIGINAL_IMPLEMENTATION_TASK: NOT_RUN
MINECRAFT_RUNTIME_IN_ORIGINAL_IMPLEMENTATION_TASK: NOT_RUN
COMMIT_OR_PUSH: NONE
```

A later user-run runtime observation does not rewrite the historical offline
verification block above. It adds the following current operational status:

```text
POST_RECORD_RUNTIME_OBSERVATION: USER_RUN_LOGS_REVIEWED
COMMAND_EXECUTION_AND_GET_EFFECT: PASSED
JAVA_AND_PYTHON_TERMINAL_EXACTLY_ONCE: PASSED
TERMINAL_OUTPUT_AND_TTS_EXACTLY_ONCE: PASSED
BROWSER_UI_HISTORICAL_OBSERVATION: FAIL_REPEATED_PRESENTATION
VERIFIED_FAULT_DOMAIN: PYTHON_GRADIO_ASYNC_UI_PRESENTATION
PYTHON_UI_FIX_STATUS: IMPLEMENTED_VERIFIED_OFFLINE
OFFLINE_GRADIO_EXACTLY_ONCE: PASS_REAL_POSTPROCESS_PREPROCESS_ROUND_TRIP
LIVE_BROWSER_UI_RETEST: NOT_RUN
POST_RECORD_CHANGE_SCOPE: PYTHON_GRADIO_PRESENTATION_ONLY
POST_GRADIO_STOP_STORE_HOME_LOG_REVIEW: POLICY_AND_ROLLOUT_BOUNDARIES_VERIFIED
TRUSTED_STOP_SINGLE_VISIBLE_RESPONSE_FIX: IMPLEMENTED_VERIFIED_OFFLINE
TRUSTED_STORE_HOME_STRONG_TERMINAL_FIX: IMPLEMENTED_VERIFIED_OFFLINE
SUPPRESSED_STREAM_GRADIO_COMPLETION_FIX: IMPLEMENTED_VERIFIED_OFFLINE
POST_GRADIO_FOLLOW_UP_SCOPE: PYTHON_LIFECYCLE_FEEDBACK_AND_LOCAL_CHAT_GRADIO_BOUNDARY
```

See the
[Gradio Routed-Response Duplicate Presentation Investigation](chatclef-gradio-routed-response-duplicate-presentation-investigation-2026-09-07.md)
for the sanitized runtime counts, verified representation mismatch, correction
contract, and required regression tests.

A second user-run review established one requested STOP policy adjustment and
one STORE_HOME rollout gap after the Gradio correction. Both are now
implemented as separate Python rollback units: accepted trusted STOP suppresses
its START publication while retaining its verified terminal, and only a
strictly correlated and validated trusted-translation STORE_HOME `COMPLETED`
or zero-work result earns strong success wording. The separate local-Chat
completion adapter closes the Gradio empty-stream input-lock boundary without
creating an assistant card. These changes are not a Gradio duplicate and not a
Java STORE_HOME failure; see the
[Python STOP Single-Response and STORE_HOME Verified-Terminal Pre-Change Contract](chatclef-python-stop-single-response-store-home-verified-terminal-pre-change-contract-2026-09-08.md).
This later follow-up does not rewrite the historical implementation and
verification results in this record. Its final selected Python integration run
passed 1226 tests with two skips and 7052 subtests; Ruff also passed.

The registered response profile set is closed over:

```text
attack
auto_deposit_trust
auto_deposit_trusted_list
auto_deposit_untrust
chatclef
deposit
deposit_all
equip
follow
food
gamer
gamma
get
give
goto
hero
idle
locate_structure
meat
overlay
reload_settings
resetmemory
scan
stop
store_home
자동보관등록
```

A registry drift test fails if a command is added or removed without an exact
phrase, lifecycle-kind, raw-form, and terminal-evidence profile decision.

## 2. Accepted input boundary

Lifecycle narration attaches only after the existing command submission is
accepted and the same active owner plus immutable response descriptor are
committed under the existing `command_lock`. Translation alone, failed send,
busy rejection, malformed raw text, an unregistered command, or an unsupported
item never earns a START response.

The raw `lavi_gui` path uses a bounded closed decoder for the 26 registered
forms. It rejects controls, chaining tokens, quotes, backslashes, malformed
numbers, excess arguments, and unknown commands. It does not reinterpret
arbitrary ChatClef text and does not broaden any Korean public-command policy.
Trusted LAVI Chat and final microphone input continue through their existing
translation and admission owners.

“All items” therefore means both:

- every item already admitted by the applicable Korean action policy; and
- a syntactically valid dynamic raw item target already accepted by the raw
  command path.

It does not mean that every row in the 591-entry target artifact is newly
enabled for every action. Known admitted targets use their bounded Korean
display label. A valid dynamic raw target without a trusted spoken label gets a
target-neutral sentence; its canonical ID may appear only in typed UI detail.

## 3. Immutable descriptor and lifecycle truth

The response descriptor freezes the validated command name, lifecycle kind,
response lifecycle kind, item/player/location/settings slots, quantity and its
semantics, acquisition verb class, source provenance, and request event
identity. It never stores an unvalidated phrase as trusted speech.

The existing ordinary command owner remains the only active-command truth.
The generalized feedback lifecycle is a synchronized participant under the same
external command lock; it is not a second command owner. The previous crafting
tracker API remains as a compatibility facade over the generalized lifecycle.
The specialized STOP tracker remains independent and cannot lend its verified
terminal sentence to an ordinary raw `stop` command.

## 4. Response lifecycle kinds

The 26 commands are explicitly classified:

- `finite_task`: `attack`, `deposit`, `deposit_all`, `equip`, `food`, `gamer`,
  `get`, `give`, `goto`, `locate_structure`, `meat`, `store_home`.
- `persistent_task`: `follow`, `hero`, `idle`.
- `asynchronous_immediate`: `auto_deposit_trust`,
  `auto_deposit_trusted_list`, `auto_deposit_untrust`, `chatclef`, `gamma`,
  `overlay`, `reload_settings`, `resetmemory`, `scan`, `자동보관등록`.
- `specialized_control`: `stop`.

Finite commands publish START, answer admitted read-only STATUS questions, and
publish one terminal sentence. Persistent commands publish START and STATUS but
do not turn an ordinary `completed` callback into a false finite-success
sentence; only a typed stop/failure boundary may end their narration.
Asynchronous-immediate commands publish START then TERMINAL, except when an
already-staged terminal can be selected after the START permit becomes ready
and immediately before publication. That case emits one terminal-centered
`command_coalesced` response. A result arriving after that selection remains a
separate non-preempting terminal response.

START/STATUS permits are FIFO. START publication failure retires that response
lifecycle. STATUS publication failure retires only that query. A terminal fact
waits behind every earlier permit and one terminal CAS prevents duplicate
terminal-fact publication. Downstream sink receipts and browser presentation
have their own delivery boundaries.

## 5. Natural Korean rendering

Rendering is deterministic. No LLM is called to write lifecycle responses.
Command profiles select the verb and sentence boundary, while separate Korean
particle, quantity, subject, location, hunger-goal, and settings renderers own
grammar details. The acquisition verb from a trusted request is preserved when
it is supported:

```text
다이아 곡괭이 만들어줘
  -> 다이아 곡괭이 만들어 줄게
  -> 다이아 곡괭이 만드는 중이야
  -> 다이아 곡괭이 다 만들었어

다이아 곡괭이 구해줘
  -> 다이아 곡괭이 구해 올게
  -> 다이아 곡괭이 구하는 중이야
  -> 다이아 곡괭이 다 구했어
```

Raw GET has no trusted Korean source verb, so it uses the neutral acquisition
wording. Other families render their validated subject, player, coordinates,
structure, destination, setting value, or quantity only when that slot is
trusted and bounded.

## 6. Read-only status questions

Route ordering is STOP classification, then lifecycle STATUS classification,
then the ordinary busy path. A status question never submits a command. The
classifier accepts addressed generic/family questions and exact target-qualified
questions while allowing unrelated prefixless conversation to fall through.

A positive progress sentence requires all of the following under the command
lock:

```text
same active owner
same command family and any stated target
status=running
result_reason=dispatch_started
strictly increasing positive integer evidence_sequence
```

Callback/quiescence observations, UNKNOWN quarantine, stale identity, target
mismatch, and merely accepted ownership do not become “하는 중이야”. The route
uses a conservative unable-to-confirm sentence instead.

## 7. Terminal evidence policy

Outer WebSocket, session, connection-generation, request, message, and active
owner reconciliation must accept the result before any terminal fact can be
created. `completed` alone is never gameplay success.

One current strong effect oracle is the generalized single-target GET profile.
Java records inventory-plus-cursor counts before dispatch and at matching task
completion, resolves the TaskCatalogue target match set, and emits the closed
`fabric_chatclef_get_acquire_delta` version-1 payload. Python requires exact
profile/descriptor agreement, `matching_task_finished`,
`callback_plus_matching_user_task_event`, authoritative same-binding counts,
and a delta at least as large as the requested acquisition quantity. The exact
`get diamond_pickaxe 1` command additionally keeps its legacy flat fields for
wire compatibility.

The later STORE_HOME follow-up adds a second, narrower strong effect profile.
It is limited to trusted translation from LAVI Chat or final microphone input,
requires exact active-command identity and common reconciliation, and accepts
only the closed typed `COMPLETED` payload with `goal_satisfied=true`,
`remaining_stacks=0`, and the remaining strict lifecycle fields. Multi-target
or bracket GET and every other command profile continue to use cautious
terminal narration unless a command-specific oracle exists. New strong
profiles for deposit, equipment, movement, hunger, or settings must remain
additive observation projections with their own tests.

Trusted Korean STOP continues to use its separately verified STOP-control
identity and may say `멈췄어` exactly once at terminal. Its accepted START is
suppressed. An ordinary raw STOP descriptor is unchanged and cannot borrow that
proof.

## 8. Java observation boundary

Only LAVI-owned Fabric bridge code changed. No upstream ChatClef/AltoClef task,
selection, retry, input, Baritone goal/path, dependency, or version changed.

Java now uses one execution-wide lifecycle sequence source:

```text
initial running/dispatch_started -> evidence_sequence=1
later nonterminal observations   -> evidence_sequence=2+
```

A shared JSON fixture is serialized through the real Java result/envelope
producer and consumed through the real Python DTO, result handler,
reconciliation, and lifecycle path. Boolean, missing, zero, negative, duplicate,
and decreasing sequences remain fail-closed.

The GET effect code is responsibility-split under
`bridge/command/result/effect/get/**`: command decoding, catalogue resolution,
count observation, immutable evidence, payload projection, and tracker lifetime
have separate types. Observation failure produces unavailable evidence instead
of fabricated success.

## 9. UI and TTS delivery

The conversational body never contains `[Minecraft]`. UI provenance is a typed
Minecraft badge plus a bounded canonical JSON detail record. The detail
projector accepts only validated descriptor slots; raw input and unknown fields
are rejected. Canonical item IDs are never copied into spoken text.

Lifecycle TTS is non-preempting and does not interrupt a newer LLM response.
Queue acceptance and playback are different immutable receipts. Reservation,
commit, synchronous-callback buffering, playback token, clear epoch, bounded
active-state registry, interruption, stale callback, and worker-exception paths
are serialized by one facade lock and covered by focused race tests.

UI receipt means accepted presentation/yield, not proof of a browser paint.
Failed async UI drain retains the item for a later retry. History reset and
shutdown clear pending presentation state without changing Minecraft command
ownership.

The later live observation proved an implementation defect inside that async
retry boundary. The queue accepted one terminal item, but Gradio normalized its
plain-string content into a text-block list before the confirmation callback.
Because the drain identity includes `str(content)`, it did not recognize the
already displayed item, did not acknowledge the pending prefix, and appended it
again on the 0.25-second timer. The defect and applied correction record are
recorded in the
[duplicate-presentation investigation](chatclef-gradio-routed-response-duplicate-presentation-investigation-2026-09-07.md).
It is not a Java terminal, command-CAS, effect-proof, output, or TTS duplicate.

The 2026-09-08 correction replaces event-ID-plus-`str(content)` matching with a
versioned lifecycle presentation token in Gradio's preserved `metadata.id` and
a strict string/single-text-block integrity fingerprint. It preserves delayed
acknowledgement: the first callback proposes an update, the next normalized
callback confirms and removes the exact FIFO prefix, and later callbacks skip.

## 10. Responsibility and folder split

The implementation follows the repository rule that independent responsibilities
must not accumulate in one new class:

```text
input/status/command_lifecycle/
  classification/   resolution/   routing/

response/command_lifecycle/
  profiles/   grammar/arguments/   grammar/boundary/
  selection/  publication/         coalesced/

transport/command_feedback/lifecycle/
  admission/  descriptor/{raw_form,trusted_translation,validation}/
  evidence/   kind/   publication/   result/   state/   status/   terminal/

presentation/command_lifecycle/
ui/feedback/{provenance,routing}/
tts_core/delivery/lifecycle_response/{state,receipt}/

Java bridge/command/result/effect/get/
```

Stable compatibility facades remain at the old crafting and connection APIs,
but all new parsing, rendering, effect evaluation, publication ordering,
presentation, and receipt state belongs to focused collaborators.

## 11. Verification

Final offline integration verification completed on 2026-09-07:

```text
Focused command/crafting/STOP/structure Python:
  187 passed, 3978 subtests passed

Minecraft Python:
  1032 passed, 2 skipped, 6891 subtests passed

Adjacent LLM/app/TTS Python:
  125 passed, 23 subtests passed

Full Python repository suite, outside the named-pipe-restricted sandbox:
  2285 passed, 4 skipped, 7945 subtests passed
  1 pre-existing HEAD-hash contract subfailure
  failing contract target: FabricChatClefCommandDispatcher.java
  worktree target equals HEAD: yes

Fabric 1.20.1 full Java tests:
  248 XML suite files
  843 tests, 0 failures, 0 errors, 1 skipped
  :1.20.1:test --rerun-tasks --no-daemon
  46 tasks executed, BUILD SUCCESSFUL

Clean forced build:
  launched in new visible PowerShell window, PID 13724
  repository-owned script: codex-chatclef-build.ps1
  Gradle command: .\gradlew.bat clean build --rerun-tasks
  JDK: Eclipse Adoptium 21.0.12.8
  171 tasks executed, BUILD SUCCESSFUL in 6m 12s
  script/Gradle exit code: 0

Fresh 1.20.1 artifact:
  file: versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar
  bytes: 8412545
  SHA-256: BD051BEC090D299C086EE9A65CD3EEE52605F5C49DB5E63E287D54C3A34E150B

Build log:
  codex-build-logs/chatclef-fabric-1.20.1-build-20260907-221202.log
```

### 11.1 Later user-run runtime observation

The subsequent `LAVI_TEST_Fabric01` run proved one accepted command, one Java
dispatch, one matching Task completion, one authoritative GET delta from one to
two diamond pickaxes, one Java terminal send, one Python terminal fact, one UI
queue enqueue, and one TTS queue/playback. It also reproduced repeated copies
of the same terminal card in Chatbot.

That pre-fix runtime conclusion is therefore split by boundary:

```text
MINECRAFT_COMMAND_AND_EFFECT: PASS
JAVA_TERMINAL_SEND: PASS_EXACTLY_ONCE
PYTHON_TERMINAL_PUBLICATION: PASS_EXACTLY_ONCE
TERMINAL_OUTPUT_AND_TTS_DELIVERY: PASS_EXACTLY_ONCE
HISTORICAL_GRADIO_BROWSER_PRESENTATION: FAIL_REPEATED_CARD
```

This runtime observation is evidence for the separately documented Python UI
defect. It is not a runtime pass for browser presentation and does not convert
the original mixed-provenance build into isolated source proof.

The repository-root Python suite's `HEAD`-hash contract failure is not a
worktree regression: the dispatcher source is unchanged relative to `HEAD`,
while the historical test constant already disagrees with that `HEAD` blob.
This statement does not classify the Gradio UI defect as pre-existing. The
Windows SQLite named-pipe integration test that could not create a pipe inside
the restricted sandbox passed when rerun outside it.

The clean build necessarily compiles the independently owned diagnostics
refactor already present in the dirty worktree. Its result is therefore
`MIXED_PROVENANCE` integration evidence, not proof that diagnostics and natural
responses share a source or rollback unit.

### 11.2 Python-only UI correction verification

The 2026-09-08 follow-up changed only the LAVI Python Gradio presentation
boundary and its tests. Real Gradio 6.18.0 postprocess/preprocess coverage now
confirms one queued terminal, one proposed history update, acknowledgement on
the following callback, an empty queue, and ten later `gr.skip()` results.
Same-event START/TERMINAL, same-text different-event, same-event same-text
different-response-kind, FIFO prefix, capacity, overlap, reset, malformed
content, and output/TTS-facing listener non-replay cases are covered. The
separate adjacent TTS regression covers the existing queue and playback
deduplication; the UI retry test does not claim physical audio playback.

```text
UI presentation responsibility tests: 28 passed, 19 subtests passed
Focused UI/input/builder integration: 48 passed, 19 subtests passed
Routed-response/input/UI/composition: 83 passed, 19 subtests passed
Minecraft/app/TTS adjacent: 1075 passed, 2 skipped, 6912 subtests passed
Full Python: 2305 passed, 4 skipped, 7964 subtests passed
Full Python residual: 1 pre-existing HEAD-hash contract subfailure
SQLite named-pipe test outside sandbox: 1 passed
Ruff focused check: PASS
Live browser smoke: NOT_RUN
Java tests and Gradle build for this Python-only follow-up: NOT_REQUIRED_NOT_RUN
```

## 12. Not run and rollback

Deployment, CurseForge JAR replacement, Minecraft launch, server connection,
and live-world execution were not part of the original implementation request
and remain `NOT_RUN` in its historical verification ledger. A later user-run
instance supplied read-only runtime evidence; this documentation task did not
deploy, launch, connect, or mutate a live world.

The natural-response changes form one top-level rollback family separate from
the diagnostic ledger/home-timeout refactor, even though the final build
compiled both dirty-worktree families. Inside the natural-response family, the
independently revertible profile, grammar, descriptor, status, lifecycle,
effect, presentation, and TTS units defined by the
[pre-change contract](chatclef-general-natural-korean-command-lifecycle-feedback-pre-change-contract-2026-09-07.md#13-independent-rollback-units)
remain separate. The applied Gradio identity correction is the narrow Python UI
presentation rollback unit defined by the duplicate-presentation investigation.
