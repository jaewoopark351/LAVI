#20260905_kpopmodder: Own process-lifetime H5 event reservation and one-time receipt state.
from __future__ import annotations

import re
import threading
from collections.abc import Mapping

from input_core.input_event.contracts import LaviInputEvent

from .contracts import AutoDepositTrustInputClaimReceipt


class AutoDepositTrustInputEventClaimRegistry:
    CAPACITY = 4096
    COMMAND_NAME = "auto_deposit_trust"
    COMMAND = "auto_deposit_trust area 16x16"
    _EVENT_ID_RE = re.compile(r"^[0-9a-f]{32}$", re.ASCII)

    def __init__(self, capacity: int = CAPACITY):
        if type(capacity) is not int or capacity < 0:
            raise ValueError("capacity must be a non-negative exact int")
        self._capacity = capacity
        self._lock = threading.RLock()
        self._registry_token = object()
        self._claim_owner = None
        self._records: dict[str, dict[str, object]] = {}

    def _bind_claim_owner(self, owner: object) -> None:
        if owner is None:
            raise ValueError("claim owner is required")
        with self._lock:
            if self._claim_owner is None:
                self._claim_owner = owner
                return
            if self._claim_owner is not owner:
                raise RuntimeError("claim registry already has a different owner")

    def claim(
        self,
        event: LaviInputEvent,
        *,
        claim_owner: object = None,
    ) -> tuple[AutoDepositTrustInputClaimReceipt | None, str]:
        with self._lock:
            if self._claim_owner is None or claim_owner is not self._claim_owner:
                return None, "auto_deposit_trust_input_internal_error"
        event_id = getattr(event, "event_id", None)
        if type(event_id) is not str or self._EVENT_ID_RE.fullmatch(event_id) is None:
            return None, "auto_deposit_trust_input_provenance_invalid"
        with self._lock:
            if event_id in self._records:
                return None, "auto_deposit_trust_duplicate_input_event"
            if len(self._records) >= self._capacity:
                return None, "auto_deposit_trust_input_event_capacity_exhausted"
            nonce = object()
            receipt = AutoDepositTrustInputClaimReceipt(
                event_id=event_id,
                source=event.source,
                provider_id=event.provider_id,
                event_kind=event.event_kind,
                registry_token=self._registry_token,
                nonce=nonce,
            )
            self._records[event_id] = {
                "receipt": receipt,
                "nonce": nonce,
                "state": "ISSUED",
                "source": event.source,
                "provider_id": event.provider_id,
                "event_kind": event.event_kind,
            }
            return receipt, ""

    def inspect(
        self,
        receipt: object,
        *,
        source: str,
        command_name: str,
    ) -> bool:
        with self._lock:
            return self._matches_issued(
                receipt,
                source=source,
                command_name=command_name,
            )

    def commit(
        self,
        receipt: object,
        *,
        source: str,
        command_name: str,
        request: object,
    ) -> bool:
        with self._lock:
            if not self._matches_issued(
                receipt,
                source=source,
                command_name=command_name,
            ):
                return False
            request_source = self._request_value(request, "source")
            request_command = self._request_value(request, "command")
            metadata = self._request_value(request, "metadata")
            input_event = (
                metadata.get("input_event")
                if isinstance(metadata, Mapping)
                else None
            )
            record = self._records[receipt.event_id]
            if (
                request_source != source
                or request_command != self.COMMAND
                or not isinstance(input_event, Mapping)
                or input_event.get("event_id") != receipt.event_id
                or input_event.get("source") != source
                or input_event.get("provider_id") != record.get("provider_id")
                or input_event.get("event_kind") != record.get("event_kind")
                or input_event.get("final") is not True
            ):
                return False
            record["state"] = "SPENT"
            return True

    def abandon_if_issued(self, receipt: object) -> None:
        try:
            with self._lock:
                record = self._record_for(receipt)
                if record is not None and record.get("state") == "ISSUED":
                    record["state"] = "SPENT"
        except Exception:
            return

    def state(self, receipt: object) -> str:
        with self._lock:
            record = self._record_for(receipt)
            return str(record.get("state")) if record is not None else "INVALID"

    @property
    def claimed_count(self) -> int:
        with self._lock:
            return len(self._records)

    @property
    def issued_count(self) -> int:
        with self._lock:
            return sum(
                1 for record in self._records.values()
                if record.get("state") == "ISSUED"
            )

    def _matches_issued(
        self,
        receipt: object,
        *,
        source: str,
        command_name: str,
    ) -> bool:
        if command_name != self.COMMAND_NAME:
            return False
        record = self._record_for(receipt)
        if record is None or record.get("state") != "ISSUED":
            return False
        return (
            type(source) is str
            and source == receipt.source
            and record.get("source") == source
        )

    def _record_for(self, receipt: object) -> dict[str, object] | None:
        if not isinstance(receipt, AutoDepositTrustInputClaimReceipt):
            return None
        if getattr(receipt, "_registry_token", None) is not self._registry_token:
            return None
        record = self._records.get(receipt.event_id)
        if record is None:
            return None
        if record.get("receipt") is not receipt:
            return None
        if record.get("nonce") is not getattr(receipt, "_nonce", None):
            return None
        return record

    def _request_value(self, request: object, name: str) -> object:
        if isinstance(request, Mapping):
            return request.get(name)
        return getattr(request, name, None)
