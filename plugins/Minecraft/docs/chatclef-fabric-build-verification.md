<!-- 20260807_kpopmodder: Documented clean, forced, artifact-aware runtime build verification for Fabric ChatClef 1.20.1. -->

# Fabric ChatClef 1.20.1 Build Verification

Date: 2026-08-07

This runbook defines the minimum evidence required before treating a Fabric
ChatClef 1.20.1 build as valid for Minecraft runtime testing.

It is documentation only. Reading this document does not authorize a Gradle
build, dependency resolution, Minecraft launch, runtime reproduction, file
copy, commit, or push. Those actions still require the authorization applicable
to the current task.

## Scope

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
```

The runtime is a multi-version Gradle project that uses preprocess, Loom remap,
resource filtering, Mixin configuration, and a remapped runtime jar. The
verification target in this document is the Minecraft 1.20.1 artifact and the
actual CurseForge instance that loads it.

## Canonical Verification Build

When a Fabric ChatClef 1.20.1 build is separately authorized and the result will
be used as compatibility evidence, run the following from the runtime root:

```bat
cd C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1
.\gradlew.bat clean build --rerun-tasks
```

Use this clean forced build by default for verification. Do not substitute an
incremental `build` result as proof that the current source produced the runtime
jar under test.

Incremental, compile-only, or targeted Gradle tasks may be used only as quick
local sanity checks when separately authorized. They must not be reported as
runtime verification, deployment verification, or compatibility evidence.

## Why Incremental Success Is Not Sufficient

This runtime preprocesses source across multiple Minecraft versions and then
runs Loom remap and Mixin-related resource processing. Incremental Gradle state
can retain cached or stale generated, remapped, or processed output.

That can hide failures such as:

```text
an old processed Mixin configuration remaining in build output
a stale refmap or remapped descriptor being reused
a changed Mixin target not being exercised by a fresh remap
an old jar being copied to the Minecraft instance after a new source edit
Minecraft loading a different ChatClef jar than the one just built
```

A successful Gradle exit proves only that the requested build graph completed.
It does not prove that Minecraft loaded the intended jar or that every Mixin
injection succeeded at runtime.

## Required Evidence Chain

### 1. Confirm The Build Boundary

Before building, record the current directory and repository root:

```bat
cd
git rev-parse --show-toplevel
```

The intended runtime root is:

```text
C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1
```

Do not change Java, Gradle, Loom, Fabric Loader, Minecraft, ChatClef, AltoClef,
Baritone, or Carry On versions merely to make verification pass.

### 2. Run The Clean Forced Build

Run exactly:

```bat
.\gradlew.bat clean build --rerun-tasks
```

Record the full command, exit code, and first relevant failure if the build does
not complete. Do not report a failed or interrupted build as verified.

### 3. Identify The Fresh 1.20.1 Runtime Jar

The 1.20.1 runtime artifact is expected under:

```text
versions\1.20.1\build\libs\
```

Its normal unclassified filename follows:

```text
chatclef-1.20.1-<mod_version>.jar
```

`<mod_version>` comes from `gradle.properties`. Do not accidentally deploy a
sources jar, development jar, shadow-only intermediate, or an artifact from a
different Minecraft version.

Record the source jar's absolute path, file size, modification time, and SHA-256
hash. For example:

```powershell
Get-Item ".\versions\1.20.1\build\libs\chatclef-1.20.1-<mod_version>.jar"
Get-FileHash ".\versions\1.20.1\build\libs\chatclef-1.20.1-<mod_version>.jar" -Algorithm SHA256
```

### 4. Verify The Jar Copied Into CurseForge

Before judging runtime behavior, identify the exact active CurseForge instance
and its `mods` directory. Do not infer the destination from an old test run or
from a similarly named instance.

Check for duplicate or stale ChatClef jars in the active instance, then compare
the deployed jar with the freshly built source jar:

```powershell
Get-ChildItem "<active CurseForge instance>\mods" -Filter "*chatclef*.jar"
Get-FileHash "<active CurseForge instance>\mods\<deployed ChatClef jar>.jar" -Algorithm SHA256
```

The source and deployed SHA-256 hashes must match. A matching filename or
modification time alone is not sufficient proof that the copy is current.

### 5. Verify Minecraft Runtime Loading And Mixin Injection

After the matching jar is deployed and Minecraft launch is separately
authorized, inspect the new launch's logs and any crash report. At minimum,
check:

```text
<active CurseForge instance>\logs\latest.log
<active CurseForge instance>\crash-reports\*.txt
```

Confirm that the intended ChatClef version was loaded and look for Mixin or
remap failures, including patterns such as:

```text
MixinApplyError
InvalidMixinException
InvalidInjectionException
InjectionError
NoSuchMethodError
NoSuchFieldError
target method was not found
descriptor mismatch
refmap warning or remap failure tied to ChatClef
```

A window appearing is useful launch evidence, but it is not enough by itself.
The log and crash-report review remains required because some injection or
initialization failures are only visible there.

## Verification Outcomes

Use one of these conclusions:

```text
BUILD_FAILED
The clean forced Gradle build did not complete successfully.

BUILD_PASSED_RUNTIME_NOT_VERIFIED
The clean forced build completed, but the deployed jar hash or Minecraft runtime
log evidence is missing.

DEPLOYMENT_MISMATCH
The active CurseForge instance did not contain the freshly built 1.20.1 jar, or
multiple conflicting ChatClef jars were present.

RUNTIME_MIXIN_FAILED
Minecraft loaded the test instance, but the log or crash report showed a Mixin,
remap, descriptor, target, or injection failure. When Gradle succeeds but
Minecraft fails with a Mixin injection, descriptor, refmap, or target error,
report it as a runtime Mixin compatibility failure, not as a Gradle build
failure.

RUNTIME_VERIFIED
The clean forced build completed, the fresh 1.20.1 jar was deployed with a
matching SHA-256 hash, Minecraft loaded the intended artifact, and the reviewed
launch logs contained no relevant runtime Mixin failure.
```

Do not collapse `BUILD_PASSED_RUNTIME_NOT_VERIFIED` into `RUNTIME_VERIFIED`.

## Minimum Completion Report

A build-verification report must include:

```text
repository root
runtime working directory
exact Gradle command
Gradle exit code
1.20.1 source jar path
source jar SHA-256
deployed CurseForge jar path
deployed jar SHA-256
duplicate ChatClef jar check
Minecraft launch log path and launch time
crash report path, or an explicit statement that no new crash report was created
relevant Mixin/remap findings
final verification outcome
```

Do not claim runtime compatibility from source inspection, an incremental build,
a successful compile, a matching filename, or a copied jar without the complete
evidence chain above.
