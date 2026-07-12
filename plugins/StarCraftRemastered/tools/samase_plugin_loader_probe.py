# #20260701_kpopmodder: Prepares safe Samase plugin API loader path probes.
# import argparse
# import json
# import shutil
# import subprocess
# from dataclasses import dataclass
# from typing import Mapping, Sequence
# from pathlib import Path

# from plugins.StarCraftRemastered.starcraft_config import StarCraftConfig


# PLUGIN_DLL_NAME = "lav_samase_readonly_plugin.dll"
# DISCOVERY_CANDIDATE_KINDS = (
#     "small_u32_nonzero",
#     "bool_u8_true",
#     "readable_pointer_u32",
# )
# DISCOVERY_WINDOW_PROFILE_FIELDS = (
#     "fnv1a64",
#     "nonzero_bytes",
#     "small_u32_nonzero_count",
#     "bool_u8_true_count",
#     "readable_pointer_u32_count",
# )
# DISCOVERY_WINDOW_CONTENT_FIELDS = (
#     "fnv1a64",
#     "nonzero_bytes",
#     "small_u32_nonzero_count",
#     "bool_u8_true_count",
# )
# DISCOVERY_WINDOW_DYNAMIC_FIELDS = (
#     "readable_pointer_u32_count",
# )
# MAX_PRINTED_DISCOVERY_DIFFS = 20
# MAX_PRINTED_BYTE_DIFFS_PER_CHUNK = 8
# MAX_PRINTED_U32_DIFFS_PER_CHUNK = 6
# MAX_PRINTED_FOCUSED_U32_CANDIDATES = 30
# MAX_PRINTED_FOCUSED_U32_STABILITY = 30
# MAX_FOCUSED_U32_WATCHLIST_CANDIDATES = 16
# MAX_RESOURCE_CANDIDATES = 20
# RESOURCE_OBSERVATION_FIELDS = (
#     "minerals",
#     "gas",
#     "supply_used",
#     "supply_total",
# )
# RESOURCE_REQUIRED_EXACT_FIELDS = (
#     "minerals",
# )
# WATCHLIST_VALUE_KINDS = {
#     "small_u32",
#     "byte_sized_u32",
#     "scalar_u32",
#     "large_u32",
# }


# @dataclass(frozen=True)
# class ProbeCandidate:
#     candidate_id: str
#     description: str
#     dll_relative_path: str = ""
#     uses_more_dll: bool = False
#     extra_args: Sequence[str] = ()
#     text_files: Mapping[str, str] = None
#     binary_files: Mapping[str, bytes] = None


# DEFAULT_CANDIDATES = (
#     ProbeCandidate(
#         "control_more_dll",
#         "Control path: SAMASE_MORE_DLLS should call Initialize only.",
#         uses_more_dll=True,
#     ),
#     ProbeCandidate(
#         "root_original_name",
#         "Mod archive root with the plugin DLL original file name.",
#         PLUGIN_DLL_NAME,
#     ),
#     ProbeCandidate(
#         "root_plugin_name",
#         "Mod archive root with a generic plugin.dll file name.",
#         "plugin.dll",
#     ),
#     ProbeCandidate(
#         "plugins_original_name",
#         "Top-level plugins folder with the plugin DLL original file name.",
#         f"plugins/{PLUGIN_DLL_NAME}",
#     ),
#     ProbeCandidate(
#         "samase_original_name",
#         "samase folder with the plugin DLL original file name.",
#         f"samase/{PLUGIN_DLL_NAME}",
#     ),
#     ProbeCandidate(
#         "samase_plugin_name",
#         "samase folder with a generic plugin.dll file name.",
#         "samase/plugin.dll",
#     ),
#     ProbeCandidate(
#         "samase_plugins_original_name",
#         "samase/plugins folder with the plugin DLL original file name.",
#         f"samase/plugins/{PLUGIN_DLL_NAME}",
#     ),
#     ProbeCandidate(
#         "samase_plugins_plugin_name",
#         "samase/plugins folder with a generic plugin.dll file name.",
#         "samase/plugins/plugin.dll",
#     ),
#     ProbeCandidate(
#         "special_files_root_original_lines",
#         "samase/special_files line list pointing at a root plugin DLL.",
#         PLUGIN_DLL_NAME,
#         text_files={"samase/special_files": f"{PLUGIN_DLL_NAME}\n"},
#     ),
#     ProbeCandidate(
#         "special_files_root_plugin_lines",
#         "samase/special_files line list pointing at root plugin.dll.",
#         "plugin.dll",
#         text_files={"samase/special_files": "plugin.dll\n"},
#     ),
#     ProbeCandidate(
#         "special_files_plugins_original_lines",
#         "samase/special_files line list pointing at plugins/original-name DLL.",
#         f"plugins/{PLUGIN_DLL_NAME}",
#         text_files={"samase/special_files": f"plugins/{PLUGIN_DLL_NAME}\n"},
#     ),
#     ProbeCandidate(
#         "special_files_samase_original_lines",
#         "samase/special_files line list pointing at samase/original-name DLL.",
#         f"samase/{PLUGIN_DLL_NAME}",
#         text_files={"samase/special_files": f"samase/{PLUGIN_DLL_NAME}\n"},
#     ),
#     ProbeCandidate(
#         "special_files_samase_plugins_original_lines",
#         "samase/special_files line list pointing at samase/plugins/original-name DLL.",
#         f"samase/plugins/{PLUGIN_DLL_NAME}",
#         text_files={"samase/special_files": f"samase/plugins/{PLUGIN_DLL_NAME}\n"},
#     ),
#     ProbeCandidate(
#         "special_files_samase_plugins_original_nul",
#         "samase/special_files NUL-separated list for samase/plugins/original-name DLL.",
#         f"samase/plugins/{PLUGIN_DLL_NAME}",
#         binary_files={
#             "samase/special_files": f"samase/plugins/{PLUGIN_DLL_NAME}\0".encode(
#                 "utf-8"
#             )
#         },
#     ),
#     ProbeCandidate(
#         "special_files_samase_plugins_original_json",
#         "samase/special_files JSON list for samase/plugins/original-name DLL.",
#         f"samase/plugins/{PLUGIN_DLL_NAME}",
#         text_files={
#             "samase/special_files": json.dumps(
#                 [f"samase/plugins/{PLUGIN_DLL_NAME}"],
#                 indent=2,
#             )
#             + "\n"
#         },
#     ),
# )


# def project_root():
#     return Path(__file__).resolve().parents[3]


# def default_output_dir():
#     return project_root() / "logs" / "samase_plugin_loader_probe"


# def default_plugin_dll_path(config):
#     configured = config.resolve_path("samase_readonly_plugin_dll_path")
#     if configured:
#         return Path(configured)
#     return (
#         project_root()
#         / "plugins"
#         / "StarCraftRemastered"
#         / "samase_readonly_plugin"
#         / "target"
#         / "i686-pc-windows-msvc"
#         / "release"
#         / PLUGIN_DLL_NAME
#     )


# def normalize_mod_args(value):
#     if value is None:
#         return []
#     if isinstance(value, (list, tuple)):
#         return [str(part) for part in value if str(part).strip()]
#     value = str(value).strip()
#     return [value] if value else []


# def cmd_path(value):
#     return str(Path(value))


# def resolve_for_script(value):
#     return Path(value).resolve()


# def write_candidate_side_files(candidate_dir, candidate):
#     for relative_path, content in (candidate.text_files or {}).items():
#         path = candidate_dir / Path(relative_path)
#         path.parent.mkdir(parents=True, exist_ok=True)
#         path.write_text(content, encoding="utf-8", newline="")

#     for relative_path, content in (candidate.binary_files or {}).items():
#         path = candidate_dir / Path(relative_path)
#         path.parent.mkdir(parents=True, exist_ok=True)
#         path.write_bytes(content)


# def render_run_script(entry):
#     lines = [
#         "@echo off",
#         "setlocal",
#         f"set \"LAV_SAMASE_STATE_PATH={entry['state_path']}\"",
#         f"set \"LAV_SAMASE_STATE_EVERY_N_FRAMES={entry['write_every_n_frames']}\"",
#         f"set \"LAV_SAMASE_HEARTBEAT_INTERVAL_MS={entry['heartbeat_interval_ms']}\"",
#         f"set \"LAV_SAMASE_RESOURCE_FOCUSED_SCAN={entry['resource_focused_scan']}\"",
#         f"set \"LAV_SAMASE_RESOURCE_FOCUSED_START_WINDOW={entry['resource_focused_start_window']}\"",
#         f"set \"LAV_SAMASE_RESOURCE_FOCUSED_WINDOWS={entry['resource_focused_windows']}\"",
#     ]
#     if entry["uses_more_dll"]:
#         lines.append(f"set \"SAMASE_MORE_DLLS={entry['plugin_dll_path']}\"")
#     else:
#         lines.append("set \"SAMASE_MORE_DLLS=\"")

#     lines.extend(
#         [
#             f"cd /d \"{entry['starcraft_x86_dir']}\"",
#             f"echo [LAV Samase probe] candidate={entry['candidate_id']}",
#             subprocess.list2cmdline(entry["command"]),
#             "echo.",
#             "echo [LAV Samase probe] close StarCraft before starting another candidate.",
#             f"if exist \"{entry['state_path']}\" type \"{entry['state_path']}\"",
#             "endlocal",
#             "",
#         ]
#     )
#     return "\n".join(lines)


# def prepare_probe_plan(
#     plugin_dll_path,
#     samase_exe_path,
#     starcraft_x86_dir,
#     out_dir,
#     mod_args=None,
#     write_every_n_frames=8,
#     heartbeat_interval_ms=1000,
#     resource_focused_scan=True,
#     resource_focused_start_window=0,
#     resource_focused_windows=64,
#     candidates=DEFAULT_CANDIDATES,
# ):
#     plugin_dll_path = resolve_for_script(plugin_dll_path)
#     samase_exe_path = resolve_for_script(samase_exe_path)
#     starcraft_x86_dir = resolve_for_script(starcraft_x86_dir)
#     out_dir = resolve_for_script(out_dir)
#     mod_args = normalize_mod_args(mod_args)

#     if not plugin_dll_path.is_file():
#         raise FileNotFoundError(f"Samase plugin DLL does not exist: {plugin_dll_path}")

#     candidates_dir = out_dir / "candidates"
#     states_dir = out_dir / "states"
#     scripts_dir = out_dir / "run"
#     candidates_dir.mkdir(parents=True, exist_ok=True)
#     states_dir.mkdir(parents=True, exist_ok=True)
#     scripts_dir.mkdir(parents=True, exist_ok=True)

#     entries = []
#     for candidate in candidates:
#         candidate_dir = candidates_dir / candidate.candidate_id
#         state_path = states_dir / f"{candidate.candidate_id}.json"
#         candidate_dir.mkdir(parents=True, exist_ok=True)
#         if state_path.is_file():
#             state_path.unlink()

#         copied_dll_path = ""
#         if candidate.dll_relative_path:
#             copied_dll = candidate_dir / Path(candidate.dll_relative_path)
#             copied_dll.parent.mkdir(parents=True, exist_ok=True)
#             shutil.copy2(plugin_dll_path, copied_dll)
#             copied_dll_path = cmd_path(copied_dll)
#         write_candidate_side_files(candidate_dir, candidate)

#         command_args = [cmd_path(samase_exe_path)]
#         if candidate.uses_more_dll:
#             command_args.extend(mod_args)
#         else:
#             command_args.append(cmd_path(candidate_dir))
#         command_args.extend(str(part) for part in candidate.extra_args)
#         command_args.append("--log")

