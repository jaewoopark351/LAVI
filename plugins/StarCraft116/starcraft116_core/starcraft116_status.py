#20260703_kpopmodder: Reads external StarCraft 1.16/BWAPI status without touching game memory.
import os
import time

from core.process import CalledProcessError
from .starcraft116_exporter import StarCraft116ExporterManager
from .starcraft116_status_io import (
    basename_starcraft116_path,
    decode_starcraft116_text,
    parse_starcraft116_tasklist_output,
    read_latest_starcraft116_jsonl_event,
    read_starcraft116_ini_values,
    read_starcraft116_tail_lines,
    starcraft116_tasklist_rows,
)


class StarCraft116StatusReader:
    #20260703_kpopmodder: Keeps BWAPI/Chaoslauncher verification read-only for runtime safety.
    PROCESS_NAMES = ("StarCraft.exe", "Chaoslauncher.exe")

    def __init__(self, config_manager):
        self.config_manager = config_manager
        self.exporter_manager = StarCraft116ExporterManager(config_manager)

    def snapshot(self, profile_name=None):
        profile_name = self._profile_name(profile_name)
        profile = self.config_manager.get_profile(profile_name)
        is_monster_profile = self._is_monster_profile(profile_name)
        processes = self.process_snapshot()
        exporter = self.exporter_manager.status(profile_name)
        bwapi_ini = self.bwapi_ini_snapshot(
            profile,
            exporter,
            skip_required=is_monster_profile,
            profile_name=profile_name,
        )
        chaoslauncher_log = self.chaoslauncher_log_snapshot(profile)
        game_events = self.game_events_snapshot(exporter)
        monster_log = (
            self.monster_log_snapshot(profile_name)
            if is_monster_profile
            else {}
        )
        readiness = self._readiness(
            processes,
            bwapi_ini,
            chaoslauncher_log,
            game_events,
            monster_log,
        )
        return {
            "profile": profile_name,
            "generated_at": time.time(),
            "processes": processes,
            "bwapi_ini": bwapi_ini,
            "bwapi_event_exporter": exporter,
            "game_events": game_events,
            "monster_log": monster_log,
            "chaoslauncher_log": chaoslauncher_log,
            "readiness": readiness,
            "summary": self._summary(
                processes,
                bwapi_ini,
                chaoslauncher_log,
                game_events,
                readiness,
                exporter,
                is_monster_profile=is_monster_profile,
            ),
        }

    def management_paths(self, profile_name=None):
        profile_name = self._profile_name(profile_name)
        profile = self.config_manager.get_profile(profile_name)
        chaoslauncher_path = self.config_manager.resolve_profile_path(
            profile,
            "chaoslauncher_path",
        )
        chaoslauncher_folder = self.config_manager.resolve_profile_path(
            profile,
            "chaoslauncher_working_dir",
        )
        if not chaoslauncher_folder and chaoslauncher_path:
            chaoslauncher_folder = os.path.dirname(chaoslauncher_path)

        starcraft_folder = self.config_manager.resolve_profile_path(
            profile,
            "starcraft_116_dir",
        )
        if not starcraft_folder:
            starcraft_path = self.config_manager.resolve_profile_path(
                profile,
                "starcraft_exe_path",
            )
            if starcraft_path:
                starcraft_folder = os.path.dirname(starcraft_path)

        return {
            "profile": profile_name,
            "bwapi_ini": self._bwapi_ini_path(profile, profile_name),
            "chaoslauncher_folder": chaoslauncher_folder,
            "starcraft_folder": starcraft_folder,
        }

    def process_snapshot(self):
        matches = {name: [] for name in self.PROCESS_NAMES}
        errors = []
        if os.name != "nt":
            return {
                "supported": False,
                "matches": matches,
                "errors": ["tasklist process scan is Windows-only."],
            }

        for process_name in self.PROCESS_NAMES:
            try:
                matches[process_name] = self._tasklist_rows(process_name)
            except CalledProcessError as e:
                message = str(e)
                if e.output:
                    message = str(e.output).strip()
                errors.append(f"{process_name}: {message}")
            except Exception as e:
                errors.append(f"{process_name}: {e}")

        return {
            "supported": True,
            "matches": matches,
            "errors": errors,
        }

    def bwapi_ini_snapshot(
        self,
        profile,
        exporter=None,
        skip_required=False,
        profile_name=None,
    ):
        path = self._bwapi_ini_path(profile, profile_name)
        exporter = exporter or {}
        result = {
            "path": path,
            "exists": bool(path and os.path.isfile(path)),
            "not_required": bool(skip_required),
            "values": {},
            "expected_bot_binary": self._basename(profile.get("bot_binary_path", "")),
            "expected_exporter_binary": self._basename(
                exporter.get("expected_ai_binary", "LAVIEventExporter.dll")
            ),
            "configured_ai_binary": "",
            "configured_ai_is_profile_bot": False,
            "configured_ai_is_exporter": False,
            "expected_bot_matches_ini": False,
            "error": "",
        }
        if skip_required:
            #20260705_kpopmodder: Monster is a standalone BWAPI client, not a DLL loaded through bwapi.ini.
            result["expected_bot_matches_ini"] = True
            return result
        if not result["exists"]:
            return result

        try:
            values = self._read_ini_values(path)
        except Exception as e:
            result["error"] = str(e)
            return result

        interesting_keys = ("ai", "ai_dbg", "race", "auto_menu", "map")
        result["values"] = {
            key: values[key]
            for key in interesting_keys
            if key in values
        }
        configured_ai = values.get("ai") or values.get("ai_dbg") or ""
        result["configured_ai_binary"] = self._basename(configured_ai)
        result["configured_ai_is_profile_bot"] = bool(
            result["expected_bot_binary"]
            and result["configured_ai_binary"]
            and result["expected_bot_binary"].lower()
            == result["configured_ai_binary"].lower()
        )
        result["configured_ai_is_exporter"] = bool(
            result["expected_exporter_binary"]
            and result["configured_ai_binary"]
            and result["expected_exporter_binary"].lower()
            == result["configured_ai_binary"].lower()
        )
        result["expected_bot_matches_ini"] = bool(
            result["configured_ai_is_profile_bot"]
            or result["configured_ai_is_exporter"]
        )
        return result

    def chaoslauncher_log_snapshot(self, profile):
        path = self._chaoslauncher_log_path(profile)
        result = {
            "path": path,
            "exists": bool(path and os.path.isfile(path)),
            "modified_at": None,
            "markers": {},
            "recent_relevant_lines": [],
            "error": "",
        }
        if not result["exists"]:
            return result

        try:
            result["modified_at"] = os.path.getmtime(path)
            lines = self._read_tail_lines(path)
        except Exception as e:
            result["error"] = str(e)
            return result

        marker_patterns = {
            "bwapi_release_plugin_loaded": "Plugin loaded BWAPI 4.4.0 Injector [RELEASE]",
            "bwapi_release_patch_applied": "ApplyPatch for BWAPI 4.4.0 Injector [RELEASE]",
            "wmode_loaded": "Plugin loaded W-MODE 1.02",
            "wmode_patch_applied": "ApplyPatch for W-MODE 1.02",
            "debug_privilege_obtained": "Obtained DebugPrivilege",
            "starcraft_start_requested": "Starting Game Starcraft 1.16.1",
            "starcraft_start_completed": "Starting Starcraft completed",
        }
        result["markers"] = {
            key: any(pattern in line for line in lines)
            for key, pattern in marker_patterns.items()
        }
        relevant_patterns = tuple(marker_patterns.values())
        result["recent_relevant_lines"] = [
            line
            for line in lines
            if any(pattern in line for pattern in relevant_patterns)
        ][-12:]
        return result

    def game_events_snapshot(self, exporter=None):
        exporter = exporter or {}
        path = self.config_manager.resolve_path_value(
            str(exporter.get("events_path", "") or "")
        )
        if not path:
            path = self.config_manager.resolve_game_events_path()

        result = {
            "path": path,
            "exists": bool(path and os.path.isfile(path)),
            "modified_at": None,
            "latest_event_type": "",
            "latest_summary": "",
            "latest_frame": None,
            "latest_game_time_seconds": None,
            "latest_age_seconds": None,
            "runtime_event_recent": False,
            "error": "",
        }
        if not result["exists"]:
            return result

        try:
            result["modified_at"] = os.path.getmtime(path)
            latest = self._read_latest_jsonl_event(path)
        except Exception as e:
            result["error"] = str(e)
            return result

        if not latest:
            return result

        result["latest_event_type"] = str(latest.get("event_type", "") or "")
        result["latest_summary"] = str(latest.get("summary", "") or "")
        result["latest_frame"] = latest.get("frame")
        result["latest_game_time_seconds"] = latest.get("game_time_seconds")
        result["latest_age_seconds"] = max(0.0, time.time() - result["modified_at"])
        #20260704_kpopmodder: Recent exporter events prove BWAPI is live even when Chaoslauncher logs miss ApplyPatch lines.
        result["runtime_event_recent"] = bool(
            result["latest_event_type"]
            and result["latest_event_type"] != "game_ended"
            and result["latest_age_seconds"] <= 180
        )
        return result

    def monster_log_snapshot(self, profile_name=None):
        #20260718_kpopmodder: Monster uses a standalone BWAPI observer, so its log is runtime evidence.
        path = self._monster_log_path(profile_name)
        result = {
            "path": path,
            "exists": bool(path and os.path.isfile(path)),
            "modified_at": None,
            "latest_age_seconds": None,
            "latest_relevant_marker": "",
            "latest_relevant_line": "",
            "runtime_event_recent": False,
            "observer_connected": False,
            "joined_game_seen": False,
            "disconnected_seen": False,
            "ended_seen": False,
            "recent_relevant_lines": [],
            "error": "",
        }
        if not result["exists"]:
            return result

        try:
            result["modified_at"] = os.path.getmtime(path)
            lines = self._read_tail_lines(path)
        except Exception as e:
            result["error"] = str(e)
            return result

        markers = []
        for line in lines:
            marker = self._monster_log_marker(line)
            if marker:
                markers.append((marker, line))

        result["recent_relevant_lines"] = [line for _, line in markers][-12:]
        if markers:
            latest_marker, latest_line = markers[-1]
            result["latest_relevant_marker"] = latest_marker
            result["latest_relevant_line"] = latest_line

        result["latest_age_seconds"] = max(0.0, time.time() - result["modified_at"])
        result["joined_game_seen"] = any(marker == "joined_game" for marker, _ in markers)
        result["disconnected_seen"] = any(
            marker == "disconnected" for marker, _ in markers
        )
        result["ended_seen"] = any(
            marker in {"game_ended", "exit_code"} for marker, _ in markers
        )
        result["observer_connected"] = result["latest_relevant_marker"] in {
            "connected",
            "joined_game",
            "lav_event",
        }
        result["runtime_event_recent"] = bool(
            result["observer_connected"]
            and result["latest_age_seconds"] <= 180
        )
        return result

    def _readiness(
        self,
        processes,
        bwapi_ini,
        chaoslauncher_log,
        game_events=None,
        monster_log=None,
    ):
        matches = processes.get("matches", {})
        markers = chaoslauncher_log.get("markers", {})
        game_events = game_events or {}
        monster_log = monster_log or {}
        return {
            "starcraft_process_running": bool(matches.get("StarCraft.exe")),
            "chaoslauncher_process_running": bool(matches.get("Chaoslauncher.exe")),
            "bwapi_ini_present": bool(bwapi_ini.get("exists")),
            "bwapi_ini_not_required": bool(bwapi_ini.get("not_required")),
            "bwapi_ai_configured": bool(bwapi_ini.get("configured_ai_binary")),
            "configured_bot_matches_ini": bool(
                bwapi_ini.get("expected_bot_matches_ini")
            ),
            "bwapi_event_exporter_configured": bool(
                bwapi_ini.get("configured_ai_is_exporter")
            ),
            "bwapi_release_plugin_loaded": bool(
                markers.get("bwapi_release_plugin_loaded")
            ),
            "bwapi_release_patch_applied": bool(
                markers.get("bwapi_release_patch_applied")
            ),
            "bwapi_runtime_event_seen": bool(
                game_events.get("runtime_event_recent")
            ),
            "monster_log_present": bool(monster_log.get("exists")),
            "monster_log_recent": bool(monster_log.get("runtime_event_recent")),
            "monster_observer_connected": bool(
                monster_log.get("observer_connected")
            ),
            "monster_joined_game_seen": bool(
                monster_log.get("joined_game_seen")
            ),
            "wmode_ready": bool(
                markers.get("wmode_loaded") or markers.get("wmode_patch_applied")
            ),
            "debug_privilege_obtained": bool(
                markers.get("debug_privilege_obtained")
            ),
            "starcraft_start_completed": bool(
                markers.get("starcraft_start_completed")
            ),
        }

    def _summary(
        self,
        processes,
        bwapi_ini,
        chaoslauncher_log,
        game_events,
        readiness,
        exporter=None,
        is_monster_profile=False,
    ):
        exporter = exporter or {}
        game_events = game_events or {}
        messages = []
        next_actions = []
        severity = "ok"
        phase = "idle"

        process_errors = list(processes.get("errors", []))
        if process_errors:
            severity = "warning"

        if is_monster_profile:
            messages.append(
                "Monster profile uses a standalone BWAPI client; bwapi.ini AI DLL checks are skipped."
            )
        elif not bwapi_ini.get("exists"):
            severity = "error"
            phase = "config_missing"
            messages.append("BWAPI ini was not found for the active profile.")
            next_actions.append("Check bwapi_data_dir and Stardust profile paths.")
        elif not bwapi_ini.get("configured_ai_binary"):
            severity = "warning"
            phase = "config_incomplete"
            messages.append("BWAPI ini is present, but ai/ai_dbg is not configured.")
            next_actions.append("Set ai and ai_dbg to bwapi-data/AI/Stardust.dll.")
        elif not bwapi_ini.get("expected_bot_matches_ini"):
            severity = "warning"
            phase = "config_mismatch"
            messages.append(
                "Active profile bot does not match the BWAPI ini ai setting."
            )
            next_actions.append("Confirm active_profile and bwapi.ini use the same bot DLL.")

        if readiness.get("starcraft_process_running"):
            if is_monster_profile:
                if readiness.get("monster_observer_connected"):
                    phase = "monster_observer_connected"
                    messages.append(
                        "Monster standalone BWAPI observer is connected."
                    )
                else:
                    phase = "monster_observer_waiting"
                    messages.append(
                        "StarCraft is running; waiting for Monster standalone observer log evidence."
                    )
                    next_actions.append(
                        "If Monster does not join, check BWAPI 4.2.0 Client Connection and W-MODE 1.02, then press Start."
                    )
            elif readiness.get("bwapi_release_patch_applied"):
                phase = "game_running"
                messages.append("StarCraft is running with BWAPI release patch evidence.")
            elif readiness.get("bwapi_runtime_event_seen"):
                phase = "game_running"
                messages.append("StarCraft is running with BWAPI runtime event evidence.")
            else:
                phase = "game_running"
                severity = "warning" if severity == "ok" else severity
                messages.append("StarCraft is running, but BWAPI patch evidence is missing.")
                next_actions.append("Refresh after Chaoslauncher finishes applying plugins.")
        elif readiness.get("chaoslauncher_process_running"):
            if readiness.get("starcraft_start_completed"):
                phase = "launcher_running_after_start"
                messages.append("Chaoslauncher is still running after a completed Start.")
            else:
                phase = "launcher_waiting_for_start"
                messages.append("Chaoslauncher is open and waiting for Start.")
                if is_monster_profile:
                    next_actions.append(
                        "In Chaoslauncher, check BWAPI 4.2.0 Client Connection and W-MODE 1.02, then press Start."
                    )
                else:
                    next_actions.append(
                        "In Chaoslauncher, check BWAPI 4.4.0 Injector [RELEASE] and W-MODE 1.02, then press Start."
                    )
        elif readiness.get("starcraft_start_completed"):
            phase = "last_run_completed_or_exited"
            severity = "warning" if severity == "ok" else severity
            messages.append(
                "Latest Chaoslauncher log shows StarCraft completed startup, but no process is currently detected."
            )
        elif readiness.get("bwapi_release_plugin_loaded"):
            phase = "last_launcher_log_only"
            severity = "warning" if severity == "ok" else severity
            messages.append(
                "Latest Chaoslauncher log shows BWAPI loaded, but no current process is detected."
            )
            next_actions.append("Use Launch BWAPI Profile to open elevated Chaoslauncher.")
        elif phase == "idle":
            messages.append("StarCraft 1.16 is not currently detected.")
            next_actions.append("Use Launch BWAPI Profile when ready.")

        bot = bwapi_ini.get("configured_ai_binary")
        if bot:
            if bwapi_ini.get("configured_ai_is_exporter"):
                wrapped_ai = exporter.get("configured_wrapped_ai") or "the wrapped bot"
                messages.append(
                    f"BWAPI ai is configured as {bot}, wrapping {wrapped_ai}."
                )
                if readiness.get("bwapi_runtime_event_seen"):
                    latest = game_events.get("latest_event_type") or "event"
                    messages.append(
                        f"LAVEventExporter is writing game events ({latest})."
                    )
                if not exporter.get("wrapped_ai_matches_profile"):
                    severity = "warning" if severity == "ok" else severity
                    next_actions.append(
                        "Check LAVEventExporter.ini wrapped_ai against the active profile bot."
                    )
            else:
                messages.append(f"BWAPI ai is configured as {bot}.")

        if (
            exporter.get("enabled")
            and not is_monster_profile
            and not bwapi_ini.get("configured_ai_is_exporter")
        ):
            severity = "warning" if severity == "ok" else severity
            messages.append(
                "BWAPI event exporter is enabled in LAV, but bwapi.ini is not using LAVIEventExporter.dll."
            )
            next_actions.append(
                "Set ai and ai_dbg to bwapi-data/AI/LAVIEventExporter.dll after installing the exporter."
            )

        if process_errors:
            messages.append("Process scan reported warnings.")
            next_actions.extend(process_errors)

        return {
            "phase": phase,
            "severity": severity,
            "message": messages[0] if messages else "",
            "messages": messages,
            "next_actions": next_actions,
        }

    def _profile_name(self, profile_name):
        profile_name = str(profile_name or "").strip()
        if profile_name in self.config_manager.profile_names():
            return profile_name
        return self.config_manager.get_active_profile_name()

    @staticmethod
    def _is_monster_profile(profile_name):
        return str(profile_name or "").strip().lower() == "monster"

    def _bwapi_ini_path(self, profile, profile_name=None):
        runtime_resolver = getattr(
            self.config_manager,
            "resolve_profile_runtime_bwapi_data_dir",
            None,
        )
        if callable(runtime_resolver):
            bwapi_data_dir = runtime_resolver(profile_name)
            if bwapi_data_dir:
                return os.path.join(bwapi_data_dir, "bwapi.ini")

        bwapi_data_dir = self.config_manager.resolve_profile_path(
            profile,
            "bwapi_data_dir",
        )
        if bwapi_data_dir:
            return os.path.join(bwapi_data_dir, "bwapi.ini")

        starcraft_dir = self.config_manager.resolve_profile_path(
            profile,
            "starcraft_116_dir",
        )
        if starcraft_dir:
            return os.path.join(starcraft_dir, "bwapi-data", "bwapi.ini")
        return ""

    def _chaoslauncher_log_path(self, profile):
        working_dir = self.config_manager.resolve_profile_path(
            profile,
            "chaoslauncher_working_dir",
        )
        if not working_dir:
            chaoslauncher_path = self.config_manager.resolve_profile_path(
                profile,
                "chaoslauncher_path",
            )
            if chaoslauncher_path:
                working_dir = os.path.dirname(chaoslauncher_path)
        if not working_dir:
            return ""
        return os.path.join(working_dir, "Chaoslauncher.log")

    def _monster_log_path(self, profile_name=None):
        resolver = getattr(self.config_manager, "resolve_monster_log_path", None)
        if not callable(resolver):
            return ""
        try:
            return resolver(profile_name)
        except TypeError:
            return resolver()

    @staticmethod
    def _monster_log_marker(line):
        lowered = str(line or "").strip().lower()
        if not lowered:
            return ""
        if lowered.startswith("[lav_event]") or lowered.startswith("lav_event"):
            return "lav_event"
        if lowered in {
            "connected",
            "connection successful",
            "connected to bwapi.",
        }:
            return "connected"
        if lowered == "joined a game.":
            return "joined_game"
        if lowered == "disconnected":
            return "disconnected"
        if lowered == "game ended.":
            return "game_ended"
        if "exit code" in lowered:
            return "exit_code"
        return ""

    def _read_ini_values(self, path):
        #20260706_kpopmodder: Preserve private method for tests while IO parsing lives in helper.
        return read_starcraft116_ini_values(path)

    def _read_tail_lines(self, path, line_count=80, max_bytes=65536):
        return read_starcraft116_tail_lines(
            path,
            line_count=line_count,
            max_bytes=max_bytes,
        )

    def _read_latest_jsonl_event(self, path, line_count=80):
        return read_latest_starcraft116_jsonl_event(path, line_count=line_count)

    def _tasklist_rows(self, process_name):
        return starcraft116_tasklist_rows(process_name)

    @staticmethod
    def _parse_tasklist_output(output):
        return parse_starcraft116_tasklist_output(output)

    @staticmethod
    def _basename(value):
        return basename_starcraft116_path(value)

    @staticmethod
    def _decode_text(data):
        return decode_starcraft116_text(data)
