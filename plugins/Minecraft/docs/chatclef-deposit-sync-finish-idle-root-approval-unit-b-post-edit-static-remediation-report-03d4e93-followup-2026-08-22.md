# ChatClef deposit sync finish idle root - Approval unit B follow-up static remediation report

Date: 2026-08-22

## Scope and authorization

This report records the Java/JUnit/document edit-only follow-up remediation after the post-commit static review of commit `03d4e93`.

This pass changed only Fabric ChatClef Java source, JUnit source, and this new static report. It did not run Gradle, JUnit, Java compile, build, JAR copy, Minecraft runtime, or Python. It did not change `adris/**`, Forge/MineMind, dependencies, thresholds, result envelope fields, result_fidelity values, terminal outbox behavior, replay/retry behavior, active release behavior, or historical review documents.

## Fresh provenance

Repository root: `C:\Vtuber_Souorce_Code\LAVI`

Branch: `minecraft-plugin-fix/alto-clef-infinite-loop`

HEAD before this edit-only pass: `03d4e93b36ffdde4936da274eab72632569ea5b0`

Pre-edit `git status --short` contained only the already-created untracked reviewed addendum:

```text
?? plugins/Minecraft/docs/chatclef-deposit-sync-finish-idle-root-approval-unit-b-post-commit-static-review-fail-addendum-03d4e93-2026-08-22.md
```

That reviewed addendum was not modified by this pass. Its SHA-256 remained:

```text
957B75CA2B92E4633D19C726109731EEC96AED9D7CA04E3CE5C6CC153536D9CF
```

## Touched files and SHA-256

| File | Pre-edit SHA-256 | Post-edit SHA-256 |
| --- | --- | --- |
| `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandLifecycleCoordinator.java` | `E0C2A422DD520FF9B4BD6EDB56CCA921532913A27591BAC7C3097C67B7F49BA1` | `4DC9179A449DB0D21786E7F72EE9E0B66B3D76F0AF2C38B5A5075C239C7D1AF8` |
| `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefPreexistingIdleRootStabilityGate.java` | `2E45A1684902E98678E6545BC0D58F9D1F130584BF4E7882AAC30D40A3250871` | `E1D64EA45BB826AA765CDF7DC9E7FFAA186BEC0B623CC9A887EA14C78CE41F36` |
| `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/evidence/FabricChatClefNonterminalLifecycleEvidencePublisher.java` | `4D18D4BAD8FF368F3AE47A5CA0ED9F7B591D3EFE01F789085225B002680B6F83` | `F69346C0D69055D6552D2EB8EC62DD91F026A25F31D0CA2961D520D1D00C3A88` |
| `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/evidence/FabricChatClefStableRequestQuiescenceTracker.java` | `37C4DB60FBFE72E0CD9277858BED1E6F2C3B1D718056F24212EFF20C558BE186` | `F53C24DB4565D5BDAC90786DA13EAF4C911311B12CC3B7D61166B8054B2822B5` |
| `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandOutcomeClassifierTest.java` | `3C50E85361216D0A038AF5B42E91D1B93095AE49567DF206B7C1F13F66DAED97` | `153E090A496559DBA47F1906B2783327E4137E99DE1D48BCC3653112DA149C3D` |
| `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefPreexistingIdleRootStabilityGateTest.java` | `7E8CC435A83CF480C1541FA77EA9F6136BA44299C4195E9D7E132A32477C0DC0` | `6428D276B5A82CE3A601489CEDE17A1E287BF2EE5618CB52B98B4B16AAFC78B8` |
| `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefTaskFinishedEventAssociationTest.java` | `D77D92F065E5D31F405F5D864E2CB80BE9CCF9FA7D696F76787D76DD785EACEA` | `8C1E26CCAE7ABD4643271F195B16A995C16B0DCCB84DC59432E674AB30349706` |
| `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/evidence/FabricChatClefStableRequestQuiescenceTrackerTest.java` | `2D0B76B8B89EB190BFEC3C163D55B06B6798610170492AC244F89BD2AD5B876D` | `19CCBF7DD98D4A2D0C1E0D365937C4A6E3AB6246B33A42C5F50BA2F8939ED7D6` |
| `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/evidence/FabricChatClefNonterminalLifecycleEvidencePublisherTest.java` | `ABSENT/NEW` | `E9B165961ED6566D3CB3E0E08836A0F8DFE5CC5E5D8321F98DF34988AED5A6E7` |
| `plugins/Minecraft/docs/chatclef-deposit-sync-finish-idle-root-approval-unit-b-post-edit-static-remediation-report-03d4e93-followup-2026-08-22.md` | `ABSENT/NEW` | Final SHA-256 is computed after this file is written. |

## Remediation summary

### Publisher clock and evidence ordering

`FabricChatClefNonterminalLifecycleEvidencePublisher.publishIfEligible()` now captures task ownership evidence before reading monotonic and wall clocks:

1. `ownershipEvidenceSupplier.get()`
2. `nanoTimeSupplier.getAsLong()`
3. `currentTimeMillisSupplier.getAsLong()`

The public production constructor still uses `FabricChatClefTaskStateReader::ownershipEvidence`, `System::nanoTime`, and `System::currentTimeMillis`. The added supplier/clock constructor is package-private and exists only to prove ordering without a global mutable clock or runtime sleep.

### Request-root observation state table

Allowed `request_root_observation_state` values after this pass:

| Condition | State | Qualification effect |
| --- | --- | --- |
| No execution | `UNKNOWN` | Not qualified |
| Execution has no bound request root | `NEVER_OBSERVED` | Does not set reappeared latch |
| Evidence unavailable, stale, future-dated, or current root unavailable | `UNKNOWN` | Not qualified |
| Current root is the exact bound request root object | `STILL_PRESENT` | Sets execution-lifetime `request_root_reappeared=true`; not qualified |
| Current root is neutral `IdleTask` and not the exact bound request root | `OBSERVED_AND_GONE` | Can qualify only if the reappeared latch is false |
| Any other visible root | `UNKNOWN` | Not qualified |

`OBSERVED_NEUTRAL_ROOT_STABLE` is no longer used as a request-root observation state.

The `request_root_reappeared` latch is no longer cleared by `resetWindow()` or signature changes. It clears only on full tracker reset or execution replacement.

### Snapshot age semantics

`FabricChatClefStableRequestQuiescenceTracker` and `FabricChatClefPreexistingIdleRootStabilityGate` now compute `snapshot_age_ms` once per observation and use that same value for both the stale/future predicate and the diagnostic payload.

If `nowNanos < capturedAtNanos`, the age is `Long.MAX_VALUE`; it is not hidden as `0`.

### Active-context proof table

`same_session_generation=true` now requires all of the following:

| Proof | Required |
| --- | --- |
| Execution object | Non-null and currently tracked |
| Active queue context | Non-null |
| Context identity | `execution.context() == activeContext` |
| Session id | Both nonblank and equal |
| Connection generation | Both nonnegative and equal |

Failure yields:

```text
qualified=false
same_session_generation=false
blocked_reason=active_context_identity_mismatch
```

The coordinator now passes the current end-tick active queue context to the preexisting-idle gate and nonterminal publisher. The existing coordinator behavior that clears an execution without a matching active queue context was intentionally preserved; no active ownership release behavior was redesigned.

## JUnit boundary map

No JUnit was executed in this pass. The following test-source changes were added or updated as static coverage:

| Test file | Production boundary covered |
| --- | --- |
| `FabricChatClefNonterminalLifecycleEvidencePublisherTest` | `publishIfEligible()` captures evidence before clocks, calls `FabricChatClefCommandExecution.runningLifecycleEvidenceResult(...)`, and sends through `FabricChatClefCommandResultSender.sendCommandResult(...)`; verifies finish sequence 1 and stable sequence 2. |
| `FabricChatClefStableRequestQuiescenceTrackerTest` | `FabricChatClefStableRequestQuiescenceTracker.observe(...)` state machine for stale/future evidence, exact root identity, execution-lifetime reappeared latch, window reset, execution replacement, and active-context proof. |
| `FabricChatClefPreexistingIdleRootStabilityGateTest` | `FabricChatClefPreexistingIdleRootStabilityGate.observe(...)` with active context proof, stable window, stale evidence, assignment/generation changes, and compatibility payload aliases. |
| `FabricChatClefCommandOutcomeClassifierTest` | Manual qualified observation fixture updated to the valid `NEVER_OBSERVED` request-root state for preexisting idle root. |
| `FabricChatClefTaskFinishedEventAssociationTest` | Manual qualified observation fixture updated to the valid `NEVER_OBSERVED` request-root state for preexisting idle root audit-only association. |

## Execution status

Static check run:

```text
git diff --check
```

Result: passed, with only Git line-ending conversion warnings.

Not run:

```text
Gradle
JUnit
Java compile
build
JAR copy
Minecraft runtime
Python
commit
push
```

## Current status before this report file was added

```text
 M plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandLifecycleCoordinator.java
 M plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefPreexistingIdleRootStabilityGate.java
 M plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/evidence/FabricChatClefNonterminalLifecycleEvidencePublisher.java
 M plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/evidence/FabricChatClefStableRequestQuiescenceTracker.java
 M plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandOutcomeClassifierTest.java
 M plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefPreexistingIdleRootStabilityGateTest.java
 M plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefTaskFinishedEventAssociationTest.java
 M plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/evidence/FabricChatClefStableRequestQuiescenceTrackerTest.java
?? plugins/Minecraft/docs/chatclef-deposit-sync-finish-idle-root-approval-unit-b-post-commit-static-review-fail-addendum-03d4e93-2026-08-22.md
?? plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/evidence/FabricChatClefNonterminalLifecycleEvidencePublisherTest.java
```
