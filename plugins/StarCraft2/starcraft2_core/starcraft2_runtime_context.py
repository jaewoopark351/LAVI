#20260628_kpopmodder: Carry StarCraft2 runtime state in one object for orchestration.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, Iterable, List, Optional


@dataclass
class SC2RuntimeContext:
    process: Any = None
    process_role: str = ""
    process_pid: Optional[int] = None
    stdout_tail: List[str] = field(default_factory=list)
    stderr_tail: List[str] = field(default_factory=list)
    event_bus: Optional[Any] = None
    status: Dict[str, Any] = field(default_factory=dict)
    started_at: Optional[float] = None
    stopped_at: Optional[float] = None
    timeout_sec: float = 5.0
    ports: List[int] = field(default_factory=lambda: [5677, 5678])
    check_hosts: List[str] = field(default_factory=lambda: ["127.0.0.1"])
    runtime_error: Optional[str] = None

    def set_process(self, process: Any, role: str = "") -> None:
        self.process = process
        self.process_role = str(role or "")
        self.process_pid = self._safe_pid(process)

    def clear_process(self) -> None:
        self.process = None
        self.process_role = ""
        self.process_pid = None

    def set_tails(self, stdout_tail: Iterable[Any], stderr_tail: Iterable[Any]) -> None:
        self.stdout_tail = [str(line) for line in stdout_tail]
        self.stderr_tail = [str(line) for line in stderr_tail]

    def set_status(self, value: Dict[str, Any] | None = None) -> None:
        self.status = value if isinstance(value, dict) else {}

    def snapshot(self) -> Dict[str, Any]:
        return {
            "process_pid": self.process_pid,
            "process_role": self.process_role,
            "status": dict(self.status),
            "stdout_tail": list(self.stdout_tail),
            "stderr_tail": list(self.stderr_tail),
            "runtime_error": self.runtime_error,
            "timeout_sec": self.timeout_sec,
            "ports": list(self.ports),
            "check_hosts": list(self.check_hosts),
            "started_at": self.started_at,
            "stopped_at": self.stopped_at,
        }

    @staticmethod
    def _safe_pid(process: Any) -> Optional[int]:
        try:
            value = getattr(process, "pid", None)
        except Exception:
            return None
        if value is None:
            return None
        try:
            return int(value)
        except (TypeError, ValueError):
            return None
