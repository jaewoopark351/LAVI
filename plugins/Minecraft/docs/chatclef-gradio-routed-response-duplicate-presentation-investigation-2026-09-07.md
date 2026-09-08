<!-- 20260907_kpopmodder: Recorded the verified Python/Gradio routed-response duplicate-presentation boundary before any source fix. -->
<!-- 20260907_kpopmodder: Kept the UI defect separate from Minecraft command execution, terminal correlation, effect proof, and TTS delivery. -->
<!-- 20260908_kpopmodder: Recorded the responsibility-split Python correction and offline Gradio round-trip verification. -->

# ChatClef Gradio Routed-Response Duplicate Presentation Investigation

Date: 2026-09-07

## 1. Status and scope

This document began as the live symptom, sanitized log evidence, direct source
evidence, read-only reproduction, exact failure mechanism, and pre-change
correction contract. The original documentation-only task changed no source or
runtime state. The explicit 2026-09-08 follow-up implemented the Python UI
correction and is recorded in Section 11 without rewriting the historical
runtime evidence.

```text
DOCUMENT_STATUS: IMPLEMENTATION_FOLLOWUP_VERIFIED_OFFLINE
ROOT_CAUSE_STATUS: VERIFIED
FAULT_DOMAIN: PYTHON_GRADIO_ASYNC_UI_PRESENTATION
REPRODUCTION_BACKEND_SCOPE: FABRIC_CHATCLEF_ONLY
FAULT_IMPLEMENTATION_SCOPE: SHARED_LAVI_PYTHON_ROUTED_RESPONSE_UI
REPRODUCED_PROFILE: GET_DIAMOND_PICKAXE_1_CRAFT_INTENT
AFFECTED_SCOPE: EVERY_TEXT_PRESENTATION_RENDERED_BY_THE_ROUTED_RESPONSE_UI_ADAPTER_AND_DRAINED_THROUGH_GRADIO
JAVA_COMMAND_EXECUTION_COUNT: 1
JAVA_TERMINAL_SEND_COUNT: 1
PYTHON_TERMINAL_PUBLICATION_COUNT: 1
TERMINAL_UI_PRESENTATION_ENQUEUE_COUNT: 1
TERMINAL_TTS_QUEUE_ACCEPTANCE_COUNT: 1
TERMINAL_TTS_PLAYBACK_COUNT: 1
BROWSER_UI_HISTORICAL_OBSERVATION: FAIL_REPEATED_PRESENTATION
OFFLINE_GRADIO_EXACTLY_ONCE: PASS_REAL_POSTPROCESS_PREPROCESS_ROUND_TRIP
LIVE_BROWSER_UI_EXACTLY_ONCE_RETEST: NOT_RUN
RUNTIME_EVIDENCE: USER_RUN_LOGS_AND_SCREENSHOT_REVIEWED_READ_ONLY
READ_ONLY_GRADIO_ROUND_TRIP_REPRODUCTION: PASSED
FIX_STATUS: IMPLEMENTED_VERIFIED_OFFLINE
CODE_OR_TEST_CHANGE_IN_FOLLOWUP: PYTHON_UI_PRESENTATION_AND_TESTS
DEPENDENCY_OR_GRADIO_VERSION_CHANGE: NONE
BUILD_IN_FOLLOWUP: NOT_REQUIRED_PYTHON_ONLY
DEPLOYMENT_IN_FOLLOWUP: NOT_RUN
MINECRAFT_LAUNCH_IN_FOLLOWUP: NOT_RUN
COMMIT_OR_PUSH: NONE
```

The observed sentence was `다이아 곡괭이 다 만들었어`, but the defect is not
specific to that item, GET, crafting wording, or Minecraft. It affects an
asynchronous response after it has been accepted by
`RoutedResponseUiPresentationQueue` and is later merged into Gradio Chatbot
history. A synchronous START response returned directly through the current
Chat UI yield path is not the failing boundary observed here.

The authoritative related records remain:

- [General Natural Korean Command Lifecycle Feedback Implementation Record](chatclef-general-natural-korean-command-lifecycle-feedback-implementation-record-2026-09-07.md)
- [General Natural Korean Command Lifecycle Feedback Pre-Change Contract](chatclef-general-natural-korean-command-lifecycle-feedback-pre-change-contract-2026-09-07.md)
- [Natural Korean Crafting Lifecycle Feedback Contract](chatclef-natural-korean-crafting-lifecycle-feedback-pre-change-contract-2026-09-07.md)
- [Fabric ChatClef Bridge Protocol V1](fabric-chatclef-bridge-protocol-v1.md)
- [ChatClef Command Lifecycle and Threading](chatclef-command-lifecycle-and-threading.md)

