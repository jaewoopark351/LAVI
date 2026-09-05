#20260905_kpopmodder: Record early generic-crafting submission results only.
from __future__ import annotations

from typing import Any, Callable


class GenericCraftingSubmissionResultRecorder:
    def __init__(
        self,
        result_recorder: Callable[[dict[str, Any], str], None],
        *,
        action: str,
    ):
        self._result_recorder = result_recorder
        self._action = action

    def record(self, payload: dict[str, Any]) -> dict[str, Any]:
        self._result_recorder(payload, self._action)
        return payload


__all__ = ("GenericCraftingSubmissionResultRecorder",)
