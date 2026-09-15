#20260915_kpopmodder: Immutable one-use exact single-container capability, distinct from H5 area and confirmation.
from dataclasses import dataclass


@dataclass(frozen=True, slots=True, eq=False)
class SingleContainerTrustReceipt:
    _owner: object
    event: object
    proof: object
    session: tuple[str, int]

    def matches_admission(self, command_name, source):
        from .single_container_trust_owner import SingleContainerTrustOwner

        return (
            type(self._owner) is SingleContainerTrustOwner
            and SingleContainerTrustOwner.is_live(self._owner, self)
            and command_name == "auto_deposit_trust"
            and source == self.event.source
        )

    def binding_metadata(self):
        return {
            "expected_session_id": self.session[0],
            "expected_generation": self.session[1],
            "original_event_id": self.event.event_id,
        }

    def commit(self, request):
        from .single_container_trust_owner import SingleContainerTrustOwner

        return (
            type(self._owner) is SingleContainerTrustOwner
            and SingleContainerTrustOwner.commit(self._owner, self, request)
        )

    def abandon(self):
        from .single_container_trust_owner import SingleContainerTrustOwner

        if type(self._owner) is SingleContainerTrustOwner:
            SingleContainerTrustOwner.abandon(self._owner, self)

    def __reduce__(self):
        raise TypeError(
            "single-container trust receipts are process-local capabilities"
        )
