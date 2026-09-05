<!-- 20260906_kpopmodder: Recorded the live wooden-button StackOverflow root cause, minimum engine hunk, verification contract, and rollback unit before source changes. -->
<!-- 20260906_kpopmodder: Appended the applied-hunk, focused-test, clean-build, artifact, and remaining-runtime verification ledger. -->
<!-- 20260906_kpopmodder: Added matching-JAR Chat and exact final-microphone post-fix evidence and retained the unrun explicit-material comparison. -->

# ChatClef Wooden Button Stack Overflow Pre-Change Root-Cause Report

## 0. Document status

```text
DOCUMENT_TYPE: PRE_CHANGE_ROOT_CAUSE_AND_IMPLEMENTATION_CONTRACT
DECISION_DATE: 2026-09-06
DECISION_STATUS: ROOT_CAUSE_PROVEN_FIX_APPLIED_RUNTIME_PARTIALLY_VERIFIED

BACKEND_SCOPE: FABRIC_CHATCLEF_ONLY
INCIDENT_ID: wooden-button-stack-overflow-2026-09-05-235653-kst
EVIDENCE_MODE: RUNTIME_REPRODUCTION
INCIDENT_RUNTIME_STATUS: CRASH_REPRODUCED_FROM_LOGS
ROOT_CAUSE_STATUS: SOURCE_AND_RUNTIME_STACK_TRACE_CORRELATED
APPLIED_ENGINE_DIVERGENCE_STATUS: PARTIALLY_VERIFIED

PRODUCTION_SOURCE_CHANGE_IN_THIS_TASK: ONE_BOOLEAN_VALUE_PLUS_REQUIRED_TRACEABILITY_COMMENT
TEST_SOURCE_CHANGE_IN_THIS_TASK: ONE_LAVI_OWNED_FOCUSED_REGRESSION_FILE
DOCUMENTATION_CHANGE_IN_THIS_TASK: IMPLEMENTATION_AND_VERIFICATION_LEDGER_UPDATED
TEST_EXECUTION_IN_THIS_TASK: PASSED
JAVA_BUILD_IN_THIS_TASK: CLEAN_FORCED_BUILD_PASSED_MIXED_PROVENANCE
DEPLOYMENT_IN_FOLLOW_UP: MATCHING_JAR_CONFIRMED
POST_FIX_MINECRAFT_RUNTIME_IN_FOLLOW_UP: GENERIC_BUTTON_CHAT_AND_FINAL_MICROPHONE_VERIFIED
COMMIT_PUSH_IN_FOLLOW_UP: AUTHORIZED_IN_PROGRESS
```

The incident runtime and the post-fix runtime are different evidence states.
The earlier artifact reproduced the crash. The corrected artifact was then
deployed byte-identically; two Chat requests and one exact final-microphone
request for the generic wooden button completed naturally without
`StackOverflowError`. The microphone path also delivered and played its Korean
TTS feedback once. This report records `PARTIALLY_VERIFIED`, because the live
explicit-oak and stone-button comparison remains unexercised.

## 1. Authority and scope

This report is the focused follow-up to
[ChatClef Korean Command Feedback, Generic Crafting, and Stop Pre-Change Contract](chatclef-korean-command-feedback-crafting-stop-pre-change-contract-2026-09-05.md).
It follows the upstream-baseline and engine-divergence rules in
[ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md),
the canonical build evidence chain in
[Fabric ChatClef 1.20.1 Build Verification](chatclef-fabric-build-verification.md),
and the backend boundary in
[Minecraft Backend Separation](minecraft-backend-separation.md).

The scope is exact:

```text
trusted Korean LAVI Chat input "버튼 만들어줘"
  -> request-local generic crafting default
  -> get wooden_button 1
  -> CollectWoodenButtonTask
  -> first normal Task evaluation
  -> StackOverflowError
```

