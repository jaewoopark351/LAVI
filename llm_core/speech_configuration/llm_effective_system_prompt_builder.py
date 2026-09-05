#20260905_kpopmodder: Owns effective system-prompt composition for the selected speech style.
from __future__ import annotations


class LlmEffectiveSystemPromptBuilder:
    def __init__(
        self,
        *,
        helper_callback,
        current_mode_callback,
        default_prompt_callback,
    ) -> None:
        callbacks = (
            helper_callback,
            current_mode_callback,
            default_prompt_callback,
        )
        if not all(callable(callback) for callback in callbacks):
            raise TypeError("prompt builder callbacks must be callable")
        self._helper_callback = helper_callback
        self._current_mode_callback = current_mode_callback
        self._default_prompt_callback = default_prompt_callback

    def build(self, system_prompt=None) -> str:
        if system_prompt is None:
            base_prompt = str(self._default_prompt_callback() or "")
        else:
            base_prompt = str(system_prompt or "")
        return self._helper_callback().build_prompt(
            base_prompt,
            self._current_mode_callback(),
        )


__all__ = ("LlmEffectiveSystemPromptBuilder",)
