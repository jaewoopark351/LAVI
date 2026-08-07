<!-- 20260807_kpopmodder: Documented report-only Mixin verification before making it a blocking CI gate. -->

# ChatClef Mixin Advisory Verification

Date: 2026-08-07

This runbook documents the report-only verification path for Fabric ChatClef
1.20.1 Mixin compatibility. It is intended to make Mixin, refmap, preprocessing,
and nested Baritone evidence visible before any GitHub Actions job is promoted
to a blocking required check.

This document does not authorize a Gradle build, Minecraft launch, runtime
reproduction, Java behavior change, Mixin config split, commit, push, or
deployment. Those actions still require the authorization applicable to the
current task.

## Current Incident Conclusion

The recent payload, handshake, and diagnostic assembly refactor commits are not
the direct cause of the observed `EntityAnimationSwungMixin` and
`MovementHelperMixin` runtime failures.

The stronger local explanation is:

```text
an earlier branch state contained a 1.20.1 production namespace, refmap, or
target selector mismatch
    -> later refactor commits inherited that whole branch state
    -> CI and runtime tested the full checkout at each commit
    -> the same Mixin mismatch appeared unrelated to the refactor diff
```

The two observed failures were in existing AltoClef/ChatClef Mixins:

```text
EntityAnimationSwungMixin
    failed by looking for named onEntityAnimation(...) on runtime class_634

MovementHelperMixin
    failed because allowInfested could not find the requested redirect target
    inside Baritone MovementHelper.avoidBreaking
```

The later tactical fixes use 1.20.1 runtime/intermediary selectors with
`remap=false`. That is a valid short-term fallback when the final production
bytecode has been inspected, but it must be guarded by final-jar checks because
the source, preprocessed source, compiled class, refmap, remapped jar, and
runtime class loader can all differ.

## Important Correction

The diagnostic-Mixin commit was not only a JSON/class addition. It also touched
the refmap/build pipeline. Current evidence does not prove that this build
setting change was the single root cause, but it is part of the suspect range.

Use this wider classification:

```text
diagnostic Mixin addition, associated refmap/build pipeline changes, or an
existing AltoClef selector mismatch continued to be exposed by later commits.
```

Do not reduce the explanation to "the payload refactor broke Mixin" or to
"the four diagnostic Mixins caused the two crashes."

## Advisory Mode

The initial GitHub Actions workflow must be advisory:

```text
manual workflow_dispatch only
non-blocking by default
report and artifacts are produced even when findings exist
no branch-protection required check
no automatic push or pull_request trigger
```

This avoids a new red X on every commit while still preserving evidence that can
be reviewed before promoting the check.

The advisory workflow may later gain a strict mode input, but the default must
remain report-only until the false positive rate is understood.

## Evidence Chain

The advisory check records evidence from the final artifact, not from source
files alone.

Minimum evidence:

```text
repository root
runtime root
Gradle build exit code, when a build was requested
selected final 1.20.1 jar path
final jar SHA-256
final altoclef.mixins.json presence and copy
final chatclef-refmap.json presence
nested Baritone jar entries and SHA-256 values
EntityAnimationSwungMixin final annotation selector
MovementHelperMixin final redirect target, ordinal, and remap value
Baritone MovementHelper bytecode signal for method_26204 call count
optional latest.log Mixin fatal-pattern scan
optional crash-report Mixin fatal-pattern scan
```

The current advisory script is not a full replacement for an ASM contract test.
It is a first report-only gate that inspects the final jar with zip metadata,
hashes, and `javap`. A later strict implementation should add an ASM verifier
for target classes, descriptors, instruction owners, opcodes, and ordinals.

## Local Report Command

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File `
  .\plugins\Minecraft\tools\chatclef_mixin_advisory_report.ps1
```

Optional log scan:

```powershell
powershell -ExecutionPolicy Bypass -File `
  .\plugins\Minecraft\tools\chatclef_mixin_advisory_report.ps1 `
  -MinecraftLogPath "C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log" `
  -CrashReportDirectory "C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\crash-reports"
```

The script writes report files under:

```text
artifacts/chatclef-mixin-advisory/
```

By default, findings do not fail the process. Use `-FailOnFindings` only after
the check is intentionally promoted to a strict gate.

## GitHub Actions Workflow

The initial workflow is:

```text
.github/workflows/chatclef-mixin-advisory.yml
```

It is manual-only and non-blocking by default. It can run a clean 1.20.1 Gradle
build in advisory mode and then always writes a report artifact.

Default build command inside the workflow:

```powershell
.\gradlew.bat clean :1.20.1:build `
  --rerun-tasks `
  --no-build-cache `
  --no-daemon `
  --stacktrace
```

The job records the Gradle exit code but exits successfully in advisory mode so
that the workflow does not create a red X merely because the investigation
found evidence.

## Promotion Path

Only promote this to a blocking CI gate after the following are stable:

```text
final jar discovery is reliable
required and optional findings are separated
nested Baritone hash is expected and documented
known 1.20.1 Mixin selectors are covered
false positives have been reviewed
GitHub Actions artifacts contain enough evidence to debug failures
```

Suggested next strict checks:

```text
processed Mixin resource verifier
ASM target-class verifier
ASM instruction and ordinal verifier
production-like Fabric class-load smoke
one minimal Baritone pathing smoke
release or nightly full preprocess-version matrix
```

## Diagnostic Mixin Policy

Diagnostic Mixins should not be permanently treated the same as core strict
Mixins unless they are required for normal gameplay correctness.

Safer future shape:

```text
core config:
    required=true
    defaultRequire=1

diagnostic config:
    required=false
    defaultRequire=0
    individual injectors use require=0 and expect=1
    config plugin keeps diagnostics disabled by default
```

This is a future runtime-configuration change, not part of this advisory
workflow. Applying it would change when diagnostic Mixins are loaded and needs
separate review and approval.

The important operational rule is:

```text
turning diagnostic logging OFF is not the same as disabling diagnostic Mixins
```

Mixin application happens before the diagnostic handler decides whether to emit
logs. A bad diagnostic selector can still crash startup or delayed class load
even when its logger is quiet.

## What This Does Not Prove

The advisory workflow does not prove every Minecraft runtime path. In
particular, it does not prove:

```text
Minecraft selected the same Baritone jar as the final artifact
another mod did not transform the same target before ChatClef
Mixin priority and transformer ordering are harmless
every late-loaded Baritone class was transformed successfully
pathing behavior is semantically correct
all preprocess versions beyond 1.20.1 are safe
```

Runtime verification still requires the artifact-aware build verification
runbook:

```text
plugins/Minecraft/docs/chatclef-fabric-build-verification.md
```
