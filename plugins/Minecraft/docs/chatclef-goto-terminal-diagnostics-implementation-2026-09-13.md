<!-- 20260913_kpopmodder: Record the independently reversible diagnostics-first implementation and its evidence limits. -->

# GOTO terminal diagnostics implementation — 2026-09-13

## 1. Request and implementation boundary

The current request is to implement the
[arrival/result/TTS response contract](chatclef-goto-arrival-result-and-tts-response-pre-change-contract-2026-09-13.md),
with separate files and meaningful packages for independent responsibilities.
Its earlier documentation-only authorization is historical; this request authorizes
repository-local source, tests, documentation, bounded diagnostics and build verification.

[AGENTS.md Section 0](../../../AGENTS.md) requires:
“the first separately authorized source-bearing unit must be
`DIAGNOSTICS_ONLY`, behavior-preserving, and independently reviewable and revertible.”
The incident gate also covers terminal projection. The current first unit observes
the missing boundaries. It does not implement the later successful-arrival dialogue,
typed failure projection, or TTS latency optimization.

The runtime navigation cause and authoritative Task ARRIVED evidence remain UNKNOWN.
A matching disk artifact, isolated test, or successful build cannot fill those fields.
There is no rollback, failed implementation salvage, new navigation owner, dependency
change, external-instance mutation, game action, commit, or push in this unit.

## 2. Read-only artifact reconciliation

Repository/cwd: `C:\Vtuber_Souorce_Code\LAVI`.

- Branch: `minecraft-plugin-fix/alto-clef-infinite-loop`.
- HEAD at entry: `99af65f2cb0609de31fae24d2f1704aa6853f659`.
- Preexisting dirty files: 39, individually fingerprinted before this unit.
- Runtime source at entry: clean.
- Installed file: `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\chatclef-1.20.1-0.18.23.jar`.
- Repository file: `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar`.
- Both disk files: 8,449,485 bytes; SHA-256
  `FD76A9D2F6C186D7DCAEE974A790EEEAE8E8F13395267E225C97E4CFE0FE826B`.
- `latest.log:250` identifies the exact installed code-source path and its modification
  time `2026-09-12T16:20:34.9617592Z`. This is runtime loading-path evidence;
  no loaded-byte SHA-256 was emitted.
- Existing runtime-local log
  `codex-build-logs/chatclef-fabric-1.20.1-recovery-build-20260913-011721-6ddee9f9.log`
  records HEAD `3a97f8ec76fb02ddaf047849afeebe2b869dae7f`, only dirty runtime
  input `PreparedGotoTask.java`, SHA-256
  `2C2876FF404BFB8B23FCFE39A0309DE859EAABB89FD74B8E5E7E3767258888CD`.
  That fingerprint matches this turn's pre-edit Task source. The runtime diff
  between that build baseline and this turn's HEAD is only that file.
- That historical build was targeted `:1.20.1:clean :1.20.1:remapJar`,
  explicitly MIXED_PROVENANCE. It was not the canonical full build.

These facts reconcile the recorded inputs with current runtime source. They do not
prove the previously unlogged Task terminal, independent reproducibility, or live acceptance.

## 3. Responsibility boundaries and diagnostic flow

### Java Task observation

`lavi/minecraft/task/movement/gotopreflight/diagnostics/` contains:

- `GotoPreparationDiagnostics.java`: observe existing owner log calls and route output.
- `GotoPreparationLogBudget.java`: operation-local output admission.
- `GotoPreparationLogFormatter.java`: known semantic signatures and bounded fields.

Only diagnostic fields and the bodies of the existing `decision`/`log` helpers
are extracted from `PreparedGotoTask`. Original phase assignments, predicates,
navigation/collection calls, cleanup counters and stop ordering remain unchanged.
Task phase orchestration continues to use the existing inventory, plan, probe and
acquisition components.

The old Debug path is retained, including its existing ChatClef log integration.
A separate physical lifecycle log records owner-produced messages even when the
ordinary Debug path does not display them. The operation ID is the original Task's
identity, scoped by the existing request/session identifiers, not a globally unique ID.

