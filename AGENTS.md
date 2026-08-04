# AGENTS.md

This file contains project-specific instructions for AI coding agents such as Codex.

The goal of this file is to keep the project stable, prevent accidental dependency/version changes, and make future AI-assisted coding work consistent.

---

## 0. Critical Safety Gate for Codex

These rules override all other instructions in this file.

### Active repository root

For this project, the active repository root is:

```bat
C:\Vtuber_Souorce_Code\LAVI
```

Codex must treat this path as the default and primary write boundary.

Before editing, moving, deleting, generating, or cleaning up files, Codex must verify both the current directory and the Git repository root:

```bat
cd
git rev-parse --show-toplevel
```

If the current directory or Git root is not the intended repository root, Codex must stop and ask the user.

### Minecraft Backend Loader and Engine Complete Separation Rule

This subsection applies to the full Minecraft plugin tree:

```text
plugins/Minecraft/**
```

<!-- 20260801_kpopmodder: Added a hard boundary for separate Minecraft backend loaders and engines. -->

Required companion design document:

[Minecraft Backend Separation](plugins/Minecraft/docs/minecraft-backend-separation.md)

Codex must preserve the following backend ownership model:

```text
LAVI -> Fabric adapter -> Fabric mod -> ChatClef / AltoClef
LAVI -> Forge adapter  -> Forge mod  -> MineMind
```

Fabric ChatClef and Forge MineMind are independent sibling backends. They must not share implementation, build/runtime ownership, lifecycle state, GUI, configuration namespace, diagnostics implementation, transport/session/reconnect implementation, tests, or packaging artifacts.

Shared Minecraft code is limited to backend-neutral protocol contracts, interfaces, DTOs, JSON schema, error codes, and documentation. Do not put WebSocket, HTTP, socket, thread, asyncio, retry, reconnect, session registry, config loader, logger, GUI, Fabric implementation, Forge implementation, ChatClef adapter, or MineMind adapter code in the common layer.

The currently approved implementation scope is Fabric ChatClef only. Do not create Forge/MineMind placeholder directories, classes, enums, config keys, module IDs, GUI tabs, tests, runtime dependencies, or protocol values before a separate explicit approval for that backend.

Do not implement a generic Minecraft Java bridge that chooses Fabric or Forge through loader detection. Do not create a shared Minecraft Java module, shared Gradle project, shared WebSocket server, shared session registry, or shared reconnect manager for Fabric and Forge.

### Minecraft ChatClef Upstream Baseline Override

This subsection is a scoped safety override for the following tree:

```text
Absolute path:
C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\**

Repository-relative path:
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
```

<!-- 20260730_kpopmodder: Linked the canonical ChatClef / Carry On integration direction document. -->

Required companion design document:

[ChatClef / Carry On Integration Direction](plugins/Minecraft/docs/chatclef-carryon-integration-direction.md)

Codex must read and apply this document before proposing or modifying
ChatClef, AltoClef, Baritone interaction, Carry On integration, or related
diagnostic behavior.

Reading the companion document alone does not authorize a later phase, Java
modification, diagnostics patch, behavioral fix, build, commit, or push.
The standing authorization in `ChatClef Fabric 1.20.1 Diagnostics-Only and
Bounded Logging Policy` below separately authorizes the smallest bounded
diagnostics-only source edit when all of that policy's conditions are met.

This subsection has higher priority for that tree than:

* Section 29 refactoring rules
* Section 29.1 folder and package organization rules
* Section 29.2 file and type separation rules
* Section 29.3 inheritance rules
* general cleanup, modernization, style, folderization, or responsibility-splitting rules
* any general rule that would require restructuring existing code merely because the current task touches it

The ChatClef runtime tree is an upstream-derived compatibility baseline. Treat it as stable third-party-derived code with LAVI-specific compatibility patches, not as ordinary LAVI-owned production code that must be retroactively redesigned.

#### Required ownership classification

Before changing a file in this tree, Codex must determine whether the target is:

1. untouched or substantially upstream-derived ChatClef/AltoClef code
2. an existing LAVI-specific compatibility patch
3. a new LAVI-owned integration file

If ownership is uncertain, preserve the existing structure and inspect provenance before editing. Do not classify a file as LAVI-owned merely because it exists inside the LAVI repository.

#### Baseline preservation rules

For this tree:

* Prefer the smallest method-level or diff-hunk-level patch.
* Do not replace a whole file when a local change is sufficient.
* Do not rename, move, repackage, split, merge, or broadly refactor upstream-derived classes during a bug fix.
* Do not redesign Task, TaskRunner, chain, behavior, input, event, or Baritone lifecycle architecture unless the user explicitly requests that exact structural change.
* Do not apply the mandatory two-responsibility split retroactively to existing upstream-derived files.
* Do not create new folders merely to make the upstream tree match general LAVI folder rules.
* Do not introduce a broad abstract base class, inheritance hierarchy, framework, service layer, manager layer, or generic utility solely to make a small compatibility fix look cleaner. A narrow LAVI-owned optional adapter, wrapper, observer, or Task-local helper is preferred when it keeps Carry On-specific policy out of the upstream engine.
* Do not change Minecraft, Fabric, Fabric Loader, Loom, Java, Gradle, ChatClef, AltoClef, Baritone, or Carry On versions unless the user explicitly approves the exact version change.
* Do not add a hard runtime dependency on Carry On. ChatClef must remain loadable when Carry On is absent, disabled, incompatible, or fails to expose the expected API.
* Do not download, regenerate, replace, or remove Gradle wrapper files without explicit user approval.
* Do not treat `gradle/wrapper/gradle-wrapper.jar`, required API JARs, vendor JARs, or runtime JARs as disposable build artifacts.
* Do not add broad `*.jar` ignore rules that can hide required wrapper, API, vendor, or runtime JARs. Verify every ignored JAR path with `git check-ignore -v` before proposing an ignore change.

General refactoring rules still apply to newly created LAVI-owned code when that code is not replacing, restructuring, or forcing changes across the upstream baseline. Any such new structure must remain narrow and optional.

#### ChatClef engine boundary and composition-first design rule

For this scoped tree, and for LAVI-owned Carry On integration code that calls into it, treat ChatClef and AltoClef as a third-party automation engine. Do not absorb the engine into LAVI, fork its lifecycle through inheritance, or redesign it into a new LAVI-specific engine.

This subsection overrides Section 29.3's mandatory inheritance evaluation trigger and any general preference for extracting a common base class when the affected behavior belongs to ChatClef, AltoClef, Baritone interaction, or optional Carry On integration. In this scope, composition, delegation, observation, and a narrow optional boundary are the default. Inheritance is allowed only where the existing upstream framework requires a type to participate in its established Task contract, and even then the new type must not override engine-wide lifecycle ownership.

##### Required architecture view

Use the following ownership model:

```text
ChatClef / AltoClef
    -> preserved third-party automation engine

LAVI Minecraft plugin
    -> external orchestration and command layer

Carry On integration
    -> optional capability layer

Diagnostics
    -> side-effect-free observation layer

Root-cause fix
    -> smallest evidence-backed hunk at the verified failure boundary
```

Do not redesign ChatClef internals around LAVI-specific concepts. Package moves, class renames, upstream class splitting, Task hierarchy rewrites, parallel engine subclasses, and broad lifecycle overrides are unsafe by default and belong in the discard category unless the user explicitly requests that exact structural change.

##### Composition-first gate

Do not create a LAVI-specific subclass of `AltoClef`, `TaskRunner`, `PlayerInteractionFixChain`, or another engine-wide lifecycle owner to split the engine into a parallel implementation.

Do not use inheritance to override or duplicate:

* global Task selection or scheduling
* TaskRunner lifecycle
* chain registration or priority
* generic interaction completion
* global interruption or cancellation
* Baritone input ownership
* Baritone goal or path ownership
* generic right-click behavior
* engine startup, shutdown, or reload behavior

Prefer narrowly scoped LAVI-owned collaborators such as:

* a small adapter
* an optional integration wrapper
* a diagnostic observer
* a task-local helper
* an interface-based state reader
* a side-effect-free reflection bridge
* a Carry On availability and version probe
* a dedicated Carry On parent Task with explicit local ownership

A new Carry On-specific Task may extend the existing upstream Task abstraction only when that inheritance is required to participate in the established Task lifecycle. Such a Task must compose optional capability readers and helpers, own only its local operation state, and must not replace or override engine-wide lifecycle behavior.

Do not deepen existing upstream inheritance merely to reuse a few helpers. Do not introduce an abstract base class for Carry On integration unless direct evidence shows a stable `is-a` contract that cannot be represented safely by composition and the user explicitly approves the hierarchy.

Before proposing inheritance in this scope, report:

```text
Proposed subclass:
Exact base class:
Exact methods to override:
Why composition is insufficient:
Lifecycle owned by the base:
Lifecycle newly owned by the subclass:
Inputs, goals, paths, and cleanup affected:
Generic ChatClef behavior that could regress:
Upstream comparison impact:
User approval required:
```

If these questions cannot be answered with direct code and runtime evidence, reject the inheritance proposal and use composition or diagnostics instead.

##### Layer and dependency direction

Keep dependencies directed toward the preserved engine boundary:

```text
LAVI orchestration
    -> optional Carry On capability boundary
        -> ChatClef / AltoClef public or already-established behavior

Diagnostics observer
    -> reads operation and engine state
    -> does not choose behavior or mutate state
```

Do not make broad upstream engine classes depend on LAVI orchestration, LAVI UI, or LAVI-specific command policy.

Do not place Carry On-specific policy into a generic ChatClef class merely because that class is easy to reach. Place policy in the narrowest LAVI-owned Task, adapter, wrapper, or helper that owns the operation.

A diagnostic observer must not become a hidden recovery controller. Observation, retry policy, timeout policy, success classification, and cleanup ownership are separate responsibilities.

##### Strict Task and resource ownership

Before proposing any non-diagnostic Carry On behavior, identify ownership explicitly:

```text
Operation or correlation ID:
Parent Task:
Child Task:
Task that owns completion:
Task that initiates the click:
Task that owns retry count:
Task that owns timeout or terminal reason:
Task that acquires each input override:
Task that releases each input override:
Task that creates each custom goal or path:
Task that cancels each custom goal or path:
Interruption entry point:
Cleanup entry point:
Fallback owner:
Global state touched:
Evidence for each ownership claim:
```

Apply these rules:

* The Task that acquires an input may release only that input and only for the operation it owns.
* The Task that creates a custom goal or path may cancel only that goal or path.
* The Task that starts a retry sequence owns the retry count, retry reason, retry limit, and terminal result.
* The Task that owns a timeout owns the timeout start, elapsed-time calculation, terminal reason, and cleanup.
* If a parent Task owns completion, do not globally change a child Task's `isFinished()` contract.
* Do not stop the entire TaskRunner because one optional capability attempt failed.
* Do not cancel all Baritone pathing because one operation failed.
* Do not release every right-click, sneak, jump, movement, or attack input as generic cleanup.
* Do not mutate state whose owner cannot be proven.
* Keep operation-specific state local to the owning Task or narrow collaborator. Do not use broad static state to coordinate Carry On attempts.
* Log cleanup start, each owned resource released, each resource intentionally retained, and the final terminal reason.

`InteractWithBlockTask.isFinished()` is a generic interaction completion contract. Carry On state observation must not be implemented by globally changing this method. Prefer a Carry On-specific parent Task or adapter that observes the child interaction and owns the Carry On-specific completion condition.

##### Carry On success and state-observation contract

A click request, click callback, interaction result, elapsed timeout, or lack of exception is not proof of Carry On success.

When a reliable state can be observed, success requires the expected transition:

```text
Pickup success:
NOT_CARRYING -> CARRYING

Placement success:
CARRYING -> NOT_CARRYING
```

Record both the pre-action state and post-action state with the same correlation identifier and relevant game tick.

A state reader should distinguish at least these semantic outcomes without inventing success:

```text
Carry On absent
Carry On available and NOT_CARRYING
Carry On available and CARRYING
Carry On present but incompatible
Carry On state unavailable or unreadable
Carry On observation failed
```

An unavailable, unreadable, incompatible, or failed observation must not be converted into success. It must not trigger blind retry. Preserve observability and fall back to generic ChatClef behavior when the optional capability cannot be used safely.

Do not call a state-changing Carry On method merely to discover whether Carry On is installed or whether the player is carrying something. Detection and state observation must be side-effect-free.

##### Optional Carry On dependency boundary

Carry On must be treated as optional.

Do not:

* place unconditional Carry On class references in a generic class-loading path when absence can cause startup or linkage failure
* assume that a class, method, field, signature, return type, or state name from another Minecraft or Carry On version exists in the installed version
* swallow reflection, linkage, API, or state-read failures and report success
* retry an action when Carry On state cannot be read
* break generic ChatClef behavior when Carry On is absent, disabled, incompatible, or unavailable
* add or change a Carry On dependency version without explicit user approval

Prefer one narrow optional boundary that:

1. detects absence without mutating game state
2. reports the detected version when available
3. validates reflected classes, methods, signatures, and return values before use
4. normalizes observation into explicit capability and carry-state results
5. logs failure with operation context
6. returns an unavailable result rather than fabricated success
7. allows generic ChatClef behavior to continue when Carry On is unavailable

Reflection may be used only inside this narrow optional bridge and only when it avoids a hard class-loading dependency. Reflection is not permission to bypass type checks, ignore version differences, or invoke side-effecting methods during detection.

##### `PlayerInteractionFixChain` global-impact gate

Treat `PlayerInteractionFixChain` as a global interaction boundary, not a Carry On-local hook.

The following changes in that chain are unsafe by default:

* Carry On-specific branches
* forced sneak release
* forced right-click retry
* Carry On-specific cooldown
* Carry On-specific blacklist
* Carry On-specific fallback
* global input cleanup
* generic interaction completion changes

Do not modify this chain for Carry On unless diagnostic evidence proves that the chain is the exact first failing boundary and the proposed hunk cannot be placed in a narrower owner.

Before modifying it, prove and report that the change preserves:

* generic right-click interaction
* container opening
* doors and trapdoors
* beds
* buttons and levers
* block placement
* item use
* Carry On-absent class loading
* unrelated Task interruption and cleanup
* unrelated Baritone input, goal, and path state

A compilation result is not proof of preservation. The applicable interaction scenarios must be reproduced and observed.

##### Diagnostics before behavior

At the current investigation stage, prefer a diagnostic observer or the smallest logging hunk over a behavior fix.

Before proposing a root-cause patch, logs must establish:

* active parent Task and child Task
* Task transition path
* tick of each right-click attempt
* Carry On presence and version
* carry state before the click
* carry state after the click
* input state and input owner
* Baritone pathing state
* custom goal or path owner
* retry owner, attempt number, and retry reason
* stop reason
* interruption, cancellation, and cleanup path
* last successful boundary
* first failing boundary

Diagnostics must not change return values, Task selection, retry count, timeout, cooldown, input state, pathing, goal ownership, fallback, exception handling, or cleanup behavior.

Use this marker at the first LAVI-specific Java diagnostic block added for this investigation:

```java
//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
```

##### Root-cause patch placement order

After evidence proves the failure, evaluate patch placement in this order:

1. side-effect-free diagnostic observer
2. optional Carry On availability or state adapter
3. task-local helper owned by the affected operation
4. dedicated Carry On parent Task with explicit input, retry, completion, and cleanup ownership
5. smallest method-level upstream hunk only when evidence proves the upstream method is the exact failure boundary and no narrower LAVI-owned boundary can correct it

Do not skip directly to an upstream engine modification because it requires fewer files.

If an upstream hunk is necessary, report:

```text
Verified failing boundary:
Why a LAVI-owned adapter or Task cannot contain the fix:
Exact upstream file and method:
Exact minimal hunk:
Behavior preserved outside the hunk:
Generic interactions tested:
Upstream comparison and rollback method:
Remaining risk:
```

##### ChatClef Engine Modification Last-Resort Gate

Any edit to upstream-derived ChatClef or AltoClef source is an engine divergence.

A diagnostics-only engine divergence may be applied under the standing diagnostics-only authorization when the required failure boundary cannot be observed from a LAVI-owned layer. It must remain the smallest method-level or diff-hunk-level observation change and must not change return values, task selection, completion, retry, timeout, input state, Baritone goal or path state, fallback behavior, exception handling, cleanup, or lifecycle ordering. If the observation requires a broad engine change, more than the minimum upstream hunks, or any behavior change, stop and report instead of applying it.

A behavior-changing engine divergence is a last-resort operation.

Before proposing such a change, Codex must complete the `ChatClef Engine Modification Last-Resort Gate` procedure in:

[ChatClef / Carry On Integration Direction](plugins/Minecraft/docs/chatclef-carryon-integration-direction.md)

Codex must prove why the defect cannot be contained in LAVI orchestration, the optional Carry On bridge, a task-local helper, or a LAVI-owned parent Task. Codex must then report the exact proposed files, methods, and hunks and stop for explicit user approval.

The first direct behavior-changing engine patch should normally be limited to:

* one upstream-derived file
* one method
* one minimal hunk

If more than one upstream lifecycle class, more than one behavior-owning method, or a wider engine boundary is required, Codex must stop and report why the failure cannot be isolated more narrowly.

Carry On-specific types, imports, version checks, retry policy, timeout policy, success criteria, and cleanup policy must remain outside generic engine classes.

When a generic extension seam is proven necessary, it must use a generic contract and a behavior-preserving default. Observation hooks must default to no-op. Decision hooks must default to an explicit no-override or unchanged result, never to success. The Carry On implementation must be connected through external composition from a LAVI-owned namespace.

Every approved engine divergence must include the required marker near the exact hunk and must be recorded with its evidence, baseline, regression scope, and rollback procedure:

`//20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.`

Approval for an engine patch does not authorize build, runtime reproduction, commit, or push. Each action requires separate explicit approval.

##### Required design report before implementation

Before implementing a Carry On integration or root-cause fix, report:

```text
Engine behavior being preserved:
LAVI-owned orchestration boundary:
Optional Carry On boundary:
Diagnostic observation boundary:
Owning parent and child Tasks:
Completion owner and predicate:
Input owner and release path:
Retry owner and terminal reason:
Custom goal or path owner and cleanup path:
Carry On absence fallback:
Carry state observation contract:
Why composition is safer than inheritance here:
Exact files proposed for creation or modification:
Existing upstream files intentionally left unchanged:
Evidence still missing:
Tests required before behavior change:
```

If ownership, state transition, or the exact failing boundary remains unproven, stop at diagnostics. Do not implement the behavioral change.

#### Previous failed AI work is reference-only

A previous branch, ZIP, patch, backup, or AI-generated implementation that damaged or destabilized this component must be treated as reference material only.

Do not:

* copy previous files over the restored baseline
* merge or cherry-pick the previous implementation as a whole
* reapply an old patch blindly
* treat an old class or package structure as authoritative
* preserve a workaround merely because significant effort was previously spent on it

Compare previous work against the restored baseline at method and diff-hunk level. Classify each change as:

1. evidence-backed and reusable as-is
2. idea-only and requiring a fresh minimal implementation
3. a hypothesis requiring diagnostic logs
4. unsafe and to be discarded

#### Diagnostic-only first pass

When the root cause is not already proven, the first change in this tree must be diagnostic-only and must not alter runtime behavior.

The diagnostic pass should make the following observable when applicable:

```text
correlationId
gameTick
topLevelTask
childTask
previousTask
nextTask
targetType
targetId
targetPosition
dimension
carryOnLoaded
carryOnVersion
carryStateBefore
carryStateAfter
rightClickState
sneakState
leftClickState
clickResult
attemptCount
elapsedTicks
screenName
equippedItem
baritonePathing
customGoalOwner
stopReason
exceptionType
```

Use boundary and state-change logging rather than unchanged-state polling logs. For every high-frequency game-tick or slot path, the bounded logging controls defined below are mandatory. Logging volume must not be used as a reason to leave a material transition unobservable, and material observability must not be implemented through unbounded output.

