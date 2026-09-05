#20260905_kpopmodder: Preserve ordinary translation sequencing as a thin facade.

from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.translation.ordinary_translation_component_graph import OrdinaryTranslationComponentGraph


class OrdinaryTranslationStage:
    def __init__(
        self,
        *,
        extension,
        translation_boundary,
        decision_factory,
        failure_handler,
        router_logger,
    ):
        self._extension = extension
        self._translation_boundary = translation_boundary
        self._decision_factory = decision_factory
        self._failure_handler = failure_handler
        self._router_logger = router_logger
        self._component_graph = OrdinaryTranslationComponentGraph(
            extension=extension,
            translation_boundary=translation_boundary,
            decision_factory=decision_factory,
            failure_handler=failure_handler,
            router_logger=router_logger,
        )
        self._invocation = self._component_graph.invocation
        self._dto_validation = self._component_graph.dto_validation

    def translate(self, command_text: str):
        raw_translation, failure = self._invocation.invoke(command_text)
        if failure is not None:
            return None, failure
        return self._dto_validation.validate(raw_translation)


__all__ = ("OrdinaryTranslationStage",)