The same request-local mapping is also available to a trusted final
VoiceInput microphone transcript, but the reproduced crash came from
`lavi_chat_ui`. This incident is not evidence of a microphone transport defect.

This report does not approve or propose:

- a Forge or MineMind change
- a Minecraft, Fabric, Loader, Loom, Java, Gradle, ChatClef, AltoClef, Baritone,
  or Carry On version change
- a `TaskCatalogue`, `CataloguedResourceTask`, TaskRunner, scheduler, input,
  goal, path, retry, timeout, or cleanup redesign
- a global Korean alias change
- replacing the broad `wooden_button` default with a hard-coded wood species
- deployment, Minecraft launch, live-world mutation, commit, or push

## 2. Runtime reproduction evidence

### 2.1 Artifact and log identity

The reviewed live instance was:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01
```

The supplied files were read successfully and were not empty:

| Evidence | Size | Relevant location |
|---|---:|---|
| `logs/latest.log` | 9,731,193 bytes | lines 5750-5759 |
| `logs/instance_audit.txt` | 2,639,828 bytes | latest launch record at line 558 |
| `logs/stdout-logs.txt` | 10,889,625 bytes | lines 17038, 17041, and 17055-17061 |
| `crash-reports/crash-2026-09-05_23.56.53-client.txt` | 189,106 bytes | crash header and recursive stack |
| `C:\Vtuber_Souorce_Code\LAVI\logs\20260905_234629_log.txt` | non-empty | lines 437-449 |

The active deployed JAR was:

```text
PATH: C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\chatclef-1.20.1-0.18.23.jar
SIZE_BYTES: 8370930
SHA256: 0C8E7F774C52D79DE5BD1BB9B0E516F6E75C5C20CF5D73414EDD6D8154128161
BUILD_ARTIFACT_HASH_MATCH: TRUE
```

The deployed hash equals the previously recorded clean-build artifact hash.
This incident is not attributed to a stale or mismatched ChatClef JAR.

### 2.2 Exact command identity and timeline

```text
INPUT_SOURCE: lavi_chat_ui
COMMAND: get wooden_button 1
REQUEST_ID: lavi-input-ko-83534b8c6b75453e858032487e7a37d8
CORRELATION_ID: lavi-d02edd568fa744639c464cefa907ed54
SESSION_ID: fabric-chatclef-86f8b275562e4c838bc8693c7024edf1
CONNECTION_GENERATION: 1
CLIENT_TICK_ID: 12915
ROOT_ASSIGNMENT_ID: user-root-14
ROOT_TASK: adris.altoclef.tasks.resources.wood.CollectWoodenButtonTask
```

| Time, KST | Evidence |
|---|---|
| `2026-09-05 23:56:52.786` | Python/Java bridge accepts and queues `get wooden_button 1`. |
| `23:56:52.800` | Render thread dispatches `@get wooden_button 1`. |
| `23:56:52.801` | `CollectWoodenButtonTask` becomes the user root Task. |
| `23:56:52.802` | Bridge records `waiting_for_terminal_condition`; no terminal result exists. |
| `23:56:52.965` | Render thread reports `java.lang.StackOverflowError`. |
| `23:56:54` | LAVI records WebSocket closure without a close frame and clears the active owner. |

The Korean response saying that one button was requested is a truthful dispatch
acknowledgement. It is not a completion claim. The crash happened before a
`completed` or `failed` command terminal could be produced.

### 2.3 Adjacent live behavior

The same session provides a useful narrow comparison:

| Command path | Observed result |
|---|---|
| `get trapdoor 1` | terminal completion at `23:52:31` |
| `get map 1` | terminal completion at `23:55:59` |
| `get wooden_pressure_plate 1` | terminal completion at `23:56:13` and `23:56:37` |
| exact Korean STOP | verified execution at ticks `6813` and `6814` |
| `get wooden_button 1` | `StackOverflowError` before terminal result |

This evidence narrows the regression to the broad wooden-button Task path. It
does not establish that every possible world, inventory, or recipe state has
been tested for the other commands.

## 3. Proven failure mechanism

The crash report repeatedly contains this cycle:

```text
CataloguedResourceTask.<init>(CataloguedResourceTask.java:30)
  -> TaskCatalogue.getSquashedItemTask(TaskCatalogue.java:692)
  -> TaskCatalogue.getItemTask(TaskCatalogue.java:722)
  -> CataloguedResourceTask.<init>(CataloguedResourceTask.java:30)
  -> ...
