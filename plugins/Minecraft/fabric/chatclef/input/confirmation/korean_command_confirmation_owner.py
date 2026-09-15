#20260915_kpopmodder: Own at most one pending request per trusted source and a single-use confirmation handoff.
from collections.abc import Mapping
from time import monotonic

from .confirmation_input_classifier import KoreanConfirmationInputClassifier
from .confirmation_response_factory import KoreanConfirmationResponseFactory
from .confirmation_receipt import KoreanCommandConfirmationReceipt
from .confirmation_session_reader import KoreanConfirmationSessionReader
from .diagnostics.confirmation_logger import KoreanConfirmationLogger
from .pending_confirmation import PendingKoreanCommandConfirmation, semantic_translation


class KoreanCommandConfirmationOwner:
    _SOURCES = frozenset(("lavi_chat_ui", "voice_input_final"))

    def __init__(
        self,
        *,
        extension,
        submission_precheck,
        live_proof_validator,
        log_callback,
        now=monotonic,
        ttl_seconds=60,
    ):
        self._extension = extension
        self._live_proof_validator = live_proof_validator
        self._sessions = KoreanConfirmationSessionReader(
            extension=extension, submission_precheck=submission_precheck
        )
        self._classifier = KoreanConfirmationInputClassifier()
        self._responses = KoreanConfirmationResponseFactory()
        self._logger = KoreanConfirmationLogger(log_callback)
        self._now = now
        self._ttl = max(1, min(120, ttl_seconds))
        self._pending = {}
        self._receipts = {}

    def requires_confirmation(self, translation):
        command = str(translation.get("command") or "").split(" ", 1)[0]
        registry = getattr(self._extension, "korean_command_registry", None)
        if registry is None:
            return False
        try:
            spec = registry.spec(command)
        except KeyError:
            return False
        return spec.safety_tier in {"R3", "R4"}

    def begin(
        self, *, event, proof, command_text, translation, goto_input_binding=None
    ):
        if not self.requires_confirmation(translation):
            return None
        if not self._trusted(proof, event):
            return self._decision("confirmation_trust_required", event)
        session, reason = self._sessions.inspect()
        if session is None:
            return self._decision(reason, event)
        if len(command_text) > 512:
            return self._decision("confirmation_request_too_long", event)
        self._expire()
        replaced = self._pending.pop(event.source, None)
        if replaced is not None:
            self._logger.record(
                "replaced",
                event=replaced.event,
                command=replaced.command,
                pending=len(self._pending),
            )
        pending = PendingKoreanCommandConfirmation(
            event=event,
            command_text=command_text,
            command=translation["command"],
            semantic_json=semantic_translation(translation),
            session=session,
            expires_at=self._now() + self._ttl,
            goto_input_binding=goto_input_binding,
        )
        self._pending[event.source] = pending
        self._logger.record(
            "pending", event=event, command=pending.command, pending=len(self._pending)
        )
        return self._responses.pending(command_text, translation, self._ttl)

    def reply(self, *, event, proof):
        action = self._classifier.classify(event.text)
        if action is None:
            return None, None
        if not self._trusted(proof, event):
            return None, self._decision("confirmation_trust_required", event)
        pending = self._pending.pop(event.source, None)
        if pending is None:
            return None, self._decision("confirmation_missing", event)
        if action == "cancel":
            return None, self._decision(
                "confirmation_cancelled", event, pending.command
            )
        if self._now() >= pending.expires_at:
            return None, self._decision("confirmation_expired", event, pending.command)
        session, reason = self._sessions.inspect()
        if session != pending.session:
            return None, self._decision(
                reason if session is None else "confirmation_session_changed",
                event,
                pending.command,
            )
        if event.event_id == pending.event.event_id:
            return None, self._decision(
                "confirmation_duplicate", event, pending.command
            )
        receipt = KoreanCommandConfirmationReceipt(
            owner=self, pending=pending, confirmation_event=event, proof=proof
        )
        self._receipts[event.source] = receipt
        self._logger.record(
            "confirmed",
            event=event,
            command=pending.command,
            pending=len(self._pending),
        )
        return receipt, None

    def accepts_translation(self, receipt, *, event, translation):
        return (
            self.is_live_receipt(receipt, event)
            and semantic_translation(translation) == receipt.pending.semantic_json
        )

    def is_live_receipt(self, receipt, event):
        if (
            type(receipt) is not KoreanCommandConfirmationReceipt
            or receipt._owner is not self
            or receipt._spent
            or type(receipt.pending) is not PendingKoreanCommandConfirmation
        ):
            return False
        pending = receipt.pending
        return (
            self._receipts.get(getattr(pending.event, "source", None)) is receipt
            and event is pending.event
            and self._now() < pending.expires_at
            and self._trusted(receipt._proof, receipt.confirmation_event)
            and self._sessions.inspect()[0] == pending.session
        )

    def validate_feedback_proof(self, proof, event):
        if type(proof) is KoreanCommandConfirmationReceipt:
            return self.is_live_receipt(proof, event)
        return self._live_proof_validator(proof, event) is True

    def commit(self, receipt, request):
        pending = receipt.pending
        metadata = getattr(request, "metadata", None)
        input_event = (
            metadata.get("input_event") if isinstance(metadata, Mapping) else None
        )
        values = {
            "receipt_live": KoreanCommandConfirmationOwner.is_live_receipt(self, receipt, pending.event),
            "command_match": getattr(request, "command", None) == pending.command,
            "source_match": getattr(request, "source", None) == pending.event.source,
            "confirmation_binding_match": isinstance(metadata, Mapping)
            and metadata.get("korean_confirmation") == receipt.binding_metadata(),
            "input_binding_match": (
                isinstance(input_event, Mapping)
                and input_event.get("event_id") == pending.event.event_id
                and input_event.get("source") == pending.event.source
                and input_event.get("provider_id") == pending.event.provider_id
                and input_event.get("event_kind") == pending.event.event_kind
                and input_event.get("final") is True
            ),
        }
        accepted = all(values.values())
        KoreanCommandConfirmationOwner.abandon(
            self,
            receipt,
            reason="committed" if accepted else "commit_rejected",
            decision_values=values,
        )
        return accepted

    def abandon(self, receipt, reason="abandoned", decision_values=None):
        if (
            type(receipt) is not KoreanCommandConfirmationReceipt
            or receipt._owner is not self
            or receipt._spent
        ):
            return
        object.__setattr__(receipt, "_spent", True)
        if self._receipts.get(receipt.pending.event.source) is receipt:
            self._receipts.pop(receipt.pending.event.source, None)
        self._logger.record(
            reason,
            event=receipt.confirmation_event,
            command=receipt.pending.command,
            pending=len(self._pending),
            decision_values=decision_values,
        )

    def cancel_all(self):
        for pending in tuple(self._pending.values()):
            self._logger.record(
                "stop_cancelled",
                event=pending.event,
                command=pending.command,
                pending=0,
            )
        self._pending.clear()
        for receipt in tuple(self._receipts.values()):
            self.abandon(receipt, reason="stop_cancelled")

    def _expire(self):
        now = self._now()
        for source, pending in tuple(self._pending.items()):
            if now >= pending.expires_at:
                self._pending.pop(source, None)
                self._logger.record(
                    "expired",
                    event=pending.event,
                    command=pending.command,
                    pending=len(self._pending),
                )

    def _trusted(self, proof, event):
        return (
            getattr(event, "source", None) in self._SOURCES
            and getattr(event, "final", None) is True
            and self._live_proof_validator(proof, event) is True
        )

    def _decision(self, reason, event, command=""):
        self._logger.record(
            reason, event=event, command=command, pending=len(self._pending)
        )
        return self._responses.outcome(reason)
