#20260702_kpopmodder: Keeps StarCraft 1.16 BWAPI bot paths local and optional.
import copy
import json
import os
from dataclasses import dataclass, field


DEFAULT_PROFILE = {
    "display_name": "",
    "starcraft_116_dir": "",
    "bwapi_data_dir": "",
    "bot_binary_path": "",
    "start_chaoslauncher": True,
    "chaoslauncher_path": "",
    "chaoslauncher_arguments": [],
    "chaoslauncher_working_dir": "",
    "chaoslauncher_run_as_admin": False,
    "start_starcraft": False,
    "starcraft_exe_path": "",
    "starcraft_arguments": [],
    "starcraft_working_dir": "",
    "starcraft_run_as_admin": False,
    "start_bot_process": False,
    "bot_process_path": "",
    "bot_process_arguments": [],
    "bot_process_working_dir": "",
    "bot_process_run_as_admin": False,
    "start_observer_process": False,
    "observer_process_path": "",
    "observer_process_arguments": [],
    "observer_process_working_dir": "",
    "observer_process_run_as_admin": False,
    "environment": {},
}


KNOWN_BOT_PROFILES = {
    "saida": {
        "display_name": "SAIDA",
        "aliases": ("saida",),
    },
    "monster": {
        "display_name": "Monster",
        "aliases": ("monster",),
    },
    "stardust": {
        "display_name": "Stardust",
        "race_label": "프로토스",
        "aliases": ("stardust",),
    },
    "crona": {
        "display_name": "Crona (BananaBrain)",
        "race_label": "저그",
        "aliases": ("crona",),
    },
    "terminus": {
        "display_name": "Terminus (BananaBrain)",
        "race_label": "테란",
        "aliases": ("terminus",),
    },
}


DEFAULT_CONFIG = {
    "enabled": False,
    "active_profile": "saida",
    "auto_launch": False,
    "terminate_on_stop": False,
    "write_state_log": True,
    "state_log_path": "logs\\starcraft116_state.jsonl",
    "openai_reactions_enabled": True,
    "game_events_enabled": True,
    "game_events_path": "logs\\starcraft116_game_events.jsonl",
    "game_events_poll_interval_sec": 1.0,
    "game_events_reaction_cooldown_sec": 8.0,
    "game_events_max_events_per_poll": 6,
    "monster_log_events_enabled": True,
    "monster_log_tts_enabled": False,
    "monster_log_path": "",
    "bwapi_proxy_events_enabled": True,
    "bwapi_proxy_events_tts_enabled": True,
    "bwapi_proxy_events_path": "",
    "bwapi_event_exporter_enabled": False,
    "bwapi_event_exporter_build_config": "Release",
    "bwapi_event_exporter_source_dll_path": "",
    "profiles": {
        "saida": {
            "display_name": "SAIDA",
        },
        "monster": {
            "display_name": "Monster",
        },
        "stardust": {
            "display_name": "Stardust",
        },
        "crona": {
            "display_name": "Crona (BananaBrain)",
        },
        "terminus": {
            "display_name": "Terminus (BananaBrain)",
        },
    },
}


@dataclass
class StarCraft116PathCheck:
    ok: bool
    messages: list = field(default_factory=list)

    def message(self):
        if not self.messages:
            return "StarCraft 1.16 paths are ready."
        return "\n".join(str(message) for message in self.messages)


@dataclass
class StarCraft116Discovery:
    #20260702_kpopmodder: Represents local StarCraft 1.16/BWAPI install scan results.
    ok: bool
    root_dir: str
    messages: list = field(default_factory=list)
    starcraft_exe_path: str = ""
    chaoslauncher_path: str = ""
    bwapi_data_dir: str = ""
    ai_dir: str = ""
    bot_files: list = field(default_factory=list)

    def message(self):
        if not self.messages:
            return "StarCraft 1.16 install scan completed."
        return "\n".join(str(message) for message in self.messages)

    def to_dict(self):
        return {
            "ok": self.ok,
            "root_dir": self.root_dir,
            "starcraft_exe_path": self.starcraft_exe_path,
            "chaoslauncher_path": self.chaoslauncher_path,
            "bwapi_data_dir": self.bwapi_data_dir,
            "ai_dir": self.ai_dir,
            "bot_files": list(self.bot_files),
            "messages": list(self.messages),
        }