This investigation does not change the protocol or command-lifecycle
contracts. Their one-command and one-terminal boundaries behaved as specified.

## 2. Observed symptom

One accepted `get diamond_pickaxe 1` command produced one successful terminal
sentence. The Chatbot showed the same Minecraft-badged terminal card repeatedly;
four copies were visible in the supplied screenshot. Four is only the number
accumulated when the screenshot was taken. It is not a configured retry limit
or a four-publication policy.

The repeated visual cards did not cause:

- another Minecraft command submission;
- another ChatClef Task;
- another Java terminal result;
- another Python terminal fact;
- another LLM call;
- another output-listener publication; or
- another TTS queue acceptance or playback.

The presentation queue retained one logical pending item while Chatbot history
could grow on successive timer callbacks.

## 3. Sanitized runtime evidence

### 3.1 Supplied CurseForge logs

The supplied wildcard resolved to four instance directories and twelve log
files. Every `latest.log`, `instance_audit.txt`, and `stdout-logs.txt` file was
non-empty. `LAVI_TEST_Fabric01` was the only currently updating instance.

At the 23:16:25 KST observation point its files were:

```text
latest.log          2,524,984 bytes
instance_audit.txt  2,688,273 bytes
stdout-logs.txt     2,793,472 bytes
```

The two live output files continued growing after that snapshot. File size is
recorded only to close the reported zero-byte ambiguity; it is not lifecycle
evidence.

The current `latest.log` showed:

```text
line 786-787   one generation-1 connection and handshake
line 1058      one command queue event
line 1060      one dispatch
line 1616-1619 one matching TaskFinishedEvent publication/observation sequence
line 1627      one matching_task_finished terminal decision
line 1628      one terminal send start, attempt=1
line 1637      one command-context unbind after send completion
line 1638      one terminal_result_sent with lifecycle_cleared=true
```

Between the 23:05:59 handshake and the investigated 23:08:20 successful
terminal send, there was no disconnect, reconnect, terminal send failure, or
later terminal send attempt beyond `attempt=1`. One running Minecraft Java
process and one established Minecraft-to-LAVI bridge connection were observed
during that command window. A later 23:20:46 connection reset occurred after
the complete investigated lifecycle and is unrelated to the repeated cards.

`stdout-logs.txt` contains an XML-wrapped mirror of the same Java log events.
Matching event/request/session identities and timestamps show that it is not a
second execution or send. `instance_audit.txt` records instance/launch audit
information and is not a command-result stream.

The same Java evidence recorded authoritative acquisition proof:

```text
before_target_count=1
after_target_count=2
target_count_delta=1
effect_observation_status=authoritative
result_reason=matching_task_finished
result_fidelity=callback_plus_matching_user_task_event
```

### 3.2 LAVI process log

The active repository log was `logs/20260907_230517_log.txt`. The relevant
sanitized counts were:

```text
line 147-150  one command admission/send/route decision
line 151      one START response fact
line 152-155  one START output, Chat UI yield, and TTS enqueue
line 165      one START playback observation
line 198      one accepted completed Java result
line 199      one terminal response fact
line 200      one terminal output-listener delivery
line 202      one terminal ui_presentation enqueue receipt
line 203      one terminal TTS queue receipt
line 212      one terminal TTS playback receipt
```

Repeated appearances of the sentence in TTS split/start/finish log messages are
different observations of one TTS item, not repeated UI publication. The
asynchronous UI drain does not currently log callback-history confirmation or
queue acknowledgement, and no browser-paint receipt exists. That distinction
explains why one enqueue receipt and repeated visible cards can coexist.

Raw request, message, session, and event identifiers are intentionally omitted
from this repository documentation record. Runtime log files remain
uncommitted evidence.

## 4. Verified pre-change failure mechanism

The affected pre-change LAVI-owned path was:

```text
RoutedResponseUiPresentationAdapter
  -> gr.ChatMessage(content=<plain string>, metadata.id=<input event ID>)
  -> RoutedResponseUiPresentationQueue.enqueue()
  -> RoutedResponseUiPresentationDrain.append_to_history()
  -> Gradio Chatbot postprocess/browser/preprocess round trip
  -> append_to_history() on the next 0.25-second timer callback
```

