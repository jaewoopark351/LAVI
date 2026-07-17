#20260717_kpopmodder: Keeps concrete runtime imports behind the composition service.

from .core_component_composition_result import CoreComponentCompositionResult


class CoreComponentCompositionService:
    #20260717_kpopmodder: Keep concrete component imports behind compose() to avoid AppComposer owning classes directly.
    def compose(
        self,
        memory_context_builder=None,
        memory_command_handler=None,
        screen_question_router=None,
    ) -> CoreComponentCompositionResult:
        from input_core.input_component import Input
        from llm_core.llm_component import LLM
        from translation_core.translate_component import Translate
        from tts_core.tts_component import TTS
        from vtuber_core.vtuber_component import Vtuber

        input_component = Input()
        translate = Translate()
        tts = TTS()
        vtuber = Vtuber()
        llm = LLM(
            memory_context_builder=memory_context_builder,
            memory_command_handler=memory_command_handler,
            screen_question_router=screen_question_router,
        )
        startup_components = (
            input_component,
            translate,
            tts,
            vtuber,
            llm,
        )
        return CoreComponentCompositionResult(
            input=input_component,
            llm=llm,
            translate=translate,
            tts=tts,
            vtuber=vtuber,
            core_components=startup_components,
            startup_components=startup_components,
        )
