#20260907_kpopmodder: Route generalized command terminals through typed non-preempting presentation.
from __future__ import annotations

from llm_core.routed_response import (
    RoutedResponseNonPreemptingDeliveryPolicy,
    RoutedResponsePresentationMetadata,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleTerminalResponse,
)
from plugins.Minecraft.fabric.chatclef.response.crafting_lifecycle import (
    CraftingLifecycleTerminalResponse,
)


class MinecraftCommandLifecycleTerminalResponseWiring:
    def __init__(self) -> None:
        self._callbacks = {}

    def wire(self, *, llm, extension=None):
        if extension is None:
            return None
        setter = getattr(
            extension,
            "set_command_lifecycle_terminal_response_callback",
            None,
        )
        emitter = getattr(llm, "emit_external_response", None)
        if not callable(setter) or not callable(emitter):
            return None
        key = (id(llm), id(extension))
        callback = self._callbacks.get(key)
        if callback is None:

            def callback(response):
                if type(response) not in {
                    CommandLifecycleTerminalResponse,
                    CraftingLifecycleTerminalResponse,
                }:
                    return None
                return emitter(
                    response.text,
                    source="minecraft_chatclef",
                    send_output=True,
                    send_full_output=False,
                    remember_history=False,
                    event_id=response.event_id,
                    route_kind=response.route_kind,
                    response_kind=response.response_kind,
                    delivery_mode=(
                        RoutedResponseNonPreemptingDeliveryPolicy.NON_PREEMPTING
                    ),
                    presentation_metadata=(
                        RoutedResponsePresentationMetadata.minecraft(
                            detail_log=response.presentation_detail_log,
                        )
                    ),
                    send_ui=True,
                )

            self._callbacks[key] = callback
        setter(callback)
        return callback


__all__ = ("MinecraftCommandLifecycleTerminalResponseWiring",)
