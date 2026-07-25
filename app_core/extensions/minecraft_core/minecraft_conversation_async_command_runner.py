#20260725_kpopmodder: Added background runner so long Minecraft actions do not block chat replies.
from __future__ import annotations

import threading
from typing import Any, Callable

from core.logger import log_print

from .minecraft_conversation_command_route import MinecraftConversationCommandRoute
from .minecraft_conversation_completion_notifier import (
    MinecraftConversationCompletionNotifier,
)
from .minecraft_conversation_completion_result_formatter import (
    MinecraftConversationCompletionResultFormatter,
)
from .minecraft_conversation_pending_action_watcher import (
    MinecraftConversationPendingActionWatcher,
)
from .minecraft_conversation_pending_result_detector import (
    MinecraftConversationPendingResultDetector,
)


class MinecraftConversationAsyncCommandRunner:
    def __init__(
        self,
        completion_formatter: MinecraftConversationCompletionResultFormatter | None = None,
        completion_notifier: MinecraftConversationCompletionNotifier | None = None,
        pending_result_detector: MinecraftConversationPendingResultDetector | None = None,
        pending_action_watcher: MinecraftConversationPendingActionWatcher | None = None,
        thread_factory: Callable[..., threading.Thread] | None = None,
    ):
        self.completion_formatter = (
            completion_formatter or MinecraftConversationCompletionResultFormatter()
        )
        self.completion_notifier = (
            completion_notifier or MinecraftConversationCompletionNotifier()
        )
        self.pending_result_detector = (
            pending_result_detector or MinecraftConversationPendingResultDetector()
        )
        self.pending_action_watcher = (
            pending_action_watcher or MinecraftConversationPendingActionWatcher()
        )
        self.thread_factory = thread_factory or threading.Thread

    @property
    def can_notify(self) -> bool:
        return self.completion_notifier.enabled

    def start(self, route: MinecraftConversationCommandRoute, extension: Any):
        thread = self.thread_factory(
            target=self._run,
            args=(route, extension),
            daemon=True,
        )
        thread.start()
        return thread

    def _run(self, route: MinecraftConversationCommandRoute, extension: Any) -> None:
        try:
            result = extension.handle_command(route.command)
        except Exception as error:
            log_print(f"[MinecraftConversation] async command failed: {error}")
            message = self.completion_formatter.command_failed(route, error)
        else:
            message = self.completion_formatter.format(route, result)
        self.completion_notifier.notify(message)
        if self.pending_result_detector.is_pending(result):
            self._watch_pending_action(route, extension, result)

    def _watch_pending_action(
        self,
        route: MinecraftConversationCommandRoute,
        extension: Any,
        result,
    ) -> None:
        final_result = self.pending_action_watcher.wait(extension, result)
        if final_result is None:
            return
        self.completion_notifier.notify(
            self.completion_formatter.format(route, final_result)
        )
