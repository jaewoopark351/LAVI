<!-- 20260908_kpopmodder: Recorded the trusted-STOP terminal-only Gradio input-lock incident and read-only failure reproduction. -->
<!-- 20260908_kpopmodder: Kept the UI submit failure separate from Java STOP, command correlation, terminal publication, and TTS delivery. -->
<!-- 20260908_kpopmodder: Defined a Python-only diagnostic and correction contract without changing source or runtime state. -->
<!-- 20260908_kpopmodder: Reconciled the applied Gradio-only completion adapter and offline queue-lifecycle verification. -->
<!-- 20260908_kpopmodder: Reconciled two user-observed live recovery sequences with Python and Java logs. -->

# ChatClef Gradio Suppressed-Response Input Lock Investigation

Date: 2026-09-08

## 1. Status and scope

This document records the live symptom, sanitized runtime evidence, direct
source evidence, read-only local reproduction, exact failure mechanism, the
applied Python correction, and its verification contract for a trusted Korean
STOP submission that displayed its verified terminal response but left the
Gradio Chat input disabled.

```text
DOCUMENT_TYPE: INCIDENT_INVESTIGATION_WITH_IMPLEMENTATION_FOLLOWUP
DOCUMENT_STATUS: ROOT_CAUSE_FIXED_AND_VERIFIED_OFFLINE_AND_LIVE
ROOT_CAUSE_STATUS: VERIFIED_SOURCE_AND_READ_ONLY_LOCAL_GRADIO_REPRODUCTION
PRODUCTION_GRADIO_TRACEBACK_CAPTURE: NOT_AVAILABLE
LIVE_SYMPTOM_CAUSAL_MATCH: HIGH_CONFIDENCE

BACKEND_SCOPE: FABRIC_CHATCLEF_ONLY
FAULT_DOMAIN: LAVI_PYTHON_TO_GRADIO_LOCAL_CHAT_STREAM_BOUNDARY
TRIGGER: HANDLED_TRUSTED_STOP_WITH_SUPPRESS_RESPONSE_TRUE
VISIBLE_STOP_TERMINAL_RESULT: PASS_EXACTLY_ONCE
POST_STOP_CHAT_INPUT_RECOVERY: PASS_TWO_USER_OBSERVED_SEQUENCES
JAVA_STOP_RESULT: PASS_COMPLETED_STOPPED
JAVA_STOP_BARRIER: PASS_RELEASED
PYTHON_STOP_BARRIER: PASS_RELEASED
ORDINARY_COMMAND_OWNER_GATE: PASS_OPEN
WEBSOCKET_CONNECTION: PASS_REMAINED_CONNECTED
TTS_TERMINAL_DELIVERY: PASS_EXACTLY_ONCE

FIX_STATUS: GRADIO_ONLY_COMPLETION_ADAPTER_IMPLEMENTED
PRODUCTION_SOURCE_CHANGE: LAVI_OWNED_PYTHON_ONLY
TEST_SOURCE_CHANGE: LAVI_OWNED_PYTHON_ONLY
OFFLINE_GRADIO_STREAM_COMPLETION: PASS
OFFLINE_GRADIO_QUEUE_ITERATOR_COMPLETION: PASS
OFFLINE_SECOND_CHAT_SUBMIT: PASS
POST_FIX_LIVE_BROWSER_RETEST: PASS_TWO_USER_OBSERVED_SEQUENCES
POST_FIX_LIVE_LOG_CORRELATION: PASS_TWO_POST_STOP_CHAT_SUBMITS
PRECEDING_READ_ONLY_MINIMAL_REPRODUCTION: PASS
DEPENDENCY_OR_GRADIO_VERSION_CHANGE: NONE
JAVA_SOURCE_CHANGE: NONE
JAVA_BUILD: NOT_REQUIRED_PYTHON_ONLY
DEPLOYMENT: NOT_RUN
MINECRAFT_LIVE_SESSION: USER_EXECUTED_AND_REVIEWED_READ_ONLY
EXISTING_USER_RUNTIME_LOGS: REVIEWED_READ_ONLY
COMMIT_SCOPE: GRADIO_ONLY_SELECTIVE_ROLLBACK_UNIT
```

