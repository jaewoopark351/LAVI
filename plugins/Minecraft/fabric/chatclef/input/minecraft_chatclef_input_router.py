#20260803_kpopmodder: Route recognized LAVI chat/mic input into Fabric ChatClef commands.
from __future__ import annotations

import uuid
from typing import Any, Mapping

from core.logger import log_print
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_intent_gate import (
    MinecraftChatClefInputIntentGate,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class MinecraftChatClefInputRouter:
    def __init__(
        self,
        extension: Any = None,
        intent_gate: MinecraftChatClefInputIntentGate | None = None,
        log_callback: Any = log_print,
    ):
        self.extension = extension
        self.intent_gate = intent_gate or MinecraftChatClefInputIntentGate()
        self.log_callback = log_callback

    def route(self, text: object) -> MinecraftChatClefInputRouteDecision:
        command_text = str(text or "").strip()
        if not command_text:
            return MinecraftChatClefInputRouteDecision.not_handled("empty_input")
        if not self.intent_gate.should_consider(command_text):
            return MinecraftChatClefInputRouteDecision.not_handled(
                "no_minecraft_trigger"
            )
        if self.extension is None:
            self._log("route skipped: extension unavailable")
            return MinecraftChatClefInputRouteDecision.not_handled(
                "extension_unavailable"
            )

        translator = getattr(self.extension, "translate_natural_language_command", None)
        handler = getattr(self.extension, "handle_natural_language_command", None)
        if not callable(translator) or not callable(handler):
            self._log("route skipped: natural-language handler unavailable")
            return MinecraftChatClefInputRouteDecision.not_handled(
                "handler_unavailable"
            )

        try:
            translation = self._mapping_payload(translator(command_text))
        except Exception as error:
            return self._handled_exception("translation_failed", error)

        if self._translation_status(translation) == "unknown":
            return MinecraftChatClefInputRouteDecision.not_handled("unknown_intent")

        try:
            result = self._mapping_payload(handler(self._request(command_text)))
        except Exception as error:
            return self._handled_exception("submission_failed", error, translation)

        response_text = self._response_text(translation, result)
        self._log(
            "route handled: "
            f"translation_status={self._translation_status(translation)} "
            f"command={translation.get('command')} "
            f"result_status={self._result_status(result)} "
            f"ok={result.get('ok')}"
        )
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_command_routed",
            response_text=response_text,
            result=result,
            translation=translation,
        )

    def _request(self, text: str) -> dict[str, Any]:
        return {
            "request_id": f"lavi-input-ko-{uuid.uuid4().hex}",
            "text": text,
            "source": "lavi_chat_mic_router",
            "metadata": {
                "input_route": "minecraft_fabric_chatclef",
                "language": "ko",
            },
        }

    def _mapping_payload(self, payload: Any) -> dict[str, Any]:
        if isinstance(payload, Mapping):
            return dict(payload)
        to_dict = getattr(payload, "to_dict", None)
        if callable(to_dict):
            return dict(to_dict())
        return {"raw": payload}

    def _translation_status(self, translation: Mapping[str, Any]) -> str:
        return str(translation.get("status") or "").strip().lower()

    def _result_status(self, result: Mapping[str, Any]) -> str:
        status = result.get("status")
        if isinstance(status, Mapping):
            return str(status.get("status") or "").strip().lower()
        return str(status or "").strip().lower()

    def _response_text(
        self,
        translation: Mapping[str, Any],
        result: Mapping[str, Any],
    ) -> str:
        command = str(translation.get("command") or "").strip()
        result_status = self._result_status(result)
        message = str(result.get("message") or "").strip()

        if bool(result.get("ok")):
            if result_status in {"accepted", "running"}:
                return f"[Minecraft] command sent: {command}"
            if result_status == "completed":
                return f"[Minecraft] command completed: {command}"
            return f"[Minecraft] command routed: {command}"

        if message:
            return f"[Minecraft] command rejected: {message}"
        return "[Minecraft] command rejected."

    def _handled_exception(
        self,
        reason: str,
        error: Exception,
        translation: dict[str, Any] | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        message = f"{type(error).__name__}: {error}"
        self._log(f"route failed: reason={reason} error={message}")
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason,
            response_text=f"[Minecraft] command failed: {message}",
            result={"ok": False, "error": reason, "message": message},
            translation=translation or {},
        )

    def _log(self, message: str) -> None:
        try:
            self.log_callback(f"[MinecraftChatClefInputRouter] {message}")
        except Exception:
            pass