Do not add timeout, retry, blacklist, cancellation, input release, path cancellation, or fallback behavior until logs or direct evidence prove the exact failing boundary and the ownership of the state being changed.

#### ChatClef Fabric 1.20.1 Diagnostics-Only and Bounded Logging Policy

<!-- 20260804_kpopmodder: Added a standing diagnostics-only policy that preserves ChatClef behavior while preventing tick-by-slot log explosions. -->

This policy applies to the Fabric ChatClef 1.20.1 runtime tree and to LAVI-owned Fabric ChatClef bridge or diagnostics code that observes commands executed by that runtime. It applies to ChatClef, AltoClef, Baritone interaction boundaries, inventory tracking, container transfer, command lifecycle, and optional Carry On observation. It is not limited to Carry On bugs.

When this policy conflicts with the generic logging guidance in Sections 21 or 21.1, this scoped policy has higher priority for the Fabric ChatClef 1.20.1 investigation.

##### Standing diagnostics-only authorization

When the user asks Codex to investigate, diagnose, trace, or debug a failure, hang, loop, missing terminal event, crash, or unexplained result in this scope, and the exact root cause is not already proven, Codex must classify the next source change as diagnostics-only.

Codex may apply the smallest bounded diagnostics-only logging edit without asking a separate question such as whether logs should be added first. This is standing permission to choose diagnostics-only work over a speculative behavior fix.

This standing authorization:

* applies only to source edits whose sole runtime purpose is observation
* applies to the smallest necessary LAVI-owned diagnostic helper or upstream-derived diagnostic hunk
* does not authorize a behavior-changing fix
* does not authorize a dependency or version change
* does not authorize a wire-protocol change
* does not authorize a build, Minecraft launch, runtime reproduction, commit, or push unless the user's current instruction separately authorizes that action
* does not override an explicit read-only, audit-only, no-edit, or proposal-only instruction
* does not override the strict read-only restoration and salvage protocol
* does not authorize edits outside the active repository boundary

When the required observation boundary exists only inside upstream-derived ChatClef or AltoClef code, Codex may add one minimal method-level or diff-hunk-level diagnostic block without separate behavior-fix approval. If the required observation would span multiple upstream lifecycle owners, restructure an upstream class, or modify behavior, stop and report the wider scope instead.

##### Unknown-cause behavior gate

If the exact last successful boundary, first failing boundary, and triggering state are not proven, behavior changes are prohibited.

A diagnostics-only change may:

* read already available state without mutating it
* calculate a bounded summary for logging
* emit structured logs through the existing logger
* keep diagnostic-only counters, fingerprints, suppression counts, and session budgets that influence only whether a diagnostic event is emitted
* expire or clear diagnostic-only bookkeeping without calling engine cleanup or changing task results

A diagnostics-only change must not change or influence:

```text
return values
Task selection, scheduling, ordering, completion, or ownership
retry count, retry policy, retry timing, or retry reason
timeout creation, timeout duration, timeout result, cooldown, delay, or sleep
input state, input ownership, input acquisition, or input release
Baritone pathing state, goal creation, goal replacement, goal cancellation, or goal ownership
fallback selection, fallback result, or recovery policy
exception catching, suppression, conversion, propagation, or terminal classification
cleanup entry, cleanup ordering, cleanup ownership, or resources released
wire-protocol schema, message fields, ordering, status values, or acknowledgements
container clicks, cursor state, slot transfer, sync-id handling, screen opening, or screen closing
TaskRunner, Task chain, UserTaskChain, SingleTaskChain, or command lifecycle behavior
thread scheduling, synchronization, object lifecycle, or game-tick ordering
```

Do not add a new `try/catch` around observed engine behavior merely to collect a log. Do not catch and suppress a new exception. A diagnostic formatter may protect only its own formatting from malformed optional values; it must not consume an exception thrown by the observed ChatClef, AltoClef, Baritone, input, transport, or container operation.

Do not call a state-changing ChatClef, AltoClef, Baritone, Minecraft, Fabric, Carry On, input, or container API merely to populate a log field.

##### Diagnostics defaults and overlay separation

Use these defaults unless an existing project configuration defines a stricter, quieter policy:

```text
Normal runtime startup: diagnostics OFF
Explicitly enabled investigation: diagnostics BOUNDARY
VERBOSE or equivalent detail mode: explicit user selection only
Overlay or HUD rendering: independent from diagnostics mode
```

`OFF` disables investigation-only detail but does not suppress existing lifecycle, warning, error, crash, or terminal-failure logs that are part of normal operation.

`BOUNDARY` is the default investigation mode. It emits operation boundaries, meaningful state changes, decisions, refusals, sampled progress, exceptions, and terminal reasons. It must not emit unchanged state every tick.

`VERBOSE` may be used only for a bounded reproduction when the user explicitly selects it or the current task explicitly authorizes that detail level. `VERBOSE` is still subject to rate limiting, deduplication, sampling, payload bounds, and the session hard cap.

Overlay and diagnostics are separate controls:

* turning an overlay or HUD on or off must not enable, disable, or change diagnostics
* enabling or disabling diagnostics must not change HUD visibility, Baritone path rendering, goal rendering, or any other visual overlay
* diagnostics state must not be inferred from overlay state
* overlay configuration must not be used as the diagnostics storage location merely because both are developer-facing controls

If the current implementation couples overlay and diagnostics, decoupling them is a separate configuration or behavior task. Do not hide that behavior change inside a diagnostics-only logging hunk. A diagnostics-only patch must not silently change the current startup default or command semantics.

##### Boundary, state-change, and terminal event model

Prefer a small set of reconstructable structured events over many low-value loop messages.

Log the following when applicable:

```text
operation or command accepted
correlation established
root Task started
child Task selected or changed
important decision made
existing retry attempt or retry reason changed
existing fallback activated
relevant inventory, tool, container, input, or pathing state changed
an action was refused and the exact refusal reason is known
interruption or cancellation entered
cleanup entered and completed when already owned by the observed code
terminal success, rejection, cancellation, interruption, existing timeout, or failure
exception type and exact observation boundary
```

A repeated unchanged state is not a state-change event.

Terminal, exception, and diagnostic-cap events must have reserved emission budget so repetitive progress logs cannot consume the entire session budget before the final reason is recorded.

##### Forbidden high-frequency logging patterns

Do not add or retain investigation logging that emits any of the following without a boundary or state change:

```text
game tick x inventory slot
game tick x container slot
game tick x candidate tool
every-tick full inventory snapshot
every-tick full container snapshot
per-slot InventorySubTracker.registerItem logging
per-call StorageHelper slot-read logging
per-node or per-movement Baritone planning logging
per-tick TaskRunner or child-Task dump when the Task identity is unchanged
raw Object.toString output for every observed object
repeated full stack traces for the same exception boundary
```

Do not instrument a low-level slot accessor merely because it is called by the failing path. Prefer the caller boundary that owns the count, decision, transfer request, refusal, or terminal result.

##### Mandatory hot-path controls

A path is hot when it may execute every game tick, once per slot, once per inventory registration, once per path node, once per movement candidate, once per render frame, or repeatedly while a Task remains unchanged.

Every diagnostic event emitted from a hot path must use all of the following controls:

1. **Rate limit**: bound the maximum emission frequency per correlation and event family.
2. **Deduplication**: suppress identical meaningful fingerprints and count suppressed repeats.
3. **Sampling**: emit periodic unchanged-state progress only at a coarse documented interval; state changes and terminal events are not sampled away.
4. **Session hard cap**: cap total diagnostic events for the bounded diagnostic session, with reserved capacity for terminal, exception, suppression-summary, and cap-reached events.

Use an existing stricter project limit when one exists. If no limit exists, use these conservative defaults:

```text
first unique event: emit immediately
unchanged repeat summary: no more than once per 200 game ticks or 10 seconds
per-correlation detailed-event cap: 256 events
total diagnostic-session hard cap: 5000 events
reserved terminal/exception/cap budget: at least 32 events inside the total cap
```

These values are log budgets only. They must never become a Task retry limit, gameplay timeout, cancellation trigger, pathing timeout, container timeout, or command terminal condition.

When events are suppressed, emit a bounded summary containing the event family, meaningful fingerprint, suppressed count, first-observed time or tick, and last-observed time or tick. Do not emit one suppression message per suppressed event.

When the session cap is reached, emit one `DIAGNOSTIC_SESSION_CAP_REACHED` event, continue reserving the terminal budget, and suppress only non-terminal diagnostic detail. Do not stop, cancel, retry, time out, or otherwise alter the observed operation.

##### Fingerprint stability and payload bounds

A deduplication fingerprint must use stable semantic values that explain why two events are meaningfully the same or different.

Preferred fingerprint fields include only applicable values such as:

```text
event family
correlationId or local operation id
root Task class
child Task class
normalized item or block identifier
requested count
current count
target count
delta needed
normalized screen handler type
container sync id
normalized source and target slot identities
normalized pathing phase or goal summary
move result
refusal reason
terminal reason
exception type
exception boundary
```

Do not put these values in the default fingerprint:

```text
game tick or wall-clock timestamp
System.identityHashCode or another identity hash
default or opaque Object.toString output
unordered HashMap.toString, HashSet.toString, or another unordered collection rendering
full NBT
full inventory or full container dump
stack trace
random UUID generated per log attempt
mutable object address or implementation-specific identity
```

`gameTick` and timestamps may be emitted as ordinary fields for ordering and elapsed-time analysis, but they must not make every fingerprint unique.

Normalize and sort maps, sets, item collections, candidate lists, and slot summaries before logging or fingerprinting. Slot summaries must be ordered by stable slot identity or slot index. Include an `omittedCount` when a bounded summary truncates additional entries.

A full stack trace may be emitted for the first occurrence of a unique exception type and boundary in a correlation. Repeated occurrences must use deduplication and a repeat count rather than repeating the full trace. The stack trace is evidence payload, not fingerprint material.

Full NBT is disabled by default. Log only the specific semantic fields required to explain the decision, such as item id, count, damage, max damage, custom-name presence, or another explicitly relevant property. Any temporary full-NBT investigation requires explicit user approval and its own strict payload and session cap.

##### Inventory, item-count, and free-slot suspicion

When inventory count, item availability, or free-slot state may explain a hang or refusal, log at the count/decision boundary rather than inside every slot read.

Capture the smallest applicable state:

```text
requestId
correlationId
rootTask
childTask
requestedItem
requestedCount
currentItemCount
targetItemCount
deltaNeeded
inventoryFreeSlots
sourceItem
sourceItemCount
sourceSlotsSummary
counting boundary or helper
selected branch
refusalReason
terminalReason
```

`sourceSlotsSummary` must contain only relevant matching or decision-driving slots, sorted by slot index and bounded in length. Do not dump every inventory slot every tick. Emit a new snapshot only on operation start, relevant count change, free-slot change, refusal, terminal result, or exception.

When command semantics distinguish requested quantity from final target quantity, log both. Do not assume `requestedCount`, `targetItemCount`, and `deltaNeeded` are interchangeable.

##### Tool selection and block-breakability suspicion

When tool selection or block breakability may explain stalled `DestroyBlockTask` or mining behavior, log one bounded decision event when the target, candidate set, selected tool, or refusal reason changes.

Capture applicable fields:

```text
requestId
correlationId
rootTask
childTask
targetBlockId
targetBlockPosition
targetBlockStateSummary
requiredToolCategory or mining level when already available
breakability decision
candidateToolsSummary
selectedToolSlot
selectedToolItem
selectedToolDamage
selectedToolMaxDamage
selectionOutcome
selectionReason
save-tool decision and reason when already evaluated
fallback considered by existing code
terminalReason
exceptionType
exceptionBoundary
```

`candidateToolsSummary` must be sorted and bounded. It should contain only decision-driving values such as item id, slot, suitability, durability, and skip reason. Do not call an additional mining, pathing, or tool-selection operation solely to fill the log.

##### Furnace and container slot-transfer suspicion

When `SmeltInFurnaceTask`, `MoveItemToSlotTask`, or another container transfer may be stalled, log at screen-entry, transfer-decision, observed relevant-slot state change, refusal, and terminal boundaries.

Capture applicable fields:

```text
requestId
correlationId
rootTask
childTask
requestedItem
requestedCount
currentItemCount
targetItemCount
deltaNeeded
inventoryFreeSlots
sourceItem
sourceItemCount
sourceSlotsSummary
targetSlot
cursorStackSummary
screenHandlerType
containerSyncId
furnaceInputSlot
furnaceFuelSlot
furnaceOutputSlot
moveAction already selected by existing code
moveResult
refusalReason
terminalReason
exceptionType
exceptionBoundary
```

Represent the furnace input, fuel, and output slots as bounded semantic summaries such as item id and count. Do not log full NBT or every unrelated container slot.

A diagnostics-only patch must not click a slot, change the target slot, move the cursor stack, alter `syncId`, reopen or close a screen, retry a transfer, or reinterpret a refusal as success.

##### Pathfinding and long Baritone planning suspicion

When Baritone planning appears long-running, log planning boundaries and sampled progress instead of nodes or movements individually.

Capture applicable fields:

```text
requestId
correlationId
rootTask
childTask
pathing process or phase
baritonePathing state
goal type and bounded semantic summary
goal owner when observable
path or custom-goal owner when observable
planning start tick or time
elapsed ticks or time
movements considered when already exposed
open-set size when already exposed
path-node map size when already exposed
best-path or progress summary when already exposed
state-change reason
existing cancellation or terminal reason
exceptionType
exceptionBoundary
```

Emit immediately on planning start, goal or owner change, pathing-state change, completion, existing cancellation, or exception. Emit unchanged progress only at the coarse sampling interval and under the hard cap.

Do not create, replace, cancel, pause, resume, or restart a Baritone goal or path to obtain diagnostics. Do not add a pathing timeout as part of diagnostics-only work.

##### Command lifecycle terminal-event suspicion

When a command appears complete in Minecraft but LAVI remains active, trace the existing lifecycle boundaries without changing the wire protocol or command state machine.

Capture applicable fields:

```text
requestId
correlationId
command accepted or queued boundary
render-thread or command-dispatch boundary
root Task start
root Task change
child Task change
existing actually-done or completion decision
TaskFinishedEvent publication boundary
user Task chain idle transition
result normalization boundary
result publication boundary
active-command clear boundary
terminal status and terminal reason
exceptionType
exceptionBoundary
```

Reuse the existing `requestId` and `correlationId` when available. If the engine boundary has no protocol identifier, use a local diagnostics-only correlation value without adding or changing a wire field.

Do not change acknowledgement ordering, completion semantics, active-command clearing, terminal classification, TaskRunner state, event publication, or response payload as part of a diagnostics-only patch.

##### Crash and exception suspicion

For a crash or exception, record the first unique exception occurrence with enough operation context to identify the exact boundary.

Capture applicable fields:

```text
requestId
correlationId
rootTask
childTask
lastSuccessfulBoundary
exceptionBoundary
exceptionType
exceptionMessage
relevant inventory, container, input, or pathing summary
whether existing code propagates, retries, falls back, or terminates
terminalReason when reached
```

Emit the full stack trace once per unique exception type and boundary per correlation when the existing logger supports it. Deduplicate repeats and emit a bounded repeat summary.

Do not add a broad catch, suppress propagation, return success, select a fallback, or change cleanup merely to keep the process running. Exception diagnostics must observe the existing behavior.

##### Hot-path log-explosion review

Before adding a log inside a tick loop, slot loop, inventory registration method, Baritone planning loop, Task runner loop, render loop, or polling callback, Codex must document:

```text
Why this exact low-level boundary is required:
Why a higher-level owner boundary is insufficient:
Event fingerprint fields:
Rate limit:
Deduplication rule:
Sampling interval:
Per-correlation cap:
Session hard cap:
Reserved terminal budget:
Suppression summary event:
Payload truncation rule:
```

If any field is missing, the hot-path logging change is incomplete and must not be applied.

Prefer moving the diagnostic event to a higher-level owner boundary rather than logging inside `registerItem`, a generic slot accessor, every `Task.onTick`, or each Baritone node expansion.

##### Gold smelting minimum diagnostic contract

For a long-running command such as `@get gold_ingot 100`, the trace must make the item requirement and furnace transfer decision reconstructable without a full inventory dump.

Use the same `requestId` and `correlationId` across the command, root Task, child Task, transfer decision, exception, and terminal event.

At minimum, log these fields at the applicable start, Task-transition, item-requirement, transfer-decision, refusal, exception, and terminal boundaries:

```text
commandRequestId
correlationId
rootTask
childTask
requestedItem
requestedCount
currentItemCount
targetItemCount
deltaNeeded
inventoryFreeSlots
sourceItem
sourceItemCount
sourceSlotsSummary
targetSlot
screenHandlerType
containerSyncId
furnaceInputSlot
furnaceFuelSlot
furnaceOutputSlot
moveResult
refusalReason
terminalReason
exceptionType
exceptionBoundary
```

For the gold-smelting case, keep requested output and source input explicit, for example `requestedItem=minecraft:gold_ingot` and `sourceItem=minecraft:raw_gold`, when those are the actual values selected by existing code.

A useful bounded decision record should be capable of answering all of these questions without another per-slot trace:

```text
Which command and Task chain owns the operation?
How many gold ingots were requested?
How many currently exist?
What final target did the Task compute?
How many are still needed?
How many free inventory slots exist?
How much raw gold exists and in which relevant slots?
Which furnace or container slot was targeted?
Which screen handler and sync id were active?
What occupied the furnace input, fuel, and output slots?
Did the move occur, or why was it refused?
What terminal reason or exception boundary ended the observed path?
```

Do not emit the complete field set every tick. Emit it on operation start, meaningful count or slot-state change, transfer decision or refusal, terminal result, and exception, subject to deduplication and the hard cap.

##### Upstream minimal-hunk and LAVI-owned helper rule

Upstream-derived ChatClef and AltoClef files must remain in their existing files, packages, inheritance structure, and folder structure. A diagnostics-only edit to an upstream-derived file must be the smallest local hunk required to expose the boundary. Do not rename, move, split, merge, repackage, broadly format, or refactor the file while adding diagnostics.

Do not force an upstream-derived class to satisfy the LAVI-owned two-responsibility, one-class-per-file, folderization, or inheritance rules merely because a diagnostic line is added.

New LAVI-owned diagnostics code remains subject to Sections 29, 29.1, and 29.2:

* a narrow helper with one responsibility may remain a single focused file
* when a new LAVI-owned diagnostics helper owns two or more independent responsibilities, split those responsibilities into focused files and evaluate a meaningful diagnostics package or folder
* keep fingerprinting, rate limiting, snapshot collection, formatting, and emission separate when they have independent state or reasons to change
* do not create a folder for one trivial helper when an existing LAVI-owned diagnostics package already represents the responsibility
* do not use the new LAVI-owned structure as a reason to move or reorganize upstream-derived files
* do not add a broad diagnostics manager or utility dumping ground

Prefer a LAVI-owned observer or helper when it can expose the same boundary without changing upstream code. Prefer a local upstream diagnostic hunk when extracting a helper would require broader method signatures, lifecycle changes, or upstream restructuring.

##### Required diagnostics-only completion report

After applying a diagnostics-only change in this scope, report:

```text
Root-cause status: verified or still unknown
Diagnostics-only classification:
Exact files and methods changed:
Observed boundaries added:
Behavior explicitly preserved:
Hot-path classification:
Rate limit:
Deduplication fingerprint:
Sampling interval:
Per-correlation cap:
Session hard cap:
Reserved terminal budget:
Overlay/diagnostics independence preserved:
Wire protocol unchanged:
TaskRunner, Baritone, input, and container behavior unchanged:
Upstream minimal-hunk confirmation:
LAVI-owned helper responsibilities and folder decision:
Runtime reproduction still required:
```

Do not claim the bug is fixed merely because the diagnostic patch compiles or the log volume remains small.

#### Carry On compatibility guard

