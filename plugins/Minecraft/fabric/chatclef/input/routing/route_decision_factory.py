#20260818_kpopmodder: Render Fabric ChatClef routing outcomes into the existing LAVI decision contract.
#20260819_kpopmodder: Report verified reconciliation without submitting the triggering command.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)

from .submission_readiness import MinecraftChatClefSubmissionReadiness


class MinecraftChatClefRouteDecisionFactory:
    def reconciled_without_submission(
        self,
        request_id: str,
    ) -> MinecraftChatClefInputRouteDecision:
        message = (
            "The previous Fabric ChatClef request reached a matching terminal "
            "result. The current command was not submitted; send it again as "
            "a fresh explicit command if it is still wanted."
        )
        result = {
            "ok": False,
            "request_id": request_id,
            "error": "current_command_not_submitted",
            "message": message,
            "details": {
                "reconciliation_completed": True,
                "current_command_submitted": False,
            },
        }
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_submission_reconciled_command_not_submitted",
            response_text=f"[Minecraft] {message}",
            result=result,
        )

    def precheck_rejection(
        self,
        readiness: MinecraftChatClefSubmissionReadiness,
    ) -> MinecraftChatClefInputRouteDecision:
        result = {
            "ok": False,
            "error": readiness.error,
            "message": readiness.message,
            "status": dict(readiness.status),
        }
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=readiness.reason,
            response_text=f"[Minecraft] command rejected: {readiness.message}",
            result=result,
        )

    def submitted(
        self,
        translation: Mapping[str, Any],
        result: Mapping[str, Any],
    ) -> MinecraftChatClefInputRouteDecision:
        result_status = self.result_status(result)
        details = result.get("details")
        result_details = dict(details) if isinstance(details, Mapping) else {}
        if (
            result_status == "unknown"
            or result_details.get("reconciliation_required") is True
        ):
            reason = "minecraft_submission_outcome_unknown"
        elif result.get("ok") is True:
            reason = "minecraft_command_routed"
        else:
            reason = "minecraft_command_rejected"
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason,
            response_text=self._response_text(translation, result),
            result=dict(result),
            translation=dict(translation),
        )

    def translation_rejection(
        self,
        translation: Mapping[str, Any],
    ) -> MinecraftChatClefInputRouteDecision:
        reason_code = str(
            translation.get("reason_code")
            or translation.get("status")
            or "translation_rejected"
        )
        message = str(translation.get("message") or reason_code).strip()
        data = translation.get("data")
        details = dict(data) if isinstance(data, Mapping) else {}
        result = {
            "ok": False,
            "status": dict(translation),
            "error": reason_code,
            "message": message,
            "details": details,
        }
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_translation_rejected",
            response_text=f"[Minecraft] command rejected: {message}",
            result=result,
            translation=dict(translation),
        )

    def malformed_translation(
        self,
        error: Exception,
    ) -> MinecraftChatClefInputRouteDecision:
        message = f"{type(error).__name__}: {error}"
        result = {
            "ok": False,
            "error": "malformed_translation_result",
            "message": message,
            "details": {},
        }
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_translation_malformed",
            response_text=f"[Minecraft] command rejected: {message}",
            result=result,
            translation={},
        )

    def operation_failure(
        self,
        reason: str,
        error: Exception,
        translation: Mapping[str, Any] | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        message = f"{type(error).__name__}: {error}"
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason,
            response_text=f"[Minecraft] command failed: {message}",
            result={"ok": False, "error": reason, "message": message},
            translation=dict(translation or {}),
        )

    def _response_text(
        self,
        translation: Mapping[str, Any],
        result: Mapping[str, Any],
    ) -> str:
        command = str(translation.get("command") or "").strip()
        result_status = self.result_status(result)
        message = str(result.get("message") or "").strip()
        if result.get("ok") is True:
            if result_status in {"accepted", "running"}:
                return f"[Minecraft] command sent: {command}"
            if result_status == "completed":
                return f"[Minecraft] command completed: {command}"
            return f"[Minecraft] command routed: {command}"
        if result_status == "unknown":
            if message:
                return f"[Minecraft] command status unknown: {message}"
            return "[Minecraft] command status unknown; reconciliation required."
        if message:
            return f"[Minecraft] command rejected: {message}"
        return "[Minecraft] command rejected."

    def result_status(self, result: Mapping[str, Any]) -> str:
        status = result.get("status")
        if isinstance(status, Mapping):
            return str(status.get("status") or "").strip().lower()
        return str(status or "").strip().lower()
