<!-- 20260806_kpopmodder: Added LAVI-specific integration notes for the Fabric ChatClef runtime. -->

# LAVI Integration Notes For Fabric ChatClef 1.20.1

This file documents how the upstream-derived Fabric ChatClef runtime is used
inside LAVI.

It is documentation only. It does not approve source changes, Gradle changes,
dependency changes, Minecraft launch, runtime reproduction, instance file
changes, commit, or push.

## Runtime Location

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
```

This tree is an upstream-derived ChatClef / AltoClef compatibility baseline
with LAVI-specific bridge, diagnostics, overlay, and optional integration
entrypoints.

Do not treat the whole tree as ordinary LAVI-owned code. Classify a target file
before editing it:

```text
upstream-derived ChatClef / AltoClef / Baritone-adjacent code
existing LAVI-specific compatibility patch
new or existing LAVI-owned integration file
```

## Observed Upstream Identity

Current local metadata from `src/main/resources/fabric.mod.json`:

```text
mod id: altoclef
mod name: Alto Clef
homepage: https://github.com/MiranCZ/altoclef
sources: https://github.com/MiranCZ/altoclef
```

Current local Gradle metadata from `gradle.properties`:

```text
mod_version=0.18.23
maven_group=gaucho-matrero.altoclef
archives_base_name=chatclef
loader_version=0.16.2
```

Runtime logs have locally shown:

```text
altoclef 1.20.1-0.18.23
baritone 1.10.1-9-geace2ad1-dirty
```

The exact upstream repository commit, release tag, and import date are not yet
verified. Do not promote runtime labels or `fabric.mod.json` contact metadata
to exact provenance without an upstream comparison.

## LAVI-Owned Java Entrypoints

Current Fabric main entrypoints include:

```text
adris.altoclef.AltoClef
lavi.minecraft.integration.toolselect.ToolSavePolicyEntrypoint
lavi.minecraft.integration.carryon.CarryOnDiagnosticEntrypoint
lavi.minecraft.overlay.OverlayEntrypoint
lavi.minecraft.fabric.chatclef.bridge.FabricChatClefBridgeEntrypoint
```

`adris.altoclef.AltoClef` is the upstream-derived engine entrypoint.

The `lavi.minecraft...` entrypoints are LAVI-owned integration, diagnostics,
overlay, or bridge boundaries. Keep optional integrations optional and keep
generic ChatClef engine behavior outside LAVI-specific policy unless the
last-resort engine modification gate has been satisfied.

## Build Command

From the runtime root:

```powershell
cd C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1
.\gradlew.bat build
```

For a narrower Java compile check:

```powershell
cd C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1
.\gradlew.bat :1.20.1:compileJava
```

Running a build is a separate action from editing docs or source. Do not run it
unless the current task authorizes build verification.

## Version Notes For 1.20.1

For project `:1.20.1`, the current build files map:

```text
Minecraft: 1.20.1
Yarn mappings: 1.20.1+build.10
Fabric API: 0.92.2+1.20.1
Fabric Loader property: 0.16.2
MixinExtras: 0.3.5
Jackson: 2.16.0
nether-pathfinder: 1.5
Baritone artifact: cabaletta:baritone-unoptimized-fabric:1.20.1
```

Launcher runtime logs may show a different Fabric Loader version because the
Minecraft launcher or modpack controls the installed loader. Treat the Gradle
property and launcher runtime log as separate evidence until the active jar and
instance loader are verified together.

## Build Output

The expected 1.20.1 build output directory is:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/
```

After a build, verify the exact jar filename and timestamp from the filesystem
or Gradle output before copying it into a Minecraft instance.

Do not assume a Minecraft crash is using the newly built jar unless the active
instance `mods` directory, jar timestamp, and runtime logs prove it.

## Instance Install Procedure

Operational install steps are:

```text
1. Stop Minecraft and the launcher-managed game process.
2. Build the runtime jar when build verification is authorized.
3. Identify the newly generated 1.20.1 jar under versions/1.20.1/build/libs/.
4. Copy that jar into the target CurseForge instance mods directory.
5. Preserve required dependency mods already in the instance.
6. Launch the instance.
7. Verify the active mod list, startup log, bridge handshake, and jar timestamp.
```

Do not change global launcher settings, system Java installation, registry,
environment variables, or unrelated instance files as part of this procedure.

## Expected Bridge Startup Evidence

Useful evidence in logs includes:

```text
Fabric Loader and Minecraft version
loaded mod list includes altoclef 1.20.1-0.18.23
LAVI Fabric ChatClef bridge startup message
Python Fabric ChatClef server enabled and listening
handshake envelope from Java
handshake_ack from Python
StatusSnapshotDTO lifecycle_state connected
command gate accepted or rejected reason
command_request sent
Java command enqueued
client-tick command dispatch
command_result sent
Python accept_result accepted or rejected
```

Exact log text can change as diagnostics are added. Prefer stable fields such
as request ID, correlation ID, session ID, generation, status, and result
reason over visual ordering alone.

## Cache And World Reset Notes

For pathfinding loops, wrong-target movement, impossible routes, mining stalls,
or symptoms that appear only on one copied/restored world, read:

```text
plugins/Minecraft/docs/chatclef-baritone-cache-troubleshooting.md
```

Baritone cache reset is an operational diagnostic step, not a LAVI behavior
fix. If symptoms reproduce after cache reset, use command/task lifecycle
diagnostics before changing behavior.

## Difference From The Upstream README

The upstream README describes the base AltoClef project. LAVI adds:

```text
Fabric ChatClef bridge
Python-owned WebSocket server and session ownership
v1 bridge envelopes
command lifecycle diagnostics
optional Carry On diagnostics boundary
tool-save policy snapshot entrypoint
LAVI overlay entrypoint
LAVI-specific documentation and divergence records
```

Use this file for LAVI runtime integration details. Use the upstream README
only for upstream project context.

## Safe Change Rules

When modifying this runtime tree:

```text
preserve upstream-derived structure
prefer the smallest method-level hunk
keep Carry On optional
keep diagnostics side-effect-free
do not rename wire keys while logs are active evidence
do not add retries, timeouts, cancellations, or global input cleanup without proof
record any engine divergence in chatclef-engine-divergence-record.md
```

When the root cause is not proven, stop at diagnostics.