`RoutedResponseUiPresentationDrain.append_to_history()` snapshots the queue and
keeps the item pending until a later callback confirms that the same logical
item is present in Chatbot history. This delayed acknowledgement is intentional:
removing the item before the browser update is observed could lose a message
when an update fails.

The reviewed pre-change source boundaries were
`routed_response_ui_presentation_drain.py:20-53` for drain/merge,
`:56-68` for confirmed-prefix matching, and `:71-86` for identity extraction.
`llm_chat_ui_builder.py:161-168` binds that drain to the 0.25-second timer with
the same Chatbot as callback input and output.

The defect was in the pre-change confirmation identity:

```text
(role, str(content), metadata.title, metadata.id)
```

Before the Gradio round trip, the queued `gr.ChatMessage.content` is:

```text
"다이아 곡괭이 다 만들었어"
```

With the repository-pinned Gradio 6.18.0, Chatbot postprocess/preprocess
normalizes that same text in callback history to:

```text
[{"text": "다이아 곡괭이 다 만들었어", "type": "text"}]
```

The role, title, and metadata event ID are preserved, but `str(content)` is
different. Consequently:

1. `_presented_prefix_count()` does not recognize the pending prefix in the
   returned history.
2. `acknowledge_presented()` is not called for that item.
3. The item remains in the presentation queue.
4. The additions check compares the same incompatible identities and regards
   the queued item as absent.
5. The timer appends the same logical item again.
6. The next Gradio round trip repeats the same mismatch.

A read-only local reproduction using the real Gradio `Chatbot.postprocess()`
and `Chatbot.preprocess()` boundary increased history again on the second drain
while the queue pending count stayed at one. Repeating the round trip reproduced
monotonic history growth with one retained pending item.

## 5. Why the pre-change tests passed

The pre-change focused tests covered bounded queue construction, an
unconfirmed-update retry, confirmed-prefix acknowledgement, overlapping
enqueue, epoch invalidation, and reset. They pass the Python object returned by
the first drain directly into the next drain. They did not exercise a
capacity-full rejection at this presentation-queue boundary.

That direct object still contains `ChatMessage.content` as a plain string. The
tests do not pass it through the real Gradio Chatbot postprocess/preprocess
normalization boundary, so both calls calculate the same `str(content)` and the
test acknowledgement succeeds. The live browser callback uses the normalized
text-block list and exposes the missing case.

This closes the test gap without invalidating the useful existing retry and
epoch tests.

## 6. Ownership and excluded causes

Ownership classification:

| Boundary | Classification | Finding |
| --- | --- | --- |
| `llm_core/routed_response/presentation/ui/**` | LAVI-owned Python presentation component | Verified fault domain |
| `llm_core/ui/llm_chat_ui_builder.py` | LAVI-owned Gradio composition/timer binding | Repeats the retained item; not a second publisher |
| Gradio 6.18.0 Chatbot normalization | pinned third-party behavior | Triggering representation boundary; unchanged |
| Fabric Python transport and lifecycle correlation | LAVI-owned Fabric backend | One accepted terminal; not the cause |
| Fabric Java bridge and ChatClef Task | LAVI-owned bridge plus preserved engine | One execution/send; not the cause |
| output and TTS sinks | LAVI-owned delivery components | One terminal delivery each; not the cause |

No Java, WebSocket, session, terminal CAS, effect-projection, command renderer,
TTS, ChatClef/AltoClef, Forge/MineMind, common-protocol, or dependency change is
needed to correct this defect.

## 7. Implemented correction contract

The 2026-09-08 source follow-up makes presentation identity stable across the
exact Gradio serialization forms while preserving the existing non-lossy
acknowledgement protocol. The requirements below remain the normative contract
for the applied code.

Required behavior:

1. A plain string and Gradio's equivalent normalized text-block list identify
   the same presentation body.
2. Identity must not depend on a container's incidental `str()` serialization.
3. The immutable input event ID remains receipt/log correlation, but event ID
   alone is insufficient. START and TERMINAL for one command can share it. A
   separate immutable presentation identity must also include the bounded
   route/response lifecycle discriminator and survive the Gradio round trip.
4. The first drain may propose an update but must not remove the queue item
   until a later callback history confirms that exact logical presentation.
5. A confirmed item is acknowledged once from the FIFO prefix. Later timer
   callbacks return `gr.skip()` and do not grow history.
