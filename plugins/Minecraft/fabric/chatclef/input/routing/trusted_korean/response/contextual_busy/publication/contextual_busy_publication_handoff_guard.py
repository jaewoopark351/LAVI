#20260905_kpopmodder: Keep trusted Korean route responsibilities split by module.
#20260909_kpopmodder: Transfer one local contextual STATUS acknowledgement safely.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.publication.status import (
    CommandStatusPublicationCustody,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication import (
    CommandFeedbackPublicationAcknowledgement,
)

from ..contextual_busy_response_preparation import (
    ContextualBusyResponsePreparation,
)
from ..contextual_busy_suppressed_decision import (
    CONTEXTUAL_BUSY_SUPPRESSED_DECISION,
)


class ContextualBusyPublicationHandoffGuard:
    _STAGE = "contextual_busy_custody_handoff"

    def claim_and_transfer(self, preparation: object, *, outer_claim):
        if type(preparation) is not ContextualBusyResponsePreparation:
            raise TypeError("contextual busy preparation must be exact")
        acknowledgement = preparation.local_handoff_token
        if acknowledgement is None:
            return preparation.decision, outer_claim(preparation.decision)

        try:
            custody = outer_claim(preparation.decision)
        except Exception as error:
            return self._fail(acknowledgement, type(error).__name__)
        if not self._valid_transfer(
            preparation=preparation,
            acknowledgement=acknowledgement,
            custody=custody,
        ):
            return self._fail(acknowledgement, "none")
        return preparation.decision, custody

    @staticmethod
    def _valid_transfer(*, preparation, acknowledgement, custody) -> bool:
        try:
            return bool(
                type(acknowledgement)
                is CommandFeedbackPublicationAcknowledgement
                and type(custody) is CommandStatusPublicationCustody
                and custody.emergency is False
                and custody.decision is preparation.decision
                and custody.acknowledgement is acknowledgement
                and custody.decision_type is type(preparation.decision)
                and custody.route_kind == "command_busy_current_work"
                and custody.response_kind == "command_status"
                and preparation.decision.response_publication_acknowledgement
                is acknowledgement
            )
        except Exception:
            return False

    def _fail(self, acknowledgement: object, exception_class: str):
        try:
            diagnostic_custody = getattr(
                acknowledgement,
                "publication_failure_diagnostic_custody",
                None,
            )
            record_once = getattr(diagnostic_custody, "record_once", None)
        except Exception:
            record_once = None
        if callable(record_once):
            try:
                record_once(self._STAGE, exception_class)
            except Exception:
                pass
        try:
            acknowledge = getattr(acknowledgement, "acknowledge", None)
        except Exception:
            acknowledge = None
        if callable(acknowledge):
            try:
                acknowledge(published=False)
            except Exception:
                pass
        return CONTEXTUAL_BUSY_SUPPRESSED_DECISION, None


__all__ = ("ContextualBusyPublicationHandoffGuard",)