#         run_script = scripts_dir / f"run_{candidate.candidate_id}.cmd"
#         entry = {
#             "candidate_id": candidate.candidate_id,
#             "description": candidate.description,
#             "uses_more_dll": candidate.uses_more_dll,
#             "mod_dir": "" if candidate.uses_more_dll else cmd_path(candidate_dir),
#             "dll_relative_path": candidate.dll_relative_path,
#             "extra_args": [str(part) for part in candidate.extra_args],
#             "text_files": sorted((candidate.text_files or {}).keys()),
#             "binary_files": sorted((candidate.binary_files or {}).keys()),
#             "copied_dll_path": copied_dll_path,
#             "plugin_dll_path": cmd_path(plugin_dll_path),
#             "samase_exe_path": cmd_path(samase_exe_path),
#             "starcraft_x86_dir": cmd_path(starcraft_x86_dir),
#             "state_path": cmd_path(state_path),
#             "run_script": cmd_path(run_script),
#             "command": command_args,
#             "write_every_n_frames": int(write_every_n_frames),
#             "heartbeat_interval_ms": int(heartbeat_interval_ms),
#             "resource_focused_scan": "1" if resource_focused_scan else "0",
#             "resource_focused_start_window": max(
#                 0,
#                 int_value(resource_focused_start_window, 0),
#             ),
#             "resource_focused_windows": max(1, int_value(resource_focused_windows, 64)),
#             "success_loader": "samase_plugin_api",
#             "initialize_only_loader": "samase_more_dll_thread",
#         }
#         run_script.write_text(render_run_script(entry), encoding="utf-8")
#         entries.append(entry)

#     manifest = {
#         "schema": "lav_samase_plugin_loader_probe_v1",
#         "summary": (
#             "Run one candidate script at a time. loader=samase_plugin_api_init "
#             "means Samase called samase_plugin_init(api); "
#             "loader=samase_plugin_api means the game-loop hook also fired. "
#             "loader=samase_more_dll_thread means only Initialize loaded."
#         ),
#         "out_dir": cmd_path(out_dir),
#         "entries": entries,
#     }
#     (out_dir / "probe_manifest.json").write_text(
#         json.dumps(manifest, indent=2),
#         encoding="utf-8",
#     )
#     return manifest


# def classify_state_payload(payload):
#     loader = str(payload.get("loader", "") or "")
#     game = payload.get("game", {})
#     frame_count = game.get("frame_count", 0) if isinstance(game, dict) else 0
#     if loader == "samase_plugin_api":
#         status = "api_loader_confirmed"
#     elif loader == "samase_plugin_api_init":
#         status = "api_init_confirmed"
#     elif loader.startswith("samase_more_dll"):
#         status = "initialize_only"
#     elif loader == "manual_export_test":
#         status = "manual_export_only"
#     elif loader:
#         status = "unknown_loader"
#     else:
#         status = "missing_loader"
#     return {
#         "status": status,
#         "loader": loader,
#         "frame_count": frame_count,
#     }


# def classify_state_file(path):
#     path = Path(path)
#     if not path.is_file():
#         return {
#             "status": "missing_state_file",
#             "loader": "",
#             "frame_count": 0,
#         }
#     try:
#         payload = json.loads(path.read_text(encoding="utf-8"))
#     except Exception as error:
#         return {
#             "status": "invalid_state_json",
#             "loader": "",
#             "frame_count": 0,
#             "error": str(error),
#         }
#     return classify_state_payload(payload)


# def default_control_state_path(out_dir):
#     return Path(out_dir) / "states" / "control_more_dll.json"


# def default_named_state_path(out_dir, label):
#     name = Path(str(label).strip()).name
#     if not name:
#         raise ValueError("state label must not be empty")
#     if not name.lower().endswith(".json"):
#         name = f"{name}.json"
#     return Path(out_dir) / "states" / name


# def load_json_payload(path):
#     with open(path, "r", encoding="utf-8") as file:
#         return json.load(file)


# def state_metadata(payload):
#     game = payload.get("game", {}) if isinstance(payload, dict) else {}
#     bridge = payload.get("bridge", {}) if isinstance(payload, dict) else {}
#     process = bridge.get("process", {}) if isinstance(bridge, dict) else {}
#     snapshot = bridge.get("scr_version_snapshot", {}) if isinstance(bridge, dict) else {}
#     units = payload.get("units", {}) if isinstance(payload, dict) else {}
#     if not isinstance(game, dict):
#         game = {}
#     if not isinstance(bridge, dict):
#         bridge = {}
#     if not isinstance(process, dict):
#         process = {}
#     if not isinstance(snapshot, dict):
#         snapshot = {}
#     if not isinstance(units, dict):
#         units = {}
#     starcraft_module = (
#         snapshot.get("starcraft_module", {})
#         if isinstance(snapshot.get("starcraft_module", {}), dict)
#         else {}
#     )
#     clientsdk_module = (
#         snapshot.get("clientsdk_module", {})
#         if isinstance(snapshot.get("clientsdk_module", {}), dict)
#         else {}
#     )
#     samase_temp_module = (
#         snapshot.get("samase_temp_module", {})
#         if isinstance(snapshot.get("samase_temp_module", {}), dict)
#         else {}
#     )
#     return {
#         "written_at": payload.get("written_at") if isinstance(payload, dict) else None,
#         "loader": payload.get("loader", "") if isinstance(payload, dict) else "",
#         "bridge_loader": bridge.get("loader", ""),
#         "process_pid": process.get("pid"),
#         "in_game": bool(game.get("in_game", False)),
#         "frame_count": game.get("frame_count", 0),
#         "map_name": game.get("map_name", ""),
#         "starcraft_base": starcraft_module.get("base", ""),
#         "clientsdk_base": clientsdk_module.get("base", ""),
#         "samase_temp_name": samase_temp_module.get("name", ""),
#         "samase_temp_base": samase_temp_module.get("base", ""),
#         "my_units": len(units.get("my", [])) if isinstance(units.get("my", []), list) else 0,
#         "enemy_units": len(units.get("enemy", []))
#         if isinstance(units.get("enemy", []), list)
#         else 0,
#         "neutral_units": len(units.get("neutral", []))
#         if isinstance(units.get("neutral", []), list)
#         else 0,
#     }


# def format_state_metadata(label, payload):
#     meta = state_metadata(payload)
#     return (
#         f"[Samase state {label}] "
#         f"written_at={meta['written_at']} "
#         f"pid={meta['process_pid'] or '-'} "
#         f"loader={meta['loader'] or '-'} "
#         f"bridge_loader={meta['bridge_loader'] or '-'} "
#         f"in_game={meta['in_game']} "
#         f"frame={meta['frame_count']} "
#         f"map={meta['map_name'] or '-'} "
#         f"units={meta['my_units']}/{meta['enemy_units']}/{meta['neutral_units']}"
#     )


# def offset_discovery_payload(payload):
#     bridge = payload.get("bridge", {}) if isinstance(payload, dict) else {}
#     if not isinstance(bridge, dict):
#         return {}
#     discovery = bridge.get("offset_discovery", {})
#     return discovery if isinstance(discovery, dict) else {}


# def focused_u32_watch_payload(payload):
#     bridge = payload.get("bridge", {}) if isinstance(payload, dict) else {}
#     if not isinstance(bridge, dict):
#         return {}
#     watch = bridge.get("focused_u32_watch", {})
#     return watch if isinstance(watch, dict) else {}


# def summarize_focused_u32_watch_payload(payload):
#     watch = focused_u32_watch_payload(payload)
#     if not watch:
#         return ["[Samase focused u32 watch] missing bridge.focused_u32_watch"]
#     candidates = watch.get("candidates", [])
#     if not isinstance(candidates, list):
#         candidates = []
#     read_count = sum(1 for row in candidates if isinstance(row, dict) and row.get("ok"))
#     lines = [
#         "[Samase focused u32 watch] "
#         f"schema={watch.get('schema', '-')} "
#         f"status={watch.get('status', '-')} "
#         f"enabled={watch.get('enabled', '-')} "
#         f"loaded={watch.get('loaded_candidates', 0)} "
#         f"printed={watch.get('printed_candidates', len(candidates))} "
#         f"read_count={watch.get('read_count', read_count)} "
#         f"source={watch.get('source_path', '-')}",
#     ]
#     for index, row in enumerate(candidates[:MAX_PRINTED_DISCOVERY_DIFFS], start=1):
#         if not isinstance(row, dict):
#             continue
#         lines.append(
#             "[Samase focused u32 watch] "
#             f"rank={index} "
#             f"status={row.get('status', '-')} "
#             f"rva={row.get('rva', '-')} "
#             f"value={row.get('hex_value', row.get('value', '-'))} "
#             f"confidence={row.get('confidence', '-')} "
#             f"error={row.get('error', '-')}"
#         )
#     return lines


# def focused_u32_watch_rows(payload):
#     watch = focused_u32_watch_payload(payload)
#     candidates = watch.get("candidates", []) if isinstance(watch, dict) else []
#     if not isinstance(candidates, list):
#         return []
#     rows = []
#     for row in candidates:
#         if not isinstance(row, dict) or not row.get("ok"):
#             continue
#         value = row.get("value")
#         if value is None:
#             value = row.get("hex_value", 0)
#         value = int_value(value)
#         rows.append(
#             {
#                 "rva": str(row.get("rva", "")),
#                 "value": value,
#                 "hex_value": row.get("hex_value", f"0x{value:08X}"),
#                 "confidence": str(row.get("confidence", "")),
#                 "status": str(row.get("status", "")),
#             }
#         )
#     return rows


# def focused_u32_watch_signal(payload, active_nonzero_threshold=3):
#     watch = focused_u32_watch_payload(payload)
#     rows = focused_u32_watch_rows(payload)
#     read_count = len(rows)
#     nonzero_rows = [row for row in rows if row["value"] != 0]
#     zero_rows = [row for row in rows if row["value"] == 0]
#     if not watch:
#         status = "missing_watch"
#     elif watch.get("status") != "read":
#         status = "watch_not_read"
#     elif read_count == 0:
#         status = "no_read_values"
#     elif len(nonzero_rows) >= active_nonzero_threshold:
#         status = "active"
#     else:
#         status = "menu_like"
#     return {
#         "status": status,
#         "read_count": read_count,
#         "nonzero_count": len(nonzero_rows),
#         "zero_count": len(zero_rows),
#         "threshold": active_nonzero_threshold,
#         "nonzero_rvas": [row["rva"] for row in nonzero_rows],
#         "zero_rvas": [row["rva"] for row in zero_rows],
#         "rows": rows,
#     }


# def format_focused_u32_watch_signal(signal, label=""):
#     label_text = f" label={label}" if label else ""
#     return (
#         "[Samase focused u32 signal]"
#         f"{label_text} "
#         f"status={signal['status']} "
#         f"read_count={signal['read_count']} "
#         f"nonzero={signal['nonzero_count']} "
#         f"zero={signal['zero_count']} "
#         f"threshold={signal['threshold']} "
#         f"nonzero_rvas={','.join(signal['nonzero_rvas']) or '-'}"
#     )


# def format_focused_u32_watch_transition(before_signal, after_signal, before_label, after_label):
#     before_values = {row["rva"]: row["value"] for row in before_signal["rows"]}
#     after_values = {row["rva"]: row["value"] for row in after_signal["rows"]}
#     changed = [
#         rva
#         for rva in sorted(set(before_values) & set(after_values))
#         if before_values[rva] != after_values[rva]
#     ]
#     samples = ",".join(
#         f"{rva}:0x{before_values[rva]:08X}->0x{after_values[rva]:08X}"
#         for rva in changed[:MAX_PRINTED_U32_DIFFS_PER_CHUNK]
#     )
#     return (
#         "[Samase focused u32 signal] transition "
#         f"{before_label}->{after_label} "
#         f"status={before_signal['status']}->{after_signal['status']} "
#         f"nonzero={before_signal['nonzero_count']}->{after_signal['nonzero_count']} "
#         f"changed={len(changed)} "
#         f"samples={samples or '-'}"
#     )


