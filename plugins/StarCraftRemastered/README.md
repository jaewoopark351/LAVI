# StarCraft Remastered

Korean: [README_KO.md](README_KO.md)

This optional plugin helps LAV launch a user-installed StarCraft Remastered AI script setup through Samase, prepare ScreenVision-based coaching prompts, and expose a safe BWAPI-compatible adapter surface for future SAIDA strategy-port work.

It is not a native BWAPI DLL, memory injector, unit-control bot, reinforcement-learning agent, or mouse automation layer. The first goal is launch assistance, status tracking, short coaching from visible screen observations, and a data-only compatibility contract for strategy logic.

## What It Does

- Validates local StarCraft Remastered, Samase, and `aiscript.bin` paths.
- Launches Samase with the configured mod argument, usually `samase.exe custom`.
- Tracks the process PID when LAV started the process.
- Pulls the latest ScreenVision observation when connected from `main.py`.
- Sends a StarCraft coaching prompt to the normal LAV LLM pipeline only when the UI button is clicked.
- Provides `Command`, `GameState`, `Unit`, provider, event, and BWAPI-style wrapper classes for adapting SAIDA-like strategy code.
- Includes a C++ `bwapi_shim` scaffold with `BWAPI.h`, `BWAPI::Broodwar`, `AIModule`, `Unit`, `Player`, and a safe `LAVBWAPIRM::Bridge` contract.
- Writes a `logs\starcraft_bwapi_rm_snapshot.json` snapshot so SAIDA-style shim code can read a BWAPI-RM game state without touching game memory.
- Can relay a Samase-side read-only state file from `logs\starcraft_samase_readonly_state.json` into that BWAPI-RM snapshot.
- Reserves `logs\starcraft_bwapi_rm_commands.jsonl` for shim-emitted commands; commands are still logged/no-op on the Python side unless a later safe control backend is explicitly added.
- Blocks future automation when `mode` is not `single_player_only`, or when Battle.net/multiplayer screens are detected and not explicitly allowed.

## What It Does Not Include

- No `samase.exe`.
- No BWMetaAI or UEDAIP files.
- No `aiscript.bin`.
- No StarCraft game files, MPQ files, or other third-party binaries.
- No direct game control.
- No unmodified native SAIDA/BWAPI binary injection.
- No Remastered memory hooks, packet control, anti-cheat bypass, or Battle.net automation.
- No complete BWAPI binary ABI replacement yet.
- No live Remastered command execution from SAIDA yet; command queue handling is intentionally conservative.

Install StarCraft Remastered, Samase, BWMetaAI, or UEDAIP yourself, then point the config file at those local paths.

## Setup

1. Keep `modules.json` disabled until your local paths are ready:

```json
"StarCraftRemastered": false
```

2. Copy the example config:

```bat
copy plugins\StarCraftRemastered\config\starcraft_remastered_config.example.json plugins\StarCraftRemastered\config\starcraft_remastered_config.json
```

3. Edit `plugins\StarCraftRemastered\config\starcraft_remastered_config.json` for your local install paths.

   Keep these safety defaults unless you are deliberately testing a local-only adapter:

```json
{
  "mode": "single_player_only",
  "provider": "screen_input",
  "allow_battlenet": false,
  "allow_multiplayer": false,
  "auto_control": false
}
```

4. Set `"enabled": true` in the personal StarCraft config when you are ready to let the Launch button start Samase.

5. Enable the module:

```json
"StarCraftRemastered": true
```

6. Start LAV:

```bat
venv\Scripts\python.exe main.py
```

## Read-Only State Probe

You can write a sample Samase read-only state and mirror it into the BWAPI-RM
snapshot without launching StarCraft:

```bat
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_readonly_state_writer --frames 1
cd /d plugins\StarCraftRemastered\bwapi_shim
build-saida\Debug\scr_readonly_runtime.exe --snapshot=..\..\..\logs\starcraft_bwapi_rm_snapshot.json
```

Expected output includes `inGame=true`, non-zero minerals, unit counts, and
`commands=disabled`.

## Native Samase Plugin Probe

The first native Samase plugin is a heartbeat writer. It proves that Samase can
load a LAV plugin DLL and write `LAV_SAMASE_STATE_PATH`; it does not parse units
yet.

```bat
cargo build --manifest-path plugins\StarCraftRemastered\samase_readonly_plugin\Cargo.toml --target i686-pc-windows-msvc --release
```

Output:

```text
plugins\StarCraftRemastered\samase_readonly_plugin\target\i686-pc-windows-msvc\release\lav_samase_readonly_plugin.dll
```

When launched through LAV, `StarCraftLauncher` passes:

```bat
LAV_SAMASE_STATE_PATH
LAV_BWAPI_RM_SNAPSHOT_PATH
LAV_SAMASE_STATE_EVERY_N_FRAMES
LAV_SAMASE_HEARTBEAT_INTERVAL_MS
SAMASE_MORE_DLLS
```

`SAMASE_MORE_DLLS` is populated with the built read-only plugin DLL when
`samase_readonly_plugin_enabled=true` and the DLL exists. This is the confirmed
InitializeBridge v3 entrypoint. It writes `bridge.schema=lav_initialize_bridge_v3`,
`bridge.compat_schema=lav_initialize_bridge_v2`, and
`bridge.legacy_compat_schema=lav_initialize_bridge_v1` inside the read-only state
JSON with process diagnostics, loaded module list, StarCraft/client SDK/Samase
temp DLL/bridge module fingerprints, PE section maps, and verified section-bounded
scan ranges. Its `bridge.in_game_detector` v1 can read only a tiny allowlist of
fixed primitive values after `VirtualQuery` validation; it still does not
dereference unit pointers, resource pointers, or arbitrary game memory.
`bridge.offset_discovery` v2 can additionally scan small verified `.data` windows
for candidate offsets, 4KB window-profile hashes, and 256-byte focused chunk
profiles only, with pointer following disabled.

To search for the real Samase plugin API loader path, prepare candidate mod
folders and per-candidate run scripts:

```bat
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --plugin-dll plugins\StarCraftRemastered\samase_readonly_plugin\target\i686-pc-windows-msvc\release\lav_samase_readonly_plugin.dll --samase-exe "C:\Program Files (x86)\StarCraft\x86\samase-0.8.31.exe" --starcraft-x86-dir "C:\Program Files (x86)\StarCraft\x86"
```

Then run one generated script at a time, closing StarCraft before trying the
next candidate:

```bat
logs\samase_plugin_loader_probe\run\run_samase_plugins_original_name.cmd
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --check
```

`loader=samase_plugin_api_init` means Samase called `samase_plugin_init(api)`.
`loader=samase_plugin_api` means the game-loop hook also fired.
`loader=samase_more_dll_thread` means only the fallback `Initialize` path loaded.

If the direct DLL placement candidates all report `missing_state_file`, continue
with the generated `special_files_*` scripts. Those candidates test the
`samase/special_files` archive metadata path found in Samase binary strings.

## Notes

- The personal config file is gitignored.
- The state log is written under `logs\` by default, which is also gitignored.
- External game processes are not killed automatically by LAV shutdown.
- Direct gameplay control and mouse automation are future work only.
- The BWAPI-compatible layer is intended for source-level strategy adaptation, not for loading an unmodified native SAIDA DLL into Remastered.
- The intended long-term path is `SAIDA -> BWAPI-compatible shim -> LAV-BWAPI-RM bridge -> Samase single-player -> StarCraft Remastered`.
- The current bridge stage is `Samase/read-only state or ScreenVision observation -> StarCraftGameState -> BWAPI-RM snapshot JSON -> C++ FileBridge`.
