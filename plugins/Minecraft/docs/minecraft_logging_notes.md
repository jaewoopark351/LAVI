<!-- 20260727_kpopmodder: Added logging notes for ChatClef/AltoClef troubleshooting. -->

# Minecraft Logging Notes

This document records the logging conventions used while troubleshooting the LAVI Minecraft module.

## Runtime Log Files

For CurseForge instances, inspect all three files when diagnosing Minecraft behavior:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\<instance>\logs\latest.log
C:\Users\jaewo\curseforge\minecraft\Instances\<instance>\logs\stdout-logs.txt
C:\Users\jaewo\curseforge\minecraft\Instances\<instance>\logs\instance_audit.txt
```

Use `latest.log` for the human-readable Minecraft and AltoClef flow. Use `stdout-logs.txt` when messages are duplicated through Log4j XML-style events or when `latest.log` was rotated during a run. Use `instance_audit.txt` to confirm the CurseForge launch time, Minecraft version, Fabric loader, and instance-level changes.

LAVI application logs live here:

```text
C:\Vtuber_Souorce_Code\LAVI\logs
```

Use the LAVI log to connect natural-language commands, background responses, TTS playback, and game extension startup with the Minecraft-side task logs.

## StateChangeLogger Usage

Use `StateChangeLogger` for task and chain diagnostics that should log only when behavior changes.

Preferred pattern:

```java
debugLogger.state("stable phase key", "detail with counters, slots, or positions");
```

The first argument must be stable for the current phase. The second argument may include changing details such as walk cost, fuel count, slot contents, health, or position. This prevents tick spam while preserving useful context at the moment the phase changes.

Use `event(...)` for one-shot boundaries such as start, stop, interrupt, task assignment, config load, and explicit state resets.

Avoid this pattern:

```java
debugLogger.state("walk/open container: walkCost=" + costToWalk);
```

Changing numbers in the state key cause repeated per-tick log output.

## High-Noise Areas

These areas are useful to log, but must be kept phase-based:

- `DoStuffInContainerTask`: container discovery, crafting-table/furnace placement, walking/opening containers, cursor cleanup.
- `AbstractSmeltInContainerTask`: material collection, fuel collection, inaccessible material movement, input/fuel/output slot operations, waiting for smelting.
- `MobDefenseChain`: defense pauses, shielding, projectile avoidance, hostile target changes, falling/MLG interruptions.
- `MineAndCollectTask` and inner mining/pickup tasks: target selection, pickup-vs-mine choice, task stop reason.
- `DestroyBlockTask`: target start/stop, unreachable blocks, unsafe direct breaks, stuck recovery, water mining failure.

Do not log every tick just because a method runs every tick. Log the phase transition or the reason a decision changes.

## Smelting Diagnostics

For furnace, smoker, and blast furnace tasks, verify these phases in order:

1. Resource task started for the requested `SmeltTarget`.
2. Materials are available or a material collection task starts.
3. Fuel is available or a fuel collection task starts.
4. Inaccessible materials are moved into inventory if needed.
5. Container is placed or an existing container is opened.
6. Input slot receives material.
7. Fuel slot receives fuel.
8. Output slot is collected.
9. The parent resource task finishes.

Repeated `wait for smelting` or `ready for furnace interaction` lines usually mean the logger key includes tick-changing details or the code is running from an old jar.

## Config Diagnostics

If `latest.log` contains this pattern:

```text
ConfigHelper using defaults after parse failure
Unrecognized field "foodPickCookedFoodBonus"
14 known properties
```

the active Minecraft instance is probably still running an old ChatClef jar. Rebuild the 1.20.1 jar, copy the remapped jar into the instance `mods` folder, and restart Minecraft.

Expected 1.20.1 build command:

```powershell
cd C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1
.\gradlew.bat :1.20.1:remapJar
```

Copy this file only:

```text
C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar
```

Do not copy `-sources.jar`, `-dev.jar`, `-dev-sources.jar`, or `-all.jar` into the CurseForge instance.

## Verification Checklist

After replacing the jar and restarting Minecraft:

1. Confirm `instance_audit.txt` has a new launch entry.
2. Confirm the jar in the instance `mods` folder has the expected new timestamp.
3. Confirm `latest.log` does not show `FoodChainConfig` unknown-field parse failure.
4. Confirm task completion with `main task finished/stopped: finished=true` when testing a user task.
5. Check repeated counts for noisy messages such as `walk/open container`, `get container item`, `wait for smelting`, and `MobDefenseChain`.

Logs are debugging artifacts. Do not commit Minecraft logs, LAVI logs, or generated audio/log outputs.