# def offset_discovery_candidate_counts(group):
#     candidates = group.get("candidates", {}) if isinstance(group, dict) else {}
#     if not isinstance(candidates, dict):
#         return {kind: 0 for kind in DISCOVERY_CANDIDATE_KINDS}
#     return {
#         kind: len(candidates.get(kind, []))
#         if isinstance(candidates.get(kind, []), list)
#         else 0
#         for kind in DISCOVERY_CANDIDATE_KINDS
#     }


# def summarize_offset_discovery_payload(payload):
#     discovery = offset_discovery_payload(payload)
#     if not discovery:
#         return ["[Samase offset discovery] missing bridge.offset_discovery"]

#     lines = [
#         "[Samase offset discovery] "
#         f"schema={discovery.get('schema', '-')} "
#         f"mode={discovery.get('mode', '-')} "
#         f"enabled={discovery.get('enabled', '-')}",
#     ]
#     for group in discovery.get("module_groups", []):
#         if not isinstance(group, dict):
#             continue
#         counts = offset_discovery_candidate_counts(group)
#         counts_text = " ".join(
#             f"{kind}={counts[kind]}" for kind in DISCOVERY_CANDIDATE_KINDS
#         )
#         window_profiles = group.get("window_profiles", [])
#         window_count = len(window_profiles) if isinstance(window_profiles, list) else 0
#         lines.append(
#             "[Samase offset discovery] "
#             f"{group.get('role', '-')} status={group.get('status', '-')} "
#             f"ranges={group.get('ranges_scanned', 0)} "
#             f"bytes={group.get('bytes_scanned', 0)} "
#             f"windows={window_count} "
#             f"{counts_text}"
#         )
#     return lines


# def discovery_candidate_key(group, kind, candidate):
#     return (
#         str(group.get("role", "")),
#         str(candidate.get("module_name", "")),
#         str(candidate.get("section_name", "")),
#         str(kind),
#         str(candidate.get("rva", "")),
#     )


# def flatten_offset_discovery_candidates(payload):
#     discovery = offset_discovery_payload(payload)
#     flattened = {}
#     for group in discovery.get("module_groups", []):
#         if not isinstance(group, dict):
#             continue
#         candidates = group.get("candidates", {})
#         if not isinstance(candidates, dict):
#             continue
#         for kind in DISCOVERY_CANDIDATE_KINDS:
#             for candidate in candidates.get(kind, []):
#                 if not isinstance(candidate, dict):
#                     continue
#                 flattened[discovery_candidate_key(group, kind, candidate)] = {
#                     "value": candidate.get("value"),
#                     "hex_value": candidate.get("hex_value", ""),
#                     "address": candidate.get("address", ""),
#                     "range_offset": candidate.get("range_offset", 0),
#                 }
#     return flattened


# def discovery_key_text(key):
#     role, module_name, section_name, kind, rva = key
#     return f"{role} {module_name} {section_name} {kind} rva={rva}"


# def window_profile_key(group, profile):
#     return (
#         str(group.get("role", "")),
#         str(profile.get("module_name", "")),
#         str(profile.get("section_name", "")),
#         str(profile.get("rva_start", "")),
#     )


# def flatten_offset_window_profiles(payload):
#     discovery = offset_discovery_payload(payload)
#     flattened = {}
#     for group in discovery.get("module_groups", []):
#         if not isinstance(group, dict):
#             continue
#         profiles = group.get("window_profiles", [])
#         if not isinstance(profiles, list):
#             continue
#         for profile in profiles:
#             if not isinstance(profile, dict):
#                 continue
#             flattened[window_profile_key(group, profile)] = {
#                 field: profile.get(field) for field in DISCOVERY_WINDOW_PROFILE_FIELDS
#             }
#     return flattened


# def window_profile_key_text(key):
#     role, module_name, section_name, rva_start = key
#     return f"{role} {module_name} {section_name} window_rva={rva_start}"


# def focused_chunk_key(group, window, chunk):
#     return (
#         str(group.get("role", "")),
#         str(chunk.get("module_name", window.get("module_name", ""))),
#         str(chunk.get("section_name", window.get("section_name", ""))),
#         str(chunk.get("window_rva_start", window.get("rva_start", ""))),
#         str(chunk.get("chunk_rva_start", "")),
#     )


# def flatten_focused_chunks(payload):
#     discovery = offset_discovery_payload(payload)
#     flattened = {}
#     for group in discovery.get("module_groups", []):
#         if not isinstance(group, dict):
#             continue
#         windows = group.get("focused_window_drilldown", [])
#         if not isinstance(windows, list):
#             continue
#         for window in windows:
#             if not isinstance(window, dict):
#                 continue
#             chunks = window.get("chunks", [])
#             if not isinstance(chunks, list):
#                 continue
#             for chunk in chunks:
#                 if not isinstance(chunk, dict):
#                     continue
#                 flattened[focused_chunk_key(group, window, chunk)] = {
#                     field: chunk.get(field) for field in DISCOVERY_WINDOW_PROFILE_FIELDS
#                 }
#                 flattened[focused_chunk_key(group, window, chunk)]["bytes_hex"] = chunk.get(
#                     "bytes_hex",
#                     "",
#                 )
#     return flattened


# def focused_chunks_have_bytes_hex(payload):
#     return any(
#         bool(profile.get("bytes_hex"))
#         for profile in flatten_focused_chunks(payload).values()
#     )


# def focused_chunk_key_text(key):
#     role, module_name, section_name, window_rva_start, chunk_rva_start = key
#     return (
#         f"{role} {module_name} {section_name} "
#         f"window_rva={window_rva_start} chunk_rva={chunk_rva_start}"
#     )


# def int_value(value, default=0):
#     if isinstance(value, bool):
#         return int(value)
#     if isinstance(value, int):
#         return value
#     try:
#         return int(str(value), 0)
#     except (TypeError, ValueError):
#         return default


# def window_field_delta(before, after, field):
#     return int_value(after.get(field)) - int_value(before.get(field))


# def window_change_score(before, after):
#     score = 0
#     if before.get("fnv1a64") != after.get("fnv1a64"):
#         score += 1
#     score += abs(window_field_delta(before, after, "nonzero_bytes"))
#     score += 8 * abs(window_field_delta(before, after, "small_u32_nonzero_count"))
#     score += 4 * abs(window_field_delta(before, after, "bool_u8_true_count"))
#     return score


# def decode_hex_bytes(value):
#     value = str(value or "").strip()
#     if not value:
#         return b""
#     if len(value) % 2 != 0:
#         return b""
#     try:
#         return bytes.fromhex(value)
#     except ValueError:
#         return b""


# def focused_chunk_byte_diffs(before, after, limit=MAX_PRINTED_BYTE_DIFFS_PER_CHUNK):
#     before_bytes = decode_hex_bytes(before.get("bytes_hex", ""))
#     after_bytes = decode_hex_bytes(after.get("bytes_hex", ""))
#     if not before_bytes or not after_bytes:
#         return {
#             "changed_count": 0,
#             "items": [],
#         }
#     changed_offsets = [
#         offset
#         for offset, (before_byte, after_byte) in enumerate(zip(before_bytes, after_bytes))
#         if before_byte != after_byte
#     ]
#     return {
#         "changed_count": len(changed_offsets),
#         "items": [
#             {
#                 "offset": offset,
#                 "before": before_bytes[offset],
#                 "after": after_bytes[offset],
#             }
#             for offset in changed_offsets[:limit]
#         ],
#     }


# def focused_chunk_u32_diffs(before, after, limit=MAX_PRINTED_U32_DIFFS_PER_CHUNK):
#     before_bytes = decode_hex_bytes(before.get("bytes_hex", ""))
#     after_bytes = decode_hex_bytes(after.get("bytes_hex", ""))
#     if len(before_bytes) < 4 or len(after_bytes) < 4:
#         return {
#             "changed_count": 0,
#             "items": [],
#         }
#     max_len = min(len(before_bytes), len(after_bytes))
#     changed = []
#     for offset in range(0, max_len - 3, 4):
#         before_value = int.from_bytes(before_bytes[offset : offset + 4], "little")
#         after_value = int.from_bytes(after_bytes[offset : offset + 4], "little")
#         if before_value != after_value:
#             changed.append(
#                 {
#                     "offset": offset,
#                     "before": before_value,
#                     "after": after_value,
#                 }
#             )
#     return {
#         "changed_count": len(changed),
#         "items": changed[:limit],
#     }


# def focused_byte_probe_status(before_payload, after_payload):
#     before_chunks = flatten_focused_chunks(before_payload)
#     after_chunks = flatten_focused_chunks(after_payload)
#     focused_result = compare_focused_chunks(before_payload, after_payload)
#     changed_with_bytes = [
#         (key, before, after)
#         for key, before, after in focused_result["changed"]
#         if before.get("bytes_hex") and after.get("bytes_hex")
#     ]
#     before_bytes_chunks = sum(
#         1 for profile in before_chunks.values() if profile.get("bytes_hex")
#     )
#     after_bytes_chunks = sum(
#         1 for profile in after_chunks.values() if profile.get("bytes_hex")
#     )
#     u32_changed = sum(
#         focused_chunk_u32_diffs(before, after)["changed_count"]
#         for _, before, after in changed_with_bytes
#     )
#     if not before_chunks or not after_chunks:
#         status = "missing_focused_chunks"
#     elif focused_result["changed"] and not changed_with_bytes:
#         status = "missing_focused_bytes"
#     elif focused_result["changed"] and changed_with_bytes:
#         status = "ready_for_u32_samples"
#     elif before_bytes_chunks and after_bytes_chunks:
#         status = "bytes_ready_no_memory_change"
#     else:
#         status = "no_focused_memory_change"
#     return {
#         "status": status,
#         "before_chunks": len(before_chunks),
#         "after_chunks": len(after_chunks),
#         "before_bytes_chunks": before_bytes_chunks,
#         "after_bytes_chunks": after_bytes_chunks,
#         "memory_changed": len(focused_result["changed"]),
#         "changed_with_bytes": len(changed_with_bytes),
#         "pointer_plausibility_changed": len(
#             focused_result["pointer_plausibility_changed"]
#         ),
#         "u32_changed": u32_changed,
#         "top_changed": focused_result["changed"][0]
#         if focused_result["changed"]
#         else None,
#     }


# def format_focused_byte_probe_status(status):
#     lines = [
#         "[Samase focused byte check] "
#         f"status={status['status']} "
#         f"before_chunks={status['before_chunks']} "
#         f"after_chunks={status['after_chunks']} "
#         f"before_bytes={status['before_bytes_chunks']} "
#         f"after_bytes={status['after_bytes_chunks']} "
#         f"memory_changed={status['memory_changed']} "
#         f"changed_with_bytes={status['changed_with_bytes']} "
#         f"u32_changed={status['u32_changed']} "
#         f"pointer_plausibility_changed={status['pointer_plausibility_changed']}",
#     ]
#     top_changed = status.get("top_changed")
#     if top_changed:
#         key, before, after = top_changed
#         byte_diff = focused_chunk_byte_diffs(before, after)
#         u32_diff = focused_chunk_u32_diffs(before, after)
#         lines.append(
#             "[Samase focused byte check] top_changed "
#             f"{focused_chunk_key_text(key)} "
#             f"score={window_change_score(before, after)} "
#             f"bytes_changed={byte_diff['changed_count']} "
#             f"byte_samples={format_byte_diff_items(byte_diff)} "
#             f"u32_changed={u32_diff['changed_count']} "
#             f"u32_samples={format_u32_diff_items(u32_diff)}"
#         )
#     if status["status"] == "missing_focused_bytes":
#         lines.append(
#             "[Samase focused byte check] action=close StarCraft, rebuild/run the "
#             "target-probe DLL through run_control_more_dll.cmd, then recapture "
#             "menu and ingame states"
#         )
#     elif status["status"] == "ready_for_u32_samples":
#         lines.append(
#             "[Samase focused byte check] action=proceed to focused u32 candidate "
#             "ranking on the printed chunk RVAs"
#         )
#     return lines


