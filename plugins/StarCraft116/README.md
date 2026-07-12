# StarCraft 1.16

This optional plugin is for local StarCraft 1.16 / BWAPI-era bot setups such
as SAIDA, Monster, and Stardust.

It does not include StarCraft, BWAPI, Chaoslauncher, bot DLLs, bot EXEs, MPQ
files, or any third-party binaries. It only validates and launches paths that
already exist on your machine.

## Enable

Set the main module switch:

```json
"StarCraft116": true,
"StarCraftRemastered": false
```

Then copy the example config and edit local paths:

```bat
copy plugins\StarCraft116\config\starcraft116_config.example.json plugins\StarCraft116\config\starcraft116_config.json
```

Set `"enabled": true` inside `starcraft116_config.json` only after the paths
are correct.

## Setup Tab

After StarCraft 1.16.1, BWAPI, Chaoslauncher, and bot files are installed
locally, open the LAV `StarCraft 1.16` tab and use `Setup`.

1. Put the install folder in `Install Folder`, for example:

```bat
C:\StarCraft116
```

2. Click `Scan Folder`.
3. Confirm that `StarCraft.exe`, `Chaoslauncher.exe`, `bwapi-data`, and
   `bwapi-data\AI\*.dll` are detected.
4. Click `Generate Config`.
5. Go back to `Launch` and click `Validate Paths`.

## Launch Model

Each profile can launch one or more local programs:

- `start_chaoslauncher`: starts Chaoslauncher or a compatible BWAPI launcher.
- `start_starcraft`: starts a StarCraft 1.16 executable directly.
- `start_bot_process`: starts a standalone BWAPI client bot process.

For classic DLL bots, `bot_binary_path` is a validation/status field. The DLL
is still loaded by your BWAPI/Chaoslauncher setup, not by LAV.

## Game Event JSONL

LAV can also watch an append-only JSONL file for in-game BWAPI events:

```json
"game_events_enabled": true,
"game_events_path": "logs\\starcraft116_game_events.jsonl",
"game_events_poll_interval_sec": 1.0,
"game_events_reaction_cooldown_sec": 8.0
```

Each line should be one JSON object. Existing lines are skipped when the watcher
starts; newly appended complete lines can trigger short OpenAI/TTS reactions.

Example line:

```json
{"event_type":"enemy_spotted","summary":"Enemy Zergling spotted near natural.","frame":3270,"resources":{"minerals":180,"gas":0,"supply":"12/17"}}
```

Useful fields include `event_type`, `summary`, `frame`, `game_time_seconds`,
`resources`, `units`, `combat`, `build_order`, `production`, `supply`,
`workers`, `army`, `enemy`, and `scouting`.

## Monster / BWAPI 4.2.0

Monster is an EXE BWAPI client bot, not a BWAPI AIModule DLL like Stardust.
Use BWAPI 4.2.0 for the Monster profile.

1. Install BWAPI 4.2.0 from the official release page:
   https://github.com/bwapi/bwapi/releases/tag/v4.2.0

   Use the installer asset:

   ```text
   BWAPI_Setup.VS.15.7.3.exe
   ```

2. Download Monster from SSCAIT:
   https://sscaitournament.com/index.php?action=botDetails&bot=Monster

   Binary download:
   https://sscaitournament.com/bot_binary.php?bot=Monster

3. Use a patched `Monster.exe` whose internal paths were forcibly adjusted
   with ChatGPT so it can recognize `sc.dat` and `fp.dat` in the LAV
   Monster/StarCraft layout. Keep a backup of the original `Monster.exe`.

4. Copy `run_monster_robust_log.bat` from this plugin folder into the folder
   that contains `Monster.exe`:

   ```bat
   copy C:\Vtuber_Souorce_Code\LAV_v0.2\plugins\StarCraft116\run_monster_robust_log.bat C:\Vtuber_Souorce_Code\StarCraft_1.16\Monster\run_monster_robust_log.bat
   ```

   The copied `.bat` must sit beside `Monster.exe`. It writes
   `monster_log.txt` and restarts `Monster.exe` after disconnects so the bot
   can wait for the next BWAPI game.

5. Back up the original StarCraft-side BWAPI DLL, then install the LAV proxy:

   ```bat
   copy C:\Vtuber_Souorce_Code\StarCraft_1.16\StarCraft\bwapi-data\BWAPI.dll C:\Vtuber_Souorce_Code\StarCraft_1.16\StarCraft\bwapi-data\BWAPI_real.dll
   copy C:\Vtuber_Souorce_Code\LAV_v0.2\plugins\StarCraft116\BWAPI.dll C:\Vtuber_Souorce_Code\StarCraft_1.16\StarCraft\bwapi-data\BWAPI.dll
   ```

`plugins\StarCraft116\BWAPI.dll` is the LAV proxy DLL. It loads
`BWAPI_real.dll` internally and writes Monster game-state events to
`bwapi_proxy_events.jsonl` under StarCraft's `bwapi-data` folder.

## BWAPI Event Exporter

For exact in-game commentary, build the BWAPI AIModule proxy in:

```bat
plugins\StarCraft116\bwapi_event_exporter
```

The exporter DLL is loaded by BWAPI instead of `Stardust.dll`, then it loads
`Stardust.dll` internally and forwards all callbacks. This lets Stardust keep
playing while LAV receives automatic JSONL events.

Build:

```bat
cd /d C:\Vtuber_Souorce_Code\LAV_v0.2\plugins\StarCraft116\bwapi_event_exporter
msbuild LAVEventExporter.vcxproj /p:Configuration=Release /p:Platform=Win32 /p:BWAPIReleaseDir=C:\Vtuber_Souorce_Code\StarCraft_1.16\BWAPI\Release_Binary
```

The BWAPI 4.4.0 project may require the Visual Studio C++ `v141_xp` Win32
toolset. If MSBuild reports `MSB8020`, install that toolset or retarget the
BWAPI project to an installed Win32 C++ toolset.

Install:

```bat
copy bin\Release\LAVEventExporter.dll C:\Vtuber_Souorce_Code\StarCraft_1.16\StarCraft\bwapi-data\AI\LAVEventExporter.dll
copy LAVEventExporter.ini.example C:\Vtuber_Souorce_Code\StarCraft_1.16\StarCraft\bwapi-data\AI\LAVEventExporter.ini
```

Set `bwapi-data\bwapi.ini`:

```ini
ai     = bwapi-data/AI/LAVEventExporter.dll
ai_dbg = bwapi-data/AI/LAVEventExporter.dll
```

Set the plugin config flag when this path is intentionally enabled:

```json
"bwapi_event_exporter_enabled": true
```