6. Distinct event IDs with identical visible text both remain deliverable.
7. Several presentations from one event remain distinct when their lifecycle
   boundary differs, including when their visible canonical bodies happen to
   be equal.
8. Existing capacity, FIFO ordering, prefix-only acknowledgement, queue epoch,
   reset, shutdown clear, typed badge/detail, output, TTS, and non-preempting
   delivery contracts remain unchanged.
9. Unknown or malformed Gradio content shapes must not be silently treated as
   confirmed presence in callback history. Such confirmation would still not
   prove a physical browser paint.

Applied responsibility split:

```text
llm_core/routed_response/presentation/ui/
  routed_response_ui_sink_delivery.py
    -> derive an explicit presentation identity from the validated request's
       event, route, and response lifecycle identity

  routed_response_ui_presentation_adapter.py
    -> project the presentation identity into metadata that survives the
       Gradio round trip; keep the input event ID on the existing receipt

  routed_response_ui_presentation_drain.py
    -> snapshot/merge/confirmed-prefix orchestration only

  routed_response_ui_presentation_identity.py
    -> immutable versioned lifecycle presentation token

  routed_response_ui_presentation_fingerprint.py
    -> canonical extraction and integrity validation for supported raw and
       Gradio-normalized message shapes

  routed_response_ui_presentation_queue.py
    -> existing bounded FIFO, epoch, and prefix acknowledgement owner
```

The existing `presentation/ui/` package already represents the correct
responsibility boundary. No new top-level folder, generic manager, shared
mutable global, or backend-common UI implementation is warranted.

The stable presentation identity is the primary same-presentation key.
Canonicalized content may be retained as an integrity check, but incidental
container serialization must not be part of that key. The exact metadata
encoding remains an implementation decision only if the real Gradio
postprocess/preprocess test proves that the complete bounded identity survives.

If permanent observability is added, it must be state-change-based and bounded:
one sanitized enqueue/confirmation/acknowledgement outcome per presentation
identity and reason, with no raw conversation body and no log on every
unchanged 0.25-second tick.

## 8. Rejected workarounds and non-goals

The following do not address the verified cause and are outside the correction:

- increasing the timer interval;
- clearing the queue immediately after returning a browser update;
- reducing or increasing queue capacity;
- deduplicating by visible Korean text alone;
- deduplicating by input event ID alone;
- suppressing terminal responses;
- changing Java terminal send or command correlation;
- changing TTS deduplication;
- adding retries, sleeps, command resubmission, or an LLM call;
- upgrading or downgrading Gradio;
- changing Minecraft, Fabric, ChatClef, AltoClef, or Java versions;
- adding Forge/MineMind code or shared backend UI state.

## 9. Regression and runtime verification contract

The offline implementation covers items 1 through 11 below. Item 12 remains a
manual live-browser requirement and is not inferred from callback history:

1. A real `gr.Chatbot.postprocess()`/`preprocess()` round trip for a queued
   `ChatMessage`, followed by confirmation and an empty queue.
2. Multiple later timer-equivalent drains that all return `gr.skip()` and keep
   history length unchanged.
3. A failed/unconfirmed first browser update that retains the item and retries
   without loss.
4. START and TERMINAL presentations sharing one command input event ID, each
   shown exactly once.
5. Two commands with identical visible text and different event IDs, both
   shown exactly once.
6. The same event ID and visible text with different `response_kind` values,
   each shown exactly once.
7. Several pending items with FIFO and confirmed-prefix acknowledgement.
8. Queue-capacity exhaustion rejects the new item without evicting or
   acknowledging an existing pending item.
9. Enqueue overlap between proposal and confirmation without losing the newer
   item.
10. Reset/clear between proposal and confirmation without reviving the stale
    item.
11. Output-listener and TTS counts remaining one per lifecycle fact while UI
    confirmation/retry is exercised.
12. A live Chat UI smoke observation for at least two seconds, spanning several
    0.25-second timer callbacks, with one terminal card remaining one card.

Verification order used by the source follow-up:

```text
characterize the real Gradio round trip
  -> add the focused canonical-identity responsibility
  -> run focused presentation and UI-builder tests
  -> run routed-response, LLM/UI, TTS, and Minecraft Python regressions
  -> launch the application only when separately authorized
  -> observe one async terminal across multiple timer callbacks
  -> confirm one UI card, one output delivery, and one TTS playback
```