# def classify_u32_candidate_value(value):
#     value = int_value(value)
#     if value == 0:
#         return "zero"
#     if 1 <= value <= 12:
#         return "small_u32"
#     if value <= 255:
#         return "byte_sized_u32"
#     if value <= 10000:
#         return "scalar_u32"
#     if value >= 0x01000000:
#         return "large_or_pointer_like_u32"
#     return "large_u32"


# def focused_u32_candidate_score(before_value, after_value, chunk_score):
#     score = int_value(chunk_score)
#     if before_value == 0 and after_value != 0:
#         score += 24
#     elif before_value != after_value:
#         score += 4

#     if 1 <= after_value <= 12:
#         score += 40
#     elif 13 <= after_value <= 255:
#         score += 28
#     elif 256 <= after_value <= 10000:
#         score += 18
#     elif after_value >= 0x01000000:
#         score -= 8
#     return score


# def focused_u32_candidate_rows(before_payload, after_payload):
#     rows = []
#     focused_result = compare_focused_chunks(before_payload, after_payload)
#     for key, before, after in focused_result["changed"]:
#         if not before.get("bytes_hex") or not after.get("bytes_hex"):
#             continue
#         role, module_name, section_name, window_rva_start, chunk_rva_start = key
#         chunk_rva = int_value(chunk_rva_start)
#         chunk_score = window_change_score(before, after)
#         u32_diff = focused_chunk_u32_diffs(before, after, limit=None)
#         for item in u32_diff["items"]:
#             offset = int_value(item.get("offset"))
#             before_value = int_value(item.get("before"))
#             after_value = int_value(item.get("after"))
#             rows.append(
#                 {
#                     "role": role,
#                     "module_name": module_name,
#                     "section_name": section_name,
#                     "window_rva": window_rva_start,
#                     "chunk_rva": chunk_rva_start,
#                     "offset": offset,
#                     "rva": chunk_rva + offset,
#                     "before": before_value,
#                     "after": after_value,
#                     "delta": after_value - before_value,
#                     "chunk_score": chunk_score,
#                     "score": focused_u32_candidate_score(
#                         before_value,
#                         after_value,
#                         chunk_score,
#                     ),
#                     "value_kind": classify_u32_candidate_value(after_value),
#                 }
#             )
#     rows.sort(
#         key=lambda row: (
#             -row["score"],
#             -row["chunk_score"],
#             row["role"],
#             row["module_name"],
#             row["rva"],
#         )
#     )
#     return rows


# def format_focused_u32_candidate(row, rank):
#     return (
#         "[Samase focused u32 candidate] "
#         f"rank={rank} "
#         f"score={row['score']} "
#         f"chunk_score={row['chunk_score']} "
#         f"{row['role']} {row['module_name']} {row['section_name']} "
#         f"window_rva={row['window_rva']} "
#         f"chunk_rva={row['chunk_rva']} "
#         f"offset=+0x{row['offset']:02X} "
#         f"rva=0x{row['rva']:08X} "
#         f"before=0x{row['before']:08X} "
#         f"after=0x{row['after']:08X} "
#         f"after_dec={row['after']} "
#         f"delta={row['delta']} "
#         f"kind={row['value_kind']}"
#     )


# def focused_u32_row_key(row):
#     return (
#         row["role"],
#         row["module_name"],
#         row["section_name"],
#         row["rva"],
#     )


# def direction_sign(delta):
#     if delta > 0:
#         return 1
#     if delta < 0:
#         return -1
#     return 0


# def focused_u32_stability_report(payloads):
#     metas = [state_metadata(payload) for payload in payloads]
#     pids = [meta["process_pid"] for meta in metas]
#     non_empty_pids = [pid for pid in pids if pid]
#     same_pid = bool(non_empty_pids) and len(set(non_empty_pids)) == 1
#     transitions = []
#     buckets = {}

#     for index in range(max(0, len(payloads) - 1)):
#         before_payload = payloads[index]
#         after_payload = payloads[index + 1]
#         byte_status = focused_byte_probe_status(before_payload, after_payload)
#         rows = focused_u32_candidate_rows(before_payload, after_payload)
#         transitions.append(
#             {
#                 "index": index,
#                 "before_pid": pids[index],
#                 "after_pid": pids[index + 1],
#                 "byte_status": byte_status["status"],
#                 "u32_changed": byte_status["u32_changed"],
#                 "candidate_count": len(rows),
#             }
#         )
#         for row in rows:
#             key = focused_u32_row_key(row)
#             bucket = buckets.setdefault(
#                 key,
#                 {
#                     "role": row["role"],
#                     "module_name": row["module_name"],
#                     "section_name": row["section_name"],
#                     "rva": row["rva"],
#                     "occurrences": 0,
#                     "transition_indexes": [],
#                     "before_values": [],
#                     "after_values": [],
#                     "deltas": [],
#                     "directions": [],
#                     "value_kinds": [],
#                     "total_score": 0,
#                     "max_score": 0,
#                     "max_chunk_score": 0,
#                 },
#             )
#             bucket["occurrences"] += 1
#             bucket["transition_indexes"].append(index)
#             bucket["before_values"].append(row["before"])
#             bucket["after_values"].append(row["after"])
#             bucket["deltas"].append(row["delta"])
#             bucket["directions"].append(direction_sign(row["delta"]))
#             bucket["value_kinds"].append(row["value_kind"])
#             bucket["total_score"] += row["score"]
#             bucket["max_score"] = max(bucket["max_score"], row["score"])
#             bucket["max_chunk_score"] = max(bucket["max_chunk_score"], row["chunk_score"])

#     transition_count = len(transitions)
#     candidates = []
#     for bucket in buckets.values():
#         nonzero_directions = {
#             direction for direction in bucket["directions"] if direction != 0
#         }
#         bucket["consistent_direction"] = len(nonzero_directions) <= 1
#         bucket["changed_in_all_transitions"] = (
#             transition_count > 0 and bucket["occurrences"] == transition_count
#         )
#         bucket["average_score"] = (
#             bucket["total_score"] / bucket["occurrences"]
#             if bucket["occurrences"]
#             else 0
#         )
#         if bucket["occurrences"] >= 2 or bucket["changed_in_all_transitions"]:
#             candidates.append(bucket)

#     candidates.sort(
#         key=lambda row: (
#             not row["changed_in_all_transitions"],
#             not row["consistent_direction"],
#             -row["occurrences"],
#             -row["average_score"],
#             row["role"],
#             row["module_name"],
#             row["rva"],
#         )
#     )

#     if len(payloads) < 3:
#         status = "need_at_least_three_snapshots"
#     elif not same_pid:
#         status = "pid_mismatch"
#     elif any(
#         transition["byte_status"] != "ready_for_u32_samples"
#         for transition in transitions
#     ):
#         status = "not_ready_for_stability"
#     elif not candidates:
#         status = "no_repeated_candidates"
#     else:
#         status = "ready"

#     return {
#         "status": status,
#         "same_pid": same_pid,
#         "pids": pids,
#         "metas": metas,
#         "transitions": transitions,
#         "candidates": candidates,
#     }


# def format_focused_u32_stability_candidate(row, rank, transition_count):
#     transitions = ",".join(str(index + 1) for index in row["transition_indexes"])
#     before_values = ",".join(f"0x{value:08X}" for value in row["before_values"])
#     after_values = ",".join(f"0x{value:08X}" for value in row["after_values"])
#     deltas = ",".join(str(value) for value in row["deltas"])
#     kinds = ",".join(row["value_kinds"])
#     return (
#         "[Samase focused u32 stability] "
#         f"rank={rank} "
#         f"occurrences={row['occurrences']}/{transition_count} "
#         f"consistent_direction={row['consistent_direction']} "
#         f"changed_in_all={row['changed_in_all_transitions']} "
#         f"avg_score={row['average_score']:.1f} "
#         f"max_score={row['max_score']} "
#         f"{row['role']} {row['module_name']} {row['section_name']} "
#         f"rva=0x{row['rva']:08X} "
#         f"transitions={transitions} "
#         f"before={before_values} "
#         f"after={after_values} "
#         f"deltas={deltas} "
#         f"kinds={kinds}"
#     )


# def format_focused_u32_stability_report(report, labels, limit):
#     pid_text = ",".join(str(pid or "-") for pid in report["pids"])
#     lines = [
#         "[Samase focused u32 stability] "
#         f"status={report['status']} "
#         f"snapshots={len(report['metas'])} "
#         f"transitions={len(report['transitions'])} "
#         f"same_pid={report['same_pid']} "
#         f"pids={pid_text} "
#         f"candidates={len(report['candidates'])}",
#     ]
#     for index, meta in enumerate(report["metas"]):
#         label = labels[index] if index < len(labels) else str(index + 1)
#         lines.append(
#             "[Samase focused u32 stability] snapshot "
#             f"index={index + 1} "
#             f"label={label} "
#             f"pid={meta['process_pid'] or '-'} "
#             f"frame={meta['frame_count']} "
#             f"in_game={meta['in_game']} "
#             f"map={meta['map_name'] or '-'}"
#         )
#     for transition in report["transitions"]:
#         before_label = labels[transition["index"]]
#         after_label = labels[transition["index"] + 1]
#         lines.append(
#             "[Samase focused u32 stability] transition "
#             f"index={transition['index'] + 1} "
#             f"{before_label}->{after_label} "
#             f"pid={transition['before_pid'] or '-'}->{transition['after_pid'] or '-'} "
#             f"byte_status={transition['byte_status']} "
#             f"u32_changed={transition['u32_changed']} "
#             f"candidate_count={transition['candidate_count']}"
#         )
#     if report["status"] == "pid_mismatch":
#         lines.append(
#             "[Samase focused u32 stability] "
#             "candidate_rows_suppressed=true reason=pid_mismatch"
#         )
#         lines.append(
#             "[Samase focused u32 stability] action=recapture menu, ingame, "
#             "and ingame_later from one StarCraft launch; do not close or relaunch "
#             "between snapshots"
#         )
#     else:
#         for rank, row in enumerate(report["candidates"][:limit], start=1):
#             lines.append(
#                 format_focused_u32_stability_candidate(
#                     row,
#                     rank,
#                     len(report["transitions"]),
#                 )
#             )
#         if report["status"] == "ready":
#             lines.append(
#                 "[Samase focused u32 stability] action=promote repeated same-pid RVAs "
#                 "to the next read-only candidate watch list"
#             )
#     return lines


