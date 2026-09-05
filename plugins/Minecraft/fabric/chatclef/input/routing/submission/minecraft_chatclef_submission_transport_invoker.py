#20260905_kpopmodder: Isolate one-shot translated-command transport invocation.
from __future__ import annotations

from typing import Any, Mapping


class MinecraftChatClefSubmissionTransportInvoker:
    def is_available(self, extension: Any) -> bool:
        return callable(getattr(extension, "submit_translated_command", None))

    def is_generic_crafting_defaults_available(self, extension: Any) -> bool:
        return callable(
            getattr(
                extension,
                "submit_translated_generic_crafting_defaults_command",
                None,
            )
        )

    def invoke(
        self,
        extension: Any,
        request: object,
        translation: Mapping[str, Any],
        *,
        route_claim: object,
    ) -> Any:
        submitter = getattr(extension, "submit_translated_command")
        if route_claim is None:
            return submitter(request, dict(translation))
        return submitter(
            request,
            dict(translation),
            route_claim=route_claim,
        )

    def invoke_generic_crafting_defaults(
        self,
        extension: Any,
        request: object,
        translation: Mapping[str, Any],
        *,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ) -> Any:
        submitter = getattr(
            extension,
            "submit_translated_generic_crafting_defaults_command",
        )
        return submitter(
            request,
            dict(translation),
            activation_receipt=activation_receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )


__all__ = ("MinecraftChatClefSubmissionTransportInvoker",)
