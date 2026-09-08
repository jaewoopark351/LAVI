<!-- 20260806_kpopmodder: Added the Minecraft plugin documentation entrypoint and reading map. -->
<!-- 20260905_kpopmodder: Reconciled the H5 Korean Chat/microphone entry after Python implementation and offline verification. -->
<!-- 20260905_kpopmodder: Indexed the Chat/final-microphone Korean-only feedback, GET-alias, and trusted-stop pre-change contract. -->
<!-- 20260905_kpopmodder: Reconciled the Korean feedback, scoped crafting defaults, and STOP implementation verification status. -->
<!-- 20260906_kpopmodder: Reconciled the applied wooden-button engine hunk, refreshed clean-build artifact, and remaining runtime status. -->
<!-- 20260907_kpopmodder: Indexed the exact and generalized lifecycle-feedback records with their source-identified delivery gaps. -->
<!-- 20260907_kpopmodder: Qualified the targeted diagnostics refactor as mixed-worktree offline integration evidence. -->
<!-- 20260907_kpopmodder: Reconciled the applied generalized command lifecycle and its current single-target GET evidence ceiling. -->
<!-- 20260907_kpopmodder: Indexed the verified Python/Gradio async duplicate-presentation investigation. -->
<!-- 20260908_kpopmodder: Reconciled the applied Python-only Gradio identity correction and pending live-browser smoke. -->
<!-- 20260908_kpopmodder: Indexed the pending trusted-STOP terminal-only policy and STORE_HOME verified-success rollout contract. -->
<!-- 20260908_kpopmodder: Indexed the implemented Python rollout and the distinct live Gradio suppressed-response input-lock investigation. -->
<!-- 20260908_kpopmodder: Reconciled the applied Gradio-only empty-stream completion adapter and offline verification. -->
<!-- 20260908_kpopmodder: Reconciled STORE_HOME's verified profile and the final selected Python regression. -->
<!-- 20260908_kpopmodder: Reconciled the implemented contextual active-command STATUS response without changing command admission. -->

# LAVI Minecraft Plugin

This directory contains the Minecraft integration owned by LAVI.

This document is the first reading point for Minecraft work. It is
documentation only. It does not approve Java changes, Python behavior changes,
Gradle changes, dependency changes, Minecraft launch, runtime reproduction,
cache deletion, commit, or push.

### 2026-09-07 natural lifecycle-feedback reconciliation

Deterministic Korean lifecycle feedback is now implemented for all 26
registered response profiles when an existing LAVI-owned interactive ingress
actually accepts the command, including bounded wording for item/forms already
admitted by that ingress. It changes neither command admission nor Minecraft
Task behavior. The six earlier exact-crafting boundary gaps are closed in
source and focused tests: initial progress sequencing, STOP terminal
arbitration, TTS enqueue/play receipts, non-preempting late terminal delivery,
UI-only Minecraft provenance, and diagnostic-kind coverage.

The current Python worktree's strong positive terminal evidence covers
generalized single-target GET and the later strictly verified
trusted-translation STORE_HOME `COMPLETED`/zero-work slice. Multi-target/bracket
GET and every other gameplay-effect profile remain cautious unless separately
proven. Final selected Python integration verification passed 1226 tests with
two skips and 7052 subtests, and Ruff passed. The implementation record retains
the earlier repository-wide run's unrelated pre-existing `HEAD`-hash contract
failure as historical evidence. Deployment and the formal full 26-profile
runtime matrix in the original generalized implementation checkpoint are
`NOT_RUN`. See the
[implementation record](docs/chatclef-general-natural-korean-command-lifecycle-feedback-implementation-record-2026-09-07.md).

A later user-run runtime passed command execution, authoritative single-target
GET effect, terminal correlation, output, and TTS once, but exposed a Chatbot
exactly-once defect: Gradio-normalized history could not confirm and remove the
queued terminal presentation, so the 0.25-second UI timer appended it
repeatedly. The Python-only presentation fix is implemented and verified
offline through Gradio postprocess/JSON/preprocess; a live browser smoke retest
remains `NOT_RUN`. This was not repeated Minecraft execution. See the
[duplicate-presentation investigation](docs/chatclef-gradio-routed-response-duplicate-presentation-investigation-2026-09-07.md).