Carry On-specific behavior must remain isolated from generic ChatClef and AltoClef interaction behavior.

Do not:

* globally change `InteractWithBlockTask.isFinished()` to make one Carry On scenario complete
* place a Carry On workaround into `PlayerInteractionFixChain` or another global interaction chain without direct evidence that the global chain is the exact root-cause boundary
* globally suppress right-click, left-click, sneak, jump, movement, or Baritone pathing
* stop the entire TaskRunner because one Carry On attempt failed
* cancel a path, goal, input override, or task that the Carry On operation does not own
* treat a click attempt, timeout, or lack of exception as successful pickup or placement
* assume a Carry On API from a newer Minecraft branch matches the installed Minecraft 1.20.1 version

A Carry On operation must use observed state transition as the success condition whenever the installed version exposes a reliable state:

```text
Pickup success:
NOT_CARRYING -> CARRYING

Placement success:
CARRYING -> NOT_CARRYING
```

Retry behavior must be bounded. On retry exhaustion or interruption:

* release only inputs owned by that operation
* cancel only goals or paths created by that operation
* retain unrelated Baritone and TaskRunner state
* record the exact terminal reason
* return control without crashing ChatClef

Carry On absence, API lookup failure, version mismatch, invalid state, unexpected return value, or integration exception must remain observable and must not crash generic ChatClef startup or normal block interaction.

#### Java marker rule for this tree

Do not insert project-history markers retroactively into untouched upstream files.

A new LAVI-specific Java file, class, or important LAVI-specific compatibility block must use Java comment syntax:

```java
//YYYYMMDD_kpopmodder: Reason for this LAVI-specific change.
```

Preserve existing markers. If marked code must be disabled, keep the smallest necessary old block commented with a reason unless the user explicitly approves deletion.

#### Minimum validation for Carry On changes

A Carry On compatibility change is not verified by compilation alone. Validate the smallest applicable set of scenarios:

* ChatClef loads when Carry On is not installed
* normal Carry On pickup completes
* normal Carry On placement completes
* an invalid, protected, unreachable, or non-pickable target does not cause an infinite loop
* normal chest, door, bed, button, lever, block-placement, and generic right-click behavior remains unchanged
* user interruption releases only operation-owned input
* task interruption, death, dimension change, disconnect, and reconnect do not retain stale operation state
* retry exhaustion produces a bounded terminal result
* Baritone continues operating outside the failed Carry On action
* logs show start, state transition, retry, success, cancellation, cleanup, and terminal reason

#### Required report before a behavior change

Before applying a non-diagnostic Carry On or ChatClef behavior change, Codex must report:

```text
Observed symptom:
Existing evidence:
Verified last successful boundary:
Verified failing boundary:
Affected file and method:
Why the proposed change is the smallest root-cause fix:
Generic interactions that could be affected:
Inputs, goals, paths, and state owned by the operation:
Exact files to modify:
Tests to run:
Remaining uncertainty:
```

If the exact failure boundary is still unknown, stop at diagnostics and do not apply the behavioral change.

#### Read-only restoration baseline audit and failed-work salvage protocol

This protocol applies when the user asks for either or both of the following:

* confirmation of a clean restored baseline
* identification of the smallest reusable ideas from a previous failed or destabilizing implementation

When both goals are requested, complete the baseline audit even if the previous-work source is a placeholder or unavailable. In that case, do not fabricate the salvage analysis.

This is an audit and evidence-collection mode, not an implementation mode. It overrides general brevity guidance in Section 32 and any normal workflow step that would proceed from inspection to editing. The report must be detailed enough for another engineer to reproduce every conclusion without relying on an unsupported summary.

##### Strict read-only mode

During this protocol, do not modify repository state or project files.

Do not:

* edit, create, delete, move, rename, overwrite, format, or regenerate files
* extract an archive over the current baseline
* extract an archive into a temporary repository subdirectory or another location merely to simplify comparison
* create temporary source copies, generated reports, caches, test artifacts, or comparison outputs inside the repository
* copy files or diff hunks from previous work into the current baseline
* apply a patch or create a patch for automatic application
* run a build, compiler, Gradle task, test task, application startup, or runtime reproduction
* install, remove, upgrade, downgrade, or resolve dependencies
* download or generate Gradle wrapper files
* run `git commit`, `git push`, `git fetch`, `git pull`, `git merge`, `git rebase`, `git cherry-pick`, `git am`, or `git apply`
* switch, create, delete, reset, rewrite, or force-update branches
* stash, restore, reset, checkout, clean, or otherwise alter the working tree or index

Read-only commands such as `git status`, `git show`, `git diff`, `git ls-files`, `git check-ignore`, `git rev-parse`, `git branch -vv`, and `git ls-remote` are allowed. `git ls-remote` may query the remote without updating local refs; it must not be replaced with `fetch` merely to make remote state easier to inspect.

If a requested fact cannot be established without a prohibited operation, report it as `[확인 불가]` and explain the exact missing evidence. Do not perform the prohibited operation.

##### Mandatory evidence labels

Every material conclusion must use one of these labels:

```text
[근거 있음] Directly supported by command output, file content, line numbers, method body, a local diff, an existing Git object, or another explicitly identified artifact.
[추정] An inference that is plausible but not yet proven by direct evidence.
[확인 불가] The required artifact, command capability, authoritative comparison source, runtime result, or exact path is unavailable.
```

Do not use vague conclusions such as:

```text
문제가 없어 보인다.
대체로 정상이다.
아마 upstream과 같다.
Carry On 코드가 없는 것 같다.
```

Replace them with a specific label, exact evidence, and the file, line, section, method, command, hash, or output that supports the statement.

##### Full command transcript requirement

For every command actually executed, report all of the following:

```text
Command number:
Purpose:
Working directory:
Exact command:
Exit code:
Actual stdout:
Actual stderr:
Expected result:
Actual result:
Expected and actual state comparison:
Evidence classification: [근거 있음] / [추정] / [확인 불가]
Interpretation:
```

Do not report a command that was not actually executed. Do not replace actual output with a paraphrase. Preserve output order and significant whitespace where practical.

Redact only secrets, credentials, tokens, authentication headers, or a credential embedded in a remote URL. When redaction is required, keep the non-sensitive structure and mark the exact field as `[REDACTED_SECRET]`. Do not hide ordinary paths, branch names, hashes, status entries, warnings, or errors merely to shorten the report.

If a command produces no stdout or no stderr, state `(no output)` for that stream. If an exit code cannot be captured by the available shell or tool, state `[확인 불가] exit code was not exposed by the execution environment` rather than inventing one.

##### Required baseline command set

Use the smallest applicable read-only commands needed to establish the baseline. The report must include the exact commands that were actually run and their complete results.

Recommended baseline commands for Windows CMD are:

```bat
cd /d C:\Vtuber_Souorce_Code\LAVI
cd
git rev-parse --show-toplevel
git status --short --branch
git status --porcelain=v1 --untracked-files=all
git branch -vv
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git rev-parse "76026f8^{commit}"
git show -s --decorate=full --format=fuller HEAD
git rev-parse --abbrev-ref --symbolic-full-name "@{upstream}"
git rev-list --left-right --count HEAD..."@{upstream}"
git remote -v
git ls-remote --heads origin refs/heads/minecraft-plugin-fix/alto-clef-infinite-loop refs/heads/p1a-plugin-lifecycle
git diff --name-status
git diff --cached --name-status
```

A command that is unsupported, unavailable, fails because an upstream is not configured, or cannot reach the remote must remain in the report with its actual error. Do not hide a failed command and do not substitute a state-changing command.

For the expected baseline commit `76026f8`, compare the full object ID resolved by:

```bat
git rev-parse HEAD
git rev-parse "76026f8^{commit}"
```

Classify the result precisely:

* `[근거 있음] exact match` only when the two full commit IDs are identical
* `[근거 있음] mismatch` when both resolve and differ
* `[확인 불가]` when either object cannot be resolved

Do not treat an ancestor relationship, similar subject line, abbreviated display, or remote branch name as an exact commit match.

For upstream state, report separately:

* configured upstream ref
* local HEAD commit
* locally recorded upstream commit
* ahead and behind counts when available
* live remote ref returned by `git ls-remote`

Do not claim that all branches are uploaded merely because the current branch is synchronized.
Do not claim that a remote branch is absent when `git ls-remote` failed, timed out, was blocked, or returned an authentication/network error. In that case classify the live remote state as `[확인 불가]` and preserve the exact error.

##### Required working-tree inventory

Report separately:

* modified tracked files
* staged files
* deleted tracked files
* renamed or copied files reported by Git
* untracked files
* ignored files only when directly relevant to Gradle wrapper, required JAR, build input, or the current audit

Preserve exact repository-relative paths. If the working tree is clean, support the claim with the exact `git status` output. Do not modify, stash, restore, or hide any item to obtain a clean result.

##### Required files and line-level evidence

At minimum, read and list these files when they exist:

```text
AGENTS.md
.gitignore
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/.gitignore
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/gradle.properties
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/gradle/wrapper/gradle-wrapper.properties
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/gradlew
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/gradlew.bat
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/settings.gradle
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/settings.gradle.kts
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/build.gradle
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/build.gradle.kts
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/AltoClef.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/chains/PlayerInteractionFixChain.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/InteractWithBlockTask.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/construction/DestroyBlockTask.java
```

Also read every directly relevant caller, parent task, task chain, input owner, goal owner, fallback, and interruption/cleanup path discovered while tracing the behavior.

For every file used as evidence, report:

```text
Repository-relative path:
Absolute path:
File existence:
Relevant line range:
Relevant class, method, field, or section:
Observed behavior:
Why this evidence matters:
Evidence classification:
```

Use current line numbers from the inspected baseline. If a method spans multiple non-contiguous ranges, cite each range. Do not cite only a class name when the conclusion depends on one branch or statement inside a method.

##### Gradle wrapper, Java, Gradle, and JAR audit

Without building or downloading anything, inspect and report:

* Java runtime executable path
* Java runtime version output
* Java compiler executable path
* Java compiler version output
* system Gradle presence and path
* system Gradle version only when the current execution environment can run `gradle --version` without creating caches or other files; otherwise report the path and classify the version as `[확인 불가]`
* `gradlew.bat` existence
* `gradle/wrapper/gradle-wrapper.properties` existence and relevant distribution settings
* `gradle/wrapper/gradle-wrapper.jar` existence
* whether the wrapper JAR is tracked
* whether root or nested `.gitignore` rules would ignore the wrapper JAR
* whether any broad JAR rule may also ignore required API, vendor, or runtime JARs
* exact factors that would prevent `gradlew.bat` from starting
* whether a build would require a download, generated file, dependency resolution, or external tool that is not currently present
* whether each download or file-generation requirement is directly proven, merely possible from configuration, or not verifiable without running a prohibited command

Recommended read-only checks include:

```bat
where java
java -version
where javac
javac -version
where gradle
rem Run gradle --version only when it is confirmed not to create files or caches.
gradle --version
dir /a:-d /b C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\gradle\wrapper
git ls-files --stage -- plugins/Minecraft/runtime/chatclef_fabric_1.20.1/gradle/wrapper/gradle-wrapper.jar
git check-ignore -v --no-index plugins/Minecraft/runtime/chatclef_fabric_1.20.1/gradle/wrapper/gradle-wrapper.jar
```

A `git check-ignore` exit code indicating no matching ignore rule is evidence of no match for that exact path, not proof that all JARs are safe. Evaluate each relevant broad rule and each required JAR path separately.

Do not download `gradle-wrapper.jar`, run `gradle wrapper`, invoke `gradlew.bat`, or start dependency resolution during this audit.

##### Required ChatClef baseline analysis

The report must distinguish direct local evidence from an authoritative upstream comparison.

To claim that the baseline preserves ChatClef `0.18.23` upstream behavior, identify the exact authoritative upstream source used for comparison, its commit or release identity, and the comparison method. Compare at file, method, and diff-hunk level where practical.

If an authoritative ChatClef `0.18.23` source is not locally or otherwise read-only accessible under the current instruction, report:

```text
[확인 불가] Exact preservation against ChatClef 0.18.23 upstream could not be verified because the authoritative upstream source or exact commit was not available for read-only comparison.
```

Do not replace this missing comparison with model memory or a general description of AltoClef.

At minimum, trace and report:

* whether any Carry On-specific class, package, import, field, branch, method call, event hook, mixin, reflection path, or optional integration currently exists
* the exact body and return behavior of `InteractWithBlockTask.isFinished()`
* every right-click attempt and retry site directly relevant to the failure
* which caller or parent Task owns the actual completion condition for each inspected use of `InteractWithBlockTask`
* the construction and call sites of `InteractWithBlockTask`
* the scope of `PlayerInteractionFixChain`, including whether it is global and which interactions it can affect
* every directly relevant Baritone input override call
* every directly relevant custom goal set, replace, cancel, or clear site
* existing fallback behavior
* interruption, cancellation, `onStop`, cleanup, and terminal-state behavior
* which existing chest, door, bed, button, lever, block-placement, generic right-click, movement, pathing, or Task behavior could be affected by a proposed Carry On change

Search broadly enough to establish callers and ownership. Useful read-only search targets include:

```text
InteractWithBlockTask
isFinished(
CLICK_RIGHT
SNEAK
setInputForceState
InputOverrideHandler
setCustomGoal
setGoalAndPath
cancelEverything
onStop
stop(
reset
PlayerInteractionFixChain
CarryOn
carryon
```

A search result count alone is not enough. Open and read each material result used in a conclusion.

##### Previous-work artifact validation gate

Before comparing previous failed work, validate that the user supplied at least one real, accessible artifact:

* an exact ZIP or archive path
* an exact directory path
* an exact Git branch or commit that exists in the current repository or remote query
* an exact patch or diff file path

Treat values such as these as placeholders, not real artifacts:

```text
C:\정확한\경로\broken_work.zip
<branch-name>
<path>
<commit>
TODO
PLACEHOLDER
YOUR_PATH_HERE
```

Also treat a path that does not exist, an unreadable archive, a branch that cannot be resolved, or a branch name that is only described but not provided as unavailable.

When no real artifact is available, state exactly:

```text
이전 작업 자료가 없어 비교 불가
```

Then:

* do not invent previous changes
* do not infer the previous implementation from conversation summaries alone
* do not perform the salvage comparison
* keep required report Sections 2 through 7, but mark each as `[확인 불가] 이전 작업 자료가 없어 비교 불가`
* continue the baseline-only audit when it can be completed read-only

A user-supplied archive outside the repository may be inspected read-only when its exact path is provided. Do not extract it into the current baseline. Prefer archive listing and in-memory or stream-based content comparison. Do not create temporary extracted files during strict read-only mode.

A previous Git branch must be inspected with read-only operations such as `git show`, `git diff`, or `git log`. Do not checkout, merge, rebase, cherry-pick, or fetch it. If the branch is only on a remote that is not already locally available, `git ls-remote` may establish existence, but content comparison remains `[확인 불가]` unless the content is already accessible without a prohibited operation.

For every supplied previous-work source, report this validation record before comparison:

```text
Provided source type:
Literal value received:
Placeholder detected:
Exists or resolves:
Read-only inspection method:
Usable for comparison:
Reason:
Evidence classification:
```

##### Method and diff-hunk salvage analysis

When valid previous-work material exists, compare it against the restored baseline at method and diff-hunk level. Do not classify an entire file as reusable merely because one small hunk is useful.

Assign every material change to exactly one category:

```text
1. 증거가 있어 그대로 재사용 가능한 변경
2. 아이디어만 재사용하고 코드는 다시 작성해야 하는 변경
3. diagnostic log로 먼저 검증해야 하는 가설
4. 기존 동작을 훼손하므로 폐기해야 하는 변경
```

For every changed hunk, use this exact report schema:

```text
파일:
메서드 또는 범위:
분류:
이전 변경의 의도:
실제 변경 내용:
근거가 있는가:
재사용 가능한 최소 hunk:
폐기해야 하는 부분:
폐기 이유:
영향받을 수 있는 기존 동작:
필요한 diagnostic log:
필요한 재현 테스트:
```

`재사용 가능한 최소 hunk` must identify the smallest statements or idea that can be isolated. Do not paste a whole changed file when only one guard, field, log event, or state check is relevant.

A change may be category 1 only when direct evidence supports both its necessity and its safety against the restored baseline. The report must identify the exact source artifact, file, method, and hunk boundaries. Compilation success, absence of an exception, or a previously observed symptom disappearing is not enough by itself.

##### Mandatory separate risky-change list

List risky changes separately from the salvage matrix. The following are risky by default and normally belong in category 3 or 4 unless direct evidence proves a narrower safe case:

* a global completion-condition change in `InteractWithBlockTask.isFinished()`
* inserting Carry On-specific behavior into `PlayerInteractionFixChain` or another global chain
* stopping the entire TaskRunner
* stopping all Baritone pathing
* globally releasing or suppressing all right-click, sneak, jump, movement, or other input
* adding duplicate timeout behavior across multiple classes
* treating a click attempt as successful completion
* retrying without observing Carry On state
* broad decomposition of upstream-derived classes
* moving packages or renaming upstream files
* swallowing an exception and returning success
* adding cooldown, blacklist, fallback, retry, sleep, timeout, or silent recovery without diagnostic evidence
* adding a hard dependency that prevents startup when Carry On is absent

For each risky change, identify the exact previous file, method, hunk, affected global state, and likely existing behavior exposed to regression. Mark regression impact as `[추정]` unless a caller or test directly proves it.

##### Diagnostics-only patch proposal rules

A diagnostics-only patch may be proposed in the report, but it must not be applied, saved as an applicable patch file, or written into the working tree.

For each proposed diagnostic hunk, report:

```text
Target file:
Target class and method:
Insertion boundary:
Why this boundary is currently unobservable:
Why the proposal does not change behavior:
Smallest proposed hunk:
Log level:
Correlation identifier source:
Fields logged:
State-change or rate-limiting rule:
Sensitive data excluded:
Java marker location:
Expected diagnostic result:
```

Use this exact marker for the proposed Java diagnostic block:

```java
//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
```

The marker belongs immediately above the first LAVI-specific diagnostic field, helper, or log block. Do not add it to unrelated upstream lines and do not insert markers throughout untouched upstream code.

The proposed logs should include applicable fields from:

```text
correlationId
gameTick
topLevelTask
childTask
previousTask
nextTask
targetType
targetId
targetPosition
dimension
carryOnLoaded
carryOnVersion
carryStateBefore
carryStateAfter
rightClickState
sneakState
leftClickState
clickResult
attemptCount
elapsedTicks
screenName
equippedItem
baritonePathing
customGoalOwner
stopReason
exceptionType
exceptionMessage
```

High-frequency paths must use state-transition logging, first-attempt and terminal-attempt logging, bounded attempt counters, a per-operation correlation identifier, and/or a documented rate limit. The rate limit must not hide the transition that proves success, retry, interruption, or terminal failure.

A diagnostics-only proposal must not:

* alter return values
* change task completion
* add or change retry behavior
* release input
* cancel goals or paths
* add cooldowns, timeouts, blacklists, sleeps, or fallbacks
* catch and suppress new exceptions
* change task ordering or ownership
* change synchronization, thread scheduling, exception propagation, fallback result, object lifecycle, or cleanup ownership
* call a side-effecting Carry On or Baritone API merely to populate a log field

##### Root-cause patch evidence gate

Do not apply or fully specify a behavioral root-cause patch during the read-only audit. Report only the evidence conditions that must be satisfied before such a patch is allowed.

At minimum, the evidence must prove:

* the exact Task, method, game tick, branch, and state transition where progress stops or repeats
* Carry On installation state and exact installed version
* whether pickup success is observed as `NOT_CARRYING -> CARRYING`
* whether placement success is observed as `CARRYING -> NOT_CARRYING`
* whether each right-click attempt corresponds to an actual Carry On state transition
* the exact retry owner and retry count
* which input overrides were acquired by the failing operation
* which custom goal or path was created by the failing operation
* which operation-owned input and custom goal must be cleaned up on terminal failure
* that unrelated TaskRunner and Baritone global state do not need to be changed
* that generic right-click behavior remains unaffected in the relevant regression scenarios

