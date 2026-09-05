#20260905_kpopmodder: Preserve translation rejection sequencing as a thin facade.
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.rejection.ordinary_translation_rejection_component_graph import OrdinaryTranslationRejectionComponentGraph
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.rejection.ordinary_item_translation_rejection_owner import OrdinaryItemTranslationRejectionOwner


class OrdinaryTranslationRejectionStage:
    _ITEM_REJECTION_STATUSES = (
        OrdinaryItemTranslationRejectionOwner._OWNED_STATUSES
    )

    def __init__(
        self,
        *,
        translation_boundary,
        decision_factory,
        item_command_ownership_classifier,
        item_command_rejection_evidence_parser,
        live_proof_validator,
    ):
        self._translation_boundary = translation_boundary
        self._decision_factory = decision_factory
        self._item_command_ownership_classifier = (
            item_command_ownership_classifier
        )
        self._item_command_rejection_evidence_parser = (
            item_command_rejection_evidence_parser
        )
        self._live_proof_validator = live_proof_validator
        self._component_graph = OrdinaryTranslationRejectionComponentGraph(
            decision_factory=decision_factory,
            item_command_ownership_classifier=item_command_ownership_classifier,
            item_command_rejection_evidence_parser=(
                item_command_rejection_evidence_parser
            ),
            live_proof_validator=live_proof_validator,
        )
        self._item_rejection_owner = self._component_graph.item_rejection_owner
        self._generic_rejection = self._component_graph.generic_rejection

    def inspect(
        self,
        *,
        event: object,
        command_text: str,
        translation,
        korean_eligibility_proof: object,
    ):
        translation_status = self._translation_boundary.status(translation)
        if self._item_rejection_owner.owns_status(translation_status):
            return translation_status, self._item_rejection_owner.decide(
                event=event,
                command_text=command_text,
                translation=translation,
                translation_status=translation_status,
                korean_eligibility_proof=korean_eligibility_proof,
            )
        return translation_status, self._generic_rejection.decide(
            translation_status,
            translation,
        )


__all__ = ("OrdinaryTranslationRejectionStage",)
