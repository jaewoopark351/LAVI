#20260905_kpopmodder: Assemble system-prompt persistence and state synchronization.
from __future__ import annotations

from .llm_system_prompt_content_persistence import (
    LlmSystemPromptContentPersistence,
)
from .llm_system_prompt_current_state_synchronizer import (
    LlmSystemPromptCurrentStateSynchronizer,
)


class LlmSystemPromptContentComponentGraph:
    def __init__(self, *, context_manager, update_current_callback):
        self.persistence = LlmSystemPromptContentPersistence(context_manager)
        self.state_synchronizer = LlmSystemPromptCurrentStateSynchronizer(
            update_current_callback
        )


__all__ = ("LlmSystemPromptContentComponentGraph",)
