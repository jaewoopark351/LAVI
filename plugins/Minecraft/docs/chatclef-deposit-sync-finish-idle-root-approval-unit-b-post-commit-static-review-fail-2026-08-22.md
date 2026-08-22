<!-- 20260822_kpopmodder: Reviewed-v2 clarifies the post-commit Approval unit B static-remediation proof contract after d5da93b. -->

# ChatClef Deposit Sync-Finish Idle Root Approval Unit B Post-Commit Static Review Fail - 2026-08-22

Date: 2026-08-22 KST

Status:

```text
document status: APPROVAL_UNIT_B_POST_COMMIT_STATIC_REVIEW_FAIL_FOLLOWUP
document revision: reviewed-v2
source code change in this pass: none
test source change in this pass: none
document change in this pass: this follow-up addendum only
Gradle/build/test execution in this pass: none
runtime/JAR/Minecraft execution in this pass: none
implementation approval granted by this document: no
test execution approval granted by this document: no
runtime/JAR deployment approval granted by this document: no
commit/push approval granted by this document: no
next proposed phase before execution: separately approved Java/JUnit/document edit-only static-remediation pass
```

This document records the follow-up external static-review verdict for Approval
unit B after commit `d5da93b` was pushed to
`minecraft-plugin-fix/alto-clef-infinite-loop`.

It does not replace the 2026-08-21 investigation, implementation plan,
pre-change report, pre-edit evidence package, scope clarification, or the
earlier 2026-08-22 static-review fail addendum. The earlier addendum recorded
the pre-`d5da93b` blockers. This document records the remaining blockers after
the first remediation commit.

The `reviewed-v2` revision does not change the FAIL verdict and does not grant
implementation approval. It clarifies that the next pass includes one scoped
evidence-document change, requires fresh pre-edit provenance, and must derive
stability diagnostics from the same evidence values used by the corresponding
predicate. It also closes ambiguities around blank-thread evidence,
`request_root_reappeared`, and report self-hash handling.

## Review Input

Initial follow-up document supplied for this `reviewed-v2` correction:

```text
repository path: plugins/Minecraft/docs/chatclef-deposit-sync-finish-idle-root-approval-unit-b-post-commit-static-review-fail-2026-08-22.md
initial revision SHA-256: D368C5E1CD7FEFFAF3AE5C29EED478D15C40D0BF8A0B54D1390F5B11E41D5A2B
```

External review text attachment:

```text
path: C:\Users\jaewo\.codex\attachments\17d4d74d-9300-4221-85eb-dc8306d1cff3\pasted-text.txt
SHA-256: DF66CEDB60BE3D3BF9760DE6420CD9CDD1D7953EAF278012A6B376F1DAC16B71
```

Reviewed implementation ZIP reported by that external review:

```text
ZIP: LAVI-minecraft-plugin-fix-alto-clef-infinite-loop (39)(1).zip
ZIP SHA-256: c36f0bd95aea0e0f6c2950f28e9a6468ab6ce5f4eebaf43be9bc7a1d463b3854
review method: static source, test source, and document-contract comparison
not independently verified by reviewer: Gradle, JUnit, build, JAR load, Minecraft edge runtime
```

Local repository context when this follow-up document was created:

```text
repository root: C:\Vtuber_Souorce_Code\LAVI
git toplevel: C:/Vtuber_Souorce_Code/LAVI
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: d5da93ba30f57dcb6b2f06e158b2a26b90af4083
HEAD subject: fix(minecraft): gate ChatClef task finish ownership
```

Because the reviewed ZIP does not contain `.git`, this document does not claim
that the ZIP bytes are identical to the remote branch tip. The external review
reported that the ZIP file composition and core implementation shape matched
the pushed `d5da93b` change.

## Verdict

```text
Approval unit B: FAIL
reason: remaining ownership, side-effect proof, result-fidelity, diagnostics, and evidence-package gaps
execution stage: do not advance to build/runtime approval from this review
```

