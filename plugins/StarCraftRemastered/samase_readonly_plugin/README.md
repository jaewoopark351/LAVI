# LAV Samase Read-Only Plugin

<!-- #20260701_kpopmodder: Documents the minimal Samase plugin heartbeat writer. -->

This is the first native Samase plugin for the LAV StarCraft Remastered bridge.

It writes a read-only JSON heartbeat to `LAV_SAMASE_STATE_PATH`. It does not
issue game commands, automate input, hook Battle.net, or expose SAIDA control.

The DLL exports both:

- `Initialize`: used by Samase `SAMASE_MORE_DLLS`; writes a load heartbeat.
- `samase_plugin_init`: used by the Samase plugin API path; hooks game-loop heartbeat when that loader path is available.
- `lav_samase_readonly_write_test_state`: manual export for loader diagnostics.

Build:

```bat
cd /d C:\Vtuber_Souorce_Code\LAV_v0.2
cargo build --manifest-path plugins\StarCraftRemastered\samase_readonly_plugin\Cargo.toml --target i686-pc-windows-msvc --release
```

Output:

```text
plugins\StarCraftRemastered\samase_readonly_plugin\target\i686-pc-windows-msvc\release\lav_samase_readonly_plugin.dll
```

Runtime environment:

```bat
set LAV_SAMASE_STATE_PATH=C:\Vtuber_Souorce_Code\LAV_v0.2\logs\starcraft_samase_readonly_state.json
set LAV_SAMASE_STATE_EVERY_N_FRAMES=8
set LAV_SAMASE_HEARTBEAT_INTERVAL_MS=1000
set SAMASE_MORE_DLLS=C:\Vtuber_Souorce_Code\LAV_v0.2\plugins\StarCraftRemastered\samase_readonly_plugin\target\i686-pc-windows-msvc\release\lav_samase_readonly_plugin.dll
```

The plugin currently proves that Samase can load a LAV DLL and write live
heartbeat state. The `Initialize` path is now InitializeBridge v3, the official
`SAMASE_MORE_DLLS` entry for this bridge. It writes `loader=samase_more_dll_thread`
with a synthetic heartbeat counter plus `bridge.schema=lav_initialize_bridge_v3`,
`bridge.compat_schema=lav_initialize_bridge_v2`, and
`bridge.legacy_compat_schema=lav_initialize_bridge_v1`.
The bridge diagnostics include the current process id, pointer width, loaded
module list, StarCraft/client SDK/Samase temp DLL/bridge module base addresses,
module fingerprints, PE section maps, and verified section-bounded scan ranges.
InitializeBridge v3 also includes `bridge.in_game_detector` v1. The detector uses
`VirtualQuery` and a tiny allowlist of legacy Samase shim primitive addresses to
read only fixed 1-byte/4-byte values for the first `in_game` gate. It does not
dereference unit pointers, resource pointers, or arbitrary game memory.

`bridge.offset_discovery` v2 performs a bounded read-only scan inside the same
verified section ranges. It scans only small `.data` windows, records candidate
`small_u32_nonzero`, `bool_u8_true`, and `readable_pointer_u32` offsets, suppresses
zero-valued primitive noise, writes 4KB `window_profiles` hashes/counts for later
menu-vs-game comparison, and never follows a candidate pointer. It also keeps
`compat_schema=lav_scr_offset_discovery_v1` for older local probes. Set
`LAV_SAMASE_OFFSET_DISCOVERY=0` to disable this local discovery pass.

After running the generated `control_more_dll` script, summarize the large state
JSON with:

```bat
cd /d C:\Vtuber_Souorce_Code\LAV_v0.2
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --offset-summary
```

To capture and compare a menu snapshot with a later in-game snapshot:

```bat
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --save-state menu
rem Enter a single-player game, wait a few seconds, then run:
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --save-state ingame
rem Stay in the same StarCraft process, wait a few seconds, then run:
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --save-state ingame_later
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --focused-byte-check logs\samase_plugin_loader_probe\states\menu.json logs\samase_plugin_loader_probe\states\ingame.json
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --focused-u32-candidates logs\samase_plugin_loader_probe\states\menu.json logs\samase_plugin_loader_probe\states\ingame.json
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --focused-u32-stability logs\samase_plugin_loader_probe\states\menu.json logs\samase_plugin_loader_probe\states\ingame.json logs\samase_plugin_loader_probe\states\ingame_later.json
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --focused-u32-watchlist logs\samase_plugin_loader_probe\states\menu.json logs\samase_plugin_loader_probe\states\ingame.json logs\samase_plugin_loader_probe\states\ingame_later.json --watchlist-out logs\samase_plugin_loader_probe\focused_u32_watchlist.json
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --compare-offset-discovery logs\samase_plugin_loader_probe\states\menu.json logs\samase_plugin_loader_probe\states\ingame.json
```