class StarCraft116Config:
    #20260702_kpopmodder: Supports BWAPI-era bot profiles without bundling game files.
    def __init__(self, plugin_root=None):
        self.plugin_root = plugin_root or os.path.dirname(__file__)
        self.project_root = os.path.dirname(os.path.dirname(self.plugin_root))
        self.config_dir = os.path.join(self.plugin_root, "config")
        self.config_path = os.path.join(
            self.config_dir,
            "starcraft116_config.json",
        )
        self.example_config_path = os.path.join(
            self.config_dir,
            "starcraft116_config.example.json",
        )
        self.config = self._default_config()
        self.config_exists = False
        self.load_error = ""
        self.load()

    def load(self):
        self.config = self._default_config()
        self.config_exists = os.path.exists(self.config_path)
        self.load_error = ""

        if not self.config_exists:
            return self.config

        try:
            with open(self.config_path, "r", encoding="utf-8") as file:
                loaded = json.load(file)
        except Exception as e:
            self.load_error = str(e)
            return self.config

        if not isinstance(loaded, dict):
            self.load_error = "config root must be a JSON object"
            return self.config

        self._merge_loaded_config(loaded)
        return self.config

    def reload(self):
        return self.load()

    def get(self, key, default=None):
        return self.config.get(key, default)

    def get_bool(self, key, default=False):
        value = self.config.get(key, default)
        if isinstance(value, bool):
            return value
        return str(value).strip().lower() in {"1", "true", "yes", "on"}

    def get_float(self, key, default=0.0):
        try:
            return float(self.config.get(key, default))
        except (TypeError, ValueError):
            return float(default)

    def get_int(self, key, default=0):
        try:
            return int(self.config.get(key, default))
        except (TypeError, ValueError):
            return int(default)

    def profile_names(self):
        profiles = self.config.get("profiles", {})
        if not isinstance(profiles, dict):
            return []
        return list(profiles.keys())

    def profile_dropdown_choices(self):
        return [
            (self.profile_dropdown_label(profile_name), profile_name)
            for profile_name in self.profile_names()
        ]

    def profile_dropdown_label(self, profile_name):
        profile_name = str(profile_name or "").strip()
        if not profile_name:
            return ""
        race_label = self._race_label_from_profile(profile_name)
        if race_label:
            return f"{profile_name}_{race_label}"
        profile = self.get_profile(profile_name)
        display_name = str(profile.get("display_name", "") or "").strip()
        return display_name or profile_name

    def get_active_profile_name(self):
        profile_name = str(self.config.get("active_profile", "saida") or "saida")
        if profile_name in self.config.get("profiles", {}):
            return profile_name
        names = self.profile_names()
        return names[0] if names else "saida"

    def set_active_profile(self, profile_name):
        profile_name = str(profile_name or "").strip()
        if profile_name in self.config.get("profiles", {}):
            self.config["active_profile"] = profile_name
        return self.get_active_profile_name()

    def get_active_profile(self):
        return self.get_profile(self.get_active_profile_name())

    def get_profile(self, profile_name):
        profiles = self.config.get("profiles", {})
        profile = profiles.get(profile_name, {})
        if not isinstance(profile, dict):
            return copy.deepcopy(DEFAULT_PROFILE)
        return profile

    def get_profile_bool(self, profile, key, default=False):
        value = profile.get(key, default)
        if isinstance(value, bool):
            return value
        return str(value).strip().lower() in {"1", "true", "yes", "on"}

    def config_message(self):
        if not self.config_exists:
            return (
                "StarCraft 1.16 config missing. Copy "
                f"{self.example_config_path} to {self.config_path} and set "
                "your local Chaoslauncher, BWAPI, and bot paths."
            )

        if self.load_error:
            return f"StarCraft 1.16 config load failed: {self.load_error}"

        if not self.get_bool("enabled", False):
            return "StarCraft 1.16 config loaded. enabled=false in plugin config."

        return (
            "StarCraft 1.16 config loaded. active_profile="
            f"{self.get_active_profile_name()}"
        )

    def resolve_state_log_path(self):
        path = self.resolve_path_value(str(self.config.get("state_log_path", "")))
        if path:
            return path
        return os.path.join(self.project_root, "logs", "starcraft116_state.jsonl")

    def resolve_game_events_path(self):
        path = self.resolve_path_value(str(self.config.get("game_events_path", "")))
        if path:
            return path
        return os.path.join(self.project_root, "logs", "starcraft116_game_events.jsonl")

    def resolve_monster_log_path(self, profile_name=None):
        #20260705_kpopmodder: Monster.exe is a standalone client, so its text log is the event source.
        path = self.resolve_path_value(str(self.config.get("monster_log_path", "")))
        if path:
            return path

        profile = self.get_profile(profile_name or self.get_active_profile_name())
        working_dir = self.resolve_profile_path(profile, "bot_process_working_dir")
        if working_dir:
            return os.path.join(working_dir, "monster_log.txt")

        bot_path = self.resolve_profile_path(profile, "bot_process_path")
        if not bot_path:
            bot_path = self.resolve_profile_path(profile, "bot_binary_path")
        if bot_path:
            return os.path.join(os.path.dirname(bot_path), "monster_log.txt")
        return ""

    def resolve_bwapi_proxy_events_path(self, profile_name=None):
        #20260705_kpopmodder: The Monster BWAPI proxy writes JSONL beside StarCraft's loaded BWAPI.dll.
        path = self.resolve_path_value(str(self.config.get("bwapi_proxy_events_path", "")))
        if path:
            return path

        profile = self.get_profile(profile_name or self.get_active_profile_name())
        for key in ("bwapi_data_dir", "starcraft_working_dir", "starcraft_116_dir"):
            directory = self.resolve_profile_path(profile, key)
            if not directory:
                continue
            if os.path.basename(directory).lower() == "bwapi-data":
                return os.path.join(directory, "bwapi_proxy_events.jsonl")
            candidate = os.path.join(directory, "bwapi-data")
            if os.path.isdir(candidate):
                return os.path.join(candidate, "bwapi_proxy_events.jsonl")
        return ""

    def resolve_profile_path(self, profile, key):
        return self.resolve_path_value(str(profile.get(key, "") or ""))

    def resolve_path_value(self, value):
        value = str(value or "").strip().strip("\"'")
        if not value:
            return ""

        value = os.path.expandvars(os.path.expanduser(value))
        if os.path.isabs(value):
            return os.path.normpath(value)
        return os.path.normpath(os.path.join(self.project_root, value))

    def validate_paths(self, profile_name=None):
        self.reload()
        messages = []

        if not self.config_exists:
            return StarCraft116PathCheck(False, [self.config_message()])

        if self.load_error:
            return StarCraft116PathCheck(False, [self.config_message()])

        if profile_name is not None:
            self.set_active_profile(profile_name)
        profile_name = self.get_active_profile_name()
        profile = self.get_profile(profile_name)
        launch_checks = (
            (
                "start_chaoslauncher",
                "chaoslauncher_path",
                "file",
                "Chaoslauncher executable",
            ),
            ("start_starcraft", "starcraft_exe_path", "file", "StarCraft executable"),
            (
                "start_bot_process",
                "bot_process_path",
                "file",
                "BWAPI bot process",
            ),
            (
                "start_observer_process",
                "observer_process_path",
                "file",
                "BWAPI observer process",
            ),
        )
        selected = False
        for flag_key, path_key, path_type, label in launch_checks:
            if not self.get_profile_bool(profile, flag_key, False):
                continue
            selected = True
            self._check_required_path(messages, profile, path_key, path_type, label)

        if not selected:
            messages.append(
                f"Profile {profile_name} has no launch target enabled."
            )

        optional_checks = (
            ("starcraft_116_dir", "directory", "StarCraft 1.16 directory"),
            ("bwapi_data_dir", "directory", "BWAPI data directory"),
            ("bot_binary_path", "file", "BWAPI bot binary"),
            ("chaoslauncher_working_dir", "directory", "Chaoslauncher working dir"),
            ("starcraft_working_dir", "directory", "StarCraft working dir"),
            ("bot_process_working_dir", "directory", "Bot process working dir"),
            (
                "observer_process_working_dir",
                "directory",
                "Observer process working dir",
            ),
        )
        for path_key, path_type, label in optional_checks:
            path = self.resolve_profile_path(profile, path_key)
            if path:
                self._check_path_exists(messages, path, path_type, label)

        if messages:
            return StarCraft116PathCheck(False, messages)

        return StarCraft116PathCheck(
            True,
            [f"StarCraft 1.16 profile {profile_name} paths look ready."],
        )

    def discover_install(self, root_dir):
        root_dir = self.resolve_path_value(root_dir)
        messages = []
        if not root_dir:
            return StarCraft116Discovery(
                ok=False,
                root_dir="",
                messages=["StarCraft 1.16 install folder is not configured."],
            )

        if not os.path.isdir(root_dir):
            return StarCraft116Discovery(
                ok=False,
                root_dir=root_dir,
                messages=[f"StarCraft 1.16 folder does not exist: {root_dir}"],
            )

        starcraft_exe_path = self._find_first_file(
            root_dir,
            (
                "StarCraft.exe",
                "Starcraft.exe",
            ),
        )
        chaoslauncher_path = self._find_first_file(
            root_dir,
            (
                "Chaoslauncher.exe",
                "ChaosLauncher.exe",
            ),
        )
        bwapi_data_dir = self._find_first_dir(root_dir, ("bwapi-data",))
        ai_dir = ""
        bot_files = []
        if bwapi_data_dir:
            ai_dir = self._find_first_dir(bwapi_data_dir, ("AI", "ai"))
            if ai_dir:
                bot_files = self._find_bot_files(ai_dir)
        bot_files = self._merge_bot_files(
            bot_files,
            self._find_loose_bot_files(root_dir),
        )

        if starcraft_exe_path:
            messages.append(f"Found StarCraft executable: {starcraft_exe_path}")
        else:
            messages.append("Missing StarCraft.exe in the install folder.")

        if chaoslauncher_path:
            messages.append(f"Found Chaoslauncher: {chaoslauncher_path}")
        else:
            messages.append("Missing Chaoslauncher.exe.")

        if bwapi_data_dir:
            messages.append(f"Found bwapi-data: {bwapi_data_dir}")
        else:
            messages.append("Missing bwapi-data folder.")

        if bot_files:
            messages.append(
                "Found BWAPI bot files: "
                + ", ".join(os.path.basename(path) for path in bot_files)
            )
        else:
            messages.append("No BWAPI bot DLL/EXE files found under bwapi-data\\AI.")

        ok = bool(
            starcraft_exe_path
            and chaoslauncher_path
            and bwapi_data_dir
            and bot_files
        )
        return StarCraft116Discovery(
            ok=ok,
            root_dir=root_dir,
            messages=messages,
            starcraft_exe_path=starcraft_exe_path,
            chaoslauncher_path=chaoslauncher_path,
            bwapi_data_dir=bwapi_data_dir,
            ai_dir=ai_dir,
            bot_files=bot_files,
        )

    def build_config_from_install(self, root_dir):
        discovery = self.discover_install(root_dir)
        generated = self._default_config()
        generated["enabled"] = bool(discovery.ok)
        generated["auto_launch"] = False
        generated["terminate_on_stop"] = False
        profiles = {}

        bot_files = list(discovery.bot_files)
        if not bot_files:
            profile = copy.deepcopy(DEFAULT_PROFILE)
            profile["display_name"] = "StarCraft 1.16"
            profile["starcraft_116_dir"] = discovery.root_dir
            profile["bwapi_data_dir"] = discovery.bwapi_data_dir
            profile["start_chaoslauncher"] = False
            profile["chaoslauncher_path"] = discovery.chaoslauncher_path
            profile["chaoslauncher_working_dir"] = discovery.root_dir
            profile["start_starcraft"] = bool(discovery.starcraft_exe_path)
            profile["starcraft_exe_path"] = discovery.starcraft_exe_path
            profile["starcraft_working_dir"] = discovery.root_dir
            profiles["starcraft"] = profile
            generated["profiles"] = profiles
            generated["active_profile"] = "starcraft"
            generated["enabled"] = bool(discovery.starcraft_exe_path)
            return generated, discovery

        for bot_file in bot_files:
            profile_name = self._unique_profile_name(
                self._profile_name_from_bot_path(bot_file),
                profiles,
            )
            profile = copy.deepcopy(DEFAULT_PROFILE)
            profile["display_name"] = self._display_name_from_profile(profile_name)
            profile["starcraft_116_dir"] = discovery.root_dir
            profile["bwapi_data_dir"] = discovery.bwapi_data_dir
            profile["bot_binary_path"] = bot_file
            profile["start_chaoslauncher"] = bool(discovery.chaoslauncher_path)
            profile["chaoslauncher_path"] = discovery.chaoslauncher_path
            profile["chaoslauncher_working_dir"] = discovery.root_dir
            profile["chaoslauncher_run_as_admin"] = bool(discovery.chaoslauncher_path)
            profile["start_starcraft"] = (
                bool(discovery.starcraft_exe_path)
                and not bool(discovery.chaoslauncher_path)
            )
            profile["starcraft_exe_path"] = discovery.starcraft_exe_path
            profile["starcraft_working_dir"] = discovery.root_dir
            profiles[profile_name] = profile

        if profiles:
            generated["profiles"] = profiles
            generated["active_profile"] = self._preferred_profile_name(profiles)
        return generated, discovery

    def write_config_from_install(self, root_dir):
        generated, discovery = self.build_config_from_install(root_dir)
        os.makedirs(self.config_dir, exist_ok=True)
        with open(self.config_path, "w", encoding="utf-8") as file:
            json.dump(generated, file, indent=2, ensure_ascii=False)
            file.write("\n")
        self.load()
        return discovery

    def _check_required_path(self, messages, profile, path_key, path_type, label):
        path = self.resolve_profile_path(profile, path_key)
        if not path:
            messages.append(f"{label} is not configured: {path_key}")
            return
        self._check_path_exists(messages, path, path_type, label)

    def _check_path_exists(self, messages, path, path_type, label):
        if path_type == "directory" and not os.path.isdir(path):
            messages.append(f"{label} does not exist: {path}")
        elif path_type == "file" and not os.path.isfile(path):
            messages.append(f"{label} does not exist: {path}")

    def _default_config(self):
        config = copy.deepcopy(DEFAULT_CONFIG)
        profiles = {}
        for name, profile in config.get("profiles", {}).items():
            merged = copy.deepcopy(DEFAULT_PROFILE)
            merged.update(profile)
            profiles[name] = merged
        config["profiles"] = profiles
        return config

    def _merge_loaded_config(self, loaded):
        for key, value in loaded.items():
            if key == "profiles":
                continue
            self.config[key] = value

        loaded_profiles = loaded.get("profiles", None)
        if loaded_profiles is None:
            return
        if not isinstance(loaded_profiles, dict):
            self.load_error = "profiles must be a JSON object"
            return

        profiles = {}
        for name, profile in loaded_profiles.items():
            if not isinstance(profile, dict):
                continue
            merged = copy.deepcopy(DEFAULT_PROFILE)
            merged.update(profile)
            profiles[name] = merged
        self.config["profiles"] = profiles

    def _find_first_file(self, root_dir, filenames):
        filename_set = {str(name).lower() for name in filenames}
        for current_root, dirnames, filenames_in_dir in os.walk(root_dir):
            dirnames[:] = self._bounded_dirnames(current_root, root_dir, dirnames)
            for filename in filenames_in_dir:
                if filename.lower() in filename_set:
                    return os.path.normpath(os.path.join(current_root, filename))
        return ""

    def _find_first_dir(self, root_dir, dirnames):
        dirname_set = {str(name).lower() for name in dirnames}
        for current_root, child_dirnames, _filenames in os.walk(root_dir):
            child_dirnames[:] = self._bounded_dirnames(
                current_root,
                root_dir,
                child_dirnames,
            )
            for dirname in child_dirnames:
                if dirname.lower() in dirname_set:
                    return os.path.normpath(os.path.join(current_root, dirname))
        return ""

    def _find_bot_files(self, ai_dir):
        bot_files = []
        try:
            filenames = os.listdir(ai_dir)
        except Exception:
            return bot_files
        for filename in filenames:
            path = os.path.join(ai_dir, filename)
            if not os.path.isfile(path):
                continue
            if self._is_bot_binary_path(path):
                bot_files.append(os.path.normpath(path))
        return sorted(bot_files, key=lambda path: os.path.basename(path).lower())

    def _merge_bot_files(self, *path_groups):
        merged = []
        seen = set()
        for path_group in path_groups:
            for path in path_group or []:
                normalized = os.path.normpath(path)
                key = os.path.normcase(normalized)
                if key in seen:
                    continue
                seen.add(key)
                merged.append(normalized)
        return sorted(merged, key=lambda path: os.path.basename(path).lower())

    def _bounded_dirnames(self, current_root, root_dir, dirnames):
        rel = os.path.relpath(current_root, root_dir)
        if rel == ".":
            depth = 0
        else:
            depth = len(rel.split(os.sep))
        if depth >= 5:
            return []
        ignored = {"logs", "replays", "screenshots", "__pycache__"}
        return [
            dirname
            for dirname in dirnames
            if dirname.lower() not in ignored
        ]

    def _profile_name_from_bot_path(self, path):
        known_profile = self._known_profile_from_bot_path(path)
        if known_profile:
            return known_profile

        stem = os.path.splitext(os.path.basename(path))[0].strip()
        cleaned = "".join(
            char.lower() if char.isalnum() else "_"
            for char in stem
        ).strip("_")
        return cleaned or "bot"

    def _find_loose_bot_files(self, root_dir):
        bot_files = []
        for current_root, dirnames, filenames in os.walk(root_dir):
            dirnames[:] = self._bounded_dirnames(current_root, root_dir, dirnames)
            for filename in filenames:
                path = os.path.join(current_root, filename)
                if not self._is_bot_binary_path(path):
                    continue
                if not self._known_profile_from_bot_path(path):
                    continue
                bot_files.append(os.path.normpath(path))
        return sorted(bot_files, key=lambda path: os.path.basename(path).lower())

    def _is_bot_binary_path(self, path):
        extension = os.path.splitext(str(path))[1].lower()
        if extension not in {".dll", ".exe"}:
            return False
        basename = os.path.basename(str(path)).lower()
        return basename not in {"laveventexporter.dll"}

    def _known_profile_from_bot_path(self, path):
        path_text = os.path.normpath(str(path or "")).lower()
        basename = os.path.splitext(os.path.basename(path_text))[0]
        searchable = " ".join(
            filter(
                None,
                [
                    basename.replace("_", " ").replace("-", " "),
                    path_text.replace("_", " ").replace("-", " "),
                ],
            )
        )
        for profile_name, metadata in KNOWN_BOT_PROFILES.items():
            for alias in metadata.get("aliases", ()):
                alias = str(alias).lower()
                if alias and alias in searchable:
                    return profile_name
        return ""

    def _unique_profile_name(self, profile_name, profiles):
        base_name = str(profile_name or "bot").strip() or "bot"
        if base_name not in profiles:
            return base_name
        index = 2
        while f"{base_name}_{index}" in profiles:
            index += 1
        return f"{base_name}_{index}"

    def _display_name_from_profile(self, profile_name):
        if profile_name in KNOWN_BOT_PROFILES:
            return KNOWN_BOT_PROFILES[profile_name]["display_name"]
        base_name = str(profile_name or "bot")
        if base_name.rsplit("_", 1)[-1].isdigit():
            base_name = base_name.rsplit("_", 1)[0]
        if base_name in KNOWN_BOT_PROFILES:
            suffix = profile_name.replace(base_name, "", 1).strip("_")
            display_name = KNOWN_BOT_PROFILES[base_name]["display_name"]
            return f"{display_name} {suffix}" if suffix else display_name
        return profile_name

    def _race_label_from_profile(self, profile_name):
        profile_name = str(profile_name or "").strip()
        if profile_name in KNOWN_BOT_PROFILES:
            return str(KNOWN_BOT_PROFILES[profile_name].get("race_label", "") or "")
        base_name = profile_name
        if base_name.rsplit("_", 1)[-1].isdigit():
            base_name = base_name.rsplit("_", 1)[0]
        if base_name in KNOWN_BOT_PROFILES:
            return str(KNOWN_BOT_PROFILES[base_name].get("race_label", "") or "")
        return ""

    def _preferred_profile_name(self, profiles):
        for name in ("saida", "monster", "stardust", "crona", "terminus"):
            if name in profiles:
                return name
        for name in profiles:
            return name
        return "saida"