A subsequent user-run review found a different pair of Python lifecycle
boundaries. The Python worktree now suppresses the accepted trusted-STOP START
response and adds proof-gated STORE_HOME success rendering as separate rollback
units. Offline verification passed without a Java build. A later live STOP run
showed only one terminal `멈췄어`, proving the visible response policy, but the
empty current-input generator exposed a separate Gradio 6.18.0 input lock: the
terminal arrived through the asynchronous UI sink while the textbox remained
in its square-stop state. A Gradio-only Python completion adapter is now
implemented and verified offline: it produces no assistant card, completes the
queue iterator, and accepts an immediate second submit. The post-fix live
browser smoke and the STORE_HOME post-implementation live retest remain
`NOT_RUN`. See the
[STOP single-response and STORE_HOME verified-terminal contract](docs/chatclef-python-stop-single-response-store-home-verified-terminal-pre-change-contract-2026-09-08.md)
and the distinct
[suppressed-response input-lock investigation](docs/chatclef-gradio-suppressed-response-input-lock-investigation-2026-09-08.md).

A later Python-only follow-up implements the narrower STATUS-intent correction.
The deterministic closed classifier now recognizes the established `마크 AI`
addressee and contextually safe prefixless progress questions before ordinary
busy handling. It submits zero commands, reuses the exact immutable active
descriptor across all ordinary command-lifecycle profiles, preserves trusted
STOP as terminal-only, and preserves genuine second-command blocking. For
example, an active diamond-pickaxe craft can answer
`다이아 곡괭이 만드는 중이야`. A validated prefixless STATUS candidate that
cannot claim the current exact nonterminal context returns directly to outer
conversation and skips every later Minecraft translator/submission owner. The
implementation also closes bounded STATUS permit/acknowledgement custody gaps
without changing the existing success-path FIFO model. Offline tests pass;
live LAVI/Minecraft verification remains `NOT_RUN`. See the
[contextual active-command STATUS response contract](docs/chatclef-python-contextual-active-command-status-response-pre-change-contract-2026-09-08.md).

## Current Implementation Scope

Supported now:

```text
LAVI
  -> Fabric adapter
    -> Fabric ChatClef bridge
      -> ChatClef / AltoClef
        -> Baritone
```

Not implemented now:

```text
LAVI
  -> Forge adapter
    -> Forge MineMind
```

The current worktree also contains the Fabric ChatClef-only implementation for
trusted Korean LAVI Chat and final VoiceInput transcripts:

```text
one-shot trusted ingress
  -> Korean eligibility proof
  -> STOP-first branch
     | exact safe STOP -> guarded stop_control_v1 priority lane
     | unsafe stop-like input -> deterministic local rejection
     | unrelated -> ordinary Minecraft-command branch
  -> ordinary Korean command ownership
     -> existing command or request-local generic crafting defaults
     -> deterministic source-correct command feedback
```

Its offline verification status is `IMPLEMENTED_VERIFIED_OFFLINE`. The final
behavior-focused suite passed 116 tests and 617 subtests; the Minecraft Python
suite passed 901 tests, skipped 2, and passed 3462 subtests; and the full Python
suite passed 2093 tests, skipped 4, and passed 4496 subtests. The required clean
forced Gradle build completed all 171 tasks. The 1.20.1 test XML records 216
suites and 762 tests with 0 failures, 0 errors, and 1 skip. The fresh
unclassified runtime JAR has SHA-256
`0C8E7F774C52D79DE5BD1BB9B0E516F6E75C5C20CF5D73414EDD6D8154128161`.

That matching JAR was later observed in a live test instance. Trapdoor, empty
map, wooden pressure plate, and trusted STOP paths completed, while the broad
`get wooden_button 1` path reproduced an engine `StackOverflowError`. The
button root cause was source-recorded, and the minimum one-value correction has
since been applied. Its focused Java suite passed 5 tests with no failures,
errors, or skips; the refreshed clean forced build executed all 171 tasks, and
the 1.20.1 XML records 767 tests with 0 failures, 0 errors, and 1 unrelated
skip. The corrected mixed-provenance JAR has SHA-256
`488C195B8C87919E349549DD5B0766D05D669222BB63FCCDEDD0E38FDB2861B2`.
The active test-instance JAR is byte-identical. Two trusted Chat requests and
one exact `VoiceInput` final-transcript request for the generic wooden button
all reached natural terminal completion without `StackOverflowError` or a
fatal error; the microphone request also delivered and played its Korean TTS
feedback once. The button divergence is `PARTIALLY_VERIFIED` because the
contract's live explicit-oak and stone-button comparison remains unexercised.
The build/deployment identity and runtime-load status are `RUNTIME_VERIFIED`.
See the focused report linked below.