The investigated defect is not the earlier asynchronous duplicate-card
defect. The earlier defect concerned presentation identity after a terminal
item had entered the asynchronous UI queue. This defect occurs in the current
Chat submit stream after an accepted response is intentionally suppressed.

Related records:

- [STOP Single-Response and STORE_HOME Verified-Terminal Contract](chatclef-python-stop-single-response-store-home-verified-terminal-pre-change-contract-2026-09-08.md)
- [Gradio Routed-Response Duplicate Presentation Investigation](chatclef-gradio-routed-response-duplicate-presentation-investigation-2026-09-07.md)
- [General Natural Korean Command Lifecycle Feedback Implementation Record](chatclef-general-natural-korean-command-lifecycle-feedback-implementation-record-2026-09-07.md)
- [Fabric ChatClef Bridge Protocol V1](fabric-chatclef-bridge-protocol-v1.md)
- [Minecraft Backend Separation](minecraft-backend-separation.md)

## 2. Observed symptom

The user submitted a trusted Korean STOP from LAVI Chat while a translated
Minecraft command was active. The Chatbot showed only the intended terminal
Minecraft card:

```text
멈췄어
```

There was no visible accepted START card such as `멈출게`, so the new
terminal-only product policy succeeded at the visible-response boundary.
After the terminal card appeared, however, the Chat textbox remained disabled
and its submit control remained the square Gradio stop button. A later input
could not start a new Chat submission.

This combination is important:

```text
STOP command and terminal lifecycle complete
  + terminal card appears once through the asynchronous UI sink
  + original Chat submit stream does not complete normally
  -> Gradio input remains in its in-flight presentation state
```

## 3. Sanitized runtime evidence

### 3.1 Supplied files are non-empty

The supplied wildcard and exact `LAVI_TEST_Fabric01` paths were inspected
read-only. Other matching CurseForge instances contained older logs;
`LAVI_TEST_Fabric01` was the current live instance.

At the 2026-09-08T12:47:41+09:00 size snapshot:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
  2,831,849 bytes; actively growing

C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\instance_audit.txt
  2,707,651 bytes

C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\stdout-logs.txt
  3,153,920 bytes; actively growing

C:\Vtuber_Souorce_Code\LAVI\logs\20260908_121023_log.txt
  107,220 bytes
```

File sizes are recorded only to close the reported zero-byte ambiguity. They
are snapshots, not lifecycle invariants. `instance_audit.txt` was last written
at 12:10:28, before the 12:13:43 incident, so it proves the selected instance
and launch context but is not used as STOP lifecycle evidence.

### 3.2 LAVI Python timeline

The active LAVI log records the second reproduced STOP as follows:

```text
line 196       trusted Chat STOP classified as valid_exact_stop
lines 197-199  STOP submitted and admission accepted
line 200       interrupted GET result accepted as cancelled; active owner cleared
line 201       STOP completed/stopped; Python barrier released; ordinary gate open
line 202       one stop_terminal response containing "멈췄어"
line 203       one output-listener delivery
line 205       one asynchronous ui_presentation enqueue
line 206       one TTS-queue enqueue
lines 208-215  one TTS synthesis/playback lifecycle
line 216       IS_AI_SPEAKING=False
```

The process continued producing ScreenVision observations after the STOP, but
there was no later `chat_submit`, routing decision, command gate, or command
submission. This excludes a process-wide Python deadlock, a retained TTS busy
flag, and a later Python-to-Java command rejection. The later input did not
enter the local Chat submission path.

The same run contains an earlier trusted STOP at 12:13:07 followed by another
GET admission at 12:13:37. That proves the Java side was capable of accepting
commands after a completed STOP. The logs do not prove how the browser was
recovered between those events; the 30-second interval must not be described
as an automatic timeout.

### 3.3 Java and bridge timeline

The active `latest.log` and the XML-wrapped mirror in `stdout-logs.txt` agree:

```text
12:11:48  one WebSocket connection and accepted generation-1 handshake
12:13:37  GET iron_pickaxe request queued and dispatched
12:13:43  matching STOP control admitted for that active request
12:13:43  Java command context unbound; queue active request became empty
12:13:43  STOP completed/stopped; target changed active -> retired
12:13:44  control result sent; Java barrier released; ordinary gate open
afterward no new queue admission or command execution from LAVI Chat
afterward periodic heartbeats continued with no disconnect or bridge error
```

Representative source locations in the growing logs were `latest.log` lines
1718-1784 and `stdout-logs.txt` lines 4937-5135. Later heartbeats were observed
through at least `stdout-logs.txt` line 5216.

Therefore the first failing boundary is upstream of the Python-to-Java enqueue.
It is not the STOP tracker, command barrier, session correlation, terminal CAS,
Java command queue, Minecraft TaskRunner, or WebSocket connection.

## 4. Verified source and reproduction evidence

### 4.1 LAVI suppression path

The accepted trusted STOP path is intentionally terminal-only:

```text
StopControlRouteOutcomeBuilder.submitted()
  -> accepted result retains successful submission state
  -> response_text=""