```

The first allocation frames include `MovementProgressChecker`,
`TimeoutWanderTask`, `PickupDroppedItemTask`, and `ResourceTask`. Those frames
show where repeated Task construction finally exhausted the Java stack. They
do not make movement or Baritone pathfinding the root cause.

The reviewed evidence also excludes the following direct causes:

- `OutOfMemoryError` or heap exhaustion
- a new native JVM fatal error or current `hs_err_pid` file
- ChatClef Mixin apply, transform, descriptor, or injection failure
- Carry On state observation or interaction
- Baritone world-cache corruption
- bridge admission or Korean command translation failure

## 4. Source-level causal chain

The runtime stack and the reviewed pre-fix source agreed at every boundary.

1. `CollectWoodenButtonTask.java:27` defines a one-plank recipe:

   ```java
   new ItemTarget[]{p, null, null, null}
   ```

2. `CollectWoodenButtonTask.java:13` in the reviewed pre-fix source incorrectly
   marked two slots as the same required material:

   ```java
   new boolean[]{true, true, false, false}
   ```

3. `CraftingRecipe.java:49` normalizes a `null` recipe slot to
   `ItemTarget.EMPTY`; `getSlot()` also returns that sentinel for an empty slot.

4. `CraftWithMatchingMaterialsTask.java:35-41` walks both `true` positions. The
   second position overwrites `sameResourceTarget` with the empty second slot.

5. `CraftWithMatchingMaterialsTask.java:133-135` copies that empty target with
   count `999999` and calls `TaskCatalogue.getItemTask(...)`.

6. The copied target is an uncatalogued zero-match target. Therefore
   `TaskCatalogue.java:716-722` selects `getSquashedItemTask(target)`.

7. `TaskCatalogue.java:691-692` constructs `CataloguedResourceTask`, whose
   constructor at `CataloguedResourceTask.java:28-30` calls
   `TaskCatalogue.getItemTask(target)` for the same zero-match target.

8. Steps 6 and 7 repeat without a base case until `StackOverflowError`.

The pressure-plate Task uses the same two-slot mask legitimately because its
recipe is `{p, p, null, null}`. The defect is the mismatch between the button's
one occupied recipe slot and its copied two-slot mask.

## 5. Ownership and provenance classification

```text
TARGET_FILE: plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/resources/wood/CollectWoodenButtonTask.java
OWNERSHIP: UPSTREAM_DERIVED_CHATCLEF_ALTOCLEF_BASELINE
WORKTREE_STATE_AT_PRE_CHANGE_RECORD: CLEAN_UNMODIFIED
RECORDED_IMPORT_COMMIT: daa9b13641cb3beb33ddb12b51f8e2bed5be3944
REPOSITORY_HEAD_AT_DOCUMENTATION: 0931e07874f9bd634aada282233e41bd66d1c000
BASELINE_FILE_SHA256: 85AA18165EFFCA7BB5A3E00B41AAF93AB85503AADFB022AFDFAE2B80A6CA8DC8
UPSTREAM_EXACT_COMMIT: UNVERIFIED
```

The applied edit is therefore a behavior-changing engine divergence even
though the functional correction changes only one boolean value. The affected
constructor owns one responsibility: describing which occupied recipe slots
must use the same plank material for wooden-button crafting. No class split,
new folder, inheritance change, or broader refactor is justified.

## 6. Last-resort containment analysis

```text
Verified failing boundary:
  CollectWoodenButtonTask(Item[] targets, ItemTarget planks, int count)
  passes a two-slot same-material mask for a one-slot recipe.

