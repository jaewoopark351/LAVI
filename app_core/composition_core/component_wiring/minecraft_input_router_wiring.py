#20260905_kpopmodder: Installs the required Minecraft input router even when its runtime extension is absent.
from app_core.composition_core.component_wiring.app_component_wiring_error import (
    AppComponentWiringError,
)


class MinecraftInputRouterWiring:
    def __init__(self):
        self._installed_llm = None
        self._installed_extension = None
        self._installed_router = None

    def wire(self, *, llm, extension=None):
        setter = getattr(llm, "set_input_router", None)
        if not callable(setter):
            raise AppComponentWiringError(
                "minecraft_input_router_setter",
                "LLM does not expose callable set_input_router",
            )
        if (
            self._installed_llm is llm
            and self._installed_extension is extension
            and self._installed_router is not None
        ):
            return self._installed_router
        try:
            from plugins.Minecraft.fabric.chatclef.input import (
                MinecraftChatClefInputRouter,
            )
            from plugins.Minecraft.fabric.chatclef.input.eligibility import (
                KoreanChatMicrophoneEligibilityAdmission,
            )
        except Exception as error:
            raise AppComponentWiringError(
                "minecraft_input_router_import",
                f"{type(error).__name__}: {error}",
            ) from error
        try:
            evidence_validator = getattr(
                llm,
                "validate_trusted_consumed_ingress_evidence",
                None,
            )
            router = MinecraftChatClefInputRouter(
                extension=extension,
                korean_eligibility_admission=(
                    KoreanChatMicrophoneEligibilityAdmission(
                        evidence_validator
                        if callable(evidence_validator)
                        else None
                    )
                ),
            )
        except Exception as error:
            raise AppComponentWiringError(
                "minecraft_input_router_construction",
                f"{type(error).__name__}: {error}",
            ) from error
        try:
            setter(router)
        except Exception as error:
            raise AppComponentWiringError(
                "minecraft_input_router_installation",
                f"{type(error).__name__}: {error}",
            ) from error
        self._installed_llm = llm
        self._installed_extension = extension
        self._installed_router = router
        return router


__all__ = ("MinecraftInputRouterWiring",)
