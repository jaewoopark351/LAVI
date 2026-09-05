#20260905_kpopmodder: Own speech-style and system-prompt collaborator binding.
from __future__ import annotations

from core.config_manager import config_manager
from llm_core.content import LlmSystemPromptContentRepository
from llm_core.context_manager import LLMContextManager
from llm_core.speech_configuration import (
    LlmEffectiveSystemPromptBuilder,
    LlmSpeechStylePersistence,
    LlmSpeechStyleStateController,
    LlmSpeechStyleUpdateCoordinator,
)
from llm_core.speech_style import LLMSpeechStyleHelper


class LlmSpeechContentComponentGraph:
    def __init__(self, facade) -> None:
        self._facade = facade

    def install(self) -> None:
        facade = self._facade
        facade.context_manager = LLMContextManager(
            "ai_character_system_prompt.txt"
        )
        facade.system_prompt_text = facade.context_manager.system_prompt_text
        self.ensure_content_repository()

        facade.speech_style_helper = self.ensure_speech_style_helper()
        facade.llm_config = (
            self.ensure_speech_style_persistence().load_configuration()
        )
        initial_style = facade.llm_config.get(
            "speech_style",
            facade.speech_style_default,
        )
        facade.speech_style_mode = (
            self.ensure_speech_style_state_controller().initialize(
                initial_style
            )
        )

    def ensure_speech_style_helper(self):
        facade = self._facade
        helper = getattr(facade, "speech_style_helper", None)
        if helper is None:
            helper = LLMSpeechStyleHelper(
                default_mode=facade.speech_style_default,
                labels=facade.speech_style_labels,
                prompts=facade.speech_style_prompts,
                runtime_ability_prompt=facade.runtime_ability_prompt,
            )
            facade.speech_style_helper = helper
        return helper

    def ensure_speech_style_state_controller(self):
        facade = self._facade
        controller = getattr(facade, "speech_style_state_controller", None)
        if controller is None:
            controller = LlmSpeechStyleStateController(
                helper_callback=self.ensure_speech_style_helper,
                current_mode_callback=lambda: getattr(
                    facade,
                    "speech_style_mode",
                    facade.speech_style_default,
                ),
                update_mode_callback=lambda mode: setattr(
                    facade,
                    "speech_style_mode",
                    mode,
                ),
                default_mode=facade.speech_style_default,
            )
            facade.speech_style_state_controller = controller
        return controller

    def ensure_speech_style_persistence(self):
        facade = self._facade
        persistence = getattr(facade, "speech_style_persistence", None)
        if persistence is None:
            persistence = LlmSpeechStylePersistence(
                load_section_callback=config_manager.load_section,
                save_config_callback=config_manager.save_config,
            )
            facade.speech_style_persistence = persistence
        return persistence

    def ensure_speech_style_update_coordinator(self):
        facade = self._facade
        coordinator = getattr(facade, "speech_style_update_coordinator", None)
        if coordinator is None:
            coordinator = LlmSpeechStyleUpdateCoordinator(
                state_controller=self.ensure_speech_style_state_controller(),
                persistence=self.ensure_speech_style_persistence(),
            )
            facade.speech_style_update_coordinator = coordinator
        return coordinator

    def ensure_effective_system_prompt_builder(self):
        facade = self._facade
        builder = getattr(facade, "effective_system_prompt_builder", None)
        if builder is None:
            builder = LlmEffectiveSystemPromptBuilder(
                helper_callback=self.ensure_speech_style_helper,
                current_mode_callback=lambda: getattr(
                    facade,
                    "speech_style_mode",
                    facade.speech_style_default,
                ),
                default_prompt_callback=lambda: getattr(
                    facade,
                    "system_prompt_text",
                    "",
                ),
            )
            facade.effective_system_prompt_builder = builder
        return builder

    def ensure_content_repository(self):
        facade = self._facade
        repository = getattr(facade, "system_prompt_content_repository", None)
        context_manager = getattr(facade, "context_manager", None)
        if (
            repository is None
            or repository.context_manager is not context_manager
        ):
            if context_manager is None:
                context_manager = LLMContextManager(
                    "ai_character_system_prompt.txt"
                )
                facade.context_manager = context_manager
                facade.system_prompt_text = context_manager.system_prompt_text
            repository = LlmSystemPromptContentRepository(
                context_manager=context_manager,
                update_current_callback=lambda content: setattr(
                    facade,
                    "system_prompt_text",
                    content,
                ),
            )
            facade.system_prompt_content_repository = repository
        return repository


__all__ = ("LlmSpeechContentComponentGraph",)