First required semantic signatures have their own operation-local reservations:
NATIVE_FIRST; each PHASE from/to; preparation/acquisition/handoff decisions;
ARRIVED; each typed FAILED reason. Runtime IDs are correlation fields, never
reservation keys. Native repeat details have a separate finite budget.
Suppression summaries cannot consume or replace the first required transition or terminal.
A failed sink is not evidence of physical emission; runtime evidence remains mandatory.

### Java result observation

`lavi/minecraft/fabric/chatclef/bridge/command/diagnostics/gotoresult/` contains:

- `GotoTerminalTaskSnapshot.java`: immutable stored Task facts.
- `GotoTerminalDiagnosticProjector.java`: allowlisted context, Task and actual result fields.
- `GotoTerminalDiagnosticFormatter.java`: finite single-line output.
- `GotoTerminalDiagnostics.java`: failure-isolated emission and original-result pass-through.

The hook is in `FabricChatClefCommandTerminalResultDispatcher.dispatch`, inside
the existing terminal result factory. The existing context commit owns its one-time
invocation and cached result, including transport retries. The hook observes the
factory's actual returned payload; it does not call the factory twice, classify a result,
claim another terminal, or change transmission/retirement ordering.

For PreparedGotoTask it reads only stored `arrived()` and `failureReason()`.
It never calls `isFinished()`, creates goals, samples inventory, moves the player,
or performs cleanup. A generic GetToBlockTask yields unavailable semantic facts.
Observed Task identity and bound-root match are distinct fields: a mismatched
observation is visible and cannot become success through the diagnostic path.
The diagnostic snapshot is not a new wire DTO or production arrival evidence.

Event: `GOTO_TERMINAL_RESULT_OBSERVED`.
Key fields: request/correlation/session/generations, original parsed XYZ/dimension
when available, Task owner/identity, bound-root match, stored owner arrival/failure,
existing STOP binding, actual payload status/ok/reason/fidelity.
Unparsed raw commands and message bodies are omitted.

### Python observation

`fabric/chatclef/diagnostics/goto_terminal/` separates immutable record,
allowlisted projection, bounded formatting, and failure-isolated observer.

Hooks observe the actual evidence evaluator return and already rendered terminal text.
Existing status, evidence profile, typed DTO/fact, STOP arbitration, terminal claim,
Korean sentences and UI/TTS routing retain their behavior.
No success reason is inferred by running evaluation a second time.

Both composition roots are included:

- server runtime graph: late terminal response;
- ordinary submission graph: immediate/coalesced terminal response.

The response observation records metadata and a bounded text fingerprint/length,
not raw conversation or audio. Logging failure must not change the exact returned
evaluation object or sentence. TTS synthesis/playback timing is outside this unit.

### Exact required causal signatures for this evidence pass

Each row denotes owner + physical event + semantic transition/result/reason.
Bracketed alternatives below expand to distinct signatures, not runtime IDs.
Only the branch actually taken is expected in one operation. An untaken branch
must not be reported as emitted or as proof of a different scenario.

