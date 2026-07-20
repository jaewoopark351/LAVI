#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260628_kpopmodder: Add shared StarCraft2 contracts to replace ad-hoc dict payloads.
from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Optional

import time

from app_core.extensions.game_extension_contracts import GameResultDTO, GameStatusDTO


def _coerce_dict(value: Any) -> Dict[str, Any]:
    return dict(value) if isinstance(value, dict) else {}


def _normalize_ports(value: Any) -> list[int]:
    if value is None:
        return [5677, 5678]
    if isinstance(value, str):
        raw = [part.strip() for part in value.split(",")]
    elif isinstance(value, (list, tuple)):
        raw = [str(part).strip() for part in value]
    else:
        return [5677, 5678]
    ports: list[int] = []
    for item in raw:
        try:
            port = int(item)
        except (TypeError, ValueError):
            continue
        if 0 < port < 65536 and port not in ports:
            ports.append(port)
    return ports or [5677, 5678]


def _coerce_str(value: Any, fallback: str = "") -> str:
    return str(value or "").strip() if value is not None else fallback


def _coerce_float(value: Any, fallback: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return float(fallback)


def _coerce_int(value: Any, fallback: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return int(fallback)


def _coerce_optional_int(value: Any) -> Optional[int]:
    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


#20260715_kpopmodder: Add typed command, result, and status contracts for SC2 engines.

@dataclass(frozen=True)
class LocalMatchLaunchConfigDTO:
    executable_path: str = ""
    working_directory: str = ""
    args: str = ""
    proxy_ports: list[int] = field(default_factory=lambda: [5677, 5678])
    bot_name: str = ""
    bot_display_name: str = ""
    ai_race: str = "Zerg"
    human_race: str = "Terran"
    capture_output: bool = True
    keep_local_match_identity_args: bool = False
    starcraft2_exe_path: str = ""
    starcraft2_support64_path: str = ""
    starcraft2_base_path: str = ""
    check_hosts: list[str] = field(default_factory=lambda: ["127.0.0.1"])
    proxy_host: str = ""
    connect_timeout_sec: float = 0.5
    restart_unhealthy: bool = True
    restart_unhealthy_after_sec: float = 20.0
    runtime_download: Dict[str, Any] = field(default_factory=dict)
    bot_profile_validation: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(cls, value: Any) -> "LocalMatchLaunchConfigDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, dict) else {}
        args = payload.get("args", "")
        if isinstance(args, list):
            args_text = " ".join(str(item) for item in args)
        else:
            args_text = _coerce_str(args, "")
        raw_hosts = payload.get("check_hosts", ["127.0.0.1"])
        if isinstance(raw_hosts, str):
            normalized_hosts = [
                part.strip() for part in raw_hosts.split(",") if part.strip()
            ]
        elif isinstance(raw_hosts, (list, tuple)):
            normalized_hosts = [
                _coerce_str(item) for item in raw_hosts if _coerce_str(item)
            ]
        else:
            normalized_hosts = []
        return cls(
            executable_path=_coerce_str(payload.get("executable_path")),
            working_directory=_coerce_str(payload.get("working_directory")),
            args=args_text,
            proxy_ports=_normalize_ports(
                payload.get("proxy_ports", payload.get("ports"))
            ),
            bot_name=_coerce_str(payload.get("bot_name")),
            bot_display_name=_coerce_str(payload.get("bot_display_name")),
            ai_race=_coerce_str(payload.get("ai_race"), "Zerg"),
            human_race=_coerce_str(payload.get("human_race"), "Terran"),
            capture_output=(
                bool(payload.get("capture_output"))
                if isinstance(payload, dict) and "capture_output" in payload
                else True
            ),
            keep_local_match_identity_args=bool(payload.get("keep_local_match_identity_args")),
            starcraft2_exe_path=_coerce_str(payload.get("starcraft2_exe_path")),
            starcraft2_support64_path=_coerce_str(payload.get("starcraft2_support64_path")),
            starcraft2_base_path=_coerce_str(payload.get("starcraft2_base_path")),
            check_hosts=normalized_hosts or ["127.0.0.1"],
            proxy_host=_coerce_str(payload.get("proxy_host")),
            connect_timeout_sec=_coerce_float(payload.get("connect_timeout_sec"), 0.5),
            restart_unhealthy=(
                bool(payload.get("restart_unhealthy"))
                if "restart_unhealthy" in payload
                else True
            ),
            restart_unhealthy_after_sec=_coerce_float(
                payload.get("restart_unhealthy_after_sec"),
                20.0,
            ),
            runtime_download=_coerce_dict(payload.get("runtime_download")),
            bot_profile_validation=_coerce_dict(
                payload.get("bot_profile_validation")
            ),
        )

    def with_args(self, args: str) -> "LocalMatchLaunchConfigDTO":
        return LocalMatchLaunchConfigDTO(
            executable_path=self.executable_path,
            working_directory=self.working_directory,
            args=_coerce_str(args),
            proxy_ports=list(self.proxy_ports),
            bot_name=self.bot_name,
            bot_display_name=self.bot_display_name,
            ai_race=self.ai_race,
            human_race=self.human_race,
            capture_output=self.capture_output,
            keep_local_match_identity_args=self.keep_local_match_identity_args,
            starcraft2_exe_path=self.starcraft2_exe_path,
            starcraft2_support64_path=self.starcraft2_support64_path,
            starcraft2_base_path=self.starcraft2_base_path,
            check_hosts=list(self.check_hosts),
            proxy_host=self.proxy_host,
            connect_timeout_sec=self.connect_timeout_sec,
            restart_unhealthy=self.restart_unhealthy,
            restart_unhealthy_after_sec=self.restart_unhealthy_after_sec,
            runtime_download=dict(self.runtime_download),
            bot_profile_validation=dict(self.bot_profile_validation),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "executable_path": self.executable_path,
            "working_directory": self.working_directory,
            "args": self.args,
            "ports": list(self.proxy_ports),
            "check_hosts": list(self.check_hosts),
            "proxy_host": self.proxy_host,
            "connect_timeout_sec": self.connect_timeout_sec,
            "restart_unhealthy": bool(self.restart_unhealthy),
            "restart_unhealthy_after_sec": self.restart_unhealthy_after_sec,
            "runtime_download": dict(self.runtime_download),
            "bot_profile_validation": dict(self.bot_profile_validation),
            "bot_name": self.bot_name,
            "bot_display_name": self.bot_display_name,
            "ai_race": self.ai_race,
            "human_race": self.human_race,
            "capture_output": bool(self.capture_output),
            "keep_local_match_identity_args": bool(self.keep_local_match_identity_args),
            "starcraft2_exe_path": self.starcraft2_exe_path,
            "starcraft2_support64_path": self.starcraft2_support64_path,
            "starcraft2_base_path": self.starcraft2_base_path,
        }
