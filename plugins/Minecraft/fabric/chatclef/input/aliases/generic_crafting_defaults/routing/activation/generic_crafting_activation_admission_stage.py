#20260905_kpopmodder: Own Feature-B activation admission and rejection only.


class GenericCraftingActivationAdmissionStage:
    def __init__(self, *, admission, decision_factory):
        self._admission = admission
        self._decision_factory = decision_factory

    def admit(
        self,
        event: object,
        korean_eligibility_proof: object,
        candidate: object,
    ):
        admission = self._admission.admit(
            event,
            korean_eligibility_proof,
            candidate,
        )
        if not admission.admitted:
            if not admission.feature_owned:
                return None
            return self._decision_factory.generic_crafting_defaults_rejection(
                admission.reason_code,
                admission.message,
            )
        if admission.receipt is None:
            return self._decision_factory.generic_crafting_defaults_rejection(
                "generic_crafting_activation_invalid",
                "제작 요청 권한을 발급하지 않았어요.",
            )
        return admission.receipt


__all__ = ("GenericCraftingActivationAdmissionStage",)
