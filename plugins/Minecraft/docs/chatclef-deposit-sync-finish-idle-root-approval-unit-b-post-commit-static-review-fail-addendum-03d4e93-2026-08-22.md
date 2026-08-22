<!-- 20260822_kpopmodder: Reviewed-v2 records the remaining Approval unit B blockers after 03d4e93 and narrows the next edit-only proof contract. -->

# ChatClef Deposit Sync-Finish Idle Root Approval Unit B Post-Commit Static Review Fail Addendum - 03d4e93 - 2026-08-22

Date: 2026-08-22 KST

Status:

```text
document status: APPROVAL_UNIT_B_POST_COMMIT_STATIC_REVIEW_FAIL_ADDENDUM
document revision: reviewed-v2
reviewed source baseline: 03d4e93b36ffdde4936da274eab72632569ea5b0
Approval unit B verdict: FAIL
source code change in this document pass: none
test source change in this document pass: none
document change in this document pass: this replacement reviewed-v2 addendum only
Gradle/JUnit/compile/build execution in this document pass: none
JAR copy/Minecraft runtime in this document pass: none
implementation approval granted by this document: no
test/build/runtime approval granted by this document: no
commit/push approval granted by this document: no
next proposed phase: separately approved Java/JUnit/document edit-only static remediation
```

This addendum records the remaining Approval unit B blockers after commit
`03d4e93b36ffdde4936da274eab72632569ea5b0`.

It does not replace the investigation, implementation plan, reviewed-v2
post-commit FAIL document after `d5da93b`, or the historical post-edit/build
report. It narrows the next remediation and evidence contract for the defects
found in the `03d4e93` source state.

## Input Integrity Note

The initially reported addendum was:

```text
repository path:
plugins/Minecraft/docs/chatclef-deposit-sync-finish-idle-root-approval-unit-b-post-commit-static-review-fail-addendum-03d4e93-2026-08-22.md

reported SHA-256:
AEEE1A15403B241A46F3AAA50221D508DA8070F68365581168605D696E8FD84C
```

The bytes of that reported `AEEE...` document were not available to the
external reviewer. The supplied attachment was the earlier `d5da93b`
post-commit reviewed-v2 document, with SHA-256
`D2F46A78229FA2D82F66A514336D0751E206ADE0E049B8C8DF56A055CF501C70`.

Therefore this file is a complete replacement reviewed-v2 contract, not a
line-by-line patch against the reported `AEEE...` bytes. Before replacing the
repo document, the local file must still have the reported `AEEE...` SHA. If it
does not, stop instead of overwriting an unreviewed variant.

## Review Input

Reviewed source archive:

```text
ZIP: LAVI-minecraft-plugin-fix-alto-clef-infinite-loop (40).zip
ZIP SHA-256: C8BC86013A9C1DA2D42E0B7C3BC122C952677C1318699216E65CE082A74CF999
archive comment / source commit:
03d4e93b36ffdde4936da274eab72632569ea5b0
review method: static Java/JUnit/document contract inspection
```

Historical post-edit report in that source archive:

```text
path:
plugins/Minecraft/docs/chatclef-deposit-sync-finish-idle-root-approval-unit-b-post-edit-report-2026-08-22.md

SHA-256:
B912A23C1D089A4CAB135F162621E957F6B56EABFFB5396F11B38E90E67F1DB2
```

Not independently executed by this review:

```text
Gradle
JUnit
Java compile
build
JAR load or copy
Minecraft runtime
runtime log reproduction
```

## Verdict

```text
Approval unit B: FAIL
build/runtime advancement from this review: prohibited
remaining blockers:
  1. nonterminal publisher monotonic clock/evidence capture ordering
  2. request-owned-root state and request_root_reappeared latch semantics
  3. same_session_generation active queue-context proof
  4. historical post-edit provenance/build-source reconciliation
```

The `03d4e93` remediation correctly fixed several earlier blockers, but its
nonterminal stability evidence can still fail closed for the wrong reason, and
its request-root history can still qualify a request-owned root as neutral.
Approval unit B therefore remains FAIL.

## Confirmed Improvements In 03d4e93

The following earlier findings are statically accepted as corrected:

```text
PREEXISTING_UNCHANGED_IDLE_ROOT requires the same captured client tick.
PREEXISTING_UNCHANGED_IDLE_ROOT requires equal nonblank capture-thread evidence.
Raw root identity, IdleTask type, assignment/generation, and snapshot consistency
  are checked fail-closed.
Across-tick evidence now resolves to OWNERSHIP_UNKNOWN.
Dispatcher detach testing uses a package-private final cancellation-action seam.
Preexisting, ownership-unknown, different-root, and no-lifecycle detach paths
  prove cancellation-action invocation count 0.
Exact command-owned bound root proves cancellation-action invocation count 1.
finish_callback_observed_nonterminal maps explicitly to
  callback_without_matching_user_task_event.
stable_request_quiescence_observed maps explicitly to
  callback_without_matching_user_task_event.
diagnosticPayload(...) maps explicitly to unknown while preserving its reason.
Both stability producers now use monotonic duration calculations.
Snapshot age is no longer intentionally emitted as a fixed 0 value.
A post-edit evidence report exists.
```

These improvements are necessary but are not sufficient for Approval unit B
PASS.

# Blocking Findings

## P1 - Nonterminal Publisher Captures nowNanos Before Ownership Evidence

Affected production path:

```text
FabricChatClefNonterminalLifecycleEvidencePublisher.publishIfEligible(...)
  -> System.currentTimeMillis()
  -> System.nanoTime()
  -> taskStateReader.ownershipEvidence()
  -> FabricChatClefStableRequestQuiescenceTracker.observe(...)
```

`ownershipEvidence()` captures its own `capturedAtNanos` after the publisher has
already captured `nowNanos`. The tracker rejects a future monotonic capture:

```text
nowNanos < currentEvidence.capturedAtNanos
  -> snapshot_age_ms = Long.MAX_VALUE
  -> blocked_reason = current_ownership_stale
  -> stability window reset
```

For the normal publisher call order, the evidence timestamp is ordinarily newer
than the supplied `nowNanos`. This makes `stable_request_quiescence_observed`
effectively unreachable through the actual publisher path even though direct
tracker tests with hand-authored timestamps can pass.

This is a production-path defect, not only a diagnostic formatting defect.

### Required source correction

```text
1. Capture one coherent FabricChatClefTaskOwnershipEvidence object first.
2. Capture nowNanos after ownershipEvidence() returns.
3. Capture nowMs from the same observation call after the evidence capture.
4. Derive current root, task snapshot, ownership snapshot, client tick, and
   capturedAtNanos from that one evidence object.
5. Use the same nowNanos and the same snapshot_age_ms value for both the stale
   predicate and emitted payload.
6. Missing, nonpositive, or genuinely future capturedAtNanos remains stale;
   do not hide a bad order by clamping it to 0 ms.
```

The public production constructor must retain system clocks. A package-private
constructor may accept narrow `LongSupplier` clock seams for tests. Do not add a
public clock hook, static mutable clock, global mutable test state, sleep-based
test, or new wire field.

### Required publisher-boundary tests

A new or equivalently scoped publisher test must exercise
`FabricChatClefNonterminalLifecycleEvidencePublisher.publishIfEligible(...)`, not
only call the tracker directly.

Required proof:

```text
first eligible observation:
  emitted lifecycle_evidence.stage = finish_callback_observed_nonterminal
  evidence_sequence = 1
  qualified = false

later coherent observations on distinct client ticks and >= existing duration:
  emitted lifecycle_evidence.stage = stable_request_quiescence_observed
  evidence_sequence = 2
  qualified = true
  blocked_reason = none
  snapshot_age_ms != Long.MAX_VALUE
  snapshot_age_ms equals the value used by the stale predicate
  stable_duration_ms >= the existing configured threshold

future or invalid evidence supplied intentionally by the fixture:
  no stable sequence 2
  blocked_reason = current_ownership_stale
```

The test may inject a result sender that records accepted payloads. It must not
use a real network connection, Gradle timing, `Thread.sleep`, or Minecraft
runtime.

## P1 - request_root_reappeared Is Not A Truthful Execution-Lifetime Latch

Affected source:

```text
FabricChatClefStableRequestQuiescenceTracker
```

Current control flow computes `requestRootObservationState(...)`, then returns
early on a precondition block, and only afterward latches
`requestRootReappeared` when the state is `STILL_PRESENT`.

This creates two unsafe cases:

```text
exact bound non-Idle root:
  state calculation can produce STILL_PRESENT
  precondition returns current_root_not_neutral first
  resetWindow() clears requestRootReappeared
  emitted request_root_reappeared remains false

exact bound IdleTask:
  state calculation produces OBSERVED_NEUTRAL_ROOT_STABLE
  neutral-root precondition passes
  STILL_PRESENT latch is never set
  the exact request-owned root can satisfy the stability window
```

The second case can produce `qualified=true` while the exact bound root remains
current. The publisher timestamp-order defect currently masks that path by
making evidence stale; fixing only the publisher order would expose the false
qualification.

The current tracker also clears `requestRootReappeared` when the stability
signature changes and inside `resetWindow()`. That loses historical evidence
that a request-owned root returned during the same execution.

### Required request-root state contract

Only the established request-owned-root states are authorized:

```text
OBSERVED_AND_GONE
NEVER_OBSERVED
STILL_PRESENT
UNKNOWN
```

`OBSERVED_NEUTRAL_ROOT_STABLE` is not an authorized request-root observation
state and must be removed from this tracker path.

State and latch rules:

```text
fresh coherent current root is the exact bound-root Java object:
  request_root_observation_state = STILL_PRESENT
  request_root_reappeared = true
  applies even when the bound root class is IdleTask
  qualified = false

execution has no bound root:
  request_root_observation_state = NEVER_OBSERVED

bound root was recorded and fresh coherent current root is a different approved
neutral root:
  request_root_observation_state = OBSERVED_AND_GONE

current evidence is missing, unavailable, stale, future-dated, or ambiguous:
  request_root_observation_state = UNKNOWN
  stale/ambiguous evidence must not newly set or clear the historical latch
```

Exact bound-root identity must be evaluated before neutral-class acceptance.
The latch must be updated from fresh coherent identity evidence before any early
return that reports a blocked observation.

### Latch lifetime

`requestRootReappeared` is execution-lifetime history:

```text
resetWindow() or signature change:
  reset count, duration, tick, and signature fields only
  preserve requestRootReappeared

full reset() or tracked execution replacement:
  clear requestRootReappeared

requestRootReappeared == true:
  qualified must remain false for the rest of that execution
```

A current state of `OBSERVED_AND_GONE` after a previous reappearance does not
clear the latch. Current state and historical reappearance are separate facts.

### Required tracker tests

```text
fresh exact bound non-Idle root:
  state = STILL_PRESENT
  request_root_reappeared = true
  qualified = false

fresh exact bound IdleTask:
  state = STILL_PRESENT
  request_root_reappeared = true
  qualified = false

exact bound root appears, then a different neutral IdleTask appears:
  current state may become OBSERVED_AND_GONE
  request_root_reappeared remains true
  qualified remains false

signature/window reset during the same execution:
  request_root_reappeared remains true

new execution object:
  request_root_reappeared resets to false

stale or future-dated current evidence:
  state = UNKNOWN
  does not newly set or clear the latch
```

No active release, retry, replay, terminal outbox, or command-profile behavior
may be changed to make these tests pass.

## P2 - same_session_generation Does Not Compare The Active Queue Context

Affected source:

```text
FabricChatClefPreexistingIdleRootStabilityGate.sameSessionGeneration(...)
FabricChatClefStableRequestQuiescenceTracker.sameSessionGeneration(...)
```

Current logic effectively proves only:

```text
execution is non-null
execution is the producer's trackedExecution
session_id is nonblank
connection_generation is nonnegative
```

The producer assigns the supplied execution to `trackedExecution` before this
check. It does not compare the execution context to the current active command
queue context, and it does not compare expected and observed session/generation
values from two identities.

`FabricChatClefCommandLifecycleCoordinator.syncActiveContext(...)` already has
an exact active-context object guard, but the proof is not supplied to the two
stability producers. The payload therefore claims a comparison that the
producer did not perform.