Last successful boundary:
  trusted Korean input ownership, generic-default resolution, bridge
  admission, Render-thread dispatch, and root Task assignment all succeed.

First failing boundary:
  the first normal CollectWoodenButtonTask evaluation constructs a material
  acquisition Task from the empty second recipe slot.

Why LAVI orchestration is insufficient:
  LAVI correctly requests the documented broad wooden-button target. Replacing
  it with oak_button would hide the engine defect and break the "any wood"
  default contract.

Why the optional Carry On boundary is insufficient:
  Carry On is not present in the causal stack and owns no crafting recipe data.

Why a task-local helper is insufficient:
  the narrowest existing owner is already CollectWoodenButtonTask's constructor;
  adding a helper would expand a one-value correction into additional structure.

Why a LAVI-owned parent Task is insufficient:
  it would duplicate or intercept established AltoClef crafting/material
  selection and create a broader lifecycle divergence.

Why TaskCatalogue hardening is not the first patch:
  a global zero-match guard would alter every resource Task while leaving the
  incorrect button recipe contract in place.
```

No input override, retry counter, timeout, custom goal, Baritone path,
interruption rule, cleanup rule, or global mutable state is owned or changed by
the applied hunk.

### 6.1 Required pre-implementation design report

```text
Engine behavior being preserved:
  broad wooden-button acquisition chooses one available plank family and crafts
  the corresponding wooden button through the existing AltoClef lifecycle

LAVI-owned orchestration boundary:
  trusted Korean Chat/final-Voice ownership, request-local
  버튼 -> get wooden_button 1 resolution, exactly-once submission, and truthful
  dispatch feedback; no behavior change proposed

Optional Carry On boundary:
  not involved and intentionally unchanged

Diagnostic observation boundary:
  existing LAVI command lifecycle logs, latest.log, stdout-logs.txt, and the
  crash report already identify the request, tick, root Task, first failing
  constructor cycle, and disconnect; no new diagnostic behavior is proposed

Owning parent and child Tasks:
  CollectWoodenButtonTask is the assigned user root; the intended child is the
  existing plank-acquisition ResourceTask, but recursive child construction
  throws before a child becomes active

Completion owner and predicate:
  existing ResourceTask/UserTaskChain completion remains the owner; the hunk
  does not change completion or terminal-result predicates

Input owner and release path:
  none acquired at the failing construction boundary; unchanged

Retry owner and terminal reason:
  existing engine resource lifecycle; no retry policy or terminal reason added

Custom goal or path owner and cleanup path:
  none created at the failing construction boundary; unchanged

Carry On absence fallback:
  not applicable; generic ChatClef crafting remains independent of Carry On

Carry state observation contract:
  not applicable and untouched

Why composition is safer than inheritance here:
  no new composition object or inheritance is needed; the narrowest existing
  Task already owns the incorrect recipe mask

Exact production file modified:
  plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/resources/wood/CollectWoodenButtonTask.java

Exact focused test file created:
  plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/resources/wood/WoodenButtonRecipeMaskRegressionTest.java

Existing upstream files intentionally left unchanged:
  CraftWithMatchingMaterialsTask.java
  CraftingRecipe.java
  TaskCatalogue.java
  CataloguedResourceTask.java

Evidence now present:
  focused post-hunk test result, clean-build result, and fresh corrected JAR
  identity; matching-JAR deployment; Chat generic-button runtime twice; exact
  final-microphone generic-button runtime and one-shot TTS once

Evidence still missing:
  explicit oak-button and stone-button live comparison results

Verification contract:
  section 9 of this report
