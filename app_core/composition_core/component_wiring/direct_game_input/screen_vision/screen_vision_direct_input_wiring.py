#20260905_kpopmodder: Own ScreenVision observation callback wiring.
from __future__ import annotations

from input_core.input_event.provenance import SCREEN_VISION


class ScreenVisionDirectInputWiring:
    def __init__(self, adapter_cache):
        self._adapter_cache = adapter_cache

    def wire(
        self,
        *,
        screen_vision,
        screen_vision_input_callback,
    ) -> None:
        if screen_vision is None or screen_vision_input_callback is None:
            return
        callback = self._adapter_cache.get_or_create(
            source=SCREEN_VISION,
            provider_id="ScreenVision",
            event_kind="screen_observation",
            output_callback=screen_vision_input_callback,
        )
        screen_vision.add_output_event_listener(callback)


__all__ = ("ScreenVisionDirectInputWiring",)