# def focused_u32_watchlist_candidates(report, limit):
#     if report.get("status") != "ready":
#         return []
#     rows = []
#     for row in report.get("candidates", []):
#         if not row.get("changed_in_all_transitions"):
#             continue
#         if not row.get("consistent_direction"):
#             continue
#         value_kinds = set(row.get("value_kinds", []))
#         if not value_kinds or not value_kinds.issubset(WATCHLIST_VALUE_KINDS):
#             continue
#         confidence = "stable_small_u32" if value_kinds == {"small_u32"} else "stable_scalar_u32"
#         rows.append(
#             {
#                 "role": row["role"],
#                 "module_name": row["module_name"],
#                 "section_name": row["section_name"],
#                 "rva": f"0x{row['rva']:08X}",
#                 "rva_value": row["rva"],
#                 "value_type": "u32_le",
#                 "read_policy": "direct_read_only_no_pointer_deref",
#                 "confidence": confidence,
#                 "occurrences": row["occurrences"],
#                 "consistent_direction": row["consistent_direction"],
#                 "changed_in_all_transitions": row["changed_in_all_transitions"],
#                 "average_score": round(row["average_score"], 3),
#                 "max_score": row["max_score"],
#                 "value_kinds": row["value_kinds"],
#                 "before_values": [f"0x{value:08X}" for value in row["before_values"]],
#                 "after_values": [f"0x{value:08X}" for value in row["after_values"]],
#                 "deltas": row["deltas"],
#             }
#         )
#     return rows[:limit]


# def focused_u32_watchlist_payload(payloads, labels, limit):
#     report = focused_u32_stability_report(payloads)
#     metas = report["metas"]
#     candidates = focused_u32_watchlist_candidates(report, limit)
#     return {
#         "schema": "lav_scr_focused_u32_watchlist_v1",
#         "status": report["status"],
#         "same_pid": report["same_pid"],
#         "pids": report["pids"],
#         "policy": {
#             "source": "focused_u32_stability",
#             "requires_same_pid": True,
#             "requires_changed_in_all_transitions": True,
#             "requires_consistent_direction": True,
#             "pointer_dereference": False,
#             "read_type": "direct_u32_le",
#             "excluded_value_kinds": ["large_or_pointer_like_u32", "zero"],
#         },
#         "snapshots": [
#             {
#                 "label": labels[index] if index < len(labels) else str(index + 1),
#                 "pid": meta["process_pid"],
#                 "frame": meta["frame_count"],
#                 "in_game": meta["in_game"],
#                 "map": meta["map_name"],
#             }
#             for index, meta in enumerate(metas)
#         ],
#         "transitions": report["transitions"],
#         "candidates": candidates,
#     }


# def format_focused_u32_watchlist_candidate(row, rank):
#     return (
#         "[Samase focused u32 watchlist] "
#         f"rank={rank} "
#         f"{row['role']} {row['module_name']} {row['section_name']} "
#         f"rva={row['rva']} "
#         f"confidence={row['confidence']} "
#         f"occurrences={row['occurrences']} "
#         f"avg_score={row['average_score']} "
#         f"kinds={','.join(row['value_kinds'])} "
#         f"deltas={','.join(str(delta) for delta in row['deltas'])}"
#     )


# def format_focused_u32_watchlist_payload(payload, output_path=""):
#     lines = [
#         "[Samase focused u32 watchlist] "
#         f"status={payload['status']} "
#         f"same_pid={payload['same_pid']} "
#         f"candidates={len(payload['candidates'])} "
#         f"output={output_path or '-'}",
#     ]
#     if payload["status"] != "ready":
#         lines.append(
#             "[Samase focused u32 watchlist] action=run "
#             "--focused-u32-stability first and recapture until status=ready"
#         )
#         return lines
#     for rank, row in enumerate(payload["candidates"], start=1):
#         lines.append(format_focused_u32_watchlist_candidate(row, rank))
#     if not payload["candidates"]:
#         lines.append(
#             "[Samase focused u32 watchlist] action=no scalar candidates passed "
#             "the conservative watchlist filter"
#         )
#     return lines


# def focused_u32_values_by_rva(payload):
#     values = {}
#     for key, chunk in flatten_focused_chunks(payload).items():
#         data = decode_hex_bytes(chunk.get("bytes_hex", ""))
#         if len(data) < 4:
#             continue
#         role, module_name, section_name, window_rva_start, chunk_rva_start = key
#         chunk_rva = int_value(chunk_rva_start)
#         for offset in range(0, len(data) - 3, 4):
#             rva = chunk_rva + offset
#             value = int.from_bytes(data[offset : offset + 4], "little")
#             values[(role, module_name, section_name, rva)] = {
#                 "role": role,
#                 "module_name": module_name,
#                 "section_name": section_name,
#                 "window_rva": window_rva_start,
#                 "chunk_rva": chunk_rva_start,
#                 "offset": offset,
#                 "rva": rva,
#                 "value": value,
#             }
#     return values


# def resource_expected_values(snapshots, field):
#     values = []
#     for snapshot in snapshots:
#         observed = snapshot.get("observed", {})
#         if not isinstance(observed, dict) or field not in observed:
#             return []
#         value = int_value(observed.get(field), None)
#         if value is None:
#             return []
#         values.append(value)
#     return values


# def resource_change_count(values):
#     return sum(
#         1
#         for index in range(1, len(values))
#         if values[index] != values[index - 1]
#     )


# def resource_candidate_confidence(
#     expected_count,
#     exact_matches,
#     expected_changes,
#     change_exact_matches,
#     direction_matches,
# ):
#     if exact_matches == expected_count and expected_changes:
#         return "exact_changed"
#     if exact_matches == expected_count:
#         return "exact_constant"
#     if expected_changes and change_exact_matches == expected_changes:
#         return "delta_exact"
#     if expected_changes and direction_matches == expected_changes:
#         return "direction_match"
#     if exact_matches:
#         return "partial_value_match"
#     return "weak"


# def resource_candidate_score(
#     expected_values,
#     actual_values,
#     exact_matches,
#     change_exact_matches,
#     direction_matches,
# ):
#     expected_changes = resource_change_count(expected_values)
#     if expected_changes:
#         score = exact_matches * 100
#         score += change_exact_matches * 45
#         score += direction_matches * 20
#         score -= (len(expected_values) - exact_matches) * 35
#         return score
#     return exact_matches * 60


# def resource_candidate_rows_for_field(value_maps, expected_values):
#     if not value_maps or len(expected_values) != len(value_maps):
#         return []
#     common_keys = set(value_maps[0])
#     for value_map in value_maps[1:]:
#         common_keys &= set(value_map)

#     rows = []
#     expected_changes = resource_change_count(expected_values)
#     for key in sorted(common_keys):
#         samples = [value_map[key] for value_map in value_maps]
#         actual_values = [sample["value"] for sample in samples]
#         exact_matches = sum(
#             1
#             for expected, actual in zip(expected_values, actual_values)
#             if expected == actual
#         )
#         if exact_matches == 0:
#             continue

#         change_exact_matches = 0
#         direction_matches = 0
#         for index in range(1, len(expected_values)):
#             expected_delta = expected_values[index] - expected_values[index - 1]
#             if expected_delta == 0:
#                 continue
#             actual_delta = actual_values[index] - actual_values[index - 1]
#             if actual_delta == expected_delta:
#                 change_exact_matches += 1
#             if direction_sign(actual_delta) == direction_sign(expected_delta):
#                 direction_matches += 1

#         row = dict(samples[-1])
#         row.update(
#             {
#                 "expected_values": expected_values,
#                 "actual_values": actual_values,
#                 "exact_matches": exact_matches,
#                 "expected_count": len(expected_values),
#                 "expected_changes": expected_changes,
#                 "change_exact_matches": change_exact_matches,
#                 "direction_matches": direction_matches,
#                 "score": resource_candidate_score(
#                     expected_values,
#                     actual_values,
#                     exact_matches,
#                     change_exact_matches,
#                     direction_matches,
#                 ),
#                 "confidence": resource_candidate_confidence(
#                     len(expected_values),
#                     exact_matches,
#                     expected_changes,
#                     change_exact_matches,
#                     direction_matches,
#                 ),
#             }
#         )
#         rows.append(row)

#     rows.sort(
#         key=lambda row: (
#             -row["score"],
#             -row["exact_matches"],
#             -row["change_exact_matches"],
#             -row["direction_matches"],
#             row["role"],
#             row["module_name"],
#             row["rva"],
#         )
#     )
#     return rows


# def build_resource_candidate_payload_from_snapshots(snapshots, limit=MAX_RESOURCE_CANDIDATES):
#     metas = [state_metadata(snapshot.get("payload", {})) for snapshot in snapshots]
#     pids = [meta["process_pid"] for meta in metas]
#     non_empty_pids = [pid for pid in pids if pid]
#     same_pid = bool(non_empty_pids) and len(set(non_empty_pids)) == 1
#     value_maps = [
#         focused_u32_values_by_rva(snapshot.get("payload", {}))
#         for snapshot in snapshots
#     ]
#     focused_bytes_ready = all(bool(value_map) for value_map in value_maps)

#     field_payloads = {}
#     candidate_fields = 0
#     exact_changed_fields = 0
#     exact_changed_field_names = []
#     for field in RESOURCE_OBSERVATION_FIELDS:
#         expected_values = resource_expected_values(snapshots, field)
#         if not expected_values:
#             field_payloads[field] = {
#                 "status": "missing_expected_values",
#                 "expected_values": [],
#                 "expected_changes": 0,
#                 "candidates": [],
#             }
#             continue
#         if len(expected_values) < 2:
#             field_payloads[field] = {
#                 "status": "need_at_least_two_snapshots",
#                 "expected_values": expected_values,
#                 "expected_changes": 0,
#                 "candidates": [],
#             }
#             continue
#         expected_changes = resource_change_count(expected_values)
#         if expected_changes == 0 and all(value == 0 for value in expected_values):
#             field_payloads[field] = {
#                 "status": "constant_zero_not_ranked",
#                 "expected_values": expected_values,
#                 "expected_changes": 0,
#                 "candidates": [],
#             }
#             continue
#         rows = resource_candidate_rows_for_field(value_maps, expected_values)
#         exact_changed_candidate_count = sum(
#             1 for row in rows if row.get("confidence") == "exact_changed"
#         )
#         candidates = [
#             {
#                 "role": row["role"],
#                 "module_name": row["module_name"],
#                 "section_name": row["section_name"],
#                 "window_rva": row["window_rva"],
#                 "chunk_rva": row["chunk_rva"],
#                 "offset": row["offset"],
#                 "rva": f"0x{row['rva']:08X}",
#                 "rva_value": row["rva"],
#                 "expected_values": row["expected_values"],
#                 "actual_values": row["actual_values"],
#                 "exact_matches": row["exact_matches"],
#                 "expected_count": row["expected_count"],
#                 "expected_changes": row["expected_changes"],
#                 "change_exact_matches": row["change_exact_matches"],
#                 "direction_matches": row["direction_matches"],
#                 "score": row["score"],
#                 "confidence": row["confidence"],
#             }
#             for row in rows[:limit]
#         ]
#         if candidates:
#             candidate_fields += 1
#         if exact_changed_candidate_count:
#             exact_changed_fields += 1
#             exact_changed_field_names.append(field)
#         field_payloads[field] = {
#             "status": (
#                 "exact_changed"
#                 if exact_changed_candidate_count
#                 else "ranked"
#                 if candidates
#                 else "no_matching_u32_values"
#             ),
#             "expected_values": expected_values,
#             "expected_changes": expected_changes,
#             "exact_changed_candidates": exact_changed_candidate_count,
#             "candidates": candidates,
#         }

#     required_exact_ready = all(
#         field in exact_changed_field_names for field in RESOURCE_REQUIRED_EXACT_FIELDS
#     )
#     if len(snapshots) < 2:
#         status = "need_at_least_two_snapshots"
#     elif not focused_bytes_ready:
#         status = "missing_focused_bytes"
#     elif required_exact_ready:
#         status = "ready"
#     elif exact_changed_fields:
#         status = "support_only_candidates"
#     elif candidate_fields:
#         status = "partial_candidates"
#     else:
#         status = "no_resource_candidates"

