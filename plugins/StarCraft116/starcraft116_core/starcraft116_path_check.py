#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260702_kpopmodder: Keeps StarCraft 1.16 BWAPI bot paths local and optional.
import copy
import json
import os
from dataclasses import dataclass, field


DEFAULT_PROFILE = {
    "display_name": "",
    "starcraft_116_dir": "",
    "bwapi_bundle_dir": "",
    "bwapi_starcraft_dir": "",
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
    "active_profile": "monster",
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
    "bwapi_proxy_events_log_sample_rate": 25,
    "bwapi_proxy_events_path": "",
    "bwapi_proxy_events_prefer_starcraft_path": True,
    "bwapi_launch_config_prefer_starcraft_path": True,
    "bwapi_proxy_dll_auto_install": True,
    "bwapi_proxy_dll_project_only": True,
    "bwapi_proxy_dll_source_path": "plugins\\StarCraft116\\BWAPI.dll",
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
