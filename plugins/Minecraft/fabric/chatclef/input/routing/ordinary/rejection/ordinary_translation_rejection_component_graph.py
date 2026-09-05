#20260905_kpopmodder: Assemble focused ordinary translation rejection owners.
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.rejection.ordinary_item_translation_rejection_owner import OrdinaryItemTranslationRejectionOwner
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.rejection.ordinary_generic_translation_rejection import OrdinaryGenericTranslationRejection


class OrdinaryTranslationRejectionComponentGraph:
    def __init__(
        self,
        *,
        decision_factory,
        item_command_ownership_classifier,
        item_command_rejection_evidence_parser,
        live_proof_validator,
    ) -> None:
        self.item_rejection_owner = OrdinaryItemTranslationRejectionOwner(
            decision_factory=decision_factory,
            item_command_ownership_classifier=item_command_ownership_classifier,
            item_command_rejection_evidence_parser=(
                item_command_rejection_evidence_parser
            ),
            live_proof_validator=live_proof_validator,
        )
        self.generic_rejection = OrdinaryGenericTranslationRejection(
            decision_factory
        )


__all__ = ("OrdinaryTranslationRejectionComponentGraph",)
