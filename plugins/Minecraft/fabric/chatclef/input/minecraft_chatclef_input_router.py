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
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
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
        submitter = getattr(self.extension, "submit_translated_command", None)
        handler = getattr(self.extension, "handle_natural_language_command", None)
        if not callable(translator) or not (
            callable(submitter) or callable(handler)
        ):
            self._log("route skipped: natural-language handler unavailable")
            return MinecraftChatClefInputRouteDecision.not_handled(
                "handler_unavailable"
            )

        try:
            raw_translation = translator(command_text)
        except Exception as error:
            return self._handled_exception("translation_failed", error)

        try:
            translation = self._validated_translation_payload(raw_translation)
        except Exception as error:
            return self._handled_malformed_translation(error)

        translation_status = self._translation_status(translation)
        if translation_status in {"unknown", "ambiguous", "unsupported"}:
            return MinecraftChatClefInputRouteDecision.not_handled(
                f"{translation_status}_intent"
            )
        if translation_status in {"invalid", "internal_error"}:
            return self._handled_translation_rejection(translation)
        if translation_status != "validated":
            return self._handled_translation_rejection(translation)

        bridge_precheck = self._validated_submission_precheck()
        if bridge_precheck is not None:
            return bridge_precheck

        try:
            request = self._request(command_text)
            if callable(submitter):
                result = self._mapping_payload(submitter(request, translation))
            else:
                result = self._mapping_payload(handler(request))
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

    def _validated_translation_payload(self, payload: Any) -> dict[str, Any]:
        translation = ChatClefTranslationResultDTO.from_mapping(
            self._mapping_payload(payload)
        )
        return translation.to_dict()

    def _translation_status(self, translation: Mapping[str, Any]) -> str:
        return str(translation.get("status") or "").strip().lower()

    def _result_status(self, result: Mapping[str, Any]) -> str:
        status = result.get("status")
        if isinstance(status, Mapping):
            return str(status.get("status") or "").strip().lower()
        return str(status or "").strip().lower()

    def _validated_submission_precheck(
        self,
    ) -> MinecraftChatClefInputRouteDecision | None:
        status = self._extension_status()
        bridge = self._bridge_status(status)
        if not bridge:
            return None
        if "connected" in bridge and not bool(bridge.get("connected")):
            detail = str(
                bridge.get("detail")
                or bridge.get("last_error_message")
                or "Fabric ChatClef bridge client is not connected."
            )
            return MinecraftChatClefInputRouteDecision.handled_result(
                reason="minecraft_bridge_disconnected",
                response_text=f"[Minecraft] command rejected: {detail}",
                result={
                    "ok": False,
                    "error": "not_connected",
                    "message": detail,
                    "status": bridge,
                },
            )
        active_request_id = self._active_request_id(bridge)
        if active_request_id:
            message = f"Fabric ChatClef command already active: {active_request_id}"
            return MinecraftChatClefInputRouteDecision.handled_result(
                reason="minecraft_command_busy",
                response_text=f"[Minecraft] command rejected: {message}",
                result={
                    "ok": False,
                    "error": "active_command",
                    "message": message,
                    "status": bridge,
                },
            )
        return None

    def _extension_status(self) -> dict[str, Any]:
        status_method = getattr(self.extension, "get_status", None)
        if not callable(status_method):
            return {}
        try:
            return self._mapping_payload(status_method())
        except Exception as error:
            self._log(
                "status precheck skipped: "
                f"error={type(error).__name__}: {error}"
            )
            return {}

    def _bridge_status(self, status: Mapping[str, Any]) -> dict[str, Any]:
        details = status.get("details")
        if isinstance(details, Mapping) and (
            "connected" in details or "enabled" in details
        ):
            return dict(details)
        if "connected" in status or "enabled" in status:
            return dict(status)
        return {}

    def _active_request_id(self, bridge: Mapping[str, Any]) -> str:
        details = bridge.get("details")
        if not isinstance(details, Mapping):
            return ""
        commands = details.get("commands")
        if not isinstance(commands, Mapping):
            return ""
        return str(commands.get("active_request_id") or "").strip()

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

    def _handled_translation_rejection(
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

    def _handled_malformed_translation(
        self,
        error: Exception,
    ) -> MinecraftChatClefInputRouteDecision:
        message = f"{type(error).__name__}: {error}"
        self._log(f"route rejected malformed translation: error={message}")
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

    def _log(self, message: str) -> None:
        try:
            self.log_callback(f"[MinecraftChatClefInputRouter] {message}")
        except Exception:
            pass