No Java test or Gradle build was required because the correction changed only
the Python presentation layer. Runtime launch and live Chat UI verification
remain separate actions unless an active request explicitly includes them.

## 10. Pre-change provenance and rollback boundary

The inspected repository was:

```text
root: C:\Vtuber_Souorce_Code\LAVI
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: c5968582a826e7fb10b857e7c50388c5e3ec93d2
worktree: dirty; existing changes preserved
```

Pre-change SHA-256 values for the directly relevant dirty-worktree files were:

```text
routed_response_ui_presentation_drain.py
  BF98FC627BD06626814859171EAF5FAA351B3DFAD0167345CE9AE1DDB5216D51
routed_response_ui_presentation_queue.py
  2692EFD580777E2B06364689358B1248B11A1374749B1BD048A32DB7833526C9
llm_chat_ui_builder.py
  2B8C7F8F3EDB3771E0A0283D3656ECDEFE58FFBD245538B5328A4FF968033D41
test_routed_response_presentation.py
  249FF9508EB9250CF6FD95F3A3B1288ABEB0CAB5BD11DC1467E546456C8C0546
```

The implemented correction rollback unit is the Python UI sink-to-adapter
presentation-identity propagation, Gradio-shape normalization, drain matching,
their focused tests, and the corresponding status updates in this
documentation. It must remain separate from Java effect evidence, command
profiles, diagnostics refactoring, TTS behavior, dependency versions, and
deployment artifacts.

## 11. 2026-09-08 Python UI implementation follow-up

The applied lifecycle token is a bounded, versioned SHA-256 token derived from
the input event ID, route kind, response kind, routed-response source,
presentation source kind, and badge label. It is projected into Gradio's
supported `metadata.id` field. The original input event ID remains unchanged in
the existing presentation receipt and delivery diagnostics. Visible text is
not the primary identity; normalized body, assistant role, and badge title are
confirmation integrity fields.

Production responsibility boundaries are now:

```text
routed_response_ui_presentation_identity.py
  immutable lifecycle token only

routed_response_ui_presentation_fingerprint.py
  exact string/single-text-block normalization and integrity fingerprint

routed_response_ui_presentation_adapter.py
  pure Gradio ChatMessage rendering

routed_response_ui_sink_delivery.py
  request-to-identity derivation, one callback enqueue, receipt, observation

routed_response_ui_presentation_drain.py
  queue snapshot, confirmed FIFO-prefix acknowledgement, merge, epoch check
```

The direct LAVI Chat UI START/coalesced path and asynchronous queue path use the
same identity factory. A START and TERMINAL sharing one input event therefore
have different tokens when their response kind differs. The queue owner,
capacity, FIFO, epoch, reset/shutdown clear, 0.25-second timer, output sink, TTS
delivery, command lifecycle, Java bridge, and Minecraft behavior were not
changed by this rollback unit.

Offline verification on 2026-09-08:

```text
UI presentation responsibility tests:
  28 passed, 19 subtests passed

Focused UI/input/builder integration:
  48 passed, 19 subtests passed

Routed-response/input/UI/composition regression:
  83 passed, 19 subtests passed

Minecraft/app/TTS adjacent regression:
  1075 passed, 2 skipped, 6912 subtests passed

Full Python repository run outside the named-pipe-restricted sandbox:
  2305 passed, 4 skipped, 7964 subtests passed
  1 pre-existing HEAD-hash contract subfailure remained
  failing target: FabricChatClefCommandDispatcher.java
  Java source and hash-contract test were unchanged by this correction

Restricted-sandbox SQLite named-pipe failure:
  rerun outside sandbox: 1 passed

Ruff focused source/test check:
  PASS
```

The real Gradio 6.18.0 postprocess/preprocess test proves that the first drain
keeps the item pending, the next callback-history confirmation removes it, and
ten later timer-equivalent callbacks return `gr.skip()` without history growth.
Failed first updates retry without loss; START/TERMINAL, identical text across
different events, FIFO prefix, capacity rejection, overlap, reset, malformed
content, and output/TTS-facing listener non-replay cases also pass. The separate
adjacent TTS regression covers the existing queue and playback deduplication;
the UI retry test does not claim a physical audio-playback observation.

This is offline callback-boundary proof, not proof of physical browser paint.
Application launch, a two-second live Chat UI smoke observation, deployment,
Minecraft launch, Java tests, and Gradle build were `NOT_RUN` for this
Python-only follow-up.
