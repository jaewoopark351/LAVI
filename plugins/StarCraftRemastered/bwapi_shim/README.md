# LAV-BWAPI-RM Shim

<!-- #20260701_kpopmodder: Documents the safe C++ shim boundary for SAIDA compatibility work. -->

This folder is the first C++ compatibility layer for running SAIDA-style BWAPI strategy code against a future StarCraft Remastered single-player bridge.

It is not a loader, injector, memory hook, packet tool, anti-cheat bypass, or Battle.net automation layer.

## Intended Shape

```text
SAIDA strategy code
  -> BWAPI-compatible shim headers/runtime
  -> LAV-BWAPI-RM Bridge
  -> Samase single-player runtime work
  -> StarCraft Remastered local client
```

The current implementation is source-level compatibility scaffolding. It gives SAIDA-like code BWAPI names such as `BWAPI::Broodwar`, `BWAPI::AIModule`, `BWAPI::Unit`, `BWAPI::Player`, `BWAPI::UnitType`, and `BWAPI::Position`.

It now also includes the first mock runtime path:

```text
BWAPI-compatible shim
  -> LAVBWAPIRM::GameStateProvider
  -> LAVBWAPIRM::MockGameStateProvider
  -> LAVBWAPIRM::MockBridge
  -> LAVBWAPIRM::CompatRunner
```

That path calls `onStart()` once, calls `onFrame()` repeatedly, exposes mock minerals/gas/supply/unit lists, and logs unit commands instead of executing them.

## Current Status

- Provides a minimal `BWAPI.h` facade.
- Provides a `LAVBWAPIRM::Bridge` interface for snapshots and commands.
- Provides a `NullBridge` that never controls the game.
- Provides a `FileBridge` that reads Python-written snapshots and appends commands to a JSONL queue.
- Provides a `GameStateProvider` interface and deterministic `MockGameStateProvider`.
- Provides a `MockBridge` that logs commands.
- Provides a `CompatRunner` for `onStart()` / `onFrame()` callback loops.
- Provides a minimal SAIDA-style bot compile probe.
- Provides a mock runtime probe executable.
- Provides a read-only snapshot probe executable for Samase/LAV state files.
- Does not attach to StarCraft Remastered.
- Does not implement BWAPI binary ABI compatibility.

## Local Mock Probe

From this folder, build and run the source-compatibility probe with CMake:

```bat
cmake -S . -B build
cmake --build build --config Debug
build\Debug\mock_runtime_probe.exe
```

Expected output includes:

```text
[Bot] onStart
[Bot] onFrame
[Command] frame=1 type=TRAIN
[Command] frame=2 type=GATHER
[Command] frame=3 type=BUILD
[Command] frame=4 type=MOVE
[Command] frame=5 type=ATTACK
```

## Read-Only Snapshot Probe

After LAV or a future Samase-side helper writes `logs\starcraft_bwapi_rm_snapshot.json`,
verify the native state bridge without game control:

```bat
cmake --build build-saida --config Debug --target scr_readonly_runtime
build-saida\Debug\scr_readonly_runtime.exe --snapshot=..\..\..\logs\starcraft_bwapi_rm_snapshot.json
```

Expected output includes:

```text
[SCR readonly] connected=true
[SCR readonly] commands=disabled
[SCR readonly] ok=true
```

## Local SAIDA Source Probe

Keep the SAIDA source outside this repository. The expected local path is:

```bat
C:\Vtuber_Souorce_Code\SAIDA-saida-aiide2018
```

Then build the optional source-compatibility target:

```bat
cd /d C:\Vtuber_Souorce_Code\LAVI\plugins\StarCraftRemastered\bwapi_shim
cmake -S . -B build-saida -G "Visual Studio 17 2022" -A Win32 -DLAV_BUILD_SAIDA_MOCK=ON -DLAV_SAIDA_SOURCE_DIR=C:/Vtuber_Souorce_Code/SAIDA-saida-aiide2018
cmake --build build-saida --config Debug --target saida_mock_runtime
build-saida\Debug\saida_mock_runtime.exe
```

This target excludes SAIDA's original `main.cpp` and `dllmain.cpp`. It constructs `MyBot::MyBotModule` directly, then lets `CompatRunner` call `onStart()` and `onFrame()` over `MockBridge`.

## Next Work

1. Collect SAIDA's actual BWAPI include and call surface.
2. Expand `include/BWAPI.h` only for APIs SAIDA really uses.
3. Build the shim with Visual Studio/CMake and run the minimal SAIDA-style compile probe.
4. Run `mock_runtime_probe` and verify command logs for `train`, `gather`, `build`, `move`, and `attack`.
5. Build `saida_mock_runtime` and use compiler errors as the real SAIDA BWAPI compatibility checklist.
6. Expand `FileBridge` parsing only as real SAIDA compile/runtime needs appear.
7. Add a Samase single-player backend for safe state translation.