| Owner | Event | Required semantic signature |
| --- | --- | --- |
| PreparedGotoTask | GOTO_PREPARATION_NATIVE_FIRST | bound operation begins in NATIVE |
| PreparedGotoTask | GOTO_PREPARATION_PHASE | NATIVE → ARRIVAL_CLEANUP |
| PreparedGotoTask | GOTO_PREPARATION_PHASE | NATIVE → DRAIN_TO_PREPARE |
| PreparedGotoTask | GOTO_PREPARATION_PHASE | PREPARE → DRAIN_TO_FINAL |
| PreparedGotoTask | GOTO_PREPARATION_PHASE | FINAL_NAVIGATION → ARRIVAL_CLEANUP |
| PreparedGotoTask | GOTO_PREPARATION_PREPARATION_SELECTED | local preparation selected; placement demand/required held frozen |
| PreparedGotoTask | GOTO_PREPARATION_PREPARATION_CHECK | DRAIN_TO_PREPARE → PREPARE after cleanup/site validation |
| PreparedGotoTask | GOTO_PREPARATION_MATERIALS_ALREADY_SUFFICIENT | PREPARE; no acquisition; placement demand already held |
| PreparedGotoTask | GOTO_PREPARATION_ACQUIRE_START | PREPARE; acquisition child created |
| PreparedGotoTask | GOTO_PREPARATION_PREPARED | PREPARE; required held threshold satisfied |
| PreparedGotoTask | GOTO_PREPARATION_HANDOFF_CHECK | DRAIN_TO_FINAL; quantityReady=[true,false], separate signatures |
| PreparedGotoTask | GOTO_PREPARATION_RESUME_ORIGINAL | DRAIN_TO_FINAL → FINAL_NAVIGATION; fresh original XYZ child |
| PreparedGotoTask | GOTO_PREPARATION_CLEANUP_WAIT | phase=[DRAIN_TO_PREPARE,DRAIN_TO_FINAL,ARRIVAL_CLEANUP], ticks=[1,40]; each pair distinct |
| PreparedGotoTask | GOTO_PREPARATION_ARRIVED | ARRIVAL_CLEANUP → TERMINAL; stored arrived=true |
| PreparedGotoTask | GOTO_PREPARATION_FAILED | terminal with each exact failure reason enumerated below |
| PreparedGotoTask | GOTO_PREPARATION_NATIVE_DECISION | first occurrence of each exact native reason enumerated below |
| FabricChatClefCommandTerminalResultDispatcher | GOTO_TERMINAL_RESULT_OBSERVED | existing terminal factory returned its fixed payload; stored owner-arrived/failure, bound-root match and actual status/reason remain distinct observations |
| CommandTerminalEvidenceEvaluator | goto_terminal_diagnostic / evidence_decided | role=result_evaluation; actual returned verdict, with each exact branch reason enumerated below |
| CommandLifecycleResponseRenderer | goto_terminal_diagnostic / response_rendered | reason=rendered; role=[late_terminal,immediate_coalesced]; each actual terminal status is a distinct result signature |

Prepared terminal reasons (27):
`WORLD_CHANGED, ROOT_REPLACED, NOT_SURVIVAL, SETTINGS_CHANGED, PLACING_DISABLED,
BREAKING_DISABLED, INTERACTION_PAUSED, INVENTORY_UNAVAILABLE, UNSAFE_ACCEPTED_STACK,
INVENTORY_FULL, NO_SAFE_SOURCE, CANDIDATE_LIMIT, ACQUISITION_TIMEOUT, NO_PROGRESS,
SOURCE_TIMEOUT, SOURCE_INVALIDATED, DROP_NOT_OBSERVED, DROP_LOST, PICKUP_UNCONFIRMED,
TOOL_NOT_READY, OUT_OF_BOUNDS, CLEANUP_TIMEOUT, HANDOFF_SHORTAGE, ARRIVAL_LOST,
AIR_COLUMN_CHANGED, FALLBACK_UNAVAILABLE, INTERNAL_ERROR`.

Native reasons (12):
`AIR_COLUMN_UNAVAILABLE, AERIAL_CLASSIFIED, NATIVE_NOT_SAFE_TO_PAUSE,
MATERIALS_SUFFICIENT_NATIVE_CONTINUES, FAR_FROM_FOUNDATION, BELOW_PREPARATION_AREA,
ABOVE_PREPARATION_AREA, PREPARATION_AREA_UNLOADED, NOT_DRY_GROUNDED,
PREPARATION_UNDER_COVER, PREPARATION_BODY_OBSTRUCTED, PREPARATION_FOOTING_UNSAFE`.

