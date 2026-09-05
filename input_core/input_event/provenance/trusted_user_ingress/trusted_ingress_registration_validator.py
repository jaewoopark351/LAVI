#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

import hashlib
import re

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent


class TrustedIngressRegistrationValidator:
    _EVENT_ID_RE = re.compile(r"^[0-9a-f]{32}$", re.ASCII)

    def __init__(
        self,
        *,
        registry_token: object,
        producer_factory_token_callback,
    ) -> None:
        if registry_token is None:
            raise ValueError("registry_token is required")
        if not callable(producer_factory_token_callback):
            raise TypeError("producer_factory_token_callback must be callable")
        self._registry_token = registry_token
        self._producer_factory_token_callback = producer_factory_token_callback

    def rejection_reason(
        self,
        *,
        ticket: object,
        capability: object,
        event: object,
    ) -> str:
        producer_factory_token = self._producer_factory_token_callback()
        if (
            getattr(ticket, "_registry_token", None) is not self._registry_token
            or getattr(ticket, "_factory_token", None)
            is not producer_factory_token
            or not callable(getattr(ticket, "_is_open", None))
            or not ticket._is_open()
        ):
            return "trusted_ingress_creation_ticket_invalid"
        if (
            getattr(capability, "_registry_token", None)
            is not self._registry_token
            or getattr(capability, "_factory_token", None)
            is not producer_factory_token
            or getattr(capability, "_ticket", None) is not ticket
            or getattr(capability, "_invocation_token", None)
            is not getattr(ticket, "_invocation_token", None)
            or not callable(getattr(capability, "_is_open", None))
            or not capability._is_open()
        ):
            return "trusted_ingress_producer_capability_invalid"
        if type(event) is not LaviInputEvent:
            return "trusted_ingress_event_type_invalid"
        if not self.valid_event_fields(event):
            return "trusted_ingress_event_fields_invalid"
        policy = getattr(ticket, "_policy", None)
        if (
            event.source != getattr(policy, "source", None)
            or event.provider_id != getattr(policy, "provider_id", None)
            or event.event_kind != getattr(policy, "event_kind", None)
            or event.final is not getattr(policy, "final", None)
        ):
            return "trusted_ingress_event_policy_mismatch"
        return ""

    def valid_event_fields(self, event: LaviInputEvent) -> bool:
        return (
            type(event.text) is str
            and type(event.source) is str
            and type(event.provider_id) is str
            and type(event.event_kind) is str
            and type(event.final) is bool
            and type(event.event_id) is str
            and self._EVENT_ID_RE.fullmatch(event.event_id) is not None
        )

    @staticmethod
    def event_signature(event: LaviInputEvent) -> tuple[object, ...]:
        text_digest = hashlib.sha256(event.text.encode("utf-8")).digest()
        return (
            event.source,
            event.provider_id,
            event.event_kind,
            event.final,
            event.event_id,
            len(event.text),
            text_digest,
        )


__all__ = ("TrustedIngressRegistrationValidator",)
