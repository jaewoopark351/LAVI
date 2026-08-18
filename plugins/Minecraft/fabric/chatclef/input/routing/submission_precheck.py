#20260818_kpopmodder: Fail closed unless the Fabric ChatClef bridge is enabled, connected, and idle.
from __future__ import annotations

from typing import Any, Mapping

from .submission_readiness import MinecraftChatClefSubmissionReadiness


class MinecraftChatClefSubmissionPrecheck:
    EXPECTED_BACKEND = "fabric_chatclef"

    def inspect(self, extension: Any) -> MinecraftChatClefSubmissionReadiness:
        status_method = getattr(extension, "get_status", None)
        if not callable(status_method):
            return self._status_unavailable(
                "Fabric ChatClef status reader is unavailable."
            )
        try:
            raw_status = status_method()
        except Exception as error:
            return self._status_unavailable(
                "Fabric ChatClef status read failed: "
                f"{type(error).__name__}: {error}"
            )
        if not isinstance(raw_status, Mapping):
            return self._status_unavailable(
                "Fabric ChatClef status must be an object."
            )

        bridge = self._bridge_status(raw_status)
        if bridge is None:
            return self._status_unavailable(
                "Fabric ChatClef bridge status is missing."
            )
        backend = self._text(bridge.get("backend_id")).lower()
        if backend != self.EXPECTED_BACKEND:
            return self._status_unavailable(
                "Fabric ChatClef backend status does not match the router.",
                bridge,
                error="backend_mismatch",
            )

        enabled = bridge.get("enabled")
        if type(enabled) is not bool:
            return self._status_unavailable(
                "Fabric ChatClef enabled status is missing or invalid.",
                bridge,
            )
        if not enabled:
            return MinecraftChatClefSubmissionReadiness.rejected(
                reason="minecraft_bridge_disabled",
                error="bridge_disabled",
                message="Fabric ChatClef bridge is disabled.",
                status=bridge,
            )

        connected = bridge.get("connected")
        if type(connected) is not bool:
            return self._status_unavailable(
                "Fabric ChatClef connection status is missing or invalid.",
                bridge,
            )
        if not connected:
            return self._disconnected(bridge)

        lifecycle_state = self._text(bridge.get("lifecycle_state")).lower()
        if not lifecycle_state:
            return self._status_unavailable(
                "Fabric ChatClef lifecycle status is missing.",
                bridge,
            )
        if lifecycle_state != "connected":
            return MinecraftChatClefSubmissionReadiness.rejected(
                reason="minecraft_bridge_disconnected",
                error="not_connected",
                message=(
                    "Fabric ChatClef lifecycle is not connected: "
                    f"{lifecycle_state}"
                ),
                status=bridge,
            )

        commands = self._command_status(bridge)
        if commands is None or "active_request_id" not in commands:
            return self._status_unavailable(
                "Fabric ChatClef command status is incomplete.",
                bridge,
            )
        active_request_id = commands.get("active_request_id")
        if active_request_id is not None and not isinstance(active_request_id, str):
            return self._status_unavailable(
                "Fabric ChatClef active request status is invalid.",
                bridge,
            )
        active_request_text = self._text(active_request_id)
        if active_request_text:
            message = (
                "Fabric ChatClef command already active: "
                f"{active_request_text}"
            )
            return MinecraftChatClefSubmissionReadiness.rejected(
                reason="minecraft_command_busy",
                error="active_command",
                message=message,
                status=bridge,
            )
        return MinecraftChatClefSubmissionReadiness.accepted(bridge)

    def _bridge_status(
        self,
        status: Mapping[str, Any],
    ) -> dict[str, Any] | None:
        if "backend_id" in status:
            return dict(status)
        details = status.get("details")
        if isinstance(details, Mapping) and "backend_id" in details:
            return dict(details)
        return None

    def _command_status(
        self,
        bridge: Mapping[str, Any],
    ) -> dict[str, Any] | None:
        details = bridge.get("details")
        if not isinstance(details, Mapping):
            return None
        commands = details.get("commands")
        return dict(commands) if isinstance(commands, Mapping) else None

    def _disconnected(
        self,
        bridge: Mapping[str, Any],
    ) -> MinecraftChatClefSubmissionReadiness:
        message = str(
            bridge.get("detail")
            or bridge.get("last_error_message")
            or "Fabric ChatClef bridge client is not connected."
        ).strip()
        return MinecraftChatClefSubmissionReadiness.rejected(
            reason="minecraft_bridge_disconnected",
            error="not_connected",
            message=message,
            status=bridge,
        )

    def _status_unavailable(
        self,
        message: str,
        status: Mapping[str, Any] | None = None,
        *,
        error: str = "status_unavailable",
    ) -> MinecraftChatClefSubmissionReadiness:
        return MinecraftChatClefSubmissionReadiness.rejected(
            reason="minecraft_bridge_status_unavailable",
            error=error,
            message=message,
            status=status,
        )

    def _text(self, value: object) -> str:
        return str(value or "").strip()