Python evaluator reasons (9):
`invalid_result_type, invalid_result_data, profile_unavailable, profile_not_verified,
descriptor_not_verified, unsupported_detail, evaluator_unavailable,
invalid_evaluation_type, evaluated`.
The current valid GOTO completion takes `profile_not_verified / verified=false`.
Failed/cancelled results do not enter the existing completed-only success evaluator;
their required Python boundary is the renderer observation, preserving that distinction.

Terminal statuses are `completed, failed, rejected, cancelled, deadline_exceeded,
unknown`. For the historical case's narrow bridge signature, check the exact tuple
`bound-root match=true / existing completed / matching_task_finished` alongside
the newly observed owner-arrived/failure fields. A different tuple is recorded as a
different observed outcome; it is not proof of that historical tuple.

Reservations are a bounded superset of reachable Task signatures: at most 112,
plus 32 native detail records and one suppression summary per Prepared instance.
The existing context terminal commit reserves the bridge observation independently;
the existing Python terminal claim/presentation path owns evaluation/render emission.
No random/request/session identifier participates in semantic reservation keys.

## 4. Exact independently reversible unit

This is a new diagnostics-only unit, not RECOVERY_ROLLBACK.

Existing source hunks (all clean before this unit):

1. Runtime `src/main/java/lavi/minecraft/task/movement/gotopreflight/PreparedGotoTask.java`:
   diagnostic import/field and extraction of existing log/decision helper bodies.
2. Runtime `src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/terminal/FabricChatClefCommandTerminalResultDispatcher.java`:
   diagnostic dependency and observation around the already-owned result factory.
3. Python `transport/command_feedback/lifecycle/evidence/command_terminal_evidence_evaluator.py`:
   optional observation after existing decisions.
4. Python `response/command_lifecycle/command_lifecycle_response_renderer.py`:
   optional observation after text creation.
5. Python `transport/server/runtime/fabric_chatclef_server_component_graph.py`:
   observer construction/injection.
6. Python `input/routing/ordinary/submission/ordinary_submission_result_component_graph.py`:
   observer construction/injection.

All Python paths above are relative to `plugins/Minecraft/fabric/chatclef/`.
Runtime paths are relative to `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/`.

New production files are the seven named Java files in Section 3 and
`diagnostics/goto_terminal/{__init__,goto_terminal_diagnostic_record,goto_terminal_diagnostic_projector,goto_terminal_diagnostic_formatter,goto_terminal_diagnostic_observer}.py`.
New tests live in the corresponding Java diagnostic test packages and
`tests/minecraft_chatclef/command_lifecycle/goto_terminal_diagnostics/`.
The isolated verification entry is
`test/test_Isolation/minecraft/goto_terminal_diagnostics/verify_goto_terminal_diagnostics.ps1`.
This document belongs to the same record.

An inverse unit would remove only these six diagnostic hunks and these newly
created diagnostic/test/document files, after rechecking hashes and overlapping edits.
It must not restore whole dirty files, delete any of the 39 preexisting files, or
reuse an old failed GOTO implementation. No rollback is performed or authorized here.

## 5. Gate ledger

