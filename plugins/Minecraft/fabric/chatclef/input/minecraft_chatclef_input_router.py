#20260803_kpopmodder: Route recognized LAVI chat/mic input into Fabric ChatClef commands.
from __future__ import annotations

from typing import Any

from core.logger import log_print
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_intent_gate import (
    MinecraftChatClefInputIntentGate,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing import (
    MinecraftChatClefRouteDecisionFactory,
    MinecraftChatClefSubmissionBoundary,
    MinecraftChatClefSubmissionPrecheck,
    MinecraftChatClefTranslationBoundary,
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
        self._translation_boundary = MinecraftChatClefTranslationBoundary()
        self._submission_precheck = MinecraftChatClefSubmissionPrecheck()
        self._submission_boundary = MinecraftChatClefSubmissionBoundary()
        self._decision_factory = MinecraftChatClefRouteDecisionFactory()

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
        if not self._translation_boundary.is_available(
            self.extension
        ) or not self._submission_boundary.is_available(self.extension):
            self._log("route skipped: single-pass submission boundary unavailable")
            return MinecraftChatClefInputRouteDecision.not_handled(
                "handler_unavailable"
            )

        try:
            raw_translation = self._translation_boundary.translate_once(
                self.extension,
                command_text,
            )
        except Exception as error:
            return self._operation_failure("translation_failed", error)
        try:
            translation = self._translation_boundary.validate(raw_translation)
        except Exception as error:
            self._log(
                "route rejected malformed translation: "
                f"error={type(error).__name__}: {error}"
            )
            return self._decision_factory.malformed_translation(error)

        translation_status = self._translation_boundary.status(translation)
        if translation_status in {"unknown", "ambiguous", "unsupported"}:
            return MinecraftChatClefInputRouteDecision.not_handled(
                f"{translation_status}_intent"
            )
        if translation_status != "validated":
            return self._decision_factory.translation_rejection(translation)

        readiness = self._submission_precheck.inspect(self.extension)
        if not readiness.ready:
            self._log(
                "route rejected by submission precheck: "
                f"reason={readiness.reason} error={readiness.error} "
                f"message={readiness.message}"
            )
            return self._decision_factory.precheck_rejection(readiness)

        try:
            result = self._submission_boundary.submit_once(
                self.extension,
                command_text,
                translation,
            )
        except Exception as error:
            return self._operation_failure(
                "submission_failed",
                error,
                translation,
            )

        self._log(
            "route handled: "
            f"translation_status={translation_status} "
            f"command={translation.get('command')} "
            f"result_status={self._decision_factory.result_status(result)} "
            f"ok={result.get('ok')}"
        )
        return self._decision_factory.submitted(translation, result)

    def _operation_failure(
        self,
        reason: str,
        error: Exception,
        translation: dict[str, Any] | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        message = f"{type(error).__name__}: {error}"
        self._log(f"route failed: reason={reason} error={message}")
        return self._decision_factory.operation_failure(
            reason,
            error,
            translation,
        )

    def _log(self, message: str) -> None:
        try:
            self.log_callback(f"[MinecraftChatClefInputRouter] {message}")
        except Exception:
            pass
