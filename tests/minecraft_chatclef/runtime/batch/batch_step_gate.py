#20260819_kpopmodder: Preserve the legacy import for terminal checkpoint admission.
from __future__ import annotations

from collections.abc import Mapping

from .terminal_checkpoint_gate import terminal_checkpoint_gate_error


def batch_step_gate_error(command_result: Mapping[str, object]) -> str:
    return terminal_checkpoint_gate_error(command_result)
