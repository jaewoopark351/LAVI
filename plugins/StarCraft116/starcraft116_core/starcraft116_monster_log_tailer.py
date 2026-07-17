#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#202600707_kpopmodder
#20260705_kpopmodder: Added Monster.exe log tailing so standalone BWAPI client logs can drive TTS reactions.
import json
import os
import re
from dataclasses import dataclass, field


from .starcraft116_monster_log_read_result import StarCraft116MonsterLogReadResult

class StarCraft116MonsterLogTailer:
    #20260705_kpopmodder: Monster.exe writes plain text, not LAVEventExporter JSONL.
    _NEG_EXIT_CAUSE_MAP = {
        -1073741510: "user_interrupt_or_reboot",
        -1073740791: "runtime_exception",
        -1073741819: "access_violation",
        -1073741567: "killed_by_task",
    }
    _END_CAUSE_HINT_MAP = {
        "failed, disconnecting": "launch_or_runtime_error",
        "connection lost. terminating.": "connection_loss",
    }
    _EXIT_CODE_PATTERNS = (
        r"\bexit[_\s]*code\s*[:=]?\s*(-?\d+)",
        r"\bexitcode\s*[:=]?\s*(-?\d+)",
    )

    def __init__(self, start_at_end=True):
        self.start_at_end = start_at_end
        self._offsets = {}
        self._session_states = {}

    def reset(self, path=None):
        if path is None:
            self._offsets = {}
            return
        self._offsets.pop(str(path), None)

    def prime_to_end(self, path):
        path = str(path or "")
        if path and os.path.isfile(path):
            self._offsets[path] = os.path.getsize(path)
        elif path:
            self._offsets[path] = 0

    def read_new_events(self, path, max_events=6):
        path = str(path or "")
        if not path:
            return StarCraft116MonsterLogReadResult(
                path=path,
                errors=["Monster log path is not configured."],
            )
        if not os.path.isfile(path):
            self._offsets[path] = 0
            self._session_states[path] = {
                "in_session": False,
                "exit_hint": None,
            }
            return StarCraft116MonsterLogReadResult(path=path)

        size = os.path.getsize(path)
        offset = self._offsets.get(path)
        if offset is None:
            if self.start_at_end:
                self._offsets[path] = size
                return StarCraft116MonsterLogReadResult(path=path, offset=size)
            offset = 0
        if offset > size:
            offset = 0
            state = self._session_states.setdefault(
                path,
                {
                    "in_session": False,
                    "exit_hint": None,
                },
            )
            state["exit_hint"] = None
            state["in_session"] = False

        state = self._session_states.setdefault(
            path,
            {
                "in_session": False,
                "exit_hint": None,
            },
        )

        events = []
        max_events = max(1, int(max_events or 1))
        with open(path, "rb") as file:
            file.seek(offset)
            while len(events) < max_events:
                before = file.tell()
                raw_line = file.readline()
                if not raw_line:
                    break
                after = file.tell()
                if not self._is_complete_line(raw_line) and after >= size:
                    file.seek(before)
                    break

                text = raw_line.decode("utf-8-sig", errors="replace").strip()
                for event in self._events_from_line(text, state):
                    event["event_id"] = f"monster-log-{before}-{event['event_type']}"
                    event["log_offset"] = before
                    event["log_path"] = path
                    events.append(event)
                    if len(events) >= max_events:
                        break
            offset = file.tell()

        self._offsets[path] = offset
        return StarCraft116MonsterLogReadResult(
            path=path,
            events=events,
            offset=offset,
        )

    @staticmethod
    def _is_complete_line(raw_line):
        return raw_line.endswith(b"\n") or raw_line.endswith(b"\r")

    def _events_from_line(self, line, state):
        line = str(line or "").strip()
        if not line:
            return []

        explicit_event = self._lav_event_from_line(line)
        if explicit_event:
            return [explicit_event]

        lowered = line.lower()
        if lowered.startswith("start:"):
            state["in_session"] = True
            state["exit_hint"] = None
            return []

        if lowered.startswith("end:"):
            state["in_session"] = False
            return []

        for cause_hint, cause_value in self._END_CAUSE_HINT_MAP.items():
            if cause_hint in lowered:
                state["exit_hint"] = cause_value

        if lowered == "waiting to connect...":
            return []
        if lowered in {"connected", "connection successful", "connected to bwapi."}:
            return [self._build_event(
                "monster_connection_successful",
                "Monster connected to BWAPI.",
                line,
            )]
        if lowered == "joined a game.":
            return [self._build_event(
                "monster_joined_game",
                "Monster joined the StarCraft game.",
                line,
            )]
        if lowered == "disconnected":
            state["exit_hint"] = "disconnected"
            return [self._build_event(
                "monster_disconnected",
                "Monster disconnected from BWAPI.",
                line,
                severity="warning",
            )]
        if "sc.dat is missing" in lowered:
            state["exit_hint"] = "launch_or_runtime_error"
            return [self._build_event(
                "monster_missing_sc_dat",
                "Monster reported sc.dat is missing and aborted.",
                line,
                severity="error",
            )]
        if lowered == "game ended.":
            state["in_session"] = False
            return [self._build_event(
                "monster_game_ended",
                "Monster game session ended.",
                line,
                severity="info",
                details={"phase": "normal"},
            )]

        exit_code = self._extract_exit_code(line)
        if exit_code is not None:
            exit_code_cause = self._resolve_exit_code_cause(
                exit_code,
                cause_hint=state.get("exit_hint"),
            )
            state["exit_hint"] = None
            severity = exit_code_cause["severity"]
            return [self._build_event(
                "monster_exit_code",
                exit_code_cause["summary"],
                line,
                severity=severity,
                details={
                    "exit_code": exit_code,
                    "cause": exit_code_cause["cause"],
                    "reason": exit_code_cause["reason"],
                    "exit_code_cause": exit_code_cause["cause"],
                    "exit_code_reason": exit_code_cause["reason"],
                },
            )]
        return []

    @classmethod
    def _extract_exit_code(cls, line):
        for pattern in cls._EXIT_CODE_PATTERNS:
            match = re.search(pattern, line, flags=re.IGNORECASE)
            if match:
                return int(match.group(1))
        return None

    @staticmethod
    def _build_event(event_type, summary, line, severity="info", details=None):
        if details is None:
            details = {}
        return {
            "schema": "lav_starcraft116_monster_log_event_v1",
            "source": "Monster.exe",
            "event_type": event_type,
            "summary": summary,
            "severity": severity,
            "tts_eligible": False,
            "details": {
                "raw_line": line,
                **details,
            },
        }

    @classmethod
    def _resolve_exit_code_cause(cls, exit_code, cause_hint=None):
        if exit_code != 0 and cause_hint == "disconnected":
            return {
                "cause": "disconnected",
                "reason": "disconnection after gameplay",
                "summary": "Monster exited with code 1 (disconnected).",
                "severity": "error",
            }
        if exit_code != 0 and cause_hint == "connection_loss":
            return {
                "cause": "connection_loss",
                "reason": "connection lost before normal shutdown",
                "summary": "Monster exited with code 1 after connection loss.",
                "severity": "error",
            }
        if exit_code != 0 and cause_hint == "launch_or_runtime_error":
            return {
                "cause": "launch_or_runtime_error",
                "reason": "non-zero exit after launch/runtime failure sequence",
                "summary": "Monster exited with code 1 (launch/runtime error).",
                "severity": "error",
            }
        if exit_code == 0:
            return {
                "cause": "normal_exit",
                "reason": "process ended normally",
                "summary": "Monster exited with code 0 (normal shutdown).",
                "severity": "ok",
            }
        if exit_code == 1:
            return {
                "cause": "launch_or_runtime_error",
                "reason": "non-zero exit likely indicates launch/runtime failure",
                "summary": "Monster exited with code 1 (launch/runtime error).",
                "severity": "error",
            }
        if exit_code in cls._NEG_EXIT_CAUSE_MAP:
            return {
                "cause": cls._NEG_EXIT_CAUSE_MAP[exit_code],
                "reason": "exit code indicates external termination",
                "summary": (
                    f"Monster exited abnormally with code {exit_code} "
                    f"({cls._NEG_EXIT_CAUSE_MAP[exit_code]})."
                ),
                "severity": "error",
            }
        if exit_code < 0:
            return {
                "cause": "abnormal_negative_exit",
                "reason": "exit code indicates external termination",
                "summary": f"Monster exited with negative code {exit_code} (external termination).",
                "severity": "error",
            }

        return {
            "cause": "nonzero_exit",
            "reason": "non-zero exit code indicates abnormal end",
            "summary": f"Monster exited with code {exit_code}.",
            "severity": "error",
        }

    def _lav_event_from_line(self, line):
        #20260705_kpopmodder: Only explicit Monster LAV_EVENT JSON is allowed to drive TTS.
        match = re.match(r"^\[?LAV_EVENT\]?\s*:?\s*(\{.*\})\s*$", line)
        if not match:
            return None

        raw_json = match.group(1)
        try:
            event = json.loads(raw_json)
        except Exception as e:
            return self._build_event(
                "monster_lav_event_invalid",
                f"Monster emitted invalid LAV_EVENT JSON: {e}",
                line,
                severity="error",
            )

        if not isinstance(event, dict):
            return self._build_event(
                "monster_lav_event_invalid",
                "Monster emitted non-object LAV_EVENT JSON.",
                line,
                severity="error",
            )

        event_type = str(event.get("event_type", "") or "").strip()
        if not event_type:
            return self._build_event(
                "monster_lav_event_invalid",
                "Monster emitted LAV_EVENT JSON without event_type.",
                line,
                severity="error",
            )

        summary = str(event.get("summary", "") or "").strip()
        if not summary:
            summary = event_type.replace("_", " ")

        event = dict(event)
        event.setdefault("schema", "lav_starcraft116_bwapi_event_v1")
        event.setdefault("source", "Monster.exe")
        event.setdefault("severity", "info")
        event["event_type"] = event_type
        event["summary"] = summary
        event["tts_eligible"] = True
        event.setdefault("details", {})
        if isinstance(event["details"], dict):
            event["details"].setdefault("raw_line", line)
        return event