Commit `d5da93b` correctly addressed the two previously blocking P1 issues for
`OWNERSHIP_UNKNOWN` completion and unsafe unbound `TaskFinishedEvent`
association. However, the follow-up review found additional blockers that still
prevent Approval unit B from passing.

## Confirmed Improvements In d5da93b

The external review accepted these corrections as properly reflected:

```text
OWNERSHIP_UNKNOWN no longer routes to COMPLETED.
OWNERSHIP_UNKNOWN remains nonterminal under root_ownership_unknown.
COMMAND_OWNED_ROOT with no bound root no longer routes to no-user-task completion.
PREEXISTING_UNCHANGED_IDLE_ROOT unbound TaskFinishedEvent handling is audit-only.
OWNERSHIP_UNKNOWN unbound TaskFinishedEvent handling is audit-only.
TaskFinishedEvent association tests now reach the coordinator tick boundary.
The first callback observation is latched and duplicates no longer overwrite it.
Terminal send immediate failure, async failure, and retry-backoff cases preserve active ownership.
```

These improvements are necessary but not sufficient for Approval unit B PASS.

## Blocking Findings

### P1 - PREEXISTING_UNCHANGED_IDLE_ROOT Classification Is Too Loose

The implementation plan requires before/after ownership evidence to be captured
on the dispatch/client thread in the same client tick before the bridge may
classify an unchanged idle root as preexisting.

Implementation-plan anchor:

```text
plugins/Minecraft/docs/chatclef-deposit-sync-finish-idle-root-implementation-plan-2026-08-21.md
lines 539-554
```

Static review concern:

```text
FabricChatClefRootOwnershipClassifier does not require:
  before.capturedClientTick == after.capturedClientTick
  before.captureThread value-equals after.captureThread
  nonblank captureThread on both evidence objects
  raw root and ownership snapshot root-class consistency
```

The review also found that the current test expectation preserves the unsafe
behavior:

```text
sameIdleRootAcrossTickBoundaryStillClassifiesAsPreexistingUnchangedIdleRoot()
current expectation: PREEXISTING_UNCHANGED_IDLE_ROOT for tick 10 vs tick 11
required expectation: OWNERSHIP_UNKNOWN
```

Required remediation:

```text
before.capturedClientTick must equal after.capturedClientTick.
before.captureThread must value-equal after.captureThread.
both captureThread values must be nonblank.
before and after raw roots must be the exact same non-null Java object.
before and after raw roots must each be instanceof IdleTask.
before and after ownership-snapshot user_task_root_class evidence must be
  nonblank and must not contradict the raw IdleTask evidence.
The class-name string is corroborating evidence only; it must not replace exact
  raw-object identity and instanceof IdleTask checks.
Any missing, unavailable, cross-tick, cross-thread, blank-thread, or contradictory
  evidence must fail closed to OWNERSHIP_UNKNOWN.

Do not invent a new elapsed-time freshness threshold for the immediate
before/after dispatch pair in this remediation. The approved predicate is based
on availability, same tick, same nonblank thread, exact object identity,
assignment/generation continuity, idle flags, and raw/snapshot consistency.
```

Required classifier tests:

```text
different client tick -> OWNERSHIP_UNKNOWN
different capture thread -> OWNERSHIP_UNKNOWN
blank before or after capture thread -> OWNERSHIP_UNKNOWN
unavailable before or after evidence -> OWNERSHIP_UNKNOWN
same object but changed assignment id -> not PREEXISTING_UNCHANGED_IDLE_ROOT
same object but changed generation -> not PREEXISTING_UNCHANGED_IDLE_ROOT
contradictory raw root and ownership-snapshot root class -> OWNERSHIP_UNKNOWN
```

### P1 - Detach Tests Do Not Prove The Actual cancelUserTask() Side Effect

The follow-up review accepted that the test now reaches the production
dispatcher boundary, but found that it still asserts only diagnostic text and
queue state.