TrustedKoreanResponseAuthorizer.authorize()
  -> empty handled response
  -> suppress_response=True
  -> no response emission capability

LlmPredictionDispatchCoordinator.predict()
  -> handled and suppress_response
  -> return before any yield
```

The reviewed source boundaries are:

```text
plugins/Minecraft/fabric/chatclef/input/stop/routing/
  stop_control_route_outcome_builder.py:36-60

plugins/Minecraft/fabric/chatclef/input/routing/trusted_korean/response/
  trusted_korean_response_authorizer.py:13-17,36-44

llm_core/input_routing/
  llm_prediction_dispatch_coordinator.py:36-50
```

At the domain level, zero visible responses is correct. At the Gradio stream
protocol level, however, this is an empty synchronous generator.

### 4.2 Installed Gradio behavior

The installed dependency is Gradio 6.18.0. `LocalChatInterfaceFactory`
registers the bound generator method with `gr.ChatInterface`, so Gradio uses
`ChatInterface._stream_fn()`.

The exact installed path behaves as follows:

```text
gradio/chat_interface.py:945-965
  convert the sync generator to SyncToAsyncIterator
  await the first response with utils.async_iteration(generator)

gradio/utils.py:848-854
  next(iterator) receives StopIteration from the empty sync generator
  convert it to StopAsyncIteration

gradio/chat_interface.py:975-976
  catch StopIteration only
  do not catch the actual StopAsyncIteration
```

The unhandled async-iteration termination escapes from the async generator as:

```text
RuntimeError: async generator raised StopAsyncIteration
```

### 4.3 Read-only minimal reproduction

A read-only probe used the repository virtual environment, an empty sync
generator, and the installed `ChatInterface._stream_fn()` without starting
Minecraft or modifying source. It produced this exact boundary trace:

```text
gradio/chat_interface.py:965
  first_response = await utils.async_iteration(generator)
gradio/utils.py:886
  return await anext(iterator)
gradio/utils.py:868
  return await anyio.to_thread.run_sync(...)
gradio/utils.py:854
  raise StopAsyncIteration() from None
StopAsyncIteration

RuntimeError: async generator raised StopAsyncIteration
```

The application suppression path was separately enumerated and produced zero
application yields. This validates the same trigger state used by accepted
trusted STOP.

The production LAVI log does not capture Gradio queue stderr, and no browser
console trace was supplied. The production occurrence of that traceback is
therefore not claimed as directly logged. The source path, installed dependency
path, exact read-only reproduction, absence of later `chat_submit`, and observed
square stop button establish a high-confidence causal match.

## 5. Exact failure mechanism

The failure sequence is:

```text
trusted Chat STOP accepted
  -> STOP START text intentionally empty
  -> trusted authorizer suppresses every START sink and capability
  -> local Chat generator yields zero chunks
  -> Gradio requests the mandatory first stream chunk
  -> sync iterator exhaustion becomes StopAsyncIteration
  -> Gradio catches only StopIteration
  -> RuntimeError escapes the ChatInterface stream
  -> submit completion/input-restoration lifecycle does not finish normally
  -> textbox remains disabled with the square stop control