If both comparison headers still show `in_game=False`, the diff is only menu or
heartbeat noise and should not be treated as a real gameplay-state candidate.
If the comparison headers show different `pid=` values, pointer-value changes are
usually process/heap noise; capture both snapshots from the same StarCraft launch.
In window diffs, `memory_changed` is the useful signal. `pointer_plausibility_changed`
means only the read-only pointer plausibility count moved while the window hash
and scalar counts stayed the same. The printed `score=` ranks memory-changed
windows so the strongest candidate appears first.
`focused_window_drilldown` v1 further splits the strongest observed StarCraft
`.data` windows into 256-byte chunks; use `Samase focused chunk diff` to choose
the next byte/u32-level probe range. Focused chunks include local `bytes_hex`
copies so the Python probe can print `bytes_changed`, `byte_samples`, and
`u32_samples` without dereferencing pointers. If `--focused-byte-check` prints
`status=missing_focused_bytes`, close StarCraft and launch it again through the
generated `run_control_more_dll.cmd` so Samase loads the rebuilt probe DLL.
Once it prints `status=ready_for_u32_samples`, use `--focused-u32-candidates`
to rank direct u32 value changes by RVA before adding any new read-only offset.
Use `--focused-u32-stability` with `menu`, `ingame`, and `ingame_later` snapshots
to reject cross-process noise; `status=pid_mismatch` means the snapshots were
not captured from one StarCraft launch.
If stability is ready, `--focused-u32-watchlist` writes conservative direct-u32
RVA candidates to a local JSON file for the next read-only watch pass. Pointer-like
values are excluded by default.
The native DLL then looks for that file next to the probe `states` directory and
writes `bridge.focused_u32_watch` on each heartbeat. Set
`LAV_SAMASE_U32_WATCH=0` to disable this pass, or set
`LAV_SAMASE_U32_WATCHLIST_PATH` to point at a specific watchlist JSON.
`bridge.in_game_detector` v2 also uses the same watch values: when at least
three watched u32 values are nonzero, the state JSON reports `game.in_game=true`.
Override that threshold with `LAV_SAMASE_U32_WATCH_ACTIVE_THRESHOLD`.
After launching the rebuilt DLL, summarize the live watch values with:

```bat
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --u32-watch-summary
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --u32-watch-signal
```

Resource discovery v1 is the next read-only step. It still does not dereference
pointers. It compares focused chunk `u32` values against visible UI values that
you record in a small spec file, then ranks likely direct RVAs for minerals, gas,
and supply:

```bat
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --resource-spec-template
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --resource-candidates logs\samase_plugin_loader_probe\resource_probe_spec.json --resource-candidates-out logs\samase_plugin_loader_probe\resource_candidates.json
```

Use at least two in-game snapshots where minerals changes. A constant `gas=0`
field is intentionally not ranked because it produces too many zero-value false
positives. The strongest rows should show `confidence=exact_changed`; those RVAs
are the only ones that should be promoted to the next native read-only resource
watch pass. If the header shows `status=partial_candidates` or
`exact_changed_fields=0`, do not promote any row yet; the current focused scan
surface did not include the live resource field. If it shows
`status=support_only_candidates`, an auxiliary field such as supply matched, but
minerals did not; keep those RVAs as hints only and continue paging until
`required_exact_ready=True` and minerals has an `exact_changed` candidate.

`focused_window_drilldown` v2 keeps the seed RVA windows and adds a bounded
resource-focused `.data` sweep. The sweep reads only full 4KB windows that pass
module bounds, section bounds, writable `.data` eligibility, and `VirtualQuery`
readability checks; it still does not follow pointers. By default it dumps 64
resource windows. Set `LAV_SAMASE_RESOURCE_FOCUSED_SCAN=0` to disable the sweep,
set `LAV_SAMASE_RESOURCE_FOCUSED_WINDOWS` to change the local page size, or set
`LAV_SAMASE_RESOURCE_FOCUSED_START_WINDOW` to scan later `.data` pages. If
resource discovery reports `partial_candidates`, rebuild and relaunch the DLL
with the next start window, for example:

```bat
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --plugin-dll plugins\StarCraftRemastered\samase_readonly_plugin\target-resource-page-v2\i686-pc-windows-msvc\release\lav_samase_readonly_plugin.dll --samase-exe "C:\Program Files (x86)\StarCraft\x86\samase-0.8.31.exe" --starcraft-x86-dir "C:\Program Files (x86)\StarCraft\x86" --resource-focused-start-window 64 --resource-focused-windows 64
```

Then capture fresh `game_start`, `after_mining`, and `after_spending` states from
one StarCraft process and rerun `--resource-candidates`.

The `samase_plugin_init` path still writes `loader=samase_plugin_api_init` when
the real plugin API loader calls the init entrypoint, then `loader=samase_plugin_api`
once the game-loop hook fires. Minerals, gas, supply, map, and unit parsing are
the next incremental surface once either PluginApi or a direct read-only surface
is confirmed.

To prepare mod-folder placement candidates for the real plugin API path:

```bat
cd /d C:\Vtuber_Souorce_Code\LAV_v0.2
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --plugin-dll plugins\StarCraftRemastered\samase_readonly_plugin\target\i686-pc-windows-msvc\release\lav_samase_readonly_plugin.dll --samase-exe "C:\Program Files (x86)\StarCraft\x86\samase-0.8.31.exe" --starcraft-x86-dir "C:\Program Files (x86)\StarCraft\x86"
```

Run one generated `logs\samase_plugin_loader_probe\run\run_*.cmd` file at a
time, then check:

```bat
venv\Scripts\python.exe -m plugins.StarCraftRemastered.tools.samase_plugin_loader_probe --check
```

After the direct DLL placement scripts, the generated `special_files_*` scripts
probe Samase's `samase/special_files` archive metadata path.