Fabric ChatClef and Forge MineMind are independent sibling backends. Do not add
Forge, MineMind, shared Java runtime, shared WebSocket server, shared reconnect
manager, shared session registry, shared diagnostics runtime, or shared GUI
work before a separate explicit approval for that backend.

## Backend Ownership Boundary

The common layer may contain only backend-neutral contracts:

```text
DTOs
wire protocol enums
JSON schema
error codes
documentation
neutral interfaces
```

The common layer must not own:

```text
WebSocket or HTTP transport
threading or asyncio loops
session registry
reconnect policy
runtime diagnostics implementation
config loaders
GUI implementation
Fabric runtime behavior
Forge runtime behavior
ChatClef adapter behavior
MineMind adapter behavior
```

## Folder Ownership Map

```text
plugins/Minecraft/common/dto/**
```

LAVI backend-neutral wire DTOs. These are the Python-side authority for the
current v1 bridge payload shape.

```text
plugins/Minecraft/common/protocol/**
plugins/Minecraft/common/schema/**
```

Backend-neutral protocol names, status values, and schemas.

```text
plugins/Minecraft/fabric/chatclef/**
```

LAVI-owned Python side of the Fabric ChatClef integration. This includes the
adapter, config, diagnostics, extension, input routing, intent extraction,
session ownership, WebSocket transport, and UI glue for Fabric ChatClef.

The trusted Korean Chat/final-microphone feature keeps its responsibilities in
separate boundaries:

```text
input_core/input_event/provenance/trusted_user_ingress/**
input_core/input_event/delivery/**
input_core/input_event/delivery/trusted_voice/**
llm_core/chat_input/trusted_local/**
llm_core/input_queue/**
llm_core/input_routing/**
llm_core/composition/speech_content/**
llm_core/composition/generation_output/**
llm_core/composition/trusted_ingress/**
llm_core/composition/ui_lifecycle/**
llm_core/routed_response/**
plugins/Minecraft/fabric/chatclef/input/eligibility/**
plugins/Minecraft/fabric/chatclef/input/diagnostics/**
plugins/Minecraft/fabric/chatclef/input/ownership/item_command/**
plugins/Minecraft/fabric/chatclef/input/aliases/generic_crafting_defaults/**
plugins/Minecraft/fabric/chatclef/intent/scoped_resolution/**
plugins/Minecraft/fabric/chatclef/intent/natural_language/**
plugins/Minecraft/fabric/chatclef/extension/natural_language/generic_crafting_defaults/**
plugins/Minecraft/fabric/chatclef/input/stop/**
plugins/Minecraft/fabric/chatclef/response/stop/**
plugins/Minecraft/fabric/chatclef/transport/control/stop/**
```