```

The terminal response still appears because it has a different delivery path:

```text
verified Java STOP terminal callback
  -> Python STOP terminal validator
  -> routed output listener
  -> asynchronous UI presentation queue
  -> Gradio timer appends "멈췄어" to Chatbot history
```

Consequently, a visible terminal card is not proof that the original Chat
submit callback completed. The terminal sink succeeded while the direct
current-input stream failed its transport-level completion contract.

## 6. Why existing tests passed

The existing STOP and routed-input tests correctly assert the product/domain
contract:

```text
accepted STOP response_text is empty
suppress_response is true
no START output, Chat UI card, TTS item, or emission capability is created
list(predict_wrapper(...)) == []
one later verified STOP terminal is published
```

They do not pass that empty generator through the real installed
`ChatInterface._stream_fn()`. The existing local Chat factory integration test
uses a generator that emits `first` and `second`; it does not characterize a
zero-chunk generator. Therefore the Python suites could pass while the live
Gradio callback failed.

Reviewed test boundaries:

```text
tests/llm_core/chat_input/test_local_chat_interface_factory.py:29-60
  real _stream_fn, but only a non-empty two-chunk stream

tests/llm_core/input_routing/test_routed_input_dispatch_coordinator.py:415-429
  domain suppression represented as an empty result list

tests/minecraft_chatclef/lavi_input/
  test_trusted_korean_chat_voice_integration.py:192-221
  trusted STOP has zero START emissions and one terminal publication
```

Required missing characterization:

```text
handled suppressed local-Chat route
  -> real LocalChatPredictionEntrypoint
  -> real LocalChatInterfaceFactory
  -> installed ChatInterface._stream_fn
  -> normal request completion
  -> no assistant START card
  -> textbox can accept the next input
```

## 7. Ownership and excluded causes

| Boundary | Ownership | Finding |
| --- | --- | --- |
| STOP submission, tracker, barrier, terminal validation | LAVI-owned Fabric Python transport | Completed correctly; preserve |
| Java STOP execution and command retirement | Fabric ChatClef Java bridge | Completed correctly; no change |
| Terminal output/UI/TTS sinks | LAVI-owned routed-response delivery | Delivered exactly once; preserve |
| `LlmPredictionDispatchCoordinator` suppression | LAVI-owned input-routing policy | Correct domain policy; creates zero-chunk UI stream |
| Local Chat generator-to-Gradio adaptation | LAVI-owned Python UI integration | Correction boundary |
| Gradio 6.18.0 initial stream iteration | Pinned third-party dependency behavior | Reproduced trigger; do not patch vendored environment |

No change is required in Java, the bridge protocol, STOP correlation, terminal
CAS, Minecraft command admission, ChatClef/AltoClef, Baritone, Forge/MineMind,
TTS, or the asynchronous presentation identity algorithm.

## 8. Completed live verification and remaining observability

The user performed the prescribed live sequence twice without pressing the
square cancel control, refreshing the browser, or restarting LAVI. The user
observed the submit control return automatically, submitted the next command,
and observed no blank assistant card. The read-only Python and Minecraft log
review correlates both sequences:

```text
sequence 1
  13:34:53  accepted STOP completed/stopped
  13:34:53  one terminal "멈췄어" published
  13:34:59  next LAVI Chat submit accepted: get iron_axe 1
  13:34:59  matching Java command queued and dispatched
  13:35:37  next command completed and terminal response published

sequence 2
  13:35:50  accepted STOP submitted
  13:35:51  STOP completed/stopped; Python and Java barriers released
  13:35:51  one terminal "멈췄어" published
  13:44:04  next LAVI Chat submit accepted: get pumpkin_pie 1
  13:44:04  matching Java command queued and dispatched
  13:47:29  next command completed and terminal response published
```

Across both sequences, the LAVI log contains no `멈출게` translation output,
and each STOP has one terminal output, one UI enqueue, one TTS enqueue, and one
TTS playback receipt. The Java log records each STOP target as retired, the
control result as sent, the Java barrier as released, and the ordinary owner
gate as open. Heartbeats continued without a disconnect.

The existing file logger does not record the browser button DOM transition or
the Gradio queue's stderr directly. Those two facts therefore combine user
observation with correlated post-STOP submissions rather than a dedicated UI
telemetry event. No additional live action is required for this correction.

If the symptom recurs, an explicitly authorized diagnostic run may capture
Python stdout and stderr in a new timestamped repository log without
overwriting an existing file:

```powershell
$stamp = Get-Date -Format yyyyMMdd_HHmmss
.\venv\Scripts\python.exe -B .\main.py 2>&1 |
    Tee-Object -FilePath ".\logs\gradio-debug-$stamp.txt"