Current proof gap:

```text
Observed by test:
  lastDetachCancelAction
  lastBoundRootOwnershipForDetach
  queue.hasActive()

Not proven by test:
  PREEXISTING_UNCHANGED_IDLE_ROOT causes actual cancellation invocation count == 0
  COMMAND_OWNED_ROOT with the exact same task instance causes actual cancellation invocation count == 1
```

Static review concern:

```text
FabricChatClefCommandDispatcher selects a detach branch before the helper tries to cancel.
The helper returns without cancellation when AltoClef.getInstance() or getUserTaskChain() is absent.
The current test harness does not install an AltoClef singleton or inject a cancellation counter.
Therefore a test can pass even when actual cancelUserTask() invocation count is 0.
```

Required remediation:

```text
Add the narrowest non-behavioral production seam needed to observe the
cancellation action invocation count.
The seam must sit on the action path used only after exact ownership matching;
it must not count entry into the detach branch or entry into a helper as if
cancellation had occurred.
The default production constructor/path must retain the existing AltoClef
lookup, UserTaskChain availability check, and AltoClef.cancelUserTask() behavior.
A package-private test constructor may inject only the final cancellation action
or an equivalent counter seam.
Do not use diagnostic strings as the proxy for a production side effect.
Do not change detach queue clearing, lifecycle clearing, or ownership matching
semantics in order to make the test pass.
```

The injected counter proves invocation of the dispatcher's production
cancellation-action boundary. In this edit-only phase, the default wiring from
that boundary to the existing `AltoClef.cancelUserTask()` path remains a static
source contract; the test must not claim that AltoClef internals were executed.

Required assertions:

```text
PREEXISTING_UNCHANGED_IDLE_ROOT:
  cancellation action invocation count == 0

COMMAND_OWNED_ROOT plus exact same task instance:
  cancellation action invocation count == 1

COMMAND_OWNED_ROOT plus a different current task instance:
  cancellation action invocation count == 0

OWNERSHIP_UNKNOWN or no matching lifecycle execution:
  cancellation action invocation count == 0
```

### P1 - Not All FabricChatClefCommandResultFactory Paths Have result_fidelity Tests

The implementation plan and previous FAIL addendum require every reachable
result factory path and result reason to have truthful `result_fidelity`
coverage.

Existing coverage is useful for many terminal, exception, deadline, and duplicate
paths, but the follow-up review found missing paths:

```text
nonterminal lifecycle evidence:
  runningLifecycleEvidenceResult(...)
  finish_callback_observed_nonterminal
  stable_request_quiescence_observed

diagnostic factory path:
  diagnosticPayload(String diagnosticReason)
```

Static review concern:

```text
finish_callback_observed_nonterminal and stable_request_quiescence_observed
currently rely on the default fidelityFor() path unless explicitly mapped.
UNKNOWN can be conservative, but the contract requires an intentional mapping
and executable coverage for every reachable reason.
```

Required tests:

```text
finish_callback_observed_nonterminal:
  status == running
  result_reason == finish_callback_observed_nonterminal
  result_fidelity == callback_without_matching_user_task_event

stable_request_quiescence_observed:
  status == running
  result_reason == stable_request_quiescence_observed
  result_fidelity == callback_without_matching_user_task_event

diagnosticPayload(<known diagnostic reason>):
  result_reason is preserved at the documented payload location
  result_fidelity == unknown through an explicit diagnostic-path mapping

diagnosticPayload(<arbitrary unrecognized diagnostic reason>):
  result_reason is preserved at the documented payload location
  result_fidelity == unknown through the same explicit diagnostic-path mapping
```

The two nonterminal lifecycle stages already prove a callback without a
matching user-task event, so they must use the existing
`callback_without_matching_user_task_event` value while remaining
`status=running`. The generic diagnostic path remains `unknown`, but that value
must be selected explicitly by the diagnostic factory path rather than inherited
accidentally from the default switch branch. No new wire field or fidelity enum
value is authorized.

