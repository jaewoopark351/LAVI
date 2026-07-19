#20260703_kpopmodder: Tracks the optional BWAPI AIModule proxy that emits StarCraft 1.16 JSONL events.
import os
import shutil


class StarCraft116ExporterManager:
    #20260703_kpopmodder: Keeps exporter installation checks separate from launch/status logic.
    EXPORTER_DLL_NAME = "LAVEventExporter.dll"
    EXPORTER_INI_NAME = "LAVEventExporter.ini"
    STARDUST_PROFILE_NAME = "stardust"
    DEFAULT_EXPORTER_PROFILE_NAMES = {
        "stardust",
        "crona",
        "terminus",
    }

    def __init__(self, config_manager):
        self.config_manager = config_manager

    def status(self, profile_name=None):
        profile_name = self._profile_name(profile_name)
        profile = self.config_manager.get_profile(profile_name)
        paths = self.paths(profile, profile_name)
        config_values = self.read_exporter_ini(paths["target_ini_path"])
        configured_wrapped_ai = config_values.get("wrapped_ai", "")
        wrapped_ai = os.path.basename(configured_wrapped_ai)
        expected_wrapped_ai = os.path.basename(profile.get("bot_binary_path", ""))
        return {
            "enabled": self.should_use_exporter(profile_name),
            "source_dir": paths["source_dir"],
            "project_path": paths["project_path"],
            "source_dll_path": paths["source_dll_path"],
            "source_dll_exists": bool(
                paths["source_dll_path"]
                and os.path.isfile(paths["source_dll_path"])
            ),
            "target_dll_path": paths["target_dll_path"],
            "target_dll_exists": bool(
                paths["target_dll_path"]
                and os.path.isfile(paths["target_dll_path"])
            ),
            "target_ini_path": paths["target_ini_path"],
            "target_ini_exists": bool(
                paths["target_ini_path"]
                and os.path.isfile(paths["target_ini_path"])
            ),
            "expected_bwapi_ai": self.expected_bwapi_ai_path(),
            "expected_ai_binary": self.EXPORTER_DLL_NAME,
            "expected_wrapped_ai": expected_wrapped_ai,
            "configured_wrapped_ai": wrapped_ai,
            "configured_wrapped_ai_path": configured_wrapped_ai,
            "wrapped_ai_matches_profile": self._wrapped_ai_matches_profile(
                configured_wrapped_ai,
                profile,
                profile_name,
            ),
            "events_path": config_values.get(
                "events_path",
                self.config_manager.resolve_game_events_path(),
            ),
            "config_values": config_values,
        }

    def paths(self, profile, profile_name=None):
        source_dir = os.path.join(
            self.config_manager.plugin_root,
            "bwapi_event_exporter",
        )
        build_config = str(
            self.config_manager.get(
                "bwapi_event_exporter_build_config",
                "Release",
            )
            or "Release"
        )
        source_dll = self.config_manager.resolve_path_value(str(
            self.config_manager.get("bwapi_event_exporter_source_dll_path", "")
            or ""
        ))
        if not source_dll:
            source_dll = os.path.join(
                source_dir,
                "bin",
                build_config,
                self.EXPORTER_DLL_NAME,
            )

        ai_dir = self._ai_dir(profile, profile_name)
        target_dll = os.path.join(ai_dir, self.EXPORTER_DLL_NAME) if ai_dir else ""
        target_ini = os.path.join(ai_dir, self.EXPORTER_INI_NAME) if ai_dir else ""
        return {
            "source_dir": source_dir,
            "project_path": os.path.join(source_dir, "LAVEventExporter.vcxproj"),
            "source_dll_path": source_dll,
            "target_dll_path": target_dll,
            "target_ini_path": target_ini,
        }

    def expected_bwapi_ai_path(self):
        return f"bwapi-data/AI/{self.EXPORTER_DLL_NAME}"

    def should_use_exporter(self, profile_name=None):
        profile_name = self._profile_name(profile_name)
        if self.config_manager.get_bool("bwapi_event_exporter_enabled", False):
            return True
        profile_key = profile_name.lower()
        if profile_key not in self.DEFAULT_EXPORTER_PROFILE_NAMES:
            return False
        #20260719_kpopmodder: These DLL bots run through LAVEventExporter by default so LAV receives BWAPI events.
        return self.config_manager.get_bool(
            f"bwapi_event_exporter_{profile_key}_enabled",
            True,
        )

    def build_ini_text(self, profile_name=None):
        profile_name = self._profile_name(profile_name)
        profile = self.config_manager.get_profile(profile_name)
        wrapped_ai = self._wrapped_ai_value(profile, profile_name)
        lines = [
            "#20260703_kpopmodder: LAV StarCraft 1.16 BWAPI event exporter config.",
            f"wrapped_ai={wrapped_ai}",
            f"events_path={self.config_manager.resolve_game_events_path()}",
            "snapshot_interval_frames=144",
            "combat_cooldown_frames=96",
            "supply_block_cooldown_frames=240",
            "",
        ]
        return "\n".join(lines)

    def write_ini(self, profile_name=None):
        profile_name = self._profile_name(profile_name)
        profile = self.config_manager.get_profile(profile_name)
        paths = self.paths(profile, profile_name)
        source_dll = paths["source_dll_path"]
        target_dll = paths["target_dll_path"]
        target_ini = paths["target_ini_path"]
        if not target_ini:
            return False, "LAVEventExporter.ini target path is not configured."
        if not source_dll or not os.path.isfile(source_dll):
            return False, f"LAVEventExporter.dll source does not exist: {source_dll}"
        if not target_dll:
            return False, "LAVEventExporter.dll target path is not configured."

        target_dir = os.path.dirname(target_ini)
        if target_dir:
            os.makedirs(target_dir, exist_ok=True)
        self._ensure_exporter_profile_runtime_dirs(profile, profile_name)
        #20260718_kpopmodder: BWAPI loads the exporter DLL from bwapi-data/AI, not from the build output folder.
        shutil.copy2(source_dll, target_dll)
        with open(target_ini, "w", encoding="utf-8", newline="\n") as file:
            file.write(self.build_ini_text(profile_name))
        return True, (
            f"Updated LAVEventExporter for {profile_name}: "
            f"ini={target_ini}, dll={target_dll}"
        )

    def write_bwapi_ini_ai(self, profile_name=None, use_exporter=None):
        #20260704_kpopmodder: BWAPI reads this file for DLL bots, so set ai/ai_dbg to the selected profile before launch.
        profile_name = self._profile_name(profile_name)
        profile = self.config_manager.get_profile(profile_name)
        self._ensure_exporter_profile_runtime_dirs(profile, profile_name)
        ai_value = self._bwapi_ai_value(
            profile,
            use_exporter=use_exporter,
            profile_name=profile_name,
        )
        if not ai_value:
            return True, "Skipped bwapi.ini AI update for standalone BWAPI client bot."

        bwapi_ini = self._bwapi_ini_path(profile, profile_name)
        if not bwapi_ini:
            return False, "bwapi.ini target path is not configured."
        if not os.path.isfile(bwapi_ini):
            return False, f"bwapi.ini does not exist: {bwapi_ini}"

        self._write_ini_values(
            bwapi_ini,
            "ai",
            {
                "ai": ai_value,
                "ai_dbg": ai_value,
            },
        )
        return True, f"Updated bwapi.ini AI for {profile_name}: {ai_value}"

    def _wrapped_ai_value(self, profile, profile_name=None):
        bot_path = self.config_manager.resolve_profile_path(
            profile,
            "bot_binary_path",
        )
        if not bot_path:
            return "Stardust.dll"

        bot_basename = os.path.basename(bot_path)
        ai_dir = self._ai_dir(profile, profile_name)
        if ai_dir:
            try:
                bot_dir = os.path.normcase(os.path.dirname(bot_path))
                expected_ai_dir = os.path.normcase(ai_dir)
                if bot_dir == expected_ai_dir:
                    return bot_basename
                #20260719_kpopmodder: Stardust is sidecar-sensitive; prefer the runtime AI copy next to LAVEventExporter when present.
                if (
                    self._profile_name(profile_name).lower() == self.STARDUST_PROFILE_NAME
                    and bot_basename
                    and os.path.isfile(os.path.join(ai_dir, bot_basename))
                ):
                    return bot_basename
            except Exception:
                pass
        return bot_path

    def _wrapped_ai_matches_profile(self, configured_wrapped_ai, profile, profile_name=None):
        configured_wrapped_ai = str(configured_wrapped_ai or "").strip()
        expected_wrapped_ai = str(self._wrapped_ai_value(profile, profile_name) or "").strip()
        if not configured_wrapped_ai or not expected_wrapped_ai:
            return False
        if os.path.isabs(configured_wrapped_ai) or os.path.isabs(expected_wrapped_ai):
            return (
                os.path.normcase(os.path.normpath(configured_wrapped_ai))
                == os.path.normcase(os.path.normpath(expected_wrapped_ai))
            )
        return os.path.basename(configured_wrapped_ai).lower() == os.path.basename(
            expected_wrapped_ai
        ).lower()

    def _bwapi_ai_value(self, profile, use_exporter=None, profile_name=None):
        bot_path = self.config_manager.resolve_profile_path(
            profile,
            "bot_binary_path",
        )
        #20260704_kpopmodder: Monster-style EXE bots connect as BWAPI clients and must not be written as AI DLLs.
        if os.path.splitext(bot_path or "")[1].lower() == ".exe":
            return ""

        if use_exporter is None:
            use_exporter = self.should_use_exporter(profile_name)
        #20260704_kpopmodder: Use the exporter proxy when available so LAV can receive game events while wrapping the real bot.
        if use_exporter:
            return self.expected_bwapi_ai_path()

        if not bot_path:
            return ""

        ai_dir = self._ai_dir(profile, profile_name)
        if ai_dir:
            try:
                bot_dir = os.path.normcase(os.path.dirname(bot_path))
                expected_ai_dir = os.path.normcase(ai_dir)
                if bot_dir == expected_ai_dir:
                    return f"bwapi-data/AI/{os.path.basename(bot_path)}"
            except Exception:
                pass
        return bot_path

    def _bwapi_ini_path(self, profile, profile_name=None):
        runtime_bwapi_data_dir = self._runtime_bwapi_data_dir(profile_name)
        if runtime_bwapi_data_dir:
            return os.path.join(runtime_bwapi_data_dir, "bwapi.ini")

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

    def _write_ini_values(self, path, section, values):
        #20260704_kpopmodder: Keep BWAPI's sample ini mostly intact while replacing only the AI keys.
        with open(path, "r", encoding="utf-8", errors="replace") as file:
            lines = file.read().splitlines()

        section_header = f"[{section.lower()}]"
        in_section = False
        section_found = False
        written = set()
        output = []

        for line in lines:
            stripped = line.strip()
            is_header = stripped.startswith("[") and stripped.endswith("]")
            if is_header:
                if in_section:
                    for key, value in values.items():
                        if key not in written:
                            output.append(f"{key}     = {value}")
                            written.add(key)
                in_section = stripped.lower() == section_header
                section_found = section_found or in_section
                output.append(line)
                continue

            if in_section and "=" in stripped and not stripped.startswith((";", "#")):
                key = stripped.split("=", 1)[0].strip().lower()
                if key in values:
                    output.append(f"{key}     = {values[key]}")
                    written.add(key)
                    continue

            output.append(line)

        if in_section:
            for key, value in values.items():
                if key not in written:
                    output.append(f"{key}     = {value}")
                    written.add(key)

        if not section_found:
            output.extend(["", f"[{section}]"])
            for key, value in values.items():
                output.append(f"{key}     = {value}")

        with open(path, "w", encoding="utf-8", newline="\n") as file:
            file.write("\n".join(output) + "\n")

    def read_exporter_ini(self, path):
        values = {}
        if not path or not os.path.isfile(path):
            return values
        with open(path, "r", encoding="utf-8", errors="replace") as file:
            for line in file:
                stripped = line.strip()
                if not stripped or stripped[0] in {"#", ";"} or "=" not in stripped:
                    continue
                key, value = stripped.split("=", 1)
                values[key.strip().lower()] = value.strip().strip("\"'")
        return values

    def _profile_name(self, profile_name):
        profile_name = str(profile_name or "").strip()
        if profile_name in self.config_manager.profile_names():
            return profile_name
        return self.config_manager.get_active_profile_name()

    def _ai_dir(self, profile, profile_name=None):
        runtime_bwapi_data_dir = self._runtime_bwapi_data_dir(profile_name)
        if runtime_bwapi_data_dir:
            return os.path.join(runtime_bwapi_data_dir, "AI")

        bwapi_data_dir = self.config_manager.resolve_profile_path(
            profile,
            "bwapi_data_dir",
        )
        if bwapi_data_dir:
            return os.path.join(bwapi_data_dir, "AI")
        starcraft_dir = self.config_manager.resolve_profile_path(
            profile,
            "starcraft_116_dir",
        )
        if starcraft_dir:
            return os.path.join(starcraft_dir, "bwapi-data", "AI")
        return ""

    def _runtime_bwapi_data_dir(self, profile_name):
        resolver = getattr(
            self.config_manager,
            "resolve_profile_runtime_bwapi_data_dir",
            None,
        )
        if callable(resolver):
            return resolver(profile_name)
        return ""

    def _ensure_exporter_profile_runtime_dirs(self, profile, profile_name=None):
        if self._profile_name(profile_name).lower() not in self.DEFAULT_EXPORTER_PROFILE_NAMES:
            return
        ai_dir = self._ai_dir(profile, profile_name)
        if not ai_dir:
            return
        bwapi_data_dir = os.path.dirname(ai_dir)
        if not bwapi_data_dir:
            return
        #20260719_kpopmodder: Exporter-wrapped tournament bots may write logs, opponent data, and CherryVis output under bwapi-data\write.
        for relative_path in (
            "read",
            "write",
            os.path.join("write", "cvis"),
        ):
            os.makedirs(os.path.join(bwapi_data_dir, relative_path), exist_ok=True)
