#20260905_kpopmodder: Route verified asynchronous STOP terminals to output/TTS only.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.response.stop import (
    StopControlTerminalResponse,
)


class MinecraftStopTerminalResponseWiring:
    def __init__(self):
        self._callbacks = {}

    def wire(self, *, llm, extension=None):
        if extension is None:
            return None
        setter = getattr(extension, "set_stop_terminal_response_callback", None)
        emitter = getattr(llm, "emit_external_response", None)
        if not callable(setter) or not callable(emitter):
            return None
        key = (id(llm), id(extension))
        callback = self._callbacks.get(key)
        if callback is None:

            def callback(response):
                if type(response) is not StopControlTerminalResponse:
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
                )

            self._callbacks[key] = callback
        setter(callback)
        return callback


__all__ = ("MinecraftStopTerminalResponseWiring",)