#     return {
#         "schema": "lav_scr_resource_candidates_v1",
#         "status": status,
#         "same_pid": same_pid,
#         "pids": pids,
#         "snapshot_count": len(snapshots),
#         "candidate_fields": candidate_fields,
#         "exact_changed_fields": exact_changed_fields,
#         "exact_changed_field_names": exact_changed_field_names,
#         "required_exact_fields": list(RESOURCE_REQUIRED_EXACT_FIELDS),
#         "required_exact_ready": required_exact_ready,
#         "policy": {
#             "source": "focused_window_drilldown.bytes_hex",
#             "pointer_dereference": False,
#             "read_type": "aligned_u32_le",
#             "constant_zero_fields_ranked": False,
#         },
#         "snapshots": [
#             {
#                 "label": snapshot.get("label", str(index + 1)),
#                 "path": str(snapshot.get("path", "")),
#                 "pid": metas[index]["process_pid"],
#                 "frame": metas[index]["frame_count"],
#                 "in_game": metas[index]["in_game"],
#                 "observed": snapshot.get("observed", {}),
#             }
#             for index, snapshot in enumerate(snapshots)
#         ],
#         "fields": field_payloads,
#     }


# def resource_snapshot_path(spec_path, path_text):
#     raw_path = Path(str(path_text))
#     if raw_path.is_absolute():
#         return raw_path
#     spec_parent = Path(spec_path).parent if spec_path else Path.cwd()
#     for candidate in (spec_parent / raw_path, Path.cwd() / raw_path, raw_path):
#         if candidate.is_file():
#             return candidate
#     return spec_parent / raw_path


# def load_resource_probe_spec(spec_path):
#     spec_path = Path(spec_path)
#     spec = load_json_payload(spec_path)
#     snapshots = []
#     for index, entry in enumerate(spec.get("snapshots", []), start=1):
#         if not isinstance(entry, dict):
#             continue
#         path_text = entry.get("path", "")
#         path = resource_snapshot_path(spec_path, path_text)
#         observed_source = entry.get("observed", entry)
#         if not isinstance(observed_source, dict):
#             observed_source = {}
#         observed = {}
#         for field in RESOURCE_OBSERVATION_FIELDS:
#             value = observed_source.get(field)
#             if value is not None:
#                 observed[field] = int_value(value, None)
#         snapshots.append(
#             {
#                 "label": entry.get("label") or f"snapshot_{index}",
#                 "path": str(path),
#                 "observed": observed,
#                 "payload": load_json_payload(path),
#             }
#         )
#     return spec, snapshots


# def resource_probe_spec_template(out_dir):
#     states_dir = Path(out_dir) / "states"
#     return {
#         "schema": "lav_scr_resource_probe_spec_v1",
#         "notes": [
#             "Fill observed values from the visible in-game UI for each captured state.",
#             "Use at least two snapshots where minerals changes; gas=0 is ignored until it changes.",
#         ],
#         "snapshots": [
#             {
#                 "label": "game_start",
#                 "path": str(states_dir / "game_start.json"),
#                 "observed": {
#                     "minerals": None,
#                     "gas": None,
#                     "supply_used": None,
#                     "supply_total": None,
#                 },
#             },
#             {
#                 "label": "after_mining",
#                 "path": str(states_dir / "after_mining.json"),
#                 "observed": {
#                     "minerals": None,
#                     "gas": None,
#                     "supply_used": None,
#                     "supply_total": None,
#                 },
#             },
#             {
#                 "label": "after_spending",
#                 "path": str(states_dir / "after_spending.json"),
#                 "observed": {
#                     "minerals": None,
#                     "gas": None,
#                     "supply_used": None,
#                     "supply_total": None,
#                 },
#             },
#         ],
#     }


# def format_resource_candidate(row, rank):
#     expected = ",".join(str(value) for value in row["expected_values"])
#     actual = ",".join(str(value) for value in row["actual_values"])
#     return (
#         "[Samase resource candidate] "
#         f"rank={rank} "
#         f"score={row['score']} "
#         f"{row['role']} {row['module_name']} {row['section_name']} "
#         f"rva={row['rva']} "
#         f"offset=+0x{row['offset']:02X} "
#         f"exact={row['exact_matches']}/{row['expected_count']} "
#         f"change_exact={row['change_exact_matches']}/{row['expected_changes']} "
#         f"direction={row['direction_matches']}/{row['expected_changes']} "
#         f"confidence={row['confidence']} "
#         f"expected={expected} "
#         f"actual={actual}"
#     )


# def format_resource_candidate_payload(payload, output_path=""):
#     pid_text = ",".join(str(pid or "-") for pid in payload["pids"])
#     exact_field_text = ",".join(payload.get("exact_changed_field_names", [])) or "-"
#     required_field_text = ",".join(payload.get("required_exact_fields", [])) or "-"
#     lines = [
#         "[Samase resource candidate] "
#         f"schema={payload['schema']} "
#         f"status={payload['status']} "
#         f"snapshots={payload['snapshot_count']} "
#         f"same_pid={payload['same_pid']} "
#         f"pids={pid_text} "
#         f"candidate_fields={payload.get('candidate_fields', 0)} "
#         f"exact_changed_fields={payload.get('exact_changed_fields', 0)} "
#         f"exact_field_names={exact_field_text} "
#         f"required_exact_fields={required_field_text} "
#         f"required_exact_ready={payload.get('required_exact_ready', False)} "
#         f"output={output_path or '-'}",
#     ]
#     for snapshot in payload["snapshots"]:
#         observed = ",".join(
#             f"{field}={snapshot['observed'].get(field, '-')}"
#             for field in RESOURCE_OBSERVATION_FIELDS
#         )
#         lines.append(
#             "[Samase resource candidate] snapshot "
#             f"label={snapshot['label']} "
#             f"pid={snapshot['pid'] or '-'} "
#             f"frame={snapshot['frame']} "
#             f"in_game={snapshot['in_game']} "
#             f"{observed}"
#         )
#     for field in RESOURCE_OBSERVATION_FIELDS:
#         field_payload = payload["fields"].get(field, {})
#         expected = ",".join(
#             str(value) for value in field_payload.get("expected_values", [])
#         )
#         candidates = field_payload.get("candidates", [])
#         lines.append(
#             "[Samase resource candidate] "
#             f"field={field} "
#             f"status={field_payload.get('status', '-')} "
#             f"expected={expected or '-'} "
#             f"changes={field_payload.get('expected_changes', 0)} "
#             f"exact_changed_candidates={field_payload.get('exact_changed_candidates', 0)} "
#             f"candidates={len(candidates)}"
#         )
#         for rank, row in enumerate(candidates, start=1):
#             lines.append(format_resource_candidate(row, rank))
#     if payload["status"] == "missing_focused_bytes":
#         lines.append(
#             "[Samase resource candidate] action=recapture states through the "
#             "rebuilt control_more_dll path so focused bytes_hex is present"
#         )
#     elif payload["status"] == "ready":
#         lines.append(
#             "[Samase resource candidate] action=promote exact_changed resource "
#             "RVAs to the next read-only resource watchlist"
#         )
#     elif payload["status"] == "partial_candidates":
#         lines.append(
#             "[Samase resource candidate] action=do not promote yet; current "
#             "matches are partial only, so recapture with changed resource values "
#             "or expand the read-only focused scan surface"
#         )
#     elif payload["status"] == "support_only_candidates":
#         lines.append(
#             "[Samase resource candidate] action=keep support-field candidates "
#             "as hints only; do not promote resource state until minerals has an "
#             "exact_changed candidate"
#         )
#     return lines


# def format_byte_diff_items(diff):
#     items = diff.get("items", [])
#     if not items:
#         return "-"
#     return ",".join(
#         f"+0x{item['offset']:02X}:{item['before']:02X}->{item['after']:02X}"
#         for item in items
#     )


# def format_u32_diff_items(diff):
#     items = diff.get("items", [])
#     if not items:
#         return "-"
#     return ",".join(
#         f"+0x{item['offset']:02X}:0x{item['before']:08X}->0x{item['after']:08X}"
#         for item in items
#     )


# def offset_comparison_warnings(before_payload, after_payload):
#     before_meta = state_metadata(before_payload)
#     after_meta = state_metadata(after_payload)
#     warnings = []
#     if not before_meta["in_game"] and not after_meta["in_game"]:
#         warnings.append(
#             "both snapshots report in_game=false; detector has not confirmed gameplay, "
#             "so treat this as a manual/uncertain capture and prefer same-pid memory_changed windows"
#         )
#     if (
#         before_meta["process_pid"]
#         and after_meta["process_pid"]
#         and before_meta["process_pid"] != after_meta["process_pid"]
#     ):
#         warnings.append(
#             "snapshots come from different process ids; readable_pointer_u32 "
#             "value changes are usually ASLR/heap noise across launches"
#         )
#     for field in (
#         "starcraft_base",
#         "clientsdk_base",
#         "samase_temp_base",
#         "samase_temp_name",
#     ):
#         if before_meta.get(field) and after_meta.get(field) and before_meta[field] != after_meta[field]:
#             warnings.append(
#                 f"{field} differs between snapshots; compare RVAs and scalar gates "
#                 "before trusting pointer-value changes"
#             )
#     return warnings


# def compare_offset_discovery_payloads(before_payload, after_payload):
#     before = flatten_offset_discovery_candidates(before_payload)
#     after = flatten_offset_discovery_candidates(after_payload)
#     before_keys = set(before)
#     after_keys = set(after)
#     common_keys = before_keys & after_keys

#     changed = [
#         (key, before[key], after[key])
#         for key in sorted(common_keys)
#         if before[key].get("value") != after[key].get("value")
#     ]
#     added = [(key, after[key]) for key in sorted(after_keys - before_keys)]
#     removed = [(key, before[key]) for key in sorted(before_keys - after_keys)]
#     return {
#         "changed": changed,
#         "added": added,
#         "removed": removed,
#     }


# def compare_offset_window_profiles(before_payload, after_payload):
#     return compare_profile_maps(
#         flatten_offset_window_profiles(before_payload),
#         flatten_offset_window_profiles(after_payload),
#     )


# def compare_focused_chunks(before_payload, after_payload):
#     return compare_profile_maps(
#         flatten_focused_chunks(before_payload),
#         flatten_focused_chunks(after_payload),
#     )


# def compare_profile_maps(before, after):
#     before_keys = set(before)
#     after_keys = set(after)
#     common_keys = before_keys & after_keys

#     changed = []
#     pointer_plausibility_changed = []
#     for key in sorted(common_keys):
#         before_profile = before[key]
#         after_profile = after[key]
#         content_changed = any(
#             before_profile.get(field) != after_profile.get(field)
#             for field in DISCOVERY_WINDOW_CONTENT_FIELDS
#         )
#         dynamic_changed = any(
#             before_profile.get(field) != after_profile.get(field)
#             for field in DISCOVERY_WINDOW_DYNAMIC_FIELDS
#         )
#         if content_changed:
#             changed.append((key, before_profile, after_profile))
#         elif dynamic_changed:
#             pointer_plausibility_changed.append((key, before_profile, after_profile))
#     changed.sort(key=lambda item: (-window_change_score(item[1], item[2]), item[0]))
#     pointer_plausibility_changed.sort(
#         key=lambda item: (
#             -abs(window_field_delta(item[1], item[2], "readable_pointer_u32_count")),
#             item[0],
#         )
#     )
#     added = [(key, after[key]) for key in sorted(after_keys - before_keys)]
#     removed = [(key, before[key]) for key in sorted(before_keys - after_keys)]
#     return {
#         "changed": changed,
#         "pointer_plausibility_changed": pointer_plausibility_changed,
#         "added": added,
#         "removed": removed,
#     }


# def summarize_offset_discovery_file(path):
#     return summarize_offset_discovery_payload(load_json_payload(path))