If any item remains unproven, label it `[추정]` or `[확인 불가]` and keep the root-cause patch blocked.

##### Required final report order

Do not replace the full report with a brief summary. Use these exact top-level sections and this order:

```text
1. 기준선 상태
2. 이전 작업에서 변경된 파일
3. 위험 변경 목록
4. salvage matrix
5. 재사용 가능한 아이디어
6. 그대로 재사용 가능한 최소 hunk
7. 폐기해야 할 코드
8. diagnostics-only patch 제안
9. root-cause patch를 적용하기 위한 증거 조건
10. 예상 영향 범위
11. 테스트 계획
12. 사용자 승인이 필요한 정확한 파일 목록
13. 아직 증명되지 않은 가설
14. 실행하지 않은 작업과 그 이유
```

Section 1 must contain the complete command transcript, expected-versus-actual comparisons, current directory, Git root, branch, HEAD, expected commit comparison, upstream, working tree, untracked files, remote branch results, read-file inventory, line-level findings, ignore analysis, wrapper presence, Java/Gradle status, and build blockers.

Sections 2 through 7 must contain method/diff-hunk evidence when valid previous-work material exists. If no valid material exists, retain the headings and state `[확인 불가] 이전 작업 자료가 없어 비교 불가` under each.

Section 12 must list exact repository-relative and absolute paths that would require approval for any future modification. A diagnostic proposal is not approval to edit those files.

Section 13 must contain only unproven propositions and must label every item `[추정]` or `[확인 불가]`. Do not repeat verified facts there.

Section 14 must enumerate every prohibited or intentionally omitted action, including file edits, archive extraction, build, tests, dependency changes, Git writes, remote updates, patch application, commit, and push, with the reason each was not performed.

End the report with one of the following truthful statements, without weakening or shortening its meaning.

When valid previous-work material was inspected:

```text
읽기 전용 점검만 수행했으며 파일 수정, 삭제, dependency 변경, build, commit, push는 수행하지 않았다. 이전 작업은 참고 자료로만 분석했으며 현재 기준선에 파일을 복사하거나 변경을 적용하지 않았다.
```

When previous-work material was a placeholder, missing, unresolved, unreadable, or otherwise unusable:

```text
읽기 전용 점검만 수행했으며 파일 수정, 삭제, dependency 변경, build, commit, push는 수행하지 않았다. 이전 작업 자료가 없어 비교 분석은 수행하지 않았으며 현재 기준선에 파일을 복사하거나 변경을 적용하지 않았다.
```

Do not claim that previous work was analyzed when no valid previous-work source was available.

### Write permission boundary

By default, Codex may only modify files inside:

```bat
C:\Vtuber_Souorce_Code\LAVI
```

Do not modify files outside this repository unless the user explicitly provides an exact target path and confirms the operation.

The following folders are read-only by default:

```bat
C:\Program Files (x86)\StarCraft II
C:\Users
C:\Users\*\Desktop
C:\Users\*\Documents
C:\Users\*\Downloads
C:\Users\*\Pictures
C:\Users\*\Videos
C:\Windows
C:\Program Files
C:\ProgramData
```

`C:\Program Files (x86)\StarCraft II` may be inspected for integration or debugging, but must not be modified, cleaned up, moved, renamed, or deleted unless the user explicitly requests a specific file-level change.

### Test isolation directory

The default directory for isolated testing, reproduction scripts, exploratory checks, temporary test fixtures, and test-generated artifacts is:

```bat
C:\Vtuber_Souorce_Code\LAVI\test\test_Isolation
```

Repository-relative form:

```text
test\test_Isolation
```

When creating a new ad-hoc test or a temporary test-only file, Codex must use this directory unless:

* the repository already has an established test location for that specific component, or
* the user explicitly provides a different path.

Do not automatically move existing tests into this directory.
Do not place production runtime code, model files, secrets, or permanent user configuration in this directory.
Do not treat this directory as an automatic cleanup target. All deletion and cleanup safety rules still apply.

### Forbidden destructive commands

Never run broad destructive commands.

Forbidden commands include, but are not limited to:

```bat
git clean -fd
git clean -fdx
git clean -xdf
git reset --hard
git checkout -- .
git restore .
git checkout -f
git switch -f
git branch -D
git push --force
git push --force-with-lease
rm -rf
del /s
rmdir /s
rd /s
Remove-Item -Recurse
Remove-Item -Recurse -Force
robocopy /MIR
```

Never run deletion commands against:

```bat
C:\
C:\Users
Desktop
Documents
Downloads
AppData
Program Files
Windows
any parent folder of the active repo
```

### Cleanup policy

Repo cleanup means updating `.gitignore` first.

Do not mass-delete files just because they match patterns such as:

```text
__pycache__/
*.pyc
tmp_pycache/
plugins/**/bin/
plugins/**/obj/
*.pdb
pretrained_models/**/.cache/
pretrained_models/**/hub/
pretrained_models/**/snapshots/
pretrained_models/**/blobs/
```

If cleanup seems necessary, Codex must first print the exact candidate list and stop.

Do not delete, unlink, or remove symlinks, junctions, shortcuts, model caches, build outputs, or DLL files without explicit user confirmation.

### Runtime DLL preservation

Do not delete DLL files required for game integration.

Examples that may be required:

```text
BWAPI.dll
BWAPIClient.dll
BWTA.dll
StormLib.dll
Monster.dll
```

If unsure whether a DLL is required, keep it and ask the user.

### Future game folders

For future Minecraft, Helltaker, StarCraft2, or other game folders, Codex must not invent paths.

If a task requires a new game folder path, Codex must ask:

```text
This task requires a path outside the current approved repository boundary.
Please provide the exact folder path and confirm whether it should be added to AGENTS.md before I modify it:

<target path>
```

### Required behavior before risky actions

Before any delete, move, rename, cleanup, reset, or mass file operation, Codex must do this instead:

1. Print the exact target paths.
2. Explain why the operation is needed.
3. Confirm that every path is inside the active repo.
4. Stop and ask the user for confirmation.

Do not perform broad cleanup automatically.

---


## 1. Project Overview

This repository is a Windows-based Live AI Vtuber Interface project.

Main components:

* Python application
* Gradio UI
* Transformers Whisper / PyTorch based STT
* Local LLM using GGUF / GGML models
* llama-cpp-python CUDA backend
* GPT-SoVITS based TTS
* VTube Studio integration
* Audio playback, interrupt, and queue control logic

Primary goal:

* Keep the project runnable and stable on Windows.
* Prefer small, safe, incremental changes.
* Avoid large rewrites unless explicitly requested by the user.
* Preserve working behavior over theoretical code cleanliness.

---

## 2. Important Project Policy

Do not assume that the latest package version is better.

This project has fragile dependencies involving:

* Python version
* CUDA Toolkit
* Visual Studio C++ Build Tools
* PyTorch
* llama-cpp-python
* Gradio
* GPT-SoVITS
* Transformers Whisper / PyTorch STT
* audio playback libraries
* VTube Studio integration

Do not upgrade or downgrade these dependencies unless the user explicitly asks.

Always follow `README.md` for the current installation and runtime instructions.

---

## 3. Repository Location

Expected local project path:

```bat
C:\Vtuber_Souorce_Code\LAVI
```

Primary entry point:

```bat
python main.py
```

Do not assume Linux paths unless the user explicitly asks for Linux support.

## 3.1 Repository Boundary / Filesystem Safety Rules

The default writable project boundary is the active repository root:

```bat
C:\Vtuber_Souorce_Code\LAVI
```

Codex may edit files inside this repository only.

`C:\Vtuber_Souorce_Code` is a parent workspace folder, not a broad cleanup target.

`C:\Program Files (x86)\StarCraft II` is read-only by default. It may be inspected for debugging or integration, but Codex must not modify, delete, move, rename, or clean up files there unless the user explicitly provides an exact file path and confirms the change.

Do not treat the entire `C:\Program Files`, `C:\Program Files (x86)`, `C:\Users`, Desktop, Documents, Downloads, AppData, Windows, or any parent folder of the repo as editable.

Before editing, deleting, moving, renaming, or generating any file, Codex must verify that the resolved absolute target path is inside the active repository root.

If a requested task requires editing a path outside the active repository root, Codex must stop and ask the user before making any change.

Required message:

```text
This task requires editing a path outside the active repository root.
Please confirm the exact target path and whether it should be added to AGENTS.md before I modify it:

<target path>
```

For future Minecraft, Helltaker, StarCraft2, or other game folders, ask the user for the exact folder path first. Do not invent or add new paths automatically.


---

## 4. Operating System Rules

This project primarily targets Windows.

When giving commands, prefer Windows CMD or PowerShell examples.

CMD examples are preferred when dealing with:

* `vcvars64.bat`
* Visual Studio C++ environment
* CUDA path setup
* llama-cpp-python build/install commands
* venv activation

Use PowerShell only when it is clearly better or when the user asks for it.

---

## 5. Python Environment Rules

Use the existing project virtual environment unless instructed otherwise.

Typical venv path:

```bat
venv\
```

Do not delete or recreate the virtual environment unless explicitly requested.

Do not commit virtual environment files.

Never modify files inside:

```bat
venv\
```

unless the user specifically asks for debugging inside installed packages.

---

## 6. Dependency Rules

Do not casually modify:

```text
requirements.txt
requirements_full.txt
pyproject.toml
setup.py
```

Do not add heavy dependencies unless necessary.

Do not upgrade core packages without explicit approval.

Fragile packages include but are not limited to:

```text
torch
torchvision
torchaudio
llama-cpp-python
gradio
gradio_client
fastapi
starlette
uvicorn
transformers
tokenizers
huggingface_hub
numpy
scipy
librosa
soundfile
onnxruntime
speechbrain
GPT-SoVITS dependencies
```

If a dependency change is required:

1. Explain why.
2. Show the exact package change.
3. Warn about possible breakage.
4. Ask for user confirmation before applying it.

---

## 7. CUDA / llama-cpp-python Rules

CUDA behavior is fragile.

Do not change CUDA-related settings unless explicitly requested.

Do not change these casually:

```bat
CUDA_PATH
CUDA_PATH_V13_2
PATH
CMAKE_ARGS
FORCE_CMAKE
CMAKE_CUDA_ARCHITECTURES
CMAKE_CUDA_FLAGS
CL
CC
CXX
```

Do not reinstall `llama-cpp-python` unless the user explicitly asks.

When CUDA verification is needed, use a small test first.

Example:

```bat
python -c "from llama_cpp import llama_cpp; print(llama_cpp.llama_print_system_info().decode())"
```

A successful CUDA check should show CUDA device information.

---

## 8. Visual Studio C++ Build Tools Rules

This project may require Visual Studio C++ build environment for CUDA-related Python package builds.

Do not assume `cl.exe` is available until `vcvars64.bat` has been called.

Typical command:

```bat
call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
```

If the user has multiple Visual Studio versions installed, do not switch versions without asking.

Prefer the Visual Studio version documented in `README.md`.

---

## 9. Git Rules

Before editing files, verify the current directory, Git root, branch, upstream, and working tree without changing them.

Recommended read-only commands:

```bat
cd
git rev-parse --show-toplevel
git status --short --branch
git branch -vv
git remote -v
```

Do not modify the working tree merely to obtain a clean status.

Do not discard, overwrite, hide, stash, restore, reset, or force-checkout user changes unless the user explicitly approves the exact paths and exact operation.

Do not delete a branch or force-push unless the user explicitly requests the exact branch and understands the remote-history impact.

Do not commit automatically unless explicitly asked.

Do not push automatically unless explicitly asked.

When the user asks for a commit message, provide a clear conventional-style message.

Examples:

```text
docs: update Windows CUDA setup guide
fix: stabilize TTS interrupt handling
refactor: split GPT-SoVITS controller logic
chore: update gitignore for model artifacts
```

---

## 10. Files That Must Not Be Committed

Never commit:

```text
venv/
__pycache__/
*.pyc
*.pyo
*.log
*.tmp
*.bak
*.wav
*.mp3
*.pth
*.ckpt
*.gguf
*.ggml
*.safetensors
*.onnx
*.bin
.cache/
logs/
outputs/
models/
```

Be especially careful with:

* GPT-SoVITS model files
* LLM model files
* Whisper model files
* generated audio files
* temporary debug logs
* local config files containing secrets or tokens

---

## 10.1 Cleanup and Ignore Rules

Cleanup must be handled in this order:

1. Update `.gitignore` first.
2. Check what is already tracked.
3. Print cleanup candidates.
4. Stop and ask the user before removing anything.

Recommended inspection commands:

```bat
git status --short
git ls-files
```

Do not use broad cleanup commands.

Forbidden:

```bat
git clean -fdx
git clean -xdf
rm -rf
del /s
rmdir /s
rd /s
Remove-Item -Recurse
Remove-Item -Recurse -Force
robocopy /MIR
```

If files should no longer be tracked, prefer a precise `git rm --cached` only after user confirmation.

Example safe pattern:

```bat
git rm --cached path\to\specific_file.pyc
```

Do not run recursive delete commands for these patterns:

```text
__pycache__/
*.pyc
*.pyo
tmp_pycache/
plugins/**/bin/
plugins/**/obj/
*.pdb
pretrained_models/**/.cache/
pretrained_models/**/hub/
pretrained_models/**/snapshots/
pretrained_models/**/blobs/
```

For these patterns, add or update `.gitignore` first.

Do not delete runtime DLL files unless the user explicitly confirms the exact file.

Preserve possible runtime DLLs such as:

```text
BWAPI.dll
BWAPIClient.dll
BWTA.dll
StormLib.dll
Monster.dll
```

If unsure whether a file is a build artifact or a runtime dependency, keep it and ask the user.

---

## 11. Code Editing Rules

Always inspect existing code before editing.

Prefer minimal patches.

Do not rewrite a whole file when a small change is enough.

Do not remove working fallback logic unless the user confirms it is no longer needed.

Do not rename classes, files, or config keys unless explicitly requested.

Preserve existing Korean comments when they explain project-specific behavior.

Preserve comments marked with:

```text
#YYYYMMDD_kpopmodder
```

If code marked with `#YYYYMMDD_kpopmodder` must be removed or disabled, do not hard-delete it.

Instead, comment out the old code using the language-appropriate comment syntax, keep the original `#YYYYMMDD_kpopmodder` marker visible, and add a short reason using the same comment style.

Example:

```python
#YYYYMMDD_kpopmodder: Disabled old CUDA path handling because Transformers local LLM no longer uses llama-cpp-python.
# old_code = load_llama_cpp_cuda_path()  #YYYYMMDD_kpopmodder
```

For multi-line removed blocks, comment out the smallest necessary block only.

Do not leave large obsolete sections unless they explain an important compatibility or rollback decision.

When adding a risky or important change, add a comment using the same style.

Example:

```python
#YYYYMMDD_kpopmodder: Keep CUDA path explicit because llama-cpp-python may fail to load CUDA DLLs on Windows.
```

When adding a new class, new module, or new file, include a `#YYYYMMDD_kpopmodder` marker comment near the top of the new code.
This marker is required even if the class or module is otherwise straightforward, because it records project history and AI-agent changes.

Recommended Python examples:

```python
#YYYYMMDD_kpopmodder: Added this module to isolate MemoryRouter preflight behavior.


#YYYYMMDD_kpopmodder: Added this class to keep GPU preflight checks separate from runtime startup.
class GpuPreflightRunner:
    ...
```

For non-Python files, use the equivalent language-appropriate comment syntax while keeping the marker text visible.

Do not spam comments on obvious code outside these required new class/module/file markers.

Use comments only for fragile behavior, compatibility notes, rollback points, or project-specific decisions.

---

## 12. Stability First Rule

This project values runtime stability more than ideal architecture.

Before refactoring, consider:

* Does the current code already work?
* Is this change necessary?
* Could this break audio playback?
* Could this break interrupt behavior?
* Could this break GPT-SoVITS?
* Could this break CUDA loading?
* Could this break Gradio UI state?

If the risk is high, propose the change first instead of directly applying it.

---

## 13. STT / Whisper Rules

STT behavior is fragile.

Be careful with:

* microphone input
* silence detection
* VAD
* interrupt detection
* AI voice feedback filtering
* speaker recognition
* hallucinated text during silence
* `condition_on_previous_text`
* `initial_prompt`
* `no_speech_prob`
* phrase time limits
* cooldown timing

Do not casually change Whisper parameters unless asked.

If the user reports ghost text during silence, check:

* VAD settings
* silence threshold
* `initial_prompt`
* `condition_on_previous_text`
* no-speech filtering
* microphone energy threshold

---

## 14. TTS / GPT-SoVITS Rules

GPT-SoVITS is the main TTS backend.

Be careful with:

* TTS queue
* audio playback locks
* interrupt handling
* sentence splitting
* API server startup
* port 9880
* reference audio
* GPT weight path
* SoVITS weight path
* mouth movement sync
* VTube Studio state reset

Do not re-enable RVC casually.

RVC has historically caused instability, latency, skipped audio, and crashes.

If modifying TTS logic, preserve:

* queue behavior
* interrupt cooldown
* playback cleanup
* mouth close behavior after interruption
* fallback behavior when audio fails

---

## 15. LLM Rules

This project may use local GGUF / GGML LLMs.

Be careful with:

* context length
* GPU layer count
* VRAM usage
* llama-cpp-python backend
* CUDA loading
* prompt formatting
* streaming response behavior
* interruption during generation

Do not increase context length, GPU layers, or model size unless the user explicitly asks.

Large model changes can cause VRAM exhaustion.

---


## 15.1 Memory System Rules

Memory behavior is fragile and can easily pollute LLM answers.

Do not replace the existing keyword-based memory retrieval completely.
Keep the existing keyword trigger logic as a safe fallback path.

When modifying memory retrieval:

* Add a MemoryRouter / MemoryNeedClassifier before memory DB search.
* The router must decide whether stored memory is needed before retrieval.
* The router must not generate user-facing answers.
* The router must return JSON only.
* If the router fails, times out, or returns invalid JSON, recover safely.
* Router failure must not crash the main conversation flow.
* If router parsing fails, fallback to the existing keyword-based memory retrieval.
* If fallback is disabled or also fails, continue without memory instead of crashing.

MemoryRouter output should follow this general shape:

```json
{
  "intent": "search",
  "need_memory": true,
  "reason": "ongoing_project_context",
  "queries": ["LAV Whisper microphone setting"],
  "memory_scope": ["working", "derived", "long_term"],
  "max_items": 5
}
```

Do not treat every Korean word containing "기억" as a search trigger.

Examples:

* "기억나?", "아까", "전에", "그때" may require memory search.
* General programming or command questions should usually avoid memory search.

Prefer this retrieval order for real-time answers:

1. working memory
2. derived_memory.sqlite3
3. long_term_memory.json

Treat raw_events as source material for consolidation, summarization, promotion, or derived memory rebuild.
Do not use raw_events as the primary real-time retrieval store unless explicitly requested.

Before injecting memory into the main LLM prompt:

* Deduplicate results.
* Limit Top-K results.
* Avoid long raw logs.
* Convert relevant memory into short bullet summaries.
* Keep memory context concise.

Example prompt context:

```text
[Relevant Memory]
- The user uses Whisper large-v3 in LAVI.
- AudioDeviceManager and VoiceInput had an input_device_index synchronization issue.
- input_device_index_callback returning None should fallback to the system default input device.
```

Memory router config should be configurable when possible.

Recommended config keys:

```json
{
  "memory_router_enabled": true,
  "memory_router_timeout_sec": 3,
  "memory_router_max_items": 5,
  "memory_router_fallback_to_keyword": true,
  "memory_router_provider": "auto"
}
```

Do not hardcode router behavior if the existing config system can support it.

