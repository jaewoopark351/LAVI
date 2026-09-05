#20260905_kpopmodder: Preserve the system-prompt content repository facade.
from __future__ import annotations

from .llm_system_prompt_content_component_graph import (
    LlmSystemPromptContentComponentGraph,
)


class LlmSystemPromptContentRepository:
    def __init__(self, *, context_manager, update_current_callback) -> None:
        self._components = LlmSystemPromptContentComponentGraph(
            context_manager=context_manager,
            update_current_callback=update_current_callback,
        )

    @property
    def context_manager(self):
        return self._components.persistence.context_manager

    def load(self) -> str:
        content = self._components.persistence.load()
        self._components.state_synchronizer.synchronize(content)
        return content

    def update(self, new_content) -> None:
        self._components.persistence.update(new_content)
        self._components.state_synchronizer.synchronize(
            self._components.persistence.current_content
        )


__all__ = ("LlmSystemPromptContentRepository",)
