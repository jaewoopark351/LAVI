#20260905_kpopmodder: Own system-prompt content persistence access.
from __future__ import annotations


class LlmSystemPromptContentPersistence:
    def __init__(self, context_manager):
        required = ("load_content", "update_file")
        if not all(
            callable(getattr(context_manager, name, None))
            for name in required
        ) or not hasattr(context_manager, "system_prompt_text"):
            raise TypeError("context_manager does not satisfy the content contract")
        self._context_manager = context_manager

    @property
    def context_manager(self):
        return self._context_manager

    @property
    def current_content(self):
        return self._context_manager.system_prompt_text

    def load(self):
        return self._context_manager.load_content()

    def update(self, new_content) -> None:
        self._context_manager.update_file(new_content)


__all__ = ("LlmSystemPromptContentPersistence",)