When adding tests for memory routing, include at least:

* General question returns need_memory=false.
* Previous-context project question returns need_memory=true.
* LAV / Whisper / ScreenVision project-context question returns need_memory=true.
* Invalid JSON falls back safely.
* Timeout or router exception falls back safely.
* need_memory=true with empty queries uses the user input as fallback query.
* max_items is clamped to the configured limit.

---

## 16. Gradio UI Rules

Gradio behavior may break between versions.

Do not upgrade Gradio unless explicitly requested.

When editing UI code:

* Preserve existing event flow.
* Preserve queue behavior unless testing queue issues.
* Avoid changing component names unnecessarily.
* Avoid breaking live textbox behavior.
* Avoid breaking audio device dropdown behavior.
* Avoid breaking refresh/fallback behavior.

If Gradio warnings appear but the app works, do not over-fix them without user approval.

---

## 17. Audio Device Rules

Audio device handling is important for this project.

Be careful with:

* input device selection
* output device selection
* default fallback
* device refresh
* device index changes
* Windows audio device names
* USB audio devices

When modifying audio device code, preserve fallback to default device.

---

## 18. VTube Studio Rules

VTube Studio integration must remain stable.

Be careful with:

* token authentication
* websocket connection
* mouth open / close state
* expression state
* interrupt cleanup
* reconnect behavior

If TTS is interrupted, the mouth should close properly.

Do not change token handling unless explicitly requested.

Do not commit local VTube Studio tokens.

---

## 19. Config Rules

Project configuration files are part of the repository and must be tracked in Git by default.

Configuration files required to install, run, test, reproduce, or maintain the project must be committed and uploaded to the remote Git repository.

This includes, but is not limited to:

```text
config/
plugins/**/config.json
*.config.json
default_config.json
settings.default.json
configuration schemas
configuration migration files
configuration documentation
```

Do not add the entire `config/` directory or broad configuration-file patterns to `.gitignore` merely because settings may differ between computers.

When creating or modifying a configuration file, Codex must:

1. Keep the configuration structure, keys, safe defaults, and documentation in Git.
2. Verify that the configuration file is tracked unless it contains secrets or private credentials.
3. Replace secrets with empty values, safe placeholders, or environment-variable references before staging or committing.
4. Preserve backward compatibility for existing configuration keys and loading behavior when possible.
5. Check `.gitignore`, `git status --short`, and `git ls-files` to ensure required configuration files are not accidentally excluded.
6. Report any required configuration file that is currently ignored or untracked.

Safe tracked values may include:

```json
{
  "memory_router_enabled": true,
  "memory_router_timeout_sec": 3,
  "memory_router_max_items": 5,
  "openai_api_key": "",
  "huggingface_token": "",
  "external_model_path": ""
}
```

Safe placeholders include:

```text
""
null
"YOUR_API_KEY_HERE"
"${OPENAI_API_KEY}"
"${HUGGINGFACE_TOKEN}"
```

The following values must not be committed:

```text
real API keys
access tokens
passwords
private credentials
VTube Studio authentication tokens
personal access tokens
private server credentials
sensitive private network addresses
private user information embedded in paths or values
```

When a configuration file requires secrets or private machine-specific values, use a tracked base configuration and a local override structure where practical:

```text
config/config.json          # tracked project configuration
config/config.schema.json   # tracked configuration schema
config/local_config.json    # ignored only when it contains private values
.env.example                # tracked environment-variable template
.env                        # ignored secret values
```

A configuration file must not be excluded from Git solely because it is a configuration file.

Only a secret-bearing or privacy-sensitive local override may remain untracked. When such an override exists, the repository must still contain a tracked default, template, schema, or example that preserves every required key and explains how to configure the project.

If an existing configuration file mixes shareable settings with secrets, Codex must not ignore the entire configuration system. Instead, Codex must:

1. Keep shareable settings and the complete configuration structure in a tracked file.
2. Move secrets to environment variables or an explicitly ignored local override.
3. Preserve existing runtime behavior and compatibility where practical.
4. Update `.gitignore`, README, and configuration-loading logic when required.
5. Explain which files are tracked and which values remain local.

Do not overwrite user configuration files casually.

When editing configuration defaults:

* Explain the runtime impact.
* Preserve existing configuration keys when possible.
* Maintain backward compatibility.
* Do not replace real user values with defaults during application startup.

Be careful with files under:

```text
config/
plugins/
voices/
```

Do not hardcode private absolute local paths.

Repository-internal paths should use repository-relative paths.

External paths may use absolute paths when required by runtime behavior, but tracked defaults should use safe examples, empty values, or documented placeholders when the real value contains private information.

---

## 19.1 Internal and External Path Rules

Runtime resource paths and configuration paths must follow these rules:

* Files located inside the active LAVI repository must be stored as paths relative to the repository root.
* Files located outside the active LAVI repository must be stored as absolute paths.
* Relative paths must always be resolved from the active repository root, not from the current working directory.
* Do not use `os.getcwd()` or the process launch directory as the base for project resource paths.
* Existing absolute paths must remain readable for backward compatibility.
* When saving or rewriting configuration:

  * Convert a path to a relative path only when its resolved target is inside the active repository root.
  * Preserve the absolute path when its resolved target is outside the active repository root.
* Do not blindly convert every absolute path to a relative path.
* Repository safety boundaries, approved external application locations, and Codex write boundaries must remain absolute paths.

Examples:

```text
# Inside the LAVI repository: store as relative paths
config/config.json
plugins/GPTSoVITS/config.json
voices/LAVI/reference.wav
pretrained_models/whisper-large-v3

# Outside the LAVI repository: store as absolute paths
C:\Program Files (x86)\StarCraft II
D:\AI_Models\external_model.gguf
C:\GPT-SoVITS\GPT_SoVITS\pretrained_models
```

Path loading must support both forms:

```text
relative path -> resolve from the LAVI repository root
absolute path -> use the absolute path directly
```

Path normalization must be centralized in an existing path/config utility when possible. Do not add independent path-resolution implementations to multiple plugins.

---

## 20. Model File Rules

Model files are large and should not be committed.

Do not move, rename, delete, or regenerate model files unless explicitly requested.

Model file extensions include:

```text
.pth
.ckpt
.gguf
.ggml
.safetensors
.onnx
.bin
```

If a model path is broken, suggest a config/path fix first.

---

## 21. Logging Rules

Logs are useful for debugging but should not be committed.

When adding logs:

* Use existing logger utilities if available.
* Avoid printing secrets.
* Prefer clear, structured messages that identify the exact operation, component, branch, state, and result.
* Diagnostic completeness and runtime safety have equal priority during troubleshooting.
* Temporarily expanded `DEBUG` coverage is allowed only when it is bounded by boundary or state-change emission and, for hot paths, rate limiting, deduplication, sampling, payload bounds, and a session hard cap.
* Do not omit a material transition merely because raw per-loop logging would be noisy; aggregate, summarize, or move the event to the owning boundary instead.
* For real-time audio, video, model-token, game-tick, polling, event, and queue loops, log every material state transition, rejection reason, existing retry, existing timeout, cancellation, and terminal result needed to reconstruct the failure without emitting unchanged state continuously.

Unbounded per-tick, per-item, per-slot, per-token, per-node, per-frame, or per-poll logging is prohibited.

Do not remove useful debug logs during active troubleshooting unless the user asks.
Do not reduce or silence investigation logs until the root cause has been verified and permanent observability remains.

---

## 21.1 Diagnostic Logging First Rule

When a feature fails, behaves inconsistently, hangs, returns an unexpected result, or cannot be reproduced reliably, Codex must first determine whether the existing logs are sufficient to identify the exact failing step.

### Absolute No-Guessing Gate

If Codex cannot prove the exact cause using a stack trace, compiler error, failing test, validated reproduction, direct code evidence, or diagnostic logs, the root cause is unknown.

When the root cause is unknown, Codex must:

1. State clearly that the root cause is still unknown.
2. Stop proposing or applying behavior changes based only on likelihood, intuition, code appearance, or a plausible story.
3. Identify every execution boundary, branch, value, state transition, callback, queue operation, external response, retry, fallback, timeout, cancellation, cleanup step, or background-task result that is still unobservable.
4. Add diagnostic logs across those unobservable points.
5. Reproduce the symptom again.
6. Continue adding logs repeatedly until the exact last successful boundary and the exact failing boundary are observable.
7. Apply a fix only after the evidence identifies the failure mechanism.

One small logging patch is not automatically sufficient.
If uncertainty remains after the first reproduction, Codex must add more logs and reproduce again instead of choosing the most likely explanation.

During active investigation, structured and bounded `DEBUG` coverage is preferable to speculation. Repetitive unbounded logs are not acceptable evidence collection.

Expanded diagnostic detail is acceptable only when:

* secrets, credentials, raw microphone audio, private conversations, and sensitive payloads are not logged
* each message identifies its component, operation, branch, state, attempt, or correlation identifier
* unchanged high-frequency events are rate-limited, deduplicated, sampled, and covered by a session hard cap
* material state changes, terminal reasons, and first exceptions retain reserved emission budget
* logging volume and formatting overhead do not create a new resource-exhaustion or timing failure

Do not optimize for artificially clean output while the failing path is still invisible. Make the path observable with bounded boundary events and summaries rather than raw loop dumps.
Do not treat "probably", "likely", "appears to be", or "seems related" as proof.

### Core Rule

If the root cause is not directly proven by an existing stack trace, compiler error, failing test, validated reproduction, or clearly observable code defect, strengthen diagnostic logging before applying a behavioral fix.

Use this order:

```text
reproduce the symptom
    -> inspect existing logs and error output
    -> identify every unobservable execution boundary
    -> add structured diagnostic logs across the affected path
    -> reproduce again
    -> if any material uncertainty remains, add more logs
    -> reproduce again as many times as required
    -> verify the exact last successful step, failing step, and triggering state
    -> apply the smallest root-cause fix
    -> validate the fix using tests and logs
```

A suspected cause is not a verified cause.

Do not skip diagnostic logging merely because one module appears likely to be responsible.
Do not stop after adding only entry and final-error logs when intermediate decisions remain invisible.
Logging-first may be skipped only when existing evidence already identifies the exact failing operation, triggering state, and required correction.
When in doubt, add more logs rather than more hypotheses.

### Mandatory Diagnostic Review Trigger

Codex must review and, when insufficient, strengthen logging when:

* an operation silently fails or returns no result
* the application reports only a generic error
* the user reports that something does not work and the current logs do not show where execution stopped
* a callback, event, command, request, response, queue item, or state transition disappears
* a plugin, bridge, model, game integration, external process, HTTP endpoint, or WebSocket operation does not respond as expected
* a background thread, async task, future, timer, subprocess, or queue worker terminates unexpectedly
* an exception is caught without sufficient operation context or stack information
* the result is incorrect but intermediate decisions cannot be observed
* several modules or boundaries could plausibly be responsible
* the problem is intermittent, timing-dependent, or difficult to reproduce
* retry or fallback behavior may be hiding the original failure

### Required Diagnostic Coverage

Add the smallest set of logs needed to trace the affected operation from entry to completion.

When applicable, diagnostic logs must identify:

```text
operation, action, command, or request name
component, class, module, plugin, or worker name
request or task entry
important validated input metadata
selected branch or state transition
dependency, handler, parser, action, or plugin resolution
external call start and completion
response status and normalized result
queue insertion, dequeue, cancellation, and completion
thread, async task, future, subprocess, or timer start and completion
retry attempt and exact reason
fallback activation and exact reason
exception type, message, and stack trace
cleanup start, completion, and cleanup failure
final success, rejection, cancellation, timeout, or failure result
elapsed time when delay or timeout is relevant
```

The resulting logs must make it possible to answer:

```text
Where did the operation start?
Which steps completed?
Which branch was selected?
What was the last successful boundary?
What value, state, or response caused rejection or failure?
Where did control flow stop?
Was a fallback, retry, timeout, or cancellation path used?
Did cleanup and final state publication complete?
```

### Boundary-First Logging

Prefer logs at ownership and trust boundaries rather than adding random messages throughout unrelated code.

Important boundaries include:

```text
UI -> application service
application service -> domain or action logic
parser -> typed command
registry or factory -> resolved implementation
plugin loader -> initialized plugin instance
Python -> Java, TypeScript, C, or C++ bridge
HTTP or WebSocket request -> normalized response
queue producer -> queue consumer
thread or async task entry -> terminal result
configuration loader -> validated configuration
model request -> normalized model result
game action request -> observed game state
reload preparation -> validated state swap
```

Log both sides of a boundary when the failure could occur during transfer, conversion, dispatch, or response handling.

### Exception and Background-Task Logging

When an exception is caught, the log must normally include:

* the operation being performed
* the component and relevant non-sensitive identifier
* the exception type
* the exception message
* the stack trace
* whether the operation will stop, retry, return a typed failure, or activate a fallback

Do not use messages such as:

```text
Something went wrong.
Operation failed.
Exception occurred.
```

without identifying the failing operation and location.

Do not allow exceptions from callbacks, threads, async tasks, futures, queue workers, subprocess readers, or timers to disappear silently. Their terminal failure must be observable by the owning component.

Do not catch an exception only to log it and then return misleading success.

### State and Result Logging

Log expected versus actual state when that distinction is necessary to diagnose the failure.

Prefer safe summaries such as:

```text
value type
value length
item count
field names
status code
state name
request or action identifier
sanitized path or abbreviated identifier
normalized success or failure category
```

Do not log complete sensitive or excessively large values.

Never log:

```text
API keys
access tokens
passwords
authentication headers
private credentials
VTube Studio tokens
complete private conversations
raw microphone audio
large model prompts or responses unless explicitly sanitized and required
personal information
```

### Logging Level and Volume Rules

Use logging levels deliberately:

```text
DEBUG    detailed execution flow and investigation data
INFO     meaningful lifecycle and successful operation milestones
WARNING  recoverable abnormal state, retry, timeout, or fallback activation
ERROR    operation failure requiring investigation
CRITICAL unrecoverable process-wide or system-wide failure
```

Do not use `ERROR` for a normal optional absence.

Do not use `INFO` for high-frequency loop details.

During active troubleshooting, expanded `DEBUG` coverage is allowed only when it is bounded and is required to expose the failing path.
Real-time audio, video, model-token, game-tick, polling, event, render, slot, and queue loops must not emit unbounded logs. This restriction must not be used as a reason to leave a failure unobservable; use ownership-boundary events and bounded summaries instead.
Use state-change logging, attempt counters, correlation identifiers, rate limiting, deduplication, sampling, aggregation, payload truncation, and a documented session hard cap as appropriate. Hot paths require all applicable controls, not merely one of them.
For the affected test run, log enough detail to reconstruct every relevant transition and terminal result without logging unchanged state continuously.

### Correlation and Traceability

When one operation crosses multiple modules, reuse an existing request, command, action, job, trace, or correlation identifier when available.

The identifier should make one operation traceable through:

```text
request received
command parsed
handler selected
action submitted
external operation executed
result observed
response returned
```

Use the project's existing logging context mechanism when one exists. Do not add a new logging framework or dependency solely for correlation without user approval.

### Speculative Fix Prohibition

Before the failure boundary is verified, do not attempt to solve an unknown problem by:

* selecting a cause because it is the most likely explanation
* presenting a hypothesis as the root cause
* changing behavior before the missing execution path has been instrumented
* stopping diagnostic work after one logging patch while material uncertainty remains
* rewriting the affected component
* changing dependency or runtime versions
* adding broad `try/catch` or `try/except` blocks
* adding arbitrary delays, sleeps, retries, or timeouts
* changing thread, async, queue, or lifecycle behavior
* adding a silent fallback
* suppressing, downgrading, or ignoring the error
* changing several plausible modules at once

These changes may alter the symptom and hide the original defect.

If a temporary guard is required to prevent data loss or a process-wide crash, keep the failure visible, label the guard as temporary, and continue the root-cause investigation.

### Fix Verification Rule

A fix is not verified merely because the visible error disappeared.

After applying the fix, Codex must use the improved logs and the smallest relevant test to confirm:

1. The operation enters the expected execution path.
2. The previously failing boundary now completes successfully.
3. The expected state or result is produced.
4. No retry or fallback is silently hiding the original failure.
5. Cleanup and final state transitions complete.
6. The caller receives a result consistent with the observed runtime outcome.

Keep permanent logs that provide continuing operational value, especially for:

* startup and shutdown
* dependency and plugin initialization
* bridge connection, disconnection, and reconnection
* action rejection reasons
* retry, timeout, and fallback activation
* malformed external responses
* queue or worker termination
* reload failure and retained previous state
* cleanup failure

Temporary investigation-only diagnostics may be removed or downgraded only after the root cause is verified and sufficient permanent observability remains for the same failure class. Unbounded high-volume logging is never an acceptable temporary mode.

### Required Investigation Report

For a failure that requires diagnostic logging, Codex must report:

```text
Observed symptom:
- <what failed or behaved unexpectedly>

Existing logging gap:
- <which execution step or state could not be observed>

Logs added:
- <exact file and diagnostic event>

Reproduction result:
- <last successful step and verified failing step>

Root-cause status:
- <verified or still suspected>

Verified root cause:
- <cause supported by logs, tests, stack trace, or direct evidence>

Fix applied:
- <smallest change addressing the verified cause>

Validation:
- <commands, tests, and relevant log result>

Remaining uncertainty:
- <anything not yet proven>

Unobserved boundaries remaining:
- <every branch, transition, value, callback, queue operation, external response, or terminal state that is still invisible>

Next diagnostic logs required:
- <exact files and events to add before any behavioral fix>
```

If the root cause remains unknown after logging is added, state that it remains unknown, add more diagnostic coverage, and reproduce again.
Do not present a suspected cause as verified.
Do not proceed to a behavioral fix while a material failure boundary remains unobserved.

### Forbidden Patterns

Do not:

* make several speculative fixes before improving observability
* log only at the final outermost exception boundary
* swallow callback, thread, async-task, queue-worker, timer, or subprocess exceptions
* replace a reproducible error with a silent fallback before identifying its cause
* return success after logging a required operation failure
* print secrets or complete sensitive payloads
* rely on console `print` when an existing logger is available
* use vague, unlabeled, or uncorrelated log messages that make a high-volume trace impossible to follow
* leave a material boundary unobservable merely because raw hot-path output would be large; use bounded events, aggregation, and summaries instead
* remove useful diagnostic logs immediately after the first successful run
* claim that the problem is fixed without reproducing and observing the affected path
* stop investigating while the report still contains material unobserved boundaries

The purpose of logging is to make the exact failing boundary observable before code behavior is changed.
Complete boundary observability and runtime safety are both mandatory during an active investigation.
Bounded structured diagnostics are acceptable; unbounded output and unsupported guessing are not.

---

## 22. Testing Rules

After code changes, suggest the smallest relevant test first.

For isolated or ad-hoc testing, use the repository-relative directory:

```bat
test\test_Isolation
```

Resolve it from the active repository root as:

```bat
C:\Vtuber_Souorce_Code\LAVI\test\test_Isolation
```

Testing behavior:

* Put new reproduction scripts, exploratory tests, temporary fixtures, test logs, and test-generated outputs in this directory by default.
* Keep existing established unit or integration tests in their current project-defined locations unless the user explicitly requests migration.
* Do not make production code depend on files under `test\test_Isolation`.
* When practical, configure test output, cache, and temporary paths to stay inside this directory.
* A test isolation directory is not permission for automatic deletion; print exact cleanup candidates and ask for confirmation first.

Basic syntax check:

```bat
python -m py_compile main.py
```

Run application:

```bat
python main.py
```

CUDA backend check:

```bat
python -c "from llama_cpp import llama_cpp; print(llama_cpp.llama_print_system_info().decode())"
```

Dependency consistency check:

```bat
pip check
```

Do not assume the app is fixed just because syntax checks pass.

For audio/TTS/STT changes, runtime testing is required.

---

## 23. README Rules

`README.md` is for human users.

