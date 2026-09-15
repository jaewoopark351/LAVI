#20260915_kpopmodder: Keep the one-use confirmation capability out of serialized request metadata.
from dataclasses import dataclass


@dataclass(frozen=True, slots=True, init=False)
class KoreanCommandConfirmationReceipt:
    _owner: object
    pending: object
    confirmation_event: object
    _proof: object
    _spent: bool

    def __init__(self, *, owner, pending, confirmation_event, proof):
        object.__setattr__(self, "_owner", owner)
        object.__setattr__(self, "pending", pending)
        object.__setattr__(self, "confirmation_event", confirmation_event)
        object.__setattr__(self, "_proof", proof)
        object.__setattr__(self, "_spent", False)

    def binding_metadata(self):
        return {
            "expected_session_id": self.pending.session[0],
            "expected_generation": self.pending.session[1],
            "original_event_id": self.pending.event.event_id,
            "confirmation_event_id": self.confirmation_event.event_id,
        }

    def is_live(self, event):
        from .korean_command_confirmation_owner import KoreanCommandConfirmationOwner

        return (
            type(self._owner) is KoreanCommandConfirmationOwner
            and KoreanCommandConfirmationOwner.is_live_receipt(self._owner, self, event)
        )

    def matches_admission(self, command_name, source):
        if not self.is_live(getattr(self.pending, "event", None)):
            return False
        return (
            self.pending.command.split(" ", 1)[0] == command_name
            and self.pending.event.source == source
        )

    def commit(self, request):
        from .korean_command_confirmation_owner import KoreanCommandConfirmationOwner

        return (
            type(self._owner) is KoreanCommandConfirmationOwner
            and KoreanCommandConfirmationOwner.commit(self._owner, self, request)
        )

    def abandon(self):
        from .korean_command_confirmation_owner import KoreanCommandConfirmationOwner

        if type(self._owner) is KoreanCommandConfirmationOwner:
            KoreanCommandConfirmationOwner.abandon(self._owner, self)

    def __reduce__(self):
        raise TypeError("confirmation receipts are process-local capabilities")