# def compare_offset_discovery_files(before_path, after_path):
#     return compare_offset_discovery_payloads(
#         load_json_payload(before_path),
#         load_json_payload(after_path),
#     )


# def print_offset_discovery_summary(path):
#     payload = load_json_payload(path)
#     print(format_state_metadata(Path(path).name, payload))
#     for line in summarize_offset_discovery_payload(payload):
#         print(line)


# def print_focused_u32_watch_summary(path):
#     payload = load_json_payload(path)
#     print(format_state_metadata(Path(path).name, payload))
#     for line in summarize_focused_u32_watch_payload(payload):
#         print(line)


# def print_focused_u32_watch_signal(paths):
#     payloads = [load_json_payload(path) for path in paths]
#     labels = [Path(path).name for path in paths]
#     signals = []
#     for label, payload in zip(labels, payloads):
#         print(format_state_metadata(label, payload))
#         signal = focused_u32_watch_signal(payload)
#         signals.append(signal)
#         print(format_focused_u32_watch_signal(signal, label))
#     for index in range(len(signals) - 1):
#         print(
#             format_focused_u32_watch_transition(
#                 signals[index],
#                 signals[index + 1],
#                 labels[index],
#                 labels[index + 1],
#             )
#         )
#     return signals


# def print_offset_discovery_comparison(before_path, after_path):
#     before_payload = load_json_payload(before_path)
#     after_payload = load_json_payload(after_path)
#     print(format_state_metadata("before", before_payload))
#     print(format_state_metadata("after", after_payload))
#     for warning in offset_comparison_warnings(before_payload, after_payload):
#         print(f"[Samase offset discovery diff] warning={warning}")
#     result = compare_offset_discovery_payloads(before_payload, after_payload)
#     window_result = compare_offset_window_profiles(before_payload, after_payload)
#     focused_result = compare_focused_chunks(before_payload, after_payload)
#     focused_bytes_available = focused_chunks_have_bytes_hex(
#         before_payload,
#     ) and focused_chunks_have_bytes_hex(after_payload)
#     print(
#         "[Samase offset discovery diff] "
#         f"changed={len(result['changed'])} "
#         f"added={len(result['added'])} "
#         f"removed={len(result['removed'])}"
#     )
#     print(
#         "[Samase offset window diff] "
#         f"memory_changed={len(window_result['changed'])} "
#         f"pointer_plausibility_changed={len(window_result['pointer_plausibility_changed'])} "
#         f"added={len(window_result['added'])} "
#         f"removed={len(window_result['removed'])}"
#     )
#     print(
#         "[Samase focused chunk diff] "
#         f"memory_changed={len(focused_result['changed'])} "
#         f"pointer_plausibility_changed={len(focused_result['pointer_plausibility_changed'])} "
#         f"bytes_hex_available={focused_bytes_available} "
#         f"added={len(focused_result['added'])} "
#         f"removed={len(focused_result['removed'])}"
#     )
#     if focused_result["changed"] and not focused_bytes_available:
#         print(
#             "[Samase focused chunk diff] warning=focused chunks are missing bytes_hex; "
#             "restart StarCraft with the rebuilt target-probe DLL before trusting byte/u32 samples"
#         )
#     window_printed = 0
#     for key, before, after in window_result["changed"]:
#         if window_printed >= MAX_PRINTED_DISCOVERY_DIFFS:
#             break
#         print(
#             "[Samase offset window diff] changed "
#             f"{window_profile_key_text(key)} "
#             f"score={window_change_score(before, after)} "
#             f"fnv={before.get('fnv1a64')}->{after.get('fnv1a64')} "
#             f"nonzero={before.get('nonzero_bytes')}->{after.get('nonzero_bytes')} "
#             f"d_nonzero={window_field_delta(before, after, 'nonzero_bytes')} "
#             f"small={before.get('small_u32_nonzero_count')}->{after.get('small_u32_nonzero_count')} "
#             f"d_small={window_field_delta(before, after, 'small_u32_nonzero_count')} "
#             f"bool1={before.get('bool_u8_true_count')}->{after.get('bool_u8_true_count')} "
#             f"d_bool1={window_field_delta(before, after, 'bool_u8_true_count')} "
#             f"ptr={before.get('readable_pointer_u32_count')}->{after.get('readable_pointer_u32_count')}"
#         )
#         window_printed += 1
#     for key, before, after in window_result["pointer_plausibility_changed"]:
#         if window_printed >= MAX_PRINTED_DISCOVERY_DIFFS:
#             break
#         print(
#             "[Samase offset window diff] pointer_plausibility_changed "
#             f"{window_profile_key_text(key)} "
#             f"ptr={before.get('readable_pointer_u32_count')}->{after.get('readable_pointer_u32_count')} "
#             "note=memory hash and scalar counts were unchanged"
#         )
#         window_printed += 1
#     chunk_printed = 0
#     for key, before, after in focused_result["changed"]:
#         if chunk_printed >= MAX_PRINTED_DISCOVERY_DIFFS:
#             break
#         byte_diff = focused_chunk_byte_diffs(before, after)
#         u32_diff = focused_chunk_u32_diffs(before, after)
#         print(
#             "[Samase focused chunk diff] changed "
#             f"{focused_chunk_key_text(key)} "
#             f"score={window_change_score(before, after)} "
#             f"fnv={before.get('fnv1a64')}->{after.get('fnv1a64')} "
#             f"nonzero={before.get('nonzero_bytes')}->{after.get('nonzero_bytes')} "
#             f"d_nonzero={window_field_delta(before, after, 'nonzero_bytes')} "
#             f"small={before.get('small_u32_nonzero_count')}->{after.get('small_u32_nonzero_count')} "
#             f"d_small={window_field_delta(before, after, 'small_u32_nonzero_count')} "
#             f"bool1={before.get('bool_u8_true_count')}->{after.get('bool_u8_true_count')} "
#             f"d_bool1={window_field_delta(before, after, 'bool_u8_true_count')} "
#             f"ptr={before.get('readable_pointer_u32_count')}->{after.get('readable_pointer_u32_count')} "
#             f"bytes_hex_available={bool(before.get('bytes_hex')) and bool(after.get('bytes_hex'))} "
#             f"bytes_changed={byte_diff['changed_count']} "
#             f"byte_samples={format_byte_diff_items(byte_diff)} "
#             f"u32_changed={u32_diff['changed_count']} "
#             f"u32_samples={format_u32_diff_items(u32_diff)}"
#         )
#         chunk_printed += 1
#     printed = 0
#     for key, before, after in result["changed"]:
#         if printed >= MAX_PRINTED_DISCOVERY_DIFFS:
#             return
#         print(
#             "[Samase offset discovery diff] changed "
#             f"{discovery_key_text(key)} "
#             f"{before.get('hex_value', before.get('value'))}"
#             f"->{after.get('hex_value', after.get('value'))}"
#         )
#         printed += 1
#     for key, value in result["added"]:
#         if printed >= MAX_PRINTED_DISCOVERY_DIFFS:
#             return
#         print(
#             "[Samase offset discovery diff] added "
#             f"{discovery_key_text(key)} "
#             f"value={value.get('hex_value', value.get('value'))}"
#         )
#         printed += 1
#     for key, value in result["removed"]:
#         if printed >= MAX_PRINTED_DISCOVERY_DIFFS:
#             return
#         print(
#             "[Samase offset discovery diff] removed "
#             f"{discovery_key_text(key)} "
#             f"value={value.get('hex_value', value.get('value'))}"
#         )
#         printed += 1


# def print_focused_byte_probe_check(before_path, after_path):
#     before_payload = load_json_payload(before_path)
#     after_payload = load_json_payload(after_path)
#     print(format_state_metadata("before", before_payload))
#     print(format_state_metadata("after", after_payload))
#     for warning in offset_comparison_warnings(before_payload, after_payload):
#         print(f"[Samase focused byte check] warning={warning}")
#     status = focused_byte_probe_status(before_payload, after_payload)
#     for line in format_focused_byte_probe_status(status):
#         print(line)
#     return status


# def print_focused_u32_candidates(before_path, after_path, limit):
#     before_payload = load_json_payload(before_path)
#     after_payload = load_json_payload(after_path)
#     print(format_state_metadata("before", before_payload))
#     print(format_state_metadata("after", after_payload))
#     for warning in offset_comparison_warnings(before_payload, after_payload):
#         print(f"[Samase focused u32 candidate] warning={warning}")
#     byte_status = focused_byte_probe_status(before_payload, after_payload)
#     print(
#         "[Samase focused u32 candidate] "
#         f"byte_status={byte_status['status']} "
#         f"u32_changed={byte_status['u32_changed']} "
#         f"changed_with_bytes={byte_status['changed_with_bytes']}"
#     )
#     rows = focused_u32_candidate_rows(before_payload, after_payload)
#     print(f"[Samase focused u32 candidate] count={len(rows)} printed={min(limit, len(rows))}")
#     for rank, row in enumerate(rows[:limit], start=1):
#         print(format_focused_u32_candidate(row, rank))
#     if byte_status["status"] != "ready_for_u32_samples":
#         print(
#             "[Samase focused u32 candidate] action=recapture states until "
#             "--focused-byte-check reports status=ready_for_u32_samples"
#         )
#     return rows


# def print_focused_u32_stability(paths, limit):
#     payloads = [load_json_payload(path) for path in paths]
#     labels = [Path(path).name for path in paths]
#     report = focused_u32_stability_report(payloads)
#     for line in format_focused_u32_stability_report(report, labels, limit):
#         print(line)
#     return report


# def print_focused_u32_watchlist(paths, limit, output_path=None):
#     payloads = [load_json_payload(path) for path in paths]
#     labels = [Path(path).name for path in paths]
#     payload = focused_u32_watchlist_payload(payloads, labels, limit)
#     written_path = ""
#     if output_path:
#         output_path = Path(output_path)
#         output_path.parent.mkdir(parents=True, exist_ok=True)
#         output_path.write_text(
#             json.dumps(payload, indent=2),
#             encoding="utf-8",
#         )
#         written_path = str(output_path)
#     for line in format_focused_u32_watchlist_payload(payload, written_path):
#         print(line)
#     return payload


# def print_resource_probe_spec_template(out_dir, output_path):
#     path = Path(output_path) if output_path else Path(out_dir) / "resource_probe_spec.json"
#     path.parent.mkdir(parents=True, exist_ok=True)
#     path.write_text(
#         json.dumps(resource_probe_spec_template(out_dir), indent=2),
#         encoding="utf-8",
#     )
#     print(f"[Samase resource spec] wrote {path}")
#     return path


# def print_resource_candidates(spec_path, limit, output_path=None):
#     _, snapshots = load_resource_probe_spec(spec_path)
#     payload = build_resource_candidate_payload_from_snapshots(snapshots, limit)
#     written_path = ""
#     if output_path:
#         output_path = Path(output_path)
#         output_path.parent.mkdir(parents=True, exist_ok=True)
#         output_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
#         written_path = str(output_path)
#     for line in format_resource_candidate_payload(payload, written_path):
#         print(line)
#     return payload


# def save_current_state_snapshot(out_dir, label):
#     source = default_control_state_path(out_dir)
#     target = default_named_state_path(out_dir, label)
#     if not source.is_file():
#         raise FileNotFoundError(f"source state file does not exist: {source}")
#     target.parent.mkdir(parents=True, exist_ok=True)
#     shutil.copy2(source, target)
#     return target


# def load_manifest(out_dir):
#     manifest_path = Path(out_dir) / "probe_manifest.json"
#     with open(manifest_path, "r", encoding="utf-8") as file:
#         return json.load(file)


# def check_probe_results(out_dir):
#     manifest = load_manifest(out_dir)
#     results = []
#     for entry in manifest.get("entries", []):
#         result = dict(entry)
#         result.update(classify_state_file(entry.get("state_path", "")))
#         results.append(result)
#     return results


