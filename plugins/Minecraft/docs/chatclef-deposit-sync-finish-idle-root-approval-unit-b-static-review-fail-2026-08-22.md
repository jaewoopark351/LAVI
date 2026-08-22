<!-- 20260822_kpopmodder: Recorded the external Approval unit B static-review failure and next edit-only remediation contract. -->
<!-- 20260822_reviewed-v2: Clarified the separate approval gate, tightened OWNERSHIP_UNKNOWN fail-closed behavior, and constrained remediation scope. -->

# ChatClef Deposit Sync-Finish Idle Root Approval Unit B Static Review Fail - 2026-08-22

Date: 2026-08-22 KST

Status:

```text
document status: APPROVAL_UNIT_B_STATIC_REVIEW_FAIL_ADDENDUM
document revision: reviewed-v2
source code change in this pass: none
test source change in this pass: none
document change in this pass: this addendum only
Gradle/build/test execution in this pass: none
runtime/JAR/Minecraft execution in this pass: none
implementation approval granted by this document: no
test execution approval granted by this document: no
runtime/JAR deployment approval granted by this document: no
commit/push approval granted by this document: no
next proposed phase before execution: separately approved edit-only static-remediation pass
```

This addendum records the external static-review verdict for Approval unit B
after the edit-only implementation package was reviewed. It does not replace the
2026-08-21 investigation, implementation plan, pre-change report, pre-edit
evidence package, scope clarification, or evidence supplement manifest.

It also does not rewrite the original raw-v2 evidence directory or its manifest.
The finding below is a later static review of an uploaded implementation ZIP,
not a Gradle, JUnit, build, JAR-copy, or Minecraft-runtime result.

## Review Input

External review text attachment:

```text
path: C:\Users\jaewo\.codex\attachments\ffa6ca4f-ec95-4b92-8a6a-4551e6c5c33a\pasted-text.txt
SHA-256: 91043DF4CC86D371D7E4C9524CE40C189AA5023797CFDDB406DA171D3D1CC314
```

Reviewed implementation ZIP reported by that external review:

```text
ZIP: LAVI-minecraft-plugin-fix-alto-clef-infinite-loop (38)(20260821-170623).zip
ZIP SHA-256: C27732A558B883D3B8289ACE78D138F106FD220F0BD0A020DC94620312D77286
target branch reported by review: minecraft-plugin-fix/alto-clef-infinite-loop
review method: static code, test source, and document-contract comparison
not executed by reviewer: Gradle, Java compile, JUnit, build, JAR copy, Minecraft runtime
```

Local repository context reported by Codex as spot-checked before writing this addendum:

```text
repository root: C:\Vtuber_Souorce_Code\LAVI
git toplevel: C:/Vtuber_Souorce_Code/LAVI
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: debeafcfd247d409461fd240b3aa6ba17736c6d7
```

Because the reviewed ZIP does not contain `.git`, this addendum does not claim
that the ZIP bytes, current local HEAD, or any remote branch tip are identical.
Local code references below are static spot-check anchors only.

The local attachment path above is non-portable. The recorded SHA-256 and the
findings reproduced in this addendum are the durable repository-facing record;
the path alone must not be treated as portable evidence.

## Verdict

```text
Approval unit B: FAIL
reason: fail-closed ownership and event-association boundaries are incomplete
execution stage: do not advance to Gradle/JUnit/build/runtime approval
```

The incident-specific happy path is mostly present, but the surrounding
fail-closed boundaries and production-boundary tests are not sufficient for
approval. The next candidate work, only after separate explicit user approval,
must be a narrow edit-only static-remediation pass.

## Blocking Findings

### P1 - OWNERSHIP_UNKNOWN Can Still Become COMPLETED

Required invariant from the implementation plan:

```text
OWNERSHIP_UNKNOWN must not produce COMPLETED
OWNERSHIP_UNKNOWN must not clear active ownership
OWNERSHIP_UNKNOWN may continue waiting or become UNKNOWN only through a separate approved predicate
```

Current static shape:

```text
FabricChatClefRootOwnershipClassifier returns OWNERSHIP_UNKNOWN when ownership evidence is unavailable or contradictory.
FabricChatClefCommandExecutionState stores no boundRootTask for OWNERSHIP_UNKNOWN.
FabricChatClefCommandOutcomeClassifier handles PREEXISTING_UNCHANGED_IDLE_ROOT specially.
All other no-bound-root states can reach classifyCommandWithoutUserTask().
classifyCommandWithoutUserTask() can call completedWithoutUserTask().
completedWithoutUserTask() emits status=completed and ok=true.
Successful terminal send can release active ownership.
```

Local spot-check anchors:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandOutcomeClassifier.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandExecutionState.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefRootOwnershipClassifier.java
```

Required remediation:

```text
PREEXISTING_UNCHANGED_IDLE_ROOT:
  keep the existing incident UNKNOWN path

NO_ROOT_VISIBLE:
  keep the existing no-user-task behavior for this patch unless separately audited