```text
GOTO_INCIDENT_RECORD_READ: YES
REPRODUCTION_ID: historical-chat-goto-20260913-125249
ACTIVE_ARTIFACT_SHA256: FD76A9D2F6C186D7DCAEE974A790EEEAE8E8F13395267E225C97E4CFE0FE826B
COMMAND_CORRELATION_ID: lavi-8fa675881e26473684c492959be3b20f
OPERATION_ID: UNKNOWN
NAVIGATION_ROOT_CAUSE_STATUS: UNKNOWN
PARENT_TASK: PreparedGotoTask#22ebb1fc
CHILD_TASK: runtime UNKNOWN; source GetToBlockTask / optional CollectGotoMaterialsTask
FINITE_COUNTER_OWNER: source PreparedGotoTask / CollectGotoMaterialsTask; runtime UNKNOWN
TERMINAL_DECISION_OWNER: PreparedGotoTask -> FabricChatClefCommandOutcomeClassifier
DIAGNOSTIC_BUDGET_OWNER: GotoPreparationLogBudget; existing context terminal commit; existing Python terminal claim
REQUIRED_CAUSAL_OWNER_BOUNDARY_SET: Section 3 Exact required causal signatures table and enumerations
REQUIRED_CAUSAL_OWNER_BOUNDARY_EMISSION_STATUS: UNKNOWN
REQUIRED_TERMINAL_EMISSION_STATUS: UNKNOWN
FIRST_REQUIRED_PHASE_TRANSITION_EMISSION_STATUS: UNKNOWN
DIAGNOSTIC_SUPPRESSION_REASON: old Debug path; exact runtime suppression cause UNKNOWN
PATH_OR_GOAL_OWNER: source GetToBlockTask / Baritone; runtime UNKNOWN
LAST_SUCCESSFUL_BOUNDARY: matching task-finished -> completed payload -> cautious sentence
FIRST_FAILING_BOUNDARY: UNKNOWN at Task semantic-result boundary
TRIGGERING_STATE: XYZ 500 90 -928; screenshot at target; owner ARRIVED UNKNOWN
EXPECTED_TRANSITION: owner terminal -> matching result -> matching sentence
OBSERVED_TRANSITION: generic completed -> arrival unverified sentence
RETRY_OWNER: source existing engine; runtime UNKNOWN
WANDER_OWNER: source existing engine; runtime UNKNOWN
EXACT_FILES_AND_HUNKS_PROPOSED: Section 4
EXACT_PROPOSED_FIX_BOUNDARY: observation only; no behavior fix
FAILED_IMPLEMENTATION_ROLLBACK_SCOPE: NONE
PROPOSED_CHANGE_EXACT_ROLLBACK_UNIT: Section 4 exact diagnostic hunks/new files
USER_APPROVED_EXACT_ROLLBACK_UNIT: NONE
PROPOSED_CHANGE_CLASSIFICATION: DIAGNOSTICS_ONLY
MAXIMUM_NORMAL_SOURCE_CHANGE_CLASSIFICATION: DIAGNOSTICS_ONLY
ACTIVE_WORLD_IDENTITY: UNKNOWN
ACTIVE_WORLD_SAVE_PATH: UNKNOWN
WORLD_COPY_RESTORE_REPLACE_RENAME_HISTORY: UNKNOWN
BARITONE_CACHE_EVIDENCE: not inspected
CACHE_TARGET_ABSOLUTE_PATH: UNKNOWN
CACHE_ACTION: NONE
TASK_STOPPED: UNKNOWN
TASK_STOPPED_EVIDENCE_STATUS: UNKNOWN
WORLD_EXITED: UNKNOWN
WORLD_EXITED_EVIDENCE_STATUS: UNKNOWN
MINECRAFT_PROCESS_CLOSED: NO
MINECRAFT_PROCESS_CLOSED_EVIDENCE_STATUS: VERIFIED_RUNTIME at pre-edit read-only audit
NOT_APPLICABLE_REASON: no fields marked NOT_APPLICABLE
ACTIVE_REQUEST_AUTHORIZES_SOURCE_EDIT: YES
ACTIVE_REQUEST_AUTHORIZES_RECOVERY_ROLLBACK: NO
ACTIVE_REQUEST_AUTHORIZES_BUILD: YES
ACTIVE_REQUEST_AUTHORIZES_DEPLOYMENT: NO
ACTIVE_REQUEST_AUTHORIZES_MINECRAFT_LAUNCH: NO
ACTIVE_REQUEST_AUTHORIZES_EXTERNAL_INSTANCE_COPY: NO
ACTIVE_REQUEST_AUTHORIZES_RUNTIME_REPRODUCTION: NO
ACTIVE_REQUEST_AUTHORIZES_LIVE_WORLD_EXECUTION: NO
ACTIVE_REQUEST_AUTHORIZES_LIVE_WORLD_BLOCK_MUTATION: NO
ACTIVE_REQUEST_AUTHORIZES_TASK_STOP: NO
ACTIVE_REQUEST_AUTHORIZES_WORLD_EXIT: NO
ACTIVE_REQUEST_AUTHORIZES_MINECRAFT_PROCESS_CONTROL: NO
ACTIVE_REQUEST_AUTHORIZES_MANUAL_CACHE_MUTATION: NO
ACTIVE_REQUEST_AUTHORIZES_COMMIT: NO
ACTIVE_REQUEST_AUTHORIZES_PUSH: NO
```