# def parse_args(argv=None):
#     config = StarCraftConfig()
#     parser = argparse.ArgumentParser(
#         description=(
#             "Prepare or check Samase plugin API loader probes. This writes "
#             "candidate mod folders and .cmd launch scripts only."
#         )
#     )
#     parser.add_argument(
#         "--check",
#         action="store_true",
#         help="Check probe state files instead of preparing candidates.",
#     )
#     parser.add_argument(
#         "--offset-summary",
#         nargs="?",
#         const="",
#         help=(
#             "Print a compact bridge.offset_discovery summary for a state JSON. "
#             "Defaults to logs/samase_plugin_loader_probe/states/control_more_dll.json."
#         ),
#     )
#     parser.add_argument(
#         "--u32-watch-summary",
#         nargs="?",
#         const="",
#         help=(
#             "Print a compact bridge.focused_u32_watch summary for a state JSON. "
#             "Defaults to logs/samase_plugin_loader_probe/states/control_more_dll.json."
#         ),
#     )
#     parser.add_argument(
#         "--u32-watch-signal",
#         nargs="*",
#         help=(
#             "Classify one or more bridge.focused_u32_watch snapshots as "
#             "menu_like or active. Defaults to the current control state."
#         ),
#     )
#     parser.add_argument(
#         "--save-state",
#         metavar="LABEL",
#         help=(
#             "Copy the current control_more_dll state JSON to states/LABEL.json "
#             "and print its offset discovery summary."
#         ),
#     )
#     parser.add_argument(
#         "--compare-offset-discovery",
#         nargs=2,
#         metavar=("BEFORE_JSON", "AFTER_JSON"),
#         help="Compare two bridge.offset_discovery payloads and print changed candidates.",
#     )
#     parser.add_argument(
#         "--focused-byte-check",
#         nargs=2,
#         metavar=("BEFORE_JSON", "AFTER_JSON"),
#         help="Check whether focused chunks include byte/u32 samples for candidate ranking.",
#     )
#     parser.add_argument(
#         "--focused-u32-candidates",
#         nargs=2,
#         metavar=("BEFORE_JSON", "AFTER_JSON"),
#         help="Rank changed u32 values inside focused byte chunks without pointer dereferences.",
#     )
#     parser.add_argument(
#         "--max-focused-u32-candidates",
#         type=int,
#         default=MAX_PRINTED_FOCUSED_U32_CANDIDATES,
#         help="Maximum candidate rows printed by --focused-u32-candidates.",
#     )
#     parser.add_argument(
#         "--focused-u32-stability",
#         nargs="+",
#         metavar="STATE_JSON",
#         help=(
#             "Compare three or more focused byte snapshots and rank repeated "
#             "same-RVA u32 changes. This does not dereference pointers."
#         ),
#     )
#     parser.add_argument(
#         "--max-focused-u32-stability",
#         type=int,
#         default=MAX_PRINTED_FOCUSED_U32_STABILITY,
#         help="Maximum candidate rows printed by --focused-u32-stability.",
#     )
#     parser.add_argument(
#         "--focused-u32-watchlist",
#         nargs="+",
#         metavar="STATE_JSON",
#         help=(
#             "Export conservative same-pid scalar u32 watch candidates from "
#             "three or more focused byte snapshots."
#         ),
#     )
#     parser.add_argument(
#         "--watchlist-out",
#         help="Optional JSON output path for --focused-u32-watchlist.",
#     )
#     parser.add_argument(
#         "--max-focused-u32-watchlist",
#         type=int,
#         default=MAX_FOCUSED_U32_WATCHLIST_CANDIDATES,
#         help="Maximum candidate rows written by --focused-u32-watchlist.",
#     )
#     parser.add_argument(
#         "--resource-spec-template",
#         nargs="?",
#         const="",
#         help=(
#             "Write a lav_scr_resource_probe_spec_v1 template JSON. Defaults to "
#             "logs/samase_plugin_loader_probe/resource_probe_spec.json."
#         ),
#     )
#     parser.add_argument(
#         "--resource-candidates",
#         metavar="SPEC_JSON",
#         help=(
#             "Rank resource field u32 candidates from a spec containing captured "
#             "state JSON paths and observed minerals/gas/supply values."
#         ),
#     )
#     parser.add_argument(
#         "--resource-candidates-out",
#         help="Optional JSON output path for --resource-candidates.",
#     )
#     parser.add_argument(
#         "--max-resource-candidates",
#         type=int,
#         default=MAX_RESOURCE_CANDIDATES,
#         help="Maximum candidate rows per resource field.",
#     )
#     parser.add_argument(
#         "--require-focused-bytes",
#         action="store_true",
#         help="Return exit code 3 unless --focused-byte-check is ready for u32 samples.",
#     )
#     parser.add_argument(
#         "--require-api",
#         action="store_true",
#         help="Return exit code 2 if no candidate wrote loader=samase_plugin_api.",
#     )
#     parser.add_argument(
#         "--out-dir",
#         default=str(default_output_dir()),
#         help="Probe output directory under logs by default.",
#     )
#     parser.add_argument(
#         "--plugin-dll",
#         default=str(default_plugin_dll_path(config)),
#         help="Built lav_samase_readonly_plugin.dll path.",
#     )
#     parser.add_argument(
#         "--samase-exe",
#         default=config.resolve_path("samase_path")
#         or config.resolve_path("samase_exe_path"),
#         help="Samase executable path.",
#     )
#     parser.add_argument(
#         "--starcraft-x86-dir",
#         default=config.resolve_path("starcraft_x86_dir"),
#         help="StarCraft Remastered x86 directory.",
#     )
#     parser.add_argument(
#         "--mod-arg",
#         default=config.get("samase_arg", config.get("mod_argument", "custom")),
#         help="Configured mod arg used only by the SAMASE_MORE_DLLS control.",
#     )
#     parser.add_argument(
#         "--write-every-n-frames",
#         type=int,
#         default=config.get_int("samase_state_write_every_n_frames", 8),
#         help="LAV_SAMASE_STATE_EVERY_N_FRAMES value.",
#     )
#     parser.add_argument(
#         "--heartbeat-ms",
#         type=int,
#         default=config.get_int("samase_initialize_heartbeat_interval_ms", 1000),
#         help="LAV_SAMASE_HEARTBEAT_INTERVAL_MS value.",
#     )
#     parser.add_argument(
#         "--disable-resource-focused-scan",
#         action="store_true",
#         help="Set LAV_SAMASE_RESOURCE_FOCUSED_SCAN=0 in generated run scripts.",
#     )
#     parser.add_argument(
#         "--resource-focused-start-window",
#         type=int,
#         default=0,
#         help=(
#             "LAV_SAMASE_RESOURCE_FOCUSED_START_WINDOW value for paged .data "
#             "resource sweeps."
#         ),
#     )
#     parser.add_argument(
#         "--resource-focused-windows",
#         type=int,
#         default=64,
#         help="LAV_SAMASE_RESOURCE_FOCUSED_WINDOWS value for generated run scripts.",
#     )
#     return parser.parse_args(argv)


# def main(argv=None):
#     args = parse_args(argv)
#     out_dir = Path(args.out_dir)

#     if args.offset_summary is not None:
#         path = Path(args.offset_summary) if args.offset_summary else default_control_state_path(out_dir)
#         print_offset_discovery_summary(path)
#         return 0

#     if args.u32_watch_summary is not None:
#         path = Path(args.u32_watch_summary) if args.u32_watch_summary else default_control_state_path(out_dir)
#         print_focused_u32_watch_summary(path)
#         return 0

#     if args.u32_watch_signal is not None:
#         paths = (
#             [Path(path) for path in args.u32_watch_signal]
#             if args.u32_watch_signal
#             else [default_control_state_path(out_dir)]
#         )
#         print_focused_u32_watch_signal(paths)
#         return 0

#     if args.save_state:
#         path = save_current_state_snapshot(out_dir, args.save_state)
#         print(f"[Samase state save] {path}")
#         print_offset_discovery_summary(path)
#         return 0

#     if args.compare_offset_discovery:
#         before_path, after_path = args.compare_offset_discovery
#         print_offset_discovery_comparison(before_path, after_path)
#         return 0

#     if args.focused_byte_check:
#         before_path, after_path = args.focused_byte_check
#         status = print_focused_byte_probe_check(before_path, after_path)
#         if args.require_focused_bytes and status["status"] != "ready_for_u32_samples":
#             return 3
#         return 0

#     if args.focused_u32_candidates:
#         before_path, after_path = args.focused_u32_candidates
#         limit = max(1, int_value(args.max_focused_u32_candidates, MAX_PRINTED_FOCUSED_U32_CANDIDATES))
#         print_focused_u32_candidates(before_path, after_path, limit)
#         return 0

#     if args.focused_u32_stability:
#         if len(args.focused_u32_stability) < 3:
#             print(
#                 "[Samase focused u32 stability] "
#                 "status=need_at_least_three_snapshots"
#             )
#             return 4
#         limit = max(
#             1,
#             int_value(
#                 args.max_focused_u32_stability,
#                 MAX_PRINTED_FOCUSED_U32_STABILITY,
#             ),
#         )
#         print_focused_u32_stability(args.focused_u32_stability, limit)
#         return 0

#     if args.focused_u32_watchlist:
#         if len(args.focused_u32_watchlist) < 3:
#             print(
#                 "[Samase focused u32 watchlist] "
#                 "status=need_at_least_three_snapshots"
#             )
#             return 4
#         limit = max(
#             1,
#             int_value(
#                 args.max_focused_u32_watchlist,
#                 MAX_FOCUSED_U32_WATCHLIST_CANDIDATES,
#             ),
#         )
#         print_focused_u32_watchlist(
#             args.focused_u32_watchlist,
#             limit,
#             args.watchlist_out,
#         )
#         return 0

#     if args.resource_spec_template is not None:
#         print_resource_probe_spec_template(out_dir, args.resource_spec_template)
#         return 0

#     if args.resource_candidates:
#         limit = max(
#             1,
#             int_value(args.max_resource_candidates, MAX_RESOURCE_CANDIDATES),
#         )
#         print_resource_candidates(
#             args.resource_candidates,
#             limit,
#             args.resource_candidates_out,
#         )
#         return 0

#     if args.check:
#         results = check_probe_results(out_dir)
#         confirmed = False
#         for result in results:
#             if result["status"] in {
#                 "api_init_confirmed",
#                 "api_loader_confirmed",
#             }:
#                 confirmed = True
#             print(
#                 "[Samase plugin probe] "
#                 f"{result['candidate_id']} status={result['status']} "
#                 f"loader={result['loader'] or '-'} frame={result['frame_count']}"
#             )
#         return 0 if confirmed or not args.require_api else 2

#     manifest = prepare_probe_plan(
#         plugin_dll_path=args.plugin_dll,
#         samase_exe_path=args.samase_exe,
#         starcraft_x86_dir=args.starcraft_x86_dir,
#         out_dir=out_dir,
#         mod_args=args.mod_arg,
#         write_every_n_frames=args.write_every_n_frames,
#         heartbeat_interval_ms=args.heartbeat_ms,
#         resource_focused_scan=not args.disable_resource_focused_scan,
#         resource_focused_start_window=args.resource_focused_start_window,
#         resource_focused_windows=args.resource_focused_windows,
#     )
#     print(f"[Samase plugin probe] wrote {out_dir / 'probe_manifest.json'}")
#     for entry in manifest["entries"]:
#         print(
#             "[Samase plugin probe] "
#             f"{entry['candidate_id']} -> {entry['run_script']}"
#         )
#     return 0


# if __name__ == "__main__":
#     raise SystemExit(main())