```

## 7. Exact applied engine hunk

Applied functional change:

```diff
-        super(targets, woodItems -> woodItems.button, createRecipe(planks), new boolean[]{true, true, false, false}, count);
+        super(targets, woodItems -> woodItems.button, createRecipe(planks), new boolean[]{true, false, false, false}, count);
```

The implementation places the required engine-divergence marker and a short
reference to this report adjacent to the hunk:

```java
//20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
```

The functional behavior change is one boolean value in one constructor. The
marker and focused regression test are additional traceability and verification
changes; no other production method was modified for this defect.

The following files are intentionally not part of the production patch:

- `TaskCatalogue.java`
- `CataloguedResourceTask.java`
- `CraftWithMatchingMaterialsTask.java`
- the Python generic crafting-default mapping
- the global Korean item alias artifact
- TaskRunner, AltoClef lifecycle, Baritone, Carry On, and Forge/MineMind code

## 8. Behavior that must remain unchanged

- `버튼 만들어줘` still resolves to `get wooden_button 1` for both trusted
  LAVI Chat and trusted final VoiceInput microphone input.
- Explicit material commands such as `참나무 버튼 만들어줘` and
  `돌 버튼 만들어줘` retain their existing specific-target resolution.
- The chosen wooden-button output continues to correspond to the selected
  plank species.
- Wooden pressure plates keep their valid two-plank recipe and two-slot mask.
- Trapdoor, map, pressure-plate, command-feedback, STOP, and ordinary command
  ownership remain unchanged.
- No automatic retry or replay is introduced after a crash, disconnect, or
  uncertain terminal result.
- No new global alias, backend, protocol value, dependency, or version is added.

## 9. Required verification after implementation

### 9.1 Focused tests

The pre-change acceptance contract required the implementation and runtime
verification to prove:

1. The wooden-button recipe has one occupied same-material slot and exactly one
   `true` mask entry.
2. With no planks available, the first `CollectWoodenButtonTask` evaluation
   returns a finite plank-acquisition Task and does not construct a zero-match
   squashed target.
3. `get wooden_button 1` can begin and reach the normal terminal lifecycle
   without `StackOverflowError`.
4. Multiple plank species still map to their corresponding wooden buttons.
5. Explicit oak-button and stone-button requests remain specific.
6. Wooden pressure plates retain `{p, p, null, null}` plus two matching slots.
7. Python Chat and final-microphone tests continue to compile
   `버튼 만들어줘` to `get wooden_button 1` exactly once.
8. Adjacent trapdoor, map, pressure-plate, feedback, and STOP tests remain green.

The earlier offline suite proved command registration, translation, admission,
and build integrity but did not exercise the broad button Task's first
material-acquisition evaluation. The focused test then closed the exact
pre-recursion mask/occupied-slot invariant. A full Task tick was still a
live-runtime requirement at the close of that offline stage; the later
matching-JAR Chat and final-microphone runs in Sections 9.3 and 9.4 satisfied
the generic-button portion of that requirement.

### 9.2 Clean build

Run from the exact Fabric runtime root:

```powershell
.\gradlew.bat clean build --rerun-tasks
```

Record the repository HEAD and dirty inputs, command exit code, Java/Gradle
identity, test results, and fresh 1.20.1 JAR path, size, modification time, and
SHA-256. A successful build without post-fix runtime evidence must be reported
as `BUILD_PASSED_RUNTIME_NOT_VERIFIED`.

### 9.3 Deployment and post-fix runtime

Deployment and Minecraft execution require their own active scope. When run,
the fresh source and deployed JAR hashes must match and the log review must
prove all of the following:

```text
expected ChatClef artifact loaded
no duplicate active ChatClef JAR
no relevant Mixin/remap failure
trusted Chat generic-button request completes without StackOverflowError
trusted final-microphone generic-button request completes without duplicate submit
specific-material button requests remain specific
pressure-plate comparison remains successful
```

Current evidence status:

```text
FIX_STATUS: APPLIED_RUNTIME_PARTIALLY_VERIFIED
FOCUSED_TESTS: PASSED_5_FAILURES_0_ERRORS_0_SKIPPED_0
CLEAN_FORCED_BUILD: PASSED_MIXED_PROVENANCE
POST_FIX_DEPLOYMENT: MATCHING_JAR_CONFIRMED
POST_FIX_BUILD_DEPLOYMENT_LOAD_STATUS: RUNTIME_VERIFIED
POST_FIX_GENERIC_BUTTON_CHAT_RUNTIME: VERIFIED_TWICE
POST_FIX_GENERIC_BUTTON_FINAL_MICROPHONE_RUNTIME: VERIFIED_ONCE
POST_FIX_FINAL_MICROPHONE_FEEDBACK_TTS: VERIFIED_ONCE
POST_FIX_EXPLICIT_OAK_BUTTON_RUNTIME: NOT_RUN
POST_FIX_EXPLICIT_STONE_BUTTON_RUNTIME: NOT_RUN
```

### 9.4 Applied implementation and offline evidence

```text
IMPLEMENTATION_DATE: 2026-09-06
BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: 0931e07874f9bd634aada282233e41bd66d1c000
BUILD_INPUT_PROVENANCE: MIXED_PROVENANCE