### P2 - Stability Evidence Producers Use Fixed Or Unenforced Values

The initial external review identified the terminal-gate payload problem.
The `reviewed-v2` source inspection additionally found that the nonterminal
running-evidence producer has parallel fixed or unenforced values. The correction
must therefore cover both reachable producers of
`FabricChatClefStableRequestQuiescenceObservation`:

```text
FabricChatClefPreexistingIdleRootStabilityGate
  terminal-eligibility and lifecycle-detail observation

FabricChatClefStableRequestQuiescenceTracker
  nonterminal running command_result evidence published by
  FabricChatClefNonterminalLifecycleEvidencePublisher
```

Current fixed or under-proven values reported by the review:

```text
PreexistingIdleRootStabilityGate:
  snapshotAgeMs = 0L
  sameSessionGeneration = execution != null
  requestRootReappeared = false
  requestRootObservationState = "OBSERVED_NEUTRAL_ROOT_STABLE"

StableRequestQuiescenceTracker:
  snapshotAgeMs = 0L
  sameSessionGeneration = execution != null
  MAX_SNAPSHOT_AGE_MS is reported but is not enforced by the precondition
  stability duration is based on wall-clock milliseconds rather than a monotonic clock
```

Static review concern:

```text
Blocked gate states such as current_root_changed, current_ownership_stale,
assignment_or_generation_changed, or next_task_idle_flag_true can still be
reported with request_root_observation_state=OBSERVED_NEUTRAL_ROOT_STABLE.

The gate qualification duration uses monotonic time, while the gate payload's
stable_duration_ms is calculated from wall-clock milliseconds. The nonterminal
tracker uses wall-clock duration for both qualification and payload and does not
enforce its advertised maximum snapshot age. System time changes or stale
snapshots can therefore make emitted evidence disagree with the intended
contract. Fixing only the terminal gate would leave the running evidence path
incorrect.
```

Required remediation:

```text
producer coverage:
  apply the correction to both FabricChatClefPreexistingIdleRootStabilityGate
  and FabricChatClefStableRequestQuiescenceTracker
  do not leave the nonterminal publisher on a separate false-value path
  for the nonterminal publisher, prefer one coherent FabricChatClefTaskOwnershipEvidence
  capture and derive its current raw root, typed snapshot, ownership snapshot,
  client tick, and monotonic capture time from that same evidence object

snapshot_age_ms:
  compute once from nowNanos and capturedAtNanos evidence
  use that same monotonic value for both the stale-evidence predicate and payload
  do not clamp missing, nonpositive, or future monotonic timestamps to a fresh 0 ms observation
  because FabricChatClefTaskOwnershipSnapshot does not expose capturedAtNanos,
  pass FabricChatClefTaskOwnershipEvidence or the narrowest equivalent typed
  monotonic capture value to the nonterminal tracker; do not add a wire field

stable_duration_ms:
  compute once from monotonic stability-window timestamps in each producer
  use that same value for both qualification and payload

same_session_generation:
  derive from an exact active-execution/active-context identity comparison that
  includes the execution context's session and connection generation
  execution != null alone is not evidence

request_root_reappeared:
  derive from request-owned-root evidence rather than a constant
  for PREEXISTING_UNCHANGED_IDLE_ROOT it may be false by construction only when
  no command-owned bound root existed and the exact preexisting root remained current

request_root_observation_state:
  preserve the existing request-owned-root observation semantics
  the PREEXISTING_UNCHANGED_IDLE_ROOT gate has no command-owned bound root, so
  report NEVER_OBSERVED for that branch unless later evidence proves and binds one
  do not encode qualified, collecting, or blocked gate state in this field
  use qualified and blocked_reason to represent gate state
  never report OBSERVED_NEUTRAL_ROOT_STABLE for the no-bound-root gate branch
  retain the nonterminal tracker's documented request-root states for its own path

threshold and lifecycle guard:
  retain MIN_DISTINCT_CLIENT_TICKS, MIN_STABLE_DURATION_MS, and
  MAX_NEWEST_EVIDENCE_AGE_MS
  limit eligibility effects to the exact evidence-consistency corrections above
  do not otherwise change terminal eligibility, active release, retry, replay,
  or outbox behavior
```