## 6. Verification results

### Source and focused tests

- Preexisting 39 dirty-file SHA-256 values: unchanged after implementation/tests.
- PreparedGotoTask: all 24 behavior method bodies equal HEAD after CRLF normalization;
  only diagnostic imports/fields/helpers differ.
- Independent Java/Python read-only reviews: no concrete code defect found.
  The review requested the exact causal-signature table now included in Section 3.
- Scoped/global `git diff --check`: passed.
- Java isolated compilation: `javac --release 17 -proc:none -implicit:none`, passed.
- Jupiter tests: **19 passed, 0 failed/aborted/skipped**. The first run used existing
  named 1.20.1 dependency classes. The second run used the freshly compiled 1.20.1
  classes from the targeted remap build, and also passed all 19.
- Python related regression: **430 passed, 10,978 subtests passed in 9.24 s**, exit 0.

Python command, from the repository root:

```powershell
& .\venv\Scripts\python.exe -B -m pytest -p no:cacheprovider tests/minecraft_chatclef/command_lifecycle tests/minecraft_chatclef/lavi_input tests/minecraft_chatclef/structure tests/minecraft_chatclef/crafting_feedback tests/test_minecraft_chatclef_natural_language_service.py tests/test_minecraft_chatclef_korean_rule_parser.py -q
```

Python results were captured by the execution tool; no separate Python log file was saved.
Java commands, source-input hashes, and test logs are under
`test/test_Isolation/minecraft/goto_terminal_diagnostics/`:

- first output: `output/2bb2fe916d634a60b216b6e037e17462/`;
- fresh-class output: `output/7e0d317a5cc842b59fb05280220de530/`.

Direct PowerShell script invocation was refused by the existing execution policy.
The reviewed verification commands were then executed as ordinary PowerShell command
text. No execution-policy or system setting was changed. The Java warnings concern
the preexisting TestObjects Unsafe/ReflectionFactory helper and the existing Jupiter
runner's deprecated API, not a failed diagnostic-source compilation.

### Build results — separate outcomes

Runtime working directory:
`C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1`.
JDK and Gradle JVM: Eclipse Temurin **21.0.12+8**; Gradle **8.8**.
The 1.20.1 compile target remains Java 17; all seven new diagnostic classes in
the remapped JAR have class-file major version **61**.

Each build directory below is relative to
`test/test_Isolation/minecraft/goto_terminal_diagnostics/builds/` and contains
the exact command, full output and `build-inputs.txt` with dirty runtime source hashes.
All dirty runtime source/test inputs are this diagnostic unit; the historical
baseline and retained local cache are recorded separately. Build provenance remains
**MIXED_PROVENANCE**, including retained local Maven metadata whose remote authorship
is not independently verified.

| Run | Result | Evidence directory |
| --- | --- | --- |
| Canonical clean build | Exit 1 before source compilation: fabric-loom 1.7-SNAPSHOT plugin resolution | 20260913-133911-47fbad76 |
| Same clean build with existing cache/init script in offline mode | Exit 1 at 1.21.1 compileJava; 1.20.1 clean was executed | 20260913-134022-ba0d1f33 |
| 1.20.1 build/test graph after that clean | Exit 1 at the same 1.21.1 dependency compile boundary | 20260913-134306-8f13e351 |
| 1.20.1 remapJar, forced tasks, existing cache | Exit 0; BUILD SUCCESSFUL in 2 m 2 s; targeted packaging only | 20260913-134508-57b995e9 |

Canonical command:

```powershell
.\gradlew.bat clean build --rerun-tasks --no-build-cache --no-daemon --stacktrace
```