```

The generated runtime log is investigation evidence and must not be committed.

Any temporary or permanent diagnostic must be bounded to one submission
boundary and record only safe fields such as:

```text
event_id
source
handled
suppress_response
application_yield_count
gradio_completion_kind
exception_class
generator_closed
```

Do not log complete conversation text, prompts, credentials, raw microphone
content, every 0.25-second timer tick, or an unbounded exception loop. Do not
modify `venv/Lib/site-packages/gradio` to add diagnostics.

## 9. Python-only correction contract

The product requirement remains unchanged:

```text
accepted trusted STOP
  -> no START UI card
  -> no START output delivery
  -> no START TTS
  -> no START asynchronous UI enqueue
  -> no START emission capability
  -> one verified terminal "멈췄어"
```

The correction must distinguish that product-level absence from the Gradio
transport requirement to complete one submitted stream. The LAVI-owned local
Chat/Gradio boundary must provide a Gradio-compatible completion that:

1. causes the submit event to complete normally;
2. creates no visible assistant card or blank card;
3. does not enter output, TTS, or asynchronous presentation sinks;
4. restores the textbox and normal submit control;
5. does not change `suppress_response=True` domain semantics;
6. does not convert rejected STOP or unrelated empty failures into success;
7. does not depend on a Gradio version change or a site-packages patch; and
8. remains isolated from voice-input draining, raw/legacy STOP, and ordinary
   command responses.

A transport-level no-op representation, including an empty message-list
candidate, is not accepted by inspection alone. It must be passed through the
real Gradio `_stream_fn`, Chatbot postprocess/preprocess, and browser callback
history path to prove that it is invisible and completes normally.

Because local Chat generation and Gradio protocol completion are separate
responsibilities, any new adapter belongs in a focused LAVI-owned Chat/Gradio
boundary. Do not put Gradio-specific completion policy into Minecraft STOP
transport, terminal validation, or Java code.

## 10. Regression and runtime verification contract

The smallest complete verification matrix is:

1. accepted trusted Chat STOP produces zero product START emissions;
2. accepted trusted final-microphone STOP retains its current off-screen drain
   and exactly-once terminal behavior;
3. rejected or disconnected STOP retains its one immediate error response;
4. raw/legacy STOP behavior is unchanged;
5. the real Gradio `_stream_fn` completes without `StopAsyncIteration` or
   `RuntimeError` for a suppressed handled route;
6. the Gradio-compatible completion produces no assistant card after a real
   Chatbot serialization round trip;
7. the textbox becomes interactive and the submit icon returns;
8. a new Chat input immediately after `멈췄어` reaches `chat_submit`;
9. an ordinary Minecraft command after STOP reaches Java queue admission;
10. terminal arrival before direct-submit completion preserves one terminal;
11. terminal arrival after direct-submit completion preserves one terminal;
12. repeated UI timer drains neither erase nor duplicate the terminal;
13. UI retry/cancel does not replay output or TTS; and
14. no LLM recall, automatic retry, STOP resubmission, or command resubmission
    occurs.

Verification order:

```text
empty-generator Gradio characterization
  -> focused LAVI-owned completion adapter tests
  -> real ChatInterface and Chatbot round-trip tests
  -> STOP Chat/final-microphone source matrix
  -> routed-response and asynchronous terminal race tests
  -> Minecraft Python regression
  -> separately authorized live Gradio smoke
