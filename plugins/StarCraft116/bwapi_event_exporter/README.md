# LAV BWAPI Event Exporter

<!--20260703_kpopmodder: Documents the StarCraft 1.16 BWAPI JSONL exporter proxy.-->

`LAVEventExporter.dll` is a BWAPI AIModule proxy. BWAPI loads this DLL, this
DLL loads the real bot DLL such as `Stardust.dll`, forwards every BWAPI
callback to the bot, and appends structured game events to JSONL for LAV.

This keeps the original bot binary unchanged.

## Build

Open a Visual Studio Developer Command Prompt that can build Win32 C++ projects,
then run:

```bat
cd /d C:\Vtuber_Souorce_Code\LAVI\plugins\StarCraft116\bwapi_event_exporter
msbuild LAVEventExporter.vcxproj /p:Configuration=Release /p:Platform=Win32 /p:BWAPIReleaseDir=C:\Vtuber_Souorce_Code\StarCraft_1.16\BWAPI\Release_Binary
```

The bundled BWAPI project targets `v141_xp`. If MSBuild reports `MSB8020`,
install the matching Visual Studio C++ v141 XP toolset or retarget the BWAPI
projects to an installed Win32 C++ toolset.

The output DLL is:

```bat
bin\Release\LAVEventExporter.dll
```

## Install

Copy the DLL and config next to `Stardust.dll`:

```bat
copy bin\Release\LAVEventExporter.dll C:\Vtuber_Souorce_Code\StarCraft_1.16\StarCraft\bwapi-data\AI\LAVEventExporter.dll
copy LAVEventExporter.ini.example C:\Vtuber_Souorce_Code\StarCraft_1.16\StarCraft\bwapi-data\AI\LAVEventExporter.ini
```

Edit `LAVEventExporter.ini` if needed:

```ini
wrapped_ai=Stardust.dll
events_path=C:\Vtuber_Souorce_Code\LAVI\logs\starcraft116_game_events.jsonl
snapshot_interval_frames=144
combat_cooldown_frames=96
supply_block_cooldown_frames=240
```

Then point `bwapi-data\bwapi.ini` at the proxy:

```ini
ai     = bwapi-data/AI/LAVEventExporter.dll
ai_dbg = bwapi-data/AI/LAVEventExporter.dll
```

The proxy loads `Stardust.dll` through `wrapped_ai`, so Stardust should still
control units while LAV receives event lines.

## Event Types

Initial events include:

- `game_started`
- `game_ended`
- `state_snapshot`
- `enemy_spotted`
- `unit_created`
- `building_started`
- `unit_completed`
- `building_completed`
- `unit_destroyed`
- `unit_morphed`
- `combat_started`
- `supply_blocked`

Each line uses the `lav_starcraft116_bwapi_event_v1` schema.
