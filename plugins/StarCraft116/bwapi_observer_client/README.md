# LAV BWAPI Observer Client

`LAVBWAPIObserverClient.exe` is a standalone BWAPI 4.2.0 client for the
Monster profile.

It connects beside `Monster.exe`, reads BWAPI game state without issuing unit
commands, and writes JSONL events using the same schema as `LAVEventExporter`.
Monster console output should remain a diagnostic log only.

Build from a Visual Studio developer command prompt:

```bat
msbuild plugins\StarCraft116\bwapi_observer_client\LAVBWAPIObserverClient.vcxproj /p:Configuration=Release /p:Platform=Win32
```

Runtime example:

```bat
plugins\StarCraft116\bwapi_observer_client\bin\Release\LAVBWAPIObserverClient.exe --events-path C:\Vtuber_Souorce_Code\LAV_v0.2\logs\starcraft116_game_events.jsonl
```

Useful arguments:

```text
--events-path <path>
--snapshot-frames <frames>
--combat-cooldown-frames <frames>
--supply-block-cooldown-frames <frames>
--complete-map-info 0|1
```