Keep it practical and copy-paste friendly.

When editing README:

* Prefer Windows CMD examples.
* Clearly separate PowerShell and CMD commands.
* Do not include unnecessary advanced theory.
* Include warnings for fragile CUDA / Visual Studio / llama-cpp-python steps.
* Keep Korean and English sections consistent when both exist.
* Do not mention unsupported features as if they are stable.

---

## 24. AGENTS.md Rules

This file is for AI coding agents.

When project rules change, update this file.

Do not remove safety rules unless the user explicitly asks.

If a rule conflicts with the user's latest instruction, follow the user's latest explicit instruction and note the conflict.

---

## 25. Branching Rules

Use branches for risky work.

Recommended branch names:

```text
fix/...
docs/...
refactor/...
test/...
stabilize/...
```

Examples:

```text
docs/update-cuda-readme
fix/tts-interrupt-cleanup
refactor/gpt-sovits-controller
stabilize/audio-device-fallback
```

Do not merge branches unless the user explicitly asks.

---

## 26. Commit Message Rules

Use clear commit messages.

Preferred format:

```text
type: short summary
```

Common types:

```text
fix
docs
refactor
test
chore
build
style
```

Examples:

```text
docs: add AGENTS.md for Codex project rules
fix: prevent TTS queue crash during interrupt
refactor: split Whisper transcription helper
build: document CUDA llama-cpp-python install steps
chore: update gitignore for model files
```

For Korean commit messages, concise Korean is acceptable when the editor and terminal are confirmed to use UTF-8.

To avoid console encoding issues, keep examples in this file ASCII-only.

If Korean text appears broken in Codex, CMD, PowerShell, or logs, do not assume the file is corrupted. Verify UTF-8 decoding first.

Example:

```text
docs: add Codex work rules
```

---

## 27. Push Rules

Do not push automatically.

Before push:

1. Check changed files.
2. Check staged files.
3. Confirm commit message.
4. Confirm branch.
5. Push only when the user asks.

Recommended commands:

```bat
git status
git log --oneline -5
git push
```

VS Code GUI push is acceptable.

---

## 28. VS Code Rules

VS Code GUI is acceptable for:

* reviewing diffs
* staging files
* committing
* pushing
* checking changed files

Before committing with VS Code GUI, verify that unwanted files are not staged.

Especially check for:

```text
venv/
logs/
models/
*.pth
*.ckpt
*.gguf
*.ggml
__pycache__/
```

---

## 29. Refactoring Rules

This section applies to LAVI-owned production code.

It does not override the scoped Minecraft ChatClef upstream baseline rules in Section 0, and it does not require retroactive restructuring of vendored, upstream-derived, generated, or third-party code. When ownership is uncertain, classify the code before applying this section.

For LAVI-owned production code covered by this section, refactoring is mandatory whenever the code touched by the current task violates the responsibility rules below.

Maintaining working behavior remains required, but existing behavior is not a reason to keep mixed responsibilities in the same LAVI-owned class, module, source file, service, manager, controller, adapter, facade, worker, handler, or package.

### Non-Negotiable Two-Responsibility Rule

Use this rule as a mandatory gate for LAVI-owned production code, subject to the higher-priority scoped exceptions in Section 0:

```text
one responsibility        -> the unit may remain as-is
two or more responsibilities -> refactor, separate files, and evaluate folder/package separation immediately
```

If a LAVI-owned production unit has two or more independent responsibilities, Codex must split it during the current task before adding, extending, or fixing behavior in that unit.

This is a mandatory execution gate, not a recommendation, optional cleanup, future improvement, or style preference.

A responsibility is independent when one or more of the following is true:

* it can change for a different reason
* it has a separate lifecycle, state, dependency, or failure mode
* it can be tested independently
* it belongs to a different feature, domain, layer, integration, or ownership boundary
* it performs a distinct stage such as parsing, validation, orchestration, execution, persistence, transport, UI rendering, logging, retry, cleanup, or result conversion
* it could be replaced, reused, disabled, or extended without replacing the other behavior

Common mixed-responsibility examples that require immediate separation include:

```text
parsing + command execution
validation + external transport
UI rendering + application orchestration
state storage + business decisions
plugin loading + plugin lifecycle execution
HTTP/WebSocket transport + game logic
configuration loading + path normalization
queue ownership + worker execution
model invocation + response formatting
logging/diagnostics + behavioral recovery policy
```

Do not use line count as the deciding factor. A small class or file with two responsibilities must still be split.

### Mandatory Refactoring Procedure

Before modifying production code, Codex must:

1. Inspect every directly affected class, module, source file, service, manager, controller, adapter, facade, worker, handler, and package.
2. List the responsibilities currently owned by each affected unit.
3. Count independent responsibilities by reason-to-change, lifecycle, dependency, failure mode, and ownership boundary.
4. If the count is two or more, stop adding behavior to that unit.
5. Define focused replacement units with one primary responsibility each.
6. Place the extracted units in responsibility-oriented, feature-oriented, domain-oriented, or component-oriented folders/packages.
7. Perform the split in the same task before implementing the requested feature or fix.
8. Update imports, exports, package declarations, registrations, dynamic loading paths, configuration references, tests, mocks, build files, and documentation affected by the split.
9. Preserve public APIs, config keys, runtime behavior, result shapes, and compatibility paths unless the user explicitly approves a breaking change.
10. Run the smallest relevant syntax, compile, unit, integration, and runtime-loading checks.

The required order is:

```text
inspect responsibilities
    -> split mixed responsibilities
    -> organize files into clear folders/packages
    -> update references and compatibility paths
    -> implement the requested behavior
    -> validate structure and runtime behavior
```

Do not implement the requested behavior first and postpone the split until later.

### Folderization Is Part of the Refactor

Separating classes or functions into new files is not sufficient when the files still remain in a folder that mixes unrelated responsibilities.

When a responsibility split creates multiple focused units, Codex must also evaluate and, when needed, reorganize the directly affected folder/package structure in the same task.

Required outcome:

```text
one primary responsibility per class/type/file
related responsibilities grouped in one clear component boundary
unrelated responsibilities placed in separate folders/packages
```

Do not leave extracted files beside unrelated code merely to avoid updating imports.
Do not create meaningless folders only to satisfy the wording of this rule.
Every new or reorganized folder must represent a clear responsibility, feature, domain, component, integration, or lifecycle boundary.

The detailed folder rules in section 29.1 remain mandatory.

### No Deferral or Avoidance

The following are not valid reasons to skip or postpone the split:

* the requested change is small
* the current code already works
* only a few lines are being added
* the class or file is not yet large
* creating more files feels inconvenient
* imports or tests need updates
* the user did not explicitly ask for refactoring in the current prompt
* the task is described as a bug fix, hotfix, compatibility fix, logging change, or minor feature
* the existing mixed-responsibility design was created by a previous Codex task
* the split increases class count or file count
* a generic manager, service, helper, utility, facade, or controller could hide the additional responsibility

Do not respond only with a future refactoring recommendation.
Do not add `TODO`, `later`, `follow-up`, or backlog notes as a substitute for performing the required split.
Do not continue expanding a mixed-responsibility unit.

### Safety and Blocker Handling

The responsibility split remains mandatory even when a filesystem safety rule, move/rename confirmation, external dependency, unavailable runtime, or compatibility risk temporarily blocks completion.

When blocked, Codex must:

1. Stop adding behavior to the mixed-responsibility unit.
2. Print the exact current responsibilities.
3. Print the exact proposed files and folder/package structure.
4. Print every exact source and destination path requiring confirmation.
5. Explain the specific blocker.
6. Ask only for the confirmation or missing external condition required by the safety rules.
7. Mark the task as structurally incomplete until the split is completed.

A blocker may delay execution, but it must not be used to justify adding more logic to the mixed-responsibility code.

### Readability-First Separation Rule

* Prefer more small, explicit classes and files over fewer dense classes when that makes ownership easier to understand, test, replace, and extend.
* Do not avoid a split merely because it increases the class or file count.
* Optimize for clear responsibility boundaries first. Performance, allocation count, and class-count efficiency are secondary unless a measured runtime problem exists or the user explicitly asks for optimization.
* Avoid clever consolidation that hides responsibilities behind generic utility classes, broad managers, overloaded facades, configurable god objects, or unrelated helper modules.
* If there is doubt whether two behaviors are independent, prefer separation when they have different reasons to change or different failure modes.

### Required Refactoring Report

Before editing mixed-responsibility code, Codex must report:

```text
Affected unit:
- <exact path and class/type/module>

Current responsibilities:
- <responsibility 1>
- <responsibility 2>
- <additional responsibilities>

Mandatory split:
- <exact new or retained path>: <one responsibility>

Folder/package organization:
- <folder path>: <component boundary>

References to update:
- imports
- exports
- registrations
- dynamic loading paths
- configuration references
- tests and mocks
- build or packaging files
- documentation

Compatibility to preserve:
- public APIs
- config keys
- result shapes
- runtime behavior
- legacy import/loading paths

Validation:
- <commands and runtime checks>
```

After completing the change, Codex must report:

```text
Responsibilities separated:
- <old mixed responsibility> -> <new focused units>

Created folders:
- <exact folder>: <responsibility boundary>

Created files:
- <exact file>: <single responsibility>

Moved files:
- <old path> -> <new path>

Updated references:
- <imports, exports, registrations, config, tests, dynamic loading>

Compatibility preserved:
- <public and legacy paths>

Validation completed:
- <command>: <result>

Remaining runtime validation:
- <manual or integration test, if any>
```

### Preferred Refactoring Style

* Extract one focused class, type, module, or function for each independent responsibility.
* Preserve existing behavior while relocating ownership.
* Preserve public method names and config keys when possible.
* Use compatibility exports, adapters, or staged migration paths when required.
* Extract shared contracts when separation would otherwise create circular dependencies.
* Keep each structural change limited to the directly affected component, but complete the responsibility split inside that component.

Avoid:

* repository-wide uncontrolled rewrites
* speculative abstractions unrelated to an observed responsibility boundary
* dependency injection frameworks added only for style
* combining the responsibility split with unrelated dependency upgrades or feature changes
* replacing stable behavior only to rename or restyle it

## Code Structure / Refactoring Enforcement Rule

When writing or modifying production code, Codex must check both behavior and structure.

Do not only make the code work. Confirm that every affected behavior belongs to the correct class, module, file, folder, package, feature, domain, or component.

Before adding new logic:

* Check whether an existing focused unit already owns the responsibility.
* Do not append a second responsibility to an existing class, module, file, service, manager, controller, adapter, facade, worker, or handler.
* If the target already owns another independent responsibility, perform the mandatory split first.
* Group related code by responsibility, feature, domain, component, integration, or lifecycle.
* Evaluate folder and package placement during every production code change.
* Create or reorganize folders when the current structure does not clearly represent ownership.
* Do not create arbitrary directory depth without a meaningful boundary.
* Prefer clear boundaries between UI, application logic, domain/core logic, game integration, memory, audio, TTS, STT, vision, configuration, infrastructure, transport, persistence, and adapters.
* Preserve existing behavior and compatibility while improving structure.
* Perform the refactor in small, safe, complete steps. “Small” limits scope; it does not permit leaving two responsibilities together.

When refactoring:

* Preserve public method names unless the user explicitly approves a rename.
* Preserve config keys and externally referenced paths unless the user explicitly approves a change.
* Keep backward-compatible import, loading, registration, and fallback paths when practical.
* Avoid unrelated large rewrites.
* Explain why each extracted unit and folder represents a clearer responsibility boundary.

---


## 29.1 Folder and Package Organization Rule

This section applies to LAVI-owned production files and is subordinate to the scoped upstream baseline override in Section 0. Do not move or reorganize the ChatClef runtime baseline merely because a compatibility task touches it.

LAVI-owned production files must be grouped into folders and packages by responsibility, feature, domain, or component boundary.

Folder organization is a continuous code-structure rule. It is not a subordinate task that applies only when classes are split into separate files.

Whenever Codex creates, modifies, splits, moves, or reorganizes production code, it must evaluate whether the affected files are located in the correct folder or package. If the current structure does not clearly represent ownership and responsibility, Codex must include the necessary folder organization in the same structural change, limited to the directly affected component.

### Core Principle

Use this structure principle:

```text
One primary class, type, or responsibility per file.
Related files grouped by responsibility, feature, domain, or component.
```

Files must not be grouped only by file extension, class suffix, or implementation type.

Prefer feature-oriented or component-oriented folders over global type-oriented folders.

Preferred:

```text
audio/
    devices/
        audio_device_manager.py
        input_device_resolver.py
    playback/
        audio_player.py
        playback_controller.py

memory/
    routing/
        memory_router.py
        memory_need_classifier.py
    storage/
        derived_memory_store.py
        long_term_memory_store.py

games/
    starcraft/
        extension/
            starcraft_extension.py
        bridge/
            starcraft_bridge.py
        adapters/
            bwapi_adapter.py
```

Avoid grouping unrelated code into global dumping-ground folders such as:

```text
classes/
managers/
controllers/
services/
helpers/
utils/
common/
misc/
```

A generic folder may be used only when its contents are genuinely shared across multiple features and the folder has one clearly explainable responsibility.

For example:

```text
audio/devices/audio_device_manager.py
memory/routing/memory_router.py
games/starcraft/bridge/starcraft_bridge.py
```

are preferred over:

```text
managers/audio_device_manager.py
services/memory_router.py
adapters/starcraft_bridge.py
```

### Required Folder Evaluation

During every production code change, Codex must check:

1. Which responsibility, feature, domain, or component owns each affected file.
2. Whether an appropriate existing folder or package already exists.
3. Whether the current folder mixes unrelated responsibilities.
4. Whether related files should be grouped into a clearer package boundary.
5. Whether a new folder would clarify ownership or merely add unnecessary depth.
6. Which imports, exports, tests, configuration references, dynamic import paths, build files, and documentation would be affected.
7. Whether the proposed structure would introduce circular dependencies.
8. Whether backward-compatible import or loading paths must be preserved.

Do not leave a file in an unrelated folder merely because moving it was not explicitly requested.

Do not perform unrelated repository-wide folder reorganization during a small feature, bug-fix, or maintenance task.

### When Folder Organization Is Required

Create or reorganize folders when one or more of the following conditions apply:

* Files with different responsibilities are mixed in the same folder.
* A new feature, domain, component, plugin, adapter, or external integration boundary is being introduced.
* Several related files form one independently understandable, testable, replaceable, removable, or extensible component.
* A flat folder has grown enough that related files are difficult to locate or understand.
* The folder name no longer accurately describes all files contained within it.
* UI, application logic, domain logic, infrastructure, and external integration code are mixed without clear boundaries.
* Multiple files share a responsibility or feature relationship that is not represented by the directory structure.
* New code would otherwise be added to an unrelated existing folder.
* A class, function, interface, event, adapter, worker, controller, configuration module, or other production unit is being separated and needs a clearer package boundary.

Folder organization must be performed when it is necessary for maintainability, portability, extensibility, or clear responsibility boundaries.

Do not postpone an obviously necessary folder separation merely because the current task is not explicitly named as a refactoring task.

### When a New Folder Must Not Be Created

Do not create a new folder merely because:

* One new file was added.
* One class was moved into its own file.
* Every individual class could technically have its own directory.
* More directories would make the repository appear organized.
* The folder would contain only one file without representing a meaningful component boundary.
* The new directory would increase navigation depth without clarifying ownership.
* An appropriate existing folder already represents the responsibility.

Folders represent groups of related responsibilities or components, not individual classes.

Avoid:

```text
memory_router/
    memory_router.py

audio_player/
    audio_player.py
```

unless each folder represents a real component with closely related implementation, interface, configuration, resource, or test files.

### Folder Boundary Guidelines

Recommended major responsibility boundaries may include:

```text
application/
core/
interfaces/
events/
plugins/
extensions/
audio/
stt/
tts/
vision/
memory/
games/
config/
ui/
infrastructure/
adapters/
```

These are examples, not mandatory fixed directories.

Inspect and reuse the repository's existing structure before creating a new boundary.

Do not create parallel folders with overlapping meanings, such as:

```text
audio/
audios/
sound/
playback_audio/
```

Choose one clear ownership boundary and place related files beneath it.

A folder must have one concise and explainable responsibility.

### Required Plan Before Folder Creation or Reorganization

Before creating a new production folder or moving existing production files, Codex must print:

```text
Current structure:
- <current path>: <responsibility>

Proposed structure:
- <proposed path>: <responsibility>

Files to create:
- <exact path>

Files to move:
- <current path> -> <new path>

References to update:
- imports
- package exports
- tests
- configuration paths
- dynamic imports
- plugin registration
- build or packaging files
- documentation

Reason:
- <why the proposed folder represents a clearer responsibility, feature, domain, or component boundary>
```

All source and destination paths must resolve inside the active repository root.

Creating an empty folder does not require separate approval, but moving or renaming existing files remains subject to the repository safety rules. Before any move or rename, print the exact source and destination paths and stop for user confirmation unless the user's latest instruction already explicitly approves those exact paths.

### Imports, Exports, and Compatibility

When creating or reorganizing folders, update all affected:

* Python imports
* Python `__init__.py` files
* package public exports
* relative and absolute imports
* dynamic import strings
* plugin and extension registration paths
* mock and patch target paths
* configuration module paths
* test imports
* C and C++ include paths
* Java package declarations
* build and packaging files
* documentation examples

Do not consider folder organization complete while broken or outdated references remain.

For Python packages, create `__init__.py` when required by the existing repository convention.

Use `__init__.py` for package initialization and deliberate public exports. Do not place unrelated runtime logic in it.

When an existing public import path may still be used, preserve compatibility where practical by re-exporting the moved type from the old path.

Example:

```python
#YYYYMMDD_kpopmodder: Compatibility export after moving MemoryRouter.
from memory.routing.memory_router import MemoryRouter

__all__ = ["MemoryRouter"]
```

Do not remove a compatibility path until repository-wide references have been checked and the user has approved its removal.

### Circular Dependency Prevention

Folder organization must not create circular dependencies.

When multiple components require the same contract, extract it into an appropriate independent module.

Shared contracts may include:

* interfaces
* protocols
* abstract base classes
* event types
* callback types
* DTOs
* enums
* shared data structures

Example:

```text
memory/
    interfaces/
        memory_store_interface.py
    routing/
        memory_router.py
    storage/
        derived_memory_store.py
```

Do not hide circular dependencies through duplicated types, unrelated utility modules, or scattered runtime imports when a clear shared contract can be extracted.

### Incremental Scope

Folder organization must be incremental.

Limit each structural change to the directly affected:

* feature
* package
* plugin
* extension
* game integration
* component
* responsibility group

Do not reorganize the entire repository in one uncontrolled change.

Do not combine folder reorganization with unrelated:

* dependency upgrades
* configuration format changes
* public API renames
* feature additions outside the affected component
* large architectural rewrites

A file or class split and the directly required folder organization may be performed together because they belong to the same structural change.

### Validation

After creating or reorganizing folders, Codex must:

1. List all created folders.
2. List all created and moved files.
3. Search for references to old paths.
4. Verify package exports.
5. Check for circular imports.
6. Run syntax or compile checks.
7. Run the smallest relevant tests.
8. Verify plugin, extension, and dynamic import paths.
9. Inspect `git diff`.
10. Report any remaining runtime validation.

Recommended Python checks:

```bat
python -m compileall <changed_package>
python -m pytest <smallest_relevant_test_path>
git diff --check
git status --short
```

Syntax checks alone are not sufficient when the affected code uses dynamic imports, plugin discovery, configuration-based loading, UI registration, or runtime extension loading.

### Forbidden Folderization Patterns

Do not create a folder containing unrelated classes:

```text
classes/
    audio_device_manager.py
    memory_router.py
    starcraft_worker.py
    gradio_controller.py
```

Do not organize the entire repository globally by class suffix:

```text
managers/
controllers/
workers/
services/
adapters/
```

Do not create meaningless dumping grounds:

```text
utils/
helpers/
common/
misc/
temp/
```

Do not create excessive directory depth without a real responsibility boundary:

```text
src/core/modules/components/services/managers/
```

Do not mix unrelated responsibilities inside a feature folder:

```text
games/starcraft/
    starcraft_extension.py
    memory_router.py
    audio_player.py
    gradio_ui_controller.py
```

### Required Completion Report

After folder organization, Codex must report:

```text
Created folders:
- <folder path>: <responsibility>

Created files:
- <file path>: <responsibility>

Moved files:
- <old path> -> <new path>

Updated references:
- imports
- exports
- tests
- configuration
- dynamic loading
- build or packaging files

Compatibility preserved:
- <old import or API path>

Validation completed:
- <command>: <result>

Runtime validation still required:
- <remaining manual or integration test>
```

Folder organization is not complete merely because directories and files were created.

It is complete only after responsibility boundaries, references, backward compatibility, tests, and runtime loading paths have been verified.

---

## 29.2 File and Type Separation Rule

This section applies to LAVI-owned production code and is subordinate to the scoped upstream baseline override in Section 0. Existing vendored or upstream-derived files are not retroactive split targets unless the user explicitly requests that migration.

LAVI-owned production code must follow a one-primary-type-or-responsibility-per-file rule.

The purpose of this rule is to keep classes, types, and modules independently maintainable without changing existing runtime behavior.

### Common Rules

* One source file must contain only one primary class, type, or module responsibility.
* Independently reusable classes or types must be placed in separate files.
* The file name must correspond to the primary class or type name, following the language and existing repository naming convention.
* Do not place multiple independent classes in one file merely to reduce the number of files.
* When adding a new independent class or type, create a new file instead of appending it to an unrelated existing file.
* When splitting a file, update all affected imports, includes, package declarations, namespaces, public exports, build files, and tests.
* If a split introduces a circular dependency, extract the shared interface, data type, callback type, or constant into a separate file.
* Do not create a giant static class, utility class, or miscellaneous helper file to avoid proper separation.
* Preserve existing public APIs, class names, config keys, runtime behavior, and compatibility paths unless the user explicitly approves a change.

### Python

Python production code must follow the one-class-per-file rule.

* A `.py` file may define at most one top-level project class.
* If a module already contains a class, do not add a second top-level class to that module.
* Move each additional class into its own `.py` file and import it where needed.
* Use `PascalCase` for class names and `snake_case.py` for module names.
* Functions, constants, and type aliases that do not belong to a class may remain in a class-free module organized by responsibility.
* A small class must still be separated when it has independent state or responsibility.

Examples:

```text
PluginManager       -> plugin_manager.py
ExtensionRegistry   -> extension_registry.py
MemoryRouter        -> memory_router.py
GameWorker          -> game_worker.py
```

For this rule, Python classes include:

* regular classes
* `@dataclass` classes
* abstract base classes
* protocol classes
* enums declared as classes
* exception classes
* helper classes
* compatibility wrapper classes

When creating a new Python class file, keep the required project-history marker near the top:

```python
#YYYYMMDD_kpopmodder: Added this module to keep one project class per Python file.
```

### C

C does not provide class syntax, so one `.h` / `.c` file pair must represent one primary structure or one cohesive module responsibility.

* One `.h` / `.c` pair must own one primary `struct` or one clearly defined module.
* Declare the primary structure and its public API in the `.h` file.
* Place the corresponding implementation in the matching `.c` file.
* Do not declare multiple independent primary structures in one header.
* Do not collect unrelated global functions in one `.c` file.
* Small `enum` values, constants, callback typedefs, and private helper types directly owned by the primary module may remain with that module.
* Shared `enum` values, constants, typedefs, callback contracts, or data structures used by multiple modules must be placed in separate headers.
* Keep private implementation helpers `static` inside the owning `.c` file when they are not independently reusable.

Examples:

```text
audio_manager.h
audio_manager.c        -> AudioManager structure and related functions

event_bus.h
event_bus.c             -> EventBus structure and related functions
```

When creating a new C header or source file, keep the required project-history marker near the top:

```c
//YYYYMMDD_kpopmodder: Added this module to isolate one C structure or module responsibility.
```

### C++

One `.h` / `.hpp` and `.cpp` file pair must represent one primary class or independently reusable type.

* Give each primary class its own header and implementation file.
* The header and implementation base names must correspond to the primary class or type name.
* Do not declare multiple independent classes in one header.
* Do not implement member functions for multiple independent classes in one `.cpp` file.
* Interfaces, abstract classes, independently reusable `enum class` types, and public helper classes must be placed in separate files.
* A small nested class, private implementation type, or internal helper may remain with the owning class only when it is not referenced independently.
* A PImpl implementation class may remain private inside the owning `.cpp` file.
* Template implementation may remain in the owning header or matching implementation header when required by C++ compilation rules.

Examples:

```text
AudioManager.h
AudioManager.cpp       -> AudioManager

PluginRegistry.h
PluginRegistry.cpp     -> PluginRegistry

EventBus.h
EventBus.cpp           -> EventBus
```

When creating a new C++ header or source file, keep the required project-history marker near the top:

```cpp
//YYYYMMDD_kpopmodder: Added this class file to keep one primary C++ class per file pair.
```

### Java

Java production code must follow the one-top-level-type-per-file rule.

* A `.java` file may contain only one primary top-level project type.
* The file name must exactly match the primary `class`, `interface`, `enum`, `record`, or `@interface` name.
* Do not place multiple independent package-private top-level classes in one file.
* Each independent `class`, `interface`, `enum`, `record`, or annotation type must have its own `.java` file.
* A small nested class may remain inside the owning class only when it is used exclusively by that class and has no independent responsibility.
* Move a nested class into its own file when it grows, is referenced externally, or becomes independently testable or reusable.

Examples:

```text
AudioManager.java      -> AudioManager
PluginRegistry.java    -> PluginRegistry
EventBus.java           -> EventBus
Plugin.java             -> Plugin interface
PluginState.java        -> PluginState enum
```

When creating a new Java type file, keep the required project-history marker near the top:

```java
//YYYYMMDD_kpopmodder: Added this type file to keep one primary Java type per file.
```

### Allowed Exceptions

The following exceptions are allowed only when the additional type has no independent responsibility and is not independently referenced:

* Python `__init__.py` files used only for imports and public exports
* modules containing functions, constants, or type aliases without a class
* short nested classes used only by their owning class
* C++ PImpl implementation types
* C or C++ private helper types used only inside one implementation file
* language-required template implementation kept with its owning C++ template
* lambdas, anonymous classes, and local classes
* small test fixtures or local test doubles tightly coupled to one isolated test
* generated code
* vendored or third-party code that should remain unmodified

Do not use an exception merely because a class or type is small.
Do not use an exception to hide a second independent responsibility in an existing file.

### Refactoring Existing Files

When an existing file contains multiple independent classes or primary types:

1. List every class or primary type and its current file.
2. Propose the exact new file paths before moving code.
3. Split one package, component, or responsibility group at a time.
4. Preserve public names, behavior, configuration, and compatibility paths.
5. Move each independent class or type into its own file or matching header/source pair.
6. Update imports, includes, package declarations, namespaces, exports, build settings, and tests.
7. Check for circular dependencies and extract shared contracts when necessary.
8. Run syntax or compile checks and the smallest relevant tests.
9. Inspect `git diff` and report any runtime testing that remains necessary.

Do not perform a repository-wide class split in one uncontrolled change.
Do not combine file separation with unrelated feature work or large architectural redesign.

---


## 29.3 Inheritance and Common Base Class Rule

This section applies to LAVI-owned project classes and is subordinate to the scoped upstream baseline override in Section 0. Do not introduce or reshape inheritance in upstream-derived ChatClef code merely to remove duplication or satisfy a general architecture preference.

Inheritance must be actively considered when multiple LAVI-owned project classes share the same stable responsibility, lifecycle, validation flow, execution sequence, or error-handling template.

Do not repeatedly copy the same control flow into sibling classes when the differences can be expressed as small subclass-specific steps.

### Mandatory Inheritance Evaluation Trigger

Before adding or modifying a second class with behavior similar to an existing class, Codex must compare the classes and determine whether they share:

* the same public operation or lifecycle
* the same ordered execution steps
* the same validation, permission, retry, logging, cleanup, or error-handling flow
* the same constructor dependencies
* the same result conversion or response-building flow
* differences limited to an action name, endpoint, command prefix, strategy hook, payload type, or one small execution step

If two or more concrete classes share a stable algorithm or control-flow skeleton, Codex must evaluate a common abstract base class before adding more duplicated logic.

If three or more classes already repeat the same skeleton, Codex must not add another copied implementation. It must first extract or extend a common base class unless doing so would violate substitutability, create an unsafe dependency, or break compatibility.

This evaluation is required even when the current duplicated methods are individually short.

### Preferred Inheritance Pattern: Template Method

Use the Template Method pattern when the overall algorithm must remain consistent but one or more steps vary by implementation.

The base class should:

* own the stable public execution method
* enforce shared ordering and invariants
* perform common validation, permission checks, logging, retry, cleanup, and response conversion
* expose only the smallest necessary protected or abstract hooks
* keep subclass-specific logic out of the shared algorithm

The subclass should:

* declare its domain-specific identity clearly
* override only the variable step or steps
* avoid reimplementing the complete public algorithm
* remain substitutable anywhere the base type is accepted

Python example:

```python
from abc import ABC, abstractmethod
from typing import ClassVar


class MinecraftVerifiedItemActionBase(ABC):
    action_name: ClassVar[str]

    def run(self, item: str, count: int):
        blocked = self._permission_guard.blocked_response(self.action_name)
        if blocked is not None:
            return blocked

        validation = self._validator.validate(self.action_name, item, count)
        if validation is not None:
            return validation

        return self._verified_runner.run(
            action=self.action_name,
            item=item,
            count=count,
            submit=lambda: self._submit(item, count),
        )

    @abstractmethod
    def _submit(self, item: str, count: int):
        raise NotImplementedError
```

```python
class MinecraftCraftAction(MinecraftVerifiedItemActionBase):
    action_name = "craft"

    def _submit(self, item: str, count: int):
        return self._client.craft(item, count)
```

In this structure, permission checks, validation, verified execution, and result handling belong to the base class. Only the bridge operation belongs to the subclass.

### ABC, Interface, and Protocol Selection

Choose the contract type deliberately.

Use an abstract base class when:

* subclasses have a real `is-a` relationship
* shared implementation or state is required
* a stable execution template must be enforced
* subclasses must override one or more well-defined hooks
* duplicated behavior would otherwise exist in every implementation

Use a `Protocol`, interface, or pure abstract contract when:

* only a callable or behavioral contract is required
* implementations may be structurally unrelated
* shared implementation is not appropriate
* dependency inversion across plugins, adapters, extensions, or infrastructure boundaries is the primary goal

A Python `Protocol` provides a structural contract but does not remove duplicated implementation by itself.
If sibling classes implement the same `Protocol` and also duplicate the same algorithm, keep the `Protocol` for the external contract and add an abstract base class for the shared implementation when appropriate.

Example relationship:

```text
MinecraftCommandHandler Protocol
    <- external handler contract

MinecraftItemCommandHandlerBase ABC
    <- shared parsing, validation, and response flow

MinecraftCraftCommandHandler
MinecraftEquipCommandHandler
    <- domain-specific implementations
```

### Inheritance Versus Composition Decision Rule

Inheritance and composition are both valid, but they solve different problems.

Prefer inheritance when:

* the subtype is a specialized form of the base type
* the shared algorithm must not be reordered by each implementation
* subclass identity is meaningful in logs, registration, testing, or public APIs
* most behavior is invariant and only a small hook varies
* preserving named domain classes improves readability and traceability

Prefer composition, delegation, or strategy objects when:

* behavior must be selected or replaced at runtime
* collaborators have independent lifecycles
* there is no true `is-a` relationship
* only a utility function or isolated operation is shared
* multiple independent capabilities must be combined
* inheritance would expose unrelated protected state or methods

Do not use composition merely to avoid inheritance when it causes every sibling class to duplicate the same orchestration flow.

Do not use inheritance merely to share a few unrelated helper functions. Extract a focused function or collaborator instead.

### Preserve Domain-Specific Classes

Do not collapse several meaningful domain classes into one overly generic configurable class solely to reduce the class count.

Prefer thin subclasses when separate class identities are useful for:

* dependency injection wiring
* command or action registration
* plugin discovery
* logging and diagnostics
* tests and mocks
* public imports
* future subclass-specific behavior
* human-readable architecture

For example, these named classes may remain separate:

```text
MinecraftCraftAction
MinecraftGetItemAction
MinecraftGetAndEquipAction
```

while inheriting their common execution flow from:

```text
MinecraftVerifiedItemActionBase
```

A generic class such as `ConfiguredAction(action_name, callback)` is acceptable only when the objects have no meaningful independent domain identity and the generic form is clearly easier to understand.

### Base Class Design Rules

A shared base class must have one clear responsibility.

Required rules:

* Give the base class a name that describes the shared domain role, not a vague name such as `Base`, `Common`, `HelperBase`, or `ManagerBase`.
* Prefer names such as `MinecraftVerifiedItemActionBase`, `MinecraftPrefixCandidateSelectorBase`, or `GameExtensionBase`.
* Keep public behavior in the base class and variable behavior behind protected or abstract methods.
* Keep abstract hooks small and purpose-specific.
* Do not make subclasses override a method only to copy most of the base implementation.
* Do not require subclasses to know the internal ordering of the template algorithm.
* Do not expose mutable base-class state unless subclasses genuinely require it.
* Prefer constructor injection for shared dependencies.
* Document invariants that subclasses must preserve.
* Use `final` or language-equivalent restrictions when a template method must not be overridden and the language/project supports it safely.
* Do not add unrelated responsibilities to a base class because multiple subclasses happen to need them.

### Substitutability Rule

Every subclass must obey the behavioral contract of its base class.

A subclass must not:

* weaken required validation or permission checks
* change the meaning of an inherited public method
* return an incompatible result shape
* require callers to perform subtype-specific preconditions not required by the base type
* silently skip shared logging, cleanup, retry, or error handling
* raise new unexpected exceptions for normal inputs accepted by the base contract

If a proposed subclass cannot follow the base contract, it is not a valid subtype. Use composition or a separate hierarchy instead.

### Language-Specific Inheritance Rules

Apply the same design rule using the language's native mechanism.

* Python: use `ABC` and `@abstractmethod` when shared implementation and enforced hooks are required. Keep `Protocol` for structural contracts.
* TypeScript: use an `abstract class` for shared implementation and `protected abstract` hooks. Use an `interface` when only the external contract is required.
* Java: use an `abstract class` for the template implementation and abstract methods for variable steps. Mark the template method `final` when subclasses must not replace the algorithm.
* C++: use an abstract base class with pure virtual hooks and a virtual destructor. Keep ownership explicit and avoid deep virtual hierarchies.
* C: C has no class inheritance. Use a focused interface struct with function pointers, an explicit context pointer, and shared non-virtual functions for the stable execution template.

TypeScript example:

```typescript
//YYYYMMDD_kpopmodder: Added this abstract action to centralize verified item-action execution.
abstract class MinecraftVerifiedItemActionBase {
  protected abstract readonly actionName: string

  public async run(item: string, count: number): Promise<ActionResult> {
    const blocked = this.permissionGuard.blockedResponse(this.actionName)
    if (blocked !== null) {
      return blocked
    }

    const validation = this.validator.validate(this.actionName, item, count)
    if (validation !== null) {
      return validation
    }

    return this.verifiedRunner.run({
      action: this.actionName,
      item,
      count,
      submit: () => this.submit(item, count),
    })
  }

  protected abstract submit(item: string, count: number): Promise<ActionResult>
}
```

Do not simulate inheritance with repeated wrapper classes that each copy the same public method.
Do not replace a useful abstract base class with an interface plus duplicated implementations merely to claim that the code uses composition.

### Inheritance Depth and Multiple Inheritance

Keep inheritance shallow.

Preferred structure:

```text
interface or Protocol
    -> abstract base class
        -> concrete implementation
```

Prefer one abstract implementation level and one concrete subclass level.
Do not add deeper inheritance chains without a clear written justification.

Avoid multiple inheritance for production behavior.
Multiple inheritance is allowed only for small, stateless, clearly named mixins whose responsibilities do not overlap and whose method-resolution order is obvious.

Do not use mixins to bypass the one-primary-responsibility rule or to assemble a hidden large class from many unrelated behaviors.

### File and Folder Placement

A reusable abstract base class is an independent project type and must follow the one-class-per-file rule.

Python examples:

```text
plugins/Minecraft/minecraft_core/actions/base/
    minecraft_verified_item_action_base.py

plugins/Minecraft/minecraft_core/actions/
    minecraft_craft_action.py
    minecraft_get_item_action.py
    minecraft_get_and_equip_action.py
```

Folder placement must follow the existing component structure.
Do not create a `base/` folder automatically when the component has only one base class and an existing responsibility-oriented folder is already clear.

Acceptable alternatives include:

```text
plugins/Minecraft/minecraft_core/actions/minecraft_verified_item_action_base.py
```

or a focused contract folder when multiple related contracts exist:

```text
plugins/Minecraft/minecraft_core/actions/contracts/
    minecraft_item_action.py
    minecraft_verified_item_action_base.py
```

Do not create repository-wide dumping grounds such as:

```text
base_classes/
abstracts/
inheritance/
common_bases/
```

### Refactoring Existing Duplicate Implementations

When converting existing sibling classes to inheritance, Codex must:

1. List the duplicate public flow shared by the classes.
2. Separate invariant steps from subclass-specific steps.
3. Explain why the subclasses satisfy an `is-a` relationship.
4. Propose the exact base class and subclass file paths.
5. Preserve existing public class names, methods, constructor compatibility, imports, registration keys, configuration keys, and response shapes.
6. Move only the shared behavior into the base class.
7. Keep specialized behavior in the concrete subclass.
8. Avoid unrelated cleanup or renaming in the same change.
9. Add or update tests for both the shared base behavior and every subclass hook.
10. Search for direct construction, mocks, patch targets, registries, and dynamic imports that reference the original classes.

If constructor compatibility cannot be preserved directly, use a small compatibility adapter or staged migration rather than breaking all call sites at once.

### Required Tests for Inheritance Refactoring

At minimum, test:

* shared validation runs for every subclass
* shared permission checks cannot be bypassed
* the correct subclass hook is called
* action names, command names, endpoints, or payload types remain correct
* shared exceptions and cleanup behavior remain unchanged
* each subclass remains usable through the base contract or interface
* existing registration and dynamic loading still resolve the concrete class
* no old duplicated public flow remains in concrete subclasses

Recommended Python checks:

```bat
python -m compileall <changed_package>
python -m pytest <smallest_relevant_test_path>
git diff --check
```

### Forbidden Inheritance Patterns

Do not:

* create an empty base class with no contract or shared behavior
* create a base class only because two class names share a suffix
* move unrelated helpers into a base class
* use inheritance to access another class's internal state
* override a template method and replace the entire shared algorithm
* duplicate the base algorithm inside each subclass
* create subclass flags that produce many hidden execution branches in the base class
* use `isinstance` chains in the base class to detect concrete subclasses
* create a deep hierarchy that makes runtime behavior difficult to trace
* replace clear domain classes with one large inheritance tree for speculative future reuse
* introduce inheritance into stable third-party, vendored, or generated code

### Required Planning Report

Before introducing or changing a production inheritance hierarchy, Codex must report:

```text
Shared contract:
- <public operation and expected behavior>

Invariant flow:
- <steps owned by the base class>

Variable hooks:
- <steps implemented by subclasses>

Proposed base class:
- <exact path>: <responsibility>

Concrete subclasses:
- <exact path>: <specialized behavior>

Why inheritance is valid:
- <is-a relationship and substitutability explanation>

Compatibility to preserve:
- constructors
- public imports
- registration keys
- config keys
- response types
- dynamic loading paths

Validation:
- <tests and commands>
```

