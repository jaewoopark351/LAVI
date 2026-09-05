#20260905_kpopmodder: Deliver one activation-bound crafting command only.


class GenericCraftingSubmissionDelivery:
    def __init__(self, *, extension, submission_boundary):
        self._extension = extension
        self._submission_boundary = submission_boundary

    def deliver(
        self,
        *,
        event: object,
        translation: object,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ):
        return self._submission_boundary.submit_generic_crafting_defaults_once(
            self._extension,
            event,
            translation,
            activation_receipt=activation_receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )


__all__ = ("GenericCraftingSubmissionDelivery",)