### Required active-context proof

Use the actual active queue context from the current coordinator tick, or a
narrow typed immutable proof derived immediately from that context. Do not let a
test or arbitrary caller supply an unexplained `true` boolean.

The value is true only when all are true:

```text
active queue context is present
execution.context() is the exact same FabricChatClefCommandContext object as the
  active queue context
both session_id values are nonblank and value-equal
both connection_generation values are nonnegative and equal
```

The same calculated proof must be used by the predicate and payload.

For both the preexisting-idle gate and nonterminal tracker:

```text
same_session_generation = false
  -> qualified = false
  -> blocked_reason = active_context_identity_mismatch
```

This is an evidence-consistency correction. It must not create a new active
context, replace queue ownership, release ownership, or alter reconnect/retry
semantics.

### Narrow implementation boundary

A narrow signature change in the coordinator, gate, publisher, and tracker is
allowed to pass the current active queue context. Creating a separate helper
class is not required. If a new typed helper file becomes necessary, stop and
report its planned path and pre-edit/nonexistent provenance before adding it.

### Required tests

```text
exact active context object, equal nonblank session, equal nonnegative generation:
  same_session_generation = true

non-null execution but different active context object:
  same_session_generation = false
  qualified = false

same session but different generation:
  same_session_generation = false
  qualified = false

different session but same generation:
  same_session_generation = false
  qualified = false

missing active context, blank session, or negative generation:
  same_session_generation = false
  qualified = false
```

The production-boundary map must explain where the active queue context is
obtained and where the exact comparison is performed.

## P2 - Historical Post-Edit Provenance Does Not Identify The Current Build Source Set

The historical post-edit report records this dispatcher post-edit hash:

```text
FabricChatClefCommandDispatcher.java
143C53768D4182507BE2A9D6E7C323DC5022E0DB84FE8ADD5B89C92A52216B11
```

The dispatcher in the reviewed `03d4e93` archive has this SHA-256:

```text
5AFBB6D923A453F7888EFB46957920BF3E665AD415F3F492AF8980D2A764D8FC
```

The historical report also records pre-edit HEAD/status, but it does not record
the actual post-edit HEAD and `git status --short` for the source state that was
later built and committed.

The mismatch may be a report typo or an unrecorded source transition. The
evidence package cannot choose between those explanations without additional
provenance. Therefore it cannot claim:

```text
current 03d4e93 source set
  == source set used by the reported clean build
  == source set represented by the reported JAR
```

### Required reconciliation

Do not silently rewrite the historical report. The next post-edit evidence
report must include a section named `Historical 03d4e93 provenance
reconciliation` with:

```text
03d4e93 branch and full HEAD
current git status --short before the new remediation
current SHA-256 of every file touched by 03d4e93 that is relevant to this pass
historical reported dispatcher SHA
actual 03d4e93 dispatcher SHA
a supported explanation if one can be proven
otherwise: build_source_identity = UNKNOWN
```

The next edit-only remediation changes source again. The previous successful
build and JAR are historical evidence only and do not validate the newly edited
source. The next report must state:

```text
previous build applies to new uncommitted remediation source: no
new source compile/JUnit/build status: NOT RUN in this edit-only phase
```

Do not rerun Gradle, JUnit, build, copy a JAR, or launch Minecraft under this
static-remediation approval.

# Additional Tracked Risk - Not In This Remediation

`FabricChatClefCommandExecutionState.markTaskFinishedObservation(...)` can still
overwrite an earlier `TaskFinishedEvent` observation with a later event. If a
terminal send fails and active ownership remains, a later classification can be
built from different mutable event evidence.

This remains a separately tracked lifecycle risk. Do not fold a first-event
latch, terminal-decision snapshot, or related coordinator redesign into the
current remediation without a separate defect document and approval.

# Required Next Java/JUnit/Document Edit-Only Static Remediation

The next pass may begin only after an explicit user handoff outside this FAIL
document.

## Fresh pre-edit provenance

Before changing any file, record:

```text
repository root
git rev-parse --show-toplevel
branch
full HEAD
git status --short
reported current addendum SHA before replacement
planned existing-file edit list
pre-edit SHA-256 for every planned existing Java/JUnit/document file
ABSENT/NEW marker for each planned new test/report file
```