The offline retry appended
`--offline --init-script .gradle/codex-build-init.gradle`.
The retained init script hash is
`CBAD217C20223027508048187A6FC5F32935AA265F200CB7DA40D5EAC00D0483`,
matching the prior build record. No dependency version, checked-in build configuration,
global JDK/environment setting, or external cache was edited.
Writable Gradle cache and temporary output stayed inside the repository;
`C:\Users\jaewo\.gradle\caches` was supplied as a read-only dependency cache.

The offline/full and target-build errors are in **untouched, HEAD-identical files**:

- `GotoMaterialInventory.java:67,92`: `ItemStack.hasNbt()` unavailable in the
  selected 1.21.1 API;
- `GotoMaterialSources.java:148`: enchantment RegistryKey/RegistryEntry mismatch.

They are not repaired in the diagnostics unit. Their presence in the current source
is proven; no claim is made about when the broader multi-version build last passed.

Successful targeted command:

```powershell
.\gradlew.bat :1.20.1:remapJar --rerun-tasks --no-build-cache --no-daemon --stacktrace --offline --init-script .gradle/codex-build-init.gradle
```

This consumed the earlier canonical retry's clean outputs and freshly compiled
1.20.1 source. It does not replace the failed full clean build or full Gradle test graph.
Final full-build outcome is **BUILD_FAILED**, with **TARGETED_1201_JAR_CREATED**.

### Artifact and runtime status

Fresh candidate:
`plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar`

- Size: **8,465,898 bytes**.
- Modified UTC: **2026-09-13T04:47:11.9080179Z**.
- SHA-256: `4B9CF0F9477B9899C7BC7D429CB195B474DF0A89AD6201AAE8BFD04313736887`.
- Seven new diagnostic classes are present.
- Exact artifact metadata is saved in the remap run's `artifact-proof.json`.

The old repository artifact was preserved under the first build evidence directory
as `before-build-chatclef-1.20.1-0.18.23.jar` before clean.
The installed CurseForge JAR remains unchanged at SHA-256
`FD76A9D2F6C186D7DCAEE974A790EEEAE8E8F13395267E225C97E4CFE0FE826B`.
No new JAR was copied into the instance.

```text
DEPLOYMENT: NOT_RUN
MINECRAFT_LAUNCH: NOT_RUN
LIVE_GOTO_REPRODUCTION: NOT_RUN
CHAT_MICROPHONE_ACCEPTANCE: NOT_RUN
RUNTIME_MIXIN_VERIFICATION: NOT_RUN
NEW_CRASH_REPORT_REVIEW: NOT_RUN (no new launch performed)
AUTHORITATIVE_ARRIVAL_RESPONSE_IMPLEMENTATION: NOT_IMPLEMENTED_IN_THIS_UNIT
TTS_LATENCY_CHANGE: NOT_IMPLEMENTED_IN_THIS_UNIT
COMMIT: NOT_RUN
PUSH: NOT_RUN
```

## 7. Next evidence pass and later implementation

The complete build remains blocked at the recorded multi-version compile boundary.
The targeted candidate is not full-build or runtime acceptance.
After resolving the applicable build-verification boundary and a separately
authorized fresh-JAR deployment and controlled runtime test,
join the owner phase/terminal, Java result observation, received Python result,
actual evaluator decision and rendered response by request/session/correlation and
Task identity. Record exact last successful/first failing boundaries and stop/replacement
ownership. Preserve unavailable values as unavailable.

A source-only isolated test cannot prove this live chain. Same-height/downward
GetToBlockTask has no new authoritative arrival snapshot in this diagnostic unit.
The later behavior unit must review that owner separately before promoting its result.
XZ/Y-only/dimension-only/cross-dimension/old payloads keep the existing cautious semantics.

Only after the applicable incident gate passes may a separate minimal behavior unit
connect verified success and typed failure facts to Korean responses. TTS latency
measurement/improvement remains a separate later unit.
