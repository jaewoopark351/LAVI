#20260703_kpopmodder: Consumes exported StarCraft 1.16 game events from JSONL without touching BWAPI memory.
import json
import os
from dataclasses import dataclass, field


@dataclass
class StarCraft116GameEventReadResult:
    #20260703_kpopmodder: Keeps JSONL read results explicit for tests and runtime logging.
    path: str
    events: list = field(default_factory=list)
    errors: list = field(default_factory=list)
    offset: int = 0


class StarCraft116GameEventTailer:
    #20260703_kpopmodder: Tail only complete JSONL objects so exporter writes can be append-only.
    def __init__(self, start_at_end=True):
        self.start_at_end = start_at_end
        self._offsets = {}

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
            return StarCraft116GameEventReadResult(
                path=path,
                errors=["game events path is not configured."],
            )
        if not os.path.isfile(path):
            self._offsets[path] = 0
            return StarCraft116GameEventReadResult(path=path)

        size = os.path.getsize(path)
        offset = self._offsets.get(path)
        if offset is None:
            if self.start_at_end:
                self._offsets[path] = size
                return StarCraft116GameEventReadResult(path=path, offset=size)
            offset = 0
        if offset > size:
            offset = 0

        events = []
        errors = []
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

                text = (
                    raw_line.decode("utf-8-sig", errors="replace")
                    .lstrip("\ufeff")
                    .strip()
                )
                if not text:
                    continue
                try:
                    payload = json.loads(text)
                except json.JSONDecodeError as e:
                    errors.append(f"invalid JSONL at byte {before}: {e}")
                    continue
                if not isinstance(payload, dict):
                    errors.append(f"ignored non-object JSONL at byte {before}")
                    continue
                events.append(payload)
            offset = file.tell()

        self._offsets[path] = offset
        return StarCraft116GameEventReadResult(
            path=path,
            events=events,
            errors=errors,
            offset=offset,
        )

    @staticmethod
    def _is_complete_line(raw_line):
        return raw_line.endswith(b"\n") or raw_line.endswith(b"\r")
