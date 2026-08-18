#20260819_kpopmodder: Select one unambiguous Fabric ChatClef bridge status object.
from __future__ import annotations

from typing import Any, Mapping


class MinecraftChatClefBridgeStatusSelector:
    def select(
        self,
        status: Mapping[str, Any],
    ) -> tuple[dict[str, Any] | None, str]:
        direct = "backend_id" in status
        nested_value = status.get("details")
        nested = isinstance(nested_value, Mapping) and "backend_id" in nested_value

        if direct and nested:
            return (
                None,
                "Fabric ChatClef status contains ambiguous bridge objects.",
            )
        if direct:
            return dict(status), ""
        if nested:
            return dict(nested_value), ""
        return None, "Fabric ChatClef bridge status is missing."
