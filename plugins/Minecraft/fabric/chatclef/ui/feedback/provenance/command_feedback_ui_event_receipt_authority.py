#20260907_kpopmodder: Issue and consume bounded one-shot UI provenance receipts.
from __future__ import annotations

import hashlib
import re
import threading
import uuid

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent

from .command_feedback_ui_event_receipt import CommandFeedbackUiEventReceipt


class CommandFeedbackUiEventReceiptAuthority:
    RAW_SOURCE = "lavi_gui"
    KOREAN_SOURCE = "lavi_gui_korean"
    PROVIDER_ID = "minecraft_fabric_chatclef_ui"

    _EVENT_KINDS = {
        RAW_SOURCE: "minecraft_raw_gui_submit",
        KOREAN_SOURCE: "minecraft_korean_gui_submit",
    }
    _EVENT_ID = re.compile(r"[0-9a-f]{32}\Z", re.ASCII)

    def __init__(self, *, capacity: int = 8, event_id_factory=None) -> None:
        if type(capacity) is not int or capacity < 1:
            raise ValueError("UI receipt capacity must be a positive exact int")
        self._capacity = capacity
        self._event_id_factory = event_id_factory or (lambda: uuid.uuid4().hex)
        self._active: dict[str, CommandFeedbackUiEventReceipt] = {}
        self._lock = threading.Lock()

    def issue(
        self,
        *,
        request_id: object,
        source: object,
        text: object,
    ) -> CommandFeedbackUiEventReceipt | None:
        if not self._valid_request(request_id, source, text):
            return None
        event_id = self._event_id_factory()
        if type(event_id) is not str or self._EVENT_ID.fullmatch(event_id) is None:
            return None
        receipt = CommandFeedbackUiEventReceipt(
            request_id=request_id,
            event_id=event_id,
            source=source,
            event_kind=self._EVENT_KINDS[source],
            provider_id=self.PROVIDER_ID,
            text_digest=self._digest(text),
        )
        with self._lock:
            if len(self._active) >= self._capacity or event_id in self._active:
                return None
            self._active[event_id] = receipt
        return receipt

    def consume(
        self,
        receipt: object,
        *,
        request_id: object,
        source: object,
        text: object,
    ) -> LaviInputEvent | None:
        if type(receipt) is not CommandFeedbackUiEventReceipt:
            return None
        with self._lock:
            issued = self._active.pop(receipt.event_id, None)
        if issued is not receipt or not self._matches(
            receipt,
            request_id=request_id,
            source=source,
            text=text,
        ):
            return None
        return LaviInputEvent(
            text=text,
            source=receipt.source,
            event_id=receipt.event_id,
            event_kind=receipt.event_kind,
            final=True,
            provider_id=receipt.provider_id,
            fallback_payload=None,
        )

    def abandon(self, receipt: object) -> bool:
        if type(receipt) is not CommandFeedbackUiEventReceipt:
            return False
        with self._lock:
            return self._active.pop(receipt.event_id, None) is receipt

    def _valid_request(self, request_id: object, source: object, text: object) -> bool:
        return bool(
            type(request_id) is str
            and request_id == request_id.strip()
            and 1 <= len(request_id) <= 160
            and source in self._EVENT_KINDS
            and type(text) is str
            and bool(text.strip())
        )

    def _matches(
        self,
        receipt: CommandFeedbackUiEventReceipt,
        *,
        request_id: object,
        source: object,
        text: object,
    ) -> bool:
        return bool(
            self._valid_request(request_id, source, text)
            and receipt.request_id == request_id
            and receipt.source == source
            and receipt.event_kind == self._EVENT_KINDS[source]
            and receipt.provider_id == self.PROVIDER_ID
            and receipt.text_digest == self._digest(text)
        )

    @staticmethod
    def _digest(text: str) -> str:
        return hashlib.sha256(text.encode("utf-8")).hexdigest()


__all__ = ("CommandFeedbackUiEventReceiptAuthority",)