OWNERSHIP_UNKNOWN:
  must not become COMPLETED
  must not clear active ownership
  must remain nonterminal and waiting under an explicit reason in this pass
  must not introduce a new UNKNOWN terminal predicate without separate approval

COMMAND_OWNED_ROOT:
  if the bound root is unexpectedly absent, do not route to no-user-task completion
  return a nonterminal fail-closed waiting decision under an explicit reason
```

`classifyCommandWithoutUserTask()` must be reachable only from the explicitly
approved `NO_ROOT_VISIBLE` path in this incident patch.

### P1 - OWNERSHIP_UNKNOWN Can Attach An Unowned TaskFinishedEvent

TaskFinishedEvent does not carry the bridge request id, session id, connection
generation, or correlation id. Therefore event timing alone is not ownership
proof when root ownership is unknown.

Current static shape:

```text
FabricChatClefCommandLifecycleCoordinator.observeTaskTermination()
  treats PREEXISTING_UNCHANGED_IDLE_ROOT with no bound root as audit-only
  otherwise calls execution.markTaskFinishedObservation(observation)

OWNERSHIP_UNKNOWN with no bound root can therefore attach an unrelated event.
Later callback classification can terminalize from a task identity mismatch.
Successful terminal send can release active ownership.
```

Local spot-check anchors:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandLifecycleCoordinator.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/execution/FabricChatClefCommandExecutionState.java
```

Required event-association table:

```text
PREEXISTING_UNCHANGED_IDLE_ROOT:
  unbound TaskFinishedEvent is audit-only
  reset stability as currently intended
  do not attach to execution

OWNERSHIP_UNKNOWN:
  unbound TaskFinishedEvent is audit-only diagnostic evidence
  do not attach to execution
  do not terminalize from the event alone
  do not release active ownership from the event alone

COMMAND_OWNED_ROOT:
  preserve the existing command-owned matching and mismatch semantics
  do not redefine nonmatching-event behavior in this remediation without separate review

NO_ROOT_VISIBLE:
  keep existing behavior unchanged in this incident patch unless separately approved
```

The required patch is a narrow association gate before
`markTaskFinishedObservation()` for the classifications proven unsafe by this
review. Preserving `NO_ROOT_VISIBLE` and existing `COMMAND_OWNED_ROOT` mismatch
behavior is a scope constraint, not a correctness approval. Do not add a broad
retired-root ledger for this Approval unit B remediation.

### P1 - Tests Do Not Yet Exercise The Required Production Boundaries

The current test package has useful helper-level coverage, but the external
review found that two important tests do not call the production boundary named
by their purpose:

```text
FabricChatClefTaskFinishedEventAssociationTest:
  constructs FabricChatClefCommandExecution directly
  does not publish or dequeue a TaskFinishedEvent
  does not call FabricChatClefCommandLifecycleCoordinator.onEndClientTick()
  does not prove the audit-only branch or stability reset

FabricChatClefBoundRootDetachOwnershipTest:
  checks FabricChatClefCommandExecution matching directly
  does not call FabricChatClefCommandDispatcher.handleConnectionDetached()
  does not prove cancelUserTaskForDetachedCommand() is skipped or preserved
```

Required test matrix before Approval unit B can pass:

```text
OWNERSHIP_UNKNOWN plus synchronous callback -> nonterminal, not COMPLETED
OWNERSHIP_UNKNOWN plus unbound TaskFinishedEvent -> observation is not attached
preexisting idle plus real observer event -> audit-only plus stability reset
first callback observed AFTER_DISPATCH_RETURN remains AFTER_DISPATCH_RETURN after duplicates; a later duplicate cannot reclassify it as BEFORE_DISPATCH_RETURN
duplicate callback -> no duplicate terminal result
stability reset matrix for root/context/event/assignment/generation/next-idle
detach boundary with preexisting idle -> cancelUserTask is not called
detach boundary with exact command-owned root -> existing cancel behavior is preserved
immediate terminal send failure -> Java active ownership remains uncleared
async terminal send failure -> Java active ownership remains uncleared
every existing and newly reachable FabricChatClefCommandResultFactory path and result reason -> truthful result_fidelity
```

Each test must state the production boundary it exercises. Static source-contract
tests may still be useful, but they must not be reported as proof of the
coordinator, dispatcher, terminal outbox, or async send boundary unless they
actually execute that boundary or a minimal production seam used by that runtime
boundary. Any new testability seam must be narrow, non-behavioral, and documented;
do not broadly refactor production code merely to make the tests easier.

### P2 - Structured Diagnostics Do Not Yet Match The Plan

Existing diagnostics include several useful fields, but the static review found
that the planned compatibility aliases and detach/event details are incomplete.

Current spot-check examples:

```text
stable request quiescence payload includes:
  observation_count
  blocked_reason
  stable_duration_ms

preexisting idle-root details include:
  root_ownership_classification
  finish_callback_first_observation
  finish_callback_duplicate_count
  stable_idle_root_observation
  task_finished_event_association
```

Required additive diagnostics:

```text
stable_idle_root_observation.distinct_tick_count
stable_idle_root_observation.reset_reason
bound_root_ownership_for_detach
detach_cancel_action
unbound event audit details with event identity, event sequence, task class, enqueue tick, and dequeue tick where available
```

Compatibility guidance:

```text
keep observation_count
add distinct_tick_count
keep blocked_reason
add reset_reason
```

The diagnostic work must remain additive and observational. These fields are
log/details diagnostics only; this addendum does not approve expanding
`command_result.data`. The work must not change return values, task selection,
retry, timeout, input state, Baritone state, fallback, cleanup, wire schema status
values, or command lifecycle ordering.

## Proposed Remediation Contract For A Separately Approved Edit-Only Pass

Proposed scope if separately approved:

```text
LAVI-owned Fabric ChatClef Java bridge files needed for the ownership classifier,
event-association gate, additive diagnostics, minimal non-behavioral test seams
when strictly necessary, and scoped JUnit test-source additions

terminal send failure work is proof-oriented test coverage first; production
FabricChatClefCommandResultOutbox behavior must not be rewritten unless a separate
static defect is documented and separately approved
```

Explicitly disallowed without separate approval:

```text
Gradle/test/build execution
JAR copy
Minecraft launch
commit or push
Python changes
adris/** changes
broad retired-root ledger
dependency or version changes; retain the already reviewed JUnit Jupiter 5.10.2 lines unchanged unless separately approved
Forge/MineMind files, placeholders, config keys, GUI tabs, tests, or module ids
```

Required static-remediation deliverables after separate edit approval:

```text
0. pre-edit provenance: repository root, branch, HEAD, git status --short, and SHA-256 for every file to be edited
1. explicit FabricChatClefRootOwnershipClassification branching in FabricChatClefCommandOutcomeClassifier
2. pre-attach ownership association gate in FabricChatClefCommandLifecycleCoordinator.observeTaskTermination()
3. production-boundary JUnit additions or replacements for the matrix above
4. additive log/details diagnostics matching the plan field names without command_result.data expansion
5. post-edit report with touched files, file SHA-256 values, state tables, test-boundary map, and execution-not-run statement
```

## OWNERSHIP_UNKNOWN State Table

```text
Input state:
  root ownership evidence is unavailable, partial, or contradictory

Allowed command outcome in this remediation pass:
  nonterminal waiting under an explicit reason

Potential future terminal outcome:
  UNKNOWN only through a separately approved predicate; not authorized by this addendum

Disallowed command outcome:
  COMPLETED

Allowed TaskFinishedEvent handling:
  audit-only diagnostic observation outside execution state

Disallowed TaskFinishedEvent association:
  execution.markTaskFinishedObservation(observation)

Allowed active ownership release:
  none inferred from OWNERSHIP_UNKNOWN alone
  independent pre-existing lifecycle retirement predicates remain outside this state rule

Disallowed active ownership release:
  release caused by completedWithoutUserTask() or event-only identity-mismatch terminalization
```

## Event Association State Table

```text
Classification: PREEXISTING_UNCHANGED_IDLE_ROOT
Bound root: absent by design
Unbound event action: audit-only
Execution attachment: no
Stability action: reset
Terminal action from event alone: no

Classification: OWNERSHIP_UNKNOWN
Bound root: absent or untrusted
Unbound event action: audit-only diagnostic evidence
Execution attachment: no
Stability action: do not infer stable ownership from the event
Terminal action from event alone: no

Classification: COMMAND_OWNED_ROOT
Bound root: required
Event action: preserve existing exact-match and nonmatching-event semantics
Execution attachment: preserve the current command-owned path; this addendum does not redefine mismatch behavior
Stability action: not the preexisting-idle path
Terminal action from event alone: only through the existing approved command-owned path

Classification: NO_ROOT_VISIBLE
Bound root: absent
Unbound event action: preserve current no-root-visible behavior unless separately audited
Execution attachment: do not broaden in this incident patch
Stability action: not the preexisting-idle path
Terminal action from event alone: preserve existing behavior only
```

## Relationship To Existing Approval Documents

This addendum supersedes only the prior assumption that Approval unit B was
ready to move toward execution approval. It does not invalidate the 2026-08-21
Approval unit A evidence supplement, implementation plan, or investigation.

The existing approved direction remains:

```text
do not modify adris/** for this incident
do not alter ChatClef/AltoClef engine behavior
do not use DepositCommand text, StoreInAnyContainerTask absence, or Korean command content as production predicates
do not implement Python reconciliation mutation in this patch
do not claim gameplay success from lifecycle terminal status
do not advance to Gradle/JUnit/build/runtime execution until the static blockers are fixed and separately approved
```

## Explicit Approval Gate

This addendum records a FAIL verdict and a proposed remediation scope. It does
not authorize source or test-source edits by itself. Before Codex edits Java or
JUnit files, the user must separately approve the edit-only static-remediation
scope. That approval still must not include Gradle, JUnit execution, build, JAR
copy, Minecraft runtime, commit, or push.

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
commit
push
```

Only static repository reads and this documentation addition were performed.