BASELINE_BUTTON_SOURCE_SHA256: 85AA18165EFFCA7BB5A3E00B41AAF93AB85503AADFB022AFDFAE2B80A6CA8DC8
APPLIED_BUTTON_SOURCE_SHA256: CE447971FC1596C6333B9EAFB86DBDD0FB32C09862177F3E3F7FC6DE709D3283
FOCUSED_TEST_SOURCE_SHA256: F9F8DD95B892AC466CB505A63E34A76DFEC0DC1B40781E9278F2BC62586A8E1E

FOCUSED_JAVA_TESTS: 5 passed, 0 failed, 0 errors, 0 skipped
ADJACENT_PYTHON_TESTS: 52 passed, 121 subtests passed
FULL_MINECRAFT_CHATCLEF_PYTHON_TESTS: 901 passed, 2 skipped, 3462 subtests passed

CLEAN_BUILD_COMMAND: .\gradlew.bat clean build --rerun-tasks
CLEAN_BUILD_ENVIRONMENT: Eclipse Temurin 21.0.12+8; Gradle 8.8
CLEAN_BUILD_RESULT: BUILD SUCCESSFUL in 5m 23s
CLEAN_BUILD_TASKS: 171 executed
ALL_VERSION_JAVA_TESTS: 8437 tests, 0 failures, 0 errors, 9 skipped
MINECRAFT_1_20_1_JAVA_TESTS: 767 tests, 0 failures, 0 errors, 1 skipped

FRESH_JAR: versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar
FRESH_JAR_SIZE_BYTES: 8370931
FRESH_JAR_TIMESTAMP_LOCAL: 2026-09-06T00:58:35.6170045+09:00
FRESH_JAR_SHA256: 488C195B8C87919E349549DD5B0766D05D669222BB63FCCDEDD0E38FDB2861B2