The five generic crafting defaults are not stored in the global generated
Korean item-alias artifact. They are activated only for one proved Chat/final
microphone request.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
```

Upstream-derived ChatClef / AltoClef Fabric runtime with LAVI compatibility
patches and LAVI-owned bridge or integration entrypoints. The implemented STOP
and command-result responsibilities remain separated under
`bridge/command/control/stop/**`, `bridge/command/lifecycle/outbox/**`,
`bridge/transport/inbound/{stop,command}/**`, and
`bridge/transport/{session,connection,result}/**`. Treat this tree as a
preserved third-party-derived baseline unless a file is proven LAVI-owned.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/**
```

LAVI-owned Java bridge, diagnostics, optional integration, and overlay code
inside the Fabric runtime.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/**
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/baritone/**
```

Upstream-derived ChatClef / AltoClef / Baritone-adjacent engine code. Modify
only with the smallest evidence-backed hunk and only after the applicable
diagnostic or last-resort gate is satisfied.

```text
plugins/Minecraft/docs/**
```

Architecture, runbooks, audit notes, and divergence records for this
integration.

## End-To-End Ordinary Command Flow

Current ordinary Fabric ChatClef command flow (after the trusted Korean
STOP-first branch has returned `unrelated`):

```text
raw user text
  -> LAVI input gate
  -> Minecraft route decision
  -> Korean or natural-language normalization
  -> rule or LLM intent extraction
  -> intent schema validation
  -> target and quantity resolution
  -> ChatClef command string
  -> MinecraftFabricChatClefExtension.submit_command()
  -> Python FabricChatClefWebSocketServer
  -> v1 bridge envelope command_request
  -> Java WebSocket callback decode and enqueue
  -> Fabric END_CLIENT_TICK dispatch
  -> FabricChatClefCommandExecutor
  -> ChatClef UserTaskChain root task
  -> ChatClef child tasks and Baritone helpers
  -> Java terminal observation or exception/deadline result
  -> v1 bridge envelope command_result
  -> Python FabricChatClefConnectionOwnership.accept_result()
  -> LAVI UI/status/extension result
```

The Java WebSocket callback is not the command executor. It must only decode,
validate, and enqueue. The client tick dispatcher owns Minecraft-client-thread
dispatch. The higher-priority STOP-control flow is documented separately in
[Fabric ChatClef Bridge Protocol V1](docs/fabric-chatclef-bridge-protocol-v1.md#additive-stop-control-profile-within-v1).

## Reading Order

Start with these documents:

```text
plugins/Minecraft/README.md
plugins/Minecraft/docs/minecraft-backend-separation.md
plugins/Minecraft/docs/fabric-chatclef-bridge-protocol-v1.md
plugins/Minecraft/docs/chatclef-command-lifecycle-and-threading.md
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/LAVI_INTEGRATION.md
```

For ChatClef / AltoClef runtime behavior, read:

```text
plugins/Minecraft/docs/chatclef-structure-overview.md
plugins/Minecraft/docs/chatclef-engine-diagrams.md
plugins/Minecraft/docs/chatclef-tick-accuracy-principles.md
```

For Carry On or interaction diagnostics, read:

```text
plugins/Minecraft/docs/chatclef-carryon-integration-direction.md
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
```

For pathfinding, wrong-target movement, mining stalls, or world replacement
regressions, read:

```text
plugins/Minecraft/docs/chatclef-baritone-cache-troubleshooting.md
```

For furnace/container arbitration, repeated `OPEN_CONTAINER` /
`GET_CONTAINER_ITEM` decisions, or `DestroyBlockTask` restart symptoms while a
smelting command is active, read:

```text
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
```

For the dated Store/container incident evidence, read the matching incident
analysis instead of applying one run's hypothesis to another:

```text
plugins/Minecraft/docs/chatclef-post-completion-store-loop-investigation.md
    2026-08-07 post-completion Store/Craft/Baritone loop

plugins/Minecraft/docs/chatclef-bare-deposit-container-handoff-loop-investigation.md
    2026-08-19 bare deposit branch/lifecycle loop and explicit StopCommand end

plugins/Minecraft/docs/chatclef-resource-target-retry-thrashing-analysis.md
    2026-08-08 resource target/path retry thrashing that eventually completed
```

For the bounded diagnostics-only design and its current slice-by-slice
implementation status, read:

```text
plugins/Minecraft/docs/chatclef-bare-deposit-diagnostics-plan.md
```

For the implemented responsibility split and folderization record for
`StoreDepositSliceADiagnosticsContractTest`,
`StoreDepositAutomaticLifecycleLedger`, and the residual
`StoreHomeTimeoutDiagnostics`, read:

```text
plugins/Minecraft/docs/chatclef-targeted-diagnostics-refactoring-folderization-plan-2026-09-07.md
```

Its offline Java test/build result is mixed-dirty-worktree integration evidence
that also includes the separately owned natural-response Java GET-effect and
result-projection slice; it is not an isolated diagnostics-only build or a
combined source rollback unit. Deployment and Minecraft runtime verification
for this refactor remain `NOT_RUN`.

For the first live prefix captured with the partial diagnostics JAR, read:

```text
plugins/Minecraft/docs/chatclef-bare-deposit-diagnostics-reproduction-2026-08-19-r1.md
    separate run; live prefix, not a finalized terminal record
```

For payload typing and map-shape stability, read:

```text
plugins/Minecraft/docs/chatclef-command-payload-map-audit.md
```

For Python-only command replies, Korean mining phrases, transport result
events, stale active-command reconciliation, and inventory preflight cleanup
planning, read:

```text
plugins/Minecraft/docs/chatclef-python-command-orchestration-plan.md
```

The orchestration plan owns stale-active UNKNOWN reconciliation and
active-ownership release. Releasing active ownership never proves command
completion or gameplay success.

For the stale active-command terminal reconciliation gap, Java should publish
additive nonterminal lifecycle evidence through the existing
`command_result` channel with `status=running`. Python may store that validated
running result as `details.commands.last_result`, but active ownership remains
held until a terminal result or a separately approved Python-local
`RECONCILED_UNKNOWN` compare-and-release path runs.

`RECONCILED_UNKNOWN` is a Python-local action, not a new bridge status. The
wire/DTO result remains `status=unknown`, `ok=false`, with
`gameplay_effect=UNVERIFIED`. A Python release does not prove Java
`FabricChatClefCommandQueue.active` has retired, so new command admission must
remain quarantined until authoritative evidence proves Java queue and lifecycle
retirement. A late terminal, connection-generation replacement, or recovery
completion may clear that gate only when its source contract and tests prove it
establishes Java retirement; otherwise it is supporting evidence only.
The next implementation plan must define the monotonic lifecycle
`evidence_sequence` acceptance rules, authoritative transport admission gate,
and reconnect/restart fail-closed strategy before any code change.

2026-08-21 review update: the Python stale-active direction is
`CONDITIONAL PASS / PRODUCTION ON BLOCKED`. A default-OFF shadow classifier and
test-injected guarded mutation path may be implemented, but production config
must not enable release unless a separate runtime readiness gate proves
authoritative Java retirement evidence or restart-safe quarantine capability.
Python-local reconciliation state must be split away from Java-facing wire
status snapshots: synthetic UNKNOWN, tombstones, and admission quarantine must
not appear in `handshake_ack` or `status_snapshot` payloads sent to Java. The
phrase `다이아 곡괭이 만들어줘` already validates to `get diamond_pickaxe 1`; this
incident remains classified as stale active-command admission blocking, not a
Korean compiler gap.

For the dated Korean command-registry baselines, activation-aware command
catalog, command-by-command lifecycle classification, safety tiers,
confirmation modes, resolver domains, and public enablement axes, read:

```text
plugins/Minecraft/docs/chatclef-python-korean-command-registry-plan.md
```

For the implementation contract and offline verification record for exposing the existing H5 player-centered
`auto_deposit_trust area 16x16` operation to LAVI Chat and microphone Korean
input, including the required typed input-provenance migration, exact trusted
two-form `@` adapter, deterministic parsing, prefixless compilation, catalog
parity, focused Java test-only lifecycle proof, ACK wording and tests, read:

```text
plugins/Minecraft/docs/chatclef-h5-auto-deposit-trust-korean-chat-microphone-pre-change-contract-2026-09-04.md
```

For the pre-change contract covering deterministic replies, Korean
GET-acquisition aliases/defaults for trapdoors, empty maps, pressure plates,
and buttons, plus global `stop_ai`, all limited to Korean input from LAVI Chat
or final VoiceInput microphone transcripts, including its current
implementation, offline-verification ledger, and the later live-runtime
wooden-button regression status, read:

```text
plugins/Minecraft/docs/chatclef-korean-command-feedback-crafting-stop-pre-change-contract-2026-09-05.md
```

For the historical exact `diamond_pickaxe` quantity-one natural craft-intent
baseline and its migration into the generalized lifecycle, read:

```text
plugins/Minecraft/docs/chatclef-natural-korean-crafting-lifecycle-feedback-pre-change-contract-2026-09-07.md
```

For the historical pre-change contract that defines natural START, read-only
STATUS, and proof-gated TERMINAL responses for every accepted registered
command and every item target already admitted by its existing action policy,
read:

```text
plugins/Minecraft/docs/chatclef-general-natural-korean-command-lifecycle-feedback-pre-change-contract-2026-09-07.md
```

For the applied generalized implementation, current evidence ceiling,
responsibility split, and final offline integration verification, read:

```text
plugins/Minecraft/docs/chatclef-general-natural-korean-command-lifecycle-feedback-implementation-record-2026-09-07.md
```

For the verified Python/Gradio async UI duplicate-presentation root cause,
sanitized runtime evidence, applied Python-only correction, offline real-Gradio
round-trip verification, and remaining live-browser smoke requirement, read:

```text
plugins/Minecraft/docs/chatclef-gradio-routed-response-duplicate-presentation-investigation-2026-09-07.md
```

For the implemented Python-only trusted-STOP terminal policy, STORE_HOME
verified-success rollout, responsibility split, offline verification, and
separate rollback units, read:

```text
plugins/Minecraft/docs/chatclef-python-stop-single-response-store-home-verified-terminal-pre-change-contract-2026-09-08.md
```

For the implemented and offline-verified Python-only follow-up that recognizes
common addressed and contextually safe prefixless progress questions across all
ordinary command-lifecycle profiles and item targets already accepted by their
existing ingress, while trusted STOP remains terminal-only, read:

```text
plugins/Minecraft/docs/chatclef-python-contextual-active-command-status-response-pre-change-contract-2026-09-08.md
```

For the later live STOP symptom where `멈췄어` appeared once but the Gradio
textbox remained in its square-stop state, including non-empty runtime-log
evidence, the exact empty-stream reproduction, applied Python-only completion
adapter, offline queue/serialization verification, and remaining live-browser
smoke, read:

```text
plugins/Minecraft/docs/chatclef-gradio-suppressed-response-input-lock-investigation-2026-09-08.md
```

For the exact `get wooden_button 1` StackOverflow reproduction, source-proven
one-plank recipe/two-slot mask mismatch, applied one-value engine hunk, offline
verification evidence, remaining runtime contract, and rollback unit, read:

```text
plugins/Minecraft/docs/chatclef-wooden-button-stack-overflow-pre-change-report-2026-09-06.md
```

For the Python-only inventory-full cleanup preflight contract, protected-item
policy, post-cleanup verification, and fail-closed primary-submit gate, read:

```text
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
```

For the historical diagnosis of Korean standalone item aliases,
mining/acquisition verb parsing, and Minecraft-active routing boundaries, read:

```text
plugins/Minecraft/docs/chatclef-korean-item-command-resolution-analysis.md
```

For the v2 Korean item alias expansion shared by `get`, `equip`, `deposit`,
and `give`, including default-policy boundaries, command-specific policy
boundaries, conditional review notes, and the required Phase 0 contract freeze,
read:

```text
plugins/Minecraft/docs/chatclef-korean-item-action-alias-v2-plan.md
```

For Korean ChatClef test strategy, prefixless DSL ownership, alias coverage
snapshots, command support matrix rules, and opt-in live test tiers, read:

```text
plugins/Minecraft/docs/chatclef-korean-test-strategy.md
```

For the post-implementation Korean ChatClef merge-blocker review, including
Java contract hash authority, CI coverage, quantity validation,
TranslationResultDTO safety, live result correlation, and alias snapshot
follow-up, read:

```text
plugins/Minecraft/docs/chatclef-korean-post-review-merge-blockers.md
```

For upstream-derived engine divergence, read:

```text
plugins/Minecraft/docs/chatclef-engine-divergence-record.md
```

## Document Status

```text
Current contract:
  minecraft-backend-separation.md
  fabric-chatclef-bridge-protocol-v1.md
  chatclef-command-lifecycle-and-threading.md

Runtime integration runbook:
  runtime/chatclef_fabric_1.20.1/LAVI_INTEGRATION.md

Diagnostic runbooks:
  chatclef-task-lifecycle-diagnostics.md
  chatclef-baritone-cache-troubleshooting.md

Diagnostics-only design and implementation status:
  chatclef-bare-deposit-diagnostics-plan.md
  chatclef-targeted-diagnostics-refactoring-folderization-plan-2026-09-07.md

Diagnostics reproduction evidence:
  chatclef-bare-deposit-diagnostics-reproduction-2026-08-19-r1.md

Policy and investigation direction:
  chatclef-carryon-integration-direction.md

Audit and active snapshot:
  chatclef-command-payload-map-audit.md
  chatclef-engine-divergence-record.md

Incident analyses:
  chatclef-cooked-beef-entity-path-calculation-investigation.md
  chatclef-post-completion-store-loop-investigation.md
  chatclef-bare-deposit-container-handoff-loop-investigation.md
  chatclef-resource-target-retry-thrashing-analysis.md

Python orchestration planning:
  chatclef-python-command-orchestration-plan.md

Python Korean command registry planning:
  chatclef-python-korean-command-registry-plan.md

H5 Korean Chat/microphone pre-change contract:
  chatclef-h5-auto-deposit-trust-korean-chat-microphone-pre-change-contract-2026-09-04.md

Chat/final-microphone Korean command feedback, scoped GET defaults, and STOP contract (implemented; matching-JAR generic-button runtime evidence recorded for Chat and final microphone; explicit oak/stone-button comparison pending):
  chatclef-korean-command-feedback-crafting-stop-pre-change-contract-2026-09-05.md

Exact diamond-pickaxe quantity-one natural Korean crafting lifecycle feedback (historical exact-slice baseline; later command/effect runtime passed; shared async UI fix verified offline, live browser retest pending):
  chatclef-natural-korean-crafting-lifecycle-feedback-pre-change-contract-2026-09-07.md

All-item/all-command natural Korean lifecycle feedback generalization (implemented and verified offline; Python UI correction also verified offline, live browser retest pending):
  chatclef-general-natural-korean-command-lifecycle-feedback-pre-change-contract-2026-09-07.md

All-item/all-command natural Korean lifecycle feedback implementation record (single-target GET plus strict trusted STORE_HOME strong evidence; remaining effect profiles cautious; browser defects corrected offline, live retest pending):
  chatclef-general-natural-korean-command-lifecycle-feedback-implementation-record-2026-09-07.md

Python/Gradio asynchronous routed-response duplicate presentation (root cause verified; Python correction verified offline; live browser smoke not run):
  chatclef-gradio-routed-response-duplicate-presentation-investigation-2026-09-07.md

Trusted Korean STOP terminal-only publication and trusted-translation STORE_HOME verified-success rollout (Python worktree implemented and verified offline; live STOP visible response passed; STORE_HOME live retest pending):
  chatclef-python-stop-single-response-store-home-verified-terminal-pre-change-contract-2026-09-08.md

Python/Gradio suppressed-response input lock after terminal-only STOP (failure mechanism reproduced; Python correction verified offline; live-browser smoke pending):
  chatclef-gradio-suppressed-response-input-lock-investigation-2026-09-08.md

Contextual natural active-command STATUS responses for all ordinary command-lifecycle profiles and item targets already accepted by their existing ingress; trusted STOP remains terminal-only (implemented and verified offline; live runtime not run):
  chatclef-python-contextual-active-command-status-response-pre-change-contract-2026-09-08.md

Generic wooden-button StackOverflow root cause, applied correction, clean-build proof, and matching-JAR `PARTIALLY_VERIFIED` runtime status:
  chatclef-wooden-button-stack-overflow-pre-change-report-2026-09-06.md

Python inventory cleanup preflight contract:
  chatclef-python-inventory-cleanup-preflight-contract.md

Korean item command diagnosis:
  chatclef-korean-item-command-resolution-analysis.md

Korean item action alias v2 planning:
  chatclef-korean-item-action-alias-v2-plan.md

Korean ChatClef test strategy:
  chatclef-korean-test-strategy.md

Korean ChatClef post-review merge blockers:
  chatclef-korean-post-review-merge-blockers.md

Engine reference:
  chatclef-structure-overview.md
  chatclef-engine-diagrams.md
  chatclef-tick-accuracy-principles.md
```

`AGENTS.md` contains operating rules. It is not the architecture overview or
day-to-day reading map.

## Command Lifecycle Ownership Summary

Python owns:

```text
server start and stop
asyncio loop
WebSocket server binding
active connection generation
active session identity
single active command request
accepting or rejecting terminal command_result envelopes
UI and extension result adaptation
Korean command interpretation
deterministic user response rendering
Python-local command orchestration
inventory preflight evidence evaluation
targeted cleanup planning
trusted Chat/final-Voice ingress claims and one-shot response authorization
request-local generic crafting-default activation
STOP control claim, barrier, result demux, strict terminal validation, and quarantine
```

Java bridge owns:

```text
WebSocket client connection to Python
handshake payload
command_request decoding
Java-side command queue
client-tick dispatch
command root binding
terminal observation
command_result envelope sending
nonterminal status=running lifecycle evidence publication
STOP-control candidate validation and queueing
END_CLIENT_TICK StopCommand dispatch and bounded STOP result verification
```

ChatClef / AltoClef owns:

```text
TaskRunner lifecycle
UserTaskChain root task selection
child task selection
generic interaction behavior
inventory/container task behavior
Baritone goal and path use through established engine paths
```

Diagnostics own observation only. Diagnostics must not choose tasks, mutate
inputs, create retries, cancel Baritone paths, change cleanup, or alter terminal
classification.

## Test Map

Protocol and backend separation:

```text
tests/test_minecraft_bridge_protocol.py
tests/test_minecraft_backend_separation_contract.py
```

Fabric ChatClef bridge and GUI:

```text
tests/test_minecraft_fabric_chatclef_transport.py
tests/test_minecraft_fabric_chatclef_java_bridge_contract.py
tests/test_minecraft_fabric_chatclef_adapter_skeleton.py
tests/test_minecraft_fabric_chatclef_composition.py
tests/test_minecraft_fabric_chatclef_extension.py
tests/test_minecraft_fabric_chatclef_gui.py
tests/test_minecraft_fabric_chatclef_korean_gui.py
```

Diagnostics contracts:

```text
tests/test_minecraft_fabric_chatclef_tasktrace_diagnostics_contract.py
tests/test_minecraft_fabric_chatclef_tool_equip_diagnostics_contract.py
tests/test_minecraft_fabric_chatclef_mining_tool_readiness_contract.py
```

Korean command parsing and routing:

```text
tests/test_minecraft_chatclef_input_router.py
tests/test_llm_minecraft_input_router.py
tests/test_minecraft_chatclef_command_compiler.py
tests/test_minecraft_chatclef_korean_command_integration.py
tests/test_minecraft_chatclef_korean_gui.py
tests/test_minecraft_chatclef_korean_intent_schema.py
tests/test_minecraft_chatclef_korean_rule_parser.py
tests/test_minecraft_chatclef_llm_intent_extractor.py
tests/test_minecraft_chatclef_natural_language_service.py
tests/test_minecraft_chatclef_item_phrase_resolver.py
```

Responsibility-split Korean ChatClef tests:

```text
tests/minecraft_chatclef/alias_contract/
tests/minecraft_chatclef/catalog_coverage/
tests/minecraft_chatclef/command_catalog/
tests/minecraft_chatclef/input_gate/
tests/minecraft_chatclef/korean_translation/
tests/minecraft_chatclef/lavi_input/
tests/minecraft_chatclef/lifecycle/
tests/minecraft_chatclef/runtime/
```

Trusted ingress, routed response, generic crafting defaults, and STOP control:

```text
tests/input_core/input_event/provenance/trusted_user_ingress/
tests/input_core/input_event/delivery/
tests/llm_core/chat_input/
tests/llm_core/routed_response/
tests/minecraft_chatclef/generic_crafting_defaults/
tests/minecraft_chatclef/stop/
tests/minecraft_chatclef/lavi_input/test_trusted_korean_chat_voice_integration.py
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/control/stop/
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/transport/FabricChatClefResultEnvelopeSenderTest.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/transport/connection/FabricChatClefConnectionDetachHandlerTest.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/resources/wood/WoodenButtonRecipeMaskRegressionTest.java
```

Run tests only when the current task explicitly authorizes tests or when the
test run is part of the requested implementation work. Documentation edits do
not automatically authorize a Minecraft launch or runtime reproduction.

## Investigation Rule Of Thumb

If the exact last successful boundary, first failing boundary, and triggering
state are not proven, stop at diagnostics or documentation.

Do not use an unproven timeout, retry, global cancellation, forced input
release, Baritone cache deletion, or broad refactor as the first fix.