Required tests:

```text
gate: blocked current_root_changed reports blocked_reason=current_root_changed
  and request_root_observation_state=NEVER_OBSERVED
gate: blocked current_ownership_stale reports blocked_reason=current_ownership_stale
  and request_root_observation_state=NEVER_OBSERVED
gate: blocked assignment_or_generation_changed reports the matching blocked_reason
  and request_root_observation_state=NEVER_OBSERVED
gate: blocked next_task_idle_flag_true reports the matching blocked_reason
  and request_root_observation_state=NEVER_OBSERVED
gate: qualified PREEXISTING_UNCHANGED_IDLE_ROOT reports blocked_reason=none,
  request_root_observation_state=NEVER_OBSERVED, and request_root_reappeared=false
both producers: collecting and qualified states are distinguishable through
  qualified and blocked_reason rather than by changing request-root semantics
both producers: snapshot_age_ms is evidence-based rather than fixed at 0
both producers: snapshot_age_ms is the same value used by the stale predicate
both producers: missing, nonpositive, or future capturedAtNanos evidence does not appear fresh
both producers: stable_duration_ms matches the monotonic qualification duration
both producers: same_session_generation is not true merely because execution is non-null
gate: request_root_reappeared=false is backed by the no-bound-root invariant
nonterminal tracker: advertised MAX_SNAPSHOT_AGE_MS is actually enforced
nonterminal publisher path: emitted stable_request_quiescence_observed carries the corrected values
compatibility aliases are equal only when they are both correct

test fixtures must use coherent capturedAtNanos values relative to the supplied
nowNanos; a constant synthetic timestamp must not accidentally turn every later
observation stale after the monotonic correction.
```

### P2 - Post-Edit Provenance And Evidence Report Is Missing

The previous FAIL addendum requires fresh pre-edit provenance before each edit
pass and a post-edit report after remediation. The local context captured while
writing this follow-up document is not a substitute for the next pass's
pre-edit provenance.

Required next-pass pre-edit provenance:

```text
repository root
git toplevel
branch
HEAD
git status --short
planned edit file list
pre-edit SHA-256 for every planned Java, JUnit, or pre-existing document file
ABSENT/NEW marker for the planned new post-edit report instead of an invented hash
```

Required post-edit report contents:

```text
touched files
post-edit HEAD and git status --short
post-edit SHA-256 values for every Java/JUnit file and any pre-existing document modified
confirmation that HEAD did not change because commit/push were not authorized
OWNERSHIP_UNKNOWN outcome state table
TaskFinishedEvent association state table
root-ownership classifier decision table
detach cancellation-action 0/1 invocation table
result_fidelity reason-to-wire-value matrix
stability diagnostic field-semantics table
JUnit production-boundary map
test seam and default-production-wiring explanation, if any
explicit statement of every command not executed
```

The post-edit report must not attempt to embed its own SHA-256 because that is
self-referential. After its bytes are final, report the document's SHA-256 in the
Codex handoff message or in a separate non-self-referential manifest.

The follow-up review did not find a separate report containing the actual
`d5da93b` touched-file hashes, state tables, or test-boundary map. This is a
documentation/evidence-package blocker even after the code blockers are fixed.

This document records the missing report requirement, but it is not the final
post-edit remediation report for the next code pass.

## Additional Tracked Risk

### P2 - Later TaskFinishedEvent Observations Can Overwrite The First Observation

The review identified a lifecycle risk that is not part of the immediate
Approval unit B edit scope unless separately approved:

```text
callback observation is first-observation latched
TaskFinishedEvent observation is overwritten by each markTaskFinishedObservation(...) call
coordinator can drain up to 32 events in one tick
if terminal send fails and active ownership remains, a later event may change
the event evidence used by a retry or later terminal decision
```

An already-created `FabricChatClefCommandTerminalDecision` contains an immutable
payload for that one send attempt. The tracked risk is that, after an immediate
or asynchronous send failure leaves active ownership in place, a later tick can
re-run classification from mutable execution state and construct a different
payload for a later send attempt after another event overwrites the first one.

This should be recorded for a later lifecycle audit. A future pass should
evaluate a first-event latch or terminal-decision snapshot. Do not fold that
wider lifecycle change into the current Approval unit B remediation without
separate defect documentation and explicit approval.

## Required Next Edit-Only Static Remediation

The next proposed work is not build or runtime. It is a narrow
Java/JUnit/document edit-only static-remediation pass, only after separate
explicit user approval.

Proposed scope if separately approved:

```text
0. Capture fresh pre-edit provenance before changing any file.
1. Tighten FabricChatClefRootOwnershipClassifier to require same tick, same
   nonblank thread, exact raw-object identity, and raw/snapshot consistency for
   PREEXISTING_UNCHANGED_IDLE_ROOT.
2. Change across-tick classifier tests to expect OWNERSHIP_UNKNOWN and add the
   missing cross-thread, blank-thread, unavailable, and contradictory cases.
3. Add a narrow non-behavioral dispatcher cancellation-action seam and verify
   actual action invocation count 0/1 for the two detach ownership paths.
4. Add explicit result_fidelity mappings and tests for both nonterminal lifecycle
   evidence reasons and for diagnosticPayload().
5. Correct both stability observation producers and the nonterminal publisher
   path to use single-source, evidence-based monotonic values; add meaning-based
   tests for blocked, collecting, qualified, stale, and emitted-running states.
6. Produce one scoped post-edit evidence report with touched-file hashes, state
   tables, production-boundary map, seam explanation, and non-execution statement.
```

The document edit authorized by that proposed pass is limited to the required
post-edit evidence report, unless a newly discovered contradiction in an
existing contract document is separately recorded and approved. The report's
own final hash is supplied outside the report after its bytes are fixed.

Explicitly not authorized by this document:

```text
Gradle execution
JUnit execution
Java compile
build
JAR copy
Minecraft runtime
Python changes
adris/** changes
Forge/MineMind work
dependency or version changes
public production test hooks or global mutable test state
new result-envelope fields or result_fidelity enum values
stability threshold changes
terminal eligibility changes outside the exact evidence-consistency corrections above
active-release, retry, replay, or outbox behavior changes
broad dispatcher/coordinator/outbox refactor
document edits other than the scoped post-edit evidence report
commit
push
```

## Current Verification Status

```text
Previous OWNERSHIP_UNKNOWN -> COMPLETED P1: PASS, based on static review
Previous unbound TaskFinishedEvent attachment P1: PASS, based on static review
same tick/thread ownership contract: FAIL
actual detach cancellation side-effect test: FAIL
all result factory path fidelity tests: FAIL
all stability observation producer accuracy: FAIL
post-edit provenance/report: FAIL
next-pass pre-edit provenance: NOT YET CAPTURED
ZIP exact identity with d5da93b: UNKNOWN
independent JUnit/Gradle rerun by reviewer: UNKNOWN
Minecraft runtime edge evidence for OWNERSHIP_UNKNOWN/preexisting idle: UNKNOWN
```

## Non-Execution And Non-Mutation Statement

For this documentation pass, Codex did not execute:

```text
Gradle
Java compile
JUnit
build
Minecraft runtime
```

Codex also did not perform:

```text
JAR copy
Python source or configuration mutation
Java source mutation
JUnit source mutation
commit
push
```

Only static repository reads and this documentation-file clarification were performed.
This reviewed-v2 revision grants no implementation, execution, deployment, commit, or push approval.