BUILD_LOG: codex-build-logs/chatclef-fabric-1.20.1-build-20260906-005443.log
BUILD_LOG_SHA256: F2BE63098FEB16706873E1BDAE36AB44499350D17C1F80318DB0973F97708183
DEPLOYMENT: MATCHING_JAR_CONFIRMED
POST_FIX_MINECRAFT_RUNTIME: GENERIC_BUTTON_CHAT_VERIFIED_TWICE_FINAL_MICROPHONE_VERIFIED_ONCE
POST_FIX_FINAL_MICROPHONE_FEEDBACK_TTS: VERIFIED_ONCE
EXPLICIT_OAK_BUTTON_RUNTIME: NOT_RUN
EXPLICIT_STONE_BUTTON_RUNTIME: NOT_RUN
FINAL_STATUS: PARTIALLY_VERIFIED
```

The active `LAVI_TEST_Fabric01` JAR and fresh build artifact are both 8,370,931
bytes and have SHA-256
`488C195B8C87919E349549DD5B0766D05D669222BB63FCCDEDD0E38FDB2861B2`.
The post-fix runtime evidence is:

```text
Chat request lavi-input-ko-da979ad84d4f4c44bb6d62f3f1f09403 -> natural completion
Chat request lavi-input-ko-3a04f90c28004e599b3c95e27359f76f -> natural completion
final microphone request lavi-input-ko-7af892f671bc4e71aefb6b17aad8deaf -> natural completion
final microphone provenance VoiceInput / final_transcript / final=true
final microphone immediate Korean feedback delivery -> exactly once
final microphone TTS start and completion -> exactly once
current-session StackOverflowError -> 0
current-session fatal errors -> 0
```

The clean build input is deliberately classified `MIXED_PROVENANCE`: the
runtime tree already contained numerous unrelated user-owned Java and test
changes. The focused button hunk and test are identified separately above; the
successful build must not be described as a button-only or clean-HEAD artifact.

The focused test executes both production `createRecipe` builders, derives the
same-material target from the production source mask, proves that the button
selects the one non-empty plank slot, locks the broad `planks ->
CollectPlanksTask` boundary, preserves species-derived output selection, and
keeps the pressure-plate comparison unchanged. Its five cases ran without a
skip. Direct construction and first ticking of the full resource Task is not
safe in this Loom unit-test classpath: upstream `ResourceTask` construction
initializes Minecraft `Blocks`, while the headless test runtime does not apply
the runtime access-widener boundary and fails during registry bootstrap. The
test therefore closes the exact pre-recursion invariant without fabricating a
live client. At the close of offline verification, the actual ChatClef Task
lifecycle was still part of the required post-deployment Minecraft
reproduction. Sections 9.3 and 9.4 record the later matching-JAR Chat and final-
microphone completion; the explicit oak- and stone-button comparisons remain.

Disassembly of the fresh JAR additionally shows the constructor bytecode stores
`true` only at boolean-array index 0 and stores `false` at indices 1 through 3;
the recipe bytecode places the plank target only at slot 0.

### 9.5 Separate remaining wood-family risk

`ItemHelper.PLANKS` includes `BAMBOO_PLANKS`, while the existing BAMBOO
`WoodItems` entry has no plank field. This is not the reproduced zero-match
recipe-mask defect and was not changed in this hunk. Oak/spruce-style
species-derived selection remains structurally preserved, but this offline
evidence must not be expanded into a claim that the pre-existing bamboo path is
verified. Any bamboo correction requires its own source and runtime scope.

## 10. Rollback unit

If the applied change must be rolled back:

1. Apply the exact inverse boolean hunk in
   `CollectWoodenButtonTask(Item[] targets, ItemTarget planks, int count)`.
2. Remove the adjacent engine-divergence marker/reference only when no remaining
   divergence uses it.
3. Remove or update only the focused test that depends on the corrected mask.
4. Mark the canonical divergence entry `ROLLED_BACK` and retain this incident
   report as historical evidence.
5. Re-run the focused comparison and canonical clean build if rollback
   verification is requested.

Do not revert an entire upstream file, the Korean mapping, existing STOP work,
other ChatClef divergences, unrelated dirty documentation, or the repository.
Do not use broad reset, checkout, restore, clean, or commit-revert operations as
the rollback mechanism.

## 11. Completion criteria

The defect may be marked `VERIFIED` only after all of the following are true:

- the exact one-value source hunk and required marker are present
- the focused recipe-mask/pre-recursion regression passes
- adjacent Java and Python regressions pass
- the canonical clean forced build passes
- the fresh JAR identity is recorded
- the same fresh JAR is deployed to the exact test instance
- Chat and final-microphone generic-button runtime scenarios complete without
  stack overflow or duplicate submission
- the post-fix logs and any crash-report directory are reviewed

Documentation, compilation, or a matching artifact hash alone is not runtime
verification.