The replacement of this addendum and the later Java/JUnit remediation must not
be presented as one indistinguishable edit. Record document replacement first,
then capture fresh remediation provenance from the resulting working tree.

## Authorized Java source boundary

Expected existing Java files:

```text
FabricChatClefCommandLifecycleCoordinator.java
FabricChatClefPreexistingIdleRootStabilityGate.java
FabricChatClefNonterminalLifecycleEvidencePublisher.java
FabricChatClefStableRequestQuiescenceTracker.java
```

Only narrow changes required for:

```text
publisher evidence-before-clock ordering
package-private clock seam if needed for deterministic publisher testing
request-root state and execution-lifetime latch correction
current active queue-context propagation and exact session/generation proof
```

No broad lifecycle, dispatcher, queue, result-outbox, transport, or protocol
refactor is authorized.

## Authorized JUnit source boundary

Expected test work:

```text
modify FabricChatClefStableRequestQuiescenceTrackerTest.java
modify FabricChatClefPreexistingIdleRootStabilityGateTest.java
add FabricChatClefNonterminalLifecycleEvidencePublisherTest.java or an
  equivalently narrow package-local publisher-boundary test
```

A coordinator-level test may be added only if the existing tests cannot prove
that the actual active queue context reaches both producers. Before adding a new
coordinator test file, report its planned path and `ABSENT/NEW` provenance.

JUnit source may be edited but not executed in this phase.

## Authorized document boundary

After source/test edits, add one new post-edit static-remediation report. Do not
rewrite the historical build report.

Required report contents:

```text
touched files
pre-edit and post-edit SHA-256 values
post-edit full HEAD
git status --short
confirmation that HEAD is unchanged because commit/push were not authorized
publisher capture-order table
publisher sequence 1 -> 2 production-boundary map
request-root current-state table
request_root_reappeared execution-lifetime latch table
active queue-context/session/generation comparison table
gate and tracker qualification/blocking matrix
historical 03d4e93 provenance reconciliation
statement that the previous build does not cover the new source
all commands performed
all commands explicitly not performed
```

The new report must not embed its own SHA-256. Report its final hash only after
its bytes are fixed, in the Codex handoff or a separate non-self-referential
manifest.

# Explicitly Prohibited

```text
Gradle execution
JUnit execution
Java compile
build
JAR copy or deployment
Minecraft launch or runtime reproduction
Python changes
adris/** changes
Forge/MineMind changes
dependency or version changes
stability threshold changes
new result-envelope fields
new result_fidelity values
public production test hooks
global mutable clock or test state
Thread.sleep timing tests
active-release behavior changes
retry/replay/StopCommand behavior changes
terminal outbox behavior changes
queue ownership replacement
broad coordinator/dispatcher/outbox refactor
TaskFinishedEvent overwrite remediation in this pass
modification of the historical post-edit/build report
commit
push
```

If an additional Java/JUnit/document file is genuinely required, do not edit it
first. Report its path, purpose, and pre-edit SHA-256 or `ABSENT/NEW` marker for
separate scope review.

# Current Verification Status

```text
03d4e93 archive identity: PASS by ZIP archive comment
same tick/thread ownership classifier: PASS by static review
detach cancellation-action boundary: PASS by static review
result_fidelity explicit mapping: PASS by static review
monotonic duration formulas: PASS by static review
nonterminal publisher evidence/clock order: FAIL
stable_request_quiescence_observed production reachability: FAIL
request_root_reappeared truthfulness: FAIL
exact bound IdleTask false-qualification prevention: FAIL
same_session_generation active-context proof: FAIL
historical build/source identity reconciliation: FAIL
independent JUnit/Gradle/build verification by reviewer: NOT RUN
Minecraft runtime edge verification: NOT RUN
Approval unit B: FAIL
```

# Non-Execution And Non-Mutation Statement

For this reviewed-v2 document pass, no Java source, JUnit source, Python source,
Gradle configuration, dependency, JAR, or runtime file was modified. No Gradle,
JUnit, Java compile, build, JAR copy, Minecraft runtime, commit, or push was
performed.
