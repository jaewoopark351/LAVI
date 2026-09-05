#20260905_kpopmodder: Assemble focused ordinary translation stages.
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.translation.ordinary_translation_invocation import OrdinaryTranslationInvocation
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.translation.ordinary_translation_dto_validation import OrdinaryTranslationDtoValidation


class OrdinaryTranslationComponentGraph:
    def __init__(
        self,
        *,
        extension,
        translation_boundary,
        decision_factory,
        failure_handler,
        router_logger,
    ) -> None:
        self.invocation = OrdinaryTranslationInvocation(
            extension=extension,
            translation_boundary=translation_boundary,
            failure_handler=failure_handler,
        )
        self.dto_validation = OrdinaryTranslationDtoValidation(
            translation_boundary=translation_boundary,
            decision_factory=decision_factory,
            router_logger=router_logger,
        )


__all__ = ("OrdinaryTranslationComponentGraph",)