If inheritance is rejected after evaluation, Codex must briefly state why composition, delegation, a strategy, a shared function, or a protocol-only contract is safer.

---

## 29.4 Game Extension Migration Rule

New games must be added as `GameExtension` implementations and integrated through `ExtensionRegistry`.

* Do not add future game-specific behavior as direct ad-hoc plugin wiring in startup or UI paths.
* Each new game must provide a `GameExtensionInterface` implementation (`name`, `initialize`, `start`, `stop`, `handle_command`, `get_status`).
* Register and lookup games by logical name via `ExtensionRegistry` (for example: `chess`, `starcraft116`).
* Add game runtime execution/observation/command split through `GameBridge`/`GameWorker` or equivalent adapter layers.
* Keep existing legacy game modules working during migration by using shim/wrapper compatibility paths.
* Update this rule when any new game (Minecraft, Helltaker, StarCraft2, etc.) is added.

For any new game:

1. Add extension wrapper first.
2. Register through AppComposer/registry layer.
3. Preserve old behavior as fallback until new path is verified.
4. Document migration status in extension adapter and project docs.

## 30. Error Handling Rules

Prefer safe fallback behavior.

When adding exception handling:

* Log enough information to debug.
* Do not hide critical errors silently.
* Do not crash the whole app for recoverable audio/device errors.
* Preserve user-visible behavior when possible.

For audio playback and TTS, cleanup should happen even after exceptions.

Use `finally` blocks where appropriate.

---

## 30.1 Null, Nil, Undefined, and Invalid Reference Safety Rule

Null-reference failures are not limited to Java.

Different languages use different names, but the underlying defect is usually the same: code assumes that a value, object, pointer, callback, collection element, dependency, configuration value, or external response exists when it may actually be absent, invalid, uninitialized, or already disposed.

Common manifestations include:

```text
Java                  NullPointerException
Kotlin                NullPointerException or failed `!!`
C#                    NullReferenceException
Python                AttributeError on None, TypeError from None, or UnboundLocalError
JavaScript/TypeScript TypeError when reading or calling a property of null/undefined
C/C++                 null pointer dereference, access violation, segmentation fault, or undefined behavior
Go                    panic caused by nil pointer dereference or nil interface misuse
Rust                  panic from unwrap/expect on None, or unsafe raw-pointer misuse
Swift                 fatal error from unexpectedly unwrapping nil
Objective-C           silent nil behavior or invalid pointer access
```

Codex must treat all of these as one broad defect category:

```text
Null / nil / None / undefined / invalid-reference safety
```

### Core Rule

Do not assume that a value exists merely because:

* a type annotation says it should exist
* a constructor parameter is normally supplied
* a dictionary or map key usually exists
* a configuration key has a documented default
* an external API normally returns an object
* a callback normally returns a value
* a dependency injection container normally supplies an implementation
* a parser normally succeeds
* a UI component normally exists
* a collection is normally non-empty
* a background operation normally reaches an assignment
* validation occurred in another layer
* a test mock normally behaves like the real implementation

At every trust boundary, determine:

1. Can the value be absent?
2. Can it be present with the wrong type?
3. Can creation fail before assignment?
4. Can reload, shutdown, disposal, callback, async work, or another thread invalidate it?
5. Does absence mean optional, not found, disabled, pending, failure, or invalid input?
6. Should the correct behavior be fallback, typed failure, validation error, or fail-fast exception?

Do not add a null check without deciding the correct semantic behavior.

### Mandatory Review Trigger

Perform an explicit null-safety review whenever code involves:

* optional configuration values
* JSON, YAML, TOML, environment variables, or external settings
* HTTP, WebSocket, database, subprocess, file, plugin, game bridge, or model responses
* callbacks and event handlers
* dependency injection, registries, factories, and plugin loading
* parser or extractor results
* dictionary, map, object, or payload lookups
* collection indexing or first/last element access
* UI component maps
* lazy initialization
* reload, reconnect, shutdown, disposal, or lifecycle transitions
* background threads, async tasks, futures, queues, or timers
* variables assigned inside `try`, conditional, loop, callback, or asynchronous branches
* C/C++ pointers, function pointers, handles, COM pointers, or API output pointers
* force unwraps, non-null assertions, casts, and untrusted numeric or enum conversions

Review both explicit null-like values and wrong-type values that cause the same failure at the use site.

### Validate at Boundaries

Validate nullable and untrusted values where they enter the owning component.

Preferred boundaries include:

```text
configuration loader
HTTP or bridge transport
database or repository adapter
parser-result boundary
composition root
plugin or action registry
UI component builder
callback adapter
thread or task entry point
C/C++ API wrapper
```

Preferred flow:

```text
untrusted value
    -> validate and normalize once
    -> typed domain value or explicit failure
    -> internal code
```

Avoid passing raw nullable values through several services and scattering `.get()`, casts, default values, and defensive checks across unrelated modules.

### Absence Must Have One Meaning

Do not use one null-like value to represent several unrelated states.

Avoid using `None`, `null`, `nil`, or `undefined` interchangeably to mean:

```text
not configured
not found
not initialized
operation failed
operation pending
feature disabled
empty result
permission denied
transport disconnected
invalid input
```

Use a result type, status enum, error object, or explicit state when callers must distinguish these cases.

Do not return null for an error when the caller requires an error reason.

### Configuration Null Semantics

Configuration handling must distinguish between:

```text
key absent
key present with null
key present with an invalid value
key present with a valid false, zero, or empty value
```

Do not automatically convert explicit null to false, zero, an empty string, or an empty collection unless the configuration contract defines that behavior.

Do not use truthiness fallback when false, zero, or an empty value is meaningful.

Avoid:

```python
count = int(value or 1)
```

Prefer deliberate handling:

```python
if value is None:
    count = 1
else:
    count = parse_count(value)
```

Use a default only when explicit null is documented to mean “use the default.” Otherwise return a validation error.

### Python Rules

Python equivalents commonly appear as:

```text
AttributeError: 'NoneType' object has no attribute ...
TypeError caused by None in int(), len(), iteration, arithmetic, or context-manager use
UnboundLocalError when assignment occurred only in a failed branch
KeyError or IndexError caused by unsafe assumptions
```

Required practices:

* Use `Optional[T]` or `T | None` when absence is part of the contract.
* Narrow optional values before dereferencing them.
* Do not use `typing.cast()` to hide a real nullability problem.
* Do not use `assert value is not None` for public or external runtime validation.
* Initialize variables before conditional or `try` branches when used afterward, or return immediately from failure branches.
* Validate values returned from callbacks, mocks, transports, plugins, registries, and dynamic imports.
* Validate dictionary values, not only key presence.
* Catch `TypeError` and `ValueError` narrowly around untrusted conversions when a typed failure is appropriate.
* Do not catch `Exception` merely to hide a `None` defect.

Unsafe:

```python
try:
    result = extension.handle_command(command)
except Exception as error:
    message = format_error(error)

notify(message)

if result.get("pending"):
    watch(result)
```

Safe:

```python
try:
    result = extension.handle_command(command)
except Exception as error:
    notify(format_error(error))
    return

notify(format_result(result))

if is_pending(result):
    watch(result)
```

### Java and Kotlin Rules

Java:

* Do not catch `NullPointerException` as normal control flow.
* Validate required arguments with `Objects.requireNonNull()` when immediate failure is appropriate.
* Use `Optional<T>` only when absence is a meaningful return result.
* Do not return null for collections when an empty immutable collection is the defined result.
* Validate nullable intermediate values in chained calls.
* Preserve useful context in null-related errors.

Kotlin:

* Prefer nullable types and explicit handling.
* Avoid `!!` unless the invariant is locally proven and documented.
* Validate Java platform types.
* Use `requireNotNull`, `checkNotNull`, safe calls, Elvis handling, or typed results according to the contract.
* Do not use `?: default` when null should be invalid.

### C# Rules

* Enable and respect nullable reference types when supported.
* Do not use the null-forgiving operator `!` merely to silence diagnostics.
* Validate required constructor arguments and dependency injection results.
* Use `ArgumentNullException.ThrowIfNull()` or the project equivalent for required public inputs.
* Use `?.` and `??` only when the fallback is semantically correct.
* Distinguish absence from operation failure.

### JavaScript and TypeScript Rules

Typical failures include:

```text
TypeError: Cannot read properties of null
TypeError: Cannot read properties of undefined
TypeError: value is not a function
```

Required practices:

* Enable and respect `strictNullChecks` when supported.
* Do not use non-null assertion `!` merely to silence the compiler.
* Do not use optional chaining to hide a missing required dependency.
* Validate JSON, DOM, IPC, HTTP, plugin, environment-variable, and callback boundaries.
* Use discriminated unions for success, failure, pending, disabled, and not-found states.
* Validate both property existence and property type.
* Check `Map.get()` results before dereferencing.
* Do not use `value || default` when `0`, `false`, or `""` is valid.
* Use `value ?? default` only when nullish fallback is the documented behavior.

### C and C++ Rules

A null pointer dereference may cause an access violation, segmentation fault, memory corruption, or undefined behavior.

Required practices:

* Validate nullable raw pointers before dereferencing them.
* Initialize pointers and handles to a defined invalid state.
* Check API return codes before using output pointers or handles.
* Validate function pointers before calling them.
* Use RAII and explicit ownership in C++.
* Prefer references or an established non-null contract for required dependencies.
* Use `std::optional<T>` for absent values that do not require pointer identity.
* Preserve the distinction between null pointers, invalid OS handles, empty buffers, and failed operations.
* Do not return pointers to expired objects.
* Inspect lifetime and ownership, not only nullness.

A null check does not fix:

```text
dangling pointer
use-after-free
uninitialized pointer
invalid handle
data race
buffer lifetime error
```

### Go Rules

* Check errors before using returned values.
* Do not use a value when `err != nil`.
* Account for interfaces that are non-nil while containing a typed nil pointer.
* Validate nil maps, slices, channels, functions, and pointers according to their language behavior.
* Do not call a nil function.
* Do not recover from a nil panic merely to continue with invalid state.

### Rust Rules

* Use `Option<T>` for meaningful absence.
* Do not use `unwrap()` or `expect()` on runtime input, configuration, external responses, plugins, or lifecycle state unless failure is deliberately fatal and documented.
* Handle `Option` and `Result` with pattern matching, `?`, or explicit error mapping.
* In `unsafe` or FFI code, validate raw pointers and document ownership and lifetime.

### Swift and Objective-C Rules

Swift:

* Avoid force unwrap `!` for runtime or external values.
* Use optional binding, `guard let`, or typed errors.
* Do not use implicitly unwrapped optionals for values that may remain absent after initialization.

Objective-C:

* Sending a message to `nil` can silently return zero-like values and hide defects.
* Explicitly validate required objects when silent nil behavior would corrupt state.
* Distinguish deliberate optional receivers from missing required dependencies.

### Dependency Injection and Registry Rules

Composition roots, registries, factories, plugin loaders, and tests must reject invalid dependencies before runtime use.

Do not permit:

```text
action name -> null
parser list item -> null
service dependency -> null
callback -> null
UI component key -> null
plugin object without its required method
```

Validate that:

```text
the object exists
required methods exist
required methods are callable
result types match the contract
missing registration returns a named error
duplicate registration is handled deliberately
```

Prefer a precise composition-time error over a later unrelated attribute or method failure.

### External Response Normalization

External systems may return:

```text
null
an empty body
a scalar instead of an object
an object missing required fields
a field present with null
a field with the wrong type
a stale or partially initialized object
```

Transport and adapter boundaries must normalize malformed responses into the project’s standard failure result.

Do not let raw malformed data pass through multiple service layers.

A method that promises a dictionary, object, or DTO must not return null without declaring that possibility.

### Collection and Mapping Rules

Before using a collection or mapping:

* distinguish missing from empty
* validate the collection type
* validate required keys
* validate values associated with those keys
* check length before indexing
* check map lookup results before dereferencing
* account for concurrent invalidation when relevant

This is not sufficient for untrusted data:

```python
count = int(payload.get("count", 0))
```

The key may exist with `None` or an invalid string.

### Lifecycle, Reload, and Concurrency Rules

Null and invalid-reference defects often occur during:

```text
startup
partial initialization
reload
reconnect
shutdown
plugin replacement
thread cancellation
async failure
UI teardown
resource disposal
```

Required practices:

* Do not publish partially initialized dependencies.
* Build and validate replacement state before swapping it into live state.
* On failed reload, retain the previous valid state.
* Do not clear a shared dependency while active users may still access it without a defined synchronized lifecycle.
* Ensure callbacks cannot observe half-updated state.
* Return from failure branches before using values that require success.
* After disposal, return an explicit unavailable result rather than dereferencing cleared state.
* Add synchronization only when an actual shared-state race is identified.

### Null Object Pattern

A Null Object is allowed only when “do nothing successfully” is valid domain behavior.

Potentially valid examples:

```text
no-op logger
disabled metrics sink
optional notification sink
```

Do not use a Null Object to hide:

```text
missing required bridge client
missing model
failed plugin initialization
invalid configuration
missing action handler
permission denial
transport failure
```

A Null Object must obey the full contract and must not report misleading success.

### Exception and Fallback Rules

Do not catch a null-reference failure and continue without repairing or rejecting the invalid state.

Avoid:

```text
catch NullPointerException and ignore
catch NullReferenceException and return success
catch AttributeError and assume every case means None
catch TypeError around unrelated code
recover from panic without identifying the invalid state
```

When a value is required, reject it early with a precise error.

When it is optional, handle absence explicitly.

When an external system is malformed, return a typed failure and preserve the last valid internal state.

When continuing would corrupt state, fail the current operation safely rather than inventing a default.

### Required Tests

When nullable values, optional results, dependency composition, boundary validation, or lifecycle state are changed, consider tests for:

* required dependency missing
* optional dependency absent
* dictionary key absent
* dictionary key present with null
* wrong value type
* callback returns null
* callback raises before later assignment
* transport returns null
* transport returns empty or malformed data
* parser returns no match
* invalid registry entry
* empty collection
* configuration key absent
* configuration key explicitly null
* false, zero, or empty string used as valid values
* reload preparation fails before swap
* callback fails during reload
* shutdown occurs before a late callback
* background operation fails before result assignment
* previous valid state remains usable after failure

Do not weaken tests to accept a silent default unless that default is part of the documented contract.

### Static Analysis and Compiler Support

Use existing project tools when available:

```text
Python       mypy, pyright, Ruff, pylint, pytest
TypeScript   strictNullChecks, tsc, ESLint
Java         compiler warnings, SpotBugs, Error Prone, NullAway
Kotlin       compiler null-safety
C#           nullable reference types and analyzers
C/C++        compiler warnings, clang-tidy, sanitizers, static analyzers
Go           go vet, staticcheck, tests
Rust         compiler, clippy, tests, Miri where appropriate
Swift        compiler warnings and tests
```

Do not add or upgrade analysis dependencies without following the dependency rules.

Static-analysis success does not replace runtime boundary tests.

### Required Review Report

For null-safety investigation or remediation, report:

```text
Nullable or invalid-reference sources:
- <path and value>

Possible failure:
- <language-specific exception, panic, access violation, or undefined behavior>

Current contract:
- <required, optional, pending, disabled, not found, or failure>

Chosen behavior:
- <fallback, validation error, typed result, or fail-fast exception>

Files changed:
- <exact paths>

Tests added:
- missing value
- explicit null
- wrong type
- failure before assignment
- lifecycle or reload failure when relevant

Validation:
- <commands and results>

Remaining runtime risk:
- <manual or integration validation>
```

### Forbidden Patterns

Do not:

* add broad exception handling only to hide a null defect
* use force unwraps or non-null assertions without a locally proven invariant
* replace every null with zero, false, an empty string, or an empty object
* use truthiness when false, zero, or an empty value is valid
* return success after a required dependency is missing
* publish partially initialized objects
* dereference external responses before type and field validation
* assume a mapping value is valid because its key exists
* assume a variable was assigned inside a branch that may fail
* use null checks as a substitute for C/C++ ownership and lifetime analysis
* silently convert malformed responses into normal empty results
* spread unrelated defensive checks throughout internal code instead of validating the owning boundary
* weaken types to `Any`, `object`, raw pointers, or broad nullable unions merely to silence diagnostics

The goal is not to eliminate every optional value.

The goal is to make absence explicit, validate it at the correct boundary, preserve valid state after failures, and prevent language-specific null-reference crashes or silent state corruption.

---

## 31. Security Rules

Do not commit secrets.

Do not print secrets.

Sensitive values include:

* OpenAI API keys
* Hugging Face tokens
* VTube Studio tokens
* local credentials
* private server addresses
* personal access tokens

If a secret appears in a file, warn the user.

---

## 32. User Communication Rules

When explaining changes to the user:

* Be direct.
* Explain what changed.
* Explain why it changed.
* Mention risky areas.
* Provide Windows commands when useful.
* Do not over-explain obvious Git basics unless asked.

The user prefers practical guidance and copy-paste-ready commands.

---

## 33. Korean Project Notes

The user may use Korean comments and Korean documentation.

Do not delete Korean comments simply because they are not English.

Korean comments often contain important project history and risk notes.

Preserve comments that explain why a workaround exists.

---

## 34. Known Fragile Areas

Treat these as high-risk areas:

```text
TTS queue
TTS interrupt
audio playback
VTube Studio mouth control
Whisper silence handling
speaker recognition
GPT-SoVITS API startup
CUDA DLL loading
llama-cpp-python installation
Gradio queue/event behavior
audio device selection
model path configuration
memory retrieval
MemoryRouter / MemoryNeedClassifier
derived_memory.sqlite3
long_term_memory.json
raw_events consolidation
memory context injection
```

Any change in these areas should be small and carefully explained.

---

## 35. Preferred Workflow for AI Agents

Before editing:

1. Verify the current directory, Git root, branch, upstream, and working tree using read-only commands.
2. Classify each affected file as LAVI-owned, upstream-derived, vendored, generated, or third-party, and apply the highest-priority scoped rule before evaluating refactoring.
3. Inspect the relevant files, surrounding callers, fallback paths, cleanup ownership, and existing tests.
4. For a failure or unexpected behavior, inspect the existing logs, stack traces, compiler output, tests, and reproduction evidence.
5. If the root cause is not proven, state that it is unknown and add structured diagnostic logs before changing behavior.
6. Reproduce the problem and inspect the new trace.
7. If any material boundary, branch, state, value, callback, queue operation, external response, timeout, fallback, cleanup step, or terminal result remains unobservable, add more logs and reproduce again.
8. Continue the logging and reproduction cycle until the exact failure mechanism is verified. No speculative behavioral patch is allowed before this point.
9. Summarize the planned root-cause change, exact files, ownership boundaries, and compatibility that must be preserved.
10. Make the smallest safe patch allowed by the applicable scoped rules.
11. Show what changed.
12. Suggest the smallest relevant test command and any required manual runtime scenario.
13. Before any cleanup, deletion, move, rename, reset, branch deletion, force operation, or mass file operation, print the exact target list and stop for user confirmation.

After editing:

1. Check syntax or compilation if possible.
2. Run the smallest relevant test and inspect diagnostic output.
3. Check `git diff --check`, inspect the actual diff, and check `git status --short --branch`.
4. Warn about runtime tests that still need to be done.
5. Report every file created, modified, moved, or proposed for deletion.
6. Do not commit or push unless asked.
7. If cleanup candidates were found, report them only. Do not delete them automatically.

---

## 36. Final Principle

This project is already working in many areas.

Do not break working behavior for unnecessary cleanup.

When in doubt:

* preserve existing behavior
* do not guess the root cause
* add more structured diagnostic logs until the failing boundary is proven
* accept bounded, information-dense investigation logs rather than an unsupported behavioral change
* make smaller changes
* document fragile assumptions
* ask before changing versions
* prioritize Windows runtime stability