```

No Java test or Gradle build proves this correction. Java build status remains
`NOT_REQUIRED` unless a separately approved Java change is introduced.

## 11. Rejected workarounds and non-goals

The following are not root-cause corrections:

- restoring the visible `멈출게` START response;
- yielding an unverified empty string or `None` that may create a blank card or
  another Gradio conversion error;
- swallowing the `RuntimeError` without completing the UI event;
- retrying the Chat submission or STOP command;
- adding a delay, timeout, polling loop, or automatic browser refresh;
- clearing terminal presentation history;
- weakening STOP correlation, validation, barrier, or terminal CAS;
- changing TTS timing or interrupt behavior;
- patching files inside `venv/Lib/site-packages`;
- changing the Gradio dependency version during this correction;
- changing Java, Fabric, ChatClef, AltoClef, Baritone, or Minecraft behavior;
- adding Forge/MineMind code or shared backend runtime state.

The square stop button or a browser refresh may be used as a manual recovery
during investigation, but neither is verification of a fixed lifecycle.

## 12. Provenance and rollback boundary

The inspected repository was:

```text
root: C:\Vtuber_Souorce_Code\LAVI
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: c5968582a826e7fb10b857e7c50388c5e3ec93d2
worktree: dirty; existing source and documentation changes preserved
```

The initial documentation pass created this investigation record. The follow-up
applied only the LAVI-owned local Chat/Gradio correction and its tests. The user
later ran and observed the two live recovery sequences recorded above; their
logs were reviewed read-only. This change set does not deploy a JAR, build
Java, change a dependency, or modify Minecraft runtime files.

The correction rollback unit is the LAVI-owned local Chat/Gradio
suppressed-stream completion adapter, its factory connection, and its focused
tests. It remains separate from:

- the trusted STOP terminal-only policy;
- STOP tracker, barrier, and terminal delivery;
- STORE_HOME evidence evaluation and rendering;
- asynchronous presentation identity/deduplication;
- TTS delivery and interruption;
- Java bridge or Minecraft Task behavior; and
- dependency or runtime-version changes.

Rolling back the completion adapter may reproduce the input lock but must not
automatically restore the visible STOP START response or remove verified
terminal delivery.

## 13. 2026-09-08 Python implementation follow-up

The correction is implemented at the local Chat/Gradio boundary:

```text
llm_core/chat_input/gradio/
  __init__.py
  gradio_local_chat_stream_completion_adapter.py

llm_core/chat_input/local_chat_interface_factory.py
  registers the adapter generator with gr.ChatInterface
```

`GradioLocalChatStreamCompletionAdapter` forwards every ordinary product chunk
unchanged. If and only if the wrapped generator reaches normal exhaustion
without yielding a product chunk, it yields one fresh empty list. In the exact
Gradio 6.18.0 Local Chat contract, the first stream result is routed to a hidden
State and the empty list expands to zero assistant messages. It therefore
advances the Gradio generator lifecycle without creating a UI card.

The adapter uses no shared mutable state. Exceptions before or after a product
chunk propagate unchanged. Closing the outer generator propagates
`GeneratorExit`, closes the wrapped generator, and does not manufacture an
empty-success value. The adapter does not call output, TTS, presentation,
Minecraft, transport, or LLM APIs.

Characterization rejected the other scalar candidates:

```text
None -> AttributeError in Gradio message normalization
""   -> serialized empty assistant message and possible blank card
[]   -> user-only history and normal stream completion
```

Offline verification recorded:

```text
adapter + real Gradio focused tests: 11 passed
llm_core/chat_input:                  19 passed, 2 subtests
STOP/terminal/voice/UI selection:     74 passed, 468 subtests
llm_core:                             110 passed, 21 subtests
app_core:                             9 passed
Minecraft Python:                    1060 passed, 2 skipped, 7010 subtests
Ruff for the changed Python files:    PASS
compileall for the changed Python:    PASS
Java build:                           NOT_REQUIRED_NOT_RUN
deployment:                           NOT_REQUIRED_NOT_RUN
live browser smoke after correction:  PASS, two user-observed sequences
live Python/Java log correlation:     PASS, two post-STOP submissions
```

The repository-wide Python run reached `2343 passed`, `4 skipped`, and `8083`
subtests, with the same two independently classified limitations already
recorded by the parent contract: one HEAD-relative Java source-hash assertion
in the dirty worktree and one sandbox-only Windows named-pipe permission
failure. Neither failure exercises the new adapter.
